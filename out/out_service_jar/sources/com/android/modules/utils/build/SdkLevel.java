package com.android.modules.utils.build;

import android.os.Build;
/* loaded from: classes.dex */
public final class SdkLevel {
    private SdkLevel() {
    }

    public static boolean isAtLeastR() {
        return Build.VERSION.SDK_INT >= 30;
    }

    public static boolean isAtLeastS() {
        return Build.VERSION.SDK_INT >= 31;
    }

    public static boolean isAtLeastSv2() {
        return Build.VERSION.SDK_INT >= 32;
    }

    public static boolean isAtLeastT() {
        return Build.VERSION.SDK_INT >= 33;
    }

    private static boolean isAtLeastPreReleaseCodename(String codename) {
        return !"REL".equals(Build.VERSION.CODENAME) && Build.VERSION.CODENAME.compareTo(codename) >= 0;
    }
}
