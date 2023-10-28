package com.android.server.biometrics.sensors.face.hidl;

import android.content.Context;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.hardware.biometrics.face.V1_0.IBiometricsFaceClientCallback;
import android.hardware.biometrics.face.V1_0.OptionalBool;
import android.hardware.biometrics.face.V1_0.OptionalUint64;
import android.hardware.face.Face;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.sensors.face.FaceUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/* loaded from: classes.dex */
public class TestHal extends IBiometricsFace.Stub {
    private static final String TAG = "face.hidl.TestHal";
    private IBiometricsFaceClientCallback mCallback;
    private final Context mContext;
    private final int mSensorId;
    private int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TestHal(Context context, int sensorId) {
        this.mContext = context;
        this.mSensorId = sensorId;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public OptionalUint64 setCallback(IBiometricsFaceClientCallback clientCallback) {
        this.mCallback = clientCallback;
        OptionalUint64 result = new OptionalUint64();
        result.status = 0;
        return new OptionalUint64();
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public int setActiveUser(int userId, String storePath) {
        this.mUserId = userId;
        return 0;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public OptionalUint64 generateChallenge(int challengeTimeoutSec) {
        Slog.w(TAG, "generateChallenge");
        OptionalUint64 result = new OptionalUint64();
        result.status = 0;
        result.value = 0L;
        return result;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public int enroll(ArrayList<Byte> hat, int timeoutSec, ArrayList<Integer> disabledFeatures) {
        Slog.w(TAG, "enroll");
        return 0;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public int revokeChallenge() {
        return 0;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public int setFeature(int feature, boolean enabled, ArrayList<Byte> hat, int faceId) {
        return 0;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public OptionalBool getFeature(int feature, int faceId) {
        OptionalBool result = new OptionalBool();
        result.status = 0;
        result.value = true;
        return result;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public OptionalUint64 getAuthenticatorId() {
        OptionalUint64 result = new OptionalUint64();
        result.status = 0;
        result.value = 0L;
        return result;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public int cancel() throws RemoteException {
        IBiometricsFaceClientCallback iBiometricsFaceClientCallback = this.mCallback;
        if (iBiometricsFaceClientCallback != null) {
            iBiometricsFaceClientCallback.onError(0L, 0, 5, 0);
            return 0;
        }
        return 0;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public int enumerate() throws RemoteException {
        Slog.w(TAG, "enumerate");
        IBiometricsFaceClientCallback iBiometricsFaceClientCallback = this.mCallback;
        if (iBiometricsFaceClientCallback != null) {
            iBiometricsFaceClientCallback.onEnumerate(0L, new ArrayList<>(), 0);
        }
        return 0;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public int remove(int faceId) throws RemoteException {
        Slog.w(TAG, "remove");
        IBiometricsFaceClientCallback iBiometricsFaceClientCallback = this.mCallback;
        if (iBiometricsFaceClientCallback != null) {
            if (faceId == 0) {
                List<Face> faces = FaceUtils.getInstance(this.mSensorId).getBiometricsForUser(this.mContext, this.mUserId);
                ArrayList<Integer> faceIds = new ArrayList<>();
                for (Face face : faces) {
                    faceIds.add(Integer.valueOf(face.getBiometricId()));
                }
                this.mCallback.onRemoved(0L, faceIds, this.mUserId);
                return 0;
            }
            iBiometricsFaceClientCallback.onRemoved(0L, new ArrayList<>(Collections.singletonList(Integer.valueOf(faceId))), this.mUserId);
            return 0;
        }
        return 0;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public int authenticate(long operationId) {
        Slog.w(TAG, "authenticate");
        return 0;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public int userActivity() {
        return 0;
    }

    @Override // android.hardware.biometrics.face.V1_0.IBiometricsFace
    public int resetLockout(ArrayList<Byte> hat) {
        Slog.w(TAG, "resetLockout");
        return 0;
    }
}
