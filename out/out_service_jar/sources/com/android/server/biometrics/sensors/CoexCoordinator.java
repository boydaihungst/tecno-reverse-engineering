package com.android.server.biometrics.sensors;

import android.os.Handler;
import android.util.Slog;
import com.android.server.biometrics.sensors.fingerprint.Udfps;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
/* loaded from: classes.dex */
public class CoexCoordinator {
    private static final boolean DEBUG = true;
    public static final String FACE_HAPTIC_DISABLE = "com.android.server.biometrics.sensors.CoexCoordinator.disable_face_haptics";
    public static final String SETTING_ENABLE_NAME = "com.android.server.biometrics.sensors.CoexCoordinator.enable";
    static final long SUCCESSFUL_AUTH_VALID_DURATION_MS = 5000;
    private static final String TAG = "BiometricCoexCoordinator";
    private static CoexCoordinator sInstance;
    private boolean mAdvancedLogicEnabled;
    private boolean mFaceHapticDisabledWhenNonBypass;
    private final Map<Integer, AuthenticationClient<?>> mClientMap = new HashMap();
    final LinkedList<SuccessfulAuth> mSuccessfulAuths = new LinkedList<>();
    private final Handler mHandler = new Handler(BiometricScheduler.getBiometricLooper());

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Callback {
        void handleLifecycleAfterAuth();

        void sendAuthenticationCanceled();

        void sendAuthenticationResult(boolean z);

        void sendHapticFeedback();
    }

    /* loaded from: classes.dex */
    interface ErrorCallback {
        void sendHapticFeedback();
    }

    /* loaded from: classes.dex */
    public static class SuccessfulAuth {
        final long mAuthTimestamp;
        final AuthenticationClient<?> mAuthenticationClient;
        final Callback mCallback;
        final CleanupRunnable mCleanupRunnable;
        final int mSensorType;

        /* loaded from: classes.dex */
        public static class CleanupRunnable implements Runnable {
            final SuccessfulAuth mAuth;
            final Callback mCallback;
            final LinkedList<SuccessfulAuth> mSuccessfulAuths;

            public CleanupRunnable(LinkedList<SuccessfulAuth> successfulAuths, SuccessfulAuth auth, Callback callback) {
                this.mSuccessfulAuths = successfulAuths;
                this.mAuth = auth;
                this.mCallback = callback;
            }

            @Override // java.lang.Runnable
            public void run() {
                boolean removed = this.mSuccessfulAuths.remove(this.mAuth);
                Slog.w(CoexCoordinator.TAG, "Removing stale successfulAuth: " + this.mAuth.toString() + ", success: " + removed);
                this.mCallback.handleLifecycleAfterAuth();
            }
        }

        public SuccessfulAuth(Handler handler, LinkedList<SuccessfulAuth> successfulAuths, long currentTimeMillis, int sensorType, AuthenticationClient<?> authenticationClient, Callback callback) {
            this.mAuthTimestamp = currentTimeMillis;
            this.mSensorType = sensorType;
            this.mAuthenticationClient = authenticationClient;
            this.mCallback = callback;
            CleanupRunnable cleanupRunnable = new CleanupRunnable(successfulAuths, this, callback);
            this.mCleanupRunnable = cleanupRunnable;
            handler.postDelayed(cleanupRunnable, CoexCoordinator.SUCCESSFUL_AUTH_VALID_DURATION_MS);
        }

        public String toString() {
            return "SensorType: " + BiometricScheduler.sensorTypeToString(this.mSensorType) + ", mAuthTimestamp: " + this.mAuthTimestamp + ", authenticationClient: " + this.mAuthenticationClient;
        }
    }

    public static CoexCoordinator getInstance() {
        if (sInstance == null) {
            sInstance = new CoexCoordinator();
        }
        return sInstance;
    }

    public void setAdvancedLogicEnabled(boolean enabled) {
        this.mAdvancedLogicEnabled = enabled;
    }

