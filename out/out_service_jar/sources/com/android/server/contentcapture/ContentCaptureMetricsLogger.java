package com.android.server.contentcapture;

import android.content.ComponentName;
import android.content.ContentCaptureOptions;
import android.service.contentcapture.FlushMetrics;
import com.android.internal.util.FrameworkStatsLog;
import java.util.List;
/* loaded from: classes.dex */
public final class ContentCaptureMetricsLogger {
    private ContentCaptureMetricsLogger() {
    }

    public static void writeServiceEvent(int eventType, String serviceName) {
        FrameworkStatsLog.write(207, eventType, serviceName, (String) null, 0, 0);
    }

    public static void writeServiceEvent(int eventType, ComponentName service) {
        writeServiceEvent(eventType, ComponentName.flattenToShortString(service));
    }

    public static void writeSetWhitelistEvent(ComponentName service, List<String> packages, List<ComponentName> activities) {
        String serviceName = ComponentName.flattenToShortString(service);
        int packageCount = packages != null ? packages.size() : 0;
        int activityCount = activities != null ? activities.size() : 0;
        FrameworkStatsLog.write(207, 3, serviceName, (String) null, packageCount, activityCount);
    }

    public static void writeSessionEvent(int sessionId, int event, int flags, ComponentName service, boolean isChildSession) {
        FrameworkStatsLog.write(208, sessionId, event, flags, ComponentName.flattenToShortString(service), (String) null, isChildSession);
    }

    public static void writeSessionFlush(int sessionId, ComponentName service, FlushMetrics fm, ContentCaptureOptions options, int flushReason) {
        FrameworkStatsLog.write(209, sessionId, ComponentName.flattenToShortString(service), (String) null, fm.sessionStarted, fm.sessionFinished, fm.viewAppearedCount, fm.viewDisappearedCount, fm.viewTextChangedCount, options.maxBufferSize, options.idleFlushingFrequencyMs, options.textChangeFlushingFrequencyMs, flushReason);
    }
}
