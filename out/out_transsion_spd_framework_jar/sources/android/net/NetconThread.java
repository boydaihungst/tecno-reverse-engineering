package android.net;

import android.os.HandlerThread;
import android.os.Looper;
/* loaded from: classes.dex */
public final class NetconThread extends HandlerThread {
    static /* synthetic */ NetconThread access$000() {
        return createInstance();
    }

    /* loaded from: classes.dex */
    private static class Singleton {
        private static final NetconThread INSTANCE = NetconThread.access$000();

        private Singleton() {
        }
    }

    private NetconThread() {
        super("NetconThread");
    }

    private static NetconThread createInstance() {
        NetconThread t = new NetconThread();
        t.start();
        return t;
    }

    public static NetconThread get() {
        return Singleton.INSTANCE;
    }

    public static Looper getInstanceLooper() {
        return Singleton.INSTANCE.getLooper();
    }
}
