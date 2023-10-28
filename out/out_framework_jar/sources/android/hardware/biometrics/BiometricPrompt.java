package android.hardware.biometrics;

import android.content.Context;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.biometrics.IAuthService;
import android.hardware.biometrics.IBiometricServiceReceiver;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.security.identity.IdentityCredential;
import android.security.identity.PresentationSession;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.util.FrameworkStatsLog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.Signature;
import java.util.List;
import java.util.concurrent.Executor;
import javax.crypto.Cipher;
import javax.crypto.Mac;
/* loaded from: classes.dex */
public class BiometricPrompt implements BiometricAuthenticator, BiometricConstants {
    public static final int AUTHENTICATION_RESULT_TYPE_BIOMETRIC = 2;
    public static final int AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL = 1;
    public static final int DISMISSED_REASON_BIOMETRIC_CONFIRMED = 1;
    public static final int DISMISSED_REASON_BIOMETRIC_CONFIRM_NOT_REQUIRED = 4;
    public static final int DISMISSED_REASON_CREDENTIAL_CONFIRMED = 7;
    public static final int DISMISSED_REASON_ERROR = 5;
    public static final int DISMISSED_REASON_NEGATIVE = 2;
    public static final int DISMISSED_REASON_SERVER_REQUESTED = 6;
    public static final int DISMISSED_REASON_USER_CANCEL = 3;
    public static final int HIDE_DIALOG_DELAY = 2000;
    private static final String TAG = "BiometricPrompt";
    private AuthenticationCallback mAuthenticationCallback;
    private final IBiometricServiceReceiver mBiometricServiceReceiver;
    private final Context mContext;
    private CryptoObject mCryptoObject;
    private Executor mExecutor;
    private final ButtonInfo mNegativeButtonInfo;
    private final PromptInfo mPromptInfo;
    private final IAuthService mService;
    private final IBinder mToken;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface AuthenticationResultType {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface DismissedReason {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ButtonInfo {
        Executor executor;
        DialogInterface.OnClickListener listener;

        ButtonInfo(Executor ex, DialogInterface.OnClickListener l) {
            this.executor = ex;
            this.listener = l;
        }
    }

    /* loaded from: classes.dex */
    public static class Builder {
        private Context mContext;
        private ButtonInfo mNegativeButtonInfo;
        private PromptInfo mPromptInfo = new PromptInfo();

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setTitle(CharSequence title) {
            this.mPromptInfo.setTitle(title);
            return this;
        }

        public Builder setUseDefaultTitle() {
            this.mPromptInfo.setUseDefaultTitle(true);
            return this;
        }

        public Builder setSubtitle(CharSequence subtitle) {
            this.mPromptInfo.setSubtitle(subtitle);
            return this;
        }

        public Builder setDescription(CharSequence description) {
            this.mPromptInfo.setDescription(description);
            return this;
        }

        public Builder setTextForDeviceCredential(CharSequence title, CharSequence subtitle, CharSequence description) {
            if (title != null) {
                this.mPromptInfo.setDeviceCredentialTitle(title);
            }
            if (subtitle != null) {
                this.mPromptInfo.setDeviceCredentialSubtitle(subtitle);
            }
            if (description != null) {
                this.mPromptInfo.setDeviceCredentialDescription(description);
            }
            return this;
        }

        public Builder setNegativeButton(CharSequence text, Executor executor, DialogInterface.OnClickListener listener) {
            if (TextUtils.isEmpty(text)) {
                throw new IllegalArgumentException("Text must be set and non-empty");
            }
            if (executor == null) {
                throw new IllegalArgumentException("Executor must not be null");
            }
            if (listener == null) {
                throw new IllegalArgumentException("Listener must not be null");
            }
            this.mPromptInfo.setNegativeButtonText(text);
            this.mNegativeButtonInfo = new ButtonInfo(executor, listener);
            return this;
        }

        public Builder setConfirmationRequired(boolean requireConfirmation) {
            this.mPromptInfo.setConfirmationRequested(requireConfirmation);
            return this;
        }

        @Deprecated
        public Builder setDeviceCredentialAllowed(boolean allowed) {
            this.mPromptInfo.setDeviceCredentialAllowed(allowed);
            return this;
        }

        public Builder setAllowedAuthenticators(int authenticators) {
            this.mPromptInfo.setAuthenticators(authenticators);
            return this;
        }

        public Builder setAllowedSensorIds(List<Integer> sensorIds) {
            this.mPromptInfo.setAllowedSensorIds(sensorIds);
            return this;
        }

        public Builder setAllowBackgroundAuthentication(boolean allow) {
            this.mPromptInfo.setAllowBackgroundAuthentication(allow);
            return this;
        }

        public Builder setDisallowBiometricsIfPolicyExists(boolean checkDevicePolicyManager) {
            this.mPromptInfo.setDisallowBiometricsIfPolicyExists(checkDevicePolicyManager);
            return this;
        }

        public Builder setReceiveSystemEvents(boolean set) {
            this.mPromptInfo.setReceiveSystemEvents(set);
            return this;
        }

        public Builder setIgnoreEnrollmentState(boolean ignoreEnrollmentState) {
            this.mPromptInfo.setIgnoreEnrollmentState(ignoreEnrollmentState);
            return this;
        }

        public Builder setIsForLegacyFingerprintManager(int sensorId) {
            this.mPromptInfo.setIsForLegacyFingerprintManager(sensorId);
            return this;
        }

        public BiometricPrompt build() {
            CharSequence title = this.mPromptInfo.getTitle();
            CharSequence negative = this.mPromptInfo.getNegativeButtonText();
            boolean useDefaultTitle = this.mPromptInfo.isUseDefaultTitle();
            boolean deviceCredentialAllowed = this.mPromptInfo.isDeviceCredentialAllowed();
            int authenticators = this.mPromptInfo.getAuthenticators();
            boolean willShowDeviceCredentialButton = deviceCredentialAllowed || BiometricPrompt.isCredentialAllowed(authenticators);
            if (TextUtils.isEmpty(title) && !useDefaultTitle) {
                throw new IllegalArgumentException("Title must be set and non-empty");
            }
            if (TextUtils.isEmpty(negative) && !willShowDeviceCredentialButton) {
                throw new IllegalArgumentException("Negative text must be set and non-empty");
            }
            if (!TextUtils.isEmpty(negative) && willShowDeviceCredentialButton) {
                throw new IllegalArgumentException("Can't have both negative button behavior and device credential enabled");
            }
            return new BiometricPrompt(this.mContext, this.mPromptInfo, this.mNegativeButtonInfo);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class OnAuthenticationCancelListener implements CancellationSignal.OnCancelListener {
        private final long mAuthRequestId;

        OnAuthenticationCancelListener(long id) {
            this.mAuthRequestId = id;
        }

        @Override // android.os.CancellationSignal.OnCancelListener
        public void onCancel() {
            Log.d(BiometricPrompt.TAG, "Cancel BP authentication requested for: " + this.mAuthRequestId);
            BiometricPrompt.this.cancelAuthentication(this.mAuthRequestId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.hardware.biometrics.BiometricPrompt$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends IBiometricServiceReceiver.Stub {
        AnonymousClass1() {
        }

        @Override // android.hardware.biometrics.IBiometricServiceReceiver
        public void onAuthenticationSucceeded(final int authenticationType) {
            BiometricPrompt.this.mExecutor.execute(new Runnable() { // from class: android.hardware.biometrics.BiometricPrompt$1$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricPrompt.AnonymousClass1.this.m1266x6ef1fb33(authenticationType);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticationSucceeded$0$android-hardware-biometrics-BiometricPrompt$1  reason: not valid java name */
        public /* synthetic */ void m1266x6ef1fb33(int authenticationType) {
            AuthenticationResult result = new AuthenticationResult(BiometricPrompt.this.mCryptoObject, authenticationType);
            BiometricPrompt.this.mAuthenticationCallback.onAuthenticationSucceeded(result);
        }

        @Override // android.hardware.biometrics.IBiometricServiceReceiver
        public void onAuthenticationFailed() {
            BiometricPrompt.this.mExecutor.execute(new Runnable() { // from class: android.hardware.biometrics.BiometricPrompt$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricPrompt.AnonymousClass1.this.m1265x349588f8();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticationFailed$1$android-hardware-biometrics-BiometricPrompt$1  reason: not valid java name */
        public /* synthetic */ void m1265x349588f8() {
            BiometricPrompt.this.mAuthenticationCallback.onAuthenticationFailed();
        }

        @Override // android.hardware.biometrics.IBiometricServiceReceiver
        public void onError(int modality, final int error, int vendorCode) {
            String errorMessage = null;
            switch (modality) {
                case 2:
                    errorMessage = FingerprintManager.getErrorString(BiometricPrompt.this.mContext, error, vendorCode);
                    break;
                case 8:
                    errorMessage = FaceManager.getErrorString(BiometricPrompt.this.mContext, error, vendorCode);
                    break;
            }
            if (errorMessage == null) {
                switch (error) {
                    case 5:
                        errorMessage = BiometricPrompt.this.mContext.getString(R.string.biometric_error_canceled);
                        break;
                    case 10:
                        errorMessage = BiometricPrompt.this.mContext.getString(R.string.biometric_error_user_canceled);
                        break;
                    case 12:
                        errorMessage = BiometricPrompt.this.mContext.getString(R.string.biometric_error_hw_unavailable);
                        break;
                    case 14:
                        errorMessage = BiometricPrompt.this.mContext.getString(R.string.biometric_error_device_not_secured);
                        break;
                    default:
                        Log.e(BiometricPrompt.TAG, "Unknown error, modality: " + modality + " error: " + error + " vendorCode: " + vendorCode);
                        errorMessage = BiometricPrompt.this.mContext.getString(R.string.biometric_error_generic);
                        break;
                }
            }
            final String stringToSend = errorMessage;
            BiometricPrompt.this.mExecutor.execute(new Runnable() { // from class: android.hardware.biometrics.BiometricPrompt$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricPrompt.AnonymousClass1.this.m1268lambda$onError$2$androidhardwarebiometricsBiometricPrompt$1(error, stringToSend);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onError$2$android-hardware-biometrics-BiometricPrompt$1  reason: not valid java name */
        public /* synthetic */ void m1268lambda$onError$2$androidhardwarebiometricsBiometricPrompt$1(int error, String stringToSend) {
            BiometricPrompt.this.mAuthenticationCallback.onAuthenticationError(error, stringToSend);
        }

        @Override // android.hardware.biometrics.IBiometricServiceReceiver
        public void onAcquired(final int acquireInfo, final String message) {
            BiometricPrompt.this.mExecutor.execute(new Runnable() { // from class: android.hardware.biometrics.BiometricPrompt$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricPrompt.AnonymousClass1.this.m1264x1c20caf3(acquireInfo, message);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAcquired$3$android-hardware-biometrics-BiometricPrompt$1  reason: not valid java name */
        public /* synthetic */ void m1264x1c20caf3(int acquireInfo, String message) {
            BiometricPrompt.this.mAuthenticationCallback.onAuthenticationHelp(acquireInfo, message);
        }

        @Override // android.hardware.biometrics.IBiometricServiceReceiver
        public void onDialogDismissed(int reason) {
            if (reason == 2) {
                BiometricPrompt.this.mNegativeButtonInfo.executor.execute(new Runnable() { // from class: android.hardware.biometrics.BiometricPrompt$1$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        BiometricPrompt.AnonymousClass1.this.m1267x163919af();
                    }
                });
            } else {
                Log.e(BiometricPrompt.TAG, "Unknown reason: " + reason);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDialogDismissed$4$android-hardware-biometrics-BiometricPrompt$1  reason: not valid java name */
        public /* synthetic */ void m1267x163919af() {
            BiometricPrompt.this.mNegativeButtonInfo.listener.onClick(null, -2);
        }

        @Override // android.hardware.biometrics.IBiometricServiceReceiver
        public void onSystemEvent(final int event) {
            BiometricPrompt.this.mExecutor.execute(new Runnable() { // from class: android.hardware.biometrics.BiometricPrompt$1$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricPrompt.AnonymousClass1.this.m1269x4a2a235a(event);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSystemEvent$5$android-hardware-biometrics-BiometricPrompt$1  reason: not valid java name */
        public /* synthetic */ void m1269x4a2a235a(int event) {
            BiometricPrompt.this.mAuthenticationCallback.onSystemEvent(event);
        }
    }

    private BiometricPrompt(Context context, PromptInfo promptInfo, ButtonInfo negativeButtonInfo) {
        this.mToken = new Binder();
        this.mBiometricServiceReceiver = new AnonymousClass1();
        this.mContext = context;
        this.mPromptInfo = promptInfo;
        this.mNegativeButtonInfo = negativeButtonInfo;
        this.mService = IAuthService.Stub.asInterface(ServiceManager.getService(Context.AUTH_SERVICE));
    }

    public CharSequence getTitle() {
        return this.mPromptInfo.getTitle();
    }

    public boolean shouldUseDefaultTitle() {
        return this.mPromptInfo.isUseDefaultTitle();
    }

    public CharSequence getSubtitle() {
        return this.mPromptInfo.getSubtitle();
    }

    public CharSequence getDescription() {
        return this.mPromptInfo.getDescription();
    }

    public CharSequence getNegativeButtonText() {
        return this.mPromptInfo.getNegativeButtonText();
    }

    public boolean isConfirmationRequired() {
        return this.mPromptInfo.isConfirmationRequested();
    }

    public int getAllowedAuthenticators() {
        return this.mPromptInfo.getAuthenticators();
    }

    public List<Integer> getAllowedSensorIds() {
        return this.mPromptInfo.getAllowedSensorIds();
    }

    public boolean isAllowBackgroundAuthentication() {
        return this.mPromptInfo.isAllowBackgroundAuthentication();
    }

    /* loaded from: classes.dex */
    public static final class CryptoObject extends android.hardware.biometrics.CryptoObject {
        public CryptoObject(Signature signature) {
            super(signature);
        }

        public CryptoObject(Cipher cipher) {
            super(cipher);
        }

        public CryptoObject(Mac mac) {
            super(mac);
        }

        @Deprecated
        public CryptoObject(IdentityCredential credential) {
            super(credential);
        }

        public CryptoObject(PresentationSession session) {
            super(session);
        }

        @Override // android.hardware.biometrics.CryptoObject
        public Signature getSignature() {
            return super.getSignature();
        }

        @Override // android.hardware.biometrics.CryptoObject
        public Cipher getCipher() {
            return super.getCipher();
        }

        @Override // android.hardware.biometrics.CryptoObject
        public Mac getMac() {
            return super.getMac();
        }

        @Override // android.hardware.biometrics.CryptoObject
        @Deprecated
        public IdentityCredential getIdentityCredential() {
            return super.getIdentityCredential();
        }

        @Override // android.hardware.biometrics.CryptoObject
        public PresentationSession getPresentationSession() {
            return super.getPresentationSession();
        }
    }

    /* loaded from: classes.dex */
    public static class AuthenticationResult extends BiometricAuthenticator.AuthenticationResult {
        public AuthenticationResult(CryptoObject crypto, int authenticationType) {
            super(crypto, authenticationType, null, 0);
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.hardware.biometrics.BiometricAuthenticator.AuthenticationResult
        public CryptoObject getCryptoObject() {
            return (CryptoObject) super.getCryptoObject();
        }

        @Override // android.hardware.biometrics.BiometricAuthenticator.AuthenticationResult
        public int getAuthenticationType() {
            return super.getAuthenticationType();
        }
    }

    /* loaded from: classes.dex */
    public static abstract class AuthenticationCallback extends BiometricAuthenticator.AuthenticationCallback {
        @Override // android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback
        public void onAuthenticationError(int errorCode, CharSequence errString) {
        }

        @Override // android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        }

        public void onAuthenticationSucceeded(AuthenticationResult result) {
        }

        @Override // android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback
        public void onAuthenticationFailed() {
        }

        @Override // android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback
        public void onAuthenticationAcquired(int acquireInfo) {
        }

        public void onSystemEvent(int event) {
        }
    }

    public void authenticateUser(CancellationSignal cancel, Executor executor, AuthenticationCallback callback, int userId) {
        if (cancel == null) {
            throw new IllegalArgumentException("Must supply a cancellation signal");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must supply an executor");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Must supply a callback");
        }
        authenticateInternal(0L, cancel, executor, callback, userId);
    }

    public long authenticateForOperation(CancellationSignal cancel, Executor executor, AuthenticationCallback callback, long operationId) {
        if (cancel == null) {
            throw new IllegalArgumentException("Must supply a cancellation signal");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must supply an executor");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Must supply a callback");
        }
        return authenticateInternal(operationId, cancel, executor, callback, this.mContext.getUserId());
    }

    public void authenticate(CryptoObject crypto, CancellationSignal cancel, Executor executor, AuthenticationCallback callback) {
        FrameworkStatsLog.write(353, true, this.mPromptInfo.isConfirmationRequested(), this.mPromptInfo.isDeviceCredentialAllowed(), this.mPromptInfo.getAuthenticators() != 0, this.mPromptInfo.getAuthenticators());
        if (crypto == null) {
            throw new IllegalArgumentException("Must supply a crypto object");
        }
        if (cancel == null) {
            throw new IllegalArgumentException("Must supply a cancellation signal");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must supply an executor");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Must supply a callback");
        }
        int authenticators = this.mPromptInfo.getAuthenticators();
        if (authenticators == 0) {
            authenticators = 15;
        }
        int biometricStrength = authenticators & 255;
        if ((biometricStrength & (-16)) != 0) {
            throw new IllegalArgumentException("Only Strong biometrics supported with crypto");
        }
        authenticateInternal(crypto, cancel, executor, callback, this.mContext.getUserId());
    }

    public void authenticate(CancellationSignal cancel, Executor executor, AuthenticationCallback callback) {
        FrameworkStatsLog.write(353, false, this.mPromptInfo.isConfirmationRequested(), this.mPromptInfo.isDeviceCredentialAllowed(), this.mPromptInfo.getAuthenticators() != 0, this.mPromptInfo.getAuthenticators());
        if (cancel == null) {
            throw new IllegalArgumentException("Must supply a cancellation signal");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Must supply an executor");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Must supply a callback");
        }
        authenticateInternal((CryptoObject) null, cancel, executor, callback, this.mContext.getUserId());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelAuthentication(long requestId) {
        IAuthService iAuthService = this.mService;
        if (iAuthService != null) {
            try {
                iAuthService.cancelAuthentication(this.mToken, this.mContext.getPackageName(), requestId);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to cancel authentication", e);
            }
        }
    }

    private void authenticateInternal(CryptoObject crypto, CancellationSignal cancel, Executor executor, AuthenticationCallback callback, int userId) {
        this.mCryptoObject = crypto;
        long operationId = crypto != null ? crypto.getOpId() : 0L;
        authenticateInternal(operationId, cancel, executor, callback, userId);
    }

    private long authenticateInternal(long operationId, CancellationSignal cancel, Executor executor, final AuthenticationCallback callback, int userId) {
        PromptInfo promptInfo;
        CryptoObject cryptoObject = this.mCryptoObject;
        if (cryptoObject != null && cryptoObject.getOpId() != operationId) {
            Log.w(TAG, "CryptoObject operation ID does not match argument; setting field to null");
            this.mCryptoObject = null;
        }
        try {
            if (!cancel.isCanceled()) {
                try {
                    this.mExecutor = executor;
                    this.mAuthenticationCallback = callback;
                    if (operationId != 0) {
                        Parcel parcel = Parcel.obtain();
                        this.mPromptInfo.writeToParcel(parcel, 0);
                        parcel.setDataPosition(0);
                        PromptInfo promptInfo2 = new PromptInfo(parcel);
                        if (promptInfo2.getAuthenticators() == 0) {
                            promptInfo2.setAuthenticators(15);
                        }
                        promptInfo = promptInfo2;
                    } else {
                        promptInfo = this.mPromptInfo;
                    }
                    long authId = this.mService.authenticate(this.mToken, operationId, userId, this.mBiometricServiceReceiver, this.mContext.getPackageName(), promptInfo);
                    try {
                        cancel.setOnCancelListener(new OnAuthenticationCancelListener(authId));
                        return authId;
                    } catch (RemoteException e) {
                        e = e;
                        Log.e(TAG, "Remote exception while authenticating", e);
                        this.mExecutor.execute(new Runnable() { // from class: android.hardware.biometrics.BiometricPrompt$$ExternalSyntheticLambda0
                            @Override // java.lang.Runnable
                            public final void run() {
                                BiometricPrompt.this.m1263x18619ec8(callback);
                            }
                        });
                        return -1L;
                    }
                } catch (RemoteException e2) {
                    e = e2;
                }
            } else {
                Log.w(TAG, "Authentication already canceled");
                return -1L;
            }
        } catch (RemoteException e3) {
            e = e3;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$authenticateInternal$0$android-hardware-biometrics-BiometricPrompt  reason: not valid java name */
    public /* synthetic */ void m1263x18619ec8(AuthenticationCallback callback) {
        callback.onAuthenticationError(1, this.mContext.getString(R.string.biometric_error_hw_unavailable));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isCredentialAllowed(int allowedAuthenticators) {
        return (32768 & allowedAuthenticators) != 0;
    }
}
