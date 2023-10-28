package com.transsion.hubcore.server.alarm;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.SparseIntArray;
import com.transsion.hubcore.server.alarm.ITranAlarmManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAlarmManagerService {
    public static final boolean DEBUG = true;
    public static final String TAG = "ITranAlarmManagerService";
    public static final TranClassInfo<ITranAlarmManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.alarm.TranAlarmManagerServiceImpl", ITranAlarmManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.alarm.ITranAlarmManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAlarmManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAlarmManagerService {
    }

    @FunctionalInterface
    /* loaded from: classes2.dex */
    public interface IAlarmManagerServiceTroy {
        List<String> getAlarmListWhenUpdateNextAlarmClockLocked();
    }

    static ITranAlarmManagerService Instance() {
        return (ITranAlarmManagerService) classInfo.getImpl();
    }

    default void onNetworkReceiver(Context context) {
    }

    default void initSystemFlagApps(Context context) {
    }

    default void setImpl(Context context, PendingIntent operation, int type, long windowLength, int flags) {
    }

    default void setImplLocked(int flags, PendingIntent operation, String packageName, String statsTag) {
    }

    default boolean dump(String[] args, PrintWriter pw) {
        return false;
    }

    default void updateAlarms(IAlarmManagerServiceTroy troy) {
    }

    default boolean deliverAlarmsLocked(String packageName, int flags, String statsTag, int uid, SparseIntArray mAlarmsPerUid) {
        return false;
    }

    default void onInteractiveStateReceive(Intent intent) {
    }

    default void removeGmsAlarms(String packageName, int flags, PendingIntent operation) {
    }

    default void dumpHprof(int callingUid) {
    }
}
