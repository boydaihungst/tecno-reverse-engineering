package com.android.server.wm;

import android.os.HandlerExecutor;
import android.util.ArrayMap;
import android.util.Slog;
import android.view.SurfaceControl;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class AsyncRotationController extends FadeAnimationController implements Consumer<WindowState> {
    private static final boolean DEBUG = false;
    private static final int OP_APP_SWITCH = 1;
    private static final int OP_CHANGE = 2;
    private static final int OP_CHANGE_MAY_SEAMLESS = 3;
    private static final int OP_LEGACY = 0;
    private static final String TAG = "AsyncRotation";
    private final boolean mHasScreenRotationAnimation;
    private boolean mHideImmediately;
    private boolean mIsStartTransactionCommitted;
    private boolean mIsSyncDrawRequested;
    private WindowToken mNavBarToken;
    private Runnable mOnShowRunnable;
    private final int mOriginalRotation;
    private SeamlessRotator mRotator;
    private final WindowManagerService mService;
    private final ArrayMap<WindowToken, Operation> mTargetWindowTokens;
    private Runnable mTimeoutRunnable;
    private final int mTransitionOp;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface TransitionOp {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AsyncRotationController(DisplayContent displayContent) {
        super(displayContent);
        this.mTargetWindowTokens = new ArrayMap<>();
        this.mService = displayContent.mWmService;
        int rotation = displayContent.getWindowConfiguration().getRotation();
        this.mOriginalRotation = rotation;
        int transitionType = displayContent.mTransitionController.getCollectingTransitionType();
        boolean z = false;
        if (transitionType == 6) {
            DisplayRotation dr = displayContent.getDisplayRotation();
            WindowState w = displayContent.getDisplayPolicy().getTopFullscreenOpaqueWindow();
            if (w != null && w.mAttrs.rotationAnimation == 3 && w.getTask() != null && dr.canRotateSeamlessly(rotation, dr.getRotation())) {
                this.mTransitionOp = 3;
            } else {
                this.mTransitionOp = 2;
            }
        } else if (transitionType != 0) {
            this.mTransitionOp = 1;
        } else {
            this.mTransitionOp = 0;
        }
        z = (displayContent.getRotationAnimation() != null || this.mTransitionOp == 2) ? true : z;
        this.mHasScreenRotationAnimation = z;
        if (z) {
            this.mHideImmediately = true;
        }
        displayContent.forAllWindows((Consumer<WindowState>) this, true);
        if (this.mTransitionOp == 0) {
            this.mIsStartTransactionCommitted = true;
        } else if (displayContent.mTransitionController.useShellTransitionsRotation() || displayContent.mTransitionController.isCollecting(displayContent)) {
            keepAppearanceInPreviousRotation();
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // java.util.function.Consumer
    public void accept(WindowState w) {
        if (w.mActivityRecord != null || !w.mHasSurface || w.mIsWallpaper || w.mIsImWindow || w.mAttrs.type == 2040) {
            return;
        }
        if (this.mTransitionOp == 0 && w.mForceSeamlesslyRotate) {
            return;
        }
        if (w.mAttrs.type == 2019) {
            int action = 2;
            boolean navigationBarCanMove = this.mDisplayContent.getDisplayPolicy().navigationBarCanMove();
            int i = this.mTransitionOp;
            if (i == 0) {
                this.mNavBarToken = w.mToken;
                if (navigationBarCanMove) {
                    return;
                }
                RecentsAnimationController recents = this.mService.getRecentsAnimationController();
                if (recents != null && recents.isNavigationBarAttachedToApp()) {
                    return;
                }
            } else if (navigationBarCanMove || i == 3) {
                action = 1;
            }
            this.mTargetWindowTokens.put(w.mToken, new Operation(action));
            return;
        }
        int action2 = this.mTransitionOp;
        int action3 = (action2 == 3 || w.mForceSeamlesslyRotate) ? 1 : 2;
        this.mTargetWindowTokens.put(w.mToken, new Operation(action3));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void keepAppearanceInPreviousRotation() {
        for (int i = this.mTargetWindowTokens.size() - 1; i >= 0; i--) {
            if (!this.mHasScreenRotationAnimation || this.mTargetWindowTokens.valueAt(i).mAction != 2) {
                WindowToken token = this.mTargetWindowTokens.keyAt(i);
                for (int j = token.getChildCount() - 1; j >= 0; j--) {
                    ((WindowState) token.getChildAt(j)).applyWithNextDraw(new Consumer() { // from class: com.android.server.wm.AsyncRotationController$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            AsyncRotationController.lambda$keepAppearanceInPreviousRotation$0((SurfaceControl.Transaction) obj);
                        }
                    });
                }
            }
        }
        this.mIsSyncDrawRequested = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$keepAppearanceInPreviousRotation$0(SurfaceControl.Transaction t) {
    }

    private void finishOp(WindowToken windowToken) {
        Operation op = this.mTargetWindowTokens.remove(windowToken);
        if (op == null) {
            return;
        }
        if (op.mCapturedDrawTransaction != null) {
            this.mDisplayContent.getPendingTransaction().merge(op.mCapturedDrawTransaction);
            op.mCapturedDrawTransaction = null;
        }
        if (op.mAction == 2) {
            fadeWindowToken(true, windowToken, 64);
        } else if (op.mAction == 1 && this.mRotator != null && op.mLeash != null && op.mLeash.isValid()) {
            this.mRotator.setIdentityMatrix(this.mDisplayContent.getPendingTransaction(), op.mLeash);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void completeAll() {
        for (int i = this.mTargetWindowTokens.size() - 1; i >= 0; i--) {
            finishOp(this.mTargetWindowTokens.keyAt(i));
        }
        this.mTargetWindowTokens.clear();
        if (this.mTimeoutRunnable != null) {
            this.mService.mH.removeCallbacks(this.mTimeoutRunnable);
        }
        Runnable runnable = this.mOnShowRunnable;
        if (runnable != null) {
            runnable.run();
            this.mOnShowRunnable = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean completeRotation(WindowToken token) {
        Operation op;
        if (!this.mIsStartTransactionCommitted) {
            Operation op2 = this.mTargetWindowTokens.get(token);
            if (op2 != null) {
                op2.mIsCompletionPending = true;
            }
            return false;
        } else if (!(this.mTransitionOp == 1 && token.mTransitionController.inTransition() && (op = this.mTargetWindowTokens.get(token)) != null && op.mAction == 2) && isTargetToken(token)) {
            if (this.mHasScreenRotationAnimation || this.mTransitionOp != 0) {
                finishOp(token);
                if (this.mTargetWindowTokens.isEmpty()) {
                    if (this.mTimeoutRunnable != null) {
                        this.mService.mH.removeCallbacks(this.mTimeoutRunnable);
                    }
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void start() {
        for (int i = this.mTargetWindowTokens.size() - 1; i >= 0; i--) {
            WindowToken windowToken = this.mTargetWindowTokens.keyAt(i);
            Operation op = this.mTargetWindowTokens.valueAt(i);
            if (op.mAction == 2) {
                fadeWindowToken(false, windowToken, 64);
                op.mLeash = windowToken.getAnimationLeash();
            } else if (op.mAction == 1) {
                op.mLeash = windowToken.mSurfaceControl;
            }
        }
        if (this.mHasScreenRotationAnimation) {
            scheduleTimeout();
        }
    }

    private void scheduleTimeout() {
        if (this.mTimeoutRunnable == null) {
            this.mTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.AsyncRotationController$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    AsyncRotationController.this.m7862x26edc4f5();
                }
            };
        }
        this.mService.mH.postDelayed(this.mTimeoutRunnable, 2000L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleTimeout$1$com-android-server-wm-AsyncRotationController  reason: not valid java name */
    public /* synthetic */ void m7862x26edc4f5() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Slog.i(TAG, "Async rotation timeout: " + this.mTargetWindowTokens);
                this.mIsStartTransactionCommitted = true;
                this.mDisplayContent.finishAsyncRotationIfPossible();
                this.mService.mWindowPlacerLocked.performSurfacePlacement();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideImmediately(WindowToken windowToken) {
        boolean original = this.mHideImmediately;
        this.mHideImmediately = true;
        Operation op = new Operation(2);
        this.mTargetWindowTokens.put(windowToken, op);
        fadeWindowToken(false, windowToken, 64);
        op.mLeash = windowToken.getAnimationLeash();
        this.mHideImmediately = original;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAsync(WindowState w) {
        return w.mToken == this.mNavBarToken || (w.mForceSeamlesslyRotate && this.mTransitionOp == 0) || isTargetToken(w.mToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTargetToken(WindowToken token) {
        return this.mTargetWindowTokens.containsKey(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldFreezeInsetsPosition(WindowState w) {
        return this.mTransitionOp == 1 && w.mTransitionController.inTransition() && isTargetToken(w.mToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOnShowRunnable(Runnable onShowRunnable) {
        this.mOnShowRunnable = onShowRunnable;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setupStartTransaction(SurfaceControl.Transaction t) {
        for (int i = this.mTargetWindowTokens.size() - 1; i >= 0; i--) {
            Operation op = this.mTargetWindowTokens.valueAt(i);
            SurfaceControl leash = op.mLeash;
            if (leash != null && leash.isValid()) {
                if (this.mHasScreenRotationAnimation && op.mAction == 2) {
                    t.setAlpha(leash, 0.0f);
                } else {
                    if (this.mRotator == null) {
                        this.mRotator = new SeamlessRotator(this.mOriginalRotation, this.mDisplayContent.getWindowConfiguration().getRotation(), this.mDisplayContent.getDisplayInfo(), false);
                    }
                    this.mRotator.applyTransform(t, leash);
                }
            }
        }
        if (this.mIsStartTransactionCommitted) {
            return;
        }
        t.addTransactionCommittedListener(new HandlerExecutor(this.mService.mH), new SurfaceControl.TransactionCommittedListener() { // from class: com.android.server.wm.AsyncRotationController$$ExternalSyntheticLambda2
            public final void onTransactionCommitted() {
                AsyncRotationController.this.m7863x212569e5();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupStartTransaction$2$com-android-server-wm-AsyncRotationController  reason: not valid java name */
    public /* synthetic */ void m7863x212569e5() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mIsStartTransactionCommitted = true;
                for (int i = this.mTargetWindowTokens.size() - 1; i >= 0; i--) {
                    if (this.mTargetWindowTokens.valueAt(i).mIsCompletionPending) {
                        this.mDisplayContent.finishAsyncRotation(this.mTargetWindowTokens.keyAt(i));
                    }
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTransitionFinished() {
        if (this.mTransitionOp == 2) {
            return;
        }
        for (int i = this.mTargetWindowTokens.size() - 1; i >= 0; i--) {
            WindowToken token = this.mTargetWindowTokens.keyAt(i);
            if (token.isVisible()) {
                int j = token.getChildCount() - 1;
                while (true) {
                    if (j >= 0) {
                        if (((WindowState) token.getChildAt(j)).isDrawFinishedLw()) {
                            this.mDisplayContent.finishAsyncRotation(token);
                            break;
                        } else {
                            j--;
                        }
                    } else {
                        break;
                    }
                }
            } else {
                this.mDisplayContent.finishAsyncRotation(token);
            }
        }
        if (!this.mTargetWindowTokens.isEmpty()) {
            scheduleTimeout();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleFinishDrawing(WindowState w, SurfaceControl.Transaction postDrawTransaction) {
        Operation op;
        if (this.mTransitionOp == 0 || postDrawTransaction == null || !this.mIsSyncDrawRequested || !w.mTransitionController.inTransition() || (op = this.mTargetWindowTokens.get(w.mToken)) == null) {
            return false;
        }
        boolean keepUntilTransitionFinish = this.mTransitionOp == 1 && op.mAction == 2;
        boolean keepUntilStartTransaction = !this.mIsStartTransactionCommitted && op.mAction == 1;
        if (keepUntilTransitionFinish || keepUntilStartTransaction) {
            if (op.mCapturedDrawTransaction == null) {
                op.mCapturedDrawTransaction = postDrawTransaction;
            } else {
                op.mCapturedDrawTransaction.merge(postDrawTransaction);
            }
            return true;
        }
        return false;
    }

    @Override // com.android.server.wm.FadeAnimationController
    public Animation getFadeInAnimation() {
        if (this.mHasScreenRotationAnimation) {
            return AnimationUtils.loadAnimation(this.mContext, 17432720);
        }
        return super.getFadeInAnimation();
    }

    @Override // com.android.server.wm.FadeAnimationController
    public Animation getFadeOutAnimation() {
        if (this.mHideImmediately) {
            float alpha = this.mTransitionOp == 2 ? 1.0f : 0.0f;
            return new AlphaAnimation(alpha, alpha);
        }
        return super.getFadeOutAnimation();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class Operation {
        static final int ACTION_FADE = 2;
        static final int ACTION_SEAMLESS = 1;
        final int mAction;
        SurfaceControl.Transaction mCapturedDrawTransaction;
        boolean mIsCompletionPending;
        SurfaceControl mLeash;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes2.dex */
        @interface Action {
        }

        Operation(int action) {
            this.mAction = action;
        }
    }
}
