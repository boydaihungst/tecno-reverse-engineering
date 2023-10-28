package android.os;
/* loaded from: classes2.dex */
public class DeadSystemRuntimeException extends RuntimeException {
    public DeadSystemRuntimeException() {
        super(new DeadSystemException());
    }
}
