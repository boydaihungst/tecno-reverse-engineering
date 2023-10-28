package com.android.server.soundtrigger_middleware;

import android.media.permission.Identity;
import android.media.permission.IdentityContext;
import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseRecognitionEvent;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.Properties;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.RecognitionEvent;
import android.media.soundtrigger.SoundModel;
import android.media.soundtrigger_middleware.ISoundTriggerCallback;
import android.media.soundtrigger_middleware.ISoundTriggerModule;
import android.media.soundtrigger_middleware.SoundTriggerModuleDescriptor;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.server.soundtrigger_middleware.SoundTriggerMiddlewareValidation;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public class SoundTriggerMiddlewareValidation implements ISoundTriggerMiddlewareInternal, Dumpable {
    private static final String TAG = "SoundTriggerMiddlewareValidation";
    private final ISoundTriggerMiddlewareInternal mDelegate;
    private Map<Integer, ModuleState> mModules;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public enum ModuleStatus {
        ALIVE,
        DETACHED,
        DEAD
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ModuleState {
        public Properties properties;
        public Set<Session> sessions;

        private ModuleState(Properties properties) {
            this.sessions = new HashSet();
            this.properties = properties;
        }
    }

    public SoundTriggerMiddlewareValidation(ISoundTriggerMiddlewareInternal delegate) {
        this.mDelegate = delegate;
    }

    static RuntimeException handleException(Exception e) {
        if (e instanceof RecoverableException) {
            throw new ServiceSpecificException(((RecoverableException) e).errorCode, e.getMessage());
        }
        Log.wtf(TAG, "Unexpected exception", e);
        throw new ServiceSpecificException(5, e.getMessage());
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerMiddlewareInternal
    public SoundTriggerModuleDescriptor[] listModules() {
        SoundTriggerModuleDescriptor[] result;
        synchronized (this) {
            try {
                result = this.mDelegate.listModules();
                Map<Integer, ModuleState> map = this.mModules;
                int i = 0;
                if (map == null) {
                    this.mModules = new HashMap(result.length);
                    int length = result.length;
                    while (i < length) {
                        SoundTriggerModuleDescriptor desc = result[i];
                        this.mModules.put(Integer.valueOf(desc.handle), new ModuleState(desc.properties));
                        i++;
                    }
                } else if (result.length != map.size()) {
                    throw new RuntimeException("listModules must always return the same result.");
                } else {
                    int length2 = result.length;
                    while (i < length2) {
                        SoundTriggerModuleDescriptor desc2 = result[i];
                        if (!this.mModules.containsKey(Integer.valueOf(desc2.handle))) {
                            throw new RuntimeException("listModules must always return the same result.");
                        }
                        this.mModules.get(Integer.valueOf(desc2.handle)).properties = desc2.properties;
                        i++;
                    }
                }
            } catch (Exception e) {
                throw handleException(e);
            }
        }
        return result;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerMiddlewareInternal
    public ISoundTriggerModule attach(int handle, ISoundTriggerCallback callback) {
        Session session;
        Objects.requireNonNull(callback);
        Objects.requireNonNull(callback.asBinder());
        synchronized (this) {
            Map<Integer, ModuleState> map = this.mModules;
            if (map == null) {
                throw new IllegalStateException("Client must call listModules() prior to attaching.");
            }
            if (!map.containsKey(Integer.valueOf(handle))) {
                throw new IllegalArgumentException("Invalid handle: " + handle);
            }
            try {
                session = new Session(handle, callback);
                session.attach(this.mDelegate.attach(handle, session.getCallbackWrapper()));
            } catch (Exception e) {
                throw handleException(e);
            }
        }
        return session;
    }

    public String toString() {
        return this.mDelegate.toString();
    }

    @Override // com.android.server.soundtrigger_middleware.Dumpable
    public void dump(PrintWriter pw) {
        synchronized (this) {
            Map<Integer, ModuleState> map = this.mModules;
            if (map != null) {
                for (Integer num : map.keySet()) {
                    int handle = num.intValue();
                    ModuleState module = this.mModules.get(Integer.valueOf(handle));
                    pw.println("=========================================");
                    pw.printf("Module %d\n%s\n", Integer.valueOf(handle), ObjectPrinter.print(module.properties, 16));
                    pw.println("=========================================");
                    for (Session session : module.sessions) {
                        session.dump(pw);
                    }
                }
            } else {
                pw.println("Modules have not yet been enumerated.");
            }
        }
        pw.println();
        ISoundTriggerMiddlewareInternal iSoundTriggerMiddlewareInternal = this.mDelegate;
        if (iSoundTriggerMiddlewareInternal instanceof Dumpable) {
            ((Dumpable) iSoundTriggerMiddlewareInternal).dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class ModelState {
        RecognitionConfig config;
        final String description;
        Activity activityState = Activity.LOADED;
        private final Map<Integer, ModelParameterRange> parameterSupport = new HashMap();

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public enum Activity {
            LOADED,
            ACTIVE,
            INTERCEPTED,
            PREEMPTED
        }

        ModelState(SoundModel model) {
            this.description = ObjectPrinter.print(model, 16);
        }

        ModelState(PhraseSoundModel model) {
            this.description = ObjectPrinter.print(model, 16);
        }

        void checkSupported(int modelParam) {
            if (!this.parameterSupport.containsKey(Integer.valueOf(modelParam))) {
                throw new IllegalStateException("Parameter has not been checked for support.");
            }
            ModelParameterRange range = this.parameterSupport.get(Integer.valueOf(modelParam));
            if (range == null) {
                throw new IllegalArgumentException("Paramater is not supported.");
            }
        }

        void checkSupported(int modelParam, int value) {
            if (!this.parameterSupport.containsKey(Integer.valueOf(modelParam))) {
                throw new IllegalStateException("Parameter has not been checked for support.");
            }
            ModelParameterRange range = this.parameterSupport.get(Integer.valueOf(modelParam));
            if (range == null) {
                throw new IllegalArgumentException("Paramater is not supported.");
            }
            Preconditions.checkArgumentInRange(value, range.minInclusive, range.maxInclusive, "value");
        }
    }

    /* loaded from: classes2.dex */
    private class Session extends ISoundTriggerModule.Stub {
        private final CallbackWrapper mCallbackWrapper;
        private ISoundTriggerModule mDelegate;
        private final int mHandle;
        private final Map<Integer, ModelState> mLoadedModels = new HashMap();
        private ModuleStatus mState = ModuleStatus.ALIVE;
        private final Identity mOriginatorIdentity = IdentityContext.get();

        Session(int handle, ISoundTriggerCallback callback) {
            this.mCallbackWrapper = new CallbackWrapper(callback);
            this.mHandle = handle;
        }

        ISoundTriggerCallback getCallbackWrapper() {
            return this.mCallbackWrapper;
        }

        void attach(ISoundTriggerModule delegate) {
            this.mDelegate = delegate;
            ((ModuleState) SoundTriggerMiddlewareValidation.this.mModules.get(Integer.valueOf(this.mHandle))).sessions.add(this);
        }

        public int loadModel(SoundModel model) {
            int handle;
            ValidationUtil.validateGenericModel(model);
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    throw new IllegalStateException("Module has been detached.");
                }
                try {
                    handle = this.mDelegate.loadModel(model);
                    this.mLoadedModels.put(Integer.valueOf(handle), new ModelState(model));
                } catch (Exception e) {
                    throw SoundTriggerMiddlewareValidation.handleException(e);
                }
            }
            return handle;
        }

        public int loadPhraseModel(PhraseSoundModel model) {
            int handle;
            ValidationUtil.validatePhraseModel(model);
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    throw new IllegalStateException("Module has been detached.");
                }
                try {
                    handle = this.mDelegate.loadPhraseModel(model);
                    this.mLoadedModels.put(Integer.valueOf(handle), new ModelState(model));
                } catch (Exception e) {
                    throw SoundTriggerMiddlewareValidation.handleException(e);
                }
            }
            return handle;
        }

        public void unloadModel(int modelHandle) {
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    throw new IllegalStateException("Module has been detached.");
                }
                ModelState modelState = this.mLoadedModels.get(Integer.valueOf(modelHandle));
                if (modelState == null) {
                    throw new IllegalStateException("Invalid handle: " + modelHandle);
                }
                if (modelState.activityState != ModelState.Activity.LOADED && modelState.activityState != ModelState.Activity.PREEMPTED) {
                    throw new IllegalStateException("Model with handle: " + modelHandle + " has invalid state for unloading");
                }
            }
            try {
                this.mDelegate.unloadModel(modelHandle);
                synchronized (SoundTriggerMiddlewareValidation.this) {
                    this.mLoadedModels.remove(Integer.valueOf(modelHandle));
                }
            } catch (Exception e) {
                throw SoundTriggerMiddlewareValidation.handleException(e);
            }
        }

        public void startRecognition(int modelHandle, RecognitionConfig config) {
            ValidationUtil.validateRecognitionConfig(config);
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    throw new IllegalStateException("Module has been detached.");
                }
                ModelState modelState = this.mLoadedModels.get(Integer.valueOf(modelHandle));
                if (modelState == null) {
                    throw new IllegalStateException("Invalid handle: " + modelHandle);
                }
                ModelState.Activity activityState = modelState.activityState;
                if (activityState != ModelState.Activity.LOADED && activityState != ModelState.Activity.PREEMPTED) {
                    throw new IllegalStateException("Model with handle: " + modelHandle + " has invalid state for starting recognition");
                }
                try {
                    this.mDelegate.startRecognition(modelHandle, config);
                    modelState.config = config;
                    modelState.activityState = ModelState.Activity.ACTIVE;
                } catch (Exception e) {
                    throw SoundTriggerMiddlewareValidation.handleException(e);
                }
            }
        }

        public void stopRecognition(int modelHandle) {
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    throw new IllegalStateException("Module has been detached.");
                }
                ModelState modelState = this.mLoadedModels.get(Integer.valueOf(modelHandle));
                if (modelState == null) {
                    throw new IllegalStateException("Invalid handle: " + modelHandle);
                }
                try {
                    if (modelState.activityState == ModelState.Activity.INTERCEPTED) {
                        modelState.activityState = ModelState.Activity.LOADED;
                        return;
                    }
                    try {
                        this.mDelegate.stopRecognition(modelHandle);
                        synchronized (SoundTriggerMiddlewareValidation.this) {
                            ModelState modelState2 = this.mLoadedModels.get(Integer.valueOf(modelHandle));
                            if (modelState2 == null) {
                                return;
                            }
                            if (modelState2.activityState != ModelState.Activity.PREEMPTED) {
                                modelState2.activityState = ModelState.Activity.LOADED;
                            }
                        }
                    } catch (Exception e) {
                        throw SoundTriggerMiddlewareValidation.handleException(e);
                    }
                } catch (Exception e2) {
                    throw SoundTriggerMiddlewareValidation.handleException(e2);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void restartIfIntercepted(int modelHandle) {
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    return;
                }
                ModelState modelState = this.mLoadedModels.get(Integer.valueOf(modelHandle));
                if (modelState == null || modelState.activityState != ModelState.Activity.INTERCEPTED) {
                    return;
                }
                try {
                    this.mDelegate.startRecognition(modelHandle, modelState.config);
                    modelState.activityState = ModelState.Activity.ACTIVE;
                    Log.i(SoundTriggerMiddlewareValidation.TAG, "Restarted intercepted model " + modelHandle);
                } catch (Exception e) {
                    Log.i(SoundTriggerMiddlewareValidation.TAG, "Failed to restart intercepted model " + modelHandle, e);
                }
            }
        }

        public void forceRecognitionEvent(int modelHandle) {
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    throw new IllegalStateException("Module has been detached.");
                }
                ModelState modelState = this.mLoadedModels.get(Integer.valueOf(modelHandle));
                if (modelState == null) {
                    throw new IllegalStateException("Invalid handle: " + modelHandle);
                }
                try {
                    if (modelState.activityState == ModelState.Activity.ACTIVE) {
                        this.mDelegate.forceRecognitionEvent(modelHandle);
                    }
                } catch (Exception e) {
                    throw SoundTriggerMiddlewareValidation.handleException(e);
                }
            }
        }

        public void setModelParameter(int modelHandle, int modelParam, int value) {
            ValidationUtil.validateModelParameter(modelParam);
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    throw new IllegalStateException("Module has been detached.");
                }
                ModelState modelState = this.mLoadedModels.get(Integer.valueOf(modelHandle));
                if (modelState == null) {
                    throw new IllegalStateException("Invalid handle: " + modelHandle);
                }
                modelState.checkSupported(modelParam, value);
                try {
                    this.mDelegate.setModelParameter(modelHandle, modelParam, value);
                } catch (Exception e) {
                    throw SoundTriggerMiddlewareValidation.handleException(e);
                }
            }
        }

        public int getModelParameter(int modelHandle, int modelParam) {
            int modelParameter;
            ValidationUtil.validateModelParameter(modelParam);
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    throw new IllegalStateException("Module has been detached.");
                }
                ModelState modelState = this.mLoadedModels.get(Integer.valueOf(modelHandle));
                if (modelState == null) {
                    throw new IllegalStateException("Invalid handle: " + modelHandle);
                }
                modelState.checkSupported(modelParam);
                try {
                    modelParameter = this.mDelegate.getModelParameter(modelHandle, modelParam);
                } catch (Exception e) {
                    throw SoundTriggerMiddlewareValidation.handleException(e);
                }
            }
            return modelParameter;
        }

        public ModelParameterRange queryModelParameterSupport(int modelHandle, int modelParam) {
            ModelParameterRange result;
            ValidationUtil.validateModelParameter(modelParam);
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    throw new IllegalStateException("Module has been detached.");
                }
                ModelState modelState = this.mLoadedModels.get(Integer.valueOf(modelHandle));
                if (modelState == null) {
                    throw new IllegalStateException("Invalid handle: " + modelHandle);
                }
                try {
                    result = this.mDelegate.queryModelParameterSupport(modelHandle, modelParam);
                    modelState.parameterSupport.put(Integer.valueOf(modelParam), result);
                } catch (Exception e) {
                    throw SoundTriggerMiddlewareValidation.handleException(e);
                }
            }
            return result;
        }

        public void detach() {
            synchronized (SoundTriggerMiddlewareValidation.this) {
                if (this.mState == ModuleStatus.DETACHED) {
                    throw new IllegalStateException("Module has already been detached.");
                }
                if (this.mState == ModuleStatus.ALIVE && !this.mLoadedModels.isEmpty()) {
                    throw new IllegalStateException("Cannot detach while models are loaded.");
                }
                try {
                    detachInternal();
                } catch (Exception e) {
                    throw SoundTriggerMiddlewareValidation.handleException(e);
                }
            }
        }

        public String toString() {
            return Objects.toString(this.mDelegate);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void detachInternal() {
            try {
                this.mDelegate.detach();
                this.mState = ModuleStatus.DETACHED;
                this.mCallbackWrapper.detached();
                ((ModuleState) SoundTriggerMiddlewareValidation.this.mModules.get(Integer.valueOf(this.mHandle))).sessions.remove(this);
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }

        void dump(PrintWriter pw) {
            if (this.mState == ModuleStatus.ALIVE) {
                pw.println("-------------------------------");
                pw.printf("Session %s, client: %s\n", toString(), ObjectPrinter.print(this.mOriginatorIdentity, 16));
                pw.println("Loaded models (handle, active, description):");
                pw.println();
                pw.println("-------------------------------");
                for (Map.Entry<Integer, ModelState> entry : this.mLoadedModels.entrySet()) {
                    pw.print(entry.getKey());
                    pw.print('\t');
                    pw.print(entry.getValue().activityState.name());
                    pw.print('\t');
                    pw.print(entry.getValue().description);
                    pw.println();
                }
                pw.println();
                return;
            }
            pw.printf("Session %s is dead", toString());
            pw.println();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes2.dex */
        public class CallbackWrapper implements ISoundTriggerCallback, IBinder.DeathRecipient {
            private final ISoundTriggerCallback mCallback;

            CallbackWrapper(ISoundTriggerCallback callback) {
                this.mCallback = callback;
                try {
                    callback.asBinder().linkToDeath(this, 0);
                } catch (RemoteException e) {
                    throw e.rethrowAsRuntimeException();
                }
            }

            void detached() {
                this.mCallback.asBinder().unlinkToDeath(this, 0);
            }

            public void onRecognition(final int modelHandle, RecognitionEvent event, int captureSession) {
                synchronized (SoundTriggerMiddlewareValidation.this) {
                    ModelState modelState = (ModelState) Session.this.mLoadedModels.get(Integer.valueOf(modelHandle));
                    if (!event.recognitionStillActive) {
                        modelState.activityState = ModelState.Activity.LOADED;
                    }
                }
                try {
                    this.mCallback.onRecognition(modelHandle, event, captureSession);
                } catch (Exception e) {
                    Log.w(SoundTriggerMiddlewareValidation.TAG, "Client callback exception.", e);
                    synchronized (SoundTriggerMiddlewareValidation.this) {
                        ModelState modelState2 = (ModelState) Session.this.mLoadedModels.get(Integer.valueOf(modelHandle));
                        if (event.status != 3) {
                            modelState2.activityState = ModelState.Activity.INTERCEPTED;
                            new Thread(new Runnable() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerMiddlewareValidation$Session$CallbackWrapper$$ExternalSyntheticLambda0
                                @Override // java.lang.Runnable
                                public final void run() {
                                    SoundTriggerMiddlewareValidation.Session.CallbackWrapper.this.m6559x6d6e3698(modelHandle);
                                }
                            }).start();
                        }
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$onRecognition$0$com-android-server-soundtrigger_middleware-SoundTriggerMiddlewareValidation$Session$CallbackWrapper  reason: not valid java name */
            public /* synthetic */ void m6559x6d6e3698(int modelHandle) {
                Session.this.restartIfIntercepted(modelHandle);
            }

            public void onPhraseRecognition(final int modelHandle, PhraseRecognitionEvent event, int captureSession) {
                synchronized (SoundTriggerMiddlewareValidation.this) {
                    ModelState modelState = (ModelState) Session.this.mLoadedModels.get(Integer.valueOf(modelHandle));
                    if (!event.common.recognitionStillActive) {
                        modelState.activityState = ModelState.Activity.LOADED;
                    }
                }
                try {
                    this.mCallback.onPhraseRecognition(modelHandle, event, captureSession);
                } catch (Exception e) {
                    Log.w(SoundTriggerMiddlewareValidation.TAG, "Client callback exception.", e);
                    synchronized (SoundTriggerMiddlewareValidation.this) {
                        ModelState modelState2 = (ModelState) Session.this.mLoadedModels.get(Integer.valueOf(modelHandle));
                        if (!event.common.recognitionStillActive) {
                            modelState2.activityState = ModelState.Activity.INTERCEPTED;
                            new Thread(new Runnable() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerMiddlewareValidation$Session$CallbackWrapper$$ExternalSyntheticLambda1
                                @Override // java.lang.Runnable
                                public final void run() {
                                    SoundTriggerMiddlewareValidation.Session.CallbackWrapper.this.m6558xf95e0560(modelHandle);
                                }
                            }).start();
                        }
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$onPhraseRecognition$1$com-android-server-soundtrigger_middleware-SoundTriggerMiddlewareValidation$Session$CallbackWrapper  reason: not valid java name */
            public /* synthetic */ void m6558xf95e0560(int modelHandle) {
                Session.this.restartIfIntercepted(modelHandle);
            }

            public void onModelUnloaded(int modelHandle) {
                synchronized (SoundTriggerMiddlewareValidation.this) {
                    ModelState modelState = (ModelState) Session.this.mLoadedModels.get(Integer.valueOf(modelHandle));
                    modelState.activityState = ModelState.Activity.PREEMPTED;
                }
                try {
                    this.mCallback.onModelUnloaded(modelHandle);
                } catch (Exception e) {
                    Log.w(SoundTriggerMiddlewareValidation.TAG, "Client callback exception.", e);
                }
            }

            public void onResourcesAvailable() {
                try {
                    this.mCallback.onResourcesAvailable();
                } catch (RemoteException e) {
                    Log.e(SoundTriggerMiddlewareValidation.TAG, "Client callback exception.", e);
                }
            }

            public void onModuleDied() {
                synchronized (SoundTriggerMiddlewareValidation.this) {
                    Session.this.mState = ModuleStatus.DEAD;
                }
                try {
                    this.mCallback.onModuleDied();
                } catch (RemoteException e) {
                    Log.e(SoundTriggerMiddlewareValidation.TAG, "Client callback exception.", e);
                }
            }

            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                synchronized (SoundTriggerMiddlewareValidation.this) {
                    try {
                        try {
                            for (Map.Entry<Integer, ModelState> entry : Session.this.mLoadedModels.entrySet()) {
                                if (entry.getValue().activityState == ModelState.Activity.ACTIVE) {
                                    Session.this.mDelegate.stopRecognition(entry.getKey().intValue());
                                }
                                Session.this.mDelegate.unloadModel(entry.getKey().intValue());
                            }
                            Session.this.detachInternal();
                        } catch (Exception e) {
                            throw SoundTriggerMiddlewareValidation.handleException(e);
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }

            public IBinder asBinder() {
                return this.mCallback.asBinder();
            }

            public String toString() {
                return Objects.toString(Session.this.mDelegate);
            }
        }
    }
}
