package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.app.trust.TrustManager;
import android.content.ContentResolver;
import android.content.Context;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.hardware.fingerprint.IUdfpsOverlayController;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseBooleanArray;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.sensors.AuthenticationConsumer;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.BiometricScheduler;
import com.android.server.biometrics.sensors.BiometricStateCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.LockoutResetDispatcher;
import com.android.server.biometrics.sensors.fingerprint.GestureAvailabilityDispatcher;
import com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21;
import com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21UdfpsMock;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
/* loaded from: classes.dex */
public class Fingerprint21UdfpsMock extends Fingerprint21 implements TrustManager.TrustListener {
    private static final String CONFIG_AUTH_DELAY_PT1 = "com.android.server.biometrics.sensors.fingerprint.test_udfps.auth_delay_pt1";
    private static final String CONFIG_AUTH_DELAY_PT2 = "com.android.server.biometrics.sensors.fingerprint.test_udfps.auth_delay_pt2";
    private static final String CONFIG_AUTH_DELAY_RANDOMNESS = "com.android.server.biometrics.sensors.fingerprint.test_udfps.auth_delay_randomness";
    public static final String CONFIG_ENABLE_TEST_UDFPS = "com.android.server.biometrics.sensors.fingerprint.test_udfps.enable";
    private static final int DEFAULT_AUTH_DELAY_PT1_MS = 300;
    private static final int DEFAULT_AUTH_DELAY_PT2_MS = 400;
    private static final int DEFAULT_AUTH_DELAY_RANDOMNESS_MS = 100;
    private static final String TAG = "Fingerprint21UdfpsMock";
    private final FakeAcceptRunnable mFakeAcceptRunnable;
    private final FakeRejectRunnable mFakeRejectRunnable;
    private final Handler mHandler;
    private final MockHalResultController mMockHalResultController;
    private final Random mRandom;
    private final RestartAuthRunnable mRestartAuthRunnable;
    private final TestableBiometricScheduler mScheduler;
    private final FingerprintSensorPropertiesInternal mSensorProperties;
    private final TrustManager mTrustManager;
    private final SparseBooleanArray mUserHasTrust;

    /* loaded from: classes.dex */
    private static class TestableBiometricScheduler extends BiometricScheduler {
        private Fingerprint21UdfpsMock mFingerprint21;

        TestableBiometricScheduler(String tag, Handler handler, GestureAvailabilityDispatcher gestureAvailabilityDispatcher) {
            super(tag, 3, gestureAvailabilityDispatcher);
        }

        void init(Fingerprint21UdfpsMock fingerprint21) {
            this.mFingerprint21 = fingerprint21;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MockHalResultController extends Fingerprint21.HalResultController {
        private static final int AUTH_VALIDITY_MS = 10000;
        private Fingerprint21UdfpsMock mFingerprint21;
        private LastAuthArgs mLastAuthArgs;
        private RestartAuthRunnable mRestartAuthRunnable;

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public static class LastAuthArgs {
            final long deviceId;
            final int fingerId;
            final int groupId;
            final AuthenticationConsumer lastAuthenticatedClient;
            final ArrayList<Byte> token;

            LastAuthArgs(AuthenticationConsumer authenticationConsumer, long deviceId, int fingerId, int groupId, ArrayList<Byte> token) {
                this.lastAuthenticatedClient = authenticationConsumer;
                this.deviceId = deviceId;
                this.fingerId = fingerId;
                this.groupId = groupId;
                if (token == null) {
                    this.token = null;
                } else {
                    this.token = new ArrayList<>(token);
                }
            }
        }

        MockHalResultController(int sensorId, Context context, Handler handler, BiometricScheduler scheduler) {
            super(sensorId, context, handler, scheduler);
        }

        void init(RestartAuthRunnable restartAuthRunnable, Fingerprint21UdfpsMock fingerprint21) {
            this.mRestartAuthRunnable = restartAuthRunnable;
            this.mFingerprint21 = fingerprint21;
        }

        AuthenticationConsumer getLastAuthenticatedClient() {
            LastAuthArgs lastAuthArgs = this.mLastAuthArgs;
            if (lastAuthArgs != null) {
                return lastAuthArgs.lastAuthenticatedClient;
            }
            return null;
        }

        @Override // com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21.HalResultController, android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback
        public void onAuthenticated(final long deviceId, final int fingerId, final int groupId, final ArrayList<Byte> token) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21UdfpsMock$MockHalResultController$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Fingerprint21UdfpsMock.MockHalResultController.this.m2546x23ba4c6f(fingerId, deviceId, groupId, token);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticated$0$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21UdfpsMock$MockHalResultController  reason: not valid java name */
        public /* synthetic */ void m2546x23ba4c6f(int fingerId, long deviceId, int groupId, ArrayList token) {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof AuthenticationConsumer)) {
                Slog.e(Fingerprint21UdfpsMock.TAG, "Non authentication consumer: " + client);
                return;
            }
            boolean accepted = fingerId != 0;
            if (accepted) {
                this.mFingerprint21.setDebugMessage("Finger accepted");
            } else {
                this.mFingerprint21.setDebugMessage("Finger rejected");
            }
            AuthenticationConsumer authenticationConsumer = (AuthenticationConsumer) client;
            this.mLastAuthArgs = new LastAuthArgs(authenticationConsumer, deviceId, fingerId, groupId, token);
            this.mHandler.removeCallbacks(this.mRestartAuthRunnable);
            this.mRestartAuthRunnable.setLastAuthReference(authenticationConsumer);
            this.mHandler.postDelayed(this.mRestartAuthRunnable, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }

        void sendAuthenticated(long deviceId, int fingerId, int groupId, ArrayList<Byte> token) {
            Slog.d(Fingerprint21UdfpsMock.TAG, "sendAuthenticated: " + (fingerId != 0));
            this.mFingerprint21.setDebugMessage("Udfps match: " + (fingerId != 0));
            super.onAuthenticated(deviceId, fingerId, groupId, token);
        }
    }

