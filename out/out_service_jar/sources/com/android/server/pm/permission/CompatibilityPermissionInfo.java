package com.android.server.pm.permission;

import android.annotation.NonNull;
import com.android.internal.util.AnnotationValidations;
/* loaded from: classes2.dex */
public class CompatibilityPermissionInfo {
    public static final CompatibilityPermissionInfo[] COMPAT_PERMS = {new CompatibilityPermissionInfo("android.permission.POST_NOTIFICATIONS", 33), new CompatibilityPermissionInfo("android.permission.WRITE_EXTERNAL_STORAGE", 4), new CompatibilityPermissionInfo("android.permission.READ_PHONE_STATE", 4)};
    private final String mName;
    private final int mSdkVersion;

    public CompatibilityPermissionInfo(String name, int sdkVersion) {
        this.mName = name;
        AnnotationValidations.validate(NonNull.class, (NonNull) null, name);
        this.mSdkVersion = sdkVersion;
    }

    public String getName() {
        return this.mName;
    }

    public int getSdkVersion() {
        return this.mSdkVersion;
    }

    @Deprecated
    private void __metadata() {
    }
}
