package com.android.server.notification;

import android.app.NotificationChannel;
import android.content.Context;
/* loaded from: classes2.dex */
public class NotificationChannelExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "ChannelExtractor";
    private RankingConfig mConfig;
    private Context mContext;

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void initialize(Context ctx, NotificationUsageStats usageStats) {
        this.mContext = ctx;
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public RankingReconsideration process(NotificationRecord record) {
        RankingConfig rankingConfig;
        if (record == null || record.getNotification() == null || (rankingConfig = this.mConfig) == null) {
            return null;
        }
        NotificationChannel updatedChannel = rankingConfig.getConversationNotificationChannel(record.getSbn().getPackageName(), record.getSbn().getUid(), record.getChannel().getId(), record.getSbn().getShortcutId(), true, false);
        record.updateNotificationChannel(updatedChannel);
        return null;
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void setConfig(RankingConfig config) {
        this.mConfig = config;
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void setZenHelper(ZenModeHelper helper) {
    }
}
