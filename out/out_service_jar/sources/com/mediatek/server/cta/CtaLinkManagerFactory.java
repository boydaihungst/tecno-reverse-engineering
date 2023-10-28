package com.mediatek.server.cta;

import android.util.Log;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes2.dex */
public class CtaLinkManagerFactory {
    private static final String TAG = "CtaManagerFactory";
    public static PathClassLoader sClassLoader;
    private static CtaLinkManagerFactory sInstance = null;
    protected static CtaLinkManager sCtaLinkManager = null;

    public static CtaLinkManagerFactory getInstance() {
        CtaLinkManagerFactory ctaLinkManagerFactory;
        CtaLinkManagerFactory ctaLinkManagerFactory2 = sInstance;
        if (ctaLinkManagerFactory2 == null) {
            try {
                try {
                    PathClassLoader pathClassLoader = new PathClassLoader("/system/framework/mediatek-cta.jar", CtaLinkManagerFactory.class.getClassLoader());
                    sClassLoader = pathClassLoader;
                    Class<?> clazz = Class.forName("com.mediatek.cta.CtaLinkManagerFactoryImpl", false, pathClassLoader);
                    Constructor constructorFunc = clazz.getConstructor(new Class[0]);
                    CtaLinkManagerFactory ctaLinkManagerFactory3 = (CtaLinkManagerFactory) constructorFunc.newInstance(new Object[0]);
                    sInstance = ctaLinkManagerFactory3;
                    if (ctaLinkManagerFactory3 == null) {
                        sInstance = new CtaLinkManagerFactory();
                    }
                    return sInstance;
                } catch (Exception e) {
                    Log.w(TAG, "CtaLinkManagerFactoryImpl not found");
                    if (sInstance == null) {
                        ctaLinkManagerFactory = new CtaLinkManagerFactory();
                        sInstance = ctaLinkManagerFactory;
                    }
                    return sInstance;
                }
            } catch (Throwable th) {
                if (sInstance == null) {
                    ctaLinkManagerFactory = new CtaLinkManagerFactory();
                    sInstance = ctaLinkManagerFactory;
                }
                return sInstance;
            }
        }
        return ctaLinkManagerFactory2;
    }

    public CtaLinkManager makeCtaLinkManager() {
        if (sCtaLinkManager == null) {
            sCtaLinkManager = new CtaLinkManager();
        }
        return sCtaLinkManager;
    }
}
