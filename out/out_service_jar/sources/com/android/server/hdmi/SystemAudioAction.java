package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import com.android.server.hdmi.HdmiControlService;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class SystemAudioAction extends HdmiCecFeatureAction {
    private static final int MAX_SEND_RETRY_COUNT = 2;
    private static final int OFF_TIMEOUT_MS = 2000;
    private static final int ON_TIMEOUT_MS = 5000;
    private static final int STATE_CHECK_ROUTING_IN_PRGRESS = 1;
    private static final int STATE_WAIT_FOR_SET_SYSTEM_AUDIO_MODE = 2;
    private static final String TAG = "SystemAudioAction";
    protected final int mAvrLogicalAddress;
    private int mSendRetryCount;
    protected boolean mTargetAudioStatus;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemAudioAction(HdmiCecLocalDevice source, int avrAddress, boolean targetStatus, IHdmiControlCallback callback) {
        super(source, callback);
        this.mSendRetryCount = 0;
        HdmiUtils.verifyAddressType(avrAddress, 5);
        this.mAvrLogicalAddress = avrAddress;
        this.mTargetAudioStatus = targetStatus;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void sendSystemAudioModeRequest() {
        List<RoutingControlAction> routingActions = getActions(RoutingControlAction.class);
        if (!routingActions.isEmpty()) {
            this.mState = 1;
            RoutingControlAction routingAction = routingActions.get(0);
            routingAction.addOnFinishedCallback(this, new Runnable() { // from class: com.android.server.hdmi.SystemAudioAction.1
                @Override // java.lang.Runnable
                public void run() {
                    SystemAudioAction.this.sendSystemAudioModeRequestInternal();
                }
            });
            return;
        }
        sendSystemAudioModeRequestInternal();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendSystemAudioModeRequestInternal() {
        HdmiCecMessage command = HdmiCecMessageBuilder.buildSystemAudioModeRequest(getSourceAddress(), this.mAvrLogicalAddress, getSystemAudioModeRequestParam(), this.mTargetAudioStatus);
        sendCommand(command, new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.SystemAudioAction.2
            @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
            public void onSendCompleted(int error) {
                if (error != 0) {
                    HdmiLogger.debug("Failed to send <System Audio Mode Request>:" + error, new Object[0]);
                    SystemAudioAction.this.setSystemAudioMode(false);
                    SystemAudioAction.this.finishWithCallback(7);
                }
            }
        });
        this.mState = 2;
        addTimer(this.mState, this.mTargetAudioStatus ? 5000 : 2000);
    }

    private int getSystemAudioModeRequestParam() {
        if (tv().getActiveSource().isValid()) {
            return tv().getActiveSource().physicalAddress;
        }
        int param = tv().getActivePath();
        if (param != 65535) {
            return param;
        }
        return 0;
    }

    private void handleSendSystemAudioModeRequestTimeout() {
        if (this.mTargetAudioStatus) {
            int i = this.mSendRetryCount;
            this.mSendRetryCount = i + 1;
            if (i < 2) {
                sendSystemAudioModeRequest();
                return;
            }
        }
        HdmiLogger.debug("[T]:wait for <Set System Audio Mode>.", new Object[0]);
        setSystemAudioMode(false);
        finishWithCallback(1);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setSystemAudioMode(boolean mode) {
        tv().setSystemAudioMode(mode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public final boolean processCommand(HdmiCecMessage cmd) {
        if (cmd.getSource() != this.mAvrLogicalAddress) {
            return false;
        }
        switch (this.mState) {
            case 2:
                if (cmd.getOpcode() == 0 && (cmd.getParams()[0] & 255) == 112) {
                    HdmiLogger.debug("Failed to start system audio mode request.", new Object[0]);
                    setSystemAudioMode(false);
                    finishWithCallback(5);
                    return true;
                } else if (cmd.getOpcode() == 114 && HdmiUtils.checkCommandSource(cmd, this.mAvrLogicalAddress, TAG)) {
                    boolean receivedStatus = HdmiUtils.parseCommandParamSystemAudioStatus(cmd);
                    if (receivedStatus == this.mTargetAudioStatus) {
                        setSystemAudioMode(receivedStatus);
                        finish();
                        return true;
                    }
                    HdmiLogger.debug("Unexpected system audio mode request:" + receivedStatus, new Object[0]);
                    finishWithCallback(5);
                    return false;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void removeSystemAudioActionInProgress() {
        removeActionExcept(SystemAudioActionFromTv.class, this);
        removeActionExcept(SystemAudioActionFromAvr.class, this);
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    final void handleTimerEvent(int state) {
        if (this.mState != state) {
            return;
        }
        switch (this.mState) {
            case 2:
                handleSendSystemAudioModeRequestTimeout();
                return;
            default:
                return;
        }
    }
}
