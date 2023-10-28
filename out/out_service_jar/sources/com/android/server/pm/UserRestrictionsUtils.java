package com.android.server.pm;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IStopUserCallback;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManagerInternal;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.BundleUtils;
import com.android.server.LocalServices;
import com.google.android.collect.Sets;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;
/* loaded from: classes2.dex */
public class UserRestrictionsUtils {
    private static final String TAG = "UserRestrictionsUtils";
    public static final Set<String> USER_RESTRICTIONS = newSetWithUniqueCheck(new String[]{"no_config_wifi", "no_config_locale", "no_modify_accounts", "no_install_apps", "no_uninstall_apps", "no_share_location", "no_install_unknown_sources", "no_install_unknown_sources_globally", "no_config_bluetooth", "no_bluetooth", "no_bluetooth_sharing", "no_usb_file_transfer", "no_config_credentials", "no_remove_user", "no_remove_managed_profile", "no_debugging_features", "no_config_vpn", "no_config_date_time", "no_config_tethering", "no_network_reset", "no_factory_reset", "no_add_user", "no_add_managed_profile", "no_add_clone_profile", "ensure_verify_apps", "no_config_cell_broadcasts", "no_config_mobile_networks", "no_control_apps", "no_physical_media", "no_unmute_microphone", "no_adjust_volume", "no_outgoing_calls", "no_sms", "no_fun", "no_create_windows", "no_system_error_dialogs", "no_cross_profile_copy_paste", "no_outgoing_beam", "no_wallpaper", "no_safe_boot", "allow_parent_profile_app_linking", "no_record_audio", "no_camera", "no_run_in_background", "no_data_roaming", "no_set_user_icon", "no_set_wallpaper", "no_oem_unlock", "disallow_unmute_device", "no_autofill", "no_content_capture", "no_content_suggestions", "no_user_switch", "no_unified_password", "no_config_location", "no_airplane_mode", "no_config_brightness", "no_sharing_into_profile", "no_ambient_display", "no_config_screen_timeout", "no_printing", "disallow_config_private_dns", "disallow_microphone_toggle", "disallow_camera_toggle", "no_change_wifi_state", "no_wifi_tethering", "no_sharing_admin_configured_wifi", "no_wifi_direct", "no_add_wifi_config"});
    public static final Set<String> DEPRECATED_USER_RESTRICTIONS = Sets.newArraySet(new String[]{"no_add_managed_profile", "no_remove_managed_profile"});
    private static final Set<String> NON_PERSIST_USER_RESTRICTIONS = Sets.newArraySet(new String[]{"no_record_audio"});
    private static final Set<String> PRIMARY_USER_ONLY_RESTRICTIONS = Sets.newArraySet(new String[]{"no_bluetooth", "no_usb_file_transfer", "no_config_tethering", "no_network_reset", "no_factory_reset", "no_add_user", "no_config_cell_broadcasts", "no_config_mobile_networks", "no_physical_media", "no_sms", "no_fun", "no_safe_boot", "no_create_windows", "no_data_roaming", "no_airplane_mode"});
    private static final Set<String> DEVICE_OWNER_ONLY_RESTRICTIONS = Sets.newArraySet(new String[]{"no_user_switch", "disallow_config_private_dns", "disallow_microphone_toggle", "disallow_camera_toggle", "no_change_wifi_state", "no_wifi_tethering", "no_wifi_direct", "no_add_wifi_config"});
    private static final Set<String> IMMUTABLE_BY_OWNERS = Sets.newArraySet(new String[]{"no_record_audio", "no_wallpaper", "no_oem_unlock"});
    private static final Set<String> GLOBAL_RESTRICTIONS = Sets.newArraySet(new String[]{"no_adjust_volume", "no_bluetooth_sharing", "no_config_date_time", "no_system_error_dialogs", "no_run_in_background", "no_unmute_microphone", "disallow_unmute_device", "no_camera"});
    private static final Set<String> PROFILE_OWNER_ORGANIZATION_OWNED_GLOBAL_RESTRICTIONS = Sets.newArraySet(new String[]{"no_airplane_mode", "no_config_date_time", "disallow_config_private_dns", "no_change_wifi_state", "no_wifi_tethering", "no_wifi_direct", "no_add_wifi_config"});
    private static final Set<String> PROFILE_OWNER_ORGANIZATION_OWNED_LOCAL_RESTRICTIONS = Sets.newArraySet(new String[]{"no_config_bluetooth", "no_config_location", "no_config_wifi", "no_content_capture", "no_content_suggestions", "no_debugging_features", "no_share_location", "no_outgoing_calls", "no_camera", "no_bluetooth", "no_bluetooth_sharing", "no_config_cell_broadcasts", "no_config_mobile_networks", "no_config_tethering", "no_data_roaming", "no_safe_boot", "no_sms", "no_usb_file_transfer", "no_physical_media", "no_unmute_microphone"});
    private static final Set<String> DEFAULT_ENABLED_FOR_MANAGED_PROFILES = Sets.newArraySet(new String[]{"no_bluetooth_sharing"});
    private static final Set<String> PROFILE_GLOBAL_RESTRICTIONS = Sets.newArraySet(new String[]{"ensure_verify_apps", "no_airplane_mode", "no_install_unknown_sources_globally"});
    private static final Set<String> FINANCED_DEVICE_OWNER_RESTRICTIONS = Sets.newArraySet(new String[]{"no_add_user", "no_debugging_features", "no_install_unknown_sources", "no_safe_boot", "no_config_date_time", "no_outgoing_calls"});

