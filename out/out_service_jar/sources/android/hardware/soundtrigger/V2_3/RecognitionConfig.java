package android.hardware.soundtrigger.V2_3;

import android.hardware.soundtrigger.V2_1.ISoundTriggerHw;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
/* loaded from: classes.dex */
public final class RecognitionConfig {
    public int audioCapabilities;
    public ISoundTriggerHw.RecognitionConfig base = new ISoundTriggerHw.RecognitionConfig();

    public final String toString() {
        return "{.base = " + this.base + ", .audioCapabilities = " + AudioCapabilities.dumpBitfield(this.audioCapabilities) + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(96L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<RecognitionConfig> readVectorFromParcel(HwParcel parcel) {
        ArrayList<RecognitionConfig> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 96, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            RecognitionConfig _hidl_vec_element = new RecognitionConfig();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 96);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.base.readEmbeddedFromParcel(parcel, _hidl_blob, 0 + _hidl_offset);
        this.audioCapabilities = _hidl_blob.getInt32(88 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(96);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<RecognitionConfig> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 96);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 96);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        this.base.writeEmbeddedToBlob(_hidl_blob, 0 + _hidl_offset);
        _hidl_blob.putInt32(88 + _hidl_offset, this.audioCapabilities);
    }
}
