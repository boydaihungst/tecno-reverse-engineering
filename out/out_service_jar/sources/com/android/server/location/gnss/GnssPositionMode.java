package com.android.server.location.gnss;

import java.util.Arrays;
/* loaded from: classes.dex */
public class GnssPositionMode {
    private final boolean mLowPowerMode;
    private final int mMinInterval;
    private final int mMode;
    private final int mPreferredAccuracy;
    private final int mPreferredTime;
    private final int mRecurrence;

    public GnssPositionMode(int mode, int recurrence, int minInterval, int preferredAccuracy, int preferredTime, boolean lowPowerMode) {
        this.mMode = mode;
        this.mRecurrence = recurrence;
        this.mMinInterval = minInterval;
        this.mPreferredAccuracy = preferredAccuracy;
        this.mPreferredTime = preferredTime;
        this.mLowPowerMode = lowPowerMode;
    }

    public boolean equals(Object other) {
        if (other instanceof GnssPositionMode) {
            GnssPositionMode that = (GnssPositionMode) other;
            return this.mMode == that.mMode && this.mRecurrence == that.mRecurrence && this.mMinInterval == that.mMinInterval && this.mPreferredAccuracy == that.mPreferredAccuracy && this.mPreferredTime == that.mPreferredTime && this.mLowPowerMode == that.mLowPowerMode && getClass() == that.getClass();
        }
        return false;
    }

    public int hashCode() {
        return Arrays.hashCode(new Object[]{Integer.valueOf(this.mMode), Integer.valueOf(this.mRecurrence), Integer.valueOf(this.mMinInterval), Integer.valueOf(this.mPreferredAccuracy), Integer.valueOf(this.mPreferredTime), Boolean.valueOf(this.mLowPowerMode), getClass()});
    }
}
