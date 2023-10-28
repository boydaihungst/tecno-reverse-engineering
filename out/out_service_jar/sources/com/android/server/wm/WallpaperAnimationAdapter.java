package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WallpaperAnimationAdapter implements AnimationAdapter {
    private static final String TAG = "WallpaperAnimationAdapter";
    private Consumer<WallpaperAnimationAdapter> mAnimationCanceledRunnable;
    private SurfaceControl mCapturedLeash;
    private SurfaceAnimator.OnAnimationFinishedCallback mCapturedLeashFinishCallback;
    private long mDurationHint;
    private int mLastAnimationType;
    private long mStatusBarTransitionDelay;
    private RemoteAnimationTarget mTarget;
    private final WallpaperWindowToken mWallpaperToken;

    WallpaperAnimationAdapter(WallpaperWindowToken wallpaperToken, long durationHint, long statusBarTransitionDelay, Consumer<WallpaperAnimationAdapter> animationCanceledRunnable) {
        this.mWallpaperToken = wallpaperToken;
        this.mDurationHint = durationHint;
        this.mStatusBarTransitionDelay = statusBarTransitionDelay;
        this.mAnimationCanceledRunnable = animationCanceledRunnable;
    }

    public static RemoteAnimationTarget[] startWallpaperAnimations(DisplayContent displayContent, final long durationHint, final long statusBarTransitionDelay, final Consumer<WallpaperAnimationAdapter> animationCanceledRunnable, final ArrayList<WallpaperAnimationAdapter> adaptersOut) {
        if (!shouldStartWallpaperAnimation(displayContent)) {
            if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
                String protoLogParam0 = String.valueOf(displayContent);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 2024493888, 0, (String) null, new Object[]{protoLogParam0});
            }
            return new RemoteAnimationTarget[0];
        }
        final ArrayList<RemoteAnimationTarget> targets = new ArrayList<>();
        displayContent.forAllWallpaperWindows(new Consumer() { // from class: com.android.server.wm.WallpaperAnimationAdapter$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WallpaperAnimationAdapter.lambda$startWallpaperAnimations$0(durationHint, statusBarTransitionDelay, animationCanceledRunnable, targets, adaptersOut, (WallpaperWindowToken) obj);
            }
        });
        return (RemoteAnimationTarget[]) targets.toArray(new RemoteAnimationTarget[targets.size()]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$startWallpaperAnimations$0(long durationHint, long statusBarTransitionDelay, Consumer animationCanceledRunnable, ArrayList targets, ArrayList adaptersOut, WallpaperWindowToken wallpaperWindow) {
        WallpaperAnimationAdapter wallpaperAdapter = new WallpaperAnimationAdapter(wallpaperWindow, durationHint, statusBarTransitionDelay, animationCanceledRunnable);
        wallpaperWindow.startAnimation(wallpaperWindow.getPendingTransaction(), wallpaperAdapter, false, 16);
        targets.add(wallpaperAdapter.createRemoteAnimationTarget());
        adaptersOut.add(wallpaperAdapter);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean shouldStartWallpaperAnimation(DisplayContent displayContent) {
        return displayContent.mWallpaperController.isWallpaperVisible();
    }

    RemoteAnimationTarget createRemoteAnimationTarget() {
        RemoteAnimationTarget remoteAnimationTarget = new RemoteAnimationTarget(-1, -1, getLeash(), false, (Rect) null, (Rect) null, this.mWallpaperToken.getPrefixOrderIndex(), new Point(), (Rect) null, (Rect) null, this.mWallpaperToken.getWindowConfiguration(), true, (SurfaceControl) null, (Rect) null, (ActivityManager.RunningTaskInfo) null, false);
        this.mTarget = remoteAnimationTarget;
        return remoteAnimationTarget;
    }

    SurfaceControl getLeash() {
        return this.mCapturedLeash;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceAnimator.OnAnimationFinishedCallback getLeashFinishedCallback() {
        return this.mCapturedLeashFinishCallback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLastAnimationType() {
        return this.mLastAnimationType;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WallpaperWindowToken getToken() {
        return this.mWallpaperToken;
    }

    @Override // com.android.server.wm.AnimationAdapter
    public boolean getShowWallpaper() {
        return false;
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void startAnimation(SurfaceControl animationLeash, SurfaceControl.Transaction t, int type, SurfaceAnimator.OnAnimationFinishedCallback finishCallback) {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 1073230342, 0, (String) null, (Object[]) null);
        }
        t.setLayer(animationLeash, this.mWallpaperToken.getPrefixOrderIndex());
        this.mCapturedLeash = animationLeash;
        this.mCapturedLeashFinishCallback = finishCallback;
        this.mLastAnimationType = type;
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void onAnimationCancelled(SurfaceControl animationLeash) {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -760801764, 0, (String) null, (Object[]) null);
        }
        this.mAnimationCanceledRunnable.accept(this);
    }

    @Override // com.android.server.wm.AnimationAdapter
    public long getDurationHint() {
        return this.mDurationHint;
    }

    @Override // com.android.server.wm.AnimationAdapter
    public long getStatusBarTransitionsStartTime() {
        return SystemClock.uptimeMillis() + this.mStatusBarTransitionDelay;
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("token=");
        pw.println(this.mWallpaperToken);
        if (this.mTarget != null) {
            pw.print(prefix);
            pw.println("Target:");
            this.mTarget.dump(pw, prefix + "  ");
            return;
        }
        pw.print(prefix);
        pw.println("Target: null");
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void dumpDebug(ProtoOutputStream proto) {
        long token = proto.start(1146756268034L);
        RemoteAnimationTarget remoteAnimationTarget = this.mTarget;
        if (remoteAnimationTarget != null) {
            remoteAnimationTarget.dumpDebug(proto, 1146756268033L);
        }
        proto.end(token);
    }
}
