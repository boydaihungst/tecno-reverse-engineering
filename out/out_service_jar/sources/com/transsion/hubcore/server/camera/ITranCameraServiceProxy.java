package com.transsion.hubcore.server.camera;

import android.os.Handler;
import com.transsion.hubcore.server.camera.ITranCameraServiceProxy;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranCameraServiceProxy {
    public static final TranClassInfo<ITranCameraServiceProxy> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.camera.TranCameraServiceProxyImpl", ITranCameraServiceProxy.class, new Supplier() { // from class: com.transsion.hubcore.server.camera.ITranCameraServiceProxy$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranCameraServiceProxy.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranCameraServiceProxy {
    }

    static ITranCameraServiceProxy Instance() {
        return (ITranCameraServiceProxy) classInfo.getImpl();
    }

    default void onCameraOpen(Handler handler, String cameraId) {
    }

    default void onCameraClose(Handler handler, int state, String cameraId) {
    }

    default void onCameraActive(String clientName) {
    }

    default void onCameraInactive(String clientName) {
    }

    default boolean handleMessage(int msgId) {
        return false;
    }
}
