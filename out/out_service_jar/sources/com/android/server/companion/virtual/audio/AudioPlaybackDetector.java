package com.android.server.companion.virtual.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import java.util.List;
/* loaded from: classes.dex */
final class AudioPlaybackDetector extends AudioManager.AudioPlaybackCallback {
    private final AudioManager mAudioManager;
    private AudioPlaybackCallback mAudioPlaybackCallback;

    /* loaded from: classes.dex */
    interface AudioPlaybackCallback {
        void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> list);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioPlaybackDetector(Context context) {
        this.mAudioManager = (AudioManager) context.getSystemService(AudioManager.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void register(AudioPlaybackCallback callback) {
        this.mAudioPlaybackCallback = callback;
        this.mAudioManager.registerAudioPlaybackCallback(this, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregister() {
        if (this.mAudioPlaybackCallback != null) {
            this.mAudioPlaybackCallback = null;
            this.mAudioManager.unregisterAudioPlaybackCallback(this);
        }
    }

    @Override // android.media.AudioManager.AudioPlaybackCallback
    public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
        super.onPlaybackConfigChanged(configs);
        AudioPlaybackCallback audioPlaybackCallback = this.mAudioPlaybackCallback;
        if (audioPlaybackCallback != null) {
            audioPlaybackCallback.onPlaybackConfigChanged(configs);
        }
    }
}
