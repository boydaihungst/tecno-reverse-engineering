package com.android.server.notification;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.am.HostingRecord;
/* loaded from: classes2.dex */
public class BubbleExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "BubbleExtractor";
    private ActivityManager mActivityManager;
    private RankingConfig mConfig;
    private Context mContext;
    private ShortcutHelper mShortcutHelper;
    boolean mSupportsBubble;

    @Override // com.android.server.notification.NotificationSignalExtractor
    public void initialize(Context context, NotificationUsageStats usageStats) {
        this.mContext = context;
        this.mActivityManager = (ActivityManager) context.getSystemService(HostingRecord.HOSTING_TYPE_ACTIVITY);
        this.mSupportsBubble = Resources.getSystem().getBoolean(17891777);
    }

    @Override // com.android.server.notification.NotificationSignalExtractor
    public RankingReconsideration process(NotificationRecord record) {
        if (record == null || record.getNotification() == null || this.mConfig == null || this.mShortcutHelper == null) {
            return null;
        }
        boolean applyFlag = false;
        boolean notifCanPresentAsBubble = canPresentAsBubble(record) && !this.mActivityManager.isLowRamDevice() && record.isConversation() && record.getShortcutInfo() != null && (record.getNotification().flags & 64) == 0;
        boolean userEnabledBubbles = this.mConfig.bubblesEnabled(record.getUser());
        int appPreference = this.mConfig.getBubblePreference(record.getSbn().getPackageName(), record.getSbn().getUid());
        NotificationChannel recordChannel = record.getChannel();
        if (!userEnabledBubbles || appPreference == 0 || !notifCanPresentAsBubble) {
            record.setAllowBubble(false);
            if (!notifCanPresentAsBubble) {
                record.getNotification().setBubbleMetadata(null);
            }
        } else if (recordChannel == null) {
            record.setAllowBubble(true);
        } else if (appPreference == 1) {
            record.setAllowBubble(recordChannel.getAllowBubbles() != 0);
        } else if (appPreference == 2) {
            record.setAllowBubble(recordChannel.canBubble());
        }
        if (record.canBubble() && !record.isFlagBubbleRemoved()) {
            applyFlag = true;
        }
        if (applyFlag) {
            record.getNotification().flags |= 4096;
        } else {
            record.getNotification().flags &= -4097;
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

    public void setShortcutHelper(ShortcutHelper helper) {
        this.mShortcutHelper = helper;
    }

    public void setActivityManager(ActivityManager manager) {
        this.mActivityManager = manager;
    }

    boolean canPresentAsBubble(NotificationRecord r) {
        String notificationShortcutId;
        if (this.mSupportsBubble) {
            Notification notification = r.getNotification();
            Notification.BubbleMetadata metadata = notification.getBubbleMetadata();
            String pkg = r.getSbn().getPackageName();
            if (metadata == null) {
                return false;
            }
            String shortcutId = metadata.getShortcutId();
            if (r.getShortcutInfo() != null) {
                notificationShortcutId = r.getShortcutInfo().getId();
            } else {
                notificationShortcutId = null;
            }
            boolean shortcutValid = false;
            if (notificationShortcutId != null && shortcutId != null) {
                shortcutValid = shortcutId.equals(notificationShortcutId);
            } else if (shortcutId != null) {
                shortcutValid = this.mShortcutHelper.getValidShortcutInfo(shortcutId, pkg, r.getUser()) != null;
            }
            if (metadata.getIntent() == null && !shortcutValid) {
                logBubbleError(r.getKey(), "couldn't find valid shortcut for bubble with shortcutId: " + shortcutId);
                return false;
            } else if (shortcutValid) {
                return true;
            } else {
                return canLaunchInTaskView(this.mContext, metadata.getIntent(), pkg);
            }
        }
        return false;
    }

    protected boolean canLaunchInTaskView(Context context, PendingIntent pendingIntent, String packageName) {
        ActivityInfo info;
        if (pendingIntent == null) {
            Slog.w(TAG, "Unable to create bubble -- no intent");
            return false;
        }
        Intent intent = pendingIntent.getIntent();
        if (intent != null) {
            info = intent.resolveActivityInfo(context.getPackageManager(), 0);
        } else {
            info = null;
        }
        if (info == null) {
            FrameworkStatsLog.write(173, packageName, 1);
            Slog.w(TAG, "Unable to send as bubble -- couldn't find activity info for intent: " + intent);
            return false;
        } else if (ActivityInfo.isResizeableMode(info.resizeMode)) {
            return true;
        } else {
            FrameworkStatsLog.write(173, packageName, 2);
            Slog.w(TAG, "Unable to send as bubble -- activity is not resizable for intent: " + intent);
            return false;
        }
    }

    private void logBubbleError(String key, String failureMessage) {
    }
}
