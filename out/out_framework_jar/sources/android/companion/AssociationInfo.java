package android.companion;

import android.annotation.SystemApi;
import android.net.MacAddress;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;
import java.util.Date;
import java.util.Objects;
/* loaded from: classes.dex */
public final class AssociationInfo implements Parcelable {
    public static final Parcelable.Creator<AssociationInfo> CREATOR = new Parcelable.Creator<AssociationInfo>() { // from class: android.companion.AssociationInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AssociationInfo[] newArray(int size) {
            return new AssociationInfo[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AssociationInfo createFromParcel(Parcel in) {
            return new AssociationInfo(in);
        }
    };
    private static final String LAST_TIME_CONNECTED_NONE = "None";
    private final MacAddress mDeviceMacAddress;
    private final String mDeviceProfile;
    private final CharSequence mDisplayName;
    private final int mId;
    private final long mLastTimeConnectedMs;
    private final boolean mNotifyOnDeviceNearby;
    private final String mPackageName;
    private final boolean mSelfManaged;
    private final long mTimeApprovedMs;
    private final int mUserId;

    /* loaded from: classes.dex */
    public interface NonActionableBuilder {
        Builder setLastTimeConnected(long j);

        Builder setNotifyOnDeviceNearby(boolean z);
    }

    public AssociationInfo(int id, int userId, String packageName, MacAddress macAddress, CharSequence displayName, String deviceProfile, boolean selfManaged, boolean notifyOnDeviceNearby, long timeApprovedMs, long lastTimeConnectedMs) {
        if (id <= 0) {
            throw new IllegalArgumentException("Association ID should be greater than 0");
        }
        if (macAddress == null && displayName == null) {
            throw new IllegalArgumentException("MAC address and the Display Name must NOT be null at the same time");
        }
        this.mId = id;
        this.mUserId = userId;
        this.mPackageName = packageName;
        this.mDeviceMacAddress = macAddress;
        this.mDisplayName = displayName;
        this.mDeviceProfile = deviceProfile;
        this.mSelfManaged = selfManaged;
        this.mNotifyOnDeviceNearby = notifyOnDeviceNearby;
        this.mTimeApprovedMs = timeApprovedMs;
        this.mLastTimeConnectedMs = lastTimeConnectedMs;
    }

    public int getId() {
        return this.mId;
    }

    public int getUserId() {
        return this.mUserId;
    }

    @SystemApi
    public String getPackageName() {
        return this.mPackageName;
    }

    public MacAddress getDeviceMacAddress() {
        return this.mDeviceMacAddress;
    }

    public String getDeviceMacAddressAsString() {
        MacAddress macAddress = this.mDeviceMacAddress;
        if (macAddress != null) {
            return macAddress.toString().toUpperCase();
        }
        return null;
    }

    public CharSequence getDisplayName() {
        return this.mDisplayName;
    }

    public String getDeviceProfile() {
        return this.mDeviceProfile;
    }

    @SystemApi
    public boolean isSelfManaged() {
        return this.mSelfManaged;
    }

    public boolean isNotifyOnDeviceNearby() {
        return this.mNotifyOnDeviceNearby;
    }

    public long getTimeApprovedMs() {
        return this.mTimeApprovedMs;
    }

    public boolean belongsToPackage(int userId, String packageName) {
        return this.mUserId == userId && Objects.equals(this.mPackageName, packageName);
    }

    public Long getLastTimeConnectedMs() {
        return Long.valueOf(this.mLastTimeConnectedMs);
    }

    public boolean isLinkedTo(String addr) {
        if (this.mSelfManaged || addr == null) {
            return false;
        }
        try {
            MacAddress macAddress = MacAddress.fromString(addr);
            return macAddress.equals(this.mDeviceMacAddress);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean shouldBindWhenPresent() {
        return this.mNotifyOnDeviceNearby || this.mSelfManaged;
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id=").append(this.mId);
        if (this.mDeviceMacAddress != null) {
            sb.append(", addr=").append(getDeviceMacAddressAsString());
        }
        if (this.mSelfManaged) {
            sb.append(", self-managed");
        }
        sb.append(", pkg=u").append(this.mUserId).append('/').append(this.mPackageName);
        return sb.toString();
    }

    public String toString() {
        return "Association{mId=" + this.mId + ", mUserId=" + this.mUserId + ", mPackageName='" + this.mPackageName + DateFormat.QUOTE + ", mDeviceMacAddress=" + this.mDeviceMacAddress + ", mDisplayName='" + ((Object) this.mDisplayName) + DateFormat.QUOTE + ", mDeviceProfile='" + this.mDeviceProfile + DateFormat.QUOTE + ", mSelfManaged=" + this.mSelfManaged + ", mNotifyOnDeviceNearby=" + this.mNotifyOnDeviceNearby + ", mTimeApprovedMs=" + new Date(this.mTimeApprovedMs) + ", mLastTimeConnectedMs=" + (this.mLastTimeConnectedMs == Long.MAX_VALUE ? "None" : new Date(this.mLastTimeConnectedMs)) + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof AssociationInfo) {
            AssociationInfo that = (AssociationInfo) o;
            return this.mId == that.mId && this.mUserId == that.mUserId && this.mSelfManaged == that.mSelfManaged && this.mNotifyOnDeviceNearby == that.mNotifyOnDeviceNearby && this.mTimeApprovedMs == that.mTimeApprovedMs && this.mLastTimeConnectedMs == that.mLastTimeConnectedMs && Objects.equals(this.mPackageName, that.mPackageName) && Objects.equals(this.mDeviceMacAddress, that.mDeviceMacAddress) && Objects.equals(this.mDisplayName, that.mDisplayName) && Objects.equals(this.mDeviceProfile, that.mDeviceProfile);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mId), Integer.valueOf(this.mUserId), this.mPackageName, this.mDeviceMacAddress, this.mDisplayName, this.mDeviceProfile, Boolean.valueOf(this.mSelfManaged), Boolean.valueOf(this.mNotifyOnDeviceNearby), Long.valueOf(this.mTimeApprovedMs), Long.valueOf(this.mLastTimeConnectedMs));
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mId);
        dest.writeInt(this.mUserId);
        dest.writeString(this.mPackageName);
        dest.writeTypedObject(this.mDeviceMacAddress, 0);
        dest.writeCharSequence(this.mDisplayName);
        dest.writeString(this.mDeviceProfile);
        dest.writeBoolean(this.mSelfManaged);
        dest.writeBoolean(this.mNotifyOnDeviceNearby);
        dest.writeLong(this.mTimeApprovedMs);
        dest.writeLong(this.mLastTimeConnectedMs);
    }

