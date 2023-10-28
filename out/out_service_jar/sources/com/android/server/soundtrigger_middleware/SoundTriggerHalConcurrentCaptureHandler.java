package com.android.server.soundtrigger_middleware;

import android.media.soundtrigger.ModelParameterRange;
import android.media.soundtrigger.PhraseRecognitionEvent;
import android.media.soundtrigger.PhraseSoundModel;
import android.media.soundtrigger.Properties;
import android.media.soundtrigger.RecognitionConfig;
import android.media.soundtrigger.RecognitionEvent;
import android.media.soundtrigger.SoundModel;
import android.os.IBinder;
import android.util.Log;
import com.android.server.soundtrigger_middleware.ICaptureStateNotifier;
import com.android.server.soundtrigger_middleware.ISoundTriggerHal;
import com.android.server.soundtrigger_middleware.SoundTriggerHalConcurrentCaptureHandler;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
/* loaded from: classes2.dex */
public class SoundTriggerHalConcurrentCaptureHandler implements ISoundTriggerHal, ICaptureStateNotifier.Listener {
    private static final String TAG = "SoundTriggerHalConcurrentCaptureHandler";
    private boolean mCaptureState;
    private final ISoundTriggerHal mDelegate;
    private ISoundTriggerHal.GlobalCallback mGlobalCallback;
    private final ICaptureStateNotifier mNotifier;
    private final Object mStartStopLock = new Object();
    private final Map<Integer, LoadedModel> mLoadedModels = new ConcurrentHashMap();
    private final Set<Integer> mActiveModels = new HashSet();
    private final Map<IBinder.DeathRecipient, IBinder.DeathRecipient> mDeathRecipientMap = new ConcurrentHashMap();
    private final CallbackThread mCallbackThread = new CallbackThread();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class LoadedModel {
        public final ISoundTriggerHal.ModelCallback callback;
        public final int type;

        LoadedModel(int type, ISoundTriggerHal.ModelCallback callback) {
            this.type = type;
            this.callback = callback;
        }
    }

