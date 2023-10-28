package com.android.server.hdmi;

import android.os.PowerManagerInternal;
import com.android.server.LocalServices;
/* loaded from: classes.dex */
public class PowerManagerInternalWrapper {
    private static final String TAG = "PowerManagerInternalWrapper";
    private PowerManagerInternal mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);

    public boolean wasDeviceIdleFor(long ms) {
        return this.mPowerManagerInternal.wasDeviceIdleFor(ms);
    }
}
