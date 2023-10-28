package android.os.strictmode;
/* loaded from: classes2.dex */
public final class ServiceConnectionLeakedViolation extends Violation {
    public ServiceConnectionLeakedViolation(Throwable originStack) {
        super(null);
        setStackTrace(originStack.getStackTrace());
    }
}