    public SoundTriggerHalConcurrentCaptureHandler(ISoundTriggerHal delegate, ICaptureStateNotifier notifier) {
        this.mDelegate = delegate;
        this.mNotifier = notifier;
        this.mCaptureState = notifier.registerListener(this);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void startRecognition(int modelHandle, int deviceHandle, int ioHandle, RecognitionConfig config) {
        synchronized (this.mStartStopLock) {
            synchronized (this.mActiveModels) {
                Log.d(TAG, "startRecognition, mCaptureState = " + this.mCaptureState);
                if (this.mCaptureState) {
                    throw new RecoverableException(1);
                }
                this.mDelegate.startRecognition(modelHandle, deviceHandle, ioHandle, config);
                this.mActiveModels.add(Integer.valueOf(modelHandle));
            }
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void stopRecognition(int modelHandle) {
        boolean wasActive;
        synchronized (this.mStartStopLock) {
            synchronized (this.mActiveModels) {
                wasActive = this.mActiveModels.remove(Integer.valueOf(modelHandle));
            }
            if (wasActive) {
                this.mDelegate.stopRecognition(modelHandle);
            }
        }
        this.mCallbackThread.flush();
    }

    @Override // com.android.server.soundtrigger_middleware.ICaptureStateNotifier.Listener
    public void onCaptureStateChange(boolean active) {
        synchronized (this.mStartStopLock) {
            if (active) {
                abortAllActiveModels();
            } else {
                ISoundTriggerHal.GlobalCallback globalCallback = this.mGlobalCallback;
                if (globalCallback != null) {
                    globalCallback.onResourcesAvailable();
                }
            }
            this.mCaptureState = active;
            Log.d(TAG, "onCaptureStateChange, mCaptureState = " + this.mCaptureState);
        }
    }

    private void abortAllActiveModels() {
        final int toStop;
        while (true) {
            synchronized (this.mActiveModels) {
                Iterator<Integer> iterator = this.mActiveModels.iterator();
                if (!iterator.hasNext()) {
                    return;
                }
                toStop = iterator.next().intValue();
                this.mActiveModels.remove(Integer.valueOf(toStop));
            }
            this.mDelegate.stopRecognition(toStop);
            final LoadedModel model = this.mLoadedModels.get(Integer.valueOf(toStop));
            this.mCallbackThread.push(new Runnable() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHalConcurrentCaptureHandler$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    SoundTriggerHalConcurrentCaptureHandler.notifyAbort(toStop, model);
                }
            });
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadSoundModel(SoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        int handle = this.mDelegate.loadSoundModel(soundModel, new CallbackWrapper(callback));
        this.mLoadedModels.put(Integer.valueOf(handle), new LoadedModel(1, callback));
        return handle;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int loadPhraseSoundModel(PhraseSoundModel soundModel, ISoundTriggerHal.ModelCallback callback) {
        int handle = this.mDelegate.loadPhraseSoundModel(soundModel, new CallbackWrapper(callback));
        this.mLoadedModels.put(Integer.valueOf(handle), new LoadedModel(0, callback));
        return handle;
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unloadSoundModel(int modelHandle) {
        this.mLoadedModels.remove(Integer.valueOf(modelHandle));
        this.mDelegate.unloadSoundModel(modelHandle);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$registerCallback$1$com-android-server-soundtrigger_middleware-SoundTriggerHalConcurrentCaptureHandler  reason: not valid java name */
    public /* synthetic */ void m6537x8b523992(final ISoundTriggerHal.GlobalCallback callback) {
        CallbackThread callbackThread = this.mCallbackThread;
        Objects.requireNonNull(callback);
        callbackThread.push(new Runnable() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHalConcurrentCaptureHandler$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ISoundTriggerHal.GlobalCallback.this.onResourcesAvailable();
            }
        });
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void registerCallback(final ISoundTriggerHal.GlobalCallback callback) {
        ISoundTriggerHal.GlobalCallback globalCallback = new ISoundTriggerHal.GlobalCallback() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHalConcurrentCaptureHandler$$ExternalSyntheticLambda2
            @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.GlobalCallback
            public final void onResourcesAvailable() {
                SoundTriggerHalConcurrentCaptureHandler.this.m6537x8b523992(callback);
            }
        };
        this.mGlobalCallback = globalCallback;
        this.mDelegate.registerCallback(globalCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$linkToDeath$2$com-android-server-soundtrigger_middleware-SoundTriggerHalConcurrentCaptureHandler  reason: not valid java name */
    public /* synthetic */ void m6536xc4030d54(final IBinder.DeathRecipient recipient) {
        CallbackThread callbackThread = this.mCallbackThread;
        Objects.requireNonNull(recipient);
        callbackThread.push(new Runnable() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHalConcurrentCaptureHandler$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                recipient.binderDied();
            }
        });
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void linkToDeath(final IBinder.DeathRecipient recipient) {
        IBinder.DeathRecipient wrapper = new IBinder.DeathRecipient() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHalConcurrentCaptureHandler$$ExternalSyntheticLambda1
            @Override // android.os.IBinder.DeathRecipient
            public final void binderDied() {
                SoundTriggerHalConcurrentCaptureHandler.this.m6536xc4030d54(recipient);
            }
        };
        this.mDelegate.linkToDeath(wrapper);
        this.mDeathRecipientMap.put(recipient, wrapper);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void unlinkToDeath(IBinder.DeathRecipient recipient) {
        this.mDelegate.unlinkToDeath(this.mDeathRecipientMap.remove(recipient));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class CallbackWrapper implements ISoundTriggerHal.ModelCallback {
        private final ISoundTriggerHal.ModelCallback mDelegateCallback;

        private CallbackWrapper(ISoundTriggerHal.ModelCallback delegateCallback) {
            this.mDelegateCallback = delegateCallback;
        }

        @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.ModelCallback
        public void recognitionCallback(final int modelHandle, final RecognitionEvent event) {
            synchronized (SoundTriggerHalConcurrentCaptureHandler.this.mActiveModels) {
                if (SoundTriggerHalConcurrentCaptureHandler.this.mActiveModels.contains(Integer.valueOf(modelHandle))) {
                    if (!event.recognitionStillActive) {
                        SoundTriggerHalConcurrentCaptureHandler.this.mActiveModels.remove(Integer.valueOf(modelHandle));
                    }
                    SoundTriggerHalConcurrentCaptureHandler.this.mCallbackThread.push(new Runnable() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHalConcurrentCaptureHandler$CallbackWrapper$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            SoundTriggerHalConcurrentCaptureHandler.CallbackWrapper.this.m6541x287a67c3(modelHandle, event);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$recognitionCallback$0$com-android-server-soundtrigger_middleware-SoundTriggerHalConcurrentCaptureHandler$CallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m6541x287a67c3(int modelHandle, RecognitionEvent event) {
            this.mDelegateCallback.recognitionCallback(modelHandle, event);
        }

        @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.ModelCallback
        public void phraseRecognitionCallback(final int modelHandle, final PhraseRecognitionEvent event) {
            synchronized (SoundTriggerHalConcurrentCaptureHandler.this.mActiveModels) {
                if (SoundTriggerHalConcurrentCaptureHandler.this.mActiveModels.contains(Integer.valueOf(modelHandle))) {
                    int eventDataLength = 0;
                    if (event.common.data != null) {
                        eventDataLength = event.common.data.length;
                    }
                    if (!event.common.recognitionStillActive && (eventDataLength == 0 || event.common.data[0] != 1)) {
                        SoundTriggerHalConcurrentCaptureHandler.this.mActiveModels.remove(Integer.valueOf(modelHandle));
                    }
                    SoundTriggerHalConcurrentCaptureHandler.this.mCallbackThread.push(new Runnable() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHalConcurrentCaptureHandler$CallbackWrapper$$ExternalSyntheticLambda2
                        @Override // java.lang.Runnable
                        public final void run() {
                            SoundTriggerHalConcurrentCaptureHandler.CallbackWrapper.this.m6540x1772423b(modelHandle, event);
                        }
                    });
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$phraseRecognitionCallback$1$com-android-server-soundtrigger_middleware-SoundTriggerHalConcurrentCaptureHandler$CallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m6540x1772423b(int modelHandle, PhraseRecognitionEvent event) {
            this.mDelegateCallback.phraseRecognitionCallback(modelHandle, event);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$modelUnloaded$2$com-android-server-soundtrigger_middleware-SoundTriggerHalConcurrentCaptureHandler$CallbackWrapper  reason: not valid java name */
        public /* synthetic */ void m6539xff18dc16(int modelHandle) {
            this.mDelegateCallback.modelUnloaded(modelHandle);
        }

        @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal.ModelCallback
        public void modelUnloaded(final int modelHandle) {
            SoundTriggerHalConcurrentCaptureHandler.this.mCallbackThread.push(new Runnable() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHalConcurrentCaptureHandler$CallbackWrapper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    SoundTriggerHalConcurrentCaptureHandler.CallbackWrapper.this.m6539xff18dc16(modelHandle);
                }
            });
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void flushCallbacks() {
        this.mDelegate.flushCallbacks();
        this.mCallbackThread.flush();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class CallbackThread {
        private final Queue<Runnable> mList = new LinkedList();
        private int mPushCount = 0;
        private int mProcessedCount = 0;

        CallbackThread() {
            new Thread(new Runnable() { // from class: com.android.server.soundtrigger_middleware.SoundTriggerHalConcurrentCaptureHandler$CallbackThread$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SoundTriggerHalConcurrentCaptureHandler.CallbackThread.this.m6538xfa01e55e();
                }
            }).start();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-soundtrigger_middleware-SoundTriggerHalConcurrentCaptureHandler$CallbackThread  reason: not valid java name */
        public /* synthetic */ void m6538xfa01e55e() {
            while (true) {
                try {
                    pop().run();
                    synchronized (this.mList) {
                        this.mProcessedCount++;
                        this.mList.notifyAll();
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        void push(Runnable runnable) {
            synchronized (this.mList) {
                this.mList.add(runnable);
                this.mPushCount++;
                this.mList.notifyAll();
            }
        }

        void flush() {
            try {
                synchronized (this.mList) {
                    int pushCount = this.mPushCount;
                    while (this.mProcessedCount != pushCount) {
                        this.mList.wait();
                    }
                }
            } catch (InterruptedException e) {
            }
        }

        private Runnable pop() throws InterruptedException {
            Runnable remove;
            synchronized (this.mList) {
                while (this.mList.isEmpty()) {
                    this.mList.wait();
                }
                remove = this.mList.remove();
            }
            return remove;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void notifyAbort(int modelHandle, LoadedModel model) {
        switch (model.type) {
            case 0:
                model.callback.phraseRecognitionCallback(modelHandle, AidlUtil.newAbortPhraseEvent());
                return;
            case 1:
                model.callback.recognitionCallback(modelHandle, AidlUtil.newAbortEvent());
                return;
            default:
                return;
        }
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void detach() {
        this.mDelegate.detach();
        this.mNotifier.unregisterListener(this);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void reboot() {
        this.mDelegate.reboot();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public Properties getProperties() {
        return this.mDelegate.getProperties();
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void forceRecognitionEvent(int modelHandle) {
        this.mDelegate.forceRecognitionEvent(modelHandle);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public int getModelParameter(int modelHandle, int param) {
        return this.mDelegate.getModelParameter(modelHandle, param);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public void setModelParameter(int modelHandle, int param, int value) {
        this.mDelegate.setModelParameter(modelHandle, param, value);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public ModelParameterRange queryParameter(int modelHandle, int param) {
        return this.mDelegate.queryParameter(modelHandle, param);
    }

    @Override // com.android.server.soundtrigger_middleware.ISoundTriggerHal
    public String interfaceDescriptor() {
        return this.mDelegate.interfaceDescriptor();
    }
}
