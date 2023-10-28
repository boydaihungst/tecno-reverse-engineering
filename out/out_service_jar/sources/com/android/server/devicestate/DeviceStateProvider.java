package com.android.server.devicestate;
/* loaded from: classes.dex */
public interface DeviceStateProvider {

    /* loaded from: classes.dex */
    public interface Listener {
        void onStateChanged(int i);

        void onSupportedDeviceStatesChanged(DeviceState[] deviceStateArr);
    }

    void onSystemBootedEnd();

    void setHallKeyStateUp(boolean z);

    void setListener(Listener listener);
}
