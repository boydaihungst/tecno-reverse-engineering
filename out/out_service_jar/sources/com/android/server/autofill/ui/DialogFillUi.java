package com.android.server.autofill.ui;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.text.TextUtils;
import android.util.PluralsMessageFormatter;
import android.util.Slog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.server.am.AssistDataRequester;
import com.android.server.autofill.AutofillManagerService;
import com.android.server.autofill.Helper;
import com.android.server.autofill.ui.DialogFillUi;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DialogFillUi {
    private static final String TAG = "DialogFillUi";
    private static final int THEME_ID_DARK = 16974827;
    private static final int THEME_ID_LIGHT = 16974838;
    private final ItemsAdapter mAdapter;
    private AnnounceFilterResult mAnnounceFilterResult;
    private final UiCallback mCallback;
    private final ComponentName mComponentName;
    private final Context mContext;
    private boolean mDestroyed;
    private final Dialog mDialog;
    private String mFilterText;
    private final ListView mListView;
    private final OverlayControl mOverlayControl;
    private final String mServicePackageName;
    private final int mThemeId;
    private final int mVisibleDatasetsMaxCount;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface UiCallback {
        void onCanceled();

        void onDatasetPicked(Dataset dataset);

        void onDismissed();

        void onResponsePicked(FillResponse fillResponse);

        void startIntentSender(IntentSender intentSender);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DialogFillUi(Context context, FillResponse response, AutofillId focusedViewId, String filterText, Drawable serviceIcon, String servicePackageName, ComponentName componentName, OverlayControl overlayControl, boolean nightMode, UiCallback callback) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "nightMode: " + nightMode);
        }
        int i = nightMode ? THEME_ID_DARK : THEME_ID_LIGHT;
        this.mThemeId = i;
        this.mCallback = callback;
        this.mOverlayControl = overlayControl;
        this.mServicePackageName = servicePackageName;
        this.mComponentName = componentName;
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, i);
        this.mContext = contextThemeWrapper;
        LayoutInflater inflater = LayoutInflater.from(contextThemeWrapper);
        View decor = inflater.inflate(17367109, (ViewGroup) null);
        setServiceIcon(decor, serviceIcon);
        setHeader(decor, response);
        this.mVisibleDatasetsMaxCount = getVisibleDatasetsMaxCount();
        if (response.getAuthentication() == null) {
            List<ViewItem> items = createDatasetItems(response, focusedViewId);
            this.mAdapter = new ItemsAdapter(items);
            this.mListView = (ListView) decor.findViewById(16908804);
            initialDatasetLayout(decor, filterText);
        } else {
            this.mListView = null;
            this.mAdapter = null;
            try {
                initialAuthenticationLayout(decor, response);
            } catch (RuntimeException e) {
                callback.onCanceled();
                Slog.e(TAG, "Error inflating remote views", e);
                this.mDialog = null;
                return;
            }
        }
        setDismissButton(decor);
        Dialog dialog = new Dialog(contextThemeWrapper, i);
        this.mDialog = dialog;
        dialog.setContentView(decor);
        setDialogParamsAsBottomSheet();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() { // from class: com.android.server.autofill.ui.DialogFillUi$$ExternalSyntheticLambda5
            @Override // android.content.DialogInterface.OnCancelListener
            public final void onCancel(DialogInterface dialogInterface) {
                DialogFillUi.this.m2102lambda$new$0$comandroidserverautofilluiDialogFillUi(dialogInterface);
            }
        });
        show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-autofill-ui-DialogFillUi  reason: not valid java name */
    public /* synthetic */ void m2102lambda$new$0$comandroidserverautofilluiDialogFillUi(DialogInterface d) {
        this.mCallback.onCanceled();
    }

    private int getVisibleDatasetsMaxCount() {
        if (AutofillManagerService.getVisibleDatasetsMaxCount() > 0) {
            int maxCount = AutofillManagerService.getVisibleDatasetsMaxCount();
            if (Helper.sVerbose) {
                Slog.v(TAG, "overriding maximum visible datasets to " + maxCount);
            }
            return maxCount;
        }
        return this.mContext.getResources().getInteger(17694724);
    }

    private void setDialogParamsAsBottomSheet() {
        Window window = this.mDialog.getWindow();
        window.setType(2038);
        window.addFlags(131074);
        window.setDimAmount(0.6f);
        window.addPrivateFlags(16);
        window.setSoftInputMode(32);
        window.setGravity(81);
        window.setCloseOnTouchOutside(true);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = -1;
        params.accessibilityTitle = this.mContext.getString(17039755);
        params.windowAnimations = 16974615;
    }

    private void setServiceIcon(View decor, Drawable serviceIcon) {
        if (serviceIcon == null) {
            return;
        }
        ImageView iconView = (ImageView) decor.findViewById(16908814);
        int actualWidth = serviceIcon.getMinimumWidth();
        int actualHeight = serviceIcon.getMinimumHeight();
        if (Helper.sDebug) {
            Slog.d(TAG, "Adding service icon (" + actualWidth + "x" + actualHeight + ")");
        }
        iconView.setImageDrawable(serviceIcon);
        iconView.setVisibility(0);
    }

    private void setHeader(View decor, FillResponse response) {
        RemoteViews presentation = response.getDialogHeader();
        if (presentation == null) {
            return;
        }
        ViewGroup container = (ViewGroup) decor.findViewById(16908803);
        RemoteViews.InteractionHandler interceptionHandler = new RemoteViews.InteractionHandler() { // from class: com.android.server.autofill.ui.DialogFillUi$$ExternalSyntheticLambda4
            public final boolean onInteraction(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse remoteResponse) {
                return DialogFillUi.this.m2104lambda$setHeader$1$comandroidserverautofilluiDialogFillUi(view, pendingIntent, remoteResponse);
            }
        };
        View content = presentation.applyWithTheme(this.mContext, (ViewGroup) decor, interceptionHandler, this.mThemeId);
        container.addView(content);
        container.setVisibility(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setHeader$1$com-android-server-autofill-ui-DialogFillUi  reason: not valid java name */
    public /* synthetic */ boolean m2104lambda$setHeader$1$comandroidserverautofilluiDialogFillUi(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse r) {
        if (pendingIntent != null) {
            this.mCallback.startIntentSender(pendingIntent.getIntentSender());
            return true;
        }
        return true;
    }

    private void setDismissButton(View decor) {
        TextView noButton = (TextView) decor.findViewById(16908805);
        noButton.setText(17039764);
        noButton.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.autofill.ui.DialogFillUi$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                DialogFillUi.this.m2103x5e648a1e(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setDismissButton$2$com-android-server-autofill-ui-DialogFillUi  reason: not valid java name */
    public /* synthetic */ void m2103x5e648a1e(View v) {
        this.mCallback.onDismissed();
    }

    private void setContinueButton(View decor, View.OnClickListener listener) {
        TextView yesButton = (TextView) decor.findViewById(16908807);
        yesButton.setText(17039727);
        yesButton.setOnClickListener(listener);
        yesButton.setVisibility(0);
    }

    private void initialAuthenticationLayout(View decor, final FillResponse response) {
        RemoteViews presentation = response.getDialogPresentation();
        if (presentation == null) {
            presentation = response.getPresentation();
        }
        if (presentation == null) {
            throw new RuntimeException("No presentation for fill dialog authentication");
        }
        ViewGroup container = (ViewGroup) decor.findViewById(16908802);
        RemoteViews.InteractionHandler interceptionHandler = new RemoteViews.InteractionHandler() { // from class: com.android.server.autofill.ui.DialogFillUi$$ExternalSyntheticLambda0
            public final boolean onInteraction(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse remoteResponse) {
                return DialogFillUi.this.m2097xf748fe91(view, pendingIntent, remoteResponse);
            }
        };
        View content = presentation.applyWithTheme(this.mContext, (ViewGroup) decor, interceptionHandler, this.mThemeId);
        container.addView(content);
        container.setVisibility(0);
        container.setFocusable(true);
        container.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.autofill.ui.DialogFillUi$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                DialogFillUi.this.m2098x91e9c112(response, view);
            }
        });
        setContinueButton(decor, new View.OnClickListener() { // from class: com.android.server.autofill.ui.DialogFillUi$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                DialogFillUi.this.m2099x2c8a8393(response, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initialAuthenticationLayout$3$com-android-server-autofill-ui-DialogFillUi  reason: not valid java name */
    public /* synthetic */ boolean m2097xf748fe91(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse r) {
        if (pendingIntent != null) {
            this.mCallback.startIntentSender(pendingIntent.getIntentSender());
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initialAuthenticationLayout$4$com-android-server-autofill-ui-DialogFillUi  reason: not valid java name */
    public /* synthetic */ void m2098x91e9c112(FillResponse response, View v) {
        this.mCallback.onResponsePicked(response);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initialAuthenticationLayout$5$com-android-server-autofill-ui-DialogFillUi  reason: not valid java name */
    public /* synthetic */ void m2099x2c8a8393(FillResponse response, View v) {
        this.mCallback.onResponsePicked(response);
    }

    private ArrayList<ViewItem> createDatasetItems(FillResponse response, AutofillId focusedViewId) {
        Pattern filterPattern;
        String valueText;
        boolean filterable;
        int datasetCount = response.getDatasets().size();
        if (Helper.sVerbose) {
            Slog.v(TAG, "Number datasets: " + datasetCount + " max visible: " + this.mVisibleDatasetsMaxCount);
        }
        RemoteViews.InteractionHandler interceptionHandler = new RemoteViews.InteractionHandler() { // from class: com.android.server.autofill.ui.DialogFillUi$$ExternalSyntheticLambda9
            public final boolean onInteraction(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse remoteResponse) {
                return DialogFillUi.this.m2096xb404ff8c(view, pendingIntent, remoteResponse);
            }
        };
        ArrayList<ViewItem> items = new ArrayList<>(datasetCount);
        for (int i = 0; i < datasetCount; i++) {
            Dataset dataset = (Dataset) response.getDatasets().get(i);
            int index = dataset.getFieldIds().indexOf(focusedViewId);
            if (index >= 0) {
                RemoteViews presentation = dataset.getFieldDialogPresentation(index);
                if (presentation == null) {
                    if (Helper.sDebug) {
                        Slog.w(TAG, "not displaying UI on field " + focusedViewId + " because service didn't provide a presentation for it on " + dataset);
                    }
                } else {
                    try {
                        if (Helper.sVerbose) {
                            try {
                                Slog.v(TAG, "setting remote view for " + focusedViewId);
                            } catch (RuntimeException e) {
                                e = e;
                                Slog.e(TAG, "Error inflating remote views", e);
                            }
                        }
                        View view = presentation.applyWithTheme(this.mContext, null, interceptionHandler, this.mThemeId);
                        Dataset.DatasetFieldFilter filter = dataset.getFilter(index);
                        String valueText2 = null;
                        if (filter == null) {
                            AutofillValue value = (AutofillValue) dataset.getFieldValues().get(index);
                            if (value != null && value.isText()) {
                                valueText2 = value.getTextValue().toString().toLowerCase();
                            }
                            filterPattern = null;
                            valueText = valueText2;
                            filterable = true;
                        } else {
                            Pattern filterPattern2 = filter.pattern;
                            if (filterPattern2 != null) {
                                filterPattern = filterPattern2;
                                valueText = null;
                                filterable = true;
                            } else {
                                if (Helper.sVerbose) {
                                    Slog.v(TAG, "Explicitly disabling filter at id " + focusedViewId + " for dataset #" + index);
                                }
                                filterPattern = filterPattern2;
                                valueText = null;
                                filterable = false;
                            }
                        }
                        items.add(new ViewItem(dataset, filterPattern, filterable, valueText, view));
                    } catch (RuntimeException e2) {
                        e = e2;
                    }
                }
            }
        }
        return items;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$createDatasetItems$6$com-android-server-autofill-ui-DialogFillUi  reason: not valid java name */
    public /* synthetic */ boolean m2096xb404ff8c(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse r) {
        if (pendingIntent != null) {
            this.mCallback.startIntentSender(pendingIntent.getIntentSender());
            return true;
        }
        return true;
    }

    private void initialDatasetLayout(View decor, String filterText) {
        final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() { // from class: com.android.server.autofill.ui.DialogFillUi$$ExternalSyntheticLambda6
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                DialogFillUi.this.m2100xe860a8e7(adapterView, view, i, j);
            }
        };
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setVisibility(0);
        this.mListView.setOnItemClickListener(onItemClickListener);
        if (this.mAdapter.getCount() == 1) {
            setContinueButton(decor, new View.OnClickListener() { // from class: com.android.server.autofill.ui.DialogFillUi$$ExternalSyntheticLambda7
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    onItemClickListener.onItemClick(null, null, 0, 0L);
                }
            });
        }
        if (filterText == null) {
            this.mFilterText = null;
        } else {
            this.mFilterText = filterText.toLowerCase();
        }
        final int oldCount = this.mAdapter.getCount();
        this.mAdapter.getFilter().filter(this.mFilterText, new Filter.FilterListener() { // from class: com.android.server.autofill.ui.DialogFillUi$$ExternalSyntheticLambda8
            @Override // android.widget.Filter.FilterListener
            public final void onFilterComplete(int i) {
                DialogFillUi.this.m2101x1da22de9(oldCount, i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initialDatasetLayout$7$com-android-server-autofill-ui-DialogFillUi  reason: not valid java name */
    public /* synthetic */ void m2100xe860a8e7(AdapterView adapter, View view, int position, long id) {
        ViewItem vi = this.mAdapter.getItem(position);
        this.mCallback.onDatasetPicked(vi.dataset);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$initialDatasetLayout$9$com-android-server-autofill-ui-DialogFillUi  reason: not valid java name */
    public /* synthetic */ void m2101x1da22de9(int oldCount, int count) {
        if (this.mDestroyed) {
            return;
        }
        if (count <= 0) {
            if (Helper.sDebug) {
                String str = this.mFilterText;
                int size = str != null ? str.length() : 0;
                Slog.d(TAG, "No dataset matches filter with " + size + " chars");
            }
            this.mCallback.onCanceled();
            return;
        }
        if (this.mAdapter.getCount() <= this.mVisibleDatasetsMaxCount) {
            this.mListView.setVerticalScrollBarEnabled(false);
        } else {
            this.mListView.setVerticalScrollBarEnabled(true);
            this.mListView.onVisibilityAggregated(true);
        }
        if (this.mAdapter.getCount() != oldCount) {
            this.mListView.requestLayout();
        }
    }

    private void show() {
        Slog.i(TAG, "Showing fill dialog");
        this.mDialog.show();
        this.mOverlayControl.hideOverlays();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isShowing() {
        return this.mDialog.isShowing();
    }

    void hide() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "Hiding fill dialog.");
        }
        try {
            this.mDialog.hide();
        } finally {
            this.mOverlayControl.showOverlays();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroy() {
        try {
            if (Helper.sDebug) {
                Slog.d(TAG, "destroy()");
            }
            throwIfDestroyed();
            this.mDialog.dismiss();
            this.mDestroyed = true;
        } finally {
            this.mOverlayControl.showOverlays();
        }
    }

    private void throwIfDestroyed() {
        if (this.mDestroyed) {
            throw new IllegalStateException("cannot interact with a destroyed instance");
        }
    }

    public String toString() {
        return "NO TITLE";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("service: ");
        pw.println(this.mServicePackageName);
        pw.print(prefix);
        pw.print("app: ");
        pw.println(this.mComponentName.toShortString());
        pw.print(prefix);
        pw.print("theme id: ");
        pw.print(this.mThemeId);
        switch (this.mThemeId) {
            case THEME_ID_DARK /* 16974827 */:
                pw.println(" (dark)");
                break;
            case THEME_ID_LIGHT /* 16974838 */:
                pw.println(" (light)");
                break;
            default:
                pw.println("(UNKNOWN_MODE)");
                break;
        }
        View view = this.mDialog.getWindow().getDecorView();
        int[] loc = view.getLocationOnScreen();
        pw.print(prefix);
        pw.print("coordinates: ");
        pw.print('(');
        pw.print(loc[0]);
        pw.print(',');
        pw.print(loc[1]);
        pw.print(')');
        pw.print('(');
        pw.print(loc[0] + view.getWidth());
        pw.print(',');
        pw.print(loc[1] + view.getHeight());
        pw.println(')');
        pw.print(prefix);
        pw.print("destroyed: ");
        pw.println(this.mDestroyed);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void announceSearchResultIfNeeded() {
        if (AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            if (this.mAnnounceFilterResult == null) {
                this.mAnnounceFilterResult = new AnnounceFilterResult();
            }
            this.mAnnounceFilterResult.post();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class AnnounceFilterResult implements Runnable {
        private static final int SEARCH_RESULT_ANNOUNCEMENT_DELAY = 1000;

        private AnnounceFilterResult() {
        }

        public void post() {
            remove();
            DialogFillUi.this.mListView.postDelayed(this, 1000L);
        }

        public void remove() {
            DialogFillUi.this.mListView.removeCallbacks(this);
        }

        @Override // java.lang.Runnable
        public void run() {
            String text;
            int count = DialogFillUi.this.mListView.getAdapter().getCount();
            if (count <= 0) {
                text = DialogFillUi.this.mContext.getString(17039756);
            } else {
                Map<String, Object> arguments = new HashMap<>();
                arguments.put(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.valueOf(count));
                text = PluralsMessageFormatter.format(DialogFillUi.this.mContext.getResources(), arguments, 17039757);
            }
            DialogFillUi.this.mListView.announceForAccessibility(text);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ItemsAdapter extends BaseAdapter implements Filterable {
        private final List<ViewItem> mAllItems;
        private final List<ViewItem> mFilteredItems;

        ItemsAdapter(List<ViewItem> items) {
            ArrayList arrayList = new ArrayList();
            this.mFilteredItems = arrayList;
            this.mAllItems = Collections.unmodifiableList(new ArrayList(items));
            arrayList.addAll(items);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: com.android.server.autofill.ui.DialogFillUi$ItemsAdapter$1  reason: invalid class name */
        /* loaded from: classes.dex */
        public class AnonymousClass1 extends Filter {
            AnonymousClass1() {
            }

            @Override // android.widget.Filter
            protected Filter.FilterResults performFiltering(final CharSequence filterText) {
                List<ViewItem> filtered = (List) ItemsAdapter.this.mAllItems.stream().filter(new Predicate() { // from class: com.android.server.autofill.ui.DialogFillUi$ItemsAdapter$1$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        boolean matches;
                        matches = ((DialogFillUi.ViewItem) obj).matches(filterText);
                        return matches;
                    }
                }).collect(Collectors.toList());
                Filter.FilterResults results = new Filter.FilterResults();
                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            @Override // android.widget.Filter
            protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                int oldItemCount = ItemsAdapter.this.mFilteredItems.size();
                ItemsAdapter.this.mFilteredItems.clear();
                if (results.count > 0) {
                    List<ViewItem> items = (List) results.values;
                    ItemsAdapter.this.mFilteredItems.addAll(items);
                }
                boolean resultCountChanged = oldItemCount != ItemsAdapter.this.mFilteredItems.size();
                if (resultCountChanged) {
                    DialogFillUi.this.announceSearchResultIfNeeded();
                }
                ItemsAdapter.this.notifyDataSetChanged();
            }
        }

        @Override // android.widget.Filterable
        public Filter getFilter() {
            return new AnonymousClass1();
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return this.mFilteredItems.size();
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // android.widget.Adapter
        public ViewItem getItem(int position) {
            return this.mFilteredItems.get(position);
        }

        @Override // android.widget.Adapter
        public long getItemId(int position) {
            return position;
        }

        @Override // android.widget.Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).view;
        }

        public String toString() {
            return "ItemsAdapter: [all=" + this.mAllItems + ", filtered=" + this.mFilteredItems + "]";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ViewItem {
        public final Dataset dataset;
        public final Pattern filter;
        public final boolean filterable;
        public final String value;
        public final View view;

        ViewItem(Dataset dataset, Pattern filter, boolean filterable, String value, View view) {
            this.dataset = dataset;
            this.value = value;
            this.view = view;
            this.filter = filter;
            this.filterable = filterable;
        }

        public boolean matches(CharSequence filterText) {
            if (TextUtils.isEmpty(filterText)) {
                return true;
            }
            if (this.filterable) {
                String constraintLowerCase = filterText.toString().toLowerCase();
                Pattern pattern = this.filter;
                if (pattern != null) {
                    return pattern.matcher(constraintLowerCase).matches();
                }
                String str = this.value;
                if (str == null) {
                    return this.dataset.getAuthentication() == null;
                }
                return str.toLowerCase().startsWith(constraintLowerCase);
            }
            return false;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder("ViewItem:[view=").append(this.view.getAutofillId());
            Dataset dataset = this.dataset;
            String datasetId = dataset == null ? null : dataset.getId();
            if (datasetId != null) {
                builder.append(", dataset=").append(datasetId);
            }
            if (this.value != null) {
                builder.append(", value=").append(this.value.length()).append("_chars");
            }
            if (this.filterable) {
                builder.append(", filterable");
            }
            if (this.filter != null) {
                builder.append(", filter=").append(this.filter.pattern().length()).append("_chars");
            }
            return builder.append(']').toString();
        }
    }
}
