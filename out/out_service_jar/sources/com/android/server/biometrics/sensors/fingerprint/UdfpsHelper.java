package com.android.server.biometrics.sensors.fingerprint;

import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.IHwInterface;
import android.os.RemoteException;
import android.util.Slog;
/* loaded from: classes.dex */
public class UdfpsHelper {
    private static final String TAG = "UdfpsHelper";

    public static void onFingerDown(IBiometricsFingerprint daemon, int x, int y, float minor, float major) {
        android.hardware.biometrics.fingerprint.V2_3.IBiometricsFingerprint extension = android.hardware.biometrics.fingerprint.V2_3.IBiometricsFingerprint.castFrom((IHwInterface) daemon);
        if (extension == null) {
            Slog.v(TAG, "onFingerDown | failed to cast the HIDL to V2_3");
            return;
        }
        try {
            extension.onFingerDown(x, y, minor, major);
        } catch (RemoteException e) {
            Slog.e(TAG, "onFingerDown | RemoteException: ", e);
        }
    }

    public static void onFingerUp(IBiometricsFingerprint daemon) {
        android.hardware.biometrics.fingerprint.V2_3.IBiometricsFingerprint extension = android.hardware.biometrics.fingerprint.V2_3.IBiometricsFingerprint.castFrom((IHwInterface) daemon);
        if (extension == null) {
            Slog.v(TAG, "onFingerUp | failed to cast the HIDL to V2_3");
            return;
        }
        try {
            extension.onFingerUp();
        } catch (RemoteException e) {
            Slog.e(TAG, "onFingerUp | RemoteException: ", e);
        }
    }

    public static boolean isValidAcquisitionMessage(Context context, int acquireInfo, int vendorCode) {
        return FingerprintManager.getAcquiredString(context, acquireInfo, vendorCode) != null;
    }
}
