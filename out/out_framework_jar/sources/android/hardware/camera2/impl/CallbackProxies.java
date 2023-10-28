package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.impl.CallbackProxies;
import android.os.Binder;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public class CallbackProxies {

    /* loaded from: classes.dex */
    public static class SessionStateCallbackProxy extends CameraCaptureSession.StateCallback {
        private final CameraCaptureSession.StateCallback mCallback;
        private final Executor mExecutor;

        public SessionStateCallbackProxy(Executor executor, CameraCaptureSession.StateCallback callback) {
            this.mExecutor = (Executor) Preconditions.checkNotNull(executor, "executor must not be null");
            this.mCallback = (CameraCaptureSession.StateCallback) Preconditions.checkNotNull(callback, "callback must not be null");
        }

        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onConfigured(final CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.hardware.camera2.impl.CallbackProxies$SessionStateCallbackProxy$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        CallbackProxies.SessionStateCallbackProxy.this.m1295x11487bad(session);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onConfigured$0$android-hardware-camera2-impl-CallbackProxies$SessionStateCallbackProxy  reason: not valid java name */
        public /* synthetic */ void m1295x11487bad(CameraCaptureSession session) {
            this.mCallback.onConfigured(session);
        }

        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onConfigureFailed(final CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.hardware.camera2.impl.CallbackProxies$SessionStateCallbackProxy$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        CallbackProxies.SessionStateCallbackProxy.this.m1294x11b64e7b(session);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onConfigureFailed$1$android-hardware-camera2-impl-CallbackProxies$SessionStateCallbackProxy  reason: not valid java name */
        public /* synthetic */ void m1294x11b64e7b(CameraCaptureSession session) {
            this.mCallback.onConfigureFailed(session);
        }

        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onReady(final CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.hardware.camera2.impl.CallbackProxies$SessionStateCallbackProxy$$ExternalSyntheticLambda5
                    @Override // java.lang.Runnable
                    public final void run() {
                        CallbackProxies.SessionStateCallbackProxy.this.m1296x42cf2d5c(session);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReady$2$android-hardware-camera2-impl-CallbackProxies$SessionStateCallbackProxy  reason: not valid java name */
        public /* synthetic */ void m1296x42cf2d5c(CameraCaptureSession session) {
            this.mCallback.onReady(session);
        }

        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onActive(final CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.hardware.camera2.impl.CallbackProxies$SessionStateCallbackProxy$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        CallbackProxies.SessionStateCallbackProxy.this.m1291x5bf37978(session);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onActive$3$android-hardware-camera2-impl-CallbackProxies$SessionStateCallbackProxy  reason: not valid java name */
        public /* synthetic */ void m1291x5bf37978(CameraCaptureSession session) {
            this.mCallback.onActive(session);
        }

        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onCaptureQueueEmpty(final CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.hardware.camera2.impl.CallbackProxies$SessionStateCallbackProxy$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        CallbackProxies.SessionStateCallbackProxy.this.m1292xab01539d(session);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onCaptureQueueEmpty$4$android-hardware-camera2-impl-CallbackProxies$SessionStateCallbackProxy  reason: not valid java name */
        public /* synthetic */ void m1292xab01539d(CameraCaptureSession session) {
            this.mCallback.onCaptureQueueEmpty(session);
        }

        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onClosed(final CameraCaptureSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.hardware.camera2.impl.CallbackProxies$SessionStateCallbackProxy$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        CallbackProxies.SessionStateCallbackProxy.this.m1293xda343260(session);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onClosed$5$android-hardware-camera2-impl-CallbackProxies$SessionStateCallbackProxy  reason: not valid java name */
        public /* synthetic */ void m1293xda343260(CameraCaptureSession session) {
            this.mCallback.onClosed(session);
        }

        @Override // android.hardware.camera2.CameraCaptureSession.StateCallback
        public void onSurfacePrepared(final CameraCaptureSession session, final Surface surface) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mExecutor.execute(new Runnable() { // from class: android.hardware.camera2.impl.CallbackProxies$SessionStateCallbackProxy$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        CallbackProxies.SessionStateCallbackProxy.this.m1297xf1d2b607(session, surface);
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onSurfacePrepared$6$android-hardware-camera2-impl-CallbackProxies$SessionStateCallbackProxy  reason: not valid java name */
        public /* synthetic */ void m1297xf1d2b607(CameraCaptureSession session, Surface surface) {
            this.mCallback.onSurfacePrepared(session, surface);
        }
    }

    private CallbackProxies() {
        throw new AssertionError();
    }
}
