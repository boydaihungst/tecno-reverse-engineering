package com.android.server.biometrics;

import android.content.Context;
import android.hardware.biometrics.IBiometricAuthenticator;
import android.hardware.biometrics.IBiometricSensorReceiver;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes.dex */
public abstract class BiometricSensor {
    static final int STATE_AUTHENTICATING = 3;
    static final int STATE_CANCELING = 4;
    static final int STATE_COOKIE_RETURNED = 2;
    static final int STATE_STOPPED = 5;
    static final int STATE_UNKNOWN = 0;
    static final int STATE_WAITING_FOR_COOKIE = 1;
    private static final String TAG = "BiometricService/Sensor";
    public final int id;
    public final IBiometricAuthenticator impl;
    private final Context mContext;
    private int mCookie;
    private int mError;
    private int mSensorState;
    private int mUpdatedStrength;
    public final int modality;
    public final int oemStrength;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface SensorState {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean confirmationAlwaysRequired(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract boolean confirmationSupported();

    /* JADX INFO: Access modifiers changed from: package-private */
    public BiometricSensor(Context context, int id, int modality, int strength, IBiometricAuthenticator impl) {
        this.mContext = context;
        this.id = id;
        this.modality = modality;
        this.oemStrength = strength;
        this.impl = impl;
        this.mUpdatedStrength = strength;
        goToStateUnknown();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void goToStateUnknown() {
        this.mSensorState = 0;
        this.mCookie = 0;
        this.mError = 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void goToStateWaitingForCookie(boolean requireConfirmation, IBinder token, long sessionId, int userId, IBiometricSensorReceiver sensorReceiver, String opPackageName, long requestId, int cookie, boolean allowBackgroundAuthentication) throws RemoteException {
        this.mCookie = cookie;
        this.impl.prepareForAuthentication(requireConfirmation, token, sessionId, userId, sensorReceiver, opPackageName, requestId, cookie, allowBackgroundAuthentication);
        this.mSensorState = 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void goToStateCookieReturnedIfCookieMatches(int cookie) {
        if (cookie == this.mCookie) {
            Slog.d(TAG, "Sensor(" + this.id + ") matched cookie: " + cookie);
            this.mSensorState = 2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startSensor() throws RemoteException {
        this.impl.startPreparedClient(this.mCookie);
        this.mSensorState = 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void goToStateCancelling(IBinder token, String opPackageName, long requestId) throws RemoteException {
        if (this.mSensorState != 4) {
            this.impl.cancelAuthenticationFromService(token, opPackageName, requestId);
            this.mSensorState = 4;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void goToStoppedStateIfCookieMatches(int cookie, int error) {
        if (cookie == this.mCookie) {
            Slog.d(TAG, "Sensor(" + this.id + ") now in STATE_STOPPED");
            this.mError = error;
            this.mSensorState = 5;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentStrength() {
        return this.oemStrength | this.mUpdatedStrength;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSensorState() {
        return this.mSensorState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCookie() {
        return this.mCookie;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateStrength(int newStrength) {
        String log = "updateStrength: Before(" + this + ")";
        this.mUpdatedStrength = newStrength;
        Slog.d(TAG, log + " After(" + this + ")");
    }

    public String toString() {
        return "ID(" + this.id + "), oemStrength: " + this.oemStrength + ", updatedStrength: " + this.mUpdatedStrength + ", modality " + this.modality + ", state: " + this.mSensorState + ", cookie: " + this.mCookie;
    }
}
