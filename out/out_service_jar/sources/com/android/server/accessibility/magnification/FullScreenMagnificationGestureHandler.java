package com.android.server.accessibility.magnification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import com.android.internal.accessibility.util.AccessibilityStatsLogUtils;
import com.android.server.accessibility.AccessibilityTraceManager;
import com.android.server.accessibility.gestures.GestureUtils;
import com.android.server.accessibility.magnification.MagnificationGestureHandler;
import java.util.Arrays;
/* loaded from: classes.dex */
public class FullScreenMagnificationGestureHandler extends MagnificationGestureHandler {
    private static final float MAX_SCALE = 8.0f;
    private static final float MIN_SCALE = 2.0f;
    State mCurrentState;
    final DelegatingState mDelegatingState;
    final DetectingState mDetectingState;
    final FullScreenMagnificationController mFullScreenMagnificationController;
    final PanningScalingState mPanningScalingState;
    State mPreviousState;
    private final WindowMagnificationPromptController mPromptController;
    private final ScreenStateReceiver mScreenStateReceiver;
    private MotionEvent.PointerCoords[] mTempPointerCoords;
    private MotionEvent.PointerProperties[] mTempPointerProperties;
    final ViewportDraggingState mViewportDraggingState;
    private static final boolean DEBUG_STATE_TRANSITIONS = DEBUG_ALL | false;
    private static final boolean DEBUG_DETECTING = DEBUG_ALL | false;
    private static final boolean DEBUG_PANNING_SCALING = DEBUG_ALL | false;

    public FullScreenMagnificationGestureHandler(Context context, FullScreenMagnificationController fullScreenMagnificationController, AccessibilityTraceManager trace, MagnificationGestureHandler.Callback callback, boolean detectTripleTap, boolean detectShortcutTrigger, WindowMagnificationPromptController promptController, int displayId) {
        super(displayId, detectTripleTap, detectShortcutTrigger, trace, callback);
        if (DEBUG_ALL) {
            Log.i(this.mLogTag, "FullScreenMagnificationGestureHandler(detectTripleTap = " + detectTripleTap + ", detectShortcutTrigger = " + detectShortcutTrigger + ")");
        }
        this.mFullScreenMagnificationController = fullScreenMagnificationController;
        this.mPromptController = promptController;
        this.mDelegatingState = new DelegatingState();
        DetectingState detectingState = new DetectingState(context);
        this.mDetectingState = detectingState;
        this.mViewportDraggingState = new ViewportDraggingState();
        this.mPanningScalingState = new PanningScalingState(context);
        if (this.mDetectShortcutTrigger) {
            ScreenStateReceiver screenStateReceiver = new ScreenStateReceiver(context, this);
            this.mScreenStateReceiver = screenStateReceiver;
            screenStateReceiver.register();
        } else {
            this.mScreenStateReceiver = null;
        }
        transitionTo(detectingState);
    }

