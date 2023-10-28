package android.util;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/* loaded from: classes3.dex */
public class FeatureFlagUtils {
    private static final Map<String, String> DEFAULT_FLAGS;
    public static final String FFLAG_OVERRIDE_PREFIX = "sys.fflag.override.";
    public static final String FFLAG_PREFIX = "sys.fflag.";
    public static final String HEARING_AID_SETTINGS = "settings_bluetooth_hearing_aid";
    private static final Set<String> PERSISTENT_FLAGS;
    public static final String PERSIST_PREFIX = "persist.sys.fflag.override.";
    public static final String SETTINGS_APP_ALLOW_DARK_THEME_ACTIVATION_AT_BEDTIME = "settings_app_allow_dark_theme_activation_at_bedtime";
    public static final String SETTINGS_APP_LANGUAGE_SELECTION = "settings_app_language_selection";
    public static final String SETTINGS_APP_LOCALE_OPT_IN_ENABLED = "settings_app_locale_opt_in_enabled";
    public static final String SETTINGS_DO_NOT_RESTORE_PRESERVED = "settings_do_not_restore_preserved";
    public static final String SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS = "settings_enable_monitor_phantom_procs";
    public static final String SETTINGS_ENABLE_SECURITY_HUB = "settings_enable_security_hub";
    public static final String SETTINGS_HIDE_SECOND_LAYER_PAGE_NAVIGATE_UP_BUTTON_IN_TWO_PANE = "settings_hide_second_layer_page_navigate_up_button_in_two_pane";
    public static final String SETTINGS_SUPPORT_LARGE_SCREEN = "settings_support_large_screen";
    public static final String SETTINGS_USE_NEW_BACKUP_ELIGIBILITY_RULES = "settings_use_new_backup_eligibility_rules";
    public static final String SETTINGS_WIFITRACKER2 = "settings_wifitracker2";

    static {
        HashMap hashMap = new HashMap();
        DEFAULT_FLAGS = hashMap;
        hashMap.put("settings_audio_switcher", "true");
        hashMap.put("settings_systemui_theme", "true");
        hashMap.put(HEARING_AID_SETTINGS, "false");
        hashMap.put("settings_wifi_details_datausage_header", "false");
        hashMap.put("settings_skip_direction_mutable", "true");
        hashMap.put(SETTINGS_WIFITRACKER2, "true");
        hashMap.put("settings_controller_loading_enhancement", "true");
        hashMap.put("settings_conditionals", "false");
        hashMap.put(SETTINGS_DO_NOT_RESTORE_PRESERVED, "true");
        hashMap.put("settings_tether_all_in_one", "false");
        hashMap.put("settings_contextual_home", "false");
        hashMap.put(SETTINGS_USE_NEW_BACKUP_ELIGIBILITY_RULES, "true");
        hashMap.put(SETTINGS_ENABLE_SECURITY_HUB, "true");
        hashMap.put(SETTINGS_SUPPORT_LARGE_SCREEN, "true");
        hashMap.put("settings_search_always_expand", "true");
        hashMap.put(SETTINGS_APP_LANGUAGE_SELECTION, "true");
        hashMap.put(SETTINGS_APP_LOCALE_OPT_IN_ENABLED, "true");
        hashMap.put(SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS, "true");
        hashMap.put(SETTINGS_APP_ALLOW_DARK_THEME_ACTIVATION_AT_BEDTIME, "true");
        hashMap.put(SETTINGS_HIDE_SECOND_LAYER_PAGE_NAVIGATE_UP_BUTTON_IN_TWO_PANE, "true");
        HashSet hashSet = new HashSet();
        PERSISTENT_FLAGS = hashSet;
        hashSet.add(SETTINGS_APP_LANGUAGE_SELECTION);
        hashSet.add(SETTINGS_APP_LOCALE_OPT_IN_ENABLED);
        hashSet.add(SETTINGS_SUPPORT_LARGE_SCREEN);
        hashSet.add(SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS);
        hashSet.add(SETTINGS_APP_ALLOW_DARK_THEME_ACTIVATION_AT_BEDTIME);
        hashSet.add(SETTINGS_HIDE_SECOND_LAYER_PAGE_NAVIGATE_UP_BUTTON_IN_TWO_PANE);
    }

    public static boolean isEnabled(Context context, String feature) {
        if (context != null) {
            String value = Settings.Global.getString(context.getContentResolver(), feature);
            if (!TextUtils.isEmpty(value)) {
                return Boolean.parseBoolean(value);
            }
        }
        String value2 = SystemProperties.get(getSystemPropertyPrefix(feature) + feature);
        if (!TextUtils.isEmpty(value2)) {
            return Boolean.parseBoolean(value2);
        }
        return Boolean.parseBoolean(getAllFeatureFlags().get(feature));
    }

    public static void setEnabled(Context context, String feature, boolean enabled) {
        SystemProperties.set(getSystemPropertyPrefix(feature) + feature, enabled ? "true" : "false");
    }

    public static Map<String, String> getAllFeatureFlags() {
        return DEFAULT_FLAGS;
    }

    private static String getSystemPropertyPrefix(String feature) {
        return PERSISTENT_FLAGS.contains(feature) ? PERSIST_PREFIX : FFLAG_OVERRIDE_PREFIX;
    }
}
