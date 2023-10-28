package com.android.server.pm.permission;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.content.AttributionSource;
import android.content.AttributionSourceState;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.permission.SplitPermissionInfoParcelable;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.permission.IOnPermissionsChangeListener;
import android.permission.IPermissionChecker;
import android.permission.IPermissionManager;
import android.permission.PermissionManager;
import android.permission.PermissionManagerInternal;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.TriFunction;
import com.android.server.LocalServices;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import com.android.server.vibrator.VibratorManagerService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class PermissionManagerService extends IPermissionManager.Stub {
    private static final String LOG_TAG = PermissionManagerService.class.getSimpleName();
    private static final ConcurrentHashMap<IBinder, RegisteredAttribution> sRunningAttributionSources = new ConcurrentHashMap<>();
    private final AppOpsManager mAppOpsManager;
    private final AttributionSourceRegistry mAttributionSourceRegistry;
    private CheckPermissionDelegate mCheckPermissionDelegate;
    private final Context mContext;
    private final DefaultPermissionGrantPolicy mDefaultPermissionGrantPolicy;
    private PermissionManagerServiceInternal.HotwordDetectionServiceProvider mHotwordDetectionServiceProvider;
    private final Object mLock = new Object();
    private final SparseArray<OneTimePermissionUserManager> mOneTimePermissionUserManagers = new SparseArray<>();
    private final PackageManagerInternal mPackageManagerInt;
    private final PermissionManagerServiceImpl mPermissionManagerServiceImpl;
    private final UserManagerInternal mUserManagerInt;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface CheckPermissionDelegate {
        int checkPermission(String str, String str2, int i, TriFunction<String, String, Integer, Integer> triFunction);

        int checkUidPermission(int i, String str, BiFunction<Integer, String, Integer> biFunction);

        List<String> getDelegatedPermissionNames();

        int getDelegatedUid();
    }

    PermissionManagerService(Context context, ArrayMap<String, FeatureInfo> availableFeatures) {
        PackageManager.invalidatePackageInfoCache();
        PermissionManager.disablePackageNamePermissionCache();
        this.mContext = context;
        this.mPackageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mUserManagerInt = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mAttributionSourceRegistry = new AttributionSourceRegistry(context);
        PermissionManagerServiceInternalImpl localService = new PermissionManagerServiceInternalImpl();
        LocalServices.addService(PermissionManagerServiceInternal.class, localService);
        LocalServices.addService(PermissionManagerInternal.class, localService);
        this.mPermissionManagerServiceImpl = new PermissionManagerServiceImpl(context, availableFeatures);
        this.mDefaultPermissionGrantPolicy = new DefaultPermissionGrantPolicy(context);
    }

    public static PermissionManagerServiceInternal create(Context context, ArrayMap<String, FeatureInfo> availableFeatures) {
        PermissionManagerServiceInternal permMgrInt = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
        if (permMgrInt != null) {
            return permMgrInt;
        }
        PermissionManagerService permissionService = (PermissionManagerService) ServiceManager.getService("permissionmgr");
        if (permissionService == null) {
            ServiceManager.addService("permissionmgr", new PermissionManagerService(context, availableFeatures));
            ServiceManager.addService("permission_checker", new PermissionCheckerService(context));
        }
        return (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
    }

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

    /* JADX INFO: Access modifiers changed from: private */
    public int checkPermission(String pkgName, String permName, int userId) {
        CheckPermissionDelegate checkPermissionDelegate;
        if (pkgName == null || permName == null) {
            return -1;
        }
        synchronized (this.mLock) {
            checkPermissionDelegate = this.mCheckPermissionDelegate;
        }
        if (checkPermissionDelegate == null) {
            return this.mPermissionManagerServiceImpl.checkPermission(pkgName, permName, userId);
        }
        final PermissionManagerServiceImpl permissionManagerServiceImpl = this.mPermissionManagerServiceImpl;
        Objects.requireNonNull(permissionManagerServiceImpl);
        return checkPermissionDelegate.checkPermission(pkgName, permName, userId, new TriFunction() { // from class: com.android.server.pm.permission.PermissionManagerService$$ExternalSyntheticLambda1
            public final Object apply(Object obj, Object obj2, Object obj3) {
                return Integer.valueOf(PermissionManagerServiceImpl.this.checkPermission((String) obj, (String) obj2, ((Integer) obj3).intValue()));
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int checkUidPermission(int uid, String permName) {
        CheckPermissionDelegate checkPermissionDelegate;
        if (permName == null) {
            return -1;
        }
        synchronized (this.mLock) {
            checkPermissionDelegate = this.mCheckPermissionDelegate;
        }
        if (checkPermissionDelegate == null) {
            return this.mPermissionManagerServiceImpl.checkUidPermission(uid, permName);
        }
        final PermissionManagerServiceImpl permissionManagerServiceImpl = this.mPermissionManagerServiceImpl;
        Objects.requireNonNull(permissionManagerServiceImpl);
        return checkPermissionDelegate.checkUidPermission(uid, permName, new BiFunction() { // from class: com.android.server.pm.permission.PermissionManagerService$$ExternalSyntheticLambda2
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                return Integer.valueOf(PermissionManagerServiceImpl.this.checkUidPermission(((Integer) obj).intValue(), (String) obj2));
            }
        });
    }

    public boolean setAutoRevokeExempted(String packageName, boolean exempted, int userId) {
        Objects.requireNonNull(packageName);
        AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
        int callingUid = Binder.getCallingUid();
        if (!checkAutoRevokeAccess(pkg, callingUid)) {
            return false;
        }
        return setAutoRevokeExemptedInternal(pkg, exempted, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setAutoRevokeExemptedInternal(AndroidPackage pkg, boolean exempted, int userId) {
        int packageUid = UserHandle.getUid(userId, pkg.getUid());
        if (this.mAppOpsManager.checkOpNoThrow(98, packageUid, pkg.getPackageName()) != 0) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mAppOpsManager.setMode(97, packageUid, pkg.getPackageName(), exempted ? 1 : 0);
            return true;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean checkAutoRevokeAccess(AndroidPackage pkg, int callingUid) {
        if (pkg == null) {
            return false;
        }
        boolean isCallerPrivileged = this.mContext.checkCallingOrSelfPermission("android.permission.WHITELIST_AUTO_REVOKE_PERMISSIONS") == 0;
        boolean isCallerInstallerOnRecord = this.mPackageManagerInt.isCallerInstallerOfRecord(pkg, callingUid);
        if (isCallerPrivileged || isCallerInstallerOnRecord) {
            return true;
        }
        throw new SecurityException("Caller must either hold android.permission.WHITELIST_AUTO_REVOKE_PERMISSIONS or be the installer on record");
    }

    public boolean isAutoRevokeExempted(String packageName, int userId) {
        Objects.requireNonNull(packageName);
        AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
        int callingUid = Binder.getCallingUid();
        if (!this.mPackageManagerInt.filterAppAccess(packageName, callingUid, userId) && checkAutoRevokeAccess(pkg, callingUid)) {
            int packageUid = UserHandle.getUid(userId, pkg.getUid());
            long identity = Binder.clearCallingIdentity();
            try {
                return this.mAppOpsManager.checkOpNoThrow(97, packageUid, packageName) == 1;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startShellPermissionIdentityDelegationInternal(int uid, String packageName, List<String> permissionNames) {
        synchronized (this.mLock) {
            CheckPermissionDelegate oldDelegate = this.mCheckPermissionDelegate;
            if (oldDelegate != null && oldDelegate.getDelegatedUid() != uid) {
                throw new SecurityException("Shell can delegate permissions only to one UID at a time");
            }
            ShellDelegate delegate = new ShellDelegate(uid, packageName, permissionNames);
            setCheckPermissionDelegateLocked(delegate);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopShellPermissionIdentityDelegationInternal() {
        synchronized (this.mLock) {
            setCheckPermissionDelegateLocked(null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<String> getDelegatedShellPermissionsInternal() {
        synchronized (this.mLock) {
            CheckPermissionDelegate checkPermissionDelegate = this.mCheckPermissionDelegate;
            if (checkPermissionDelegate == null) {
                return Collections.EMPTY_LIST;
            }
            return checkPermissionDelegate.getDelegatedPermissionNames();
        }
    }

    private void setCheckPermissionDelegateLocked(CheckPermissionDelegate delegate) {
        if (delegate != null || this.mCheckPermissionDelegate != null) {
            PackageManager.invalidatePackageInfoCache();
        }
        this.mCheckPermissionDelegate = delegate;
    }

    private OneTimePermissionUserManager getOneTimePermissionUserManager(int userId) {
        synchronized (this.mLock) {
            OneTimePermissionUserManager oneTimePermissionUserManager = this.mOneTimePermissionUserManagers.get(userId);
            if (oneTimePermissionUserManager != null) {
                return oneTimePermissionUserManager;
            }
            OneTimePermissionUserManager newOneTimePermissionUserManager = new OneTimePermissionUserManager(this.mContext.createContextAsUser(UserHandle.of(userId), 0));
            synchronized (this.mLock) {
                OneTimePermissionUserManager oneTimePermissionUserManager2 = this.mOneTimePermissionUserManagers.get(userId);
                if (oneTimePermissionUserManager2 != null) {
                    return oneTimePermissionUserManager2;
                }
                this.mOneTimePermissionUserManagers.put(userId, newOneTimePermissionUserManager);
                newOneTimePermissionUserManager.registerUninstallListener();
                return newOneTimePermissionUserManager;
            }
        }
    }

    public void startOneTimePermissionSession(String packageName, int userId, long timeoutMillis, long revokeAfterKilledDelayMillis) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_ONE_TIME_PERMISSION_SESSIONS", "Must hold android.permission.MANAGE_ONE_TIME_PERMISSION_SESSIONS to register permissions as one time.");
        Objects.requireNonNull(packageName);
        long token = Binder.clearCallingIdentity();
        try {
            getOneTimePermissionUserManager(userId).startPackageOneTimeSession(packageName, timeoutMillis, revokeAfterKilledDelayMillis);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void stopOneTimePermissionSession(String packageName, int userId) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_ONE_TIME_PERMISSION_SESSIONS", "Must hold android.permission.MANAGE_ONE_TIME_PERMISSION_SESSIONS to remove permissions as one time.");
        Objects.requireNonNull(packageName);
        long token = Binder.clearCallingIdentity();
        try {
            getOneTimePermissionUserManager(userId).stopPackageOneTimeSession(packageName);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void registerAttributionSource(AttributionSourceState source) {
        this.mAttributionSourceRegistry.registerAttributionSource(new AttributionSource(source));
    }

    public boolean isRegisteredAttributionSource(AttributionSourceState source) {
        return this.mAttributionSourceRegistry.isRegisteredAttributionSource(new AttributionSource(source));
    }

    public List<String> getAutoRevokeExemptionRequestedPackages(int userId) {
        return getPackagesWithAutoRevokePolicy(1, userId);
    }

    public List<String> getAutoRevokeExemptionGrantedPackages(int userId) {
        return getPackagesWithAutoRevokePolicy(2, userId);
    }

    private List<String> getPackagesWithAutoRevokePolicy(final int autoRevokePolicy, int userId) {
        this.mContext.enforceCallingPermission("android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY", "Must hold android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY");
        final List<String> result = new ArrayList<>();
        this.mPackageManagerInt.forEachInstalledPackage(new Consumer() { // from class: com.android.server.pm.permission.PermissionManagerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PermissionManagerService.lambda$getPackagesWithAutoRevokePolicy$0(autoRevokePolicy, result, (AndroidPackage) obj);
            }
        }, userId);
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getPackagesWithAutoRevokePolicy$0(int autoRevokePolicy, List result, AndroidPackage pkg) {
        if (pkg.getAutoRevokePermissions() == autoRevokePolicy) {
            result.add(pkg.getPackageName());
        }
    }

    public ParceledListSlice<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        return new ParceledListSlice<>(this.mPermissionManagerServiceImpl.getAllPermissionGroups(flags));
    }

    public PermissionGroupInfo getPermissionGroupInfo(String groupName, int flags) {
        return this.mPermissionManagerServiceImpl.getPermissionGroupInfo(groupName, flags);
    }

    public PermissionInfo getPermissionInfo(String permissionName, String packageName, int flags) {
        return this.mPermissionManagerServiceImpl.getPermissionInfo(permissionName, packageName, flags);
    }

    public ParceledListSlice<PermissionInfo> queryPermissionsByGroup(String groupName, int flags) {
        List<PermissionInfo> permissionInfo = this.mPermissionManagerServiceImpl.queryPermissionsByGroup(groupName, flags);
        if (permissionInfo == null) {
            return null;
        }
        return new ParceledListSlice<>(permissionInfo);
    }

    public boolean addPermission(PermissionInfo permissionInfo, boolean async) {
        return this.mPermissionManagerServiceImpl.addPermission(permissionInfo, async);
    }

    public void removePermission(String permissionName) {
        this.mPermissionManagerServiceImpl.removePermission(permissionName);
    }

    public int getPermissionFlags(String packageName, String permissionName, int userId) {
        return this.mPermissionManagerServiceImpl.getPermissionFlags(packageName, permissionName, userId);
    }

    public void updatePermissionFlags(String packageName, String permissionName, int flagMask, int flagValues, boolean checkAdjustPolicyFlagPermission, int userId) {
        this.mPermissionManagerServiceImpl.updatePermissionFlags(packageName, permissionName, flagMask, flagValues, checkAdjustPolicyFlagPermission, userId);
    }

    public void updatePermissionFlagsForAllApps(int flagMask, int flagValues, int userId) {
        this.mPermissionManagerServiceImpl.updatePermissionFlagsForAllApps(flagMask, flagValues, userId);
    }

    public void addOnPermissionsChangeListener(IOnPermissionsChangeListener listener) {
        this.mPermissionManagerServiceImpl.addOnPermissionsChangeListener(listener);
    }

    public void removeOnPermissionsChangeListener(IOnPermissionsChangeListener listener) {
        this.mPermissionManagerServiceImpl.removeOnPermissionsChangeListener(listener);
    }

    public List<String> getAllowlistedRestrictedPermissions(String packageName, int flags, int userId) {
        return this.mPermissionManagerServiceImpl.getAllowlistedRestrictedPermissions(packageName, flags, userId);
    }

    public boolean addAllowlistedRestrictedPermission(String packageName, String permissionName, int flags, int userId) {
        return this.mPermissionManagerServiceImpl.addAllowlistedRestrictedPermission(packageName, permissionName, flags, userId);
    }

    public boolean removeAllowlistedRestrictedPermission(String packageName, String permissionName, int flags, int userId) {
        return this.mPermissionManagerServiceImpl.removeAllowlistedRestrictedPermission(packageName, permissionName, flags, userId);
    }

    public void grantRuntimePermission(String packageName, String permissionName, int userId) {
        this.mPermissionManagerServiceImpl.grantRuntimePermission(packageName, permissionName, userId);
    }

    public void revokeRuntimePermission(String packageName, String permissionName, int userId, String reason) {
        this.mPermissionManagerServiceImpl.revokeRuntimePermission(packageName, permissionName, userId, reason);
    }

    public void revokePostNotificationPermissionWithoutKillForTest(String packageName, int userId) {
        this.mPermissionManagerServiceImpl.revokePostNotificationPermissionWithoutKillForTest(packageName, userId);
    }

    public boolean shouldShowRequestPermissionRationale(String packageName, String permissionName, int userId) {
        return this.mPermissionManagerServiceImpl.shouldShowRequestPermissionRationale(packageName, permissionName, userId);
    }

    public boolean isPermissionRevokedByPolicy(String packageName, String permissionName, int userId) {
        return this.mPermissionManagerServiceImpl.isPermissionRevokedByPolicy(packageName, permissionName, userId);
    }

    public List<SplitPermissionInfoParcelable> getSplitPermissions() {
        return this.mPermissionManagerServiceImpl.getSplitPermissions();
    }

    /* loaded from: classes2.dex */
    private class PermissionManagerServiceInternalImpl implements PermissionManagerServiceInternal {
        private PermissionManagerServiceInternalImpl() {
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public int checkPermission(String packageName, String permissionName, int userId) {
            return PermissionManagerService.this.checkPermission(packageName, permissionName, userId);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void grantRequestedRuntimePermissionsInternal(AndroidPackage pkg, List<String> permissions, int userId) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.grantRequestedRuntimePermissionsInternal(pkg, permissions, userId);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public int checkUidPermission(int uid, String permissionName) {
            return PermissionManagerService.this.checkUidPermission(uid, permissionName);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void startShellPermissionIdentityDelegation(int uid, String packageName, List<String> permissionNames) {
            Objects.requireNonNull(packageName, DomainVerificationLegacySettings.ATTR_PACKAGE_NAME);
            PermissionManagerService.this.startShellPermissionIdentityDelegationInternal(uid, packageName, permissionNames);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void stopShellPermissionIdentityDelegation() {
            PermissionManagerService.this.stopShellPermissionIdentityDelegationInternal();
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public List<String> getDelegatedShellPermissions() {
            return PermissionManagerService.this.getDelegatedShellPermissionsInternal();
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void setHotwordDetectionServiceProvider(PermissionManagerServiceInternal.HotwordDetectionServiceProvider provider) {
            PermissionManagerService.this.mHotwordDetectionServiceProvider = provider;
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public PermissionManagerServiceInternal.HotwordDetectionServiceProvider getHotwordDetectionServiceProvider() {
            return PermissionManagerService.this.mHotwordDetectionServiceProvider;
        }

        @Override // com.android.server.pm.permission.LegacyPermissionDataProvider
        public int[] getGidsForUid(int uid) {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.getGidsForUid(uid);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionDataProvider
        public Map<String, Set<String>> getAllAppOpPermissionPackages() {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.getAllAppOpPermissionPackages();
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void onUserCreated(int userId) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.onUserCreated(userId);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionDataProvider
        public List<LegacyPermission> getLegacyPermissions() {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.getLegacyPermissions();
        }

        @Override // com.android.server.pm.permission.LegacyPermissionDataProvider
        public LegacyPermissionState getLegacyPermissionState(int appId) {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.getLegacyPermissionState(appId);
        }

        public byte[] backupRuntimePermissions(int userId) {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.backupRuntimePermissions(userId);
        }

        public void restoreRuntimePermissions(byte[] backup, int userId) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.restoreRuntimePermissions(backup, userId);
        }

        public void restoreDelayedRuntimePermissions(String packageName, int userId) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.restoreDelayedRuntimePermissions(packageName, userId);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void readLegacyPermissionsTEMP(LegacyPermissionSettings legacyPermissionSettings) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.readLegacyPermissionsTEMP(legacyPermissionSettings);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void writeLegacyPermissionsTEMP(LegacyPermissionSettings legacyPermissionSettings) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.writeLegacyPermissionsTEMP(legacyPermissionSettings);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void onPackageAdded(AndroidPackage pkg, boolean isInstantApp, AndroidPackage oldPkg) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.onPackageAdded(pkg, isInstantApp, oldPkg);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void onPackageInstalled(AndroidPackage pkg, int previousAppId, PermissionManagerServiceInternal.PackageInstalledParams params, int rawUserId) {
            Objects.requireNonNull(pkg, "pkg");
            Objects.requireNonNull(params, "params");
            Preconditions.checkArgument(rawUserId >= 0 || rawUserId == -1, "userId");
            PermissionManagerService.this.mPermissionManagerServiceImpl.onPackageInstalled(pkg, previousAppId, params, rawUserId);
            int[] userIds = rawUserId == -1 ? PermissionManagerService.this.getAllUserIds() : new int[]{rawUserId};
            for (int userId : userIds) {
                int autoRevokePermissionsMode = params.getAutoRevokePermissionsMode();
                if (autoRevokePermissionsMode == 0 || autoRevokePermissionsMode == 1) {
                    PermissionManagerService.this.setAutoRevokeExemptedInternal(pkg, autoRevokePermissionsMode == 1, userId);
                }
            }
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void onPackageRemoved(AndroidPackage pkg) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.onPackageRemoved(pkg);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void onPackageUninstalled(String packageName, int appId, AndroidPackage pkg, List<AndroidPackage> sharedUserPkgs, int userId) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.onPackageUninstalled(packageName, appId, pkg, sharedUserPkgs, userId);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void addOnRuntimePermissionStateChangedListener(PermissionManagerServiceInternal.OnRuntimePermissionStateChangedListener listener) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.addOnRuntimePermissionStateChangedListener(listener);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void removeOnRuntimePermissionStateChangedListener(PermissionManagerServiceInternal.OnRuntimePermissionStateChangedListener listener) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.removeOnRuntimePermissionStateChangedListener(listener);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void onSystemReady() {
            PermissionManagerService.this.mPermissionManagerServiceImpl.onSystemReady();
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public boolean isPermissionsReviewRequired(String packageName, int userId) {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.isPermissionsReviewRequired(packageName, userId);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void readLegacyPermissionStateTEMP() {
            PermissionManagerService.this.mPermissionManagerServiceImpl.readLegacyPermissionStateTEMP();
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal, com.android.server.pm.permission.LegacyPermissionDataProvider
        public void writeLegacyPermissionStateTEMP() {
            PermissionManagerService.this.mPermissionManagerServiceImpl.writeLegacyPermissionStateTEMP();
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void onUserRemoved(int userId) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.onUserRemoved(userId);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public Set<String> getGrantedPermissions(String packageName, int userId) {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.getGrantedPermissions(packageName, userId);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public int[] getPermissionGids(String permissionName, int userId) {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.getPermissionGids(permissionName, userId);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public String[] getAppOpPermissionPackages(String permissionName) {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.getAppOpPermissionPackages(permissionName);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void onStorageVolumeMounted(String volumeUuid, boolean fingerprintChanged) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.onStorageVolumeMounted(volumeUuid, fingerprintChanged);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public void resetRuntimePermissions(AndroidPackage pkg, int userId) {
            PermissionManagerService.this.mPermissionManagerServiceImpl.resetRuntimePermissions(pkg, userId);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public DefaultPermissionGrantPolicy getDefaultPermissionGrantPolicy() {
            return PermissionManagerService.this.mDefaultPermissionGrantPolicy;
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public Permission getPermissionTEMP(String permName) {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.getPermissionTEMP(permName);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public ArrayList<PermissionInfo> getAllPermissionsWithProtection(int protection) {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.getAllPermissionsWithProtection(protection);
        }

        @Override // com.android.server.pm.permission.PermissionManagerServiceInternal
        public ArrayList<PermissionInfo> getAllPermissionsWithProtectionFlags(int protectionFlags) {
            return PermissionManagerService.this.mPermissionManagerServiceImpl.getAllPermissionsWithProtectionFlags(protectionFlags);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int[] getAllUserIds() {
        return UserManagerService.getInstance().getUserIdsIncludingPreCreated();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ShellDelegate implements CheckPermissionDelegate {
        private final String mDelegatedPackageName;
        private final List<String> mDelegatedPermissionNames;
        private final int mDelegatedUid;

        public ShellDelegate(int delegatedUid, String delegatedPackageName, List<String> delegatedPermissionNames) {
            this.mDelegatedUid = delegatedUid;
            this.mDelegatedPackageName = delegatedPackageName;
            this.mDelegatedPermissionNames = delegatedPermissionNames;
        }

        @Override // com.android.server.pm.permission.PermissionManagerService.CheckPermissionDelegate
        public int getDelegatedUid() {
            return this.mDelegatedUid;
        }

        @Override // com.android.server.pm.permission.PermissionManagerService.CheckPermissionDelegate
        public int checkPermission(String packageName, String permissionName, int userId, TriFunction<String, String, Integer, Integer> superImpl) {
            if (this.mDelegatedPackageName.equals(packageName) && isDelegatedPermission(permissionName)) {
                long identity = Binder.clearCallingIdentity();
                try {
                    return ((Integer) superImpl.apply(VibratorManagerService.VibratorManagerShellCommand.SHELL_PACKAGE_NAME, permissionName, Integer.valueOf(userId))).intValue();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return ((Integer) superImpl.apply(packageName, permissionName, Integer.valueOf(userId))).intValue();
        }

        @Override // com.android.server.pm.permission.PermissionManagerService.CheckPermissionDelegate
        public int checkUidPermission(int uid, String permissionName, BiFunction<Integer, String, Integer> superImpl) {
            if (uid == this.mDelegatedUid && isDelegatedPermission(permissionName)) {
                long identity = Binder.clearCallingIdentity();
                try {
                    return superImpl.apply(2000, permissionName).intValue();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return superImpl.apply(Integer.valueOf(uid), permissionName).intValue();
        }

        @Override // com.android.server.pm.permission.PermissionManagerService.CheckPermissionDelegate
        public List<String> getDelegatedPermissionNames() {
            if (this.mDelegatedPermissionNames == null) {
                return null;
            }
            return new ArrayList(this.mDelegatedPermissionNames);
        }

        private boolean isDelegatedPermission(String permissionName) {
            List<String> list = this.mDelegatedPermissionNames;
            return list == null || list.contains(permissionName);
        }
    }

    /* loaded from: classes2.dex */
    private static final class AttributionSourceRegistry {
        private final Context mContext;
        private final Object mLock = new Object();
        private final WeakHashMap<IBinder, AttributionSource> mAttributions = new WeakHashMap<>();

        AttributionSourceRegistry(Context context) {
            this.mContext = context;
        }

        public void registerAttributionSource(AttributionSource source) {
            int callingUid = Binder.getCallingUid();
            if (source.getUid() != callingUid && this.mContext.checkPermission("android.permission.UPDATE_APP_OPS_STATS", -1, callingUid) != 0) {
                throw new SecurityException("Cannot register attribution source for uid:" + source.getUid() + " from uid:" + callingUid);
            }
            PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            int userId = UserHandle.getUserId(callingUid == 1000 ? source.getUid() : callingUid);
            if (packageManagerInternal.getPackageUid(source.getPackageName(), 0L, userId) != source.getUid()) {
                throw new SecurityException("Cannot register attribution source for package:" + source.getPackageName() + " from uid:" + callingUid);
            }
            AttributionSource next = source.getNext();
            if (next != null && next.getNext() != null && !isRegisteredAttributionSource(next)) {
                throw new SecurityException("Cannot register forged attribution source:" + source);
            }
            synchronized (this.mLock) {
                this.mAttributions.put(source.getToken(), source);
            }
        }

        public boolean isRegisteredAttributionSource(AttributionSource source) {
            synchronized (this.mLock) {
                AttributionSource cachedSource = this.mAttributions.get(source.getToken());
                if (cachedSource != null) {
                    return cachedSource.equals(source);
                }
                return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class PermissionCheckerService extends IPermissionChecker.Stub {
        private final AppOpsManager mAppOpsManager;
        private final Context mContext;
        private final PermissionManagerServiceInternal mPermissionManagerServiceInternal = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
        private static final ConcurrentHashMap<String, PermissionInfo> sPlatformPermissions = new ConcurrentHashMap<>();
        private static final AtomicInteger sAttributionChainIds = new AtomicInteger(0);

        PermissionCheckerService(Context context) {
            this.mContext = context;
            this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        }

        public int checkPermission(String permission, AttributionSourceState attributionSourceState, String message, boolean forDataDelivery, boolean startDataDelivery, boolean fromDatasource, int attributedOp) {
            Objects.requireNonNull(permission);
            Objects.requireNonNull(attributionSourceState);
            AttributionSource attributionSource = new AttributionSource(attributionSourceState);
            int result = checkPermission(this.mContext, this.mPermissionManagerServiceInternal, permission, attributionSource, message, forDataDelivery, startDataDelivery, fromDatasource, attributedOp);
            if (startDataDelivery && result != 0 && result != 1) {
                if (attributedOp == -1) {
                    finishDataDelivery(AppOpsManager.permissionToOpCode(permission), attributionSource.asState(), fromDatasource);
                } else {
                    finishDataDelivery(attributedOp, attributionSource.asState(), fromDatasource);
                }
            }
            return result;
        }

        public void finishDataDelivery(int op, AttributionSourceState attributionSourceState, boolean fromDataSource) {
            finishDataDelivery(this.mContext, op, attributionSourceState, fromDataSource);
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* JADX WARN: Code restructure failed: missing block: B:54:0x00ad, code lost:
            if (r12 == null) goto L46;
         */
        /* JADX WARN: Code restructure failed: missing block: B:55:0x00af, code lost:
            r3 = (com.android.server.pm.permission.PermissionManagerService.RegisteredAttribution) com.android.server.pm.permission.PermissionManagerService.sRunningAttributionSources.remove(r12.getToken());
         */
        /* JADX WARN: Code restructure failed: missing block: B:56:0x00be, code lost:
            if (r3 == null) goto L45;
         */
        /* JADX WARN: Code restructure failed: missing block: B:57:0x00c0, code lost:
            r3.unregister();
         */
        /* JADX WARN: Code restructure failed: missing block: B:58:0x00c3, code lost:
            return;
         */
        /* JADX WARN: Code restructure failed: missing block: B:64:?, code lost:
            return;
         */
        /* JADX WARN: Code restructure failed: missing block: B:65:?, code lost:
            return;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public static void finishDataDelivery(Context context, int op, AttributionSourceState attributionSourceState, boolean fromDatasource) {
            Objects.requireNonNull(attributionSourceState);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
            if (op == -1) {
                return;
            }
            AttributionSource current = new AttributionSource(attributionSourceState);
            AttributionSource next = null;
            AttributionSource current2 = current;
            while (true) {
                boolean current3 = false;
                boolean skipCurrentFinish = fromDatasource || next != null;
                AttributionSource next2 = current2.getNext();
                if ((!fromDatasource || current2.asState() != attributionSourceState) && next2 != null && !current2.isTrusted(context)) {
                    return;
                }
                boolean singleReceiverFromDatasource = fromDatasource && current2.asState() == attributionSourceState && next2 != null && next2.getNext() == null;
                if (singleReceiverFromDatasource || next2 == null) {
                    current3 = true;
                }
                boolean selfAccess = current3;
                AttributionSource accessorSource = !singleReceiverFromDatasource ? current2 : next2;
                if (selfAccess) {
                    String resolvedPackageName = resolvePackageName(context, accessorSource);
                    if (resolvedPackageName == null) {
                        return;
                    }
                    appOpsManager.finishOp(attributionSourceState.token, op, accessorSource.getUid(), resolvedPackageName, accessorSource.getAttributionTag());
                } else {
                    AttributionSource resolvedAttributionSource = resolveAttributionSource(context, accessorSource);
                    if (resolvedAttributionSource.getPackageName() == null) {
                        return;
                    }
                    appOpsManager.finishProxyOp(attributionSourceState.token, AppOpsManager.opToPublicName(op), resolvedAttributionSource, skipCurrentFinish);
                }
                RegisteredAttribution registered = (RegisteredAttribution) PermissionManagerService.sRunningAttributionSources.remove(current2.getToken());
                if (registered != null) {
                    registered.unregister();
                }
                if (next2 == null || next2.getNext() == null) {
                    break;
                }
                current2 = next2;
                next = next2;
            }
        }

        public int checkOp(int op, AttributionSourceState attributionSource, String message, boolean forDataDelivery, boolean startDataDelivery) {
            int result = checkOp(this.mContext, op, this.mPermissionManagerServiceInternal, new AttributionSource(attributionSource), message, forDataDelivery, startDataDelivery);
            if (result != 0 && startDataDelivery) {
                finishDataDelivery(op, attributionSource, false);
            }
            return result;
        }

        private static int checkPermission(Context context, PermissionManagerServiceInternal permissionManagerServiceInt, String permission, AttributionSource attributionSource, String message, boolean forDataDelivery, boolean startDataDelivery, boolean fromDatasource, int attributedOp) {
            PermissionInfo permissionInfo;
            ConcurrentHashMap<String, PermissionInfo> concurrentHashMap = sPlatformPermissions;
            PermissionInfo permissionInfo2 = concurrentHashMap.get(permission);
            if (permissionInfo2 != null) {
                permissionInfo = permissionInfo2;
            } else {
                try {
                    PermissionInfo permissionInfo3 = context.getPackageManager().getPermissionInfo(permission, 0);
                    if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(permissionInfo3.packageName)) {
                        concurrentHashMap.put(permission, permissionInfo3);
                    }
                    permissionInfo = permissionInfo3;
                } catch (PackageManager.NameNotFoundException e) {
                    return 2;
                }
            }
            if (permissionInfo.isAppOp()) {
                return checkAppOpPermission(context, permissionManagerServiceInt, permission, attributionSource, message, forDataDelivery, fromDatasource);
            }
            if (permissionInfo.isRuntime()) {
                return checkRuntimePermission(context, permissionManagerServiceInt, permission, attributionSource, message, forDataDelivery, startDataDelivery, fromDatasource, attributedOp);
            }
            if (!fromDatasource && !checkPermission(context, permissionManagerServiceInt, permission, attributionSource.getUid(), attributionSource.getRenouncedPermissions())) {
                return 2;
            }
            if (attributionSource.getNext() != null) {
                return checkPermission(context, permissionManagerServiceInt, permission, attributionSource.getNext(), message, forDataDelivery, startDataDelivery, false, attributedOp);
            }
            return 0;
        }

        /* JADX WARN: Code restructure failed: missing block: B:54:0x00dd, code lost:
            return 0;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private static int checkAppOpPermission(Context context, PermissionManagerServiceInternal permissionManagerServiceInt, String permission, AttributionSource attributionSource, String message, boolean forDataDelivery, boolean fromDatasource) {
            String str;
            Object obj = attributionSource;
            int op = AppOpsManager.permissionToOpCode(permission);
            if (op < 0) {
                Slog.wtf(PermissionManagerService.LOG_TAG, "Appop permission " + permission + " with no app op defined!");
                return 2;
            }
            AttributionSource next = null;
            AttributionSource current = attributionSource;
            while (true) {
                boolean skipCurrentChecks = fromDatasource || next != null;
                AttributionSource next2 = current.getNext();
                if ((!fromDatasource || !current.equals(obj)) && next2 != null && !current.isTrusted(context)) {
                    return 2;
                }
                boolean singleReceiverFromDatasource = fromDatasource && current.equals(obj) && next2 != null && next2.getNext() == null;
                boolean selfAccess = singleReceiverFromDatasource || next2 == null;
                int opMode = performOpTransaction(context, attributionSource.getToken(), op, current, message, forDataDelivery, false, skipCurrentChecks, selfAccess, singleReceiverFromDatasource, -1, 0, 0, -1);
                switch (opMode) {
                    case 1:
                    case 2:
                        return 2;
                    case 3:
                        if (!skipCurrentChecks) {
                            str = permission;
                            if (!checkPermission(context, permissionManagerServiceInt, str, attributionSource.getUid(), attributionSource.getRenouncedPermissions())) {
                                return 2;
                            }
                        } else {
                            str = permission;
                        }
                        if (next2 != null && !checkPermission(context, permissionManagerServiceInt, str, next2.getUid(), next2.getRenouncedPermissions())) {
                            return 2;
                        }
                        break;
                    default:
                        str = permission;
                        break;
                }
                if (next2 != null && next2.getNext() != null) {
                    current = next2;
                    obj = attributionSource;
                    next = next2;
                }
            }
        }

        private static int checkRuntimePermission(Context context, PermissionManagerServiceInternal permissionManagerServiceInt, String permission, AttributionSource attributionSource, String message, boolean forDataDelivery, boolean startDataDelivery, boolean fromDatasource, int attributedOp) {
            int proxyAttributionFlags;
            AttributionSource next;
            int i;
            boolean z;
            int op;
            PermissionManagerServiceInternal permissionManagerServiceInternal = permissionManagerServiceInt;
            String str = permission;
            AttributionSource attributionSource2 = attributionSource;
            boolean z2 = fromDatasource;
            int op2 = AppOpsManager.permissionToOpCode(permission);
            int attributionChainId = getAttributionChainId(startDataDelivery, attributionSource2);
            boolean hasChain = attributionChainId != -1;
            AttributionSource next2 = null;
            boolean isChainStartTrusted = !hasChain || checkPermission(context, permissionManagerServiceInternal, "android.permission.UPDATE_APP_OPS_STATS", attributionSource.getUid(), attributionSource.getRenouncedPermissions());
            AttributionSource current = attributionSource;
            while (true) {
                boolean skipCurrentChecks = z2 || next2 != null;
                AttributionSource next3 = current.getNext();
                if ((!z2 || !current.equals(attributionSource2)) && next3 != null && !current.isTrusted(context)) {
                    return 2;
                }
                if (!skipCurrentChecks && !checkPermission(context, permissionManagerServiceInternal, str, current.getUid(), current.getRenouncedPermissions())) {
                    return 2;
                }
                if (next3 != null && !checkPermission(context, permissionManagerServiceInternal, str, next3.getUid(), next3.getRenouncedPermissions())) {
                    return 2;
                }
                if (op2 < 0) {
                    if (sPlatformPermissions.containsKey(str) && !"android.permission.ACCESS_BACKGROUND_LOCATION".equals(str) && !"android.permission.BODY_SENSORS_BACKGROUND".equals(str)) {
                        Slog.wtf(PermissionManagerService.LOG_TAG, "Platform runtime permission " + str + " with no app op defined!");
                    }
                    if (next3 == null) {
                        return 0;
                    }
                    current = next3;
                    next2 = next3;
                } else {
                    boolean singleReceiverFromDatasource = z2 && current.equals(attributionSource2) && next3 != null && next3.getNext() == null;
                    boolean selfAccess = singleReceiverFromDatasource || next3 == null;
                    boolean isLinkTrusted = isChainStartTrusted && (current.isTrusted(context) || current.equals(attributionSource2)) && (next3 == null || next3.isTrusted(context));
                    if (!skipCurrentChecks && hasChain) {
                        proxyAttributionFlags = resolveProxyAttributionFlags(attributionSource, current, fromDatasource, startDataDelivery, selfAccess, isLinkTrusted);
                    } else {
                        proxyAttributionFlags = 0;
                    }
                    if (hasChain) {
                        next = next3;
                        i = resolveProxiedAttributionFlags(attributionSource, next3, fromDatasource, startDataDelivery, selfAccess, isLinkTrusted);
                    } else {
                        next = next3;
                        i = 0;
                    }
                    AttributionSource current2 = current;
                    int proxiedAttributionFlags = i;
                    int attributionChainId2 = attributionChainId;
                    int op3 = op2;
                    int opMode = performOpTransaction(context, attributionSource.getToken(), op2, current2, message, forDataDelivery, startDataDelivery, skipCurrentChecks, selfAccess, singleReceiverFromDatasource, attributedOp, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId2);
                    switch (opMode) {
                        case 1:
                            return 1;
                        case 2:
                            return 2;
                        default:
                            if (startDataDelivery) {
                                z = fromDatasource;
                                op = op3;
                                RegisteredAttribution registered = new RegisteredAttribution(context, op, current2, z);
                                PermissionManagerService.sRunningAttributionSources.put(current2.getToken(), registered);
                            } else {
                                z = fromDatasource;
                                op = op3;
                            }
                            if (next != null && next.getNext() != null) {
                                current = next;
                                permissionManagerServiceInternal = permissionManagerServiceInt;
                                str = permission;
                                attributionSource2 = attributionSource;
                                z2 = z;
                                op2 = op;
                                next2 = next;
                                attributionChainId = attributionChainId2;
                                break;
                            }
                            break;
                    }
                }
            }
            return 0;
        }

        private static boolean checkPermission(Context context, PermissionManagerServiceInternal permissionManagerServiceInt, String permission, int uid, Set<String> renouncedPermissions) {
            boolean z = true;
            boolean permissionGranted = context.checkPermission(permission, -1, uid) == 0;
            if (!permissionGranted && Process.isIsolated(uid) && (permission.equals("android.permission.RECORD_AUDIO") || permission.equals("android.permission.CAPTURE_AUDIO_HOTWORD") || permission.equals("android.permission.CAPTURE_AUDIO_OUTPUT"))) {
                PermissionManagerServiceInternal.HotwordDetectionServiceProvider hotwordServiceProvider = permissionManagerServiceInt.getHotwordDetectionServiceProvider();
                if (hotwordServiceProvider == null || uid != hotwordServiceProvider.getUid()) {
                    z = false;
                }
                permissionGranted = z;
            }
            if (permissionGranted && renouncedPermissions.contains(permission) && context.checkPermission("android.permission.RENOUNCE_PERMISSIONS", -1, uid) == 0) {
                return false;
            }
            return permissionGranted;
        }

        private static int resolveProxyAttributionFlags(AttributionSource attributionChain, AttributionSource current, boolean fromDatasource, boolean startDataDelivery, boolean selfAccess, boolean isTrusted) {
            return resolveAttributionFlags(attributionChain, current, fromDatasource, startDataDelivery, selfAccess, isTrusted, true);
        }

        private static int resolveProxiedAttributionFlags(AttributionSource attributionChain, AttributionSource current, boolean fromDatasource, boolean startDataDelivery, boolean selfAccess, boolean isTrusted) {
            return resolveAttributionFlags(attributionChain, current, fromDatasource, startDataDelivery, selfAccess, isTrusted, false);
        }

        private static int resolveAttributionFlags(AttributionSource attributionChain, AttributionSource current, boolean fromDatasource, boolean startDataDelivery, boolean selfAccess, boolean isTrusted, boolean flagsForProxy) {
            int trustedFlag;
            if (current == null || !startDataDelivery) {
                return 0;
            }
            if (!isTrusted) {
                trustedFlag = 0;
            } else {
                trustedFlag = 8;
            }
            if (flagsForProxy) {
                if (selfAccess) {
                    return trustedFlag | 1;
                }
                if (!fromDatasource && current.equals(attributionChain)) {
                    return trustedFlag | 1;
                }
            } else if (selfAccess) {
                return trustedFlag | 4;
            } else {
                if (fromDatasource && current.equals(attributionChain.getNext())) {
                    return trustedFlag | 1;
                }
                if (current.getNext() == null) {
                    return trustedFlag | 4;
                }
            }
            if (fromDatasource && current.equals(attributionChain)) {
                return 0;
            }
            return trustedFlag | 2;
        }

        /* JADX WARN: Code restructure failed: missing block: B:58:0x00ea, code lost:
            return 0;
         */
        /* JADX WARN: Removed duplicated region for block: B:21:0x0049  */
        /* JADX WARN: Removed duplicated region for block: B:22:0x004c  */
        /* JADX WARN: Removed duplicated region for block: B:29:0x005f  */
        /* JADX WARN: Removed duplicated region for block: B:30:0x0062  */
        /* JADX WARN: Removed duplicated region for block: B:47:0x0096  */
        /* JADX WARN: Removed duplicated region for block: B:48:0x00a6  */
        /* JADX WARN: Removed duplicated region for block: B:51:0x00d1  */
        /* JADX WARN: Removed duplicated region for block: B:60:0x00da A[SYNTHETIC] */
        /* JADX WARN: Removed duplicated region for block: B:61:0x00db A[SYNTHETIC] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private static int checkOp(Context context, int op, PermissionManagerServiceInternal permissionManagerServiceInt, AttributionSource attributionSource, String message, boolean forDataDelivery, boolean startDataDelivery) {
            boolean z;
            boolean isChainStartTrusted;
            AttributionSource current;
            boolean skipCurrentChecks;
            AttributionSource next;
            int proxyAttributionFlags;
            int opMode;
            AttributionSource attributionSource2 = attributionSource;
            if (op < 0 || attributionSource.getPackageName() == null) {
                return 2;
            }
            int attributionChainId = getAttributionChainId(startDataDelivery, attributionSource2);
            boolean hasChain = attributionChainId != -1;
            AttributionSource next2 = null;
            if (hasChain && !checkPermission(context, permissionManagerServiceInt, "android.permission.UPDATE_APP_OPS_STATS", attributionSource.getUid(), attributionSource.getRenouncedPermissions())) {
                z = false;
                isChainStartTrusted = z;
                current = attributionSource;
                while (true) {
                    skipCurrentChecks = next2 == null;
                    next = current.getNext();
                    if (next != null || current.isTrusted(context)) {
                        boolean selfAccess = next != null;
                        boolean isLinkTrusted = !isChainStartTrusted && (current.isTrusted(context) || current.equals(attributionSource2)) && (next == null || next.isTrusted(context));
                        if (skipCurrentChecks && hasChain) {
                            proxyAttributionFlags = resolveProxyAttributionFlags(attributionSource, current, false, startDataDelivery, selfAccess, isLinkTrusted);
                        } else {
                            proxyAttributionFlags = 0;
                        }
                        int proxiedAttributionFlags = !hasChain ? resolveProxiedAttributionFlags(attributionSource, next, false, startDataDelivery, selfAccess, isLinkTrusted) : 0;
                        int attributionChainId2 = attributionChainId;
                        opMode = performOpTransaction(context, current.getToken(), op, current, message, forDataDelivery, startDataDelivery, skipCurrentChecks, selfAccess, false, -1, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId2);
                        switch (opMode) {
                            case 1:
                                return 1;
                            case 2:
                                return 2;
                            default:
                                if (next != null && next.getNext() != null) {
                                    current = next;
                                    attributionSource2 = attributionSource;
                                    next2 = next;
                                    attributionChainId = attributionChainId2;
                                }
                                break;
                        }
                    } else {
                        return 2;
                    }
                }
            }
            z = true;
            isChainStartTrusted = z;
            current = attributionSource;
            while (true) {
                skipCurrentChecks = next2 == null;
                next = current.getNext();
                if (next != null) {
                }
                if (next != null) {
                }
                if (isChainStartTrusted) {
                }
                if (skipCurrentChecks) {
                }
                proxyAttributionFlags = 0;
                if (!hasChain) {
                }
                int attributionChainId22 = attributionChainId;
                opMode = performOpTransaction(context, current.getToken(), op, current, message, forDataDelivery, startDataDelivery, skipCurrentChecks, selfAccess, false, -1, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId22);
                switch (opMode) {
                }
                current = next;
                attributionSource2 = attributionSource;
                next2 = next;
                attributionChainId = attributionChainId22;
            }
        }

        /* JADX WARN: Removed duplicated region for block: B:74:0x026a  */
        /* JADX WARN: Removed duplicated region for block: B:86:0x0202 A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        private static int performOpTransaction(Context context, IBinder chainStartToken, int op, AttributionSource attributionSource, String message, boolean forDataDelivery, boolean startDataDelivery, boolean skipProxyOperation, boolean selfAccess, boolean singleReceiverFromDatasource, int attributedOp, int proxyAttributionFlags, int proxiedAttributionFlags, int attributionChainId) {
            AppOpsManager appOpsManager;
            int notedOp;
            String str;
            String str2;
            String str3;
            String str4;
            int checkedOpResult;
            int notedOpResult;
            AttributionSource accessorSource;
            int startedOp;
            int checkedOpResult2;
            int startedOpResult;
            AppOpsManager appOpsManager2 = (AppOpsManager) context.getSystemService(AppOpsManager.class);
            AttributionSource accessorSource2 = !singleReceiverFromDatasource ? attributionSource : attributionSource.getNext();
            if (!forDataDelivery) {
                String resolvedAccessorPackageName = resolvePackageName(context, accessorSource2);
                if (resolvedAccessorPackageName == null) {
                    return 2;
                }
                int opMode = appOpsManager2.unsafeCheckOpRawNoThrow(op, accessorSource2.getUid(), resolvedAccessorPackageName);
                AttributionSource next = accessorSource2.getNext();
                if (!selfAccess && opMode == 0 && next != null) {
                    String resolvedNextPackageName = resolvePackageName(context, next);
                    if (resolvedNextPackageName == null) {
                        return 2;
                    }
                    return appOpsManager2.unsafeCheckOpRawNoThrow(op, next.getUid(), resolvedNextPackageName);
                }
                return opMode;
            } else if (!startDataDelivery) {
                AttributionSource resolvedAttributionSource = resolveAttributionSource(context, accessorSource2);
                if (resolvedAttributionSource.getPackageName() == null) {
                    return 2;
                }
                int checkedOpResult3 = 0;
                if (attributedOp != -1 && attributedOp != op) {
                    appOpsManager = appOpsManager2;
                    checkedOpResult3 = appOpsManager.checkOpNoThrow(op, resolvedAttributionSource.getUid(), resolvedAttributionSource.getPackageName());
                    if (checkedOpResult3 == 2) {
                        return checkedOpResult3;
                    }
                    notedOp = attributedOp;
                    if (!selfAccess) {
                        try {
                            str = "Datasource ";
                            str2 = " protecting data with platform defined runtime permission ";
                            str3 = " while not having ";
                            str4 = "android.permission.UPDATE_APP_OPS_STATS";
                            checkedOpResult = checkedOpResult3;
                            try {
                                notedOpResult = appOpsManager.noteOpNoThrow(notedOp, resolvedAttributionSource.getUid(), resolvedAttributionSource.getPackageName(), resolvedAttributionSource.getAttributionTag(), message);
                            } catch (SecurityException e) {
                                Slog.w(PermissionManagerService.LOG_TAG, str + attributionSource + str2 + AppOpsManager.opToPermission(op) + str3 + str4);
                                notedOpResult = appOpsManager.noteProxyOpNoThrow(notedOp, attributionSource, message, skipProxyOperation);
                                checkedOpResult3 = checkedOpResult;
                                return Math.max(checkedOpResult3, notedOpResult);
                            }
                        } catch (SecurityException e2) {
                            str = "Datasource ";
                            str2 = " protecting data with platform defined runtime permission ";
                            str3 = " while not having ";
                            str4 = "android.permission.UPDATE_APP_OPS_STATS";
                            checkedOpResult = checkedOpResult3;
                        }
                        checkedOpResult3 = checkedOpResult;
                    } else {
                        try {
                            notedOpResult = appOpsManager.noteProxyOpNoThrow(notedOp, resolvedAttributionSource, message, skipProxyOperation);
                        } catch (SecurityException e3) {
                            String msg = "Security exception for op " + notedOp + " with source " + attributionSource.getUid() + ":" + attributionSource.getPackageName() + ", " + attributionSource.getNextUid() + ":" + attributionSource.getNextPackageName();
                            if (attributionSource.getNext() != null) {
                                AttributionSource next2 = attributionSource.getNext();
                                msg = msg + ", " + next2.getNextPackageName() + ":" + next2.getNextUid();
                            }
                            throw new SecurityException(msg + ":" + e3.getMessage());
                        }
                    }
                    return Math.max(checkedOpResult3, notedOpResult);
                }
                appOpsManager = appOpsManager2;
                notedOp = op;
                if (!selfAccess) {
                }
                return Math.max(checkedOpResult3, notedOpResult);
            } else {
                AttributionSource resolvedAttributionSource2 = resolveAttributionSource(context, accessorSource2);
                if (resolvedAttributionSource2.getPackageName() == null) {
                    return 2;
                }
                if (attributedOp == -1 || attributedOp == op) {
                    accessorSource = accessorSource2;
                    startedOp = op;
                    checkedOpResult2 = 0;
                } else {
                    accessorSource = accessorSource2;
                    int checkedOpResult4 = appOpsManager2.checkOpNoThrow(op, resolvedAttributionSource2.getUid(), resolvedAttributionSource2.getPackageName());
                    if (checkedOpResult4 == 2) {
                        return checkedOpResult4;
                    }
                    checkedOpResult2 = checkedOpResult4;
                    startedOp = attributedOp;
                }
                if (selfAccess) {
                    try {
                        startedOpResult = appOpsManager2.startOpNoThrow(chainStartToken, startedOp, resolvedAttributionSource2.getUid(), resolvedAttributionSource2.getPackageName(), false, resolvedAttributionSource2.getAttributionTag(), message, proxyAttributionFlags, attributionChainId);
                    } catch (SecurityException e4) {
                        Slog.w(PermissionManagerService.LOG_TAG, "Datasource " + attributionSource + " protecting data with platform defined runtime permission " + AppOpsManager.opToPermission(op) + " while not having android.permission.UPDATE_APP_OPS_STATS");
                        startedOpResult = appOpsManager2.startProxyOpNoThrow(chainStartToken, attributedOp, attributionSource, message, skipProxyOperation, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId);
                    }
                } else {
                    int startedOp2 = startedOp;
                    try {
                        startedOpResult = appOpsManager2.startProxyOpNoThrow(chainStartToken, startedOp2, resolvedAttributionSource2, message, skipProxyOperation, proxyAttributionFlags, proxiedAttributionFlags, attributionChainId);
                    } catch (SecurityException e5) {
                        String msg2 = "Security exception for op " + startedOp2 + " with source " + attributionSource.getUid() + ":" + attributionSource.getPackageName() + ", " + attributionSource.getNextUid() + ":" + attributionSource.getNextPackageName();
                        if (attributionSource.getNext() != null) {
                            AttributionSource next3 = attributionSource.getNext();
                            msg2 = msg2 + ", " + next3.getNextPackageName() + ":" + next3.getNextUid();
                        }
                        throw new SecurityException(msg2 + ":" + e5.getMessage());
                    }
                }
                return Math.max(checkedOpResult2, startedOpResult);
            }
        }

        private static int getAttributionChainId(boolean startDataDelivery, AttributionSource source) {
            if (source == null || source.getNext() == null || !startDataDelivery) {
                return -1;
            }
            AtomicInteger atomicInteger = sAttributionChainIds;
            int attributionChainId = atomicInteger.incrementAndGet();
            if (attributionChainId < 0) {
                atomicInteger.set(0);
                return 0;
            }
            return attributionChainId;
        }

        private static String resolvePackageName(Context context, AttributionSource attributionSource) {
            if (attributionSource.getPackageName() != null) {
                return attributionSource.getPackageName();
            }
            String[] packageNames = context.getPackageManager().getPackagesForUid(attributionSource.getUid());
            if (packageNames != null) {
                return packageNames[0];
            }
            return AppOpsManager.resolvePackageName(attributionSource.getUid(), attributionSource.getPackageName());
        }

        private static AttributionSource resolveAttributionSource(Context context, AttributionSource attributionSource) {
            if (attributionSource.getPackageName() != null) {
                return attributionSource;
            }
            return attributionSource.withPackageName(resolvePackageName(context, attributionSource));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class RegisteredAttribution {
        private final IBinder.DeathRecipient mDeathRecipient;
        private final AtomicBoolean mFinished = new AtomicBoolean(false);
        private final IBinder mToken;

        RegisteredAttribution(final Context context, final int op, final AttributionSource source, final boolean fromDatasource) {
            IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.pm.permission.PermissionManagerService$RegisteredAttribution$$ExternalSyntheticLambda0
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    PermissionManagerService.RegisteredAttribution.this.m5830xd71119a9(context, op, source, fromDatasource);
                }
            };
            this.mDeathRecipient = deathRecipient;
            IBinder token = source.getToken();
            this.mToken = token;
            if (token != null) {
                try {
                    token.linkToDeath(deathRecipient, 0);
                } catch (RemoteException e) {
                    this.mDeathRecipient.binderDied();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-pm-permission-PermissionManagerService$RegisteredAttribution  reason: not valid java name */
        public /* synthetic */ void m5830xd71119a9(Context context, int op, AttributionSource source, boolean fromDatasource) {
            if (unregister()) {
                PermissionCheckerService.finishDataDelivery(context, op, source.asState(), fromDatasource);
            }
        }

        public boolean unregister() {
            if (this.mFinished.compareAndSet(false, true)) {
                try {
                    IBinder iBinder = this.mToken;
                    if (iBinder != null) {
                        iBinder.unlinkToDeath(this.mDeathRecipient, 0);
                    }
                } catch (NoSuchElementException e) {
                }
                return true;
            }
            return false;
        }
    }
}
