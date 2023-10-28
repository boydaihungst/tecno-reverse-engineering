package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DeviceSelectActionFromPlayback extends HdmiCecFeatureAction {
    private static final int LOOP_COUNTER_MAX = 2;
    static final int STATE_WAIT_FOR_ACTIVE_SOURCE_MESSAGE_AFTER_ROUTING_CHANGE = 4;
    private static final int STATE_WAIT_FOR_ACTIVE_SOURCE_MESSAGE_AFTER_SET_STREAM_PATH = 5;
    static final int STATE_WAIT_FOR_DEVICE_POWER_ON = 3;
    private static final int STATE_WAIT_FOR_DEVICE_TO_TRANSIT_TO_STANDBY = 2;
    static final int STATE_WAIT_FOR_REPORT_POWER_STATUS = 1;
    private static final String TAG = "DeviceSelectActionFromPlayback";
    private static final int TIMEOUT_POWER_ON_MS = 5000;
    private static final int TIMEOUT_TRANSIT_TO_STANDBY_MS = 5000;
    private final HdmiCecMessage mGivePowerStatus;
    private final boolean mIsCec20;
    private int mPowerStatusCounter;
    private final HdmiDeviceInfo mTarget;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DeviceSelectActionFromPlayback(HdmiCecLocalDevicePlayback source, HdmiDeviceInfo target, IHdmiControlCallback callback) {
        this(source, target, callback, source.getDeviceInfo().getCecVersion() >= 6 && target.getCecVersion() >= 6);
    }

    DeviceSelectActionFromPlayback(HdmiCecLocalDevicePlayback source, HdmiDeviceInfo target, IHdmiControlCallback callback, boolean isCec20) {
        super(source, callback);
        this.mPowerStatusCounter = 0;
        this.mTarget = target;
        this.mGivePowerStatus = HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), getTargetAddress());
        this.mIsCec20 = isCec20;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getTargetAddress() {
        return this.mTarget.getLogicalAddress();
    }

    private int getTargetPath() {
        return this.mTarget.getPhysicalAddress();
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        sendRoutingChange();
        if (!this.mIsCec20) {
            queryDevicePowerStatus();
        } else {
            int targetPowerStatus = -1;
            HdmiDeviceInfo targetDevice = localDevice().mService.getHdmiCecNetwork().getCecDeviceInfo(getTargetAddress());
            if (targetDevice != null) {
                targetPowerStatus = targetDevice.getDevicePowerStatus();
            }
            if (targetPowerStatus == -1) {
                queryDevicePowerStatus();
            } else if (targetPowerStatus == 0) {
                this.mState = 4;
                addTimer(this.mState, 2000);
                return true;
            }
        }
        this.mState = 1;
        addTimer(this.mState, 2000);
        return true;
    }

    private void queryDevicePowerStatus() {
        sendCommand(this.mGivePowerStatus, new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.DeviceSelectActionFromPlayback.1
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public void onSendCompleted(int error) {
                if (error != 0) {
                    DeviceSelectActionFromPlayback.this.finishWithCallback(7);
                }
            }
        });
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (cmd.getSource() != getTargetAddress()) {
            return false;
        }
        int opcode = cmd.getOpcode();
        byte[] params = cmd.getParams();
        if (opcode == 130) {
            finishWithCallback(0);
            return true;
        } else if (this.mState == 1 && opcode == 144) {
            return handleReportPowerStatus(params[0]);
        } else {
            return false;
        }
    }

    private boolean handleReportPowerStatus(int powerStatus) {
        switch (powerStatus) {
            case 0:
                selectDevice();
                return true;
            case 1:
                if (this.mPowerStatusCounter == 0) {
                    sendRoutingChange();
                    this.mState = 3;
                    addTimer(this.mState, 5000);
                } else {
                    selectDevice();
                }
                return true;
            case 2:
                if (this.mPowerStatusCounter < 2) {
                    this.mState = 3;
                    addTimer(this.mState, 5000);
                } else {
                    selectDevice();
                }
                return true;
            case 3:
                if (this.mPowerStatusCounter < 4) {
                    this.mState = 2;
                    addTimer(this.mState, 5000);
                } else {
                    selectDevice();
                }
                return true;
            default:
                return false;
        }
    }

    private void selectDevice() {
        sendRoutingChange();
        this.mState = 4;
        addTimer(this.mState, 2000);
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int timeoutState) {
        if (this.mState != timeoutState) {
            Slog.w(TAG, "Timer in a wrong state. Ignored.");
            return;
        }
        switch (this.mState) {
            case 1:
                selectDevice();
                addTimer(this.mState, 2000);
                return;
            case 2:
            default:
                return;
            case 3:
                this.mPowerStatusCounter++;
                queryDevicePowerStatus();
                this.mState = 1;
                addTimer(this.mState, 2000);
                return;
            case 4:
                sendSetStreamPath();
                this.mState = 5;
                addTimer(this.mState, 2000);
                return;
            case 5:
                finishWithCallback(1);
                return;
        }
    }

    private void sendRoutingChange() {
        sendCommand(HdmiCecMessageBuilder.buildRoutingChange(getSourceAddress(), playback().getActiveSource().physicalAddress, getTargetPath()));
    }

    private void sendSetStreamPath() {
        sendCommand(HdmiCecMessageBuilder.buildSetStreamPath(getSourceAddress(), getTargetPath()));
    }
}
