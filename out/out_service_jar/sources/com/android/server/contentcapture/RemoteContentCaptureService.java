package com.android.server.contentcapture;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;
import android.service.contentcapture.ActivityEvent;
import android.service.contentcapture.IContentCaptureService;
import android.service.contentcapture.IContentCaptureServiceCallback;
import android.service.contentcapture.IDataShareCallback;
import android.service.contentcapture.SnapshotData;
import android.util.Slog;
import android.view.contentcapture.ContentCaptureContext;
import android.view.contentcapture.ContentCaptureHelper;
import android.view.contentcapture.DataRemovalRequest;
import android.view.contentcapture.DataShareRequest;
import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;
import com.android.internal.os.IResultReceiver;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class RemoteContentCaptureService extends AbstractMultiplePendingRequestsRemoteService<RemoteContentCaptureService, IContentCaptureService> {
    private final int mIdleUnbindTimeoutMs;
    private final ContentCapturePerUserService mPerUserService;
    private final IBinder mServerCallback;

    /* loaded from: classes.dex */
    public interface ContentCaptureServiceCallbacks extends AbstractRemoteService.VultureCallback<RemoteContentCaptureService> {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteContentCaptureService(Context context, String serviceInterface, ComponentName serviceComponentName, IContentCaptureServiceCallback callback, int userId, ContentCapturePerUserService perUserService, boolean bindInstantServiceAllowed, boolean verbose, int idleUnbindTimeoutMs) {
        super(context, serviceInterface, serviceComponentName, userId, perUserService, context.getMainThreadHandler(), (bindInstantServiceAllowed ? 4194304 : 0) | 4096, verbose, 2);
        this.mPerUserService = perUserService;
        this.mServerCallback = callback.asBinder();
        this.mIdleUnbindTimeoutMs = idleUnbindTimeoutMs;
        ensureBoundLocked();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public IContentCaptureService getServiceInterface(IBinder service) {
        return IContentCaptureService.Stub.asInterface(service);
    }

    protected long getTimeoutIdleBindMillis() {
        return this.mIdleUnbindTimeoutMs;
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:15:0x0041 -> B:16:0x0063). Please submit an issue!!! */
    protected void handleOnConnectedStateChanged(boolean connected) {
        if (connected && getTimeoutIdleBindMillis() != 0) {
            scheduleUnbind();
        }
        try {
            if (connected) {
                this.mService.onConnected(this.mServerCallback, ContentCaptureHelper.sVerbose, ContentCaptureHelper.sDebug);
                ContentCaptureMetricsLogger.writeServiceEvent(1, this.mComponentName);
                this.mPerUserService.onConnected();
                return;
            }
            this.mService.onDisconnected();
            ContentCaptureMetricsLogger.writeServiceEvent(2, this.mComponentName);
        } catch (Exception e) {
            while (true) {
                Slog.w(this.mTag, "Exception calling onConnectedStateChanged(" + connected + "): " + e);
                return;
            }
        }
    }

    public void ensureBoundLocked() {
        scheduleBind();
    }

    public void onSessionStarted(final ContentCaptureContext context, final int sessionId, final int uid, final IResultReceiver clientReceiver, final int initialState) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentcapture.RemoteContentCaptureService$$ExternalSyntheticLambda1
            public final void run(IInterface iInterface) {
                ((IContentCaptureService) iInterface).onSessionStarted(context, sessionId, uid, clientReceiver, initialState);
            }
        });
        ContentCaptureMetricsLogger.writeSessionEvent(sessionId, 1, initialState, getComponentName(), false);
    }

    public void onSessionFinished(final int sessionId) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentcapture.RemoteContentCaptureService$$ExternalSyntheticLambda0
            public final void run(IInterface iInterface) {
                ((IContentCaptureService) iInterface).onSessionFinished(sessionId);
            }
        });
        ContentCaptureMetricsLogger.writeSessionEvent(sessionId, 2, 0, getComponentName(), false);
    }

    public void onActivitySnapshotRequest(final int sessionId, final SnapshotData snapshotData) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentcapture.RemoteContentCaptureService$$ExternalSyntheticLambda4
            public final void run(IInterface iInterface) {
                ((IContentCaptureService) iInterface).onActivitySnapshot(sessionId, snapshotData);
            }
        });
    }

    public void onDataRemovalRequest(final DataRemovalRequest request) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentcapture.RemoteContentCaptureService$$ExternalSyntheticLambda2
            public final void run(IInterface iInterface) {
                ((IContentCaptureService) iInterface).onDataRemovalRequest(request);
            }
        });
        ContentCaptureMetricsLogger.writeServiceEvent(5, this.mComponentName);
    }

    public void onDataShareRequest(final DataShareRequest request, final IDataShareCallback.Stub dataShareCallback) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentcapture.RemoteContentCaptureService$$ExternalSyntheticLambda3
            public final void run(IInterface iInterface) {
                ((IContentCaptureService) iInterface).onDataShared(request, dataShareCallback);
            }
        });
        ContentCaptureMetricsLogger.writeServiceEvent(6, this.mComponentName);
    }

    public void onActivityLifecycleEvent(final ActivityEvent event) {
        scheduleAsyncRequest(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.contentcapture.RemoteContentCaptureService$$ExternalSyntheticLambda5
            public final void run(IInterface iInterface) {
                ((IContentCaptureService) iInterface).onActivityEvent(event);
            }
        });
    }
}
