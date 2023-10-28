package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import com.android.server.location.gnss.hal.GnssNative;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class HdmiMhlLocalDeviceStub {
    private static final HdmiDeviceInfo INFO = HdmiDeviceInfo.mhlDevice((int) GnssNative.GNSS_AIDING_TYPE_ALL, -1, -1, -1);
    private final int mPortId;
    private final HdmiControlService mService;

    /* JADX INFO: Access modifiers changed from: protected */
    public HdmiMhlLocalDeviceStub(HdmiControlService service, int portId) {
        this.mService = service;
        this.mPortId = portId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDeviceRemoved() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiDeviceInfo getInfo() {
        return INFO;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBusMode(int cbusmode) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBusOvercurrentDetected(boolean on) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeviceStatusChange(int adopterId, int deviceId) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPortId() {
        return this.mPortId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void turnOn(IHdmiControlCallback callback) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendKeyEvent(int keycode, boolean isPressed) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendStandby() {
    }
}
