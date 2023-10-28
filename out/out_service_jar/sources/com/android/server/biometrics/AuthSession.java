package com.android.server.biometrics;

import android.content.Context;
import android.hardware.biometrics.IBiometricSensorReceiver;
import android.hardware.biometrics.IBiometricServiceReceiver;
import android.hardware.biometrics.IBiometricSysuiReceiver;
import android.hardware.biometrics.PromptInfo;
import android.hardware.biometrics.common.OperationContext;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.IBinder;
import android.os.RemoteException;
import android.security.KeyStore;
import android.util.Slog;
import com.android.internal.statusbar.IStatusBarService;
import com.android.server.biometrics.log.BiometricFrameworkStatsLogger;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
/* loaded from: classes.dex */
public final class AuthSession implements IBinder.DeathRecipient {
    private static final boolean DEBUG = false;
    private static final String TAG = "BiometricService/AuthSession";
    private long mAuthenticatedTimeMs;
    private boolean mCancelled;
    private final ClientDeathReceiver mClientDeathReceiver;
    private final IBiometricServiceReceiver mClientReceiver;
    private final Context mContext;
    private final boolean mDebugEnabled;
    private int mErrorEscrow;
    private final List<FingerprintSensorPropertiesInternal> mFingerprintSensorProperties;
    private final KeyStore mKeyStore;
    private int mMultiSensorMode;
    private final String mOpPackageName;
    private final long mOperationId;
    final PreAuthInfo mPreAuthInfo;
    final PromptInfo mPromptInfo;
    private final Random mRandom;
    private final long mRequestId;
    final IBiometricSensorReceiver mSensorReceiver;
    private int[] mSensors;
    private long mStartTimeMs;
    private final IStatusBarService mStatusBarService;
    final IBiometricSysuiReceiver mSysuiReceiver;
    final IBinder mToken;
    private byte[] mTokenEscrow;
    private final int mUserId;
    private int mVendorCodeEscrow;
    private int mState = 0;
    private int mAuthenticatedSensorId = -1;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface ClientDeathReceiver {
        void onClientDied();
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface SessionState {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AuthSession(Context context, IStatusBarService statusBarService, IBiometricSysuiReceiver sysuiReceiver, KeyStore keystore, Random random, ClientDeathReceiver clientDeathReceiver, PreAuthInfo preAuthInfo, IBinder token, long requestId, long operationId, int userId, IBiometricSensorReceiver sensorReceiver, IBiometricServiceReceiver clientReceiver, String opPackageName, PromptInfo promptInfo, boolean debugEnabled, List<FingerprintSensorPropertiesInternal> fingerprintSensorProperties) {
        Slog.d(TAG, "Creating AuthSession with: " + preAuthInfo);
        this.mContext = context;
        this.mStatusBarService = statusBarService;
        this.mSysuiReceiver = sysuiReceiver;
        this.mKeyStore = keystore;
        this.mRandom = random;
        this.mClientDeathReceiver = clientDeathReceiver;
        this.mPreAuthInfo = preAuthInfo;
        this.mToken = token;
        this.mRequestId = requestId;
        this.mOperationId = operationId;
        this.mUserId = userId;
        this.mSensorReceiver = sensorReceiver;
        this.mClientReceiver = clientReceiver;
        this.mOpPackageName = opPackageName;
        this.mPromptInfo = promptInfo;
        this.mDebugEnabled = debugEnabled;
        this.mFingerprintSensorProperties = fingerprintSensorProperties;
        this.mCancelled = false;
        try {
            clientReceiver.asBinder().linkToDeath(this, 0);
        } catch (RemoteException e) {
            Slog.w(TAG, "Unable to link to death");
        }
        setSensorsToStateUnknown();
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        Slog.e(TAG, "Binder died, session: " + this);
        this.mClientDeathReceiver.onClientDied();
    }

    private int getEligibleModalities() {
        return this.mPreAuthInfo.getEligibleModalities();
    }

    private void setSensorsToStateUnknown() {
        for (BiometricSensor sensor : this.mPreAuthInfo.eligibleSensors) {
            sensor.goToStateUnknown();
        }
    }

    private void setSensorsToStateWaitingForCookie(boolean isTryAgain) throws RemoteException {
        for (BiometricSensor sensor : this.mPreAuthInfo.eligibleSensors) {
            int state = sensor.getSensorState();
            if (isTryAgain && state != 5 && state != 4) {
                Slog.d(TAG, "Skip retry because sensor: " + sensor.id + " is: " + state);
            } else {
                int cookie = this.mRandom.nextInt(2147483646) + 1;
                boolean requireConfirmation = isConfirmationRequired(sensor);
                sensor.goToStateWaitingForCookie(requireConfirmation, this.mToken, this.mOperationId, this.mUserId, this.mSensorReceiver, this.mOpPackageName, this.mRequestId, cookie, this.mPromptInfo.isAllowBackgroundAuthentication());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void goToInitialState() throws RemoteException {
        if (this.mPreAuthInfo.credentialAvailable && this.mPreAuthInfo.eligibleSensors.isEmpty()) {
            this.mState = 9;
            int[] iArr = new int[0];
            this.mSensors = iArr;
            this.mMultiSensorMode = 0;
            this.mStatusBarService.showAuthenticationDialog(this.mPromptInfo, this.mSysuiReceiver, iArr, true, false, this.mUserId, this.mOperationId, this.mOpPackageName, this.mRequestId, 0);
        } else if (!this.mPreAuthInfo.eligibleSensors.isEmpty()) {
            setSensorsToStateWaitingForCookie(false);
            this.mState = 1;
        } else {
            throw new IllegalStateException("No authenticators requested");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCookieReceived(int cookie) {
        String str;
        if (this.mCancelled) {
            Slog.w(TAG, "Received cookie but already cancelled (ignoring): " + cookie);
        } else if (hasAuthenticated()) {
            Slog.d(TAG, "onCookieReceived after successful auth");
        } else {
            for (BiometricSensor sensor : this.mPreAuthInfo.eligibleSensors) {
                sensor.goToStateCookieReturnedIfCookieMatches(cookie);
            }
            if (allCookiesReceived()) {
                this.mStartTimeMs = System.currentTimeMillis();
                startAllPreparedSensorsExceptFingerprint();
                if (this.mState == 5) {
                    this.mState = 3;
                    return;
                }
                try {
                    boolean requireConfirmation = isConfirmationRequiredByAnyEligibleSensor();
                    this.mSensors = new int[this.mPreAuthInfo.eligibleSensors.size()];
                    for (int i = 0; i < this.mPreAuthInfo.eligibleSensors.size(); i++) {
                        this.mSensors[i] = this.mPreAuthInfo.eligibleSensors.get(i).id;
                    }
                    this.mMultiSensorMode = getMultiSensorModeForNewSession(this.mPreAuthInfo.eligibleSensors);
                    IStatusBarService iStatusBarService = this.mStatusBarService;
                    PromptInfo promptInfo = this.mPromptInfo;
                    IBiometricSysuiReceiver iBiometricSysuiReceiver = this.mSysuiReceiver;
                    int[] iArr = this.mSensors;
                    boolean shouldShowCredential = this.mPreAuthInfo.shouldShowCredential();
                    int i2 = this.mUserId;
                    long j = this.mOperationId;
                    String str2 = this.mOpPackageName;
                    str = TAG;
                    try {
                        iStatusBarService.showAuthenticationDialog(promptInfo, iBiometricSysuiReceiver, iArr, shouldShowCredential, requireConfirmation, i2, j, str2, this.mRequestId, this.mMultiSensorMode);
                        this.mState = 2;
                    } catch (RemoteException e) {
                        e = e;
                        Slog.e(str, "Remote exception", e);
                    }
                } catch (RemoteException e2) {
                    e = e2;
                    str = TAG;
                }
            } else {
                Slog.v(TAG, "onCookieReceived: still waiting");
            }
        }
    }

    private boolean isConfirmationRequired(BiometricSensor sensor) {
        return sensor.confirmationSupported() && (sensor.confirmationAlwaysRequired(this.mUserId) || this.mPreAuthInfo.confirmationRequested);
    }

    private boolean isConfirmationRequiredByAnyEligibleSensor() {
        for (BiometricSensor sensor : this.mPreAuthInfo.eligibleSensors) {
            if (isConfirmationRequired(sensor)) {
                return true;
            }
        }
        return false;
    }

    private void startAllPreparedSensorsExceptFingerprint() {
        startAllPreparedSensors(new Function() { // from class: com.android.server.biometrics.AuthSession$$ExternalSyntheticLambda4
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Boolean valueOf;
                BiometricSensor biometricSensor = (BiometricSensor) obj;
                valueOf = Boolean.valueOf(sensor.modality != 2);
                return valueOf;
            }
        });
    }

    private void startAllPreparedFingerprintSensors() {
        startAllPreparedSensors(new Function() { // from class: com.android.server.biometrics.AuthSession$$ExternalSyntheticLambda2
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Boolean valueOf;
                BiometricSensor biometricSensor = (BiometricSensor) obj;
                valueOf = Boolean.valueOf(sensor.modality == 2);
                return valueOf;
            }
        });
    }

    private void startAllPreparedSensors(Function<BiometricSensor, Boolean> filter) {
        for (BiometricSensor sensor : this.mPreAuthInfo.eligibleSensors) {
            if (filter.apply(sensor).booleanValue()) {
                try {
                    sensor.startSensor();
                } catch (RemoteException e) {
                    Slog.e(TAG, "Unable to start prepared client, sensor: " + sensor, e);
                }
            }
        }
    }

    private void cancelAllSensors() {
        cancelAllSensors(new Function() { // from class: com.android.server.biometrics.AuthSession$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return AuthSession.lambda$cancelAllSensors$2((BiometricSensor) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Boolean lambda$cancelAllSensors$2(BiometricSensor sensor) {
        return true;
    }

    private void cancelAllSensors(Function<BiometricSensor, Boolean> filter) {
        for (BiometricSensor sensor : this.mPreAuthInfo.eligibleSensors) {
            try {
                if (filter.apply(sensor).booleanValue()) {
                    Slog.d(TAG, "Cancelling sensorId: " + sensor.id);
                    sensor.goToStateCancelling(this.mToken, this.mOpPackageName, this.mRequestId);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to cancel authentication");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onErrorReceived(int sensorId, int cookie, int error, int vendorCode) throws RemoteException {
        Slog.d(TAG, "onErrorReceived sensor: " + sensorId + " error: " + error);
        if (!containsCookie(cookie)) {
            Slog.e(TAG, "Unknown/expired cookie: " + cookie);
            return false;
        }
        for (BiometricSensor sensor : this.mPreAuthInfo.eligibleSensors) {
            if (sensor.getSensorState() == 3) {
                sensor.goToStoppedStateIfCookieMatches(cookie, error);
            }
        }
        if (hasAuthenticated()) {
            Slog.d(TAG, "onErrorReceived after successful auth (ignoring)");
            return false;
        }
        this.mErrorEscrow = error;
        this.mVendorCodeEscrow = vendorCode;
        int modality = sensorIdToModality(sensorId);
        switch (this.mState) {
            case 1:
                boolean errorLockout = isAllowDeviceCredential();
                if (errorLockout) {
                    int authenticators = this.mPromptInfo.getAuthenticators();
                    this.mPromptInfo.setAuthenticators(Utils.removeBiometricBits(authenticators));
                    this.mState = 9;
                    this.mMultiSensorMode = 0;
                    int[] iArr = new int[0];
                    this.mSensors = iArr;
                    this.mStatusBarService.showAuthenticationDialog(this.mPromptInfo, this.mSysuiReceiver, iArr, true, false, this.mUserId, this.mOperationId, this.mOpPackageName, this.mRequestId, 0);
                    return false;
                }
                this.mClientReceiver.onError(modality, error, vendorCode);
                return true;
            case 2:
            case 3:
                boolean errorLockout2 = error == 7 || error == 9;
                if (isAllowDeviceCredential() && errorLockout2) {
                    this.mState = 9;
                    this.mStatusBarService.onBiometricError(modality, error, vendorCode);
                    return false;
                } else if (error == 5) {
                    this.mStatusBarService.hideAuthenticationDialog(this.mRequestId);
                    this.mClientReceiver.onError(modality, error, vendorCode);
                    return true;
                } else {
                    this.mState = 8;
                    this.mStatusBarService.onBiometricError(modality, error, vendorCode);
                    return false;
                }
            case 4:
                this.mClientReceiver.onError(modality, error, vendorCode);
                this.mStatusBarService.hideAuthenticationDialog(this.mRequestId);
                return true;
            case 5:
            case 6:
            case 7:
            case 8:
            default:
                Slog.e(TAG, "Unhandled error state, mState: " + this.mState);
                return false;
            case 9:
                Slog.d(TAG, "Biometric canceled, ignoring from state: " + this.mState);
                return false;
            case 10:
                this.mStatusBarService.hideAuthenticationDialog(this.mRequestId);
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAcquired(int sensorId, int acquiredInfo, int vendorCode) {
        if (hasAuthenticated()) {
            Slog.d(TAG, "onAcquired after successful auth");
            return;
        }
        String message = getAcquiredMessageForSensor(sensorId, acquiredInfo, vendorCode);
        Slog.d(TAG, "sensorId: " + sensorId + " acquiredInfo: " + acquiredInfo + " message: " + message);
        if (message == null) {
            return;
        }
        try {
            this.mStatusBarService.onBiometricHelp(sensorIdToModality(sensorId), message);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemEvent(int event) {
        if (hasAuthenticated()) {
            Slog.d(TAG, "onSystemEvent after successful auth");
        } else if (!this.mPromptInfo.isReceiveSystemEvents()) {
        } else {
            try {
                this.mClientReceiver.onSystemEvent(event);
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDialogAnimatedIn() {
        if (this.mState != 2) {
            Slog.e(TAG, "onDialogAnimatedIn, unexpected state: " + this.mState);
            return;
        }
        this.mState = 3;
        startAllPreparedFingerprintSensors();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTryAgainPressed() {
        if (hasAuthenticated()) {
            Slog.d(TAG, "onTryAgainPressed after successful auth");
            return;
        }
        if (this.mState != 4) {
            Slog.w(TAG, "onTryAgainPressed, state: " + this.mState);
        }
        try {
            setSensorsToStateWaitingForCookie(true);
            this.mState = 5;
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException: " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAuthenticationSucceeded(final int sensorId, boolean strong, byte[] token) {
        if (hasAuthenticated()) {
            Slog.d(TAG, "onAuthenticationSucceeded after successful auth");
            return;
        }
        this.mAuthenticatedSensorId = sensorId;
        if (strong) {
            this.mTokenEscrow = token;
        } else if (token != null) {
            Slog.w(TAG, "Dropping authToken for non-strong biometric, id: " + sensorId);
        }
        try {
            this.mStatusBarService.onBiometricAuthenticated(sensorIdToModality(sensorId));
            boolean requireConfirmation = isConfirmationRequiredByAnyEligibleSensor();
            if (!requireConfirmation) {
                this.mState = 7;
            } else {
                this.mAuthenticatedTimeMs = System.currentTimeMillis();
                this.mState = 6;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException", e);
        }
        cancelAllSensors(new Function() { // from class: com.android.server.biometrics.AuthSession$$ExternalSyntheticLambda3
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Boolean valueOf;
                int i = sensorId;
                BiometricSensor biometricSensor = (BiometricSensor) obj;
                valueOf = Boolean.valueOf(sensor.id != sensorId);
                return valueOf;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAuthenticationRejected(int sensorId) {
        if (hasAuthenticated()) {
            Slog.d(TAG, "onAuthenticationRejected after successful auth");
            return;
        }
        try {
            this.mStatusBarService.onBiometricError(sensorIdToModality(sensorId), 100, 0);
            if (pauseSensorIfSupported(sensorId)) {
                this.mState = 4;
            }
            this.mClientReceiver.onAuthenticationFailed();
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAuthenticationTimedOut(int sensorId, int cookie, int error, int vendorCode) {
        if (hasAuthenticated()) {
            Slog.d(TAG, "onAuthenticationTimedOut after successful auth");
            return;
        }
        try {
            this.mStatusBarService.onBiometricError(sensorIdToModality(sensorId), error, vendorCode);
            pauseSensorIfSupported(sensorId);
            this.mState = 4;
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException", e);
        }
    }

    private boolean pauseSensorIfSupported(final int sensorId) {
        if (sensorIdToModality(sensorId) == 8) {
            cancelAllSensors(new Function() { // from class: com.android.server.biometrics.AuthSession$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    Boolean valueOf;
                    int i = sensorId;
                    BiometricSensor biometricSensor = (BiometricSensor) obj;
                    valueOf = Boolean.valueOf(sensor.id == sensorId);
                    return valueOf;
                }
            });
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDeviceCredentialPressed() {
        if (hasAuthenticated()) {
            Slog.d(TAG, "onDeviceCredentialPressed after successful auth");
            return;
        }
        cancelAllSensors();
        this.mState = 9;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onClientDied() {
        try {
            switch (this.mState) {
                case 2:
                case 3:
                    this.mState = 10;
                    cancelAllSensors();
                    return false;
                default:
                    this.mStatusBarService.hideAuthenticationDialog(this.mRequestId);
                    return true;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote Exception: " + e);
            return true;
        }
    }

    private boolean hasAuthenticated() {
        return this.mAuthenticatedSensorId != -1;
    }

    private void logOnDialogDismissed(int reason) {
        int error;
        if (reason == 1) {
            long latency = System.currentTimeMillis() - this.mAuthenticatedTimeMs;
            OperationContext operationContext = new OperationContext();
            operationContext.isCrypto = isCrypto();
            BiometricFrameworkStatsLogger.getInstance().authenticate(operationContext, statsModality(), 0, 2, this.mDebugEnabled, latency, 3, this.mPreAuthInfo.confirmationRequested, this.mUserId, -1.0f);
            return;
        }
        long latency2 = System.currentTimeMillis() - this.mStartTimeMs;
        if (reason == 2) {
            error = 13;
        } else if (reason == 3) {
            error = 10;
        } else {
            error = 0;
        }
        OperationContext operationContext2 = new OperationContext();
        operationContext2.isCrypto = isCrypto();
        BiometricFrameworkStatsLogger.getInstance().error(operationContext2, statsModality(), 2, 2, this.mDebugEnabled, latency2, error, 0, this.mUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:13:0x003c A[Catch: all -> 0x006a, RemoteException -> 0x006c, TryCatch #0 {RemoteException -> 0x006c, blocks: (B:6:0x000b, B:7:0x0011, B:8:0x0017, B:9:0x0025, B:10:0x0032, B:11:0x0038, B:13:0x003c, B:15:0x0060, B:14:0x005a, B:20:0x006e), top: B:28:0x0005, outer: #1 }] */
    /* JADX WARN: Removed duplicated region for block: B:14:0x005a A[Catch: all -> 0x006a, RemoteException -> 0x006c, TryCatch #0 {RemoteException -> 0x006c, blocks: (B:6:0x000b, B:7:0x0011, B:8:0x0017, B:9:0x0025, B:10:0x0032, B:11:0x0038, B:13:0x003c, B:15:0x0060, B:14:0x005a, B:20:0x006e), top: B:28:0x0005, outer: #1 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void onDialogDismissed(int reason, byte[] credentialAttestation) {
        byte[] bArr;
        logOnDialogDismissed(reason);
        try {
            try {
                switch (reason) {
                    case 1:
                    case 4:
                        bArr = this.mTokenEscrow;
                        if (bArr != null) {
                            Slog.e(TAG, "mTokenEscrow is null");
                        } else {
                            int result = this.mKeyStore.addAuthToken(bArr);
                            Slog.d(TAG, "addAuthToken: " + result);
                        }
                        this.mClientReceiver.onAuthenticationSucceeded(Utils.getAuthenticationTypeForResult(reason));
                        break;
                    case 2:
                        this.mClientReceiver.onDialogDismissed(reason);
                        break;
                    case 3:
                        this.mClientReceiver.onError(getEligibleModalities(), 10, 0);
                        break;
                    case 5:
                    case 6:
                        this.mClientReceiver.onError(getEligibleModalities(), this.mErrorEscrow, this.mVendorCodeEscrow);
                        break;
                    case 7:
                        if (credentialAttestation == null) {
                            Slog.e(TAG, "credentialAttestation is null");
                        } else {
                            this.mKeyStore.addAuthToken(credentialAttestation);
                        }
                        bArr = this.mTokenEscrow;
                        if (bArr != null) {
                        }
                        this.mClientReceiver.onAuthenticationSucceeded(Utils.getAuthenticationTypeForResult(reason));
                        break;
                    default:
                        Slog.w(TAG, "Unhandled reason: " + reason);
                        break;
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception", e);
            }
            cancelAllSensors();
        } catch (Throwable th) {
            cancelAllSensors();
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onCancelAuthSession(boolean force) {
        if (hasAuthenticated()) {
            Slog.d(TAG, "onCancelAuthSession after successful auth");
            return true;
        }
        this.mCancelled = true;
        int i = this.mState;
        boolean authStarted = i == 1 || i == 2 || i == 3;
        cancelAllSensors();
        if (!authStarted || force) {
            try {
                this.mClientReceiver.onError(getEligibleModalities(), 5, 0);
                this.mStatusBarService.hideAuthenticationDialog(this.mRequestId);
                return true;
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception", e);
                return false;
            }
        }
        return false;
    }

    boolean isCrypto() {
        return this.mOperationId != 0;
    }

    private boolean containsCookie(int cookie) {
        for (BiometricSensor sensor : this.mPreAuthInfo.eligibleSensors) {
            if (sensor.getCookie() == cookie) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowDeviceCredential() {
        return Utils.isCredentialRequested(this.mPromptInfo);
    }

    boolean allCookiesReceived() {
        int remainingCookies = this.mPreAuthInfo.numSensorsWaitingForCookie();
        Slog.d(TAG, "Remaining cookies: " + remainingCookies);
        return remainingCookies == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getState() {
        return this.mState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getRequestId() {
        return this.mRequestId;
    }

    private int statsModality() {
        int modality = 0;
        for (BiometricSensor sensor : this.mPreAuthInfo.eligibleSensors) {
            if ((sensor.modality & 2) != 0) {
                modality |= 1;
            }
            if ((sensor.modality & 4) != 0) {
                modality |= 2;
            }
            if ((sensor.modality & 8) != 0) {
                modality |= 4;
            }
        }
        return modality;
    }

    private int sensorIdToModality(int sensorId) {
        for (BiometricSensor sensor : this.mPreAuthInfo.eligibleSensors) {
            if (sensorId == sensor.id) {
                return sensor.modality;
            }
        }
        Slog.e(TAG, "Unknown sensor: " + sensorId);
        return 0;
    }

    private String getAcquiredMessageForSensor(int sensorId, int acquiredInfo, int vendorCode) {
        int modality = sensorIdToModality(sensorId);
        switch (modality) {
            case 2:
                return FingerprintManager.getAcquiredString(this.mContext, acquiredInfo, vendorCode);
            case 8:
                return FaceManager.getAuthHelpMessage(this.mContext, acquiredInfo, vendorCode);
            default:
                return null;
        }
    }

    private static int getMultiSensorModeForNewSession(Collection<BiometricSensor> sensors) {
        boolean hasFace = false;
        boolean hasFingerprint = false;
        for (BiometricSensor sensor : sensors) {
            if (sensor.modality == 8) {
                hasFace = true;
            } else if (sensor.modality == 2) {
                hasFingerprint = true;
            }
        }
        if (hasFace && hasFingerprint) {
            return 1;
        }
        return 0;
    }

    public String toString() {
        return "State: " + this.mState + ", cancelled: " + this.mCancelled + ", isCrypto: " + isCrypto() + ", PreAuthInfo: " + this.mPreAuthInfo + ", requestId: " + this.mRequestId;
    }
}
