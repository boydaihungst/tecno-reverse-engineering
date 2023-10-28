package com.android.server.pm.pkg.component;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public interface ParsedUsesPermission {
    public static final int FLAG_NEVER_FOR_LOCATION = 65536;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface UsesPermissionFlags {
    }

    String getName();

    int getUsesPermissionFlags();
}
