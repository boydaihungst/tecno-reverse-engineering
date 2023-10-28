package com.mediatek.view;

import android.util.Log;
/* loaded from: classes4.dex */
public class MsyncFactory {
    private static final String CLASS_NAME_MSYNC_FACTORY_IMPL = "com.mediatek.view.impl.MsyncFactoryImpl";
    private static final String TAG = "MsyncFactory";
    private static final MsyncFactory sMsyncFactory;

    static {
        MsyncFactory MsyncFactory = null;
        try {
            Class clazz = Class.forName(CLASS_NAME_MSYNC_FACTORY_IMPL);
            MsyncFactory = (MsyncFactory) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "[static] ClassNotFoundException", e);
        } catch (IllegalAccessException e2) {
            Log.e(TAG, "[static] InstantiationException", e2);
        } catch (InstantiationException e3) {
            Log.e(TAG, "[static] InstantiationException", e3);
        }
        MsyncFactory msyncFactory = MsyncFactory != null ? MsyncFactory : new MsyncFactory();
        sMsyncFactory = msyncFactory;
        Log.i(TAG, "[static] sMsyncFactory = " + msyncFactory);
    }

    public static final MsyncFactory getInstance() {
        return sMsyncFactory;
    }

    public MsyncExt getMsyncExt() {
        return new MsyncExt();
    }
}
