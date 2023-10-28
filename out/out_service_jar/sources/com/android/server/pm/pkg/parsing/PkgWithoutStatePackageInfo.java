package com.android.server.pm.pkg.parsing;

import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureGroupInfo;
import android.content.pm.FeatureInfo;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedInstrumentation;
import com.android.server.pm.pkg.component.ParsedPermission;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedService;
import java.util.List;
/* loaded from: classes2.dex */
public interface PkgWithoutStatePackageInfo {
    List<ParsedActivity> getActivities();

    int getBaseRevisionCode();

    int getCompileSdkVersion();

    String getCompileSdkVersionCodeName();

    List<ConfigurationInfo> getConfigPreferences();

    List<FeatureGroupInfo> getFeatureGroups();

    List<ParsedInstrumentation> getInstrumentations();

    long getLongVersionCode();

    String getPackageName();

    List<ParsedPermission> getPermissions();

    List<ParsedProvider> getProviders();

    List<ParsedActivity> getReceivers();

    List<FeatureInfo> getRequestedFeatures();

    List<String> getRequestedPermissions();

    String getRequiredAccountType();

    String getRestrictedAccountType();

    List<ParsedService> getServices();

    String getSharedUserId();

    int getSharedUserLabel();

    String[] getSplitNames();

    int[] getSplitRevisionCodes();

    String getVersionName();

    boolean isRequiredForAllUsers();
}
