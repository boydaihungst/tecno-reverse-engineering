package android.media.audiopolicy;

import android.annotation.SystemApi;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.media.AudioAttributes;
import android.media.AudioSystem;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
@SystemApi
/* loaded from: classes2.dex */
public final class AudioProductStrategy implements Parcelable {
    public static final int DEFAULT_GROUP = -1;
    private static final String TAG = "AudioProductStrategy";
    private static List<AudioProductStrategy> sAudioProductStrategies;
    private final AudioAttributesGroup[] mAudioAttributesGroups;
    private int mId;
    private final String mName;
    private static final Object sLock = new Object();
    public static final Parcelable.Creator<AudioProductStrategy> CREATOR = new Parcelable.Creator<AudioProductStrategy>() { // from class: android.media.audiopolicy.AudioProductStrategy.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AudioProductStrategy createFromParcel(Parcel in) {
            String name = in.readString();
            int id = in.readInt();
            int nbAttributesGroups = in.readInt();
            AudioAttributesGroup[] aag = new AudioAttributesGroup[nbAttributesGroups];
            for (int index = 0; index < nbAttributesGroups; index++) {
                aag[index] = AudioAttributesGroup.CREATOR.createFromParcel(in);
            }
            return new AudioProductStrategy(name, id, aag);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AudioProductStrategy[] newArray(int size) {
            return new AudioProductStrategy[size];
        }
    };
    private static final AudioAttributes DEFAULT_ATTRIBUTES = new AudioAttributes.Builder().setCapturePreset(0).build();

    private static native int native_list_audio_product_strategies(ArrayList<AudioProductStrategy> arrayList);

    public static List<AudioProductStrategy> getAudioProductStrategies() {
        if (sAudioProductStrategies == null) {
            synchronized (sLock) {
                if (sAudioProductStrategies == null) {
                    sAudioProductStrategies = initializeAudioProductStrategies();
                }
            }
        }
        return sAudioProductStrategies;
    }

    public static AudioProductStrategy getAudioProductStrategyWithId(int id) {
        synchronized (sLock) {
            if (sAudioProductStrategies == null) {
                sAudioProductStrategies = initializeAudioProductStrategies();
            }
            for (AudioProductStrategy strategy : sAudioProductStrategies) {
                if (strategy.getId() == id) {
                    return strategy;
                }
            }
            return null;
        }
    }

    @SystemApi
    public static AudioProductStrategy createInvalidAudioProductStrategy(int id) {
        return new AudioProductStrategy("dummy strategy", id, new AudioAttributesGroup[0]);
    }

    public static AudioAttributes getAudioAttributesForStrategyWithLegacyStreamType(int streamType) {
        for (AudioProductStrategy productStrategy : getAudioProductStrategies()) {
            AudioAttributes aa = productStrategy.getAudioAttributesForLegacyStreamType(streamType);
            if (aa != null) {
                return aa;
            }
        }
        return new AudioAttributes.Builder().setContentType(0).setUsage(0).build();
    }

    public static int getLegacyStreamTypeForStrategyWithAudioAttributes(AudioAttributes audioAttributes) {
        Preconditions.checkNotNull(audioAttributes, "AudioAttributes must not be null");
        for (AudioProductStrategy productStrategy : getAudioProductStrategies()) {
            if (productStrategy.supportsAudioAttributes(audioAttributes)) {
                int streamType = productStrategy.getLegacyStreamTypeForAudioAttributes(audioAttributes);
                if (streamType == -1) {
                    Log.w(TAG, "Attributes " + audioAttributes.toString() + " ported by strategy " + productStrategy.getId() + " has no stream type associated, DO NOT USE STREAM TO CONTROL THE VOLUME");
                    return 3;
                } else if (streamType < AudioSystem.getNumStreamTypes()) {
                    return streamType;
                }
            }
        }
        return 3;
    }

