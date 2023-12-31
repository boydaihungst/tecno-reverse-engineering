package android.hardware.fingerprint;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricFingerprintConstants;
import android.hardware.biometrics.BiometricStateListener;
import android.hardware.biometrics.BiometricTestSession;
import android.hardware.biometrics.IBiometricServiceLockoutResetCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.security.identity.IdentityCredential;
import android.security.identity.PresentationSession;
import android.util.Slog;
import com.android.internal.R;
import com.android.internal.util.FrameworkStatsLog;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.Mac;
@Deprecated
/* loaded from: classes.dex */
public class FingerprintManager implements BiometricAuthenticator, BiometricFingerprintConstants {
    private static final boolean DEBUG = true;
    public static final int ENROLL_ENROLL = 2;
    public static final int ENROLL_FIND_SENSOR = 1;
    public static final int FINGERPRINT_ACQUIRED_BAD_IMAGE = 1008;
    public static final int FINGERPRINT_ACQUIRED_DUPLICATE_AREA = 1006;
    public static final int FINGERPRINT_ACQUIRED_DUPLICATE_FINGER = 1005;
    public static final int FINGERPRINT_ACQUIRED_FINGER_DOWN = 1002;
    public static final int FINGERPRINT_ACQUIRED_FINGER_UP = 1003;
    public static final int FINGERPRINT_ACQUIRED_LOW_COVER = 1007;
    private static final int MSG_ACQUIRED = 101;
    private static final int MSG_AUTHENTICATION_FAILED = 103;
    private static final int MSG_AUTHENTICATION_SUCCEEDED = 102;
    private static final int MSG_CHALLENGE_GENERATED = 106;
    private static final int MSG_ENROLL_RESULT = 100;
    private static final int MSG_ERROR = 104;
    private static final int MSG_FINGERPRINT_DETECTED = 107;
    private static final int MSG_REMOVED = 105;
    private static final int MSG_UDFPS_POINTER_DOWN = 108;
    private static final int MSG_UDFPS_POINTER_UP = 109;
    private static final boolean OPTICAL_FINGERPRINT = "1".equals(SystemProperties.get("ro.optical_fingerprint_support", ""));
    public static final int SENSOR_ID_ANY = -1;
    private static final String TAG = "FingerprintManager";
    private AuthenticationCallback mAuthenticationCallback;
    private Context mContext;
    private CryptoObject mCryptoObject;
    private float[] mEnrollStageThresholds;
    private EnrollmentCallback mEnrollmentCallback;
    private FingerprintDetectionCallback mFingerprintDetectionCallback;
    private GenerateChallengeCallback mGenerateChallengeCallback;
    private Handler mHandler;
    private RemovalCallback mRemovalCallback;
    private RemoveTracker mRemoveTracker;
    private IFingerprintService mService;
    private IBinder mToken = new Binder();
    private boolean mCurrentNightDisplay = false;
    private IFingerprintServiceReceiver mServiceReceiver = new IFingerprintServiceReceiver.Stub() { // from class: android.hardware.fingerprint.FingerprintManager.2
        @Override // android.hardware.fingerprint.IFingerprintServiceReceiver
        public void onEnrollResult(Fingerprint fp, int remaining) {
            FingerprintManager.this.mHandler.obtainMessage(100, remaining, 0, fp).sendToTarget();
        }

        @Override // android.hardware.fingerprint.IFingerprintServiceReceiver
        public void onAcquired(int acquireInfo, int vendorCode) {
            FingerprintManager.this.mHandler.obtainMessage(101, acquireInfo, vendorCode).sendToTarget();
        }

        @Override // android.hardware.fingerprint.IFingerprintServiceReceiver
        public void onAuthenticationSucceeded(Fingerprint fp, int userId, boolean isStrongBiometric) {
            FingerprintManager.this.mHandler.obtainMessage(102, userId, isStrongBiometric ? 1 : 0, fp).sendToTarget();
        }

        @Override // android.hardware.fingerprint.IFingerprintServiceReceiver
        public void onFingerprintDetected(int sensorId, int userId, boolean isStrongBiometric) {
            FingerprintManager.this.mHandler.obtainMessage(107, sensorId, userId, Boolean.valueOf(isStrongBiometric)).sendToTarget();
        }

        @Override // android.hardware.fingerprint.IFingerprintServiceReceiver
        public void onAuthenticationFailed() {
            FingerprintManager.this.mHandler.obtainMessage(103).sendToTarget();
        }

        @Override // android.hardware.fingerprint.IFingerprintServiceReceiver
        public void onError(int error, int vendorCode) {
            FingerprintManager.this.mHandler.obtainMessage(104, error, vendorCode).sendToTarget();
        }

        @Override // android.hardware.fingerprint.IFingerprintServiceReceiver
        public void onRemoved(Fingerprint fp, int remaining) {
            FingerprintManager.this.mHandler.obtainMessage(105, remaining, 0, fp).sendToTarget();
        }

        @Override // android.hardware.fingerprint.IFingerprintServiceReceiver
        public void onChallengeGenerated(int sensorId, int userId, long challenge) {
            FingerprintManager.this.mHandler.obtainMessage(106, sensorId, userId, Long.valueOf(challenge)).sendToTarget();
        }

        @Override // android.hardware.fingerprint.IFingerprintServiceReceiver
        public void onUdfpsPointerDown(int sensorId) {
            FingerprintManager.this.mHandler.obtainMessage(108, sensorId, 0).sendToTarget();
        }

        @Override // android.hardware.fingerprint.IFingerprintServiceReceiver
        public void onUdfpsPointerUp(int sensorId) {
            FingerprintManager.this.mHandler.obtainMessage(109, sensorId, 0).sendToTarget();
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface EnrollReason {
    }

    /* loaded from: classes.dex */
    public interface FingerprintDetectionCallback {
        void onFingerprintDetected(int i, int i2, boolean z);
    }

    /* loaded from: classes.dex */
    public interface GenerateChallengeCallback {
        void onChallengeGenerated(int i, int i2, long j);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class RemoveTracker {
        static final int REMOVE_ALL = 2;
        static final int REMOVE_SINGLE = 1;
        final int mRemoveRequest;
        final Fingerprint mSingleFingerprint;

        /* loaded from: classes.dex */
        @interface RemoveRequest {
        }

        RemoveTracker(int request, Fingerprint fingerprint) {
            this.mRemoveRequest = request;
            this.mSingleFingerprint = fingerprint;
        }
    }

    public List<SensorProperties> getSensorProperties() {
        List<SensorProperties> properties = new ArrayList<>();
        List<FingerprintSensorPropertiesInternal> internalProperties = getSensorPropertiesInternal();
        for (FingerprintSensorPropertiesInternal internalProp : internalProperties) {
            properties.add(FingerprintSensorProperties.from(internalProp));
        }
        return properties;
    }

    public BiometricTestSession createTestSession(int sensorId) {
        try {
            return new BiometricTestSession(this.mContext, sensorId, new BiometricTestSession.TestSessionProvider() { // from class: android.hardware.fingerprint.FingerprintManager$$ExternalSyntheticLambda0
                @Override // android.hardware.biometrics.BiometricTestSession.TestSessionProvider
                public final ITestSession createTestSession(Context context, int i, ITestSessionCallback iTestSessionCallback) {
                    return FingerprintManager.this.m1555x1235338c(context, i, iTestSessionCallback);
                }
            });
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createTestSession$0$android-hardware-fingerprint-FingerprintManager  reason: not valid java name */
    public /* synthetic */ ITestSession m1555x1235338c(Context context, int sensorId1, ITestSessionCallback callback) throws RemoteException {
        return this.mService.createTestSession(sensorId1, callback, context.getOpPackageName());
    }

    /* loaded from: classes.dex */
    private class OnEnrollCancelListener implements CancellationSignal.OnCancelListener {
        private final long mAuthRequestId;

        private OnEnrollCancelListener(long id) {
            this.mAuthRequestId = id;
        }

        @Override // android.os.CancellationSignal.OnCancelListener
        public void onCancel() {
            Slog.d(FingerprintManager.TAG, "Cancel fingerprint enrollment requested for: " + this.mAuthRequestId);
            FingerprintManager.this.cancelEnrollment(this.mAuthRequestId);
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
            Slog.d(FingerprintManager.TAG, "Cancel fingerprint authentication requested for: " + this.mAuthRequestId);
            FingerprintManager.this.cancelAuthentication(this.mAuthRequestId);
        }
    }

    /* loaded from: classes.dex */
    private class OnFingerprintDetectionCancelListener implements CancellationSignal.OnCancelListener {
        private final long mAuthRequestId;

        OnFingerprintDetectionCancelListener(long id) {
            this.mAuthRequestId = id;
        }

        @Override // android.os.CancellationSignal.OnCancelListener
        public void onCancel() {
            Slog.d(FingerprintManager.TAG, "Cancel fingerprint detect requested for: " + this.mAuthRequestId);
            FingerprintManager.this.cancelFingerprintDetect(this.mAuthRequestId);
        }
    }

    @Deprecated
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

    @Deprecated
    /* loaded from: classes.dex */
    public static class AuthenticationResult {
        private CryptoObject mCryptoObject;
        private Fingerprint mFingerprint;
        private boolean mIsStrongBiometric;
        private int mUserId;

        public AuthenticationResult(CryptoObject crypto, Fingerprint fingerprint, int userId, boolean isStrongBiometric) {
            this.mCryptoObject = crypto;
            this.mFingerprint = fingerprint;
            this.mUserId = userId;
            this.mIsStrongBiometric = isStrongBiometric;
        }

        public CryptoObject getCryptoObject() {
            return this.mCryptoObject;
        }

        public Fingerprint getFingerprint() {
            return this.mFingerprint;
        }

        public int getUserId() {
            return this.mUserId;
        }

        public boolean isStrongBiometric() {
            return this.mIsStrongBiometric;
        }
    }

    @Deprecated
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

        public void onUdfpsPointerDown(int sensorId) {
        }

        public void onUdfpsPointerUp(int sensorId) {
        }
    }

    /* loaded from: classes.dex */
    public static abstract class EnrollmentCallback {
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
        }

        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        }

        public void onEnrollmentProgress(int remaining) {
        }
    }

    /* loaded from: classes.dex */
    public static abstract class RemovalCallback {
        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
        }

        public void onRemovalSucceeded(Fingerprint fp, int remaining) {
        }
    }

    /* loaded from: classes.dex */
    public static abstract class LockoutResetCallback {
        public void onLockoutReset(int sensorId) {
        }
    }

    private void useHandler(Handler handler) {
        if (handler != null) {
            this.mHandler = new MyHandler(handler.getLooper());
        } else if (this.mHandler.getLooper() != this.mContext.getMainLooper()) {
            this.mHandler = new MyHandler(this.mContext.getMainLooper());
        }
    }

    @Deprecated
    public void authenticate(CryptoObject crypto, CancellationSignal cancel, int flags, AuthenticationCallback callback, Handler handler) {
        authenticate(crypto, cancel, callback, handler, -1, this.mContext.getUserId(), flags);
    }

    public void authenticate(CryptoObject crypto, CancellationSignal cancel, AuthenticationCallback callback, Handler handler, int userId) {
        authenticate(crypto, cancel, callback, handler, -1, userId, 0);
    }

    public void authenticate(CryptoObject crypto, CancellationSignal cancel, AuthenticationCallback callback, Handler handler, int sensorId, int userId, int flags) {
        FrameworkStatsLog.write(356, 1, this.mContext.getApplicationInfo().uid, this.mContext.getApplicationInfo().targetSdkVersion);
        if (callback == null) {
            throw new IllegalArgumentException("Must supply an authentication callback");
        }
        if (cancel != null && cancel.isCanceled()) {
            Slog.w(TAG, "authentication already canceled");
            return;
        }
        boolean ignoreEnrollmentState = flags != 0;
        if (this.mService != null) {
            try {
                useHandler(handler);
                this.mAuthenticationCallback = callback;
                this.mCryptoObject = crypto;
                long operationId = crypto != null ? crypto.getOpId() : 0L;
                long authId = this.mService.authenticate(this.mToken, operationId, sensorId, userId, this.mServiceReceiver, this.mContext.getOpPackageName(), this.mContext.getAttributionTag(), ignoreEnrollmentState);
                if (cancel != null) {
                    cancel.setOnCancelListener(new OnAuthenticationCancelListener(authId));
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception while authenticating: ", e);
                callback.onAuthenticationError(1, getErrorString(this.mContext, 1, 0));
            }
        }
    }

    public void detectFingerprint(CancellationSignal cancel, FingerprintDetectionCallback callback, int userId) {
        if (this.mService == null) {
            return;
        }
        if (cancel.isCanceled()) {
            Slog.w(TAG, "Detection already cancelled");
            return;
        }
        this.mFingerprintDetectionCallback = callback;
        try {
            long authId = this.mService.detectFingerprint(this.mToken, userId, this.mServiceReceiver, this.mContext.getOpPackageName());
            cancel.setOnCancelListener(new OnFingerprintDetectionCancelListener(authId));
        } catch (RemoteException e) {
            Slog.w(TAG, "Remote exception when requesting finger detect", e);
        }
    }

    public void enroll(byte[] hardwareAuthToken, CancellationSignal cancel, int userId, EnrollmentCallback callback, int enrollReason) {
        if (userId == -2) {
            userId = getCurrentUserId();
        }
        if (callback == null) {
            throw new IllegalArgumentException("Must supply an enrollment callback");
        }
        if (cancel != null && cancel.isCanceled()) {
            Slog.w(TAG, "enrollment already canceled");
            return;
        }
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                this.mEnrollmentCallback = callback;
                long enrollId = iFingerprintService.enroll(this.mToken, hardwareAuthToken, userId, this.mServiceReceiver, this.mContext.getOpPackageName(), enrollReason);
                if (cancel != null) {
                    cancel.setOnCancelListener(new OnEnrollCancelListener(enrollId));
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception in enroll: ", e);
                callback.onEnrollmentError(1, getErrorString(this.mContext, 1, 0));
            }
        }
    }

    public void generateChallenge(int sensorId, int userId, GenerateChallengeCallback callback) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                this.mGenerateChallengeCallback = callback;
                iFingerprintService.generateChallenge(this.mToken, sensorId, userId, this.mServiceReceiver, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void generateChallenge(int userId, GenerateChallengeCallback callback) {
        FingerprintSensorPropertiesInternal sensorProps = getFirstFingerprintSensor();
        if (sensorProps == null) {
            Slog.e(TAG, "No sensors");
        } else {
            generateChallenge(sensorProps.sensorId, userId, callback);
        }
    }

    public void revokeChallenge(int userId, long challenge) {
        if (this.mService != null) {
            try {
                FingerprintSensorPropertiesInternal sensorProps = getFirstFingerprintSensor();
                if (sensorProps == null) {
                    Slog.e(TAG, "No sensors");
                } else {
                    this.mService.revokeChallenge(this.mToken, sensorProps.sensorId, userId, this.mContext.getOpPackageName(), challenge);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void resetLockout(int sensorId, int userId, byte[] hardwareAuthToken) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.resetLockout(this.mToken, sensorId, userId, hardwareAuthToken, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void remove(Fingerprint fp, int userId, RemovalCallback callback) {
        if (this.mService != null) {
            try {
                this.mRemovalCallback = callback;
                this.mRemoveTracker = new RemoveTracker(1, fp);
                this.mService.remove(this.mToken, fp.getBiometricId(), userId, this.mServiceReceiver, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void removeAll(int userId, RemovalCallback callback) {
        if (this.mService != null) {
            try {
                this.mRemovalCallback = callback;
                this.mRemoveTracker = new RemoveTracker(2, null);
                this.mService.removeAll(this.mToken, userId, this.mServiceReceiver, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void removeAll(int userId, RemovalCallback callback, Handler handler) {
        if (this.mService != null) {
            try {
                useHandler(handler);
                this.mRemovalCallback = callback;
                this.mRemoveTracker = new RemoveTracker(2, null);
                this.mService.removeAll(this.mToken, userId, this.mServiceReceiver, this.mContext.getOpPackageName());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void rename(int fpId, int userId, String newName) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.rename(fpId, userId, newName);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "rename(): Service not connected!");
    }

    public void setAppBiometrics(int fpId, String packageName) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.setAppBiometrics(fpId, getCurrentUserId(), packageName, getCurrentUserId());
                return;
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in rename(): ", e);
                return;
            }
        }
        Slog.w(TAG, "rename(): Service not connected!");
    }

    public String getAppPackagename(int fpId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.getAppPackagename(fpId, getCurrentUserId(), this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in rename(): ", e);
                return null;
            }
        }
        Slog.w(TAG, "rename(): Service not connected!");
        return null;
    }

    public boolean hasAppPackagename() {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.hasAppPackagename(getCurrentUserId(), this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in hasAppPackagename: ", e);
                return false;
            }
        }
        return false;
    }

    public Fingerprint getAddFingerprint(int userId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.getAddFingerprint(userId, this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in getAddFingerprint: ", e);
                return null;
            }
        }
        return null;
    }

    public boolean checkName(String name, int userId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.checkName(userId, this.mContext.getOpPackageName(), name, this.mContext.getAttributionTag());
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in checkName: ", e);
                return false;
            }
        }
        return false;
    }

    public void startAppForFp(int fpId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.startAppForFp(getCurrentUserId(), fpId);
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in startAppForFp: ", e);
            }
        }
    }

    public void notifyAppResumeForFp(String packagename, String name) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.notifyAppResumeForFp(getCurrentUserId(), packagename, name);
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in notifyAppResumeForFp: ", e);
            }
        }
    }

    public void notifyAppPauseForFp(String packagename, String name) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.notifyAppPauseForFp(getCurrentUserId(), packagename, name);
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in notifyAppPauseForFp: ", e);
            }
        }
    }

