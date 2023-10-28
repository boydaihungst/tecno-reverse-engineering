package com.android.server.voiceinteraction;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.app.ApplicationExitInfo;
import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.app.ProfilerInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ServiceInfo;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.permission.Identity;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SharedMemory;
import android.os.UserHandle;
import android.service.voice.HotwordDetector;
import android.service.voice.IMicrophoneHotwordDetectionVoiceInteractionCallback;
import android.service.voice.IVoiceInteractionService;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.VoiceInteractionServiceInfo;
import android.system.OsConstants;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.view.IWindowManager;
import com.android.internal.app.IHotwordRecognitionStatusCallback;
import com.android.internal.app.IVoiceActionCheckCallback;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.util.function.HexConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.voiceinteraction.HotwordDetectionConnection;
import com.android.server.voiceinteraction.VoiceInteractionManagerService;
import com.android.server.voiceinteraction.VoiceInteractionSessionConnection;
import com.android.server.wm.ActivityAssistInfo;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class VoiceInteractionManagerServiceImpl implements VoiceInteractionSessionConnection.Callback {
    static final String CLOSE_REASON_VOICE_INTERACTION = "voiceinteraction";
    static final boolean DEBUG = false;
    private static final long REQUEST_DIRECT_ACTIONS_RETRY_TIME_MS = 200;
    static final String TAG = "VoiceInteractionServiceManager";
    VoiceInteractionSessionConnection mActiveSession;
    final IActivityManager mAm;
    final IActivityTaskManager mAtm;
    boolean mBound = false;
    final BroadcastReceiver mBroadcastReceiver;
    final ComponentName mComponent;
    final ServiceConnection mConnection;
    final Context mContext;
    int mDetectorType;
    final Handler mDirectActionsHandler;
    int mDisabledShowContext;
    final Handler mHandler;
    final ComponentName mHotwordDetectionComponentName;
    volatile HotwordDetectionConnection mHotwordDetectionConnection;
    final IWindowManager mIWindowManager;
    final VoiceInteractionServiceInfo mInfo;
    final PackageManagerInternal mPackageManagerInternal;
    IVoiceInteractionService mService;
    final VoiceInteractionManagerService.VoiceInteractionManagerServiceStub mServiceStub;
    final ComponentName mSessionComponentName;
    final int mUser;
    final boolean mValid;

    /* JADX INFO: Access modifiers changed from: package-private */
    public VoiceInteractionManagerServiceImpl(Context context, Handler handler, VoiceInteractionManagerService.VoiceInteractionManagerServiceStub stub, int userHandle, ComponentName service) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerServiceImpl.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                    String reason = intent.getStringExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY);
                    if (!VoiceInteractionManagerServiceImpl.CLOSE_REASON_VOICE_INTERACTION.equals(reason) && !"dream".equals(reason)) {
                        synchronized (VoiceInteractionManagerServiceImpl.this.mServiceStub) {
                            if (VoiceInteractionManagerServiceImpl.this.mActiveSession != null && VoiceInteractionManagerServiceImpl.this.mActiveSession.mSession != null) {
                                try {
                                    VoiceInteractionManagerServiceImpl.this.mActiveSession.mSession.closeSystemDialogs();
                                } catch (RemoteException e) {
                                }
                            }
                        }
                    }
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        this.mConnection = new ServiceConnection() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerServiceImpl.2
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service2) {
                synchronized (VoiceInteractionManagerServiceImpl.this.mServiceStub) {
                    VoiceInteractionManagerServiceImpl.this.mService = IVoiceInteractionService.Stub.asInterface(service2);
                    try {
                        VoiceInteractionManagerServiceImpl.this.mService.ready();
                    } catch (RemoteException e) {
                    }
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                synchronized (VoiceInteractionManagerServiceImpl.this.mServiceStub) {
                    VoiceInteractionManagerServiceImpl.this.mService = null;
                    VoiceInteractionManagerServiceImpl.this.resetHotwordDetectionConnectionLocked();
                }
            }

            @Override // android.content.ServiceConnection
            public void onBindingDied(ComponentName name) {
                Slog.d(VoiceInteractionManagerServiceImpl.TAG, "onBindingDied to " + name);
                String packageName = name.getPackageName();
                ParceledListSlice<ApplicationExitInfo> plistSlice = null;
                try {
                    plistSlice = VoiceInteractionManagerServiceImpl.this.mAm.getHistoricalProcessExitReasons(packageName, 0, 1, VoiceInteractionManagerServiceImpl.this.mUser);
                } catch (RemoteException e) {
                }
                if (plistSlice == null) {
                    return;
                }
                List<ApplicationExitInfo> list = plistSlice.getList();
                if (list.isEmpty()) {
                    return;
                }
                ApplicationExitInfo info = list.get(0);
                if (info.getReason() == 10 && info.getSubReason() == 23) {
                    VoiceInteractionManagerServiceImpl.this.mServiceStub.handleUserStop(packageName, VoiceInteractionManagerServiceImpl.this.mUser);
                }
            }
        };
        this.mContext = context;
        this.mHandler = handler;
        this.mDirectActionsHandler = new Handler(true);
        this.mServiceStub = stub;
        this.mUser = userHandle;
        this.mComponent = service;
        this.mAm = ActivityManager.getService();
        this.mAtm = ActivityTaskManager.getService();
        this.mPackageManagerInternal = (PackageManagerInternal) Objects.requireNonNull((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
        try {
            VoiceInteractionServiceInfo info = new VoiceInteractionServiceInfo(context.getPackageManager(), service, userHandle);
            this.mInfo = info;
            if (info.getParseError() != null) {
                Slog.w(TAG, "Bad voice interaction service: " + info.getParseError());
                this.mSessionComponentName = null;
                this.mHotwordDetectionComponentName = null;
                this.mIWindowManager = null;
                this.mValid = false;
                return;
            }
            this.mValid = true;
            this.mSessionComponentName = new ComponentName(service.getPackageName(), info.getSessionService());
            String hotwordDetectionServiceName = info.getHotwordDetectionService();
            this.mHotwordDetectionComponentName = hotwordDetectionServiceName != null ? new ComponentName(service.getPackageName(), hotwordDetectionServiceName) : null;
            this.mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            context.registerReceiver(broadcastReceiver, filter, null, handler, 2);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Voice interaction service not found: " + service, e);
            this.mInfo = null;
            this.mSessionComponentName = null;
            this.mHotwordDetectionComponentName = null;
            this.mIWindowManager = null;
            this.mValid = false;
        }
    }

    public void grantImplicitAccessLocked(int grantRecipientUid, Intent intent) {
        int grantRecipientAppId = UserHandle.getAppId(grantRecipientUid);
        int grantRecipientUserId = UserHandle.getUserId(grantRecipientUid);
        int voiceInteractionUid = this.mInfo.getServiceInfo().applicationInfo.uid;
        this.mPackageManagerInternal.grantImplicitAccess(grantRecipientUserId, intent, grantRecipientAppId, voiceInteractionUid, true);
    }

    public boolean showSessionLocked(Bundle args, int flags, IVoiceInteractionSessionShowCallback showCallback, IBinder activityToken) {
        List<ActivityAssistInfo> visibleActivities;
        if (this.mActiveSession == null) {
            this.mActiveSession = new VoiceInteractionSessionConnection(this.mServiceStub, this.mSessionComponentName, this.mUser, this.mContext, this, this.mInfo.getServiceInfo().applicationInfo.uid, this.mHandler);
        }
        List<ActivityAssistInfo> allVisibleActivities = ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).getTopVisibleActivities();
        if (activityToken != null) {
            visibleActivities = new ArrayList<>();
            int activitiesCount = allVisibleActivities.size();
            int i = 0;
            while (true) {
                if (i >= activitiesCount) {
                    break;
                }
                ActivityAssistInfo info = allVisibleActivities.get(i);
                if (info.getActivityToken() != activityToken) {
                    i++;
                } else {
                    visibleActivities.add(info);
                    break;
                }
            }
        } else {
            visibleActivities = allVisibleActivities;
        }
        return this.mActiveSession.showLocked(args, flags, this.mDisabledShowContext, showCallback, visibleActivities);
    }

    public void getActiveServiceSupportedActions(List<String> commands, IVoiceActionCheckCallback callback) {
        IVoiceInteractionService iVoiceInteractionService = this.mService;
        if (iVoiceInteractionService == null) {
            Slog.w(TAG, "Not bound to voice interaction service " + this.mComponent);
            try {
                callback.onComplete((List) null);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        try {
            iVoiceInteractionService.getActiveServiceSupportedActions(commands, callback);
        } catch (RemoteException e2) {
            Slog.w(TAG, "RemoteException while calling getActiveServiceSupportedActions", e2);
        }
    }

    public boolean hideSessionLocked() {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
        if (voiceInteractionSessionConnection != null) {
            return voiceInteractionSessionConnection.hideLocked();
        }
        return false;
    }

    public boolean deliverNewSessionLocked(IBinder token, IVoiceInteractionSession session, IVoiceInteractor interactor) {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
        if (voiceInteractionSessionConnection == null || token != voiceInteractionSessionConnection.mToken) {
            Slog.w(TAG, "deliverNewSession does not match active session");
            return false;
        }
        this.mActiveSession.deliverNewSessionLocked(session, interactor);
        return true;
    }

    public int startVoiceActivityLocked(String callingFeatureId, int callingPid, int callingUid, IBinder token, Intent intent, String resolvedType) {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection;
        try {
            voiceInteractionSessionConnection = this.mActiveSession;
            try {
            } catch (RemoteException e) {
                e = e;
                throw new IllegalStateException("Unexpected remote error", e);
            }
        } catch (RemoteException e2) {
            e = e2;
        }
        if (voiceInteractionSessionConnection != null) {
            if (token == voiceInteractionSessionConnection.mToken) {
                try {
                    if (!this.mActiveSession.mShown) {
                        try {
                            Slog.w(TAG, "startVoiceActivity not allowed on hidden session");
                            return -100;
                        } catch (RemoteException e3) {
                            e = e3;
                        }
                    } else {
                        Intent intent2 = new Intent(intent);
                        try {
                            intent2.addCategory("android.intent.category.VOICE");
                            intent2.addFlags(AudioFormat.MP2);
                            return this.mAtm.startVoiceActivity(this.mComponent.getPackageName(), callingFeatureId, callingPid, callingUid, intent2, resolvedType, this.mActiveSession.mSession, this.mActiveSession.mInteractor, 0, (ProfilerInfo) null, (Bundle) null, this.mUser);
                        } catch (RemoteException e4) {
                            e = e4;
                        }
                    }
                } catch (RemoteException e5) {
                    e = e5;
                    throw new IllegalStateException("Unexpected remote error", e);
                }
                throw new IllegalStateException("Unexpected remote error", e);
            }
        }
        Slog.w(TAG, "startVoiceActivity does not match active session");
        return -99;
    }

    public int startAssistantActivityLocked(String callingFeatureId, int callingPid, int callingUid, IBinder token, Intent intent, String resolvedType) {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection;
        try {
            voiceInteractionSessionConnection = this.mActiveSession;
            try {
            } catch (RemoteException e) {
                e = e;
                throw new IllegalStateException("Unexpected remote error", e);
            }
        } catch (RemoteException e2) {
            e = e2;
        }
        if (voiceInteractionSessionConnection != null) {
            if (token == voiceInteractionSessionConnection.mToken) {
                try {
                    if (!this.mActiveSession.mShown) {
                        try {
                            Slog.w(TAG, "startAssistantActivity not allowed on hidden session");
                            return -90;
                        } catch (RemoteException e3) {
                            e = e3;
                        }
                    } else {
                        Intent intent2 = new Intent(intent);
                        try {
                            intent2.addFlags(268435456);
                            ActivityOptions options = ActivityOptions.makeBasic();
                            options.setLaunchActivityType(4);
                            return this.mAtm.startAssistantActivity(this.mComponent.getPackageName(), callingFeatureId, callingPid, callingUid, intent2, resolvedType, options.toBundle(), this.mUser);
                        } catch (RemoteException e4) {
                            e = e4;
                        }
                    }
                } catch (RemoteException e5) {
                    e = e5;
                    throw new IllegalStateException("Unexpected remote error", e);
                }
                throw new IllegalStateException("Unexpected remote error", e);
            }
        }
        Slog.w(TAG, "startAssistantActivity does not match active session");
        return -89;
    }

    public void requestDirectActionsLocked(IBinder token, int taskId, IBinder assistToken, RemoteCallback cancellationCallback, RemoteCallback callback) {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
        if (voiceInteractionSessionConnection == null || token != voiceInteractionSessionConnection.mToken) {
            Slog.w(TAG, "requestDirectActionsLocked does not match active session");
            callback.sendResult((Bundle) null);
            return;
        }
        ActivityTaskManagerInternal.ActivityTokens tokens = ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).getAttachedNonFinishingActivityForTask(taskId, null);
        if (tokens == null || tokens.getAssistToken() != assistToken) {
            Slog.w(TAG, "Unknown activity to query for direct actions");
            this.mDirectActionsHandler.sendMessageDelayed(PooledLambda.obtainMessage(new HexConsumer() { // from class: com.android.server.voiceinteraction.VoiceInteractionManagerServiceImpl$$ExternalSyntheticLambda0
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
                    ((VoiceInteractionManagerServiceImpl) obj).retryRequestDirectActions((IBinder) obj2, ((Integer) obj3).intValue(), (IBinder) obj4, (RemoteCallback) obj5, (RemoteCallback) obj6);
                }
            }, this, token, Integer.valueOf(taskId), assistToken, cancellationCallback, callback), REQUEST_DIRECT_ACTIONS_RETRY_TIME_MS);
            return;
        }
        grantImplicitAccessLocked(tokens.getUid(), null);
        try {
            tokens.getApplicationThread().requestDirectActions(tokens.getActivityToken(), this.mActiveSession.mInteractor, cancellationCallback, callback);
        } catch (RemoteException e) {
            Slog.w("Unexpected remote error", e);
            callback.sendResult((Bundle) null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void retryRequestDirectActions(IBinder token, int taskId, IBinder assistToken, RemoteCallback cancellationCallback, RemoteCallback callback) {
        synchronized (this.mServiceStub) {
            VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
            if (voiceInteractionSessionConnection != null && token == voiceInteractionSessionConnection.mToken) {
                ActivityTaskManagerInternal.ActivityTokens tokens = ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).getAttachedNonFinishingActivityForTask(taskId, null);
                if (tokens == null || tokens.getAssistToken() != assistToken) {
                    Slog.w(TAG, "Unknown activity to query for direct actions during retrying");
                    callback.sendResult((Bundle) null);
                } else {
                    try {
                        tokens.getApplicationThread().requestDirectActions(tokens.getActivityToken(), this.mActiveSession.mInteractor, cancellationCallback, callback);
                    } catch (RemoteException e) {
                        Slog.w("Unexpected remote error", e);
                        callback.sendResult((Bundle) null);
                    }
                }
                return;
            }
            Slog.w(TAG, "retryRequestDirectActions does not match active session");
            callback.sendResult((Bundle) null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void performDirectActionLocked(IBinder token, String actionId, Bundle arguments, int taskId, IBinder assistToken, RemoteCallback cancellationCallback, RemoteCallback resultCallback) {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
        if (voiceInteractionSessionConnection != null && token == voiceInteractionSessionConnection.mToken) {
            ActivityTaskManagerInternal.ActivityTokens tokens = ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).getAttachedNonFinishingActivityForTask(taskId, null);
            if (tokens != null && tokens.getAssistToken() == assistToken) {
                try {
                    tokens.getApplicationThread().performDirectAction(tokens.getActivityToken(), actionId, arguments, cancellationCallback, resultCallback);
                    return;
                } catch (RemoteException e) {
                    Slog.w("Unexpected remote error", e);
                    resultCallback.sendResult((Bundle) null);
                    return;
                }
            }
            Slog.w(TAG, "Unknown activity to perform a direct action");
            resultCallback.sendResult((Bundle) null);
            return;
        }
        Slog.w(TAG, "performDirectActionLocked does not match active session");
        resultCallback.sendResult((Bundle) null);
    }

    public void setKeepAwakeLocked(IBinder token, boolean keepAwake) {
        try {
            VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
            if (voiceInteractionSessionConnection != null && token == voiceInteractionSessionConnection.mToken) {
                this.mAtm.setVoiceKeepAwake(this.mActiveSession.mSession, keepAwake);
                return;
            }
            Slog.w(TAG, "setKeepAwake does not match active session");
        } catch (RemoteException e) {
            throw new IllegalStateException("Unexpected remote error", e);
        }
    }

    public void closeSystemDialogsLocked(IBinder token) {
        try {
            VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
            if (voiceInteractionSessionConnection != null && token == voiceInteractionSessionConnection.mToken) {
                this.mAm.closeSystemDialogs(CLOSE_REASON_VOICE_INTERACTION);
                return;
            }
            Slog.w(TAG, "closeSystemDialogs does not match active session");
        } catch (RemoteException e) {
            throw new IllegalStateException("Unexpected remote error", e);
        }
    }

    public void finishLocked(IBinder token, boolean finishTask) {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
        if (voiceInteractionSessionConnection == null || (!finishTask && token != voiceInteractionSessionConnection.mToken)) {
            Slog.w(TAG, "finish does not match active session");
            return;
        }
        this.mActiveSession.cancelLocked(finishTask);
        this.mActiveSession = null;
    }

    public void setDisabledShowContextLocked(int callingUid, int flags) {
        int activeUid = this.mInfo.getServiceInfo().applicationInfo.uid;
        if (callingUid != activeUid) {
            throw new SecurityException("Calling uid " + callingUid + " does not match active uid " + activeUid);
        }
        this.mDisabledShowContext = flags;
    }

    public int getDisabledShowContextLocked(int callingUid) {
        int activeUid = this.mInfo.getServiceInfo().applicationInfo.uid;
        if (callingUid != activeUid) {
            throw new SecurityException("Calling uid " + callingUid + " does not match active uid " + activeUid);
        }
        return this.mDisabledShowContext;
    }

    public int getUserDisabledShowContextLocked(int callingUid) {
        int activeUid = this.mInfo.getServiceInfo().applicationInfo.uid;
        if (callingUid != activeUid) {
            throw new SecurityException("Calling uid " + callingUid + " does not match active uid " + activeUid);
        }
        VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
        if (voiceInteractionSessionConnection != null) {
            return voiceInteractionSessionConnection.getUserDisabledShowContextLocked();
        }
        return 0;
    }

    public boolean supportsLocalVoiceInteraction() {
        return this.mInfo.getSupportsLocalInteraction();
    }

    public void startListeningVisibleActivityChangedLocked(IBinder token) {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
        if (voiceInteractionSessionConnection == null || token != voiceInteractionSessionConnection.mToken) {
            Slog.w(TAG, "startListeningVisibleActivityChangedLocked does not match active session");
        } else {
            this.mActiveSession.startListeningVisibleActivityChangedLocked();
        }
    }

    public void stopListeningVisibleActivityChangedLocked(IBinder token) {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
        if (voiceInteractionSessionConnection == null || token != voiceInteractionSessionConnection.mToken) {
            Slog.w(TAG, "stopListeningVisibleActivityChangedLocked does not match active session");
        } else {
            this.mActiveSession.stopListeningVisibleActivityChangedLocked();
        }
    }

    public void notifyActivityEventChangedLocked() {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
        if (voiceInteractionSessionConnection == null || !voiceInteractionSessionConnection.mShown) {
            return;
        }
        this.mActiveSession.notifyActivityEventChangedLocked();
    }

    public void updateStateLocked(Identity voiceInteractorIdentity, PersistableBundle options, SharedMemory sharedMemory, IHotwordRecognitionStatusCallback callback, int detectorType) {
        Slog.v(TAG, "updateStateLocked");
        int voiceInteractionServiceUid = this.mInfo.getServiceInfo().applicationInfo.uid;
        ComponentName componentName = this.mHotwordDetectionComponentName;
        if (componentName == null) {
            Slog.w(TAG, "Hotword detection service name not found");
            logDetectorCreateEventIfNeeded(callback, detectorType, false, voiceInteractionServiceUid);
            throw new IllegalStateException("Hotword detection service name not found");
        }
        ServiceInfo hotwordDetectionServiceInfo = getServiceInfoLocked(componentName, this.mUser);
        if (hotwordDetectionServiceInfo == null) {
            Slog.w(TAG, "Hotword detection service info not found");
            logDetectorCreateEventIfNeeded(callback, detectorType, false, voiceInteractionServiceUid);
            throw new IllegalStateException("Hotword detection service info not found");
        } else if (!isIsolatedProcessLocked(hotwordDetectionServiceInfo)) {
            Slog.w(TAG, "Hotword detection service not in isolated process");
            logDetectorCreateEventIfNeeded(callback, detectorType, false, voiceInteractionServiceUid);
            throw new IllegalStateException("Hotword detection service not in isolated process");
        } else if (!"android.permission.BIND_HOTWORD_DETECTION_SERVICE".equals(hotwordDetectionServiceInfo.permission)) {
            Slog.w(TAG, "Hotword detection service does not require permission android.permission.BIND_HOTWORD_DETECTION_SERVICE");
            logDetectorCreateEventIfNeeded(callback, detectorType, false, voiceInteractionServiceUid);
            throw new SecurityException("Hotword detection service does not require permission android.permission.BIND_HOTWORD_DETECTION_SERVICE");
        } else if (this.mContext.getPackageManager().checkPermission("android.permission.BIND_HOTWORD_DETECTION_SERVICE", this.mInfo.getServiceInfo().packageName) == 0) {
            Slog.w(TAG, "Voice interaction service should not hold permission android.permission.BIND_HOTWORD_DETECTION_SERVICE");
            logDetectorCreateEventIfNeeded(callback, detectorType, false, voiceInteractionServiceUid);
            throw new SecurityException("Voice interaction service should not hold permission android.permission.BIND_HOTWORD_DETECTION_SERVICE");
        } else if (sharedMemory != null && !sharedMemory.setProtect(OsConstants.PROT_READ)) {
            Slog.w(TAG, "Can't set sharedMemory to be read-only");
            logDetectorCreateEventIfNeeded(callback, detectorType, false, voiceInteractionServiceUid);
            throw new IllegalStateException("Can't set sharedMemory to be read-only");
        } else {
            this.mDetectorType = detectorType;
            logDetectorCreateEventIfNeeded(callback, detectorType, true, voiceInteractionServiceUid);
            if (this.mHotwordDetectionConnection == null) {
                this.mHotwordDetectionConnection = new HotwordDetectionConnection(this.mServiceStub, this.mContext, this.mInfo.getServiceInfo().applicationInfo.uid, voiceInteractorIdentity, this.mHotwordDetectionComponentName, this.mUser, false, options, sharedMemory, callback, detectorType);
            } else {
                this.mHotwordDetectionConnection.updateStateLocked(options, sharedMemory);
            }
        }
    }

    private void logDetectorCreateEventIfNeeded(IHotwordRecognitionStatusCallback callback, int detectorType, boolean isCreated, int voiceInteractionServiceUid) {
        if (callback != null) {
            HotwordMetricsLogger.writeDetectorCreateEvent(detectorType, true, voiceInteractionServiceUid);
        }
    }

    public void shutdownHotwordDetectionServiceLocked() {
        if (this.mHotwordDetectionConnection == null) {
            Slog.w(TAG, "shutdown, but no hotword detection connection");
            return;
        }
        this.mHotwordDetectionConnection.cancelLocked();
        this.mHotwordDetectionConnection = null;
    }

    public void startListeningFromMicLocked(android.media.AudioFormat audioFormat, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) {
        if (this.mHotwordDetectionConnection == null) {
            return;
        }
        this.mHotwordDetectionConnection.startListeningFromMic(audioFormat, callback);
    }

    public void startListeningFromExternalSourceLocked(ParcelFileDescriptor audioStream, android.media.AudioFormat audioFormat, PersistableBundle options, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) {
        if (this.mHotwordDetectionConnection == null) {
            return;
        }
        if (audioStream == null) {
            Slog.w(TAG, "External source is null for hotword detector");
            throw new IllegalStateException("External source is null for hotword detector");
        } else {
            this.mHotwordDetectionConnection.startListeningFromExternalSource(audioStream, audioFormat, options, callback);
        }
    }

    public void stopListeningFromMicLocked() {
        if (this.mHotwordDetectionConnection == null) {
            Slog.w(TAG, "stopListeningFromMic() called but connection isn't established");
        } else {
            this.mHotwordDetectionConnection.stopListening();
        }
    }

    public void triggerHardwareRecognitionEventForTestLocked(SoundTrigger.KeyphraseRecognitionEvent event, IHotwordRecognitionStatusCallback callback) {
        if (this.mHotwordDetectionConnection == null) {
            Slog.w(TAG, "triggerHardwareRecognitionEventForTestLocked() called but connection isn't established");
        } else {
            this.mHotwordDetectionConnection.triggerHardwareRecognitionEventForTestLocked(event, callback);
        }
    }

    public IRecognitionStatusCallback createSoundTriggerCallbackLocked(IHotwordRecognitionStatusCallback callback) {
        return new HotwordDetectionConnection.SoundTriggerCallback(callback, this.mHotwordDetectionConnection);
    }

    private static ServiceInfo getServiceInfoLocked(ComponentName componentName, int userHandle) {
        try {
            return AppGlobals.getPackageManager().getServiceInfo(componentName, 786560L, userHandle);
        } catch (RemoteException e) {
            return null;
        }
    }

    boolean isIsolatedProcessLocked(ServiceInfo serviceInfo) {
        return (serviceInfo.flags & 2) != 0 && (serviceInfo.flags & 4) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceRestartHotwordDetector() {
        if (this.mHotwordDetectionConnection == null) {
            Slog.w(TAG, "Failed to force-restart hotword detection: no hotword detection active");
        } else {
            this.mHotwordDetectionConnection.forceRestart();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDebugHotwordLoggingLocked(boolean logging) {
        if (this.mHotwordDetectionConnection == null) {
            Slog.w(TAG, "Failed to set temporary debug logging: no hotword detection active");
        } else {
            this.mHotwordDetectionConnection.setDebugHotwordLoggingLocked(logging);
        }
    }

    void resetHotwordDetectionConnectionLocked() {
        if (this.mHotwordDetectionConnection == null) {
            return;
        }
        this.mHotwordDetectionConnection.cancelLocked();
        this.mHotwordDetectionConnection = null;
    }

    public void dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!this.mValid) {
            pw.print("  NOT VALID: ");
            VoiceInteractionServiceInfo voiceInteractionServiceInfo = this.mInfo;
            if (voiceInteractionServiceInfo == null) {
                pw.println("no info");
                return;
            } else {
                pw.println(voiceInteractionServiceInfo.getParseError());
                return;
            }
        }
        pw.print("  mUser=");
        pw.println(this.mUser);
        pw.print("  mComponent=");
        pw.println(this.mComponent.flattenToShortString());
        pw.print("  Session service=");
        pw.println(this.mInfo.getSessionService());
        pw.println("  Service info:");
        this.mInfo.getServiceInfo().dump(new PrintWriterPrinter(pw), "    ");
        pw.print("  Recognition service=");
        pw.println(this.mInfo.getRecognitionService());
        pw.print("  Hotword detection service=");
        pw.println(this.mInfo.getHotwordDetectionService());
        pw.print("  Settings activity=");
        pw.println(this.mInfo.getSettingsActivity());
        pw.print("  Supports assist=");
        pw.println(this.mInfo.getSupportsAssist());
        pw.print("  Supports launch from keyguard=");
        pw.println(this.mInfo.getSupportsLaunchFromKeyguard());
        if (this.mDisabledShowContext != 0) {
            pw.print("  mDisabledShowContext=");
            pw.println(Integer.toHexString(this.mDisabledShowContext));
        }
        pw.print("  mBound=");
        pw.print(this.mBound);
        pw.print(" mService=");
        pw.println(this.mService);
        pw.print("  mDetectorType=");
        pw.println(HotwordDetector.detectorTypeToString(this.mDetectorType));
        if (this.mHotwordDetectionConnection != null) {
            pw.println("  Hotword detection connection:");
            this.mHotwordDetectionConnection.dump("    ", pw);
        }
        if (this.mActiveSession != null) {
            pw.println("  Active session:");
            this.mActiveSession.dump("    ", pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startLocked() {
        Intent intent = new Intent("android.service.voice.VoiceInteractionService");
        intent.setComponent(this.mComponent);
        boolean bindServiceAsUser = this.mContext.bindServiceAsUser(intent, this.mConnection, 68161537, new UserHandle(this.mUser));
        this.mBound = bindServiceAsUser;
        if (!bindServiceAsUser) {
            Slog.w(TAG, "Failed binding to voice interaction service " + this.mComponent);
        }
    }

    public void launchVoiceAssistFromKeyguard() {
        IVoiceInteractionService iVoiceInteractionService = this.mService;
        if (iVoiceInteractionService == null) {
            Slog.w(TAG, "Not bound to voice interaction service " + this.mComponent);
            return;
        }
        try {
            iVoiceInteractionService.launchVoiceAssistFromKeyguard();
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException while calling launchVoiceAssistFromKeyguard", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void shutdownLocked() {
        VoiceInteractionSessionConnection voiceInteractionSessionConnection = this.mActiveSession;
        if (voiceInteractionSessionConnection != null) {
            voiceInteractionSessionConnection.cancelLocked(false);
            this.mActiveSession = null;
        }
        try {
            IVoiceInteractionService iVoiceInteractionService = this.mService;
            if (iVoiceInteractionService != null) {
                iVoiceInteractionService.shutdown();
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in shutdown", e);
        }
        if (this.mHotwordDetectionConnection != null) {
            this.mHotwordDetectionConnection.cancelLocked();
            this.mHotwordDetectionConnection = null;
        }
        if (this.mBound) {
            this.mContext.unbindService(this.mConnection);
            this.mBound = false;
        }
        if (this.mValid) {
            this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifySoundModelsChangedLocked() {
        IVoiceInteractionService iVoiceInteractionService = this.mService;
        if (iVoiceInteractionService == null) {
            Slog.w(TAG, "Not bound to voice interaction service " + this.mComponent);
            return;
        }
        try {
            iVoiceInteractionService.soundModelsChanged();
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException while calling soundModelsChanged", e);
        }
    }

    @Override // com.android.server.voiceinteraction.VoiceInteractionSessionConnection.Callback
    public void sessionConnectionGone(VoiceInteractionSessionConnection connection) {
        synchronized (this.mServiceStub) {
            finishLocked(connection.mToken, false);
        }
    }

    @Override // com.android.server.voiceinteraction.VoiceInteractionSessionConnection.Callback
    public void onSessionShown(VoiceInteractionSessionConnection connection) {
        this.mServiceStub.onSessionShown();
    }

    @Override // com.android.server.voiceinteraction.VoiceInteractionSessionConnection.Callback
    public void onSessionHidden(VoiceInteractionSessionConnection connection) {
        this.mServiceStub.onSessionHidden();
    }
}
