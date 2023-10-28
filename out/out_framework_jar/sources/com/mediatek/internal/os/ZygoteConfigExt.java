package com.mediatek.internal.os;

import android.content.ContentResolver;
import android.provider.Settings;
import android.util.Log;
/* loaded from: classes4.dex */
public class ZygoteConfigExt {
    public static AppDispatchPolicy DISPATCH_POLICY = PolicySelector.get();
    private static final String LOG_TAG = "app32_boost";

    public static boolean isApp32BoostEnabled() {
        return DISPATCH_POLICY != null;
    }

    public static boolean shouldAttemptApp32Boost(int zygotePolicyFlags, int runtimeFlags) {
        return isApp32BoostEnabled() && DISPATCH_POLICY.checkZygotePolicy(zygotePolicyFlags) && DISPATCH_POLICY.checkruntimeFlags(runtimeFlags);
    }

    public static int updateZygotePolicyFlags(ContentResolver cr, int zygotePolicyFlags) {
        if (isApp32BoostEnabled() && Settings.System.getInt(cr, LOG_TAG, 1) == 1) {
            return zygotePolicyFlags | Integer.MIN_VALUE;
        }
        return zygotePolicyFlags;
    }

    public static boolean checkPolicy(String packageName) {
        boolean enableBoost = false;
        if (packageName != null && (enableBoost = DISPATCH_POLICY.checkPackageName(packageName))) {
            Log.i(LOG_TAG, "run " + packageName);
        }
        return enableBoost;
    }
}
