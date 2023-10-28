package com.android.server.wm;

import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import com.android.server.wm.LocalAnimationAdapter;
import defpackage.CompanionAppsPermissions;
import java.io.PrintWriter;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class WindowAnimationSpec implements LocalAnimationAdapter.AnimationSpec {
    private Animation mAnimation;
    private final boolean mCanSkipFirstFrame;
    private final boolean mIsAppAnimation;
    private final Point mPosition;
    private final Rect mRootTaskBounds;
    private int mRootTaskClipMode;
    private final ThreadLocal<TmpValues> mThreadLocalTmps;
    private final Rect mTmpRect;
    private final float mWindowCornerRadius;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TmpValues lambda$new$0() {
        return new TmpValues();
    }

    public WindowAnimationSpec(Animation animation, Point position, boolean canSkipFirstFrame, float windowCornerRadius) {
        this(animation, position, null, canSkipFirstFrame, 1, false, windowCornerRadius);
    }

    public WindowAnimationSpec(Animation animation, Point position, Rect rootTaskBounds, boolean canSkipFirstFrame, int rootTaskClipMode, boolean isAppAnimation, float windowCornerRadius) {
        Point point = new Point();
        this.mPosition = point;
        this.mThreadLocalTmps = ThreadLocal.withInitial(new Supplier() { // from class: com.android.server.wm.WindowAnimationSpec$$ExternalSyntheticLambda0
            @Override // java.util.function.Supplier
            public final Object get() {
                return WindowAnimationSpec.lambda$new$0();
            }
        });
        Rect rect = new Rect();
        this.mRootTaskBounds = rect;
        this.mTmpRect = new Rect();
        this.mAnimation = animation;
        if (position != null) {
            point.set(position.x, position.y);
        }
        this.mWindowCornerRadius = windowCornerRadius;
        this.mCanSkipFirstFrame = canSkipFirstFrame;
        this.mIsAppAnimation = isAppAnimation;
        this.mRootTaskClipMode = rootTaskClipMode;
        if (rootTaskBounds != null) {
            rect.set(rootTaskBounds);
        }
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public WindowAnimationSpec asWindowAnimationSpec() {
        return this;
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public boolean getShowWallpaper() {
        return this.mAnimation.getShowWallpaper();
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public boolean getShowBackground() {
        return this.mAnimation.getShowBackdrop();
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public int getBackgroundColor() {
        return this.mAnimation.getBackdropColor();
    }

    public boolean hasExtension() {
        return this.mAnimation.hasExtension();
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public long getDuration() {
        return this.mAnimation.computeDurationHint();
    }

    public Rect getRootTaskBounds() {
        return this.mRootTaskBounds;
    }

    public Animation getAnimation() {
        return this.mAnimation;
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public void apply(SurfaceControl.Transaction t, SurfaceControl leash, long currentPlayTime) {
        TmpValues tmp = this.mThreadLocalTmps.get();
        tmp.transformation.clear();
        this.mAnimation.getTransformation(currentPlayTime, tmp.transformation);
        tmp.transformation.getMatrix().postTranslate(this.mPosition.x, this.mPosition.y);
        t.setMatrix(leash, tmp.transformation.getMatrix(), tmp.floats);
        t.setAlpha(leash, tmp.transformation.getAlpha());
        boolean cropSet = false;
        if (this.mRootTaskClipMode == 1) {
            if (tmp.transformation.hasClipRect()) {
                Rect clipRect = tmp.transformation.getClipRect();
                accountForExtension(tmp.transformation, clipRect);
                t.setWindowCrop(leash, clipRect);
                cropSet = true;
            }
        } else {
            this.mTmpRect.set(this.mRootTaskBounds);
            if (tmp.transformation.hasClipRect()) {
                this.mTmpRect.intersect(tmp.transformation.getClipRect());
            }
            accountForExtension(tmp.transformation, this.mTmpRect);
            t.setWindowCrop(leash, this.mTmpRect);
            cropSet = true;
        }
        if (cropSet && this.mAnimation.hasRoundedCorners()) {
            float f = this.mWindowCornerRadius;
            if (f > 0.0f) {
                t.setCornerRadius(leash, f);
            }
        }
    }

    private void accountForExtension(Transformation transformation, Rect clipRect) {
        Insets extensionInsets = Insets.min(transformation.getInsets(), Insets.NONE);
        if (!extensionInsets.equals(Insets.NONE)) {
            clipRect.inset(extensionInsets);
        }
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public long calculateStatusBarTransitionStartTime() {
        TranslateAnimation openTranslateAnimation = findTranslateAnimation(this.mAnimation);
        if (openTranslateAnimation != null) {
            if (openTranslateAnimation.isXAxisTransition() && openTranslateAnimation.isFullWidthTranslate()) {
                float t = findMiddleOfTranslationFraction(openTranslateAnimation.getInterpolator());
                return ((SystemClock.uptimeMillis() + openTranslateAnimation.getStartOffset()) + (((float) openTranslateAnimation.getDuration()) * t)) - 60;
            }
            float t2 = findAlmostThereFraction(openTranslateAnimation.getInterpolator());
            return ((SystemClock.uptimeMillis() + openTranslateAnimation.getStartOffset()) + (((float) openTranslateAnimation.getDuration()) * t2)) - 120;
        }
        return SystemClock.uptimeMillis();
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public boolean canSkipFirstFrame() {
        return this.mCanSkipFirstFrame;
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public boolean needsEarlyWakeup() {
        return this.mIsAppAnimation;
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println(this.mAnimation);
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public void dumpDebugInner(ProtoOutputStream proto) {
        long token = proto.start(1146756268033L);
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.mAnimation.toString());
        proto.end(token);
    }

    private static TranslateAnimation findTranslateAnimation(Animation animation) {
        if (animation instanceof TranslateAnimation) {
            return (TranslateAnimation) animation;
        }
        if (animation instanceof AnimationSet) {
            AnimationSet set = (AnimationSet) animation;
            for (int i = 0; i < set.getAnimations().size(); i++) {
                Animation a = set.getAnimations().get(i);
                if (a instanceof TranslateAnimation) {
                    return (TranslateAnimation) a;
                }
            }
            return null;
        }
        return null;
    }

    private static float findAlmostThereFraction(Interpolator interpolator) {
        return findInterpolationAdjustedTargetFraction(interpolator, 0.99f, 0.01f);
    }

    private float findMiddleOfTranslationFraction(Interpolator interpolator) {
        return findInterpolationAdjustedTargetFraction(interpolator, 0.5f, 0.01f);
    }

    private static float findInterpolationAdjustedTargetFraction(Interpolator interpolator, float target, float epsilon) {
        float val = 0.5f;
        for (float adj = 0.25f; adj >= epsilon; adj /= 2.0f) {
            if (interpolator.getInterpolation(val) < target) {
                val += adj;
            } else {
                val -= adj;
            }
        }
        return val;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class TmpValues {
        final float[] floats;
        final Transformation transformation;

        private TmpValues() {
            this.transformation = new Transformation();
            this.floats = new float[9];
        }
    }
}
