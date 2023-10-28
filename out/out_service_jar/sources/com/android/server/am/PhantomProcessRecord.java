package com.android.server.am;

import android.os.Handler;
import android.os.Process;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimeUtils;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public final class PhantomProcessRecord {
    static final String TAG = "ActivityManager";
    long mCurrentCputime;
    final Handler mKillHandler;
    long mLastCputime;
    final Object mLock;
    final Consumer<PhantomProcessRecord> mOnKillListener;
    final int mPid;
    final FileDescriptor mPidFd;
    final int mPpid;
    final String mProcessName;
    final ActivityManagerService mService;
    String mStringName;
    final int mUid;
    int mUpdateSeq;
    boolean mZombie;
    static final long[] LONG_OUT = new long[1];
    static final int[] LONG_FORMAT = {8202};
    private Runnable mProcKillTimer = new Runnable() { // from class: com.android.server.am.PhantomProcessRecord.1
        @Override // java.lang.Runnable
        public void run() {
            synchronized (PhantomProcessRecord.this.mLock) {
                Slog.w(PhantomProcessRecord.TAG, "Process " + toString() + " is still alive after " + PhantomProcessRecord.this.mService.mConstants.mProcessKillTimeoutMs + "ms");
                PhantomProcessRecord.this.mZombie = true;
                PhantomProcessRecord.this.onProcDied(false);
            }
        }
    };
    boolean mKilled = false;
    int mAdj = -1000;
    final long mKnownSince = SystemClock.elapsedRealtime();

    /* JADX INFO: Access modifiers changed from: package-private */
    public PhantomProcessRecord(String processName, int uid, int pid, int ppid, ActivityManagerService service, Consumer<PhantomProcessRecord> onKillListener) throws IllegalStateException {
        this.mProcessName = processName;
        this.mUid = uid;
        this.mPid = pid;
        this.mPpid = ppid;
        this.mService = service;
        this.mLock = service.mPhantomProcessList.mLock;
        this.mOnKillListener = onKillListener;
        ProcessList processList = service.mProcessList;
        this.mKillHandler = ProcessList.sKillHandler;
        if (Process.supportsPidFd()) {
            StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
            try {
                try {
                    FileDescriptor openPidFd = Process.openPidFd(pid, 0);
                    this.mPidFd = openPidFd;
                    if (openPidFd == null) {
                        throw new IllegalStateException();
                    }
                    return;
                } catch (IOException e) {
                    Slog.w(TAG, "Unable to open process " + pid + ", it might be gone");
                    IllegalStateException ex = new IllegalStateException();
                    ex.initCause(e);
                    throw ex;
                }
            } finally {
                StrictMode.setThreadPolicy(oldPolicy);
            }
        }
        this.mPidFd = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void killLocked(String reason, boolean noisy) {
        if (!this.mKilled) {
            Trace.traceBegin(64L, "kill");
            if (noisy || this.mUid == this.mService.mCurOomAdjUid) {
                this.mService.reportUidInfoMessageLocked(TAG, "Killing " + toString() + ": " + reason, this.mUid);
            }
            if (this.mPid > 0) {
                EventLog.writeEvent((int) EventLogTags.AM_KILL, Integer.valueOf(UserHandle.getUserId(this.mUid)), Integer.valueOf(this.mPid), this.mProcessName, Integer.valueOf(this.mAdj), reason);
                if (!Process.supportsPidFd()) {
                    onProcDied(false);
                } else {
                    this.mKillHandler.postDelayed(this.mProcKillTimer, this, this.mService.mConstants.mProcessKillTimeoutMs);
                }
                Process.killProcessQuiet(this.mPid);
                ProcessList.killProcessGroup(this.mUid, this.mPid);
            }
            this.mKilled = true;
            Trace.traceEnd(64L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAdjLocked() {
        String str = "/proc/" + this.mPid + "/oom_score_adj";
        int[] iArr = LONG_FORMAT;
        long[] jArr = LONG_OUT;
        if (Process.readProcFile(str, iArr, null, jArr, null)) {
            this.mAdj = (int) jArr[0];
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onProcDied(boolean reallyDead) {
        if (reallyDead) {
            Slog.i(TAG, "Process " + toString() + " died");
        }
        this.mKillHandler.removeCallbacks(this.mProcKillTimer, this);
        Consumer<PhantomProcessRecord> consumer = this.mOnKillListener;
        if (consumer != null) {
            consumer.accept(this);
        }
    }

    public String toString() {
        String str = this.mStringName;
        if (str != null) {
            return str;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("PhantomProcessRecord {");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.mPid);
        sb.append(':');
        sb.append(this.mPpid);
        sb.append(':');
        sb.append(this.mProcessName);
        sb.append('/');
        int i = this.mUid;
        if (i < 10000) {
            sb.append(i);
        } else {
            sb.append('u');
            sb.append(UserHandle.getUserId(this.mUid));
            int appId = UserHandle.getAppId(this.mUid);
            if (appId >= 10000) {
                sb.append('a');
                sb.append(appId - 10000);
            } else {
                sb.append('s');
                sb.append(appId);
            }
            if (appId >= 99000 && appId <= 99999) {
                sb.append('i');
                sb.append(appId - 99000);
            }
        }
        sb.append('}');
        String sb2 = sb.toString();
        this.mStringName = sb2;
        return sb2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        long now = SystemClock.elapsedRealtime();
        pw.print(prefix);
        pw.print("user #");
        pw.print(UserHandle.getUserId(this.mUid));
        pw.print(" uid=");
        pw.print(this.mUid);
        pw.print(" pid=");
        pw.print(this.mPid);
        pw.print(" ppid=");
        pw.print(this.mPpid);
        pw.print(" knownSince=");
        TimeUtils.formatDuration(this.mKnownSince, now, pw);
        pw.print(" killed=");
        pw.println(this.mKilled);
        pw.print(prefix);
        pw.print("lastCpuTime=");
        pw.print(this.mLastCputime);
        if (this.mLastCputime > 0) {
            pw.print(" timeUsed=");
            TimeUtils.formatDuration(this.mCurrentCputime - this.mLastCputime, pw);
        }
        pw.print(" oom adj=");
        pw.print(this.mAdj);
        pw.print(" seq=");
        pw.println(this.mUpdateSeq);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean equals(String processName, int uid, int pid) {
        return this.mUid == uid && this.mPid == pid && TextUtils.equals(this.mProcessName, processName);
    }
}
