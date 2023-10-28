package com.android.server.autofill.ui;

import android.content.IntentSender;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.service.autofill.InlinePresentation;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InlineSuggestionInfo;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.widget.inline.InlinePresentationSpec;
import com.android.internal.view.inline.IInlineContentProvider;
import com.android.server.autofill.Helper;
import com.android.server.autofill.ui.InlineFillUi;
import com.android.server.location.gnss.hal.GnssNative;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class InlineSuggestionFactory {
    private static final String TAG = "InlineSuggestionFactory";

    public static InlineSuggestion createInlineAuthentication(InlineFillUi.InlineFillUiInfo inlineFillUiInfo, FillResponse response, final InlineFillUi.InlineSuggestionUiCallback uiCallback) {
        InlinePresentation inlineAuthentication = response.getInlinePresentation();
        final int requestId = response.getRequestId();
        return createInlineSuggestion(inlineFillUiInfo, "android:autofill", "android:autofill:action", new Runnable() { // from class: com.android.server.autofill.ui.InlineSuggestionFactory$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                InlineFillUi.InlineSuggestionUiCallback.this.authenticate(requestId, GnssNative.GNSS_AIDING_TYPE_ALL);
            }
        }, mergedInlinePresentation(inlineFillUiInfo.mInlineRequest, 0, inlineAuthentication), createInlineSuggestionTooltip(inlineFillUiInfo.mInlineRequest, inlineFillUiInfo, "android:autofill", response.getInlineTooltipPresentation()), uiCallback);
    }

    public static SparseArray<Pair<Dataset, InlineSuggestion>> createInlineSuggestions(InlineFillUi.InlineFillUiInfo inlineFillUiInfo, String suggestionSource, List<Dataset> datasets, final InlineFillUi.InlineSuggestionUiCallback uiCallback) {
        boolean hasTooltip;
        InlineSuggestion inlineSuggestionTooltip;
        if (Helper.sDebug) {
            Slog.d(TAG, "createInlineSuggestions(source=" + suggestionSource + ") called");
        }
        InlineSuggestionsRequest request = inlineFillUiInfo.mInlineRequest;
        SparseArray<Pair<Dataset, InlineSuggestion>> response = new SparseArray<>(datasets.size());
        boolean hasTooltip2 = false;
        for (int datasetIndex = 0; datasetIndex < datasets.size(); datasetIndex++) {
            final Dataset dataset = datasets.get(datasetIndex);
            int fieldIndex = dataset.getFieldIds().indexOf(inlineFillUiInfo.mFocusedId);
            if (fieldIndex < 0) {
                Slog.w(TAG, "AutofillId=" + inlineFillUiInfo.mFocusedId + " not found in dataset");
            } else {
                InlinePresentation inlinePresentation = dataset.getFieldInlinePresentation(fieldIndex);
                if (inlinePresentation == null) {
                    Slog.w(TAG, "InlinePresentation not found in dataset");
                } else {
                    String suggestionType = dataset.getAuthentication() == null ? "android:autofill:suggestion" : "android:autofill:action";
                    final int index = datasetIndex;
                    if (hasTooltip2) {
                        hasTooltip = hasTooltip2;
                        inlineSuggestionTooltip = null;
                    } else {
                        InlineSuggestion inlineSuggestionTooltip2 = createInlineSuggestionTooltip(request, inlineFillUiInfo, suggestionSource, dataset.getFieldInlineTooltipPresentation(fieldIndex));
                        if (inlineSuggestionTooltip2 == null) {
                            hasTooltip = hasTooltip2;
                            inlineSuggestionTooltip = inlineSuggestionTooltip2;
                        } else {
                            hasTooltip = true;
                            inlineSuggestionTooltip = inlineSuggestionTooltip2;
                        }
                    }
                    InlineSuggestion inlineSuggestion = createInlineSuggestion(inlineFillUiInfo, suggestionSource, suggestionType, new Runnable() { // from class: com.android.server.autofill.ui.InlineSuggestionFactory$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            InlineFillUi.InlineSuggestionUiCallback.this.autofill(dataset, index);
                        }
                    }, mergedInlinePresentation(request, datasetIndex, inlinePresentation), inlineSuggestionTooltip, uiCallback);
                    response.append(datasetIndex, Pair.create(dataset, inlineSuggestion));
                    hasTooltip2 = hasTooltip;
                }
            }
        }
        return response;
    }

    private static InlineSuggestion createInlineSuggestion(InlineFillUi.InlineFillUiInfo inlineFillUiInfo, String suggestionSource, String suggestionType, Runnable onClickAction, InlinePresentation inlinePresentation, InlineSuggestion tooltip, InlineFillUi.InlineSuggestionUiCallback uiCallback) {
        InlineSuggestionInfo inlineSuggestionInfo = new InlineSuggestionInfo(inlinePresentation.getInlinePresentationSpec(), suggestionSource, inlinePresentation.getAutofillHints(), suggestionType, inlinePresentation.isPinned(), tooltip);
        return new InlineSuggestion(inlineSuggestionInfo, createInlineContentProvider(inlineFillUiInfo, inlinePresentation, onClickAction, uiCallback));
    }

    private static InlinePresentation mergedInlinePresentation(InlineSuggestionsRequest request, int index, InlinePresentation inlinePresentation) {
        List<InlinePresentationSpec> specs = request.getInlinePresentationSpecs();
        if (specs.isEmpty()) {
            return inlinePresentation;
        }
        InlinePresentationSpec specFromHost = specs.get(Math.min(specs.size() - 1, index));
        InlinePresentationSpec mergedInlinePresentation = new InlinePresentationSpec.Builder(inlinePresentation.getInlinePresentationSpec().getMinSize(), inlinePresentation.getInlinePresentationSpec().getMaxSize()).setStyle(specFromHost.getStyle()).build();
        return new InlinePresentation(inlinePresentation.getSlice(), mergedInlinePresentation, inlinePresentation.isPinned());
    }

    private static InlineSuggestion createInlineSuggestionTooltip(InlineSuggestionsRequest request, InlineFillUi.InlineFillUiInfo inlineFillUiInfo, String suggestionSource, InlinePresentation tooltipPresentation) {
        InlinePresentationSpec mergedSpec;
        if (tooltipPresentation == null) {
            return null;
        }
        InlinePresentationSpec spec = request.getInlineTooltipPresentationSpec();
        if (spec == null) {
            mergedSpec = tooltipPresentation.getInlinePresentationSpec();
        } else {
            mergedSpec = new InlinePresentationSpec.Builder(tooltipPresentation.getInlinePresentationSpec().getMinSize(), tooltipPresentation.getInlinePresentationSpec().getMaxSize()).setStyle(spec.getStyle()).build();
        }
        InlineFillUi.InlineSuggestionUiCallback uiCallback = new InlineFillUi.InlineSuggestionUiCallback() { // from class: com.android.server.autofill.ui.InlineSuggestionFactory.1
            @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
            public void autofill(Dataset dataset, int datasetIndex) {
            }

            @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
            public void authenticate(int requestId, int datasetIndex) {
            }

            @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
            public void startIntentSender(IntentSender intentSender) {
            }

            @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
            public void onError() {
                Slog.w(InlineSuggestionFactory.TAG, "An error happened on the tooltip");
            }
        };
        InlinePresentation tooltipInline = new InlinePresentation(tooltipPresentation.getSlice(), mergedSpec, false);
        IInlineContentProvider tooltipContentProvider = createInlineContentProvider(inlineFillUiInfo, tooltipInline, new Runnable() { // from class: com.android.server.autofill.ui.InlineSuggestionFactory$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                InlineSuggestionFactory.lambda$createInlineSuggestionTooltip$2();
            }
        }, uiCallback);
        InlineSuggestionInfo tooltipInlineSuggestionInfo = new InlineSuggestionInfo(mergedSpec, suggestionSource, null, "android:autofill:suggestion", false, null);
        return new InlineSuggestion(tooltipInlineSuggestionInfo, tooltipContentProvider);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$createInlineSuggestionTooltip$2() {
    }

    private static IInlineContentProvider createInlineContentProvider(InlineFillUi.InlineFillUiInfo inlineFillUiInfo, InlinePresentation inlinePresentation, Runnable onClickAction, InlineFillUi.InlineSuggestionUiCallback uiCallback) {
        RemoteInlineSuggestionViewConnector remoteInlineSuggestionViewConnector = new RemoteInlineSuggestionViewConnector(inlineFillUiInfo, inlinePresentation, onClickAction, uiCallback);
        return new InlineContentProviderImpl(remoteInlineSuggestionViewConnector, null);
    }

    private InlineSuggestionFactory() {
    }
}
