package com.android.server.sensorprivacy;

import android.os.Handler;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.sensorprivacy.SensorPrivacyStateController;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SensorPrivacyStateControllerImpl extends SensorPrivacyStateController {
    private static final String SENSOR_PRIVACY_XML_FILE = "sensor_privacy_impl.xml";
    private static SensorPrivacyStateControllerImpl sInstance;
    private SensorPrivacyStateController.SensorPrivacyListener mListener;
    private Handler mListenerHandler;
    private PersistedState mPersistedState = PersistedState.fromFile(SENSOR_PRIVACY_XML_FILE);

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SensorPrivacyStateController getInstance() {
        if (sInstance == null) {
            sInstance = new SensorPrivacyStateControllerImpl();
        }
        return sInstance;
    }

    private SensorPrivacyStateControllerImpl() {
        persistAll();
    }

    @Override // com.android.server.sensorprivacy.SensorPrivacyStateController
    SensorState getStateLocked(int toggleType, int userId, int sensor) {
        SensorState sensorState = this.mPersistedState.getState(toggleType, userId, sensor);
        if (sensorState != null) {
            return new SensorState(sensorState);
        }
        return getDefaultSensorState();
    }

    private static SensorState getDefaultSensorState() {
        return new SensorState(false);
    }

    @Override // com.android.server.sensorprivacy.SensorPrivacyStateController
    void setStateLocked(int toggleType, int userId, int sensor, boolean enabled, Handler callbackHandler, SensorPrivacyStateController.SetStateResultCallback callback) {
        SensorState lastState = this.mPersistedState.getState(toggleType, userId, sensor);
        if (lastState == null) {
            if (!enabled) {
                sendSetStateCallback(callbackHandler, callback, false);
                return;
            } else if (enabled) {
                SensorState sensorState = new SensorState(true);
                this.mPersistedState.setState(toggleType, userId, sensor, sensorState);
                notifyStateChangeLocked(toggleType, userId, sensor, sensorState);
                sendSetStateCallback(callbackHandler, callback, true);
                return;
            }
        }
        if (lastState.setEnabled(enabled)) {
            notifyStateChangeLocked(toggleType, userId, sensor, lastState);
            sendSetStateCallback(callbackHandler, callback, true);
            return;
        }
        sendSetStateCallback(callbackHandler, callback, false);
    }

    private void notifyStateChangeLocked(int toggleType, int userId, int sensor, SensorState sensorState) {
        Handler handler = this.mListenerHandler;
        if (handler != null && this.mListener != null) {
            handler.sendMessage(PooledLambda.obtainMessage(new QuintConsumer() { // from class: com.android.server.sensorprivacy.SensorPrivacyStateControllerImpl$$ExternalSyntheticLambda0
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                    ((SensorPrivacyStateController.SensorPrivacyListener) obj).onSensorPrivacyChanged(((Integer) obj2).intValue(), ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), (SensorState) obj5);
                }
            }, this.mListener, Integer.valueOf(toggleType), Integer.valueOf(userId), Integer.valueOf(sensor), new SensorState(sensorState)));
        }
        schedulePersistLocked();
    }

    @Override // com.android.server.sensorprivacy.SensorPrivacyStateController
    void setSensorPrivacyListenerLocked(Handler handler, SensorPrivacyStateController.SensorPrivacyListener listener) {
        Objects.requireNonNull(handler);
        Objects.requireNonNull(listener);
        if (this.mListener != null) {
            throw new IllegalStateException("Listener is already set");
        }
        this.mListener = listener;
        this.mListenerHandler = handler;
    }

    @Override // com.android.server.sensorprivacy.SensorPrivacyStateController
    void schedulePersistLocked() {
        this.mPersistedState.schedulePersist();
    }

    @Override // com.android.server.sensorprivacy.SensorPrivacyStateController
    void forEachStateLocked(final SensorPrivacyStateController.SensorPrivacyStateConsumer consumer) {
        PersistedState persistedState = this.mPersistedState;
        Objects.requireNonNull(consumer);
        persistedState.forEachKnownState(new QuadConsumer() { // from class: com.android.server.sensorprivacy.SensorPrivacyStateControllerImpl$$ExternalSyntheticLambda1
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                SensorPrivacyStateController.SensorPrivacyStateConsumer.this.accept(((Integer) obj).intValue(), ((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (SensorState) obj4);
            }
        });
    }

    @Override // com.android.server.sensorprivacy.SensorPrivacyStateController
    void resetForTestingImpl() {
        this.mPersistedState.resetForTesting();
        this.mListener = null;
        this.mListenerHandler = null;
        sInstance = null;
    }

    @Override // com.android.server.sensorprivacy.SensorPrivacyStateController
    void dumpLocked(DualDumpOutputStream dumpStream) {
        this.mPersistedState.dump(dumpStream);
    }
}
