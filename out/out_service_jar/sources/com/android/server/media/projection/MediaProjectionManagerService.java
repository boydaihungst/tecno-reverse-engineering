package com.android.server.media.projection;

import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.IProcessObserver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaRouter;
import android.media.projection.IMediaProjection;
import android.media.projection.IMediaProjectionCallback;
import android.media.projection.IMediaProjectionManager;
import android.media.projection.IMediaProjectionWatcherCallback;
import android.media.projection.MediaProjectionInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.ContentRecordingSession;
import android.window.WindowContainerToken;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.wm.WindowManagerInternal;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.server.media.projection.ITranMediaProjectionManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Map;
/* loaded from: classes2.dex */
public final class MediaProjectionManagerService extends SystemService implements Watchdog.Monitor {
    private static final boolean REQUIRE_FG_SERVICE_FOR_PROJECTION = true;
    public static int SCREEN_RECORDING_EVENT_START = 1;
    public static int SCREEN_RECORDING_EVENT_STOP = 2;
    private static final String TAG = "MediaProjectionManagerService";
    private final ActivityManagerInternal mActivityManagerInternal;
    private final AppOpsManager mAppOps;
    private final CallbackDelegate mCallbackDelegate;
    private final Context mContext;
    private final Map<IBinder, IBinder.DeathRecipient> mDeathEaters;
    private final Object mLock;
    private MediaRouter.RouteInfo mMediaRouteInfo;
    private final MediaRouter mMediaRouter;
    private final MediaRouterCallback mMediaRouterCallback;
    private final PackageManager mPackageManager;
    private MediaProjection mProjectionGrant;
    private IBinder mProjectionToken;

