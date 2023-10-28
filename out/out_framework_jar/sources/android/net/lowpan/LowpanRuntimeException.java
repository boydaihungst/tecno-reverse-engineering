package android.net.lowpan;

import android.util.AndroidRuntimeException;
/* loaded from: classes2.dex */
public class LowpanRuntimeException extends AndroidRuntimeException {
    public LowpanRuntimeException() {
    }

    public LowpanRuntimeException(String message) {
        super(message);
    }

    public LowpanRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public LowpanRuntimeException(Exception cause) {
        super(cause);
    }
}
