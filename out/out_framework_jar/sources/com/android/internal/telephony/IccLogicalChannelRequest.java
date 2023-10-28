package com.android.internal.telephony;

import android.os.BadParcelableException;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
/* loaded from: classes4.dex */
public class IccLogicalChannelRequest implements Parcelable {
    public static final Parcelable.Creator<IccLogicalChannelRequest> CREATOR = new Parcelable.Creator<IccLogicalChannelRequest>() { // from class: com.android.internal.telephony.IccLogicalChannelRequest.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IccLogicalChannelRequest createFromParcel(Parcel _aidl_source) {
            IccLogicalChannelRequest _aidl_out = new IccLogicalChannelRequest();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public IccLogicalChannelRequest[] newArray(int _aidl_size) {
            return new IccLogicalChannelRequest[_aidl_size];
        }
    };
    public String aid;
    public IBinder binder;
    public String callingPackage;
    public int subId = -1;
    public int slotIndex = -1;
    public int portIndex = 0;
    public int p2 = 0;
    public int channel = -1;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.subId);
        _aidl_parcel.writeInt(this.slotIndex);
        _aidl_parcel.writeInt(this.portIndex);
        _aidl_parcel.writeString(this.callingPackage);
        _aidl_parcel.writeString(this.aid);
        _aidl_parcel.writeInt(this.p2);
        _aidl_parcel.writeInt(this.channel);
        _aidl_parcel.writeStrongBinder(this.binder);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [80=11, 81=10, 83=10] */
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
            this.subId = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.slotIndex = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.portIndex = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.callingPackage = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.aid = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.p2 = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.channel = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.binder = _aidl_parcel.readStrongBinder();
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
        _aidl_sj.add("subId: " + this.subId);
        _aidl_sj.add("slotIndex: " + this.slotIndex);
        _aidl_sj.add("portIndex: " + this.portIndex);
        _aidl_sj.add("callingPackage: " + Objects.toString(this.callingPackage));
        _aidl_sj.add("aid: " + Objects.toString(this.aid));
        _aidl_sj.add("p2: " + this.p2);
        _aidl_sj.add("channel: " + this.channel);
        _aidl_sj.add("binder: " + Objects.toString(this.binder));
        return "com.android.internal.telephony.IccLogicalChannelRequest" + _aidl_sj.toString();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof IccLogicalChannelRequest)) {
            return false;
        }
        IccLogicalChannelRequest that = (IccLogicalChannelRequest) other;
        if (Objects.deepEquals(Integer.valueOf(this.subId), Integer.valueOf(that.subId)) && Objects.deepEquals(Integer.valueOf(this.slotIndex), Integer.valueOf(that.slotIndex)) && Objects.deepEquals(Integer.valueOf(this.portIndex), Integer.valueOf(that.portIndex)) && Objects.deepEquals(this.callingPackage, that.callingPackage) && Objects.deepEquals(this.aid, that.aid) && Objects.deepEquals(Integer.valueOf(this.p2), Integer.valueOf(that.p2)) && Objects.deepEquals(Integer.valueOf(this.channel), Integer.valueOf(that.channel)) && Objects.deepEquals(this.binder, that.binder)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Arrays.deepHashCode(Arrays.asList(Integer.valueOf(this.subId), Integer.valueOf(this.slotIndex), Integer.valueOf(this.portIndex), this.callingPackage, this.aid, Integer.valueOf(this.p2), Integer.valueOf(this.channel), this.binder).toArray());
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
