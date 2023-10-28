package com.android.server.hdmi;
/* loaded from: classes.dex */
final class AbsoluteVolumeAudioStatusAction extends HdmiCecFeatureAction {
    private static final int STATE_MONITOR_AUDIO_STATUS = 2;
    private static final int STATE_WAIT_FOR_INITIAL_AUDIO_STATUS = 1;
    private static final String TAG = "AbsoluteVolumeAudioStatusAction";
    private int mInitialAudioStatusRetriesLeft;
    private AudioStatus mLastAudioStatus;
    private final int mTargetAddress;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AbsoluteVolumeAudioStatusAction(HdmiCecLocalDevice source, int targetAddress) {
        super(source);
        this.mInitialAudioStatusRetriesLeft = 2;
        this.mTargetAddress = targetAddress;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    boolean start() {
        this.mState = 1;
        sendGiveAudioStatus();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateVolume(int volumeIndex) {
        this.mLastAudioStatus = new AudioStatus(volumeIndex, this.mLastAudioStatus.getMute());
    }

    private void sendGiveAudioStatus() {
        addTimer(this.mState, 2000);
        sendCommand(HdmiCecMessageBuilder.buildGiveAudioStatus(getSourceAddress(), this.mTargetAddress));
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    boolean processCommand(HdmiCecMessage cmd) {
        switch (cmd.getOpcode()) {
            case 122:
                return handleReportAudioStatus(cmd);
            default:
                return false;
        }
    }

    private boolean handleReportAudioStatus(HdmiCecMessage cmd) {
        if (this.mTargetAddress != cmd.getSource() || cmd.getParams().length == 0) {
            return false;
        }
        boolean mute = HdmiUtils.isAudioStatusMute(cmd);
        int volume = HdmiUtils.getAudioStatusVolume(cmd);
        AudioStatus audioStatus = new AudioStatus(volume, mute);
        if (this.mState == 1) {
            localDevice().getService().enableAbsoluteVolumeControl(audioStatus);
            this.mState = 2;
        } else if (this.mState == 2) {
            if (audioStatus.getVolume() != this.mLastAudioStatus.getVolume()) {
                localDevice().getService().notifyAvcVolumeChange(audioStatus.getVolume());
            }
            if (audioStatus.getMute() != this.mLastAudioStatus.getMute()) {
                localDevice().getService().notifyAvcMuteChange(audioStatus.getMute());
            }
        }
        this.mLastAudioStatus = audioStatus;
        return true;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        int i;
        if (this.mState == state && (i = this.mInitialAudioStatusRetriesLeft) > 0) {
            this.mInitialAudioStatusRetriesLeft = i - 1;
            sendGiveAudioStatus();
        }
    }
}
