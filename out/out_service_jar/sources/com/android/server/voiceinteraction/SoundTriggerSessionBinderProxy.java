package com.android.server.voiceinteraction;

import android.hardware.soundtrigger.SoundTrigger;
import android.os.RemoteException;
import com.android.internal.app.IHotwordRecognitionStatusCallback;
import com.android.internal.app.IVoiceInteractionSoundTriggerSession;
/* loaded from: classes2.dex */
final class SoundTriggerSessionBinderProxy extends IVoiceInteractionSoundTriggerSession.Stub {
    private final IVoiceInteractionSoundTriggerSession mDelegate;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SoundTriggerSessionBinderProxy(IVoiceInteractionSoundTriggerSession delegate) {
        this.mDelegate = delegate;
    }

    public SoundTrigger.ModuleProperties getDspModuleProperties() throws RemoteException {
        return this.mDelegate.getDspModuleProperties();
    }

    public int startRecognition(int i, String s, IHotwordRecognitionStatusCallback iHotwordRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig, boolean b) throws RemoteException {
        return this.mDelegate.startRecognition(i, s, iHotwordRecognitionStatusCallback, recognitionConfig, b);
    }

    public int stopRecognition(int i, IHotwordRecognitionStatusCallback iHotwordRecognitionStatusCallback) throws RemoteException {
        return this.mDelegate.stopRecognition(i, iHotwordRecognitionStatusCallback);
    }

    public int setParameter(int i, int i1, int i2) throws RemoteException {
        return this.mDelegate.setParameter(i, i1, i2);
    }

    public int getParameter(int i, int i1) throws RemoteException {
        return this.mDelegate.getParameter(i, i1);
    }

    public SoundTrigger.ModelParamRange queryParameter(int i, int i1) throws RemoteException {
        return this.mDelegate.queryParameter(i, i1);
    }
}
