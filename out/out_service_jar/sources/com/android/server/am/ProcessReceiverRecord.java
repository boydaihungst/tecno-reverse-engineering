package com.android.server.am;

import android.util.ArraySet;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class ProcessReceiverRecord {
    final ProcessRecord mApp;
    private final ArraySet<BroadcastRecord> mCurReceivers = new ArraySet<>();
    private final ArraySet<ReceiverList> mReceivers = new ArraySet<>();
    private final ActivityManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public int numberOfCurReceivers() {
        return this.mCurReceivers.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastRecord getCurReceiverAt(int index) {
        return this.mCurReceivers.valueAt(index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasCurReceiver(BroadcastRecord receiver) {
        return this.mCurReceivers.contains(receiver);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addCurReceiver(BroadcastRecord receiver) {
        this.mCurReceivers.add(receiver);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeCurReceiver(BroadcastRecord receiver) {
        this.mCurReceivers.remove(receiver);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int numberOfReceivers() {
        return this.mReceivers.size();
    }

    ReceiverList getReceiverAt(int index) {
        return this.mReceivers.valueAt(index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addReceiver(ReceiverList receiver) {
        this.mReceivers.add(receiver);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeReceiver(ReceiverList receiver) {
        this.mReceivers.remove(receiver);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessReceiverRecord(ProcessRecord app) {
        this.mApp = app;
        this.mService = app.mService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCleanupApplicationRecordLocked() {
        for (int i = this.mReceivers.size() - 1; i >= 0; i--) {
            this.mService.removeReceiverLocked(this.mReceivers.valueAt(i));
        }
        this.mReceivers.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, long nowUptime) {
        if (!this.mCurReceivers.isEmpty()) {
            pw.print(prefix);
            pw.println("Current mReceivers:");
            int size = this.mCurReceivers.size();
            for (int i = 0; i < size; i++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.mCurReceivers.valueAt(i));
            }
        }
        if (this.mReceivers.size() > 0) {
            pw.print(prefix);
            pw.println("mReceivers:");
            int size2 = this.mReceivers.size();
            for (int i2 = 0; i2 < size2; i2++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.mReceivers.valueAt(i2));
            }
        }
    }
}
