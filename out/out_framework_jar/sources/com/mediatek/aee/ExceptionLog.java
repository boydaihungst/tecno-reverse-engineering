package com.mediatek.aee;

import android.util.Slog;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class ExceptionLog {
    public static PathClassLoader sClassLoader;
    private static ExceptionLog sInstance;

    public static ExceptionLog getInstance() {
        if (sInstance == null) {
            try {
                PathClassLoader pathClassLoader = new PathClassLoader("/system/framework/mediatek-framework.jar", ExceptionLog.class.getClassLoader());
                sClassLoader = pathClassLoader;
                Class<?> clazz = Class.forName("com.mediatek.aee.ExceptionLogImpl", false, pathClassLoader);
                Constructor constructor = clazz.getConstructor(new Class[0]);
                sInstance = (ExceptionLog) constructor.newInstance(new Object[0]);
            } catch (Exception e) {
                Slog.e("ExceptionLog", "getInstance: " + e.toString());
                sInstance = new ExceptionLog();
            }
        }
        return sInstance;
    }

    public void handle(String type, String info, String pid) {
    }

    public void systemreport(byte Type, String Module, String Msg, String Path) {
    }

    public boolean getNativeExceptionPidList(int[] pidList) {
        return false;
    }

    public void switchFtrace(int config) {
    }

    public void WDTMatterJava(long lParam) {
    }

    public long SFMatterJava(long setorget, long lParam) {
        return -1L;
    }
}
