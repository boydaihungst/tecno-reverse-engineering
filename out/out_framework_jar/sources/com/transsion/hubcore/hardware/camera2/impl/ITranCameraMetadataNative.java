package com.transsion.hubcore.hardware.camera2.impl;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfiguration;
import com.transsion.hubcore.hardware.camera2.impl.ITranCameraMetadataNative;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranCameraMetadataNative {
    public static final TranClassInfo<ITranCameraMetadataNative> classInfo = new TranClassInfo<>("com.transsion.hubcore.hardware.camera2.impl.TranCameraMetadataNativeImpl", ITranCameraMetadataNative.class, new Supplier() { // from class: com.transsion.hubcore.hardware.camera2.impl.ITranCameraMetadataNative$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranCameraMetadataNative.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranCameraMetadataNative {
    }

    @FunctionalInterface
    /* loaded from: classes4.dex */
    public interface ICallback {
        StreamConfiguration[] getBase(CameraCharacteristics.Key<StreamConfiguration[]> key);
    }

    static ITranCameraMetadataNative Instance() {
        return classInfo.getImpl();
    }

    default StreamConfiguration[] getStreamConfigurationMap(String appName, StreamConfiguration[] configurations, ICallback callback) {
        return configurations;
    }
}
