package com.android.server.pm.snapshot;

import android.content.pm.UserInfo;
import android.util.ArrayMap;
import com.android.server.pm.SharedUserSetting;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import java.util.Collection;
/* loaded from: classes2.dex */
public interface PackageDataSnapshot {
    Collection<SharedUserSetting> getAllSharedUsers();

    AndroidPackage getPackage(String str);

    ArrayMap<String, ? extends PackageStateInternal> getPackageStates();

    UserInfo[] getUserInfos();
}
