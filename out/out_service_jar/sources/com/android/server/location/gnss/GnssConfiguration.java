package com.android.server.location.gnss;

import android.content.Context;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.FrameworkStatsLog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public class GnssConfiguration {
    private static final String CONFIG_A_GLONASS_POS_PROTOCOL_SELECT = "A_GLONASS_POS_PROTOCOL_SELECT";
    private static final String CONFIG_C2K_HOST = "C2K_HOST";
    private static final String CONFIG_C2K_PORT = "C2K_PORT";
    private static final String CONFIG_ENABLE_PSDS_PERIODIC_DOWNLOAD = "ENABLE_PSDS_PERIODIC_DOWNLOAD";
    private static final String CONFIG_ES_EXTENSION_SEC = "ES_EXTENSION_SEC";
    private static final String CONFIG_GPS_LOCK = "GPS_LOCK";
    static final String CONFIG_LONGTERM_PSDS_SERVER_1 = "LONGTERM_PSDS_SERVER_1";
    static final String CONFIG_LONGTERM_PSDS_SERVER_2 = "LONGTERM_PSDS_SERVER_2";
    static final String CONFIG_LONGTERM_PSDS_SERVER_3 = "LONGTERM_PSDS_SERVER_3";
    private static final String CONFIG_LPP_PROFILE = "LPP_PROFILE";
    static final String CONFIG_NFW_PROXY_APPS = "NFW_PROXY_APPS";
    static final String CONFIG_NORMAL_PSDS_SERVER = "NORMAL_PSDS_SERVER";
    static final String CONFIG_REALTIME_PSDS_SERVER = "REALTIME_PSDS_SERVER";
    private static final String CONFIG_SUPL_ES = "SUPL_ES";
    private static final String CONFIG_SUPL_HOST = "SUPL_HOST";
    private static final String CONFIG_SUPL_MODE = "SUPL_MODE";
    private static final String CONFIG_SUPL_PORT = "SUPL_PORT";
    private static final String CONFIG_SUPL_VER = "SUPL_VER";
    private static final String CONFIG_USE_EMERGENCY_PDN_FOR_EMERGENCY_SUPL = "USE_EMERGENCY_PDN_FOR_EMERGENCY_SUPL";
    private static final String DEBUG_PROPERTIES_FILE = "/etc/gps_debug.conf";
    static final String LPP_PROFILE = "persist.sys.gps.lpp";
    private static final int MAX_EMERGENCY_MODE_EXTENSION_SECONDS = 300;
    private final Context mContext;
    private int mEsExtensionSec = 0;
    private final Properties mProperties = new Properties();
    private static final String TAG = "GnssConfiguration";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    /* loaded from: classes.dex */
    interface SetCarrierProperty {
        boolean set(int i);
    }

    private static native HalInterfaceVersion native_get_gnss_configuration_version();

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_emergency_supl_pdn(int i);

    private static native boolean native_set_es_extension_sec(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_gnss_pos_protocol_select(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_gps_lock(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_lpp_profile(int i);

    private static native boolean native_set_satellite_blocklist(int[] iArr, int[] iArr2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_supl_es(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_supl_mode(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_supl_version(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class HalInterfaceVersion {
        final int mMajor;
        final int mMinor;

        HalInterfaceVersion(int major, int minor) {
            this.mMajor = major;
            this.mMinor = minor;
        }
    }

    public GnssConfiguration(Context context) {
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Properties getProperties() {
        return this.mProperties;
    }

    public int getEsExtensionSec() {
        return this.mEsExtensionSec;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getSuplHost() {
        return this.mProperties.getProperty(CONFIG_SUPL_HOST);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSuplPort(int defaultPort) {
        return getIntConfig(CONFIG_SUPL_PORT, defaultPort);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getC2KHost() {
        return this.mProperties.getProperty(CONFIG_C2K_HOST);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getC2KPort(int defaultPort) {
        return getIntConfig(CONFIG_C2K_PORT, defaultPort);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSuplMode(int defaultMode) {
        return getIntConfig(CONFIG_SUPL_MODE, defaultMode);
    }

    public int getSuplEs(int defaultSuplEs) {
        return getIntConfig(CONFIG_SUPL_ES, defaultSuplEs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getLppProfile() {
        return this.mProperties.getProperty(CONFIG_LPP_PROFILE);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<String> getProxyApps() {
        String proxyAppsStr = this.mProperties.getProperty(CONFIG_NFW_PROXY_APPS);
        if (TextUtils.isEmpty(proxyAppsStr)) {
            return Collections.emptyList();
        }
        String[] proxyAppsArray = proxyAppsStr.trim().split("\\s+");
        if (proxyAppsArray.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.asList(proxyAppsArray);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPsdsPeriodicDownloadEnabled() {
        return getBooleanConfig(CONFIG_ENABLE_PSDS_PERIODIC_DOWNLOAD, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLongTermPsdsServerConfigured() {
        return (this.mProperties.getProperty(CONFIG_LONGTERM_PSDS_SERVER_1) == null && this.mProperties.getProperty(CONFIG_LONGTERM_PSDS_SERVER_2) == null && this.mProperties.getProperty(CONFIG_LONGTERM_PSDS_SERVER_3) == null) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSatelliteBlocklist(int[] constellations, int[] svids) {
        native_set_satellite_blocklist(constellations, svids);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HalInterfaceVersion getHalInterfaceVersion() {
        return native_get_gnss_configuration_version();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reloadGpsProperties() {
        boolean z = DEBUG;
        if (z) {
            Log.d(TAG, "Reset GPS properties, previous size = " + this.mProperties.size());
        }
        loadPropertiesFromCarrierConfig();
        String lpp_prof = SystemProperties.get(LPP_PROFILE);
        if (!TextUtils.isEmpty(lpp_prof)) {
            this.mProperties.setProperty(CONFIG_LPP_PROFILE, lpp_prof);
        }
        loadPropertiesFromGpsDebugConfig(this.mProperties);
        this.mEsExtensionSec = getRangeCheckedConfigEsExtensionSec();
        logConfigurations();
        HalInterfaceVersion gnssConfigurationIfaceVersion = getHalInterfaceVersion();
        if (gnssConfigurationIfaceVersion != null) {
            if (isConfigEsExtensionSecSupported(gnssConfigurationIfaceVersion) && !native_set_es_extension_sec(this.mEsExtensionSec)) {
                Log.e(TAG, "Unable to set ES_EXTENSION_SEC: " + this.mEsExtensionSec);
            }
            Map<String, SetCarrierProperty> map = new AnonymousClass1(gnssConfigurationIfaceVersion);
            for (Map.Entry<String, SetCarrierProperty> entry : map.entrySet()) {
                String propertyName = entry.getKey();
                String propertyValueString = this.mProperties.getProperty(propertyName);
                if (propertyValueString != null) {
                    try {
                        int propertyValueInt = Integer.decode(propertyValueString).intValue();
                        boolean result = entry.getValue().set(propertyValueInt);
                        if (!result) {
                            Log.e(TAG, "Unable to set " + propertyName);
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Unable to parse propertyName: " + propertyValueString);
                    }
                }
            }
        } else if (z) {
            Log.d(TAG, "Skipped configuration update because GNSS configuration in GPS HAL is not supported");
        }
    }

    /* renamed from: com.android.server.location.gnss.GnssConfiguration$1  reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass1 extends HashMap<String, SetCarrierProperty> {
        final /* synthetic */ HalInterfaceVersion val$gnssConfigurationIfaceVersion;

        AnonymousClass1(HalInterfaceVersion halInterfaceVersion) {
            this.val$gnssConfigurationIfaceVersion = halInterfaceVersion;
            put(GnssConfiguration.CONFIG_SUPL_VER, new SetCarrierProperty() { // from class: com.android.server.location.gnss.GnssConfiguration$1$$ExternalSyntheticLambda0
                @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
                public final boolean set(int i) {
                    boolean native_set_supl_version;
                    native_set_supl_version = GnssConfiguration.native_set_supl_version(i);
                    return native_set_supl_version;
                }
            });
            put(GnssConfiguration.CONFIG_SUPL_MODE, new SetCarrierProperty() { // from class: com.android.server.location.gnss.GnssConfiguration$1$$ExternalSyntheticLambda1
                @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
                public final boolean set(int i) {
                    boolean native_set_supl_mode;
                    native_set_supl_mode = GnssConfiguration.native_set_supl_mode(i);
                    return native_set_supl_mode;
                }
            });
            if (GnssConfiguration.isConfigSuplEsSupported(halInterfaceVersion)) {
                put(GnssConfiguration.CONFIG_SUPL_ES, new SetCarrierProperty() { // from class: com.android.server.location.gnss.GnssConfiguration$1$$ExternalSyntheticLambda2
                    @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
                    public final boolean set(int i) {
                        boolean native_set_supl_es;
                        native_set_supl_es = GnssConfiguration.native_set_supl_es(i);
                        return native_set_supl_es;
                    }
                });
            }
            put(GnssConfiguration.CONFIG_LPP_PROFILE, new SetCarrierProperty() { // from class: com.android.server.location.gnss.GnssConfiguration$1$$ExternalSyntheticLambda3
                @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
                public final boolean set(int i) {
                    boolean native_set_lpp_profile;
                    native_set_lpp_profile = GnssConfiguration.native_set_lpp_profile(i);
                    return native_set_lpp_profile;
                }
            });
            put(GnssConfiguration.CONFIG_A_GLONASS_POS_PROTOCOL_SELECT, new SetCarrierProperty() { // from class: com.android.server.location.gnss.GnssConfiguration$1$$ExternalSyntheticLambda4
                @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
                public final boolean set(int i) {
                    boolean native_set_gnss_pos_protocol_select;
                    native_set_gnss_pos_protocol_select = GnssConfiguration.native_set_gnss_pos_protocol_select(i);
                    return native_set_gnss_pos_protocol_select;
                }
            });
            put(GnssConfiguration.CONFIG_USE_EMERGENCY_PDN_FOR_EMERGENCY_SUPL, new SetCarrierProperty() { // from class: com.android.server.location.gnss.GnssConfiguration$1$$ExternalSyntheticLambda5
                @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
                public final boolean set(int i) {
                    boolean native_set_emergency_supl_pdn;
                    native_set_emergency_supl_pdn = GnssConfiguration.native_set_emergency_supl_pdn(i);
                    return native_set_emergency_supl_pdn;
                }
            });
            if (GnssConfiguration.isConfigGpsLockSupported(halInterfaceVersion)) {
                put(GnssConfiguration.CONFIG_GPS_LOCK, new SetCarrierProperty() { // from class: com.android.server.location.gnss.GnssConfiguration$1$$ExternalSyntheticLambda6
                    @Override // com.android.server.location.gnss.GnssConfiguration.SetCarrierProperty
                    public final boolean set(int i) {
                        boolean native_set_gps_lock;
                        native_set_gps_lock = GnssConfiguration.native_set_gps_lock(i);
                        return native_set_gps_lock;
                    }
                });
            }
        }
    }

    private void logConfigurations() {
        FrameworkStatsLog.write(132, getSuplHost(), getSuplPort(0), getC2KHost(), getC2KPort(0), getIntConfig(CONFIG_SUPL_VER, 0), getSuplMode(0), getSuplEs(0) == 1, getIntConfig(CONFIG_LPP_PROFILE, 0), getIntConfig(CONFIG_A_GLONASS_POS_PROTOCOL_SELECT, 0), getIntConfig(CONFIG_USE_EMERGENCY_PDN_FOR_EMERGENCY_SUPL, 0) == 1, getIntConfig(CONFIG_GPS_LOCK, 0), getEsExtensionSec(), this.mProperties.getProperty(CONFIG_NFW_PROXY_APPS));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadPropertiesFromCarrierConfig() {
        CarrierConfigManager configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (configManager == null) {
            return;
        }
        int ddSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        PersistableBundle configs = SubscriptionManager.isValidSubscriptionId(ddSubId) ? configManager.getConfigForSubId(ddSubId) : configManager.getConfig();
        if (configs == null) {
            if (DEBUG) {
                Log.d(TAG, "SIM not ready, use default carrier config.");
            }
            configs = CarrierConfigManager.getDefaultConfig();
        }
        for (String configKey : configs.keySet()) {
            if (configKey.startsWith("gps.")) {
                String key = configKey.substring("gps.".length()).toUpperCase();
                Object value = configs.get(configKey);
                if (DEBUG) {
                    Log.d(TAG, "Gps config: " + key + " = " + value);
                }
                if (value instanceof String) {
                    this.mProperties.setProperty(key, (String) value);
                } else if (value != null) {
                    this.mProperties.setProperty(key, value.toString());
                }
            }
        }
    }

    private void loadPropertiesFromGpsDebugConfig(Properties properties) {
        try {
            File file = new File(DEBUG_PROPERTIES_FILE);
            FileInputStream stream = new FileInputStream(file);
            properties.load(stream);
            IoUtils.closeQuietly(stream);
        } catch (IOException e) {
            if (DEBUG) {
                Log.d(TAG, "Could not open GPS configuration file /etc/gps_debug.conf");
            }
        }
    }

    private int getRangeCheckedConfigEsExtensionSec() {
        int emergencyExtensionSeconds = getIntConfig(CONFIG_ES_EXTENSION_SEC, 0);
        if (emergencyExtensionSeconds > 300) {
            Log.w(TAG, "ES_EXTENSION_SEC: " + emergencyExtensionSeconds + " too high, reset to 300");
            return 300;
        } else if (emergencyExtensionSeconds < 0) {
            Log.w(TAG, "ES_EXTENSION_SEC: " + emergencyExtensionSeconds + " is negative, reset to zero.");
            return 0;
        } else {
            return emergencyExtensionSeconds;
        }
    }

    private int getIntConfig(String configParameter, int defaultValue) {
        String valueString = this.mProperties.getProperty(configParameter);
        if (TextUtils.isEmpty(valueString)) {
            return defaultValue;
        }
        try {
            return Integer.decode(valueString).intValue();
        } catch (NumberFormatException e) {
            Log.e(TAG, "Unable to parse config parameter " + configParameter + " value: " + valueString + ". Using default value: " + defaultValue);
            return defaultValue;
        }
    }

    private boolean getBooleanConfig(String configParameter, boolean defaultValue) {
        String valueString = this.mProperties.getProperty(configParameter);
        if (TextUtils.isEmpty(valueString)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(valueString);
    }

    private static boolean isConfigEsExtensionSecSupported(HalInterfaceVersion gnssConfiguartionIfaceVersion) {
        return gnssConfiguartionIfaceVersion.mMajor >= 2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isConfigSuplEsSupported(HalInterfaceVersion gnssConfiguartionIfaceVersion) {
        return gnssConfiguartionIfaceVersion.mMajor < 2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isConfigGpsLockSupported(HalInterfaceVersion gnssConfiguartionIfaceVersion) {
        return gnssConfiguartionIfaceVersion.mMajor < 2;
    }
}
