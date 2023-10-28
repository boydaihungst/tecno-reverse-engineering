package com.android.server.alarm;

import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.jobs.StatLogger;
import com.android.server.alarm.AlarmStore;
import com.android.server.alarm.BatchingAlarmStore;
import com.android.server.job.controllers.JobStatus;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
/* loaded from: classes.dex */
public class BatchingAlarmStore implements AlarmStore {
    static final String TAG = BatchingAlarmStore.class.getSimpleName();
    private static final Comparator<Batch> sBatchOrder = Comparator.comparingLong(new ToLongFunction() { // from class: com.android.server.alarm.BatchingAlarmStore$$ExternalSyntheticLambda0
        @Override // java.util.function.ToLongFunction
        public final long applyAsLong(Object obj) {
            long j;
            j = ((BatchingAlarmStore.Batch) obj).mStart;
            return j;
        }
    });
    private static final Comparator<Alarm> sIncreasingTimeOrder = Comparator.comparingLong(new BatchingAlarmStore$$ExternalSyntheticLambda1());
    private Runnable mOnAlarmClockRemoved;
    private int mSize;
    private final ArrayList<Batch> mAlarmBatches = new ArrayList<>();
    final StatLogger mStatLogger = new StatLogger(TAG + " stats", new String[]{"REBATCH_ALL_ALARMS", "GET_COUNT"});

    /* loaded from: classes.dex */
    interface Stats {
        public static final int GET_COUNT = 1;
        public static final int REBATCH_ALL_ALARMS = 0;
    }

    @Override // com.android.server.alarm.AlarmStore
    public void add(Alarm a) {
        insertAndBatchAlarm(a);
        this.mSize++;
    }

    @Override // com.android.server.alarm.AlarmStore
    public void addAll(ArrayList<Alarm> alarms) {
        if (alarms == null) {
            return;
        }
        Iterator<Alarm> it = alarms.iterator();
        while (it.hasNext()) {
            Alarm a = it.next();
            add(a);
        }
    }

    @Override // com.android.server.alarm.AlarmStore
    public ArrayList<Alarm> remove(Predicate<Alarm> whichAlarms) {
        ArrayList<Alarm> removed = new ArrayList<>();
        for (int i = this.mAlarmBatches.size() - 1; i >= 0; i--) {
            Batch b = this.mAlarmBatches.get(i);
            removed.addAll(b.remove(whichAlarms));
            if (b.size() == 0) {
                this.mAlarmBatches.remove(i);
            }
        }
        if (!removed.isEmpty()) {
            this.mSize -= removed.size();
            rebatchAllAlarms();
        }
        return removed;
    }

    @Override // com.android.server.alarm.AlarmStore
    public void setAlarmClockRemovalListener(Runnable listener) {
        this.mOnAlarmClockRemoved = listener;
    }

    @Override // com.android.server.alarm.AlarmStore
    public Alarm getNextWakeFromIdleAlarm() {
        Iterator<Batch> it = this.mAlarmBatches.iterator();
        while (it.hasNext()) {
            Batch batch = it.next();
            if ((batch.mFlags & 2) != 0) {
                for (int i = 0; i < batch.size(); i++) {
                    Alarm a = batch.get(i);
                    if ((a.flags & 2) != 0) {
                        return a;
                    }
                }
                continue;
            }
        }
        return null;
    }

    private void rebatchAllAlarms() {
        long start = this.mStatLogger.getTime();
        ArrayList<Batch> oldBatches = (ArrayList) this.mAlarmBatches.clone();
        this.mAlarmBatches.clear();
        Iterator<Batch> it = oldBatches.iterator();
        while (it.hasNext()) {
            Batch batch = it.next();
            for (int i = 0; i < batch.size(); i++) {
                insertAndBatchAlarm(batch.get(i));
            }
        }
        this.mStatLogger.logDurationStat(0, start);
    }

    @Override // com.android.server.alarm.AlarmStore
    public int size() {
        return this.mSize;
    }

    @Override // com.android.server.alarm.AlarmStore
    public long getNextWakeupDeliveryTime() {
        Iterator<Batch> it = this.mAlarmBatches.iterator();
        while (it.hasNext()) {
            Batch b = it.next();
            if (b.hasWakeups()) {
                return b.mStart;
            }
        }
        return 0L;
    }

