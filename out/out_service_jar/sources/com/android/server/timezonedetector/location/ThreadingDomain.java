package com.android.server.timezonedetector.location;

import com.android.internal.util.Preconditions;
import com.android.server.timezonedetector.location.ThreadingDomain;
import java.util.concurrent.Callable;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public abstract class ThreadingDomain {
    private final Object mLockObject = new Object();

    abstract Thread getThread();

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract void post(Runnable runnable);

    /* JADX INFO: Access modifiers changed from: package-private */
    public abstract <V> V postAndWait(Callable<V> callable, long j) throws Exception;

    abstract void postDelayed(Runnable runnable, long j);

    abstract void postDelayed(Runnable runnable, Object obj, long j);

    abstract void removeQueuedRunnables(Object obj);

    /* JADX INFO: Access modifiers changed from: package-private */
    public Object getLockObject() {
        return this.mLockObject;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assertCurrentThread() {
        Preconditions.checkState(Thread.currentThread() == getThread());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assertNotCurrentThread() {
        Preconditions.checkState(Thread.currentThread() != getThread());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void postAndWait(final Runnable runnable, long durationMillis) {
        try {
            postAndWait(new Callable() { // from class: com.android.server.timezonedetector.location.ThreadingDomain$$ExternalSyntheticLambda0
                @Override // java.util.concurrent.Callable
                public final Object call() {
                    return ThreadingDomain.lambda$postAndWait$0(runnable);
                }
            }, durationMillis);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Object lambda$postAndWait$0(Runnable runnable) throws Exception {
        runnable.run();
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SingleRunnableQueue createSingleRunnableQueue() {
        return new SingleRunnableQueue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class SingleRunnableQueue {
        private long mDelayMillis;
        private boolean mIsQueued;

        SingleRunnableQueue() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void runDelayed(final Runnable r, long delayMillis) {
            cancel();
            this.mIsQueued = true;
            this.mDelayMillis = delayMillis;
            ThreadingDomain.this.postDelayed(new Runnable() { // from class: com.android.server.timezonedetector.location.ThreadingDomain$SingleRunnableQueue$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ThreadingDomain.SingleRunnableQueue.this.m6929xf61a35fe(r);
                }
            }, this, delayMillis);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$runDelayed$0$com-android-server-timezonedetector-location-ThreadingDomain$SingleRunnableQueue  reason: not valid java name */
        public /* synthetic */ void m6929xf61a35fe(Runnable r) {
            this.mIsQueued = false;
            this.mDelayMillis = -2L;
            r.run();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean hasQueued() {
            ThreadingDomain.this.assertCurrentThread();
            return this.mIsQueued;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public long getQueuedDelayMillis() {
            ThreadingDomain.this.assertCurrentThread();
            if (!this.mIsQueued) {
                throw new IllegalStateException("No item queued");
            }
            return this.mDelayMillis;
        }

        public void cancel() {
            ThreadingDomain.this.assertCurrentThread();
            if (this.mIsQueued) {
                ThreadingDomain.this.removeQueuedRunnables(this);
            }
            this.mIsQueued = false;
            this.mDelayMillis = -1L;
        }
    }
}
