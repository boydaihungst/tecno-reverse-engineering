package com.android.server.notification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
/* loaded from: classes2.dex */
public class ReviewNotificationPermissionsReceiver extends BroadcastReceiver {
    private static final long JOB_RESCHEDULE_TIME = 604800000;
    public static final String TAG = "ReviewNotifPermissions";
    static final boolean DEBUG = Log.isLoggable(TAG, 3);

    /* JADX INFO: Access modifiers changed from: package-private */
    public static IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("REVIEW_NOTIF_ACTION_REMIND");
        filter.addAction("REVIEW_NOTIF_ACTION_DISMISS");
        filter.addAction("REVIEW_NOTIF_ACTION_CANCELED");
        return filter;
    }

    protected void cancelNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.cancel(NotificationManagerService.TAG, 71);
        } else {
            Slog.w(TAG, "could not cancel notification: NotificationManager not found");
        }
    }

    protected void rescheduleNotification(Context context) {
        ReviewNotificationPermissionsJobService.scheduleJob(context, 604800000L);
        if (DEBUG) {
            Slog.d(TAG, "Scheduled review permissions notification for on or after: " + LocalDateTime.now(ZoneId.systemDefault()).plus(604800000L, (TemporalUnit) ChronoUnit.MILLIS));
        }
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("REVIEW_NOTIF_ACTION_REMIND")) {
            rescheduleNotification(context);
            Settings.Global.putInt(context.getContentResolver(), "review_permissions_notification_state", 1);
            cancelNotification(context);
        } else if (action.equals("REVIEW_NOTIF_ACTION_DISMISS")) {
            Settings.Global.putInt(context.getContentResolver(), "review_permissions_notification_state", 2);
            cancelNotification(context);
        } else if (action.equals("REVIEW_NOTIF_ACTION_CANCELED")) {
            int notifState = Settings.Global.getInt(context.getContentResolver(), "review_permissions_notification_state", -1);
            if (notifState == 0) {
                rescheduleNotification(context);
                Settings.Global.putInt(context.getContentResolver(), "review_permissions_notification_state", 1);
            } else if (notifState == 3) {
                Settings.Global.putInt(context.getContentResolver(), "review_permissions_notification_state", 1);
            }
        }
    }
}
