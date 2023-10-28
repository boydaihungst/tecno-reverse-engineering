package android.net.netd.aidl;

import android.net.UidRangeParcel;
import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes.dex */
public class NativeUidRangeConfig implements Parcelable {
    public static final Parcelable.Creator<NativeUidRangeConfig> CREATOR = new Parcelable.Creator<NativeUidRangeConfig>() { // from class: android.net.netd.aidl.NativeUidRangeConfig.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NativeUidRangeConfig createFromParcel(Parcel _aidl_source) {
            return NativeUidRangeConfig.internalCreateFromParcel(_aidl_source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NativeUidRangeConfig[] newArray(int _aidl_size) {
            return new NativeUidRangeConfig[_aidl_size];
        }
    };
    public final int netId;
    public final int subPriority;
    public final UidRangeParcel[] uidRanges;

    /* loaded from: classes.dex */
    public static final class Builder {
        private int netId = 0;
        private int subPriority = 0;
        private UidRangeParcel[] uidRanges;

        public Builder setNetId(int netId) {
            this.netId = netId;
            return this;
        }

        public Builder setUidRanges(UidRangeParcel[] uidRanges) {
            this.uidRanges = uidRanges;
            return this;
        }

        public Builder setSubPriority(int subPriority) {
            this.subPriority = subPriority;
            return this;
        }

        public NativeUidRangeConfig build() {
            return new NativeUidRangeConfig(this.netId, this.uidRanges, this.subPriority);
        }
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.netId);
        _aidl_parcel.writeTypedArray(this.uidRanges, _aidl_flag);
        _aidl_parcel.writeInt(this.subPriority);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    public NativeUidRangeConfig(int netId, UidRangeParcel[] uidRanges, int subPriority) {
        this.netId = netId;
        this.uidRanges = uidRanges;
        this.subPriority = subPriority;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[ARITH]}, finally: {[ARITH, CONSTRUCTOR, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [80=5, 81=5, 83=5] */
    /* JADX INFO: Access modifiers changed from: private */
    public static NativeUidRangeConfig internalCreateFromParcel(Parcel _aidl_parcel) {
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
                int _aidl_temp_netId = _aidl_parcel.readInt();
                _aidl_parcelable_builder.setNetId(_aidl_temp_netId);
                if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                    _aidl_parcelable_builder.build();
                    if (_aidl_start_pos <= Integer.MAX_VALUE - _aidl_parcelable_size) {
                        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                        return _aidl_parcelable_builder.build();
                    }
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                UidRangeParcel[] _aidl_temp_uidRanges = (UidRangeParcel[]) _aidl_parcel.createTypedArray(UidRangeParcel.CREATOR);
                _aidl_parcelable_builder.setUidRanges(_aidl_temp_uidRanges);
                if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                    _aidl_parcelable_builder.build();
                    if (_aidl_start_pos <= Integer.MAX_VALUE - _aidl_parcelable_size) {
                        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                        return _aidl_parcelable_builder.build();
                    }
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                int _aidl_temp_subPriority = _aidl_parcel.readInt();
                _aidl_parcelable_builder.setSubPriority(_aidl_temp_subPriority);
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
        _aidl_sj.add("netId: " + this.netId);
        _aidl_sj.add("uidRanges: " + Arrays.toString(this.uidRanges));
        _aidl_sj.add("subPriority: " + this.subPriority);
        return "android.net.netd.aidl.NativeUidRangeConfig" + _aidl_sj.toString();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof NativeUidRangeConfig)) {
            return false;
        }
        NativeUidRangeConfig that = (NativeUidRangeConfig) other;
        if (Objects.deepEquals(Integer.valueOf(this.netId), Integer.valueOf(that.netId)) && Objects.deepEquals(this.uidRanges, that.uidRanges) && Objects.deepEquals(Integer.valueOf(this.subPriority), Integer.valueOf(that.subPriority))) {
            return true;
        }
        return false;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v1, resolved type: java.io.Serializable[] */
    /* JADX WARN: Multi-variable type inference failed */
    public int hashCode() {
        return Arrays.deepHashCode(Arrays.asList(Integer.valueOf(this.netId), this.uidRanges, Integer.valueOf(this.subPriority)).toArray());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        int _mask = 0 | describeContents(this.uidRanges);
        return _mask;
    }

    private int describeContents(Object _v) {
        Object[] objArr;
        if (_v == null) {
            return 0;
        }
        if (_v instanceof Object[]) {
            int _mask = 0;
            for (Object o : (Object[]) _v) {
                _mask |= describeContents(o);
            }
            return _mask;
        } else if (!(_v instanceof Parcelable)) {
            return 0;
        } else {
            return ((Parcelable) _v).describeContents();
        }
    }
}
