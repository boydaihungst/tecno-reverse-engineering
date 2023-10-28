package com.android.server.wm;

import android.os.Build;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import android.view.Choreographer;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.TraceBuffer;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowTracing {
    private static final int BUFFER_CAPACITY_ALL = 20971520;
    private static final int BUFFER_CAPACITY_CRITICAL = 5242880;
    private static final int BUFFER_CAPACITY_TRIM = 10485760;
    private static final long MAGIC_NUMBER_VALUE = 4990904633914181975L;
    private static final String TAG = "WindowTracing";
    private static final String TRACE_FILENAME = "/data/misc/wmtrace/wm_trace.winscope";
    static final String WINSCOPE_EXT = ".winscope";
    private final boolean IS_USER_ADB;
    private final TraceBuffer mBuffer;
    private final Choreographer mChoreographer;
    private boolean mEnabled;
    private final Object mEnabledLock;
    private volatile boolean mEnabledLockFree;
    private final Choreographer.FrameCallback mFrameCallback;
    private final WindowManagerGlobalLock mGlobalLock;
    private int mLogLevel;
    private boolean mLogOnFrame;
    private boolean mScheduled;
    private final WindowManagerService mService;
    private final File mTraceFile;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-WindowTracing  reason: not valid java name */
    public /* synthetic */ void m8542lambda$new$0$comandroidserverwmWindowTracing(long frameTimeNanos) {
        log("onFrame");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static WindowTracing createDefaultAndStartLooper(WindowManagerService service, Choreographer choreographer) {
        File file = new File(TRACE_FILENAME);
        return new WindowTracing(file, service, choreographer, BUFFER_CAPACITY_TRIM);
    }

    private WindowTracing(File file, WindowManagerService service, Choreographer choreographer, int bufferCapacity) {
        this(file, service, choreographer, service.mGlobalLock, bufferCapacity);
    }

    WindowTracing(File file, WindowManagerService service, Choreographer choreographer, WindowManagerGlobalLock globalLock, int bufferCapacity) {
        this.IS_USER_ADB = "1".equals(SystemProperties.get("persist.sys.adb.support", "0"));
        this.mEnabledLock = new Object();
        this.mFrameCallback = new Choreographer.FrameCallback() { // from class: com.android.server.wm.WindowTracing$$ExternalSyntheticLambda0
            @Override // android.view.Choreographer.FrameCallback
            public final void doFrame(long j) {
                WindowTracing.this.m8542lambda$new$0$comandroidserverwmWindowTracing(j);
            }
        };
        this.mLogLevel = 1;
        this.mLogOnFrame = false;
        this.mChoreographer = choreographer;
        this.mService = service;
        this.mGlobalLock = globalLock;
        this.mTraceFile = file;
        this.mBuffer = new TraceBuffer(bufferCapacity);
        setLogLevel(1, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startTrace(PrintWriter pw) {
        if (Build.IS_USER && !this.IS_USER_ADB) {
            logAndPrintln(pw, "Error: Tracing is not supported on user builds.");
            return;
        }
        synchronized (this.mEnabledLock) {
            ProtoLogImpl.getSingleInstance().startProtoLog(pw);
            logAndPrintln(pw, "Start tracing to " + this.mTraceFile + ".");
            this.mBuffer.resetBuffer();
            this.mEnabledLockFree = true;
            this.mEnabled = true;
        }
        log("trace.enable");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopTrace(PrintWriter pw) {
        if (Build.IS_USER && !this.IS_USER_ADB) {
            logAndPrintln(pw, "Error: Tracing is not supported on user builds.");
            return;
        }
        synchronized (this.mEnabledLock) {
            logAndPrintln(pw, "Stop tracing to " + this.mTraceFile + ". Waiting for traces to flush.");
            this.mEnabledLockFree = false;
            this.mEnabled = false;
            writeTraceToFileLocked();
            logAndPrintln(pw, "Trace written to " + this.mTraceFile + ".");
        }
        ProtoLogImpl.getSingleInstance().stopProtoLog(pw, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveForBugreport(PrintWriter pw) {
        if (Build.IS_USER && !this.IS_USER_ADB) {
            logAndPrintln(pw, "Error: Tracing is not supported on user builds.");
            return;
        }
        synchronized (this.mEnabledLock) {
            if (this.mEnabled) {
                this.mEnabledLockFree = false;
                this.mEnabled = false;
                logAndPrintln(pw, "Stop tracing to " + this.mTraceFile + ". Waiting for traces to flush.");
                writeTraceToFileLocked();
                logAndPrintln(pw, "Trace written to " + this.mTraceFile + ".");
                ProtoLogImpl.getSingleInstance().stopProtoLog(pw, true);
                logAndPrintln(pw, "Start tracing to " + this.mTraceFile + ".");
                this.mBuffer.resetBuffer();
                this.mEnabledLockFree = true;
                this.mEnabled = true;
                ProtoLogImpl.getSingleInstance().startProtoLog(pw);
            }
        }
    }

    private void setLogLevel(int logLevel, PrintWriter pw) {
        logAndPrintln(pw, "Setting window tracing log level to " + logLevel);
        this.mLogLevel = logLevel;
        switch (logLevel) {
            case 0:
                setBufferCapacity(BUFFER_CAPACITY_ALL, pw);
                return;
            case 1:
                setBufferCapacity(BUFFER_CAPACITY_TRIM, pw);
                return;
            case 2:
                setBufferCapacity(BUFFER_CAPACITY_CRITICAL, pw);
                return;
            default:
                return;
        }
    }

    private void setLogFrequency(boolean onFrame, PrintWriter pw) {
        logAndPrintln(pw, "Setting window tracing log frequency to " + (onFrame ? "frame" : "transaction"));
        this.mLogOnFrame = onFrame;
    }

    private void setBufferCapacity(int capacity, PrintWriter pw) {
        logAndPrintln(pw, "Setting window tracing buffer capacity to " + capacity + "bytes");
        this.mBuffer.setCapacity(capacity);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEnabled() {
        return this.mEnabledLockFree;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onShellCommand(ShellCommand shell) {
        char c;
        PrintWriter pw = shell.getOutPrintWriter();
        String cmd = shell.getNextArgRequired();
        char c2 = 65535;
        switch (cmd.hashCode()) {
            case -892481550:
                if (cmd.equals("status")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -390772652:
                if (cmd.equals("save-for-bugreport")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 3530753:
                if (cmd.equals("size")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 3540994:
                if (cmd.equals("stop")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 97692013:
                if (cmd.equals("frame")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 102865796:
                if (cmd.equals("level")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 109757538:
                if (cmd.equals("start")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 2141246174:
                if (cmd.equals("transaction")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                startTrace(pw);
                return 0;
            case 1:
                stopTrace(pw);
                return 0;
            case 2:
                saveForBugreport(pw);
                return 0;
            case 3:
                logAndPrintln(pw, getStatus());
                return 0;
            case 4:
                setLogFrequency(true, pw);
                this.mBuffer.resetBuffer();
                return 0;
            case 5:
                setLogFrequency(false, pw);
                this.mBuffer.resetBuffer();
                return 0;
            case 6:
                String logLevelStr = shell.getNextArgRequired().toLowerCase();
                switch (logLevelStr.hashCode()) {
                    case 96673:
                        if (logLevelStr.equals("all")) {
                            c2 = 0;
                            break;
                        }
                        break;
                    case 3568674:
                        if (logLevelStr.equals("trim")) {
                            c2 = 1;
                            break;
                        }
                        break;
                    case 1952151455:
                        if (logLevelStr.equals("critical")) {
                            c2 = 2;
                            break;
                        }
                        break;
                }
                switch (c2) {
                    case 0:
                        setLogLevel(0, pw);
                        break;
                    case 1:
                        setLogLevel(1, pw);
                        break;
                    case 2:
                        setLogLevel(2, pw);
                        break;
                    default:
                        setLogLevel(1, pw);
                        break;
                }
                this.mBuffer.resetBuffer();
                return 0;
            case 7:
                setBufferCapacity(Integer.parseInt(shell.getNextArgRequired()) * 1024, pw);
                this.mBuffer.resetBuffer();
                return 0;
            default:
                pw.println("Unknown command: " + cmd);
                pw.println("Window manager trace options:");
                pw.println("  start: Start logging");
                pw.println("  stop: Stop logging");
                pw.println("  save-for-bugreport: Save logging data to file if it's running.");
                pw.println("  frame: Log trace once per frame");
                pw.println("  transaction: Log each transaction");
                pw.println("  size: Set the maximum log size (in KB)");
                pw.println("  status: Print trace status");
                pw.println("  level [lvl]: Set the log level between");
                pw.println("    lvl may be one of:");
                pw.println("      critical: Only visible windows with reduced information");
                pw.println("      trim: All windows with reduced");
                pw.println("      all: All window and information");
                return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getStatus() {
        return "Status: " + (isEnabled() ? "Enabled" : "Disabled") + "\nLog level: " + this.mLogLevel + "\n" + this.mBuffer.getStatus();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logState(String where) {
        if (!isEnabled()) {
            return;
        }
        if (this.mLogOnFrame) {
            schedule();
        } else {
            log(where);
        }
    }

    private void schedule() {
        if (this.mScheduled) {
            return;
        }
        this.mScheduled = true;
        this.mChoreographer.postFrameCallback(this.mFrameCallback);
    }

    private void log(String where) {
        Trace.traceBegin(32L, "traceStateLocked");
        try {
            try {
                ProtoOutputStream os = new ProtoOutputStream();
                long tokenOuter = os.start(2246267895810L);
                os.write(1125281431553L, SystemClock.elapsedRealtimeNanos());
                os.write(1138166333442L, where);
                long tokenInner = os.start(1146756268035L);
                synchronized (this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        Trace.traceBegin(32L, "dumpDebugLocked");
                        this.mService.dumpDebugLocked(os, this.mLogLevel);
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                os.end(tokenInner);
                os.end(tokenOuter);
                this.mBuffer.add(os);
                this.mScheduled = false;
            } catch (Exception e) {
                Log.wtf(TAG, "Exception while tracing state", e);
            }
        } finally {
            Trace.traceEnd(32L);
        }
    }

    private void logAndPrintln(PrintWriter pw, String msg) {
        Log.i(TAG, msg);
        if (pw != null) {
            pw.println(msg);
            pw.flush();
        }
    }

    private void writeTraceToFileLocked() {
        try {
            try {
                Trace.traceBegin(32L, "writeTraceToFileLocked");
                ProtoOutputStream proto = new ProtoOutputStream();
                proto.write(1125281431553L, MAGIC_NUMBER_VALUE);
                this.mBuffer.writeTraceToFile(this.mTraceFile, proto);
            } catch (IOException e) {
                Log.e(TAG, "Unable to write buffer to file", e);
            }
        } finally {
            Trace.traceEnd(32L);
        }
    }
}
