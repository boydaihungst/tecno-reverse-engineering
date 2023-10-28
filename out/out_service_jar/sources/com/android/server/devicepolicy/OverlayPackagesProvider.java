package com.android.server.devicepolicy;

import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.view.inputmethod.InputMethodInfo;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.Preconditions;
import com.android.server.devicepolicy.OverlayPackagesProvider;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.pm.ApexManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes.dex */
public class OverlayPackagesProvider {
    protected static final String TAG = "OverlayPackagesProvider";
    private static final Map<String, String> sActionToMetadataKeyMap = new HashMap();
    private static final Set<String> sAllowedActions = new HashSet();
    private final Context mContext;
    private final Injector mInjector;
    private final PackageManager mPm;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Injector {
        String getActiveApexPackageNameContainingPackage(String str);

        String getDevicePolicyManagementRoleHolderPackageName(Context context);

        List<InputMethodInfo> getInputMethodListAsUser(int i);
    }

    public OverlayPackagesProvider(Context context) {
        this(context, new DefaultInjector());
    }

    /* loaded from: classes.dex */
    private static final class DefaultInjector implements Injector {
        private DefaultInjector() {
        }

        @Override // com.android.server.devicepolicy.OverlayPackagesProvider.Injector
        public List<InputMethodInfo> getInputMethodListAsUser(int userId) {
            return InputMethodManagerInternal.get().getInputMethodListAsUser(userId);
        }

        @Override // com.android.server.devicepolicy.OverlayPackagesProvider.Injector
        public String getActiveApexPackageNameContainingPackage(String packageName) {
            return ApexManager.getInstance().getActiveApexPackageNameContainingPackage(packageName);
        }

