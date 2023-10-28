package com.android.server.pm.pkg.parsing;

import android.util.SparseArray;
import java.util.Set;
/* loaded from: classes2.dex */
public interface PkgWithoutStateAppInfo {
    boolean areAttributionsUserVisible();

    String getAppComponentFactory();

    int getAutoRevokePermissions();

    String getBackupAgentName();

    int getBanner();

    String getBaseApkPath();

    int getCategory();

    String getClassLoaderName();

    String getClassName();

    int getCompatibleWidthLimitDp();

    int getCompileSdkVersion();

    String getCompileSdkVersionCodeName();

    int getDataExtractionRules();

    int getDescriptionRes();

    int getFullBackupContent();

    int getGwpAsanMode();

    int getIconRes();

    int getInstallLocation();

    Set<String> getKnownActivityEmbeddingCerts();

    int getLabelRes();

    int getLargestWidthLimitDp();

    int getLogo();

    long getLongVersionCode();

    String getManageSpaceActivityName();

    float getMaxAspectRatio();

    int getMaxSdkVersion();

    int getMemtagMode();

    float getMinAspectRatio();

    int getMinSdkVersion();

    int getNativeHeapZeroInitialized();

    int getNetworkSecurityConfigRes();

    CharSequence getNonLocalizedLabel();

    String getPath();

    String getPermission();

    String getProcessName();

    int getRequiresSmallestWidthDp();

    Boolean getResizeableActivity();

    int getRoundIconRes();

    String[] getSplitClassLoaderNames();

    String[] getSplitCodePaths();

    SparseArray<int[]> getSplitDependencies();

    int getTargetSandboxVersion();

    int getTargetSdkVersion();

    String getTaskAffinity();

    int getTheme();

    int getUiOptions();

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

    boolean isCrossProfile();

    boolean isDebuggable();

    boolean isDefaultToDeviceProtectedStorage();

    boolean isDirectBootAware();

    boolean isEnabled();

    boolean isExternalStorage();

    boolean isExtractNativeLibs();

    boolean isFullBackupOnly();

    boolean isHasCode();

    boolean isHasDomainUrls();

    boolean isHasFragileUserData();

    boolean isIsolatedSplitLoading();

    boolean isKillAfterRestore();

    boolean isLargeHeap();

    boolean isMultiArch();

    boolean isOverlay();

    boolean isPartiallyDirectBootAware();

    boolean isPersistent();

    boolean isProfileable();

    boolean isProfileableByShell();

    boolean isRequestLegacyExternalStorage();

    boolean isResizeable();

    boolean isResizeableActivityViaSdkVersion();

    boolean isRestoreAnyVersion();

    boolean isSdkLibrary();

    boolean isStaticSharedLibrary();

    boolean isSupportsExtraLargeScreens();

    boolean isSupportsLargeScreens();

    boolean isSupportsNormalScreens();

    boolean isSupportsRtl();

    boolean isSupportsSmallScreens();

    boolean isTestOnly();

    boolean isUseEmbeddedDex();

    boolean isUsesCleartextTraffic();

    boolean isUsesNonSdkApi();

    boolean isVmSafeMode();
}
