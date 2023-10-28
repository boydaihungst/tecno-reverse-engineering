package com.android.server.notification;

import android.util.Slog;
import java.util.Comparator;
/* loaded from: classes2.dex */
public class GlobalSortKeyComparator implements Comparator<NotificationRecord> {
    private static final String TAG = "GlobalSortComp";

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.util.Comparator
    public int compare(NotificationRecord left, NotificationRecord right) {
        if (left.getGlobalSortKey() == null) {
            Slog.wtf(TAG, "Missing left global sort key: " + left);
            return 1;
        } else if (right.getGlobalSortKey() == null) {
            Slog.wtf(TAG, "Missing right global sort key: " + right);
            return -1;
        } else {
            return left.getGlobalSortKey().compareTo(right.getGlobalSortKey());
        }
    }
}
