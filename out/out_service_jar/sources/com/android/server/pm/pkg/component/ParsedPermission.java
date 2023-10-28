package com.android.server.pm.pkg.component;

import java.util.Set;
/* loaded from: classes2.dex */
public interface ParsedPermission extends ParsedComponent {
    String getBackgroundPermission();

    String getGroup();

    Set<String> getKnownCerts();

    ParsedPermissionGroup getParsedPermissionGroup();

    int getProtectionLevel();

    int getRequestRes();

    boolean isTree();
}
