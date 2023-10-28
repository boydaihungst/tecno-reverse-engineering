package com.android.server.biometrics.sensors;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Slog;
import com.transsion.griffin.Griffin;
/* loaded from: classes.dex */
public class BiometricNotificationUtils {
    private static final String BAD_CALIBRATION_NOTIFICATION_TAG = "FingerprintService";
    private static final int NOTIFICATION_ID = 1;
    private static final long NOTIFICATION_INTERVAL_MS = 86400000;
    private static final String RE_ENROLL_NOTIFICATION_TAG = "FaceService";
    private static final String TAG = "BiometricNotificationUtils";
    private static long sLastAlertTime = 0;

    public static void showReEnrollmentNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        String name = context.getString(17040309);
        String title = context.getString(17040310);
        String content = context.getString(17040308);
        Intent intent = new Intent("android.settings.FACE_SETTINGS");
        intent.setPackage("com.android.settings");
        PendingIntent pendingIntent = PendingIntent.getActivityAsUser(context, 0, intent, 67108864, null, UserHandle.CURRENT);
        showNotificationHelper(context, name, title, content, pendingIntent, "FaceEnrollNotificationChannel", RE_ENROLL_NOTIFICATION_TAG);
    }

    public static void showBadCalibrationNotification(Context context) {
        long currentTime = SystemClock.elapsedRealtime();
        long j = sLastAlertTime;
        long timeSinceLastAlert = currentTime - j;
        if (j != 0 && timeSinceLastAlert < 86400000) {
            Slog.v(TAG, "Skipping calibration notification : " + timeSinceLastAlert);
            return;
        }
        sLastAlertTime = currentTime;
        String name = context.getString(17040363);
        String title = context.getString(17040364);
        String content = context.getString(17040362);
        Intent intent = new Intent("android.settings.FINGERPRINT_SETTINGS");
        intent.setPackage("com.android.settings");
        PendingIntent pendingIntent = PendingIntent.getActivityAsUser(context, 0, intent, 67108864, null, UserHandle.CURRENT);
        showNotificationHelper(context, name, title, content, pendingIntent, "FingerprintBadCalibrationNotificationChannel", BAD_CALIBRATION_NOTIFICATION_TAG);
    }

    private static void showNotificationHelper(Context context, String name, String title, String content, PendingIntent pendingIntent, String channelName, String notificationTag) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(channelName, name, 4);
        Notification notification = new Notification.Builder(context, channelName).setSmallIcon(17302516).setContentTitle(title).setContentText(content).setSubText(name).setOnlyAlertOnce(true).setLocalOnly(true).setAutoCancel(true).setCategory(Griffin.SYS).setContentIntent(pendingIntent).setVisibility(-1).build();
        notificationManager.createNotificationChannel(channel);
        notificationManager.notifyAsUser(notificationTag, 1, notification, UserHandle.CURRENT);
    }

    public static void cancelReEnrollNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        notificationManager.cancelAsUser(RE_ENROLL_NOTIFICATION_TAG, 1, UserHandle.CURRENT);
    }

    public static void cancelBadCalibrationNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        notificationManager.cancelAsUser(BAD_CALIBRATION_NOTIFICATION_TAG, 1, UserHandle.CURRENT);
    }
}
