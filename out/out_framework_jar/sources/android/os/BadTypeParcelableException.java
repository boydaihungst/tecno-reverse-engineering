package android.os;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class BadTypeParcelableException extends BadParcelableException {
    /* JADX INFO: Access modifiers changed from: package-private */
    public BadTypeParcelableException(String msg) {
        super(msg);
    }

    BadTypeParcelableException(Exception cause) {
        super(cause);
    }

    BadTypeParcelableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
