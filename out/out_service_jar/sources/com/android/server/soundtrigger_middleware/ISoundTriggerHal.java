package com.android.server.soundtrigger_middleware;

import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseRecognitionEvent;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.Properties;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.RecognitionEvent;
import android.media.soundtrigger.SoundModel;
import android.os.IBinder;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public interface ISoundTriggerHal {

    /* loaded from: classes2.dex */
    public interface GlobalCallback {
        void onResourcesAvailable();
    }

    /* loaded from: classes2.dex */
    public interface ModelCallback {
        void modelUnloaded(int i);

        void phraseRecognitionCallback(int i, PhraseRecognitionEvent phraseRecognitionEvent);

        void recognitionCallback(int i, RecognitionEvent recognitionEvent);
    }

    void detach();

    void flushCallbacks();

    void forceRecognitionEvent(int i);

    int getModelParameter(int i, int i2);

    Properties getProperties();

    String interfaceDescriptor();

    void linkToDeath(IBinder.DeathRecipient deathRecipient);

    int loadPhraseSoundModel(PhraseSoundModel phraseSoundModel, ModelCallback modelCallback);

    int loadSoundModel(SoundModel soundModel, ModelCallback modelCallback);

    ModelParameterRange queryParameter(int i, int i2);

    void reboot();

    void registerCallback(GlobalCallback globalCallback);

    void setModelParameter(int i, int i2, int i3);

    void startRecognition(int i, int i2, int i3, RecognitionConfig recognitionConfig);

    void stopRecognition(int i);

    void unlinkToDeath(IBinder.DeathRecipient deathRecipient);

    void unloadSoundModel(int i);
}
