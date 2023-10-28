package com.android.server.am;

import android.os.SystemClock;
import android.os.UserHandle;
import android.util.TimeUtils;
import com.android.server.am.BaseAppStateEvents;
import com.android.server.am.BaseAppStateTimeEvents;
import com.android.server.am.BaseAppStateTimeEvents.BaseTimeEvent;
import com.android.server.job.controllers.JobStatus;
import com.android.server.slice.SliceClientPermissions;
import java.util.Iterator;
import java.util.LinkedList;
/* loaded from: classes.dex */
abstract class BaseAppStateDurations<T extends BaseAppStateTimeEvents.BaseTimeEvent> extends BaseAppStateTimeEvents<T> {
    static final boolean DEBUG_BASE_APP_STATE_DURATIONS = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateDurations(int uid, String packageName, int numOfEventTypes, String tag, BaseAppStateEvents.MaxTrackingDurationConfig maxTrackingDurationConfig) {
        super(uid, packageName, numOfEventTypes, tag, maxTrackingDurationConfig);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateDurations(BaseAppStateDurations other) {
        super(other);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v4, resolved type: java.util.LinkedList<E>[] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public void addEvent(boolean start, T event, int index) {
        if (this.mEvents[index] == null) {
            this.mEvents[index] = new LinkedList();
        }
        LinkedList linkedList = this.mEvents[index];
        linkedList.size();
        boolean active = isActive(index);
        if (start != active) {
            linkedList.add(event);
        }
        trimEvents(getEarliest(event.getTimestamp()), index);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: com.android.server.am.BaseAppStateDurations<T extends com.android.server.am.BaseAppStateTimeEvents$BaseTimeEvent> */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.am.BaseAppStateTimeEvents, com.android.server.am.BaseAppStateEvents
    void trimEvents(long earliest, int index) {
        trimEvents(earliest, (LinkedList) this.mEvents[index]);
    }

    void trimEvents(long earliest, LinkedList<T> events) {
        if (events == null) {
            return;
        }
        while (events.size() > 1) {
            T current = events.peek();
            if (current.getTimestamp() >= earliest) {
                return;
            }
            if (events.get(1).getTimestamp() > earliest) {
                events.get(0).trimTo(earliest);
                return;
            } else {
                events.pop();
                events.pop();
            }
        }
        if (events.size() == 1) {
            events.get(0).trimTo(Math.max(earliest, events.peek().getTimestamp()));
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: java.util.LinkedList<T extends com.android.server.am.BaseAppStateTimeEvents$BaseTimeEvent> */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.am.BaseAppStateTimeEvents, com.android.server.am.BaseAppStateEvents
    LinkedList<T> add(LinkedList<T> durations, LinkedList<T> otherDurations) {
        T earliest;
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
        boolean actl = false;
        boolean actr = false;
        long lts = l.getTimestamp();
        long rts = r.getTimestamp();
        while (true) {
            long j2 = JobStatus.NO_LATEST_RUNTIME;
            if (lts != JobStatus.NO_LATEST_RUNTIME || rts != JobStatus.NO_LATEST_RUNTIME) {
                boolean z = false;
                boolean actCur = actl || actr;
                if (lts == rts) {
                    earliest = l;
                    actl = !actl;
                    actr = !actr;
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
                    earliest = l;
                    actl = !actl;
                    if (itl.hasNext()) {
                        T next3 = itl.next();
                        l = next3;
                        j2 = next3.getTimestamp();
                    }
                    lts = j2;
                } else {
                    earliest = r;
                    actr = !actr;
                    if (itr.hasNext()) {
                        T next4 = itr.next();
                        r = next4;
                        j2 = next4.getTimestamp();
                    }
                    rts = j2;
                }
                if (actl || actr) {
                    z = true;
                }
                if (actCur != z) {
                    dest.add((BaseAppStateTimeEvents.BaseTimeEvent) earliest.clone());
                }
            } else {
                return dest;
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v8, resolved type: java.util.LinkedList<E>[] */
    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.am.BaseAppStateDurations<T extends com.android.server.am.BaseAppStateTimeEvents$BaseTimeEvent> */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public void subtract(BaseAppStateDurations otherDurations, int thisIndex, int otherIndex) {
        if (this.mEvents.length <= thisIndex || this.mEvents[thisIndex] == null || otherDurations.mEvents.length <= otherIndex || otherDurations.mEvents[otherIndex] == null) {
            return;
        }
        this.mEvents[thisIndex] = subtract((LinkedList) this.mEvents[thisIndex], (LinkedList) otherDurations.mEvents[otherIndex]);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v4, resolved type: java.util.LinkedList<E>[] */
    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: com.android.server.am.BaseAppStateDurations<T extends com.android.server.am.BaseAppStateTimeEvents$BaseTimeEvent> */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public void subtract(BaseAppStateDurations otherDurations, int otherIndex) {
        if (otherDurations.mEvents.length <= otherIndex || otherDurations.mEvents[otherIndex] == null) {
            return;
        }
        for (int i = 0; i < this.mEvents.length; i++) {
            if (this.mEvents[i] != null) {
                this.mEvents[i] = subtract((LinkedList) this.mEvents[i], (LinkedList) otherDurations.mEvents[otherIndex]);
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r4v0, resolved type: java.util.LinkedList<T extends com.android.server.am.BaseAppStateTimeEvents$BaseTimeEvent> */
    /* JADX WARN: Multi-variable type inference failed */
    LinkedList<T> subtract(LinkedList<T> durations, LinkedList<T> otherDurations) {
        T earliest;
        long j;
        if (otherDurations == null || otherDurations.size() == 0 || durations == null || durations.size() == 0) {
            return durations;
        }
        Iterator<T> itl = durations.iterator();
        Iterator<T> itr = otherDurations.iterator();
        T l = itl.next();
        T r = itr.next();
        LinkedList<T> dest = (LinkedList<T>) new LinkedList();
        boolean actl = false;
        boolean actr = false;
        long lts = l.getTimestamp();
        long rts = r.getTimestamp();
        while (true) {
            long j2 = JobStatus.NO_LATEST_RUNTIME;
            if (lts != JobStatus.NO_LATEST_RUNTIME || rts != JobStatus.NO_LATEST_RUNTIME) {
                boolean z = true;
                boolean actCur = actl && !actr;
                if (lts == rts) {
                    earliest = l;
                    actl = !actl;
                    actr = !actr;
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
                    earliest = l;
                    actl = !actl;
                    if (itl.hasNext()) {
                        T next3 = itl.next();
                        l = next3;
                        j2 = next3.getTimestamp();
                    }
                    lts = j2;
                } else {
                    earliest = r;
                    actr = !actr;
                    if (itr.hasNext()) {
                        T next4 = itr.next();
                        r = next4;
                        j2 = next4.getTimestamp();
                    }
                    rts = j2;
                }
                if (!actl || actr) {
                    z = false;
                }
                if (actCur != z) {
                    dest.add((BaseAppStateTimeEvents.BaseTimeEvent) earliest.clone());
                }
            } else {
                return dest;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTotalDurations(long now, int index) {
        return getTotalDurationsSince(getEarliest(0L), now, index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getTotalDurationsSince(long since, long now, int index) {
        LinkedList linkedList = this.mEvents[index];
        if (linkedList == null || linkedList.size() == 0) {
            return 0L;
        }
        boolean active = true;
        long last = 0;
        long duration = 0;
        Iterator it = linkedList.iterator();
        while (true) {
            boolean z = true;
            if (!it.hasNext()) {
                break;
            }
            BaseAppStateTimeEvents.BaseTimeEvent baseTimeEvent = (BaseAppStateTimeEvents.BaseTimeEvent) it.next();
            if (baseTimeEvent.getTimestamp() >= since && !active) {
                duration += Math.max(0L, baseTimeEvent.getTimestamp() - Math.max(last, since));
            } else {
                last = baseTimeEvent.getTimestamp();
            }
            if (active) {
                z = false;
            }
            active = z;
        }
        if ((linkedList.size() & 1) == 1) {
            return duration + Math.max(0L, now - Math.max(last, since));
        }
        return duration;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isActive(int index) {
        return this.mEvents[index] != null && (this.mEvents[index].size() & 1) == 1;
    }

    @Override // com.android.server.am.BaseAppStateEvents
    String formatEventSummary(long now, int index) {
        return TimeUtils.formatDuration(getTotalDurations(now, index));
    }

    @Override // com.android.server.am.BaseAppStateEvents
    public String toString() {
        return this.mPackageName + SliceClientPermissions.SliceAuthority.DELIMITER + UserHandle.formatUid(this.mUid) + " isActive[0]=" + isActive(0) + " totalDurations[0]=" + getTotalDurations(SystemClock.elapsedRealtime(), 0);
    }
}
