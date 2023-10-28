package android.service.voice;

import android.hardware.soundtrigger.SoundTrigger;
import android.media.AudioFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SharedMemory;
import android.service.voice.AlwaysOnHotwordDetector;
import android.service.voice.HotwordDetector;
import android.service.voice.HotwordRejectedResult;
import android.service.voice.IMicrophoneHotwordDetectionVoiceInteractionCallback;
import android.util.Slog;
import com.android.internal.app.IHotwordRecognitionStatusCallback;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.util.function.pooled.PooledLambda;
import java.io.PrintWriter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* loaded from: classes3.dex */
class SoftwareHotwordDetector extends AbstractHotwordDetector {
    private static final boolean DEBUG = false;
    private static final String TAG = SoftwareHotwordDetector.class.getSimpleName();
    private final AudioFormat mAudioFormat;
    private final HotwordDetector.Callback mCallback;
    private final Handler mHandler;
    private final IVoiceInteractionManagerService mManagerService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SoftwareHotwordDetector(IVoiceInteractionManagerService managerService, AudioFormat audioFormat, PersistableBundle options, SharedMemory sharedMemory, HotwordDetector.Callback callback) {
        super(managerService, callback, 2);
        this.mManagerService = managerService;
        this.mAudioFormat = audioFormat;
        this.mCallback = callback;
        Handler handler = new Handler(Looper.getMainLooper());
        this.mHandler = handler;
        updateStateLocked(options, sharedMemory, new InitializationStateListener(handler, callback), 2);
    }

    @Override // android.service.voice.HotwordDetector
    public boolean startRecognition() {
        throwIfDetectorIsNoLongerActive();
        maybeCloseExistingSession();
        try {
            this.mManagerService.startListeningFromMic(this.mAudioFormat, new BinderCallback(this.mHandler, this.mCallback));
            return true;
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return true;
        } catch (SecurityException e2) {
            Slog.e(TAG, "startRecognition failed: " + e2);
            return false;
        }
    }

    @Override // android.service.voice.HotwordDetector
    public boolean stopRecognition() {
        throwIfDetectorIsNoLongerActive();
        try {
            this.mManagerService.stopListeningFromMic();
            return true;
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return true;
        }
    }

    @Override // android.service.voice.AbstractHotwordDetector, android.service.voice.HotwordDetector
    public void destroy() {
        stopRecognition();
        maybeCloseExistingSession();
        try {
            this.mManagerService.shutdownHotwordDetectionService();
        } catch (RemoteException ex) {
            ex.rethrowFromSystemServer();
        }
        super.destroy();
    }

    private void maybeCloseExistingSession() {
    }

    /* loaded from: classes3.dex */
    private static class BinderCallback extends IMicrophoneHotwordDetectionVoiceInteractionCallback.Stub {
        private final HotwordDetector.Callback mCallback;
        private final Handler mHandler;

        BinderCallback(Handler handler, HotwordDetector.Callback callback) {
            this.mHandler = handler;
            this.mCallback = callback;
        }

        @Override // android.service.voice.IMicrophoneHotwordDetectionVoiceInteractionCallback
        public void onDetected(HotwordDetectedResult hotwordDetectedResult, AudioFormat audioFormat, ParcelFileDescriptor audioStream) {
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new AbstractHotwordDetector$BinderCallback$$ExternalSyntheticLambda0(), this.mCallback, new AlwaysOnHotwordDetector.EventPayload.Builder().setCaptureAudioFormat(audioFormat).setAudioStream(audioStream).setHotwordDetectedResult(hotwordDetectedResult).build()));
        }

        @Override // android.service.voice.IMicrophoneHotwordDetectionVoiceInteractionCallback
        public void onError() {
            Slog.v(SoftwareHotwordDetector.TAG, "BinderCallback#onError");
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new AbstractHotwordDetector$BinderCallback$$ExternalSyntheticLambda1(), this.mCallback));
        }

        @Override // android.service.voice.IMicrophoneHotwordDetectionVoiceInteractionCallback
        public void onRejected(HotwordRejectedResult result) {
            if (result == null) {
                result = new HotwordRejectedResult.Builder().build();
            }
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new AbstractHotwordDetector$BinderCallback$$ExternalSyntheticLambda2(), this.mCallback, result));
        }
    }

    /* loaded from: classes3.dex */
    private static class InitializationStateListener extends IHotwordRecognitionStatusCallback.Stub {
        private final HotwordDetector.Callback mCallback;
        private final Handler mHandler;

        InitializationStateListener(Handler handler, HotwordDetector.Callback callback) {
            this.mHandler = handler;
            this.mCallback = callback;
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onKeyphraseDetected(SoundTrigger.KeyphraseRecognitionEvent recognitionEvent, HotwordDetectedResult result) {
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onGenericSoundTriggerDetected(SoundTrigger.GenericRecognitionEvent recognitionEvent) throws RemoteException {
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onRejected(HotwordRejectedResult result) throws RemoteException {
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onError(int status) throws RemoteException {
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onRecognitionPaused() throws RemoteException {
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onRecognitionResumed() throws RemoteException {
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onStatusReported(int status) {
            Slog.v(SoftwareHotwordDetector.TAG, "onStatusReported");
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: android.service.voice.SoftwareHotwordDetector$InitializationStateListener$$ExternalSyntheticLambda1
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((HotwordDetector.Callback) obj).onHotwordDetectionServiceInitialized(((Integer) obj2).intValue());
                }
            }, this.mCallback, Integer.valueOf(status)));
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onProcessRestarted() throws RemoteException {
            Slog.v(SoftwareHotwordDetector.TAG, "onProcessRestarted()");
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: android.service.voice.SoftwareHotwordDetector$InitializationStateListener$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((HotwordDetector.Callback) obj).onHotwordDetectionServiceRestarted();
                }
            }, this.mCallback));
        }
    }

    public void dump(String prefix, PrintWriter pw) {
    }
}