        @Override // com.android.server.devicepolicy.OverlayPackagesProvider.Injector
        public String getDevicePolicyManagementRoleHolderPackageName(final Context context) {
            return (String) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.devicepolicy.OverlayPackagesProvider$DefaultInjector$$ExternalSyntheticLambda0
                public final Object getOrThrow() {
                    return OverlayPackagesProvider.DefaultInjector.lambda$getDevicePolicyManagementRoleHolderPackageName$0(context);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ String lambda$getDevicePolicyManagementRoleHolderPackageName$0(Context context) throws Exception {
            RoleManager roleManager = (RoleManager) context.getSystemService(RoleManager.class);
            List<String> roleHolders = roleManager.getRoleHolders("android.app.role.DEVICE_POLICY_MANAGEMENT");
            if (roleHolders.isEmpty()) {
                return null;
            }
            return roleHolders.get(0);
        }
    }

    OverlayPackagesProvider(Context context, Injector injector) {
        Map<String, String> map = sActionToMetadataKeyMap;
        map.put("android.app.action.PROVISION_MANAGED_USER", "android.app.REQUIRED_APP_MANAGED_USER");
        map.put("android.app.action.PROVISION_MANAGED_PROFILE", "android.app.REQUIRED_APP_MANAGED_PROFILE");
        map.put("android.app.action.PROVISION_MANAGED_DEVICE", "android.app.REQUIRED_APP_MANAGED_DEVICE");
        Set<String> set = sAllowedActions;
        set.add("android.app.action.PROVISION_MANAGED_USER");
        set.add("android.app.action.PROVISION_MANAGED_PROFILE");
        set.add("android.app.action.PROVISION_MANAGED_DEVICE");
        this.mContext = context;
        this.mPm = (PackageManager) Preconditions.checkNotNull(context.getPackageManager());
        this.mInjector = (Injector) Preconditions.checkNotNull(injector);
    }

    public Set<String> getNonRequiredApps(ComponentName admin, int userId, String provisioningAction) {
        Objects.requireNonNull(admin);
        Preconditions.checkArgument(sAllowedActions.contains(provisioningAction));
        Set<String> nonRequiredApps = getLaunchableApps(userId);
        nonRequiredApps.removeAll(getRequiredApps(provisioningAction, admin.getPackageName()));
        nonRequiredApps.removeAll(getSystemInputMethods(userId));
        nonRequiredApps.addAll(getDisallowedApps(provisioningAction));
        nonRequiredApps.removeAll(getRequiredAppsMainlineModules(nonRequiredApps, provisioningAction));
        nonRequiredApps.removeAll(getDeviceManagerRoleHolders());
        return nonRequiredApps;
    }

    private Set<String> getDeviceManagerRoleHolders() {
        HashSet<String> result = new HashSet<>();
        String deviceManagerRoleHolderPackageName = this.mInjector.getDevicePolicyManagementRoleHolderPackageName(this.mContext);
        if (deviceManagerRoleHolderPackageName != null) {
            result.add(deviceManagerRoleHolderPackageName);
        }
        return result;
    }

    private Set<String> getRequiredAppsMainlineModules(Set<String> packageNames, String provisioningAction) {
        Set<String> result = new HashSet<>();
        for (String packageName : packageNames) {
            if (isMainlineModule(packageName) && isRequiredAppDeclaredInMetadata(packageName, provisioningAction)) {
                result.add(packageName);
            }
        }
        return result;
    }

    private boolean isRequiredAppDeclaredInMetadata(String packageName, String provisioningAction) {
        try {
            PackageInfo packageInfo = this.mPm.getPackageInfo(packageName, 128);
            String metadataKey = sActionToMetadataKeyMap.get(provisioningAction);
            return packageInfo.applicationInfo.metaData.getBoolean(metadataKey);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isMainlineModule(String packageName) {
        return isRegularMainlineModule(packageName) || isApkInApexMainlineModule(packageName);
    }

    private boolean isRegularMainlineModule(String packageName) {
        try {
            this.mPm.getModuleInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isApkInApexMainlineModule(String packageName) {
        String apexPackageName = this.mInjector.getActiveApexPackageNameContainingPackage(packageName);
        return apexPackageName != null;
    }

    private Set<String> getLaunchableApps(int userId) {
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> resolveInfos = this.mPm.queryIntentActivitiesAsUser(launcherIntent, 795136, userId);
        Set<String> apps = new ArraySet<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            apps.add(resolveInfo.activityInfo.packageName);
        }
        return apps;
    }

    private Set<String> getSystemInputMethods(int userId) {
        List<InputMethodInfo> inputMethods = this.mInjector.getInputMethodListAsUser(userId);
        Set<String> systemInputMethods = new ArraySet<>();
        for (InputMethodInfo inputMethodInfo : inputMethods) {
            ApplicationInfo applicationInfo = inputMethodInfo.getServiceInfo().applicationInfo;
            if (applicationInfo.isSystemApp()) {
                systemInputMethods.add(inputMethodInfo.getPackageName());
            }
        }
        return systemInputMethods;
    }

    private Set<String> getRequiredApps(String provisioningAction, String dpcPackageName) {
        Set<String> requiredApps = new ArraySet<>();
        requiredApps.addAll(getRequiredAppsSet(provisioningAction));
        requiredApps.addAll(getVendorRequiredAppsSet(provisioningAction));
        requiredApps.add(dpcPackageName);
        return requiredApps;
    }

    private Set<String> getDisallowedApps(String provisioningAction) {
        Set<String> disallowedApps = new ArraySet<>();
        disallowedApps.addAll(getDisallowedAppsSet(provisioningAction));
        disallowedApps.addAll(getVendorDisallowedAppsSet(provisioningAction));
        return disallowedApps;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private Set<String> getRequiredAppsSet(String provisioningAction) {
        char c;
        int resId;
        switch (provisioningAction.hashCode()) {
            case -920528692:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -514404415:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -340845101:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                resId = 17236185;
                break;
            case 1:
                resId = 17236184;
                break;
            case 2:
                resId = 17236183;
                break;
            default:
                throw new IllegalArgumentException("Provisioning type " + provisioningAction + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(resId)));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private Set<String> getDisallowedAppsSet(String provisioningAction) {
        char c;
        int resId;
        switch (provisioningAction.hashCode()) {
            case -920528692:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -514404415:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -340845101:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                resId = 17236160;
                break;
            case 1:
                resId = 17236159;
                break;
            case 2:
                resId = 17236158;
                break;
            default:
                throw new IllegalArgumentException("Provisioning type " + provisioningAction + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(resId)));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private Set<String> getVendorRequiredAppsSet(String provisioningAction) {
        char c;
        int resId;
        switch (provisioningAction.hashCode()) {
            case -920528692:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -514404415:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -340845101:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                resId = 17236206;
                break;
            case 1:
                resId = 17236205;
                break;
            case 2:
                resId = 17236204;
                break;
            default:
                throw new IllegalArgumentException("Provisioning type " + provisioningAction + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(resId)));
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private Set<String> getVendorDisallowedAppsSet(String provisioningAction) {
        char c;
        int resId;
        switch (provisioningAction.hashCode()) {
            case -920528692:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -514404415:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -340845101:
                if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                resId = 17236202;
                break;
            case 1:
                resId = 17236201;
                break;
            case 2:
                resId = 17236200;
                break;
            default:
                throw new IllegalArgumentException("Provisioning type " + provisioningAction + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(resId)));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        pw.println(TAG);
        pw.increaseIndent();
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "required_apps_managed_device", 17236183);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "required_apps_managed_user", 17236185);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "required_apps_managed_profile", 17236184);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "disallowed_apps_managed_device", 17236158);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "disallowed_apps_managed_user", 17236160);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "disallowed_apps_managed_device", 17236158);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "vendor_required_apps_managed_device", 17236204);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "vendor_required_apps_managed_user", 17236206);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "vendor_required_apps_managed_profile", 17236205);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "vendor_disallowed_apps_managed_user", 17236202);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "vendor_disallowed_apps_managed_device", 17236200);
        DevicePolicyManagerService.dumpResources(pw, this.mContext, "vendor_disallowed_apps_managed_profile", 17236201);
        pw.decreaseIndent();
    }
}
