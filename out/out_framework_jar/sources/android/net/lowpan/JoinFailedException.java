package android.net.lowpan;
/* loaded from: classes2.dex */
public class JoinFailedException extends LowpanException {
    public JoinFailedException() {
    }

    public JoinFailedException(String message) {
        super(message);
    }

    public JoinFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public JoinFailedException(Exception cause) {
        super(cause);
    }
}
