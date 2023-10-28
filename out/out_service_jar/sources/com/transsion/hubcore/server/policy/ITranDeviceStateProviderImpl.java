package com.transsion.hubcore.server.policy;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import com.android.server.policy.DeviceStateProviderImpl;
import com.transsion.hubcore.server.policy.ITranDeviceStateProviderImpl;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.Map;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranDeviceStateProviderImpl {
    public static final TranClassInfo<ITranDeviceStateProviderImpl> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.policy.TranDeviceStateProviderImpl", ITranDeviceStateProviderImpl.class, new Supplier() { // from class: com.transsion.hubcore.server.policy.ITranDeviceStateProviderImpl$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDeviceStateProviderImpl.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranDeviceStateProviderImpl {
    }

    static ITranDeviceStateProviderImpl Instance() {
        return (ITranDeviceStateProviderImpl) classInfo.getImpl();
    }

    default void onConstruct(DeviceStateProviderImpl self, ConstructParameters constructParameters) {
    }

    /* loaded from: classes2.dex */
    public interface ConstructParameters {
        default Map<Sensor, SensorEvent> getLatestSensorEventMap() {
            return null;
        }
    }

    default void hookSensorBeforeRegister(Sensor sensor, SensorManager sensorManager) {
    }

    default void onSystemBootedEnd() {
    }

    default void setHallKeyStateUp(boolean isUp) {
    }

    default boolean isSensorValueInValid(SensorEvent event) {
        return false;
    }
}
