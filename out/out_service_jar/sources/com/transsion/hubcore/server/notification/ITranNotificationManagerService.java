package com.transsion.hubcore.server.notification;

import android.content.Context;
import android.os.Handler;
import android.service.notification.NotificationStats;
import android.service.notification.StatusBarNotification;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.NotificationManagerService;
import com.android.server.notification.NotificationRecord;
import com.transsion.hubcore.server.notification.ITranNotificationManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranNotificationManagerService {
    public static final TranClassInfo<ITranNotificationManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.notification.TranNotificationManagerServiceImpl", ITranNotificationManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.notification.ITranNotificationManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranNotificationManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranNotificationManagerService {
    }

    static ITranNotificationManagerService Instance() {
        return (ITranNotificationManagerService) classInfo.getImpl();
    }

    default Boolean onIsVisibleToListener(StatusBarNotification sbn, ManagedServices.ManagedServiceInfo listener) {
        return null;
    }

    default void onBootPhase(Handler handle, Context context) {
    }

    default boolean filter(Context context, StatusBarNotification sbn, Runnable r) {
        return false;
    }

    default boolean onEnqueueNotificationInternal(Handler handler, Supplier<Runnable> runnableSupplier, NotificationRecord notificationRecord, StatusBarNotification sbn, int callingUid) {
        return false;
    }

    default boolean onCancelNotification(Handler handler, Supplier<Runnable> runnableSupplier, int callingUid, int callingPid, String pkg, String tag, int id, int mustHaveFlags, int mustNotHaveFlags, boolean sendDelete, int userId, int reason, int rank, int count, ManagedServices.ManagedServiceInfo listener) {
        return false;
    }

    default StatusBarNotification onNotifyPosted(StatusBarNotification sbn, ManagedServices.ManagedServiceInfo info) {
        return sbn;
    }

    default boolean onNotifyRemoved(StatusBarNotification sbn, NotificationStats ntStats, int reason) {
        return false;
    }

    default void initNotifyScreenOn(NotificationManagerService nms, Handler handler) {
    }

    default void notificationPostedScreenOn(StatusBarNotification sbn) {
    }
}
