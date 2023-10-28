package com.android.server.power;

import android.hardware.display.DisplayManagerInternal;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.Trace;
import android.util.Slog;
import com.android.internal.util.LatencyTracker;
import com.transsion.hubcore.server.power.ITranPowerManagerService;
/* loaded from: classes2.dex */
public class PowerGroup {
    private static final boolean DEBUG = false;
    private static final String TAG = PowerGroup.class.getSimpleName();
    private final DisplayManagerInternal mDisplayManagerInternal;
    final DisplayManagerInternal.DisplayPowerRequest mDisplayPowerRequest;
    private final int mGroupId;
    private boolean mIsSandmanSummoned;
    private long mLastPowerOnTime;
    private long mLastSleepTime;
    private long mLastUserActivityTime;
    private long mLastUserActivityTimeNoChangeLights;
    private long mLastWakeTime;
    private final Notifier mNotifier;
    private boolean mPoweringOn;
    private boolean mReady;
    private boolean mShutdownFlag;
    private final boolean mSupportsSandman;
    private int mUserActivitySummary;
    private int mWakeLockSummary;
    private int mWakefulness;
    private final PowerGroupListener mWakefulnessListener;

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public interface PowerGroupListener {
        void onWakefulnessChangedLocked(int i, int i2, long j, int i3, int i4, int i5, String str, String str2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PowerGroup(int groupId, PowerGroupListener wakefulnessListener, Notifier notifier, DisplayManagerInternal displayManagerInternal, int wakefulness, boolean ready, boolean supportsSandman, long eventTime) {
        this.mDisplayPowerRequest = new DisplayManagerInternal.DisplayPowerRequest();
        this.mShutdownFlag = false;
        this.mGroupId = groupId;
        this.mWakefulnessListener = wakefulnessListener;
        this.mNotifier = notifier;
        this.mDisplayManagerInternal = displayManagerInternal;
        this.mWakefulness = wakefulness;
        this.mReady = ready;
        this.mSupportsSandman = supportsSandman;
        this.mLastWakeTime = eventTime;
        this.mLastSleepTime = eventTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PowerGroup(int wakefulness, PowerGroupListener wakefulnessListener, Notifier notifier, DisplayManagerInternal displayManagerInternal, long eventTime) {
        this.mDisplayPowerRequest = new DisplayManagerInternal.DisplayPowerRequest();
        this.mShutdownFlag = false;
        this.mGroupId = 0;
        this.mWakefulnessListener = wakefulnessListener;
        this.mNotifier = notifier;
        this.mDisplayManagerInternal = displayManagerInternal;
        this.mWakefulness = wakefulness;
        this.mReady = false;
        this.mSupportsSandman = true;
        this.mLastWakeTime = eventTime;
        this.mLastSleepTime = eventTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastWakeTimeLocked() {
        return this.mLastWakeTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastSleepTimeLocked() {
        return this.mLastSleepTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getWakefulnessLocked() {
        return this.mWakefulness;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getGroupId() {
        return this.mGroupId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setWakefulnessLocked(int newWakefulness, long eventTime, int uid, int reason, int opUid, String opPackageName, String details) {
        int i = this.mWakefulness;
        if (i != newWakefulness) {
            if (newWakefulness == 1) {
                setLastPowerOnTimeLocked(eventTime);
                setIsPoweringOnLocked(true);
                this.mLastWakeTime = eventTime;
            } else if (PowerManagerInternal.isInteractive(i) && !PowerManagerInternal.isInteractive(newWakefulness)) {
                this.mLastSleepTime = eventTime;
            }
            this.mWakefulness = newWakefulness;
            this.mWakefulnessListener.onWakefulnessChangedLocked(this.mGroupId, newWakefulness, eventTime, reason, uid, opUid, opPackageName, details);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isReadyLocked() {
        return this.mReady;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setReadyLocked(boolean isReady) {
        if (this.mReady != isReady) {
            this.mReady = isReady;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastPowerOnTimeLocked() {
        return this.mLastPowerOnTime;
    }

    void setLastPowerOnTimeLocked(long time) {
        this.mLastPowerOnTime = time;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPoweringOnLocked() {
        return this.mPoweringOn;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsPoweringOnLocked(boolean isPoweringOnNew) {
        this.mPoweringOn = isPoweringOnNew;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSandmanSummonedLocked() {
        return this.mIsSandmanSummoned;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSandmanSummonedLocked(boolean isSandmanSummoned) {
        this.mIsSandmanSummoned = isSandmanSummoned;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void wakeUpLocked(long eventTime, int reason, String details, int uid, String opPackageName, int opUid, LatencyTracker latencyTracker) {
        if (eventTime >= this.mLastSleepTime && this.mWakefulness != 1) {
            Trace.traceBegin(131072L, "wakePowerGroup" + this.mGroupId);
            try {
                try {
                    try {
                        Slog.i(TAG, "Waking up power group from " + PowerManagerInternal.wakefulnessToString(this.mWakefulness) + " (groupId=" + this.mGroupId + ", uid=" + uid + ", reason=" + PowerManager.wakeReasonToString(reason) + ", details=" + details + ")...");
                        Trace.asyncTraceBegin(131072L, "Screen turning on", this.mGroupId);
                        try {
                            latencyTracker.onActionStart(5, String.valueOf(this.mGroupId));
                            setWakefulnessLocked(1, eventTime, uid, reason, opUid, opPackageName, details);
                            Trace.traceEnd(131072L);
                        } catch (Throwable th) {
                            th = th;
                            Trace.traceEnd(131072L);
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        Trace.traceEnd(131072L);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dreamLocked(long eventTime, int uid) {
        if (eventTime >= this.mLastWakeTime && this.mWakefulness == 1) {
            Trace.traceBegin(131072L, "dreamPowerGroup" + getGroupId());
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                Slog.i(TAG, "Napping power group (groupId=" + getGroupId() + ", uid=" + uid + ")...");
                setSandmanSummonedLocked(true);
                setWakefulnessLocked(2, eventTime, uid, 0, 0, null, null);
                Trace.traceEnd(131072L);
                return true;
            } catch (Throwable th2) {
                th = th2;
                Trace.traceEnd(131072L);
                throw th;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dozeLocked(long eventTime, int uid, int reason) {
        if (eventTime >= getLastWakeTimeLocked() && PowerManagerInternal.isInteractive(this.mWakefulness)) {
            Trace.traceBegin(131072L, "powerOffDisplay");
            try {
                int reason2 = Math.min(13, Math.max(reason, 0));
                try {
                    try {
                        Slog.i(TAG, "Powering off display group due to " + PowerManager.sleepReasonToString(reason2) + " (groupId= " + getGroupId() + ", uid= " + uid + ")...");
                        setSandmanSummonedLocked(true);
                        setWakefulnessLocked(3, eventTime, uid, reason2, 0, null, null);
                        Trace.traceEnd(131072L);
                        return true;
                    } catch (Throwable th) {
                        th = th;
                        Trace.traceEnd(131072L);
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean sleepLocked(long eventTime, int uid, int reason) {
        if (reason == 8) {
            this.mShutdownFlag = true;
            return true;
        }
        if (eventTime >= this.mLastWakeTime && getWakefulnessLocked() != 0) {
            Trace.traceBegin(131072L, "sleepPowerGroup");
            try {
                try {
                    Slog.i(TAG, "Sleeping power group (groupId=" + getGroupId() + ", uid=" + uid + ")...");
                    setSandmanSummonedLocked(true);
                    setWakefulnessLocked(0, eventTime, uid, reason, 0, null, null);
                    Trace.traceEnd(131072L);
                    return true;
                } catch (Throwable th) {
                    th = th;
                    Trace.traceEnd(131072L);
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastUserActivityTimeLocked() {
        return this.mLastUserActivityTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastUserActivityTimeLocked(long lastUserActivityTime) {
        this.mLastUserActivityTime = lastUserActivityTime;
    }

    public long getLastUserActivityTimeNoChangeLightsLocked() {
        return this.mLastUserActivityTimeNoChangeLights;
    }

    public void setLastUserActivityTimeNoChangeLightsLocked(long time) {
        this.mLastUserActivityTimeNoChangeLights = time;
    }

    public int getUserActivitySummaryLocked() {
        return this.mUserActivitySummary;
    }

    public boolean isPolicyBrightLocked() {
        return this.mDisplayPowerRequest.policy == 3;
    }

    public boolean isPolicyDimLocked() {
        return this.mDisplayPowerRequest.policy == 2;
    }

    public boolean isPolicyVrLocked() {
        return this.mDisplayPowerRequest.isVr();
    }

    public boolean isBrightOrDimLocked() {
        return this.mDisplayPowerRequest.isBrightOrDim();
    }

    public void setUserActivitySummaryLocked(int summary) {
        this.mUserActivitySummary = summary;
    }

    public int getWakeLockSummaryLocked() {
        return this.mWakeLockSummary;
    }

    public void setWakeLockSummaryLocked(int summary) {
        this.mWakeLockSummary = summary;
    }

    public boolean supportsSandmanLocked() {
        return this.mSupportsSandman;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean needSuspendBlockerLocked(boolean proximityPositive, boolean suspendWhenScreenOffDueToProximityConfig) {
        if (!isBrightOrDimLocked() || (this.mDisplayPowerRequest.useProximitySensor && proximityPositive && suspendWhenScreenOffDueToProximityConfig)) {
            return this.mDisplayPowerRequest.policy == 1 && this.mDisplayPowerRequest.dozeScreenState == 2;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDesiredScreenPolicyLocked(boolean quiescent, boolean dozeAfterScreenOff, boolean vrModeEnabled, boolean bootCompleted, boolean screenBrightnessBoostInProgress) {
        int wakefulness = getWakefulnessLocked();
        int wakeLockSummary = getWakeLockSummaryLocked();
        if (wakefulness == 0 || quiescent || this.mShutdownFlag) {
            return 0;
        }
        if (wakefulness == 3) {
            if ((wakeLockSummary & 64) != 0) {
                return 1;
            }
            if (dozeAfterScreenOff && !ITranPowerManagerService.Instance().isNeedAodTransparent()) {
                return 0;
            }
        }
        if (wakefulness == 4) {
            return 5;
        }
        if (!ITranPowerManagerService.Instance().getIsConnectSource() || (getUserActivitySummaryLocked() & 2) == 0) {
            if (vrModeEnabled) {
                return 4;
            }
            return ((wakeLockSummary & 2) == 0 && bootCompleted && (getUserActivitySummaryLocked() & 1) == 0 && !screenBrightnessBoostInProgress) ? 2 : 3;
        }
        return 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPolicyLocked() {
        return this.mDisplayPowerRequest.policy;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateLocked(float screenBrightnessOverride, boolean autoBrightness, boolean useProximitySensor, boolean boostScreenBrightness, int dozeScreenState, float dozeScreenBrightness, boolean overrideDrawWakeLock, PowerSaveState powerSaverState, boolean quiescent, boolean dozeAfterScreenOff, boolean vrModeEnabled, boolean bootCompleted, boolean screenBrightnessBoostInProgress, boolean waitForNegativeProximity) {
        this.mDisplayPowerRequest.policy = getDesiredScreenPolicyLocked(quiescent, dozeAfterScreenOff, vrModeEnabled, bootCompleted, screenBrightnessBoostInProgress);
        this.mDisplayPowerRequest.screenBrightnessOverride = screenBrightnessOverride;
        this.mDisplayPowerRequest.useAutoBrightness = autoBrightness;
        this.mDisplayPowerRequest.useProximitySensor = useProximitySensor;
        this.mDisplayPowerRequest.boostScreenBrightness = boostScreenBrightness;
        if (this.mDisplayPowerRequest.policy != 1) {
            this.mDisplayPowerRequest.dozeScreenState = 0;
            this.mDisplayPowerRequest.dozeScreenBrightness = Float.NaN;
        } else {
            this.mDisplayPowerRequest.dozeScreenState = dozeScreenState;
            if ((getWakeLockSummaryLocked() & 128) != 0 && !overrideDrawWakeLock) {
                if (this.mDisplayPowerRequest.dozeScreenState == 4) {
                    this.mDisplayPowerRequest.dozeScreenState = 3;
                }
                if (this.mDisplayPowerRequest.dozeScreenState == 6) {
                    this.mDisplayPowerRequest.dozeScreenState = 2;
                }
            }
            this.mDisplayPowerRequest.dozeScreenBrightness = dozeScreenBrightness;
        }
        this.mDisplayPowerRequest.lowPowerMode = powerSaverState.batterySaverEnabled;
        this.mDisplayPowerRequest.screenLowPowerBrightnessFactor = powerSaverState.brightnessFactor;
        boolean ready = this.mDisplayManagerInternal.requestPowerState(this.mGroupId, this.mDisplayPowerRequest, waitForNegativeProximity);
        this.mNotifier.onScreenPolicyUpdate(this.mGroupId, this.mDisplayPowerRequest.policy);
        return ready;
    }
}
