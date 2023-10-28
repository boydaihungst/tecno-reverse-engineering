package com.android.internal.os;

import android.os.BatteryStats;
/* loaded from: classes4.dex */
public class UsageBasedPowerEstimator {
    private static final double MILLIS_IN_HOUR = 3600000.0d;
    private final double mAveragePowerMahPerMs;

    public UsageBasedPowerEstimator(double averagePowerMilliAmp) {
        this.mAveragePowerMahPerMs = averagePowerMilliAmp / 3600000.0d;
    }

    public boolean isSupported() {
        return this.mAveragePowerMahPerMs != 0.0d;
    }

    public long calculateDuration(BatteryStats.Timer timer, long rawRealtimeUs, int statsType) {
        if (timer == null) {
            return 0L;
        }
        return timer.getTotalTimeLocked(rawRealtimeUs, statsType) / 1000;
    }

    public double calculatePower(long durationMs) {
        return this.mAveragePowerMahPerMs * durationMs;
    }
}
