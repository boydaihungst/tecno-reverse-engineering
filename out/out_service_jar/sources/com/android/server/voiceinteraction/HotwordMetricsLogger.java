package com.android.server.voiceinteraction;

import com.android.internal.util.FrameworkStatsLog;
/* loaded from: classes2.dex */
public final class HotwordMetricsLogger {
    private static final int METRICS_INIT_DETECTOR_DSP = 1;
    private static final int METRICS_INIT_DETECTOR_SOFTWARE = 2;
    private static final int METRICS_INIT_NORMAL_DETECTOR = 0;

    private HotwordMetricsLogger() {
    }

    public static void writeDetectorCreateEvent(int detectorType, boolean isCreated, int uid) {
        int metricsDetectorType = getCreateMetricsDetectorType(detectorType);
        FrameworkStatsLog.write((int) FrameworkStatsLog.HOTWORD_DETECTOR_CREATE_REQUESTED, metricsDetectorType, isCreated, uid);
    }

    public static void writeServiceInitResultEvent(int detectorType, int result) {
        int metricsDetectorType = getInitMetricsDetectorType(detectorType);
        FrameworkStatsLog.write((int) FrameworkStatsLog.HOTWORD_DETECTION_SERVICE_INIT_RESULT_REPORTED, metricsDetectorType, result);
    }

    public static void writeServiceRestartEvent(int detectorType, int reason) {
        int metricsDetectorType = getRestartMetricsDetectorType(detectorType);
        FrameworkStatsLog.write((int) FrameworkStatsLog.HOTWORD_DETECTION_SERVICE_RESTARTED, metricsDetectorType, reason);
    }

    public static void writeKeyphraseTriggerEvent(int detectorType, int result) {
        int metricsDetectorType = getKeyphraseMetricsDetectorType(detectorType);
        FrameworkStatsLog.write((int) FrameworkStatsLog.HOTWORD_DETECTOR_KEYPHRASE_TRIGGERED, metricsDetectorType, result);
    }

    public static void writeDetectorEvent(int detectorType, int event, int uid) {
        int metricsDetectorType = getDetectorMetricsDetectorType(detectorType);
        FrameworkStatsLog.write((int) FrameworkStatsLog.HOTWORD_DETECTOR_EVENTS, metricsDetectorType, event, uid);
    }

    private static int getCreateMetricsDetectorType(int detectorType) {
        switch (detectorType) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
        }
    }

    private static int getRestartMetricsDetectorType(int detectorType) {
        switch (detectorType) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
        }
    }

    private static int getInitMetricsDetectorType(int detectorType) {
        switch (detectorType) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
        }
    }

    private static int getKeyphraseMetricsDetectorType(int detectorType) {
        switch (detectorType) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
        }
    }

    private static int getDetectorMetricsDetectorType(int detectorType) {
        switch (detectorType) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
        }
    }
}
