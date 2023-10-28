package android.net.networkstack.aidl.ip;

import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes.dex */
public class ReachabilityLossInfoParcelable implements Parcelable {
    public static final Parcelable.Creator<ReachabilityLossInfoParcelable> CREATOR = new Parcelable.Creator<ReachabilityLossInfoParcelable>() { // from class: android.net.networkstack.aidl.ip.ReachabilityLossInfoParcelable.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ReachabilityLossInfoParcelable createFromParcel(Parcel _aidl_source) {
            return ReachabilityLossInfoParcelable.internalCreateFromParcel(_aidl_source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ReachabilityLossInfoParcelable[] newArray(int _aidl_size) {
            return new ReachabilityLossInfoParcelable[_aidl_size];
        }
    };
    public final String message;
    public final int reason;

    /* loaded from: classes.dex */
    public static final class Builder {
        private String message;
        private int reason;

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setReason(int reason) {
            this.reason = reason;
            return this;
        }

        public ReachabilityLossInfoParcelable build() {
            return new ReachabilityLossInfoParcelable(this.message, this.reason);
        }
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeString(this.message);
        _aidl_parcel.writeInt(this.reason);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    public ReachabilityLossInfoParcelable(String message, int reason) {
        this.message = message;
        this.reason = reason;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[ARITH]}, finally: {[ARITH, CONSTRUCTOR, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [67=4, 68=4, 70=4] */
    /* JADX INFO: Access modifiers changed from: private */
    public static ReachabilityLossInfoParcelable internalCreateFromParcel(Parcel _aidl_parcel) {
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
                String _aidl_temp_message = _aidl_parcel.readString();
                _aidl_parcelable_builder.setMessage(_aidl_temp_message);
                if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                    _aidl_parcelable_builder.build();
                    if (_aidl_start_pos <= Integer.MAX_VALUE - _aidl_parcelable_size) {
                        _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                        return _aidl_parcelable_builder.build();
                    }
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                int _aidl_temp_reason = _aidl_parcel.readInt();
                _aidl_parcelable_builder.setReason(_aidl_temp_reason);
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
        _aidl_sj.add("message: " + Objects.toString(this.message));
        _aidl_sj.add("reason: " + this.reason);
        return "android.net.networkstack.aidl.ip.ReachabilityLossInfoParcelable" + _aidl_sj.toString();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof ReachabilityLossInfoParcelable)) {
            return false;
        }
        ReachabilityLossInfoParcelable that = (ReachabilityLossInfoParcelable) other;
        if (Objects.deepEquals(this.message, that.message) && Objects.deepEquals(Integer.valueOf(this.reason), Integer.valueOf(that.reason))) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Arrays.deepHashCode(Arrays.asList(this.message, Integer.valueOf(this.reason)).toArray());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
