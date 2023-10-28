package com.android.server.location.eventlog;

import com.android.internal.util.Preconditions;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
/* loaded from: classes.dex */
public class LocalEventLog<T> {
    private static final int IS_FILLER_MASK = Integer.MIN_VALUE;
    private static final int TIME_DELTA_MASK = Integer.MAX_VALUE;
    final int[] mEntries;
    long mLastLogTime;
    int mLogEndIndex;
    final T[] mLogEvents;
    int mLogSize;
    long mModificationCount;
    long mStartTime;
    private static final int IS_FILLER_OFFSET = countTrailingZeros(Integer.MIN_VALUE);
    private static final int TIME_DELTA_OFFSET = countTrailingZeros(Integer.MAX_VALUE);
    static final int MAX_TIME_DELTA = (1 << Integer.bitCount(Integer.MAX_VALUE)) - 1;

    /* loaded from: classes.dex */
    public interface LogConsumer<T> {
        void acceptLog(long j, T t);
    }

    private static int countTrailingZeros(int i) {
        int c = 0;
        while (i != 0 && (i & 1) == 0) {
            c++;
            i >>>= 1;
        }
        return c;
    }

    private static int createEntry(boolean isFiller, int timeDelta) {
        Preconditions.checkArgument(timeDelta >= 0 && timeDelta <= MAX_TIME_DELTA);
        return (((isFiller ? 1 : 0) << IS_FILLER_OFFSET) & Integer.MIN_VALUE) | ((timeDelta << TIME_DELTA_OFFSET) & Integer.MAX_VALUE);
    }

    static int getTimeDelta(int entry) {
        return (Integer.MAX_VALUE & entry) >>> TIME_DELTA_OFFSET;
    }

    static boolean isFiller(int entry) {
        return (Integer.MIN_VALUE & entry) != 0;
    }

