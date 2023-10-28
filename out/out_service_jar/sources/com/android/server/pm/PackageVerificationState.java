package com.android.server.pm;

import android.util.SparseBooleanArray;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PackageVerificationState {
    private boolean mIntegrityVerificationComplete;
    private final VerificationParams mParams;
    private boolean mRequiredVerificationComplete;
    private boolean mRequiredVerificationPassed;
    private int mRequiredVerifierUid;
    private boolean mSufficientVerificationComplete;
    private boolean mSufficientVerificationPassed;
    private final SparseBooleanArray mSufficientVerifierUids = new SparseBooleanArray();
    private boolean mExtendedTimeout = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageVerificationState(VerificationParams params) {
        this.mParams = params;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VerificationParams getVerificationParams() {
        return this.mParams;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRequiredVerifierUid(int uid) {
        this.mRequiredVerifierUid = uid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addSufficientVerifier(int uid) {
        this.mSufficientVerifierUids.put(uid, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setVerifierResponse(int uid, int code) {
        if (uid == this.mRequiredVerifierUid) {
            this.mRequiredVerificationComplete = true;
            switch (code) {
                case 2:
                    this.mSufficientVerifierUids.clear();
                case 1:
                    this.mRequiredVerificationPassed = true;
                    break;
                default:
                    this.mRequiredVerificationPassed = false;
                    break;
            }
            return true;
        } else if (this.mSufficientVerifierUids.get(uid)) {
            if (code == 1) {
                this.mSufficientVerificationComplete = true;
                this.mSufficientVerificationPassed = true;
            }
            this.mSufficientVerifierUids.delete(uid);
            if (this.mSufficientVerifierUids.size() == 0) {
                this.mSufficientVerificationComplete = true;
            }
            return true;
        } else {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVerificationComplete() {
        if (!this.mRequiredVerificationComplete) {
            return false;
        }
        if (this.mSufficientVerifierUids.size() == 0) {
            return true;
        }
        return this.mSufficientVerificationComplete;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInstallAllowed() {
        if (!this.mRequiredVerificationPassed) {
            return false;
        }
        if (this.mSufficientVerificationComplete) {
            return this.mSufficientVerificationPassed;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void extendTimeout() {
        if (!this.mExtendedTimeout) {
            this.mExtendedTimeout = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean timeoutExtended() {
        return this.mExtendedTimeout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIntegrityVerificationResult(int code) {
        this.mIntegrityVerificationComplete = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isIntegrityVerificationComplete() {
        return this.mIntegrityVerificationComplete;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean areAllVerificationsComplete() {
        return this.mIntegrityVerificationComplete && isVerificationComplete();
    }
}