    public void setFaceHapticDisabledWhenNonBypass(boolean disabled) {
        this.mFaceHapticDisabledWhenNonBypass = disabled;
    }

    void reset() {
        this.mClientMap.clear();
    }

    private CoexCoordinator() {
    }

    public void addAuthenticationClient(int sensorType, AuthenticationClient<?> client) {
        Slog.d(TAG, "addAuthenticationClient(" + BiometricScheduler.sensorTypeToString(sensorType) + "), client: " + client);
        if (this.mClientMap.containsKey(Integer.valueOf(sensorType))) {
            Slog.w(TAG, "Overwriting existing client: " + this.mClientMap.get(Integer.valueOf(sensorType)) + " with new client: " + client);
        }
        this.mClientMap.put(Integer.valueOf(sensorType), client);
    }

    public void removeAuthenticationClient(int sensorType, AuthenticationClient<?> client) {
        Slog.d(TAG, "removeAuthenticationClient(" + BiometricScheduler.sensorTypeToString(sensorType) + "), client: " + client);
        if (!this.mClientMap.containsKey(Integer.valueOf(sensorType))) {
            Slog.e(TAG, "sensorType: " + sensorType + " does not exist in map. Client: " + client);
        } else {
            this.mClientMap.remove(Integer.valueOf(sensorType));
        }
    }

    public void onAuthenticationSucceeded(long currentTimeMillis, AuthenticationClient<?> client, Callback callback) {
        boolean isUsingSingleModality = isSingleAuthOnly(client);
        if (client.isBiometricPrompt()) {
            if (isUsingSingleModality || !hasMultipleSuccessfulAuthentications()) {
                callback.sendHapticFeedback();
            }
            callback.sendAuthenticationResult(false);
            callback.handleLifecycleAfterAuth();
        } else if (isUnknownClient(client)) {
            callback.sendHapticFeedback();
            callback.sendAuthenticationResult(true);
            callback.handleLifecycleAfterAuth();
        } else if (this.mAdvancedLogicEnabled && client.isKeyguard()) {
            if (isUsingSingleModality) {
                callback.sendHapticFeedback();
                callback.sendAuthenticationResult(true);
                callback.handleLifecycleAfterAuth();
                return;
            }
            AuthenticationClient<?> udfps = this.mClientMap.getOrDefault(2, null);
            AuthenticationClient<?> face = this.mClientMap.getOrDefault(1, null);
            if (isCurrentFaceAuth(client)) {
                if (isUdfpsActivelyAuthing(udfps)) {
                    LinkedList<SuccessfulAuth> linkedList = this.mSuccessfulAuths;
                    linkedList.add(new SuccessfulAuth(this.mHandler, linkedList, currentTimeMillis, 1, client, callback));
                    return;
                }
                if (this.mFaceHapticDisabledWhenNonBypass && !face.isKeyguardBypassEnabled()) {
                    Slog.w(TAG, "Skipping face success haptic");
                } else {
                    callback.sendHapticFeedback();
                }
                callback.sendAuthenticationResult(true);
                callback.handleLifecycleAfterAuth();
            } else if (isCurrentUdfps(client)) {
                if (isFaceScanning()) {
                    face.cancel();
                }
                removeAndFinishAllFaceFromQueue();
                callback.sendHapticFeedback();
                callback.sendAuthenticationResult(true);
                callback.handleLifecycleAfterAuth();
            } else {
                callback.sendHapticFeedback();
                callback.sendAuthenticationResult(true);
                callback.handleLifecycleAfterAuth();
            }
        } else {
            callback.sendHapticFeedback();
            callback.sendAuthenticationResult(true);
            callback.handleLifecycleAfterAuth();
        }
    }

