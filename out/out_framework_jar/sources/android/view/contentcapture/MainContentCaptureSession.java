package android.view.contentcapture;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.graphics.Insets;
import android.graphics.Rect;
import android.inputmethodservice.navigationbar.NavigationBarInflaterView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.TimeUtils;
import android.view.autofill.AutofillId;
import android.view.contentcapture.IContentCaptureDirectManager;
import android.view.contentcapture.ViewNode;
import android.view.inputmethod.BaseInputConnection;
import com.android.internal.os.IResultReceiver;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
/* loaded from: classes3.dex */
public final class MainContentCaptureSession extends ContentCaptureSession {
    public static final String EXTRA_BINDER = "binder";
    public static final String EXTRA_ENABLED_STATE = "enabled";
    private static final boolean FORCE_FLUSH = true;
    private static final int MSG_FLUSH = 1;
    private static final String TAG = MainContentCaptureSession.class.getSimpleName();
    private IBinder mApplicationToken;
    private ComponentName mComponentName;
    private final Context mContext;
    private IContentCaptureDirectManager mDirectServiceInterface;
    private IBinder.DeathRecipient mDirectServiceVulture;
    private ArrayList<ContentCaptureEvent> mEvents;
    private final LocalLog mFlushHistory;
    private final Handler mHandler;
    private final ContentCaptureManager mManager;
    private long mNextFlush;
    private final SessionStateReceiver mSessionStateReceiver;
    private IBinder mShareableActivityToken;
    private final IContentCaptureManager mSystemServerInterface;
    private final AtomicBoolean mDisabled = new AtomicBoolean(false);
    private int mState = 0;
    private boolean mNextFlushForTextChanged = false;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes3.dex */
    public static class SessionStateReceiver extends IResultReceiver.Stub {
        private final WeakReference<MainContentCaptureSession> mMainSession;

        SessionStateReceiver(MainContentCaptureSession session) {
            this.mMainSession = new WeakReference<>(session);
        }

