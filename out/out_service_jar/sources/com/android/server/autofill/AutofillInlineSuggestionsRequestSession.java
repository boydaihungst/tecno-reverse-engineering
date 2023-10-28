package com.android.server.autofill;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Slog;
import android.view.autofill.AutofillId;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InlineSuggestionsResponse;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.view.IInlineSuggestionsRequestCallback;
import com.android.internal.view.IInlineSuggestionsResponseCallback;
import com.android.internal.view.InlineSuggestionsRequestInfo;
import com.android.server.autofill.ui.InlineFillUi;
import com.android.server.inputmethod.InputMethodManagerInternal;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AutofillInlineSuggestionsRequestSession {
    private static final String TAG = AutofillInlineSuggestionsRequestSession.class.getSimpleName();
    private AutofillId mAutofillId;
    private final ComponentName mComponentName;
    private final Handler mHandler;
    private AutofillId mImeCurrentFieldId;
    private boolean mImeInputStarted;
    private boolean mImeInputViewStarted;
    private InlineSuggestionsRequest mImeRequest;
    private Consumer<InlineSuggestionsRequest> mImeRequestConsumer;
    private boolean mImeRequestReceived;
    private InlineFillUi mInlineFillUi;
    private final InputMethodManagerInternal mInputMethodManagerInternal;
    private final Object mLock;
    private boolean mPreviousHasNonPinSuggestionShow;
    private IInlineSuggestionsResponseCallback mResponseCallback;
    private final InlineFillUi.InlineUiEventCallback mUiCallback;
    private final Bundle mUiExtras;
    private final int mUserId;
    private Boolean mPreviousResponseIsNotEmpty = null;
    private boolean mDestroyed = false;
    private boolean mImeSessionInvalidated = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AutofillInlineSuggestionsRequestSession(InputMethodManagerInternal inputMethodManagerInternal, int userId, ComponentName componentName, Handler handler, Object lock, AutofillId autofillId, Consumer<InlineSuggestionsRequest> requestConsumer, Bundle uiExtras, InlineFillUi.InlineUiEventCallback callback) {
        this.mInputMethodManagerInternal = inputMethodManagerInternal;
        this.mUserId = userId;
        this.mComponentName = componentName;
        this.mHandler = handler;
        this.mLock = lock;
        this.mUiExtras = uiExtras;
        this.mUiCallback = callback;
        this.mAutofillId = autofillId;
        this.mImeRequestConsumer = requestConsumer;
    }

    AutofillId getAutofillIdLocked() {
        return this.mAutofillId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Optional<InlineSuggestionsRequest> getInlineSuggestionsRequestLocked() {
        if (this.mDestroyed) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.mImeRequest);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onInlineSuggestionsResponseLocked(InlineFillUi inlineFillUi) {
        if (this.mDestroyed) {
            return false;
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "onInlineSuggestionsResponseLocked called for:" + inlineFillUi.getAutofillId());
        }
        if (this.mImeRequest == null || this.mResponseCallback == null || this.mImeSessionInvalidated) {
            return false;
        }
        this.mAutofillId = inlineFillUi.getAutofillId();
        this.mInlineFillUi = inlineFillUi;
        maybeUpdateResponseToImeLocked();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroySessionLocked() {
        this.mDestroyed = true;
        if (!this.mImeRequestReceived) {
            Slog.w(TAG, "Never received an InlineSuggestionsRequest from the IME for " + this.mAutofillId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCreateInlineSuggestionsRequestLocked() {
        if (this.mDestroyed) {
            return;
        }
        this.mImeSessionInvalidated = false;
        if (Helper.sDebug) {
            Slog.d(TAG, "onCreateInlineSuggestionsRequestLocked called: " + this.mAutofillId);
        }
        this.mInputMethodManagerInternal.onCreateInlineSuggestionsRequest(this.mUserId, new InlineSuggestionsRequestInfo(this.mComponentName, this.mAutofillId, this.mUiExtras), new InlineSuggestionsRequestCallbackImpl());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetInlineFillUiLocked() {
        this.mInlineFillUi = null;
    }

    private void maybeUpdateResponseToImeLocked() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "maybeUpdateResponseToImeLocked called");
        }
        if (!this.mDestroyed && this.mResponseCallback != null && this.mImeInputViewStarted && this.mInlineFillUi != null && match(this.mAutofillId, this.mImeCurrentFieldId)) {
            InlineSuggestionsResponse response = this.mInlineFillUi.getInlineSuggestionsResponse();
            boolean isEmptyResponse = response.getInlineSuggestions().isEmpty();
            if (isEmptyResponse && Boolean.FALSE.equals(this.mPreviousResponseIsNotEmpty)) {
                return;
            }
            maybeNotifyFillUiEventLocked(response.getInlineSuggestions());
            updateResponseToImeUncheckLocked(response);
            this.mPreviousResponseIsNotEmpty = Boolean.valueOf(!isEmptyResponse);
        }
    }

    private void updateResponseToImeUncheckLocked(InlineSuggestionsResponse response) {
        if (this.mDestroyed) {
            return;
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "Send inline response: " + response.getInlineSuggestions().size());
        }
        try {
            this.mResponseCallback.onInlineSuggestionsResponse(this.mAutofillId, response);
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException sending InlineSuggestionsResponse to IME");
        }
    }

    private void maybeNotifyFillUiEventLocked(List<InlineSuggestion> suggestions) {
        if (this.mDestroyed) {
            return;
        }
        boolean hasSuggestionToShow = false;
        int i = 0;
        while (true) {
            if (i >= suggestions.size()) {
                break;
            }
            InlineSuggestion suggestion = suggestions.get(i);
            if (suggestion.getInfo().isPinned()) {
                i++;
            } else {
                hasSuggestionToShow = true;
                break;
            }
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "maybeNotifyFillUiEventLoked(): hasSuggestionToShow=" + hasSuggestionToShow + ", mPreviousHasNonPinSuggestionShow=" + this.mPreviousHasNonPinSuggestionShow);
        }
        if (hasSuggestionToShow && !this.mPreviousHasNonPinSuggestionShow) {
            this.mUiCallback.notifyInlineUiShown(this.mAutofillId);
        } else if (!hasSuggestionToShow && this.mPreviousHasNonPinSuggestionShow) {
            this.mUiCallback.notifyInlineUiHidden(this.mAutofillId);
        }
        this.mPreviousHasNonPinSuggestionShow = hasSuggestionToShow;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnReceiveImeRequest(InlineSuggestionsRequest request, IInlineSuggestionsResponseCallback callback) {
        synchronized (this.mLock) {
            if (!this.mDestroyed && !this.mImeRequestReceived) {
                this.mImeRequestReceived = true;
                this.mImeSessionInvalidated = false;
                if (request != null && callback != null) {
                    this.mImeRequest = request;
                    this.mResponseCallback = callback;
                    handleOnReceiveImeStatusUpdated(this.mAutofillId, true, false);
                }
                Consumer<InlineSuggestionsRequest> consumer = this.mImeRequestConsumer;
                if (consumer != null) {
                    consumer.accept(this.mImeRequest);
                    this.mImeRequestConsumer = null;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnReceiveImeStatusUpdated(boolean imeInputStarted, boolean imeInputViewStarted) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            if (this.mImeCurrentFieldId != null) {
                boolean imeInputViewStartedChanged = true;
                boolean imeInputStartedChanged = this.mImeInputStarted != imeInputStarted;
                if (this.mImeInputViewStarted == imeInputViewStarted) {
                    imeInputViewStartedChanged = false;
                }
                this.mImeInputStarted = imeInputStarted;
                this.mImeInputViewStarted = imeInputViewStarted;
                if (imeInputStartedChanged || imeInputViewStartedChanged) {
                    maybeUpdateResponseToImeLocked();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnReceiveImeStatusUpdated(AutofillId imeFieldId, boolean imeInputStarted, boolean imeInputViewStarted) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            if (imeFieldId != null) {
                this.mImeCurrentFieldId = imeFieldId;
            }
            handleOnReceiveImeStatusUpdated(imeInputStarted, imeInputViewStarted);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOnReceiveImeSessionInvalidated() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                return;
            }
            this.mImeSessionInvalidated = true;
        }
    }

    /* loaded from: classes.dex */
    private static final class InlineSuggestionsRequestCallbackImpl extends IInlineSuggestionsRequestCallback.Stub {
        private final WeakReference<AutofillInlineSuggestionsRequestSession> mSession;

        private InlineSuggestionsRequestCallbackImpl(AutofillInlineSuggestionsRequestSession session) {
            this.mSession = new WeakReference<>(session);
        }

        public void onInlineSuggestionsUnsupported() throws RemoteException {
            if (Helper.sDebug) {
                Slog.d(AutofillInlineSuggestionsRequestSession.TAG, "onInlineSuggestionsUnsupported() called.");
            }
            AutofillInlineSuggestionsRequestSession session = this.mSession.get();
            if (session != null) {
                session.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.autofill.AutofillInlineSuggestionsRequestSession$InlineSuggestionsRequestCallbackImpl$$ExternalSyntheticLambda6
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((AutofillInlineSuggestionsRequestSession) obj).handleOnReceiveImeRequest((InlineSuggestionsRequest) obj2, (IInlineSuggestionsResponseCallback) obj3);
                    }
                }, session, (Object) null, (Object) null));
            }
        }

        public void onInlineSuggestionsRequest(InlineSuggestionsRequest request, IInlineSuggestionsResponseCallback callback) {
            if (Helper.sDebug) {
                Slog.d(AutofillInlineSuggestionsRequestSession.TAG, "onInlineSuggestionsRequest() received: " + request);
            }
            AutofillInlineSuggestionsRequestSession session = this.mSession.get();
            if (session != null) {
                session.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.autofill.AutofillInlineSuggestionsRequestSession$InlineSuggestionsRequestCallbackImpl$$ExternalSyntheticLambda0
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((AutofillInlineSuggestionsRequestSession) obj).handleOnReceiveImeRequest((InlineSuggestionsRequest) obj2, (IInlineSuggestionsResponseCallback) obj3);
                    }
                }, session, request, callback));
            }
        }

        public void onInputMethodStartInput(AutofillId imeFieldId) throws RemoteException {
            if (Helper.sVerbose) {
                Slog.v(AutofillInlineSuggestionsRequestSession.TAG, "onInputMethodStartInput() received on " + imeFieldId);
            }
            AutofillInlineSuggestionsRequestSession session = this.mSession.get();
            if (session != null) {
                session.mHandler.sendMessage(PooledLambda.obtainMessage(new QuadConsumer() { // from class: com.android.server.autofill.AutofillInlineSuggestionsRequestSession$InlineSuggestionsRequestCallbackImpl$$ExternalSyntheticLambda1
                    public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                        ((AutofillInlineSuggestionsRequestSession) obj).handleOnReceiveImeStatusUpdated((AutofillId) obj2, ((Boolean) obj3).booleanValue(), ((Boolean) obj4).booleanValue());
                    }
                }, session, imeFieldId, true, false));
            }
        }

        public void onInputMethodShowInputRequested(boolean requestResult) throws RemoteException {
            if (Helper.sVerbose) {
                Slog.v(AutofillInlineSuggestionsRequestSession.TAG, "onInputMethodShowInputRequested() received: " + requestResult);
            }
        }

        public void onInputMethodStartInputView() {
            if (Helper.sVerbose) {
                Slog.v(AutofillInlineSuggestionsRequestSession.TAG, "onInputMethodStartInputView() received");
            }
            AutofillInlineSuggestionsRequestSession session = this.mSession.get();
            if (session != null) {
                session.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.autofill.AutofillInlineSuggestionsRequestSession$InlineSuggestionsRequestCallbackImpl$$ExternalSyntheticLambda5
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((AutofillInlineSuggestionsRequestSession) obj).handleOnReceiveImeStatusUpdated(((Boolean) obj2).booleanValue(), ((Boolean) obj3).booleanValue());
                    }
                }, session, true, true));
            }
        }

        public void onInputMethodFinishInputView() {
            if (Helper.sVerbose) {
                Slog.v(AutofillInlineSuggestionsRequestSession.TAG, "onInputMethodFinishInputView() received");
            }
            AutofillInlineSuggestionsRequestSession session = this.mSession.get();
            if (session != null) {
                session.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.autofill.AutofillInlineSuggestionsRequestSession$InlineSuggestionsRequestCallbackImpl$$ExternalSyntheticLambda4
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((AutofillInlineSuggestionsRequestSession) obj).handleOnReceiveImeStatusUpdated(((Boolean) obj2).booleanValue(), ((Boolean) obj3).booleanValue());
                    }
                }, session, true, false));
            }
        }

        public void onInputMethodFinishInput() throws RemoteException {
            if (Helper.sVerbose) {
                Slog.v(AutofillInlineSuggestionsRequestSession.TAG, "onInputMethodFinishInput() received");
            }
            AutofillInlineSuggestionsRequestSession session = this.mSession.get();
            if (session != null) {
                session.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.autofill.AutofillInlineSuggestionsRequestSession$InlineSuggestionsRequestCallbackImpl$$ExternalSyntheticLambda2
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((AutofillInlineSuggestionsRequestSession) obj).handleOnReceiveImeStatusUpdated(((Boolean) obj2).booleanValue(), ((Boolean) obj3).booleanValue());
                    }
                }, session, false, false));
            }
        }

        public void onInlineSuggestionsSessionInvalidated() throws RemoteException {
            if (Helper.sDebug) {
                Slog.d(AutofillInlineSuggestionsRequestSession.TAG, "onInlineSuggestionsSessionInvalidated() called.");
            }
            AutofillInlineSuggestionsRequestSession session = this.mSession.get();
            if (session != null) {
                session.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.autofill.AutofillInlineSuggestionsRequestSession$InlineSuggestionsRequestCallbackImpl$$ExternalSyntheticLambda3
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((AutofillInlineSuggestionsRequestSession) obj).handleOnReceiveImeSessionInvalidated();
                    }
                }, session));
            }
        }
    }

    private static boolean match(AutofillId autofillId, AutofillId imeClientFieldId) {
        return (autofillId == null || imeClientFieldId == null || autofillId.getViewId() != imeClientFieldId.getViewId()) ? false : true;
    }
}
