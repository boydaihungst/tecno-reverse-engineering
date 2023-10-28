package com.android.server.biometrics.sensors;

import android.app.ActivityTaskManager;
import android.app.TaskStackListener;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.security.KeyStore;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.CoexCoordinator;
import com.android.server.biometrics.sensors.fingerprint.FingerprintUtils;
import com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintUtils;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public abstract class AuthenticationClient<T> extends AcquisitionClient<T> implements AuthenticationConsumer {
    private static final long[] FP_ERROR_VIBRATE_PATTERN = {30, 100};
    public static final int STATE_NEW = 0;
    public static final int STATE_STARTED = 1;
    public static final int STATE_STARTED_PAUSED = 2;
    public static final int STATE_STARTED_PAUSED_ATTEMPTED = 3;
    public static final int STATE_STOPPED = 4;
    private static final String TAG = "Biometrics/AuthenticationClient";
    private final ActivityTaskManager mActivityTaskManager;
    private final boolean mAllowBackgroundAuthentication;
    private boolean mAuthAttempted;
    private boolean mAuthSuccess;
    private final BiometricManager mBiometricManager;
    private final boolean mIsKeyguardBypassEnabled;
    private final boolean mIsRestricted;
    private final boolean mIsStrongBiometric;
    private final LockoutTracker mLockoutTracker;
    protected final long mOperationId;
    private final boolean mRequireConfirmation;
    private long mStartTimeMs;
    protected int mState;
    private final TaskStackListener mTaskStackListener;

    /* loaded from: classes.dex */
    @interface State {
    }

    protected abstract void handleLifecycleAfterAuth(boolean z);

    public abstract boolean wasUserDetected();

    public AuthenticationClient(Context context, Supplier<T> lazyDaemon, IBinder token, ClientMonitorCallbackConverter listener, int targetUserId, long operationId, boolean restricted, String owner, int cookie, boolean requireConfirmation, int sensorId, BiometricLogger biometricLogger, BiometricContext biometricContext, boolean isStrongBiometric, TaskStackListener taskStackListener, LockoutTracker lockoutTracker, boolean allowBackgroundAuthentication, boolean shouldVibrate, boolean isKeyguardBypassEnabled) {
        super(context, lazyDaemon, token, listener, targetUserId, owner, cookie, sensorId, shouldVibrate, biometricLogger, biometricContext);
        this.mAuthSuccess = false;
        this.mState = 0;
        this.mIsStrongBiometric = isStrongBiometric;
        this.mOperationId = operationId;
        this.mRequireConfirmation = requireConfirmation;
        this.mActivityTaskManager = getActivityTaskManager();
        this.mBiometricManager = (BiometricManager) context.getSystemService(BiometricManager.class);
        this.mTaskStackListener = taskStackListener;
        this.mLockoutTracker = lockoutTracker;
        this.mIsRestricted = restricted;
        this.mAllowBackgroundAuthentication = allowBackgroundAuthentication;
        this.mIsKeyguardBypassEnabled = isKeyguardBypassEnabled;
        setVibrateState(true);
    }

    public int handleFailedAttempt(int userId) {
        int lockoutMode = this.mLockoutTracker.getLockoutModeForUser(userId);
        PerformanceTracker performanceTracker = PerformanceTracker.getInstanceForSensorId(getSensorId());
        if (lockoutMode == 2) {
            performanceTracker.incrementPermanentLockoutForUser(userId);
        } else if (lockoutMode == 1) {
            performanceTracker.incrementTimedLockoutForUser(userId);
        }
        return lockoutMode;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public long getStartTimeMs() {
        return this.mStartTimeMs;
    }

    protected ActivityTaskManager getActivityTaskManager() {
        return ActivityTaskManager.getInstance();
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor, android.os.IBinder.DeathRecipient
    public void binderDied() {
        boolean clearListener = !isBiometricPrompt();
        binderDiedInternal(clearListener);
    }

    public boolean isBiometricPrompt() {
        return getCookie() != 0;
    }

    public long getOperationId() {
        return this.mOperationId;
    }

    public boolean isRestricted() {
        return this.mIsRestricted;
    }

    public boolean isKeyguard() {
        return Utils.isKeyguard(getContext(), getOwnerString());
    }

    private boolean isSettings() {
        return Utils.isSettings(getContext(), getOwnerString());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public boolean isCryptoOperation() {
        return this.mOperationId != 0;
    }

    @Override // com.android.server.biometrics.sensors.AuthenticationConsumer
    public void onAuthenticated(final BiometricAuthenticator.Identifier identifier, boolean authenticated, ArrayList<Byte> hardwareAuthToken) {
        boolean isBackgroundAuth;
        boolean authenticated2;
        getLogger().logOnAuthenticated(getContext(), getOperationContext(), authenticated, this.mRequireConfirmation, getTargetUserId(), isBiometricPrompt());
        final ClientMonitorCallbackConverter listener = getListener();
        Slog.v(TAG, "onAuthenticated(" + authenticated + "), ID:" + identifier.getBiometricId() + ", Owner: " + getOwnerString() + ", isBP: " + isBiometricPrompt() + ", listener: " + listener + ", requireConfirmation: " + this.mRequireConfirmation + ", user: " + getTargetUserId() + ", clientMonitor: " + toString());
        PerformanceTracker pm = PerformanceTracker.getInstanceForSensorId(getSensorId());
        if (isCryptoOperation()) {
            pm.incrementCryptoAuthForUser(getTargetUserId(), authenticated);
        } else {
            pm.incrementAuthForUser(getTargetUserId(), authenticated);
        }
        if (this.mAllowBackgroundAuthentication) {
            Slog.w(TAG, "Allowing background authentication, this is allowed only for platform or test invocations");
        }
        if (!this.mAllowBackgroundAuthentication && authenticated && !Utils.isKeyguard(getContext(), getOwnerString()) && !Utils.isSystem(getContext(), getOwnerString())) {
            boolean isBackgroundAuth2 = Utils.isBackground(getOwnerString());
            isBackgroundAuth = isBackgroundAuth2;
        } else {
            isBackgroundAuth = false;
        }
        if (isBackgroundAuth) {
            Slog.e(TAG, "Failing possible background authentication");
            ApplicationInfo appInfo = getContext().getApplicationInfo();
            Object[] objArr = new Object[3];
            objArr[0] = "159249069";
            objArr[1] = Integer.valueOf(appInfo != null ? appInfo.uid : -1);
            objArr[2] = "Attempted background authentication";
            EventLog.writeEvent(1397638484, objArr);
            authenticated2 = false;
        } else {
            authenticated2 = authenticated;
        }
        if (!authenticated2) {
            if (!isBackgroundAuth) {
                int lockoutMode = handleFailedAttempt(getTargetUserId());
                if (lockoutMode != 0) {
                    markAlreadyDone();
                }
                CoexCoordinator coordinator = CoexCoordinator.getInstance();
                coordinator.onAuthenticationRejected(SystemClock.uptimeMillis(), this, lockoutMode, new CoexCoordinator.Callback() { // from class: com.android.server.biometrics.sensors.AuthenticationClient.2
                    @Override // com.android.server.biometrics.sensors.CoexCoordinator.Callback
                    public void sendAuthenticationResult(boolean addAuthTokenIfStrong) {
                        ClientMonitorCallbackConverter clientMonitorCallbackConverter = listener;
                        if (clientMonitorCallbackConverter != null) {
                            try {
                                clientMonitorCallbackConverter.onAuthenticationFailed(AuthenticationClient.this.getSensorId());
                            } catch (RemoteException e) {
                                Slog.e(AuthenticationClient.TAG, "Unable to notify listener", e);
                            }
                        }
                    }

                    @Override // com.android.server.biometrics.sensors.CoexCoordinator.Callback
                    public void sendHapticFeedback() {
                        if (listener != null && ITranFingerprintUtils.Instance().getVibrateState()) {
                            FingerprintUtils.vibrateFingerprintError(AuthenticationClient.this.getContext());
                        }
                    }

                    @Override // com.android.server.biometrics.sensors.CoexCoordinator.Callback
                    public void handleLifecycleAfterAuth() {
                        AuthenticationClient.this.handleLifecycleAfterAuth(false);
                    }

                    @Override // com.android.server.biometrics.sensors.CoexCoordinator.Callback
                    public void sendAuthenticationCanceled() {
                        AuthenticationClient.this.sendCancelOnly(listener);
                    }
                });
                return;
            }
            Slog.e(TAG, "cancelling due to background auth");
            cancel();
            return;
        }
        if (isBackgroundAuth) {
            ApplicationInfo appInfo2 = getContext().getApplicationInfo();
            Object[] objArr2 = new Object[3];
            objArr2[0] = "159249069";
            objArr2[1] = Integer.valueOf(appInfo2 != null ? appInfo2.uid : -1);
            objArr2[2] = "Successful background authentication!";
            EventLog.writeEvent(1397638484, objArr2);
        }
        this.mAuthSuccess = true;
        markAlreadyDone();
        TaskStackListener taskStackListener = this.mTaskStackListener;
        if (taskStackListener != null) {
            this.mActivityTaskManager.unregisterTaskStackListener(taskStackListener);
        }
        final byte[] byteToken = new byte[hardwareAuthToken.size()];
        for (int i = 0; i < hardwareAuthToken.size(); i++) {
            byteToken[i] = hardwareAuthToken.get(i).byteValue();
        }
        if (this.mIsStrongBiometric) {
            this.mBiometricManager.resetLockoutTimeBound(getToken(), getContext().getOpPackageName(), getSensorId(), getTargetUserId(), byteToken);
        }
        CoexCoordinator coordinator2 = CoexCoordinator.getInstance();
        coordinator2.onAuthenticationSucceeded(SystemClock.uptimeMillis(), this, new CoexCoordinator.Callback() { // from class: com.android.server.biometrics.sensors.AuthenticationClient.1
            @Override // com.android.server.biometrics.sensors.CoexCoordinator.Callback
            public void sendAuthenticationResult(boolean addAuthTokenIfStrong) {
                if (!addAuthTokenIfStrong || !AuthenticationClient.this.mIsStrongBiometric) {
                    Slog.d(AuthenticationClient.TAG, "Skipping addAuthToken");
                } else {
                    int result = KeyStore.getInstance().addAuthToken(byteToken);
                    Slog.d(AuthenticationClient.TAG, "addAuthToken: " + result);
                }
                if (listener == null) {
                    Slog.w(AuthenticationClient.TAG, "Client not listening");
                    return;
                }
                try {
                    if (AuthenticationClient.this.mIsRestricted && !ITranFingerprintUtils.Instance().isInLauncher(AuthenticationClient.this.getOwnerString())) {
                        listener.onAuthenticationSucceeded(AuthenticationClient.this.getSensorId(), null, byteToken, AuthenticationClient.this.getTargetUserId(), AuthenticationClient.this.mIsStrongBiometric);
                    }
                    listener.onAuthenticationSucceeded(AuthenticationClient.this.getSensorId(), identifier, byteToken, AuthenticationClient.this.getTargetUserId(), AuthenticationClient.this.mIsStrongBiometric);
                } catch (RemoteException e) {
                    Slog.e(AuthenticationClient.TAG, "Unable to notify listener", e);
                }
            }

            @Override // com.android.server.biometrics.sensors.CoexCoordinator.Callback
            public void sendHapticFeedback() {
            }

            @Override // com.android.server.biometrics.sensors.CoexCoordinator.Callback
            public void handleLifecycleAfterAuth() {
                AuthenticationClient.this.handleLifecycleAfterAuth(true);
            }

            @Override // com.android.server.biometrics.sensors.CoexCoordinator.Callback
            public void sendAuthenticationCanceled() {
                AuthenticationClient.this.sendCancelOnly(listener);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onLockoutTimed(long durationMillis) {
        final ClientMonitorCallbackConverter listener = getListener();
        CoexCoordinator coordinator = CoexCoordinator.getInstance();
        coordinator.onAuthenticationError(this, 7, new CoexCoordinator.ErrorCallback() { // from class: com.android.server.biometrics.sensors.AuthenticationClient.3
            @Override // com.android.server.biometrics.sensors.CoexCoordinator.ErrorCallback
            public void sendHapticFeedback() {
                if (listener != null && AuthenticationClient.this.mShouldVibrate) {
                    AuthenticationClient.this.vibrateError();
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onLockoutPermanent() {
        final ClientMonitorCallbackConverter listener = getListener();
        CoexCoordinator coordinator = CoexCoordinator.getInstance();
        coordinator.onAuthenticationError(this, 9, new CoexCoordinator.ErrorCallback() { // from class: com.android.server.biometrics.sensors.AuthenticationClient.4
            @Override // com.android.server.biometrics.sensors.CoexCoordinator.ErrorCallback
            public void sendHapticFeedback() {
                if (listener != null && AuthenticationClient.this.mShouldVibrate) {
                    AuthenticationClient.this.vibrateError();
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendCancelOnly(ClientMonitorCallbackConverter listener) {
        if (listener == null) {
            Slog.e(TAG, "Unable to sendAuthenticationCanceled, listener null");
            return;
        }
        try {
            listener.onError(getSensorId(), getCookie(), 5, 0);
        } catch (RemoteException e) {
            Slog.e(TAG, "Remote exception", e);
        }
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient
    public void onAcquired(int acquiredInfo, int vendorCode) {
        super.onAcquired(acquiredInfo, vendorCode);
        int lockoutMode = this.mLockoutTracker.getLockoutModeForUser(getTargetUserId());
        if (lockoutMode == 0) {
            PerformanceTracker pt = PerformanceTracker.getInstanceForSensorId(getSensorId());
            pt.incrementAcquireForUser(getTargetUserId(), isCryptoOperation());
        }
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient, com.android.server.biometrics.sensors.ErrorConsumer
    public void onError(int errorCode, int vendorCode) {
        super.onError(errorCode, vendorCode);
        this.mState = 4;
        CoexCoordinator.getInstance().onAuthenticationError(this, errorCode, new CoexCoordinator.ErrorCallback() { // from class: com.android.server.biometrics.sensors.AuthenticationClient$$ExternalSyntheticLambda0
            @Override // com.android.server.biometrics.sensors.CoexCoordinator.ErrorCallback
            public final void sendHapticFeedback() {
                AuthenticationClient.this.vibrateError();
            }
        });
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public void start(ClientMonitorCallback callback) {
        int errorCode;
        super.start(callback);
        int lockoutMode = this.mLockoutTracker.getLockoutModeForUser(getTargetUserId());
        if (lockoutMode != 0) {
            Slog.v(TAG, "In lockout mode(" + lockoutMode + ") ; disallowing authentication");
            if (lockoutMode == 1) {
                errorCode = 7;
            } else {
                errorCode = 9;
            }
            onError(errorCode, 0);
            return;
        }
        TaskStackListener taskStackListener = this.mTaskStackListener;
        if (taskStackListener != null) {
            this.mActivityTaskManager.registerTaskStackListener(taskStackListener);
        }
        Slog.d(TAG, "Requesting auth for " + getOwnerString());
        this.mStartTimeMs = System.currentTimeMillis();
        this.mAuthAttempted = true;
        startHalOperation();
    }

    @Override // com.android.server.biometrics.sensors.AcquisitionClient, com.android.server.biometrics.sensors.Interruptable
    public void cancel() {
        super.cancel();
        TaskStackListener taskStackListener = this.mTaskStackListener;
        if (taskStackListener != null) {
            this.mActivityTaskManager.unregisterTaskStackListener(taskStackListener);
        }
    }

    public int getState() {
        return this.mState;
    }

    public boolean isKeyguardBypassEnabled() {
        return this.mIsKeyguardBypassEnabled;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public int getProtoEnum() {
        return 3;
    }

    @Override // com.android.server.biometrics.sensors.BaseClientMonitor
    public boolean interruptsPrecedingClients() {
        return true;
    }

    public boolean wasAuthAttempted() {
        return this.mAuthAttempted;
    }

    public boolean wasAuthSuccessful() {
        return this.mAuthSuccess;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public int getShowOverlayReason() {
        if (isKeyguard()) {
            return 4;
        }
        if (isBiometricPrompt()) {
            return 3;
        }
        if (isSettings()) {
            return 6;
        }
        return 5;
    }

    public void setVibrateState(boolean state) {
        ITranFingerprintUtils.Instance().setVibrateState(state);
    }
}
