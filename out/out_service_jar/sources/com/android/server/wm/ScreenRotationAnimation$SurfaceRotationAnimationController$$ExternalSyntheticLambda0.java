package com.android.server.wm;

import com.android.server.wm.ScreenRotationAnimation;
import com.android.server.wm.SurfaceAnimator;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class ScreenRotationAnimation$SurfaceRotationAnimationController$$ExternalSyntheticLambda0 implements SurfaceAnimator.OnAnimationFinishedCallback {
    public final /* synthetic */ ScreenRotationAnimation.SurfaceRotationAnimationController f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.server.wm.ScreenRotationAnimation.SurfaceRotationAnimationController.startDisplayRotation():com.android.server.wm.SurfaceAnimator, com.android.server.wm.ScreenRotationAnimation.SurfaceRotationAnimationController.startEnterBlackFrameAnimation():com.android.server.wm.SurfaceAnimator, com.android.server.wm.ScreenRotationAnimation.SurfaceRotationAnimationController.startScreenshotAlphaAnimation():com.android.server.wm.SurfaceAnimator, com.android.server.wm.ScreenRotationAnimation.SurfaceRotationAnimationController.startScreenshotRotationAnimation():com.android.server.wm.SurfaceAnimator] */
    public /* synthetic */ ScreenRotationAnimation$SurfaceRotationAnimationController$$ExternalSyntheticLambda0(ScreenRotationAnimation.SurfaceRotationAnimationController surfaceRotationAnimationController) {
        this.f$0 = surfaceRotationAnimationController;
    }

    @Override // com.android.server.wm.SurfaceAnimator.OnAnimationFinishedCallback
    public final void onAnimationFinished(int i, AnimationAdapter animationAdapter) {
        ScreenRotationAnimation.SurfaceRotationAnimationController.m8226$r8$lambda$wJe6NDrB63BnRyvWEaFgRopM(this.f$0, i, animationAdapter);
    }
}
