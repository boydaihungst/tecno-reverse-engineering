package com.android.server.usage;

import android.app.usage.BroadcastResponseStats;
import android.util.ArrayMap;
import com.android.internal.util.IndentingPrintWriter;
import java.util.List;
/* loaded from: classes2.dex */
class UserBroadcastResponseStats {
    private ArrayMap<BroadcastEvent, BroadcastResponseStats> mResponseStats = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastResponseStats getBroadcastResponseStats(BroadcastEvent broadcastEvent) {
        return this.mResponseStats.get(broadcastEvent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastResponseStats getOrCreateBroadcastResponseStats(BroadcastEvent broadcastEvent) {
        BroadcastResponseStats responseStats = this.mResponseStats.get(broadcastEvent);
        if (responseStats == null) {
            BroadcastResponseStats responseStats2 = new BroadcastResponseStats(broadcastEvent.getTargetPackage(), broadcastEvent.getIdForResponseEvent());
            this.mResponseStats.put(broadcastEvent, responseStats2);
            return responseStats2;
        }
        return responseStats;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void populateAllBroadcastResponseStats(List<BroadcastResponseStats> broadcastResponseStatsList, String packageName, long id) {
        for (int i = this.mResponseStats.size() - 1; i >= 0; i--) {
            BroadcastEvent broadcastEvent = this.mResponseStats.keyAt(i);
            if ((id == 0 || id == broadcastEvent.getIdForResponseEvent()) && (packageName == null || packageName.equals(broadcastEvent.getTargetPackage()))) {
                broadcastResponseStatsList.add(this.mResponseStats.valueAt(i));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearBroadcastResponseStats(String packageName, long id) {
        for (int i = this.mResponseStats.size() - 1; i >= 0; i--) {
            BroadcastEvent broadcastEvent = this.mResponseStats.keyAt(i);
            if ((id == 0 || id == broadcastEvent.getIdForResponseEvent()) && (packageName == null || packageName.equals(broadcastEvent.getTargetPackage()))) {
                this.mResponseStats.removeAt(i);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageRemoved(String packageName) {
        for (int i = this.mResponseStats.size() - 1; i >= 0; i--) {
            if (this.mResponseStats.keyAt(i).getTargetPackage().equals(packageName)) {
                this.mResponseStats.removeAt(i);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter ipw) {
        for (int i = 0; i < this.mResponseStats.size(); i++) {
            BroadcastEvent broadcastEvent = this.mResponseStats.keyAt(i);
            BroadcastResponseStats responseStats = this.mResponseStats.valueAt(i);
            ipw.print(broadcastEvent);
            ipw.print(" -> ");
            ipw.println(responseStats);
        }
    }
}
