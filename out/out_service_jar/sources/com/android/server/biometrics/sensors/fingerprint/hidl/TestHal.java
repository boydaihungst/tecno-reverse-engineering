package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback;
import android.hardware.biometrics.fingerprint.V2_3.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.sensors.fingerprint.FingerprintUtils;
import java.util.List;
/* loaded from: classes.dex */
public class TestHal extends IBiometricsFingerprint.Stub {
    private static final String TAG = "fingerprint.hidl.TestHal";
    private IBiometricsFingerprintClientCallback mCallback;
    private final Context mContext;
    private final int mSensorId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TestHal(Context context, int sensorId) {
        this.mContext = context;
        this.mSensorId = sensorId;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_3.IBiometricsFingerprint
    public boolean isUdfps(int sensorId) {
        return false;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_3.IBiometricsFingerprint
    public void onFingerDown(int x, int y, float minor, float major) {
    }

    @Override // android.hardware.biometrics.fingerprint.V2_3.IBiometricsFingerprint
    public void onFingerUp() {
    }

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
    public long setNotify(IBiometricsFingerprintClientCallback clientCallback) {
        this.mCallback = clientCallback;
        return 0L;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
    public long preEnroll() {
        return 0L;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
    public int enroll(byte[] hat, int gid, int timeoutSec) {
        Slog.w(TAG, "enroll");
        return 0;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
    public int postEnroll() {
        return 0;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
    public long getAuthenticatorId() {
        return 0L;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
    public int cancel() throws RemoteException {
        IBiometricsFingerprintClientCallback iBiometricsFingerprintClientCallback = this.mCallback;
        if (iBiometricsFingerprintClientCallback != null) {
            iBiometricsFingerprintClientCallback.onError(0L, 5, 0);
        }
        return 0;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
    public int enumerate() throws RemoteException {
        Slog.w(TAG, "Enumerate");
        IBiometricsFingerprintClientCallback iBiometricsFingerprintClientCallback = this.mCallback;
        if (iBiometricsFingerprintClientCallback != null) {
            iBiometricsFingerprintClientCallback.onEnumerate(0L, 0, 0, 0);
            return 0;
        }
        return 0;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
    public int remove(int gid, int fid) throws RemoteException {
        Slog.w(TAG, "Remove");
        IBiometricsFingerprintClientCallback iBiometricsFingerprintClientCallback = this.mCallback;
        if (iBiometricsFingerprintClientCallback != null) {
            if (fid == 0) {
                List<Fingerprint> fingerprints = FingerprintUtils.getInstance(this.mSensorId).getBiometricsForUser(this.mContext, gid);
                for (int i = 0; i < fingerprints.size(); i++) {
                    Fingerprint fp = fingerprints.get(i);
                    this.mCallback.onRemoved(0L, fp.getBiometricId(), gid, (fingerprints.size() - i) - 1);
                }
                return 0;
            }
            iBiometricsFingerprintClientCallback.onRemoved(0L, fid, gid, 0);
            return 0;
        }
        return 0;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
    public int setActiveGroup(int gid, String storePath) {
        return 0;
    }

    @Override // android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint
    public int authenticate(long operationId, int gid) {
        Slog.w(TAG, "Authenticate");
        return 0;
    }
}