    public LocalEventLog(int size, Class<T> clazz) {
        Preconditions.checkArgument(size > 0);
        this.mEntries = new int[size];
        this.mLogEvents = (T[]) ((Object[]) Array.newInstance((Class<?>) clazz, size));
        this.mLogSize = 0;
        this.mLogEndIndex = 0;
        this.mStartTime = -1L;
        this.mLastLogTime = -1L;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public synchronized void addLog(long time, T logEvent) {
        Preconditions.checkArgument(logEvent != null);
        long delta = 0;
        if (!isEmpty()) {
            delta = time - this.mLastLogTime;
            if (delta >= 0 && delta / MAX_TIME_DELTA < this.mEntries.length - 1) {
                while (true) {
                    int i = MAX_TIME_DELTA;
                    if (delta < i) {
                        break;
                    }
                    addLogEventInternal(true, i, null);
                    delta -= i;
                }
            }
            clear();
            delta = 0;
        }
        if (isEmpty()) {
            this.mStartTime = time;
            this.mLastLogTime = time;
            this.mModificationCount++;
        }
        addLogEventInternal(false, (int) delta, logEvent);
    }

    private void addLogEventInternal(boolean isFiller, int timeDelta, T logEvent) {
        int[] iArr;
        boolean z = false;
        Preconditions.checkArgument(isFiller || logEvent != null);
        if (this.mStartTime != -1 && this.mLastLogTime != -1) {
            z = true;
        }
        Preconditions.checkState(z);
        int i = this.mLogSize;
        if (i == this.mEntries.length) {
            this.mStartTime += getTimeDelta(iArr[startIndex()]);
            this.mModificationCount++;
        } else {
            this.mLogSize = i + 1;
        }
        this.mEntries[this.mLogEndIndex] = createEntry(isFiller, timeDelta);
        T[] tArr = this.mLogEvents;
        int i2 = this.mLogEndIndex;
        tArr[i2] = logEvent;
        this.mLogEndIndex = incrementIndex(i2);
        this.mLastLogTime += timeDelta;
    }

    public synchronized void clear() {
        Arrays.fill(this.mLogEvents, (Object) null);
        this.mLogEndIndex = 0;
        this.mLogSize = 0;
        this.mModificationCount++;
        this.mStartTime = -1L;
        this.mLastLogTime = -1L;
    }

    private boolean isEmpty() {
        return this.mLogSize == 0;
    }

    public synchronized void iterate(LogConsumer<? super T> consumer) {
        LocalEventLog<T>.LogIterator it = new LogIterator();
        while (it.hasNext()) {
            it.next();
            consumer.acceptLog(it.getTime(), (Object) it.getLog());
        }
    }

    @SafeVarargs
    public static <T> void iterate(LogConsumer<? super T> consumer, LocalEventLog<T>... logs) {
        ArrayList<LocalEventLog<T>.LogIterator> its = new ArrayList<>(logs.length);
        for (LocalEventLog<T> log : logs) {
            Objects.requireNonNull(log);
            LocalEventLog<T>.LogIterator it = new LogIterator();
            if (it.hasNext()) {
                its.add(it);
                it.next();
            }
        }
        while (true) {
            LocalEventLog<T>.LogIterator next = null;
            Iterator<LocalEventLog<T>.LogIterator> it2 = its.iterator();
            while (it2.hasNext()) {
                LocalEventLog<T>.LogIterator it3 = it2.next();
                if (it3 != null && (next == null || it3.getTime() < next.getTime())) {
                    next = it3;
                }
            }
            if (next == null) {
                return;
            }
            consumer.acceptLog(next.getTime(), (Object) next.getLog());
            if (next.hasNext()) {
                next.next();
            } else {
                its.remove(next);
            }
        }
    }

    int startIndex() {
        return wrapIndex(this.mLogEndIndex - this.mLogSize);
    }

    int incrementIndex(int index) {
        if (index == -1) {
            return startIndex();
        }
        if (index >= 0) {
            return wrapIndex(index + 1);
        }
        throw new IllegalArgumentException();
    }

    int wrapIndex(int index) {
        int[] iArr = this.mEntries;
        return ((index % iArr.length) + iArr.length) % iArr.length;
    }

    /* loaded from: classes.dex */
    protected final class LogIterator {
        private int mCount;
        private T mCurrentLogEvent;
        private long mCurrentTime;
        private int mIndex;
        private long mLogTime;
        private final long mModificationCount;

        public LogIterator() {
            synchronized (LocalEventLog.this) {
                this.mModificationCount = LocalEventLog.this.mModificationCount;
                this.mLogTime = LocalEventLog.this.mStartTime;
                this.mIndex = -1;
                this.mCount = -1;
                increment();
            }
        }

        public boolean hasNext() {
            boolean z;
            synchronized (LocalEventLog.this) {
                checkModifications();
                z = this.mCount < LocalEventLog.this.mLogSize;
            }
            return z;
        }

        public void next() {
            synchronized (LocalEventLog.this) {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                this.mCurrentTime = this.mLogTime + LocalEventLog.getTimeDelta(LocalEventLog.this.mEntries[this.mIndex]);
                this.mCurrentLogEvent = (T) Objects.requireNonNull(LocalEventLog.this.mLogEvents[this.mIndex]);
                increment();
            }
        }

        public long getTime() {
            return this.mCurrentTime;
        }

        public T getLog() {
            return this.mCurrentLogEvent;
        }

        private void increment() {
            long nextDeltaMs = this.mIndex == -1 ? 0L : LocalEventLog.getTimeDelta(LocalEventLog.this.mEntries[this.mIndex]);
            do {
                this.mLogTime += nextDeltaMs;
                this.mIndex = LocalEventLog.this.incrementIndex(this.mIndex);
                int i = this.mCount + 1;
                this.mCount = i;
                if (i < LocalEventLog.this.mLogSize) {
                    nextDeltaMs = LocalEventLog.getTimeDelta(LocalEventLog.this.mEntries[this.mIndex]);
                }
                if (this.mCount >= LocalEventLog.this.mLogSize) {
                    return;
                }
            } while (LocalEventLog.isFiller(LocalEventLog.this.mEntries[this.mIndex]));
        }

        private void checkModifications() {
            if (this.mModificationCount != LocalEventLog.this.mModificationCount) {
                throw new ConcurrentModificationException();
            }
        }
    }
}
