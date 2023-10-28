package com.android.server.notification;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
/* loaded from: classes2.dex */
public class VisibilityExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "VisibilityExtractor";
    private RankingConfig mConfig;
    private DevicePolicyManager mDpm;

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void initialize(Context ctx, NotificationUsageStats usageStats) {
        this.mDpm = (DevicePolicyManager) ctx.getSystemService(DevicePolicyManager.class);
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public RankingReconsideration process(NotificationRecord record) {
        if (record == null || record.getNotification() == null || this.mConfig == null) {
            return null;
        }
        int userId = record.getUserId();
        if (userId == -1) {
            record.setPackageVisibilityOverride(record.getChannel().getLockscreenVisibility());
        } else {
            boolean userCanShowNotifications = this.mConfig.canShowNotificationsOnLockscreen(userId);
            boolean dpmCanShowNotifications = adminAllowsKeyguardFeature(userId, 4);
            boolean channelCanShowNotifications = record.getChannel().getLockscreenVisibility() != -1;
            if (!userCanShowNotifications || !dpmCanShowNotifications || !channelCanShowNotifications) {
                record.setPackageVisibilityOverride(-1);
            } else {
                boolean userCanShowContents = this.mConfig.canShowPrivateNotificationsOnLockScreen(userId);
                boolean dpmCanShowContents = adminAllowsKeyguardFeature(userId, 8);
                boolean channelCanShowContents = record.getChannel().getLockscreenVisibility() != 0;
                if (!userCanShowContents || !dpmCanShowContents || !channelCanShowContents) {
                    record.setPackageVisibilityOverride(0);
                } else {
                    record.setPackageVisibilityOverride(-1000);
                }
            }
        }
        return null;
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void setConfig(RankingConfig config) {
        this.mConfig = config;
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void setZenHelper(ZenModeHelper helper) {
    }

    private boolean adminAllowsKeyguardFeature(int userHandle, int feature) {
        if (userHandle == -1) {
            return true;
        }
        int dpmFlags = this.mDpm.getKeyguardDisabledFeatures(null, userHandle);
        return (dpmFlags & feature) == 0;
    }
}
