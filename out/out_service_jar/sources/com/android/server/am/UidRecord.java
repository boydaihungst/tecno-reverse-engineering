package com.android.server.am;

import android.app.ActivityManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import com.android.server.am.UidObserverController;
import com.android.server.pm.verify.domain.DomainVerificationPersistence;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public final class UidRecord {
    static final int CHANGE_ACTIVE = 4;
    static final int CHANGE_CACHED = 8;
    static final int CHANGE_CAPABILITY = 32;
    static final int CHANGE_GONE = 1;
    static final int CHANGE_IDLE = 2;
    static final int CHANGE_PROCADJ = 64;
    static final int CHANGE_PROCSTATE = Integer.MIN_VALUE;
    static final int CHANGE_UNCACHED = 16;
    private static int[] ORIG_ENUMS = {1, 2, 4, 8, 16, 32, Integer.MIN_VALUE};
    private static int[] PROTO_ENUMS = {0, 1, 2, 3, 4, 5, 6};
    long curProcStateSeq;
    volatile boolean hasInternetPermission;
    long lastNetworkUpdatedProcStateSeq;
    private boolean mCurAllowList;
    private int mCurCapability;
    private int mCurProcState;
    private boolean mEphemeral;
    private boolean mForegroundServices;
    private boolean mIdle;
    private long mLastBackgroundTime;
    private int mLastReportedChange;
    private int mNumProcs;
    private boolean mProcAdjChanged;
    private final ActivityManagerGlobalLock mProcLock;
    private final ActivityManagerService mService;
    private boolean mSetAllowList;
    private int mSetCapability;
    private boolean mSetIdle;
    private final int mUid;
    volatile long procStateSeqWaitingForNetwork;
    private int mSetProcState = 20;
    private ArraySet<ProcessRecord> mProcRecords = new ArraySet<>();
    final Object networkStateLock = new Object();
    final UidObserverController.ChangeRecord pendingChange = new UidObserverController.ChangeRecord();

    public UidRecord(int uid, ActivityManagerService service) {
        this.mUid = uid;
        this.mService = service;
        this.mProcLock = service != null ? service.mProcLock : null;
        this.mIdle = true;
        reset();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUid() {
        return this.mUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurProcState() {
        return this.mCurProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurProcState(int curProcState) {
        this.mCurProcState = curProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSetProcState() {
        return this.mSetProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetProcState(int setProcState) {
        this.mSetProcState = setProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noteProcAdjChanged() {
        this.mProcAdjChanged = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearProcAdjChanged() {
        this.mProcAdjChanged = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getProcAdjChanged() {
        return this.mProcAdjChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurCapability() {
        return this.mCurCapability;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurCapability(int curCapability) {
        this.mCurCapability = curCapability;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSetCapability() {
        return this.mSetCapability;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetCapability(int setCapability) {
        this.mSetCapability = setCapability;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastBackgroundTime() {
        return this.mLastBackgroundTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastBackgroundTime(long lastBackgroundTime) {
        this.mLastBackgroundTime = lastBackgroundTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEphemeral() {
        return this.mEphemeral;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setEphemeral(boolean ephemeral) {
        this.mEphemeral = ephemeral;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundServices() {
        return this.mForegroundServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForegroundServices(boolean foregroundServices) {
        this.mForegroundServices = foregroundServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCurAllowListed() {
        return this.mCurAllowList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurAllowListed(boolean curAllowList) {
        this.mCurAllowList = curAllowList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSetAllowListed() {
        return this.mSetAllowList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetAllowListed(boolean setAllowlist) {
        this.mSetAllowList = setAllowlist;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isIdle() {
        return this.mIdle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIdle(boolean idle) {
        this.mIdle = idle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSetIdle() {
        return this.mSetIdle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSetIdle(boolean setIdle) {
        this.mSetIdle = setIdle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getNumOfProcs() {
        return this.mProcRecords.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachProcess(Consumer<ProcessRecord> callback) {
        for (int i = this.mProcRecords.size() - 1; i >= 0; i--) {
            callback.accept(this.mProcRecords.valueAt(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessRecord getProcessInPackage(String packageName) {
        for (int i = this.mProcRecords.size() - 1; i >= 0; i--) {
            ProcessRecord app = this.mProcRecords.valueAt(i);
            if (app != null && TextUtils.equals(app.info.packageName, packageName)) {
                return app;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addProcess(ProcessRecord app) {
        this.mProcRecords.add(app);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeProcess(ProcessRecord app) {
        this.mProcRecords.remove(app);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastReportedChange(int lastReportedChange) {
        this.mLastReportedChange = lastReportedChange;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reset() {
        setCurProcState(19);
        this.mForegroundServices = false;
        this.mCurCapability = 0;
    }

    public void updateHasInternetPermission() {
        this.hasInternetPermission = ActivityManager.checkUidPermission("android.permission.INTERNET", this.mUid) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, this.mUid);
        proto.write(CompanionMessage.TYPE, ProcessList.makeProcStateProtoEnum(this.mCurProcState));
        proto.write(1133871366147L, this.mEphemeral);
        proto.write(1133871366148L, this.mForegroundServices);
        proto.write(1133871366149L, this.mCurAllowList);
        ProtoUtils.toDuration(proto, 1146756268038L, this.mLastBackgroundTime, SystemClock.elapsedRealtime());
        proto.write(1133871366151L, this.mIdle);
        int i = this.mLastReportedChange;
        if (i != 0) {
            ProtoUtils.writeBitWiseFlagsToProtoEnum(proto, 2259152797704L, i, ORIG_ENUMS, PROTO_ENUMS);
        }
        proto.write(1120986464265L, this.mNumProcs);
        long seqToken = proto.start(1146756268042L);
        proto.write(1112396529665L, this.curProcStateSeq);
        proto.write(1112396529666L, this.lastNetworkUpdatedProcStateSeq);
        proto.end(seqToken);
        proto.end(token);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("UidRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        UserHandle.formatUid(sb, this.mUid);
        sb.append(' ');
        sb.append(ProcessList.makeProcStateString(this.mCurProcState));
        if (this.mEphemeral) {
            sb.append(" ephemeral");
        }
        if (this.mForegroundServices) {
            sb.append(" fgServices");
        }
        if (this.mCurAllowList) {
            sb.append(" allowlist");
        }
        if (this.mLastBackgroundTime > 0) {
            sb.append(" bg:");
            TimeUtils.formatDuration(SystemClock.elapsedRealtime() - this.mLastBackgroundTime, sb);
        }
        if (this.mIdle) {
            sb.append(" idle");
        }
        if (this.mLastReportedChange != 0) {
            sb.append(" change:");
            boolean printed = false;
            if ((this.mLastReportedChange & 1) != 0) {
                printed = true;
                sb.append("gone");
            }
            if ((this.mLastReportedChange & 2) != 0) {
                if (printed) {
                    sb.append("|");
                }
                printed = true;
                sb.append("idle");
            }
            if ((this.mLastReportedChange & 4) != 0) {
                if (printed) {
                    sb.append("|");
                }
                printed = true;
                sb.append(DomainVerificationPersistence.TAG_ACTIVE);
            }
            if ((this.mLastReportedChange & 8) != 0) {
                if (printed) {
                    sb.append("|");
                }
                printed = true;
                sb.append("cached");
            }
            if ((this.mLastReportedChange & 16) != 0) {
                if (printed) {
                    sb.append("|");
                }
                sb.append("uncached");
            }
            if ((this.mLastReportedChange & Integer.MIN_VALUE) != 0) {
                if (printed) {
                    sb.append("|");
                }
                sb.append("procstate");
            }
            if ((this.mLastReportedChange & 64) != 0) {
                if (printed) {
                    sb.append("|");
                }
                sb.append("procadj");
            }
        }
        sb.append(" procs:");
        sb.append(this.mNumProcs);
        sb.append(" seq(");
        sb.append(this.curProcStateSeq);
        sb.append(",");
        sb.append(this.lastNetworkUpdatedProcStateSeq);
        sb.append(")}");
        return sb.toString();
    }
}
