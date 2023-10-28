package com.android.server.am;

import android.os.Binder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.procstats.DumpUtils;
import com.android.internal.app.procstats.IProcessStats;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.app.procstats.ProcessStatsInternal;
import com.android.internal.app.procstats.ServiceState;
import com.android.internal.app.procstats.UidState;
import com.android.internal.os.BackgroundThread;
import com.android.server.LocalServices;
import dalvik.annotation.optimization.NeverCompile;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
/* loaded from: classes.dex */
public final class ProcessStatsService extends IProcessStats.Stub {
    static final boolean DEBUG = false;
    static final int MAX_HISTORIC_STATES = 8;
    static final String STATE_FILE_CHECKIN_SUFFIX = ".ci";
    static final String STATE_FILE_PREFIX = "state-v2-";
    static final String STATE_FILE_SUFFIX = ".bin";
    static final String TAG = "ProcessStatsService";
    static long WRITE_PERIOD = 1800000;
    final ActivityManagerService mAm;
    final File mBaseDir;
    boolean mCommitPending;
    AtomicFile mFile;
    final ReentrantLock mFileLock;
    Boolean mInjectedScreenState;
    int mLastMemOnlyState;
    long mLastWriteTime;
    final Object mLock;
    boolean mMemFactorLowered;
    Parcel mPendingWrite;
    boolean mPendingWriteCommitted;
    AtomicFile mPendingWriteFile;
    final Object mPendingWriteLock;
    final ProcessStats mProcessStats;
    boolean mShuttingDown;

