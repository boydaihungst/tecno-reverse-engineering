package android.hardware.audio.common.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;
/* loaded from: classes.dex */
public final class AudioOffloadInfo {
    public int sampleRateHz = 0;
    public int channelMask = 0;
    public int format = 0;
    public int streamType = 0;
    public int bitRatePerSecond = 0;
    public long durationMicroseconds = 0;
    public boolean hasVideo = false;
    public boolean isStreaming = false;
    public int bitWidth = 0;
    public int bufferSize = 0;
    public int usage = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != AudioOffloadInfo.class) {
            return false;
        }
        AudioOffloadInfo other = (AudioOffloadInfo) otherObject;
        if (this.sampleRateHz == other.sampleRateHz && this.channelMask == other.channelMask && this.format == other.format && this.streamType == other.streamType && this.bitRatePerSecond == other.bitRatePerSecond && this.durationMicroseconds == other.durationMicroseconds && this.hasVideo == other.hasVideo && this.isStreaming == other.isStreaming && this.bitWidth == other.bitWidth && this.bufferSize == other.bufferSize && this.usage == other.usage) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.sampleRateHz))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.channelMask))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.format))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.streamType))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bitRatePerSecond))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.durationMicroseconds))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.hasVideo))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isStreaming))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bitWidth))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bufferSize))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.usage))));
    }

    public final String toString() {
        return "{.sampleRateHz = " + this.sampleRateHz + ", .channelMask = " + AudioChannelMask.toString(this.channelMask) + ", .format = " + AudioFormat.toString(this.format) + ", .streamType = " + AudioStreamType.toString(this.streamType) + ", .bitRatePerSecond = " + this.bitRatePerSecond + ", .durationMicroseconds = " + this.durationMicroseconds + ", .hasVideo = " + this.hasVideo + ", .isStreaming = " + this.isStreaming + ", .bitWidth = " + this.bitWidth + ", .bufferSize = " + this.bufferSize + ", .usage = " + AudioUsage.toString(this.usage) + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(48L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<AudioOffloadInfo> readVectorFromParcel(HwParcel parcel) {
        ArrayList<AudioOffloadInfo> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 48, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            AudioOffloadInfo _hidl_vec_element = new AudioOffloadInfo();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 48);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.sampleRateHz = _hidl_blob.getInt32(0 + _hidl_offset);
        this.channelMask = _hidl_blob.getInt32(4 + _hidl_offset);
        this.format = _hidl_blob.getInt32(8 + _hidl_offset);
        this.streamType = _hidl_blob.getInt32(12 + _hidl_offset);
        this.bitRatePerSecond = _hidl_blob.getInt32(16 + _hidl_offset);
        this.durationMicroseconds = _hidl_blob.getInt64(24 + _hidl_offset);
        this.hasVideo = _hidl_blob.getBool(32 + _hidl_offset);
        this.isStreaming = _hidl_blob.getBool(33 + _hidl_offset);
        this.bitWidth = _hidl_blob.getInt32(36 + _hidl_offset);
        this.bufferSize = _hidl_blob.getInt32(40 + _hidl_offset);
        this.usage = _hidl_blob.getInt32(44 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(48);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<AudioOffloadInfo> _hidl_vec) {
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
        _hidl_blob.putInt32(0 + _hidl_offset, this.sampleRateHz);
        _hidl_blob.putInt32(4 + _hidl_offset, this.channelMask);
        _hidl_blob.putInt32(8 + _hidl_offset, this.format);
        _hidl_blob.putInt32(12 + _hidl_offset, this.streamType);
        _hidl_blob.putInt32(16 + _hidl_offset, this.bitRatePerSecond);
        _hidl_blob.putInt64(24 + _hidl_offset, this.durationMicroseconds);
        _hidl_blob.putBool(32 + _hidl_offset, this.hasVideo);
        _hidl_blob.putBool(33 + _hidl_offset, this.isStreaming);
        _hidl_blob.putInt32(36 + _hidl_offset, this.bitWidth);
        _hidl_blob.putInt32(40 + _hidl_offset, this.bufferSize);
        _hidl_blob.putInt32(44 + _hidl_offset, this.usage);
    }
}
