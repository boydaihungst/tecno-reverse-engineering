package android.view;

import android.os.IBinder;
/* loaded from: classes3.dex */
public final class InputApplicationHandle {
    public final long dispatchingTimeoutMillis;
    public final String name;
    private long ptr;
    public final IBinder token;

    private native void nativeDispose();

    public InputApplicationHandle(IBinder token, String name, long dispatchingTimeoutMillis) {
        this.token = token;
        this.name = name;
        this.dispatchingTimeoutMillis = dispatchingTimeoutMillis;
    }

    public InputApplicationHandle(InputApplicationHandle handle) {
        this.token = handle.token;
        this.dispatchingTimeoutMillis = handle.dispatchingTimeoutMillis;
        this.name = handle.name;
    }

    protected void finalize() throws Throwable {
        try {
            nativeDispose();
        } finally {
            super.finalize();
        }
    }
}
