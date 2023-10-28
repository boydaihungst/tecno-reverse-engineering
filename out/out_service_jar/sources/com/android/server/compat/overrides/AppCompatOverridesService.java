package com.android.server.compat.overrides;

import android.app.compat.PackageOverride;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.DeviceConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.compat.CompatibilityOverrideConfig;
import com.android.internal.compat.CompatibilityOverridesByPackageConfig;
import com.android.internal.compat.CompatibilityOverridesToRemoveByPackageConfig;
import com.android.internal.compat.CompatibilityOverridesToRemoveConfig;
import com.android.internal.compat.IPlatformCompat;
import com.android.server.SystemService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
/* loaded from: classes.dex */
public final class AppCompatOverridesService {
    private static final List<String> SUPPORTED_NAMESPACES = Arrays.asList("app_compat_overrides");
    private static final String TAG = "AppCompatOverridesService";
    private final Context mContext;
    private final List<DeviceConfigListener> mDeviceConfigListeners;
    private final AppCompatOverridesParser mOverridesParser;
    private final PackageManager mPackageManager;
    private final PackageReceiver mPackageReceiver;
    private final IPlatformCompat mPlatformCompat;
    private final List<String> mSupportedNamespaces;

    private AppCompatOverridesService(Context context) {
        this(context, IPlatformCompat.Stub.asInterface(ServiceManager.getService("platform_compat")), SUPPORTED_NAMESPACES);
    }

    AppCompatOverridesService(Context context, IPlatformCompat platformCompat, List<String> supportedNamespaces) {
        this.mContext = context;
        PackageManager packageManager = context.getPackageManager();
        this.mPackageManager = packageManager;
        this.mPlatformCompat = platformCompat;
        this.mSupportedNamespaces = supportedNamespaces;
        this.mOverridesParser = new AppCompatOverridesParser(packageManager);
        this.mPackageReceiver = new PackageReceiver(context);
        this.mDeviceConfigListeners = new ArrayList();
        for (String namespace : supportedNamespaces) {
            this.mDeviceConfigListeners.add(new DeviceConfigListener(this.mContext, namespace));
        }
    }

    public void finalize() {
        unregisterDeviceConfigListeners();
        unregisterPackageReceiver();
    }

    void registerDeviceConfigListeners() {
        for (DeviceConfigListener listener : this.mDeviceConfigListeners) {
            listener.register();
        }
    }

    private void unregisterDeviceConfigListeners() {
        for (DeviceConfigListener listener : this.mDeviceConfigListeners) {
            listener.unregister();
        }
    }

    void registerPackageReceiver() {
        this.mPackageReceiver.register();
    }

