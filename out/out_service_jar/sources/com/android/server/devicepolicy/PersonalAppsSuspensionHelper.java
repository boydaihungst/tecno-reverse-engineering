package com.android.server.devicepolicy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.IAccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.utils.Slogf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public final class PersonalAppsSuspensionHelper {
    private static final String LOG_TAG = "DevicePolicyManager";
    private static final int PACKAGE_QUERY_FLAGS = 786432;
    private final Context mContext;
    private final PackageManager mPackageManager;

    public static PersonalAppsSuspensionHelper forUser(Context context, int userId) {
        return new PersonalAppsSuspensionHelper(context.createContextAsUser(UserHandle.of(userId), 0));
    }

    private PersonalAppsSuspensionHelper(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] getPersonalAppsForSuspension() {
        List<PackageInfo> installedPackageInfos = this.mPackageManager.getInstalledPackages(PACKAGE_QUERY_FLAGS);
        Set<String> result = new ArraySet<>();
        for (PackageInfo packageInfo : installedPackageInfos) {
            ApplicationInfo info = packageInfo.applicationInfo;
            if ((!info.isSystemApp() && !info.isUpdatedSystemApp()) || hasLauncherIntent(packageInfo.packageName)) {
                result.add(packageInfo.packageName);
            }
        }
        result.removeAll(getCriticalPackages());
        result.removeAll(getSystemLauncherPackages());
        result.removeAll(getAccessibilityServices());
        result.removeAll(getInputMethodPackages());
        result.remove(Telephony.Sms.getDefaultSmsPackage(this.mContext));
        result.remove(getSettingsPackageName());
        String[] unsuspendablePackages = this.mPackageManager.getUnsuspendablePackages((String[]) result.toArray(new String[0]));
        for (String pkg : unsuspendablePackages) {
            result.remove(pkg);
        }
        if (Log.isLoggable(LOG_TAG, 4)) {
            Slogf.i(LOG_TAG, "Packages subject to suspension: %s", String.join(",", result));
        }
        return (String[]) result.toArray(new String[0]);
    }

    private List<String> getSystemLauncherPackages() {
        List<String> result = new ArrayList<>();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        List<ResolveInfo> matchingActivities = this.mPackageManager.queryIntentActivities(intent, PACKAGE_QUERY_FLAGS);
        for (ResolveInfo resolveInfo : matchingActivities) {
            if (resolveInfo.activityInfo == null || TextUtils.isEmpty(resolveInfo.activityInfo.packageName)) {
                Slogf.wtf(LOG_TAG, "Could not find package name for launcher app %s", resolveInfo);
            } else {
                String packageName = resolveInfo.activityInfo.packageName;
                try {
                    ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(packageName, PACKAGE_QUERY_FLAGS);
                    if (applicationInfo.isSystemApp() || applicationInfo.isUpdatedSystemApp()) {
                        result.add(packageName);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Slogf.e(LOG_TAG, "Could not find application info for launcher app: %s", packageName);
                }
            }
        }
        return result;
    }

    private List<String> getAccessibilityServices() {
        IBinder iBinder = ServiceManager.getService("accessibility");
        IAccessibilityManager service = iBinder == null ? null : IAccessibilityManager.Stub.asInterface(iBinder);
        Context context = this.mContext;
        AccessibilityManager am = new AccessibilityManager(context, service, context.getUserId());
        try {
            List<AccessibilityServiceInfo> accessibilityServiceInfos = am.getEnabledAccessibilityServiceList(-1);
            am.removeClient();
            List<String> result = new ArrayList<>();
            for (AccessibilityServiceInfo serviceInfo : accessibilityServiceInfos) {
                ComponentName componentName = ComponentName.unflattenFromString(serviceInfo.getId());
                if (componentName != null) {
                    result.add(componentName.getPackageName());
                }
            }
            return result;
        } catch (Throwable th) {
            am.removeClient();
            throw th;
        }
    }

    private List<String> getInputMethodPackages() {
        List<InputMethodInfo> enabledImes = InputMethodManagerInternal.get().getEnabledInputMethodListAsUser(this.mContext.getUserId());
        List<String> result = new ArrayList<>();
        for (InputMethodInfo info : enabledImes) {
            result.add(info.getPackageName());
        }
        return result;
    }

    private String getSettingsPackageName() {
        Intent intent = new Intent("android.settings.SETTINGS");
        intent.addCategory("android.intent.category.DEFAULT");
        ResolveInfo resolveInfo = this.mPackageManager.resolveActivity(intent, PACKAGE_QUERY_FLAGS);
        if (resolveInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        return null;
    }

    private List<String> getCriticalPackages() {
        return Arrays.asList(this.mContext.getResources().getStringArray(17236107));
    }

    private boolean hasLauncherIntent(String packageName) {
        Intent intentToResolve = new Intent("android.intent.action.MAIN");
        intentToResolve.addCategory("android.intent.category.LAUNCHER");
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> resolveInfos = this.mPackageManager.queryIntentActivities(intentToResolve, PACKAGE_QUERY_FLAGS);
        return (resolveInfos == null || resolveInfos.isEmpty()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        pw.println("PersonalAppsSuspensionHelper");
        pw.increaseIndent();
        DevicePolicyManagerService.dumpApps(pw, "critical packages", getCriticalPackages());
        DevicePolicyManagerService.dumpApps(pw, "launcher packages", getSystemLauncherPackages());
        DevicePolicyManagerService.dumpApps(pw, "accessibility services", getAccessibilityServices());
        DevicePolicyManagerService.dumpApps(pw, "input method packages", getInputMethodPackages());
        pw.printf("SMS package: %s\n", new Object[]{Telephony.Sms.getDefaultSmsPackage(this.mContext)});
        pw.printf("Settings package: %s\n", new Object[]{getSettingsPackageName()});
        DevicePolicyManagerService.dumpApps(pw, "Packages subject to suspension", getPersonalAppsForSuspension());
        pw.decreaseIndent();
    }
}
