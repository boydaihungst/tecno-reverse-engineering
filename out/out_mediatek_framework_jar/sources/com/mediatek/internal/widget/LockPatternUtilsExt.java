package com.mediatek.internal.widget;
/* loaded from: classes.dex */
public class LockPatternUtilsExt {
    private static final int ALLOWING_FR = 4;

    public static boolean isFRAllowedForUser(int stongAuthForUser) {
        return (stongAuthForUser & (-5)) == 0;
    }
}
