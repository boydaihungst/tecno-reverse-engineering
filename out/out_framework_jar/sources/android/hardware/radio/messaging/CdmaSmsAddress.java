package android.hardware.radio.messaging;

import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.StringJoiner;
/* loaded from: classes2.dex */
public class CdmaSmsAddress implements Parcelable {
    public static final Parcelable.Creator<CdmaSmsAddress> CREATOR = new Parcelable.Creator<CdmaSmsAddress>() { // from class: android.hardware.radio.messaging.CdmaSmsAddress.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public CdmaSmsAddress createFromParcel(Parcel _aidl_source) {
            CdmaSmsAddress _aidl_out = new CdmaSmsAddress();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public CdmaSmsAddress[] newArray(int _aidl_size) {
            return new CdmaSmsAddress[_aidl_size];
        }
    };
    public static final int DIGIT_MODE_EIGHT_BIT = 1;
    public static final int DIGIT_MODE_FOUR_BIT = 0;
    public static final int NUMBER_PLAN_DATA = 3;
    public static final int NUMBER_PLAN_PRIVATE = 9;
    public static final int NUMBER_PLAN_RESERVED_10 = 10;
    public static final int NUMBER_PLAN_RESERVED_11 = 11;
    public static final int NUMBER_PLAN_RESERVED_12 = 12;
    public static final int NUMBER_PLAN_RESERVED_13 = 13;
    public static final int NUMBER_PLAN_RESERVED_14 = 14;
    public static final int NUMBER_PLAN_RESERVED_15 = 15;
    public static final int NUMBER_PLAN_RESERVED_2 = 2;
    public static final int NUMBER_PLAN_RESERVED_5 = 5;
    public static final int NUMBER_PLAN_RESERVED_6 = 6;
    public static final int NUMBER_PLAN_RESERVED_7 = 7;
    public static final int NUMBER_PLAN_RESERVED_8 = 8;
    public static final int NUMBER_PLAN_TELEPHONY = 1;
    public static final int NUMBER_PLAN_TELEX = 4;
    public static final int NUMBER_PLAN_UNKNOWN = 0;
    public static final int NUMBER_TYPE_ABBREVIATED = 6;
    public static final int NUMBER_TYPE_ALPHANUMERIC = 5;
    public static final int NUMBER_TYPE_INTERNATIONAL_OR_DATA_IP = 1;
    public static final int NUMBER_TYPE_NATIONAL_OR_INTERNET_MAIL = 2;
    public static final int NUMBER_TYPE_NETWORK = 3;
    public static final int NUMBER_TYPE_RESERVED_7 = 7;
    public static final int NUMBER_TYPE_SUBSCRIBER = 4;
    public static final int NUMBER_TYPE_UNKNOWN = 0;
    public byte[] digits;
    public int digitMode = 0;
    public boolean isNumberModeDataNetwork = false;
    public int numberType = 0;
    public int numberPlan = 0;

    @Override // android.os.Parcelable
    public final int getStability() {
        return 1;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.digitMode);
        _aidl_parcel.writeBoolean(this.isNumberModeDataNetwork);
        _aidl_parcel.writeInt(this.numberType);
        _aidl_parcel.writeInt(this.numberPlan);
        _aidl_parcel.writeByteArray(this.digits);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [57=8, 58=7, 60=7] */
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
            this.digitMode = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.isNumberModeDataNetwork = _aidl_parcel.readBoolean();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.numberType = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.numberPlan = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.digits = _aidl_parcel.createByteArray();
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
        _aidl_sj.add("digitMode: " + this.digitMode);
        _aidl_sj.add("isNumberModeDataNetwork: " + this.isNumberModeDataNetwork);
        _aidl_sj.add("numberType: " + this.numberType);
        _aidl_sj.add("numberPlan: " + this.numberPlan);
        _aidl_sj.add("digits: " + Arrays.toString(this.digits));
        return "android.hardware.radio.messaging.CdmaSmsAddress" + _aidl_sj.toString();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
