package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DevicePowerStatusAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_FOR_REPORT_POWER_STATUS = 1;
    private static final String TAG = "DevicePowerStatusAction";
    private int mRetriesOnTimeout;
    private final int mTargetAddress;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DevicePowerStatusAction create(HdmiCecLocalDevice source, int targetAddress, IHdmiControlCallback callback) {
        if (source == null || callback == null) {
            Slog.e(TAG, "Wrong arguments");
            return null;
        }
        return new DevicePowerStatusAction(source, targetAddress, callback);
    }

    private DevicePowerStatusAction(HdmiCecLocalDevice localDevice, int targetAddress, IHdmiControlCallback callback) {
        super(localDevice, callback);
        this.mRetriesOnTimeout = 1;
        this.mTargetAddress = targetAddress;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        HdmiDeviceInfo deviceInfo;
        int powerStatus;
        HdmiControlService service = localDevice().mService;
        if (service.getCecVersion() >= 6 && (deviceInfo = service.getHdmiCecNetwork().getCecDeviceInfo(this.mTargetAddress)) != null && deviceInfo.getCecVersion() >= 6 && (powerStatus = deviceInfo.getDevicePowerStatus()) != -1) {
            finishWithCallback(powerStatus);
            return true;
        }
        queryDevicePowerStatus();
        this.mState = 1;
        addTimer(this.mState, 2000);
        return true;
    }

    private void queryDevicePowerStatus() {
        sendCommand(HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), this.mTargetAddress), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.DevicePowerStatusAction$$ExternalSyntheticLambda0
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public final void onSendCompleted(int i) {
                DevicePowerStatusAction.this.m3730xfcd8d420(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$queryDevicePowerStatus$0$com-android-server-hdmi-DevicePowerStatusAction  reason: not valid java name */
    public /* synthetic */ void m3730xfcd8d420(int error) {
        if (error == 1) {
            finishWithCallback(-1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState == 1 && this.mTargetAddress == cmd.getSource() && cmd.getOpcode() == 144) {
            int status = cmd.getParams()[0];
            finishWithCallback(status);
            return true;
        }
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        if (this.mState == state && state == 1) {
            int i = this.mRetriesOnTimeout;
            if (i > 0) {
                this.mRetriesOnTimeout = i - 1;
                start();
                return;
            }
            finishWithCallback(-1);
        }
    }
}
