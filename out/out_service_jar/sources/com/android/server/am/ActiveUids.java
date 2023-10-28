package com.android.server.am;

import android.app.ActivityManager;
import android.os.UserHandle;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class ActiveUids {
    private final SparseArray<UidRecord> mActiveUids = new SparseArray<>();
    private final boolean mPostChangesToAtm;
    private final ActivityManagerGlobalLock mProcLock;
    private final ActivityManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActiveUids(ActivityManagerService service, boolean postChangesToAtm) {
        this.mService = service;
        this.mProcLock = service != null ? service.mProcLock : null;
        this.mPostChangesToAtm = postChangesToAtm;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void put(int uid, UidRecord value) {
        this.mActiveUids.put(uid, value);
        if (this.mPostChangesToAtm) {
            this.mService.mAtmInternal.onUidActive(uid, value.getCurProcState());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void remove(int uid) {
        this.mActiveUids.remove(uid);
        if (this.mPostChangesToAtm) {
            this.mService.mAtmInternal.onUidInactive(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        this.mActiveUids.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UidRecord get(int uid) {
        return this.mActiveUids.get(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int size() {
        return this.mActiveUids.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UidRecord valueAt(int index) {
        return this.mActiveUids.valueAt(index);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int keyAt(int index) {
        return this.mActiveUids.keyAt(index);
    }

    int indexOfKey(int uid) {
        return this.mActiveUids.indexOfKey(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dump(final PrintWriter pw, String dumpPackage, int dumpAppId, String header, boolean needSep) {
        boolean printed = false;
        for (int i = 0; i < this.mActiveUids.size(); i++) {
            UidRecord uidRec = this.mActiveUids.valueAt(i);
            if (dumpPackage == null || UserHandle.getAppId(uidRec.getUid()) == dumpAppId) {
                if (!printed) {
                    printed = true;
                    if (needSep) {
                        pw.println();
                    }
                    pw.print("  ");
                    pw.println(header);
                }
                pw.print("    UID ");
                UserHandle.formatUid(pw, uidRec.getUid());
                pw.print(": ");
                pw.println(uidRec);
                pw.print("      curProcState=");
                pw.print(uidRec.getCurProcState());
                pw.print(" curCapability=");
                ActivityManager.printCapabilitiesFull(pw, uidRec.getCurCapability());
                pw.println();
                uidRec.forEachProcess(new Consumer() { // from class: com.android.server.am.ActiveUids$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ActiveUids.lambda$dump$0(pw, (ProcessRecord) obj);
                    }
                });
            }
        }
        return printed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dump$0(PrintWriter pw, ProcessRecord app) {
        pw.print("      proc=");
        pw.println(app);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpProto(ProtoOutputStream proto, String dumpPackage, int dumpAppId, long fieldId) {
        for (int i = 0; i < this.mActiveUids.size(); i++) {
            UidRecord uidRec = this.mActiveUids.valueAt(i);
            if (dumpPackage == null || UserHandle.getAppId(uidRec.getUid()) == dumpAppId) {
                uidRec.dumpDebug(proto, fieldId);
            }
        }
    }
}
