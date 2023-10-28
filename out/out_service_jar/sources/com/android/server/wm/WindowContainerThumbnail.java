package com.android.server.wm;

import android.graphics.ColorSpace;
import android.graphics.GraphicBuffer;
import android.graphics.Point;
import android.hardware.HardwareBuffer;
import android.os.Process;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.job.controllers.JobStatus;
import com.android.server.wm.SurfaceAnimator;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowContainerThumbnail implements SurfaceAnimator.Animatable {
    private static final String TAG = "WindowManager";
    private final int mHeight;
    private final SurfaceAnimator mSurfaceAnimator;
    private SurfaceControl mSurfaceControl;
    private final int mWidth;
    private final WindowContainer mWindowContainer;

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowContainerThumbnail(SurfaceControl.Transaction t, WindowContainer container, HardwareBuffer thumbnailHeader) {
        this(t, container, thumbnailHeader, null);
    }

    WindowContainerThumbnail(SurfaceControl.Transaction t, WindowContainer container, HardwareBuffer thumbnailHeader, SurfaceAnimator animator) {
        this.mWindowContainer = container;
        if (animator != null) {
            this.mSurfaceAnimator = animator;
        } else {
            this.mSurfaceAnimator = new SurfaceAnimator(this, new SurfaceAnimator.OnAnimationFinishedCallback() { // from class: com.android.server.wm.WindowContainerThumbnail$$ExternalSyntheticLambda0
                @Override // com.android.server.wm.SurfaceAnimator.OnAnimationFinishedCallback
                public final void onAnimationFinished(int i, AnimationAdapter animationAdapter) {
                    WindowContainerThumbnail.this.onAnimationFinished(i, animationAdapter);
                }
            }, container.mWmService);
        }
        this.mWidth = thumbnailHeader.getWidth();
        this.mHeight = thumbnailHeader.getHeight();
        this.mSurfaceControl = container.makeChildSurface(container.getTopChild()).setName("thumbnail anim: " + container.toString()).setBLASTLayer().setFormat(-3).setMetadata(2, container.getWindowingMode()).setMetadata(1, Process.myUid()).setCallsite("WindowContainerThumbnail").build();
        if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
            String protoLogParam0 = String.valueOf(this.mSurfaceControl);
            ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, 531242746, 0, (String) null, new Object[]{protoLogParam0});
        }
        GraphicBuffer graphicBuffer = GraphicBuffer.createFromHardwareBuffer(thumbnailHeader);
        t.setBuffer(this.mSurfaceControl, graphicBuffer);
        t.setColorSpace(this.mSurfaceControl, ColorSpace.get(ColorSpace.Named.SRGB));
        t.show(this.mSurfaceControl);
        t.setLayer(this.mSurfaceControl, Integer.MAX_VALUE);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAnimation(SurfaceControl.Transaction t, Animation anim) {
        startAnimation(t, anim, (Point) null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAnimation(SurfaceControl.Transaction t, Animation anim, Point position) {
        anim.restrictDuration(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        anim.scaleCurrentDuration(this.mWindowContainer.mWmService.getTransitionAnimationScaleLocked());
        this.mSurfaceAnimator.startAnimation(t, new LocalAnimationAdapter(new WindowAnimationSpec(anim, position, this.mWindowContainer.getDisplayContent().mAppTransition.canSkipFirstFrame(), this.mWindowContainer.getDisplayContent().getWindowCornerRadius()), this.mWindowContainer.mWmService.mSurfaceAnimationRunner), false, 8);
    }

    void startAnimation(SurfaceControl.Transaction t, AnimationAdapter anim, boolean hidden) {
        this.mSurfaceAnimator.startAnimation(t, anim, hidden, 8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onAnimationFinished(int type, AnimationAdapter anim) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShowing(SurfaceControl.Transaction pendingTransaction, boolean show) {
        if (show) {
            pendingTransaction.show(this.mSurfaceControl);
        } else {
            pendingTransaction.hide(this.mSurfaceControl);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroy() {
        this.mSurfaceAnimator.cancelAnimation();
        getPendingTransaction().remove(this.mSurfaceControl);
        this.mSurfaceControl = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, this.mWidth);
        proto.write(1120986464258L, this.mHeight);
        if (this.mSurfaceAnimator.isAnimating()) {
            this.mSurfaceAnimator.dumpDebug(proto, 1146756268035L);
        }
        proto.end(token);
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl.Transaction getSyncTransaction() {
        return this.mWindowContainer.getSyncTransaction();
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl.Transaction getPendingTransaction() {
        return this.mWindowContainer.getPendingTransaction();
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public void commitPendingTransaction() {
        this.mWindowContainer.commitPendingTransaction();
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public void onAnimationLeashCreated(SurfaceControl.Transaction t, SurfaceControl leash) {
        t.setLayer(leash, Integer.MAX_VALUE);
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public void onAnimationLeashLost(SurfaceControl.Transaction t) {
        t.hide(this.mSurfaceControl);
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl.Builder makeAnimationLeash() {
        WindowContainer windowContainer = this.mWindowContainer;
        return windowContainer.makeChildSurface(windowContainer.getTopChild());
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl getAnimationLeashParent() {
        return this.mWindowContainer.getAnimationLeashParent();
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl getParentSurfaceControl() {
        return this.mWindowContainer.getParentSurfaceControl();
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public int getSurfaceWidth() {
        return this.mWidth;
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public int getSurfaceHeight() {
        return this.mHeight;
    }
}
