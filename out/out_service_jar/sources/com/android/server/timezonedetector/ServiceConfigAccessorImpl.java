package com.android.server.timezonedetector;

import android.app.ActivityManagerInternal;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.location.LocationManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import com.android.server.LocalServices;
import com.android.server.timedetector.ServerFlags;
import com.android.server.timezonedetector.ConfigurationInternal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
/* loaded from: classes2.dex */
public final class ServiceConfigAccessorImpl implements ServiceConfigAccessor {
    private static ServiceConfigAccessor sInstance;
    private final List<ConfigurationChangeListener> mConfigurationInternalListeners = new ArrayList();
    private final Context mContext;
    private final ContentResolver mCr;
    private final LocationManager mLocationManager;
    private boolean mRecordStateChangesForTests;
    private final ServerFlags mServerFlags;
    private String mTestPrimaryLocationTimeZoneProviderMode;
    private String mTestPrimaryLocationTimeZoneProviderPackageName;
    private String mTestSecondaryLocationTimeZoneProviderMode;
    private String mTestSecondaryLocationTimeZoneProviderPackageName;
    private final UserManager mUserManager;
    private static final Set<String> CONFIGURATION_INTERNAL_SERVER_FLAGS_KEYS_TO_WATCH = Set.of(ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_FEATURE_SUPPORTED, ServerFlags.KEY_PRIMARY_LTZP_MODE_OVERRIDE, ServerFlags.KEY_SECONDARY_LTZP_MODE_OVERRIDE, ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_RUN_IN_BACKGROUND_ENABLED, ServerFlags.KEY_ENHANCED_METRICS_COLLECTION_ENABLED, ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_SETTING_ENABLED_DEFAULT, ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_SETTING_ENABLED_OVERRIDE, ServerFlags.KEY_TIME_ZONE_DETECTOR_TELEPHONY_FALLBACK_SUPPORTED);
    private static final Set<String> LOCATION_TIME_ZONE_MANAGER_SERVER_FLAGS_KEYS_TO_WATCH = Set.of(ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_FEATURE_SUPPORTED, ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_RUN_IN_BACKGROUND_ENABLED, ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_SETTING_ENABLED_DEFAULT, ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_SETTING_ENABLED_OVERRIDE, ServerFlags.KEY_PRIMARY_LTZP_MODE_OVERRIDE, ServerFlags.KEY_SECONDARY_LTZP_MODE_OVERRIDE, ServerFlags.KEY_LTZP_INITIALIZATION_TIMEOUT_MILLIS, ServerFlags.KEY_LTZP_INITIALIZATION_TIMEOUT_FUZZ_MILLIS, ServerFlags.KEY_LTZP_EVENT_FILTERING_AGE_THRESHOLD_MILLIS, ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_UNCERTAINTY_DELAY_MILLIS);
    private static final Duration DEFAULT_LTZP_INITIALIZATION_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration DEFAULT_LTZP_INITIALIZATION_TIMEOUT_FUZZ = Duration.ofMinutes(1);
    private static final Duration DEFAULT_LTZP_UNCERTAINTY_DELAY = Duration.ofMinutes(5);
    private static final Duration DEFAULT_LTZP_EVENT_FILTER_AGE_THRESHOLD = Duration.ofMinutes(1);
    private static final Object SLOCK = new Object();

