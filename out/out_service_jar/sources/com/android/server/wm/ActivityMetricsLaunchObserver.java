package com.android.server.wm;

import android.content.ComponentName;
import android.content.Intent;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
public class ActivityMetricsLaunchObserver {
    public static final int TEMPERATURE_COLD = 1;
    public static final int TEMPERATURE_HOT = 3;
    public static final int TEMPERATURE_WARM = 2;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface Temperature {
    }

    public void onIntentStarted(Intent intent, long timestampNanos) {
    }

    public void onIntentFailed(long id) {
    }

    public void onActivityLaunched(long id, ComponentName name, int temperature) {
    }

    public void onActivityLaunchCancelled(long id) {
    }

    public void onActivityLaunchFinished(long id, ComponentName name, long timestampNanos) {
    }

    public void onReportFullyDrawn(long id, long timestampNanos) {
    }
}
