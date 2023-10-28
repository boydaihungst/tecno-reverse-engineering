package com.android.server.am;

import android.os.Handler;
import android.os.MessageQueue;
import android.os.Process;
import android.os.StrictMode;
import android.util.FeatureFlagUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ProcStatsUtil;
import com.android.internal.os.ProcessCpuTracker;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public final class PhantomProcessList {
    private static final String[] CGROUP_PATH_PREFIXES = {"/acct/uid_", "/sys/fs/cgroup/uid_"};
    private static final String CGROUP_PID_PREFIX = "/pid_";
    private static final String CGROUP_PROCS = "/cgroup.procs";
    private static final int CGROUP_V1 = 0;
    private static final int CGROUP_V2 = 1;
    static final String TAG = "ActivityManager";
    Injector mInjector;
    private final Handler mKillHandler;
    private final ActivityManagerService mService;
    int mUpdateSeq;
    final Object mLock = new Object();
    final SparseArray<PhantomProcessRecord> mPhantomProcesses = new SparseArray<>();
    final SparseArray<SparseArray<PhantomProcessRecord>> mAppPhantomProcessMap = new SparseArray<>();
    final SparseArray<PhantomProcessRecord> mPhantomProcessesPidFds = new SparseArray<>();
    final SparseArray<PhantomProcessRecord> mZombiePhantomProcesses = new SparseArray<>();
    private final ArrayList<PhantomProcessRecord> mTempPhantomProcesses = new ArrayList<>();
    private final SparseArray<ProcessRecord> mPhantomToAppProcessMap = new SparseArray<>();
    private final SparseArray<InputStream> mCgroupProcsFds = new SparseArray<>();
    private final byte[] mDataBuffer = new byte[4096];
    private boolean mTrimPhantomProcessScheduled = false;
    int mCgroupVersion = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PhantomProcessList(ActivityManagerService service) {
        this.mService = service;
        ProcessList processList = service.mProcessList;
        this.mKillHandler = ProcessList.sKillHandler;
        this.mInjector = new Injector();
        probeCgroupVersion();
    }

    void lookForPhantomProcessesLocked() {
        this.mPhantomToAppProcessMap.clear();
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            synchronized (this.mService.mPidsSelfLocked) {
                for (int i = this.mService.mPidsSelfLocked.size() - 1; i >= 0; i--) {
                    ProcessRecord app = this.mService.mPidsSelfLocked.valueAt(i);
                    lookForPhantomProcessesLocked(app);
                }
            }
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void lookForPhantomProcessesLocked(ProcessRecord app) {
        int read;
        int i;
        if (app.appZygote || app.isKilled() || app.isKilledByAm()) {
            return;
        }
        int appPid = app.getPid();
        InputStream input = this.mCgroupProcsFds.get(appPid);
        if (input == null) {
            String path = getCgroupFilePath(app.info.uid, appPid);
            try {
                input = this.mInjector.openCgroupProcs(path);
                this.mCgroupProcsFds.put(appPid, input);
            } catch (FileNotFoundException | SecurityException e) {
                if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.w(TAG, "Unable to open " + path, e);
                    return;
                }
                return;
            }
        }
        byte[] buf = this.mDataBuffer;
        int pid = 0;
        long totalRead = 0;
        do {
            try {
                read = this.mInjector.readCgroupProcs(input, buf, 0, buf.length);
                if (read == -1) {
                    break;
                }
                totalRead += read;
                for (int i2 = 0; i2 < read; i2++) {
                    byte b = buf[i2];
                    if (b == 10) {
                        addChildPidLocked(app, pid, appPid);
                        pid = 0;
                    } else {
                        pid = (pid * 10) + (b - 48);
                    }
                }
                i = buf.length;
            } catch (IOException e2) {
                Slog.e(TAG, "Error in reading cgroup procs from " + app, e2);
                IoUtils.closeQuietly(input);
                this.mCgroupProcsFds.delete(appPid);
                return;
            }
        } while (read >= i);
        if (pid != 0) {
            addChildPidLocked(app, pid, appPid);
        }
        input.skip(-totalRead);
    }

    private void probeCgroupVersion() {
        for (int i = CGROUP_PATH_PREFIXES.length - 1; i >= 0; i--) {
            if (new File(CGROUP_PATH_PREFIXES[i] + 1000).exists()) {
                this.mCgroupVersion = i;
                return;
            }
        }
    }

    String getCgroupFilePath(int uid, int pid) {
        return CGROUP_PATH_PREFIXES[this.mCgroupVersion] + uid + CGROUP_PID_PREFIX + pid + CGROUP_PROCS;
    }

    static String getProcessName(int pid) {
        String procName = ProcStatsUtil.readTerminatedProcFile("/proc/" + pid + "/cmdline", (byte) 0);
        if (procName == null) {
            return null;
        }
        int l = procName.lastIndexOf(47);
        if (l > 0 && l < procName.length() - 1) {
            return procName.substring(l + 1);
        }
        return procName;
    }

    private void addChildPidLocked(ProcessRecord app, int pid, int appPid) {
        if (appPid != pid) {
            ProcessRecord r = this.mService.mPidsSelfLocked.get(pid);
            if (r != null) {
                if (!r.appZygote && ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.w(TAG, "Unexpected: " + r + " appears in the cgroup.procs of " + app);
                    return;
                }
                return;
            }
            int index = this.mPhantomToAppProcessMap.indexOfKey(pid);
            if (index >= 0) {
                ProcessRecord current = this.mPhantomToAppProcessMap.valueAt(index);
                if (app == current) {
                    return;
                }
                this.mPhantomToAppProcessMap.setValueAt(index, app);
            } else {
                this.mPhantomToAppProcessMap.put(pid, app);
            }
            int uid = Process.getUidForPid(pid);
            String procName = this.mInjector.getProcessName(pid);
            if (procName == null || uid < 0) {
                this.mPhantomToAppProcessMap.delete(pid);
            } else {
                getOrCreatePhantomProcessIfNeededLocked(procName, uid, pid, true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppDied(int pid) {
        synchronized (this.mLock) {
            int index = this.mCgroupProcsFds.indexOfKey(pid);
            if (index >= 0) {
                InputStream inputStream = this.mCgroupProcsFds.valueAt(index);
                this.mCgroupProcsFds.removeAt(index);
                IoUtils.closeQuietly(inputStream);
            }
        }
    }

    PhantomProcessRecord getOrCreatePhantomProcessIfNeededLocked(String processName, int uid, int pid, boolean createIfNeeded) {
        ProcessRecord r;
        if (isAppProcess(pid)) {
            return null;
        }
        int index = this.mPhantomProcesses.indexOfKey(pid);
        if (index >= 0) {
            PhantomProcessRecord proc = this.mPhantomProcesses.valueAt(index);
            if (proc.equals(processName, uid, pid)) {
                return proc;
            }
            Slog.w(TAG, "Stale " + proc + ", removing");
            onPhantomProcessKilledLocked(proc);
        } else {
            int idx = this.mZombiePhantomProcesses.indexOfKey(pid);
            if (idx >= 0) {
                PhantomProcessRecord proc2 = this.mZombiePhantomProcesses.valueAt(idx);
                if (proc2.equals(processName, uid, pid)) {
                    return proc2;
                }
                this.mZombiePhantomProcesses.removeAt(idx);
            }
        }
        if (createIfNeeded && (r = this.mPhantomToAppProcessMap.get(pid)) != null) {
            try {
                int appPid = r.getPid();
                PhantomProcessRecord proc3 = new PhantomProcessRecord(processName, uid, pid, appPid, this.mService, new Consumer() { // from class: com.android.server.am.PhantomProcessList$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        PhantomProcessList.this.onPhantomProcessKilledLocked((PhantomProcessRecord) obj);
                    }
                });
                proc3.mUpdateSeq = this.mUpdateSeq;
                this.mPhantomProcesses.put(pid, proc3);
                SparseArray<PhantomProcessRecord> array = this.mAppPhantomProcessMap.get(appPid);
                if (array == null) {
                    array = new SparseArray<>();
                    this.mAppPhantomProcessMap.put(appPid, array);
                }
                array.put(pid, proc3);
                if (proc3.mPidFd != null) {
                    this.mKillHandler.getLooper().getQueue().addOnFileDescriptorEventListener(proc3.mPidFd, 5, new MessageQueue.OnFileDescriptorEventListener() { // from class: com.android.server.am.PhantomProcessList$$ExternalSyntheticLambda1
                        @Override // android.os.MessageQueue.OnFileDescriptorEventListener
                        public final int onFileDescriptorEvents(FileDescriptor fileDescriptor, int i) {
                            int onPhantomProcessFdEvent;
                            onPhantomProcessFdEvent = PhantomProcessList.this.onPhantomProcessFdEvent(fileDescriptor, i);
                            return onPhantomProcessFdEvent;
                        }
                    });
                    this.mPhantomProcessesPidFds.put(proc3.mPidFd.getInt$(), proc3);
                }
                scheduleTrimPhantomProcessesLocked();
                return proc3;
            } catch (IllegalStateException e) {
                return null;
            }
        }
        return null;
    }

    private boolean isAppProcess(int pid) {
        boolean z;
        synchronized (this.mService.mPidsSelfLocked) {
            z = this.mService.mPidsSelfLocked.get(pid) != null;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int onPhantomProcessFdEvent(FileDescriptor fd, int events) {
        synchronized (this.mLock) {
            PhantomProcessRecord proc = this.mPhantomProcessesPidFds.get(fd.getInt$());
            if (proc == null) {
                return 0;
            }
            if ((events & 1) != 0) {
                proc.onProcDied(true);
            } else {
                proc.killLocked("Process error", true);
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPhantomProcessKilledLocked(PhantomProcessRecord proc) {
        if (proc.mPidFd != null && proc.mPidFd.valid()) {
            this.mKillHandler.getLooper().getQueue().removeOnFileDescriptorEventListener(proc.mPidFd);
            this.mPhantomProcessesPidFds.remove(proc.mPidFd.getInt$());
            IoUtils.closeQuietly(proc.mPidFd);
        }
        this.mPhantomProcesses.remove(proc.mPid);
        int index = this.mAppPhantomProcessMap.indexOfKey(proc.mPpid);
        if (index < 0) {
            return;
        }
        SparseArray<PhantomProcessRecord> array = this.mAppPhantomProcessMap.valueAt(index);
        array.remove(proc.mPid);
        if (array.size() == 0) {
            this.mAppPhantomProcessMap.removeAt(index);
        }
        if (proc.mZombie) {
            this.mZombiePhantomProcesses.put(proc.mPid, proc);
        } else {
            this.mZombiePhantomProcesses.remove(proc.mPid);
        }
    }

    private void scheduleTrimPhantomProcessesLocked() {
        if (!this.mTrimPhantomProcessScheduled) {
            this.mTrimPhantomProcessScheduled = true;
            this.mService.mHandler.post(new ActivityManagerConstants$$ExternalSyntheticLambda2(this));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void trimPhantomProcessesIfNecessary() {
        if (!this.mService.mSystemReady || !FeatureFlagUtils.isEnabled(this.mService.mContext, "settings_enable_monitor_phantom_procs")) {
            return;
        }
        synchronized (this.mService.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                synchronized (this.mLock) {
                    this.mTrimPhantomProcessScheduled = false;
                    if (this.mService.mConstants.MAX_PHANTOM_PROCESSES < this.mPhantomProcesses.size()) {
                        for (int i = this.mPhantomProcesses.size() - 1; i >= 0; i--) {
                            this.mTempPhantomProcesses.add(this.mPhantomProcesses.valueAt(i));
                        }
                        synchronized (this.mService.mPidsSelfLocked) {
                            Collections.sort(this.mTempPhantomProcesses, new Comparator() { // from class: com.android.server.am.PhantomProcessList$$ExternalSyntheticLambda2
                                @Override // java.util.Comparator
                                public final int compare(Object obj, Object obj2) {
                                    return PhantomProcessList.this.m1462x1af897f6((PhantomProcessRecord) obj, (PhantomProcessRecord) obj2);
                                }
                            });
                        }
                        for (int i2 = this.mTempPhantomProcesses.size() - 1; i2 >= this.mService.mConstants.MAX_PHANTOM_PROCESSES; i2--) {
                            PhantomProcessRecord proc = this.mTempPhantomProcesses.get(i2);
                            proc.killLocked("Trimming phantom processes", true);
                        }
                        this.mTempPhantomProcesses.clear();
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$trimPhantomProcessesIfNecessary$0$com-android-server-am-PhantomProcessList  reason: not valid java name */
    public /* synthetic */ int m1462x1af897f6(PhantomProcessRecord a, PhantomProcessRecord b) {
        ProcessRecord ra = this.mService.mPidsSelfLocked.get(a.mPpid);
        ProcessRecord rb = this.mService.mPidsSelfLocked.get(b.mPpid);
        if (ra == null && rb == null) {
            return 0;
        }
        if (ra == null) {
            return 1;
        }
        if (rb == null) {
            return -1;
        }
        if (ra.mState.getCurAdj() != rb.mState.getCurAdj()) {
            return ra.mState.getCurAdj() - rb.mState.getCurAdj();
        }
        if (a.mKnownSince == b.mKnownSince) {
            return 0;
        }
        if (a.mKnownSince < b.mKnownSince) {
            return 1;
        }
        return -1;
    }

    void pruneStaleProcessesLocked() {
        for (int i = this.mPhantomProcesses.size() - 1; i >= 0; i--) {
            PhantomProcessRecord proc = this.mPhantomProcesses.valueAt(i);
            if (proc.mUpdateSeq < this.mUpdateSeq) {
                if (ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                    Slog.v(TAG, "Pruning " + proc + " as it should have been dead.");
                }
                proc.killLocked("Stale process", true);
            }
        }
        for (int i2 = this.mZombiePhantomProcesses.size() - 1; i2 >= 0; i2--) {
            PhantomProcessRecord proc2 = this.mZombiePhantomProcesses.valueAt(i2);
            if (proc2.mUpdateSeq < this.mUpdateSeq && ActivityManagerDebugConfig.DEBUG_PROCESSES) {
                Slog.v(TAG, "Pruning " + proc2 + " as it should have been dead.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killPhantomProcessGroupLocked(ProcessRecord app, PhantomProcessRecord proc, int reasonCode, int subReason, String msg) {
        synchronized (this.mLock) {
            int index = this.mAppPhantomProcessMap.indexOfKey(proc.mPpid);
            if (index >= 0) {
                SparseArray<PhantomProcessRecord> array = this.mAppPhantomProcessMap.valueAt(index);
                for (int i = array.size() - 1; i >= 0; i--) {
                    PhantomProcessRecord r = array.valueAt(i);
                    if (r == proc) {
                        r.killLocked(msg, true);
                    } else {
                        r.killLocked("Caused by siling process: " + msg, false);
                    }
                }
            }
        }
        app.killLocked("Caused by child process: " + msg, reasonCode, subReason, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachPhantomProcessOfApp(ProcessRecord app, Function<PhantomProcessRecord, Boolean> callback) {
        synchronized (this.mLock) {
            int index = this.mAppPhantomProcessMap.indexOfKey(app.getPid());
            if (index >= 0) {
                SparseArray<PhantomProcessRecord> array = this.mAppPhantomProcessMap.valueAt(index);
                for (int i = array.size() - 1; i >= 0; i--) {
                    PhantomProcessRecord r = array.valueAt(i);
                    if (!callback.apply(r).booleanValue()) {
                        break;
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateProcessCpuStatesLocked(ProcessCpuTracker tracker) {
        synchronized (this.mLock) {
            this.mUpdateSeq++;
            lookForPhantomProcessesLocked();
            for (int i = tracker.countStats() - 1; i >= 0; i--) {
                ProcessCpuTracker.Stats st = tracker.getStats(i);
                PhantomProcessRecord r = getOrCreatePhantomProcessIfNeededLocked(st.name, st.uid, st.pid, false);
                if (r != null) {
                    r.mUpdateSeq = this.mUpdateSeq;
                    r.mCurrentCputime += st.rel_utime + st.rel_stime;
                    if (r.mLastCputime == 0) {
                        r.mLastCputime = r.mCurrentCputime;
                    }
                    r.updateAdjLocked();
                }
            }
            pruneStaleProcessesLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        synchronized (this.mLock) {
            dumpPhantomeProcessLocked(pw, prefix, "All Active App Child Processes:", this.mPhantomProcesses);
            dumpPhantomeProcessLocked(pw, prefix, "All Zombie App Child Processes:", this.mZombiePhantomProcesses);
        }
    }

    void dumpPhantomeProcessLocked(PrintWriter pw, String prefix, String headline, SparseArray<PhantomProcessRecord> list) {
        int size = list.size();
        if (size == 0) {
            return;
        }
        pw.println();
        pw.print(prefix);
        pw.println(headline);
        for (int i = 0; i < size; i++) {
            PhantomProcessRecord proc = list.valueAt(i);
            pw.print(prefix);
            pw.print("  proc #");
            pw.print(i);
            pw.print(": ");
            pw.println(proc.toString());
            proc.dump(pw, prefix + "    ");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        Injector() {
        }

        InputStream openCgroupProcs(String path) throws FileNotFoundException, SecurityException {
            return new FileInputStream(path);
        }

        int readCgroupProcs(InputStream input, byte[] buf, int offset, int len) throws IOException {
            return input.read(buf, offset, len);
        }

        String getProcessName(int pid) {
            return PhantomProcessList.getProcessName(pid);
        }
    }
}
