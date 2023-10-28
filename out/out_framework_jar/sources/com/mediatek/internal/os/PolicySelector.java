package com.mediatek.internal.os;

import android.os.SystemProperties;
/* loaded from: classes4.dex */
public class PolicySelector {
    private static final String PROP_APP32_BOOST_MODE = "ro.vendor.mtk.app32_boost_support";

    /* loaded from: classes4.dex */
    private class BoostMode {
        public static final int ALWAYS_ON = 1;
        public static final int BLACKLIST = 3;
        public static final int CUSTOM = 4;
        public static final int DISABLED = 0;
        public static final int WHITELIST = 2;

        private BoostMode() {
        }
    }

    private static int getBoostMode() {
        if (BoardApiLevelChecker.check(33)) {
            int boostMode = SystemProperties.getInt(PROP_APP32_BOOST_MODE, 0);
            return boostMode;
        }
        return 0;
    }

    public static AppDispatchPolicy get() {
        switch (getBoostMode()) {
            case 1:
                return new AlwaysOnPolicy();
            case 2:
                return new WhitelistPolicy();
            case 3:
                return new BlacklistPolicy();
            case 4:
                return new CustomPolicy();
            default:
                return null;
        }
    }
}
