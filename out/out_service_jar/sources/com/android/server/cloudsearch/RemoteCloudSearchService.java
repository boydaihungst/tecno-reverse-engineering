package com.android.server.cloudsearch;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.service.cloudsearch.ICloudSearchService;
import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;
/* loaded from: classes.dex */
public class RemoteCloudSearchService extends AbstractMultiplePendingRequestsRemoteService<RemoteCloudSearchService, ICloudSearchService> {
    private static final String TAG = "RemoteCloudSearchService";
    private static final long TIMEOUT_IDLE_BOUND_TIMEOUT_MS = 600000;
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 2000;
    private final RemoteCloudSearchServiceCallbacks mCallback;

    /* loaded from: classes.dex */
    public interface RemoteCloudSearchServiceCallbacks extends AbstractRemoteService.VultureCallback<RemoteCloudSearchService> {
        void onConnectedStateChanged(boolean z);

        void onFailureOrTimeout(boolean z);
    }

    public RemoteCloudSearchService(Context context, String serviceInterface, ComponentName componentName, int userId, RemoteCloudSearchServiceCallbacks callback, boolean bindInstantServiceAllowed, boolean verbose) {
        super(context, serviceInterface, componentName, userId, callback, context.getMainThreadHandler(), bindInstantServiceAllowed ? 4194304 : 0, verbose, 1);
        this.mCallback = callback;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public ICloudSearchService getServiceInterface(IBinder service) {
        return ICloudSearchService.Stub.asInterface(service);
    }

    protected long getTimeoutIdleBindMillis() {
        return 600000L;
    }

    protected long getRemoteRequestMillis() {
        return TIMEOUT_REMOTE_REQUEST_MILLIS;
    }

    public void reconnect() {
        super.scheduleBind();
    }

    public void scheduleOnResolvedService(AbstractRemoteService.AsyncRequest<ICloudSearchService> request) {
        scheduleAsyncRequest(request);
    }

    public void executeOnResolvedService(AbstractRemoteService.AsyncRequest<ICloudSearchService> request) {
        executeAsyncRequest(request);
    }

    protected void handleOnConnectedStateChanged(boolean connected) {
        RemoteCloudSearchServiceCallbacks remoteCloudSearchServiceCallbacks = this.mCallback;
        if (remoteCloudSearchServiceCallbacks != null) {
            remoteCloudSearchServiceCallbacks.onConnectedStateChanged(connected);
        }
    }
}