    @Override // com.android.server.accessibility.magnification.MagnificationGestureHandler
    void onMotionEventInternal(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        handleEventWith(this.mCurrentState, event, rawEvent, policyFlags);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleEventWith(State stateHandler, MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        this.mPanningScalingState.mScrollGestureDetector.onTouchEvent(event);
        this.mPanningScalingState.mScaleGestureDetector.onTouchEvent(event);
        stateHandler.onMotionEvent(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void clearEvents(int inputSource) {
        if (inputSource == 4098) {
            clearAndTransitionToStateDetecting();
        }
        super.clearEvents(inputSource);
    }

    @Override // com.android.server.accessibility.EventStreamTransformation
    public void onDestroy() {
        if (DEBUG_STATE_TRANSITIONS) {
            Slog.i(this.mLogTag, "onDestroy(); delayed = " + MotionEventInfo.toString(this.mDetectingState.mDelayedEventQueue));
        }
        ScreenStateReceiver screenStateReceiver = this.mScreenStateReceiver;
        if (screenStateReceiver != null) {
            screenStateReceiver.unregister();
        }
        this.mPromptController.onDestroy();
        this.mFullScreenMagnificationController.resetIfNeeded(this.mDisplayId, 0);
        clearAndTransitionToStateDetecting();
    }

    @Override // com.android.server.accessibility.magnification.MagnificationGestureHandler
    public void handleShortcutTriggered() {
        boolean wasMagnifying = this.mFullScreenMagnificationController.resetIfNeeded(this.mDisplayId, true);
        if (wasMagnifying) {
            clearAndTransitionToStateDetecting();
            return;
        }
        this.mPromptController.showNotificationIfNeeded();
        this.mDetectingState.toggleShortcutTriggered();
    }

    @Override // com.android.server.accessibility.magnification.MagnificationGestureHandler
    public int getMode() {
        return 1;
    }

    void clearAndTransitionToStateDetecting() {
        DetectingState detectingState = this.mDetectingState;
        this.mCurrentState = detectingState;
        detectingState.clear();
        this.mViewportDraggingState.clear();
        this.mPanningScalingState.clear();
    }

    private MotionEvent.PointerCoords[] getTempPointerCoordsWithMinSize(int size) {
        MotionEvent.PointerCoords[] pointerCoordsArr = this.mTempPointerCoords;
        int oldSize = pointerCoordsArr != null ? pointerCoordsArr.length : 0;
        if (oldSize < size) {
            MotionEvent.PointerCoords[] oldTempPointerCoords = this.mTempPointerCoords;
            MotionEvent.PointerCoords[] pointerCoordsArr2 = new MotionEvent.PointerCoords[size];
            this.mTempPointerCoords = pointerCoordsArr2;
            if (oldTempPointerCoords != null) {
                System.arraycopy(oldTempPointerCoords, 0, pointerCoordsArr2, 0, oldSize);
            }
        }
        for (int i = oldSize; i < size; i++) {
            this.mTempPointerCoords[i] = new MotionEvent.PointerCoords();
        }
        return this.mTempPointerCoords;
    }

    private MotionEvent.PointerProperties[] getTempPointerPropertiesWithMinSize(int size) {
        MotionEvent.PointerProperties[] pointerPropertiesArr = this.mTempPointerProperties;
        int oldSize = pointerPropertiesArr != null ? pointerPropertiesArr.length : 0;
        if (oldSize < size) {
            MotionEvent.PointerProperties[] oldTempPointerProperties = this.mTempPointerProperties;
            MotionEvent.PointerProperties[] pointerPropertiesArr2 = new MotionEvent.PointerProperties[size];
            this.mTempPointerProperties = pointerPropertiesArr2;
            if (oldTempPointerProperties != null) {
                System.arraycopy(oldTempPointerProperties, 0, pointerPropertiesArr2, 0, oldSize);
            }
        }
        for (int i = oldSize; i < size; i++) {
            this.mTempPointerProperties[i] = new MotionEvent.PointerProperties();
        }
        return this.mTempPointerProperties;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void transitionTo(State state) {
        if (DEBUG_STATE_TRANSITIONS) {
            Slog.i(this.mLogTag, (State.nameOf(this.mCurrentState) + " -> " + State.nameOf(state) + " at " + Arrays.asList((StackTraceElement[]) Arrays.copyOfRange(new RuntimeException().getStackTrace(), 1, 5))).replace(getClass().getName(), ""));
        }
        this.mPreviousState = this.mCurrentState;
        this.mCurrentState = state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface State {
        void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i);

        default void clear() {
        }

        default String name() {
            return getClass().getSimpleName();
        }

        static String nameOf(State s) {
            return s != null ? s.name() : "null";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class PanningScalingState extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener, State {
        float mInitialScaleFactor = -1.0f;
        private final ScaleGestureDetector mScaleGestureDetector;
        boolean mScaling;
        final float mScalingThreshold;
        private final GestureDetector mScrollGestureDetector;

        PanningScalingState(Context context) {
            TypedValue scaleValue = new TypedValue();
            context.getResources().getValue(17105110, scaleValue, false);
            this.mScalingThreshold = scaleValue.getFloat();
            ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context, this, Handler.getMain());
            this.mScaleGestureDetector = scaleGestureDetector;
            scaleGestureDetector.setQuickScaleEnabled(false);
            this.mScrollGestureDetector = new GestureDetector(context, this, Handler.getMain());
        }

        @Override // com.android.server.accessibility.magnification.FullScreenMagnificationGestureHandler.State
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            int action = event.getActionMasked();
            if (action == 6 && event.getPointerCount() == 2 && FullScreenMagnificationGestureHandler.this.mPreviousState == FullScreenMagnificationGestureHandler.this.mViewportDraggingState) {
                persistScaleAndTransitionTo(FullScreenMagnificationGestureHandler.this.mViewportDraggingState);
            } else if (action == 1 || action == 3) {
                persistScaleAndTransitionTo(FullScreenMagnificationGestureHandler.this.mDetectingState);
            }
        }

        public void persistScaleAndTransitionTo(State state) {
            FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.persistScale(FullScreenMagnificationGestureHandler.this.mDisplayId);
            clear();
            FullScreenMagnificationGestureHandler.this.transitionTo(state);
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
        public boolean onScroll(MotionEvent first, MotionEvent second, float distanceX, float distanceY) {
            if (FullScreenMagnificationGestureHandler.this.mCurrentState != FullScreenMagnificationGestureHandler.this.mPanningScalingState) {
                return true;
            }
            if (FullScreenMagnificationGestureHandler.DEBUG_PANNING_SCALING) {
                Slog.i(FullScreenMagnificationGestureHandler.this.mLogTag, "Panned content by scrollX: " + distanceX + " scrollY: " + distanceY);
            }
            FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.offsetMagnifiedRegion(FullScreenMagnificationGestureHandler.this.mDisplayId, distanceX, distanceY, 0);
            return true;
        }

        @Override // android.view.ScaleGestureDetector.OnScaleGestureListener
        public boolean onScale(ScaleGestureDetector detector) {
            float scale;
            if (!this.mScaling) {
                if (this.mInitialScaleFactor < 0.0f) {
                    this.mInitialScaleFactor = detector.getScaleFactor();
                    return false;
                }
                float deltaScale = detector.getScaleFactor() - this.mInitialScaleFactor;
                boolean z = Math.abs(deltaScale) > this.mScalingThreshold;
                this.mScaling = z;
                return z;
            }
            float initialScale = FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.getScale(FullScreenMagnificationGestureHandler.this.mDisplayId);
            float targetScale = detector.getScaleFactor() * initialScale;
            if (targetScale > 8.0f && targetScale > initialScale) {
                scale = 8.0f;
            } else if (targetScale < FullScreenMagnificationGestureHandler.MIN_SCALE && targetScale < initialScale) {
                scale = FullScreenMagnificationGestureHandler.MIN_SCALE;
            } else {
                scale = targetScale;
            }
            float pivotX = detector.getFocusX();
            float pivotY = detector.getFocusY();
            if (FullScreenMagnificationGestureHandler.DEBUG_PANNING_SCALING) {
                Slog.i(FullScreenMagnificationGestureHandler.this.mLogTag, "Scaled content to: " + scale + "x");
            }
            FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.setScale(FullScreenMagnificationGestureHandler.this.mDisplayId, scale, pivotX, pivotY, false, 0);
            return true;
        }

        @Override // android.view.ScaleGestureDetector.OnScaleGestureListener
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return FullScreenMagnificationGestureHandler.this.mCurrentState == FullScreenMagnificationGestureHandler.this.mPanningScalingState;
        }

        @Override // android.view.ScaleGestureDetector.OnScaleGestureListener
        public void onScaleEnd(ScaleGestureDetector detector) {
            clear();
        }

        @Override // com.android.server.accessibility.magnification.FullScreenMagnificationGestureHandler.State
        public void clear() {
            this.mInitialScaleFactor = -1.0f;
            this.mScaling = false;
        }

        public String toString() {
            return "PanningScalingState{mInitialScaleFactor=" + this.mInitialScaleFactor + ", mScaling=" + this.mScaling + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ViewportDraggingState implements State {
        private boolean mLastMoveOutsideMagnifiedRegion;
        boolean mZoomedInBeforeDrag;

        ViewportDraggingState() {
        }

        @Override // com.android.server.accessibility.magnification.FullScreenMagnificationGestureHandler.State
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            int action = event.getActionMasked();
            switch (action) {
                case 0:
                case 6:
                    throw new IllegalArgumentException("Unexpected event type: " + MotionEvent.actionToString(action));
                case 1:
                case 3:
                    if (!this.mZoomedInBeforeDrag) {
                        FullScreenMagnificationGestureHandler.this.zoomOff();
                    }
                    clear();
                    FullScreenMagnificationGestureHandler fullScreenMagnificationGestureHandler = FullScreenMagnificationGestureHandler.this;
                    fullScreenMagnificationGestureHandler.transitionTo(fullScreenMagnificationGestureHandler.mDetectingState);
                    return;
                case 2:
                    if (event.getPointerCount() != 1) {
                        throw new IllegalStateException("Should have one pointer down.");
                    }
                    float eventX = event.getX();
                    float eventY = event.getY();
                    if (FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.magnificationRegionContains(FullScreenMagnificationGestureHandler.this.mDisplayId, eventX, eventY)) {
                        FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.setCenter(FullScreenMagnificationGestureHandler.this.mDisplayId, eventX, eventY, this.mLastMoveOutsideMagnifiedRegion, 0);
                        this.mLastMoveOutsideMagnifiedRegion = false;
                        return;
                    }
                    this.mLastMoveOutsideMagnifiedRegion = true;
                    return;
                case 4:
                default:
                    return;
                case 5:
                    clear();
                    FullScreenMagnificationGestureHandler fullScreenMagnificationGestureHandler2 = FullScreenMagnificationGestureHandler.this;
                    fullScreenMagnificationGestureHandler2.transitionTo(fullScreenMagnificationGestureHandler2.mPanningScalingState);
                    return;
            }
        }

        @Override // com.android.server.accessibility.magnification.FullScreenMagnificationGestureHandler.State
        public void clear() {
            this.mLastMoveOutsideMagnifiedRegion = false;
        }

        public String toString() {
            return "ViewportDraggingState{mZoomedInBeforeDrag=" + this.mZoomedInBeforeDrag + ", mLastMoveOutsideMagnifiedRegion=" + this.mLastMoveOutsideMagnifiedRegion + '}';
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class DelegatingState implements State {
        public long mLastDelegatedDownEventTime;

        DelegatingState() {
        }

        @Override // com.android.server.accessibility.magnification.FullScreenMagnificationGestureHandler.State
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            switch (event.getActionMasked()) {
                case 0:
                    FullScreenMagnificationGestureHandler fullScreenMagnificationGestureHandler = FullScreenMagnificationGestureHandler.this;
                    fullScreenMagnificationGestureHandler.transitionTo(fullScreenMagnificationGestureHandler.mDelegatingState);
                    this.mLastDelegatedDownEventTime = event.getDownTime();
                    break;
                case 1:
                case 3:
                    FullScreenMagnificationGestureHandler fullScreenMagnificationGestureHandler2 = FullScreenMagnificationGestureHandler.this;
                    fullScreenMagnificationGestureHandler2.transitionTo(fullScreenMagnificationGestureHandler2.mDetectingState);
                    break;
            }
            if (FullScreenMagnificationGestureHandler.this.getNext() != null) {
                event.setDownTime(this.mLastDelegatedDownEventTime);
                FullScreenMagnificationGestureHandler.this.dispatchTransformedEvent(event, rawEvent, policyFlags);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class DetectingState implements State, Handler.Callback {
        private static final int MESSAGE_ON_TRIPLE_TAP_AND_HOLD = 1;
        private static final int MESSAGE_TRANSITION_TO_DELEGATING_STATE = 2;
        private static final int MESSAGE_TRANSITION_TO_PANNINGSCALING_STATE = 3;
        private MotionEventInfo mDelayedEventQueue;
        private long mLastDetectingDownEventTime;
        MotionEvent mLastDown;
        private MotionEvent mLastUp;
        final int mMultiTapMaxDelay;
        final int mMultiTapMaxDistance;
        private MotionEvent mPreLastDown;
        private MotionEvent mPreLastUp;
        boolean mShortcutTriggered;
        final int mSwipeMinDistance;
        private PointF mSecondPointerDownLocation = new PointF(Float.NaN, Float.NaN);
        Handler mHandler = new Handler(Looper.getMainLooper(), this);
        final int mLongTapMinDelay = ViewConfiguration.getLongPressTimeout();

        DetectingState(Context context) {
            this.mMultiTapMaxDelay = ViewConfiguration.getDoubleTapTimeout() + context.getResources().getInteger(17694938);
            this.mSwipeMinDistance = ViewConfiguration.get(context).getScaledTouchSlop();
            this.mMultiTapMaxDistance = ViewConfiguration.get(context).getScaledDoubleTapSlop();
        }

        @Override // android.os.Handler.Callback
        public boolean handleMessage(Message message) {
            int type = message.what;
            switch (type) {
                case 1:
                    MotionEvent down = (MotionEvent) message.obj;
                    transitionToViewportDraggingStateAndClear(down);
                    down.recycle();
                    return true;
                case 2:
                    transitionToDelegatingStateAndClear();
                    return true;
                case 3:
                    transitToPanningScalingStateAndClear();
                    return true;
                default:
                    throw new IllegalArgumentException("Unknown message type: " + type);
            }
        }

        @Override // com.android.server.accessibility.magnification.FullScreenMagnificationGestureHandler.State
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            cacheDelayedMotionEvent(event, rawEvent, policyFlags);
            switch (event.getActionMasked()) {
                case 0:
                    this.mLastDetectingDownEventTime = event.getDownTime();
                    this.mHandler.removeMessages(2);
                    if (!FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.magnificationRegionContains(FullScreenMagnificationGestureHandler.this.mDisplayId, event.getX(), event.getY())) {
                        transitionToDelegatingStateAndClear();
                        return;
                    } else if (isMultiTapTriggered(2)) {
                        afterLongTapTimeoutTransitionToDraggingState(event);
                        return;
                    } else if (isTapOutOfDistanceSlop()) {
                        transitionToDelegatingStateAndClear();
                        return;
                    } else if (FullScreenMagnificationGestureHandler.this.mDetectTripleTap || FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.isMagnifying(FullScreenMagnificationGestureHandler.this.mDisplayId)) {
                        afterMultiTapTimeoutTransitionToDelegatingState();
                        return;
                    } else {
                        transitionToDelegatingStateAndClear();
                        return;
                    }
                case 1:
                    this.mHandler.removeMessages(1);
                    if (!FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.magnificationRegionContains(FullScreenMagnificationGestureHandler.this.mDisplayId, event.getX(), event.getY())) {
                        transitionToDelegatingStateAndClear();
                        return;
                    } else if (isMultiTapTriggered(3)) {
                        onTripleTap(event);
                        return;
                    } else if (isFingerDown()) {
                        if (timeBetween(this.mLastDown, this.mLastUp) >= this.mLongTapMinDelay || GestureUtils.distance(this.mLastDown, this.mLastUp) >= this.mSwipeMinDistance) {
                            transitionToDelegatingStateAndClear();
                            return;
                        }
                        return;
                    } else {
                        return;
                    }
                case 2:
                    if (isFingerDown() && GestureUtils.distance(this.mLastDown, event) > this.mSwipeMinDistance) {
                        if (isMultiTapTriggered(2) && event.getPointerCount() == 1) {
                            transitionToViewportDraggingStateAndClear(event);
                            return;
                        } else if (isMagnifying() && event.getPointerCount() == 2) {
                            transitToPanningScalingStateAndClear();
                            return;
                        } else {
                            transitionToDelegatingStateAndClear();
                            return;
                        }
                    } else if (isMagnifying() && secondPointerDownValid() && GestureUtils.distanceClosestPointerToPoint(this.mSecondPointerDownLocation, event) > this.mSwipeMinDistance) {
                        transitToPanningScalingStateAndClear();
                        return;
                    } else {
                        return;
                    }
                case 3:
                case 4:
                default:
                    return;
                case 5:
                    if (FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.isMagnifying(FullScreenMagnificationGestureHandler.this.mDisplayId) && event.getPointerCount() == 2) {
                        storeSecondPointerDownLocation(event);
                        this.mHandler.sendEmptyMessageDelayed(3, ViewConfiguration.getTapTimeout());
                        return;
                    }
                    transitionToDelegatingStateAndClear();
                    return;
                case 6:
                    transitionToDelegatingStateAndClear();
                    return;
            }
        }

        private void storeSecondPointerDownLocation(MotionEvent event) {
            int index = event.getActionIndex();
            this.mSecondPointerDownLocation.set(event.getX(index), event.getY(index));
        }

        private boolean secondPointerDownValid() {
            return (Float.isNaN(this.mSecondPointerDownLocation.x) && Float.isNaN(this.mSecondPointerDownLocation.y)) ? false : true;
        }

        private void transitToPanningScalingStateAndClear() {
            FullScreenMagnificationGestureHandler fullScreenMagnificationGestureHandler = FullScreenMagnificationGestureHandler.this;
            fullScreenMagnificationGestureHandler.transitionTo(fullScreenMagnificationGestureHandler.mPanningScalingState);
            clear();
        }

        public boolean isMultiTapTriggered(int numTaps) {
            boolean z = true;
            if (this.mShortcutTriggered) {
                return tapCount() + 2 >= numTaps;
            }
            if (!FullScreenMagnificationGestureHandler.this.mDetectTripleTap || tapCount() < numTaps || !isMultiTap(this.mPreLastDown, this.mLastDown) || !isMultiTap(this.mPreLastUp, this.mLastUp)) {
                z = false;
            }
            boolean multitapTriggered = z;
            if (multitapTriggered && numTaps > 2) {
                boolean enabled = FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.isMagnifying(FullScreenMagnificationGestureHandler.this.mDisplayId);
                AccessibilityStatsLogUtils.logMagnificationTripleTap(enabled);
            }
            return multitapTriggered;
        }

        private boolean isMultiTap(MotionEvent first, MotionEvent second) {
            return GestureUtils.isMultiTap(first, second, this.mMultiTapMaxDelay, this.mMultiTapMaxDistance);
        }

        public boolean isFingerDown() {
            return this.mLastDown != null;
        }

        private long timeBetween(MotionEvent a, MotionEvent b) {
            if (a == null && b == null) {
                return 0L;
            }
            return Math.abs(timeOf(a) - timeOf(b));
        }

        private long timeOf(MotionEvent event) {
            if (event != null) {
                return event.getEventTime();
            }
            return Long.MIN_VALUE;
        }

        public int tapCount() {
            return MotionEventInfo.countOf(this.mDelayedEventQueue, 1);
        }

        public void afterMultiTapTimeoutTransitionToDelegatingState() {
            this.mHandler.sendEmptyMessageDelayed(2, this.mMultiTapMaxDelay);
        }

        public void afterLongTapTimeoutTransitionToDraggingState(MotionEvent event) {
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(1, MotionEvent.obtain(event)), ViewConfiguration.getLongPressTimeout());
        }

        @Override // com.android.server.accessibility.magnification.FullScreenMagnificationGestureHandler.State
        public void clear() {
            setShortcutTriggered(false);
            removePendingDelayedMessages();
            clearDelayedMotionEvents();
            this.mSecondPointerDownLocation.set(Float.NaN, Float.NaN);
        }

        private void removePendingDelayedMessages() {
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
            this.mHandler.removeMessages(3);
        }

        private void cacheDelayedMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            if (event.getActionMasked() == 0) {
                this.mPreLastDown = this.mLastDown;
                this.mLastDown = MotionEvent.obtain(event);
            } else if (event.getActionMasked() == 1) {
                this.mPreLastUp = this.mLastUp;
                this.mLastUp = MotionEvent.obtain(event);
            }
            MotionEventInfo info = MotionEventInfo.obtain(event, rawEvent, policyFlags);
            if (this.mDelayedEventQueue == null) {
                this.mDelayedEventQueue = info;
                return;
            }
            MotionEventInfo tail = this.mDelayedEventQueue;
            while (tail.mNext != null) {
                tail = tail.mNext;
            }
            tail.mNext = info;
        }

        private void sendDelayedMotionEvents() {
            if (this.mDelayedEventQueue == null) {
                return;
            }
            long offset = Math.min(SystemClock.uptimeMillis() - this.mLastDetectingDownEventTime, this.mMultiTapMaxDelay);
            do {
                MotionEventInfo info = this.mDelayedEventQueue;
                this.mDelayedEventQueue = info.mNext;
                info.event.setDownTime(info.event.getDownTime() + offset);
                FullScreenMagnificationGestureHandler fullScreenMagnificationGestureHandler = FullScreenMagnificationGestureHandler.this;
                fullScreenMagnificationGestureHandler.handleEventWith(fullScreenMagnificationGestureHandler.mDelegatingState, info.event, info.rawEvent, info.policyFlags);
                info.recycle();
            } while (this.mDelayedEventQueue != null);
        }

        private void clearDelayedMotionEvents() {
            while (this.mDelayedEventQueue != null) {
                MotionEventInfo info = this.mDelayedEventQueue;
                this.mDelayedEventQueue = info.mNext;
                info.recycle();
            }
            this.mPreLastDown = null;
            this.mPreLastUp = null;
            this.mLastDown = null;
            this.mLastUp = null;
        }

        void transitionToDelegatingStateAndClear() {
            FullScreenMagnificationGestureHandler fullScreenMagnificationGestureHandler = FullScreenMagnificationGestureHandler.this;
            fullScreenMagnificationGestureHandler.transitionTo(fullScreenMagnificationGestureHandler.mDelegatingState);
            sendDelayedMotionEvents();
            removePendingDelayedMessages();
            this.mSecondPointerDownLocation.set(Float.NaN, Float.NaN);
        }

        private void onTripleTap(MotionEvent up) {
            if (FullScreenMagnificationGestureHandler.DEBUG_DETECTING) {
                Slog.i(FullScreenMagnificationGestureHandler.this.mLogTag, "onTripleTap(); delayed: " + MotionEventInfo.toString(this.mDelayedEventQueue));
            }
            clear();
            if (FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.isMagnifying(FullScreenMagnificationGestureHandler.this.mDisplayId)) {
                FullScreenMagnificationGestureHandler.this.zoomOff();
                return;
            }
            FullScreenMagnificationGestureHandler.this.mPromptController.showNotificationIfNeeded();
            FullScreenMagnificationGestureHandler.this.zoomOn(up.getX(), up.getY());
        }

        private boolean isMagnifying() {
            return FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.isMagnifying(FullScreenMagnificationGestureHandler.this.mDisplayId);
        }

        void transitionToViewportDraggingStateAndClear(MotionEvent down) {
            if (FullScreenMagnificationGestureHandler.DEBUG_DETECTING) {
                Slog.i(FullScreenMagnificationGestureHandler.this.mLogTag, "onTripleTapAndHold()");
            }
            clear();
            FullScreenMagnificationGestureHandler.this.mViewportDraggingState.mZoomedInBeforeDrag = FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.isMagnifying(FullScreenMagnificationGestureHandler.this.mDisplayId);
            boolean enabled = !FullScreenMagnificationGestureHandler.this.mViewportDraggingState.mZoomedInBeforeDrag;
            AccessibilityStatsLogUtils.logMagnificationTripleTap(enabled);
            FullScreenMagnificationGestureHandler.this.zoomOn(down.getX(), down.getY());
            FullScreenMagnificationGestureHandler fullScreenMagnificationGestureHandler = FullScreenMagnificationGestureHandler.this;
            fullScreenMagnificationGestureHandler.transitionTo(fullScreenMagnificationGestureHandler.mViewportDraggingState);
        }

        public String toString() {
            return "DetectingState{tapCount()=" + tapCount() + ", mShortcutTriggered=" + this.mShortcutTriggered + ", mDelayedEventQueue=" + MotionEventInfo.toString(this.mDelayedEventQueue) + '}';
        }

        void toggleShortcutTriggered() {
            setShortcutTriggered(!this.mShortcutTriggered);
        }

        void setShortcutTriggered(boolean state) {
            if (this.mShortcutTriggered == state) {
                return;
            }
            if (FullScreenMagnificationGestureHandler.DEBUG_DETECTING) {
                Slog.i(FullScreenMagnificationGestureHandler.this.mLogTag, "setShortcutTriggered(" + state + ")");
            }
            this.mShortcutTriggered = state;
            FullScreenMagnificationGestureHandler.this.mFullScreenMagnificationController.setForceShowMagnifiableBounds(FullScreenMagnificationGestureHandler.this.mDisplayId, state);
        }

        boolean isTapOutOfDistanceSlop() {
            MotionEvent motionEvent;
            MotionEvent motionEvent2;
            if (!FullScreenMagnificationGestureHandler.this.mDetectTripleTap || (motionEvent = this.mPreLastDown) == null || (motionEvent2 = this.mLastDown) == null) {
                return false;
            }
            boolean outOfDistanceSlop = GestureUtils.distance(motionEvent, motionEvent2) > ((double) this.mMultiTapMaxDistance);
            if (tapCount() > 0) {
                return outOfDistanceSlop;
            }
            return outOfDistanceSlop && !GestureUtils.isTimedOut(this.mPreLastDown, this.mLastDown, this.mMultiTapMaxDelay);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void zoomOn(float centerX, float centerY) {
        if (DEBUG_DETECTING) {
            Slog.i(this.mLogTag, "zoomOn(" + centerX + ", " + centerY + ")");
        }
        float scale = MathUtils.constrain(this.mFullScreenMagnificationController.getPersistedScale(this.mDisplayId), (float) MIN_SCALE, 8.0f);
        this.mFullScreenMagnificationController.setScaleAndCenter(this.mDisplayId, scale, centerX, centerY, true, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void zoomOff() {
        if (DEBUG_DETECTING) {
            Slog.i(this.mLogTag, "zoomOff()");
        }
        this.mFullScreenMagnificationController.reset(this.mDisplayId, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static MotionEvent recycleAndNullify(MotionEvent event) {
        if (event != null) {
            event.recycle();
            return null;
        }
        return null;
    }

    public String toString() {
        return "MagnificationGesture{mDetectingState=" + this.mDetectingState + ", mDelegatingState=" + this.mDelegatingState + ", mMagnifiedInteractionState=" + this.mPanningScalingState + ", mViewportDraggingState=" + this.mViewportDraggingState + ", mDetectTripleTap=" + this.mDetectTripleTap + ", mDetectShortcutTrigger=" + this.mDetectShortcutTrigger + ", mCurrentState=" + State.nameOf(this.mCurrentState) + ", mPreviousState=" + State.nameOf(this.mPreviousState) + ", mMagnificationController=" + this.mFullScreenMagnificationController + ", mDisplayId=" + this.mDisplayId + '}';
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class MotionEventInfo {
        private static final int MAX_POOL_SIZE = 10;
        private static final Object sLock = new Object();
        private static MotionEventInfo sPool;
        private static int sPoolSize;
        public MotionEvent event;
        private boolean mInPool;
        private MotionEventInfo mNext;
        public int policyFlags;
        public MotionEvent rawEvent;

        private MotionEventInfo() {
        }

        public static MotionEventInfo obtain(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            MotionEventInfo info;
            synchronized (sLock) {
                info = obtainInternal();
                info.initialize(event, rawEvent, policyFlags);
            }
            return info;
        }

        private static MotionEventInfo obtainInternal() {
            int i = sPoolSize;
            if (i > 0) {
                sPoolSize = i - 1;
                MotionEventInfo info = sPool;
                sPool = info.mNext;
                info.mNext = null;
                info.mInPool = false;
                return info;
            }
            return new MotionEventInfo();
        }

        private void initialize(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            this.event = MotionEvent.obtain(event);
            this.rawEvent = MotionEvent.obtain(rawEvent);
            this.policyFlags = policyFlags;
        }

        public void recycle() {
            synchronized (sLock) {
                if (this.mInPool) {
                    throw new IllegalStateException("Already recycled.");
                }
                clear();
                int i = sPoolSize;
                if (i < 10) {
                    sPoolSize = i + 1;
                    this.mNext = sPool;
                    sPool = this;
                    this.mInPool = true;
                }
            }
        }

        private void clear() {
            this.event = FullScreenMagnificationGestureHandler.recycleAndNullify(this.event);
            this.rawEvent = FullScreenMagnificationGestureHandler.recycleAndNullify(this.rawEvent);
            this.policyFlags = 0;
        }

        static int countOf(MotionEventInfo info, int eventType) {
            if (info == null) {
                return 0;
            }
            return (info.event.getAction() == eventType ? 1 : 0) + countOf(info.mNext, eventType);
        }

        public static String toString(MotionEventInfo info) {
            if (info == null) {
                return "";
            }
            return MotionEvent.actionToString(info.event.getAction()).replace("ACTION_", "") + " " + toString(info.mNext);
        }
    }

    /* loaded from: classes.dex */
    private static class ScreenStateReceiver extends BroadcastReceiver {
        private final Context mContext;
        private final FullScreenMagnificationGestureHandler mGestureHandler;
        private boolean mRegistered = false;

        ScreenStateReceiver(Context context, FullScreenMagnificationGestureHandler gestureHandler) {
            this.mContext = context;
            this.mGestureHandler = gestureHandler;
        }

        public void register() {
            if (!this.mRegistered) {
                this.mContext.registerReceiver(this, new IntentFilter("android.intent.action.SCREEN_OFF"));
                this.mRegistered = true;
            }
        }

        public void unregister() {
            if (this.mRegistered) {
                try {
                    this.mContext.unregisterReceiver(this);
                } catch (IllegalArgumentException e) {
                    Slog.d("FullScreenMagnificationGestureHandler", "unregisterReceiver fail", e);
                }
                this.mRegistered = false;
            }
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            this.mGestureHandler.mDetectingState.setShortcutTriggered(false);
        }
    }
}
