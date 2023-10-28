package android.net;

import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes.dex */
public class UidRangeParcel implements Parcelable {
    public static final Parcelable.Creator<UidRangeParcel> CREATOR = new Parcelable.Creator<UidRangeParcel>() { // from class: android.net.UidRangeParcel.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public UidRangeParcel createFromParcel(Parcel _aidl_source) {
            return UidRangeParcel.internalCreateFromParcel(_aidl_source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public UidRangeParcel[] newArray(int _aidl_size) {
            return new UidRangeParcel[_aidl_size];
        }
    };
    public final int start;
    public final int stop;

    /* loaded from: classes.dex */
    public static final class Builder {
        private int start = 0;
        private int stop = 0;

        public Builder setStart(int start) {
            this.start = start;
            return this;
        }

        public Builder setStop(int stop) {
            this.stop = stop;
            return this;
        }

        public UidRangeParcel build() {
            return new UidRangeParcel(this.start, this.stop);
        }
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.start);
        _aidl_parcel.writeInt(this.stop);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    public UidRangeParcel(int start, int stop) {
        this.start = start;
        this.stop = stop;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[ARITH]}, finally: {[ARITH, CONSTRUCTOR, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [68=4, 69=4, 71=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public static UidRangeParcel internalCreateFromParcel(Parcel _aidl_parcel) {
        int i;
        Builder _aidl_parcelable_builder = new Builder();
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        int _aidl_parcelable_size = _aidl_parcel.readInt();
        try {
        } finally {
            if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                BadParcelableException badParcelableException = new BadParcelableException("Overflow in the size of parcelable");
            }
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
            return _aidl_parcelable_builder.build();
        }
        if (_aidl_parcelable_size >= 4) {
            _aidl_parcelable_builder.build();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                _aidl_parcelable_builder.build();
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
            } else {
                int _aidl_temp_start = _aidl_parcel.readInt();
                _aidl_parcelable_builder.setStart(_aidl_temp_start);
                if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                    _aidl_parcelable_builder.build();
                    if (_aidl_start_pos <= Integer.MAX_VALUE - _aidl_parcelable_size) {
                        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                        return _aidl_parcelable_builder.build();
                    }
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                int _aidl_temp_stop = _aidl_parcel.readInt();
                _aidl_parcelable_builder.setStop(_aidl_temp_stop);
                if (_aidl_start_pos > i) {
                    throw new BadParcelableException(r4);
                }
            }
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
            return _aidl_parcelable_builder.build();
        }
        throw new BadParcelableException("Parcelable too small");
    }

    public String toString() {
        StringJoiner _aidl_sj = new StringJoiner(", ", "{", "}");
        _aidl_sj.add("start: " + this.start);
        _aidl_sj.add("stop: " + this.stop);
        return "android.net.UidRangeParcel" + _aidl_sj.toString();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof UidRangeParcel)) {
            return false;
        }
        UidRangeParcel that = (UidRangeParcel) other;
        if (Objects.deepEquals(Integer.valueOf(this.start), Integer.valueOf(that.start)) && Objects.deepEquals(Integer.valueOf(this.stop), Integer.valueOf(that.stop))) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Arrays.deepHashCode(Arrays.asList(Integer.valueOf(this.start), Integer.valueOf(this.stop)).toArray());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
