package com.android.server.biometrics.log;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.biometrics.common.OperationContext;
import android.util.Slog;
import com.android.server.biometrics.Utils;
/* loaded from: classes.dex */
public class BiometricLogger {
    public static final boolean DEBUG = false;
    public static final String TAG = "BiometricLogger";
    private long mFirstAcquireTimeMs;
    private volatile float mLastAmbientLux;
    private boolean mLightSensorEnabled;
    private final SensorEventListener mLightSensorListener;
    private final SensorManager mSensorManager;
    private boolean mShouldLogMetrics;
    private final BiometricFrameworkStatsLogger mSink;
    private final int mStatsAction;
    private final int mStatsClient;
    private final int mStatsModality;

    /* loaded from: classes.dex */
    private class ALSProbe implements Probe {
        private boolean mDestroyed;

        private ALSProbe() {
            this.mDestroyed = false;
        }

        @Override // com.android.server.biometrics.log.Probe
        public synchronized void enable() {
            if (!this.mDestroyed) {
                BiometricLogger biometricLogger = BiometricLogger.this;
                biometricLogger.setLightSensorLoggingEnabled(biometricLogger.getAmbientLightSensor(biometricLogger.mSensorManager));
            }
        }

        @Override // com.android.server.biometrics.log.Probe
        public synchronized void disable() {
            if (!this.mDestroyed) {
                BiometricLogger.this.setLightSensorLoggingEnabled(null);
            }
        }

        @Override // com.android.server.biometrics.log.Probe
        public synchronized void destroy() {
            disable();
            this.mDestroyed = true;
        }
    }

    public static BiometricLogger ofUnknown(Context context) {
        return new BiometricLogger(context, 0, 0, 0);
    }

    public BiometricLogger(Context context, int statsModality, int statsAction, int statsClient) {
        this(statsModality, statsAction, statsClient, BiometricFrameworkStatsLogger.getInstance(), (SensorManager) context.getSystemService(SensorManager.class));
    }

