package com.android.server.pm.pkg.mutate;

import android.util.ArraySet;
/* loaded from: classes2.dex */
public interface PackageStateWrite {
    void onChanged();

    PackageStateWrite setCategoryOverride(int i);

    PackageStateWrite setHiddenUntilInstalled(boolean z);

    PackageStateWrite setInstaller(String str);

    PackageStateWrite setLastPackageUsageTime(int i, long j);

    PackageStateWrite setLoadingProgress(float f);

    PackageStateWrite setMimeGroup(String str, ArraySet<String> arraySet);

    PackageStateWrite setOverrideSeInfo(String str);

    PackageStateWrite setRequiredForSystemUser(boolean z);

    PackageStateWrite setUpdateAvailable(boolean z);

    PackageUserStateWrite userState(int i);
}
