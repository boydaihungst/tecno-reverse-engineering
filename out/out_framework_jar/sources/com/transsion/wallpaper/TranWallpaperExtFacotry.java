package com.transsion.wallpaper;

import android.util.Slog;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes4.dex */
public class TranWallpaperExtFacotry {
    private static final String CLASS_NAME_WALLPAPER_FACTORY_IMPL = "com.transsion.wallpaper.impl.TranWallpaperExtFacotryImpl";
    private static final String TAG = "TranWallpaperExtFacotry";
    public static PathClassLoader sClassLoader;
    private static final TranWallpaperExtFacotry sTranWallpaperExtFacotry;

    static {
        TranWallpaperExtFacotry tranWallpaperExtFacotry = null;
        try {
            PathClassLoader pathClassLoader = new PathClassLoader("/system/framework/transsion-services.jar", TranWallpaperExtFacotry.class.getClassLoader());
            sClassLoader = pathClassLoader;
            Class clazz = Class.forName(CLASS_NAME_WALLPAPER_FACTORY_IMPL, false, pathClassLoader);
            Constructor constructor = clazz.getConstructor(new Class[0]);
            tranWallpaperExtFacotry = (TranWallpaperExtFacotry) constructor.newInstance(new Object[0]);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        } catch (InstantiationException e3) {
            e3.printStackTrace();
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        TranWallpaperExtFacotry tranWallpaperExtFacotry2 = tranWallpaperExtFacotry != null ? tranWallpaperExtFacotry : new TranWallpaperExtFacotry();
        sTranWallpaperExtFacotry = tranWallpaperExtFacotry2;
        Slog.i(TAG, "TranWallpaperExtFacotry = " + tranWallpaperExtFacotry2);
    }

    public static final TranWallpaperExtFacotry getInstance() {
        return sTranWallpaperExtFacotry;
    }

    public TranWallpaperExt getTranWallpaperExt() {
        return new TranWallpaperExt();
    }
}
