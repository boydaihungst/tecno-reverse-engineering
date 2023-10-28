package com.android.server.wm;

import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class LocalAnimationAdapter implements AnimationAdapter {
    private final SurfaceAnimationRunner mAnimator;
    private final AnimationSpec mSpec;

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocalAnimationAdapter(AnimationSpec spec, SurfaceAnimationRunner animator) {
        this.mSpec = spec;
        this.mAnimator = animator;
    }

    @Override // com.android.server.wm.AnimationAdapter
    public boolean getShowWallpaper() {
        return this.mSpec.getShowWallpaper();
    }

    @Override // com.android.server.wm.AnimationAdapter
    public boolean getShowBackground() {
        return this.mSpec.getShowBackground();
    }

    @Override // com.android.server.wm.AnimationAdapter
    public int getBackgroundColor() {
        return this.mSpec.getBackgroundColor();
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void startAnimation(SurfaceControl animationLeash, SurfaceControl.Transaction t, final int type, final SurfaceAnimator.OnAnimationFinishedCallback finishCallback) {
        this.mAnimator.startAnimation(this.mSpec, animationLeash, t, new Runnable() { // from class: com.android.server.wm.LocalAnimationAdapter$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LocalAnimationAdapter.this.m8110x44ed85e6(finishCallback, type);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startAnimation$0$com-android-server-wm-LocalAnimationAdapter  reason: not valid java name */
    public /* synthetic */ void m8110x44ed85e6(SurfaceAnimator.OnAnimationFinishedCallback finishCallback, int type) {
        finishCallback.onAnimationFinished(type, this);
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void onAnimationCancelled(SurfaceControl animationLeash) {
        this.mAnimator.onAnimationCancelled(animationLeash);
    }

    @Override // com.android.server.wm.AnimationAdapter
    public long getDurationHint() {
        return this.mSpec.getDuration();
    }

    @Override // com.android.server.wm.AnimationAdapter
    public long getStatusBarTransitionsStartTime() {
        return this.mSpec.calculateStatusBarTransitionStartTime();
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void dump(PrintWriter pw, String prefix) {
        this.mSpec.dump(pw, prefix);
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void dumpDebug(ProtoOutputStream proto) {
        long token = proto.start(1146756268033L);
        this.mSpec.dumpDebug(proto, 1146756268033L);
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface AnimationSpec {
        void apply(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl, long j);

        void dump(PrintWriter printWriter, String str);

        void dumpDebugInner(ProtoOutputStream protoOutputStream);

        long getDuration();

        default boolean getShowWallpaper() {
            return false;
        }

        default boolean getShowBackground() {
            return false;
        }

        default int getBackgroundColor() {
            return 0;
        }

        default long calculateStatusBarTransitionStartTime() {
            return SystemClock.uptimeMillis();
        }

        default boolean canSkipFirstFrame() {
            return false;
        }

        default boolean needsEarlyWakeup() {
            return false;
        }

        default float getFraction(float currentPlayTime) {
            float duration = (float) getDuration();
            if (duration > 0.0f) {
                return currentPlayTime / duration;
            }
            return 1.0f;
        }

        default void dumpDebug(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            dumpDebugInner(proto);
            proto.end(token);
        }

        default WindowAnimationSpec asWindowAnimationSpec() {
            return null;
        }
    }
}