    public ProcessStatsService(ActivityManagerService am, File file) {
        Object obj = new Object();
        this.mLock = obj;
        this.mPendingWriteLock = new Object();
        this.mFileLock = new ReentrantLock();
        this.mLastMemOnlyState = -1;
        this.mAm = am;
        this.mBaseDir = file;
        file.mkdirs();
        synchronized (obj) {
            this.mProcessStats = new ProcessStats(true);
            updateFileLocked();
        }
        SystemProperties.addChangeCallback(new Runnable() { // from class: com.android.server.am.ProcessStatsService.1
            @Override // java.lang.Runnable
            public void run() {
                synchronized (ProcessStatsService.this.mLock) {
                    if (ProcessStatsService.this.mProcessStats.evaluateSystemProperties(false)) {
                        ProcessStatsService.this.mProcessStats.mFlags |= 4;
                        ProcessStatsService.this.writeStateLocked(true, true);
                        ProcessStatsService.this.mProcessStats.evaluateSystemProperties(true);
                    }
                }
            }
        });
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Process Stats Crash", e);
            }
            throw e;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateProcessStateHolderLocked(ProcessStats.ProcessStateHolder holder, String packageName, int uid, long versionCode, String processName) {
        holder.pkg = this.mProcessStats.getPackageStateLocked(packageName, uid, versionCode);
        holder.state = this.mProcessStats.getProcessStateLocked(holder.pkg, processName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProcessState getProcessStateLocked(String packageName, int uid, long versionCode, String processName) {
        return this.mProcessStats.getProcessStateLocked(packageName, uid, versionCode, processName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ServiceState getServiceState(String packageName, int uid, long versionCode, String processName, String className) {
        ServiceState serviceStateLocked;
        synchronized (this.mLock) {
            serviceStateLocked = this.mProcessStats.getServiceStateLocked(packageName, uid, versionCode, processName, className);
        }
        return serviceStateLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isMemFactorLowered() {
        return this.mMemFactorLowered;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setMemFactorLocked(int memFactor, boolean screenOn, long now) {
        this.mMemFactorLowered = memFactor < this.mLastMemOnlyState;
        this.mLastMemOnlyState = memFactor;
        Boolean bool = this.mInjectedScreenState;
        if (bool != null) {
            screenOn = bool.booleanValue();
        }
        if (screenOn) {
            memFactor += 4;
        }
        if (memFactor != this.mProcessStats.mMemFactor) {
            if (this.mProcessStats.mMemFactor != -1) {
                long[] jArr = this.mProcessStats.mMemFactorDurations;
                int i = this.mProcessStats.mMemFactor;
                jArr[i] = jArr[i] + (now - this.mProcessStats.mStartTime);
            }
            this.mProcessStats.mMemFactor = memFactor;
            this.mProcessStats.mStartTime = now;
            ArrayMap<String, SparseArray<LongSparseArray<ProcessStats.PackageState>>> pmap = this.mProcessStats.mPackages.getMap();
            for (int ipkg = pmap.size() - 1; ipkg >= 0; ipkg--) {
                SparseArray<LongSparseArray<ProcessStats.PackageState>> uids = pmap.valueAt(ipkg);
                for (int iuid = uids.size() - 1; iuid >= 0; iuid--) {
                    LongSparseArray<ProcessStats.PackageState> vers = uids.valueAt(iuid);
                    for (int iver = vers.size() - 1; iver >= 0; iver--) {
                        ProcessStats.PackageState pkg = vers.valueAt(iver);
                        ArrayMap<String, ServiceState> services = pkg.mServices;
                        for (int isvc = services.size() - 1; isvc >= 0; isvc--) {
                            ServiceState service = services.valueAt(isvc);
                            service.setMemFactor(memFactor, now);
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMemFactorLocked() {
        if (this.mProcessStats.mMemFactor != -1) {
            return this.mProcessStats.mMemFactor;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addSysMemUsageLocked(long cachedMem, long freeMem, long zramMem, long kernelMem, long nativeMem) {
        this.mProcessStats.addSysMemUsage(cachedMem, freeMem, zramMem, kernelMem, nativeMem);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTrackingAssociationsLocked(int curSeq, long now) {
        this.mProcessStats.updateTrackingAssociationsLocked(curSeq, now);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldWriteNowLocked(long now) {
        if (now > this.mLastWriteTime + WRITE_PERIOD) {
            if (SystemClock.elapsedRealtime() > this.mProcessStats.mTimePeriodStartRealtime + ProcessStats.COMMIT_PERIOD && SystemClock.uptimeMillis() > this.mProcessStats.mTimePeriodStartUptime + ProcessStats.COMMIT_UPTIME_PERIOD) {
                this.mCommitPending = true;
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void shutdown() {
        Slog.w(TAG, "Writing process stats before shutdown...");
        synchronized (this.mLock) {
            this.mProcessStats.mFlags |= 2;
            writeStateSyncLocked();
            this.mShuttingDown = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeStateAsync() {
        synchronized (this.mLock) {
            writeStateLocked(false);
        }
    }

    private void writeStateSyncLocked() {
        writeStateLocked(true);
    }

    private void writeStateLocked(boolean sync) {
        if (this.mShuttingDown) {
            return;
        }
        boolean commitPending = this.mCommitPending;
        this.mCommitPending = false;
        writeStateLocked(sync, commitPending);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeStateLocked(boolean sync, boolean commit) {
        synchronized (this.mPendingWriteLock) {
            long now = SystemClock.uptimeMillis();
            if (this.mPendingWrite == null || !this.mPendingWriteCommitted) {
                this.mPendingWrite = Parcel.obtain();
                this.mProcessStats.mTimePeriodEndRealtime = SystemClock.elapsedRealtime();
                this.mProcessStats.mTimePeriodEndUptime = now;
                if (commit) {
                    this.mProcessStats.mFlags |= 1;
                }
                this.mProcessStats.writeToParcel(this.mPendingWrite, 0);
                this.mPendingWriteFile = new AtomicFile(getCurrentFile());
                this.mPendingWriteCommitted = commit;
            }
            if (commit) {
                this.mProcessStats.resetSafely();
                updateFileLocked();
                scheduleRequestPssAllProcs(true, false);
            }
            this.mLastWriteTime = SystemClock.uptimeMillis();
            final long totalTime = SystemClock.uptimeMillis() - now;
            if (!sync) {
                BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.am.ProcessStatsService.2
                    @Override // java.lang.Runnable
                    public void run() {
                        ProcessStatsService.this.performWriteState(totalTime);
                    }
                });
            } else {
                performWriteState(totalTime);
            }
        }
    }

    private void scheduleRequestPssAllProcs(final boolean always, final boolean memLowered) {
        this.mAm.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessStatsService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ProcessStatsService.this.m1497xdd56377d(always, memLowered);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleRequestPssAllProcs$0$com-android-server-am-ProcessStatsService  reason: not valid java name */
    public /* synthetic */ void m1497xdd56377d(boolean always, boolean memLowered) {
        synchronized (this.mAm.mProcLock) {
            try {
                ActivityManagerService.boostPriorityForProcLockedSection();
                this.mAm.mAppProfiler.requestPssAllProcsLPr(SystemClock.uptimeMillis(), always, memLowered);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterProcLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterProcLockedSection();
    }

    private void updateFileLocked() {
        this.mFileLock.lock();
        try {
            this.mFile = new AtomicFile(new File(this.mBaseDir, STATE_FILE_PREFIX + this.mProcessStats.mTimePeriodStartClockStr + STATE_FILE_SUFFIX));
            this.mFileLock.unlock();
            this.mLastWriteTime = SystemClock.uptimeMillis();
        } catch (Throwable th) {
            this.mFileLock.unlock();
            throw th;
        }
    }

    private File getCurrentFile() {
        this.mFileLock.lock();
        try {
            return this.mFile.getBaseFile();
        } finally {
            this.mFileLock.unlock();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performWriteState(long initialTime) {
        synchronized (this.mPendingWriteLock) {
            Parcel data = this.mPendingWrite;
            AtomicFile file = this.mPendingWriteFile;
            this.mPendingWriteCommitted = false;
            if (data == null) {
                return;
            }
            this.mPendingWrite = null;
            this.mPendingWriteFile = null;
            this.mFileLock.lock();
            long startTime = SystemClock.uptimeMillis();
            FileOutputStream stream = null;
            try {
                try {
                    stream = file.startWrite();
                    stream.write(data.marshall());
                    stream.flush();
                    file.finishWrite(stream);
                    com.android.internal.logging.EventLogTags.writeCommitSysConfigFile("procstats", (SystemClock.uptimeMillis() - startTime) + initialTime);
                } catch (IOException e) {
                    Slog.w(TAG, "Error writing process statistics", e);
                    file.failWrite(stream);
                }
            } finally {
                data.recycle();
                trimHistoricStatesWriteLF();
                this.mFileLock.unlock();
            }
        }
    }

    private boolean readLF(ProcessStats stats, AtomicFile file) {
        try {
            FileInputStream stream = file.openRead();
            stats.read(stream);
            stream.close();
            if (stats.mReadError != null) {
                Slog.w(TAG, "Ignoring existing stats; " + stats.mReadError);
                return false;
            }
            return true;
        } catch (Throwable e) {
            stats.mReadError = "caught exception: " + e;
            Slog.e(TAG, "Error reading process statistics", e);
            return false;
        }
    }

    private ArrayList<String> getCommittedFilesLF(int minNum, boolean inclCurrent, boolean inclCheckedIn) {
        File[] files = this.mBaseDir.listFiles();
        if (files == null || files.length <= minNum) {
            return null;
        }
        ArrayList<String> filesArray = new ArrayList<>(files.length);
        String currentFile = this.mFile.getBaseFile().getPath();
        for (File file : files) {
            String fileStr = file.getPath();
            if (file.getName().startsWith(STATE_FILE_PREFIX) && ((inclCheckedIn || !fileStr.endsWith(STATE_FILE_CHECKIN_SUFFIX)) && (inclCurrent || !fileStr.equals(currentFile)))) {
                filesArray.add(fileStr);
            }
        }
        Collections.sort(filesArray);
        return filesArray;
    }

    private void trimHistoricStatesWriteLF() {
        File[] files = this.mBaseDir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (!files[i].getName().startsWith(STATE_FILE_PREFIX)) {
                    files[i].delete();
                }
            }
        }
        ArrayList<String> filesArray = getCommittedFilesLF(8, false, true);
        if (filesArray != null) {
            while (filesArray.size() > 8) {
                String file = filesArray.remove(0);
                Slog.i(TAG, "Pruning old procstats: " + file);
                new File(file).delete();
            }
        }
    }

    private boolean dumpFilteredProcessesCsvLocked(PrintWriter pw, String header, boolean sepScreenStates, int[] screenStates, boolean sepMemStates, int[] memStates, boolean sepProcStates, int[] procStates, long now, String reqPackage) {
        ArrayList<ProcessState> procs = this.mProcessStats.collectProcessesLocked(screenStates, memStates, procStates, procStates, now, reqPackage, false);
        if (procs.size() > 0) {
            if (header != null) {
                pw.println(header);
            }
            DumpUtils.dumpProcessListCsv(pw, procs, sepScreenStates, screenStates, sepMemStates, memStates, sepProcStates, procStates, now);
            return true;
        }
        return false;
    }

    static int[] parseStateList(String[] states, int mult, String arg, boolean[] outSep, String[] outError) {
        ArrayList<Integer> res = new ArrayList<>();
        int lastPos = 0;
        int i = 0;
        while (i <= arg.length()) {
            char c = i < arg.length() ? arg.charAt(i) : (char) 0;
            if (c == ',' || c == '+' || c == ' ' || c == 0) {
                boolean isSep = c == ',';
                if (lastPos == 0) {
                    outSep[0] = isSep;
                } else if (c != 0 && outSep[0] != isSep) {
                    outError[0] = "inconsistent separators (can't mix ',' with '+')";
                    return null;
                }
                if (lastPos < i - 1) {
                    String str = arg.substring(lastPos, i);
                    int j = 0;
                    while (true) {
                        if (j < states.length) {
                            if (!str.equals(states[j])) {
                                j++;
                            } else {
                                res.add(Integer.valueOf(j));
                                str = null;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (str != null) {
                        outError[0] = "invalid word \"" + str + "\"";
                        return null;
                    }
                }
                lastPos = i + 1;
            }
            i++;
        }
        int i2 = res.size();
        int[] finalRes = new int[i2];
        for (int i3 = 0; i3 < res.size(); i3++) {
            finalRes[i3] = res.get(i3).intValue() * mult;
        }
        return finalRes;
    }

    static int parseSectionOptions(String optionsStr) {
        String[] sectionsStr = optionsStr.split(",");
        if (sectionsStr.length == 0) {
            return 31;
        }
        int res = 0;
        List<String> optionStrList = Arrays.asList(ProcessStats.OPTIONS_STR);
        for (String sectionStr : sectionsStr) {
            int optionIndex = optionStrList.indexOf(sectionStr);
            if (optionIndex != -1) {
                res |= ProcessStats.OPTIONS[optionIndex];
            }
        }
        return res;
    }

    public byte[] getCurrentStats(List<ParcelFileDescriptor> historic) {
        this.mAm.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", null);
        Parcel current = Parcel.obtain();
        synchronized (this.mLock) {
            long now = SystemClock.uptimeMillis();
            this.mProcessStats.mTimePeriodEndRealtime = SystemClock.elapsedRealtime();
            this.mProcessStats.mTimePeriodEndUptime = now;
            this.mProcessStats.writeToParcel(current, now, 0);
        }
        this.mFileLock.lock();
        if (historic != null) {
            try {
                ArrayList<String> files = getCommittedFilesLF(0, false, true);
                if (files != null) {
                    for (int i = files.size() - 1; i >= 0; i--) {
                        try {
                            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(files.get(i)), 268435456);
                            historic.add(pfd);
                        } catch (IOException e) {
                            Slog.w(TAG, "Failure opening procstat file " + files.get(i), e);
                        }
                    }
                }
            } catch (Throwable th) {
                this.mFileLock.unlock();
                throw th;
            }
        }
        this.mFileLock.unlock();
        return current.marshall();
    }

    public long getCommittedStats(long highWaterMarkMs, int section, boolean doAggregate, List<ParcelFileDescriptor> committedStats) {
        return getCommittedStatsMerged(highWaterMarkMs, section, doAggregate, committedStats, new ProcessStats(false));
    }

    public long getCommittedStatsMerged(long highWaterMarkMs, int section, boolean doAggregate, List<ParcelFileDescriptor> committedStats, ProcessStats mergedStats) {
        ArrayList<String> files;
        String str;
        ArrayList<String> files2;
        String highWaterMarkStr;
        String str2 = STATE_FILE_PREFIX;
        this.mAm.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", null);
        long newHighWaterMark = highWaterMarkMs;
        this.mFileLock.lock();
        try {
            files = getCommittedFilesLF(0, false, true);
        } catch (IOException e) {
            e = e;
        } catch (Throwable th) {
            th = th;
            this.mFileLock.unlock();
            throw th;
        }
        if (files == null) {
            this.mFileLock.unlock();
            return newHighWaterMark;
        }
        try {
            try {
                String highWaterMarkStr2 = DateFormat.format("yyyy-MM-dd-HH-mm-ss", highWaterMarkMs).toString();
                ProcessStats stats = new ProcessStats(false);
                int i = files.size() - 1;
                while (i >= 0) {
                    String fileName = files.get(i);
                    try {
                        str = str2;
                        try {
                            String startTimeStr = fileName.substring(fileName.lastIndexOf(str2) + str2.length(), fileName.lastIndexOf(STATE_FILE_SUFFIX));
                            if (startTimeStr.compareToIgnoreCase(highWaterMarkStr2) > 0) {
                                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(fileName), 268435456);
                                InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                                stats.reset();
                                stats.read(is);
                                is.close();
                                files2 = files;
                                highWaterMarkStr = highWaterMarkStr2;
                                try {
                                    if (stats.mTimePeriodStartClock > newHighWaterMark) {
                                        newHighWaterMark = stats.mTimePeriodStartClock;
                                    }
                                    if (doAggregate) {
                                        mergedStats.add(stats);
                                    } else if (committedStats != null) {
                                        committedStats.add(protoToParcelFileDescriptor(stats, section));
                                    }
                                    if (stats.mReadError != null) {
                                        Log.w(TAG, "Failure reading process stats: " + stats.mReadError);
                                    }
                                } catch (IOException e2) {
                                    e = e2;
                                    Slog.w(TAG, "Failure opening procstat file " + fileName, e);
                                    i--;
                                    str2 = str;
                                    files = files2;
                                    highWaterMarkStr2 = highWaterMarkStr;
                                } catch (IndexOutOfBoundsException e3) {
                                    e = e3;
                                    Slog.w(TAG, "Failure to read and parse commit file " + fileName, e);
                                    i--;
                                    str2 = str;
                                    files = files2;
                                    highWaterMarkStr2 = highWaterMarkStr;
                                }
                            } else {
                                files2 = files;
                                highWaterMarkStr = highWaterMarkStr2;
                            }
                        } catch (IOException e4) {
                            e = e4;
                            files2 = files;
                            highWaterMarkStr = highWaterMarkStr2;
                            Slog.w(TAG, "Failure opening procstat file " + fileName, e);
                            i--;
                            str2 = str;
                            files = files2;
                            highWaterMarkStr2 = highWaterMarkStr;
                        } catch (IndexOutOfBoundsException e5) {
                            e = e5;
                            files2 = files;
                            highWaterMarkStr = highWaterMarkStr2;
                            Slog.w(TAG, "Failure to read and parse commit file " + fileName, e);
                            i--;
                            str2 = str;
                            files = files2;
                            highWaterMarkStr2 = highWaterMarkStr;
                        }
                    } catch (IOException e6) {
                        e = e6;
                        str = str2;
                    } catch (IndexOutOfBoundsException e7) {
                        e = e7;
                        str = str2;
                    }
                    i--;
                    str2 = str;
                    files = files2;
                    highWaterMarkStr2 = highWaterMarkStr;
                }
                if (doAggregate && committedStats != null) {
                    committedStats.add(protoToParcelFileDescriptor(mergedStats, section));
                }
                this.mFileLock.unlock();
                return newHighWaterMark;
            } catch (IOException e8) {
                e = e8;
                Slog.w(TAG, "Failure opening procstat file", e);
                this.mFileLock.unlock();
                return newHighWaterMark;
            }
        } catch (Throwable th2) {
            th = th2;
            this.mFileLock.unlock();
            throw th;
        }
    }

    public long getMinAssociationDumpDuration() {
        ActivityManagerConstants activityManagerConstants = this.mAm.mConstants;
        return ActivityManagerConstants.MIN_ASSOC_LOG_DURATION;
    }

    private static ParcelFileDescriptor protoToParcelFileDescriptor(final ProcessStats stats, final int section) throws IOException {
        final ParcelFileDescriptor[] fds = ParcelFileDescriptor.createPipe();
        Thread thr = new Thread("ProcessStats pipe output") { // from class: com.android.server.am.ProcessStatsService.3
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                try {
                    FileOutputStream fout = new ParcelFileDescriptor.AutoCloseOutputStream(fds[1]);
                    ProtoOutputStream proto = new ProtoOutputStream(fout);
                    ProcessStats processStats = stats;
                    processStats.dumpDebug(proto, processStats.mTimePeriodEndRealtime, section);
                    proto.flush();
                    fout.close();
                } catch (IOException e) {
                    Slog.w(ProcessStatsService.TAG, "Failure writing pipe", e);
                }
            }
        };
        thr.start();
        return fds[0];
    }

    public ParcelFileDescriptor getStatsOverTime(long minTime) {
        long curTime;
        this.mAm.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", null);
        Parcel current = Parcel.obtain();
        synchronized (this.mLock) {
            long now = SystemClock.uptimeMillis();
            this.mProcessStats.mTimePeriodEndRealtime = SystemClock.elapsedRealtime();
            this.mProcessStats.mTimePeriodEndUptime = now;
            this.mProcessStats.writeToParcel(current, now, 0);
            curTime = this.mProcessStats.mTimePeriodEndRealtime - this.mProcessStats.mTimePeriodStartRealtime;
        }
        this.mFileLock.lock();
        try {
            if (curTime < minTime) {
                ArrayList<String> files = getCommittedFilesLF(0, false, true);
                if (files != null && files.size() > 0) {
                    current.setDataPosition(0);
                    ProcessStats stats = (ProcessStats) ProcessStats.CREATOR.createFromParcel(current);
                    current.recycle();
                    int i = files.size() - 1;
                    while (i >= 0 && stats.mTimePeriodEndRealtime - stats.mTimePeriodStartRealtime < minTime) {
                        AtomicFile file = new AtomicFile(new File(files.get(i)));
                        i--;
                        ProcessStats moreStats = new ProcessStats(false);
                        readLF(moreStats, file);
                        if (moreStats.mReadError == null) {
                            stats.add(moreStats);
                            StringBuilder sb = new StringBuilder();
                            sb.append("Added stats: ");
                            sb.append(moreStats.mTimePeriodStartClockStr);
                            sb.append(", over ");
                            TimeUtils.formatDuration(moreStats.mTimePeriodEndRealtime - moreStats.mTimePeriodStartRealtime, sb);
                            Slog.i(TAG, sb.toString());
                        } else {
                            Slog.w(TAG, "Failure reading " + files.get(i + 1) + "; " + moreStats.mReadError);
                        }
                    }
                    current = Parcel.obtain();
                    stats.writeToParcel(current, 0);
                }
            }
            final byte[] outData = current.marshall();
            current.recycle();
            final ParcelFileDescriptor[] fds = ParcelFileDescriptor.createPipe();
            Thread thr = new Thread("ProcessStats pipe output") { // from class: com.android.server.am.ProcessStatsService.4
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    FileOutputStream fout = new ParcelFileDescriptor.AutoCloseOutputStream(fds[1]);
                    try {
                        fout.write(outData);
                        fout.close();
                    } catch (IOException e) {
                        Slog.w(ProcessStatsService.TAG, "Failure writing pipe", e);
                    }
                }
            };
            thr.start();
            return fds[0];
        } catch (IOException e) {
            Slog.w(TAG, "Failed building output pipe", e);
            return null;
        } finally {
            this.mFileLock.unlock();
        }
    }

    public int getCurrentMemoryState() {
        int i;
        synchronized (this.mLock) {
            i = this.mLastMemOnlyState;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public SparseArray<long[]> getUidProcStateStatsOverTime(long minTime) {
        long curTime;
        ProcessStats stats = new ProcessStats();
        synchronized (this.mLock) {
            long now = SystemClock.uptimeMillis();
            this.mProcessStats.mTimePeriodEndRealtime = SystemClock.elapsedRealtime();
            this.mProcessStats.mTimePeriodEndUptime = now;
            stats.add(this.mProcessStats);
            curTime = this.mProcessStats.mTimePeriodEndRealtime - this.mProcessStats.mTimePeriodStartRealtime;
        }
        if (curTime < minTime) {
            try {
                this.mFileLock.lock();
                ArrayList<String> files = getCommittedFilesLF(0, false, true);
                if (files != null && files.size() > 0) {
                    int i = files.size() - 1;
                    while (i >= 0) {
                        if (stats.mTimePeriodEndRealtime - stats.mTimePeriodStartRealtime >= minTime) {
                            break;
                        }
                        AtomicFile file = new AtomicFile(new File(files.get(i)));
                        i--;
                        ProcessStats moreStats = new ProcessStats(false);
                        readLF(moreStats, file);
                        if (moreStats.mReadError == null) {
                            stats.add(moreStats);
                        } else {
                            Slog.w(TAG, "Failure reading " + files.get(i + 1) + "; " + moreStats.mReadError);
                        }
                    }
                }
            } finally {
                this.mFileLock.unlock();
            }
        }
        SparseArray<UidState> uidStates = stats.mUidStates;
        SparseArray<long[]> results = new SparseArray<>();
        int size = uidStates.size();
        for (int i2 = 0; i2 < size; i2++) {
            int uid = uidStates.keyAt(i2);
            UidState uidState = uidStates.valueAt(i2);
            results.put(uid, uidState.getAggregatedDurationsInStates());
        }
        return results;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void publish() {
        LocalServices.addService(ProcessStatsInternal.class, new LocalService());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LocalService extends ProcessStatsInternal {
        private LocalService() {
        }

        public SparseArray<long[]> getUidProcStateStatsOverTime(long minTime) {
            return ProcessStatsService.this.getUidProcStateStatsOverTime(minTime);
        }
    }

    private void dumpAggregatedStats(PrintWriter pw, long aggregateHours, long now, String reqPackage, boolean isCompact, boolean dumpDetails, boolean dumpFullDetails, boolean dumpAll, boolean activeOnly, int section) {
        ParcelFileDescriptor pfd = getStatsOverTime((((aggregateHours * 60) * 60) * 1000) - (ProcessStats.COMMIT_PERIOD / 2));
        if (pfd == null) {
            pw.println("Unable to build stats!");
            return;
        }
        ProcessStats stats = new ProcessStats(false);
        InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        stats.read(stream);
        if (stats.mReadError != null) {
            pw.print("Failure reading: ");
            pw.println(stats.mReadError);
        } else if (isCompact) {
            stats.dumpCheckinLocked(pw, reqPackage, section);
        } else if (dumpDetails || dumpFullDetails) {
            stats.dumpLocked(pw, reqPackage, now, !dumpFullDetails, dumpDetails, dumpAll, activeOnly, section);
        } else {
            stats.dumpSummaryLocked(pw, reqPackage, now, activeOnly);
        }
    }

    private static void dumpHelp(PrintWriter pw) {
        pw.println("Process stats (procstats) dump options:");
        pw.println("    [--checkin|-c|--csv] [--csv-screen] [--csv-proc] [--csv-mem]");
        pw.println("    [--details] [--full-details] [--current] [--hours N] [--last N]");
        pw.println("    [--max N] --active] [--commit] [--reset] [--clear] [--write] [-h]");
        pw.println("    [--start-testing] [--stop-testing] ");
        pw.println("    [--pretend-screen-on] [--pretend-screen-off] [--stop-pretend-screen]");
        pw.println("    [<package.name>]");
        pw.println("  --checkin: perform a checkin: print and delete old committed states.");
        pw.println("  -c: print only state in checkin format.");
        pw.println("  --csv: output data suitable for putting in a spreadsheet.");
        pw.println("  --csv-screen: on, off.");
        pw.println("  --csv-mem: norm, mod, low, crit.");
        pw.println("  --csv-proc: pers, top, fore, vis, precept, backup,");
        pw.println("    service, home, prev, cached");
        pw.println("  --details: dump per-package details, not just summary.");
        pw.println("  --full-details: dump all timing and active state details.");
        pw.println("  --current: only dump current state.");
        pw.println("  --hours: aggregate over about N last hours.");
        pw.println("  --last: only show the last committed stats at index N (starting at 1).");
        pw.println("  --max: for -a, max num of historical batches to print.");
        pw.println("  --active: only show currently active processes/services.");
        pw.println("  --commit: commit current stats to disk and reset to start new stats.");
        pw.println("  --section: proc|pkg-proc|pkg-svc|pkg-asc|pkg-all|all ");
        pw.println("    options can be combined to select desired stats");
        pw.println("  --reset: reset current stats, without committing.");
        pw.println("  --clear: clear all stats; does both --reset and deletes old stats.");
        pw.println("  --write: write current in-memory stats to disk.");
        pw.println("  --read: replace current stats with last-written stats.");
        pw.println("  --start-testing: clear all stats and starting high frequency pss sampling.");
        pw.println("  --stop-testing: stop high frequency pss sampling.");
        pw.println("  --pretend-screen-on: pretend screen is on.");
        pw.println("  --pretend-screen-off: pretend screen is off.");
        pw.println("  --stop-pretend-screen: forget \"pretend screen\" and use the real state.");
        pw.println("  -a: print everything.");
        pw.println("  -h: print this help text.");
        pw.println("  <package.name>: optional name of package to filter output by.");
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [945=4] */
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (com.android.internal.util.DumpUtils.checkDumpAndUsageStatsPermission(this.mAm.mContext, TAG, pw)) {
            long ident = Binder.clearCallingIdentity();
            try {
                if (args.length > 0) {
                    if ("--proto".equals(args[0])) {
                        dumpProto(fd);
                        return;
                    } else if ("--statsd".equals(args[0])) {
                        dumpProtoForStatsd(fd);
                        return;
                    }
                }
                dumpInner(pw, args);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1250=4, 1337=4, 1367=4] */
    /* JADX WARN: Removed duplicated region for block: B:349:0x0824 A[Catch: all -> 0x084b, TRY_LEAVE, TryCatch #15 {all -> 0x084b, blocks: (B:347:0x081f, B:349:0x0824), top: B:442:0x081f }] */
    /* JADX WARN: Removed duplicated region for block: B:351:0x0846  */
    /* JADX WARN: Removed duplicated region for block: B:370:0x0894  */
    /* JADX WARN: Removed duplicated region for block: B:413:0x0950  */
    @NeverCompile
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void dumpInner(PrintWriter pw, String[] args) {
        boolean isCompact;
        boolean isCsv;
        boolean currentOnly;
        String reqPackage;
        int[] csvScreenStats;
        boolean csvSepProcStats;
        int[] csvProcStats;
        int maxNum;
        boolean csvSepMemStats;
        boolean csvSepProcStats2;
        int maxNum2;
        int[] csvMemStats;
        boolean dumpDetails;
        boolean dumpAll;
        int section;
        boolean csvSepMemStats2;
        boolean csvSepScreenStats;
        boolean z;
        boolean z2;
        boolean sepNeeded;
        int i;
        boolean z3;
        int i2;
        ProcessStats processStats;
        String fileStr;
        String reqPackage2;
        int section2;
        boolean sepNeeded2;
        ProcessStats processStats2;
        String reqPackage3;
        int section3;
        String reqPackage4;
        int section4;
        boolean sepNeeded3;
        long now = SystemClock.uptimeMillis();
        int lastIndex = 0;
        boolean isCheckin = false;
        int aggregateHours = 0;
        int[] csvScreenStats2 = {0, 4};
        boolean quit = false;
        int[] csvScreenStats3 = {3};
        int[] csvProcStats2 = ProcessStats.ALL_PROC_STATES;
        if (args != null) {
            int i3 = 0;
            int section5 = 31;
            int[] csvProcStats3 = csvProcStats2;
            boolean csvSepProcStats3 = true;
            boolean csvSepProcStats4 = false;
            boolean csvSepMemStats3 = false;
            String reqPackage5 = null;
            boolean activeOnly = false;
            int maxNum3 = 2;
            int maxNum4 = 0;
            boolean dumpAll2 = false;
            boolean dumpAll3 = false;
            boolean dumpFullDetails = false;
            boolean dumpDetails2 = false;
            boolean currentOnly2 = false;
            boolean isCsv2 = false;
            int[] csvMemStats2 = csvScreenStats3;
            while (i3 < args.length) {
                String arg = args[i3];
                if ("--checkin".equals(arg)) {
                    isCheckin = true;
                } else if ("-c".equals(arg)) {
                    isCsv2 = true;
                } else if ("--csv".equals(arg)) {
                    currentOnly2 = true;
                } else if ("--csv-screen".equals(arg)) {
                    i3++;
                    if (i3 >= args.length) {
                        pw.println("Error: argument required for --csv-screen");
                        dumpHelp(pw);
                        return;
                    }
                    int[] csvMemStats3 = csvMemStats2;
                    boolean[] sep = new boolean[1];
                    boolean isCompact2 = isCsv2;
                    String[] error = new String[1];
                    boolean isCsv3 = currentOnly2;
                    boolean currentOnly3 = dumpDetails2;
                    csvScreenStats2 = parseStateList(DumpUtils.ADJ_SCREEN_NAMES_CSV, 4, args[i3], sep, error);
                    if (csvScreenStats2 == null) {
                        pw.println("Error in \"" + args[i3] + "\": " + error[0]);
                        dumpHelp(pw);
                        return;
                    }
                    csvSepMemStats3 = sep[0];
                    csvMemStats2 = csvMemStats3;
                    isCsv2 = isCompact2;
                    currentOnly2 = isCsv3;
                    dumpDetails2 = currentOnly3;
                } else {
                    int[] csvMemStats4 = csvMemStats2;
                    boolean isCompact3 = isCsv2;
                    boolean isCsv4 = currentOnly2;
                    boolean currentOnly4 = dumpDetails2;
                    if ("--csv-mem".equals(arg)) {
                        i3++;
                        if (i3 >= args.length) {
                            pw.println("Error: argument required for --csv-mem");
                            dumpHelp(pw);
                            return;
                        }
                        boolean[] sep2 = new boolean[1];
                        String[] error2 = new String[1];
                        int[] csvMemStats5 = parseStateList(DumpUtils.ADJ_MEM_NAMES_CSV, 1, args[i3], sep2, error2);
                        if (csvMemStats5 == null) {
                            pw.println("Error in \"" + args[i3] + "\": " + error2[0]);
                            dumpHelp(pw);
                            return;
                        }
                        csvSepProcStats4 = sep2[0];
                        csvMemStats2 = csvMemStats5;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--csv-proc".equals(arg)) {
                        i3++;
                        if (i3 >= args.length) {
                            pw.println("Error: argument required for --csv-proc");
                            dumpHelp(pw);
                            return;
                        }
                        boolean[] sep3 = new boolean[1];
                        String[] error3 = new String[1];
                        csvProcStats3 = parseStateList(DumpUtils.STATE_NAMES_CSV, 1, args[i3], sep3, error3);
                        if (csvProcStats3 == null) {
                            pw.println("Error in \"" + args[i3] + "\": " + error3[0]);
                            dumpHelp(pw);
                            return;
                        }
                        csvSepProcStats3 = sep3[0];
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--details".equals(arg)) {
                        dumpFullDetails = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--full-details".equals(arg)) {
                        dumpAll3 = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--hours".equals(arg)) {
                        i3++;
                        if (i3 >= args.length) {
                            pw.println("Error: argument required for --hours");
                            dumpHelp(pw);
                            return;
                        }
                        try {
                            aggregateHours = Integer.parseInt(args[i3]);
                            csvMemStats2 = csvMemStats4;
                            isCsv2 = isCompact3;
                            currentOnly2 = isCsv4;
                            dumpDetails2 = currentOnly4;
                        } catch (NumberFormatException e) {
                            pw.println("Error: --hours argument not an int -- " + args[i3]);
                            dumpHelp(pw);
                            return;
                        }
                    } else if ("--last".equals(arg)) {
                        i3++;
                        if (i3 >= args.length) {
                            pw.println("Error: argument required for --last");
                            dumpHelp(pw);
                            return;
                        }
                        try {
                            maxNum4 = Integer.parseInt(args[i3]);
                            csvMemStats2 = csvMemStats4;
                            isCsv2 = isCompact3;
                            currentOnly2 = isCsv4;
                            dumpDetails2 = currentOnly4;
                        } catch (NumberFormatException e2) {
                            pw.println("Error: --last argument not an int -- " + args[i3]);
                            dumpHelp(pw);
                            return;
                        }
                    } else if ("--max".equals(arg)) {
                        i3++;
                        if (i3 >= args.length) {
                            pw.println("Error: argument required for --max");
                            dumpHelp(pw);
                            return;
                        }
                        try {
                            maxNum3 = Integer.parseInt(args[i3]);
                            csvMemStats2 = csvMemStats4;
                            isCsv2 = isCompact3;
                            currentOnly2 = isCsv4;
                            dumpDetails2 = currentOnly4;
                        } catch (NumberFormatException e3) {
                            pw.println("Error: --max argument not an int -- " + args[i3]);
                            dumpHelp(pw);
                            return;
                        }
                    } else if ("--active".equals(arg)) {
                        activeOnly = true;
                        dumpDetails2 = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                    } else if ("--current".equals(arg)) {
                        dumpDetails2 = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                    } else if ("--commit".equals(arg)) {
                        synchronized (this.mLock) {
                            this.mProcessStats.mFlags |= 1;
                            writeStateLocked(true, true);
                            pw.println("Process stats committed.");
                            quit = true;
                        }
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--section".equals(arg)) {
                        i3++;
                        if (i3 >= args.length) {
                            pw.println("Error: argument required for --section");
                            dumpHelp(pw);
                            return;
                        }
                        section5 = parseSectionOptions(args[i3]);
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--clear".equals(arg)) {
                        synchronized (this.mLock) {
                            this.mProcessStats.resetSafely();
                            scheduleRequestPssAllProcs(true, false);
                            this.mFileLock.lock();
                            ArrayList<String> files = getCommittedFilesLF(0, true, true);
                            if (files != null) {
                                for (int fi = files.size() - 1; fi >= 0; fi--) {
                                    new File(files.get(fi)).delete();
                                }
                            }
                            this.mFileLock.unlock();
                            pw.println("All process stats cleared.");
                            quit = true;
                        }
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--write".equals(arg)) {
                        synchronized (this.mLock) {
                            writeStateSyncLocked();
                            pw.println("Process stats written.");
                            quit = true;
                        }
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--read".equals(arg)) {
                        synchronized (this.mLock) {
                            this.mFileLock.lock();
                            readLF(this.mProcessStats, this.mFile);
                            pw.println("Process stats read.");
                            quit = true;
                        }
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--start-testing".equals(arg)) {
                        this.mAm.mAppProfiler.setTestPssMode(true);
                        pw.println("Started high frequency sampling.");
                        quit = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--stop-testing".equals(arg)) {
                        this.mAm.mAppProfiler.setTestPssMode(false);
                        pw.println("Stopped high frequency sampling.");
                        quit = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--pretend-screen-on".equals(arg)) {
                        synchronized (this.mLock) {
                            this.mInjectedScreenState = true;
                        }
                        quit = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--pretend-screen-off".equals(arg)) {
                        synchronized (this.mLock) {
                            this.mInjectedScreenState = false;
                        }
                        quit = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("--stop-pretend-screen".equals(arg)) {
                        synchronized (this.mLock) {
                            this.mInjectedScreenState = null;
                        }
                        quit = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if ("-h".equals(arg)) {
                        dumpHelp(pw);
                        return;
                    } else if ("-a".equals(arg)) {
                        dumpFullDetails = true;
                        dumpAll2 = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    } else if (arg.length() > 0 && arg.charAt(0) == '-') {
                        pw.println("Unknown option: " + arg);
                        dumpHelp(pw);
                        return;
                    } else {
                        reqPackage5 = arg;
                        dumpFullDetails = true;
                        csvMemStats2 = csvMemStats4;
                        isCsv2 = isCompact3;
                        currentOnly2 = isCsv4;
                        dumpDetails2 = currentOnly4;
                    }
                }
                i3++;
            }
            isCompact = isCsv2;
            isCsv = currentOnly2;
            currentOnly = dumpDetails2;
            reqPackage = reqPackage5;
            csvProcStats = csvProcStats3;
            dumpDetails = dumpFullDetails;
            csvSepMemStats2 = csvSepProcStats4;
            csvSepMemStats = activeOnly;
            csvScreenStats = csvScreenStats2;
            maxNum = maxNum3;
            maxNum2 = aggregateHours;
            dumpAll = dumpAll2;
            lastIndex = maxNum4;
            csvMemStats = csvMemStats2;
            boolean z4 = csvSepMemStats3;
            csvSepScreenStats = dumpAll3;
            section = section5;
            csvSepProcStats = csvSepProcStats3;
            csvSepProcStats2 = z4;
        } else {
            isCompact = false;
            isCsv = false;
            currentOnly = false;
            reqPackage = null;
            csvScreenStats = csvScreenStats2;
            csvSepProcStats = true;
            csvProcStats = csvProcStats2;
            maxNum = 2;
            csvSepMemStats = false;
            csvSepProcStats2 = false;
            maxNum2 = 0;
            csvMemStats = csvScreenStats3;
            dumpDetails = false;
            dumpAll = false;
            section = 31;
            csvSepMemStats2 = false;
            csvSepScreenStats = false;
        }
        if (quit) {
            return;
        }
        if (isCsv) {
            pw.print("Processes running summed over");
            if (!csvSepProcStats2) {
                for (int i4 : csvScreenStats) {
                    pw.print(" ");
                    DumpUtils.printScreenLabelCsv(pw, i4);
                }
            }
            if (!csvSepMemStats2) {
                for (int i5 : csvMemStats) {
                    pw.print(" ");
                    DumpUtils.printMemLabelCsv(pw, i5);
                }
            }
            if (!csvSepProcStats) {
                for (int i6 : csvProcStats) {
                    pw.print(" ");
                    pw.print(DumpUtils.STATE_NAMES_CSV[i6]);
                }
            }
            pw.println();
            synchronized (this.mLock) {
                dumpFilteredProcessesCsvLocked(pw, null, csvSepProcStats2, csvScreenStats, csvSepMemStats2, csvMemStats, csvSepProcStats, csvProcStats, now, reqPackage);
            }
            return;
        }
        int section6 = section;
        String reqPackage6 = reqPackage;
        int lastIndex2 = lastIndex;
        int aggregateHours2 = maxNum2;
        if (aggregateHours2 != 0) {
            pw.print("AGGREGATED OVER LAST ");
            pw.print(aggregateHours2);
            pw.println(" HOURS:");
            dumpAggregatedStats(pw, aggregateHours2, now, reqPackage6, isCompact, dumpDetails, csvSepScreenStats, dumpAll, csvSepMemStats, section6);
        } else if (lastIndex2 > 0) {
            pw.print("LAST STATS AT INDEX ");
            pw.print(lastIndex2);
            pw.println(":");
            this.mFileLock.lock();
            try {
                ArrayList<String> files2 = getCommittedFilesLF(0, false, true);
                if (lastIndex2 >= files2.size()) {
                    try {
                        pw.print("Only have ");
                        pw.print(files2.size());
                        pw.println(" data sets");
                        return;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                AtomicFile file = new AtomicFile(new File(files2.get(lastIndex2)));
                ProcessStats processStats3 = new ProcessStats(false);
                readLF(processStats3, file);
                this.mFileLock.unlock();
                if (processStats3.mReadError != null) {
                    if (isCheckin || isCompact) {
                        pw.print("err,");
                    }
                    pw.print("Failure reading ");
                    pw.print(files2.get(lastIndex2));
                    pw.print("; ");
                    pw.println(processStats3.mReadError);
                    return;
                }
                boolean checkedIn = file.getBaseFile().getPath().endsWith(STATE_FILE_CHECKIN_SUFFIX);
                if (!isCheckin && !isCompact) {
                    pw.print("COMMITTED STATS FROM ");
                    pw.print(processStats3.mTimePeriodStartClockStr);
                    if (checkedIn) {
                        pw.print(" (checked in)");
                    }
                    pw.println(":");
                    if (!dumpDetails && !csvSepScreenStats) {
                        processStats3.dumpSummaryLocked(pw, reqPackage6, now, csvSepMemStats);
                        return;
                    }
                    processStats3.dumpLocked(pw, reqPackage6, now, !csvSepScreenStats, dumpDetails, dumpAll, csvSepMemStats, section6);
                    if (dumpAll) {
                        pw.print("  mFile=");
                        pw.println(getCurrentFile());
                        return;
                    }
                    return;
                }
                processStats3.dumpCheckinLocked(pw, reqPackage6, section6);
            } catch (Throwable th2) {
                th = th2;
            }
        } else {
            int section7 = section6;
            String reqPackage7 = reqPackage6;
            boolean z5 = true;
            boolean sepNeeded4 = false;
            if (!dumpAll && !isCheckin) {
                z = true;
                z2 = false;
            } else if (!currentOnly) {
                this.mFileLock.lock();
                try {
                    ArrayList<String> files3 = getCommittedFilesLF(0, false, !isCheckin);
                    if (files3 != null) {
                        int start = isCheckin ? 0 : files3.size() - maxNum;
                        int i7 = start < 0 ? 0 : start;
                        while (i7 < files3.size()) {
                            try {
                                AtomicFile file2 = new AtomicFile(new File(files3.get(i7)));
                                try {
                                    ProcessStats processStats4 = new ProcessStats(false);
                                    readLF(processStats4, file2);
                                    if (processStats4.mReadError != null) {
                                        if (isCheckin || isCompact) {
                                            try {
                                                pw.print("err,");
                                            } catch (Throwable th3) {
                                                e = th3;
                                                i = i7;
                                                z3 = z5;
                                                pw.print("**** FAILURE DUMPING STATE: ");
                                                i2 = i;
                                                pw.println(files3.get(i2));
                                                e.printStackTrace(pw);
                                                i7 = i2 + 1;
                                                z5 = z3;
                                            }
                                        }
                                        pw.print("Failure reading ");
                                        pw.print(files3.get(i7));
                                        pw.print("; ");
                                        pw.println(processStats4.mReadError);
                                        new File(files3.get(i7)).delete();
                                        i2 = i7;
                                        z3 = z5;
                                    } else {
                                        String fileStr2 = file2.getBaseFile().getPath();
                                        boolean checkedIn2 = fileStr2.endsWith(STATE_FILE_CHECKIN_SUFFIX);
                                        try {
                                            if (isCheckin) {
                                                processStats = processStats4;
                                                fileStr = fileStr2;
                                                i = i7;
                                                z3 = z5;
                                                reqPackage2 = reqPackage7;
                                                section2 = section7;
                                            } else if (isCompact) {
                                                processStats = processStats4;
                                                fileStr = fileStr2;
                                                i = i7;
                                                z3 = z5;
                                                reqPackage2 = reqPackage7;
                                                section2 = section7;
                                            } else {
                                                if (sepNeeded4) {
                                                    pw.println();
                                                    sepNeeded2 = sepNeeded4;
                                                } else {
                                                    sepNeeded2 = true;
                                                }
                                                try {
                                                    pw.print("COMMITTED STATS FROM ");
                                                    pw.print(processStats4.mTimePeriodStartClockStr);
                                                    if (checkedIn2) {
                                                        try {
                                                            pw.print(" (checked in)");
                                                        } catch (Throwable th4) {
                                                            e = th4;
                                                            i = i7;
                                                            z3 = z5;
                                                            sepNeeded4 = sepNeeded2;
                                                            pw.print("**** FAILURE DUMPING STATE: ");
                                                            i2 = i;
                                                            pw.println(files3.get(i2));
                                                            e.printStackTrace(pw);
                                                            i7 = i2 + 1;
                                                            z5 = z3;
                                                        }
                                                    }
                                                    pw.println(":");
                                                    if (csvSepScreenStats) {
                                                        processStats2 = processStats4;
                                                        fileStr = fileStr2;
                                                        i = i7;
                                                        z3 = z5;
                                                        reqPackage3 = reqPackage7;
                                                        section3 = section7;
                                                        try {
                                                            processStats4.dumpLocked(pw, reqPackage7, now, false, false, false, csvSepMemStats, section7);
                                                        } catch (Throwable th5) {
                                                            e = th5;
                                                            sepNeeded4 = sepNeeded2;
                                                            reqPackage7 = reqPackage3;
                                                            section7 = section3;
                                                            pw.print("**** FAILURE DUMPING STATE: ");
                                                            i2 = i;
                                                            pw.println(files3.get(i2));
                                                            e.printStackTrace(pw);
                                                            i7 = i2 + 1;
                                                            z5 = z3;
                                                        }
                                                    } else {
                                                        processStats2 = processStats4;
                                                        fileStr = fileStr2;
                                                        i = i7;
                                                        z3 = z5;
                                                        reqPackage3 = reqPackage7;
                                                        section3 = section7;
                                                        processStats2.dumpSummaryLocked(pw, reqPackage3, now, csvSepMemStats);
                                                    }
                                                    sepNeeded4 = sepNeeded2;
                                                    reqPackage7 = reqPackage3;
                                                    section7 = section3;
                                                    if (!isCheckin) {
                                                        file2.getBaseFile().renameTo(new File(fileStr + STATE_FILE_CHECKIN_SUFFIX));
                                                    }
                                                    i2 = i;
                                                } catch (Throwable th6) {
                                                    e = th6;
                                                    i = i7;
                                                    z3 = z5;
                                                    sepNeeded4 = sepNeeded2;
                                                }
                                            }
                                            processStats.dumpCheckinLocked(pw, reqPackage7, section7);
                                            if (!isCheckin) {
                                            }
                                            i2 = i;
                                        } catch (Throwable th7) {
                                            e = th7;
                                            pw.print("**** FAILURE DUMPING STATE: ");
                                            i2 = i;
                                            pw.println(files3.get(i2));
                                            e.printStackTrace(pw);
                                            i7 = i2 + 1;
                                            z5 = z3;
                                        }
                                        reqPackage7 = reqPackage2;
                                        section7 = section2;
                                    }
                                } catch (Throwable th8) {
                                    e = th8;
                                    i = i7;
                                    z3 = z5;
                                }
                            } catch (Throwable th9) {
                                e = th9;
                                i = i7;
                                z3 = z5;
                            }
                            i7 = i2 + 1;
                            z5 = z3;
                        }
                        z = z5;
                        z2 = false;
                    } else {
                        z = true;
                        z2 = false;
                    }
                    this.mFileLock.unlock();
                    sepNeeded = sepNeeded4;
                    if (isCheckin) {
                        synchronized (this.mLock) {
                            try {
                                if (isCompact) {
                                    this.mProcessStats.dumpCheckinLocked(pw, reqPackage7, section7);
                                    reqPackage4 = reqPackage7;
                                    section4 = section7;
                                    sepNeeded3 = sepNeeded;
                                } else {
                                    if (sepNeeded) {
                                        pw.println();
                                    }
                                    try {
                                        pw.println("CURRENT STATS:");
                                        if (dumpDetails || csvSepScreenStats) {
                                            reqPackage4 = reqPackage7;
                                            section4 = section7;
                                            try {
                                                this.mProcessStats.dumpLocked(pw, reqPackage7, now, !csvSepScreenStats ? z : z2, dumpDetails, dumpAll, csvSepMemStats, section7);
                                                if (dumpAll) {
                                                    try {
                                                        pw.print("  mFile=");
                                                        pw.println(getCurrentFile());
                                                    } catch (Throwable th10) {
                                                        th = th10;
                                                        while (true) {
                                                            try {
                                                                break;
                                                            } catch (Throwable th11) {
                                                                th = th11;
                                                            }
                                                        }
                                                        throw th;
                                                    }
                                                }
                                            } catch (Throwable th12) {
                                                th = th12;
                                            }
                                        } else {
                                            this.mProcessStats.dumpSummaryLocked(pw, reqPackage7, now, csvSepMemStats);
                                            reqPackage4 = reqPackage7;
                                            section4 = section7;
                                        }
                                        sepNeeded3 = true;
                                    } catch (Throwable th13) {
                                        th = th13;
                                    }
                                }
                            } catch (Throwable th14) {
                                th = th14;
                            }
                            try {
                                if (!currentOnly) {
                                    if (sepNeeded3) {
                                        pw.println();
                                    }
                                    pw.println("AGGREGATED OVER LAST 24 HOURS:");
                                    String str = reqPackage4;
                                    boolean z6 = isCompact;
                                    boolean z7 = dumpDetails;
                                    boolean z8 = csvSepScreenStats;
                                    boolean z9 = dumpAll;
                                    boolean z10 = csvSepMemStats;
                                    int lastIndex3 = section4;
                                    dumpAggregatedStats(pw, 24L, now, str, z6, z7, z8, z9, z10, lastIndex3);
                                    pw.println();
                                    pw.println("AGGREGATED OVER LAST 3 HOURS:");
                                    dumpAggregatedStats(pw, 3L, now, str, z6, z7, z8, z9, z10, lastIndex3);
                                }
                                return;
                            } catch (Throwable th15) {
                                th = th15;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        }
                    }
                    return;
                } finally {
                    this.mFileLock.unlock();
                }
            } else {
                z = true;
                z2 = false;
            }
            sepNeeded = false;
            if (isCheckin) {
            }
        }
    }

    private void dumpAggregatedStats(ProtoOutputStream proto, long fieldId, int aggregateHours, long now) {
        ParcelFileDescriptor pfd = getStatsOverTime((((aggregateHours * 60) * 60) * 1000) - (ProcessStats.COMMIT_PERIOD / 2));
        if (pfd == null) {
            return;
        }
        ProcessStats stats = new ProcessStats(false);
        InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        stats.read(stream);
        if (stats.mReadError != null) {
            return;
        }
        long token = proto.start(fieldId);
        stats.dumpDebug(proto, now, 31);
        proto.end(token);
    }

    private void dumpProto(FileDescriptor fd) {
        long now;
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mLock) {
            now = SystemClock.uptimeMillis();
            long token = proto.start(1146756268033L);
            this.mProcessStats.dumpDebug(proto, now, 31);
            proto.end(token);
        }
        dumpAggregatedStats(proto, 1146756268034L, 3, now);
        dumpAggregatedStats(proto, 1146756268035L, 24, now);
        proto.flush();
    }

    private void dumpProtoForStatsd(FileDescriptor fd) {
        ProtoOutputStream[] protos = {new ProtoOutputStream(fd)};
        ProcessStats procStats = new ProcessStats(false);
        getCommittedStatsMerged(0L, 0, true, null, procStats);
        procStats.dumpAggregatedProtoForStatsd(protos, 999999L);
        protos[0].flush();
    }
}
