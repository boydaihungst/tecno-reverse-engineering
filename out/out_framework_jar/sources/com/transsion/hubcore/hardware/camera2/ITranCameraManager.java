package com.transsion.hubcore.hardware.camera2;

import android.hardware.camera2.CameraManager;
import android.util.ArrayMap;
import com.transsion.hubcore.hardware.camera2.ITranCameraManager;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranCameraManager {
    public static final TranClassInfo<ITranCameraManager> classInfo = new TranClassInfo<>("com.transsion.hubcore.hardware.camera2.TranCameraManagerImpl", ITranCameraManager.class, new Supplier() { // from class: com.transsion.hubcore.hardware.camera2.ITranCameraManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranCameraManager.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranCameraManager {
    }

    static ITranCameraManager Instance() {
        return classInfo.getImpl();
    }

    default int extractCameraIdListLocked(int idCount) {
        return idCount;
    }

    default boolean extractCameraIdListLocked(String status, int idCount) {
        return false;
    }

    default boolean onStatusChangedLocked(String id, ArrayMap<CameraManager.AvailabilityCallback, Executor> callbackMap) {
        return false;
    }

    default boolean filterCameraIDLocked(int deviceSize, String id) {
        return false;
    }
}
