package com.android.server.notification;

import android.content.Context;
import android.os.Handler;
import java.io.File;
/* loaded from: classes2.dex */
public class NotificationHistoryDatabaseFactory {
    private static NotificationHistoryDatabase sTestingNotificationHistoryDb;

    public static void setTestingNotificationHistoryDatabase(NotificationHistoryDatabase db) {
        sTestingNotificationHistoryDb = db;
    }

    public static NotificationHistoryDatabase create(Context context, Handler handler, File rootDir) {
        NotificationHistoryDatabase notificationHistoryDatabase = sTestingNotificationHistoryDb;
        if (notificationHistoryDatabase != null) {
            return notificationHistoryDatabase;
        }
        return new NotificationHistoryDatabase(context, handler, rootDir);
    }
}
