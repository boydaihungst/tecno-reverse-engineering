package android.os.strictmode;

import android.content.Intent;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class UnsafeIntentLaunchViolation extends Violation {
    private transient Intent mIntent;

    public UnsafeIntentLaunchViolation(Intent intent) {
        super("Launch of unsafe intent: " + intent);
        this.mIntent = (Intent) Objects.requireNonNull(intent);
    }

    public Intent getIntent() {
        return this.mIntent;
    }
}
