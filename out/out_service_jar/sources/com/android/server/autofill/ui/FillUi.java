package com.android.server.autofill.ui;

import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.text.TextUtils;
import android.util.PluralsMessageFormatter;
import android.util.Slog;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutofillWindowPresenter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.server.UiThread;
import com.android.server.am.AssistDataRequester;
import com.android.server.autofill.AutofillManagerService;
import com.android.server.autofill.Helper;
import com.android.server.autofill.ui.FillUi;
import com.android.server.pm.PackageManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class FillUi {
    private static final String TAG = "FillUi";
    private static final int THEME_ID_DARK = 16974825;
    private static final int THEME_ID_LIGHT = 16974837;
    private static final TypedValue sTempTypedValue = new TypedValue();
    private final ItemsAdapter mAdapter;
    private AnnounceFilterResult mAnnounceFilterResult;
    private final Callback mCallback;
    private int mContentHeight;
    private int mContentWidth;
    private final Context mContext;
    private boolean mDestroyed;
    private String mFilterText;
    private final View mFooter;
    private final boolean mFullScreen;
    private final View mHeader;
    private final ListView mListView;
    private final int mThemeId;
    private final int mVisibleDatasetsMaxCount;
    private final AnchoredWindow mWindow;
    private final Point mTempPoint = new Point();
    private final AutofillWindowPresenter mWindowPresenter = new AutofillWindowPresenter();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Callback {
        void cancelSession();

        void dispatchUnhandledKey(KeyEvent keyEvent);

        void onCanceled();

        void onDatasetPicked(Dataset dataset);

        void onDestroy();

        void onResponsePicked(FillResponse fillResponse);

        void requestHideFillUi();

        void requestShowFillUi(int i, int i2, IAutofillWindowPresenter iAutofillWindowPresenter);

        void startIntentSender(IntentSender intentSender);
    }

    public static boolean isFullScreen(Context context) {
        if (Helper.sFullScreenMode != null) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "forcing full-screen mode to " + Helper.sFullScreenMode);
            }
            return Helper.sFullScreenMode.booleanValue();
        }
        return context.getPackageManager().hasSystemFeature("android.software.leanback");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FillUi(Context context, final FillResponse response, AutofillId focusedViewId, String filterText, OverlayControl overlayControl, CharSequence serviceLabel, Drawable serviceIcon, boolean nightMode, Callback callback) {
        ViewGroup decor;
        RemoteViews.InteractionHandler interceptionHandler;
        RemoteViews.InteractionHandler interactionBlocker;
        LayoutInflater inflater;
        RemoteViews headerPresentation;
        String valueText;
        boolean filterable;
        AutofillId autofillId = focusedViewId;
        if (Helper.sVerbose) {
            Slog.v(TAG, "nightMode: " + nightMode);
        }
        int i = nightMode ? THEME_ID_DARK : THEME_ID_LIGHT;
        this.mThemeId = i;
        this.mCallback = callback;
        boolean isFullScreen = isFullScreen(context);
        this.mFullScreen = isFullScreen;
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, i);
        this.mContext = contextThemeWrapper;
        LayoutInflater inflater2 = LayoutInflater.from(contextThemeWrapper);
        RemoteViews headerPresentation2 = response.getHeader();
        RemoteViews footerPresentation = response.getFooter();
        if (isFullScreen) {
            decor = (ViewGroup) inflater2.inflate(17367107, (ViewGroup) null);
        } else if (headerPresentation2 != null || footerPresentation != null) {
            decor = (ViewGroup) inflater2.inflate(17367108, (ViewGroup) null);
        } else {
            decor = (ViewGroup) inflater2.inflate(17367106, (ViewGroup) null);
        }
        decor.setClipToOutline(true);
        TextView titleView = (TextView) decor.findViewById(16908801);
        if (titleView != null) {
            titleView.setText(contextThemeWrapper.getString(17039789, serviceLabel));
        }
        ImageView iconView = (ImageView) decor.findViewById(16908798);
        if (iconView != null) {
            iconView.setImageDrawable(serviceIcon);
        }
        if (isFullScreen) {
            Point outPoint = this.mTempPoint;
            contextThemeWrapper.getDisplayNoVerify().getSize(outPoint);
            this.mContentWidth = -1;
            this.mContentHeight = outPoint.y / 2;
            if (Helper.sVerbose) {
                Slog.v(TAG, "initialized fillscreen LayoutParams " + this.mContentWidth + "," + this.mContentHeight);
            }
        }
        decor.addOnUnhandledKeyEventListener(new View.OnUnhandledKeyEventListener() { // from class: com.android.server.autofill.ui.FillUi$$ExternalSyntheticLambda2
            @Override // android.view.View.OnUnhandledKeyEventListener
            public final boolean onUnhandledKeyEvent(View view, KeyEvent keyEvent) {
                return FillUi.this.m2114lambda$new$0$comandroidserverautofilluiFillUi(view, keyEvent);
            }
        });
        if (AutofillManagerService.getVisibleDatasetsMaxCount() <= 0) {
            this.mVisibleDatasetsMaxCount = contextThemeWrapper.getResources().getInteger(17694724);
        } else {
            int visibleDatasetsMaxCount = AutofillManagerService.getVisibleDatasetsMaxCount();
            this.mVisibleDatasetsMaxCount = visibleDatasetsMaxCount;
            if (Helper.sVerbose) {
                Slog.v(TAG, "overriding maximum visible datasets to " + visibleDatasetsMaxCount);
            }
        }
        RemoteViews.InteractionHandler interceptionHandler2 = new RemoteViews.InteractionHandler() { // from class: com.android.server.autofill.ui.FillUi$$ExternalSyntheticLambda3
            public final boolean onInteraction(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse remoteResponse) {
                return FillUi.this.m2115lambda$new$1$comandroidserverautofilluiFillUi(view, pendingIntent, remoteResponse);
            }
        };
        if (response.getAuthentication() != null) {
            this.mHeader = null;
            this.mListView = null;
            this.mFooter = null;
            this.mAdapter = null;
            ViewGroup container = (ViewGroup) decor.findViewById(16908800);
            try {
                View content = response.getPresentation().applyWithTheme(contextThemeWrapper, decor, interceptionHandler2, i);
                container.addView(content);
                container.setFocusable(true);
                container.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.autofill.ui.FillUi$$ExternalSyntheticLambda4
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        FillUi.this.m2116lambda$new$2$comandroidserverautofilluiFillUi(response, view);
                    }
                });
                if (!isFullScreen) {
                    Point maxSize = this.mTempPoint;
                    resolveMaxWindowSize(contextThemeWrapper, maxSize);
                    content.getLayoutParams().width = isFullScreen ? maxSize.x : -2;
                    content.getLayoutParams().height = -2;
                    int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxSize.x, Integer.MIN_VALUE);
                    int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxSize.y, Integer.MIN_VALUE);
                    decor.measure(widthMeasureSpec, heightMeasureSpec);
                    this.mContentWidth = content.getMeasuredWidth();
                    this.mContentHeight = content.getMeasuredHeight();
                }
                this.mWindow = new AnchoredWindow(decor, overlayControl);
                requestShowFillUi();
                return;
            } catch (RuntimeException e) {
                callback.onCanceled();
                Slog.e(TAG, "Error inflating remote views", e);
                this.mWindow = null;
                return;
            }
        }
        int datasetCount = response.getDatasets().size();
        if (Helper.sVerbose) {
            Slog.v(TAG, "Number datasets: " + datasetCount + " max visible: " + this.mVisibleDatasetsMaxCount);
        }
        RemoteViews.InteractionHandler interactionBlocker2 = null;
        if (headerPresentation2 == null) {
            this.mHeader = null;
        } else {
            RemoteViews.InteractionHandler interactionBlocker3 = newInteractionBlocker();
            View applyWithTheme = headerPresentation2.applyWithTheme(contextThemeWrapper, null, interactionBlocker3, i);
            this.mHeader = applyWithTheme;
            LinearLayout headerContainer = (LinearLayout) decor.findViewById(16908797);
            applyCancelAction(applyWithTheme, response.getCancelIds());
            if (Helper.sVerbose) {
                Slog.v(TAG, "adding header");
            }
            headerContainer.addView(applyWithTheme);
            headerContainer.setVisibility(0);
            interactionBlocker2 = interactionBlocker3;
        }
        if (footerPresentation == null) {
            this.mFooter = null;
        } else {
            LinearLayout footerContainer = (LinearLayout) decor.findViewById(16908796);
            if (footerContainer == null) {
                this.mFooter = null;
            } else {
                interactionBlocker2 = interactionBlocker2 == null ? newInteractionBlocker() : interactionBlocker2;
                View applyWithTheme2 = footerPresentation.applyWithTheme(contextThemeWrapper, null, interactionBlocker2, i);
                this.mFooter = applyWithTheme2;
                applyCancelAction(applyWithTheme2, response.getCancelIds());
                if (Helper.sVerbose) {
                    Slog.v(TAG, "adding footer");
                }
                footerContainer.addView(applyWithTheme2);
                footerContainer.setVisibility(0);
            }
        }
        ArrayList<ViewItem> items = new ArrayList<>(datasetCount);
        int i2 = 0;
        while (i2 < datasetCount) {
            Dataset dataset = (Dataset) response.getDatasets().get(i2);
            int datasetCount2 = datasetCount;
            int index = dataset.getFieldIds().indexOf(autofillId);
            if (index < 0) {
                interceptionHandler = interceptionHandler2;
                interactionBlocker = interactionBlocker2;
                inflater = inflater2;
                headerPresentation = headerPresentation2;
            } else {
                interactionBlocker = interactionBlocker2;
                RemoteViews presentation = dataset.getFieldPresentation(index);
                if (presentation == null) {
                    inflater = inflater2;
                    Slog.w(TAG, "not displaying UI on field " + autofillId + " because service didn't provide a presentation for it on " + dataset);
                    interceptionHandler = interceptionHandler2;
                    headerPresentation = headerPresentation2;
                } else {
                    inflater = inflater2;
                    try {
                        if (Helper.sVerbose) {
                            try {
                                Slog.v(TAG, "setting remote view for " + autofillId);
                            } catch (RuntimeException e2) {
                                e = e2;
                                interceptionHandler = interceptionHandler2;
                                headerPresentation = headerPresentation2;
                                Slog.e(TAG, "Error inflating remote views", e);
                                i2++;
                                autofillId = focusedViewId;
                                datasetCount = datasetCount2;
                                interactionBlocker2 = interactionBlocker;
                                inflater2 = inflater;
                                headerPresentation2 = headerPresentation;
                                interceptionHandler2 = interceptionHandler;
                            }
                        }
                        headerPresentation = headerPresentation2;
                    } catch (RuntimeException e3) {
                        e = e3;
                        interceptionHandler = interceptionHandler2;
                        headerPresentation = headerPresentation2;
                    }
                    try {
                        View view = presentation.applyWithTheme(this.mContext, null, interceptionHandler2, this.mThemeId);
                        Dataset.DatasetFieldFilter filter = dataset.getFilter(index);
                        Pattern filterPattern = null;
                        String valueText2 = null;
                        if (filter == null) {
                            interceptionHandler = interceptionHandler2;
                            AutofillValue value = (AutofillValue) dataset.getFieldValues().get(index);
                            if (value != null && value.isText()) {
                                valueText2 = value.getTextValue().toString().toLowerCase();
                            }
                            valueText = valueText2;
                            filterable = true;
                        } else {
                            interceptionHandler = interceptionHandler2;
                            filterPattern = filter.pattern;
                            if (filterPattern != null) {
                                valueText = null;
                                filterable = true;
                            } else {
                                if (Helper.sVerbose) {
                                    Slog.v(TAG, "Explicitly disabling filter at id " + autofillId + " for dataset #" + index);
                                }
                                valueText = null;
                                filterable = false;
                            }
                        }
                        applyCancelAction(view, response.getCancelIds());
                        items.add(new ViewItem(dataset, filterPattern, filterable, valueText, view));
                    } catch (RuntimeException e4) {
                        e = e4;
                        interceptionHandler = interceptionHandler2;
                        Slog.e(TAG, "Error inflating remote views", e);
                        i2++;
                        autofillId = focusedViewId;
                        datasetCount = datasetCount2;
                        interactionBlocker2 = interactionBlocker;
                        inflater2 = inflater;
                        headerPresentation2 = headerPresentation;
                        interceptionHandler2 = interceptionHandler;
                    }
                }
            }
            i2++;
            autofillId = focusedViewId;
            datasetCount = datasetCount2;
            interactionBlocker2 = interactionBlocker;
            inflater2 = inflater;
            headerPresentation2 = headerPresentation;
            interceptionHandler2 = interceptionHandler;
        }
        ItemsAdapter itemsAdapter = new ItemsAdapter(items);
        this.mAdapter = itemsAdapter;
        ListView listView = (ListView) decor.findViewById(16908799);
        this.mListView = listView;
        listView.setAdapter((ListAdapter) itemsAdapter);
        listView.setVisibility(0);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.android.server.autofill.ui.FillUi$$ExternalSyntheticLambda5
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view2, int i3, long j) {
                FillUi.this.m2117lambda$new$3$comandroidserverautofilluiFillUi(adapterView, view2, i3, j);
            }
        });
        if (filterText == null) {
            this.mFilterText = null;
        } else {
            this.mFilterText = filterText.toLowerCase();
        }
        applyNewFilterText();
        this.mWindow = new AnchoredWindow(decor, overlayControl);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-autofill-ui-FillUi  reason: not valid java name */
    public /* synthetic */ boolean m2114lambda$new$0$comandroidserverautofilluiFillUi(View view, KeyEvent event) {
        switch (event.getKeyCode()) {
            case 4:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 66:
            case 111:
                return false;
            default:
                this.mCallback.dispatchUnhandledKey(event);
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-autofill-ui-FillUi  reason: not valid java name */
    public /* synthetic */ boolean m2115lambda$new$1$comandroidserverautofilluiFillUi(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse r) {
        if (pendingIntent != null) {
            this.mCallback.startIntentSender(pendingIntent.getIntentSender());
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$2$com-android-server-autofill-ui-FillUi  reason: not valid java name */
    public /* synthetic */ void m2116lambda$new$2$comandroidserverautofilluiFillUi(FillResponse response, View v) {
        this.mCallback.onResponsePicked(response);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$3$com-android-server-autofill-ui-FillUi  reason: not valid java name */
    public /* synthetic */ void m2117lambda$new$3$comandroidserverautofilluiFillUi(AdapterView adapter, View view, int position, long id) {
        ViewItem vi = this.mAdapter.getItem(position);
        this.mCallback.onDatasetPicked(vi.dataset);
    }

    private void applyCancelAction(View rootView, int[] ids) {
        if (ids == null) {
            return;
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "fill UI has " + ids.length + " actions");
        }
        if (!(rootView instanceof ViewGroup)) {
            Slog.w(TAG, "cannot apply actions because fill UI root is not a ViewGroup: " + rootView);
            return;
        }
        ViewGroup root = (ViewGroup) rootView;
        for (int id : ids) {
            View child = root.findViewById(id);
            if (child == null) {
                Slog.w(TAG, "Ignoring cancel action for view " + id + " because it's not on " + root);
            } else {
                child.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.autofill.ui.FillUi$$ExternalSyntheticLambda6
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        FillUi.this.m2112lambda$applyCancelAction$4$comandroidserverautofilluiFillUi(view);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyCancelAction$4$com-android-server-autofill-ui-FillUi  reason: not valid java name */
    public /* synthetic */ void m2112lambda$applyCancelAction$4$comandroidserverautofilluiFillUi(View v) {
        if (Helper.sVerbose) {
            Slog.v(TAG, " Cancelling session after " + v + " clicked");
        }
        this.mCallback.cancelSession();
    }

    void requestShowFillUi() {
        this.mCallback.requestShowFillUi(this.mContentWidth, this.mContentHeight, this.mWindowPresenter);
    }

    private RemoteViews.InteractionHandler newInteractionBlocker() {
        return new RemoteViews.InteractionHandler() { // from class: com.android.server.autofill.ui.FillUi$$ExternalSyntheticLambda1
            public final boolean onInteraction(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse remoteResponse) {
                return FillUi.lambda$newInteractionBlocker$5(view, pendingIntent, remoteResponse);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$newInteractionBlocker$5(View view, PendingIntent pendingIntent, RemoteViews.RemoteResponse response) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "Ignoring click on " + view);
            return true;
        }
        return true;
    }

    private void applyNewFilterText() {
        final int oldCount = this.mAdapter.getCount();
        this.mAdapter.getFilter().filter(this.mFilterText, new Filter.FilterListener() { // from class: com.android.server.autofill.ui.FillUi$$ExternalSyntheticLambda0
            @Override // android.widget.Filter.FilterListener
            public final void onFilterComplete(int i) {
                FillUi.this.m2113xf58c4f57(oldCount, i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyNewFilterText$6$com-android-server-autofill-ui-FillUi  reason: not valid java name */
    public /* synthetic */ void m2113xf58c4f57(int oldCount, int count) {
        if (this.mDestroyed) {
            return;
        }
        if (count <= 0) {
            if (Helper.sDebug) {
                String str = this.mFilterText;
                int size = str != null ? str.length() : 0;
                Slog.d(TAG, "No dataset matches filter with " + size + " chars");
            }
            this.mCallback.requestHideFillUi();
            return;
        }
        if (updateContentSize()) {
            requestShowFillUi();
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

    public void setFilterText(String filterText) {
        String filterText2;
        throwIfDestroyed();
        if (this.mAdapter == null) {
            if (TextUtils.isEmpty(filterText)) {
                requestShowFillUi();
                return;
            } else {
                this.mCallback.requestHideFillUi();
                return;
            }
        }
        if (filterText == null) {
            filterText2 = null;
        } else {
            filterText2 = filterText.toLowerCase();
        }
        if (Objects.equals(this.mFilterText, filterText2)) {
            return;
        }
        this.mFilterText = filterText2;
        applyNewFilterText();
    }

    public void destroy(boolean notifyClient) {
        throwIfDestroyed();
        AnchoredWindow anchoredWindow = this.mWindow;
        if (anchoredWindow != null) {
            anchoredWindow.hide(false);
        }
        this.mCallback.onDestroy();
        if (notifyClient) {
            this.mCallback.requestHideFillUi();
        }
        this.mDestroyed = true;
    }

    private boolean updateContentSize() {
        ItemsAdapter itemsAdapter = this.mAdapter;
        if (itemsAdapter == null) {
            return false;
        }
        if (this.mFullScreen) {
            return true;
        }
        boolean changed = false;
        if (itemsAdapter.getCount() <= 0) {
            if (this.mContentWidth != 0) {
                this.mContentWidth = 0;
                changed = true;
            }
            if (this.mContentHeight != 0) {
                this.mContentHeight = 0;
                return true;
            }
            return changed;
        }
        Point maxSize = this.mTempPoint;
        resolveMaxWindowSize(this.mContext, maxSize);
        this.mContentWidth = 0;
        this.mContentHeight = 0;
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxSize.x, Integer.MIN_VALUE);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxSize.y, Integer.MIN_VALUE);
        int itemCount = this.mAdapter.getCount();
        View view = this.mHeader;
        if (view != null) {
            view.measure(widthMeasureSpec, heightMeasureSpec);
            changed = false | updateWidth(this.mHeader, maxSize) | updateHeight(this.mHeader, maxSize);
        }
        for (int i = 0; i < itemCount; i++) {
            View view2 = this.mAdapter.getItem(i).view;
            view2.measure(widthMeasureSpec, heightMeasureSpec);
            changed |= updateWidth(view2, maxSize);
            if (i < this.mVisibleDatasetsMaxCount) {
                changed |= updateHeight(view2, maxSize);
            }
        }
        View view3 = this.mFooter;
        if (view3 != null) {
            view3.measure(widthMeasureSpec, heightMeasureSpec);
            return changed | updateWidth(this.mFooter, maxSize) | updateHeight(this.mFooter, maxSize);
        }
        return changed;
    }

    private boolean updateWidth(View view, Point maxSize) {
        int clampedMeasuredWidth = Math.min(view.getMeasuredWidth(), maxSize.x);
        int newContentWidth = Math.max(this.mContentWidth, clampedMeasuredWidth);
        if (newContentWidth == this.mContentWidth) {
            return false;
        }
        this.mContentWidth = newContentWidth;
        return true;
    }

    private boolean updateHeight(View view, Point maxSize) {
        int clampedMeasuredHeight = Math.min(view.getMeasuredHeight(), maxSize.y);
        int i = this.mContentHeight;
        int newContentHeight = i + clampedMeasuredHeight;
        if (newContentHeight == i) {
            return false;
        }
        this.mContentHeight = newContentHeight;
        return true;
    }

    private void throwIfDestroyed() {
        if (this.mDestroyed) {
            throw new IllegalStateException("cannot interact with a destroyed instance");
        }
    }

    private static void resolveMaxWindowSize(Context context, Point outPoint) {
        context.getDisplayNoVerify().getSize(outPoint);
        TypedValue typedValue = sTempTypedValue;
        context.getTheme().resolveAttribute(17956884, typedValue, true);
        outPoint.x = (int) typedValue.getFraction(outPoint.x, outPoint.x);
        context.getTheme().resolveAttribute(17956883, typedValue, true);
        outPoint.y = (int) typedValue.getFraction(outPoint.y, outPoint.y);
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

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class AutofillWindowPresenter extends IAutofillWindowPresenter.Stub {
        private AutofillWindowPresenter() {
        }

        public void show(final WindowManager.LayoutParams p, Rect transitionEpicenter, boolean fitsSystemWindows, int layoutDirection) {
            if (Helper.sVerbose) {
                Slog.v(FillUi.TAG, "AutofillWindowPresenter.show(): fit=" + fitsSystemWindows + ", params=" + Helper.paramsToString(p));
            }
            UiThread.getHandler().post(new Runnable() { // from class: com.android.server.autofill.ui.FillUi$AutofillWindowPresenter$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    FillUi.AutofillWindowPresenter.this.m2121x6c320299(p);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$show$0$com-android-server-autofill-ui-FillUi$AutofillWindowPresenter  reason: not valid java name */
        public /* synthetic */ void m2121x6c320299(WindowManager.LayoutParams p) {
            FillUi.this.mWindow.show(p);
        }

        public void hide(Rect transitionEpicenter) {
            Handler handler = UiThread.getHandler();
            final AnchoredWindow anchoredWindow = FillUi.this.mWindow;
            Objects.requireNonNull(anchoredWindow);
            handler.post(new Runnable() { // from class: com.android.server.autofill.ui.FillUi$AutofillWindowPresenter$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    FillUi.AnchoredWindow.this.hide();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class AnchoredWindow {
        private final View mContentView;
        private final OverlayControl mOverlayControl;
        private WindowManager.LayoutParams mShowParams;
        private boolean mShowing;
        private final WindowManager mWm;

        AnchoredWindow(View contentView, OverlayControl overlayControl) {
            this.mWm = (WindowManager) contentView.getContext().getSystemService(WindowManager.class);
            this.mContentView = contentView;
            this.mOverlayControl = overlayControl;
        }

        public void show(WindowManager.LayoutParams params) {
            this.mShowParams = params;
            if (Helper.sVerbose) {
                Slog.v(FillUi.TAG, "show(): showing=" + this.mShowing + ", params=" + Helper.paramsToString(params));
            }
            try {
                params.packageName = PackageManagerService.PLATFORM_PACKAGE_NAME;
                params.setTitle("Autofill UI");
                if (!this.mShowing) {
                    params.accessibilityTitle = this.mContentView.getContext().getString(17039755);
                    this.mWm.addView(this.mContentView, params);
                    this.mOverlayControl.hideOverlays();
                    this.mShowing = true;
                    return;
                }
                this.mWm.updateViewLayout(this.mContentView, params);
            } catch (WindowManager.BadTokenException e) {
                if (Helper.sDebug) {
                    Slog.d(FillUi.TAG, "Filed with with token " + params.token + " gone.");
                }
                FillUi.this.mCallback.onDestroy();
            } catch (IllegalStateException e2) {
                Slog.wtf(FillUi.TAG, "Exception showing window " + params, e2);
                FillUi.this.mCallback.onDestroy();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void hide() {
            hide(true);
        }

        void hide(boolean destroyCallbackOnError) {
            try {
                try {
                    if (this.mShowing) {
                        this.mWm.removeView(this.mContentView);
                        this.mShowing = false;
                    }
                } catch (IllegalStateException e) {
                    Slog.e(FillUi.TAG, "Exception hiding window ", e);
                    if (destroyCallbackOnError) {
                        FillUi.this.mCallback.onDestroy();
                    }
                }
            } finally {
                this.mOverlayControl.showOverlays();
            }
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mCallback: ");
        pw.println(this.mCallback != null);
        pw.print(prefix);
        pw.print("mFullScreen: ");
        pw.println(this.mFullScreen);
        pw.print(prefix);
        pw.print("mVisibleDatasetsMaxCount: ");
        pw.println(this.mVisibleDatasetsMaxCount);
        if (this.mHeader != null) {
            pw.print(prefix);
            pw.print("mHeader: ");
            pw.println(this.mHeader);
        }
        if (this.mListView != null) {
            pw.print(prefix);
            pw.print("mListView: ");
            pw.println(this.mListView);
        }
        if (this.mFooter != null) {
            pw.print(prefix);
            pw.print("mFooter: ");
            pw.println(this.mFooter);
        }
        if (this.mAdapter != null) {
            pw.print(prefix);
            pw.print("mAdapter: ");
            pw.println(this.mAdapter);
        }
        if (this.mFilterText != null) {
            pw.print(prefix);
            pw.print("mFilterText: ");
            Helper.printlnRedactedText(pw, this.mFilterText);
        }
        pw.print(prefix);
        pw.print("mContentWidth: ");
        pw.println(this.mContentWidth);
        pw.print(prefix);
        pw.print("mContentHeight: ");
        pw.println(this.mContentHeight);
        pw.print(prefix);
        pw.print("mDestroyed: ");
        pw.println(this.mDestroyed);
        pw.print(prefix);
        pw.print("theme id: ");
        pw.print(this.mThemeId);
        switch (this.mThemeId) {
            case THEME_ID_DARK /* 16974825 */:
                pw.println(" (dark)");
                break;
            case THEME_ID_LIGHT /* 16974837 */:
                pw.println(" (light)");
                break;
            default:
                pw.println("(UNKNOWN_MODE)");
                break;
        }
        if (this.mWindow != null) {
            pw.print(prefix);
            pw.print("mWindow: ");
            String prefix2 = prefix + "  ";
            pw.println();
            pw.print(prefix2);
            pw.print("showing: ");
            pw.println(this.mWindow.mShowing);
            pw.print(prefix2);
            pw.print("view: ");
            pw.println(this.mWindow.mContentView);
            if (this.mWindow.mShowParams != null) {
                pw.print(prefix2);
                pw.print("params: ");
                pw.println(this.mWindow.mShowParams);
            }
            pw.print(prefix2);
            pw.print("screen coordinates: ");
            if (this.mWindow.mContentView == null) {
                pw.println("N/A");
                return;
            }
            int[] coordinates = this.mWindow.mContentView.getLocationOnScreen();
            pw.print(coordinates[0]);
            pw.print("x");
            pw.println(coordinates[1]);
        }
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
        /* renamed from: com.android.server.autofill.ui.FillUi$ItemsAdapter$1  reason: invalid class name */
        /* loaded from: classes.dex */
        public class AnonymousClass1 extends Filter {
            AnonymousClass1() {
            }

            @Override // android.widget.Filter
            protected Filter.FilterResults performFiltering(final CharSequence filterText) {
                List<ViewItem> filtered = (List) ItemsAdapter.this.mAllItems.stream().filter(new Predicate() { // from class: com.android.server.autofill.ui.FillUi$ItemsAdapter$1$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        boolean matches;
                        matches = ((FillUi.ViewItem) obj).matches(filterText);
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
                    FillUi.this.announceSearchResultIfNeeded();
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
    public final class AnnounceFilterResult implements Runnable {
        private static final int SEARCH_RESULT_ANNOUNCEMENT_DELAY = 1000;

        private AnnounceFilterResult() {
        }

        public void post() {
            remove();
            FillUi.this.mListView.postDelayed(this, 1000L);
        }

        public void remove() {
            FillUi.this.mListView.removeCallbacks(this);
        }

        @Override // java.lang.Runnable
        public void run() {
            String text;
            int count = FillUi.this.mListView.getAdapter().getCount();
            if (count <= 0) {
                text = FillUi.this.mContext.getString(17039756);
            } else {
                Map<String, Object> arguments = new HashMap<>();
                arguments.put(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.valueOf(count));
                text = PluralsMessageFormatter.format(FillUi.this.mContext.getResources(), arguments, 17039757);
            }
            FillUi.this.mListView.announceForAccessibility(text);
        }
    }
}
