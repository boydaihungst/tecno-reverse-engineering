package android.os.strictmode;
/* loaded from: classes2.dex */
public final class IncorrectContextUseViolation extends Violation {
    public IncorrectContextUseViolation(String message, Throwable originStack) {
        super(message);
        initCause(originStack);
    }
}
