package com.android.server.biometrics.sensors.fingerprint;

import android.hardware.fingerprint.IFingerprintClientActiveCallback;
import android.os.RemoteException;
import android.util.Slog;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
/* loaded from: classes.dex */
public class GestureAvailabilityDispatcher {
    private static final String TAG = "GestureAvailabilityTracker";
    private boolean mIsActive;
    private final CopyOnWriteArrayList<IFingerprintClientActiveCallback> mClientActiveCallbacks = new CopyOnWriteArrayList<>();
    private final Map<Integer, Boolean> mActiveSensors = new HashMap();

    public boolean isAnySensorActive() {
        return this.mIsActive;
    }

    public void markSensorActive(int sensorId, boolean active) {
        this.mActiveSensors.put(Integer.valueOf(sensorId), Boolean.valueOf(active));
        boolean wasActive = this.mIsActive;
        boolean isActive = false;
        Iterator<Boolean> it = this.mActiveSensors.values().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Boolean b = it.next();
            if (b.booleanValue()) {
                isActive = true;
                break;
            }
        }
        if (wasActive != isActive) {
            Slog.d(TAG, "Notifying gesture availability, active=" + this.mIsActive);
            this.mIsActive = isActive;
            notifyClientActiveCallbacks(isActive);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerCallback(IFingerprintClientActiveCallback callback) {
        this.mClientActiveCallbacks.add(callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeCallback(IFingerprintClientActiveCallback callback) {
        this.mClientActiveCallbacks.remove(callback);
    }

    private void notifyClientActiveCallbacks(boolean isActive) {
        Iterator<IFingerprintClientActiveCallback> it = this.mClientActiveCallbacks.iterator();
        while (it.hasNext()) {
            IFingerprintClientActiveCallback callback = it.next();
            try {
                callback.onClientActiveChanged(isActive);
            } catch (RemoteException e) {
                this.mClientActiveCallbacks.remove(callback);
            }
        }
    }
}
