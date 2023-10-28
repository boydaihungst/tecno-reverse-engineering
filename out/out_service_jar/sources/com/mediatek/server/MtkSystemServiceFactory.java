package com.mediatek.server;

import android.util.Slog;
import com.android.server.power.ShutdownThread;
import com.mediatek.server.am.AmsExt;
import com.mediatek.server.anr.AnrManager;
import com.mediatek.server.audio.AudioServiceExt;
import com.mediatek.server.pm.PmsExt;
import com.mediatek.server.powerhal.PowerHalManager;
import com.mediatek.server.ppl.MtkPplManager;
import com.mediatek.server.wm.WindowManagerDebugger;
import com.mediatek.server.wm.WmsExt;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes2.dex */
public class MtkSystemServiceFactory {
    private static Object lock = new Object();
    private static MtkSystemServiceFactory sInstance;

    public static MtkSystemServiceFactory getInstance() {
        if (sInstance == null) {
            try {
                PathClassLoader classLoader = MtkSystemServer.sClassLoader;
                Class<?> clazz = Class.forName("com.mediatek.server.MtkSystemServiceFactoryImpl", false, classLoader);
                Constructor constructor = clazz.getConstructor(new Class[0]);
                sInstance = (MtkSystemServiceFactory) constructor.newInstance(new Object[0]);
            } catch (Exception e) {
                Slog.e("MtkSystemServiceFactory", "getInstance: " + e.toString());
                sInstance = new MtkSystemServiceFactory();
            }
        }
        return sInstance;
    }

    public AnrManager makeAnrManager() {
        return new AnrManager();
    }

    public PowerHalManager makePowerHalManager() {
        return new PowerHalManager();
    }

    public ShutdownThread makeMtkShutdownThread() {
        return new ShutdownThread();
    }

    public MtkPplManager makeMtkPplManager() {
        return new MtkPplManager();
    }

    public AmsExt makeAmsExt() {
        return new AmsExt();
    }

    public AudioServiceExt makeAudioServiceExt() {
        return new AudioServiceExt();
    }

    public WmsExt makeWmsExt() {
        return new WmsExt();
    }

    public PmsExt makePmsExt() {
        return new PmsExt();
    }

    public WindowManagerDebugger makeWindowManagerDebugger() {
        return new WindowManagerDebugger();
    }
}
