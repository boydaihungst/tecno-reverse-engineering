package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes2.dex */
public final class ParcelableUsbPort implements Parcelable {
    public static final Parcelable.Creator<ParcelableUsbPort> CREATOR = new Parcelable.Creator<ParcelableUsbPort>() { // from class: android.hardware.usb.ParcelableUsbPort.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelableUsbPort createFromParcel(Parcel in) {
            String id = in.readString();
            int supportedModes = in.readInt();
            int supportedContaminantProtectionModes = in.readInt();
            boolean supportsEnableContaminantPresenceProtection = in.readBoolean();
            boolean supportsEnableContaminantPresenceDetection = in.readBoolean();
            return new ParcelableUsbPort(id, supportedModes, supportedContaminantProtectionModes, supportsEnableContaminantPresenceProtection, supportsEnableContaminantPresenceDetection);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelableUsbPort[] newArray(int size) {
            return new ParcelableUsbPort[size];
        }
    };
    private final String mId;
    private final int mSupportedContaminantProtectionModes;
    private final int mSupportedModes;
    private final boolean mSupportsEnableContaminantPresenceDetection;
    private final boolean mSupportsEnableContaminantPresenceProtection;

    private ParcelableUsbPort(String id, int supportedModes, int supportedContaminantProtectionModes, boolean supportsEnableContaminantPresenceProtection, boolean supportsEnableContaminantPresenceDetection) {
        this.mId = id;
        this.mSupportedModes = supportedModes;
        this.mSupportedContaminantProtectionModes = supportedContaminantProtectionModes;
        this.mSupportsEnableContaminantPresenceProtection = supportsEnableContaminantPresenceProtection;
        this.mSupportsEnableContaminantPresenceDetection = supportsEnableContaminantPresenceDetection;
    }

    public static ParcelableUsbPort of(UsbPort port) {
        return new ParcelableUsbPort(port.getId(), port.getSupportedModes(), port.getSupportedContaminantProtectionModes(), port.supportsEnableContaminantPresenceProtection(), port.supportsEnableContaminantPresenceDetection());
    }

    public UsbPort getUsbPort(UsbManager usbManager) {
        return new UsbPort(usbManager, this.mId, this.mSupportedModes, this.mSupportedContaminantProtectionModes, this.mSupportsEnableContaminantPresenceProtection, this.mSupportsEnableContaminantPresenceDetection);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mId);
        dest.writeInt(this.mSupportedModes);
        dest.writeInt(this.mSupportedContaminantProtectionModes);
        dest.writeBoolean(this.mSupportsEnableContaminantPresenceProtection);
        dest.writeBoolean(this.mSupportsEnableContaminantPresenceDetection);
    }
}
