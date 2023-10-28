package com.android.server.biometrics.sensors;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes.dex */
public interface LockoutTracker {
    public static final int LOCKOUT_NONE = 0;
    public static final int LOCKOUT_PERMANENT = 2;
    public static final int LOCKOUT_TIMED = 1;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface LockoutMode {
    }

    int getLockoutModeForUser(int i);
}
