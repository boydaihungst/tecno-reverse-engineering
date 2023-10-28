package com.android.server.companion.virtual.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import java.util.List;
/* loaded from: classes.dex */
final class AudioRecordingDetector extends AudioManager.AudioRecordingCallback {
    private final AudioManager mAudioManager;
    private AudioRecordingCallback mAudioRecordingCallback;

    /* loaded from: classes.dex */
    interface AudioRecordingCallback {
        void onRecordingConfigChanged(List<AudioRecordingConfiguration> list);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioRecordingDetector(Context context) {
        this.mAudioManager = (AudioManager) context.getSystemService(AudioManager.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void register(AudioRecordingCallback callback) {
        this.mAudioRecordingCallback = callback;
        this.mAudioManager.registerAudioRecordingCallback(this, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregister() {
        if (this.mAudioRecordingCallback != null) {
            this.mAudioRecordingCallback = null;
            this.mAudioManager.unregisterAudioRecordingCallback(this);
        }
    }

    @Override // android.media.AudioManager.AudioRecordingCallback
    public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
        super.onRecordingConfigChanged(configs);
        AudioRecordingCallback audioRecordingCallback = this.mAudioRecordingCallback;
        if (audioRecordingCallback != null) {
            audioRecordingCallback.onRecordingConfigChanged(configs);
        }
    }
}
