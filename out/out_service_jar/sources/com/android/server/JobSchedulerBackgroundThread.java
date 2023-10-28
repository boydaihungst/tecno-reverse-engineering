package com.android.server;

import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.os.Looper;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public final class JobSchedulerBackgroundThread extends HandlerThread {
    private static final long SLOW_DELIVERY_THRESHOLD_MS = 30000;
    private static final long SLOW_DISPATCH_THRESHOLD_MS = 10000;
    private static Handler sHandler;
    private static Executor sHandlerExecutor;
    private static JobSchedulerBackgroundThread sInstance;

    private JobSchedulerBackgroundThread() {
        super("jobscheduler.bg", 10);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            JobSchedulerBackgroundThread jobSchedulerBackgroundThread = new JobSchedulerBackgroundThread();
            sInstance = jobSchedulerBackgroundThread;
            jobSchedulerBackgroundThread.start();
            Looper looper = sInstance.getLooper();
            looper.setTraceTag(524288L);
            looper.setSlowLogThresholdMs(10000L, 30000L);
            sHandler = new Handler(sInstance.getLooper());
            sHandlerExecutor = new HandlerExecutor(sHandler);
        }
    }

    public static JobSchedulerBackgroundThread get() {
        JobSchedulerBackgroundThread jobSchedulerBackgroundThread;
        synchronized (JobSchedulerBackgroundThread.class) {
            ensureThreadLocked();
            jobSchedulerBackgroundThread = sInstance;
        }
        return jobSchedulerBackgroundThread;
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (JobSchedulerBackgroundThread.class) {
            ensureThreadLocked();
            handler = sHandler;
        }
        return handler;
    }

    public static Executor getExecutor() {
        Executor executor;
        synchronized (JobSchedulerBackgroundThread.class) {
            ensureThreadLocked();
            executor = sHandlerExecutor;
        }
        return executor;
    }
}