    private ServiceConfigAccessorImpl(Context context) {
        Context context2 = (Context) Objects.requireNonNull(context);
        this.mContext = context2;
        this.mCr = context.getContentResolver();
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mLocationManager = (LocationManager) context.getSystemService(LocationManager.class);
        ServerFlags serverFlags = ServerFlags.getInstance(context2);
        this.mServerFlags = serverFlags;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.location.MODE_CHANGED");
        context2.registerReceiverForAllUsers(new BroadcastReceiver() { // from class: com.android.server.timezonedetector.ServiceConfigAccessorImpl.1
            {
                ServiceConfigAccessorImpl.this = this;
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context3, Intent intent) {
                ServiceConfigAccessorImpl.this.handleConfigurationInternalChangeOnMainThread();
            }
        }, filter, null, null);
        ContentResolver contentResolver = context2.getContentResolver();
        ContentObserver contentObserver = new ContentObserver(context2.getMainThreadHandler()) { // from class: com.android.server.timezonedetector.ServiceConfigAccessorImpl.2
            {
                ServiceConfigAccessorImpl.this = this;
            }

            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                ServiceConfigAccessorImpl.this.handleConfigurationInternalChangeOnMainThread();
            }
        };
        contentResolver.registerContentObserver(Settings.Global.getUriFor("auto_time_zone"), true, contentObserver);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("location_time_zone_detection_enabled"), true, contentObserver, -1);
        serverFlags.addListener(new ConfigurationChangeListener() { // from class: com.android.server.timezonedetector.ServiceConfigAccessorImpl$$ExternalSyntheticLambda0
            @Override // com.android.server.timezonedetector.ConfigurationChangeListener
            public final void onChange() {
                ServiceConfigAccessorImpl.this.handleConfigurationInternalChangeOnMainThread();
            }
        }, CONFIGURATION_INTERNAL_SERVER_FLAGS_KEYS_TO_WATCH);
    }

    public static ServiceConfigAccessor getInstance(Context context) {
        ServiceConfigAccessor serviceConfigAccessor;
        synchronized (SLOCK) {
            if (sInstance == null) {
                sInstance = new ServiceConfigAccessorImpl(context);
            }
            serviceConfigAccessor = sInstance;
        }
        return serviceConfigAccessor;
    }

    public synchronized void handleConfigurationInternalChangeOnMainThread() {
        for (ConfigurationChangeListener changeListener : this.mConfigurationInternalListeners) {
            changeListener.onChange();
        }
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized void addConfigurationInternalChangeListener(ConfigurationChangeListener listener) {
        this.mConfigurationInternalListeners.add((ConfigurationChangeListener) Objects.requireNonNull(listener));
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized void removeConfigurationInternalChangeListener(ConfigurationChangeListener listener) {
        this.mConfigurationInternalListeners.remove(Objects.requireNonNull(listener));
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized ConfigurationInternal getCurrentUserConfigurationInternal() {
        int currentUserId;
        currentUserId = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getCurrentUserId();
        return getConfigurationInternal(currentUserId);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized boolean updateConfiguration(int userId, TimeZoneConfiguration requestedConfiguration) {
        Objects.requireNonNull(requestedConfiguration);
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = getConfigurationInternal(userId).createCapabilitiesAndConfig();
        TimeZoneCapabilities capabilities = capabilitiesAndConfig.getCapabilities();
        TimeZoneConfiguration oldConfiguration = capabilitiesAndConfig.getConfiguration();
        TimeZoneConfiguration newConfiguration = capabilities.tryApplyConfigChanges(oldConfiguration, requestedConfiguration);
        if (newConfiguration == null) {
            return false;
        }
        storeConfiguration(userId, newConfiguration);
        return true;
    }

    private void storeConfiguration(int userId, TimeZoneConfiguration configuration) {
        Objects.requireNonNull(configuration);
        if (isAutoDetectionFeatureSupported()) {
            boolean autoDetectionEnabled = configuration.isAutoDetectionEnabled();
            setAutoDetectionEnabledIfRequired(autoDetectionEnabled);
            if (!getGeoDetectionSettingEnabledOverride().isPresent() && isGeoTimeZoneDetectionFeatureSupported()) {
                boolean geoDetectionEnabledSetting = configuration.isGeoDetectionEnabled();
                setGeoDetectionEnabledSettingIfRequired(userId, geoDetectionEnabledSetting);
            }
        }
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized ConfigurationInternal getConfigurationInternal(int userId) {
        return new ConfigurationInternal.Builder(userId).setTelephonyDetectionFeatureSupported(isTelephonyTimeZoneDetectionFeatureSupported()).setGeoDetectionFeatureSupported(isGeoTimeZoneDetectionFeatureSupported()).setTelephonyFallbackSupported(isTelephonyFallbackSupported()).setGeoDetectionRunInBackgroundEnabled(getGeoDetectionRunInBackgroundEnabled()).setEnhancedMetricsCollectionEnabled(isEnhancedMetricsCollectionEnabled()).setAutoDetectionEnabledSetting(getAutoDetectionEnabledSetting()).setUserConfigAllowed(isUserConfigAllowed(userId)).setLocationEnabledSetting(getLocationEnabledSetting(userId)).setGeoDetectionEnabledSetting(getGeoDetectionEnabledSetting(userId)).build();
    }

    private void setAutoDetectionEnabledIfRequired(boolean enabled) {
        if (getAutoDetectionEnabledSetting() != enabled) {
            Settings.Global.putInt(this.mCr, "auto_time_zone", enabled ? 1 : 0);
        }
    }

    private boolean getLocationEnabledSetting(int userId) {
        return this.mLocationManager.isLocationEnabledForUser(UserHandle.of(userId));
    }

    private boolean isUserConfigAllowed(int userId) {
        UserHandle userHandle = UserHandle.of(userId);
        return !this.mUserManager.hasUserRestriction("no_config_date_time", userHandle);
    }

    private boolean getAutoDetectionEnabledSetting() {
        return Settings.Global.getInt(this.mCr, "auto_time_zone", 1) > 0;
    }

    private boolean getGeoDetectionEnabledSetting(int userId) {
        Optional<Boolean> override = getGeoDetectionSettingEnabledOverride();
        if (override.isPresent()) {
            return override.get().booleanValue();
        }
        return Settings.Secure.getIntForUser(this.mCr, "location_time_zone_detection_enabled", isGeoDetectionEnabledForUsersByDefault() ? 1 : 0, userId) != 0;
    }

    private void setGeoDetectionEnabledSettingIfRequired(int userId, boolean enabled) {
        if (getGeoDetectionEnabledSetting(userId) != enabled) {
            Settings.Secure.putIntForUser(this.mCr, "location_time_zone_detection_enabled", enabled ? 1 : 0, userId);
        }
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public void addLocationTimeZoneManagerConfigListener(ConfigurationChangeListener listener) {
        this.mServerFlags.addListener(listener, LOCATION_TIME_ZONE_MANAGER_SERVER_FLAGS_KEYS_TO_WATCH);
    }

    private boolean isAutoDetectionFeatureSupported() {
        return isTelephonyTimeZoneDetectionFeatureSupported() || isGeoTimeZoneDetectionFeatureSupported();
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public boolean isTelephonyTimeZoneDetectionFeatureSupported() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.telephony");
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public boolean isGeoTimeZoneDetectionFeatureSupportedInConfig() {
        return this.mContext.getResources().getBoolean(17891634);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public boolean isGeoTimeZoneDetectionFeatureSupported() {
        return isGeoTimeZoneDetectionFeatureSupportedInConfig() && isGeoTimeZoneDetectionFeatureSupportedInternal() && atLeastOneProviderIsEnabled();
    }

    private boolean atLeastOneProviderIsEnabled() {
        return (Objects.equals(getPrimaryLocationTimeZoneProviderMode(), ServiceConfigAccessor.PROVIDER_MODE_DISABLED) && Objects.equals(getSecondaryLocationTimeZoneProviderMode(), ServiceConfigAccessor.PROVIDER_MODE_DISABLED)) ? false : true;
    }

    private boolean isGeoTimeZoneDetectionFeatureSupportedInternal() {
        return this.mServerFlags.getBoolean(ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_FEATURE_SUPPORTED, true);
    }

    private boolean getGeoDetectionRunInBackgroundEnabled() {
        return this.mServerFlags.getBoolean(ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_RUN_IN_BACKGROUND_ENABLED, false);
    }

    private boolean isEnhancedMetricsCollectionEnabled() {
        return this.mServerFlags.getBoolean(ServerFlags.KEY_ENHANCED_METRICS_COLLECTION_ENABLED, false);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized String getPrimaryLocationTimeZoneProviderPackageName() {
        if (this.mTestPrimaryLocationTimeZoneProviderMode != null) {
            return this.mTestPrimaryLocationTimeZoneProviderPackageName;
        }
        return this.mContext.getResources().getString(17040018);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized void setTestPrimaryLocationTimeZoneProviderPackageName(String testPrimaryLocationTimeZoneProviderPackageName) {
        this.mTestPrimaryLocationTimeZoneProviderPackageName = testPrimaryLocationTimeZoneProviderPackageName;
        this.mTestPrimaryLocationTimeZoneProviderMode = testPrimaryLocationTimeZoneProviderPackageName == null ? ServiceConfigAccessor.PROVIDER_MODE_DISABLED : ServiceConfigAccessor.PROVIDER_MODE_ENABLED;
        this.mContext.getMainThreadHandler().post(new ServiceConfigAccessorImpl$$ExternalSyntheticLambda1(this));
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized boolean isTestPrimaryLocationTimeZoneProvider() {
        return this.mTestPrimaryLocationTimeZoneProviderMode != null;
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized String getSecondaryLocationTimeZoneProviderPackageName() {
        if (this.mTestSecondaryLocationTimeZoneProviderMode != null) {
            return this.mTestSecondaryLocationTimeZoneProviderPackageName;
        }
        return this.mContext.getResources().getString(17040034);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized void setTestSecondaryLocationTimeZoneProviderPackageName(String testSecondaryLocationTimeZoneProviderPackageName) {
        this.mTestSecondaryLocationTimeZoneProviderPackageName = testSecondaryLocationTimeZoneProviderPackageName;
        this.mTestSecondaryLocationTimeZoneProviderMode = testSecondaryLocationTimeZoneProviderPackageName == null ? ServiceConfigAccessor.PROVIDER_MODE_DISABLED : ServiceConfigAccessor.PROVIDER_MODE_ENABLED;
        this.mContext.getMainThreadHandler().post(new ServiceConfigAccessorImpl$$ExternalSyntheticLambda1(this));
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized boolean isTestSecondaryLocationTimeZoneProvider() {
        return this.mTestSecondaryLocationTimeZoneProviderMode != null;
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized void setRecordStateChangesForTests(boolean enabled) {
        this.mRecordStateChangesForTests = enabled;
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized boolean getRecordStateChangesForTests() {
        return this.mRecordStateChangesForTests;
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized String getPrimaryLocationTimeZoneProviderMode() {
        String str = this.mTestPrimaryLocationTimeZoneProviderMode;
        if (str != null) {
            return str;
        }
        return this.mServerFlags.getOptionalString(ServerFlags.KEY_PRIMARY_LTZP_MODE_OVERRIDE).orElse(getPrimaryLocationTimeZoneProviderModeFromConfig());
    }

    private synchronized String getPrimaryLocationTimeZoneProviderModeFromConfig() {
        return getConfigBoolean(17891644) ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED;
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized String getSecondaryLocationTimeZoneProviderMode() {
        String str = this.mTestSecondaryLocationTimeZoneProviderMode;
        if (str != null) {
            return str;
        }
        return this.mServerFlags.getOptionalString(ServerFlags.KEY_SECONDARY_LTZP_MODE_OVERRIDE).orElse(getSecondaryLocationTimeZoneProviderModeFromConfig());
    }

    private synchronized String getSecondaryLocationTimeZoneProviderModeFromConfig() {
        return getConfigBoolean(17891647) ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED;
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public boolean isGeoDetectionEnabledForUsersByDefault() {
        return this.mServerFlags.getBoolean(ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_SETTING_ENABLED_DEFAULT, false);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public Optional<Boolean> getGeoDetectionSettingEnabledOverride() {
        return this.mServerFlags.getOptionalBoolean(ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_SETTING_ENABLED_OVERRIDE);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public Duration getLocationTimeZoneProviderInitializationTimeout() {
        return this.mServerFlags.getDurationFromMillis(ServerFlags.KEY_LTZP_INITIALIZATION_TIMEOUT_MILLIS, DEFAULT_LTZP_INITIALIZATION_TIMEOUT);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public Duration getLocationTimeZoneProviderInitializationTimeoutFuzz() {
        return this.mServerFlags.getDurationFromMillis(ServerFlags.KEY_LTZP_INITIALIZATION_TIMEOUT_FUZZ_MILLIS, DEFAULT_LTZP_INITIALIZATION_TIMEOUT_FUZZ);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public Duration getLocationTimeZoneUncertaintyDelay() {
        return this.mServerFlags.getDurationFromMillis(ServerFlags.KEY_LOCATION_TIME_ZONE_DETECTION_UNCERTAINTY_DELAY_MILLIS, DEFAULT_LTZP_UNCERTAINTY_DELAY);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public Duration getLocationTimeZoneProviderEventFilteringAgeThreshold() {
        return this.mServerFlags.getDurationFromMillis(ServerFlags.KEY_LTZP_EVENT_FILTERING_AGE_THRESHOLD_MILLIS, DEFAULT_LTZP_EVENT_FILTER_AGE_THRESHOLD);
    }

    @Override // com.android.server.timezonedetector.ServiceConfigAccessor
    public synchronized void resetVolatileTestConfig() {
        this.mTestPrimaryLocationTimeZoneProviderPackageName = null;
        this.mTestPrimaryLocationTimeZoneProviderMode = null;
        this.mTestSecondaryLocationTimeZoneProviderPackageName = null;
        this.mTestSecondaryLocationTimeZoneProviderMode = null;
        this.mRecordStateChangesForTests = false;
        this.mContext.getMainThreadHandler().post(new ServiceConfigAccessorImpl$$ExternalSyntheticLambda1(this));
    }

    private boolean isTelephonyFallbackSupported() {
        return this.mServerFlags.getBoolean(ServerFlags.KEY_TIME_ZONE_DETECTOR_TELEPHONY_FALLBACK_SUPPORTED, getConfigBoolean(17891776));
    }

    private boolean getConfigBoolean(int providerEnabledConfigId) {
        Resources resources = this.mContext.getResources();
        return resources.getBoolean(providerEnabledConfigId);
    }
}
