package com.android.server.autofill;

import android.service.autofill.Dataset;
import android.util.Slog;
import android.view.autofill.AutofillId;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.autofill.PresentationStatsEventLogger;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public final class PresentationStatsEventLogger {
    public static final int NOT_SHOWN_REASON_ACTIVITY_FINISHED = 4;
    public static final int NOT_SHOWN_REASON_ANY_SHOWN = 1;
    public static final int NOT_SHOWN_REASON_REQUEST_TIMEOUT = 5;
    public static final int NOT_SHOWN_REASON_SESSION_COMMITTED_PREMATURELY = 6;
    public static final int NOT_SHOWN_REASON_UNKNOWN = 0;
    public static final int NOT_SHOWN_REASON_VIEW_CHANGED = 3;
    public static final int NOT_SHOWN_REASON_VIEW_FOCUS_CHANGED = 2;
    private static final String TAG = "PresentationStatsEventLogger";
    private Optional<PresentationStatsEventInternal> mEventInternal = Optional.empty();
    private final int mSessionId;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface NotShownReason {
    }

    private PresentationStatsEventLogger(int sessionId) {
        this.mSessionId = sessionId;
    }

    public static PresentationStatsEventLogger forSessionId(int sessionId) {
        return new PresentationStatsEventLogger(sessionId);
    }

    public void startNewEvent() {
        if (this.mEventInternal.isPresent()) {
            Slog.e(TAG, "Failed to start new event because already have active event.");
        } else {
            this.mEventInternal = Optional.of(new PresentationStatsEventInternal());
        }
    }

    public void maybeSetRequestId(final int requestId) {
        this.mEventInternal.ifPresent(new Consumer() { // from class: com.android.server.autofill.PresentationStatsEventLogger$$ExternalSyntheticLambda7
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((PresentationStatsEventLogger.PresentationStatsEventInternal) obj).mRequestId = requestId;
            }
        });
    }

    public void maybeSetNoPresentationEventReason(final int reason) {
        this.mEventInternal.ifPresent(new Consumer() { // from class: com.android.server.autofill.PresentationStatsEventLogger$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PresentationStatsEventLogger.lambda$maybeSetNoPresentationEventReason$1(reason, (PresentationStatsEventLogger.PresentationStatsEventInternal) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$maybeSetNoPresentationEventReason$1(int reason, PresentationStatsEventInternal event) {
        if (event.mCountShown == 0) {
            event.mNoPresentationReason = reason;
        }
    }

    public void maybeSetAvailableCount(final List<Dataset> datasetList, final AutofillId currentViewId) {
        this.mEventInternal.ifPresent(new Consumer() { // from class: com.android.server.autofill.PresentationStatsEventLogger$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PresentationStatsEventLogger.lambda$maybeSetAvailableCount$2(datasetList, currentViewId, (PresentationStatsEventLogger.PresentationStatsEventInternal) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$maybeSetAvailableCount$2(List datasetList, AutofillId currentViewId, PresentationStatsEventInternal event) {
        int availableCount = getDatasetCountForAutofillId(datasetList, currentViewId);
        event.mAvailableCount = availableCount;
        event.mIsDatasetAvailable = availableCount > 0;
    }

    public void maybeSetCountShown(final List<Dataset> datasetList, final AutofillId currentViewId) {
        this.mEventInternal.ifPresent(new Consumer() { // from class: com.android.server.autofill.PresentationStatsEventLogger$$ExternalSyntheticLambda6
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PresentationStatsEventLogger.lambda$maybeSetCountShown$3(datasetList, currentViewId, (PresentationStatsEventLogger.PresentationStatsEventInternal) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$maybeSetCountShown$3(List datasetList, AutofillId currentViewId, PresentationStatsEventInternal event) {
        int countShown = getDatasetCountForAutofillId(datasetList, currentViewId);
        event.mCountShown = countShown;
        if (countShown > 0) {
            event.mNoPresentationReason = 1;
        }
    }

    private static int getDatasetCountForAutofillId(List<Dataset> datasetList, AutofillId currentViewId) {
        int availableCount = 0;
        if (datasetList != null) {
            for (int i = 0; i < datasetList.size(); i++) {
                Dataset data = datasetList.get(i);
                if (data != null && data.getFieldIds() != null && data.getFieldIds().contains(currentViewId)) {
                    availableCount++;
                }
            }
        }
        return availableCount;
    }

    public void maybeSetCountFilteredUserTyping(final int countFilteredUserTyping) {
        this.mEventInternal.ifPresent(new Consumer() { // from class: com.android.server.autofill.PresentationStatsEventLogger$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((PresentationStatsEventLogger.PresentationStatsEventInternal) obj).mCountFilteredUserTyping = countFilteredUserTyping;
            }
        });
    }

    public void maybeSetCountNotShownImePresentationNotDrawn(final int countNotShownImePresentationNotDrawn) {
        this.mEventInternal.ifPresent(new Consumer() { // from class: com.android.server.autofill.PresentationStatsEventLogger$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((PresentationStatsEventLogger.PresentationStatsEventInternal) obj).mCountNotShownImePresentationNotDrawn = countNotShownImePresentationNotDrawn;
            }
        });
    }

    public void maybeSetCountNotShownImeUserNotSeen(final int countNotShownImeUserNotSeen) {
        this.mEventInternal.ifPresent(new Consumer() { // from class: com.android.server.autofill.PresentationStatsEventLogger$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((PresentationStatsEventLogger.PresentationStatsEventInternal) obj).mCountNotShownImeUserNotSeen = countNotShownImeUserNotSeen;
            }
        });
    }

    public void maybeSetDisplayPresentationType(final int uiType) {
        this.mEventInternal.ifPresent(new Consumer() { // from class: com.android.server.autofill.PresentationStatsEventLogger$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((PresentationStatsEventLogger.PresentationStatsEventInternal) obj).mDisplayPresentationType = PresentationStatsEventLogger.getDisplayPresentationType(uiType);
            }
        });
    }

    public void logAndEndEvent() {
        if (!this.mEventInternal.isPresent()) {
            Slog.w(TAG, "Shouldn't be logging AutofillPresentationEventReported again for same event");
            return;
        }
        PresentationStatsEventInternal event = this.mEventInternal.get();
        if (Helper.sVerbose) {
            Slog.v(TAG, "Log AutofillPresentationEventReported: requestId=" + event.mRequestId + " sessionId=" + this.mSessionId + " mNoPresentationEventReason=" + event.mNoPresentationReason + " mAvailableCount=" + event.mAvailableCount + " mCountShown=" + event.mCountShown + " mCountFilteredUserTyping=" + event.mCountFilteredUserTyping + " mCountNotShownImePresentationNotDrawn=" + event.mCountNotShownImePresentationNotDrawn + " mCountNotShownImeUserNotSeen=" + event.mCountNotShownImeUserNotSeen + " mDisplayPresentationType=" + event.mDisplayPresentationType);
        }
        if (!event.mIsDatasetAvailable) {
            this.mEventInternal = Optional.empty();
            return;
        }
        FrameworkStatsLog.write((int) FrameworkStatsLog.AUTOFILL_PRESENTATION_EVENT_REPORTED, event.mRequestId, this.mSessionId, event.mNoPresentationReason, event.mAvailableCount, event.mCountShown, event.mCountFilteredUserTyping, event.mCountNotShownImePresentationNotDrawn, event.mCountNotShownImeUserNotSeen, event.mDisplayPresentationType);
        this.mEventInternal = Optional.empty();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PresentationStatsEventInternal {
        int mAvailableCount;
        int mCountFilteredUserTyping;
        int mCountNotShownImePresentationNotDrawn;
        int mCountNotShownImeUserNotSeen;
        int mCountShown;
        boolean mIsDatasetAvailable;
        int mRequestId;
        int mNoPresentationReason = 0;
        int mDisplayPresentationType = 0;

        PresentationStatsEventInternal() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getNoPresentationEventReason(int commitReason) {
        switch (commitReason) {
            case 1:
                return 4;
            case 2:
                return 6;
            case 3:
            default:
                return 0;
            case 4:
                return 3;
        }
    }

    private static int getDisplayPresentationType(int uiType) {
        switch (uiType) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            default:
                return 0;
        }
    }
}
