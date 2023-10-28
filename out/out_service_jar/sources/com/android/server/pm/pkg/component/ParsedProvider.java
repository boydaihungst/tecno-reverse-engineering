package com.android.server.pm.pkg.component;

import android.content.pm.PathPermission;
import android.os.PatternMatcher;
import java.util.List;
/* loaded from: classes2.dex */
public interface ParsedProvider extends ParsedMainComponent {
    String getAuthority();

    int getInitOrder();

    List<PathPermission> getPathPermissions();

    String getReadPermission();

    List<PatternMatcher> getUriPermissionPatterns();

    String getWritePermission();

    boolean isForceUriPermissions();

    boolean isGrantUriPermissions();

    boolean isMultiProcess();

    boolean isSyncable();
}
