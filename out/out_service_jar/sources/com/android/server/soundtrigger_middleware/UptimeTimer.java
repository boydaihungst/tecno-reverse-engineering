package com.android.server.soundtrigger_middleware;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class UptimeTimer {
    private static final String TAG = "UptimeTimer";
    private Handler mHandler = null;
    private ThreadPoolExecutor mThreadPoolExecutor = null;
    private TaskImpl task;

    /* loaded from: classes2.dex */
    interface Task {
        void cancel();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UptimeTimer(String threadName) {
        initThreadPoolExecutor();
        if (this.mThreadPoolExecutor != null) {
            Thread thread = new Thread(new Runnable() { // from class: com.android.server.soundtrigger_middleware.UptimeTimer$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    UptimeTimer.this.threadFunc();
                }
            }, threadName);
            this.mThreadPoolExecutor.execute(thread);
        }
        synchronized (this) {
            while (this.mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task createTask(Runnable runnable, long uptimeMs) {
        TaskImpl taskImpl = new TaskImpl(runnable);
        this.task = taskImpl;
        this.mHandler.postDelayed(taskImpl, uptimeMs);
        return this.task;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void threadFunc() {
        Looper.prepare();
        synchronized (this) {
            this.mHandler = new Handler(Looper.myLooper());
            notifyAll();
        }
        Looper.loop();
    }

    private void initThreadPoolExecutor() {
        this.mThreadPoolExecutor = new ThreadPoolExecutor(1, 5, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue(100));
    }

    public void onDetach() {
        synchronized (this) {
            try {
                Handler handler = this.mHandler;
                if (handler != null) {
                    handler.removeCallbacksAndMessages(null);
                    this.mHandler.getLooper().quit();
                }
            } catch (Exception e) {
                Log.e(TAG, "[onDetach] Exception ", e);
            }
        }
        TaskImpl taskImpl = this.task;
        if (taskImpl != null) {
            taskImpl.cancel();
        }
        shutdownThreadPoolExecutor();
    }

    private void shutdownThreadPoolExecutor() {
        try {
            ThreadPoolExecutor threadPoolExecutor = this.mThreadPoolExecutor;
            if (threadPoolExecutor != null) {
                threadPoolExecutor.shutdown();
                this.mThreadPoolExecutor = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "[shutdownThreadPoolExecutor] thread Error: " + e.getMessage());
        }
    }

    /* loaded from: classes2.dex */
    private static class TaskImpl implements Task, Runnable {
        private AtomicReference<Runnable> mRunnable;

        TaskImpl(Runnable runnable) {
            AtomicReference<Runnable> atomicReference = new AtomicReference<>();
            this.mRunnable = atomicReference;
            atomicReference.set(runnable);
        }

        @Override // com.android.server.soundtrigger_middleware.UptimeTimer.Task
        public void cancel() {
            this.mRunnable.set(null);
        }

        @Override // java.lang.Runnable
        public void run() {
            Runnable runnable = this.mRunnable.get();
            if (runnable != null) {
                runnable.run();
            }
        }
    }
}
