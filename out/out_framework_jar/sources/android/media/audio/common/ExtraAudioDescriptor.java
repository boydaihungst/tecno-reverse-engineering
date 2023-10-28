package android.media.audio.common;

import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes2.dex */
public class ExtraAudioDescriptor implements Parcelable {
    public static final Parcelable.Creator<ExtraAudioDescriptor> CREATOR = new Parcelable.Creator<ExtraAudioDescriptor>() { // from class: android.media.audio.common.ExtraAudioDescriptor.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ExtraAudioDescriptor createFromParcel(Parcel _aidl_source) {
            ExtraAudioDescriptor _aidl_out = new ExtraAudioDescriptor();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ExtraAudioDescriptor[] newArray(int _aidl_size) {
            return new ExtraAudioDescriptor[_aidl_size];
        }
    };
    public byte[] audioDescriptor;
    public int standard = 0;
    public int encapsulationType = 0;

    @Override // android.os.Parcelable
    public final int getStability() {
        return 1;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.standard);
        _aidl_parcel.writeByteArray(this.audioDescriptor);
        _aidl_parcel.writeInt(this.encapsulationType);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [50=6, 51=5, 53=5] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    public final void readFromParcel(Parcel _aidl_parcel) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        int _aidl_parcelable_size = _aidl_parcel.readInt();
        try {
            if (_aidl_parcelable_size < 4) {
                throw new BadParcelableException("Parcelable too small");
            }
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.standard = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.audioDescriptor = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.encapsulationType = _aidl_parcel.readInt();
            if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                throw new BadParcelableException("Overflow in the size of parcelable");
            }
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
        } catch (Throwable th) {
            if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                throw new BadParcelableException("Overflow in the size of parcelable");
            }
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
            throw th;
        }
    }

    public String toString() {
        StringJoiner _aidl_sj = new StringJoiner(", ", "{", "}");
        _aidl_sj.add("standard: " + this.standard);
        _aidl_sj.add("audioDescriptor: " + Arrays.toString(this.audioDescriptor));
        _aidl_sj.add("encapsulationType: " + this.encapsulationType);
        return "android.media.audio.common.ExtraAudioDescriptor" + _aidl_sj.toString();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof ExtraAudioDescriptor)) {
            return false;
        }
        ExtraAudioDescriptor that = (ExtraAudioDescriptor) other;
        if (Objects.deepEquals(Integer.valueOf(this.standard), Integer.valueOf(that.standard)) && Objects.deepEquals(this.audioDescriptor, that.audioDescriptor) && Objects.deepEquals(Integer.valueOf(this.encapsulationType), Integer.valueOf(that.encapsulationType))) {
            return true;
        }
        return false;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v1, resolved type: java.io.Serializable[] */
    /* JADX WARN: Multi-variable type inference failed */
    public int hashCode() {
        return Arrays.deepHashCode(Arrays.asList(Integer.valueOf(this.standard), this.audioDescriptor, Integer.valueOf(this.encapsulationType)).toArray());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
