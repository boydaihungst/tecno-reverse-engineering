package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.util.Slog;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class OneTouchPlayAction extends HdmiCecFeatureAction {
    private static final int LOOP_COUNTER_MAX = 10;
    static final int STATE_WAITING_FOR_REPORT_POWER_STATUS = 1;
    private static final String TAG = "OneTouchPlayAction";
    private final boolean mIsCec20;
    private int mPowerStatusCounter;
    private HdmiCecLocalDeviceSource mSource;
    private final int mTargetAddress;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static OneTouchPlayAction create(HdmiCecLocalDeviceSource source, int targetAddress, IHdmiControlCallback callback) {
        if (source == null || callback == null) {
            Slog.e(TAG, "Wrong arguments");
            return null;
        }
        return new OneTouchPlayAction(source, targetAddress, callback);
    }

    private OneTouchPlayAction(HdmiCecLocalDevice localDevice, int targetAddress, IHdmiControlCallback callback) {
        this(localDevice, targetAddress, callback, localDevice.getDeviceInfo().getCecVersion() >= 6 && getTargetCecVersion(localDevice, targetAddress) >= 6);
    }

    OneTouchPlayAction(HdmiCecLocalDevice localDevice, int targetAddress, IHdmiControlCallback callback, boolean isCec20) {
        super(localDevice, callback);
        this.mPowerStatusCounter = 0;
        this.mTargetAddress = targetAddress;
        this.mIsCec20 = isCec20;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        this.mSource = source();
        sendCommand(HdmiCecMessageBuilder.buildTextViewOn(getSourceAddress(), this.mTargetAddress));
        boolean is20TargetOnBefore = this.mIsCec20 && getTargetDevicePowerStatus(this.mSource, this.mTargetAddress, -1) == 0;
        broadcastActiveSource();
        if (shouldTurnOnConnectedAudioSystem()) {
            sendCommand(HdmiCecMessageBuilder.buildSystemAudioModeRequest(getSourceAddress(), 5, getSourcePath(), true));
        }
        if (!this.mIsCec20) {
            queryDevicePowerStatus();
        } else {
            int targetPowerStatus = getTargetDevicePowerStatus(this.mSource, this.mTargetAddress, -1);
            if (targetPowerStatus == -1) {
                queryDevicePowerStatus();
            } else if (targetPowerStatus == 0) {
                if (!is20TargetOnBefore) {
                    broadcastActiveSource();
                }
                finishWithCallback(0);
                return true;
            }
        }
        this.mState = 1;
        addTimer(this.mState, 2000);
        return true;
    }

    private void broadcastActiveSource() {
        this.mSource.mService.setAndBroadcastActiveSourceFromOneDeviceType(this.mTargetAddress, getSourcePath(), "OneTouchPlayAction#broadcastActiveSource()");
        if (this.mSource.mService.audioSystem() != null) {
            this.mSource = this.mSource.mService.audioSystem();
        }
        this.mSource.setRoutingPort(0);
        this.mSource.setLocalActivePort(0);
    }

    private void queryDevicePowerStatus() {
        sendCommand(HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), this.mTargetAddress));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState == 1 && this.mTargetAddress == cmd.getSource() && cmd.getOpcode() == 144) {
            int status = cmd.getParams()[0];
            if (status == 0) {
                broadcastActiveSource();
                finishWithCallback(0);
            }
            return true;
        }
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        if (this.mState == state && state == 1) {
            int i = this.mPowerStatusCounter;
            this.mPowerStatusCounter = i + 1;
            if (i < 10) {
                queryDevicePowerStatus();
                addTimer(this.mState, 2000);
                return;
            }
            finishWithCallback(1);
        }
    }

    private boolean shouldTurnOnConnectedAudioSystem() {
        HdmiControlService service = this.mSource.mService;
        if (service.isAudioSystemDevice()) {
            return false;
        }
        String powerControlMode = service.getHdmiCecConfig().getStringValue("power_control_mode");
        return powerControlMode.equals("to_tv_and_audio_system") || powerControlMode.equals("broadcast");
    }

    private static int getTargetCecVersion(HdmiCecLocalDevice localDevice, int targetLogicalAddress) {
        HdmiDeviceInfo targetDevice = localDevice.mService.getHdmiCecNetwork().getCecDeviceInfo(targetLogicalAddress);
        if (targetDevice != null) {
            return targetDevice.getCecVersion();
        }
        return 5;
    }

    private static int getTargetDevicePowerStatus(HdmiCecLocalDevice localDevice, int targetLogicalAddress, int defaultPowerStatus) {
        HdmiDeviceInfo targetDevice = localDevice.mService.getHdmiCecNetwork().getCecDeviceInfo(targetLogicalAddress);
        if (targetDevice != null) {
            return targetDevice.getDevicePowerStatus();
        }
        return defaultPowerStatus;
    }
}
