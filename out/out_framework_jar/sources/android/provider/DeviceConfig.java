package android.provider;

import android.Manifest;
import android.annotation.SystemApi;
import android.app.ActivityThread;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import com.android.internal.util.Preconditions;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
@SystemApi
/* loaded from: classes3.dex */
public final class DeviceConfig {
    @SystemApi
    public static final String NAMESPACE_ACTIVITY_MANAGER = "activity_manager";
    public static final String NAMESPACE_ACTIVITY_MANAGER_COMPONENT_ALIAS = "activity_manager_ca";
    @SystemApi
    public static final String NAMESPACE_ACTIVITY_MANAGER_NATIVE_BOOT = "activity_manager_native_boot";
    @SystemApi
    public static final String NAMESPACE_ADSERVICES = "adservices";
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_ALARM_MANAGER = "alarm_manager";
    @SystemApi
    public static final String NAMESPACE_AMBIENT_CONTEXT_MANAGER_SERVICE = "ambient_context_manager_service";
    public static final String NAMESPACE_ANDROID = "android";
    @SystemApi
    public static final String NAMESPACE_APPSEARCH = "appsearch";
    @SystemApi
    public static final String NAMESPACE_APP_COMPAT = "app_compat";
    public static final String NAMESPACE_APP_COMPAT_OVERRIDES = "app_compat_overrides";
    @SystemApi
    public static final String NAMESPACE_APP_HIBERNATION = "app_hibernation";
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_APP_STANDBY = "app_standby";
    @SystemApi
    public static final String NAMESPACE_ATTENTION_MANAGER_SERVICE = "attention_manager_service";
    @SystemApi
    public static final String NAMESPACE_AUTOFILL = "autofill";
    @SystemApi
    public static final String NAMESPACE_BATTERY_SAVER = "battery_saver";
    @SystemApi
    public static final String NAMESPACE_BIOMETRICS = "biometrics";
    @SystemApi
    public static final String NAMESPACE_BLOBSTORE = "blobstore";
    @SystemApi
    public static final String NAMESPACE_BLUETOOTH = "bluetooth";
    @SystemApi
    public static final String NAMESPACE_CAPTIVEPORTALLOGIN = "captive_portal_login";
    @SystemApi
    public static final String NAMESPACE_CLIPBOARD = "clipboard";
    public static final String NAMESPACE_CONFIGURATION = "configuration";
    @SystemApi
    public static final String NAMESPACE_CONNECTIVITY = "connectivity";
    public static final String NAMESPACE_CONNECTIVITY_THERMAL_POWER_MANAGER = "connectivity_thermal_power_manager";
    public static final String NAMESPACE_CONSTRAIN_DISPLAY_APIS = "constrain_display_apis";
    public static final String NAMESPACE_CONTACTS_PROVIDER = "contacts_provider";
    @SystemApi
    public static final String NAMESPACE_CONTENT_CAPTURE = "content_capture";
    @SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
    public static final String NAMESPACE_DEVICE_IDLE = "device_idle";
    @SystemApi
    @Deprecated
    public static final String NAMESPACE_DEX_BOOT = "dex_boot";
    @SystemApi
    public static final String NAMESPACE_DISPLAY_MANAGER = "display_manager";
    @SystemApi
    public static final String NAMESPACE_GAME_DRIVER = "game_driver";
    public static final String NAMESPACE_GAME_OVERLAY = "game_overlay";
    @SystemApi
    public static final String NAMESPACE_INPUT_NATIVE_BOOT = "input_native_boot";
    @SystemApi
    public static final String NAMESPACE_INTELLIGENCE_ATTENTION = "intelligence_attention";
    public static final String NAMESPACE_INTELLIGENCE_CONTENT_SUGGESTIONS = "intelligence_content_suggestions";
    public static final String NAMESPACE_INTERACTION_JANK_MONITOR = "interaction_jank_monitor";
    public static final String NAMESPACE_JOB_SCHEDULER = "jobscheduler";
    public static final String NAMESPACE_LATENCY_TRACKER = "latency_tracker";
    @SystemApi
    public static final String NAMESPACE_LMKD_NATIVE = "lmkd_native";
    @SystemApi
    public static final String NAMESPACE_LOCATION = "location";
    @SystemApi
    public static final String NAMESPACE_MEDIA = "media";
    @SystemApi
    public static final String NAMESPACE_MEDIA_NATIVE = "media_native";
    @SystemApi
    public static final String NAMESPACE_NETD_NATIVE = "netd_native";
    @SystemApi
    public static final String NAMESPACE_NNAPI_NATIVE = "nnapi_native";
    @SystemApi
    public static final String NAMESPACE_ON_DEVICE_PERSONALIZATION = "on_device_personalization";
    @SystemApi
    public static final String NAMESPACE_OTA = "ota";
    @SystemApi
    public static final String NAMESPACE_PACKAGE_MANAGER_SERVICE = "package_manager_service";
    @SystemApi
    public static final String NAMESPACE_PERMISSIONS = "permissions";
    @SystemApi
    public static final String NAMESPACE_PRIVACY = "privacy";
    @SystemApi
    public static final String NAMESPACE_PROFCOLLECT_NATIVE_BOOT = "profcollect_native_boot";
    @SystemApi
    public static final String NAMESPACE_REBOOT_READINESS = "reboot_readiness";
    @SystemApi
    public static final String NAMESPACE_ROLLBACK = "rollback";
    @SystemApi
    public static final String NAMESPACE_ROLLBACK_BOOT = "rollback_boot";
    public static final String NAMESPACE_ROTATION_RESOLVER = "rotation_resolver";
    @SystemApi
    public static final String NAMESPACE_RUNTIME_NATIVE = "runtime_native";
    @SystemApi
    public static final String NAMESPACE_RUNTIME_NATIVE_BOOT = "runtime_native_boot";
    @SystemApi
    public static final String NAMESPACE_SCHEDULER = "scheduler";
    @SystemApi
    public static final String NAMESPACE_SDK_SANDBOX = "sdk_sandbox";
    public static final String NAMESPACE_SELECTION_TOOLBAR = "selection_toolbar";
    public static final String NAMESPACE_SETTINGS_STATS = "settings_stats";
    public static final String NAMESPACE_SETTINGS_UI = "settings_ui";
    @SystemApi
    public static final String NAMESPACE_STATSD_NATIVE = "statsd_native";
    @SystemApi
    public static final String NAMESPACE_STATSD_NATIVE_BOOT = "statsd_native_boot";
    @SystemApi
    @Deprecated
    public static final String NAMESPACE_STORAGE = "storage";
    @SystemApi
    public static final String NAMESPACE_STORAGE_NATIVE_BOOT = "storage_native_boot";
    @SystemApi
    public static final String NAMESPACE_SURFACE_FLINGER_NATIVE_BOOT = "surface_flinger_native_boot";
    @SystemApi
    public static final String NAMESPACE_SWCODEC_NATIVE = "swcodec_native";
    @SystemApi
    public static final String NAMESPACE_SYSTEMUI = "systemui";
    @SystemApi
    public static final String NAMESPACE_SYSTEM_TIME = "system_time";
    public static final String NAMESPACE_TARE = "tare";
    @SystemApi
    public static final String NAMESPACE_TELEPHONY = "telephony";
    @SystemApi
    public static final String NAMESPACE_TETHERING = "tethering";
    @SystemApi
    public static final String NAMESPACE_UWB = "uwb";
    public static final String NAMESPACE_VENDOR_SYSTEM_NATIVE = "vendor_system_native";
    public static final String NAMESPACE_VIRTUALIZATION_FRAMEWORK_NATIVE = "virtualization_framework_native";
    public static final String NAMESPACE_VOICE_INTERACTION = "voice_interaction";
    public static final String NAMESPACE_WIDGET = "widget";
    public static final String NAMESPACE_WINDOW_MANAGER = "window_manager";
    @SystemApi
    public static final String NAMESPACE_WINDOW_MANAGER_NATIVE_BOOT = "window_manager_native_boot";
    private static final String TAG = "DeviceConfig";
    public static final Uri CONTENT_URI = Uri.parse("content://settings/config");
    @SystemApi
    public static final String NAMESPACE_TEXTCLASSIFIER = "textclassifier";
    @SystemApi
    public static final String NAMESPACE_RUNTIME = "runtime";
    @SystemApi
    public static final String NAMESPACE_STATSD_JAVA = "statsd_java";
    @SystemApi
    public static final String NAMESPACE_STATSD_JAVA_BOOT = "statsd_java_boot";
    public static final String NAMESPACE_DEVICE_POLICY_MANAGER = "device_policy_manager";
    private static final List<String> PUBLIC_NAMESPACES = Arrays.asList(NAMESPACE_TEXTCLASSIFIER, NAMESPACE_RUNTIME, NAMESPACE_STATSD_JAVA, NAMESPACE_STATSD_JAVA_BOOT, "selection_toolbar", "autofill", NAMESPACE_DEVICE_POLICY_MANAGER);
    private static final Object sLock = new Object();
    private static ArrayMap<OnPropertiesChangedListener, Pair<String, Executor>> sListeners = new ArrayMap<>();
    private static Map<String, Pair<ContentObserver, Integer>> sNamespaces = new HashMap();

