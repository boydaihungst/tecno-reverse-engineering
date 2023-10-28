package com.android.server.pm.permission;

import java.util.List;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public interface LegacyPermissionDataProvider {
    Map<String, Set<String>> getAllAppOpPermissionPackages();

    int[] getGidsForUid(int i);

    LegacyPermissionState getLegacyPermissionState(int i);

    List<LegacyPermission> getLegacyPermissions();

    void writeLegacyPermissionStateTEMP();
}
