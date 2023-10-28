package com.android.server.display.whitebalance;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.TypedValue;
import com.android.server.display.utils.AmbientFilter;
import com.android.server.display.utils.AmbientFilterFactory;
import com.android.server.display.whitebalance.AmbientSensor;
/* loaded from: classes.dex */
public class DisplayWhiteBalanceFactory {
    private static final String BRIGHTNESS_FILTER_TAG = "AmbientBrightnessFilter";
    private static final String COLOR_TEMPERATURE_FILTER_TAG = "AmbientColorTemperatureFilter";

    public static DisplayWhiteBalanceController create(Handler handler, SensorManager sensorManager, Resources resources) {
        AmbientSensor.AmbientBrightnessSensor brightnessSensor = createBrightnessSensor(handler, sensorManager, resources);
        AmbientFilter brightnessFilter = AmbientFilterFactory.createBrightnessFilter(BRIGHTNESS_FILTER_TAG, resources);
        AmbientSensor.AmbientColorTemperatureSensor colorTemperatureSensor = createColorTemperatureSensor(handler, sensorManager, resources);
        AmbientFilter colorTemperatureFilter = AmbientFilterFactory.createColorTemperatureFilter(COLOR_TEMPERATURE_FILTER_TAG, resources);
        DisplayWhiteBalanceThrottler throttler = createThrottler(resources);
        float[] displayWhiteBalanceLowLightAmbientBrightnesses = getFloatArray(resources, 17236050);
        float[] displayWhiteBalanceLowLightAmbientBiases = getFloatArray(resources, 17236049);
        float lowLightAmbientColorTemperature = getFloat(resources, 17105076);
        float[] displayWhiteBalanceHighLightAmbientBrightnesses = getFloatArray(resources, 17236047);
        float[] displayWhiteBalanceHighLightAmbientBiases = getFloatArray(resources, 17236046);
        float highLightAmbientColorTemperature = getFloat(resources, 17105075);
        float[] ambientColorTemperatures = getFloatArray(resources, 17236040);
        float[] displayColorTemperatures = getFloatArray(resources, 17236043);
        float[] strongAmbientColorTemperatures = getFloatArray(resources, 17236051);
        float[] strongDisplayColorTemperatures = getFloatArray(resources, 17236052);
        DisplayWhiteBalanceController controller = new DisplayWhiteBalanceController(brightnessSensor, brightnessFilter, colorTemperatureSensor, colorTemperatureFilter, throttler, displayWhiteBalanceLowLightAmbientBrightnesses, displayWhiteBalanceLowLightAmbientBiases, lowLightAmbientColorTemperature, displayWhiteBalanceHighLightAmbientBrightnesses, displayWhiteBalanceHighLightAmbientBiases, highLightAmbientColorTemperature, ambientColorTemperatures, displayColorTemperatures, strongAmbientColorTemperatures, strongDisplayColorTemperatures);
        brightnessSensor.setCallbacks(controller);
        colorTemperatureSensor.setCallbacks(controller);
        return controller;
    }

    private DisplayWhiteBalanceFactory() {
    }

    public static AmbientSensor.AmbientBrightnessSensor createBrightnessSensor(Handler handler, SensorManager sensorManager, Resources resources) {
        int rate = resources.getInteger(17694808);
        return new AmbientSensor.AmbientBrightnessSensor(handler, sensorManager, rate);
    }

    public static AmbientSensor.AmbientColorTemperatureSensor createColorTemperatureSensor(Handler handler, SensorManager sensorManager, Resources resources) {
        String name = resources.getString(17039960);
        int rate = resources.getInteger(17694813);
        return new AmbientSensor.AmbientColorTemperatureSensor(handler, sensorManager, name, rate);
    }

    private static DisplayWhiteBalanceThrottler createThrottler(Resources resources) {
        int increaseDebounce = resources.getInteger(17694814);
        int decreaseDebounce = resources.getInteger(17694815);
        float[] baseThresholds = getFloatArray(resources, 17236041);
        float[] increaseThresholds = getFloatArray(resources, 17236048);
        float[] decreaseThresholds = getFloatArray(resources, 17236042);
        return new DisplayWhiteBalanceThrottler(increaseDebounce, decreaseDebounce, baseThresholds, increaseThresholds, decreaseThresholds);
    }

    private static float getFloat(Resources resources, int id) {
        TypedValue value = new TypedValue();
        resources.getValue(id, value, true);
        if (value.type != 4) {
            return Float.NaN;
        }
        return value.getFloat();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [179=4] */
    private static float[] getFloatArray(Resources resources, int id) {
        TypedArray array = resources.obtainTypedArray(id);
        try {
            if (array.length() == 0) {
                return null;
            }
            float[] values = new float[array.length()];
            for (int i = 0; i < values.length; i++) {
                values[i] = array.getFloat(i, Float.NaN);
                if (Float.isNaN(values[i])) {
                    return null;
                }
            }
            return values;
        } finally {
            array.recycle();
        }
    }
}
