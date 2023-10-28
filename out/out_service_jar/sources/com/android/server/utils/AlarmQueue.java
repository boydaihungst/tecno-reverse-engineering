package com.android.server.utils;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.Pair;
import com.android.server.utils.AlarmQueue;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public abstract class AlarmQueue<K> implements AlarmManager.OnAlarmListener {
    private static final boolean DEBUG = false;
    private static final long NOT_SCHEDULED = -1;
    private static final String TAG = AlarmQueue.class.getSimpleName();
    private final AlarmPriorityQueue<K> mAlarmPriorityQueue;
    private final String mAlarmTag;
    private final Context mContext;
    private final String mDumpTitle;
    private final boolean mExactAlarm;
    private final Handler mHandler;
    private final Injector mInjector;
    private final Object mLock;
    private long mMinTimeBetweenAlarmsMs;
    private final Runnable mScheduleAlarmRunnable;
    private long mTriggerTimeElapsed;

    protected abstract boolean isForUser(K k, int i);

    protected abstract void processExpiredAlarms(ArraySet<K> arraySet);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class AlarmPriorityQueue<Q> extends PriorityQueue<Pair<Q, Long>> {
        AlarmPriorityQueue() {
            super(1, new Comparator() { // from class: com.android.server.utils.AlarmQueue$AlarmPriorityQueue$$ExternalSyntheticLambda0
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return AlarmQueue.AlarmPriorityQueue.lambda$new$0((Pair) obj, (Pair) obj2);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ int lambda$new$0(Pair o1, Pair o2) {
            return (int) (((Long) o1.second).longValue() - ((Long) o2.second).longValue());
        }

        public boolean removeKey(Q key) {
            boolean removed = false;
            Pair[] alarms = (Pair[]) toArray(new Pair[size()]);
            for (int i = alarms.length - 1; i >= 0; i--) {
                if (key.equals(alarms[i].first)) {
                    remove(alarms[i]);
                    removed = true;
                }
            }
            return removed;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Injector {
        Injector() {
        }

        long getElapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }
    }

    public AlarmQueue(Context context, Looper looper, String alarmTag, String dumpTitle, boolean exactAlarm, long minTimeBetweenAlarmsMs) {
        this(context, looper, alarmTag, dumpTitle, exactAlarm, minTimeBetweenAlarmsMs, new Injector());
    }

    AlarmQueue(Context context, Looper looper, String alarmTag, String dumpTitle, boolean exactAlarm, long minTimeBetweenAlarmsMs, Injector injector) {
        this.mScheduleAlarmRunnable = new Runnable() { // from class: com.android.server.utils.AlarmQueue.1
            @Override // java.lang.Runnable
            public void run() {
                AlarmQueue.this.mHandler.removeCallbacks(this);
                AlarmManager alarmManager = (AlarmManager) AlarmQueue.this.mContext.getSystemService(AlarmManager.class);
                if (alarmManager == null) {
                    AlarmQueue.this.mHandler.postDelayed(this, 30000L);
                    return;
                }
                synchronized (AlarmQueue.this.mLock) {
                    if (AlarmQueue.this.mTriggerTimeElapsed == -1) {
                        return;
                    }
                    long nextTriggerTimeElapsed = AlarmQueue.this.mTriggerTimeElapsed;
                    long minTimeBetweenAlarmsMs2 = AlarmQueue.this.mMinTimeBetweenAlarmsMs;
                    if (AlarmQueue.this.mExactAlarm) {
                        String str = AlarmQueue.this.mAlarmTag;
                        AlarmQueue alarmQueue = AlarmQueue.this;
                        alarmManager.setExact(3, nextTriggerTimeElapsed, str, alarmQueue, alarmQueue.mHandler);
                        return;
                    }
                    String str2 = AlarmQueue.this.mAlarmTag;
                    AlarmQueue alarmQueue2 = AlarmQueue.this;
                    alarmManager.setWindow(3, nextTriggerTimeElapsed, minTimeBetweenAlarmsMs2 / 2, str2, alarmQueue2, alarmQueue2.mHandler);
                }
            }
        };
        this.mLock = new Object();
        this.mAlarmPriorityQueue = new AlarmPriorityQueue<>();
        this.mTriggerTimeElapsed = -1L;
        this.mContext = context;
        this.mAlarmTag = alarmTag;
        this.mDumpTitle = dumpTitle.trim();
        this.mExactAlarm = exactAlarm;
        this.mHandler = new Handler(looper);
        this.mInjector = injector;
        if (minTimeBetweenAlarmsMs < 0) {
            throw new IllegalArgumentException("min time between alarms must be non-negative");
        }
        this.mMinTimeBetweenAlarmsMs = minTimeBetweenAlarmsMs;
    }

    public void addAlarm(K key, long alarmTimeElapsed) {
        synchronized (this.mLock) {
            boolean removed = this.mAlarmPriorityQueue.removeKey(key);
            this.mAlarmPriorityQueue.offer(new Pair(key, Long.valueOf(alarmTimeElapsed)));
            long j = this.mTriggerTimeElapsed;
            if (j == -1 || removed || alarmTimeElapsed < j) {
                setNextAlarmLocked();
            }
        }
    }

    public long getMinTimeBetweenAlarmsMs() {
        long j;
        synchronized (this.mLock) {
            j = this.mMinTimeBetweenAlarmsMs;
        }
        return j;
    }

    public void removeAlarmForKey(K key) {
        synchronized (this.mLock) {
            if (this.mAlarmPriorityQueue.removeKey(key)) {
                setNextAlarmLocked();
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r7v0, resolved type: com.android.server.utils.AlarmQueue<K> */
    /* JADX WARN: Multi-variable type inference failed */
    public void removeAlarmsForUserId(int userId) {
        boolean removed = false;
        synchronized (this.mLock) {
            AlarmPriorityQueue<K> alarmPriorityQueue = this.mAlarmPriorityQueue;
            Pair[] alarms = (Pair[]) alarmPriorityQueue.toArray(new Pair[alarmPriorityQueue.size()]);
            for (int i = alarms.length - 1; i >= 0; i--) {
                if (isForUser(alarms[i].first, userId)) {
                    this.mAlarmPriorityQueue.remove(alarms[i]);
                    removed = true;
                }
            }
            if (removed) {
                setNextAlarmLocked();
            }
        }
    }

    public void removeAllAlarms() {
        synchronized (this.mLock) {
            this.mAlarmPriorityQueue.clear();
            setNextAlarmLocked(0L);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: java.util.function.Predicate<K> */
    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Multi-variable type inference failed */
    public void removeAlarmsIf(Predicate<K> predicate) {
        boolean removed = false;
        synchronized (this.mLock) {
            AlarmPriorityQueue<K> alarmPriorityQueue = this.mAlarmPriorityQueue;
            Pair[] alarms = (Pair[]) alarmPriorityQueue.toArray(new Pair[alarmPriorityQueue.size()]);
            for (int i = alarms.length - 1; i >= 0; i--) {
                if (predicate.test(alarms[i].first)) {
                    this.mAlarmPriorityQueue.remove(alarms[i]);
                    removed = true;
                }
            }
            if (removed) {
                setNextAlarmLocked();
            }
        }
    }

    public void setMinTimeBetweenAlarmsMs(long minTimeMs) {
        if (minTimeMs < 0) {
            throw new IllegalArgumentException("min time between alarms must be non-negative");
        }
        synchronized (this.mLock) {
            this.mMinTimeBetweenAlarmsMs = minTimeMs;
        }
    }

    private void setNextAlarmLocked() {
        setNextAlarmLocked(this.mInjector.getElapsedRealtime());
    }

    private void setNextAlarmLocked(long earliestTriggerElapsed) {
        if (this.mAlarmPriorityQueue.size() == 0) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.utils.AlarmQueue$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    AlarmQueue.this.m7418lambda$setNextAlarmLocked$0$comandroidserverutilsAlarmQueue();
                }
            });
            this.mTriggerTimeElapsed = -1L;
            return;
        }
        Pair alarm = this.mAlarmPriorityQueue.peek();
        long nextTriggerTimeElapsed = Math.max(earliestTriggerElapsed, ((Long) alarm.second).longValue());
        long j = this.mTriggerTimeElapsed;
        if (j == -1 || nextTriggerTimeElapsed < j - 60000 || j < nextTriggerTimeElapsed) {
            this.mTriggerTimeElapsed = nextTriggerTimeElapsed;
            this.mHandler.post(this.mScheduleAlarmRunnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setNextAlarmLocked$0$com-android-server-utils-AlarmQueue  reason: not valid java name */
    public /* synthetic */ void m7418lambda$setNextAlarmLocked$0$comandroidserverutilsAlarmQueue() {
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(this);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r7v0, resolved type: com.android.server.utils.AlarmQueue<K> */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // android.app.AlarmManager.OnAlarmListener
    public void onAlarm() {
        ArraySet arraySet = new ArraySet();
        synchronized (this.mLock) {
            long nowElapsed = this.mInjector.getElapsedRealtime();
            while (this.mAlarmPriorityQueue.size() > 0) {
                Pair alarm = this.mAlarmPriorityQueue.peek();
                if (((Long) alarm.second).longValue() > nowElapsed) {
                    break;
                }
                arraySet.add(alarm.first);
                this.mAlarmPriorityQueue.remove(alarm);
            }
            setNextAlarmLocked(this.mMinTimeBetweenAlarmsMs + nowElapsed);
        }
        if (arraySet.size() > 0) {
            processExpiredAlarms(arraySet);
        }
    }

    public void dump(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            pw.print(this.mDumpTitle);
            pw.println(" alarms:");
            pw.increaseIndent();
            if (this.mAlarmPriorityQueue.size() == 0) {
                pw.println("NOT WAITING");
            } else {
                AlarmPriorityQueue<K> alarmPriorityQueue = this.mAlarmPriorityQueue;
                Pair[] alarms = (Pair[]) alarmPriorityQueue.toArray(new Pair[alarmPriorityQueue.size()]);
                for (int i = 0; i < alarms.length; i++) {
                    pw.print(alarms[i].first);
                    pw.print(": ");
                    pw.print(alarms[i].second);
                    pw.println();
                }
            }
            pw.decreaseIndent();
        }
    }
}
