package com.mediatek.view;

import android.util.Log;
/* loaded from: classes4.dex */
public class SurfaceFactory {
    private static final String CLASS_NAME_SURFACE_FACTORY_IMPL = "com.mediatek.view.impl.SurfaceFactoryImpl";
    private static final String TAG = "SurfaceFactory";
    private static final SurfaceFactory sSurfaceFactory;

    static {
        SurfaceFactory surfaceFactory = null;
        try {
            Class clazz = Class.forName(CLASS_NAME_SURFACE_FACTORY_IMPL);
            surfaceFactory = (SurfaceFactory) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "[static] ClassNotFoundException", e);
        } catch (IllegalAccessException e2) {
            Log.e(TAG, "[static] InstantiationException", e2);
        } catch (InstantiationException e3) {
            Log.e(TAG, "[static] InstantiationException", e3);
        }
        SurfaceFactory surfaceFactory2 = surfaceFactory != null ? surfaceFactory : new SurfaceFactory();
        sSurfaceFactory = surfaceFactory2;
        Log.i(TAG, "[static] sSurfaceFactory = " + surfaceFactory2);
    }

    public static final SurfaceFactory getInstance() {
        return sSurfaceFactory;
    }

    public SurfaceExt getSurfaceExt() {
        return new SurfaceExt();
    }
}
