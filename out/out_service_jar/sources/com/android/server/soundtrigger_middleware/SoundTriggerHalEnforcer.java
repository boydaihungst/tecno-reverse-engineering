package com.android.server.soundtrigger_middleware;

import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseRecognitionEvent;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.Properties;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.RecognitionEvent;
import android.media.soundtrigger.SoundModel;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.util.Log;
import com.android.server.soundtrigger_middleware.ISoundTriggerHal;
import java.util.HashMap;
import java.util.Map;
/* loaded from: classes2.dex */
public class SoundTriggerHalEnforcer implements ISoundTriggerHal {
    private static final String TAG = "SoundTriggerHalEnforcer";
    private final Map<Integer, ModelState> mModelStates = new HashMap();
    private final ISoundTriggerHal mUnderlying;

    /* loaded from: classes2.dex */
    private enum ModelState {
        INACTIVE,
        ACTIVE,
        PENDING_STOP
    }

    public SoundTriggerHalEnforcer(ISoundTriggerHal underlying) {
        this.mUnderlying = underlying;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public Properties getProperties() {
        try {
            return this.mUnderlying.getProperties();
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void registerCallback(ISoundTriggerHal.GlobalCallback callback) {
        try {
            this.mUnderlying.registerCallback(callback);
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadSoundModel(SoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        int handle;
        try {
            synchronized (this.mModelStates) {
                handle = this.mUnderlying.loadSoundModel(soundModel, new ModelCallbackEnforcer(callback));
                this.mModelStates.put(Integer.valueOf(handle), ModelState.INACTIVE);
            }
            return handle;
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadPhraseSoundModel(PhraseSoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        int handle;
        try {
            synchronized (this.mModelStates) {
                handle = this.mUnderlying.loadPhraseSoundModel(soundModel, new ModelCallbackEnforcer(callback));
                this.mModelStates.put(Integer.valueOf(handle), ModelState.INACTIVE);
            }
            return handle;
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unloadSoundModel(int modelHandle) {
        try {
            this.mUnderlying.unloadSoundModel(modelHandle);
            synchronized (this.mModelStates) {
                this.mModelStates.remove(Integer.valueOf(modelHandle));
            }
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void stopRecognition(int modelHandle) {
        try {
            synchronized (this.mModelStates) {
                this.mModelStates.replace(Integer.valueOf(modelHandle), ModelState.PENDING_STOP);
            }
            this.mUnderlying.stopRecognition(modelHandle);
            synchronized (this.mModelStates) {
                this.mModelStates.replace(Integer.valueOf(modelHandle), ModelState.INACTIVE);
            }
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void startRecognition(int modelHandle, int deviceHandle, int ioHandle, RecognitionConfig config) {
        try {
            synchronized (this.mModelStates) {
                this.mUnderlying.startRecognition(modelHandle, deviceHandle, ioHandle, config);
                this.mModelStates.replace(Integer.valueOf(modelHandle), ModelState.ACTIVE);
            }
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void forceRecognitionEvent(int modelHandle) {
        try {
            this.mUnderlying.forceRecognitionEvent(modelHandle);
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int getModelParameter(int modelHandle, int param) {
        try {
            return this.mUnderlying.getModelParameter(modelHandle, param);
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void setModelParameter(int modelHandle, int param, int value) {
        try {
            this.mUnderlying.setModelParameter(modelHandle, param, value);
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public ModelParameterRange queryParameter(int modelHandle, int param) {
        try {
            return this.mUnderlying.queryParameter(modelHandle, param);
        } catch (RuntimeException e) {
            throw handleException(e);
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void linkToDeath(IBinder.DeathRecipient recipient) {
        this.mUnderlying.linkToDeath(recipient);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unlinkToDeath(IBinder.DeathRecipient recipient) {
        this.mUnderlying.unlinkToDeath(recipient);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public String interfaceDescriptor() {
        return this.mUnderlying.interfaceDescriptor();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void flushCallbacks() {
        this.mUnderlying.flushCallbacks();
    }

    private RuntimeException handleException(RuntimeException e) {
        if (e instanceof RecoverableException) {
            throw e;
        }
        if (e.getCause() instanceof DeadObjectException) {
            Log.e(TAG, "HAL died");
            throw new RecoverableException(4);
        }
        Log.e(TAG, "Exception caught from HAL, rebooting HAL");
        reboot();
        throw e;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void reboot() {
        this.mUnderlying.reboot();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void detach() {
        this.mUnderlying.detach();
    }

    /* loaded from: classes2.dex */
    private class ModelCallbackEnforcer implements ISoundTriggerHal.ModelCallback {
        private final ISoundTriggerHal.ModelCallback mUnderlying;

        private ModelCallbackEnforcer(ISoundTriggerHal.ModelCallback underlying) {
            this.mUnderlying = underlying;
        }

        @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.ModelCallback
        public void recognitionCallback(int model, RecognitionEvent event) {
            synchronized (SoundTriggerHalEnforcer.this.mModelStates) {
                ModelState state = (ModelState) SoundTriggerHalEnforcer.this.mModelStates.get(Integer.valueOf(model));
                if (state != null && state != ModelState.INACTIVE) {
                    if (event.recognitionStillActive && event.status != 0 && event.status != 3) {
                        Log.wtfStack(SoundTriggerHalEnforcer.TAG, "recognitionStillActive is only allowed when the recognition status is SUCCESS");
                        SoundTriggerHalEnforcer.this.reboot();
                        return;
                    }
                    if (!event.recognitionStillActive) {
                        SoundTriggerHalEnforcer.this.mModelStates.replace(Integer.valueOf(model), ModelState.INACTIVE);
                    }
                    this.mUnderlying.recognitionCallback(model, event);
                    return;
                }
                Log.wtfStack(SoundTriggerHalEnforcer.TAG, "Unexpected recognition event for model: " + model);
                SoundTriggerHalEnforcer.this.reboot();
            }
        }

        @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.ModelCallback
        public void phraseRecognitionCallback(int model, PhraseRecognitionEvent event) {
            synchronized (SoundTriggerHalEnforcer.this.mModelStates) {
                ModelState state = (ModelState) SoundTriggerHalEnforcer.this.mModelStates.get(Integer.valueOf(model));
                if (state != null && state != ModelState.INACTIVE) {
                    if (event.common.recognitionStillActive && event.common.status != 0 && event.common.status != 3) {
                        Log.wtfStack(SoundTriggerHalEnforcer.TAG, "recognitionStillActive is only allowed when the recognition status is SUCCESS");
                        SoundTriggerHalEnforcer.this.reboot();
                        return;
                    }
                    int eventCommonDataLength = 0;
                    if (event.common.data != null) {
                        eventCommonDataLength = event.common.data.length;
                    }
                    if (!event.common.recognitionStillActive && (eventCommonDataLength == 0 || event.common.data[0] != 1)) {
                        Log.i(SoundTriggerHalEnforcer.TAG, "[phraseRecognitionCallback] ModelStates.replace ");
                        SoundTriggerHalEnforcer.this.mModelStates.replace(Integer.valueOf(model), ModelState.INACTIVE);
                    }
                    this.mUnderlying.phraseRecognitionCallback(model, event);
                    return;
                }
                Log.wtfStack(SoundTriggerHalEnforcer.TAG, "Unexpected recognition event for model: " + model);
                SoundTriggerHalEnforcer.this.reboot();
            }
        }

        @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.ModelCallback
        public void modelUnloaded(int modelHandle) {
            synchronized (SoundTriggerHalEnforcer.this.mModelStates) {
                ModelState state = (ModelState) SoundTriggerHalEnforcer.this.mModelStates.get(Integer.valueOf(modelHandle));
                if (state == null) {
                    Log.wtfStack(SoundTriggerHalEnforcer.TAG, "Unexpected unload event for model: " + modelHandle);
                    SoundTriggerHalEnforcer.this.reboot();
                } else if (state == ModelState.ACTIVE) {
                    Log.wtfStack(SoundTriggerHalEnforcer.TAG, "Trying to unload an active model: " + modelHandle);
                    SoundTriggerHalEnforcer.this.reboot();
                } else {
                    SoundTriggerHalEnforcer.this.mModelStates.remove(Integer.valueOf(modelHandle));
                    this.mUnderlying.modelUnloaded(modelHandle);
                }
            }
        }
    }
}
