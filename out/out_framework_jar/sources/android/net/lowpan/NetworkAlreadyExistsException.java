package android.net.lowpan;
/* loaded from: classes2.dex */
public class NetworkAlreadyExistsException extends LowpanException {
    public NetworkAlreadyExistsException() {
    }

    public NetworkAlreadyExistsException(String message) {
        super(message, null);
    }

    public NetworkAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkAlreadyExistsException(Exception cause) {
        super(cause);
    }
}
