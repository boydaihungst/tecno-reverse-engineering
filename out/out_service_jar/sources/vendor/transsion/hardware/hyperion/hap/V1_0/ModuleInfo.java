package vendor.transsion.hardware.hyperion.hap.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class ModuleInfo {
    public String name = new String();
    public String ver = new String();
    public byte level = 0;
    public ArrayList<Byte> data = new ArrayList<>();
    public String sig = new String();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != ModuleInfo.class) {
            return false;
        }
        ModuleInfo other = (ModuleInfo) otherObject;
        if (HidlSupport.deepEquals(this.name, other.name) && HidlSupport.deepEquals(this.ver, other.ver) && this.level == other.level && HidlSupport.deepEquals(this.data, other.data) && HidlSupport.deepEquals(this.sig, other.sig)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.name)), Integer.valueOf(HidlSupport.deepHashCode(this.ver)), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.level))), Integer.valueOf(HidlSupport.deepHashCode(this.data)), Integer.valueOf(HidlSupport.deepHashCode(this.sig)));
    }

    public final String toString() {
        return "{.name = " + this.name + ", .ver = " + this.ver + ", .level = " + ((int) this.level) + ", .data = " + this.data + ", .sig = " + this.sig + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(72L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<ModuleInfo> readVectorFromParcel(HwParcel parcel) {
        ArrayList<ModuleInfo> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 72, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ModuleInfo _hidl_vec_element = new ModuleInfo();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 72);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        String string = _hidl_blob.getString(_hidl_offset + 0);
        this.name = string;
        parcel.readEmbeddedBuffer(string.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 0 + 0, false);
        String string2 = _hidl_blob.getString(_hidl_offset + 16);
        this.ver = string2;
        parcel.readEmbeddedBuffer(string2.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 16 + 0, false);
        this.level = _hidl_blob.getInt8(_hidl_offset + 32);
        int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 40 + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 1, _hidl_blob.handle(), _hidl_offset + 40 + 0, true);
        this.data.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            byte _hidl_vec_element = childBlob.getInt8(_hidl_index_0 * 1);
            this.data.add(Byte.valueOf(_hidl_vec_element));
        }
        String string3 = _hidl_blob.getString(_hidl_offset + 56);
        this.sig = string3;
        parcel.readEmbeddedBuffer(string3.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 56 + 0, false);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(72);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<ModuleInfo> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 72);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 72);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putString(_hidl_offset + 0, this.name);
        _hidl_blob.putString(16 + _hidl_offset, this.ver);
        _hidl_blob.putInt8(32 + _hidl_offset, this.level);
        int _hidl_vec_size = this.data.size();
        _hidl_blob.putInt32(_hidl_offset + 40 + 8, _hidl_vec_size);
        _hidl_blob.putBool(_hidl_offset + 40 + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 1);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt8(_hidl_index_0 * 1, this.data.get(_hidl_index_0).byteValue());
        }
        _hidl_blob.putBlob(40 + _hidl_offset + 0, childBlob);
        _hidl_blob.putString(56 + _hidl_offset, this.sig);
    }
}
