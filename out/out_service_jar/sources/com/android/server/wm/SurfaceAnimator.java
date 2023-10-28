package com.android.server.wm;

import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.wm.SurfaceFreezer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Supplier;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class SurfaceAnimator {
    public static final int ANIMATION_TYPE_ALL = -1;
    public static final int ANIMATION_TYPE_APP_TRANSITION = 1;
    public static final int ANIMATION_TYPE_DIMMER = 4;
    public static final int ANIMATION_TYPE_INSETS_CONTROL = 32;
    public static final int ANIMATION_TYPE_NONE = 0;
    public static final int ANIMATION_TYPE_RECENTS = 8;
    public static final int ANIMATION_TYPE_SCREEN_ROTATION = 2;
    public static final int ANIMATION_TYPE_STARTING_REVEAL = 128;
    public static final int ANIMATION_TYPE_TOKEN_TRANSFORM = 64;
    public static final int ANIMATION_TYPE_WINDOW_ANIMATION = 16;
    private static final String TAG = "WindowManager";
    final Animatable mAnimatable;
    private AnimationAdapter mAnimation;
    private Runnable mAnimationCancelledCallback;
    private boolean mAnimationFinished;
    private boolean mAnimationStartDelayed;
    private int mAnimationType;
    final OnAnimationFinishedCallback mInnerAnimationFinishedCallback;
    SurfaceControl mLeash;
    private final WindowManagerService mService;
    SurfaceFreezer.Snapshot mSnapshot;
    final OnAnimationFinishedCallback mStaticAnimationFinishedCallback;
    private OnAnimationFinishedCallback mSurfaceAnimationFinishedCallback;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface AnimationType {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface OnAnimationFinishedCallback {
        void onAnimationFinished(int i, AnimationAdapter animationAdapter);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceAnimator(Animatable animatable, OnAnimationFinishedCallback staticAnimationFinishedCallback, WindowManagerService service) {
        this.mAnimatable = animatable;
        this.mService = service;
        this.mStaticAnimationFinishedCallback = staticAnimationFinishedCallback;
        this.mInnerAnimationFinishedCallback = getFinishedCallback(staticAnimationFinishedCallback);
    }

    private OnAnimationFinishedCallback getFinishedCallback(final OnAnimationFinishedCallback staticAnimationFinishedCallback) {
        return new OnAnimationFinishedCallback() { // from class: com.android.server.wm.SurfaceAnimator$$ExternalSyntheticLambda0
            @Override // com.android.server.wm.SurfaceAnimator.OnAnimationFinishedCallback
            public final void onAnimationFinished(int i, AnimationAdapter animationAdapter) {
                SurfaceAnimator.this.m8258x48f3f856(staticAnimationFinishedCallback, i, animationAdapter);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getFinishedCallback$1$com-android-server-wm-SurfaceAnimator  reason: not valid java name */
    public /* synthetic */ void m8258x48f3f856(final OnAnimationFinishedCallback staticAnimationFinishedCallback, final int type, final AnimationAdapter anim) {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                SurfaceAnimator target = this.mService.mAnimationTransferMap.remove(anim);
                if (target != null) {
                    target.mInnerAnimationFinishedCallback.onAnimationFinished(type, anim);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else if (anim != this.mAnimation) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    Runnable resetAndInvokeFinish = new Runnable() { // from class: com.android.server.wm.SurfaceAnimator$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            SurfaceAnimator.this.m8257x7ff30115(anim, staticAnimationFinishedCallback, type);
                        }
                    };
                    if (!this.mAnimatable.shouldDeferAnimationFinish(resetAndInvokeFinish) && !anim.shouldDeferAnimationFinish(resetAndInvokeFinish)) {
                        resetAndInvokeFinish.run();
                    }
                    this.mAnimationFinished = true;
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getFinishedCallback$0$com-android-server-wm-SurfaceAnimator  reason: not valid java name */
    public /* synthetic */ void m8257x7ff30115(AnimationAdapter anim, OnAnimationFinishedCallback staticAnimationFinishedCallback, int type) {
        if (anim != this.mAnimation) {
            return;
        }
        OnAnimationFinishedCallback animationFinishCallback = this.mSurfaceAnimationFinishedCallback;
        reset(this.mAnimatable.getSyncTransaction(), true);
        if (staticAnimationFinishedCallback != null) {
            staticAnimationFinishedCallback.onAnimationFinished(type, anim);
        }
        if (animationFinishCallback != null) {
            animationFinishCallback.onAnimationFinished(type, anim);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAnimation(SurfaceControl.Transaction t, AnimationAdapter anim, boolean hidden, int type, OnAnimationFinishedCallback animationFinishedCallback, Runnable animationCancelledCallback, AnimationAdapter snapshotAnim, SurfaceFreezer freezer) {
        String str;
        cancelAnimation(t, true, true);
        this.mAnimation = anim;
        this.mAnimationType = type;
        this.mSurfaceAnimationFinishedCallback = animationFinishedCallback;
        this.mAnimationCancelledCallback = animationCancelledCallback;
        SurfaceControl surface = this.mAnimatable.getSurfaceControl();
        if (surface == null) {
            Slog.w("WindowManager", "Unable to start animation, surface is null or no children.");
            cancelAnimation();
            return;
        }
        SurfaceControl takeLeashForAnimation = freezer != null ? freezer.takeLeashForAnimation() : null;
        this.mLeash = takeLeashForAnimation;
        if (takeLeashForAnimation != null) {
            str = "WindowManager";
        } else {
            Animatable animatable = this.mAnimatable;
            str = "WindowManager";
            SurfaceControl createAnimationLeash = createAnimationLeash(animatable, surface, t, type, animatable.getSurfaceWidth(), this.mAnimatable.getSurfaceHeight(), 0, 0, hidden, this.mService.mTransactionFactory);
            this.mLeash = createAnimationLeash;
            this.mAnimatable.onAnimationLeashCreated(t, createAnimationLeash);
        }
        this.mAnimatable.onLeashAnimationStarting(t, this.mLeash);
        if (!this.mAnimationStartDelayed) {
            this.mAnimation.startAnimation(this.mLeash, t, type, this.mInnerAnimationFinishedCallback);
            if (ProtoLogImpl.isEnabled(ProtoLogGroup.WM_DEBUG_ANIM)) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                this.mAnimation.dump(pw, "");
                if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                    String protoLogParam0 = String.valueOf(this.mAnimatable);
                    String protoLogParam1 = String.valueOf(sw);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ANIM, -1969928125, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
                }
            }
            if (snapshotAnim != null) {
                SurfaceFreezer.Snapshot takeSnapshotForAnimation = freezer.takeSnapshotForAnimation();
                this.mSnapshot = takeSnapshotForAnimation;
                if (takeSnapshotForAnimation == null) {
                    Slog.e(str, "No snapshot target to start animation on for " + this.mAnimatable);
                } else {
                    takeSnapshotForAnimation.startAnimation(t, snapshotAnim, type);
                }
            }
        } else if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam02 = String.valueOf(this.mAnimatable);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ANIM, 215077284, 0, (String) null, new Object[]{protoLogParam02});
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startAnimation(SurfaceControl.Transaction t, AnimationAdapter anim, boolean hidden, int type) {
        startAnimation(t, anim, hidden, type, null, null, null, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startDelayingAnimationStart() {
        if (!isAnimating()) {
            this.mAnimationStartDelayed = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void endDelayingAnimationStart() {
        AnimationAdapter animationAdapter;
        boolean delayed = this.mAnimationStartDelayed;
        this.mAnimationStartDelayed = false;
        if (delayed && (animationAdapter = this.mAnimation) != null) {
            animationAdapter.startAnimation(this.mLeash, this.mAnimatable.getSyncTransaction(), this.mAnimationType, this.mInnerAnimationFinishedCallback);
            this.mAnimatable.commitPendingTransaction();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAnimating() {
        return this.mAnimation != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAnimationType() {
        return this.mAnimationType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AnimationAdapter getAnimation() {
        return this.mAnimation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelAnimation() {
        cancelAnimation(this.mAnimatable.getSyncTransaction(), false, true);
        this.mAnimatable.commitPendingTransaction();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLayer(SurfaceControl.Transaction t, int layer) {
        SurfaceControl surfaceControl = this.mLeash;
        if (surfaceControl == null) {
            surfaceControl = this.mAnimatable.getSurfaceControl();
        }
        t.setLayer(surfaceControl, layer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRelativeLayer(SurfaceControl.Transaction t, SurfaceControl relativeTo, int layer) {
        SurfaceControl surfaceControl = this.mLeash;
        if (surfaceControl == null) {
            surfaceControl = this.mAnimatable.getSurfaceControl();
        }
        t.setRelativeLayer(surfaceControl, relativeTo, layer);
    }

    void reparent(SurfaceControl.Transaction t, SurfaceControl newParent) {
        SurfaceControl surfaceControl = this.mLeash;
        if (surfaceControl == null) {
            surfaceControl = this.mAnimatable.getSurfaceControl();
        }
        t.reparent(surfaceControl, newParent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasLeash() {
        return this.mLeash != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transferAnimation(SurfaceAnimator from) {
        if (from.mLeash == null) {
            return;
        }
        SurfaceControl surface = this.mAnimatable.getSurfaceControl();
        SurfaceControl parent = this.mAnimatable.getAnimationLeashParent();
        if (surface == null || parent == null) {
            Slog.w("WindowManager", "Unable to transfer animation, surface or parent is null");
            cancelAnimation();
        } else if (from.mAnimationFinished) {
            Slog.w("WindowManager", "Unable to transfer animation, because " + from + " animation is finished");
        } else {
            endDelayingAnimationStart();
            SurfaceControl.Transaction t = this.mAnimatable.getSyncTransaction();
            cancelAnimation(t, true, true);
            this.mLeash = from.mLeash;
            this.mAnimation = from.mAnimation;
            this.mAnimationType = from.mAnimationType;
            this.mSurfaceAnimationFinishedCallback = from.mSurfaceAnimationFinishedCallback;
            this.mAnimationCancelledCallback = from.mAnimationCancelledCallback;
            from.cancelAnimation(t, false, false);
            t.reparent(surface, this.mLeash);
            t.reparent(this.mLeash, parent);
            this.mAnimatable.onAnimationLeashCreated(t, this.mLeash);
            this.mService.mAnimationTransferMap.put(this.mAnimation, this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAnimationStartDelayed() {
        return this.mAnimationStartDelayed;
    }

    private void cancelAnimation(SurfaceControl.Transaction t, boolean restarting, boolean forwardCancel) {
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam1 = String.valueOf(this.mAnimatable);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ANIM, 397862437, 3, (String) null, new Object[]{Boolean.valueOf(restarting), protoLogParam1});
        }
        SurfaceControl leash = this.mLeash;
        AnimationAdapter animation = this.mAnimation;
        int animationType = this.mAnimationType;
        OnAnimationFinishedCallback animationFinishedCallback = this.mSurfaceAnimationFinishedCallback;
        Runnable animationCancelledCallback = this.mAnimationCancelledCallback;
        SurfaceFreezer.Snapshot snapshot = this.mSnapshot;
        reset(t, false);
        if (animation != null) {
            if (!this.mAnimationStartDelayed && forwardCancel) {
                animation.onAnimationCancelled(leash);
                if (animationCancelledCallback != null) {
                    animationCancelledCallback.run();
                }
            }
            if (!restarting) {
                OnAnimationFinishedCallback onAnimationFinishedCallback = this.mStaticAnimationFinishedCallback;
                if (onAnimationFinishedCallback != null) {
                    onAnimationFinishedCallback.onAnimationFinished(animationType, animation);
                }
                if (animationFinishedCallback != null) {
                    animationFinishedCallback.onAnimationFinished(animationType, animation);
                }
            }
        }
        if (forwardCancel) {
            if (snapshot != null) {
                snapshot.cancelAnimation(t, false);
            }
            if (leash != null) {
                t.remove(leash);
                this.mService.scheduleAnimationLocked();
            }
        }
        if (!restarting) {
            this.mAnimationStartDelayed = false;
        }
    }

    private void reset(SurfaceControl.Transaction t, boolean destroyLeash) {
        this.mService.mAnimationTransferMap.remove(this.mAnimation);
        this.mAnimation = null;
        this.mSurfaceAnimationFinishedCallback = null;
        this.mAnimationType = 0;
        SurfaceFreezer.Snapshot snapshot = this.mSnapshot;
        this.mSnapshot = null;
        if (snapshot != null) {
            snapshot.cancelAnimation(t, !destroyLeash);
        }
        if (this.mLeash == null) {
            return;
        }
        SurfaceControl leash = this.mLeash;
        this.mLeash = null;
        boolean scheduleAnim = removeLeash(t, this.mAnimatable, leash, destroyLeash);
        this.mAnimationFinished = false;
        if (scheduleAnim) {
            this.mService.scheduleAnimationLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean removeLeash(SurfaceControl.Transaction t, Animatable animatable, SurfaceControl leash, boolean destroy) {
        boolean scheduleAnim = false;
        SurfaceControl surface = animatable.getSurfaceControl();
        SurfaceControl parent = animatable.getParentSurfaceControl();
        SurfaceControl curAnimationLeash = animatable.getAnimationLeash();
        boolean reparent = surface != null && (curAnimationLeash == null || curAnimationLeash.equals(leash));
        if (reparent) {
            if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                String protoLogParam0 = String.valueOf(parent);
                String protoLogParam1 = String.valueOf(animatable);
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ANIM, -319689203, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
            }
            if (surface.isValid() && parent != null && parent.isValid()) {
                t.reparent(surface, parent);
                scheduleAnim = true;
            }
        }
        if (destroy) {
            t.remove(leash);
            scheduleAnim = true;
        }
        if (reparent) {
            animatable.onAnimationLeashLost(t);
            return true;
        }
        return scheduleAnim;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SurfaceControl createAnimationLeash(Animatable animatable, SurfaceControl surface, SurfaceControl.Transaction t, int type, int width, int height, int x, int y, boolean hidden, Supplier<SurfaceControl.Transaction> transactionFactory) {
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(animatable);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ANIM, -208664771, 0, (String) null, new Object[]{protoLogParam0});
        }
        SurfaceControl.Builder builder = animatable.makeAnimationLeash().setParent(animatable.getAnimationLeashParent()).setName(surface + " - animation-leash of " + animationTypeToString(type)).setHidden(hidden).setEffectLayer().setCallsite("SurfaceAnimator.createAnimationLeash");
        SurfaceControl leash = builder.build();
        t.setWindowCrop(leash, width, height);
        t.setPosition(leash, x, y);
        t.show(leash);
        t.setAlpha(leash, hidden ? 0.0f : 1.0f);
        t.reparent(surface, leash);
        return leash;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        AnimationAdapter animationAdapter = this.mAnimation;
        if (animationAdapter != null) {
            animationAdapter.dumpDebug(proto, 1146756268035L);
        }
        SurfaceControl surfaceControl = this.mLeash;
        if (surfaceControl != null) {
            surfaceControl.dumpDebug(proto, 1146756268033L);
        }
        proto.write(1133871366146L, this.mAnimationStartDelayed);
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mLeash=");
        pw.print(this.mLeash);
        pw.print(" mAnimationType=" + animationTypeToString(this.mAnimationType));
        pw.println(this.mAnimationStartDelayed ? " mAnimationStartDelayed=true" : "");
        pw.print(prefix);
        pw.print("Animation: ");
        pw.println(this.mAnimation);
        AnimationAdapter animationAdapter = this.mAnimation;
        if (animationAdapter != null) {
            animationAdapter.dump(pw, prefix + "  ");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String animationTypeToString(int type) {
        switch (type) {
            case 0:
                return "none";
            case 1:
                return "app_transition";
            case 2:
                return "screen_rotation";
            case 4:
                return "dimmer";
            case 8:
                return "recents_animation";
            case 16:
                return "window_animation";
            case 32:
                return "insets_animation";
            case 64:
                return "token_transform";
            case 128:
                return "starting_reveal";
            default:
                return "unknown type:" + type;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Animatable {
        void commitPendingTransaction();

        SurfaceControl getAnimationLeashParent();

        SurfaceControl getParentSurfaceControl();

        SurfaceControl.Transaction getPendingTransaction();

        SurfaceControl getSurfaceControl();

        int getSurfaceHeight();

        int getSurfaceWidth();

        SurfaceControl.Transaction getSyncTransaction();

        SurfaceControl.Builder makeAnimationLeash();

        void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl);

        void onAnimationLeashLost(SurfaceControl.Transaction transaction);

        default void onLeashAnimationStarting(SurfaceControl.Transaction t, SurfaceControl leash) {
        }

        default SurfaceControl getAnimationLeash() {
            return null;
        }

        default boolean shouldDeferAnimationFinish(Runnable endDeferFinishCallback) {
            return false;
        }
    }
}
