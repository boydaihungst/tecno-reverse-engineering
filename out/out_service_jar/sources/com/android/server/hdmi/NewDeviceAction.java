package com.android.server.hdmi;

import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.util.Slog;
import com.android.server.hdmi.HdmiCecLocalDevice;
import java.io.UnsupportedEncodingException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class NewDeviceAction extends HdmiCecFeatureAction {
    static final int STATE_WAITING_FOR_DEVICE_VENDOR_ID = 2;
    static final int STATE_WAITING_FOR_SET_OSD_NAME = 1;
    private static final String TAG = "NewDeviceAction";
    private final int mDeviceLogicalAddress;
    private final int mDevicePhysicalAddress;
    private final int mDeviceType;
    private String mDisplayName;
    private int mTimeoutRetry;
    private int mVendorId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public NewDeviceAction(HdmiCecLocalDevice source, int deviceLogicalAddress, int devicePhysicalAddress, int deviceType) {
        super(source);
        this.mDeviceLogicalAddress = deviceLogicalAddress;
        this.mDevicePhysicalAddress = devicePhysicalAddress;
        this.mDeviceType = deviceType;
        this.mVendorId = AudioFormat.SUB_MASK;
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean start() {
        requestOsdName(true);
        return true;
    }

    private void requestOsdName(boolean firstTry) {
        if (firstTry) {
            this.mTimeoutRetry = 0;
        }
        this.mState = 1;
        if (mayProcessCommandIfCached(this.mDeviceLogicalAddress, 71)) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildGiveOsdNameCommand(getSourceAddress(), this.mDeviceLogicalAddress));
        addTimer(this.mState, 2000);
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public boolean processCommand(HdmiCecMessage cmd) {
        int opcode = cmd.getOpcode();
        int src = cmd.getSource();
        byte[] params = cmd.getParams();
        if (this.mDeviceLogicalAddress != src) {
            return false;
        }
        if (this.mState == 1) {
            if (opcode == 71) {
                try {
                    this.mDisplayName = new String(params, "US-ASCII");
                } catch (UnsupportedEncodingException e) {
                    Slog.e(TAG, "Failed to get OSD name: " + e.getMessage());
                }
                requestVendorId(true);
                return true;
            } else if (opcode == 0) {
                int requestOpcode = params[0] & 255;
                if (requestOpcode == 70) {
                    requestVendorId(true);
                    return true;
                }
            }
        } else if (this.mState == 2) {
            if (opcode == 135) {
                this.mVendorId = HdmiUtils.threeBytesToInt(params);
                addDeviceInfo();
                finish();
                return true;
            } else if (opcode == 0) {
                int requestOpcode2 = params[0] & 255;
                if (requestOpcode2 == 140) {
                    addDeviceInfo();
                    finish();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean mayProcessCommandIfCached(int destAddress, int opcode) {
        HdmiCecMessage message = getCecMessageCache().getMessage(destAddress, opcode);
        if (message != null) {
            return processCommand(message);
        }
        return false;
    }

    private void requestVendorId(boolean firstTry) {
        if (firstTry) {
            this.mTimeoutRetry = 0;
        }
        this.mState = 2;
        if (mayProcessCommandIfCached(this.mDeviceLogicalAddress, 135)) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildGiveDeviceVendorIdCommand(getSourceAddress(), this.mDeviceLogicalAddress));
        addTimer(this.mState, 2000);
    }

    private void addDeviceInfo() {
        if (!localDevice().mService.getHdmiCecNetwork().isInDeviceList(this.mDeviceLogicalAddress, this.mDevicePhysicalAddress)) {
            Slog.w(TAG, String.format("Device not found (%02x, %04x)", Integer.valueOf(this.mDeviceLogicalAddress), Integer.valueOf(this.mDevicePhysicalAddress)));
            return;
        }
        if (this.mDisplayName == null) {
            this.mDisplayName = HdmiUtils.getDefaultDeviceName(this.mDeviceLogicalAddress);
        }
        HdmiDeviceInfo deviceInfo = HdmiDeviceInfo.cecDeviceBuilder().setLogicalAddress(this.mDeviceLogicalAddress).setPhysicalAddress(this.mDevicePhysicalAddress).setPortId(tv().getPortId(this.mDevicePhysicalAddress)).setDeviceType(this.mDeviceType).setVendorId(this.mVendorId).setDisplayName(this.mDisplayName).build();
        localDevice().mService.getHdmiCecNetwork().addCecDevice(deviceInfo);
        tv().processDelayedMessages(this.mDeviceLogicalAddress);
        if (HdmiUtils.isEligibleAddressForDevice(5, this.mDeviceLogicalAddress)) {
            tv().onNewAvrAdded(deviceInfo);
        }
    }

    @Override // com.android.server.hdmi.HdmiCecFeatureAction
    public void handleTimerEvent(int state) {
        if (this.mState == 0 || this.mState != state) {
            return;
        }
        if (state == 1) {
            int i = this.mTimeoutRetry + 1;
            this.mTimeoutRetry = i;
            if (i < 5) {
                requestOsdName(false);
            } else {
                requestVendorId(true);
            }
        } else if (state == 2) {
            int i2 = this.mTimeoutRetry + 1;
            this.mTimeoutRetry = i2;
            if (i2 < 5) {
                requestVendorId(false);
                return;
            }
            addDeviceInfo();
            finish();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isActionOf(HdmiCecLocalDevice.ActiveSource activeSource) {
        return this.mDeviceLogicalAddress == activeSource.logicalAddress && this.mDevicePhysicalAddress == activeSource.physicalAddress;
    }
}
