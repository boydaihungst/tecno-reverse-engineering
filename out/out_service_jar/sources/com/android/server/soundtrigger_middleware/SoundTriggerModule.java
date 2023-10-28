package com.android.server.soundtrigger_middleware;

import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseRecognitionEvent;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.Properties;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.RecognitionEvent;
import android.media.soundtrigger.SoundModel;
import android.media.soundtrigger_middleware.ISoundTriggerCallback;
import android.media.soundtrigger_middleware.ISoundTriggerModule;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.soundtrigger_middleware.ISoundTriggerHal;
import com.android.server.soundtrigger_middleware.SoundTriggerMiddlewareImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SoundTriggerModule implements IBinder.DeathRecipient, ISoundTriggerHal.GlobalCallback {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String TAG = "SoundTriggerModule";
    private final Set<Session> mActiveSessions = new HashSet();
    private final SoundTriggerMiddlewareImpl.AudioSessionProvider mAudioSessionProvider;
    private final HalFactory mHalFactory;
    private ISoundTriggerHal mHalService;
    private Properties mProperties;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public enum ModelState {
        INIT,
        LOADED,
        ACTIVE
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SoundTriggerModule(HalFactory halFactory, SoundTriggerMiddlewareImpl.AudioSessionProvider audioSessionProvider) {
        this.mHalFactory = halFactory;
        this.mAudioSessionProvider = audioSessionProvider;
        attachToHal();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized ISoundTriggerModule attach(ISoundTriggerCallback callback) {
        Session session;
        session = new Session(callback);
        this.mActiveSessions.add(session);
        return session;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized Properties getProperties() {
        return this.mProperties;
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        List<ISoundTriggerCallback> callbacks;
        Log.w(TAG, "Underlying HAL driver died.");
        synchronized (this) {
            callbacks = new ArrayList<>(this.mActiveSessions.size());
            for (Session session : this.mActiveSessions) {
                callbacks.add(session.moduleDied());
            }
            this.mActiveSessions.clear();
            reset();
        }
        for (ISoundTriggerCallback callback : callbacks) {
            try {
                callback.onModuleDied();
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }
    }

    private void reset() {
        this.mHalService.detach();
        attachToHal();
    }

    private void attachToHal() {
        SoundTriggerHalEnforcer soundTriggerHalEnforcer = new SoundTriggerHalEnforcer(new SoundTriggerHalWatchdog(this.mHalFactory.create()));
        this.mHalService = soundTriggerHalEnforcer;
        soundTriggerHalEnforcer.linkToDeath(this);
        this.mHalService.registerCallback(this);
        this.mProperties = this.mHalService.getProperties();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeSession(Session session) {
        this.mActiveSessions.remove(session);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.GlobalCallback
    public void onResourcesAvailable() {
        List<ISoundTriggerCallback> callbacks;
        synchronized (this) {
            callbacks = new ArrayList<>(this.mActiveSessions.size());
            for (Session session : this.mActiveSessions) {
                callbacks.add(session.mCallback);
            }
        }
        for (ISoundTriggerCallback callback : callbacks) {
            try {
                callback.onResourcesAvailable();
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class Session implements ISoundTriggerModule {
        private ISoundTriggerCallback mCallback;
        private final Map<Integer, Model> mLoadedModels;

        private Session(ISoundTriggerCallback callback) {
            this.mLoadedModels = new HashMap();
            this.mCallback = callback;
        }

        public void detach() {
            synchronized (SoundTriggerModule.this) {
                if (this.mCallback == null) {
                    return;
                }
                SoundTriggerModule.this.removeSession(this);
                this.mCallback = null;
            }
        }

        public int loadModel(SoundModel model) {
            int load;
            synchronized (SoundTriggerModule.this) {
                SoundTriggerMiddlewareImpl.AudioSessionProvider.AudioSession audioSession = SoundTriggerModule.this.mAudioSessionProvider.acquireSession();
                try {
                    checkValid();
                    Model loadedModel = new Model();
                    load = loadedModel.load(model, audioSession);
                } catch (Exception e) {
                    try {
                        SoundTriggerModule.this.mAudioSessionProvider.releaseSession(audioSession.mSessionHandle);
                    } catch (Exception ee) {
                        Log.e(SoundTriggerModule.TAG, "Failed to release session.", ee);
                    }
                    throw e;
                }
            }
            return load;
        }

        public int loadPhraseModel(PhraseSoundModel model) {
            int result;
            synchronized (SoundTriggerModule.this) {
                SoundTriggerMiddlewareImpl.AudioSessionProvider.AudioSession audioSession = SoundTriggerModule.this.mAudioSessionProvider.acquireSession();
                try {
                    checkValid();
                    Model loadedModel = new Model();
                    result = loadedModel.load(model, audioSession);
                    Log.d(SoundTriggerModule.TAG, String.format("loadPhraseModel()->%d", Integer.valueOf(result)));
                } catch (Exception e) {
                    try {
                        SoundTriggerModule.this.mAudioSessionProvider.releaseSession(audioSession.mSessionHandle);
                    } catch (Exception ee) {
                        Log.e(SoundTriggerModule.TAG, "Failed to release session.", ee);
                    }
                    throw e;
                }
            }
            return result;
        }

        public void unloadModel(int modelHandle) {
            synchronized (SoundTriggerModule.this) {
                checkValid();
                int sessionId = this.mLoadedModels.get(Integer.valueOf(modelHandle)).unload();
                SoundTriggerModule.this.mAudioSessionProvider.releaseSession(sessionId);
            }
        }

        public void startRecognition(int modelHandle, RecognitionConfig config) {
            synchronized (SoundTriggerModule.this) {
                checkValid();
                this.mLoadedModels.get(Integer.valueOf(modelHandle)).startRecognition(config);
            }
        }

        public void stopRecognition(int modelHandle) {
            Model model;
            synchronized (SoundTriggerModule.this) {
                checkValid();
                model = this.mLoadedModels.get(Integer.valueOf(modelHandle));
            }
            model.stopRecognition();
        }

        public void forceRecognitionEvent(int modelHandle) {
            synchronized (SoundTriggerModule.this) {
                checkValid();
                this.mLoadedModels.get(Integer.valueOf(modelHandle)).forceRecognitionEvent();
            }
        }

        public void setModelParameter(int modelHandle, int modelParam, int value) {
            synchronized (SoundTriggerModule.this) {
                checkValid();
                this.mLoadedModels.get(Integer.valueOf(modelHandle)).setParameter(modelParam, value);
            }
        }

        public int getModelParameter(int modelHandle, int modelParam) {
            int parameter;
            synchronized (SoundTriggerModule.this) {
                checkValid();
                parameter = this.mLoadedModels.get(Integer.valueOf(modelHandle)).getParameter(modelParam);
            }
            return parameter;
        }

        public ModelParameterRange queryModelParameterSupport(int modelHandle, int modelParam) {
            ModelParameterRange queryModelParameterSupport;
            synchronized (SoundTriggerModule.this) {
                checkValid();
                queryModelParameterSupport = this.mLoadedModels.get(Integer.valueOf(modelHandle)).queryModelParameterSupport(modelParam);
            }
            return queryModelParameterSupport;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public ISoundTriggerCallback moduleDied() {
            ISoundTriggerCallback callback = this.mCallback;
            this.mCallback = null;
            return callback;
        }

        private void checkValid() {
            if (this.mCallback == null) {
                throw new RecoverableException(4);
            }
        }

        public IBinder asBinder() {
            throw new UnsupportedOperationException("This implementation is not intended to be used directly with Binder.");
        }

        /* loaded from: classes2.dex */
        private class Model implements ISoundTriggerHal.ModelCallback {
            public int mHandle;
            private SoundTriggerMiddlewareImpl.AudioSessionProvider.AudioSession mSession;
            private ModelState mState;
            private int mType;

            private Model() {
                this.mState = ModelState.INIT;
                this.mType = -1;
            }

            private ModelState getState() {
                return this.mState;
            }

            private void setState(ModelState state) {
                this.mState = state;
                SoundTriggerModule.this.notifyAll();
            }

            /* JADX INFO: Access modifiers changed from: private */
            public int load(SoundModel model, SoundTriggerMiddlewareImpl.AudioSessionProvider.AudioSession audioSession) {
                this.mSession = audioSession;
                this.mHandle = SoundTriggerModule.this.mHalService.loadSoundModel(model, this);
                this.mType = 1;
                setState(ModelState.LOADED);
                Session.this.mLoadedModels.put(Integer.valueOf(this.mHandle), this);
                return this.mHandle;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public int load(PhraseSoundModel model, SoundTriggerMiddlewareImpl.AudioSessionProvider.AudioSession audioSession) {
                this.mSession = audioSession;
                this.mHandle = SoundTriggerModule.this.mHalService.loadPhraseSoundModel(model, this);
                this.mType = 0;
                setState(ModelState.LOADED);
                Session.this.mLoadedModels.put(Integer.valueOf(this.mHandle), this);
                return this.mHandle;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public int unload() {
                SoundTriggerModule.this.mHalService.unloadSoundModel(this.mHandle);
                Session.this.mLoadedModels.remove(Integer.valueOf(this.mHandle));
                return this.mSession.mSessionHandle;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void startRecognition(RecognitionConfig config) {
                SoundTriggerModule.this.mHalService.startRecognition(this.mHandle, this.mSession.mDeviceHandle, this.mSession.mIoHandle, config);
                setState(ModelState.ACTIVE);
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void stopRecognition() {
                synchronized (SoundTriggerModule.this) {
                    if (getState() == ModelState.LOADED) {
                        return;
                    }
                    SoundTriggerModule.this.mHalService.stopRecognition(this.mHandle);
                    synchronized (SoundTriggerModule.this) {
                        if (getState() == ModelState.ACTIVE) {
                            if (Session.this.mCallback != null) {
                                try {
                                    switch (this.mType) {
                                        case 0:
                                            Session.this.mCallback.onPhraseRecognition(this.mHandle, AidlUtil.newAbortPhraseEvent(), this.mSession.mSessionHandle);
                                            break;
                                        case 1:
                                            Session.this.mCallback.onRecognition(this.mHandle, AidlUtil.newAbortEvent(), this.mSession.mSessionHandle);
                                            break;
                                        default:
                                            throw new RuntimeException("Unexpected model type: " + this.mType);
                                    }
                                } catch (RemoteException e) {
                                }
                            }
                            setState(ModelState.LOADED);
                        }
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void forceRecognitionEvent() {
                if (getState() != ModelState.ACTIVE) {
                    return;
                }
                SoundTriggerModule.this.mHalService.forceRecognitionEvent(this.mHandle);
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void setParameter(int modelParam, int value) {
                SoundTriggerModule.this.mHalService.setModelParameter(this.mHandle, ConversionUtil.aidl2hidlModelParameter(modelParam), value);
            }

            /* JADX INFO: Access modifiers changed from: private */
            public int getParameter(int modelParam) {
                return SoundTriggerModule.this.mHalService.getModelParameter(this.mHandle, ConversionUtil.aidl2hidlModelParameter(modelParam));
            }

            /* JADX INFO: Access modifiers changed from: private */
            public ModelParameterRange queryModelParameterSupport(int modelParam) {
                return SoundTriggerModule.this.mHalService.queryParameter(this.mHandle, modelParam);
            }

            @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.ModelCallback
            public void recognitionCallback(int modelHandle, RecognitionEvent recognitionEvent) {
                ISoundTriggerCallback callback;
                synchronized (SoundTriggerModule.this) {
                    if (!recognitionEvent.recognitionStillActive) {
                        setState(ModelState.LOADED);
                    }
                    callback = Session.this.mCallback;
                }
                if (callback != null) {
                    try {
                        callback.onRecognition(this.mHandle, recognitionEvent, this.mSession.mSessionHandle);
                    } catch (RemoteException e) {
                        throw e.rethrowAsRuntimeException();
                    }
                }
            }

            @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.ModelCallback
            public void phraseRecognitionCallback(int modelHandle, PhraseRecognitionEvent phraseRecognitionEvent) {
                ISoundTriggerCallback callback;
                synchronized (SoundTriggerModule.this) {
                    int phraseRecognitionEventCommonDataLength = 0;
                    if (phraseRecognitionEvent.common.data != null) {
                        phraseRecognitionEventCommonDataLength = phraseRecognitionEvent.common.data.length;
                    }
                    if (!phraseRecognitionEvent.common.recognitionStillActive && (phraseRecognitionEventCommonDataLength == 0 || (phraseRecognitionEventCommonDataLength > 0 && phraseRecognitionEvent.common.data[0] != 1))) {
                        Log.i(SoundTriggerModule.TAG, "[phraseRecognitionCallback] ModelState.LOADED ");
                        setState(ModelState.LOADED);
                    }
                    callback = Session.this.mCallback;
                }
                if (callback != null) {
                    try {
                        Session.this.mCallback.onPhraseRecognition(this.mHandle, phraseRecognitionEvent, this.mSession.mSessionHandle);
                    } catch (RemoteException e) {
                        throw e.rethrowAsRuntimeException();
                    }
                }
            }

            @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.ModelCallback
            public void modelUnloaded(int modelHandle) {
                ISoundTriggerCallback callback;
                synchronized (SoundTriggerModule.this) {
                    callback = Session.this.mCallback;
                }
                if (callback != null) {
                    try {
                        callback.onModelUnloaded(modelHandle);
                    } catch (RemoteException e) {
                        throw e.rethrowAsRuntimeException();
                    }
                }
            }
        }
    }
}
