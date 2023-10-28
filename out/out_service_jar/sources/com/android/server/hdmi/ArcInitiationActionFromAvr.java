package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import com.android.server.hdmi.HdmiControlService;
/* loaded from: classes.dex */
public class ArcInitiationActionFromAvr extends HdmiCecFeatureAction {
    private static final int STATE_ARC_INITIATED = 2;
    private static final int STATE_WAITING_FOR_INITIATE_ARC_RESPONSE = 1;
    private static final int TIMEOUT_MS = 1000;

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public /* bridge */ /* synthetic */ void addCallback(IHdmiControlCallback iHdmiControlCallback) {
        super.addCallback(iHdmiControlCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArcInitiationActionFromAvr(HdmiCecLocalDevice source) {
        super(source);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        audioSystem().setArcStatus(true);
        this.mState = 1;
        addTimer(this.mState, 1000);
        sendInitiateArc();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1) {
            return false;
        }
        switch (cmd.getOpcode()) {
            case 0:
                if ((cmd.getParams()[0] & 255) == 192) {
                    audioSystem().setArcStatus(false);
                    finish();
                    return true;
                }
                return false;
            case 193:
                this.mState = 2;
                finish();
                return true;
            case 194:
                audioSystem().setArcStatus(false);
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
                handleInitiateArcTimeout();
                return;
            default:
                return;
        }
    }

    protected void sendInitiateArc() {
        sendCommand(HdmiCecMessageBuilder.buildInitiateArc(getSourceAddress(), 0), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.ArcInitiationActionFromAvr$$ExternalSyntheticLambda0
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public final void onSendCompleted(int i) {
                ArcInitiationActionFromAvr.this.m3711xae811328(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendInitiateArc$0$com-android-server-hdmi-ArcInitiationActionFromAvr  reason: not valid java name */
    public /* synthetic */ void m3711xae811328(int result) {
        if (result != 0) {
            audioSystem().setArcStatus(false);
            finish();
        }
    }

    private void handleInitiateArcTimeout() {
        HdmiLogger.debug("handleInitiateArcTimeout", new Object[0]);
        finish();
    }
}
