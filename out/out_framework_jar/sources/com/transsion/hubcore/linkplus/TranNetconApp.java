package com.transsion.hubcore.linkplus;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes4.dex */
public final class TranNetconApp {
    public static final String TAG = "TranNetconApp";
    private static final List<String> M_SWITCH_APP_LIST = new ArrayList<String>() { // from class: com.transsion.hubcore.linkplus.TranNetconApp.1
        {
            add("com.tencent.tmgp.sgame");
            add("com.tencent.tmgp.cod");
            add("com.vng.codmvn");
            add("com.activision.callofduty.shooter");
            add("com.garena.game.codm");
            add("com.yomobigroup.chat");
            add("com.zhiliaoapp.musically");
            add("com.tencent.ig");
            add("com.tencent.iglite");
            add("com.ss.android.ugc.aweme");
            add("com.ss.android.ugc.aweme.lite");
            add("com.qiyi.video");
            add("com.ss.android.article.lite");
            add("com.ss.android.article.news");
            add("com.dts.freefireth");
        }
    };
    private static final List<String> M_MP_APP_LIST = new ArrayList<String>() { // from class: com.transsion.hubcore.linkplus.TranNetconApp.2
    };

    private TranNetconApp() {
    }

    public static boolean isNetconAppProcess(String processName) {
        if (processName == null) {
            return false;
        }
        for (String name : M_SWITCH_APP_LIST) {
            if (processName.startsWith(name) && !processName.contains("sandbox")) {
                return true;
            }
        }
        for (String name2 : M_MP_APP_LIST) {
            if (processName.startsWith(name2) && !processName.contains("sandbox")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isNetconApp(String packageName) {
        if (packageName == null) {
            return false;
        }
        for (String name : M_SWITCH_APP_LIST) {
            if (packageName.equals(name)) {
                return true;
            }
        }
        for (String name2 : M_MP_APP_LIST) {
            if (packageName.equals(name2)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSwitchSupportedApp(String packageName) {
        if (packageName == null) {
            return false;
        }
        for (String name : M_SWITCH_APP_LIST) {
            if (packageName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMultiPathSupportedApp(String packageName) {
        if (packageName == null) {
            return false;
        }
        for (String name : M_MP_APP_LIST) {
            if (packageName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            return info != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static List<String> getmMpAppList() {
        return M_MP_APP_LIST;
    }

    public static List<String> getmSwitchAppList() {
        return M_SWITCH_APP_LIST;
    }
}
