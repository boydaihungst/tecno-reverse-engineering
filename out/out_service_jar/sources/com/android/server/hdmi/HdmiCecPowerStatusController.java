package com.android.server.hdmi;

import com.android.server.hdmi.HdmiAnnotations;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class HdmiCecPowerStatusController {
    private final HdmiControlService mHdmiControlService;
    private int mPowerStatus = 1;

    /* JADX INFO: Access modifiers changed from: package-private */
    public HdmiCecPowerStatusController(HdmiControlService hdmiControlService) {
        this.mHdmiControlService = hdmiControlService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPowerStatus() {
        return this.mPowerStatus;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPowerStatusOn() {
        return this.mPowerStatus == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPowerStatusStandby() {
        return this.mPowerStatus == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPowerStatusTransientToOn() {
        return this.mPowerStatus == 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPowerStatusTransientToStandby() {
        return this.mPowerStatus == 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void setPowerStatus(int powerStatus) {
        setPowerStatus(powerStatus, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @HdmiAnnotations.ServiceThreadOnly
    public void setPowerStatus(int powerStatus, boolean sendPowerStatusUpdate) {
        if (powerStatus == this.mPowerStatus) {
            return;
        }
        this.mPowerStatus = powerStatus;
        if (sendPowerStatusUpdate && this.mHdmiControlService.getCecVersion() >= 6) {
            sendReportPowerStatus(this.mPowerStatus);
        }
    }

    private void sendReportPowerStatus(int powerStatus) {
        for (HdmiCecLocalDevice localDevice : this.mHdmiControlService.getAllLocalDevices()) {
            this.mHdmiControlService.sendCecCommand(HdmiCecMessageBuilder.buildReportPowerStatus(localDevice.getDeviceInfo().getLogicalAddress(), 15, powerStatus));
        }
    }
}
