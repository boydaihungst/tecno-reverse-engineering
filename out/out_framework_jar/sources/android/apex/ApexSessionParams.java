package android.apex;

import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes.dex */
public class ApexSessionParams implements Parcelable {
    public static final Parcelable.Creator<ApexSessionParams> CREATOR = new Parcelable.Creator<ApexSessionParams>() { // from class: android.apex.ApexSessionParams.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ApexSessionParams createFromParcel(Parcel _aidl_source) {
            ApexSessionParams _aidl_out = new ApexSessionParams();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ApexSessionParams[] newArray(int _aidl_size) {
            return new ApexSessionParams[_aidl_size];
        }
    };
    public int sessionId = 0;
    public int[] childSessionIds = new int[0];
    public boolean hasRollbackEnabled = false;
    public boolean isRollback = false;
    public int rollbackId = 0;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.sessionId);
        _aidl_parcel.writeIntArray(this.childSessionIds);
        _aidl_parcel.writeInt(this.hasRollbackEnabled ? 1 : 0);
        _aidl_parcel.writeInt(this.isRollback ? 1 : 0);
        _aidl_parcel.writeInt(this.rollbackId);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [55=8, 56=7, 58=7] */
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
            this.sessionId = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.childSessionIds = _aidl_parcel.createIntArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            boolean z = true;
            this.hasRollbackEnabled = _aidl_parcel.readInt() != 0;
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            if (_aidl_parcel.readInt() == 0) {
                z = false;
            }
            this.isRollback = z;
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.rollbackId = _aidl_parcel.readInt();
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

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}
