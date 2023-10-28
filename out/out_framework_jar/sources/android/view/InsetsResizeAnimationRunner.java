package android.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Insets;
import android.graphics.Rect;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.WindowInsetsAnimation;
import android.view.animation.Interpolator;
/* loaded from: classes3.dex */
public class InsetsResizeAnimationRunner implements InsetsAnimationControlRunner, InternalInsetsAnimationController, WindowInsetsAnimationControlListener {
    private final WindowInsetsAnimation mAnimation;
    private ValueAnimator mAnimator;
    private boolean mCancelled;
    private final InsetsAnimationControlCallbacks mController;
    private boolean mFinished;
    private final InsetsState mFromState;
    private final InsetsState mToState;
    private final int mTypes;

    public InsetsResizeAnimationRunner(Rect frame, InsetsState fromState, InsetsState toState, Interpolator interpolator, long duration, int types, InsetsAnimationControlCallbacks controller) {
        this.mFromState = fromState;
        this.mToState = toState;
        this.mTypes = types;
        this.mController = controller;
        WindowInsetsAnimation windowInsetsAnimation = new WindowInsetsAnimation(types, interpolator, duration);
        this.mAnimation = windowInsetsAnimation;
        windowInsetsAnimation.setAlpha(1.0f);
        Insets fromInsets = fromState.calculateInsets(frame, types, false);
        Insets toInsets = toState.calculateInsets(frame, types, false);
        controller.startAnimation(this, this, types, windowInsetsAnimation, new WindowInsetsAnimation.Bounds(Insets.min(fromInsets, toInsets), Insets.max(fromInsets, toInsets)));
    }

    @Override // android.view.InsetsAnimationControlRunner
    public int getTypes() {
        return this.mTypes;
    }

    @Override // android.view.InsetsAnimationControlRunner
    public int getControllingTypes() {
        return this.mTypes;
    }

    @Override // android.view.InsetsAnimationControlRunner
    public WindowInsetsAnimation getAnimation() {
        return this.mAnimation;
    }

    @Override // android.view.InsetsAnimationControlRunner
    public int getAnimationType() {
        return 3;
    }

    @Override // android.view.InsetsAnimationControlRunner
    public void cancel() {
        if (this.mCancelled || this.mFinished) {
            return;
        }
        this.mCancelled = true;
        ValueAnimator valueAnimator = this.mAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
    }

    @Override // android.view.WindowInsetsAnimationController
    public boolean isCancelled() {
        return this.mCancelled;
    }

    @Override // android.view.WindowInsetsAnimationControlListener
    public void onReady(WindowInsetsAnimationController controller, int types) {
        if (this.mCancelled) {
            return;
        }
        ValueAnimator ofFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.mAnimator = ofFloat;
        ofFloat.setDuration(this.mAnimation.getDurationMillis());
        this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: android.view.InsetsResizeAnimationRunner$$ExternalSyntheticLambda0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                InsetsResizeAnimationRunner.this.m4820lambda$onReady$0$androidviewInsetsResizeAnimationRunner(valueAnimator);
            }
        });
        this.mAnimator.addListener(new AnimatorListenerAdapter() { // from class: android.view.InsetsResizeAnimationRunner.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                InsetsResizeAnimationRunner.this.mFinished = true;
                InsetsResizeAnimationRunner.this.mController.scheduleApplyChangeInsets(InsetsResizeAnimationRunner.this);
            }
        });
        this.mAnimator.start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onReady$0$android-view-InsetsResizeAnimationRunner  reason: not valid java name */
    public /* synthetic */ void m4820lambda$onReady$0$androidviewInsetsResizeAnimationRunner(ValueAnimator animation) {
        this.mAnimation.setFraction(animation.getAnimatedFraction());
        this.mController.scheduleApplyChangeInsets(this);
    }

    @Override // android.view.InternalInsetsAnimationController
    public boolean applyChangeInsets(InsetsState outState) {
        if (this.mCancelled) {
            return false;
        }
        float fraction = this.mAnimation.getInterpolatedFraction();
        for (int type = 0; type < 24; type++) {
            InsetsSource fromSource = this.mFromState.peekSource(type);
            InsetsSource toSource = this.mToState.peekSource(type);
            if (fromSource != null && toSource != null) {
                Rect fromFrame = fromSource.getFrame();
                Rect toFrame = toSource.getFrame();
                Rect frame = new Rect((int) (fromFrame.left + ((toFrame.left - fromFrame.left) * fraction)), (int) (fromFrame.top + ((toFrame.top - fromFrame.top) * fraction)), (int) (fromFrame.right + ((toFrame.right - fromFrame.right) * fraction)), (int) (fromFrame.bottom + ((toFrame.bottom - fromFrame.bottom) * fraction)));
                InsetsSource source = new InsetsSource(type);
                source.setFrame(frame);
                source.setVisible(toSource.isVisible());
                outState.addSource(source);
            }
        }
        if (this.mFinished) {
            this.mController.notifyFinished(this, true);
        }
        return this.mFinished;
    }

    @Override // android.view.InsetsAnimationControlRunner
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1133871366145L, this.mCancelled);
        proto.write(1133871366146L, this.mFinished);
        proto.write(1138166333443L, "null");
        proto.write(1138166333444L, "null");
        proto.write(1108101562373L, this.mAnimation.getInterpolatedFraction());
        proto.write(1133871366150L, true);
        proto.write(1108101562375L, 1.0f);
        proto.write(1108101562376L, 1.0f);
        proto.end(token);
    }

    @Override // android.view.WindowInsetsAnimationController
    public Insets getHiddenStateInsets() {
        return Insets.NONE;
    }

    @Override // android.view.WindowInsetsAnimationController
    public Insets getShownStateInsets() {
        return Insets.NONE;
    }

    @Override // android.view.WindowInsetsAnimationController
    public Insets getCurrentInsets() {
        return Insets.NONE;
    }

    @Override // android.view.WindowInsetsAnimationController
    public float getCurrentFraction() {
        return 0.0f;
    }

    @Override // android.view.WindowInsetsAnimationController
    public float getCurrentAlpha() {
        return 0.0f;
    }

    @Override // android.view.WindowInsetsAnimationController
    public void setInsetsAndAlpha(Insets insets, float alpha, float fraction) {
    }

    @Override // android.view.WindowInsetsAnimationController
    public void finish(boolean shown) {
    }

    @Override // android.view.WindowInsetsAnimationController
    public boolean isFinished() {
        return false;
    }

    @Override // android.view.InsetsAnimationControlRunner
    public void notifyControlRevoked(int types) {
    }

    @Override // android.view.InsetsAnimationControlRunner
    public void updateSurfacePosition(SparseArray<InsetsSourceControl> controls) {
    }

    @Override // android.view.WindowInsetsAnimationController
    public boolean hasZeroInsetsIme() {
        return false;
    }

    @Override // android.view.InternalInsetsAnimationController
    public void setReadyDispatched(boolean dispatched) {
    }

    @Override // android.view.WindowInsetsAnimationControlListener
    public void onFinished(WindowInsetsAnimationController controller) {
    }

    @Override // android.view.WindowInsetsAnimationControlListener
    public void onCancelled(WindowInsetsAnimationController controller) {
    }
}
