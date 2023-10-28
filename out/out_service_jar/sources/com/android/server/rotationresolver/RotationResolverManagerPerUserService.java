package com.android.server.rotationresolver;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.CancellationSignal;
import android.os.RemoteException;
import android.rotationresolver.RotationResolverInternal;
import android.service.rotationresolver.RotationResolutionRequest;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import com.android.internal.util.LatencyTracker;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.rotationresolver.RemoteRotationResolverService;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class RotationResolverManagerPerUserService extends AbstractPerUserSystemService<RotationResolverManagerPerUserService, RotationResolverManagerService> {
    private static final long CONNECTION_TTL_MILLIS = 60000;
    private static final String TAG = RotationResolverManagerPerUserService.class.getSimpleName();
    private ComponentName mComponentName;
    RemoteRotationResolverService.RotationRequest mCurrentRequest;
    private LatencyTracker mLatencyTracker;
    RemoteRotationResolverService mRemoteService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RotationResolverManagerPerUserService(RotationResolverManagerService main, Object lock, int userId) {
        super(main, lock, userId);
        this.mLatencyTracker = LatencyTracker.getInstance(getContext());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroyLocked() {
        if (isVerbose()) {
            Slog.v(TAG, "destroyLocked()");
        }
        if (this.mCurrentRequest == null) {
            return;
        }
        Slog.d(TAG, "Trying to cancel the remote request. Reason: Service destroyed.");
        cancelLocked();
        RemoteRotationResolverService remoteRotationResolverService = this.mRemoteService;
        if (remoteRotationResolverService != null) {
            remoteRotationResolverService.unbind();
            this.mRemoteService = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resolveRotationLocked(final RotationResolverInternal.RotationResolverCallbackInternal callbackInternal, RotationResolutionRequest request, CancellationSignal cancellationSignalInternal) {
        if (!isServiceAvailableLocked()) {
            Slog.w(TAG, "Service is not available at this moment.");
            callbackInternal.onFailure(0);
            RotationResolverManagerService.logRotationStats(request.getProposedRotation(), request.getCurrentRotation(), 7);
            return;
        }
        ensureRemoteServiceInitiated();
        RemoteRotationResolverService.RotationRequest rotationRequest = this.mCurrentRequest;
        if (rotationRequest != null && !rotationRequest.mIsFulfilled) {
            cancelLocked();
        }
        synchronized (this.mLock) {
            this.mLatencyTracker.onActionStart(10);
        }
        RotationResolverInternal.RotationResolverCallbackInternal wrapper = new RotationResolverInternal.RotationResolverCallbackInternal() { // from class: com.android.server.rotationresolver.RotationResolverManagerPerUserService.1
            public void onSuccess(int result) {
                synchronized (RotationResolverManagerPerUserService.this.mLock) {
                    RotationResolverManagerPerUserService.this.mLatencyTracker.onActionEnd(10);
                }
                callbackInternal.onSuccess(result);
            }

            public void onFailure(int error) {
                synchronized (RotationResolverManagerPerUserService.this.mLock) {
                    RotationResolverManagerPerUserService.this.mLatencyTracker.onActionEnd(10);
                }
                callbackInternal.onFailure(error);
            }
        };
        this.mCurrentRequest = new RemoteRotationResolverService.RotationRequest(wrapper, request, cancellationSignalInternal, this.mLock);
        cancellationSignalInternal.setOnCancelListener(new CancellationSignal.OnCancelListener() { // from class: com.android.server.rotationresolver.RotationResolverManagerPerUserService$$ExternalSyntheticLambda0
            @Override // android.os.CancellationSignal.OnCancelListener
            public final void onCancel() {
                RotationResolverManagerPerUserService.this.m6394x1cc9464c();
            }
        });
        this.mRemoteService.resolveRotation(this.mCurrentRequest);
        this.mCurrentRequest.mIsDispatched = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$resolveRotationLocked$0$com-android-server-rotationresolver-RotationResolverManagerPerUserService  reason: not valid java name */
    public /* synthetic */ void m6394x1cc9464c() {
        synchronized (this.mLock) {
            RemoteRotationResolverService.RotationRequest rotationRequest = this.mCurrentRequest;
            if (rotationRequest != null && !rotationRequest.mIsFulfilled) {
                Slog.d(TAG, "Trying to cancel the remote request. Reason: Client cancelled.");
                this.mCurrentRequest.cancelInternal();
            }
        }
    }

    private void ensureRemoteServiceInitiated() {
        if (this.mRemoteService == null) {
            this.mRemoteService = new RemoteRotationResolverService(getContext(), this.mComponentName, getUserId(), 60000L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    boolean isServiceAvailableLocked() {
        if (this.mComponentName == null) {
            this.mComponentName = updateServiceInfoLocked();
        }
        return this.mComponentName != null;
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        try {
            ServiceInfo serviceInfo = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 128L, this.mUserId);
            if (serviceInfo != null) {
                String permission = serviceInfo.permission;
                if (!"android.permission.BIND_ROTATION_RESOLVER_SERVICE".equals(permission)) {
                    throw new SecurityException(String.format("Service %s requires %s permission. Found %s permission", serviceInfo.getComponentName(), "android.permission.BIND_ROTATION_RESOLVER_SERVICE", serviceInfo.permission));
                }
            }
            return serviceInfo;
        } catch (RemoteException e) {
            throw new PackageManager.NameNotFoundException("Could not get service for " + serviceComponent);
        }
    }

    private void cancelLocked() {
        RemoteRotationResolverService.RotationRequest rotationRequest = this.mCurrentRequest;
        if (rotationRequest == null) {
            return;
        }
        rotationRequest.cancelInternal();
        this.mCurrentRequest = null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractPerUserSystemService
    public void dumpLocked(String prefix, PrintWriter pw) {
        super.dumpLocked(prefix, pw);
        dumpInternal(new IndentingPrintWriter(pw, "  "));
    }

    void dumpInternal(IndentingPrintWriter ipw) {
        synchronized (this.mLock) {
            RemoteRotationResolverService remoteRotationResolverService = this.mRemoteService;
            if (remoteRotationResolverService != null) {
                remoteRotationResolverService.dump("", ipw);
            }
            RemoteRotationResolverService.RotationRequest rotationRequest = this.mCurrentRequest;
            if (rotationRequest != null) {
                rotationRequest.dump(ipw);
            }
        }
    }
}
