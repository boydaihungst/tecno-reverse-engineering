package com.transsion.server;

import android.content.Context;
import android.util.Slog;
import com.android.server.lights.LightsManager;
import com.transsion.exceptionReport.LocalExceptionReporter;
import com.transsion.server.am.TranAmsExt;
import com.transsion.server.tranNV.TranNvUtilsExt;
import com.transsion.server.tranbattery.TranBatteryServiceExt;
import com.transsion.server.traneventtrack.TranAIChargingEventTrackExt;
import com.transsion.server.trangbmonitor.TranGraphicBufferMonitor;
import com.transsion.server.tranled.TranLedLightExt;
import dalvik.system.PathClassLoader;
import java.lang.reflect.Constructor;
/* loaded from: classes2.dex */
public class TranSystemServiceFactory {
    private static final String TAG = "TranSystemServiceFactory";
    private static TranSystemServiceFactory sInstance;

    public static TranSystemServiceFactory getInstance() {
        if (sInstance == null) {
            try {
                TranSystemServer.getInstance();
                PathClassLoader classLoader = TranSystemServer.sClassLoader;
                Class<?> clazz = Class.forName("com.transsion.server.TranSystemServiceFactoryImpl", false, classLoader);
                Constructor constructor = clazz.getConstructor(new Class[0]);
                sInstance = (TranSystemServiceFactory) constructor.newInstance(new Object[0]);
            } catch (Exception e) {
                Slog.e(TAG, "getInstance: " + e.toString());
                sInstance = new TranSystemServiceFactory();
            }
        }
        return sInstance;
    }

    public TranAmsExt makeTranAmsExt() {
        Slog.d(TAG, " TranSystemServiceFactory makeTranAmsExt!");
        return new TranAmsExt();
    }

    public LocalExceptionReporter makeLocalExceptionReporter(Context context) {
        return LocalExceptionReporter.getInstance(context);
    }

    public TranNvUtilsExt makeNVUtils() {
        return TranNvUtilsExt.getInstance();
    }

    public TranLedLightExt makeLight(LightsManager lightsManager, Context context) {
        return TranLedLightExt.getInstance(lightsManager, context);
    }

    public TranGraphicBufferMonitor makeTranGraphicBufferMonitor(Context context, Object amsLocked, Object pidsLocked) {
        return TranGraphicBufferMonitor.getInstance(context, amsLocked, pidsLocked);
    }

    public TranAIChargingEventTrackExt makeTranAIChargingEventTrackExt() {
        return TranAIChargingEventTrackExt.getInstance();
    }

    public TranBatteryServiceExt makeTranBatteryServiceExt(Context context) {
        return TranBatteryServiceExt.getInstance(context);
    }
}
