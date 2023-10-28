package com.android.server.display.utils;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.text.TextUtils;
import java.util.List;
/* loaded from: classes.dex */
public class SensorUtils {
    public static final int NO_FALLBACK = 0;

    public static Sensor findSensor(SensorManager sensorManager, String sensorType, String sensorName, int fallbackType) {
        boolean isNameSpecified = !TextUtils.isEmpty(sensorName);
        boolean isTypeSpecified = !TextUtils.isEmpty(sensorType);
        if (isNameSpecified || isTypeSpecified) {
            List<Sensor> sensors = sensorManager.getSensorList(-1);
            for (Sensor sensor : sensors) {
                if (!isNameSpecified || sensorName.equals(sensor.getName())) {
                    if (!isTypeSpecified || sensorType.equals(sensor.getStringType())) {
                        return sensor;
                    }
                }
            }
        }
        if (fallbackType != 0) {
            return sensorManager.getDefaultSensor(fallbackType);
        }
        return null;
    }
}
