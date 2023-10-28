package com.transsion.xmlprotect;

import android.util.Log;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class VerifyFacotry {
    private static final String CLASS_NAME_VERIFACOTRY_FACTORY_IMPL = "com.transsion.xmlprotect.impl.VerifyFacotryImpl";
    private static final String TAG = "VerifyFacory";
    public static PathClassLoader sClassLoader;
    private static final VerifyFacotry sVerifyFacotry;

    static {
        VerifyFacotry verifyFacotry = null;
        try {
            PathClassLoader pathClassLoader = new PathClassLoader("/system/framework/transsion-services.jar", VerifyFacotry.class.getClassLoader());
            sClassLoader = pathClassLoader;
            Class clazz = Class.forName(CLASS_NAME_VERIFACOTRY_FACTORY_IMPL, false, pathClassLoader);
            Constructor constructor = clazz.getConstructor(new Class[0]);
            verifyFacotry = (VerifyFacotry) constructor.newInstance(new Object[0]);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "[static] ClassNotFoundException", e);
        } catch (IllegalAccessException e2) {
            Log.e(TAG, "[static] InstantiationException", e2);
        } catch (InstantiationException e3) {
            Log.e(TAG, "[static] InstantiationException", e3);
        } catch (Exception e4) {
            Log.e(TAG, "[static] Exception ", e4);
        }
        VerifyFacotry verifyFacotry2 = verifyFacotry != null ? verifyFacotry : new VerifyFacotry();
        sVerifyFacotry = verifyFacotry2;
        Log.i(TAG, "[static] sVerifyFacotry = " + verifyFacotry2);
    }

    public static final VerifyFacotry getInstance() {
        return sVerifyFacotry;
    }

    public VerifyExt getVerifyExt() {
        return new VerifyExt();
    }
}
