package android.os.vibrator;

import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.VibrationEffect;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class PrebakedSegment extends VibrationEffectSegment {
    public static final Parcelable.Creator<PrebakedSegment> CREATOR = new Parcelable.Creator<PrebakedSegment>() { // from class: android.os.vibrator.PrebakedSegment.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PrebakedSegment createFromParcel(Parcel in) {
            in.readInt();
            return new PrebakedSegment(in);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PrebakedSegment[] newArray(int size) {
            return new PrebakedSegment[size];
        }
    };
    private final int mEffectId;
    private final int mEffectStrength;
    private final boolean mFallback;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PrebakedSegment(Parcel in) {
        this.mEffectId = in.readInt();
        this.mFallback = in.readByte() != 0;
        this.mEffectStrength = in.readInt();
    }

    public PrebakedSegment(int effectId, boolean shouldFallback, int effectStrength) {
        this.mEffectId = effectId;
        this.mFallback = shouldFallback;
        this.mEffectStrength = effectStrength;
    }

    public int getEffectId() {
        return this.mEffectId;
    }

    public int getEffectStrength() {
        return this.mEffectStrength;
    }

    public boolean shouldFallback() {
        return this.mFallback;
    }

    @Override // android.os.vibrator.VibrationEffectSegment
    public long getDuration() {
        return -1L;
    }

    @Override // android.os.vibrator.VibrationEffectSegment
    public boolean isHapticFeedbackCandidate() {
        switch (this.mEffectId) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 21:
                return true;
            default:
                return false;
        }
    }

    @Override // android.os.vibrator.VibrationEffectSegment
    public boolean hasNonZeroAmplitude() {
        return true;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.vibrator.VibrationEffectSegment
    public PrebakedSegment resolve(int defaultAmplitude) {
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.vibrator.VibrationEffectSegment
    public PrebakedSegment scale(float scaleFactor) {
        return this;
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // android.os.vibrator.VibrationEffectSegment
    public PrebakedSegment applyEffectStrength(int effectStrength) {
        if (effectStrength != this.mEffectStrength && isValidEffectStrength(effectStrength)) {
            return new PrebakedSegment(this.mEffectId, this.mFallback, effectStrength);
        }
        return this;
    }

    private static boolean isValidEffectStrength(int strength) {
        switch (strength) {
            case 0:
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    @Override // android.os.vibrator.VibrationEffectSegment
    public void validate() {
        switch (this.mEffectId) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 21:
                break;
            default:
                int[] ringtones = VibrationEffect.RINGTONES;
                int i = this.mEffectId;
                if (i < ringtones[0] || i > ringtones[ringtones.length - 1]) {
                    throw new IllegalArgumentException("Unknown prebaked effect type (value=" + this.mEffectId + NavigationBarInflaterView.KEY_CODE_END);
                }
                break;
        }
        if (!isValidEffectStrength(this.mEffectStrength)) {
            throw new IllegalArgumentException("Unknown prebaked effect strength (value=" + this.mEffectStrength + NavigationBarInflaterView.KEY_CODE_END);
        }
    }

    public boolean equals(Object o) {
        if (o instanceof PrebakedSegment) {
            PrebakedSegment other = (PrebakedSegment) o;
            return this.mEffectId == other.mEffectId && this.mFallback == other.mFallback && this.mEffectStrength == other.mEffectStrength;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mEffectId), Boolean.valueOf(this.mFallback), Integer.valueOf(this.mEffectStrength));
    }

    public String toString() {
        return "Prebaked{effect=" + VibrationEffect.effectIdToString(this.mEffectId) + ", strength=" + VibrationEffect.effectStrengthToString(this.mEffectStrength) + ", fallback=" + this.mFallback + "}";
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(1);
        out.writeInt(this.mEffectId);
        out.writeByte(this.mFallback ? (byte) 1 : (byte) 0);
        out.writeInt(this.mEffectStrength);
    }
}