    @SystemApi
    /* loaded from: classes3.dex */
    public static class BadConfigException extends Exception {
    }

    @SystemApi
    /* loaded from: classes3.dex */
    public interface OnPropertiesChangedListener {
        void onPropertiesChanged(Properties properties);
    }

    private DeviceConfig() {
    }

    @SystemApi
    public static String getProperty(String namespace, String name) {
        return getProperties(namespace, name).getString(name, null);
    }

    @SystemApi
    public static Properties getProperties(String namespace, String... names) {
        ContentResolver contentResolver = ActivityThread.currentApplication().getContentResolver();
        return new Properties(namespace, Settings.Config.getStrings(contentResolver, namespace, Arrays.asList(names)));
    }

    @SystemApi
    public static String getString(String namespace, String name, String defaultValue) {
        String value = getProperty(namespace, name);
        return value != null ? value : defaultValue;
    }

    @SystemApi
    public static boolean getBoolean(String namespace, String name, boolean defaultValue) {
        String value = getProperty(namespace, name);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    @SystemApi
    public static int getInt(String namespace, String name, int defaultValue) {
        String value = getProperty(namespace, name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Parsing integer failed for " + namespace + ":" + name);
            return defaultValue;
        }
    }

    @SystemApi
    public static long getLong(String namespace, String name, long defaultValue) {
        String value = getProperty(namespace, name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Parsing long failed for " + namespace + ":" + name);
            return defaultValue;
        }
    }

    @SystemApi
    public static float getFloat(String namespace, String name, float defaultValue) {
        String value = getProperty(namespace, name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Parsing float failed for " + namespace + ":" + name);
            return defaultValue;
        }
    }

    @SystemApi
    public static boolean setProperty(String namespace, String name, String value, boolean makeDefault) {
        ContentResolver contentResolver = ActivityThread.currentApplication().getContentResolver();
        return Settings.Config.putString(contentResolver, namespace, name, value, makeDefault);
    }

    @SystemApi
    public static boolean setProperties(Properties properties) throws BadConfigException {
        ContentResolver contentResolver = ActivityThread.currentApplication().getContentResolver();
        return Settings.Config.setStrings(contentResolver, properties.getNamespace(), properties.mMap);
    }

    @SystemApi
    public static boolean deleteProperty(String namespace, String name) {
        ContentResolver contentResolver = ActivityThread.currentApplication().getContentResolver();
        return Settings.Config.deleteString(contentResolver, namespace, name);
    }

    @SystemApi
    public static void resetToDefaults(int resetMode, String namespace) {
        ContentResolver contentResolver = ActivityThread.currentApplication().getContentResolver();
        Settings.Config.resetToDefaults(contentResolver, resetMode, namespace);
    }

    public static void setSyncDisabledMode(int syncDisabledMode) {
        ContentResolver contentResolver = ActivityThread.currentApplication().getContentResolver();
        Settings.Config.setSyncDisabledMode(contentResolver, syncDisabledMode);
    }

    public static int getSyncDisabledMode() {
        ContentResolver contentResolver = ActivityThread.currentApplication().getContentResolver();
        return Settings.Config.getSyncDisabledMode(contentResolver);
    }

    @SystemApi
    public static void addOnPropertiesChangedListener(String namespace, Executor executor, OnPropertiesChangedListener onPropertiesChangedListener) {
        enforceReadPermission(ActivityThread.currentApplication().getApplicationContext(), namespace);
        synchronized (sLock) {
            Pair<String, Executor> oldNamespace = sListeners.get(onPropertiesChangedListener);
            if (oldNamespace == null) {
                sListeners.put(onPropertiesChangedListener, new Pair<>(namespace, executor));
                incrementNamespace(namespace);
            } else if (namespace.equals(oldNamespace.first)) {
                sListeners.put(onPropertiesChangedListener, new Pair<>(namespace, executor));
            } else {
                decrementNamespace(sListeners.get(onPropertiesChangedListener).first);
                sListeners.put(onPropertiesChangedListener, new Pair<>(namespace, executor));
                incrementNamespace(namespace);
            }
        }
    }

    @SystemApi
    public static void removeOnPropertiesChangedListener(OnPropertiesChangedListener onPropertiesChangedListener) {
        Preconditions.checkNotNull(onPropertiesChangedListener);
        synchronized (sLock) {
            if (sListeners.containsKey(onPropertiesChangedListener)) {
                decrementNamespace(sListeners.get(onPropertiesChangedListener).first);
                sListeners.remove(onPropertiesChangedListener);
            }
        }
    }

    private static Uri createNamespaceUri(String namespace) {
        Preconditions.checkNotNull(namespace);
        return CONTENT_URI.buildUpon().appendPath(namespace).build();
    }

    private static void incrementNamespace(String namespace) {
        Preconditions.checkNotNull(namespace);
        Pair<ContentObserver, Integer> namespaceCount = sNamespaces.get(namespace);
        if (namespaceCount != null) {
            sNamespaces.put(namespace, new Pair<>(namespaceCount.first, Integer.valueOf(namespaceCount.second.intValue() + 1)));
            return;
        }
        ContentObserver contentObserver = new ContentObserver(null) { // from class: android.provider.DeviceConfig.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null) {
                    DeviceConfig.handleChange(uri);
                }
            }
        };
        ActivityThread.currentApplication().getContentResolver().registerContentObserver(createNamespaceUri(namespace), true, contentObserver);
        sNamespaces.put(namespace, new Pair<>(contentObserver, 1));
    }

    private static void decrementNamespace(String namespace) {
        Preconditions.checkNotNull(namespace);
        Pair<ContentObserver, Integer> namespaceCount = sNamespaces.get(namespace);
        if (namespaceCount == null) {
            return;
        }
        if (namespaceCount.second.intValue() > 1) {
            sNamespaces.put(namespace, new Pair<>(namespaceCount.first, Integer.valueOf(namespaceCount.second.intValue() - 1)));
            return;
        }
        ActivityThread.currentApplication().getContentResolver().unregisterContentObserver(namespaceCount.first);
        sNamespaces.remove(namespace);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void handleChange(Uri uri) {
        Preconditions.checkNotNull(uri);
        List<String> pathSegments = uri.getPathSegments();
        String namespace = pathSegments.get(1);
        Properties.Builder propBuilder = new Properties.Builder(namespace);
        try {
            Properties allProperties = getProperties(namespace, new String[0]);
            for (int i = 2; i < pathSegments.size(); i++) {
                String key = pathSegments.get(i);
                propBuilder.setString(key, allProperties.getString(key, null));
            }
            final Properties properties = propBuilder.build();
            synchronized (sLock) {
                for (int i2 = 0; i2 < sListeners.size(); i2++) {
                    if (namespace.equals(sListeners.valueAt(i2).first)) {
                        final OnPropertiesChangedListener listener = sListeners.keyAt(i2);
                        sListeners.valueAt(i2).second.execute(new Runnable() { // from class: android.provider.DeviceConfig$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                DeviceConfig.OnPropertiesChangedListener.this.onPropertiesChanged(properties);
                            }
                        });
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "OnPropertyChangedListener update failed: permission violation.");
        }
    }

    public static void enforceReadPermission(Context context, String namespace) {
        if (context.checkCallingOrSelfPermission(Manifest.permission.READ_DEVICE_CONFIG) != 0 && !PUBLIC_NAMESPACES.contains(namespace)) {
            throw new SecurityException("Permission denial: reading from settings requires:android.permission.READ_DEVICE_CONFIG");
        }
    }

    public static List<String> getPublicNamespaces() {
        return PUBLIC_NAMESPACES;
    }

    @SystemApi
    /* loaded from: classes3.dex */
    public static class Properties {
        private Set<String> mKeyset;
        private final HashMap<String, String> mMap;
        private final String mNamespace;

        public Properties(String namespace, Map<String, String> keyValueMap) {
            Preconditions.checkNotNull(namespace);
            this.mNamespace = namespace;
            HashMap<String, String> hashMap = new HashMap<>();
            this.mMap = hashMap;
            if (keyValueMap != null) {
                hashMap.putAll(keyValueMap);
            }
        }

        public String getNamespace() {
            return this.mNamespace;
        }

        public Set<String> getKeyset() {
            if (this.mKeyset == null) {
                this.mKeyset = Collections.unmodifiableSet(this.mMap.keySet());
            }
            return this.mKeyset;
        }

        public String getString(String name, String defaultValue) {
            Preconditions.checkNotNull(name);
            String value = this.mMap.get(name);
            return value != null ? value : defaultValue;
        }

        public boolean getBoolean(String name, boolean defaultValue) {
            Preconditions.checkNotNull(name);
            String value = this.mMap.get(name);
            return value != null ? Boolean.parseBoolean(value) : defaultValue;
        }

        public int getInt(String name, int defaultValue) {
            Preconditions.checkNotNull(name);
            String value = this.mMap.get(name);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                Log.e(DeviceConfig.TAG, "Parsing int failed for " + name);
                return defaultValue;
            }
        }

        public long getLong(String name, long defaultValue) {
            Preconditions.checkNotNull(name);
            String value = this.mMap.get(name);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                Log.e(DeviceConfig.TAG, "Parsing long failed for " + name);
                return defaultValue;
            }
        }

        public float getFloat(String name, float defaultValue) {
            Preconditions.checkNotNull(name);
            String value = this.mMap.get(name);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                Log.e(DeviceConfig.TAG, "Parsing float failed for " + name);
                return defaultValue;
            }
        }

        /* loaded from: classes3.dex */
        public static final class Builder {
            private final Map<String, String> mKeyValues = new HashMap();
            private final String mNamespace;

            public Builder(String namespace) {
                this.mNamespace = namespace;
            }

            public Builder setString(String name, String value) {
                this.mKeyValues.put(name, value);
                return this;
            }

            public Builder setBoolean(String name, boolean value) {
                this.mKeyValues.put(name, Boolean.toString(value));
                return this;
            }

            public Builder setInt(String name, int value) {
                this.mKeyValues.put(name, Integer.toString(value));
                return this;
            }

            public Builder setLong(String name, long value) {
                this.mKeyValues.put(name, Long.toString(value));
                return this;
            }

            public Builder setFloat(String name, float value) {
                this.mKeyValues.put(name, Float.toString(value));
                return this;
            }

            public Properties build() {
                return new Properties(this.mNamespace, this.mKeyValues);
            }
        }
    }
}