        @Override // com.android.internal.os.IResultReceiver
        public void send(final int resultCode, Bundle resultData) {
            final IBinder binder;
            final MainContentCaptureSession mainSession = this.mMainSession.get();
            if (mainSession == null) {
                Log.w(MainContentCaptureSession.TAG, "received result after mina session released");
                return;
            }
            if (resultData != null) {
                boolean hasEnabled = resultData.getBoolean("enabled");
                if (hasEnabled) {
                    boolean disabled = resultCode == 2;
                    mainSession.mDisabled.set(disabled);
                    return;
                }
                binder = resultData.getBinder("binder");
                if (binder == null) {
                    Log.wtf(MainContentCaptureSession.TAG, "No binder extra result");
                    mainSession.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$SessionStateReceiver$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            MainContentCaptureSession.this.resetSession(260);
                        }
                    });
                    return;
                }
            } else {
                binder = null;
            }
            mainSession.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$SessionStateReceiver$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MainContentCaptureSession.this.onSessionStarted(resultCode, binder);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public MainContentCaptureSession(Context context, ContentCaptureManager manager, Handler handler, IContentCaptureManager systemServerInterface) {
        this.mContext = context;
        this.mManager = manager;
        this.mHandler = handler;
        this.mSystemServerInterface = systemServerInterface;
        int logHistorySize = manager.mOptions.logHistorySize;
        this.mFlushHistory = logHistorySize > 0 ? new LocalLog(logHistorySize) : null;
        this.mSessionStateReceiver = new SessionStateReceiver(this);
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    MainContentCaptureSession getMainCaptureSession() {
        return this;
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    ContentCaptureSession newChild(ContentCaptureContext clientContext) {
        ContentCaptureSession child = new ChildContentCaptureSession(this, clientContext);
        notifyChildSessionStarted(this.mId, child.mId, clientContext);
        return child;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void start(IBinder token, IBinder shareableActivityToken, ComponentName component, int flags) {
        if (isContentCaptureEnabled()) {
            if (ContentCaptureHelper.sVerbose) {
                Log.v(TAG, "start(): token=" + token + ", comp=" + ComponentName.flattenToShortString(component));
            }
            if (hasStarted()) {
                if (ContentCaptureHelper.sDebug) {
                    Log.d(TAG, "ignoring handleStartSession(" + token + "/" + ComponentName.flattenToShortString(component) + " while on state " + getStateAsString(this.mState));
                    return;
                }
                return;
            }
            this.mState = 1;
            this.mApplicationToken = token;
            this.mShareableActivityToken = shareableActivityToken;
            this.mComponentName = component;
            if (ContentCaptureHelper.sVerbose) {
                Log.v(TAG, "handleStartSession(): token=" + token + ", act=" + getDebugState() + ", id=" + this.mId);
            }
            try {
                this.mSystemServerInterface.startSession(this.mApplicationToken, this.mShareableActivityToken, component, this.mId, flags, this.mSessionStateReceiver);
            } catch (RemoteException e) {
                Log.w(TAG, "Error starting session for " + component.flattenToShortString() + ": " + e);
            }
        }
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    void onDestroy() {
        this.mHandler.removeMessages(1);
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5282x9b3ec380();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onDestroy$0$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5282x9b3ec380() {
        try {
            flush(4);
        } finally {
            destroySession();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSessionStarted(int resultCode, IBinder binder) {
        if (binder != null) {
            this.mDirectServiceInterface = IContentCaptureDirectManager.Stub.asInterface(binder);
            IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda6
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    MainContentCaptureSession.this.m5283xcc88ed4a();
                }
            };
            this.mDirectServiceVulture = deathRecipient;
            try {
                binder.linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to link to death on " + binder + ": " + e);
            }
        }
        if ((resultCode & 4) != 0) {
            resetSession(resultCode);
        } else {
            this.mState = resultCode;
            this.mDisabled.set(false);
            m5284xd6bc414(7);
        }
        if (ContentCaptureHelper.sVerbose) {
            String str = TAG;
            StringBuilder append = new StringBuilder().append("handleSessionStarted() result: id=").append(this.mId).append(" resultCode=").append(resultCode).append(", state=").append(getStateAsString(this.mState)).append(", disabled=").append(this.mDisabled.get()).append(", binder=").append(binder).append(", events=");
            ArrayList<ContentCaptureEvent> arrayList = this.mEvents;
            Log.v(str, append.append(arrayList != null ? arrayList.size() : 0).toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSessionStarted$1$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5283xcc88ed4a() {
        Log.w(TAG, "Keeping session " + this.mId + " when service died");
        this.mState = 1024;
        this.mDisabled.set(true);
    }

    private void sendEvent(ContentCaptureEvent event) {
        sendEvent(event, false);
    }

    private void sendEvent(ContentCaptureEvent event, boolean forceFlush) {
        int flushReason;
        int flushReason2;
        int eventType = event.getType();
        if (ContentCaptureHelper.sVerbose) {
            Log.v(TAG, "handleSendEvent(" + getDebugState() + "): " + event);
        }
        if (!hasStarted() && eventType != -1 && eventType != 6) {
            if (ContentCaptureHelper.sVerbose) {
                Log.v(TAG, "handleSendEvent(" + getDebugState() + ", " + ContentCaptureEvent.getTypeAsString(eventType) + "): dropping because session not started yet");
            }
        } else if (this.mDisabled.get()) {
            if (ContentCaptureHelper.sVerbose) {
                Log.v(TAG, "handleSendEvent(): ignoring when disabled");
            }
        } else {
            int maxBufferSize = this.mManager.mOptions.maxBufferSize;
            if (this.mEvents == null) {
                if (ContentCaptureHelper.sVerbose) {
                    Log.v(TAG, "handleSendEvent(): creating buffer for " + maxBufferSize + " events");
                }
                this.mEvents = new ArrayList<>(maxBufferSize);
            }
            boolean addEvent = true;
            if (eventType == 3) {
                CharSequence text = event.getText();
                boolean hasComposingSpan = event.hasComposingSpan();
                if (hasComposingSpan) {
                    ContentCaptureEvent lastEvent = null;
                    int index = this.mEvents.size() - 1;
                    while (true) {
                        if (index < 0) {
                            break;
                        }
                        ContentCaptureEvent tmpEvent = this.mEvents.get(index);
                        if (!event.getId().equals(tmpEvent.getId())) {
                            index--;
                        } else {
                            lastEvent = tmpEvent;
                            break;
                        }
                    }
                    if (lastEvent != null && lastEvent.hasComposingSpan()) {
                        CharSequence lastText = lastEvent.getText();
                        boolean bothNonEmpty = (TextUtils.isEmpty(lastText) || TextUtils.isEmpty(text)) ? false : true;
                        boolean equalContent = TextUtils.equals(lastText, text) && lastEvent.hasSameComposingSpan(event) && lastEvent.hasSameSelectionSpan(event);
                        if (equalContent) {
                            addEvent = false;
                        } else if (bothNonEmpty) {
                            lastEvent.mergeEvent(event);
                            addEvent = false;
                        }
                        if (!addEvent && ContentCaptureHelper.sVerbose) {
                            Log.v(TAG, "Buffering VIEW_TEXT_CHANGED event, updated text=" + ContentCaptureHelper.getSanitizedString(text));
                        }
                    }
                }
            }
            if (!this.mEvents.isEmpty() && eventType == 2) {
                ArrayList<ContentCaptureEvent> arrayList = this.mEvents;
                ContentCaptureEvent lastEvent2 = arrayList.get(arrayList.size() - 1);
                if (lastEvent2.getType() == 2 && event.getSessionId() == lastEvent2.getSessionId()) {
                    if (ContentCaptureHelper.sVerbose) {
                        Log.v(TAG, "Buffering TYPE_VIEW_DISAPPEARED events for session " + lastEvent2.getSessionId());
                    }
                    lastEvent2.mergeEvent(event);
                    addEvent = false;
                }
            }
            if (addEvent) {
                this.mEvents.add(event);
            }
            int numberEvents = this.mEvents.size();
            boolean bufferEvent = numberEvents < maxBufferSize;
            if (bufferEvent && !forceFlush) {
                if (eventType == 3) {
                    this.mNextFlushForTextChanged = true;
                    flushReason2 = 6;
                } else if (this.mNextFlushForTextChanged) {
                    if (ContentCaptureHelper.sVerbose) {
                        Log.i(TAG, "Not scheduling flush because next flush is for text changed");
                        return;
                    }
                    return;
                } else {
                    flushReason2 = 5;
                }
                scheduleFlush(flushReason2, true);
                return;
            }
            int flushReason3 = this.mState;
            if (flushReason3 != 2 && numberEvents >= maxBufferSize) {
                if (ContentCaptureHelper.sDebug) {
                    Log.d(TAG, "Closing session for " + getDebugState() + " after " + numberEvents + " delayed events");
                }
                resetSession(132);
                return;
            }
            switch (eventType) {
                case -2:
                    flushReason = 4;
                    break;
                case -1:
                    flushReason = 3;
                    break;
                default:
                    flushReason = 1;
                    break;
            }
            flush(flushReason);
        }
    }

    private boolean hasStarted() {
        return this.mState != 0;
    }

    private void scheduleFlush(final int reason, boolean checkExisting) {
        int flushFrequencyMs;
        if (ContentCaptureHelper.sVerbose) {
            Log.v(TAG, "handleScheduleFlush(" + getDebugState(reason) + ", checkExisting=" + checkExisting);
        }
        if (!hasStarted()) {
            if (ContentCaptureHelper.sVerbose) {
                Log.v(TAG, "handleScheduleFlush(): session not started yet");
            }
        } else if (this.mDisabled.get()) {
            String str = TAG;
            StringBuilder append = new StringBuilder().append("handleScheduleFlush(").append(getDebugState(reason)).append("): should not be called when disabled. events=");
            ArrayList<ContentCaptureEvent> arrayList = this.mEvents;
            Log.e(str, append.append(arrayList == null ? null : Integer.valueOf(arrayList.size())).toString());
        } else {
            if (checkExisting && this.mHandler.hasMessages(1)) {
                this.mHandler.removeMessages(1);
            }
            if (reason == 6) {
                flushFrequencyMs = this.mManager.mOptions.textChangeFlushingFrequencyMs;
            } else {
                if (reason != 5 && ContentCaptureHelper.sDebug) {
                    Log.d(TAG, "handleScheduleFlush(" + getDebugState(reason) + "): not a timeout reason because mDirectServiceInterface is not ready yet");
                }
                flushFrequencyMs = this.mManager.mOptions.idleFlushingFrequencyMs;
            }
            this.mNextFlush = System.currentTimeMillis() + flushFrequencyMs;
            if (ContentCaptureHelper.sVerbose) {
                Log.v(TAG, "handleScheduleFlush(): scheduled to flush in " + flushFrequencyMs + "ms: " + TimeUtils.logTimeOfDay(this.mNextFlush));
            }
            this.mHandler.postDelayed(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    MainContentCaptureSession.this.m5284xd6bc414(reason);
                }
            }, 1, flushFrequencyMs);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: flushIfNeeded */
    public void m5284xd6bc414(int reason) {
        ArrayList<ContentCaptureEvent> arrayList = this.mEvents;
        if (arrayList == null || arrayList.isEmpty()) {
            if (ContentCaptureHelper.sVerbose) {
                Log.v(TAG, "Nothing to flush");
                return;
            }
            return;
        }
        flush(reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // android.view.contentcapture.ContentCaptureSession
    public void flush(int reason) {
        if (this.mEvents == null) {
            return;
        }
        if (this.mDisabled.get()) {
            Log.e(TAG, "handleForceFlush(" + getDebugState(reason) + "): should not be when disabled");
        } else if (this.mDirectServiceInterface == null) {
            if (ContentCaptureHelper.sVerbose) {
                Log.v(TAG, "handleForceFlush(" + getDebugState(reason) + "): hold your horses, client not ready: " + this.mEvents);
            }
            if (!this.mHandler.hasMessages(1)) {
                scheduleFlush(reason, false);
            }
        } else {
            this.mNextFlushForTextChanged = false;
            int numberEvents = this.mEvents.size();
            String reasonString = getFlushReasonAsString(reason);
            if (ContentCaptureHelper.sDebug) {
                Log.d(TAG, "Flushing " + numberEvents + " event(s) for " + getDebugState(reason));
            }
            if (this.mFlushHistory != null) {
                String logRecord = "r=" + reasonString + " s=" + numberEvents + " m=" + this.mManager.mOptions.maxBufferSize + " i=" + this.mManager.mOptions.idleFlushingFrequencyMs;
                this.mFlushHistory.log(logRecord);
            }
            try {
                this.mHandler.removeMessages(1);
                ParceledListSlice<ContentCaptureEvent> events = clearEvents();
                this.mDirectServiceInterface.sendEvents(events, reason, this.mManager.mOptions);
            } catch (RemoteException e) {
                Log.w(TAG, "Error sending " + numberEvents + " for " + getDebugState() + ": " + e);
            }
        }
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    public void updateContentCaptureContext(ContentCaptureContext context) {
        notifyContextUpdated(this.mId, context);
    }

    private ParceledListSlice<ContentCaptureEvent> clearEvents() {
        if (this.mEvents == null) {
            return new ParceledListSlice<>(Collections.EMPTY_LIST);
        }
        List<ContentCaptureEvent> events = new ArrayList<>(this.mEvents);
        this.mEvents.clear();
        return new ParceledListSlice<>(events);
    }

    private void destroySession() {
        if (ContentCaptureHelper.sDebug) {
            String str = TAG;
            StringBuilder append = new StringBuilder().append("Destroying session (ctx=").append(this.mContext).append(", id=").append(this.mId).append(") with ");
            ArrayList<ContentCaptureEvent> arrayList = this.mEvents;
            Log.d(str, append.append(arrayList == null ? 0 : arrayList.size()).append(" event(s) for ").append(getDebugState()).toString());
        }
        try {
            this.mSystemServerInterface.finishSession(this.mId);
        } catch (RemoteException e) {
            Log.e(TAG, "Error destroying system-service session " + this.mId + " for " + getDebugState() + ": " + e);
        }
        IContentCaptureDirectManager iContentCaptureDirectManager = this.mDirectServiceInterface;
        if (iContentCaptureDirectManager != null) {
            iContentCaptureDirectManager.asBinder().unlinkToDeath(this.mDirectServiceVulture, 0);
        }
        this.mDirectServiceInterface = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetSession(int newState) {
        if (ContentCaptureHelper.sVerbose) {
            Log.v(TAG, "handleResetSession(" + getActivityName() + "): from " + getStateAsString(this.mState) + " to " + getStateAsString(newState));
        }
        this.mState = newState;
        this.mDisabled.set((newState & 4) != 0);
        this.mApplicationToken = null;
        this.mShareableActivityToken = null;
        this.mComponentName = null;
        this.mEvents = null;
        IContentCaptureDirectManager iContentCaptureDirectManager = this.mDirectServiceInterface;
        if (iContentCaptureDirectManager != null) {
            iContentCaptureDirectManager.asBinder().unlinkToDeath(this.mDirectServiceVulture, 0);
        }
        this.mDirectServiceInterface = null;
        this.mHandler.removeMessages(1);
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    void internalNotifyViewAppeared(ViewNode.ViewStructureImpl node) {
        notifyViewAppeared(this.mId, node);
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    void internalNotifyViewDisappeared(AutofillId id) {
        notifyViewDisappeared(this.mId, id);
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    void internalNotifyViewTextChanged(AutofillId id, CharSequence text) {
        notifyViewTextChanged(this.mId, id, text);
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    void internalNotifyViewInsetsChanged(Insets viewInsets) {
        notifyViewInsetsChanged(this.mId, viewInsets);
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    public void internalNotifyViewTreeEvent(boolean started) {
        notifyViewTreeEvent(this.mId, started);
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    public void internalNotifySessionResumed() {
        notifySessionResumed(this.mId);
    }

    @Override // android.view.contentcapture.ContentCaptureSession
    public void internalNotifySessionPaused() {
        notifySessionPaused(this.mId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // android.view.contentcapture.ContentCaptureSession
    public boolean isContentCaptureEnabled() {
        return super.isContentCaptureEnabled() && this.mManager.isContentCaptureEnabled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDisabled() {
        return this.mDisabled.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setDisabled(boolean disabled) {
        return this.mDisabled.compareAndSet(!disabled, disabled);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyChildSessionStarted$3$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5272x9f4a2a66(int childSessionId, int parentSessionId, ContentCaptureContext clientContext) {
        sendEvent(new ContentCaptureEvent(childSessionId, -1).setParentSessionId(parentSessionId).setClientContext(clientContext), true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyChildSessionStarted(final int parentSessionId, final int childSessionId, final ContentCaptureContext clientContext) {
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5272x9f4a2a66(childSessionId, parentSessionId, clientContext);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyChildSessionFinished$4$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5271x4a58c458(int childSessionId, int parentSessionId) {
        sendEvent(new ContentCaptureEvent(childSessionId, -2).setParentSessionId(parentSessionId), true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyChildSessionFinished(final int parentSessionId, final int childSessionId) {
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5271x4a58c458(childSessionId, parentSessionId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyViewAppeared$5$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5276xe902eee6(int sessionId, ViewNode.ViewStructureImpl node) {
        sendEvent(new ContentCaptureEvent(sessionId, 1).setViewNode(node.mNode));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyViewAppeared(final int sessionId, final ViewNode.ViewStructureImpl node) {
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5276xe902eee6(sessionId, node);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyViewDisappeared$6$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5277xa71e8f7f(int sessionId, AutofillId id) {
        sendEvent(new ContentCaptureEvent(sessionId, 2).setAutofillId(id));
    }

    public void notifyViewDisappeared(final int sessionId, final AutofillId id) {
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5277xa71e8f7f(sessionId, id);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyViewTextChanged(final int sessionId, final AutofillId id, CharSequence text) {
        int composingStart;
        int composingEnd;
        final CharSequence eventText = stringOrSpannedStringWithoutNoCopySpans(text);
        if (text instanceof Spannable) {
            int composingStart2 = BaseInputConnection.getComposingSpanStart((Spannable) text);
            composingStart = composingStart2;
            composingEnd = BaseInputConnection.getComposingSpanEnd((Spannable) text);
        } else {
            composingStart = -1;
            composingEnd = -1;
        }
        final int startIndex = Selection.getSelectionStart(text);
        final int endIndex = Selection.getSelectionEnd(text);
        final int i = composingStart;
        final int i2 = composingEnd;
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5279x41a6b05(sessionId, id, eventText, i, i2, startIndex, endIndex);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyViewTextChanged$7$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5279x41a6b05(int sessionId, AutofillId id, CharSequence eventText, int composingStart, int composingEnd, int startIndex, int endIndex) {
        sendEvent(new ContentCaptureEvent(sessionId, 3).setAutofillId(id).setText(eventText).setComposingIndex(composingStart, composingEnd).setSelectionIndex(startIndex, endIndex));
    }

    private CharSequence stringOrSpannedStringWithoutNoCopySpans(CharSequence source) {
        if (source == null) {
            return null;
        }
        if (source instanceof Spanned) {
            return new SpannableString(source, true);
        }
        return source.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyViewInsetsChanged$8$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5278xf4241d(int sessionId, Insets viewInsets) {
        sendEvent(new ContentCaptureEvent(sessionId, 9).setInsets(viewInsets));
    }

    public void notifyViewInsetsChanged(final int sessionId, final Insets viewInsets) {
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5278xf4241d(sessionId, viewInsets);
            }
        });
    }

    public void notifyViewTreeEvent(final int sessionId, boolean started) {
        final int type = started ? 4 : 5;
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5280xc78149c(sessionId, type);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyViewTreeEvent$9$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5280xc78149c(int sessionId, int type) {
        sendEvent(new ContentCaptureEvent(sessionId, type), true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifySessionResumed$10$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5275xb0350592(int sessionId) {
        sendEvent(new ContentCaptureEvent(sessionId, 7), true);
    }

    void notifySessionResumed(final int sessionId) {
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5275xb0350592(sessionId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifySessionPaused$11$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5274x3828e73c(int sessionId) {
        sendEvent(new ContentCaptureEvent(sessionId, 8), true);
    }

    void notifySessionPaused(final int sessionId) {
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5274x3828e73c(sessionId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyContextUpdated$12$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5273xe96e50c9(int sessionId, ContentCaptureContext context) {
        sendEvent(new ContentCaptureEvent(sessionId, 6).setClientContext(context), true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyContextUpdated(final int sessionId, final ContentCaptureContext context) {
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5273xe96e50c9(sessionId, context);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyWindowBoundsChanged$13$android-view-contentcapture-MainContentCaptureSession  reason: not valid java name */
    public /* synthetic */ void m5281x94070ad3(int sessionId, Rect bounds) {
        sendEvent(new ContentCaptureEvent(sessionId, 10).setBounds(bounds));
    }

    public void notifyWindowBoundsChanged(final int sessionId, final Rect bounds) {
        this.mHandler.post(new Runnable() { // from class: android.view.contentcapture.MainContentCaptureSession$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MainContentCaptureSession.this.m5281x94070ad3(sessionId, bounds);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // android.view.contentcapture.ContentCaptureSession
    public void dump(String prefix, PrintWriter pw) {
        super.dump(prefix, pw);
        pw.print(prefix);
        pw.print("mContext: ");
        pw.println(this.mContext);
        pw.print(prefix);
        pw.print("user: ");
        pw.println(this.mContext.getUserId());
        if (this.mDirectServiceInterface != null) {
            pw.print(prefix);
            pw.print("mDirectServiceInterface: ");
            pw.println(this.mDirectServiceInterface);
        }
        pw.print(prefix);
        pw.print("mDisabled: ");
        pw.println(this.mDisabled.get());
        pw.print(prefix);
        pw.print("isEnabled(): ");
        pw.println(isContentCaptureEnabled());
        pw.print(prefix);
        pw.print("state: ");
        pw.println(getStateAsString(this.mState));
        if (this.mApplicationToken != null) {
            pw.print(prefix);
            pw.print("app token: ");
            pw.println(this.mApplicationToken);
        }
        if (this.mShareableActivityToken != null) {
            pw.print(prefix);
            pw.print("sharable activity token: ");
            pw.println(this.mShareableActivityToken);
        }
        if (this.mComponentName != null) {
            pw.print(prefix);
            pw.print("component name: ");
            pw.println(this.mComponentName.flattenToShortString());
        }
        ArrayList<ContentCaptureEvent> arrayList = this.mEvents;
        if (arrayList != null && !arrayList.isEmpty()) {
            int numberEvents = this.mEvents.size();
            pw.print(prefix);
            pw.print("buffered events: ");
            pw.print(numberEvents);
            pw.print('/');
            pw.println(this.mManager.mOptions.maxBufferSize);
            if (ContentCaptureHelper.sVerbose && numberEvents > 0) {
                String prefix3 = prefix + "  ";
                for (int i = 0; i < numberEvents; i++) {
                    ContentCaptureEvent event = this.mEvents.get(i);
                    pw.print(prefix3);
                    pw.print(i);
                    pw.print(": ");
                    event.dump(pw);
                    pw.println();
                }
            }
            pw.print(prefix);
            pw.print("mNextFlushForTextChanged: ");
            pw.println(this.mNextFlushForTextChanged);
            pw.print(prefix);
            pw.print("flush frequency: ");
            if (this.mNextFlushForTextChanged) {
                pw.println(this.mManager.mOptions.textChangeFlushingFrequencyMs);
            } else {
                pw.println(this.mManager.mOptions.idleFlushingFrequencyMs);
            }
            pw.print(prefix);
            pw.print("next flush: ");
            TimeUtils.formatDuration(this.mNextFlush - System.currentTimeMillis(), pw);
            pw.print(" (");
            pw.print(TimeUtils.logTimeOfDay(this.mNextFlush));
            pw.println(NavigationBarInflaterView.KEY_CODE_END);
        }
        if (this.mFlushHistory != null) {
            pw.print(prefix);
            pw.println("flush history:");
            this.mFlushHistory.reverseDump(null, pw, null);
            pw.println();
        } else {
            pw.print(prefix);
            pw.println("not logging flush history");
        }
        super.dump(prefix, pw);
    }

    private String getActivityName() {
        if (this.mComponentName == null) {
            return "pkg:" + this.mContext.getPackageName();
        }
        return "act:" + this.mComponentName.flattenToShortString();
    }

    private String getDebugState() {
        return getActivityName() + " [state=" + getStateAsString(this.mState) + ", disabled=" + this.mDisabled.get() + NavigationBarInflaterView.SIZE_MOD_END;
    }

    private String getDebugState(int reason) {
        return getDebugState() + ", reason=" + getFlushReasonAsString(reason);
    }
}
