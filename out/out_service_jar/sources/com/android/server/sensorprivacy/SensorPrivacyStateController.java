package com.android.server.sensorprivacy;

import android.os.Handler;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.sensorprivacy.SensorPrivacyStateController;
import java.util.function.BiConsumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class SensorPrivacyStateController {
    private static SensorPrivacyStateController sInstance;
    AllSensorStateController mAllSensorStateController = AllSensorStateController.getInstance();
    private final Object mLock = new Object();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface AllSensorPrivacyListener {
        void onAllSensorPrivacyChanged(boolean z);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface SensorPrivacyListener {
        void onSensorPrivacyChanged(int i, int i2, int i3, SensorState sensorState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface SensorPrivacyStateConsumer {
        void accept(int i, int i2, int i3, SensorState sensorState);
    }

    /* loaded from: classes2.dex */
    interface SetStateResultCallback {
        void callback(boolean z);
    }

    abstract void dumpLocked(DualDumpOutputStream dualDumpOutputStream);

    abstract void forEachStateLocked(SensorPrivacyStateConsumer sensorPrivacyStateConsumer);

    abstract SensorState getStateLocked(int i, int i2, int i3);

    abstract void resetForTestingImpl();

    abstract void schedulePersistLocked();

    abstract void setSensorPrivacyListenerLocked(Handler handler, SensorPrivacyListener sensorPrivacyListener);

    abstract void setStateLocked(int i, int i2, int i3, boolean z, Handler handler, SetStateResultCallback setStateResultCallback);

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SensorPrivacyStateController getInstance() {
        if (sInstance == null) {
            sInstance = SensorPrivacyStateControllerImpl.getInstance();
        }
        return sInstance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SensorState getState(int toggleType, int userId, int sensor) {
        SensorState stateLocked;
        synchronized (this.mLock) {
            stateLocked = getStateLocked(toggleType, userId, sensor);
        }
        return stateLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setState(int toggleType, int userId, int sensor, boolean enabled, Handler callbackHandler, SetStateResultCallback callback) {
        synchronized (this.mLock) {
            setStateLocked(toggleType, userId, sensor, enabled, callbackHandler, callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSensorPrivacyListener(Handler handler, SensorPrivacyListener listener) {
        synchronized (this.mLock) {
            setSensorPrivacyListenerLocked(handler, listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getAllSensorState() {
        boolean allSensorStateLocked;
        synchronized (this.mLock) {
            allSensorStateLocked = this.mAllSensorStateController.getAllSensorStateLocked();
        }
        return allSensorStateLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAllSensorState(boolean enable) {
        synchronized (this.mLock) {
            this.mAllSensorStateController.setAllSensorStateLocked(enable);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAllSensorPrivacyListener(Handler handler, AllSensorPrivacyListener listener) {
        synchronized (this.mLock) {
            this.mAllSensorStateController.setAllSensorPrivacyListenerLocked(handler, listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void persistAll() {
        synchronized (this.mLock) {
            this.mAllSensorStateController.schedulePersistLocked();
            schedulePersistLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachState(SensorPrivacyStateConsumer consumer) {
        synchronized (this.mLock) {
            forEachStateLocked(consumer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(DualDumpOutputStream dumpStream) {
        synchronized (this.mLock) {
            this.mAllSensorStateController.dumpLocked(dumpStream);
            dumpLocked(dumpStream);
        }
        dumpStream.flush();
    }

    public void atomic(Runnable r) {
        synchronized (this.mLock) {
            r.run();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void sendSetStateCallback(Handler callbackHandler, SetStateResultCallback callback, boolean success) {
        callbackHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.sensorprivacy.SensorPrivacyStateController$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((SensorPrivacyStateController.SetStateResultCallback) obj).callback(((Boolean) obj2).booleanValue());
            }
        }, callback, Boolean.valueOf(success)));
    }

    void resetForTesting() {
        this.mAllSensorStateController.resetForTesting();
        resetForTestingImpl();
        sInstance = null;
    }
}
