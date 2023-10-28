package com.android.server.am;

import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.media.session.MediaSessionManager;
import android.os.BatteryManagerInternal;
import android.os.BatteryStatsInternal;
import android.os.Handler;
import android.os.ServiceManager;
import android.permission.PermissionManager;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.IAppOpsService;
import com.android.server.DeviceIdleInternal;
import com.android.server.LocalServices;
import com.android.server.am.BaseAppStatePolicy;
import com.android.server.notification.NotificationManagerInternal;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
/* loaded from: classes.dex */
public abstract class BaseAppStateTracker<T extends BaseAppStatePolicy> {
    static final long ONE_DAY = 86400000;
    static final long ONE_HOUR = 3600000;
    static final long ONE_MINUTE = 60000;
    static final int STATE_TYPE_FGS_LOCATION = 4;
    static final int STATE_TYPE_FGS_MEDIA_PLAYBACK = 2;
    static final int STATE_TYPE_FGS_WITH_NOTIFICATION = 8;
    static final int STATE_TYPE_INDEX_FGS_LOCATION = 2;
    static final int STATE_TYPE_INDEX_FGS_MEDIA_PLAYBACK = 1;
    static final int STATE_TYPE_INDEX_FGS_WITH_NOTIFICATION = 3;
    static final int STATE_TYPE_INDEX_MEDIA_SESSION = 0;
    static final int STATE_TYPE_INDEX_PERMISSION = 4;
    static final int STATE_TYPE_MEDIA_SESSION = 1;
    static final int STATE_TYPE_NUM = 5;
    static final int STATE_TYPE_PERMISSION = 16;
    protected static final String TAG = "ActivityManager";
    protected final AppRestrictionController mAppRestrictionController;
    protected final Handler mBgHandler;
    protected final Context mContext;
    protected final Injector<T> mInjector;
    protected final Object mLock;
    protected final ArrayList<StateListener> mStateListeners = new ArrayList<>();

