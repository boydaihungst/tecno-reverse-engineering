package com.android.server.locksettings;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.service.gatekeeper.IGateKeeperService;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.widget.VerifyCredentialResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public class BiometricDeferredQueue {
    private static final String TAG = "BiometricDeferredQueue";
    private final Context mContext;
    private FaceManager mFaceManager;
    private FaceResetLockoutTask mFaceResetLockoutTask;
    private FingerprintManager mFingerprintManager;
    private final Handler mHandler;
    private final SyntheticPasswordManager mSpManager;
    private final FaceResetLockoutTask.FinishCallback mFaceFinishCallback = new FaceResetLockoutTask.FinishCallback() { // from class: com.android.server.locksettings.BiometricDeferredQueue$$ExternalSyntheticLambda1
        @Override // com.android.server.locksettings.BiometricDeferredQueue.FaceResetLockoutTask.FinishCallback
        public final void onFinished() {
            BiometricDeferredQueue.this.m4542x512b22ae();
        }
    };
    private final ArrayList<UserAuthInfo> mPendingResetLockoutsForFingerprint = new ArrayList<>();
    private final ArrayList<UserAuthInfo> mPendingResetLockoutsForFace = new ArrayList<>();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class UserAuthInfo {
        final byte[] gatekeeperPassword;
        final int userId;

        UserAuthInfo(int userId, byte[] gatekeeperPassword) {
            this.userId = userId;
            this.gatekeeperPassword = gatekeeperPassword;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class FaceResetLockoutTask implements FaceManager.GenerateChallengeCallback {
        FaceManager faceManager;
        FinishCallback finishCallback;
        List<UserAuthInfo> pendingResetLockuts;
        Set<Integer> sensorIds;
        SyntheticPasswordManager spManager;

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public interface FinishCallback {
            void onFinished();
        }

        FaceResetLockoutTask(FinishCallback finishCallback, FaceManager faceManager, SyntheticPasswordManager spManager, Set<Integer> sensorIds, List<UserAuthInfo> pendingResetLockouts) {
            this.finishCallback = finishCallback;
            this.faceManager = faceManager;
            this.spManager = spManager;
            this.sensorIds = sensorIds;
            this.pendingResetLockuts = pendingResetLockouts;
        }

        public void onGenerateChallengeResult(int sensorId, int userId, long challenge) {
            if (!this.sensorIds.contains(Integer.valueOf(sensorId))) {
                Slog.e(BiometricDeferredQueue.TAG, "Unknown sensorId received: " + sensorId);
                return;
            }
            for (UserAuthInfo userAuthInfo : this.pendingResetLockuts) {
                Slog.d(BiometricDeferredQueue.TAG, "Resetting face lockout for sensor: " + sensorId + ", user: " + userAuthInfo.userId);
                byte[] hat = BiometricDeferredQueue.requestHatFromGatekeeperPassword(this.spManager, userAuthInfo, challenge);
                if (hat != null) {
                    this.faceManager.resetLockout(sensorId, userAuthInfo.userId, hat);
                }
            }
            this.sensorIds.remove(Integer.valueOf(sensorId));
            this.faceManager.revokeChallenge(sensorId, userId, challenge);
            if (this.sensorIds.isEmpty()) {
                Slog.d(BiometricDeferredQueue.TAG, "Done requesting resetLockout for all face sensors");
                this.finishCallback.onFinished();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-locksettings-BiometricDeferredQueue  reason: not valid java name */
    public /* synthetic */ void m4542x512b22ae() {
        this.mFaceResetLockoutTask = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BiometricDeferredQueue(Context context, SyntheticPasswordManager spManager, Handler handler) {
        this.mContext = context;
        this.mSpManager = spManager;
        this.mHandler = handler;
    }

    public void systemReady(FingerprintManager fingerprintManager, FaceManager faceManager) {
        this.mFingerprintManager = fingerprintManager;
        this.mFaceManager = faceManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addPendingLockoutResetForUser(final int userId, final byte[] gatekeeperPassword) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.BiometricDeferredQueue$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BiometricDeferredQueue.this.m4541xae398cfb(userId, gatekeeperPassword);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addPendingLockoutResetForUser$1$com-android-server-locksettings-BiometricDeferredQueue  reason: not valid java name */
    public /* synthetic */ void m4541xae398cfb(int userId, byte[] gatekeeperPassword) {
        FaceManager faceManager = this.mFaceManager;
        if (faceManager != null && faceManager.hasEnrolledTemplates(userId)) {
            Slog.d(TAG, "Face addPendingLockoutResetForUser: " + userId);
            this.mPendingResetLockoutsForFace.add(new UserAuthInfo(userId, gatekeeperPassword));
        }
        FingerprintManager fingerprintManager = this.mFingerprintManager;
        if (fingerprintManager != null && fingerprintManager.hasEnrolledFingerprints(userId)) {
            Slog.d(TAG, "Fingerprint addPendingLockoutResetForUser: " + userId);
            this.mPendingResetLockoutsForFingerprint.add(new UserAuthInfo(userId, gatekeeperPassword));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void processPendingLockoutResets() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.BiometricDeferredQueue$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                BiometricDeferredQueue.this.m4543x829320d();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$processPendingLockoutResets$2$com-android-server-locksettings-BiometricDeferredQueue  reason: not valid java name */
    public /* synthetic */ void m4543x829320d() {
        if (!this.mPendingResetLockoutsForFace.isEmpty()) {
            Slog.d(TAG, "Processing pending resetLockout for face");
            processPendingLockoutsForFace(new ArrayList(this.mPendingResetLockoutsForFace));
            this.mPendingResetLockoutsForFace.clear();
        }
        if (!this.mPendingResetLockoutsForFingerprint.isEmpty()) {
            Slog.d(TAG, "Processing pending resetLockout for fingerprint");
            processPendingLockoutsForFingerprint(new ArrayList(this.mPendingResetLockoutsForFingerprint));
            this.mPendingResetLockoutsForFingerprint.clear();
        }
    }

    private void processPendingLockoutsForFingerprint(List<UserAuthInfo> pendingResetLockouts) {
        FingerprintManager fingerprintManager = this.mFingerprintManager;
        if (fingerprintManager != null) {
            List<FingerprintSensorPropertiesInternal> fingerprintSensorProperties = fingerprintManager.getSensorPropertiesInternal();
            for (FingerprintSensorPropertiesInternal prop : fingerprintSensorProperties) {
                if (!prop.resetLockoutRequiresHardwareAuthToken) {
                    for (UserAuthInfo user : pendingResetLockouts) {
                        this.mFingerprintManager.resetLockout(prop.sensorId, user.userId, null);
                    }
                } else if (!prop.resetLockoutRequiresChallenge) {
                    for (UserAuthInfo user2 : pendingResetLockouts) {
                        Slog.d(TAG, "Resetting fingerprint lockout for sensor: " + prop.sensorId + ", user: " + user2.userId);
                        byte[] hat = requestHatFromGatekeeperPassword(this.mSpManager, user2, 0L);
                        if (hat != null) {
                            this.mFingerprintManager.resetLockout(prop.sensorId, user2.userId, hat);
                        }
                    }
                } else {
                    Slog.w(TAG, "No fingerprint HAL interface requires HAT with challenge, sensorId: " + prop.sensorId);
                }
            }
        }
    }

    private void processPendingLockoutsForFace(List<UserAuthInfo> pendingResetLockouts) {
        if (this.mFaceManager != null) {
            if (this.mFaceResetLockoutTask != null) {
                Slog.w(TAG, "mFaceGenerateChallengeCallback not null, previous operation may be stuck");
            }
            List<FaceSensorPropertiesInternal> faceSensorProperties = this.mFaceManager.getSensorPropertiesInternal();
            Set<Integer> sensorIds = new ArraySet<>();
            for (FaceSensorPropertiesInternal prop : faceSensorProperties) {
                sensorIds.add(Integer.valueOf(prop.sensorId));
            }
            this.mFaceResetLockoutTask = new FaceResetLockoutTask(this.mFaceFinishCallback, this.mFaceManager, this.mSpManager, sensorIds, pendingResetLockouts);
            for (FaceSensorPropertiesInternal prop2 : faceSensorProperties) {
                if (prop2.resetLockoutRequiresHardwareAuthToken) {
                    for (UserAuthInfo user : pendingResetLockouts) {
                        if (prop2.resetLockoutRequiresChallenge) {
                            Slog.d(TAG, "Generating challenge for sensor: " + prop2.sensorId + ", user: " + user.userId);
                            this.mFaceManager.generateChallenge(prop2.sensorId, user.userId, this.mFaceResetLockoutTask);
                        } else {
                            Slog.d(TAG, "Resetting face lockout for sensor: " + prop2.sensorId + ", user: " + user.userId);
                            byte[] hat = requestHatFromGatekeeperPassword(this.mSpManager, user, 0L);
                            if (hat != null) {
                                this.mFaceManager.resetLockout(prop2.sensorId, user.userId, hat);
                            }
                        }
                    }
                } else {
                    Slog.w(TAG, "Lockout is below the HAL for all face authentication interfaces, sensorId: " + prop2.sensorId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static byte[] requestHatFromGatekeeperPassword(SyntheticPasswordManager spManager, UserAuthInfo userAuthInfo, long challenge) {
        VerifyCredentialResponse response = spManager.verifyChallengeInternal(getGatekeeperService(), userAuthInfo.gatekeeperPassword, challenge, userAuthInfo.userId);
        if (response == null) {
            Slog.wtf(TAG, "VerifyChallenge failed, null response");
            return null;
        } else if (response.getResponseCode() != 0) {
            Slog.wtf(TAG, "VerifyChallenge failed, response: " + response.getResponseCode());
            return null;
        } else {
            if (response.getGatekeeperHAT() == null) {
                Slog.e(TAG, "Null HAT received from spManager");
            }
            return response.getGatekeeperHAT();
        }
    }

    private static synchronized IGateKeeperService getGatekeeperService() {
        synchronized (BiometricDeferredQueue.class) {
            IBinder service = ServiceManager.getService("android.service.gatekeeper.IGateKeeperService");
            if (service == null) {
                Slog.e(TAG, "Unable to acquire GateKeeperService");
                return null;
            }
            return IGateKeeperService.Stub.asInterface(service);
        }
    }
}
