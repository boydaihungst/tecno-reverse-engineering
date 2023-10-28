package com.android.server.am;

import android.os.SystemClock;
import android.os.UserHandle;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;
import java.util.LinkedList;
/* loaded from: classes.dex */
abstract class BaseAppStateEvents<E> {
    static final boolean DEBUG_BASE_APP_STATE_EVENTS = false;
    final LinkedList<E>[] mEvents;
    int mExemptReason = -1;
    final MaxTrackingDurationConfig mMaxTrackingDurationConfig;
    final String mPackageName;
    final String mTag;
    final int mUid;

    /* loaded from: classes.dex */
    interface Factory<T extends BaseAppStateEvents> {
        T createAppStateEvents(int i, String str);

        T createAppStateEvents(T t);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface MaxTrackingDurationConfig {
        long getMaxTrackingDuration();
    }

    abstract LinkedList<E> add(LinkedList<E> linkedList, LinkedList<E> linkedList2);

    abstract int getTotalEventsSince(long j, long j2, int i);

    abstract void trimEvents(long j, int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateEvents(int uid, String packageName, int numOfEventTypes, String tag, MaxTrackingDurationConfig maxTrackingDurationConfig) {
        this.mUid = uid;
        this.mPackageName = packageName;
        this.mTag = tag;
        this.mMaxTrackingDurationConfig = maxTrackingDurationConfig;
        this.mEvents = new LinkedList[numOfEventTypes];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BaseAppStateEvents(BaseAppStateEvents other) {
        this.mUid = other.mUid;
        this.mPackageName = other.mPackageName;
        this.mTag = other.mTag;
        this.mMaxTrackingDurationConfig = other.mMaxTrackingDurationConfig;
        this.mEvents = new LinkedList[other.mEvents.length];
        int i = 0;
        while (true) {
            LinkedList<E>[] linkedListArr = this.mEvents;
            if (i < linkedListArr.length) {
                if (other.mEvents[i] != null) {
                    linkedListArr[i] = new LinkedList<>(other.mEvents[i]);
                }
                i++;
            } else {
                return;
            }
        }
    }

    void addEvent(E event, long now, int index) {
        LinkedList<E>[] linkedListArr = this.mEvents;
        if (linkedListArr[index] == null) {
            linkedListArr[index] = new LinkedList<>();
        }
        LinkedList<E> events = this.mEvents[index];
        events.add(event);
        trimEvents(getEarliest(now), index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void trim(long earliest) {
        for (int i = 0; i < this.mEvents.length; i++) {
            trimEvents(earliest, i);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEmpty() {
        int i = 0;
        while (true) {
            LinkedList<E>[] linkedListArr = this.mEvents;
            if (i < linkedListArr.length) {
                LinkedList<E> linkedList = linkedListArr[i];
                if (linkedList == null || linkedList.isEmpty()) {
                    i++;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    boolean isEmpty(int index) {
        LinkedList<E> linkedList = this.mEvents[index];
        return linkedList == null || linkedList.isEmpty();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void add(BaseAppStateEvents other) {
        if (this.mEvents.length != other.mEvents.length) {
            return;
        }
        int i = 0;
        while (true) {
            LinkedList<E>[] linkedListArr = this.mEvents;
            if (i < linkedListArr.length) {
                linkedListArr[i] = add(linkedListArr[i], other.mEvents[i]);
                i++;
            } else {
                return;
            }
        }
    }

    LinkedList<E> getRawEvents(int index) {
        return this.mEvents[index];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getTotalEvents(long now, int index) {
        return getTotalEventsSince(getEarliest(0L), now, index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getEarliest(long now) {
        return Math.max(0L, now - this.mMaxTrackingDurationConfig.getMaxTrackingDuration());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, long nowElapsed) {
        int i = 0;
        while (true) {
            LinkedList<E>[] linkedListArr = this.mEvents;
            if (i < linkedListArr.length) {
                if (linkedListArr[i] != null) {
                    pw.print(prefix);
                    pw.print(formatEventTypeLabel(i));
                    pw.println(formatEventSummary(nowElapsed, i));
                }
                i++;
            } else {
                return;
            }
        }
    }

    String formatEventSummary(long now, int index) {
        return Integer.toString(getTotalEvents(now, index));
    }

    String formatEventTypeLabel(int index) {
        return Integer.toString(index) + ":";
    }

    public String toString() {
        return this.mPackageName + SliceClientPermissions.SliceAuthority.DELIMITER + UserHandle.formatUid(this.mUid) + " totalEvents[0]=" + formatEventSummary(SystemClock.elapsedRealtime(), 0);
    }
}
