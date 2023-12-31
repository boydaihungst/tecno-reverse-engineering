package com.android.internal.graphics.drawable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.HardwareRenderer;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RenderNode;
import android.graphics.drawable.Drawable;
import android.util.ArraySet;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;
import com.android.internal.R;
import com.android.internal.graphics.drawable.BackgroundBlurDrawable;
/* loaded from: classes4.dex */
public final class BackgroundBlurDrawable extends Drawable {
    private static final boolean DEBUG = true;
    private static final String TAG = BackgroundBlurDrawable.class.getSimpleName();
    private final Aggregator mAggregator;
    private float mAlpha;
    private int mBlurRadius;
    private float mCornerRadiusBL;
    private float mCornerRadiusBR;
    private float mCornerRadiusTL;
    private float mCornerRadiusTR;
    private final Paint mPaint;
    public final RenderNode.PositionUpdateListener mPositionUpdateListener;
    private final Rect mRect;
    private final Path mRectPath;
    private final RenderNode mRenderNode;
    float mSaturationBR;
    private final float[] mTmpRadii;
    private boolean mVisible;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.internal.graphics.drawable.BackgroundBlurDrawable$1  reason: invalid class name */
    /* loaded from: classes4.dex */
    public class AnonymousClass1 implements RenderNode.PositionUpdateListener {
        AnonymousClass1() {
        }

