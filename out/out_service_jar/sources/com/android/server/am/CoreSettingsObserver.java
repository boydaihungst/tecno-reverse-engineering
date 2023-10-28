package com.android.server.am;

import android.app.ActivityThread;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class CoreSettingsObserver extends ContentObserver {
    private static final String LOG_TAG = CoreSettingsObserver.class.getSimpleName();
    private static volatile boolean sDeviceConfigContextEntriesLoaded;
    static final List<DeviceConfigEntry> sDeviceConfigEntries;
    static final Map<String, Class<?>> sGlobalSettingToTypeMap;
    static final Map<String, Class<?>> sSecureSettingToTypeMap;
    static final Map<String, Class<?>> sSystemSettingToTypeMap;
    private final ActivityManagerService mActivityManagerService;
    private final Bundle mCoreSettings;

    static {
        HashMap hashMap = new HashMap();
        sSecureSettingToTypeMap = hashMap;
        HashMap hashMap2 = new HashMap();
        sSystemSettingToTypeMap = hashMap2;
        HashMap hashMap3 = new HashMap();
        sGlobalSettingToTypeMap = hashMap3;
        ArrayList arrayList = new ArrayList();
        sDeviceConfigEntries = arrayList;
        hashMap.put("long_press_timeout", Integer.TYPE);
        hashMap.put("multi_press_timeout", Integer.TYPE);
        hashMap2.put("time_12_24", String.class);
        hashMap3.put("debug_view_attributes", Integer.TYPE);
        hashMap3.put("debug_view_attributes_application_package", String.class);
        hashMap3.put("angle_debug_package", String.class);
        hashMap3.put("angle_gl_driver_all_angle", Integer.TYPE);
        hashMap3.put("angle_gl_driver_selection_pkgs", String.class);
        hashMap3.put("angle_gl_driver_selection_values", String.class);
        hashMap3.put("angle_egl_features", String.class);
        hashMap3.put("show_angle_in_use_dialog_box", String.class);
        hashMap3.put("enable_gpu_debug_layers", Integer.TYPE);
        hashMap3.put("gpu_debug_app", String.class);
        hashMap3.put("gpu_debug_layers", String.class);
        hashMap3.put("gpu_debug_layers_gles", String.class);
        hashMap3.put("gpu_debug_layer_app", String.class);
        hashMap3.put("updatable_driver_all_apps", Integer.TYPE);
        hashMap3.put("updatable_driver_production_opt_in_apps", String.class);
        hashMap3.put("updatable_driver_prerelease_opt_in_apps", String.class);
        hashMap3.put("updatable_driver_production_opt_out_apps", String.class);
        hashMap3.put("updatable_driver_production_denylist", String.class);
        hashMap3.put("updatable_driver_production_allowlist", String.class);
        hashMap3.put("updatable_driver_production_denylists", String.class);
        hashMap3.put("updatable_driver_sphal_libraries", String.class);
        arrayList.add(new DeviceConfigEntry("widget", "CursorControlFeature__enable_cursor_drag_from_anywhere", "widget__enable_cursor_drag_from_anywhere", Boolean.TYPE, true));
        arrayList.add(new DeviceConfigEntry("widget", "CursorControlFeature__min_angle_from_vertical_to_start_cursor_drag", "widget__min_angle_from_vertical_to_start_cursor_drag", Integer.TYPE, 45));
        arrayList.add(new DeviceConfigEntry("widget", "CursorControlFeature__finger_to_cursor_distance", "widget__finger_to_cursor_distance", Integer.TYPE, -1));
        arrayList.add(new DeviceConfigEntry("widget", "CursorControlFeature__enable_insertion_handle_gestures", "widget__enable_insertion_handle_gestures", Boolean.TYPE, false));
        arrayList.add(new DeviceConfigEntry("widget", "CursorControlFeature__insertion_handle_delta_height", "widget__insertion_handle_delta_height", Integer.TYPE, 25));
        arrayList.add(new DeviceConfigEntry("widget", "CursorControlFeature__insertion_handle_opacity", "widget__insertion_handle_opacity", Integer.TYPE, 50));
        arrayList.add(new DeviceConfigEntry("widget", "CursorControlFeature__line_slop_ratio", "widget__line_slop_ratio", Float.TYPE, Float.valueOf(0.5f)));
        arrayList.add(new DeviceConfigEntry("widget", "CursorControlFeature__enable_new_magnifier", "widget__enable_new_magnifier", Boolean.TYPE, false));
        arrayList.add(new DeviceConfigEntry("widget", "CursorControlFeature__magnifier_zoom_factor", "widget__magnifier_zoom_factor", Float.TYPE, Float.valueOf(1.5f)));
        arrayList.add(new DeviceConfigEntry("widget", "CursorControlFeature__magnifier_aspect_ratio", "widget__magnifier_aspect_ratio", Float.TYPE, Float.valueOf(5.5f)));
        sDeviceConfigContextEntriesLoaded = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DeviceConfigEntry<T> {
        String coreSettingKey;
        T defaultValue;
        String flag;
        String namespace;
        Class<T> type;

        DeviceConfigEntry(String namespace, String flag, String coreSettingKey, Class<T> type, T defaultValue) {
            this.namespace = namespace;
            this.flag = flag;
            this.coreSettingKey = coreSettingKey;
            this.type = type;
            this.defaultValue = (T) Objects.requireNonNull(defaultValue);
        }
    }

    public CoreSettingsObserver(ActivityManagerService activityManagerService) {
        super(activityManagerService.mHandler);
        this.mCoreSettings = new Bundle();
        if (!sDeviceConfigContextEntriesLoaded) {
            synchronized (sDeviceConfigEntries) {
                if (!sDeviceConfigContextEntriesLoaded) {
                    loadDeviceConfigContextEntries(activityManagerService.mContext);
                    sDeviceConfigContextEntriesLoaded = true;
                }
            }
        }
        this.mActivityManagerService = activityManagerService;
        beginObserveCoreSettings();
        sendCoreSettings();
    }

    private static void loadDeviceConfigContextEntries(Context context) {
        sDeviceConfigEntries.add(new DeviceConfigEntry("widget", "AnalogClockFeature__analog_clock_seconds_hand_fps", "widget__analog_clock_seconds_hand_fps", Integer.TYPE, Integer.valueOf(context.getResources().getInteger(17694781))));
    }

    public Bundle getCoreSettingsLocked() {
        return (Bundle) this.mCoreSettings.clone();
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean selfChange) {
        synchronized (this.mActivityManagerService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                sendCoreSettings();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    private void sendCoreSettings() {
        populateSettings(this.mCoreSettings, sSecureSettingToTypeMap);
        populateSettings(this.mCoreSettings, sSystemSettingToTypeMap);
        populateSettings(this.mCoreSettings, sGlobalSettingToTypeMap);
        populateSettingsFromDeviceConfig();
        this.mActivityManagerService.onCoreSettingsChange(this.mCoreSettings);
    }

    private void beginObserveCoreSettings() {
        for (String setting : sSecureSettingToTypeMap.keySet()) {
            Uri uri = Settings.Secure.getUriFor(setting);
            this.mActivityManagerService.mContext.getContentResolver().registerContentObserver(uri, false, this);
        }
        for (String setting2 : sSystemSettingToTypeMap.keySet()) {
            Uri uri2 = Settings.System.getUriFor(setting2);
            this.mActivityManagerService.mContext.getContentResolver().registerContentObserver(uri2, false, this);
        }
        for (String setting3 : sGlobalSettingToTypeMap.keySet()) {
            Uri uri3 = Settings.Global.getUriFor(setting3);
            this.mActivityManagerService.mContext.getContentResolver().registerContentObserver(uri3, false, this);
        }
        HashSet<String> deviceConfigNamespaces = new HashSet<>();
        for (DeviceConfigEntry entry : sDeviceConfigEntries) {
            if (!deviceConfigNamespaces.contains(entry.namespace)) {
                DeviceConfig.addOnPropertiesChangedListener(entry.namespace, ActivityThread.currentApplication().getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.am.CoreSettingsObserver$$ExternalSyntheticLambda0
                    public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                        CoreSettingsObserver.this.m1423x5ebcd2bf(properties);
                    }
                });
                deviceConfigNamespaces.add(entry.namespace);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$beginObserveCoreSettings$0$com-android-server-am-CoreSettingsObserver  reason: not valid java name */
    public /* synthetic */ void m1423x5ebcd2bf(DeviceConfig.Properties prop) {
        onChange(false);
    }

    void populateSettings(Bundle snapshot, Map<String, Class<?>> map) {
        String value;
        Context context = this.mActivityManagerService.mContext;
        ContentResolver cr = context.getContentResolver();
        for (Map.Entry<String, Class<?>> entry : map.entrySet()) {
            String setting = entry.getKey();
            if (map == sSecureSettingToTypeMap) {
                value = Settings.Secure.getStringForUser(cr, setting, cr.getUserId());
            } else if (map == sSystemSettingToTypeMap) {
                value = Settings.System.getStringForUser(cr, setting, cr.getUserId());
            } else {
                value = Settings.Global.getString(cr, setting);
            }
            if (value == null) {
                snapshot.remove(setting);
            } else {
                Class<?> type = entry.getValue();
                if (type == String.class) {
                    snapshot.putString(setting, value);
                } else if (type == Integer.TYPE) {
                    snapshot.putInt(setting, Integer.parseInt(value));
                } else if (type == Float.TYPE) {
                    snapshot.putFloat(setting, Float.parseFloat(value));
                } else if (type == Long.TYPE) {
                    snapshot.putLong(setting, Long.parseLong(value));
                }
            }
        }
    }

    private void populateSettingsFromDeviceConfig() {
        for (DeviceConfigEntry<?> entry : sDeviceConfigEntries) {
            if (entry.type == String.class) {
                String defaultValue = (String) entry.defaultValue;
                this.mCoreSettings.putString(entry.coreSettingKey, DeviceConfig.getString(entry.namespace, entry.flag, defaultValue));
            } else if (entry.type == Integer.TYPE) {
                int defaultValue2 = ((Integer) entry.defaultValue).intValue();
                this.mCoreSettings.putInt(entry.coreSettingKey, DeviceConfig.getInt(entry.namespace, entry.flag, defaultValue2));
            } else if (entry.type == Float.TYPE) {
                float defaultValue3 = ((Float) entry.defaultValue).floatValue();
                this.mCoreSettings.putFloat(entry.coreSettingKey, DeviceConfig.getFloat(entry.namespace, entry.flag, defaultValue3));
            } else if (entry.type == Long.TYPE) {
                long defaultValue4 = ((Long) entry.defaultValue).longValue();
                this.mCoreSettings.putLong(entry.coreSettingKey, DeviceConfig.getLong(entry.namespace, entry.flag, defaultValue4));
            } else if (entry.type == Boolean.TYPE) {
                boolean defaultValue5 = ((Boolean) entry.defaultValue).booleanValue();
                this.mCoreSettings.putInt(entry.coreSettingKey, DeviceConfig.getBoolean(entry.namespace, entry.flag, defaultValue5) ? 1 : 0);
            }
        }
    }
}
