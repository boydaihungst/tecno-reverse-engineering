package com.android.server;
/* loaded from: classes.dex */
public class AppFuseMountException extends Exception {
    public AppFuseMountException(String detailMessage) {
        super(detailMessage);
    }

    public AppFuseMountException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public IllegalArgumentException rethrowAsParcelableException() {
        throw new IllegalStateException(getMessage(), this);
    }
}
