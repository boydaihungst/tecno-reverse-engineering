package org.apache.http.conn;

import java.io.InterruptedIOException;
@Deprecated
/* loaded from: classes4.dex */
public class ConnectTimeoutException extends InterruptedIOException {
    private static final long serialVersionUID = -4816682903149535989L;

    public ConnectTimeoutException() {
    }

    public ConnectTimeoutException(String message) {
        super(message);
    }
}
