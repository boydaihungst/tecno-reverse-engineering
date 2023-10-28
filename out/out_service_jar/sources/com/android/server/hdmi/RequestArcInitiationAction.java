package com.android.server.hdmi;

import com.android.server.hdmi.HdmiControlService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class RequestArcInitiationAction extends RequestArcAction {
    private static final String TAG = "RequestArcInitiationAction";

    /* JADX INFO: Access modifiers changed from: package-private */
    public RequestArcInitiationAction(HdmiCecLocalDevice source, int avrAddress) {
        super(source, avrAddress);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        this.mState = 1;
        addTimer(this.mState, 2000);
        HdmiCecMessage command = HdmiCecMessageBuilder.buildRequestArcInitiation(getSourceAddress(), this.mAvrAddress);
        sendCommand(command, new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.RequestArcInitiationAction.1
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public void onSendCompleted(int error) {
                if (error != 0) {
                    RequestArcInitiationAction.this.tv().disableArc();
                    RequestArcInitiationAction.this.finish();
                }
            }
        });
        return true;
    }
}
