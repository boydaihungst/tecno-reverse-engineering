package com.android.server.wallpapereffectsgeneration;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.service.wallpapereffectsgeneration.IWallpaperEffectsGenerationService;
import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;
/* loaded from: classes2.dex */
public class RemoteWallpaperEffectsGenerationService extends AbstractMultiplePendingRequestsRemoteService<RemoteWallpaperEffectsGenerationService, IWallpaperEffectsGenerationService> {
    private static final String TAG = RemoteWallpaperEffectsGenerationService.class.getSimpleName();
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 2000;
    private final RemoteWallpaperEffectsGenerationServiceCallback mCallback;

    /* loaded from: classes2.dex */
    public interface RemoteWallpaperEffectsGenerationServiceCallback extends AbstractRemoteService.VultureCallback<RemoteWallpaperEffectsGenerationService> {
        void onConnectedStateChanged(boolean z);
    }

    public RemoteWallpaperEffectsGenerationService(Context context, ComponentName componentName, int userId, RemoteWallpaperEffectsGenerationServiceCallback callback, boolean bindInstantServiceAllowed, boolean verbose) {
        super(context, "android.service.wallpapereffectsgeneration.WallpaperEffectsGenerationService", componentName, userId, callback, context.getMainThreadHandler(), bindInstantServiceAllowed ? 4194304 : 0, verbose, 1);
        this.mCallback = callback;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public IWallpaperEffectsGenerationService getServiceInterface(IBinder service) {
        return IWallpaperEffectsGenerationService.Stub.asInterface(service);
    }

    protected long getTimeoutIdleBindMillis() {
        return 0L;
    }

    protected long getRemoteRequestMillis() {
        return TIMEOUT_REMOTE_REQUEST_MILLIS;
    }

    public void reconnect() {
        super.scheduleBind();
    }

    public void scheduleOnResolvedService(AbstractRemoteService.AsyncRequest<IWallpaperEffectsGenerationService> request) {
        scheduleAsyncRequest(request);
    }

    public void executeOnResolvedService(AbstractRemoteService.AsyncRequest<IWallpaperEffectsGenerationService> request) {
        executeAsyncRequest(request);
    }

    protected void handleOnConnectedStateChanged(boolean connected) {
        RemoteWallpaperEffectsGenerationServiceCallback remoteWallpaperEffectsGenerationServiceCallback = this.mCallback;
        if (remoteWallpaperEffectsGenerationServiceCallback != null) {
            remoteWallpaperEffectsGenerationServiceCallback.onConnectedStateChanged(connected);
        }
    }
}
