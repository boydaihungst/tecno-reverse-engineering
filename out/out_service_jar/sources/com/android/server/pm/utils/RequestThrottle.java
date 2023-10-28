package com.android.server.pm.utils;

import android.os.Handler;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class RequestThrottle {
    private static final int DEFAULT_BACKOFF_BASE = 2;
    private static final int DEFAULT_DELAY_MS = 1000;
    private static final int DEFAULT_RETRY_MAX_ATTEMPTS = 5;
    private final int mBackoffBase;
    private final Supplier<Boolean> mBlock;
    private final AtomicInteger mCurrentRetry;
    private final int mFirstDelay;
    private final Handler mHandler;
    private final AtomicInteger mLastCommitted;
    private final AtomicInteger mLastRequest;
    private final int mMaxAttempts;
    private final Runnable mRunnable;

    public RequestThrottle(Handler handler, Supplier<Boolean> block) {
        this(handler, 5, 1000, 2, block);
    }

    public RequestThrottle(Handler handler, int maxAttempts, int firstDelay, int backoffBase, Supplier<Boolean> block) {
        this.mLastRequest = new AtomicInteger(0);
        this.mLastCommitted = new AtomicInteger(-1);
        this.mCurrentRetry = new AtomicInteger(0);
        this.mHandler = handler;
        this.mBlock = block;
        this.mMaxAttempts = maxAttempts;
        this.mFirstDelay = firstDelay;
        this.mBackoffBase = backoffBase;
        this.mRunnable = new Runnable() { // from class: com.android.server.pm.utils.RequestThrottle$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RequestThrottle.this.runInternal();
            }
        };
    }

    public void schedule() {
        this.mLastRequest.incrementAndGet();
        this.mHandler.post(this.mRunnable);
    }

    public boolean runNow() {
        this.mLastRequest.incrementAndGet();
        return runInternal();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean runInternal() {
        int lastRequest = this.mLastRequest.get();
        int lastCommitted = this.mLastCommitted.get();
        if (lastRequest == lastCommitted) {
            return true;
        }
        if (this.mBlock.get().booleanValue()) {
            this.mCurrentRetry.set(0);
            this.mLastCommitted.set(lastRequest);
            return true;
        }
        int currentRetry = this.mCurrentRetry.getAndIncrement();
        if (currentRetry < this.mMaxAttempts) {
            long nextDelay = (long) (this.mFirstDelay * Math.pow(this.mBackoffBase, currentRetry));
            this.mHandler.postDelayed(this.mRunnable, nextDelay);
        } else {
            this.mCurrentRetry.set(0);
        }
        return false;
    }
}
