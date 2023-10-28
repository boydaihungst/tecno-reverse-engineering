package com.android.server.ambientcontext;

import android.app.ambientcontext.AmbientContextEventRequest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.service.ambientcontext.IAmbientContextDetectionService;
import android.util.Slog;
import com.android.internal.infra.ServiceConnector;
import java.util.function.Function;
/* loaded from: classes.dex */
final class RemoteAmbientContextDetectionService extends ServiceConnector.Impl<IAmbientContextDetectionService> {
    private static final String TAG = RemoteAmbientContextDetectionService.class.getSimpleName();

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteAmbientContextDetectionService(Context context, ComponentName serviceName, int userId) {
        super(context, new Intent("android.service.ambientcontext.AmbientContextDetectionService").setComponent(serviceName), 67112960, userId, new Function() { // from class: com.android.server.ambientcontext.RemoteAmbientContextDetectionService$$ExternalSyntheticLambda3
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return IAmbientContextDetectionService.Stub.asInterface((IBinder) obj);
            }
        });
        connect();
    }

    protected long getAutoDisconnectTimeoutMs() {
        return -1L;
    }

    public void startDetection(final AmbientContextEventRequest request, final String packageName, final RemoteCallback detectionResultCallback, final RemoteCallback statusCallback) {
        Slog.i(TAG, "Start detection for " + request.getEventTypes());
        post(new ServiceConnector.VoidJob() { // from class: com.android.server.ambientcontext.RemoteAmbientContextDetectionService$$ExternalSyntheticLambda0
            public final void runNoResult(Object obj) {
                ((IAmbientContextDetectionService) obj).startDetection(request, packageName, detectionResultCallback, statusCallback);
            }
        });
    }

    public void stopDetection(final String packageName) {
        Slog.i(TAG, "Stop detection for " + packageName);
        post(new ServiceConnector.VoidJob() { // from class: com.android.server.ambientcontext.RemoteAmbientContextDetectionService$$ExternalSyntheticLambda1
            public final void runNoResult(Object obj) {
                ((IAmbientContextDetectionService) obj).stopDetection(packageName);
            }
        });
    }

    public void queryServiceStatus(final int[] eventTypes, final String packageName, final RemoteCallback callback) {
        Slog.i(TAG, "Query status for " + packageName);
        post(new ServiceConnector.VoidJob() { // from class: com.android.server.ambientcontext.RemoteAmbientContextDetectionService$$ExternalSyntheticLambda2
            public final void runNoResult(Object obj) {
                ((IAmbientContextDetectionService) obj).queryServiceStatus(eventTypes, packageName, callback);
            }
        });
    }
}
