package com.android.server.sensorprivacy;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SensorState {
    private long mLastChange;
    private int mStateType;

    SensorState(int stateType) {
        this.mStateType = stateType;
        this.mLastChange = SensorPrivacyService.getCurrentTimeMillis();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SensorState(int stateType, long lastChange) {
        this.mStateType = stateType;
        this.mLastChange = Math.min(SensorPrivacyService.getCurrentTimeMillis(), lastChange);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SensorState(SensorState sensorState) {
        this.mStateType = sensorState.getState();
        this.mLastChange = sensorState.getLastChange();
    }

    boolean setState(int stateType) {
        if (this.mStateType != stateType) {
            this.mStateType = stateType;
            this.mLastChange = SensorPrivacyService.getCurrentTimeMillis();
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getState() {
        return this.mStateType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastChange() {
        return this.mLastChange;
    }

    private static int enabledToState(boolean enabled) {
        return enabled ? 1 : 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SensorState(boolean enabled) {
        this(enabledToState(enabled));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setEnabled(boolean enabled) {
        return setState(enabledToState(enabled));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEnabled() {
        return getState() == 1;
    }
}