    @Override // com.android.server.alarm.AlarmStore
    public long getNextDeliveryTime() {
        if (this.mAlarmBatches.size() > 0) {
            return this.mAlarmBatches.get(0).mStart;
        }
        return 0L;
    }

    @Override // com.android.server.alarm.AlarmStore
    public ArrayList<Alarm> removePendingAlarms(long nowElapsed) {
        ArrayList<Alarm> removedAlarms = new ArrayList<>();
        while (this.mAlarmBatches.size() > 0) {
            Batch batch = this.mAlarmBatches.get(0);
            if (batch.mStart > nowElapsed) {
                break;
            }
            this.mAlarmBatches.remove(0);
            for (int i = 0; i < batch.size(); i++) {
                removedAlarms.add(batch.get(i));
            }
        }
        this.mSize -= removedAlarms.size();
        return removedAlarms;
    }

    @Override // com.android.server.alarm.AlarmStore
    public boolean updateAlarmDeliveries(AlarmStore.AlarmDeliveryCalculator deliveryCalculator) {
        boolean changed = false;
        Iterator<Batch> it = this.mAlarmBatches.iterator();
        while (it.hasNext()) {
            Batch b = it.next();
            for (int i = 0; i < b.size(); i++) {
                changed |= deliveryCalculator.updateAlarmDelivery(b.get(i));
            }
        }
        if (changed) {
            rebatchAllAlarms();
        }
        return changed;
    }

    @Override // com.android.server.alarm.AlarmStore
    public ArrayList<Alarm> asList() {
        ArrayList<Alarm> allAlarms = new ArrayList<>();
        Iterator<Batch> it = this.mAlarmBatches.iterator();
        while (it.hasNext()) {
            Batch batch = it.next();
            for (int i = 0; i < batch.size(); i++) {
                allAlarms.add(batch.get(i));
            }
        }
        return allAlarms;
    }

    @Override // com.android.server.alarm.AlarmStore
    public void dump(IndentingPrintWriter ipw, long nowElapsed, SimpleDateFormat sdf) {
        ipw.print("Pending alarm batches: ");
        ipw.println(this.mAlarmBatches.size());
        Iterator<Batch> it = this.mAlarmBatches.iterator();
        while (it.hasNext()) {
            Batch b = it.next();
            ipw.print(b);
            ipw.println(':');
            ipw.increaseIndent();
            AlarmManagerService.dumpAlarmList(ipw, b.mAlarms, nowElapsed, sdf);
            ipw.decreaseIndent();
        }
        this.mStatLogger.dump(ipw);
    }

