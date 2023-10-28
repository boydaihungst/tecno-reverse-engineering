package com.android.server.autofill;

import android.app.AppGlobals;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.service.autofill.Dataset;
import android.service.autofill.augmented.IAugmentedAutofillService;
import android.service.autofill.augmented.IFillCallback;
import android.util.Pair;
import android.util.Slog;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManagerClient;
import android.view.inputmethod.InlineSuggestionsRequest;
import com.android.internal.infra.AbstractRemoteService;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.infra.ServiceConnector;
import com.android.internal.os.IResultReceiver;
import com.android.server.autofill.ui.InlineFillUi;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class RemoteAugmentedAutofillService extends ServiceConnector.Impl<IAugmentedAutofillService> {
    private static final String TAG = RemoteAugmentedAutofillService.class.getSimpleName();
    private final RemoteAugmentedAutofillServiceCallbacks mCallbacks;
    private final ComponentName mComponentName;
    private final int mIdleUnbindTimeoutMs;
    private final int mRequestTimeoutMs;
    private final AutofillUriGrantsManager mUriGrantsManager;

    /* loaded from: classes.dex */
    public interface RemoteAugmentedAutofillServiceCallbacks extends AbstractRemoteService.VultureCallback<RemoteAugmentedAutofillService> {
        void logAugmentedAutofillAuthenticationSelected(int i, String str, Bundle bundle);

        void logAugmentedAutofillSelected(int i, String str, Bundle bundle);

        void logAugmentedAutofillShown(int i, Bundle bundle);

        void resetLastResponse();

        void setLastResponse(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteAugmentedAutofillService(Context context, int serviceUid, ComponentName serviceName, int userId, RemoteAugmentedAutofillServiceCallbacks callbacks, boolean bindInstantServiceAllowed, boolean verbose, int idleUnbindTimeoutMs, int requestTimeoutMs) {
        super(context, new Intent("android.service.autofill.augmented.AugmentedAutofillService").setComponent(serviceName), bindInstantServiceAllowed ? 4194304 : 0, userId, new Function() { // from class: com.android.server.autofill.RemoteAugmentedAutofillService$$ExternalSyntheticLambda2
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return IAugmentedAutofillService.Stub.asInterface((IBinder) obj);
            }
        });
        this.mIdleUnbindTimeoutMs = idleUnbindTimeoutMs;
        this.mRequestTimeoutMs = requestTimeoutMs;
        this.mComponentName = serviceName;
        this.mCallbacks = callbacks;
        this.mUriGrantsManager = new AutofillUriGrantsManager(serviceUid);
        connect();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Pair<ServiceInfo, ComponentName> getComponentName(String componentName, int userId, boolean isTemporary) {
        int flags = isTemporary ? 128 : 128 | 1048576;
        try {
            ComponentName serviceComponent = ComponentName.unflattenFromString(componentName);
            ServiceInfo serviceInfo = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, flags, userId);
            if (serviceInfo == null) {
                Slog.e(TAG, "Bad service name for flags " + flags + ": " + componentName);
                return null;
            }
            return new Pair<>(serviceInfo, serviceComponent);
        } catch (Exception e) {
            Slog.e(TAG, "Error getting service info for '" + componentName + "': " + e);
            return null;
        }
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public AutofillUriGrantsManager getAutofillUriGrantsManager() {
        return this.mUriGrantsManager;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public void onServiceConnectionStatusChanged(IAugmentedAutofillService service, boolean connected) {
        try {
            if (connected) {
                service.onConnected(Helper.sDebug, Helper.sVerbose);
            } else {
                service.onDisconnected();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Exception calling onServiceConnectionStatusChanged(" + connected + "): ", e);
        }
    }

    protected long getAutoDisconnectTimeoutMs() {
        return this.mIdleUnbindTimeoutMs;
    }

    public void onRequestAutofillLocked(final int sessionId, final IAutoFillManagerClient client, final int taskId, final ComponentName activityComponent, final IBinder activityToken, final AutofillId focusedId, final AutofillValue focusedValue, final InlineSuggestionsRequest inlineSuggestionsRequest, final Function<InlineFillUi, Boolean> inlineSuggestionsCallback, final Runnable onErrorCallback, final RemoteInlineSuggestionRenderService remoteRenderService, final int userId) {
        final long requestTime = SystemClock.elapsedRealtime();
        final AtomicReference<ICancellationSignal> cancellationRef = new AtomicReference<>();
        postAsync(new ServiceConnector.Job() { // from class: com.android.server.autofill.RemoteAugmentedAutofillService$$ExternalSyntheticLambda3
            public final Object run(Object obj) {
                return RemoteAugmentedAutofillService.this.m2018xae22101d(client, sessionId, taskId, activityComponent, focusedId, focusedValue, requestTime, inlineSuggestionsRequest, inlineSuggestionsCallback, onErrorCallback, remoteRenderService, userId, activityToken, cancellationRef, (IAugmentedAutofillService) obj);
            }
        }).orTimeout(this.mRequestTimeoutMs, TimeUnit.MILLISECONDS).whenComplete(new BiConsumer() { // from class: com.android.server.autofill.RemoteAugmentedAutofillService$$ExternalSyntheticLambda4
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                RemoteAugmentedAutofillService.this.m2019xb425db7c(cancellationRef, activityComponent, sessionId, (Void) obj, (Throwable) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onRequestAutofillLocked$0$com-android-server-autofill-RemoteAugmentedAutofillService  reason: not valid java name */
    public /* synthetic */ CompletableFuture m2018xae22101d(final IAutoFillManagerClient client, final int sessionId, final int taskId, final ComponentName activityComponent, final AutofillId focusedId, final AutofillValue focusedValue, final long requestTime, final InlineSuggestionsRequest inlineSuggestionsRequest, final Function inlineSuggestionsCallback, final Runnable onErrorCallback, final RemoteInlineSuggestionRenderService remoteRenderService, final int userId, final IBinder activityToken, final AtomicReference cancellationRef, final IAugmentedAutofillService service) throws Exception {
        final AndroidFuture<Void> requestAutofill = new AndroidFuture<>();
        client.getAugmentedAutofillClient(new IResultReceiver.Stub() { // from class: com.android.server.autofill.RemoteAugmentedAutofillService.1
            public void send(int resultCode, Bundle resultData) throws RemoteException {
                IBinder realClient = resultData.getBinder("android.view.autofill.extra.AUGMENTED_AUTOFILL_CLIENT");
                service.onFillRequest(sessionId, realClient, taskId, activityComponent, focusedId, focusedValue, requestTime, inlineSuggestionsRequest, new IFillCallback.Stub() { // from class: com.android.server.autofill.RemoteAugmentedAutofillService.1.1
                    public void onSuccess(List<Dataset> list, Bundle clientState, boolean showingFillWindow) {
                        RemoteAugmentedAutofillService.this.mCallbacks.resetLastResponse();
                        RemoteAugmentedAutofillService.this.maybeRequestShowInlineSuggestions(sessionId, inlineSuggestionsRequest, list, clientState, focusedId, focusedValue, inlineSuggestionsCallback, client, onErrorCallback, remoteRenderService, userId, activityComponent, activityToken);
                        if (!showingFillWindow) {
                            requestAutofill.complete((Object) null);
                        }
                    }

                    public boolean isCompleted() {
                        return requestAutofill.isDone() && !requestAutofill.isCancelled();
                    }

                    public void onCancellable(ICancellationSignal cancellation) {
                        if (requestAutofill.isCancelled()) {
                            RemoteAugmentedAutofillService.this.dispatchCancellation(cancellation);
                        } else {
                            cancellationRef.set(cancellation);
                        }
                    }

                    public void cancel() {
                        requestAutofill.cancel(true);
                    }
                });
            }
        });
        return requestAutofill;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onRequestAutofillLocked$1$com-android-server-autofill-RemoteAugmentedAutofillService  reason: not valid java name */
    public /* synthetic */ void m2019xb425db7c(AtomicReference cancellationRef, ComponentName activityComponent, int sessionId, Void res, Throwable err) {
        if (err instanceof CancellationException) {
            dispatchCancellation((ICancellationSignal) cancellationRef.get());
        } else if (err instanceof TimeoutException) {
            Slog.w(TAG, "PendingAutofillRequest timed out (" + this.mRequestTimeoutMs + "ms) for " + this);
            dispatchCancellation((ICancellationSignal) cancellationRef.get());
            ComponentName componentName = this.mComponentName;
            if (componentName != null) {
                android.service.autofill.augmented.Helper.logResponse(15, componentName.getPackageName(), activityComponent, sessionId, this.mRequestTimeoutMs);
            }
        } else if (err != null) {
            Slog.e(TAG, "exception handling getAugmentedAutofillClient() for " + sessionId + ": ", err);
        }
    }

    void dispatchCancellation(final ICancellationSignal cancellation) {
        if (cancellation == null) {
            return;
        }
        Handler.getMain().post(new Runnable() { // from class: com.android.server.autofill.RemoteAugmentedAutofillService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                RemoteAugmentedAutofillService.lambda$dispatchCancellation$2(cancellation);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dispatchCancellation$2(ICancellationSignal cancellation) {
        try {
            cancellation.cancel();
        } catch (RemoteException e) {
            Slog.e(TAG, "Error requesting a cancellation", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void maybeRequestShowInlineSuggestions(final int sessionId, InlineSuggestionsRequest request, List<Dataset> inlineSuggestionsData, final Bundle clientState, final AutofillId focusedId, AutofillValue focusedValue, final Function<InlineFillUi, Boolean> inlineSuggestionsCallback, final IAutoFillManagerClient client, final Runnable onErrorCallback, RemoteInlineSuggestionRenderService remoteRenderService, final int userId, final ComponentName targetActivity, final IBinder targetActivityToken) {
        Function<InlineFillUi, Boolean> function;
        if (inlineSuggestionsData == null || inlineSuggestionsData.isEmpty() || inlineSuggestionsCallback == null || request == null) {
            function = inlineSuggestionsCallback;
        } else if (remoteRenderService != null) {
            this.mCallbacks.setLastResponse(sessionId);
            String filterText = (focusedValue == null || !focusedValue.isText()) ? null : focusedValue.getTextValue().toString();
            InlineFillUi.InlineFillUiInfo inlineFillUiInfo = new InlineFillUi.InlineFillUiInfo(request, focusedId, filterText, remoteRenderService, userId, sessionId);
            InlineFillUi inlineFillUi = InlineFillUi.forAugmentedAutofill(inlineFillUiInfo, inlineSuggestionsData, new InlineFillUi.InlineSuggestionUiCallback() { // from class: com.android.server.autofill.RemoteAugmentedAutofillService.2
                @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
                public void autofill(Dataset dataset, int datasetIndex) {
                    boolean hideHighlight = true;
                    if (dataset.getAuthentication() != null) {
                        RemoteAugmentedAutofillService.this.mCallbacks.logAugmentedAutofillAuthenticationSelected(sessionId, dataset.getId(), clientState);
                        IntentSender action = dataset.getAuthentication();
                        int authenticationId = AutofillManager.makeAuthenticationId(1, datasetIndex);
                        Intent fillInIntent = new Intent();
                        fillInIntent.putExtra("android.view.autofill.extra.CLIENT_STATE", clientState);
                        try {
                            client.authenticate(sessionId, authenticationId, action, fillInIntent, false);
                            return;
                        } catch (RemoteException e) {
                            Slog.w(RemoteAugmentedAutofillService.TAG, "Error starting auth flow");
                            inlineSuggestionsCallback.apply(InlineFillUi.emptyUi(focusedId));
                            return;
                        }
                    }
                    RemoteAugmentedAutofillService.this.mCallbacks.logAugmentedAutofillSelected(sessionId, dataset.getId(), clientState);
                    try {
                        ArrayList<AutofillId> fieldIds = dataset.getFieldIds();
                        ClipData content = dataset.getFieldContent();
                        if (content != null) {
                            RemoteAugmentedAutofillService.this.mUriGrantsManager.grantUriPermissions(targetActivity, targetActivityToken, userId, content);
                            AutofillId fieldId = fieldIds.get(0);
                            if (Helper.sDebug) {
                                Slog.d(RemoteAugmentedAutofillService.TAG, "Calling client autofillContent(): id=" + fieldId + ", content=" + content);
                            }
                            client.autofillContent(sessionId, fieldId, content);
                        } else {
                            int size = fieldIds.size();
                            if (size != 1 || !fieldIds.get(0).equals(focusedId)) {
                                hideHighlight = false;
                            }
                            if (Helper.sDebug) {
                                Slog.d(RemoteAugmentedAutofillService.TAG, "Calling client autofill(): ids=" + fieldIds + ", values=" + dataset.getFieldValues());
                            }
                            client.autofill(sessionId, fieldIds, dataset.getFieldValues(), hideHighlight);
                        }
                        inlineSuggestionsCallback.apply(InlineFillUi.emptyUi(focusedId));
                    } catch (RemoteException e2) {
                        Slog.w(RemoteAugmentedAutofillService.TAG, "Encounter exception autofilling the values");
                    }
                }

                @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
                public void authenticate(int requestId, int datasetIndex) {
                    Slog.e(RemoteAugmentedAutofillService.TAG, "authenticate not implemented for augmented autofill");
                }

                @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
                public void startIntentSender(IntentSender intentSender) {
                    try {
                        client.startIntentSender(intentSender, new Intent());
                    } catch (RemoteException e) {
                        Slog.w(RemoteAugmentedAutofillService.TAG, "RemoteException starting intent sender");
                    }
                }

                @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
                public void onError() {
                    onErrorCallback.run();
                }
            });
            if (inlineSuggestionsCallback.apply(inlineFillUi).booleanValue()) {
                this.mCallbacks.logAugmentedAutofillShown(sessionId, clientState);
                return;
            }
            return;
        } else {
            function = inlineSuggestionsCallback;
        }
        if (function != null && request != null) {
            function.apply(InlineFillUi.emptyUi(focusedId));
        }
    }

    public String toString() {
        return "RemoteAugmentedAutofillService[" + ComponentName.flattenToShortString(this.mComponentName) + "]";
    }

    public void onDestroyAutofillWindowsRequest() {
        run(new ServiceConnector.VoidJob() { // from class: com.android.server.autofill.RemoteAugmentedAutofillService$$ExternalSyntheticLambda0
            public final void runNoResult(Object obj) {
                ((IAugmentedAutofillService) obj).onDestroyAllFillWindowsRequest();
            }
        });
    }
}
