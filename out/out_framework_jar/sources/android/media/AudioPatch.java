package android.media;

import android.os.Parcel;
import android.os.Parcelable;
/* loaded from: classes2.dex */
public class AudioPatch {
    private final AudioHandle mHandle;
    private final AudioPortConfig[] mSinks;
    private final AudioPortConfig[] mSources;

    /* renamed from: android.media.AudioPatch$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass1 implements Parcelable.Creator<AudioPatch> {
        AnonymousClass1() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AudioPatch createFromParcel(Parcel _aidl_source) {
            AudioPatch _aidl_out = new AudioPatch();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AudioPatch[] newArray(int _aidl_size) {
            return new AudioPatch[_aidl_size];
        }
    }

    AudioPatch(AudioHandle patchHandle, AudioPortConfig[] sources, AudioPortConfig[] sinks) {
        this.mHandle = patchHandle;
        this.mSources = sources;
        this.mSinks = sinks;
    }

    public AudioPortConfig[] sources() {
        return this.mSources;
    }

    public AudioPortConfig[] sinks() {
        return this.mSinks;
    }

    public int id() {
        return this.mHandle.id();
    }

    public String toString() {
        AudioPortConfig[] audioPortConfigArr;
        AudioPortConfig[] audioPortConfigArr2;
        StringBuilder s = new StringBuilder();
        s.append("mHandle: ");
        s.append(this.mHandle.toString());
        s.append(" mSources: {");
        for (AudioPortConfig source : this.mSources) {
            s.append(source.toString());
            s.append(", ");
        }
        s.append("} mSinks: {");
        for (AudioPortConfig sink : this.mSinks) {
            s.append(sink.toString());
            s.append(", ");
        }
        s.append("}");
        return s.toString();
    }
}
