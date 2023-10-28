package com.android.server.contentsuggestions;

import android.app.contentsuggestions.ClassificationsRequest;
import android.app.contentsuggestions.IClassificationsCallback;
import android.app.contentsuggestions.ISelectionsCallback;
import android.app.contentsuggestions.SelectionsRequest;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.HardwareBuffer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.service.contentsuggestions.IContentSuggestionsService;
import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;
/* loaded from: classes.dex */
public class RemoteContentSuggestionsService extends AbstractMultiplePendingRequestsRemoteService<RemoteContentSuggestionsService, IContentSuggestionsService> {
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 2000;

    /* loaded from: classes.dex */
    interface Callbacks extends AbstractRemoteService.VultureCallback<RemoteContentSuggestionsService> {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteContentSuggestionsService(Context context, ComponentName serviceName, int userId, Callbacks callbacks, boolean bindInstantServiceAllowed, boolean verbose) {
        super(context, "android.service.contentsuggestions.ContentSuggestionsService", serviceName, userId, callbacks, context.getMainThreadHandler(), bindInstantServiceAllowed ? 4194304 : 0, verbose, 1);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public IContentSuggestionsService getServiceInterface(IBinder service) {
        return IContentSuggestionsService.Stub.asInterface(service);
    }

    protected long getTimeoutIdleBindMillis() {
        return 0L;
    }

    protected long getRemoteRequestMillis() {
        return TIMEOUT_REMOTE_REQUEST_MILLIS;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void provideContextImage(final int taskId, final HardwareBuffer contextImage, final int colorSpaceId, final Bundle imageContextRequestExtras) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentsuggestions.RemoteContentSuggestionsService$$ExternalSyntheticLambda3
            public final void run(IInterface iInterface) {
                ((IContentSuggestionsService) iInterface).provideContextImage(taskId, contextImage, colorSpaceId, imageContextRequestExtras);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void suggestContentSelections(final SelectionsRequest selectionsRequest, final ISelectionsCallback selectionsCallback) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentsuggestions.RemoteContentSuggestionsService$$ExternalSyntheticLambda2
            public final void run(IInterface iInterface) {
                ((IContentSuggestionsService) iInterface).suggestContentSelections(selectionsRequest, selectionsCallback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void classifyContentSelections(final ClassificationsRequest classificationsRequest, final IClassificationsCallback callback) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentsuggestions.RemoteContentSuggestionsService$$ExternalSyntheticLambda0
            public final void run(IInterface iInterface) {
                ((IContentSuggestionsService) iInterface).classifyContentSelections(classificationsRequest, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyInteraction(final String requestId, final Bundle bundle) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentsuggestions.RemoteContentSuggestionsService$$ExternalSyntheticLambda1
            public final void run(IInterface iInterface) {
                ((IContentSuggestionsService) iInterface).notifyInteraction(requestId, bundle);
            }
        });
    }
}
