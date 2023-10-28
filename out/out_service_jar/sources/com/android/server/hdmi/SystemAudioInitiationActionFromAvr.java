package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import com.android.server.hdmi.HdmiCecLocalDeviceAudioSystem;
import com.android.server.hdmi.HdmiControlService;
/* loaded from: classes.dex */
public class SystemAudioInitiationActionFromAvr extends HdmiCecFeatureAction {
    static final int MAX_RETRY_COUNT = 5;
    private static final int STATE_WAITING_FOR_ACTIVE_SOURCE = 1;
    private static final int STATE_WAITING_FOR_TV_SUPPORT = 2;
    private int mSendRequestActiveSourceRetryCount;
    private int mSendSetSystemAudioModeRetryCount;

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public /* bridge */ /* synthetic */ void addCallback(IHdmiControlCallback iHdmiControlCallback) {
        super.addCallback(iHdmiControlCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemAudioInitiationActionFromAvr(HdmiCecLocalDevice source) {
        super(source);
        this.mSendRequestActiveSourceRetryCount = 0;
        this.mSendSetSystemAudioModeRetryCount = 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        if (audioSystem().getActiveSource().physicalAddress == 65535) {
            this.mState = 1;
            addTimer(this.mState, 2000);
            sendRequestActiveSource();
        } else {
            this.mState = 2;
            queryTvSystemAudioModeSupport();
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        switch (cmd.getOpcode()) {
            case 130:
                if (this.mState != 1) {
                    return false;
                }
                this.mActionTimer.clearTimerMessage();
                audioSystem().handleActiveSource(cmd);
                this.mState = 2;
                queryTvSystemAudioModeSupport();
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
                handleActiveSourceTimeout();
                return;
            default:
                return;
        }
    }

    protected void sendRequestActiveSource() {
        sendCommand(HdmiCecMessageBuilder.buildRequestActiveSource(getSourceAddress()), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.SystemAudioInitiationActionFromAvr$$ExternalSyntheticLambda2
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public final void onSendCompleted(int i) {
                SystemAudioInitiationActionFromAvr.this.m3875x9c04ea5c(i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendRequestActiveSource$0$com-android-server-hdmi-SystemAudioInitiationActionFromAvr  reason: not valid java name */
    public /* synthetic */ void m3875x9c04ea5c(int result) {
        if (result != 0) {
            int i = this.mSendRequestActiveSourceRetryCount;
            if (i < 5) {
                this.mSendRequestActiveSourceRetryCount = i + 1;
                sendRequestActiveSource();
                return;
            }
            audioSystem().checkSupportAndSetSystemAudioMode(false);
            finish();
        }
    }

    protected void sendSetSystemAudioMode(final boolean on, final int dest) {
        sendCommand(HdmiCecMessageBuilder.buildSetSystemAudioMode(getSourceAddress(), dest, on), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.SystemAudioInitiationActionFromAvr$$ExternalSyntheticLambda0
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public final void onSendCompleted(int i) {
                SystemAudioInitiationActionFromAvr.this.m3876x331959d9(on, dest, i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendSetSystemAudioMode$1$com-android-server-hdmi-SystemAudioInitiationActionFromAvr  reason: not valid java name */
    public /* synthetic */ void m3876x331959d9(boolean on, int dest, int result) {
        if (result != 0) {
            int i = this.mSendSetSystemAudioModeRetryCount;
            if (i < 5) {
                this.mSendSetSystemAudioModeRetryCount = i + 1;
                sendSetSystemAudioMode(on, dest);
                return;
            }
            audioSystem().checkSupportAndSetSystemAudioMode(false);
            finish();
        }
    }

    private void handleActiveSourceTimeout() {
        HdmiLogger.debug("Cannot get active source.", new Object[0]);
        if (!audioSystem().mService.isPlaybackDevice()) {
            audioSystem().checkSupportAndSetSystemAudioMode(false);
        } else {
            audioSystem().mService.setAndBroadcastActiveSourceFromOneDeviceType(15, getSourcePath(), "SystemAudioInitiationActionFromAvr#handleActiveSourceTimeout()");
            this.mState = 2;
            queryTvSystemAudioModeSupport();
        }
        finish();
    }

    private void queryTvSystemAudioModeSupport() {
        audioSystem().queryTvSystemAudioModeSupport(new HdmiCecLocalDeviceAudioSystem.TvSystemAudioModeSupportedCallback() { // from class: com.android.server.hdmi.SystemAudioInitiationActionFromAvr$$ExternalSyntheticLambda1
            @Override // com.android.server.hdmi.HdmiCecLocalDeviceAudioSystem.TvSystemAudioModeSupportedCallback
            public final void onResult(boolean z) {
                SystemAudioInitiationActionFromAvr.this.m3874xd9f5e1b3(z);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$queryTvSystemAudioModeSupport$2$com-android-server-hdmi-SystemAudioInitiationActionFromAvr  reason: not valid java name */
    public /* synthetic */ void m3874xd9f5e1b3(boolean supported) {
        if (supported) {
            if (audioSystem().checkSupportAndSetSystemAudioMode(true)) {
                sendSetSystemAudioMode(true, 15);
            }
            finish();
            return;
        }
        audioSystem().checkSupportAndSetSystemAudioMode(false);
        finish();
    }
}
