package com.mediatek.res;

import android.util.Log;
import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class ResOptExtFactory {
    private static final String TAG = "ResOptExtFactory";
    private static ResOptExtFactory sInstance = null;
    protected static ResOptExt sResOptExt = null;

    public static ResOptExtFactory getInstance() {
        ResOptExtFactory resOptExtFactory;
        ResOptExtFactory resOptExtFactory2;
        ResOptExtFactory resOptExtFactory3 = sInstance;
        if (resOptExtFactory3 == null) {
            try {
                try {
                    Class<?> clazz = Class.forName("com.mediatek.res.ResOptExtFactoryImpl", false, null);
                    Constructor constructorFunc = clazz.getConstructor(new Class[0]);
                    resOptExtFactory2 = (ResOptExtFactory) constructorFunc.newInstance(new Object[0]);
                    sInstance = resOptExtFactory2;
                } catch (Exception e) {
                    Log.w(TAG, "ResOptExtFactoryImpl not found");
                    if (sInstance == null) {
                        resOptExtFactory = new ResOptExtFactory();
                    }
                }
            } catch (Throwable th) {
                if (sInstance == null) {
                    resOptExtFactory = new ResOptExtFactory();
                }
            }
            if (resOptExtFactory2 == null) {
                resOptExtFactory = new ResOptExtFactory();
                sInstance = resOptExtFactory;
            }
            return sInstance;
        }
        return resOptExtFactory3;
    }

    public ResOptExt makeResOptExt() {
        if (sResOptExt == null) {
            sResOptExt = new ResOptExt();
        }
        return sResOptExt;
    }
}
