package com.android.server.accessibility.magnification;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.server.accessibility.gestures.GestureMatcher;
import com.android.server.accessibility.gestures.GestureUtils;
/* loaded from: classes.dex */
class SimpleSwipe extends GestureMatcher {
    private final int mDetectionDurationMillis;
    private MotionEvent mLastDown;
    private final int mSwipeMinDistance;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SimpleSwipe(Context context) {
        super(102, new Handler(context.getMainLooper()), null);
        this.mSwipeMinDistance = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mDetectionDurationMillis = MagnificationGestureMatcher.getMagnificationMultiTapTimeout(context);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        this.mLastDown = MotionEvent.obtain(event);
        cancelAfter(this.mDetectionDurationMillis, event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onMove(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (gestureMatched(event, rawEvent, policyFlags)) {
            completeGesture(event, rawEvent, policyFlags);
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (gestureMatched(event, rawEvent, policyFlags)) {
            completeGesture(event, rawEvent, policyFlags);
        } else {
            cancelGesture(event, rawEvent, policyFlags);
        }
    }

    private boolean gestureMatched(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        MotionEvent motionEvent = this.mLastDown;
        return motionEvent != null && GestureUtils.distance(motionEvent, event) > ((double) this.mSwipeMinDistance);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public void clear() {
        MotionEvent motionEvent = this.mLastDown;
        if (motionEvent != null) {
            motionEvent.recycle();
        }
        this.mLastDown = null;
        super.clear();
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected String getGestureName() {
        return getClass().getSimpleName();
    }
}
