package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class SystemAudioActionFromAvr extends SystemAudioAction {
    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemAudioActionFromAvr(HdmiCecLocalDevice source, int avrAddress, boolean targetStatus, IHdmiControlCallback callback) {
        super(source, avrAddress, targetStatus, callback);
        HdmiUtils.verifyAddressType(getSourceAddress(), 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        removeSystemAudioActionInProgress();
        handleSystemAudioActionFromAvr();
        return true;
    }

    private void handleSystemAudioActionFromAvr() {
        if (this.mTargetAudioStatus == tv().isSystemAudioActivated()) {
            finishWithCallback(0);
        } else if (tv().isProhibitMode()) {
            sendCommand(HdmiCecMessageBuilder.buildFeatureAbortCommand(getSourceAddress(), this.mAvrLogicalAddress, 114, 4));
            this.mTargetAudioStatus = false;
            sendSystemAudioModeRequest();
        } else {
            removeAction(SystemAudioAutoInitiationAction.class);
            if (this.mTargetAudioStatus) {
                setSystemAudioMode(true);
                finish();
                return;
            }
            setSystemAudioMode(false);
            finishWithCallback(0);
        }
    }
}
