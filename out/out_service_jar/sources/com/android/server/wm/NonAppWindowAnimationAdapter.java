package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class NonAppWindowAnimationAdapter implements AnimationAdapter {
    private SurfaceControl mCapturedLeash;
    private SurfaceAnimator.OnAnimationFinishedCallback mCapturedLeashFinishCallback;
    private long mDurationHint;
    private int mLastAnimationType;
    private long mStatusBarTransitionDelay;
    private RemoteAnimationTarget mTarget;
    private final WindowContainer mWindowContainer;

    @Override // com.android.server.wm.AnimationAdapter
    public boolean getShowWallpaper() {
        return false;
    }

    NonAppWindowAnimationAdapter(WindowContainer w, long durationHint, long statusBarTransitionDelay) {
        this.mWindowContainer = w;
        this.mDurationHint = durationHint;
        this.mStatusBarTransitionDelay = statusBarTransitionDelay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static RemoteAnimationTarget[] startNonAppWindowAnimations(WindowManagerService service, DisplayContent displayContent, int transit, long durationHint, long statusBarTransitionDelay, ArrayList<NonAppWindowAnimationAdapter> adaptersOut) {
        ArrayList<RemoteAnimationTarget> targets = new ArrayList<>();
        if (shouldStartNonAppWindowAnimationsForKeyguardExit(transit)) {
            startNonAppWindowAnimationsForKeyguardExit(service, durationHint, statusBarTransitionDelay, targets, adaptersOut);
        } else if (shouldAttachNavBarToApp(service, displayContent, transit)) {
            startNavigationBarWindowAnimation(displayContent, durationHint, statusBarTransitionDelay, targets, adaptersOut);
        }
        return (RemoteAnimationTarget[]) targets.toArray(new RemoteAnimationTarget[targets.size()]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean shouldStartNonAppWindowAnimationsForKeyguardExit(int transit) {
        return transit == 20 || transit == 21;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean shouldAttachNavBarToApp(WindowManagerService service, DisplayContent displayContent, int transit) {
        return (transit == 8 || transit == 10 || transit == 12) && displayContent.getDisplayPolicy().shouldAttachNavBarToAppDuringTransition() && service.getRecentsAnimationController() == null && displayContent.getAsyncRotationController() == null;
    }

    private static void startNonAppWindowAnimationsForKeyguardExit(final WindowManagerService service, final long durationHint, final long statusBarTransitionDelay, final ArrayList<RemoteAnimationTarget> targets, final ArrayList<NonAppWindowAnimationAdapter> adaptersOut) {
        WindowManagerPolicy windowManagerPolicy = service.mPolicy;
        service.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.NonAppWindowAnimationAdapter$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                NonAppWindowAnimationAdapter.lambda$startNonAppWindowAnimationsForKeyguardExit$0(WindowManagerService.this, durationHint, statusBarTransitionDelay, adaptersOut, targets, (WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$startNonAppWindowAnimationsForKeyguardExit$0(WindowManagerService service, long durationHint, long statusBarTransitionDelay, ArrayList adaptersOut, ArrayList targets, WindowState nonAppWindow) {
        if (nonAppWindow.mActivityRecord == null && nonAppWindow.canBeHiddenByKeyguard() && nonAppWindow.wouldBeVisibleIfPolicyIgnored() && !nonAppWindow.isVisible() && nonAppWindow != service.mRoot.getCurrentInputMethodWindow()) {
            NonAppWindowAnimationAdapter nonAppAdapter = new NonAppWindowAnimationAdapter(nonAppWindow, durationHint, statusBarTransitionDelay);
            adaptersOut.add(nonAppAdapter);
            nonAppWindow.startAnimation(nonAppWindow.getPendingTransaction(), nonAppAdapter, false, 16);
            targets.add(nonAppAdapter.createRemoteAnimationTarget());
        }
    }

    private static void startNavigationBarWindowAnimation(DisplayContent displayContent, long durationHint, long statusBarTransitionDelay, ArrayList<RemoteAnimationTarget> targets, ArrayList<NonAppWindowAnimationAdapter> adaptersOut) {
        WindowState navWindow = displayContent.getDisplayPolicy().getNavigationBar();
        NonAppWindowAnimationAdapter nonAppAdapter = new NonAppWindowAnimationAdapter(navWindow.mToken, durationHint, statusBarTransitionDelay);
        adaptersOut.add(nonAppAdapter);
        navWindow.mToken.startAnimation(navWindow.mToken.getPendingTransaction(), nonAppAdapter, false, 16);
        targets.add(nonAppAdapter.createRemoteAnimationTarget());
    }

    RemoteAnimationTarget createRemoteAnimationTarget() {
        RemoteAnimationTarget remoteAnimationTarget = new RemoteAnimationTarget(-1, -1, getLeash(), false, new Rect(), (Rect) null, this.mWindowContainer.getPrefixOrderIndex(), this.mWindowContainer.getLastSurfacePosition(), this.mWindowContainer.getBounds(), (Rect) null, this.mWindowContainer.getWindowConfiguration(), true, (SurfaceControl) null, (Rect) null, (ActivityManager.RunningTaskInfo) null, false, this.mWindowContainer.getWindowType());
        this.mTarget = remoteAnimationTarget;
        return remoteAnimationTarget;
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void startAnimation(SurfaceControl animationLeash, SurfaceControl.Transaction t, int type, SurfaceAnimator.OnAnimationFinishedCallback finishCallback) {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, 1999594750, 0, (String) null, (Object[]) null);
        }
        this.mCapturedLeash = animationLeash;
        this.mCapturedLeashFinishCallback = finishCallback;
        this.mLastAnimationType = type;
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
    public WindowContainer getWindowContainer() {
        return this.mWindowContainer;
    }

    @Override // com.android.server.wm.AnimationAdapter
    public long getDurationHint() {
        return this.mDurationHint;
    }

    @Override // com.android.server.wm.AnimationAdapter
    public long getStatusBarTransitionsStartTime() {
        return SystemClock.uptimeMillis() + this.mStatusBarTransitionDelay;
    }

    SurfaceControl getLeash() {
        return this.mCapturedLeash;
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void onAnimationCancelled(SurfaceControl animationLeash) {
        if (ProtoLogCache.WM_DEBUG_REMOTE_ANIMATIONS_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_REMOTE_ANIMATIONS, -1153814764, 0, (String) null, (Object[]) null);
        }
    }

    @Override // com.android.server.wm.AnimationAdapter
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("windowContainer=");
        pw.println(this.mWindowContainer);
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
