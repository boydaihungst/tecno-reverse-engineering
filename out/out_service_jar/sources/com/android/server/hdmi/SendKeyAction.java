package com.android.server.hdmi;

import android.util.Slog;
import com.android.server.hdmi.HdmiControlService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class SendKeyAction extends HdmiCecFeatureAction {
    private static final int AWAIT_LONGPRESS_MS = 400;
    private static final int AWAIT_RELEASE_KEY_MS = 1000;
    private static final int STATE_CHECKING_LONGPRESS = 1;
    private static final int STATE_PROCESSING_KEYCODE = 2;
    private static final String TAG = "SendKeyAction";
    private int mLastKeycode;
    private long mLastSendKeyTime;
    private final int mTargetAddress;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SendKeyAction(HdmiCecLocalDevice source, int targetAddress, int keycode) {
        super(source);
        this.mTargetAddress = targetAddress;
        this.mLastKeycode = keycode;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        sendKeyDown(this.mLastKeycode);
        this.mLastSendKeyTime = getCurrentTime();
        if (!HdmiCecKeycode.isRepeatableKey(this.mLastKeycode)) {
            sendKeyUp();
            finish();
            return true;
        }
        this.mState = 1;
        addTimer(this.mState, 400);
        return true;
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void processKeyEvent(int keycode, boolean isPressed) {
        if (this.mState != 1 && this.mState != 2) {
            Slog.w(TAG, "Not in a valid state");
        } else if (isPressed) {
            if (keycode != this.mLastKeycode) {
                sendKeyDown(keycode);
                this.mLastSendKeyTime = getCurrentTime();
                if (!HdmiCecKeycode.isRepeatableKey(keycode)) {
                    sendKeyUp();
                    finish();
                    return;
                }
            } else if (getCurrentTime() - this.mLastSendKeyTime >= 300) {
                sendKeyDown(keycode);
                this.mLastSendKeyTime = getCurrentTime();
            }
            this.mActionTimer.clearTimerMessage();
            addTimer(this.mState, 1000);
            this.mLastKeycode = keycode;
        } else if (keycode == this.mLastKeycode) {
            sendKeyUp();
            finish();
        }
    }

    private void sendKeyDown(int keycode) {
        byte[] cecKeycodeAndParams = HdmiCecKeycode.androidKeyToCecKey(keycode);
        if (cecKeycodeAndParams == null) {
            return;
        }
        if (this.mTargetAddress == 5 && localDevice().getDeviceInfo().getLogicalAddress() != 0) {
            sendCommand(HdmiCecMessageBuilder.buildUserControlPressed(getSourceAddress(), this.mTargetAddress, cecKeycodeAndParams), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.SendKeyAction.1
                @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
                public void onSendCompleted(int error) {
                    if (error != 0) {
                        HdmiLogger.debug("AVR did not respond to <User Control Pressed>", new Object[0]);
                        SendKeyAction.this.localDevice().mService.setSystemAudioActivated(false);
                    }
                }
            });
        } else {
            sendCommand(HdmiCecMessageBuilder.buildUserControlPressed(getSourceAddress(), this.mTargetAddress, cecKeycodeAndParams));
        }
    }

    private void sendKeyUp() {
        if (HdmiCecKeycode.isVolumeKeycode(this.mLastKeycode) && localDevice().getService().isAbsoluteVolumeControlEnabled()) {
            sendCommand(HdmiCecMessageBuilder.buildUserControlReleased(getSourceAddress(), this.mTargetAddress), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.SendKeyAction$$ExternalSyntheticLambda0
                @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
                public final void onSendCompleted(int i) {
                    SendKeyAction.this.m3868lambda$sendKeyUp$0$comandroidserverhdmiSendKeyAction(i);
                }
            });
        } else {
            sendCommand(HdmiCecMessageBuilder.buildUserControlReleased(getSourceAddress(), this.mTargetAddress));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendKeyUp$0$com-android-server-hdmi-SendKeyAction  reason: not valid java name */
    public /* synthetic */ void m3868lambda$sendKeyUp$0$comandroidserverhdmiSendKeyAction(int __) {
        sendCommand(HdmiCecMessageBuilder.buildGiveAudioStatus(getSourceAddress(), localDevice().findAudioReceiverAddress()));
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        return false;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public void handleTimerEvent(int state) {
        switch (this.mState) {
            case 1:
                this.mActionTimer.clearTimerMessage();
                this.mState = 2;
                sendKeyDown(this.mLastKeycode);
                this.mLastSendKeyTime = getCurrentTime();
                addTimer(this.mState, 1000);
                return;
            case 2:
                sendKeyUp();
                finish();
                return;
            default:
                Slog.w(TAG, "Not in a valid state");
                return;
        }
    }
}
