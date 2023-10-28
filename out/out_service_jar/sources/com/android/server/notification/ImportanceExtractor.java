package com.android.server.notification;

import android.content.Context;
/* loaded from: classes2.dex */
public class ImportanceExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "ImportanceExtractor";
    private RankingConfig mConfig;

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void initialize(Context ctx, NotificationUsageStats usageStats) {
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public RankingReconsideration process(NotificationRecord record) {
        if (record == null || record.getNotification() == null || this.mConfig == null) {
            return null;
        }
        record.calculateImportance();
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
