package android.hardware.audio.common.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;
/* loaded from: classes.dex */
public final class AudioConfig {
    public int sampleRateHz = 0;
    public int channelMask = 0;
    public int format = 0;
    public AudioOffloadInfo offloadInfo = new AudioOffloadInfo();
    public long frameCount = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != AudioConfig.class) {
            return false;
        }
        AudioConfig other = (AudioConfig) otherObject;
        if (this.sampleRateHz == other.sampleRateHz && this.channelMask == other.channelMask && this.format == other.format && HidlSupport.deepEquals(this.offloadInfo, other.offloadInfo) && this.frameCount == other.frameCount) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.sampleRateHz))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.channelMask))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.format))), Integer.valueOf(HidlSupport.deepHashCode(this.offloadInfo)), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.frameCount))));
    }

    public final String toString() {
        return "{.sampleRateHz = " + this.sampleRateHz + ", .channelMask = " + AudioChannelMask.toString(this.channelMask) + ", .format = " + AudioFormat.toString(this.format) + ", .offloadInfo = " + this.offloadInfo + ", .frameCount = " + this.frameCount + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(72L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<AudioConfig> readVectorFromParcel(HwParcel parcel) {
        ArrayList<AudioConfig> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 72, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            AudioConfig _hidl_vec_element = new AudioConfig();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 72);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.sampleRateHz = _hidl_blob.getInt32(0 + _hidl_offset);
        this.channelMask = _hidl_blob.getInt32(4 + _hidl_offset);
        this.format = _hidl_blob.getInt32(8 + _hidl_offset);
        this.offloadInfo.readEmbeddedFromParcel(parcel, _hidl_blob, 16 + _hidl_offset);
        this.frameCount = _hidl_blob.getInt64(64 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(72);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<AudioConfig> _hidl_vec) {
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
        _hidl_blob.putInt32(0 + _hidl_offset, this.sampleRateHz);
        _hidl_blob.putInt32(4 + _hidl_offset, this.channelMask);
        _hidl_blob.putInt32(8 + _hidl_offset, this.format);
        this.offloadInfo.writeEmbeddedToBlob(_hidl_blob, 16 + _hidl_offset);
        _hidl_blob.putInt64(64 + _hidl_offset, this.frameCount);
    }
}
