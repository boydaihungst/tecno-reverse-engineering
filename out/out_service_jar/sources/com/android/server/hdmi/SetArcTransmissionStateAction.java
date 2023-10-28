package com.android.server.hdmi;

import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
import com.android.server.hdmi.RequestSadAction;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class SetArcTransmissionStateAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_TIMEOUT = 1;
    private static final String TAG = "SetArcTransmissionStateAction";
    private final int mAvrAddress;
    private final boolean mEnabled;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SetArcTransmissionStateAction(HdmiCecLocalDevice source, int avrAddress, boolean enabled) {
        super(source);
        HdmiUtils.verifyAddressType(getSourceAddress(), 0);
        HdmiUtils.verifyAddressType(avrAddress, 5);
        this.mAvrAddress = avrAddress;
        this.mEnabled = enabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        if (this.mEnabled) {
            RequestSadAction action = new RequestSadAction(localDevice(), 5, new RequestSadAction.RequestSadCallback() { // from class: com.android.server.hdmi.SetArcTransmissionStateAction.1
                @Override // com.android.server.hdmi.RequestSadAction.RequestSadCallback
                public void onRequestSadDone(List<byte[]> supportedSads) {
                    Slog.i(SetArcTransmissionStateAction.TAG, "Enabling ARC");
                    SetArcTransmissionStateAction.this.tv().enableArc(supportedSads);
                    SetArcTransmissionStateAction.this.mState = 1;
                    SetArcTransmissionStateAction setArcTransmissionStateAction = SetArcTransmissionStateAction.this;
                    setArcTransmissionStateAction.addTimer(setArcTransmissionStateAction.mState, 2000);
                    SetArcTransmissionStateAction.this.sendReportArcInitiated();
                }
            });
            addAndStartAction(action);
            return true;
        }
        disableArc();
        finish();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendReportArcInitiated() {
        HdmiCecMessage command = HdmiCecMessageBuilder.buildReportArcInitiated(getSourceAddress(), this.mAvrAddress);
        sendCommand(command, new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.SetArcTransmissionStateAction.2
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public void onSendCompleted(int error) {
                switch (error) {
                    case 0:
                    case 2:
                    case 3:
                    default:
                        return;
                    case 1:
                        SetArcTransmissionStateAction.this.disableArc();
                        HdmiLogger.debug("Failed to send <Report Arc Initiated>.", new Object[0]);
                        SetArcTransmissionStateAction.this.finish();
                        return;
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void disableArc() {
        Slog.i(TAG, "Disabling ARC");
        tv().disableArc();
        sendCommand(HdmiCecMessageBuilder.buildReportArcTerminated(getSourceAddress(), this.mAvrAddress));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1) {
            return false;
        }
        int opcode = cmd.getOpcode();
        if (opcode == 0) {
            int originalOpcode = cmd.getParams()[0] & 255;
            if (originalOpcode == 193) {
                HdmiLogger.debug("Feature aborted for <Report Arc Initiated>", new Object[0]);
                disableArc();
                finish();
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        if (this.mState != state || this.mState != 1) {
            return;
        }
        finish();
    }
}
