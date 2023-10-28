package com.android.server.security;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelDuration;
import android.os.RemoteException;
import android.security.attestationverification.AttestationProfile;
import android.security.attestationverification.IAttestationVerificationManagerService;
import android.security.attestationverification.IVerificationResult;
import android.security.attestationverification.VerificationToken;
import android.util.ExceptionUtils;
import android.util.Slog;
import com.android.internal.infra.AndroidFuture;
import com.android.server.SystemService;
/* loaded from: classes2.dex */
public class AttestationVerificationManagerService extends SystemService {
    private static final String TAG = "AVF";
    private final AttestationVerificationPeerDeviceVerifier mPeerDeviceVerifier;
    private final IBinder mService;

    public AttestationVerificationManagerService(Context context) throws Exception {
        super(context);
        this.mService = new IAttestationVerificationManagerService.Stub() { // from class: com.android.server.security.AttestationVerificationManagerService.1
            public void verifyAttestation(AttestationProfile profile, int localBindingType, Bundle requirements, byte[] attestation, AndroidFuture resultCallback) throws RemoteException {
                enforceUsePermission();
                try {
                    Slog.d(AttestationVerificationManagerService.TAG, "verifyAttestation");
                    AttestationVerificationManagerService.this.verifyAttestationForAllVerifiers(profile, localBindingType, requirements, attestation, resultCallback);
                } catch (Throwable t) {
                    Slog.e(AttestationVerificationManagerService.TAG, "failed to verify attestation", t);
                    throw ExceptionUtils.propagate(t, RemoteException.class);
                }
            }

            public void verifyToken(VerificationToken token, ParcelDuration parcelDuration, AndroidFuture resultCallback) throws RemoteException {
                enforceUsePermission();
                resultCallback.complete(0);
            }

            private void enforceUsePermission() {
                AttestationVerificationManagerService.this.getContext().enforceCallingOrSelfPermission("android.permission.USE_ATTESTATION_VERIFICATION_SERVICE", null);
            }
        };
        this.mPeerDeviceVerifier = new AttestationVerificationPeerDeviceVerifier(context);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void verifyAttestationForAllVerifiers(AttestationProfile profile, int localBindingType, Bundle requirements, byte[] attestation, AndroidFuture<IVerificationResult> resultCallback) {
        IVerificationResult result = new IVerificationResult();
        result.token = null;
        switch (profile.getAttestationProfileId()) {
            case 2:
                Slog.d(TAG, "Verifying Self Trusted profile.");
                try {
                    result.resultCode = AttestationVerificationSelfTrustedVerifierForTesting.getInstance().verifyAttestation(localBindingType, requirements, attestation);
                    break;
                } catch (Throwable th) {
                    result.resultCode = 2;
                    break;
                }
            case 3:
                Slog.d(TAG, "Verifying Peer Device profile.");
                result.resultCode = this.mPeerDeviceVerifier.verifyAttestation(localBindingType, requirements, attestation);
                break;
            default:
                Slog.d(TAG, "No profile found, defaulting.");
                result.resultCode = 0;
                break;
        }
        resultCallback.complete(result);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        Slog.d(TAG, "Started");
        publishBinderService("attestation_verification", this.mService);
    }
}
