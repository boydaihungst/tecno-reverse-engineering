package android.service.voice;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.content.pm.AppSearchShortcutInfo;
import android.content.res.Resources;
import android.media.AudioRecord;
import android.media.MediaSyncEvent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import com.android.internal.R;
import com.android.internal.util.AnnotationValidations;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
@SystemApi
/* loaded from: classes3.dex */
public final class HotwordDetectedResult implements Parcelable {
    public static final int AUDIO_CHANNEL_UNSET = -1;
    public static final int CONFIDENCE_LEVEL_HIGH = 5;
    public static final int CONFIDENCE_LEVEL_LOW = 1;
    public static final int CONFIDENCE_LEVEL_LOW_MEDIUM = 2;
    public static final int CONFIDENCE_LEVEL_MEDIUM = 3;
    public static final int CONFIDENCE_LEVEL_MEDIUM_HIGH = 4;
    public static final int CONFIDENCE_LEVEL_NONE = 0;
    public static final int CONFIDENCE_LEVEL_VERY_HIGH = 6;
    public static final int HOTWORD_OFFSET_UNSET = -1;
    private static final int LIMIT_AUDIO_CHANNEL_MAX_VALUE = 63;
    private static final int LIMIT_HOTWORD_OFFSET_MAX_VALUE = 3600000;
    private int mAudioChannel;
    private final int mConfidenceLevel;
    private final PersistableBundle mExtras;
    private boolean mHotwordDetectionPersonalized;
    private int mHotwordDurationMillis;
    private int mHotwordOffsetMillis;
    private final int mHotwordPhraseId;
    private MediaSyncEvent mMediaSyncEvent;
    private final int mPersonalizedScore;
    private final int mScore;
    private static int sMaxBundleSize = -1;
    public static final Parcelable.Creator<HotwordDetectedResult> CREATOR = new Parcelable.Creator<HotwordDetectedResult>() { // from class: android.service.voice.HotwordDetectedResult.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public HotwordDetectedResult[] newArray(int size) {
            return new HotwordDetectedResult[size];
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public HotwordDetectedResult createFromParcel(Parcel in) {
            return new HotwordDetectedResult(in);
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface ConfidenceLevel {
    }

    /* loaded from: classes3.dex */
    @interface HotwordConfidenceLevelValue {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    @interface Limit {
    }

    /* renamed from: -$$Nest$smdefaultConfidenceLevel  reason: not valid java name */
    static /* bridge */ /* synthetic */ int m3704$$Nest$smdefaultConfidenceLevel() {
        return defaultConfidenceLevel();
    }

    /* renamed from: -$$Nest$smdefaultExtras  reason: not valid java name */
    static /* bridge */ /* synthetic */ PersistableBundle m3705$$Nest$smdefaultExtras() {
        return defaultExtras();
    }

    /* renamed from: -$$Nest$smdefaultHotwordPhraseId  reason: not valid java name */
    static /* bridge */ /* synthetic */ int m3706$$Nest$smdefaultHotwordPhraseId() {
        return defaultHotwordPhraseId();
    }

    /* renamed from: -$$Nest$smdefaultPersonalizedScore  reason: not valid java name */
    static /* bridge */ /* synthetic */ int m3707$$Nest$smdefaultPersonalizedScore() {
        return defaultPersonalizedScore();
    }

    /* renamed from: -$$Nest$smdefaultScore  reason: not valid java name */
    static /* bridge */ /* synthetic */ int m3708$$Nest$smdefaultScore() {
        return defaultScore();
    }

    private static int defaultConfidenceLevel() {
        return 0;
    }

    private static int defaultScore() {
        return 0;
    }

    private static int defaultPersonalizedScore() {
        return 0;
    }

    public static int getMaxScore() {
        return 255;
    }

    private static int defaultHotwordPhraseId() {
        return 0;
    }

    public static int getMaxHotwordPhraseId() {
        return 63;
    }

    private static PersistableBundle defaultExtras() {
        return new PersistableBundle();
    }

    public static int getMaxBundleSize() {
        if (sMaxBundleSize < 0) {
            sMaxBundleSize = Resources.getSystem().getInteger(R.integer.config_hotwordDetectedResultMaxBundleSize);
        }
        return sMaxBundleSize;
    }

    public MediaSyncEvent getMediaSyncEvent() {
        return this.mMediaSyncEvent;
    }

    public static int getParcelableSize(Parcelable parcelable) {
        Parcel p = Parcel.obtain();
        parcelable.writeToParcel(p, 0);
        p.setDataPosition(0);
        int size = p.dataSize();
        p.recycle();
        return size;
    }

    public static int getUsageSize(HotwordDetectedResult hotwordDetectedResult) {
        int totalBits = hotwordDetectedResult.getConfidenceLevel() != defaultConfidenceLevel() ? 0 + bitCount(6L) : 0;
        if (hotwordDetectedResult.getHotwordOffsetMillis() != -1) {
            totalBits += bitCount(3600000L);
        }
        if (hotwordDetectedResult.getHotwordDurationMillis() != 0) {
            totalBits += bitCount(AudioRecord.getMaxSharedAudioHistoryMillis());
        }
        if (hotwordDetectedResult.getAudioChannel() != -1) {
            totalBits += bitCount(63L);
        }
        int totalBits2 = totalBits + 1;
        if (hotwordDetectedResult.getScore() != defaultScore()) {
            totalBits2 += bitCount(getMaxScore());
        }
        if (hotwordDetectedResult.getPersonalizedScore() != defaultPersonalizedScore()) {
            totalBits2 += bitCount(getMaxScore());
        }
        if (hotwordDetectedResult.getHotwordPhraseId() != defaultHotwordPhraseId()) {
            totalBits2 += bitCount(getMaxHotwordPhraseId());
        }
        PersistableBundle persistableBundle = hotwordDetectedResult.getExtras();
        if (!persistableBundle.isEmpty()) {
            return totalBits2 + (getParcelableSize(persistableBundle) * 8);
        }
        return totalBits2;
    }

    private static int bitCount(long value) {
        int bits = 0;
        while (value > 0) {
            bits++;
            value >>= 1;
        }
        return bits;
    }

    private void onConstructed() {
        Preconditions.checkArgumentInRange(this.mScore, 0, getMaxScore(), "score");
        Preconditions.checkArgumentInRange(this.mPersonalizedScore, 0, getMaxScore(), "personalizedScore");
        Preconditions.checkArgumentInRange(this.mHotwordPhraseId, 0, getMaxHotwordPhraseId(), "hotwordPhraseId");
        Preconditions.checkArgumentInRange(this.mHotwordDurationMillis, 0L, AudioRecord.getMaxSharedAudioHistoryMillis(), "hotwordDurationMillis");
        int i = this.mHotwordOffsetMillis;
        if (i != -1) {
            Preconditions.checkArgumentInRange(i, 0, (int) LIMIT_HOTWORD_OFFSET_MAX_VALUE, "hotwordOffsetMillis");
        }
        int i2 = this.mAudioChannel;
        if (i2 != -1) {
            Preconditions.checkArgumentInRange(i2, 0, 63, "audioChannel");
        }
        if (!this.mExtras.isEmpty()) {
            Preconditions.checkArgumentInRange(getParcelableSize(this.mExtras), 0, getMaxBundleSize(), AppSearchShortcutInfo.KEY_EXTRAS);
        }
    }

    public static String confidenceLevelToString(int value) {
        switch (value) {
            case 0:
                return "CONFIDENCE_LEVEL_NONE";
            case 1:
                return "CONFIDENCE_LEVEL_LOW";
            case 2:
                return "CONFIDENCE_LEVEL_LOW_MEDIUM";
            case 3:
                return "CONFIDENCE_LEVEL_MEDIUM";
            case 4:
                return "CONFIDENCE_LEVEL_MEDIUM_HIGH";
            case 5:
                return "CONFIDENCE_LEVEL_HIGH";
            case 6:
                return "CONFIDENCE_LEVEL_VERY_HIGH";
            default:
                return Integer.toHexString(value);
        }
    }

    static String limitToString(int value) {
        switch (value) {
            case 63:
                return "LIMIT_AUDIO_CHANNEL_MAX_VALUE";
            case LIMIT_HOTWORD_OFFSET_MAX_VALUE /* 3600000 */:
                return "LIMIT_HOTWORD_OFFSET_MAX_VALUE";
            default:
                return Integer.toHexString(value);
        }
    }

    HotwordDetectedResult(int confidenceLevel, MediaSyncEvent mediaSyncEvent, int hotwordOffsetMillis, int hotwordDurationMillis, int audioChannel, boolean hotwordDetectionPersonalized, int score, int personalizedScore, int hotwordPhraseId, PersistableBundle extras) {
        this.mMediaSyncEvent = null;
        this.mHotwordOffsetMillis = -1;
        this.mHotwordDurationMillis = 0;
        this.mAudioChannel = -1;
        this.mHotwordDetectionPersonalized = false;
        this.mConfidenceLevel = confidenceLevel;
        AnnotationValidations.validate((Class<? extends Annotation>) HotwordConfidenceLevelValue.class, (Annotation) null, confidenceLevel);
        this.mMediaSyncEvent = mediaSyncEvent;
        this.mHotwordOffsetMillis = hotwordOffsetMillis;
        this.mHotwordDurationMillis = hotwordDurationMillis;
        this.mAudioChannel = audioChannel;
        this.mHotwordDetectionPersonalized = hotwordDetectionPersonalized;
        this.mScore = score;
        this.mPersonalizedScore = personalizedScore;
        this.mHotwordPhraseId = hotwordPhraseId;
        this.mExtras = extras;
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) extras);
        onConstructed();
    }

    public int getConfidenceLevel() {
        return this.mConfidenceLevel;
    }

    public int getHotwordOffsetMillis() {
        return this.mHotwordOffsetMillis;
    }

    public int getHotwordDurationMillis() {
        return this.mHotwordDurationMillis;
    }

    public int getAudioChannel() {
        return this.mAudioChannel;
    }

    public boolean isHotwordDetectionPersonalized() {
        return this.mHotwordDetectionPersonalized;
    }

    public int getScore() {
        return this.mScore;
    }

    public int getPersonalizedScore() {
        return this.mPersonalizedScore;
    }

    public int getHotwordPhraseId() {
        return this.mHotwordPhraseId;
    }

    public PersistableBundle getExtras() {
        return this.mExtras;
    }

    public String toString() {
        return "HotwordDetectedResult { confidenceLevel = " + this.mConfidenceLevel + ", mediaSyncEvent = " + this.mMediaSyncEvent + ", hotwordOffsetMillis = " + this.mHotwordOffsetMillis + ", hotwordDurationMillis = " + this.mHotwordDurationMillis + ", audioChannel = " + this.mAudioChannel + ", hotwordDetectionPersonalized = " + this.mHotwordDetectionPersonalized + ", score = " + this.mScore + ", personalizedScore = " + this.mPersonalizedScore + ", hotwordPhraseId = " + this.mHotwordPhraseId + ", extras = " + this.mExtras + " }";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HotwordDetectedResult that = (HotwordDetectedResult) o;
        if (this.mConfidenceLevel == that.mConfidenceLevel && Objects.equals(this.mMediaSyncEvent, that.mMediaSyncEvent) && this.mHotwordOffsetMillis == that.mHotwordOffsetMillis && this.mHotwordDurationMillis == that.mHotwordDurationMillis && this.mAudioChannel == that.mAudioChannel && this.mHotwordDetectionPersonalized == that.mHotwordDetectionPersonalized && this.mScore == that.mScore && this.mPersonalizedScore == that.mPersonalizedScore && this.mHotwordPhraseId == that.mHotwordPhraseId && Objects.equals(this.mExtras, that.mExtras)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int _hash = (1 * 31) + this.mConfidenceLevel;
        return (((((((((((((((((_hash * 31) + Objects.hashCode(this.mMediaSyncEvent)) * 31) + this.mHotwordOffsetMillis) * 31) + this.mHotwordDurationMillis) * 31) + this.mAudioChannel) * 31) + Boolean.hashCode(this.mHotwordDetectionPersonalized)) * 31) + this.mScore) * 31) + this.mPersonalizedScore) * 31) + this.mHotwordPhraseId) * 31) + Objects.hashCode(this.mExtras);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        int flg = this.mHotwordDetectionPersonalized ? 0 | 32 : 0;
        if (this.mMediaSyncEvent != null) {
            flg |= 2;
        }
        dest.writeInt(flg);
        dest.writeInt(this.mConfidenceLevel);
        MediaSyncEvent mediaSyncEvent = this.mMediaSyncEvent;
        if (mediaSyncEvent != null) {
            dest.writeTypedObject(mediaSyncEvent, flags);
        }
        dest.writeInt(this.mHotwordOffsetMillis);
        dest.writeInt(this.mHotwordDurationMillis);
        dest.writeInt(this.mAudioChannel);
        dest.writeInt(this.mScore);
        dest.writeInt(this.mPersonalizedScore);
        dest.writeInt(this.mHotwordPhraseId);
        dest.writeTypedObject(this.mExtras, flags);
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    HotwordDetectedResult(Parcel in) {
        this.mMediaSyncEvent = null;
        this.mHotwordOffsetMillis = -1;
        this.mHotwordDurationMillis = 0;
        this.mAudioChannel = -1;
        this.mHotwordDetectionPersonalized = false;
        int flg = in.readInt();
        boolean hotwordDetectionPersonalized = (flg & 32) != 0;
        int confidenceLevel = in.readInt();
        MediaSyncEvent mediaSyncEvent = (flg & 2) == 0 ? null : (MediaSyncEvent) in.readTypedObject(MediaSyncEvent.CREATOR);
        int hotwordOffsetMillis = in.readInt();
        int hotwordDurationMillis = in.readInt();
        int audioChannel = in.readInt();
        int score = in.readInt();
        int personalizedScore = in.readInt();
        int hotwordPhraseId = in.readInt();
        PersistableBundle extras = (PersistableBundle) in.readTypedObject(PersistableBundle.CREATOR);
        this.mConfidenceLevel = confidenceLevel;
        AnnotationValidations.validate((Class<? extends Annotation>) HotwordConfidenceLevelValue.class, (Annotation) null, confidenceLevel);
        this.mMediaSyncEvent = mediaSyncEvent;
        this.mHotwordOffsetMillis = hotwordOffsetMillis;
        this.mHotwordDurationMillis = hotwordDurationMillis;
        this.mAudioChannel = audioChannel;
        this.mHotwordDetectionPersonalized = hotwordDetectionPersonalized;
        this.mScore = score;
        this.mPersonalizedScore = personalizedScore;
        this.mHotwordPhraseId = hotwordPhraseId;
        this.mExtras = extras;
        AnnotationValidations.validate((Class<NonNull>) NonNull.class, (NonNull) null, (Object) extras);
        onConstructed();
    }

    /* loaded from: classes3.dex */
    public static final class Builder {
        private int mAudioChannel;
        private long mBuilderFieldsSet = 0;
        private int mConfidenceLevel;
        private PersistableBundle mExtras;
        private boolean mHotwordDetectionPersonalized;
        private int mHotwordDurationMillis;
        private int mHotwordOffsetMillis;
        private int mHotwordPhraseId;
        private MediaSyncEvent mMediaSyncEvent;
        private int mPersonalizedScore;
        private int mScore;

        public Builder setConfidenceLevel(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 1;
            this.mConfidenceLevel = value;
            return this;
        }

        public Builder setMediaSyncEvent(MediaSyncEvent value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 2;
            this.mMediaSyncEvent = value;
            return this;
        }

        public Builder setHotwordOffsetMillis(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 4;
            this.mHotwordOffsetMillis = value;
            return this;
        }

        public Builder setHotwordDurationMillis(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 8;
            this.mHotwordDurationMillis = value;
            return this;
        }

        public Builder setAudioChannel(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 16;
            this.mAudioChannel = value;
            return this;
        }

        public Builder setHotwordDetectionPersonalized(boolean value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 32;
            this.mHotwordDetectionPersonalized = value;
            return this;
        }

        public Builder setScore(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 64;
            this.mScore = value;
            return this;
        }

        public Builder setPersonalizedScore(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 128;
            this.mPersonalizedScore = value;
            return this;
        }

        public Builder setHotwordPhraseId(int value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 256;
            this.mHotwordPhraseId = value;
            return this;
        }

        public Builder setExtras(PersistableBundle value) {
            checkNotUsed();
            this.mBuilderFieldsSet |= 512;
            this.mExtras = value;
            return this;
        }

        public HotwordDetectedResult build() {
            checkNotUsed();
            long j = this.mBuilderFieldsSet | 1024;
            this.mBuilderFieldsSet = j;
            if ((j & 1) == 0) {
                this.mConfidenceLevel = HotwordDetectedResult.m3704$$Nest$smdefaultConfidenceLevel();
            }
            long j2 = this.mBuilderFieldsSet;
            if ((2 & j2) == 0) {
                this.mMediaSyncEvent = null;
            }
            if ((4 & j2) == 0) {
                this.mHotwordOffsetMillis = -1;
            }
            if ((8 & j2) == 0) {
                this.mHotwordDurationMillis = 0;
            }
            if ((16 & j2) == 0) {
                this.mAudioChannel = -1;
            }
            if ((32 & j2) == 0) {
                this.mHotwordDetectionPersonalized = false;
            }
            if ((j2 & 64) == 0) {
                this.mScore = HotwordDetectedResult.m3708$$Nest$smdefaultScore();
            }
            if ((this.mBuilderFieldsSet & 128) == 0) {
                this.mPersonalizedScore = HotwordDetectedResult.m3707$$Nest$smdefaultPersonalizedScore();
            }
            if ((this.mBuilderFieldsSet & 256) == 0) {
                this.mHotwordPhraseId = HotwordDetectedResult.m3706$$Nest$smdefaultHotwordPhraseId();
            }
            if ((this.mBuilderFieldsSet & 512) == 0) {
                this.mExtras = HotwordDetectedResult.m3705$$Nest$smdefaultExtras();
            }
            HotwordDetectedResult o = new HotwordDetectedResult(this.mConfidenceLevel, this.mMediaSyncEvent, this.mHotwordOffsetMillis, this.mHotwordDurationMillis, this.mAudioChannel, this.mHotwordDetectionPersonalized, this.mScore, this.mPersonalizedScore, this.mHotwordPhraseId, this.mExtras);
            return o;
        }

        private void checkNotUsed() {
            if ((this.mBuilderFieldsSet & 1024) != 0) {
                throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @Deprecated
    private void __metadata() {
    }
}
