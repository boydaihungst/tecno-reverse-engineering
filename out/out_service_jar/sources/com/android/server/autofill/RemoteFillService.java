package com.android.server.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Handler;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.RemoteException;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.IAutoFillService;
import android.service.autofill.IFillCallback;
import android.service.autofill.ISaveCallback;
import android.service.autofill.SaveRequest;
import android.util.Slog;
import com.android.internal.infra.AbstractRemoteService;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.infra.ServiceConnector;
import com.android.internal.os.IResultReceiver;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class RemoteFillService extends ServiceConnector.Impl<IAutoFillService> {
    private static final String TAG = "RemoteFillService";
    private static final long TIMEOUT_IDLE_BIND_MILLIS = 5000;
    private static final long TIMEOUT_REMOTE_REQUEST_MILLIS = 5000;
    private final FillServiceCallbacks mCallbacks;
    private final ComponentName mComponentName;
    private final Object mLock;
    private CompletableFuture<FillResponse> mPendingFillRequest;
    private int mPendingFillRequestId;

    /* loaded from: classes.dex */
    public interface FillServiceCallbacks extends AbstractRemoteService.VultureCallback<RemoteFillService> {
        void onFillRequestFailure(int i, CharSequence charSequence);

        void onFillRequestSuccess(int i, FillResponse fillResponse, String str, int i2);

        void onFillRequestTimeout(int i);

        void onSaveRequestFailure(CharSequence charSequence, String str);

        void onSaveRequestSuccess(String str, IntentSender intentSender);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteFillService(Context context, ComponentName componentName, int userId, FillServiceCallbacks callbacks, boolean bindInstantServiceAllowed) {
        super(context, new Intent("android.service.autofill.AutofillService").setComponent(componentName), (bindInstantServiceAllowed ? 4194304 : 0) | 1048576, userId, new Function() { // from class: com.android.server.autofill.RemoteFillService$$ExternalSyntheticLambda2
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return IAutoFillService.Stub.asInterface((IBinder) obj);
            }
        });
        this.mLock = new Object();
        this.mPendingFillRequestId = Integer.MIN_VALUE;
        this.mCallbacks = callbacks;
        this.mComponentName = componentName;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public void onServiceConnectionStatusChanged(IAutoFillService service, boolean connected) {
        try {
            service.onConnectedStateChanged(connected);
        } catch (Exception e) {
            Slog.w(TAG, "Exception calling onConnectedStateChanged(" + connected + "): " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchCancellationSignal(ICancellationSignal signal) {
        if (signal == null) {
            return;
        }
        try {
            signal.cancel();
        } catch (RemoteException e) {
            Slog.e(TAG, "Error requesting a cancellation", e);
        }
    }

    protected long getAutoDisconnectTimeoutMs() {
        return 5000L;
    }

    /* JADX DEBUG: Method merged with bridge method */
    public void addLast(ServiceConnector.Job<IAutoFillService, ?> iAutoFillServiceJob) {
        cancelPendingJobs();
        super.addLast(iAutoFillServiceJob);
    }

    public int cancelCurrentRequest() {
        int i;
        synchronized (this.mLock) {
            CompletableFuture<FillResponse> completableFuture = this.mPendingFillRequest;
            if (completableFuture != null && completableFuture.cancel(false)) {
                i = this.mPendingFillRequestId;
            } else {
                i = Integer.MIN_VALUE;
            }
        }
        return i;
    }

    public void onFillRequest(final FillRequest request) {
        final AtomicReference<ICancellationSignal> cancellationSink = new AtomicReference<>();
        final AtomicReference<CompletableFuture<FillResponse>> futureRef = new AtomicReference<>();
        AndroidFuture orTimeout = postAsync(new ServiceConnector.Job() { // from class: com.android.server.autofill.RemoteFillService$$ExternalSyntheticLambda4
            public final Object run(Object obj) {
                return RemoteFillService.this.m2021xef3141a9(request, futureRef, cancellationSink, (IAutoFillService) obj);
            }
        }).orTimeout(5000L, TimeUnit.MILLISECONDS);
        futureRef.set(orTimeout);
        synchronized (this.mLock) {
            this.mPendingFillRequest = orTimeout;
            this.mPendingFillRequestId = request.getId();
        }
        orTimeout.whenComplete(new BiConsumer() { // from class: com.android.server.autofill.RemoteFillService$$ExternalSyntheticLambda5
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                RemoteFillService.this.m2023xda258e2b(request, cancellationSink, (FillResponse) obj, (Throwable) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onFillRequest$0$com-android-server-autofill-RemoteFillService  reason: not valid java name */
    public /* synthetic */ CompletableFuture m2021xef3141a9(FillRequest request, final AtomicReference futureRef, final AtomicReference cancellationSink, IAutoFillService remoteService) throws Exception {
        if (Helper.sVerbose) {
            Slog.v(TAG, "calling onFillRequest() for id=" + request.getId());
        }
        final CompletableFuture<FillResponse> fillRequest = new CompletableFuture<>();
        remoteService.onFillRequest(request, new IFillCallback.Stub() { // from class: com.android.server.autofill.RemoteFillService.1
            public void onCancellable(ICancellationSignal cancellation) {
                CompletableFuture future = (CompletableFuture) futureRef.get();
                if (future != null && future.isCancelled()) {
                    RemoteFillService.this.dispatchCancellationSignal(cancellation);
                } else {
                    cancellationSink.set(cancellation);
                }
            }

            public void onSuccess(FillResponse response) {
                fillRequest.complete(response);
            }

            public void onFailure(int requestId, CharSequence message) {
                String errorMessage = message == null ? "" : String.valueOf(message);
                fillRequest.completeExceptionally(new RuntimeException(errorMessage));
            }
        });
        return fillRequest;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onFillRequest$2$com-android-server-autofill-RemoteFillService  reason: not valid java name */
    public /* synthetic */ void m2023xda258e2b(final FillRequest request, final AtomicReference cancellationSink, final FillResponse res, final Throwable err) {
        Handler.getMain().post(new Runnable() { // from class: com.android.server.autofill.RemoteFillService$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                RemoteFillService.this.m2022x64ab67ea(err, request, res, cancellationSink);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onFillRequest$1$com-android-server-autofill-RemoteFillService  reason: not valid java name */
    public /* synthetic */ void m2022x64ab67ea(Throwable err, FillRequest request, FillResponse res, AtomicReference cancellationSink) {
        synchronized (this.mLock) {
            this.mPendingFillRequest = null;
            this.mPendingFillRequestId = Integer.MIN_VALUE;
        }
        if (err == null) {
            this.mCallbacks.onFillRequestSuccess(request.getId(), res, this.mComponentName.getPackageName(), request.getFlags());
            return;
        }
        Slog.e(TAG, "Error calling on fill request", err);
        if (err instanceof TimeoutException) {
            dispatchCancellationSignal((ICancellationSignal) cancellationSink.get());
            this.mCallbacks.onFillRequestTimeout(request.getId());
        } else if (err instanceof CancellationException) {
            dispatchCancellationSignal((ICancellationSignal) cancellationSink.get());
        } else {
            this.mCallbacks.onFillRequestFailure(request.getId(), err.getMessage());
        }
    }

    public void onSaveRequest(final SaveRequest request) {
        postAsync(new ServiceConnector.Job() { // from class: com.android.server.autofill.RemoteFillService$$ExternalSyntheticLambda0
            public final Object run(Object obj) {
                return RemoteFillService.this.m2024x30be7732(request, (IAutoFillService) obj);
            }
        }).orTimeout(5000L, TimeUnit.MILLISECONDS).whenComplete(new BiConsumer() { // from class: com.android.server.autofill.RemoteFillService$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                RemoteFillService.this.m2026x1bb2c3b4((IntentSender) obj, (Throwable) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSaveRequest$3$com-android-server-autofill-RemoteFillService  reason: not valid java name */
    public /* synthetic */ CompletableFuture m2024x30be7732(SaveRequest request, IAutoFillService service) throws Exception {
        if (Helper.sVerbose) {
            Slog.v(TAG, "calling onSaveRequest()");
        }
        final CompletableFuture<IntentSender> save = new CompletableFuture<>();
        service.onSaveRequest(request, new ISaveCallback.Stub() { // from class: com.android.server.autofill.RemoteFillService.2
            public void onSuccess(IntentSender intentSender) {
                save.complete(intentSender);
            }

            public void onFailure(CharSequence message) {
                save.completeExceptionally(new RuntimeException(String.valueOf(message)));
            }
        });
        return save;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSaveRequest$5$com-android-server-autofill-RemoteFillService  reason: not valid java name */
    public /* synthetic */ void m2026x1bb2c3b4(final IntentSender res, final Throwable err) {
        Handler.getMain().post(new Runnable() { // from class: com.android.server.autofill.RemoteFillService$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                RemoteFillService.this.m2025xa6389d73(err, res);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSaveRequest$4$com-android-server-autofill-RemoteFillService  reason: not valid java name */
    public /* synthetic */ void m2025xa6389d73(Throwable err, IntentSender res) {
        if (err == null) {
            this.mCallbacks.onSaveRequestSuccess(this.mComponentName.getPackageName(), res);
        } else {
            this.mCallbacks.onSaveRequestFailure(this.mComponentName.getPackageName(), err.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSavedPasswordCountRequest(final IResultReceiver receiver) {
        run(new ServiceConnector.VoidJob() { // from class: com.android.server.autofill.RemoteFillService$$ExternalSyntheticLambda3
            public final void runNoResult(Object obj) {
                ((IAutoFillService) obj).onSavedPasswordCountRequest(receiver);
            }
        });
    }

    public void destroy() {
        unbind();
    }
}
