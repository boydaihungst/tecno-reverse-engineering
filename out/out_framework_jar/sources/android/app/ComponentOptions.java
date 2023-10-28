package android.app;

import android.os.Bundle;
/* loaded from: classes.dex */
public class ComponentOptions {
    public static final String KEY_PENDING_INTENT_BACKGROUND_ACTIVITY_ALLOWED = "android.pendingIntent.backgroundActivityAllowed";
    public static final String KEY_PENDING_INTENT_BACKGROUND_ACTIVITY_ALLOWED_BY_PERMISSION = "android.pendingIntent.backgroundActivityAllowedByPermission";
    public static final boolean PENDING_INTENT_BAL_ALLOWED_DEFAULT = true;
    private boolean mPendingIntentBalAllowed = true;
    private boolean mPendingIntentBalAllowedByPermission = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentOptions() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentOptions(Bundle opts) {
        opts.setDefusable(true);
        setPendingIntentBackgroundActivityLaunchAllowed(opts.getBoolean(KEY_PENDING_INTENT_BACKGROUND_ACTIVITY_ALLOWED, true));
        setPendingIntentBackgroundActivityLaunchAllowedByPermission(opts.getBoolean(KEY_PENDING_INTENT_BACKGROUND_ACTIVITY_ALLOWED_BY_PERMISSION, false));
    }

    public void setPendingIntentBackgroundActivityLaunchAllowed(boolean allowed) {
        this.mPendingIntentBalAllowed = allowed;
    }

    public boolean isPendingIntentBackgroundActivityLaunchAllowed() {
        return this.mPendingIntentBalAllowed;
    }

    public void setPendingIntentBackgroundActivityLaunchAllowedByPermission(boolean allowed) {
        this.mPendingIntentBalAllowedByPermission = allowed;
    }

    public boolean isPendingIntentBackgroundActivityLaunchAllowedByPermission() {
        return this.mPendingIntentBalAllowedByPermission;
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putBoolean(KEY_PENDING_INTENT_BACKGROUND_ACTIVITY_ALLOWED, this.mPendingIntentBalAllowed);
        boolean z = this.mPendingIntentBalAllowedByPermission;
        if (z) {
            b.putBoolean(KEY_PENDING_INTENT_BACKGROUND_ACTIVITY_ALLOWED_BY_PERMISSION, z);
        }
        return b;
    }
}
