package com.android.server;

import android.os.Build;
import android.os.Process;
import android.util.Dumpable;
import android.util.Slog;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.Preconditions;
import com.android.server.am.ActivityManagerService;
import com.android.server.utils.TimingsTraceAndSlog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public final class SystemServerInitThreadPool implements Dumpable {
    private static final int SHUTDOWN_TIMEOUT_MILLIS = 20000;
    private static SystemServerInitThreadPool sInstance;
    private final List<String> mPendingTasks = new ArrayList();
    private final ExecutorService mService;
    private boolean mShutDown;
    private final int mSize;
    private static final String TAG = SystemServerInitThreadPool.class.getSimpleName();
    private static final boolean IS_DEBUGGABLE = Build.IS_DEBUGGABLE;
    private static final Object LOCK = new Object();

    private SystemServerInitThreadPool() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        this.mSize = availableProcessors;
        Slog.i(TAG, "Creating instance with " + availableProcessors + " threads");
        this.mService = ConcurrentUtils.newFixedThreadPool(availableProcessors, "system-server-init-thread", -2);
    }

    public static Future<?> submit(Runnable runnable, String description) {
        SystemServerInitThreadPool instance;
        Objects.requireNonNull(description, "description cannot be null");
        synchronized (LOCK) {
            Preconditions.checkState(sInstance != null, "Cannot get " + TAG + " - it has been shut down");
            instance = sInstance;
        }
        return instance.submitTask(runnable, description);
    }

    private Future<?> submitTask(final Runnable runnable, final String description) {
        synchronized (this.mPendingTasks) {
            Preconditions.checkState(!this.mShutDown, TAG + " already shut down");
            this.mPendingTasks.add(description);
        }
        return this.mService.submit(new Runnable() { // from class: com.android.server.SystemServerInitThreadPool$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SystemServerInitThreadPool.this.m428x83c775ca(description, runnable);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$submitTask$0$com-android-server-SystemServerInitThreadPool  reason: not valid java name */
    public /* synthetic */ void m428x83c775ca(String description, Runnable runnable) {
        TimingsTraceAndSlog traceLog = TimingsTraceAndSlog.newAsyncLog();
        traceLog.traceBegin("InitThreadPoolExec:" + description);
        boolean z = IS_DEBUGGABLE;
        if (z) {
            Slog.d(TAG, "Started executing " + description);
        }
        try {
            runnable.run();
            synchronized (this.mPendingTasks) {
                this.mPendingTasks.remove(description);
            }
            if (z) {
                Slog.d(TAG, "Finished executing " + description);
            }
            traceLog.traceEnd();
        } catch (RuntimeException e) {
            Slog.e(TAG, "Failure in " + description + ": " + e, e);
            traceLog.traceEnd();
            throw e;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SystemServerInitThreadPool start() {
        SystemServerInitThreadPool instance;
        synchronized (LOCK) {
            Preconditions.checkState(sInstance == null, TAG + " already started");
            instance = new SystemServerInitThreadPool();
            sInstance = instance;
        }
        return instance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void shutdown() {
        SystemServerInitThreadPool systemServerInitThreadPool;
        String str = TAG;
        Slog.d(str, "Shutdown requested");
        synchronized (LOCK) {
            TimingsTraceAndSlog t = new TimingsTraceAndSlog();
            t.traceBegin("WaitInitThreadPoolShutdown");
            SystemServerInitThreadPool systemServerInitThreadPool2 = sInstance;
            if (systemServerInitThreadPool2 == null) {
                t.traceEnd();
                Slog.wtf(str, "Already shutdown", new Exception());
                return;
            }
            synchronized (systemServerInitThreadPool2.mPendingTasks) {
                systemServerInitThreadPool = sInstance;
                systemServerInitThreadPool.mShutDown = true;
            }
            systemServerInitThreadPool.mService.shutdown();
            try {
                boolean terminated = sInstance.mService.awaitTermination(20000L, TimeUnit.MILLISECONDS);
                if (!terminated) {
                    dumpStackTraces();
                }
                List<Runnable> unstartedRunnables = sInstance.mService.shutdownNow();
                if (!terminated) {
                    List<String> copy = new ArrayList<>();
                    synchronized (sInstance.mPendingTasks) {
                        copy.addAll(sInstance.mPendingTasks);
                    }
                    t.traceEnd();
                    throw new IllegalStateException("Cannot shutdown. Unstarted tasks " + unstartedRunnables + " Unfinished tasks " + copy);
                }
                sInstance = null;
                Slog.d(str, "Shutdown successful");
                t.traceEnd();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                dumpStackTraces();
                t.traceEnd();
                throw new IllegalStateException(TAG + " init interrupted");
            }
        }
    }

    private static void dumpStackTraces() {
        ArrayList<Integer> pids = new ArrayList<>();
        pids.add(Integer.valueOf(Process.myPid()));
        ActivityManagerService.dumpStackTraces(pids, null, null, Watchdog.getInterestingNativePids(), null, null, null);
    }

    public String getDumpableName() {
        return SystemServerInitThreadPool.class.getSimpleName();
    }

    public void dump(PrintWriter pw, String[] args) {
        synchronized (LOCK) {
            Object[] objArr = new Object[1];
            objArr[0] = Boolean.valueOf(sInstance != null);
            pw.printf("has instance: %b\n", objArr);
        }
        pw.printf("number of threads: %d\n", Integer.valueOf(this.mSize));
        pw.printf("service: %s\n", this.mService);
        synchronized (this.mPendingTasks) {
            pw.printf("is shutdown: %b\n", Boolean.valueOf(this.mShutDown));
            int pendingTasks = this.mPendingTasks.size();
            if (pendingTasks == 0) {
                pw.println("no pending tasks");
            } else {
                pw.printf("%d pending tasks: %s\n", Integer.valueOf(pendingTasks), this.mPendingTasks);
            }
        }
    }
}
