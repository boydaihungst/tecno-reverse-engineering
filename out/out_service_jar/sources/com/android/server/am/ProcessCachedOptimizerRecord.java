package com.android.server.am;

import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class ProcessCachedOptimizerRecord {
    static final String IS_FROZEN = "isFrozen";
    private final ProcessRecord mApp;
    private boolean mForceCompact;
    private boolean mFreezeExempt;
    private long mFreezeUnfreezeTime;
    boolean mFreezerOverride;
    private boolean mFrozen;
    private int mLastCompactAction;
    private long mLastCompactTime;
    private boolean mPendingCompact;
    private boolean mPendingFreeze;
    private final ActivityManagerGlobalLock mProcLock;
    private int mReqCompactAction;
    private boolean mShouldNotFreeze;

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastCompactTime() {
        return this.mLastCompactTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastCompactTime(long lastCompactTime) {
        this.mLastCompactTime = lastCompactTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getReqCompactAction() {
        return this.mReqCompactAction;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReqCompactAction(int reqCompactAction) {
        this.mReqCompactAction = reqCompactAction;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLastCompactAction() {
        return this.mLastCompactAction;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastCompactAction(int lastCompactAction) {
        this.mLastCompactAction = lastCompactAction;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPendingCompact() {
        return this.mPendingCompact;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasPendingCompact(boolean pendingCompact) {
        this.mPendingCompact = pendingCompact;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isForceCompact() {
        return this.mForceCompact;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForceCompact(boolean forceCompact) {
        this.mForceCompact = forceCompact;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFrozen() {
        return this.mFrozen;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFrozen(boolean frozen) {
        this.mFrozen = frozen;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasFreezerOverride() {
        return this.mFreezerOverride;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFreezerOverride(boolean freezerOverride) {
        this.mFreezerOverride = freezerOverride;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getFreezeUnfreezeTime() {
        return this.mFreezeUnfreezeTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFreezeUnfreezeTime(long freezeUnfreezeTime) {
        this.mFreezeUnfreezeTime = freezeUnfreezeTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldNotFreeze() {
        return this.mShouldNotFreeze;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShouldNotFreeze(boolean shouldNotFreeze) {
        this.mShouldNotFreeze = shouldNotFreeze;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFreezeExempt() {
        return this.mFreezeExempt;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPendingFreeze(boolean freeze) {
        this.mPendingFreeze = freeze;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPendingFreeze() {
        return this.mPendingFreeze;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFreezeExempt(boolean exempt) {
        this.mFreezeExempt = exempt;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessCachedOptimizerRecord(ProcessRecord app) {
        this.mApp = app;
        this.mProcLock = app.mService.mProcLock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(long nowUptime) {
        this.mFreezeUnfreezeTime = nowUptime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, long nowUptime) {
        pw.print(prefix);
        pw.print("lastCompactTime=");
        pw.print(this.mLastCompactTime);
        pw.print(" lastCompactAction=");
        pw.println(this.mLastCompactAction);
        pw.print(prefix);
        pw.print("hasPendingCompaction=");
        pw.print(this.mPendingCompact);
        pw.print(prefix);
        pw.print("isFreezeExempt=");
        pw.print(this.mFreezeExempt);
        pw.print(" isPendingFreeze=");
        pw.print(this.mPendingFreeze);
        pw.print(" isFrozen=");
        pw.println(this.mFrozen);
    }
}