    BiometricLogger(int statsModality, int statsAction, int statsClient, BiometricFrameworkStatsLogger logSink, SensorManager sensorManager) {
        this.mLightSensorEnabled = false;
        this.mShouldLogMetrics = true;
        this.mLastAmbientLux = 0.0f;
        this.mLightSensorListener = new SensorEventListener() { // from class: com.android.server.biometrics.log.BiometricLogger.1
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                BiometricLogger.this.mLastAmbientLux = event.values[0];
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mStatsModality = statsModality;
        this.mStatsAction = statsAction;
        this.mStatsClient = statsClient;
        this.mSink = logSink;
        this.mSensorManager = sensorManager;
    }

    public BiometricLogger swapAction(Context context, int statsAction) {
        return new BiometricLogger(context, this.mStatsModality, statsAction, this.mStatsClient);
    }

    public void disableMetrics() {
        this.mShouldLogMetrics = false;
    }

    public int getStatsClient() {
        return this.mStatsClient;
    }

    private boolean shouldSkipLogging() {
        int i = this.mStatsModality;
        boolean shouldSkipLogging = i == 0 || this.mStatsAction == 0;
        if (i == 0) {
            Slog.w(TAG, "Unknown field detected: MODALITY_UNKNOWN, will not report metric");
        }
        if (this.mStatsAction == 0) {
            Slog.w(TAG, "Unknown field detected: ACTION_UNKNOWN, will not report metric");
        }
        if (this.mStatsClient == 0) {
            Slog.w(TAG, "Unknown field detected: CLIENT_UNKNOWN");
        }
        return shouldSkipLogging;
    }

    public void logOnAcquired(Context context, OperationContext operationContext, int acquiredInfo, int vendorCode, int targetUserId) {
        if (!this.mShouldLogMetrics) {
            return;
        }
        int i = this.mStatsModality;
        boolean isFace = i == 4;
        boolean isFingerprint = i == 1;
        if (isFace || isFingerprint) {
            if ((isFingerprint && acquiredInfo == 7) || (isFace && acquiredInfo == 20)) {
                this.mFirstAcquireTimeMs = System.currentTimeMillis();
            }
        } else if (acquiredInfo == 0 && this.mFirstAcquireTimeMs == 0) {
            this.mFirstAcquireTimeMs = System.currentTimeMillis();
        }
        if (shouldSkipLogging()) {
            return;
        }
        this.mSink.acquired(operationContext, this.mStatsModality, this.mStatsAction, this.mStatsClient, Utils.isDebugEnabled(context, targetUserId), acquiredInfo, vendorCode, targetUserId);
    }

    public void logOnError(Context context, OperationContext operationContext, int error, int vendorCode, int targetUserId) {
        if (!this.mShouldLogMetrics) {
            return;
        }
        long latency = this.mFirstAcquireTimeMs != 0 ? System.currentTimeMillis() - this.mFirstAcquireTimeMs : -1L;
        Slog.v(TAG, "Error latency: " + latency);
        if (shouldSkipLogging()) {
            return;
        }
        this.mSink.error(operationContext, this.mStatsModality, this.mStatsAction, this.mStatsClient, Utils.isDebugEnabled(context, targetUserId), latency, error, vendorCode, targetUserId);
    }

    public void logOnAuthenticated(Context context, OperationContext operationContext, boolean authenticated, boolean requireConfirmation, int targetUserId, boolean isBiometricPrompt) {
        int authState;
        long j;
        if (!this.mShouldLogMetrics) {
            return;
        }
        if (!authenticated) {
            authState = 1;
        } else if (isBiometricPrompt && requireConfirmation) {
            authState = 2;
        } else {
            authState = 3;
        }
        if (this.mFirstAcquireTimeMs != 0) {
            j = System.currentTimeMillis() - this.mFirstAcquireTimeMs;
        } else {
            j = -1;
        }
        long latency = j;
        Slog.v(TAG, "Authentication latency: " + latency);
        if (shouldSkipLogging()) {
            return;
        }
        this.mSink.authenticate(operationContext, this.mStatsModality, this.mStatsAction, this.mStatsClient, Utils.isDebugEnabled(context, targetUserId), latency, authState, requireConfirmation, targetUserId, this.mLastAmbientLux);
    }

    public void logOnEnrolled(int targetUserId, long latency, boolean enrollSuccessful) {
        if (!this.mShouldLogMetrics) {
            return;
        }
        Slog.v(TAG, "Enroll latency: " + latency);
        if (shouldSkipLogging()) {
            return;
        }
        this.mSink.enroll(this.mStatsModality, this.mStatsAction, this.mStatsClient, targetUserId, latency, enrollSuccessful, this.mLastAmbientLux);
    }

    public void logUnknownEnrollmentInHal() {
        if (shouldSkipLogging()) {
            return;
        }
        this.mSink.reportUnknownTemplateEnrolledHal(this.mStatsModality);
    }

    public void logUnknownEnrollmentInFramework() {
        if (shouldSkipLogging()) {
            return;
        }
        this.mSink.reportUnknownTemplateEnrolledFramework(this.mStatsModality);
    }

    public CallbackWithProbe<Probe> createALSCallback(boolean startWithClient) {
        return new CallbackWithProbe<>(new ALSProbe(), startWithClient);
    }

    protected Sensor getAmbientLightSensor(SensorManager sensorManager) {
        if (this.mShouldLogMetrics) {
            return sensorManager.getDefaultSensor(5);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setLightSensorLoggingEnabled(Sensor lightSensor) {
        if (lightSensor != null) {
            if (!this.mLightSensorEnabled) {
                this.mLightSensorEnabled = true;
                this.mLastAmbientLux = 0.0f;
                this.mSensorManager.registerListener(this.mLightSensorListener, lightSensor, 3);
                return;
            }
            return;
        }
        this.mLightSensorEnabled = false;
        this.mLastAmbientLux = 0.0f;
        this.mSensorManager.unregisterListener(this.mLightSensorListener);
    }
}
