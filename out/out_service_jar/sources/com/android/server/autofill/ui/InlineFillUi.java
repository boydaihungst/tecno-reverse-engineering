package com.android.server.autofill.ui;

import android.content.IntentSender;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.service.autofill.InlinePresentation;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.inputmethod.InlineSuggestion;
import android.view.inputmethod.InlineSuggestionsRequest;
import android.view.inputmethod.InlineSuggestionsResponse;
import com.android.server.autofill.Helper;
import com.android.server.autofill.RemoteInlineSuggestionRenderService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
/* loaded from: classes.dex */
public final class InlineFillUi {
    private static final String TAG = "InlineFillUi";
    final AutofillId mAutofillId;
    private final ArrayList<Dataset> mDatasets;
    private boolean mFilterMatchingDisabled;
    private String mFilterText;
    private final ArrayList<InlineSuggestion> mInlineSuggestions;

    /* loaded from: classes.dex */
    public interface InlineSuggestionUiCallback {
        void authenticate(int i, int i2);

        void autofill(Dataset dataset, int i);

        void onError();

        void startIntentSender(IntentSender intentSender);
    }

    /* loaded from: classes.dex */
    public interface InlineUiEventCallback {
        void notifyInlineUiHidden(AutofillId autofillId);

        void notifyInlineUiShown(AutofillId autofillId);
    }

    public static InlineFillUi emptyUi(AutofillId autofillId) {
        return new InlineFillUi(autofillId);
    }

    /* loaded from: classes.dex */
    public static class InlineFillUiInfo {
        public String mFilterText;
        public AutofillId mFocusedId;
        public InlineSuggestionsRequest mInlineRequest;
        public RemoteInlineSuggestionRenderService mRemoteRenderService;
        public int mSessionId;
        public int mUserId;

        public InlineFillUiInfo(InlineSuggestionsRequest inlineRequest, AutofillId focusedId, String filterText, RemoteInlineSuggestionRenderService remoteRenderService, int userId, int sessionId) {
            this.mUserId = userId;
            this.mSessionId = sessionId;
            this.mInlineRequest = inlineRequest;
            this.mFocusedId = focusedId;
            this.mFilterText = filterText;
            this.mRemoteRenderService = remoteRenderService;
        }
    }

    public static InlineFillUi forAutofill(InlineFillUiInfo inlineFillUiInfo, FillResponse response, InlineSuggestionUiCallback uiCallback) {
        if (response.getAuthentication() != null && response.getInlinePresentation() != null) {
            InlineSuggestion inlineAuthentication = InlineSuggestionFactory.createInlineAuthentication(inlineFillUiInfo, response, uiCallback);
            return new InlineFillUi(inlineFillUiInfo, inlineAuthentication);
        } else if (response.getDatasets() != null) {
            SparseArray<Pair<Dataset, InlineSuggestion>> inlineSuggestions = InlineSuggestionFactory.createInlineSuggestions(inlineFillUiInfo, "android:autofill", response.getDatasets(), uiCallback);
            return new InlineFillUi(inlineFillUiInfo, inlineSuggestions);
        } else {
            return new InlineFillUi(inlineFillUiInfo, new SparseArray());
        }
    }

    public static InlineFillUi forAugmentedAutofill(InlineFillUiInfo inlineFillUiInfo, List<Dataset> datasets, InlineSuggestionUiCallback uiCallback) {
        SparseArray<Pair<Dataset, InlineSuggestion>> inlineSuggestions = InlineSuggestionFactory.createInlineSuggestions(inlineFillUiInfo, "android:platform", datasets, uiCallback);
        return new InlineFillUi(inlineFillUiInfo, inlineSuggestions);
    }