    private AssociationInfo(Parcel in) {
        this.mId = in.readInt();
        this.mUserId = in.readInt();
        this.mPackageName = in.readString();
        this.mDeviceMacAddress = (MacAddress) in.readTypedObject(MacAddress.CREATOR);
        this.mDisplayName = in.readCharSequence();
        this.mDeviceProfile = in.readString();
        this.mSelfManaged = in.readBoolean();
        this.mNotifyOnDeviceNearby = in.readBoolean();
        this.mTimeApprovedMs = in.readLong();
        this.mLastTimeConnectedMs = in.readLong();
    }

    public static NonActionableBuilder builder(AssociationInfo info) {
        return new Builder();
    }

    /* loaded from: classes.dex */
    public static final class Builder implements NonActionableBuilder {
        private long mLastTimeConnectedMs;
        private boolean mNotifyOnDeviceNearby;
        private final AssociationInfo mOriginalInfo;

        private Builder(AssociationInfo info) {
            this.mOriginalInfo = info;
            this.mNotifyOnDeviceNearby = info.mNotifyOnDeviceNearby;
            this.mLastTimeConnectedMs = info.mLastTimeConnectedMs;
        }

        @Override // android.companion.AssociationInfo.NonActionableBuilder
        public Builder setLastTimeConnected(long lastTimeConnectedMs) {
            if (lastTimeConnectedMs < 0) {
                throw new IllegalArgumentException("lastTimeConnectedMs must not be negative! (Given " + lastTimeConnectedMs + " )");
            }
            this.mLastTimeConnectedMs = lastTimeConnectedMs;
            return this;
        }

        @Override // android.companion.AssociationInfo.NonActionableBuilder
        public Builder setNotifyOnDeviceNearby(boolean notifyOnDeviceNearby) {
            this.mNotifyOnDeviceNearby = notifyOnDeviceNearby;
            return this;
        }

        public AssociationInfo build() {
            return new AssociationInfo(this.mOriginalInfo.mId, this.mOriginalInfo.mUserId, this.mOriginalInfo.mPackageName, this.mOriginalInfo.mDeviceMacAddress, this.mOriginalInfo.mDisplayName, this.mOriginalInfo.mDeviceProfile, this.mOriginalInfo.mSelfManaged, this.mNotifyOnDeviceNearby, this.mOriginalInfo.mTimeApprovedMs, this.mLastTimeConnectedMs);
        }
    }
}
