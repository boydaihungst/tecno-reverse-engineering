package com.android.server.compat;

import com.android.internal.compat.IPlatformCompatNative;
/* loaded from: classes.dex */
public class PlatformCompatNative extends IPlatformCompatNative.Stub {
    private final PlatformCompat mPlatformCompat;

    public PlatformCompatNative(PlatformCompat platformCompat) {
        this.mPlatformCompat = platformCompat;
    }

    public void reportChangeByPackageName(long changeId, String packageName, int userId) {
        this.mPlatformCompat.reportChangeByPackageName(changeId, packageName, userId);
    }

    public void reportChangeByUid(long changeId, int uid) {
        this.mPlatformCompat.reportChangeByUid(changeId, uid);
    }

    public boolean isChangeEnabledByPackageName(long changeId, String packageName, int userId) {
        return this.mPlatformCompat.isChangeEnabledByPackageName(changeId, packageName, userId);
    }

    public boolean isChangeEnabledByUid(long changeId, int uid) {
        return this.mPlatformCompat.isChangeEnabledByUid(changeId, uid);
    }
}
