package com.android.server.wm;

import android.graphics.Rect;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.server.wm.Dimmer;
import com.android.server.wm.LocalAnimationAdapter;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class Dimmer {
    private static final int DEFAULT_DIM_ANIM_DURATION = 200;
    private static final String TAG = "WindowManager";
    DimState mDimState;
    private WindowContainer mHost;
    private WindowContainer mLastRequestedDimContainer;
    private float mSaturation;
    private final SurfaceAnimatorStarter mSurfaceAnimatorStarter;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface SurfaceAnimatorStarter {
        void startAnimation(SurfaceAnimator surfaceAnimator, SurfaceControl.Transaction transaction, AnimationAdapter animationAdapter, boolean z, int i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class DimAnimatable implements SurfaceAnimator.Animatable {
        private SurfaceControl mDimLayer;

        private DimAnimatable(SurfaceControl dimLayer) {
            this.mDimLayer = dimLayer;
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public SurfaceControl.Transaction getSyncTransaction() {
            return Dimmer.this.mHost.getSyncTransaction();
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public SurfaceControl.Transaction getPendingTransaction() {
            return Dimmer.this.mHost.getPendingTransaction();
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public void commitPendingTransaction() {
            Dimmer.this.mHost.commitPendingTransaction();
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public void onAnimationLeashCreated(SurfaceControl.Transaction t, SurfaceControl leash) {
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public void onAnimationLeashLost(SurfaceControl.Transaction t) {
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public SurfaceControl.Builder makeAnimationLeash() {
            return Dimmer.this.mHost.makeAnimationLeash();
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public SurfaceControl getAnimationLeashParent() {
            return Dimmer.this.mHost.getSurfaceControl();
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public SurfaceControl getSurfaceControl() {
            return this.mDimLayer;
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public SurfaceControl getParentSurfaceControl() {
            return Dimmer.this.mHost.getSurfaceControl();
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public int getSurfaceWidth() {
            return Dimmer.this.mHost.getSurfaceWidth();
        }

        @Override // com.android.server.wm.SurfaceAnimator.Animatable
        public int getSurfaceHeight() {
            return Dimmer.this.mHost.getSurfaceHeight();
        }

        void removeSurface() {
            SurfaceControl surfaceControl = this.mDimLayer;
            if (surfaceControl != null && surfaceControl.isValid()) {
                getSyncTransaction().remove(this.mDimLayer);
            }
            this.mDimLayer = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class DimState {
        boolean isVisible;
        SurfaceControl mDimLayer;
        boolean mDontReset;
        SurfaceAnimator mSurfaceAnimator;
        boolean mAnimateExit = true;
        boolean mDimming = true;

        DimState(SurfaceControl dimLayer) {
            this.mDimLayer = dimLayer;
            final DimAnimatable dimAnimatable = new DimAnimatable(dimLayer);
            this.mSurfaceAnimator = new SurfaceAnimator(dimAnimatable, new SurfaceAnimator.OnAnimationFinishedCallback() { // from class: com.android.server.wm.Dimmer$DimState$$ExternalSyntheticLambda0
                @Override // com.android.server.wm.SurfaceAnimator.OnAnimationFinishedCallback
                public final void onAnimationFinished(int i, AnimationAdapter animationAdapter) {
                    Dimmer.DimState.this.m7888lambda$new$0$comandroidserverwmDimmer$DimState(dimAnimatable, i, animationAdapter);
                }
            }, Dimmer.this.mHost.mWmService);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-wm-Dimmer$DimState  reason: not valid java name */
        public /* synthetic */ void m7888lambda$new$0$comandroidserverwmDimmer$DimState(DimAnimatable dimAnimatable, int type, AnimationAdapter anim) {
            if (!this.mDimming) {
                dimAnimatable.removeSurface();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Dimmer(WindowContainer host) {
        this(host, new SurfaceAnimatorStarter() { // from class: com.android.server.wm.Dimmer$$ExternalSyntheticLambda0
            @Override // com.android.server.wm.Dimmer.SurfaceAnimatorStarter
            public final void startAnimation(SurfaceAnimator surfaceAnimator, SurfaceControl.Transaction transaction, AnimationAdapter animationAdapter, boolean z, int i) {
                surfaceAnimator.startAnimation(transaction, animationAdapter, z, i);
            }
        });
    }

    Dimmer(WindowContainer host, SurfaceAnimatorStarter surfaceAnimatorStarter) {
        this.mSaturation = -2.0f;
        this.mHost = host;
        this.mSurfaceAnimatorStarter = surfaceAnimatorStarter;
    }

    private SurfaceControl makeDimLayer() {
        return this.mHost.makeChildSurface(null).setParent(this.mHost.getSurfaceControl()).setColorLayer().setName("Dim Layer for - " + this.mHost.getName()).setCallsite("Dimmer.makeDimLayer").build();
    }

    private DimState getDimState(WindowContainer container) {
        if (this.mDimState == null) {
            try {
                SurfaceControl ctl = makeDimLayer();
                DimState dimState = new DimState(ctl);
                this.mDimState = dimState;
                if (container == null) {
                    dimState.mDontReset = true;
                }
            } catch (Surface.OutOfResourcesException e) {
                Log.w("WindowManager", "OutOfResourcesException creating dim surface");
            }
        }
        this.mLastRequestedDimContainer = container;
        return this.mDimState;
    }

    private void dim(SurfaceControl.Transaction t, WindowContainer container, int relativeLayer, float alpha, int blurRadius) {
        DimState d = getDimState(container);
        if (d == null) {
            return;
        }
        if (container != null) {
            t.setRelativeLayer(d.mDimLayer, container.getSurfaceControl(), relativeLayer);
        } else {
            t.setLayer(d.mDimLayer, Integer.MAX_VALUE);
        }
        if (-2.0f != this.mSaturation) {
            Log.d("TranSaturation", " 2 dim setSaturation before: mSaturation=" + this.mSaturation + " blurRadius=" + blurRadius);
            t.setSaturation(d.mDimLayer, this.mSaturation, blurRadius);
        } else {
            t.setBackgroundBlurRadius(d.mDimLayer, blurRadius);
        }
        t.setAlpha(d.mDimLayer, alpha);
        d.mDimming = true;
    }

    void stopDim(SurfaceControl.Transaction t) {
        DimState dimState = this.mDimState;
        if (dimState != null) {
            t.hide(dimState.mDimLayer);
            this.mDimState.isVisible = false;
            this.mDimState.mDontReset = false;
        }
    }

    void dimAbove(SurfaceControl.Transaction t, float alpha) {
        dim(t, null, 1, alpha, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dimAbove(SurfaceControl.Transaction t, WindowContainer container, float alpha) {
        dim(t, container, 1, alpha, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dimBelow(SurfaceControl.Transaction t, WindowContainer container, float alpha, int blurRadius) {
        dim(t, container, -1, alpha, blurRadius);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSaturation(float saturation) {
        this.mSaturation = saturation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetDimStates() {
        DimState dimState = this.mDimState;
        if (dimState != null && !dimState.mDontReset) {
            this.mDimState.mDimming = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dontAnimateExit() {
        DimState dimState = this.mDimState;
        if (dimState != null) {
            dimState.mAnimateExit = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateDims(SurfaceControl.Transaction t, Rect bounds) {
        DimState dimState = this.mDimState;
        if (dimState == null) {
            return false;
        }
        if (!dimState.mDimming) {
            if (!this.mDimState.mAnimateExit) {
                if (this.mDimState.mDimLayer.isValid()) {
                    t.remove(this.mDimState.mDimLayer);
                }
            } else {
                startDimExit(this.mLastRequestedDimContainer, this.mDimState.mSurfaceAnimator, t);
            }
            this.mDimState = null;
            return false;
        }
        t.setPosition(this.mDimState.mDimLayer, bounds.left, bounds.top);
        t.setWindowCrop(this.mDimState.mDimLayer, bounds.width(), bounds.height());
        if (!this.mDimState.isVisible) {
            this.mDimState.isVisible = true;
            t.show(this.mDimState.mDimLayer);
            startDimEnter(this.mLastRequestedDimContainer, this.mDimState.mSurfaceAnimator, t);
        }
        return true;
    }

    private void startDimEnter(WindowContainer container, SurfaceAnimator animator, SurfaceControl.Transaction t) {
        startAnim(container, animator, t, 0.0f, 1.0f);
    }

    private void startDimExit(WindowContainer container, SurfaceAnimator animator, SurfaceControl.Transaction t) {
        startAnim(container, animator, t, 1.0f, 0.0f);
    }

    private void startAnim(WindowContainer container, SurfaceAnimator animator, SurfaceControl.Transaction t, float startAlpha, float endAlpha) {
        this.mSurfaceAnimatorStarter.startAnimation(animator, t, new LocalAnimationAdapter(new AlphaAnimationSpec(startAlpha, endAlpha, getDimDuration(container)), this.mHost.mWmService.mSurfaceAnimationRunner), false, 4);
    }

    private long getDimDuration(WindowContainer container) {
        if (container == null) {
            return 0L;
        }
        AnimationAdapter animationAdapter = container.mSurfaceAnimator.getAnimation();
        if (animationAdapter == null) {
            return 200L;
        }
        return animationAdapter.getDurationHint();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class AlphaAnimationSpec implements LocalAnimationAdapter.AnimationSpec {
        private final long mDuration;
        private final float mFromAlpha;
        private final float mToAlpha;

        AlphaAnimationSpec(float fromAlpha, float toAlpha, long duration) {
            this.mFromAlpha = fromAlpha;
            this.mToAlpha = toAlpha;
            this.mDuration = duration;
        }

        @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
        public long getDuration() {
            return this.mDuration;
        }

        @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
        public void apply(SurfaceControl.Transaction t, SurfaceControl sc, long currentPlayTime) {
            float fraction = getFraction((float) currentPlayTime);
            float f = this.mToAlpha;
            float f2 = this.mFromAlpha;
            float alpha = ((f - f2) * fraction) + f2;
            t.setAlpha(sc, alpha);
        }

        @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
        public void dump(PrintWriter pw, String prefix) {
            pw.print(prefix);
            pw.print("from=");
            pw.print(this.mFromAlpha);
            pw.print(" to=");
            pw.print(this.mToAlpha);
            pw.print(" duration=");
            pw.println(this.mDuration);
        }

        @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
        public void dumpDebugInner(ProtoOutputStream proto) {
            long token = proto.start(1146756268035L);
            proto.write(1108101562369L, this.mFromAlpha);
            proto.write(1108101562370L, this.mToAlpha);
            proto.write(1112396529667L, this.mDuration);
            proto.end(token);
        }
    }
}
