package com.android.server.soundtrigger_middleware;

import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.Properties;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.SoundModel;
import android.os.IBinder;
import com.android.server.soundtrigger_middleware.ISoundTriggerHal;
/* loaded from: classes2.dex */
public class SoundTriggerHalMaxModelLimiter implements ISoundTriggerHal {
    private final ISoundTriggerHal mDelegate;
    private ISoundTriggerHal.GlobalCallback mGlobalCallback;
    private final int mMaxModels;
    private int mNumLoadedModels = 0;

    public SoundTriggerHalMaxModelLimiter(ISoundTriggerHal delegate, int maxModels) {
        this.mDelegate = delegate;
        this.mMaxModels = maxModels;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void reboot() {
        this.mDelegate.reboot();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void detach() {
        this.mDelegate.detach();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public Properties getProperties() {
        return this.mDelegate.getProperties();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void registerCallback(ISoundTriggerHal.GlobalCallback callback) {
        this.mGlobalCallback = callback;
        this.mDelegate.registerCallback(callback);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadSoundModel(SoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        int result;
        synchronized (this) {
            if (this.mNumLoadedModels == this.mMaxModels) {
                throw new RecoverableException(1);
            }
            result = this.mDelegate.loadSoundModel(soundModel, callback);
            this.mNumLoadedModels++;
        }
        return result;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadPhraseSoundModel(PhraseSoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        int result;
        synchronized (this) {
            if (this.mNumLoadedModels == this.mMaxModels) {
                throw new RecoverableException(1);
            }
            result = this.mDelegate.loadPhraseSoundModel(soundModel, callback);
            this.mNumLoadedModels++;
        }
        return result;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unloadSoundModel(int modelHandle) {
        boolean wasAtMaxCapacity;
        synchronized (this) {
            int i = this.mNumLoadedModels;
            this.mNumLoadedModels = i - 1;
            wasAtMaxCapacity = i == this.mMaxModels;
        }
        try {
            this.mDelegate.unloadSoundModel(modelHandle);
            if (wasAtMaxCapacity) {
                this.mGlobalCallback.onResourcesAvailable();
            }
        } catch (Exception e) {
            synchronized (this) {
                this.mNumLoadedModels++;
                throw e;
            }
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void stopRecognition(int modelHandle) {
        this.mDelegate.stopRecognition(modelHandle);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void startRecognition(int modelHandle, int deviceHandle, int ioHandle, RecognitionConfig config) {
        this.mDelegate.startRecognition(modelHandle, deviceHandle, ioHandle, config);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void forceRecognitionEvent(int modelHandle) {
        this.mDelegate.forceRecognitionEvent(modelHandle);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int getModelParameter(int modelHandle, int param) {
        return this.mDelegate.getModelParameter(modelHandle, param);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void setModelParameter(int modelHandle, int param, int value) {
        this.mDelegate.setModelParameter(modelHandle, param, value);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public ModelParameterRange queryParameter(int modelHandle, int param) {
        return this.mDelegate.queryParameter(modelHandle, param);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void linkToDeath(IBinder.DeathRecipient recipient) {
        this.mDelegate.linkToDeath(recipient);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unlinkToDeath(IBinder.DeathRecipient recipient) {
        this.mDelegate.unlinkToDeath(recipient);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public String interfaceDescriptor() {
        return this.mDelegate.interfaceDescriptor();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void flushCallbacks() {
        this.mDelegate.flushCallbacks();
    }
}
