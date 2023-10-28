package com.android.server.notification;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.logging.UiEventLoggerImpl;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.notification.NotificationChannelLogger;
/* loaded from: classes2.dex */
public class NotificationChannelLoggerImpl implements NotificationChannelLogger {
    UiEventLogger mUiEventLogger = new UiEventLoggerImpl();

    @Override // com.android.server.notification.NotificationChannelLogger
    public void logNotificationChannel(NotificationChannelLogger.NotificationChannelEvent event, NotificationChannel channel, int uid, String pkg, int oldImportance, int newImportance) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.NOTIFICATION_CHANNEL_MODIFIED, event.getId(), uid, pkg, NotificationChannelLogger.getIdHash(channel), oldImportance, newImportance, channel.isConversation(), NotificationChannelLogger.getConversationIdHash(channel), channel.isDemoted(), channel.isImportantConversation());
    }

    @Override // com.android.server.notification.NotificationChannelLogger
    public void logNotificationChannelGroup(NotificationChannelLogger.NotificationChannelEvent event, NotificationChannelGroup channelGroup, int uid, String pkg, boolean wasBlocked) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.NOTIFICATION_CHANNEL_MODIFIED, event.getId(), uid, pkg, NotificationChannelLogger.getIdHash(channelGroup), NotificationChannelLogger.getImportance(wasBlocked), NotificationChannelLogger.getImportance(channelGroup), false, 0, false, false);
    }

    @Override // com.android.server.notification.NotificationChannelLogger
    public void logAppEvent(NotificationChannelLogger.NotificationChannelEvent event, int uid, String pkg) {
        this.mUiEventLogger.log(event, uid, pkg);
    }
}
