package com.android.server.pm.pkg;

import android.content.pm.SigningDetails;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.LegacyPermissionState;
import com.android.server.pm.pkg.component.ParsedProcess;
import java.util.List;
/* loaded from: classes2.dex */
public interface SharedUserApi {
    int getAppId();

    ArraySet<? extends PackageStateInternal> getDisabledPackageStates();

    String getName();

    ArraySet<? extends PackageStateInternal> getPackageStates();

    List<AndroidPackage> getPackages();

    int getPrivateUidFlags();

    ArrayMap<String, ParsedProcess> getProcesses();

    int getSeInfoTargetSdkVersion();

    LegacyPermissionState getSharedUserLegacyPermissionState();

    SigningDetails getSigningDetails();

    int getUidFlags();

    boolean isPrivileged();
}
