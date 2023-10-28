package com.android.server.pm.pkg;

import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureGroupInfo;
import android.content.pm.FeatureInfo;
import android.util.SparseArray;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedAttribution;
import com.android.server.pm.pkg.component.ParsedInstrumentation;
import com.android.server.pm.pkg.component.ParsedPermission;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedService;
import java.util.List;
/* loaded from: classes2.dex */
public interface AndroidPackageApi {
    boolean areAttributionsUserVisible();

    List<ParsedActivity> getActivities();

    List<String> getAdoptPermissions();

    String getAppComponentFactory();

    List<ParsedAttribution> getAttributions();

    int getAutoRevokePermissions();

    String getBackupAgentName();

    int getBanner();

    String getBaseApkPath();

    int getBaseRevisionCode();

    int getCategory();

    String getClassLoaderName();

    String getClassName();

    int getCompatibleWidthLimitDp();

    int getCompileSdkVersion();

    String getCompileSdkVersionCodeName();

    List<ConfigurationInfo> getConfigPreferences();

    int getDataExtractionRules();

    int getDescriptionRes();

    List<FeatureGroupInfo> getFeatureGroups();

    int getFullBackupContent();

    int getGwpAsanMode();

    int getIconRes();

    List<String> getImplicitPermissions();

    int getInstallLocation();

    List<ParsedInstrumentation> getInstrumentations();

    int getLabelRes();

    int getLargestWidthLimitDp();

    int getLogo();

    long getLongVersionCode();

    String getManageSpaceActivityName();

    float getMaxAspectRatio();

    int getMemtagMode();

    float getMinAspectRatio();

    int getMinSdkVersion();

    int getNativeHeapZeroInitialized();

    String getNativeLibraryDir();

    String getNativeLibraryRootDir();

    int getNetworkSecurityConfigRes();

    CharSequence getNonLocalizedLabel();

    String getPackageName();

    String getPath();

    String getPermission();

    List<ParsedPermission> getPermissions();

    String getProcessName();

    List<ParsedProvider> getProviders();

    List<ParsedActivity> getReceivers();

    List<FeatureInfo> getRequestedFeatures();

    List<String> getRequestedPermissions();

    String getRequiredAccountType();

    int getRequiresSmallestWidthDp();

    Boolean getResizeableActivity();

    String getRestrictedAccountType();

    int getRoundIconRes();

    String getSecondaryNativeLibraryDir();

    List<ParsedService> getServices();

    String getSharedUserId();

    int getSharedUserLabel();

    String[] getSplitClassLoaderNames();

    String[] getSplitCodePaths();

    SparseArray<int[]> getSplitDependencies();

    String[] getSplitNames();

    int[] getSplitRevisionCodes();

    int getTargetSandboxVersion();

    int getTargetSdkVersion();

    String getTaskAffinity();

    int getTheme();

    int getUiOptions();

    int getUid();

    String getVersionName();

    String getVolumeUuid();

    String getZygotePreloadName();

    boolean hasRequestForegroundServiceExemption();

    Boolean hasRequestRawExternalStorageAccess();

    boolean isAllowAudioPlaybackCapture();

    boolean isAllowBackup();

    boolean isAllowClearUserData();

    boolean isAllowClearUserDataOnFailedRestore();

    boolean isAllowNativeHeapPointerTagging();

    boolean isAllowTaskReparenting();

    boolean isAnyDensity();

    boolean isBackupInForeground();

    boolean isBaseHardwareAccelerated();

    boolean isCantSaveState();

    boolean isCoreApp();

    boolean isCrossProfile();

    boolean isDebuggable();

    boolean isDefaultToDeviceProtectedStorage();

    boolean isDirectBootAware();

    boolean isEnabled();

    boolean isExternalStorage();

    boolean isExtractNativeLibs();

    boolean isFactoryTest();

    boolean isFullBackupOnly();

    boolean isHasCode();

    boolean isHasDomainUrls();

    boolean isHasFragileUserData();

    boolean isIsolatedSplitLoading();

    boolean isKillAfterRestore();

    boolean isLargeHeap();

    boolean isMultiArch();

    boolean isNativeLibraryRootRequiresIsa();

    boolean isOdm();

    boolean isOem();

    boolean isOverlay();

    boolean isPartiallyDirectBootAware();

    boolean isPersistent();

    boolean isPrivileged();

    boolean isProduct();

    boolean isProfileable();

    boolean isProfileableByShell();

    boolean isRequestLegacyExternalStorage();

    boolean isRequiredForAllUsers();

    boolean isResizeable();

    boolean isResizeableActivityViaSdkVersion();

    boolean isRestoreAnyVersion();

    boolean isSdkLibrary();

    boolean isSignedWithPlatformKey();

    boolean isStaticSharedLibrary();

    boolean isStub();

    boolean isSupportsExtraLargeScreens();

    boolean isSupportsLargeScreens();

    boolean isSupportsNormalScreens();

    boolean isSupportsRtl();

    boolean isSupportsSmallScreens();

    boolean isSystem();

    boolean isSystemExt();

    boolean isTestOnly();

    boolean isUseEmbeddedDex();

    boolean isUsesCleartextTraffic();

    boolean isUsesNonSdkApi();

    boolean isVendor();

    boolean isVmSafeMode();
}
