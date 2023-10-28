package com.android.server.notification;

import android.content.Context;
import android.os.Handler;
import android.service.notification.NotificationStats;
import android.service.notification.StatusBarNotification;
import com.android.server.SystemService;
import com.android.server.notification.INotificationManagerServiceLice;
import com.android.server.notification.ManagedServices;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.io.PrintWriter;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface INotificationManagerServiceLice {
    public static final LiceInfo<INotificationManagerServiceLice> sLiceInfo = new LiceInfo<>("com.transsion.server.notification.NotificationManagerServiceLice", INotificationManagerServiceLice.class, new Supplier() { // from class: com.android.server.notification.INotificationManagerServiceLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new INotificationManagerServiceLice.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements INotificationManagerServiceLice {
    }

    static INotificationManagerServiceLice Instance() {
        return (INotificationManagerServiceLice) sLiceInfo.getImpl();
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

    default void onUpdateNotificationRecordImportance(Context context, String pkg, int uid, NotificationRecord r) {
    }

    default void onDump(PrintWriter pw) {
    }

    default void onUserUnlocked(Context context, SystemService.TargetUser user) {
    }

    default void onPackagesChanged(boolean removingPackage, int changeUserId, String[] pkgList, int[] uidList) {
    }
}
