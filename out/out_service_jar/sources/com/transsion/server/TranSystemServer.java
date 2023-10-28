package com.transsion.server;

import android.util.Slog;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes2.dex */
public class TranSystemServer {
    private static final String TAG = "TranSystemServer";
    public static PathClassLoader sClassLoader;
    private static TranSystemServer sInstance;

    public static TranSystemServer getInstance() {
        if (sInstance == null) {
            try {
                PathClassLoader pathClassLoader = new PathClassLoader("system/framework/transsion-services.jar", TranSystemServer.class.getClassLoader());
                sClassLoader = pathClassLoader;
                Class<?> clazz = Class.forName("com.transsion.server.TranSystemServerImpl", false, pathClassLoader);
                Constructor constructor = clazz.getConstructor(new Class[0]);
                sInstance = (TranSystemServer) constructor.newInstance(new Object[0]);
            } catch (Exception e) {
                Slog.e(TAG, "getInstance: " + e.toString());
                sInstance = new TranSystemServer();
            }
        }
        return sInstance;
    }
}