    private UserRestrictionsUtils() {
    }

    private static Set<String> newSetWithUniqueCheck(String[] strings) {
        Set<String> ret = Sets.newArraySet(strings);
        Preconditions.checkState(ret.size() == strings.length);
        return ret;
    }

    public static boolean isValidRestriction(String restriction) {
        if (USER_RESTRICTIONS.contains(restriction)) {
            return true;
        }
        int uid = Binder.getCallingUid();
        String[] pkgs = null;
        try {
            pkgs = AppGlobals.getPackageManager().getPackagesForUid(uid);
        } catch (RemoteException e) {
        }
        StringBuilder msg = new StringBuilder("Unknown restriction queried by uid ");
        msg.append(uid);
        if (pkgs != null && pkgs.length > 0) {
            msg.append(" (");
            msg.append(pkgs[0]);
            if (pkgs.length > 1) {
                msg.append(" et al");
            }
            msg.append(")");
        }
        msg.append(": ");
        msg.append(restriction);
        if (restriction == null || !isSystemApp(uid, pkgs)) {
            Slog.e(TAG, msg.toString());
        } else {
            Slog.wtf(TAG, msg.toString());
        }
        return false;
    }

    private static boolean isSystemApp(int uid, String[] packageList) {
        if (UserHandle.isCore(uid)) {
            return true;
        }
        if (packageList == null) {
            return false;
        }
        IPackageManager pm = AppGlobals.getPackageManager();
        for (String str : packageList) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(str, 794624L, UserHandle.getUserId(uid));
                if (appInfo != null && appInfo.isSystemApp()) {
                    return true;
                }
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    public static void writeRestrictions(XmlSerializer serializer, Bundle restrictions, String tag) throws IOException {
        writeRestrictions(XmlUtils.makeTyped(serializer), restrictions, tag);
    }

    public static void writeRestrictions(TypedXmlSerializer serializer, Bundle restrictions, String tag) throws IOException {
        if (restrictions == null) {
            return;
        }
        serializer.startTag((String) null, tag);
        for (String key : restrictions.keySet()) {
            if (!NON_PERSIST_USER_RESTRICTIONS.contains(key)) {
                if (USER_RESTRICTIONS.contains(key)) {
                    if (restrictions.getBoolean(key)) {
                        serializer.attributeBoolean((String) null, key, true);
                    }
                } else {
                    Log.w(TAG, "Unknown user restriction detected: " + key);
                }
            }
        }
        serializer.endTag((String) null, tag);
    }

    public static void readRestrictions(XmlPullParser parser, Bundle restrictions) {
        readRestrictions(XmlUtils.makeTyped(parser), restrictions);
    }

    public static void readRestrictions(TypedXmlPullParser parser, Bundle restrictions) {
        restrictions.clear();
        for (String key : USER_RESTRICTIONS) {
            boolean value = parser.getAttributeBoolean((String) null, key, false);
            if (value) {
                restrictions.putBoolean(key, true);
            }
        }
    }

    public static Bundle readRestrictions(XmlPullParser parser) {
        return readRestrictions(XmlUtils.makeTyped(parser));
    }

    public static Bundle readRestrictions(TypedXmlPullParser parser) {
        Bundle result = new Bundle();
        readRestrictions(parser, result);
        return result;
    }

    public static Bundle nonNull(Bundle in) {
        return in != null ? in : new Bundle();
    }

    public static boolean contains(Bundle in, String restriction) {
        return in != null && in.getBoolean(restriction);
    }

    public static void merge(Bundle dest, Bundle in) {
        Objects.requireNonNull(dest);
        Preconditions.checkArgument(dest != in);
        if (in == null) {
            return;
        }
        for (String key : in.keySet()) {
            if (in.getBoolean(key, false)) {
                dest.putBoolean(key, true);
            }
        }
    }

    public static boolean canDeviceOwnerChange(String restriction) {
        return !IMMUTABLE_BY_OWNERS.contains(restriction);
    }

    public static boolean canProfileOwnerChange(String restriction, int userId) {
        return (IMMUTABLE_BY_OWNERS.contains(restriction) || DEVICE_OWNER_ONLY_RESTRICTIONS.contains(restriction) || (userId != 0 && PRIMARY_USER_ONLY_RESTRICTIONS.contains(restriction))) ? false : true;
    }

    public static boolean canProfileOwnerOfOrganizationOwnedDeviceChange(String restriction) {
        return PROFILE_OWNER_ORGANIZATION_OWNED_GLOBAL_RESTRICTIONS.contains(restriction) || PROFILE_OWNER_ORGANIZATION_OWNED_LOCAL_RESTRICTIONS.contains(restriction);
    }

    public static Set<String> getDefaultEnabledForManagedProfiles() {
        return DEFAULT_ENABLED_FOR_MANAGED_PROFILES;
    }

    public static boolean canFinancedDeviceOwnerChange(String restriction) {
        return FINANCED_DEVICE_OWNER_RESTRICTIONS.contains(restriction) && canDeviceOwnerChange(restriction);
    }

    public static boolean isGlobal(int restrictionOwnerType, String key) {
        return (restrictionOwnerType == 0 && (PRIMARY_USER_ONLY_RESTRICTIONS.contains(key) || GLOBAL_RESTRICTIONS.contains(key))) || (restrictionOwnerType == 2 && PROFILE_OWNER_ORGANIZATION_OWNED_GLOBAL_RESTRICTIONS.contains(key)) || PROFILE_GLOBAL_RESTRICTIONS.contains(key) || DEVICE_OWNER_ONLY_RESTRICTIONS.contains(key);
    }

    public static boolean isLocal(int restrictionOwnerType, String key) {
        return !isGlobal(restrictionOwnerType, key);
    }

    public static boolean areEqual(Bundle a, Bundle b) {
        if (a == b) {
            return true;
        }
        if (BundleUtils.isEmpty(a)) {
            return BundleUtils.isEmpty(b);
        }
        if (BundleUtils.isEmpty(b)) {
            return false;
        }
        for (String key : a.keySet()) {
            if (a.getBoolean(key) != b.getBoolean(key)) {
                return false;
            }
        }
        for (String key2 : b.keySet()) {
            if (a.getBoolean(key2) != b.getBoolean(key2)) {
                return false;
            }
        }
        return true;
    }

    public static void applyUserRestrictions(Context context, int userId, Bundle newRestrictions, Bundle prevRestrictions) {
        for (String key : USER_RESTRICTIONS) {
            boolean newValue = newRestrictions.getBoolean(key);
            boolean prevValue = prevRestrictions.getBoolean(key);
            if (newValue != prevValue) {
                applyUserRestriction(context, userId, key, newValue);
            }
        }
    }

    private static void applyUserRestriction(Context context, int userId, String key, boolean newValue) {
        ContentResolver cr = context.getContentResolver();
        long id = Binder.clearCallingIdentity();
        char c = 65535;
        try {
            int i = 1;
            switch (key.hashCode()) {
                case -1475388515:
                    if (key.equals("no_ambient_display")) {
                        c = '\t';
                        break;
                    }
                    break;
                case -1387500078:
                    if (key.equals("no_control_apps")) {
                        c = '\n';
                        break;
                    }
                    break;
                case -1315771401:
                    if (key.equals("ensure_verify_apps")) {
                        c = 3;
                        break;
                    }
                    break;
                case -1145953970:
                    if (key.equals("no_install_unknown_sources_globally")) {
                        c = 4;
                        break;
                    }
                    break;
                case -1082175374:
                    if (key.equals("no_airplane_mode")) {
                        c = '\b';
                        break;
                    }
                    break;
                case -6578707:
                    if (key.equals("no_uninstall_apps")) {
                        c = 11;
                        break;
                    }
                    break;
                case 387189153:
                    if (key.equals("no_install_unknown_sources")) {
                        c = 5;
                        break;
                    }
                    break;
                case 721128150:
                    if (key.equals("no_run_in_background")) {
                        c = 6;
                        break;
                    }
                    break;
                case 928851522:
                    if (key.equals("no_data_roaming")) {
                        c = 0;
                        break;
                    }
                    break;
                case 995816019:
                    if (key.equals("no_share_location")) {
                        c = 1;
                        break;
                    }
                    break;
                case 1095593830:
                    if (key.equals("no_safe_boot")) {
                        c = 7;
                        break;
                    }
                    break;
                case 1760762284:
                    if (key.equals("no_debugging_features")) {
                        c = 2;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    if (newValue) {
                        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(SubscriptionManager.class);
                        List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                        if (subscriptionInfoList != null) {
                            for (SubscriptionInfo subInfo : subscriptionInfoList) {
                                Settings.Global.putStringForUser(cr, "data_roaming" + subInfo.getSubscriptionId(), "0", userId);
                            }
                        }
                        Settings.Global.putStringForUser(cr, "data_roaming", "0", userId);
                        break;
                    }
                    break;
                case 1:
                    if (newValue) {
                        Settings.Secure.putIntForUser(cr, "location_mode", 0, userId);
                        break;
                    }
                    break;
                case 2:
                    if (newValue && userId == 0) {
                        Settings.Global.putStringForUser(cr, "adb_enabled", "0", userId);
                        Settings.Global.putStringForUser(cr, "adb_wifi_enabled", "0", userId);
                        break;
                    }
                    break;
                case 3:
                    if (newValue) {
                        Settings.Global.putStringForUser(context.getContentResolver(), "verifier_verify_adb_installs", "1", userId);
                        break;
                    }
                    break;
                case 4:
                    setInstallMarketAppsRestriction(cr, userId, getNewUserRestrictionSetting(context, userId, "no_install_unknown_sources", newValue));
                    break;
                case 5:
                    setInstallMarketAppsRestriction(cr, userId, getNewUserRestrictionSetting(context, userId, "no_install_unknown_sources_globally", newValue));
                    break;
                case 6:
                    if (newValue) {
                        int currentUser = ActivityManager.getCurrentUser();
                        if (currentUser != userId && userId != 0) {
                            try {
                                ActivityManager.getService().stopUser(userId, false, (IStopUserCallback) null);
                            } catch (RemoteException e) {
                                throw e.rethrowAsRuntimeException();
                            }
                        }
                        break;
                    }
                    break;
                case 7:
                    ContentResolver contentResolver = context.getContentResolver();
                    if (!newValue) {
                        i = 0;
                    }
                    Settings.Global.putInt(contentResolver, "safe_boot_disallowed", i);
                    break;
                case '\b':
                    if (newValue) {
                        if (Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 1) {
                            i = 0;
                        }
                        if (i != 0) {
                            Settings.Global.putInt(context.getContentResolver(), "airplane_mode_on", 0);
                            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
                            intent.putExtra("state", false);
                            context.sendBroadcastAsUser(intent, UserHandle.ALL);
                        }
                        break;
                    }
                    break;
                case '\t':
                    if (newValue) {
                        AmbientDisplayConfiguration config = new AmbientDisplayConfiguration(context);
                        config.disableDozeSettings(userId);
                        break;
                    }
                    break;
                case '\n':
                case 11:
                    PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                    pmi.removeAllNonSystemPackageSuspensions(userId);
                    pmi.removeAllDistractingPackageRestrictions(userId);
                    pmi.flushPackageRestrictions(userId);
                    break;
            }
        } finally {
            Binder.restoreCallingIdentity(id);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static boolean isSettingRestrictedForUser(Context context, String setting, int userId, String value, int callingUid) {
        char c;
        String restriction;
        Objects.requireNonNull(setting);
        UserManager mUserManager = (UserManager) context.getSystemService(UserManager.class);
        switch (setting.hashCode()) {
            case -2099894345:
                if (setting.equals("adb_wifi_enabled")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -1796809747:
                if (setting.equals("location_mode")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1490222856:
                if (setting.equals("doze_enabled")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case -1115710219:
                if (setting.equals("verifier_verify_adb_installs")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -970351711:
                if (setting.equals("adb_enabled")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -693072130:
                if (setting.equals("screen_brightness_mode")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case -623873498:
                if (setting.equals("always_on_vpn_app")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -416662510:
                if (setting.equals("preferred_network_mode")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -101820922:
                if (setting.equals("doze_always_on")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case -32505807:
                if (setting.equals("doze_pulse_on_long_press")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case -16943007:
                if (setting.equals("screen_brightness_float")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case 58027029:
                if (setting.equals("safe_boot_disallowed")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 258514750:
                if (setting.equals("screen_off_timeout")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case 683724341:
                if (setting.equals("private_dns_mode")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case 926123534:
                if (setting.equals("airplane_mode_on")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 1073289638:
                if (setting.equals("doze_pulse_on_double_tap")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 1223734380:
                if (setting.equals("private_dns_specifier")) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case 1275530062:
                if (setting.equals("auto_time_zone")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case 1334097968:
                if (setting.equals("always_on_vpn_lockdown_whitelist")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 1602982312:
                if (setting.equals("doze_pulse_on_pick_up")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case 1646894952:
                if (setting.equals("always_on_vpn_lockdown")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 1661297501:
                if (setting.equals("auto_time")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case 1701140351:
                if (setting.equals("install_non_market_apps")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1735689732:
                if (setting.equals("screen_brightness")) {
                    c = 16;
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
                if (!mUserManager.hasUserRestriction("no_config_location", UserHandle.of(userId)) || callingUid == 1000) {
                    if (!String.valueOf(0).equals(value)) {
                        restriction = "no_share_location";
                        break;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            case 1:
                if (!"0".equals(value)) {
                    restriction = "no_install_unknown_sources";
                    break;
                } else {
                    return false;
                }
            case 2:
            case 3:
                if (!"0".equals(value)) {
                    restriction = "no_debugging_features";
                    break;
                } else {
                    return false;
                }
            case 4:
                if (!"1".equals(value)) {
                    restriction = "ensure_verify_apps";
                    break;
                } else {
                    return false;
                }
            case 5:
                restriction = "no_config_mobile_networks";
                break;
            case 6:
            case 7:
            case '\b':
                int appId = UserHandle.getAppId(callingUid);
                if (appId != 1000 && appId != 0) {
                    restriction = "no_config_vpn";
                    break;
                } else {
                    return false;
                }
            case '\t':
                if (!"1".equals(value)) {
                    restriction = "no_safe_boot";
                    break;
                } else {
                    return false;
                }
            case '\n':
                if (!"0".equals(value)) {
                    restriction = "no_airplane_mode";
                    break;
                } else {
                    return false;
                }
            case 11:
            case '\f':
            case '\r':
            case 14:
            case 15:
                if (!"0".equals(value)) {
                    restriction = "no_ambient_display";
                    break;
                } else {
                    return false;
                }
            case 16:
            case 17:
            case 18:
                if (callingUid != 1000) {
                    restriction = "no_config_brightness";
                    break;
                } else {
                    return false;
                }
            case 19:
            case 20:
                if (callingUid != 1000) {
                    restriction = "no_config_date_time";
                    break;
                } else {
                    return false;
                }
            case 21:
                if (callingUid != 1000) {
                    restriction = "no_config_screen_timeout";
                    break;
                } else {
                    return false;
                }
            case 22:
            case 23:
                if (callingUid != 1000) {
                    restriction = "disallow_config_private_dns";
                    break;
                } else {
                    return false;
                }
            default:
                if (setting.startsWith("data_roaming") && !"0".equals(value)) {
                    restriction = "no_data_roaming";
                    break;
                } else {
                    return false;
                }
        }
        if (0 != 0) {
            return mUserManager.hasUserRestrictionOnAnyUser(restriction);
        }
        return mUserManager.hasUserRestriction(restriction, UserHandle.of(userId));
    }

    public static void dumpRestrictions(PrintWriter pw, String prefix, Bundle restrictions) {
        boolean noneSet = true;
        if (restrictions != null) {
            for (String key : restrictions.keySet()) {
                if (restrictions.getBoolean(key, false)) {
                    pw.println(prefix + key);
                    noneSet = false;
                }
            }
            if (noneSet) {
                pw.println(prefix + "none");
                return;
            }
            return;
        }
        pw.println(prefix + "null");
    }

    public static void moveRestriction(String restrictionKey, SparseArray<RestrictionsSet> sourceRestrictionsSets, RestrictionsSet destRestrictionSet) {
        for (int i = 0; i < sourceRestrictionsSets.size(); i++) {
            RestrictionsSet sourceRestrictionsSet = sourceRestrictionsSets.valueAt(i);
            sourceRestrictionsSet.moveRestriction(destRestrictionSet, restrictionKey);
        }
    }

    public static boolean restrictionsChanged(Bundle oldRestrictions, Bundle newRestrictions, String... restrictions) {
        if (restrictions.length == 0) {
            return areEqual(oldRestrictions, newRestrictions);
        }
        for (String restriction : restrictions) {
            if (oldRestrictions.getBoolean(restriction, false) != newRestrictions.getBoolean(restriction, false)) {
                return true;
            }
        }
        return false;
    }

    private static void setInstallMarketAppsRestriction(ContentResolver cr, int userId, int settingValue) {
        Settings.Secure.putIntForUser(cr, "install_non_market_apps", settingValue, userId);
    }

    private static int getNewUserRestrictionSetting(Context context, int userId, String userRestriction, boolean newValue) {
        return (newValue || UserManager.get(context).hasUserRestriction(userRestriction, UserHandle.of(userId))) ? 0 : 1;
    }
}
