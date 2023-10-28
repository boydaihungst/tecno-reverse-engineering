package com.android.server.location.injector;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.PackageTagsList;
import android.os.RemoteException;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.SystemConfig;
import com.android.server.location.LocationManagerService;
import com.android.server.location.injector.SettingsHelper;
import java.io.FileDescriptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class SystemSettingsHelper extends SettingsHelper {
    private static final long DEFAULT_BACKGROUND_THROTTLE_INTERVAL_MS = 1800000;
    private static final long DEFAULT_BACKGROUND_THROTTLE_PROXIMITY_ALERT_INTERVAL_MS = 1800000;
    private static final float DEFAULT_COARSE_LOCATION_ACCURACY_M = 2000.0f;
    private static final String LOCATION_PACKAGE_BLACKLIST = "locationPackagePrefixBlacklist";
    private static final String LOCATION_PACKAGE_WHITELIST = "locationPackagePrefixWhitelist";
    private final LongGlobalSetting mBackgroundThrottleIntervalMs;
    private final StringSetCachedGlobalSetting mBackgroundThrottlePackageWhitelist;
    private final Context mContext;
    private final BooleanGlobalSetting mGnssMeasurementFullTracking;
    private final IntegerSecureSetting mLocationMode;
    private final StringListCachedSecureSetting mLocationPackageBlacklist;
    private final StringListCachedSecureSetting mLocationPackageWhitelist;
    private final PackageTagsListSetting mAdasPackageAllowlist = new PackageTagsListSetting("adas_settings_allowlist", new Supplier() { // from class: com.android.server.location.injector.SystemSettingsHelper$$ExternalSyntheticLambda1
        @Override // java.util.function.Supplier
        public final Object get() {
            ArrayMap allowAdasLocationSettings;
            allowAdasLocationSettings = SystemConfig.getInstance().getAllowAdasLocationSettings();
            return allowAdasLocationSettings;
        }
    });
    private final PackageTagsListSetting mIgnoreSettingsPackageAllowlist = new PackageTagsListSetting("ignore_settings_allowlist", new Supplier() { // from class: com.android.server.location.injector.SystemSettingsHelper$$ExternalSyntheticLambda2
        @Override // java.util.function.Supplier
        public final Object get() {
            ArrayMap allowIgnoreLocationSettings;
            allowIgnoreLocationSettings = SystemConfig.getInstance().getAllowIgnoreLocationSettings();
            return allowIgnoreLocationSettings;
        }
    });

    public SystemSettingsHelper(Context context) {
        this.mContext = context;
        this.mLocationMode = new IntegerSecureSetting(context, "location_mode", FgThread.getHandler());
        this.mBackgroundThrottleIntervalMs = new LongGlobalSetting(context, "location_background_throttle_interval_ms", FgThread.getHandler());
        this.mGnssMeasurementFullTracking = new BooleanGlobalSetting(context, "enable_gnss_raw_meas_full_tracking", FgThread.getHandler());
        this.mLocationPackageBlacklist = new StringListCachedSecureSetting(context, LOCATION_PACKAGE_BLACKLIST, FgThread.getHandler());
        this.mLocationPackageWhitelist = new StringListCachedSecureSetting(context, LOCATION_PACKAGE_WHITELIST, FgThread.getHandler());
        this.mBackgroundThrottlePackageWhitelist = new StringSetCachedGlobalSetting(context, "location_background_throttle_package_whitelist", new Supplier() { // from class: com.android.server.location.injector.SystemSettingsHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                ArraySet allowUnthrottledLocation;
                allowUnthrottledLocation = SystemConfig.getInstance().getAllowUnthrottledLocation();
                return allowUnthrottledLocation;
            }
        }, FgThread.getHandler());
    }

    public void onSystemReady() {
        this.mLocationMode.register();
        this.mBackgroundThrottleIntervalMs.register();
        this.mLocationPackageBlacklist.register();
        this.mLocationPackageWhitelist.register();
        this.mBackgroundThrottlePackageWhitelist.register();
        this.mIgnoreSettingsPackageAllowlist.register();
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public boolean isLocationEnabled(int userId) {
        return this.mLocationMode.getValueForUser(0, userId) != 0;
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void setLocationEnabled(boolean enabled, int userId) {
        int i;
        long identity = Binder.clearCallingIdentity();
        try {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            if (enabled) {
                i = 3;
            } else {
                i = 0;
            }
            Settings.Secure.putIntForUser(contentResolver, "location_mode", i, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void addOnLocationEnabledChangedListener(SettingsHelper.UserSettingChangedListener listener) {
        this.mLocationMode.addListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void removeOnLocationEnabledChangedListener(SettingsHelper.UserSettingChangedListener listener) {
        this.mLocationMode.removeListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public long getBackgroundThrottleIntervalMs() {
        return this.mBackgroundThrottleIntervalMs.getValue(1800000L);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void addOnBackgroundThrottleIntervalChangedListener(SettingsHelper.GlobalSettingChangedListener listener) {
        this.mBackgroundThrottleIntervalMs.addListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void removeOnBackgroundThrottleIntervalChangedListener(SettingsHelper.GlobalSettingChangedListener listener) {
        this.mBackgroundThrottleIntervalMs.removeListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public boolean isLocationPackageBlacklisted(int userId, String packageName) {
        List<String> locationPackageBlacklist = this.mLocationPackageBlacklist.getValueForUser(userId);
        if (locationPackageBlacklist.isEmpty()) {
            return false;
        }
        List<String> locationPackageWhitelist = this.mLocationPackageWhitelist.getValueForUser(userId);
        for (String locationWhitelistPackage : locationPackageWhitelist) {
            if (packageName.startsWith(locationWhitelistPackage)) {
                return false;
            }
        }
        for (String locationBlacklistPackage : locationPackageBlacklist) {
            if (packageName.startsWith(locationBlacklistPackage)) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void addOnLocationPackageBlacklistChangedListener(SettingsHelper.UserSettingChangedListener listener) {
        this.mLocationPackageBlacklist.addListener(listener);
        this.mLocationPackageWhitelist.addListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void removeOnLocationPackageBlacklistChangedListener(SettingsHelper.UserSettingChangedListener listener) {
        this.mLocationPackageBlacklist.removeListener(listener);
        this.mLocationPackageWhitelist.removeListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public Set<String> getBackgroundThrottlePackageWhitelist() {
        return this.mBackgroundThrottlePackageWhitelist.getValue();
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void addOnBackgroundThrottlePackageWhitelistChangedListener(SettingsHelper.GlobalSettingChangedListener listener) {
        this.mBackgroundThrottlePackageWhitelist.addListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void removeOnBackgroundThrottlePackageWhitelistChangedListener(SettingsHelper.GlobalSettingChangedListener listener) {
        this.mBackgroundThrottlePackageWhitelist.removeListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public boolean isGnssMeasurementsFullTrackingEnabled() {
        return this.mGnssMeasurementFullTracking.getValue(false);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void addOnGnssMeasurementsFullTrackingEnabledChangedListener(SettingsHelper.GlobalSettingChangedListener listener) {
        this.mGnssMeasurementFullTracking.addListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void removeOnGnssMeasurementsFullTrackingEnabledChangedListener(SettingsHelper.GlobalSettingChangedListener listener) {
        this.mGnssMeasurementFullTracking.removeListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public PackageTagsList getAdasAllowlist() {
        return this.mAdasPackageAllowlist.getValue();
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void addAdasAllowlistChangedListener(SettingsHelper.GlobalSettingChangedListener listener) {
        this.mAdasPackageAllowlist.addListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void removeAdasAllowlistChangedListener(SettingsHelper.GlobalSettingChangedListener listener) {
        this.mAdasPackageAllowlist.removeListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public PackageTagsList getIgnoreSettingsAllowlist() {
        return this.mIgnoreSettingsPackageAllowlist.getValue();
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void addIgnoreSettingsAllowlistChangedListener(SettingsHelper.GlobalSettingChangedListener listener) {
        this.mIgnoreSettingsPackageAllowlist.addListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void removeIgnoreSettingsAllowlistChangedListener(SettingsHelper.GlobalSettingChangedListener listener) {
        this.mIgnoreSettingsPackageAllowlist.removeListener(listener);
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public long getBackgroundThrottleProximityAlertIntervalMs() {
        long identity = Binder.clearCallingIdentity();
        try {
            return Settings.Global.getLong(this.mContext.getContentResolver(), "location_background_throttle_proximity_alert_interval_ms", 1800000L);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public float getCoarseLocationAccuracyM() {
        long identity = Binder.clearCallingIdentity();
        ContentResolver cr = this.mContext.getContentResolver();
        try {
            return Settings.Secure.getFloatForUser(cr, "locationCoarseAccuracy", DEFAULT_COARSE_LOCATION_ACCURACY_M, cr.getUserId());
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.injector.SettingsHelper
    public void dump(FileDescriptor fd, IndentingPrintWriter ipw, String[] args) {
        try {
            int[] userIds = ActivityManager.getService().getRunningUserIds();
            ipw.print("Location Setting: ");
            ipw.increaseIndent();
            if (userIds.length > 1) {
                ipw.println();
                for (int userId : userIds) {
                    ipw.print("[u");
                    ipw.print(userId);
                    ipw.print("] ");
                    ipw.println(isLocationEnabled(userId));
                }
            } else {
                ipw.println(isLocationEnabled(userIds[0]));
            }
            ipw.decreaseIndent();
            ipw.println("Location Allow/Deny Packages:");
            ipw.increaseIndent();
            if (userIds.length > 1) {
                for (int userId2 : userIds) {
                    List<String> locationPackageBlacklist = this.mLocationPackageBlacklist.getValueForUser(userId2);
                    if (!locationPackageBlacklist.isEmpty()) {
                        ipw.print("user ");
                        ipw.print(userId2);
                        ipw.println(":");
                        ipw.increaseIndent();
                        for (String packageName : locationPackageBlacklist) {
                            ipw.print("[deny] ");
                            ipw.println(packageName);
                        }
                        List<String> locationPackageWhitelist = this.mLocationPackageWhitelist.getValueForUser(userId2);
                        for (String packageName2 : locationPackageWhitelist) {
                            ipw.print("[allow] ");
                            ipw.println(packageName2);
                        }
                        ipw.decreaseIndent();
                    }
                }
            } else {
                for (String packageName3 : this.mLocationPackageBlacklist.getValueForUser(userIds[0])) {
                    ipw.print("[deny] ");
                    ipw.println(packageName3);
                }
                List<String> locationPackageWhitelist2 = this.mLocationPackageWhitelist.getValueForUser(userIds[0]);
                for (String packageName4 : locationPackageWhitelist2) {
                    ipw.print("[allow] ");
                    ipw.println(packageName4);
                }
            }
            ipw.decreaseIndent();
            Set<String> backgroundThrottlePackageWhitelist = this.mBackgroundThrottlePackageWhitelist.getValue();
            if (!backgroundThrottlePackageWhitelist.isEmpty()) {
                ipw.println("Throttling Allow Packages:");
                ipw.increaseIndent();
                for (String packageName5 : backgroundThrottlePackageWhitelist) {
                    ipw.println(packageName5);
                }
                ipw.decreaseIndent();
            }
            PackageTagsList ignoreSettingsAllowlist = this.mIgnoreSettingsPackageAllowlist.getValue();
            if (!ignoreSettingsAllowlist.isEmpty()) {
                ipw.println("Emergency Bypass Allow Packages:");
                ipw.increaseIndent();
                ignoreSettingsAllowlist.dump(ipw);
                ipw.decreaseIndent();
            }
            PackageTagsList adasPackageAllowlist = this.mAdasPackageAllowlist.getValue();
            if (!adasPackageAllowlist.isEmpty()) {
                ipw.println("ADAS Bypass Allow Packages:");
                ipw.increaseIndent();
                adasPackageAllowlist.dump(ipw);
                ipw.decreaseIndent();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static abstract class ObservingSetting extends ContentObserver {
        private final CopyOnWriteArrayList<SettingsHelper.UserSettingChangedListener> mListeners;
        private boolean mRegistered;

        ObservingSetting(Handler handler) {
            super(handler);
            this.mListeners = new CopyOnWriteArrayList<>();
        }

        protected synchronized boolean isRegistered() {
            return this.mRegistered;
        }

        protected synchronized void register(Context context, Uri uri) {
            if (this.mRegistered) {
                return;
            }
            context.getContentResolver().registerContentObserver(uri, false, this, -1);
            this.mRegistered = true;
        }

        public void addListener(SettingsHelper.UserSettingChangedListener listener) {
            this.mListeners.add(listener);
        }

        public void removeListener(SettingsHelper.UserSettingChangedListener listener) {
            this.mListeners.remove(listener);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (LocationManagerService.D) {
                Log.d(LocationManagerService.TAG, "location setting changed [u" + userId + "]: " + uri);
            }
            Iterator<SettingsHelper.UserSettingChangedListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                SettingsHelper.UserSettingChangedListener listener = it.next();
                listener.onSettingChanged(userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class IntegerSecureSetting extends ObservingSetting {
        private final Context mContext;
        private final String mSettingName;

        IntegerSecureSetting(Context context, String settingName, Handler handler) {
            super(handler);
            this.mContext = context;
            this.mSettingName = settingName;
        }

        void register() {
            register(this.mContext, Settings.Secure.getUriFor(this.mSettingName));
        }

        public int getValueForUser(int defaultValue, int userId) {
            long identity = Binder.clearCallingIdentity();
            try {
                return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), this.mSettingName, defaultValue, userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class StringListCachedSecureSetting extends ObservingSetting {
        private int mCachedUserId;
        private List<String> mCachedValue;
        private final Context mContext;
        private final String mSettingName;

        StringListCachedSecureSetting(Context context, String settingName, Handler handler) {
            super(handler);
            this.mContext = context;
            this.mSettingName = settingName;
            this.mCachedUserId = -10000;
        }

        public void register() {
            register(this.mContext, Settings.Secure.getUriFor(this.mSettingName));
        }

        public synchronized List<String> getValueForUser(int userId) {
            List<String> value;
            Preconditions.checkArgument(userId != -10000);
            value = this.mCachedValue;
            if (userId != this.mCachedUserId) {
                long identity = Binder.clearCallingIdentity();
                try {
                    String setting = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), this.mSettingName, userId);
                    if (TextUtils.isEmpty(setting)) {
                        try {
                            value = Collections.emptyList();
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    } else {
                        value = Arrays.asList(setting.split(","));
                    }
                    Binder.restoreCallingIdentity(identity);
                    if (isRegistered()) {
                        this.mCachedUserId = userId;
                        this.mCachedValue = value;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            return value;
        }

        public synchronized void invalidateForUser(int userId) {
            if (this.mCachedUserId == userId) {
                this.mCachedUserId = -10000;
                this.mCachedValue = null;
            }
        }

        @Override // com.android.server.location.injector.SystemSettingsHelper.ObservingSetting, android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            invalidateForUser(userId);
            super.onChange(selfChange, uri, userId);
        }
    }

    /* loaded from: classes.dex */
    private static class BooleanGlobalSetting extends ObservingSetting {
        private final Context mContext;
        private final String mSettingName;

        BooleanGlobalSetting(Context context, String settingName, Handler handler) {
            super(handler);
            this.mContext = context;
            this.mSettingName = settingName;
        }

        public void register() {
            register(this.mContext, Settings.Global.getUriFor(this.mSettingName));
        }

        public boolean getValue(boolean defaultValue) {
            long identity = Binder.clearCallingIdentity();
            try {
                return Settings.Global.getInt(this.mContext.getContentResolver(), this.mSettingName, defaultValue ? 1 : 0) != 0;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class LongGlobalSetting extends ObservingSetting {
        private final Context mContext;
        private final String mSettingName;

        LongGlobalSetting(Context context, String settingName, Handler handler) {
            super(handler);
            this.mContext = context;
            this.mSettingName = settingName;
        }

        public void register() {
            register(this.mContext, Settings.Global.getUriFor(this.mSettingName));
        }

        public long getValue(long defaultValue) {
            long identity = Binder.clearCallingIdentity();
            try {
                return Settings.Global.getLong(this.mContext.getContentResolver(), this.mSettingName, defaultValue);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class StringSetCachedGlobalSetting extends ObservingSetting {
        private final Supplier<ArraySet<String>> mBaseValuesSupplier;
        private ArraySet<String> mCachedValue;
        private final Context mContext;
        private final String mSettingName;
        private boolean mValid;

        StringSetCachedGlobalSetting(Context context, String settingName, Supplier<ArraySet<String>> baseValuesSupplier, Handler handler) {
            super(handler);
            this.mContext = context;
            this.mSettingName = settingName;
            this.mBaseValuesSupplier = baseValuesSupplier;
            this.mValid = false;
        }

        public void register() {
            register(this.mContext, Settings.Global.getUriFor(this.mSettingName));
        }

        public synchronized Set<String> getValue() {
            ArraySet<String> value;
            value = this.mCachedValue;
            if (!this.mValid) {
                long identity = Binder.clearCallingIdentity();
                try {
                    value = new ArraySet<>(this.mBaseValuesSupplier.get());
                    String setting = Settings.Global.getString(this.mContext.getContentResolver(), this.mSettingName);
                    if (!TextUtils.isEmpty(setting)) {
                        try {
                            value.addAll(Arrays.asList(setting.split(",")));
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    }
                    Binder.restoreCallingIdentity(identity);
                    if (isRegistered()) {
                        this.mValid = true;
                        this.mCachedValue = value;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            return value;
        }

        public synchronized void invalidate() {
            this.mValid = false;
            this.mCachedValue = null;
        }

        @Override // com.android.server.location.injector.SystemSettingsHelper.ObservingSetting, android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            invalidate();
            super.onChange(selfChange, uri, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DeviceConfigSetting implements DeviceConfig.OnPropertiesChangedListener {
        private final CopyOnWriteArrayList<SettingsHelper.GlobalSettingChangedListener> mListeners = new CopyOnWriteArrayList<>();
        protected final String mName;
        private boolean mRegistered;

        DeviceConfigSetting(String name) {
            this.mName = name;
        }

        protected synchronized boolean isRegistered() {
            return this.mRegistered;
        }

        protected synchronized void register() {
            if (this.mRegistered) {
                return;
            }
            DeviceConfig.addOnPropertiesChangedListener("location", FgThread.getExecutor(), this);
            this.mRegistered = true;
        }

        public void addListener(SettingsHelper.GlobalSettingChangedListener listener) {
            this.mListeners.add(listener);
        }

        public void removeListener(SettingsHelper.GlobalSettingChangedListener listener) {
            this.mListeners.remove(listener);
        }

        public final void onPropertiesChanged(DeviceConfig.Properties properties) {
            if (!properties.getKeyset().contains(this.mName)) {
                return;
            }
            onPropertiesChanged();
        }

        public void onPropertiesChanged() {
            if (LocationManagerService.D) {
                Log.d(LocationManagerService.TAG, "location device config setting changed: " + this.mName);
            }
            Iterator<SettingsHelper.GlobalSettingChangedListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                SettingsHelper.UserSettingChangedListener listener = it.next();
                listener.onSettingChanged(-1);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PackageTagsListSetting extends DeviceConfigSetting {
        private final Supplier<ArrayMap<String, ArraySet<String>>> mBaseValuesSupplier;
        private PackageTagsList mCachedValue;
        private boolean mValid;

        PackageTagsListSetting(String name, Supplier<ArrayMap<String, ArraySet<String>>> baseValuesSupplier) {
            super(name);
            this.mBaseValuesSupplier = baseValuesSupplier;
        }

        public synchronized PackageTagsList getValue() {
            PackageTagsList value;
            value = this.mCachedValue;
            if (!this.mValid) {
                long identity = Binder.clearCallingIdentity();
                try {
                    PackageTagsList.Builder builder = new PackageTagsList.Builder().add(this.mBaseValuesSupplier.get());
                    String setting = DeviceConfig.getProperty("location", this.mName);
                    if (!TextUtils.isEmpty(setting)) {
                        try {
                            String[] split = setting.split(",");
                            int length = split.length;
                            char c = 0;
                            int i = 0;
                            while (i < length) {
                                String packageAndTags = split[i];
                                if (!TextUtils.isEmpty(packageAndTags)) {
                                    String[] packageThenTags = packageAndTags.split(";");
                                    String packageName = packageThenTags[c];
                                    if (packageThenTags.length == 1) {
                                        builder.add(packageName);
                                    } else {
                                        for (int i2 = 1; i2 < packageThenTags.length; i2++) {
                                            String attributionTag = packageThenTags[i2];
                                            if ("null".equals(attributionTag)) {
                                                attributionTag = null;
                                            }
                                            if ("*".equals(attributionTag)) {
                                                builder.add(packageName);
                                            } else {
                                                builder.add(packageName, attributionTag);
                                            }
                                        }
                                    }
                                }
                                i++;
                                c = 0;
                            }
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    }
                    value = builder.build();
                    Binder.restoreCallingIdentity(identity);
                    if (isRegistered()) {
                        this.mValid = true;
                        this.mCachedValue = value;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            return value;
        }

        public synchronized void invalidate() {
            this.mValid = false;
            this.mCachedValue = null;
        }

        @Override // com.android.server.location.injector.SystemSettingsHelper.DeviceConfigSetting
        public void onPropertiesChanged() {
            invalidate();
            super.onPropertiesChanged();
        }
    }
}
