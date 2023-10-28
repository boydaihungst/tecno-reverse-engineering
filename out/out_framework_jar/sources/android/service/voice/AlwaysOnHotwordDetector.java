package android.service.voice;

import android.annotation.SystemApi;
import android.app.ActivityThread;
import android.content.Intent;
import android.hardware.soundtrigger.KeyphraseEnrollmentInfo;
import android.hardware.soundtrigger.KeyphraseMetadata;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.AudioFormat;
import android.media.permission.Identity;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SharedMemory;
import android.service.voice.HotwordDetector;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.IHotwordRecognitionStatusCallback;
import com.android.internal.app.IVoiceInteractionManagerService;
import com.android.internal.app.IVoiceInteractionSoundTriggerSession;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
@SystemApi
/* loaded from: classes3.dex */
public class AlwaysOnHotwordDetector extends AbstractHotwordDetector {
    public static final int AUDIO_CAPABILITY_ECHO_CANCELLATION = 1;
    public static final int AUDIO_CAPABILITY_NOISE_SUPPRESSION = 2;
    static final boolean DBG = false;
    public static final int MODEL_PARAM_THRESHOLD_FACTOR = 0;
    private static final int MSG_AVAILABILITY_CHANGED = 1;
    private static final int MSG_DETECTION_ERROR = 3;
    private static final int MSG_DETECTION_PAUSE = 4;
    private static final int MSG_DETECTION_RESUME = 5;
    private static final int MSG_HOTWORD_DETECTED = 2;
    private static final int MSG_HOTWORD_REJECTED = 6;
    private static final int MSG_HOTWORD_STATUS_REPORTED = 7;
    private static final int MSG_PROCESS_RESTARTED = 8;
    public static final int RECOGNITION_FLAG_ALLOW_MULTIPLE_TRIGGERS = 2;
    public static final int RECOGNITION_FLAG_CAPTURE_TRIGGER_AUDIO = 1;
    public static final int RECOGNITION_FLAG_ENABLE_AUDIO_ECHO_CANCELLATION = 4;
    public static final int RECOGNITION_FLAG_ENABLE_AUDIO_NOISE_SUPPRESSION = 8;
    public static final int RECOGNITION_FLAG_NONE = 0;
    public static final int RECOGNITION_FLAG_RUN_IN_BATTERY_SAVER = 16;
    public static final int RECOGNITION_MODE_USER_IDENTIFICATION = 2;
    public static final int RECOGNITION_MODE_VOICE_TRIGGER = 1;
    public static final int STATE_ERROR = 3;
    public static final int STATE_HARDWARE_UNAVAILABLE = -2;
    private static final int STATE_INVALID = -3;
    public static final int STATE_KEYPHRASE_ENROLLED = 2;
    public static final int STATE_KEYPHRASE_UNENROLLED = 1;
    @Deprecated
    public static final int STATE_KEYPHRASE_UNSUPPORTED = -1;
    private static final int STATE_NOT_READY = 0;
    private static final int STATUS_ERROR = Integer.MIN_VALUE;
    private static final int STATUS_OK = 0;
    static final String TAG = "AlwaysOnHotwordDetector";
    private int mAvailability;
    private final IBinder mBinder;
    private final Callback mExternalCallback;
    private final Handler mHandler;
    private final SoundTriggerListener mInternalCallback;
    private final KeyphraseEnrollmentInfo mKeyphraseEnrollmentInfo;
    private KeyphraseMetadata mKeyphraseMetadata;
    private final Locale mLocale;
    private final IVoiceInteractionManagerService mModelManagementService;
    private final IVoiceInteractionSoundTriggerSession mSoundTriggerSession;
    private final boolean mSupportHotwordDetectionService;
    private final int mTargetSdkVersion;
    private final String mText;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface AudioCapabilities {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface ModelParams {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface RecognitionFlags {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface RecognitionModes {
    }

    @Override // android.service.voice.AbstractHotwordDetector, android.service.voice.HotwordDetector
    public /* bridge */ /* synthetic */ boolean startRecognition(ParcelFileDescriptor parcelFileDescriptor, AudioFormat audioFormat, PersistableBundle persistableBundle) {
        return super.startRecognition(parcelFileDescriptor, audioFormat, persistableBundle);
    }

    /* loaded from: classes3.dex */
    public static final class ModelParamRange {
        private final SoundTrigger.ModelParamRange mModelParamRange;

        ModelParamRange(SoundTrigger.ModelParamRange modelParamRange) {
            this.mModelParamRange = modelParamRange;
        }

        public int getStart() {
            return this.mModelParamRange.getStart();
        }

        public int getEnd() {
            return this.mModelParamRange.getEnd();
        }

        public String toString() {
            return this.mModelParamRange.toString();
        }

        public boolean equals(Object obj) {
            return this.mModelParamRange.equals(obj);
        }

        public int hashCode() {
            return this.mModelParamRange.hashCode();
        }
    }

    /* loaded from: classes3.dex */
    public static class EventPayload {
        public static final int DATA_FORMAT_RAW = 0;
        public static final int DATA_FORMAT_TRIGGER_AUDIO = 1;
        private final AudioFormat mAudioFormat;
        private final ParcelFileDescriptor mAudioStream;
        private final boolean mCaptureAvailable;
        private final int mCaptureSession;
        private final byte[] mData;
        private final int mDataFormat;
        private final HotwordDetectedResult mHotwordDetectedResult;
        private final List<SoundTrigger.KeyphraseRecognitionExtra> mKephraseExtras;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes3.dex */
        public @interface DataFormat {
        }

        private EventPayload(boolean captureAvailable, AudioFormat audioFormat, int captureSession, int dataFormat, byte[] data, HotwordDetectedResult hotwordDetectedResult, ParcelFileDescriptor audioStream, List<SoundTrigger.KeyphraseRecognitionExtra> keyphraseExtras) {
            this.mCaptureAvailable = captureAvailable;
            this.mCaptureSession = captureSession;
            this.mAudioFormat = audioFormat;
            this.mDataFormat = dataFormat;
            this.mData = data;
            this.mHotwordDetectedResult = hotwordDetectedResult;
            this.mAudioStream = audioStream;
            this.mKephraseExtras = keyphraseExtras;
        }

        public AudioFormat getCaptureAudioFormat() {
            return this.mAudioFormat;
        }

        @Deprecated
        public byte[] getTriggerAudio() {
            if (this.mDataFormat == 1) {
                return this.mData;
            }
            return null;
        }

        public int getDataFormat() {
            return this.mDataFormat;
        }

        public byte[] getData() {
            return this.mData;
        }

        public Integer getCaptureSession() {
            if (this.mCaptureAvailable) {
                return Integer.valueOf(this.mCaptureSession);
            }
            return null;
        }

        public HotwordDetectedResult getHotwordDetectedResult() {
            return this.mHotwordDetectedResult;
        }

        public ParcelFileDescriptor getAudioStream() {
            return this.mAudioStream;
        }

        public List<SoundTrigger.KeyphraseRecognitionExtra> getKeyphraseRecognitionExtras() {
            return this.mKephraseExtras;
        }

        /* loaded from: classes3.dex */
        public static final class Builder {
            private boolean mCaptureAvailable = false;
            private int mCaptureSession = -1;
            private AudioFormat mAudioFormat = null;
            private int mDataFormat = 0;
            private byte[] mData = null;
            private HotwordDetectedResult mHotwordDetectedResult = null;
            private ParcelFileDescriptor mAudioStream = null;
            private List<SoundTrigger.KeyphraseRecognitionExtra> mKeyphraseExtras = Collections.emptyList();

            public Builder() {
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public Builder(SoundTrigger.KeyphraseRecognitionEvent keyphraseRecognitionEvent) {
                setCaptureAvailable(keyphraseRecognitionEvent.isCaptureAvailable());
                setCaptureSession(keyphraseRecognitionEvent.getCaptureSession());
                if (keyphraseRecognitionEvent.getCaptureFormat() != null) {
                    setCaptureAudioFormat(keyphraseRecognitionEvent.getCaptureFormat());
                }
                setDataFormat(keyphraseRecognitionEvent.triggerInData ? 1 : 0);
                if (keyphraseRecognitionEvent.getData() != null) {
                    setData(keyphraseRecognitionEvent.getData());
                }
                if (keyphraseRecognitionEvent.keyphraseExtras != null) {
                    setKeyphraseRecognitionExtras(Arrays.asList(keyphraseRecognitionEvent.keyphraseExtras));
                }
            }

            public Builder setCaptureAvailable(boolean captureAvailable) {
                this.mCaptureAvailable = captureAvailable;
                return this;
            }

            public Builder setCaptureSession(int captureSession) {
                this.mCaptureSession = captureSession;
                return this;
            }

            public Builder setCaptureAudioFormat(AudioFormat audioFormat) {
                this.mAudioFormat = audioFormat;
                return this;
            }

            public Builder setDataFormat(int dataFormat) {
                this.mDataFormat = dataFormat;
                return this;
            }

            public Builder setData(byte[] data) {
                this.mData = data;
                return this;
            }

            public Builder setHotwordDetectedResult(HotwordDetectedResult hotwordDetectedResult) {
                this.mHotwordDetectedResult = hotwordDetectedResult;
                return this;
            }

            public Builder setAudioStream(ParcelFileDescriptor audioStream) {
                this.mAudioStream = audioStream;
                return this;
            }

            public Builder setKeyphraseRecognitionExtras(List<SoundTrigger.KeyphraseRecognitionExtra> keyphraseRecognitionExtras) {
                this.mKeyphraseExtras = keyphraseRecognitionExtras;
                return this;
            }

            public EventPayload build() {
                return new EventPayload(this.mCaptureAvailable, this.mAudioFormat, this.mCaptureSession, this.mDataFormat, this.mData, this.mHotwordDetectedResult, this.mAudioStream, this.mKeyphraseExtras);
            }
        }
    }

    /* loaded from: classes3.dex */
    public static abstract class Callback implements HotwordDetector.Callback {
        public abstract void onAvailabilityChanged(int i);

        @Override // android.service.voice.HotwordDetector.Callback
        public abstract void onDetected(EventPayload eventPayload);

        @Override // android.service.voice.HotwordDetector.Callback
        public abstract void onError();

        @Override // android.service.voice.HotwordDetector.Callback
        public abstract void onRecognitionPaused();

        @Override // android.service.voice.HotwordDetector.Callback
        public abstract void onRecognitionResumed();

        @Override // android.service.voice.HotwordDetector.Callback
        public void onRejected(HotwordRejectedResult result) {
        }

        @Override // android.service.voice.HotwordDetector.Callback
        public void onHotwordDetectionServiceInitialized(int status) {
        }

        @Override // android.service.voice.HotwordDetector.Callback
        public void onHotwordDetectionServiceRestarted() {
        }
    }

    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public AlwaysOnHotwordDetector(String text, Locale locale, Callback callback, KeyphraseEnrollmentInfo keyphraseEnrollmentInfo, IVoiceInteractionManagerService modelManagementService, int targetSdkVersion, boolean supportHotwordDetectionService, PersistableBundle options, SharedMemory sharedMemory) {
        super(modelManagementService, callback, r2);
        int i;
        if (supportHotwordDetectionService) {
            i = 1;
        } else {
            i = 0;
        }
        Binder binder = new Binder();
        this.mBinder = binder;
        this.mAvailability = 0;
        MyHandler myHandler = new MyHandler();
        this.mHandler = myHandler;
        this.mText = text;
        this.mLocale = locale;
        this.mKeyphraseEnrollmentInfo = keyphraseEnrollmentInfo;
        this.mExternalCallback = callback;
        SoundTriggerListener soundTriggerListener = new SoundTriggerListener(myHandler);
        this.mInternalCallback = soundTriggerListener;
        this.mModelManagementService = modelManagementService;
        this.mTargetSdkVersion = targetSdkVersion;
        this.mSupportHotwordDetectionService = supportHotwordDetectionService;
        if (supportHotwordDetectionService) {
            updateStateLocked(options, sharedMemory, soundTriggerListener, 1);
        }
        try {
            Identity identity = new Identity();
            identity.packageName = ActivityThread.currentOpPackageName();
            this.mSoundTriggerSession = modelManagementService.createSoundTriggerSessionAsOriginator(identity, binder);
            new RefreshAvailabiltyTask().execute(new Void[0]);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    @Override // android.service.voice.AbstractHotwordDetector, android.service.voice.HotwordDetector
    public final void updateState(PersistableBundle options, SharedMemory sharedMemory) {
        synchronized (this.mLock) {
            if (!this.mSupportHotwordDetectionService) {
                throw new IllegalStateException("updateState called, but it doesn't support hotword detection service");
            }
            int i = this.mAvailability;
            if (i == -3 || i == 3) {
                throw new IllegalStateException("updateState called on an invalid detector or error state");
            }
        }
        super.updateState(options, sharedMemory);
    }

    public void triggerHardwareRecognitionEventForTest(int status, int soundModelHandle, boolean captureAvailable, int captureSession, int captureDelayMs, int capturePreambleMs, boolean triggerInData, AudioFormat captureFormat, byte[] data, List<SoundTrigger.KeyphraseRecognitionExtra> keyphraseRecognitionExtras) {
        Log.d(TAG, "triggerHardwareRecognitionEventForTest()");
        synchronized (this.mLock) {
            try {
                try {
                    int i = this.mAvailability;
                    if (i == -3 || i == 3) {
                        throw new IllegalStateException("triggerHardwareRecognitionEventForTest called on an invalid detector or error state");
                    }
                    try {
                        try {
                            this.mModelManagementService.triggerHardwareRecognitionEventForTest(new SoundTrigger.KeyphraseRecognitionEvent(status, soundModelHandle, captureAvailable, captureSession, captureDelayMs, capturePreambleMs, triggerInData, captureFormat, data, (SoundTrigger.KeyphraseRecognitionExtra[]) keyphraseRecognitionExtras.toArray(new SoundTrigger.KeyphraseRecognitionExtra[0])), this.mInternalCallback);
                        } catch (RemoteException e) {
                            e = e;
                            throw e.rethrowFromSystemServer();
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                    }
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public int getSupportedRecognitionModes() {
        int supportedRecognitionModesLocked;
        synchronized (this.mLock) {
            supportedRecognitionModesLocked = getSupportedRecognitionModesLocked();
        }
        return supportedRecognitionModesLocked;
    }

    private int getSupportedRecognitionModesLocked() {
        KeyphraseMetadata keyphraseMetadata;
        int i = this.mAvailability;
        if (i == -3 || i == 3) {
            throw new IllegalStateException("getSupportedRecognitionModes called on an invalid detector or error state");
        }
        if (i != 2 || (keyphraseMetadata = this.mKeyphraseMetadata) == null) {
            throw new UnsupportedOperationException("Getting supported recognition modes for the keyphrase is not supported");
        }
        return keyphraseMetadata.getRecognitionModeFlags();
    }

    public int getSupportedAudioCapabilities() {
        int supportedAudioCapabilitiesLocked;
        synchronized (this.mLock) {
            supportedAudioCapabilitiesLocked = getSupportedAudioCapabilitiesLocked();
        }
        return supportedAudioCapabilitiesLocked;
    }

    private int getSupportedAudioCapabilitiesLocked() {
        try {
            SoundTrigger.ModuleProperties properties = this.mSoundTriggerSession.getDspModuleProperties();
            if (properties != null) {
                return properties.getAudioCapabilities();
            }
            return 0;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean startRecognition(int recognitionFlags) {
        boolean z;
        synchronized (this.mLock) {
            int i = this.mAvailability;
            if (i == -3 || i == 3) {
                throw new IllegalStateException("startRecognition called on an invalid detector or error state");
            }
            if (i != 2) {
                throw new UnsupportedOperationException("Recognition for the given keyphrase is not supported");
            }
            z = startRecognitionLocked(recognitionFlags) == 0;
        }
        return z;
    }

    @Override // android.service.voice.HotwordDetector
    public boolean startRecognition() {
        return startRecognition(0);
    }

    @Override // android.service.voice.HotwordDetector
    public boolean stopRecognition() {
        boolean z;
        synchronized (this.mLock) {
            int i = this.mAvailability;
            if (i == -3 || i == 3) {
                throw new IllegalStateException("stopRecognition called on an invalid detector or error state");
            }
            if (i != 2) {
                throw new UnsupportedOperationException("Recognition for the given keyphrase is not supported");
            }
            z = stopRecognitionLocked() == 0;
        }
        return z;
    }

    public int setParameter(int modelParam, int value) {
        int parameterLocked;
        synchronized (this.mLock) {
            int i = this.mAvailability;
            if (i == -3 || i == 3) {
                throw new IllegalStateException("setParameter called on an invalid detector or error state");
            }
            parameterLocked = setParameterLocked(modelParam, value);
        }
        return parameterLocked;
    }

    public int getParameter(int modelParam) {
        int parameterLocked;
        synchronized (this.mLock) {
            int i = this.mAvailability;
            if (i == -3 || i == 3) {
                throw new IllegalStateException("getParameter called on an invalid detector or error state");
            }
            parameterLocked = getParameterLocked(modelParam);
        }
        return parameterLocked;
    }

    public ModelParamRange queryParameter(int modelParam) {
        ModelParamRange queryParameterLocked;
        synchronized (this.mLock) {
            int i = this.mAvailability;
            if (i == -3 || i == 3) {
                throw new IllegalStateException("queryParameter called on an invalid detector or error state");
            }
            queryParameterLocked = queryParameterLocked(modelParam);
        }
        return queryParameterLocked;
    }

    public Intent createEnrollIntent() {
        Intent manageIntentLocked;
        synchronized (this.mLock) {
            manageIntentLocked = getManageIntentLocked(0);
        }
        return manageIntentLocked;
    }

    public Intent createUnEnrollIntent() {
        Intent manageIntentLocked;
        synchronized (this.mLock) {
            manageIntentLocked = getManageIntentLocked(2);
        }
        return manageIntentLocked;
    }

    public Intent createReEnrollIntent() {
        Intent manageIntentLocked;
        synchronized (this.mLock) {
            manageIntentLocked = getManageIntentLocked(1);
        }
        return manageIntentLocked;
    }

    private Intent getManageIntentLocked(int action) {
        int i = this.mAvailability;
        if (i == -3 || i == 3) {
            throw new IllegalStateException("getManageIntent called on an invalid detector or error state");
        }
        if (i != 2 && i != 1) {
            throw new UnsupportedOperationException("Managing the given keyphrase is not supported");
        }
        return this.mKeyphraseEnrollmentInfo.getManageKeyphraseIntent(action, this.mText, this.mLocale);
    }

    @Override // android.service.voice.AbstractHotwordDetector, android.service.voice.HotwordDetector
    public void destroy() {
        synchronized (this.mLock) {
            if (this.mAvailability == 2) {
                stopRecognition();
            }
            this.mAvailability = -3;
            notifyStateChangedLocked();
            if (this.mSupportHotwordDetectionService) {
                try {
                    this.mModelManagementService.shutdownHotwordDetectionService();
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
        super.destroy();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSoundModelsChanged() {
        synchronized (this.mLock) {
            int i = this.mAvailability;
            if (i == -3 || i == -2 || i == 3) {
                Slog.w(TAG, "Received onSoundModelsChanged for an unsupported keyphrase/config or in the error state");
                return;
            }
            if (i == 2) {
                try {
                    stopRecognitionLocked();
                } catch (SecurityException e) {
                    Slog.w(TAG, "Failed to Stop the recognition", e);
                    if (this.mTargetSdkVersion <= 30) {
                        throw e;
                    }
                    updateAndNotifyStateChangedLocked(3);
                    return;
                }
            }
            new RefreshAvailabiltyTask().execute(new Void[0]);
        }
    }

    private int startRecognitionLocked(int recognitionFlags) {
        SoundTrigger.KeyphraseRecognitionExtra[] recognitionExtra = {new SoundTrigger.KeyphraseRecognitionExtra(this.mKeyphraseMetadata.getId(), this.mKeyphraseMetadata.getRecognitionModeFlags(), 0, new SoundTrigger.ConfidenceLevel[0])};
        boolean captureTriggerAudio = (recognitionFlags & 1) != 0;
        boolean allowMultipleTriggers = (recognitionFlags & 2) != 0;
        boolean runInBatterySaver = (recognitionFlags & 16) != 0;
        int audioCapabilities = 0;
        if ((recognitionFlags & 4) != 0) {
            audioCapabilities = 0 | 1;
        }
        if ((recognitionFlags & 8) != 0) {
            audioCapabilities |= 2;
        }
        try {
            int code = this.mSoundTriggerSession.startRecognition(this.mKeyphraseMetadata.getId(), this.mLocale.toLanguageTag(), this.mInternalCallback, new SoundTrigger.RecognitionConfig(captureTriggerAudio, allowMultipleTriggers, recognitionExtra, null, audioCapabilities), runInBatterySaver);
            if (code != 0) {
                Slog.w(TAG, "startRecognition() failed with error code " + code);
            }
            return code;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private int stopRecognitionLocked() {
        try {
            int code = this.mSoundTriggerSession.stopRecognition(this.mKeyphraseMetadata.getId(), this.mInternalCallback);
            if (code != 0) {
                Slog.w(TAG, "stopRecognition() failed with error code " + code);
            }
            return code;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private int setParameterLocked(int modelParam, int value) {
        try {
            int code = this.mSoundTriggerSession.setParameter(this.mKeyphraseMetadata.getId(), modelParam, value);
            if (code != 0) {
                Slog.w(TAG, "setParameter failed with error code " + code);
            }
            return code;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private int getParameterLocked(int modelParam) {
        try {
            return this.mSoundTriggerSession.getParameter(this.mKeyphraseMetadata.getId(), modelParam);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private ModelParamRange queryParameterLocked(int modelParam) {
        try {
            SoundTrigger.ModelParamRange modelParamRange = this.mSoundTriggerSession.queryParameter(this.mKeyphraseMetadata.getId(), modelParam);
            if (modelParamRange == null) {
                return null;
            }
            return new ModelParamRange(modelParamRange);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAndNotifyStateChangedLocked(int availability) {
        this.mAvailability = availability;
        notifyStateChangedLocked();
    }

    private void notifyStateChangedLocked() {
        Message message = Message.obtain(this.mHandler, 1);
        message.arg1 = this.mAvailability;
        message.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes3.dex */
    public static final class SoundTriggerListener extends IHotwordRecognitionStatusCallback.Stub {
        private final Handler mHandler;

        public SoundTriggerListener(Handler handler) {
            this.mHandler = handler;
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onKeyphraseDetected(SoundTrigger.KeyphraseRecognitionEvent event, HotwordDetectedResult result) {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onDetected");
            Message.obtain(this.mHandler, 2, new EventPayload.Builder(event).setHotwordDetectedResult(result).build()).sendToTarget();
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onGenericSoundTriggerDetected(SoundTrigger.GenericRecognitionEvent event) {
            Slog.w(AlwaysOnHotwordDetector.TAG, "Generic sound trigger event detected at AOHD: " + event);
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onRejected(HotwordRejectedResult result) {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onRejected");
            Message.obtain(this.mHandler, 6, result).sendToTarget();
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onError(int status) {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onError: " + status);
            this.mHandler.sendEmptyMessage(3);
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onRecognitionPaused() {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onRecognitionPaused");
            this.mHandler.sendEmptyMessage(4);
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onRecognitionResumed() {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onRecognitionResumed");
            this.mHandler.sendEmptyMessage(5);
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onStatusReported(int status) {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onStatusReported");
            Message message = Message.obtain(this.mHandler, 7);
            message.arg1 = status;
            message.sendToTarget();
        }

        @Override // com.android.internal.app.IHotwordRecognitionStatusCallback
        public void onProcessRestarted() {
            Slog.i(AlwaysOnHotwordDetector.TAG, "onProcessRestarted");
            this.mHandler.sendEmptyMessage(8);
        }
    }

    /* loaded from: classes3.dex */
    class MyHandler extends Handler {
        MyHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            synchronized (AlwaysOnHotwordDetector.this.mLock) {
                if (AlwaysOnHotwordDetector.this.mAvailability == -3) {
                    Slog.w(AlwaysOnHotwordDetector.TAG, "Received message: " + msg.what + " for an invalid detector");
                    return;
                }
                switch (msg.what) {
                    case 1:
                        AlwaysOnHotwordDetector.this.mExternalCallback.onAvailabilityChanged(msg.arg1);
                        return;
                    case 2:
                        AlwaysOnHotwordDetector.this.mExternalCallback.onDetected((EventPayload) msg.obj);
                        return;
                    case 3:
                        AlwaysOnHotwordDetector.this.mExternalCallback.onError();
                        return;
                    case 4:
                        AlwaysOnHotwordDetector.this.mExternalCallback.onRecognitionPaused();
                        return;
                    case 5:
                        AlwaysOnHotwordDetector.this.mExternalCallback.onRecognitionResumed();
                        return;
                    case 6:
                        AlwaysOnHotwordDetector.this.mExternalCallback.onRejected((HotwordRejectedResult) msg.obj);
                        return;
                    case 7:
                        AlwaysOnHotwordDetector.this.mExternalCallback.onHotwordDetectionServiceInitialized(msg.arg1);
                        return;
                    case 8:
                        AlwaysOnHotwordDetector.this.mExternalCallback.onHotwordDetectionServiceRestarted();
                        return;
                    default:
                        super.handleMessage(msg);
                        return;
                }
            }
        }
    }

    /* loaded from: classes3.dex */
    class RefreshAvailabiltyTask extends AsyncTask<Void, Void, Void> {
        RefreshAvailabiltyTask() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.os.AsyncTask
        public Void doInBackground(Void... params) {
            try {
                int availability = internalGetInitialAvailability();
                synchronized (AlwaysOnHotwordDetector.this.mLock) {
                    if (availability == 0) {
                        internalUpdateEnrolledKeyphraseMetadata();
                        if (AlwaysOnHotwordDetector.this.mKeyphraseMetadata != null) {
                            availability = 2;
                        } else {
                            availability = 1;
                        }
                    }
                    AlwaysOnHotwordDetector.this.updateAndNotifyStateChangedLocked(availability);
                }
                return null;
            } catch (SecurityException e) {
                Slog.w(AlwaysOnHotwordDetector.TAG, "Failed to refresh availability", e);
                if (AlwaysOnHotwordDetector.this.mTargetSdkVersion <= 30) {
                    throw e;
                }
                synchronized (AlwaysOnHotwordDetector.this.mLock) {
                    AlwaysOnHotwordDetector.this.updateAndNotifyStateChangedLocked(3);
                    return null;
                }
            }
        }

        private int internalGetInitialAvailability() {
            synchronized (AlwaysOnHotwordDetector.this.mLock) {
                if (AlwaysOnHotwordDetector.this.mAvailability == -3) {
                    return -3;
                }
                try {
                    SoundTrigger.ModuleProperties dspModuleProperties = AlwaysOnHotwordDetector.this.mSoundTriggerSession.getDspModuleProperties();
                    if (dspModuleProperties == null) {
                        return -2;
                    }
                    return 0;
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }

        private void internalUpdateEnrolledKeyphraseMetadata() {
            try {
                AlwaysOnHotwordDetector alwaysOnHotwordDetector = AlwaysOnHotwordDetector.this;
                alwaysOnHotwordDetector.mKeyphraseMetadata = alwaysOnHotwordDetector.mModelManagementService.getEnrolledKeyphraseMetadata(AlwaysOnHotwordDetector.this.mText, AlwaysOnHotwordDetector.this.mLocale.toLanguageTag());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        synchronized (this.mLock) {
            pw.print(prefix);
            pw.print("Text=");
            pw.println(this.mText);
            pw.print(prefix);
            pw.print("Locale=");
            pw.println(this.mLocale);
            pw.print(prefix);
            pw.print("Availability=");
            pw.println(this.mAvailability);
            pw.print(prefix);
            pw.print("KeyphraseMetadata=");
            pw.println(this.mKeyphraseMetadata);
            pw.print(prefix);
            pw.print("EnrollmentInfo=");
            pw.println(this.mKeyphraseEnrollmentInfo);
        }
    }
}
