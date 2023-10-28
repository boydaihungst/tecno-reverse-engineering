package android.view.textclassifier;

import android.view.textclassifier.ConversationActions;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextLanguage;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import com.android.internal.util.Preconditions;
import java.util.Objects;
import java.util.function.Supplier;
import sun.misc.Cleaner;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes3.dex */
public final class TextClassificationSession implements TextClassifier {
    private static final String LOG_TAG = "TextClassificationSession";
    private final TextClassificationContext mClassificationContext;
    private final Cleaner mCleaner;
    private final TextClassifier mDelegate;
    private boolean mDestroyed;
    private final SelectionEventHelper mEventHelper;
    private final Object mLock = new Object();
    private final TextClassificationSessionId mSessionId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TextClassificationSession(TextClassificationContext context, TextClassifier delegate) {
        TextClassificationContext textClassificationContext = (TextClassificationContext) Objects.requireNonNull(context);
        this.mClassificationContext = textClassificationContext;
        TextClassifier textClassifier = (TextClassifier) Objects.requireNonNull(delegate);
        this.mDelegate = textClassifier;
        TextClassificationSessionId textClassificationSessionId = new TextClassificationSessionId();
        this.mSessionId = textClassificationSessionId;
        SelectionEventHelper selectionEventHelper = new SelectionEventHelper(textClassificationSessionId, textClassificationContext);
        this.mEventHelper = selectionEventHelper;
        initializeRemoteSession();
        this.mCleaner = Cleaner.create(this, new CleanerRunnable(selectionEventHelper, textClassifier));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$suggestSelection$0$android-view-textclassifier-TextClassificationSession  reason: not valid java name */
    public /* synthetic */ TextSelection m5417xd152265b(TextSelection.Request request) {
        return this.mDelegate.suggestSelection(request);
    }

    @Override // android.view.textclassifier.TextClassifier
    public TextSelection suggestSelection(final TextSelection.Request request) {
        return (TextSelection) checkDestroyedAndRun(new Supplier() { // from class: android.view.textclassifier.TextClassificationSession$$ExternalSyntheticLambda3
            @Override // java.util.function.Supplier
            public final Object get() {
                return TextClassificationSession.this.m5417xd152265b(request);
            }
        });
    }

    private void initializeRemoteSession() {
        TextClassifier textClassifier = this.mDelegate;
        if (textClassifier instanceof SystemTextClassifier) {
            ((SystemTextClassifier) textClassifier).initializeRemoteSession(this.mClassificationContext, this.mSessionId);
        }
    }

    @Override // android.view.textclassifier.TextClassifier
    public TextClassification classifyText(final TextClassification.Request request) {
        return (TextClassification) checkDestroyedAndRun(new Supplier() { // from class: android.view.textclassifier.TextClassificationSession$$ExternalSyntheticLambda1
            @Override // java.util.function.Supplier
            public final Object get() {
                return TextClassificationSession.this.m5411x13332e85(request);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$classifyText$1$android-view-textclassifier-TextClassificationSession  reason: not valid java name */
    public /* synthetic */ TextClassification m5411x13332e85(TextClassification.Request request) {
        return this.mDelegate.classifyText(request);
    }

    @Override // android.view.textclassifier.TextClassifier
    public TextLinks generateLinks(final TextLinks.Request request) {
        return (TextLinks) checkDestroyedAndRun(new Supplier() { // from class: android.view.textclassifier.TextClassificationSession$$ExternalSyntheticLambda2
            @Override // java.util.function.Supplier
            public final Object get() {
                return TextClassificationSession.this.m5413xd76f725f(request);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$generateLinks$2$android-view-textclassifier-TextClassificationSession  reason: not valid java name */
    public /* synthetic */ TextLinks m5413xd76f725f(TextLinks.Request request) {
        return this.mDelegate.generateLinks(request);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$suggestConversationActions$3$android-view-textclassifier-TextClassificationSession  reason: not valid java name */
    public /* synthetic */ ConversationActions m5416xe40d424c(ConversationActions.Request request) {
        return this.mDelegate.suggestConversationActions(request);
    }

    @Override // android.view.textclassifier.TextClassifier
    public ConversationActions suggestConversationActions(final ConversationActions.Request request) {
        return (ConversationActions) checkDestroyedAndRun(new Supplier() { // from class: android.view.textclassifier.TextClassificationSession$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return TextClassificationSession.this.m5416xe40d424c(request);
            }
        });
    }

    @Override // android.view.textclassifier.TextClassifier
    public TextLanguage detectLanguage(final TextLanguage.Request request) {
        return (TextLanguage) checkDestroyedAndRun(new Supplier() { // from class: android.view.textclassifier.TextClassificationSession$$ExternalSyntheticLambda6
            @Override // java.util.function.Supplier
            public final Object get() {
                return TextClassificationSession.this.m5412x2b02652(request);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$detectLanguage$4$android-view-textclassifier-TextClassificationSession  reason: not valid java name */
    public /* synthetic */ TextLanguage m5412x2b02652(TextLanguage.Request request) {
        return this.mDelegate.detectLanguage(request);
    }

    @Override // android.view.textclassifier.TextClassifier
    public int getMaxGenerateLinksTextLength() {
        final TextClassifier textClassifier = this.mDelegate;
        Objects.requireNonNull(textClassifier);
        return ((Integer) checkDestroyedAndRun(new Supplier() { // from class: android.view.textclassifier.TextClassificationSession$$ExternalSyntheticLambda7
            @Override // java.util.function.Supplier
            public final Object get() {
                return Integer.valueOf(TextClassifier.this.getMaxGenerateLinksTextLength());
            }
        })).intValue();
    }

    @Override // android.view.textclassifier.TextClassifier
    public void onSelectionEvent(final SelectionEvent event) {
        checkDestroyedAndRun(new Supplier() { // from class: android.view.textclassifier.TextClassificationSession$$ExternalSyntheticLambda4
            @Override // java.util.function.Supplier
            public final Object get() {
                return TextClassificationSession.this.m5414x95bb3765(event);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSelectionEvent$5$android-view-textclassifier-TextClassificationSession  reason: not valid java name */
    public /* synthetic */ Object m5414x95bb3765(SelectionEvent event) {
        try {
            if (this.mEventHelper.sanitizeEvent(event)) {
                this.mDelegate.onSelectionEvent(event);
                return null;
            }
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error reporting text classifier selection event", e);
            return null;
        }
    }

    @Override // android.view.textclassifier.TextClassifier
    public void onTextClassifierEvent(final TextClassifierEvent event) {
        checkDestroyedAndRun(new Supplier() { // from class: android.view.textclassifier.TextClassificationSession$$ExternalSyntheticLambda5
            @Override // java.util.function.Supplier
            public final Object get() {
                return TextClassificationSession.this.m5415xee8f39cc(event);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onTextClassifierEvent$6$android-view-textclassifier-TextClassificationSession  reason: not valid java name */
    public /* synthetic */ Object m5415xee8f39cc(TextClassifierEvent event) {
        try {
            event.mHiddenTempSessionId = this.mSessionId;
            this.mDelegate.onTextClassifierEvent(event);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error reporting text classifier event", e);
            return null;
        }
    }

    @Override // android.view.textclassifier.TextClassifier
    public void destroy() {
        synchronized (this.mLock) {
            if (!this.mDestroyed) {
                this.mCleaner.clean();
                this.mDestroyed = true;
            }
        }
    }

    @Override // android.view.textclassifier.TextClassifier
    public boolean isDestroyed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDestroyed;
        }
        return z;
    }

    private <T> T checkDestroyedAndRun(Supplier<T> responseSupplier) {
        if (!isDestroyed()) {
            T response = responseSupplier.get();
            synchronized (this.mLock) {
                if (!this.mDestroyed) {
                    return response;
                }
            }
        }
        throw new IllegalStateException("This TextClassification session has been destroyed");
    }

    /* loaded from: classes3.dex */
    private static final class SelectionEventHelper {
        private final TextClassificationContext mContext;
        private int mInvocationMethod = 0;
        private SelectionEvent mPrevEvent;
        private final TextClassificationSessionId mSessionId;
        private SelectionEvent mSmartEvent;
        private SelectionEvent mStartEvent;

        SelectionEventHelper(TextClassificationSessionId sessionId, TextClassificationContext context) {
            this.mSessionId = (TextClassificationSessionId) Objects.requireNonNull(sessionId);
            this.mContext = (TextClassificationContext) Objects.requireNonNull(context);
        }

        boolean sanitizeEvent(SelectionEvent event) {
            updateInvocationMethod(event);
            modifyAutoSelectionEventType(event);
            if (event.getEventType() != 1 && this.mStartEvent == null) {
                Log.d(TextClassificationSession.LOG_TAG, "Selection session not yet started. Ignoring event");
                return false;
            }
            long now = System.currentTimeMillis();
            switch (event.getEventType()) {
                case 1:
                    Preconditions.checkArgument(event.getAbsoluteEnd() == event.getAbsoluteStart() + 1);
                    event.setSessionId(this.mSessionId);
                    this.mStartEvent = event;
                    break;
                case 2:
                    SelectionEvent selectionEvent = this.mPrevEvent;
                    if (selectionEvent != null && selectionEvent.getAbsoluteStart() == event.getAbsoluteStart() && this.mPrevEvent.getAbsoluteEnd() == event.getAbsoluteEnd()) {
                        return false;
                    }
                    break;
                case 3:
                case 4:
                case 5:
                    this.mSmartEvent = event;
                    break;
                case 100:
                case 107:
                    SelectionEvent selectionEvent2 = this.mPrevEvent;
                    if (selectionEvent2 != null) {
                        event.setEntityType(selectionEvent2.getEntityType());
                        break;
                    }
                    break;
            }
            event.setEventTime(now);
            SelectionEvent selectionEvent3 = this.mStartEvent;
            if (selectionEvent3 != null) {
                event.setSessionId(selectionEvent3.getSessionId()).setDurationSinceSessionStart(now - this.mStartEvent.getEventTime()).setStart(event.getAbsoluteStart() - this.mStartEvent.getAbsoluteStart()).setEnd(event.getAbsoluteEnd() - this.mStartEvent.getAbsoluteStart());
            }
            SelectionEvent selectionEvent4 = this.mSmartEvent;
            if (selectionEvent4 != null) {
                event.setResultId(selectionEvent4.getResultId()).setSmartStart(this.mSmartEvent.getAbsoluteStart() - this.mStartEvent.getAbsoluteStart()).setSmartEnd(this.mSmartEvent.getAbsoluteEnd() - this.mStartEvent.getAbsoluteStart());
            }
            SelectionEvent selectionEvent5 = this.mPrevEvent;
            if (selectionEvent5 != null) {
                event.setDurationSincePreviousEvent(now - selectionEvent5.getEventTime()).setEventIndex(this.mPrevEvent.getEventIndex() + 1);
            }
            this.mPrevEvent = event;
            return true;
        }

        void endSession() {
            this.mPrevEvent = null;
            this.mSmartEvent = null;
            this.mStartEvent = null;
        }

        private void updateInvocationMethod(SelectionEvent event) {
            event.setTextClassificationSessionContext(this.mContext);
            if (event.getInvocationMethod() == 0) {
                event.setInvocationMethod(this.mInvocationMethod);
            } else {
                this.mInvocationMethod = event.getInvocationMethod();
            }
        }

        private void modifyAutoSelectionEventType(SelectionEvent event) {
            switch (event.getEventType()) {
                case 3:
                case 4:
                case 5:
                    if (SelectionSessionLogger.isPlatformLocalTextClassifierSmartSelection(event.getResultId())) {
                        if (event.getAbsoluteEnd() - event.getAbsoluteStart() > 1) {
                            event.setEventType(4);
                            return;
                        } else {
                            event.setEventType(3);
                            return;
                        }
                    }
                    event.setEventType(5);
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes3.dex */
    private static class CleanerRunnable implements Runnable {
        private final TextClassifier mDelegate;
        private final SelectionEventHelper mEventHelper;

        CleanerRunnable(SelectionEventHelper eventHelper, TextClassifier delegate) {
            this.mEventHelper = (SelectionEventHelper) Objects.requireNonNull(eventHelper);
            this.mDelegate = (TextClassifier) Objects.requireNonNull(delegate);
        }

        @Override // java.lang.Runnable
        public void run() {
            this.mEventHelper.endSession();
            this.mDelegate.destroy();
        }
    }
}
