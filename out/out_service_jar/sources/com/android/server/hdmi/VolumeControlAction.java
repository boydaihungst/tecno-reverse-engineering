package com.android.server.hdmi;

import android.media.AudioManager;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VolumeControlAction extends HdmiCecFeatureAction {
    private static final int MAX_VOLUME = 100;
    private static final int STATE_WAIT_FOR_NEXT_VOLUME_PRESS = 1;
    private static final String TAG = "VolumeControlAction";
    private static final int UNKNOWN_AVR_VOLUME = -1;
    private final int mAvrAddress;
    private boolean mIsVolumeUp;
    private boolean mLastAvrMute;
    private int mLastAvrVolume;
    private long mLastKeyUpdateTime;
    private boolean mSentKeyPressed;

    public static int scaleToCecVolume(int volume, int scale) {
        return (volume * 100) / scale;
    }

    public static int scaleToCustomVolume(int cecVolume, int scale) {
        return (cecVolume * scale) / 100;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VolumeControlAction(HdmiCecLocalDevice source, int avrAddress, boolean isVolumeUp) {
        super(source);
        this.mAvrAddress = avrAddress;
        this.mIsVolumeUp = isVolumeUp;
        this.mLastAvrVolume = -1;
        this.mLastAvrMute = false;
        this.mSentKeyPressed = false;
        updateLastKeyUpdateTime();
    }

    private void updateLastKeyUpdateTime() {
        this.mLastKeyUpdateTime = System.currentTimeMillis();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        this.mState = 1;
        sendVolumeKeyPressed();
        resetTimer();
        return true;
    }

    private void sendVolumeKeyPressed() {
        sendCommand(HdmiCecMessageBuilder.buildUserControlPressed(getSourceAddress(), this.mAvrAddress, this.mIsVolumeUp ? 65 : 66));
        this.mSentKeyPressed = true;
    }

    private void resetTimer() {
        this.mActionTimer.clearTimerMessage();
        addTimer(1, 300);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleVolumeChange(boolean isVolumeUp) {
        boolean z = this.mIsVolumeUp;
        if (z != isVolumeUp) {
            HdmiLogger.debug("Volume Key Status Changed[old:%b new:%b]", Boolean.valueOf(z), Boolean.valueOf(isVolumeUp));
            sendVolumeKeyReleased();
            this.mIsVolumeUp = isVolumeUp;
            sendVolumeKeyPressed();
            resetTimer();
        }
        updateLastKeyUpdateTime();
    }

    private void sendVolumeKeyReleased() {
        sendCommand(HdmiCecMessageBuilder.buildUserControlReleased(getSourceAddress(), this.mAvrAddress));
        this.mSentKeyPressed = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState == 1 && cmd.getSource() == this.mAvrAddress) {
            switch (cmd.getOpcode()) {
                case 0:
                    return handleFeatureAbort(cmd);
                case 122:
                    return handleReportAudioStatus(cmd);
                default:
                    return false;
            }
        }
        return false;
    }

    private boolean handleReportAudioStatus(HdmiCecMessage cmd) {
        boolean mute = HdmiUtils.isAudioStatusMute(cmd);
        int volume = HdmiUtils.getAudioStatusVolume(cmd);
        this.mLastAvrVolume = volume;
        this.mLastAvrMute = mute;
        if (shouldUpdateAudioVolume(mute)) {
            HdmiLogger.debug("Force volume change[mute:%b, volume=%d]", Boolean.valueOf(mute), Integer.valueOf(volume));
            tv().setAudioStatus(mute, volume);
            this.mLastAvrVolume = -1;
            this.mLastAvrMute = false;
        }
        return true;
    }

    private boolean shouldUpdateAudioVolume(boolean mute) {
        if (mute) {
            return true;
        }
        AudioManager audioManager = tv().getService().getAudioManager();
        int currentVolume = audioManager.getStreamVolume(3);
        if (!this.mIsVolumeUp) {
            return currentVolume == 0;
        }
        int maxVolume = audioManager.getStreamMaxVolume(3);
        return currentVolume == maxVolume;
    }

    private boolean handleFeatureAbort(HdmiCecMessage cmd) {
        int originalOpcode = cmd.getParams()[0] & 255;
        if (originalOpcode == 68) {
            finish();
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public void clear() {
        super.clear();
        if (this.mSentKeyPressed) {
            sendVolumeKeyReleased();
        }
        if (this.mLastAvrVolume != -1) {
            tv().setAudioStatus(this.mLastAvrMute, this.mLastAvrVolume);
            this.mLastAvrVolume = -1;
            this.mLastAvrMute = false;
        }
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        if (state != 1) {
            return;
        }
        if (System.currentTimeMillis() - this.mLastKeyUpdateTime >= 300) {
            finish();
            return;
        }
        sendVolumeKeyPressed();
        resetTimer();
    }
}
