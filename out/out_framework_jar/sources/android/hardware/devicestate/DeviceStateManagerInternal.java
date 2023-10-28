package android.hardware.devicestate;
/* loaded from: classes.dex */
public abstract class DeviceStateManagerInternal {
    public abstract int getCurrentState();

    public abstract int[] getSupportedStateIdentifiers();

    public abstract void onHallKeyUp(boolean z);

    public abstract void onSystemBootedEnd();
}
