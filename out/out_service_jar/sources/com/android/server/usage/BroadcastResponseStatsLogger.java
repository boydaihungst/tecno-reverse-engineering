package com.android.server.usage;

import android.app.ActivityManager;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.RingBuffer;
/* loaded from: classes2.dex */
public class BroadcastResponseStatsLogger {
    private static final int MAX_LOG_SIZE;
    private final LogBuffer mBroadcastEventsBuffer;
    private final Object mLock = new Object();
    private final LogBuffer mNotificationEventsBuffer;

    /* loaded from: classes2.dex */
    public interface Data {
        void reset();
    }

    public BroadcastResponseStatsLogger() {
        int i = MAX_LOG_SIZE;
        this.mBroadcastEventsBuffer = new LogBuffer(BroadcastEvent.class, i);
        this.mNotificationEventsBuffer = new LogBuffer(NotificationEvent.class, i);
    }

    static {
        MAX_LOG_SIZE = ActivityManager.isLowRamDeviceStatic() ? 20 : 50;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logBroadcastDispatchEvent(int sourceUid, String targetPackage, UserHandle targetUser, long idForResponseEvent, long timeStampMs, int targetUidProcessState) {
        synchronized (this.mLock) {
            if (UsageStatsService.DEBUG_RESPONSE_STATS) {
                Slog.d("ResponseStatsTracker", getBroadcastDispatchEventLog(sourceUid, targetPackage, targetUser.getIdentifier(), idForResponseEvent, timeStampMs, targetUidProcessState));
            }
            this.mBroadcastEventsBuffer.logBroadcastDispatchEvent(sourceUid, targetPackage, targetUser, idForResponseEvent, timeStampMs, targetUidProcessState);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logNotificationEvent(int event, String packageName, UserHandle user, long timestampMs) {
        synchronized (this.mLock) {
            if (UsageStatsService.DEBUG_RESPONSE_STATS) {
                Slog.d("ResponseStatsTracker", getNotificationEventLog(event, packageName, user.getIdentifier(), timestampMs));
            }
            this.mNotificationEventsBuffer.logNotificationEvent(event, packageName, user, timestampMs);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpLogs(IndentingPrintWriter ipw) {
        synchronized (this.mLock) {
            ipw.println("Broadcast events (most recent first):");
            ipw.increaseIndent();
            this.mBroadcastEventsBuffer.reverseDump(ipw);
            ipw.decreaseIndent();
            ipw.println();
            ipw.println("Notification events (most recent first):");
            ipw.increaseIndent();
            this.mNotificationEventsBuffer.reverseDump(ipw);
            ipw.decreaseIndent();
        }
    }

    /* loaded from: classes2.dex */
    private static final class LogBuffer<T extends Data> extends RingBuffer<T> {
        LogBuffer(Class<T> classType, int capacity) {
            super(classType, capacity);
        }

        void logBroadcastDispatchEvent(int sourceUid, String targetPackage, UserHandle targetUser, long idForResponseEvent, long timeStampMs, int targetUidProcessState) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            BroadcastEvent event = (BroadcastEvent) data;
            event.sourceUid = sourceUid;
            event.targetUserId = targetUser.getIdentifier();
            event.targetUidProcessState = targetUidProcessState;
            event.targetPackage = targetPackage;
            event.idForResponseEvent = idForResponseEvent;
            event.timestampMs = timeStampMs;
        }

        void logNotificationEvent(int type, String packageName, UserHandle user, long timestampMs) {
            Data data = (Data) getNextSlot();
            if (data == null) {
                return;
            }
            data.reset();
            NotificationEvent event = (NotificationEvent) data;
            event.type = type;
            event.packageName = packageName;
            event.userId = user.getIdentifier();
            event.timestampMs = timestampMs;
        }

        public void reverseDump(IndentingPrintWriter pw) {
            Data[] allData = (Data[]) toArray();
            for (int i = allData.length - 1; i >= 0; i--) {
                if (allData[i] != null) {
                    pw.println(getContent(allData[i]));
                }
            }
        }

        public String getContent(Data data) {
            return data.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getBroadcastDispatchEventLog(int sourceUid, String targetPackage, int targetUserId, long idForResponseEvent, long timestampMs, int targetUidProcState) {
        return TextUtils.formatSimple("broadcast:%s; srcUid=%d, tgtPkg=%s, tgtUsr=%d, id=%d, state=%s", new Object[]{TimeUtils.formatDuration(timestampMs), Integer.valueOf(sourceUid), targetPackage, Integer.valueOf(targetUserId), Long.valueOf(idForResponseEvent), ActivityManager.procStateToString(targetUidProcState)});
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getNotificationEventLog(int event, String packageName, int userId, long timestampMs) {
        return TextUtils.formatSimple("notification:%s; event=<%s>, pkg=%s, usr=%d", new Object[]{TimeUtils.formatDuration(timestampMs), notificationEventToString(event), packageName, Integer.valueOf(userId)});
    }

    private static String notificationEventToString(int event) {
        switch (event) {
            case 0:
                return "posted";
            case 1:
                return "updated";
            case 2:
                return "cancelled";
            default:
                return String.valueOf(event);
        }
    }

    /* loaded from: classes2.dex */
    public static final class BroadcastEvent implements Data {
        public long idForResponseEvent;
        public int sourceUid;
        public String targetPackage;
        public int targetUidProcessState;
        public int targetUserId;
        public long timestampMs;

        @Override // com.android.server.usage.BroadcastResponseStatsLogger.Data
        public void reset() {
            this.targetPackage = null;
        }

        public String toString() {
            return BroadcastResponseStatsLogger.getBroadcastDispatchEventLog(this.sourceUid, this.targetPackage, this.targetUserId, this.idForResponseEvent, this.timestampMs, this.targetUidProcessState);
        }
    }

    /* loaded from: classes2.dex */
    public static final class NotificationEvent implements Data {
        public String packageName;
        public long timestampMs;
        public int type;
        public int userId;

        @Override // com.android.server.usage.BroadcastResponseStatsLogger.Data
        public void reset() {
            this.packageName = null;
        }

        public String toString() {
            return BroadcastResponseStatsLogger.getNotificationEventLog(this.type, this.packageName, this.userId, this.timestampMs);
        }
    }
}
