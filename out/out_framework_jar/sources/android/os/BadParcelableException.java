package android.os;

import android.util.AndroidRuntimeException;
/* loaded from: classes2.dex */
public class BadParcelableException extends AndroidRuntimeException {
    public BadParcelableException(String msg) {
        super(msg);
    }

    public BadParcelableException(Exception cause) {
        super(cause);
    }

    public BadParcelableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
