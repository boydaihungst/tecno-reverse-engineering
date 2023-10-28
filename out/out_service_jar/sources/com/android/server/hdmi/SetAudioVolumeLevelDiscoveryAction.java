package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.hdmi.HdmiControlService;
/* loaded from: classes.dex */
public class SetAudioVolumeLevelDiscoveryAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_FOR_FEATURE_ABORT = 1;
    private static final String TAG = "SetAudioVolumeLevelDiscoveryAction";
    private final int mTargetAddress;

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public /* bridge */ /* synthetic */ void addCallback(IHdmiControlCallback iHdmiControlCallback) {
        super.addCallback(iHdmiControlCallback);
    }

    public SetAudioVolumeLevelDiscoveryAction(HdmiCecLocalDevice source, int targetAddress, IHdmiControlCallback callback) {
        super(source, callback);
        this.mTargetAddress = targetAddress;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        sendCommand(SetAudioVolumeLevelMessage.build(getSourceAddress(), this.mTargetAddress, (int) FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__SET_AUTO_TIME), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.SetAudioVolumeLevelDiscoveryAction$$ExternalSyntheticLambda0
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public final void onSendCompleted(int i) {
                SetAudioVolumeLevelDiscoveryAction.this.m3871x4cdb7098(i);
            }
        });
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$start$0$com-android-server-hdmi-SetAudioVolumeLevelDiscoveryAction  reason: not valid java name */
    public /* synthetic */ void m3871x4cdb7098(int result) {
        if (result == 0) {
            this.mState = 1;
            addTimer(this.mState, 2000);
            return;
        }
        finishWithCallback(7);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1) {
            return false;
        }
        switch (cmd.getOpcode()) {
            case 0:
                return handleFeatureAbort(cmd);
            default:
                return false;
        }
    }

    private boolean handleFeatureAbort(HdmiCecMessage cmd) {
        if (cmd.getParams().length < 2) {
            return false;
        }
        int originalOpcode = cmd.getParams()[0] & 255;
        if (originalOpcode == 115 && cmd.getSource() == this.mTargetAddress) {
            finishWithCallback(0);
            return true;
        }
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        if (updateAvcSupport(1)) {
            finishWithCallback(0);
        } else {
            finishWithCallback(5);
        }
    }

    private boolean updateAvcSupport(int setAudioVolumeLevelSupport) {
        HdmiCecNetwork network = localDevice().mService.getHdmiCecNetwork();
        HdmiDeviceInfo currentDeviceInfo = network.getCecDeviceInfo(this.mTargetAddress);
        if (currentDeviceInfo == null) {
            return false;
        }
        network.updateCecDevice(currentDeviceInfo.toBuilder().setDeviceFeatures(currentDeviceInfo.getDeviceFeatures().toBuilder().setSetAudioVolumeLevelSupport(setAudioVolumeLevelSupport).build()).build());
        return true;
    }

    public int getTargetAddress() {
        return this.mTargetAddress;
    }
}
