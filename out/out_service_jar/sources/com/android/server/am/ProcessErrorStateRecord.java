package com.android.server.am;

import android.app.ActivityManager;
import android.app.AnrController;
import android.app.ApplicationErrorReport;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.IncrementalStatesInfo;
import android.content.pm.PackageManagerInternal;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.incremental.IIncrementalService;
import android.os.incremental.IncrementalManager;
import android.os.incremental.IncrementalMetrics;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.MemoryPressureUtil;
import com.android.server.UiModeManagerService;
import com.android.server.Watchdog;
import com.android.server.am.AppNotRespondingDialog;
import com.android.server.criticalevents.CriticalEventLog;
import com.android.server.wm.WindowProcessController;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class ProcessErrorStateRecord {
    private String mAnrAnnotation;
    private AppNotRespondingDialog.Data mAnrData;
    public final ProcessRecord mApp;
    private boolean mBad;
    private Runnable mCrashHandler;
    private boolean mCrashing;
    private ActivityManager.ProcessErrorStateInfo mCrashingReport;
    private final ErrorDialogController mDialogController;
    private ComponentName mErrorReportReceiver;
    private boolean mForceCrashReport;
    private boolean mNotResponding;
    private ActivityManager.ProcessErrorStateInfo mNotRespondingReport;
    private final ActivityManagerGlobalLock mProcLock;
    private final ActivityManagerService mService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBad() {
        return this.mBad;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBad(boolean bad) {
        this.mBad = bad;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCrashing() {
        return this.mCrashing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCrashing(boolean crashing) {
        this.mCrashing = crashing;
        this.mApp.getWindowProcessController().setCrashing(crashing);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isForceCrashReport() {
        return this.mForceCrashReport;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForceCrashReport(boolean forceCrashReport) {
        this.mForceCrashReport = forceCrashReport;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNotResponding() {
        return this.mNotResponding;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setNotResponding(boolean notResponding) {
        this.mNotResponding = notResponding;
        this.mApp.getWindowProcessController().setNotResponding(notResponding);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Runnable getCrashHandler() {
        return this.mCrashHandler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCrashHandler(Runnable crashHandler) {
        this.mCrashHandler = crashHandler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManager.ProcessErrorStateInfo getCrashingReport() {
        return this.mCrashingReport;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCrashingReport(ActivityManager.ProcessErrorStateInfo crashingReport) {
        this.mCrashingReport = crashingReport;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getAnrAnnotation() {
        return this.mAnrAnnotation;
    }

    void setAnrAnnotation(String anrAnnotation) {
        this.mAnrAnnotation = anrAnnotation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManager.ProcessErrorStateInfo getNotRespondingReport() {
        return this.mNotRespondingReport;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setNotRespondingReport(ActivityManager.ProcessErrorStateInfo notRespondingReport) {
        this.mNotRespondingReport = notRespondingReport;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getErrorReportReceiver() {
        return this.mErrorReportReceiver;
    }

    void setErrorReportReceiver(ComponentName errorReportReceiver) {
        this.mErrorReportReceiver = errorReportReceiver;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ErrorDialogController getDialogController() {
        return this.mDialogController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAnrData(AppNotRespondingDialog.Data data) {
        this.mAnrData = data;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppNotRespondingDialog.Data getAnrData() {
        return this.mAnrData;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessErrorStateRecord(ProcessRecord app) {
        this.mApp = app;
        ActivityManagerService activityManagerService = app.mService;
        this.mService = activityManagerService;
        this.mProcLock = activityManagerService.mProcLock;
        this.mDialogController = new ErrorDialogController(app);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [581=5] */
    /* JADX DEBUG: Multi-variable search result rejected for r36v0, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r36v1, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r36v2, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r42v0, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r42v1, resolved type: boolean */
    /* JADX DEBUG: Multi-variable search result rejected for r42v2, resolved type: boolean */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:158:0x052d  */
    /* JADX WARN: Removed duplicated region for block: B:161:0x054d  */
    /* JADX WARN: Removed duplicated region for block: B:162:0x0553  */
    /* JADX WARN: Removed duplicated region for block: B:165:0x055b  */
    /* JADX WARN: Removed duplicated region for block: B:169:0x056b  */
    /* JADX WARN: Removed duplicated region for block: B:172:0x0575  */
    /* JADX WARN: Removed duplicated region for block: B:173:0x0578  */
    /* JADX WARN: Removed duplicated region for block: B:176:0x0586  */
    /* JADX WARN: Removed duplicated region for block: B:177:0x058d  */
    /* JADX WARN: Removed duplicated region for block: B:180:0x0593  */
    /* JADX WARN: Removed duplicated region for block: B:181:0x0596  */
    /* JADX WARN: Removed duplicated region for block: B:184:0x059c  */
    /* JADX WARN: Removed duplicated region for block: B:185:0x05a3  */
    /* JADX WARN: Removed duplicated region for block: B:188:0x05a8  */
    /* JADX WARN: Removed duplicated region for block: B:189:0x05af  */
    /* JADX WARN: Removed duplicated region for block: B:191:0x05b3  */
    /* JADX WARN: Removed duplicated region for block: B:192:0x05ba  */
    /* JADX WARN: Removed duplicated region for block: B:199:0x05cb  */
    /* JADX WARN: Removed duplicated region for block: B:200:0x05d2  */
    /* JADX WARN: Removed duplicated region for block: B:202:0x05d6  */
    /* JADX WARN: Removed duplicated region for block: B:203:0x05dd  */
    /* JADX WARN: Removed duplicated region for block: B:205:0x05e1  */
    /* JADX WARN: Removed duplicated region for block: B:206:0x05e8  */
    /* JADX WARN: Removed duplicated region for block: B:208:0x05ec  */
    /* JADX WARN: Removed duplicated region for block: B:209:0x05f3  */
    /* JADX WARN: Removed duplicated region for block: B:211:0x05f7  */
    /* JADX WARN: Removed duplicated region for block: B:212:0x05fe  */
    /* JADX WARN: Removed duplicated region for block: B:214:0x0602  */
    /* JADX WARN: Removed duplicated region for block: B:215:0x0609  */
    /* JADX WARN: Removed duplicated region for block: B:217:0x060d  */
    /* JADX WARN: Removed duplicated region for block: B:218:0x0614  */
    /* JADX WARN: Removed duplicated region for block: B:220:0x0618  */
    /* JADX WARN: Removed duplicated region for block: B:221:0x061f  */
    /* JADX WARN: Removed duplicated region for block: B:224:0x0630  */
    /* JADX WARN: Removed duplicated region for block: B:225:0x0636  */
    /* JADX WARN: Removed duplicated region for block: B:228:0x0686 A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:229:0x0687  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void appNotResponding(String activityShortComponentName, ApplicationInfo aInfo, String parentShortComponentName, WindowProcessController parentProcess, boolean aboveSystem, final String annotation, boolean onlyDumpSelf) {
        UUID errorId;
        long anrDialogDelayMs;
        String[] nativeProcs;
        AnrController anrController;
        ArrayList<Integer> nativePids;
        long anrDialogDelayMs2;
        long anrTime;
        long anrDialogDelayMs3;
        int i;
        ApplicationInfo applicationInfo;
        IncrementalMetrics incrementalMetrics;
        float loadingProgress;
        IncrementalMetrics incrementalMetrics2;
        String str;
        final ArrayList<Integer> firstPids = new ArrayList<>(5);
        final SparseArray<Boolean> lastPids = new SparseArray<>(20);
        this.mApp.getWindowProcessController().appEarlyNotResponding(annotation, new Runnable() { // from class: com.android.server.am.ProcessErrorStateRecord$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ProcessErrorStateRecord.this.m1465xd24e796c(annotation);
            }
        });
        long anrTime2 = SystemClock.uptimeMillis();
        if (isMonitorCpuUsage()) {
            this.mService.updateCpuStatsNow();
        }
        final int pid = this.mApp.getPid();
        boolean isSilentAnr = isSilentAnr();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                setAnrAnnotation(annotation);
            } catch (Throwable th) {
                th = th;
            }
            try {
                if (this.mService.mAtmInternal.isShuttingDown()) {
                    Slog.i("ActivityManager", "During shutdown skipping ANR: " + this + " " + annotation);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } else if (isNotResponding()) {
                    Slog.i("ActivityManager", "Skipping duplicate ANR: " + this + " " + annotation);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } else if (isCrashing()) {
                    Slog.i("ActivityManager", "Crashing app skipping ANR: " + this + " " + annotation);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } else if (this.mApp.isKilledByAm()) {
                    Slog.i("ActivityManager", "App already killed by AM skipping ANR: " + this + " " + annotation);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } else if (this.mApp.isKilled()) {
                    Slog.i("ActivityManager", "Skipping died app ANR: " + this + " " + annotation);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } else {
                    synchronized (this.mProcLock) {
                        try {
                            ActivityManagerService.boostPriorityForProcLockedSection();
                            setNotResponding(true);
                        }
                    }
                    ActivityManagerService.resetPriorityAfterProcLockedSection();
                    EventLog.writeEvent((int) EventLogTags.AM_ANR, Integer.valueOf(this.mApp.userId), Integer.valueOf(pid), this.mApp.processName, Integer.valueOf(this.mApp.info.flags), annotation);
                    if (this.mService.mTraceErrorLogger == null || !this.mService.mTraceErrorLogger.isAddErrorIdEnabled()) {
                        errorId = null;
                    } else {
                        UUID errorId2 = this.mService.mTraceErrorLogger.generateErrorId();
                        this.mService.mTraceErrorLogger.addErrorIdToTrace(this.mApp.processName, errorId2);
                        this.mService.mTraceErrorLogger.addSubjectToTrace(annotation, errorId2);
                        errorId = errorId2;
                    }
                    FrameworkStatsLog.write((int) FrameworkStatsLog.ANR_OCCURRED_PROCESSING_STARTED, this.mApp.processName);
                    firstPids.add(Integer.valueOf(pid));
                    if (!isSilentAnr && !onlyDumpSelf) {
                        int parentPid = pid;
                        if (parentProcess != null && parentProcess.getPid() > 0) {
                            parentPid = parentProcess.getPid();
                        }
                        if (parentPid != pid) {
                            firstPids.add(Integer.valueOf(parentPid));
                        }
                        if (ActivityManagerService.MY_PID != pid && ActivityManagerService.MY_PID != parentPid) {
                            firstPids.add(Integer.valueOf(ActivityManagerService.MY_PID));
                        }
                        final int ppid = parentPid;
                        this.mService.mProcessList.forEachLruProcessesLOSP(false, new Consumer() { // from class: com.android.server.am.ProcessErrorStateRecord$$ExternalSyntheticLambda1
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                ProcessErrorStateRecord.lambda$appNotResponding$1(pid, ppid, firstPids, lastPids, (ProcessRecord) obj);
                            }
                        });
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    String criticalEventLog = CriticalEventLog.getInstance().logLinesForTraceFile(this.mApp.getProcessClassEnum(), this.mApp.processName, this.mApp.uid);
                    CriticalEventLog.getInstance().logAnr(annotation, this.mApp.getProcessClassEnum(), this.mApp.processName, this.mApp.uid, this.mApp.mPid);
                    ProcessRecord parentPro = parentProcess != null ? (ProcessRecord) parentProcess.mOwner : null;
                    if (this.mService.mAnrManager.startAnrDump(this.mService, this, activityShortComponentName, aInfo, parentShortComponentName, parentPro, aboveSystem, annotation, isSilentAnr, anrTime2, onlyDumpSelf, errorId, criticalEventLog)) {
                        return;
                    }
                    StringBuilder info = new StringBuilder();
                    info.setLength(0);
                    info.append("ANR in ").append(this.mApp.processName);
                    if (activityShortComponentName != null) {
                        info.append(" (").append(activityShortComponentName).append(")");
                    }
                    info.append("\n");
                    info.append("PID: ").append(pid).append("\n");
                    if (annotation != null) {
                        info.append("Reason: ").append(annotation).append("\n");
                    }
                    if (parentShortComponentName != null && parentShortComponentName.equals(activityShortComponentName)) {
                        info.append("Parent: ").append(parentShortComponentName).append("\n");
                    }
                    if (errorId != null) {
                        info.append("ErrorId: ").append(errorId.toString()).append("\n");
                    }
                    info.append("Frozen: ").append(this.mApp.mOptRecord.isFrozen()).append("\n");
                    AnrController anrController2 = this.mService.mActivityTaskManager.getAnrController(aInfo);
                    if (anrController2 != null) {
                        String packageName = aInfo.packageName;
                        int uid = aInfo.uid;
                        long anrDialogDelayMs4 = anrController2.getAnrDelayMillis(packageName, uid);
                        anrController2.onAnrDelayStarted(packageName, uid);
                        Slog.i("ActivityManager", "ANR delay of " + anrDialogDelayMs4 + "ms started for " + packageName);
                        anrDialogDelayMs = anrDialogDelayMs4;
                    } else {
                        anrDialogDelayMs = 0;
                    }
                    StringBuilder report = new StringBuilder();
                    report.append(MemoryPressureUtil.currentPsiState());
                    ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(true);
                    String[] nativeProcs2 = null;
                    if (isSilentAnr || onlyDumpSelf) {
                        int i2 = 0;
                        while (true) {
                            if (i2 >= Watchdog.NATIVE_STACKS_OF_INTEREST.length) {
                                break;
                            }
                            String[] nativeProcs3 = nativeProcs2;
                            if (Watchdog.NATIVE_STACKS_OF_INTEREST[i2].equals(this.mApp.processName)) {
                                nativeProcs2 = new String[]{this.mApp.processName};
                                break;
                            } else {
                                i2++;
                                nativeProcs2 = nativeProcs3;
                            }
                        }
                        nativeProcs = nativeProcs2;
                    } else {
                        String[] nativeProcs4 = Watchdog.NATIVE_STACKS_OF_INTEREST;
                        nativeProcs = nativeProcs4;
                    }
                    int[] pids = nativeProcs == null ? null : Process.getPidsForCommands(nativeProcs);
                    if (pids != null) {
                        ArrayList<Integer> nativePids2 = new ArrayList<>(pids.length);
                        int length = pids.length;
                        int i3 = 0;
                        while (i3 < length) {
                            int i4 = pids[i3];
                            nativePids2.add(Integer.valueOf(i4));
                            i3++;
                            anrController2 = anrController2;
                        }
                        anrController = anrController2;
                        nativePids = nativePids2;
                    } else {
                        anrController = anrController2;
                        nativePids = null;
                    }
                    StringWriter tracesFileException = new StringWriter();
                    long[] offsets = new long[2];
                    File tracesFile = ActivityManagerService.dumpStackTraces(firstPids, isSilentAnr ? null : processCpuTracker, isSilentAnr ? null : lastPids, nativePids, tracesFileException, offsets, annotation, criticalEventLog);
                    if (isMonitorCpuUsage()) {
                        this.mService.updateCpuStatsNow();
                        anrDialogDelayMs2 = anrDialogDelayMs;
                        anrTime = anrTime2;
                        this.mService.mAppProfiler.printCurrentCpuState(report, anrTime);
                        info.append(processCpuTracker.printCurrentLoad());
                        info.append((CharSequence) report);
                    } else {
                        anrDialogDelayMs2 = anrDialogDelayMs;
                        anrTime = anrTime2;
                    }
                    report.append(tracesFileException.getBuffer());
                    info.append(processCpuTracker.printCurrentState(anrTime));
                    Slog.e("ActivityManager", info.toString());
                    if (tracesFile == null) {
                        Process.sendSignal(pid, 3);
                        anrDialogDelayMs3 = anrDialogDelayMs2;
                        i = 0;
                        applicationInfo = aInfo;
                    } else if (offsets[1] > 0) {
                        i = 0;
                        anrDialogDelayMs3 = anrDialogDelayMs2;
                        applicationInfo = aInfo;
                        this.mService.mProcessList.mAppExitInfoTracker.scheduleLogAnrTrace(pid, this.mApp.uid, this.mApp.getPackageList(), tracesFile, offsets[0], offsets[1]);
                    } else {
                        anrDialogDelayMs3 = anrDialogDelayMs2;
                        i = 0;
                        applicationInfo = aInfo;
                    }
                    PackageManagerInternal packageManagerInternal = this.mService.getPackageManagerInternal();
                    if (this.mApp.info == null || this.mApp.info.packageName == null || packageManagerInternal == null) {
                        incrementalMetrics = null;
                    } else {
                        IncrementalStatesInfo incrementalStatesInfo = packageManagerInternal.getIncrementalStatesInfo(this.mApp.info.packageName, 1000, this.mApp.userId);
                        loadingProgress = incrementalStatesInfo != null ? incrementalStatesInfo.getProgress() : 1.0f;
                        String codePath = this.mApp.info.getCodePath();
                        if (codePath == null || codePath.isEmpty()) {
                            incrementalMetrics = null;
                        } else if (IncrementalManager.isIncrementalPath(codePath)) {
                            incrementalMetrics = null;
                            Slog.e("ActivityManager", "App ANR on incremental package " + this.mApp.info.packageName + " which is " + ((int) (loadingProgress * 100.0f)) + "% loaded.");
                            IBinder incrementalService = ServiceManager.getService("incremental");
                            if (incrementalService != null) {
                                IncrementalManager incrementalManager = new IncrementalManager(IIncrementalService.Stub.asInterface(incrementalService));
                                loadingProgress = loadingProgress;
                                incrementalMetrics2 = incrementalManager.getMetrics(codePath);
                                if (incrementalMetrics2 != null) {
                                    info.append("Package is ").append((int) (loadingProgress * 100.0f)).append("% loaded.\n");
                                }
                                FrameworkStatsLog.write(79, this.mApp.uid, this.mApp.processName, activityShortComponentName != null ? UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN : activityShortComponentName, annotation, this.mApp.info == null ? this.mApp.info.isInstantApp() ? 2 : 1 : i, !this.mApp.isInterestingToUserLocked() ? 2 : 1, this.mApp.getProcessClassEnum(), this.mApp.info == null ? this.mApp.info.packageName : "", incrementalMetrics2 == null ? 1 : i, loadingProgress, incrementalMetrics2 == null ? incrementalMetrics2.getMillisSinceOldestPendingRead() : -1L, incrementalMetrics2 == null ? incrementalMetrics2.getStorageHealthStatusCode() : -1, incrementalMetrics2 == null ? incrementalMetrics2.getDataLoaderStatusCode() : -1, (incrementalMetrics2 == null && incrementalMetrics2.getReadLogsEnabled()) ? 1 : i, incrementalMetrics2 == null ? incrementalMetrics2.getMillisSinceLastDataLoaderBind() : -1L, incrementalMetrics2 == null ? incrementalMetrics2.getDataLoaderBindDelayMillis() : -1L, incrementalMetrics2 == null ? incrementalMetrics2.getTotalDelayedReads() : -1, incrementalMetrics2 == null ? incrementalMetrics2.getTotalFailedReads() : -1, incrementalMetrics2 == null ? incrementalMetrics2.getLastReadErrorUid() : -1, incrementalMetrics2 == null ? incrementalMetrics2.getMillisSinceLastReadError() : -1L, incrementalMetrics2 == null ? incrementalMetrics2.getLastReadErrorNumber() : i, incrementalMetrics2 == null ? incrementalMetrics2.getTotalDelayedReadsDurationMillis() : -1L);
                                ProcessRecord parentPr = parentProcess == null ? (ProcessRecord) parentProcess.mOwner : null;
                                ActivityManagerService activityManagerService = this.mService;
                                ProcessRecord processRecord = this.mApp;
                                AnrController anrController3 = anrController;
                                activityManagerService.addErrorToDropBox("anr", processRecord, processRecord.processName, activityShortComponentName, parentShortComponentName, parentPr, null, report.toString(), tracesFile, null, new Float(loadingProgress), incrementalMetrics2, errorId);
                                if (!this.mApp.getWindowProcessController().appNotResponding(info.toString(), new Runnable() { // from class: com.android.server.am.ProcessErrorStateRecord$$ExternalSyntheticLambda2
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        ProcessErrorStateRecord.this.m1466xbd42c5ee();
                                    }
                                }, new Runnable() { // from class: com.android.server.am.ProcessErrorStateRecord$$ExternalSyntheticLambda3
                                    @Override // java.lang.Runnable
                                    public final void run() {
                                        ProcessErrorStateRecord.this.m1467x32bcec2f();
                                    }
                                })) {
                                    return;
                                }
                                synchronized (this.mService) {
                                    try {
                                        try {
                                            ActivityManagerService.boostPriorityForLockedSection();
                                            if (this.mService.mBatteryStatsService != null) {
                                                try {
                                                    this.mService.mBatteryStatsService.noteProcessAnr(this.mApp.processName, this.mApp.uid);
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                            }
                                            if (isSilentAnr() && !this.mApp.isDebugging()) {
                                                this.mApp.killLocked("bg anr", 6, true);
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                                return;
                                            }
                                            synchronized (this.mProcLock) {
                                                try {
                                                    ActivityManagerService.boostPriorityForProcLockedSection();
                                                    if (annotation != null) {
                                                        try {
                                                            str = "ANR " + annotation;
                                                        } catch (Throwable th3) {
                                                            th = th3;
                                                            while (true) {
                                                                try {
                                                                    break;
                                                                } catch (Throwable th4) {
                                                                    th = th4;
                                                                }
                                                            }
                                                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                            throw th;
                                                        }
                                                    } else {
                                                        str = "ANR";
                                                    }
                                                    makeAppNotRespondingLSP(activityShortComponentName, str, info.toString());
                                                    try {
                                                        this.mDialogController.setAnrController(anrController3);
                                                        try {
                                                            ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                            if (this.mService.mUiHandler != null) {
                                                                Message msg = Message.obtain();
                                                                msg.what = 2;
                                                                try {
                                                                    msg.obj = new AppNotRespondingDialog.Data(this.mApp, applicationInfo, aboveSystem);
                                                                    this.mService.mUiHandler.sendMessageDelayed(msg, anrDialogDelayMs3);
                                                                } catch (Throwable th5) {
                                                                    th = th5;
                                                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                                                    throw th;
                                                                }
                                                            }
                                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                                            return;
                                                        } catch (Throwable th6) {
                                                            th = th6;
                                                        }
                                                    } catch (Throwable th7) {
                                                        th = th7;
                                                        while (true) {
                                                            break;
                                                            break;
                                                        }
                                                        ActivityManagerService.resetPriorityAfterProcLockedSection();
                                                        throw th;
                                                    }
                                                } catch (Throwable th8) {
                                                    th = th8;
                                                }
                                            }
                                        } catch (Throwable th9) {
                                            th = th9;
                                        }
                                    } catch (Throwable th10) {
                                        th = th10;
                                    }
                                }
                            }
                        } else {
                            incrementalMetrics = null;
                        }
                    }
                    loadingProgress = loadingProgress;
                    incrementalMetrics2 = incrementalMetrics;
                    if (incrementalMetrics2 != null) {
                    }
                    FrameworkStatsLog.write(79, this.mApp.uid, this.mApp.processName, activityShortComponentName != null ? UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN : activityShortComponentName, annotation, this.mApp.info == null ? this.mApp.info.isInstantApp() ? 2 : 1 : i, !this.mApp.isInterestingToUserLocked() ? 2 : 1, this.mApp.getProcessClassEnum(), this.mApp.info == null ? this.mApp.info.packageName : "", incrementalMetrics2 == null ? 1 : i, loadingProgress, incrementalMetrics2 == null ? incrementalMetrics2.getMillisSinceOldestPendingRead() : -1L, incrementalMetrics2 == null ? incrementalMetrics2.getStorageHealthStatusCode() : -1, incrementalMetrics2 == null ? incrementalMetrics2.getDataLoaderStatusCode() : -1, (incrementalMetrics2 == null && incrementalMetrics2.getReadLogsEnabled()) ? 1 : i, incrementalMetrics2 == null ? incrementalMetrics2.getMillisSinceLastDataLoaderBind() : -1L, incrementalMetrics2 == null ? incrementalMetrics2.getDataLoaderBindDelayMillis() : -1L, incrementalMetrics2 == null ? incrementalMetrics2.getTotalDelayedReads() : -1, incrementalMetrics2 == null ? incrementalMetrics2.getTotalFailedReads() : -1, incrementalMetrics2 == null ? incrementalMetrics2.getLastReadErrorUid() : -1, incrementalMetrics2 == null ? incrementalMetrics2.getMillisSinceLastReadError() : -1L, incrementalMetrics2 == null ? incrementalMetrics2.getLastReadErrorNumber() : i, incrementalMetrics2 == null ? incrementalMetrics2.getTotalDelayedReadsDurationMillis() : -1L);
                    if (parentProcess == null) {
                    }
                    ActivityManagerService activityManagerService2 = this.mService;
                    ProcessRecord processRecord2 = this.mApp;
                    AnrController anrController32 = anrController;
                    activityManagerService2.addErrorToDropBox("anr", processRecord2, processRecord2.processName, activityShortComponentName, parentShortComponentName, parentPr, null, report.toString(), tracesFile, null, new Float(loadingProgress), incrementalMetrics2, errorId);
                    if (!this.mApp.getWindowProcessController().appNotResponding(info.toString(), new Runnable() { // from class: com.android.server.am.ProcessErrorStateRecord$$ExternalSyntheticLambda2
                        @Override // java.lang.Runnable
                        public final void run() {
                            ProcessErrorStateRecord.this.m1466xbd42c5ee();
                        }
                    }, new Runnable() { // from class: com.android.server.am.ProcessErrorStateRecord$$ExternalSyntheticLambda3
                        @Override // java.lang.Runnable
                        public final void run() {
                            ProcessErrorStateRecord.this.m1467x32bcec2f();
                        }
                    })) {
                    }
                }
            } catch (Throwable th11) {
                th = th11;
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$appNotResponding$0$com-android-server-am-ProcessErrorStateRecord  reason: not valid java name */
    public /* synthetic */ void m1465xd24e796c(String annotation) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                setAnrAnnotation(annotation);
                this.mApp.killLocked("anr", 6, true);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$appNotResponding$1(int pid, int ppid, ArrayList firstPids, SparseArray lastPids, ProcessRecord r) {
        int myPid;
        if (r != null && r.getThread() != null && (myPid = r.getPid()) > 0 && myPid != pid && myPid != ppid && myPid != ActivityManagerService.MY_PID) {
            if (r.isPersistent()) {
                firstPids.add(Integer.valueOf(myPid));
                if (ActivityManagerDebugConfig.DEBUG_ANR) {
                    Slog.i("ActivityManager", "Adding persistent proc: " + r);
                }
            } else if (r.mServices.isTreatedLikeActivity()) {
                firstPids.add(Integer.valueOf(myPid));
                if (ActivityManagerDebugConfig.DEBUG_ANR) {
                    Slog.i("ActivityManager", "Adding likely IME: " + r);
                }
            } else {
                lastPids.put(myPid, Boolean.TRUE);
                if (ActivityManagerDebugConfig.DEBUG_ANR) {
                    Slog.i("ActivityManager", "Adding ANR proc: " + r);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$appNotResponding$2$com-android-server-am-ProcessErrorStateRecord  reason: not valid java name */
    public /* synthetic */ void m1466xbd42c5ee() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mApp.killLocked("anr", 6, true);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$appNotResponding$3$com-android-server-am-ProcessErrorStateRecord  reason: not valid java name */
    public /* synthetic */ void m1467x32bcec2f() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.mServices.scheduleServiceTimeoutLocked(this.mApp);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    private void makeAppNotRespondingLSP(String activity, String shortMsg, String longMsg) {
        setNotResponding(true);
        if (this.mService.mAppErrors != null) {
            this.mNotRespondingReport = this.mService.mAppErrors.generateProcessError(this.mApp, 2, activity, shortMsg, longMsg, null);
        }
        startAppProblemLSP();
        this.mApp.getWindowProcessController().stopFreezingActivities();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAppProblemLSP() {
        int[] currentProfileIds;
        this.mErrorReportReceiver = null;
        for (int userId : this.mService.mUserController.getCurrentProfileIds()) {
            if (this.mApp.userId == userId) {
                this.mErrorReportReceiver = ApplicationErrorReport.getErrorReportReceiver(this.mService.mContext, this.mApp.info.packageName, this.mApp.info.flags);
            }
        }
        this.mService.skipCurrentReceiverLocked(this.mApp);
    }

    private boolean isInterestingForBackgroundTraces() {
        if (this.mApp.getPid() == ActivityManagerService.MY_PID || this.mApp.isInterestingToUserLocked()) {
            return true;
        }
        return (this.mApp.info != null && "com.android.systemui".equals(this.mApp.info.packageName)) || this.mApp.mState.hasTopUi() || this.mApp.mState.hasOverlayUi();
    }

    private boolean getShowBackground() {
        return Settings.Secure.getInt(this.mService.mContext.getContentResolver(), "anr_show_background", 0) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSilentAnr() {
        return (getShowBackground() || isInterestingForBackgroundTraces()) ? false : true;
    }

    boolean isMonitorCpuUsage() {
        AppProfiler appProfiler = this.mService.mAppProfiler;
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCleanupApplicationRecordLSP() {
        getDialogController().clearAllErrorDialogs();
        setCrashing(false);
        setNotResponding(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix, long nowUptime) {
        synchronized (this.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                if (this.mCrashing || this.mDialogController.hasCrashDialogs() || this.mNotResponding || this.mDialogController.hasAnrDialogs() || this.mBad) {
                    pw.print(prefix);
                    pw.print(" mCrashing=" + this.mCrashing);
                    pw.print(" " + this.mDialogController.getCrashDialogs());
                    pw.print(" mNotResponding=" + this.mNotResponding);
                    pw.print(" " + this.mDialogController.getAnrDialogs());
                    pw.print(" bad=" + this.mBad);
                    if (this.mErrorReportReceiver != null) {
                        pw.print(" errorReportReceiver=");
                        pw.print(this.mErrorReportReceiver.flattenToShortString());
                    }
                    pw.println();
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
    }
}
