package com.mediatek.dx;

import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class DexOptExtFactory {
    private static Object lock = new Object();
    private static DexOptExtFactory sInstance;

    public static DexOptExtFactory getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    try {
                        PathClassLoader classLoader = new PathClassLoader("/system/framework/mediatek-services.jar", Thread.currentThread().getContextClassLoader());
                        Class<?> clazz = Class.forName("com.mediatek.server.dx.DexOptExtFactoryImpl", false, classLoader);
                        Constructor constructor = clazz.getConstructor(new Class[0]);
                        sInstance = (DexOptExtFactory) constructor.newInstance(new Object[0]);
                    } catch (Exception e) {
                        sInstance = new DexOptExtFactory();
                    }
                }
            }
        }
        return sInstance;
    }

    public DexOptExt makeDexOpExt() {
        return new DexOptExt();
    }
}
