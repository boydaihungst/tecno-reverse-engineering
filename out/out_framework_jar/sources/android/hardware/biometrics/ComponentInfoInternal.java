package android.hardware.biometrics;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public class ComponentInfoInternal implements Parcelable {
    public static final Parcelable.Creator<ComponentInfoInternal> CREATOR = new Parcelable.Creator<ComponentInfoInternal>() { // from class: android.hardware.biometrics.ComponentInfoInternal.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ComponentInfoInternal createFromParcel(Parcel in) {
            return new ComponentInfoInternal(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ComponentInfoInternal[] newArray(int size) {
            return new ComponentInfoInternal[size];
        }
    };
    public final String componentId;
    public final String firmwareVersion;
    public final String hardwareVersion;
    public final String serialNumber;
    public final String softwareVersion;

    public static ComponentInfoInternal from(ComponentInfoInternal comp) {
        return new ComponentInfoInternal(comp.componentId, comp.hardwareVersion, comp.firmwareVersion, comp.serialNumber, comp.softwareVersion);
    }

    public ComponentInfoInternal(String componentId, String hardwareVersion, String firmwareVersion, String serialNumber, String softwareVersion) {
        this.componentId = componentId;
        this.hardwareVersion = hardwareVersion;
        this.firmwareVersion = firmwareVersion;
        this.serialNumber = serialNumber;
        this.softwareVersion = softwareVersion;
    }

    protected ComponentInfoInternal(Parcel in) {
        this.componentId = in.readString();
        this.hardwareVersion = in.readString();
        this.firmwareVersion = in.readString();
        this.serialNumber = in.readString();
        this.softwareVersion = in.readString();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.componentId);
        dest.writeString(this.hardwareVersion);
        dest.writeString(this.firmwareVersion);
        dest.writeString(this.serialNumber);
        dest.writeString(this.softwareVersion);
    }

    public String toString() {
        return "ComponentId: " + this.componentId + ", HardwareVersion: " + this.hardwareVersion + ", FirmwareVersion: " + this.firmwareVersion + ", SerialNumber " + this.serialNumber + ", SoftwareVersion: " + this.softwareVersion;
    }
}
