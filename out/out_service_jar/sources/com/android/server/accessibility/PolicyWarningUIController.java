package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.accessibility.util.AccessibilityStatsLogUtils;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ImageUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.accessibility.PolicyWarningUIController;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class PolicyWarningUIController {
    protected static final String ACTION_A11Y_SETTINGS;
    protected static final String ACTION_DISMISS_NOTIFICATION;
    protected static final String ACTION_SEND_NOTIFICATION;
    private static final String EXTRA_TIME_FOR_LOGGING = "start_time_to_log_a11y_tool";
    private static final int SEND_NOTIFICATION_DELAY_HOURS = 24;
    private static final String TAG;
    private final AlarmManager mAlarmManager;
    private final Context mContext;
    private final ArraySet<ComponentName> mEnabledA11yServices = new ArraySet<>();
    private final Handler mMainHandler;
    private final NotificationController mNotificationController;

    static {
        String simpleName = PolicyWarningUIController.class.getSimpleName();
        TAG = simpleName;
        ACTION_SEND_NOTIFICATION = simpleName + ".ACTION_SEND_NOTIFICATION";
        ACTION_A11Y_SETTINGS = simpleName + ".ACTION_A11Y_SETTINGS";
        ACTION_DISMISS_NOTIFICATION = simpleName + ".ACTION_DISMISS_NOTIFICATION";
    }

    public PolicyWarningUIController(Handler handler, Context context, NotificationController notificationController) {
        this.mMainHandler = handler;
        this.mContext = context;
        this.mNotificationController = notificationController;
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SEND_NOTIFICATION);
        filter.addAction(ACTION_A11Y_SETTINGS);
        filter.addAction(ACTION_DISMISS_NOTIFICATION);
        context.registerReceiver(notificationController, filter, "android.permission.MANAGE_ACCESSIBILITY", handler, 2);
    }

    public void onSwitchUser(int userId, Set<ComponentName> enabledServices) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.PolicyWarningUIController$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                PolicyWarningUIController.this.onSwitchUserInternal(((Integer) obj).intValue(), (Set) obj2);
            }
        }, Integer.valueOf(userId), enabledServices));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSwitchUserInternal(int userId, Set<ComponentName> enabledServices) {
        this.mEnabledA11yServices.clear();
        this.mEnabledA11yServices.addAll(enabledServices);
        this.mNotificationController.onSwitchUser(userId);
    }

    public void onEnabledServicesChanged(int userId, Set<ComponentName> enabledServices) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.PolicyWarningUIController$$ExternalSyntheticLambda5
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                PolicyWarningUIController.this.onEnabledServicesChangedInternal(((Integer) obj).intValue(), (Set) obj2);
            }
        }, Integer.valueOf(userId), enabledServices));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onEnabledServicesChangedInternal(int userId, Set<ComponentName> enabledServices) {
        ArraySet<ComponentName> disabledServices = new ArraySet<>(this.mEnabledA11yServices);
        disabledServices.removeAll(enabledServices);
        this.mEnabledA11yServices.clear();
        this.mEnabledA11yServices.addAll(enabledServices);
        Handler handler = this.mMainHandler;
        final NotificationController notificationController = this.mNotificationController;
        Objects.requireNonNull(notificationController);
        handler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.PolicyWarningUIController$$ExternalSyntheticLambda4
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                PolicyWarningUIController.NotificationController.this.onServicesDisabled(((Integer) obj).intValue(), (ArraySet) obj2);
            }
        }, Integer.valueOf(userId), disabledServices));
    }

    public void onNonA11yCategoryServiceBound(int userId, ComponentName service) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.PolicyWarningUIController$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                PolicyWarningUIController.this.setAlarm(((Integer) obj).intValue(), (ComponentName) obj2);
            }
        }, Integer.valueOf(userId), service));
    }

    public void onNonA11yCategoryServiceUnbound(int userId, ComponentName service) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.accessibility.PolicyWarningUIController$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                PolicyWarningUIController.this.cancelAlarm(((Integer) obj).intValue(), (ComponentName) obj2);
            }
        }, Integer.valueOf(userId), service));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAlarm(int userId, ComponentName service) {
        Calendar cal = Calendar.getInstance();
        cal.add(10, 24);
        this.mAlarmManager.set(0, cal.getTimeInMillis(), createPendingIntent(this.mContext, userId, ACTION_SEND_NOTIFICATION, service));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelAlarm(int userId, ComponentName service) {
        this.mAlarmManager.cancel(createPendingIntent(this.mContext, userId, ACTION_SEND_NOTIFICATION, service));
    }

    protected static PendingIntent createPendingIntent(Context context, int userId, String action, ComponentName serviceComponentName) {
        return PendingIntent.getBroadcast(context, 0, createIntent(context, userId, action, serviceComponentName), 67108864);
    }

    protected static Intent createIntent(Context context, int userId, String action, ComponentName serviceComponentName) {
        Intent intent = new Intent(action);
        intent.setPackage(context.getPackageName()).setIdentifier(serviceComponentName.flattenToShortString()).putExtra("android.intent.extra.COMPONENT_NAME", serviceComponentName).putExtra("android.intent.extra.USER_ID", userId).putExtra("android.intent.extra.TIME", SystemClock.elapsedRealtime());
        return intent;
    }

    public void enableSendingNonA11yToolNotification(boolean enable) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.accessibility.PolicyWarningUIController$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PolicyWarningUIController.this.enableSendingNonA11yToolNotificationInternal(((Boolean) obj).booleanValue());
            }
        }, Boolean.valueOf(enable)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableSendingNonA11yToolNotificationInternal(boolean enable) {
        this.mNotificationController.setSendingNotification(enable);
    }

    /* loaded from: classes.dex */
    public static class NotificationController extends BroadcastReceiver {
        private static final char RECORD_SEPARATOR = ':';
        private final Context mContext;
        private int mCurrentUserId;
        private final NotificationManager mNotificationManager;
        private boolean mSendNotification;
        private final ArraySet<ComponentName> mNotifiedA11yServices = new ArraySet<>();
        private final List<ComponentName> mSentA11yServiceNotification = new ArrayList();

        public NotificationController(Context context) {
            this.mContext = context;
            this.mNotificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            ComponentName componentName = (ComponentName) intent.getParcelableExtra("android.intent.extra.COMPONENT_NAME");
            if (TextUtils.isEmpty(action) || componentName == null) {
                return;
            }
            long startTimeMills = intent.getLongExtra("android.intent.extra.TIME", 0L);
            long durationMills = startTimeMills > 0 ? SystemClock.elapsedRealtime() - startTimeMills : 0L;
            int userId = intent.getIntExtra("android.intent.extra.USER_ID", 0);
            if (PolicyWarningUIController.ACTION_SEND_NOTIFICATION.equals(action)) {
                if (trySendNotification(userId, componentName)) {
                    AccessibilityStatsLogUtils.logNonA11yToolServiceWarningReported(componentName.getPackageName(), AccessibilityStatsLogUtils.ACCESSIBILITY_PRIVACY_WARNING_STATUS_SHOWN, durationMills);
                }
            } else if (PolicyWarningUIController.ACTION_A11Y_SETTINGS.equals(action)) {
                if (tryLaunchSettings(userId, componentName)) {
                    AccessibilityStatsLogUtils.logNonA11yToolServiceWarningReported(componentName.getPackageName(), AccessibilityStatsLogUtils.ACCESSIBILITY_PRIVACY_WARNING_STATUS_CLICKED, durationMills);
                }
                this.mNotificationManager.cancel(componentName.flattenToShortString(), 1005);
                this.mSentA11yServiceNotification.remove(componentName);
                onNotificationCanceled(userId, componentName);
            } else if (PolicyWarningUIController.ACTION_DISMISS_NOTIFICATION.equals(action)) {
                this.mSentA11yServiceNotification.remove(componentName);
                onNotificationCanceled(userId, componentName);
            }
        }

        protected void onSwitchUser(int userId) {
            cancelSentNotifications();
            this.mNotifiedA11yServices.clear();
            this.mCurrentUserId = userId;
            this.mNotifiedA11yServices.addAll((ArraySet<? extends ComponentName>) readNotifiedServiceList(userId));
        }

        /* JADX INFO: Access modifiers changed from: protected */
        public void onServicesDisabled(int userId, ArraySet<ComponentName> disabledServices) {
            if (this.mNotifiedA11yServices.removeAll((ArraySet<? extends ComponentName>) disabledServices)) {
                writeNotifiedServiceList(userId, this.mNotifiedA11yServices);
            }
        }

        private boolean trySendNotification(int userId, ComponentName componentName) {
            if (userId == this.mCurrentUserId && this.mSendNotification) {
                List<AccessibilityServiceInfo> enabledServiceInfos = getEnabledServiceInfos();
                int i = 0;
                while (true) {
                    if (i >= enabledServiceInfos.size()) {
                        break;
                    }
                    AccessibilityServiceInfo a11yServiceInfo = enabledServiceInfos.get(i);
                    if (!componentName.flattenToShortString().equals(a11yServiceInfo.getComponentName().flattenToShortString())) {
                        i++;
                    } else if (!a11yServiceInfo.isAccessibilityTool() && !this.mNotifiedA11yServices.contains(componentName)) {
                        CharSequence displayName = a11yServiceInfo.getResolveInfo().serviceInfo.loadLabel(this.mContext.getPackageManager());
                        Drawable drawable = a11yServiceInfo.getResolveInfo().loadIcon(this.mContext.getPackageManager());
                        int size = this.mContext.getResources().getDimensionPixelSize(17104896);
                        sendNotification(userId, componentName, displayName, ImageUtils.buildScaledBitmap(drawable, size, size));
                        return true;
                    }
                }
                return false;
            }
            return false;
        }

        private boolean tryLaunchSettings(int userId, ComponentName componentName) {
            if (userId != this.mCurrentUserId) {
                return false;
            }
            Intent intent = new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS");
            intent.addFlags(268468224);
            intent.putExtra("android.intent.extra.COMPONENT_NAME", componentName.flattenToShortString());
            intent.putExtra(PolicyWarningUIController.EXTRA_TIME_FOR_LOGGING, SystemClock.elapsedRealtime());
            Bundle bundle = ActivityOptions.makeBasic().setLaunchDisplayId(this.mContext.getDisplayId()).toBundle();
            this.mContext.startActivityAsUser(intent, bundle, UserHandle.of(userId));
            ((StatusBarManager) this.mContext.getSystemService(StatusBarManager.class)).collapsePanels();
            return true;
        }

        protected void onNotificationCanceled(int userId, ComponentName componentName) {
            if (userId == this.mCurrentUserId && this.mNotifiedA11yServices.add(componentName)) {
                writeNotifiedServiceList(userId, this.mNotifiedA11yServices);
            }
        }

        private void sendNotification(int userId, ComponentName serviceComponentName, CharSequence name, Bitmap bitmap) {
            Notification.Builder notificationBuilder = new Notification.Builder(this.mContext, SystemNotificationChannels.ACCESSIBILITY_SECURITY_POLICY);
            notificationBuilder.setSmallIcon(17302323).setContentTitle(this.mContext.getString(17041694)).setContentText(this.mContext.getString(17041693, name)).setStyle(new Notification.BigTextStyle().bigText(this.mContext.getString(17041693, name))).setTicker(this.mContext.getString(17041694)).setOnlyAlertOnce(true).setDeleteIntent(PolicyWarningUIController.createPendingIntent(this.mContext, userId, PolicyWarningUIController.ACTION_DISMISS_NOTIFICATION, serviceComponentName)).setContentIntent(PolicyWarningUIController.createPendingIntent(this.mContext, userId, PolicyWarningUIController.ACTION_A11Y_SETTINGS, serviceComponentName));
            if (bitmap != null) {
                notificationBuilder.setLargeIcon(bitmap);
            }
            this.mNotificationManager.notify(serviceComponentName.flattenToShortString(), 1005, notificationBuilder.build());
            this.mSentA11yServiceNotification.add(serviceComponentName);
        }

        private ArraySet<ComponentName> readNotifiedServiceList(int userId) {
            String notifiedServiceSetting = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "notified_non_accessibility_category_services", userId);
            if (TextUtils.isEmpty(notifiedServiceSetting)) {
                return new ArraySet<>();
            }
            TextUtils.StringSplitter componentNameSplitter = new TextUtils.SimpleStringSplitter(RECORD_SEPARATOR);
            componentNameSplitter.setString(notifiedServiceSetting);
            ArraySet<ComponentName> notifiedServices = new ArraySet<>();
            for (String componentNameString : componentNameSplitter) {
                ComponentName notifiedService = ComponentName.unflattenFromString(componentNameString);
                if (notifiedService != null) {
                    notifiedServices.add(notifiedService);
                }
            }
            return notifiedServices;
        }

        private void writeNotifiedServiceList(int userId, ArraySet<ComponentName> services) {
            StringBuilder notifiedServicesBuilder = new StringBuilder();
            for (int i = 0; i < services.size(); i++) {
                if (i > 0) {
                    notifiedServicesBuilder.append(RECORD_SEPARATOR);
                }
                ComponentName notifiedService = services.valueAt(i);
                notifiedServicesBuilder.append(notifiedService.flattenToShortString());
            }
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "notified_non_accessibility_category_services", notifiedServicesBuilder.toString(), userId);
        }

        protected List<AccessibilityServiceInfo> getEnabledServiceInfos() {
            AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(this.mContext);
            return accessibilityManager.getEnabledAccessibilityServiceList(-1);
        }

        private void cancelSentNotifications() {
            this.mSentA11yServiceNotification.forEach(new Consumer() { // from class: com.android.server.accessibility.PolicyWarningUIController$NotificationController$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    PolicyWarningUIController.NotificationController.this.m684xcb3e2751((ComponentName) obj);
                }
            });
            this.mSentA11yServiceNotification.clear();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$cancelSentNotifications$0$com-android-server-accessibility-PolicyWarningUIController$NotificationController  reason: not valid java name */
        public /* synthetic */ void m684xcb3e2751(ComponentName componentName) {
            this.mNotificationManager.cancel(componentName.flattenToShortString(), 1005);
        }

        void setSendingNotification(boolean enable) {
            this.mSendNotification = enable;
        }
    }
}
