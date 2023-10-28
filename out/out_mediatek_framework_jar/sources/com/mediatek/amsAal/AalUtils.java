package com.mediatek.amsAal;

import android.content.ComponentName;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.Xml;
import com.mediatek.boostfwk.utils.Config;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public final class AalUtils {
    private static final int AAL_DEFAULT_LEVEL = 128;
    private static final int AAL_MAX_LEVEL = 256;
    private static final int AAL_MIN_LEVEL = 0;
    public static final int AAL_MODE_BALANCE = 1;
    public static final int AAL_MODE_LOWPOWER = 2;
    public static final int AAL_MODE_PERFORMANCE = 0;
    public static final int AAL_MODE_SIZE = 3;
    private static final int AAL_NULL_LEVEL = -1;
    private static final String TAG = "AalUtils";
    private static String sAalConfigXMLPath;
    private static String sAalConfigXMLProjectPath;
    private static boolean sDebug = false;
    private static boolean sEnabled;
    private static AalUtils sInstance;
    private static boolean sIsAalSupported;
    private int mAalMode = 1;
    private Map<AalIndex, Integer> mConfig = new HashMap();
    private AalConfig mCurrentConfig = null;

    private native void setSmartBacklightStrength(int i);

    static {
        boolean z = false;
        boolean equals = SystemProperties.get("ro.vendor.mtk_aal_support").equals(Config.USER_CONFIG_DEFAULT_TYPE);
        sIsAalSupported = equals;
        if (equals && SystemProperties.get("persist.vendor.sys.mtk_app_aal_support").equals(Config.USER_CONFIG_DEFAULT_TYPE)) {
            z = true;
        }
        sEnabled = z;
        sInstance = null;
        sAalConfigXMLPath = "/system/etc/ams_aal_config.xml";
        sAalConfigXMLProjectPath = "/vendor/etc/ams_aal_config.xml";
    }

    AalUtils() {
        SystemProperties.set("persist.vendor.sys.mtk_app_aal_support", Config.USER_CONFIG_DEFAULT_TYPE);
        sEnabled = true;
        if (!sIsAalSupported) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not supported");
                return;
            }
            return;
        }
        try {
            parseXML();
        } catch (IOException e) {
            Slog.d(TAG, "IOException fail to parseXML, " + e);
        } catch (XmlPullParserException e2) {
            Slog.d(TAG, "XmlPullParserException fail to parseXML, " + e2);
        } catch (Exception e3) {
            Slog.d(TAG, "fail to parseXML, " + e3);
        }
    }

    public static boolean isSupported() {
        if (sDebug) {
            Slog.d(TAG, "isSupported = " + sIsAalSupported);
        }
        return sIsAalSupported;
    }

    public static AalUtils getInstance() {
        if (sInstance == null) {
            sInstance = new AalUtils();
        }
        return sInstance;
    }

    public void setAalMode(int mode) {
        if (!sIsAalSupported) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not supported");
                return;
            }
            return;
        }
        setAalModeInternal(mode);
    }

    public void setEnabled(boolean enabled) {
        if (!sIsAalSupported) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not supported");
                return;
            }
            return;
        }
        setEnabledInternal(enabled);
    }

    synchronized String setAalModeInternal(int mode) {
        String msg;
        if (!sEnabled) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not enabled");
            }
            return "AAL is not enabled";
        }
        if (mode >= 0 && mode < 3) {
            this.mAalMode = mode;
            msg = "setAalModeInternal " + this.mAalMode + "(" + modeToString(this.mAalMode) + ")";
        } else {
            msg = "unknown mode " + mode;
        }
        if (sDebug) {
            Slog.d(TAG, msg);
        }
        return msg;
    }

    public synchronized void setEnabledInternal(boolean enabled) {
        sEnabled = enabled;
        if (!enabled) {
            setDefaultSmartBacklightInternal("disabled");
            SystemProperties.set("persist.vendor.sys.mtk_app_aal_support", "0");
        } else {
            SystemProperties.set("persist.vendor.sys.mtk_app_aal_support", Config.USER_CONFIG_DEFAULT_TYPE);
        }
        if (sDebug) {
            Slog.d(TAG, "setEnabledInternal(" + sEnabled + ")");
        }
    }

    public synchronized void setSmartBacklightInternal(ComponentName name) {
        setSmartBacklightInternal(name, this.mAalMode);
    }

    public synchronized void setSmartBacklightInternal(ComponentName name, int mode) {
        if (!sEnabled) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not enabled");
            }
        } else if (mode < 0 || mode >= 3) {
            if (sDebug) {
                Slog.d(TAG, "Unknown mode: " + mode);
            }
        } else {
            if (this.mCurrentConfig == null) {
                if (sDebug) {
                    Slog.d(TAG, "mCurrentConfig == null");
                }
                this.mCurrentConfig = new AalConfig(null, AAL_DEFAULT_LEVEL);
            }
            AalIndex index = new AalIndex(mode, name.flattenToShortString());
            AalConfig config = getAalConfig(index);
            if (-1 == config.mLevel) {
                index = new AalIndex(mode, name.getPackageName());
                config = getAalConfig(index);
                if (-1 == config.mLevel) {
                    config.mLevel = AAL_DEFAULT_LEVEL;
                }
            }
            int validLevel = ensureBacklightLevel(config.mLevel);
            if (sDebug) {
                Slog.d(TAG, "setSmartBacklight current level: " + this.mCurrentConfig.mLevel + " for " + index);
            }
            if (this.mCurrentConfig.mLevel != validLevel) {
                Slog.d(TAG, "setSmartBacklightStrength(" + validLevel + ") for " + index);
                this.mCurrentConfig.mLevel = validLevel;
                this.mCurrentConfig.mName = index.getIndexName();
                setSmartBacklightStrength(validLevel);
            }
        }
    }

    public synchronized void setDefaultSmartBacklightInternal(String reason) {
        if (!sEnabled) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not enabled");
            }
            return;
        }
        AalConfig aalConfig = this.mCurrentConfig;
        if (aalConfig != null && aalConfig.mLevel != AAL_DEFAULT_LEVEL) {
            Slog.d(TAG, "setSmartBacklightStrength(128) " + reason);
            this.mCurrentConfig.mLevel = AAL_DEFAULT_LEVEL;
            this.mCurrentConfig.mName = null;
            setSmartBacklightStrength(AAL_DEFAULT_LEVEL);
        }
    }

    public void onAfterActivityResumed(String packageName, String activityName) {
        setSmartBacklightInternal(new ComponentName(packageName, activityName));
    }

    public void onUpdateSleep(boolean wasSleeping, boolean isSleepingAfterUpdate) {
        if (sDebug) {
            Slog.d(TAG, "onUpdateSleep before=" + wasSleeping + " after=" + isSleepingAfterUpdate);
        }
        if (wasSleeping != isSleepingAfterUpdate && isSleepingAfterUpdate) {
            setDefaultSmartBacklightInternal("for sleep");
        }
    }

    private int ensureBacklightLevel(int level) {
        if (level < 0) {
            if (sDebug) {
                Slog.e(TAG, "Invalid AAL backlight level: " + level);
                return 0;
            }
            return 0;
        } else if (level > AAL_MAX_LEVEL) {
            if (sDebug) {
                Slog.e(TAG, "Invalid AAL backlight level: " + level);
            }
            return AAL_MAX_LEVEL;
        } else {
            return level;
        }
    }

    private AalConfig getAalConfig(AalIndex index) {
        int level = -1;
        if (this.mConfig.containsKey(index)) {
            level = this.mConfig.get(index).intValue();
        } else if (sDebug) {
            Slog.d(TAG, "No config for " + index);
        }
        return new AalConfig(index.getIndexName(), level);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String modeToString(int mode) {
        switch (mode) {
            case 0:
                return "AAL_MODE_PERFORMANCE";
            case 1:
                return "AAL_MODE_BALANCE";
            case 2:
                return "AAL_MODE_LOWPOWER";
            default:
                return "Unknown mode: " + mode;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AalConfig {
        public int mLevel;
        public String mName;

        public AalConfig(String name, int level) {
            this.mName = null;
            this.mLevel = -1;
            this.mName = name;
            this.mLevel = level;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AalIndex {
        private int mMode;
        private String mName;

        AalIndex(int mode, String name) {
            set(mode, name);
        }

        private void set(int mode, String name) {
            this.mMode = mode;
            this.mName = name;
        }

        public int getMode() {
            return this.mMode;
        }

        public String getIndexName() {
            return this.mName;
        }

        public String toString() {
            return "(" + this.mMode + ": " + AalUtils.this.modeToString(this.mMode) + ", " + this.mName + ")";
        }

        public boolean equals(Object obj) {
            String str;
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof AalIndex)) {
                return false;
            }
            AalIndex index = (AalIndex) obj;
            String str2 = this.mName;
            if (str2 == null && index.mName == null) {
                if (this.mMode != index.mMode) {
                    return false;
                }
                return true;
            } else if (str2 == null || (str = index.mName) == null || this.mMode != index.mMode || !str2.equals(str)) {
                return false;
            } else {
                return true;
            }
        }

        public int hashCode() {
            String hashString = Integer.toString(this.mMode) + ":";
            if (this.mName != null) {
                hashString = hashString + Integer.toString(this.mName.hashCode());
            }
            return hashString.hashCode();
        }
    }

    public int dump(PrintWriter pw, String[] args, int opti) {
        if (sIsAalSupported) {
            if (args.length <= 1) {
                pw.println(dumpDebugUsageInternal());
                return opti;
            }
            String option = args[opti];
            if ("dump".equals(option) && args.length == 2) {
                pw.println(dumpInternal());
                return opti;
            } else if ("debugon".equals(option) && args.length == 2) {
                pw.println(setDebugInternal(true));
                pw.println("App-based AAL debug on");
                return opti;
            } else if ("debugoff".equals(option) && args.length == 2) {
                pw.println(setDebugInternal(false));
                pw.println("App-based AAL debug off");
                return opti;
            } else if ("on".equals(option) && args.length == 2) {
                setEnabledInternal(true);
                pw.println("App-based AAL on");
                return opti;
            } else if ("off".equals(option) && args.length == 2) {
                setEnabledInternal(false);
                pw.println("App-based AAL off");
                return opti;
            } else if ("mode".equals(option) && args.length == 3) {
                int opti2 = opti + 1;
                int mode = Integer.parseInt(args[opti2]);
                pw.println(setAalModeInternal(mode));
                pw.println("Done");
                return opti2;
            } else if ("set".equals(option) && (args.length == 4 || args.length == 5)) {
                int opti3 = opti + 1;
                String pkgName = args[opti3];
                int opti4 = opti3 + 1;
                int value = Integer.parseInt(args[opti4]);
                if (args.length == 4) {
                    pw.println(setSmartBacklightTableInternal(pkgName, value));
                } else {
                    opti4++;
                    int mode2 = Integer.parseInt(args[opti4]);
                    pw.println(setSmartBacklightTableInternal(pkgName, value, mode2));
                }
                pw.println("Done");
                return opti4;
            } else {
                pw.println(dumpDebugUsageInternal());
                return opti;
            }
        }
        pw.println("Not support App-based AAL");
        return opti;
    }

    private String dumpDebugUsageInternal() {
        return "\nUsage:\n1. App-based AAL help:\n    adb shell dumpsys activity aal\n2. Dump App-based AAL settings:\n    adb shell dumpsys activity aal dump\n1. App-based AAL debug on:\n    adb shell dumpsys activity aal debugon\n1. App-based AAL debug off:\n    adb shell dumpsys activity aal debugoff\n3. Enable App-based AAL:\n    adb shell dumpsys activity aal on\n4. Disable App-based AAL:\n    adb shell dumpsys activity aal off\n5. Set App-based AAL mode:\n    adb shell dumpsys activity aal mode <mode>\n6. Set App-based AAL config for current mode:\n    adb shell dumpsys activity aal set <component> <value>\n7. Set App-based AAL config for the mode:\n    adb shell dumpsys activity aal set <component> <value> <mode>\n";
    }

    private synchronized String dumpInternal() {
        StringBuilder sb;
        sb = new StringBuilder();
        sb.append("\nApp-based AAL Mode: " + this.mAalMode + "(" + modeToString(this.mAalMode) + "), Supported: " + sIsAalSupported + ", Enabled: " + sEnabled + ", Debug: " + sDebug + "\n");
        int i = 1;
        for (AalIndex index : this.mConfig.keySet()) {
            String level = Integer.toString(this.mConfig.get(index).intValue());
            sb.append("\n" + i + ". " + index + " - " + level);
            i++;
        }
        if (i == 1) {
            sb.append("\nThere is no App-based AAL configuration.\n");
            sb.append(dumpDebugUsageInternal());
        }
        if (sDebug) {
            Slog.d(TAG, "dump config: " + sb.toString());
        }
        return sb.toString();
    }

    private synchronized String setDebugInternal(boolean debug) {
        sDebug = debug;
        return "Set Debug: " + debug;
    }

    private synchronized String setSmartBacklightTableInternal(String name, int value) {
        return setSmartBacklightTableInternal(name, value, this.mAalMode);
    }

    private synchronized String setSmartBacklightTableInternal(String name, int value, int mode) {
        if (!sEnabled) {
            if (sDebug) {
                Slog.d(TAG, "AAL is not enabled");
            }
            return "AAL is not enabled";
        } else if (mode < 0 || mode >= 3) {
            String msg = "Unknown mode: " + mode;
            if (sDebug) {
                Slog.d(TAG, msg);
            }
            return msg;
        } else {
            AalIndex index = new AalIndex(mode, name);
            if (sDebug) {
                Slog.d(TAG, "setSmartBacklightTable(" + value + ") for " + index);
            }
            this.mConfig.put(index, Integer.valueOf(value));
            return "Set(" + value + ") for " + index;
        }
    }

    private void parseXML() throws XmlPullParserException, IOException {
        FileReader fileReader;
        if (!new File(sAalConfigXMLPath).exists()) {
            if (sDebug) {
                Slog.d(TAG, "parseXML file not exists: " + sAalConfigXMLPath);
                return;
            }
            return;
        }
        if (new File(sAalConfigXMLProjectPath).exists()) {
            Slog.d(TAG, "parseXML file is : " + sAalConfigXMLProjectPath);
            fileReader = new FileReader(sAalConfigXMLProjectPath);
        } else {
            fileReader = new FileReader(sAalConfigXMLPath);
        }
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(fileReader);
        for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
            switch (eventType) {
                case 2:
                    if (parser.getName().equals("config")) {
                        int mode = Integer.parseInt(parser.getAttributeValue(0));
                        String componentName = parser.getAttributeValue(1);
                        int backlight = Integer.parseInt(parser.getAttributeValue(2));
                        this.mConfig.put(new AalIndex(mode, componentName), Integer.valueOf(backlight));
                        break;
                    } else {
                        break;
                    }
            }
        }
        fileReader.close();
    }
}
