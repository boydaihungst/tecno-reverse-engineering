package com.android.server.accessibility.magnification;

import android.content.Context;
import android.graphics.Point;
import android.util.MathUtils;
import android.util.Slog;
import android.view.Display;
import android.view.MotionEvent;
import com.android.server.accessibility.AccessibilityTraceManager;
import com.android.server.accessibility.gestures.MultiTap;
import com.android.server.accessibility.gestures.MultiTapAndHold;
import com.android.server.accessibility.magnification.MagnificationGestureHandler;
import com.android.server.accessibility.magnification.MagnificationGesturesObserver;
import com.android.server.accessibility.magnification.MotionEventDispatcherDelegate;
import com.android.server.accessibility.magnification.PanningScalingHandler;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes.dex */
public class WindowMagnificationGestureHandler extends MagnificationGestureHandler {
    private static final float MAX_SCALE = 8.0f;
    private static final float MIN_SCALE = 2.0f;
    private final Context mContext;
    State mCurrentState;
    final DelegatingState mDelegatingState;
    final DetectingState mDetectingState;
    private MotionEventDispatcherDelegate mMotionEventDispatcherDelegate;
    final PanningScalingGestureState mObservePanningScalingState;
    State mPreviousState;
    private final Point mTempPoint;
    final ViewportDraggingState mViewportDraggingState;
    private final WindowMagnificationManager mWindowMagnificationMgr;
    private static final boolean DEBUG_STATE_TRANSITIONS = DEBUG_ALL | false;
    private static final boolean DEBUG_DETECTING = DEBUG_ALL | false;