    @Override // com.android.server.alarm.AlarmStore
    public void dumpProto(ProtoOutputStream pos, long nowElapsed) {
        Iterator<Batch> it = this.mAlarmBatches.iterator();
        while (it.hasNext()) {
            Batch b = it.next();
            b.dumpDebug(pos, 2246267895827L, nowElapsed);
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
        Iterator<Batch> it = this.mAlarmBatches.iterator();
        while (it.hasNext()) {
            Batch b = it.next();
            for (int i = 0; i < b.size(); i++) {
                if (condition.test(b.get(i))) {
                    count++;
                }
            }
        }
        this.mStatLogger.logDurationStat(1, start);
        return count;
    }

    private void insertAndBatchAlarm(Alarm alarm) {
        int whichBatch = (alarm.flags & 1) != 0 ? -1 : attemptCoalesce(alarm.getWhenElapsed(), alarm.getMaxWhenElapsed());
        if (whichBatch < 0) {
            addBatch(this.mAlarmBatches, new Batch(alarm));
            return;
        }
        Batch batch = this.mAlarmBatches.get(whichBatch);
        if (batch.add(alarm)) {
            this.mAlarmBatches.remove(whichBatch);
            addBatch(this.mAlarmBatches, batch);
        }
    }

    static void addBatch(ArrayList<Batch> list, Batch newBatch) {
        int index = Collections.binarySearch(list, newBatch, sBatchOrder);
        if (index < 0) {
            index = (0 - index) - 1;
        }
        list.add(index, newBatch);
    }

    private int attemptCoalesce(long whenElapsed, long maxWhen) {
        int n = this.mAlarmBatches.size();
        for (int i = 0; i < n; i++) {
            Batch b = this.mAlarmBatches.get(i);
            if ((b.mFlags & 1) == 0 && b.canHold(whenElapsed, maxWhen)) {
                return i;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class Batch {
        final ArrayList<Alarm> mAlarms;
        long mEnd;
        int mFlags;
        long mStart;

        Batch(Alarm seed) {
            ArrayList<Alarm> arrayList = new ArrayList<>();
            this.mAlarms = arrayList;
            this.mStart = seed.getWhenElapsed();
            this.mEnd = AlarmManagerService.clampPositive(seed.getMaxWhenElapsed());
            this.mFlags = seed.flags;
            arrayList.add(seed);
        }

        int size() {
            return this.mAlarms.size();
        }

        Alarm get(int index) {
            return this.mAlarms.get(index);
        }

        boolean canHold(long whenElapsed, long maxWhen) {
            return this.mEnd >= whenElapsed && this.mStart <= maxWhen;
        }

        boolean add(Alarm alarm) {
            boolean newStart = false;
            int index = Collections.binarySearch(this.mAlarms, alarm, BatchingAlarmStore.sIncreasingTimeOrder);
            if (index < 0) {
                index = (0 - index) - 1;
            }
            this.mAlarms.add(index, alarm);
            if (AlarmManagerService.DEBUG_BATCH) {
                Slog.v(BatchingAlarmStore.TAG, "Adding " + alarm + " to " + this);
            }
            if (alarm.getWhenElapsed() > this.mStart) {
                this.mStart = alarm.getWhenElapsed();
                newStart = true;
            }
            if (alarm.getMaxWhenElapsed() < this.mEnd) {
                this.mEnd = alarm.getMaxWhenElapsed();
            }
            this.mFlags |= alarm.flags;
            if (AlarmManagerService.DEBUG_BATCH) {
                Slog.v(BatchingAlarmStore.TAG, "    => now " + this);
            }
            return newStart;
        }

        ArrayList<Alarm> remove(Predicate<Alarm> predicate) {
            ArrayList<Alarm> removed = new ArrayList<>();
            long newStart = 0;
            long newEnd = JobStatus.NO_LATEST_RUNTIME;
            int newFlags = 0;
            int i = 0;
            while (i < this.mAlarms.size()) {
                Alarm alarm = this.mAlarms.get(i);
                if (predicate.test(alarm)) {
                    removed.add(this.mAlarms.remove(i));
                    if (alarm.alarmClock != null && BatchingAlarmStore.this.mOnAlarmClockRemoved != null) {
                        BatchingAlarmStore.this.mOnAlarmClockRemoved.run();
                    }
                    if (AlarmManagerService.isTimeTickAlarm(alarm)) {
                        Slog.wtf(BatchingAlarmStore.TAG, "Removed TIME_TICK alarm");
                    }
                } else {
                    if (alarm.getWhenElapsed() > newStart) {
                        newStart = alarm.getWhenElapsed();
                    }
                    if (alarm.getMaxWhenElapsed() < newEnd) {
                        newEnd = alarm.getMaxWhenElapsed();
                    }
                    newFlags |= alarm.flags;
                    i++;
                }
            }
            if (!removed.isEmpty()) {
                this.mStart = newStart;
                this.mEnd = newEnd;
                this.mFlags = newFlags;
            }
            return removed;
        }

        boolean hasWakeups() {
            int n = this.mAlarms.size();
            for (int i = 0; i < n; i++) {
                Alarm a = this.mAlarms.get(i);
                if (a.wakeup) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            StringBuilder b = new StringBuilder(40);
            b.append("Batch{");
            b.append(Integer.toHexString(hashCode()));
            b.append(" num=");
            b.append(size());
            b.append(" start=");
            b.append(this.mStart);
            b.append(" end=");
            b.append(this.mEnd);
            if (this.mFlags != 0) {
                b.append(" flgs=0x");
                b.append(Integer.toHexString(this.mFlags));
            }
            b.append('}');
            return b.toString();
        }

        public void dumpDebug(ProtoOutputStream proto, long fieldId, long nowElapsed) {
            long token = proto.start(fieldId);
            proto.write(1112396529665L, this.mStart);
            proto.write(1112396529666L, this.mEnd);
            proto.write(1120986464259L, this.mFlags);
            Iterator<Alarm> it = this.mAlarms.iterator();
            while (it.hasNext()) {
                Alarm a = it.next();
                a.dumpDebug(proto, 2246267895812L, nowElapsed);
            }
            proto.end(token);
        }
    }
}
