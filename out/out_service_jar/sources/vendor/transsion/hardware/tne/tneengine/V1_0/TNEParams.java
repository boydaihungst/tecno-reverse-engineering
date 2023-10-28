package vendor.transsion.hardware.tne.tneengine.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class TNEParams {
    public String tag = new String();
    public long type = 0;
    public int pid = 0;
    public String filepath = new String();
    public String info = new String();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != TNEParams.class) {
            return false;
        }
        TNEParams other = (TNEParams) otherObject;
        if (HidlSupport.deepEquals(this.tag, other.tag) && this.type == other.type && this.pid == other.pid && HidlSupport.deepEquals(this.filepath, other.filepath) && HidlSupport.deepEquals(this.info, other.info)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.tag)), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.pid))), Integer.valueOf(HidlSupport.deepHashCode(this.filepath)), Integer.valueOf(HidlSupport.deepHashCode(this.info)));
    }

    public final String toString() {
        return "{.tag = " + this.tag + ", .type = " + this.type + ", .pid = " + this.pid + ", .filepath = " + this.filepath + ", .info = " + this.info + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(64L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<TNEParams> readVectorFromParcel(HwParcel parcel) {
        ArrayList<TNEParams> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 64, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            TNEParams _hidl_vec_element = new TNEParams();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 64);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        String string = _hidl_blob.getString(_hidl_offset + 0);
        this.tag = string;
        parcel.readEmbeddedBuffer(string.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 0 + 0, false);
        this.type = _hidl_blob.getInt64(_hidl_offset + 16);
        this.pid = _hidl_blob.getInt32(_hidl_offset + 24);
        String string2 = _hidl_blob.getString(_hidl_offset + 32);
        this.filepath = string2;
        parcel.readEmbeddedBuffer(string2.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 32 + 0, false);
        String string3 = _hidl_blob.getString(_hidl_offset + 48);
        this.info = string3;
        parcel.readEmbeddedBuffer(string3.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 48 + 0, false);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(64);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<TNEParams> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 64);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 64);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putString(0 + _hidl_offset, this.tag);
        _hidl_blob.putInt64(16 + _hidl_offset, this.type);
        _hidl_blob.putInt32(24 + _hidl_offset, this.pid);
        _hidl_blob.putString(32 + _hidl_offset, this.filepath);
        _hidl_blob.putString(48 + _hidl_offset, this.info);
    }
}
