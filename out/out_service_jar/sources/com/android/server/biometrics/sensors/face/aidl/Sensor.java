package com.android.server.biometrics.sensors.face.aidl;

import android.app.ActivityManager;
import android.app.SynchronousUserSwitchObserver;
import android.app.UserSwitchObserver;
import android.content.Context;
import android.content.pm.UserInfo;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.face.AuthenticationFrame;
import android.hardware.biometrics.face.EnrollmentFrame;
import android.hardware.biometrics.face.ISession;
import android.hardware.biometrics.face.ISessionCallback;
import android.hardware.face.Face;
import android.hardware.face.FaceSensorPropertiesInternal;
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
import com.android.server.biometrics.sensors.AuthenticationConsumer;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricScheduler;
import com.android.server.biometrics.sensors.EnumerateConsumer;
import com.android.server.biometrics.sensors.ErrorConsumer;
import com.android.server.biometrics.sensors.Interruptable;
import com.android.server.biometrics.sensors.LockoutCache;
import com.android.server.biometrics.sensors.LockoutConsumer;
import com.android.server.biometrics.sensors.LockoutResetDispatcher;
import com.android.server.biometrics.sensors.RemovalConsumer;
import com.android.server.biometrics.sensors.StartUserClient;
import com.android.server.biometrics.sensors.StopUserClient;
import com.android.server.biometrics.sensors.UserAwareBiometricScheduler;
import com.android.server.biometrics.sensors.face.FaceUtils;
import com.android.server.biometrics.sensors.face.aidl.Sensor;
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
    private final FaceProvider mProvider;
    private final UserAwareBiometricScheduler mScheduler;
    private final FaceSensorPropertiesInternal mSensorProperties;
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

        @Override // android.hardware.biometrics.face.ISessionCallback
        public int getInterfaceVersion() {
            return 2;
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public String getInterfaceHash() {
            return "74b0b7cb149ee205b12cd2254d216725c6e5429c";
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onChallengeGenerated(final long challenge) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2377x3d67ceaf(challenge);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onChallengeGenerated$0$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2377x3d67ceaf(long challenge) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceGenerateChallengeClient)) {
                Slog.e(this.mTag, "onChallengeGenerated for wrong client: " + Utils.getClientName(client));
                return;
            }
            FaceGenerateChallengeClient generateChallengeClient = (FaceGenerateChallengeClient) client;
            generateChallengeClient.onChallengeGenerated(this.mSensorId, this.mUserId, challenge);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onChallengeRevoked(final long challenge) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2378xd424ce3f(challenge);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onChallengeRevoked$1$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2378xd424ce3f(long challenge) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceRevokeChallengeClient)) {
                Slog.e(this.mTag, "onChallengeRevoked for wrong client: " + Utils.getClientName(client));
                return;
            }
            FaceRevokeChallengeClient revokeChallengeClient = (FaceRevokeChallengeClient) client;
            revokeChallengeClient.onChallengeRevoked(this.mSensorId, this.mUserId, challenge);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onAuthenticationFrame(final AuthenticationFrame frame) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2373xd73155a2(frame);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticationFrame$2$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2373xd73155a2(AuthenticationFrame frame) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceAuthenticationClient)) {
                Slog.e(this.mTag, "onAuthenticationFrame for incompatible client: " + Utils.getClientName(client));
            } else if (frame == null) {
                Slog.e(this.mTag, "Received null authentication frame for client: " + Utils.getClientName(client));
            } else {
                ((FaceAuthenticationClient) client).onAuthenticationFrame(AidlConversionUtils.toFrameworkAuthenticationFrame(frame));
            }
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onEnrollmentFrame(final EnrollmentFrame frame) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2379xaeb0eb57(frame);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnrollmentFrame$3$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2379xaeb0eb57(EnrollmentFrame frame) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceEnrollClient)) {
                Slog.e(this.mTag, "onEnrollmentFrame for incompatible client: " + Utils.getClientName(client));
            } else if (frame == null) {
                Slog.e(this.mTag, "Received null enrollment frame for client: " + Utils.getClientName(client));
            } else {
                ((FaceEnrollClient) client).onEnrollmentFrame(AidlConversionUtils.toFrameworkEnrollmentFrame(frame));
            }
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onError(final byte error, final int vendorCode) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2383xd33385d7(error, vendorCode);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onError$4$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2383xd33385d7(byte error, int vendorCode) {
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

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onEnrollmentProgress(final int enrollmentId, final int remaining) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda18
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2380xc0e95d9(enrollmentId, remaining);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnrollmentProgress$5$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2380xc0e95d9(int enrollmentId, int remaining) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceEnrollClient)) {
                Slog.e(this.mTag, "onEnrollmentProgress for non-enroll client: " + Utils.getClientName(client));
                return;
            }
            int currentUserId = client.getTargetUserId();
            CharSequence name = FaceUtils.getInstance(this.mSensorId).getUniqueName(this.mContext, currentUserId);
            FaceEnrollClient enrollClient = (FaceEnrollClient) client;
            enrollClient.onEnrollResult(new Face(name, enrollmentId, this.mSensorId), remaining);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onAuthenticationSucceeded(final int enrollmentId, final HardwareAuthToken hat) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2374x9370559a(enrollmentId, hat);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticationSucceeded$6$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2374x9370559a(int enrollmentId, HardwareAuthToken hat) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof AuthenticationConsumer)) {
                Slog.e(this.mTag, "onAuthenticationSucceeded for non-authentication consumer: " + Utils.getClientName(client));
                return;
            }
            AuthenticationConsumer authenticationConsumer = (AuthenticationConsumer) client;
            Face face = new Face("", enrollmentId, this.mSensorId);
            byte[] byteArray = HardwareAuthTokenUtils.toByteArray(hat);
            ArrayList<Byte> byteList = new ArrayList<>();
            for (byte b : byteArray) {
                byteList.add(Byte.valueOf(b));
            }
            authenticationConsumer.onAuthenticated(face, true, byteList);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onAuthenticationFailed() {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2372x21e939df();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticationFailed$7$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2372x21e939df() {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof AuthenticationConsumer)) {
                Slog.e(this.mTag, "onAuthenticationFailed for non-authentication consumer: " + Utils.getClientName(client));
                return;
            }
            AuthenticationConsumer authenticationConsumer = (AuthenticationConsumer) client;
            Face face = new Face("", 0, this.mSensorId);
            authenticationConsumer.onAuthenticated(face, false, null);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onLockoutTimed(final long durationMillis) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2389x67a15bff(durationMillis);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onLockoutTimed$8$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2389x67a15bff(long durationMillis) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof LockoutConsumer)) {
                Slog.e(this.mTag, "onLockoutTimed for non-lockout consumer: " + Utils.getClientName(client));
                return;
            }
            LockoutConsumer lockoutConsumer = (LockoutConsumer) client;
            lockoutConsumer.onLockoutTimed(durationMillis);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onLockoutPermanent() {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2388x671e7d37();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onLockoutPermanent$9$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2388x671e7d37() {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof LockoutConsumer)) {
                Slog.e(this.mTag, "onLockoutPermanent for non-lockout consumer: " + Utils.getClientName(client));
                return;
            }
            LockoutConsumer lockoutConsumer = (LockoutConsumer) client;
            lockoutConsumer.onLockoutPermanent();
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onLockoutCleared() {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2387xbf731e79();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onLockoutCleared$10$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2387xbf731e79() {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceResetLockoutClient)) {
                Slog.d(this.mTag, "onLockoutCleared outside of resetLockout by HAL");
                FaceResetLockoutClient.resetLocalLockoutStateToNone(this.mSensorId, this.mUserId, this.mLockoutCache, this.mLockoutResetDispatcher);
                return;
            }
            Slog.d(this.mTag, "onLockoutCleared after resetLockout");
            FaceResetLockoutClient resetLockoutClient = (FaceResetLockoutClient) client;
            resetLockoutClient.onLockoutCleared();
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onInteractionDetected() {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2386x63e975c7();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInteractionDetected$11$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2386x63e975c7() {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceDetectClient)) {
                Slog.e(this.mTag, "onInteractionDetected for wrong client: " + Utils.getClientName(client));
                return;
            }
            FaceDetectClient detectClient = (FaceDetectClient) client;
            detectClient.onInteractionDetected();
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onEnrollmentsEnumerated(final int[] enrollmentIds) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda17
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2381x913cd24d(enrollmentIds);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnrollmentsEnumerated$12$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2381x913cd24d(int[] enrollmentIds) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof EnumerateConsumer)) {
                Slog.e(this.mTag, "onEnrollmentsEnumerated for non-enumerate consumer: " + Utils.getClientName(client));
                return;
            }
            EnumerateConsumer enumerateConsumer = (EnumerateConsumer) client;
            if (enrollmentIds.length > 0) {
                for (int i = 0; i < enrollmentIds.length; i++) {
                    Face face = new Face("", enrollmentIds[i], this.mSensorId);
                    enumerateConsumer.onEnumerationResult(face, (enrollmentIds.length - i) - 1);
                }
                return;
            }
            enumerateConsumer.onEnumerationResult(null, 0);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onFeaturesRetrieved(final byte[] features) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2385xb3606272(features);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onFeaturesRetrieved$13$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2385xb3606272(byte[] features) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceGetFeatureClient)) {
                Slog.e(this.mTag, "onFeaturesRetrieved for non-get feature consumer: " + Utils.getClientName(client));
                return;
            }
            FaceGetFeatureClient faceGetFeatureClient = (FaceGetFeatureClient) client;
            faceGetFeatureClient.onFeatureGet(true, features);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onFeatureSet(byte feature) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2384x14768aba();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onFeatureSet$14$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2384x14768aba() {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceSetFeatureClient)) {
                Slog.e(this.mTag, "onFeatureSet for non-set consumer: " + Utils.getClientName(client));
                return;
            }
            FaceSetFeatureClient faceSetFeatureClient = (FaceSetFeatureClient) client;
            faceSetFeatureClient.onFeatureSet(true);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onEnrollmentsRemoved(final int[] enrollmentIds) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2382x7562fbb6(enrollmentIds);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onEnrollmentsRemoved$15$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2382x7562fbb6(int[] enrollmentIds) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof RemovalConsumer)) {
                Slog.e(this.mTag, "onRemoved for non-removal consumer: " + Utils.getClientName(client));
                return;
            }
            RemovalConsumer removalConsumer = (RemovalConsumer) client;
            if (enrollmentIds.length > 0) {
                for (int i = 0; i < enrollmentIds.length; i++) {
                    Face face = new Face("", enrollmentIds[i], this.mSensorId);
                    removalConsumer.onRemoved(face, (enrollmentIds.length - i) - 1);
                }
                return;
            }
            removalConsumer.onRemoved(null, 0);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onAuthenticatorIdRetrieved(final long authenticatorId) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda13
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2376x2e93902e(authenticatorId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticatorIdRetrieved$16$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2376x2e93902e(long authenticatorId) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceGetAuthenticatorIdClient)) {
                Slog.e(this.mTag, "onAuthenticatorIdRetrieved for wrong consumer: " + Utils.getClientName(client));
                return;
            }
            FaceGetAuthenticatorIdClient getAuthenticatorIdClient = (FaceGetAuthenticatorIdClient) client;
            getAuthenticatorIdClient.onAuthenticatorIdRetrieved(authenticatorId);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onAuthenticatorIdInvalidated(final long newAuthenticatorId) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$HalSessionCallback$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    Sensor.HalSessionCallback.this.m2375xdc890d2e(newAuthenticatorId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticatorIdInvalidated$17$com-android-server-biometrics-sensors-face-aidl-Sensor$HalSessionCallback  reason: not valid java name */
        public /* synthetic */ void m2375xdc890d2e(long newAuthenticatorId) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FaceInvalidationClient)) {
                Slog.e(this.mTag, "onAuthenticatorIdInvalidated for wrong consumer: " + Utils.getClientName(client));
                return;
            }
            FaceInvalidationClient invalidationClient = (FaceInvalidationClient) client;
            invalidationClient.onAuthenticatorIdInvalidated(newAuthenticatorId);
        }

        @Override // android.hardware.biometrics.face.ISessionCallback
        public void onSessionClosed() {
            Handler handler = this.mHandler;
            UserAwareBiometricScheduler userAwareBiometricScheduler = this.mScheduler;
            Objects.requireNonNull(userAwareBiometricScheduler);
            handler.post(new Sensor$HalSessionCallback$$ExternalSyntheticLambda2(userAwareBiometricScheduler));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Sensor(String tag, FaceProvider provider, Context context, Handler handler, FaceSensorPropertiesInternal sensorProperties, LockoutResetDispatcher lockoutResetDispatcher, BiometricContext biometricContext) {
        SynchronousUserSwitchObserver synchronousUserSwitchObserver = new SynchronousUserSwitchObserver() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor.1
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
        this.mScheduler = new UserAwareBiometricScheduler(tag, 1, null, new UserAwareBiometricScheduler.CurrentUserRetriever() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$$ExternalSyntheticLambda0
            @Override // com.android.server.biometrics.sensors.UserAwareBiometricScheduler.CurrentUserRetriever
            public final int getCurrentUserId() {
                return Sensor.this.m2367x6758a5e2();
            }
        }, new AnonymousClass2(biometricContext, lockoutResetDispatcher, provider));
        this.mLockoutCache = new LockoutCache();
        this.mAuthenticatorIds = new HashMap();
        this.mLazySession = new Supplier() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                return Sensor.this.m2368x688ef8c1();
            }
        };
        try {
            ActivityManager.getService().registerUserSwitchObserver(synchronousUserSwitchObserver, tag);
        } catch (RemoteException e) {
            Slog.e(this.mTag, "Unable to register user switch observer");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.biometrics.sensors.face.aidl.Sensor$2  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements UserAwareBiometricScheduler.UserSwitchCallback {
        final /* synthetic */ BiometricContext val$biometricContext;
        final /* synthetic */ LockoutResetDispatcher val$lockoutResetDispatcher;
        final /* synthetic */ FaceProvider val$provider;

        AnonymousClass2(BiometricContext biometricContext, LockoutResetDispatcher lockoutResetDispatcher, FaceProvider faceProvider) {
            this.val$biometricContext = biometricContext;
            this.val$lockoutResetDispatcher = lockoutResetDispatcher;
            this.val$provider = faceProvider;
        }

        @Override // com.android.server.biometrics.sensors.UserAwareBiometricScheduler.UserSwitchCallback
        public StopUserClient<?> getStopUserClient(int userId) {
            return new FaceStopUserClient(Sensor.this.mContext, Sensor.this.mLazySession, Sensor.this.mToken, userId, Sensor.this.mSensorProperties.sensorId, BiometricLogger.ofUnknown(Sensor.this.mContext), this.val$biometricContext, new StopUserClient.UserStoppedCallback() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$2$$ExternalSyntheticLambda3
                @Override // com.android.server.biometrics.sensors.StopUserClient.UserStoppedCallback
                public final void onUserStopped() {
                    Sensor.AnonymousClass2.this.m2371x900b8a02();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getStopUserClient$0$com-android-server-biometrics-sensors-face-aidl-Sensor$2  reason: not valid java name */
        public /* synthetic */ void m2371x900b8a02() {
            Sensor.this.mCurrentSession = null;
        }

        @Override // com.android.server.biometrics.sensors.UserAwareBiometricScheduler.UserSwitchCallback
        public StartUserClient<?, ?> getStartUserClient(int newUserId) {
            final int sensorId = Sensor.this.mSensorProperties.sensorId;
            final HalSessionCallback resultController = new HalSessionCallback(Sensor.this.mContext, Sensor.this.mHandler, Sensor.this.mTag, Sensor.this.mScheduler, sensorId, newUserId, Sensor.this.mLockoutCache, this.val$lockoutResetDispatcher, new HalSessionCallback.Callback() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$2$$ExternalSyntheticLambda0
                @Override // com.android.server.biometrics.sensors.face.aidl.Sensor.HalSessionCallback.Callback
                public final void onHardwareUnavailable() {
                    Sensor.AnonymousClass2.this.m2369x611b2b93();
                }
            });
            final FaceProvider faceProvider = this.val$provider;
            StartUserClient.UserStartedCallback<ISession> userStartedCallback = new StartUserClient.UserStartedCallback() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$2$$ExternalSyntheticLambda1
                @Override // com.android.server.biometrics.sensors.StartUserClient.UserStartedCallback
                public final void onUserStarted(int i, Object obj, int i2) {
                    Sensor.AnonymousClass2.this.m2370xee0842b2(resultController, sensorId, faceProvider, i, (ISession) obj, i2);
                }
            };
            Context context = Sensor.this.mContext;
            final FaceProvider faceProvider2 = this.val$provider;
            Objects.requireNonNull(faceProvider2);
            return new FaceStartUserClient(context, new Supplier() { // from class: com.android.server.biometrics.sensors.face.aidl.Sensor$2$$ExternalSyntheticLambda2
                @Override // java.util.function.Supplier
                public final Object get() {
                    return FaceProvider.this.getHalInstance();
                }
            }, Sensor.this.mToken, newUserId, Sensor.this.mSensorProperties.sensorId, BiometricLogger.ofUnknown(Sensor.this.mContext), this.val$biometricContext, resultController, userStartedCallback);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getStartUserClient$1$com-android-server-biometrics-sensors-face-aidl-Sensor$2  reason: not valid java name */
        public /* synthetic */ void m2369x611b2b93() {
            Slog.e(Sensor.this.mTag, "Got ERROR_HW_UNAVAILABLE");
            Sensor.this.mCurrentSession = null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getStartUserClient$2$com-android-server-biometrics-sensors-face-aidl-Sensor$2  reason: not valid java name */
        public /* synthetic */ void m2370xee0842b2(HalSessionCallback resultController, int sensorId, FaceProvider provider, int userIdStarted, ISession newSession, int halInterfaceVersion) {
            Slog.d(Sensor.this.mTag, "New session created for user: " + userIdStarted + " with hal version: " + halInterfaceVersion);
            Sensor.this.mCurrentSession = new AidlSession(halInterfaceVersion, newSession, userIdStarted, resultController);
            if (FaceUtils.getLegacyInstance(sensorId).isInvalidationInProgress(Sensor.this.mContext, userIdStarted)) {
                Slog.w(Sensor.this.mTag, "Scheduling unfinished invalidation request for sensor: " + sensorId + ", user: " + userIdStarted);
                provider.scheduleInvalidationRequest(sensorId, userIdStarted);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-biometrics-sensors-face-aidl-Sensor  reason: not valid java name */
    public /* synthetic */ int m2367x6758a5e2() {
        AidlSession aidlSession = this.mCurrentSession;
        if (aidlSession != null) {
            return aidlSession.getUserId();
        }
        return -10000;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-biometrics-sensors-face-aidl-Sensor  reason: not valid java name */
    public /* synthetic */ AidlSession m2368x688ef8c1() {
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
    public FaceSensorPropertiesInternal getSensorProperties() {
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
    public ITestSession createTestSession(ITestSessionCallback callback) {
        return new BiometricTestSessionImpl(this.mContext, this.mSensorProperties.sensorId, callback, this.mProvider, this);
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
        proto.write(CompanionMessage.TYPE, 2);
        proto.write(1120986464259L, Utils.getCurrentStrength(this.mSensorProperties.sensorId));
        proto.write(1146756268036L, this.mScheduler.dumpProtoState(clearSchedulerBuffer));
        for (UserInfo user : UserManager.get(this.mContext).getUsers()) {
            int userId = user.getUserHandle().getIdentifier();
            long userToken = proto.start(2246267895813L);
            proto.write(CompanionMessage.MESSAGE_ID, userId);
            proto.write(1120986464258L, FaceUtils.getInstance(this.mSensorProperties.sensorId).getBiometricsForUser(this.mContext, userId).size());
            proto.end(userToken);
        }
        proto.write(1133871366150L, this.mSensorProperties.resetLockoutRequiresHardwareAuthToken);
        proto.write(1133871366151L, this.mSensorProperties.resetLockoutRequiresChallenge);
        proto.end(sensorToken);
    }

    public void onBinderDied() {
        BaseClientMonitor client = this.mScheduler.getCurrentClient();
        if (client instanceof Interruptable) {
            Slog.e(this.mTag, "Sending ERROR_HW_UNAVAILABLE for client: " + client);
            ErrorConsumer errorConsumer = (ErrorConsumer) client;
            errorConsumer.onError(1, 0);
            FrameworkStatsLog.write(148, 4, 1, -1);
        }
        this.mScheduler.recordCrashState();
        this.mScheduler.reset();
        this.mCurrentSession = null;
    }
}
