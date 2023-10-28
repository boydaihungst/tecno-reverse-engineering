package android.service.textclassifier;

import android.Manifest;
import android.annotation.SystemApi;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
import android.service.textclassifier.ITextClassifierService;
import android.service.textclassifier.TextClassifierService;
import android.text.TextUtils;
import android.util.Slog;
import android.view.textclassifier.ConversationActions;
import android.view.textclassifier.SelectionEvent;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassificationContext;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassificationSessionId;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextClassifierEvent;
import android.view.textclassifier.TextLanguage;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
@SystemApi
/* loaded from: classes3.dex */
public abstract class TextClassifierService extends Service {
    public static final int CONNECTED = 0;
    public static final int DISCONNECTED = 1;
    private static final String KEY_RESULT = "key_result";
    private static final String LOG_TAG = "TextClassifierService";
    public static final String SERVICE_INTERFACE = "android.service.textclassifier.TextClassifierService";
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper(), null, true);
    private final ExecutorService mSingleThreadExecutor = Executors.newSingleThreadExecutor();
    private final ITextClassifierService.Stub mBinder = new AnonymousClass1();

    /* loaded from: classes3.dex */
    public interface Callback<T> {
        void onFailure(CharSequence charSequence);

        void onSuccess(T t);
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes3.dex */
    public @interface ConnectionState {
    }

    public abstract void onClassifyText(TextClassificationSessionId textClassificationSessionId, TextClassification.Request request, CancellationSignal cancellationSignal, Callback<TextClassification> callback);

    public abstract void onGenerateLinks(TextClassificationSessionId textClassificationSessionId, TextLinks.Request request, CancellationSignal cancellationSignal, Callback<TextLinks> callback);

    public abstract void onSuggestSelection(TextClassificationSessionId textClassificationSessionId, TextSelection.Request request, CancellationSignal cancellationSignal, Callback<TextSelection> callback);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: android.service.textclassifier.TextClassifierService$1  reason: invalid class name */
    /* loaded from: classes3.dex */
    public class AnonymousClass1 extends ITextClassifierService.Stub {
        private final CancellationSignal mCancellationSignal = new CancellationSignal();

        AnonymousClass1() {
        }

        @Override // android.service.textclassifier.ITextClassifierService
        public void onSuggestSelection(final TextClassificationSessionId sessionId, final TextSelection.Request request, final ITextClassifierCallback callback) {
            Objects.requireNonNull(request);
            Objects.requireNonNull(callback);
            TextClassifierService.this.mMainThreadHandler.post(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    TextClassifierService.AnonymousClass1.this.m3667x1cba9eff(sessionId, request, callback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSuggestSelection$0$android-service-textclassifier-TextClassifierService$1  reason: not valid java name */
        public /* synthetic */ void m3667x1cba9eff(TextClassificationSessionId sessionId, TextSelection.Request request, ITextClassifierCallback callback) {
            TextClassifierService.this.onSuggestSelection(sessionId, request, this.mCancellationSignal, new ProxyCallback(callback));
        }

        @Override // android.service.textclassifier.ITextClassifierService
        public void onClassifyText(final TextClassificationSessionId sessionId, final TextClassification.Request request, final ITextClassifierCallback callback) {
            Objects.requireNonNull(request);
            Objects.requireNonNull(callback);
            TextClassifierService.this.mMainThreadHandler.post(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    TextClassifierService.AnonymousClass1.this.m3660xfdedd495(sessionId, request, callback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onClassifyText$1$android-service-textclassifier-TextClassifierService$1  reason: not valid java name */
        public /* synthetic */ void m3660xfdedd495(TextClassificationSessionId sessionId, TextClassification.Request request, ITextClassifierCallback callback) {
            TextClassifierService.this.onClassifyText(sessionId, request, this.mCancellationSignal, new ProxyCallback(callback));
        }

        @Override // android.service.textclassifier.ITextClassifierService
        public void onGenerateLinks(final TextClassificationSessionId sessionId, final TextLinks.Request request, final ITextClassifierCallback callback) {
            Objects.requireNonNull(request);
            Objects.requireNonNull(callback);
            TextClassifierService.this.mMainThreadHandler.post(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    TextClassifierService.AnonymousClass1.this.m3664xc712ded9(sessionId, request, callback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onGenerateLinks$2$android-service-textclassifier-TextClassifierService$1  reason: not valid java name */
        public /* synthetic */ void m3664xc712ded9(TextClassificationSessionId sessionId, TextLinks.Request request, ITextClassifierCallback callback) {
            TextClassifierService.this.onGenerateLinks(sessionId, request, this.mCancellationSignal, new ProxyCallback(callback));
        }

        @Override // android.service.textclassifier.ITextClassifierService
        public void onSelectionEvent(final TextClassificationSessionId sessionId, final SelectionEvent event) {
            Objects.requireNonNull(event);
            TextClassifierService.this.mMainThreadHandler.post(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    TextClassifierService.AnonymousClass1.this.m3665xe0ca156(sessionId, event);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSelectionEvent$3$android-service-textclassifier-TextClassifierService$1  reason: not valid java name */
        public /* synthetic */ void m3665xe0ca156(TextClassificationSessionId sessionId, SelectionEvent event) {
            TextClassifierService.this.onSelectionEvent(sessionId, event);
        }

        @Override // android.service.textclassifier.ITextClassifierService
        public void onTextClassifierEvent(final TextClassificationSessionId sessionId, final TextClassifierEvent event) {
            Objects.requireNonNull(event);
            TextClassifierService.this.mMainThreadHandler.post(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    TextClassifierService.AnonymousClass1.this.m3668xcfb8ebcf(sessionId, event);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTextClassifierEvent$4$android-service-textclassifier-TextClassifierService$1  reason: not valid java name */
        public /* synthetic */ void m3668xcfb8ebcf(TextClassificationSessionId sessionId, TextClassifierEvent event) {
            TextClassifierService.this.onTextClassifierEvent(sessionId, event);
        }

        @Override // android.service.textclassifier.ITextClassifierService
        public void onDetectLanguage(final TextClassificationSessionId sessionId, final TextLanguage.Request request, final ITextClassifierCallback callback) {
            Objects.requireNonNull(request);
            Objects.requireNonNull(callback);
            TextClassifierService.this.mMainThreadHandler.post(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    TextClassifierService.AnonymousClass1.this.m3663xba608507(sessionId, request, callback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDetectLanguage$5$android-service-textclassifier-TextClassifierService$1  reason: not valid java name */
        public /* synthetic */ void m3663xba608507(TextClassificationSessionId sessionId, TextLanguage.Request request, ITextClassifierCallback callback) {
            TextClassifierService.this.onDetectLanguage(sessionId, request, this.mCancellationSignal, new ProxyCallback(callback));
        }

        @Override // android.service.textclassifier.ITextClassifierService
        public void onSuggestConversationActions(final TextClassificationSessionId sessionId, final ConversationActions.Request request, final ITextClassifierCallback callback) {
            Objects.requireNonNull(request);
            Objects.requireNonNull(callback);
            TextClassifierService.this.mMainThreadHandler.post(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    TextClassifierService.AnonymousClass1.this.m3666xe9f048b(sessionId, request, callback);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSuggestConversationActions$6$android-service-textclassifier-TextClassifierService$1  reason: not valid java name */
        public /* synthetic */ void m3666xe9f048b(TextClassificationSessionId sessionId, ConversationActions.Request request, ITextClassifierCallback callback) {
            TextClassifierService.this.onSuggestConversationActions(sessionId, request, this.mCancellationSignal, new ProxyCallback(callback));
        }

        @Override // android.service.textclassifier.ITextClassifierService
        public void onCreateTextClassificationSession(final TextClassificationContext context, final TextClassificationSessionId sessionId) {
            Objects.requireNonNull(context);
            Objects.requireNonNull(sessionId);
            TextClassifierService.this.mMainThreadHandler.post(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    TextClassifierService.AnonymousClass1.this.m3661xd735bbd1(context, sessionId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onCreateTextClassificationSession$7$android-service-textclassifier-TextClassifierService$1  reason: not valid java name */
        public /* synthetic */ void m3661xd735bbd1(TextClassificationContext context, TextClassificationSessionId sessionId) {
            TextClassifierService.this.onCreateTextClassificationSession(context, sessionId);
        }

        @Override // android.service.textclassifier.ITextClassifierService
        public void onDestroyTextClassificationSession(final TextClassificationSessionId sessionId) {
            TextClassifierService.this.mMainThreadHandler.post(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    TextClassifierService.AnonymousClass1.this.m3662xd39509b6(sessionId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDestroyTextClassificationSession$8$android-service-textclassifier-TextClassifierService$1  reason: not valid java name */
        public /* synthetic */ void m3662xd39509b6(TextClassificationSessionId sessionId) {
            TextClassifierService.this.onDestroyTextClassificationSession(sessionId);
        }

        @Override // android.service.textclassifier.ITextClassifierService
        public void onConnectedStateChanged(int connected) {
            Runnable runnable;
            Handler handler = TextClassifierService.this.mMainThreadHandler;
            if (connected == 0) {
                final TextClassifierService textClassifierService = TextClassifierService.this;
                runnable = new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        TextClassifierService.this.onConnected();
                    }
                };
            } else {
                final TextClassifierService textClassifierService2 = TextClassifierService.this;
                runnable = new Runnable() { // from class: android.service.textclassifier.TextClassifierService$1$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        TextClassifierService.this.onDisconnected();
                    }
                };
            }
            handler.post(runnable);
        }
    }

    @Override // android.app.Service
    public final IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return this.mBinder;
        }
        return null;
    }

    @Override // android.app.Service
    public boolean onUnbind(Intent intent) {
        onDisconnected();
        return super.onUnbind(intent);
    }

    public void onConnected() {
    }

    public void onDisconnected() {
    }

    public void onDetectLanguage(TextClassificationSessionId sessionId, final TextLanguage.Request request, CancellationSignal cancellationSignal, final Callback<TextLanguage> callback) {
        this.mSingleThreadExecutor.submit(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TextClassifierService.this.m3658xb510dddf(callback, request);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onDetectLanguage$0$android-service-textclassifier-TextClassifierService  reason: not valid java name */
    public /* synthetic */ void m3658xb510dddf(Callback callback, TextLanguage.Request request) {
        callback.onSuccess(getLocalTextClassifier().detectLanguage(request));
    }

    public void onSuggestConversationActions(TextClassificationSessionId sessionId, final ConversationActions.Request request, CancellationSignal cancellationSignal, final Callback<ConversationActions> callback) {
        this.mSingleThreadExecutor.submit(new Runnable() { // from class: android.service.textclassifier.TextClassifierService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TextClassifierService.this.m3659xf316e63(callback, request);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSuggestConversationActions$1$android-service-textclassifier-TextClassifierService  reason: not valid java name */
    public /* synthetic */ void m3659xf316e63(Callback callback, ConversationActions.Request request) {
        callback.onSuccess(getLocalTextClassifier().suggestConversationActions(request));
    }

    @Deprecated
    public void onSelectionEvent(TextClassificationSessionId sessionId, SelectionEvent event) {
    }

    public void onTextClassifierEvent(TextClassificationSessionId sessionId, TextClassifierEvent event) {
    }

    public void onCreateTextClassificationSession(TextClassificationContext context, TextClassificationSessionId sessionId) {
    }

    public void onDestroyTextClassificationSession(TextClassificationSessionId sessionId) {
    }

    @Deprecated
    public final TextClassifier getLocalTextClassifier() {
        return TextClassifier.NO_OP;
    }

    public static TextClassifier getDefaultTextClassifierImplementation(Context context) {
        String defaultTextClassifierPackageName = context.getPackageManager().getDefaultTextClassifierPackageName();
        if (TextUtils.isEmpty(defaultTextClassifierPackageName)) {
            return TextClassifier.NO_OP;
        }
        if (defaultTextClassifierPackageName.equals(context.getPackageName())) {
            throw new RuntimeException("The default text classifier itself should not call thegetDefaultTextClassifierImplementation() method.");
        }
        TextClassificationManager tcm = (TextClassificationManager) context.getSystemService(TextClassificationManager.class);
        return tcm.getTextClassifier(2);
    }

    public static <T extends Parcelable> T getResponse(Bundle bundle) {
        return (T) bundle.getParcelable(KEY_RESULT);
    }

    public static <T extends Parcelable> void putResponse(Bundle bundle, T response) {
        bundle.putParcelable(KEY_RESULT, response);
    }

    public static ComponentName getServiceComponentName(Context context, String packageName, int resolveFlags) {
        Intent intent = new Intent(SERVICE_INTERFACE).setPackage(packageName);
        ResolveInfo ri = context.getPackageManager().resolveService(intent, resolveFlags);
        if (ri == null || ri.serviceInfo == null) {
            Slog.w(LOG_TAG, String.format("Package or service not found in package %s for user %d", packageName, Integer.valueOf(context.getUserId())));
            return null;
        }
        ServiceInfo si = ri.serviceInfo;
        String permission = si.permission;
        if (Manifest.permission.BIND_TEXTCLASSIFIER_SERVICE.equals(permission)) {
            return si.getComponentName();
        }
        Slog.w(LOG_TAG, String.format("Service %s should require %s permission. Found %s permission", si.getComponentName(), Manifest.permission.BIND_TEXTCLASSIFIER_SERVICE, si.permission));
        return null;
    }

    /* loaded from: classes3.dex */
    private static final class ProxyCallback<T extends Parcelable> implements Callback<T> {
        private ITextClassifierCallback mTextClassifierCallback;

        /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: android.service.textclassifier.TextClassifierService$ProxyCallback<T extends android.os.Parcelable> */
        /* JADX WARN: Multi-variable type inference failed */
        @Override // android.service.textclassifier.TextClassifierService.Callback
        public /* bridge */ /* synthetic */ void onSuccess(Object obj) {
            onSuccess((ProxyCallback<T>) ((Parcelable) obj));
        }

        private ProxyCallback(ITextClassifierCallback textClassifierCallback) {
            this.mTextClassifierCallback = (ITextClassifierCallback) Objects.requireNonNull(textClassifierCallback);
        }

        public void onSuccess(T result) {
            try {
                Bundle bundle = new Bundle(1);
                bundle.putParcelable(TextClassifierService.KEY_RESULT, result);
                this.mTextClassifierCallback.onSuccess(bundle);
            } catch (RemoteException e) {
                Slog.d(TextClassifierService.LOG_TAG, "Error calling callback");
            }
        }

        @Override // android.service.textclassifier.TextClassifierService.Callback
        public void onFailure(CharSequence error) {
            try {
                Slog.w(TextClassifierService.LOG_TAG, "Request fail: " + ((Object) error));
                this.mTextClassifierCallback.onFailure();
            } catch (RemoteException e) {
                Slog.d(TextClassifierService.LOG_TAG, "Error calling callback");
            }
        }
    }
}
