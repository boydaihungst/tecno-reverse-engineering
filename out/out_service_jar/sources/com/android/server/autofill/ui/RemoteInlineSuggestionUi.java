package com.android.server.autofill.ui;

import android.content.IntentSender;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.autofill.IInlineSuggestionUi;
import android.service.autofill.IInlineSuggestionUiCallback;
import android.service.autofill.ISurfacePackageResultCallback;
import android.util.Slog;
import android.view.SurfaceControlViewHost;
import com.android.internal.view.inline.IInlineContentCallback;
import com.android.server.autofill.Helper;
import com.android.server.autofill.ui.RemoteInlineSuggestionUi;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class RemoteInlineSuggestionUi {
    private static final long RELEASE_REMOTE_VIEW_HOST_DELAY_MS = 200;
    private static final String TAG = RemoteInlineSuggestionUi.class.getSimpleName();
    private int mActualHeight;
    private int mActualWidth;
    private Runnable mDelayedReleaseViewRunnable;
    private final Handler mHandler;
    private final int mHeight;
    private IInlineContentCallback mInlineContentCallback;
    private IInlineSuggestionUi mInlineSuggestionUi;
    private final RemoteInlineSuggestionViewConnector mRemoteInlineSuggestionViewConnector;
    private final int mWidth;
    private int mRefCount = 0;
    private boolean mWaitingForUiCreation = false;
    private final InlineSuggestionUiCallbackImpl mInlineSuggestionUiCallback = new InlineSuggestionUiCallbackImpl();

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteInlineSuggestionUi(RemoteInlineSuggestionViewConnector remoteInlineSuggestionViewConnector, int width, int height, Handler handler) {
        this.mHandler = handler;
        this.mRemoteInlineSuggestionViewConnector = remoteInlineSuggestionViewConnector;
        this.mWidth = width;
        this.mHeight = height;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInlineContentCallback(final IInlineContentCallback inlineContentCallback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInlineSuggestionUi.this.m2138xc8ba7fd(inlineContentCallback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setInlineContentCallback$0$com-android-server-autofill-ui-RemoteInlineSuggestionUi  reason: not valid java name */
    public /* synthetic */ void m2138xc8ba7fd(IInlineContentCallback inlineContentCallback) {
        this.mInlineContentCallback = inlineContentCallback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestSurfacePackage() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInlineSuggestionUi.this.handleRequestSurfacePackage();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$surfacePackageReleased$1$com-android-server-autofill-ui-RemoteInlineSuggestionUi  reason: not valid java name */
    public /* synthetic */ void m2139xe648e8f1() {
        handleUpdateRefCount(-1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void surfacePackageReleased() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                RemoteInlineSuggestionUi.this.m2139xe648e8f1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean match(int width, int height) {
        return this.mWidth == width && this.mHeight == height;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRequestSurfacePackage() {
        cancelPendingReleaseViewRequest();
        IInlineSuggestionUi iInlineSuggestionUi = this.mInlineSuggestionUi;
        if (iInlineSuggestionUi == null) {
            if (this.mWaitingForUiCreation) {
                if (Helper.sDebug) {
                    Slog.d(TAG, "Inline suggestion ui is not ready");
                    return;
                }
                return;
            }
            this.mRemoteInlineSuggestionViewConnector.renderSuggestion(this.mWidth, this.mHeight, this.mInlineSuggestionUiCallback);
            this.mWaitingForUiCreation = true;
            return;
        }
        try {
            iInlineSuggestionUi.getSurfacePackage(new AnonymousClass1());
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException calling getSurfacePackage.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.autofill.ui.RemoteInlineSuggestionUi$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends ISurfacePackageResultCallback.Stub {
        AnonymousClass1() {
        }

        public void onResult(final SurfaceControlViewHost.SurfacePackage result) {
            RemoteInlineSuggestionUi.this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInlineSuggestionUi.AnonymousClass1.this.m2140x5ff5e1e3(result);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onResult$0$com-android-server-autofill-ui-RemoteInlineSuggestionUi$1  reason: not valid java name */
        public /* synthetic */ void m2140x5ff5e1e3(SurfaceControlViewHost.SurfacePackage result) {
            if (Helper.sVerbose) {
                Slog.v(RemoteInlineSuggestionUi.TAG, "Sending refreshed SurfacePackage to IME");
            }
            try {
                RemoteInlineSuggestionUi.this.mInlineContentCallback.onContent(result, RemoteInlineSuggestionUi.this.mActualWidth, RemoteInlineSuggestionUi.this.mActualHeight);
                RemoteInlineSuggestionUi.this.handleUpdateRefCount(1);
            } catch (RemoteException e) {
                Slog.w(RemoteInlineSuggestionUi.TAG, "RemoteException calling onContent");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUpdateRefCount(int delta) {
        cancelPendingReleaseViewRequest();
        int i = this.mRefCount + delta;
        this.mRefCount = i;
        if (i <= 0) {
            Runnable runnable = new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInlineSuggestionUi.this.m2137x4bb077a9();
                }
            };
            this.mDelayedReleaseViewRunnable = runnable;
            this.mHandler.postDelayed(runnable, RELEASE_REMOTE_VIEW_HOST_DELAY_MS);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleUpdateRefCount$2$com-android-server-autofill-ui-RemoteInlineSuggestionUi  reason: not valid java name */
    public /* synthetic */ void m2137x4bb077a9() {
        if (this.mInlineSuggestionUi != null) {
            try {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "releasing the host");
                }
                this.mInlineSuggestionUi.releaseSurfaceControlViewHost();
                this.mInlineSuggestionUi = null;
            } catch (RemoteException e) {
                Slog.w(TAG, "RemoteException calling releaseSurfaceControlViewHost");
            }
        }
        this.mDelayedReleaseViewRunnable = null;
    }

    private void cancelPendingReleaseViewRequest() {
        Runnable runnable = this.mDelayedReleaseViewRunnable;
        if (runnable != null) {
            this.mHandler.removeCallbacks(runnable);
            this.mDelayedReleaseViewRunnable = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleInlineSuggestionUiReady(IInlineSuggestionUi content, SurfaceControlViewHost.SurfacePackage surfacePackage, int width, int height) {
        this.mInlineSuggestionUi = content;
        this.mRefCount = 0;
        this.mWaitingForUiCreation = false;
        this.mActualWidth = width;
        this.mActualHeight = height;
        if (this.mInlineContentCallback != null) {
            try {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "Sending new UI content to IME");
                }
                handleUpdateRefCount(1);
                this.mInlineContentCallback.onContent(surfacePackage, this.mActualWidth, this.mActualHeight);
            } catch (RemoteException e) {
                Slog.w(TAG, "RemoteException calling onContent");
            }
        }
        if (surfacePackage != null) {
            surfacePackage.release();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnClick() {
        this.mRemoteInlineSuggestionViewConnector.onClick();
        IInlineContentCallback iInlineContentCallback = this.mInlineContentCallback;
        if (iInlineContentCallback != null) {
            try {
                iInlineContentCallback.onClick();
            } catch (RemoteException e) {
                Slog.w(TAG, "RemoteException calling onClick");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnLongClick() {
        IInlineContentCallback iInlineContentCallback = this.mInlineContentCallback;
        if (iInlineContentCallback != null) {
            try {
                iInlineContentCallback.onLongClick();
            } catch (RemoteException e) {
                Slog.w(TAG, "RemoteException calling onLongClick");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnError() {
        this.mRemoteInlineSuggestionViewConnector.onError();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnTransferTouchFocusToImeWindow(IBinder sourceInputToken, int displayId) {
        this.mRemoteInlineSuggestionViewConnector.onTransferTouchFocusToImeWindow(sourceInputToken, displayId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnStartIntentSender(IntentSender intentSender) {
        this.mRemoteInlineSuggestionViewConnector.onStartIntentSender(intentSender);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class InlineSuggestionUiCallbackImpl extends IInlineSuggestionUiCallback.Stub {
        private InlineSuggestionUiCallbackImpl() {
        }

        public void onClick() {
            Handler handler = RemoteInlineSuggestionUi.this.mHandler;
            final RemoteInlineSuggestionUi remoteInlineSuggestionUi = RemoteInlineSuggestionUi.this;
            handler.post(new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$InlineSuggestionUiCallbackImpl$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInlineSuggestionUi.this.handleOnClick();
                }
            });
        }

        public void onLongClick() {
            Handler handler = RemoteInlineSuggestionUi.this.mHandler;
            final RemoteInlineSuggestionUi remoteInlineSuggestionUi = RemoteInlineSuggestionUi.this;
            handler.post(new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$InlineSuggestionUiCallbackImpl$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInlineSuggestionUi.this.handleOnLongClick();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onContent$2$com-android-server-autofill-ui-RemoteInlineSuggestionUi$InlineSuggestionUiCallbackImpl  reason: not valid java name */
        public /* synthetic */ void m2141xb75239e(IInlineSuggestionUi content, SurfaceControlViewHost.SurfacePackage surface, int width, int height) {
            RemoteInlineSuggestionUi.this.handleInlineSuggestionUiReady(content, surface, width, height);
        }

        public void onContent(final IInlineSuggestionUi content, final SurfaceControlViewHost.SurfacePackage surface, final int width, final int height) {
            RemoteInlineSuggestionUi.this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$InlineSuggestionUiCallbackImpl$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInlineSuggestionUi.InlineSuggestionUiCallbackImpl.this.m2141xb75239e(content, surface, width, height);
                }
            });
        }

        public void onError() {
            Handler handler = RemoteInlineSuggestionUi.this.mHandler;
            final RemoteInlineSuggestionUi remoteInlineSuggestionUi = RemoteInlineSuggestionUi.this;
            handler.post(new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$InlineSuggestionUiCallbackImpl$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInlineSuggestionUi.this.handleOnError();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onTransferTouchFocusToImeWindow$4$com-android-server-autofill-ui-RemoteInlineSuggestionUi$InlineSuggestionUiCallbackImpl  reason: not valid java name */
        public /* synthetic */ void m2143x862735e3(IBinder sourceInputToken, int displayId) {
            RemoteInlineSuggestionUi.this.handleOnTransferTouchFocusToImeWindow(sourceInputToken, displayId);
        }

        public void onTransferTouchFocusToImeWindow(final IBinder sourceInputToken, final int displayId) {
            RemoteInlineSuggestionUi.this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$InlineSuggestionUiCallbackImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInlineSuggestionUi.InlineSuggestionUiCallbackImpl.this.m2143x862735e3(sourceInputToken, displayId);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onStartIntentSender$5$com-android-server-autofill-ui-RemoteInlineSuggestionUi$InlineSuggestionUiCallbackImpl  reason: not valid java name */
        public /* synthetic */ void m2142x99966181(IntentSender intentSender) {
            RemoteInlineSuggestionUi.this.handleOnStartIntentSender(intentSender);
        }

        public void onStartIntentSender(final IntentSender intentSender) {
            RemoteInlineSuggestionUi.this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionUi$InlineSuggestionUiCallbackImpl$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    RemoteInlineSuggestionUi.InlineSuggestionUiCallbackImpl.this.m2142x99966181(intentSender);
                }
            });
        }
    }
}
