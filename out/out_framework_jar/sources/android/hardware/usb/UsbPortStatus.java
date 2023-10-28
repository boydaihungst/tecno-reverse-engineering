package android.hardware.usb;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@SystemApi
/* loaded from: classes2.dex */
public final class UsbPortStatus implements Parcelable {
    public static final int CONTAMINANT_DETECTION_DETECTED = 3;
    public static final int CONTAMINANT_DETECTION_DISABLED = 1;
    public static final int CONTAMINANT_DETECTION_NOT_DETECTED = 2;
    public static final int CONTAMINANT_DETECTION_NOT_SUPPORTED = 0;
    public static final int CONTAMINANT_PROTECTION_DISABLED = 8;
    public static final int CONTAMINANT_PROTECTION_FORCE_DISABLE = 4;
    public static final int CONTAMINANT_PROTECTION_NONE = 0;
    public static final int CONTAMINANT_PROTECTION_SINK = 1;
    public static final int CONTAMINANT_PROTECTION_SOURCE = 2;
    public static final Parcelable.Creator<UsbPortStatus> CREATOR = new Parcelable.Creator<UsbPortStatus>() { // from class: android.hardware.usb.UsbPortStatus.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public UsbPortStatus createFromParcel(Parcel in) {
            int currentMode = in.readInt();
            int currentPowerRole = in.readInt();
            int currentDataRole = in.readInt();
            int supportedRoleCombinations = in.readInt();
            int contaminantProtectionStatus = in.readInt();
            int contaminantDetectionStatus = in.readInt();
            int usbDataStatus = in.readInt();
            boolean powerTransferLimited = in.readBoolean();
            int powerBrickConnectionStatus = in.readInt();
            return new UsbPortStatus(currentMode, currentPowerRole, currentDataRole, supportedRoleCombinations, contaminantProtectionStatus, contaminantDetectionStatus, usbDataStatus, powerTransferLimited, powerBrickConnectionStatus);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public UsbPortStatus[] newArray(int size) {
            return new UsbPortStatus[size];
        }
    };
    public static final int DATA_ROLE_DEVICE = 2;
    public static final int DATA_ROLE_HOST = 1;
    public static final int DATA_ROLE_NONE = 0;
    public static final int DATA_STATUS_DISABLED_CONTAMINANT = 4;
    public static final int DATA_STATUS_DISABLED_DEBUG = 32;
    public static final int DATA_STATUS_DISABLED_DOCK = 8;
    public static final int DATA_STATUS_DISABLED_FORCE = 16;
    public static final int DATA_STATUS_DISABLED_OVERHEAT = 2;
    public static final int DATA_STATUS_ENABLED = 1;
    public static final int DATA_STATUS_UNKNOWN = 0;
    public static final int MODE_AUDIO_ACCESSORY = 4;
    public static final int MODE_DEBUG_ACCESSORY = 8;
    public static final int MODE_DFP = 2;
    public static final int MODE_DUAL = 3;
    public static final int MODE_NONE = 0;
    public static final int MODE_UFP = 1;
    public static final int POWER_BRICK_STATUS_CONNECTED = 1;
    public static final int POWER_BRICK_STATUS_DISCONNECTED = 2;
    public static final int POWER_BRICK_STATUS_UNKNOWN = 0;
    public static final int POWER_ROLE_NONE = 0;
    public static final int POWER_ROLE_SINK = 2;
    public static final int POWER_ROLE_SOURCE = 1;
    private static final String TAG = "UsbPortStatus";
    private final int mContaminantDetectionStatus;
    private final int mContaminantProtectionStatus;
    private final int mCurrentDataRole;
    private final int mCurrentMode;
    private final int mCurrentPowerRole;
    private final int mPowerBrickConnectionStatus;
    private final boolean mPowerTransferLimited;
    private final int mSupportedRoleCombinations;
    private final int mUsbDataStatus;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface ContaminantDetectionStatus {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface ContaminantProtectionStatus {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface PowerBrickConnectionStatus {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface UsbDataRole {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface UsbDataStatus {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface UsbPortMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface UsbPowerRole {
    }

    public UsbPortStatus(int currentMode, int currentPowerRole, int currentDataRole, int supportedRoleCombinations, int contaminantProtectionStatus, int contaminantDetectionStatus, int usbDataStatus, boolean powerTransferLimited, int powerBrickConnectionStatus) {
        this.mCurrentMode = currentMode;
        this.mCurrentPowerRole = currentPowerRole;
        this.mCurrentDataRole = currentDataRole;
        this.mSupportedRoleCombinations = supportedRoleCombinations;
        this.mContaminantProtectionStatus = contaminantProtectionStatus;
        this.mContaminantDetectionStatus = contaminantDetectionStatus;
        this.mUsbDataStatus = usbDataStatus;
        this.mPowerTransferLimited = powerTransferLimited;
        this.mPowerBrickConnectionStatus = powerBrickConnectionStatus;
    }

    public UsbPortStatus(int currentMode, int currentPowerRole, int currentDataRole, int supportedRoleCombinations, int contaminantProtectionStatus, int contaminantDetectionStatus) {
        this.mCurrentMode = currentMode;
        this.mCurrentPowerRole = currentPowerRole;
        this.mCurrentDataRole = currentDataRole;
        this.mSupportedRoleCombinations = supportedRoleCombinations;
        this.mContaminantProtectionStatus = contaminantProtectionStatus;
        this.mContaminantDetectionStatus = contaminantDetectionStatus;
        this.mUsbDataStatus = 0;
        this.mPowerBrickConnectionStatus = 0;
        this.mPowerTransferLimited = false;
    }

    public boolean isConnected() {
        return this.mCurrentMode != 0;
    }

    public int getCurrentMode() {
        return this.mCurrentMode;
    }

    public int getCurrentPowerRole() {
        return this.mCurrentPowerRole;
    }

    public int getCurrentDataRole() {
        return this.mCurrentDataRole;
    }

    public boolean isRoleCombinationSupported(int powerRole, int dataRole) {
        return (this.mSupportedRoleCombinations & UsbPort.combineRolesAsBit(powerRole, dataRole)) != 0;
    }

    public int getSupportedRoleCombinations() {
        return this.mSupportedRoleCombinations;
    }

    public int getContaminantDetectionStatus() {
        return this.mContaminantDetectionStatus;
    }

    public int getContaminantProtectionStatus() {
        return this.mContaminantProtectionStatus;
    }

    public int getUsbDataStatus() {
        return this.mUsbDataStatus;
    }

    public boolean isPowerTransferLimited() {
        return this.mPowerTransferLimited;
    }

    public int getPowerBrickConnectionStatus() {
        return this.mPowerBrickConnectionStatus;
    }

    public String toString() {
        return "UsbPortStatus{connected=" + isConnected() + ", currentMode=" + UsbPort.modeToString(this.mCurrentMode) + ", currentPowerRole=" + UsbPort.powerRoleToString(this.mCurrentPowerRole) + ", currentDataRole=" + UsbPort.dataRoleToString(this.mCurrentDataRole) + ", supportedRoleCombinations=" + UsbPort.roleCombinationsToString(this.mSupportedRoleCombinations) + ", contaminantDetectionStatus=" + getContaminantDetectionStatus() + ", contaminantProtectionStatus=" + getContaminantProtectionStatus() + ", usbDataStatus=" + UsbPort.usbDataStatusToString(getUsbDataStatus()) + ", isPowerTransferLimited=" + isPowerTransferLimited() + ", powerBrickConnectionStatus=" + UsbPort.powerBrickConnectionStatusToString(getPowerBrickConnectionStatus()) + "}";
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mCurrentMode);
        dest.writeInt(this.mCurrentPowerRole);
        dest.writeInt(this.mCurrentDataRole);
        dest.writeInt(this.mSupportedRoleCombinations);
        dest.writeInt(this.mContaminantProtectionStatus);
        dest.writeInt(this.mContaminantDetectionStatus);
        dest.writeInt(this.mUsbDataStatus);
        dest.writeBoolean(this.mPowerTransferLimited);
        dest.writeInt(this.mPowerBrickConnectionStatus);
    }

    /* loaded from: classes2.dex */
    public static final class Builder {
        private boolean mPowerTransferLimited;
        private int mSupportedRoleCombinations;
        private int mCurrentMode = 0;
        private int mCurrentPowerRole = 0;
        private int mCurrentDataRole = 0;
        private int mContaminantProtectionStatus = 0;
        private int mContaminantDetectionStatus = 0;
        private int mUsbDataStatus = 0;
        private int mPowerBrickConnectionStatus = 0;

        public Builder setCurrentMode(int currentMode) {
            this.mCurrentMode = currentMode;
            return this;
        }

        public Builder setCurrentRoles(int currentPowerRole, int currentDataRole) {
            this.mCurrentPowerRole = currentPowerRole;
            this.mCurrentDataRole = currentDataRole;
            return this;
        }

        public Builder setSupportedRoleCombinations(int supportedRoleCombinations) {
            this.mSupportedRoleCombinations = supportedRoleCombinations;
            return this;
        }

        public Builder setContaminantStatus(int contaminantProtectionStatus, int contaminantDetectionStatus) {
            this.mContaminantProtectionStatus = contaminantProtectionStatus;
            this.mContaminantDetectionStatus = contaminantDetectionStatus;
            return this;
        }

        public Builder setPowerTransferLimited(boolean powerTransferLimited) {
            this.mPowerTransferLimited = powerTransferLimited;
            return this;
        }

        public Builder setUsbDataStatus(int usbDataStatus) {
            this.mUsbDataStatus = usbDataStatus;
            return this;
        }

        public Builder setPowerBrickConnectionStatus(int powerBrickConnectionStatus) {
            this.mPowerBrickConnectionStatus = powerBrickConnectionStatus;
            return this;
        }

        public UsbPortStatus build() {
            UsbPortStatus status = new UsbPortStatus(this.mCurrentMode, this.mCurrentPowerRole, this.mCurrentDataRole, this.mSupportedRoleCombinations, this.mContaminantProtectionStatus, this.mContaminantDetectionStatus, this.mUsbDataStatus, this.mPowerTransferLimited, this.mPowerBrickConnectionStatus);
            return status;
        }
    }
}