    public void onAuthenticationRejected(long currentTimeMillis, AuthenticationClient<?> client, int lockoutMode, Callback callback) {
        boolean isUsingSingleModality = isSingleAuthOnly(client);
        if (this.mAdvancedLogicEnabled && client.isKeyguard()) {
            if (isUsingSingleModality) {
                callback.sendHapticFeedback();
                callback.handleLifecycleAfterAuth();
            } else {
                AuthenticationClient<?> udfps = this.mClientMap.getOrDefault(2, null);
                AuthenticationClient<?> face = this.mClientMap.getOrDefault(1, null);
                if (isCurrentFaceAuth(client)) {
                    if (isUdfpsActivelyAuthing(udfps)) {
                        Slog.d(TAG, "Face rejected in multi-sensor auth, udfps: " + udfps);
                        callback.handleLifecycleAfterAuth();
                    } else if (isUdfpsAuthAttempted(udfps)) {
                        callback.sendHapticFeedback();
                        callback.handleLifecycleAfterAuth();
                    } else {
                        if (this.mFaceHapticDisabledWhenNonBypass && !face.isKeyguardBypassEnabled()) {
                            Slog.w(TAG, "Skipping face reject haptic");
                        } else {
                            callback.sendHapticFeedback();
                        }
                        callback.handleLifecycleAfterAuth();
                    }
                } else if (isCurrentUdfps(client)) {
                    SuccessfulAuth auth = popSuccessfulFaceAuthIfExists(currentTimeMillis);
                    if (auth != null) {
                        Slog.d(TAG, "Using recent auth: " + auth);
                        callback.handleLifecycleAfterAuth();
                        auth.mCallback.sendHapticFeedback();
                        auth.mCallback.sendAuthenticationResult(true);
                        auth.mCallback.handleLifecycleAfterAuth();
                    } else if (isFaceScanning()) {
                        Slog.d(TAG, "UDFPS rejected in multi-sensor auth, face: " + face);
                        callback.handleLifecycleAfterAuth();
                    } else {
                        Slog.d(TAG, "UDFPS rejected in multi-sensor auth, face not scanning");
                        callback.sendHapticFeedback();
                        callback.handleLifecycleAfterAuth();
                    }
                } else {
                    Slog.d(TAG, "Unknown client rejected: " + client);
                    callback.sendHapticFeedback();
                    callback.handleLifecycleAfterAuth();
                }
            }
        } else if (client.isBiometricPrompt() && !isUsingSingleModality) {
            if (!isCurrentFaceAuth(client)) {
                callback.sendHapticFeedback();
            }
            callback.handleLifecycleAfterAuth();
        } else {
            callback.sendHapticFeedback();
            callback.handleLifecycleAfterAuth();
        }
        if (lockoutMode == 0) {
            callback.sendAuthenticationResult(false);
        }
    }

    public void onAuthenticationError(AuthenticationClient<?> client, int error, ErrorCallback callback) {
        boolean shouldUsuallyVibrate;
        boolean hapticSuppressedByCoex;
        boolean isUsingSingleModality = isSingleAuthOnly(client);
        boolean z = true;
        if (isCurrentFaceAuth(client)) {
            boolean notDetectedOnKeyguard = client.isKeyguard() && !client.wasUserDetected();
            boolean authAttempted = client.wasAuthAttempted();
            switch (error) {
                case 3:
                case 7:
                case 9:
                    if (authAttempted && !notDetectedOnKeyguard) {
                        shouldUsuallyVibrate = true;
                        break;
                    } else {
                        shouldUsuallyVibrate = false;
                        break;
                    }
                default:
                    shouldUsuallyVibrate = false;
                    break;
            }
        } else {
            shouldUsuallyVibrate = false;
        }
        if (this.mAdvancedLogicEnabled && client.isKeyguard()) {
            if (isUsingSingleModality) {
                hapticSuppressedByCoex = false;
            } else {
                boolean hapticSuppressedByCoex2 = isCurrentFaceAuth(client);
                if (!hapticSuppressedByCoex2 || client.isKeyguardBypassEnabled()) {
                    z = false;
                }
                hapticSuppressedByCoex = z;
            }
        } else {
            boolean hapticSuppressedByCoex3 = client.isBiometricPrompt();
            if (hapticSuppressedByCoex3 && !isUsingSingleModality) {
                hapticSuppressedByCoex = isCurrentFaceAuth(client);
            } else {
                hapticSuppressedByCoex = false;
            }
        }
        if (shouldUsuallyVibrate && !hapticSuppressedByCoex) {
            callback.sendHapticFeedback();
        } else {
            Slog.v(TAG, "no haptic shouldUsuallyVibrate: " + shouldUsuallyVibrate + ", hapticSuppressedByCoex: " + hapticSuppressedByCoex);
        }
    }

