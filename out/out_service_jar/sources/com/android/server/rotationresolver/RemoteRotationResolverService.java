package com.android.server.rotationresolver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.rotationresolver.RotationResolverInternal;
import android.service.rotationresolver.IRotationResolverCallback;
import android.service.rotationresolver.IRotationResolverService;
import android.service.rotationresolver.RotationResolutionRequest;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import com.android.internal.infra.ServiceConnector;
import com.android.server.rotationresolver.RemoteRotationResolverService;
import java.lang.ref.WeakReference;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RemoteRotationResolverService extends ServiceConnector.Impl<IRotationResolverService> {
    private static final String TAG = RemoteRotationResolverService.class.getSimpleName();
    private final long mIdleUnbindTimeoutMs;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteRotationResolverService(Context context, ComponentName serviceName, int userId, long idleUnbindTimeoutMs) {
        super(context, new Intent("android.service.rotationresolver.RotationResolverService").setComponent(serviceName), 67112960, userId, new Function() { // from class: com.android.server.rotationresolver.RemoteRotationResolverService$$ExternalSyntheticLambda2
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return IRotationResolverService.Stub.asInterface((IBinder) obj);
            }
        });
        this.mIdleUnbindTimeoutMs = idleUnbindTimeoutMs;
        connect();
    }

    protected long getAutoDisconnectTimeoutMs() {
        return -1L;
    }

    public void resolveRotation(final RotationRequest request) {
        final RotationResolutionRequest remoteRequest = request.mRemoteRequest;
        post(new ServiceConnector.VoidJob() { // from class: com.android.server.rotationresolver.RemoteRotationResolverService$$ExternalSyntheticLambda0
            public final void runNoResult(Object obj) {
                IRotationResolverService iRotationResolverService = (IRotationResolverService) obj;
                iRotationResolverService.resolveRotation(RemoteRotationResolverService.RotationRequest.this.mIRotationResolverCallback, remoteRequest);
            }
        });
        getJobHandler().postDelayed(new Runnable() { // from class: com.android.server.rotationresolver.RemoteRotationResolverService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                RemoteRotationResolverService.lambda$resolveRotation$1(RemoteRotationResolverService.RotationRequest.this);
            }
        }, request.mRemoteRequest.getTimeoutMillis());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$resolveRotation$1(RotationRequest request) {
        synchronized (request.mLock) {
            if (!request.mIsFulfilled) {
                request.mCallbackInternal.onFailure(1);
                Slog.d(TAG, "Trying to cancel the remote request. Reason: Timed out.");
                request.cancelInternal();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class RotationRequest {
        final RotationResolverInternal.RotationResolverCallbackInternal mCallbackInternal;
        private ICancellationSignal mCancellation;
        private final CancellationSignal mCancellationSignalInternal;
        boolean mIsDispatched;
        boolean mIsFulfilled;
        private final Object mLock;
        final RotationResolutionRequest mRemoteRequest;
        private final IRotationResolverCallback mIRotationResolverCallback = new RotationResolverCallback(this);
        private final long mRequestStartTimeMillis = SystemClock.elapsedRealtime();

        /* JADX INFO: Access modifiers changed from: package-private */
        public RotationRequest(RotationResolverInternal.RotationResolverCallbackInternal callbackInternal, RotationResolutionRequest request, CancellationSignal cancellationSignal, Object lock) {
            this.mCallbackInternal = callbackInternal;
            this.mRemoteRequest = request;
            this.mCancellationSignalInternal = cancellationSignal;
            this.mLock = lock;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void cancelInternal() {
            Handler.getMain().post(new Runnable() { // from class: com.android.server.rotationresolver.RemoteRotationResolverService$RotationRequest$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteRotationResolverService.RotationRequest.this.m6392xfe9fa46b();
                }
            });
            this.mCallbackInternal.onFailure(0);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$cancelInternal$0$com-android-server-rotationresolver-RemoteRotationResolverService$RotationRequest  reason: not valid java name */
        public /* synthetic */ void m6392xfe9fa46b() {
            synchronized (this.mLock) {
                if (this.mIsFulfilled) {
                    return;
                }
                this.mIsFulfilled = true;
                try {
                    ICancellationSignal iCancellationSignal = this.mCancellation;
                    if (iCancellationSignal != null) {
                        iCancellationSignal.cancel();
                        this.mCancellation = null;
                    }
                } catch (RemoteException e) {
                    Slog.w(RemoteRotationResolverService.TAG, "Failed to cancel request in remote service.");
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void dump(IndentingPrintWriter ipw) {
            ipw.increaseIndent();
            ipw.println("is dispatched=" + this.mIsDispatched);
            ipw.println("is fulfilled:=" + this.mIsFulfilled);
            ipw.decreaseIndent();
        }

        /* loaded from: classes2.dex */
        private static class RotationResolverCallback extends IRotationResolverCallback.Stub {
            private final WeakReference<RotationRequest> mRequestWeakReference;

            RotationResolverCallback(RotationRequest request) {
                this.mRequestWeakReference = new WeakReference<>(request);
            }

            public void onSuccess(int rotation) {
                RotationRequest request = this.mRequestWeakReference.get();
                synchronized (request.mLock) {
                    if (request.mIsFulfilled) {
                        Slog.w(RemoteRotationResolverService.TAG, "Callback received after the rotation request is fulfilled.");
                        return;
                    }
                    request.mIsFulfilled = true;
                    request.mCallbackInternal.onSuccess(rotation);
                    long timeToCalculate = SystemClock.elapsedRealtime() - request.mRequestStartTimeMillis;
                    RotationResolverManagerService.logRotationStatsWithTimeToCalculate(request.mRemoteRequest.getProposedRotation(), request.mRemoteRequest.getCurrentRotation(), RotationResolverManagerService.surfaceRotationToProto(rotation), timeToCalculate);
                    Slog.d(RemoteRotationResolverService.TAG, "onSuccess:" + rotation);
                    Slog.d(RemoteRotationResolverService.TAG, "timeToCalculate:" + timeToCalculate);
                }
            }

            public void onFailure(int error) {
                RotationRequest request = this.mRequestWeakReference.get();
                synchronized (request.mLock) {
                    if (request.mIsFulfilled) {
                        Slog.w(RemoteRotationResolverService.TAG, "Callback received after the rotation request is fulfilled.");
                        return;
                    }
                    request.mIsFulfilled = true;
                    request.mCallbackInternal.onFailure(error);
                    long timeToCalculate = SystemClock.elapsedRealtime() - request.mRequestStartTimeMillis;
                    RotationResolverManagerService.logRotationStatsWithTimeToCalculate(request.mRemoteRequest.getProposedRotation(), request.mRemoteRequest.getCurrentRotation(), RotationResolverManagerService.errorCodeToProto(error), timeToCalculate);
                    Slog.d(RemoteRotationResolverService.TAG, "onFailure:" + error);
                    Slog.d(RemoteRotationResolverService.TAG, "timeToCalculate:" + timeToCalculate);
                }
            }

            public void onCancellable(ICancellationSignal cancellation) {
                RotationRequest request = this.mRequestWeakReference.get();
                synchronized (request.mLock) {
                    request.mCancellation = cancellation;
                    if (request.mCancellationSignalInternal.isCanceled()) {
                        try {
                            cancellation.cancel();
                        } catch (RemoteException e) {
                            Slog.w(RemoteRotationResolverService.TAG, "Failed to cancel the remote request.");
                        }
                    }
                }
            }
        }
    }
}
