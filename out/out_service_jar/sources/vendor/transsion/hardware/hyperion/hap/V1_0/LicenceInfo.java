package vendor.transsion.hardware.hyperion.hap.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;
/* loaded from: classes2.dex */
public final class LicenceInfo {
    public String customerName = new String();
    public String appId = new String();
    public int issuedTime = 0;
    public int notBefore = 0;
    public int notAfter = 0;
    public boolean persistOn = false;
    public boolean clearSfs = false;
    public boolean clearPersist = false;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != LicenceInfo.class) {
            return false;
        }
        LicenceInfo other = (LicenceInfo) otherObject;
        if (HidlSupport.deepEquals(this.customerName, other.customerName) && HidlSupport.deepEquals(this.appId, other.appId) && this.issuedTime == other.issuedTime && this.notBefore == other.notBefore && this.notAfter == other.notAfter && this.persistOn == other.persistOn && this.clearSfs == other.clearSfs && this.clearPersist == other.clearPersist) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.customerName)), Integer.valueOf(HidlSupport.deepHashCode(this.appId)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.issuedTime))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.notBefore))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.notAfter))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.persistOn))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.clearSfs))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.clearPersist))));
    }

    public final String toString() {
        return "{.customerName = " + this.customerName + ", .appId = " + this.appId + ", .issuedTime = " + this.issuedTime + ", .notBefore = " + this.notBefore + ", .notAfter = " + this.notAfter + ", .persistOn = " + this.persistOn + ", .clearSfs = " + this.clearSfs + ", .clearPersist = " + this.clearPersist + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(48L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<LicenceInfo> readVectorFromParcel(HwParcel parcel) {
        ArrayList<LicenceInfo> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 48, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            LicenceInfo _hidl_vec_element = new LicenceInfo();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 48);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        String string = _hidl_blob.getString(_hidl_offset + 0);
        this.customerName = string;
        parcel.readEmbeddedBuffer(string.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 0 + 0, false);
        String string2 = _hidl_blob.getString(_hidl_offset + 16);
        this.appId = string2;
        parcel.readEmbeddedBuffer(string2.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 16 + 0, false);
        this.issuedTime = _hidl_blob.getInt32(_hidl_offset + 32);
        this.notBefore = _hidl_blob.getInt32(_hidl_offset + 36);
        this.notAfter = _hidl_blob.getInt32(_hidl_offset + 40);
        this.persistOn = _hidl_blob.getBool(_hidl_offset + 44);
        this.clearSfs = _hidl_blob.getBool(_hidl_offset + 45);
        this.clearPersist = _hidl_blob.getBool(_hidl_offset + 46);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(48);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<LicenceInfo> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 48);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 48);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putString(0 + _hidl_offset, this.customerName);
        _hidl_blob.putString(16 + _hidl_offset, this.appId);
        _hidl_blob.putInt32(32 + _hidl_offset, this.issuedTime);
        _hidl_blob.putInt32(36 + _hidl_offset, this.notBefore);
        _hidl_blob.putInt32(40 + _hidl_offset, this.notAfter);
        _hidl_blob.putBool(44 + _hidl_offset, this.persistOn);
        _hidl_blob.putBool(45 + _hidl_offset, this.clearSfs);
        _hidl_blob.putBool(46 + _hidl_offset, this.clearPersist);
    }
}
