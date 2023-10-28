package com.android.server.hdmi;

import android.content.Context;
import android.os.PowerManager;
/* loaded from: classes.dex */
public class PowerManagerWrapper {
    private static final String TAG = "PowerManagerWrapper";
    private final PowerManager mPowerManager;

    public PowerManagerWrapper(Context context) {
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInteractive() {
        return this.mPowerManager.isInteractive();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void wakeUp(long time, int reason, String details) {
        this.mPowerManager.wakeUp(time, reason, details);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void goToSleep(long time, int reason, int flags) {
        this.mPowerManager.goToSleep(time, reason, flags);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PowerManager.WakeLock newWakeLock(int levelAndFlags, String tag) {
        return this.mPowerManager.newWakeLock(levelAndFlags, tag);
    }
}
