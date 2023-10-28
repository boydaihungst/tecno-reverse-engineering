package com.android.server.musicrecognition;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioRecord;
import android.media.MediaMetadata;
import android.media.musicrecognition.IMusicRecognitionManagerCallback;
import android.media.musicrecognition.IMusicRecognitionServiceCallback;
import android.media.musicrecognition.RecognitionRequest;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Pair;
import android.util.Slog;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.musicrecognition.RemoteMusicRecognitionService;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public final class MusicRecognitionManagerPerUserService extends AbstractPerUserSystemService<MusicRecognitionManagerPerUserService, MusicRecognitionManagerService> implements RemoteMusicRecognitionService.Callbacks {
    private static final int BYTES_PER_SAMPLE = 2;
    private static final String KEY_MUSIC_RECOGNITION_SERVICE_ATTRIBUTION_TAG = "android.media.musicrecognition.attributiontag";
    private static final int MAX_STREAMING_SECONDS = 24;
    private static final String MUSIC_RECOGNITION_MANAGER_ATTRIBUTION_TAG = "MusicRecognitionManagerService";
    private static final String TAG = MusicRecognitionManagerPerUserService.class.getSimpleName();
    private final AppOpsManager mAppOpsManager;
    private final String mAttributionMessage;
    private CompletableFuture<String> mAttributionTagFuture;
    private RemoteMusicRecognitionService mRemoteService;
    private ServiceInfo mServiceInfo;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MusicRecognitionManagerPerUserService(MusicRecognitionManagerService primary, Object lock, int userId) {
        super(primary, lock, userId);
        this.mAppOpsManager = (AppOpsManager) getContext().createAttributionContext(MUSIC_RECOGNITION_MANAGER_ATTRIBUTION_TAG).getSystemService(AppOpsManager.class);
        this.mAttributionMessage = String.format("MusicRecognitionManager.invokedByUid.%s", Integer.valueOf(userId));
        this.mAttributionTagFuture = null;
        this.mServiceInfo = null;
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        try {
            ServiceInfo si = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 128L, this.mUserId);
            if (!"android.permission.BIND_MUSIC_RECOGNITION_SERVICE".equals(si.permission)) {
                Slog.w(TAG, "MusicRecognitionService from '" + si.packageName + "' does not require permission android.permission.BIND_MUSIC_RECOGNITION_SERVICE");
                throw new SecurityException("Service does not require permission android.permission.BIND_MUSIC_RECOGNITION_SERVICE");
            }
            return si;
        } catch (RemoteException e) {
            throw new PackageManager.NameNotFoundException("Could not get service for " + serviceComponent);
        }
    }

    private RemoteMusicRecognitionService ensureRemoteServiceLocked(IMusicRecognitionManagerCallback clientCallback) {
        if (this.mRemoteService == null) {
            String serviceName = getComponentNameLocked();
            if (serviceName == null) {
                if (((MusicRecognitionManagerService) this.mMaster).verbose) {
                    Slog.v(TAG, "ensureRemoteServiceLocked(): not set");
                }
                return null;
            }
            ComponentName serviceComponent = ComponentName.unflattenFromString(serviceName);
            this.mRemoteService = new RemoteMusicRecognitionService(getContext(), serviceComponent, this.mUserId, this, new MusicRecognitionServiceCallback(clientCallback), ((MusicRecognitionManagerService) this.mMaster).isBindInstantServiceAllowed(), ((MusicRecognitionManagerService) this.mMaster).verbose);
            try {
                this.mServiceInfo = getContext().getPackageManager().getServiceInfo(this.mRemoteService.getComponentName(), 128);
                this.mAttributionTagFuture = this.mRemoteService.getAttributionTag();
                Slog.i(TAG, "Remote service bound: " + this.mRemoteService.getComponentName());
            } catch (PackageManager.NameNotFoundException e) {
                Slog.e(TAG, "Service was not found.", e);
            }
        }
        return this.mRemoteService;
    }

    public void beginRecognitionLocked(final RecognitionRequest recognitionRequest, IBinder callback) {
        final IMusicRecognitionManagerCallback clientCallback = IMusicRecognitionManagerCallback.Stub.asInterface(callback);
        RemoteMusicRecognitionService ensureRemoteServiceLocked = ensureRemoteServiceLocked(clientCallback);
        this.mRemoteService = ensureRemoteServiceLocked;
        if (ensureRemoteServiceLocked == null) {
            try {
                clientCallback.onRecognitionFailed(3);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        Pair<ParcelFileDescriptor, ParcelFileDescriptor> clientPipe = createPipe();
        if (clientPipe == null) {
            try {
                clientCallback.onRecognitionFailed(7);
                return;
            } catch (RemoteException e2) {
                return;
            }
        }
        final ParcelFileDescriptor audioSink = (ParcelFileDescriptor) clientPipe.second;
        ParcelFileDescriptor clientRead = (ParcelFileDescriptor) clientPipe.first;
        this.mAttributionTagFuture.thenAcceptAsync(new Consumer() { // from class: com.android.server.musicrecognition.MusicRecognitionManagerPerUserService$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MusicRecognitionManagerPerUserService.this.m4894xa9fb912a(recognitionRequest, clientCallback, audioSink, (String) obj);
            }
        }, (Executor) ((MusicRecognitionManagerService) this.mMaster).mExecutorService);
        this.mRemoteService.onAudioStreamStarted(clientRead, recognitionRequest.getAudioFormat());
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: streamAudio */
    public void m4894xa9fb912a(String attributionTag, RecognitionRequest recognitionRequest, IMusicRecognitionManagerCallback clientCallback, ParcelFileDescriptor audioSink) {
        int maxAudioLengthSeconds = Math.min(recognitionRequest.getMaxAudioLengthSeconds(), 24);
        if (maxAudioLengthSeconds <= 0) {
            Slog.i(TAG, "No audio requested. Closing stream.");
            try {
                audioSink.close();
                clientCallback.onAudioStreamClosed();
                return;
            } catch (RemoteException e) {
                return;
            } catch (IOException e2) {
                Slog.e(TAG, "Problem closing stream.", e2);
                return;
            }
        }
        try {
            startRecordAudioOp(attributionTag);
            AudioRecord audioRecord = createAudioRecord(recognitionRequest, maxAudioLengthSeconds);
            try {
                try {
                    try {
                        OutputStream fos = new ParcelFileDescriptor.AutoCloseOutputStream(audioSink);
                        try {
                            streamAudio(recognitionRequest, maxAudioLengthSeconds, audioRecord, fos);
                            fos.close();
                            audioRecord.release();
                            finishRecordAudioOp(attributionTag);
                            clientCallback.onAudioStreamClosed();
                        } catch (Throwable th) {
                            try {
                                fos.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                            throw th;
                        }
                    } catch (RemoteException e3) {
                    }
                } catch (IOException e4) {
                    Slog.e(TAG, "Audio streaming stopped.", e4);
                    audioRecord.release();
                    finishRecordAudioOp(attributionTag);
                    clientCallback.onAudioStreamClosed();
                }
            } catch (Throwable th3) {
                audioRecord.release();
                finishRecordAudioOp(attributionTag);
                try {
                    clientCallback.onAudioStreamClosed();
                } catch (RemoteException e5) {
                }
                throw th3;
            }
        } catch (SecurityException e6) {
            Slog.e(TAG, "RECORD_AUDIO op not permitted on behalf of " + this.mServiceInfo.getComponentName(), e6);
            try {
                clientCallback.onRecognitionFailed(7);
            } catch (RemoteException e7) {
            }
        }
    }

    private void streamAudio(RecognitionRequest recognitionRequest, int maxAudioLengthSeconds, AudioRecord audioRecord, OutputStream outputStream) throws IOException {
        int halfSecondBufferSize = audioRecord.getBufferSizeInFrames() / maxAudioLengthSeconds;
        byte[] byteBuffer = new byte[halfSecondBufferSize];
        int bytesRead = 0;
        int totalBytesRead = 0;
        int ignoreBytes = recognitionRequest.getIgnoreBeginningFrames() * 2;
        audioRecord.startRecording();
        while (bytesRead >= 0 && totalBytesRead < audioRecord.getBufferSizeInFrames() * 2 && this.mRemoteService != null) {
            bytesRead = audioRecord.read(byteBuffer, 0, byteBuffer.length);
            if (bytesRead > 0) {
                totalBytesRead += bytesRead;
                if (ignoreBytes > 0) {
                    ignoreBytes -= bytesRead;
                    if (ignoreBytes < 0) {
                        outputStream.write(byteBuffer, bytesRead + ignoreBytes, -ignoreBytes);
                    }
                } else {
                    outputStream.write(byteBuffer);
                }
            }
        }
        Slog.i(TAG, String.format("Streamed %s bytes from audio record", Integer.valueOf(totalBytesRead)));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class MusicRecognitionServiceCallback extends IMusicRecognitionServiceCallback.Stub {
        private final IMusicRecognitionManagerCallback mClientCallback;

        private MusicRecognitionServiceCallback(IMusicRecognitionManagerCallback clientCallback) {
            this.mClientCallback = clientCallback;
        }

        public void onRecognitionSucceeded(MediaMetadata result, Bundle extras) {
            try {
                MusicRecognitionManagerPerUserService.sanitizeBundle(extras);
                this.mClientCallback.onRecognitionSucceeded(result, extras);
            } catch (RemoteException e) {
            }
            MusicRecognitionManagerPerUserService.this.destroyService();
        }

        public void onRecognitionFailed(int failureCode) {
            try {
                this.mClientCallback.onRecognitionFailed(failureCode);
            } catch (RemoteException e) {
            }
            MusicRecognitionManagerPerUserService.this.destroyService();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public IMusicRecognitionManagerCallback getClientCallback() {
            return this.mClientCallback;
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    public void onServiceDied(RemoteMusicRecognitionService service) {
        try {
            service.getServerCallback().getClientCallback().onRecognitionFailed(5);
        } catch (RemoteException e) {
        }
        Slog.w(TAG, "remote service died: " + service);
        destroyService();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void destroyService() {
        synchronized (this.mLock) {
            RemoteMusicRecognitionService remoteMusicRecognitionService = this.mRemoteService;
            if (remoteMusicRecognitionService != null) {
                remoteMusicRecognitionService.destroy();
                this.mRemoteService = null;
            }
        }
    }

    private void startRecordAudioOp(String attributionTag) {
        int status = this.mAppOpsManager.startProxyOp((String) Objects.requireNonNull(AppOpsManager.permissionToOp("android.permission.RECORD_AUDIO")), this.mServiceInfo.applicationInfo.uid, this.mServiceInfo.packageName, attributionTag, this.mAttributionMessage);
        if (status != 0) {
            throw new SecurityException(String.format("Failed to obtain RECORD_AUDIO permission (status: %d) for receiving service: %s", Integer.valueOf(status), this.mServiceInfo.getComponentName()));
        }
        Slog.i(TAG, String.format("Starting audio streaming. Attributing to %s (%d) with tag '%s'", this.mServiceInfo.packageName, Integer.valueOf(this.mServiceInfo.applicationInfo.uid), attributionTag));
    }

    private void finishRecordAudioOp(String attributionTag) {
        this.mAppOpsManager.finishProxyOp((String) Objects.requireNonNull(AppOpsManager.permissionToOp("android.permission.RECORD_AUDIO")), this.mServiceInfo.applicationInfo.uid, this.mServiceInfo.packageName, attributionTag);
    }

    private static AudioRecord createAudioRecord(RecognitionRequest recognitionRequest, int maxAudioLengthSeconds) {
        int sampleRate = recognitionRequest.getAudioFormat().getSampleRate();
        int bufferSize = getBufferSizeInBytes(sampleRate, maxAudioLengthSeconds);
        return new AudioRecord(recognitionRequest.getAudioAttributes(), recognitionRequest.getAudioFormat(), bufferSize, recognitionRequest.getCaptureSession());
    }

    private static int getBufferSizeInBytes(int sampleRate, int bufferLengthSeconds) {
        return sampleRate * 2 * bufferLengthSeconds;
    }

    private static Pair<ParcelFileDescriptor, ParcelFileDescriptor> createPipe() {
        try {
            ParcelFileDescriptor[] fileDescriptors = ParcelFileDescriptor.createPipe();
            if (fileDescriptors.length != 2) {
                Slog.e(TAG, "Failed to create audio stream pipe, unexpected number of file descriptors");
                return null;
            } else if (!fileDescriptors[0].getFileDescriptor().valid() || !fileDescriptors[1].getFileDescriptor().valid()) {
                Slog.e(TAG, "Failed to create audio stream pipe, didn't receive a pair of valid file descriptors.");
                return null;
            } else {
                return Pair.create(fileDescriptors[0], fileDescriptors[1]);
            }
        } catch (IOException e) {
            Slog.e(TAG, "Failed to create audio stream pipe", e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void sanitizeBundle(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        for (String key : bundle.keySet()) {
            Object o = bundle.get(key);
            if (o instanceof Bundle) {
                sanitizeBundle((Bundle) o);
            } else if ((o instanceof IBinder) || (o instanceof ParcelFileDescriptor)) {
                bundle.remove(key);
            }
        }
    }
}
