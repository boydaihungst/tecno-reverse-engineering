package android.os.strictmode;
/* loaded from: classes2.dex */
public final class SqliteObjectLeakedViolation extends Violation {
    public SqliteObjectLeakedViolation(String message, Throwable originStack) {
        super(message);
        initCause(originStack);
    }
}
