package com.android.server.biometrics.sensors;

import android.hardware.fingerprint.ISidefpsController;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.hardware.fingerprint.IUdfpsOverlayControllerCallback;
import android.os.RemoteException;
import android.util.Slog;
import java.util.Optional;
/* loaded from: classes.dex */
public final class SensorOverlays {
    private static final String TAG = "SensorOverlays";
    private final Optional<ISidefpsController> mSidefpsController;
    private final Optional<IUdfpsOverlayController> mUdfpsOverlayController;

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface OverlayControllerConsumer<T> {
        void accept(T t) throws RemoteException;
    }

    public SensorOverlays(IUdfpsOverlayController udfpsOverlayController, ISidefpsController sidefpsController) {
        this.mUdfpsOverlayController = Optional.ofNullable(udfpsOverlayController);
        this.mSidefpsController = Optional.ofNullable(sidefpsController);
    }

    public void show(int sensorId, int reason, final AcquisitionClient<?> client) {
        if (this.mSidefpsController.isPresent()) {
            try {
                this.mSidefpsController.get().show(sensorId, reason);
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception when showing the side-fps overlay", e);
            }
        }
        if (this.mUdfpsOverlayController.isPresent()) {
            try {
                this.mUdfpsOverlayController.get().showUdfpsOverlay(client.getRequestId(), sensorId, reason, new IUdfpsOverlayControllerCallback.Stub() { // from class: com.android.server.biometrics.sensors.SensorOverlays.1
                    public void onUserCanceled() {
                        client.onUserCanceled();
                    }
                });
            } catch (RemoteException e2) {
                Slog.e(TAG, "Remote exception when showing the UDFPS overlay", e2);
            }
        }
    }

    public void hide(int sensorId) {
        if (this.mSidefpsController.isPresent()) {
            try {
                this.mSidefpsController.get().hide(sensorId);
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception when hiding the side-fps overlay", e);
            }
        }
        if (this.mUdfpsOverlayController.isPresent()) {
            try {
                this.mUdfpsOverlayController.get().hideUdfpsOverlay(sensorId);
            } catch (RemoteException e2) {
                Slog.e(TAG, "Remote exception when hiding the UDFPS overlay", e2);
            }
        }
    }

    public void ifUdfps(OverlayControllerConsumer<IUdfpsOverlayController> consumer) {
        if (this.mUdfpsOverlayController.isPresent()) {
            try {
                consumer.accept(this.mUdfpsOverlayController.get());
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception using overlay controller", e);
            }
        }
    }
}