    public WindowMagnificationGestureHandler(Context context, WindowMagnificationManager windowMagnificationMgr, AccessibilityTraceManager trace, MagnificationGestureHandler.Callback callback, boolean detectTripleTap, boolean detectShortcutTrigger, int displayId) {
        super(displayId, detectTripleTap, detectShortcutTrigger, trace, callback);
        this.mTempPoint = new Point();
        if (DEBUG_ALL) {
            Slog.i(this.mLogTag, "WindowMagnificationGestureHandler() , displayId = " + displayId + ")");
        }
        this.mContext = context;
        this.mWindowMagnificationMgr = windowMagnificationMgr;
        MotionEventDispatcherDelegate motionEventDispatcherDelegate = new MotionEventDispatcherDelegate(context, new MotionEventDispatcherDelegate.EventDispatcher() { // from class: com.android.server.accessibility.magnification.WindowMagnificationGestureHandler$$ExternalSyntheticLambda0
            @Override // com.android.server.accessibility.magnification.MotionEventDispatcherDelegate.EventDispatcher
            public final void dispatchMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
                WindowMagnificationGestureHandler.this.m745x1ef03080(motionEvent, motionEvent2, i);
            }
        });
        this.mMotionEventDispatcherDelegate = motionEventDispatcherDelegate;
        this.mDelegatingState = new DelegatingState(motionEventDispatcherDelegate);
        DetectingState detectingState = new DetectingState(context, this.mDetectTripleTap);
        this.mDetectingState = detectingState;
        this.mViewportDraggingState = new ViewportDraggingState();
        this.mObservePanningScalingState = new PanningScalingGestureState(new PanningScalingHandler(context, 8.0f, MIN_SCALE, true, new PanningScalingHandler.MagnificationDelegate() { // from class: com.android.server.accessibility.magnification.WindowMagnificationGestureHandler.1
            @Override // com.android.server.accessibility.magnification.PanningScalingHandler.MagnificationDelegate
            public boolean processScroll(int displayId2, float distanceX, float distanceY) {
                return WindowMagnificationGestureHandler.this.mWindowMagnificationMgr.processScroll(displayId2, distanceX, distanceY);
            }

            @Override // com.android.server.accessibility.magnification.PanningScalingHandler.MagnificationDelegate
            public void setScale(int displayId2, float scale) {
                WindowMagnificationGestureHandler.this.mWindowMagnificationMgr.setScale(displayId2, scale);
            }

            @Override // com.android.server.accessibility.magnification.PanningScalingHandler.MagnificationDelegate
            public float getScale(int displayId2) {
                return WindowMagnificationGestureHandler.this.mWindowMagnificationMgr.getScale(displayId2);
            }
        }));
        transitionTo(detectingState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-accessibility-magnification-WindowMagnificationGestureHandler  reason: not valid java name */
    public /* synthetic */ void m745x1ef03080(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        dispatchTransformedEvent(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.magnification.MagnificationGestureHandler
    void onMotionEventInternal(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        this.mObservePanningScalingState.mPanningScalingHandler.onTouchEvent(event);
        this.mCurrentState.onMotionEvent(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void clearEvents(int inputSource) {
        if (inputSource == 4098) {
            resetToDetectState();
        }
        super.clearEvents(inputSource);
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void onDestroy() {
        if (DEBUG_ALL) {
            Slog.i(this.mLogTag, "onDestroy(); delayed = " + this.mDetectingState.toString());
        }
        this.mWindowMagnificationMgr.disableWindowMagnification(this.mDisplayId, true);
        resetToDetectState();
    }

    @Override // com.android.server.accessibility.magnification.MagnificationGestureHandler
    public void handleShortcutTriggered() {
        Point screenSize = this.mTempPoint;
        getScreenSize(this.mTempPoint);
        toggleMagnification(screenSize.x / MIN_SCALE, screenSize.y / MIN_SCALE, 0);
    }

    private void getScreenSize(Point outSize) {
        Display display = this.mContext.getDisplay();
        display.getRealSize(outSize);
    }

    @Override // com.android.server.accessibility.magnification.MagnificationGestureHandler
    public int getMode() {
        return 2;
    }

    private void enableWindowMagnifier(float centerX, float centerY, int windowPosition) {
        if (DEBUG_ALL) {
            Slog.i(this.mLogTag, "enableWindowMagnifier :" + centerX + ", " + centerY + ", " + windowPosition);
        }
        float scale = MathUtils.constrain(this.mWindowMagnificationMgr.getPersistedScale(this.mDisplayId), (float) MIN_SCALE, 8.0f);
        this.mWindowMagnificationMgr.enableWindowMagnification(this.mDisplayId, scale, centerX, centerY, windowPosition);
    }

    private void disableWindowMagnifier() {
        if (DEBUG_ALL) {
            Slog.i(this.mLogTag, "disableWindowMagnifier()");
        }
        this.mWindowMagnificationMgr.disableWindowMagnification(this.mDisplayId, false);
    }

    private void toggleMagnification(float centerX, float centerY, int windowPosition) {
        if (this.mWindowMagnificationMgr.isWindowMagnifierEnabled(this.mDisplayId)) {
            disableWindowMagnifier();
        } else {
            enableWindowMagnifier(centerX, centerY, windowPosition);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTripleTap(MotionEvent up) {
        if (DEBUG_DETECTING) {
            Slog.i(this.mLogTag, "onTripleTap()");
        }
        toggleMagnification(up.getX(), up.getY(), 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTripleTapAndHold(MotionEvent up) {
        if (DEBUG_DETECTING) {
            Slog.i(this.mLogTag, "onTripleTapAndHold()");
        }
        enableWindowMagnifier(up.getX(), up.getY(), 1);
        transitionTo(this.mViewportDraggingState);
    }

    void resetToDetectState() {
        transitionTo(this.mDetectingState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface State {
        void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i);

        default void clear() {
        }

        default void onEnter() {
        }

        default void onExit() {
        }

        default String name() {
            return getClass().getSimpleName();
        }

        static String nameOf(State s) {
            return s != null ? s.name() : "null";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void transitionTo(State state) {
        if (DEBUG_STATE_TRANSITIONS) {
            Slog.i(this.mLogTag, "state transition: " + (State.nameOf(this.mCurrentState) + " -> " + State.nameOf(state) + " at " + Arrays.asList((StackTraceElement[]) Arrays.copyOfRange(new RuntimeException().getStackTrace(), 1, 5))).replace(getClass().getName(), ""));
        }
        State state2 = this.mCurrentState;
        this.mPreviousState = state2;
        if (state2 != null) {
            state2.onExit();
        }
        this.mCurrentState = state;
        if (state != null) {
            state.onEnter();
        }
    }

    /* loaded from: classes.dex */
    final class PanningScalingGestureState implements State {
        private final PanningScalingHandler mPanningScalingHandler;

        PanningScalingGestureState(PanningScalingHandler panningScalingHandler) {
            this.mPanningScalingHandler = panningScalingHandler;
        }

        @Override // com.android.server.accessibility.magnification.WindowMagnificationGestureHandler.State
        public void onEnter() {
            this.mPanningScalingHandler.setEnabled(true);
        }

        @Override // com.android.server.accessibility.magnification.WindowMagnificationGestureHandler.State
        public void onExit() {
            this.mPanningScalingHandler.setEnabled(false);
            WindowMagnificationGestureHandler.this.mWindowMagnificationMgr.persistScale(WindowMagnificationGestureHandler.this.mDisplayId);
            clear();
        }

        @Override // com.android.server.accessibility.magnification.WindowMagnificationGestureHandler.State
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            int action = event.getActionMasked();
            if (action == 1 || action == 3) {
                WindowMagnificationGestureHandler windowMagnificationGestureHandler = WindowMagnificationGestureHandler.this;
                windowMagnificationGestureHandler.transitionTo(windowMagnificationGestureHandler.mDetectingState);
            }
        }

        @Override // com.android.server.accessibility.magnification.WindowMagnificationGestureHandler.State
        public void clear() {
            this.mPanningScalingHandler.clear();
        }

        public String toString() {
            return "PanningScalingState{mPanningScalingHandler =" + this.mPanningScalingHandler + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class DelegatingState implements State {
        private final MotionEventDispatcherDelegate mMotionEventDispatcherDelegate;

        DelegatingState(MotionEventDispatcherDelegate motionEventDispatcherDelegate) {
            this.mMotionEventDispatcherDelegate = motionEventDispatcherDelegate;
        }

        @Override // com.android.server.accessibility.magnification.WindowMagnificationGestureHandler.State
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            this.mMotionEventDispatcherDelegate.dispatchMotionEvent(event, rawEvent, policyFlags);
            switch (event.getActionMasked()) {
                case 1:
                case 3:
                    WindowMagnificationGestureHandler windowMagnificationGestureHandler = WindowMagnificationGestureHandler.this;
                    windowMagnificationGestureHandler.transitionTo(windowMagnificationGestureHandler.mDetectingState);
                    return;
                case 2:
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ViewportDraggingState implements State {
        private float mLastX = Float.NaN;
        private float mLastY = Float.NaN;

        ViewportDraggingState() {
        }

        @Override // com.android.server.accessibility.magnification.WindowMagnificationGestureHandler.State
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            int action = event.getActionMasked();
            switch (action) {
                case 1:
                case 3:
                    WindowMagnificationGestureHandler.this.mWindowMagnificationMgr.disableWindowMagnification(WindowMagnificationGestureHandler.this.mDisplayId, true);
                    WindowMagnificationGestureHandler windowMagnificationGestureHandler = WindowMagnificationGestureHandler.this;
                    windowMagnificationGestureHandler.transitionTo(windowMagnificationGestureHandler.mDetectingState);
                    return;
                case 2:
                    if (!Float.isNaN(this.mLastX) && !Float.isNaN(this.mLastY)) {
                        float offsetX = event.getX() - this.mLastX;
                        float offsetY = event.getY() - this.mLastY;
                        WindowMagnificationGestureHandler.this.mWindowMagnificationMgr.moveWindowMagnification(WindowMagnificationGestureHandler.this.mDisplayId, offsetX, offsetY);
                    }
                    float offsetX2 = event.getX();
                    this.mLastX = offsetX2;
                    this.mLastY = event.getY();
                    return;
                default:
                    return;
            }
        }

        @Override // com.android.server.accessibility.magnification.WindowMagnificationGestureHandler.State
        public void clear() {
            this.mLastX = Float.NaN;
            this.mLastY = Float.NaN;
        }

        @Override // com.android.server.accessibility.magnification.WindowMagnificationGestureHandler.State
        public void onExit() {
            clear();
        }

        public String toString() {
            return "ViewportDraggingState{mLastX=" + this.mLastX + ",mLastY=" + this.mLastY + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class DetectingState implements State, MagnificationGesturesObserver.Callback {
        private final boolean mDetectTripleTap;
        private final MagnificationGesturesObserver mGesturesObserver;

        DetectingState(Context context, boolean detectTripleTap) {
            int i;
            int i2;
            this.mDetectTripleTap = detectTripleTap;
            int i3 = detectTripleTap ? 3 : 1;
            if (detectTripleTap) {
                i = 105;
            } else {
                i = 103;
            }
            MultiTap multiTap = new MultiTap(context, i3, i, null);
            int i4 = detectTripleTap ? 3 : 1;
            if (detectTripleTap) {
                i2 = 106;
            } else {
                i2 = 104;
            }
            MultiTapAndHold multiTapAndHold = new MultiTapAndHold(context, i4, i2, null);
            this.mGesturesObserver = new MagnificationGesturesObserver(this, new SimpleSwipe(context), multiTap, multiTapAndHold, new TwoFingersDownOrSwipe(context));
        }

        @Override // com.android.server.accessibility.magnification.WindowMagnificationGestureHandler.State
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            this.mGesturesObserver.onMotionEvent(event, rawEvent, policyFlags);
        }

        public String toString() {
            return "DetectingState{, mGestureTimeoutObserver =" + this.mGesturesObserver + '}';
        }

        @Override // com.android.server.accessibility.magnification.MagnificationGesturesObserver.Callback
        public boolean shouldStopDetection(MotionEvent motionEvent) {
            return (WindowMagnificationGestureHandler.this.mWindowMagnificationMgr.isWindowMagnifierEnabled(WindowMagnificationGestureHandler.this.mDisplayId) || this.mDetectTripleTap) ? false : true;
        }

        @Override // com.android.server.accessibility.magnification.MagnificationGesturesObserver.Callback
        public void onGestureCompleted(int gestureId, long lastDownEventTime, List<MotionEventInfo> delayedEventQueue, MotionEvent motionEvent) {
            if (WindowMagnificationGestureHandler.DEBUG_DETECTING) {
                Slog.d(WindowMagnificationGestureHandler.this.mLogTag, "onGestureDetected : gesture = " + MagnificationGestureMatcher.gestureIdToString(gestureId));
                Slog.d(WindowMagnificationGestureHandler.this.mLogTag, "onGestureDetected : delayedEventQueue = " + delayedEventQueue);
            }
            if (gestureId == 101 && WindowMagnificationGestureHandler.this.mWindowMagnificationMgr.pointersInWindow(WindowMagnificationGestureHandler.this.mDisplayId, motionEvent) > 0) {
                WindowMagnificationGestureHandler windowMagnificationGestureHandler = WindowMagnificationGestureHandler.this;
                windowMagnificationGestureHandler.transitionTo(windowMagnificationGestureHandler.mObservePanningScalingState);
            } else if (gestureId == 105) {
                WindowMagnificationGestureHandler.this.onTripleTap(motionEvent);
            } else if (gestureId == 106) {
                WindowMagnificationGestureHandler.this.onTripleTapAndHold(motionEvent);
            } else {
                WindowMagnificationGestureHandler.this.mMotionEventDispatcherDelegate.sendDelayedMotionEvents(delayedEventQueue, lastDownEventTime);
                changeToDelegateStateIfNeed(motionEvent);
            }
        }

        @Override // com.android.server.accessibility.magnification.MagnificationGesturesObserver.Callback
        public void onGestureCancelled(long lastDownEventTime, List<MotionEventInfo> delayedEventQueue, MotionEvent motionEvent) {
            if (WindowMagnificationGestureHandler.DEBUG_DETECTING) {
                Slog.d(WindowMagnificationGestureHandler.this.mLogTag, "onGestureCancelled : delayedEventQueue = " + delayedEventQueue);
            }
            WindowMagnificationGestureHandler.this.mMotionEventDispatcherDelegate.sendDelayedMotionEvents(delayedEventQueue, lastDownEventTime);
            changeToDelegateStateIfNeed(motionEvent);
        }

        private void changeToDelegateStateIfNeed(MotionEvent motionEvent) {
            if (motionEvent != null && (motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3)) {
                return;
            }
            WindowMagnificationGestureHandler windowMagnificationGestureHandler = WindowMagnificationGestureHandler.this;
            windowMagnificationGestureHandler.transitionTo(windowMagnificationGestureHandler.mDelegatingState);
        }
    }

    public String toString() {
        return "WindowMagnificationGestureHandler{mDetectingState=" + this.mDetectingState + ", mDelegatingState=" + this.mDelegatingState + ", mViewportDraggingState=" + this.mViewportDraggingState + ", mMagnifiedInteractionState=" + this.mObservePanningScalingState + ", mCurrentState=" + State.nameOf(this.mCurrentState) + ", mPreviousState=" + State.nameOf(this.mPreviousState) + ", mWindowMagnificationMgr=" + this.mWindowMagnificationMgr + ", mDisplayId=" + this.mDisplayId + '}';
    }
}
