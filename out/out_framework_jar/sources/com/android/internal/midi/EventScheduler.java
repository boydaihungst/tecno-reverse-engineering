package com.android.internal.midi;

import java.util.SortedMap;
import java.util.TreeMap;
/* loaded from: classes4.dex */
public class EventScheduler {
    private static final long NANOS_PER_MILLI = 1000000;
    private boolean mClosed;
    private final Object mLock = new Object();
    private FastEventQueue mEventPool = null;
    private int mMaxPoolSize = 200;
    private volatile SortedMap<Long, FastEventQueue> mEventBuffer = new TreeMap();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes4.dex */
    public class FastEventQueue {
        volatile long mEventsAdded = 1;
        volatile long mEventsRemoved = 0;
        volatile SchedulableEvent mFirst;
        volatile SchedulableEvent mLast;

        FastEventQueue(SchedulableEvent event) {
            this.mFirst = event;
            this.mLast = this.mFirst;
        }

        int size() {
            return (int) (this.mEventsAdded - this.mEventsRemoved);
        }

        public SchedulableEvent remove() {
            this.mEventsRemoved++;
            SchedulableEvent event = this.mFirst;
            this.mFirst = event.mNext;
            event.mNext = null;
            return event;
        }

        public void add(SchedulableEvent event) {
            event.mNext = null;
            this.mLast.mNext = event;
            this.mLast = event;
            this.mEventsAdded++;
        }
    }

    /* loaded from: classes4.dex */
    public static class SchedulableEvent {
        private volatile SchedulableEvent mNext = null;
        private long mTimestamp;

        public SchedulableEvent(long timestamp) {
            this.mTimestamp = timestamp;
        }

        public long getTimestamp() {
            return this.mTimestamp;
        }

        public void setTimestamp(long timestamp) {
            this.mTimestamp = timestamp;
        }
    }

    public SchedulableEvent removeEventfromPool() {
        FastEventQueue fastEventQueue = this.mEventPool;
        if (fastEventQueue == null || fastEventQueue.size() <= 1) {
            return null;
        }
        SchedulableEvent event = this.mEventPool.remove();
        return event;
    }

    public void addEventToPool(SchedulableEvent event) {
        FastEventQueue fastEventQueue = this.mEventPool;
        if (fastEventQueue == null) {
            this.mEventPool = new FastEventQueue(event);
        } else if (fastEventQueue.size() < this.mMaxPoolSize) {
            this.mEventPool.add(event);
        }
    }

    public void add(SchedulableEvent event) {
        synchronized (this.mLock) {
            FastEventQueue list = this.mEventBuffer.get(Long.valueOf(event.getTimestamp()));
            if (list == null) {
                long lowestTime = this.mEventBuffer.isEmpty() ? Long.MAX_VALUE : this.mEventBuffer.firstKey().longValue();
                this.mEventBuffer.put(Long.valueOf(event.getTimestamp()), new FastEventQueue(event));
                if (event.getTimestamp() < lowestTime) {
                    this.mLock.notify();
                }
            } else {
                list.add(event);
            }
        }
    }

    private SchedulableEvent removeNextEventLocked(long lowestTime) {
        FastEventQueue list = this.mEventBuffer.get(Long.valueOf(lowestTime));
        if (list.size() == 1) {
            this.mEventBuffer.remove(Long.valueOf(lowestTime));
        }
        SchedulableEvent event = list.remove();
        return event;
    }

    public SchedulableEvent getNextEvent(long time) {
        SchedulableEvent event = null;
        synchronized (this.mLock) {
            if (!this.mEventBuffer.isEmpty()) {
                long lowestTime = this.mEventBuffer.firstKey().longValue();
                if (lowestTime <= time) {
                    event = removeNextEventLocked(lowestTime);
                }
            }
        }
        return event;
    }

    public SchedulableEvent waitNextEvent() throws InterruptedException {
        SchedulableEvent event = null;
        synchronized (this.mLock) {
            while (true) {
                if (this.mClosed) {
                    break;
                }
                long millisToWait = 2147483647L;
                if (!this.mEventBuffer.isEmpty()) {
                    long now = System.nanoTime();
                    long lowestTime = this.mEventBuffer.firstKey().longValue();
                    if (lowestTime <= now) {
                        event = removeNextEventLocked(lowestTime);
                        break;
                    }
                    long nanosToWait = lowestTime - now;
                    millisToWait = (nanosToWait / 1000000) + 1;
                    if (millisToWait > 2147483647L) {
                        millisToWait = 2147483647L;
                    }
                }
                this.mLock.wait((int) millisToWait);
            }
        }
        return event;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void flush() {
        this.mEventBuffer = new TreeMap();
    }

    public void close() {
        synchronized (this.mLock) {
            this.mClosed = true;
            this.mLock.notify();
        }
    }
}
