package com.android.server.compat;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.compat.PackageOverride;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.compat.AndroidBuildClassifier;
import com.android.internal.compat.ChangeReporter;
import com.android.internal.compat.CompatibilityChangeConfig;
import com.android.internal.compat.CompatibilityChangeInfo;
import com.android.internal.compat.CompatibilityOverrideConfig;
import com.android.internal.compat.CompatibilityOverridesByPackageConfig;
import com.android.internal.compat.CompatibilityOverridesToRemoveByPackageConfig;
import com.android.internal.compat.CompatibilityOverridesToRemoveConfig;
import com.android.internal.compat.IOverrideValidator;
import com.android.internal.compat.IPlatformCompat;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.compat.CompatChange;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class PlatformCompat extends IPlatformCompat.Stub {
    private static final String TAG = "Compatibility";
    private final AndroidBuildClassifier mBuildClassifier;
    private final ChangeReporter mChangeReporter = new ChangeReporter(2);
    private final CompatConfig mCompatConfig;
    private final Context mContext;

    public PlatformCompat(Context context) {
        this.mContext = context;
        AndroidBuildClassifier androidBuildClassifier = new AndroidBuildClassifier();
        this.mBuildClassifier = androidBuildClassifier;
        this.mCompatConfig = CompatConfig.create(androidBuildClassifier, context);
    }

    PlatformCompat(Context context, CompatConfig compatConfig, AndroidBuildClassifier buildClassifier) {
        this.mContext = context;
        this.mCompatConfig = compatConfig;
        this.mBuildClassifier = buildClassifier;
        registerPackageReceiver(context);
    }

    public void reportChange(long changeId, ApplicationInfo appInfo) {
        checkCompatChangeLogPermission();
        reportChangeInternal(changeId, appInfo.uid, 3);
    }

    public void reportChangeByPackageName(long changeId, String packageName, int userId) {
        checkCompatChangeLogPermission();
        ApplicationInfo appInfo = getApplicationInfo(packageName, userId);
        if (appInfo != null) {
            reportChangeInternal(changeId, appInfo.uid, 3);
        }
    }

    public void reportChangeByUid(long changeId, int uid) {
        checkCompatChangeLogPermission();
        reportChangeInternal(changeId, uid, 3);
    }

    private void reportChangeInternal(long changeId, int uid, int state) {
        this.mChangeReporter.reportChange(uid, changeId, state);
    }

    public boolean isChangeEnabled(long changeId, ApplicationInfo appInfo) {
        checkCompatChangeReadAndLogPermission();
        return isChangeEnabledInternal(changeId, appInfo);
    }

    public boolean isChangeEnabledByPackageName(long changeId, String packageName, int userId) {
        checkCompatChangeReadAndLogPermission();
        ApplicationInfo appInfo = getApplicationInfo(packageName, userId);
        if (appInfo == null) {
            return this.mCompatConfig.willChangeBeEnabled(changeId, packageName);
        }
        return isChangeEnabledInternal(changeId, appInfo);
    }

    public boolean isChangeEnabledByUid(long changeId, int uid) {
        checkCompatChangeReadAndLogPermission();
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (packages == null || packages.length == 0) {
            return this.mCompatConfig.defaultChangeIdValue(changeId);
        }
        boolean enabled = true;
        for (String packageName : packages) {
            enabled &= isChangeEnabledByPackageName(changeId, packageName, UserHandle.getUserId(uid));
        }
        return enabled;
    }

    public boolean isChangeEnabledInternalNoLogging(long changeId, ApplicationInfo appInfo) {
        return this.mCompatConfig.isChangeEnabled(changeId, appInfo);
    }

    public boolean isChangeEnabledInternal(long changeId, ApplicationInfo appInfo) {
        boolean enabled = isChangeEnabledInternalNoLogging(changeId, appInfo);
        if (appInfo != null) {
            reportChangeInternal(changeId, appInfo.uid, enabled ? 1 : 2);
        }
        return enabled;
    }

    public boolean isChangeEnabledInternal(long changeId, String packageName, int targetSdkVersion) {
        if (this.mCompatConfig.willChangeBeEnabled(changeId, packageName)) {
            ApplicationInfo appInfo = new ApplicationInfo();
            appInfo.packageName = packageName;
            appInfo.targetSdkVersion = targetSdkVersion;
            return isChangeEnabledInternalNoLogging(changeId, appInfo);
        }
        return false;
    }

    public void setOverrides(CompatibilityChangeConfig overrides, String packageName) {
        checkCompatChangeOverridePermission();
        Map<Long, PackageOverride> overridesMap = new HashMap<>();
        for (Long l : overrides.enabledChanges()) {
            long change = l.longValue();
            overridesMap.put(Long.valueOf(change), new PackageOverride.Builder().setEnabled(true).build());
        }
        for (Long l2 : overrides.disabledChanges()) {
            long change2 = l2.longValue();
            overridesMap.put(Long.valueOf(change2), new PackageOverride.Builder().setEnabled(false).build());
        }
        this.mCompatConfig.addPackageOverrides(new CompatibilityOverrideConfig(overridesMap), packageName, false);
        killPackage(packageName);
    }

    public void setOverridesForTest(CompatibilityChangeConfig overrides, String packageName) {
        checkCompatChangeOverridePermission();
        Map<Long, PackageOverride> overridesMap = new HashMap<>();
        for (Long l : overrides.enabledChanges()) {
            long change = l.longValue();
            overridesMap.put(Long.valueOf(change), new PackageOverride.Builder().setEnabled(true).build());
        }
        for (Long l2 : overrides.disabledChanges()) {
            long change2 = l2.longValue();
            overridesMap.put(Long.valueOf(change2), new PackageOverride.Builder().setEnabled(false).build());
        }
        this.mCompatConfig.addPackageOverrides(new CompatibilityOverrideConfig(overridesMap), packageName, false);
    }

    public void putAllOverridesOnReleaseBuilds(CompatibilityOverridesByPackageConfig overridesByPackage) {
        checkCompatChangeOverrideOverridablePermission();
        for (CompatibilityOverrideConfig overrides : overridesByPackage.packageNameToOverrides.values()) {
            checkAllCompatOverridesAreOverridable(overrides.overrides.keySet());
        }
        this.mCompatConfig.addAllPackageOverrides(overridesByPackage, true);
    }

    public void putOverridesOnReleaseBuilds(CompatibilityOverrideConfig overrides, String packageName) {
        checkCompatChangeOverrideOverridablePermission();
        checkAllCompatOverridesAreOverridable(overrides.overrides.keySet());
        this.mCompatConfig.addPackageOverrides(overrides, packageName, true);
    }

    public int enableTargetSdkChanges(String packageName, int targetSdkVersion) {
        checkCompatChangeOverridePermission();
        int numChanges = this.mCompatConfig.enableTargetSdkChangesForPackage(packageName, targetSdkVersion);
        killPackage(packageName);
        return numChanges;
    }

    public int disableTargetSdkChanges(String packageName, int targetSdkVersion) {
        checkCompatChangeOverridePermission();
        int numChanges = this.mCompatConfig.disableTargetSdkChangesForPackage(packageName, targetSdkVersion);
        killPackage(packageName);
        return numChanges;
    }

    public void clearOverrides(String packageName) {
        checkCompatChangeOverridePermission();
        this.mCompatConfig.removePackageOverrides(packageName);
        killPackage(packageName);
    }

    public void clearOverridesForTest(String packageName) {
        checkCompatChangeOverridePermission();
        this.mCompatConfig.removePackageOverrides(packageName);
    }

    public boolean clearOverride(long changeId, String packageName) {
        checkCompatChangeOverridePermission();
        boolean existed = this.mCompatConfig.removeOverride(changeId, packageName);
        killPackage(packageName);
        return existed;
    }

    public boolean clearOverrideForTest(long changeId, String packageName) {
        checkCompatChangeOverridePermission();
        return this.mCompatConfig.removeOverride(changeId, packageName);
    }

    public void removeAllOverridesOnReleaseBuilds(CompatibilityOverridesToRemoveByPackageConfig overridesToRemoveByPackage) {
        checkCompatChangeOverrideOverridablePermission();
        for (CompatibilityOverridesToRemoveConfig overridesToRemove : overridesToRemoveByPackage.packageNameToOverridesToRemove.values()) {
            checkAllCompatOverridesAreOverridable(overridesToRemove.changeIds);
        }
        this.mCompatConfig.removeAllPackageOverrides(overridesToRemoveByPackage);
    }

    public void removeOverridesOnReleaseBuilds(CompatibilityOverridesToRemoveConfig overridesToRemove, String packageName) {
        checkCompatChangeOverrideOverridablePermission();
        checkAllCompatOverridesAreOverridable(overridesToRemove.changeIds);
        this.mCompatConfig.removePackageOverrides(overridesToRemove, packageName);
    }

    public CompatibilityChangeConfig getAppConfig(ApplicationInfo appInfo) {
        checkCompatChangeReadAndLogPermission();
        return this.mCompatConfig.getAppConfig(appInfo);
    }

    public CompatibilityChangeInfo[] listAllChanges() {
        checkCompatChangeReadPermission();
        return this.mCompatConfig.dumpChanges();
    }

    public CompatibilityChangeInfo[] listUIChanges() {
        return (CompatibilityChangeInfo[]) Arrays.stream(listAllChanges()).filter(new Predicate() { // from class: com.android.server.compat.PlatformCompat$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isShownInUI;
                isShownInUI = PlatformCompat.this.isShownInUI((CompatibilityChangeInfo) obj);
                return isShownInUI;
            }
        }).toArray(new IntFunction() { // from class: com.android.server.compat.PlatformCompat$$ExternalSyntheticLambda1
            @Override // java.util.function.IntFunction
            public final Object apply(int i) {
                return PlatformCompat.lambda$listUIChanges$0(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ CompatibilityChangeInfo[] lambda$listUIChanges$0(int x$0) {
        return new CompatibilityChangeInfo[x$0];
    }

    public boolean isKnownChangeId(long changeId) {
        return this.mCompatConfig.isKnownChangeId(changeId);
    }

    public long[] getDisabledChanges(ApplicationInfo appInfo) {
        return this.mCompatConfig.getDisabledChanges(appInfo);
    }

    public long lookupChangeId(String name) {
        return this.mCompatConfig.lookupChangeId(name);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, "platform_compat", pw)) {
            return;
        }
        checkCompatChangeReadAndLogPermission();
        this.mCompatConfig.dumpConfig(pw);
    }

    public IOverrideValidator getOverrideValidator() {
        return this.mCompatConfig.getOverrideValidator();
    }

    public void resetReporting(ApplicationInfo appInfo) {
        this.mChangeReporter.resetReportedChanges(appInfo.uid);
    }

    private ApplicationInfo getApplicationInfo(String packageName, int userId) {
        return ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getApplicationInfo(packageName, 0L, Process.myUid(), userId);
    }

    private void killPackage(String packageName) {
        int uid = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageUid(packageName, 0L, UserHandle.myUserId());
        if (uid < 0) {
            Slog.w(TAG, "Didn't find package " + packageName + " on device.");
            return;
        }
        Slog.d(TAG, "Killing package " + packageName + " (UID " + uid + ").");
        killUid(UserHandle.getAppId(uid));
    }

    private void killUid(int appId) {
        long identity = Binder.clearCallingIdentity();
        try {
            IActivityManager am = ActivityManager.getService();
            if (am != null) {
                am.killUid(appId, -1, "PlatformCompat overrides");
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
        Binder.restoreCallingIdentity(identity);
    }

    private void checkCompatChangeLogPermission() throws SecurityException {
        if (Binder.getCallingUid() != 1000 && this.mContext.checkCallingOrSelfPermission("android.permission.LOG_COMPAT_CHANGE") != 0) {
            throw new SecurityException("Cannot log compat change usage");
        }
    }

    private void checkCompatChangeReadPermission() {
        if (Binder.getCallingUid() != 1000 && this.mContext.checkCallingOrSelfPermission("android.permission.READ_COMPAT_CHANGE_CONFIG") != 0) {
            throw new SecurityException("Cannot read compat change");
        }
    }

    private void checkCompatChangeOverridePermission() {
        if (Binder.getCallingUid() != 1000 && this.mContext.checkCallingOrSelfPermission("android.permission.OVERRIDE_COMPAT_CHANGE_CONFIG") != 0) {
            throw new SecurityException("Cannot override compat change");
        }
    }

    private void checkCompatChangeOverrideOverridablePermission() {
        if (Binder.getCallingUid() != 1000 && this.mContext.checkCallingOrSelfPermission("android.permission.OVERRIDE_COMPAT_CHANGE_CONFIG_ON_RELEASE_BUILD") != 0) {
            throw new SecurityException("Cannot override compat change");
        }
    }

    private void checkAllCompatOverridesAreOverridable(Collection<Long> changeIds) {
        for (Long changeId : changeIds) {
            if (isKnownChangeId(changeId.longValue()) && !this.mCompatConfig.isOverridable(changeId.longValue())) {
                throw new SecurityException("Only change ids marked as Overridable can be overridden.");
            }
        }
    }

    private void checkCompatChangeReadAndLogPermission() {
        checkCompatChangeReadPermission();
        checkCompatChangeLogPermission();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isShownInUI(CompatibilityChangeInfo change) {
        if (change.getLoggingOnly() || change.getId() == 149391281) {
            return false;
        }
        if (change.getEnableSinceTargetSdk() > 0) {
            return change.getEnableSinceTargetSdk() >= 29 && change.getEnableSinceTargetSdk() <= this.mBuildClassifier.platformTargetSdk();
        }
        return true;
    }

    public boolean registerListener(long changeId, CompatChange.ChangeListener listener) {
        return this.mCompatConfig.registerListener(changeId, listener);
    }

    public void registerPackageReceiver(Context context) {
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.android.server.compat.PlatformCompat.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                Uri packageData;
                String packageName;
                if (intent == null || (packageData = intent.getData()) == null || (packageName = packageData.getSchemeSpecificPart()) == null) {
                    return;
                }
                PlatformCompat.this.mCompatConfig.recheckOverrides(packageName);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        context.registerReceiverForAllUsers(receiver, filter, null, null);
    }

    public void registerContentObserver() {
        this.mCompatConfig.registerContentObserver();
    }
}
