package com.android.server.am;

import android.app.IUidObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import com.android.server.SystemUtil;
import java.io.PrintWriter;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class UidObserverController {
    public static final boolean IS_ROOT_ENABLE = "1".equals(SystemProperties.get("persist.user.root.support", "0"));
    private static final int SLOW_UID_OBSERVER_THRESHOLD_MS = 20;
    private static final boolean VALIDATE_UID_STATES = true;
    private final Handler mHandler;
    SystemUtil mStl;
    private int mUidChangeDispatchCount;
    private final Object mLock = new Object();
    final RemoteCallbackList<IUidObserver> mUidObservers = new RemoteCallbackList<>();
    private final ArrayList<ChangeRecord> mPendingUidChanges = new ArrayList<>();
    private final ArrayList<ChangeRecord> mAvailUidChanges = new ArrayList<>();
    private ChangeRecord[] mActiveUidChanges = new ChangeRecord[5];
    private final Runnable mDispatchRunnable = new Runnable() { // from class: com.android.server.am.UidObserverController$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            UidObserverController.this.dispatchUidsChanged();
        }
    };
    private final ActiveUids mValidateUids = new ActiveUids(null, false);

    /* JADX INFO: Access modifiers changed from: package-private */
    public UidObserverController(Handler handler) {
        this.mHandler = handler;
        if (IS_ROOT_ENABLE) {
            HandlerThread handlerThread = new HandlerThread("KillProcessHandlerOomAdjuster");
            handlerThread.start();
            this.mStl = new SystemUtil(handlerThread.getLooper());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void register(IUidObserver observer, int which, int cutpoint, String callingPackage, int callingUid) {
        synchronized (this.mLock) {
            if (IS_ROOT_ENABLE) {
                this.mUidObservers.register(observer, new UidObserverRegistration(callingUid, callingPackage, which, cutpoint, Binder.getCallingPid()));
            } else {
                this.mUidObservers.register(observer, new UidObserverRegistration(callingUid, callingPackage, which, cutpoint));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregister(IUidObserver observer) {
        synchronized (this.mLock) {
            this.mUidObservers.unregister(observer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int enqueueUidChange(ChangeRecord currentRecord, int uid, int change, int procState, long procStateSeq, int capability, boolean ephemeral) {
        int i;
        synchronized (this.mLock) {
            if (this.mPendingUidChanges.size() == 0) {
                if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                    Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "*** Enqueueing dispatch uid changed!");
                }
                this.mHandler.post(this.mDispatchRunnable);
            }
            ChangeRecord changeRecord = currentRecord != null ? currentRecord : getOrCreateChangeRecordLocked();
            if (!changeRecord.isPending) {
                changeRecord.isPending = true;
                this.mPendingUidChanges.add(changeRecord);
            } else {
                change = mergeWithPendingChange(change, changeRecord.change);
            }
            changeRecord.uid = uid;
            changeRecord.change = change;
            changeRecord.procState = procState;
            changeRecord.procStateSeq = procStateSeq;
            changeRecord.capability = capability;
            changeRecord.ephemeral = ephemeral;
            i = changeRecord.change;
        }
        return i;
    }

    ArrayList<ChangeRecord> getPendingUidChangesForTest() {
        return this.mPendingUidChanges;
    }

    ActiveUids getValidateUidsForTest() {
        return this.mValidateUids;
    }

    Runnable getDispatchRunnableForTest() {
        return this.mDispatchRunnable;
    }

    static int mergeWithPendingChange(int currentChange, int pendingChange) {
        if ((currentChange & 6) == 0) {
            currentChange |= pendingChange & 6;
        }
        if ((currentChange & 24) == 0) {
            currentChange |= pendingChange & 24;
        }
        if ((currentChange & 1) != 0) {
            currentChange &= -13;
        }
        if ((pendingChange & 32) != 0) {
            currentChange |= 32;
        }
        if ((pendingChange & Integer.MIN_VALUE) != 0) {
            currentChange |= Integer.MIN_VALUE;
        }
        if ((pendingChange & 64) != 0) {
            return currentChange | 64;
        }
        return currentChange;
    }

    private ChangeRecord getOrCreateChangeRecordLocked() {
        ChangeRecord changeRecord;
        int size = this.mAvailUidChanges.size();
        if (size > 0) {
            changeRecord = this.mAvailUidChanges.remove(size - 1);
            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "Retrieving available item: " + changeRecord);
            }
        } else {
            changeRecord = new ChangeRecord();
            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "Allocating new item: " + changeRecord);
            }
        }
        return changeRecord;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchUidsChanged() {
        int numUidChanges;
        synchronized (this.mLock) {
            numUidChanges = this.mPendingUidChanges.size();
            if (this.mActiveUidChanges.length < numUidChanges) {
                this.mActiveUidChanges = new ChangeRecord[numUidChanges];
            }
            for (int i = 0; i < numUidChanges; i++) {
                ChangeRecord changeRecord = this.mPendingUidChanges.get(i);
                this.mActiveUidChanges[i] = getOrCreateChangeRecordLocked();
                changeRecord.copyTo(this.mActiveUidChanges[i]);
                changeRecord.isPending = false;
            }
            this.mPendingUidChanges.clear();
            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "*** Delivering " + numUidChanges + " uid changes");
            }
            this.mUidChangeDispatchCount += numUidChanges;
        }
        int i2 = this.mUidObservers.beginBroadcast();
        while (true) {
            int i3 = i2 - 1;
            if (i2 <= 0) {
                break;
            }
            dispatchUidsChangedForObserver(this.mUidObservers.getBroadcastItem(i3), (UidObserverRegistration) this.mUidObservers.getBroadcastCookie(i3), numUidChanges);
            i2 = i3;
        }
        this.mUidObservers.finishBroadcast();
        if (this.mUidObservers.getRegisteredCallbackCount() > 0) {
            for (int j = 0; j < numUidChanges; j++) {
                ChangeRecord item = this.mActiveUidChanges[j];
                if ((item.change & 1) != 0) {
                    this.mValidateUids.remove(item.uid);
                } else {
                    UidRecord validateUid = this.mValidateUids.get(item.uid);
                    if (validateUid == null) {
                        validateUid = new UidRecord(item.uid, null);
                        this.mValidateUids.put(item.uid, validateUid);
                    }
                    if ((item.change & 2) != 0) {
                        validateUid.setIdle(true);
                    } else if ((item.change & 4) != 0) {
                        validateUid.setIdle(false);
                    }
                    validateUid.setSetProcState(item.procState);
                    validateUid.setCurProcState(item.procState);
                    validateUid.setSetCapability(item.capability);
                    validateUid.setCurCapability(item.capability);
                }
            }
        }
        synchronized (this.mLock) {
            for (int j2 = 0; j2 < numUidChanges; j2++) {
                ChangeRecord changeRecord2 = this.mActiveUidChanges[j2];
                changeRecord2.isPending = false;
                this.mAvailUidChanges.add(changeRecord2);
            }
        }
    }

    private void dispatchUidsChangedForObserver(IUidObserver observer, UidObserverRegistration reg, int changesSize) {
        boolean doReport;
        String str;
        int i;
        String str2 = ": ";
        if (observer == null) {
            return;
        }
        int j = 0;
        while (j < changesSize) {
            try {
                ChangeRecord item = this.mActiveUidChanges[j];
                long start = SystemClock.uptimeMillis();
                int change = item.change;
                if (change == Integer.MIN_VALUE && (reg.mWhich & 1) == 0) {
                    str = str2;
                } else if (change == 64 && (reg.mWhich & 64) == 0) {
                    str = str2;
                } else {
                    boolean z = IS_ROOT_ENABLE;
                    if (z) {
                        this.mStl.sendNoBinderMessage(reg.mPid, reg.mPkg, "dispatchUidsChangedForObserver");
                    }
                    if ((change & 2) != 0) {
                        if ((reg.mWhich & 4) != 0) {
                            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                                Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "UID idle uid=" + item.uid);
                            }
                            observer.onUidIdle(item.uid, item.ephemeral);
                        }
                    } else if ((change & 4) != 0 && (reg.mWhich & 8) != 0) {
                        if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                            Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "UID active uid=" + item.uid);
                        }
                        observer.onUidActive(item.uid);
                    }
                    if ((reg.mWhich & 16) != 0) {
                        if ((change & 8) != 0) {
                            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                                Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "UID cached uid=" + item.uid);
                            }
                            observer.onUidCachedChanged(item.uid, true);
                        } else if ((change & 16) != 0) {
                            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                                Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "UID active uid=" + item.uid);
                            }
                            observer.onUidCachedChanged(item.uid, false);
                        }
                    }
                    if ((change & 1) != 0) {
                        if ((reg.mWhich & 2) != 0) {
                            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                                Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "UID gone uid=" + item.uid);
                            }
                            observer.onUidGone(item.uid, item.ephemeral);
                        }
                        if (reg.mLastProcStates == null) {
                            str = str2;
                            i = 20;
                        } else {
                            reg.mLastProcStates.delete(item.uid);
                            str = str2;
                            i = 20;
                        }
                    } else {
                        boolean doReport2 = false;
                        if ((reg.mWhich & 1) != 0) {
                            doReport2 = true;
                            if (reg.mCutpoint >= 0) {
                                int lastState = reg.mLastProcStates.get(item.uid, -1);
                                if (lastState != -1) {
                                    boolean lastAboveCut = lastState <= reg.mCutpoint;
                                    boolean newAboveCut = item.procState <= reg.mCutpoint;
                                    doReport2 = lastAboveCut != newAboveCut;
                                } else {
                                    doReport2 = item.procState != 20;
                                }
                            }
                        }
                        if ((reg.mWhich & 32) == 0) {
                            doReport = doReport2;
                        } else {
                            doReport = doReport2 | ((change & 32) != 0);
                        }
                        if (!doReport) {
                            str = str2;
                            i = 20;
                        } else {
                            if (ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS) {
                                Slog.i(ActivityManagerService.TAG_UID_OBSERVERS, "UID CHANGED uid=" + item.uid + str2 + item.procState + str2 + item.capability);
                            }
                            if (reg.mLastProcStates != null) {
                                reg.mLastProcStates.put(item.uid, item.procState);
                            }
                            str = str2;
                            i = 20;
                            observer.onUidStateChanged(item.uid, item.procState, item.procStateSeq, item.capability);
                        }
                        if ((reg.mWhich & 64) != 0 && (change & 64) != 0) {
                            observer.onUidProcAdjChanged(item.uid);
                        }
                    }
                    int duration = (int) (SystemClock.uptimeMillis() - start);
                    if (reg.mMaxDispatchTime < duration) {
                        reg.mMaxDispatchTime = duration;
                    }
                    if (duration >= i) {
                        reg.mSlowDispatchCount++;
                    }
                    if (z) {
                        this.mStl.sendBinderMessage();
                    }
                }
                j++;
                str2 = str;
            } catch (RemoteException e) {
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UidRecord getValidateUidRecord(int uid) {
        return this.mValidateUids.get(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String dumpPackage) {
        synchronized (this.mLock) {
            int count = this.mUidObservers.getRegisteredCallbackCount();
            boolean printed = false;
            for (int i = 0; i < count; i++) {
                UidObserverRegistration reg = (UidObserverRegistration) this.mUidObservers.getRegisteredCallbackCookie(i);
                if (dumpPackage == null || dumpPackage.equals(reg.mPkg)) {
                    if (!printed) {
                        pw.println("  mUidObservers:");
                        printed = true;
                    }
                    reg.dump(pw, this.mUidObservers.getRegisteredCallbackItem(i));
                }
            }
            pw.println();
            pw.print("  mUidChangeDispatchCount=");
            pw.print(this.mUidChangeDispatchCount);
            pw.println();
            pw.println("  Slow UID dispatches:");
            for (int i2 = 0; i2 < count; i2++) {
                UidObserverRegistration reg2 = (UidObserverRegistration) this.mUidObservers.getRegisteredCallbackCookie(i2);
                pw.print("    ");
                pw.print(this.mUidObservers.getRegisteredCallbackItem(i2).getClass().getTypeName());
                pw.print(": ");
                pw.print(reg2.mSlowDispatchCount);
                pw.print(" / Max ");
                pw.print(reg2.mMaxDispatchTime);
                pw.println("ms");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, String dumpPackage) {
        synchronized (this.mLock) {
            int count = this.mUidObservers.getRegisteredCallbackCount();
            for (int i = 0; i < count; i++) {
                UidObserverRegistration reg = (UidObserverRegistration) this.mUidObservers.getRegisteredCallbackCookie(i);
                if (dumpPackage == null || dumpPackage.equals(reg.mPkg)) {
                    reg.dumpDebug(proto, 2246267895831L);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpValidateUids(PrintWriter pw, String dumpPackage, int dumpAppId, String header, boolean needSep) {
        return this.mValidateUids.dump(pw, dumpPackage, dumpAppId, header, needSep);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpValidateUidsProto(ProtoOutputStream proto, String dumpPackage, int dumpAppId, long fieldId) {
        this.mValidateUids.dumpProto(proto, dumpPackage, dumpAppId, fieldId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class ChangeRecord {
        public int capability;
        public int change;
        public boolean ephemeral;
        public boolean isPending;
        public int procState;
        public long procStateSeq;
        public int uid;

        void copyTo(ChangeRecord changeRecord) {
            changeRecord.isPending = this.isPending;
            changeRecord.uid = this.uid;
            changeRecord.change = this.change;
            changeRecord.procState = this.procState;
            changeRecord.capability = this.capability;
            changeRecord.ephemeral = this.ephemeral;
            changeRecord.procStateSeq = this.procStateSeq;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class UidObserverRegistration {
        private static final int[] ORIG_ENUMS = {4, 8, 2, 1, 32, 64};
        private static final int[] PROTO_ENUMS = {3, 4, 2, 1, 6, 7};
        private final int mCutpoint;
        final SparseIntArray mLastProcStates;
        int mMaxDispatchTime;
        private int mPid;
        private final String mPkg;
        int mSlowDispatchCount;
        private final int mUid;
        private final int mWhich;

        UidObserverRegistration(int uid, String pkg, int which, int cutpoint) {
            this.mPid = 0;
            this.mUid = uid;
            this.mPkg = pkg;
            this.mWhich = which;
            this.mCutpoint = cutpoint;
            this.mLastProcStates = cutpoint >= 0 ? new SparseIntArray() : null;
        }

        UidObserverRegistration(int uid, String pkg, int which, int cutpoint, int pid) {
            this.mPid = 0;
            this.mUid = uid;
            this.mPkg = pkg;
            this.mWhich = which;
            this.mCutpoint = cutpoint;
            this.mPid = pid;
            this.mLastProcStates = cutpoint >= 0 ? new SparseIntArray() : null;
        }

        void dump(PrintWriter pw, IUidObserver observer) {
            pw.print("    ");
            UserHandle.formatUid(pw, this.mUid);
            pw.print(" ");
            pw.print(this.mPkg);
            pw.print(" ");
            pw.print(observer.getClass().getTypeName());
            pw.print(":");
            if ((this.mWhich & 4) != 0) {
                pw.print(" IDLE");
            }
            if ((this.mWhich & 8) != 0) {
                pw.print(" ACT");
            }
            if ((this.mWhich & 2) != 0) {
                pw.print(" GONE");
            }
            if ((this.mWhich & 32) != 0) {
                pw.print(" CAP");
            }
            if ((this.mWhich & 1) != 0) {
                pw.print(" STATE");
                pw.print(" (cut=");
                pw.print(this.mCutpoint);
                pw.print(")");
            }
            pw.println();
            SparseIntArray sparseIntArray = this.mLastProcStates;
            if (sparseIntArray != null) {
                int size = sparseIntArray.size();
                for (int j = 0; j < size; j++) {
                    pw.print("      Last ");
                    UserHandle.formatUid(pw, this.mLastProcStates.keyAt(j));
                    pw.print(": ");
                    pw.println(this.mLastProcStates.valueAt(j));
                }
            }
        }

        void dumpDebug(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(CompanionMessage.MESSAGE_ID, this.mUid);
            proto.write(1138166333442L, this.mPkg);
            ProtoUtils.writeBitWiseFlagsToProtoEnum(proto, 2259152797699L, this.mWhich, ORIG_ENUMS, PROTO_ENUMS);
            proto.write(1120986464260L, this.mCutpoint);
            SparseIntArray sparseIntArray = this.mLastProcStates;
            if (sparseIntArray != null) {
                int size = sparseIntArray.size();
                for (int i = 0; i < size; i++) {
                    long pToken = proto.start(2246267895813L);
                    proto.write(CompanionMessage.MESSAGE_ID, this.mLastProcStates.keyAt(i));
                    proto.write(1120986464258L, this.mLastProcStates.valueAt(i));
                    proto.end(pToken);
                }
            }
            proto.end(token);
        }
    }
}
