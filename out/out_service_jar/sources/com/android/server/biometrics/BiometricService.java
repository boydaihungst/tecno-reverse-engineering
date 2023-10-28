package com.android.server.biometrics;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.UserSwitchObserver;
import android.app.admin.DevicePolicyManager;
import android.app.trust.ITrustManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.biometrics.IBiometricAuthenticator;
import android.hardware.biometrics.IBiometricEnabledOnKeyguardCallback;
import android.hardware.biometrics.IBiometricSensorReceiver;
import android.hardware.biometrics.IBiometricService;
import android.hardware.biometrics.IBiometricServiceReceiver;
import android.hardware.biometrics.IBiometricSysuiReceiver;
import android.hardware.biometrics.IInvalidationCallback;
import android.hardware.biometrics.ITestSession;
import android.hardware.biometrics.ITestSessionCallback;
import android.hardware.biometrics.PromptInfo;
import android.hardware.biometrics.SensorPropertiesInternal;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemService;
import com.android.server.biometrics.AuthSession;
import com.android.server.biometrics.BiometricService;
import com.android.server.biometrics.sensors.CoexCoordinator;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class BiometricService extends SystemService {
    static final String TAG = "BiometricService";
    private static final HandlerThread mBiometricHandlerThread;
    AuthSession mAuthSession;
    BiometricStrengthController mBiometricStrengthController;
    private final DevicePolicyManager mDevicePolicyManager;
    private final CopyOnWriteArrayList<EnabledOnKeyguardCallback> mEnabledOnKeyguardCallbacks;
    private final Handler mHandler;
    final IBiometricService.Stub mImpl;
    private final Injector mInjector;
    KeyStore mKeyStore;
    private final Random mRandom;
    private final Supplier<Long> mRequestCounter;
    final ArrayList<BiometricSensor> mSensors;
    final SettingObserver mSettingObserver;
    IStatusBarService mStatusBarService;
    ITrustManager mTrustManager;

    static {
        HandlerThread handlerThread = new HandlerThread("BiometricserviceThread");
        mBiometricHandlerThread = handlerThread;
        handlerThread.start();
    }

    public static Looper getBiometricLooper() {
        return mBiometricHandlerThread.getLooper();
    }

    /* loaded from: classes.dex */
    static class InvalidationTracker {
        private final IInvalidationCallback mClientCallback;
        private final Set<Integer> mSensorsPendingInvalidation = new ArraySet();

        public static InvalidationTracker start(Context context, ArrayList<BiometricSensor> sensors, int userId, int fromSensorId, IInvalidationCallback clientCallback) {
            return new InvalidationTracker(context, sensors, userId, fromSensorId, clientCallback);
        }

        private InvalidationTracker(Context context, ArrayList<BiometricSensor> sensors, int userId, int fromSensorId, IInvalidationCallback clientCallback) {
            this.mClientCallback = clientCallback;
            Iterator<BiometricSensor> it = sensors.iterator();
            while (it.hasNext()) {
                final BiometricSensor sensor = it.next();
                if (sensor.id != fromSensorId && Utils.isAtLeastStrength(sensor.oemStrength, 15)) {
                    try {
                    } catch (RemoteException e) {
                        Slog.e(BiometricService.TAG, "Remote Exception", e);
                    }
                    if (sensor.impl.hasEnrolledTemplates(userId, context.getOpPackageName())) {
                        Slog.d(BiometricService.TAG, "Requesting authenticatorId invalidation for sensor: " + sensor.id);
                        synchronized (this) {
                            this.mSensorsPendingInvalidation.add(Integer.valueOf(sensor.id));
                        }
                        try {
                            sensor.impl.invalidateAuthenticatorId(userId, new IInvalidationCallback.Stub() { // from class: com.android.server.biometrics.BiometricService.InvalidationTracker.1
                                public void onCompleted() {
                                    InvalidationTracker.this.onInvalidated(sensor.id);
                                }
                            });
                        } catch (RemoteException e2) {
                            Slog.d(BiometricService.TAG, "RemoteException", e2);
                        }
                    } else {
                        continue;
                    }
                }
            }
            synchronized (this) {
                if (this.mSensorsPendingInvalidation.isEmpty()) {
                    try {
                        Slog.d(BiometricService.TAG, "No sensors require invalidation");
                        this.mClientCallback.onCompleted();
                    } catch (RemoteException e3) {
                        Slog.e(BiometricService.TAG, "Remote Exception", e3);
                    }
                }
            }
        }

        void onInvalidated(int sensorId) {
            synchronized (this) {
                this.mSensorsPendingInvalidation.remove(Integer.valueOf(sensorId));
                Slog.d(BiometricService.TAG, "Sensor " + sensorId + " invalidated, remaining size: " + this.mSensorsPendingInvalidation.size());
                if (this.mSensorsPendingInvalidation.isEmpty()) {
                    try {
                        this.mClientCallback.onCompleted();
                    } catch (RemoteException e) {
                        Slog.e(BiometricService.TAG, "Remote Exception", e);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static class SettingObserver extends ContentObserver {
        private static final boolean DEFAULT_ALWAYS_REQUIRE_CONFIRMATION = false;
        private static final boolean DEFAULT_APP_ENABLED = true;
        private static final boolean DEFAULT_KEYGUARD_ENABLED = true;
        private final Uri BIOMETRIC_APP_ENABLED;
        private final Uri BIOMETRIC_KEYGUARD_ENABLED;
        private final Uri FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION;
        private final Uri FACE_UNLOCK_APP_ENABLED;
        private final Uri FACE_UNLOCK_KEYGUARD_ENABLED;
        private final Map<Integer, Boolean> mBiometricEnabledForApps;
        private final Map<Integer, Boolean> mBiometricEnabledOnKeyguard;
        private final CopyOnWriteArrayList<EnabledOnKeyguardCallback> mCallbacks;
        private final ContentResolver mContentResolver;
        private final Map<Integer, Boolean> mFaceAlwaysRequireConfirmation;
        private final boolean mUseLegacyFaceOnlySettings;

        public SettingObserver(Context context, Handler handler, List<EnabledOnKeyguardCallback> callbacks) {
            super(handler);
            this.FACE_UNLOCK_KEYGUARD_ENABLED = Settings.Secure.getUriFor("face_unlock_keyguard_enabled");
            this.FACE_UNLOCK_APP_ENABLED = Settings.Secure.getUriFor("face_unlock_app_enabled");
            this.FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION = Settings.Secure.getUriFor("face_unlock_always_require_confirmation");
            this.BIOMETRIC_KEYGUARD_ENABLED = Settings.Secure.getUriFor("biometric_keyguard_enabled");
            this.BIOMETRIC_APP_ENABLED = Settings.Secure.getUriFor("biometric_app_enabled");
            this.mBiometricEnabledOnKeyguard = new HashMap();
            this.mBiometricEnabledForApps = new HashMap();
            this.mFaceAlwaysRequireConfirmation = new HashMap();
            this.mContentResolver = context.getContentResolver();
            this.mCallbacks = (CopyOnWriteArrayList) callbacks;
            boolean hasFingerprint = context.getPackageManager().hasSystemFeature("android.hardware.fingerprint");
            boolean hasFace = context.getPackageManager().hasSystemFeature("android.hardware.biometrics.face");
            this.mUseLegacyFaceOnlySettings = Build.VERSION.DEVICE_INITIAL_SDK_INT <= 29 && hasFace && !hasFingerprint;
            updateContentObserver();
        }

        public void updateContentObserver() {
            this.mContentResolver.unregisterContentObserver(this);
            if (this.mUseLegacyFaceOnlySettings) {
                this.mContentResolver.registerContentObserver(this.FACE_UNLOCK_KEYGUARD_ENABLED, false, this, -1);
                this.mContentResolver.registerContentObserver(this.FACE_UNLOCK_APP_ENABLED, false, this, -1);
            } else {
                this.mContentResolver.registerContentObserver(this.BIOMETRIC_KEYGUARD_ENABLED, false, this, -1);
                this.mContentResolver.registerContentObserver(this.BIOMETRIC_APP_ENABLED, false, this, -1);
            }
            this.mContentResolver.registerContentObserver(this.FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION, false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.FACE_UNLOCK_KEYGUARD_ENABLED.equals(uri)) {
                this.mBiometricEnabledOnKeyguard.put(Integer.valueOf(userId), Boolean.valueOf(Settings.Secure.getIntForUser(this.mContentResolver, "face_unlock_keyguard_enabled", 1, userId) != 0));
                if (userId == ActivityManager.getCurrentUser() && !selfChange) {
                    notifyEnabledOnKeyguardCallbacks(userId);
                }
            } else if (this.FACE_UNLOCK_APP_ENABLED.equals(uri)) {
                this.mBiometricEnabledForApps.put(Integer.valueOf(userId), Boolean.valueOf(Settings.Secure.getIntForUser(this.mContentResolver, "face_unlock_app_enabled", 1, userId) != 0));
            } else if (this.FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION.equals(uri)) {
                this.mFaceAlwaysRequireConfirmation.put(Integer.valueOf(userId), Boolean.valueOf(Settings.Secure.getIntForUser(this.mContentResolver, "face_unlock_always_require_confirmation", 0, userId) != 0));
            } else if (this.BIOMETRIC_KEYGUARD_ENABLED.equals(uri)) {
                this.mBiometricEnabledOnKeyguard.put(Integer.valueOf(userId), Boolean.valueOf(Settings.Secure.getIntForUser(this.mContentResolver, "biometric_keyguard_enabled", 1, userId) != 0));
                if (userId == ActivityManager.getCurrentUser() && !selfChange) {
                    notifyEnabledOnKeyguardCallbacks(userId);
                }
            } else if (this.BIOMETRIC_APP_ENABLED.equals(uri)) {
                this.mBiometricEnabledForApps.put(Integer.valueOf(userId), Boolean.valueOf(Settings.Secure.getIntForUser(this.mContentResolver, "biometric_app_enabled", 1, userId) != 0));
            }
        }

        public boolean getEnabledOnKeyguard(int userId) {
            if (!this.mBiometricEnabledOnKeyguard.containsKey(Integer.valueOf(userId))) {
                if (this.mUseLegacyFaceOnlySettings) {
                    onChange(true, this.FACE_UNLOCK_KEYGUARD_ENABLED, userId);
                } else {
                    onChange(true, this.BIOMETRIC_KEYGUARD_ENABLED, userId);
                }
            }
            return this.mBiometricEnabledOnKeyguard.get(Integer.valueOf(userId)).booleanValue();
        }

        public boolean getEnabledForApps(int userId) {
            if (!this.mBiometricEnabledForApps.containsKey(Integer.valueOf(userId))) {
                if (this.mUseLegacyFaceOnlySettings) {
                    onChange(true, this.FACE_UNLOCK_APP_ENABLED, userId);
                } else {
                    onChange(true, this.BIOMETRIC_APP_ENABLED, userId);
                }
            }
            return this.mBiometricEnabledForApps.getOrDefault(Integer.valueOf(userId), true).booleanValue();
        }

        public boolean getConfirmationAlwaysRequired(int modality, int userId) {
            switch (modality) {
                case 8:
                    if (!this.mFaceAlwaysRequireConfirmation.containsKey(Integer.valueOf(userId))) {
                        onChange(true, this.FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION, userId);
                    }
                    return this.mFaceAlwaysRequireConfirmation.get(Integer.valueOf(userId)).booleanValue();
                default:
                    return false;
            }
        }

        void notifyEnabledOnKeyguardCallbacks(int userId) {
            CopyOnWriteArrayList<EnabledOnKeyguardCallback> callbacks = this.mCallbacks;
            for (int i = 0; i < callbacks.size(); i++) {
                callbacks.get(i).notify(this.mBiometricEnabledOnKeyguard.getOrDefault(Integer.valueOf(userId), true).booleanValue(), userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class EnabledOnKeyguardCallback implements IBinder.DeathRecipient {
        private final IBiometricEnabledOnKeyguardCallback mCallback;

        EnabledOnKeyguardCallback(IBiometricEnabledOnKeyguardCallback callback) {
            this.mCallback = callback;
            try {
                callback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.w(BiometricService.TAG, "Unable to linkToDeath", e);
            }
        }

        void notify(boolean enabled, int userId) {
            try {
                this.mCallback.onChanged(enabled, userId);
            } catch (DeadObjectException e) {
                Slog.w(BiometricService.TAG, "Death while invoking notify", e);
                BiometricService.this.mEnabledOnKeyguardCallbacks.remove(this);
            } catch (RemoteException e2) {
                Slog.w(BiometricService.TAG, "Failed to invoke onChanged", e2);
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.e(BiometricService.TAG, "Enabled callback binder died");
            BiometricService.this.mEnabledOnKeyguardCallbacks.remove(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.biometrics.BiometricService$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends IBiometricSensorReceiver.Stub {
        final /* synthetic */ long val$requestId;

        AnonymousClass1(long j) {
            this.val$requestId = j;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticationSucceeded$0$com-android-server-biometrics-BiometricService$1  reason: not valid java name */
        public /* synthetic */ void m2276xcd0139a7(long requestId, int sensorId, byte[] token) {
            BiometricService.this.handleAuthenticationSucceeded(requestId, sensorId, token);
        }

        public void onAuthenticationSucceeded(final int sensorId, final byte[] token) {
            Handler handler = BiometricService.this.mHandler;
            final long j = this.val$requestId;
            handler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$1$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.AnonymousClass1.this.m2276xcd0139a7(j, sensorId, token);
                }
            });
        }

        public void onAuthenticationFailed(final int sensorId) {
            Slog.v(BiometricService.TAG, "onAuthenticationFailed");
            Handler handler = BiometricService.this.mHandler;
            final long j = this.val$requestId;
            handler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.AnonymousClass1.this.m2275x40f019c2(j, sensorId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAuthenticationFailed$1$com-android-server-biometrics-BiometricService$1  reason: not valid java name */
        public /* synthetic */ void m2275x40f019c2(long requestId, int sensorId) {
            BiometricService.this.handleAuthenticationRejected(requestId, sensorId);
        }

        public void onError(final int sensorId, final int cookie, final int error, final int vendorCode) {
            if (error == 3) {
                Handler handler = BiometricService.this.mHandler;
                final long j = this.val$requestId;
                handler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$1$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        BiometricService.AnonymousClass1.this.m2277xf18b9f46(j, sensorId, cookie, error, vendorCode);
                    }
                });
                return;
            }
            Handler handler2 = BiometricService.this.mHandler;
            final long j2 = this.val$requestId;
            handler2.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.AnonymousClass1.this.m2278xe3354565(j2, sensorId, cookie, error, vendorCode);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onError$2$com-android-server-biometrics-BiometricService$1  reason: not valid java name */
        public /* synthetic */ void m2277xf18b9f46(long requestId, int sensorId, int cookie, int error, int vendorCode) {
            BiometricService.this.handleAuthenticationTimedOut(requestId, sensorId, cookie, error, vendorCode);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onError$3$com-android-server-biometrics-BiometricService$1  reason: not valid java name */
        public /* synthetic */ void m2278xe3354565(long requestId, int sensorId, int cookie, int error, int vendorCode) {
            BiometricService.this.handleOnError(requestId, sensorId, cookie, error, vendorCode);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAcquired$4$com-android-server-biometrics-BiometricService$1  reason: not valid java name */
        public /* synthetic */ void m2274x392cf806(long requestId, int sensorId, int acquiredInfo, int vendorCode) {
            BiometricService.this.handleOnAcquired(requestId, sensorId, acquiredInfo, vendorCode);
        }

        public void onAcquired(final int sensorId, final int acquiredInfo, final int vendorCode) {
            Handler handler = BiometricService.this.mHandler;
            final long j = this.val$requestId;
            handler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$1$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.AnonymousClass1.this.m2274x392cf806(j, sensorId, acquiredInfo, vendorCode);
                }
            });
        }
    }

    private IBiometricSensorReceiver createBiometricSensorReceiver(long requestId) {
        return new AnonymousClass1(requestId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.biometrics.BiometricService$2  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends IBiometricSysuiReceiver.Stub {
        final /* synthetic */ long val$requestId;

        AnonymousClass2(long j) {
            this.val$requestId = j;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDialogDismissed$0$com-android-server-biometrics-BiometricService$2  reason: not valid java name */
        public /* synthetic */ void m2281x538f2130(long requestId, int reason, byte[] credentialAttestation) {
            BiometricService.this.handleOnDismissed(requestId, reason, credentialAttestation);
        }

        public void onDialogDismissed(final int reason, final byte[] credentialAttestation) {
            Handler handler = BiometricService.this.mHandler;
            final long j = this.val$requestId;
            handler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$2$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.AnonymousClass2.this.m2281x538f2130(j, reason, credentialAttestation);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTryAgainPressed$1$com-android-server-biometrics-BiometricService$2  reason: not valid java name */
        public /* synthetic */ void m2283x37ee8413(long requestId) {
            BiometricService.this.handleOnTryAgainPressed(requestId);
        }

        public void onTryAgainPressed() {
            Handler handler = BiometricService.this.mHandler;
            final long j = this.val$requestId;
            handler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.AnonymousClass2.this.m2283x37ee8413(j);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDeviceCredentialPressed$2$com-android-server-biometrics-BiometricService$2  reason: not valid java name */
        public /* synthetic */ void m2279xf0910d1a(long requestId) {
            BiometricService.this.handleOnDeviceCredentialPressed(requestId);
        }

        public void onDeviceCredentialPressed() {
            Handler handler = BiometricService.this.mHandler;
            final long j = this.val$requestId;
            handler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$2$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.AnonymousClass2.this.m2279xf0910d1a(j);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSystemEvent$3$com-android-server-biometrics-BiometricService$2  reason: not valid java name */
        public /* synthetic */ void m2282xb5b67fe3(long requestId, int event) {
            BiometricService.this.handleOnSystemEvent(requestId, event);
        }

        public void onSystemEvent(final int event) {
            Handler handler = BiometricService.this.mHandler;
            final long j = this.val$requestId;
            handler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.AnonymousClass2.this.m2282xb5b67fe3(j, event);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDialogAnimatedIn$4$com-android-server-biometrics-BiometricService$2  reason: not valid java name */
        public /* synthetic */ void m2280xb538a585(long requestId) {
            BiometricService.this.handleOnDialogAnimatedIn(requestId);
        }

        public void onDialogAnimatedIn() {
            Handler handler = BiometricService.this.mHandler;
            final long j = this.val$requestId;
            handler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$2$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.AnonymousClass2.this.m2280xb538a585(j);
                }
            });
        }
    }

    private IBiometricSysuiReceiver createSysuiReceiver(long requestId) {
        return new AnonymousClass2(requestId);
    }

    private AuthSession.ClientDeathReceiver createClientDeathReceiver(final long requestId) {
        return new AuthSession.ClientDeathReceiver() { // from class: com.android.server.biometrics.BiometricService$$ExternalSyntheticLambda2
            @Override // com.android.server.biometrics.AuthSession.ClientDeathReceiver
            public final void onClientDied() {
                BiometricService.this.m2272x592d0dc7(requestId);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createClientDeathReceiver$1$com-android-server-biometrics-BiometricService  reason: not valid java name */
    public /* synthetic */ void m2272x592d0dc7(final long requestId) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                BiometricService.this.m2271x1f626be8(requestId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class BiometricServiceWrapper extends IBiometricService.Stub {
        private BiometricServiceWrapper() {
        }

        public ITestSession createTestSession(int sensorId, ITestSessionCallback callback, String opPackageName) throws RemoteException {
            BiometricService.this.checkInternalPermission();
            Iterator<BiometricSensor> it = BiometricService.this.mSensors.iterator();
            while (it.hasNext()) {
                BiometricSensor sensor = it.next();
                if (sensor.id == sensorId) {
                    return sensor.impl.createTestSession(callback, opPackageName);
                }
            }
            Slog.e(BiometricService.TAG, "Unknown sensor for createTestSession: " + sensorId);
            return null;
        }

        public List<SensorPropertiesInternal> getSensorProperties(String opPackageName) throws RemoteException {
            BiometricService.this.checkInternalPermission();
            List<SensorPropertiesInternal> sensors = new ArrayList<>();
            Iterator<BiometricSensor> it = BiometricService.this.mSensors.iterator();
            while (it.hasNext()) {
                BiometricSensor sensor = it.next();
                SensorPropertiesInternal prop = SensorPropertiesInternal.from(sensor.impl.getSensorProperties(opPackageName));
                sensors.add(prop);
            }
            return sensors;
        }

        public void onReadyForAuthentication(final long requestId, final int cookie) {
            BiometricService.this.checkInternalPermission();
            BiometricService.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$BiometricServiceWrapper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.BiometricServiceWrapper.this.m2286xa7528e7f(requestId, cookie);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReadyForAuthentication$0$com-android-server-biometrics-BiometricService$BiometricServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m2286xa7528e7f(long requestId, int cookie) {
            BiometricService.this.handleOnReadyForAuthentication(requestId, cookie);
        }

        public long authenticate(final IBinder token, final long operationId, final int userId, final IBiometricServiceReceiver receiver, final String opPackageName, final PromptInfo promptInfo) {
            BiometricService.this.checkInternalPermission();
            if (token == null || receiver == null || opPackageName == null || promptInfo == null) {
                Slog.e(BiometricService.TAG, "Unable to authenticate, one or more null arguments");
                return -1L;
            } else if (!Utils.isValidAuthenticatorConfig(promptInfo)) {
                throw new SecurityException("Invalid authenticator configuration");
            } else {
                Utils.combineAuthenticatorBundles(promptInfo);
                if (promptInfo.isUseDefaultTitle() && TextUtils.isEmpty(promptInfo.getTitle())) {
                    promptInfo.setTitle(BiometricService.this.getContext().getString(17039804));
                }
                final long requestId = ((Long) BiometricService.this.mRequestCounter.get()).longValue();
                BiometricService.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$BiometricServiceWrapper$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        BiometricService.BiometricServiceWrapper.this.m2284x9564cd06(token, requestId, operationId, userId, receiver, opPackageName, promptInfo);
                    }
                });
                return requestId;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$authenticate$1$com-android-server-biometrics-BiometricService$BiometricServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m2284x9564cd06(IBinder token, long requestId, long operationId, int userId, IBiometricServiceReceiver receiver, String opPackageName, PromptInfo promptInfo) {
            BiometricService.this.handleAuthenticate(token, requestId, operationId, userId, receiver, opPackageName, promptInfo);
        }

        public void cancelAuthentication(IBinder token, String opPackageName, final long requestId) {
            BiometricService.this.checkInternalPermission();
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = token;
            args.arg2 = opPackageName;
            args.arg3 = Long.valueOf(requestId);
            BiometricService.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$BiometricServiceWrapper$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    BiometricService.BiometricServiceWrapper.this.m2285x77c402c8(requestId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$cancelAuthentication$2$com-android-server-biometrics-BiometricService$BiometricServiceWrapper  reason: not valid java name */
        public /* synthetic */ void m2285x77c402c8(long requestId) {
            BiometricService.this.handleCancelAuthentication(requestId);
        }

        public int canAuthenticate(String opPackageName, int userId, int callingUserId, int authenticators) {
            BiometricService.this.checkInternalPermission();
            Slog.d(BiometricService.TAG, "canAuthenticate: User=" + userId + ", Caller=" + callingUserId + ", Authenticators=" + authenticators);
            if (!Utils.isValidAuthenticatorConfig(authenticators)) {
                throw new SecurityException("Invalid authenticator configuration");
            }
            try {
                PreAuthInfo preAuthInfo = BiometricService.this.createPreAuthInfo(opPackageName, userId, authenticators);
                return preAuthInfo.getCanAuthenticateResult();
            } catch (RemoteException e) {
                Slog.e(BiometricService.TAG, "Remote exception", e);
                return 1;
            }
        }

        public boolean hasEnrolledBiometrics(int userId, String opPackageName) {
            BiometricService.this.checkInternalPermission();
            try {
                Iterator<BiometricSensor> it = BiometricService.this.mSensors.iterator();
                while (it.hasNext()) {
                    BiometricSensor sensor = it.next();
                    if (sensor.impl.hasEnrolledTemplates(userId, opPackageName)) {
                        return true;
                    }
                }
                return false;
            } catch (RemoteException e) {
                Slog.e(BiometricService.TAG, "Remote exception", e);
                return false;
            }
        }

        public synchronized void registerAuthenticator(int id, int modality, int strength, IBiometricAuthenticator authenticator) {
            BiometricService.this.checkInternalPermission();
            Slog.d(BiometricService.TAG, "Registering ID: " + id + " Modality: " + modality + " Strength: " + strength);
            if (authenticator == null) {
                throw new IllegalArgumentException("Authenticator must not be null. Did you forget to modify the core/res/res/values/xml overlay for config_biometric_sensors?");
            }
            if (strength != 15 && strength != 255 && strength != 4095) {
                throw new IllegalStateException("Unsupported strength");
            }
            Iterator<BiometricSensor> it = BiometricService.this.mSensors.iterator();
            while (it.hasNext()) {
                BiometricSensor sensor = it.next();
                if (sensor.id == id) {
                    throw new IllegalStateException("Cannot register duplicate authenticator");
                }
            }
            BiometricService.this.mSensors.add(new BiometricSensor(BiometricService.this.getContext(), id, modality, strength, authenticator) { // from class: com.android.server.biometrics.BiometricService.BiometricServiceWrapper.1
                /* JADX INFO: Access modifiers changed from: package-private */
                @Override // com.android.server.biometrics.BiometricSensor
                public boolean confirmationAlwaysRequired(int userId) {
                    return BiometricService.this.mSettingObserver.getConfirmationAlwaysRequired(this.modality, userId);
                }

                /* JADX INFO: Access modifiers changed from: package-private */
                @Override // com.android.server.biometrics.BiometricSensor
                public boolean confirmationSupported() {
                    return Utils.isConfirmationSupported(this.modality);
                }
            });
            BiometricService.this.mBiometricStrengthController.updateStrengths();
        }

        public void registerEnabledOnKeyguardCallback(IBiometricEnabledOnKeyguardCallback callback, int callingUserId) {
            BiometricService.this.checkInternalPermission();
            BiometricService.this.mEnabledOnKeyguardCallbacks.add(new EnabledOnKeyguardCallback(callback));
            try {
                callback.onChanged(BiometricService.this.mSettingObserver.getEnabledOnKeyguard(callingUserId), callingUserId);
            } catch (RemoteException e) {
                Slog.w(BiometricService.TAG, "Remote exception", e);
            }
        }

        public void invalidateAuthenticatorIds(int userId, int fromSensorId, IInvalidationCallback callback) {
            BiometricService.this.checkInternalPermission();
            InvalidationTracker.start(BiometricService.this.getContext(), BiometricService.this.mSensors, userId, fromSensorId, callback);
        }

        public long[] getAuthenticatorIds(int callingUserId) {
            BiometricService.this.checkInternalPermission();
            List<Long> authenticatorIds = new ArrayList<>();
            Iterator<BiometricSensor> it = BiometricService.this.mSensors.iterator();
            while (it.hasNext()) {
                BiometricSensor sensor = it.next();
                try {
                    boolean hasEnrollments = sensor.impl.hasEnrolledTemplates(callingUserId, BiometricService.this.getContext().getOpPackageName());
                    long authenticatorId = sensor.impl.getAuthenticatorId(callingUserId);
                    if (!hasEnrollments || !Utils.isAtLeastStrength(sensor.getCurrentStrength(), 15)) {
                        Slog.d(BiometricService.TAG, "Sensor " + sensor + ", sensorId " + sensor.id + ", hasEnrollments: " + hasEnrollments + " cannot participate in Keystore operations");
                    } else {
                        authenticatorIds.add(Long.valueOf(authenticatorId));
                    }
                } catch (RemoteException e) {
                    Slog.e(BiometricService.TAG, "RemoteException", e);
                }
            }
            long[] result = new long[authenticatorIds.size()];
            for (int i = 0; i < authenticatorIds.size(); i++) {
                result[i] = authenticatorIds.get(i).longValue();
            }
            return result;
        }

        public void resetLockoutTimeBound(IBinder token, String opPackageName, int fromSensorId, int userId, byte[] hardwareAuthToken) {
            BiometricService.this.checkInternalPermission();
            if (!Utils.isAtLeastStrength(BiometricService.this.getSensorForId(fromSensorId).getCurrentStrength(), 15)) {
                Slog.w(BiometricService.TAG, "Sensor: " + fromSensorId + " is does not meet the required strength to request resetLockout");
                return;
            }
            Iterator<BiometricSensor> it = BiometricService.this.mSensors.iterator();
            while (it.hasNext()) {
                BiometricSensor sensor = it.next();
                if (sensor.id != fromSensorId) {
                    try {
                        SensorPropertiesInternal props = sensor.impl.getSensorProperties(BiometricService.this.getContext().getOpPackageName());
                        boolean supportsChallengelessHat = props.resetLockoutRequiresHardwareAuthToken && !props.resetLockoutRequiresChallenge;
                        boolean doesNotRequireHat = true ^ props.resetLockoutRequiresHardwareAuthToken;
                        if (supportsChallengelessHat || doesNotRequireHat) {
                            Slog.d(BiometricService.TAG, "resetLockout from: " + fromSensorId + ", for: " + sensor.id + ", userId: " + userId);
                            sensor.impl.resetLockout(token, opPackageName, userId, hardwareAuthToken);
                        }
                    } catch (RemoteException e) {
                        Slog.e(BiometricService.TAG, "Remote exception", e);
                    }
                }
            }
        }

        public int getCurrentStrength(int sensorId) {
            BiometricService.this.checkInternalPermission();
            Iterator<BiometricSensor> it = BiometricService.this.mSensors.iterator();
            while (it.hasNext()) {
                BiometricSensor sensor = it.next();
                if (sensor.id == sensorId) {
                    return sensor.getCurrentStrength();
                }
            }
            Slog.e(BiometricService.TAG, "Unknown sensorId: " + sensorId);
            return 0;
        }

        public int getCurrentModality(String opPackageName, int userId, int callingUserId, int authenticators) {
            BiometricService.this.checkInternalPermission();
            Slog.d(BiometricService.TAG, "getCurrentModality: User=" + userId + ", Caller=" + callingUserId + ", Authenticators=" + authenticators);
            if (!Utils.isValidAuthenticatorConfig(authenticators)) {
                throw new SecurityException("Invalid authenticator configuration");
            }
            try {
                PreAuthInfo preAuthInfo = BiometricService.this.createPreAuthInfo(opPackageName, userId, authenticators);
                return ((Integer) preAuthInfo.getPreAuthenticateStatus().first).intValue();
            } catch (RemoteException e) {
                Slog.e(BiometricService.TAG, "Remote exception", e);
                return 0;
            }
        }

        public int getSupportedModalities(int authenticators) {
            int modality;
            BiometricService.this.checkInternalPermission();
            Slog.d(BiometricService.TAG, "getSupportedModalities: Authenticators=" + authenticators);
            if (!Utils.isValidAuthenticatorConfig(authenticators)) {
                throw new SecurityException("Invalid authenticator configuration");
            }
            if (Utils.isCredentialRequested(authenticators)) {
                modality = 1;
            } else {
                modality = 0;
            }
            if (Utils.isBiometricRequested(authenticators)) {
                int requestedStrength = Utils.getPublicBiometricStrength(authenticators);
                Iterator<BiometricSensor> it = BiometricService.this.mSensors.iterator();
                while (it.hasNext()) {
                    BiometricSensor sensor = it.next();
                    int sensorStrength = sensor.getCurrentStrength();
                    if (Utils.isAtLeastStrength(sensorStrength, requestedStrength)) {
                        modality |= sensor.modality;
                    }
                }
            }
            return modality;
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(BiometricService.this.getContext(), BiometricService.TAG, pw)) {
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                } catch (RemoteException e) {
                    Slog.e(BiometricService.TAG, "Remote exception", e);
                }
                if (args.length > 0) {
                    if ("--proto".equals(args[0])) {
                        boolean z = true;
                        if (args.length <= 1 || !"--clear-scheduler-buffer".equals(args[1])) {
                            z = false;
                        }
                        boolean clearSchedulerBuffer = z;
                        Slog.d(BiometricService.TAG, "ClearSchedulerBuffer: " + clearSchedulerBuffer);
                        ProtoOutputStream proto = new ProtoOutputStream(fd);
                        proto.write(CompanionMessage.TYPE, BiometricService.this.mAuthSession != null ? BiometricService.this.mAuthSession.getState() : 0);
                        Iterator<BiometricSensor> it = BiometricService.this.mSensors.iterator();
                        while (it.hasNext()) {
                            BiometricSensor sensor = it.next();
                            byte[] serviceState = sensor.impl.dumpSensorServiceStateProto(clearSchedulerBuffer);
                            proto.write(CompanionAppsPermissions.APP_PERMISSIONS, serviceState);
                        }
                        proto.flush();
                    }
                }
                BiometricService.this.dumpInternal(pw);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkInternalPermission() {
        getContext().enforceCallingOrSelfPermission("android.permission.USE_BIOMETRIC_INTERNAL", "Must have USE_BIOMETRIC_INTERNAL permission");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PreAuthInfo createPreAuthInfo(String opPackageName, int userId, int authenticators) throws RemoteException {
        PromptInfo promptInfo = new PromptInfo();
        promptInfo.setAuthenticators(authenticators);
        return PreAuthInfo.create(this.mTrustManager, this.mDevicePolicyManager, this.mSettingObserver, this.mSensors, userId, promptInfo, opPackageName, false, getContext());
    }

    /* loaded from: classes.dex */
    public static class Injector {
        public IActivityManager getActivityManagerService() {
            return ActivityManager.getService();
        }

        public ITrustManager getTrustManager() {
            return ITrustManager.Stub.asInterface(ServiceManager.getService("trust"));
        }

        public IStatusBarService getStatusBarService() {
            return IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        }

        public SettingObserver getSettingObserver(Context context, Handler handler, List<EnabledOnKeyguardCallback> callbacks) {
            return new SettingObserver(context, handler, callbacks);
        }

        public KeyStore getKeyStore() {
            return KeyStore.getInstance();
        }

        public boolean isDebugEnabled(Context context, int userId) {
            return Utils.isDebugEnabled(context, userId);
        }

        public void publishBinderService(BiometricService service, IBiometricService.Stub impl) {
            service.publishBinderService("biometric", impl);
        }

        public BiometricStrengthController getBiometricStrengthController(BiometricService service) {
            return new BiometricStrengthController(service);
        }

        public String[] getConfiguration(Context context) {
            return context.getResources().getStringArray(17236004);
        }

        public DevicePolicyManager getDevicePolicyManager(Context context) {
            return (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        }

        public List<FingerprintSensorPropertiesInternal> getFingerprintSensorProperties(Context context) {
            FingerprintManager fpm;
            if (context.getPackageManager().hasSystemFeature("android.hardware.fingerprint") && (fpm = (FingerprintManager) context.getSystemService(FingerprintManager.class)) != null) {
                return fpm.getSensorPropertiesInternal();
            }
            return new ArrayList();
        }

        public boolean isAdvancedCoexLogicEnabled(Context context) {
            return Settings.Secure.getInt(context.getContentResolver(), CoexCoordinator.SETTING_ENABLE_NAME, 1) != 0;
        }

        public boolean isCoexFaceNonBypassHapticsDisabled(Context context) {
            return Settings.Secure.getInt(context.getContentResolver(), CoexCoordinator.FACE_HAPTIC_DISABLE, 1) != 0;
        }

        public Supplier<Long> getRequestGenerator() {
            final AtomicLong generator = new AtomicLong(0L);
            return new Supplier() { // from class: com.android.server.biometrics.BiometricService$Injector$$ExternalSyntheticLambda0
                @Override // java.util.function.Supplier
                public final Object get() {
                    Long valueOf;
                    valueOf = Long.valueOf(generator.incrementAndGet());
                    return valueOf;
                }
            };
        }
    }

    public BiometricService(Context context) {
        this(context, new Injector());
    }

    BiometricService(Context context, Injector injector) {
        super(context);
        this.mRandom = new Random();
        this.mSensors = new ArrayList<>();
        Handler handler = new Handler(getBiometricLooper());
        this.mHandler = handler;
        this.mInjector = injector;
        this.mDevicePolicyManager = injector.getDevicePolicyManager(context);
        this.mImpl = new BiometricServiceWrapper();
        CopyOnWriteArrayList<EnabledOnKeyguardCallback> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        this.mEnabledOnKeyguardCallbacks = copyOnWriteArrayList;
        this.mSettingObserver = injector.getSettingObserver(context, handler, copyOnWriteArrayList);
        this.mRequestCounter = injector.getRequestGenerator();
        CoexCoordinator coexCoordinator = CoexCoordinator.getInstance();
        coexCoordinator.setAdvancedLogicEnabled(injector.isAdvancedCoexLogicEnabled(context));
        coexCoordinator.setFaceHapticDisabledWhenNonBypass(injector.isCoexFaceNonBypassHapticsDisabled(context));
        try {
            injector.getActivityManagerService().registerUserSwitchObserver(new UserSwitchObserver() { // from class: com.android.server.biometrics.BiometricService.3
                public void onUserSwitchComplete(int newUserId) {
                    BiometricService.this.mSettingObserver.updateContentObserver();
                    BiometricService.this.mSettingObserver.notifyEnabledOnKeyguardCallbacks(newUserId);
                }
            }, BiometricService.class.getName());
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to register user switch observer", e);
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        this.mKeyStore = this.mInjector.getKeyStore();
        this.mStatusBarService = this.mInjector.getStatusBarService();
        this.mTrustManager = this.mInjector.getTrustManager();
        this.mInjector.publishBinderService(this, this.mImpl);
        BiometricStrengthController biometricStrengthController = this.mInjector.getBiometricStrengthController(this);
        this.mBiometricStrengthController = biometricStrengthController;
        biometricStrengthController.startListening();
    }

    private boolean isStrongBiometric(int id) {
        Iterator<BiometricSensor> it = this.mSensors.iterator();
        while (it.hasNext()) {
            BiometricSensor sensor = it.next();
            if (sensor.id == id) {
                return Utils.isAtLeastStrength(sensor.getCurrentStrength(), 15);
            }
        }
        Slog.e(TAG, "Unknown sensorId: " + id);
        return false;
    }

    private AuthSession getAuthSessionIfCurrent(long requestId) {
        AuthSession session = this.mAuthSession;
        if (session != null && session.getRequestId() == requestId) {
            return session;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAuthenticationSucceeded(long requestId, int sensorId, byte[] token) {
        Slog.v(TAG, "handleAuthenticationSucceeded(), sensorId: " + sensorId);
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.e(TAG, "handleAuthenticationSucceeded: AuthSession is null");
        } else {
            session.onAuthenticationSucceeded(sensorId, isStrongBiometric(sensorId), token);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAuthenticationRejected(long requestId, int sensorId) {
        Slog.v(TAG, "handleAuthenticationRejected()");
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "handleAuthenticationRejected: AuthSession is not current");
        } else {
            session.onAuthenticationRejected(sensorId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAuthenticationTimedOut(long requestId, int sensorId, int cookie, int error, int vendorCode) {
        Slog.v(TAG, "handleAuthenticationTimedOut(), sensorId: " + sensorId + ", cookie: " + cookie + ", error: " + error + ", vendorCode: " + vendorCode);
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "handleAuthenticationTimedOut: AuthSession is not current");
        } else {
            session.onAuthenticationTimedOut(sensorId, cookie, error, vendorCode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnError(long requestId, int sensorId, int cookie, int error, int vendorCode) {
        Slog.d(TAG, "handleOnError() sensorId: " + sensorId + ", cookie: " + cookie + ", error: " + error + ", vendorCode: " + vendorCode);
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "handleOnError: AuthSession is not current");
            return;
        }
        try {
            boolean finished = session.onErrorReceived(sensorId, cookie, error, vendorCode);
            if (finished) {
                Slog.d(TAG, "handleOnError: AuthSession finished");
                this.mAuthSession = null;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnAcquired(long requestId, int sensorId, int acquiredInfo, int vendorCode) {
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "onAcquired: AuthSession is not current");
        } else {
            session.onAcquired(sensorId, acquiredInfo, vendorCode);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnDismissed(long requestId, int reason, byte[] credentialAttestation) {
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.e(TAG, "onDismissed: " + reason + ", AuthSession is not current");
            return;
        }
        session.onDialogDismissed(reason, credentialAttestation);
        this.mAuthSession = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnTryAgainPressed(long requestId) {
        Slog.d(TAG, "onTryAgainPressed");
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "handleOnTryAgainPressed: AuthSession is not current");
        } else {
            session.onTryAgainPressed();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnDeviceCredentialPressed(long requestId) {
        Slog.d(TAG, "onDeviceCredentialPressed");
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "handleOnDeviceCredentialPressed: AuthSession is not current");
        } else {
            session.onDeviceCredentialPressed();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnSystemEvent(long requestId, int event) {
        Slog.d(TAG, "onSystemEvent: " + event);
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "handleOnSystemEvent: AuthSession is not current");
        } else {
            session.onSystemEvent(event);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleClientDied */
    public void m2271x1f626be8(long requestId) {
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "handleClientDied: AuthSession is not current");
            return;
        }
        Slog.e(TAG, "Session: " + session);
        boolean finished = session.onClientDied();
        if (finished) {
            this.mAuthSession = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnDialogAnimatedIn(long requestId) {
        Slog.d(TAG, "handleOnDialogAnimatedIn");
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "handleOnDialogAnimatedIn: AuthSession is not current");
        } else {
            session.onDialogAnimatedIn();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnReadyForAuthentication(long requestId, int cookie) {
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "handleOnReadyForAuthentication: AuthSession is not current");
        } else {
            session.onCookieReceived(cookie);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAuthenticate(final IBinder token, final long requestId, final long operationId, final int userId, final IBiometricServiceReceiver receiver, final String opPackageName, final PromptInfo promptInfo) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.BiometricService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BiometricService.this.m2273x52d8b12b(userId, promptInfo, opPackageName, requestId, token, operationId, receiver);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleAuthenticate$2$com-android-server-biometrics-BiometricService  reason: not valid java name */
    public /* synthetic */ void m2273x52d8b12b(int userId, PromptInfo promptInfo, String opPackageName, long requestId, IBinder token, long operationId, IBiometricServiceReceiver receiver) {
        try {
            PreAuthInfo preAuthInfo = PreAuthInfo.create(this.mTrustManager, this.mDevicePolicyManager, this.mSettingObserver, this.mSensors, userId, promptInfo, opPackageName, promptInfo.isDisallowBiometricsIfPolicyExists(), getContext());
            Pair<Integer, Integer> preAuthStatus = preAuthInfo.getPreAuthenticateStatus();
            Slog.d(TAG, "handleAuthenticate: modality(" + preAuthStatus.first + "), status(" + preAuthStatus.second + "), preAuthInfo: " + preAuthInfo + " requestId: " + requestId + " promptInfo.isIgnoreEnrollmentState: " + promptInfo.isIgnoreEnrollmentState());
            try {
                if (((Integer) preAuthStatus.second).intValue() != 0 && ((Integer) preAuthStatus.second).intValue() != 18) {
                    receiver.onError(((Integer) preAuthStatus.first).intValue(), ((Integer) preAuthStatus.second).intValue(), 0);
                    return;
                }
                if (preAuthInfo.credentialRequested) {
                    try {
                        if (preAuthInfo.credentialAvailable) {
                            if (preAuthInfo.eligibleSensors.isEmpty()) {
                                promptInfo.setAuthenticators(32768);
                            }
                            authenticateInternal(token, requestId, operationId, userId, receiver, opPackageName, promptInfo, preAuthInfo);
                        }
                    } catch (RemoteException e) {
                        e = e;
                        Slog.e(TAG, "Remote exception", e);
                        return;
                    }
                }
                authenticateInternal(token, requestId, operationId, userId, receiver, opPackageName, promptInfo, preAuthInfo);
            } catch (RemoteException e2) {
                e = e2;
            }
        } catch (RemoteException e3) {
            e = e3;
        }
    }

    private void authenticateInternal(IBinder token, long requestId, long operationId, int userId, IBiometricServiceReceiver receiver, String opPackageName, PromptInfo promptInfo, PreAuthInfo preAuthInfo) {
        Slog.d(TAG, "Creating authSession with authRequest: " + preAuthInfo);
        if (this.mAuthSession != null) {
            Slog.w(TAG, "Existing AuthSession: " + this.mAuthSession);
            this.mAuthSession.onCancelAuthSession(true);
            this.mAuthSession = null;
        }
        boolean debugEnabled = this.mInjector.isDebugEnabled(getContext(), userId);
        AuthSession authSession = new AuthSession(getContext(), this.mStatusBarService, createSysuiReceiver(requestId), this.mKeyStore, this.mRandom, createClientDeathReceiver(requestId), preAuthInfo, token, requestId, operationId, userId, createBiometricSensorReceiver(requestId), receiver, opPackageName, promptInfo, debugEnabled, this.mInjector.getFingerprintSensorProperties(getContext()));
        this.mAuthSession = authSession;
        try {
            authSession.goToInitialState();
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleCancelAuthentication(long requestId) {
        AuthSession session = getAuthSessionIfCurrent(requestId);
        if (session == null) {
            Slog.w(TAG, "handleCancelAuthentication: AuthSession is not current");
            return;
        }
        boolean finished = session.onCancelAuthSession(false);
        if (finished) {
            Slog.d(TAG, "handleCancelAuthentication: AuthSession finished");
            this.mAuthSession = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public BiometricSensor getSensorForId(int sensorId) {
        Iterator<BiometricSensor> it = this.mSensors.iterator();
        while (it.hasNext()) {
            BiometricSensor sensor = it.next();
            if (sensor.id == sensorId) {
                return sensor;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(PrintWriter pw) {
        pw.println("Legacy Settings: " + this.mSettingObserver.mUseLegacyFaceOnlySettings);
        pw.println();
        pw.println("Sensors:");
        Iterator<BiometricSensor> it = this.mSensors.iterator();
        while (it.hasNext()) {
            BiometricSensor sensor = it.next();
            pw.println(" " + sensor);
        }
        pw.println();
        pw.println("CurrentSession: " + this.mAuthSession);
        pw.println();
        pw.println("CoexCoordinator: " + CoexCoordinator.getInstance().toString());
        pw.println();
    }
}
