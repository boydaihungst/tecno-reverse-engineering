package com.android.server.biometrics;

import android.provider.DeviceConfig;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/* loaded from: classes.dex */
public class BiometricStrengthController {
    private static final String KEY_BIOMETRIC_STRENGTHS = "biometric_strengths";
    private static final String TAG = "BiometricStrengthController";
    private DeviceConfig.OnPropertiesChangedListener mDeviceConfigListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.biometrics.BiometricStrengthController$$ExternalSyntheticLambda0
        public final void onPropertiesChanged(DeviceConfig.Properties properties) {
            BiometricStrengthController.this.m2288xbe691fae(properties);
        }
    };
    private final BiometricService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-biometrics-BiometricStrengthController  reason: not valid java name */
    public /* synthetic */ void m2288xbe691fae(DeviceConfig.Properties properties) {
        if (properties.getKeyset().contains(KEY_BIOMETRIC_STRENGTHS)) {
            updateStrengths();
        }
    }

    public BiometricStrengthController(BiometricService service) {
        this.mService = service;
    }

    public void startListening() {
        DeviceConfig.addOnPropertiesChangedListener("biometrics", BackgroundThread.getExecutor(), this.mDeviceConfigListener);
    }

    public void updateStrengths() {
        String newValue = DeviceConfig.getString("biometrics", KEY_BIOMETRIC_STRENGTHS, "null");
        if ("null".equals(newValue) || newValue.isEmpty()) {
            revertStrengths();
        } else {
            updateStrengths(newValue);
        }
    }

    private void updateStrengths(String flags) {
        Map<Integer, Integer> idToStrength = getIdToStrengthMap(flags);
        if (idToStrength == null) {
            return;
        }
        Iterator<BiometricSensor> it = this.mService.mSensors.iterator();
        while (it.hasNext()) {
            BiometricSensor sensor = it.next();
            int id = sensor.id;
            if (idToStrength.containsKey(Integer.valueOf(id))) {
                int newStrength = idToStrength.get(Integer.valueOf(id)).intValue();
                Slog.d(TAG, "updateStrengths: update sensorId=" + id + " to newStrength=" + newStrength);
                sensor.updateStrength(newStrength);
            }
        }
    }

    private void revertStrengths() {
        Iterator<BiometricSensor> it = this.mService.mSensors.iterator();
        while (it.hasNext()) {
            BiometricSensor sensor = it.next();
            Slog.d(TAG, "updateStrengths: revert sensorId=" + sensor.id + " to oemStrength=" + sensor.oemStrength);
            sensor.updateStrength(sensor.oemStrength);
        }
    }

    private static Map<Integer, Integer> getIdToStrengthMap(String flags) {
        String[] split;
        if (flags == null || flags.isEmpty()) {
            Slog.d(TAG, "Flags are null or empty");
            return null;
        }
        Map<Integer, Integer> map = new HashMap<>();
        try {
            for (String item : flags.split(",")) {
                String[] elems = item.split(":");
                int id = Integer.parseInt(elems[0]);
                int strength = Integer.parseInt(elems[1]);
                map.put(Integer.valueOf(id), Integer.valueOf(strength));
            }
            return map;
        } catch (Exception e) {
            Slog.e(TAG, "Can't parse flag: " + flags);
            return null;
        }
    }
}
