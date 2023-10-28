package com.mediatek.boostfwk;

import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class BoostFwkFactory {
    private static BoostFwkFactory sInstance;
    protected static BoostFwkManager sBoostFwkManager = null;
    private static Object lock = new Object();

    public static BoostFwkFactory getInstance() {
        if (sInstance == null) {
            synchronized (lock) {
                if (sInstance == null) {
                    try {
                        Class<?> clazz = Class.forName("com.mediatek.boostfwk.BoostFwkFactoryImpl", false, null);
                        Constructor constructor = clazz.getConstructor(new Class[0]);
                        sInstance = (BoostFwkFactory) constructor.newInstance(new Object[0]);
                    } catch (Exception e) {
                        sInstance = new BoostFwkFactory();
                    }
                }
            }
        }
        return sInstance;
    }

    public BoostFwkManager makeBoostFwkManager() {
        if (sBoostFwkManager == null) {
            sBoostFwkManager = new BoostFwkManager();
        }
        return sBoostFwkManager;
    }
}
