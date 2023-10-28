package com.android.server.smartspace;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.service.smartspace.ISmartspaceService;
import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;
/* loaded from: classes2.dex */
public class RemoteSmartspaceService extends AbstractMultiplePendingRequestsRemoteService<RemoteSmartspaceService, ISmartspaceService> {
    private static final String TAG = "RemoteSmartspaceService";
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 2000;
    private final RemoteSmartspaceServiceCallbacks mCallback;

    /* loaded from: classes2.dex */
    public interface RemoteSmartspaceServiceCallbacks extends AbstractRemoteService.VultureCallback<RemoteSmartspaceService> {
        void onConnectedStateChanged(boolean z);

        void onFailureOrTimeout(boolean z);
    }

    public RemoteSmartspaceService(Context context, String serviceInterface, ComponentName componentName, int userId, RemoteSmartspaceServiceCallbacks callback, boolean bindInstantServiceAllowed, boolean verbose) {
        super(context, serviceInterface, componentName, userId, callback, context.getMainThreadHandler(), bindInstantServiceAllowed ? 4194304 : 0, verbose, 1);
        this.mCallback = callback;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public ISmartspaceService getServiceInterface(IBinder service) {
        return ISmartspaceService.Stub.asInterface(service);
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

    public void scheduleOnResolvedService(AbstractRemoteService.AsyncRequest<ISmartspaceService> request) {
        scheduleAsyncRequest(request);
    }

    public void executeOnResolvedService(AbstractRemoteService.AsyncRequest<ISmartspaceService> request) {
        executeAsyncRequest(request);
    }

    protected void handleOnConnectedStateChanged(boolean connected) {
        RemoteSmartspaceServiceCallbacks remoteSmartspaceServiceCallbacks = this.mCallback;
        if (remoteSmartspaceServiceCallbacks != null) {
            remoteSmartspaceServiceCallbacks.onConnectedStateChanged(connected);
        }
    }
}
