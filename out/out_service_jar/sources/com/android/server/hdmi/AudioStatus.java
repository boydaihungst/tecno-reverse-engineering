package com.android.server.hdmi;

import java.util.Objects;
/* loaded from: classes.dex */
public class AudioStatus {
    public static final int MAX_VOLUME = 100;
    public static final int MIN_VOLUME = 0;
    boolean mMute;
    int mVolume;

    public AudioStatus(int volume, boolean mute) {
        this.mVolume = volume;
        this.mMute = mute;
    }

    public int getVolume() {
        return this.mVolume;
    }

    public boolean getMute() {
        return this.mMute;
    }

    public boolean equals(Object obj) {
        if (obj instanceof AudioStatus) {
            AudioStatus other = (AudioStatus) obj;
            return this.mVolume == other.mVolume && this.mMute == other.mMute;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mVolume), Boolean.valueOf(this.mMute));
    }

    public String toString() {
        return "AudioStatus mVolume:" + this.mVolume + " mMute:" + this.mMute;
    }
}
