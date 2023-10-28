package com.android.server.pm.permission;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.SigningDetails;
import android.content.pm.permission.SplitPermissionInfoParcelable;
import android.metrics.LogMaker;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.permission.IOnPermissionsChangeListener;
import android.permission.PermissionControllerManager;
import android.permission.PermissionManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.IntArray;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.compat.IPlatformCompat;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.RoSystemProperties;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IntPair;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.SystemConfig;
import com.android.server.Watchdog;
import com.android.server.am.HostingRecord;
import com.android.server.pm.ApexManager;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageSetting;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.permission.LegacyPermissionState;
import com.android.server.pm.permission.PermissionManagerServiceImpl;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.SharedUserApi;
import com.android.server.pm.pkg.component.ComponentMutateUtils;
import com.android.server.pm.pkg.component.ParsedPermission;
import com.android.server.pm.pkg.component.ParsedPermissionGroup;
import com.android.server.pm.pkg.component.ParsedPermissionUtils;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import com.android.server.policy.PermissionPolicyInternal;
import com.android.server.policy.SoftRestrictedPermissionPolicy;
import com.android.server.slice.SliceClientPermissions;
import com.mediatek.cta.CtaManagerFactory;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public class PermissionManagerServiceImpl implements PermissionManagerServiceInterface {
    private static final long BACKGROUND_RATIONALE_CHANGE_ID = 147316723;
    private static final int BLOCKING_PERMISSION_FLAGS = 52;
    private static final Map<String, String> FULLER_PERMISSION_MAP;
    private static final int MAX_PERMISSION_TREE_FOOTPRINT = 32768;
    private static final List<String> NEARBY_DEVICES_PERMISSIONS;
    private static final List<String> NOTIFICATION_PERMISSIONS;
    private static final Set<String> READ_MEDIA_AURAL_PERMISSIONS;
    private static final Set<String> READ_MEDIA_VISUAL_PERMISSIONS;
    private static final String SKIP_KILL_APP_REASON_NOTIFICATION_TEST = "skip permission revoke app kill for notification test";
    private static final List<String> STORAGE_PERMISSIONS;
    private static final String TAG = "PackageManager";
    private static final int UPDATE_PERMISSIONS_ALL = 1;
    private static final int UPDATE_PERMISSIONS_REPLACE_ALL = 4;
    private static final int UPDATE_PERMISSIONS_REPLACE_PKG = 2;
    private static final int USER_PERMISSION_FLAGS = 3;
    private final Context mContext;
    private final PermissionCallback mDefaultPermissionCallback;
    private final int[] mGlobalGids;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private final SparseBooleanArray mHasNoDelayedPermBackup;
    private final boolean mIsLeanback;
    private final Object mLock;
    private final MetricsLogger mMetricsLogger;
    private final OnPermissionChangeListeners mOnPermissionChangeListeners;
    private final PackageManagerInternal mPackageManagerInt;
    private PermissionControllerManager mPermissionControllerManager;
    private PermissionPolicyInternal mPermissionPolicyInternal;
    private final IPlatformCompat mPlatformCompat;
    private ArraySet<String> mPrivappPermissionsViolations;
    private final ArraySet<String> mPrivilegedPermissionAllowlistSourcePackageNames;
    private final PermissionRegistry mRegistry;
    private final ArrayList<PermissionManagerServiceInternal.OnRuntimePermissionStateChangedListener> mRuntimePermissionStateChangedListeners;
    private final DevicePermissionState mState;
    private final SparseArray<ArraySet<String>> mSystemPermissions;
    private boolean mSystemReady;
    private final UserManagerInternal mUserManagerInt;
    private static final String LOG_TAG = PermissionManagerServiceImpl.class.getSimpleName();
    private static final long BACKUP_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60);
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    private @interface UpdatePermissionFlags {
    }

    static {
        ArrayList arrayList = new ArrayList();
        STORAGE_PERMISSIONS = arrayList;
        ArraySet arraySet = new ArraySet();
        READ_MEDIA_AURAL_PERMISSIONS = arraySet;
        ArraySet arraySet2 = new ArraySet();
        READ_MEDIA_VISUAL_PERMISSIONS = arraySet2;
        ArrayList arrayList2 = new ArrayList();
        NEARBY_DEVICES_PERMISSIONS = arrayList2;
        ArrayList arrayList3 = new ArrayList();
        NOTIFICATION_PERMISSIONS = arrayList3;
        HashMap hashMap = new HashMap();
        FULLER_PERMISSION_MAP = hashMap;
        hashMap.put("android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION");
        hashMap.put("android.permission.INTERACT_ACROSS_USERS", "android.permission.INTERACT_ACROSS_USERS_FULL");
        arrayList.add("android.permission.READ_EXTERNAL_STORAGE");
        arrayList.add("android.permission.WRITE_EXTERNAL_STORAGE");
        arraySet.add("android.permission.READ_MEDIA_AUDIO");
        arraySet2.add("android.permission.READ_MEDIA_VIDEO");
        arraySet2.add("android.permission.READ_MEDIA_IMAGES");
        arraySet2.add("android.permission.ACCESS_MEDIA_LOCATION");
        arrayList2.add("android.permission.BLUETOOTH_ADVERTISE");
        arrayList2.add("android.permission.BLUETOOTH_CONNECT");
        arrayList2.add("android.permission.BLUETOOTH_SCAN");
        arrayList3.add("android.permission.POST_NOTIFICATIONS");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.pm.permission.PermissionManagerServiceImpl$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 extends PermissionCallback {
        AnonymousClass1() {
            super();
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
        public void onGidsChanged(final int appId, final int userId) {
            PermissionManagerServiceImpl.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    PermissionManagerServiceImpl.killUid(appId, userId, "permission grant or revoke changed gids");
                }
            });
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
        public void onPermissionGranted(int uid, int userId) {
            PermissionManagerServiceImpl.this.mOnPermissionChangeListeners.onPermissionsChanged(uid);
            PermissionManagerServiceImpl.this.mPackageManagerInt.writeSettings(true);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
        public void onInstallPermissionGranted() {
            PermissionManagerServiceImpl.this.mPackageManagerInt.writeSettings(true);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
        public void onPermissionRevoked(final int uid, final int userId, final String reason, boolean overrideKill, final String permissionName) {
            PermissionManagerServiceImpl.this.mOnPermissionChangeListeners.onPermissionsChanged(uid);
            PermissionManagerServiceImpl.this.mPackageManagerInt.writeSettings(false);
            if (overrideKill) {
                return;
            }
            PermissionManagerServiceImpl.this.mHandler.post(new Runnable() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PermissionManagerServiceImpl.AnonymousClass1.this.m5853xe2848fae(permissionName, uid, reason, userId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onPermissionRevoked$1$com-android-server-pm-permission-PermissionManagerServiceImpl$1  reason: not valid java name */
        public /* synthetic */ void m5853xe2848fae(String permissionName, int uid, String reason, int userId) {
            if ("android.permission.POST_NOTIFICATIONS".equals(permissionName) && isAppBackupAndRestoreRunning(uid)) {
                return;
            }
            int appId = UserHandle.getAppId(uid);
            if (reason == null) {
                PermissionManagerServiceImpl.killUid(appId, userId, "permissions revoked");
            } else {
                PermissionManagerServiceImpl.killUid(appId, userId, reason);
            }
        }

        private boolean isAppBackupAndRestoreRunning(int uid) {
            if (PermissionManagerServiceImpl.this.checkUidPermission(uid, "android.permission.BACKUP") != 0) {
                return false;
            }
            try {
                int userId = UserHandle.getUserId(uid);
                boolean isInSetup = Settings.Secure.getIntForUser(PermissionManagerServiceImpl.this.mContext.getContentResolver(), "user_setup_complete", userId) == 0;
                boolean isInDeferredSetup = Settings.Secure.getIntForUser(PermissionManagerServiceImpl.this.mContext.getContentResolver(), "user_setup_personalization_state", userId) == 1;
                return isInSetup || isInDeferredSetup;
            } catch (Settings.SettingNotFoundException e) {
                Slog.w(PermissionManagerServiceImpl.LOG_TAG, "Failed to check if the user is in restore: " + e);
                return false;
            }
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
        public void onInstallPermissionRevoked() {
            PermissionManagerServiceImpl.this.mPackageManagerInt.writeSettings(true);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
        public void onPermissionUpdated(int[] userIds, boolean sync) {
            PermissionManagerServiceImpl.this.mPackageManagerInt.writePermissionSettings(userIds, !sync);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
        public void onInstallPermissionUpdated() {
            PermissionManagerServiceImpl.this.mPackageManagerInt.writeSettings(true);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
        public void onPermissionRemoved() {
            PermissionManagerServiceImpl.this.mPackageManagerInt.writeSettings(false);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
        public void onPermissionUpdatedNotifyListener(int[] updatedUserIds, boolean sync, int uid) {
            onPermissionUpdated(updatedUserIds, sync);
            for (int i : updatedUserIds) {
                int userUid = UserHandle.getUid(i, UserHandle.getAppId(uid));
                PermissionManagerServiceImpl.this.mOnPermissionChangeListeners.onPermissionsChanged(userUid);
            }
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
        public void onInstallPermissionUpdatedNotifyListener(int uid) {
            onInstallPermissionUpdated();
            PermissionManagerServiceImpl.this.mOnPermissionChangeListeners.onPermissionsChanged(uid);
        }
    }

    public PermissionManagerServiceImpl(Context context, ArrayMap<String, FeatureInfo> availableFeatures) {
        String carServicePackage;
        ArraySet<String> arraySet = new ArraySet<>();
        this.mPrivilegedPermissionAllowlistSourcePackageNames = arraySet;
        Object obj = new Object();
        this.mLock = obj;
        this.mState = new DevicePermissionState();
        this.mMetricsLogger = new MetricsLogger();
        this.mPlatformCompat = IPlatformCompat.Stub.asInterface(ServiceManager.getService("platform_compat"));
        this.mRegistry = new PermissionRegistry();
        this.mHasNoDelayedPermBackup = new SparseBooleanArray();
        this.mRuntimePermissionStateChangedListeners = new ArrayList<>();
        this.mDefaultPermissionCallback = new AnonymousClass1();
        PackageManager.invalidatePackageInfoCache();
        PermissionManager.disablePackageNamePermissionCache();
        this.mContext = context;
        this.mPackageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mUserManagerInt = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        this.mIsLeanback = availableFeatures.containsKey("android.software.leanback");
        arraySet.add(PackageManagerService.PLATFORM_PACKAGE_NAME);
        if (availableFeatures.containsKey("android.hardware.type.automotive") && (carServicePackage = SystemProperties.get("ro.android.car.carservice.package", (String) null)) != null) {
            arraySet.add(carServicePackage);
        }
        ServiceThread serviceThread = new ServiceThread(TAG, 10, true);
        this.mHandlerThread = serviceThread;
        serviceThread.start();
        Handler handler = new Handler(serviceThread.getLooper());
        this.mHandler = handler;
        Watchdog.getInstance().addThread(handler);
        SystemConfig systemConfig = SystemConfig.getInstance();
        this.mSystemPermissions = systemConfig.getSystemPermissions();
        this.mGlobalGids = systemConfig.getGlobalGids();
        this.mOnPermissionChangeListeners = new OnPermissionChangeListeners(FgThread.get().getLooper());
        ArrayMap<String, SystemConfig.PermissionEntry> permConfig = SystemConfig.getInstance().getPermissions();
        synchronized (obj) {
            for (int i = 0; i < permConfig.size(); i++) {
                SystemConfig.PermissionEntry perm = permConfig.valueAt(i);
                Permission bp = this.mRegistry.getPermission(perm.name);
                if (bp == null) {
                    bp = new Permission(perm.name, PackageManagerService.PLATFORM_PACKAGE_NAME, 1);
                    this.mRegistry.addPermission(bp);
                }
                if (perm.gids != null) {
                    bp.setGids(perm.gids, perm.perUser);
                }
            }
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            return;
        }
        ((PermissionControllerManager) this.mContext.getSystemService(PermissionControllerManager.class)).dump(fd, args);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void killUid(int appId, int userId, String reason) {
        long identity = Binder.clearCallingIdentity();
        try {
            IActivityManager am = ActivityManager.getService();
            if (am != null) {
                try {
                    am.killUidForPermissionChange(appId, userId, reason);
                } catch (RemoteException e) {
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private String[] getAppOpPermissionPackagesInternal(String permissionName) {
        synchronized (this.mLock) {
            ArraySet<String> packageNames = this.mRegistry.getAppOpPermissionPackages(permissionName);
            if (packageNames == null) {
                return EmptyArray.STRING;
            }
            return (String[]) packageNames.toArray(new String[0]);
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        final int callingUid = Binder.getCallingUid();
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            return Collections.emptyList();
        }
        List<PermissionGroupInfo> out = new ArrayList<>();
        synchronized (this.mLock) {
            for (ParsedPermissionGroup pg : this.mRegistry.getPermissionGroups()) {
                out.add(PackageInfoUtils.generatePermissionGroupInfo(pg, flags));
            }
        }
        final int callingUserId = UserHandle.getUserId(callingUid);
        out.removeIf(new Predicate() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda7
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return PermissionManagerServiceImpl.this.m5839x5ead9858(callingUid, callingUserId, (PermissionGroupInfo) obj);
            }
        });
        return out;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getAllPermissionGroups$0$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ boolean m5839x5ead9858(int callingUid, int callingUserId, PermissionGroupInfo it) {
        return this.mPackageManagerInt.filterAppAccess(it.packageName, callingUid, callingUserId);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public PermissionGroupInfo getPermissionGroupInfo(String groupName, int flags) {
        int callingUid = Binder.getCallingUid();
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        synchronized (this.mLock) {
            ParsedPermissionGroup permissionGroup = this.mRegistry.getPermissionGroup(groupName);
            if (permissionGroup == null) {
                return null;
            }
            PermissionGroupInfo permissionGroupInfo = PackageInfoUtils.generatePermissionGroupInfo(permissionGroup, flags);
            int callingUserId = UserHandle.getUserId(callingUid);
            if (this.mPackageManagerInt.filterAppAccess(permissionGroupInfo.packageName, callingUid, callingUserId)) {
                EventLog.writeEvent(1397638484, "186113473", Integer.valueOf(callingUid), groupName);
                return null;
            }
            return permissionGroupInfo;
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public PermissionInfo getPermissionInfo(String permName, String opPackageName, int flags) {
        int callingUid = Binder.getCallingUid();
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        AndroidPackage opPackage = this.mPackageManagerInt.getPackage(opPackageName);
        int targetSdkVersion = getPermissionInfoCallingTargetSdkVersion(opPackage, callingUid);
        synchronized (this.mLock) {
            Permission bp = this.mRegistry.getPermission(permName);
            if (bp == null) {
                return null;
            }
            PermissionInfo permissionInfo = bp.generatePermissionInfo(flags, targetSdkVersion);
            int callingUserId = UserHandle.getUserId(callingUid);
            if (this.mPackageManagerInt.filterAppAccess(permissionInfo.packageName, callingUid, callingUserId)) {
                EventLog.writeEvent(1397638484, "183122164", Integer.valueOf(callingUid), permName);
                return null;
            }
            return permissionInfo;
        }
    }

    private int getPermissionInfoCallingTargetSdkVersion(AndroidPackage pkg, int uid) {
        int appId = UserHandle.getAppId(uid);
        if (appId == 0 || appId == 1000 || appId == 2000 || pkg == null) {
            return 10000;
        }
        return pkg.getTargetSdkVersion();
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public List<PermissionInfo> queryPermissionsByGroup(String groupName, int flags) {
        final int callingUid = Binder.getCallingUid();
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        List<PermissionInfo> out = new ArrayList<>(10);
        synchronized (this.mLock) {
            if (groupName != null) {
                if (this.mRegistry.getPermissionGroup(groupName) == null) {
                    return null;
                }
            }
            for (Permission bp : this.mRegistry.getPermissions()) {
                if (Objects.equals(bp.getGroup(), groupName)) {
                    out.add(bp.generatePermissionInfo(flags));
                }
            }
            final int callingUserId = UserHandle.getUserId(callingUid);
            out.removeIf(new Predicate() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda4
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return PermissionManagerServiceImpl.this.m5843xfc07fe5d(callingUid, callingUserId, (PermissionInfo) obj);
                }
            });
            return out;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$queryPermissionsByGroup$1$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ boolean m5843xfc07fe5d(int callingUid, int callingUserId, PermissionInfo it) {
        return this.mPackageManagerInt.filterAppAccess(it.packageName, callingUid, callingUserId);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public boolean addPermission(PermissionInfo info, boolean async) {
        boolean added;
        boolean changed;
        int callingUid = Binder.getCallingUid();
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            throw new SecurityException("Instant apps can't add permissions");
        }
        if (info.labelRes == 0 && info.nonLocalizedLabel == null) {
            throw new SecurityException("Label must be specified in permission");
        }
        synchronized (this.mLock) {
            Permission tree = this.mRegistry.enforcePermissionTree(info.name, callingUid);
            Permission bp = this.mRegistry.getPermission(info.name);
            added = bp == null;
            int fixedLevel = PermissionInfo.fixProtectionLevel(info.protectionLevel);
            enforcePermissionCapLocked(info, tree);
            if (added) {
                bp = new Permission(info.name, tree.getPackageName(), 2);
            } else if (!bp.isDynamic()) {
                throw new SecurityException("Not allowed to modify non-dynamic permission " + info.name);
            }
            changed = bp.addToTree(fixedLevel, info, tree);
            if (added) {
                this.mRegistry.addPermission(bp);
            }
        }
        if (changed) {
            this.mPackageManagerInt.writeSettings(async);
        }
        return added;
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void removePermission(String permName) {
        int callingUid = Binder.getCallingUid();
        if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        synchronized (this.mLock) {
            this.mRegistry.enforcePermissionTree(permName, callingUid);
            Permission bp = this.mRegistry.getPermission(permName);
            if (bp == null) {
                return;
            }
            if (bp.isDynamic()) {
                Slog.wtf(TAG, "Not allowed to modify non-dynamic permission " + permName);
            }
            this.mRegistry.removePermission(permName);
            this.mPackageManagerInt.writeSettings(false);
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public int getPermissionFlags(String packageName, String permName, int userId) {
        int callingUid = Binder.getCallingUid();
        return getPermissionFlagsInternal(packageName, permName, callingUid, userId);
    }

    private int getPermissionFlagsInternal(String packageName, String permName, int callingUid, int userId) {
        if (this.mUserManagerInt.exists(userId)) {
            enforceGrantRevokeGetRuntimePermissionPermissions("getPermissionFlags");
            enforceCrossUserPermission(callingUid, userId, true, false, "getPermissionFlags");
            AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
            if (pkg == null || this.mPackageManagerInt.filterAppAccess(pkg, callingUid, userId)) {
                return 0;
            }
            synchronized (this.mLock) {
                if (this.mRegistry.getPermission(permName) == null) {
                    return 0;
                }
                UidPermissionState uidState = getUidStateLocked(pkg, userId);
                if (uidState == null) {
                    Slog.e(TAG, "Missing permissions state for " + packageName + " and user " + userId);
                    return 0;
                }
                return uidState.getPermissionFlags(permName);
            }
        }
        return 0;
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void updatePermissionFlags(String packageName, String permName, int flagMask, int flagValues, boolean checkAdjustPolicyFlagPermission, int userId) {
        boolean overridePolicy;
        int callingUid = Binder.getCallingUid();
        boolean overridePolicy2 = false;
        if (callingUid != 1000 && callingUid != 0) {
            long callingIdentity = Binder.clearCallingIdentity();
            if ((flagMask & 4) != 0) {
                try {
                    if (checkAdjustPolicyFlagPermission) {
                        this.mContext.enforceCallingOrSelfPermission("android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY", "Need android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY to change policy flags");
                    } else if (this.mPackageManagerInt.getUidTargetSdkVersion(callingUid) >= 29) {
                        throw new IllegalArgumentException("android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY needs  to be checked for packages targeting 29 or later when changing policy flags");
                    }
                    overridePolicy2 = true;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(callingIdentity);
                    throw th;
                }
            }
            Binder.restoreCallingIdentity(callingIdentity);
            overridePolicy = overridePolicy2;
        } else {
            overridePolicy = false;
        }
        updatePermissionFlagsInternal(packageName, permName, flagMask, flagValues, callingUid, userId, overridePolicy, this.mDefaultPermissionCallback);
    }

    private void updatePermissionFlagsInternal(String packageName, String permName, int flagMask, int flagValues, int callingUid, int userId, boolean overridePolicy, PermissionCallback callback) {
        int flagValues2;
        int flagValues3;
        boolean isRequested;
        boolean isRequested2;
        boolean isRequested3;
        if (this.mUserManagerInt.exists(userId)) {
            enforceGrantRevokeRuntimePermissionPermissions("updatePermissionFlags");
            enforceCrossUserPermission(callingUid, userId, true, true, "updatePermissionFlags");
            if ((flagMask & 4) != 0 && !overridePolicy) {
                throw new SecurityException("updatePermissionFlags requires android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY");
            }
            if (callingUid == 1000) {
                flagValues2 = flagMask;
                flagValues3 = flagValues;
            } else {
                int flagMask2 = flagMask & (-17) & (-33);
                int flagValues4 = flagValues & (-17) & (-33) & (-4097) & (-2049) & (-8193) & (-16385);
                if (!"android.permission.POST_NOTIFICATIONS".equals(permName) && callingUid != 2000 && callingUid != 0) {
                    flagValues3 = flagValues4 & (-65);
                    flagValues2 = flagMask2;
                } else {
                    flagValues3 = flagValues4;
                    flagValues2 = flagMask2;
                }
            }
            AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
            PackageStateInternal ps = this.mPackageManagerInt.getPackageStateInternal(packageName);
            if (pkg != null && ps != null) {
                if (this.mPackageManagerInt.filterAppAccess(pkg, callingUid, userId)) {
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                }
                boolean isRequested4 = false;
                if (pkg.getRequestedPermissions().contains(permName)) {
                    isRequested4 = true;
                }
                if (isRequested4) {
                    isRequested = isRequested4;
                } else {
                    String[] sharedUserPackageNames = this.mPackageManagerInt.getSharedUserPackagesForPackage(packageName, userId);
                    int length = sharedUserPackageNames.length;
                    int i = 0;
                    while (i < length) {
                        String sharedUserPackageName = sharedUserPackageNames[i];
                        AndroidPackage sharedUserPkg = this.mPackageManagerInt.getPackage(sharedUserPackageName);
                        if (sharedUserPkg == null) {
                            isRequested3 = isRequested4;
                        } else {
                            isRequested3 = isRequested4;
                            if (sharedUserPkg.getRequestedPermissions().contains(permName)) {
                                isRequested2 = true;
                                break;
                            }
                        }
                        i++;
                        isRequested4 = isRequested3;
                    }
                    isRequested = isRequested4;
                }
                isRequested2 = isRequested;
                synchronized (this.mLock) {
                    try {
                        try {
                            Permission bp = this.mRegistry.getPermission(permName);
                            if (bp == null) {
                                throw new IllegalArgumentException("Unknown permission: " + permName);
                            }
                            boolean isRuntimePermission = bp.isRuntime();
                            UidPermissionState uidState = getUidStateLocked(pkg, userId);
                            if (uidState == null) {
                                Slog.e(TAG, "Missing permissions state for " + packageName + " and user " + userId);
                                return;
                            } else if (!uidState.hasPermissionState(permName) && !isRequested2) {
                                Log.e(TAG, "Permission " + permName + " isn't requested by package " + packageName);
                                return;
                            } else {
                                boolean permissionUpdated = uidState.updatePermissionFlags(bp, flagValues2, flagValues3);
                                if (permissionUpdated && isRuntimePermission) {
                                    notifyRuntimePermissionStateChanged(packageName, userId);
                                }
                                if (permissionUpdated && callback != null) {
                                    if (!isRuntimePermission) {
                                        int userUid = UserHandle.getUid(userId, pkg.getUid());
                                        callback.onInstallPermissionUpdatedNotifyListener(userUid);
                                        return;
                                    }
                                    callback.onPermissionUpdatedNotifyListener(new int[]{userId}, false, pkg.getUid());
                                    return;
                                }
                                return;
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
            Log.e(TAG, "Unknown package: " + packageName);
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void updatePermissionFlagsForAllApps(int flagMask, int flagValues, final int userId) {
        int callingUid = Binder.getCallingUid();
        if (!this.mUserManagerInt.exists(userId)) {
            return;
        }
        enforceGrantRevokeRuntimePermissionPermissions("updatePermissionFlagsForAllApps");
        enforceCrossUserPermission(callingUid, userId, true, true, "updatePermissionFlagsForAllApps");
        final int effectiveFlagMask = callingUid != 1000 ? flagMask : flagMask & (-17);
        final int effectiveFlagValues = callingUid != 1000 ? flagValues : flagValues & (-17);
        final boolean[] changed = new boolean[1];
        this.mPackageManagerInt.forEachPackage(new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PermissionManagerServiceImpl.this.m5848x573aaf6b(userId, changed, effectiveFlagMask, effectiveFlagValues, (AndroidPackage) obj);
            }
        });
        if (changed[0]) {
            this.mPackageManagerInt.writePermissionSettings(new int[]{userId}, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updatePermissionFlagsForAllApps$2$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5848x573aaf6b(int userId, boolean[] changed, int effectiveFlagMask, int effectiveFlagValues, AndroidPackage pkg) {
        synchronized (this.mLock) {
            UidPermissionState uidState = getUidStateLocked(pkg, userId);
            if (uidState == null) {
                Slog.e(TAG, "Missing permissions state for " + pkg.getPackageName() + " and user " + userId);
                return;
            }
            changed[0] = changed[0] | uidState.updatePermissionFlagsForAllPermissions(effectiveFlagMask, effectiveFlagValues);
            this.mOnPermissionChangeListeners.onPermissionsChanged(pkg.getUid());
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public int checkPermission(String pkgName, String permName, int userId) {
        AndroidPackage pkg;
        if (this.mUserManagerInt.exists(userId) && (pkg = this.mPackageManagerInt.getPackage(pkgName)) != null) {
            return checkPermissionInternal(pkg, true, permName, userId);
        }
        return -1;
    }

    private int checkPermissionInternal(AndroidPackage pkg, boolean isPackageExplicit, String permissionName, int userId) {
        String fullerPermissionName;
        int callingUid = Binder.getCallingUid();
        if (isPackageExplicit || pkg.getSharedUserId() == null) {
            if (this.mPackageManagerInt.filterAppAccess(pkg, callingUid, userId)) {
                return -1;
            }
        } else if (this.mPackageManagerInt.getInstantAppPackageName(callingUid) != null) {
            return -1;
        }
        int uid = UserHandle.getUid(userId, pkg.getUid());
        Integer ret = ITranPackageManagerService.Instance().checkUidPermission(permissionName, pkg.getUid(), pkg);
        if (ret != null) {
            return ret.intValue();
        }
        boolean isInstantApp = this.mPackageManagerInt.getInstantAppPackageName(uid) != null;
        synchronized (this.mLock) {
            UidPermissionState uidState = getUidStateLocked(pkg, userId);
            if (uidState == null) {
                Slog.e(TAG, "Missing permissions state for " + pkg.getPackageName() + " and user " + userId);
                return -1;
            } else if (checkSinglePermissionInternalLocked(uidState, permissionName, isInstantApp)) {
                return 0;
            } else {
                return (CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported() || (fullerPermissionName = FULLER_PERMISSION_MAP.get(permissionName)) == null || !checkSinglePermissionInternalLocked(uidState, fullerPermissionName, isInstantApp)) ? -1 : 0;
            }
        }
    }

    private boolean checkSinglePermissionInternalLocked(UidPermissionState uidState, String permissionName, boolean isInstantApp) {
        if (uidState.isPermissionGranted(permissionName)) {
            if (!isInstantApp) {
                return true;
            }
            Permission permission = this.mRegistry.getPermission(permissionName);
            return permission != null && permission.isInstant();
        }
        return false;
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public int checkUidPermission(int uid, String permName) {
        int userId = UserHandle.getUserId(uid);
        if (!this.mUserManagerInt.exists(userId)) {
            return -1;
        }
        AndroidPackage pkg = this.mPackageManagerInt.getPackage(uid);
        return checkUidPermissionInternal(pkg, uid, permName);
    }

    private int checkUidPermissionInternal(AndroidPackage pkg, int uid, String permissionName) {
        String fullerPermissionName;
        if (pkg != null) {
            int userId = UserHandle.getUserId(uid);
            return checkPermissionInternal(pkg, false, permissionName, userId);
        }
        synchronized (this.mLock) {
            if (checkSingleUidPermissionInternalLocked(uid, permissionName)) {
                return 0;
            }
            if (!CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported() && (fullerPermissionName = FULLER_PERMISSION_MAP.get(permissionName)) != null && checkSingleUidPermissionInternalLocked(uid, fullerPermissionName)) {
                return 0;
            }
            return -1;
        }
    }

    private boolean checkSingleUidPermissionInternalLocked(int uid, String permissionName) {
        ArraySet<String> permissions = this.mSystemPermissions.get(uid);
        return permissions != null && permissions.contains(permissionName);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void addOnPermissionsChangeListener(IOnPermissionsChangeListener listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.OBSERVE_GRANT_REVOKE_PERMISSIONS", "addOnPermissionsChangeListener");
        this.mOnPermissionChangeListeners.addListener(listener);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void removeOnPermissionsChangeListener(IOnPermissionsChangeListener listener) {
        if (this.mPackageManagerInt.getInstantAppPackageName(Binder.getCallingUid()) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        this.mOnPermissionChangeListeners.removeListener(listener);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public List<String> getAllowlistedRestrictedPermissions(String packageName, int flags, int userId) {
        Objects.requireNonNull(packageName);
        Preconditions.checkFlagsArgument(flags, 7);
        Preconditions.checkArgumentNonNegative(userId, (String) null);
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", "getAllowlistedRestrictedPermissions for user " + userId);
        }
        AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
        if (pkg == null) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        if (this.mPackageManagerInt.filterAppAccess(pkg, callingUid, UserHandle.getCallingUserId())) {
            return null;
        }
        boolean isCallerPrivileged = this.mContext.checkCallingOrSelfPermission("android.permission.WHITELIST_RESTRICTED_PERMISSIONS") == 0;
        boolean isCallerInstallerOnRecord = this.mPackageManagerInt.isCallerInstallerOfRecord(pkg, callingUid);
        if ((flags & 1) != 0 && !isCallerPrivileged) {
            throw new SecurityException("Querying system allowlist requires android.permission.WHITELIST_RESTRICTED_PERMISSIONS");
        }
        if ((flags & 6) != 0 && !isCallerPrivileged && !isCallerInstallerOnRecord) {
            throw new SecurityException("Querying upgrade or installer allowlist requires being installer on record or android.permission.WHITELIST_RESTRICTED_PERMISSIONS");
        }
        long identity = Binder.clearCallingIdentity();
        try {
            return getAllowlistedRestrictedPermissionsInternal(pkg, flags, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private List<String> getAllowlistedRestrictedPermissionsInternal(AndroidPackage pkg, int flags, int userId) {
        synchronized (this.mLock) {
            UidPermissionState uidState = getUidStateLocked(pkg, userId);
            if (uidState == null) {
                Slog.e(TAG, "Missing permissions state for " + pkg.getPackageName() + " and user " + userId);
                return null;
            }
            int queryFlags = 0;
            if ((flags & 1) != 0) {
                queryFlags = 0 | 4096;
            }
            if ((flags & 4) != 0) {
                queryFlags |= 8192;
            }
            if ((flags & 2) != 0) {
                queryFlags |= 2048;
            }
            ArrayList<String> allowlistedPermissions = null;
            int permissionCount = ArrayUtils.size(pkg.getRequestedPermissions());
            for (int i = 0; i < permissionCount; i++) {
                String permissionName = pkg.getRequestedPermissions().get(i);
                int currentFlags = uidState.getPermissionFlags(permissionName);
                if ((currentFlags & queryFlags) != 0) {
                    if (allowlistedPermissions == null) {
                        allowlistedPermissions = new ArrayList<>();
                    }
                    allowlistedPermissions.add(permissionName);
                }
            }
            return allowlistedPermissions;
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public boolean addAllowlistedRestrictedPermission(String packageName, String permName, int flags, int userId) {
        Objects.requireNonNull(permName);
        if (checkExistsAndEnforceCannotModifyImmutablyRestrictedPermission(permName)) {
            List<String> permissions = getAllowlistedRestrictedPermissions(packageName, flags, userId);
            if (permissions == null) {
                permissions = new ArrayList(1);
            }
            if (permissions.indexOf(permName) < 0) {
                permissions.add(permName);
                return setAllowlistedRestrictedPermissions(packageName, permissions, flags, userId);
            }
            return false;
        }
        return false;
    }

    private boolean checkExistsAndEnforceCannotModifyImmutablyRestrictedPermission(String permName) {
        synchronized (this.mLock) {
            Permission bp = this.mRegistry.getPermission(permName);
            if (bp == null) {
                Slog.w(TAG, "No such permissions: " + permName);
                return false;
            }
            String permissionPackageName = bp.getPackageName();
            Permission bp2 = (bp.isHardOrSoftRestricted() && bp.isImmutablyRestricted()) ? 1 : null;
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getUserId(callingUid);
            if (this.mPackageManagerInt.filterAppAccess(permissionPackageName, callingUid, callingUserId)) {
                EventLog.writeEvent(1397638484, "186404356", Integer.valueOf(callingUid), permName);
                return false;
            } else if (bp2 == null || this.mContext.checkCallingOrSelfPermission("android.permission.WHITELIST_RESTRICTED_PERMISSIONS") == 0) {
                return true;
            } else {
                throw new SecurityException("Cannot modify allowlisting of an immutably restricted permission: " + permName);
            }
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public boolean removeAllowlistedRestrictedPermission(String packageName, String permName, int flags, int userId) {
        List<String> permissions;
        Objects.requireNonNull(permName);
        if (checkExistsAndEnforceCannotModifyImmutablyRestrictedPermission(permName) && (permissions = getAllowlistedRestrictedPermissions(packageName, flags, userId)) != null && permissions.remove(permName)) {
            return setAllowlistedRestrictedPermissions(packageName, permissions, flags, userId);
        }
        return false;
    }

    private boolean setAllowlistedRestrictedPermissions(String packageName, List<String> permissions, int flags, int userId) {
        Objects.requireNonNull(packageName);
        Preconditions.checkFlagsArgument(flags, 7);
        Preconditions.checkArgument(Integer.bitCount(flags) == 1);
        Preconditions.checkArgumentNonNegative(userId, (String) null);
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", "setAllowlistedRestrictedPermissions for user " + userId);
        }
        AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
        if (pkg == null) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        if (this.mPackageManagerInt.filterAppAccess(pkg, callingUid, UserHandle.getCallingUserId())) {
            return false;
        }
        boolean isCallerPrivileged = this.mContext.checkCallingOrSelfPermission("android.permission.WHITELIST_RESTRICTED_PERMISSIONS") == 0;
        boolean isCallerInstallerOnRecord = this.mPackageManagerInt.isCallerInstallerOfRecord(pkg, callingUid);
        if ((flags & 1) != 0 && !isCallerPrivileged) {
            throw new SecurityException("Modifying system allowlist requires android.permission.WHITELIST_RESTRICTED_PERMISSIONS");
        }
        if ((flags & 4) != 0) {
            if (!isCallerPrivileged && !isCallerInstallerOnRecord) {
                throw new SecurityException("Modifying upgrade allowlist requires being installer on record or android.permission.WHITELIST_RESTRICTED_PERMISSIONS");
            }
            List<String> allowlistedPermissions = getAllowlistedRestrictedPermissions(pkg.getPackageName(), flags, userId);
            if (permissions == null || permissions.isEmpty()) {
                if (allowlistedPermissions == null || allowlistedPermissions.isEmpty()) {
                    return true;
                }
            } else {
                int permissionCount = permissions.size();
                for (int i = 0; i < permissionCount; i++) {
                    if ((allowlistedPermissions == null || !allowlistedPermissions.contains(permissions.get(i))) && !isCallerPrivileged) {
                        throw new SecurityException("Adding to upgrade allowlist requiresandroid.permission.WHITELIST_RESTRICTED_PERMISSIONS");
                    }
                }
            }
            if ((flags & 2) != 0 && !isCallerPrivileged && !isCallerInstallerOnRecord) {
                throw new SecurityException("Modifying installer allowlist requires being installer on record or android.permission.WHITELIST_RESTRICTED_PERMISSIONS");
            }
        }
        long identity = Binder.clearCallingIdentity();
        try {
            setAllowlistedRestrictedPermissionsInternal(pkg, permissions, flags, userId);
            return true;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void grantRuntimePermission(String packageName, String permName, int userId) {
        int callingUid = Binder.getCallingUid();
        boolean overridePolicy = checkUidPermission(callingUid, "android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY") == 0;
        grantRuntimePermissionInternal(packageName, permName, overridePolicy, callingUid, userId, this.mDefaultPermissionCallback);
    }

    /* JADX WARN: Removed duplicated region for block: B:127:0x02f8  */
    /* JADX WARN: Removed duplicated region for block: B:130:0x0307  */
    /* JADX WARN: Removed duplicated region for block: B:136:0x031f  */
    /* JADX WARN: Removed duplicated region for block: B:164:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void grantRuntimePermissionInternal(String packageName, String permName, boolean overridePolicy, int callingUid, int userId, PermissionCallback callback) {
        boolean isRolePermission;
        boolean isSoftRestrictedPermission;
        if (!this.mUserManagerInt.exists(userId)) {
            Log.e(TAG, "No such user:" + userId);
            return;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS", "grantRuntimePermission");
        enforceCrossUserPermission(callingUid, userId, true, true, "grantRuntimePermission");
        AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
        PackageStateInternal ps = this.mPackageManagerInt.getPackageStateInternal(packageName);
        if (pkg == null || ps == null) {
            Log.e(TAG, "Unknown package: " + packageName);
        } else if (this.mPackageManagerInt.filterAppAccess(pkg, callingUid, userId)) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        } else {
            synchronized (this.mLock) {
                Permission permission = this.mRegistry.getPermission(permName);
                if (permission == null) {
                    throw new IllegalArgumentException("Unknown permission: " + permName);
                }
                isRolePermission = permission.isRole();
                isSoftRestrictedPermission = permission.isSoftRestricted();
            }
            boolean z = true;
            boolean mayGrantRolePermission = isRolePermission && mayManageRolePermission(callingUid);
            if (!isSoftRestrictedPermission || !SoftRestrictedPermissionPolicy.forPermission(this.mContext, AndroidPackageUtils.generateAppInfoWithoutState(pkg), pkg, UserHandle.of(userId), permName).mayGrantPermission()) {
                z = false;
            }
            boolean mayGrantSoftRestrictedPermission = z;
            synchronized (this.mLock) {
                try {
                    try {
                        Permission bp = this.mRegistry.getPermission(permName);
                        if (bp == null) {
                            throw new IllegalArgumentException("Unknown permission: " + permName);
                        }
                        boolean isRuntimePermission = bp.isRuntime();
                        boolean permissionHasGids = bp.hasGids();
                        if (!isRuntimePermission && !bp.isDevelopment()) {
                            if (bp.isRole()) {
                                if (!mayGrantRolePermission) {
                                    try {
                                    } catch (Throwable th) {
                                        th = th;
                                    }
                                    try {
                                        throw new SecurityException("Permission " + permName + " is managed by role");
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                }
                            } else {
                                throw new SecurityException("Permission " + permName + " requested by " + pkg.getPackageName() + " is not a changeable permission type");
                            }
                        }
                        UidPermissionState uidState = getUidStateLocked(pkg, userId);
                        if (uidState == null) {
                            Slog.e(TAG, "Missing permissions state for " + pkg.getPackageName() + " and user " + userId);
                            return;
                        }
                        if (!uidState.hasPermissionState(permName) && !pkg.getRequestedPermissions().contains(permName)) {
                            throw new SecurityException("Package " + pkg.getPackageName() + " has not requested permission " + permName);
                        }
                        if (pkg.getTargetSdkVersion() < 23 && bp.isRuntime()) {
                            return;
                        }
                        int flags = uidState.getPermissionFlags(permName);
                        if ((flags & 16) != 0) {
                            Log.e(TAG, "Cannot grant system fixed permission " + permName + " for package " + packageName);
                        } else if (!overridePolicy && (flags & 4) != 0) {
                            Log.e(TAG, "Cannot grant policy fixed permission " + permName + " for package " + packageName);
                        } else if (bp.isHardRestricted() && (flags & 14336) == 0) {
                            Log.e(TAG, "Cannot grant hard restricted non-exempt permission " + permName + " for package " + packageName);
                        } else if (bp.isSoftRestricted() && !mayGrantSoftRestrictedPermission) {
                            Log.e(TAG, "Cannot grant soft restricted permission " + permName + " for package " + packageName);
                        } else {
                            if (!bp.isDevelopment() && !bp.isRole()) {
                                if (ps.getUserStateOrDefault(userId).isInstantApp() && !bp.isInstant()) {
                                    throw new SecurityException("Cannot grant non-ephemeral permission " + permName + " for package " + packageName);
                                }
                                if (pkg.getTargetSdkVersion() < 23) {
                                    Slog.w(TAG, "Cannot grant runtime permission to a legacy app");
                                    return;
                                }
                                if (!uidState.grantPermission(bp)) {
                                    return;
                                }
                                Slog.d(TAG, permName + " is granted, packageName: " + packageName + ", callingUid: " + callingUid + ", userId: " + userId);
                                if (bp.isRuntime()) {
                                    logPermission(1243, permName, packageName);
                                }
                                int uid = UserHandle.getUid(userId, pkg.getUid());
                                if (callback != null) {
                                    if (isRuntimePermission) {
                                        callback.onPermissionGranted(uid, userId);
                                    } else {
                                        callback.onInstallPermissionGranted();
                                    }
                                    if (permissionHasGids) {
                                        callback.onGidsChanged(UserHandle.getAppId(pkg.getUid()), userId);
                                    }
                                }
                                if (!isRuntimePermission) {
                                    notifyRuntimePermissionStateChanged(packageName, userId);
                                    return;
                                }
                                return;
                            }
                            if (!uidState.grantPermission(bp)) {
                                return;
                            }
                            Slog.d(TAG, permName + " is granted, packageName: " + packageName + ", callingUid: " + callingUid + ", userId: " + userId);
                            if (bp.isRuntime()) {
                            }
                            int uid2 = UserHandle.getUid(userId, pkg.getUid());
                            if (callback != null) {
                            }
                            if (!isRuntimePermission) {
                            }
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } catch (Throwable th4) {
                    th = th4;
                }
            }
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void revokeRuntimePermission(String packageName, String permName, int userId, String reason) {
        int callingUid = Binder.getCallingUid();
        boolean overridePolicy = checkUidPermission(callingUid, "android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY") == 0;
        revokeRuntimePermissionInternal(packageName, permName, overridePolicy, callingUid, userId, reason, this.mDefaultPermissionCallback);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void revokePostNotificationPermissionWithoutKillForTest(String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        boolean overridePolicy = checkUidPermission(callingUid, "android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY") == 0;
        this.mContext.enforceCallingPermission("android.permission.REVOKE_POST_NOTIFICATIONS_WITHOUT_KILL", "");
        revokeRuntimePermissionInternal(packageName, "android.permission.POST_NOTIFICATIONS", overridePolicy, true, callingUid, userId, SKIP_KILL_APP_REASON_NOTIFICATION_TEST, this.mDefaultPermissionCallback);
    }

    private void revokeRuntimePermissionInternal(String packageName, String permName, boolean overridePolicy, int callingUid, int userId, String reason, PermissionCallback callback) {
        revokeRuntimePermissionInternal(packageName, permName, overridePolicy, false, callingUid, userId, reason, callback);
    }

    /* JADX WARN: Code restructure failed: missing block: B:69:0x01ab, code lost:
        if ((r3 & 4) != 0) goto L72;
     */
    /* JADX WARN: Code restructure failed: missing block: B:72:0x01d0, code lost:
        throw new java.lang.SecurityException("Cannot revoke policy fixed permission " + r18 + " for package " + r17);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void revokeRuntimePermissionInternal(String packageName, String permName, boolean overridePolicy, boolean overrideKill, int callingUid, int userId, String reason, PermissionCallback callback) {
        boolean isRolePermission;
        if (!this.mUserManagerInt.exists(userId)) {
            Log.e(TAG, "No such user:" + userId);
            return;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS", "revokeRuntimePermission");
        enforceCrossUserPermission(callingUid, userId, true, true, "revokeRuntimePermission");
        AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
        if (pkg == null) {
            Log.e(TAG, "Unknown package: " + packageName);
        } else if (this.mPackageManagerInt.filterAppAccess(pkg, callingUid, userId)) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        } else {
            synchronized (this.mLock) {
                Permission permission = this.mRegistry.getPermission(permName);
                if (permission == null) {
                    throw new IllegalArgumentException("Unknown permission: " + permName);
                }
                isRolePermission = permission.isRole();
            }
            boolean mayRevokeRolePermission = isRolePermission && (callingUid == Process.myUid() || mayManageRolePermission(callingUid));
            synchronized (this.mLock) {
                Permission bp = this.mRegistry.getPermission(permName);
                if (bp == null) {
                    throw new IllegalArgumentException("Unknown permission: " + permName);
                }
                boolean isRuntimePermission = bp.isRuntime();
                if (!isRuntimePermission && !bp.isDevelopment()) {
                    if (bp.isRole()) {
                        if (!mayRevokeRolePermission) {
                            throw new SecurityException("Permission " + permName + " is managed by role");
                        }
                    } else {
                        throw new SecurityException("Permission " + permName + " requested by " + pkg.getPackageName() + " is not a changeable permission type");
                    }
                }
                UidPermissionState uidState = getUidStateLocked(pkg, userId);
                if (uidState == null) {
                    Slog.e(TAG, "Missing permissions state for " + pkg.getPackageName() + " and user " + userId);
                    return;
                }
                if (!uidState.hasPermissionState(permName) && !pkg.getRequestedPermissions().contains(permName)) {
                    throw new SecurityException("Package " + pkg.getPackageName() + " has not requested permission " + permName);
                }
                if (pkg.getTargetSdkVersion() >= 23 || !bp.isRuntime()) {
                    int flags = uidState.getPermissionFlags(permName);
                    if ((flags & 16) != 0 && UserHandle.getCallingAppId() != 1000) {
                        throw new SecurityException("Non-System UID cannot revoke system fixed permission " + permName + " for package " + packageName);
                    }
                    if (uidState.revokePermission(bp)) {
                        if (isRuntimePermission) {
                            logPermission(1245, permName, packageName);
                        }
                        if (callback != null) {
                            if (isRuntimePermission) {
                                callback.onPermissionRevoked(UserHandle.getUid(userId, pkg.getUid()), userId, reason, overrideKill, permName);
                            } else {
                                this.mDefaultPermissionCallback.onInstallPermissionRevoked();
                            }
                        }
                        if (isRuntimePermission) {
                            notifyRuntimePermissionStateChanged(packageName, userId);
                        }
                    }
                }
            }
        }
    }

    private boolean mayManageRolePermission(int uid) {
        PackageManager packageManager = this.mContext.getPackageManager();
        String[] packageNames = packageManager.getPackagesForUid(uid);
        if (packageNames == null) {
            return false;
        }
        String permissionControllerPackageName = packageManager.getPermissionControllerPackageName();
        return Arrays.asList(packageNames).contains(permissionControllerPackageName);
    }

    /* JADX WARN: Code restructure failed: missing block: B:15:0x0067, code lost:
        r25 = r9;
        r20 = r10;
        r26 = r11;
        r21 = r12;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x0078, code lost:
        r8 = r27.mPackageManagerInt.getSharedUserPackagesForPackage(r28.getPackageName(), r13);
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0083, code lost:
        if (r8.length <= 0) goto L38;
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0085, code lost:
        r6 = false;
        r7 = r8.length;
        r0 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x0088, code lost:
        if (r0 >= r7) goto L36;
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x008a, code lost:
        r2 = r8[r0];
        r20 = r6;
        r6 = r27.mPackageManagerInt.getPackage(r2);
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0094, code lost:
        if (r6 == null) goto L34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0096, code lost:
        r2 = r6.getPackageName();
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x00a0, code lost:
        if (r2.equals(r12) != false) goto L33;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x00aa, code lost:
        if (r6.getRequestedPermissions().contains(r3) == false) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x00ac, code lost:
        r6 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x00b1, code lost:
        r0 = r0 + 1;
        r6 = r20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x00b8, code lost:
        if (r6 == false) goto L38;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x00ba, code lost:
        r25 = r9;
        r20 = r10;
        r26 = r11;
        r21 = r12;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x00c4, code lost:
        r0 = getPermissionFlagsInternal(r12, r3, 1000, r13);
        r2 = r27.mPackageManagerInt.getPackageUid(r12, 0, r13);
        r7 = r27.mPackageManagerInt.getUidTargetSdkVersion(r2);
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00da, code lost:
        if (r7 >= 23) goto L66;
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00dc, code lost:
        if (r8 == false) goto L66;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00de, code lost:
        r19 = 72;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x00e3, code lost:
        r19 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x00e5, code lost:
        r25 = r9;
        r20 = r10;
        r26 = r11;
        r21 = r12;
        updatePermissionFlagsInternal(r12, r3, 589899, r19, 1000, r29, false, r14);
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x010a, code lost:
        if (r8 != false) goto L44;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x010f, code lost:
        if ((r0 & 20) == 0) goto L46;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x0114, code lost:
        if ((r0 & 32) != 0) goto L63;
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x011a, code lost:
        if ((32768 & r0) == 0) goto L53;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x0121, code lost:
        if ((r19 & 64) != 0) goto L61;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x0129, code lost:
        if (isPermissionSplitFromNonRuntime(r3, r7) != false) goto L59;
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x012b, code lost:
        revokeRuntimePermissionInternal(r21, r3, false, 1000, r29, null, r14);
     */
    /* JADX WARN: Code restructure failed: missing block: B:58:0x0145, code lost:
        grantRuntimePermissionInternal(r21, r3, false, 1000, r29, r14);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void resetRuntimePermissionsInternal(AndroidPackage pkg, int userId) {
        int i = userId;
        String packageName = pkg.getPackageName();
        int permissionCount = ArrayUtils.size(pkg.getRequestedPermissions());
        final boolean[] permissionRemoved = new boolean[1];
        final ArraySet<Long> revokedPermissions = new ArraySet<>();
        final IntArray syncUpdatedUsers = new IntArray(permissionCount);
        final IntArray asyncUpdatedUsers = new IntArray(permissionCount);
        PermissionCallback delayingPermCallback = new PermissionCallback() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl.2
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            {
                super();
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onGidsChanged(int appId, int userId2) {
                PermissionManagerServiceImpl.this.mDefaultPermissionCallback.onGidsChanged(appId, userId2);
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onPermissionChanged() {
                PermissionManagerServiceImpl.this.mDefaultPermissionCallback.onPermissionChanged();
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onPermissionGranted(int uid, int userId2) {
                PermissionManagerServiceImpl.this.mDefaultPermissionCallback.onPermissionGranted(uid, userId2);
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onInstallPermissionGranted() {
                PermissionManagerServiceImpl.this.mDefaultPermissionCallback.onInstallPermissionGranted();
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onPermissionRevoked(int uid, int userId2, String reason) {
                revokedPermissions.add(Long.valueOf(IntPair.of(uid, userId2)));
                syncUpdatedUsers.add(userId2);
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onInstallPermissionRevoked() {
                PermissionManagerServiceImpl.this.mDefaultPermissionCallback.onInstallPermissionRevoked();
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onPermissionUpdated(int[] updatedUserIds, boolean sync) {
                for (int userId2 : updatedUserIds) {
                    if (sync) {
                        syncUpdatedUsers.add(userId2);
                        asyncUpdatedUsers.remove(userId2);
                    } else if (syncUpdatedUsers.indexOf(userId2) == -1) {
                        asyncUpdatedUsers.add(userId2);
                    }
                }
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onPermissionRemoved() {
                permissionRemoved[0] = true;
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onInstallPermissionUpdated() {
                PermissionManagerServiceImpl.this.mDefaultPermissionCallback.onInstallPermissionUpdated();
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onPermissionUpdatedNotifyListener(int[] updatedUserIds, boolean sync, int uid) {
                onPermissionUpdated(updatedUserIds, sync);
                PermissionManagerServiceImpl.this.mOnPermissionChangeListeners.onPermissionsChanged(uid);
            }

            @Override // com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback
            public void onInstallPermissionUpdatedNotifyListener(int uid) {
                PermissionManagerServiceImpl.this.mDefaultPermissionCallback.onInstallPermissionUpdatedNotifyListener(uid);
            }
        };
        int i2 = 0;
        while (i2 < permissionCount) {
            String permName = pkg.getRequestedPermissions().get(i2);
            synchronized (this.mLock) {
                try {
                    Permission permission = this.mRegistry.getPermission(permName);
                    if (permission != null) {
                        if (!permission.isRemoved()) {
                            boolean isRuntimePermission = permission.isRuntime();
                        }
                    } else {
                        try {
                        } catch (Throwable th) {
                            th = th;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            throw th;
                        }
                    }
                    i2++;
                    i = userId;
                    permissionRemoved = permissionRemoved;
                    packageName = packageName;
                    revokedPermissions = revokedPermissions;
                    permissionCount = permissionCount;
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            ArraySet<Long> revokedPermissions2 = revokedPermissions;
            boolean[] permissionRemoved2 = permissionRemoved;
            int permissionCount2 = permissionCount;
            String packageName2 = packageName;
            i2++;
            i = userId;
            permissionRemoved = permissionRemoved2;
            packageName = packageName2;
            revokedPermissions = revokedPermissions2;
            permissionCount = permissionCount2;
        }
        ArraySet<Long> revokedPermissions3 = revokedPermissions;
        if (permissionRemoved[0]) {
            this.mDefaultPermissionCallback.onPermissionRemoved();
        }
        if (!revokedPermissions3.isEmpty()) {
            int numRevokedPermissions = revokedPermissions3.size();
            for (int i3 = 0; i3 < numRevokedPermissions; i3++) {
                final int revocationUID = IntPair.first(revokedPermissions3.valueAt(i3).longValue());
                final int revocationUserId = IntPair.second(revokedPermissions3.valueAt(i3).longValue());
                this.mOnPermissionChangeListeners.onPermissionsChanged(revocationUID);
                this.mHandler.post(new Runnable() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        PermissionManagerServiceImpl.killUid(UserHandle.getAppId(revocationUID), revocationUserId, "permissions revoked");
                    }
                });
            }
        }
        this.mPackageManagerInt.writePermissionSettings(syncUpdatedUsers.toArray(), false);
        this.mPackageManagerInt.writePermissionSettings(asyncUpdatedUsers.toArray(), true);
    }

    private boolean isPermissionSplitFromNonRuntime(String permName, int targetSdk) {
        List<PermissionManager.SplitPermissionInfo> splitPerms = getSplitPermissionInfos();
        int size = splitPerms.size();
        int i = 0;
        while (true) {
            boolean z = false;
            if (i >= size) {
                return false;
            }
            PermissionManager.SplitPermissionInfo splitPerm = splitPerms.get(i);
            if (targetSdk >= splitPerm.getTargetSdk() || !splitPerm.getNewPermissions().contains(permName)) {
                i++;
            } else {
                synchronized (this.mLock) {
                    Permission perm = this.mRegistry.getPermission(splitPerm.getSplitPermission());
                    if (perm != null && !perm.isRuntime()) {
                        z = true;
                    }
                }
                return z;
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:53:0x00b1  */
    /* JADX WARN: Removed duplicated region for block: B:54:0x00b3 A[ORIG_RETURN, RETURN] */
    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean shouldShowRequestPermissionRationale(String packageName, String permName, int userId) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "canShowRequestPermissionRationale for user " + userId);
        }
        int uid = this.mPackageManagerInt.getPackageUid(packageName, 268435456L, userId);
        if (UserHandle.getAppId(callingUid) != UserHandle.getAppId(uid) || checkPermission(packageName, permName, userId) == 0) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            int flags = getPermissionFlagsInternal(packageName, permName, callingUid, userId);
            Binder.restoreCallingIdentity(token);
            if ((flags & 22) != 0) {
                return false;
            }
            synchronized (this.mLock) {
                try {
                    Permission permission = this.mRegistry.getPermission(permName);
                    try {
                        if (permission == null) {
                            return false;
                        }
                        if (permission.isHardRestricted() && (flags & 14336) == 0) {
                            return false;
                        }
                        token = Binder.clearCallingIdentity();
                        try {
                            try {
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } catch (RemoteException e) {
                            e = e;
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                        if (permName.equals("android.permission.ACCESS_BACKGROUND_LOCATION")) {
                            try {
                                if (this.mPlatformCompat.isChangeEnabledByPackageName((long) BACKGROUND_RATIONALE_CHANGE_ID, packageName, userId)) {
                                    return true;
                                }
                            } catch (RemoteException e2) {
                                e = e2;
                                Log.e(TAG, "Unable to check if compatibility change is enabled.", e);
                                if ((flags & 1) != 0) {
                                }
                            }
                            return (flags & 1) != 0;
                        } else if ((flags & 1) != 0) {
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th4) {
                                th = th4;
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public boolean isPermissionRevokedByPolicy(String packageName, String permName, int userId) {
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "isPermissionRevokedByPolicy for user " + userId);
        }
        if (checkPermission(packageName, permName, userId) == 0) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        if (this.mPackageManagerInt.filterAppAccess(packageName, callingUid, userId)) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            int flags = getPermissionFlagsInternal(packageName, permName, callingUid, userId);
            return (flags & 4) != 0;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public byte[] backupRuntimePermissions(int userId) {
        Preconditions.checkArgumentNonNegative(userId, "userId");
        final CompletableFuture<byte[]> backup = new CompletableFuture<>();
        PermissionControllerManager permissionControllerManager = this.mPermissionControllerManager;
        UserHandle of = UserHandle.of(userId);
        Executor mainExecutor = this.mContext.getMainExecutor();
        Objects.requireNonNull(backup);
        permissionControllerManager.getRuntimePermissionBackup(of, mainExecutor, new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                backup.complete((byte[]) obj);
            }
        });
        try {
            return backup.get(BACKUP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Slog.e(TAG, "Cannot create permission backup for user " + userId, e);
            return null;
        }
    }

    public void restoreRuntimePermissions(byte[] backup, int userId) {
        Objects.requireNonNull(backup, HostingRecord.HOSTING_TYPE_BACKUP);
        Preconditions.checkArgumentNonNegative(userId, "userId");
        synchronized (this.mLock) {
            this.mHasNoDelayedPermBackup.delete(userId);
        }
        this.mPermissionControllerManager.stageAndApplyRuntimePermissionsBackup(backup, UserHandle.of(userId));
    }

    public void restoreDelayedRuntimePermissions(String packageName, final int userId) {
        Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
        Preconditions.checkArgumentNonNegative(userId, "userId");
        synchronized (this.mLock) {
            if (this.mHasNoDelayedPermBackup.get(userId, false)) {
                return;
            }
            this.mPermissionControllerManager.applyStagedRuntimePermissionBackup(packageName, UserHandle.of(userId), this.mContext.getMainExecutor(), new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda11
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PermissionManagerServiceImpl.this.m5845xce236bae(userId, (Boolean) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$restoreDelayedRuntimePermissions$4$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5845xce236bae(int userId, Boolean hasMoreBackup) {
        if (hasMoreBackup.booleanValue()) {
            return;
        }
        synchronized (this.mLock) {
            this.mHasNoDelayedPermBackup.put(userId, true);
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void addOnRuntimePermissionStateChangedListener(PermissionManagerServiceInternal.OnRuntimePermissionStateChangedListener listener) {
        synchronized (this.mLock) {
            this.mRuntimePermissionStateChangedListeners.add(listener);
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void removeOnRuntimePermissionStateChangedListener(PermissionManagerServiceInternal.OnRuntimePermissionStateChangedListener listener) {
        synchronized (this.mLock) {
            this.mRuntimePermissionStateChangedListeners.remove(listener);
        }
    }

    private void notifyRuntimePermissionStateChanged(String packageName, int userId) {
        FgThread.getHandler().sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda13
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((PermissionManagerServiceImpl) obj).doNotifyRuntimePermissionStateChanged((String) obj2, ((Integer) obj3).intValue());
            }
        }, this, packageName, Integer.valueOf(userId)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void doNotifyRuntimePermissionStateChanged(String packageName, int userId) {
        synchronized (this.mLock) {
            if (this.mRuntimePermissionStateChangedListeners.isEmpty()) {
                return;
            }
            ArrayList<PermissionManagerServiceInternal.OnRuntimePermissionStateChangedListener> listeners = new ArrayList<>(this.mRuntimePermissionStateChangedListeners);
            int listenerCount = listeners.size();
            for (int i = 0; i < listenerCount; i++) {
                listeners.get(i).onRuntimePermissionStateChanged(packageName, userId);
            }
        }
    }

    private void revokeStoragePermissionsIfScopeExpandedInternal(AndroidPackage newPackage, AndroidPackage oldPackage) {
        PermissionInfo permInfo;
        int i;
        int numRequestedPermissions;
        int userId;
        int i2;
        int i3;
        int[] iArr;
        int numRequestedPermissions2 = 1;
        int userId2 = 0;
        boolean downgradedSdk = oldPackage.getTargetSdkVersion() >= 29 && newPackage.getTargetSdkVersion() < 29;
        boolean upgradedSdk = oldPackage.getTargetSdkVersion() < 29 && newPackage.getTargetSdkVersion() >= 29;
        boolean newlyRequestsLegacy = (upgradedSdk || oldPackage.isRequestLegacyExternalStorage() || !newPackage.isRequestLegacyExternalStorage()) ? false : true;
        if (!newlyRequestsLegacy && !downgradedSdk) {
            return;
        }
        int callingUid = Binder.getCallingUid();
        int[] allUserIds = getAllUserIds();
        int length = allUserIds.length;
        int i4 = 0;
        while (i4 < length) {
            int userId3 = allUserIds[i4];
            int numRequestedPermissions3 = newPackage.getRequestedPermissions().size();
            int i5 = 0;
            while (i5 < numRequestedPermissions3) {
                PermissionInfo permInfo2 = getPermissionInfo(newPackage.getRequestedPermissions().get(i5), newPackage.getPackageName(), userId2);
                if (permInfo2 == null) {
                    i = i5;
                    numRequestedPermissions = numRequestedPermissions3;
                    userId = userId3;
                    i2 = i4;
                    i3 = length;
                    iArr = allUserIds;
                } else {
                    if (((STORAGE_PERMISSIONS.contains(permInfo2.name) || READ_MEDIA_AURAL_PERMISSIONS.contains(permInfo2.name) || READ_MEDIA_VISUAL_PERMISSIONS.contains(permInfo2.name)) ? numRequestedPermissions2 : userId2) == 0) {
                        i = i5;
                        numRequestedPermissions = numRequestedPermissions3;
                        userId = userId3;
                        i2 = i4;
                        i3 = length;
                        iArr = allUserIds;
                    } else {
                        Object[] objArr = new Object[3];
                        objArr[userId2] = "171430330";
                        objArr[numRequestedPermissions2] = Integer.valueOf(newPackage.getUid());
                        objArr[2] = "Revoking permission " + permInfo2.name + " from package " + newPackage.getPackageName() + " as either the sdk downgraded " + downgradedSdk + " or newly requested legacy full storage " + newlyRequestsLegacy;
                        EventLog.writeEvent(1397638484, objArr);
                        try {
                            permInfo = permInfo2;
                            i = i5;
                            numRequestedPermissions = numRequestedPermissions3;
                            userId = userId3;
                            i2 = i4;
                            i3 = length;
                            iArr = allUserIds;
                        } catch (IllegalStateException | SecurityException e) {
                            e = e;
                            permInfo = permInfo2;
                            i = i5;
                            numRequestedPermissions = numRequestedPermissions3;
                            userId = userId3;
                            i2 = i4;
                            i3 = length;
                            iArr = allUserIds;
                        }
                        try {
                            revokeRuntimePermissionInternal(newPackage.getPackageName(), permInfo2.name, false, callingUid, userId, null, this.mDefaultPermissionCallback);
                        } catch (IllegalStateException | SecurityException e2) {
                            e = e2;
                            Log.e(TAG, "unable to revoke " + permInfo.name + " for " + newPackage.getPackageName() + " user " + userId, e);
                            i5 = i + 1;
                            numRequestedPermissions3 = numRequestedPermissions;
                            userId3 = userId;
                            allUserIds = iArr;
                            i4 = i2;
                            length = i3;
                            numRequestedPermissions2 = 1;
                            userId2 = 0;
                        }
                    }
                }
                i5 = i + 1;
                numRequestedPermissions3 = numRequestedPermissions;
                userId3 = userId;
                allUserIds = iArr;
                i4 = i2;
                length = i3;
                numRequestedPermissions2 = 1;
                userId2 = 0;
            }
            i4++;
            numRequestedPermissions2 = 1;
            userId2 = 0;
        }
    }

    private void revokeSystemAlertWindowIfUpgradedPast23(AndroidPackage newPackage, AndroidPackage oldPackage) {
        Permission saw;
        int[] allUserIds;
        if (oldPackage.getTargetSdkVersion() >= 23 || newPackage.getTargetSdkVersion() < 23 || !newPackage.getRequestedPermissions().contains("android.permission.SYSTEM_ALERT_WINDOW")) {
            return;
        }
        synchronized (this.mLock) {
            saw = this.mRegistry.getPermission("android.permission.SYSTEM_ALERT_WINDOW");
        }
        PackageStateInternal ps = this.mPackageManagerInt.getPackageStateInternal(newPackage.getPackageName());
        if (shouldGrantPermissionByProtectionFlags(newPackage, ps, saw, new ArraySet<>()) || shouldGrantPermissionBySignature(newPackage, saw)) {
            return;
        }
        for (int userId : getAllUserIds()) {
            try {
                revokePermissionFromPackageForUser(newPackage.getPackageName(), "android.permission.SYSTEM_ALERT_WINDOW", false, userId, this.mDefaultPermissionCallback);
            } catch (IllegalStateException | SecurityException e) {
                Log.e(TAG, "unable to revoke SYSTEM_ALERT_WINDOW for " + newPackage.getPackageName() + " user " + userId, e);
            }
        }
    }

    private void revokeRuntimePermissionsIfGroupChangedInternal(final AndroidPackage newPackage, AndroidPackage oldPackage) {
        int numOldPackagePermissions;
        PermissionManagerServiceImpl permissionManagerServiceImpl = this;
        int numOldPackagePermissions2 = ArrayUtils.size(oldPackage.getPermissions());
        ArrayMap<String, String> oldPermissionNameToGroupName = new ArrayMap<>(numOldPackagePermissions2);
        for (int i = 0; i < numOldPackagePermissions2; i++) {
            ParsedPermission permission = oldPackage.getPermissions().get(i);
            if (permission.getParsedPermissionGroup() != null) {
                oldPermissionNameToGroupName.put(permission.getName(), permission.getParsedPermissionGroup().getName());
            }
        }
        final int callingUid = Binder.getCallingUid();
        int numNewPackagePermissions = ArrayUtils.size(newPackage.getPermissions());
        int newPermissionNum = 0;
        while (newPermissionNum < numNewPackagePermissions) {
            ParsedPermission newPermission = newPackage.getPermissions().get(newPermissionNum);
            int newProtection = ParsedPermissionUtils.getProtection(newPermission);
            if ((newProtection & 1) == 0) {
                numOldPackagePermissions = numOldPackagePermissions2;
            } else {
                final String permissionName = newPermission.getName();
                final String newPermissionGroupName = newPermission.getParsedPermissionGroup() == null ? null : newPermission.getParsedPermissionGroup().getName();
                final String oldPermissionGroupName = oldPermissionNameToGroupName.get(permissionName);
                if (newPermissionGroupName == null) {
                    numOldPackagePermissions = numOldPackagePermissions2;
                } else if (newPermissionGroupName.equals(oldPermissionGroupName)) {
                    numOldPackagePermissions = numOldPackagePermissions2;
                } else {
                    final int[] userIds = permissionManagerServiceImpl.mUserManagerInt.getUserIds();
                    numOldPackagePermissions = numOldPackagePermissions2;
                    permissionManagerServiceImpl.mPackageManagerInt.forEachPackage(new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda12
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            PermissionManagerServiceImpl.this.m5846xca1224b0(userIds, permissionName, newPackage, oldPermissionGroupName, newPermissionGroupName, callingUid, (AndroidPackage) obj);
                        }
                    });
                }
            }
            newPermissionNum++;
            permissionManagerServiceImpl = this;
            numOldPackagePermissions2 = numOldPackagePermissions;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$revokeRuntimePermissionsIfGroupChangedInternal$5$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5846xca1224b0(int[] userIds, String permissionName, AndroidPackage newPackage, String oldPermissionGroupName, String newPermissionGroupName, int callingUid, AndroidPackage pkg) {
        String packageName = pkg.getPackageName();
        for (int userId : userIds) {
            int permissionState = checkPermission(packageName, permissionName, userId);
            if (permissionState == 0) {
                EventLog.writeEvent(1397638484, "72710897", Integer.valueOf(newPackage.getUid()), "Revoking permission " + permissionName + " from package " + packageName + " as the group changed from " + oldPermissionGroupName + " to " + newPermissionGroupName);
                try {
                    try {
                        revokeRuntimePermissionInternal(packageName, permissionName, false, callingUid, userId, null, this.mDefaultPermissionCallback);
                    } catch (IllegalArgumentException e) {
                        e = e;
                        Slog.e(TAG, "Could not revoke " + permissionName + " from " + packageName, e);
                    }
                } catch (IllegalArgumentException e2) {
                    e = e2;
                }
            }
        }
    }

    private void revokeRuntimePermissionsIfPermissionDefinitionChangedInternal(List<String> permissionsToRevoke) {
        final int[] userIds = this.mUserManagerInt.getUserIds();
        int numPermissions = permissionsToRevoke.size();
        final int callingUid = Binder.getCallingUid();
        for (int permNum = 0; permNum < numPermissions; permNum++) {
            final String permName = permissionsToRevoke.get(permNum);
            synchronized (this.mLock) {
                Permission bp = this.mRegistry.getPermission(permName);
                if (bp != null && (bp.isInternal() || bp.isRuntime())) {
                    final boolean isInternalPermission = bp.isInternal();
                    this.mPackageManagerInt.forEachPackage(new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            PermissionManagerServiceImpl.this.m5847xc4e115f6(userIds, permName, isInternalPermission, callingUid, (AndroidPackage) obj);
                        }
                    });
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$revokeRuntimePermissionsIfPermissionDefinitionChangedInternal$6$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5847xc4e115f6(int[] userIds, String permName, boolean isInternalPermission, int callingUid, AndroidPackage pkg) {
        int i;
        String str;
        PermissionCallback permissionCallback;
        String packageName = pkg.getPackageName();
        int appId = pkg.getUid();
        if (appId < 10000) {
            return;
        }
        int length = userIds.length;
        char c = 0;
        int i2 = 0;
        while (i2 < length) {
            int userId = userIds[i2];
            int permissionState = checkPermission(packageName, permName, userId);
            int flags = getPermissionFlags(packageName, permName, userId);
            if (permissionState == 0 && (flags & 32820) == 0) {
                int uid = UserHandle.getUid(userId, appId);
                if (isInternalPermission) {
                    Object[] objArr = new Object[3];
                    objArr[c] = "195338390";
                    objArr[1] = Integer.valueOf(uid);
                    objArr[2] = "Revoking permission " + permName + " from package " + packageName + " due to definition change";
                    EventLog.writeEvent(1397638484, objArr);
                } else {
                    Object[] objArr2 = new Object[3];
                    objArr2[c] = "154505240";
                    objArr2[1] = Integer.valueOf(uid);
                    objArr2[2] = "Revoking permission " + permName + " from package " + packageName + " due to definition change";
                    EventLog.writeEvent(1397638484, objArr2);
                    Object[] objArr3 = new Object[3];
                    objArr3[c] = "168319670";
                    objArr3[1] = Integer.valueOf(uid);
                    objArr3[2] = "Revoking permission " + permName + " from package " + packageName + " due to definition change";
                    EventLog.writeEvent(1397638484, objArr3);
                }
                Slog.e(TAG, "Revoking permission " + permName + " from package " + packageName + " due to definition change");
                try {
                    permissionCallback = this.mDefaultPermissionCallback;
                    str = TAG;
                    i = i2;
                } catch (Exception e) {
                    e = e;
                    str = TAG;
                    i = i2;
                }
                try {
                    revokeRuntimePermissionInternal(packageName, permName, false, callingUid, userId, null, permissionCallback);
                } catch (Exception e2) {
                    e = e2;
                    Slog.e(str, "Could not revoke " + permName + " from " + packageName, e);
                    i2 = i + 1;
                    c = 0;
                }
            } else {
                i = i2;
            }
            i2 = i + 1;
            c = 0;
        }
    }

    private List<String> addAllPermissionsInternal(AndroidPackage pkg) {
        PermissionInfo permissionInfo;
        Permission oldPermission;
        int N = ArrayUtils.size(pkg.getPermissions());
        ArrayList<String> definitionChangedPermissions = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            ParsedPermission p = pkg.getPermissions().get(i);
            ComponentMutateUtils.setExactFlags(p, p.getFlags() & (-1073741825));
            synchronized (this.mLock) {
                if (pkg.getTargetSdkVersion() > 22) {
                    ComponentMutateUtils.setParsedPermissionGroup(p, this.mRegistry.getPermissionGroup(p.getGroup()));
                    if (PackageManagerService.DEBUG_PERMISSIONS && p.getGroup() != null && p.getParsedPermissionGroup() == null) {
                        Slog.i(TAG, "Permission " + p.getName() + " from package " + p.getPackageName() + " in an unknown group " + p.getGroup());
                    }
                }
                permissionInfo = PackageInfoUtils.generatePermissionInfo(p, 128L);
                oldPermission = p.isTree() ? this.mRegistry.getPermissionTree(p.getName()) : this.mRegistry.getPermission(p.getName());
            }
            boolean isOverridingSystemPermission = Permission.isOverridingSystemPermission(oldPermission, permissionInfo, this.mPackageManagerInt);
            synchronized (this.mLock) {
                Permission permission = Permission.createOrUpdate(oldPermission, permissionInfo, pkg, this.mRegistry.getPermissionTrees(), isOverridingSystemPermission);
                if (p.isTree()) {
                    this.mRegistry.addPermissionTree(permission);
                } else {
                    this.mRegistry.addPermission(permission);
                }
                if (permission.isInstalled()) {
                    ComponentMutateUtils.setExactFlags(p, p.getFlags() | 1073741824);
                }
                if (permission.isDefinitionChanged()) {
                    definitionChangedPermissions.add(p.getName());
                    permission.setDefinitionChanged(false);
                }
            }
        }
        return definitionChangedPermissions;
    }

    private void addAllPermissionGroupsInternal(AndroidPackage pkg) {
        synchronized (this.mLock) {
            int N = ArrayUtils.size(pkg.getPermissionGroups());
            StringBuilder r = null;
            for (int i = 0; i < N; i++) {
                ParsedPermissionGroup pg = pkg.getPermissionGroups().get(i);
                ParsedPermissionGroup cur = this.mRegistry.getPermissionGroup(pg.getName());
                String curPackageName = cur == null ? null : cur.getPackageName();
                boolean isPackageUpdate = pg.getPackageName().equals(curPackageName);
                if (cur != null && !isPackageUpdate) {
                    Slog.w(TAG, "Permission group " + pg.getName() + " from package " + pg.getPackageName() + " ignored: original from " + cur.getPackageName());
                    if (PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                        if (r == null) {
                            r = new StringBuilder(256);
                        } else {
                            r.append(' ');
                        }
                        r.append("DUP:");
                        r.append(pg.getName());
                    }
                }
                this.mRegistry.addPermissionGroup(pg);
                if (PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                    if (r == null) {
                        r = new StringBuilder(256);
                    } else {
                        r.append(' ');
                    }
                    if (isPackageUpdate) {
                        r.append("UPD:");
                    }
                    r.append(pg.getName());
                }
            }
            if (r != null && PackageManagerService.DEBUG_PACKAGE_SCANNING) {
                Log.d(TAG, "  Permission Groups: " + ((Object) r));
            }
        }
    }

    private void removeAllPermissionsInternal(AndroidPackage pkg) {
        synchronized (this.mLock) {
            int n = ArrayUtils.size(pkg.getPermissions());
            StringBuilder r = null;
            for (int i = 0; i < n; i++) {
                ParsedPermission p = pkg.getPermissions().get(i);
                Permission bp = this.mRegistry.getPermission(p.getName());
                if (bp == null) {
                    bp = this.mRegistry.getPermissionTree(p.getName());
                }
                if (bp != null && bp.isPermission(p)) {
                    bp.setPermissionInfo(null);
                    if (PackageManagerService.DEBUG_REMOVE) {
                        if (r == null) {
                            r = new StringBuilder(256);
                        } else {
                            r.append(' ');
                        }
                        r.append(p.getName());
                    }
                }
                if (ParsedPermissionUtils.isAppOp(p)) {
                    this.mRegistry.removeAppOpPermissionPackage(p.getName(), pkg.getPackageName());
                }
            }
            if (r != null && PackageManagerService.DEBUG_REMOVE) {
                Log.d(TAG, "  Permissions: " + ((Object) r));
            }
            int n2 = pkg.getRequestedPermissions().size();
            for (int i2 = 0; i2 < n2; i2++) {
                String permissionName = pkg.getRequestedPermissions().get(i2);
                Permission permission = this.mRegistry.getPermission(permissionName);
                if (permission != null && permission.isAppOp()) {
                    this.mRegistry.removeAppOpPermissionPackage(permissionName, pkg.getPackageName());
                }
            }
            if (0 != 0 && PackageManagerService.DEBUG_REMOVE) {
                Log.d(TAG, "  Permissions: " + ((Object) null));
            }
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void onUserRemoved(int userId) {
        Preconditions.checkArgumentNonNegative(userId, "userId");
        synchronized (this.mLock) {
            this.mState.removeUserState(userId);
        }
    }

    private Set<String> getGrantedPermissionsInternal(String packageName, final int userId) {
        final PackageStateInternal ps = this.mPackageManagerInt.getPackageStateInternal(packageName);
        if (ps == null) {
            return Collections.emptySet();
        }
        synchronized (this.mLock) {
            UidPermissionState uidState = getUidStateLocked(ps, userId);
            if (uidState == null) {
                Slog.e(TAG, "Missing permissions state for " + packageName + " and user " + userId);
                return Collections.emptySet();
            } else if (!ps.getUserStateOrDefault(userId).isInstantApp()) {
                return uidState.getGrantedPermissions();
            } else {
                Set<String> instantPermissions = new ArraySet<>(uidState.getGrantedPermissions());
                instantPermissions.removeIf(new Predicate() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda16
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return PermissionManagerServiceImpl.this.m5840x6e5b013(userId, ps, (String) obj);
                    }
                });
                return instantPermissions;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getGrantedPermissionsInternal$7$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ boolean m5840x6e5b013(int userId, PackageStateInternal ps, String permissionName) {
        Permission permission = this.mRegistry.getPermission(permissionName);
        if (permission == null) {
            return true;
        }
        if (permission.isInstant()) {
            return false;
        }
        EventLog.writeEvent(1397638484, "140256621", Integer.valueOf(UserHandle.getUid(userId, ps.getAppId())), permissionName);
        return true;
    }

    private int[] getPermissionGidsInternal(String permissionName, int userId) {
        synchronized (this.mLock) {
            Permission permission = this.mRegistry.getPermission(permissionName);
            if (permission == null) {
                return EmptyArray.INT;
            }
            return permission.computeGids(userId);
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:473:0x0ac2
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3076=22, 2743=4] */
    private void restorePermissionState(com.android.server.pm.parsing.pkg.AndroidPackage r52, boolean r53, java.lang.String r54, com.android.server.pm.permission.PermissionManagerServiceImpl.PermissionCallback r55, int r56) {
        /*
            r51 = this;
            r8 = r51
            r9 = r52
            r10 = r53
            r11 = r54
            r12 = r55
            r13 = r56
            android.content.pm.PackageManagerInternal r0 = r8.mPackageManagerInt
            java.lang.String r1 = r52.getPackageName()
            com.android.server.pm.pkg.PackageStateInternal r14 = r0.getPackageStateInternal(r1)
            if (r14 != 0) goto L19
            return
        L19:
            android.content.pm.PackageManagerInternal r0 = r8.mPackageManagerInt
            int r1 = r14.getSharedUserAppId()
            com.android.server.pm.pkg.SharedUserApi r0 = r0.getSharedUserApi(r1)
            boolean r15 = r8.isPackageNeedsReview(r9, r0)
            r0 = -1
            r7 = 0
            r6 = 1
            if (r13 != r0) goto L31
            int[] r0 = r51.getAllUserIds()
            goto L35
        L31:
            int[] r0 = new int[r6]
            r0[r7] = r13
        L35:
            r5 = r0
            r1 = 0
            int[] r2 = com.android.server.pm.permission.PermissionManagerServiceImpl.EMPTY_INT_ARRAY
            r0 = 0
            r3 = 0
            r4 = 0
            android.util.ArraySet r16 = new android.util.ArraySet
            r16.<init>()
            r17 = r16
            java.util.List r7 = r52.getRequestedPermissions()
            int r6 = r7.size()
            r19 = 0
            r13 = r4
            r4 = r0
            r50 = r19
            r19 = r1
            r1 = r50
        L55:
            if (r1 >= r6) goto Ldc
            java.util.List r0 = r52.getRequestedPermissions()
            java.lang.Object r0 = r0.get(r1)
            r20 = r2
            r2 = r0
            java.lang.String r2 = (java.lang.String) r2
            java.lang.Object r12 = r8.mLock
            monitor-enter(r12)
            com.android.server.pm.permission.PermissionRegistry r0 = r8.mRegistry     // Catch: java.lang.Throwable -> Ld1
            com.android.server.pm.permission.Permission r0 = r0.getPermission(r2)     // Catch: java.lang.Throwable -> Ld1
            monitor-exit(r12)     // Catch: java.lang.Throwable -> Ld1
            if (r0 != 0) goto L73
            r12 = r17
            goto Lc8
        L73:
            boolean r12 = r0.isPrivileged()
            if (r12 == 0) goto L8a
            boolean r12 = r8.checkPrivilegedPermissionAllowlist(r9, r14, r0)
            if (r12 == 0) goto L8a
            if (r4 != 0) goto L87
            android.util.ArraySet r12 = new android.util.ArraySet
            r12.<init>()
            r4 = r12
        L87:
            r4.add(r2)
        L8a:
            boolean r12 = r0.isSignature()
            if (r12 == 0) goto Lae
            boolean r12 = r8.shouldGrantPermissionBySignature(r9, r0)
            if (r12 != 0) goto L9f
            r12 = r17
            boolean r17 = r8.shouldGrantPermissionByProtectionFlags(r9, r14, r0, r12)
            if (r17 == 0) goto Lb0
            goto La1
        L9f:
            r12 = r17
        La1:
            if (r3 != 0) goto Laa
            android.util.ArraySet r17 = new android.util.ArraySet
            r17.<init>()
            r3 = r17
        Laa:
            r3.add(r2)
            goto Lb0
        Lae:
            r12 = r17
        Lb0:
            boolean r17 = r0.isInternal()
            if (r17 == 0) goto Lc8
            boolean r17 = r8.shouldGrantPermissionByProtectionFlags(r9, r14, r0, r12)
            if (r17 == 0) goto Lc8
            if (r13 != 0) goto Lc5
            android.util.ArraySet r17 = new android.util.ArraySet
            r17.<init>()
            r13 = r17
        Lc5:
            r13.add(r2)
        Lc8:
            int r1 = r1 + 1
            r17 = r12
            r2 = r20
            r12 = r55
            goto L55
        Ld1:
            r0 = move-exception
            r50 = r17
            r17 = r2
            r2 = r50
        Ld8:
            monitor-exit(r12)     // Catch: java.lang.Throwable -> Lda
            throw r0
        Lda:
            r0 = move-exception
            goto Ld8
        Ldc:
            r20 = r2
            r2 = r17
            android.util.SparseBooleanArray r0 = new android.util.SparseBooleanArray
            r0.<init>()
            r12 = r0
            com.android.server.policy.PermissionPolicyInternal r0 = r8.mPermissionPolicyInternal
            if (r0 == 0) goto L10a
            int r0 = r5.length
            r1 = 0
        Lec:
            if (r1 >= r0) goto L107
            r17 = r0
            r0 = r5[r1]
            r21 = r13
            com.android.server.policy.PermissionPolicyInternal r13 = r8.mPermissionPolicyInternal
            boolean r13 = r13.isInitialized(r0)
            if (r13 == 0) goto L100
            r13 = 1
            r12.put(r0, r13)
        L100:
            int r1 = r1 + 1
            r0 = r17
            r13 = r21
            goto Lec
        L107:
            r21 = r13
            goto L10c
        L10a:
            r21 = r13
        L10c:
            boolean r0 = r14.hasSharedUser()
            if (r0 != 0) goto L129
            java.util.List r0 = r52.getRequestedPermissions()
            java.util.List r1 = r52.getImplicitPermissions()
            int r13 = r52.getTargetSdkVersion()
            r23 = r1
            r22 = r2
            r24 = r3
            r17 = r4
            r4 = r13
            r13 = r0
            goto L189
        L129:
            android.util.ArraySet r0 = new android.util.ArraySet
            r0.<init>()
            android.util.ArraySet r1 = new android.util.ArraySet
            r1.<init>()
            r13 = 10000(0x2710, float:1.4013E-41)
            r17 = r13
            android.content.pm.PackageManagerInternal r13 = r8.mPackageManagerInt
            r22 = r2
            int r2 = r14.getSharedUserAppId()
            android.util.ArraySet r2 = r13.getSharedUserPackages(r2)
            int r13 = r2.size()
            r23 = 0
            r24 = r3
            r3 = r17
            r17 = r4
            r4 = r23
        L151:
            if (r4 >= r13) goto L183
        L154:
            java.lang.Object r23 = r2.valueAt(r4)
            com.android.server.pm.pkg.PackageStateInternal r23 = (com.android.server.pm.pkg.PackageStateInternal) r23
            com.android.server.pm.pkg.AndroidPackageApi r23 = r23.getAndroidPackage()
            if (r23 != 0) goto L163
            r25 = r2
            goto L17e
        L163:
            r25 = r2
            java.util.List r2 = r23.getRequestedPermissions()
            r0.addAll(r2)
            java.util.List r2 = r23.getImplicitPermissions()
            r1.addAll(r2)
            int r2 = r23.getTargetSdkVersion()
            int r3 = java.lang.Math.min(r3, r2)
        L17e:
            int r4 = r4 + 1
            r2 = r25
            goto L151
        L183:
            r25 = r2
            r13 = r0
            r23 = r1
            r4 = r3
        L189:
            java.lang.Object r3 = r8.mLock
            monitor-enter(r3)
            int r0 = r5.length     // Catch: java.lang.Throwable -> Le56
            r25 = r3
            r1 = r19
            r2 = r20
            r3 = 0
        L194:
            if (r3 >= r0) goto Le11
            r19 = r5[r3]     // Catch: java.lang.Throwable -> Ldf2
            r20 = r19
            r26 = r0
            com.android.server.pm.permission.DevicePermissionState r0 = r8.mState     // Catch: java.lang.Throwable -> Ldf2
            r27 = r5
            r5 = r20
            com.android.server.pm.permission.UserPermissionState r0 = r0.getOrCreateUserState(r5)     // Catch: java.lang.Throwable -> Ldd2
            r19 = r3
            int r3 = r14.getAppId()     // Catch: java.lang.Throwable -> Ldd2
            com.android.server.pm.permission.UidPermissionState r3 = r0.getOrCreateUidState(r3)     // Catch: java.lang.Throwable -> Ldd2
            boolean r20 = r3.isMissing()     // Catch: java.lang.Throwable -> Ldd2
            r28 = r1
            if (r20 == 0) goto L262
            java.util.Iterator r20 = r13.iterator()     // Catch: java.lang.Throwable -> L244
        L1bc:
            boolean r29 = r20.hasNext()     // Catch: java.lang.Throwable -> L244
            if (r29 == 0) goto L236
            java.lang.Object r29 = r20.next()     // Catch: java.lang.Throwable -> L244
            java.lang.String r29 = (java.lang.String) r29     // Catch: java.lang.Throwable -> L244
            r30 = r29
            com.android.server.pm.permission.PermissionRegistry r1 = r8.mRegistry     // Catch: java.lang.Throwable -> L244
            r31 = r15
            r15 = r30
            com.android.server.pm.permission.Permission r1 = r1.getPermission(r15)     // Catch: java.lang.Throwable -> L217
            if (r1 != 0) goto L1d9
            r15 = r31
            goto L1bc
        L1d9:
            r30 = r15
            java.lang.String r15 = r1.getPackageName()     // Catch: java.lang.Throwable -> L217
            r32 = r12
            java.lang.String r12 = "android"
            boolean r12 = java.util.Objects.equals(r15, r12)     // Catch: java.lang.Throwable -> L295
            if (r12 == 0) goto L212
            boolean r12 = r1.isRuntime()     // Catch: java.lang.Throwable -> L295
            if (r12 == 0) goto L212
            boolean r12 = r1.isRemoved()     // Catch: java.lang.Throwable -> L295
            if (r12 != 0) goto L212
            boolean r12 = r1.isHardOrSoftRestricted()     // Catch: java.lang.Throwable -> L295
            if (r12 != 0) goto L201
            boolean r12 = r1.isImmutablyRestricted()     // Catch: java.lang.Throwable -> L295
            if (r12 == 0) goto L206
        L201:
            r12 = 8192(0x2000, float:1.14794E-41)
            r3.updatePermissionFlags(r1, r12, r12)     // Catch: java.lang.Throwable -> L295
        L206:
            r12 = 23
            if (r4 >= r12) goto L212
            r12 = 72
            r3.updatePermissionFlags(r1, r12, r12)     // Catch: java.lang.Throwable -> L295
            r3.grantPermission(r1)     // Catch: java.lang.Throwable -> L295
        L212:
            r15 = r31
            r12 = r32
            goto L1bc
        L217:
            r0 = move-exception
            r1 = r55
            r29 = r4
            r32 = r6
            r34 = r7
            r33 = r13
            r18 = r21
            r13 = r25
            r11 = r27
            r25 = r17
            r17 = r12
            r12 = r31
            r31 = r14
            r14 = r22
            r22 = r24
            goto Le74
        L236:
            r32 = r12
            r31 = r15
            r1 = 0
            r3.setMissing(r1)     // Catch: java.lang.Throwable -> L295
            int[] r1 = com.android.internal.util.ArrayUtils.appendInt(r2, r5)     // Catch: java.lang.Throwable -> L295
            r2 = r1
            goto L266
        L244:
            r0 = move-exception
            r1 = r55
            r29 = r4
            r32 = r6
            r34 = r7
            r33 = r13
            r31 = r14
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r25 = r17
            r17 = r12
            r12 = r15
            goto Le74
        L262:
            r32 = r12
            r31 = r15
        L266:
            r1 = r3
            r12 = 0
            if (r10 == 0) goto L2b4
            java.lang.String r15 = r14.getPackageName()     // Catch: java.lang.Throwable -> L295
            r20 = r12
            r12 = 0
            r0.setInstallPermissionsFixed(r15, r12)     // Catch: java.lang.Throwable -> L295
            boolean r15 = r14.hasSharedUser()     // Catch: java.lang.Throwable -> L295
            if (r15 != 0) goto L285
            com.android.server.pm.permission.UidPermissionState r15 = new com.android.server.pm.permission.UidPermissionState     // Catch: java.lang.Throwable -> L295
            r15.<init>(r3)     // Catch: java.lang.Throwable -> L295
            r1 = r15
            r3.reset()     // Catch: java.lang.Throwable -> L295
            r15 = r1
            goto L2b8
        L285:
            boolean r15 = r8.revokeUnusedSharedUserPermissionsLocked(r13, r3)     // Catch: java.lang.Throwable -> L295
            if (r15 == 0) goto L2b7
            int[] r15 = com.android.internal.util.ArrayUtils.appendInt(r2, r5)     // Catch: java.lang.Throwable -> L295
            r2 = r15
            r15 = 1
            r28 = r15
            r15 = r1
            goto L2b8
        L295:
            r0 = move-exception
            r1 = r55
            r29 = r4
            r34 = r7
            r33 = r13
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r31 = r14
            r25 = r17
            r14 = r22
            r22 = r24
            r17 = r32
            r32 = r6
            goto Le74
        L2b4:
            r20 = r12
            r12 = 0
        L2b7:
            r15 = r1
        L2b8:
            android.util.ArraySet r1 = new android.util.ArraySet     // Catch: java.lang.Throwable -> Ldb1
            r1.<init>()     // Catch: java.lang.Throwable -> Ldb1
            java.lang.StringBuilder r12 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Ldb1
            r12.<init>()     // Catch: java.lang.Throwable -> Ldb1
            r30 = r2
            java.lang.String r2 = r52.getPackageName()     // Catch: java.lang.Throwable -> Ld90
            java.lang.StringBuilder r2 = r12.append(r2)     // Catch: java.lang.Throwable -> Ld90
            java.lang.String r12 = "("
            java.lang.StringBuilder r2 = r2.append(r12)     // Catch: java.lang.Throwable -> Ld90
            int r12 = r52.getUid()     // Catch: java.lang.Throwable -> Ld90
            java.lang.StringBuilder r2 = r2.append(r12)     // Catch: java.lang.Throwable -> Ld90
            java.lang.String r12 = ")"
            java.lang.StringBuilder r2 = r2.append(r12)     // Catch: java.lang.Throwable -> Ld90
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> Ld90
            r12 = r2
            r2 = 0
            r33 = r13
            r13 = r30
        L2ea:
            if (r2 >= r6) goto Lc9d
            java.lang.Object r30 = r7.get(r2)     // Catch: java.lang.Throwable -> Lc7b
            java.lang.String r30 = (java.lang.String) r30     // Catch: java.lang.Throwable -> Lc7b
            r34 = r30
            r35 = r4
            com.android.server.pm.permission.PermissionRegistry r4 = r8.mRegistry     // Catch: java.lang.Throwable -> Lc59
            r36 = r6
            r6 = r34
            com.android.server.pm.permission.Permission r4 = r4.getPermission(r6)     // Catch: java.lang.Throwable -> Lc39
            r34 = r7
            int r7 = r52.getTargetSdkVersion()     // Catch: java.lang.Throwable -> Lc1b
            r30 = r2
            r2 = 23
            if (r7 < r2) goto L30f
            r2 = 1
            goto L310
        L30f:
            r2 = 0
        L310:
            boolean r7 = com.android.server.pm.PackageManagerService.DEBUG_INSTALL     // Catch: java.lang.Throwable -> Lc1b
            if (r7 == 0) goto L366
            if (r4 == 0) goto L366
            java.lang.String r7 = "PackageManager"
            r37 = r13
            java.lang.StringBuilder r13 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L3dc
            r13.<init>()     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r10 = "Package "
            java.lang.StringBuilder r10 = r13.append(r10)     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r13 = " checking "
            java.lang.StringBuilder r10 = r10.append(r13)     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r10 = r10.append(r6)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r13 = ": "
            java.lang.StringBuilder r10 = r10.append(r13)     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r10 = r10.append(r4)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r10 = r10.toString()     // Catch: java.lang.Throwable -> L3dc
            android.util.Log.i(r7, r10)     // Catch: java.lang.Throwable -> L3dc
            goto L368
        L345:
            r0 = move-exception
            r37 = r13
            r10 = r53
            r1 = r55
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r2 = r37
            r31 = r14
            r25 = r17
            r14 = r22
            r22 = r24
            r17 = r32
            r32 = r36
            goto Le74
        L366:
            r37 = r13
        L368:
            if (r4 != 0) goto L3fb
            if (r11 == 0) goto L38c
            java.lang.String r7 = r52.getPackageName()     // Catch: java.lang.Throwable -> L3dc
            boolean r7 = r11.equals(r7)     // Catch: java.lang.Throwable -> L3dc
            if (r7 == 0) goto L377
            goto L38c
        L377:
            r10 = r53
            r39 = r0
            r38 = r1
            r45 = r12
            r44 = r14
            r46 = r15
            r43 = r32
            r15 = r37
            r12 = r5
            r5 = r31
            goto L8b3
        L38c:
            boolean r7 = com.android.server.pm.PackageManagerService.DEBUG_PERMISSIONS     // Catch: java.lang.Throwable -> L3dc
            if (r7 == 0) goto L3c7
            java.lang.String r7 = "PackageManager"
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L3dc
            r10.<init>()     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r13 = "Unknown permission "
            java.lang.StringBuilder r10 = r10.append(r13)     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r10 = r10.append(r6)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r13 = " in package "
            java.lang.StringBuilder r10 = r10.append(r13)     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r10 = r10.toString()     // Catch: java.lang.Throwable -> L3dc
            android.util.Slog.i(r7, r10)     // Catch: java.lang.Throwable -> L3dc
            r10 = r53
            r39 = r0
            r38 = r1
            r45 = r12
            r44 = r14
            r46 = r15
            r43 = r32
            r15 = r37
            r12 = r5
            r5 = r31
            goto L8b3
        L3c7:
            r10 = r53
            r39 = r0
            r38 = r1
            r45 = r12
            r44 = r14
            r46 = r15
            r43 = r32
            r15 = r37
            r12 = r5
            r5 = r31
            goto L8b3
        L3dc:
            r0 = move-exception
            r10 = r53
            r1 = r55
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r2 = r37
            r31 = r14
            r25 = r17
            r14 = r22
            r22 = r24
            r17 = r32
            r32 = r36
            goto Le74
        L3fb:
            boolean r7 = r15.hasPermissionState(r6)     // Catch: java.lang.Throwable -> Lbfa
            if (r7 != 0) goto L42e
            java.util.List r7 = r52.getImplicitPermissions()     // Catch: java.lang.Throwable -> L3dc
            boolean r7 = r7.contains(r6)     // Catch: java.lang.Throwable -> L3dc
            if (r7 == 0) goto L42e
            r1.add(r6)     // Catch: java.lang.Throwable -> L3dc
            boolean r7 = com.android.server.pm.PackageManagerService.DEBUG_PERMISSIONS     // Catch: java.lang.Throwable -> L3dc
            if (r7 == 0) goto L42e
            java.lang.String r7 = "PackageManager"
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L3dc
            r10.<init>()     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r10 = r10.append(r6)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r13 = " is newly added for "
            java.lang.StringBuilder r10 = r10.append(r13)     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r10 = r10.toString()     // Catch: java.lang.Throwable -> L3dc
            android.util.Slog.i(r7, r10)     // Catch: java.lang.Throwable -> L3dc
        L42e:
            boolean r7 = r4.isRuntimeOnly()     // Catch: java.lang.Throwable -> Lbfa
            if (r7 == 0) goto L48a
            if (r2 != 0) goto L48a
            boolean r7 = com.android.server.pm.PackageManagerService.DEBUG_PERMISSIONS     // Catch: java.lang.Throwable -> L3dc
            if (r7 == 0) goto L475
            java.lang.String r7 = "PackageManager"
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L3dc
            r10.<init>()     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r13 = "Denying runtime-only permission "
            java.lang.StringBuilder r10 = r10.append(r13)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r13 = r4.getName()     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r10 = r10.append(r13)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r13 = " for package "
            java.lang.StringBuilder r10 = r10.append(r13)     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r10 = r10.append(r12)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r10 = r10.toString()     // Catch: java.lang.Throwable -> L3dc
            android.util.Log.i(r7, r10)     // Catch: java.lang.Throwable -> L3dc
            r10 = r53
            r39 = r0
            r38 = r1
            r45 = r12
            r44 = r14
            r46 = r15
            r43 = r32
            r15 = r37
            r12 = r5
            r5 = r31
            goto L8b3
        L475:
            r10 = r53
            r39 = r0
            r38 = r1
            r45 = r12
            r44 = r14
            r46 = r15
            r43 = r32
            r15 = r37
            r12 = r5
            r5 = r31
            goto L8b3
        L48a:
            java.lang.String r7 = r4.getName()     // Catch: java.lang.Throwable -> Lbfa
            boolean r10 = r4.isAppOp()     // Catch: java.lang.Throwable -> Lbfa
            if (r10 == 0) goto L49d
            com.android.server.pm.permission.PermissionRegistry r10 = r8.mRegistry     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r13 = r52.getPackageName()     // Catch: java.lang.Throwable -> L3dc
            r10.addAppOpPermissionPackage(r7, r13)     // Catch: java.lang.Throwable -> L3dc
        L49d:
            r10 = 1
            boolean r13 = r4.isNormal()     // Catch: java.lang.Throwable -> Lbfa
            if (r13 == 0) goto L4c1
            boolean r13 = r15.isPermissionGranted(r7)     // Catch: java.lang.Throwable -> L3dc
            if (r13 != 0) goto L4c1
            boolean r13 = r14.isSystem()     // Catch: java.lang.Throwable -> L3dc
            if (r13 != 0) goto L4c1
            java.lang.String r13 = r14.getPackageName()     // Catch: java.lang.Throwable -> L3dc
            boolean r13 = r0.areInstallPermissionsFixed(r13)     // Catch: java.lang.Throwable -> L3dc
            if (r13 == 0) goto L4c1
            boolean r13 = isCompatPlatformPermissionForPackage(r7, r9)     // Catch: java.lang.Throwable -> L3dc
            if (r13 != 0) goto L4c1
            r10 = 0
        L4c1:
            boolean r13 = com.android.server.pm.PackageManagerService.DEBUG_PERMISSIONS     // Catch: java.lang.Throwable -> Lbfa
            if (r13 == 0) goto L4f0
            java.lang.String r13 = "PackageManager"
            r38 = r1
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L3dc
            r1.<init>()     // Catch: java.lang.Throwable -> L3dc
            r39 = r0
            java.lang.String r0 = "Considering granting permission "
            java.lang.StringBuilder r0 = r1.append(r0)     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r0 = r0.append(r7)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r1 = " to package "
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r1 = r52.getPackageName()     // Catch: java.lang.Throwable -> L3dc
            java.lang.StringBuilder r0 = r0.append(r1)     // Catch: java.lang.Throwable -> L3dc
            java.lang.String r0 = r0.toString()     // Catch: java.lang.Throwable -> L3dc
            android.util.Slog.i(r13, r0)     // Catch: java.lang.Throwable -> L3dc
            goto L4f4
        L4f0:
            r39 = r0
            r38 = r1
        L4f4:
            boolean r0 = r4.isNormal()     // Catch: java.lang.Throwable -> Lbfa
            if (r0 != 0) goto L8fc
            boolean r0 = r4.isSignature()     // Catch: java.lang.Throwable -> L8dc
            if (r0 != 0) goto L8fc
            boolean r0 = r4.isInternal()     // Catch: java.lang.Throwable -> L8dc
            if (r0 == 0) goto L51b
            r47 = r7
            r48 = r10
            r45 = r12
            r44 = r14
            r46 = r15
            r43 = r32
            r15 = r37
            r10 = r53
            r12 = r5
            r5 = r31
            goto L90f
        L51b:
            boolean r0 = r4.isRuntime()     // Catch: java.lang.Throwable -> L8dc
            if (r0 == 0) goto L876
            boolean r0 = r4.isHardRestricted()     // Catch: java.lang.Throwable -> L8dc
            boolean r13 = r4.isSoftRestricted()     // Catch: java.lang.Throwable -> L8dc
            r1 = r32
            boolean r32 = r1.get(r5)     // Catch: java.lang.Throwable -> L856
            com.android.server.pm.permission.PermissionState r40 = r15.getPermissionState(r7)     // Catch: java.lang.Throwable -> L856
            if (r40 == 0) goto L55a
            int r41 = r40.getFlags()     // Catch: java.lang.Throwable -> L53b
            goto L55c
        L53b:
            r0 = move-exception
            r10 = r53
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r2 = r37
            r31 = r14
            r25 = r17
            r14 = r22
            r22 = r24
            r17 = r1
            r1 = r55
            goto Le74
        L55a:
            r41 = 0
        L55c:
            r42 = r41
            r41 = 0
            r43 = r1
            java.lang.String r1 = r4.getName()     // Catch: java.lang.Throwable -> L836
            int r1 = r15.getPermissionFlags(r1)     // Catch: java.lang.Throwable -> L836
            r1 = r1 & 14336(0x3800, float:2.0089E-41)
            if (r1 == 0) goto L571
            r1 = 1
            goto L572
        L571:
            r1 = 0
        L572:
            r44 = r14
            java.lang.String r14 = r4.getName()     // Catch: java.lang.Throwable -> L816
            int r14 = r15.getPermissionFlags(r14)     // Catch: java.lang.Throwable -> L816
            r14 = r14 & 16384(0x4000, float:2.2959E-41)
            if (r14 == 0) goto L583
            r14 = 1
            goto L584
        L583:
            r14 = 0
        L584:
            if (r2 == 0) goto L702
            if (r32 == 0) goto L5d3
            if (r0 == 0) goto L5d3
            if (r1 != 0) goto L5ce
            if (r40 == 0) goto L5bc
            boolean r45 = r40.isGranted()     // Catch: java.lang.Throwable -> L59d
            if (r45 == 0) goto L5bc
            boolean r45 = r3.revokePermission(r4)     // Catch: java.lang.Throwable -> L59d
            if (r45 == 0) goto L5bc
            r41 = 1
            goto L5bc
        L59d:
            r0 = move-exception
            r10 = r53
            r1 = r55
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r2 = r37
            r31 = r44
            r25 = r17
            r17 = r43
            goto Le74
        L5bc:
            if (r14 != 0) goto L5c9
            r45 = r12
            r12 = r42
            r12 = r12 | 16384(0x4000, float:2.2959E-41)
            r41 = 1
            r42 = r12
            goto L5e8
        L5c9:
            r45 = r12
            r12 = r42
            goto L5e8
        L5ce:
            r45 = r12
            r12 = r42
            goto L5e6
        L5d3:
            r45 = r12
            r12 = r42
            if (r32 == 0) goto L5e6
            if (r13 == 0) goto L5e6
            if (r1 != 0) goto L5e6
            if (r14 != 0) goto L5e6
            r12 = r12 | 16384(0x4000, float:2.2959E-41)
            r41 = 1
            r42 = r12
            goto L5e8
        L5e6:
            r42 = r12
        L5e8:
            java.util.List<java.lang.String> r12 = com.android.server.pm.permission.PermissionManagerServiceImpl.NOTIFICATION_PERMISSIONS     // Catch: java.lang.Throwable -> L6e3
            boolean r46 = r12.contains(r7)     // Catch: java.lang.Throwable -> L6e3
            if (r46 != 0) goto L624
            r46 = r42 & 64
            if (r46 == 0) goto L619
            com.mediatek.cta.CtaManagerFactory r46 = com.mediatek.cta.CtaManagerFactory.getInstance()     // Catch: java.lang.Throwable -> L6e3
            r47 = r7
            com.mediatek.cta.CtaManager r7 = r46.makeCtaManager()     // Catch: java.lang.Throwable -> L6e3
            r46 = r15
            java.lang.String r15 = r4.getPackageName()     // Catch: java.lang.Throwable -> L6e3
            r48 = r10
            java.lang.String r10 = r4.getName()     // Catch: java.lang.Throwable -> L6e3
            r49 = r5
            r5 = r31
            boolean r7 = r7.needClearReviewFlagAfterUpgrade(r5, r15, r10)     // Catch: java.lang.Throwable -> L641
            if (r7 == 0) goto L62e
            r42 = r42 & (-65)
            r41 = 1
            goto L62e
        L619:
            r49 = r5
            r47 = r7
            r48 = r10
            r46 = r15
            r5 = r31
            goto L62e
        L624:
            r49 = r5
            r47 = r7
            r48 = r10
            r46 = r15
            r5 = r31
        L62e:
            r7 = r42 & 8
            if (r7 == 0) goto L65f
            int r7 = r52.getTargetSdkVersion()     // Catch: java.lang.Throwable -> L641
            boolean r7 = r8.isPermissionSplitFromNonRuntime(r6, r7)     // Catch: java.lang.Throwable -> L641
            if (r7 != 0) goto L65f
            r42 = r42 & (-9)
            r41 = 1
            goto L675
        L641:
            r0 = move-exception
            r10 = r53
            r1 = r55
            r12 = r5
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r2 = r37
            r31 = r44
            r25 = r17
            r17 = r43
            goto Le74
        L65f:
            if (r32 == 0) goto L665
            if (r0 == 0) goto L665
            if (r1 == 0) goto L675
        L665:
            if (r40 == 0) goto L675
            boolean r7 = r40.isGranted()     // Catch: java.lang.Throwable -> L641
            if (r7 == 0) goto L675
            boolean r7 = r3.grantPermission(r4)     // Catch: java.lang.Throwable -> L641
            if (r7 != 0) goto L675
            r41 = 1
        L675:
            com.mediatek.cta.CtaManagerFactory r7 = com.mediatek.cta.CtaManagerFactory.getInstance()     // Catch: java.lang.Throwable -> L641
            com.mediatek.cta.CtaManager r7 = r7.makeCtaManager()     // Catch: java.lang.Throwable -> L641
            java.lang.String r10 = r4.getPackageName()     // Catch: java.lang.Throwable -> L641
            java.lang.String r15 = r4.getName()     // Catch: java.lang.Throwable -> L641
            boolean r7 = r7.isPlatformPermission(r10, r15)     // Catch: java.lang.Throwable -> L641
            if (r7 == 0) goto L6bc
            if (r5 == 0) goto L6bc
            r7 = r42 & 64
            if (r7 != 0) goto L6b9
            r7 = r42 & 16
            if (r7 != 0) goto L6b9
            boolean r7 = r4.isRemoved()     // Catch: java.lang.Throwable -> L641
            if (r7 != 0) goto L6b6
            if (r11 == 0) goto L6b3
            java.lang.String r7 = r52.getPackageName()     // Catch: java.lang.Throwable -> L641
            boolean r7 = r11.equals(r7)     // Catch: java.lang.Throwable -> L641
            if (r7 == 0) goto L6b0
            r10 = r53
            if (r10 != 0) goto L6be
            r42 = r42 | 64
            r41 = 1
            goto L6be
        L6b0:
            r10 = r53
            goto L6be
        L6b3:
            r10 = r53
            goto L6be
        L6b6:
            r10 = r53
            goto L6be
        L6b9:
            r10 = r53
            goto L6be
        L6bc:
            r10 = r53
        L6be:
            boolean r7 = r8.mIsLeanback     // Catch: java.lang.Throwable -> L72b
            if (r7 == 0) goto L6df
            boolean r7 = r12.contains(r6)     // Catch: java.lang.Throwable -> L72b
            if (r7 == 0) goto L6df
            r3.grantPermission(r4)     // Catch: java.lang.Throwable -> L72b
            if (r40 == 0) goto L6d3
            boolean r7 = r40.isGranted()     // Catch: java.lang.Throwable -> L72b
            if (r7 != 0) goto L6df
        L6d3:
            boolean r7 = r3.grantPermission(r4)     // Catch: java.lang.Throwable -> L72b
            if (r7 == 0) goto L6df
            r41 = 1
            r7 = r42
            goto L7a1
        L6df:
            r7 = r42
            goto L7a1
        L6e3:
            r0 = move-exception
            r10 = r53
            r1 = r55
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r2 = r37
            r31 = r44
            r25 = r17
            r17 = r43
            goto Le74
        L702:
            r49 = r5
            r47 = r7
            r48 = r10
            r45 = r12
            r46 = r15
            r5 = r31
            r12 = r42
            r10 = r53
            if (r40 != 0) goto L747
            java.lang.String r7 = "android"
            java.lang.String r15 = r4.getPackageName()     // Catch: java.lang.Throwable -> L72b
            boolean r7 = r7.equals(r15)     // Catch: java.lang.Throwable -> L72b
            if (r7 == 0) goto L747
            boolean r7 = r4.isRemoved()     // Catch: java.lang.Throwable -> L72b
            if (r7 != 0) goto L747
            r42 = r12 | 72
            r41 = 1
            goto L749
        L72b:
            r0 = move-exception
            r1 = r55
            r12 = r5
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r2 = r37
            r31 = r44
            r25 = r17
            r17 = r43
            goto Le74
        L747:
            r42 = r12
        L749:
            com.mediatek.cta.CtaManagerFactory r7 = com.mediatek.cta.CtaManagerFactory.getInstance()     // Catch: java.lang.Throwable -> L7f9
            com.mediatek.cta.CtaManager r7 = r7.makeCtaManager()     // Catch: java.lang.Throwable -> L7f9
            java.lang.String r12 = r4.getPackageName()     // Catch: java.lang.Throwable -> L7f9
            java.lang.String r15 = r4.getName()     // Catch: java.lang.Throwable -> L7f9
            boolean r7 = r7.isPlatformPermission(r12, r15)     // Catch: java.lang.Throwable -> L7f9
            if (r7 == 0) goto L77e
            if (r5 == 0) goto L77e
            r7 = r42 & 64
            if (r7 != 0) goto L77e
            boolean r7 = r4.isRemoved()     // Catch: java.lang.Throwable -> L72b
            if (r7 != 0) goto L77e
            if (r11 == 0) goto L77e
            java.lang.String r7 = r52.getPackageName()     // Catch: java.lang.Throwable -> L72b
            boolean r7 = r11.equals(r7)     // Catch: java.lang.Throwable -> L72b
            if (r7 == 0) goto L77e
            if (r10 != 0) goto L77e
            r7 = r42 | 64
            r41 = 1
            goto L780
        L77e:
            r7 = r42
        L780:
            java.lang.String r12 = r4.getName()     // Catch: java.lang.Throwable -> L7f9
            boolean r12 = r3.isPermissionGranted(r12)     // Catch: java.lang.Throwable -> L7f9
            if (r12 != 0) goto L793
            boolean r12 = r3.grantPermission(r4)     // Catch: java.lang.Throwable -> L72b
            if (r12 == 0) goto L793
            r12 = 1
            r41 = r12
        L793:
            if (r32 == 0) goto L7a1
            if (r0 != 0) goto L799
            if (r13 == 0) goto L7a1
        L799:
            if (r1 != 0) goto L7a1
            if (r14 != 0) goto L7a1
            r7 = r7 | 16384(0x4000, float:2.2959E-41)
            r41 = 1
        L7a1:
            if (r32 == 0) goto L7b3
            if (r0 != 0) goto L7a7
            if (r13 == 0) goto L7a9
        L7a7:
            if (r1 == 0) goto L7b3
        L7a9:
            if (r14 == 0) goto L7b3
            r7 = r7 & (-16385(0xffffffffffffbfff, float:NaN))
            if (r2 != 0) goto L7b1
            r7 = r7 | 64
        L7b1:
            r41 = 1
        L7b3:
            com.mediatek.cta.CtaManagerFactory r12 = com.mediatek.cta.CtaManagerFactory.getInstance()     // Catch: java.lang.Throwable -> L7f9
            com.mediatek.cta.CtaManager r12 = r12.makeCtaManager()     // Catch: java.lang.Throwable -> L7f9
            boolean r12 = r12.isCtaSupported()     // Catch: java.lang.Throwable -> L7f9
            if (r12 == 0) goto L7d0
            if (r9 == 0) goto L7d0
            int r12 = r52.getTargetSdkVersion()     // Catch: java.lang.Throwable -> L72b
            r15 = 23
            if (r12 >= r15) goto L7d2
            r7 = r7 | 64
            r41 = 1
            goto L7d2
        L7d0:
            r15 = 23
        L7d2:
            if (r41 == 0) goto L7df
            r15 = r37
            r12 = r49
            int[] r31 = com.android.internal.util.ArrayUtils.appendInt(r15, r12)     // Catch: java.lang.Throwable -> L8c1
            r15 = r31
            goto L7e3
        L7df:
            r15 = r37
            r12 = r49
        L7e3:
            r31 = r0
            r0 = 261119(0x3fbff, float:3.65906E-40)
            r3.updatePermissionFlags(r4, r0, r7)     // Catch: java.lang.Throwable -> L8c1
            r31 = r5
            r37 = r17
            r14 = r22
            r13 = r24
            r6 = r44
            r0 = r46
            goto Lb91
        L7f9:
            r0 = move-exception
            r15 = r37
            r1 = r55
            r12 = r5
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r31 = r44
            r25 = r17
            r17 = r43
            goto Le74
        L816:
            r0 = move-exception
            r10 = r53
            r15 = r37
            r1 = r55
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r31 = r44
            r25 = r17
            r17 = r43
            goto Le74
        L836:
            r0 = move-exception
            r10 = r53
            r15 = r37
            r1 = r55
            r2 = r15
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r31 = r14
            r25 = r17
            r14 = r22
            r22 = r24
            r17 = r43
            goto Le74
        L856:
            r0 = move-exception
            r10 = r53
            r15 = r37
            r2 = r15
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r31 = r14
            r25 = r17
            r14 = r22
            r22 = r24
            r17 = r1
            r1 = r55
            goto Le74
        L876:
            r47 = r7
            r48 = r10
            r45 = r12
            r44 = r14
            r46 = r15
            r43 = r32
            r15 = r37
            r10 = r53
            r12 = r5
            r5 = r31
            java.lang.String r0 = com.android.server.pm.permission.PermissionManagerServiceImpl.LOG_TAG     // Catch: java.lang.Throwable -> L8c1
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L8c1
            r1.<init>()     // Catch: java.lang.Throwable -> L8c1
            java.lang.String r7 = "Unknown permission protection "
            java.lang.StringBuilder r1 = r1.append(r7)     // Catch: java.lang.Throwable -> L8c1
            int r7 = r4.getProtection()     // Catch: java.lang.Throwable -> L8c1
            java.lang.StringBuilder r1 = r1.append(r7)     // Catch: java.lang.Throwable -> L8c1
            java.lang.String r7 = " for permission "
            java.lang.StringBuilder r1 = r1.append(r7)     // Catch: java.lang.Throwable -> L8c1
            java.lang.String r7 = r4.getName()     // Catch: java.lang.Throwable -> L8c1
            java.lang.StringBuilder r1 = r1.append(r7)     // Catch: java.lang.Throwable -> L8c1
            java.lang.String r1 = r1.toString()     // Catch: java.lang.Throwable -> L8c1
            android.util.Slog.wtf(r0, r1)     // Catch: java.lang.Throwable -> L8c1
        L8b3:
            r31 = r5
            r37 = r17
            r14 = r22
            r13 = r24
            r6 = r44
            r0 = r46
            goto Lb91
        L8c1:
            r0 = move-exception
            r1 = r55
            r12 = r5
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r31 = r44
            r25 = r17
            r17 = r43
            goto Le74
        L8dc:
            r0 = move-exception
            r10 = r53
            r15 = r37
            r1 = r55
            r2 = r15
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r31 = r14
            r25 = r17
            r14 = r22
            r22 = r24
            r17 = r32
            r32 = r36
            goto Le74
        L8fc:
            r47 = r7
            r48 = r10
            r45 = r12
            r44 = r14
            r46 = r15
            r43 = r32
            r15 = r37
            r10 = r53
            r12 = r5
            r5 = r31
        L90f:
            boolean r0 = r4.isNormal()     // Catch: java.lang.Throwable -> Lbdf
            if (r0 == 0) goto L924
            if (r48 != 0) goto L918
            goto L924
        L918:
            r7 = r17
            r1 = r21
            r14 = r22
            r13 = r24
            r0 = r46
            goto La31
        L924:
            boolean r0 = r4.isSignature()     // Catch: java.lang.Throwable -> Lbdf
            if (r0 == 0) goto L9d3
            boolean r0 = r4.isPrivileged()     // Catch: java.lang.Throwable -> L9b8
            if (r0 == 0) goto L95c
            r7 = r17
            boolean r0 = com.android.internal.util.CollectionUtils.contains(r7, r6)     // Catch: java.lang.Throwable -> L941
            if (r0 == 0) goto L939
            goto L95e
        L939:
            r14 = r22
            r13 = r24
            r0 = r46
            goto L9db
        L941:
            r0 = move-exception
            r1 = r55
            r12 = r5
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r17 = r43
            r31 = r44
            r25 = r7
            goto Le74
        L95c:
            r7 = r17
        L95e:
            r13 = r24
            boolean r0 = com.android.internal.util.CollectionUtils.contains(r13, r6)     // Catch: java.lang.Throwable -> L99d
            if (r0 != 0) goto L995
            boolean r0 = r4.isPrivileged()     // Catch: java.lang.Throwable -> L99d
            if (r0 == 0) goto L975
            r14 = r22
            boolean r0 = com.android.internal.util.CollectionUtils.contains(r14, r6)     // Catch: java.lang.Throwable -> L9f2
            if (r0 != 0) goto L988
            goto L977
        L975:
            r14 = r22
        L977:
            boolean r0 = r4.isDevelopment()     // Catch: java.lang.Throwable -> L9f2
            if (r0 != 0) goto L988
            boolean r0 = r4.isRole()     // Catch: java.lang.Throwable -> L9f2
            if (r0 == 0) goto L984
            goto L988
        L984:
            r0 = r46
            goto L9db
        L988:
            r0 = r46
            boolean r1 = r0.isPermissionGranted(r6)     // Catch: java.lang.Throwable -> L9f2
            if (r1 != 0) goto L991
            goto L9db
        L991:
            r1 = r21
            goto La31
        L995:
            r14 = r22
            r0 = r46
            r1 = r21
            goto La31
        L99d:
            r0 = move-exception
            r1 = r55
            r12 = r5
            r2 = r15
            r18 = r21
            r14 = r22
            r11 = r27
            r29 = r35
            r32 = r36
            r17 = r43
            r31 = r44
            r22 = r13
            r13 = r25
            r25 = r7
            goto Le74
        L9b8:
            r0 = move-exception
            r1 = r55
            r12 = r5
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r31 = r44
            r25 = r17
            r17 = r43
            goto Le74
        L9d3:
            r7 = r17
            r14 = r22
            r13 = r24
            r0 = r46
        L9db:
            boolean r1 = r4.isInternal()     // Catch: java.lang.Throwable -> Lbc6
            if (r1 == 0) goto La8b
            boolean r1 = r4.isPrivileged()     // Catch: java.lang.Throwable -> La72
            if (r1 == 0) goto La0b
            boolean r1 = com.android.internal.util.CollectionUtils.contains(r7, r6)     // Catch: java.lang.Throwable -> L9f2
            if (r1 == 0) goto L9ee
            goto La0b
        L9ee:
            r1 = r21
            goto La8d
        L9f2:
            r0 = move-exception
            r1 = r55
            r12 = r5
            r22 = r13
            r2 = r15
            r18 = r21
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r17 = r43
            r31 = r44
            r25 = r7
            goto Le74
        La0b:
            r1 = r21
            boolean r17 = com.android.internal.util.CollectionUtils.contains(r1, r6)     // Catch: java.lang.Throwable -> La59
            if (r17 != 0) goto La31
            boolean r17 = r4.isPrivileged()     // Catch: java.lang.Throwable -> La59
            if (r17 == 0) goto La1f
            boolean r17 = com.android.internal.util.CollectionUtils.contains(r14, r6)     // Catch: java.lang.Throwable -> La59
            if (r17 != 0) goto La2b
        La1f:
            boolean r17 = r4.isDevelopment()     // Catch: java.lang.Throwable -> La59
            if (r17 != 0) goto La2b
            boolean r17 = r4.isRole()     // Catch: java.lang.Throwable -> La59
            if (r17 == 0) goto La8d
        La2b:
            boolean r17 = r0.isPermissionGranted(r6)     // Catch: java.lang.Throwable -> La59
            if (r17 == 0) goto La8d
        La31:
            boolean r17 = r3.grantPermission(r4)     // Catch: java.lang.Throwable -> La59
            if (r17 == 0) goto La49
            r20 = 1
            r21 = r1
            r17 = r2
            r31 = r5
            r22 = r6
            r37 = r7
            r6 = r44
            r5 = r47
            goto Lb7e
        La49:
            r21 = r1
            r17 = r2
            r31 = r5
            r22 = r6
            r37 = r7
            r6 = r44
            r5 = r47
            goto Lb7e
        La59:
            r0 = move-exception
            r18 = r1
            r12 = r5
            r22 = r13
            r2 = r15
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r17 = r43
            r31 = r44
            r1 = r55
            r25 = r7
            goto Le74
        La72:
            r0 = move-exception
            r1 = r55
            r12 = r5
            r22 = r13
            r2 = r15
            r18 = r21
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r17 = r43
            r31 = r44
            r25 = r7
            goto Le74
        La8b:
            r1 = r21
        La8d:
            boolean r17 = com.android.server.pm.PackageManagerService.DEBUG_PERMISSIONS     // Catch: java.lang.Throwable -> Lbad
            if (r17 == 0) goto Lb68
            r21 = r1
            java.lang.String r1 = r4.getName()     // Catch: java.lang.Throwable -> Lbc6
            boolean r1 = r3.isPermissionGranted(r1)     // Catch: java.lang.Throwable -> Lbc6
            if (r1 != 0) goto Lab2
            boolean r17 = r4.isAppOp()     // Catch: java.lang.Throwable -> L9f2
            if (r17 == 0) goto Laa4
            goto Lab2
        Laa4:
            r17 = r2
            r31 = r5
            r22 = r6
            r37 = r7
            r6 = r44
            r5 = r47
            goto Lb76
        Lab2:
            r17 = r2
            java.lang.String r2 = "PackageManager"
            r31 = r5
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Lb4e
            r5.<init>()     // Catch: java.lang.Throwable -> Lb4e
            if (r1 == 0) goto Ladc
            java.lang.String r22 = "Un-granting"
            goto Lade
        Lac2:
            r0 = move-exception
            r1 = r55
            r22 = r13
            r2 = r15
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r17 = r43
            r31 = r44
            r25 = r7
            goto Le74
        Ladc:
            java.lang.String r22 = "Not granting"
        Lade:
            r24 = r1
            r1 = r22
            java.lang.StringBuilder r1 = r5.append(r1)     // Catch: java.lang.Throwable -> Lb4e
            java.lang.String r5 = " permission "
            java.lang.StringBuilder r1 = r1.append(r5)     // Catch: java.lang.Throwable -> Lb4e
            r5 = r47
            java.lang.StringBuilder r1 = r1.append(r5)     // Catch: java.lang.Throwable -> Lb4e
            r22 = r6
            java.lang.String r6 = " from package "
            java.lang.StringBuilder r1 = r1.append(r6)     // Catch: java.lang.Throwable -> Lb4e
            r6 = r45
            java.lang.StringBuilder r1 = r1.append(r6)     // Catch: java.lang.Throwable -> Lb4e
            r45 = r6
            java.lang.String r6 = " (protectionLevel="
            java.lang.StringBuilder r1 = r1.append(r6)     // Catch: java.lang.Throwable -> Lb4e
            int r6 = r4.getProtectionLevel()     // Catch: java.lang.Throwable -> Lb4e
            java.lang.StringBuilder r1 = r1.append(r6)     // Catch: java.lang.Throwable -> Lb4e
            java.lang.String r6 = " flags=0x"
            java.lang.StringBuilder r1 = r1.append(r6)     // Catch: java.lang.Throwable -> Lb4e
            r6 = r44
            int r32 = com.android.server.pm.parsing.PackageInfoUtils.appInfoFlags(r9, r6)     // Catch: java.lang.Throwable -> Lb34
            r37 = r7
            java.lang.String r7 = java.lang.Integer.toHexString(r32)     // Catch: java.lang.Throwable -> Lcd2
            java.lang.StringBuilder r1 = r1.append(r7)     // Catch: java.lang.Throwable -> Lcd2
            java.lang.String r7 = ")"
            java.lang.StringBuilder r1 = r1.append(r7)     // Catch: java.lang.Throwable -> Lcd2
            java.lang.String r1 = r1.toString()     // Catch: java.lang.Throwable -> Lcd2
            android.util.Slog.i(r2, r1)     // Catch: java.lang.Throwable -> Lcd2
            goto Lb76
        Lb34:
            r0 = move-exception
            r1 = r55
            r22 = r13
            r2 = r15
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r17 = r43
            r31 = r6
            r25 = r7
            goto Le74
        Lb4e:
            r0 = move-exception
            r1 = r55
            r22 = r13
            r2 = r15
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r17 = r43
            r31 = r44
            r25 = r7
            goto Le74
        Lb68:
            r21 = r1
            r17 = r2
            r31 = r5
            r22 = r6
            r37 = r7
            r6 = r44
            r5 = r47
        Lb76:
            boolean r1 = r3.revokePermission(r4)     // Catch: java.lang.Throwable -> Lcd2
            if (r1 == 0) goto Lb7e
            r20 = 1
        Lb7e:
            com.android.server.pm.permission.PermissionState r1 = r0.getPermissionState(r5)     // Catch: java.lang.Throwable -> Lcd2
            if (r1 == 0) goto Lb89
            int r2 = r1.getFlags()     // Catch: java.lang.Throwable -> Lcd2
            goto Lb8a
        Lb89:
            r2 = 0
        Lb8a:
            r7 = 261119(0x3fbff, float:3.65906E-40)
            r3.updatePermissionFlags(r4, r7, r2)     // Catch: java.lang.Throwable -> Lcd2
        Lb91:
            int r2 = r30 + 1
            r5 = r12
            r24 = r13
            r22 = r14
            r13 = r15
            r7 = r34
            r4 = r35
            r17 = r37
            r1 = r38
            r32 = r43
            r12 = r45
            r15 = r0
            r14 = r6
            r6 = r36
            r0 = r39
            goto L2ea
        Lbad:
            r0 = move-exception
            r18 = r1
            r12 = r5
            r22 = r13
            r2 = r15
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r17 = r43
            r31 = r44
            r1 = r55
            r25 = r7
            goto Le74
        Lbc6:
            r0 = move-exception
            r1 = r55
            r12 = r5
            r22 = r13
            r2 = r15
            r18 = r21
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r17 = r43
            r31 = r44
            r25 = r7
            goto Le74
        Lbdf:
            r0 = move-exception
            r1 = r55
            r12 = r5
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r29 = r35
            r32 = r36
            r31 = r44
            r25 = r17
            r17 = r43
            goto Le74
        Lbfa:
            r0 = move-exception
            r10 = r53
            r6 = r14
            r15 = r37
            r1 = r55
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r31 = r6
            r25 = r17
            r17 = r32
            r32 = r36
            goto Le74
        Lc1b:
            r0 = move-exception
            r15 = r13
            r6 = r14
            r1 = r55
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r31 = r6
            r25 = r17
            r17 = r32
            r32 = r36
            goto Le74
        Lc39:
            r0 = move-exception
            r15 = r13
            r6 = r14
            r1 = r55
            r34 = r7
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r31 = r6
            r25 = r17
            r17 = r32
            r32 = r36
            goto Le74
        Lc59:
            r0 = move-exception
            r36 = r6
            r15 = r13
            r6 = r14
            r1 = r55
            r34 = r7
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r31 = r6
            r25 = r17
            r17 = r32
            r32 = r36
            goto Le74
        Lc7b:
            r0 = move-exception
            r36 = r6
            r15 = r13
            r6 = r14
            r1 = r55
            r29 = r4
            r34 = r7
            r2 = r15
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r12 = r31
            r31 = r6
            r25 = r17
            r17 = r32
            r32 = r36
            goto Le74
        Lc9d:
            r39 = r0
            r38 = r1
            r30 = r2
            r35 = r4
            r36 = r6
            r34 = r7
            r45 = r12
            r6 = r14
            r0 = r15
            r37 = r17
            r14 = r22
            r43 = r32
            r12 = r5
            r15 = r13
            r13 = r24
            if (r20 != 0) goto Lcbf
            if (r10 == 0) goto Lcbc
            goto Lcbf
        Lcbc:
            r7 = r39
            goto Lcec
        Lcbf:
            java.lang.String r1 = r6.getPackageName()     // Catch: java.lang.Throwable -> Ld76
            r7 = r39
            boolean r1 = r7.areInstallPermissionsFixed(r1)     // Catch: java.lang.Throwable -> Ld76
            if (r1 != 0) goto Lcec
            boolean r1 = r6.isSystem()     // Catch: java.lang.Throwable -> Lcd2
            if (r1 == 0) goto Lcf6
            goto Lcec
        Lcd2:
            r0 = move-exception
            r1 = r55
            r22 = r13
            r2 = r15
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r25 = r37
            r17 = r43
            r31 = r6
            goto Le74
        Lcec:
            com.android.server.pm.pkg.PackageStateUnserialized r1 = r6.getTransientState()     // Catch: java.lang.Throwable -> Ld76
            boolean r1 = r1.isUpdatedSystemApp()     // Catch: java.lang.Throwable -> Ld76
            if (r1 == 0) goto Lcff
        Lcf6:
            java.lang.String r1 = r6.getPackageName()     // Catch: java.lang.Throwable -> Lcd2
            r5 = 1
            r7.setInstallPermissionsFixed(r1, r5)     // Catch: java.lang.Throwable -> Lcd2
            goto Ld00
        Lcff:
            r5 = 1
        Ld00:
            java.lang.String r4 = r52.getPackageName()     // Catch: java.lang.Throwable -> Ld76
            r18 = r21
            r21 = r38
            r17 = r43
            r1 = r51
            r2 = r3
            r24 = r3
            r22 = r13
            r13 = r25
            r3 = r4
            r29 = r35
            r25 = r37
            r4 = r23
            r30 = r5
            r11 = r27
            r27 = r12
            r12 = r31
            r5 = r29
            r31 = r6
            r35 = r30
            r32 = r36
            r30 = r45
            r6 = r27
            r36 = r7
            r16 = 0
            r7 = r15
            int[] r7 = r1.revokePermissionsNoLongerImplicitLocked(r2, r3, r4, r5, r6, r7)     // Catch: java.lang.Throwable -> Ld70
            r1 = r51
            r2 = r0
            r3 = r24
            r4 = r52
            r5 = r21
            r6 = r27
            int[] r1 = r1.setInitialGrantForNewImplicitPermissionsLocked(r2, r3, r4, r5, r6, r7)     // Catch: java.lang.Throwable -> Ld6a
            r2 = r1
            int r3 = r19 + 1
            r5 = r11
            r15 = r12
            r12 = r17
            r21 = r18
            r24 = r22
            r17 = r25
            r0 = r26
            r1 = r28
            r4 = r29
            r6 = r32
            r7 = r34
            r11 = r54
            r25 = r13
            r22 = r14
            r14 = r31
            r13 = r33
            goto L194
        Ld6a:
            r0 = move-exception
            r1 = r55
            r2 = r7
            goto Le74
        Ld70:
            r0 = move-exception
            r1 = r55
            r2 = r15
            goto Le74
        Ld76:
            r0 = move-exception
            r22 = r13
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r29 = r35
            r32 = r36
            r25 = r37
            r17 = r43
            r31 = r6
            r1 = r55
            r2 = r15
            goto Le74
        Ld90:
            r0 = move-exception
            r29 = r4
            r34 = r7
            r33 = r13
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r31 = r14
            r25 = r17
            r14 = r22
            r22 = r24
            r17 = r32
            r32 = r6
            r1 = r55
            r2 = r30
            goto Le74
        Ldb1:
            r0 = move-exception
            r30 = r2
            r29 = r4
            r34 = r7
            r33 = r13
            r18 = r21
            r13 = r25
            r11 = r27
            r12 = r31
            r31 = r14
            r25 = r17
            r14 = r22
            r22 = r24
            r17 = r32
            r32 = r6
            r1 = r55
            goto Le74
        Ldd2:
            r0 = move-exception
            r28 = r1
            r29 = r4
            r32 = r6
            r34 = r7
            r33 = r13
            r31 = r14
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r11 = r27
            r25 = r17
            r17 = r12
            r12 = r15
            r1 = r55
            goto Le74
        Ldf2:
            r0 = move-exception
            r28 = r1
            r29 = r4
            r11 = r5
            r32 = r6
            r34 = r7
            r33 = r13
            r31 = r14
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r25 = r17
            r17 = r12
            r12 = r15
            r1 = r55
            goto Le74
        Le11:
            r28 = r1
            r29 = r4
            r11 = r5
            r32 = r6
            r34 = r7
            r33 = r13
            r31 = r14
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r25
            r16 = 0
            r25 = r17
            r17 = r12
            r12 = r15
            monitor-exit(r13)     // Catch: java.lang.Throwable -> Le50
            int[] r0 = r8.checkIfLegacyStorageOpsNeedToBeUpdated(r9, r10, r11, r2)
            r1 = r55
            if (r1 == 0) goto Le3c
            r3 = r28
            r1.onPermissionUpdated(r0, r3)
            goto Le3e
        Le3c:
            r3 = r28
        Le3e:
            int r2 = r0.length
            r7 = r16
        Le41:
            if (r7 >= r2) goto Le4f
            r4 = r0[r7]
            java.lang.String r5 = r52.getPackageName()
            r8.notifyRuntimePermissionStateChanged(r5, r4)
            int r7 = r7 + 1
            goto Le41
        Le4f:
            return
        Le50:
            r0 = move-exception
            r1 = r55
            r3 = r28
            goto Le74
        Le56:
            r0 = move-exception
            r1 = r55
            r29 = r4
            r11 = r5
            r32 = r6
            r34 = r7
            r33 = r13
            r31 = r14
            r25 = r17
            r18 = r21
            r14 = r22
            r22 = r24
            r13 = r3
            r17 = r12
            r12 = r15
            r28 = r19
            r2 = r20
        Le74:
            monitor-exit(r13)     // Catch: java.lang.Throwable -> Le76
            throw r0
        Le76:
            r0 = move-exception
            goto Le74
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.permission.PermissionManagerServiceImpl.restorePermissionState(com.android.server.pm.parsing.pkg.AndroidPackage, boolean, java.lang.String, com.android.server.pm.permission.PermissionManagerServiceImpl$PermissionCallback, int):void");
    }

    private int[] getAllUserIds() {
        return UserManagerService.getInstance().getUserIdsIncludingPreCreated();
    }

    private int[] revokePermissionsNoLongerImplicitLocked(UidPermissionState ps, String packageName, Collection<String> uidImplicitPermissions, int uidTargetSdkVersion, int userId, int[] updatedUserIds) {
        boolean supportsRuntimePermissions = uidTargetSdkVersion >= 23;
        int[] updatedUserIds2 = updatedUserIds;
        for (String permission : ps.getGrantedPermissions()) {
            if (!uidImplicitPermissions.contains(permission)) {
                Permission bp = this.mRegistry.getPermission(permission);
                if (bp != null && bp.isRuntime()) {
                    int flags = ps.getPermissionFlags(permission);
                    if ((flags & 128) != 0) {
                        int flagsToRemove = 128;
                        boolean preserveGrant = false;
                        if (ArrayUtils.contains(NEARBY_DEVICES_PERMISSIONS, permission) && ps.isPermissionGranted("android.permission.ACCESS_BACKGROUND_LOCATION") && (ps.getPermissionFlags("android.permission.ACCESS_BACKGROUND_LOCATION") & FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_MANAGED_PROFILE_MAXIMUM_TIME_OFF) == 0) {
                            preserveGrant = true;
                        }
                        if ((flags & 52) == 0 && supportsRuntimePermissions && !preserveGrant) {
                            if (ps.revokePermission(bp) && PackageManagerService.DEBUG_PERMISSIONS) {
                                Slog.i(TAG, "Revoking runtime permission " + permission + " for " + packageName + " as it is now requested");
                            }
                            flagsToRemove = 128 | 3;
                        }
                        ps.updatePermissionFlags(bp, flagsToRemove, 0);
                        updatedUserIds2 = ArrayUtils.appendInt(updatedUserIds2, userId);
                    }
                }
            }
        }
        return updatedUserIds2;
    }

    private void inheritPermissionStateToNewImplicitPermissionLocked(ArraySet<String> sourcePerms, String newPerm, UidPermissionState ps, AndroidPackage pkg) {
        String pkgName = pkg.getPackageName();
        boolean isGranted = false;
        int flags = 0;
        int numSourcePerm = sourcePerms.size();
        for (int i = 0; i < numSourcePerm; i++) {
            String sourcePerm = sourcePerms.valueAt(i);
            if (ps.isPermissionGranted(sourcePerm)) {
                if (!isGranted) {
                    flags = 0;
                }
                isGranted = true;
                flags |= ps.getPermissionFlags(sourcePerm);
            } else if (!isGranted) {
                flags |= ps.getPermissionFlags(sourcePerm);
            }
        }
        if (isGranted) {
            if (PackageManagerService.DEBUG_PERMISSIONS) {
                Slog.i(TAG, newPerm + " inherits runtime perm grant from " + sourcePerms + " for " + pkgName);
            }
            ps.grantPermission(this.mRegistry.getPermission(newPerm));
        }
        ps.updatePermissionFlags(this.mRegistry.getPermission(newPerm), flags, flags);
    }

    private int[] checkIfLegacyStorageOpsNeedToBeUpdated(AndroidPackage pkg, boolean replace, int[] userIds, int[] updatedUserIds) {
        if (replace && pkg.isRequestLegacyExternalStorage() && (pkg.getRequestedPermissions().contains("android.permission.READ_EXTERNAL_STORAGE") || pkg.getRequestedPermissions().contains("android.permission.WRITE_EXTERNAL_STORAGE"))) {
            return (int[]) userIds.clone();
        }
        return updatedUserIds;
    }

    private int[] setInitialGrantForNewImplicitPermissionsLocked(UidPermissionState origPs, UidPermissionState ps, AndroidPackage pkg, ArraySet<String> newImplicitPermissions, int userId, int[] updatedUserIds) {
        ArrayMap<String, ArraySet<String>> newToSplitPerms;
        List<PermissionManager.SplitPermissionInfo> permissionList;
        int numSplitPerms;
        int numNewImplicitPerms;
        String pkgName = pkg.getPackageName();
        ArrayMap<String, ArraySet<String>> newToSplitPerms2 = new ArrayMap<>();
        List<PermissionManager.SplitPermissionInfo> permissionList2 = getSplitPermissionInfos();
        int numSplitPerms2 = permissionList2.size();
        for (int splitPermNum = 0; splitPermNum < numSplitPerms2; splitPermNum++) {
            PermissionManager.SplitPermissionInfo spi = permissionList2.get(splitPermNum);
            List<String> newPerms = spi.getNewPermissions();
            int numNewPerms = newPerms.size();
            for (int newPermNum = 0; newPermNum < numNewPerms; newPermNum++) {
                String newPerm = newPerms.get(newPermNum);
                ArraySet<String> splitPerms = newToSplitPerms2.get(newPerm);
                if (splitPerms == null) {
                    splitPerms = new ArraySet<>();
                    newToSplitPerms2.put(newPerm, splitPerms);
                }
                splitPerms.add(spi.getSplitPermission());
            }
        }
        int numNewImplicitPerms2 = newImplicitPermissions.size();
        int newImplicitPermNum = 0;
        int[] updatedUserIds2 = updatedUserIds;
        while (newImplicitPermNum < numNewImplicitPerms2) {
            String newPerm2 = newImplicitPermissions.valueAt(newImplicitPermNum);
            ArraySet<String> sourcePerms = newToSplitPerms2.get(newPerm2);
            if (sourcePerms != null) {
                Permission bp = this.mRegistry.getPermission(newPerm2);
                if (bp == null) {
                    throw new IllegalStateException("Unknown new permission in split permission: " + newPerm2);
                }
                if (bp.isRuntime()) {
                    if (!newPerm2.equals("android.permission.ACTIVITY_RECOGNITION") && !READ_MEDIA_AURAL_PERMISSIONS.contains(newPerm2) && !READ_MEDIA_VISUAL_PERMISSIONS.contains(newPerm2)) {
                        ps.updatePermissionFlags(bp, 128, 128);
                    }
                    updatedUserIds2 = ArrayUtils.appendInt(updatedUserIds2, userId);
                    if (origPs.hasPermissionState(sourcePerms)) {
                        newToSplitPerms = newToSplitPerms2;
                        permissionList = permissionList2;
                        numSplitPerms = numSplitPerms2;
                        numNewImplicitPerms = numNewImplicitPerms2;
                    } else {
                        boolean inheritsFromInstallPerm = false;
                        newToSplitPerms = newToSplitPerms2;
                        int sourcePermNum = 0;
                        while (true) {
                            permissionList = permissionList2;
                            if (sourcePermNum >= sourcePerms.size()) {
                                numSplitPerms = numSplitPerms2;
                                numNewImplicitPerms = numNewImplicitPerms2;
                                break;
                            }
                            String sourcePerm = sourcePerms.valueAt(sourcePermNum);
                            numSplitPerms = numSplitPerms2;
                            Permission sourceBp = this.mRegistry.getPermission(sourcePerm);
                            if (sourceBp == null) {
                                throw new IllegalStateException("Unknown source permission in split permission: " + sourcePerm);
                            }
                            if (sourceBp.isRuntime()) {
                                sourcePermNum++;
                                permissionList2 = permissionList;
                                numSplitPerms2 = numSplitPerms;
                            } else {
                                inheritsFromInstallPerm = true;
                                numNewImplicitPerms = numNewImplicitPerms2;
                                break;
                            }
                        }
                        if (!inheritsFromInstallPerm) {
                            if (PackageManagerService.DEBUG_PERMISSIONS) {
                                Slog.i(TAG, newPerm2 + " does not inherit from " + sourcePerms + " for " + pkgName + " as split permission is also new");
                            }
                        }
                    }
                    inheritPermissionStateToNewImplicitPermissionLocked(sourcePerms, newPerm2, ps, pkg);
                } else {
                    newToSplitPerms = newToSplitPerms2;
                    permissionList = permissionList2;
                    numSplitPerms = numSplitPerms2;
                    numNewImplicitPerms = numNewImplicitPerms2;
                }
            } else {
                newToSplitPerms = newToSplitPerms2;
                permissionList = permissionList2;
                numSplitPerms = numSplitPerms2;
                numNewImplicitPerms = numNewImplicitPerms2;
            }
            newImplicitPermNum++;
            permissionList2 = permissionList;
            newToSplitPerms2 = newToSplitPerms;
            numSplitPerms2 = numSplitPerms;
            numNewImplicitPerms2 = numNewImplicitPerms;
        }
        return updatedUserIds2;
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public List<SplitPermissionInfoParcelable> getSplitPermissions() {
        return PermissionManager.splitPermissionInfoListToParcelableList(getSplitPermissionInfos());
    }

    private List<PermissionManager.SplitPermissionInfo> getSplitPermissionInfos() {
        return SystemConfig.getInstance().getSplitPermissions();
    }

    private static boolean isCompatPlatformPermissionForPackage(String perm, AndroidPackage pkg) {
        int size = CompatibilityPermissionInfo.COMPAT_PERMS.length;
        for (int i = 0; i < size; i++) {
            CompatibilityPermissionInfo info = CompatibilityPermissionInfo.COMPAT_PERMS[i];
            if (info.getName().equals(perm) && pkg.getTargetSdkVersion() < info.getSdkVersion()) {
                Log.i(TAG, "Auto-granting " + perm + " to old pkg " + pkg.getPackageName());
                return true;
            }
        }
        return false;
    }

    private boolean checkPrivilegedPermissionAllowlist(AndroidPackage pkg, PackageStateInternal packageSetting, Permission permission) {
        if (RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_DISABLE) {
            return true;
        }
        String packageName = pkg.getPackageName();
        if (!Objects.equals(packageName, PackageManagerService.PLATFORM_PACKAGE_NAME) && pkg.isPrivileged() && this.mPrivilegedPermissionAllowlistSourcePackageNames.contains(permission.getPackageName())) {
            String permissionName = permission.getName();
            ApexManager apexManager = ApexManager.getInstance();
            String containingApexPackageName = apexManager.getActiveApexPackageNameContainingPackage(packageName);
            if (isInSystemConfigPrivAppPermissions(pkg, permissionName, containingApexPackageName)) {
                return true;
            }
            boolean z = false;
            if (isInSystemConfigPrivAppDenyPermissions(pkg, permissionName, containingApexPackageName)) {
                return false;
            }
            if (packageSetting.getTransientState().isUpdatedSystemApp()) {
                return true;
            }
            if (!this.mSystemReady) {
                if (containingApexPackageName != null && !ApexManager.isFactory(apexManager.getPackageInfo(containingApexPackageName, 1))) {
                    z = true;
                }
                boolean isInUpdatedApex = z;
                if (!isInUpdatedApex && !"com.android.cellbroadcastservice".equals(packageName) && !"com.android.cellbroadcastreceiver".equals(packageName)) {
                    Slog.w(TAG, "Privileged permission " + permissionName + " for package " + packageName + " (" + pkg.getPath() + ") not in privapp-permissions allowlist");
                    if (RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_ENFORCE) {
                        synchronized (this.mLock) {
                            if (this.mPrivappPermissionsViolations == null) {
                                this.mPrivappPermissionsViolations = new ArraySet<>();
                            }
                            this.mPrivappPermissionsViolations.add(packageName + " (" + pkg.getPath() + "): " + permissionName);
                        }
                    }
                }
            }
            boolean isInUpdatedApex2 = RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_ENFORCE;
            return true ^ isInUpdatedApex2;
        }
        return true;
    }

    private boolean isInSystemConfigPrivAppPermissions(AndroidPackage pkg, String permission, String containingApexPackageName) {
        Set<String> permissions;
        SystemConfig systemConfig = SystemConfig.getInstance();
        if (pkg.isVendor()) {
            permissions = systemConfig.getVendorPrivAppPermissions(pkg.getPackageName());
        } else if (pkg.isProduct()) {
            permissions = systemConfig.getProductPrivAppPermissions(pkg.getPackageName());
        } else if (pkg.isSystemExt()) {
            permissions = systemConfig.getSystemExtPrivAppPermissions(pkg.getPackageName());
        } else if (containingApexPackageName != null) {
            ApexManager apexManager = ApexManager.getInstance();
            String apexName = apexManager.getApexModuleNameForPackageName(containingApexPackageName);
            Set<String> privAppPermissions = systemConfig.getPrivAppPermissions(pkg.getPackageName());
            Set<String> apexPermissions = systemConfig.getApexPrivAppPermissions(apexName, pkg.getPackageName());
            if (privAppPermissions != null) {
                Slog.w(TAG, "Package " + pkg.getPackageName() + " is an APK in APEX, but has permission allowlist on the system image. Please bundle the allowlist in the " + containingApexPackageName + " APEX instead.");
                if (apexPermissions != null) {
                    Set<String> permissions2 = new ArraySet<>(privAppPermissions);
                    permissions2.addAll(apexPermissions);
                    permissions = permissions2;
                } else {
                    permissions = privAppPermissions;
                }
            } else {
                permissions = apexPermissions;
            }
        } else if (true == SystemProperties.getBoolean("ro.tran.partition.extend", false)) {
            permissions = getTrPrivPermissions(systemConfig, pkg);
        } else {
            permissions = systemConfig.getPrivAppPermissions(pkg.getPackageName());
        }
        return CollectionUtils.contains(permissions, permission);
    }

    private ArraySet<String> getTrPrivPermissions(SystemConfig systemConfig, AndroidPackage pkg) {
        if (pkg.getPath() == null) {
            return null;
        }
        boolean trProduct = pkg.getPath().startsWith(Environment.getTrProductDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trMi = pkg.getPath().startsWith(Environment.getTrMiDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trPreload = pkg.getPath().startsWith(Environment.getTrPreloadDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trCompany = pkg.getPath().startsWith(Environment.getTrCompanyDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trRegion = pkg.getPath().startsWith(Environment.getTrRegionDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trCarrier = pkg.getPath().startsWith(Environment.getTrCarrierDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trTheme = pkg.getPath().startsWith(Environment.getTrThemeDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        if (trProduct) {
            ArraySet<String> permissions = systemConfig.getTrProductPrivAppPermissions(pkg.getPackageName());
            return permissions;
        } else if (trMi) {
            ArraySet<String> permissions2 = systemConfig.getTrMiPrivAppPermissions(pkg.getPackageName());
            return permissions2;
        } else if (trPreload) {
            Slog.w("PackageManagerPermissionManagerService", "isInSystemConfigPrivAppPermissions path:" + pkg.getPath());
            ArraySet<String> permissions3 = systemConfig.getTrPreloadPrivAppPermissions(pkg.getPackageName());
            return permissions3;
        } else if (trCompany) {
            ArraySet<String> permissions4 = systemConfig.getTrCompanyPrivAppPermissions(pkg.getPackageName());
            return permissions4;
        } else if (trRegion) {
            ArraySet<String> permissions5 = systemConfig.getTrRegionPrivAppPermissions(pkg.getPackageName());
            return permissions5;
        } else if (trCarrier) {
            ArraySet<String> permissions6 = systemConfig.getTrCarrierPrivAppPermissions(pkg.getPackageName());
            return permissions6;
        } else if (trTheme) {
            ArraySet<String> permissions7 = systemConfig.getTrThemePrivAppPermissions(pkg.getPackageName());
            return permissions7;
        } else {
            ArraySet<String> permissions8 = systemConfig.getPrivAppPermissions(pkg.getPackageName());
            return permissions8;
        }
    }

    private Set<String> getTrPrivDenyPermissions(SystemConfig systemConfig, AndroidPackage pkg) {
        if (pkg.getPath() == null) {
            return null;
        }
        boolean trProduct = pkg.getPath().startsWith(Environment.getTrProductDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trMi = pkg.getPath().startsWith(Environment.getTrMiDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trPreload = pkg.getPath().startsWith(Environment.getTrPreloadDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trCompany = pkg.getPath().startsWith(Environment.getTrCompanyDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trRegion = pkg.getPath().startsWith(Environment.getTrRegionDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trCarrier = pkg.getPath().startsWith(Environment.getTrCarrierDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        boolean trTheme = pkg.getPath().startsWith(Environment.getTrThemeDirectory().toPath() + SliceClientPermissions.SliceAuthority.DELIMITER);
        if (trProduct) {
            ArraySet<String> permissions = systemConfig.getTrProductPrivAppDenyPermissions(pkg.getPackageName());
            return permissions;
        } else if (trMi) {
            ArraySet<String> permissions2 = systemConfig.getTrMiPrivAppDenyPermissions(pkg.getPackageName());
            return permissions2;
        } else if (trPreload) {
            ArraySet<String> permissions3 = systemConfig.getTrPreloadPrivAppDenyPermissions(pkg.getPackageName());
            return permissions3;
        } else if (trCompany) {
            ArraySet<String> permissions4 = systemConfig.getTrCompanyPrivAppDenyPermissions(pkg.getPackageName());
            return permissions4;
        } else if (trRegion) {
            ArraySet<String> permissions5 = systemConfig.getTrRegionPrivAppDenyPermissions(pkg.getPackageName());
            return permissions5;
        } else if (trCarrier) {
            ArraySet<String> permissions6 = systemConfig.getTrCarrierPrivAppDenyPermissions(pkg.getPackageName());
            return permissions6;
        } else if (trTheme) {
            ArraySet<String> permissions7 = systemConfig.getTrThemePrivAppDenyPermissions(pkg.getPackageName());
            return permissions7;
        } else {
            ArraySet<String> permissions8 = systemConfig.getPrivAppDenyPermissions(pkg.getPackageName());
            return permissions8;
        }
    }

    private boolean isInSystemConfigPrivAppDenyPermissions(AndroidPackage pkg, String permission, String containingApexPackageName) {
        Set<String> permissions;
        SystemConfig systemConfig = SystemConfig.getInstance();
        if (pkg.isVendor()) {
            permissions = systemConfig.getVendorPrivAppDenyPermissions(pkg.getPackageName());
        } else if (pkg.isProduct()) {
            permissions = systemConfig.getProductPrivAppDenyPermissions(pkg.getPackageName());
        } else if (pkg.isSystemExt()) {
            permissions = systemConfig.getSystemExtPrivAppDenyPermissions(pkg.getPackageName());
        } else if (containingApexPackageName != null) {
            permissions = systemConfig.getApexPrivAppDenyPermissions(containingApexPackageName, pkg.getPackageName());
        } else if (true == SystemProperties.getBoolean("ro.tran.partition.extend", false)) {
            permissions = getTrPrivDenyPermissions(systemConfig, pkg);
        } else {
            permissions = systemConfig.getPrivAppDenyPermissions(pkg.getPackageName());
        }
        return CollectionUtils.contains(permissions, permission);
    }

    private boolean shouldGrantPermissionBySignature(AndroidPackage pkg, Permission bp) {
        String systemPackageName = (String) ArrayUtils.firstOrNull(this.mPackageManagerInt.getKnownPackageNames(0, 0));
        AndroidPackage systemPackage = this.mPackageManagerInt.getPackage(systemPackageName);
        SigningDetails sourceSigningDetails = getSourcePackageSigningDetails(bp);
        return sourceSigningDetails.hasCommonSignerWithCapability(pkg.getSigningDetails(), 4) || pkg.getSigningDetails().hasAncestorOrSelf(systemPackage.getSigningDetails()) || systemPackage.getSigningDetails().checkCapability(pkg.getSigningDetails(), 4);
    }

    private boolean shouldGrantPermissionByProtectionFlags(AndroidPackage pkg, PackageStateInternal pkgSetting, Permission bp, ArraySet<String> shouldGrantPrivilegedPermissionIfWasGranted) {
        boolean allowed = false;
        boolean isPrivilegedPermission = bp.isPrivileged();
        boolean isOemPermission = bp.isOem();
        if (0 == 0 && ((isPrivilegedPermission || isOemPermission) && pkg.isSystem())) {
            String permissionName = bp.getName();
            if (pkgSetting.getTransientState().isUpdatedSystemApp()) {
                PackageStateInternal disabledPs = this.mPackageManagerInt.getDisabledSystemPackage(pkg.getPackageName());
                AndroidPackage disabledPkg = disabledPs == null ? null : disabledPs.getPkg();
                if (disabledPkg != null && ((isPrivilegedPermission && disabledPkg.isPrivileged()) || (isOemPermission && canGrantOemPermission(disabledPkg, permissionName)))) {
                    if (disabledPkg.getRequestedPermissions().contains(permissionName)) {
                        allowed = true;
                    } else {
                        shouldGrantPrivilegedPermissionIfWasGranted.add(permissionName);
                    }
                }
            } else {
                allowed = (isPrivilegedPermission && pkg.isPrivileged()) || (isOemPermission && canGrantOemPermission(pkg, permissionName));
            }
            if (allowed && isPrivilegedPermission && !bp.isVendorPrivileged() && pkg.isVendor()) {
                Slog.w(TAG, "Permission " + permissionName + " cannot be granted to privileged vendor apk " + pkg.getPackageName() + " because it isn't a 'vendorPrivileged' permission.");
                allowed = false;
            }
        }
        if (!allowed && bp.isPre23() && pkg.getTargetSdkVersion() < 23) {
            allowed = true;
        }
        if (!allowed && bp.isInstaller() && (ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(2, 0), pkg.getPackageName()) || ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(7, 0), pkg.getPackageName()))) {
            allowed = true;
        }
        if (!allowed && bp.isVerifier() && ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(4, 0), pkg.getPackageName())) {
            allowed = true;
        }
        if (!allowed && bp.isPreInstalled() && pkg.isSystem()) {
            allowed = true;
        }
        if (!allowed && bp.isKnownSigner()) {
            allowed = pkg.getSigningDetails().hasAncestorOrSelfWithDigest(bp.getKnownCerts());
        }
        if (!allowed && bp.isSetup() && ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(1, 0), pkg.getPackageName())) {
            allowed = true;
        }
        if (!allowed && bp.isSystemTextClassifier() && ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(6, 0), pkg.getPackageName())) {
            allowed = true;
        }
        if (!allowed && bp.isConfigurator() && ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(10, 0), pkg.getPackageName())) {
            allowed = true;
        }
        if (!allowed && bp.isIncidentReportApprover() && ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(11, 0), pkg.getPackageName())) {
            allowed = true;
        }
        if (!allowed && bp.isAppPredictor() && ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(12, 0), pkg.getPackageName())) {
            allowed = true;
        }
        if (!allowed && bp.isCompanion() && ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(15, 0), pkg.getPackageName())) {
            allowed = true;
        }
        if (!allowed && bp.isRetailDemo() && ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(16, 0), pkg.getPackageName()) && isProfileOwner(pkg.getUid())) {
            allowed = true;
        }
        if (!allowed && bp.isRecents() && ArrayUtils.contains(this.mPackageManagerInt.getKnownPackageNames(17, 0), pkg.getPackageName())) {
            return true;
        }
        return allowed;
    }

    private SigningDetails getSourcePackageSigningDetails(Permission bp) {
        PackageStateInternal ps = getSourcePackageSetting(bp);
        if (ps == null) {
            return SigningDetails.UNKNOWN;
        }
        return ps.getSigningDetails();
    }

    private PackageStateInternal getSourcePackageSetting(Permission bp) {
        String sourcePackageName = bp.getPackageName();
        return this.mPackageManagerInt.getPackageStateInternal(sourcePackageName);
    }

    private static boolean canGrantOemPermission(AndroidPackage pkg, String permission) {
        if (pkg.isOem()) {
            Boolean granted = (Boolean) SystemConfig.getInstance().getOemPermissions(pkg.getPackageName()).get(permission);
            if (granted != null) {
                return Boolean.TRUE == granted;
            }
            throw new IllegalStateException("OEM permission " + permission + " requested by package " + pkg.getPackageName() + " must be explicitly declared granted or not");
        }
        return false;
    }

    private static boolean isProfileOwner(int uid) {
        DevicePolicyManagerInternal dpmInternal = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (dpmInternal != null) {
            return dpmInternal.isActiveProfileOwner(uid) || dpmInternal.isActiveDeviceOwner(uid);
        }
        return false;
    }

    private boolean isPermissionsReviewRequiredInternal(String packageName, int userId) {
        boolean z;
        AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
        if (pkg == null) {
            return false;
        }
        if (pkg.getTargetSdkVersion() >= 23 && !CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported()) {
            return false;
        }
        synchronized (this.mLock) {
            UidPermissionState uidState = getUidStateLocked(pkg, userId);
            if (uidState == null) {
                Slog.e(TAG, "Missing permissions state for " + pkg.getPackageName() + " and user " + userId);
                return false;
            }
            boolean reviewRequired = uidState.isPermissionsReviewRequired();
            if (CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported()) {
                z = CtaManagerFactory.getInstance().makeCtaManager().isPermissionReviewRequired(pkg.getPackageName(), pkg.getSharedUserId(), pkg.getRequestedPermissions(), userId, reviewRequired);
            } else {
                z = reviewRequired;
            }
            return z;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void grantRequestedRuntimePermissionsInternal(AndroidPackage pkg, List<String> permissions, int userId) {
        boolean z;
        boolean shouldGrantPermission;
        int myUid;
        boolean supportsRuntimePermissions = pkg.getTargetSdkVersion() >= 23;
        boolean instantApp = this.mPackageManagerInt.isInstantApp(pkg.getPackageName(), userId);
        int myUid2 = Process.myUid();
        for (String permission : pkg.getRequestedPermissions()) {
            synchronized (this.mLock) {
                try {
                    Permission bp = this.mRegistry.getPermission(permission);
                    if (bp != null) {
                        try {
                            if ((bp.isRuntime() || bp.isDevelopment()) && ((!instantApp || bp.isInstant()) && ((supportsRuntimePermissions || !bp.isRuntimeOnly()) && (permissions == null || permissions.contains(permission))))) {
                                z = true;
                                shouldGrantPermission = z;
                            }
                        } catch (Throwable th) {
                            th = th;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            throw th;
                        }
                    }
                    z = false;
                    shouldGrantPermission = z;
                } catch (Throwable th3) {
                    th = th3;
                }
            }
            if (shouldGrantPermission) {
                int flags = getPermissionFlagsInternal(pkg.getPackageName(), permission, myUid2, userId);
                if (supportsRuntimePermissions) {
                    if ((flags & 20) == 0) {
                        grantRuntimePermissionInternal(pkg.getPackageName(), permission, false, myUid2, userId, this.mDefaultPermissionCallback);
                        myUid = myUid2;
                    } else {
                        myUid = myUid2;
                    }
                } else if ((flags & 72) != 0) {
                    myUid = myUid2;
                    updatePermissionFlagsInternal(pkg.getPackageName(), permission, 72, 0, myUid2, userId, false, this.mDefaultPermissionCallback);
                } else {
                    myUid = myUid2;
                }
            } else {
                myUid = myUid2;
            }
            myUid2 = myUid;
        }
    }

    public boolean isPackageNeedsReview(AndroidPackage pkg, SharedUserApi sharedUserApi) {
        if (CtaManagerFactory.getInstance().makeCtaManager().isCtaSupported()) {
            boolean appSupportsRuntimePermissions = pkg.getTargetSdkVersion() >= 23;
            if (pkg.getSharedUserId() == null) {
                return (appSupportsRuntimePermissions && isSystemApp(pkg)) ? false : true;
            }
            if (sharedUserApi != null) {
                for (AndroidPackage pkg2 : sharedUserApi.getPackages()) {
                    if (appSupportsRuntimePermissions) {
                        if (isSystemApp(pkg2)) {
                            return false;
                        }
                    } else if (pkg2.getTargetSdkVersion() >= 23) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isSystemApp(AndroidPackage pkg) {
        return (AndroidPackageUtils.generateAppInfoWithoutState(pkg).flags & 1) != 0;
    }

    /* JADX WARN: Code restructure failed: missing block: B:20:0x007c, code lost:
        if (r3 == false) goto L95;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x007e, code lost:
        if (r7 != null) goto L27;
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0080, code lost:
        r7 = new android.util.ArraySet<>();
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0086, code lost:
        r7.add(r6);
        r16 = r7;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x008c, code lost:
        r16 = r7;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x008e, code lost:
        r7 = getPermissionFlagsInternal(r28.getPackageName(), r6, r15, r31);
        r1 = r7;
        r2 = 0;
        r17 = r30;
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x009c, code lost:
        r3 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x009d, code lost:
        if (r17 == 0) goto L71;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x009f, code lost:
        r3 = 1 << java.lang.Integer.numberOfTrailingZeros(r17);
        r17 = r17 & (~r3);
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x00a7, code lost:
        switch(r3) {
            case 1: goto L58;
            case 2: goto L46;
            case 3: goto L70;
            case 4: goto L33;
            default: goto L70;
        };
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x00ab, code lost:
        r2 = r2 | 8192;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x00ad, code lost:
        if (r29 == null) goto L45;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x00b3, code lost:
        if (r29.contains(r6) == false) goto L38;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00b5, code lost:
        r1 = r1 | 8192;
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00b8, code lost:
        r1 = r1 & (-8193);
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00bb, code lost:
        r2 = r2 | 2048;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x00bd, code lost:
        if (r29 == null) goto L57;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x00c3, code lost:
        if (r29.contains(r6) == false) goto L51;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x00c5, code lost:
        r1 = r1 | 2048;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x00c8, code lost:
        r1 = r1 & (-2049);
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00cb, code lost:
        r2 = r2 | 4096;
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x00cd, code lost:
        if (r29 == null) goto L69;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00d3, code lost:
        if (r29.contains(r6) == false) goto L63;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x00d5, code lost:
        r1 = r1 | 4096;
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x00d8, code lost:
        r1 = r1 & (-4097);
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x00dc, code lost:
        if (r7 != r1) goto L75;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x00de, code lost:
        r26 = r9;
        r7 = r16;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x00e9, code lost:
        if ((r7 & 14336) == 0) goto L94;
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x00eb, code lost:
        r4 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x00ed, code lost:
        r4 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:56:0x00ee, code lost:
        r19 = r4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x00f2, code lost:
        if ((r1 & 14336) == 0) goto L80;
     */
    /* JADX WARN: Code restructure failed: missing block: B:59:0x00f5, code lost:
        r3 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:60:0x00f6, code lost:
        r20 = r3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x00fa, code lost:
        if ((r7 & 4) == 0) goto L86;
     */
    /* JADX WARN: Code restructure failed: missing block: B:62:0x00fc, code lost:
        if (r20 != false) goto L86;
     */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x00fe, code lost:
        if (r3 == false) goto L86;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x0100, code lost:
        r2 = r2 | 4;
        r1 = r1 & (-5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:66:0x010a, code lost:
        if (r28.getTargetSdkVersion() >= 23) goto L93;
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x010c, code lost:
        if (r19 != false) goto L93;
     */
    /* JADX WARN: Code restructure failed: missing block: B:68:0x010e, code lost:
        if (r20 == false) goto L93;
     */
    /* JADX WARN: Code restructure failed: missing block: B:69:0x0110, code lost:
        r21 = r1 | 64;
        r22 = r2 | 64;
     */
    /* JADX WARN: Code restructure failed: missing block: B:70:0x0119, code lost:
        r21 = r1;
        r22 = r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:71:0x011d, code lost:
        r26 = r9;
        updatePermissionFlagsInternal(r28.getPackageName(), r6, r22, r21, r15, r31, false, null);
        r7 = r16;
        r8 = true;
     */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:109:? -> B:79:0x014e). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void setAllowlistedRestrictedPermissionsInternal(AndroidPackage pkg, List<String> permissions, int allowlistFlags, int userId) {
        Permission bp;
        int j;
        int permissionCount = pkg.getRequestedPermissions().size();
        int myUid = Process.myUid();
        ArraySet<String> oldGrantedRestrictedPermissions = null;
        boolean updatePermissions = false;
        int j2 = 0;
        while (j2 < permissionCount) {
            String permissionName = pkg.getRequestedPermissions().get(j2);
            synchronized (this.mLock) {
                try {
                    bp = this.mRegistry.getPermission(permissionName);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    if (bp == null) {
                        j = j2;
                    } else if (bp.isHardOrSoftRestricted()) {
                        UidPermissionState uidState = getUidStateLocked(pkg, userId);
                        if (uidState == null) {
                            try {
                                Slog.e(TAG, "Missing permissions state for " + pkg.getPackageName() + " and user " + userId);
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        } else {
                            boolean isGranted = uidState.isPermissionGranted(permissionName);
                        }
                        j2 = j + 1;
                    } else {
                        j = j2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
            j = j2;
            j2 = j + 1;
        }
        if (updatePermissions) {
            restorePermissionState(pkg, false, pkg.getPackageName(), this.mDefaultPermissionCallback, userId);
            if (oldGrantedRestrictedPermissions == null) {
                return;
            }
            int oldGrantedCount = oldGrantedRestrictedPermissions.size();
            for (int j3 = 0; j3 < oldGrantedCount; j3++) {
                String permissionName2 = oldGrantedRestrictedPermissions.valueAt(j3);
                synchronized (this.mLock) {
                    UidPermissionState uidState2 = getUidStateLocked(pkg, userId);
                    if (uidState2 == null) {
                        Slog.e(TAG, "Missing permissions state for " + pkg.getPackageName() + " and user " + userId);
                    } else {
                        boolean isGranted2 = uidState2.isPermissionGranted(permissionName2);
                        if (!isGranted2) {
                            this.mDefaultPermissionCallback.onPermissionRevoked(pkg.getUid(), userId, null);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void revokeSharedUserPermissionsForLeavingPackageInternal(AndroidPackage pkg, final int appId, List<AndroidPackage> sharedUserPkgs, int userId) {
        if (pkg == null) {
            Slog.i(TAG, "Trying to update info for null package. Just ignoring");
        } else if (!sharedUserPkgs.isEmpty()) {
            PackageStateInternal disabledPs = this.mPackageManagerInt.getDisabledSystemPackage(pkg.getPackageName());
            boolean isShadowingSystemPkg = disabledPs != null && disabledPs.getAppId() == pkg.getUid();
            boolean shouldKillUid = false;
            for (String eachPerm : pkg.getRequestedPermissions()) {
                boolean used = false;
                Iterator<AndroidPackage> it = sharedUserPkgs.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    AndroidPackage sharedUserpkg = it.next();
                    if (sharedUserpkg != null && !sharedUserpkg.getPackageName().equals(pkg.getPackageName()) && sharedUserpkg.getRequestedPermissions().contains(eachPerm)) {
                        used = true;
                        break;
                    }
                }
                if (!used && (!isShadowingSystemPkg || !disabledPs.getPkg().getRequestedPermissions().contains(eachPerm))) {
                    synchronized (this.mLock) {
                        UidPermissionState uidState = getUidStateLocked(appId, userId);
                        if (uidState == null) {
                            Slog.e(TAG, "Missing permissions state for " + pkg.getPackageName() + " and user " + userId);
                        } else {
                            Permission bp = this.mRegistry.getPermission(eachPerm);
                            if (bp != null) {
                                if (uidState.removePermissionState(bp.getName()) && bp.hasGids()) {
                                    shouldKillUid = true;
                                }
                            }
                        }
                    }
                }
            }
            if (shouldKillUid) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        PermissionManagerServiceImpl.killUid(appId, -1, "permission grant or revoke changed gids");
                    }
                });
            }
        }
    }

    private boolean revokeUnusedSharedUserPermissionsLocked(Collection<String> uidRequestedPermissions, UidPermissionState uidState) {
        Permission bp;
        boolean runtimePermissionChanged = false;
        List<PermissionState> permissionStates = uidState.getPermissionStates();
        int permissionStatesSize = permissionStates.size();
        for (int i = permissionStatesSize - 1; i >= 0; i--) {
            PermissionState permissionState = permissionStates.get(i);
            if (!uidRequestedPermissions.contains(permissionState.getName()) && (bp = this.mRegistry.getPermission(permissionState.getName())) != null && uidState.removePermissionState(bp.getName()) && bp.isRuntime()) {
                runtimePermissionChanged = true;
            }
        }
        return runtimePermissionChanged;
    }

    private void updatePermissions(String packageName, AndroidPackage pkg) {
        int flags = pkg == null ? 3 : 2;
        updatePermissions(packageName, pkg, getVolumeUuidForPackage(pkg), flags, this.mDefaultPermissionCallback);
    }

    private void updateAllPermissions(String volumeUuid, boolean fingerprintChanged) {
        int i;
        PackageManager.corkPackageInfoCache();
        if (fingerprintChanged) {
            i = 6;
        } else {
            i = 0;
        }
        int flags = 1 | i;
        try {
            updatePermissions(null, null, volumeUuid, flags, this.mDefaultPermissionCallback);
        } finally {
            PackageManager.uncorkPackageInfoCache();
        }
    }

    private void updatePermissions(final String changingPkgName, final AndroidPackage changingPkg, final String replaceVolumeUuid, int flags, final PermissionCallback callback) {
        int flags2;
        boolean replace;
        boolean permissionTreesSourcePackageChanged = updatePermissionTreeSourcePackage(changingPkgName, changingPkg);
        boolean permissionSourcePackageChanged = updatePermissionSourcePackage(changingPkgName, callback);
        if (!(permissionTreesSourcePackageChanged | permissionSourcePackageChanged)) {
            flags2 = flags;
        } else {
            Slog.i(TAG, "Permission ownership changed. Updating all permissions.");
            flags2 = flags | 1;
        }
        Trace.traceBegin(262144L, "restorePermissionState");
        if ((flags2 & 1) != 0) {
            final boolean replaceAll = (flags2 & 4) != 0;
            this.mPackageManagerInt.forEachPackage(new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda17
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PermissionManagerServiceImpl.this.m5851xed51e874(changingPkg, replaceAll, replaceVolumeUuid, changingPkgName, callback, (AndroidPackage) obj);
                }
            });
        }
        if (changingPkg != null) {
            String volumeUuid = getVolumeUuidForPackage(changingPkg);
            if ((flags2 & 2) != 0 && Objects.equals(replaceVolumeUuid, volumeUuid)) {
                replace = true;
                restorePermissionState(changingPkg, replace, changingPkgName, callback, -1);
            }
            replace = false;
            restorePermissionState(changingPkg, replace, changingPkgName, callback, -1);
        }
        Trace.traceEnd(262144L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updatePermissions$9$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5851xed51e874(AndroidPackage changingPkg, boolean replaceAll, String replaceVolumeUuid, String changingPkgName, PermissionCallback callback, AndroidPackage pkg) {
        if (pkg == changingPkg) {
            return;
        }
        String volumeUuid = getVolumeUuidForPackage(pkg);
        boolean replace = replaceAll && Objects.equals(replaceVolumeUuid, volumeUuid);
        restorePermissionState(pkg, replace, changingPkgName, callback, -1);
    }

    private boolean updatePermissionSourcePackage(String packageName, final PermissionCallback callback) {
        if (packageName == null) {
            return true;
        }
        boolean changed = false;
        Set<Permission> needsUpdate = null;
        synchronized (this.mLock) {
            for (Permission bp : this.mRegistry.getPermissions()) {
                if (bp.isDynamic()) {
                    bp.updateDynamicPermission(this.mRegistry.getPermissionTrees());
                }
                if (packageName.equals(bp.getPackageName())) {
                    changed = true;
                    if (needsUpdate == null) {
                        needsUpdate = new ArraySet<>();
                    }
                    needsUpdate.add(bp);
                }
            }
        }
        if (needsUpdate != null) {
            AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
            for (final Permission bp2 : needsUpdate) {
                if (pkg == null || !hasPermission(pkg, bp2.getName())) {
                    if (!isPermissionDeclaredByDisabledSystemPkg(bp2)) {
                        Slog.i(TAG, "Removing permission " + bp2.getName() + " that used to be declared by " + bp2.getPackageName());
                        if (bp2.isRuntime()) {
                            int[] userIds = this.mUserManagerInt.getUserIds();
                            for (final int userId : userIds) {
                                this.mPackageManagerInt.forEachPackage(new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda8
                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        PermissionManagerServiceImpl.this.m5849x5b5dc0e(bp2, userId, callback, (AndroidPackage) obj);
                                    }
                                });
                            }
                        } else {
                            this.mPackageManagerInt.forEachPackage(new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda9
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    PermissionManagerServiceImpl.this.m5850xf945604f(bp2, (AndroidPackage) obj);
                                }
                            });
                        }
                    }
                    synchronized (this.mLock) {
                        this.mRegistry.removePermission(bp2.getName());
                    }
                } else {
                    AndroidPackage sourcePkg = this.mPackageManagerInt.getPackage(bp2.getPackageName());
                    PackageStateInternal sourcePs = this.mPackageManagerInt.getPackageStateInternal(bp2.getPackageName());
                    synchronized (this.mLock) {
                        if (sourcePkg == null || sourcePs == null) {
                            Slog.w(TAG, "Removing dangling permission: " + bp2.getName() + " from package " + bp2.getPackageName());
                            this.mRegistry.removePermission(bp2.getName());
                        }
                    }
                }
            }
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updatePermissionSourcePackage$10$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5849x5b5dc0e(Permission bp, int userId, PermissionCallback callback, AndroidPackage p) {
        revokePermissionFromPackageForUser(p.getPackageName(), bp.getName(), true, userId, callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updatePermissionSourcePackage$11$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5850xf945604f(Permission bp, AndroidPackage p) {
        int[] userIds = this.mUserManagerInt.getUserIds();
        synchronized (this.mLock) {
            for (int userId : userIds) {
                UidPermissionState uidState = getUidStateLocked(p, userId);
                if (uidState == null) {
                    Slog.e(TAG, "Missing permissions state for " + p.getPackageName() + " and user " + userId);
                } else {
                    uidState.removePermissionState(bp.getName());
                }
            }
        }
    }

    private boolean isPermissionDeclaredByDisabledSystemPkg(Permission permission) {
        PackageStateInternal disabledSourcePs = this.mPackageManagerInt.getDisabledSystemPackage(permission.getPackageName());
        if (disabledSourcePs != null && disabledSourcePs.getPkg() != null) {
            String permissionName = permission.getName();
            List<ParsedPermission> sourcePerms = disabledSourcePs.getPkg().getPermissions();
            for (ParsedPermission sourcePerm : sourcePerms) {
                if (TextUtils.equals(permissionName, sourcePerm.getName()) && permission.getProtectionLevel() == sourcePerm.getProtectionLevel()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private void revokePermissionFromPackageForUser(String pName, String permissionName, boolean overridePolicy, int userId, PermissionCallback callback) {
        ApplicationInfo appInfo = this.mPackageManagerInt.getApplicationInfo(pName, 0L, 1000, 0);
        if ((appInfo == null || appInfo.targetSdkVersion >= 23) && checkPermission(pName, permissionName, userId) == 0) {
            try {
                revokeRuntimePermissionInternal(pName, permissionName, overridePolicy, 1000, userId, null, callback);
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "Failed to revoke " + permissionName + " from " + pName, e);
            }
        }
    }

    private boolean updatePermissionTreeSourcePackage(String packageName, AndroidPackage pkg) {
        if (packageName == null) {
            return true;
        }
        boolean changed = false;
        Set<Permission> needsUpdate = null;
        synchronized (this.mLock) {
            Iterator<Permission> it = this.mRegistry.getPermissionTrees().iterator();
            while (it.hasNext()) {
                Permission bp = it.next();
                if (packageName.equals(bp.getPackageName())) {
                    changed = true;
                    if (pkg == null || !hasPermission(pkg, bp.getName())) {
                        Slog.i(TAG, "Removing permission tree " + bp.getName() + " that used to be declared by " + bp.getPackageName());
                        it.remove();
                    }
                    if (needsUpdate == null) {
                        needsUpdate = new ArraySet<>();
                    }
                    needsUpdate.add(bp);
                }
            }
        }
        if (needsUpdate != null) {
            for (Permission bp2 : needsUpdate) {
                AndroidPackage sourcePkg = this.mPackageManagerInt.getPackage(bp2.getPackageName());
                PackageStateInternal sourcePs = this.mPackageManagerInt.getPackageStateInternal(bp2.getPackageName());
                synchronized (this.mLock) {
                    if (sourcePkg == null || sourcePs == null) {
                        Slog.w(TAG, "Removing dangling permission tree: " + bp2.getName() + " from package " + bp2.getPackageName());
                        this.mRegistry.removePermission(bp2.getName());
                    }
                }
            }
        }
        return changed;
    }

    private void enforceGrantRevokeRuntimePermissionPermissions(String message) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS") != 0) {
            throw new SecurityException(message + " requires android.permission.GRANT_RUNTIME_PERMISSIONS or android.permission.REVOKE_RUNTIME_PERMISSIONS");
        }
    }

    private void enforceGrantRevokeGetRuntimePermissionPermissions(String message) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.GET_RUNTIME_PERMISSIONS") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS") != 0) {
            throw new SecurityException(message + " requires android.permission.GRANT_RUNTIME_PERMISSIONS or android.permission.REVOKE_RUNTIME_PERMISSIONS or android.permission.GET_RUNTIME_PERMISSIONS");
        }
    }

    private void enforceCrossUserPermission(int callingUid, int userId, boolean requireFullPermission, boolean checkShell, String message) {
        if (userId < 0) {
            throw new IllegalArgumentException("Invalid userId " + userId);
        }
        if (checkShell) {
            enforceShellRestriction("no_debugging_features", callingUid, userId);
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        if (checkCrossUserPermission(callingUid, callingUserId, userId, requireFullPermission)) {
            return;
        }
        String errorMessage = buildInvalidCrossUserPermissionMessage(callingUid, userId, message, requireFullPermission);
        Slog.w(TAG, errorMessage);
        throw new SecurityException(errorMessage);
    }

    private void enforceShellRestriction(String restriction, int callingUid, int userId) {
        if (callingUid == 2000) {
            if (userId >= 0 && this.mUserManagerInt.hasUserRestriction(restriction, userId)) {
                throw new SecurityException("Shell does not have permission to access user " + userId);
            }
            if (userId < 0) {
                Slog.e(LOG_TAG, "Unable to check shell permission for user " + userId + "\n\t" + Debug.getCallers(3));
            }
        }
    }

    private boolean checkCrossUserPermission(int callingUid, int callingUserId, int userId, boolean requireFullPermission) {
        if (userId == callingUserId || callingUid == 1000 || callingUid == 0) {
            return true;
        }
        if (requireFullPermission) {
            return checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL");
        }
        return checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") || checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS");
    }

    private boolean checkCallingOrSelfPermission(String permission) {
        return this.mContext.checkCallingOrSelfPermission(permission) == 0;
    }

    private static String buildInvalidCrossUserPermissionMessage(int callingUid, int userId, String message, boolean requireFullPermission) {
        StringBuilder builder = new StringBuilder();
        if (message != null) {
            builder.append(message);
            builder.append(": ");
        }
        builder.append("UID ");
        builder.append(callingUid);
        builder.append(" requires ");
        builder.append("android.permission.INTERACT_ACROSS_USERS_FULL");
        if (!requireFullPermission) {
            builder.append(" or ");
            builder.append("android.permission.INTERACT_ACROSS_USERS");
        }
        builder.append(" to access user ");
        builder.append(userId);
        builder.append(".");
        return builder.toString();
    }

    private int calculateCurrentPermissionFootprintLocked(Permission permissionTree) {
        int size = 0;
        for (Permission permission : this.mRegistry.getPermissions()) {
            size += permissionTree.calculateFootprint(permission);
        }
        return size;
    }

    private void enforcePermissionCapLocked(PermissionInfo info, Permission tree) {
        if (tree.getUid() != 1000) {
            int curTreeSize = calculateCurrentPermissionFootprintLocked(tree);
            if (info.calculateFootprint() + curTreeSize > 32768) {
                throw new SecurityException("Permission tree size cap exceeded");
            }
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void onSystemReady() {
        updateAllPermissions(StorageManager.UUID_PRIVATE_INTERNAL, false);
        PermissionPolicyInternal permissionPolicyInternal = (PermissionPolicyInternal) LocalServices.getService(PermissionPolicyInternal.class);
        permissionPolicyInternal.setOnInitializedCallback(new PermissionPolicyInternal.OnInitializedCallback() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda15
            @Override // com.android.server.policy.PermissionPolicyInternal.OnInitializedCallback
            public final void onInitialized(int i) {
                PermissionManagerServiceImpl.this.m5842xa177d08e(i);
            }
        });
        synchronized (this.mLock) {
            this.mSystemReady = true;
            if (this.mPrivappPermissionsViolations != null) {
                throw new IllegalStateException("Signature|privileged permissions not in privapp-permissions allowlist: " + this.mPrivappPermissionsViolations);
            }
        }
        this.mPermissionControllerManager = (PermissionControllerManager) this.mContext.getSystemService(PermissionControllerManager.class);
        this.mPermissionPolicyInternal = (PermissionPolicyInternal) LocalServices.getService(PermissionPolicyInternal.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSystemReady$12$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5842xa177d08e(int userId) {
        updateAllPermissions(StorageManager.UUID_PRIVATE_INTERNAL, false);
    }

    private static String getVolumeUuidForPackage(AndroidPackage pkg) {
        if (pkg == null) {
            return StorageManager.UUID_PRIVATE_INTERNAL;
        }
        if (pkg.isExternalStorage()) {
            if (TextUtils.isEmpty(pkg.getVolumeUuid())) {
                return "primary_physical";
            }
            return pkg.getVolumeUuid();
        }
        return StorageManager.UUID_PRIVATE_INTERNAL;
    }

    private static boolean hasPermission(AndroidPackage pkg, String permName) {
        if (pkg.getPermissions().isEmpty()) {
            return false;
        }
        for (int i = pkg.getPermissions().size() - 1; i >= 0; i--) {
            if (pkg.getPermissions().get(i).getName().equals(permName)) {
                return true;
            }
        }
        return false;
    }

    private void logPermission(int action, String name, String packageName) {
        LogMaker log = new LogMaker(action);
        log.setPackageName(packageName);
        log.addTaggedData(1241, name);
        this.mMetricsLogger.write(log);
    }

    private UidPermissionState getUidStateLocked(PackageStateInternal ps, int userId) {
        return getUidStateLocked(ps.getAppId(), userId);
    }

    private UidPermissionState getUidStateLocked(AndroidPackage pkg, int userId) {
        return getUidStateLocked(pkg.getUid(), userId);
    }

    private UidPermissionState getUidStateLocked(int appId, int userId) {
        UserPermissionState userState = this.mState.getUserState(userId);
        if (userState == null) {
            return null;
        }
        return userState.getUidState(appId);
    }

    private void removeUidStateAndResetPackageInstallPermissionsFixed(int appId, String packageName, int userId) {
        synchronized (this.mLock) {
            UserPermissionState userState = this.mState.getUserState(userId);
            if (userState == null) {
                return;
            }
            userState.removeUidState(appId);
            userState.setInstallPermissionsFixed(packageName, false);
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void readLegacyPermissionStateTEMP() {
        final int[] userIds = getAllUserIds();
        this.mPackageManagerInt.forEachPackageState(new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda10
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PermissionManagerServiceImpl.this.m5844xc93aad0d(userIds, (PackageStateInternal) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$readLegacyPermissionStateTEMP$13$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5844xc93aad0d(int[] userIds, PackageStateInternal ps) {
        LegacyPermissionState legacyState;
        int appId = ps.getAppId();
        if (ps.hasSharedUser()) {
            legacyState = this.mPackageManagerInt.getSharedUserApi(ps.getSharedUserAppId()).getSharedUserLegacyPermissionState();
        } else {
            legacyState = ps.getLegacyPermissionState();
        }
        synchronized (this.mLock) {
            for (int userId : userIds) {
                UserPermissionState userState = this.mState.getOrCreateUserState(userId);
                userState.setInstallPermissionsFixed(ps.getPackageName(), ps.isInstallPermissionsFixed());
                UidPermissionState uidState = userState.getOrCreateUidState(appId);
                uidState.reset();
                uidState.setMissing(legacyState.isMissing(userId));
                readLegacyPermissionStatesLocked(uidState, legacyState.getPermissionStates(userId));
            }
        }
    }

    private void readLegacyPermissionStatesLocked(UidPermissionState uidState, Collection<LegacyPermissionState.PermissionState> permissionStates) {
        for (LegacyPermissionState.PermissionState permissionState : permissionStates) {
            String permissionName = permissionState.getName();
            Permission permission = this.mRegistry.getPermission(permissionName);
            if (permission == null) {
                Slog.w(TAG, "Unknown permission: " + permissionName);
            } else {
                uidState.putPermissionState(permission, permissionState.isGranted(), permissionState.getFlags());
            }
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void writeLegacyPermissionStateTEMP() {
        final int[] userIds;
        synchronized (this.mLock) {
            userIds = this.mState.getUserIds();
        }
        this.mPackageManagerInt.forEachPackageSetting(new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PermissionManagerServiceImpl.this.m5852xa165d4fd(userIds, (PackageSetting) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$writeLegacyPermissionStateTEMP$14$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5852xa165d4fd(int[] userIds, PackageSetting ps) {
        LegacyPermissionState legacyState;
        PermissionManagerServiceImpl permissionManagerServiceImpl = this;
        int[] iArr = userIds;
        PackageSetting packageSetting = ps;
        int i = 0;
        packageSetting.setInstallPermissionsFixed(false);
        if (ps.hasSharedUser()) {
            legacyState = permissionManagerServiceImpl.mPackageManagerInt.getSharedUserApi(ps.getSharedUserAppId()).getSharedUserLegacyPermissionState();
        } else {
            legacyState = ps.getLegacyPermissionState();
        }
        legacyState.reset();
        int appId = ps.getAppId();
        synchronized (permissionManagerServiceImpl.mLock) {
            try {
                try {
                    int length = iArr.length;
                    while (i < length) {
                        int userId = iArr[i];
                        UserPermissionState userState = permissionManagerServiceImpl.mState.getUserState(userId);
                        if (userState == null) {
                            try {
                                Slog.e(TAG, "Missing user state for " + userId);
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } else {
                            if (userState.areInstallPermissionsFixed(ps.getPackageName())) {
                                packageSetting.setInstallPermissionsFixed(true);
                            }
                            UidPermissionState uidState = userState.getUidState(appId);
                            if (uidState == null) {
                                Slog.e(TAG, "Missing permission state for " + ps.getPackageName() + " and user " + userId);
                            } else {
                                legacyState.setMissing(uidState.isMissing(), userId);
                                List<PermissionState> permissionStates = uidState.getPermissionStates();
                                int permissionStatesSize = permissionStates.size();
                                int i2 = 0;
                                while (i2 < permissionStatesSize) {
                                    PermissionState permissionState = permissionStates.get(i2);
                                    int appId2 = appId;
                                    LegacyPermissionState.PermissionState legacyPermissionState = new LegacyPermissionState.PermissionState(permissionState.getName(), permissionState.getPermission().isRuntime(), permissionState.isGranted(), permissionState.getFlags());
                                    legacyState.putPermissionState(legacyPermissionState, userId);
                                    i2++;
                                    appId = appId2;
                                }
                            }
                        }
                        i++;
                        permissionManagerServiceImpl = this;
                        iArr = userIds;
                        packageSetting = ps;
                        appId = appId;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void readLegacyPermissionsTEMP(LegacyPermissionSettings legacyPermissionSettings) {
        List<LegacyPermission> legacyPermissions;
        for (int readPermissionOrPermissionTree = 0; readPermissionOrPermissionTree < 2; readPermissionOrPermissionTree++) {
            if (readPermissionOrPermissionTree == 0) {
                legacyPermissions = legacyPermissionSettings.getPermissions();
            } else {
                legacyPermissions = legacyPermissionSettings.getPermissionTrees();
            }
            synchronized (this.mLock) {
                int legacyPermissionsSize = legacyPermissions.size();
                for (int i = 0; i < legacyPermissionsSize; i++) {
                    LegacyPermission legacyPermission = legacyPermissions.get(i);
                    Permission permission = new Permission(legacyPermission.getPermissionInfo(), legacyPermission.getType());
                    if (readPermissionOrPermissionTree == 0) {
                        Permission configPermission = this.mRegistry.getPermission(permission.getName());
                        if (configPermission != null && configPermission.getType() == 1) {
                            permission.setGids(configPermission.getRawGids(), configPermission.areGidsPerUser());
                        }
                        this.mRegistry.addPermission(permission);
                    } else {
                        this.mRegistry.addPermissionTree(permission);
                    }
                }
            }
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void writeLegacyPermissionsTEMP(LegacyPermissionSettings legacyPermissionSettings) {
        int writePermissionOrPermissionTree = 0;
        while (writePermissionOrPermissionTree < 2) {
            List<LegacyPermission> legacyPermissions = new ArrayList<>();
            synchronized (this.mLock) {
                Collection<Permission> permissions = writePermissionOrPermissionTree == 0 ? this.mRegistry.getPermissions() : this.mRegistry.getPermissionTrees();
                for (Permission permission : permissions) {
                    LegacyPermission legacyPermission = new LegacyPermission(permission.getPermissionInfo(), permission.getType(), 0, EmptyArray.INT);
                    legacyPermissions.add(legacyPermission);
                }
            }
            if (writePermissionOrPermissionTree == 0) {
                legacyPermissionSettings.replacePermissions(legacyPermissions);
            } else {
                legacyPermissionSettings.replacePermissionTrees(legacyPermissions);
            }
            writePermissionOrPermissionTree++;
        }
    }

    private void onPackageAddedInternal(final AndroidPackage pkg, boolean isInstantApp, final AndroidPackage oldPkg) {
        List<String> permissionsWithChangedDefinition;
        if (!pkg.getAdoptPermissions().isEmpty()) {
            for (int i = pkg.getAdoptPermissions().size() - 1; i >= 0; i--) {
                String origName = pkg.getAdoptPermissions().get(i);
                if (canAdoptPermissionsInternal(origName, pkg)) {
                    Slog.i(TAG, "Adopting permissions from " + origName + " to " + pkg.getPackageName());
                    synchronized (this.mLock) {
                        this.mRegistry.transferPermissions(origName, pkg.getPackageName());
                    }
                }
            }
        }
        if (isInstantApp) {
            Slog.w(TAG, "Permission groups from package " + pkg.getPackageName() + " ignored: instant apps cannot define new permission groups.");
        } else {
            addAllPermissionGroupsInternal(pkg);
        }
        if (isInstantApp) {
            permissionsWithChangedDefinition = null;
            Slog.w(TAG, "Permissions from package " + pkg.getPackageName() + " ignored: instant apps cannot define new permissions.");
        } else {
            permissionsWithChangedDefinition = addAllPermissionsInternal(pkg);
        }
        final boolean hasOldPkg = oldPkg != null;
        final boolean hasPermissionDefinitionChanges = true ^ CollectionUtils.isEmpty(permissionsWithChangedDefinition);
        if (hasOldPkg || hasPermissionDefinitionChanges) {
            final List<String> list = permissionsWithChangedDefinition;
            AsyncTask.execute(new Runnable() { // from class: com.android.server.pm.permission.PermissionManagerServiceImpl$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    PermissionManagerServiceImpl.this.m5841xef4b10b6(hasOldPkg, pkg, oldPkg, hasPermissionDefinitionChanges, list);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPackageAddedInternal$15$com-android-server-pm-permission-PermissionManagerServiceImpl  reason: not valid java name */
    public /* synthetic */ void m5841xef4b10b6(boolean hasOldPkg, AndroidPackage pkg, AndroidPackage oldPkg, boolean hasPermissionDefinitionChanges, List permissionsWithChangedDefinition) {
        if (hasOldPkg) {
            revokeRuntimePermissionsIfGroupChangedInternal(pkg, oldPkg);
            revokeStoragePermissionsIfScopeExpandedInternal(pkg, oldPkg);
            revokeSystemAlertWindowIfUpgradedPast23(pkg, oldPkg);
        }
        if (hasPermissionDefinitionChanges) {
            revokeRuntimePermissionsIfPermissionDefinitionChangedInternal(permissionsWithChangedDefinition);
        }
    }

    private boolean canAdoptPermissionsInternal(String oldPackageName, AndroidPackage newPkg) {
        PackageStateInternal oldPs = this.mPackageManagerInt.getPackageStateInternal(oldPackageName);
        if (oldPs == null) {
            return false;
        }
        if (!oldPs.isSystem()) {
            Slog.w(TAG, "Unable to update from " + oldPs.getPackageName() + " to " + newPkg.getPackageName() + ": old package not in system partition");
            return false;
        } else if (this.mPackageManagerInt.getPackage(oldPs.getPackageName()) != null) {
            Slog.w(TAG, "Unable to update from " + oldPs.getPackageName() + " to " + newPkg.getPackageName() + ": old package still exists");
            return false;
        } else {
            return true;
        }
    }

    private boolean isEffectivelyGranted(PermissionState state) {
        int flags = state.getFlags();
        if ((flags & 16) != 0) {
            return true;
        }
        if ((flags & 4) != 0) {
            return (flags & 8) == 0 && state.isGranted();
        } else if ((65608 & flags) != 0) {
            return false;
        } else {
            return state.isGranted();
        }
    }

    private Pair<Boolean, Integer> mergePermissionState(int appId, PermissionState srcState, PermissionState destState) {
        boolean effectivelyGranted;
        int newFlags;
        boolean newGrantState;
        int destFlags = destState.getFlags();
        boolean destIsGranted = isEffectivelyGranted(destState);
        int srcFlags = srcState.getFlags();
        boolean srcIsGranted = isEffectivelyGranted(srcState);
        int combinedFlags = destFlags | srcFlags;
        int newFlags2 = 0 | (524291 & destFlags) | (combinedFlags & 14336);
        if ((newFlags2 & 14336) == 0) {
            newFlags2 |= 16384;
        }
        int newFlags3 = newFlags2 | (combinedFlags & 32820);
        if ((combinedFlags & 32820) == 0) {
            newFlags3 |= combinedFlags & 128;
        }
        if ((newFlags3 & 20) == 0) {
            if ((557091 & newFlags3) == 0 && NOTIFICATION_PERMISSIONS.contains(srcState.getName())) {
                newFlags3 |= combinedFlags & 64;
            } else if ((32820 & newFlags3) == 0) {
                newFlags3 |= destFlags & 64;
            }
        }
        if ((newFlags3 & 16) != 0) {
            effectivelyGranted = true;
        } else if ((destFlags & 4) != 0) {
            effectivelyGranted = destIsGranted;
        } else {
            boolean z = false;
            if ((srcFlags & 4) != 0) {
                if (destIsGranted || srcIsGranted) {
                    z = true;
                }
                effectivelyGranted = z;
                if (destIsGranted != srcIsGranted) {
                    newFlags3 &= -5;
                }
            } else if ((destFlags & 32800) != 0) {
                effectivelyGranted = destIsGranted;
            } else if ((32800 & srcFlags) != 0) {
                if (destIsGranted || srcIsGranted) {
                    z = true;
                }
                effectivelyGranted = z;
            } else if ((destFlags & 128) != 0) {
                effectivelyGranted = destIsGranted;
            } else if ((srcFlags & 128) != 0) {
                if (destIsGranted || srcIsGranted) {
                    z = true;
                }
                effectivelyGranted = z;
                if (destIsGranted) {
                    newFlags3 &= -129;
                }
            } else {
                effectivelyGranted = destIsGranted;
            }
        }
        if (!effectivelyGranted) {
            newFlags = (newFlags3 | (131072 & combinedFlags)) & (-129);
        } else {
            newFlags = newFlags3 & (-65);
        }
        if (effectivelyGranted != destIsGranted) {
            newFlags &= -524292;
        }
        if (!effectivelyGranted && isPermissionSplitFromNonRuntime(srcState.getName(), this.mPackageManagerInt.getUidTargetSdkVersion(appId))) {
            newFlags |= 8;
            newGrantState = true;
            return new Pair<>(Boolean.valueOf(newGrantState), Integer.valueOf(newFlags));
        }
        newGrantState = effectivelyGranted;
        return new Pair<>(Boolean.valueOf(newGrantState), Integer.valueOf(newFlags));
    }

    private void handleAppIdMigration(AndroidPackage pkg, int previousAppId) {
        UidPermissionState prevUidState;
        int[] iArr;
        int i;
        int[] iArr2;
        int i2;
        int userId;
        UidPermissionState uidState;
        PackageStateInternal ps = this.mPackageManagerInt.getPackageStateInternal(pkg.getPackageName());
        int i3 = 0;
        if (ps.hasSharedUser()) {
            synchronized (this.mLock) {
                int[] allUserIds = getAllUserIds();
                int length = allUserIds.length;
                while (i3 < length) {
                    int userId2 = allUserIds[i3];
                    UserPermissionState userState = this.mState.getOrCreateUserState(userId2);
                    UidPermissionState uidState2 = userState.getUidState(previousAppId);
                    if (uidState2 == null) {
                        iArr = allUserIds;
                        i = length;
                    } else {
                        UidPermissionState sharedUidState = userState.getUidState(ps.getAppId());
                        if (sharedUidState == null) {
                            userState.createUidStateWithExisting(ps.getAppId(), uidState2);
                            iArr = allUserIds;
                            i = length;
                        } else {
                            List<PermissionState> states = uidState2.getPermissionStates();
                            int count = states.size();
                            int i4 = 0;
                            while (i4 < count) {
                                PermissionState srcState = states.get(i4);
                                PermissionState destState = sharedUidState.getPermissionState(srcState.getName());
                                if (destState != null) {
                                    iArr2 = allUserIds;
                                    Pair<Boolean, Integer> newState = mergePermissionState(ps.getAppId(), srcState, destState);
                                    i2 = length;
                                    userId = userId2;
                                    uidState = uidState2;
                                    sharedUidState.putPermissionState(srcState.getPermission(), ((Boolean) newState.first).booleanValue(), ((Integer) newState.second).intValue());
                                } else {
                                    iArr2 = allUserIds;
                                    i2 = length;
                                    userId = userId2;
                                    uidState = uidState2;
                                    sharedUidState.putPermissionState(srcState.getPermission(), srcState.isGranted(), srcState.getFlags());
                                }
                                i4++;
                                allUserIds = iArr2;
                                length = i2;
                                userId2 = userId;
                                uidState2 = uidState;
                            }
                            iArr = allUserIds;
                            i = length;
                        }
                        userState.removeUidState(previousAppId);
                    }
                    i3++;
                    allUserIds = iArr;
                    length = i;
                }
            }
            return;
        }
        List<AndroidPackage> origSharedUserPackages = this.mPackageManagerInt.getPackagesForAppId(previousAppId);
        synchronized (this.mLock) {
            try {
                int[] allUserIds2 = getAllUserIds();
                int length2 = allUserIds2.length;
                while (i3 < length2) {
                    int userId3 = allUserIds2[i3];
                    UserPermissionState userState2 = this.mState.getUserState(userId3);
                    if (userState2 != null && (prevUidState = userState2.getUidState(previousAppId)) != null) {
                        userState2.createUidStateWithExisting(ps.getAppId(), prevUidState);
                        if (!origSharedUserPackages.isEmpty()) {
                            try {
                                revokeSharedUserPermissionsForLeavingPackageInternal(pkg, previousAppId, origSharedUserPackages, userId3);
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } else {
                            removeUidStateAndResetPackageInstallPermissionsFixed(previousAppId, pkg.getPackageName(), userId3);
                        }
                    }
                    i3++;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private void onPackageInstalledInternal(AndroidPackage pkg, int previousAppId, PermissionManagerServiceInternal.PackageInstalledParams params, int[] userIds) {
        if (previousAppId != -1) {
            handleAppIdMigration(pkg, previousAppId);
        }
        updatePermissions(pkg.getPackageName(), pkg);
        for (int userId : userIds) {
            addAllowlistedRestrictedPermissionsInternal(pkg, params.getAllowlistedRestrictedPermissions(), 2, userId);
            grantRequestedRuntimePermissionsInternal(pkg, params.getGrantedPermissions(), userId);
        }
    }

    private void addAllowlistedRestrictedPermissionsInternal(AndroidPackage pkg, List<String> allowlistedRestrictedPermissions, int flags, int userId) {
        List<String> permissions;
        List<String> permissions2 = getAllowlistedRestrictedPermissionsInternal(pkg, flags, userId);
        if (permissions2 != null) {
            ArraySet<String> permissionSet = new ArraySet<>(permissions2);
            permissionSet.addAll(allowlistedRestrictedPermissions);
            permissions = new ArrayList<>(permissionSet);
        } else {
            permissions = allowlistedRestrictedPermissions;
        }
        setAllowlistedRestrictedPermissionsInternal(pkg, permissions, flags, userId);
    }

    private void onPackageRemovedInternal(AndroidPackage pkg) {
        removeAllPermissionsInternal(pkg);
    }

    private void onPackageUninstalledInternal(String packageName, int appId, AndroidPackage pkg, List<AndroidPackage> sharedUserPkgs, int[] userIds) {
        int i = 0;
        if (pkg != null && pkg.isSystem() && this.mPackageManagerInt.getPackage(packageName) != null) {
            int length = userIds.length;
            while (i < length) {
                resetRuntimePermissionsInternal(pkg, userIds[i]);
                i++;
            }
            return;
        }
        updatePermissions(packageName, null);
        int length2 = userIds.length;
        while (i < length2) {
            int userId = userIds[i];
            if (sharedUserPkgs.isEmpty()) {
                removeUidStateAndResetPackageInstallPermissionsFixed(appId, packageName, userId);
            } else {
                revokeSharedUserPermissionsForLeavingPackageInternal(pkg, appId, sharedUserPkgs, userId);
            }
            i++;
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public List<LegacyPermission> getLegacyPermissions() {
        List<LegacyPermission> legacyPermissions;
        synchronized (this.mLock) {
            legacyPermissions = new ArrayList<>();
            for (Permission permission : this.mRegistry.getPermissions()) {
                LegacyPermission legacyPermission = new LegacyPermission(permission.getPermissionInfo(), permission.getType(), permission.getUid(), permission.getRawGids());
                legacyPermissions.add(legacyPermission);
            }
        }
        return legacyPermissions;
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public Map<String, Set<String>> getAllAppOpPermissionPackages() {
        Map<String, Set<String>> deepClone;
        synchronized (this.mLock) {
            ArrayMap<String, ArraySet<String>> appOpPermissionPackages = this.mRegistry.getAllAppOpPermissionPackages();
            deepClone = new ArrayMap<>();
            int appOpPermissionPackagesSize = appOpPermissionPackages.size();
            for (int i = 0; i < appOpPermissionPackagesSize; i++) {
                String appOpPermission = appOpPermissionPackages.keyAt(i);
                ArraySet<String> packageNames = appOpPermissionPackages.valueAt(i);
                deepClone.put(appOpPermission, new ArraySet<>(packageNames));
            }
        }
        return deepClone;
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public LegacyPermissionState getLegacyPermissionState(int appId) {
        int[] userIds;
        PermissionManagerServiceImpl permissionManagerServiceImpl = this;
        LegacyPermissionState legacyState = new LegacyPermissionState();
        synchronized (permissionManagerServiceImpl.mLock) {
            int[] userIds2 = permissionManagerServiceImpl.mState.getUserIds();
            int length = userIds2.length;
            int i = 0;
            while (i < length) {
                int userId = userIds2[i];
                UidPermissionState uidState = permissionManagerServiceImpl.getUidStateLocked(appId, userId);
                if (uidState == null) {
                    Slog.e(TAG, "Missing permissions state for app ID " + appId + " and user ID " + userId);
                    userIds = userIds2;
                } else {
                    List<PermissionState> permissionStates = uidState.getPermissionStates();
                    int permissionStatesSize = permissionStates.size();
                    int i2 = 0;
                    while (i2 < permissionStatesSize) {
                        PermissionState permissionState = permissionStates.get(i2);
                        LegacyPermissionState.PermissionState legacyPermissionState = new LegacyPermissionState.PermissionState(permissionState.getName(), permissionState.getPermission().isRuntime(), permissionState.isGranted(), permissionState.getFlags());
                        legacyState.putPermissionState(legacyPermissionState, userId);
                        i2++;
                        userIds2 = userIds2;
                    }
                    userIds = userIds2;
                }
                i++;
                permissionManagerServiceImpl = this;
                userIds2 = userIds;
            }
        }
        return legacyState;
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public int[] getGidsForUid(int uid) {
        int appId = UserHandle.getAppId(uid);
        int userId = UserHandle.getUserId(uid);
        synchronized (this.mLock) {
            UidPermissionState uidState = getUidStateLocked(appId, userId);
            if (uidState == null) {
                Slog.e(TAG, "Missing permissions state for app ID " + appId + " and user ID " + userId);
                return EMPTY_INT_ARRAY;
            }
            return uidState.computeGids(this.mGlobalGids, userId);
        }
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public boolean isPermissionsReviewRequired(String packageName, int userId) {
        Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
        return isPermissionsReviewRequiredInternal(packageName, userId);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public Set<String> getGrantedPermissions(String packageName, int userId) {
        Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
        Preconditions.checkArgumentNonNegative(userId, "userId");
        return getGrantedPermissionsInternal(packageName, userId);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public int[] getPermissionGids(String permissionName, int userId) {
        Objects.requireNonNull(permissionName, "permissionName");
        Preconditions.checkArgumentNonNegative(userId, "userId");
        return getPermissionGidsInternal(permissionName, userId);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public String[] getAppOpPermissionPackages(String permissionName) {
        Objects.requireNonNull(permissionName, "permissionName");
        return getAppOpPermissionPackagesInternal(permissionName);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void onStorageVolumeMounted(String volumeUuid, boolean fingerprintChanged) {
        updateAllPermissions(volumeUuid, fingerprintChanged);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void resetRuntimePermissions(AndroidPackage pkg, int userId) {
        Objects.requireNonNull(pkg, "pkg");
        Preconditions.checkArgumentNonNegative(userId, "userId");
        resetRuntimePermissionsInternal(pkg, userId);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public Permission getPermissionTEMP(String permName) {
        Permission permission;
        synchronized (this.mLock) {
            permission = this.mRegistry.getPermission(permName);
        }
        return permission;
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public ArrayList<PermissionInfo> getAllPermissionsWithProtection(int protection) {
        ArrayList<PermissionInfo> matchingPermissions = new ArrayList<>();
        synchronized (this.mLock) {
            for (Permission permission : this.mRegistry.getPermissions()) {
                if (permission.getProtection() == protection) {
                    matchingPermissions.add(permission.generatePermissionInfo(0));
                }
            }
        }
        return matchingPermissions;
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public ArrayList<PermissionInfo> getAllPermissionsWithProtectionFlags(int protectionFlags) {
        ArrayList<PermissionInfo> matchingPermissions = new ArrayList<>();
        synchronized (this.mLock) {
            for (Permission permission : this.mRegistry.getPermissions()) {
                if ((permission.getProtectionFlags() & protectionFlags) == protectionFlags) {
                    matchingPermissions.add(permission.generatePermissionInfo(0));
                }
            }
        }
        return matchingPermissions;
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void onUserCreated(int userId) {
        Preconditions.checkArgumentNonNegative(userId, "userId");
        updateAllPermissions(StorageManager.UUID_PRIVATE_INTERNAL, true);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void onPackageAdded(AndroidPackage pkg, boolean isInstantApp, AndroidPackage oldPkg) {
        Objects.requireNonNull(pkg);
        onPackageAddedInternal(pkg, isInstantApp, oldPkg);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void onPackageInstalled(AndroidPackage pkg, int previousAppId, PermissionManagerServiceInternal.PackageInstalledParams params, int userId) {
        Objects.requireNonNull(pkg, "pkg");
        Objects.requireNonNull(params, "params");
        Preconditions.checkArgument(userId >= 0 || userId == -1, "userId");
        int[] userIds = userId == -1 ? getAllUserIds() : new int[]{userId};
        onPackageInstalledInternal(pkg, previousAppId, params, userIds);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void onPackageRemoved(AndroidPackage pkg) {
        Objects.requireNonNull(pkg);
        onPackageRemovedInternal(pkg);
    }

    @Override // com.android.server.pm.permission.PermissionManagerServiceInterface
    public void onPackageUninstalled(String packageName, int appId, AndroidPackage pkg, List<AndroidPackage> sharedUserPkgs, int userId) {
        Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
        Objects.requireNonNull(sharedUserPkgs, "sharedUserPkgs");
        Preconditions.checkArgument(userId >= 0 || userId == -1, "userId");
        int[] userIds = userId == -1 ? getAllUserIds() : new int[]{userId};
        onPackageUninstalledInternal(packageName, appId, pkg, sharedUserPkgs, userIds);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PermissionCallback {
        private PermissionCallback() {
        }

        public void onGidsChanged(int appId, int userId) {
        }

        public void onPermissionChanged() {
        }

        public void onPermissionGranted(int uid, int userId) {
        }

        public void onInstallPermissionGranted() {
        }

        public void onPermissionRevoked(int uid, int userId, String reason) {
            onPermissionRevoked(uid, userId, reason, false);
        }

        public void onPermissionRevoked(int uid, int userId, String reason, boolean overrideKill) {
            onPermissionRevoked(uid, userId, reason, false, null);
        }

        public void onPermissionRevoked(int uid, int userId, String reason, boolean overrideKill, String permissionName) {
        }

        public void onInstallPermissionRevoked() {
        }

        public void onPermissionUpdated(int[] updatedUserIds, boolean sync) {
        }

        public void onPermissionUpdatedNotifyListener(int[] updatedUserIds, boolean sync, int uid) {
            onPermissionUpdated(updatedUserIds, sync);
        }

        public void onPermissionRemoved() {
        }

        public void onInstallPermissionUpdated() {
        }

        public void onInstallPermissionUpdatedNotifyListener(int uid) {
            onInstallPermissionUpdated();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class OnPermissionChangeListeners extends Handler {
        private static final int MSG_ON_PERMISSIONS_CHANGED = 1;
        private final RemoteCallbackList<IOnPermissionsChangeListener> mPermissionListeners;

        OnPermissionChangeListeners(Looper looper) {
            super(looper);
            this.mPermissionListeners = new RemoteCallbackList<>();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    int uid = msg.arg1;
                    handleOnPermissionsChanged(uid);
                    return;
                default:
                    return;
            }
        }

        public void addListener(IOnPermissionsChangeListener listener) {
            this.mPermissionListeners.register(listener);
        }

        public void removeListener(IOnPermissionsChangeListener listener) {
            this.mPermissionListeners.unregister(listener);
        }

        public void onPermissionsChanged(int uid) {
            if (this.mPermissionListeners.getRegisteredCallbackCount() > 0) {
                obtainMessage(1, uid, 0).sendToTarget();
            }
        }

        private void handleOnPermissionsChanged(int uid) {
            int count = this.mPermissionListeners.beginBroadcast();
            for (int i = 0; i < count; i++) {
                try {
                    IOnPermissionsChangeListener callback = this.mPermissionListeners.getBroadcastItem(i);
                    try {
                        callback.onPermissionsChanged(uid);
                    } catch (RemoteException e) {
                        Log.e(PermissionManagerServiceImpl.TAG, "Permission listener is dead", e);
                    }
                } finally {
                    this.mPermissionListeners.finishBroadcast();
                }
            }
        }
    }
}
