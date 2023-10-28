package com.android.server.notification;

import com.android.internal.logging.InstanceId;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.logging.UiEventLoggerImpl;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.notification.NotificationRecordLogger;
/* loaded from: classes2.dex */
public class NotificationRecordLoggerImpl implements NotificationRecordLogger {
    private UiEventLogger mUiEventLogger = new UiEventLoggerImpl();

    @Override // com.android.server.notification.NotificationRecordLogger
    public void maybeLogNotificationPosted(NotificationRecord r, NotificationRecord old, int position, int buzzBeepBlink, InstanceId groupId) {
        NotificationRecordLogger.NotificationRecordPair p = new NotificationRecordLogger.NotificationRecordPair(r, old);
        if (!p.shouldLogReported(buzzBeepBlink)) {
            return;
        }
        writeNotificationReportedAtom(p, NotificationRecordLogger.NotificationReportedEvent.fromRecordPair(p), position, buzzBeepBlink, groupId);
    }

    @Override // com.android.server.notification.NotificationRecordLogger
    public void logNotificationAdjusted(NotificationRecord r, int position, int buzzBeepBlink, InstanceId groupId) {
        NotificationRecordLogger.NotificationRecordPair p = new NotificationRecordLogger.NotificationRecordPair(r, null);
        writeNotificationReportedAtom(p, NotificationRecordLogger.NotificationReportedEvent.NOTIFICATION_ADJUSTED, position, buzzBeepBlink, groupId);
    }

    private void writeNotificationReportedAtom(NotificationRecordLogger.NotificationRecordPair p, NotificationRecordLogger.NotificationReportedEvent eventType, int position, int buzzBeepBlink, InstanceId groupId) {
        FrameworkStatsLog.write(FrameworkStatsLog.NOTIFICATION_REPORTED, eventType.getId(), p.r.getUid(), p.r.getSbn().getPackageName(), p.getInstanceId(), p.getNotificationIdHash(), p.getChannelIdHash(), p.getGroupIdHash(), groupId == null ? 0 : groupId.getId(), p.r.getSbn().getNotification().isGroupSummary(), p.r.getSbn().getNotification().category, p.getStyle(), p.getNumPeople(), position, NotificationRecordLogger.getLoggingImportance(p.r), buzzBeepBlink, p.r.getImportanceExplanationCode(), p.r.getInitialImportance(), p.r.getInitialImportanceExplanationCode(), p.r.getAssistantImportance(), p.getAssistantHash(), p.r.getRankingScore(), p.r.getSbn().isOngoing(), NotificationRecordLogger.isForegroundService(p.r), p.r.getSbn().getNotification().getTimeoutAfter());
    }

    @Override // com.android.server.notification.NotificationRecordLogger
    public void log(UiEventLogger.UiEventEnum event, NotificationRecord r) {
        if (r == null) {
            return;
        }
        this.mUiEventLogger.logWithInstanceId(event, r.getUid(), r.getSbn().getPackageName(), r.getSbn().getInstanceId());
    }

    @Override // com.android.server.notification.NotificationRecordLogger
    public void log(UiEventLogger.UiEventEnum event) {
        this.mUiEventLogger.log(event);
    }
}
