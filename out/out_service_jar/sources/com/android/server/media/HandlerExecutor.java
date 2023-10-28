package com.android.server.media;

import android.os.Handler;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
/* loaded from: classes2.dex */
public class HandlerExecutor implements Executor {
    private final Handler mHandler;

    public HandlerExecutor(Handler handler) {
        this.mHandler = (Handler) Objects.requireNonNull(handler);
    }

    @Override // java.util.concurrent.Executor
    public void execute(Runnable command) {
        if (!this.mHandler.post(command)) {
            throw new RejectedExecutionException(this.mHandler + " is shutting down");
        }
    }
}
