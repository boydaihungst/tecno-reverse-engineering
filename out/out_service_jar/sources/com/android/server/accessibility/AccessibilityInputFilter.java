package com.android.server.accessibility;

import android.content.Context;
import android.graphics.Region;
import android.os.PowerManager;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.InputEvent;
import android.view.InputFilter;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import com.android.server.LocalServices;
import com.android.server.accessibility.gestures.TouchExplorer;
import com.android.server.accessibility.magnification.FullScreenMagnificationGestureHandler;
import com.android.server.accessibility.magnification.MagnificationGestureHandler;
import com.android.server.accessibility.magnification.WindowMagnificationGestureHandler;
import com.android.server.accessibility.magnification.WindowMagnificationPromptController;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.usb.descriptors.UsbACInterface;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringJoiner;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AccessibilityInputFilter extends InputFilter implements EventStreamTransformation {
    private static final boolean DEBUG = false;
    static final int FEATURES_AFFECTING_MOTION_EVENTS = 987;
    static final int FLAG_FEATURE_AUTOCLICK = 8;
    static final int FLAG_FEATURE_CONTROL_SCREEN_MAGNIFIER = 32;
    static final int FLAG_FEATURE_FILTER_KEY_EVENTS = 4;
    static final int FLAG_FEATURE_INJECT_MOTION_EVENTS = 16;
    static final int FLAG_FEATURE_SCREEN_MAGNIFIER = 1;
    static final int FLAG_FEATURE_TOUCH_EXPLORATION = 2;
    static final int FLAG_FEATURE_TRIGGERED_SCREEN_MAGNIFIER = 64;
    static final int FLAG_REQUEST_2_FINGER_PASSTHROUGH = 512;
    static final int FLAG_REQUEST_MULTI_FINGER_GESTURES = 256;
    static final int FLAG_SEND_MOTION_EVENTS = 1024;
    static final int FLAG_SERVICE_HANDLES_DOUBLE_TAP = 128;
    private static final String TAG = AccessibilityInputFilter.class.getSimpleName();
    private final AccessibilityManagerService mAms;
    private AutoclickController mAutoclickController;
    private final Context mContext;
    private int mEnabledFeatures;
    private final SparseArray<EventStreamTransformation> mEventHandler;
    private boolean mInstalled;
    private KeyboardInterceptor mKeyboardInterceptor;
    private EventStreamState mKeyboardStreamState;
    private final SparseArray<MagnificationGestureHandler> mMagnificationGestureHandler;
    private final SparseArray<MotionEventInjector> mMotionEventInjectors;
    private final SparseArray<EventStreamState> mMouseStreamStates;
    private final PowerManager mPm;
    private final SparseArray<TouchExplorer> mTouchExplorer;
    private final SparseArray<EventStreamState> mTouchScreenStreamStates;
    private int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AccessibilityInputFilter(Context context, AccessibilityManagerService service) {
        this(context, service, new SparseArray(0));
    }

    AccessibilityInputFilter(Context context, AccessibilityManagerService service, SparseArray<EventStreamTransformation> eventHandler) {
        super(context.getMainLooper());
        this.mTouchExplorer = new SparseArray<>(0);
        this.mMagnificationGestureHandler = new SparseArray<>(0);
        this.mMotionEventInjectors = new SparseArray<>(0);
        this.mMouseStreamStates = new SparseArray<>(0);
        this.mTouchScreenStreamStates = new SparseArray<>(0);
        this.mContext = context;
        this.mAms = service;
        this.mPm = (PowerManager) context.getSystemService("power");
        this.mEventHandler = eventHandler;
    }

    public void onInstalled() {
        this.mInstalled = true;
        disableFeatures();
        enableFeatures();
        super.onInstalled();
    }

    public void onUninstalled() {
        this.mInstalled = false;
        disableFeatures();
        super.onUninstalled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayAdded(Display display) {
        if (this.mInstalled) {
            resetStreamStateForDisplay(display.getDisplayId());
            enableFeaturesForDisplay(display);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayRemoved(int displayId) {
        if (this.mInstalled) {
            disableFeaturesForDisplay(displayId);
            resetStreamStateForDisplay(displayId);
        }
    }

    public void onInputEvent(InputEvent event, int policyFlags) {
        if (this.mAms.getTraceManager().isA11yTracingEnabledForTypes(4096L)) {
            this.mAms.getTraceManager().logTrace(TAG + ".onInputEvent", 4096L, "event=" + event + ";policyFlags=" + policyFlags);
        }
        if (this.mEventHandler.size() == 0) {
            super.onInputEvent(event, policyFlags);
            return;
        }
        EventStreamState state = getEventStreamState(event);
        if (state == null) {
            super.onInputEvent(event, policyFlags);
            return;
        }
        int eventSource = event.getSource();
        int displayId = event.getDisplayId();
        if ((1073741824 & policyFlags) == 0) {
            state.reset();
            clearEventStreamHandler(displayId, eventSource);
            super.onInputEvent(event, policyFlags);
            return;
        }
        if (state.updateInputSource(event.getSource())) {
            clearEventStreamHandler(displayId, eventSource);
        }
        if (!state.inputSourceValid()) {
            super.onInputEvent(event, policyFlags);
        } else if (event instanceof MotionEvent) {
            if ((this.mEnabledFeatures & FEATURES_AFFECTING_MOTION_EVENTS) != 0) {
                MotionEvent motionEvent = (MotionEvent) event;
                processMotionEvent(state, motionEvent, policyFlags);
                return;
            }
            super.onInputEvent(event, policyFlags);
        } else if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            processKeyEvent(state, keyEvent, policyFlags);
        }
    }

    private EventStreamState getEventStreamState(InputEvent event) {
        if (event instanceof MotionEvent) {
            int displayId = event.getDisplayId();
            if (event.isFromSource(UsbACInterface.FORMAT_II_AC3)) {
                EventStreamState touchScreenStreamState = this.mTouchScreenStreamStates.get(displayId);
                if (touchScreenStreamState == null) {
                    EventStreamState touchScreenStreamState2 = new TouchScreenEventStreamState();
                    this.mTouchScreenStreamStates.put(displayId, touchScreenStreamState2);
                    return touchScreenStreamState2;
                }
                return touchScreenStreamState;
            } else if (event.isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
                EventStreamState mouseStreamState = this.mMouseStreamStates.get(displayId);
                if (mouseStreamState == null) {
                    EventStreamState mouseStreamState2 = new MouseEventStreamState();
                    this.mMouseStreamStates.put(displayId, mouseStreamState2);
                    return mouseStreamState2;
                }
                return mouseStreamState;
            } else {
                return null;
            }
        } else if ((event instanceof KeyEvent) && event.isFromSource(257)) {
            if (this.mKeyboardStreamState == null) {
                this.mKeyboardStreamState = new KeyboardEventStreamState();
            }
            return this.mKeyboardStreamState;
        } else {
            return null;
        }
    }

    private void clearEventStreamHandler(int displayId, int eventSource) {
        EventStreamTransformation eventHandler = this.mEventHandler.get(displayId);
        if (eventHandler != null) {
            eventHandler.clearEvents(eventSource);
        }
    }

    private void processMotionEvent(EventStreamState state, MotionEvent event, int policyFlags) {
        if (!state.shouldProcessScroll() && event.getActionMasked() == 8) {
            super.onInputEvent(event, policyFlags);
        } else if (!state.shouldProcessMotionEvent(event)) {
        } else {
            try {
                handleMotionEvent(event, policyFlags);
            } catch (Exception e) {
                Slog.i(TAG, " handleMotionEvent exception:", e);
            }
        }
    }

    private void processKeyEvent(EventStreamState state, KeyEvent event, int policyFlags) {
        if (!state.shouldProcessKeyEvent(event)) {
            super.onInputEvent(event, policyFlags);
        } else {
            this.mEventHandler.get(0).onKeyEvent(event, policyFlags);
        }
    }

    private void handleMotionEvent(MotionEvent event, int policyFlags) {
        this.mPm.userActivity(event.getEventTime(), false);
        MotionEvent transformedEvent = MotionEvent.obtain(event);
        int displayId = event.getDisplayId();
        this.mEventHandler.get(isDisplayIdValid(displayId) ? displayId : 0).onMotionEvent(transformedEvent, event, policyFlags);
        transformedEvent.recycle();
    }

    private boolean isDisplayIdValid(int displayId) {
        return this.mEventHandler.get(displayId) != null;
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void onMotionEvent(MotionEvent transformedEvent, MotionEvent rawEvent, int policyFlags) {
        sendInputEvent(transformedEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void onKeyEvent(KeyEvent event, int policyFlags) {
        sendInputEvent(event, policyFlags);
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void setNext(EventStreamTransformation sink) {
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public EventStreamTransformation getNext() {
        return null;
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void clearEvents(int inputSource) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUserAndEnabledFeatures(int userId, int enabledFeatures) {
        if (this.mEnabledFeatures == enabledFeatures && this.mUserId == userId) {
            return;
        }
        if (this.mInstalled) {
            disableFeatures();
        }
        this.mUserId = userId;
        this.mEnabledFeatures = enabledFeatures;
        if (this.mInstalled) {
            enableFeatures();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyAccessibilityEvent(AccessibilityEvent event) {
        for (int i = 0; i < this.mEventHandler.size(); i++) {
            EventStreamTransformation eventHandler = this.mEventHandler.valueAt(i);
            if (eventHandler != null) {
                eventHandler.onAccessibilityEvent(event);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyAccessibilityButtonClicked(int displayId) {
        MagnificationGestureHandler handler;
        if (this.mMagnificationGestureHandler.size() != 0 && (handler = this.mMagnificationGestureHandler.get(displayId)) != null) {
            handler.notifyShortcutTriggered();
        }
    }

    private void enableFeatures() {
        resetAllStreamState();
        ArrayList<Display> displaysList = this.mAms.getValidDisplayList();
        for (int i = displaysList.size() - 1; i >= 0; i--) {
            enableFeaturesForDisplay(displaysList.get(i));
        }
        enableDisplayIndependentFeatures();
    }

    private void enableFeaturesForDisplay(Display display) {
        Context displayContext = this.mContext.createDisplayContext(display);
        int displayId = display.getDisplayId();
        if ((this.mEnabledFeatures & 8) != 0) {
            if (this.mAutoclickController == null) {
                this.mAutoclickController = new AutoclickController(this.mContext, this.mUserId, this.mAms.getTraceManager());
            }
            addFirstEventHandler(displayId, this.mAutoclickController);
        }
        if ((this.mEnabledFeatures & 2) != 0) {
            TouchExplorer explorer = new TouchExplorer(displayContext, this.mAms);
            if ((this.mEnabledFeatures & 128) != 0) {
                explorer.setServiceHandlesDoubleTap(true);
            }
            if ((this.mEnabledFeatures & 256) != 0) {
                explorer.setMultiFingerGesturesEnabled(true);
            }
            if ((this.mEnabledFeatures & 512) != 0) {
                explorer.setTwoFingerPassthroughEnabled(true);
            }
            if ((this.mEnabledFeatures & 1024) != 0) {
                explorer.setSendMotionEventsEnabled(true);
            }
            addFirstEventHandler(displayId, explorer);
            this.mTouchExplorer.put(displayId, explorer);
        }
        int i = this.mEnabledFeatures;
        if ((i & 32) != 0 || (i & 1) != 0 || (i & 64) != 0) {
            MagnificationGestureHandler magnificationGestureHandler = createMagnificationGestureHandler(displayId, displayContext);
            addFirstEventHandler(displayId, magnificationGestureHandler);
            this.mMagnificationGestureHandler.put(displayId, magnificationGestureHandler);
        }
        if ((this.mEnabledFeatures & 16) != 0) {
            MotionEventInjector injector = new MotionEventInjector(this.mContext.getMainLooper(), this.mAms.getTraceManager());
            addFirstEventHandler(displayId, injector);
            this.mMotionEventInjectors.put(displayId, injector);
        }
    }

    private void enableDisplayIndependentFeatures() {
        if ((this.mEnabledFeatures & 16) != 0) {
            this.mAms.setMotionEventInjectors(this.mMotionEventInjectors);
        }
        if ((this.mEnabledFeatures & 4) != 0) {
            KeyboardInterceptor keyboardInterceptor = new KeyboardInterceptor(this.mAms, (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class));
            this.mKeyboardInterceptor = keyboardInterceptor;
            addFirstEventHandler(0, keyboardInterceptor);
        }
    }

    private void addFirstEventHandler(int displayId, EventStreamTransformation handler) {
        EventStreamTransformation eventHandler = this.mEventHandler.get(displayId);
        if (eventHandler != null) {
            handler.setNext(eventHandler);
        } else {
            handler.setNext(this);
        }
        this.mEventHandler.put(displayId, handler);
    }

    private void disableFeatures() {
        ArrayList<Display> displaysList = this.mAms.getValidDisplayList();
        for (int i = displaysList.size() - 1; i >= 0; i--) {
            disableFeaturesForDisplay(displaysList.get(i).getDisplayId());
        }
        this.mAms.setMotionEventInjectors(null);
        disableDisplayIndependentFeatures();
        resetAllStreamState();
    }

    private void disableFeaturesForDisplay(int displayId) {
        MotionEventInjector injector = this.mMotionEventInjectors.get(displayId);
        if (injector != null) {
            injector.onDestroy();
            this.mMotionEventInjectors.remove(displayId);
        }
        TouchExplorer explorer = this.mTouchExplorer.get(displayId);
        if (explorer != null) {
            explorer.onDestroy();
            this.mTouchExplorer.remove(displayId);
        }
        MagnificationGestureHandler handler = this.mMagnificationGestureHandler.get(displayId);
        if (handler != null) {
            handler.onDestroy();
            this.mMagnificationGestureHandler.remove(displayId);
        }
        EventStreamTransformation eventStreamTransformation = this.mEventHandler.get(displayId);
        if (eventStreamTransformation != null) {
            this.mEventHandler.remove(displayId);
        }
    }

    private void disableDisplayIndependentFeatures() {
        AutoclickController autoclickController = this.mAutoclickController;
        if (autoclickController != null) {
            autoclickController.onDestroy();
            this.mAutoclickController = null;
        }
        KeyboardInterceptor keyboardInterceptor = this.mKeyboardInterceptor;
        if (keyboardInterceptor != null) {
            keyboardInterceptor.onDestroy();
            this.mKeyboardInterceptor = null;
        }
    }

    private MagnificationGestureHandler createMagnificationGestureHandler(int displayId, Context displayContext) {
        int i = this.mEnabledFeatures;
        boolean detectControlGestures = (i & 1) != 0;
        boolean triggerable = (i & 64) != 0;
        if (this.mAms.getMagnificationMode(displayId) == 2) {
            Context uiContext = displayContext.createWindowContext(2039, null);
            MagnificationGestureHandler magnificationGestureHandler = new WindowMagnificationGestureHandler(uiContext, this.mAms.getWindowMagnificationMgr(), this.mAms.getTraceManager(), this.mAms.getMagnificationController(), detectControlGestures, triggerable, displayId);
            return magnificationGestureHandler;
        }
        Context uiContext2 = displayContext.createWindowContext(2027, null);
        MagnificationGestureHandler magnificationGestureHandler2 = new FullScreenMagnificationGestureHandler(uiContext2, this.mAms.getMagnificationController().getFullScreenMagnificationController(), this.mAms.getTraceManager(), this.mAms.getMagnificationController(), detectControlGestures, triggerable, new WindowMagnificationPromptController(displayContext, this.mUserId), displayId);
        return magnificationGestureHandler2;
    }

    void resetAllStreamState() {
        ArrayList<Display> displaysList = this.mAms.getValidDisplayList();
        for (int i = displaysList.size() - 1; i >= 0; i--) {
            resetStreamStateForDisplay(displaysList.get(i).getDisplayId());
        }
        EventStreamState eventStreamState = this.mKeyboardStreamState;
        if (eventStreamState != null) {
            eventStreamState.reset();
        }
    }

    void resetStreamStateForDisplay(int displayId) {
        EventStreamState touchScreenStreamState = this.mTouchScreenStreamStates.get(displayId);
        if (touchScreenStreamState != null) {
            touchScreenStreamState.reset();
            this.mTouchScreenStreamStates.remove(displayId);
        }
        EventStreamState mouseStreamState = this.mMouseStreamStates.get(displayId);
        if (mouseStreamState != null) {
            mouseStreamState.reset();
            this.mMouseStreamStates.remove(displayId);
        }
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void onDestroy() {
    }

    public void refreshMagnificationMode(Display display) {
        int displayId = display.getDisplayId();
        MagnificationGestureHandler magnificationGestureHandler = this.mMagnificationGestureHandler.get(displayId);
        if (magnificationGestureHandler == null || magnificationGestureHandler.getMode() == this.mAms.getMagnificationMode(displayId)) {
            return;
        }
        magnificationGestureHandler.onDestroy();
        MagnificationGestureHandler currentMagnificationGestureHandler = createMagnificationGestureHandler(displayId, this.mContext.createDisplayContext(display));
        switchEventStreamTransformation(displayId, magnificationGestureHandler, currentMagnificationGestureHandler);
        this.mMagnificationGestureHandler.put(displayId, currentMagnificationGestureHandler);
    }

    private void switchEventStreamTransformation(int displayId, EventStreamTransformation oldStreamTransformation, EventStreamTransformation currentStreamTransformation) {
        EventStreamTransformation eventStreamTransformation = this.mEventHandler.get(displayId);
        if (eventStreamTransformation == null) {
            return;
        }
        if (eventStreamTransformation == oldStreamTransformation) {
            currentStreamTransformation.setNext(oldStreamTransformation.getNext());
            this.mEventHandler.put(displayId, currentStreamTransformation);
            return;
        }
        while (eventStreamTransformation != null) {
            if (eventStreamTransformation.getNext() == oldStreamTransformation) {
                eventStreamTransformation.setNext(currentStreamTransformation);
                currentStreamTransformation.setNext(oldStreamTransformation.getNext());
                return;
            }
            eventStreamTransformation = eventStreamTransformation.getNext();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class EventStreamState {
        private int mSource = -1;

        EventStreamState() {
        }

        public boolean updateInputSource(int source) {
            if (this.mSource == source) {
                return false;
            }
            reset();
            this.mSource = source;
            return true;
        }

        public boolean inputSourceValid() {
            return this.mSource >= 0;
        }

        public void reset() {
            this.mSource = -1;
        }

        public boolean shouldProcessScroll() {
            return false;
        }

        public boolean shouldProcessMotionEvent(MotionEvent event) {
            return false;
        }

        public boolean shouldProcessKeyEvent(KeyEvent event) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MouseEventStreamState extends EventStreamState {
        private boolean mMotionSequenceStarted;

        public MouseEventStreamState() {
            reset();
        }

        @Override // com.android.server.accessibility.AccessibilityInputFilter.EventStreamState
        public final void reset() {
            super.reset();
            this.mMotionSequenceStarted = false;
        }

        @Override // com.android.server.accessibility.AccessibilityInputFilter.EventStreamState
        public final boolean shouldProcessScroll() {
            return true;
        }

        @Override // com.android.server.accessibility.AccessibilityInputFilter.EventStreamState
        public final boolean shouldProcessMotionEvent(MotionEvent event) {
            boolean z = true;
            if (this.mMotionSequenceStarted) {
                return true;
            }
            int action = event.getActionMasked();
            if (action != 0 && action != 7) {
                z = false;
            }
            this.mMotionSequenceStarted = z;
            return z;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class TouchScreenEventStreamState extends EventStreamState {
        private boolean mHoverSequenceStarted;
        private boolean mTouchSequenceStarted;

        public TouchScreenEventStreamState() {
            reset();
        }

        @Override // com.android.server.accessibility.AccessibilityInputFilter.EventStreamState
        public final void reset() {
            super.reset();
            this.mTouchSequenceStarted = false;
            this.mHoverSequenceStarted = false;
        }

        @Override // com.android.server.accessibility.AccessibilityInputFilter.EventStreamState
        public final boolean shouldProcessMotionEvent(MotionEvent event) {
            boolean z;
            if (event.isTouchEvent()) {
                if (this.mTouchSequenceStarted) {
                    return true;
                }
                z = event.getActionMasked() == 0;
                this.mTouchSequenceStarted = z;
                return z;
            } else if (this.mHoverSequenceStarted) {
                return true;
            } else {
                z = event.getActionMasked() == 9;
                this.mHoverSequenceStarted = z;
                return z;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class KeyboardEventStreamState extends EventStreamState {
        private SparseBooleanArray mEventSequenceStartedMap = new SparseBooleanArray();

        public KeyboardEventStreamState() {
            reset();
        }

        @Override // com.android.server.accessibility.AccessibilityInputFilter.EventStreamState
        public final void reset() {
            super.reset();
            this.mEventSequenceStartedMap.clear();
        }

        @Override // com.android.server.accessibility.AccessibilityInputFilter.EventStreamState
        public boolean updateInputSource(int deviceId) {
            return false;
        }

        @Override // com.android.server.accessibility.AccessibilityInputFilter.EventStreamState
        public boolean inputSourceValid() {
            return true;
        }

        @Override // com.android.server.accessibility.AccessibilityInputFilter.EventStreamState
        public final boolean shouldProcessKeyEvent(KeyEvent event) {
            int deviceId = event.getDeviceId();
            if (this.mEventSequenceStartedMap.get(deviceId, false)) {
                return true;
            }
            boolean shouldProcess = event.getAction() == 0;
            this.mEventSequenceStartedMap.put(deviceId, shouldProcess);
            return shouldProcess;
        }
    }

    public void setGestureDetectionPassthroughRegion(int displayId, Region region) {
        if (region != null && this.mTouchExplorer.contains(displayId)) {
            this.mTouchExplorer.get(displayId).setGestureDetectionPassthroughRegion(region);
        }
    }

    public void setTouchExplorationPassthroughRegion(int displayId, Region region) {
        if (region != null && this.mTouchExplorer.contains(displayId)) {
            this.mTouchExplorer.get(displayId).setTouchExplorationPassthroughRegion(region);
        }
    }

    public void setServiceDetectsGesturesEnabled(int displayId, boolean mode) {
        if (this.mTouchExplorer.contains(displayId)) {
            this.mTouchExplorer.get(displayId).setServiceDetectsGestures(mode);
        }
    }

    public void requestTouchExploration(int displayId) {
        if (this.mTouchExplorer.contains(displayId)) {
            this.mTouchExplorer.get(displayId).requestTouchExploration();
        }
    }

    public void requestDragging(int displayId, int pointerId) {
        if (this.mTouchExplorer.contains(displayId)) {
            this.mTouchExplorer.get(displayId).requestDragging(pointerId);
        }
    }

    public void requestDelegating(int displayId) {
        if (this.mTouchExplorer.contains(displayId)) {
            this.mTouchExplorer.get(displayId).requestDelegating();
        }
    }

    public void onDoubleTap(int displayId) {
        if (this.mTouchExplorer.contains(displayId)) {
            this.mTouchExplorer.get(displayId).onDoubleTap();
        }
    }

    public void onDoubleTapAndHold(int displayId) {
        if (this.mTouchExplorer.contains(displayId)) {
            this.mTouchExplorer.get(displayId).onDoubleTapAndHold();
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mEventHandler == null) {
            return;
        }
        pw.append("A11yInputFilter Info : ");
        pw.println();
        ArrayList<Display> displaysList = this.mAms.getValidDisplayList();
        for (int i = 0; i < displaysList.size(); i++) {
            int displayId = displaysList.get(i).getDisplayId();
            EventStreamTransformation next = this.mEventHandler.get(displayId);
            if (next != null) {
                pw.append("Enabled features of Display [");
                pw.append((CharSequence) Integer.toString(displayId));
                pw.append("] = ");
                StringJoiner joiner = new StringJoiner(",", "[", "]");
                while (next != null) {
                    if (next instanceof MagnificationGestureHandler) {
                        joiner.add("MagnificationGesture");
                    } else if (next instanceof KeyboardInterceptor) {
                        joiner.add("KeyboardInterceptor");
                    } else if (next instanceof TouchExplorer) {
                        joiner.add("TouchExplorer");
                    } else if (next instanceof AutoclickController) {
                        joiner.add("AutoclickController");
                    } else if (next instanceof MotionEventInjector) {
                        joiner.add("MotionEventInjector");
                    }
                    next = next.getNext();
                }
                pw.append((CharSequence) joiner.toString());
            }
            pw.println();
        }
    }
}