    private void unregisterPackageReceiver() {
        this.mPackageReceiver.unregister();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyAllOverrides(String namespace, Set<Long> ownedChangeIds, Map<String, Set<Long>> packageToChangeIdsToSkip) {
        applyOverrides(DeviceConfig.getProperties(namespace, new String[0]), ownedChangeIds, packageToChangeIdsToSkip);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyOverrides(DeviceConfig.Properties properties, Set<Long> ownedChangeIds, Map<String, Set<Long>> packageToChangeIdsToSkip) {
        Set<String> packageNames = new ArraySet<>(properties.getKeyset());
        packageNames.remove("owned_change_ids");
        packageNames.remove("remove_overrides");
        Map<String, CompatibilityOverrideConfig> packageNameToOverridesToAdd = new ArrayMap<>();
        Map<String, CompatibilityOverridesToRemoveConfig> packageNameToOverridesToRemove = new ArrayMap<>();
        for (String packageName : packageNames) {
            Set<Long> changeIdsToSkip = packageToChangeIdsToSkip.getOrDefault(packageName, Collections.emptySet());
            Map<Long, PackageOverride> overridesToAdd = Collections.emptyMap();
            Long versionCode = getVersionCodeOrNull(packageName);
            if (versionCode != null) {
                overridesToAdd = this.mOverridesParser.parsePackageOverrides(properties.getString(packageName, ""), packageName, versionCode.longValue(), changeIdsToSkip);
            }
            if (!overridesToAdd.isEmpty()) {
                packageNameToOverridesToAdd.put(packageName, new CompatibilityOverrideConfig(overridesToAdd));
            }
            Set<Long> overridesToRemove = new ArraySet<>();
            for (Long changeId : ownedChangeIds) {
                if (!overridesToAdd.containsKey(changeId) && !changeIdsToSkip.contains(changeId)) {
                    overridesToRemove.add(changeId);
                }
            }
            if (!overridesToRemove.isEmpty()) {
                packageNameToOverridesToRemove.put(packageName, new CompatibilityOverridesToRemoveConfig(overridesToRemove));
            }
        }
        putAllPackageOverrides(packageNameToOverridesToAdd);
        removeAllPackageOverrides(packageNameToOverridesToRemove);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addAllPackageOverrides(String packageName) {
        Long versionCode = getVersionCodeOrNull(packageName);
        if (versionCode == null) {
            return;
        }
        for (String namespace : this.mSupportedNamespaces) {
            Set<Long> ownedChangeIds = getOwnedChangeIds(namespace);
            putPackageOverrides(packageName, this.mOverridesParser.parsePackageOverrides(DeviceConfig.getString(namespace, packageName, ""), packageName, versionCode.longValue(), getOverridesToRemove(namespace, ownedChangeIds).getOrDefault(packageName, Collections.emptySet())));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeAllPackageOverrides(String packageName) {
        for (String namespace : this.mSupportedNamespaces) {
            if (!DeviceConfig.getString(namespace, packageName, "").isEmpty()) {
                removePackageOverrides(packageName, getOwnedChangeIds(namespace));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeOverrides(Map<String, Set<Long>> packageNameToOverridesToRemove) {
        Map<String, CompatibilityOverridesToRemoveConfig> packageNameToConfig = new ArrayMap<>();
        for (Map.Entry<String, Set<Long>> packageNameAndChangeIds : packageNameToOverridesToRemove.entrySet()) {
            packageNameToConfig.put(packageNameAndChangeIds.getKey(), new CompatibilityOverridesToRemoveConfig(packageNameAndChangeIds.getValue()));
        }
        removeAllPackageOverrides(packageNameToConfig);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Map<String, Set<Long>> getOverridesToRemove(String namespace, Set<Long> ownedChangeIds) {
        return this.mOverridesParser.parseRemoveOverrides(DeviceConfig.getString(namespace, "remove_overrides", ""), ownedChangeIds);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Set<Long> getOwnedChangeIds(String namespace) {
        return AppCompatOverridesParser.parseOwnedChangeIds(DeviceConfig.getString(namespace, "owned_change_ids", ""));
    }

    private void putAllPackageOverrides(Map<String, CompatibilityOverrideConfig> packageNameToOverrides) {
        if (packageNameToOverrides.isEmpty()) {
            return;
        }
        CompatibilityOverridesByPackageConfig config = new CompatibilityOverridesByPackageConfig(packageNameToOverrides);
        try {
            this.mPlatformCompat.putAllOverridesOnReleaseBuilds(config);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to call IPlatformCompat#putAllOverridesOnReleaseBuilds", e);
        }
    }

    private void putPackageOverrides(String packageName, Map<Long, PackageOverride> overridesToAdd) {
        if (overridesToAdd.isEmpty()) {
            return;
        }
        CompatibilityOverrideConfig config = new CompatibilityOverrideConfig(overridesToAdd);
        try {
            this.mPlatformCompat.putOverridesOnReleaseBuilds(config, packageName);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to call IPlatformCompat#putOverridesOnReleaseBuilds", e);
        }
    }

    private void removeAllPackageOverrides(Map<String, CompatibilityOverridesToRemoveConfig> packageNameToOverridesToRemove) {
        if (packageNameToOverridesToRemove.isEmpty()) {
            return;
        }
        CompatibilityOverridesToRemoveByPackageConfig config = new CompatibilityOverridesToRemoveByPackageConfig(packageNameToOverridesToRemove);
        try {
            this.mPlatformCompat.removeAllOverridesOnReleaseBuilds(config);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to call IPlatformCompat#removeAllOverridesOnReleaseBuilds", e);
        }
    }

    private void removePackageOverrides(String packageName, Set<Long> overridesToRemove) {
        if (overridesToRemove.isEmpty()) {
            return;
        }
        CompatibilityOverridesToRemoveConfig config = new CompatibilityOverridesToRemoveConfig(overridesToRemove);
        try {
            this.mPlatformCompat.removeOverridesOnReleaseBuilds(config, packageName);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to call IPlatformCompat#removeOverridesOnReleaseBuilds", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isInstalledForAnyUser(String packageName) {
        return getVersionCodeOrNull(packageName) != null;
    }

    private Long getVersionCodeOrNull(String packageName) {
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(packageName, 4194304);
            return Long.valueOf(applicationInfo.longVersionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private AppCompatOverridesService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            AppCompatOverridesService appCompatOverridesService = new AppCompatOverridesService(getContext());
            this.mService = appCompatOverridesService;
            appCompatOverridesService.registerDeviceConfigListeners();
            this.mService.registerPackageReceiver();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DeviceConfigListener implements DeviceConfig.OnPropertiesChangedListener {
        private final Context mContext;
        private final String mNamespace;

        private DeviceConfigListener(Context context, String namespace) {
            this.mContext = context;
            this.mNamespace = namespace;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void register() {
            DeviceConfig.addOnPropertiesChangedListener(this.mNamespace, this.mContext.getMainExecutor(), this);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void unregister() {
            DeviceConfig.removeOnPropertiesChangedListener(this);
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            boolean removeOverridesFlagChanged = properties.getKeyset().contains("remove_overrides");
            boolean ownedChangedIdsFlagChanged = properties.getKeyset().contains("owned_change_ids");
            Set<Long> ownedChangeIds = AppCompatOverridesService.getOwnedChangeIds(this.mNamespace);
            Map<String, Set<Long>> overridesToRemove = AppCompatOverridesService.this.getOverridesToRemove(this.mNamespace, ownedChangeIds);
            if (removeOverridesFlagChanged || ownedChangedIdsFlagChanged) {
                AppCompatOverridesService.this.removeOverrides(overridesToRemove);
            }
            if (removeOverridesFlagChanged) {
                AppCompatOverridesService.this.applyAllOverrides(this.mNamespace, ownedChangeIds, overridesToRemove);
            } else {
                AppCompatOverridesService.this.applyOverrides(properties, ownedChangeIds, overridesToRemove);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PackageReceiver extends BroadcastReceiver {
        private final Context mContext;
        private final IntentFilter mIntentFilter;

        private PackageReceiver(Context context) {
            this.mContext = context;
            IntentFilter intentFilter = new IntentFilter();
            this.mIntentFilter = intentFilter;
            intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
            intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addDataScheme("package");
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void register() {
            this.mContext.registerReceiverForAllUsers(this, this.mIntentFilter, null, null);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void unregister() {
            this.mContext.unregisterReceiver(this);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            if (data == null) {
                Slog.w(AppCompatOverridesService.TAG, "Failed to get package name in package receiver");
                return;
            }
            String packageName = data.getSchemeSpecificPart();
            String action = intent.getAction();
            if (action == null) {
                Slog.w(AppCompatOverridesService.TAG, "Failed to get action in package receiver");
                return;
            }
            char c = 65535;
            switch (action.hashCode()) {
                case 172491798:
                    if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                        c = 1;
                        break;
                    }
                    break;
                case 525384130:
                    if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                        c = 2;
                        break;
                    }
                    break;
                case 1544582882:
                    if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                    AppCompatOverridesService.this.addAllPackageOverrides(packageName);
                    return;
                case 2:
                    if (!AppCompatOverridesService.this.isInstalledForAnyUser(packageName)) {
                        AppCompatOverridesService.this.removeAllPackageOverrides(packageName);
                        return;
                    }
                    return;
                default:
                    Slog.w(AppCompatOverridesService.TAG, "Unsupported action in package receiver: " + action);
                    return;
            }
        }
    }
}
