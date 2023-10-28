package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.hdmi.HdmiCecLocalDevice;
/* loaded from: classes.dex */
final class ActiveSourceHandler {
    private static final String TAG = "ActiveSourceHandler";
    private final IHdmiControlCallback mCallback;
    private final HdmiControlService mService;
    private final HdmiCecLocalDeviceTv mSource;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ActiveSourceHandler create(HdmiCecLocalDeviceTv source, IHdmiControlCallback callback) {
        if (source == null) {
            Slog.e(TAG, "Wrong arguments");
            return null;
        }
        return new ActiveSourceHandler(source, callback);
    }

    private ActiveSourceHandler(HdmiCecLocalDeviceTv source, IHdmiControlCallback callback) {
        this.mSource = source;
        this.mService = source.getService();
        this.mCallback = callback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void process(HdmiCecLocalDevice.ActiveSource newActive, int deviceType) {
        HdmiCecLocalDeviceTv tv = this.mSource;
        HdmiDeviceInfo device = this.mService.getDeviceInfo(newActive.logicalAddress);
        if (device == null) {
            tv.startNewDeviceAction(newActive, deviceType);
        }
        if (!tv.isProhibitMode()) {
            HdmiCecLocalDevice.ActiveSource old = HdmiCecLocalDevice.ActiveSource.of(tv.getActiveSource());
            tv.updateActiveSource(newActive, TAG);
            boolean notifyInputChange = this.mCallback == null;
            if (!old.equals(newActive)) {
                tv.setPrevPortId(tv.getActivePortId());
            }
            tv.updateActiveInput(newActive.physicalAddress, notifyInputChange);
            invokeCallback(0);
            return;
        }
        HdmiCecLocalDevice.ActiveSource current = tv.getActiveSource();
        if (current.logicalAddress == getSourceAddress()) {
            HdmiCecMessage activeSourceCommand = HdmiCecMessageBuilder.buildActiveSource(current.logicalAddress, current.physicalAddress);
            this.mService.sendCecCommand(activeSourceCommand);
            tv.updateActiveSource(current, TAG);
            invokeCallback(0);
            return;
        }
        tv.startRoutingControl(newActive.physicalAddress, current.physicalAddress, this.mCallback);
    }

    private final int getSourceAddress() {
        return this.mSource.getDeviceInfo().getLogicalAddress();
    }

    private void invokeCallback(int result) {
        IHdmiControlCallback iHdmiControlCallback = this.mCallback;
        if (iHdmiControlCallback == null) {
            return;
        }
        try {
            iHdmiControlCallback.onComplete(result);
        } catch (RemoteException e) {
            Slog.e(TAG, "Callback failed:" + e);
        }
    }
}
