package com.android.server.clipboard;

import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IUriGrantsManager;
import android.app.KeyguardManager;
import android.app.UriGrantsManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IClipboard;
import android.content.IOnPrimaryClipChangedListener;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.autofill.AutofillManagerInternal;
import android.view.textclassifier.TextClassificationContext;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextClassifierEvent;
import android.view.textclassifier.TextLinks;
import android.widget.Toast;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.FunctionalUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.UiThread;
import com.android.server.companion.virtual.VirtualDeviceManagerInternal;
import com.android.server.contentcapture.ContentCaptureManagerInternal;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.uri.UriGrantsManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class ClipboardService extends SystemService {
    public static final long DEFAULT_CLIPBOARD_TIMEOUT_MILLIS = 3600000;
    private static final int DEFAULT_MAX_CLASSIFICATION_LENGTH = 400;
    private static final boolean IS_EMULATOR = SystemProperties.getBoolean("ro.boot.qemu", false);
    public static final String PROPERTY_AUTO_CLEAR_ENABLED = "auto_clear_enabled";
    public static final String PROPERTY_AUTO_CLEAR_TIMEOUT = "auto_clear_timeout";
    private static final String PROPERTY_MAX_CLASSIFICATION_LENGTH = "max_classification_length";
    private static final String TAG = "ClipboardService";
    private final ActivityManagerInternal mAmInternal;
    private final AppOpsManager mAppOps;
    private final AutofillManagerInternal mAutofillInternal;
    private final SparseArray<PerUserClipboard> mClipboards;
    private final ContentCaptureManagerInternal mContentCaptureInternal;
    private final Consumer<ClipData> mEmulatorClipboardMonitor;
    private final Object mLock;
    private int mMaxClassificationLength;
    private final IBinder mPermissionOwner;
    private final PackageManager mPm;
    private boolean mShowAccessNotifications;
    private final IUriGrantsManager mUgm;
    private final UriGrantsManagerInternal mUgmInternal;
    private final IUserManager mUm;
    private final VirtualDeviceManagerInternal mVdm;
    private final WindowManagerInternal mWm;
    private final Handler mWorkerHandler;

    public ClipboardService(Context context) {
        super(context);
        this.mClipboards = new SparseArray<>();
        this.mShowAccessNotifications = true;
        this.mMaxClassificationLength = 400;
        this.mLock = new Object();
        this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mUgm = UriGrantsManager.getService();
        UriGrantsManagerInternal uriGrantsManagerInternal = (UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class);
        this.mUgmInternal = uriGrantsManagerInternal;
        this.mWm = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mVdm = (VirtualDeviceManagerInternal) LocalServices.getService(VirtualDeviceManagerInternal.class);
        this.mPm = getContext().getPackageManager();
        this.mUm = ServiceManager.getService("user");
        this.mAppOps = (AppOpsManager) getContext().getSystemService("appops");
        this.mContentCaptureInternal = (ContentCaptureManagerInternal) LocalServices.getService(ContentCaptureManagerInternal.class);
        this.mAutofillInternal = (AutofillManagerInternal) LocalServices.getService(AutofillManagerInternal.class);
        IBinder permOwner = uriGrantsManagerInternal.newUriPermissionOwner("clipboard");
        this.mPermissionOwner = permOwner;
        if (IS_EMULATOR) {
            this.mEmulatorClipboardMonitor = new EmulatorClipboardMonitor(new Consumer() { // from class: com.android.server.clipboard.ClipboardService$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ClipboardService.this.m2663lambda$new$0$comandroidserverclipboardClipboardService((ClipData) obj);
                }
            });
        } else {
            this.mEmulatorClipboardMonitor = new Consumer() { // from class: com.android.server.clipboard.ClipboardService$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ClipboardService.lambda$new$1((ClipData) obj);
                }
            };
        }
        updateConfig();
        DeviceConfig.addOnPropertiesChangedListener("clipboard", getContext().getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.clipboard.ClipboardService$$ExternalSyntheticLambda2
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                ClipboardService.this.m2664lambda$new$2$comandroidserverclipboardClipboardService(properties);
            }
        });
        HandlerThread workerThread = new HandlerThread(TAG);
        workerThread.start();
        this.mWorkerHandler = workerThread.getThreadHandler();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-clipboard-ClipboardService  reason: not valid java name */
    public /* synthetic */ void m2663lambda$new$0$comandroidserverclipboardClipboardService(ClipData clip) {
        synchronized (this.mLock) {
            setPrimaryClipInternalLocked(getClipboardLocked(0), clip, 1000, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$new$1(ClipData clip) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$2$com-android-server-clipboard-ClipboardService  reason: not valid java name */
    public /* synthetic */ void m2664lambda$new$2$comandroidserverclipboardClipboardService(DeviceConfig.Properties properties) {
        updateConfig();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("clipboard", new ClipboardImpl());
    }

    @Override // com.android.server.SystemService
    public void onUserStopped(SystemService.TargetUser user) {
        synchronized (this.mLock) {
            this.mClipboards.remove(user.getUserIdentifier());
        }
    }

    private void updateConfig() {
        synchronized (this.mLock) {
            this.mShowAccessNotifications = DeviceConfig.getBoolean("clipboard", "show_access_notifications", true);
            this.mMaxClassificationLength = DeviceConfig.getInt("clipboard", PROPERTY_MAX_CLASSIFICATION_LENGTH, 400);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ListenerInfo {
        final String mPackageName;
        final int mUid;

        ListenerInfo(int uid, String packageName) {
            this.mUid = uid;
            this.mPackageName = packageName;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PerUserClipboard {
        String mPackageName;
        String mPrimaryClipPackage;
        TextClassifier mTextClassifier;
        ClipData primaryClip;
        final int userId;
        final RemoteCallbackList<IOnPrimaryClipChangedListener> primaryClipListeners = new RemoteCallbackList<>();
        int primaryClipUid = 9999;
        final SparseBooleanArray mNotifiedUids = new SparseBooleanArray();
        final SparseBooleanArray mNotifiedTextClassifierUids = new SparseBooleanArray();
        final HashSet<String> activePermissionOwners = new HashSet<>();

        PerUserClipboard(int userId) {
            this.userId = userId;
        }
    }

    private boolean isInternalSysWindowAppWithWindowFocus(String callingPackage) {
        if (this.mPm.checkPermission("android.permission.INTERNAL_SYSTEM_WINDOW", callingPackage) == 0 && this.mWm.isUidFocused(Binder.getCallingUid())) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getIntendingUserId(String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        if (!UserManager.supportsMultipleUsers() || callingUserId == userId) {
            return callingUserId;
        }
        int intendingUserId = this.mAmInternal.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "checkClipboardServiceCallingUser", packageName);
        return intendingUserId;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getIntendingUid(String packageName, int userId) {
        return UserHandle.getUid(getIntendingUserId(packageName, userId), UserHandle.getAppId(Binder.getCallingUid()));
    }

    /* loaded from: classes.dex */
    private class ClipboardImpl extends IClipboard.Stub {
        private final Handler mClipboardClearHandler;

        private ClipboardImpl() {
            this.mClipboardClearHandler = new ClipboardClearHandler(ClipboardService.this.mWorkerHandler.getLooper());
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                return super.onTransact(code, data, reply, flags);
            } catch (RuntimeException e) {
                if (!(e instanceof SecurityException)) {
                    Slog.wtf("clipboard", "Exception: ", e);
                }
                throw e;
            }
        }

        public void setPrimaryClip(ClipData clip, String callingPackage, int userId) {
            checkAndSetPrimaryClip(clip, callingPackage, userId, callingPackage);
        }

        public void setPrimaryClipAsPackage(ClipData clip, String callingPackage, int userId, String sourcePackage) {
            ClipboardService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_CLIP_SOURCE", "Requires SET_CLIP_SOURCE permission");
            checkAndSetPrimaryClip(clip, callingPackage, userId, sourcePackage);
        }

        private void checkAndSetPrimaryClip(ClipData clip, String callingPackage, int userId, String sourcePackage) {
            if (clip == null || clip.getItemCount() <= 0) {
                throw new IllegalArgumentException("No items");
            }
            int intendingUid = ClipboardService.this.getIntendingUid(callingPackage, userId);
            int intendingUserId = UserHandle.getUserId(intendingUid);
            if (!ClipboardService.this.clipboardAccessAllowed(30, callingPackage, intendingUid, intendingUserId)) {
                return;
            }
            ClipboardService.this.checkDataOwner(clip, intendingUid);
            synchronized (ClipboardService.this.mLock) {
                PerUserClipboard clipboard = ClipboardService.this.getClipboardLocked(intendingUserId);
                if (clipboard != null) {
                    clipboard.mPackageName = callingPackage;
                }
            }
            synchronized (ClipboardService.this.mLock) {
                scheduleAutoClear(userId, intendingUid);
                ClipboardService.this.setPrimaryClipInternalLocked(clip, intendingUid, sourcePackage);
            }
        }

        private void scheduleAutoClear(int userId, int intendingUid) {
            long oldIdentity = Binder.clearCallingIdentity();
            try {
                if (DeviceConfig.getBoolean("clipboard", ClipboardService.PROPERTY_AUTO_CLEAR_ENABLED, true)) {
                    this.mClipboardClearHandler.removeEqualMessages(101, Integer.valueOf(userId));
                    Message clearMessage = Message.obtain(this.mClipboardClearHandler, 101, userId, intendingUid, Integer.valueOf(userId));
                    this.mClipboardClearHandler.sendMessageDelayed(clearMessage, getTimeoutForAutoClear());
                }
            } finally {
                Binder.restoreCallingIdentity(oldIdentity);
            }
        }

        private long getTimeoutForAutoClear() {
            return DeviceConfig.getLong("clipboard", ClipboardService.PROPERTY_AUTO_CLEAR_TIMEOUT, 3600000L);
        }

        public void clearPrimaryClip(String callingPackage, int userId) {
            int intendingUid = ClipboardService.this.getIntendingUid(callingPackage, userId);
            int intendingUserId = UserHandle.getUserId(intendingUid);
            if (!ClipboardService.this.clipboardAccessAllowed(30, callingPackage, intendingUid, intendingUserId)) {
                return;
            }
            synchronized (ClipboardService.this.mLock) {
                this.mClipboardClearHandler.removeEqualMessages(101, Integer.valueOf(userId));
                ClipboardService.this.setPrimaryClipInternalLocked(null, intendingUid, callingPackage);
            }
        }

        public ClipData getPrimaryClip(String pkg, int userId) {
            ClipData clipData;
            int intendingUid = ClipboardService.this.getIntendingUid(pkg, userId);
            int intendingUserId = UserHandle.getUserId(intendingUid);
            if (!ClipboardService.this.clipboardAccessAllowed(29, pkg, intendingUid, intendingUserId) || ClipboardService.this.isDeviceLocked(intendingUserId)) {
                return null;
            }
            synchronized (ClipboardService.this.mLock) {
                try {
                    try {
                        ClipboardService.this.addActiveOwnerLocked(intendingUid, pkg);
                        PerUserClipboard clipboard = ClipboardService.this.getClipboardLocked(intendingUserId);
                        ClipboardService.this.showAccessNotificationLocked(pkg, intendingUid, intendingUserId, clipboard);
                        ClipboardService.this.notifyTextClassifierLocked(clipboard, pkg, intendingUid);
                        if (clipboard.primaryClip != null) {
                            scheduleAutoClear(userId, intendingUid);
                        }
                        clipData = clipboard.primaryClip;
                    } catch (SecurityException e) {
                        Slog.i(ClipboardService.TAG, "Could not grant permission to primary clip. Clearing clipboard.");
                        ClipboardService.this.setPrimaryClipInternalLocked(null, intendingUid, pkg);
                        return null;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return clipData;
        }

        public ClipDescription getPrimaryClipDescription(String callingPackage, int userId) {
            ClipDescription description;
            int intendingUid = ClipboardService.this.getIntendingUid(callingPackage, userId);
            int intendingUserId = UserHandle.getUserId(intendingUid);
            if (!ClipboardService.this.clipboardAccessAllowed(29, callingPackage, intendingUid, intendingUserId, false) || ClipboardService.this.isDeviceLocked(intendingUserId)) {
                return null;
            }
            synchronized (ClipboardService.this.mLock) {
                PerUserClipboard clipboard = ClipboardService.this.getClipboardLocked(intendingUserId);
                description = clipboard.primaryClip != null ? clipboard.primaryClip.getDescription() : null;
            }
            return description;
        }

        public boolean hasPrimaryClip(String callingPackage, int userId) {
            boolean z;
            int intendingUid = ClipboardService.this.getIntendingUid(callingPackage, userId);
            int intendingUserId = UserHandle.getUserId(intendingUid);
            if (!ClipboardService.this.clipboardAccessAllowed(29, callingPackage, intendingUid, intendingUserId, false) || ClipboardService.this.isDeviceLocked(intendingUserId)) {
                return false;
            }
            synchronized (ClipboardService.this.mLock) {
                z = ClipboardService.this.getClipboardLocked(intendingUserId).primaryClip != null;
            }
            return z;
        }

        public void addPrimaryClipChangedListener(IOnPrimaryClipChangedListener listener, String callingPackage, int userId) {
            int intendingUid = ClipboardService.this.getIntendingUid(callingPackage, userId);
            int intendingUserId = UserHandle.getUserId(intendingUid);
            synchronized (ClipboardService.this.mLock) {
                ClipboardService.this.getClipboardLocked(intendingUserId).primaryClipListeners.register(listener, new ListenerInfo(intendingUid, callingPackage));
            }
        }

        public void removePrimaryClipChangedListener(IOnPrimaryClipChangedListener listener, String callingPackage, int userId) {
            int intendingUserId = ClipboardService.this.getIntendingUserId(callingPackage, userId);
            synchronized (ClipboardService.this.mLock) {
                ClipboardService.this.getClipboardLocked(intendingUserId).primaryClipListeners.unregister(listener);
            }
        }

        public boolean hasClipboardText(String callingPackage, int userId) {
            int intendingUid = ClipboardService.this.getIntendingUid(callingPackage, userId);
            int intendingUserId = UserHandle.getUserId(intendingUid);
            boolean z = false;
            if (!ClipboardService.this.clipboardAccessAllowed(29, callingPackage, intendingUid, intendingUserId, false) || ClipboardService.this.isDeviceLocked(intendingUserId)) {
                return false;
            }
            synchronized (ClipboardService.this.mLock) {
                PerUserClipboard clipboard = ClipboardService.this.getClipboardLocked(intendingUserId);
                if (clipboard.primaryClip != null) {
                    CharSequence text = clipboard.primaryClip.getItemAt(0).getText();
                    if (text != null && text.length() > 0) {
                        z = true;
                    }
                    return z;
                }
                return false;
            }
        }

        public String getPrimaryClipSource(String callingPackage, int userId) {
            ClipboardService.this.getContext().enforceCallingOrSelfPermission("android.permission.SET_CLIP_SOURCE", "Requires SET_CLIP_SOURCE permission");
            int intendingUid = ClipboardService.this.getIntendingUid(callingPackage, userId);
            int intendingUserId = UserHandle.getUserId(intendingUid);
            if (!ClipboardService.this.clipboardAccessAllowed(29, callingPackage, intendingUid, intendingUserId, false) || ClipboardService.this.isDeviceLocked(intendingUserId)) {
                return null;
            }
            synchronized (ClipboardService.this.mLock) {
                PerUserClipboard clipboard = ClipboardService.this.getClipboardLocked(intendingUserId);
                if (clipboard.primaryClip != null) {
                    return clipboard.mPrimaryClipPackage;
                }
                return null;
            }
        }

        /* loaded from: classes.dex */
        private class ClipboardClearHandler extends Handler {
            public static final int MSG_CLEAR = 101;

            ClipboardClearHandler(Looper looper) {
                super(looper);
            }

            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 101:
                        int userId = msg.arg1;
                        int intendingUid = msg.arg2;
                        synchronized (ClipboardService.this.mLock) {
                            if (ClipboardService.this.getClipboardLocked(userId).primaryClip != null) {
                                FrameworkStatsLog.write((int) FrameworkStatsLog.CLIPBOARD_CLEARED, 1);
                                ClipboardService.this.setPrimaryClipInternalLocked(null, intendingUid, null);
                            }
                        }
                        return;
                    default:
                        Slog.wtf(ClipboardService.TAG, "ClipboardClearHandler received unknown message " + msg.what);
                        return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PerUserClipboard getClipboardLocked(int userId) {
        PerUserClipboard puc = this.mClipboards.get(userId);
        if (puc == null) {
            PerUserClipboard puc2 = new PerUserClipboard(userId);
            this.mClipboards.put(userId, puc2);
            return puc2;
        }
        return puc;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [606=4] */
    List<UserInfo> getRelatedProfiles(int userId) {
        long origId = Binder.clearCallingIdentity();
        try {
            List<UserInfo> related = this.mUm.getProfiles(userId, true);
            return related;
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote Exception calling UserManager: " + e);
            return null;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private boolean hasRestriction(String restriction, int userId) {
        try {
            return this.mUm.hasUserRestriction(restriction, userId);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote Exception calling UserManager.getUserRestrictions: ", e);
            return true;
        }
    }

    void setPrimaryClipInternal(ClipData clip, int uid) {
        synchronized (this.mLock) {
            setPrimaryClipInternalLocked(clip, uid, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPrimaryClipInternalLocked(ClipData clip, int uid, String sourcePackage) {
        int size;
        this.mEmulatorClipboardMonitor.accept(clip);
        int userId = UserHandle.getUserId(uid);
        setPrimaryClipInternalLocked(getClipboardLocked(userId), clip, uid, sourcePackage);
        PerUserClipboard clipboard = getClipboardLocked(userId);
        if (clipboard != null) {
            ITranActivityManagerService.Instance().onClipboardChange(clipboard.mPackageName);
        }
        List<UserInfo> related = getRelatedProfiles(userId);
        if (related != null && (size = related.size()) > 1) {
            boolean canCopy = !hasRestriction("no_cross_profile_copy_paste", userId);
            if (!canCopy) {
                clip = null;
            } else if (clip != null) {
                clip = new ClipData(clip);
                for (int i = clip.getItemCount() - 1; i >= 0; i--) {
                    clip.setItemAt(i, new ClipData.Item(clip.getItemAt(i)));
                }
                clip.fixUrisLight(userId);
            }
            for (int i2 = 0; i2 < size; i2++) {
                int id = related.get(i2).id;
                if (id != userId) {
                    boolean canCopyIntoProfile = !hasRestriction("no_sharing_into_profile", id);
                    if (canCopyIntoProfile) {
                        setPrimaryClipInternalNoClassifyLocked(getClipboardLocked(id), clip, uid, sourcePackage);
                    }
                }
            }
        }
    }

    void setPrimaryClipInternal(PerUserClipboard clipboard, ClipData clip, int uid) {
        synchronized (this.mLock) {
            setPrimaryClipInternalLocked(clipboard, clip, uid, null);
        }
    }

    private void setPrimaryClipInternalLocked(PerUserClipboard clipboard, ClipData clip, int uid, String sourcePackage) {
        int userId = UserHandle.getUserId(uid);
        if (clip != null) {
            startClassificationLocked(clip, userId);
        }
        setPrimaryClipInternalNoClassifyLocked(clipboard, clip, uid, sourcePackage);
    }

    private void setPrimaryClipInternalNoClassifyLocked(PerUserClipboard clipboard, ClipData clip, int uid, String sourcePackage) {
        ClipDescription description;
        revokeUris(clipboard);
        clipboard.activePermissionOwners.clear();
        if (clip == null && clipboard.primaryClip == null) {
            return;
        }
        clipboard.primaryClip = clip;
        clipboard.mNotifiedUids.clear();
        clipboard.mNotifiedTextClassifierUids.clear();
        if (clip != null) {
            clipboard.primaryClipUid = uid;
            clipboard.mPrimaryClipPackage = sourcePackage;
        } else {
            clipboard.primaryClipUid = 9999;
            clipboard.mPrimaryClipPackage = null;
        }
        if (clip != null && (description = clip.getDescription()) != null) {
            description.setTimestamp(System.currentTimeMillis());
        }
        sendClipChangedBroadcast(clipboard);
    }

    private void sendClipChangedBroadcast(PerUserClipboard clipboard) {
        long ident = Binder.clearCallingIdentity();
        int n = clipboard.primaryClipListeners.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                ListenerInfo li = (ListenerInfo) clipboard.primaryClipListeners.getBroadcastCookie(i);
                if (clipboardAccessAllowed(29, li.mPackageName, li.mUid, UserHandle.getUserId(li.mUid))) {
                    clipboard.primaryClipListeners.getBroadcastItem(i).dispatchPrimaryClipChanged();
                }
            } catch (RemoteException | SecurityException e) {
            } catch (Throwable th) {
                clipboard.primaryClipListeners.finishBroadcast();
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
        clipboard.primaryClipListeners.finishBroadcast();
        Binder.restoreCallingIdentity(ident);
    }

    private void startClassificationLocked(final ClipData clip, final int userId) {
        final CharSequence text = clip.getItemCount() == 0 ? null : clip.getItemAt(0).getText();
        if (TextUtils.isEmpty(text) || text.length() > this.mMaxClassificationLength) {
            clip.getDescription().setClassificationStatus(2);
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            final TextClassifier classifier = createTextClassificationManagerAsUser(userId).createTextClassificationSession(new TextClassificationContext.Builder(getContext().getPackageName(), "clipboard").build());
            Binder.restoreCallingIdentity(ident);
            if (text.length() > classifier.getMaxGenerateLinksTextLength()) {
                clip.getDescription().setClassificationStatus(2);
            } else {
                this.mWorkerHandler.post(new Runnable() { // from class: com.android.server.clipboard.ClipboardService$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        ClipboardService.this.m2666x8cbfd76(text, clip, classifier, userId);
                    }
                });
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: doClassification */
    public void m2666x8cbfd76(CharSequence text, ClipData clip, TextClassifier classifier, int userId) {
        TextLinks.Request request = new TextLinks.Request.Builder(text).build();
        TextLinks links = classifier.generateLinks(request);
        ArrayMap<String, Float> confidences = new ArrayMap<>();
        for (TextLinks.TextLink link : links.getLinks()) {
            for (int i = 0; i < link.getEntityCount(); i++) {
                String entity = link.getEntity(i);
                float conf = link.getConfidenceScore(entity);
                if (conf > confidences.getOrDefault(entity, Float.valueOf(0.0f)).floatValue()) {
                    confidences.put(entity, Float.valueOf(conf));
                }
            }
        }
        synchronized (this.mLock) {
            try {
                try {
                    PerUserClipboard clipboard = getClipboardLocked(userId);
                    if (clipboard.primaryClip == clip) {
                        applyClassificationAndSendBroadcastLocked(clipboard, confidences, links, classifier);
                        List<UserInfo> related = getRelatedProfiles(userId);
                        if (related != null) {
                            int size = related.size();
                            for (int i2 = 0; i2 < size; i2++) {
                                int id = related.get(i2).id;
                                if (id != userId) {
                                    boolean canCopyIntoProfile = !hasRestriction("no_sharing_into_profile", id);
                                    if (canCopyIntoProfile) {
                                        PerUserClipboard relatedClipboard = getClipboardLocked(id);
                                        if (hasTextLocked(relatedClipboard, text)) {
                                            applyClassificationAndSendBroadcastLocked(relatedClipboard, confidences, links, classifier);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private void applyClassificationAndSendBroadcastLocked(PerUserClipboard clipboard, ArrayMap<String, Float> confidences, TextLinks links, TextClassifier classifier) {
        clipboard.mTextClassifier = classifier;
        clipboard.primaryClip.getDescription().setConfidenceScores(confidences);
        if (!links.getLinks().isEmpty()) {
            clipboard.primaryClip.getItemAt(0).setTextLinks(links);
        }
        sendClipChangedBroadcast(clipboard);
    }

    private boolean hasTextLocked(PerUserClipboard clipboard, CharSequence text) {
        return clipboard.primaryClip != null && clipboard.primaryClip.getItemCount() > 0 && text.equals(clipboard.primaryClip.getItemAt(0).getText());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDeviceLocked(int userId) {
        boolean z;
        long token = Binder.clearCallingIdentity();
        try {
            KeyguardManager keyguardManager = (KeyguardManager) getContext().getSystemService(KeyguardManager.class);
            if (keyguardManager != null) {
                if (keyguardManager.isDeviceLocked(userId)) {
                    z = true;
                    return z;
                }
            }
            z = false;
            return z;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void checkUriOwner(Uri uri, int sourceUid) {
        if (uri == null || !ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(uri.getScheme())) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mUgmInternal.checkGrantUriPermission(sourceUid, null, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)));
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void checkItemOwner(ClipData.Item item, int uid) {
        if (item.getUri() != null) {
            checkUriOwner(item.getUri(), uid);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            checkUriOwner(intent.getData(), uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkDataOwner(ClipData data, int uid) {
        int N = data.getItemCount();
        for (int i = 0; i < N; i++) {
            checkItemOwner(data.getItemAt(i), uid);
        }
    }

    private void grantUriPermission(Uri uri, int sourceUid, String targetPkg, int targetUserId) {
        if (uri == null || !ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(uri.getScheme())) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mUgm.grantUriPermissionFromOwner(this.mPermissionOwner, sourceUid, targetPkg, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)), targetUserId);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
        Binder.restoreCallingIdentity(ident);
    }

    private void grantItemPermission(ClipData.Item item, int sourceUid, String targetPkg, int targetUserId) {
        if (item.getUri() != null) {
            grantUriPermission(item.getUri(), sourceUid, targetPkg, targetUserId);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            grantUriPermission(intent.getData(), sourceUid, targetPkg, targetUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addActiveOwnerLocked(int uid, String pkg) {
        PackageInfo pi;
        IPackageManager pm = AppGlobals.getPackageManager();
        int targetUserHandle = UserHandle.getCallingUserId();
        long oldIdentity = Binder.clearCallingIdentity();
        try {
            pi = pm.getPackageInfo(pkg, 0L, targetUserHandle);
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(oldIdentity);
            throw th;
        }
        if (pi == null) {
            throw new IllegalArgumentException("Unknown package " + pkg);
        }
        if (!UserHandle.isSameApp(pi.applicationInfo.uid, uid)) {
            throw new SecurityException("Calling uid " + uid + " does not own package " + pkg);
        }
        Binder.restoreCallingIdentity(oldIdentity);
        PerUserClipboard clipboard = getClipboardLocked(UserHandle.getUserId(uid));
        if (clipboard.primaryClip != null && !clipboard.activePermissionOwners.contains(pkg)) {
            int N = clipboard.primaryClip.getItemCount();
            for (int i = 0; i < N; i++) {
                grantItemPermission(clipboard.primaryClip.getItemAt(i), clipboard.primaryClipUid, pkg, UserHandle.getUserId(uid));
            }
            clipboard.activePermissionOwners.add(pkg);
        }
    }

    private void revokeUriPermission(Uri uri, int sourceUid) {
        if (uri == null || !ActivityTaskManagerInternal.ASSIST_KEY_CONTENT.equals(uri.getScheme())) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.mUgmInternal.revokeUriPermissionFromOwner(this.mPermissionOwner, ContentProvider.getUriWithoutUserId(uri), 1, ContentProvider.getUserIdFromUri(uri, UserHandle.getUserId(sourceUid)));
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void revokeItemPermission(ClipData.Item item, int sourceUid) {
        if (item.getUri() != null) {
            revokeUriPermission(item.getUri(), sourceUid);
        }
        Intent intent = item.getIntent();
        if (intent != null && intent.getData() != null) {
            revokeUriPermission(intent.getData(), sourceUid);
        }
    }

    private void revokeUris(PerUserClipboard clipboard) {
        if (clipboard.primaryClip == null) {
            return;
        }
        int N = clipboard.primaryClip.getItemCount();
        for (int i = 0; i < N; i++) {
            revokeItemPermission(clipboard.primaryClip.getItemAt(i), clipboard.primaryClipUid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean clipboardAccessAllowed(int op, String callingPackage, int uid, int userId) {
        return clipboardAccessAllowed(op, callingPackage, uid, userId, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean clipboardAccessAllowed(int op, String callingPackage, int uid, int userId, boolean shouldNoteOp) {
        boolean allowed;
        AutofillManagerInternal autofillManagerInternal;
        ContentCaptureManagerInternal contentCaptureManagerInternal;
        int appOpsResult;
        this.mAppOps.checkPackage(uid, callingPackage);
        VirtualDeviceManagerInternal virtualDeviceManagerInternal = this.mVdm;
        if (virtualDeviceManagerInternal != null && virtualDeviceManagerInternal.isAppRunningOnAnyVirtualDevice(uid)) {
            Slog.w(TAG, "Clipboard access denied to " + uid + SliceClientPermissions.SliceAuthority.DELIMITER + callingPackage + " within a virtual device session");
            return false;
        }
        if (this.mPm.checkPermission("android.permission.READ_CLIPBOARD_IN_BACKGROUND", callingPackage) == 0) {
            allowed = true;
        } else {
            allowed = isDefaultIme(userId, callingPackage);
        }
        switch (op) {
            case 29:
                if (!allowed) {
                    allowed = this.mWm.isUidFocused(uid) || isInternalSysWindowAppWithWindowFocus(callingPackage);
                }
                if (!allowed && (contentCaptureManagerInternal = this.mContentCaptureInternal) != null) {
                    allowed = contentCaptureManagerInternal.isContentCaptureServiceForUser(uid, userId);
                }
                if (!allowed && (autofillManagerInternal = this.mAutofillInternal) != null) {
                    allowed = autofillManagerInternal.isAugmentedAutofillServiceForUser(uid, userId);
                    break;
                }
                break;
            case 30:
                allowed = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown clipboard appop " + op);
        }
        if (!allowed) {
            Slog.e(TAG, "Denying clipboard access to " + callingPackage + ", application is not in focus nor is it a system service for user " + userId);
            return false;
        }
        if (shouldNoteOp) {
            appOpsResult = this.mAppOps.noteOp(op, uid, callingPackage);
        } else {
            appOpsResult = this.mAppOps.checkOp(op, uid, callingPackage);
        }
        return appOpsResult == 0;
    }

    private boolean isDefaultIme(int userId, String packageName) {
        String defaultIme = Settings.Secure.getStringForUser(getContext().getContentResolver(), "default_input_method", userId);
        if (!TextUtils.isEmpty(defaultIme)) {
            String imePkg = ComponentName.unflattenFromString(defaultIme).getPackageName();
            return imePkg.equals(packageName);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showAccessNotificationLocked(final String callingPackage, int uid, final int userId, PerUserClipboard clipboard) {
        if (clipboard.primaryClip == null || Settings.Secure.getInt(getContext().getContentResolver(), "clipboard_show_access_notifications", this.mShowAccessNotifications ? 1 : 0) == 0 || UserHandle.isSameApp(uid, clipboard.primaryClipUid) || isDefaultIme(userId, callingPackage)) {
            return;
        }
        ContentCaptureManagerInternal contentCaptureManagerInternal = this.mContentCaptureInternal;
        if (contentCaptureManagerInternal != null && contentCaptureManagerInternal.isContentCaptureServiceForUser(uid, userId)) {
            return;
        }
        AutofillManagerInternal autofillManagerInternal = this.mAutofillInternal;
        if ((autofillManagerInternal != null && autofillManagerInternal.isAugmentedAutofillServiceForUser(uid, userId)) || this.mPm.checkPermission("android.permission.SUPPRESS_CLIPBOARD_ACCESS_NOTIFICATION", callingPackage) == 0 || TextUtils.equals(callingPackage, "com.transsion.connectx.mirror.source") || clipboard.mNotifiedUids.get(uid)) {
            return;
        }
        clipboard.mNotifiedUids.put(uid, true);
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.clipboard.ClipboardService$$ExternalSyntheticLambda5
            public final void runOrThrow() {
                ClipboardService.this.m2665xb1048075(callingPackage, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showAccessNotificationLocked$4$com-android-server-clipboard-ClipboardService  reason: not valid java name */
    public /* synthetic */ void m2665xb1048075(String callingPackage, int userId) throws Exception {
        try {
            PackageManager packageManager = this.mPm;
            CharSequence callingAppLabel = packageManager.getApplicationLabel(packageManager.getApplicationInfoAsUser(callingPackage, 0, userId));
            String message = getContext().getString(17040958, callingAppLabel);
            Slog.i(TAG, message);
            Toast.makeText(getContext(), UiThread.get().getLooper(), message, 0).show();
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private static boolean isText(ClipData data) {
        if (data.getItemCount() > 1) {
            return false;
        }
        ClipData.Item item = data.getItemAt(0);
        return !TextUtils.isEmpty(item.getText()) && item.getUri() == null && item.getIntent() == null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyTextClassifierLocked(final PerUserClipboard clipboard, final String callingPackage, int callingUid) {
        final TextClassifier textClassifier;
        if (clipboard.primaryClip == null) {
            return;
        }
        ClipData.Item item = clipboard.primaryClip.getItemAt(0);
        if (item == null || !isText(clipboard.primaryClip) || (textClassifier = clipboard.mTextClassifier) == null || !this.mWm.isUidFocused(callingUid) || clipboard.mNotifiedTextClassifierUids.get(callingUid)) {
            return;
        }
        clipboard.mNotifiedTextClassifierUids.put(callingUid, true);
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.clipboard.ClipboardService$$ExternalSyntheticLambda4
            public final void runOrThrow() {
                ClipboardService.lambda$notifyTextClassifierLocked$5(callingPackage, clipboard, textClassifier);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$notifyTextClassifierLocked$5(String callingPackage, PerUserClipboard clipboard, TextClassifier textClassifier) throws Exception {
        TextClassifierEvent.TextLinkifyEvent pasteEvent = ((TextClassifierEvent.TextLinkifyEvent.Builder) ((TextClassifierEvent.TextLinkifyEvent.Builder) new TextClassifierEvent.TextLinkifyEvent.Builder(22).setEventContext(new TextClassificationContext.Builder(callingPackage, "clipboard").build())).setExtras(Bundle.forPair("source_package", clipboard.mPrimaryClipPackage))).build();
        textClassifier.onTextClassifierEvent(pasteEvent);
    }

    private TextClassificationManager createTextClassificationManagerAsUser(int userId) {
        Context context = getContext().createContextAsUser(UserHandle.of(userId), 0);
        return (TextClassificationManager) context.getSystemService(TextClassificationManager.class);
    }
}
