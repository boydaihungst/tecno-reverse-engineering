package com.android.server;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.internal.modules.utils.build.UnboundedSdkLevel;
import android.media.MediaMetrics;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.CarrierAssociatedAppEntry;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Process;
import android.os.SystemProperties;
import android.os.VintfRuntimeInfo;
import android.os.incremental.IncrementalManager;
import android.os.storage.StorageManager;
import android.permission.PermissionManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.sysprop.ApexProperties;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimingsTraceLog;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import libcore.io.IoUtils;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes4.dex */
public class SystemConfig {
    private static final int ALLOW_ALL = -1;
    private static final int ALLOW_APP_CONFIGS = 8;
    private static final int ALLOW_ASSOCIATIONS = 128;
    private static final int ALLOW_FEATURES = 1;
    private static final int ALLOW_HIDDENAPI_WHITELISTING = 64;
    private static final int ALLOW_IMPLICIT_BROADCASTS = 512;
    private static final int ALLOW_LIBS = 2;
    private static final int ALLOW_OEM_PERMISSIONS = 32;
    private static final int ALLOW_OVERRIDE_APP_RESTRICTIONS = 256;
    private static final int ALLOW_PERMISSIONS = 4;
    private static final int ALLOW_PRIVAPP_PERMISSIONS = 16;
    private static final int ALLOW_VENDOR_APEX = 1024;
    private static final ArrayMap<String, ArraySet<String>> EMPTY_PERMISSIONS = new ArrayMap<>();
    private static final String SKU_PROPERTY = "ro.boot.product.hardware.sku";
    static final String TAG = "SystemConfig";
    private static final String VENDOR_SKU_PROPERTY = "ro.boot.product.vendor.sku";
    static SystemConfig sInstance;
    private String mModulesInstallerPackageName;
    private String mOverlayConfigSignaturePackage;
    int[] mGlobalGids = EmptyArray.INT;
    final SparseArray<ArraySet<String>> mSystemPermissions = new SparseArray<>();
    final ArrayList<PermissionManager.SplitPermissionInfo> mSplitPermissions = new ArrayList<>();
    final ArrayMap<String, SharedLibraryEntry> mSharedLibraries = new ArrayMap<>();
    final ArrayMap<String, FeatureInfo> mAvailableFeatures = new ArrayMap<>();
    final ArraySet<String> mUnavailableFeatures = new ArraySet<>();
    final ArrayMap<String, PermissionEntry> mPermissions = new ArrayMap<>();
    final ArraySet<String> mAllowInPowerSaveExceptIdle = new ArraySet<>();
    final ArraySet<String> mAllowInPowerSave = new ArraySet<>();
    final ArraySet<String> mAllowInDataUsageSave = new ArraySet<>();
    final ArraySet<String> mAllowUnthrottledLocation = new ArraySet<>();
    final ArrayMap<String, ArraySet<String>> mAllowAdasSettings = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mAllowIgnoreLocationSettings = new ArrayMap<>();
    final ArraySet<String> mAllowImplicitBroadcasts = new ArraySet<>();
    final ArraySet<String> mBgRestrictionExemption = new ArraySet<>();
    final ArraySet<String> mLinkedApps = new ArraySet<>();
    final ArraySet<ComponentName> mDefaultVrComponents = new ArraySet<>();
    final ArraySet<ComponentName> mBackupTransportWhitelist = new ArraySet<>();
    final ArrayMap<String, ArrayMap<String, Boolean>> mPackageComponentEnabledState = new ArrayMap<>();
    final ArraySet<String> mHiddenApiPackageWhitelist = new ArraySet<>();
    final ArraySet<String> mDisabledUntilUsedPreinstalledCarrierApps = new ArraySet<>();
    final ArrayMap<String, List<CarrierAssociatedAppEntry>> mDisabledUntilUsedPreinstalledCarrierAssociatedApps = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mVendorPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mVendorPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mProductPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mProductPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mSystemExtPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mSystemExtPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArrayMap<String, ArraySet<String>>> mApexPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArrayMap<String, ArraySet<String>>> mApexPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArrayMap<String, Boolean>> mOemPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrProductPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrProductPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrMiPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrMiPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrPreloadPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrPreloadPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrRegionPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrRegionPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrCarrierPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrCarrierPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrCompanyPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrCompanyPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrThemePrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mTrThemePrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mAllowedAssociations = new ArrayMap<>();
    private final ArraySet<String> mBugreportWhitelistedPackages = new ArraySet<>();
    private final ArraySet<String> mAppDataIsolationWhitelistedApps = new ArraySet<>();
    private ArrayMap<String, Set<String>> mPackageToUserTypeWhitelist = new ArrayMap<>();
    private ArrayMap<String, Set<String>> mPackageToUserTypeBlacklist = new ArrayMap<>();
    private final ArraySet<String> mRollbackWhitelistedPackages = new ArraySet<>();
    private final ArraySet<String> mWhitelistedStagedInstallers = new ArraySet<>();
    private final ArrayMap<String, String> mAllowedVendorApexes = new ArrayMap<>();
    private final ArraySet<String> mWhiteListedAllowInIdleUserApp = new ArraySet<>();
    private Map<String, Map<String, String>> mNamedActors = null;

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isAtLeastSdkLevel(String version) {
        try {
            return UnboundedSdkLevel.isAtLeast(version);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isAtMostSdkLevel(String version) {
        try {
            return UnboundedSdkLevel.isAtMost(version);
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /* loaded from: classes4.dex */
    public static final class SharedLibraryEntry {
        public final boolean canBeSafelyIgnored;
        public final String[] dependencies;
        public final String filename;
        public final boolean isNative;
        public final String name;
        public final String onBootclasspathBefore;
        public final String onBootclasspathSince;

        public SharedLibraryEntry(String name, String filename, String[] dependencies, boolean isNative) {
            this(name, filename, dependencies, null, null, isNative);
        }

        public SharedLibraryEntry(String name, String filename, String[] dependencies, String onBootclasspathSince, String onBootclasspathBefore) {
            this(name, filename, dependencies, onBootclasspathSince, onBootclasspathBefore, false);
        }

        SharedLibraryEntry(String name, String filename, String[] dependencies, String onBootclasspathSince, String onBootclasspathBefore, boolean isNative) {
            this.name = name;
            this.filename = filename;
            this.dependencies = dependencies;
            this.onBootclasspathSince = onBootclasspathSince;
            this.onBootclasspathBefore = onBootclasspathBefore;
            this.isNative = isNative;
            this.canBeSafelyIgnored = (onBootclasspathSince != null && SystemConfig.isAtLeastSdkLevel(onBootclasspathSince)) || !(onBootclasspathBefore == null || SystemConfig.isAtLeastSdkLevel(onBootclasspathBefore));
        }
    }

    /* loaded from: classes4.dex */
    public static final class PermissionEntry {
        public int[] gids;
        public final String name;
        public boolean perUser;

        PermissionEntry(String name, boolean perUser) {
            this.name = name;
            this.perUser = perUser;
        }
    }

    public static SystemConfig getInstance() {
        SystemConfig systemConfig;
        if (!isSystemProcess()) {
            Slog.wtf(TAG, "SystemConfig is being accessed by a process other than system_server.");
        }
        synchronized (SystemConfig.class) {
            if (sInstance == null) {
                sInstance = new SystemConfig();
            }
            systemConfig = sInstance;
        }
        return systemConfig;
    }

    public int[] getGlobalGids() {
        return this.mGlobalGids;
    }

    public SparseArray<ArraySet<String>> getSystemPermissions() {
        return this.mSystemPermissions;
    }

    public ArrayList<PermissionManager.SplitPermissionInfo> getSplitPermissions() {
        return this.mSplitPermissions;
    }

    public ArrayMap<String, SharedLibraryEntry> getSharedLibraries() {
        return this.mSharedLibraries;
    }

    public ArrayMap<String, FeatureInfo> getAvailableFeatures() {
        return this.mAvailableFeatures;
    }

    public ArrayMap<String, PermissionEntry> getPermissions() {
        return this.mPermissions;
    }

    public ArraySet<String> getAllowImplicitBroadcasts() {
        return this.mAllowImplicitBroadcasts;
    }

    public ArraySet<String> getAllowInPowerSaveExceptIdle() {
        return this.mAllowInPowerSaveExceptIdle;
    }

    public ArraySet<String> getAllowInPowerSave() {
        return this.mAllowInPowerSave;
    }

    public ArraySet<String> getAllowInDataUsageSave() {
        return this.mAllowInDataUsageSave;
    }

    public ArraySet<String> getAllowUnthrottledLocation() {
        return this.mAllowUnthrottledLocation;
    }

    public ArrayMap<String, ArraySet<String>> getAllowAdasLocationSettings() {
        return this.mAllowAdasSettings;
    }

    public ArrayMap<String, ArraySet<String>> getAllowIgnoreLocationSettings() {
        return this.mAllowIgnoreLocationSettings;
    }

    public ArraySet<String> getBgRestrictionExemption() {
        return this.mBgRestrictionExemption;
    }

    public ArraySet<String> getLinkedApps() {
        return this.mLinkedApps;
    }

    public ArraySet<String> getHiddenApiWhitelistedApps() {
        return this.mHiddenApiPackageWhitelist;
    }

    public ArraySet<ComponentName> getDefaultVrComponents() {
        return this.mDefaultVrComponents;
    }

    public ArraySet<ComponentName> getBackupTransportWhitelist() {
        return this.mBackupTransportWhitelist;
    }

    public ArrayMap<String, Boolean> getComponentsEnabledStates(String packageName) {
        return this.mPackageComponentEnabledState.get(packageName);
    }

    public ArraySet<String> getDisabledUntilUsedPreinstalledCarrierApps() {
        return this.mDisabledUntilUsedPreinstalledCarrierApps;
    }

    public ArrayMap<String, List<CarrierAssociatedAppEntry>> getDisabledUntilUsedPreinstalledCarrierAssociatedApps() {
        return this.mDisabledUntilUsedPreinstalledCarrierAssociatedApps;
    }

    public ArraySet<String> getPrivAppPermissions(String packageName) {
        return this.mPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getPrivAppDenyPermissions(String packageName) {
        return this.mPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getApexPrivAppPermissions(String apexName, String apkPackageName) {
        return this.mApexPrivAppPermissions.getOrDefault(apexName, EMPTY_PERMISSIONS).get(apkPackageName);
    }

    public ArraySet<String> getApexPrivAppDenyPermissions(String apexName, String apkPackageName) {
        return this.mApexPrivAppDenyPermissions.getOrDefault(apexName, EMPTY_PERMISSIONS).get(apkPackageName);
    }

    public ArraySet<String> getVendorPrivAppPermissions(String packageName) {
        return this.mVendorPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getVendorPrivAppDenyPermissions(String packageName) {
        return this.mVendorPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getProductPrivAppPermissions(String packageName) {
        return this.mProductPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getProductPrivAppDenyPermissions(String packageName) {
        return this.mProductPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getAllowInIdleUserApp() {
        return this.mWhiteListedAllowInIdleUserApp;
    }

    public ArraySet<String> getTrProductPrivAppPermissions(String packageName) {
        return this.mTrProductPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getTrProductPrivAppDenyPermissions(String packageName) {
        return this.mTrProductPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getTrMiPrivAppPermissions(String packageName) {
        return this.mTrMiPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getTrMiPrivAppDenyPermissions(String packageName) {
        return this.mTrMiPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getTrPreloadPrivAppPermissions(String packageName) {
        return this.mTrPreloadPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getTrPreloadPrivAppDenyPermissions(String packageName) {
        return this.mTrPreloadPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getTrCompanyPrivAppPermissions(String packageName) {
        return this.mTrCompanyPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getTrCompanyPrivAppDenyPermissions(String packageName) {
        return this.mTrCompanyPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getTrRegionPrivAppPermissions(String packageName) {
        return this.mTrRegionPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getTrRegionPrivAppDenyPermissions(String packageName) {
        return this.mTrRegionPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getTrCarrierPrivAppPermissions(String packageName) {
        return this.mTrCarrierPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getTrCarrierPrivAppDenyPermissions(String packageName) {
        return this.mTrCarrierPrivAppDenyPermissions.get(packageName);
    }

    public ArraySet<String> getTrThemePrivAppPermissions(String packageName) {
        return this.mTrThemePrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getTrThemePrivAppDenyPermissions(String packageName) {
        return this.mTrThemePrivAppDenyPermissions.get(packageName);
    }

    private void readTrPrivAppPermissions(XmlPullParser parser, File permFile) throws IOException, XmlPullParserException {
        boolean trProduct = permFile.toPath().startsWith(Environment.getTrProductDirectory().toPath() + "/");
        boolean trMi = permFile.toPath().startsWith(Environment.getTrMiDirectory().toPath() + "/");
        boolean trPreload = permFile.toPath().startsWith(Environment.getTrPreloadDirectory().toPath() + "/");
        boolean trCompany = permFile.toPath().startsWith(Environment.getTrCompanyDirectory().toPath() + "/");
        boolean trRegion = permFile.toPath().startsWith(Environment.getTrRegionDirectory().toPath() + "/");
        boolean trCarrier = permFile.toPath().startsWith(Environment.getTrCarrierDirectory().toPath() + "/");
        boolean trTheme = permFile.toPath().startsWith(Environment.getTrThemeDirectory().toPath() + "/");
        if (trProduct) {
            readPrivAppPermissions(parser, this.mTrProductPrivAppPermissions, this.mTrProductPrivAppDenyPermissions);
        } else if (trMi) {
            readPrivAppPermissions(parser, this.mTrMiPrivAppPermissions, this.mTrMiPrivAppDenyPermissions);
        } else if (trPreload) {
            readPrivAppPermissions(parser, this.mTrPreloadPrivAppPermissions, this.mTrPreloadPrivAppDenyPermissions);
        } else if (trCompany) {
            readPrivAppPermissions(parser, this.mTrCompanyPrivAppPermissions, this.mTrCompanyPrivAppDenyPermissions);
        } else if (trRegion) {
            readPrivAppPermissions(parser, this.mTrRegionPrivAppPermissions, this.mTrRegionPrivAppDenyPermissions);
        } else if (trCarrier) {
            readPrivAppPermissions(parser, this.mTrCarrierPrivAppPermissions, this.mTrCarrierPrivAppDenyPermissions);
        } else if (trTheme) {
            readPrivAppPermissions(parser, this.mTrThemePrivAppPermissions, this.mTrThemePrivAppDenyPermissions);
        } else {
            readPrivAppPermissions(parser, this.mPrivAppPermissions, this.mPrivAppDenyPermissions);
        }
    }

    public ArraySet<String> getSystemExtPrivAppPermissions(String packageName) {
        return this.mSystemExtPrivAppPermissions.get(packageName);
    }

    public ArraySet<String> getSystemExtPrivAppDenyPermissions(String packageName) {
        return this.mSystemExtPrivAppDenyPermissions.get(packageName);
    }

    public Map<String, Boolean> getOemPermissions(String packageName) {
        Map<String, Boolean> oemPermissions = this.mOemPermissions.get(packageName);
        if (oemPermissions != null) {
            return oemPermissions;
        }
        return Collections.emptyMap();
    }

    public ArrayMap<String, ArraySet<String>> getAllowedAssociations() {
        return this.mAllowedAssociations;
    }

    public ArraySet<String> getBugreportWhitelistedPackages() {
        return this.mBugreportWhitelistedPackages;
    }

    public Set<String> getRollbackWhitelistedPackages() {
        return this.mRollbackWhitelistedPackages;
    }

    public Set<String> getWhitelistedStagedInstallers() {
        return this.mWhitelistedStagedInstallers;
    }

    public Map<String, String> getAllowedVendorApexes() {
        return this.mAllowedVendorApexes;
    }

    public String getModulesInstallerPackageName() {
        return this.mModulesInstallerPackageName;
    }

    public ArraySet<String> getAppDataIsolationWhitelistedApps() {
        return this.mAppDataIsolationWhitelistedApps;
    }

    public ArrayMap<String, Set<String>> getAndClearPackageToUserTypeWhitelist() {
        ArrayMap<String, Set<String>> r = this.mPackageToUserTypeWhitelist;
        this.mPackageToUserTypeWhitelist = new ArrayMap<>(0);
        return r;
    }

    public ArrayMap<String, Set<String>> getAndClearPackageToUserTypeBlacklist() {
        ArrayMap<String, Set<String>> r = this.mPackageToUserTypeBlacklist;
        this.mPackageToUserTypeBlacklist = new ArrayMap<>(0);
        return r;
    }

    public Map<String, Map<String, String>> getNamedActors() {
        Map<String, Map<String, String>> map = this.mNamedActors;
        return map != null ? map : Collections.emptyMap();
    }

    public String getOverlayConfigSignaturePackage() {
        if (TextUtils.isEmpty(this.mOverlayConfigSignaturePackage)) {
            return null;
        }
        return this.mOverlayConfigSignaturePackage;
    }

    public SystemConfig(boolean readPermissions) {
        if (readPermissions) {
            Slog.w(TAG, "Constructing a test SystemConfig");
            readAllPermissions();
            return;
        }
        Slog.w(TAG, "Constructing an empty test SystemConfig");
    }

    SystemConfig() {
        TimingsTraceLog log = new TimingsTraceLog(TAG, 524288L);
        log.traceBegin("readAllPermissions");
        try {
            readAllPermissions();
            readPublicNativeLibrariesList();
        } finally {
            log.traceEnd();
        }
    }

    private void readAllPermissions() {
        int vendorPermissionFlag;
        XmlPullParser parser = Xml.newPullParser();
        readPermissions(parser, Environment.buildPath(Environment.getRootDirectory(), "etc", "sysconfig"), -1);
        readPermissions(parser, Environment.buildPath(Environment.getRootDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), -1);
        int vendorPermissionFlag2 = 1171;
        if (Build.VERSION.DEVICE_INITIAL_SDK_INT <= 27) {
            vendorPermissionFlag2 = 1171 | 12;
        }
        readPermissions(parser, Environment.buildPath(Environment.getVendorDirectory(), "etc", "sysconfig"), vendorPermissionFlag2);
        readPermissions(parser, Environment.buildPath(Environment.getVendorDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), vendorPermissionFlag2);
        String vendorSkuProperty = SystemProperties.get(VENDOR_SKU_PROPERTY, "");
        int i = 0;
        if (!vendorSkuProperty.isEmpty()) {
            String vendorSkuDir = "sku_" + vendorSkuProperty;
            readPermissions(parser, Environment.buildPath(Environment.getVendorDirectory(), "etc", "sysconfig", vendorSkuDir), vendorPermissionFlag2);
            readPermissions(parser, Environment.buildPath(Environment.getVendorDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS, vendorSkuDir), vendorPermissionFlag2);
        }
        int odmPermissionFlag = vendorPermissionFlag2;
        readPermissions(parser, Environment.buildPath(Environment.getOdmDirectory(), "etc", "sysconfig"), odmPermissionFlag);
        readPermissions(parser, Environment.buildPath(Environment.getOdmDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), odmPermissionFlag);
        String skuProperty = SystemProperties.get(SKU_PROPERTY, "");
        if (!skuProperty.isEmpty()) {
            String skuDir = "sku_" + skuProperty;
            readPermissions(parser, Environment.buildPath(Environment.getOdmDirectory(), "etc", "sysconfig", skuDir), odmPermissionFlag);
            readPermissions(parser, Environment.buildPath(Environment.getOdmDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS, skuDir), odmPermissionFlag);
        }
        readPermissions(parser, Environment.buildPath(Environment.getOemDirectory(), "etc", "sysconfig"), 1185);
        readPermissions(parser, Environment.buildPath(Environment.getOemDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), 1185);
        int productPermissionFlag = 2015;
        if (Build.VERSION.DEVICE_INITIAL_SDK_INT <= 30) {
            productPermissionFlag = -1;
        }
        readPermissions(parser, Environment.buildPath(Environment.getProductDirectory(), "etc", "sysconfig"), productPermissionFlag);
        readPermissions(parser, Environment.buildPath(Environment.getProductDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), productPermissionFlag);
        readPermissions(parser, Environment.buildPath(Environment.getSystemExtDirectory(), "etc", "sysconfig"), -1);
        readPermissions(parser, Environment.buildPath(Environment.getSystemExtDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), -1);
        if (true == SystemProperties.getBoolean("ro.tran.partition.extend", false)) {
            readPermissions(parser, Environment.buildPath(Environment.getTrProductDirectory(), "etc", "sysconfig"), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrProductDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrMiDirectory(), "etc", "sysconfig"), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrMiDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrPreloadDirectory(), "etc", "sysconfig"), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrPreloadDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrCompanyDirectory(), "etc", "sysconfig"), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrCompanyDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrRegionDirectory(), "etc", "sysconfig"), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrRegionDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrCarrierDirectory(), "etc", "sysconfig"), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrCarrierDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrThemeDirectory(), "etc", "sysconfig"), 2015);
            readPermissions(parser, Environment.buildPath(Environment.getTrThemeDirectory(), "etc", DeviceConfig.NAMESPACE_PERMISSIONS), 2015);
        }
        if (!isSystemProcess()) {
            return;
        }
        File[] listFilesOrEmpty = FileUtils.listFilesOrEmpty(Environment.getApexDirectory());
        int length = listFilesOrEmpty.length;
        while (i < length) {
            File f = listFilesOrEmpty[i];
            if (f.isFile()) {
                vendorPermissionFlag = vendorPermissionFlag2;
            } else {
                vendorPermissionFlag = vendorPermissionFlag2;
                if (!f.getPath().contains("@")) {
                    readPermissions(parser, Environment.buildPath(f, "etc", DeviceConfig.NAMESPACE_PERMISSIONS), 19);
                }
            }
            i++;
            vendorPermissionFlag2 = vendorPermissionFlag;
        }
    }

    public void readPermissions(XmlPullParser parser, File libraryDir, int permissionFlag) {
        File[] listFiles;
        if (!libraryDir.exists() || !libraryDir.isDirectory()) {
            if (permissionFlag == -1) {
                Slog.w(TAG, "No directory " + libraryDir + ", skipping");
            }
        } else if (!libraryDir.canRead()) {
            Slog.w(TAG, "Directory " + libraryDir + " cannot be read");
        } else {
            File platformFile = null;
            for (File f : libraryDir.listFiles()) {
                if (f.isFile()) {
                    if (f.getPath().endsWith("etc/permissions/platform.xml")) {
                        platformFile = f;
                    } else if (!f.getPath().endsWith(".xml")) {
                        Slog.i(TAG, "Non-xml file " + f + " in " + libraryDir + " directory, ignoring");
                    } else if (!f.canRead()) {
                        Slog.w(TAG, "Permissions library file " + f + " cannot be read");
                    } else {
                        readPermissionsFromXml(parser, f, permissionFlag);
                    }
                }
            }
            if (platformFile != null) {
                readPermissionsFromXml(parser, platformFile, permissionFlag);
            }
        }
    }

    private void logNotAllowedInPartition(String name, File permFile, XmlPullParser parser) {
        Slog.w(TAG, "<" + name + "> not allowed in partition of " + permFile + " at " + parser.getPositionDescription());
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1640=4, 1642=4, 1645=5] */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0040, code lost:
        if (r15 != 2) goto L641;
     */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x004d, code lost:
        if (r41.getName().equals(android.provider.DeviceConfig.NAMESPACE_PERMISSIONS) != false) goto L16;
     */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x0059, code lost:
        if (r41.getName().equals("config") == false) goto L633;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0088, code lost:
        throw new org.xmlpull.v1.XmlPullParserException("Unexpected start tag in " + r42 + ": found " + r41.getName() + ", expected 'permissions' or 'config'");
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0089, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x008a, code lost:
        r4 = r0;
        r25 = r7;
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0091, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0092, code lost:
        r4 = r0;
        r26 = "Got exception parsing permissions.";
        r25 = r7;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x009b, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x009c, code lost:
        r4 = r0;
        r25 = r7;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x00a4, code lost:
        if (r43 != (-1)) goto L628;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x00a6, code lost:
        r10 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x00a8, code lost:
        r10 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x00ab, code lost:
        if ((r43 & 2) == 0) goto L627;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x00ad, code lost:
        r17 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x00b0, code lost:
        r17 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00b4, code lost:
        if ((r43 & 1) == 0) goto L626;
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00b6, code lost:
        r18 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00b9, code lost:
        r18 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x00bd, code lost:
        if ((r43 & 4) == 0) goto L625;
     */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x00bf, code lost:
        r19 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x00c2, code lost:
        r19 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00c6, code lost:
        if ((r43 & 8) == 0) goto L624;
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x00c8, code lost:
        r20 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x00cb, code lost:
        r20 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x00cf, code lost:
        if ((r43 & 16) == 0) goto L623;
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x00d1, code lost:
        r21 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00d4, code lost:
        r21 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x00d8, code lost:
        if ((r43 & 32) == 0) goto L622;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x00da, code lost:
        r22 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x00dd, code lost:
        r22 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x00e1, code lost:
        if ((r43 & 64) == 0) goto L621;
     */
    /* JADX WARN: Code restructure failed: missing block: B:56:0x00e3, code lost:
        r23 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x00e6, code lost:
        r23 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:585:0x114e, code lost:
        throw new org.xmlpull.v1.XmlPullParserException("No start tag found");
     */
    /* JADX WARN: Code restructure failed: missing block: B:586:0x114f, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:588:0x1152, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:59:0x00ea, code lost:
        if ((r43 & 128) == 0) goto L620;
     */
    /* JADX WARN: Code restructure failed: missing block: B:60:0x00ec, code lost:
        r9 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x00ee, code lost:
        r9 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x00f1, code lost:
        if ((r43 & 256) == 0) goto L619;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x00f3, code lost:
        r11 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:0x00f5, code lost:
        r11 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x00f8, code lost:
        if ((r43 & 512) == 0) goto L618;
     */
    /* JADX WARN: Code restructure failed: missing block: B:68:0x00fa, code lost:
        r12 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:69:0x00fc, code lost:
        r12 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:71:0x00ff, code lost:
        if ((r43 & 1024) == 0) goto L617;
     */
    /* JADX WARN: Code restructure failed: missing block: B:72:0x0101, code lost:
        r14 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:73:0x0103, code lost:
        r14 = false;
     */
    /* JADX WARN: Removed duplicated region for block: B:312:0x0839 A[Catch: IOException -> 0x114f, XmlPullParserException -> 0x1152, all -> 0x1203, TryCatch #1 {all -> 0x1203, blocks: (B:582:0x10f9, B:195:0x02f8, B:197:0x0304, B:199:0x032e, B:202:0x035c, B:205:0x0365, B:204:0x0362, B:595:0x1169, B:598:0x1174, B:207:0x0370, B:209:0x037d, B:212:0x03ad, B:214:0x03b1, B:215:0x03b4, B:216:0x03bb, B:219:0x03c0, B:210:0x03a6, B:218:0x03bd, B:220:0x03c9, B:222:0x03cf, B:224:0x03fd, B:223:0x03f8, B:226:0x0408, B:228:0x040e, B:236:0x046e, B:229:0x0437, B:231:0x043f, B:233:0x0446, B:234:0x046a, B:235:0x046b, B:237:0x0477, B:239:0x0495, B:257:0x0555, B:240:0x04c1, B:242:0x04c7, B:243:0x04f2, B:245:0x04f8, B:246:0x0523, B:248:0x052b, B:250:0x052f, B:251:0x0536, B:253:0x0540, B:256:0x0552, B:254:0x054c, B:258:0x055e, B:259:0x059a, B:260:0x059b, B:261:0x05c3, B:262:0x05c4, B:263:0x05d1, B:265:0x05d7, B:267:0x0605, B:266:0x0600, B:268:0x060e, B:270:0x0614, B:272:0x0642, B:271:0x063d, B:274:0x064d, B:276:0x0656, B:277:0x0689, B:279:0x0691, B:280:0x06c4, B:282:0x06d8, B:283:0x06e3, B:285:0x070b, B:284:0x0708, B:287:0x0716, B:289:0x071c, B:293:0x074e, B:290:0x0745, B:292:0x074b, B:295:0x0759, B:296:0x0762, B:298:0x0770, B:300:0x0793, B:305:0x07ba, B:307:0x081f, B:312:0x0839, B:314:0x0843, B:316:0x084d, B:318:0x0858, B:319:0x0864, B:321:0x086f, B:322:0x0873, B:324:0x0880, B:326:0x088e, B:328:0x0894, B:332:0x08c6, B:329:0x08bd, B:331:0x08c3, B:334:0x08d1, B:338:0x08e0, B:340:0x08ed, B:345:0x0928, B:347:0x0932, B:348:0x093d, B:352:0x0974, B:344:0x08f5, B:349:0x0946, B:351:0x0971, B:354:0x097f, B:356:0x0988, B:363:0x09f7, B:357:0x09b3, B:359:0x09b9, B:360:0x09ee, B:362:0x09f4, B:364:0x0a00, B:366:0x0a0b, B:368:0x0a17, B:374:0x0a7b, B:370:0x0a42, B:371:0x0a6d, B:373:0x0a78, B:376:0x0a86, B:378:0x0a8c, B:382:0x0abe, B:379:0x0ab5, B:381:0x0abb, B:384:0x0ac9, B:386:0x0acf, B:390:0x0b01, B:387:0x0af8, B:389:0x0afe, B:392:0x0b0c, B:394:0x0b14, B:398:0x0b48, B:395:0x0b3f, B:397:0x0b45, B:400:0x0b53, B:402:0x0b5f, B:417:0x0bbf, B:403:0x0b88, B:405:0x0b92, B:408:0x0b9a, B:409:0x0ba6, B:411:0x0bae, B:414:0x0bb8, B:416:0x0bbc, B:419:0x0bca, B:421:0x0bd6, B:436:0x0c36, B:422:0x0bff, B:424:0x0c09, B:427:0x0c11, B:428:0x0c1d, B:430:0x0c25, B:433:0x0c2f, B:435:0x0c33, B:438:0x0c41, B:440:0x0c47, B:444:0x0c79, B:441:0x0c70, B:443:0x0c76, B:446:0x0c84, B:448:0x0c8a, B:452:0x0cb6, B:449:0x0cad, B:451:0x0cb3, B:454:0x0cc1, B:456:0x0cc7, B:460:0x0cf9, B:457:0x0cf0, B:459:0x0cf6, B:462:0x0d04, B:464:0x0d0a, B:468:0x0d3c, B:465:0x0d33, B:467:0x0d39, B:470:0x0d47, B:472:0x0d4d, B:476:0x0d7f, B:473:0x0d76, B:475:0x0d7c, B:478:0x0d8a, B:480:0x0d90, B:484:0x0dc4, B:481:0x0dbb, B:483:0x0dc1, B:487:0x0dd1, B:492:0x0df5, B:497:0x0e28, B:494:0x0e20, B:490:0x0de2, B:496:0x0e24, B:503:0x0e59, B:544:0x0f6a, B:505:0x0e87, B:507:0x0eb7, B:513:0x0ec3, B:543:0x0f67, B:547:0x0f75, B:548:0x0f7a, B:551:0x0f8a, B:553:0x0f91, B:554:0x0fbe, B:556:0x0fc8, B:557:0x0ff7, B:559:0x0ffd, B:560:0x1036, B:562:0x1045, B:563:0x1050, B:565:0x1058, B:564:0x1055, B:568:0x1065, B:570:0x106c, B:571:0x1099, B:572:0x10a3, B:575:0x10b1, B:577:0x10ba, B:581:0x10f5, B:578:0x10c7, B:580:0x10f2, B:584:0x113f, B:585:0x114e), top: B:637:0x002c }] */
    /* JADX WARN: Removed duplicated region for block: B:313:0x0841  */
    /* JADX WARN: Removed duplicated region for block: B:529:0x0f23 A[Catch: all -> 0x0f52, IOException -> 0x0f57, XmlPullParserException -> 0x0f5c, TryCatch #10 {IOException -> 0x0f57, XmlPullParserException -> 0x0f5c, all -> 0x0f52, blocks: (B:501:0x0e36, B:518:0x0ecd, B:522:0x0edc, B:524:0x0ef1, B:526:0x0efb, B:525:0x0ef5, B:527:0x0f0e, B:529:0x0f23, B:531:0x0f2e, B:533:0x0f39, B:534:0x0f48), top: B:643:0x0e36 }] */
    /* JADX WARN: Removed duplicated region for block: B:531:0x0f2e A[Catch: all -> 0x0f52, IOException -> 0x0f57, XmlPullParserException -> 0x0f5c, TryCatch #10 {IOException -> 0x0f57, XmlPullParserException -> 0x0f5c, all -> 0x0f52, blocks: (B:501:0x0e36, B:518:0x0ecd, B:522:0x0edc, B:524:0x0ef1, B:526:0x0efb, B:525:0x0ef5, B:527:0x0f0e, B:529:0x0f23, B:531:0x0f2e, B:533:0x0f39, B:534:0x0f48), top: B:643:0x0e36 }] */
    /* JADX WARN: Removed duplicated region for block: B:533:0x0f39 A[Catch: all -> 0x0f52, IOException -> 0x0f57, XmlPullParserException -> 0x0f5c, TryCatch #10 {IOException -> 0x0f57, XmlPullParserException -> 0x0f5c, all -> 0x0f52, blocks: (B:501:0x0e36, B:518:0x0ecd, B:522:0x0edc, B:524:0x0ef1, B:526:0x0efb, B:525:0x0ef5, B:527:0x0f0e, B:529:0x0f23, B:531:0x0f2e, B:533:0x0f39, B:534:0x0f48), top: B:643:0x0e36 }] */
    /* JADX WARN: Removed duplicated region for block: B:602:0x1182  */
    /* JADX WARN: Removed duplicated region for block: B:603:0x118e  */
    /* JADX WARN: Removed duplicated region for block: B:606:0x1195  */
    /* JADX WARN: Removed duplicated region for block: B:609:0x11a0  */
    /* JADX WARN: Removed duplicated region for block: B:610:0x11a6  */
    /* JADX WARN: Removed duplicated region for block: B:613:0x11b1  */
    /* JADX WARN: Removed duplicated region for block: B:616:0x11c2  */
    /* JADX WARN: Removed duplicated region for block: B:619:0x11cd  */
    /* JADX WARN: Removed duplicated region for block: B:628:0x11f8 A[LOOP:2: B:626:0x11f2->B:628:0x11f8, LOOP_END] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void readPermissionsFromXml(XmlPullParser parser, File permFile, int permissionFlag) {
        FileReader permReader;
        Throwable th;
        XmlPullParserException e;
        String str;
        IOException e2;
        int i;
        int type;
        boolean allowAll;
        boolean allowLibs;
        boolean allowFeatures;
        boolean allowPermissions;
        boolean allowAppConfigs;
        boolean allowPrivappPermissions;
        boolean allowOemPermissions;
        boolean allowApiWhitelisting;
        boolean allowAssociations;
        boolean allowOverrideAppRestrictions;
        boolean allowImplicitBroadcasts;
        boolean allowVendorApex;
        char c;
        boolean allowAll2;
        boolean allowVendorApex2;
        boolean allowedMinSdk;
        boolean allowedMaxSdk;
        boolean exists;
        boolean allowed;
        boolean vendor2;
        XmlPullParser xmlPullParser = parser;
        String str2 = "Got exception parsing permissions.";
        try {
            FileReader permReader2 = new FileReader(permFile);
            Slog.i(TAG, "Reading permissions from " + permFile);
            boolean lowRam = ActivityManager.isLowRamDeviceStatic();
            try {
                try {
                    xmlPullParser.setInput(permReader2);
                    while (true) {
                        int type2 = parser.next();
                        if (type2 == 2) {
                            type = type2;
                            break;
                        } else {
                            type = type2;
                            if (type != 1) {
                            }
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e3) {
                str = str2;
                permReader = permReader2;
                e2 = e3;
            } catch (XmlPullParserException e4) {
                permReader = permReader2;
                e = e4;
            } catch (Throwable th3) {
                permReader = permReader2;
                th = th3;
            }
            if (StorageManager.isFileEncryptedNativeOnly()) {
                i = 0;
                addFeature(PackageManager.FEATURE_FILE_BASED_ENCRYPTION, 0);
                addFeature(PackageManager.FEATURE_SECURELY_REMOVES_USERS, 0);
            } else {
                i = 0;
            }
            if (StorageManager.hasAdoptable()) {
                addFeature(PackageManager.FEATURE_ADOPTABLE_STORAGE, i);
            }
            if (ActivityManager.isLowRamDeviceStatic()) {
                addFeature(PackageManager.FEATURE_RAM_LOW, i);
            } else {
                addFeature(PackageManager.FEATURE_RAM_NORMAL, i);
            }
            int incrementalVersion = IncrementalManager.getVersion();
            if (incrementalVersion > 0) {
                addFeature(PackageManager.FEATURE_INCREMENTAL_DELIVERY, incrementalVersion);
            }
            addFeature(PackageManager.FEATURE_APP_ENUMERATION, 0);
            if (Build.VERSION.DEVICE_INITIAL_SDK_INT >= 29) {
                addFeature(PackageManager.FEATURE_IPSEC_TUNNELS, 0);
            }
            if (isErofsSupported()) {
                if (isKernelVersionAtLeast(5, 10)) {
                    addFeature(PackageManager.FEATURE_EROFS, 0);
                } else if (isKernelVersionAtLeast(4, 19)) {
                    addFeature(PackageManager.FEATURE_EROFS_LEGACY, 0);
                }
            }
            Iterator<String> it = this.mUnavailableFeatures.iterator();
            while (it.hasNext()) {
                String featureName = it.next();
                removeFeature(featureName);
            }
            return;
            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getEventType() == 1) {
                    IoUtils.closeQuietly(permReader2);
                } else {
                    String name = parser.getName();
                    if (name == null) {
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        switch (name.hashCode()) {
                            case -2040330235:
                                if (name.equals("allow-unthrottled-location")) {
                                    c = '\f';
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1882490007:
                                if (name.equals("allow-in-power-save")) {
                                    c = '\t';
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1748425132:
                                if (name.equals("allow-in-idle-user-app")) {
                                    c = 11;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1582324217:
                                if (name.equals("allow-adas-location-settings")) {
                                    c = '\r';
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1554938271:
                                if (name.equals("named-actor")) {
                                    c = 30;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1461465444:
                                if (name.equals("component-override")) {
                                    c = 19;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -1005864890:
                                if (name.equals("disabled-until-used-preinstalled-carrier-app")) {
                                    c = 22;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -980620291:
                                if (name.equals("allow-association")) {
                                    c = 26;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -979207434:
                                if (name.equals("feature")) {
                                    c = 6;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -828905863:
                                if (name.equals("unavailable-feature")) {
                                    c = 7;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -642819164:
                                if (name.equals("allow-in-power-save-except-idle")) {
                                    c = '\b';
                                    break;
                                }
                                c = 65535;
                                break;
                            case -634266752:
                                if (name.equals("bg-restriction-exemption")) {
                                    c = 17;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -560717308:
                                if (name.equals("allow-ignore-location-settings")) {
                                    c = 14;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -517618225:
                                if (name.equals("permission")) {
                                    c = 1;
                                    break;
                                }
                                c = 65535;
                                break;
                            case -150068154:
                                if (name.equals("install-in-user-type")) {
                                    c = 29;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 98629247:
                                if (name.equals("group")) {
                                    c = 0;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 166208699:
                                if (name.equals("library")) {
                                    c = 5;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 180165796:
                                if (name.equals("hidden-api-whitelisted-app")) {
                                    c = 25;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 347247519:
                                if (name.equals("backup-transport-whitelisted-service")) {
                                    c = 20;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 414198242:
                                if (name.equals("allowed-vendor-apex")) {
                                    c = '\"';
                                    break;
                                }
                                c = 65535;
                                break;
                            case 802332808:
                                if (name.equals("allow-in-data-usage-save")) {
                                    c = '\n';
                                    break;
                                }
                                c = 65535;
                                break;
                            case 953292141:
                                if (name.equals("assign-permission")) {
                                    c = 2;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 968751633:
                                if (name.equals("rollback-whitelisted-app")) {
                                    c = ' ';
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1005096720:
                                if (name.equals("apex-library")) {
                                    c = 4;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1044015374:
                                if (name.equals("oem-permissions")) {
                                    c = 24;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1046683496:
                                if (name.equals("whitelisted-staged-installer")) {
                                    c = '!';
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1121420326:
                                if (name.equals("app-link")) {
                                    c = 16;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1269564002:
                                if (name.equals("split-permission")) {
                                    c = 3;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1347585732:
                                if (name.equals("app-data-isolation-whitelisted-app")) {
                                    c = 27;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1567330472:
                                if (name.equals("default-enabled-vr-app")) {
                                    c = 18;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1633270165:
                                if (name.equals("disabled-until-used-preinstalled-carrier-associated-app")) {
                                    c = 21;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1723146313:
                                if (name.equals("privapp-permissions")) {
                                    c = 23;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1723586945:
                                if (name.equals("bugreport-whitelisted")) {
                                    c = 28;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1793277898:
                                if (name.equals("overlay-config-signature")) {
                                    c = 31;
                                    break;
                                }
                                c = 65535;
                                break;
                            case 1954925533:
                                if (name.equals("allow-implicit-broadcast")) {
                                    c = 15;
                                    break;
                                }
                                c = 65535;
                                break;
                            default:
                                c = 65535;
                                break;
                        }
                        int type3 = type;
                        permReader = permReader2;
                        str = str2;
                        boolean lowRam2 = lowRam;
                        boolean allowImplicitBroadcasts2 = allowImplicitBroadcasts;
                        boolean allowOverrideAppRestrictions2 = allowOverrideAppRestrictions;
                        boolean allowAssociations2 = allowAssociations;
                        switch (c) {
                            case 0:
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                if (allowAll2) {
                                    String gidStr = xmlPullParser.getAttributeValue(null, "gid");
                                    if (gidStr != null) {
                                        int gid = Process.getGidForName(gidStr);
                                        this.mGlobalGids = ArrayUtils.appendInt(this.mGlobalGids, gid);
                                    } else {
                                        Slog.w(TAG, "<" + name + "> without gid in " + permFile + " at " + parser.getPositionDescription());
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                break;
                            case 1:
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                if (!allowPermissions) {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                    XmlUtils.skipCurrentTag(parser);
                                    break;
                                } else {
                                    String perm = xmlPullParser.getAttributeValue(null, "name");
                                    if (perm != null) {
                                        readPermission(xmlPullParser, perm.intern());
                                        break;
                                    } else {
                                        Slog.w(TAG, "<" + name + "> without name in " + permFile + " at " + parser.getPositionDescription());
                                        XmlUtils.skipCurrentTag(parser);
                                        break;
                                    }
                                }
                            case 2:
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                if (allowPermissions) {
                                    String perm2 = xmlPullParser.getAttributeValue(null, "name");
                                    if (perm2 == null) {
                                        Slog.w(TAG, "<" + name + "> without name in " + permFile + " at " + parser.getPositionDescription());
                                        XmlUtils.skipCurrentTag(parser);
                                        break;
                                    } else {
                                        String uidStr = xmlPullParser.getAttributeValue(null, "uid");
                                        if (uidStr == null) {
                                            Slog.w(TAG, "<" + name + "> without uid in " + permFile + " at " + parser.getPositionDescription());
                                            XmlUtils.skipCurrentTag(parser);
                                            break;
                                        } else {
                                            int uid = Process.getUidForName(uidStr);
                                            if (uid < 0) {
                                                Slog.w(TAG, "<" + name + "> with unknown uid \"" + uidStr + "  in " + permFile + " at " + parser.getPositionDescription());
                                                XmlUtils.skipCurrentTag(parser);
                                                break;
                                            } else {
                                                String perm3 = perm2.intern();
                                                ArraySet<String> perms = this.mSystemPermissions.get(uid);
                                                if (perms == null) {
                                                    perms = new ArraySet<>();
                                                    this.mSystemPermissions.put(uid, perms);
                                                }
                                                perms.add(perm3);
                                            }
                                        }
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                break;
                            case 3:
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                if (!allowPermissions) {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                    XmlUtils.skipCurrentTag(parser);
                                    break;
                                } else {
                                    readSplitPermission(parser, permFile);
                                    break;
                                }
                            case 4:
                            case 5:
                                if (allowLibs) {
                                    try {
                                        String lname = xmlPullParser.getAttributeValue(null, "name");
                                        String lfile = xmlPullParser.getAttributeValue(null, "file");
                                        String ldependency = xmlPullParser.getAttributeValue(null, "dependency");
                                        String minDeviceSdk = xmlPullParser.getAttributeValue(null, "min-device-sdk");
                                        allowVendorApex2 = allowVendorApex;
                                        String maxDeviceSdk = xmlPullParser.getAttributeValue(null, "max-device-sdk");
                                        if (lname == null) {
                                            Slog.w(TAG, "<" + name + "> without name in " + permFile + " at " + parser.getPositionDescription());
                                            allowAll2 = allowAll;
                                        } else if (lfile == null) {
                                            Slog.w(TAG, "<" + name + "> without file in " + permFile + " at " + parser.getPositionDescription());
                                            allowAll2 = allowAll;
                                        } else {
                                            if (minDeviceSdk != null && !isAtLeastSdkLevel(minDeviceSdk)) {
                                                allowedMinSdk = false;
                                                allowedMaxSdk = maxDeviceSdk != null || isAtMostSdkLevel(maxDeviceSdk);
                                                exists = new File(lfile).exists();
                                                if (!allowedMinSdk && allowedMaxSdk && exists) {
                                                    allowAll2 = allowAll;
                                                    String bcpSince = xmlPullParser.getAttributeValue(null, "on-bootclasspath-since");
                                                    String bcpBefore = xmlPullParser.getAttributeValue(null, "on-bootclasspath-before");
                                                    SharedLibraryEntry entry = new SharedLibraryEntry(lname, lfile, ldependency == null ? new String[0] : ldependency.split(":"), bcpSince, bcpBefore);
                                                    this.mSharedLibraries.put(lname, entry);
                                                } else {
                                                    allowAll2 = allowAll;
                                                    StringBuilder msg = new StringBuilder("Ignore shared library ").append(lname).append(":");
                                                    if (!allowedMinSdk) {
                                                        msg.append(" min-device-sdk=").append(minDeviceSdk);
                                                    }
                                                    if (!allowedMaxSdk) {
                                                        msg.append(" max-device-sdk=").append(maxDeviceSdk);
                                                    }
                                                    if (!exists) {
                                                        msg.append(" ").append(lfile).append(" does not exist");
                                                    }
                                                    Slog.i(TAG, msg.toString());
                                                }
                                            }
                                            allowedMinSdk = true;
                                            if (maxDeviceSdk != null) {
                                            }
                                            exists = new File(lfile).exists();
                                            if (!allowedMinSdk) {
                                            }
                                            allowAll2 = allowAll;
                                            StringBuilder msg2 = new StringBuilder("Ignore shared library ").append(lname).append(":");
                                            if (!allowedMinSdk) {
                                            }
                                            if (!allowedMaxSdk) {
                                            }
                                            if (!exists) {
                                            }
                                            Slog.i(TAG, msg2.toString());
                                        }
                                        xmlPullParser = parser;
                                    } catch (IOException e5) {
                                        e = e5;
                                        e2 = e;
                                        Slog.w(TAG, str, e2);
                                        IoUtils.closeQuietly(permReader);
                                        if (StorageManager.isFileEncryptedNativeOnly()) {
                                        }
                                        if (StorageManager.hasAdoptable()) {
                                        }
                                        if (ActivityManager.isLowRamDeviceStatic()) {
                                        }
                                        int incrementalVersion2 = IncrementalManager.getVersion();
                                        if (incrementalVersion2 > 0) {
                                        }
                                        addFeature(PackageManager.FEATURE_APP_ENUMERATION, 0);
                                        if (Build.VERSION.DEVICE_INITIAL_SDK_INT >= 29) {
                                        }
                                        if (isErofsSupported()) {
                                        }
                                        Iterator<String> it2 = this.mUnavailableFeatures.iterator();
                                        while (it2.hasNext()) {
                                        }
                                        return;
                                    } catch (XmlPullParserException e6) {
                                        e = e6;
                                        e = e;
                                        str2 = str;
                                        Slog.w(TAG, str2, e);
                                        IoUtils.closeQuietly(permReader);
                                        if (StorageManager.isFileEncryptedNativeOnly()) {
                                        }
                                        if (StorageManager.hasAdoptable()) {
                                        }
                                        if (ActivityManager.isLowRamDeviceStatic()) {
                                        }
                                        int incrementalVersion22 = IncrementalManager.getVersion();
                                        if (incrementalVersion22 > 0) {
                                        }
                                        addFeature(PackageManager.FEATURE_APP_ENUMERATION, 0);
                                        if (Build.VERSION.DEVICE_INITIAL_SDK_INT >= 29) {
                                        }
                                        if (isErofsSupported()) {
                                        }
                                        Iterator<String> it22 = this.mUnavailableFeatures.iterator();
                                        while (it22.hasNext()) {
                                        }
                                        return;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        th = th;
                                        IoUtils.closeQuietly(permReader);
                                        throw th;
                                    }
                                } else {
                                    allowAll2 = allowAll;
                                    allowVendorApex2 = allowVendorApex;
                                    xmlPullParser = parser;
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                break;
                            case 6:
                                if (allowFeatures) {
                                    String fname = xmlPullParser.getAttributeValue(null, "name");
                                    int fversion = XmlUtils.readIntAttribute(xmlPullParser, "version", 0);
                                    if (lowRam2) {
                                        String notLowRam = xmlPullParser.getAttributeValue(null, "notLowRam");
                                        allowed = !"true".equals(notLowRam);
                                    } else {
                                        allowed = true;
                                    }
                                    if (fname == null) {
                                        Slog.w(TAG, "<" + name + "> without name in " + permFile + " at " + parser.getPositionDescription());
                                    } else if (allowed) {
                                        addFeature(fname, fversion);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 7:
                                if (allowFeatures) {
                                    String fname2 = xmlPullParser.getAttributeValue(null, "name");
                                    if (fname2 == null) {
                                        Slog.w(TAG, "<" + name + "> without name in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mUnavailableFeatures.add(fname2);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case '\b':
                                if (allowOverrideAppRestrictions2) {
                                    String pkgname = xmlPullParser.getAttributeValue(null, "package");
                                    if (pkgname == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mAllowInPowerSaveExceptIdle.add(pkgname);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case '\t':
                                if (allowOverrideAppRestrictions2) {
                                    String pkgname2 = xmlPullParser.getAttributeValue(null, "package");
                                    if (pkgname2 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mAllowInPowerSave.add(pkgname2);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case '\n':
                                if (allowOverrideAppRestrictions2) {
                                    String pkgname3 = xmlPullParser.getAttributeValue(null, "package");
                                    if (pkgname3 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mAllowInDataUsageSave.add(pkgname3);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 11:
                                if (allowOverrideAppRestrictions2) {
                                    String pkgname4 = xmlPullParser.getAttributeValue(null, "package");
                                    if (pkgname4 == null) {
                                        Slog.w(TAG, "<allow-in-idle-user-app> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mWhiteListedAllowInIdleUserApp.add(pkgname4);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case '\f':
                                if (allowOverrideAppRestrictions2) {
                                    String pkgname5 = xmlPullParser.getAttributeValue(null, "package");
                                    if (pkgname5 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mAllowUnthrottledLocation.add(pkgname5);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case '\r':
                                if (allowOverrideAppRestrictions2) {
                                    String pkgname6 = xmlPullParser.getAttributeValue(null, "package");
                                    String attributionTag = xmlPullParser.getAttributeValue(null, "attributionTag");
                                    if (pkgname6 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        ArraySet<String> tags = this.mAllowAdasSettings.get(pkgname6);
                                        if (tags == null || !tags.isEmpty()) {
                                            if (tags == null) {
                                                tags = new ArraySet<>(1);
                                                this.mAllowAdasSettings.put(pkgname6, tags);
                                            }
                                            if (!"*".equals(attributionTag)) {
                                                if ("null".equals(attributionTag)) {
                                                    attributionTag = null;
                                                }
                                                tags.add(attributionTag);
                                            }
                                        }
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 14:
                                if (allowOverrideAppRestrictions2) {
                                    String pkgname7 = xmlPullParser.getAttributeValue(null, "package");
                                    String attributionTag2 = xmlPullParser.getAttributeValue(null, "attributionTag");
                                    if (pkgname7 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        ArraySet<String> tags2 = this.mAllowIgnoreLocationSettings.get(pkgname7);
                                        if (tags2 == null || !tags2.isEmpty()) {
                                            if (tags2 == null) {
                                                tags2 = new ArraySet<>(1);
                                                this.mAllowIgnoreLocationSettings.put(pkgname7, tags2);
                                            }
                                            if (!"*".equals(attributionTag2)) {
                                                if ("null".equals(attributionTag2)) {
                                                    attributionTag2 = null;
                                                }
                                                tags2.add(attributionTag2);
                                            }
                                        }
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 15:
                                if (allowImplicitBroadcasts2) {
                                    String action = xmlPullParser.getAttributeValue(null, "action");
                                    if (action == null) {
                                        Slog.w(TAG, "<" + name + "> without action in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mAllowImplicitBroadcasts.add(action);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 16:
                                if (allowAppConfigs) {
                                    String pkgname8 = xmlPullParser.getAttributeValue(null, "package");
                                    if (pkgname8 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mLinkedApps.add(pkgname8);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 17:
                                if (allowOverrideAppRestrictions2) {
                                    String pkgname9 = xmlPullParser.getAttributeValue(null, "package");
                                    if (pkgname9 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mBgRestrictionExemption.add(pkgname9);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 18:
                                if (allowAppConfigs) {
                                    String pkgname10 = xmlPullParser.getAttributeValue(null, "package");
                                    String clsname = xmlPullParser.getAttributeValue(null, "class");
                                    if (pkgname10 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else if (clsname == null) {
                                        Slog.w(TAG, "<" + name + "> without class in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mDefaultVrComponents.add(new ComponentName(pkgname10, clsname));
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 19:
                                readComponentOverrides(parser, permFile);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 20:
                                if (allowFeatures) {
                                    String serviceName = xmlPullParser.getAttributeValue(null, "service");
                                    if (serviceName == null) {
                                        Slog.w(TAG, "<" + name + "> without service in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        ComponentName cn = ComponentName.unflattenFromString(serviceName);
                                        if (cn == null) {
                                            Slog.w(TAG, "<" + name + "> with invalid service name " + serviceName + " in " + permFile + " at " + parser.getPositionDescription());
                                        } else {
                                            this.mBackupTransportWhitelist.add(cn);
                                        }
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 21:
                                if (allowAppConfigs) {
                                    String pkgname11 = xmlPullParser.getAttributeValue(null, "package");
                                    String carrierPkgname = xmlPullParser.getAttributeValue(null, "carrierAppPackage");
                                    if (pkgname11 != null && carrierPkgname != null) {
                                        int addedInSdk = -1;
                                        String addedInSdkStr = xmlPullParser.getAttributeValue(null, "addedInSdk");
                                        if (!TextUtils.isEmpty(addedInSdkStr)) {
                                            try {
                                                addedInSdk = Integer.parseInt(addedInSdkStr);
                                            } catch (NumberFormatException e7) {
                                                Slog.w(TAG, "<" + name + "> addedInSdk not an integer in " + permFile + " at " + parser.getPositionDescription());
                                                XmlUtils.skipCurrentTag(parser);
                                                allowAll2 = allowAll;
                                                allowVendorApex2 = allowVendorApex;
                                                type = type3;
                                                permReader2 = permReader;
                                                str2 = str;
                                                lowRam = lowRam2;
                                                allowImplicitBroadcasts = allowImplicitBroadcasts2;
                                                allowOverrideAppRestrictions = allowOverrideAppRestrictions2;
                                                allowAssociations = allowAssociations2;
                                                allowVendorApex = allowVendorApex2;
                                                allowAll = allowAll2;
                                            }
                                        }
                                        List<CarrierAssociatedAppEntry> associatedPkgs = this.mDisabledUntilUsedPreinstalledCarrierAssociatedApps.get(carrierPkgname);
                                        if (associatedPkgs == null) {
                                            associatedPkgs = new ArrayList();
                                            this.mDisabledUntilUsedPreinstalledCarrierAssociatedApps.put(carrierPkgname, associatedPkgs);
                                        }
                                        associatedPkgs.add(new CarrierAssociatedAppEntry(pkgname11, addedInSdk));
                                    }
                                    Slog.w(TAG, "<" + name + "> without package or carrierAppPackage in " + permFile + " at " + parser.getPositionDescription());
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                            case 22:
                                if (allowAppConfigs) {
                                    String pkgname12 = xmlPullParser.getAttributeValue(null, "package");
                                    if (pkgname12 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mDisabledUntilUsedPreinstalledCarrierApps.add(pkgname12);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 23:
                                if (!allowPrivappPermissions) {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                    XmlUtils.skipCurrentTag(parser);
                                    allowAll2 = allowAll;
                                    allowVendorApex2 = allowVendorApex;
                                    break;
                                } else {
                                    if (!permFile.toPath().startsWith(Environment.getVendorDirectory().toPath() + "/") && !permFile.toPath().startsWith(Environment.getOdmDirectory().toPath() + "/")) {
                                        vendor2 = false;
                                        boolean product = permFile.toPath().startsWith(Environment.getProductDirectory().toPath() + "/");
                                        boolean systemExt = permFile.toPath().startsWith(Environment.getSystemExtDirectory().toPath() + "/");
                                        boolean apex = !permFile.toPath().startsWith(new StringBuilder().append(Environment.getApexDirectory().toPath()).append("/").toString()) && ApexProperties.updatable().orElse(false).booleanValue();
                                        if (!vendor2) {
                                            readPrivAppPermissions(xmlPullParser, this.mVendorPrivAppPermissions, this.mVendorPrivAppDenyPermissions);
                                        } else if (product) {
                                            readPrivAppPermissions(xmlPullParser, this.mProductPrivAppPermissions, this.mProductPrivAppDenyPermissions);
                                        } else if (systemExt) {
                                            readPrivAppPermissions(xmlPullParser, this.mSystemExtPrivAppPermissions, this.mSystemExtPrivAppDenyPermissions);
                                        } else if (apex) {
                                            readApexPrivAppPermissions(xmlPullParser, permFile, Environment.getApexDirectory().toPath());
                                        } else if (true == SystemProperties.getBoolean("ro.tran.partition.extend", false)) {
                                            readTrPrivAppPermissions(parser, permFile);
                                        } else {
                                            readPrivAppPermissions(xmlPullParser, this.mPrivAppPermissions, this.mPrivAppDenyPermissions);
                                        }
                                        allowAll2 = allowAll;
                                        allowVendorApex2 = allowVendorApex;
                                        break;
                                    }
                                    vendor2 = true;
                                    boolean product2 = permFile.toPath().startsWith(Environment.getProductDirectory().toPath() + "/");
                                    boolean systemExt2 = permFile.toPath().startsWith(Environment.getSystemExtDirectory().toPath() + "/");
                                    if (permFile.toPath().startsWith(new StringBuilder().append(Environment.getApexDirectory().toPath()).append("/").toString())) {
                                    }
                                    if (!vendor2) {
                                    }
                                    allowAll2 = allowAll;
                                    allowVendorApex2 = allowVendorApex;
                                }
                                break;
                            case 24:
                                if (!allowOemPermissions) {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                    XmlUtils.skipCurrentTag(parser);
                                    allowAll2 = allowAll;
                                    allowVendorApex2 = allowVendorApex;
                                    break;
                                } else {
                                    readOemPermissions(parser);
                                    allowAll2 = allowAll;
                                    allowVendorApex2 = allowVendorApex;
                                    break;
                                }
                            case 25:
                                if (allowApiWhitelisting) {
                                    String pkgname13 = xmlPullParser.getAttributeValue(null, "package");
                                    if (pkgname13 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mHiddenApiPackageWhitelist.add(pkgname13);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 26:
                                if (allowAssociations2) {
                                    String target = xmlPullParser.getAttributeValue(null, "target");
                                    if (target == null) {
                                        Slog.w(TAG, "<" + name + "> without target in " + permFile + " at " + parser.getPositionDescription());
                                        XmlUtils.skipCurrentTag(parser);
                                        allowAll2 = allowAll;
                                        allowVendorApex2 = allowVendorApex;
                                        break;
                                    } else {
                                        String allowed2 = xmlPullParser.getAttributeValue(null, "allowed");
                                        if (allowed2 == null) {
                                            Slog.w(TAG, "<" + name + "> without allowed in " + permFile + " at " + parser.getPositionDescription());
                                            XmlUtils.skipCurrentTag(parser);
                                            allowAll2 = allowAll;
                                            allowVendorApex2 = allowVendorApex;
                                            break;
                                        } else {
                                            String target2 = target.intern();
                                            String allowed3 = allowed2.intern();
                                            ArraySet<String> associations = this.mAllowedAssociations.get(target2);
                                            if (associations == null) {
                                                associations = new ArraySet<>();
                                                this.mAllowedAssociations.put(target2, associations);
                                            }
                                            Slog.i(TAG, "Adding association: " + target2 + " <- " + allowed3);
                                            associations.add(allowed3);
                                        }
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 27:
                                String pkgname14 = xmlPullParser.getAttributeValue(null, "package");
                                if (pkgname14 == null) {
                                    Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                } else {
                                    this.mAppDataIsolationWhitelistedApps.add(pkgname14);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 28:
                                String pkgname15 = xmlPullParser.getAttributeValue(null, "package");
                                if (pkgname15 == null) {
                                    Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                } else {
                                    this.mBugreportWhitelistedPackages.add(pkgname15);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 29:
                                readInstallInUserType(xmlPullParser, this.mPackageToUserTypeWhitelist, this.mPackageToUserTypeBlacklist);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 30:
                                String namespace = TextUtils.safeIntern(xmlPullParser.getAttributeValue(null, Settings.EXTRA_NAMESPACE));
                                String actorName = xmlPullParser.getAttributeValue(null, "name");
                                String pkgName = TextUtils.safeIntern(xmlPullParser.getAttributeValue(null, "package"));
                                if (TextUtils.isEmpty(namespace)) {
                                    Slog.wtf(TAG, "<" + name + "> without namespace in " + permFile + " at " + parser.getPositionDescription());
                                } else if (TextUtils.isEmpty(actorName)) {
                                    Slog.wtf(TAG, "<" + name + "> without actor name in " + permFile + " at " + parser.getPositionDescription());
                                } else if (TextUtils.isEmpty(pkgName)) {
                                    Slog.wtf(TAG, "<" + name + "> without package name in " + permFile + " at " + parser.getPositionDescription());
                                } else if ("android".equalsIgnoreCase(namespace)) {
                                    throw new IllegalStateException("Defining " + actorName + " as " + pkgName + " for the android namespace is not allowed");
                                } else {
                                    if (this.mNamedActors == null) {
                                        this.mNamedActors = new ArrayMap();
                                    }
                                    Map<String, String> nameToPkgMap = this.mNamedActors.get(namespace);
                                    if (nameToPkgMap == null) {
                                        nameToPkgMap = new ArrayMap();
                                        this.mNamedActors.put(namespace, nameToPkgMap);
                                    } else if (nameToPkgMap.containsKey(actorName)) {
                                        String existing = nameToPkgMap.get(actorName);
                                        throw new IllegalStateException("Duplicate actor definition for " + namespace + "/" + actorName + "; defined as both " + existing + " and " + pkgName);
                                    }
                                    nameToPkgMap.put(actorName, pkgName);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case 31:
                                if (allowAll) {
                                    String pkgName2 = xmlPullParser.getAttributeValue(null, "package");
                                    if (pkgName2 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else if (!TextUtils.isEmpty(this.mOverlayConfigSignaturePackage)) {
                                        throw new IllegalStateException("Reference signature package defined as both " + this.mOverlayConfigSignaturePackage + " and " + pkgName2);
                                    } else {
                                        this.mOverlayConfigSignaturePackage = pkgName2.intern();
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case ' ':
                                String pkgname16 = xmlPullParser.getAttributeValue(null, "package");
                                if (pkgname16 == null) {
                                    Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                } else {
                                    this.mRollbackWhitelistedPackages.add(pkgname16);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case '!':
                                if (allowAppConfigs) {
                                    String pkgname17 = xmlPullParser.getAttributeValue(null, "package");
                                    boolean isModulesInstaller = XmlUtils.readBooleanAttribute(xmlPullParser, "isModulesInstaller", false);
                                    if (pkgname17 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    } else {
                                        this.mWhitelistedStagedInstallers.add(pkgname17);
                                    }
                                    if (isModulesInstaller) {
                                        if (this.mModulesInstallerPackageName != null) {
                                            throw new IllegalStateException("Multiple modules installers");
                                        }
                                        this.mModulesInstallerPackageName = pkgname17;
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            case '\"':
                                if (allowVendorApex) {
                                    String pkgName3 = xmlPullParser.getAttributeValue(null, "package");
                                    String installerPkgName = xmlPullParser.getAttributeValue(null, "installerPackage");
                                    if (pkgName3 == null) {
                                        Slog.w(TAG, "<" + name + "> without package in " + permFile + " at " + parser.getPositionDescription());
                                    }
                                    if (installerPkgName == null) {
                                        Slog.w(TAG, "<" + name + "> without installerPackage in " + permFile + " at " + parser.getPositionDescription());
                                    }
                                    if (pkgName3 != null && installerPkgName != null) {
                                        this.mAllowedVendorApexes.put(pkgName3, installerPkgName);
                                    }
                                } else {
                                    logNotAllowedInPartition(name, permFile, xmlPullParser);
                                }
                                XmlUtils.skipCurrentTag(parser);
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                break;
                            default:
                                allowAll2 = allowAll;
                                allowVendorApex2 = allowVendorApex;
                                Slog.w(TAG, "Tag " + name + " is unknown in " + permFile + " at " + parser.getPositionDescription());
                                XmlUtils.skipCurrentTag(parser);
                                break;
                        }
                        type = type3;
                        permReader2 = permReader;
                        str2 = str;
                        lowRam = lowRam2;
                        allowImplicitBroadcasts = allowImplicitBroadcasts2;
                        allowOverrideAppRestrictions = allowOverrideAppRestrictions2;
                        allowAssociations = allowAssociations2;
                        allowVendorApex = allowVendorApex2;
                        allowAll = allowAll2;
                    }
                }
            }
        } catch (FileNotFoundException e8) {
            Slog.w(TAG, "Couldn't find or open permissions file " + permFile);
        }
    }

    private void addFeature(String name, int version) {
        FeatureInfo fi = this.mAvailableFeatures.get(name);
        if (fi == null) {
            FeatureInfo fi2 = new FeatureInfo();
            fi2.name = name;
            fi2.version = version;
            this.mAvailableFeatures.put(name, fi2);
            return;
        }
        fi.version = Math.max(fi.version, version);
    }

    private void removeFeature(String name) {
        if (this.mAvailableFeatures.remove(name) != null) {
            Slog.d(TAG, "Removed unavailable feature " + name);
        }
    }

    void readPermission(XmlPullParser parser, String name) throws IOException, XmlPullParserException {
        if (this.mPermissions.containsKey(name)) {
            throw new IllegalStateException("Duplicate permission definition for " + name);
        }
        boolean perUser = XmlUtils.readBooleanAttribute(parser, "perUser", false);
        PermissionEntry perm = new PermissionEntry(name, perUser);
        this.mPermissions.put(name, perm);
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if ("group".equals(tagName)) {
                            String gidStr = parser.getAttributeValue(null, "gid");
                            if (gidStr != null) {
                                int gid = Process.getGidForName(gidStr);
                                perm.gids = ArrayUtils.appendInt(perm.gids, gid);
                            } else {
                                Slog.w(TAG, "<group> without gid at " + parser.getPositionDescription());
                            }
                        }
                        XmlUtils.skipCurrentTag(parser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readPrivAppPermissions(XmlPullParser parser, ArrayMap<String, ArraySet<String>> grantMap, ArrayMap<String, ArraySet<String>> denyMap) throws IOException, XmlPullParserException {
        String packageName = parser.getAttributeValue(null, "package");
        if (TextUtils.isEmpty(packageName)) {
            Slog.w(TAG, "package is required for <privapp-permissions> in " + parser.getPositionDescription());
            return;
        }
        ArraySet<String> permissions = grantMap.get(packageName);
        if (permissions == null) {
            permissions = new ArraySet<>();
        }
        ArraySet<String> denyPermissions = denyMap.get(packageName);
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            String name = parser.getName();
            if ("permission".equals(name)) {
                String permName = parser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(permName)) {
                    Slog.w(TAG, "name is required for <permission> in " + parser.getPositionDescription());
                } else {
                    permissions.add(permName);
                }
            } else if ("deny-permission".equals(name)) {
                String permName2 = parser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(permName2)) {
                    Slog.w(TAG, "name is required for <deny-permission> in " + parser.getPositionDescription());
                } else {
                    if (denyPermissions == null) {
                        denyPermissions = new ArraySet<>();
                    }
                    denyPermissions.add(permName2);
                }
            }
        }
        grantMap.put(packageName, permissions);
        if (denyPermissions != null) {
            denyMap.put(packageName, denyPermissions);
        }
    }

    private void readInstallInUserType(XmlPullParser parser, Map<String, Set<String>> doInstallMap, Map<String, Set<String>> nonInstallMap) throws IOException, XmlPullParserException {
        String packageName = parser.getAttributeValue(null, "package");
        if (TextUtils.isEmpty(packageName)) {
            Slog.w(TAG, "package is required for <install-in-user-type> in " + parser.getPositionDescription());
            return;
        }
        Set<String> userTypesYes = doInstallMap.get(packageName);
        Set<String> userTypesNo = nonInstallMap.get(packageName);
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            String name = parser.getName();
            if ("install-in".equals(name)) {
                String userType = parser.getAttributeValue(null, "user-type");
                if (TextUtils.isEmpty(userType)) {
                    Slog.w(TAG, "user-type is required for <install-in-user-type> in " + parser.getPositionDescription());
                } else {
                    if (userTypesYes == null) {
                        userTypesYes = new ArraySet<>();
                        doInstallMap.put(packageName, userTypesYes);
                    }
                    userTypesYes.add(userType);
                }
            } else if ("do-not-install-in".equals(name)) {
                String userType2 = parser.getAttributeValue(null, "user-type");
                if (TextUtils.isEmpty(userType2)) {
                    Slog.w(TAG, "user-type is required for <install-in-user-type> in " + parser.getPositionDescription());
                } else {
                    if (userTypesNo == null) {
                        userTypesNo = new ArraySet();
                        nonInstallMap.put(packageName, userTypesNo);
                    }
                    userTypesNo.add(userType2);
                }
            } else {
                Slog.w(TAG, "unrecognized tag in <install-in-user-type> in " + parser.getPositionDescription());
            }
        }
    }

    void readOemPermissions(XmlPullParser parser) throws IOException, XmlPullParserException {
        String packageName = parser.getAttributeValue(null, "package");
        if (TextUtils.isEmpty(packageName)) {
            Slog.w(TAG, "package is required for <oem-permissions> in " + parser.getPositionDescription());
            return;
        }
        ArrayMap<String, Boolean> permissions = this.mOemPermissions.get(packageName);
        if (permissions == null) {
            permissions = new ArrayMap<>();
        }
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            String name = parser.getName();
            if ("permission".equals(name)) {
                String permName = parser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(permName)) {
                    Slog.w(TAG, "name is required for <permission> in " + parser.getPositionDescription());
                } else {
                    permissions.put(permName, Boolean.TRUE);
                }
            } else if ("deny-permission".equals(name)) {
                String permName2 = parser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(permName2)) {
                    Slog.w(TAG, "name is required for <deny-permission> in " + parser.getPositionDescription());
                } else {
                    permissions.put(permName2, Boolean.FALSE);
                }
            }
        }
        this.mOemPermissions.put(packageName, permissions);
    }

    private void readSplitPermission(XmlPullParser parser, File permFile) throws IOException, XmlPullParserException {
        String splitPerm = parser.getAttributeValue(null, "name");
        if (splitPerm != null) {
            String targetSdkStr = parser.getAttributeValue(null, "targetSdk");
            int targetSdk = 10001;
            if (!TextUtils.isEmpty(targetSdkStr)) {
                try {
                    targetSdk = Integer.parseInt(targetSdkStr);
                } catch (NumberFormatException e) {
                    Slog.w(TAG, "<split-permission> targetSdk not an integer in " + permFile + " at " + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                    return;
                }
            }
            int depth = parser.getDepth();
            List<String> newPermissions = new ArrayList<>();
            while (XmlUtils.nextElementWithin(parser, depth)) {
                String name = parser.getName();
                if ("new-permission".equals(name)) {
                    String newName = parser.getAttributeValue(null, "name");
                    if (TextUtils.isEmpty(newName)) {
                        Slog.w(TAG, "name is required for <new-permission> in " + parser.getPositionDescription());
                    } else {
                        newPermissions.add(newName);
                    }
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
            if (!newPermissions.isEmpty()) {
                this.mSplitPermissions.add(new PermissionManager.SplitPermissionInfo(splitPerm, newPermissions, targetSdk));
                return;
            }
            return;
        }
        Slog.w(TAG, "<split-permission> without name in " + permFile + " at " + parser.getPositionDescription());
        XmlUtils.skipCurrentTag(parser);
    }

    private void readComponentOverrides(XmlPullParser parser, File permFile) throws IOException, XmlPullParserException {
        String pkgname = parser.getAttributeValue(null, "package");
        if (pkgname == null) {
            Slog.w(TAG, "<component-override> without package in " + permFile + " at " + parser.getPositionDescription());
            return;
        }
        String pkgname2 = pkgname.intern();
        int depth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, depth)) {
            if (CardEmulation.EXTRA_SERVICE_COMPONENT.equals(parser.getName())) {
                String clsname = parser.getAttributeValue(null, "class");
                String enabled = parser.getAttributeValue(null, "enabled");
                if (clsname == null) {
                    Slog.w(TAG, "<component> without class in " + permFile + " at " + parser.getPositionDescription());
                    return;
                } else if (enabled == null) {
                    Slog.w(TAG, "<component> without enabled in " + permFile + " at " + parser.getPositionDescription());
                    return;
                } else {
                    if (clsname.startsWith(MediaMetrics.SEPARATOR)) {
                        clsname = pkgname2 + clsname;
                    }
                    String clsname2 = clsname.intern();
                    ArrayMap<String, Boolean> componentEnabledStates = this.mPackageComponentEnabledState.get(pkgname2);
                    if (componentEnabledStates == null) {
                        componentEnabledStates = new ArrayMap<>();
                        this.mPackageComponentEnabledState.put(pkgname2, componentEnabledStates);
                    }
                    componentEnabledStates.put(clsname2, Boolean.valueOf(!"false".equals(enabled)));
                }
            }
        }
    }

    private void readPublicNativeLibrariesList() {
        readPublicLibrariesListFile(new File("/vendor/etc/public.libraries.txt"));
        String[] dirs = {"/system/etc", "/system_ext/etc", "/product/etc"};
        for (String dir : dirs) {
            File[] files = new File(dir).listFiles();
            if (files == null) {
                Slog.w(TAG, "Public libraries file folder missing: " + dir);
            } else {
                for (File f : files) {
                    String name = f.getName();
                    if (name.startsWith("public.libraries-") && name.endsWith(".txt")) {
                        readPublicLibrariesListFile(f);
                    }
                }
            }
        }
    }

    private void readPublicLibrariesListFile(File listFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(listFile));
            while (true) {
                String line = br.readLine();
                if (line != null) {
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        String soname = line.trim().split(" ")[0];
                        SharedLibraryEntry entry = new SharedLibraryEntry(soname, soname, new String[0], true);
                        this.mSharedLibraries.put(entry.name, entry);
                    }
                } else {
                    br.close();
                    return;
                }
            }
        } catch (IOException e) {
            Slog.w(TAG, "Failed to read public libraries file " + listFile, e);
        }
    }

    private String getApexModuleNameFromFilePath(Path path, Path apexDirectoryPath) {
        if (!path.startsWith(apexDirectoryPath)) {
            throw new IllegalArgumentException("File " + path + " is not part of an APEX.");
        }
        if (path.getNameCount() <= apexDirectoryPath.getNameCount() + 1) {
            throw new IllegalArgumentException("File " + path + " is in the APEX partition, but not inside a module.");
        }
        return path.getName(apexDirectoryPath.getNameCount()).toString();
    }

    public void readApexPrivAppPermissions(XmlPullParser parser, File permFile, Path apexDirectoryPath) throws IOException, XmlPullParserException {
        ArrayMap<String, ArraySet<String>> privAppPermissions;
        ArrayMap<String, ArraySet<String>> privAppDenyPermissions;
        String moduleName = getApexModuleNameFromFilePath(permFile.toPath(), apexDirectoryPath);
        if (this.mApexPrivAppPermissions.containsKey(moduleName)) {
            privAppPermissions = this.mApexPrivAppPermissions.get(moduleName);
        } else {
            privAppPermissions = new ArrayMap<>();
            this.mApexPrivAppPermissions.put(moduleName, privAppPermissions);
        }
        if (this.mApexPrivAppDenyPermissions.containsKey(moduleName)) {
            privAppDenyPermissions = this.mApexPrivAppDenyPermissions.get(moduleName);
        } else {
            privAppDenyPermissions = new ArrayMap<>();
            this.mApexPrivAppDenyPermissions.put(moduleName, privAppDenyPermissions);
        }
        readPrivAppPermissions(parser, privAppPermissions, privAppDenyPermissions);
    }

    private static boolean isSystemProcess() {
        return Process.myUid() == 1000;
    }

    private static boolean isErofsSupported() {
        try {
            Path path = Paths.get("/sys/fs/erofs", new String[0]);
            return Files.exists(path, new LinkOption[0]);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isKernelVersionAtLeast(int major, int minor) {
        String kernelVersion = VintfRuntimeInfo.getKernelVersion();
        String[] parts = kernelVersion.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int majorVersion = Integer.parseInt(parts[0]);
            int minorVersion = Integer.parseInt(parts[1]);
            return majorVersion > major || (majorVersion == major && minorVersion >= minor);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
