package com.android.server.soundtrigger;

import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.os.IBinder;
import java.io.FileDescriptor;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public interface SoundTriggerInternal {
    public static final int STATUS_ERROR = Integer.MIN_VALUE;
    public static final int STATUS_OK = 0;

    /* loaded from: classes2.dex */
    public interface Session {
        void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

        SoundTrigger.ModuleProperties getModuleProperties();

        int getParameter(int i, int i2);

        SoundTrigger.ModelParamRange queryParameter(int i, int i2);

        int setParameter(int i, int i2, int i3);

        int startRecognition(int i, SoundTrigger.KeyphraseSoundModel keyphraseSoundModel, IRecognitionStatusCallback iRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig, boolean z);

        int stopRecognition(int i, IRecognitionStatusCallback iRecognitionStatusCallback);

        int unloadKeyphraseModel(int i);
    }

    Session attach(IBinder iBinder);

    void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);
}
