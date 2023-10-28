package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
/* loaded from: classes.dex */
public class ActiveSourceAction extends HdmiCecFeatureAction {
    private static final int STATE_FINISHED = 2;
    private static final int STATE_STARTED = 1;
    private final int mDestination;

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public /* bridge */ /* synthetic */ void addCallback(IHdmiControlCallback iHdmiControlCallback) {
        super.addCallback(iHdmiControlCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActiveSourceAction(HdmiCecLocalDevice source, int destination) {
        super(source);
        this.mDestination = destination;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        this.mState = 1;
        int logicalAddress = getSourceAddress();
        int physicalAddress = getSourcePath();
        sendCommand(HdmiCecMessageBuilder.buildActiveSource(logicalAddress, physicalAddress));
        if (source().getType() == 4) {
            sendCommand(HdmiCecMessageBuilder.buildReportMenuStatus(logicalAddress, this.mDestination, 0));
        }
        source().setActiveSource(logicalAddress, physicalAddress, "ActiveSourceAction");
        this.mState = 2;
        finish();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
    }
}
