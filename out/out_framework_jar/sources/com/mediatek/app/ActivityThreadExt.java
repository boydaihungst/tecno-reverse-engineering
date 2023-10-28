package com.mediatek.app;

import android.app.Activity;
import android.app.ActivityThread;
import android.os.SystemProperties;
import com.mediatek.boostfwk.BoostFwkFactory;
import com.mediatek.boostfwk.scenario.launch.LaunchScenario;
/* loaded from: classes4.dex */
public class ActivityThreadExt {
    public static void enableActivityThreadLog(ActivityThread activityThread) {
        String activitylog = SystemProperties.get("persist.vendor.sys.activitylog", null);
        if (activitylog != null && !activitylog.equals("")) {
            if (activitylog.indexOf(" ") != -1 && activitylog.indexOf(" ") + 1 <= activitylog.length()) {
                String option = activitylog.substring(0, activitylog.indexOf(" "));
                String enable = activitylog.substring(activitylog.indexOf(" ") + 1, activitylog.length());
                boolean isEnable = "on".equals(enable);
                if (option.equals("x")) {
                    enableActivityThreadLog(isEnable, activityThread);
                    return;
                }
                return;
            }
            SystemProperties.set("persist.vendor.sys.activitylog", "");
        }
    }

    public static void enableActivityThreadLog(boolean isEnable, ActivityThread activityThread) {
        ActivityThread.localLOGV = isEnable;
        ActivityThread.DEBUG_MESSAGES = isEnable;
        ActivityThread.DEBUG_BROADCAST = isEnable;
        ActivityThread.DEBUG_RESULTS = isEnable;
        ActivityThread.DEBUG_BACKUP = isEnable;
        ActivityThread.DEBUG_CONFIGURATION = isEnable;
        ActivityThread.DEBUG_SERVICE = isEnable;
        ActivityThread.DEBUG_MEMORY_TRIM = isEnable;
        ActivityThread.DEBUG_PROVIDER = isEnable;
        ActivityThread.DEBUG_ORDER = isEnable;
    }

    public static void hintBoostFWKActivityResumed(Activity activity) {
        if (activity == null) {
            return;
        }
        BoostFwkFactory.getInstance().makeBoostFwkManager().perfHint(new LaunchScenario(3, 3, activity));
    }
}
