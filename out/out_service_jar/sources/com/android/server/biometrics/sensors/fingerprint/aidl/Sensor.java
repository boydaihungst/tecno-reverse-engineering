package com.android.server.biometrics.sensors.fingerprint.aidl;

import android.app.ActivityManager;
import android.app.SynchronousUserSwitchObserver;
import android.app.UserSwitchObserver;
import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.fingerprint.ISession;
import android.hardware.biometrics.fingerprint.ISessionCallback;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.keymaster.HardwareAuthToken;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.biometrics.HardwareAuthTokenUtils;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.AcquisitionClient;
import com.android.server.biometrics.sensors.AuthenticationConsumer;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricScheduler;
import com.android.server.biometrics.sensors.BiometricStateCallback;
import com.android.server.biometrics.sensors.EnumerateConsumer;
import com.android.server.biometrics.sensors.ErrorConsumer;
import com.android.server.biometrics.sensors.LockoutCache;
import com.android.server.biometrics.sensors.LockoutConsumer;
import com.android.server.biometrics.sensors.LockoutResetDispatcher;
import com.android.server.biometrics.sensors.RemovalConsumer;
import com.android.server.biometrics.sensors.StartUserClient;
import com.android.server.biometrics.sensors.StopUserClient;
import com.android.server.biometrics.sensors.UserAwareBiometricScheduler;
import com.android.server.biometrics.sensors.fingerprint.FingerprintUtils;
import com.android.server.biometrics.sensors.fingerprint.GestureAvailabilityDispatcher;
import com.android.server.biometrics.sensors.fingerprint.aidl.Sensor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class Sensor {
    private final Map<Integer, Long> mAuthenticatorIds;
    private final Context mContext;
    private AidlSession mCurrentSession;
    private final Handler mHandler;
    private final Supplier<AidlSession> mLazySession;
    private final LockoutCache mLockoutCache;
    private final FingerprintProvider mProvider;
    private final UserAwareBiometricScheduler mScheduler;
    private final FingerprintSensorPropertiesInternal mSensorProperties;
    private final String mTag;
    private boolean mTestHalEnabled;
    private final IBinder mToken;
    private final UserSwitchObserver mUserSwitchObserver;

    /* loaded from: classes.dex */
    public static class HalSessionCallback extends ISessionCallback.Stub {
        private final Callback mCallback;
        private final Context mContext;
        private final Handler mHandler;
        private final LockoutCache mLockoutCache;
        private final LockoutResetDispatcher mLockoutResetDispatcher;
        private final UserAwareBiometricScheduler mScheduler;
        private final int mSensorId;
        private final String mTag;
        private final int mUserId;

        /* loaded from: classes.dex */
        public interface Callback {
            void onHardwareUnavailable();
        }

        HalSessionCallback(Context context, Handler handler, String tag, UserAwareBiometricScheduler scheduler, int sensorId, int userId, LockoutCache lockoutTracker, LockoutResetDispatcher lockoutResetDispatcher, Callback callback) {
            this.mContext = context;
            this.mHandler = handler;
            this.mTag = tag;
            this.mScheduler = scheduler;
            this.mSensorId = sensorId;
            this.mUserId = userId;
            this.mLockoutCache = lockoutTracker;
            this.mLockoutResetDispatcher = lockoutResetDispatcher;
            this.mCallback = callback;
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public int getInterfaceVersion() {
            return 2;
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public String getInterfaceHash() {
            return "c2f3b863b6dff925bc4451473ee6caa1aa304b8f";
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onChallengeGenerated(final long challenge) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2493x4d763df4(challenge);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onChallengeGenerated$0$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2493x4d763df4(long challenge) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FingerprintGenerateChallengeClient)) {
                Slog.e(this.mTag, "onChallengeGenerated for wrong client: " + Utils.getClientName(client));
                return;
            }
            FingerprintGenerateChallengeClient generateChallengeClient = (FingerprintGenerateChallengeClient) client;
            generateChallengeClient.onChallengeGenerated(this.mSensorId, this.mUserId, challenge);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onChallengeRevoked(final long challenge) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2494xa5959c64(challenge);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onChallengeRevoked$1$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2494xa5959c64(long challenge) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FingerprintRevokeChallengeClient)) {
                Slog.e(this.mTag, "onChallengeRevoked for wrong client: " + Utils.getClientName(client));
                return;
            }
            FingerprintRevokeChallengeClient revokeChallengeClient = (FingerprintRevokeChallengeClient) client;
            revokeChallengeClient.onChallengeRevoked(this.mSensorId, this.mUserId, challenge);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onAcquired(final byte info, final int vendorCode) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2488x32d65a10(info, vendorCode);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAcquired$2$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2488x32d65a10(byte info, int vendorCode) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof AcquisitionClient)) {
                Slog.e(this.mTag, "onAcquired for non-acquisition client: " + Utils.getClientName(client));
                return;
            }
            AcquisitionClient<?> acquisitionClient = (AcquisitionClient) client;
            acquisitionClient.onAcquired(AidlConversionUtils.toFrameworkAcquiredInfo(info), vendorCode);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onError(final byte error, final int vendorCode) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2498xb0dc3fad(error, vendorCode);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onError$3$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2498xb0dc3fad(byte error, int vendorCode) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            Slog.d(this.mTag, "onError, client: " + Utils.getClientName(client) + ", error: " + ((int) error) + ", vendorCode: " + vendorCode);
            if (!(client instanceof ErrorConsumer)) {
                Slog.e(this.mTag, "onError for non-error consumer: " + Utils.getClientName(client));
                return;
            }
            ErrorConsumer errorConsumer = (ErrorConsumer) client;
            errorConsumer.onError(AidlConversionUtils.toFrameworkError(error), vendorCode);
            if (error == 1) {
                this.mCallback.onHardwareUnavailable();
            }
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onEnrollmentProgress(final int enrollmentId, final int remaining) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2495xbe31896b(enrollmentId, remaining);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnrollmentProgress$4$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2495xbe31896b(int enrollmentId, int remaining) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FingerprintEnrollClient)) {
                Slog.e(this.mTag, "onEnrollmentProgress for non-enroll client: " + Utils.getClientName(client));
                return;
            }
            int currentUserId = client.getTargetUserId();
            CharSequence name = FingerprintUtils.getInstance(this.mSensorId).getUniqueName(this.mContext, currentUserId);
            FingerprintEnrollClient enrollClient = (FingerprintEnrollClient) client;
            enrollClient.onEnrollResult(new Fingerprint(name, enrollmentId, this.mSensorId), remaining);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onAuthenticationSucceeded(final int enrollmentId, final HardwareAuthToken hat) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2490xa8edbe8a(enrollmentId, hat);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticationSucceeded$5$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2490xa8edbe8a(int enrollmentId, HardwareAuthToken hat) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof AuthenticationConsumer)) {
                Slog.e(this.mTag, "onAuthenticationSucceeded for non-authentication consumer: " + Utils.getClientName(client));
                return;
            }
            AuthenticationConsumer authenticationConsumer = (AuthenticationConsumer) client;
            Fingerprint fp = new Fingerprint("", enrollmentId, this.mSensorId);
            byte[] byteArray = HardwareAuthTokenUtils.toByteArray(hat);
            ArrayList<Byte> byteList = new ArrayList<>();
            for (byte b : byteArray) {
                byteList.add(Byte.valueOf(b));
            }
            authenticationConsumer.onAuthenticated(fp, true, byteList);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onAuthenticationFailed() {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2489xef1d72a5();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticationFailed$6$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2489xef1d72a5() {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof AuthenticationConsumer)) {
                Slog.e(this.mTag, "onAuthenticationFailed for non-authentication consumer: " + Utils.getClientName(client));
                return;
            }
            AuthenticationConsumer authenticationConsumer = (AuthenticationConsumer) client;
            Fingerprint fp = new Fingerprint("", 0, this.mSensorId);
            authenticationConsumer.onAuthenticated(fp, false, null);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onLockoutTimed(final long durationMillis) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2502x2e80ac85(durationMillis);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onLockoutTimed$7$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2502x2e80ac85(long durationMillis) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof LockoutConsumer)) {
                Slog.e(this.mTag, "onLockoutTimed for non-lockout consumer: " + Utils.getClientName(client));
                return;
            }
            LockoutConsumer lockoutConsumer = (LockoutConsumer) client;
            lockoutConsumer.onLockoutTimed(durationMillis);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onLockoutPermanent() {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2501xf86e3c4d();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onLockoutPermanent$8$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2501xf86e3c4d() {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof LockoutConsumer)) {
                Slog.e(this.mTag, "onLockoutPermanent for non-lockout consumer: " + Utils.getClientName(client));
                return;
            }
            LockoutConsumer lockoutConsumer = (LockoutConsumer) client;
            lockoutConsumer.onLockoutPermanent();
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onLockoutCleared() {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2500xc21ae86e();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onLockoutCleared$9$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2500xc21ae86e() {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FingerprintResetLockoutClient)) {
                Slog.d(this.mTag, "onLockoutCleared outside of resetLockout by HAL");
                FingerprintResetLockoutClient.resetLocalLockoutStateToNone(this.mSensorId, this.mUserId, this.mLockoutCache, this.mLockoutResetDispatcher);
                return;
            }
            Slog.d(this.mTag, "onLockoutCleared after resetLockout");
            FingerprintResetLockoutClient resetLockoutClient = (FingerprintResetLockoutClient) client;
            resetLockoutClient.onLockoutCleared();
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onInteractionDetected() {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2499x768581bd();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInteractionDetected$10$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2499x768581bd() {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FingerprintDetectClient)) {
                Slog.e(this.mTag, "onInteractionDetected for non-detect client: " + Utils.getClientName(client));
                return;
            }
            FingerprintDetectClient fingerprintDetectClient = (FingerprintDetectClient) client;
            fingerprintDetectClient.onInteractionDetected();
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onEnrollmentsEnumerated(final int[] enrollmentIds) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2496x81d02277(enrollmentIds);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnrollmentsEnumerated$11$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2496x81d02277(int[] enrollmentIds) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof EnumerateConsumer)) {
                Slog.e(this.mTag, "onEnrollmentsEnumerated for non-enumerate consumer: " + Utils.getClientName(client));
                return;
            }
            EnumerateConsumer enumerateConsumer = (EnumerateConsumer) client;
            if (enrollmentIds.length > 0) {
                for (int i = 0; i < enrollmentIds.length; i++) {
                    Fingerprint fp = new Fingerprint("", enrollmentIds[i], this.mSensorId);
                    enumerateConsumer.onEnumerationResult(fp, (enrollmentIds.length - i) - 1);
                }
                return;
            }
            enumerateConsumer.onEnumerationResult(null, 0);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onEnrollmentsRemoved(final int[] enrollmentIds) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2497x15126cb0(enrollmentIds);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnrollmentsRemoved$12$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2497x15126cb0(int[] enrollmentIds) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof RemovalConsumer)) {
                Slog.e(this.mTag, "onRemoved for non-removal consumer: " + Utils.getClientName(client));
                return;
            }
            RemovalConsumer removalConsumer = (RemovalConsumer) client;
            if (enrollmentIds.length > 0) {
                for (int i = 0; i < enrollmentIds.length; i++) {
                    Fingerprint fp = new Fingerprint("", enrollmentIds[i], this.mSensorId);
                    removalConsumer.onRemoved(fp, (enrollmentIds.length - i) - 1);
                }
                return;
            }
            removalConsumer.onRemoved(null, 0);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onAuthenticatorIdRetrieved(final long authenticatorId) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2492x85606138(authenticatorId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticatorIdRetrieved$13$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2492x85606138(long authenticatorId) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FingerprintGetAuthenticatorIdClient)) {
                Slog.e(this.mTag, "onAuthenticatorIdRetrieved for wrong consumer: " + Utils.getClientName(client));
                return;
            }
            FingerprintGetAuthenticatorIdClient getAuthenticatorIdClient = (FingerprintGetAuthenticatorIdClient) client;
            getAuthenticatorIdClient.onAuthenticatorIdRetrieved(authenticatorId);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onAuthenticatorIdInvalidated(final long newAuthenticatorId) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2491x1cb44438(newAuthenticatorId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticatorIdInvalidated$14$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2491x1cb44438(long newAuthenticatorId) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FingerprintInvalidationClient)) {
                Slog.e(this.mTag, "onAuthenticatorIdInvalidated for wrong consumer: " + Utils.getClientName(client));
                return;
            }
            FingerprintInvalidationClient invalidationClient = (FingerprintInvalidationClient) client;
            invalidationClient.onAuthenticatorIdInvalidated(newAuthenticatorId);
        }

        @Override // android.hardware.biometrics.fingerprint.ISessionCallback
        public void onSessionClosed() {
            Handler handler = this.mHandler;
            UserAwareBiometricScheduler userAwareBiometricScheduler = this.mScheduler;
            Objects.requireNonNull(userAwareBiometricScheduler);
            handler.post(new com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda2(userAwareBiometricScheduler));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Sensor(String tag, FingerprintProvider provider, Context context, Handler handler, FingerprintSensorPropertiesInternal sensorProperties, LockoutResetDispatcher lockoutResetDispatcher, GestureAvailabilityDispatcher gestureAvailabilityDispatcher, BiometricContext biometricContext) {
        SynchronousUserSwitchObserver synchronousUserSwitchObserver = new SynchronousUserSwitchObserver() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor.1
            public void onUserSwitching(int newUserId) {
                Sensor.this.mProvider.scheduleInternalCleanup(Sensor.this.mSensorProperties.sensorId, newUserId, null);
            }
        };
        this.mUserSwitchObserver = synchronousUserSwitchObserver;
        this.mTag = tag;
        this.mProvider = provider;
        this.mContext = context;
        this.mToken = new Binder();
        this.mHandler = handler;
        this.mSensorProperties = sensorProperties;
        this.mLockoutCache = new LockoutCache();
        this.mScheduler = new UserAwareBiometricScheduler(tag, BiometricScheduler.sensorTypeFromFingerprintProperties(sensorProperties), gestureAvailabilityDispatcher, new UserAwareBiometricScheduler.CurrentUserRetriever() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$$ExternalSyntheticLambda0
            @Override // com.android.server.biometrics.sensors.UserAwareBiometricScheduler.CurrentUserRetriever
            public final int getCurrentUserId() {
                return Sensor.this.m2483x72e5f613();
            }
        }, new AnonymousClass2(biometricContext, lockoutResetDispatcher, provider));
        this.mAuthenticatorIds = new HashMap();
        this.mLazySession = new Supplier() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                return Sensor.this.m2484x66757a54();
            }
        };
        try {
            ActivityManager.getService().registerUserSwitchObserver(synchronousUserSwitchObserver, tag);
        } catch (RemoteException e) {
            Slog.e(this.mTag, "Unable to register user switch observer");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$2  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements UserAwareBiometricScheduler.UserSwitchCallback {
        final /* synthetic */ BiometricContext val$biometricContext;
        final /* synthetic */ LockoutResetDispatcher val$lockoutResetDispatcher;
        final /* synthetic */ FingerprintProvider val$provider;

        AnonymousClass2(BiometricContext biometricContext, LockoutResetDispatcher lockoutResetDispatcher, FingerprintProvider fingerprintProvider) {
            this.val$biometricContext = biometricContext;
            this.val$lockoutResetDispatcher = lockoutResetDispatcher;
            this.val$provider = fingerprintProvider;
        }

        @Override // com.android.server.biometrics.sensors.UserAwareBiometricScheduler.UserSwitchCallback
        public StopUserClient<?> getStopUserClient(int userId) {
            return new FingerprintStopUserClient(Sensor.this.mContext, Sensor.this.mLazySession, Sensor.this.mToken, userId, Sensor.this.mSensorProperties.sensorId, BiometricLogger.ofUnknown(Sensor.this.mContext), this.val$biometricContext, new StopUserClient.UserStoppedCallback() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$2$$ExternalSyntheticLambda0
                @Override // com.android.server.biometrics.sensors.StopUserClient.UserStoppedCallback
                public final void onUserStopped() {
                    Sensor.AnonymousClass2.this.m2487x62c7198f();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getStopUserClient$0$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$2  reason: not valid java name */
        public /* synthetic */ void m2487x62c7198f() {
            Sensor.this.mCurrentSession = null;
        }

        @Override // com.android.server.biometrics.sensors.UserAwareBiometricScheduler.UserSwitchCallback
        public StartUserClient<?, ?> getStartUserClient(int newUserId) {
            final int sensorId = Sensor.this.mSensorProperties.sensorId;
            final HalSessionCallback resultController = new HalSessionCallback(Sensor.this.mContext, Sensor.this.mHandler, Sensor.this.mTag, Sensor.this.mScheduler, sensorId, newUserId, Sensor.this.mLockoutCache, this.val$lockoutResetDispatcher, new HalSessionCallback.Callback() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$2$$ExternalSyntheticLambda1
                @Override // com.android.server.biometrics.sensors.fingerprint.aidl.Sensor.HalSessionCallback.Callback
                public final void onHardwareUnavailable() {
                    Sensor.AnonymousClass2.this.m2485x869ac2de();
                }
            });
            final FingerprintProvider fingerprintProvider = this.val$provider;
            StartUserClient.UserStartedCallback<ISession> userStartedCallback = new StartUserClient.UserStartedCallback() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$2$$ExternalSyntheticLambda2
                @Override // com.android.server.biometrics.sensors.StartUserClient.UserStartedCallback
                public final void onUserStarted(int i, Object obj, int i2) {
                    Sensor.AnonymousClass2.this.m2486xd45a3adf(resultController, sensorId, fingerprintProvider, i, (ISession) obj, i2);
                }
            };
            Context context = Sensor.this.mContext;
            final FingerprintProvider fingerprintProvider2 = this.val$provider;
            Objects.requireNonNull(fingerprintProvider2);
            return new FingerprintStartUserClient(context, new Supplier() { // from class: com.android.server.biometrics.sensors.fingerprint.aidl.Sensor$2$$ExternalSyntheticLambda3
                @Override // java.util.function.Supplier
                public final Object get() {
                    return FingerprintProvider.this.getHalInstance();
                }
            }, Sensor.this.mToken, newUserId, Sensor.this.mSensorProperties.sensorId, BiometricLogger.ofUnknown(Sensor.this.mContext), this.val$biometricContext, resultController, userStartedCallback);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getStartUserClient$1$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$2  reason: not valid java name */
        public /* synthetic */ void m2485x869ac2de() {
            Slog.e(Sensor.this.mTag, "Got ERROR_HW_UNAVAILABLE");
            Sensor.this.mCurrentSession = null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getStartUserClient$2$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor$2  reason: not valid java name */
        public /* synthetic */ void m2486xd45a3adf(HalSessionCallback resultController, int sensorId, FingerprintProvider provider, int userIdStarted, ISession newSession, int halInterfaceVersion) {
            Slog.d(Sensor.this.mTag, "New session created for user: " + userIdStarted + " with hal version: " + halInterfaceVersion);
            Sensor.this.mCurrentSession = new AidlSession(halInterfaceVersion, newSession, userIdStarted, resultController);
            if (FingerprintUtils.getInstance(sensorId).isInvalidationInProgress(Sensor.this.mContext, userIdStarted)) {
                Slog.w(Sensor.this.mTag, "Scheduling unfinished invalidation request for sensor: " + sensorId + ", user: " + userIdStarted);
                provider.scheduleInvalidationRequest(sensorId, userIdStarted);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor  reason: not valid java name */
    public /* synthetic */ int m2483x72e5f613() {
        AidlSession aidlSession = this.mCurrentSession;
        if (aidlSession != null) {
            return aidlSession.getUserId();
        }
        return -10000;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-biometrics-sensors-fingerprint-aidl-Sensor  reason: not valid java name */
    public /* synthetic */ AidlSession m2484x66757a54() {
        AidlSession aidlSession = this.mCurrentSession;
        if (aidlSession != null) {
            return aidlSession;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Supplier<AidlSession> getLazySession() {
        return this.mLazySession;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FingerprintSensorPropertiesInternal getSensorProperties() {
        return this.mSensorProperties;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AidlSession getSessionForUser(int userId) {
        AidlSession aidlSession = this.mCurrentSession;
        if (aidlSession != null && aidlSession.getUserId() == userId) {
            return this.mCurrentSession;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ITestSession createTestSession(ITestSessionCallback callback, BiometricStateCallback biometricStateCallback) {
        return new BiometricTestSessionImpl(this.mContext, this.mSensorProperties.sensorId, callback, biometricStateCallback, this.mProvider, this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BiometricScheduler getScheduler() {
        return this.mScheduler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LockoutCache getLockoutCache() {
        return this.mLockoutCache;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<Integer, Long> getAuthenticatorIds() {
        return this.mAuthenticatorIds;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTestHalEnabled(boolean enabled) {
        Slog.w(this.mTag, "setTestHalEnabled: " + enabled);
        if (enabled != this.mTestHalEnabled) {
            try {
                if (this.mCurrentSession != null) {
                    Slog.d(this.mTag, "Closing old session");
                    this.mCurrentSession.getSession().close();
                }
            } catch (RemoteException e) {
                Slog.e(this.mTag, "RemoteException", e);
            }
            this.mCurrentSession = null;
        }
        this.mTestHalEnabled = enabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpProtoState(int sensorId, ProtoOutputStream proto, boolean clearSchedulerBuffer) {
        long sensorToken = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
        proto.write(CompanionMessage.MESSAGE_ID, this.mSensorProperties.sensorId);
        proto.write(CompanionMessage.TYPE, 1);
        if (this.mSensorProperties.isAnyUdfpsType()) {
            proto.write(2259152797704L, 0);
        }
        proto.write(1120986464259L, Utils.getCurrentStrength(this.mSensorProperties.sensorId));
        proto.write(1146756268036L, this.mScheduler.dumpProtoState(clearSchedulerBuffer));
        for (UserInfo user : UserManager.get(this.mContext).getUsers()) {
            int userId = user.getUserHandle().getIdentifier();
            long userToken = proto.start(2246267895813L);
            proto.write(CompanionMessage.MESSAGE_ID, userId);
            proto.write(1120986464258L, FingerprintUtils.getInstance(this.mSensorProperties.sensorId).getBiometricsForUser(this.mContext, userId).size());
            proto.end(userToken);
        }
        proto.write(1133871366150L, this.mSensorProperties.resetLockoutRequiresHardwareAuthToken);
        proto.write(1133871366151L, this.mSensorProperties.resetLockoutRequiresChallenge);
        proto.end(sensorToken);
    }

    public void onBinderDied() {
        BaseClientMonitor client = this.mScheduler.getCurrentClient();
        if (client instanceof ErrorConsumer) {
            Slog.e(this.mTag, "Sending ERROR_HW_UNAVAILABLE for client: " + client);
            ErrorConsumer errorConsumer = (ErrorConsumer) client;
            errorConsumer.onError(1, 0);
            FrameworkStatsLog.write(148, 1, 1, -1);
        }
        this.mScheduler.recordCrashState();
        this.mScheduler.reset();
        this.mCurrentSession = null;
    }
}
