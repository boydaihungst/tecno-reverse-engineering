package android.net.lowpan;
/* loaded from: classes2.dex */
public class WrongStateException extends LowpanException {
    public WrongStateException() {
    }

    public WrongStateException(String message) {
        super(message);
    }

    public WrongStateException(String message, Throwable cause) {
        super(message, cause);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public WrongStateException(Exception cause) {
        super(cause);
    }
}
