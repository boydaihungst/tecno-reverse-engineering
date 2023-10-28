package android.view;

import android.os.SystemProperties;
/* loaded from: classes3.dex */
final class TranFoldViewCustody {
    public static final boolean DEBUG = true;
    private static final boolean FOLD_SUPPORT = "1".equals(SystemProperties.get("ro.os_foldable_screen_support", ""));
    public static final String TAG = "os.fold";
    public static final int TRANSACTION_META_KEY_DISPLAY_PHYSICAL_ID = 15301;

    private TranFoldViewCustody() {
    }

    public static boolean disable() {
        return !FOLD_SUPPORT;
    }
}
