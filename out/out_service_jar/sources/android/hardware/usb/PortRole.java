package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.server.app.GameManagerService;
/* loaded from: classes.dex */
public final class PortRole implements Parcelable {
    public static final Parcelable.Creator<PortRole> CREATOR = new Parcelable.Creator<PortRole>() { // from class: android.hardware.usb.PortRole.1
        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PortRole createFromParcel(Parcel _aidl_source) {
            return new PortRole(_aidl_source);
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PortRole[] newArray(int _aidl_size) {
            return new PortRole[_aidl_size];
        }
    };
    public static final int dataRole = 1;
    public static final int mode = 2;
    public static final int powerRole = 0;
    private int _tag;
    private Object _value;

    /* loaded from: classes.dex */
    public @interface Tag {
        public static final int dataRole = 1;
        public static final int mode = 2;
        public static final int powerRole = 0;
    }

    public PortRole() {
        this._tag = 0;
        this._value = (byte) 0;
    }

    private PortRole(Parcel _aidl_parcel) {
        readFromParcel(_aidl_parcel);
    }

    private PortRole(int _tag, Object _value) {
        this._tag = _tag;
        this._value = _value;
    }

    public int getTag() {
        return this._tag;
    }

    public static PortRole powerRole(byte _value) {
        return new PortRole(0, Byte.valueOf(_value));
    }

    public byte getPowerRole() {
        _assertTag(0);
        return ((Byte) this._value).byteValue();
    }

    public void setPowerRole(byte _value) {
        _set(0, Byte.valueOf(_value));
    }

    public static PortRole dataRole(byte _value) {
        return new PortRole(1, Byte.valueOf(_value));
    }

    public byte getDataRole() {
        _assertTag(1);
        return ((Byte) this._value).byteValue();
    }

    public void setDataRole(byte _value) {
        _set(1, Byte.valueOf(_value));
    }

    public static PortRole mode(byte _value) {
        return new PortRole(2, Byte.valueOf(_value));
    }

    public byte getMode() {
        _assertTag(2);
        return ((Byte) this._value).byteValue();
    }

    public void setMode(byte _value) {
        _set(2, Byte.valueOf(_value));
    }

    public final int getStability() {
        return 1;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        _aidl_parcel.writeInt(this._tag);
        switch (this._tag) {
            case 0:
                _aidl_parcel.writeByte(getPowerRole());
                return;
            case 1:
                _aidl_parcel.writeByte(getDataRole());
                return;
            case 2:
                _aidl_parcel.writeByte(getMode());
                return;
            default:
                return;
        }
    }

    public void readFromParcel(Parcel _aidl_parcel) {
        int _aidl_tag = _aidl_parcel.readInt();
        switch (_aidl_tag) {
            case 0:
                byte _aidl_value = _aidl_parcel.readByte();
                _set(_aidl_tag, Byte.valueOf(_aidl_value));
                return;
            case 1:
                byte _aidl_value2 = _aidl_parcel.readByte();
                _set(_aidl_tag, Byte.valueOf(_aidl_value2));
                return;
            case 2:
                byte _aidl_value3 = _aidl_parcel.readByte();
                _set(_aidl_tag, Byte.valueOf(_aidl_value3));
                return;
            default:
                throw new IllegalArgumentException("union: unknown tag: " + _aidl_tag);
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        getTag();
        return 0;
    }

    private void _assertTag(int tag) {
        if (getTag() != tag) {
            throw new IllegalStateException("bad access: " + _tagString(tag) + ", " + _tagString(getTag()) + " is available.");
        }
    }

    private String _tagString(int _tag) {
        switch (_tag) {
            case 0:
                return "powerRole";
            case 1:
                return "dataRole";
            case 2:
                return GameManagerService.GamePackageConfiguration.GameModeConfiguration.MODE_KEY;
            default:
                throw new IllegalStateException("unknown field: " + _tag);
        }
    }

    private void _set(int _tag, Object _value) {
        this._tag = _tag;
        this._value = _value;
    }
}
