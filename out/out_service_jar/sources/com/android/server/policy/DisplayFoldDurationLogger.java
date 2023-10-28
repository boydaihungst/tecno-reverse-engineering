package com.android.server.policy;

import android.metrics.LogMaker;
import android.os.SystemClock;
import com.android.internal.logging.MetricsLogger;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes2.dex */
class DisplayFoldDurationLogger {
    private static final int LOG_SUBTYPE_DURATION_MASK = Integer.MIN_VALUE;
    private static final int LOG_SUBTYPE_FOLDED = 1;
    private static final int LOG_SUBTYPE_UNFOLDED = 0;
    static final int SCREEN_STATE_OFF = 0;
    static final int SCREEN_STATE_ON_FOLDED = 2;
    static final int SCREEN_STATE_ON_UNFOLDED = 1;
    static final int SCREEN_STATE_UNKNOWN = -1;
    private volatile int mScreenState = -1;
    private volatile Long mLastChanged = null;
    private final MetricsLogger mLogger = new MetricsLogger();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface ScreenState {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onFinishedWakingUp(Boolean folded) {
        if (folded == null) {
            this.mScreenState = -1;
        } else if (folded.booleanValue()) {
            this.mScreenState = 2;
        } else {
            this.mScreenState = 1;
        }
        this.mLastChanged = Long.valueOf(SystemClock.uptimeMillis());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onFinishedGoingToSleep() {
        log();
        this.mScreenState = 0;
        this.mLastChanged = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeviceFolded(boolean folded) {
        if (!isOn()) {
            return;
        }
        log();
        this.mScreenState = folded ? 2 : 1;
        this.mLastChanged = Long.valueOf(SystemClock.uptimeMillis());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logFocusedAppWithFoldState(boolean folded, String packageName) {
        this.mLogger.write(new LogMaker(1594).setType(4).setSubtype(folded ? 1 : 0).setPackageName(packageName));
    }

    private void log() {
        int subtype;
        if (this.mLastChanged == null) {
            return;
        }
        switch (this.mScreenState) {
            case 1:
                subtype = Integer.MIN_VALUE;
                break;
            case 2:
                subtype = -2147483647;
                break;
            default:
                return;
        }
        this.mLogger.write(new LogMaker(1594).setType(4).setSubtype(subtype).setLatency(SystemClock.uptimeMillis() - this.mLastChanged.longValue()));
    }

    private boolean isOn() {
        return this.mScreenState == 1 || this.mScreenState == 2;
    }
}
