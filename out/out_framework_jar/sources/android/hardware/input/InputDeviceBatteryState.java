package android.hardware.input;

import android.hardware.BatteryState;
/* loaded from: classes2.dex */
public final class InputDeviceBatteryState extends BatteryState {
    private static final float NULL_BATTERY_CAPACITY = Float.NaN;
    private final int mDeviceId;
    private final boolean mHasBattery;
    private final InputManager mInputManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputDeviceBatteryState(InputManager inputManager, int deviceId, boolean hasBattery) {
        this.mInputManager = inputManager;
        this.mDeviceId = deviceId;
        this.mHasBattery = hasBattery;
    }

    @Override // android.hardware.BatteryState
    public boolean isPresent() {
        return this.mHasBattery;
    }

    @Override // android.hardware.BatteryState
    public int getStatus() {
        if (!this.mHasBattery) {
            return 1;
        }
        return this.mInputManager.getBatteryStatus(this.mDeviceId);
    }

    @Override // android.hardware.BatteryState
    public float getCapacity() {
        int capacity;
        if (this.mHasBattery && (capacity = this.mInputManager.getBatteryCapacity(this.mDeviceId)) != -1) {
            return capacity / 100.0f;
        }
        return Float.NaN;
    }
}
