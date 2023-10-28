package com.android.server.alarm;

import android.app.ActivityManager;
import android.app.StatsManager;
import android.content.Context;
import android.os.SystemClock;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.FrameworkStatsLog;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class MetricsHelper {
    private final Context mContext;
    private final Object mLock;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MetricsHelper(Context context, Object lock) {
        this.mContext = context;
        this.mLock = lock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerPuller(final Supplier<AlarmStore> alarmStoreSupplier) {
        StatsManager statsManager = (StatsManager) this.mContext.getSystemService(StatsManager.class);
        statsManager.setPullAtomCallback((int) FrameworkStatsLog.PENDING_ALARM_INFO, (StatsManager.PullAtomMetadata) null, BackgroundThread.getExecutor(), new StatsManager.StatsPullAtomCallback() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda0
            public final int onPullAtom(int i, List list) {
                return MetricsHelper.this.m961lambda$registerPuller$12$comandroidserveralarmMetricsHelper(alarmStoreSupplier, i, list);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerPuller$12$com-android-server-alarm-MetricsHelper  reason: not valid java name */
    public /* synthetic */ int m961lambda$registerPuller$12$comandroidserveralarmMetricsHelper(Supplier alarmStoreSupplier, int atomTag, List data) {
        Object obj;
        if (atomTag != 10106) {
            throw new UnsupportedOperationException("Unknown tag" + atomTag);
        }
        final long now = SystemClock.elapsedRealtime();
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                try {
                    AlarmStore alarmStore = (AlarmStore) alarmStoreSupplier.get();
                    obj = obj2;
                    try {
                        data.add(FrameworkStatsLog.buildStatsEvent(atomTag, alarmStore.size(), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda1
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                return MetricsHelper.lambda$registerPuller$0((Alarm) obj3);
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda4
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                boolean z;
                                z = ((Alarm) obj3).wakeup;
                                return z;
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda5
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                return MetricsHelper.lambda$registerPuller$2((Alarm) obj3);
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda6
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                return MetricsHelper.lambda$registerPuller$3((Alarm) obj3);
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda7
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                return MetricsHelper.lambda$registerPuller$4((Alarm) obj3);
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda8
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                return MetricsHelper.lambda$registerPuller$5((Alarm) obj3);
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda9
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                return MetricsHelper.lambda$registerPuller$6((Alarm) obj3);
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda10
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                return MetricsHelper.lambda$registerPuller$7((Alarm) obj3);
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda11
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                return MetricsHelper.lambda$registerPuller$8(now, (Alarm) obj3);
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda12
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                return MetricsHelper.lambda$registerPuller$9((Alarm) obj3);
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda2
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                return MetricsHelper.lambda$registerPuller$10((Alarm) obj3);
                            }
                        }), alarmStore.getCount(new Predicate() { // from class: com.android.server.alarm.MetricsHelper$$ExternalSyntheticLambda3
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj3) {
                                boolean isRtc;
                                isRtc = AlarmManagerService.isRtc(((Alarm) obj3).type);
                                return isRtc;
                            }
                        })));
                        return 0;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    obj = obj2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerPuller$0(Alarm a) {
        return a.windowLength == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerPuller$2(Alarm a) {
        return (a.flags & 4) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerPuller$3(Alarm a) {
        return (a.flags & 64) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerPuller$4(Alarm a) {
        return a.operation != null && a.operation.isForegroundService();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerPuller$5(Alarm a) {
        return a.operation != null && a.operation.isActivity();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerPuller$6(Alarm a) {
        return a.operation != null && a.operation.isService();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerPuller$7(Alarm a) {
        return a.listener != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerPuller$8(long now, Alarm a) {
        return a.getRequestedElapsed() > 31536000000L + now;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerPuller$9(Alarm a) {
        return a.repeatInterval != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$registerPuller$10(Alarm a) {
        return a.alarmClock != null;
    }

    private static int reasonToStatsReason(int reasonCode) {
        switch (reasonCode) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void pushAlarmScheduled(Alarm a, int callerProcState) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.ALARM_SCHEDULED, a.uid, a.windowLength == 0, a.wakeup, (a.flags & 4) != 0, a.alarmClock != null, a.repeatInterval != 0, reasonToStatsReason(a.mExactAllowReason), AlarmManagerService.isRtc(a.type), ActivityManager.processStateAmToProto(callerProcState));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void pushAlarmBatchDelivered(int numAlarms, int wakeups) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.ALARM_BATCH_DELIVERED, numAlarms, wakeups);
    }
}
