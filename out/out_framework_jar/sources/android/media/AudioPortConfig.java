package android.media;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes2.dex */
public class AudioPortConfig {
    static final int CHANNEL_MASK = 2;
    static final int FORMAT = 4;
    static final int GAIN = 8;
    static final int SAMPLE_RATE = 1;
    private final int mChannelMask;
    int mConfigMask = 0;
    private final int mFormat;
    private final AudioGainConfig mGain;
    final AudioPort mPort;
    private final int mSamplingRate;

    /* renamed from: android.media.AudioPortConfig$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass1 implements Parcelable.Creator<AudioPortConfig> {
        AnonymousClass1() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AudioPortConfig createFromParcel(Parcel _aidl_source) {
            AudioPortConfig _aidl_out = new AudioPortConfig();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AudioPortConfig[] newArray(int _aidl_size) {
            return new AudioPortConfig[_aidl_size];
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioPortConfig(AudioPort port, int samplingRate, int channelMask, int format, AudioGainConfig gain) {
        this.mPort = port;
        this.mSamplingRate = samplingRate;
        this.mChannelMask = channelMask;
        this.mFormat = format;
        this.mGain = gain;
    }

    public AudioPort port() {
        return this.mPort;
    }

    public int samplingRate() {
        return this.mSamplingRate;
    }

    public int channelMask() {
        return this.mChannelMask;
    }

    public int format() {
        return this.mFormat;
    }

    public AudioGainConfig gain() {
        return this.mGain;
    }

    public String toString() {
        return "{mPort:" + this.mPort + ", mSamplingRate:" + this.mSamplingRate + ", mChannelMask: " + this.mChannelMask + ", mFormat:" + this.mFormat + ", mGain:" + this.mGain + "}";
    }
}
