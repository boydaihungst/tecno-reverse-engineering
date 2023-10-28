package com.android.server.am;

import android.app.ActivityManager;
import android.app.IApplicationThread;
import android.content.pm.ApplicationInfo;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.os.Debug;
import android.os.SystemClock;
import android.util.DebugUtils;
import android.util.TimeUtils;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.am.ProcessList;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class ProcessProfileRecord {
    static final int HOSTING_COMPONENT_TYPE_ACTIVITY = 16;
    static final int HOSTING_COMPONENT_TYPE_BACKUP = 4;
    static final int HOSTING_COMPONENT_TYPE_BOUND_SERVICE = 512;
    static final int HOSTING_COMPONENT_TYPE_BROADCAST_RECEIVER = 32;
    static final int HOSTING_COMPONENT_TYPE_EMPTY = 0;
    static final int HOSTING_COMPONENT_TYPE_FOREGROUND_SERVICE = 256;
    static final int HOSTING_COMPONENT_TYPE_INSTRUMENTATION = 8;
    static final int HOSTING_COMPONENT_TYPE_PERSISTENT = 2;
    static final int HOSTING_COMPONENT_TYPE_PROVIDER = 64;
    static final int HOSTING_COMPONENT_TYPE_STARTED_SERVICE = 128;
    static final int HOSTING_COMPONENT_TYPE_SYSTEM = 1;
    final ProcessRecord mApp;
    private ProcessState mBaseProcessTracker;
    private BatteryStatsImpl.Uid.Proc mCurProcBatteryStats;
    private int mCurRawAdj;
    private long mInitialIdlePss;
    private long mLastCachedPss;
    private long mLastCachedSwapPss;
    private long mLastLowMemory;
    private Debug.MemoryInfo mLastMemInfo;
    private long mLastMemInfoTime;
    private long mLastPss;
    private long mLastPssTime;
    private long mLastRequestedGc;
    private long mLastRss;
    private long mLastStateTime;
    private long mLastSwapPss;
    private long mNextPssTime;
    private boolean mPendingUiClean;
    private int mPid;
    private final ActivityManagerGlobalLock mProcLock;
    final Object mProfilerLock;
    private int mPssStatType;
    private boolean mReportLowMemory;
    private final ActivityManagerService mService;
    private int mSetAdj;
    private int mSetProcState;
    private IApplicationThread mThread;
    private int mTrimMemoryLevel;
    private final ProcessList.ProcStateMemTracker mProcStateMemTracker = new ProcessList.ProcStateMemTracker();
    private int mPssProcState = 20;
    final AtomicLong mLastCpuTime = new AtomicLong(0);
    final AtomicLong mCurCpuTime = new AtomicLong(0);
    private AtomicInteger mCurrentHostingComponentTypes = new AtomicInteger(0);
    private AtomicInteger mHistoricalHostingComponentTypes = new AtomicInteger(0);

    /* loaded from: classes.dex */
    @interface HostingComponentType {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessProfileRecord(ProcessRecord app) {
        this.mApp = app;
        ActivityManagerService activityManagerService = app.mService;
        this.mService = activityManagerService;
        this.mProcLock = activityManagerService.mProcLock;
        this.mProfilerLock = activityManagerService.mAppProfiler.mProfilerLock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(long now) {
        this.mNextPssTime = now;
        this.mLastPssTime = now;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessState getBaseProcessTracker() {
        return this.mBaseProcessTracker;
    }

    void setBaseProcessTracker(ProcessState baseProcessTracker) {
        this.mBaseProcessTracker = baseProcessTracker;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onProcessActive(IApplicationThread thread, final ProcessStatsService tracker) {
        if (this.mThread == null) {
            synchronized (this.mProfilerLock) {
                synchronized (tracker.mLock) {
                    final ProcessState origBase = getBaseProcessTracker();
                    PackageList pkgList = this.mApp.getPkgList();
                    if (origBase != null) {
                        synchronized (pkgList) {
                            origBase.setState(-1, tracker.getMemFactorLocked(), SystemClock.uptimeMillis(), pkgList.getPackageListLocked());
                            pkgList.forEachPackage(new BiConsumer() { // from class: com.android.server.am.ProcessProfileRecord$$ExternalSyntheticLambda0
                                @Override // java.util.function.BiConsumer
                                public final void accept(Object obj, Object obj2) {
                                    ProcessProfileRecord.this.m1480x2095c8b((String) obj, (ProcessStats.ProcessStateHolder) obj2);
                                }
                            });
                        }
                        origBase.makeInactive();
                    }
                    ApplicationInfo info = this.mApp.info;
                    final ProcessState baseProcessTracker = tracker.getProcessStateLocked(info.packageName, info.uid, info.longVersionCode, this.mApp.processName);
                    setBaseProcessTracker(baseProcessTracker);
                    baseProcessTracker.makeActive();
                    pkgList.forEachPackage(new BiConsumer() { // from class: com.android.server.am.ProcessProfileRecord$$ExternalSyntheticLambda1
                        @Override // java.util.function.BiConsumer
                        public final void accept(Object obj, Object obj2) {
                            ProcessProfileRecord.this.m1481xc4f5c5ea(origBase, tracker, baseProcessTracker, (String) obj, (ProcessStats.ProcessStateHolder) obj2);
                        }
                    });
                    this.mThread = thread;
                }
            }
            return;
        }
        synchronized (this.mProfilerLock) {
            this.mThread = thread;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onProcessActive$0$com-android-server-am-ProcessProfileRecord  reason: not valid java name */
    public /* synthetic */ void m1480x2095c8b(String pkgName, ProcessStats.ProcessStateHolder holder) {
        FrameworkStatsLog.write(3, this.mApp.uid, this.mApp.processName, pkgName, ActivityManager.processStateAmToProto(-1), holder.appVersion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onProcessActive$1$com-android-server-am-ProcessProfileRecord  reason: not valid java name */
    public /* synthetic */ void m1481xc4f5c5ea(ProcessState origBase, ProcessStatsService tracker, ProcessState baseProcessTracker, String pkgName, ProcessStats.ProcessStateHolder holder) {
        if (holder.state != null && holder.state != origBase) {
            holder.state.makeInactive();
        }
        tracker.updateProcessStateHolderLocked(holder, pkgName, this.mApp.info.uid, this.mApp.info.longVersionCode, this.mApp.processName);
        if (holder.state != baseProcessTracker) {
            holder.state.makeActive();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onProcessInactive(ProcessStatsService tracker) {
        synchronized (this.mProfilerLock) {
            synchronized (tracker.mLock) {
                final ProcessState origBase = getBaseProcessTracker();
                if (origBase != null) {
                    PackageList pkgList = this.mApp.getPkgList();
                    synchronized (pkgList) {
                        origBase.setState(-1, tracker.getMemFactorLocked(), SystemClock.uptimeMillis(), pkgList.getPackageListLocked());
                        pkgList.forEachPackage(new BiConsumer() { // from class: com.android.server.am.ProcessProfileRecord$$ExternalSyntheticLambda2
                            @Override // java.util.function.BiConsumer
                            public final void accept(Object obj, Object obj2) {
                                ProcessProfileRecord.this.m1482x81ef02c4((String) obj, (ProcessStats.ProcessStateHolder) obj2);
                            }
                        });
                    }
                    origBase.makeInactive();
                    setBaseProcessTracker(null);
                    pkgList.forEachPackageProcessStats(new Consumer() { // from class: com.android.server.am.ProcessProfileRecord$$ExternalSyntheticLambda3
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ProcessProfileRecord.lambda$onProcessInactive$3(origBase, (ProcessStats.ProcessStateHolder) obj);
                        }
                    });
                }
                this.mThread = null;
            }
        }
        this.mCurrentHostingComponentTypes.set(0);
        this.mHistoricalHostingComponentTypes.set(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onProcessInactive$2$com-android-server-am-ProcessProfileRecord  reason: not valid java name */
    public /* synthetic */ void m1482x81ef02c4(String pkgName, ProcessStats.ProcessStateHolder holder) {
        FrameworkStatsLog.write(3, this.mApp.uid, this.mApp.processName, pkgName, ActivityManager.processStateAmToProto(-1), holder.appVersion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onProcessInactive$3(ProcessState origBase, ProcessStats.ProcessStateHolder holder) {
        if (holder.state != null && holder.state != origBase) {
            holder.state.makeInactive();
        }
        holder.pkg = null;
        holder.state = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastPssTime() {
        return this.mLastPssTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastPssTime(long lastPssTime) {
        this.mLastPssTime = lastPssTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getNextPssTime() {
        return this.mNextPssTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setNextPssTime(long nextPssTime) {
        this.mNextPssTime = nextPssTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getInitialIdlePss() {
        return this.mInitialIdlePss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInitialIdlePss(long initialIdlePss) {
        this.mInitialIdlePss = initialIdlePss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastPss() {
        return this.mLastPss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastPss(long lastPss) {
        this.mLastPss = lastPss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastCachedPss() {
        return this.mLastCachedPss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastCachedPss(long lastCachedPss) {
        this.mLastCachedPss = lastCachedPss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastSwapPss() {
        return this.mLastSwapPss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastSwapPss(long lastSwapPss) {
        this.mLastSwapPss = lastSwapPss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastCachedSwapPss() {
        return this.mLastCachedSwapPss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastCachedSwapPss(long lastCachedSwapPss) {
        this.mLastCachedSwapPss = lastCachedSwapPss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastRss() {
        return this.mLastRss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastRss(long lastRss) {
        this.mLastRss = lastRss;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Debug.MemoryInfo getLastMemInfo() {
        return this.mLastMemInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastMemInfo(Debug.MemoryInfo lastMemInfo) {
        this.mLastMemInfo = lastMemInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastMemInfoTime() {
        return this.mLastMemInfoTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastMemInfoTime(long lastMemInfoTime) {
        this.mLastMemInfoTime = lastMemInfoTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPssProcState() {
        return this.mPssProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPssProcState(int pssProcState) {
        this.mPssProcState = pssProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPssStatType() {
        return this.mPssStatType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPssStatType(int pssStatType) {
        this.mPssStatType = pssStatType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getTrimMemoryLevel() {
        return this.mTrimMemoryLevel;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTrimMemoryLevel(int trimMemoryLevel) {
        this.mTrimMemoryLevel = trimMemoryLevel;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPendingUiClean() {
        return this.mPendingUiClean;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPendingUiClean(boolean pendingUiClean) {
        this.mPendingUiClean = pendingUiClean;
        this.mApp.getWindowProcessController().setPendingUiClean(pendingUiClean);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BatteryStatsImpl.Uid.Proc getCurProcBatteryStats() {
        return this.mCurProcBatteryStats;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurProcBatteryStats(BatteryStatsImpl.Uid.Proc curProcBatteryStats) {
        this.mCurProcBatteryStats = curProcBatteryStats;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastRequestedGc() {
        return this.mLastRequestedGc;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastRequestedGc(long lastRequestedGc) {
        this.mLastRequestedGc = lastRequestedGc;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastLowMemory() {
        return this.mLastLowMemory;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastLowMemory(long lastLowMemory) {
        this.mLastLowMemory = lastLowMemory;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getReportLowMemory() {
        return this.mReportLowMemory;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setReportLowMemory(boolean reportLowMemory) {
        this.mReportLowMemory = reportLowMemory;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addPss(long pss, long uss, long rss, boolean always, int type, long duration) {
        synchronized (this.mService.mProcessStats.mLock) {
            ProcessState tracker = this.mBaseProcessTracker;
            if (tracker != null) {
                PackageList pkgList = this.mApp.getPkgList();
                synchronized (pkgList) {
                    tracker.addPss(pss, uss, rss, always, type, duration, pkgList.getPackageListLocked());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportExcessiveCpu() {
        synchronized (this.mService.mProcessStats.mLock) {
            ProcessState tracker = this.mBaseProcessTracker;
            if (tracker != null) {
                PackageList pkgList = this.mApp.getPkgList();
                synchronized (pkgList) {
                    tracker.reportExcessiveCpu(pkgList.getPackageListLocked());
                }
            }
        }
    }

    void reportCachedKill() {
        synchronized (this.mService.mProcessStats.mLock) {
            ProcessState tracker = this.mBaseProcessTracker;
            if (tracker != null) {
                PackageList pkgList = this.mApp.getPkgList();
                synchronized (pkgList) {
                    tracker.reportCachedKill(pkgList.getPackageListLocked(), this.mLastCachedPss);
                    pkgList.forEachPackageProcessStats(new Consumer() { // from class: com.android.server.am.ProcessProfileRecord$$ExternalSyntheticLambda4
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ProcessProfileRecord.this.m1483x79a36daf((ProcessStats.ProcessStateHolder) obj);
                        }
                    });
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reportCachedKill$4$com-android-server-am-ProcessProfileRecord  reason: not valid java name */
    public /* synthetic */ void m1483x79a36daf(ProcessStats.ProcessStateHolder holder) {
        FrameworkStatsLog.write(17, this.mApp.info.uid, holder.state.getName(), holder.state.getPackage(), this.mLastCachedPss, holder.appVersion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setProcessTrackerState(int procState, int memFactor) {
        synchronized (this.mService.mProcessStats.mLock) {
            ProcessState tracker = this.mBaseProcessTracker;
            if (tracker != null && procState != 20) {
                PackageList pkgList = this.mApp.getPkgList();
                long now = SystemClock.uptimeMillis();
                synchronized (pkgList) {
                    tracker.setState(procState, memFactor, now, pkgList.getPackageListLocked());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void commitNextPssTime() {
        commitNextPssTime(this.mProcStateMemTracker);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void abortNextPssTime() {
        abortNextPssTime(this.mProcStateMemTracker);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long computeNextPssTime(int procState, boolean test, boolean sleeping, long now) {
        return ProcessList.computeNextPssTime(procState, this.mProcStateMemTracker, test, sleeping, now);
    }

    private static void commitNextPssTime(ProcessList.ProcStateMemTracker tracker) {
        if (tracker.mPendingMemState >= 0) {
            tracker.mHighestMem[tracker.mPendingMemState] = tracker.mPendingHighestMemState;
            tracker.mScalingFactor[tracker.mPendingMemState] = tracker.mPendingScalingFactor;
            tracker.mTotalHighestMem = tracker.mPendingHighestMemState;
            tracker.mPendingMemState = -1;
        }
    }

    private static void abortNextPssTime(ProcessList.ProcStateMemTracker tracker) {
        tracker.mPendingMemState = -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPid() {
        return this.mPid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPid(int pid) {
        this.mPid = pid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IApplicationThread getThread() {
        return this.mThread;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSetProcState() {
        return this.mSetProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSetAdj() {
        return this.mSetAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurRawAdj() {
        return this.mCurRawAdj;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastStateTime() {
        return this.mLastStateTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateProcState(ProcessStateRecord state) {
        this.mSetProcState = state.getCurProcState();
        this.mSetAdj = state.getCurAdj();
        this.mCurRawAdj = state.getCurRawAdj();
        this.mLastStateTime = state.getLastStateTime();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addHostingComponentType(int type) {
        AtomicInteger atomicInteger = this.mCurrentHostingComponentTypes;
        atomicInteger.set(atomicInteger.get() | type);
        AtomicInteger atomicInteger2 = this.mHistoricalHostingComponentTypes;
        atomicInteger2.set(atomicInteger2.get() | type);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearHostingComponentType(int type) {
        AtomicInteger atomicInteger = this.mCurrentHostingComponentTypes;
        atomicInteger.set(atomicInteger.get() & (~type));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentHostingComponentTypes() {
        return this.mCurrentHostingComponentTypes.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getHistoricalHostingComponentTypes() {
        return this.mHistoricalHostingComponentTypes.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpPss(PrintWriter pw, String prefix, long nowUptime) {
        synchronized (this.mProfilerLock) {
            pw.print(prefix);
            pw.print("lastPssTime=");
            TimeUtils.formatDuration(this.mLastPssTime, nowUptime, pw);
            pw.print(" pssProcState=");
            pw.print(this.mPssProcState);
            pw.print(" pssStatType=");
            pw.print(this.mPssStatType);
            pw.print(" nextPssTime=");
            TimeUtils.formatDuration(this.mNextPssTime, nowUptime, pw);
            pw.println();
            pw.print(prefix);
            pw.print("lastPss=");
            DebugUtils.printSizeValue(pw, this.mLastPss * GadgetFunction.NCM);
            pw.print(" lastSwapPss=");
            DebugUtils.printSizeValue(pw, this.mLastSwapPss * GadgetFunction.NCM);
            pw.print(" lastCachedPss=");
            DebugUtils.printSizeValue(pw, this.mLastCachedPss * GadgetFunction.NCM);
            pw.print(" lastCachedSwapPss=");
            DebugUtils.printSizeValue(pw, this.mLastCachedSwapPss * GadgetFunction.NCM);
            pw.print(" lastRss=");
            DebugUtils.printSizeValue(pw, this.mLastRss * GadgetFunction.NCM);
            pw.println();
            pw.print(prefix);
            pw.print("trimMemoryLevel=");
            pw.println(this.mTrimMemoryLevel);
            pw.print(prefix);
            pw.print("procStateMemTracker: ");
            this.mProcStateMemTracker.dumpLine(pw);
            pw.print(prefix);
            pw.print("lastRequestedGc=");
            TimeUtils.formatDuration(this.mLastRequestedGc, nowUptime, pw);
            pw.print(" lastLowMemory=");
            TimeUtils.formatDuration(this.mLastLowMemory, nowUptime, pw);
            pw.print(" reportLowMemory=");
            pw.println(this.mReportLowMemory);
        }
        pw.print(prefix);
        pw.print("currentHostingComponentTypes=0x");
        pw.print(Integer.toHexString(getCurrentHostingComponentTypes()));
        pw.print(" historicalHostingComponentTypes=0x");
        pw.println(Integer.toHexString(getHistoricalHostingComponentTypes()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpCputime(PrintWriter pw, String prefix) {
        long lastCpuTime = this.mLastCpuTime.get();
        pw.print(prefix);
        pw.print("lastCpuTime=");
        pw.print(lastCpuTime);
        if (lastCpuTime > 0) {
            pw.print(" timeUsed=");
            TimeUtils.formatDuration(this.mCurCpuTime.get() - lastCpuTime, pw);
        }
        pw.println();
    }
}
