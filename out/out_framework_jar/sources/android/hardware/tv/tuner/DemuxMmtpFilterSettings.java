package android.hardware.tv.tuner;

import android.os.BadParcelableException;
import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes2.dex */
public class DemuxMmtpFilterSettings implements Parcelable {
    public static final Parcelable.Creator<DemuxMmtpFilterSettings> CREATOR = new Parcelable.Creator<DemuxMmtpFilterSettings>() { // from class: android.hardware.tv.tuner.DemuxMmtpFilterSettings.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DemuxMmtpFilterSettings createFromParcel(Parcel _aidl_source) {
            DemuxMmtpFilterSettings _aidl_out = new DemuxMmtpFilterSettings();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DemuxMmtpFilterSettings[] newArray(int _aidl_size) {
            return new DemuxMmtpFilterSettings[_aidl_size];
        }
    };
    public DemuxMmtpFilterSettingsFilterSettings filterSettings;
    public int mmtpPid = 0;

    @Override // android.os.Parcelable
    public final int getStability() {
        return 1;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.mmtpPid);
        _aidl_parcel.writeTypedObject(this.filterSettings, _aidl_flag);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [49=4, 46=5, 47=4] */
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
            this.mmtpPid = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {
                    throw new BadParcelableException("Overflow in the size of parcelable");
                }
                _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
                return;
            }
            this.filterSettings = (DemuxMmtpFilterSettingsFilterSettings) _aidl_parcel.readTypedObject(DemuxMmtpFilterSettingsFilterSettings.CREATOR);
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
        int _mask = 0 | describeContents(this.filterSettings);
        return _mask;
    }

    private int describeContents(Object _v) {
        if (_v == null || !(_v instanceof Parcelable)) {
            return 0;
        }
        return ((Parcelable) _v).describeContents();
    }
}
