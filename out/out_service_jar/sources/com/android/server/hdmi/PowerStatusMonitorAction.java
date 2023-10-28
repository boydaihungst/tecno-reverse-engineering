package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.hdmi.IHdmiControlCallback;
import android.util.SparseIntArray;
import com.android.server.hdmi.HdmiControlService;
import java.util.List;
/* loaded from: classes.dex */
public class PowerStatusMonitorAction extends HdmiCecFeatureAction {
    private static final int INVALID_POWER_STATUS = -2;
    private static final int MONITORING_INTERVAL_MS = 60000;
    private static final int REPORT_POWER_STATUS_TIMEOUT_MS = 5000;
    private static final int STATE_WAIT_FOR_NEXT_MONITORING = 2;
    private static final int STATE_WAIT_FOR_REPORT_POWER_STATUS = 1;
    private static final String TAG = "PowerStatusMonitorAction";
    private final SparseIntArray mPowerStatus;

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public /* bridge */ /* synthetic */ void addCallback(IHdmiControlCallback iHdmiControlCallback) {
        super.addCallback(iHdmiControlCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PowerStatusMonitorAction(HdmiCecLocalDevice source) {
        super(source);
        this.mPowerStatus = new SparseIntArray();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        queryPowerStatus();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState == 1 && cmd.getOpcode() == 144) {
            return handleReportPowerStatus(cmd);
        }
        return false;
    }

    private boolean handleReportPowerStatus(HdmiCecMessage cmd) {
        int sourceAddress = cmd.getSource();
        int oldStatus = this.mPowerStatus.get(sourceAddress, -2);
        if (oldStatus == -2) {
            return false;
        }
        int newStatus = cmd.getParams()[0] & 255;
        updatePowerStatus(sourceAddress, newStatus, true);
        return true;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    void handleTimerEvent(int state) {
        switch (this.mState) {
            case 1:
                handleTimeout();
                return;
            case 2:
                queryPowerStatus();
                return;
            default:
                return;
        }
    }

    private void handleTimeout() {
        for (int i = 0; i < this.mPowerStatus.size(); i++) {
            int logicalAddress = this.mPowerStatus.keyAt(i);
            updatePowerStatus(logicalAddress, -1, false);
        }
        this.mPowerStatus.clear();
        this.mState = 2;
    }

    private void resetPowerStatus(List<HdmiDeviceInfo> deviceInfos) {
        this.mPowerStatus.clear();
        for (HdmiDeviceInfo info : deviceInfos) {
            if (localDevice().mService.getCecVersion() < 6 || info.getCecVersion() < 6) {
                this.mPowerStatus.append(info.getLogicalAddress(), info.getDevicePowerStatus());
            }
        }
    }

    private void queryPowerStatus() {
        List<HdmiDeviceInfo> deviceInfos = localDevice().mService.getHdmiCecNetwork().getDeviceInfoList(false);
        resetPowerStatus(deviceInfos);
        for (HdmiDeviceInfo info : deviceInfos) {
            if (localDevice().mService.getCecVersion() < 6 || info.getCecVersion() < 6) {
                final int logicalAddress = info.getLogicalAddress();
                sendCommand(HdmiCecMessageBuilder.buildGiveDevicePowerStatus(getSourceAddress(), logicalAddress), new HdmiControlService.SendMessageCallback() { // from class: com.android.server.hdmi.PowerStatusMonitorAction.1
                    @Override // com.android.server.hdmi.HdmiControlService.SendMessageCallback
                    public void onSendCompleted(int error) {
                        if (error != 0) {
                            PowerStatusMonitorAction.this.updatePowerStatus(logicalAddress, -1, true);
                        }
                    }
                });
            }
        }
        this.mState = 1;
        addTimer(2, 60000);
        addTimer(1, 5000);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePowerStatus(int logicalAddress, int newStatus, boolean remove) {
        localDevice().mService.getHdmiCecNetwork().updateDevicePowerStatus(logicalAddress, newStatus);
        if (remove) {
            this.mPowerStatus.delete(logicalAddress);
        }
    }
}
