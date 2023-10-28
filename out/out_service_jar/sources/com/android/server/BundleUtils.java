package com.android.server;

import android.os.Bundle;
/* loaded from: classes.dex */
public final class BundleUtils {
    private BundleUtils() {
    }

    public static boolean isEmpty(Bundle in) {
        return in == null || in.size() == 0;
    }

    public static Bundle clone(Bundle in) {
        return in != null ? new Bundle(in) : new Bundle();
    }
}
