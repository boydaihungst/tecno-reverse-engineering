package com.transsion.hubcore.server.tranhbm;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemProperties;
import com.android.server.display.HysteresisLevels;
import com.transsion.hubcore.server.tranhbm.ITranHBMManager;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranHBMManager {
    public static final boolean TRAN_HIGH_BRIGHTNESS_MODE_SUPPORT = "1".equals(SystemProperties.get("ro.tran_high_brightness_mode.support"));
    public static final String IMPL_CLASS_NAME = "com.transsion.hubcore.server.tranhbm.TranHBMManagerImpl";
    public static final TranClassInfo<ITranHBMManager> classInfo = new TranClassInfo<>(IMPL_CLASS_NAME, ITranHBMManager.class, new Supplier() { // from class: com.transsion.hubcore.server.tranhbm.ITranHBMManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranHBMManager.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranHBMManager {
    }

    static ITranHBMManager Instance() {
        return (ITranHBMManager) classInfo.getImpl();
    }

    default void init(int displayId, Context context, Handler handler) {
    }

    default void makeTranHbmDisplayPowerState(int displayId, int displayState) {
    }

    default void makeTranAutomaticBrightnessController(int displayId, SensorManager sensorManager, int lightSensorWarmUpTime, float dozeScaleFactor, int lightSensorRate, int initialLightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, boolean resetAmbientLuxAfterWarmUpConfig, HysteresisLevels ambientBrightnessThresholds, HysteresisLevels screenBrightnessThresholds) {
    }

    default void setScreenState(int displayId, int state) {
    }

    default void onSystemBrightnessChanged(int displayId, float brightness, boolean animate, float duration) {
    }

    default boolean allowHbmMode() {
        return false;
    }

    default boolean allowHbmMode(int displayId) {
        return false;
    }

    default int brightnessFloatTo512Int(float brightness) {
        return -1;
    }

    default void hookBatteryTemperatureTenthsCelsius(int batteryTemperature) {
    }

    default void destroy(int displayId) {
    }
}