    private static List<AudioProductStrategy> initializeAudioProductStrategies() {
        ArrayList<AudioProductStrategy> apsList = new ArrayList<>();
        int status = native_list_audio_product_strategies(apsList);
        if (status != 0) {
            Log.w(TAG, ": initializeAudioProductStrategies failed");
        }
        return apsList;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioProductStrategy thatStrategy = (AudioProductStrategy) o;
        if (this.mName == thatStrategy.mName && this.mId == thatStrategy.mId && this.mAudioAttributesGroups.equals(thatStrategy.mAudioAttributesGroups)) {
            return true;
        }
        return false;
    }

    private AudioProductStrategy(String name, int id, AudioAttributesGroup[] aag) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(aag, "AudioAttributesGroups must not be null");
        this.mName = name;
        this.mId = id;
        this.mAudioAttributesGroups = aag;
    }

    @SystemApi
    public int getId() {
        return this.mId;
    }

    @SystemApi
    public AudioAttributes getAudioAttributes() {
        AudioAttributesGroup[] audioAttributesGroupArr = this.mAudioAttributesGroups;
        return audioAttributesGroupArr.length == 0 ? new AudioAttributes.Builder().build() : audioAttributesGroupArr[0].getAudioAttributes();
    }

    public AudioAttributes getAudioAttributesForLegacyStreamType(int streamType) {
        AudioAttributesGroup[] audioAttributesGroupArr;
        for (AudioAttributesGroup aag : this.mAudioAttributesGroups) {
            if (aag.supportsStreamType(streamType)) {
                return aag.getAudioAttributes();
            }
        }
        return null;
    }

    public int getLegacyStreamTypeForAudioAttributes(AudioAttributes aa) {
        AudioAttributesGroup[] audioAttributesGroupArr;
        Preconditions.checkNotNull(aa, "AudioAttributes must not be null");
        for (AudioAttributesGroup aag : this.mAudioAttributesGroups) {
            if (aag.supportsAttributes(aa)) {
                return aag.getStreamType();
            }
        }
        return -1;
    }

    @SystemApi
    public boolean supportsAudioAttributes(AudioAttributes aa) {
        AudioAttributesGroup[] audioAttributesGroupArr;
        Preconditions.checkNotNull(aa, "AudioAttributes must not be null");
        for (AudioAttributesGroup aag : this.mAudioAttributesGroups) {
            if (aag.supportsAttributes(aa)) {
                return true;
            }
        }
        return false;
    }

    public int getVolumeGroupIdForLegacyStreamType(int streamType) {
        AudioAttributesGroup[] audioAttributesGroupArr;
        for (AudioAttributesGroup aag : this.mAudioAttributesGroups) {
            if (aag.supportsStreamType(streamType)) {
                return aag.getVolumeGroupId();
            }
        }
        return -1;
    }

    public int getVolumeGroupIdForAudioAttributes(AudioAttributes aa) {
        AudioAttributesGroup[] audioAttributesGroupArr;
        Preconditions.checkNotNull(aa, "AudioAttributes must not be null");
        for (AudioAttributesGroup aag : this.mAudioAttributesGroups) {
            if (aag.supportsAttributes(aa)) {
                return aag.getVolumeGroupId();
            }
        }
        return -1;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        AudioAttributesGroup[] audioAttributesGroupArr;
        dest.writeString(this.mName);
        dest.writeInt(this.mId);
        dest.writeInt(this.mAudioAttributesGroups.length);
        for (AudioAttributesGroup aag : this.mAudioAttributesGroups) {
            aag.writeToParcel(dest, flags);
        }
    }

    public String toString() {
        AudioAttributesGroup[] audioAttributesGroupArr;
        StringBuilder s = new StringBuilder();
        s.append("\n Name: ");
        s.append(this.mName);
        s.append(" Id: ");
        s.append(Integer.toString(this.mId));
        for (AudioAttributesGroup aag : this.mAudioAttributesGroups) {
            s.append(aag.toString());
        }
        return s.toString();
    }

    public static AudioAttributes getDefaultAttributes() {
        return DEFAULT_ATTRIBUTES;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean attributesMatches(AudioAttributes refAttr, AudioAttributes attr) {
        Preconditions.checkNotNull(refAttr, "refAttr must not be null");
        Preconditions.checkNotNull(attr, "attr must not be null");
        String refFormattedTags = TextUtils.join(NavigationBarInflaterView.GRAVITY_SEPARATOR, refAttr.getTags());
        String cliFormattedTags = TextUtils.join(NavigationBarInflaterView.GRAVITY_SEPARATOR, attr.getTags());
        if (refAttr.equals(DEFAULT_ATTRIBUTES)) {
            return false;
        }
        if (refAttr.getSystemUsage() == 0 || attr.getSystemUsage() == refAttr.getSystemUsage()) {
            if (refAttr.getContentType() == 0 || attr.getContentType() == refAttr.getContentType()) {
                if (refAttr.getAllFlags() == 0 || (attr.getAllFlags() != 0 && (attr.getAllFlags() & refAttr.getAllFlags()) == refAttr.getAllFlags())) {
                    return refFormattedTags.length() == 0 || refFormattedTags.equals(cliFormattedTags);
                }
                return false;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class AudioAttributesGroup implements Parcelable {
        public static final Parcelable.Creator<AudioAttributesGroup> CREATOR = new Parcelable.Creator<AudioAttributesGroup>() { // from class: android.media.audiopolicy.AudioProductStrategy.AudioAttributesGroup.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public AudioAttributesGroup createFromParcel(Parcel in) {
                int volumeGroupId = in.readInt();
                int streamType = in.readInt();
                int nbAttributes = in.readInt();
                AudioAttributes[] aa = new AudioAttributes[nbAttributes];
                for (int index = 0; index < nbAttributes; index++) {
                    aa[index] = AudioAttributes.CREATOR.createFromParcel(in);
                }
                return new AudioAttributesGroup(volumeGroupId, streamType, aa);
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public AudioAttributesGroup[] newArray(int size) {
                return new AudioAttributesGroup[size];
            }
        };
        private final AudioAttributes[] mAudioAttributes;
        private int mLegacyStreamType;
        private int mVolumeGroupId;

        AudioAttributesGroup(int volumeGroupId, int streamType, AudioAttributes[] audioAttributes) {
            this.mVolumeGroupId = volumeGroupId;
            this.mLegacyStreamType = streamType;
            this.mAudioAttributes = audioAttributes;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AudioAttributesGroup thatAag = (AudioAttributesGroup) o;
            if (this.mVolumeGroupId == thatAag.mVolumeGroupId && this.mLegacyStreamType == thatAag.mLegacyStreamType && this.mAudioAttributes.equals(thatAag.mAudioAttributes)) {
                return true;
            }
            return false;
        }

        public int getStreamType() {
            return this.mLegacyStreamType;
        }

        public int getVolumeGroupId() {
            return this.mVolumeGroupId;
        }

        public AudioAttributes getAudioAttributes() {
            AudioAttributes[] audioAttributesArr = this.mAudioAttributes;
            return audioAttributesArr.length == 0 ? new AudioAttributes.Builder().build() : audioAttributesArr[0];
        }

        public boolean supportsAttributes(AudioAttributes attributes) {
            AudioAttributes[] audioAttributesArr;
            for (AudioAttributes refAa : this.mAudioAttributes) {
                if (refAa.equals(attributes) || AudioProductStrategy.attributesMatches(refAa, attributes)) {
                    return true;
                }
            }
            return false;
        }

        public boolean supportsStreamType(int streamType) {
            return this.mLegacyStreamType == streamType;
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            AudioAttributes[] audioAttributesArr;
            dest.writeInt(this.mVolumeGroupId);
            dest.writeInt(this.mLegacyStreamType);
            dest.writeInt(this.mAudioAttributes.length);
            for (AudioAttributes attributes : this.mAudioAttributes) {
                attributes.writeToParcel(dest, flags | 1);
            }
        }

        public String toString() {
            AudioAttributes[] audioAttributesArr;
            StringBuilder s = new StringBuilder();
            s.append("\n    Legacy Stream Type: ");
            s.append(Integer.toString(this.mLegacyStreamType));
            s.append(" Volume Group Id: ");
            s.append(Integer.toString(this.mVolumeGroupId));
            for (AudioAttributes attribute : this.mAudioAttributes) {
                s.append("\n    -");
                s.append(attribute.toString());
            }
            return s.toString();
        }
    }
}
