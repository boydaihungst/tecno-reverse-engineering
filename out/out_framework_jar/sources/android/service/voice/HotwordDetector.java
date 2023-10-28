package android.service.voice;

import android.annotation.SystemApi;
import android.graphics.FontListParser;
import android.media.AudioFormat;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.SharedMemory;
import android.service.voice.AlwaysOnHotwordDetector;
@SystemApi
/* loaded from: classes3.dex */
public interface HotwordDetector {
    public static final int DETECTOR_TYPE_NORMAL = 0;
    public static final int DETECTOR_TYPE_TRUSTED_HOTWORD_DSP = 1;
    public static final int DETECTOR_TYPE_TRUSTED_HOTWORD_SOFTWARE = 2;

    /* loaded from: classes3.dex */
    public interface Callback {
        void onDetected(AlwaysOnHotwordDetector.EventPayload eventPayload);

        void onError();

        void onHotwordDetectionServiceInitialized(int i);

        void onHotwordDetectionServiceRestarted();

        void onRecognitionPaused();

        void onRecognitionResumed();

        void onRejected(HotwordRejectedResult hotwordRejectedResult);
    }

    boolean startRecognition();

    boolean startRecognition(ParcelFileDescriptor parcelFileDescriptor, AudioFormat audioFormat, PersistableBundle persistableBundle);

    boolean stopRecognition();

    void updateState(PersistableBundle persistableBundle, SharedMemory sharedMemory);

    default void destroy() {
        throw new UnsupportedOperationException("Not implemented. Must override in a subclass.");
    }

    static String detectorTypeToString(int detectorType) {
        switch (detectorType) {
            case 0:
                return FontListParser.STYLE_NORMAL;
            case 1:
                return "trusted_hotword_dsp";
            case 2:
                return "trusted_hotword_software";
            default:
                return Integer.toString(detectorType);
        }
    }
}
