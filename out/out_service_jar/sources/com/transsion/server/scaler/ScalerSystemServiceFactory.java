package com.transsion.server.scaler;

import android.util.Slog;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes2.dex */
public class ScalerSystemServiceFactory {
    private static Object lock = new Object();
    private static ScalerSystemServiceFactory sInstance;
    private WmsExt mWmsExt = new WmsExt();

    public static ScalerSystemServiceFactory getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    try {
                        PathClassLoader classLoader = new PathClassLoader("/system/framework/transsion_scaler.jar", ScalerSystemServiceFactory.class.getClassLoader());
                        Class<?> clazz = Class.forName("com.transsion.rt.ScalerSystemServiceFactoryImpl", false, classLoader);
                        Constructor constructor = clazz.getConstructor(new Class[0]);
                        sInstance = (ScalerSystemServiceFactory) constructor.newInstance(new Object[0]);
                    } catch (Exception e) {
                        Slog.e("ScalerSystemServiceFactory", "getInstance: " + e.toString());
                        sInstance = new ScalerSystemServiceFactory();
                    }
                }
            }
        }
        return sInstance;
    }

    public WmsExt makeWmsExt() {
        return this.mWmsExt;
    }
}
