package com.android.server.hdmi;

import com.android.server.hdmi.HdmiControlService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class SystemAudioAutoInitiationAction extends HdmiCecFeatureAction {
    static final int RETRIES_ON_TIMEOUT = 1;
    private static final int STATE_WAITING_FOR_SYSTEM_AUDIO_MODE_STATUS = 1;
    private final int mAvrAddress;
    private int mRetriesOnTimeOut;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemAudioAutoInitiationAction(HdmiCecLocalDevice source, int avrAddress) {
        super(source);
        this.mRetriesOnTimeOut = 1;
        this.mAvrAddress = avrAddress;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        this.mState = 1;
        addTimer(this.mState, 2000);
        sendGiveSystemAudioModeStatus();
        return true;
    }

    private void sendGiveSystemAudioModeStatus() {
        sendCommand(HdmiCecMessageBuilder.buildGiveSystemAudioModeStatus(getSourceAddress(), this.mAvrAddress), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.SystemAudioAutoInitiationAction.1
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public void onSendCompleted(int error) {
                if (error != 0) {
                    SystemAudioAutoInitiationAction.this.handleSystemAudioModeStatusTimeout();
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState == 1 && this.mAvrAddress == cmd.getSource() && cmd.getOpcode() == 126) {
            handleSystemAudioModeStatusMessage(HdmiUtils.parseCommandParamSystemAudioStatus(cmd));
            return true;
        }
        return false;
    }

    private void handleSystemAudioModeStatusMessage(boolean currentSystemAudioMode) {
        if (!canChangeSystemAudio()) {
            HdmiLogger.debug("Cannot change system audio mode in auto initiation action.", new Object[0]);
            finish();
            return;
        }
        boolean targetSystemAudioMode = tv().isSystemAudioControlFeatureEnabled();
        if (currentSystemAudioMode != targetSystemAudioMode) {
            addAndStartAction(new SystemAudioActionFromTv(tv(), this.mAvrAddress, targetSystemAudioMode, null));
        } else {
            tv().setSystemAudioMode(targetSystemAudioMode);
        }
        finish();
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        if (this.mState != state) {
            return;
        }
        switch (this.mState) {
            case 1:
                int i = this.mRetriesOnTimeOut;
                if (i > 0) {
                    this.mRetriesOnTimeOut = i - 1;
                    addTimer(this.mState, 2000);
                    sendGiveSystemAudioModeStatus();
                    return;
                }
                handleSystemAudioModeStatusTimeout();
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSystemAudioModeStatusTimeout() {
        if (!canChangeSystemAudio()) {
            HdmiLogger.debug("Cannot change system audio mode in auto initiation action.", new Object[0]);
            finish();
            return;
        }
        addAndStartAction(new SystemAudioActionFromTv(tv(), this.mAvrAddress, tv().isSystemAudioControlFeatureEnabled(), null));
        finish();
    }

    private boolean canChangeSystemAudio() {
        return (tv().hasAction(SystemAudioActionFromTv.class) || tv().hasAction(SystemAudioActionFromAvr.class)) ? false : true;
    }
}