    /* loaded from: classes.dex */
    interface StateListener {
        void onStateChange(int i, String str, boolean z, long j, int i2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateTracker(Context context, AppRestrictionController controller, Constructor<? extends Injector<T>> injector, Object outerContext) {
        this.mContext = context;
        this.mAppRestrictionController = controller;
        this.mBgHandler = controller.getBackgroundHandler();
        this.mLock = controller.getLock();
        if (injector == null) {
            this.mInjector = new Injector<>();
            return;
        }
        Injector<T> localInjector = null;
        try {
            localInjector = injector.newInstance(outerContext);
        } catch (Exception e) {
            Slog.w(TAG, "Unable to instantiate " + injector, e);
        }
        this.mInjector = localInjector == null ? new Injector<>() : localInjector;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int stateTypeToIndex(int stateType) {
        return Integer.numberOfTrailingZeros(stateType);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int stateIndexToType(int stateTypeIndex) {
        return 1 << stateTypeIndex;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String stateTypesToString(int stateTypes) {
        StringBuilder sb = new StringBuilder("[");
        boolean needDelimiter = false;
        int stateType = Integer.highestOneBit(stateTypes);
        while (stateType != 0) {
            if (needDelimiter) {
                sb.append('|');
            }
            needDelimiter = true;
            switch (stateType) {
                case 1:
                    sb.append("MEDIA_SESSION");
                    break;
                case 2:
                    sb.append("FGS_MEDIA_PLAYBACK");
                    break;
                case 4:
                    sb.append("FGS_LOCATION");
                    break;
                case 8:
                    sb.append("FGS_NOTIFICATION");
                    break;
                case 16:
                    sb.append("PERMISSION");
                    break;
                default:
                    return "[UNKNOWN(" + Integer.toHexString(stateTypes) + ")]";
            }
            stateTypes &= ~stateType;
            stateType = Integer.highestOneBit(stateTypes);
        }
        sb.append("]");
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerStateListener(StateListener listener) {
        synchronized (this.mLock) {
            this.mStateListeners.add(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyListenersOnStateChange(int uid, String packageName, boolean start, long now, int stateType) {
        synchronized (this.mLock) {
            int size = this.mStateListeners.size();
            for (int i = 0; i < size; i++) {
                this.mStateListeners.get(i).onStateChange(uid, packageName, start, now, stateType);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getType() {
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public byte[] getTrackerInfoForStatsd(int uid) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public T getPolicy() {
        return this.mInjector.getPolicy();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        this.mInjector.onSystemReady();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUidAdded(int uid) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUidRemoved(int uid) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserAdded(int userId) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserStarted(int userId) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserStopped(int userId) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserRemoved(int userId) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onLockedBootCompleted() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPropertiesChanged(String name) {
        getPolicy().onPropertiesChanged(name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserInteractionStarted(String packageName, int uid) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBackgroundRestrictionChanged(int uid, String pkgName, boolean restricted) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUidProcStateChanged(int uid, int procState) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUidGone(int uid) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        this.mInjector.getPolicy().dump(pw, "  " + prefix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpAsProto(ProtoOutputStream proto, int uid) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector<T extends BaseAppStatePolicy> {
        ActivityManagerInternal mActivityManagerInternal;
        AppOpsManager mAppOpsManager;
        T mAppStatePolicy;
        BatteryManagerInternal mBatteryManagerInternal;
        BatteryStatsInternal mBatteryStatsInternal;
        DeviceIdleInternal mDeviceIdleInternal;
        IAppOpsService mIAppOpsService;
        MediaSessionManager mMediaSessionManager;
        NotificationManagerInternal mNotificationManagerInternal;
        PackageManager mPackageManager;
        PackageManagerInternal mPackageManagerInternal;
        PermissionManager mPermissionManager;
        PermissionManagerServiceInternal mPermissionManagerServiceInternal;
        RoleManager mRoleManager;
        UserManagerInternal mUserManagerInternal;

        Injector() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void setPolicy(T policy) {
            this.mAppStatePolicy = policy;
        }

        void onSystemReady() {
            this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            this.mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
            this.mBatteryStatsInternal = (BatteryStatsInternal) LocalServices.getService(BatteryStatsInternal.class);
            this.mDeviceIdleInternal = (DeviceIdleInternal) LocalServices.getService(DeviceIdleInternal.class);
            this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            this.mPermissionManagerServiceInternal = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
            Context context = this.mAppStatePolicy.mTracker.mContext;
            this.mPackageManager = context.getPackageManager();
            this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
            this.mMediaSessionManager = (MediaSessionManager) context.getSystemService(MediaSessionManager.class);
            this.mPermissionManager = (PermissionManager) context.getSystemService(PermissionManager.class);
            this.mRoleManager = (RoleManager) context.getSystemService(RoleManager.class);
            this.mNotificationManagerInternal = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
            this.mIAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
            getPolicy().onSystemReady();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ActivityManagerInternal getActivityManagerInternal() {
            return this.mActivityManagerInternal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BatteryManagerInternal getBatteryManagerInternal() {
            return this.mBatteryManagerInternal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BatteryStatsInternal getBatteryStatsInternal() {
            return this.mBatteryStatsInternal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public T getPolicy() {
            return this.mAppStatePolicy;
        }

        DeviceIdleInternal getDeviceIdleInternal() {
            return this.mDeviceIdleInternal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public UserManagerInternal getUserManagerInternal() {
            return this.mUserManagerInternal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public PackageManager getPackageManager() {
            return this.mPackageManager;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public PackageManagerInternal getPackageManagerInternal() {
            return this.mPackageManagerInternal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public PermissionManager getPermissionManager() {
            return this.mPermissionManager;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public PermissionManagerServiceInternal getPermissionManagerServiceInternal() {
            return this.mPermissionManagerServiceInternal;
        }

        AppOpsManager getAppOpsManager() {
            return this.mAppOpsManager;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public MediaSessionManager getMediaSessionManager() {
            return this.mMediaSessionManager;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public long getServiceStartForegroundTimeout() {
            return this.mActivityManagerInternal.getServiceStartForegroundTimeout();
        }

        RoleManager getRoleManager() {
            return this.mRoleManager;
        }

        NotificationManagerInternal getNotificationManagerInternal() {
            return this.mNotificationManagerInternal;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public IAppOpsService getIAppOpsService() {
            return this.mIAppOpsService;
        }
    }
}
