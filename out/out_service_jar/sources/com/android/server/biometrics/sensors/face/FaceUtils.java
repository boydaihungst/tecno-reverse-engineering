package com.android.server.biometrics.sensors.face;

import android.content.Context;
import android.hardware.face.Face;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.server.biometrics.sensors.BiometricUtils;
import java.util.List;
/* loaded from: classes.dex */
public class FaceUtils implements BiometricUtils<Face> {
    private static final String LEGACY_FACE_FILE = "settings_face.xml";
    private static final Object sInstanceLock = new Object();
    private static SparseArray<FaceUtils> sInstances;
    private final String mFileName;
    private final SparseArray<FaceUserState> mUserStates = new SparseArray<>();

    public static FaceUtils getInstance(int sensorId) {
        return getInstance(sensorId, null);
    }

    private static FaceUtils getInstance(int sensorId, String fileName) {
        FaceUtils utils;
        synchronized (sInstanceLock) {
            if (sInstances == null) {
                sInstances = new SparseArray<>();
            }
            if (sInstances.get(sensorId) == null) {
                if (fileName == null) {
                    fileName = "settings_face_" + sensorId + ".xml";
                }
                sInstances.put(sensorId, new FaceUtils(fileName));
            }
            utils = sInstances.get(sensorId);
        }
        return utils;
    }

    public static FaceUtils getLegacyInstance(int sensorId) {
        return getInstance(sensorId, LEGACY_FACE_FILE);
    }

    private FaceUtils(String fileName) {
        this.mFileName = fileName;
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public List<Face> getBiometricsForUser(Context ctx, int userId) {
        return getStateForUser(ctx, userId).getBiometrics();
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public void addBiometricForUser(Context ctx, int userId, Face face) {
        getStateForUser(ctx, userId).addBiometric(face);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public void removeBiometricForUser(Context ctx, int userId, int faceId) {
        getStateForUser(ctx, userId).removeBiometric(faceId);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public void renameBiometricForUser(Context ctx, int userId, int faceId, CharSequence name) {
        if (TextUtils.isEmpty(name)) {
            return;
        }
        getStateForUser(ctx, userId).renameBiometric(faceId, name);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public CharSequence getUniqueName(Context context, int userId) {
        return getStateForUser(context, userId).getUniqueName();
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public void setInvalidationInProgress(Context context, int userId, boolean inProgress) {
        getStateForUser(context, userId).setInvalidationInProgress(inProgress);
    }

    @Override // com.android.server.biometrics.sensors.BiometricUtils
    public boolean isInvalidationInProgress(Context context, int userId) {
        return getStateForUser(context, userId).isInvalidationInProgress();
    }

    private FaceUserState getStateForUser(Context ctx, int userId) {
        FaceUserState state;
        synchronized (this) {
            state = this.mUserStates.get(userId);
            if (state == null) {
                state = new FaceUserState(ctx, userId, this.mFileName);
                this.mUserStates.put(userId, state);
            }
        }
        return state;
    }
}
