package com.android.server.accessibility.magnification;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class PanningScalingHandler extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
    private final boolean mBlockScroll;
    private final int mDisplayId;
    private boolean mEnable;
    private float mInitialScaleFactor = -1.0f;
    private final MagnificationDelegate mMagnificationDelegate;
    private final float mMaxScale;
    private final float mMinScale;
    private final ScaleGestureDetector mScaleGestureDetector;
    private boolean mScaling;
    private final float mScalingThreshold;
    private final GestureDetector mScrollGestureDetector;
    private static final String TAG = "PanningScalingHandler";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    /* loaded from: classes.dex */
    interface MagnificationDelegate {
        float getScale(int i);

        boolean processScroll(int i, float f, float f2);

        void setScale(int i, float f);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PanningScalingHandler(Context context, float maxScale, float minScale, boolean blockScroll, MagnificationDelegate magnificationDelegate) {
        this.mDisplayId = context.getDisplayId();
        this.mMaxScale = maxScale;
        this.mMinScale = minScale;
        this.mBlockScroll = blockScroll;
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context, this, Handler.getMain());
        this.mScaleGestureDetector = scaleGestureDetector;
        this.mScrollGestureDetector = new GestureDetector(context, this, Handler.getMain());
        scaleGestureDetector.setQuickScaleEnabled(false);
        this.mMagnificationDelegate = magnificationDelegate;
        TypedValue scaleValue = new TypedValue();
        context.getResources().getValue(17105110, scaleValue, false);
        this.mScalingThreshold = scaleValue.getFloat();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setEnabled(boolean enable) {
        clear();
        this.mEnable = enable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTouchEvent(MotionEvent motionEvent) {
        this.mScaleGestureDetector.onTouchEvent(motionEvent);
        this.mScrollGestureDetector.onTouchEvent(motionEvent);
    }

    @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnGestureListener
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (this.mEnable) {
            if (this.mBlockScroll && this.mScaling) {
                return true;
            }
            return this.mMagnificationDelegate.processScroll(this.mDisplayId, distanceX, distanceY);
        }
        return true;
    }

    @Override // android.view.ScaleGestureDetector.OnScaleGestureListener
    public boolean onScale(ScaleGestureDetector detector) {
        float scale;
        boolean z = DEBUG;
        if (z) {
            Slog.i(TAG, "onScale: triggered ");
        }
        if (!this.mScaling) {
            if (this.mInitialScaleFactor < 0.0f) {
                this.mInitialScaleFactor = detector.getScaleFactor();
                return false;
            }
            float deltaScale = detector.getScaleFactor() - this.mInitialScaleFactor;
            boolean z2 = Math.abs(deltaScale) > this.mScalingThreshold;
            this.mScaling = z2;
            return z2;
        }
        float initialScale = this.mMagnificationDelegate.getScale(this.mDisplayId);
        float targetScale = detector.getScaleFactor() * initialScale;
        if (targetScale > this.mMaxScale && targetScale > initialScale) {
            scale = this.mMaxScale;
        } else {
            float scale2 = this.mMinScale;
            if (targetScale < scale2 && targetScale < initialScale) {
                scale = this.mMinScale;
            } else {
                scale = targetScale;
            }
        }
        if (z) {
            Slog.i(TAG, "Scaled content to: " + scale + "x");
        }
        this.mMagnificationDelegate.setScale(this.mDisplayId, scale);
        return true;
    }

    @Override // android.view.ScaleGestureDetector.OnScaleGestureListener
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return this.mEnable;
    }

    @Override // android.view.ScaleGestureDetector.OnScaleGestureListener
    public void onScaleEnd(ScaleGestureDetector detector) {
        clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        this.mInitialScaleFactor = -1.0f;
        this.mScaling = false;
    }

    public String toString() {
        return "PanningScalingHandler{mInitialScaleFactor=" + this.mInitialScaleFactor + ", mScaling=" + this.mScaling + '}';
    }
}
