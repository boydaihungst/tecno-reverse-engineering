package com.android.server.soundtrigger_middleware;

import android.hardware.soundtrigger3.ISoundTriggerHw;
import android.hardware.soundtrigger3.ISoundTriggerHwCallback;
import android.hardware.soundtrigger3.ISoundTriggerHwGlobalCallback;
import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseRecognitionEvent;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.Properties;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.RecognitionEvent;
import android.media.soundtrigger.SoundModel;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import com.android.server.soundtrigger_middleware.ISoundTriggerHal;
/* loaded from: classes2.dex */
public class SoundTriggerHw3Compat implements ISoundTriggerHal {
    private final ISoundTriggerHw mDriver;
    private final Runnable mRebootRunnable;

    public SoundTriggerHw3Compat(IBinder binder, Runnable rebootRunnable) {
        this.mDriver = ISoundTriggerHw.Stub.asInterface(binder);
        this.mRebootRunnable = rebootRunnable;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public Properties getProperties() {
        try {
            return this.mDriver.getProperties();
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void registerCallback(ISoundTriggerHal.GlobalCallback callback) {
        try {
            this.mDriver.registerGlobalCallback(new GlobalCallbackAdaper(callback));
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadSoundModel(SoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        try {
            return this.mDriver.loadSoundModel(soundModel, new ModelCallbackAdaper(callback));
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode == 1) {
                throw new RecoverableException(1);
            }
            throw e2;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadPhraseSoundModel(PhraseSoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        try {
            return this.mDriver.loadPhraseSoundModel(soundModel, new ModelCallbackAdaper(callback));
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode == 1) {
                throw new RecoverableException(1);
            }
            throw e2;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unloadSoundModel(int modelHandle) {
        try {
            this.mDriver.unloadSoundModel(modelHandle);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void startRecognition(int modelHandle, int deviceHandle, int ioHandle, RecognitionConfig config) {
        try {
            this.mDriver.startRecognition(modelHandle, deviceHandle, ioHandle, config);
        } catch (ServiceSpecificException e) {
            if (e.errorCode == 1) {
                throw new RecoverableException(1);
            }
            throw e;
        } catch (RemoteException e2) {
            throw e2.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void stopRecognition(int modelHandle) {
        try {
            this.mDriver.stopRecognition(modelHandle);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void forceRecognitionEvent(int modelHandle) {
        try {
            this.mDriver.forceRecognitionEvent(modelHandle);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public ModelParameterRange queryParameter(int modelHandle, int param) {
        try {
            return this.mDriver.queryParameter(modelHandle, param);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int getModelParameter(int modelHandle, int param) {
        try {
            return this.mDriver.getParameter(modelHandle, param);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void setModelParameter(int modelHandle, int param, int value) {
        try {
            this.mDriver.setParameter(modelHandle, param, value);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public String interfaceDescriptor() {
        try {
            return this.mDriver.asBinder().getInterfaceDescriptor();
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void linkToDeath(IBinder.DeathRecipient recipient) {
        try {
            this.mDriver.asBinder().linkToDeath(recipient, 0);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unlinkToDeath(IBinder.DeathRecipient recipient) {
        this.mDriver.asBinder().unlinkToDeath(recipient, 0);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void flushCallbacks() {
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void reboot() {
        this.mRebootRunnable.run();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void detach() {
    }

    /* loaded from: classes2.dex */
    private static class GlobalCallbackAdaper extends ISoundTriggerHwGlobalCallback.Stub {
        private final ISoundTriggerHal.GlobalCallback mDelegate;

        public GlobalCallbackAdaper(ISoundTriggerHal.GlobalCallback callback) {
            this.mDelegate = callback;
        }

        public void onResourcesAvailable() {
            this.mDelegate.onResourcesAvailable();
        }

        public int getInterfaceVersion() {
            return 1;
        }

        public String getInterfaceHash() {
            return "7d8d63478cd50e766d2072140c8aa3457f9fb585";
        }
    }

    /* loaded from: classes2.dex */
    private static class ModelCallbackAdaper extends ISoundTriggerHwCallback.Stub {
        private final ISoundTriggerHal.ModelCallback mDelegate;

        public ModelCallbackAdaper(ISoundTriggerHal.ModelCallback callback) {
            this.mDelegate = callback;
        }

        public void modelUnloaded(int model) {
            this.mDelegate.modelUnloaded(model);
        }

        public void phraseRecognitionCallback(int model, PhraseRecognitionEvent event) {
            event.common.recognitionStillActive |= event.common.status == 3;
            this.mDelegate.phraseRecognitionCallback(model, event);
        }

        public void recognitionCallback(int model, RecognitionEvent event) {
            event.recognitionStillActive |= event.status == 3;
            this.mDelegate.recognitionCallback(model, event);
        }

        public int getInterfaceVersion() {
            return 1;
        }

        public String getInterfaceHash() {
            return "7d8d63478cd50e766d2072140c8aa3457f9fb585";
        }
    }
}
