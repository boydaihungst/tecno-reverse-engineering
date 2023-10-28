package com.android.server.voiceinteraction;

import android.content.ComponentName;
import android.content.ContentCaptureOptions;
import android.content.Context;
import android.content.Intent;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.AudioFormat;
import android.media.AudioManagerInternal;
import android.media.permission.Identity;
import android.media.permission.PermissionUtil;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SharedMemory;
import android.provider.DeviceConfig;
import android.service.voice.HotwordDetectedResult;
import android.service.voice.HotwordDetectionService;
import android.service.voice.HotwordDetector;
import android.service.voice.HotwordRejectedResult;
import android.service.voice.IDspHotwordDetectionCallback;
import android.service.voice.IHotwordDetectionService;
import android.service.voice.IMicrophoneHotwordDetectionVoiceInteractionCallback;
import android.service.voice.VoiceInteractionManagerInternal;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.view.contentcapture.IContentCaptureManager;
import com.android.internal.app.IHotwordRecognitionStatusCallback;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.infra.ServiceConnector;
import com.android.internal.util.FunctionalUtils;
import com.android.server.LocalServices;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.voiceinteraction.HotwordDetectionConnection;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class HotwordDetectionConnection {
    private static final int CALLBACK_ONDETECTED_GOT_SECURITY_EXCEPTION = -2;
    static final boolean DEBUG = false;
    private static final long EXTERNAL_HOTWORD_CLEANUP_MILLIS = 2000;
    private static final int HOTWORD_DETECTION_SERVICE_DIED = -1;
    private static final String KEY_RESTART_PERIOD_IN_SECONDS = "restart_period_in_seconds";
    private static final int MAX_ISOLATED_PROCESS_NUMBER = 10;
    private static final Duration MAX_UPDATE_TIMEOUT_DURATION = Duration.ofMillis(30000);
    private static final long MAX_UPDATE_TIMEOUT_MILLIS = 30000;
    private static final int METRICS_INIT_CALLBACK_STATE_ERROR = 1;
    private static final int METRICS_INIT_CALLBACK_STATE_SUCCESS = 0;
    private static final int METRICS_INIT_UNKNOWN_NO_VALUE = 2;
    private static final int METRICS_INIT_UNKNOWN_OVER_MAX_CUSTOM_VALUE = 3;
    private static final int METRICS_INIT_UNKNOWN_TIMEOUT = 4;
    private static final int METRICS_KEYPHRASE_TRIGGERED_DETECT_SECURITY_EXCEPTION = 8;
    private static final int METRICS_KEYPHRASE_TRIGGERED_DETECT_UNEXPECTED_CALLBACK = 7;
    private static final int METRICS_KEYPHRASE_TRIGGERED_REJECT_UNEXPECTED_CALLBACK = 9;
    private static final String OP_MESSAGE = "Providing hotword detection result to VoiceInteractionService";
    private static final long RESET_DEBUG_HOTWORD_LOGGING_TIMEOUT_MILLIS = 3600000;
    private static final String TAG = "HotwordDetectionConnection";
    private static final long VALIDATION_TIMEOUT_MILLIS = 4000;
    private final Executor mAudioCopyExecutor = Executors.newCachedThreadPool();
    private IBinder mAudioFlinger;
    private final IBinder.DeathRecipient mAudioServerDeathRecipient;
    private final IHotwordRecognitionStatusCallback mCallback;
    private ScheduledFuture<?> mCancellationKeyPhraseDetectionFuture;
    private final ScheduledFuture<?> mCancellationTaskFuture;
    final Context mContext;
    private ParcelFileDescriptor mCurrentAudioSink;
    private boolean mDebugHotwordLogging;
    private ScheduledFuture<?> mDebugHotwordLoggingTimeoutFuture;
    final ComponentName mDetectionComponentName;
    private final int mDetectorType;
    volatile VoiceInteractionManagerInternal.HotwordDetectionServiceIdentity mIdentity;
    private Instant mLastRestartInstant;
    final Object mLock;
    private boolean mPerformingSoftwareHotwordDetection;
    private final int mReStartPeriodSeconds;
    private ServiceConnection mRemoteHotwordDetectionService;
    private final ScheduledExecutorService mScheduledExecutorService;
    private final ServiceConnectionFactory mServiceConnectionFactory;
    private IMicrophoneHotwordDetectionVoiceInteractionCallback mSoftwareCallback;
    private final AtomicBoolean mUpdateStateAfterStartFinished;
    final int mUser;
    private boolean mValidatingDspTrigger;
    final int mVoiceInteractionServiceUid;
    private final Identity mVoiceInteractorIdentity;

    /* JADX INFO: Access modifiers changed from: package-private */
    public HotwordDetectionConnection(Object lock, Context context, int voiceInteractionServiceUid, Identity voiceInteractorIdentity, ComponentName serviceName, int userId, boolean bindInstantServiceAllowed, PersistableBundle options, SharedMemory sharedMemory, IHotwordRecognitionStatusCallback callback, int detectorType) {
        ScheduledExecutorService newSingleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.mScheduledExecutorService = newSingleThreadScheduledExecutor;
        this.mUpdateStateAfterStartFinished = new AtomicBoolean(false);
        this.mAudioServerDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda12
            @Override // android.os.IBinder.DeathRecipient
            public final void binderDied() {
                HotwordDetectionConnection.this.audioServerDied();
            }
        };
        this.mDebugHotwordLoggingTimeoutFuture = null;
        this.mValidatingDspTrigger = false;
        this.mDebugHotwordLogging = false;
        if (callback != null) {
            this.mLock = lock;
            this.mContext = context;
            this.mVoiceInteractionServiceUid = voiceInteractionServiceUid;
            this.mVoiceInteractorIdentity = voiceInteractorIdentity;
            this.mDetectionComponentName = serviceName;
            this.mUser = userId;
            this.mCallback = callback;
            this.mDetectorType = detectorType;
            int i = DeviceConfig.getInt("voice_interaction", KEY_RESTART_PERIOD_IN_SECONDS, 0);
            this.mReStartPeriodSeconds = i;
            Intent intent = new Intent("android.service.voice.HotwordDetectionService");
            intent.setComponent(serviceName);
            initAudioFlingerLocked();
            ServiceConnectionFactory serviceConnectionFactory = new ServiceConnectionFactory(intent, bindInstantServiceAllowed);
            this.mServiceConnectionFactory = serviceConnectionFactory;
            this.mRemoteHotwordDetectionService = serviceConnectionFactory.createLocked();
            this.mLastRestartInstant = Instant.now();
            updateStateAfterProcessStart(options, sharedMemory);
            if (i > 0) {
                this.mCancellationTaskFuture = newSingleThreadScheduledExecutor.scheduleAtFixedRate(new Runnable() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda13
                    @Override // java.lang.Runnable
                    public final void run() {
                        HotwordDetectionConnection.this.m7583xa3b395ee();
                    }
                }, i, i, TimeUnit.SECONDS);
                return;
            } else {
                this.mCancellationTaskFuture = null;
                return;
            }
        }
        Slog.w(TAG, "Callback is null while creating connection");
        throw new IllegalArgumentException("Callback is null while creating connection");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-voiceinteraction-HotwordDetectionConnection  reason: not valid java name */
    public /* synthetic */ void m7583xa3b395ee() {
        Slog.v(TAG, "Time to restart the process, TTL has passed");
        synchronized (this.mLock) {
            restartProcessLocked();
            HotwordMetricsLogger.writeServiceRestartEvent(this.mDetectorType, 2);
        }
    }

    private void initAudioFlingerLocked() {
        IBinder waitForService = ServiceManager.waitForService("media.audio_flinger");
        this.mAudioFlinger = waitForService;
        if (waitForService == null) {
            throw new IllegalStateException("Service media.audio_flinger wasn't found.");
        }
        try {
            waitForService.linkToDeath(this.mAudioServerDeathRecipient, 0);
        } catch (RemoteException e) {
            Slog.w(TAG, "Audio server died before we registered a DeathRecipient; retrying init.", e);
            initAudioFlingerLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void audioServerDied() {
        Slog.w(TAG, "Audio server died; restarting the HotwordDetectionService.");
        synchronized (this.mLock) {
            initAudioFlingerLocked();
            restartProcessLocked();
            HotwordMetricsLogger.writeServiceRestartEvent(this.mDetectorType, 1);
        }
    }

    private void updateStateAfterProcessStart(final PersistableBundle options, final SharedMemory sharedMemory) {
        this.mRemoteHotwordDetectionService.postAsync(new ServiceConnector.Job() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda15
            public final Object run(Object obj) {
                return HotwordDetectionConnection.this.m7586x976676ac(options, sharedMemory, (IHotwordDetectionService) obj);
            }
        }).whenComplete(new BiConsumer() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda16
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                HotwordDetectionConnection.this.m7587x15c77a8b((Void) obj, (Throwable) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateStateAfterProcessStart$1$com-android-server-voiceinteraction-HotwordDetectionConnection  reason: not valid java name */
    public /* synthetic */ CompletableFuture m7586x976676ac(PersistableBundle options, SharedMemory sharedMemory, IHotwordDetectionService service) throws Exception {
        final AndroidFuture<Void> future = new AndroidFuture<>();
        try {
            service.updateState(options, sharedMemory, new IRemoteCallback.Stub() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection.1
                public void sendResult(Bundle bundle) throws RemoteException {
                    future.complete((Object) null);
                    if (HotwordDetectionConnection.this.mUpdateStateAfterStartFinished.getAndSet(true)) {
                        Slog.w(HotwordDetectionConnection.TAG, "call callback after timeout");
                        HotwordMetricsLogger.writeDetectorEvent(HotwordDetectionConnection.this.mDetectorType, 5, HotwordDetectionConnection.this.mVoiceInteractionServiceUid);
                        return;
                    }
                    Pair statusResultPair = HotwordDetectionConnection.getInitStatusAndMetricsResult(bundle);
                    int status = ((Integer) statusResultPair.first).intValue();
                    int initResultMetricsResult = ((Integer) statusResultPair.second).intValue();
                    try {
                        HotwordDetectionConnection.this.mCallback.onStatusReported(status);
                        HotwordMetricsLogger.writeServiceInitResultEvent(HotwordDetectionConnection.this.mDetectorType, initResultMetricsResult);
                    } catch (RemoteException e) {
                        Slog.w(HotwordDetectionConnection.TAG, "Failed to report initialization status: " + e);
                        HotwordMetricsLogger.writeServiceInitResultEvent(HotwordDetectionConnection.this.mDetectorType, 1);
                    }
                }
            });
            HotwordMetricsLogger.writeDetectorEvent(this.mDetectorType, 4, this.mVoiceInteractionServiceUid);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to updateState for HotwordDetectionService", e);
        }
        return future.orTimeout(30000L, TimeUnit.MILLISECONDS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateStateAfterProcessStart$2$com-android-server-voiceinteraction-HotwordDetectionConnection  reason: not valid java name */
    public /* synthetic */ void m7587x15c77a8b(Void res, Throwable err) {
        if (err instanceof TimeoutException) {
            Slog.w(TAG, "updateState timed out");
            if (this.mUpdateStateAfterStartFinished.getAndSet(true)) {
                return;
            }
            try {
                this.mCallback.onStatusReported(100);
                HotwordMetricsLogger.writeServiceInitResultEvent(this.mDetectorType, 4);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to report initialization status UNKNOWN", e);
                HotwordMetricsLogger.writeServiceInitResultEvent(this.mDetectorType, 1);
            }
        } else if (err != null) {
            Slog.w(TAG, "Failed to update state: " + err);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Pair<Integer, Integer> getInitStatusAndMetricsResult(Bundle bundle) {
        int metricsResult;
        int i = 2;
        if (bundle == null) {
            return new Pair<>(100, 2);
        }
        int status = bundle.getInt("initialization_status", 100);
        if (status > HotwordDetectionService.getMaxCustomInitializationStatus()) {
            if (status != 100) {
                i = 3;
            }
            return new Pair<>(100, Integer.valueOf(i));
        }
        if (status == 0) {
            metricsResult = 0;
        } else {
            metricsResult = 1;
        }
        return new Pair<>(Integer.valueOf(status), Integer.valueOf(metricsResult));
    }

    private boolean isBound() {
        boolean isBound;
        synchronized (this.mLock) {
            isBound = this.mRemoteHotwordDetectionService.isBound();
        }
        return isBound;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelLocked() {
        Slog.v(TAG, "cancelLocked");
        clearDebugHotwordLoggingTimeoutLocked();
        this.mDebugHotwordLogging = false;
        this.mRemoteHotwordDetectionService.unbind();
        ((PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class)).setHotwordDetectionServiceProvider(null);
        if (this.mIdentity != null) {
            removeServiceUidForAudioPolicy(this.mIdentity.getIsolatedUid());
        }
        this.mIdentity = null;
        ScheduledFuture<?> scheduledFuture = this.mCancellationTaskFuture;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        IBinder iBinder = this.mAudioFlinger;
        if (iBinder != null) {
            iBinder.unlinkToDeath(this.mAudioServerDeathRecipient, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateStateLocked(final PersistableBundle options, final SharedMemory sharedMemory) {
        if (!this.mUpdateStateAfterStartFinished.get() && Instant.now().minus((TemporalAmount) MAX_UPDATE_TIMEOUT_DURATION).isBefore(this.mLastRestartInstant)) {
            Slog.v(TAG, "call updateStateAfterProcessStart");
            updateStateAfterProcessStart(options, sharedMemory);
            return;
        }
        this.mRemoteHotwordDetectionService.run(new ServiceConnector.VoidJob() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda17
            public final void runNoResult(Object obj) {
                ((IHotwordDetectionService) obj).updateState(options, sharedMemory, (IRemoteCallback) null);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startListeningFromMic(AudioFormat audioFormat, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) {
        this.mSoftwareCallback = callback;
        synchronized (this.mLock) {
            if (this.mPerformingSoftwareHotwordDetection) {
                Slog.i(TAG, "Hotword validation is already in progress, ignoring.");
                return;
            }
            this.mPerformingSoftwareHotwordDetection = true;
            startListeningFromMicLocked();
        }
    }

    private void startListeningFromMicLocked() {
        final IDspHotwordDetectionCallback.Stub stub = new IDspHotwordDetectionCallback.Stub() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection.2
            public void onDetected(HotwordDetectedResult result) throws RemoteException {
                synchronized (HotwordDetectionConnection.this.mLock) {
                    if (!HotwordDetectionConnection.this.mPerformingSoftwareHotwordDetection) {
                        Slog.i(HotwordDetectionConnection.TAG, "Hotword detection has already completed");
                        return;
                    }
                    HotwordDetectionConnection.this.mPerformingSoftwareHotwordDetection = false;
                    try {
                        HotwordDetectionConnection.this.enforcePermissionsForDataDelivery();
                        HotwordDetectionConnection.this.mSoftwareCallback.onDetected(result, (AudioFormat) null, (ParcelFileDescriptor) null);
                        if (result != null) {
                            Slog.i(HotwordDetectionConnection.TAG, "Egressed " + HotwordDetectedResult.getUsageSize(result) + " bits from hotword trusted process");
                            if (HotwordDetectionConnection.this.mDebugHotwordLogging) {
                                Slog.i(HotwordDetectionConnection.TAG, "Egressed detected result: " + result);
                            }
                        }
                    } catch (SecurityException e) {
                        HotwordDetectionConnection.this.mSoftwareCallback.onError();
                    }
                }
            }

            public void onRejected(HotwordRejectedResult result) throws RemoteException {
            }
        };
        this.mRemoteHotwordDetectionService.run(new ServiceConnector.VoidJob() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda14
            public final void runNoResult(Object obj) {
                ((IHotwordDetectionService) obj).detectFromMicrophoneSource((ParcelFileDescriptor) null, 1, (AudioFormat) null, (PersistableBundle) null, stub);
            }
        });
    }

    public void startListeningFromExternalSource(ParcelFileDescriptor audioStream, AudioFormat audioFormat, PersistableBundle options, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) {
        handleExternalSourceHotwordDetection(audioStream, audioFormat, options, callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopListening() {
        synchronized (this.mLock) {
            stopListeningLocked();
        }
    }

    private void stopListeningLocked() {
        if (!this.mPerformingSoftwareHotwordDetection) {
            Slog.i(TAG, "Hotword detection is not running");
            return;
        }
        this.mPerformingSoftwareHotwordDetection = false;
        this.mRemoteHotwordDetectionService.run(new ServiceConnector.VoidJob() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda9
            public final void runNoResult(Object obj) {
                ((IHotwordDetectionService) obj).stopDetection();
            }
        });
        if (this.mCurrentAudioSink != null) {
            Slog.i(TAG, "Closing audio stream to hotword detector: stopping requested");
            bestEffortClose(this.mCurrentAudioSink);
        }
        this.mCurrentAudioSink = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void triggerHardwareRecognitionEventForTestLocked(SoundTrigger.KeyphraseRecognitionEvent event, IHotwordRecognitionStatusCallback callback) {
        detectFromDspSourceForTest(event, callback);
    }

    private void detectFromDspSourceForTest(final SoundTrigger.KeyphraseRecognitionEvent recognitionEvent, final IHotwordRecognitionStatusCallback externalCallback) {
        Slog.v(TAG, "detectFromDspSourceForTest");
        final IDspHotwordDetectionCallback.Stub stub = new IDspHotwordDetectionCallback.Stub() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection.3
            public void onDetected(HotwordDetectedResult result) throws RemoteException {
                Slog.v(HotwordDetectionConnection.TAG, "onDetected");
                synchronized (HotwordDetectionConnection.this.mLock) {
                    if (!HotwordDetectionConnection.this.mValidatingDspTrigger) {
                        Slog.i(HotwordDetectionConnection.TAG, "Ignored hotword detected since trigger has been handled");
                        return;
                    }
                    HotwordDetectionConnection.this.mValidatingDspTrigger = false;
                    try {
                        HotwordDetectionConnection.this.enforcePermissionsForDataDelivery();
                        externalCallback.onKeyphraseDetected(recognitionEvent, result);
                        if (result != null) {
                            Slog.i(HotwordDetectionConnection.TAG, "Egressed " + HotwordDetectedResult.getUsageSize(result) + " bits from hotword trusted process");
                            if (HotwordDetectionConnection.this.mDebugHotwordLogging) {
                                Slog.i(HotwordDetectionConnection.TAG, "Egressed detected result: " + result);
                            }
                        }
                    } catch (SecurityException e) {
                        externalCallback.onError(-2);
                    }
                }
            }

            public void onRejected(HotwordRejectedResult result) throws RemoteException {
                Slog.v(HotwordDetectionConnection.TAG, "onRejected");
                synchronized (HotwordDetectionConnection.this.mLock) {
                    if (HotwordDetectionConnection.this.mValidatingDspTrigger) {
                        HotwordDetectionConnection.this.mValidatingDspTrigger = false;
                        externalCallback.onRejected(result);
                        if (HotwordDetectionConnection.this.mDebugHotwordLogging && result != null) {
                            Slog.i(HotwordDetectionConnection.TAG, "Egressed rejected result: " + result);
                        }
                    } else {
                        Slog.i(HotwordDetectionConnection.TAG, "Ignored hotword rejected since trigger has been handled");
                    }
                }
            }
        };
        synchronized (this.mLock) {
            this.mValidatingDspTrigger = true;
            this.mRemoteHotwordDetectionService.run(new ServiceConnector.VoidJob() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda6
                public final void runNoResult(Object obj) {
                    ((IHotwordDetectionService) obj).detectFromDspSource(r0, recognitionEvent.getCaptureFormat(), (long) HotwordDetectionConnection.VALIDATION_TIMEOUT_MILLIS, stub);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void detectFromDspSource(final SoundTrigger.KeyphraseRecognitionEvent recognitionEvent, final IHotwordRecognitionStatusCallback externalCallback) {
        final IDspHotwordDetectionCallback.Stub stub = new IDspHotwordDetectionCallback.Stub() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection.4
            public void onDetected(HotwordDetectedResult result) throws RemoteException {
                synchronized (HotwordDetectionConnection.this.mLock) {
                    if (HotwordDetectionConnection.this.mCancellationKeyPhraseDetectionFuture != null) {
                        HotwordDetectionConnection.this.mCancellationKeyPhraseDetectionFuture.cancel(true);
                    }
                    HotwordMetricsLogger.writeKeyphraseTriggerEvent(HotwordDetectionConnection.this.mDetectorType, 5);
                    if (!HotwordDetectionConnection.this.mValidatingDspTrigger) {
                        Slog.i(HotwordDetectionConnection.TAG, "Ignoring #onDetected due to a process restart");
                        HotwordMetricsLogger.writeKeyphraseTriggerEvent(HotwordDetectionConnection.this.mDetectorType, 7);
                        return;
                    }
                    HotwordDetectionConnection.this.mValidatingDspTrigger = false;
                    try {
                        HotwordDetectionConnection.this.enforcePermissionsForDataDelivery();
                        externalCallback.onKeyphraseDetected(recognitionEvent, result);
                        if (result != null) {
                            Slog.i(HotwordDetectionConnection.TAG, "Egressed " + HotwordDetectedResult.getUsageSize(result) + " bits from hotword trusted process");
                            if (HotwordDetectionConnection.this.mDebugHotwordLogging) {
                                Slog.i(HotwordDetectionConnection.TAG, "Egressed detected result: " + result);
                            }
                        }
                    } catch (SecurityException e) {
                        HotwordMetricsLogger.writeKeyphraseTriggerEvent(HotwordDetectionConnection.this.mDetectorType, 8);
                        externalCallback.onError(-2);
                    }
                }
            }

            public void onRejected(HotwordRejectedResult result) throws RemoteException {
                synchronized (HotwordDetectionConnection.this.mLock) {
                    if (HotwordDetectionConnection.this.mCancellationKeyPhraseDetectionFuture != null) {
                        HotwordDetectionConnection.this.mCancellationKeyPhraseDetectionFuture.cancel(true);
                    }
                    HotwordMetricsLogger.writeKeyphraseTriggerEvent(HotwordDetectionConnection.this.mDetectorType, 6);
                    if (!HotwordDetectionConnection.this.mValidatingDspTrigger) {
                        Slog.i(HotwordDetectionConnection.TAG, "Ignoring #onRejected due to a process restart");
                        HotwordMetricsLogger.writeKeyphraseTriggerEvent(HotwordDetectionConnection.this.mDetectorType, 9);
                        return;
                    }
                    HotwordDetectionConnection.this.mValidatingDspTrigger = false;
                    externalCallback.onRejected(result);
                    if (HotwordDetectionConnection.this.mDebugHotwordLogging && result != null) {
                        Slog.i(HotwordDetectionConnection.TAG, "Egressed rejected result: " + result);
                    }
                }
            }
        };
        synchronized (this.mLock) {
            this.mValidatingDspTrigger = true;
            this.mRemoteHotwordDetectionService.run(new ServiceConnector.VoidJob() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda10
                public final void runNoResult(Object obj) {
                    HotwordDetectionConnection.this.m7579xad07e7b8(recognitionEvent, stub, (IHotwordDetectionService) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$detectFromDspSource$7$com-android-server-voiceinteraction-HotwordDetectionConnection  reason: not valid java name */
    public /* synthetic */ void m7579xad07e7b8(SoundTrigger.KeyphraseRecognitionEvent recognitionEvent, IDspHotwordDetectionCallback internalCallback, IHotwordDetectionService service) throws Exception {
        this.mCancellationKeyPhraseDetectionFuture = this.mScheduledExecutorService.schedule(new Runnable() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                HotwordDetectionConnection.this.m7578x2ea6e3d9();
            }
        }, VALIDATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        service.detectFromDspSource(recognitionEvent, recognitionEvent.getCaptureFormat(), (long) VALIDATION_TIMEOUT_MILLIS, internalCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$detectFromDspSource$6$com-android-server-voiceinteraction-HotwordDetectionConnection  reason: not valid java name */
    public /* synthetic */ void m7578x2ea6e3d9() {
        HotwordMetricsLogger.writeKeyphraseTriggerEvent(this.mDetectorType, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceRestart() {
        Slog.v(TAG, "Requested to restart the service internally. Performing the restart");
        synchronized (this.mLock) {
            restartProcessLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDebugHotwordLoggingLocked(boolean logging) {
        Slog.v(TAG, "setDebugHotwordLoggingLocked: " + logging);
        clearDebugHotwordLoggingTimeoutLocked();
        this.mDebugHotwordLogging = logging;
        if (logging) {
            this.mDebugHotwordLoggingTimeoutFuture = this.mScheduledExecutorService.schedule(new Runnable() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    HotwordDetectionConnection.this.m7584x715ea7a9();
                }
            }, 3600000L, TimeUnit.MILLISECONDS);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDebugHotwordLoggingLocked$8$com-android-server-voiceinteraction-HotwordDetectionConnection  reason: not valid java name */
    public /* synthetic */ void m7584x715ea7a9() {
        Slog.v(TAG, "Timeout to reset mDebugHotwordLogging to false");
        synchronized (this.mLock) {
            this.mDebugHotwordLogging = false;
        }
    }

    private void clearDebugHotwordLoggingTimeoutLocked() {
        ScheduledFuture<?> scheduledFuture = this.mDebugHotwordLoggingTimeoutFuture;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            this.mDebugHotwordLoggingTimeoutFuture = null;
        }
    }

    private void restartProcessLocked() {
        Slog.v(TAG, "Restarting hotword detection process");
        ServiceConnection oldConnection = this.mRemoteHotwordDetectionService;
        VoiceInteractionManagerInternal.HotwordDetectionServiceIdentity previousIdentity = this.mIdentity;
        if (this.mValidatingDspTrigger) {
            try {
                this.mCallback.onRejected(new HotwordRejectedResult.Builder().build());
                HotwordMetricsLogger.writeKeyphraseTriggerEvent(this.mDetectorType, 10);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to call #rejected");
            }
            this.mValidatingDspTrigger = false;
        }
        this.mUpdateStateAfterStartFinished.set(false);
        this.mLastRestartInstant = Instant.now();
        this.mRemoteHotwordDetectionService = this.mServiceConnectionFactory.createLocked();
        Slog.v(TAG, "Started the new process, issuing #onProcessRestarted");
        try {
            this.mCallback.onProcessRestarted();
        } catch (RemoteException e2) {
            Slog.w(TAG, "Failed to communicate #onProcessRestarted", e2);
        }
        if (this.mPerformingSoftwareHotwordDetection) {
            Slog.i(TAG, "Process restarted: calling startRecognition() again");
            startListeningFromMicLocked();
        }
        if (this.mCurrentAudioSink != null) {
            Slog.i(TAG, "Closing external audio stream to hotword detector: process restarted");
            bestEffortClose(this.mCurrentAudioSink);
            this.mCurrentAudioSink = null;
        }
        oldConnection.ignoreConnectionStatusEvents();
        oldConnection.unbind();
        if (previousIdentity != null) {
            removeServiceUidForAudioPolicy(previousIdentity.getIsolatedUid());
        }
    }

    /* loaded from: classes2.dex */
    static final class SoundTriggerCallback extends IRecognitionStatusCallback.Stub {
        private final IHotwordRecognitionStatusCallback mExternalCallback;
        private final HotwordDetectionConnection mHotwordDetectionConnection;
        private SoundTrigger.KeyphraseRecognitionEvent mRecognitionEvent;

        /* JADX INFO: Access modifiers changed from: package-private */
        public SoundTriggerCallback(IHotwordRecognitionStatusCallback callback, HotwordDetectionConnection connection) {
            this.mHotwordDetectionConnection = connection;
            this.mExternalCallback = callback;
        }

        public void onKeyphraseDetected(SoundTrigger.KeyphraseRecognitionEvent recognitionEvent) throws RemoteException {
            boolean useHotwordDetectionService = this.mHotwordDetectionConnection != null;
            if (useHotwordDetectionService) {
                HotwordMetricsLogger.writeKeyphraseTriggerEvent(1, 0);
                this.mRecognitionEvent = recognitionEvent;
                this.mHotwordDetectionConnection.detectFromDspSource(recognitionEvent, this.mExternalCallback);
                return;
            }
            HotwordMetricsLogger.writeKeyphraseTriggerEvent(0, 0);
            this.mExternalCallback.onKeyphraseDetected(recognitionEvent, (HotwordDetectedResult) null);
        }

        public void onGenericSoundTriggerDetected(SoundTrigger.GenericRecognitionEvent recognitionEvent) throws RemoteException {
            this.mExternalCallback.onGenericSoundTriggerDetected(recognitionEvent);
        }

        public void onError(int status) throws RemoteException {
            this.mExternalCallback.onError(status);
        }

        public void onRecognitionPaused() throws RemoteException {
            this.mExternalCallback.onRecognitionPaused();
        }

        public void onRecognitionResumed() throws RemoteException {
            this.mExternalCallback.onRecognitionResumed();
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mReStartPeriodSeconds=");
        pw.println(this.mReStartPeriodSeconds);
        pw.print(prefix);
        pw.print("mBound=" + this.mRemoteHotwordDetectionService.isBound());
        pw.print(", mValidatingDspTrigger=" + this.mValidatingDspTrigger);
        pw.print(", mPerformingSoftwareHotwordDetection=" + this.mPerformingSoftwareHotwordDetection);
        pw.print(", mRestartCount=" + this.mServiceConnectionFactory.mRestartCount);
        pw.print(", mLastRestartInstant=" + this.mLastRestartInstant);
        pw.println(", mDetectorType=" + HotwordDetector.detectorTypeToString(this.mDetectorType));
    }

    private void handleExternalSourceHotwordDetection(ParcelFileDescriptor audioStream, final AudioFormat audioFormat, final PersistableBundle options, final IMicrophoneHotwordDetectionVoiceInteractionCallback callback) {
        final InputStream audioSource = new ParcelFileDescriptor.AutoCloseInputStream(audioStream);
        Pair<ParcelFileDescriptor, ParcelFileDescriptor> clientPipe = createPipe();
        if (clientPipe == null) {
            return;
        }
        final ParcelFileDescriptor serviceAudioSink = (ParcelFileDescriptor) clientPipe.second;
        final ParcelFileDescriptor serviceAudioSource = (ParcelFileDescriptor) clientPipe.first;
        synchronized (this.mLock) {
            try {
                this.mCurrentAudioSink = serviceAudioSink;
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        this.mAudioCopyExecutor.execute(new Runnable() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                HotwordDetectionConnection.this.m7582x818db06f(audioSource, serviceAudioSink, callback);
            }
        });
        this.mRemoteHotwordDetectionService.run(new ServiceConnector.VoidJob() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda4
            public final void runNoResult(Object obj) {
                HotwordDetectionConnection.this.m7581x26b13153(serviceAudioSource, audioFormat, options, serviceAudioSink, audioSource, callback, (IHotwordDetectionService) obj);
            }
        });
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [874=4, 876=5] */
    /* JADX DEBUG: Finally have unexpected throw blocks count: 2, expect 1 */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleExternalSourceHotwordDetection$9$com-android-server-voiceinteraction-HotwordDetectionConnection  reason: not valid java name */
    public /* synthetic */ void m7582x818db06f(InputStream audioSource, ParcelFileDescriptor serviceAudioSink, IMicrophoneHotwordDetectionVoiceInteractionCallback callback) {
        try {
            try {
                try {
                    OutputStream fos = new ParcelFileDescriptor.AutoCloseOutputStream(serviceAudioSink);
                    byte[] buffer = new byte[1024];
                    while (true) {
                        int bytesRead = audioSource.read(buffer, 0, 1024);
                        if (bytesRead < 0) {
                            break;
                        }
                        fos.write(buffer, 0, bytesRead);
                    }
                    Slog.i(TAG, "Reached end of stream for external hotword");
                    fos.close();
                    if (audioSource != null) {
                        audioSource.close();
                    }
                    synchronized (this.mLock) {
                        this.mCurrentAudioSink = null;
                    }
                } catch (IOException e) {
                    Slog.w(TAG, "Failed supplying audio data to validator", e);
                    try {
                        callback.onError();
                    } catch (RemoteException ex) {
                        Slog.w(TAG, "Failed to report onError status: " + ex);
                    }
                    synchronized (this.mLock) {
                        this.mCurrentAudioSink = null;
                    }
                }
            } catch (Throwable th) {
                if (audioSource != null) {
                    try {
                        audioSource.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (Throwable th3) {
            synchronized (this.mLock) {
                this.mCurrentAudioSink = null;
                throw th3;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleExternalSourceHotwordDetection$10$com-android-server-voiceinteraction-HotwordDetectionConnection  reason: not valid java name */
    public /* synthetic */ void m7581x26b13153(ParcelFileDescriptor serviceAudioSource, AudioFormat audioFormat, PersistableBundle options, ParcelFileDescriptor serviceAudioSink, InputStream audioSource, IMicrophoneHotwordDetectionVoiceInteractionCallback callback, IHotwordDetectionService service) throws Exception {
        service.detectFromMicrophoneSource(serviceAudioSource, 2, audioFormat, options, new AnonymousClass5(serviceAudioSink, audioSource, callback));
        bestEffortClose(serviceAudioSource);
    }

    /* renamed from: com.android.server.voiceinteraction.HotwordDetectionConnection$5  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass5 extends IDspHotwordDetectionCallback.Stub {
        final /* synthetic */ InputStream val$audioSource;
        final /* synthetic */ IMicrophoneHotwordDetectionVoiceInteractionCallback val$callback;
        final /* synthetic */ ParcelFileDescriptor val$serviceAudioSink;

        AnonymousClass5(ParcelFileDescriptor parcelFileDescriptor, InputStream inputStream, IMicrophoneHotwordDetectionVoiceInteractionCallback iMicrophoneHotwordDetectionVoiceInteractionCallback) {
            this.val$serviceAudioSink = parcelFileDescriptor;
            this.val$audioSource = inputStream;
            this.val$callback = iMicrophoneHotwordDetectionVoiceInteractionCallback;
        }

        public void onRejected(HotwordRejectedResult result) throws RemoteException {
            ScheduledExecutorService scheduledExecutorService = HotwordDetectionConnection.this.mScheduledExecutorService;
            final ParcelFileDescriptor parcelFileDescriptor = this.val$serviceAudioSink;
            final InputStream inputStream = this.val$audioSource;
            scheduledExecutorService.schedule(new Runnable() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$5$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    HotwordDetectionConnection.bestEffortClose(parcelFileDescriptor, inputStream);
                }
            }, HotwordDetectionConnection.EXTERNAL_HOTWORD_CLEANUP_MILLIS, TimeUnit.MILLISECONDS);
            this.val$callback.onRejected(result);
            if (result != null) {
                Slog.i(HotwordDetectionConnection.TAG, "Egressed 'hotword rejected result' from hotword trusted process");
                if (HotwordDetectionConnection.this.mDebugHotwordLogging) {
                    Slog.i(HotwordDetectionConnection.TAG, "Egressed detected result: " + result);
                }
            }
        }

        public void onDetected(HotwordDetectedResult triggerResult) throws RemoteException {
            ScheduledExecutorService scheduledExecutorService = HotwordDetectionConnection.this.mScheduledExecutorService;
            final ParcelFileDescriptor parcelFileDescriptor = this.val$serviceAudioSink;
            final InputStream inputStream = this.val$audioSource;
            scheduledExecutorService.schedule(new Runnable() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$5$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    HotwordDetectionConnection.bestEffortClose(parcelFileDescriptor, inputStream);
                }
            }, HotwordDetectionConnection.EXTERNAL_HOTWORD_CLEANUP_MILLIS, TimeUnit.MILLISECONDS);
            try {
                HotwordDetectionConnection.this.enforcePermissionsForDataDelivery();
                this.val$callback.onDetected(triggerResult, (AudioFormat) null, (ParcelFileDescriptor) null);
                if (triggerResult != null) {
                    Slog.i(HotwordDetectionConnection.TAG, "Egressed " + HotwordDetectedResult.getUsageSize(triggerResult) + " bits from hotword trusted process");
                    if (HotwordDetectionConnection.this.mDebugHotwordLogging) {
                        Slog.i(HotwordDetectionConnection.TAG, "Egressed detected result: " + triggerResult);
                    }
                }
            } catch (SecurityException e) {
                this.val$callback.onError();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ServiceConnectionFactory {
        private final int mBindingFlags;
        private final Intent mIntent;
        private int mRestartCount = 0;

        ServiceConnectionFactory(Intent intent, boolean bindInstantServiceAllowed) {
            this.mIntent = intent;
            this.mBindingFlags = bindInstantServiceAllowed ? 4194304 : 0;
        }

        ServiceConnection createLocked() {
            HotwordDetectionConnection hotwordDetectionConnection = HotwordDetectionConnection.this;
            Context context = hotwordDetectionConnection.mContext;
            Intent intent = this.mIntent;
            int i = this.mBindingFlags;
            int i2 = HotwordDetectionConnection.this.mUser;
            Function function = new Function() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$ServiceConnectionFactory$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return IHotwordDetectionService.Stub.asInterface((IBinder) obj);
                }
            };
            int i3 = this.mRestartCount;
            this.mRestartCount = i3 + 1;
            ServiceConnection connection = new ServiceConnection(context, intent, i, i2, function, i3 % 10);
            connection.connect();
            HotwordDetectionConnection.updateAudioFlinger(connection, HotwordDetectionConnection.this.mAudioFlinger);
            HotwordDetectionConnection.updateContentCaptureManager(connection);
            HotwordDetectionConnection.this.updateServiceIdentity(connection);
            return connection;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ServiceConnection extends ServiceConnector.Impl<IHotwordDetectionService> {
        private final int mBindingFlags;
        private final int mInstanceNumber;
        private final Intent mIntent;
        private boolean mIsBound;
        private boolean mIsLoggedFirstConnect;
        private final Object mLock;
        private boolean mRespectServiceConnectionStatusChanged;

        ServiceConnection(Context context, Intent intent, int bindingFlags, int userId, Function<IBinder, IHotwordDetectionService> binderAsInterface, int instanceNumber) {
            super(context, intent, bindingFlags, userId, binderAsInterface);
            this.mLock = new Object();
            this.mRespectServiceConnectionStatusChanged = true;
            this.mIsBound = false;
            this.mIsLoggedFirstConnect = false;
            this.mIntent = intent;
            this.mBindingFlags = bindingFlags;
            this.mInstanceNumber = instanceNumber;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: protected */
        public void onServiceConnectionStatusChanged(IHotwordDetectionService service, boolean connected) {
            synchronized (this.mLock) {
                if (!this.mRespectServiceConnectionStatusChanged) {
                    Slog.v(HotwordDetectionConnection.TAG, "Ignored onServiceConnectionStatusChanged event");
                    return;
                }
                this.mIsBound = connected;
                if (connected && !this.mIsLoggedFirstConnect) {
                    this.mIsLoggedFirstConnect = true;
                    HotwordMetricsLogger.writeDetectorEvent(HotwordDetectionConnection.this.mDetectorType, 2, HotwordDetectionConnection.this.mVoiceInteractionServiceUid);
                }
            }
        }

        protected long getAutoDisconnectTimeoutMs() {
            return -1L;
        }

        public void binderDied() {
            super.binderDied();
            synchronized (this.mLock) {
                if (!this.mRespectServiceConnectionStatusChanged) {
                    Slog.v(HotwordDetectionConnection.TAG, "Ignored #binderDied event");
                    return;
                }
                Slog.w(HotwordDetectionConnection.TAG, "binderDied");
                try {
                    HotwordDetectionConnection.this.mCallback.onError(-1);
                } catch (RemoteException e) {
                    Slog.w(HotwordDetectionConnection.TAG, "Failed to report onError status: " + e);
                }
            }
        }

        protected boolean bindService(android.content.ServiceConnection serviceConnection) {
            try {
                HotwordMetricsLogger.writeDetectorEvent(HotwordDetectionConnection.this.mDetectorType, 1, HotwordDetectionConnection.this.mVoiceInteractionServiceUid);
                boolean bindResult = this.mContext.bindIsolatedService(this.mIntent, this.mBindingFlags | android.hardware.audio.common.V2_0.AudioFormat.AAC_MAIN, "hotword_detector_" + this.mInstanceNumber, this.mExecutor, serviceConnection);
                if (!bindResult) {
                    HotwordMetricsLogger.writeDetectorEvent(HotwordDetectionConnection.this.mDetectorType, 3, HotwordDetectionConnection.this.mVoiceInteractionServiceUid);
                }
                return bindResult;
            } catch (IllegalArgumentException e) {
                HotwordMetricsLogger.writeDetectorEvent(HotwordDetectionConnection.this.mDetectorType, 3, HotwordDetectionConnection.this.mVoiceInteractionServiceUid);
                Slog.wtf(HotwordDetectionConnection.TAG, "Can't bind to the hotword detection service!", e);
                return false;
            }
        }

        boolean isBound() {
            boolean z;
            synchronized (this.mLock) {
                z = this.mIsBound;
            }
            return z;
        }

        void ignoreConnectionStatusEvents() {
            synchronized (this.mLock) {
                this.mRespectServiceConnectionStatusChanged = false;
            }
        }
    }

    private static Pair<ParcelFileDescriptor, ParcelFileDescriptor> createPipe() {
        try {
            ParcelFileDescriptor[] fileDescriptors = ParcelFileDescriptor.createPipe();
            return Pair.create(fileDescriptors[0], fileDescriptors[1]);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to create audio stream pipe", e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateAudioFlinger(ServiceConnection connection, final IBinder audioFlinger) {
        connection.run(new ServiceConnector.VoidJob() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda1
            public final void runNoResult(Object obj) {
                ((IHotwordDetectionService) obj).updateAudioFlinger(audioFlinger);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateContentCaptureManager(ServiceConnection connection) {
        IBinder b = ServiceManager.getService("content_capture");
        final IContentCaptureManager binderService = IContentCaptureManager.Stub.asInterface(b);
        connection.run(new ServiceConnector.VoidJob() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda18
            public final void runNoResult(Object obj) {
                ((IHotwordDetectionService) obj).updateContentCaptureManager(binderService, new ContentCaptureOptions((ArraySet) null));
            }
        });
    }

    /* renamed from: com.android.server.voiceinteraction.HotwordDetectionConnection$6  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass6 extends IRemoteCallback.Stub {
        AnonymousClass6() {
        }

        public void sendResult(Bundle bundle) throws RemoteException {
            final int uid = Binder.getCallingUid();
            ((PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class)).setHotwordDetectionServiceProvider(new PermissionManagerServiceInternal.HotwordDetectionServiceProvider() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$6$$ExternalSyntheticLambda0
                @Override // com.android.server.pm.permission.PermissionManagerServiceInternal.HotwordDetectionServiceProvider
                public final int getUid() {
                    return HotwordDetectionConnection.AnonymousClass6.lambda$sendResult$0(uid);
                }
            });
            HotwordDetectionConnection.this.mIdentity = new VoiceInteractionManagerInternal.HotwordDetectionServiceIdentity(uid, HotwordDetectionConnection.this.mVoiceInteractionServiceUid);
            HotwordDetectionConnection.this.addServiceUidForAudioPolicy(uid);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ int lambda$sendResult$0(int uid) {
            return uid;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateServiceIdentity(ServiceConnection connection) {
        connection.run(new ServiceConnector.VoidJob() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda5
            public final void runNoResult(Object obj) {
                HotwordDetectionConnection.this.m7585xc3322b04((IHotwordDetectionService) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateServiceIdentity$13$com-android-server-voiceinteraction-HotwordDetectionConnection  reason: not valid java name */
    public /* synthetic */ void m7585xc3322b04(IHotwordDetectionService service) throws Exception {
        service.ping(new AnonymousClass6());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addServiceUidForAudioPolicy(final int uid) {
        this.mScheduledExecutorService.execute(new Runnable() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                HotwordDetectionConnection.lambda$addServiceUidForAudioPolicy$14(uid);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addServiceUidForAudioPolicy$14(int uid) {
        AudioManagerInternal audioManager = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
        if (audioManager != null) {
            audioManager.addAssistantServiceUid(uid);
        }
    }

    private void removeServiceUidForAudioPolicy(final int uid) {
        this.mScheduledExecutorService.execute(new Runnable() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                HotwordDetectionConnection.lambda$removeServiceUidForAudioPolicy$15(uid);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$removeServiceUidForAudioPolicy$15(int uid) {
        AudioManagerInternal audioManager = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
        if (audioManager != null) {
            audioManager.removeAssistantServiceUid(uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void bestEffortClose(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            bestEffortClose(closeable);
        }
    }

    private static void bestEffortClose(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enforcePermissionsForDataDelivery() {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.voiceinteraction.HotwordDetectionConnection$$ExternalSyntheticLambda8
            public final void runOrThrow() {
                HotwordDetectionConnection.this.m7580x3984965c();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$enforcePermissionsForDataDelivery$16$com-android-server-voiceinteraction-HotwordDetectionConnection  reason: not valid java name */
    public /* synthetic */ void m7580x3984965c() throws Exception {
        enforcePermissionForDataDelivery(this.mContext, this.mVoiceInteractorIdentity, "android.permission.RECORD_AUDIO", OP_MESSAGE);
        enforcePermissionForDataDelivery(this.mContext, this.mVoiceInteractorIdentity, "android.permission.CAPTURE_AUDIO_HOTWORD", OP_MESSAGE);
    }

    private static void enforcePermissionForDataDelivery(Context context, Identity identity, String permission, String reason) {
        int status = PermissionUtil.checkPermissionForDataDelivery(context, identity, permission, reason);
        if (status != 0) {
            throw new SecurityException(TextUtils.formatSimple("Failed to obtain permission %s for identity %s", new Object[]{permission, SoundTriggerSessionPermissionsDecorator.toString(identity)}));
        }
    }
}
