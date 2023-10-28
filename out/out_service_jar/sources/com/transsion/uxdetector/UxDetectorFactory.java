package com.transsion.uxdetector;

import android.content.Context;
import android.util.Slog;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Method;
/* loaded from: classes2.dex */
public class UxDetectorFactory {
    private static Object lock = new Object();
    private static boolean initialized = false;

    public static void init(Context context) {
        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    try {
                        PathClassLoader classLoader = new PathClassLoader("/system/framework/transsion_uxdetector.jar", UxDetectorFactory.class.getClassLoader());
                        Class<?> clazz = Class.forName("com.transsion.uxdetector.UxDetectorManager", false, classLoader);
                        Method initMethod = clazz.getMethod("init", Context.class);
                        initMethod.invoke(null, context);
                    } catch (Exception e) {
                        Slog.e("UxDetector", "getInstance: " + e.toString());
                    }
                    initialized = true;
                }
            }
        }
    }
}
