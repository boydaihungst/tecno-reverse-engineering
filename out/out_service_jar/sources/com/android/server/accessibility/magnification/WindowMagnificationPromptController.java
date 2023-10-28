package com.android.server.accessibility.magnification;

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.notification.SystemNotificationChannels;
/* loaded from: classes.dex */
public class WindowMagnificationPromptController {
    static final String ACTION_DISMISS = "com.android.server.accessibility.magnification.action.DISMISS";
    static final String ACTION_TURN_ON_IN_SETTINGS = "com.android.server.accessibility.magnification.action.TURN_ON_IN_SETTINGS";
    private static final Uri MAGNIFICATION_WINDOW_MODE_PROMPT_URI = Settings.Secure.getUriFor("accessibility_show_window_magnification_prompt");
    private final ContentObserver mContentObserver;
    private final Context mContext;
    private boolean mNeedToShowNotification;
    BroadcastReceiver mNotificationActionReceiver;
    private final NotificationManager mNotificationManager;
    private final int mUserId;

    public WindowMagnificationPromptController(Context context, int userId) {
        this.mContext = context;
        this.mNotificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        this.mUserId = userId;
        ContentObserver contentObserver = new ContentObserver(null) { // from class: com.android.server.accessibility.magnification.WindowMagnificationPromptController.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                WindowMagnificationPromptController.this.onPromptSettingsValueChanged();
            }
        };
        this.mContentObserver = contentObserver;
        context.getContentResolver().registerContentObserver(MAGNIFICATION_WINDOW_MODE_PROMPT_URI, false, contentObserver, userId);
        this.mNeedToShowNotification = isWindowMagnificationPromptEnabled();
    }

    protected void onPromptSettingsValueChanged() {
        boolean needToShowNotification = isWindowMagnificationPromptEnabled();
        if (this.mNeedToShowNotification == needToShowNotification) {
            return;
        }
        this.mNeedToShowNotification = needToShowNotification;
        if (!needToShowNotification) {
            unregisterReceiverIfNeeded();
            this.mNotificationManager.cancel(1004);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showNotificationIfNeeded() {
        if (this.mNeedToShowNotification) {
            Notification.Builder notificationBuilder = new Notification.Builder(this.mContext, SystemNotificationChannels.ACCESSIBILITY_MAGNIFICATION);
            String message = this.mContext.getString(17041776);
            notificationBuilder.setSmallIcon(17302323).setContentTitle(this.mContext.getString(17041777)).setContentText(message).setLargeIcon(Icon.createWithResource(this.mContext, 17302328)).setTicker(this.mContext.getString(17041777)).setOnlyAlertOnce(true).setStyle(new Notification.BigTextStyle().bigText(message)).setDeleteIntent(createPendingIntent(ACTION_DISMISS)).setContentIntent(createPendingIntent(ACTION_TURN_ON_IN_SETTINGS)).setActions(buildTurnOnAction());
            this.mNotificationManager.notify(1004, notificationBuilder.build());
            registerReceiverIfNeeded();
        }
    }

    public void onDestroy() {
        dismissNotification();
        this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
    }

    private boolean isWindowMagnificationPromptEnabled() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_show_window_magnification_prompt", 0, this.mUserId) == 1;
    }

    private Notification.Action buildTurnOnAction() {
        return new Notification.Action.Builder((Icon) null, this.mContext.getString(17041646), createPendingIntent(ACTION_TURN_ON_IN_SETTINGS)).build();
    }

    private PendingIntent createPendingIntent(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(this.mContext.getPackageName());
        return PendingIntent.getBroadcast(this.mContext, 0, intent, 67108864);
    }

    private void registerReceiverIfNeeded() {
        if (this.mNotificationActionReceiver != null) {
            return;
        }
        this.mNotificationActionReceiver = new NotificationActionReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DISMISS);
        intentFilter.addAction(ACTION_TURN_ON_IN_SETTINGS);
        this.mContext.registerReceiver(this.mNotificationActionReceiver, intentFilter, "android.permission.MANAGE_ACCESSIBILITY", null, 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void launchMagnificationSettings() {
        Intent intent = new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS");
        intent.addFlags(268468224);
        intent.putExtra("android.intent.extra.COMPONENT_NAME", AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME.flattenToShortString());
        intent.addFlags(268435456);
        Bundle bundle = ActivityOptions.makeBasic().setLaunchDisplayId(this.mContext.getDisplayId()).toBundle();
        this.mContext.startActivityAsUser(intent, bundle, UserHandle.of(this.mUserId));
        ((StatusBarManager) this.mContext.getSystemService(StatusBarManager.class)).collapsePanels();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dismissNotification() {
        unregisterReceiverIfNeeded();
        this.mNotificationManager.cancel(1004);
    }

    private void unregisterReceiverIfNeeded() {
        BroadcastReceiver broadcastReceiver = this.mNotificationActionReceiver;
        if (broadcastReceiver == null) {
            return;
        }
        this.mContext.unregisterReceiver(broadcastReceiver);
        this.mNotificationActionReceiver = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class NotificationActionReceiver extends BroadcastReceiver {
        private NotificationActionReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            WindowMagnificationPromptController.this.mNeedToShowNotification = false;
            Settings.Secure.putIntForUser(WindowMagnificationPromptController.this.mContext.getContentResolver(), "accessibility_show_window_magnification_prompt", 0, WindowMagnificationPromptController.this.mUserId);
            if (WindowMagnificationPromptController.ACTION_TURN_ON_IN_SETTINGS.equals(action)) {
                WindowMagnificationPromptController.this.launchMagnificationSettings();
                WindowMagnificationPromptController.this.dismissNotification();
            } else if (WindowMagnificationPromptController.ACTION_DISMISS.equals(action)) {
                WindowMagnificationPromptController.this.dismissNotification();
            }
        }
    }
}