    public static Fingerprint21UdfpsMock newInstance(Context context, BiometricStateCallback biometricStateCallback, FingerprintSensorPropertiesInternal sensorProps, LockoutResetDispatcher lockoutResetDispatcher, GestureAvailabilityDispatcher gestureAvailabilityDispatcher, BiometricContext biometricContext) {
        Slog.d(TAG, "Creating Fingerprint23Mock!");
        Handler handler = new Handler(BiometricScheduler.getBiometricLooper());
        TestableBiometricScheduler scheduler = new TestableBiometricScheduler(TAG, handler, gestureAvailabilityDispatcher);
        MockHalResultController controller = new MockHalResultController(sensorProps.sensorId, context, handler, scheduler);
        return new Fingerprint21UdfpsMock(context, biometricStateCallback, sensorProps, scheduler, handler, lockoutResetDispatcher, controller, biometricContext);
    }

    /* loaded from: classes.dex */
    private static abstract class FakeFingerRunnable implements Runnable {
        private int mCaptureDuration;
        private long mFingerDownTime;

        private FakeFingerRunnable() {
        }

        void setSimulationTime(long fingerDownTime, int captureDuration) {
            this.mFingerDownTime = fingerDownTime;
            this.mCaptureDuration = captureDuration;
        }

        boolean isImageCaptureComplete() {
            return System.currentTimeMillis() - this.mFingerDownTime > ((long) this.mCaptureDuration);
        }
    }

    /* loaded from: classes.dex */
    private final class FakeRejectRunnable extends FakeFingerRunnable {
        private FakeRejectRunnable() {
            super();
        }

        @Override // java.lang.Runnable
        public void run() {
            Fingerprint21UdfpsMock.this.mMockHalResultController.sendAuthenticated(0L, 0, 0, null);
        }
    }

    /* loaded from: classes.dex */
    private final class FakeAcceptRunnable extends FakeFingerRunnable {
        private FakeAcceptRunnable() {
            super();
        }

