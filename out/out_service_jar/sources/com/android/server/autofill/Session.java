package com.android.server.autofill;

import android.app.ActivityTaskManager;
import android.app.IAssistDataReceiver;
import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.service.autofill.AutofillFieldClassificationService;
import android.service.autofill.CompositeUserData;
import android.service.autofill.Dataset;
import android.service.autofill.FieldClassification;
import android.service.autofill.FieldClassificationUserData;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.InlinePresentation;
import android.service.autofill.InternalSanitizer;
import android.service.autofill.InternalValidator;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.service.autofill.UserData;
import android.service.autofill.ValueFinder;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.KeyEvent;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManagerClient;
import android.view.autofill.IAutofillWindowPresenter;
import android.view.inputmethod.InlineSuggestionsRequest;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.function.HexConsumer;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.autofill.RemoteFillService;
import com.android.server.autofill.Session;
import com.android.server.autofill.ViewState;
import com.android.server.autofill.ui.AutoFillUI;
import com.android.server.autofill.ui.InlineFillUi;
import com.android.server.autofill.ui.PendingUi;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
/* loaded from: classes.dex */
public final class Session implements RemoteFillService.FillServiceCallbacks, ViewState.Listener, AutoFillUI.AutoFillUiCallback, ValueFinder {
    private static final String ACTION_DELAYED_FILL = "android.service.autofill.action.DELAYED_FILL";
    static final int AUGMENTED_AUTOFILL_REQUEST_ID = 1;
    private static final String EXTRA_REQUEST_ID = "android.service.autofill.extra.REQUEST_ID";
    public static final int STATE_ACTIVE = 1;
    public static final int STATE_FINISHED = 2;
    public static final int STATE_REMOVED = 3;
    public static final int STATE_UNKNOWN = 0;
    private static final String TAG = "AutofillSession";
    private static AtomicInteger sIdCounter = new AtomicInteger(2);
    public final int id;
    private IBinder mActivityToken;
    private final AssistDataReceiverImpl mAssistReceiver;
    private Runnable mAugmentedAutofillDestroyer;
    private ArrayList<AutofillId> mAugmentedAutofillableIds;
    private ArrayList<LogMaker> mAugmentedRequestsLogs;
    private IAutoFillManagerClient mClient;
    private Bundle mClientState;
    private IBinder.DeathRecipient mClientVulture;
    private final boolean mCompatMode;
    private final ComponentName mComponentName;
    private final Context mContext;
    private ArrayList<FillContext> mContexts;
    private AutofillId mCurrentViewId;
    private final BroadcastReceiver mDelayedFillBroadcastReceiver;
    private boolean mDelayedFillBroadcastReceiverRegistered;
    private PendingIntent mDelayedFillPendingIntent;
    private boolean mDestroyed;
    public final int mFlags;
    private final Handler mHandler;
    private boolean mHasCallback;
    private final AutofillInlineSessionController mInlineSessionController;
    private Pair<Integer, InlineSuggestionsRequest> mLastInlineSuggestionsRequest;
    final Object mLock;
    private final MetricsLogger mMetricsLogger;
    private PendingUi mPendingSaveUi;
    private PresentationStatsEventLogger mPresentationStatsEventLogger;
    private final RemoteFillService mRemoteFillService;
    private final SparseArray<LogMaker> mRequestLogs;
    private SparseArray<FillResponse> mResponses;
    private boolean mSaveOnAllViewsInvisible;
    private ArrayList<String> mSelectedDatasetIds;
    private final AutofillManagerServiceImpl mService;
    private final SessionFlags mSessionFlags;
    private int mSessionState;
    private final long mStartTime;
    private final AutoFillUI mUi;
    private final LocalLog mUiLatencyHistory;
    private long mUiShownTime;
    private AssistStructure.ViewNode mUrlBar;
    private final ArrayMap<AutofillId, ViewState> mViewStates;
    private final LocalLog mWtfHistory;
    public final int taskId;
    public final int uid;
    public final int userId;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    @interface SessionState {
    }

    public void onSwitchInputMethodLocked() {
        if (!this.mSessionFlags.mExpiredResponse && shouldResetSessionStateOnInputMethodSwitch()) {
            this.mSessionFlags.mExpiredResponse = true;
            this.mAugmentedAutofillableIds = null;
            if (this.mSessionFlags.mAugmentedAutofillOnly) {
                this.mCurrentViewId = null;
            }
        }
    }

    private boolean shouldResetSessionStateOnInputMethodSwitch() {
        if (this.mService.getRemoteInlineSuggestionRenderServiceLocked() == null) {
            return false;
        }
        if (this.mSessionFlags.mInlineSupportedByService) {
            return true;
        }
        ViewState state = this.mViewStates.get(this.mCurrentViewId);
        return (state == null || (state.getState() & 4096) == 0) ? false : true;
    }

    /* loaded from: classes.dex */
    public final class SessionFlags {
        private boolean mAugmentedAutofillOnly;
        private boolean mAutofillDisabled;
        private boolean mExpiredResponse;
        private boolean mFillDialogDisabled;
        private boolean mInlineSupportedByService;
        private boolean mShowingSaveUi;

        private SessionFlags() {
            Session.this = r1;
        }
    }

    /* loaded from: classes.dex */
    public final class AssistDataReceiverImpl extends IAssistDataReceiver.Stub {
        private FillRequest mLastFillRequest;
        private FillRequest mPendingFillRequest;
        private InlineSuggestionsRequest mPendingInlineSuggestionsRequest;
        private boolean mWaitForInlineRequest;

        private AssistDataReceiverImpl() {
            Session.this = r1;
        }

        Consumer<InlineSuggestionsRequest> newAutofillRequestLocked(final ViewState viewState, boolean isInlineRequest) {
            this.mPendingFillRequest = null;
            this.mWaitForInlineRequest = isInlineRequest;
            this.mPendingInlineSuggestionsRequest = null;
            if (isInlineRequest) {
                return new Consumer() { // from class: com.android.server.autofill.Session$AssistDataReceiverImpl$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        Session.AssistDataReceiverImpl.this.m2062xd9a4af1f(viewState, (InlineSuggestionsRequest) obj);
                    }
                };
            }
            return null;
        }

        /* renamed from: lambda$newAutofillRequestLocked$0$com-android-server-autofill-Session$AssistDataReceiverImpl */
        public /* synthetic */ void m2062xd9a4af1f(ViewState viewState, InlineSuggestionsRequest inlineSuggestionsRequest) {
            synchronized (Session.this.mLock) {
                if (this.mWaitForInlineRequest && this.mPendingInlineSuggestionsRequest == null) {
                    this.mWaitForInlineRequest = inlineSuggestionsRequest != null;
                    this.mPendingInlineSuggestionsRequest = inlineSuggestionsRequest;
                    maybeRequestFillLocked();
                    viewState.resetState(65536);
                }
            }
        }

        void maybeRequestFillLocked() {
            if (this.mPendingFillRequest == null) {
                return;
            }
            if (this.mWaitForInlineRequest) {
                if (this.mPendingInlineSuggestionsRequest == null) {
                    return;
                }
                this.mPendingFillRequest = new FillRequest(this.mPendingFillRequest.getId(), this.mPendingFillRequest.getFillContexts(), this.mPendingFillRequest.getClientState(), this.mPendingFillRequest.getFlags(), this.mPendingInlineSuggestionsRequest, this.mPendingFillRequest.getDelayedFillIntentSender());
            }
            this.mLastFillRequest = this.mPendingFillRequest;
            Session.this.mRemoteFillService.onFillRequest(this.mPendingFillRequest);
            this.mPendingInlineSuggestionsRequest = null;
            this.mWaitForInlineRequest = false;
            this.mPendingFillRequest = null;
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [632=4] */
        public void onHandleAssistData(Bundle resultData) throws RemoteException {
            Object obj;
            int flags;
            if (Session.this.mRemoteFillService == null) {
                Session session = Session.this;
                session.wtf(null, "onHandleAssistData() called without a remote service. mForAugmentedAutofillOnly: %s", Boolean.valueOf(session.mSessionFlags.mAugmentedAutofillOnly));
                return;
            }
            AutofillId currentViewId = Session.this.mCurrentViewId;
            if (currentViewId == null) {
                Slog.w(Session.TAG, "No current view id - session might have finished");
                return;
            }
            AssistStructure structure = (AssistStructure) resultData.getParcelable(ActivityTaskManagerInternal.ASSIST_KEY_STRUCTURE);
            if (structure == null) {
                Slog.e(Session.TAG, "No assist structure - app might have crashed providing it");
                return;
            }
            Bundle receiverExtras = resultData.getBundle(ActivityTaskManagerInternal.ASSIST_KEY_RECEIVER_EXTRAS);
            if (receiverExtras == null) {
                Slog.e(Session.TAG, "No receiver extras - app might have crashed providing it");
                return;
            }
            int requestId = receiverExtras.getInt(Session.EXTRA_REQUEST_ID);
            if (Helper.sVerbose) {
                Slog.v(Session.TAG, "New structure for requestId " + requestId + ": " + structure);
            }
            Object obj2 = Session.this.mLock;
            synchronized (obj2) {
                try {
                    try {
                        try {
                            structure.ensureDataForAutofill();
                            ArrayList<AutofillId> ids = Helper.getAutofillIds(structure, false);
                            for (int i = 0; i < ids.size(); i++) {
                                try {
                                    ids.get(i).setSessionId(Session.this.id);
                                } catch (Throwable th) {
                                    e = th;
                                    obj = obj2;
                                    throw e;
                                }
                            }
                            int flags2 = structure.getFlags();
                            if (Session.this.mCompatMode) {
                                String[] urlBarIds = Session.this.mService.getUrlBarResourceIdsForCompatMode(Session.this.mComponentName.getPackageName());
                                if (Helper.sDebug) {
                                    Slog.d(Session.TAG, "url_bars in compat mode: " + Arrays.toString(urlBarIds));
                                }
                                if (urlBarIds != null) {
                                    Session.this.mUrlBar = Helper.sanitizeUrlBar(structure, urlBarIds);
                                    if (Session.this.mUrlBar != null) {
                                        AutofillId urlBarId = Session.this.mUrlBar.getAutofillId();
                                        if (Helper.sDebug) {
                                            Slog.d(Session.TAG, "Setting urlBar as id=" + urlBarId + " and domain " + Session.this.mUrlBar.getWebDomain());
                                        }
                                        ViewState viewState = new ViewState(urlBarId, Session.this, 512);
                                        Session.this.mViewStates.put(urlBarId, viewState);
                                    }
                                }
                                flags = flags2 | 2;
                            } else {
                                flags = flags2;
                            }
                            structure.sanitizeForParceling(true);
                            if (Session.this.mContexts == null) {
                                Session.this.mContexts = new ArrayList(1);
                            }
                            Session.this.mContexts.add(new FillContext(requestId, structure, currentViewId));
                            Session.this.cancelCurrentRequestLocked();
                            int numContexts = Session.this.mContexts.size();
                            for (int i2 = 0; i2 < numContexts; i2++) {
                                Session session2 = Session.this;
                                session2.fillContextWithAllowedValuesLocked((FillContext) session2.mContexts.get(i2), flags);
                            }
                            ArrayList<FillContext> contexts = Session.this.mergePreviousSessionLocked(false);
                            Session session3 = Session.this;
                            session3.mDelayedFillPendingIntent = session3.createPendingIntent(requestId);
                            FillRequest request = new FillRequest(requestId, contexts, Session.this.mClientState, flags, null, Session.this.mDelayedFillPendingIntent == null ? null : Session.this.mDelayedFillPendingIntent.getIntentSender());
                            this.mPendingFillRequest = request;
                            maybeRequestFillLocked();
                        } catch (RuntimeException e) {
                            Session.this.wtf(e, "Exception lazy loading assist structure for %s: %s", structure.getActivityComponent(), e);
                            return;
                        }
                    } catch (Throwable th2) {
                        e = th2;
                        obj = obj2;
                    }
                } catch (Throwable th3) {
                    e = th3;
                }
            }
            if (Session.this.mActivityToken != null) {
                Session.this.mService.sendActivityAssistDataToContentCapture(Session.this.mActivityToken, resultData);
            }
        }

        public void onHandleAssistScreenshot(Bitmap screenshot) {
        }

        void processDelayedFillLocked(int requestId, FillResponse response) {
            FillRequest fillRequest = this.mLastFillRequest;
            if (fillRequest != null && requestId == fillRequest.getId()) {
                Slog.v(Session.TAG, "processDelayedFillLocked: calling onFillRequestSuccess with new response");
                Session session = Session.this;
                session.onFillRequestSuccess(requestId, response, session.mService.getServicePackageName(), this.mLastFillRequest.getFlags());
            }
        }
    }

