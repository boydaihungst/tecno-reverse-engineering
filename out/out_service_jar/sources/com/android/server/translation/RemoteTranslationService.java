package com.android.server.translation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.service.translation.ITranslationService;
import android.util.Slog;
import android.view.translation.TranslationContext;
import com.android.internal.infra.ServiceConnector;
import com.android.internal.os.IResultReceiver;
import java.util.function.Function;
/* loaded from: classes2.dex */
final class RemoteTranslationService extends ServiceConnector.Impl<ITranslationService> {
    private static final String TAG = RemoteTranslationService.class.getSimpleName();
    private static final long TIMEOUT_IDLE_UNBIND_MS = 0;
    private static final int TIMEOUT_REQUEST_MS = 5000;
    private final ComponentName mComponentName;
    private final long mIdleUnbindTimeoutMs;
    private final IBinder mRemoteCallback;
    private final int mRequestTimeoutMs;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteTranslationService(Context context, ComponentName serviceName, int userId, boolean bindInstantServiceAllowed, IBinder callback) {
        super(context, new Intent("android.service.translation.TranslationService").setComponent(serviceName), bindInstantServiceAllowed ? 4194304 : 0, userId, new Function() { // from class: com.android.server.translation.RemoteTranslationService$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ITranslationService.Stub.asInterface((IBinder) obj);
            }
        });
        this.mIdleUnbindTimeoutMs = 0L;
        this.mRequestTimeoutMs = 5000;
        this.mComponentName = serviceName;
        this.mRemoteCallback = callback;
        connect();
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    public void onServiceConnectionStatusChanged(ITranslationService service, boolean connected) {
        try {
            if (connected) {
                service.onConnected(this.mRemoteCallback);
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

    public void onSessionCreated(final TranslationContext translationContext, final int sessionId, final IResultReceiver resultReceiver) {
        run(new ServiceConnector.VoidJob() { // from class: com.android.server.translation.RemoteTranslationService$$ExternalSyntheticLambda0
            public final void runNoResult(Object obj) {
                ((ITranslationService) obj).onCreateTranslationSession(translationContext, sessionId, resultReceiver);
            }
        });
    }

    public void onTranslationCapabilitiesRequest(final int sourceFormat, final int targetFormat, final ResultReceiver resultReceiver) {
        run(new ServiceConnector.VoidJob() { // from class: com.android.server.translation.RemoteTranslationService$$ExternalSyntheticLambda2
            public final void runNoResult(Object obj) {
                ((ITranslationService) obj).onTranslationCapabilitiesRequest(sourceFormat, targetFormat, resultReceiver);
            }
        });
    }
}
