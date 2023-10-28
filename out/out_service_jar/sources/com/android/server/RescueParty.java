package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.VersionedPackage;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.RemoteCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.ExceptionUtils;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.PackageWatchdog;
import com.android.server.am.SettingsToPropertiesMapper;
import com.android.server.pm.PackageManagerServiceUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class RescueParty {
    static final long DEFAULT_OBSERVING_DURATION_MS = TimeUnit.DAYS.toMillis(2);
    static final int DEVICE_CONFIG_RESET_MODE = 4;
    static final int LEVEL_FACTORY_RESET = 5;
    static final int LEVEL_NONE = 0;
    static final int LEVEL_RESET_SETTINGS_TRUSTED_DEFAULTS = 3;
    static final int LEVEL_RESET_SETTINGS_UNTRUSTED_CHANGES = 2;
    static final int LEVEL_RESET_SETTINGS_UNTRUSTED_DEFAULTS = 1;
    static final int LEVEL_WARM_REBOOT = 4;
    private static final String NAME = "rescue-party-observer";
    static final String NAMESPACE_CONFIGURATION = "configuration";
    static final String NAMESPACE_TO_PACKAGE_MAPPING_FLAG = "namespace_to_package_mapping";
    private static final int PERSISTENT_MASK = 9;
    static final String PROP_ATTEMPTING_FACTORY_RESET = "sys.attempting_factory_reset";
    static final String PROP_ATTEMPTING_REBOOT = "sys.attempting_reboot";
    private static final String PROP_DEVICE_CONFIG_DISABLE_FLAG = "persist.device_config.configuration.disable_rescue_party";
    private static final String PROP_DISABLE_FACTORY_RESET_FLAG = "persist.device_config.configuration.disable_rescue_party_factory_reset";
    private static final String PROP_DISABLE_RESCUE = "persist.sys.disable_rescue";
    static final String PROP_ENABLE_RESCUE = "persist.sys.enable_rescue";
    static final String PROP_MAX_RESCUE_LEVEL_ATTEMPTED = "sys.max_rescue_level_attempted";
    static final String PROP_RESCUE_BOOT_COUNT = "sys.rescue_boot_count";
    private static final String PROP_VIRTUAL_DEVICE = "ro.hardware.virtual_device";
    static final String TAG = "RescueParty";

    /* renamed from: -$$Nest$smisDisabled  reason: not valid java name */
    static /* bridge */ /* synthetic */ boolean m341$$Nest$smisDisabled() {
        return isDisabled();
    }

    public static void registerHealthObserver(Context context) {
        PackageWatchdog.getInstance(context).registerHealthObserver(RescuePartyObserver.getInstance(context));
    }

    private static boolean isDisabled() {
        if (SystemProperties.getBoolean(PROP_ENABLE_RESCUE, false)) {
            return false;
        }
        if (SystemProperties.getBoolean(PROP_DEVICE_CONFIG_DISABLE_FLAG, false)) {
            Slog.v(TAG, "Disabled because of DeviceConfig flag");
            return true;
        } else if (Build.IS_ENG) {
            Slog.v(TAG, "Disabled because of eng build");
            return true;
        } else if (Build.IS_USERDEBUG && isUsbActive()) {
            Slog.v(TAG, "Disabled because of active USB connection");
            return true;
        } else if (SystemProperties.getBoolean(PROP_DISABLE_RESCUE, false)) {
            Slog.v(TAG, "Disabled because of manual property");
            return true;
        } else {
            return false;
        }
    }

    public static boolean isAttemptingFactoryReset() {
        return isFactoryResetPropertySet() || isRebootPropertySet();
    }

    static boolean isFactoryResetPropertySet() {
        return SystemProperties.getBoolean(PROP_ATTEMPTING_FACTORY_RESET, false);
    }

    static boolean isRebootPropertySet() {
        return SystemProperties.getBoolean(PROP_ATTEMPTING_REBOOT, false);
    }

    public static void onSettingsProviderPublished(final Context context) {
        handleNativeRescuePartyResets();
        ContentResolver contentResolver = context.getContentResolver();
        Settings.Config.registerMonitorCallback(contentResolver, new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.RescueParty$$ExternalSyntheticLambda1
            public final void onResult(Bundle bundle) {
                RescueParty.handleMonitorCallback(context, bundle);
            }
        }));
    }

    public static void resetDeviceConfigForPackages(List<String> packageNames) {
        if (packageNames == null) {
            return;
        }
        Set<String> namespacesToReset = new ArraySet<>();
        RescuePartyObserver rescuePartyObserver = RescuePartyObserver.getInstanceIfCreated();
        if (rescuePartyObserver != null) {
            for (String packageName : packageNames) {
                Set<String> runtimeAffectedNamespaces = rescuePartyObserver.getAffectedNamespaceSet(packageName);
                if (runtimeAffectedNamespaces != null) {
                    namespacesToReset.addAll(runtimeAffectedNamespaces);
                }
            }
        }
        Set<String> presetAffectedNamespaces = getPresetNamespacesForPackages(packageNames);
        if (presetAffectedNamespaces != null) {
            namespacesToReset.addAll(presetAffectedNamespaces);
        }
        for (String namespaceToReset : namespacesToReset) {
            DeviceConfig.Properties properties = new DeviceConfig.Properties.Builder(namespaceToReset).build();
            try {
                DeviceConfig.setProperties(properties);
            } catch (DeviceConfig.BadConfigException e) {
                PackageManagerServiceUtils.logCriticalInfo(5, "namespace " + namespaceToReset + " is already banned, skip reset.");
            }
        }
    }

    private static Set<String> getPresetNamespacesForPackages(List<String> packageNames) {
        Set<String> resultSet = new ArraySet<>();
        try {
            try {
                String flagVal = DeviceConfig.getString(NAMESPACE_CONFIGURATION, NAMESPACE_TO_PACKAGE_MAPPING_FLAG, "");
                String[] mappingEntries = flagVal.split(",");
                for (int i = 0; i < mappingEntries.length; i++) {
                    if (!TextUtils.isEmpty(mappingEntries[i])) {
                        String[] splittedEntry = mappingEntries[i].split(":");
                        if (splittedEntry.length != 2) {
                            throw new RuntimeException("Invalid mapping entry: " + mappingEntries[i]);
                        }
                        String namespace = splittedEntry[0];
                        String packageName = splittedEntry[1];
                        if (packageNames.contains(packageName)) {
                            resultSet.add(namespace);
                        }
                    }
                }
                return resultSet;
            } catch (Exception e) {
                resultSet.clear();
                Slog.e(TAG, "Failed to read preset package to namespaces mapping.", e);
                return resultSet;
            }
        } catch (Throwable th) {
            return resultSet;
        }
    }

    static long getElapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static void handleMonitorCallback(Context context, Bundle result) {
        char c;
        String callbackType = result.getString("monitor_callback_type", "");
        switch (callbackType.hashCode()) {
            case -751689299:
                if (callbackType.equals("namespace_updated_callback")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1166372032:
                if (callbackType.equals("access_callback")) {
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
                String updatedNamespace = result.getString("namespace");
                if (updatedNamespace != null) {
                    startObservingPackages(context, updatedNamespace);
                    return;
                }
                return;
            case 1:
                String callingPackage = result.getString("calling_package", null);
                String namespace = result.getString("namespace", null);
                if (namespace != null && callingPackage != null) {
                    RescuePartyObserver.getInstance(context).recordDeviceConfigAccess(callingPackage, namespace);
                    return;
                }
                return;
            default:
                Slog.w(TAG, "Unrecognized DeviceConfig callback");
                return;
        }
    }

    private static void startObservingPackages(Context context, String updatedNamespace) {
        RescuePartyObserver rescuePartyObserver = RescuePartyObserver.getInstance(context);
        Collection<? extends String> callingPackages = rescuePartyObserver.getCallingPackagesSet(updatedNamespace);
        if (callingPackages == null) {
            return;
        }
        List<String> callingPackageList = new ArrayList<>();
        callingPackageList.addAll(callingPackages);
        Slog.i(TAG, "Starting to observe: " + callingPackageList + ", updated namespace: " + updatedNamespace);
        PackageWatchdog.getInstance(context).startObservingHealth(rescuePartyObserver, callingPackageList, DEFAULT_OBSERVING_DURATION_MS);
    }

    private static void handleNativeRescuePartyResets() {
        if (SettingsToPropertiesMapper.isNativeFlagsResetPerformed()) {
            String[] resetNativeCategories = SettingsToPropertiesMapper.getResetNativeCategories();
            for (int i = 0; i < resetNativeCategories.length; i++) {
                if (!NAMESPACE_CONFIGURATION.equals(resetNativeCategories[i])) {
                    DeviceConfig.resetToDefaults(4, resetNativeCategories[i]);
                }
            }
        }
    }

    private static int getMaxRescueLevel(boolean mayPerformFactoryReset) {
        if (!mayPerformFactoryReset || SystemProperties.getBoolean(PROP_DISABLE_FACTORY_RESET_FLAG, false)) {
            return 3;
        }
        return 5;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getRescueLevel(int mitigationCount, boolean mayPerformFactoryReset) {
        if (mitigationCount == 1) {
            return 1;
        }
        if (mitigationCount == 2) {
            return 2;
        }
        if (mitigationCount == 3) {
            return 3;
        }
        if (mitigationCount == 4) {
            return Math.min(getMaxRescueLevel(mayPerformFactoryReset), 4);
        }
        if (mitigationCount >= 5) {
            return Math.min(getMaxRescueLevel(mayPerformFactoryReset), 5);
        }
        Slog.w(TAG, "Expected positive mitigation count, was " + mitigationCount);
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void executeRescueLevel(Context context, String failedPackage, int level) {
        Slog.w(TAG, "Attempting rescue level " + levelToString(level));
        try {
            executeRescueLevelInternal(context, level, failedPackage);
            EventLogTags.writeRescueSuccess(level);
            String successMsg = "Finished rescue level " + levelToString(level);
            if (!TextUtils.isEmpty(failedPackage)) {
                successMsg = successMsg + " for package " + failedPackage;
            }
            PackageManagerServiceUtils.logCriticalInfo(3, successMsg);
        } catch (Throwable t) {
            logRescueException(level, failedPackage, t);
        }
    }

    private static void executeRescueLevelInternal(final Context context, final int level, final String failedPackage) throws Exception {
        FrameworkStatsLog.write(122, level);
        Exception res = null;
        switch (level) {
            case 1:
                try {
                    resetAllSettingsIfNecessary(context, 2, level);
                } catch (Exception e) {
                    res = e;
                }
                try {
                    resetDeviceConfig(context, true, failedPackage);
                    break;
                } catch (Exception e2) {
                    res = e2;
                    break;
                }
            case 2:
                try {
                    resetAllSettingsIfNecessary(context, 3, level);
                } catch (Exception e3) {
                    res = e3;
                }
                try {
                    resetDeviceConfig(context, true, failedPackage);
                    break;
                } catch (Exception e4) {
                    res = e4;
                    break;
                }
            case 3:
                try {
                    resetAllSettingsIfNecessary(context, 4, level);
                } catch (Exception e5) {
                    res = e5;
                }
                try {
                    resetDeviceConfig(context, false, failedPackage);
                    break;
                } catch (Exception e6) {
                    res = e6;
                    break;
                }
            case 4:
                SystemProperties.set(PROP_ATTEMPTING_REBOOT, "true");
                Runnable runnable = new Runnable() { // from class: com.android.server.RescueParty$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        RescueParty.lambda$executeRescueLevelInternal$1(context, level, failedPackage);
                    }
                };
                Thread thread = new Thread(runnable);
                thread.start();
                break;
            case 5:
                SystemProperties.set(PROP_ATTEMPTING_FACTORY_RESET, "true");
                Runnable runnable2 = new Runnable() { // from class: com.android.server.RescueParty.1
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            RecoverySystem.rebootPromptAndWipeUserData(context, RescueParty.TAG);
                        } catch (Throwable t) {
                            RescueParty.logRescueException(level, failedPackage, t);
                        }
                    }
                };
                Thread thread2 = new Thread(runnable2);
                thread2.start();
                break;
        }
        if (res != null) {
            throw res;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$executeRescueLevelInternal$1(Context context, int level, String failedPackage) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(PowerManager.class);
            if (pm != null) {
                pm.reboot(TAG);
            }
        } catch (Throwable t) {
            logRescueException(level, failedPackage, t);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void logRescueException(int level, String failedPackageName, Throwable t) {
        String msg = ExceptionUtils.getCompleteMessage(t);
        EventLogTags.writeRescueFailure(level, msg);
        String failureMsg = "Failed rescue level " + levelToString(level);
        if (!TextUtils.isEmpty(failedPackageName)) {
            failureMsg = failureMsg + " for package " + failedPackageName;
        }
        PackageManagerServiceUtils.logCriticalInfo(6, failureMsg + ": " + msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int mapRescueLevelToUserImpact(int rescueLevel) {
        switch (rescueLevel) {
            case 1:
            case 2:
                return 1;
            case 3:
            case 4:
            case 5:
                return 5;
            default:
                return 0;
        }
    }

    private static void resetAllSettingsIfNecessary(Context context, int mode, int level) throws Exception {
        int[] allUserIds;
        if (SystemProperties.getInt(PROP_MAX_RESCUE_LEVEL_ATTEMPTED, 0) < level) {
            SystemProperties.set(PROP_MAX_RESCUE_LEVEL_ATTEMPTED, Integer.toString(level));
            Exception res = null;
            ContentResolver resolver = context.getContentResolver();
            try {
                Settings.Global.resetToDefaultsAsUser(resolver, null, mode, 0);
            } catch (Exception e) {
                res = new RuntimeException("Failed to reset global settings", e);
            }
            for (int userId : getAllUserIds()) {
                try {
                    Settings.Secure.resetToDefaultsAsUser(resolver, null, mode, userId);
                } catch (Exception e2) {
                    res = new RuntimeException("Failed to reset secure settings for " + userId, e2);
                }
            }
            if (res != null) {
                throw res;
            }
        }
    }

    private static void resetDeviceConfig(Context context, boolean isScoped, String failedPackage) throws Exception {
        context.getContentResolver();
        try {
            if (!isScoped || failedPackage == null) {
                resetAllAffectedNamespaces(context);
            } else {
                performScopedReset(context, failedPackage);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset config settings", e);
        }
    }

    private static void resetAllAffectedNamespaces(Context context) {
        RescuePartyObserver rescuePartyObserver = RescuePartyObserver.getInstance(context);
        Set<String> allAffectedNamespaces = rescuePartyObserver.getAllAffectedNamespaceSet();
        Slog.w(TAG, "Performing reset for all affected namespaces: " + Arrays.toString(allAffectedNamespaces.toArray()));
        for (String namespace : allAffectedNamespaces) {
            if (!NAMESPACE_CONFIGURATION.equals(namespace)) {
                DeviceConfig.resetToDefaults(4, namespace);
            }
        }
    }

    private static void performScopedReset(Context context, String failedPackage) {
        RescuePartyObserver rescuePartyObserver = RescuePartyObserver.getInstance(context);
        Set<String> affectedNamespaces = rescuePartyObserver.getAffectedNamespaceSet(failedPackage);
        if (affectedNamespaces != null) {
            Slog.w(TAG, "Performing scoped reset for package: " + failedPackage + ", affected namespaces: " + Arrays.toString(affectedNamespaces.toArray()));
            for (String namespace : affectedNamespaces) {
                if (!NAMESPACE_CONFIGURATION.equals(namespace)) {
                    DeviceConfig.resetToDefaults(4, namespace);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static class RescuePartyObserver implements PackageWatchdog.PackageHealthObserver {
        static RescuePartyObserver sRescuePartyObserver;
        private final Context mContext;
        private final Map<String, Set<String>> mCallingPackageNamespaceSetMap = new HashMap();
        private final Map<String, Set<String>> mNamespaceCallingPackageSetMap = new HashMap();

        private RescuePartyObserver(Context context) {
            this.mContext = context;
        }

        public static RescuePartyObserver getInstance(Context context) {
            RescuePartyObserver rescuePartyObserver;
            synchronized (RescuePartyObserver.class) {
                if (sRescuePartyObserver == null) {
                    sRescuePartyObserver = new RescuePartyObserver(context);
                }
                rescuePartyObserver = sRescuePartyObserver;
            }
            return rescuePartyObserver;
        }

        public static RescuePartyObserver getInstanceIfCreated() {
            RescuePartyObserver rescuePartyObserver;
            synchronized (RescuePartyObserver.class) {
                rescuePartyObserver = sRescuePartyObserver;
            }
            return rescuePartyObserver;
        }

        static void reset() {
            synchronized (RescuePartyObserver.class) {
                sRescuePartyObserver = null;
            }
        }

        @Override // com.android.server.PackageWatchdog.PackageHealthObserver
        public int onHealthCheckFailed(VersionedPackage failedPackage, int failureReason, int mitigationCount) {
            if (RescueParty.m341$$Nest$smisDisabled()) {
                return 0;
            }
            if (failureReason == 3 || failureReason == 4) {
                return RescueParty.mapRescueLevelToUserImpact(RescueParty.getRescueLevel(mitigationCount, mayPerformFactoryReset(failedPackage)));
            }
            return 0;
        }

        @Override // com.android.server.PackageWatchdog.PackageHealthObserver
        public boolean execute(VersionedPackage failedPackage, int failureReason, int mitigationCount) {
            if (RescueParty.m341$$Nest$smisDisabled()) {
                return false;
            }
            if (failureReason == 3 || failureReason == 4) {
                int level = RescueParty.getRescueLevel(mitigationCount, mayPerformFactoryReset(failedPackage));
                RescueParty.executeRescueLevel(this.mContext, failedPackage == null ? null : failedPackage.getPackageName(), level);
                return true;
            }
            return false;
        }

        @Override // com.android.server.PackageWatchdog.PackageHealthObserver
        public boolean isPersistent() {
            return true;
        }

        @Override // com.android.server.PackageWatchdog.PackageHealthObserver
        public boolean mayObservePackage(String packageName) {
            PackageManager pm = this.mContext.getPackageManager();
            try {
                if (pm.getModuleInfo(packageName, 0) != null) {
                    return true;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
            return isPersistentSystemApp(packageName);
        }

        @Override // com.android.server.PackageWatchdog.PackageHealthObserver
        public int onBootLoop(int mitigationCount) {
            if (RescueParty.m341$$Nest$smisDisabled()) {
                return 0;
            }
            return RescueParty.mapRescueLevelToUserImpact(RescueParty.getRescueLevel(mitigationCount, true));
        }

        @Override // com.android.server.PackageWatchdog.PackageHealthObserver
        public boolean executeBootLoopMitigation(int mitigationCount) {
            if (RescueParty.m341$$Nest$smisDisabled()) {
                return false;
            }
            RescueParty.executeRescueLevel(this.mContext, null, RescueParty.getRescueLevel(mitigationCount, true));
            return true;
        }

        @Override // com.android.server.PackageWatchdog.PackageHealthObserver
        public String getName() {
            return RescueParty.NAME;
        }

        private boolean mayPerformFactoryReset(VersionedPackage failingPackage) {
            if (failingPackage == null) {
                return false;
            }
            return isPersistentSystemApp(failingPackage.getPackageName());
        }

        private boolean isPersistentSystemApp(String packageName) {
            PackageManager pm = this.mContext.getPackageManager();
            try {
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                return (info.flags & 9) == 9;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized void recordDeviceConfigAccess(String callingPackage, String namespace) {
            Set<String> namespaceSet = this.mCallingPackageNamespaceSetMap.get(callingPackage);
            if (namespaceSet == null) {
                namespaceSet = new ArraySet();
                this.mCallingPackageNamespaceSetMap.put(callingPackage, namespaceSet);
            }
            namespaceSet.add(namespace);
            Set<String> callingPackageSet = this.mNamespaceCallingPackageSetMap.get(namespace);
            if (callingPackageSet == null) {
                callingPackageSet = new ArraySet();
            }
            callingPackageSet.add(callingPackage);
            this.mNamespaceCallingPackageSetMap.put(namespace, callingPackageSet);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized Set<String> getAffectedNamespaceSet(String failedPackage) {
            return this.mCallingPackageNamespaceSetMap.get(failedPackage);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized Set<String> getAllAffectedNamespaceSet() {
            return new HashSet(this.mNamespaceCallingPackageSetMap.keySet());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public synchronized Set<String> getCallingPackagesSet(String namespace) {
            return this.mNamespaceCallingPackageSetMap.get(namespace);
        }
    }

    private static int[] getAllUserIds() {
        File[] listFilesOrEmpty;
        int[] userIds = {0};
        try {
            for (File file : FileUtils.listFilesOrEmpty(Environment.getDataSystemDeDirectory())) {
                try {
                    int userId = Integer.parseInt(file.getName());
                    if (userId != 0) {
                        userIds = ArrayUtils.appendInt(userIds, userId);
                    }
                } catch (NumberFormatException e) {
                }
            }
        } catch (Throwable t) {
            Slog.w(TAG, "Trouble discovering users", t);
        }
        return userIds;
    }

    private static boolean isUsbActive() {
        if (SystemProperties.getBoolean(PROP_VIRTUAL_DEVICE, false)) {
            Slog.v(TAG, "Assuming virtual device is connected over USB");
            return true;
        }
        try {
            String state = FileUtils.readTextFile(new File("/sys/class/android_usb/android0/state"), 128, "");
            return "CONFIGURED".equals(state.trim());
        } catch (Throwable t) {
            Slog.w(TAG, "Failed to determine if device was on USB", t);
            return false;
        }
    }

    private static String levelToString(int level) {
        switch (level) {
            case 0:
                return "NONE";
            case 1:
                return "RESET_SETTINGS_UNTRUSTED_DEFAULTS";
            case 2:
                return "RESET_SETTINGS_UNTRUSTED_CHANGES";
            case 3:
                return "RESET_SETTINGS_TRUSTED_DEFAULTS";
            case 4:
                return "WARM_REBOOT";
            case 5:
                return "FACTORY_RESET";
            default:
                return Integer.toString(level);
        }
    }
}
