package android.service.voice;

import android.app.ActivityThread;
import android.media.AudioFormat;
import android.media.permission.Identity;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes3.dex */
public abstract class AbstractHotwordDetector implements HotwordDetector {
    private static final boolean DEBUG = false;
    private static final String TAG = AbstractHotwordDetector.class.getSimpleName();
    private final HotwordDetector.Callback mCallback;
    private final int mDetectorType;
    private final IVoiceInteractionManagerService mManagerService;
    private Consumer<AbstractHotwordDetector> mOnDestroyListener;
    protected final Object mLock = new Object();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean mIsDetectorActive = new AtomicBoolean(true);

    /* JADX INFO: Access modifiers changed from: package-private */
    public AbstractHotwordDetector(IVoiceInteractionManagerService managerService, HotwordDetector.Callback callback, int detectorType) {
        this.mManagerService = managerService;
        this.mCallback = callback;
        this.mDetectorType = detectorType;
    }

    @Override // android.service.voice.HotwordDetector
    public boolean startRecognition(ParcelFileDescriptor audioStream, AudioFormat audioFormat, PersistableBundle options) {
        throwIfDetectorIsNoLongerActive();
        try {
            this.mManagerService.startListeningFromExternalSource(audioStream, audioFormat, options, new BinderCallback(this.mHandler, this.mCallback));
            return true;
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return true;
        }
    }

    @Override // android.service.voice.HotwordDetector
    public void updateState(PersistableBundle options, SharedMemory sharedMemory) {
        throwIfDetectorIsNoLongerActive();
        synchronized (this.mLock) {
            updateStateLocked(options, sharedMemory, null, this.mDetectorType);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateStateLocked(PersistableBundle options, SharedMemory sharedMemory, IHotwordRecognitionStatusCallback callback, int detectorType) {
        Identity identity = new Identity();
        identity.packageName = ActivityThread.currentOpPackageName();
        try {
            this.mManagerService.updateState(identity, options, sharedMemory, callback, detectorType);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerOnDestroyListener(Consumer<AbstractHotwordDetector> onDestroyListener) {
        synchronized (this.mLock) {
            if (this.mOnDestroyListener != null) {
                throw new IllegalStateException("only one destroy listener can be registered");
            }
            this.mOnDestroyListener = onDestroyListener;
        }
    }

    @Override // android.service.voice.HotwordDetector
    public void destroy() {
        if (!this.mIsDetectorActive.get()) {
            return;
        }
        this.mIsDetectorActive.set(false);
        synchronized (this.mLock) {
            this.mOnDestroyListener.accept(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void throwIfDetectorIsNoLongerActive() {
        if (!this.mIsDetectorActive.get()) {
            Slog.e(TAG, "attempting to use a destroyed detector which is no longer active");
            throw new IllegalStateException("attempting to use a destroyed detector which is no longer active");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class BinderCallback extends IMicrophoneHotwordDetectionVoiceInteractionCallback.Stub {
        private final HotwordDetector.Callback mCallback;
        private final Handler mHandler;

        BinderCallback(Handler handler, HotwordDetector.Callback callback) {
            this.mHandler = handler;
            this.mCallback = callback;
        }

        @Override // android.service.voice.IMicrophoneHotwordDetectionVoiceInteractionCallback
        public void onDetected(HotwordDetectedResult hotwordDetectedResult, AudioFormat audioFormat, ParcelFileDescriptor audioStreamIgnored) {
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new AbstractHotwordDetector$BinderCallback$$ExternalSyntheticLambda0(), this.mCallback, new AlwaysOnHotwordDetector.EventPayload.Builder().setCaptureAudioFormat(audioFormat).setHotwordDetectedResult(hotwordDetectedResult).build()));
        }

        @Override // android.service.voice.IMicrophoneHotwordDetectionVoiceInteractionCallback
        public void onError() {
            Slog.v(AbstractHotwordDetector.TAG, "BinderCallback#onError");
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
}
