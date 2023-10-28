package android.net.lowpan;
/* loaded from: classes2.dex */
public class OperationCanceledException extends LowpanException {
    public OperationCanceledException() {
    }

    public OperationCanceledException(String message) {
        super(message);
    }

    public OperationCanceledException(String message, Throwable cause) {
        super(message, cause);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public OperationCanceledException(Exception cause) {
        super(cause);
    }
}
