package com.android.server.devicepolicy;

import android.app.admin.DeviceAdminInfo;
import android.app.admin.FactoryResetProtectionPolicy;
import android.app.admin.PasswordPolicy;
import android.app.admin.PreferentialNetworkServiceConfig;
import android.app.admin.WifiSsidPolicy;
import android.graphics.Color;
import android.net.wifi.WifiSsid;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.UserRestrictionsUtils;
import com.android.server.utils.Slogf;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ActiveAdmin {
    private static final String ATTR_LAST_NETWORK_LOGGING_NOTIFICATION = "last-notification";
    private static final String ATTR_NUM_NETWORK_LOGGING_NOTIFICATIONS = "num-notifications";
    private static final String ATTR_VALUE = "value";
    static final int DEF_KEYGUARD_FEATURES_DISABLED = 0;
    static final int DEF_MAXIMUM_FAILED_PASSWORDS_FOR_WIPE = 0;
    static final int DEF_MAXIMUM_NETWORK_LOGGING_NOTIFICATIONS_SHOWN = 2;
    static final long DEF_MAXIMUM_TIME_TO_UNLOCK = 0;
    static final int DEF_ORGANIZATION_COLOR = Color.parseColor("#00796B");
    static final long DEF_PASSWORD_EXPIRATION_DATE = 0;
    static final long DEF_PASSWORD_EXPIRATION_TIMEOUT = 0;
    static final int DEF_PASSWORD_HISTORY_LENGTH = 0;
    private static final String TAG_ACCOUNT_TYPE = "account-type";
    private static final String TAG_ADMIN_CAN_GRANT_SENSORS_PERMISSIONS = "admin-can-grant-sensors-permissions";
    private static final String TAG_ALWAYS_ON_VPN_LOCKDOWN = "vpn-lockdown";
    private static final String TAG_ALWAYS_ON_VPN_PACKAGE = "vpn-package";
    private static final String TAG_COMMON_CRITERIA_MODE = "common-criteria-mode";
    private static final String TAG_CROSS_PROFILE_CALENDAR_PACKAGES = "cross-profile-calendar-packages";
    private static final String TAG_CROSS_PROFILE_CALENDAR_PACKAGES_NULL = "cross-profile-calendar-packages-null";
    private static final String TAG_CROSS_PROFILE_PACKAGES = "cross-profile-packages";
    private static final String TAG_CROSS_PROFILE_WIDGET_PROVIDERS = "cross-profile-widget-providers";
    private static final String TAG_DEFAULT_ENABLED_USER_RESTRICTIONS = "default-enabled-user-restrictions";
    private static final String TAG_DISABLE_ACCOUNT_MANAGEMENT = "disable-account-management";
    private static final String TAG_DISABLE_BLUETOOTH_CONTACT_SHARING = "disable-bt-contacts-sharing";
    private static final String TAG_DISABLE_CALLER_ID = "disable-caller-id";
    private static final String TAG_DISABLE_CAMERA = "disable-camera";
    private static final String TAG_DISABLE_CONTACTS_SEARCH = "disable-contacts-search";
    private static final String TAG_DISABLE_KEYGUARD_FEATURES = "disable-keyguard-features";
    private static final String TAG_DISABLE_SCREEN_CAPTURE = "disable-screen-capture";
    private static final String TAG_ENCRYPTION_REQUESTED = "encryption-requested";
    private static final String TAG_END_USER_SESSION_MESSAGE = "end_user_session_message";
    private static final String TAG_ENROLLMENT_SPECIFIC_ID = "enrollment-specific-id";
    private static final String TAG_FACTORY_RESET_PROTECTION_POLICY = "factory_reset_protection_policy";
    private static final String TAG_FORCE_EPHEMERAL_USERS = "force_ephemeral_users";
    private static final String TAG_GLOBAL_PROXY_EXCLUSION_LIST = "global-proxy-exclusion-list";
    private static final String TAG_GLOBAL_PROXY_SPEC = "global-proxy-spec";
    private static final String TAG_IS_LOGOUT_ENABLED = "is_logout_enabled";
    private static final String TAG_IS_NETWORK_LOGGING_ENABLED = "is_network_logging_enabled";
    private static final String TAG_KEEP_UNINSTALLED_PACKAGES = "keep-uninstalled-packages";
    private static final String TAG_LONG_SUPPORT_MESSAGE = "long-support-message";
    private static final String TAG_MANAGE_TRUST_AGENT_FEATURES = "manage-trust-agent-features";
    private static final String TAG_MAX_FAILED_PASSWORD_WIPE = "max-failed-password-wipe";
    private static final String TAG_MAX_TIME_TO_UNLOCK = "max-time-to-unlock";
    private static final String TAG_METERED_DATA_DISABLED_PACKAGES = "metered_data_disabled_packages";
    private static final String TAG_MIN_PASSWORD_LENGTH = "min-password-length";
    private static final String TAG_MIN_PASSWORD_LETTERS = "min-password-letters";
    private static final String TAG_MIN_PASSWORD_LOWERCASE = "min-password-lowercase";
    private static final String TAG_MIN_PASSWORD_NONLETTER = "min-password-nonletter";
    private static final String TAG_MIN_PASSWORD_NUMERIC = "min-password-numeric";
    private static final String TAG_MIN_PASSWORD_SYMBOLS = "min-password-symbols";
    private static final String TAG_MIN_PASSWORD_UPPERCASE = "min-password-uppercase";
    private static final String TAG_NEARBY_APP_STREAMING_POLICY = "nearby-app-streaming-policy";
    private static final String TAG_NEARBY_NOTIFICATION_STREAMING_POLICY = "nearby-notification-streaming-policy";
    private static final String TAG_ORGANIZATION_COLOR = "organization-color";
    private static final String TAG_ORGANIZATION_ID = "organization-id";
    private static final String TAG_ORGANIZATION_NAME = "organization-name";
    private static final String TAG_PACKAGE_LIST_ITEM = "item";
    private static final String TAG_PARENT_ADMIN = "parent-admin";
    private static final String TAG_PASSWORD_COMPLEXITY = "password-complexity";
    private static final String TAG_PASSWORD_EXPIRATION_DATE = "password-expiration-date";
    private static final String TAG_PASSWORD_EXPIRATION_TIMEOUT = "password-expiration-timeout";
    private static final String TAG_PASSWORD_HISTORY_LENGTH = "password-history-length";
    private static final String TAG_PASSWORD_QUALITY = "password-quality";
    private static final String TAG_PERMITTED_ACCESSIBILITY_SERVICES = "permitted-accessiblity-services";
    private static final String TAG_PERMITTED_IMES = "permitted-imes";
    private static final String TAG_PERMITTED_NOTIFICATION_LISTENERS = "permitted-notification-listeners";
    private static final String TAG_POLICIES = "policies";
    private static final String TAG_PREFERENTIAL_NETWORK_SERVICE_CONFIG = "preferential_network_service_config";
    private static final String TAG_PREFERENTIAL_NETWORK_SERVICE_CONFIGS = "preferential_network_service_configs";
    private static final String TAG_PREFERENTIAL_NETWORK_SERVICE_ENABLED = "preferential-network-service-enabled";
    private static final String TAG_PROFILE_MAXIMUM_TIME_OFF = "profile-max-time-off";
    private static final String TAG_PROFILE_OFF_DEADLINE = "profile-off-deadline";
    private static final String TAG_PROTECTED_PACKAGES = "protected_packages";
    private static final String TAG_PROVIDER = "provider";
    private static final String TAG_REQUIRE_AUTO_TIME = "require_auto_time";
    private static final String TAG_RESTRICTION = "restriction";
    private static final String TAG_SHORT_SUPPORT_MESSAGE = "short-support-message";
    private static final String TAG_SPECIFIES_GLOBAL_PROXY = "specifies-global-proxy";
    private static final String TAG_SSID = "ssid";
    private static final String TAG_SSID_ALLOWLIST = "ssid-allowlist";
    private static final String TAG_SSID_DENYLIST = "ssid-denylist";
    private static final String TAG_START_USER_SESSION_MESSAGE = "start_user_session_message";
    private static final String TAG_STRONG_AUTH_UNLOCK_TIMEOUT = "strong-auth-unlock-timeout";
    private static final String TAG_SUSPEND_PERSONAL_APPS = "suspend-personal-apps";
    private static final String TAG_TEST_ONLY_ADMIN = "test-only-admin";
    private static final String TAG_TRUST_AGENT_COMPONENT = "component";
    private static final String TAG_TRUST_AGENT_COMPONENT_OPTIONS = "trust-agent-component-options";
    private static final String TAG_USB_DATA_SIGNALING = "usb-data-signaling";
    private static final String TAG_USER_RESTRICTIONS = "user-restrictions";
    private static final String TAG_WIFI_MIN_SECURITY = "wifi-min-security";
    private static final boolean USB_DATA_SIGNALING_ENABLED_DEFAULT = true;
    List<String> crossProfileWidgetProviders;
    DeviceAdminInfo info;
    final boolean isParent;
    List<String> keepUninstalledPackages;
    public boolean mAdminCanGrantSensorsPermissions;
    public boolean mAlwaysOnVpnLockdown;
    public String mAlwaysOnVpnPackage;
    boolean mCommonCriteriaMode;
    public String mEnrollmentSpecificId;
    public String mOrganizationId;
    WifiSsidPolicy mWifiSsidPolicy;
    List<String> meteredDisabledPackages;
    ActiveAdmin parentAdmin;
    List<String> permittedAccessiblityServices;
    List<String> permittedInputMethods;
    List<String> permittedNotificationListeners;
    List<String> protectedPackages;
    Bundle userRestrictions;
    int passwordHistoryLength = 0;
    PasswordPolicy mPasswordPolicy = new PasswordPolicy();
    int mPasswordComplexity = 0;
    int mNearbyNotificationStreamingPolicy = 3;
    int mNearbyAppStreamingPolicy = 3;
    FactoryResetProtectionPolicy mFactoryResetProtectionPolicy = null;
    long maximumTimeToUnlock = 0;
    long strongAuthUnlockTimeout = 0;
    int maximumFailedPasswordsForWipe = 0;
    long passwordExpirationTimeout = 0;
    long passwordExpirationDate = 0;
    int disabledKeyguardFeatures = 0;
    boolean encryptionRequested = false;
    boolean testOnlyAdmin = false;
    boolean disableCamera = false;
    boolean disableCallerId = false;
    boolean disableContactsSearch = false;
    boolean disableBluetoothContactSharing = true;
    boolean disableScreenCapture = false;
    boolean requireAutoTime = false;
    boolean forceEphemeralUsers = false;
    boolean isNetworkLoggingEnabled = false;
    boolean isLogoutEnabled = false;
    int numNetworkLoggingNotifications = 0;
    long lastNetworkLoggingNotificationTimeMs = 0;
    final Set<String> accountTypesWithManagementDisabled = new ArraySet();
    boolean specifiesGlobalProxy = false;
    String globalProxySpec = null;
    String globalProxyExclusionList = null;
    ArrayMap<String, TrustAgentInfo> trustAgentInfos = new ArrayMap<>();
    final Set<String> defaultEnabledRestrictionsAlreadySet = new ArraySet();
    CharSequence shortSupportMessage = null;
    CharSequence longSupportMessage = null;
    int organizationColor = DEF_ORGANIZATION_COLOR;
    String organizationName = null;
    String startUserSessionMessage = null;
    String endUserSessionMessage = null;
    List<String> mCrossProfileCalendarPackages = Collections.emptyList();
    List<String> mCrossProfilePackages = Collections.emptyList();
    boolean mSuspendPersonalApps = false;
    long mProfileMaximumTimeOffMillis = 0;
    long mProfileOffDeadline = 0;
    public boolean mPreferentialNetworkServiceEnabled = false;
    public List<PreferentialNetworkServiceConfig> mPreferentialNetworkServiceConfigs = List.of(PreferentialNetworkServiceConfig.DEFAULT);
    boolean mUsbDataSignalingEnabled = true;
    int mWifiMinimumSecurityLevel = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class TrustAgentInfo {
        public PersistableBundle options;

        /* JADX INFO: Access modifiers changed from: package-private */
        public TrustAgentInfo(PersistableBundle bundle) {
            this.options = bundle;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActiveAdmin(DeviceAdminInfo info, boolean isParent) {
        this.info = info;
        this.isParent = isParent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActiveAdmin getParentActiveAdmin() {
        Preconditions.checkState(!this.isParent);
        if (this.parentAdmin == null) {
            this.parentAdmin = new ActiveAdmin(this.info, true);
        }
        return this.parentAdmin;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasParentActiveAdmin() {
        return this.parentAdmin != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUid() {
        return this.info.getActivityInfo().applicationInfo.uid;
    }

    public UserHandle getUserHandle() {
        return UserHandle.of(UserHandle.getUserId(this.info.getActivityInfo().applicationInfo.uid));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeToXml(TypedXmlSerializer out) throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag((String) null, TAG_POLICIES);
        this.info.writePoliciesToXml(out);
        out.endTag((String) null, TAG_POLICIES);
        if (this.mPasswordPolicy.quality != 0) {
            writeAttributeValueToXml(out, TAG_PASSWORD_QUALITY, this.mPasswordPolicy.quality);
            if (this.mPasswordPolicy.length != 0) {
                writeAttributeValueToXml(out, TAG_MIN_PASSWORD_LENGTH, this.mPasswordPolicy.length);
            }
            if (this.mPasswordPolicy.upperCase != 0) {
                writeAttributeValueToXml(out, TAG_MIN_PASSWORD_UPPERCASE, this.mPasswordPolicy.upperCase);
            }
            if (this.mPasswordPolicy.lowerCase != 0) {
                writeAttributeValueToXml(out, TAG_MIN_PASSWORD_LOWERCASE, this.mPasswordPolicy.lowerCase);
            }
            if (this.mPasswordPolicy.letters != 1) {
                writeAttributeValueToXml(out, TAG_MIN_PASSWORD_LETTERS, this.mPasswordPolicy.letters);
            }
            if (this.mPasswordPolicy.numeric != 1) {
                writeAttributeValueToXml(out, TAG_MIN_PASSWORD_NUMERIC, this.mPasswordPolicy.numeric);
            }
            if (this.mPasswordPolicy.symbols != 1) {
                writeAttributeValueToXml(out, TAG_MIN_PASSWORD_SYMBOLS, this.mPasswordPolicy.symbols);
            }
            if (this.mPasswordPolicy.nonLetter > 0) {
                writeAttributeValueToXml(out, TAG_MIN_PASSWORD_NONLETTER, this.mPasswordPolicy.nonLetter);
            }
        }
        int i = this.passwordHistoryLength;
        if (i != 0) {
            writeAttributeValueToXml(out, TAG_PASSWORD_HISTORY_LENGTH, i);
        }
        long j = this.maximumTimeToUnlock;
        if (j != 0) {
            writeAttributeValueToXml(out, TAG_MAX_TIME_TO_UNLOCK, j);
        }
        long j2 = this.strongAuthUnlockTimeout;
        if (j2 != 259200000) {
            writeAttributeValueToXml(out, TAG_STRONG_AUTH_UNLOCK_TIMEOUT, j2);
        }
        int i2 = this.maximumFailedPasswordsForWipe;
        if (i2 != 0) {
            writeAttributeValueToXml(out, TAG_MAX_FAILED_PASSWORD_WIPE, i2);
        }
        boolean z = this.specifiesGlobalProxy;
        if (z) {
            writeAttributeValueToXml(out, TAG_SPECIFIES_GLOBAL_PROXY, z);
            String str = this.globalProxySpec;
            if (str != null) {
                writeAttributeValueToXml(out, TAG_GLOBAL_PROXY_SPEC, str);
            }
            String str2 = this.globalProxyExclusionList;
            if (str2 != null) {
                writeAttributeValueToXml(out, TAG_GLOBAL_PROXY_EXCLUSION_LIST, str2);
            }
        }
        long j3 = this.passwordExpirationTimeout;
        if (j3 != 0) {
            writeAttributeValueToXml(out, TAG_PASSWORD_EXPIRATION_TIMEOUT, j3);
        }
        long j4 = this.passwordExpirationDate;
        if (j4 != 0) {
            writeAttributeValueToXml(out, TAG_PASSWORD_EXPIRATION_DATE, j4);
        }
        boolean z2 = this.encryptionRequested;
        if (z2) {
            writeAttributeValueToXml(out, TAG_ENCRYPTION_REQUESTED, z2);
        }
        boolean z3 = this.testOnlyAdmin;
        if (z3) {
            writeAttributeValueToXml(out, TAG_TEST_ONLY_ADMIN, z3);
        }
        boolean z4 = this.disableCamera;
        if (z4) {
            writeAttributeValueToXml(out, TAG_DISABLE_CAMERA, z4);
        }
        boolean z5 = this.disableCallerId;
        if (z5) {
            writeAttributeValueToXml(out, TAG_DISABLE_CALLER_ID, z5);
        }
        boolean z6 = this.disableContactsSearch;
        if (z6) {
            writeAttributeValueToXml(out, TAG_DISABLE_CONTACTS_SEARCH, z6);
        }
        boolean z7 = this.disableBluetoothContactSharing;
        if (!z7) {
            writeAttributeValueToXml(out, TAG_DISABLE_BLUETOOTH_CONTACT_SHARING, z7);
        }
        boolean z8 = this.disableScreenCapture;
        if (z8) {
            writeAttributeValueToXml(out, TAG_DISABLE_SCREEN_CAPTURE, z8);
        }
        boolean z9 = this.requireAutoTime;
        if (z9) {
            writeAttributeValueToXml(out, TAG_REQUIRE_AUTO_TIME, z9);
        }
        boolean z10 = this.forceEphemeralUsers;
        if (z10) {
            writeAttributeValueToXml(out, TAG_FORCE_EPHEMERAL_USERS, z10);
        }
        if (this.isNetworkLoggingEnabled) {
            out.startTag((String) null, TAG_IS_NETWORK_LOGGING_ENABLED);
            out.attributeBoolean((String) null, ATTR_VALUE, this.isNetworkLoggingEnabled);
            out.attributeInt((String) null, ATTR_NUM_NETWORK_LOGGING_NOTIFICATIONS, this.numNetworkLoggingNotifications);
            out.attributeLong((String) null, ATTR_LAST_NETWORK_LOGGING_NOTIFICATION, this.lastNetworkLoggingNotificationTimeMs);
            out.endTag((String) null, TAG_IS_NETWORK_LOGGING_ENABLED);
        }
        int i3 = this.disabledKeyguardFeatures;
        if (i3 != 0) {
            writeAttributeValueToXml(out, TAG_DISABLE_KEYGUARD_FEATURES, i3);
        }
        if (!this.accountTypesWithManagementDisabled.isEmpty()) {
            writeAttributeValuesToXml(out, TAG_DISABLE_ACCOUNT_MANAGEMENT, TAG_ACCOUNT_TYPE, this.accountTypesWithManagementDisabled);
        }
        if (!this.trustAgentInfos.isEmpty()) {
            Set<Map.Entry<String, TrustAgentInfo>> set = this.trustAgentInfos.entrySet();
            out.startTag((String) null, TAG_MANAGE_TRUST_AGENT_FEATURES);
            for (Map.Entry<String, TrustAgentInfo> entry : set) {
                TrustAgentInfo trustAgentInfo = entry.getValue();
                out.startTag((String) null, TAG_TRUST_AGENT_COMPONENT);
                out.attribute((String) null, ATTR_VALUE, entry.getKey());
                if (trustAgentInfo.options != null) {
                    out.startTag((String) null, TAG_TRUST_AGENT_COMPONENT_OPTIONS);
                    try {
                        trustAgentInfo.options.saveToXml(out);
                    } catch (XmlPullParserException e) {
                        Slogf.e("DevicePolicyManager", e, "Failed to save TrustAgent options", new Object[0]);
                    }
                    out.endTag((String) null, TAG_TRUST_AGENT_COMPONENT_OPTIONS);
                }
                out.endTag((String) null, TAG_TRUST_AGENT_COMPONENT);
            }
            out.endTag((String) null, TAG_MANAGE_TRUST_AGENT_FEATURES);
        }
        List<String> list = this.crossProfileWidgetProviders;
        if (list != null && !list.isEmpty()) {
            writeAttributeValuesToXml(out, TAG_CROSS_PROFILE_WIDGET_PROVIDERS, TAG_PROVIDER, this.crossProfileWidgetProviders);
        }
        writePackageListToXml(out, TAG_PERMITTED_ACCESSIBILITY_SERVICES, this.permittedAccessiblityServices);
        writePackageListToXml(out, TAG_PERMITTED_IMES, this.permittedInputMethods);
        writePackageListToXml(out, TAG_PERMITTED_NOTIFICATION_LISTENERS, this.permittedNotificationListeners);
        writePackageListToXml(out, TAG_KEEP_UNINSTALLED_PACKAGES, this.keepUninstalledPackages);
        writePackageListToXml(out, TAG_METERED_DATA_DISABLED_PACKAGES, this.meteredDisabledPackages);
        writePackageListToXml(out, TAG_PROTECTED_PACKAGES, this.protectedPackages);
        if (hasUserRestrictions()) {
            UserRestrictionsUtils.writeRestrictions(out, this.userRestrictions, TAG_USER_RESTRICTIONS);
        }
        if (!this.defaultEnabledRestrictionsAlreadySet.isEmpty()) {
            writeAttributeValuesToXml(out, TAG_DEFAULT_ENABLED_USER_RESTRICTIONS, TAG_RESTRICTION, this.defaultEnabledRestrictionsAlreadySet);
        }
        if (!TextUtils.isEmpty(this.shortSupportMessage)) {
            writeTextToXml(out, TAG_SHORT_SUPPORT_MESSAGE, this.shortSupportMessage.toString());
        }
        if (!TextUtils.isEmpty(this.longSupportMessage)) {
            writeTextToXml(out, TAG_LONG_SUPPORT_MESSAGE, this.longSupportMessage.toString());
        }
        if (this.parentAdmin != null) {
            out.startTag((String) null, TAG_PARENT_ADMIN);
            this.parentAdmin.writeToXml(out);
            out.endTag((String) null, TAG_PARENT_ADMIN);
        }
        int i4 = this.organizationColor;
        if (i4 != DEF_ORGANIZATION_COLOR) {
            writeAttributeValueToXml(out, TAG_ORGANIZATION_COLOR, i4);
        }
        String str3 = this.organizationName;
        if (str3 != null) {
            writeTextToXml(out, TAG_ORGANIZATION_NAME, str3);
        }
        boolean z11 = this.isLogoutEnabled;
        if (z11) {
            writeAttributeValueToXml(out, TAG_IS_LOGOUT_ENABLED, z11);
        }
        String str4 = this.startUserSessionMessage;
        if (str4 != null) {
            writeTextToXml(out, TAG_START_USER_SESSION_MESSAGE, str4);
        }
        String str5 = this.endUserSessionMessage;
        if (str5 != null) {
            writeTextToXml(out, TAG_END_USER_SESSION_MESSAGE, str5);
        }
        List<String> list2 = this.mCrossProfileCalendarPackages;
        if (list2 != null) {
            writePackageListToXml(out, TAG_CROSS_PROFILE_CALENDAR_PACKAGES, list2);
        } else {
            out.startTag((String) null, TAG_CROSS_PROFILE_CALENDAR_PACKAGES_NULL);
            out.endTag((String) null, TAG_CROSS_PROFILE_CALENDAR_PACKAGES_NULL);
        }
        writePackageListToXml(out, TAG_CROSS_PROFILE_PACKAGES, this.mCrossProfilePackages);
        if (this.mFactoryResetProtectionPolicy != null) {
            out.startTag((String) null, TAG_FACTORY_RESET_PROTECTION_POLICY);
            this.mFactoryResetProtectionPolicy.writeToXml(out);
            out.endTag((String) null, TAG_FACTORY_RESET_PROTECTION_POLICY);
        }
        boolean z12 = this.mSuspendPersonalApps;
        if (z12) {
            writeAttributeValueToXml(out, TAG_SUSPEND_PERSONAL_APPS, z12);
        }
        long j5 = this.mProfileMaximumTimeOffMillis;
        if (j5 != 0) {
            writeAttributeValueToXml(out, TAG_PROFILE_MAXIMUM_TIME_OFF, j5);
        }
        if (this.mProfileMaximumTimeOffMillis != 0) {
            writeAttributeValueToXml(out, TAG_PROFILE_OFF_DEADLINE, this.mProfileOffDeadline);
        }
        if (!TextUtils.isEmpty(this.mAlwaysOnVpnPackage)) {
            writeAttributeValueToXml(out, TAG_ALWAYS_ON_VPN_PACKAGE, this.mAlwaysOnVpnPackage);
        }
        boolean z13 = this.mAlwaysOnVpnLockdown;
        if (z13) {
            writeAttributeValueToXml(out, TAG_ALWAYS_ON_VPN_LOCKDOWN, z13);
        }
        boolean z14 = this.mCommonCriteriaMode;
        if (z14) {
            writeAttributeValueToXml(out, TAG_COMMON_CRITERIA_MODE, z14);
        }
        int i5 = this.mPasswordComplexity;
        if (i5 != 0) {
            writeAttributeValueToXml(out, TAG_PASSWORD_COMPLEXITY, i5);
        }
        int i6 = this.mNearbyNotificationStreamingPolicy;
        if (i6 != 3) {
            writeAttributeValueToXml(out, TAG_NEARBY_NOTIFICATION_STREAMING_POLICY, i6);
        }
        int i7 = this.mNearbyAppStreamingPolicy;
        if (i7 != 3) {
            writeAttributeValueToXml(out, TAG_NEARBY_APP_STREAMING_POLICY, i7);
        }
        if (!TextUtils.isEmpty(this.mOrganizationId)) {
            writeTextToXml(out, TAG_ORGANIZATION_ID, this.mOrganizationId);
        }
        if (!TextUtils.isEmpty(this.mEnrollmentSpecificId)) {
            writeTextToXml(out, TAG_ENROLLMENT_SPECIFIC_ID, this.mEnrollmentSpecificId);
        }
        writeAttributeValueToXml(out, TAG_ADMIN_CAN_GRANT_SENSORS_PERMISSIONS, this.mAdminCanGrantSensorsPermissions);
        boolean z15 = this.mUsbDataSignalingEnabled;
        if (!z15) {
            writeAttributeValueToXml(out, TAG_USB_DATA_SIGNALING, z15);
        }
        int i8 = this.mWifiMinimumSecurityLevel;
        if (i8 != 0) {
            writeAttributeValueToXml(out, TAG_WIFI_MIN_SECURITY, i8);
        }
        WifiSsidPolicy wifiSsidPolicy = this.mWifiSsidPolicy;
        if (wifiSsidPolicy != null) {
            List<String> ssids = ssidsToStrings(wifiSsidPolicy.getSsids());
            if (this.mWifiSsidPolicy.getPolicyType() == 0) {
                writeAttributeValuesToXml(out, TAG_SSID_ALLOWLIST, TAG_SSID, ssids);
            } else if (this.mWifiSsidPolicy.getPolicyType() == 1) {
                writeAttributeValuesToXml(out, TAG_SSID_DENYLIST, TAG_SSID, ssids);
            }
        }
        if (!this.mPreferentialNetworkServiceConfigs.isEmpty()) {
            out.startTag((String) null, TAG_PREFERENTIAL_NETWORK_SERVICE_CONFIGS);
            for (PreferentialNetworkServiceConfig config : this.mPreferentialNetworkServiceConfigs) {
                config.writeToXml(out);
            }
            out.endTag((String) null, TAG_PREFERENTIAL_NETWORK_SERVICE_CONFIGS);
        }
    }

    private List<String> ssidsToStrings(Set<WifiSsid> ssids) {
        return (List) ssids.stream().map(new Function() { // from class: com.android.server.devicepolicy.ActiveAdmin$$ExternalSyntheticLambda2
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ActiveAdmin.lambda$ssidsToStrings$0((WifiSsid) obj);
            }
        }).collect(Collectors.toList());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String lambda$ssidsToStrings$0(WifiSsid ssid) {
        return new String(ssid.getBytes(), StandardCharsets.UTF_8);
    }

    void writeTextToXml(TypedXmlSerializer out, String tag, String text) throws IOException {
        out.startTag((String) null, tag);
        out.text(text);
        out.endTag((String) null, tag);
    }

    void writePackageListToXml(TypedXmlSerializer out, String outerTag, List<String> packageList) throws IllegalArgumentException, IllegalStateException, IOException {
        if (packageList == null) {
            return;
        }
        writeAttributeValuesToXml(out, outerTag, "item", packageList);
    }

    void writeAttributeValueToXml(TypedXmlSerializer out, String tag, String value) throws IOException {
        out.startTag((String) null, tag);
        out.attribute((String) null, ATTR_VALUE, value);
        out.endTag((String) null, tag);
    }

    void writeAttributeValueToXml(TypedXmlSerializer out, String tag, int value) throws IOException {
        out.startTag((String) null, tag);
        out.attributeInt((String) null, ATTR_VALUE, value);
        out.endTag((String) null, tag);
    }

    void writeAttributeValueToXml(TypedXmlSerializer out, String tag, long value) throws IOException {
        out.startTag((String) null, tag);
        out.attributeLong((String) null, ATTR_VALUE, value);
        out.endTag((String) null, tag);
    }

    void writeAttributeValueToXml(TypedXmlSerializer out, String tag, boolean value) throws IOException {
        out.startTag((String) null, tag);
        out.attributeBoolean((String) null, ATTR_VALUE, value);
        out.endTag((String) null, tag);
    }

    void writeAttributeValuesToXml(TypedXmlSerializer out, String outerTag, String innerTag, Collection<String> values) throws IOException {
        out.startTag((String) null, outerTag);
        for (String value : values) {
            out.startTag((String) null, innerTag);
            out.attribute((String) null, ATTR_VALUE, value);
            out.endTag((String) null, innerTag);
        }
        out.endTag((String) null, outerTag);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void readFromXml(TypedXmlPullParser parser, boolean shouldOverridePolicies) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tag = parser.getName();
                        if (TAG_POLICIES.equals(tag)) {
                            if (shouldOverridePolicies) {
                                Slogf.d("DevicePolicyManager", "Overriding device admin policies from XML.");
                                this.info.readPoliciesFromXml(parser);
                            }
                        } else if (TAG_PASSWORD_QUALITY.equals(tag)) {
                            this.mPasswordPolicy.quality = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_MIN_PASSWORD_LENGTH.equals(tag)) {
                            this.mPasswordPolicy.length = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_PASSWORD_HISTORY_LENGTH.equals(tag)) {
                            this.passwordHistoryLength = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_MIN_PASSWORD_UPPERCASE.equals(tag)) {
                            this.mPasswordPolicy.upperCase = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_MIN_PASSWORD_LOWERCASE.equals(tag)) {
                            this.mPasswordPolicy.lowerCase = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_MIN_PASSWORD_LETTERS.equals(tag)) {
                            this.mPasswordPolicy.letters = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_MIN_PASSWORD_NUMERIC.equals(tag)) {
                            this.mPasswordPolicy.numeric = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_MIN_PASSWORD_SYMBOLS.equals(tag)) {
                            this.mPasswordPolicy.symbols = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_MIN_PASSWORD_NONLETTER.equals(tag)) {
                            this.mPasswordPolicy.nonLetter = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_MAX_TIME_TO_UNLOCK.equals(tag)) {
                            this.maximumTimeToUnlock = parser.getAttributeLong((String) null, ATTR_VALUE);
                        } else if (TAG_STRONG_AUTH_UNLOCK_TIMEOUT.equals(tag)) {
                            this.strongAuthUnlockTimeout = parser.getAttributeLong((String) null, ATTR_VALUE);
                        } else if (TAG_MAX_FAILED_PASSWORD_WIPE.equals(tag)) {
                            this.maximumFailedPasswordsForWipe = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_SPECIFIES_GLOBAL_PROXY.equals(tag)) {
                            this.specifiesGlobalProxy = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_GLOBAL_PROXY_SPEC.equals(tag)) {
                            this.globalProxySpec = parser.getAttributeValue((String) null, ATTR_VALUE);
                        } else if (TAG_GLOBAL_PROXY_EXCLUSION_LIST.equals(tag)) {
                            this.globalProxyExclusionList = parser.getAttributeValue((String) null, ATTR_VALUE);
                        } else if (TAG_PASSWORD_EXPIRATION_TIMEOUT.equals(tag)) {
                            this.passwordExpirationTimeout = parser.getAttributeLong((String) null, ATTR_VALUE);
                        } else if (TAG_PASSWORD_EXPIRATION_DATE.equals(tag)) {
                            this.passwordExpirationDate = parser.getAttributeLong((String) null, ATTR_VALUE);
                        } else if (TAG_ENCRYPTION_REQUESTED.equals(tag)) {
                            this.encryptionRequested = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_TEST_ONLY_ADMIN.equals(tag)) {
                            this.testOnlyAdmin = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_DISABLE_CAMERA.equals(tag)) {
                            this.disableCamera = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_DISABLE_CALLER_ID.equals(tag)) {
                            this.disableCallerId = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_DISABLE_CONTACTS_SEARCH.equals(tag)) {
                            this.disableContactsSearch = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_DISABLE_BLUETOOTH_CONTACT_SHARING.equals(tag)) {
                            this.disableBluetoothContactSharing = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_DISABLE_SCREEN_CAPTURE.equals(tag)) {
                            this.disableScreenCapture = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_REQUIRE_AUTO_TIME.equals(tag)) {
                            this.requireAutoTime = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_FORCE_EPHEMERAL_USERS.equals(tag)) {
                            this.forceEphemeralUsers = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_IS_NETWORK_LOGGING_ENABLED.equals(tag)) {
                            this.isNetworkLoggingEnabled = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                            this.lastNetworkLoggingNotificationTimeMs = parser.getAttributeLong((String) null, ATTR_LAST_NETWORK_LOGGING_NOTIFICATION);
                            this.numNetworkLoggingNotifications = parser.getAttributeInt((String) null, ATTR_NUM_NETWORK_LOGGING_NOTIFICATIONS);
                        } else if (TAG_DISABLE_KEYGUARD_FEATURES.equals(tag)) {
                            this.disabledKeyguardFeatures = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_DISABLE_ACCOUNT_MANAGEMENT.equals(tag)) {
                            readAttributeValues(parser, TAG_ACCOUNT_TYPE, this.accountTypesWithManagementDisabled);
                        } else if (TAG_MANAGE_TRUST_AGENT_FEATURES.equals(tag)) {
                            this.trustAgentInfos = getAllTrustAgentInfos(parser, tag);
                        } else if (TAG_CROSS_PROFILE_WIDGET_PROVIDERS.equals(tag)) {
                            ArrayList arrayList = new ArrayList();
                            this.crossProfileWidgetProviders = arrayList;
                            readAttributeValues(parser, TAG_PROVIDER, arrayList);
                        } else if (TAG_PERMITTED_ACCESSIBILITY_SERVICES.equals(tag)) {
                            this.permittedAccessiblityServices = readPackageList(parser, tag);
                        } else if (TAG_PERMITTED_IMES.equals(tag)) {
                            this.permittedInputMethods = readPackageList(parser, tag);
                        } else if (TAG_PERMITTED_NOTIFICATION_LISTENERS.equals(tag)) {
                            this.permittedNotificationListeners = readPackageList(parser, tag);
                        } else if (TAG_KEEP_UNINSTALLED_PACKAGES.equals(tag)) {
                            this.keepUninstalledPackages = readPackageList(parser, tag);
                        } else if (TAG_METERED_DATA_DISABLED_PACKAGES.equals(tag)) {
                            this.meteredDisabledPackages = readPackageList(parser, tag);
                        } else if (TAG_PROTECTED_PACKAGES.equals(tag)) {
                            this.protectedPackages = readPackageList(parser, tag);
                        } else if (TAG_USER_RESTRICTIONS.equals(tag)) {
                            this.userRestrictions = UserRestrictionsUtils.readRestrictions(parser);
                        } else if (TAG_DEFAULT_ENABLED_USER_RESTRICTIONS.equals(tag)) {
                            readAttributeValues(parser, TAG_RESTRICTION, this.defaultEnabledRestrictionsAlreadySet);
                        } else if (TAG_SHORT_SUPPORT_MESSAGE.equals(tag)) {
                            if (parser.next() == 4) {
                                this.shortSupportMessage = parser.getText();
                            } else {
                                Slogf.w("DevicePolicyManager", "Missing text when loading short support message");
                            }
                        } else if (TAG_LONG_SUPPORT_MESSAGE.equals(tag)) {
                            if (parser.next() == 4) {
                                this.longSupportMessage = parser.getText();
                            } else {
                                Slogf.w("DevicePolicyManager", "Missing text when loading long support message");
                            }
                        } else if (TAG_PARENT_ADMIN.equals(tag)) {
                            Preconditions.checkState(!this.isParent);
                            ActiveAdmin activeAdmin = new ActiveAdmin(this.info, true);
                            this.parentAdmin = activeAdmin;
                            activeAdmin.readFromXml(parser, shouldOverridePolicies);
                        } else if (TAG_ORGANIZATION_COLOR.equals(tag)) {
                            this.organizationColor = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_ORGANIZATION_NAME.equals(tag)) {
                            if (parser.next() == 4) {
                                this.organizationName = parser.getText();
                            } else {
                                Slogf.w("DevicePolicyManager", "Missing text when loading organization name");
                            }
                        } else if (TAG_IS_LOGOUT_ENABLED.equals(tag)) {
                            this.isLogoutEnabled = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_START_USER_SESSION_MESSAGE.equals(tag)) {
                            if (parser.next() == 4) {
                                this.startUserSessionMessage = parser.getText();
                            } else {
                                Slogf.w("DevicePolicyManager", "Missing text when loading start session message");
                            }
                        } else if (TAG_END_USER_SESSION_MESSAGE.equals(tag)) {
                            if (parser.next() == 4) {
                                this.endUserSessionMessage = parser.getText();
                            } else {
                                Slogf.w("DevicePolicyManager", "Missing text when loading end session message");
                            }
                        } else if (TAG_CROSS_PROFILE_CALENDAR_PACKAGES.equals(tag)) {
                            this.mCrossProfileCalendarPackages = readPackageList(parser, tag);
                        } else if (TAG_CROSS_PROFILE_CALENDAR_PACKAGES_NULL.equals(tag)) {
                            this.mCrossProfileCalendarPackages = null;
                        } else if (TAG_CROSS_PROFILE_PACKAGES.equals(tag)) {
                            this.mCrossProfilePackages = readPackageList(parser, tag);
                        } else if (TAG_FACTORY_RESET_PROTECTION_POLICY.equals(tag)) {
                            this.mFactoryResetProtectionPolicy = FactoryResetProtectionPolicy.readFromXml(parser);
                        } else if (TAG_SUSPEND_PERSONAL_APPS.equals(tag)) {
                            this.mSuspendPersonalApps = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_PROFILE_MAXIMUM_TIME_OFF.equals(tag)) {
                            this.mProfileMaximumTimeOffMillis = parser.getAttributeLong((String) null, ATTR_VALUE);
                        } else if (TAG_PROFILE_OFF_DEADLINE.equals(tag)) {
                            this.mProfileOffDeadline = parser.getAttributeLong((String) null, ATTR_VALUE);
                        } else if (TAG_ALWAYS_ON_VPN_PACKAGE.equals(tag)) {
                            this.mAlwaysOnVpnPackage = parser.getAttributeValue((String) null, ATTR_VALUE);
                        } else if (TAG_ALWAYS_ON_VPN_LOCKDOWN.equals(tag)) {
                            this.mAlwaysOnVpnLockdown = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_PREFERENTIAL_NETWORK_SERVICE_ENABLED.equals(tag)) {
                            boolean attributeBoolean = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                            this.mPreferentialNetworkServiceEnabled = attributeBoolean;
                            if (attributeBoolean) {
                                PreferentialNetworkServiceConfig.Builder configBuilder = new PreferentialNetworkServiceConfig.Builder();
                                configBuilder.setEnabled(this.mPreferentialNetworkServiceEnabled);
                                configBuilder.setNetworkId(1);
                                this.mPreferentialNetworkServiceConfigs = List.of(configBuilder.build());
                                this.mPreferentialNetworkServiceEnabled = false;
                            }
                        } else if (TAG_COMMON_CRITERIA_MODE.equals(tag)) {
                            this.mCommonCriteriaMode = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_PASSWORD_COMPLEXITY.equals(tag)) {
                            this.mPasswordComplexity = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_NEARBY_NOTIFICATION_STREAMING_POLICY.equals(tag)) {
                            this.mNearbyNotificationStreamingPolicy = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_NEARBY_APP_STREAMING_POLICY.equals(tag)) {
                            this.mNearbyAppStreamingPolicy = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_ORGANIZATION_ID.equals(tag)) {
                            if (parser.next() == 4) {
                                this.mOrganizationId = parser.getText();
                            } else {
                                Slogf.w("DevicePolicyManager", "Missing Organization ID.");
                            }
                        } else if (TAG_ENROLLMENT_SPECIFIC_ID.equals(tag)) {
                            if (parser.next() == 4) {
                                this.mEnrollmentSpecificId = parser.getText();
                            } else {
                                Slogf.w("DevicePolicyManager", "Missing Enrollment-specific ID.");
                            }
                        } else if (TAG_ADMIN_CAN_GRANT_SENSORS_PERMISSIONS.equals(tag)) {
                            this.mAdminCanGrantSensorsPermissions = parser.getAttributeBoolean((String) null, ATTR_VALUE, false);
                        } else if (TAG_USB_DATA_SIGNALING.equals(tag)) {
                            this.mUsbDataSignalingEnabled = parser.getAttributeBoolean((String) null, ATTR_VALUE, true);
                        } else if (TAG_WIFI_MIN_SECURITY.equals(tag)) {
                            this.mWifiMinimumSecurityLevel = parser.getAttributeInt((String) null, ATTR_VALUE);
                        } else if (TAG_SSID_ALLOWLIST.equals(tag)) {
                            List<WifiSsid> ssids = readWifiSsids(parser, TAG_SSID);
                            this.mWifiSsidPolicy = new WifiSsidPolicy(0, new ArraySet(ssids));
                        } else if (TAG_SSID_DENYLIST.equals(tag)) {
                            List<WifiSsid> ssids2 = readWifiSsids(parser, TAG_SSID);
                            this.mWifiSsidPolicy = new WifiSsidPolicy(1, new ArraySet(ssids2));
                        } else if (TAG_PREFERENTIAL_NETWORK_SERVICE_CONFIGS.equals(tag)) {
                            List<PreferentialNetworkServiceConfig> configs = getPreferentialNetworkServiceConfigs(parser, tag);
                            if (!configs.isEmpty()) {
                                this.mPreferentialNetworkServiceConfigs = configs;
                            }
                        } else {
                            Slogf.w("DevicePolicyManager", "Unknown admin tag: %s", tag);
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private List<WifiSsid> readWifiSsids(TypedXmlPullParser parser, String tag) throws XmlPullParserException, IOException {
        List<String> ssidStrings = new ArrayList<>();
        readAttributeValues(parser, tag, ssidStrings);
        List<WifiSsid> ssids = (List) ssidStrings.stream().map(new Function() { // from class: com.android.server.devicepolicy.ActiveAdmin$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                WifiSsid fromBytes;
                fromBytes = WifiSsid.fromBytes(((String) obj).getBytes(StandardCharsets.UTF_8));
                return fromBytes;
            }
        }).collect(Collectors.toList());
        return ssids;
    }

    private List<String> readPackageList(TypedXmlPullParser parser, String tag) throws XmlPullParserException, IOException {
        List<String> result = new ArrayList<>();
        int outerDepth = parser.getDepth();
        while (true) {
            int outerType = parser.next();
            if (outerType == 1 || (outerType == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (outerType != 3 && outerType != 4) {
                String outerTag = parser.getName();
                if ("item".equals(outerTag)) {
                    String packageName = parser.getAttributeValue((String) null, ATTR_VALUE);
                    if (packageName != null) {
                        result.add(packageName);
                    } else {
                        Slogf.w("DevicePolicyManager", "Package name missing under %s", outerTag);
                    }
                } else {
                    Slogf.w("DevicePolicyManager", "Unknown tag under %s: ", tag, outerTag);
                }
            }
        }
        return result;
    }

    private void readAttributeValues(TypedXmlPullParser parser, String tag, Collection<String> result) throws XmlPullParserException, IOException {
        result.clear();
        int outerDepthDAM = parser.getDepth();
        while (true) {
            int typeDAM = parser.next();
            if (typeDAM != 1) {
                if (typeDAM != 3 || parser.getDepth() > outerDepthDAM) {
                    if (typeDAM != 3 && typeDAM != 4) {
                        String tagDAM = parser.getName();
                        if (tag.equals(tagDAM)) {
                            result.add(parser.getAttributeValue((String) null, ATTR_VALUE));
                        } else {
                            Slogf.e("DevicePolicyManager", "Expected tag %s but found %s", tag, tagDAM);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private ArrayMap<String, TrustAgentInfo> getAllTrustAgentInfos(TypedXmlPullParser parser, String tag) throws XmlPullParserException, IOException {
        int outerDepthDAM = parser.getDepth();
        ArrayMap<String, TrustAgentInfo> result = new ArrayMap<>();
        while (true) {
            int typeDAM = parser.next();
            if (typeDAM == 1 || (typeDAM == 3 && parser.getDepth() <= outerDepthDAM)) {
                break;
            } else if (typeDAM != 3 && typeDAM != 4) {
                String tagDAM = parser.getName();
                if (TAG_TRUST_AGENT_COMPONENT.equals(tagDAM)) {
                    String component = parser.getAttributeValue((String) null, ATTR_VALUE);
                    TrustAgentInfo trustAgentInfo = getTrustAgentInfo(parser, tag);
                    result.put(component, trustAgentInfo);
                } else {
                    Slogf.w("DevicePolicyManager", "Unknown tag under %s: %s", tag, tagDAM);
                }
            }
        }
        return result;
    }

    private TrustAgentInfo getTrustAgentInfo(TypedXmlPullParser parser, String outerTag) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        TrustAgentInfo result = new TrustAgentInfo(null);
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3 && type != 4) {
                String tag = parser.getName();
                if (TAG_TRUST_AGENT_COMPONENT_OPTIONS.equals(tag)) {
                    result.options = PersistableBundle.restoreFromXml(parser);
                } else {
                    Slogf.w("DevicePolicyManager", "Unknown tag under %s: %s", outerTag, tag);
                }
            }
        }
        return result;
    }

    private List<PreferentialNetworkServiceConfig> getPreferentialNetworkServiceConfigs(TypedXmlPullParser parser, String tag) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        List<PreferentialNetworkServiceConfig> result = new ArrayList<>();
        while (true) {
            int typeDAM = parser.next();
            if (typeDAM == 1 || (typeDAM == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (typeDAM != 3 && typeDAM != 4) {
                String tagDAM = parser.getName();
                if (TAG_PREFERENTIAL_NETWORK_SERVICE_CONFIG.equals(tagDAM)) {
                    PreferentialNetworkServiceConfig preferentialNetworkServiceConfig = PreferentialNetworkServiceConfig.getPreferentialNetworkServiceConfig(parser, tag);
                    result.add(preferentialNetworkServiceConfig);
                } else {
                    Slogf.w("DevicePolicyManager", "Unknown tag under %s: %s", tag, tagDAM);
                }
            }
        }
        return result;
    }

    boolean hasUserRestrictions() {
        Bundle bundle = this.userRestrictions;
        return bundle != null && bundle.size() > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle ensureUserRestrictions() {
        if (this.userRestrictions == null) {
            this.userRestrictions = new Bundle();
        }
        return this.userRestrictions;
    }

    public void transfer(DeviceAdminInfo deviceAdminInfo) {
        if (hasParentActiveAdmin()) {
            this.parentAdmin.info = deviceAdminInfo;
        }
        this.info = deviceAdminInfo;
    }

    Bundle addSyntheticRestrictions(Bundle restrictions) {
        if (this.disableCamera) {
            restrictions.putBoolean("no_camera", true);
        }
        if (this.requireAutoTime) {
            restrictions.putBoolean("no_config_date_time", true);
        }
        return restrictions;
    }

    static Bundle removeDeprecatedRestrictions(Bundle restrictions) {
        for (String deprecatedRestriction : UserRestrictionsUtils.DEPRECATED_USER_RESTRICTIONS) {
            restrictions.remove(deprecatedRestriction);
        }
        return restrictions;
    }

    static Bundle filterRestrictions(Bundle restrictions, Predicate<String> filter) {
        Bundle result = new Bundle();
        for (String key : restrictions.keySet()) {
            if (restrictions.getBoolean(key) && filter.test(key)) {
                result.putBoolean(key, true);
            }
        }
        return result;
    }

    Bundle getEffectiveRestrictions() {
        return addSyntheticRestrictions(removeDeprecatedRestrictions(new Bundle(ensureUserRestrictions())));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle getLocalUserRestrictions(final int adminType) {
        return filterRestrictions(getEffectiveRestrictions(), new Predicate() { // from class: com.android.server.devicepolicy.ActiveAdmin$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isLocal;
                isLocal = UserRestrictionsUtils.isLocal(adminType, (String) obj);
                return isLocal;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle getGlobalUserRestrictions(final int adminType) {
        return filterRestrictions(getEffectiveRestrictions(), new Predicate() { // from class: com.android.server.devicepolicy.ActiveAdmin$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isGlobal;
                isGlobal = UserRestrictionsUtils.isGlobal(adminType, (String) obj);
                return isGlobal;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        pw.print("uid=");
        pw.println(getUid());
        pw.print("testOnlyAdmin=");
        pw.println(this.testOnlyAdmin);
        pw.println("policies:");
        ArrayList<DeviceAdminInfo.PolicyInfo> pols = this.info.getUsedPolicies();
        if (pols != null) {
            pw.increaseIndent();
            for (int i = 0; i < pols.size(); i++) {
                pw.println(pols.get(i).tag);
            }
            pw.decreaseIndent();
        }
        pw.print("passwordQuality=0x");
        pw.println(Integer.toHexString(this.mPasswordPolicy.quality));
        pw.print("minimumPasswordLength=");
        pw.println(this.mPasswordPolicy.length);
        pw.print("passwordHistoryLength=");
        pw.println(this.passwordHistoryLength);
        pw.print("minimumPasswordUpperCase=");
        pw.println(this.mPasswordPolicy.upperCase);
        pw.print("minimumPasswordLowerCase=");
        pw.println(this.mPasswordPolicy.lowerCase);
        pw.print("minimumPasswordLetters=");
        pw.println(this.mPasswordPolicy.letters);
        pw.print("minimumPasswordNumeric=");
        pw.println(this.mPasswordPolicy.numeric);
        pw.print("minimumPasswordSymbols=");
        pw.println(this.mPasswordPolicy.symbols);
        pw.print("minimumPasswordNonLetter=");
        pw.println(this.mPasswordPolicy.nonLetter);
        pw.print("maximumTimeToUnlock=");
        pw.println(this.maximumTimeToUnlock);
        pw.print("strongAuthUnlockTimeout=");
        pw.println(this.strongAuthUnlockTimeout);
        pw.print("maximumFailedPasswordsForWipe=");
        pw.println(this.maximumFailedPasswordsForWipe);
        pw.print("specifiesGlobalProxy=");
        pw.println(this.specifiesGlobalProxy);
        pw.print("passwordExpirationTimeout=");
        pw.println(this.passwordExpirationTimeout);
        pw.print("passwordExpirationDate=");
        pw.println(this.passwordExpirationDate);
        if (this.globalProxySpec != null) {
            pw.print("globalProxySpec=");
            pw.println(this.globalProxySpec);
        }
        if (this.globalProxyExclusionList != null) {
            pw.print("globalProxyEclusionList=");
            pw.println(this.globalProxyExclusionList);
        }
        pw.print("encryptionRequested=");
        pw.println(this.encryptionRequested);
        pw.print("disableCamera=");
        pw.println(this.disableCamera);
        pw.print("disableCallerId=");
        pw.println(this.disableCallerId);
        pw.print("disableContactsSearch=");
        pw.println(this.disableContactsSearch);
        pw.print("disableBluetoothContactSharing=");
        pw.println(this.disableBluetoothContactSharing);
        pw.print("disableScreenCapture=");
        pw.println(this.disableScreenCapture);
        pw.print("requireAutoTime=");
        pw.println(this.requireAutoTime);
        pw.print("forceEphemeralUsers=");
        pw.println(this.forceEphemeralUsers);
        pw.print("isNetworkLoggingEnabled=");
        pw.println(this.isNetworkLoggingEnabled);
        pw.print("disabledKeyguardFeatures=");
        pw.println(this.disabledKeyguardFeatures);
        pw.print("crossProfileWidgetProviders=");
        pw.println(this.crossProfileWidgetProviders);
        if (this.permittedAccessiblityServices != null) {
            pw.print("permittedAccessibilityServices=");
            pw.println(this.permittedAccessiblityServices);
        }
        if (this.permittedInputMethods != null) {
            pw.print("permittedInputMethods=");
            pw.println(this.permittedInputMethods);
        }
        if (this.permittedNotificationListeners != null) {
            pw.print("permittedNotificationListeners=");
            pw.println(this.permittedNotificationListeners);
        }
        if (this.keepUninstalledPackages != null) {
            pw.print("keepUninstalledPackages=");
            pw.println(this.keepUninstalledPackages);
        }
        if (this.meteredDisabledPackages != null) {
            pw.print("meteredDisabledPackages=");
            pw.println(this.meteredDisabledPackages);
        }
        if (this.protectedPackages != null) {
            pw.print("protectedPackages=");
            pw.println(this.protectedPackages);
        }
        pw.print("organizationColor=");
        pw.println(this.organizationColor);
        if (this.organizationName != null) {
            pw.print("organizationName=");
            pw.println(this.organizationName);
        }
        pw.println("userRestrictions:");
        UserRestrictionsUtils.dumpRestrictions(pw, "  ", this.userRestrictions);
        pw.print("defaultEnabledRestrictionsAlreadySet=");
        pw.println(this.defaultEnabledRestrictionsAlreadySet);
        pw.print("isParent=");
        pw.println(this.isParent);
        if (this.parentAdmin != null) {
            pw.println("parentAdmin:");
            pw.increaseIndent();
            this.parentAdmin.dump(pw);
            pw.decreaseIndent();
        }
        if (this.mCrossProfileCalendarPackages != null) {
            pw.print("mCrossProfileCalendarPackages=");
            pw.println(this.mCrossProfileCalendarPackages);
        }
        pw.print("mCrossProfilePackages=");
        pw.println(this.mCrossProfilePackages);
        pw.print("mSuspendPersonalApps=");
        pw.println(this.mSuspendPersonalApps);
        pw.print("mProfileMaximumTimeOffMillis=");
        pw.println(this.mProfileMaximumTimeOffMillis);
        pw.print("mProfileOffDeadline=");
        pw.println(this.mProfileOffDeadline);
        pw.print("mAlwaysOnVpnPackage=");
        pw.println(this.mAlwaysOnVpnPackage);
        pw.print("mAlwaysOnVpnLockdown=");
        pw.println(this.mAlwaysOnVpnLockdown);
        pw.print("mPreferentialNetworkServiceEnabled=");
        pw.println(this.mPreferentialNetworkServiceEnabled);
        pw.print("mCommonCriteriaMode=");
        pw.println(this.mCommonCriteriaMode);
        pw.print("mPasswordComplexity=");
        pw.println(this.mPasswordComplexity);
        pw.print("mNearbyNotificationStreamingPolicy=");
        pw.println(this.mNearbyNotificationStreamingPolicy);
        pw.print("mNearbyAppStreamingPolicy=");
        pw.println(this.mNearbyAppStreamingPolicy);
        if (!TextUtils.isEmpty(this.mOrganizationId)) {
            pw.print("mOrganizationId=");
            pw.println(this.mOrganizationId);
        }
        if (!TextUtils.isEmpty(this.mEnrollmentSpecificId)) {
            pw.print("mEnrollmentSpecificId=");
            pw.println(this.mEnrollmentSpecificId);
        }
        pw.print("mAdminCanGrantSensorsPermissions=");
        pw.println(this.mAdminCanGrantSensorsPermissions);
        pw.print("mUsbDataSignaling=");
        pw.println(this.mUsbDataSignalingEnabled);
        pw.print("mWifiMinimumSecurityLevel=");
        pw.println(this.mWifiMinimumSecurityLevel);
        WifiSsidPolicy wifiSsidPolicy = this.mWifiSsidPolicy;
        if (wifiSsidPolicy != null) {
            if (wifiSsidPolicy.getPolicyType() == 0) {
                pw.print("mSsidAllowlist=");
            } else {
                pw.print("mSsidDenylist=");
            }
            pw.println(ssidsToStrings(this.mWifiSsidPolicy.getSsids()));
        }
        if (this.mFactoryResetProtectionPolicy != null) {
            pw.println("mFactoryResetProtectionPolicy:");
            pw.increaseIndent();
            this.mFactoryResetProtectionPolicy.dump(pw);
            pw.decreaseIndent();
        }
        if (this.mPreferentialNetworkServiceConfigs != null) {
            pw.println("mPreferentialNetworkServiceConfigs:");
            pw.increaseIndent();
            for (PreferentialNetworkServiceConfig config : this.mPreferentialNetworkServiceConfigs) {
                config.dump(pw);
            }
            pw.decreaseIndent();
        }
    }
}
