package com.mediatek.anr;

import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class AnrAppFactory {
    private static Object lock = new Object();
    private static AnrAppFactory sInstance;

    public static AnrAppFactory getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    try {
                        PathClassLoader classLoader = new PathClassLoader("/system/framework/mediatek-framework.jar", AnrAppFactory.class.getClassLoader());
                        Class<?> clazz = Class.forName("com.mediatek.anr.AnrAppFactoryImpl", false, classLoader);
                        Constructor constructor = clazz.getConstructor(new Class[0]);
                        sInstance = (AnrAppFactory) constructor.newInstance(new Object[0]);
                    } catch (Exception e) {
                        sInstance = new AnrAppFactory();
                    }
                }
            }
        }
        return sInstance;
    }

    public AnrAppManager makeAnrAppManager() {
        return new AnrAppManager();
    }
}