    public void setAppAndUserIdForBiometrics(int fpId, String packageName, int userId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.setAppBiometrics(fpId, getCurrentUserId(), packageName, userId);
                return;
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in rename(): ", e);
                return;
            }
        }
        Slog.w(TAG, "rename(): Service not connected!");
    }

    public int getAppUserId(int fpId) {
        int userId = getCurrentUserId();
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.getAppUserId(fpId, userId);
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in getAppUserId(): ", e);
                return userId;
            }
        }
        Slog.w(TAG, "getAppUserId(): Service not connected!");
        return userId;
    }

    public void onUpdateFocusedApp(String oldPackageName, ComponentName oldComponent, String newPackageName, ComponentName newComponent) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.onUpdateFocusedApp(oldPackageName, oldComponent, newPackageName, newComponent);
                return;
            } catch (RemoteException e) {
                Slog.v(TAG, "Remote exception in onUpdateFocusedApp: ", e);
                return;
            }
        }
        Slog.w(TAG, "onUpdateFocusedApp: Service not connected!");
    }

    public List<Fingerprint> getEnrolledFingerprints(int userId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.getEnrolledFingerprints(userId, this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return null;
    }

    public List<Fingerprint> getEnrolledFingerprints() {
        return getEnrolledFingerprints(this.mContext.getUserId());
    }

    public boolean hasEnrolledTemplates() {
        return hasEnrolledFingerprints();
    }

    public boolean hasEnrolledTemplates(int userId) {
        return hasEnrolledFingerprints(userId);
    }

    public void setUdfpsOverlayController(IUdfpsOverlayController controller) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService == null) {
            Slog.w(TAG, "setUdfpsOverlayController: no fingerprint service");
            return;
        }
        try {
            iFingerprintService.setUdfpsOverlayController(controller);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setSidefpsController(ISidefpsController controller) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService == null) {
            Slog.w(TAG, "setSidefpsController: no fingerprint service");
            return;
        }
        try {
            iFingerprintService.setSidefpsController(controller);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerBiometricStateListener(BiometricStateListener listener) {
        try {
            this.mService.registerBiometricStateListener(listener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void onPointerDown(long requestId, int sensorId, int x, int y, float minor, float major) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService == null) {
            Slog.w(TAG, "onFingerDown: no fingerprint service");
            return;
        }
        try {
            iFingerprintService.onPointerDown(requestId, sensorId, x, y, minor, major);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void onPointerUp(long requestId, int sensorId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService == null) {
            Slog.w(TAG, "onFingerDown: no fingerprint service");
            return;
        }
        try {
            iFingerprintService.onPointerUp(requestId, sensorId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void onUiReady(long requestId, int sensorId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService == null) {
            Slog.w(TAG, "onUiReady: no fingerprint service");
            return;
        }
        try {
            iFingerprintService.onUiReady(requestId, sensorId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean hasEnrolledFingerprints() {
        FrameworkStatsLog.write(356, 2, this.mContext.getApplicationInfo().uid, this.mContext.getApplicationInfo().targetSdkVersion);
        return hasEnrolledFingerprints(UserHandle.myUserId());
    }

    public boolean hasEnrolledFingerprints(int userId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.hasEnrolledFingerprintsDeprecated(userId, this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        return false;
    }

    @Deprecated
    public boolean isHardwareDetected() {
        FrameworkStatsLog.write(356, 3, this.mContext.getApplicationInfo().uid, this.mContext.getApplicationInfo().targetSdkVersion);
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.isHardwareDetectedDeprecated(this.mContext.getOpPackageName(), this.mContext.getAttributionTag());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "isFingerprintHardwareDetected(): Service not connected!");
        return false;
    }

    public List<FingerprintSensorPropertiesInternal> getSensorPropertiesInternal() {
        try {
            IFingerprintService iFingerprintService = this.mService;
            if (iFingerprintService == null) {
                return new ArrayList();
            }
            return iFingerprintService.getSensorPropertiesInternal(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isPowerbuttonFps() {
        FingerprintSensorPropertiesInternal sensorProps = getFirstFingerprintSensor();
        return sensorProps.sensorType == 4;
    }

    public void addAuthenticatorsRegisteredCallback(IFingerprintAuthenticatorsRegisteredCallback callback) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.addAuthenticatorsRegisteredCallback(callback);
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "addProvidersAvailableCallback(): Service not connected!");
    }

    public int getLockoutModeForUser(int sensorId, int userId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.getLockoutModeForUser(sensorId, userId);
            } catch (RemoteException e) {
                e.rethrowFromSystemServer();
                return 0;
            }
        }
        return 0;
    }

    public void addLockoutResetCallback(LockoutResetCallback callback) {
        if (this.mService != null) {
            try {
                PowerManager powerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
                this.mService.addLockoutResetCallback(new AnonymousClass1(powerManager, callback), this.mContext.getOpPackageName());
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Slog.w(TAG, "addLockoutResetCallback(): Service not connected!");
    }

    /* renamed from: android.hardware.fingerprint.FingerprintManager$1  reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass1 extends IBiometricServiceLockoutResetCallback.Stub {
        final /* synthetic */ LockoutResetCallback val$callback;
        final /* synthetic */ PowerManager val$powerManager;

        AnonymousClass1(PowerManager powerManager, LockoutResetCallback lockoutResetCallback) {
            this.val$powerManager = powerManager;
            this.val$callback = lockoutResetCallback;
        }

        @Override // android.hardware.biometrics.IBiometricServiceLockoutResetCallback
        public void onLockoutReset(final int sensorId, IRemoteCallback serverCallback) throws RemoteException {
            try {
                final PowerManager.WakeLock wakeLock = this.val$powerManager.newWakeLock(1, "lockoutResetCallback");
                wakeLock.acquire();
                Handler handler = FingerprintManager.this.mHandler;
                final LockoutResetCallback lockoutResetCallback = this.val$callback;
                handler.post(new Runnable() { // from class: android.hardware.fingerprint.FingerprintManager$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        FingerprintManager.AnonymousClass1.lambda$onLockoutReset$0(FingerprintManager.LockoutResetCallback.this, sensorId, wakeLock);
                    }
                });
            } finally {
                serverCallback.sendResult(null);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$onLockoutReset$0(LockoutResetCallback callback, int sensorId, PowerManager.WakeLock wakeLock) {
            try {
                callback.onLockoutReset(sensorId);
            } finally {
                wakeLock.release();
            }
        }
    }

    public void setKeyguardClientVisible(boolean visible) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.setKeyguardClientVisible(this.mContext.getOpPackageName(), visible);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception while setKeyguardClientVisible: " + e.toString());
                return;
            }
        }
        Slog.w(TAG, "mService is null.");
    }

    public void setMyClientVisible(boolean visible) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.setMyClientVisible(this.mContext.getOpPackageName(), visible);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception while setMyClientVisible: " + e.toString());
                return;
            }
        }
        Slog.w(TAG, "mService is null.");
    }

    public boolean isClientActive() {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.isClientActive();
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception while isClientActive: " + e.toString());
                return false;
            }
        }
        Slog.w(TAG, "mService is null.");
        return false;
    }

    public void notifyActivateFingerprint(boolean activate) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.notifyActivateFingerprint(activate);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception while notifyActivateFingerprint: " + e.toString());
                return;
            }
        }
        Slog.w(TAG, "mService is null.");
    }

    public boolean isAuthenticating() {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                return iFingerprintService.isAuthenticating();
            } catch (RemoteException e) {
                Slog.w(TAG, "Remote exception while isAuthenticating: " + e.toString());
                return false;
            }
        }
        Slog.w(TAG, "mService is null.");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        private MyHandler(Context context) {
            super(context.getMainLooper());
        }

        private MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    FingerprintManager.this.sendEnrollResult((Fingerprint) msg.obj, msg.arg1);
                    return;
                case 101:
                    FingerprintManager.this.sendAcquiredResult(msg.arg1, msg.arg2);
                    return;
                case 102:
                    FingerprintManager.this.sendAuthenticatedSucceeded((Fingerprint) msg.obj, msg.arg1, msg.arg2 == 1);
                    return;
                case 103:
                    FingerprintManager.this.sendAuthenticatedFailed();
                    return;
                case 104:
                    FingerprintManager.this.sendErrorResult(msg.arg1, msg.arg2);
                    return;
                case 105:
                    FingerprintManager.this.sendRemovedResult((Fingerprint) msg.obj, msg.arg1);
                    return;
                case 106:
                    FingerprintManager.this.sendChallengeGenerated(msg.arg1, msg.arg2, ((Long) msg.obj).longValue());
                    return;
                case 107:
                    FingerprintManager.this.sendFingerprintDetected(msg.arg1, msg.arg2, ((Boolean) msg.obj).booleanValue());
                    return;
                case 108:
                    FingerprintManager.this.sendUdfpsPointerDown(msg.arg1);
                    return;
                case 109:
                    FingerprintManager.this.sendUdfpsPointerUp(msg.arg1);
                    return;
                default:
                    Slog.w(FingerprintManager.TAG, "Unknown message: " + msg.what);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendRemovedResult(Fingerprint fingerprint, int remaining) {
        if (this.mRemovalCallback == null) {
            return;
        }
        RemoveTracker removeTracker = this.mRemoveTracker;
        if (removeTracker == null) {
            Slog.w(TAG, "Removal tracker is null");
            return;
        }
        if (removeTracker.mRemoveRequest == 1) {
            if (fingerprint == null) {
                Slog.e(TAG, "Received MSG_REMOVED, but fingerprint is null");
                return;
            } else if (this.mRemoveTracker.mSingleFingerprint == null) {
                Slog.e(TAG, "Missing fingerprint");
                return;
            } else {
                int fingerId = fingerprint.getBiometricId();
                int reqFingerId = this.mRemoveTracker.mSingleFingerprint.getBiometricId();
                if (reqFingerId != 0 && fingerId != 0 && fingerId != reqFingerId) {
                    Slog.w(TAG, "Finger id didn't match: " + fingerId + " != " + reqFingerId);
                    return;
                }
            }
        }
        this.mRemovalCallback.onRemovalSucceeded(fingerprint, remaining);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendEnrollResult(Fingerprint fp, int remaining) {
        EnrollmentCallback enrollmentCallback = this.mEnrollmentCallback;
        if (enrollmentCallback != null) {
            enrollmentCallback.onEnrollmentProgress(remaining);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendAuthenticatedSucceeded(Fingerprint fp, int userId, boolean isStrongBiometric) {
        if (this.mAuthenticationCallback != null) {
            AuthenticationResult result = new AuthenticationResult(this.mCryptoObject, fp, userId, isStrongBiometric);
            this.mAuthenticationCallback.onAuthenticationSucceeded(result);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendAuthenticatedFailed() {
        AuthenticationCallback authenticationCallback = this.mAuthenticationCallback;
        if (authenticationCallback != null) {
            authenticationCallback.onAuthenticationFailed();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendAcquiredResult(int acquireInfo, int vendorCode) {
        AuthenticationCallback authenticationCallback = this.mAuthenticationCallback;
        if (authenticationCallback != null) {
            authenticationCallback.onAuthenticationAcquired(acquireInfo);
        }
        String msg = getAcquiredString(this.mContext, acquireInfo, vendorCode);
        if (msg == null && acquireInfo != 6) {
            return;
        }
        int clientInfo = acquireInfo == 6 ? vendorCode + 1000 : acquireInfo;
        EnrollmentCallback enrollmentCallback = this.mEnrollmentCallback;
        if (enrollmentCallback != null) {
            enrollmentCallback.onEnrollmentHelp(clientInfo, msg);
        } else if (this.mAuthenticationCallback != null && msg != null && !"".equals(msg) && acquireInfo != 7) {
            this.mAuthenticationCallback.onAuthenticationHelp(clientInfo, msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendErrorResult(int errMsgId, int vendorCode) {
        int clientErrMsgId = errMsgId == 8 ? vendorCode + 1000 : errMsgId;
        EnrollmentCallback enrollmentCallback = this.mEnrollmentCallback;
        if (enrollmentCallback != null) {
            enrollmentCallback.onEnrollmentError(clientErrMsgId, getErrorString(this.mContext, errMsgId, vendorCode));
            return;
        }
        AuthenticationCallback authenticationCallback = this.mAuthenticationCallback;
        if (authenticationCallback != null) {
            authenticationCallback.onAuthenticationError(clientErrMsgId, getErrorString(this.mContext, errMsgId, vendorCode));
        } else if (this.mRemovalCallback != null) {
            RemoveTracker removeTracker = this.mRemoveTracker;
            Fingerprint fp = removeTracker != null ? removeTracker.mSingleFingerprint : null;
            this.mRemovalCallback.onRemovalError(fp, clientErrMsgId, getErrorString(this.mContext, errMsgId, vendorCode));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendChallengeGenerated(int sensorId, int userId, long challenge) {
        GenerateChallengeCallback generateChallengeCallback = this.mGenerateChallengeCallback;
        if (generateChallengeCallback == null) {
            Slog.e(TAG, "sendChallengeGenerated, callback null");
        } else {
            generateChallengeCallback.onChallengeGenerated(sensorId, userId, challenge);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendFingerprintDetected(int sensorId, int userId, boolean isStrongBiometric) {
        FingerprintDetectionCallback fingerprintDetectionCallback = this.mFingerprintDetectionCallback;
        if (fingerprintDetectionCallback == null) {
            Slog.e(TAG, "sendFingerprintDetected, callback null");
        } else {
            fingerprintDetectionCallback.onFingerprintDetected(sensorId, userId, isStrongBiometric);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendUdfpsPointerDown(int sensorId) {
        AuthenticationCallback authenticationCallback = this.mAuthenticationCallback;
        if (authenticationCallback == null) {
            Slog.e(TAG, "sendUdfpsPointerDown, callback null");
        } else {
            authenticationCallback.onUdfpsPointerDown(sensorId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendUdfpsPointerUp(int sensorId) {
        AuthenticationCallback authenticationCallback = this.mAuthenticationCallback;
        if (authenticationCallback == null) {
            Slog.e(TAG, "sendUdfpsPointerUp, callback null");
        } else {
            authenticationCallback.onUdfpsPointerUp(sensorId);
        }
    }

    public FingerprintManager(Context context, IFingerprintService service) {
        this.mContext = context;
        this.mService = service;
        if (service == null) {
            Slog.v(TAG, "FingerprintService was null");
        }
        this.mHandler = new MyHandler(context);
    }

    private int getCurrentUserId() {
        try {
            return ActivityManager.getService().getCurrentUser().id;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private FingerprintSensorPropertiesInternal getFirstFingerprintSensor() {
        List<FingerprintSensorPropertiesInternal> allSensors = getSensorPropertiesInternal();
        if (allSensors.isEmpty()) {
            return null;
        }
        return allSensors.get(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelEnrollment(long requestId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.cancelEnrollment(this.mToken, requestId);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelAuthentication(long requestId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService != null) {
            try {
                iFingerprintService.cancelAuthentication(this.mToken, this.mContext.getOpPackageName(), this.mContext.getAttributionTag(), requestId);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelFingerprintDetect(long requestId) {
        IFingerprintService iFingerprintService = this.mService;
        if (iFingerprintService == null) {
            return;
        }
        try {
            iFingerprintService.cancelFingerprintDetect(this.mToken, this.mContext.getOpPackageName(), requestId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getEnrollStageCount() {
        if (this.mEnrollStageThresholds == null) {
            this.mEnrollStageThresholds = createEnrollStageThresholds(this.mContext);
        }
        return this.mEnrollStageThresholds.length + 1;
    }

    public float getEnrollStageThreshold(int index) {
        if (this.mEnrollStageThresholds == null) {
            this.mEnrollStageThresholds = createEnrollStageThresholds(this.mContext);
        }
        if (index >= 0) {
            float[] fArr = this.mEnrollStageThresholds;
            if (index <= fArr.length) {
                if (index == fArr.length) {
                    return 1.0f;
                }
                return fArr[index];
            }
        }
        Slog.w(TAG, "Unsupported enroll stage index: " + index);
        return index < 0 ? 0.0f : 1.0f;
    }

    private static float[] createEnrollStageThresholds(Context context) {
        String[] enrollStageThresholdStrings = context.getResources().getStringArray(R.array.config_udfps_enroll_stage_thresholds);
        float[] enrollStageThresholds = new float[enrollStageThresholdStrings.length];
        for (int i = 0; i < enrollStageThresholds.length; i++) {
            enrollStageThresholds[i] = Float.parseFloat(enrollStageThresholdStrings[i]);
        }
        return enrollStageThresholds;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static String getErrorString(Context context, int errMsg, int vendorCode) {
        switch (errMsg) {
            case 1:
                return context.getString(R.string.fingerprint_error_hw_not_available);
            case 2:
                return context.getString(R.string.fingerprint_error_unable_to_process);
            case 3:
                return context.getString(R.string.fingerprint_error_timeout);
            case 4:
                return context.getString(R.string.fingerprint_error_no_space);
            case 5:
                return context.getString(R.string.fingerprint_error_canceled);
            case 7:
                return context.getString(R.string.fingerprint_error_lockout);
            case 8:
                String[] msgArray = context.getResources().getStringArray(R.array.fingerprint_error_vendor);
                if (vendorCode < msgArray.length) {
                    return msgArray[vendorCode];
                }
                break;
            case 9:
                return context.getString(R.string.fingerprint_error_lockout_permanent);
            case 10:
                return context.getString(R.string.fingerprint_error_user_canceled);
            case 11:
                return context.getString(R.string.fingerprint_error_no_fingerprints);
            case 12:
                return context.getString(R.string.fingerprint_error_hw_not_present);
            case 15:
                return context.getString(R.string.fingerprint_error_security_update_required);
            case 18:
                return context.getString(R.string.fingerprint_error_bad_calibration);
        }
        Slog.w(TAG, "Invalid error message: " + errMsg + ", " + vendorCode);
        return context.getString(R.string.fingerprint_error_vendor_unknown);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static String getAcquiredString(Context context, int acquireInfo, int vendorCode) {
        switch (acquireInfo) {
            case 0:
                return null;
            case 1:
                return context.getString(R.string.fingerprint_acquired_partial);
            case 2:
                return context.getString(R.string.fingerprint_acquired_insufficient);
            case 3:
                return context.getString(R.string.fingerprint_acquired_imager_dirty);
            case 4:
                return context.getString(R.string.fingerprint_acquired_too_slow);
            case 5:
                return context.getString(R.string.fingerprint_acquired_too_fast);
            case 6:
                String[] msgArray = context.getResources().getStringArray(R.array.fingerprint_acquired_vendor);
                if (vendorCode < msgArray.length) {
                    return msgArray[vendorCode];
                }
                break;
            case 7:
                return null;
            case 9:
                return context.getString(R.string.fingerprint_acquired_immobile);
            case 10:
                return context.getString(R.string.fingerprint_acquired_too_bright);
        }
        Slog.w(TAG, "Invalid acquired message: " + acquireInfo + ", " + vendorCode);
        return null;
    }
}
