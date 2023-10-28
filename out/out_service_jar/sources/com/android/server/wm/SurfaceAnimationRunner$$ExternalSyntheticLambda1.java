package com.android.server.wm;

import android.view.Choreographer;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class SurfaceAnimationRunner$$ExternalSyntheticLambda1 implements Choreographer.FrameCallback {
    public final /* synthetic */ SurfaceAnimationRunner f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.server.wm.SurfaceAnimationRunner.continueStartingAnimations():void, com.android.server.wm.SurfaceAnimationRunner.lambda$startAnimation$2$com-android-server-wm-SurfaceAnimationRunner(com.android.server.wm.LocalAnimationAdapter$AnimationSpec, android.view.SurfaceControl, com.android.server.wm.SurfaceAnimationRunner$RunningAnimation):void, com.android.server.wm.SurfaceAnimationRunner.startAnimation(com.android.server.wm.LocalAnimationAdapter$AnimationSpec, android.view.SurfaceControl, android.view.SurfaceControl$Transaction, java.lang.Runnable):void] */
    public /* synthetic */ SurfaceAnimationRunner$$ExternalSyntheticLambda1(SurfaceAnimationRunner surfaceAnimationRunner) {
        this.f$0 = surfaceAnimationRunner;
    }

    @Override // android.view.Choreographer.FrameCallback
    public final void doFrame(long j) {
        SurfaceAnimationRunner.m8244$r8$lambda$u1Jh9N5fY2HKNOPRKT57txOp8s(this.f$0, j);
    }
}
