package android.media;

import android.media.IAudioService;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class VolumeInfo implements Parcelable {
    public static final Parcelable.Creator<VolumeInfo> CREATOR = new Parcelable.Creator<VolumeInfo>() { // from class: android.media.VolumeInfo.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VolumeInfo createFromParcel(Parcel p) {
            return new VolumeInfo(p);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VolumeInfo[] newArray(int size) {
            return new VolumeInfo[size];
        }
    };
    public static final int INDEX_NOT_SET = -100;
    private static final String TAG = "VolumeInfo";
    private static VolumeInfo sDefaultVolumeInfo;
    private static IAudioService sService;
    private final boolean mIsMuted;
    private final int mMaxVolIndex;
    private final int mMinVolIndex;
    private final int mStreamType;
    private final boolean mUsesStreamType;
    private final int mVolGroupId;
    private final int mVolIndex;

    private VolumeInfo(boolean usesStreamType, boolean isMuted, int volIndex, int minVolIndex, int maxVolIndex, int volGroupId, int streamType) {
        this.mUsesStreamType = usesStreamType;
        this.mIsMuted = isMuted;
        this.mVolIndex = volIndex;
        this.mMinVolIndex = minVolIndex;
        this.mMaxVolIndex = maxVolIndex;
        this.mVolGroupId = volGroupId;
        this.mStreamType = streamType;
    }

    public boolean hasStreamType() {
        return this.mUsesStreamType;
    }

    public int getStreamType() {
        if (!this.mUsesStreamType) {
            throw new IllegalStateException("VolumeInfo doesn't use stream types");
        }
        return this.mStreamType;
    }

    public boolean hasVolumeGroup() {
        return !this.mUsesStreamType;
    }

    public android.media.audiopolicy.AudioVolumeGroup getVolumeGroup() {
        if (this.mUsesStreamType) {
            throw new IllegalStateException("VolumeInfo doesn't use AudioVolumeGroup");
        }
        List<android.media.audiopolicy.AudioVolumeGroup> volGroups = android.media.audiopolicy.AudioVolumeGroup.getAudioVolumeGroups();
        for (android.media.audiopolicy.AudioVolumeGroup group : volGroups) {
            if (group.getId() == this.mVolGroupId) {
                return group;
            }
        }
        return null;
    }

    public boolean isMuted() {
        return this.mIsMuted;
    }

    public int getVolumeIndex() {
        return this.mVolIndex;
    }

    public int getMinVolumeIndex() {
        return this.mMinVolIndex;
    }

    public int getMaxVolumeIndex() {
        return this.mMaxVolIndex;
    }

    public static VolumeInfo getDefaultVolumeInfo() {
        if (sService == null) {
            IBinder b = ServiceManager.getService("audio");
            sService = IAudioService.Stub.asInterface(b);
        }
        if (sDefaultVolumeInfo == null) {
            try {
                sDefaultVolumeInfo = sService.getDefaultVolumeInfo();
            } catch (RemoteException e) {
                Log.e(TAG, "Error calling getDefaultVolumeInfo", e);
                return new Builder(3).build();
            }
        }
        return sDefaultVolumeInfo;
    }

    /* loaded from: classes2.dex */
    public static final class Builder {
        private boolean mIsMuted;
        private int mMaxVolIndex;
        private int mMinVolIndex;
        private int mStreamType;
        private boolean mUsesStreamType;
        private int mVolGroupId;
        private int mVolIndex;

        public Builder(int streamType) {
            this.mUsesStreamType = true;
            this.mStreamType = 3;
            this.mIsMuted = false;
            this.mVolIndex = -100;
            this.mMinVolIndex = -100;
            this.mMaxVolIndex = -100;
            this.mVolGroupId = Integer.MIN_VALUE;
            this.mUsesStreamType = true;
            this.mStreamType = streamType;
        }

        public Builder(android.media.audiopolicy.AudioVolumeGroup volGroup) {
            this.mUsesStreamType = true;
            this.mStreamType = 3;
            this.mIsMuted = false;
            this.mVolIndex = -100;
            this.mMinVolIndex = -100;
            this.mMaxVolIndex = -100;
            this.mVolGroupId = Integer.MIN_VALUE;
            Objects.requireNonNull(volGroup);
            this.mUsesStreamType = false;
            this.mStreamType = Integer.MIN_VALUE;
            this.mVolGroupId = volGroup.getId();
        }

        public Builder(VolumeInfo info) {
            this.mUsesStreamType = true;
            this.mStreamType = 3;
            this.mIsMuted = false;
            this.mVolIndex = -100;
            this.mMinVolIndex = -100;
            this.mMaxVolIndex = -100;
            this.mVolGroupId = Integer.MIN_VALUE;
            Objects.requireNonNull(info);
            this.mUsesStreamType = info.mUsesStreamType;
            this.mStreamType = info.mStreamType;
            this.mIsMuted = info.mIsMuted;
            this.mVolIndex = info.mVolIndex;
            this.mMinVolIndex = info.mMinVolIndex;
            this.mMaxVolIndex = info.mMaxVolIndex;
            this.mVolGroupId = info.mVolGroupId;
        }

        public Builder setMuted(boolean isMuted) {
            this.mIsMuted = isMuted;
            return this;
        }

        public Builder setVolumeIndex(int volIndex) {
            if (volIndex != -100 && volIndex < 0) {
                throw new IllegalArgumentException("Volume index cannot be negative");
            }
            this.mVolIndex = volIndex;
            return this;
        }

        public Builder setMinVolumeIndex(int minIndex) {
            if (minIndex != -100 && minIndex < 0) {
                throw new IllegalArgumentException("Min volume index cannot be negative");
            }
            this.mMinVolIndex = minIndex;
            return this;
        }

        public Builder setMaxVolumeIndex(int maxIndex) {
            if (maxIndex != -100 && maxIndex < 0) {
                throw new IllegalArgumentException("Max volume index cannot be negative");
            }
            this.mMaxVolIndex = maxIndex;
            return this;
        }

        public VolumeInfo build() {
            int i;
            int i2 = this.mVolIndex;
            if (i2 != -100) {
                int i3 = this.mMinVolIndex;
                if (i3 != -100 && i2 < i3) {
                    throw new IllegalArgumentException("Volume index:" + this.mVolIndex + " lower than min index:" + this.mMinVolIndex);
                }
                int i4 = this.mMaxVolIndex;
                if (i4 != -100 && i2 > i4) {
                    throw new IllegalArgumentException("Volume index:" + this.mVolIndex + " greater than max index:" + this.mMaxVolIndex);
                }
            }
            int i5 = this.mMinVolIndex;
            if (i5 != -100 && (i = this.mMaxVolIndex) != -100 && i5 > i) {
                throw new IllegalArgumentException("Min volume index:" + this.mMinVolIndex + " greater than max index:" + this.mMaxVolIndex);
            }
            return new VolumeInfo(this.mUsesStreamType, this.mIsMuted, this.mVolIndex, this.mMinVolIndex, this.mMaxVolIndex, this.mVolGroupId, this.mStreamType);
        }
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.mUsesStreamType), Integer.valueOf(this.mStreamType), Boolean.valueOf(this.mIsMuted), Integer.valueOf(this.mVolIndex), Integer.valueOf(this.mMinVolIndex), Integer.valueOf(this.mMaxVolIndex), Integer.valueOf(this.mVolGroupId));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VolumeInfo that = (VolumeInfo) o;
        if (this.mUsesStreamType == that.mUsesStreamType && this.mStreamType == that.mStreamType && this.mIsMuted == that.mIsMuted && this.mVolIndex == that.mVolIndex && this.mMinVolIndex == that.mMinVolIndex && this.mMaxVolIndex == that.mMaxVolIndex && this.mVolGroupId == that.mVolGroupId) {
            return true;
        }
        return false;
    }

    public String toString() {
        return new String("VolumeInfo:" + (this.mUsesStreamType ? " streamType:" + this.mStreamType : " volGroupId" + this.mVolGroupId) + " muted:" + this.mIsMuted + (this.mVolIndex != -100 ? " volIndex:" + this.mVolIndex : "") + (this.mMinVolIndex != -100 ? " min:" + this.mMinVolIndex : "") + (this.mMaxVolIndex != -100 ? " max:" + this.mMaxVolIndex : ""));
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(this.mUsesStreamType);
        dest.writeInt(this.mStreamType);
        dest.writeBoolean(this.mIsMuted);
        dest.writeInt(this.mVolIndex);
        dest.writeInt(this.mMinVolIndex);
        dest.writeInt(this.mMaxVolIndex);
        dest.writeInt(this.mVolGroupId);
    }

    private VolumeInfo(Parcel in) {
        this.mUsesStreamType = in.readBoolean();
        this.mStreamType = in.readInt();
        this.mIsMuted = in.readBoolean();
        this.mVolIndex = in.readInt();
        this.mMinVolIndex = in.readInt();
        this.mMaxVolIndex = in.readInt();
        this.mVolGroupId = in.readInt();
    }
}
