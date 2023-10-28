package android.os;
/* loaded from: classes2.dex */
public abstract class BatteryManagerInternal {
    public abstract int getBatteryChargeCounter();

    public abstract int getBatteryFullCharge();

    public abstract int getBatteryLevel();

    public abstract boolean getBatteryLevelLow();

    public abstract int getInvalidCharger();

    public abstract int getPlugType();

    public abstract boolean isPowered(int i);

    public abstract void resetBattery(boolean z);

    public abstract void setBatteryLevel(int i, boolean z);

    public abstract void setChargerAcOnline(boolean z, boolean z2);

    public abstract void suspendBatteryInput();

    public abstract void unplugBattery(boolean z);
}