    private InlineFillUi(InlineFillUiInfo inlineFillUiInfo, SparseArray<Pair<Dataset, InlineSuggestion>> inlineSuggestions) {
        this.mAutofillId = inlineFillUiInfo.mFocusedId;
        int size = inlineSuggestions.size();
        this.mDatasets = new ArrayList<>(size);
        this.mInlineSuggestions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Pair<Dataset, InlineSuggestion> value = inlineSuggestions.valueAt(i);
            this.mDatasets.add((Dataset) value.first);
            this.mInlineSuggestions.add((InlineSuggestion) value.second);
        }
        this.mFilterText = inlineFillUiInfo.mFilterText;
    }

    private InlineFillUi(InlineFillUiInfo inlineFillUiInfo, InlineSuggestion inlineSuggestion) {
        this.mAutofillId = inlineFillUiInfo.mFocusedId;
        this.mDatasets = null;
        ArrayList<InlineSuggestion> arrayList = new ArrayList<>();
        this.mInlineSuggestions = arrayList;
        arrayList.add(inlineSuggestion);
        this.mFilterText = inlineFillUiInfo.mFilterText;
    }

    private InlineFillUi(AutofillId focusedId) {
        this.mAutofillId = focusedId;
        this.mDatasets = new ArrayList<>(0);
        this.mInlineSuggestions = new ArrayList<>(0);
        this.mFilterText = null;
    }

    public AutofillId getAutofillId() {
        return this.mAutofillId;
    }

    public void setFilterText(String filterText) {
        this.mFilterText = filterText;
    }

    public InlineSuggestionsResponse getInlineSuggestionsResponse() {
        int size = this.mInlineSuggestions.size();
        if (size == 0) {
            return new InlineSuggestionsResponse(Collections.emptyList());
        }
        List<InlineSuggestion> inlineSuggestions = new ArrayList<>();
        ArrayList<Dataset> arrayList = this.mDatasets;
        if (arrayList == null || arrayList.size() != size) {
            for (int i = 0; i < size; i++) {
                inlineSuggestions.add(copy(i, this.mInlineSuggestions.get(i)));
            }
            return new InlineSuggestionsResponse(inlineSuggestions);
        }
        for (int i2 = 0; i2 < size; i2++) {
            Dataset dataset = this.mDatasets.get(i2);
            int fieldIndex = dataset.getFieldIds().indexOf(this.mAutofillId);
            if (fieldIndex < 0) {
                Slog.w(TAG, "AutofillId=" + this.mAutofillId + " not found in dataset");
            } else {
                InlinePresentation inlinePresentation = dataset.getFieldInlinePresentation(fieldIndex);
                if (inlinePresentation == null) {
                    Slog.w(TAG, "InlinePresentation not found in dataset");
                } else if (inlinePresentation.isPinned() || includeDataset(dataset, fieldIndex)) {
                    inlineSuggestions.add(copy(i2, this.mInlineSuggestions.get(i2)));
                }
            }
        }
        return new InlineSuggestionsResponse(inlineSuggestions);
    }

    private InlineSuggestion copy(int index, InlineSuggestion inlineSuggestion) {
        InlineContentProviderImpl contentProvider = inlineSuggestion.getContentProvider();
        if (contentProvider instanceof InlineContentProviderImpl) {
            InlineSuggestion newInlineSuggestion = new InlineSuggestion(inlineSuggestion.getInfo(), contentProvider.copy());
            this.mInlineSuggestions.set(index, newInlineSuggestion);
            return newInlineSuggestion;
        }
        return inlineSuggestion;
    }

    private boolean includeDataset(Dataset dataset, int fieldIndex) {
        if (TextUtils.isEmpty(this.mFilterText)) {
            return true;
        }
        String constraintLowerCase = this.mFilterText.toString().toLowerCase();
        Dataset.DatasetFieldFilter filter = dataset.getFilter(fieldIndex);
        if (filter != null) {
            Pattern filterPattern = filter.pattern;
            if (filterPattern == null) {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "Explicitly disabling filter for dataset id" + dataset.getId());
                }
                return false;
            } else if (this.mFilterMatchingDisabled) {
                return false;
            } else {
                return filterPattern.matcher(constraintLowerCase).matches();
            }
        }
        AutofillValue value = (AutofillValue) dataset.getFieldValues().get(fieldIndex);
        if (value == null || !value.isText()) {
            return dataset.getAuthentication() == null;
        } else if (this.mFilterMatchingDisabled) {
            return false;
        } else {
            String valueText = value.getTextValue().toString().toLowerCase();
            return valueText.toLowerCase().startsWith(constraintLowerCase);
        }
    }

    public void disableFilterMatching() {
        this.mFilterMatchingDisabled = true;
    }
}
