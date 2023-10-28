package com.android.server.timezonedetector.location;

import android.os.Handler;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
/* loaded from: classes2.dex */
final class HandlerThreadingDomain extends ThreadingDomain {
    private final Handler mHandler;

    /* JADX INFO: Access modifiers changed from: package-private */
    public HandlerThreadingDomain(Handler handler) {
        this.mHandler = (Handler) Objects.requireNonNull(handler);
    }

    Handler getHandler() {
        return this.mHandler;
    }

    @Override // com.android.server.timezonedetector.location.ThreadingDomain
    Thread getThread() {
        return getHandler().getLooper().getThread();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.timezonedetector.location.ThreadingDomain
    public void post(Runnable r) {
        getHandler().post(r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.timezonedetector.location.ThreadingDomain
    public <V> V postAndWait(final Callable<V> callable, long durationMillis) throws Exception {
        assertNotCurrentThread();
        final AtomicReference<V> resultReference = new AtomicReference<>();
        final AtomicReference<Exception> exceptionReference = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        post(new Runnable() { // from class: com.android.server.timezonedetector.location.HandlerThreadingDomain$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                HandlerThreadingDomain.lambda$postAndWait$0(resultReference, callable, exceptionReference, latch);
            }
        });
        try {
            if (!latch.await(durationMillis, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Timed out");
            } else if (exceptionReference.get() != null) {
                throw exceptionReference.get();
            } else {
                return resultReference.get();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$postAndWait$0(AtomicReference resultReference, Callable callable, AtomicReference exceptionReference, CountDownLatch latch) {
        try {
            try {
                resultReference.set(callable.call());
            } catch (Exception e) {
                exceptionReference.set(e);
            }
        } finally {
            latch.countDown();
        }
    }

    @Override // com.android.server.timezonedetector.location.ThreadingDomain
    void postDelayed(Runnable r, long delayMillis) {
        getHandler().postDelayed(r, delayMillis);
    }

    @Override // com.android.server.timezonedetector.location.ThreadingDomain
    void postDelayed(Runnable r, Object token, long delayMillis) {
        getHandler().postDelayed(r, token, delayMillis);
    }

    @Override // com.android.server.timezonedetector.location.ThreadingDomain
    void removeQueuedRunnables(Object token) {
        getHandler().removeCallbacksAndMessages(token);
    }
}
