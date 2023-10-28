package com.android.server.soundtrigger_middleware;

import android.hardware.soundtrigger.V2_0.ISoundTriggerHw;
import android.hardware.soundtrigger.V2_0.ISoundTriggerHwCallback;
import android.hardware.soundtrigger.V2_1.ISoundTriggerHw;
import android.hardware.soundtrigger.V2_1.ISoundTriggerHwCallback;
import android.hardware.soundtrigger.V2_3.Properties;
import android.hardware.soundtrigger.V2_3.RecognitionConfig;
import android.os.HidlMemoryUtil;
import java.util.ArrayList;
/* loaded from: classes2.dex */
class Hw2CompatUtil {
    Hw2CompatUtil() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ISoundTriggerHw.SoundModel convertSoundModel_2_1_to_2_0(ISoundTriggerHw.SoundModel soundModel) {
        ISoundTriggerHw.SoundModel model_2_0 = soundModel.header;
        model_2_0.data = HidlMemoryUtil.hidlMemoryToByteList(soundModel.data);
        return model_2_0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ISoundTriggerHwCallback.RecognitionEvent convertRecognitionEvent_2_0_to_2_1(ISoundTriggerHwCallback.RecognitionEvent event) {
        ISoundTriggerHwCallback.RecognitionEvent event_2_1 = new ISoundTriggerHwCallback.RecognitionEvent();
        event_2_1.header = event;
        event_2_1.data = HidlMemoryUtil.byteListToHidlMemory(event_2_1.header.data, "SoundTrigger RecognitionEvent");
        event_2_1.header.data = new ArrayList<>();
        return event_2_1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ISoundTriggerHwCallback.PhraseRecognitionEvent convertPhraseRecognitionEvent_2_0_to_2_1(ISoundTriggerHwCallback.PhraseRecognitionEvent event) {
        ISoundTriggerHwCallback.PhraseRecognitionEvent event_2_1 = new ISoundTriggerHwCallback.PhraseRecognitionEvent();
        event_2_1.common = convertRecognitionEvent_2_0_to_2_1(event.common);
        event_2_1.phraseExtras = event.phraseExtras;
        return event_2_1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ISoundTriggerHw.PhraseSoundModel convertPhraseSoundModel_2_1_to_2_0(ISoundTriggerHw.PhraseSoundModel soundModel) {
        ISoundTriggerHw.PhraseSoundModel model_2_0 = new ISoundTriggerHw.PhraseSoundModel();
        model_2_0.common = convertSoundModel_2_1_to_2_0(soundModel.common);
        model_2_0.phrases = soundModel.phrases;
        return model_2_0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ISoundTriggerHw.RecognitionConfig convertRecognitionConfig_2_3_to_2_1(RecognitionConfig config) {
        return config.base;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ISoundTriggerHw.RecognitionConfig convertRecognitionConfig_2_3_to_2_0(RecognitionConfig config) {
        ISoundTriggerHw.RecognitionConfig config_2_0 = config.base.header;
        config_2_0.data = HidlMemoryUtil.hidlMemoryToByteList(config.base.data);
        return config_2_0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Properties convertProperties_2_0_to_2_3(ISoundTriggerHw.Properties properties) {
        Properties properties_2_3 = new Properties();
        properties_2_3.base = properties;
        return properties_2_3;
    }
}