    public PendingIntent createPendingIntent(int requestId) {
        Slog.d(TAG, "createPendingIntent for request " + requestId);
        long identity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent(ACTION_DELAYED_FILL).setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME).putExtra(EXTRA_REQUEST_ID, requestId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, this.id, intent, 1375731712);
            return pendingIntent;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void clearPendingIntentLocked() {
        Slog.d(TAG, "clearPendingIntentLocked");
        if (this.mDelayedFillPendingIntent == null) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mDelayedFillPendingIntent.cancel();
            this.mDelayedFillPendingIntent = null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void registerDelayedFillBroadcastLocked() {
        if (!this.mDelayedFillBroadcastReceiverRegistered) {
            Slog.v(TAG, "registerDelayedFillBroadcastLocked()");
            IntentFilter intentFilter = new IntentFilter(ACTION_DELAYED_FILL);
            this.mContext.registerReceiver(this.mDelayedFillBroadcastReceiver, intentFilter);
            this.mDelayedFillBroadcastReceiverRegistered = true;
        }
    }

    private void unregisterDelayedFillBroadcastLocked() {
        if (this.mDelayedFillBroadcastReceiverRegistered) {
            Slog.v(TAG, "unregisterDelayedFillBroadcastLocked()");
            this.mContext.unregisterReceiver(this.mDelayedFillBroadcastReceiver);
            this.mDelayedFillBroadcastReceiverRegistered = false;
        }
    }

    private AutofillId[] getIdsOfAllViewStatesLocked() {
        int numViewState = this.mViewStates.size();
        AutofillId[] ids = new AutofillId[numViewState];
        for (int i = 0; i < numViewState; i++) {
            ids[i] = this.mViewStates.valueAt(i).id;
        }
        return ids;
    }

    public String findByAutofillId(AutofillId id) {
        synchronized (this.mLock) {
            AutofillValue value = findValueLocked(id);
            if (value != null) {
                if (value.isText()) {
                    return value.getTextValue().toString();
                } else if (value.isList()) {
                    CharSequence[] options = getAutofillOptionsFromContextsLocked(id);
                    if (options != null) {
                        int index = value.getListValue();
                        CharSequence option = options[index];
                        return option != null ? option.toString() : null;
                    }
                    Slog.w(TAG, "findByAutofillId(): no autofill options for id " + id);
                }
            }
            return null;
        }
    }

    public AutofillValue findRawValueByAutofillId(AutofillId id) {
        AutofillValue findValueLocked;
        synchronized (this.mLock) {
            findValueLocked = findValueLocked(id);
        }
        return findValueLocked;
    }

    private AutofillValue findValueLocked(AutofillId autofillId) {
        AutofillValue value = findValueFromThisSessionOnlyLocked(autofillId);
        if (value != null) {
            return getSanitizedValue(Helper.createSanitizers(getSaveInfoLocked()), autofillId, value);
        }
        ArrayList<Session> previousSessions = this.mService.getPreviousSessionsLocked(this);
        if (previousSessions != null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "findValueLocked(): looking on " + previousSessions.size() + " previous sessions for autofillId " + autofillId);
            }
            for (int i = 0; i < previousSessions.size(); i++) {
                Session previousSession = previousSessions.get(i);
                AutofillValue previousValue = previousSession.findValueFromThisSessionOnlyLocked(autofillId);
                if (previousValue != null) {
                    return getSanitizedValue(Helper.createSanitizers(previousSession.getSaveInfoLocked()), autofillId, previousValue);
                }
            }
            return null;
        }
        return null;
    }

    private AutofillValue findValueFromThisSessionOnlyLocked(AutofillId autofillId) {
        ViewState state = this.mViewStates.get(autofillId);
        if (state == null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "findValueLocked(): no view state for " + autofillId);
                return null;
            }
            return null;
        }
        AutofillValue value = state.getCurrentValue();
        if (value == null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "findValueLocked(): no current value for " + autofillId);
            }
            return getValueFromContextsLocked(autofillId);
        }
        return value;
    }

    public void fillContextWithAllowedValuesLocked(FillContext fillContext, int flags) {
        AssistStructure.ViewNode[] nodes = fillContext.findViewNodesByAutofillIds(getIdsOfAllViewStatesLocked());
        int numViewState = this.mViewStates.size();
        for (int i = 0; i < numViewState; i++) {
            ViewState viewState = this.mViewStates.valueAt(i);
            AssistStructure.ViewNode node = nodes[i];
            if (node == null) {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "fillContextWithAllowedValuesLocked(): no node for " + viewState.id);
                }
            } else {
                AutofillValue currentValue = viewState.getCurrentValue();
                AutofillValue filledValue = viewState.getAutofilledValue();
                AssistStructure.AutofillOverlay overlay = new AssistStructure.AutofillOverlay();
                if (filledValue != null && filledValue.equals(currentValue)) {
                    overlay.value = currentValue;
                }
                AutofillId autofillId = this.mCurrentViewId;
                if (autofillId != null) {
                    overlay.focused = autofillId.equals(viewState.id);
                    if (overlay.focused && (flags & 1) != 0) {
                        overlay.value = currentValue;
                    }
                }
                node.setAutofillOverlay(overlay);
            }
        }
    }

    public void cancelCurrentRequestLocked() {
        ArrayList<FillContext> arrayList;
        RemoteFillService remoteFillService = this.mRemoteFillService;
        if (remoteFillService == null) {
            wtf(null, "cancelCurrentRequestLocked() called without a remote service. mForAugmentedAutofillOnly: %s", Boolean.valueOf(this.mSessionFlags.mAugmentedAutofillOnly));
            return;
        }
        int canceledRequest = remoteFillService.cancelCurrentRequest();
        if (canceledRequest != Integer.MIN_VALUE && (arrayList = this.mContexts) != null) {
            int numContexts = arrayList.size();
            for (int i = numContexts - 1; i >= 0; i--) {
                if (this.mContexts.get(i).getRequestId() == canceledRequest) {
                    if (Helper.sDebug) {
                        Slog.d(TAG, "cancelCurrentRequest(): id = " + canceledRequest);
                    }
                    this.mContexts.remove(i);
                    return;
                }
            }
        }
    }

    private boolean isViewFocusedLocked(int flags) {
        return (flags & 16) == 0;
    }

    private void requestNewFillResponseLocked(ViewState viewState, int newState, int flags) {
        final int requestId;
        FillResponse existingResponse = viewState.getResponse();
        if (existingResponse != null) {
            setViewStatesLocked(existingResponse, 1, true);
        }
        this.mSessionFlags.mExpiredResponse = false;
        this.mSessionState = 1;
        if (this.mSessionFlags.mAugmentedAutofillOnly || this.mRemoteFillService == null) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "requestNewFillResponse(): triggering augmented autofill instead (mForAugmentedAutofillOnly=" + this.mSessionFlags.mAugmentedAutofillOnly + ", flags=" + flags + ")");
            }
            this.mSessionFlags.mAugmentedAutofillOnly = true;
            triggerAugmentedAutofillLocked(flags);
            return;
        }
        viewState.setState(newState);
        do {
            requestId = sIdCounter.getAndIncrement();
        } while (requestId == Integer.MIN_VALUE);
        int ordinal = this.mRequestLogs.size() + 1;
        LogMaker log = newLogMaker(907).addTaggedData(1454, Integer.valueOf(ordinal));
        if (flags != 0) {
            log.addTaggedData(1452, Integer.valueOf(flags));
        }
        this.mRequestLogs.put(requestId, log);
        if (Helper.sVerbose) {
            Slog.v(TAG, "Requesting structure for request #" + ordinal + " ,requestId=" + requestId + ", flags=" + flags);
        }
        this.mPresentationStatsEventLogger.maybeSetRequestId(requestId);
        cancelCurrentRequestLocked();
        RemoteInlineSuggestionRenderService remoteRenderService = this.mService.getRemoteInlineSuggestionRenderServiceLocked();
        if (this.mSessionFlags.mInlineSupportedByService && remoteRenderService != null && (isViewFocusedLocked(flags) || isRequestSupportFillDialog(flags))) {
            final Consumer<InlineSuggestionsRequest> inlineSuggestionsRequestConsumer = this.mAssistReceiver.newAutofillRequestLocked(viewState, true);
            if (inlineSuggestionsRequestConsumer != null) {
                final AutofillId focusedId = this.mCurrentViewId;
                remoteRenderService.getInlineSuggestionsRendererInfo(new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda4
                    public final void onResult(Bundle bundle) {
                        Session.this.m2056x21a6431f(focusedId, inlineSuggestionsRequestConsumer, requestId, bundle);
                    }
                }, this.mHandler));
                viewState.setState(65536);
            }
        } else {
            this.mAssistReceiver.newAutofillRequestLocked(viewState, false);
        }
        requestAssistStructureLocked(requestId, flags);
    }

    /* renamed from: lambda$requestNewFillResponseLocked$0$com-android-server-autofill-Session */
    public /* synthetic */ void m2056x21a6431f(AutofillId focusedId, Consumer inlineSuggestionsRequestConsumer, int requestIdCopy, Bundle extras) {
        synchronized (this.mLock) {
            this.mInlineSessionController.onCreateInlineSuggestionsRequestLocked(focusedId, inlineSuggestionsRequestCacheDecorator(inlineSuggestionsRequestConsumer, requestIdCopy), extras);
        }
    }

    private boolean isRequestSupportFillDialog(int flags) {
        return (flags & 64) != 0;
    }

    private void requestAssistStructureLocked(int requestId, int flags) {
        try {
            Bundle receiverExtras = new Bundle();
            receiverExtras.putInt(EXTRA_REQUEST_ID, requestId);
            long identity = Binder.clearCallingIdentity();
            if (!ActivityTaskManager.getService().requestAutofillData(this.mAssistReceiver, receiverExtras, this.mActivityToken, flags)) {
                Slog.w(TAG, "failed to request autofill data for " + this.mActivityToken);
            }
            Binder.restoreCallingIdentity(identity);
        } catch (RemoteException e) {
        }
    }

    public Session(AutofillManagerServiceImpl service, AutoFillUI ui, Context context, Handler handler, int userId, Object lock, int sessionId, int taskId, int uid, IBinder activityToken, IBinder client, boolean hasCallback, LocalLog uiLatencyHistory, LocalLog wtfHistory, ComponentName serviceComponentName, ComponentName componentName, boolean compatMode, boolean bindInstantServiceAllowed, boolean forAugmentedAutofillOnly, int flags, InputMethodManagerInternal inputMethodManagerInternal) {
        MetricsLogger metricsLogger = new MetricsLogger();
        this.mMetricsLogger = metricsLogger;
        this.mSessionState = 0;
        this.mViewStates = new ArrayMap<>();
        this.mRequestLogs = new SparseArray<>(1);
        this.mAssistReceiver = new AssistDataReceiverImpl();
        this.mDelayedFillBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.autofill.Session.1
            {
                Session.this = this;
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (!intent.getAction().equals(Session.ACTION_DELAYED_FILL)) {
                    Slog.wtf(Session.TAG, "Unexpected action is received.");
                } else if (!intent.hasExtra(Session.EXTRA_REQUEST_ID)) {
                    Slog.e(Session.TAG, "Delay fill action is missing request id extra.");
                } else {
                    Slog.v(Session.TAG, "mDelayedFillBroadcastReceiver delayed fill action received");
                    synchronized (Session.this.mLock) {
                        int requestId = intent.getIntExtra(Session.EXTRA_REQUEST_ID, 0);
                        FillResponse response = (FillResponse) intent.getParcelableExtra("android.service.autofill.extra.FILL_RESPONSE");
                        Session.this.mAssistReceiver.processDelayedFillLocked(requestId, response);
                    }
                }
            }
        };
        if (sessionId < 0) {
            wtf(null, "Non-positive sessionId: %s", Integer.valueOf(sessionId));
        }
        this.id = sessionId;
        this.mFlags = flags;
        this.userId = userId;
        this.taskId = taskId;
        this.uid = uid;
        this.mStartTime = SystemClock.elapsedRealtime();
        this.mService = service;
        this.mLock = lock;
        this.mUi = ui;
        this.mHandler = handler;
        this.mRemoteFillService = serviceComponentName == null ? null : new RemoteFillService(context, serviceComponentName, userId, this, bindInstantServiceAllowed);
        this.mActivityToken = activityToken;
        this.mHasCallback = hasCallback;
        this.mUiLatencyHistory = uiLatencyHistory;
        this.mWtfHistory = wtfHistory;
        this.mContext = context;
        this.mComponentName = componentName;
        this.mCompatMode = compatMode;
        this.mSessionState = 1;
        this.mPresentationStatsEventLogger = PresentationStatsEventLogger.forSessionId(sessionId);
        synchronized (lock) {
            SessionFlags sessionFlags = new SessionFlags();
            this.mSessionFlags = sessionFlags;
            sessionFlags.mAugmentedAutofillOnly = forAugmentedAutofillOnly;
            sessionFlags.mInlineSupportedByService = service.isInlineSuggestionsEnabledLocked();
            setClientLocked(client);
        }
        this.mInlineSessionController = new AutofillInlineSessionController(inputMethodManagerInternal, userId, componentName, handler, lock, new InlineFillUi.InlineUiEventCallback() { // from class: com.android.server.autofill.Session.2
            {
                Session.this = this;
            }

            @Override // com.android.server.autofill.ui.InlineFillUi.InlineUiEventCallback
            public void notifyInlineUiShown(AutofillId autofillId) {
                Session.this.notifyFillUiShown(autofillId);
            }

            @Override // com.android.server.autofill.ui.InlineFillUi.InlineUiEventCallback
            public void notifyInlineUiHidden(AutofillId autofillId) {
                Session.this.notifyFillUiHidden(autofillId);
            }
        });
        metricsLogger.write(newLogMaker(906).addTaggedData(1452, Integer.valueOf(flags)));
    }

    public IBinder getActivityTokenLocked() {
        return this.mActivityToken;
    }

    public void switchActivity(IBinder newActivity, IBinder newClient) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#switchActivity() rejected - session: " + this.id + " destroyed");
                return;
            }
            this.mActivityToken = newActivity;
            setClientLocked(newClient);
            updateTrackedIdsLocked();
        }
    }

    private void setClientLocked(IBinder client) {
        unlinkClientVultureLocked();
        this.mClient = IAutoFillManagerClient.Stub.asInterface(client);
        this.mClientVulture = new IBinder.DeathRecipient() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda6
            @Override // android.os.IBinder.DeathRecipient
            public final void binderDied() {
                Session.this.m2057lambda$setClientLocked$1$comandroidserverautofillSession();
            }
        };
        try {
            this.mClient.asBinder().linkToDeath(this.mClientVulture, 0);
        } catch (RemoteException e) {
            Slog.w(TAG, "could not set binder death listener on autofill client: " + e);
            this.mClientVulture = null;
        }
    }

    /* renamed from: lambda$setClientLocked$1$com-android-server-autofill-Session */
    public /* synthetic */ void m2057lambda$setClientLocked$1$comandroidserverautofillSession() {
        synchronized (this.mLock) {
            Slog.d(TAG, "handling death of " + this.mActivityToken + " when saving=" + this.mSessionFlags.mShowingSaveUi);
            if (this.mSessionFlags.mShowingSaveUi) {
                this.mUi.hideFillUi(this);
            } else {
                this.mUi.destroyAll(this.mPendingSaveUi, this, false);
            }
        }
    }

    private void unlinkClientVultureLocked() {
        IAutoFillManagerClient iAutoFillManagerClient = this.mClient;
        if (iAutoFillManagerClient != null && this.mClientVulture != null) {
            boolean unlinked = iAutoFillManagerClient.asBinder().unlinkToDeath(this.mClientVulture, 0);
            if (!unlinked) {
                Slog.w(TAG, "unlinking vulture from death failed for " + this.mActivityToken);
            }
            this.mClientVulture = null;
        }
    }

    @Override // com.android.server.autofill.RemoteFillService.FillServiceCallbacks
    public void onFillRequestSuccess(int requestId, FillResponse response, String servicePackageName, int requestFlags) {
        int flags;
        long disableDuration;
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#onFillRequestSuccess() rejected - session: " + this.id + " destroyed");
                return;
            }
            LogMaker requestLog = this.mRequestLogs.get(requestId);
            if (requestLog != null) {
                requestLog.setType(10);
            } else {
                Slog.w(TAG, "onFillRequestSuccess(): no request log for id " + requestId);
            }
            if (response == null) {
                if (requestLog != null) {
                    requestLog.addTaggedData(909, -1);
                }
                processNullResponseLocked(requestId, requestFlags);
                return;
            }
            AutofillId[] fieldClassificationIds = response.getFieldClassificationIds();
            if (fieldClassificationIds != null && !this.mService.isFieldClassificationEnabledLocked()) {
                Slog.w(TAG, "Ignoring " + response + " because field detection is disabled");
                processNullResponseLocked(requestId, requestFlags);
                return;
            }
            if ((response.getFlags() & 4) != 0) {
                Slog.v(TAG, "Service requested to wait for delayed fill response.");
                registerDelayedFillBroadcastLocked();
            }
            this.mService.setLastResponse(this.id, response);
            long disableDuration2 = response.getDisableDuration();
            boolean autofillDisabled = disableDuration2 > 0;
            if (autofillDisabled) {
                int flags2 = response.getFlags();
                boolean disableActivityOnly = (flags2 & 2) != 0;
                notifyDisableAutofillToClient(disableDuration2, disableActivityOnly ? this.mComponentName : null);
                if (disableActivityOnly) {
                    flags = flags2;
                    disableDuration = disableDuration2;
                    this.mService.disableAutofillForActivity(this.mComponentName, disableDuration2, this.id, this.mCompatMode);
                } else {
                    flags = flags2;
                    disableDuration = disableDuration2;
                    this.mService.disableAutofillForApp(this.mComponentName.getPackageName(), disableDuration, this.id, this.mCompatMode);
                }
                synchronized (this.mLock) {
                    try {
                        this.mSessionFlags.mAutofillDisabled = true;
                        if (triggerAugmentedAutofillLocked(requestFlags) != null) {
                            try {
                                this.mSessionFlags.mAugmentedAutofillOnly = true;
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "Service disabled autofill for " + this.mComponentName + ", but session is kept for augmented autofill only");
                                }
                                return;
                            } catch (Throwable th) {
                                th = th;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                }
                                throw th;
                            }
                        } else if (Helper.sDebug) {
                            StringBuilder message = new StringBuilder("Service disabled autofill for ").append(this.mComponentName).append(": flags=").append(flags).append(", duration=");
                            TimeUtils.formatDuration(disableDuration, message);
                            Slog.d(TAG, message.toString());
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
            }
            if (((response.getDatasets() == null || response.getDatasets().isEmpty()) && response.getAuthentication() == null) || autofillDisabled) {
                notifyUnavailableToClient(autofillDisabled ? 4 : 0, null);
                synchronized (this.mLock) {
                    this.mInlineSessionController.setInlineFillUiLocked(InlineFillUi.emptyUi(this.mCurrentViewId));
                }
            }
            if (requestLog != null) {
                requestLog.addTaggedData(909, Integer.valueOf(response.getDatasets() != null ? response.getDatasets().size() : 0));
                if (fieldClassificationIds != null) {
                    requestLog.addTaggedData(1271, Integer.valueOf(fieldClassificationIds.length));
                }
            }
            synchronized (this.mLock) {
                processResponseLocked(response, null, requestFlags);
            }
        }
    }

    @Override // com.android.server.autofill.RemoteFillService.FillServiceCallbacks
    public void onFillRequestFailure(int requestId, CharSequence message) {
        onFillRequestFailureOrTimeout(requestId, false, message);
    }

    @Override // com.android.server.autofill.RemoteFillService.FillServiceCallbacks
    public void onFillRequestTimeout(int requestId) {
        onFillRequestFailureOrTimeout(requestId, true, null);
    }

    private void onFillRequestFailureOrTimeout(int requestId, boolean timedOut, CharSequence message) {
        boolean showMessage = !TextUtils.isEmpty(message);
        synchronized (this.mLock) {
            unregisterDelayedFillBroadcastLocked();
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#onFillRequestFailureOrTimeout(req=" + requestId + ") rejected - session: " + this.id + " destroyed");
                return;
            }
            if (Helper.sDebug) {
                Slog.d(TAG, "finishing session due to service " + (timedOut ? "timeout" : "failure"));
            }
            this.mService.resetLastResponse();
            LogMaker requestLog = this.mRequestLogs.get(requestId);
            if (requestLog == null) {
                Slog.w(TAG, "onFillRequestFailureOrTimeout(): no log for id " + requestId);
            } else {
                requestLog.setType(timedOut ? 2 : 11);
            }
            if (showMessage) {
                int targetSdk = this.mService.getTargedSdkLocked();
                if (targetSdk >= 29) {
                    showMessage = false;
                    Slog.w(TAG, "onFillRequestFailureOrTimeout(): not showing '" + ((Object) message) + "' because service's targetting API " + targetSdk);
                }
                if (message != null) {
                    requestLog.addTaggedData(1572, Integer.valueOf(message.length()));
                }
            }
            this.mPresentationStatsEventLogger.maybeSetNoPresentationEventReason(5);
            this.mPresentationStatsEventLogger.logAndEndEvent();
            notifyUnavailableToClient(6, null);
            if (showMessage) {
                getUiForShowing().showError(message, this);
            }
            removeFromService();
        }
    }

    @Override // com.android.server.autofill.RemoteFillService.FillServiceCallbacks
    public void onSaveRequestSuccess(String servicePackageName, IntentSender intentSender) {
        synchronized (this.mLock) {
            this.mSessionFlags.mShowingSaveUi = false;
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#onSaveRequestSuccess() rejected - session: " + this.id + " destroyed");
                return;
            }
            LogMaker log = newLogMaker(918, servicePackageName).setType(intentSender == null ? 10 : 1);
            this.mMetricsLogger.write(log);
            if (intentSender != null) {
                if (Helper.sDebug) {
                    Slog.d(TAG, "Starting intent sender on save()");
                }
                startIntentSenderAndFinishSession(intentSender);
            }
            removeFromService();
        }
    }

    @Override // com.android.server.autofill.RemoteFillService.FillServiceCallbacks
    public void onSaveRequestFailure(CharSequence message, String servicePackageName) {
        int targetSdk;
        boolean showMessage = !TextUtils.isEmpty(message);
        synchronized (this.mLock) {
            this.mSessionFlags.mShowingSaveUi = false;
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#onSaveRequestFailure() rejected - session: " + this.id + " destroyed");
                return;
            }
            if (showMessage && (targetSdk = this.mService.getTargedSdkLocked()) >= 29) {
                showMessage = false;
                Slog.w(TAG, "onSaveRequestFailure(): not showing '" + ((Object) message) + "' because service's targetting API " + targetSdk);
            }
            LogMaker log = newLogMaker(918, servicePackageName).setType(11);
            if (message != null) {
                log.addTaggedData(1572, Integer.valueOf(message.length()));
            }
            this.mMetricsLogger.write(log);
            if (showMessage) {
                getUiForShowing().showError(message, this);
            }
            removeFromService();
        }
    }

    private FillContext getFillContextByRequestIdLocked(int requestId) {
        ArrayList<FillContext> arrayList = this.mContexts;
        if (arrayList == null) {
            return null;
        }
        int numContexts = arrayList.size();
        for (int i = 0; i < numContexts; i++) {
            FillContext context = this.mContexts.get(i);
            if (context.getRequestId() == requestId) {
                return context;
            }
        }
        return null;
    }

    public void onServiceDied(RemoteFillService service) {
        Slog.w(TAG, "removing session because service died");
        synchronized (this.mLock) {
            forceRemoveFromServiceLocked();
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void authenticate(int requestId, int datasetIndex, IntentSender intent, Bundle extras, boolean authenticateInline) {
        if (Helper.sDebug) {
            Slog.d(TAG, "authenticate(): requestId=" + requestId + "; datasetIdx=" + datasetIndex + "; intentSender=" + intent);
        }
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#authenticate() rejected - session: " + this.id + " destroyed");
                return;
            }
            Intent fillInIntent = createAuthFillInIntentLocked(requestId, extras);
            if (fillInIntent == null) {
                forceRemoveFromServiceLocked();
                return;
            }
            this.mService.setAuthenticationSelected(this.id, this.mClientState);
            int authenticationId = AutofillManager.makeAuthenticationId(requestId, datasetIndex);
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new QuintConsumer() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda1
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                    ((Session) obj).startAuthentication(((Integer) obj2).intValue(), (IntentSender) obj3, (Intent) obj4, ((Boolean) obj5).booleanValue());
                }
            }, this, Integer.valueOf(authenticationId), intent, fillInIntent, Boolean.valueOf(authenticateInline)));
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void fill(int requestId, int datasetIndex, Dataset dataset, int uiType) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#fill() rejected - session: " + this.id + " destroyed");
            } else {
                this.mHandler.sendMessage(PooledLambda.obtainMessage(new HexConsumer() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda7
                    public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
                        ((Session) obj).autoFill(((Integer) obj2).intValue(), ((Integer) obj3).intValue(), (Dataset) obj4, ((Boolean) obj5).booleanValue(), ((Integer) obj6).intValue());
                    }
                }, this, Integer.valueOf(requestId), Integer.valueOf(datasetIndex), dataset, true, Integer.valueOf(uiType)));
            }
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void save() {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#save() rejected - session: " + this.id + " destroyed");
            } else {
                this.mHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda10
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        ((AutofillManagerServiceImpl) obj).handleSessionSave((Session) obj2);
                    }
                }, this.mService, this));
            }
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void cancelSave() {
        synchronized (this.mLock) {
            this.mSessionFlags.mShowingSaveUi = false;
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#cancelSave() rejected - session: " + this.id + " destroyed");
            } else {
                this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda2
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((Session) obj).removeFromService();
                    }
                }, this));
            }
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void requestShowFillUi(AutofillId id, int width, int height, IAutofillWindowPresenter presenter) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#requestShowFillUi() rejected - session: " + id + " destroyed");
                return;
            }
            if (id.equals(this.mCurrentViewId)) {
                try {
                    ViewState view = this.mViewStates.get(id);
                    this.mClient.requestShowFillUi(this.id, id, width, height, view.getVirtualBounds(), presenter);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error requesting to show fill UI", e);
                }
            } else if (Helper.sDebug) {
                Slog.d(TAG, "Do not show full UI on " + id + " as it is not the current view (" + this.mCurrentViewId + ") anymore");
            }
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void dispatchUnhandledKey(AutofillId id, KeyEvent keyEvent) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#dispatchUnhandledKey() rejected - session: " + id + " destroyed");
                return;
            }
            if (id.equals(this.mCurrentViewId)) {
                try {
                    this.mClient.dispatchUnhandledKey(this.id, id, keyEvent);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error requesting to dispatch unhandled key", e);
                }
            } else {
                Slog.w(TAG, "Do not dispatch unhandled key on " + id + " as it is not the current view (" + this.mCurrentViewId + ") anymore");
            }
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void requestHideFillUi(AutofillId id) {
        synchronized (this.mLock) {
            try {
                this.mClient.requestHideFillUi(this.id, id);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error requesting to hide fill UI", e);
            }
            this.mInlineSessionController.hideInlineSuggestionsUiLocked(id);
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void cancelSession() {
        synchronized (this.mLock) {
            removeFromServiceLocked();
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void startIntentSenderAndFinishSession(IntentSender intentSender) {
        startIntentSender(intentSender, null);
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void startIntentSender(IntentSender intentSender, Intent intent) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#startIntentSender() rejected - session: " + this.id + " destroyed");
                return;
            }
            if (intent == null) {
                removeFromServiceLocked();
            }
            this.mHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda9
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((Session) obj).doStartIntentSender((IntentSender) obj2, (Intent) obj3);
                }
            }, this, intentSender, intent));
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void requestShowSoftInput(AutofillId id) {
        IAutoFillManagerClient client = getClient();
        if (client != null) {
            try {
                client.requestShowSoftInput(id);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error sending input show up notification", e);
            }
        }
    }

    @Override // com.android.server.autofill.ui.AutoFillUI.AutoFillUiCallback
    public void requestFallbackFromFillDialog() {
        setFillDialogDisabled();
        synchronized (this.mLock) {
            AutofillId autofillId = this.mCurrentViewId;
            if (autofillId == null) {
                return;
            }
            ViewState currentView = this.mViewStates.get(autofillId);
            currentView.maybeCallOnFillReady(this.mFlags);
        }
    }

    public void notifyFillUiHidden(AutofillId autofillId) {
        synchronized (this.mLock) {
            try {
                this.mClient.notifyFillUiHidden(this.id, autofillId);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error sending fill UI hidden notification", e);
            }
        }
    }

    public void notifyFillUiShown(AutofillId autofillId) {
        synchronized (this.mLock) {
            try {
                this.mClient.notifyFillUiShown(this.id, autofillId);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error sending fill UI shown notification", e);
            }
        }
    }

    public void doStartIntentSender(IntentSender intentSender, Intent intent) {
        try {
            synchronized (this.mLock) {
                this.mClient.startIntentSender(intentSender, intent);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Error launching auth intent", e);
        }
    }

    public void setAuthenticationResultLocked(Bundle data, int authenticationId) {
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#setAuthenticationResultLocked() rejected - session: " + this.id + " destroyed");
            return;
        }
        int requestId = AutofillManager.getRequestIdFromAuthenticationId(authenticationId);
        if (requestId == 1) {
            setAuthenticationResultForAugmentedAutofillLocked(data, authenticationId);
            return;
        }
        SparseArray<FillResponse> sparseArray = this.mResponses;
        if (sparseArray == null) {
            Slog.w(TAG, "setAuthenticationResultLocked(" + authenticationId + "): no responses");
            removeFromService();
            return;
        }
        FillResponse authenticatedResponse = sparseArray.get(requestId);
        if (authenticatedResponse == null || data == null) {
            Slog.w(TAG, "no authenticated response");
            removeFromService();
            return;
        }
        int datasetIdx = AutofillManager.getDatasetIdFromAuthenticationId(authenticationId);
        if (datasetIdx != 65535 && ((Dataset) authenticatedResponse.getDatasets().get(datasetIdx)) == null) {
            Slog.w(TAG, "no dataset with index " + datasetIdx + " on fill response");
            removeFromService();
            return;
        }
        this.mSessionFlags.mExpiredResponse = false;
        Parcelable result = data.getParcelable("android.view.autofill.extra.AUTHENTICATION_RESULT");
        Bundle newClientState = data.getBundle("android.view.autofill.extra.CLIENT_STATE");
        if (Helper.sDebug) {
            Slog.d(TAG, "setAuthenticationResultLocked(): result=" + result + ", clientState=" + newClientState + ", authenticationId=" + authenticationId);
        }
        if (result instanceof FillResponse) {
            logAuthenticationStatusLocked(requestId, 912);
            replaceResponseLocked(authenticatedResponse, (FillResponse) result, newClientState);
        } else if (result instanceof Dataset) {
            if (datasetIdx != 65535) {
                logAuthenticationStatusLocked(requestId, 1126);
                if (newClientState != null) {
                    if (Helper.sDebug) {
                        Slog.d(TAG, "Updating client state from auth dataset");
                    }
                    this.mClientState = newClientState;
                }
                Dataset dataset = (Dataset) result;
                Dataset oldDataset = (Dataset) authenticatedResponse.getDatasets().get(datasetIdx);
                if (!isAuthResultDatasetEphemeral(oldDataset, data)) {
                    authenticatedResponse.getDatasets().set(datasetIdx, dataset);
                }
                autoFill(requestId, datasetIdx, dataset, false, 0);
                return;
            }
            Slog.w(TAG, "invalid index (" + datasetIdx + ") for authentication id " + authenticationId);
            logAuthenticationStatusLocked(requestId, 1127);
        } else {
            if (result != null) {
                Slog.w(TAG, "service returned invalid auth type: " + result);
            }
            logAuthenticationStatusLocked(requestId, 1128);
            processNullResponseLocked(requestId, 0);
        }
    }

    private static boolean isAuthResultDatasetEphemeral(Dataset oldDataset, Bundle authResultData) {
        if (authResultData.containsKey("android.view.autofill.extra.AUTHENTICATION_RESULT_EPHEMERAL_DATASET")) {
            return authResultData.getBoolean("android.view.autofill.extra.AUTHENTICATION_RESULT_EPHEMERAL_DATASET");
        }
        return isPinnedDataset(oldDataset);
    }

    private static boolean isPinnedDataset(Dataset dataset) {
        if (dataset != null && dataset.getFieldIds() != null) {
            int numOfFields = dataset.getFieldIds().size();
            for (int i = 0; i < numOfFields; i++) {
                InlinePresentation inlinePresentation = dataset.getFieldInlinePresentation(i);
                if (inlinePresentation != null && inlinePresentation.isPinned()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    void setAuthenticationResultForAugmentedAutofillLocked(Bundle data, int authId) {
        Dataset dataset = data == null ? null : (Dataset) data.getParcelable("android.view.autofill.extra.AUTHENTICATION_RESULT");
        if (Helper.sDebug) {
            Slog.d(TAG, "Auth result for augmented autofill: sessionId=" + this.id + ", authId=" + authId + ", dataset=" + dataset);
        }
        AutofillId fieldId = (dataset == null || dataset.getFieldIds().size() != 1) ? null : (AutofillId) dataset.getFieldIds().get(0);
        AutofillValue value = (dataset == null || dataset.getFieldValues().size() != 1) ? null : (AutofillValue) dataset.getFieldValues().get(0);
        ClipData content = dataset != null ? dataset.getFieldContent() : null;
        if (fieldId == null || (value == null && content == null)) {
            if (Helper.sDebug) {
                Slog.d(TAG, "Rejecting empty/invalid auth result");
            }
            this.mService.resetLastAugmentedAutofillResponse();
            removeFromServiceLocked();
            return;
        }
        RemoteAugmentedAutofillService remoteAugmentedAutofillService = this.mService.getRemoteAugmentedAutofillServiceIfCreatedLocked();
        if (remoteAugmentedAutofillService == null) {
            Slog.e(TAG, "Can't fill after auth: RemoteAugmentedAutofillService is null");
            this.mService.resetLastAugmentedAutofillResponse();
            removeFromServiceLocked();
            return;
        }
        fieldId.setSessionId(this.id);
        this.mCurrentViewId = fieldId;
        Bundle clientState = data.getBundle("android.view.autofill.extra.CLIENT_STATE");
        this.mService.logAugmentedAutofillSelected(this.id, dataset.getId(), clientState);
        if (content != null) {
            AutofillUriGrantsManager autofillUgm = remoteAugmentedAutofillService.getAutofillUriGrantsManager();
            autofillUgm.grantUriPermissions(this.mComponentName, this.mActivityToken, this.userId, content);
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "Filling after auth: fieldId=" + fieldId + ", value=" + value + ", content=" + content);
        }
        try {
            if (content != null) {
                this.mClient.autofillContent(this.id, fieldId, content);
            } else {
                this.mClient.autofill(this.id, dataset.getFieldIds(), dataset.getFieldValues(), true);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Error filling after auth: fieldId=" + fieldId + ", value=" + value + ", content=" + content, e);
        }
        this.mInlineSessionController.setInlineFillUiLocked(InlineFillUi.emptyUi(fieldId));
    }

    public void setHasCallbackLocked(boolean hasIt) {
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#setHasCallbackLocked() rejected - session: " + this.id + " destroyed");
        } else {
            this.mHasCallback = hasIt;
        }
    }

    private FillResponse getLastResponseLocked(String logPrefixFmt) {
        String logPrefix = (!Helper.sDebug || logPrefixFmt == null) ? null : String.format(logPrefixFmt, Integer.valueOf(this.id));
        if (this.mContexts == null) {
            if (logPrefix != null) {
                Slog.d(TAG, logPrefix + ": no contexts");
            }
            return null;
        } else if (this.mResponses == null) {
            if (Helper.sVerbose && logPrefix != null) {
                Slog.v(TAG, logPrefix + ": no responses on session");
            }
            return null;
        } else {
            int lastResponseIdx = getLastResponseIndexLocked();
            if (lastResponseIdx < 0) {
                if (logPrefix != null) {
                    Slog.w(TAG, logPrefix + ": did not get last response. mResponses=" + this.mResponses + ", mViewStates=" + this.mViewStates);
                }
                return null;
            }
            FillResponse response = this.mResponses.valueAt(lastResponseIdx);
            if (Helper.sVerbose && logPrefix != null) {
                Slog.v(TAG, logPrefix + ": mResponses=" + this.mResponses + ", mContexts=" + this.mContexts + ", mViewStates=" + this.mViewStates);
            }
            return response;
        }
    }

    private SaveInfo getSaveInfoLocked() {
        FillResponse response = getLastResponseLocked(null);
        if (response == null) {
            return null;
        }
        return response.getSaveInfo();
    }

    public int getSaveInfoFlagsLocked() {
        SaveInfo saveInfo = getSaveInfoLocked();
        if (saveInfo == null) {
            return 0;
        }
        return saveInfo.getFlags();
    }

    public void logContextCommitted() {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new Session$$ExternalSyntheticLambda15(), this, 0, 0));
    }

    public void logContextCommitted(int saveDialogNotShowReason, int commitReason) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new Session$$ExternalSyntheticLambda15(), this, Integer.valueOf(saveDialogNotShowReason), Integer.valueOf(commitReason)));
    }

    public void handleLogContextCommitted(int saveDialogNotShowReason, int commitReason) {
        FillResponse lastResponse;
        FieldClassificationUserData userData;
        synchronized (this.mLock) {
            lastResponse = getLastResponseLocked("logContextCommited(%s)");
        }
        if (lastResponse == null) {
            Slog.w(TAG, "handleLogContextCommitted(): last response is null");
            return;
        }
        UserData genericUserData = this.mService.getUserData();
        FieldClassificationUserData userData2 = lastResponse.getUserData();
        if (userData2 == null && genericUserData == null) {
            userData = null;
        } else if (userData2 != null && genericUserData != null) {
            userData = new CompositeUserData(genericUserData, userData2);
        } else if (userData2 != null) {
            userData = userData2;
        } else {
            userData = this.mService.getUserData();
        }
        FieldClassificationStrategy fcStrategy = this.mService.getFieldClassificationStrategy();
        if (userData != null && fcStrategy != null) {
            logFieldClassificationScore(fcStrategy, userData, saveDialogNotShowReason, commitReason);
        } else {
            logContextCommitted(null, null, saveDialogNotShowReason, commitReason);
        }
    }

    private void logContextCommitted(ArrayList<AutofillId> detectedFieldIds, ArrayList<FieldClassification> detectedFieldClassifications, int saveDialogNotShowReason, int commitReason) {
        synchronized (this.mLock) {
            logContextCommittedLocked(detectedFieldIds, detectedFieldClassifications, saveDialogNotShowReason, commitReason);
        }
    }

    private void logContextCommittedLocked(ArrayList<AutofillId> detectedFieldIds, ArrayList<FieldClassification> detectedFieldClassifications, int saveDialogNotShowReason, int commitReason) {
        String str;
        boolean hasAtLeastOneDataset;
        int responseCount;
        boolean hasAtLeastOneDataset2;
        int responseCount2;
        AutofillValue currentValue;
        String str2;
        int responseCount3;
        String str3;
        AutofillValue currentValue2;
        ArraySet<String> ignoredDatasets;
        ArrayList<AutofillValue> values;
        AutofillValue currentValue3;
        ArrayMap<AutofillId, ArraySet<String>> manuallyFilledIds;
        FillResponse lastResponse;
        int flags;
        ArrayList<AutofillId> changedFieldIds;
        ArrayList<AutofillId> changedFieldIds2;
        FillResponse lastResponse2 = getLastResponseLocked("logContextCommited(%s)");
        if (lastResponse2 == null) {
            return;
        }
        this.mPresentationStatsEventLogger.maybeSetNoPresentationEventReason(PresentationStatsEventLogger.getNoPresentationEventReason(commitReason));
        this.mPresentationStatsEventLogger.logAndEndEvent();
        int flags2 = lastResponse2.getFlags();
        if ((flags2 & 1) == 0) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "logContextCommittedLocked(): ignored by flags " + flags2);
                return;
            }
            return;
        }
        ArraySet<String> ignoredDatasets2 = null;
        ArrayList<AutofillId> changedFieldIds3 = null;
        ArrayList<String> changedDatasetIds = null;
        ArrayMap<AutofillId, ArraySet<String>> manuallyFilledIds2 = null;
        boolean hasAtLeastOneDataset3 = false;
        int responseCount4 = this.mResponses.size();
        int i = 0;
        while (true) {
            str = "logContextCommitted() skipping idless dataset ";
            if (i >= responseCount4) {
                break;
            }
            FillResponse response = this.mResponses.valueAt(i);
            List<Dataset> datasets = response.getDatasets();
            if (datasets == null) {
                lastResponse = lastResponse2;
                flags = flags2;
                changedFieldIds = changedFieldIds3;
            } else if (datasets.isEmpty()) {
                lastResponse = lastResponse2;
                flags = flags2;
                changedFieldIds = changedFieldIds3;
            } else {
                int j = 0;
                while (true) {
                    lastResponse = lastResponse2;
                    if (j >= datasets.size()) {
                        break;
                    }
                    Dataset dataset = datasets.get(j);
                    int flags3 = flags2;
                    String datasetId = dataset.getId();
                    if (datasetId == null) {
                        if (!Helper.sVerbose) {
                            changedFieldIds2 = changedFieldIds3;
                        } else {
                            changedFieldIds2 = changedFieldIds3;
                            Slog.v(TAG, "logContextCommitted() skipping idless dataset " + dataset);
                        }
                    } else {
                        changedFieldIds2 = changedFieldIds3;
                        ArrayList<String> arrayList = this.mSelectedDatasetIds;
                        if (arrayList == null || !arrayList.contains(datasetId)) {
                            if (Helper.sVerbose) {
                                Slog.v(TAG, "adding ignored dataset " + datasetId);
                            }
                            if (ignoredDatasets2 == null) {
                                ignoredDatasets2 = new ArraySet<>();
                            }
                            ignoredDatasets2.add(datasetId);
                            hasAtLeastOneDataset3 = true;
                        } else {
                            hasAtLeastOneDataset3 = true;
                        }
                    }
                    j++;
                    lastResponse2 = lastResponse;
                    flags2 = flags3;
                    changedFieldIds3 = changedFieldIds2;
                }
                flags = flags2;
                changedFieldIds = changedFieldIds3;
                i++;
                lastResponse2 = lastResponse;
                flags2 = flags;
                changedFieldIds3 = changedFieldIds;
            }
            if (Helper.sVerbose) {
                Slog.v(TAG, "logContextCommitted() no datasets at " + i);
            }
            i++;
            lastResponse2 = lastResponse;
            flags2 = flags;
            changedFieldIds3 = changedFieldIds;
        }
        int i2 = 0;
        while (i2 < this.mViewStates.size()) {
            ViewState viewState = this.mViewStates.valueAt(i2);
            int state = viewState.getState();
            if ((state & 8) == 0) {
                hasAtLeastOneDataset = hasAtLeastOneDataset3;
                responseCount = responseCount4;
            } else if ((state & 2048) != 0) {
                String datasetId2 = viewState.getDatasetId();
                if (datasetId2 == null) {
                    Slog.w(TAG, "logContextCommitted(): no dataset id on " + viewState);
                    hasAtLeastOneDataset = hasAtLeastOneDataset3;
                    responseCount = responseCount4;
                } else {
                    AutofillValue autofilledValue = viewState.getAutofilledValue();
                    AutofillValue currentValue4 = viewState.getCurrentValue();
                    if (autofilledValue != null && autofilledValue.equals(currentValue4)) {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "logContextCommitted(): ignoring changed " + viewState + " because it has same value that was autofilled");
                            hasAtLeastOneDataset = hasAtLeastOneDataset3;
                            responseCount = responseCount4;
                        } else {
                            hasAtLeastOneDataset = hasAtLeastOneDataset3;
                            responseCount = responseCount4;
                        }
                    } else {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "logContextCommitted() found changed state: " + viewState);
                        }
                        if (changedFieldIds3 == null) {
                            changedFieldIds3 = new ArrayList<>();
                            changedDatasetIds = new ArrayList<>();
                        }
                        changedFieldIds3.add(viewState.id);
                        changedDatasetIds.add(datasetId2);
                        hasAtLeastOneDataset = hasAtLeastOneDataset3;
                        responseCount = responseCount4;
                    }
                }
            } else {
                AutofillValue currentValue5 = viewState.getCurrentValue();
                if (currentValue5 == null) {
                    if (!Helper.sDebug) {
                        hasAtLeastOneDataset = hasAtLeastOneDataset3;
                        responseCount = responseCount4;
                    } else {
                        Slog.d(TAG, "logContextCommitted(): skipping view without current value ( " + viewState + ")");
                        hasAtLeastOneDataset = hasAtLeastOneDataset3;
                        responseCount = responseCount4;
                    }
                } else if (!hasAtLeastOneDataset3) {
                    hasAtLeastOneDataset = hasAtLeastOneDataset3;
                    responseCount = responseCount4;
                } else {
                    int j2 = 0;
                    while (j2 < responseCount4) {
                        FillResponse response2 = this.mResponses.valueAt(j2);
                        ArraySet<String> ignoredDatasets3 = ignoredDatasets2;
                        List<Dataset> datasets2 = response2.getDatasets();
                        if (datasets2 == null) {
                            hasAtLeastOneDataset2 = hasAtLeastOneDataset3;
                            responseCount2 = responseCount4;
                            currentValue = currentValue5;
                            str2 = str;
                        } else if (datasets2.isEmpty()) {
                            hasAtLeastOneDataset2 = hasAtLeastOneDataset3;
                            responseCount2 = responseCount4;
                            currentValue = currentValue5;
                            str2 = str;
                        } else {
                            ArrayMap<AutofillId, ArraySet<String>> manuallyFilledIds3 = manuallyFilledIds2;
                            int k = 0;
                            while (true) {
                                hasAtLeastOneDataset2 = hasAtLeastOneDataset3;
                                if (k >= datasets2.size()) {
                                    break;
                                }
                                Dataset dataset2 = datasets2.get(k);
                                List<Dataset> datasets3 = datasets2;
                                String datasetId3 = dataset2.getId();
                                if (datasetId3 == null) {
                                    if (!Helper.sVerbose) {
                                        responseCount3 = responseCount4;
                                    } else {
                                        responseCount3 = responseCount4;
                                        Slog.v(TAG, str + dataset2);
                                    }
                                    currentValue2 = currentValue5;
                                    str3 = str;
                                } else {
                                    responseCount3 = responseCount4;
                                    ArrayList<AutofillValue> values2 = dataset2.getFieldValues();
                                    int l = 0;
                                    while (true) {
                                        str3 = str;
                                        if (l >= values2.size()) {
                                            break;
                                        }
                                        AutofillValue candidate = values2.get(l);
                                        if (!currentValue5.equals(candidate)) {
                                            values = values2;
                                            currentValue3 = currentValue5;
                                        } else {
                                            if (!Helper.sDebug) {
                                                values = values2;
                                                currentValue3 = currentValue5;
                                            } else {
                                                values = values2;
                                                currentValue3 = currentValue5;
                                                Slog.d(TAG, "field " + viewState.id + " was manually filled with value set by dataset " + datasetId3);
                                            }
                                            if (manuallyFilledIds3 != null) {
                                                manuallyFilledIds = manuallyFilledIds3;
                                            } else {
                                                manuallyFilledIds = new ArrayMap<>();
                                            }
                                            ArraySet<String> datasetIds = manuallyFilledIds.get(viewState.id);
                                            if (datasetIds == null) {
                                                datasetIds = new ArraySet<>(1);
                                                manuallyFilledIds.put(viewState.id, datasetIds);
                                            }
                                            datasetIds.add(datasetId3);
                                            manuallyFilledIds3 = manuallyFilledIds;
                                        }
                                        l++;
                                        str = str3;
                                        values2 = values;
                                        currentValue5 = currentValue3;
                                    }
                                    currentValue2 = currentValue5;
                                    ArrayList<String> arrayList2 = this.mSelectedDatasetIds;
                                    if (arrayList2 == null || !arrayList2.contains(datasetId3)) {
                                        if (Helper.sVerbose) {
                                            Slog.v(TAG, "adding ignored dataset " + datasetId3);
                                        }
                                        if (ignoredDatasets3 != null) {
                                            ignoredDatasets = ignoredDatasets3;
                                        } else {
                                            ignoredDatasets = new ArraySet<>();
                                        }
                                        ignoredDatasets.add(datasetId3);
                                        ignoredDatasets3 = ignoredDatasets;
                                    }
                                }
                                k++;
                                datasets2 = datasets3;
                                str = str3;
                                currentValue5 = currentValue2;
                                hasAtLeastOneDataset3 = hasAtLeastOneDataset2;
                                responseCount4 = responseCount3;
                            }
                            responseCount2 = responseCount4;
                            currentValue = currentValue5;
                            str2 = str;
                            ignoredDatasets2 = ignoredDatasets3;
                            manuallyFilledIds2 = manuallyFilledIds3;
                            j2++;
                            str = str2;
                            currentValue5 = currentValue;
                            hasAtLeastOneDataset3 = hasAtLeastOneDataset2;
                            responseCount4 = responseCount2;
                        }
                        if (Helper.sVerbose) {
                            Slog.v(TAG, "logContextCommitted() no datasets at " + j2);
                        }
                        ignoredDatasets2 = ignoredDatasets3;
                        j2++;
                        str = str2;
                        currentValue5 = currentValue;
                        hasAtLeastOneDataset3 = hasAtLeastOneDataset2;
                        responseCount4 = responseCount2;
                    }
                    hasAtLeastOneDataset = hasAtLeastOneDataset3;
                    responseCount = responseCount4;
                }
            }
            i2++;
            str = str;
            hasAtLeastOneDataset3 = hasAtLeastOneDataset;
            responseCount4 = responseCount;
        }
        ArrayList<AutofillId> manuallyFilledFieldIds = null;
        ArrayList<ArrayList<String>> manuallyFilledDatasetIds = null;
        if (manuallyFilledIds2 != null) {
            int size = manuallyFilledIds2.size();
            manuallyFilledFieldIds = new ArrayList<>(size);
            manuallyFilledDatasetIds = new ArrayList<>(size);
            for (int i3 = 0; i3 < size; i3++) {
                AutofillId fieldId = manuallyFilledIds2.keyAt(i3);
                manuallyFilledFieldIds.add(fieldId);
                manuallyFilledDatasetIds.add(new ArrayList<>(manuallyFilledIds2.valueAt(i3)));
            }
        }
        this.mService.logContextCommittedLocked(this.id, this.mClientState, this.mSelectedDatasetIds, ignoredDatasets2, changedFieldIds3, changedDatasetIds, manuallyFilledFieldIds, manuallyFilledDatasetIds, detectedFieldIds, detectedFieldClassifications, this.mComponentName, this.mCompatMode, saveDialogNotShowReason);
    }

    private void logFieldClassificationScore(FieldClassificationStrategy fcStrategy, FieldClassificationUserData userData, final int saveDialogNotShowReason, final int commitReason) {
        String[] categoryIds;
        String[] userValues;
        Collection<ViewState> viewStates;
        final String[] userValues2 = userData.getValues();
        final String[] categoryIds2 = userData.getCategoryIds();
        String defaultAlgorithm = userData.getFieldClassificationAlgorithm();
        Bundle defaultArgs = userData.getDefaultFieldClassificationArgs();
        ArrayMap<String, String> algorithms = userData.getFieldClassificationAlgorithms();
        ArrayMap<String, Bundle> args = userData.getFieldClassificationArgs();
        if (userValues2 == null || categoryIds2 == null) {
            categoryIds = categoryIds2;
            userValues = userValues2;
        } else if (userValues2.length == categoryIds2.length) {
            int maxFieldsSize = UserData.getMaxFieldClassificationIdsSize();
            final ArrayList<AutofillId> detectedFieldIds = new ArrayList<>(maxFieldsSize);
            final ArrayList<FieldClassification> detectedFieldClassifications = new ArrayList<>(maxFieldsSize);
            synchronized (this.mLock) {
                try {
                    viewStates = this.mViewStates.values();
                } catch (Throwable th) {
                    th = th;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    throw th;
                }
            }
            final int viewsSize = viewStates.size();
            final AutofillId[] autofillIds = new AutofillId[viewsSize];
            ArrayList<AutofillValue> currentValues = new ArrayList<>(viewsSize);
            int k = 0;
            for (ViewState viewState : viewStates) {
                currentValues.add(viewState.getCurrentValue());
                autofillIds[k] = viewState.id;
                k++;
            }
            RemoteCallback callback = new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda3
                public final void onResult(Bundle bundle) {
                    Session.this.m2055xb568b61e(saveDialogNotShowReason, commitReason, viewsSize, autofillIds, userValues2, categoryIds2, detectedFieldIds, detectedFieldClassifications, bundle);
                }
            });
            fcStrategy.calculateScores(callback, currentValues, userValues2, categoryIds2, defaultAlgorithm, defaultArgs, algorithms, args);
            return;
        } else {
            categoryIds = categoryIds2;
            userValues = userValues2;
        }
        int valuesLength = userValues == null ? -1 : userValues.length;
        int idsLength = categoryIds != null ? categoryIds.length : -1;
        Slog.w(TAG, "setScores(): user data mismatch: values.length = " + valuesLength + ", ids.length = " + idsLength);
    }

    /* JADX WARN: Incorrect condition in loop: B:114:0x0134 */
    /* renamed from: lambda$logFieldClassificationScore$2$com-android-server-autofill-Session */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public /* synthetic */ void m2055xb568b61e(int saveDialogNotShowReason, int commitReason, int viewsSize, AutofillId[] autofillIds, String[] userValues, String[] categoryIds, ArrayList detectedFieldIds, ArrayList detectedFieldClassifications, Bundle result) {
        if (result == null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "setFieldClassificationScore(): no results");
            }
            logContextCommitted(null, null, saveDialogNotShowReason, commitReason);
            return;
        }
        AutofillFieldClassificationService.Scores scores = result.getParcelable("scores");
        if (scores == null) {
            Slog.w(TAG, "No field classification score on " + result);
            return;
        }
        int j = 0;
        for (int i = 0; i < viewsSize; i++) {
            try {
                AutofillId autofillId = autofillIds[i];
                ArrayMap<String, Float> scoresByField = null;
                j = 0;
                while (j < userValues.length) {
                    String categoryId = categoryIds[j];
                    float score = scores.scores[i][j];
                    if (score > 0.0f) {
                        if (scoresByField == null) {
                            scoresByField = new ArrayMap<>(userValues.length);
                        }
                        Float currentScore = scoresByField.get(categoryId);
                        if (currentScore != null && currentScore.floatValue() > score) {
                            if (Helper.sVerbose) {
                                Slog.v(TAG, "skipping score " + score + " because it's less than " + currentScore);
                            }
                        } else {
                            if (Helper.sVerbose) {
                                Slog.v(TAG, "adding score " + score + " at index " + j + " and id " + autofillId);
                            }
                            scoresByField.put(categoryId, Float.valueOf(score));
                        }
                    } else if (Helper.sVerbose) {
                        Slog.v(TAG, "skipping score 0 at index " + j + " and id " + autofillId);
                    }
                    j++;
                }
                if (scoresByField == null) {
                    if (Helper.sVerbose) {
                        Slog.v(TAG, "no score for autofillId=" + autofillId);
                    }
                } else {
                    ArrayList<FieldClassification.Match> matches = new ArrayList<>(scoresByField.size());
                    j = 0;
                    while (j < j) {
                        String fieldId = scoresByField.keyAt(j);
                        matches.add(new FieldClassification.Match(fieldId, scoresByField.valueAt(j).floatValue()));
                        j++;
                    }
                    detectedFieldIds.add(autofillId);
                    detectedFieldClassifications.add(new FieldClassification(matches));
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                wtf(e, "Error accessing FC score at [%d, %d] (%s): %s", Integer.valueOf(i), Integer.valueOf(j), scores, e);
                return;
            }
        }
        logContextCommitted(detectedFieldIds, detectedFieldClassifications, saveDialogNotShowReason, commitReason);
    }

    public void logSaveUiShown() {
        this.mHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((Session) obj).logSaveShown();
            }
        }, this));
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[MOVE, MOVE]}, finally: {[MOVE] complete} */
    /* JADX DEBUG: Different variable names in phi insn: [allRequiredAreNotEmpty, isUpdate], use first */
    /* JADX DEBUG: Multi-variable search result rejected for r1v4, resolved type: int */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v3 */
    /* JADX WARN: Type inference failed for: r1v8 */
    public SaveResult showSaveLocked() {
        boolean allRequiredAreNotEmpty;
        boolean atLeastOneChanged;
        boolean allRequiredAreNotEmpty2;
        boolean z;
        CharSequence serviceLabel;
        Drawable serviceIcon;
        int i;
        boolean z2;
        List<Dataset> datasets;
        InternalValidator validator;
        AutofillId[] requiredIds;
        ArraySet<AutofillId> savableIds;
        ArrayMap<AutofillId, AutofillValue> datasetValues;
        int i2;
        int saveDialogNotShowReason;
        boolean atLeastOneChanged2;
        boolean isUpdate;
        boolean allRequiredAreNotEmpty3;
        boolean allRequiredAreNotEmpty4;
        boolean atLeastOneChanged3;
        boolean z3;
        boolean isUpdate2;
        if (!this.mDestroyed) {
            this.mSessionState = 2;
            FillResponse response = getLastResponseLocked("showSaveLocked(%s)");
            SaveInfo saveInfo = response == null ? null : response.getSaveInfo();
            if (saveInfo == null) {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "showSaveLocked(" + this.id + "): no saveInfo from service");
                }
                return new SaveResult(false, true, 1);
            } else if ((saveInfo.getFlags() & 4) != 0) {
                if (Helper.sDebug) {
                    Slog.v(TAG, "showSaveLocked(" + this.id + "): service asked to delay save");
                }
                return new SaveResult(false, false, 2);
            } else {
                ArrayMap<AutofillId, InternalSanitizer> sanitizers = Helper.createSanitizers(saveInfo);
                ArrayMap<AutofillId, AutofillValue> currentValues = new ArrayMap<>();
                ArraySet<AutofillId> savableIds2 = new ArraySet<>();
                AutofillId[] requiredIds2 = saveInfo.getRequiredIds();
                boolean allRequiredAreNotEmpty5 = true;
                boolean atLeastOneChanged4 = false;
                boolean allRequiredAreNotEmpty6 = false;
                if (requiredIds2 != null) {
                    int i3 = 0;
                    while (true) {
                        if (i3 >= requiredIds2.length) {
                            allRequiredAreNotEmpty5 = allRequiredAreNotEmpty3;
                            break;
                        }
                        AutofillId id = requiredIds2[i3];
                        if (id == null) {
                            Slog.w(TAG, "null autofill id on " + Arrays.toString(requiredIds2));
                            allRequiredAreNotEmpty4 = allRequiredAreNotEmpty3;
                            atLeastOneChanged3 = atLeastOneChanged4;
                            z3 = allRequiredAreNotEmpty6;
                        } else {
                            savableIds2.add(id);
                            ViewState viewState = this.mViewStates.get(id);
                            if (viewState == null) {
                                Slog.w(TAG, "showSaveLocked(): no ViewState for required " + id);
                                allRequiredAreNotEmpty5 = false;
                                break;
                            }
                            AutofillValue value = viewState.getCurrentValue();
                            if (value == null || value.isEmpty()) {
                                AutofillValue initialValue = getValueFromContextsLocked(id);
                                if (initialValue != null) {
                                    if (!Helper.sDebug) {
                                        allRequiredAreNotEmpty4 = allRequiredAreNotEmpty3;
                                        atLeastOneChanged3 = atLeastOneChanged4;
                                    } else {
                                        allRequiredAreNotEmpty4 = allRequiredAreNotEmpty3;
                                        atLeastOneChanged3 = atLeastOneChanged4;
                                        Slog.d(TAG, "Value of required field " + id + " didn't change; using initial value (" + initialValue + ") instead");
                                    }
                                    value = initialValue;
                                } else {
                                    boolean atLeastOneChanged5 = atLeastOneChanged4;
                                    boolean isUpdate3 = allRequiredAreNotEmpty6;
                                    if (Helper.sDebug) {
                                        Slog.d(TAG, "empty value for required " + id);
                                    }
                                    allRequiredAreNotEmpty5 = false;
                                    atLeastOneChanged4 = atLeastOneChanged5;
                                    allRequiredAreNotEmpty6 = isUpdate3;
                                }
                            } else {
                                allRequiredAreNotEmpty4 = allRequiredAreNotEmpty3;
                                atLeastOneChanged3 = atLeastOneChanged4;
                            }
                            AutofillValue value2 = getSanitizedValue(sanitizers, id, value);
                            if (value2 == null) {
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "value of required field " + id + " failed sanitization");
                                }
                                allRequiredAreNotEmpty5 = false;
                                atLeastOneChanged4 = atLeastOneChanged3;
                            } else {
                                viewState.setSanitizedValue(value2);
                                currentValues.put(id, value2);
                                AutofillValue filledValue = viewState.getAutofilledValue();
                                if (value2.equals(filledValue)) {
                                    z3 = allRequiredAreNotEmpty6;
                                } else {
                                    boolean changed = true;
                                    if (filledValue == null) {
                                        AutofillValue initialValue2 = getValueFromContextsLocked(id);
                                        if (initialValue2 == null || !initialValue2.equals(value2)) {
                                            isUpdate2 = allRequiredAreNotEmpty6;
                                        } else {
                                            if (Helper.sDebug) {
                                                isUpdate2 = allRequiredAreNotEmpty6;
                                                Slog.d(TAG, "id " + id + " is part of dataset but initial value didn't change: " + value2);
                                            } else {
                                                isUpdate2 = allRequiredAreNotEmpty6;
                                            }
                                            changed = false;
                                        }
                                        allRequiredAreNotEmpty6 = isUpdate2;
                                    } else {
                                        allRequiredAreNotEmpty6 = true;
                                    }
                                    if (!changed) {
                                        atLeastOneChanged4 = atLeastOneChanged3;
                                    } else {
                                        if (Helper.sDebug) {
                                            Slog.d(TAG, "found a change on required " + id + ": " + filledValue + " => " + value2);
                                        }
                                        atLeastOneChanged4 = true;
                                    }
                                    i3++;
                                    allRequiredAreNotEmpty3 = allRequiredAreNotEmpty4;
                                }
                            }
                        }
                        atLeastOneChanged4 = atLeastOneChanged3;
                        allRequiredAreNotEmpty6 = z3;
                        i3++;
                        allRequiredAreNotEmpty3 = allRequiredAreNotEmpty4;
                    }
                }
                AutofillId[] optionalIds = saveInfo.getOptionalIds();
                if (Helper.sVerbose) {
                    Slog.v(TAG, "allRequiredAreNotEmpty: " + allRequiredAreNotEmpty5 + " hasOptional: " + (optionalIds != null));
                }
                if (!allRequiredAreNotEmpty5) {
                    saveDialogNotShowReason = 3;
                } else {
                    if (optionalIds != null && (!atLeastOneChanged4 || !allRequiredAreNotEmpty5)) {
                        for (AutofillId id2 : optionalIds) {
                            savableIds2.add(id2);
                            ViewState viewState2 = this.mViewStates.get(id2);
                            if (viewState2 == null) {
                                atLeastOneChanged2 = atLeastOneChanged4;
                                Slog.w(TAG, "no ViewState for optional " + id2);
                                isUpdate = allRequiredAreNotEmpty6;
                            } else {
                                atLeastOneChanged2 = atLeastOneChanged4;
                                if ((viewState2.getState() & 8) != 0) {
                                    AutofillValue value3 = getSanitizedValue(sanitizers, id2, viewState2.getCurrentValue());
                                    if (value3 == null) {
                                        if (Helper.sDebug) {
                                            isUpdate = allRequiredAreNotEmpty6;
                                            Slog.d(TAG, "value of opt. field " + id2 + " failed sanitization");
                                        } else {
                                            isUpdate = allRequiredAreNotEmpty6;
                                        }
                                    } else {
                                        boolean isUpdate4 = allRequiredAreNotEmpty6;
                                        currentValues.put(id2, value3);
                                        AutofillValue filledValue2 = viewState2.getAutofilledValue();
                                        if (value3 != null && !value3.equals(filledValue2)) {
                                            if (Helper.sDebug) {
                                                Slog.d(TAG, "found a change on optional " + id2 + ": " + filledValue2 + " => " + value3);
                                            }
                                            if (filledValue2 == null) {
                                                allRequiredAreNotEmpty6 = isUpdate4;
                                            } else {
                                                allRequiredAreNotEmpty6 = true;
                                            }
                                            atLeastOneChanged4 = true;
                                        } else {
                                            atLeastOneChanged4 = atLeastOneChanged2;
                                            allRequiredAreNotEmpty6 = isUpdate4;
                                        }
                                    }
                                } else {
                                    isUpdate = allRequiredAreNotEmpty6;
                                    AutofillValue initialValue3 = getValueFromContextsLocked(id2);
                                    if (Helper.sDebug) {
                                        Slog.d(TAG, "no current value for " + id2 + "; initial value is " + initialValue3);
                                    }
                                    if (initialValue3 != null) {
                                        currentValues.put(id2, initialValue3);
                                    }
                                }
                            }
                            atLeastOneChanged4 = atLeastOneChanged2;
                            allRequiredAreNotEmpty6 = isUpdate;
                        }
                        atLeastOneChanged = atLeastOneChanged4;
                        allRequiredAreNotEmpty2 = allRequiredAreNotEmpty6;
                    } else {
                        atLeastOneChanged = atLeastOneChanged4;
                    }
                    if (!atLeastOneChanged) {
                        saveDialogNotShowReason = 4;
                        atLeastOneChanged4 = atLeastOneChanged;
                    } else {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "at least one field changed, validate fields for save UI");
                        }
                        InternalValidator validator2 = saveInfo.getValidator();
                        if (validator2 != null) {
                            LogMaker log = newLogMaker(1133);
                            try {
                                boolean isValid = validator2.isValid(this);
                                if (Helper.sDebug) {
                                    Slog.d(TAG, validator2 + " returned " + isValid);
                                }
                                if (isValid) {
                                    i2 = 10;
                                } else {
                                    i2 = 5;
                                }
                                log.setType(i2);
                                this.mMetricsLogger.write(log);
                                if (!isValid) {
                                    Slog.i(TAG, "not showing save UI because fields failed validation");
                                    return new SaveResult(false, true, 5);
                                }
                            } catch (Exception e) {
                                Slog.e(TAG, "Not showing save UI because validation failed:", e);
                                log.setType(11);
                                this.mMetricsLogger.write(log);
                                return new SaveResult(false, true, 5);
                            }
                        }
                        List<Dataset> datasets2 = response.getDatasets();
                        if (datasets2 != null) {
                            int i4 = 0;
                            while (i4 < datasets2.size()) {
                                Dataset dataset = datasets2.get(i4);
                                ArrayMap<AutofillId, AutofillValue> datasetValues2 = Helper.getFields(dataset);
                                if (!Helper.sVerbose) {
                                    datasets = datasets2;
                                } else {
                                    datasets = datasets2;
                                    Slog.v(TAG, "Checking if saved fields match contents of dataset #" + i4 + ": " + dataset + "; savableIds=" + savableIds2);
                                }
                                int j = 0;
                                while (j < savableIds2.size()) {
                                    AutofillId id3 = savableIds2.valueAt(j);
                                    AutofillValue currentValue = currentValues.get(id3);
                                    if (currentValue == null) {
                                        if (!Helper.sDebug) {
                                            validator = validator2;
                                            requiredIds = requiredIds2;
                                            savableIds = savableIds2;
                                            datasetValues = datasetValues2;
                                        } else {
                                            validator = validator2;
                                            requiredIds = requiredIds2;
                                            savableIds = savableIds2;
                                            Slog.d(TAG, "dataset has value for field that is null: " + id3);
                                            datasetValues = datasetValues2;
                                        }
                                    } else {
                                        validator = validator2;
                                        requiredIds = requiredIds2;
                                        savableIds = savableIds2;
                                        AutofillValue datasetValue = datasetValues2.get(id3);
                                        if (!currentValue.equals(datasetValue)) {
                                            if (Helper.sDebug) {
                                                Slog.d(TAG, "found a dataset change on id " + id3 + ": from " + datasetValue + " to " + currentValue);
                                            }
                                            i4++;
                                            datasets2 = datasets;
                                            validator2 = validator;
                                            requiredIds2 = requiredIds;
                                            savableIds2 = savableIds;
                                        } else {
                                            datasetValues = datasetValues2;
                                            if (Helper.sVerbose) {
                                                Slog.v(TAG, "no dataset changes for id " + id3);
                                            }
                                        }
                                    }
                                    j++;
                                    validator2 = validator;
                                    requiredIds2 = requiredIds;
                                    savableIds2 = savableIds;
                                    datasetValues2 = datasetValues;
                                }
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "ignoring Save UI because all fields match contents of dataset #" + i4 + ": " + dataset);
                                }
                                return new SaveResult(false, true, 6);
                            }
                            z = true;
                        } else {
                            z = true;
                        }
                        if (Helper.sDebug) {
                            Slog.d(TAG, "Good news, everyone! All checks passed, show save UI for " + this.id + "!");
                        }
                        IAutoFillManagerClient client = getClient();
                        this.mPendingSaveUi = new PendingUi(new Binder(), this.id, client);
                        synchronized (this.mLock) {
                            try {
                                serviceLabel = this.mService.getServiceLabelLocked();
                                serviceIcon = this.mService.getServiceIconLocked();
                            } finally {
                                th = th;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th) {
                                        th = th;
                                    }
                                }
                            }
                        }
                        if (serviceLabel == null) {
                            i = 0;
                            z2 = z;
                        } else if (serviceIcon != null) {
                            boolean z4 = z;
                            getUiForShowing().showSaveUi(serviceLabel, serviceIcon, this.mService.getServicePackageName(), saveInfo, this, this.mComponentName, this, this.mPendingSaveUi, allRequiredAreNotEmpty2, this.mCompatMode);
                            if (client != null) {
                                try {
                                    client.setSaveUiState(this.id, z4);
                                } catch (RemoteException e2) {
                                    Slog.e(TAG, "Error notifying client to set save UI state to shown: " + e2);
                                }
                            }
                            this.mSessionFlags.mShowingSaveUi = z4;
                            return new SaveResult(z4, false, 0);
                        } else {
                            i = 0;
                            z2 = z;
                        }
                        wtf(null, "showSaveLocked(): no service label or icon", new Object[i]);
                        return new SaveResult(i, z2, i);
                    }
                }
                if (Helper.sDebug) {
                    Slog.d(TAG, "showSaveLocked(" + this.id + "): with no changes, comes no responsibilities.allRequiredAreNotNull=" + allRequiredAreNotEmpty5 + ", atLeastOneChanged=" + atLeastOneChanged4);
                }
                return new SaveResult(false, true, saveDialogNotShowReason);
            }
        }
        Slog.w(TAG, "Call to Session#showSaveLocked() rejected - session: " + this.id + " destroyed");
        return new SaveResult(false, false, 0);
    }

    public void logSaveShown() {
        this.mService.logSaveShown(this.id, this.mClientState);
    }

    private AutofillValue getSanitizedValue(ArrayMap<AutofillId, InternalSanitizer> sanitizers, AutofillId id, AutofillValue value) {
        if (sanitizers == null || value == null) {
            return value;
        }
        ViewState state = this.mViewStates.get(id);
        AutofillValue sanitized = state == null ? null : state.getSanitizedValue();
        if (sanitized == null) {
            InternalSanitizer sanitizer = sanitizers.get(id);
            if (sanitizer == null) {
                return value;
            }
            sanitized = sanitizer.sanitize(value);
            if (Helper.sDebug) {
                Slog.d(TAG, "Value for " + id + "(" + value + ") sanitized to " + sanitized);
            }
            if (state != null) {
                state.setSanitizedValue(sanitized);
            }
        }
        return sanitized;
    }

    public boolean isSaveUiShowingLocked() {
        return this.mSessionFlags.mShowingSaveUi;
    }

    private AutofillValue getValueFromContextsLocked(AutofillId autofillId) {
        int numContexts = this.mContexts.size();
        for (int i = numContexts - 1; i >= 0; i--) {
            FillContext context = this.mContexts.get(i);
            AssistStructure.ViewNode node = Helper.findViewNodeByAutofillId(context.getStructure(), autofillId);
            if (node != null) {
                AutofillValue value = node.getAutofillValue();
                if (Helper.sDebug) {
                    Slog.d(TAG, "getValueFromContexts(" + this.id + SliceClientPermissions.SliceAuthority.DELIMITER + autofillId + ") at " + i + ": " + value);
                }
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private CharSequence[] getAutofillOptionsFromContextsLocked(AutofillId autofillId) {
        int numContexts = this.mContexts.size();
        for (int i = numContexts - 1; i >= 0; i--) {
            FillContext context = this.mContexts.get(i);
            AssistStructure.ViewNode node = Helper.findViewNodeByAutofillId(context.getStructure(), autofillId);
            if (node != null && node.getAutofillOptions() != null) {
                return node.getAutofillOptions();
            }
        }
        return null;
    }

    private void updateValuesForSaveLocked() {
        ArrayMap<AutofillId, InternalSanitizer> sanitizers = Helper.createSanitizers(getSaveInfoLocked());
        int numContexts = this.mContexts.size();
        for (int contextNum = 0; contextNum < numContexts; contextNum++) {
            FillContext context = this.mContexts.get(contextNum);
            AssistStructure.ViewNode[] nodes = context.findViewNodesByAutofillIds(getIdsOfAllViewStatesLocked());
            if (Helper.sVerbose) {
                Slog.v(TAG, "updateValuesForSaveLocked(): updating " + context);
            }
            for (int viewStateNum = 0; viewStateNum < this.mViewStates.size(); viewStateNum++) {
                ViewState viewState = this.mViewStates.valueAt(viewStateNum);
                AutofillId id = viewState.id;
                AutofillValue value = viewState.getCurrentValue();
                if (value == null) {
                    if (Helper.sVerbose) {
                        Slog.v(TAG, "updateValuesForSaveLocked(): skipping " + id);
                    }
                } else {
                    AssistStructure.ViewNode node = nodes[viewStateNum];
                    if (node == null) {
                        Slog.w(TAG, "callSaveLocked(): did not find node with id " + id);
                    } else {
                        if (Helper.sVerbose) {
                            Slog.v(TAG, "updateValuesForSaveLocked(): updating " + id + " to " + value);
                        }
                        AutofillValue sanitizedValue = viewState.getSanitizedValue();
                        if (sanitizedValue == null) {
                            sanitizedValue = getSanitizedValue(sanitizers, id, value);
                        }
                        if (sanitizedValue != null) {
                            node.updateAutofillValue(sanitizedValue);
                        } else if (Helper.sDebug) {
                            Slog.d(TAG, "updateValuesForSaveLocked(): not updating field " + id + " because it failed sanitization");
                        }
                    }
                }
            }
            context.getStructure().sanitizeForParceling(false);
            if (Helper.sVerbose) {
                Slog.v(TAG, "updateValuesForSaveLocked(): dumping structure of " + context + " before calling service.save()");
                context.getStructure().dump(false);
            }
        }
    }

    public void callSaveLocked() {
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#callSaveLocked() rejected - session: " + this.id + " destroyed");
        } else if (this.mRemoteFillService == null) {
            wtf(null, "callSaveLocked() called without a remote service. mForAugmentedAutofillOnly: %s", Boolean.valueOf(this.mSessionFlags.mAugmentedAutofillOnly));
        } else {
            if (Helper.sVerbose) {
                Slog.v(TAG, "callSaveLocked(" + this.id + "): mViewStates=" + this.mViewStates);
            }
            if (this.mContexts == null) {
                Slog.w(TAG, "callSaveLocked(): no contexts");
                return;
            }
            updateValuesForSaveLocked();
            cancelCurrentRequestLocked();
            ArrayList<FillContext> contexts = mergePreviousSessionLocked(true);
            SaveRequest saveRequest = new SaveRequest(contexts, this.mClientState, this.mSelectedDatasetIds);
            this.mRemoteFillService.onSaveRequest(saveRequest);
        }
    }

    public ArrayList<FillContext> mergePreviousSessionLocked(boolean forSave) {
        ArrayList<Session> previousSessions = this.mService.getPreviousSessionsLocked(this);
        if (previousSessions != null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "mergeSessions(" + this.id + "): Merging the content of " + previousSessions.size() + " sessions for task " + this.taskId);
            }
            ArrayList<FillContext> contexts = new ArrayList<>();
            for (int i = 0; i < previousSessions.size(); i++) {
                Session previousSession = previousSessions.get(i);
                ArrayList<FillContext> previousContexts = previousSession.mContexts;
                if (previousContexts == null) {
                    Slog.w(TAG, "mergeSessions(" + this.id + "): Not merging null contexts from " + previousSession.id);
                } else {
                    if (forSave) {
                        previousSession.updateValuesForSaveLocked();
                    }
                    if (Helper.sDebug) {
                        Slog.d(TAG, "mergeSessions(" + this.id + "): adding " + previousContexts.size() + " context from previous session #" + previousSession.id);
                    }
                    contexts.addAll(previousContexts);
                    if (this.mClientState == null && previousSession.mClientState != null) {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "mergeSessions(" + this.id + "): setting client state from previous session" + previousSession.id);
                        }
                        this.mClientState = previousSession.mClientState;
                    }
                }
            }
            contexts.addAll(this.mContexts);
            return contexts;
        }
        return new ArrayList<>(this.mContexts);
    }

    private boolean requestNewFillResponseOnViewEnteredIfNecessaryLocked(AutofillId id, ViewState viewState, int flags) {
        if ((flags & 1) != 0) {
            this.mSessionFlags.mAugmentedAutofillOnly = false;
            if (Helper.sDebug) {
                Slog.d(TAG, "Re-starting session on view " + id + " and flags " + flags);
            }
            requestNewFillResponseLocked(viewState, 256, flags);
            return true;
        } else if (shouldStartNewPartitionLocked(id)) {
            if (Helper.sDebug) {
                Slog.d(TAG, "Starting partition or augmented request for view id " + id + ": " + viewState.getStateAsString());
            }
            this.mSessionFlags.mAugmentedAutofillOnly = false;
            requestNewFillResponseLocked(viewState, 32, flags);
            return true;
        } else {
            if (Helper.sVerbose) {
                Slog.v(TAG, "Not starting new partition for view " + id + ": " + viewState.getStateAsString());
            }
            return false;
        }
    }

    private boolean shouldStartNewPartitionLocked(AutofillId id) {
        ViewState currentView = this.mViewStates.get(id);
        if (this.mResponses == null) {
            return currentView != null && (currentView.getState() & 65536) == 0;
        } else if (this.mSessionFlags.mExpiredResponse) {
            if (Helper.sDebug) {
                Slog.d(TAG, "Starting a new partition because the response has expired.");
            }
            return true;
        } else {
            int numResponses = this.mResponses.size();
            if (numResponses >= AutofillManagerService.getPartitionMaxCount()) {
                Slog.e(TAG, "Not starting a new partition on " + id + " because session " + this.id + " reached maximum of " + AutofillManagerService.getPartitionMaxCount());
                return false;
            }
            for (int responseNum = 0; responseNum < numResponses; responseNum++) {
                FillResponse response = this.mResponses.valueAt(responseNum);
                if (ArrayUtils.contains(response.getIgnoredIds(), id)) {
                    return false;
                }
                SaveInfo saveInfo = response.getSaveInfo();
                if (saveInfo != null && (ArrayUtils.contains(saveInfo.getOptionalIds(), id) || ArrayUtils.contains(saveInfo.getRequiredIds(), id))) {
                    return false;
                }
                List<Dataset> datasets = response.getDatasets();
                if (datasets != null) {
                    int numDatasets = datasets.size();
                    for (int dataSetNum = 0; dataSetNum < numDatasets; dataSetNum++) {
                        ArrayList<AutofillId> fields = datasets.get(dataSetNum).getFieldIds();
                        if (fields != null && fields.contains(id)) {
                            return false;
                        }
                    }
                }
                if (ArrayUtils.contains(response.getAuthenticationIds(), id)) {
                    return false;
                }
            }
            return true;
        }
    }

    public void updateLocked(AutofillId id, Rect virtualBounds, AutofillValue value, int action, int flags) {
        String currentUrl;
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#updateLocked() rejected - session: " + id + " destroyed");
        } else if (action == 5) {
            this.mSessionFlags.mExpiredResponse = true;
            if (Helper.sDebug) {
                Slog.d(TAG, "Set the response has expired.");
            }
            this.mPresentationStatsEventLogger.maybeSetNoPresentationEventReason(3);
            this.mPresentationStatsEventLogger.logAndEndEvent();
        } else {
            id.setSessionId(this.id);
            if (Helper.sVerbose) {
                Slog.v(TAG, "updateLocked(" + this.id + "): id=" + id + ", action=" + actionAsString(action) + ", flags=" + flags);
            }
            ViewState viewState = this.mViewStates.get(id);
            if (Helper.sVerbose) {
                Slog.v(TAG, "updateLocked(" + this.id + "): mCurrentViewId=" + this.mCurrentViewId + ", mExpiredResponse=" + this.mSessionFlags.mExpiredResponse + ", viewState=" + viewState);
            }
            if (viewState == null) {
                if (action == 1 || action == 4 || action == 2) {
                    if (Helper.sVerbose) {
                        Slog.v(TAG, "Creating viewState for " + id);
                    }
                    boolean isIgnored = isIgnoredLocked(id);
                    viewState = new ViewState(id, this, isIgnored ? 128 : 1);
                    this.mViewStates.put(id, viewState);
                    if (isIgnored) {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "updateLocked(): ignoring view " + viewState);
                            return;
                        }
                        return;
                    }
                } else if (Helper.sVerbose) {
                    Slog.v(TAG, "Ignoring specific action when viewState=null");
                    return;
                } else {
                    return;
                }
            }
            switch (action) {
                case 1:
                    this.mCurrentViewId = viewState.id;
                    viewState.update(value, virtualBounds, flags);
                    if (!isRequestSupportFillDialog(flags)) {
                        this.mSessionFlags.mFillDialogDisabled = true;
                    }
                    this.mPresentationStatsEventLogger.startNewEvent();
                    requestNewFillResponseLocked(viewState, 16, flags);
                    return;
                case 2:
                    if (Helper.sVerbose && virtualBounds != null) {
                        Slog.v(TAG, "entered on virtual child " + id + ": " + virtualBounds);
                    }
                    boolean isSameViewEntered = Objects.equals(this.mCurrentViewId, viewState.id);
                    this.mCurrentViewId = viewState.id;
                    if (value != null) {
                        viewState.setCurrentValue(value);
                    }
                    if (!this.mCompatMode || (viewState.getState() & 512) == 0) {
                        this.mPresentationStatsEventLogger.maybeSetNoPresentationEventReason(2);
                        this.mPresentationStatsEventLogger.logAndEndEvent();
                        if ((flags & 1) == 0) {
                            ArrayList<AutofillId> arrayList = this.mAugmentedAutofillableIds;
                            if (arrayList != null && arrayList.contains(id)) {
                                if (!isSameViewEntered) {
                                    if (Helper.sDebug) {
                                        Slog.d(TAG, "trigger augmented autofill.");
                                    }
                                    triggerAugmentedAutofillLocked(flags);
                                    return;
                                } else if (Helper.sDebug) {
                                    Slog.d(TAG, "skip augmented autofill for same view: same view entered");
                                    return;
                                } else {
                                    return;
                                }
                            } else if (this.mSessionFlags.mAugmentedAutofillOnly && isSameViewEntered) {
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "skip augmented autofill for same view: standard autofill disabled.");
                                    return;
                                }
                                return;
                            }
                        }
                        this.mPresentationStatsEventLogger.startNewEvent();
                        if (requestNewFillResponseOnViewEnteredIfNecessaryLocked(id, viewState, flags)) {
                            return;
                        }
                        if (viewState.getResponse() != null) {
                            FillResponse response = viewState.getResponse();
                            this.mPresentationStatsEventLogger.maybeSetRequestId(response.getRequestId());
                            this.mPresentationStatsEventLogger.maybeSetAvailableCount(response.getDatasets(), this.mCurrentViewId);
                        }
                        if (isSameViewEntered) {
                            setFillDialogDisabledAndStartInput();
                            return;
                        } else {
                            viewState.update(value, virtualBounds, flags);
                            return;
                        }
                    } else if (Helper.sDebug) {
                        Slog.d(TAG, "Ignoring VIEW_ENTERED on URL BAR (id=" + id + ")");
                        return;
                    } else {
                        return;
                    }
                case 3:
                    if (Objects.equals(this.mCurrentViewId, viewState.id)) {
                        if (Helper.sVerbose) {
                            Slog.v(TAG, "Exiting view " + id);
                        }
                        this.mUi.hideFillUi(this);
                        this.mUi.hideFillDialog(this);
                        hideAugmentedAutofillLocked(viewState);
                        this.mInlineSessionController.resetInlineFillUiLocked();
                        this.mCurrentViewId = null;
                        this.mPresentationStatsEventLogger.maybeSetNoPresentationEventReason(2);
                        this.mPresentationStatsEventLogger.logAndEndEvent();
                        return;
                    }
                    return;
                case 4:
                    if (this.mCompatMode && (viewState.getState() & 512) != 0) {
                        AssistStructure.ViewNode viewNode = this.mUrlBar;
                        if (viewNode == null) {
                            currentUrl = null;
                        } else {
                            currentUrl = viewNode.getText().toString().trim();
                        }
                        if (currentUrl == null) {
                            wtf(null, "URL bar value changed, but current value is null", new Object[0]);
                            return;
                        } else if (value == null || !value.isText()) {
                            wtf(null, "URL bar value changed to null or non-text: %s", value);
                            return;
                        } else {
                            String newUrl = value.getTextValue().toString();
                            if (newUrl.equals(currentUrl)) {
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "Ignoring change on URL bar as it's the same");
                                    return;
                                }
                                return;
                            } else if (this.mSaveOnAllViewsInvisible) {
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "Ignoring change on URL because session will finish when views are gone");
                                    return;
                                }
                                return;
                            } else {
                                if (Helper.sDebug) {
                                    Slog.d(TAG, "Finishing session because URL bar changed");
                                }
                                forceRemoveFromServiceLocked(5);
                                return;
                            }
                        }
                    } else if (!Objects.equals(value, viewState.getCurrentValue())) {
                        logIfViewClearedLocked(id, value, viewState);
                        updateViewStateAndUiOnValueChangedLocked(id, value, viewState, flags);
                        return;
                    } else {
                        return;
                    }
                default:
                    Slog.w(TAG, "updateLocked(): unknown action: " + action);
                    return;
            }
        }
    }

    private void hideAugmentedAutofillLocked(ViewState viewState) {
        if ((viewState.getState() & 4096) != 0) {
            viewState.resetState(4096);
            cancelAugmentedAutofillLocked();
        }
    }

    private boolean isIgnoredLocked(AutofillId id) {
        FillResponse response = getLastResponseLocked(null);
        if (response == null) {
            return false;
        }
        return ArrayUtils.contains(response.getIgnoredIds(), id);
    }

    private void logIfViewClearedLocked(AutofillId id, AutofillValue value, ViewState viewState) {
        if ((value == null || value.isEmpty()) && viewState.getCurrentValue() != null && viewState.getCurrentValue().isText() && viewState.getCurrentValue().getTextValue() != null && getSaveInfoLocked() != null) {
            int length = viewState.getCurrentValue().getTextValue().length();
            if (Helper.sDebug) {
                Slog.d(TAG, "updateLocked(" + id + "): resetting value that was " + length + " chars long");
            }
            LogMaker log = newLogMaker(1124).addTaggedData(1125, Integer.valueOf(length));
            this.mMetricsLogger.write(log);
        }
    }

    private void updateViewStateAndUiOnValueChangedLocked(AutofillId id, AutofillValue value, ViewState viewState, int flags) {
        String textValue;
        if (value == null || !value.isText()) {
            textValue = null;
        } else {
            CharSequence text = value.getTextValue();
            textValue = text == null ? null : text.toString();
        }
        updateFilteringStateOnValueChangedLocked(textValue, viewState);
        viewState.setCurrentValue(value);
        String filterText = textValue;
        AutofillValue filledValue = viewState.getAutofilledValue();
        if (filledValue != null) {
            if (filledValue.equals(value)) {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "ignoring autofilled change on id " + id);
                }
                this.mInlineSessionController.hideInlineSuggestionsUiLocked(viewState.id);
                viewState.resetState(8);
                return;
            } else if (viewState.id.equals(this.mCurrentViewId) && (viewState.getState() & 4) != 0) {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "field changed after autofill on id " + id);
                }
                viewState.resetState(4);
                ViewState currentView = this.mViewStates.get(this.mCurrentViewId);
                currentView.maybeCallOnFillReady(flags);
            }
        }
        if (viewState.id.equals(this.mCurrentViewId) && (viewState.getState() & 8192) != 0) {
            if ((viewState.getState() & 32768) != 0) {
                this.mInlineSessionController.disableFilterMatching(viewState.id);
            }
            this.mInlineSessionController.filterInlineFillUiLocked(this.mCurrentViewId, filterText);
        } else if (viewState.id.equals(this.mCurrentViewId) && (viewState.getState() & 4096) != 0 && !TextUtils.isEmpty(filterText)) {
            this.mInlineSessionController.hideInlineSuggestionsUiLocked(this.mCurrentViewId);
        }
        viewState.setState(8);
        getUiForShowing().filterFillUi(filterText, this);
    }

    private void updateFilteringStateOnValueChangedLocked(String newTextValue, ViewState viewState) {
        String currentTextValue;
        if (newTextValue == null) {
            newTextValue = "";
        }
        AutofillValue currentValue = viewState.getCurrentValue();
        if (currentValue == null || !currentValue.isText()) {
            currentTextValue = "";
        } else {
            currentTextValue = currentValue.getTextValue().toString();
        }
        if ((viewState.getState() & 16384) == 0) {
            if (!Helper.containsCharsInOrder(newTextValue, currentTextValue)) {
                viewState.setState(16384);
            }
        } else if (!Helper.containsCharsInOrder(currentTextValue, newTextValue)) {
            viewState.setState(32768);
        }
    }

    @Override // com.android.server.autofill.ViewState.Listener
    public void onFillReady(FillResponse response, AutofillId filledId, AutofillValue value, int flags) {
        String filterText;
        CharSequence serviceLabel;
        Drawable serviceIcon;
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#onFillReady() rejected - session: " + this.id + " destroyed");
                return;
            }
            if (value != null && value.isText()) {
                String filterText2 = value.getTextValue().toString();
                filterText = filterText2;
            } else {
                filterText = null;
            }
            synchronized (this.mLock) {
                serviceLabel = this.mService.getServiceLabelLocked();
                serviceIcon = this.mService.getServiceIconLocked();
            }
            if (serviceLabel == null || serviceIcon == null) {
                wtf(null, "onFillReady(): no service label or icon", new Object[0]);
                return;
            }
            AutofillId[] ids = response.getFillDialogTriggerIds();
            if (ids != null && ArrayUtils.contains(ids, filledId)) {
                if (requestShowFillDialog(response, filledId, filterText, flags)) {
                    synchronized (this.mLock) {
                        ViewState currentView = this.mViewStates.get(this.mCurrentViewId);
                        currentView.setState(131072);
                        this.mService.logDatasetShown(this.id, this.mClientState, 3);
                        this.mPresentationStatsEventLogger.maybeSetCountShown(response.getDatasets(), this.mCurrentViewId);
                        this.mPresentationStatsEventLogger.maybeSetDisplayPresentationType(3);
                    }
                    setFillDialogDisabled();
                    return;
                }
                setFillDialogDisabled();
            }
            if (response.supportsInlineSuggestions()) {
                synchronized (this.mLock) {
                    if (requestShowInlineSuggestionsLocked(response, filterText)) {
                        ViewState currentView2 = this.mViewStates.get(this.mCurrentViewId);
                        currentView2.setState(8192);
                        this.mService.logDatasetShown(this.id, this.mClientState, 2);
                        this.mPresentationStatsEventLogger.maybeSetCountShown(response.getDatasets(), this.mCurrentViewId);
                        this.mPresentationStatsEventLogger.maybeSetDisplayPresentationType(2);
                        return;
                    }
                }
            }
            getUiForShowing().showFillUi(filledId, response, filterText, this.mService.getServicePackageName(), this.mComponentName, serviceLabel, serviceIcon, this, this.id, this.mCompatMode);
            synchronized (this.mLock) {
                this.mService.logDatasetShown(this.id, this.mClientState, 1);
                this.mPresentationStatsEventLogger.maybeSetCountShown(response.getDatasets(), this.mCurrentViewId);
                this.mPresentationStatsEventLogger.maybeSetDisplayPresentationType(1);
            }
            synchronized (this.mLock) {
                if (this.mUiShownTime == 0) {
                    long elapsedRealtime = SystemClock.elapsedRealtime();
                    this.mUiShownTime = elapsedRealtime;
                    long duration = elapsedRealtime - this.mStartTime;
                    if (Helper.sDebug) {
                        StringBuilder msg = new StringBuilder("1st UI for ").append(this.mActivityToken).append(" shown in ");
                        TimeUtils.formatDuration(duration, msg);
                        Slog.d(TAG, msg.toString());
                    }
                    StringBuilder historyLog = new StringBuilder("id=").append(this.id).append(" app=").append(this.mActivityToken).append(" svc=").append(this.mService.getServicePackageName()).append(" latency=");
                    TimeUtils.formatDuration(duration, historyLog);
                    this.mUiLatencyHistory.log(historyLog.toString());
                    addTaggedDataToRequestLogLocked(response.getRequestId(), 1145, Long.valueOf(duration));
                }
            }
        }
    }

    private void updateFillDialogTriggerIdsLocked() {
        FillResponse response = getLastResponseLocked(null);
        if (response == null) {
            return;
        }
        AutofillId[] ids = response.getFillDialogTriggerIds();
        notifyClientFillDialogTriggerIds(ids != null ? Arrays.asList(ids) : null);
    }

    private void notifyClientFillDialogTriggerIds(List<AutofillId> fieldIds) {
        try {
            if (Helper.sVerbose) {
                Slog.v(TAG, "notifyFillDialogTriggerIds(): " + fieldIds);
            }
            getClient().notifyFillDialogTriggerIds(fieldIds);
        } catch (RemoteException e) {
            Slog.w(TAG, "Cannot set trigger ids for fill dialog", e);
        }
    }

    private boolean isFillDialogUiEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = !this.mSessionFlags.mFillDialogDisabled;
        }
        return z;
    }

    private void setFillDialogDisabled() {
        synchronized (this.mLock) {
            this.mSessionFlags.mFillDialogDisabled = true;
        }
        notifyClientFillDialogTriggerIds(null);
    }

    private void setFillDialogDisabledAndStartInput() {
        AutofillId id;
        if (getUiForShowing().isFillDialogShowing()) {
            setFillDialogDisabled();
            synchronized (this.mLock) {
                id = this.mCurrentViewId;
            }
            requestShowSoftInput(id);
        }
    }

    private boolean requestShowFillDialog(FillResponse response, AutofillId filledId, String filterText, int flags) {
        if (isFillDialogUiEnabled() && (flags & 128) == 0) {
            Drawable serviceIcon = getServiceIcon();
            getUiForShowing().showFillDialog(filledId, response, filterText, this.mService.getServicePackageName(), this.mComponentName, serviceIcon, this, this.id, this.mCompatMode);
            return true;
        }
        return false;
    }

    private Drawable getServiceIcon() {
        Drawable serviceIconLocked;
        synchronized (this.mLock) {
            serviceIconLocked = this.mService.getServiceIconLocked();
        }
        return serviceIconLocked;
    }

    private boolean requestShowInlineSuggestionsLocked(final FillResponse response, String filterText) {
        if (this.mCurrentViewId == null) {
            Log.w(TAG, "requestShowInlineSuggestionsLocked(): no view currently focused");
            return false;
        }
        final AutofillId focusedId = this.mCurrentViewId;
        Optional<InlineSuggestionsRequest> inlineSuggestionsRequest = this.mInlineSessionController.getInlineSuggestionsRequestLocked();
        if (!inlineSuggestionsRequest.isPresent()) {
            Log.w(TAG, "InlineSuggestionsRequest unavailable");
            return false;
        }
        RemoteInlineSuggestionRenderService remoteRenderService = this.mService.getRemoteInlineSuggestionRenderServiceLocked();
        if (remoteRenderService == null) {
            Log.w(TAG, "RemoteInlineSuggestionRenderService not found");
            return false;
        }
        InlineFillUi.InlineFillUiInfo inlineFillUiInfo = new InlineFillUi.InlineFillUiInfo(inlineSuggestionsRequest.get(), focusedId, filterText, remoteRenderService, this.userId, this.id);
        InlineFillUi inlineFillUi = InlineFillUi.forAutofill(inlineFillUiInfo, response, new InlineFillUi.InlineSuggestionUiCallback() { // from class: com.android.server.autofill.Session.3
            {
                Session.this = this;
            }

            @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
            public void autofill(Dataset dataset, int datasetIndex) {
                Session.this.fill(response.getRequestId(), datasetIndex, dataset, 2);
            }

            @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
            public void authenticate(int requestId, int datasetIndex) {
                Session.this.authenticate(response.getRequestId(), datasetIndex, response.getAuthentication(), response.getClientState(), true);
            }

            @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
            public void startIntentSender(IntentSender intentSender) {
                Session.this.startIntentSender(intentSender, new Intent());
            }

            @Override // com.android.server.autofill.ui.InlineFillUi.InlineSuggestionUiCallback
            public void onError() {
                synchronized (Session.this.mLock) {
                    Session.this.mInlineSessionController.setInlineFillUiLocked(InlineFillUi.emptyUi(focusedId));
                }
            }
        });
        return this.mInlineSessionController.setInlineFillUiLocked(inlineFillUi);
    }

    public boolean isDestroyed() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDestroyed;
        }
        return z;
    }

    public IAutoFillManagerClient getClient() {
        IAutoFillManagerClient iAutoFillManagerClient;
        synchronized (this.mLock) {
            iAutoFillManagerClient = this.mClient;
        }
        return iAutoFillManagerClient;
    }

    private void notifyUnavailableToClient(int sessionFinishedState, ArrayList<AutofillId> autofillableIds) {
        synchronized (this.mLock) {
            AutofillId autofillId = this.mCurrentViewId;
            if (autofillId == null) {
                return;
            }
            try {
                if (this.mHasCallback) {
                    this.mClient.notifyNoFillUi(this.id, autofillId, sessionFinishedState);
                } else if (sessionFinishedState != 0) {
                    this.mClient.setSessionFinished(sessionFinishedState, autofillableIds);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Error notifying client no fill UI: id=" + this.mCurrentViewId, e);
            }
        }
    }

    private void notifyDisableAutofillToClient(long disableDuration, ComponentName componentName) {
        synchronized (this.mLock) {
            if (this.mCurrentViewId == null) {
                return;
            }
            try {
                this.mClient.notifyDisableAutofill(disableDuration, componentName);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error notifying client disable autofill: id=" + this.mCurrentViewId, e);
            }
        }
    }

    private void updateTrackedIdsLocked() {
        boolean saveOnFinish;
        AutofillId saveTriggerId;
        int flags;
        ArraySet<AutofillId> trackedViews;
        ArraySet<AutofillId> fillableIds;
        boolean z;
        FillResponse response = getLastResponseLocked(null);
        if (response == null) {
            return;
        }
        ArraySet<AutofillId> trackedViews2 = null;
        this.mSaveOnAllViewsInvisible = false;
        SaveInfo saveInfo = response.getSaveInfo();
        boolean z2 = true;
        if (saveInfo == null) {
            saveOnFinish = true;
            saveTriggerId = null;
            flags = 0;
            trackedViews = null;
        } else {
            AutofillId saveTriggerId2 = saveInfo.getTriggerId();
            if (saveTriggerId2 != null) {
                writeLog(1228);
            }
            int flags2 = saveInfo.getFlags();
            if ((flags2 & 1) == 0) {
                z = false;
            } else {
                z = true;
            }
            this.mSaveOnAllViewsInvisible = z;
            if (z) {
                if (0 == 0) {
                    trackedViews2 = new ArraySet<>();
                }
                if (saveInfo.getRequiredIds() != null) {
                    Collections.addAll(trackedViews2, saveInfo.getRequiredIds());
                }
                if (saveInfo.getOptionalIds() != null) {
                    Collections.addAll(trackedViews2, saveInfo.getOptionalIds());
                }
            }
            if ((flags2 & 2) == 0) {
                saveOnFinish = true;
                saveTriggerId = saveTriggerId2;
                flags = flags2;
                trackedViews = trackedViews2;
            } else {
                saveOnFinish = false;
                saveTriggerId = saveTriggerId2;
                flags = flags2;
                trackedViews = trackedViews2;
            }
        }
        List<Dataset> datasets = response.getDatasets();
        ArraySet<AutofillId> fillableIds2 = null;
        if (datasets == null) {
            fillableIds = null;
        } else {
            for (int i = 0; i < datasets.size(); i++) {
                Dataset dataset = datasets.get(i);
                ArrayList<AutofillId> fieldIds = dataset.getFieldIds();
                if (fieldIds != null) {
                    for (int j = 0; j < fieldIds.size(); j++) {
                        AutofillId id = fieldIds.get(j);
                        if (trackedViews == null || !trackedViews.contains(id)) {
                            fillableIds2 = ArrayUtils.add(fillableIds2, id);
                        }
                    }
                }
            }
            fillableIds = fillableIds2;
        }
        try {
            if (Helper.sVerbose) {
                try {
                    StringBuilder append = new StringBuilder().append("updateTrackedIdsLocked(): ").append(trackedViews).append(" => ").append(fillableIds).append(" triggerId: ").append(saveTriggerId).append(" saveOnFinish:").append(saveOnFinish).append(" flags: ").append(flags).append(" hasSaveInfo: ");
                    if (saveInfo == null) {
                        z2 = false;
                    }
                    Slog.v(TAG, append.append(z2).toString());
                } catch (RemoteException e) {
                    e = e;
                    Slog.w(TAG, "Cannot set tracked ids", e);
                }
            }
        } catch (RemoteException e2) {
            e = e2;
        }
        try {
            this.mClient.setTrackedViews(this.id, Helper.toArray(trackedViews), this.mSaveOnAllViewsInvisible, saveOnFinish, Helper.toArray(fillableIds), saveTriggerId);
        } catch (RemoteException e3) {
            e = e3;
            Slog.w(TAG, "Cannot set tracked ids", e);
        }
    }

    public void setAutofillFailureLocked(List<AutofillId> ids) {
        for (int i = 0; i < ids.size(); i++) {
            AutofillId id = ids.get(i);
            ViewState viewState = this.mViewStates.get(id);
            if (viewState == null) {
                Slog.w(TAG, "setAutofillFailure(): no view for id " + id);
            } else {
                viewState.resetState(4);
                int state = viewState.getState();
                viewState.setState(state | 1024);
                if (Helper.sVerbose) {
                    Slog.v(TAG, "Changed state of " + id + " to " + viewState.getStateAsString());
                }
            }
        }
    }

    private void replaceResponseLocked(FillResponse oldResponse, FillResponse newResponse, Bundle newClientState) {
        setViewStatesLocked(oldResponse, 1, true);
        newResponse.setRequestId(oldResponse.getRequestId());
        this.mResponses.put(newResponse.getRequestId(), newResponse);
        processResponseLocked(newResponse, newClientState, 0);
    }

    private void processNullResponseLocked(int requestId, int flags) {
        ArrayList<AutofillId> autofillableIds;
        unregisterDelayedFillBroadcastLocked();
        if ((flags & 1) != 0) {
            getUiForShowing().showError(17039735, this);
        }
        FillContext context = getFillContextByRequestIdLocked(requestId);
        if (context == null) {
            Slog.w(TAG, "processNullResponseLocked(): no context for req " + requestId);
            autofillableIds = null;
        } else {
            AssistStructure structure = context.getStructure();
            autofillableIds = Helper.getAutofillIds(structure, true);
        }
        this.mService.resetLastResponse();
        Runnable triggerAugmentedAutofillLocked = triggerAugmentedAutofillLocked(flags);
        this.mAugmentedAutofillDestroyer = triggerAugmentedAutofillLocked;
        if (triggerAugmentedAutofillLocked == null && (flags & 4) == 0) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "canceling session " + this.id + " when service returned null and it cannot be augmented. AutofillableIds: " + autofillableIds);
            }
            notifyUnavailableToClient(2, autofillableIds);
            removeFromService();
            return;
        }
        if ((flags & 4) != 0) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "keeping session " + this.id + " when service returned null and augmented service is disabled for password fields. AutofillableIds: " + autofillableIds);
            }
            this.mInlineSessionController.hideInlineSuggestionsUiLocked(this.mCurrentViewId);
        } else if (Helper.sVerbose) {
            Slog.v(TAG, "keeping session " + this.id + " when service returned null but it can be augmented. AutofillableIds: " + autofillableIds);
        }
        this.mAugmentedAutofillableIds = autofillableIds;
        try {
            this.mClient.setState(32);
        } catch (RemoteException e) {
            Slog.e(TAG, "Error setting client to autofill-only", e);
        }
    }

    private Runnable triggerAugmentedAutofillLocked(int flags) {
        if ((flags & 4) != 0) {
            return null;
        }
        int supportedModes = this.mService.getSupportedSmartSuggestionModesLocked();
        if (supportedModes == 0) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "triggerAugmentedAutofillLocked(): no supported modes");
            }
            return null;
        }
        final RemoteAugmentedAutofillService remoteService = this.mService.getRemoteAugmentedAutofillServiceLocked();
        if (remoteService == null) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "triggerAugmentedAutofillLocked(): no service for user");
            }
            return null;
        } else if ((supportedModes & 1) == 0) {
            Slog.w(TAG, "Unsupported Smart Suggestion mode: " + supportedModes);
            return null;
        } else if (this.mCurrentViewId == null) {
            Slog.w(TAG, "triggerAugmentedAutofillLocked(): no view currently focused");
            return null;
        } else {
            final boolean isWhitelisted = this.mService.isWhitelistedForAugmentedAutofillLocked(this.mComponentName);
            if (!isWhitelisted) {
                if (Helper.sVerbose) {
                    Slog.v(TAG, "triggerAugmentedAutofillLocked(): " + ComponentName.flattenToShortString(this.mComponentName) + " not whitelisted ");
                }
                logAugmentedAutofillRequestLocked(1, remoteService.getComponentName(), this.mCurrentViewId, isWhitelisted, null);
                return null;
            }
            if (Helper.sVerbose) {
                Slog.v(TAG, "calling Augmented Autofill Service (" + ComponentName.flattenToShortString(remoteService.getComponentName()) + ") on view " + this.mCurrentViewId + " using suggestion mode " + AutofillManager.getSmartSuggestionModeToString(1) + " when server returned null for session " + this.id);
            }
            ViewState viewState = this.mViewStates.get(this.mCurrentViewId);
            viewState.setState(4096);
            final AutofillValue currentValue = viewState.getCurrentValue();
            if (this.mAugmentedRequestsLogs == null) {
                this.mAugmentedRequestsLogs = new ArrayList<>();
            }
            LogMaker log = newLogMaker(1630, remoteService.getComponentName().getPackageName());
            this.mAugmentedRequestsLogs.add(log);
            final AutofillId focusedId = this.mCurrentViewId;
            final Function<InlineFillUi, Boolean> inlineSuggestionsResponseCallback = new Function() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda11
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return Session.this.m2058x8828dcb7((InlineFillUi) obj);
                }
            };
            final Consumer<InlineSuggestionsRequest> requestAugmentedAutofill = new Consumer() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda12
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    Session.this.m2060xaf7883b9(r2, remoteService, focusedId, isWhitelisted, currentValue, inlineSuggestionsResponseCallback, (InlineSuggestionsRequest) obj);
                }
            };
            RemoteInlineSuggestionRenderService remoteRenderService = this.mService.getRemoteInlineSuggestionRenderServiceLocked();
            if ((remoteRenderService != null && ((this.mSessionFlags.mAugmentedAutofillOnly || !this.mSessionFlags.mInlineSupportedByService || this.mSessionFlags.mExpiredResponse) && isViewFocusedLocked(flags))) || isFillDialogUiEnabled()) {
                if (Helper.sDebug) {
                    Slog.d(TAG, "Create inline request for augmented autofill");
                }
                remoteRenderService.getInlineSuggestionsRendererInfo(new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda13
                    public final void onResult(Bundle bundle) {
                        Session.this.m2061xc320573a(focusedId, requestAugmentedAutofill, bundle);
                    }
                }, this.mHandler));
            } else {
                requestAugmentedAutofill.accept(this.mInlineSessionController.getInlineSuggestionsRequestLocked().orElse(null));
            }
            if (this.mAugmentedAutofillDestroyer == null) {
                Objects.requireNonNull(remoteService);
                this.mAugmentedAutofillDestroyer = new Runnable() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda14
                    @Override // java.lang.Runnable
                    public final void run() {
                        RemoteAugmentedAutofillService.this.onDestroyAutofillWindowsRequest();
                    }
                };
            }
            return this.mAugmentedAutofillDestroyer;
        }
    }

    /* renamed from: lambda$triggerAugmentedAutofillLocked$3$com-android-server-autofill-Session */
    public /* synthetic */ Boolean m2058x8828dcb7(InlineFillUi response) {
        Boolean valueOf;
        synchronized (this.mLock) {
            valueOf = Boolean.valueOf(this.mInlineSessionController.setInlineFillUiLocked(response));
        }
        return valueOf;
    }

    /* renamed from: lambda$triggerAugmentedAutofillLocked$5$com-android-server-autofill-Session */
    public /* synthetic */ void m2060xaf7883b9(int mode, RemoteAugmentedAutofillService remoteService, AutofillId focusedId, boolean isWhitelisted, AutofillValue currentValue, Function inlineSuggestionsResponseCallback, InlineSuggestionsRequest inlineSuggestionsRequest) {
        synchronized (this.mLock) {
            logAugmentedAutofillRequestLocked(mode, remoteService.getComponentName(), focusedId, isWhitelisted, Boolean.valueOf(inlineSuggestionsRequest != null));
            remoteService.onRequestAutofillLocked(this.id, this.mClient, this.taskId, this.mComponentName, this.mActivityToken, AutofillId.withoutSession(focusedId), currentValue, inlineSuggestionsRequest, inlineSuggestionsResponseCallback, new Runnable() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Session.this.m2059x9bd0b038();
                }
            }, this.mService.getRemoteInlineSuggestionRenderServiceLocked(), this.userId);
        }
    }

    /* renamed from: lambda$triggerAugmentedAutofillLocked$4$com-android-server-autofill-Session */
    public /* synthetic */ void m2059x9bd0b038() {
        synchronized (this.mLock) {
            cancelAugmentedAutofillLocked();
            this.mInlineSessionController.setInlineFillUiLocked(InlineFillUi.emptyUi(this.mCurrentViewId));
        }
    }

    /* renamed from: lambda$triggerAugmentedAutofillLocked$6$com-android-server-autofill-Session */
    public /* synthetic */ void m2061xc320573a(AutofillId focusedId, Consumer requestAugmentedAutofill, Bundle extras) {
        synchronized (this.mLock) {
            this.mInlineSessionController.onCreateInlineSuggestionsRequestLocked(focusedId, requestAugmentedAutofill, extras);
        }
    }

    private void cancelAugmentedAutofillLocked() {
        RemoteAugmentedAutofillService remoteService = this.mService.getRemoteAugmentedAutofillServiceLocked();
        if (remoteService == null) {
            Slog.w(TAG, "cancelAugmentedAutofillLocked(): no service for user");
            return;
        }
        if (Helper.sVerbose) {
            Slog.v(TAG, "cancelAugmentedAutofillLocked() on " + this.mCurrentViewId);
        }
        remoteService.onDestroyAutofillWindowsRequest();
    }

    private void processResponseLocked(FillResponse newResponse, Bundle newClientState, int flags) {
        this.mUi.hideAll(this);
        if ((newResponse.getFlags() & 4) == 0) {
            Slog.d(TAG, "Service did not request to wait for delayed fill response.");
            unregisterDelayedFillBroadcastLocked();
        }
        int requestId = newResponse.getRequestId();
        if (Helper.sVerbose) {
            Slog.v(TAG, "processResponseLocked(): mCurrentViewId=" + this.mCurrentViewId + ",flags=" + flags + ", reqId=" + requestId + ", resp=" + newResponse + ",newClientState=" + newClientState);
        }
        if (this.mResponses == null) {
            this.mResponses = new SparseArray<>(2);
        }
        this.mResponses.put(requestId, newResponse);
        this.mClientState = newClientState != null ? newClientState : newResponse.getClientState();
        this.mPresentationStatsEventLogger.maybeSetAvailableCount(newResponse.getDatasets(), this.mCurrentViewId);
        setViewStatesLocked(newResponse, 2, false);
        updateFillDialogTriggerIdsLocked();
        updateTrackedIdsLocked();
        AutofillId autofillId = this.mCurrentViewId;
        if (autofillId == null) {
            return;
        }
        ViewState currentView = this.mViewStates.get(autofillId);
        currentView.maybeCallOnFillReady(flags);
    }

    private void setViewStatesLocked(FillResponse response, int state, boolean clearResponse) {
        AutofillId[] authenticationIds;
        List<Dataset> datasets = response.getDatasets();
        if (datasets != null) {
            for (int i = 0; i < datasets.size(); i++) {
                Dataset dataset = datasets.get(i);
                if (dataset == null) {
                    Slog.w(TAG, "Ignoring null dataset on " + datasets);
                } else {
                    setViewStatesLocked(response, dataset, state, clearResponse);
                }
            }
        } else if (response.getAuthentication() != null) {
            for (AutofillId autofillId : response.getAuthenticationIds()) {
                ViewState viewState = createOrUpdateViewStateLocked(autofillId, state, null);
                if (!clearResponse) {
                    viewState.setResponse(response);
                } else {
                    viewState.setResponse(null);
                }
            }
        }
        SaveInfo saveInfo = response.getSaveInfo();
        if (saveInfo != null) {
            AutofillId[] requiredIds = saveInfo.getRequiredIds();
            if (requiredIds != null) {
                for (AutofillId id : requiredIds) {
                    createOrUpdateViewStateLocked(id, state, null);
                }
            }
            AutofillId[] optionalIds = saveInfo.getOptionalIds();
            if (optionalIds != null) {
                for (AutofillId id2 : optionalIds) {
                    createOrUpdateViewStateLocked(id2, state, null);
                }
            }
        }
        AutofillId[] authIds = response.getAuthenticationIds();
        if (authIds != null) {
            for (AutofillId id3 : authIds) {
                createOrUpdateViewStateLocked(id3, state, null);
            }
        }
    }

    private void setViewStatesLocked(FillResponse response, Dataset dataset, int state, boolean clearResponse) {
        ArrayList<AutofillId> ids = dataset.getFieldIds();
        ArrayList<AutofillValue> values = dataset.getFieldValues();
        for (int j = 0; j < ids.size(); j++) {
            AutofillId id = ids.get(j);
            AutofillValue value = values.get(j);
            ViewState viewState = createOrUpdateViewStateLocked(id, state, value);
            String datasetId = dataset.getId();
            if (datasetId != null) {
                viewState.setDatasetId(datasetId);
            }
            if (clearResponse) {
                viewState.setResponse(null);
            } else if (response != null) {
                viewState.setResponse(response);
            }
        }
    }

    private ViewState createOrUpdateViewStateLocked(AutofillId id, int state, AutofillValue value) {
        ViewState viewState = this.mViewStates.get(id);
        if (viewState != null) {
            viewState.setState(state);
        } else {
            viewState = new ViewState(id, this, state);
            if (Helper.sVerbose) {
                Slog.v(TAG, "Adding autofillable view with id " + id + " and state " + state);
            }
            viewState.setCurrentValue(findValueLocked(id));
            this.mViewStates.put(id, viewState);
        }
        if ((state & 4) != 0) {
            viewState.setAutofilledValue(value);
        }
        return viewState;
    }

    public void autoFill(int requestId, int datasetIndex, Dataset dataset, boolean generateEvent, int uiType) {
        if (Helper.sDebug) {
            Slog.d(TAG, "autoFill(): requestId=" + requestId + "; datasetIdx=" + datasetIndex + "; dataset=" + dataset);
        }
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#autoFill() rejected - session: " + this.id + " destroyed");
            } else if (dataset.getAuthentication() == null) {
                if (generateEvent) {
                    this.mService.logDatasetSelected(dataset.getId(), this.id, this.mClientState, uiType);
                }
                AutofillId autofillId = this.mCurrentViewId;
                if (autofillId != null) {
                    this.mInlineSessionController.hideInlineSuggestionsUiLocked(autofillId);
                }
                autoFillApp(dataset);
            } else {
                this.mService.logDatasetAuthenticationSelected(dataset.getId(), this.id, this.mClientState);
                setViewStatesLocked(null, dataset, 64, false);
                Intent fillInIntent = createAuthFillInIntentLocked(requestId, this.mClientState);
                if (fillInIntent == null) {
                    forceRemoveFromServiceLocked();
                    return;
                }
                int authenticationId = AutofillManager.makeAuthenticationId(requestId, datasetIndex);
                startAuthentication(authenticationId, dataset.getAuthentication(), fillInIntent, false);
            }
        }
    }

    private Intent createAuthFillInIntentLocked(int requestId, Bundle extras) {
        Intent fillInIntent = new Intent();
        FillContext context = getFillContextByRequestIdLocked(requestId);
        if (context == null) {
            wtf(null, "createAuthFillInIntentLocked(): no FillContext. requestId=%d; mContexts=%s", Integer.valueOf(requestId), this.mContexts);
            return null;
        }
        Pair<Integer, InlineSuggestionsRequest> pair = this.mLastInlineSuggestionsRequest;
        if (pair != null && ((Integer) pair.first).intValue() == requestId) {
            fillInIntent.putExtra("android.view.autofill.extra.INLINE_SUGGESTIONS_REQUEST", (Parcelable) this.mLastInlineSuggestionsRequest.second);
        }
        fillInIntent.putExtra("android.view.autofill.extra.ASSIST_STRUCTURE", context.getStructure());
        fillInIntent.putExtra("android.view.autofill.extra.CLIENT_STATE", extras);
        return fillInIntent;
    }

    private Consumer<InlineSuggestionsRequest> inlineSuggestionsRequestCacheDecorator(final Consumer<InlineSuggestionsRequest> consumer, final int requestId) {
        return new Consumer() { // from class: com.android.server.autofill.Session$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                Session.this.m2054x82acedf9(consumer, requestId, (InlineSuggestionsRequest) obj);
            }
        };
    }

    /* renamed from: lambda$inlineSuggestionsRequestCacheDecorator$7$com-android-server-autofill-Session */
    public /* synthetic */ void m2054x82acedf9(Consumer consumer, int requestId, InlineSuggestionsRequest inlineSuggestionsRequest) {
        consumer.accept(inlineSuggestionsRequest);
        synchronized (this.mLock) {
            this.mLastInlineSuggestionsRequest = Pair.create(Integer.valueOf(requestId), inlineSuggestionsRequest);
        }
    }

    public void startAuthentication(int authenticationId, IntentSender intent, Intent fillInIntent, boolean authenticateInline) {
        try {
            synchronized (this.mLock) {
                this.mClient.authenticate(this.id, authenticationId, intent, fillInIntent, authenticateInline);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Error launching auth intent", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class SaveResult {
        private boolean mLogSaveShown;
        private boolean mRemoveSession;
        private int mSaveDialogNotShowReason;

        SaveResult(boolean logSaveShown, boolean removeSession, int saveDialogNotShowReason) {
            this.mLogSaveShown = logSaveShown;
            this.mRemoveSession = removeSession;
            this.mSaveDialogNotShowReason = saveDialogNotShowReason;
        }

        public boolean isLogSaveShown() {
            return this.mLogSaveShown;
        }

        public void setLogSaveShown(boolean logSaveShown) {
            this.mLogSaveShown = logSaveShown;
        }

        public boolean isRemoveSession() {
            return this.mRemoveSession;
        }

        public void setRemoveSession(boolean removeSession) {
            this.mRemoveSession = removeSession;
        }

        public int getNoSaveUiReason() {
            return this.mSaveDialogNotShowReason;
        }

        public void setSaveDialogNotShowReason(int saveDialogNotShowReason) {
            this.mSaveDialogNotShowReason = saveDialogNotShowReason;
        }

        public String toString() {
            return "SaveResult: [logSaveShown=" + this.mLogSaveShown + ", removeSession=" + this.mRemoveSession + ", saveDialogNotShowReason=" + this.mSaveDialogNotShowReason + "]";
        }
    }

    public String toString() {
        return "Session: [id=" + this.id + ", component=" + this.mComponentName + ", state=" + sessionStateAsString(this.mSessionState) + "]";
    }

    public void dumpLocked(String prefix, PrintWriter pw) {
        String prefix2 = prefix + "  ";
        pw.print(prefix);
        pw.print("id: ");
        pw.println(this.id);
        pw.print(prefix);
        pw.print("uid: ");
        pw.println(this.uid);
        pw.print(prefix);
        pw.print("taskId: ");
        pw.println(this.taskId);
        pw.print(prefix);
        pw.print("flags: ");
        pw.println(this.mFlags);
        pw.print(prefix);
        pw.print("state: ");
        pw.println(sessionStateAsString(this.mSessionState));
        pw.print(prefix);
        pw.print("mComponentName: ");
        pw.println(this.mComponentName);
        pw.print(prefix);
        pw.print("mActivityToken: ");
        pw.println(this.mActivityToken);
        pw.print(prefix);
        pw.print("mStartTime: ");
        pw.println(this.mStartTime);
        pw.print(prefix);
        pw.print("Time to show UI: ");
        long j = this.mUiShownTime;
        if (j == 0) {
            pw.println("N/A");
        } else {
            TimeUtils.formatDuration(j - this.mStartTime, pw);
            pw.println();
        }
        int requestLogsSizes = this.mRequestLogs.size();
        pw.print(prefix);
        pw.print("mSessionLogs: ");
        pw.println(requestLogsSizes);
        for (int i = 0; i < requestLogsSizes; i++) {
            int requestId = this.mRequestLogs.keyAt(i);
            LogMaker log = this.mRequestLogs.valueAt(i);
            pw.print(prefix2);
            pw.print('#');
            pw.print(i);
            pw.print(": req=");
            pw.print(requestId);
            pw.print(", log=");
            dumpRequestLog(pw, log);
            pw.println();
        }
        pw.print(prefix);
        pw.print("mResponses: ");
        SparseArray<FillResponse> sparseArray = this.mResponses;
        if (sparseArray == null) {
            pw.println("null");
        } else {
            pw.println(sparseArray.size());
            for (int i2 = 0; i2 < this.mResponses.size(); i2++) {
                pw.print(prefix2);
                pw.print('#');
                pw.print(i2);
                pw.print(' ');
                pw.println(this.mResponses.valueAt(i2));
            }
        }
        pw.print(prefix);
        pw.print("mCurrentViewId: ");
        pw.println(this.mCurrentViewId);
        pw.print(prefix);
        pw.print("mDestroyed: ");
        pw.println(this.mDestroyed);
        pw.print(prefix);
        pw.print("mShowingSaveUi: ");
        pw.println(this.mSessionFlags.mShowingSaveUi);
        pw.print(prefix);
        pw.print("mPendingSaveUi: ");
        pw.println(this.mPendingSaveUi);
        int numberViews = this.mViewStates.size();
        pw.print(prefix);
        pw.print("mViewStates size: ");
        pw.println(this.mViewStates.size());
        for (int i3 = 0; i3 < numberViews; i3++) {
            pw.print(prefix);
            pw.print("ViewState at #");
            pw.println(i3);
            this.mViewStates.valueAt(i3).dump(prefix2, pw);
        }
        pw.print(prefix);
        pw.print("mContexts: ");
        ArrayList<FillContext> arrayList = this.mContexts;
        if (arrayList != null) {
            int numContexts = arrayList.size();
            for (int i4 = 0; i4 < numContexts; i4++) {
                FillContext context = this.mContexts.get(i4);
                pw.print(prefix2);
                pw.print(context);
                if (Helper.sVerbose) {
                    pw.println("AssistStructure dumped at logcat)");
                    context.getStructure().dump(false);
                }
            }
        } else {
            pw.println("null");
        }
        pw.print(prefix);
        pw.print("mHasCallback: ");
        pw.println(this.mHasCallback);
        if (this.mClientState != null) {
            pw.print(prefix);
            pw.print("mClientState: ");
            pw.print(this.mClientState.getSize());
            pw.println(" bytes");
        }
        pw.print(prefix);
        pw.print("mCompatMode: ");
        pw.println(this.mCompatMode);
        pw.print(prefix);
        pw.print("mUrlBar: ");
        if (this.mUrlBar == null) {
            pw.println("N/A");
        } else {
            pw.print("id=");
            pw.print(this.mUrlBar.getAutofillId());
            pw.print(" domain=");
            pw.print(this.mUrlBar.getWebDomain());
            pw.print(" text=");
            Helper.printlnRedactedText(pw, this.mUrlBar.getText());
        }
        pw.print(prefix);
        pw.print("mSaveOnAllViewsInvisible: ");
        pw.println(this.mSaveOnAllViewsInvisible);
        pw.print(prefix);
        pw.print("mSelectedDatasetIds: ");
        pw.println(this.mSelectedDatasetIds);
        if (this.mSessionFlags.mAugmentedAutofillOnly) {
            pw.print(prefix);
            pw.println("For Augmented Autofill Only");
        }
        if (this.mAugmentedAutofillDestroyer != null) {
            pw.print(prefix);
            pw.println("has mAugmentedAutofillDestroyer");
        }
        if (this.mAugmentedRequestsLogs != null) {
            pw.print(prefix);
            pw.print("number augmented requests: ");
            pw.println(this.mAugmentedRequestsLogs.size());
        }
        if (this.mAugmentedAutofillableIds != null) {
            pw.print(prefix);
            pw.print("mAugmentedAutofillableIds: ");
            pw.println(this.mAugmentedAutofillableIds);
        }
        RemoteFillService remoteFillService = this.mRemoteFillService;
        if (remoteFillService != null) {
            remoteFillService.dump(prefix, pw);
        }
    }

    private static void dumpRequestLog(PrintWriter pw, LogMaker log) {
        pw.print("CAT=");
        pw.print(log.getCategory());
        pw.print(", TYPE=");
        int type = log.getType();
        switch (type) {
            case 2:
                pw.print("CLOSE");
                break;
            case 10:
                pw.print("SUCCESS");
                break;
            case 11:
                pw.print("FAILURE");
                break;
            default:
                pw.print("UNSUPPORTED");
                break;
        }
        pw.print('(');
        pw.print(type);
        pw.print(')');
        pw.print(", PKG=");
        pw.print(log.getPackageName());
        pw.print(", SERVICE=");
        pw.print(log.getTaggedData(908));
        pw.print(", ORDINAL=");
        pw.print(log.getTaggedData(1454));
        dumpNumericValue(pw, log, "FLAGS", 1452);
        dumpNumericValue(pw, log, "NUM_DATASETS", 909);
        dumpNumericValue(pw, log, "UI_LATENCY", 1145);
        int authStatus = Helper.getNumericValue(log, 1453);
        if (authStatus != 0) {
            pw.print(", AUTH_STATUS=");
            switch (authStatus) {
                case 912:
                    pw.print("AUTHENTICATED");
                    break;
                case 1126:
                    pw.print("DATASET_AUTHENTICATED");
                    break;
                case 1127:
                    pw.print("INVALID_DATASET_AUTHENTICATION");
                    break;
                case 1128:
                    pw.print("INVALID_AUTHENTICATION");
                    break;
                default:
                    pw.print("UNSUPPORTED");
                    break;
            }
            pw.print('(');
            pw.print(authStatus);
            pw.print(')');
        }
        dumpNumericValue(pw, log, "FC_IDS", 1271);
        dumpNumericValue(pw, log, "COMPAT_MODE", 1414);
    }

    private static void dumpNumericValue(PrintWriter pw, LogMaker log, String field, int tag) {
        int value = Helper.getNumericValue(log, tag);
        if (value != 0) {
            pw.print(", ");
            pw.print(field);
            pw.print('=');
            pw.print(value);
        }
    }

    void autoFillApp(Dataset dataset) {
        synchronized (this.mLock) {
            if (this.mDestroyed) {
                Slog.w(TAG, "Call to Session#autoFillApp() rejected - session: " + this.id + " destroyed");
                return;
            }
            try {
                int entryCount = dataset.getFieldIds().size();
                List<AutofillId> ids = new ArrayList<>(entryCount);
                List<AutofillValue> values = new ArrayList<>(entryCount);
                boolean waitingDatasetAuth = false;
                boolean hideHighlight = true;
                if (entryCount != 1 || !((AutofillId) dataset.getFieldIds().get(0)).equals(this.mCurrentViewId)) {
                    hideHighlight = false;
                }
                for (int i = 0; i < entryCount; i++) {
                    if (dataset.getFieldValues().get(i) != null) {
                        AutofillId viewId = (AutofillId) dataset.getFieldIds().get(i);
                        ids.add(viewId);
                        values.add((AutofillValue) dataset.getFieldValues().get(i));
                        ViewState viewState = this.mViewStates.get(viewId);
                        if (viewState != null && (viewState.getState() & 64) != 0) {
                            if (Helper.sVerbose) {
                                Slog.v(TAG, "autofillApp(): view " + viewId + " waiting auth");
                            }
                            waitingDatasetAuth = true;
                            viewState.resetState(64);
                        }
                    }
                }
                if (!ids.isEmpty()) {
                    if (waitingDatasetAuth) {
                        this.mUi.hideFillUi(this);
                    }
                    if (Helper.sDebug) {
                        Slog.d(TAG, "autoFillApp(): the buck is on the app: " + dataset);
                    }
                    this.mClient.autofill(this.id, ids, values, hideHighlight);
                    if (dataset.getId() != null) {
                        if (this.mSelectedDatasetIds == null) {
                            this.mSelectedDatasetIds = new ArrayList<>();
                        }
                        this.mSelectedDatasetIds.add(dataset.getId());
                    }
                    setViewStatesLocked(null, dataset, 4, false);
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Error autofilling activity: " + e);
            }
        }
    }

    private AutoFillUI getUiForShowing() {
        AutoFillUI autoFillUI;
        synchronized (this.mLock) {
            this.mUi.setCallback(this);
            autoFillUI = this.mUi;
        }
        return autoFillUI;
    }

    public RemoteFillService destroyLocked() {
        if (this.mDestroyed) {
            return null;
        }
        clearPendingIntentLocked();
        unregisterDelayedFillBroadcastLocked();
        unlinkClientVultureLocked();
        this.mUi.destroyAll(this.mPendingSaveUi, this, true);
        this.mUi.clearCallback(this);
        AutofillId autofillId = this.mCurrentViewId;
        if (autofillId != null) {
            this.mInlineSessionController.destroyLocked(autofillId);
        }
        RemoteInlineSuggestionRenderService remoteRenderService = this.mService.getRemoteInlineSuggestionRenderServiceLocked();
        if (remoteRenderService != null) {
            remoteRenderService.destroySuggestionViews(this.userId, this.id);
        }
        this.mDestroyed = true;
        int totalRequests = this.mRequestLogs.size();
        if (totalRequests > 0) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "destroyLocked(): logging " + totalRequests + " requests");
            }
            for (int i = 0; i < totalRequests; i++) {
                this.mMetricsLogger.write(this.mRequestLogs.valueAt(i));
            }
        }
        ArrayList<LogMaker> arrayList = this.mAugmentedRequestsLogs;
        int totalAugmentedRequests = arrayList == null ? 0 : arrayList.size();
        if (totalAugmentedRequests > 0) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "destroyLocked(): logging " + totalRequests + " augmented requests");
            }
            for (int i2 = 0; i2 < totalAugmentedRequests; i2++) {
                this.mMetricsLogger.write(this.mAugmentedRequestsLogs.get(i2));
            }
        }
        LogMaker log = newLogMaker(919).addTaggedData(1455, Integer.valueOf(totalRequests));
        if (totalAugmentedRequests > 0) {
            log.addTaggedData(1631, Integer.valueOf(totalAugmentedRequests));
        }
        if (this.mSessionFlags.mAugmentedAutofillOnly) {
            log.addTaggedData(1720, 1);
        }
        this.mMetricsLogger.write(log);
        return this.mRemoteFillService;
    }

    public void forceRemoveFromServiceLocked() {
        forceRemoveFromServiceLocked(0);
    }

    public void forceRemoveFromServiceIfForAugmentedOnlyLocked() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "forceRemoveFromServiceIfForAugmentedOnlyLocked(" + this.id + "): " + this.mSessionFlags.mAugmentedAutofillOnly);
        }
        if (this.mSessionFlags.mAugmentedAutofillOnly) {
            forceRemoveFromServiceLocked();
        }
    }

    void forceRemoveFromServiceLocked(int clientState) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "forceRemoveFromServiceLocked(): " + this.mPendingSaveUi);
        }
        boolean isPendingSaveUi = isSaveUiPendingLocked();
        this.mPendingSaveUi = null;
        removeFromServiceLocked();
        this.mUi.destroyAll(this.mPendingSaveUi, this, false);
        if (!isPendingSaveUi) {
            try {
                this.mClient.setSessionFinished(clientState, (List) null);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error notifying client to finish session", e);
            }
        }
        destroyAugmentedAutofillWindowsLocked();
    }

    public void destroyAugmentedAutofillWindowsLocked() {
        Runnable runnable = this.mAugmentedAutofillDestroyer;
        if (runnable != null) {
            runnable.run();
            this.mAugmentedAutofillDestroyer = null;
        }
    }

    public void removeFromService() {
        synchronized (this.mLock) {
            removeFromServiceLocked();
        }
    }

    public void removeFromServiceLocked() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "removeFromServiceLocked(" + this.id + "): " + this.mPendingSaveUi);
        }
        if (this.mDestroyed) {
            Slog.w(TAG, "Call to Session#removeFromServiceLocked() rejected - session: " + this.id + " destroyed");
        } else if (isSaveUiPendingLocked()) {
            Slog.i(TAG, "removeFromServiceLocked() ignored, waiting for pending save ui");
        } else {
            RemoteFillService remoteFillService = destroyLocked();
            this.mService.removeSessionLocked(this.id);
            if (remoteFillService != null) {
                remoteFillService.destroy();
            }
            this.mSessionState = 3;
        }
    }

    public void onPendingSaveUi(int operation, IBinder token) {
        getUiForShowing().onPendingSaveUi(operation, token);
    }

    public boolean isSaveUiPendingForTokenLocked(IBinder token) {
        return isSaveUiPendingLocked() && token.equals(this.mPendingSaveUi.getToken());
    }

    private boolean isSaveUiPendingLocked() {
        PendingUi pendingUi = this.mPendingSaveUi;
        return pendingUi != null && pendingUi.getState() == 2;
    }

    private int getLastResponseIndexLocked() {
        int lastResponseIdx = -1;
        int lastResponseId = -1;
        SparseArray<FillResponse> sparseArray = this.mResponses;
        if (sparseArray != null) {
            int responseCount = sparseArray.size();
            for (int i = 0; i < responseCount; i++) {
                if (this.mResponses.keyAt(i) > lastResponseId) {
                    lastResponseIdx = i;
                    lastResponseId = this.mResponses.keyAt(i);
                }
            }
        }
        return lastResponseIdx;
    }

    private LogMaker newLogMaker(int category) {
        return newLogMaker(category, this.mService.getServicePackageName());
    }

    private LogMaker newLogMaker(int category, String servicePackageName) {
        return Helper.newLogMaker(category, this.mComponentName, servicePackageName, this.id, this.mCompatMode);
    }

    private void writeLog(int category) {
        this.mMetricsLogger.write(newLogMaker(category));
    }

    private void logAuthenticationStatusLocked(int requestId, int status) {
        addTaggedDataToRequestLogLocked(requestId, 1453, Integer.valueOf(status));
    }

    private void addTaggedDataToRequestLogLocked(int requestId, int tag, Object value) {
        LogMaker requestLog = this.mRequestLogs.get(requestId);
        if (requestLog == null) {
            Slog.w(TAG, "addTaggedDataToRequestLogLocked(tag=" + tag + "): no log for id " + requestId);
        } else {
            requestLog.addTaggedData(tag, value);
        }
    }

    private void logAugmentedAutofillRequestLocked(int mode, ComponentName augmentedRemoteServiceName, AutofillId focusedId, boolean isWhitelisted, Boolean isInline) {
        String historyItem = "aug:id=" + this.id + " u=" + this.uid + " m=" + mode + " a=" + ComponentName.flattenToShortString(this.mComponentName) + " f=" + focusedId + " s=" + augmentedRemoteServiceName + " w=" + isWhitelisted + " i=" + isInline;
        this.mService.getMaster().logRequestLocked(historyItem);
    }

    public void wtf(Exception e, String fmt, Object... args) {
        String message = String.format(fmt, args);
        synchronized (this.mLock) {
            this.mWtfHistory.log(message);
        }
        if (e != null) {
            Slog.wtf(TAG, message, e);
        } else {
            Slog.wtf(TAG, message);
        }
    }

    private static String actionAsString(int action) {
        switch (action) {
            case 1:
                return "START_SESSION";
            case 2:
                return "VIEW_ENTERED";
            case 3:
                return "VIEW_EXITED";
            case 4:
                return "VALUE_CHANGED";
            case 5:
                return "RESPONSE_EXPIRED";
            default:
                return "UNKNOWN_" + action;
        }
    }

    private static String sessionStateAsString(int sessionState) {
        switch (sessionState) {
            case 0:
                return "STATE_UNKNOWN";
            case 1:
                return "STATE_ACTIVE";
            case 2:
                return "STATE_FINISHED";
            case 3:
                return "STATE_REMOVED";
            default:
                return "UNKNOWN_SESSION_STATE_" + sessionState;
        }
    }
}
