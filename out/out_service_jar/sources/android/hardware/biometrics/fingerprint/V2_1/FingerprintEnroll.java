package android.hardware.biometrics.fingerprint.V2_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;
/* loaded from: classes.dex */
public final class FingerprintEnroll {
    public FingerprintFingerId finger = new FingerprintFingerId();
    public int samplesRemaining = 0;
    public long msg = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != FingerprintEnroll.class) {
            return false;
        }
        FingerprintEnroll other = (FingerprintEnroll) otherObject;
        if (HidlSupport.deepEquals(this.finger, other.finger) && this.samplesRemaining == other.samplesRemaining && this.msg == other.msg) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.finger)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.samplesRemaining))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.msg))));
    }

    public final String toString() {
        return "{.finger = " + this.finger + ", .samplesRemaining = " + this.samplesRemaining + ", .msg = " + this.msg + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(24L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<FingerprintEnroll> readVectorFromParcel(HwParcel parcel) {
        ArrayList<FingerprintEnroll> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 24, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            FingerprintEnroll _hidl_vec_element = new FingerprintEnroll();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 24);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.finger.readEmbeddedFromParcel(parcel, _hidl_blob, 0 + _hidl_offset);
        this.samplesRemaining = _hidl_blob.getInt32(8 + _hidl_offset);
        this.msg = _hidl_blob.getInt64(16 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(24);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<FingerprintEnroll> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 24);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 24);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        this.finger.writeEmbeddedToBlob(_hidl_blob, 0 + _hidl_offset);
        _hidl_blob.putInt32(8 + _hidl_offset, this.samplesRemaining);
        _hidl_blob.putInt64(16 + _hidl_offset, this.msg);
    }
}