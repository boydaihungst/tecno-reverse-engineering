package com.android.server.biometrics.sensors;

import android.content.Context;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Slog;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class AcquisitionClient<T> extends HalClientMonitor<T> implements Interruptable, ErrorConsumer {
    private static final String TAG = "Biometrics/AcquisitionClient";
    private boolean mAlreadyCancelled;
    private final PowerManager mPowerManager;
    private boolean mShouldSendErrorToClient;
    protected final boolean mShouldVibrate;
    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES = VibrationAttributes.createForUsage(50);
    private static final VibrationEffect SUCCESS_VIBRATION_EFFECT = VibrationEffect.get(0);
    private static final VibrationEffect ERROR_VIBRATION_EFFECT = VibrationEffect.get(1);

    protected abstract void stopHalOperation();

    public AcquisitionClient(Context context, Supplier<T> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int userId, String owner, int cookie, int sensorId, boolean shouldVibrate, BiometricLogger logger, BiometricContext biometricContext) {
        super(context, lazyDaemon, token, listener, userId, owner, cookie, sensorId, logger, biometricContext);
        this.mShouldSendErrorToClient = true;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mShouldVibrate = shouldVibrate;
    }

    @Override // com.android.server.biometrics.sensors.HalClientMonitor
    public void unableToStart() {
        try {
            getListener().onError(getSensorId(), getCookie(), 1, 0);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to send error", e);
        }
    }

    @Override // com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int errorCode, int vendorCode) {
        onErrorInternal(errorCode, vendorCode, true);
    }

    public void onUserCanceled() {
        Slog.d(TAG, "onUserCanceled");
        onErrorInternal(10, 0, false);
        stopHalOperation();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onErrorInternal(int errorCode, int vendorCode, boolean finish) {
        Slog.d(TAG, "onErrorInternal code: " + errorCode + ", finish: " + finish);
        if (this.mShouldSendErrorToClient) {
            getLogger().logOnError(getContext(), getOperationContext(), errorCode, vendorCode, getTargetUserId());
            try {
                if (getListener() != null) {
                    this.mShouldSendErrorToClient = false;
                    getListener().onError(getSensorId(), getCookie(), errorCode, vendorCode);
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to invoke sendError", e);
            }
        }
        if (finish) {
            if (this.mCallback == null) {
                Slog.e(TAG, "Callback is null, perhaps the client hasn't been started yet?");
            } else {
                this.mCallback.onClientFinished(this, false);
            }
        }
    }

    @Override // com.android.server.biometrics.sensors.Interruptable
    public void cancel() {
        if (this.mAlreadyCancelled) {
            Slog.w(TAG, "Cancel was already requested");
            return;
        }
        stopHalOperation();
        this.mAlreadyCancelled = true;
    }

    @Override // com.android.server.biometrics.sensors.Interruptable
    public void cancelWithoutStarting(ClientMonitorCallback callback) {
        Slog.d(TAG, "cancelWithoutStarting: " + this);
        try {
            if (getListener() != null) {
                getListener().onError(getSensorId(), getCookie(), 5, 0);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to invoke sendError", e);
        }
        callback.onClientFinished(this, true);
    }

    public void onAcquired(int acquiredInfo, int vendorCode) {
        onAcquiredInternal(acquiredInfo, vendorCode, true);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void onAcquiredInternal(int acquiredInfo, int vendorCode, boolean shouldSend) {
        getLogger().logOnAcquired(getContext(), getOperationContext(), acquiredInfo, vendorCode, getTargetUserId());
        Slog.v(TAG, "Acquired: " + acquiredInfo + " " + vendorCode + ", shouldSend: " + shouldSend);
        if (acquiredInfo == 0) {
            notifyUserActivity();
        }
        try {
            if (getListener() != null && shouldSend) {
                getListener().onAcquired(getSensorId(), acquiredInfo, vendorCode);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to invoke sendAcquired", e);
            this.mCallback.onClientFinished(this, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void notifyUserActivity() {
        long now = SystemClock.uptimeMillis();
        this.mPowerManager.userActivity(now, 2, 0);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void vibrateSuccess() {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Vibrator.class);
        if (vibrator != null && this.mShouldVibrate) {
            vibrator.vibrate(Process.myUid(), getContext().getOpPackageName(), SUCCESS_VIBRATION_EFFECT, getClass().getSimpleName() + "::success", HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void vibrateError() {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Vibrator.class);
        if (vibrator != null && this.mShouldVibrate) {
            vibrator.vibrate(Process.myUid(), getContext().getOpPackageName(), ERROR_VIBRATION_EFFECT, getClass().getSimpleName() + "::error", HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES);
        }
    }
}
