package com.mediatek.powerhalmgr;

import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class PowerHalMgrFactory {
    private static PowerHalMgrFactory sInstance;
    private static Object lock = new Object();
    protected static PowerHalMgr sPowerHalMgr = null;

    public static PowerHalMgrFactory getInstance() {
        PowerHalMgrFactory powerHalMgrFactory;
        synchronized (lock) {
            if (sInstance == null) {
                try {
                    Class<?> clazz = Class.forName("com.mediatek.powerhalmgr.PowerHalMgrFactoryImpl", false, null);
                    Constructor constructor = clazz.getConstructor(new Class[0]);
                    sInstance = (PowerHalMgrFactory) constructor.newInstance(new Object[0]);
                } catch (Exception e) {
                    sInstance = new PowerHalMgrFactory();
                }
            }
            powerHalMgrFactory = sInstance;
        }
        return powerHalMgrFactory;
    }

    public PowerHalMgr makePowerHalMgr() {
        PowerHalMgr powerHalMgr = sPowerHalMgr;
        if (powerHalMgr == null) {
            PowerHalMgr powerHalMgr2 = new PowerHalMgr();
            sPowerHalMgr = powerHalMgr2;
            return powerHalMgr2;
        }
        return powerHalMgr;
    }
}
