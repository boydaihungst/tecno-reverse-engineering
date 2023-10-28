package com.android.server.voiceinteraction;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.UriGrantsManager;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.IVoiceInteractionSessionService;
import android.service.voice.VisibleActivityInfo;
import android.util.Slog;
import android.view.IWindowManager;
import com.android.internal.app.AssistUtils;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractor;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.am.AssistDataRequester;
import com.android.server.job.controllers.JobStatus;
import com.android.server.power.LowPowerStandbyControllerInternal;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.uri.UriGrantsManagerInternal;
import com.android.server.wm.ActivityAssistInfo;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class VoiceInteractionSessionConnection implements ServiceConnection, AssistDataRequester.AssistDataRequesterCallbacks {
    static final int BOOST_TIMEOUT_MS = 300;
    static final boolean DEBUG = false;
    private static final int LOW_POWER_STANDBY_ALLOWLIST_TIMEOUT_MS = 120000;
    static final int MAX_POWER_BOOST_TIMEOUT = 10000;
    static final int POWER_BOOST_TIMEOUT_MS = Integer.parseInt(System.getProperty("vendor.powerhal.interaction.max", "200"));
    static final String TAG = "VoiceInteractionServiceManager";
    final IActivityManager mAm;
    final AppOpsManager mAppOps;
    AssistDataRequester mAssistDataRequester;
    final Intent mBindIntent;
    boolean mBound;
    final Callback mCallback;
    final int mCallingUid;
    boolean mCanceled;
    final Context mContext;
    private final Handler mFgHandler;
    final ServiceConnection mFullConnection;
    boolean mFullyBound;
    final Handler mHandler;
    final IWindowManager mIWindowManager;
    IVoiceInteractor mInteractor;
    private boolean mListeningVisibleActivity;
    final Object mLock;
    private boolean mLowPowerStandbyAllowlisted;
    private final LowPowerStandbyControllerInternal mLowPowerStandbyControllerInternal;
    private List<ActivityAssistInfo> mPendingHandleAssistWithoutData;
    ArrayList<IVoiceInteractionSessionShowCallback> mPendingShowCallbacks;
    final IBinder mPermissionOwner;
    private final PowerManagerInternal mPowerManagerInternal;
    private final Runnable mRemoveFromLowPowerStandbyAllowlistRunnable;
    private final ScheduledExecutorService mScheduledExecutorService;
    IVoiceInteractionSessionService mService;
    IVoiceInteractionSession mSession;
    final ComponentName mSessionComponentName;
    private PowerBoostSetter mSetPowerBoostRunnable;
    Bundle mShowArgs;
    private Runnable mShowAssistDisclosureRunnable;
    IVoiceInteractionSessionShowCallback mShowCallback;
    int mShowFlags;
    boolean mShown;
    final IBinder mToken;
    final UriGrantsManagerInternal mUgmInternal;
    final int mUser;
    private final List<VisibleActivityInfo> mVisibleActivityInfos;

    /* loaded from: classes2.dex */
    public interface Callback {
        void onSessionHidden(VoiceInteractionSessionConnection voiceInteractionSessionConnection);

        void onSessionShown(VoiceInteractionSessionConnection voiceInteractionSessionConnection);

        void sessionConnectionGone(VoiceInteractionSessionConnection voiceInteractionSessionConnection);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class PowerBoostSetter implements Runnable {
        private boolean mCanceled;
        private final Instant mExpiryTime;

        PowerBoostSetter(Instant expiryTime) {
            this.mExpiryTime = expiryTime;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (VoiceInteractionSessionConnection.this.mLock) {
                if (this.mCanceled) {
                    return;
                }
                if (Instant.now().isBefore(this.mExpiryTime)) {
                    VoiceInteractionSessionConnection.this.mPowerManagerInternal.setPowerBoost(0, 300);
                    if (VoiceInteractionSessionConnection.this.mSetPowerBoostRunnable != null) {
                        VoiceInteractionSessionConnection.this.mFgHandler.postDelayed(VoiceInteractionSessionConnection.this.mSetPowerBoostRunnable, VoiceInteractionSessionConnection.POWER_BOOST_TIMEOUT_MS);
                    }
                } else {
                    Slog.w(VoiceInteractionSessionConnection.TAG, "Reset power boost INTERACTION because reaching max timeout.");
                    VoiceInteractionSessionConnection.this.mPowerManagerInternal.setPowerBoost(0, -1);
                }
            }
        }

        void cancel() {
            synchronized (VoiceInteractionSessionConnection.this.mLock) {
                this.mCanceled = true;
            }
        }
    }

    public VoiceInteractionSessionConnection(Object lock, ComponentName component, int user, Context context, Callback callback, int callingUid, Handler handler) {
        Binder binder = new Binder();
        this.mToken = binder;
        this.mPendingShowCallbacks = new ArrayList<>();
        this.mPendingHandleAssistWithoutData = new ArrayList();
        this.mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.mVisibleActivityInfos = new ArrayList();
        this.mRemoveFromLowPowerStandbyAllowlistRunnable = new Runnable() { // from class: com.android.server.voiceinteraction.VoiceInteractionSessionConnection$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                VoiceInteractionSessionConnection.this.removeFromLowPowerStandbyAllowlist();
            }
        };
        this.mShowCallback = new IVoiceInteractionSessionShowCallback.Stub() { // from class: com.android.server.voiceinteraction.VoiceInteractionSessionConnection.1
            public void onFailed() throws RemoteException {
                synchronized (VoiceInteractionSessionConnection.this.mLock) {
                    VoiceInteractionSessionConnection.this.notifyPendingShowCallbacksFailedLocked();
                }
            }

            public void onShown() throws RemoteException {
                synchronized (VoiceInteractionSessionConnection.this.mLock) {
                    VoiceInteractionSessionConnection.this.notifyPendingShowCallbacksShownLocked();
                }
            }
        };
        this.mFullConnection = new ServiceConnection() { // from class: com.android.server.voiceinteraction.VoiceInteractionSessionConnection.2
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        this.mShowAssistDisclosureRunnable = new Runnable() { // from class: com.android.server.voiceinteraction.VoiceInteractionSessionConnection.3
            @Override // java.lang.Runnable
            public void run() {
                StatusBarManagerInternal statusBarInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                if (statusBarInternal != null) {
                    statusBarInternal.showAssistDisclosure();
                }
            }
        };
        this.mLock = lock;
        this.mSessionComponentName = component;
        this.mUser = user;
        this.mContext = context;
        this.mCallback = callback;
        this.mCallingUid = callingUid;
        this.mHandler = handler;
        this.mAm = ActivityManager.getService();
        UriGrantsManagerInternal uriGrantsManagerInternal = (UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class);
        this.mUgmInternal = uriGrantsManagerInternal;
        IWindowManager asInterface = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        this.mIWindowManager = asInterface;
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mLowPowerStandbyControllerInternal = (LowPowerStandbyControllerInternal) LocalServices.getService(LowPowerStandbyControllerInternal.class);
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mFgHandler = FgThread.getHandler();
        this.mAssistDataRequester = new AssistDataRequester(context, asInterface, (AppOpsManager) context.getSystemService("appops"), this, lock, 49, 50);
        IBinder permOwner = uriGrantsManagerInternal.newUriPermissionOwner("voicesession:" + component.flattenToShortString());
        this.mPermissionOwner = permOwner;
        Intent intent = new Intent("android.service.voice.VoiceInteractionService");
        this.mBindIntent = intent;
        intent.setComponent(component);
        boolean bindServiceAsUser = context.bindServiceAsUser(intent, this, 1048625, new UserHandle(user));
        this.mBound = bindServiceAsUser;
        if (!bindServiceAsUser) {
            Slog.w(TAG, "Failed binding to voice interaction session service " + component);
            return;
        }
        try {
            asInterface.addWindowToken(binder, 2031, 0, (Bundle) null);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed adding window token", e);
        }
    }

    public int getUserDisabledShowContextLocked() {
        int flags = 0;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "assist_structure_enabled", 1, this.mUser) == 0) {
            flags = 0 | 1;
        }
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "assist_screenshot_enabled", 1, this.mUser) == 0) {
            return flags | 2;
        }
        return flags;
    }

    public boolean showLocked(Bundle args, int flags, int disabledContext, IVoiceInteractionSessionShowCallback showCallback, List<ActivityAssistInfo> topActivities) {
        if (!this.mBound) {
            if (showCallback != null) {
                try {
                    showCallback.onFailed();
                } catch (RemoteException e) {
                }
            }
            return false;
        }
        if (!this.mFullyBound) {
            this.mFullyBound = this.mContext.bindServiceAsUser(this.mBindIntent, this.mFullConnection, 135790593, new UserHandle(this.mUser));
        }
        this.mShown = true;
        this.mShowArgs = args;
        this.mShowFlags = flags;
        int disabledContext2 = disabledContext | getUserDisabledShowContextLocked();
        boolean fetchData = (flags & 1) != 0;
        boolean fetchScreenshot = (flags & 2) != 0;
        boolean assistDataRequestNeeded = fetchData || fetchScreenshot;
        if (assistDataRequestNeeded) {
            int topActivitiesCount = topActivities.size();
            ArrayList<IBinder> topActivitiesToken = new ArrayList<>(topActivitiesCount);
            for (int i = 0; i < topActivitiesCount; i++) {
                topActivitiesToken.add(topActivities.get(i).getActivityToken());
            }
            this.mAssistDataRequester.requestAssistData(topActivitiesToken, fetchData, fetchScreenshot, (disabledContext2 & 1) == 0, (disabledContext2 & 2) == 0, this.mCallingUid, this.mSessionComponentName.getPackageName());
            boolean needDisclosure = this.mAssistDataRequester.getPendingDataCount() > 0 || this.mAssistDataRequester.getPendingScreenshotCount() > 0;
            if (needDisclosure && AssistUtils.shouldDisclose(this.mContext, this.mSessionComponentName)) {
                this.mHandler.post(this.mShowAssistDisclosureRunnable);
            }
        }
        IVoiceInteractionSession iVoiceInteractionSession = this.mSession;
        if (iVoiceInteractionSession != null) {
            try {
                iVoiceInteractionSession.show(this.mShowArgs, this.mShowFlags, showCallback);
                this.mShowArgs = null;
                this.mShowFlags = 0;
            } catch (RemoteException e2) {
            }
            if (assistDataRequestNeeded) {
                this.mAssistDataRequester.processPendingAssistData();
            } else {
                doHandleAssistWithoutData(topActivities);
            }
        } else {
            if (showCallback != null) {
                this.mPendingShowCallbacks.add(showCallback);
            }
            if (!assistDataRequestNeeded) {
                this.mPendingHandleAssistWithoutData = topActivities;
            }
        }
        PowerBoostSetter powerBoostSetter = this.mSetPowerBoostRunnable;
        if (powerBoostSetter != null) {
            powerBoostSetter.cancel();
        }
        PowerBoostSetter powerBoostSetter2 = new PowerBoostSetter(Instant.now().plusMillis(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY));
        this.mSetPowerBoostRunnable = powerBoostSetter2;
        this.mFgHandler.post(powerBoostSetter2);
        LowPowerStandbyControllerInternal lowPowerStandbyControllerInternal = this.mLowPowerStandbyControllerInternal;
        if (lowPowerStandbyControllerInternal != null) {
            lowPowerStandbyControllerInternal.addToAllowlist(this.mCallingUid);
            this.mLowPowerStandbyAllowlisted = true;
            this.mFgHandler.removeCallbacks(this.mRemoveFromLowPowerStandbyAllowlistRunnable);
            this.mFgHandler.postDelayed(this.mRemoveFromLowPowerStandbyAllowlistRunnable, 120000L);
        }
        this.mCallback.onSessionShown(this);
        return true;
    }

    private void doHandleAssistWithoutData(List<ActivityAssistInfo> topActivities) {
        int activityCount = topActivities.size();
        for (int i = 0; i < activityCount; i++) {
            ActivityAssistInfo topActivity = topActivities.get(i);
            IBinder assistToken = topActivity.getAssistToken();
            int taskId = topActivity.getTaskId();
            int activityIndex = i;
            try {
                this.mSession.handleAssist(taskId, assistToken, (Bundle) null, (AssistStructure) null, (AssistContent) null, activityIndex, activityCount);
            } catch (RemoteException e) {
            }
        }
    }

    @Override // com.android.server.am.AssistDataRequester.AssistDataRequesterCallbacks
    public boolean canHandleReceivedAssistDataLocked() {
        return this.mSession != null;
    }

    @Override // com.android.server.am.AssistDataRequester.AssistDataRequesterCallbacks
    public void onAssistDataReceivedLocked(Bundle data, int activityIndex, int activityCount) {
        int uid;
        ClipData clipData;
        IVoiceInteractionSession iVoiceInteractionSession = this.mSession;
        if (iVoiceInteractionSession == null) {
            return;
        }
        if (data == null) {
            try {
                iVoiceInteractionSession.handleAssist(-1, (IBinder) null, (Bundle) null, (AssistStructure) null, (AssistContent) null, 0, 0);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        int taskId = data.getInt(ActivityTaskManagerInternal.ASSIST_TASK_ID);
        IBinder activityId = data.getBinder(ActivityTaskManagerInternal.ASSIST_ACTIVITY_ID);
        Bundle assistData = data.getBundle("data");
        AssistStructure structure = (AssistStructure) data.getParcelable(ActivityTaskManagerInternal.ASSIST_KEY_STRUCTURE);
        AssistContent content = (AssistContent) data.getParcelable(ActivityTaskManagerInternal.ASSIST_KEY_CONTENT);
        if (assistData == null) {
            uid = -1;
        } else {
            int uid2 = assistData.getInt("android.intent.extra.ASSIST_UID", -1);
            uid = uid2;
        }
        if (uid >= 0 && content != null) {
            Intent intent = content.getIntent();
            if (intent != null && (clipData = intent.getClipData()) != null && Intent.isAccessUriMode(intent.getFlags())) {
                grantClipDataPermissions(clipData, intent.getFlags(), uid, this.mCallingUid, this.mSessionComponentName.getPackageName());
            }
            ClipData clipData2 = content.getClipData();
            if (clipData2 != null) {
                grantClipDataPermissions(clipData2, 1, uid, this.mCallingUid, this.mSessionComponentName.getPackageName());
            }
        }
        try {
            try {
                this.mSession.handleAssist(taskId, activityId, assistData, structure, content, activityIndex, activityCount);
            } catch (RemoteException e2) {
            }
        } catch (RemoteException e3) {
        }
    }

    @Override // com.android.server.am.AssistDataRequester.AssistDataRequesterCallbacks
    public void onAssistScreenshotReceivedLocked(Bitmap screenshot) {
        IVoiceInteractionSession iVoiceInteractionSession = this.mSession;
        if (iVoiceInteractionSession == null) {
            return;
        }
        try {
            iVoiceInteractionSession.handleScreenshot(screenshot);
        } catch (RemoteException e) {
        }
    }

    void grantUriPermission(Uri uri, int mode, int srcUid, int destUid, String destPkg) {
        int sourceUserId;
        if (!ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(uri.getScheme())) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mUgmInternal.checkGrantUriPermission(srcUid, null, ContentProvider.getUriWithoutUserId(uri), mode, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(srcUid)));
            sourceUserId = ContentProvider.getUserIdFromUri(uri, this.mUser);
        } catch (RemoteException e) {
        } catch (SecurityException e2) {
            e = e2;
        } catch (Throwable th) {
            th = th;
        }
        try {
            UriGrantsManager.getService().grantUriPermissionFromOwner(this.mPermissionOwner, srcUid, destPkg, ContentProvider.getUriWithoutUserId(uri), 1, sourceUserId, this.mUser);
            Binder.restoreCallingIdentity(ident);
        } catch (RemoteException e3) {
            Binder.restoreCallingIdentity(ident);
        } catch (SecurityException e4) {
            e = e4;
            try {
                Slog.w(TAG, "Can't propagate permission", e);
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th2) {
                th = th2;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    void grantClipDataItemPermission(ClipData.Item item, int mode, int srcUid, int destUid, String destPkg) {
        if (item.getUri() != null) {
            grantUriPermission(item.getUri(), mode, srcUid, destUid, destPkg);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            grantUriPermission(intent.getData(), mode, srcUid, destUid, destPkg);
        }
    }

    void grantClipDataPermissions(ClipData data, int mode, int srcUid, int destUid, String destPkg) {
        int N = data.getItemCount();
        for (int i = 0; i < N; i++) {
            grantClipDataItemPermission(data.getItemAt(i), mode, srcUid, destUid, destPkg);
        }
    }

    public boolean hideLocked() {
        if (this.mBound) {
            if (this.mShown) {
                this.mShown = false;
                this.mShowArgs = null;
                this.mShowFlags = 0;
                this.mAssistDataRequester.cancel();
                this.mPendingShowCallbacks.clear();
                IVoiceInteractionSession iVoiceInteractionSession = this.mSession;
                if (iVoiceInteractionSession != null) {
                    try {
                        iVoiceInteractionSession.hide();
                    } catch (RemoteException e) {
                    }
                }
                this.mUgmInternal.revokeUriPermissionFromOwner(this.mPermissionOwner, null, 3, this.mUser);
                if (this.mSession != null) {
                    try {
                        ActivityTaskManager.getService().finishVoiceTask(this.mSession);
                    } catch (RemoteException e2) {
                    }
                }
                PowerBoostSetter powerBoostSetter = this.mSetPowerBoostRunnable;
                if (powerBoostSetter != null) {
                    powerBoostSetter.cancel();
                    this.mSetPowerBoostRunnable = null;
                }
                this.mPowerManagerInternal.setPowerBoost(0, -1);
                if (this.mLowPowerStandbyControllerInternal != null) {
                    removeFromLowPowerStandbyAllowlist();
                }
                this.mCallback.onSessionHidden(this);
            }
            if (this.mFullyBound) {
                this.mContext.unbindService(this.mFullConnection);
                this.mFullyBound = false;
                return true;
            }
            return true;
        }
        return false;
    }

    public void cancelLocked(boolean finishTask) {
        this.mListeningVisibleActivity = false;
        this.mVisibleActivityInfos.clear();
        hideLocked();
        this.mCanceled = true;
        if (this.mBound) {
            IVoiceInteractionSession iVoiceInteractionSession = this.mSession;
            if (iVoiceInteractionSession != null) {
                try {
                    iVoiceInteractionSession.destroy();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Voice interation session already dead");
                }
            }
            if (finishTask && this.mSession != null) {
                try {
                    ActivityTaskManager.getService().finishVoiceTask(this.mSession);
                } catch (RemoteException e2) {
                }
            }
            this.mContext.unbindService(this);
            try {
                this.mIWindowManager.removeWindowToken(this.mToken, 0);
            } catch (RemoteException e3) {
                Slog.w(TAG, "Failed removing window token", e3);
            }
            this.mBound = false;
            this.mService = null;
            this.mSession = null;
            this.mInteractor = null;
        }
        if (this.mFullyBound) {
            this.mContext.unbindService(this.mFullConnection);
            this.mFullyBound = false;
        }
    }

    public boolean deliverNewSessionLocked(IVoiceInteractionSession session, IVoiceInteractor interactor) {
        this.mSession = session;
        this.mInteractor = interactor;
        if (this.mShown) {
            try {
                session.show(this.mShowArgs, this.mShowFlags, this.mShowCallback);
                this.mShowArgs = null;
                this.mShowFlags = 0;
            } catch (RemoteException e) {
            }
            this.mAssistDataRequester.processPendingAssistData();
            if (!this.mPendingHandleAssistWithoutData.isEmpty()) {
                doHandleAssistWithoutData(this.mPendingHandleAssistWithoutData);
                this.mPendingHandleAssistWithoutData.clear();
                return true;
            }
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyPendingShowCallbacksShownLocked() {
        for (int i = 0; i < this.mPendingShowCallbacks.size(); i++) {
            try {
                this.mPendingShowCallbacks.get(i).onShown();
            } catch (RemoteException e) {
            }
        }
        this.mPendingShowCallbacks.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyPendingShowCallbacksFailedLocked() {
        for (int i = 0; i < this.mPendingShowCallbacks.size(); i++) {
            try {
                this.mPendingShowCallbacks.get(i).onFailed();
            } catch (RemoteException e) {
            }
        }
        this.mPendingShowCallbacks.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startListeningVisibleActivityChangedLocked() {
        this.mListeningVisibleActivity = true;
        this.mVisibleActivityInfos.clear();
        this.mScheduledExecutorService.execute(new Runnable() { // from class: com.android.server.voiceinteraction.VoiceInteractionSessionConnection$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                VoiceInteractionSessionConnection.this.m7610x803c20c2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startListeningVisibleActivityChangedLocked$0$com-android-server-voiceinteraction-VoiceInteractionSessionConnection  reason: not valid java name */
    public /* synthetic */ void m7610x803c20c2() {
        synchronized (this.mLock) {
            updateVisibleActivitiesLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopListeningVisibleActivityChangedLocked() {
        this.mListeningVisibleActivity = false;
        this.mVisibleActivityInfos.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityEventChangedLocked() {
        if (!this.mListeningVisibleActivity) {
            return;
        }
        this.mScheduledExecutorService.execute(new Runnable() { // from class: com.android.server.voiceinteraction.VoiceInteractionSessionConnection$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                VoiceInteractionSessionConnection.this.m7609xb7df2a69();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyActivityEventChangedLocked$1$com-android-server-voiceinteraction-VoiceInteractionSessionConnection  reason: not valid java name */
    public /* synthetic */ void m7609xb7df2a69() {
        synchronized (this.mLock) {
            updateVisibleActivitiesLocked();
        }
    }

    private List<VisibleActivityInfo> getVisibleActivityInfosLocked() {
        List<ActivityAssistInfo> allVisibleActivities = ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).getTopVisibleActivities();
        if (allVisibleActivities == null || allVisibleActivities.isEmpty()) {
            Slog.w(TAG, "no visible activity");
            return null;
        }
        int count = allVisibleActivities.size();
        List<VisibleActivityInfo> visibleActivityInfos = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ActivityAssistInfo info = allVisibleActivities.get(i);
            visibleActivityInfos.add(new VisibleActivityInfo(info.getTaskId(), info.getAssistToken()));
        }
        return visibleActivityInfos;
    }

    private void updateVisibleActivitiesLocked() {
        if (this.mSession == null || !this.mShown || !this.mListeningVisibleActivity || this.mCanceled) {
            return;
        }
        List<VisibleActivityInfo> newVisibleActivityInfos = getVisibleActivityInfosLocked();
        if (newVisibleActivityInfos == null || newVisibleActivityInfos.isEmpty()) {
            updateVisibleActivitiesChangedLocked(this.mVisibleActivityInfos, 2);
            this.mVisibleActivityInfos.clear();
        } else if (this.mVisibleActivityInfos.isEmpty()) {
            updateVisibleActivitiesChangedLocked(newVisibleActivityInfos, 1);
            this.mVisibleActivityInfos.addAll(newVisibleActivityInfos);
        } else {
            List<VisibleActivityInfo> addedActivities = new ArrayList<>();
            List<VisibleActivityInfo> removedActivities = new ArrayList<>();
            removedActivities.addAll(this.mVisibleActivityInfos);
            for (int i = 0; i < newVisibleActivityInfos.size(); i++) {
                VisibleActivityInfo candidateVisibleActivityInfo = newVisibleActivityInfos.get(i);
                if (!removedActivities.isEmpty() && removedActivities.contains(candidateVisibleActivityInfo)) {
                    removedActivities.remove(candidateVisibleActivityInfo);
                } else {
                    addedActivities.add(candidateVisibleActivityInfo);
                }
            }
            if (!addedActivities.isEmpty()) {
                updateVisibleActivitiesChangedLocked(addedActivities, 1);
            }
            if (!removedActivities.isEmpty()) {
                updateVisibleActivitiesChangedLocked(removedActivities, 2);
            }
            this.mVisibleActivityInfos.clear();
            this.mVisibleActivityInfos.addAll(newVisibleActivityInfos);
        }
    }

    private void updateVisibleActivitiesChangedLocked(List<VisibleActivityInfo> visibleActivityInfos, int type) {
        if (visibleActivityInfos == null || visibleActivityInfos.isEmpty() || this.mSession == null) {
            return;
        }
        for (int i = 0; i < visibleActivityInfos.size(); i++) {
            try {
                this.mSession.updateVisibleActivityInfo(visibleActivityInfos.get(i), type);
            } catch (RemoteException e) {
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeFromLowPowerStandbyAllowlist() {
        synchronized (this.mLock) {
            if (this.mLowPowerStandbyAllowlisted) {
                this.mFgHandler.removeCallbacks(this.mRemoveFromLowPowerStandbyAllowlistRunnable);
                this.mLowPowerStandbyControllerInternal.removeFromAllowlist(this.mCallingUid);
                this.mLowPowerStandbyAllowlisted = false;
            }
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (this.mLock) {
            IVoiceInteractionSessionService asInterface = IVoiceInteractionSessionService.Stub.asInterface(service);
            this.mService = asInterface;
            if (!this.mCanceled) {
                try {
                    asInterface.newSession(this.mToken, this.mShowArgs, this.mShowFlags);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed adding window token", e);
                }
            }
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceDisconnected(ComponentName name) {
        this.mCallback.sessionConnectionGone(this);
        synchronized (this.mLock) {
            this.mService = null;
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mToken=");
        pw.println(this.mToken);
        pw.print(prefix);
        pw.print("mShown=");
        pw.println(this.mShown);
        pw.print(prefix);
        pw.print("mShowArgs=");
        pw.println(this.mShowArgs);
        pw.print(prefix);
        pw.print("mShowFlags=0x");
        pw.println(Integer.toHexString(this.mShowFlags));
        pw.print(prefix);
        pw.print("mBound=");
        pw.println(this.mBound);
        if (this.mBound) {
            pw.print(prefix);
            pw.print("mService=");
            pw.println(this.mService);
            pw.print(prefix);
            pw.print("mSession=");
            pw.println(this.mSession);
            pw.print(prefix);
            pw.print("mInteractor=");
            pw.println(this.mInteractor);
        }
        this.mAssistDataRequester.dump(prefix, pw);
    }
}
