package com.android.server.autofill.ui;

import android.os.Handler;
import android.util.Slog;
import com.android.internal.view.inline.IInlineContentCallback;
import com.android.internal.view.inline.IInlineContentProvider;
import com.android.server.FgThread;
import com.android.server.autofill.Helper;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class InlineContentProviderImpl extends IInlineContentProvider.Stub {
    private static final String TAG = InlineContentProviderImpl.class.getSimpleName();
    private final Handler mHandler = FgThread.getHandler();
    private boolean mProvideContentCalled = false;
    private RemoteInlineSuggestionUi mRemoteInlineSuggestionUi;
    private final RemoteInlineSuggestionViewConnector mRemoteInlineSuggestionViewConnector;

    /* JADX INFO: Access modifiers changed from: package-private */
    public InlineContentProviderImpl(RemoteInlineSuggestionViewConnector remoteInlineSuggestionViewConnector, RemoteInlineSuggestionUi remoteInlineSuggestionUi) {
        this.mRemoteInlineSuggestionViewConnector = remoteInlineSuggestionViewConnector;
        this.mRemoteInlineSuggestionUi = remoteInlineSuggestionUi;
    }

    public InlineContentProviderImpl copy() {
        return new InlineContentProviderImpl(this.mRemoteInlineSuggestionViewConnector, this.mRemoteInlineSuggestionUi);
    }

    public void provideContent(final int width, final int height, final IInlineContentCallback callback) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.InlineContentProviderImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                InlineContentProviderImpl.this.m2124x20dd88d6(width, height, callback);
            }
        });
    }

    public void requestSurfacePackage() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.InlineContentProviderImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                InlineContentProviderImpl.this.handleGetSurfacePackage();
            }
        });
    }

    public void onSurfacePackageReleased() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.autofill.ui.InlineContentProviderImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                InlineContentProviderImpl.this.handleOnSurfacePackageReleased();
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleProvideContent */
    public void m2124x20dd88d6(int width, int height, IInlineContentCallback callback) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "handleProvideContent");
        }
        if (this.mProvideContentCalled) {
            return;
        }
        this.mProvideContentCalled = true;
        RemoteInlineSuggestionUi remoteInlineSuggestionUi = this.mRemoteInlineSuggestionUi;
        if (remoteInlineSuggestionUi == null || !remoteInlineSuggestionUi.match(width, height)) {
            this.mRemoteInlineSuggestionUi = new RemoteInlineSuggestionUi(this.mRemoteInlineSuggestionViewConnector, width, height, this.mHandler);
        }
        this.mRemoteInlineSuggestionUi.setInlineContentCallback(callback);
        this.mRemoteInlineSuggestionUi.requestSurfacePackage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleGetSurfacePackage() {
        RemoteInlineSuggestionUi remoteInlineSuggestionUi;
        if (Helper.sVerbose) {
            Slog.v(TAG, "handleGetSurfacePackage");
        }
        if (!this.mProvideContentCalled || (remoteInlineSuggestionUi = this.mRemoteInlineSuggestionUi) == null) {
            return;
        }
        remoteInlineSuggestionUi.requestSurfacePackage();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnSurfacePackageReleased() {
        RemoteInlineSuggestionUi remoteInlineSuggestionUi;
        if (Helper.sVerbose) {
            Slog.v(TAG, "handleOnSurfacePackageReleased");
        }
        if (!this.mProvideContentCalled || (remoteInlineSuggestionUi = this.mRemoteInlineSuggestionUi) == null) {
            return;
        }
        remoteInlineSuggestionUi.surfacePackageReleased();
    }
}
