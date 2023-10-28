package com.mediatek.cta;

import android.util.Log;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class CtaManagerFactory {
    private static final String TAG = "CtaManagerFactory";
    public static PathClassLoader sClassLoader;
    private static CtaManagerFactory sInstance = null;
    protected static CtaManager sCtaManager = null;

    public static CtaManagerFactory getInstance() {
        CtaManagerFactory ctaManagerFactory;
        CtaManagerFactory ctaManagerFactory2;
        CtaManagerFactory ctaManagerFactory3 = sInstance;
        if (ctaManagerFactory3 == null) {
            try {
                try {
                    PathClassLoader pathClassLoader = new PathClassLoader("/system/framework/mediatek-cta.jar", CtaManagerFactory.class.getClassLoader());
                    sClassLoader = pathClassLoader;
                    Class<?> clazz = Class.forName("com.mediatek.cta.CtaManagerFactoryImpl", false, pathClassLoader);
                    Constructor constructorFunc = clazz.getConstructor(new Class[0]);
                    ctaManagerFactory2 = (CtaManagerFactory) constructorFunc.newInstance(new Object[0]);
                    sInstance = ctaManagerFactory2;
                } catch (Exception e) {
                    Log.w(TAG, "CtaManagerFactoryImpl not found");
                    if (sInstance == null) {
                        ctaManagerFactory = new CtaManagerFactory();
                    }
                }
            } catch (Throwable th) {
                if (sInstance == null) {
                    ctaManagerFactory = new CtaManagerFactory();
                }
            }
            if (ctaManagerFactory2 == null) {
                ctaManagerFactory = new CtaManagerFactory();
                sInstance = ctaManagerFactory;
            }
            return sInstance;
        }
        return ctaManagerFactory3;
    }

    public CtaManager makeCtaManager() {
        if (sCtaManager == null) {
            sCtaManager = new CtaManager();
        }
        return sCtaManager;
    }
}
