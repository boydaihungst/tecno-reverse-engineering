package com.android.server.wm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Bundle;
import com.android.internal.util.ImageUtils;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class AlertWindowNotification {
    private static final String CHANNEL_PREFIX = "com.android.server.wm.AlertWindowNotification - ";
    private static final int NOTIFICATION_ID = 0;
    private static NotificationChannelGroup sChannelGroup;
    private static int sNextRequestCode = 0;
    private final NotificationManager mNotificationManager;
    private String mNotificationTag;
    private final String mPackageName;
    private boolean mPosted;
    private final int mRequestCode;
    private final WindowManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AlertWindowNotification(WindowManagerService service, String packageName) {
        this.mService = service;
        this.mPackageName = packageName;
        this.mNotificationManager = (NotificationManager) service.mContext.getSystemService("notification");
        this.mNotificationTag = CHANNEL_PREFIX + packageName;
        int i = sNextRequestCode;
        sNextRequestCode = i + 1;
        this.mRequestCode = i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void post() {
        this.mService.mH.post(new Runnable() { // from class: com.android.server.wm.AlertWindowNotification$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AlertWindowNotification.this.onPostNotification();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancel(final boolean deleteChannel) {
        this.mService.mH.post(new Runnable() { // from class: com.android.server.wm.AlertWindowNotification$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AlertWindowNotification.this.m7850lambda$cancel$0$comandroidserverwmAlertWindowNotification(deleteChannel);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: onCancelNotification */
    public void m7850lambda$cancel$0$comandroidserverwmAlertWindowNotification(boolean deleteChannel) {
        if (!this.mPosted) {
            return;
        }
        this.mPosted = false;
        this.mNotificationManager.cancel(this.mNotificationTag, 0);
        if (deleteChannel) {
            this.mNotificationManager.deleteNotificationChannel(this.mNotificationTag);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPostNotification() {
        if (this.mPosted) {
            return;
        }
        this.mPosted = true;
        Context context = this.mService.mContext;
        PackageManager pm = context.getPackageManager();
        ApplicationInfo aInfo = getApplicationInfo(pm, this.mPackageName);
        String appName = aInfo != null ? pm.getApplicationLabel(aInfo).toString() : this.mPackageName;
        createNotificationChannel(context, appName);
        String message = context.getString(17039657, appName);
        Bundle extras = new Bundle();
        extras.putStringArray("android.foregroundApps", new String[]{this.mPackageName});
        Notification.Builder builder = new Notification.Builder(context, this.mNotificationTag).setOngoing(true).setContentTitle(context.getString(17039658, appName)).setContentText(message).setSmallIcon(17301714).setColor(context.getColor(17170460)).setStyle(new Notification.BigTextStyle().bigText(message)).setLocalOnly(true).addExtras(extras).setContentIntent(getContentIntent(context, this.mPackageName));
        if (aInfo != null) {
            Drawable drawable = pm.getApplicationIcon(aInfo);
            int size = context.getResources().getDimensionPixelSize(17104896);
            Bitmap bitmap = ImageUtils.buildScaledBitmap(drawable, size, size);
            if (bitmap != null) {
                builder.setLargeIcon(bitmap);
            }
        }
        this.mNotificationManager.notify(this.mNotificationTag, 0, builder.build());
    }

    private PendingIntent getContentIntent(Context context, String packageName) {
        Intent intent = new Intent("android.settings.MANAGE_APP_OVERLAY_PERMISSION", Uri.fromParts("package", packageName, null));
        intent.setFlags(268468224);
        return PendingIntent.getActivity(context, this.mRequestCode, intent, AudioFormat.AAC_ADIF);
    }

    private void createNotificationChannel(Context context, String appName) {
        if (sChannelGroup == null) {
            NotificationChannelGroup notificationChannelGroup = new NotificationChannelGroup(CHANNEL_PREFIX, this.mService.mContext.getString(17039655));
            sChannelGroup = notificationChannelGroup;
            this.mNotificationManager.createNotificationChannelGroup(notificationChannelGroup);
        }
        String nameChannel = context.getString(17039656, appName);
        if (this.mNotificationManager.getNotificationChannel(this.mNotificationTag) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(this.mNotificationTag, nameChannel, 1);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setBlockable(true);
        channel.setGroup(sChannelGroup.getId());
        channel.setBypassDnd(true);
        this.mNotificationManager.createNotificationChannel(channel);
    }

    private ApplicationInfo getApplicationInfo(PackageManager pm, String packageName) {
        try {
            return pm.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
