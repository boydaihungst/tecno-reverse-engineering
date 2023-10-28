package com.android.server.pm.permission;

import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.PermissionChecker;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.os.Binder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.permission.ILegacyPermissionManager;
import android.util.EventLog;
import android.util.Log;
import com.android.internal.util.FunctionalUtils;
import com.android.server.LocalServices;
import com.android.server.pm.PackageManagerServiceUtils;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.LegacyPermissionManagerInternal;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class LegacyPermissionManagerService extends ILegacyPermissionManager.Stub {
    private static final String TAG = "PackageManager";
    private final Context mContext;
    private final DefaultPermissionGrantPolicy mDefaultPermissionGrantPolicy;
    private final Injector mInjector;

    public static LegacyPermissionManagerInternal create(Context context) {
        LegacyPermissionManagerInternal legacyPermissionManagerInternal = (LegacyPermissionManagerInternal) LocalServices.getService(LegacyPermissionManagerInternal.class);
        if (legacyPermissionManagerInternal == null) {
            new LegacyPermissionManagerService(context);
            return (LegacyPermissionManagerInternal) LocalServices.getService(LegacyPermissionManagerInternal.class);
        }
        return legacyPermissionManagerInternal;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.pm.permission.LegacyPermissionManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    private LegacyPermissionManagerService(Context context) {
        this(context, new Injector(context));
        LocalServices.addService(LegacyPermissionManagerInternal.class, new Internal());
        ServiceManager.addService("legacy_permission", this);
    }

    LegacyPermissionManagerService(Context context, Injector injector) {
        this.mContext = context;
        this.mInjector = injector;
        this.mDefaultPermissionGrantPolicy = new DefaultPermissionGrantPolicy(context);
    }

    public int checkDeviceIdentifierAccess(String packageName, String message, String callingFeatureId, int pid, int uid) {
        verifyCallerCanCheckAccess(packageName, message, pid, uid);
        int appId = UserHandle.getAppId(uid);
        if (appId == 1000 || appId == 0 || this.mInjector.checkPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", pid, uid) == 0) {
            return 0;
        }
        if (packageName != null) {
            long token = this.mInjector.clearCallingIdentity();
            AppOpsManager appOpsManager = (AppOpsManager) this.mInjector.getSystemService("appops");
            try {
                if (appOpsManager.noteOpNoThrow("android:read_device_identifiers", uid, packageName, callingFeatureId, message) == 0) {
                    return 0;
                }
                this.mInjector.restoreCallingIdentity(token);
                DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.mInjector.getSystemService("device_policy");
                if (devicePolicyManager != null && devicePolicyManager.hasDeviceIdentifierAccess(packageName, pid, uid)) {
                    return 0;
                }
                return -1;
            } finally {
                this.mInjector.restoreCallingIdentity(token);
            }
        }
        return -1;
    }

    public int checkPhoneNumberAccess(String packageName, String message, String callingFeatureId, int pid, int uid) {
        boolean preR;
        verifyCallerCanCheckAccess(packageName, message, pid, uid);
        if (this.mInjector.checkPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", pid, uid) == 0) {
            return 0;
        }
        if (packageName == null) {
            return -1;
        }
        int result = -1;
        try {
            ApplicationInfo info = this.mInjector.getApplicationInfo(packageName, uid);
            boolean preR2 = info.targetSdkVersion <= 29;
            preR = preR2;
        } catch (PackageManager.NameNotFoundException e) {
            preR = false;
        }
        if (preR && (result = checkPermissionAndAppop(packageName, "android.permission.READ_PHONE_STATE", "android:read_phone_state", callingFeatureId, message, pid, uid)) == 0) {
            return result;
        }
        if (checkPermissionAndAppop(packageName, null, "android:write_sms", callingFeatureId, message, pid, uid) == 0 || checkPermissionAndAppop(packageName, "android.permission.READ_PHONE_NUMBERS", "android:read_phone_numbers", callingFeatureId, message, pid, uid) == 0 || checkPermissionAndAppop(packageName, "android.permission.READ_SMS", "android:read_sms", callingFeatureId, message, pid, uid) == 0) {
            return 0;
        }
        return result;
    }

    /* JADX WARN: Code restructure failed: missing block: B:6:0x001f, code lost:
        if (r5 != r19) goto L22;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void verifyCallerCanCheckAccess(String packageName, String message, int pid, int uid) {
        boolean reportError = false;
        int callingUid = this.mInjector.getCallingUid();
        int callingPid = this.mInjector.getCallingPid();
        if (UserHandle.getAppId(callingUid) >= 10000) {
            if (callingUid == uid) {
            }
            reportError = true;
        }
        if (packageName != null && UserHandle.getAppId(uid) >= 10000) {
            int packageUid = this.mInjector.getPackageUidForUser(packageName, UserHandle.getUserId(uid));
            if (uid != packageUid) {
                Object[] objArr = new Object[3];
                objArr[0] = "193441322";
                objArr[1] = Integer.valueOf(UserHandle.getAppId(callingUid) >= 10000 ? callingUid : uid);
                objArr[2] = "Package uid mismatch";
                EventLog.writeEvent(1397638484, objArr);
                reportError = true;
            }
        }
        if (reportError) {
            String response = String.format("Calling uid %d, pid %d cannot access for package %s (uid=%d, pid=%d): %s", Integer.valueOf(callingUid), Integer.valueOf(callingPid), packageName, Integer.valueOf(uid), Integer.valueOf(pid), message);
            Log.w(TAG, response);
            throw new SecurityException(response);
        }
    }

    private int checkPermissionAndAppop(String packageName, String permission, String appop, String callingFeatureId, String message, int pid, int uid) {
        if (permission != null && this.mInjector.checkPermission(permission, pid, uid) != 0) {
            return -1;
        }
        AppOpsManager appOpsManager = (AppOpsManager) this.mInjector.getSystemService("appops");
        if (appOpsManager.noteOpNoThrow(appop, uid, packageName, callingFeatureId, message) != 0) {
            return 1;
        }
        return 0;
    }

    public void grantDefaultPermissionsToCarrierServiceApp(final String packageName, final int userId) {
        PackageManagerServiceUtils.enforceSystemOrRoot("grantDefaultPermissionsForCarrierServiceApp");
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.permission.LegacyPermissionManagerService$$ExternalSyntheticLambda1
            public final void runOrThrow() {
                LegacyPermissionManagerService.this.m5797x58aad87d(packageName, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$grantDefaultPermissionsToCarrierServiceApp$0$com-android-server-pm-permission-LegacyPermissionManagerService  reason: not valid java name */
    public /* synthetic */ void m5797x58aad87d(String packageName, int userId) throws Exception {
        this.mDefaultPermissionGrantPolicy.grantDefaultPermissionsToCarrierServiceApp(packageName, userId);
    }

    public void grantDefaultPermissionsToActiveLuiApp(final String packageName, final int userId) {
        int callingUid = Binder.getCallingUid();
        PackageManagerServiceUtils.enforceSystemOrPhoneCaller("grantDefaultPermissionsToActiveLuiApp", callingUid);
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.permission.LegacyPermissionManagerService$$ExternalSyntheticLambda6
            public final void runOrThrow() {
                LegacyPermissionManagerService.this.m5796xf2454cbb(packageName, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$grantDefaultPermissionsToActiveLuiApp$1$com-android-server-pm-permission-LegacyPermissionManagerService  reason: not valid java name */
    public /* synthetic */ void m5796xf2454cbb(String packageName, int userId) throws Exception {
        this.mDefaultPermissionGrantPolicy.grantDefaultPermissionsToActiveLuiApp(packageName, userId);
    }

    public void revokeDefaultPermissionsFromLuiApps(final String[] packageNames, final int userId) {
        int callingUid = Binder.getCallingUid();
        PackageManagerServiceUtils.enforceSystemOrPhoneCaller("revokeDefaultPermissionsFromLuiApps", callingUid);
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.permission.LegacyPermissionManagerService$$ExternalSyntheticLambda4
            public final void runOrThrow() {
                LegacyPermissionManagerService.this.m5802xd877c2ba(packageNames, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$revokeDefaultPermissionsFromLuiApps$2$com-android-server-pm-permission-LegacyPermissionManagerService  reason: not valid java name */
    public /* synthetic */ void m5802xd877c2ba(String[] packageNames, int userId) throws Exception {
        this.mDefaultPermissionGrantPolicy.revokeDefaultPermissionsFromLuiApps(packageNames, userId);
    }

    public void grantDefaultPermissionsToEnabledImsServices(final String[] packageNames, final int userId) {
        int callingUid = Binder.getCallingUid();
        PackageManagerServiceUtils.enforceSystemOrPhoneCaller("grantDefaultPermissionsToEnabledImsServices", callingUid);
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.permission.LegacyPermissionManagerService$$ExternalSyntheticLambda3
            public final void runOrThrow() {
                LegacyPermissionManagerService.this.m5799xfbcd6062(packageNames, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$grantDefaultPermissionsToEnabledImsServices$3$com-android-server-pm-permission-LegacyPermissionManagerService  reason: not valid java name */
    public /* synthetic */ void m5799xfbcd6062(String[] packageNames, int userId) throws Exception {
        this.mDefaultPermissionGrantPolicy.grantDefaultPermissionsToEnabledImsServices(packageNames, userId);
    }

    public void grantDefaultPermissionsToEnabledTelephonyDataServices(final String[] packageNames, final int userId) {
        int callingUid = Binder.getCallingUid();
        PackageManagerServiceUtils.enforceSystemOrPhoneCaller("grantDefaultPermissionsToEnabledTelephonyDataServices", callingUid);
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.permission.LegacyPermissionManagerService$$ExternalSyntheticLambda5
            public final void runOrThrow() {
                LegacyPermissionManagerService.this.m5800x853a95b6(packageNames, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$grantDefaultPermissionsToEnabledTelephonyDataServices$4$com-android-server-pm-permission-LegacyPermissionManagerService  reason: not valid java name */
    public /* synthetic */ void m5800x853a95b6(String[] packageNames, int userId) throws Exception {
        this.mDefaultPermissionGrantPolicy.grantDefaultPermissionsToEnabledTelephonyDataServices(packageNames, userId);
    }

    public void revokeDefaultPermissionsFromDisabledTelephonyDataServices(final String[] packageNames, final int userId) {
        int callingUid = Binder.getCallingUid();
        PackageManagerServiceUtils.enforceSystemOrPhoneCaller("revokeDefaultPermissionsFromDisabledTelephonyDataServices", callingUid);
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.permission.LegacyPermissionManagerService$$ExternalSyntheticLambda0
            public final void runOrThrow() {
                LegacyPermissionManagerService.this.m5801x90ed88ef(packageNames, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$revokeDefaultPermissionsFromDisabledTelephonyDataServices$5$com-android-server-pm-permission-LegacyPermissionManagerService  reason: not valid java name */
    public /* synthetic */ void m5801x90ed88ef(String[] packageNames, int userId) throws Exception {
        this.mDefaultPermissionGrantPolicy.revokeDefaultPermissionsFromDisabledTelephonyDataServices(packageNames, userId);
    }

    public void grantDefaultPermissionsToEnabledCarrierApps(final String[] packageNames, final int userId) {
        int callingUid = Binder.getCallingUid();
        PackageManagerServiceUtils.enforceSystemOrPhoneCaller("grantPermissionsToEnabledCarrierApps", callingUid);
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.pm.permission.LegacyPermissionManagerService$$ExternalSyntheticLambda2
            public final void runOrThrow() {
                LegacyPermissionManagerService.this.m5798xcfa35962(packageNames, userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$grantDefaultPermissionsToEnabledCarrierApps$6$com-android-server-pm-permission-LegacyPermissionManagerService  reason: not valid java name */
    public /* synthetic */ void m5798xcfa35962(String[] packageNames, int userId) throws Exception {
        this.mDefaultPermissionGrantPolicy.grantDefaultPermissionsToEnabledCarrierApps(packageNames, userId);
    }

    /* loaded from: classes2.dex */
    private class Internal implements LegacyPermissionManagerInternal {
        private Internal() {
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void resetRuntimePermissions() {
            int[] userIds;
            LegacyPermissionManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS", "revokeRuntimePermission");
            int callingUid = Binder.getCallingUid();
            if (callingUid != 1000 && callingUid != 0) {
                LegacyPermissionManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "resetRuntimePermissions");
            }
            PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            final PermissionManagerServiceInternal permissionManagerInternal = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
            for (final int userId : UserManagerService.getInstance().getUserIds()) {
                packageManagerInternal.forEachPackage(new Consumer() { // from class: com.android.server.pm.permission.LegacyPermissionManagerService$Internal$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        PermissionManagerServiceInternal.this.resetRuntimePermissions((AndroidPackage) obj, userId);
                    }
                });
            }
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void setDialerAppPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.setDialerAppPackagesProvider(provider);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void setLocationExtraPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.setLocationExtraPackagesProvider(provider);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void setLocationPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.setLocationPackagesProvider(provider);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void setSimCallManagerPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.setSimCallManagerPackagesProvider(provider);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void setSmsAppPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.setSmsAppPackagesProvider(provider);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void setSyncAdapterPackagesProvider(LegacyPermissionManagerInternal.SyncAdapterPackagesProvider provider) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.setSyncAdapterPackagesProvider(provider);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void setUseOpenWifiAppPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.setUseOpenWifiAppPackagesProvider(provider);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void setVoiceInteractionPackagesProvider(LegacyPermissionManagerInternal.PackagesProvider provider) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.setVoiceInteractionPackagesProvider(provider);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void grantDefaultPermissionsToDefaultSimCallManager(String packageName, int userId) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.grantDefaultPermissionsToDefaultSimCallManager(packageName, userId);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void grantDefaultPermissionsToDefaultUseOpenWifiApp(String packageName, int userId) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.grantDefaultPermissionsToDefaultUseOpenWifiApp(packageName, userId);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void grantDefaultPermissions(int userId) {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.grantDefaultPermissions(userId);
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public void scheduleReadDefaultPermissionExceptions() {
            LegacyPermissionManagerService.this.mDefaultPermissionGrantPolicy.scheduleReadDefaultPermissionExceptions();
        }

        @Override // com.android.server.pm.permission.LegacyPermissionManagerInternal
        public int checkSoundTriggerRecordAudioPermissionForDataDelivery(int uid, String packageName, String attributionTag, String reason) {
            int result = PermissionChecker.checkPermissionForPreflight(LegacyPermissionManagerService.this.mContext, "android.permission.RECORD_AUDIO", -1, uid, packageName);
            if (result != 0) {
                return result;
            }
            ((AppOpsManager) LegacyPermissionManagerService.this.mContext.getSystemService(AppOpsManager.class)).noteOpNoThrow(102, uid, packageName, attributionTag, reason);
            return result;
        }
    }

    /* loaded from: classes2.dex */
    public static class Injector {
        private final Context mContext;
        private final PackageManagerInternal mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);

        public Injector(Context context) {
            this.mContext = context;
        }

        public int getCallingUid() {
            return Binder.getCallingUid();
        }

        public int getCallingPid() {
            return Binder.getCallingPid();
        }

        public int checkPermission(String permission, int pid, int uid) {
            return this.mContext.checkPermission(permission, pid, uid);
        }

        public long clearCallingIdentity() {
            return Binder.clearCallingIdentity();
        }

        public void restoreCallingIdentity(long token) {
            Binder.restoreCallingIdentity(token);
        }

        public Object getSystemService(String name) {
            return this.mContext.getSystemService(name);
        }

        public ApplicationInfo getApplicationInfo(String packageName, int uid) throws PackageManager.NameNotFoundException {
            return this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, 0, UserHandle.getUserHandleForUid(uid));
        }

        public int getPackageUidForUser(String packageName, int userId) {
            return this.mPackageManagerInternal.getPackageUid(packageName, 0L, userId);
        }
    }
}
