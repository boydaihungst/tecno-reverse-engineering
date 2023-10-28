package com.android.server.biometrics.log;

import android.hardware.biometrics.common.OperationContext;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
/* loaded from: classes.dex */
public class BiometricFrameworkStatsLogger {
    private static final String TAG = "BiometricFrameworkStatsLogger";
    private static final BiometricFrameworkStatsLogger sInstance = new BiometricFrameworkStatsLogger();

    private BiometricFrameworkStatsLogger() {
    }

    public static BiometricFrameworkStatsLogger getInstance() {
        return sInstance;
    }

    public void acquired(OperationContext operationContext, int statsModality, int statsAction, int statsClient, boolean isDebug, int acquiredInfo, int vendorCode, int targetUserId) {
        FrameworkStatsLog.write(87, statsModality, targetUserId, operationContext.isCrypto, statsAction, statsClient, acquiredInfo, vendorCode, isDebug, -1, operationContext.id, sessionType(operationContext.reason), operationContext.isAod);
    }

    public void authenticate(OperationContext operationContext, int statsModality, int statsAction, int statsClient, boolean isDebug, long latency, int authState, boolean requireConfirmation, int targetUserId, float ambientLightLux) {
        FrameworkStatsLog.write(88, statsModality, targetUserId, operationContext.isCrypto, statsClient, requireConfirmation, authState, sanitizeLatency(latency), isDebug, -1, ambientLightLux, operationContext.id, sessionType(operationContext.reason), operationContext.isAod);
    }

    public void enroll(int statsModality, int statsAction, int statsClient, int targetUserId, long latency, boolean enrollSuccessful, float ambientLightLux) {
        FrameworkStatsLog.write(184, statsModality, targetUserId, sanitizeLatency(latency), enrollSuccessful, -1, ambientLightLux);
    }

    public void error(OperationContext operationContext, int statsModality, int statsAction, int statsClient, boolean isDebug, long latency, int error, int vendorCode, int targetUserId) {
        FrameworkStatsLog.write(89, statsModality, targetUserId, operationContext.isCrypto, statsAction, statsClient, error, vendorCode, isDebug, sanitizeLatency(latency), -1, operationContext.id, sessionType(operationContext.reason), operationContext.isAod);
    }

    public void reportUnknownTemplateEnrolledHal(int statsModality) {
        FrameworkStatsLog.write(148, statsModality, 3, -1);
    }

    public void reportUnknownTemplateEnrolledFramework(int statsModality) {
        FrameworkStatsLog.write(148, statsModality, 2, -1);
    }

    private long sanitizeLatency(long latency) {
        if (latency < 0) {
            Slog.w(TAG, "found a negative latency : " + latency);
            return -1L;
        }
        return latency;
    }

    private static int sessionType(byte reason) {
        if (reason == 1) {
            return 2;
        }
        return reason == 2 ? 1 : 0;
    }
}
