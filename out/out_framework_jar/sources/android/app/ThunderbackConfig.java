package android.app;

import android.media.AudioSystem;
import android.util.Log;
import java.lang.reflect.Method;
/* loaded from: classes.dex */
public class ThunderbackConfig {
    public static final int TB_MUTE_WIN_ID_V3 = -999;
    private static final String TB_VERSION_30 = "30";
    private static final String TB_VERSION_40 = "40";
    private static final String TB_VERSION_DEFAULT = "40";
    private static final String TB_VERSION_KEY = "ro.thunderback.version";
    private static String sCurVersion = null;
    private static Object sLock = new Object();

    private ThunderbackConfig() {
    }

    public static boolean isVersion3() {
        checkInitVersion();
        return TB_VERSION_30.equals(sCurVersion);
    }

    public static boolean isVersion4() {
        checkInitVersion();
        return "40".equals(sCurVersion);
    }

    public static boolean supportCompatibleSplitScreen() {
        boolean support = "1".equals(getSystemProperty("ro.tran_split_screen.top_bottom", AudioSystem.LEGACY_REMOTE_SUBMIX_ADDRESS));
        return support;
    }

    private static void checkInitVersion() {
        synchronized (sLock) {
            if (sCurVersion == null) {
                sCurVersion = getSystemProperty(TB_VERSION_KEY, "40");
                Log.d("ThunderbackConfig", "thundback version:" + sCurVersion);
            }
        }
    }

    private static String getSystemProperty(String key, String defaultValue) {
        try {
            try {
                Class<?> c = Class.forName("android.os.SystemProperties");
                Method methodGet = c.getMethod("get", String.class, String.class);
                if (methodGet != null) {
                    String value = (String) methodGet.invoke(c, key, defaultValue);
                    return value;
                }
                return defaultValue;
            } catch (Exception e) {
                e.printStackTrace();
                return defaultValue;
            }
        } catch (Throwable th) {
            return defaultValue;
        }
    }
}
