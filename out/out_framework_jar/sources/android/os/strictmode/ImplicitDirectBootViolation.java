package android.os.strictmode;
/* loaded from: classes2.dex */
public final class ImplicitDirectBootViolation extends Violation {
    public ImplicitDirectBootViolation() {
        super("Implicitly relying on automatic Direct Boot filtering; request explicit filtering with PackageManager.MATCH_DIRECT_BOOT flags");
    }
}
