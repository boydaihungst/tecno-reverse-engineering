package com.android.server.biometrics.sensors;

import android.hardware.biometrics.IBiometricStateListener;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.Utils;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
/* loaded from: classes.dex */
public class BiometricStateCallback implements ClientMonitorCallback {
    private static final String TAG = "BiometricStateCallback";
    private final CopyOnWriteArrayList<IBiometricStateListener> mBiometricStateListeners = new CopyOnWriteArrayList<>();
    private int mBiometricState = 0;

    public int getBiometricState() {
        return this.mBiometricState;
    }

    @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
    public void onClientStarted(BaseClientMonitor client) {
        int previousBiometricState = this.mBiometricState;
        if (client instanceof AuthenticationClient) {
            AuthenticationClient<?> authClient = (AuthenticationClient) client;
            if (authClient.isKeyguard()) {
                this.mBiometricState = 2;
            } else if (authClient.isBiometricPrompt()) {
                this.mBiometricState = 3;
            } else {
                this.mBiometricState = 4;
            }
        } else if (client instanceof EnrollClient) {
            this.mBiometricState = 1;
        } else {
            Slog.w(TAG, "Other authentication client: " + Utils.getClientName(client));
            this.mBiometricState = 0;
        }
        Slog.d(TAG, "State updated from " + previousBiometricState + " to " + this.mBiometricState + ", client " + client);
        notifyBiometricStateListeners(this.mBiometricState);
    }

    @Override // com.android.server.biometrics.sensors.ClientMonitorCallback
    public void onClientFinished(BaseClientMonitor client, boolean success) {
        this.mBiometricState = 0;
        Slog.d(TAG, "Client finished, state updated to " + this.mBiometricState + ", client " + client);
        if (client instanceof EnrollmentModifier) {
            EnrollmentModifier enrollmentModifier = (EnrollmentModifier) client;
            boolean enrollmentStateChanged = enrollmentModifier.hasEnrollmentStateChanged();
            Slog.d(TAG, "Enrollment state changed: " + enrollmentStateChanged);
            if (enrollmentStateChanged) {
                notifyAllEnrollmentStateChanged(client.getTargetUserId(), client.getSensorId(), enrollmentModifier.hasEnrollments());
            }
        }
        notifyBiometricStateListeners(this.mBiometricState);
    }

    private void notifyBiometricStateListeners(int newState) {
        Iterator<IBiometricStateListener> it = this.mBiometricStateListeners.iterator();
        while (it.hasNext()) {
            IBiometricStateListener listener = it.next();
            try {
                listener.onStateChanged(newState);
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception in biometric state change", e);
            }
        }
    }

    public void notifyAllEnrollmentStateChanged(int userId, int sensorId, boolean hasEnrollments) {
        Iterator<IBiometricStateListener> it = this.mBiometricStateListeners.iterator();
        while (it.hasNext()) {
            IBiometricStateListener listener = it.next();
            notifyEnrollmentStateChanged(listener, userId, sensorId, hasEnrollments);
        }
    }

    public void notifyEnrollmentStateChanged(IBiometricStateListener listener, int userId, int sensorId, boolean hasEnrollments) {
        try {
            listener.onEnrollmentsChanged(userId, sensorId, hasEnrollments);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
    }

    public void registerBiometricStateListener(IBiometricStateListener listener) {
        this.mBiometricStateListeners.add(listener);
    }
}