    private SuccessfulAuth popSuccessfulFaceAuthIfExists(long currentTimeMillis) {
        Iterator<SuccessfulAuth> it = this.mSuccessfulAuths.iterator();
        while (it.hasNext()) {
            SuccessfulAuth auth = it.next();
            if (currentTimeMillis - auth.mAuthTimestamp >= SUCCESSFUL_AUTH_VALID_DURATION_MS) {
                Slog.e(TAG, "Removing stale auth: " + auth);
                this.mSuccessfulAuths.remove(auth);
            } else if (auth.mSensorType == 1) {
                this.mSuccessfulAuths.remove(auth);
                return auth;
            }
        }
        return null;
    }

    private void removeAndFinishAllFaceFromQueue() {
        Iterator<SuccessfulAuth> it = this.mSuccessfulAuths.iterator();
        while (it.hasNext()) {
            SuccessfulAuth auth = it.next();
            if (auth.mSensorType == 1) {
                Slog.d(TAG, "Removing from queue, canceling, and finishing: " + auth);
                auth.mCallback.sendAuthenticationCanceled();
                auth.mCallback.handleLifecycleAfterAuth();
                this.mSuccessfulAuths.remove(auth);
            }
        }
    }

    private boolean isCurrentFaceAuth(AuthenticationClient<?> client) {
        return client == this.mClientMap.getOrDefault(1, null);
    }

    private boolean isCurrentUdfps(AuthenticationClient<?> client) {
        return client == this.mClientMap.getOrDefault(2, null);
    }

    private boolean isFaceScanning() {
        AuthenticationClient<?> client = this.mClientMap.getOrDefault(1, null);
        return client != null && client.getState() == 1;
    }

    private static boolean isUdfpsActivelyAuthing(AuthenticationClient<?> client) {
        return (client instanceof Udfps) && client.getState() == 1;
    }

    private static boolean isUdfpsAuthAttempted(AuthenticationClient<?> client) {
        return (client instanceof Udfps) && client.getState() == 3;
    }

    private boolean isUnknownClient(AuthenticationClient<?> client) {
        for (AuthenticationClient<?> c : this.mClientMap.values()) {
            if (c == client) {
                return false;
            }
        }
        return true;
    }

    private boolean isSingleAuthOnly(AuthenticationClient<?> client) {
        if (this.mClientMap.values().size() != 1) {
            return false;
        }
        for (AuthenticationClient<?> c : this.mClientMap.values()) {
            if (c != client) {
                return false;
            }
        }
        return true;
    }

    private boolean hasMultipleSuccessfulAuthentications() {
        int count = 0;
        for (AuthenticationClient<?> c : this.mClientMap.values()) {
            if (c.wasAuthSuccessful()) {
                count++;
            }
            if (count > 1) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Enabled: ").append(this.mAdvancedLogicEnabled);
        sb.append(", Face Haptic Disabled: ").append(this.mFaceHapticDisabledWhenNonBypass);
        sb.append(", Queue size: ").append(this.mSuccessfulAuths.size());
        Iterator<SuccessfulAuth> it = this.mSuccessfulAuths.iterator();
        while (it.hasNext()) {
            SuccessfulAuth auth = it.next();
            sb.append(", Auth: ").append(auth.toString());
        }
        return sb.toString();
    }
}