        @Override // android.graphics.RenderNode.PositionUpdateListener
        public void positionChanged(final long frameNumber, final int left, final int top, final int right, final int bottom) {
            Log.d(BackgroundBlurDrawable.TAG, "positionChanged rect (" + left + ", " + top + ", " + right + ", " + bottom + ") frameNumber:" + frameNumber);
            BackgroundBlurDrawable.this.mAggregator.onRenderNodePositionChanged(frameNumber, new Runnable() { // from class: com.android.internal.graphics.drawable.BackgroundBlurDrawable$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    BackgroundBlurDrawable.AnonymousClass1.this.m6591xe7379729(left, top, right, bottom, frameNumber);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$positionChanged$0$com-android-internal-graphics-drawable-BackgroundBlurDrawable$1  reason: not valid java name */
        public /* synthetic */ void m6591xe7379729(int left, int top, int right, int bottom, long frameNumber) {
            Log.d(BackgroundBlurDrawable.TAG, "onRenderNodePositionChanged rect (" + left + ", " + top + ", " + right + ", " + bottom + ") frameNumber:" + frameNumber);
            BackgroundBlurDrawable.this.mRect.set(left, top, right, bottom);
        }

        @Override // android.graphics.RenderNode.PositionUpdateListener
        public void positionLost(final long frameNumber) {
            Log.d(BackgroundBlurDrawable.TAG, "positionLost mRect: " + BackgroundBlurDrawable.this.mRect + " frameNumber:" + frameNumber);
            BackgroundBlurDrawable.this.mAggregator.onRenderNodePositionChanged(frameNumber, new Runnable() { // from class: com.android.internal.graphics.drawable.BackgroundBlurDrawable$1$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    BackgroundBlurDrawable.AnonymousClass1.this.m6592x65750f66(frameNumber);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$positionLost$1$com-android-internal-graphics-drawable-BackgroundBlurDrawable$1  reason: not valid java name */
        public /* synthetic */ void m6592x65750f66(long frameNumber) {
            BackgroundBlurDrawable.this.mRect.setEmpty();
            Log.d(BackgroundBlurDrawable.TAG, "onRenderNodePositionLost mRect: " + BackgroundBlurDrawable.this.mRect + " frameNumber:" + frameNumber);
        }
    }

    private BackgroundBlurDrawable(Aggregator aggregator) {
        Paint paint = new Paint();
        this.mPaint = paint;
        this.mRectPath = new Path();
        this.mTmpRadii = new float[8];
        this.mVisible = true;
        this.mSaturationBR = -2.0f;
        this.mAlpha = 1.0f;
        this.mRect = new Rect();
        AnonymousClass1 anonymousClass1 = new AnonymousClass1();
        this.mPositionUpdateListener = anonymousClass1;
        this.mAggregator = aggregator;
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        paint.setColor(0);
        paint.setAntiAlias(true);
        RenderNode renderNode = new RenderNode("BackgroundBlurDrawable");
        this.mRenderNode = renderNode;
        renderNode.addPositionUpdateListener(anonymousClass1);
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        if (this.mRectPath.isEmpty() || !isVisible() || getAlpha() == 0) {
            return;
        }
        canvas.drawPath(this.mRectPath, this.mPaint);
        canvas.drawRenderNode(this.mRenderNode);
    }

    public void setColor(int color) {
        this.mPaint.setColor(color);
    }

    @Override // android.graphics.drawable.Drawable
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (changed) {
            this.mVisible = visible;
            this.mAggregator.onBlurDrawableUpdated(this);
        }
        return changed;
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int alpha) {
        if (this.mAlpha != alpha / 255.0f) {
            this.mAlpha = alpha / 255.0f;
            invalidateSelf();
            this.mAggregator.onBlurDrawableUpdated(this);
        }
    }

    public void setBlurRadius(int blurRadius) {
        if (this.mBlurRadius != blurRadius) {
            this.mBlurRadius = blurRadius;
            invalidateSelf();
            this.mAggregator.onBlurDrawableUpdated(this);
        }
    }

    public void setBlurSaturation(float saturation) {
        if (this.mSaturationBR != saturation) {
            this.mSaturationBR = saturation;
            invalidateSelf();
            this.mAggregator.onSaturationUpdated(this);
        }
    }

    public void setCornerRadius(float cornerRadius) {
        setCornerRadius(cornerRadius, cornerRadius, cornerRadius, cornerRadius);
    }

    public void setCornerRadius(float cornerRadiusTL, float cornerRadiusTR, float cornerRadiusBL, float cornerRadiusBR) {
        if (this.mCornerRadiusTL != cornerRadiusTL || this.mCornerRadiusTR != cornerRadiusTR || this.mCornerRadiusBL != cornerRadiusBL || this.mCornerRadiusBR != cornerRadiusBR) {
            this.mCornerRadiusTL = cornerRadiusTL;
            this.mCornerRadiusTR = cornerRadiusTR;
            this.mCornerRadiusBL = cornerRadiusBL;
            this.mCornerRadiusBR = cornerRadiusBR;
            updatePath();
            invalidateSelf();
            this.mAggregator.onBlurDrawableUpdated(this);
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        this.mRenderNode.setPosition(left, top, right, bottom);
        updatePath();
    }

    private void updatePath() {
        float[] fArr = this.mTmpRadii;
        float f = this.mCornerRadiusTL;
        fArr[1] = f;
        fArr[0] = f;
        float f2 = this.mCornerRadiusTR;
        fArr[3] = f2;
        fArr[2] = f2;
        float f3 = this.mCornerRadiusBL;
        fArr[5] = f3;
        fArr[4] = f3;
        float f4 = this.mCornerRadiusBR;
        fArr[7] = f4;
        fArr[6] = f4;
        this.mRectPath.reset();
        if (getAlpha() == 0 || !isVisible()) {
            return;
        }
        Rect bounds = getBounds();
        this.mRectPath.addRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, this.mTmpRadii, Path.Direction.CW);
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        throw new IllegalArgumentException("not implemented");
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -3;
    }

    public String toString() {
        return "BackgroundBlurDrawable{blurRadius=" + this.mBlurRadius + ", mSaturationBR=" + this.mSaturationBR + ", corners={" + this.mCornerRadiusTL + "," + this.mCornerRadiusTR + "," + this.mCornerRadiusBL + "," + this.mCornerRadiusBR + "}, alpha=" + this.mAlpha + ", visible=" + this.mVisible + "}";
    }

    /* loaded from: classes4.dex */
    public static final class Aggregator {
        private boolean mHasUiUpdates;
        private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
        private final ViewRootImpl mViewRoot;
        private final Object mRtLock = new Object();
        private final ArraySet<BackgroundBlurDrawable> mDrawables = new ArraySet<>();
        private final LongSparseArray<ArraySet<Runnable>> mFrameRtUpdates = new LongSparseArray<>();
        private long mLastFrameNumber = 0;
        private BlurRegion[] mLastFrameBlurRegions = null;
        private BlurRegion[] mTmpBlurRegionsForFrame = new BlurRegion[0];

        public Aggregator(ViewRootImpl viewRoot) {
            this.mViewRoot = viewRoot;
        }

        public BackgroundBlurDrawable createBackgroundBlurDrawable(Context context) {
            BackgroundBlurDrawable drawable = new BackgroundBlurDrawable(this);
            drawable.setBlurRadius(context.getResources().getDimensionPixelSize(R.dimen.default_background_blur_radius));
            return drawable;
        }

        void onBlurDrawableUpdated(BackgroundBlurDrawable drawable) {
            boolean shouldBeDrawn = drawable.mAlpha != 0.0f && (drawable.mBlurRadius > 0 || drawable.mSaturationBR > -2.0f) && drawable.mVisible;
            boolean isDrawn = this.mDrawables.contains(drawable);
            Log.d(BackgroundBlurDrawable.TAG, "onBlurDrawableUpdated shouldBeDrawn :" + shouldBeDrawn + " isDrawn:" + isDrawn);
            if (shouldBeDrawn) {
                this.mHasUiUpdates = true;
                if (!isDrawn) {
                    this.mDrawables.add(drawable);
                    Log.d(BackgroundBlurDrawable.TAG, "Add " + drawable);
                } else {
                    Log.d(BackgroundBlurDrawable.TAG, "Update " + drawable);
                }
            } else if (!shouldBeDrawn && isDrawn) {
                this.mHasUiUpdates = true;
                this.mDrawables.remove(drawable);
                Log.d(BackgroundBlurDrawable.TAG, "Remove " + drawable);
            }
            if (this.mOnPreDrawListener == null && this.mViewRoot.getView() != null && hasRegions()) {
                registerPreDrawListener();
            }
        }

        void onSaturationUpdated(BackgroundBlurDrawable drawable) {
            boolean shouldBeDrawn = drawable.mSaturationBR > -2.0f && drawable.mVisible;
            boolean isDrawn = this.mDrawables.contains(drawable);
            if (shouldBeDrawn) {
                this.mHasUiUpdates = true;
                if (!isDrawn) {
                    this.mDrawables.add(drawable);
                    Log.d(BackgroundBlurDrawable.TAG, "Add " + drawable);
                } else {
                    Log.d(BackgroundBlurDrawable.TAG, "Update " + drawable);
                }
            } else if (!shouldBeDrawn && isDrawn) {
                this.mHasUiUpdates = true;
                this.mDrawables.remove(drawable);
                Log.d(BackgroundBlurDrawable.TAG, "Remove " + drawable);
            }
            if (this.mOnPreDrawListener == null && this.mViewRoot.getView() != null && hasRegions()) {
                registerPreDrawListener();
            }
        }

        private void registerPreDrawListener() {
            this.mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() { // from class: com.android.internal.graphics.drawable.BackgroundBlurDrawable$Aggregator$$ExternalSyntheticLambda1
                @Override // android.view.ViewTreeObserver.OnPreDrawListener
                public final boolean onPreDraw() {
                    return BackgroundBlurDrawable.Aggregator.this.m6594x23ebcc03();
                }
            };
            this.mViewRoot.getView().getViewTreeObserver().addOnPreDrawListener(this.mOnPreDrawListener);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$registerPreDrawListener$1$com-android-internal-graphics-drawable-BackgroundBlurDrawable$Aggregator  reason: not valid java name */
        public /* synthetic */ boolean m6594x23ebcc03() {
            final boolean hasUiUpdates = hasUpdates();
            if (hasUiUpdates || hasRegions()) {
                final BlurRegion[] blurRegionsForNextFrame = getBlurRegionsCopyForRT();
                this.mViewRoot.registerRtFrameCallback(new HardwareRenderer.FrameDrawingCallback() { // from class: com.android.internal.graphics.drawable.BackgroundBlurDrawable$Aggregator$$ExternalSyntheticLambda0
                    @Override // android.graphics.HardwareRenderer.FrameDrawingCallback
                    public final void onFrameDraw(long j) {
                        BackgroundBlurDrawable.Aggregator.this.m6593x97ad2e4(blurRegionsForNextFrame, hasUiUpdates, j);
                    }
                });
            }
            if (!hasRegions() && this.mViewRoot.getView() != null) {
                this.mViewRoot.getView().getViewTreeObserver().removeOnPreDrawListener(this.mOnPreDrawListener);
                this.mOnPreDrawListener = null;
                return true;
            }
            return true;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$registerPreDrawListener$0$com-android-internal-graphics-drawable-BackgroundBlurDrawable$Aggregator  reason: not valid java name */
        public /* synthetic */ void m6593x97ad2e4(BlurRegion[] blurRegionsForNextFrame, boolean hasUiUpdates, long frame) {
            synchronized (this.mRtLock) {
                this.mLastFrameNumber = frame;
                this.mLastFrameBlurRegions = blurRegionsForNextFrame;
                handleDispatchBlurTransactionLocked(frame, blurRegionsForNextFrame, hasUiUpdates);
            }
        }

        void onRenderNodePositionChanged(long frameNumber, Runnable update) {
            synchronized (this.mRtLock) {
                ArraySet<Runnable> frameRtUpdates = this.mFrameRtUpdates.get(frameNumber);
                if (frameRtUpdates == null) {
                    frameRtUpdates = new ArraySet<>();
                    this.mFrameRtUpdates.put(frameNumber, frameRtUpdates);
                }
                frameRtUpdates.add(update);
                if (this.mLastFrameNumber == frameNumber) {
                    Log.d(BackgroundBlurDrawable.TAG, " show mLastFrameNumber:" + this.mLastFrameNumber + " frameNumber:" + frameNumber);
                    handleDispatchBlurTransactionLocked(frameNumber, this.mLastFrameBlurRegions, true);
                }
            }
        }

        public boolean hasUpdates() {
            return this.mHasUiUpdates;
        }

        public boolean hasRegions() {
            return this.mDrawables.size() > 0;
        }

        public BlurRegion[] getBlurRegionsCopyForRT() {
            if (this.mHasUiUpdates) {
                this.mTmpBlurRegionsForFrame = new BlurRegion[this.mDrawables.size()];
                for (int i = 0; i < this.mDrawables.size(); i++) {
                    this.mTmpBlurRegionsForFrame[i] = new BlurRegion(this.mDrawables.valueAt(i));
                }
                this.mHasUiUpdates = false;
            }
            return this.mTmpBlurRegionsForFrame;
        }

        public float[][] getBlurRegionsForFrameLocked(long frameNumber, BlurRegion[] blurRegionsForFrame, boolean forceUpdate) {
            if (!forceUpdate && (this.mFrameRtUpdates.size() == 0 || this.mFrameRtUpdates.keyAt(0) > frameNumber)) {
                return null;
            }
            while (this.mFrameRtUpdates.size() != 0 && this.mFrameRtUpdates.keyAt(0) <= frameNumber) {
                ArraySet<Runnable> frameUpdates = this.mFrameRtUpdates.valueAt(0);
                this.mFrameRtUpdates.removeAt(0);
                for (int i = 0; i < frameUpdates.size(); i++) {
                    frameUpdates.valueAt(i).run();
                }
            }
            Log.d(BackgroundBlurDrawable.TAG, "Dispatching " + blurRegionsForFrame.length + " blur regions:");
            float[][] blurRegionsArray = new float[blurRegionsForFrame.length];
            for (int i2 = 0; i2 < blurRegionsArray.length; i2++) {
                blurRegionsArray[i2] = blurRegionsForFrame[i2].toFloatArray();
                Log.d(BackgroundBlurDrawable.TAG, blurRegionsForFrame[i2].toString());
            }
            return blurRegionsArray;
        }

        private void handleDispatchBlurTransactionLocked(long frameNumber, BlurRegion[] blurRegions, boolean forceUpdate) {
            float[][] blurRegionsArray = getBlurRegionsForFrameLocked(frameNumber, blurRegions, forceUpdate);
            if (blurRegionsArray != null) {
                this.mViewRoot.dispatchBlurRegions(blurRegionsArray, frameNumber);
            }
        }
    }

    /* loaded from: classes4.dex */
    public static final class BlurRegion {
        public final float alpha;
        public final int blurRadius;
        public final float cornerRadiusBL;
        public final float cornerRadiusBR;
        public final float cornerRadiusTL;
        public final float cornerRadiusTR;
        public final Rect rect;
        public final float saturationBR;

        BlurRegion(BackgroundBlurDrawable drawable) {
            this.alpha = drawable.mAlpha;
            this.blurRadius = drawable.mBlurRadius;
            this.saturationBR = drawable.mSaturationBR;
            this.cornerRadiusTL = drawable.mCornerRadiusTL;
            this.cornerRadiusTR = drawable.mCornerRadiusTR;
            this.cornerRadiusBL = drawable.mCornerRadiusBL;
            this.cornerRadiusBR = drawable.mCornerRadiusBR;
            this.rect = drawable.mRect;
        }

        float[] toFloatArray() {
            float[] floatArray = {this.blurRadius, this.alpha, this.rect.left, this.rect.top, this.rect.right, this.rect.bottom, this.cornerRadiusTL, this.cornerRadiusTR, this.cornerRadiusBL, this.cornerRadiusBR, this.saturationBR};
            return floatArray;
        }

        public String toString() {
            return "BlurRegion{blurRadius=" + this.blurRadius + "saturationBR=" + this.saturationBR + ", corners={" + this.cornerRadiusTL + "," + this.cornerRadiusTR + "," + this.cornerRadiusBL + "," + this.cornerRadiusBR + "}, alpha=" + this.alpha + ", rect=" + this.rect + "}";
        }
    }
}
