package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import com.android.server.hdmi.HdmiControlService;
/* loaded from: classes.dex */
public class ArcTerminationActionFromAvr extends HdmiCecFeatureAction {
    private static final int STATE_ARC_TERMINATED = 2;
    private static final int STATE_WAITING_FOR_INITIATE_ARC_RESPONSE = 1;
    private static final int TIMEOUT_MS = 1000;

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public /* bridge */ /* synthetic */ void addCallback(IHdmiControlCallback iHdmiControlCallback) {
        super.addCallback(iHdmiControlCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArcTerminationActionFromAvr(HdmiCecLocalDevice source) {
        super(source);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        this.mState = 1;
        addTimer(this.mState, 1000);
        sendTerminateArc();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1) {
            return false;
        }
        switch (cmd.getOpcode()) {
            case 194:
                this.mState = 2;
                audioSystem().processArcTermination();
                finish();
                return true;
            default:
                return false;
        }
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        if (this.mState != state) {
            return;
        }
        switch (this.mState) {
            case 1:
                handleTerminateArcTimeout();
                return;
            default:
                return;
        }
    }

    protected void sendTerminateArc() {
        sendCommand(HdmiCecMessageBuilder.buildTerminateArc(getSourceAddress(), 0), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.ArcTerminationActionFromAvr$$ExternalSyntheticLambda0
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public final void onSendCompleted(int i) {
                ArcTerminationActionFromAvr.this.m3712xebc778d4(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendTerminateArc$0$com-android-server-hdmi-ArcTerminationActionFromAvr  reason: not valid java name */
    public /* synthetic */ void m3712xebc778d4(int result) {
        if (result != 0) {
            if (result == 1) {
                audioSystem().setArcStatus(false);
            }
            HdmiLogger.debug("Terminate ARC was not successfully sent.", new Object[0]);
            finish();
        }
    }

    private void handleTerminateArcTimeout() {
        HdmiLogger.debug("handleTerminateArcTimeout", new Object[0]);
        finish();
    }
}
