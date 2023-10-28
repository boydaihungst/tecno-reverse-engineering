package com.android.server.power.batterysaver;

import android.app.UiModeManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatterySaverPolicyConfig;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerSaveState;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.IndentingPrintWriter;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public class BatterySaverPolicy extends ContentObserver implements DeviceConfig.OnPropertiesChangedListener {
    static final boolean DEBUG = false;
    private static final Policy DEFAULT_ADAPTIVE_POLICY;
    private static final Policy DEFAULT_FULL_POLICY;
    static final String KEY_ADJUST_BRIGHTNESS_FACTOR = "adjust_brightness_factor";
    static final String KEY_ADVERTISE_IS_ENABLED = "advertise_is_enabled";
    @Deprecated
    private static final String KEY_CPU_FREQ_INTERACTIVE = "cpufreq-i";
    @Deprecated
    private static final String KEY_CPU_FREQ_NONINTERACTIVE = "cpufreq-n";
    static final String KEY_DEFER_FULL_BACKUP = "defer_full_backup";
    static final String KEY_DEFER_KEYVALUE_BACKUP = "defer_keyvalue_backup";
    static final String KEY_DISABLE_ANIMATION = "disable_animation";
    static final String KEY_DISABLE_AOD = "disable_aod";
    static final String KEY_DISABLE_LAUNCH_BOOST = "disable_launch_boost";
    static final String KEY_DISABLE_OPTIONAL_SENSORS = "disable_optional_sensors";
    static final String KEY_DISABLE_VIBRATION = "disable_vibration";
    static final String KEY_ENABLE_BRIGHTNESS_ADJUSTMENT = "enable_brightness_adjustment";
    static final String KEY_ENABLE_DATASAVER = "enable_datasaver";
    static final String KEY_ENABLE_FIREWALL = "enable_firewall";
    static final String KEY_ENABLE_NIGHT_MODE = "enable_night_mode";
    static final String KEY_ENABLE_QUICK_DOZE = "enable_quick_doze";
    static final String KEY_FORCE_ALL_APPS_STANDBY = "force_all_apps_standby";
    static final String KEY_FORCE_BACKGROUND_CHECK = "force_background_check";
    static final String KEY_LOCATION_MODE = "location_mode";
    static final String KEY_SOUNDTRIGGER_MODE = "soundtrigger_mode";
    private static final String KEY_SUFFIX_ADAPTIVE = "_adaptive";
    static final Policy OFF_POLICY;
    static final int POLICY_LEVEL_ADAPTIVE = 1;
    static final int POLICY_LEVEL_FULL = 2;
    static final int POLICY_LEVEL_OFF = 0;
    private static final String TAG = "BatterySaverPolicy";
    final PolicyBoolean mAccessibilityEnabled;
    private Policy mAdaptivePolicy;
    final PolicyBoolean mAutomotiveProjectionActive;
    private final BatterySavingStats mBatterySavingStats;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private Policy mDefaultAdaptivePolicy;
    private Policy mDefaultFullPolicy;
    private String mDeviceSpecificSettings;
    private String mDeviceSpecificSettingsSource;
    private Policy mEffectivePolicyRaw;
    private String mEventLogKeys;
    private Policy mFullPolicy;
    private final Handler mHandler;
    private DeviceConfig.Properties mLastDeviceConfigProperties;
    private final List<BatterySaverPolicyListener> mListeners;
    private final Object mLock;
    private final UiModeManager.OnProjectionStateChangedListener mOnProjectionStateChangedListener;
    private int mPolicyLevel;
    private String mSettings;

    /* loaded from: classes2.dex */
    public interface BatterySaverPolicyListener {
        void onBatterySaverPolicyChanged(BatterySaverPolicy batterySaverPolicy);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface PolicyLevel {
    }

    static {
        Policy policy = new Policy(1.0f, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, 0, 0);
        OFF_POLICY = policy;
        DEFAULT_ADAPTIVE_POLICY = policy;
        DEFAULT_FULL_POLICY = new Policy(0.5f, true, true, true, false, true, true, true, true, false, false, true, false, true, true, true, 3, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-power-batterysaver-BatterySaverPolicy  reason: not valid java name */
    public /* synthetic */ void m6223xbe0cb4cc(int t, Set pkgs) {
        this.mAutomotiveProjectionActive.update(!pkgs.isEmpty());
    }

    public BatterySaverPolicy(Object lock, Context context, BatterySavingStats batterySavingStats) {
        super(BackgroundThread.getHandler());
        this.mAccessibilityEnabled = new PolicyBoolean("accessibility");
        this.mAutomotiveProjectionActive = new PolicyBoolean("automotiveProjection");
        Policy policy = DEFAULT_ADAPTIVE_POLICY;
        this.mDefaultAdaptivePolicy = policy;
        this.mAdaptivePolicy = policy;
        Policy policy2 = DEFAULT_FULL_POLICY;
        this.mDefaultFullPolicy = policy2;
        this.mFullPolicy = policy2;
        this.mEffectivePolicyRaw = OFF_POLICY;
        this.mPolicyLevel = 0;
        this.mOnProjectionStateChangedListener = new UiModeManager.OnProjectionStateChangedListener() { // from class: com.android.server.power.batterysaver.BatterySaverPolicy$$ExternalSyntheticLambda2
            public final void onProjectionStateChanged(int i, Set set) {
                BatterySaverPolicy.this.m6223xbe0cb4cc(i, set);
            }
        };
        this.mListeners = new ArrayList();
        this.mLock = lock;
        this.mHandler = BackgroundThread.getHandler();
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mBatterySavingStats = batterySavingStats;
    }

    public void systemReady() {
        ConcurrentUtils.wtfIfLockHeld(TAG, this.mLock);
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("battery_saver_constants"), false, this);
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("battery_saver_device_specific_constants"), false, this);
        AccessibilityManager acm = (AccessibilityManager) this.mContext.getSystemService(AccessibilityManager.class);
        acm.addAccessibilityStateChangeListener(new AccessibilityManager.AccessibilityStateChangeListener() { // from class: com.android.server.power.batterysaver.BatterySaverPolicy$$ExternalSyntheticLambda0
            @Override // android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener
            public final void onAccessibilityStateChanged(boolean z) {
                BatterySaverPolicy.this.m6224xe2c82f57(z);
            }
        });
        this.mAccessibilityEnabled.initialize(acm.isEnabled());
        UiModeManager uiModeManager = (UiModeManager) this.mContext.getSystemService(UiModeManager.class);
        uiModeManager.addOnProjectionStateChangedListener(1, this.mContext.getMainExecutor(), this.mOnProjectionStateChangedListener);
        this.mAutomotiveProjectionActive.initialize(uiModeManager.getActiveProjectionTypes() != 0);
        DeviceConfig.addOnPropertiesChangedListener("battery_saver", this.mContext.getMainExecutor(), this);
        this.mLastDeviceConfigProperties = DeviceConfig.getProperties("battery_saver", new String[0]);
        onChange(true, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$systemReady$1$com-android-server-power-batterysaver-BatterySaverPolicy  reason: not valid java name */
    public /* synthetic */ void m6224xe2c82f57(boolean enabled) {
        this.mAccessibilityEnabled.update(enabled);
    }

    public void addListener(BatterySaverPolicyListener listener) {
        synchronized (this.mLock) {
            this.mListeners.add(listener);
        }
    }

    String getGlobalSetting(String key) {
        return Settings.Global.getString(this.mContentResolver, key);
    }

    int getDeviceSpecificConfigResId() {
        return 17039886;
    }

    void invalidatePowerSaveModeCaches() {
        PowerManager.invalidatePowerSaveModeCaches();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeNotifyListenersOfPolicyChange() {
        synchronized (this.mLock) {
            if (this.mPolicyLevel == 0) {
                return;
            }
            List<BatterySaverPolicyListener> list = this.mListeners;
            final BatterySaverPolicyListener[] listeners = (BatterySaverPolicyListener[]) list.toArray(new BatterySaverPolicyListener[list.size()]);
            this.mHandler.post(new Runnable() { // from class: com.android.server.power.batterysaver.BatterySaverPolicy$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BatterySaverPolicy.this.m6222x336751a9(listeners);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$maybeNotifyListenersOfPolicyChange$2$com-android-server-power-batterysaver-BatterySaverPolicy  reason: not valid java name */
    public /* synthetic */ void m6222x336751a9(BatterySaverPolicyListener[] listeners) {
        for (BatterySaverPolicyListener listener : listeners) {
            listener.onBatterySaverPolicyChanged(this);
        }
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean selfChange, Uri uri) {
        refreshSettings();
    }

    public void onPropertiesChanged(DeviceConfig.Properties properties) {
        boolean changed;
        this.mLastDeviceConfigProperties = DeviceConfig.getProperties("battery_saver", new String[0]);
        Policy newAdaptivePolicy = null;
        Policy newFullPolicy = null;
        synchronized (this.mLock) {
            for (String name : properties.getKeyset()) {
                if (name != null) {
                    if (name.endsWith(KEY_SUFFIX_ADAPTIVE)) {
                        if (newAdaptivePolicy == null) {
                            newAdaptivePolicy = Policy.fromSettings("", "", this.mLastDeviceConfigProperties, KEY_SUFFIX_ADAPTIVE, DEFAULT_ADAPTIVE_POLICY);
                        }
                    } else if (newFullPolicy == null) {
                        newFullPolicy = Policy.fromSettings(this.mSettings, this.mDeviceSpecificSettings, this.mLastDeviceConfigProperties, null, DEFAULT_FULL_POLICY);
                    }
                }
            }
            changed = newFullPolicy != null ? false | maybeUpdateDefaultFullPolicy(newFullPolicy) : false;
            if (newAdaptivePolicy != null && !this.mAdaptivePolicy.equals(newAdaptivePolicy)) {
                this.mDefaultAdaptivePolicy = newAdaptivePolicy;
                this.mAdaptivePolicy = newAdaptivePolicy;
                changed = (this.mPolicyLevel == 1) | changed;
            }
            updatePolicyDependenciesLocked();
        }
        if (changed) {
            maybeNotifyListenersOfPolicyChange();
        }
    }

    private void refreshSettings() {
        synchronized (this.mLock) {
            String setting = getGlobalSetting("battery_saver_constants");
            String deviceSpecificSetting = getGlobalSetting("battery_saver_device_specific_constants");
            this.mDeviceSpecificSettingsSource = "battery_saver_device_specific_constants";
            if (TextUtils.isEmpty(deviceSpecificSetting) || "null".equals(deviceSpecificSetting)) {
                deviceSpecificSetting = this.mContext.getString(getDeviceSpecificConfigResId());
                this.mDeviceSpecificSettingsSource = "(overlay)";
            }
            if (updateConstantsLocked(setting, deviceSpecificSetting)) {
                maybeNotifyListenersOfPolicyChange();
            }
        }
    }

    boolean updateConstantsLocked(String setting, String deviceSpecificSetting) {
        String setting2 = TextUtils.emptyIfNull(setting);
        String deviceSpecificSetting2 = TextUtils.emptyIfNull(deviceSpecificSetting);
        if (setting2.equals(this.mSettings) && deviceSpecificSetting2.equals(this.mDeviceSpecificSettings)) {
            return false;
        }
        this.mSettings = setting2;
        this.mDeviceSpecificSettings = deviceSpecificSetting2;
        boolean changed = maybeUpdateDefaultFullPolicy(Policy.fromSettings(setting2, deviceSpecificSetting2, this.mLastDeviceConfigProperties, null, DEFAULT_FULL_POLICY));
        Policy fromSettings = Policy.fromSettings("", "", this.mLastDeviceConfigProperties, KEY_SUFFIX_ADAPTIVE, DEFAULT_ADAPTIVE_POLICY);
        this.mDefaultAdaptivePolicy = fromSettings;
        if (this.mPolicyLevel == 1 && !this.mAdaptivePolicy.equals(fromSettings)) {
            changed = true;
        }
        this.mAdaptivePolicy = this.mDefaultAdaptivePolicy;
        updatePolicyDependenciesLocked();
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePolicyDependenciesLocked() {
        int locationMode;
        Policy rawPolicy = getCurrentRawPolicyLocked();
        invalidatePowerSaveModeCaches();
        if (this.mAutomotiveProjectionActive.get() && rawPolicy.locationMode != 0 && rawPolicy.locationMode != 3) {
            locationMode = 3;
        } else {
            locationMode = rawPolicy.locationMode;
        }
        int locationMode2 = locationMode;
        this.mEffectivePolicyRaw = new Policy(rawPolicy.adjustBrightnessFactor, rawPolicy.advertiseIsEnabled, rawPolicy.deferFullBackup, rawPolicy.deferKeyValueBackup, rawPolicy.disableAnimation, rawPolicy.disableAod, rawPolicy.disableLaunchBoost, rawPolicy.disableOptionalSensors, rawPolicy.disableVibration && !this.mAccessibilityEnabled.get(), rawPolicy.enableAdjustBrightness, rawPolicy.enableDataSaver, rawPolicy.enableFirewall, rawPolicy.enableNightMode && !this.mAutomotiveProjectionActive.get(), rawPolicy.enableQuickDoze, rawPolicy.forceAllAppsStandby, rawPolicy.forceBackgroundCheck, locationMode2, rawPolicy.soundTriggerMode);
        StringBuilder sb = new StringBuilder();
        if (this.mEffectivePolicyRaw.forceAllAppsStandby) {
            sb.append("A");
        }
        if (this.mEffectivePolicyRaw.forceBackgroundCheck) {
            sb.append("B");
        }
        if (this.mEffectivePolicyRaw.disableVibration) {
            sb.append("v");
        }
        if (this.mEffectivePolicyRaw.disableAnimation) {
            sb.append(ActivityTaskManagerService.DUMP_ACTIVITIES_SHORT_CMD);
        }
        sb.append(this.mEffectivePolicyRaw.soundTriggerMode);
        if (this.mEffectivePolicyRaw.deferFullBackup) {
            sb.append("F");
        }
        if (this.mEffectivePolicyRaw.deferKeyValueBackup) {
            sb.append("K");
        }
        if (this.mEffectivePolicyRaw.enableFirewall) {
            sb.append("f");
        }
        if (this.mEffectivePolicyRaw.enableDataSaver) {
            sb.append("d");
        }
        if (this.mEffectivePolicyRaw.enableAdjustBrightness) {
            sb.append("b");
        }
        if (this.mEffectivePolicyRaw.disableLaunchBoost) {
            sb.append("l");
        }
        if (this.mEffectivePolicyRaw.disableOptionalSensors) {
            sb.append("S");
        }
        if (this.mEffectivePolicyRaw.disableAod) {
            sb.append("o");
        }
        if (this.mEffectivePolicyRaw.enableQuickDoze) {
            sb.append("q");
        }
        sb.append(this.mEffectivePolicyRaw.locationMode);
        this.mEventLogKeys = sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Policy {
        public final float adjustBrightnessFactor;
        public final boolean advertiseIsEnabled;
        public final boolean deferFullBackup;
        public final boolean deferKeyValueBackup;
        public final boolean disableAnimation;
        public final boolean disableAod;
        public final boolean disableLaunchBoost;
        public final boolean disableOptionalSensors;
        public final boolean disableVibration;
        public final boolean enableAdjustBrightness;
        public final boolean enableDataSaver;
        public final boolean enableFirewall;
        public final boolean enableNightMode;
        public final boolean enableQuickDoze;
        public final boolean forceAllAppsStandby;
        public final boolean forceBackgroundCheck;
        public final int locationMode;
        private final int mHashCode;
        public final int soundTriggerMode;

        Policy(float adjustBrightnessFactor, boolean advertiseIsEnabled, boolean deferFullBackup, boolean deferKeyValueBackup, boolean disableAnimation, boolean disableAod, boolean disableLaunchBoost, boolean disableOptionalSensors, boolean disableVibration, boolean enableAdjustBrightness, boolean enableDataSaver, boolean enableFirewall, boolean enableNightMode, boolean enableQuickDoze, boolean forceAllAppsStandby, boolean forceBackgroundCheck, int locationMode, int soundTriggerMode) {
            char c;
            this.adjustBrightnessFactor = Math.min(1.0f, Math.max(0.0f, adjustBrightnessFactor));
            this.advertiseIsEnabled = advertiseIsEnabled;
            this.deferFullBackup = deferFullBackup;
            this.deferKeyValueBackup = deferKeyValueBackup;
            this.disableAnimation = disableAnimation;
            this.disableAod = disableAod;
            this.disableLaunchBoost = disableLaunchBoost;
            this.disableOptionalSensors = disableOptionalSensors;
            this.disableVibration = disableVibration;
            this.enableAdjustBrightness = enableAdjustBrightness;
            this.enableDataSaver = enableDataSaver;
            this.enableFirewall = enableFirewall;
            this.enableNightMode = enableNightMode;
            this.enableQuickDoze = enableQuickDoze;
            this.forceAllAppsStandby = forceAllAppsStandby;
            this.forceBackgroundCheck = forceBackgroundCheck;
            if (locationMode < 0 || 4 < locationMode) {
                Slog.e(BatterySaverPolicy.TAG, "Invalid location mode: " + locationMode);
                this.locationMode = 0;
            } else {
                this.locationMode = locationMode;
            }
            if (soundTriggerMode < 0 || soundTriggerMode > 2) {
                Slog.e(BatterySaverPolicy.TAG, "Invalid SoundTrigger mode: " + soundTriggerMode);
                c = 0;
                this.soundTriggerMode = 0;
            } else {
                this.soundTriggerMode = soundTriggerMode;
                c = 0;
            }
            Object[] objArr = new Object[18];
            objArr[c] = Float.valueOf(adjustBrightnessFactor);
            objArr[1] = Boolean.valueOf(advertiseIsEnabled);
            objArr[2] = Boolean.valueOf(deferFullBackup);
            objArr[3] = Boolean.valueOf(deferKeyValueBackup);
            objArr[4] = Boolean.valueOf(disableAnimation);
            objArr[5] = Boolean.valueOf(disableAod);
            objArr[6] = Boolean.valueOf(disableLaunchBoost);
            objArr[7] = Boolean.valueOf(disableOptionalSensors);
            objArr[8] = Boolean.valueOf(disableVibration);
            objArr[9] = Boolean.valueOf(enableAdjustBrightness);
            objArr[10] = Boolean.valueOf(enableDataSaver);
            objArr[11] = Boolean.valueOf(enableFirewall);
            objArr[12] = Boolean.valueOf(enableNightMode);
            objArr[13] = Boolean.valueOf(enableQuickDoze);
            objArr[14] = Boolean.valueOf(forceAllAppsStandby);
            objArr[15] = Boolean.valueOf(forceBackgroundCheck);
            objArr[16] = Integer.valueOf(locationMode);
            objArr[17] = Integer.valueOf(soundTriggerMode);
            this.mHashCode = Objects.hash(objArr);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static Policy fromConfig(BatterySaverPolicyConfig config) {
            if (config == null) {
                Slog.e(BatterySaverPolicy.TAG, "Null config passed down to BatterySaverPolicy");
                return BatterySaverPolicy.OFF_POLICY;
            }
            config.getDeviceSpecificSettings();
            return new Policy(config.getAdjustBrightnessFactor(), config.getAdvertiseIsEnabled(), config.getDeferFullBackup(), config.getDeferKeyValueBackup(), config.getDisableAnimation(), config.getDisableAod(), config.getDisableLaunchBoost(), config.getDisableOptionalSensors(), config.getDisableVibration(), config.getEnableAdjustBrightness(), config.getEnableDataSaver(), config.getEnableFirewall(), config.getEnableNightMode(), config.getEnableQuickDoze(), config.getForceAllAppsStandby(), config.getForceBackgroundCheck(), config.getLocationMode(), config.getSoundTriggerMode());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BatterySaverPolicyConfig toConfig() {
            return new BatterySaverPolicyConfig.Builder().setAdjustBrightnessFactor(this.adjustBrightnessFactor).setAdvertiseIsEnabled(this.advertiseIsEnabled).setDeferFullBackup(this.deferFullBackup).setDeferKeyValueBackup(this.deferKeyValueBackup).setDisableAnimation(this.disableAnimation).setDisableAod(this.disableAod).setDisableLaunchBoost(this.disableLaunchBoost).setDisableOptionalSensors(this.disableOptionalSensors).setDisableVibration(this.disableVibration).setEnableAdjustBrightness(this.enableAdjustBrightness).setEnableDataSaver(this.enableDataSaver).setEnableFirewall(this.enableFirewall).setEnableNightMode(this.enableNightMode).setEnableQuickDoze(this.enableQuickDoze).setForceAllAppsStandby(this.forceAllAppsStandby).setForceBackgroundCheck(this.forceBackgroundCheck).setLocationMode(this.locationMode).setSoundTriggerMode(this.soundTriggerMode).build();
        }

        static Policy fromSettings(String settings, String deviceSpecificSettings, DeviceConfig.Properties properties, String configSuffix) {
            return fromSettings(settings, deviceSpecificSettings, properties, configSuffix, BatterySaverPolicy.OFF_POLICY);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static Policy fromSettings(String settings, String deviceSpecificSettings, DeviceConfig.Properties properties, String configSuffix, Policy defaultPolicy) {
            KeyValueListParser parser = new KeyValueListParser(',');
            String configSuffix2 = TextUtils.emptyIfNull(configSuffix);
            try {
                parser.setString(deviceSpecificSettings == null ? "" : deviceSpecificSettings);
            } catch (IllegalArgumentException e) {
                Slog.wtf(BatterySaverPolicy.TAG, "Bad device specific battery saver constants: " + deviceSpecificSettings);
            }
            try {
                parser.setString(settings != null ? settings : "");
            } catch (IllegalArgumentException e2) {
                Slog.wtf(BatterySaverPolicy.TAG, "Bad battery saver constants: " + settings);
            }
            float adjustBrightnessFactor = parser.getFloat(BatterySaverPolicy.KEY_ADJUST_BRIGHTNESS_FACTOR, properties.getFloat(BatterySaverPolicy.KEY_ADJUST_BRIGHTNESS_FACTOR + configSuffix2, defaultPolicy.adjustBrightnessFactor));
            boolean advertiseIsEnabled = parser.getBoolean(BatterySaverPolicy.KEY_ADVERTISE_IS_ENABLED, properties.getBoolean(BatterySaverPolicy.KEY_ADVERTISE_IS_ENABLED + configSuffix2, defaultPolicy.advertiseIsEnabled));
            boolean deferFullBackup = parser.getBoolean(BatterySaverPolicy.KEY_DEFER_FULL_BACKUP, properties.getBoolean(BatterySaverPolicy.KEY_DEFER_FULL_BACKUP + configSuffix2, defaultPolicy.deferFullBackup));
            boolean deferKeyValueBackup = parser.getBoolean(BatterySaverPolicy.KEY_DEFER_KEYVALUE_BACKUP, properties.getBoolean(BatterySaverPolicy.KEY_DEFER_KEYVALUE_BACKUP + configSuffix2, defaultPolicy.deferKeyValueBackup));
            boolean disableAnimation = parser.getBoolean(BatterySaverPolicy.KEY_DISABLE_ANIMATION, properties.getBoolean(BatterySaverPolicy.KEY_DISABLE_ANIMATION + configSuffix2, defaultPolicy.disableAnimation));
            boolean disableAod = parser.getBoolean(BatterySaverPolicy.KEY_DISABLE_AOD, properties.getBoolean(BatterySaverPolicy.KEY_DISABLE_AOD + configSuffix2, defaultPolicy.disableAod));
            boolean disableLaunchBoost = parser.getBoolean(BatterySaverPolicy.KEY_DISABLE_LAUNCH_BOOST, properties.getBoolean(BatterySaverPolicy.KEY_DISABLE_LAUNCH_BOOST + configSuffix2, defaultPolicy.disableLaunchBoost));
            boolean disableOptionalSensors = parser.getBoolean(BatterySaverPolicy.KEY_DISABLE_OPTIONAL_SENSORS, properties.getBoolean(BatterySaverPolicy.KEY_DISABLE_OPTIONAL_SENSORS + configSuffix2, defaultPolicy.disableOptionalSensors));
            boolean disableOptionalSensors2 = defaultPolicy.disableVibration;
            boolean disableVibrationConfig = parser.getBoolean(BatterySaverPolicy.KEY_DISABLE_VIBRATION, properties.getBoolean(BatterySaverPolicy.KEY_DISABLE_VIBRATION + configSuffix2, disableOptionalSensors2));
            boolean disableVibrationConfig2 = defaultPolicy.enableAdjustBrightness;
            boolean enableBrightnessAdjustment = parser.getBoolean(BatterySaverPolicy.KEY_ENABLE_BRIGHTNESS_ADJUSTMENT, properties.getBoolean(BatterySaverPolicy.KEY_ENABLE_BRIGHTNESS_ADJUSTMENT + configSuffix2, disableVibrationConfig2));
            boolean enableBrightnessAdjustment2 = defaultPolicy.enableDataSaver;
            boolean enableDataSaver = parser.getBoolean(BatterySaverPolicy.KEY_ENABLE_DATASAVER, properties.getBoolean(BatterySaverPolicy.KEY_ENABLE_DATASAVER + configSuffix2, enableBrightnessAdjustment2));
            boolean enableDataSaver2 = defaultPolicy.enableFirewall;
            boolean enableFirewall = parser.getBoolean(BatterySaverPolicy.KEY_ENABLE_FIREWALL, properties.getBoolean(BatterySaverPolicy.KEY_ENABLE_FIREWALL + configSuffix2, enableDataSaver2));
            boolean enableFirewall2 = defaultPolicy.enableNightMode;
            boolean enableNightMode = parser.getBoolean(BatterySaverPolicy.KEY_ENABLE_NIGHT_MODE, properties.getBoolean(BatterySaverPolicy.KEY_ENABLE_NIGHT_MODE + configSuffix2, enableFirewall2));
            boolean enableNightMode2 = defaultPolicy.enableQuickDoze;
            boolean enableQuickDoze = parser.getBoolean(BatterySaverPolicy.KEY_ENABLE_QUICK_DOZE, properties.getBoolean(BatterySaverPolicy.KEY_ENABLE_QUICK_DOZE + configSuffix2, enableNightMode2));
            boolean enableQuickDoze2 = defaultPolicy.forceAllAppsStandby;
            boolean forceAllAppsStandby = parser.getBoolean(BatterySaverPolicy.KEY_FORCE_ALL_APPS_STANDBY, properties.getBoolean(BatterySaverPolicy.KEY_FORCE_ALL_APPS_STANDBY + configSuffix2, enableQuickDoze2));
            boolean forceAllAppsStandby2 = defaultPolicy.forceBackgroundCheck;
            boolean forceBackgroundCheck = parser.getBoolean(BatterySaverPolicy.KEY_FORCE_BACKGROUND_CHECK, properties.getBoolean(BatterySaverPolicy.KEY_FORCE_BACKGROUND_CHECK + configSuffix2, forceAllAppsStandby2));
            int locationMode = parser.getInt(BatterySaverPolicy.KEY_LOCATION_MODE, properties.getInt(BatterySaverPolicy.KEY_LOCATION_MODE + configSuffix2, defaultPolicy.locationMode));
            int locationMode2 = defaultPolicy.soundTriggerMode;
            int soundTriggerMode = parser.getInt(BatterySaverPolicy.KEY_SOUNDTRIGGER_MODE, properties.getInt(BatterySaverPolicy.KEY_SOUNDTRIGGER_MODE + configSuffix2, locationMode2));
            return new Policy(adjustBrightnessFactor, advertiseIsEnabled, deferFullBackup, deferKeyValueBackup, disableAnimation, disableAod, disableLaunchBoost, disableOptionalSensors, disableVibrationConfig, enableBrightnessAdjustment, enableDataSaver, enableFirewall, enableNightMode, enableQuickDoze, forceAllAppsStandby, forceBackgroundCheck, locationMode, soundTriggerMode);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Policy) {
                Policy other = (Policy) obj;
                return Float.compare(other.adjustBrightnessFactor, this.adjustBrightnessFactor) == 0 && this.advertiseIsEnabled == other.advertiseIsEnabled && this.deferFullBackup == other.deferFullBackup && this.deferKeyValueBackup == other.deferKeyValueBackup && this.disableAnimation == other.disableAnimation && this.disableAod == other.disableAod && this.disableLaunchBoost == other.disableLaunchBoost && this.disableOptionalSensors == other.disableOptionalSensors && this.disableVibration == other.disableVibration && this.enableAdjustBrightness == other.enableAdjustBrightness && this.enableDataSaver == other.enableDataSaver && this.enableFirewall == other.enableFirewall && this.enableNightMode == other.enableNightMode && this.enableQuickDoze == other.enableQuickDoze && this.forceAllAppsStandby == other.forceAllAppsStandby && this.forceBackgroundCheck == other.forceBackgroundCheck && this.locationMode == other.locationMode && this.soundTriggerMode == other.soundTriggerMode;
            }
            return false;
        }

        public int hashCode() {
            return this.mHashCode;
        }
    }

    public PowerSaveState getBatterySaverPolicy(int type) {
        synchronized (this.mLock) {
            Policy currPolicy = getCurrentPolicyLocked();
            PowerSaveState.Builder builder = new PowerSaveState.Builder().setGlobalBatterySaverEnabled(currPolicy.advertiseIsEnabled);
            boolean soundTriggerBatterySaverEnabled = false;
            switch (type) {
                case 1:
                    if (currPolicy.advertiseIsEnabled || currPolicy.locationMode != 0) {
                        soundTriggerBatterySaverEnabled = true;
                    }
                    return builder.setBatterySaverEnabled(soundTriggerBatterySaverEnabled).setLocationMode(currPolicy.locationMode).build();
                case 2:
                    return builder.setBatterySaverEnabled(currPolicy.disableVibration).build();
                case 3:
                    return builder.setBatterySaverEnabled(currPolicy.disableAnimation).build();
                case 4:
                    return builder.setBatterySaverEnabled(currPolicy.deferFullBackup).build();
                case 5:
                    return builder.setBatterySaverEnabled(currPolicy.deferKeyValueBackup).build();
                case 6:
                    return builder.setBatterySaverEnabled(currPolicy.enableFirewall).build();
                case 7:
                    return builder.setBatterySaverEnabled(currPolicy.enableAdjustBrightness).setBrightnessFactor(currPolicy.adjustBrightnessFactor).build();
                case 8:
                    if (currPolicy.advertiseIsEnabled || currPolicy.soundTriggerMode != 0) {
                        soundTriggerBatterySaverEnabled = true;
                    }
                    return builder.setBatterySaverEnabled(soundTriggerBatterySaverEnabled).setSoundTriggerMode(currPolicy.soundTriggerMode).build();
                case 9:
                default:
                    boolean isEnabled = currPolicy.advertiseIsEnabled;
                    return builder.setBatterySaverEnabled(isEnabled).build();
                case 10:
                    return builder.setBatterySaverEnabled(currPolicy.enableDataSaver).build();
                case 11:
                    return builder.setBatterySaverEnabled(currPolicy.forceAllAppsStandby).build();
                case 12:
                    return builder.setBatterySaverEnabled(currPolicy.forceBackgroundCheck).build();
                case 13:
                    return builder.setBatterySaverEnabled(currPolicy.disableOptionalSensors).build();
                case 14:
                    return builder.setBatterySaverEnabled(currPolicy.disableAod).build();
                case 15:
                    return builder.setBatterySaverEnabled(currPolicy.enableQuickDoze).build();
                case 16:
                    return builder.setBatterySaverEnabled(currPolicy.enableNightMode).build();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setPolicyLevel(int level) {
        synchronized (this.mLock) {
            int i = this.mPolicyLevel;
            if (i == level) {
                return false;
            }
            if (i == 2) {
                this.mFullPolicy = this.mDefaultFullPolicy;
            }
            switch (level) {
                case 0:
                case 1:
                case 2:
                    this.mPolicyLevel = level;
                    updatePolicyDependenciesLocked();
                    return true;
                default:
                    Slog.wtf(TAG, "setPolicyLevel invalid level given: " + level);
                    return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Policy getPolicyLocked(int policyLevel) {
        switch (policyLevel) {
            case 0:
                return OFF_POLICY;
            case 1:
                return this.mAdaptivePolicy;
            case 2:
                return this.mFullPolicy;
            default:
                throw new IllegalArgumentException("getPolicyLocked: incorrect policy level provided - " + policyLevel);
        }
    }

    private boolean maybeUpdateDefaultFullPolicy(Policy p) {
        boolean fullPolicyChanged = false;
        if (!this.mDefaultFullPolicy.equals(p)) {
            boolean isDefaultFullPolicyOverridden = !this.mDefaultFullPolicy.equals(this.mFullPolicy);
            if (!isDefaultFullPolicyOverridden) {
                this.mFullPolicy = p;
                fullPolicyChanged = this.mPolicyLevel == 2;
            }
            this.mDefaultFullPolicy = p;
        }
        return fullPolicyChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setFullPolicyLocked(Policy p) {
        if (p == null) {
            Slog.wtf(TAG, "setFullPolicy given null policy");
            return false;
        } else if (this.mFullPolicy.equals(p)) {
            return false;
        } else {
            this.mFullPolicy = p;
            if (this.mPolicyLevel != 2) {
                return false;
            }
            updatePolicyDependenciesLocked();
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setAdaptivePolicyLocked(Policy p) {
        if (p == null) {
            Slog.wtf(TAG, "setAdaptivePolicy given null policy");
            return false;
        } else if (this.mAdaptivePolicy.equals(p)) {
            return false;
        } else {
            this.mAdaptivePolicy = p;
            if (this.mPolicyLevel != 1) {
                return false;
            }
            updatePolicyDependenciesLocked();
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resetAdaptivePolicyLocked() {
        return setAdaptivePolicyLocked(this.mDefaultAdaptivePolicy);
    }

    private Policy getCurrentPolicyLocked() {
        return this.mEffectivePolicyRaw;
    }

    private Policy getCurrentRawPolicyLocked() {
        switch (this.mPolicyLevel) {
            case 1:
                return this.mAdaptivePolicy;
            case 2:
                return this.mFullPolicy;
            default:
                return OFF_POLICY;
        }
    }

    public int getGpsMode() {
        int i;
        synchronized (this.mLock) {
            i = getCurrentPolicyLocked().locationMode;
        }
        return i;
    }

    public boolean isLaunchBoostDisabled() {
        boolean z;
        synchronized (this.mLock) {
            z = getCurrentPolicyLocked().disableLaunchBoost;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldAdvertiseIsEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = getCurrentPolicyLocked().advertiseIsEnabled;
        }
        return z;
    }

    public String toEventLogString() {
        String str;
        synchronized (this.mLock) {
            str = this.mEventLogKeys;
        }
        return str;
    }

    public void dump(PrintWriter pw) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        synchronized (this.mLock) {
            ipw.println();
            this.mBatterySavingStats.dump(ipw);
            ipw.println();
            ipw.println("Battery saver policy (*NOTE* they only apply when battery saver is ON):");
            ipw.increaseIndent();
            ipw.println("Settings: battery_saver_constants");
            ipw.increaseIndent();
            ipw.println("value: " + this.mSettings);
            ipw.decreaseIndent();
            ipw.println("Settings: " + this.mDeviceSpecificSettingsSource);
            ipw.increaseIndent();
            ipw.println("value: " + this.mDeviceSpecificSettings);
            ipw.decreaseIndent();
            ipw.println("DeviceConfig: battery_saver");
            ipw.increaseIndent();
            Set<String> keys = this.mLastDeviceConfigProperties.getKeyset();
            if (keys.size() == 0) {
                ipw.println("N/A");
            } else {
                for (String key : keys) {
                    ipw.print(key);
                    ipw.print(": ");
                    ipw.println(this.mLastDeviceConfigProperties.getString(key, (String) null));
                }
            }
            ipw.decreaseIndent();
            ipw.println("mAccessibilityEnabled=" + this.mAccessibilityEnabled.get());
            ipw.println("mAutomotiveProjectionActive=" + this.mAutomotiveProjectionActive.get());
            ipw.println("mPolicyLevel=" + this.mPolicyLevel);
            dumpPolicyLocked(ipw, "default full", this.mDefaultFullPolicy);
            dumpPolicyLocked(ipw, "current full", this.mFullPolicy);
            dumpPolicyLocked(ipw, "default adaptive", this.mDefaultAdaptivePolicy);
            dumpPolicyLocked(ipw, "current adaptive", this.mAdaptivePolicy);
            dumpPolicyLocked(ipw, "effective", this.mEffectivePolicyRaw);
            ipw.decreaseIndent();
        }
    }

    private void dumpPolicyLocked(IndentingPrintWriter pw, String label, Policy p) {
        pw.println();
        pw.println("Policy '" + label + "'");
        pw.increaseIndent();
        pw.println("advertise_is_enabled=" + p.advertiseIsEnabled);
        pw.println("disable_vibration=" + p.disableVibration);
        pw.println("disable_animation=" + p.disableAnimation);
        pw.println("defer_full_backup=" + p.deferFullBackup);
        pw.println("defer_keyvalue_backup=" + p.deferKeyValueBackup);
        pw.println("enable_firewall=" + p.enableFirewall);
        pw.println("enable_datasaver=" + p.enableDataSaver);
        pw.println("disable_launch_boost=" + p.disableLaunchBoost);
        pw.println("enable_brightness_adjustment=" + p.enableAdjustBrightness);
        pw.println("adjust_brightness_factor=" + p.adjustBrightnessFactor);
        pw.println("location_mode=" + p.locationMode);
        pw.println("force_all_apps_standby=" + p.forceAllAppsStandby);
        pw.println("force_background_check=" + p.forceBackgroundCheck);
        pw.println("disable_optional_sensors=" + p.disableOptionalSensors);
        pw.println("disable_aod=" + p.disableAod);
        pw.println("soundtrigger_mode=" + p.soundTriggerMode);
        pw.println("enable_quick_doze=" + p.enableQuickDoze);
        pw.println("enable_night_mode=" + p.enableNightMode);
        pw.decreaseIndent();
    }

    private void dumpMap(PrintWriter pw, ArrayMap<String, String> map) {
        if (map == null || map.size() == 0) {
            pw.println("N/A");
            return;
        }
        int size = map.size();
        for (int i = 0; i < size; i++) {
            pw.print(map.keyAt(i));
            pw.print(": '");
            pw.print(map.valueAt(i));
            pw.println("'");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class PolicyBoolean {
        private final String mDebugName;
        private boolean mValue;

        private PolicyBoolean(String debugName) {
            this.mDebugName = debugName;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void initialize(boolean initialValue) {
            synchronized (BatterySaverPolicy.this.mLock) {
                this.mValue = initialValue;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean get() {
            boolean z;
            synchronized (BatterySaverPolicy.this.mLock) {
                z = this.mValue;
            }
            return z;
        }

        void update(boolean newValue) {
            synchronized (BatterySaverPolicy.this.mLock) {
                if (this.mValue != newValue) {
                    Slog.d(BatterySaverPolicy.TAG, this.mDebugName + " changed to " + newValue + ", updating policy.");
                    this.mValue = newValue;
                    BatterySaverPolicy.this.updatePolicyDependenciesLocked();
                    BatterySaverPolicy.this.maybeNotifyListenersOfPolicyChange();
                }
            }
        }
    }
}
