package com.android.server.autofill.ui;

import android.content.IntentSender;
import android.os.IBinder;
import android.service.autofill.IInlineSuggestionUiCallback;
import android.service.autofill.InlinePresentation;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.autofill.Helper;
import com.android.server.autofill.RemoteInlineSuggestionRenderService;
import com.android.server.autofill.ui.InlineFillUi;
import com.android.server.inputmethod.InputMethodManagerInternal;
import java.util.Objects;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class RemoteInlineSuggestionViewConnector {
    private static final String TAG = RemoteInlineSuggestionViewConnector.class.getSimpleName();
    private final int mDisplayId;
    private final IBinder mHostInputToken;
    private final InlinePresentation mInlinePresentation;
    private final Runnable mOnAutofillCallback;
    private final Runnable mOnErrorCallback;
    private final RemoteInlineSuggestionRenderService mRemoteRenderService;
    private final int mSessionId;
    private final Consumer<IntentSender> mStartIntentSenderFromClientApp;
    private final int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteInlineSuggestionViewConnector(InlineFillUi.InlineFillUiInfo inlineFillUiInfo, InlinePresentation inlinePresentation, Runnable onAutofillCallback, final InlineFillUi.InlineSuggestionUiCallback uiCallback) {
        this.mRemoteRenderService = inlineFillUiInfo.mRemoteRenderService;
        this.mInlinePresentation = inlinePresentation;
        this.mHostInputToken = inlineFillUiInfo.mInlineRequest.getHostInputToken();
        this.mDisplayId = inlineFillUiInfo.mInlineRequest.getHostDisplayId();
        this.mUserId = inlineFillUiInfo.mUserId;
        this.mSessionId = inlineFillUiInfo.mSessionId;
        this.mOnAutofillCallback = onAutofillCallback;
        Objects.requireNonNull(uiCallback);
        this.mOnErrorCallback = new Runnable() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionViewConnector$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                InlineFillUi.InlineSuggestionUiCallback.this.onError();
            }
        };
        Objects.requireNonNull(uiCallback);
        this.mStartIntentSenderFromClientApp = new Consumer() { // from class: com.android.server.autofill.ui.RemoteInlineSuggestionViewConnector$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                InlineFillUi.InlineSuggestionUiCallback.this.startIntentSender((IntentSender) obj);
            }
        };
    }

    public boolean renderSuggestion(int width, int height, IInlineSuggestionUiCallback callback) {
        if (this.mRemoteRenderService != null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "Request to recreate the UI");
            }
            this.mRemoteRenderService.renderSuggestion(callback, this.mInlinePresentation, width, height, this.mHostInputToken, this.mDisplayId, this.mUserId, this.mSessionId);
            return true;
        }
        return false;
    }

    public void onClick() {
        this.mOnAutofillCallback.run();
    }

    public void onError() {
        this.mOnErrorCallback.run();
    }

    public void onTransferTouchFocusToImeWindow(IBinder sourceInputToken, int displayId) {
        InputMethodManagerInternal inputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
        if (!inputMethodManagerInternal.transferTouchFocusToImeWindow(sourceInputToken, displayId)) {
            Slog.e(TAG, "Cannot transfer touch focus from suggestion to IME");
            this.mOnErrorCallback.run();
        }
    }

    public void onStartIntentSender(IntentSender intentSender) {
        this.mStartIntentSenderFromClientApp.accept(intentSender);
    }
}
