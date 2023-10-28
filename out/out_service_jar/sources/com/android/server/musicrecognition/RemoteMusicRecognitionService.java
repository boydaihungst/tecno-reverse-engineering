package com.android.server.musicrecognition;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioFormat;
import android.media.musicrecognition.IMusicRecognitionAttributionTagCallback;
import android.media.musicrecognition.IMusicRecognitionService;
import android.os.IBinder;
import android.os.IInterface;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;
import com.android.server.musicrecognition.MusicRecognitionManagerPerUserService;
import java.util.concurrent.CompletableFuture;
/* loaded from: classes2.dex */
public class RemoteMusicRecognitionService extends AbstractMultiplePendingRequestsRemoteService<RemoteMusicRecognitionService, IMusicRecognitionService> {
    private static final long TIMEOUT_IDLE_BIND_MILLIS = 40000;
    private final MusicRecognitionManagerPerUserService.MusicRecognitionServiceCallback mServerCallback;

    /* loaded from: classes2.dex */
    interface Callbacks extends AbstractRemoteService.VultureCallback<RemoteMusicRecognitionService> {
    }

    public RemoteMusicRecognitionService(Context context, ComponentName serviceName, int userId, MusicRecognitionManagerPerUserService perUserService, MusicRecognitionManagerPerUserService.MusicRecognitionServiceCallback callback, boolean bindInstantServiceAllowed, boolean verbose) {
        super(context, "android.service.musicrecognition.MUSIC_RECOGNITION", serviceName, userId, perUserService, context.getMainThreadHandler(), (bindInstantServiceAllowed ? 4194304 : 0) | 4096, verbose, 1);
        this.mServerCallback = callback;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    /* renamed from: getServiceInterface */
    public IMusicRecognitionService m4900getServiceInterface(IBinder service) {
        return IMusicRecognitionService.Stub.asInterface(service);
    }

    protected long getTimeoutIdleBindMillis() {
        return TIMEOUT_IDLE_BIND_MILLIS;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MusicRecognitionManagerPerUserService.MusicRecognitionServiceCallback getServerCallback() {
        return this.mServerCallback;
    }

    public void onAudioStreamStarted(final ParcelFileDescriptor fd, final AudioFormat audioFormat) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.musicrecognition.RemoteMusicRecognitionService$$ExternalSyntheticLambda1
            public final void run(IInterface iInterface) {
                RemoteMusicRecognitionService.this.m4899x6334aa51(fd, audioFormat, (IMusicRecognitionService) iInterface);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onAudioStreamStarted$0$com-android-server-musicrecognition-RemoteMusicRecognitionService  reason: not valid java name */
    public /* synthetic */ void m4899x6334aa51(ParcelFileDescriptor fd, AudioFormat audioFormat, IMusicRecognitionService binder) throws RemoteException {
        binder.onAudioStreamStarted(fd, audioFormat, this.mServerCallback);
    }

    public CompletableFuture<String> getAttributionTag() {
        final CompletableFuture<String> attributionTagFuture = new CompletableFuture<>();
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.musicrecognition.RemoteMusicRecognitionService$$ExternalSyntheticLambda0
            public final void run(IInterface iInterface) {
                RemoteMusicRecognitionService.this.m4898x1171fddf(attributionTagFuture, (IMusicRecognitionService) iInterface);
            }
        });
        return attributionTagFuture;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getAttributionTag$1$com-android-server-musicrecognition-RemoteMusicRecognitionService  reason: not valid java name */
    public /* synthetic */ void m4898x1171fddf(final CompletableFuture attributionTagFuture, IMusicRecognitionService binder) throws RemoteException {
        binder.getAttributionTag(new IMusicRecognitionAttributionTagCallback.Stub() { // from class: com.android.server.musicrecognition.RemoteMusicRecognitionService.1
            public void onAttributionTag(String tag) throws RemoteException {
                attributionTagFuture.complete(tag);
            }
        });
    }
}
