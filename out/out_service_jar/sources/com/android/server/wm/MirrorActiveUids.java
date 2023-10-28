package com.android.server.wm;

import android.util.SparseIntArray;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
class MirrorActiveUids {
    private final SparseIntArray mUidStates = new SparseIntArray();
    private final SparseIntArray mNumNonAppVisibleWindowMap = new SparseIntArray();

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onUidActive(int uid, int procState) {
        this.mUidStates.put(uid, procState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onUidInactive(int uid) {
        this.mUidStates.delete(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onUidProcStateChanged(int uid, int procState) {
        int index = this.mUidStates.indexOfKey(uid);
        if (index >= 0) {
            this.mUidStates.setValueAt(index, procState);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized int getUidState(int uid) {
        return this.mUidStates.get(uid, 20);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onNonAppSurfaceVisibilityChanged(int uid, boolean visible) {
        int index = this.mNumNonAppVisibleWindowMap.indexOfKey(uid);
        int i = 1;
        if (index >= 0) {
            int valueAt = this.mNumNonAppVisibleWindowMap.valueAt(index);
            if (!visible) {
                i = -1;
            }
            int num = valueAt + i;
            if (num > 0) {
                this.mNumNonAppVisibleWindowMap.setValueAt(index, num);
            } else {
                this.mNumNonAppVisibleWindowMap.removeAt(index);
            }
        } else if (visible) {
            this.mNumNonAppVisibleWindowMap.append(uid, 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized boolean hasNonAppVisibleWindow(int uid) {
        return this.mNumNonAppVisibleWindowMap.get(uid) > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void dump(PrintWriter pw, String prefix) {
        pw.print(prefix + "NumNonAppVisibleWindowUidMap:[");
        for (int i = this.mNumNonAppVisibleWindowMap.size() - 1; i >= 0; i--) {
            pw.print(" " + this.mNumNonAppVisibleWindowMap.keyAt(i) + ":" + this.mNumNonAppVisibleWindowMap.valueAt(i));
        }
        pw.println("]");
    }
}