    public MediaProjectionManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mContext = context;
        this.mDeathEaters = new ArrayMap();
        this.mCallbackDelegate = new CallbackDelegate();
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mPackageManager = context.getPackageManager();
        this.mMediaRouter = (MediaRouter) context.getSystemService("media_router");
        this.mMediaRouterCallback = new MediaRouterCallback();
        Watchdog.getInstance().addMonitor(this);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("media_projection", new BinderService(), false);
        this.mMediaRouter.addCallback(4, this.mMediaRouterCallback, 8);
        this.mActivityManagerInternal.registerProcessObserver(new IProcessObserver.Stub() { // from class: com.android.server.media.projection.MediaProjectionManagerService.1
            public void onForegroundActivitiesChanged(int pid, int uid, boolean fg) {
            }

            public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
                MediaProjectionManagerService.this.handleForegroundServicesChanged(pid, uid, serviceTypes);
            }

            public void onProcessDied(int pid, int uid) {
            }
        });
    }

    @Override // com.android.server.SystemService
    public void onUserSwitching(SystemService.TargetUser from, SystemService.TargetUser to) {
        this.mMediaRouter.rebindAsUser(to.getUserIdentifier());
        synchronized (this.mLock) {
            MediaProjection mediaProjection = this.mProjectionGrant;
            if (mediaProjection != null) {
                mediaProjection.stop();
            }
        }
    }

    @Override // com.android.server.Watchdog.Monitor
    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        synchronized (this.mLock) {
            MediaProjection mediaProjection = this.mProjectionGrant;
            if (mediaProjection != null && mediaProjection.uid == uid) {
                if (this.mProjectionGrant.requiresForegroundService()) {
                    if ((serviceTypes & 32) != 0) {
                        return;
                    }
                    this.mProjectionGrant.stop();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startProjectionLocked(MediaProjection projection) {
        MediaProjection mediaProjection = this.mProjectionGrant;
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        if (this.mMediaRouteInfo != null) {
            this.mMediaRouter.getFallbackRoute().select();
        }
        this.mProjectionToken = projection.asBinder();
        this.mProjectionGrant = projection;
        dispatchStart(projection);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopProjectionLocked(MediaProjection projection) {
        this.mProjectionToken = null;
        this.mProjectionGrant = null;
        dispatchStop(projection);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addCallback(final IMediaProjectionWatcherCallback callback) {
        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.media.projection.MediaProjectionManagerService.2
            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                MediaProjectionManagerService.this.removeCallback(callback);
            }
        };
        synchronized (this.mLock) {
            this.mCallbackDelegate.add(callback);
            linkDeathRecipientLocked(callback, deathRecipient);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeCallback(IMediaProjectionWatcherCallback callback) {
        synchronized (this.mLock) {
            unlinkDeathRecipientLocked(callback);
            this.mCallbackDelegate.remove(callback);
        }
    }

    private void linkDeathRecipientLocked(IMediaProjectionWatcherCallback callback, IBinder.DeathRecipient deathRecipient) {
        try {
            IBinder token = callback.asBinder();
            token.linkToDeath(deathRecipient, 0);
            this.mDeathEaters.put(token, deathRecipient);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to link to death for media projection monitoring callback", e);
        }
    }

    private void unlinkDeathRecipientLocked(IMediaProjectionWatcherCallback callback) {
        IBinder token = callback.asBinder();
        IBinder.DeathRecipient deathRecipient = this.mDeathEaters.remove(token);
        if (deathRecipient != null) {
            token.unlinkToDeath(deathRecipient, 0);
        }
    }

    private void dispatchStart(MediaProjection projection) {
        this.mCallbackDelegate.dispatchStart(projection);
    }

    private void dispatchStop(MediaProjection projection) {
        this.mCallbackDelegate.dispatchStop(projection);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isValidMediaProjection(IBinder token) {
        synchronized (this.mLock) {
            IBinder iBinder = this.mProjectionToken;
            if (iBinder != null) {
                return iBinder.equals(token);
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public MediaProjectionInfo getActiveProjectionInfo() {
        synchronized (this.mLock) {
            MediaProjection mediaProjection = this.mProjectionGrant;
            if (mediaProjection == null) {
                return null;
            }
            return mediaProjection.getProjectionInfo();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dump(PrintWriter pw) {
        pw.println("MEDIA PROJECTION MANAGER (dumpsys media_projection)");
        synchronized (this.mLock) {
            pw.println("Media Projection: ");
            MediaProjection mediaProjection = this.mProjectionGrant;
            if (mediaProjection != null) {
                mediaProjection.dump(pw);
            } else {
                pw.println("null");
            }
        }
    }

    /* loaded from: classes2.dex */
    private final class BinderService extends IMediaProjectionManager.Stub {
        private BinderService() {
        }

        public boolean hasProjectionPermission(int uid, String packageName) {
            boolean z;
            long token = Binder.clearCallingIdentity();
            try {
                if (!checkPermission(packageName, "android.permission.CAPTURE_VIDEO_OUTPUT")) {
                    if (MediaProjectionManagerService.this.mAppOps.noteOpNoThrow(46, uid, packageName) != 0) {
                        z = false;
                        boolean hasPermission = false | z;
                        return hasPermission;
                    }
                }
                z = true;
                boolean hasPermission2 = false | z;
                return hasPermission2;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public IMediaProjection createProjection(int uid, String packageName, int type, boolean isPermanentGrant) {
            if (MediaProjectionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_MEDIA_PROJECTION") != 0) {
                throw new SecurityException("Requires MANAGE_MEDIA_PROJECTION in order to grant projection permission");
            }
            if (packageName == null || packageName.isEmpty()) {
                throw new IllegalArgumentException("package name must not be empty");
            }
            UserHandle callingUser = Binder.getCallingUserHandle();
            long callingToken = Binder.clearCallingIdentity();
            try {
                try {
                    ApplicationInfo ai = MediaProjectionManagerService.this.mPackageManager.getApplicationInfoAsUser(packageName, 0, callingUser);
                    MediaProjection projection = new MediaProjection(type, uid, packageName, ai.targetSdkVersion, ai.isPrivilegedApp());
                    if (isPermanentGrant) {
                        MediaProjectionManagerService.this.mAppOps.setMode(46, projection.uid, projection.packageName, 0);
                    }
                    return projection;
                } catch (PackageManager.NameNotFoundException e) {
                    throw new IllegalArgumentException("No package matching :" + packageName);
                }
            } finally {
                Binder.restoreCallingIdentity(callingToken);
            }
        }

        public boolean isValidMediaProjection(IMediaProjection projection) {
            return MediaProjectionManagerService.this.isValidMediaProjection(projection.asBinder());
        }

        public MediaProjectionInfo getActiveProjectionInfo() {
            if (MediaProjectionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_MEDIA_PROJECTION") != 0) {
                throw new SecurityException("Requires MANAGE_MEDIA_PROJECTION in order to add projection callbacks");
            }
            long token = Binder.clearCallingIdentity();
            try {
                return MediaProjectionManagerService.this.getActiveProjectionInfo();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void stopActiveProjection() {
            if (MediaProjectionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_MEDIA_PROJECTION") != 0) {
                throw new SecurityException("Requires MANAGE_MEDIA_PROJECTION in order to add projection callbacks");
            }
            long token = Binder.clearCallingIdentity();
            try {
                if (MediaProjectionManagerService.this.mProjectionGrant != null) {
                    MediaProjectionManagerService.this.mProjectionGrant.stop();
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void addCallback(IMediaProjectionWatcherCallback callback) {
            if (MediaProjectionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_MEDIA_PROJECTION") != 0) {
                throw new SecurityException("Requires MANAGE_MEDIA_PROJECTION in order to add projection callbacks");
            }
            long token = Binder.clearCallingIdentity();
            try {
                MediaProjectionManagerService.this.addCallback(callback);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void removeCallback(IMediaProjectionWatcherCallback callback) {
            if (MediaProjectionManagerService.this.mContext.checkCallingPermission("android.permission.MANAGE_MEDIA_PROJECTION") != 0) {
                throw new SecurityException("Requires MANAGE_MEDIA_PROJECTION in order to remove projection callbacks");
            }
            long token = Binder.clearCallingIdentity();
            try {
                MediaProjectionManagerService.this.removeCallback(callback);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setContentRecordingSession(ContentRecordingSession incomingSession, IMediaProjection projection) {
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (MediaProjectionManagerService.this.mLock) {
                    if (!isValidMediaProjection(projection)) {
                        throw new SecurityException("Invalid media projection");
                    }
                    ((WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class)).setContentRecordingSession(incomingSession);
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(MediaProjectionManagerService.this.mContext, MediaProjectionManagerService.TAG, pw)) {
                long token = Binder.clearCallingIdentity();
                try {
                    MediaProjectionManagerService.this.dump(pw);
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }

        private boolean checkPermission(String packageName, String permission) {
            return MediaProjectionManagerService.this.mContext.getPackageManager().checkPermission(permission, packageName) == 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class MediaProjection extends IMediaProjection.Stub {
        private IMediaProjectionCallback mCallback;
        private IBinder.DeathRecipient mDeathEater;
        private final boolean mIsPrivileged;
        private boolean mRestoreSystemAlertWindow;
        private final int mTargetSdkVersion;
        private WindowContainerToken mTaskRecordingWindowContainerToken = null;
        private IBinder mToken;
        private final int mType;
        public final String packageName;
        public final int uid;
        public final UserHandle userHandle;

        MediaProjection(int type, int uid, String packageName, int targetSdkVersion, boolean isPrivileged) {
            this.mType = type;
            this.uid = uid;
            this.packageName = packageName;
            this.userHandle = new UserHandle(UserHandle.getUserId(uid));
            this.mTargetSdkVersion = targetSdkVersion;
            this.mIsPrivileged = isPrivileged;
        }

        public boolean canProjectVideo() {
            int i = this.mType;
            return i == 1 || i == 0;
        }

        public boolean canProjectSecureVideo() {
            return false;
        }

        public boolean canProjectAudio() {
            int i = this.mType;
            return i == 1 || i == 2 || i == 0;
        }

        public int applyVirtualDisplayFlags(int flags) {
            int i = this.mType;
            if (i == 0) {
                return (flags & (-9)) | 18;
            }
            if (i == 1) {
                return (flags & (-18)) | 10;
            }
            if (i == 2) {
                return (flags & (-9)) | 19;
            }
            throw new RuntimeException("Unknown MediaProjection type");
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [563=4] */
        public void start(final IMediaProjectionCallback callback) {
            if (callback == null) {
                throw new IllegalArgumentException("callback must not be null");
            }
            synchronized (MediaProjectionManagerService.this.mLock) {
                if (MediaProjectionManagerService.this.isValidMediaProjection(asBinder())) {
                    Slog.w(MediaProjectionManagerService.TAG, "UID " + Binder.getCallingUid() + " attempted to start already started MediaProjection");
                    return;
                }
                if (requiresForegroundService() && !MediaProjectionManagerService.this.mActivityManagerInternal.hasRunningForegroundService(this.uid, 32)) {
                    throw new SecurityException("Media projections require a foreground service of type ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION");
                }
                this.mCallback = callback;
                registerCallback(callback);
                try {
                    this.mToken = callback.asBinder();
                    IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.media.projection.MediaProjectionManagerService.MediaProjection.1
                        @Override // android.os.IBinder.DeathRecipient
                        public void binderDied() {
                            MediaProjectionManagerService.this.mCallbackDelegate.remove(callback);
                            MediaProjection.this.stop();
                        }
                    };
                    this.mDeathEater = deathRecipient;
                    this.mToken.linkToDeath(deathRecipient, 0);
                    if (this.mType == 0) {
                        long token = Binder.clearCallingIdentity();
                        try {
                            PackageInfo packageInfo = MediaProjectionManagerService.this.mPackageManager.getPackageInfoAsUser(this.packageName, 4096, UserHandle.getUserId(this.uid));
                            if (ArrayUtils.contains(packageInfo.requestedPermissions, "android.permission.SYSTEM_ALERT_WINDOW")) {
                                int currentMode = MediaProjectionManagerService.this.mAppOps.unsafeCheckOpRawNoThrow(24, this.uid, this.packageName);
                                if (currentMode == 3) {
                                    MediaProjectionManagerService.this.mAppOps.setMode(24, this.uid, this.packageName, 0);
                                    this.mRestoreSystemAlertWindow = true;
                                }
                            }
                            Binder.restoreCallingIdentity(token);
                        } catch (PackageManager.NameNotFoundException e) {
                            Slog.w(MediaProjectionManagerService.TAG, "Package not found, aborting MediaProjection", e);
                            Binder.restoreCallingIdentity(token);
                            return;
                        }
                    }
                    MediaProjectionManagerService.this.startProjectionLocked(this);
                    if (ITranGriffinFeature.Instance().isGriffinSupport()) {
                        ITranMediaProjectionManagerService.Instance().hookScreenRecordingChanged(this.uid, this.packageName, MediaProjectionManagerService.SCREEN_RECORDING_EVENT_START);
                    }
                } catch (RemoteException e2) {
                    Slog.w(MediaProjectionManagerService.TAG, "MediaProjectionCallbacks must be valid, aborting MediaProjection", e2);
                }
            }
        }

        public void stop() {
            synchronized (MediaProjectionManagerService.this.mLock) {
                if (!MediaProjectionManagerService.this.isValidMediaProjection(asBinder())) {
                    Slog.w(MediaProjectionManagerService.TAG, "Attempted to stop inactive MediaProjection (uid=" + Binder.getCallingUid() + ", pid=" + Binder.getCallingPid() + ")");
                    return;
                }
                if (this.mRestoreSystemAlertWindow) {
                    long token = Binder.clearCallingIdentity();
                    int currentMode = MediaProjectionManagerService.this.mAppOps.unsafeCheckOpRawNoThrow(24, this.uid, this.packageName);
                    if (currentMode == 0) {
                        MediaProjectionManagerService.this.mAppOps.setMode(24, this.uid, this.packageName, 3);
                    }
                    this.mRestoreSystemAlertWindow = false;
                    Binder.restoreCallingIdentity(token);
                }
                MediaProjectionManagerService.this.stopProjectionLocked(this);
                if (ITranGriffinFeature.Instance().isGriffinSupport()) {
                    ITranMediaProjectionManagerService.Instance().hookScreenRecordingChanged(this.uid, this.packageName, MediaProjectionManagerService.SCREEN_RECORDING_EVENT_STOP);
                }
                this.mToken.unlinkToDeath(this.mDeathEater, 0);
                this.mDeathEater = null;
                this.mToken = null;
                unregisterCallback(this.mCallback);
                this.mCallback = null;
            }
        }

        public void registerCallback(IMediaProjectionCallback callback) {
            if (callback == null) {
                throw new IllegalArgumentException("callback must not be null");
            }
            MediaProjectionManagerService.this.mCallbackDelegate.add(callback);
        }

        public void unregisterCallback(IMediaProjectionCallback callback) {
            if (callback == null) {
                throw new IllegalArgumentException("callback must not be null");
            }
            MediaProjectionManagerService.this.mCallbackDelegate.remove(callback);
        }

        public void setTaskRecordingWindowContainerToken(WindowContainerToken token) {
            this.mTaskRecordingWindowContainerToken = token;
        }

        public WindowContainerToken getTaskRecordingWindowContainerToken() {
            return this.mTaskRecordingWindowContainerToken;
        }

        public MediaProjectionInfo getProjectionInfo() {
            return new MediaProjectionInfo(this.packageName, this.userHandle);
        }

        boolean requiresForegroundService() {
            return this.mTargetSdkVersion >= 29 && !this.mIsPrivileged;
        }

        public void dump(PrintWriter pw) {
            pw.println("(" + this.packageName + ", uid=" + this.uid + "): " + MediaProjectionManagerService.typeToString(this.mType));
        }
    }

    /* loaded from: classes2.dex */
    private class MediaRouterCallback extends MediaRouter.SimpleCallback {
        private MediaRouterCallback() {
        }

        @Override // android.media.MediaRouter.SimpleCallback, android.media.MediaRouter.Callback
        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            synchronized (MediaProjectionManagerService.this.mLock) {
                if ((type & 4) != 0) {
                    MediaProjectionManagerService.this.mMediaRouteInfo = info;
                    if (MediaProjectionManagerService.this.mProjectionGrant != null) {
                        MediaProjectionManagerService.this.mProjectionGrant.stop();
                    }
                }
            }
        }

        @Override // android.media.MediaRouter.SimpleCallback, android.media.MediaRouter.Callback
        public void onRouteUnselected(MediaRouter route, int type, MediaRouter.RouteInfo info) {
            if (MediaProjectionManagerService.this.mMediaRouteInfo == info) {
                MediaProjectionManagerService.this.mMediaRouteInfo = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class CallbackDelegate {
        private Object mLock = new Object();
        private Handler mHandler = new Handler(Looper.getMainLooper(), null, true);
        private Map<IBinder, IMediaProjectionCallback> mClientCallbacks = new ArrayMap();
        private Map<IBinder, IMediaProjectionWatcherCallback> mWatcherCallbacks = new ArrayMap();

        public void add(IMediaProjectionCallback callback) {
            synchronized (this.mLock) {
                this.mClientCallbacks.put(callback.asBinder(), callback);
            }
        }

        public void add(IMediaProjectionWatcherCallback callback) {
            synchronized (this.mLock) {
                this.mWatcherCallbacks.put(callback.asBinder(), callback);
            }
        }

        public void remove(IMediaProjectionCallback callback) {
            synchronized (this.mLock) {
                this.mClientCallbacks.remove(callback.asBinder());
            }
        }

        public void remove(IMediaProjectionWatcherCallback callback) {
            synchronized (this.mLock) {
                this.mWatcherCallbacks.remove(callback.asBinder());
            }
        }

        public void dispatchStart(MediaProjection projection) {
            if (projection == null) {
                Slog.e(MediaProjectionManagerService.TAG, "Tried to dispatch start notification for a null media projection. Ignoring!");
                return;
            }
            synchronized (this.mLock) {
                for (IMediaProjectionWatcherCallback callback : this.mWatcherCallbacks.values()) {
                    MediaProjectionInfo info = projection.getProjectionInfo();
                    this.mHandler.post(new WatcherStartCallback(info, callback));
                }
            }
        }

        public void dispatchStop(MediaProjection projection) {
            if (projection == null) {
                Slog.e(MediaProjectionManagerService.TAG, "Tried to dispatch stop notification for a null media projection. Ignoring!");
                return;
            }
            synchronized (this.mLock) {
                for (IMediaProjectionCallback callback : this.mClientCallbacks.values()) {
                    this.mHandler.post(new ClientStopCallback(callback));
                }
                for (IMediaProjectionWatcherCallback callback2 : this.mWatcherCallbacks.values()) {
                    MediaProjectionInfo info = projection.getProjectionInfo();
                    this.mHandler.post(new WatcherStopCallback(info, callback2));
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class WatcherStartCallback implements Runnable {
        private IMediaProjectionWatcherCallback mCallback;
        private MediaProjectionInfo mInfo;

        public WatcherStartCallback(MediaProjectionInfo info, IMediaProjectionWatcherCallback callback) {
            this.mInfo = info;
            this.mCallback = callback;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                this.mCallback.onStart(this.mInfo);
            } catch (RemoteException e) {
                Slog.w(MediaProjectionManagerService.TAG, "Failed to notify media projection has stopped", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class WatcherStopCallback implements Runnable {
        private IMediaProjectionWatcherCallback mCallback;
        private MediaProjectionInfo mInfo;

        public WatcherStopCallback(MediaProjectionInfo info, IMediaProjectionWatcherCallback callback) {
            this.mInfo = info;
            this.mCallback = callback;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                this.mCallback.onStop(this.mInfo);
            } catch (RemoteException e) {
                Slog.w(MediaProjectionManagerService.TAG, "Failed to notify media projection has stopped", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class ClientStopCallback implements Runnable {
        private IMediaProjectionCallback mCallback;

        public ClientStopCallback(IMediaProjectionCallback callback) {
            this.mCallback = callback;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                this.mCallback.onStop();
            } catch (RemoteException e) {
                Slog.w(MediaProjectionManagerService.TAG, "Failed to notify media projection has stopped", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String typeToString(int type) {
        switch (type) {
            case 0:
                return "TYPE_SCREEN_CAPTURE";
            case 1:
                return "TYPE_MIRRORING";
            case 2:
                return "TYPE_PRESENTATION";
            default:
                return Integer.toString(type);
        }
    }
}
