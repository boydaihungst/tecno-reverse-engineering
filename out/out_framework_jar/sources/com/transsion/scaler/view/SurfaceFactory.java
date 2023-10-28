package com.transsion.scaler.view;

import android.util.Log;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class SurfaceFactory {
    private static final String TAG = "SurfaceFactory";
    private static Object lock = new Object();
    private static SurfaceFactory sSurfaceFactory;

    public static SurfaceFactory getInstance() {
        if (sSurfaceFactory == null) {
            synchronized (lock) {
                if (sSurfaceFactory == null) {
                    try {
                        PathClassLoader classLoader = new PathClassLoader("/system/framework/transsion_scaler.jar", SurfaceFactory.class.getClassLoader());
                        Class<?> clazz = Class.forName("com.transsion.rt.SurfaceFactoryImpl", false, classLoader);
                        Constructor constructor = clazz.getConstructor(new Class[0]);
                        sSurfaceFactory = (SurfaceFactory) constructor.newInstance(new Object[0]);
                    } catch (Exception e) {
                        Log.e(TAG, "getInstance: " + e.toString());
                        sSurfaceFactory = new SurfaceFactory();
                    }
                    Log.i(TAG, "[static] sSurfaceFactory = " + sSurfaceFactory);
                }
            }
        }
        return sSurfaceFactory;
    }

    public SurfaceExt getSurfaceExt() {
        return new SurfaceExt();
    }
}
