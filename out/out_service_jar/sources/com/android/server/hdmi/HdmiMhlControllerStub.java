package com.android.server.hdmi;

import android.hardware.hdmi.HdmiPortInfo;
import android.util.SparseArray;
import com.android.internal.util.IndentingPrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class HdmiMhlControllerStub {
    private static final int INVALID_DEVICE_ROLES = 0;
    private static final int INVALID_MHL_VERSION = 0;
    private static final int NO_SUPPORTED_FEATURES = 0;
    private static final SparseArray<HdmiMhlLocalDeviceStub> mLocalDevices = new SparseArray<>();
    private static final HdmiPortInfo[] EMPTY_PORT_INFO = new HdmiPortInfo[0];

    private HdmiMhlControllerStub(HdmiControlService service) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isReady() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiMhlControllerStub create(HdmiControlService service) {
        return new HdmiMhlControllerStub(service);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiPortInfo[] getPortInfos() {
        return EMPTY_PORT_INFO;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiMhlLocalDeviceStub getLocalDevice(int portId) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiMhlLocalDeviceStub getLocalDeviceById(int deviceId) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SparseArray<HdmiMhlLocalDeviceStub> getAllLocalDevices() {
        return mLocalDevices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiMhlLocalDeviceStub removeLocalDevice(int portId) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiMhlLocalDeviceStub addLocalDevice(HdmiMhlLocalDeviceStub device) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAllLocalDevices() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendVendorCommand(int portId, int offset, int length, byte[] data) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOption(int flag, int value) {
    }

    int getMhlVersion(int portId) {
        return 0;
    }

    int getPeerMhlVersion(int portId) {
        return 0;
    }

    int getSupportedFeatures(int portId) {
        return 0;
    }

    int getEcbusDeviceRoles(int portId) {
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
    }
}
