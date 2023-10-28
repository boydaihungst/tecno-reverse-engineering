package com.android.server.soundtrigger_middleware;

import android.media.soundtrigger.PhraseRecognitionEvent;
import android.media.soundtrigger.PhraseRecognitionExtra;
import android.media.soundtrigger.RecognitionEvent;
/* loaded from: classes2.dex */
public class AidlUtil {
    static RecognitionEvent newEmptyRecognitionEvent() {
        RecognitionEvent result = new RecognitionEvent();
        result.data = new byte[0];
        return result;
    }

    static PhraseRecognitionEvent newEmptyPhraseRecognitionEvent() {
        PhraseRecognitionEvent result = new PhraseRecognitionEvent();
        result.common = newEmptyRecognitionEvent();
        result.phraseExtras = new PhraseRecognitionExtra[0];
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static RecognitionEvent newAbortEvent() {
        RecognitionEvent event = newEmptyRecognitionEvent();
        event.type = 1;
        event.status = 1;
        return event;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static PhraseRecognitionEvent newAbortPhraseEvent() {
        PhraseRecognitionEvent event = newEmptyPhraseRecognitionEvent();
        event.common.type = 0;
        event.common.status = 1;
        return event;
    }
}
