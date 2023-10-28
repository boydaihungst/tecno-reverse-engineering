package com.android.server.pm.pkg;

import android.content.pm.SigningDetails;
import android.util.SparseArray;
import com.android.server.pm.InstallSource;
import com.android.server.pm.PackageKeySetData;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.LegacyPermissionState;
import java.util.UUID;
/* loaded from: classes2.dex */
public interface PackageStateInternal extends PackageState {
    UUID getDomainSetId();

    int getFlags();

    InstallSource getInstallSource();

    PackageKeySetData getKeySetData();

    LegacyPermissionState getLegacyPermissionState();

    float getLoadingProgress();

    String getPathString();

    AndroidPackage getPkg();

    int getPrivateFlags();

    String getRealName();

    SigningDetails getSigningDetails();

    PackageStateUnserialized getTransientState();

    @Override // com.android.server.pm.pkg.PackageState
    SparseArray<? extends PackageUserStateInternal> getUserStates();

    boolean isLoading();

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.pm.pkg.PackageState
    default PackageUserStateInternal getUserStateOrDefault(int userId) {
        PackageUserStateInternal userState = getUserStates().get(userId);
        return userState == null ? PackageUserStateInternal.DEFAULT : userState;
    }
}
