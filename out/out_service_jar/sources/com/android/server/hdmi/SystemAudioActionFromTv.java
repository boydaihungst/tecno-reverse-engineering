package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class SystemAudioActionFromTv extends SystemAudioAction {
    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemAudioActionFromTv(HdmiCecLocalDevice sourceAddress, int avrAddress, boolean targetStatus, IHdmiControlCallback callback) {
        super(sourceAddress, avrAddress, targetStatus, callback);
        HdmiUtils.verifyAddressType(getSourceAddress(), 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        removeSystemAudioActionInProgress();
        sendSystemAudioModeRequest();
        return true;
    }
}
