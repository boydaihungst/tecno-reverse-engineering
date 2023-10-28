package android.hardware.usb.V1_1;

import android.hardware.usb.V1_0.PortStatus;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;
/* loaded from: classes.dex */
public final class PortStatus_1_1 {
    public int supportedModes;
    public PortStatus status = new PortStatus();
    public int currentMode = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != PortStatus_1_1.class) {
            return false;
        }
        PortStatus_1_1 other = (PortStatus_1_1) otherObject;
        if (HidlSupport.deepEquals(this.status, other.status) && HidlSupport.deepEquals(Integer.valueOf(this.supportedModes), Integer.valueOf(other.supportedModes)) && this.currentMode == other.currentMode) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.status)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.supportedModes))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.currentMode))));
    }

    public final String toString() {
        return "{.status = " + this.status + ", .supportedModes = " + PortMode_1_1.dumpBitfield(this.supportedModes) + ", .currentMode = " + PortMode_1_1.toString(this.currentMode) + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(48L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<PortStatus_1_1> readVectorFromParcel(HwParcel parcel) {
        ArrayList<PortStatus_1_1> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 48, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            PortStatus_1_1 _hidl_vec_element = new PortStatus_1_1();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 48);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.status.readEmbeddedFromParcel(parcel, _hidl_blob, 0 + _hidl_offset);
        this.supportedModes = _hidl_blob.getInt32(40 + _hidl_offset);
        this.currentMode = _hidl_blob.getInt32(44 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(48);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<PortStatus_1_1> _hidl_vec) {
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
        this.status.writeEmbeddedToBlob(_hidl_blob, 0 + _hidl_offset);
        _hidl_blob.putInt32(40 + _hidl_offset, this.supportedModes);
        _hidl_blob.putInt32(44 + _hidl_offset, this.currentMode);
    }
}
