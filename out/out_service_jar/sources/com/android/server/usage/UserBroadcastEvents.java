package com.android.server.usage;

import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LongArrayQueue;
import android.util.TimeUtils;
import com.android.internal.util.IndentingPrintWriter;
/* loaded from: classes2.dex */
class UserBroadcastEvents {
    private ArrayMap<String, ArraySet<BroadcastEvent>> mBroadcastEvents = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArraySet<BroadcastEvent> getBroadcastEvents(String packageName) {
        return this.mBroadcastEvents.get(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArraySet<BroadcastEvent> getOrCreateBroadcastEvents(String packageName) {
        ArraySet<BroadcastEvent> broadcastEvents = this.mBroadcastEvents.get(packageName);
        if (broadcastEvents == null) {
            ArraySet<BroadcastEvent> broadcastEvents2 = new ArraySet<>();
            this.mBroadcastEvents.put(packageName, broadcastEvents2);
            return broadcastEvents2;
        }
        return broadcastEvents;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageRemoved(String packageName) {
        this.mBroadcastEvents.remove(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUidRemoved(int uid) {
        clear(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear(int uid) {
        for (int i = this.mBroadcastEvents.size() - 1; i >= 0; i--) {
            ArraySet<BroadcastEvent> broadcastEvents = this.mBroadcastEvents.valueAt(i);
            for (int j = broadcastEvents.size() - 1; j >= 0; j--) {
                if (broadcastEvents.valueAt(j).getSourceUid() == uid) {
                    broadcastEvents.removeAt(j);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter ipw) {
        for (int i = 0; i < this.mBroadcastEvents.size(); i++) {
            String packageName = this.mBroadcastEvents.keyAt(i);
            ArraySet<BroadcastEvent> broadcastEvents = this.mBroadcastEvents.valueAt(i);
            ipw.println(packageName + ":");
            ipw.increaseIndent();
            if (broadcastEvents.size() == 0) {
                ipw.println("<empty>");
            } else {
                for (int j = 0; j < broadcastEvents.size(); j++) {
                    BroadcastEvent broadcastEvent = broadcastEvents.valueAt(j);
                    ipw.println(broadcastEvent);
                    ipw.increaseIndent();
                    LongArrayQueue timestampsMs = broadcastEvent.getTimestampsMs();
                    for (int timestampIdx = 0; timestampIdx < timestampsMs.size(); timestampIdx++) {
                        if (timestampIdx > 0) {
                            ipw.print(',');
                        }
                        long timestampMs = timestampsMs.get(timestampIdx);
                        TimeUtils.formatDuration(timestampMs, ipw);
                    }
                    ipw.println();
                    ipw.decreaseIndent();
                }
            }
            ipw.decreaseIndent();
        }
    }
}
