package com.android.server.am;

import com.android.server.am.BaseAppStateEvents;
import com.android.server.am.BaseAppStateTimeEvents.BaseTimeEvent;
import com.android.server.job.controllers.JobStatus;
import java.util.Iterator;
import java.util.LinkedList;
/* loaded from: classes.dex */
class BaseAppStateTimeEvents<T extends BaseTimeEvent> extends BaseAppStateEvents<T> {
    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateTimeEvents(int uid, String packageName, int numOfEventTypes, String tag, BaseAppStateEvents.MaxTrackingDurationConfig maxTrackingDurationConfig) {
        super(uid, packageName, numOfEventTypes, tag, maxTrackingDurationConfig);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateTimeEvents(BaseAppStateTimeEvents other) {
        super(other);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: java.util.LinkedList<T extends com.android.server.am.BaseAppStateTimeEvents$BaseTimeEvent> */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.am.BaseAppStateEvents
    LinkedList<T> add(LinkedList<T> durations, LinkedList<T> otherDurations) {
        long j;
        if (otherDurations == null || otherDurations.size() == 0) {
            return durations;
        }
        if (durations == null || durations.size() == 0) {
            return (LinkedList) otherDurations.clone();
        }
        Iterator<T> itl = durations.iterator();
        Iterator<T> itr = otherDurations.iterator();
        T l = itl.next();
        T r = itr.next();
        LinkedList<T> dest = (LinkedList<T>) new LinkedList();
        long lts = l.getTimestamp();
        long rts = r.getTimestamp();
        while (true) {
            long j2 = JobStatus.NO_LATEST_RUNTIME;
            if (lts != JobStatus.NO_LATEST_RUNTIME || rts != JobStatus.NO_LATEST_RUNTIME) {
                if (lts == rts) {
                    dest.add((BaseTimeEvent) l.clone());
                    if (itl.hasNext()) {
                        T next = itl.next();
                        l = next;
                        j = next.getTimestamp();
                    } else {
                        j = Long.MAX_VALUE;
                    }
                    lts = j;
                    if (itr.hasNext()) {
                        T next2 = itr.next();
                        r = next2;
                        j2 = next2.getTimestamp();
                    }
                    rts = j2;
                } else if (lts < rts) {
                    dest.add((BaseTimeEvent) l.clone());
                    if (itl.hasNext()) {
                        T next3 = itl.next();
                        l = next3;
                        j2 = next3.getTimestamp();
                    }
                    lts = j2;
                } else {
                    dest.add((BaseTimeEvent) r.clone());
                    if (itr.hasNext()) {
                        T next4 = itr.next();
                        r = next4;
                        j2 = next4.getTimestamp();
                    }
                    rts = j2;
                }
            } else {
                return dest;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateEvents
    public int getTotalEventsSince(long since, long now, int index) {
        LinkedList linkedList = this.mEvents[index];
        if (linkedList == null || linkedList.size() == 0) {
            return 0;
        }
        int count = 0;
        Iterator it = linkedList.iterator();
        while (it.hasNext()) {
            if (((BaseTimeEvent) it.next()).getTimestamp() >= since) {
                count++;
            }
        }
        return count;
    }

    @Override // com.android.server.am.BaseAppStateEvents
    void trimEvents(long earliest, int index) {
        LinkedList linkedList = this.mEvents[index];
        if (linkedList == null) {
            return;
        }
        while (linkedList.size() > 0 && ((BaseTimeEvent) linkedList.peek()).getTimestamp() < earliest) {
            linkedList.pop();
        }
    }

    /* loaded from: classes.dex */
    static class BaseTimeEvent implements Cloneable {
        long mTimestamp;

        /* JADX INFO: Access modifiers changed from: package-private */
        public BaseTimeEvent(long timestamp) {
            this.mTimestamp = timestamp;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public BaseTimeEvent(BaseTimeEvent other) {
            this.mTimestamp = other.mTimestamp;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void trimTo(long timestamp) {
            this.mTimestamp = timestamp;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public long getTimestamp() {
            return this.mTimestamp;
        }

        public Object clone() {
            return new BaseTimeEvent(this);
        }

        public boolean equals(Object other) {
            return other != null && other.getClass() == BaseTimeEvent.class && ((BaseTimeEvent) other).mTimestamp == this.mTimestamp;
        }

        public int hashCode() {
            return Long.hashCode(this.mTimestamp);
        }
    }
}
