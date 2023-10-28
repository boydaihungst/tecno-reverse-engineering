package android.hardware.camera2.impl;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public class PhysicalCaptureResultInfo implements Parcelable {
    public static final Parcelable.Creator<PhysicalCaptureResultInfo> CREATOR = new Parcelable.Creator<PhysicalCaptureResultInfo>() { // from class: android.hardware.camera2.impl.PhysicalCaptureResultInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PhysicalCaptureResultInfo createFromParcel(Parcel in) {
            return new PhysicalCaptureResultInfo(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PhysicalCaptureResultInfo[] newArray(int size) {
            return new PhysicalCaptureResultInfo[size];
        }
    };
    private String cameraId;
    private CameraMetadataNative cameraMetadata;

    private PhysicalCaptureResultInfo(Parcel in) {
        readFromParcel(in);
    }

    public PhysicalCaptureResultInfo(String cameraId, CameraMetadataNative cameraMetadata) {
        this.cameraId = cameraId;
        this.cameraMetadata = cameraMetadata;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.cameraId);
        this.cameraMetadata.writeToParcel(dest, flags);
    }

    public void readFromParcel(Parcel in) {
        this.cameraId = in.readString();
        CameraMetadataNative cameraMetadataNative = new CameraMetadataNative();
        this.cameraMetadata = cameraMetadataNative;
        cameraMetadataNative.readFromParcel(in);
    }

    public String getCameraId() {
        return this.cameraId;
    }

    public CameraMetadataNative getCameraMetadata() {
        return this.cameraMetadata;
    }
}