        @Override // java.lang.Runnable
        public void run() {
            if (Fingerprint21UdfpsMock.this.mMockHalResultController.mLastAuthArgs == null) {
                Slog.d(Fingerprint21UdfpsMock.TAG, "Sending fake finger");
                Fingerprint21UdfpsMock.this.mMockHalResultController.sendAuthenticated(1L, 1, 1, null);
                return;
            }
            Fingerprint21UdfpsMock.this.mMockHalResultController.sendAuthenticated(Fingerprint21UdfpsMock.this.mMockHalResultController.mLastAuthArgs.deviceId, Fingerprint21UdfpsMock.this.mMockHalResultController.mLastAuthArgs.fingerId, Fingerprint21UdfpsMock.this.mMockHalResultController.mLastAuthArgs.groupId, Fingerprint21UdfpsMock.this.mMockHalResultController.mLastAuthArgs.token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class RestartAuthRunnable implements Runnable {
        private final Fingerprint21UdfpsMock mFingerprint21;
        private AuthenticationConsumer mLastAuthConsumer;
        private final TestableBiometricScheduler mScheduler;

        RestartAuthRunnable(Fingerprint21UdfpsMock fingerprint21, TestableBiometricScheduler scheduler) {
            this.mFingerprint21 = fingerprint21;
            this.mScheduler = scheduler;
        }

        void setLastAuthReference(AuthenticationConsumer lastAuthConsumer) {
            this.mLastAuthConsumer = lastAuthConsumer;
        }

        @Override // java.lang.Runnable
        public void run() {
            BaseClientMonitor client = this.mScheduler.getCurrentClient();
            if (!(client instanceof FingerprintAuthenticationClient)) {
                Slog.e(Fingerprint21UdfpsMock.TAG, "Non-FingerprintAuthenticationClient client: " + client);
            } else if (client != this.mLastAuthConsumer) {
                Slog.e(Fingerprint21UdfpsMock.TAG, "Current client: " + client + " does not match mLastAuthConsumer: " + this.mLastAuthConsumer);
            } else {
                Slog.d(Fingerprint21UdfpsMock.TAG, "Restarting auth, current: " + client);
                this.mFingerprint21.setDebugMessage("Auth timed out");
                FingerprintAuthenticationClient authClient = (FingerprintAuthenticationClient) client;
                IBinder token = client.getToken();
                long operationId = authClient.getOperationId();
                int user = client.getTargetUserId();
                int cookie = client.getCookie();
                ClientMonitorCallbackConverter listener = client.getListener();
                String opPackageName = client.getOwnerString();
                boolean restricted = authClient.isRestricted();
                int statsClient = client.getLogger().getStatsClient();
                boolean isKeyguard = authClient.isKeyguard();
                this.mScheduler.getInternalCallback().onClientFinished(client, true);
                Fingerprint21UdfpsMock fingerprint21UdfpsMock = this.mFingerprint21;
                fingerprint21UdfpsMock.scheduleAuthenticate(fingerprint21UdfpsMock.mSensorProperties.sensorId, token, operationId, user, cookie, listener, opPackageName, restricted, statsClient, isKeyguard);
            }
        }
    }

    private Fingerprint21UdfpsMock(Context context, BiometricStateCallback biometricStateCallback, FingerprintSensorPropertiesInternal sensorProps, TestableBiometricScheduler scheduler, Handler handler, LockoutResetDispatcher lockoutResetDispatcher, MockHalResultController controller, BiometricContext biometricContext) {
        super(context, biometricStateCallback, sensorProps, scheduler, handler, lockoutResetDispatcher, controller, biometricContext);
        this.mScheduler = scheduler;
        scheduler.init(this);
        this.mHandler = handler;
        int maxTemplatesAllowed = this.mContext.getResources().getInteger(17694832);
        this.mSensorProperties = new FingerprintSensorPropertiesInternal(sensorProps.sensorId, sensorProps.sensorStrength, maxTemplatesAllowed, sensorProps.componentInfo, 3, false, false, sensorProps.getAllLocations());
        this.mMockHalResultController = controller;
        this.mUserHasTrust = new SparseBooleanArray();
        TrustManager trustManager = (TrustManager) context.getSystemService(TrustManager.class);
        this.mTrustManager = trustManager;
        trustManager.registerTrustListener(this);
        this.mRandom = new Random();
        this.mFakeRejectRunnable = new FakeRejectRunnable();
        this.mFakeAcceptRunnable = new FakeAcceptRunnable();
        RestartAuthRunnable restartAuthRunnable = new RestartAuthRunnable(this, scheduler);
        this.mRestartAuthRunnable = restartAuthRunnable;
        controller.init(restartAuthRunnable, this);
    }

    public void onTrustChanged(boolean enabled, int userId, int flags, List<String> trustGrantedMessages) {
        this.mUserHasTrust.put(userId, enabled);
    }

    public void onTrustManagedChanged(boolean enabled, int userId) {
    }

    public void onTrustError(CharSequence message) {
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21, com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public List<FingerprintSensorPropertiesInternal> getSensorProperties() {
        List<FingerprintSensorPropertiesInternal> properties = new ArrayList<>();
        properties.add(this.mSensorProperties);
        return properties;
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21, com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void onPointerDown(long requestId, int sensorId, int x, int y, float minor, float major) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21UdfpsMock$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21UdfpsMock.this.m2543x318f9721();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPointerDown$0$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21UdfpsMock  reason: not valid java name */
    public /* synthetic */ void m2543x318f9721() {
        Slog.d(TAG, "onFingerDown");
        AuthenticationConsumer lastAuthenticatedConsumer = this.mMockHalResultController.getLastAuthenticatedClient();
        BaseClientMonitor currentScheduledClient = this.mScheduler.getCurrentClient();
        if (currentScheduledClient == null) {
            Slog.d(TAG, "Not authenticating");
            return;
        }
        this.mHandler.removeCallbacks(this.mFakeRejectRunnable);
        this.mHandler.removeCallbacks(this.mFakeAcceptRunnable);
        boolean keyguardAndTrusted = true;
        boolean authenticatedClientIsCurrent = lastAuthenticatedConsumer != null && lastAuthenticatedConsumer == currentScheduledClient;
        if (currentScheduledClient instanceof FingerprintAuthenticationClient) {
            if (!((FingerprintAuthenticationClient) currentScheduledClient).isKeyguard() || !this.mUserHasTrust.get(currentScheduledClient.getTargetUserId(), false)) {
                keyguardAndTrusted = false;
            }
        } else {
            keyguardAndTrusted = false;
        }
        int captureDuration = getNewCaptureDuration();
        int matchingDuration = getMatchingDuration();
        int totalDuration = captureDuration + matchingDuration;
        setDebugMessage("Duration: " + totalDuration + " (" + captureDuration + " + " + matchingDuration + ")");
        if (authenticatedClientIsCurrent || keyguardAndTrusted) {
            this.mFakeAcceptRunnable.setSimulationTime(System.currentTimeMillis(), captureDuration);
            this.mHandler.postDelayed(this.mFakeAcceptRunnable, totalDuration);
        } else if (currentScheduledClient instanceof AuthenticationConsumer) {
            this.mFakeRejectRunnable.setSimulationTime(System.currentTimeMillis(), captureDuration);
            this.mHandler.postDelayed(this.mFakeRejectRunnable, totalDuration);
        }
    }

    @Override // com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21, com.android.server.biometrics.sensors.fingerprint.ServiceProvider
    public void onPointerUp(long requestId, int sensorId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.hidl.Fingerprint21UdfpsMock$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                Fingerprint21UdfpsMock.this.m2544xc486ad9b();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onPointerUp$1$com-android-server-biometrics-sensors-fingerprint-hidl-Fingerprint21UdfpsMock  reason: not valid java name */
    public /* synthetic */ void m2544xc486ad9b() {
        Slog.d(TAG, "onFingerUp");
        if (this.mHandler.hasCallbacks(this.mFakeRejectRunnable) && !this.mFakeRejectRunnable.isImageCaptureComplete()) {
            this.mHandler.removeCallbacks(this.mFakeRejectRunnable);
            this.mMockHalResultController.onAcquired(0L, 5, 0);
        } else if (this.mHandler.hasCallbacks(this.mFakeAcceptRunnable) && !this.mFakeAcceptRunnable.isImageCaptureComplete()) {
            this.mHandler.removeCallbacks(this.mFakeAcceptRunnable);
            this.mMockHalResultController.onAcquired(0L, 5, 0);
        }
    }

    private int getNewCaptureDuration() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        int captureTime = Settings.Secure.getIntForUser(contentResolver, CONFIG_AUTH_DELAY_PT1, 300, -2);
        int randomDelayRange = Settings.Secure.getIntForUser(contentResolver, CONFIG_AUTH_DELAY_RANDOMNESS, 100, -2);
        int randomDelay = this.mRandom.nextInt(randomDelayRange * 2) - randomDelayRange;
        return Math.max(captureTime + randomDelay, 0);
    }

    private int getMatchingDuration() {
        int matchingTime = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), CONFIG_AUTH_DELAY_PT2, 400, -2);
        return Math.max(matchingTime, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDebugMessage(String message) {
        try {
            IUdfpsOverlayController controller = getUdfpsOverlayController();
            if (controller != null) {
                Slog.d(TAG, "setDebugMessage: " + message);
                controller.setDebugMessage(this.mSensorProperties.sensorId, message);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception when sending message: " + message, e);
        }
    }
}
