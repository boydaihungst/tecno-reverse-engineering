package com.android.server.wm;

import android.view.SurfaceControl;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.server.wm.FadeAnimationController;
import com.android.server.wm.LocalAnimationAdapter;
import com.android.server.wm.SurfaceAnimator;
/* loaded from: classes2.dex */
public class NavBarFadeAnimationController extends FadeAnimationController {
    private static final int FADE_IN_DURATION = 266;
    private static final int FADE_OUT_DURATION = 133;
    private Animation mFadeInAnimation;
    private SurfaceControl mFadeInParent;
    private Animation mFadeOutAnimation;
    private SurfaceControl mFadeOutParent;
    private final WindowState mNavigationBar;
    private boolean mPlaySequentially;
    private static final Interpolator FADE_IN_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.0f, 1.0f);
    private static final Interpolator FADE_OUT_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 1.0f, 1.0f);

    public NavBarFadeAnimationController(DisplayContent displayContent) {
        super(displayContent);
        this.mPlaySequentially = false;
        this.mNavigationBar = displayContent.getDisplayPolicy().getNavigationBar();
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        this.mFadeInAnimation = alphaAnimation;
        alphaAnimation.setDuration(266L);
        this.mFadeInAnimation.setInterpolator(FADE_IN_INTERPOLATOR);
        AlphaAnimation alphaAnimation2 = new AlphaAnimation(1.0f, 0.0f);
        this.mFadeOutAnimation = alphaAnimation2;
        alphaAnimation2.setDuration(133L);
        this.mFadeOutAnimation.setInterpolator(FADE_OUT_INTERPOLATOR);
    }

    @Override // com.android.server.wm.FadeAnimationController
    public Animation getFadeInAnimation() {
        return this.mFadeInAnimation;
    }

    @Override // com.android.server.wm.FadeAnimationController
    public Animation getFadeOutAnimation() {
        return this.mFadeOutAnimation;
    }

    @Override // com.android.server.wm.FadeAnimationController
    protected FadeAnimationController.FadeAnimationAdapter createAdapter(LocalAnimationAdapter.AnimationSpec animationSpec, boolean show, WindowToken windowToken) {
        return new NavFadeAnimationAdapter(animationSpec, windowToken.getSurfaceAnimationRunner(), show, windowToken, show ? this.mFadeInParent : this.mFadeOutParent);
    }

    public void fadeWindowToken(final boolean show) {
        AsyncRotationController controller = this.mDisplayContent.getAsyncRotationController();
        Runnable fadeAnim = new Runnable() { // from class: com.android.server.wm.NavBarFadeAnimationController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                NavBarFadeAnimationController.this.m8120x6e98d7b5(show);
            }
        };
        if (controller == null) {
            fadeAnim.run();
        } else if (!controller.isTargetToken(this.mNavigationBar.mToken)) {
            if (show) {
                controller.setOnShowRunnable(fadeAnim);
            } else {
                fadeAnim.run();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$fadeWindowToken$0$com-android-server-wm-NavBarFadeAnimationController  reason: not valid java name */
    public /* synthetic */ void m8120x6e98d7b5(boolean show) {
        fadeWindowToken(show, this.mNavigationBar.mToken, 64);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void fadeOutAndInSequentially(long totalDuration, SurfaceControl fadeOutParent, SurfaceControl fadeInParent) {
        this.mPlaySequentially = true;
        if (totalDuration > 0) {
            long fadeInDuration = (2 * totalDuration) / 3;
            this.mFadeOutAnimation.setDuration(totalDuration - fadeInDuration);
            this.mFadeInAnimation.setDuration(fadeInDuration);
        }
        this.mFadeOutParent = fadeOutParent;
        this.mFadeInParent = fadeInParent;
        fadeWindowToken(false);
    }

    /* loaded from: classes2.dex */
    protected class NavFadeAnimationAdapter extends FadeAnimationController.FadeAnimationAdapter {
        private SurfaceControl mParent;

        NavFadeAnimationAdapter(LocalAnimationAdapter.AnimationSpec windowAnimationSpec, SurfaceAnimationRunner surfaceAnimationRunner, boolean show, WindowToken token, SurfaceControl parent) {
            super(windowAnimationSpec, surfaceAnimationRunner, show, token);
            this.mParent = parent;
        }

        @Override // com.android.server.wm.LocalAnimationAdapter, com.android.server.wm.AnimationAdapter
        public void startAnimation(SurfaceControl animationLeash, SurfaceControl.Transaction t, int type, SurfaceAnimator.OnAnimationFinishedCallback finishCallback) {
            super.startAnimation(animationLeash, t, type, finishCallback);
            SurfaceControl surfaceControl = this.mParent;
            if (surfaceControl != null && surfaceControl.isValid()) {
                t.reparent(animationLeash, this.mParent);
                t.setLayer(animationLeash, Integer.MAX_VALUE);
            }
        }

        @Override // com.android.server.wm.FadeAnimationController.FadeAnimationAdapter, com.android.server.wm.AnimationAdapter
        public boolean shouldDeferAnimationFinish(Runnable endDeferFinishCallback) {
            if (NavBarFadeAnimationController.this.mPlaySequentially) {
                if (!this.mShow) {
                    NavBarFadeAnimationController.this.fadeWindowToken(true);
                    return false;
                }
                return false;
            }
            return super.shouldDeferAnimationFinish(endDeferFinishCallback);
        }
    }
}
