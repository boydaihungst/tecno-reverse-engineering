package com.android.server.am;

import com.android.server.am.BaseAppStateEvents;
import java.util.Iterator;
import java.util.LinkedList;
/* loaded from: classes.dex */
class BaseAppStateTimeSlotEvents extends BaseAppStateEvents<Integer> {
    static final boolean DEBUG_BASE_APP_TIME_SLOT_EVENTS = false;
    long[] mCurSlotStartTime;
    final long mTimeSlotSize;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateTimeSlotEvents(int uid, String packageName, int numOfEventTypes, long timeslotSize, String tag, BaseAppStateEvents.MaxTrackingDurationConfig maxTrackingDurationConfig) {
        super(uid, packageName, numOfEventTypes, tag, maxTrackingDurationConfig);
        this.mTimeSlotSize = timeslotSize;
        this.mCurSlotStartTime = new long[numOfEventTypes];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateTimeSlotEvents(BaseAppStateTimeSlotEvents other) {
        super(other);
        this.mTimeSlotSize = other.mTimeSlotSize;
        this.mCurSlotStartTime = new long[other.mCurSlotStartTime.length];
        int i = 0;
        while (true) {
            long[] jArr = this.mCurSlotStartTime;
            if (i < jArr.length) {
                jArr[i] = other.mCurSlotStartTime[i];
                i++;
            } else {
                return;
            }
        }
    }

    @Override // com.android.server.am.BaseAppStateEvents
    LinkedList<Integer> add(LinkedList<Integer> events, LinkedList<Integer> otherEvents) {
        return null;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v2, resolved type: java.util.LinkedList<E>[] */
    /* JADX DEBUG: Multi-variable search result rejected for r1v7, resolved type: java.util.LinkedList<E>[] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.am.BaseAppStateEvents
    public void add(BaseAppStateEvents otherObj) {
        LinkedList linkedList;
        if (otherObj == null || !(otherObj instanceof BaseAppStateTimeSlotEvents)) {
            return;
        }
        BaseAppStateTimeSlotEvents other = (BaseAppStateTimeSlotEvents) otherObj;
        if (this.mEvents.length != other.mEvents.length) {
            return;
        }
        for (int i = 0; i < this.mEvents.length; i++) {
            LinkedList linkedList2 = other.mEvents[i];
            if (linkedList2 != null && linkedList2.size() != 0) {
                LinkedList linkedList3 = this.mEvents[i];
                if (linkedList3 == null) {
                    linkedList = linkedList2;
                } else if (linkedList3.size() == 0) {
                    linkedList = linkedList2;
                } else {
                    LinkedList<Integer> dest = new LinkedList<>();
                    Iterator<Integer> itl = linkedList3.iterator();
                    Iterator<Integer> itr = linkedList2.iterator();
                    long maxl = this.mCurSlotStartTime[i];
                    long maxr = other.mCurSlotStartTime[i];
                    BaseAppStateTimeSlotEvents other2 = other;
                    long minl = maxl - (this.mTimeSlotSize * (linkedList3.size() - 1));
                    long minr = maxr - (this.mTimeSlotSize * (linkedList2.size() - 1));
                    long latest = Math.max(maxl, maxr);
                    long earliest = Math.min(minl, minr);
                    long start = earliest;
                    while (start <= latest) {
                        int i2 = 0;
                        int intValue = (start < minl || start > maxl) ? 0 : itl.next().intValue();
                        if (start >= minr && start <= maxr) {
                            i2 = itr.next().intValue();
                        }
                        dest.add(Integer.valueOf(intValue + i2));
                        start += this.mTimeSlotSize;
                        minl = minl;
                    }
                    this.mEvents[i] = dest;
                    if (maxl >= maxr) {
                        other = other2;
                    } else {
                        other = other2;
                        this.mCurSlotStartTime[i] = other.mCurSlotStartTime[i];
                    }
                    trimEvents(getEarliest(this.mCurSlotStartTime[i]), i);
                }
                this.mEvents[i] = new LinkedList(linkedList);
                this.mCurSlotStartTime[i] = other.mCurSlotStartTime[i];
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.am.BaseAppStateEvents
    public int getTotalEventsSince(long since, long now, int index) {
        LinkedList linkedList = this.mEvents[index];
        if (linkedList != null && linkedList.size() != 0) {
            long start = getSlotStartTime(since);
            if (start <= this.mCurSlotStartTime[index]) {
                long end = Math.min(getSlotStartTime(now), this.mCurSlotStartTime[index]);
                Iterator<Integer> it = linkedList.descendingIterator();
                int count = 0;
                long time = this.mCurSlotStartTime[index];
                while (time >= start && it.hasNext()) {
                    int val = it.next().intValue();
                    if (time <= end) {
                        count += val;
                    }
                    time -= this.mTimeSlotSize;
                }
                return count;
            }
            return 0;
        }
        return 0;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v14, resolved type: java.util.LinkedList<E>[] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public void addEvent(long now, int index) {
        long slot = getSlotStartTime(now);
        LinkedList linkedList = this.mEvents[index];
        if (linkedList == null) {
            linkedList = new LinkedList();
            this.mEvents[index] = linkedList;
        }
        if (linkedList.size() == 0) {
            linkedList.add(1);
        } else {
            long start = this.mCurSlotStartTime[index];
            while (start < slot) {
                linkedList.add(0);
                start += this.mTimeSlotSize;
            }
            linkedList.offerLast(Integer.valueOf(((Integer) linkedList.pollLast()).intValue() + 1));
        }
        this.mCurSlotStartTime[index] = slot;
        trimEvents(getEarliest(now), index);
    }

    @Override // com.android.server.am.BaseAppStateEvents
    void trimEvents(long earliest, int index) {
        LinkedList linkedList = this.mEvents[index];
        if (linkedList == null || linkedList.size() == 0) {
            return;
        }
        long slot = getSlotStartTime(earliest);
        long time = this.mCurSlotStartTime[index] - (this.mTimeSlotSize * (linkedList.size() - 1));
        while (time < slot && linkedList.size() > 0) {
            linkedList.pop();
            time += this.mTimeSlotSize;
        }
    }

    long getSlotStartTime(long timestamp) {
        return timestamp - (timestamp % this.mTimeSlotSize);
    }

    long getCurrentSlotStartTime(int index) {
        return this.mCurSlotStartTime[index];
    }
}
