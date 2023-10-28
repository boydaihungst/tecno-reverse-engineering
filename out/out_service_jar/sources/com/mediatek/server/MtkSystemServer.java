package com.mediatek.server;

import android.content.Context;
import android.util.Slog;
import com.android.server.SystemServiceManager;
import com.android.server.utils.TimingsTraceAndSlog;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes2.dex */
public class MtkSystemServer {
    public static PathClassLoader sClassLoader;
    private static MtkSystemServer sInstance;

    public static MtkSystemServer getInstance() {
        if (sInstance == null) {
            try {
                PathClassLoader pathClassLoader = new PathClassLoader("/system/framework/mediatek-services.jar", MtkSystemServer.class.getClassLoader());
                sClassLoader = pathClassLoader;
                Class<?> clazz = Class.forName("com.mediatek.server.MtkSystemServerImpl", false, pathClassLoader);
                Constructor constructor = clazz.getConstructor(new Class[0]);
                sInstance = (MtkSystemServer) constructor.newInstance(new Object[0]);
            } catch (Exception e) {
                Slog.e("MtkSystemServer", "getInstance: " + e.toString());
                sInstance = new MtkSystemServer();
            }
        }
        return sInstance;
    }

    public void setPrameters(TimingsTraceAndSlog btt, SystemServiceManager ssm, Context context) {
    }

    public void startMtkBootstrapServices() {
    }

    public void startMtkCoreServices() {
    }

    public void startMtkOtherServices() {
    }

    public boolean startMtkAlarmManagerService() {
        return false;
    }

    public boolean startMtkStorageManagerService() {
        return false;
    }

    public void addBootEvent(String bootEvent) {
    }
}
