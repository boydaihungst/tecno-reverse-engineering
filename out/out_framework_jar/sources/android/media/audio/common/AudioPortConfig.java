package android.media.audio.common;

import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes2.dex */
public class AudioPortConfig implements Parcelable {
    public static final Parcelable.Creator<AudioPortConfig> CREATOR = new Parcelable.Creator<AudioPortConfig>() { // from class: android.media.audio.common.AudioPortConfig.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AudioPortConfig createFromParcel(Parcel _aidl_source) {
            AudioPortConfig _aidl_out = new AudioPortConfig();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AudioPortConfig[] newArray(int _aidl_size) {
            return new AudioPortConfig[_aidl_size];
        }
    };
    public AudioChannelLayout channelMask;
    public AudioPortExt ext;
    public AudioIoFlags flags;
    public AudioFormatDescription format;
    public AudioGainConfig gain;
    public int id = 0;
    public int portId = 0;
    public Int sampleRate;

    @Override // android.os.Parcelable
    public final int getStability() {
        return 1;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.id);
        _aidl_parcel.writeInt(this.portId);
        _aidl_parcel.writeTypedObject(this.sampleRate, _aidl_flag);
        _aidl_parcel.writeTypedObject(this.channelMask, _aidl_flag);
        _aidl_parcel.writeTypedObject(this.format, _aidl_flag);
        _aidl_parcel.writeTypedObject(this.gain, _aidl_flag);
        _aidl_parcel.writeTypedObject(this.flags, _aidl_flag);
        _aidl_parcel.writeTypedObject(this.ext, _aidl_flag);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [70=11, 71=10, 73=10] */
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
            this.id = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.portId = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.sampleRate = (Int) _aidl_parcel.readTypedObject(Int.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.channelMask = (AudioChannelLayout) _aidl_parcel.readTypedObject(AudioChannelLayout.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.format = (AudioFormatDescription) _aidl_parcel.readTypedObject(AudioFormatDescription.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.gain = (AudioGainConfig) _aidl_parcel.readTypedObject(AudioGainConfig.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.flags = (AudioIoFlags) _aidl_parcel.readTypedObject(AudioIoFlags.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.ext = (AudioPortExt) _aidl_parcel.readTypedObject(AudioPortExt.CREATOR);
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
        _aidl_sj.add("id: " + this.id);
        _aidl_sj.add("portId: " + this.portId);
        _aidl_sj.add("sampleRate: " + Objects.toString(this.sampleRate));
        _aidl_sj.add("channelMask: " + Objects.toString(this.channelMask));
        _aidl_sj.add("format: " + Objects.toString(this.format));
        _aidl_sj.add("gain: " + Objects.toString(this.gain));
        _aidl_sj.add("flags: " + Objects.toString(this.flags));
        _aidl_sj.add("ext: " + Objects.toString(this.ext));
        return "android.media.audio.common.AudioPortConfig" + _aidl_sj.toString();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof AudioPortConfig)) {
            return false;
        }
        AudioPortConfig that = (AudioPortConfig) other;
        if (Objects.deepEquals(Integer.valueOf(this.id), Integer.valueOf(that.id)) && Objects.deepEquals(Integer.valueOf(this.portId), Integer.valueOf(that.portId)) && Objects.deepEquals(this.sampleRate, that.sampleRate) && Objects.deepEquals(this.channelMask, that.channelMask) && Objects.deepEquals(this.format, that.format) && Objects.deepEquals(this.gain, that.gain) && Objects.deepEquals(this.flags, that.flags) && Objects.deepEquals(this.ext, that.ext)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Arrays.deepHashCode(Arrays.asList(Integer.valueOf(this.id), Integer.valueOf(this.portId), this.sampleRate, this.channelMask, this.format, this.gain, this.flags, this.ext).toArray());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        int _mask = 0 | describeContents(this.sampleRate);
        return _mask | describeContents(this.channelMask) | describeContents(this.format) | describeContents(this.gain) | describeContents(this.flags) | describeContents(this.ext);
    }

    private int describeContents(Object _v) {
        if (_v == null || !(_v instanceof Parcelable)) {
            return 0;
        }
        return ((Parcelable) _v).describeContents();
    }
}
