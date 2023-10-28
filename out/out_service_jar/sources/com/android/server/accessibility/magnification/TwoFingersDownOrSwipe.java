package com.android.server.accessibility.magnification;

import android.content.Context;
import android.os.Handler;
import android.util.MathUtils;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.server.accessibility.gestures.GestureMatcher;
/* loaded from: classes.dex */
final class TwoFingersDownOrSwipe extends GestureMatcher {
    private final int mDetectionDurationMillis;
    private final int mDoubleTapTimeout;
    private MotionEvent mFirstPointerDown;
    private MotionEvent mSecondPointerDown;
    private final int mSwipeMinDistance;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TwoFingersDownOrSwipe(Context context) {
        super(101, new Handler(context.getMainLooper()), null);
        this.mDetectionDurationMillis = MagnificationGestureMatcher.getMagnificationMultiTapTimeout(context);
        this.mDoubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
        this.mSwipeMinDistance = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        this.mFirstPointerDown = MotionEvent.obtain(event);
        cancelAfter(this.mDetectionDurationMillis, event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (this.mFirstPointerDown == null) {
            cancelGesture(event, rawEvent, policyFlags);
        }
        if (event.getPointerCount() == 2) {
            this.mSecondPointerDown = MotionEvent.obtain(event);
            completeAfter(this.mDoubleTapTimeout, event, rawEvent, policyFlags);
            return;
        }
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onMove(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        MotionEvent motionEvent = this.mFirstPointerDown;
        if (motionEvent == null || this.mSecondPointerDown == null) {
            return;
        }
        if (distance(motionEvent, event) > this.mSwipeMinDistance) {
            completeGesture(event, rawEvent, policyFlags);
        } else if (distance(this.mSecondPointerDown, event) > this.mSwipeMinDistance) {
            completeGesture(event, rawEvent, policyFlags);
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public void clear() {
        MotionEvent motionEvent = this.mFirstPointerDown;
        if (motionEvent != null) {
            motionEvent.recycle();
            this.mFirstPointerDown = null;
        }
        MotionEvent motionEvent2 = this.mSecondPointerDown;
        if (motionEvent2 != null) {
            motionEvent2.recycle();
            this.mSecondPointerDown = null;
        }
        super.clear();
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected String getGestureName() {
        return getClass().getSimpleName();
    }

    private static double distance(MotionEvent downEvent, MotionEvent moveEvent) {
        int downActionIndex = downEvent.getActionIndex();
        int downPointerId = downEvent.getPointerId(downActionIndex);
        int moveActionIndex = moveEvent.findPointerIndex(downPointerId);
        if (moveActionIndex < 0) {
            return -1.0d;
        }
        return MathUtils.dist(downEvent.getX(downActionIndex), downEvent.getY(downActionIndex), moveEvent.getX(moveActionIndex), moveEvent.getY(moveActionIndex));
    }
}
