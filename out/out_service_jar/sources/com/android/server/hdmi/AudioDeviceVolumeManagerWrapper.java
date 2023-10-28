package com.android.server.hdmi;

import android.content.Context;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceVolumeManager;
import android.media.VolumeInfo;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public class AudioDeviceVolumeManagerWrapper implements AudioDeviceVolumeManagerWrapperInterface {
    private static final String TAG = "AudioDeviceVolumeManagerWrapper";
    private final AudioDeviceVolumeManager mAudioDeviceVolumeManager;

    public AudioDeviceVolumeManagerWrapper(Context context) {
        this.mAudioDeviceVolumeManager = new AudioDeviceVolumeManager(context);
    }

    @Override // com.android.server.hdmi.AudioDeviceVolumeManagerWrapperInterface
    public void addOnDeviceVolumeBehaviorChangedListener(Executor executor, AudioDeviceVolumeManager.OnDeviceVolumeBehaviorChangedListener listener) throws SecurityException {
        this.mAudioDeviceVolumeManager.addOnDeviceVolumeBehaviorChangedListener(executor, listener);
    }

    @Override // com.android.server.hdmi.AudioDeviceVolumeManagerWrapperInterface
    public void removeOnDeviceVolumeBehaviorChangedListener(AudioDeviceVolumeManager.OnDeviceVolumeBehaviorChangedListener listener) {
        this.mAudioDeviceVolumeManager.removeOnDeviceVolumeBehaviorChangedListener(listener);
    }

    @Override // com.android.server.hdmi.AudioDeviceVolumeManagerWrapperInterface
    public void setDeviceAbsoluteVolumeBehavior(AudioDeviceAttributes device, VolumeInfo volume, Executor executor, AudioDeviceVolumeManager.OnAudioDeviceVolumeChangedListener vclistener, boolean handlesVolumeAdjustment) {
        this.mAudioDeviceVolumeManager.setDeviceAbsoluteVolumeBehavior(device, volume, executor, vclistener, handlesVolumeAdjustment);
    }
}
