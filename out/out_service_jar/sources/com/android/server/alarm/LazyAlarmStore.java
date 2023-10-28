package com.android.server.alarm;

import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.jobs.StatLogger;
import com.android.server.alarm.AlarmStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class LazyAlarmStore implements AlarmStore {
    private static final long ALARM_DEADLINE_SLOP = 500;
    static final String TAG = LazyAlarmStore.class.getSimpleName();
    private static final Comparator<Alarm> sDecreasingTimeOrder = Comparator.comparingLong(new BatchingAlarmStore$$ExternalSyntheticLambda1()).reversed();
    private Runnable mOnAlarmClockRemoved;
    private final ArrayList<Alarm> mAlarms = new ArrayList<>();
    final StatLogger mStatLogger = new StatLogger(TAG + " stats", new String[]{"GET_NEXT_DELIVERY_TIME", "GET_NEXT_WAKEUP_DELIVERY_TIME", "GET_COUNT"});

    /* loaded from: classes.dex */
    interface Stats {
        public static final int GET_COUNT = 2;
        public static final int GET_NEXT_DELIVERY_TIME = 0;
        public static final int GET_NEXT_WAKEUP_DELIVERY_TIME = 1;
    }

    @Override // com.android.server.alarm.AlarmStore
    public void add(Alarm a) {
        int index = Collections.binarySearch(this.mAlarms, a, sDecreasingTimeOrder);
        if (index < 0) {
            index = (0 - index) - 1;
        }
        this.mAlarms.add(index, a);
    }

    @Override // com.android.server.alarm.AlarmStore
    public void addAll(ArrayList<Alarm> alarms) {
        if (alarms == null) {
            return;
        }
        this.mAlarms.addAll(alarms);
        Collections.sort(this.mAlarms, sDecreasingTimeOrder);
    }

    @Override // com.android.server.alarm.AlarmStore
    public ArrayList<Alarm> remove(Predicate<Alarm> whichAlarms) {
        Runnable runnable;
        ArrayList<Alarm> removedAlarms = new ArrayList<>();
        for (int i = this.mAlarms.size() - 1; i >= 0; i--) {
            if (whichAlarms.test(this.mAlarms.get(i))) {
                Alarm removed = this.mAlarms.remove(i);
                if (removed.alarmClock != null && (runnable = this.mOnAlarmClockRemoved) != null) {
                    runnable.run();
                }
                if (AlarmManagerService.isTimeTickAlarm(removed)) {
                    Slog.wtf(TAG, "Removed TIME_TICK alarm");
                }
                removedAlarms.add(removed);
            }
        }
        return removedAlarms;
    }

    @Override // com.android.server.alarm.AlarmStore
    public void setAlarmClockRemovalListener(Runnable listener) {
        this.mOnAlarmClockRemoved = listener;
    }

    @Override // com.android.server.alarm.AlarmStore
    public Alarm getNextWakeFromIdleAlarm() {
        for (int i = this.mAlarms.size() - 1; i >= 0; i--) {
            Alarm alarm = this.mAlarms.get(i);
            if ((alarm.flags & 2) != 0) {
                return alarm;
            }
        }
        return null;
    }

    @Override // com.android.server.alarm.AlarmStore
    public int size() {
        return this.mAlarms.size();
    }

    @Override // com.android.server.alarm.AlarmStore
    public long getNextWakeupDeliveryTime() {
        long start = this.mStatLogger.getTime();
        long nextWakeup = 0;
        for (int i = this.mAlarms.size() - 1; i >= 0; i--) {
            Alarm a = this.mAlarms.get(i);
            if (a.wakeup) {
                if (nextWakeup == 0) {
                    nextWakeup = a.getMaxWhenElapsed();
                } else if (a.getWhenElapsed() > nextWakeup) {
                    break;
                } else {
                    nextWakeup = Math.min(nextWakeup, a.getMaxWhenElapsed());
                }
            }
        }
        this.mStatLogger.logDurationStat(1, start);
        return nextWakeup;
    }

    @Override // com.android.server.alarm.AlarmStore
    public long getNextDeliveryTime() {
        long start = this.mStatLogger.getTime();
        int n = this.mAlarms.size();
        if (n == 0) {
            return 0L;
        }
        long nextDelivery = this.mAlarms.get(n - 1).getMaxWhenElapsed();
        for (int i = n - 2; i >= 0; i--) {
            Alarm a = this.mAlarms.get(i);
            if (a.getWhenElapsed() > nextDelivery) {
                break;
            }
            nextDelivery = Math.min(nextDelivery, a.getMaxWhenElapsed());
        }
        this.mStatLogger.logDurationStat(0, start);
        return nextDelivery;
    }

    @Override // com.android.server.alarm.AlarmStore
    public ArrayList<Alarm> removePendingAlarms(long nowElapsed) {
        ArrayList<Alarm> pending = new ArrayList<>();
        boolean sendWakeups = false;
        boolean standalonesOnly = false;
        for (int i = this.mAlarms.size() - 1; i >= 0; i--) {
            Alarm alarm = this.mAlarms.get(i);
            if (alarm.getWhenElapsed() > nowElapsed) {
                break;
            }
            this.mAlarms.remove(i);
            pending.add(alarm);
            if (alarm.wakeup && alarm.getMaxWhenElapsed() <= 500 + nowElapsed) {
                sendWakeups = true;
            }
            if ((alarm.flags & 1) != 0) {
                standalonesOnly = true;
            }
        }
        ArrayList<Alarm> toSend = new ArrayList<>();
        for (int i2 = pending.size() - 1; i2 >= 0; i2--) {
            Alarm pendingAlarm = pending.get(i2);
            if ((sendWakeups || !pendingAlarm.wakeup) && (!standalonesOnly || (pendingAlarm.flags & 1) != 0)) {
                pending.remove(i2);
                toSend.add(pendingAlarm);
            }
        }
        addAll(pending);
        return toSend;
    }

    @Override // com.android.server.alarm.AlarmStore
    public boolean updateAlarmDeliveries(AlarmStore.AlarmDeliveryCalculator deliveryCalculator) {
        boolean changed = false;
        Iterator<Alarm> it = this.mAlarms.iterator();
        while (it.hasNext()) {
            Alarm alarm = it.next();
            changed |= deliveryCalculator.updateAlarmDelivery(alarm);
        }
        if (changed) {
            Collections.sort(this.mAlarms, sDecreasingTimeOrder);
        }
        return changed;
    }

    @Override // com.android.server.alarm.AlarmStore
    public ArrayList<Alarm> asList() {
        ArrayList<Alarm> copy = new ArrayList<>(this.mAlarms);
        Collections.reverse(copy);
        return copy;
    }

    @Override // com.android.server.alarm.AlarmStore
    public void dump(IndentingPrintWriter ipw, long nowElapsed, SimpleDateFormat sdf) {
        ipw.println(this.mAlarms.size() + " pending alarms: ");
        ipw.increaseIndent();
        AlarmManagerService.dumpAlarmList(ipw, this.mAlarms, nowElapsed, sdf);
        ipw.decreaseIndent();
        this.mStatLogger.dump(ipw);
    }

    @Override // com.android.server.alarm.AlarmStore
    public void dumpProto(ProtoOutputStream pos, long nowElapsed) {
        Iterator<Alarm> it = this.mAlarms.iterator();
        while (it.hasNext()) {
            Alarm a = it.next();
            a.dumpDebug(pos, 2246267895850L, nowElapsed);
        }
    }

    @Override // com.android.server.alarm.AlarmStore
    public String getName() {
        return TAG;
    }

    @Override // com.android.server.alarm.AlarmStore
    public int getCount(Predicate<Alarm> condition) {
        long start = this.mStatLogger.getTime();
        int count = 0;
        Iterator<Alarm> it = this.mAlarms.iterator();
        while (it.hasNext()) {
            Alarm a = it.next();
            if (condition.test(a)) {
                count++;
            }
        }
        this.mStatLogger.logDurationStat(2, start);
        return count;
    }
}
