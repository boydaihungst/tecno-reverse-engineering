package com.android.server.tare;

import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.os.Looper;
import java.util.concurrent.Executor;
/* loaded from: classes2.dex */
final class TareHandlerThread extends HandlerThread {
    private static Handler sHandler;
    private static Executor sHandlerExecutor;
    private static TareHandlerThread sInstance;

    private TareHandlerThread() {
        super("tare");
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            TareHandlerThread tareHandlerThread = new TareHandlerThread();
            sInstance = tareHandlerThread;
            tareHandlerThread.start();
            Looper looper = sInstance.getLooper();
            looper.setTraceTag(524288L);
            sHandler = new Handler(sInstance.getLooper());
            sHandlerExecutor = new HandlerExecutor(sHandler);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static TareHandlerThread get() {
        synchronized (TareHandlerThread.class) {
            ensureThreadLocked();
        }
        return sInstance;
    }

    public static Executor getExecutor() {
        Executor executor;
        synchronized (TareHandlerThread.class) {
            ensureThreadLocked();
            executor = sHandlerExecutor;
        }
        return executor;
    }

    public static Handler getHandler() {
        synchronized (TareHandlerThread.class) {
            ensureThreadLocked();
        }
        return sHandler;
    }
}
