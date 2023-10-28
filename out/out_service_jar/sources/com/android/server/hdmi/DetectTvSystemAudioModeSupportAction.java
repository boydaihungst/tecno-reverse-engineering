package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import com.android.server.hdmi.HdmiCecLocalDeviceAudioSystem;
import com.android.server.hdmi.HdmiControlService;
/* loaded from: classes.dex */
public class DetectTvSystemAudioModeSupportAction extends HdmiCecFeatureAction {
    static final int MAX_RETRY_COUNT = 5;
    private static final int STATE_WAITING_FOR_FEATURE_ABORT = 1;
    private static final int STATE_WAITING_FOR_SET_SAM = 2;
    private HdmiCecLocalDeviceAudioSystem.TvSystemAudioModeSupportedCallback mCallback;
    private int mSendSetSystemAudioModeRetryCount;

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public /* bridge */ /* synthetic */ void addCallback(IHdmiControlCallback iHdmiControlCallback) {
        super.addCallback(iHdmiControlCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DetectTvSystemAudioModeSupportAction(HdmiCecLocalDevice source, HdmiCecLocalDeviceAudioSystem.TvSystemAudioModeSupportedCallback callback) {
        super(source);
        this.mSendSetSystemAudioModeRetryCount = 0;
        this.mCallback = callback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        this.mState = 1;
        addTimer(this.mState, 2000);
        sendSetSystemAudioMode();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (cmd.getOpcode() == 0 && this.mState == 1 && HdmiUtils.getAbortFeatureOpcode(cmd) == 114) {
            if (HdmiUtils.getAbortReason(cmd) == 1) {
                this.mActionTimer.clearTimerMessage();
                this.mState = 2;
                addTimer(this.mState, 300);
            } else {
                finishAction(false);
            }
            return true;
        }
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        if (this.mState != state) {
            return;
        }
        switch (this.mState) {
            case 1:
                finishAction(true);
                return;
            case 2:
                int i = this.mSendSetSystemAudioModeRetryCount + 1;
                this.mSendSetSystemAudioModeRetryCount = i;
                if (i < 5) {
                    this.mState = 1;
                    addTimer(this.mState, 2000);
                    sendSetSystemAudioMode();
                    return;
                }
                finishAction(false);
                return;
            default:
                return;
        }
    }

    protected void sendSetSystemAudioMode() {
        sendCommand(HdmiCecMessageBuilder.buildSetSystemAudioMode(getSourceAddress(), 0, true), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.DetectTvSystemAudioModeSupportAction$$ExternalSyntheticLambda0
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public final void onSendCompleted(int i) {
                DetectTvSystemAudioModeSupportAction.this.m3714x31b4d680(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendSetSystemAudioMode$0$com-android-server-hdmi-DetectTvSystemAudioModeSupportAction  reason: not valid java name */
    public /* synthetic */ void m3714x31b4d680(int result) {
        if (result != 0) {
            finishAction(false);
        }
    }

    private void finishAction(boolean supported) {
        this.mCallback.onResult(supported);
        audioSystem().setTvSystemAudioModeSupport(supported);
        finish();
    }
}
