package com.android.server.accessibility.gestures;

import android.content.Context;
import android.graphics.PointF;
import android.net.INetd;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.server.accessibility.gestures.GestureMatcher;
import java.util.ArrayList;
import java.util.Arrays;
/* loaded from: classes.dex */
class MultiFingerSwipe extends GestureMatcher {
    public static final int DOWN = 3;
    public static final int LEFT = 0;
    private static final float MIN_CM_BETWEEN_SAMPLES = 0.25f;
    public static final int RIGHT = 1;
    public static final int UP = 2;
    private PointF[] mBase;
    private int mCurrentFingerCount;
    private int mDirection;
    private final float mMinPixelsBetweenSamplesX;
    private final float mMinPixelsBetweenSamplesY;
    private int[] mPointerIds;
    private PointF[] mPreviousGesturePoint;
    private final ArrayList<PointF>[] mStrokeBuffers;
    private int mTargetFingerCount;
    private boolean mTargetFingerCountReached;
    private int mTouchSlop;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MultiFingerSwipe(Context context, int fingerCount, int direction, int gesture, GestureMatcher.StateChangeListener listener) {
        super(gesture, new Handler(context.getMainLooper()), listener);
        this.mTargetFingerCountReached = false;
        this.mTargetFingerCount = fingerCount;
        this.mPointerIds = new int[fingerCount];
        this.mBase = new PointF[fingerCount];
        this.mPreviousGesturePoint = new PointF[fingerCount];
        this.mStrokeBuffers = new ArrayList[fingerCount];
        this.mDirection = direction;
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float pixelsPerCmX = displayMetrics.xdpi / GestureUtils.CM_PER_INCH;
        float pixelsPerCmY = displayMetrics.ydpi / GestureUtils.CM_PER_INCH;
        this.mMinPixelsBetweenSamplesX = pixelsPerCmX * MIN_CM_BETWEEN_SAMPLES;
        this.mMinPixelsBetweenSamplesY = MIN_CM_BETWEEN_SAMPLES * pixelsPerCmY;
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        clear();
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public void clear() {
        this.mTargetFingerCountReached = false;
        this.mCurrentFingerCount = 0;
        for (int i = 0; i < this.mTargetFingerCount; i++) {
            this.mPointerIds[i] = -1;
            PointF[] pointFArr = this.mBase;
            if (pointFArr[i] == null) {
                pointFArr[i] = new PointF();
            }
            this.mBase[i].x = Float.NaN;
            this.mBase[i].y = Float.NaN;
            PointF[] pointFArr2 = this.mPreviousGesturePoint;
            if (pointFArr2[i] == null) {
                pointFArr2[i] = new PointF();
            }
            this.mPreviousGesturePoint[i].x = Float.NaN;
            this.mPreviousGesturePoint[i].y = Float.NaN;
            ArrayList<PointF>[] arrayListArr = this.mStrokeBuffers;
            if (arrayListArr[i] == null) {
                arrayListArr[i] = new ArrayList<>(100);
            }
            this.mStrokeBuffers[i].clear();
        }
        super.clear();
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (this.mCurrentFingerCount > 0) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        this.mCurrentFingerCount = 1;
        int actionIndex = GestureUtils.getActionIndex(rawEvent);
        int pointerId = rawEvent.getPointerId(actionIndex);
        int pointerIndex = rawEvent.getPointerCount() - 1;
        if (pointerId < 0) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        int[] iArr = this.mPointerIds;
        if (iArr[pointerIndex] != -1) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        iArr[pointerIndex] = pointerId;
        if (Float.isNaN(this.mBase[pointerIndex].x) && Float.isNaN(this.mBase[pointerIndex].y)) {
            float x = rawEvent.getX(actionIndex);
            float y = rawEvent.getY(actionIndex);
            if (x < 0.0f || y < 0.0f) {
                cancelGesture(event, rawEvent, policyFlags);
                return;
            }
            this.mBase[pointerIndex].x = x;
            this.mBase[pointerIndex].y = y;
            this.mPreviousGesturePoint[pointerIndex].x = x;
            this.mPreviousGesturePoint[pointerIndex].y = y;
            return;
        }
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (event.getPointerCount() > this.mTargetFingerCount) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        int i = this.mCurrentFingerCount + 1;
        this.mCurrentFingerCount = i;
        if (i != rawEvent.getPointerCount()) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        if (this.mCurrentFingerCount == this.mTargetFingerCount) {
            this.mTargetFingerCountReached = true;
        }
        int actionIndex = GestureUtils.getActionIndex(rawEvent);
        int pointerId = rawEvent.getPointerId(actionIndex);
        if (pointerId < 0) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        int pointerIndex = this.mCurrentFingerCount - 1;
        int[] iArr = this.mPointerIds;
        if (iArr[pointerIndex] != -1) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        iArr[pointerIndex] = pointerId;
        if (Float.isNaN(this.mBase[pointerIndex].x) && Float.isNaN(this.mBase[pointerIndex].y)) {
            float x = rawEvent.getX(actionIndex);
            float y = rawEvent.getY(actionIndex);
            if (x < 0.0f || y < 0.0f) {
                cancelGesture(event, rawEvent, policyFlags);
                return;
            }
            this.mBase[pointerIndex].x = x;
            this.mBase[pointerIndex].y = y;
            this.mPreviousGesturePoint[pointerIndex].x = x;
            this.mPreviousGesturePoint[pointerIndex].y = y;
            return;
        }
        cancelGesture(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onPointerUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (!this.mTargetFingerCountReached) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        this.mCurrentFingerCount--;
        int actionIndex = GestureUtils.getActionIndex(event);
        int pointerId = event.getPointerId(actionIndex);
        if (pointerId < 0) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        int pointerIndex = Arrays.binarySearch(this.mPointerIds, pointerId);
        if (pointerIndex < 0) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        float x = rawEvent.getX(actionIndex);
        float y = rawEvent.getY(actionIndex);
        if (x < 0.0f || y < 0.0f) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        float dX = Math.abs(x - this.mPreviousGesturePoint[pointerIndex].x);
        float dY = Math.abs(y - this.mPreviousGesturePoint[pointerIndex].y);
        if (dX >= this.mMinPixelsBetweenSamplesX || dY >= this.mMinPixelsBetweenSamplesY) {
            this.mStrokeBuffers[pointerIndex].add(new PointF(x, y));
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onMove(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        int index;
        for (int pointerIndex = 0; pointerIndex < this.mTargetFingerCount; pointerIndex++) {
            int i = this.mPointerIds[pointerIndex];
            if (i != -1 && (index = rawEvent.findPointerIndex(i)) >= 0) {
                float x = rawEvent.getX(index);
                float y = rawEvent.getY(index);
                if (x < 0.0f || y < 0.0f) {
                    cancelGesture(event, rawEvent, policyFlags);
                    return;
                }
                float dX = Math.abs(x - this.mPreviousGesturePoint[pointerIndex].x);
                float dY = Math.abs(y - this.mPreviousGesturePoint[pointerIndex].y);
                double moveDelta = Math.hypot(Math.abs(x - this.mBase[pointerIndex].x), Math.abs(y - this.mBase[pointerIndex].y));
                if (getState() == 0) {
                    int i2 = this.mTargetFingerCount;
                    if (moveDelta < this.mTouchSlop * i2) {
                        continue;
                    } else if (this.mCurrentFingerCount == i2) {
                        int direction = toDirection(x - this.mBase[pointerIndex].x, y - this.mBase[pointerIndex].y);
                        if (direction != this.mDirection) {
                            cancelGesture(event, rawEvent, policyFlags);
                            return;
                        }
                        startGesture(event, rawEvent, policyFlags);
                        for (int i3 = 0; i3 < this.mTargetFingerCount; i3++) {
                            this.mStrokeBuffers[i3].add(new PointF(this.mBase[i3]));
                        }
                    } else {
                        cancelGesture(event, rawEvent, policyFlags);
                        return;
                    }
                } else if (getState() == 1) {
                    int direction2 = toDirection(x - this.mBase[pointerIndex].x, y - this.mBase[pointerIndex].y);
                    if (direction2 != this.mDirection) {
                        cancelGesture(event, rawEvent, policyFlags);
                        return;
                    } else if (dX >= this.mMinPixelsBetweenSamplesX || dY >= this.mMinPixelsBetweenSamplesY) {
                        this.mPreviousGesturePoint[pointerIndex].x = x;
                        this.mPreviousGesturePoint[pointerIndex].y = y;
                        this.mStrokeBuffers[pointerIndex].add(new PointF(x, y));
                    }
                } else {
                    continue;
                }
            }
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected void onUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (getState() != 1) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        this.mCurrentFingerCount = 0;
        int actionIndex = GestureUtils.getActionIndex(event);
        int pointerId = event.getPointerId(actionIndex);
        int pointerIndex = Arrays.binarySearch(this.mPointerIds, pointerId);
        if (pointerIndex < 0) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        float x = rawEvent.getX(actionIndex);
        float y = rawEvent.getY(actionIndex);
        if (x < 0.0f || y < 0.0f) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        float dX = Math.abs(x - this.mPreviousGesturePoint[pointerIndex].x);
        float dY = Math.abs(y - this.mPreviousGesturePoint[pointerIndex].y);
        if (dX >= this.mMinPixelsBetweenSamplesX || dY >= this.mMinPixelsBetweenSamplesY) {
            this.mStrokeBuffers[pointerIndex].add(new PointF(x, y));
        }
        recognizeGesture(event, rawEvent, policyFlags);
    }

    private void recognizeGesture(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        for (int i = 0; i < this.mTargetFingerCount; i++) {
            if (this.mStrokeBuffers[i].size() < 2) {
                Slog.d(getGestureName(), "Too few points.");
                cancelGesture(event, rawEvent, policyFlags);
                return;
            }
            ArrayList<PointF> path = this.mStrokeBuffers[i];
            if (!recognizeGesturePath(event, rawEvent, policyFlags, path)) {
                cancelGesture(event, rawEvent, policyFlags);
                return;
            }
        }
        completeGesture(event, rawEvent, policyFlags);
    }

    private boolean recognizeGesturePath(MotionEvent event, MotionEvent rawEvent, int policyFlags, ArrayList<PointF> path) {
        event.getDisplayId();
        for (int i = 0; i < path.size() - 1; i++) {
            PointF start = path.get(i);
            PointF end = path.get(i + 1);
            float dX = end.x - start.x;
            float dY = end.y - start.y;
            int direction = toDirection(dX, dY);
            if (direction != this.mDirection) {
                return false;
            }
        }
        return true;
    }

    private static int toDirection(float dX, float dY) {
        return Math.abs(dX) > Math.abs(dY) ? dX < 0.0f ? 0 : 1 : dY < 0.0f ? 2 : 3;
    }

    public static String directionToString(int direction) {
        switch (direction) {
            case 0:
                return "left";
            case 1:
                return "right";
            case 2:
                return INetd.IF_STATE_UP;
            case 3:
                return INetd.IF_STATE_DOWN;
            default:
                return "Unknown Direction";
        }
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    protected String getGestureName() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.mTargetFingerCount).append("-finger ");
        builder.append("Swipe ").append(directionToString(this.mDirection));
        return builder.toString();
    }

    @Override // com.android.server.accessibility.gestures.GestureMatcher
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        if (getState() != 3) {
            builder.append(", mBase: ").append(this.mBase.toString()).append(", mMinPixelsBetweenSamplesX:").append(this.mMinPixelsBetweenSamplesX).append(", mMinPixelsBetweenSamplesY:").append(this.mMinPixelsBetweenSamplesY);
        }
        return builder.toString();
    }
}
