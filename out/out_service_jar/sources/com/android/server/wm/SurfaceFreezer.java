package com.android.server.wm;

import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.util.Slog;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.wm.SurfaceAnimator;
import com.android.server.wm.SurfaceFreezer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SurfaceFreezer {
    private static final String TAG = "SurfaceFreezer";
    private final Freezable mAnimatable;
    SurfaceControl mLeash;
    private final WindowManagerService mWmService;
    Snapshot mSnapshot = null;
    final Rect mFreezeBounds = new Rect();

    /* loaded from: classes2.dex */
    public interface Freezable extends SurfaceAnimator.Animatable {
        SurfaceControl getFreezeSnapshotTarget();

        void onUnfrozen();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceFreezer(Freezable animatable, WindowManagerService service) {
        this.mAnimatable = animatable;
        this.mWmService = service;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void freeze(SurfaceControl.Transaction t, Rect startBounds, Point relativePosition, SurfaceControl freezeTarget) {
        reset(t);
        this.mFreezeBounds.set(startBounds);
        Freezable freezable = this.mAnimatable;
        SurfaceControl createAnimationLeash = SurfaceAnimator.createAnimationLeash(freezable, freezable.getSurfaceControl(), t, 2, startBounds.width(), startBounds.height(), relativePosition.x, relativePosition.y, false, this.mWmService.mTransactionFactory);
        this.mLeash = createAnimationLeash;
        this.mAnimatable.onAnimationLeashCreated(t, createAnimationLeash);
        SurfaceControl freezeTarget2 = freezeTarget != null ? freezeTarget : this.mAnimatable.getFreezeSnapshotTarget();
        if (freezeTarget2 != null) {
            SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer = createSnapshotBufferInner(freezeTarget2, startBounds);
            HardwareBuffer buffer = screenshotBuffer == null ? null : screenshotBuffer.getHardwareBuffer();
            if (buffer == null || buffer.getWidth() <= 1 || buffer.getHeight() <= 1) {
                Slog.w(TAG, "Failed to capture screenshot for " + this.mAnimatable);
                unfreeze(t);
                return;
            }
            this.mSnapshot = new Snapshot(t, screenshotBuffer, this.mLeash);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl takeLeashForAnimation() {
        SurfaceControl out = this.mLeash;
        this.mLeash = null;
        return out;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Snapshot takeSnapshotForAnimation() {
        Snapshot out = this.mSnapshot;
        this.mSnapshot = null;
        return out;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unfreeze(SurfaceControl.Transaction t) {
        unfreezeInner(t);
        this.mAnimatable.onUnfrozen();
    }

    private void unfreezeInner(SurfaceControl.Transaction t) {
        Snapshot snapshot = this.mSnapshot;
        if (snapshot != null) {
            snapshot.cancelAnimation(t, false);
            this.mSnapshot = null;
        }
        if (this.mLeash == null) {
            return;
        }
        SurfaceControl leash = this.mLeash;
        this.mLeash = null;
        boolean scheduleAnim = SurfaceAnimator.removeLeash(t, this.mAnimatable, leash, true);
        if (scheduleAnim) {
            this.mWmService.scheduleAnimationLocked();
        }
    }

    private void reset(SurfaceControl.Transaction t) {
        Snapshot snapshot = this.mSnapshot;
        if (snapshot != null) {
            snapshot.destroy(t);
            this.mSnapshot = null;
        }
        SurfaceControl surfaceControl = this.mLeash;
        if (surfaceControl != null) {
            t.remove(surfaceControl);
            this.mLeash = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLayer(SurfaceControl.Transaction t, int layer) {
        SurfaceControl surfaceControl = this.mLeash;
        if (surfaceControl != null) {
            t.setLayer(surfaceControl, layer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRelativeLayer(SurfaceControl.Transaction t, SurfaceControl relativeTo, int layer) {
        SurfaceControl surfaceControl = this.mLeash;
        if (surfaceControl != null) {
            t.setRelativeLayer(surfaceControl, relativeTo, layer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasLeash() {
        return this.mLeash != null;
    }

    private static SurfaceControl.ScreenshotHardwareBuffer createSnapshotBuffer(SurfaceControl target, Rect bounds) {
        Rect cropBounds = null;
        if (bounds != null) {
            cropBounds = new Rect(bounds);
            cropBounds.offsetTo(0, 0);
        }
        SurfaceControl.LayerCaptureArgs captureArgs = new SurfaceControl.LayerCaptureArgs.Builder(target).setSourceCrop(cropBounds).setCaptureSecureLayers(true).setAllowProtected(true).build();
        return SurfaceControl.captureLayers(captureArgs);
    }

    SurfaceControl.ScreenshotHardwareBuffer createSnapshotBufferInner(SurfaceControl target, Rect bounds) {
        if (TranFoldWMCustody.instance().disableCreateSnapshotBufferInner(target, bounds)) {
            return null;
        }
        return createSnapshotBuffer(target, bounds);
    }

    GraphicBuffer createFromHardwareBufferInner(SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer) {
        return GraphicBuffer.createFromHardwareBuffer(screenshotBuffer.getHardwareBuffer());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class Snapshot {
        private AnimationAdapter mAnimation;
        private SurfaceControl mSurfaceControl;

        Snapshot(SurfaceControl.Transaction t, SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer, SurfaceControl parent) {
            GraphicBuffer graphicBuffer = SurfaceFreezer.this.createFromHardwareBufferInner(screenshotBuffer);
            this.mSurfaceControl = SurfaceFreezer.this.mAnimatable.makeAnimationLeash().setName("snapshot anim: " + SurfaceFreezer.this.mAnimatable.toString()).setFormat(-3).setParent(parent).setSecure(screenshotBuffer.containsSecureLayers()).setCallsite("SurfaceFreezer.Snapshot").setBLASTLayer().build();
            if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
                String protoLogParam0 = String.valueOf(this.mSurfaceControl);
                ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, -668956537, 0, (String) null, new Object[]{protoLogParam0});
            }
            t.setBuffer(this.mSurfaceControl, graphicBuffer);
            t.setColorSpace(this.mSurfaceControl, screenshotBuffer.getColorSpace());
            t.show(this.mSurfaceControl);
            t.setLayer(this.mSurfaceControl, Integer.MAX_VALUE);
        }

        void destroy(SurfaceControl.Transaction t) {
            SurfaceControl surfaceControl = this.mSurfaceControl;
            if (surfaceControl == null) {
                return;
            }
            t.remove(surfaceControl);
            this.mSurfaceControl = null;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void startAnimation(SurfaceControl.Transaction t, AnimationAdapter anim, int type) {
            cancelAnimation(t, true);
            this.mAnimation = anim;
            SurfaceControl surfaceControl = this.mSurfaceControl;
            if (surfaceControl == null) {
                cancelAnimation(t, false);
            } else {
                anim.startAnimation(surfaceControl, t, type, new SurfaceAnimator.OnAnimationFinishedCallback() { // from class: com.android.server.wm.SurfaceFreezer$Snapshot$$ExternalSyntheticLambda0
                    @Override // com.android.server.wm.SurfaceAnimator.OnAnimationFinishedCallback
                    public final void onAnimationFinished(int i, AnimationAdapter animationAdapter) {
                        SurfaceFreezer.Snapshot.lambda$startAnimation$0(i, animationAdapter);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$startAnimation$0(int typ, AnimationAdapter ani) {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void cancelAnimation(SurfaceControl.Transaction t, boolean restarting) {
            SurfaceControl leash = this.mSurfaceControl;
            AnimationAdapter animation = this.mAnimation;
            this.mAnimation = null;
            if (animation != null) {
                animation.onAnimationCancelled(leash);
            }
            if (!restarting) {
                destroy(t);
            }
        }
    }
}
