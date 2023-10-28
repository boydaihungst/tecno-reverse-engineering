package com.android.server.usb.hal.port;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes2.dex */
public final class RawPortInfo implements Parcelable {
    public static final Parcelable.Creator<RawPortInfo> CREATOR = new Parcelable.Creator<RawPortInfo>() { // from class: com.android.server.usb.hal.port.RawPortInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RawPortInfo createFromParcel(Parcel in) {
            String id = in.readString();
            int supportedModes = in.readInt();
            int supportedContaminantProtectionModes = in.readInt();
            int currentMode = in.readInt();
            boolean canChangeMode = in.readByte() != 0;
            int currentPowerRole = in.readInt();
            boolean canChangePowerRole = in.readByte() != 0;
            int currentDataRole = in.readInt();
            boolean canChangeDataRole = in.readByte() != 0;
            boolean supportsEnableContaminantPresenceProtection = in.readBoolean();
            int contaminantProtectionStatus = in.readInt();
            boolean supportsEnableContaminantPresenceDetection = in.readBoolean();
            int contaminantDetectionStatus = in.readInt();
            int usbDataStatus = in.readInt();
            boolean powerTransferLimited = in.readBoolean();
            int powerBrickConnectionStatus = in.readInt();
            return new RawPortInfo(id, supportedModes, supportedContaminantProtectionModes, currentMode, canChangeMode, currentPowerRole, canChangePowerRole, currentDataRole, canChangeDataRole, supportsEnableContaminantPresenceProtection, contaminantProtectionStatus, supportsEnableContaminantPresenceDetection, contaminantDetectionStatus, usbDataStatus, powerTransferLimited, powerBrickConnectionStatus);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public RawPortInfo[] newArray(int size) {
            return new RawPortInfo[size];
        }
    };
    public boolean canChangeDataRole;
    public boolean canChangeMode;
    public boolean canChangePowerRole;
    public int contaminantDetectionStatus;
    public int contaminantProtectionStatus;
    public int currentDataRole;
    public int currentMode;
    public int currentPowerRole;
    public final String portId;
    public int powerBrickConnectionStatus;
    public boolean powerTransferLimited;
    public final int supportedContaminantProtectionModes;
    public final int supportedModes;
    public boolean supportsEnableContaminantPresenceDetection;
    public boolean supportsEnableContaminantPresenceProtection;
    public int usbDataStatus;

    public RawPortInfo(String portId, int supportedModes) {
        this.portId = portId;
        this.supportedModes = supportedModes;
        this.supportedContaminantProtectionModes = 0;
        this.supportsEnableContaminantPresenceProtection = false;
        this.contaminantProtectionStatus = 0;
        this.supportsEnableContaminantPresenceDetection = false;
        this.contaminantDetectionStatus = 0;
        this.usbDataStatus = 0;
        this.powerTransferLimited = false;
        this.powerBrickConnectionStatus = 0;
    }

    public RawPortInfo(String portId, int supportedModes, int supportedContaminantProtectionModes, int currentMode, boolean canChangeMode, int currentPowerRole, boolean canChangePowerRole, int currentDataRole, boolean canChangeDataRole, boolean supportsEnableContaminantPresenceProtection, int contaminantProtectionStatus, boolean supportsEnableContaminantPresenceDetection, int contaminantDetectionStatus, int usbDataStatus, boolean powerTransferLimited, int powerBrickConnectionStatus) {
        this.portId = portId;
        this.supportedModes = supportedModes;
        this.supportedContaminantProtectionModes = supportedContaminantProtectionModes;
        this.currentMode = currentMode;
        this.canChangeMode = canChangeMode;
        this.currentPowerRole = currentPowerRole;
        this.canChangePowerRole = canChangePowerRole;
        this.currentDataRole = currentDataRole;
        this.canChangeDataRole = canChangeDataRole;
        this.supportsEnableContaminantPresenceProtection = supportsEnableContaminantPresenceProtection;
        this.contaminantProtectionStatus = contaminantProtectionStatus;
        this.supportsEnableContaminantPresenceDetection = supportsEnableContaminantPresenceDetection;
        this.contaminantDetectionStatus = contaminantDetectionStatus;
        this.usbDataStatus = usbDataStatus;
        this.powerTransferLimited = powerTransferLimited;
        this.powerBrickConnectionStatus = powerBrickConnectionStatus;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.portId);
        dest.writeInt(this.supportedModes);
        dest.writeInt(this.supportedContaminantProtectionModes);
        dest.writeInt(this.currentMode);
        dest.writeByte(this.canChangeMode ? (byte) 1 : (byte) 0);
        dest.writeInt(this.currentPowerRole);
        dest.writeByte(this.canChangePowerRole ? (byte) 1 : (byte) 0);
        dest.writeInt(this.currentDataRole);
        dest.writeByte(this.canChangeDataRole ? (byte) 1 : (byte) 0);
        dest.writeBoolean(this.supportsEnableContaminantPresenceProtection);
        dest.writeInt(this.contaminantProtectionStatus);
        dest.writeBoolean(this.supportsEnableContaminantPresenceDetection);
        dest.writeInt(this.contaminantDetectionStatus);
        dest.writeInt(this.usbDataStatus);
        dest.writeBoolean(this.powerTransferLimited);
        dest.writeInt(this.powerBrickConnectionStatus);
    }
}
