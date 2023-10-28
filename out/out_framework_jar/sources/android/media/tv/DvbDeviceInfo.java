package android.media.tv;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
@SystemApi
/* loaded from: classes2.dex */
public final class DvbDeviceInfo implements Parcelable {
    public static final Parcelable.Creator<DvbDeviceInfo> CREATOR = new Parcelable.Creator<DvbDeviceInfo>() { // from class: android.media.tv.DvbDeviceInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DvbDeviceInfo createFromParcel(Parcel source) {
            try {
                return new DvbDeviceInfo(source);
            } catch (Exception e) {
                Log.e(DvbDeviceInfo.TAG, "Exception creating DvbDeviceInfo from parcel", e);
                return null;
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DvbDeviceInfo[] newArray(int size) {
            return new DvbDeviceInfo[size];
        }
    };
    static final String TAG = "DvbDeviceInfo";
    private final int mAdapterId;
    private final int mDeviceId;

    private DvbDeviceInfo(Parcel source) {
        this.mAdapterId = source.readInt();
        this.mDeviceId = source.readInt();
    }

    public DvbDeviceInfo(int adapterId, int deviceId) {
        this.mAdapterId = adapterId;
        this.mDeviceId = deviceId;
    }

    public int getAdapterId() {
        return this.mAdapterId;
    }

    public int getDeviceId() {
        return this.mDeviceId;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mAdapterId);
        dest.writeInt(this.mDeviceId);
    }
}
