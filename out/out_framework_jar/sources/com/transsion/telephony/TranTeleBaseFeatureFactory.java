package com.transsion.telephony;

import com.android.telephony.Rlog;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class TranTeleBaseFeatureFactory {
    private static TranTeleBaseFeatureFactory sInstance;

    public static TranTeleBaseFeatureFactory getInstance() {
        if (sInstance == null) {
            try {
                PathClassLoader classLoader = new PathClassLoader("/system_ext/framework/tran-telephony-base.jar", TranTeleBaseFeatureFactory.class.getClassLoader());
                Class<?> clazz = Class.forName("com.transsion.telephony.TranTeleBaseFeatureFactoryImpl", false, classLoader);
                Constructor constructor = clazz.getConstructor(new Class[0]);
                sInstance = (TranTeleBaseFeatureFactory) constructor.newInstance(new Object[0]);
            } catch (Exception e) {
                Rlog.e("TranTeleBaseFeatureFactory", "getInstance: " + e.toString());
                sInstance = new TranTeleBaseFeatureFactory();
            }
        }
        return sInstance;
    }

    public TranTeleBaseInterface makeTranCI() {
        return new TranTeleBaseInterface();
    }
}
