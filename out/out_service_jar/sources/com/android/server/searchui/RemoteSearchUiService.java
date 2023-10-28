package com.android.server.searchui;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.service.search.ISearchUiService;
import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;
/* loaded from: classes2.dex */
public class RemoteSearchUiService extends AbstractMultiplePendingRequestsRemoteService<RemoteSearchUiService, ISearchUiService> {
    private static final String TAG = "RemoteSearchUiService";
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 2000;
    private final RemoteSearchUiServiceCallbacks mCallback;

    /* loaded from: classes2.dex */
    public interface RemoteSearchUiServiceCallbacks extends AbstractRemoteService.VultureCallback<RemoteSearchUiService> {
        void onConnectedStateChanged(boolean z);

        void onFailureOrTimeout(boolean z);
    }

    public RemoteSearchUiService(Context context, String serviceInterface, ComponentName componentName, int userId, RemoteSearchUiServiceCallbacks callback, boolean bindInstantServiceAllowed, boolean verbose) {
        super(context, serviceInterface, componentName, userId, callback, context.getMainThreadHandler(), bindInstantServiceAllowed ? 4194304 : 0, verbose, 1);
        this.mCallback = callback;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public ISearchUiService getServiceInterface(IBinder service) {
        return ISearchUiService.Stub.asInterface(service);
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

    public void scheduleOnResolvedService(AbstractRemoteService.AsyncRequest<ISearchUiService> request) {
        scheduleAsyncRequest(request);
    }

    public void executeOnResolvedService(AbstractRemoteService.AsyncRequest<ISearchUiService> request) {
        executeAsyncRequest(request);
    }

    protected void handleOnConnectedStateChanged(boolean connected) {
        RemoteSearchUiServiceCallbacks remoteSearchUiServiceCallbacks = this.mCallback;
        if (remoteSearchUiServiceCallbacks != null) {
            remoteSearchUiServiceCallbacks.onConnectedStateChanged(connected);
        }
    }
}
