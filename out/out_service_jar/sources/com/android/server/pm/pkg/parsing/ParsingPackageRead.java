package com.android.server.pm.pkg.parsing;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.SigningDetails;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Pair;
import android.util.SparseIntArray;
import com.android.server.pm.pkg.component.ParsedApexSystemService;
import com.android.server.pm.pkg.component.ParsedAttribution;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedPermissionGroup;
import com.android.server.pm.pkg.component.ParsedProcess;
import com.android.server.pm.pkg.component.ParsedUsesPermission;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public interface ParsingPackageRead extends PkgWithoutStateAppInfo, PkgWithoutStatePackageInfo, ParsingPackageInternal {
    List<String> getAdoptPermissions();

    List<ParsedApexSystemService> getApexSystemServices();

    List<ParsedAttribution> getAttributions();

    List<String> getImplicitPermissions();

    Map<String, ArraySet<PublicKey>> getKeySetMapping();

    List<String> getLibraryNames();

    int getLocaleConfigRes();

    Bundle getMetaData();

    Set<String> getMimeGroups();

    SparseIntArray getMinExtensionVersions();

    List<String> getOriginalPackages();

    Map<String, String> getOverlayables();

    List<ParsedPermissionGroup> getPermissionGroups();

    List<Pair<String, ParsedIntentInfo>> getPreferredActivityFilters();

    Map<String, ParsedProcess> getProcesses();

    Map<String, PackageManager.Property> getProperties();

    List<String> getProtectedBroadcasts();

    List<Intent> getQueriesIntents();

    List<String> getQueriesPackages();

    Set<String> getQueriesProviders();

    byte[] getRestrictUpdateHash();

    String getSdkLibName();

    int getSdkLibVersionMajor();

    SigningDetails getSigningDetails();

    int[] getSplitFlags();

    String getStaticSharedLibName();

    long getStaticSharedLibVersion();

    Set<String> getUpgradeKeySets();

    List<String> getUsesLibraries();

    List<String> getUsesNativeLibraries();

    List<String> getUsesOptionalLibraries();

    List<String> getUsesOptionalNativeLibraries();

    List<ParsedUsesPermission> getUsesPermissions();

    List<String> getUsesSdkLibraries();

    String[][] getUsesSdkLibrariesCertDigests();

    long[] getUsesSdkLibrariesVersionsMajor();

    List<String> getUsesStaticLibraries();

    String[][] getUsesStaticLibrariesCertDigests();

    long[] getUsesStaticLibrariesVersions();

    boolean hasPreserveLegacyExternalStorage();

    boolean isForceQueryable();

    @Deprecated
    boolean isGame();

    boolean isLeavingSharedUid();

    boolean isOnBackInvokedCallbackEnabled();

    boolean isResetEnabledSettingsOnAppDataCleared();

    boolean isUse32BitAbi();

    boolean isVisibleToInstantApps();
}
