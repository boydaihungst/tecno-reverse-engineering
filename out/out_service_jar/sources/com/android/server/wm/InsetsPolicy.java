package com.android.server.wm;

import android.app.ThunderbackConfig;
import android.app.WindowConfiguration;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.IntArray;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.InsetsAnimationControlCallbacks;
import android.view.InsetsAnimationControlImpl;
import android.view.InsetsAnimationControlRunner;
import android.view.InsetsController;
import android.view.InsetsSource;
import android.view.InsetsSourceControl;
import android.view.InsetsState;
import android.view.InternalInsetsAnimationController;
import android.view.SurfaceControl;
import android.view.SyncRtSurfaceTransactionApplier;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.view.WindowInsetsAnimationControlListener;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import com.android.server.DisplayThread;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.InsetsPolicy;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class InsetsPolicy {
    private boolean mAnimatingShown;
    private final DisplayContent mDisplayContent;
    private WindowState mFocusedWin;
    private final boolean mHideNavBarForKeyboard;
    private final DisplayPolicy mPolicy;
    private boolean mRemoteInsetsControllerControlsSystemBars;
    private final InsetsStateController mStateController;
    private final IntArray mShowingTransientTypes = new IntArray();
    private final InsetsControlTarget mDummyControlTarget = new InsetsControlTarget() { // from class: com.android.server.wm.InsetsPolicy.1
        @Override // com.android.server.wm.InsetsControlTarget
        public void notifyInsetsControlChanged() {
            SurfaceControl leash;
            boolean hasLeash = false;
            InsetsSourceControl[] controls = InsetsPolicy.this.mStateController.getControlsForDispatch(this);
            if (controls == null) {
                return;
            }
            for (InsetsSourceControl control : controls) {
                int type = control.getType();
                if (InsetsPolicy.this.mShowingTransientTypes.indexOf(type) == -1 && (leash = control.getLeash()) != null) {
                    hasLeash = true;
                    InsetsPolicy.this.mDisplayContent.getPendingTransaction().setAlpha(leash, InsetsState.getDefaultVisibility(type) ? 1.0f : 0.0f);
                }
            }
            if (hasLeash) {
                InsetsPolicy.this.mDisplayContent.scheduleAnimation();
            }
        }
    };
    private BarWindow mStatusBar = new BarWindow(1);
    private BarWindow mNavBar = new BarWindow(2);
    private final float[] mTmpFloat9 = new float[9];

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsPolicy(InsetsStateController stateController, DisplayContent displayContent) {
        this.mStateController = stateController;
        this.mDisplayContent = displayContent;
        DisplayPolicy displayPolicy = displayContent.getDisplayPolicy();
        this.mPolicy = displayPolicy;
        Resources r = displayPolicy.getContext().getResources();
        this.mRemoteInsetsControllerControlsSystemBars = r.getBoolean(17891334);
        this.mHideNavBarForKeyboard = r.getBoolean(17891681);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getRemoteInsetsControllerControlsSystemBars() {
        return this.mRemoteInsetsControllerControlsSystemBars;
    }

    void setRemoteInsetsControllerControlsSystemBars(boolean controlsSystemBars) {
        this.mRemoteInsetsControllerControlsSystemBars = controlsSystemBars;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateBarControlTarget(WindowState focusedWin) {
        InsetsControlTarget insetsControlTarget;
        if (this.mFocusedWin != focusedWin) {
            abortTransient();
        }
        this.mFocusedWin = focusedWin;
        InsetsControlTarget statusControlTarget = getStatusControlTarget(focusedWin, false);
        InsetsControlTarget navControlTarget = getNavControlTarget(focusedWin, false);
        WindowState notificationShade = this.mPolicy.getNotificationShade();
        WindowState topApp = this.mPolicy.getTopFullscreenOpaqueWindow();
        InsetsStateController insetsStateController = this.mStateController;
        InsetsControlTarget insetsControlTarget2 = null;
        if (statusControlTarget == this.mDummyControlTarget) {
            insetsControlTarget = getStatusControlTarget(focusedWin, true);
        } else if (statusControlTarget == notificationShade) {
            insetsControlTarget = getStatusControlTarget(topApp, true);
        } else {
            insetsControlTarget = null;
        }
        if (navControlTarget == this.mDummyControlTarget) {
            insetsControlTarget2 = getNavControlTarget(focusedWin, true);
        } else if (navControlTarget == notificationShade) {
            insetsControlTarget2 = getNavControlTarget(topApp, true);
        }
        insetsStateController.onBarControlTargetChanged(statusControlTarget, insetsControlTarget, navControlTarget, insetsControlTarget2);
        this.mStatusBar.updateVisibility(statusControlTarget, 0);
        this.mNavBar.updateVisibility(navControlTarget, 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isHidden(int type) {
        WindowContainerInsetsSourceProvider provider = this.mStateController.peekSourceProvider(type);
        return (provider == null || !provider.hasWindowContainer() || provider.getSource().isVisible()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showTransient(int[] types, boolean isGestureOnSystemBar) {
        boolean changed = false;
        boolean z = true;
        for (int i = types.length - 1; i >= 0; i--) {
            int type = types[i];
            if (isHidden(type) && this.mShowingTransientTypes.indexOf(type) == -1) {
                this.mShowingTransientTypes.add(type);
                changed = true;
            }
        }
        if (changed) {
            StatusBarManagerInternal statusBarManagerInternal = this.mPolicy.getStatusBarManagerInternal();
            if (statusBarManagerInternal != null) {
                statusBarManagerInternal.showTransient(this.mDisplayContent.getDisplayId(), this.mShowingTransientTypes.toArray(), isGestureOnSystemBar);
            }
            updateBarControlTarget(this.mFocusedWin);
            WindowState windowState = this.mFocusedWin;
            if (!isTransient(0) && !isTransient(1)) {
                z = false;
            }
            dispatchTransientSystemBarsVisibilityChanged(windowState, z, isGestureOnSystemBar);
            this.mDisplayContent.mWmService.mAnimator.getChoreographer().postFrameCallback(new Choreographer.FrameCallback() { // from class: com.android.server.wm.InsetsPolicy$$ExternalSyntheticLambda0
                @Override // android.view.Choreographer.FrameCallback
                public final void doFrame(long j) {
                    InsetsPolicy.this.m8052lambda$showTransient$0$comandroidserverwmInsetsPolicy(j);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showTransient$0$com-android-server-wm-InsetsPolicy  reason: not valid java name */
    public /* synthetic */ void m8052lambda$showTransient$0$comandroidserverwmInsetsPolicy(long time) {
        synchronized (this.mDisplayContent.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                startAnimation(true, null);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideTransient() {
        if (this.mShowingTransientTypes.size() == 0) {
            return;
        }
        dispatchTransientSystemBarsVisibilityChanged(this.mFocusedWin, false, false);
        startAnimation(false, new Runnable() { // from class: com.android.server.wm.InsetsPolicy$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                InsetsPolicy.this.m8051lambda$hideTransient$1$comandroidserverwmInsetsPolicy();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hideTransient$1$com-android-server-wm-InsetsPolicy  reason: not valid java name */
    public /* synthetic */ void m8051lambda$hideTransient$1$comandroidserverwmInsetsPolicy() {
        synchronized (this.mDisplayContent.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (int i = this.mShowingTransientTypes.size() - 1; i >= 0; i--) {
                    int type = this.mShowingTransientTypes.get(i);
                    this.mStateController.getSourceProvider(type).setClientVisible(false);
                }
                this.mShowingTransientTypes.clear();
                updateBarControlTarget(this.mFocusedWin);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    boolean isTransient(int type) {
        return this.mShowingTransientTypes.indexOf(type) != -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsState adjustInsetsForWindow(WindowState target, InsetsState originalState, boolean includesTransient) {
        InsetsState state;
        if (!includesTransient) {
            state = adjustVisibilityForTransientTypes(originalState);
        } else {
            state = originalState;
        }
        InsetsState state2 = adjustVisibilityForIme(target, state, state == originalState);
        return adjustInsetsForRoundedCorners(target, state2, state2 == originalState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsState adjustInsetsForWindow(WindowState target, InsetsState originalState) {
        return adjustInsetsForWindow(target, originalState, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsState getInsetsForWindowMetrics(WindowManager.LayoutParams attrs) {
        InsetsState rotatedState;
        int type = getInsetsTypeForLayoutParams(attrs);
        WindowToken token = this.mDisplayContent.getWindowToken(attrs.token);
        if (token != null && (rotatedState = token.getFixedRotationTransformInsetsState()) != null) {
            return rotatedState;
        }
        boolean alwaysOnTop = token != null && token.isAlwaysOnTop();
        InsetsState originalState = this.mDisplayContent.getInsetsPolicy().enforceInsetsPolicyForTarget(type, 1, alwaysOnTop, this.mStateController.getRawInsetsState());
        return adjustVisibilityForTransientTypes(originalState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isInsetsTypeControllable(int type) {
        switch (type) {
            case 0:
            case 1:
            case 19:
            case 20:
            case 21:
                return true;
            default:
                return false;
        }
    }

    private static int getInsetsTypeForLayoutParams(WindowManager.LayoutParams attrs) {
        int[] iArr;
        int type = attrs.type;
        switch (type) {
            case 2000:
                return 0;
            case 2011:
                return 19;
            case 2019:
                return 1;
            default:
                if (attrs.providesInsetsTypes != null) {
                    for (int insetsType : attrs.providesInsetsTypes) {
                        switch (insetsType) {
                            case 0:
                            case 1:
                            case 20:
                            case 21:
                                return insetsType;
                            default:
                        }
                    }
                    return -1;
                }
                return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsState enforceInsetsPolicyForTarget(int type, int windowingMode, boolean isAlwaysOnTop, InsetsState state) {
        boolean stateCopied = false;
        if (type != -1) {
            state = new InsetsState(state);
            stateCopied = true;
            state.removeSource(type);
            if (type == 1 || type == 21) {
                state.removeSource(0);
                state.removeSource(20);
                state.removeSource(2);
                state.removeSource(1);
                state.removeSource(21);
            }
            if (type == 0 || type == 20) {
                state.removeSource(2);
            }
            if (type == 19) {
                ArrayMap<Integer, WindowContainerInsetsSourceProvider> providers = this.mStateController.getSourceProviders();
                for (int i = providers.size() - 1; i >= 0; i--) {
                    WindowContainerInsetsSourceProvider otherProvider = providers.valueAt(i);
                    if (otherProvider.overridesImeFrame()) {
                        InsetsSource override = new InsetsSource(state.getSource(otherProvider.getSource().getType()));
                        override.setFrame(otherProvider.getImeOverrideFrame());
                        state.addSource(override);
                    }
                }
            }
        }
        if (WindowConfiguration.isFloating(windowingMode) || (windowingMode == 6 && isAlwaysOnTop)) {
            if (!stateCopied) {
                state = new InsetsState(state);
            }
            state.removeSource(0);
            state.removeSource(1);
            state.removeSource(21);
            if (windowingMode == 2) {
                state.removeSource(19);
            }
        }
        return state;
    }

    private InsetsState adjustVisibilityForTransientTypes(InsetsState originalState) {
        InsetsState state = originalState;
        for (int i = this.mShowingTransientTypes.size() - 1; i >= 0; i--) {
            int type = this.mShowingTransientTypes.get(i);
            InsetsSource originalSource = state.peekSource(type);
            if (originalSource != null && originalSource.isVisible()) {
                if (state == originalState) {
                    state = new InsetsState(originalState);
                }
                InsetsSource source = new InsetsSource(originalSource);
                source.setVisible(false);
                state.addSource(source);
            }
        }
        return state;
    }

    private InsetsState adjustVisibilityForIme(WindowState w, InsetsState originalState, boolean copyState) {
        InsetsSource originalImeSource;
        boolean z = true;
        if (w.mIsImWindow) {
            boolean navVisible = !this.mHideNavBarForKeyboard;
            InsetsSource originalNavSource = originalState.peekSource(1);
            if (originalNavSource != null && originalNavSource.isVisible() != navVisible) {
                InsetsState state = copyState ? new InsetsState(originalState) : originalState;
                InsetsSource navSource = new InsetsSource(originalNavSource);
                navSource.setVisible(navVisible);
                state.addSource(navSource);
                return state;
            }
        } else if (w.mActivityRecord != null && w.mActivityRecord.mImeInsetsFrozenUntilStartInput && (originalImeSource = originalState.peekSource(19)) != null) {
            if (!w.mActivityRecord.mLastImeShown && !w.getRequestedVisibility(19)) {
                z = false;
            }
            boolean imeVisibility = z;
            InsetsState state2 = copyState ? new InsetsState(originalState) : originalState;
            InsetsSource imeSource = new InsetsSource(originalImeSource);
            imeSource.setVisible(imeVisibility);
            state2.addSource(imeSource);
            return state2;
        }
        return originalState;
    }

    private InsetsState adjustInsetsForRoundedCorners(WindowState w, InsetsState originalState, boolean copyState) {
        Task task = w.getTask();
        if (task != null && !task.getWindowConfiguration().tasksAreFloating()) {
            Rect roundedCornerFrame = new Rect(task.getBounds());
            InsetsState state = copyState ? new InsetsState(originalState) : originalState;
            state.setRoundedCornerFrame(roundedCornerFrame);
            return state;
        }
        return originalState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onInsetsModified(InsetsControlTarget caller) {
        this.mStateController.onInsetsModified(caller);
        checkAbortTransient(caller);
        updateBarControlTarget(this.mFocusedWin);
    }

    private void checkAbortTransient(InsetsControlTarget caller) {
        if (this.mShowingTransientTypes.size() != 0) {
            IntArray abortTypes = new IntArray();
            boolean imeRequestedVisible = caller.getRequestedVisibility(19);
            for (int i = this.mShowingTransientTypes.size() - 1; i >= 0; i--) {
                int type = this.mShowingTransientTypes.get(i);
                if ((this.mStateController.isFakeTarget(type, caller) && caller.getRequestedVisibility(type)) || (type == 1 && imeRequestedVisible)) {
                    this.mShowingTransientTypes.remove(i);
                    abortTypes.add(type);
                }
            }
            StatusBarManagerInternal statusBarManagerInternal = this.mPolicy.getStatusBarManagerInternal();
            if (abortTypes.size() > 0 && statusBarManagerInternal != null) {
                statusBarManagerInternal.abortTransient(this.mDisplayContent.getDisplayId(), abortTypes.toArray());
            }
        }
    }

    private void abortTransient() {
        StatusBarManagerInternal statusBarManagerInternal = this.mPolicy.getStatusBarManagerInternal();
        if (statusBarManagerInternal != null) {
            statusBarManagerInternal.abortTransient(this.mDisplayContent.getDisplayId(), this.mShowingTransientTypes.toArray());
        }
        this.mShowingTransientTypes.clear();
        dispatchTransientSystemBarsVisibilityChanged(this.mFocusedWin, false, false);
    }

    private InsetsControlTarget getStatusControlTarget(WindowState focusedWin, boolean fake) {
        if (!fake && isShowingTransientTypes(WindowInsets.Type.statusBars())) {
            return this.mDummyControlTarget;
        }
        WindowState notificationShade = this.mPolicy.getNotificationShade();
        if (focusedWin == notificationShade) {
            return focusedWin;
        }
        if (remoteInsetsControllerControlsSystemBars(focusedWin)) {
            this.mDisplayContent.mRemoteInsetsControlTarget.topFocusedWindowChanged(focusedWin.mAttrs.packageName, focusedWin.getRequestedVisibilities());
            return this.mDisplayContent.mRemoteInsetsControlTarget;
        } else if (this.mPolicy.areSystemBarsForcedShownLw() && !needSkipTarget(focusedWin)) {
            return null;
        } else {
            if (forceShowsStatusBarTransiently() && !fake) {
                return this.mDummyControlTarget;
            }
            if (!canBeTopFullscreenOpaqueWindow(focusedWin) && this.mPolicy.topAppHidesStatusBar() && (notificationShade == null || !notificationShade.canReceiveKeys())) {
                return this.mPolicy.getTopFullscreenOpaqueWindow();
            }
            return focusedWin;
        }
    }

    private static boolean canBeTopFullscreenOpaqueWindow(WindowState win) {
        boolean nonAttachedAppWindow = win != null && win.mAttrs.type >= 1 && win.mAttrs.type <= 99;
        return nonAttachedAppWindow && win.mAttrs.isFullscreen() && !win.isFullyTransparent() && !win.inMultiWindowMode();
    }

    private InsetsControlTarget getNavControlTarget(WindowState focusedWin, boolean fake) {
        WindowState imeWin = this.mDisplayContent.mInputMethodWindow;
        if (imeWin != null && imeWin.isVisible() && !this.mHideNavBarForKeyboard) {
            return null;
        }
        if (!fake && isShowingTransientTypes(WindowInsets.Type.navigationBars())) {
            return this.mDummyControlTarget;
        }
        if (focusedWin == this.mPolicy.getNotificationShade()) {
            return focusedWin;
        }
        if (this.mPolicy.isForceShowNavigationBarEnabled() && focusedWin != null && focusedWin.getActivityType() == 1) {
            return null;
        }
        if (remoteInsetsControllerControlsSystemBars(focusedWin)) {
            this.mDisplayContent.mRemoteInsetsControlTarget.topFocusedWindowChanged(focusedWin.mAttrs.packageName, focusedWin.getRequestedVisibilities());
            return this.mDisplayContent.mRemoteInsetsControlTarget;
        } else if (this.mPolicy.areSystemBarsForcedShownLw()) {
            return null;
        } else {
            if (forceShowsNavigationBarTransiently() && !fake) {
                return this.mDummyControlTarget;
            }
            return focusedWin;
        }
    }

    private boolean isShowingTransientTypes(int types) {
        IntArray showingTransientTypes = this.mShowingTransientTypes;
        for (int i = showingTransientTypes.size() - 1; i >= 0; i--) {
            if ((InsetsState.toPublicType(showingTransientTypes.get(i)) & types) != 0) {
                return true;
            }
        }
        return false;
    }

    boolean remoteInsetsControllerControlsSystemBars(WindowState focusedWin) {
        DisplayContent displayContent;
        return focusedWin != null && this.mRemoteInsetsControllerControlsSystemBars && (displayContent = this.mDisplayContent) != null && displayContent.mRemoteInsetsControlTarget != null && focusedWin.getAttrs().type >= 1 && focusedWin.getAttrs().type <= 99;
    }

    private boolean forceShowsStatusBarTransiently() {
        WindowState win = this.mPolicy.getStatusBar();
        return (win == null || (win.mAttrs.privateFlags & 4096) == 0) ? false : true;
    }

    private boolean forceShowsNavigationBarTransiently() {
        WindowState win = this.mPolicy.getNotificationShade();
        return (win == null || (win.mAttrs.privateFlags & 8388608) == 0) ? false : true;
    }

    void startAnimation(boolean show, Runnable callback) {
        int typesReady = 0;
        SparseArray<InsetsSourceControl> controls = new SparseArray<>();
        IntArray showingTransientTypes = this.mShowingTransientTypes;
        for (int i = showingTransientTypes.size() - 1; i >= 0; i--) {
            int type = showingTransientTypes.get(i);
            WindowContainerInsetsSourceProvider provider = this.mStateController.getSourceProvider(type);
            InsetsSourceControl control = provider.getControl(this.mDummyControlTarget);
            if (control != null && control.getLeash() != null) {
                typesReady |= InsetsState.toPublicType(type);
                controls.put(control.getType(), new InsetsSourceControl(control));
            }
        }
        controlAnimationUnchecked(typesReady, controls, show, callback);
    }

    private void controlAnimationUnchecked(int typesReady, SparseArray<InsetsSourceControl> controls, boolean show, Runnable callback) {
        InsetsPolicyAnimationControlListener listener = new InsetsPolicyAnimationControlListener(show, callback, typesReady);
        listener.mControlCallbacks.controlAnimationUnchecked(typesReady, controls, show);
    }

    private void dispatchTransientSystemBarsVisibilityChanged(WindowState focusedWindow, boolean areVisible, boolean wereRevealedFromSwipeOnSystemBar) {
        Task task;
        if (focusedWindow == null || (task = focusedWindow.getTask()) == null) {
            return;
        }
        int taskId = task.mTaskId;
        boolean isValidTaskId = taskId != -1;
        if (!isValidTaskId) {
            return;
        }
        this.mDisplayContent.mWmService.mTaskSystemBarsListenerController.dispatchTransientSystemBarVisibilityChanged(taskId, areVisible, wereRevealedFromSwipeOnSystemBar);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class BarWindow {
        private final int mId;
        private int mState = 0;

        BarWindow(int id) {
            this.mId = id;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateVisibility(InsetsControlTarget controlTarget, int type) {
            setVisible(controlTarget == null || controlTarget.getRequestedVisibility(type));
        }

        private void setVisible(boolean visible) {
            int state = visible ? 0 : 2;
            if (this.mState != state) {
                this.mState = state;
                StatusBarManagerInternal statusBarManagerInternal = InsetsPolicy.this.mPolicy.getStatusBarManagerInternal();
                if (statusBarManagerInternal != null) {
                    statusBarManagerInternal.setWindowState(InsetsPolicy.this.mDisplayContent.getDisplayId(), this.mId, state);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class InsetsPolicyAnimationControlListener extends InsetsController.InternalAnimationControlListener {
        InsetsPolicyAnimationControlCallbacks mControlCallbacks;
        Runnable mFinishCallback;

        InsetsPolicyAnimationControlListener(boolean show, Runnable finishCallback, int types) {
            super(show, false, types, 2, false, 0);
            this.mFinishCallback = finishCallback;
            this.mControlCallbacks = new InsetsPolicyAnimationControlCallbacks(this);
        }

        protected void onAnimationFinish() {
            super.onAnimationFinish();
            if (this.mFinishCallback != null) {
                DisplayThread.getHandler().post(this.mFinishCallback);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class InsetsPolicyAnimationControlCallbacks implements InsetsAnimationControlCallbacks {
            private InsetsAnimationControlImpl mAnimationControl = null;
            private InsetsPolicyAnimationControlListener mListener;

            InsetsPolicyAnimationControlCallbacks(InsetsPolicyAnimationControlListener listener) {
                this.mListener = listener;
            }

            /* JADX INFO: Access modifiers changed from: private */
            /* JADX WARN: Type inference failed for: r4v0, types: [android.view.WindowInsetsAnimationControlListener, com.android.server.wm.InsetsPolicy$InsetsPolicyAnimationControlListener] */
            public void controlAnimationUnchecked(final int typesReady, SparseArray<InsetsSourceControl> controls, boolean show) {
                int i;
                if (typesReady == 0) {
                    return;
                }
                InsetsPolicy.this.mAnimatingShown = show;
                InsetsState state = InsetsPolicy.this.mFocusedWin.getInsetsState();
                ?? r4 = this.mListener;
                long durationMs = r4.getDurationMs();
                Interpolator insetsInterpolator = InsetsPolicyAnimationControlListener.this.getInsetsInterpolator();
                int i2 = !show ? 1 : 0;
                if (show) {
                    i = 0;
                } else {
                    i = 1;
                }
                this.mAnimationControl = new InsetsAnimationControlImpl(controls, (Rect) null, state, (WindowInsetsAnimationControlListener) r4, typesReady, this, durationMs, insetsInterpolator, i2, i, (CompatibilityInfo.Translator) null);
                SurfaceAnimationThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.InsetsPolicy$InsetsPolicyAnimationControlListener$InsetsPolicyAnimationControlCallbacks$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        InsetsPolicy.InsetsPolicyAnimationControlListener.InsetsPolicyAnimationControlCallbacks.this.m8055x1e818775(typesReady);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$controlAnimationUnchecked$0$com-android-server-wm-InsetsPolicy$InsetsPolicyAnimationControlListener$InsetsPolicyAnimationControlCallbacks  reason: not valid java name */
            public /* synthetic */ void m8055x1e818775(int typesReady) {
                this.mListener.onReady(this.mAnimationControl, typesReady);
            }

            public void scheduleApplyChangeInsets(InsetsAnimationControlRunner runner) {
                if (this.mAnimationControl.applyChangeInsets((InsetsState) null)) {
                    this.mAnimationControl.finish(InsetsPolicy.this.mAnimatingShown);
                }
            }

            public void notifyFinished(InsetsAnimationControlRunner runner, boolean shown) {
            }

            public void applySurfaceParams(SyncRtSurfaceTransactionApplier.SurfaceParams... params) {
                SurfaceControl.Transaction t = new SurfaceControl.Transaction();
                for (int i = params.length - 1; i >= 0; i--) {
                    SyncRtSurfaceTransactionApplier.SurfaceParams surfaceParams = params[i];
                    SyncRtSurfaceTransactionApplier.applyParams(t, surfaceParams, InsetsPolicy.this.mTmpFloat9);
                }
                t.apply();
                t.close();
            }

            public void releaseSurfaceControlFromRt(SurfaceControl sc) {
                sc.release();
            }

            public <T extends InsetsAnimationControlRunner & InternalInsetsAnimationController> void startAnimation(T runner, WindowInsetsAnimationControlListener listener, int types, WindowInsetsAnimation animation, WindowInsetsAnimation.Bounds bounds) {
            }

            public void reportPerceptible(int types, boolean perceptible) {
            }
        }
    }

    private boolean needSkipTarget(WindowState focusedWin) {
        return (!ThunderbackConfig.isVersion4() || focusedWin == null || focusedWin.canShowStatusBar()) ? false : true;
    }
}
