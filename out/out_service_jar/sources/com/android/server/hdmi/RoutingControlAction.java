package com.android.server.hdmi;

import android.hardware.hdmi.IHdmiControlCallback;
import android.util.Slog;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class RoutingControlAction extends HdmiCecFeatureAction {
    static final int STATE_WAIT_FOR_ROUTING_INFORMATION = 1;
    private static final String TAG = "RoutingControlAction";
    private static final int TIMEOUT_ROUTING_INFORMATION_MS = 1000;
    private int mCurrentRoutingPath;
    private final boolean mNotifyInputChange;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RoutingControlAction(HdmiCecLocalDevice localDevice, int path, IHdmiControlCallback callback) {
        super(localDevice, callback);
        this.mCurrentRoutingPath = path;
        this.mNotifyInputChange = callback == null;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        this.mState = 1;
        addTimer(this.mState, 1000);
        return true;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        int opcode = cmd.getOpcode();
        byte[] params = cmd.getParams();
        if (this.mState == 1 && opcode == 129) {
            int routingPath = HdmiUtils.twoBytesToInt(params);
            if (HdmiUtils.isInActiveRoutingPath(this.mCurrentRoutingPath, routingPath)) {
                this.mCurrentRoutingPath = routingPath;
                removeActionExcept(RoutingControlAction.class, this);
                addTimer(this.mState, 1000);
                return true;
            }
            return true;
        }
        return false;
    }

    private void updateActiveInput() {
        HdmiCecLocalDeviceTv tv = tv();
        tv.setPrevPortId(tv.getActivePortId());
        tv.updateActiveInput(this.mCurrentRoutingPath, this.mNotifyInputChange);
    }

    private void sendSetStreamPath() {
        sendCommand(HdmiCecMessageBuilder.buildSetStreamPath(getSourceAddress(), this.mCurrentRoutingPath));
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public void handleTimerEvent(int timeoutState) {
        if (this.mState != timeoutState || this.mState == 0) {
            Slog.w("CEC", "Timer in a wrong state. Ignored.");
            return;
        }
        switch (timeoutState) {
            case 1:
                updateActiveInput();
                sendSetStreamPath();
                finishWithCallback(0);
                return;
            default:
                Slog.e("CEC", "Invalid timeoutState (" + timeoutState + ").");
                return;
        }
    }
}
