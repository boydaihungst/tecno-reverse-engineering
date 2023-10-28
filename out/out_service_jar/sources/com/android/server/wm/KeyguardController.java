package com.android.server.wm;

import android.app.ThunderbackConfig;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import com.android.internal.os.BackgroundThread;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.server.am.ActivityManagerService;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.KeyguardController;
import com.android.server.wm.utils.TranFpUnlockStateController;
import com.transsion.hubcore.multiwindow.ITranMultiWindowManager;
import com.transsion.hubcore.server.wm.ITranKeyguardController;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class KeyguardController {
    static final String KEYGUARD_SLEEP_TOKEN_TAG = "keyguard";
    private static final String TAG = "ActivityTaskManager";
    public static boolean mDelayHookMultiWindowToMax = false;
    public static ActivityRecord mOccludedActivity;
    private boolean mAodShowing;
    private boolean mDismissalRequested;
    public boolean mFingerWakeupOptimizePath;
    private Handler mGoingAwayQuicklyHandler;
    private boolean mIsScreenOn;
    private boolean mKeyguardGoingAway;
    private boolean mKeyguardGoingAwayQuickly;
    private boolean mKeyguardShowing;
    private RootWindowContainer mRootWindowContainer;
    private final ActivityTaskManagerService mService;
    private final ActivityTaskManagerInternal.SleepTokenAcquirer mSleepTokenAcquirer;
    private final ActivityTaskSupervisor mTaskSupervisor;
    private WindowManagerService mWindowManager;
    private final SparseArray<KeyguardDisplayState> mDisplayStates = new SparseArray<>();
    private boolean mFaceUnlockGoingAwayQuickly = false;
    private boolean mFingerprintGoingAwayQuickly = false;
    private Runnable mResetKeyguardGoingAwayRunnable = new Runnable() { // from class: com.android.server.wm.KeyguardController.1
        @Override // java.lang.Runnable
        public void run() {
            Slog.i(KeyguardController.TAG, "mResetKeyguardGoingAwayRunnable");
            synchronized (KeyguardController.this.mService.getGlobalLock()) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    KeyguardController.this.notifyKeyguardGoingAwayQuickly(false, -1);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public KeyguardController(ActivityTaskManagerService service, ActivityTaskSupervisor taskSupervisor) {
        this.mService = service;
        this.mTaskSupervisor = taskSupervisor;
        Objects.requireNonNull(service);
        this.mSleepTokenAcquirer = new ActivityTaskManagerService.SleepTokenAcquirerImpl(KEYGUARD_SLEEP_TOKEN_TAG);
        this.mGoingAwayQuicklyHandler = new Handler(BackgroundThread.get().getLooper());
        ITranKeyguardController.Instance().onConstruct(service);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowManager(WindowManagerService windowManager) {
        this.mWindowManager = windowManager;
        this.mRootWindowContainer = this.mService.mRootWindowContainer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAodShowing(int displayId) {
        return getDisplayState(displayId).mAodShowing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardOrAodShowing(int displayId) {
        KeyguardDisplayState state = getDisplayState(displayId);
        return ((!state.mKeyguardShowing && !state.mAodShowing) || state.mKeyguardGoingAway || isDisplayOccluded(displayId)) ? false : true;
    }

    boolean isKeyguardUnoccludedOrAodShowing(int displayId) {
        KeyguardDisplayState state = getDisplayState(displayId);
        if (displayId == 0 && state.mAodShowing) {
            return !state.mKeyguardGoingAway;
        }
        return isKeyguardOrAodShowing(displayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardShowing(int displayId) {
        KeyguardDisplayState state = getDisplayState(displayId);
        return (!state.mKeyguardShowing || state.mKeyguardGoingAway || isDisplayOccluded(displayId)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardLocked(int displayId) {
        KeyguardDisplayState state = getDisplayState(displayId);
        return state.mKeyguardShowing && !state.mKeyguardGoingAway;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardLocked() {
        return isKeyguardLocked(0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean topActivityOccludesKeyguard(ActivityRecord r) {
        return getDisplayState(r.getDisplayId()).mTopOccludesActivity == r;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardGoingAway(int displayId) {
        KeyguardDisplayState state = getDisplayState(displayId);
        return state.mKeyguardGoingAway && state.mKeyguardShowing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setKeyguardShown(int displayId, boolean keyguardShowing, boolean aodShowing) {
        if (this.mRootWindowContainer.getDisplayContent(displayId).isKeyguardAlwaysUnlocked()) {
            Slog.i(TAG, "setKeyguardShown ignoring always unlocked display " + displayId);
            return;
        }
        WindowManagerService windowManagerService = this.mWindowManager;
        if (windowManagerService != null) {
            windowManagerService.setKeyguardStatus(keyguardShowing);
        }
        KeyguardDisplayState state = getDisplayState(displayId);
        boolean z = true;
        boolean aodChanged = aodShowing != state.mAodShowing;
        boolean keyguardChanged = keyguardShowing != state.mKeyguardShowing || (state.mKeyguardGoingAway && keyguardShowing && !aodChanged);
        if (!keyguardChanged && !aodChanged) {
            setWakeTransitionReady();
            return;
        }
        EventLogTags.writeWmSetKeyguardShown(displayId, keyguardShowing ? 1 : 0, aodShowing ? 1 : 0, state.mKeyguardGoingAway ? 1 : 0, "setKeyguardShown");
        if ((((aodShowing ? 1 : 0) ^ (keyguardShowing ? 1 : 0)) || (aodShowing && aodChanged && keyguardChanged)) && !state.mKeyguardGoingAway && Display.isOnState(this.mRootWindowContainer.getDefaultDisplay().getDisplayInfo().state)) {
            this.mWindowManager.mTaskSnapshotController.snapshotForSleeping(0);
        }
        state.mKeyguardShowing = keyguardShowing;
        state.mAodShowing = aodShowing;
        if (aodChanged) {
            this.mWindowManager.mWindowPlacerLocked.performSurfacePlacement();
        }
        if (keyguardChanged) {
            dismissMultiWindowModeForTaskIfNeeded(displayId, null);
            state.mKeyguardGoingAway = false;
            if (keyguardShowing) {
                state.mDismissalRequested = false;
                if (ThunderbackConfig.isVersion4() && mOccludedActivity != null && ITranKeyguardController.Instance().inMultiWindow(mOccludedActivity)) {
                    boolean needHookToMax = mDelayHookMultiWindowToMax && this.mService.hasMultiWindow();
                    mDelayHookMultiWindowToMax = ITranKeyguardController.Instance().hookMultiWindowToDefault(TAG, false, needHookToMax, mOccludedActivity);
                }
            } else {
                if (ThunderbackConfig.isVersion4()) {
                    mDelayHookMultiWindowToMax = false;
                }
                this.mService.updateMultiWindowMoveToFront();
            }
        }
        ITranKeyguardController Instance = ITranKeyguardController.Instance();
        boolean z2 = getDisplayState(0).mKeyguardShowing;
        ActivityRecord activityRecord = mOccludedActivity;
        if (!isDisplayOccluded(0) || !this.mService.hasMultiWindow() || !ITranKeyguardController.Instance().inMultiWindow(mOccludedActivity)) {
            z = false;
        }
        if (Instance.hookMultiWindowToMaxOrInvisible(TAG, z2, activityRecord, z)) {
            mOccludedActivity = null;
        }
        ITranKeyguardController.Instance().hookUpdateKeyguardLocked(isKeyguardLocked());
        hookSetAuthenticateSucceed(keyguardShowing);
        ITranMultiWindowManager.Instance().hookNotifyKeyguardStateV4(displayId, keyguardShowing, aodShowing);
        updateKeyguardSleepToken();
        this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
        InputMethodManagerInternal.get().updateImeWindowStatus(false);
        setWakeTransitionReady();
    }

    private void setWakeTransitionReady() {
        if (this.mWindowManager.mAtmService.getTransitionController().getCollectingTransitionType() == 11) {
            this.mWindowManager.mAtmService.getTransitionController().setReady(this.mRootWindowContainer.getDefaultDisplay());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void keyguardGoingAway(int displayId, int flags) {
        KeyguardDisplayState state = getDisplayState(displayId);
        if (!state.mKeyguardShowing || state.mKeyguardGoingAway) {
            return;
        }
        ITranKeyguardController.Instance().hookAuthSuccess();
        hookInitKeyguardGoingAway(flags);
        Trace.traceBegin(32L, "keyguardGoingAway");
        this.mService.deferWindowLayout();
        state.mKeyguardGoingAway = true;
        ITranKeyguardController.Instance().hookUpdateKeyguardLocked(isKeyguardLocked());
        try {
            EventLogTags.writeWmSetKeyguardShown(displayId, 1, state.mAodShowing ? 1 : 0, 1, "keyguardGoingAway");
            int transitFlags = convertTransitFlags(flags);
            DisplayContent dc = this.mRootWindowContainer.getDefaultDisplay();
            dc.prepareAppTransition(7, transitFlags);
            dc.mAtmService.getTransitionController().requestTransitionIfNeeded(4, transitFlags, null, dc);
            updateKeyguardSleepToken();
            this.mRootWindowContainer.resumeFocusedTasksTopActivities();
            this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
            this.mRootWindowContainer.addStartingWindowsForVisibleActivities();
            this.mWindowManager.executeAppTransition();
            this.mService.continueWindowLayout();
            Trace.traceEnd(32L);
            this.mService.mRootWindowContainer.getDefaultDisplay().mDisplayContent.getDisplayPolicy().onKeyguardChange();
        } catch (Throwable th) {
            this.mService.continueWindowLayout();
            Trace.traceEnd(32L);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dismissKeyguard(IBinder token, IKeyguardDismissCallback callback, CharSequence message) {
        ActivityRecord activityRecord = ActivityRecord.forTokenLocked(token);
        if (activityRecord == null || !activityRecord.visibleIgnoringKeyguard) {
            failCallback(callback);
            return;
        }
        Slog.i(TAG, "Activity requesting to dismiss Keyguard: " + activityRecord);
        if (activityRecord.getTurnScreenOnFlag() && activityRecord.isTopRunningActivity()) {
            this.mTaskSupervisor.wakeUp("dismissKeyguard");
        }
        if (ITranWindowManagerService.Instance().onDismissKeyguard(callback, message, activityRecord)) {
            return;
        }
        this.mWindowManager.dismissKeyguard(callback, message);
    }

    private void failCallback(IKeyguardDismissCallback callback) {
        try {
            callback.onDismissError();
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to call callback", e);
        }
    }

    private int convertTransitFlags(int keyguardGoingAwayFlags) {
        int result = 256;
        if ((keyguardGoingAwayFlags & 1) != 0) {
            result = 256 | 1;
        }
        if ((keyguardGoingAwayFlags & 2) != 0) {
            result |= 2;
        }
        if ((keyguardGoingAwayFlags & 4) != 0) {
            result |= 4;
        }
        if ((keyguardGoingAwayFlags & 8) != 0) {
            result |= 8;
        }
        if ((keyguardGoingAwayFlags & 16) != 0) {
            return result | 22;
        }
        return result;
    }

    boolean canShowActivityWhileKeyguardShowing(ActivityRecord r) {
        KeyguardDisplayState state = getDisplayState(r.getDisplayId());
        boolean showWhenLockedRaw = r.canShowWhenLocked();
        boolean dismissKeyguardRaw = r.containsDismissKeyguardWindow();
        boolean keyguardLocked = isKeyguardLocked(r.getDisplayId());
        boolean showOnkeyguardFactor = ITranWindowManagerService.Instance().isVisibilityOnKeyguard(r, true, true, true, keyguardLocked, showWhenLockedRaw, dismissKeyguardRaw);
        boolean showWhenLocked = showWhenLockedRaw && showOnkeyguardFactor;
        boolean dismissKeyguard = dismissKeyguardRaw && showOnkeyguardFactor;
        if (dismissKeyguard && canDismissKeyguard() && showWhenLocked) {
            if (state.mDismissingKeyguardActivity != r) {
                return true;
            }
        }
        return false;
    }

    boolean canShowWhileOccluded(boolean dismissKeyguard, boolean showWhenLocked) {
        return showWhenLocked || (dismissKeyguard && !this.mWindowManager.isKeyguardSecure(this.mService.getCurrentUserId()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkKeyguardVisibility(ActivityRecord r) {
        boolean z = true;
        if (r.mDisplayContent.canShowWithInsecureKeyguard() && canDismissKeyguard()) {
            return true;
        }
        boolean isDisplayOccluded = isDisplayOccluded();
        this.mFingerWakeupOptimizePath = false;
        if (!this.mKeyguardGoingAwayQuickly || isDisplayOccluded || r.mDisplayContent.getDisplayId() != 0) {
            if (!isKeyguardOrAodShowing(r.mDisplayContent.getDisplayId())) {
                if (isKeyguardLocked(r.getDisplayId())) {
                    boolean showWhenLockedRaw = r.canShowWhenLocked();
                    boolean dismissKeyguardRaw = r.containsDismissKeyguardWindow();
                    boolean showOnkeyguardFactor = ITranWindowManagerService.Instance().isVisibilityOnKeyguard(r, true, true, false, true, showWhenLockedRaw, dismissKeyguardRaw);
                    boolean showWhenLocked = showWhenLockedRaw && showOnkeyguardFactor;
                    if (!dismissKeyguardRaw || !showOnkeyguardFactor) {
                        z = false;
                    }
                    boolean dismissKeyguard = z;
                    return canShowWhileOccluded(dismissKeyguard, showWhenLocked);
                }
                return true;
            }
            return canShowActivityWhileKeyguardShowing(r);
        }
        this.mFingerWakeupOptimizePath = true;
        if (r != null) {
            r.mVisibleByWakeupOptimizePath = true;
        }
        Slog.i(TAG, "fingerprint pre wakeup scheduled,visible activity= " + r);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateVisibility() {
        for (int displayNdx = this.mRootWindowContainer.getChildCount() - 1; displayNdx >= 0; displayNdx--) {
            DisplayContent display = (DisplayContent) this.mRootWindowContainer.getChildAt(displayNdx);
            if (!display.isRemoving() && !display.isRemoved()) {
                KeyguardDisplayState state = getDisplayState(display.mDisplayId);
                state.updateVisibility(this, display);
                if (state.mRequestDismissKeyguard) {
                    handleDismissKeyguard(display.getDisplayId());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleOccludedChanged(int displayId, ActivityRecord topActivity) {
        int i;
        if (displayId != 0) {
            updateKeyguardSleepToken(displayId);
            return;
        }
        this.mWindowManager.mPolicy.onKeyguardOccludedChangedLw(isDisplayOccluded(0));
        if (isKeyguardLocked(displayId)) {
            this.mService.deferWindowLayout();
            try {
                DisplayContent defaultDisplay = this.mRootWindowContainer.getDefaultDisplay();
                if (isDisplayOccluded(0)) {
                    i = 8;
                } else {
                    i = 9;
                }
                defaultDisplay.requestTransitionAndLegacyPrepare(i, 0);
                updateKeyguardSleepToken(0);
                this.mWindowManager.executeAppTransition();
            } finally {
                this.mService.continueWindowLayout();
            }
        }
        dismissMultiWindowModeForTaskIfNeeded(displayId, topActivity != null ? topActivity.getRootTask() : null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleKeyguardGoingAwayChanged(DisplayContent dc) {
        this.mService.deferWindowLayout();
        try {
            dc.prepareAppTransition(7, 0);
            dc.mAtmService.getTransitionController().requestTransitionIfNeeded(1, 256, null, dc);
            updateKeyguardSleepToken();
            this.mWindowManager.executeAppTransition();
        } finally {
            this.mService.continueWindowLayout();
        }
    }

    private void handleDismissKeyguard(int displayId) {
        if (!this.mWindowManager.isKeyguardSecure(this.mService.getCurrentUserId())) {
            return;
        }
        this.mWindowManager.dismissKeyguard(null, null);
        KeyguardDisplayState state = getDisplayState(displayId);
        state.mDismissalRequested = true;
        DisplayContent dc = this.mRootWindowContainer.getDefaultDisplay();
        if (state.mKeyguardShowing && canDismissKeyguard() && dc.mAppTransition.containsTransitRequest(9)) {
            this.mWindowManager.executeAppTransition();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDisplayOccluded(int displayId) {
        return getDisplayState(displayId).mOccluded;
    }

    boolean canDismissKeyguard() {
        return this.mWindowManager.mPolicy.isKeyguardTrustedLw() || !this.mWindowManager.isKeyguardSecure(this.mService.getCurrentUserId());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isShowingDream() {
        return getDisplayState(0).mShowingDream;
    }

    private void dismissMultiWindowModeForTaskIfNeeded(int displayId, Task currentTaskControllingOcclusion) {
        if (getDisplayState(displayId).mKeyguardShowing && isDisplayOccluded(0) && currentTaskControllingOcclusion != null && currentTaskControllingOcclusion.inFreeformWindowingMode()) {
            currentTaskControllingOcclusion.setWindowingMode(1);
        }
    }

    private void updateKeyguardSleepToken() {
        for (int displayNdx = this.mRootWindowContainer.getChildCount() - 1; displayNdx >= 0; displayNdx--) {
            DisplayContent display = (DisplayContent) this.mRootWindowContainer.getChildAt(displayNdx);
            updateKeyguardSleepToken(display.mDisplayId);
        }
    }

    private void updateKeyguardSleepToken(int displayId) {
        KeyguardDisplayState state = getDisplayState(displayId);
        if (isKeyguardUnoccludedOrAodShowing(displayId)) {
            state.mSleepTokenAcquirer.acquire(displayId);
        } else if (!isKeyguardUnoccludedOrAodShowing(displayId)) {
            state.mSleepTokenAcquirer.release(displayId);
        }
    }

    private KeyguardDisplayState getDisplayState(int displayId) {
        KeyguardDisplayState state = this.mDisplayStates.get(displayId);
        if (state == null) {
            KeyguardDisplayState state2 = new KeyguardDisplayState(this.mService, displayId, this.mSleepTokenAcquirer);
            this.mDisplayStates.append(displayId, state2);
            return state2;
        }
        return state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayRemoved(int displayId) {
        KeyguardDisplayState state = this.mDisplayStates.get(displayId);
        if (state != null) {
            state.onRemoved();
            this.mDisplayStates.remove(displayId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class KeyguardDisplayState {
        private boolean mAodShowing;
        private boolean mDismissalRequested;
        private ActivityRecord mDismissingKeyguardActivity;
        private final int mDisplayId;
        private boolean mKeyguardGoingAway;
        private boolean mKeyguardShowing;
        private boolean mOccluded;
        private boolean mRequestDismissKeyguard;
        private final ActivityTaskManagerService mService;
        private boolean mShowingDream;
        private final ActivityTaskManagerInternal.SleepTokenAcquirer mSleepTokenAcquirer;
        private ActivityRecord mTopOccludesActivity;
        private ActivityRecord mTopTurnScreenOnActivity;

        KeyguardDisplayState(ActivityTaskManagerService service, int displayId, ActivityTaskManagerInternal.SleepTokenAcquirer acquirer) {
            this.mService = service;
            this.mDisplayId = displayId;
            this.mSleepTokenAcquirer = acquirer;
        }

        void onRemoved() {
            this.mTopOccludesActivity = null;
            this.mDismissingKeyguardActivity = null;
            this.mTopTurnScreenOnActivity = null;
            this.mSleepTokenAcquirer.release(this.mDisplayId);
        }

        void updateVisibility(KeyguardController controller, DisplayContent display) {
            boolean z;
            boolean lastOccluded = this.mOccluded;
            boolean lastKeyguardGoingAway = this.mKeyguardGoingAway;
            ActivityRecord lastDismissKeyguardActivity = this.mDismissingKeyguardActivity;
            ActivityRecord lastTurnScreenOnActivity = this.mTopTurnScreenOnActivity;
            this.mRequestDismissKeyguard = false;
            this.mOccluded = false;
            this.mShowingDream = false;
            this.mTopOccludesActivity = null;
            this.mDismissingKeyguardActivity = null;
            this.mTopTurnScreenOnActivity = null;
            KeyguardController.mOccludedActivity = null;
            boolean occludedByActivity = false;
            Task task = getRootTaskForControllingOccluding(display);
            ActivityRecord top = task != null ? task.getTopNonFinishingActivity() : null;
            if (top != null) {
                KeyguardController.mOccludedActivity = top;
                boolean showWhenLockedRaw = top.canShowWhenLocked();
                boolean dismissKeyguardRaw = top.containsDismissKeyguardWindow();
                boolean keyguardOrAodShowing = controller.isKeyguardOrAodShowing(display.mDisplayId);
                boolean keyguardLocked = controller.isKeyguardLocked(display.getDisplayId());
                boolean showOnkeyguardFactor = ITranWindowManagerService.Instance().isVisibilityOnKeyguard(top, true, true, keyguardOrAodShowing, keyguardLocked, showWhenLockedRaw, dismissKeyguardRaw);
                boolean showWhenLocked = showWhenLockedRaw && showOnkeyguardFactor;
                boolean dismissKeyguard = dismissKeyguardRaw && showOnkeyguardFactor;
                if (dismissKeyguard) {
                    this.mDismissingKeyguardActivity = top;
                }
                if (top.getTurnScreenOnFlag() && top.currentLaunchCanTurnScreenOn()) {
                    this.mTopTurnScreenOnActivity = top;
                }
                boolean isKeyguardSecure = controller.mWindowManager.isKeyguardSecure(controller.mService.getCurrentUserId());
                if (top.mDismissKeyguardIfInsecure && this.mKeyguardShowing && !isKeyguardSecure) {
                    this.mKeyguardGoingAway = true;
                } else if (showWhenLocked) {
                    this.mTopOccludesActivity = top;
                }
                occludedByActivity = this.mTopOccludesActivity != null || (this.mDismissingKeyguardActivity != null && task.topRunningActivity() == this.mDismissingKeyguardActivity && controller.canShowWhileOccluded(true, false));
                if (this.mDisplayId != 0) {
                    occludedByActivity |= display.canShowWithInsecureKeyguard() && controller.canDismissKeyguard();
                }
            }
            boolean z2 = display.getDisplayPolicy().isShowingDreamLw() && top != null && top.getActivityType() == 5;
            this.mShowingDream = z2;
            boolean z3 = z2 || occludedByActivity;
            this.mOccluded = z3;
            ActivityRecord activityRecord = this.mDismissingKeyguardActivity;
            this.mRequestDismissKeyguard = (lastDismissKeyguardActivity == activityRecord || z3 || this.mKeyguardGoingAway || activityRecord == null) ? false : true;
            ActivityRecord activityRecord2 = this.mTopTurnScreenOnActivity;
            if (activityRecord2 == lastTurnScreenOnActivity || activityRecord2 == null) {
                z = false;
            } else if (!this.mService.mWindowManager.mPowerManager.isInteractive()) {
                if (this.mRequestDismissKeyguard || occludedByActivity) {
                    controller.mTaskSupervisor.wakeUp("handleTurnScreenOn");
                    z = false;
                    this.mTopTurnScreenOnActivity.setCurrentLaunchCanTurnScreenOn(false);
                } else {
                    z = false;
                }
            } else {
                z = false;
            }
            if (lastOccluded != this.mOccluded) {
                controller.handleOccludedChanged(this.mDisplayId, this.mTopOccludesActivity);
                if (ThunderbackConfig.isVersion4()) {
                    KeyguardController.mDelayHookMultiWindowToMax = true;
                    if (top != null && ITranKeyguardController.Instance().inMultiWindow(top)) {
                        ITranKeyguardController Instance = ITranKeyguardController.Instance();
                        boolean z4 = this.mOccluded;
                        if (z4 && controller.isKeyguardLocked()) {
                            z = true;
                        }
                        KeyguardController.mDelayHookMultiWindowToMax = Instance.hookMultiWindowToDefault(KeyguardController.TAG, z4, z, top);
                        return;
                    }
                    return;
                }
                ITranKeyguardController Instance2 = ITranKeyguardController.Instance();
                boolean z5 = this.mOccluded;
                if (z5 && this.mService.hasMultiWindow() && controller.isKeyguardLocked() && top != null && ITranKeyguardController.Instance().inMultiWindow(top)) {
                    z = true;
                }
                Instance2.hookMultiWindowToMax(KeyguardController.TAG, z5, z);
            } else if (!lastKeyguardGoingAway && this.mKeyguardGoingAway) {
                controller.handleKeyguardGoingAwayChanged(display);
            } else if (KeyguardController.mDelayHookMultiWindowToMax && this.mOccluded && controller.isKeyguardLocked() && ITranKeyguardController.Instance().inMultiWindow(top)) {
                KeyguardController.mDelayHookMultiWindowToMax = ITranKeyguardController.Instance().hookMultiWindowToDefault(KeyguardController.TAG, this.mOccluded, true, top);
            }
        }

        private Task getRootTaskForControllingOccluding(DisplayContent display) {
            return display.getRootTask(new Predicate() { // from class: com.android.server.wm.KeyguardController$KeyguardDisplayState$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return KeyguardController.KeyguardDisplayState.lambda$getRootTaskForControllingOccluding$0((Task) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$getRootTaskForControllingOccluding$0(Task task) {
            return (task == null || !task.isFocusableAndVisible() || task.inPinnedWindowingMode()) ? false : true;
        }

        void dumpStatus(PrintWriter pw, String prefix) {
            StringBuilder sb = new StringBuilder();
            sb.append(prefix);
            sb.append(" KeyguardShowing=").append(this.mKeyguardShowing).append(" AodShowing=").append(this.mAodShowing).append(" KeyguardGoingAway=").append(this.mKeyguardGoingAway).append(" DismissalRequested=").append(this.mDismissalRequested).append("  Occluded=").append(this.mOccluded).append(" DismissingKeyguardActivity=").append(this.mDismissingKeyguardActivity).append(" TurnScreenOnActivity=").append(this.mTopTurnScreenOnActivity).append(" at display=").append(this.mDisplayId);
            pw.println(sb.toString());
        }

        void dumpDebug(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(CompanionMessage.MESSAGE_ID, this.mDisplayId);
            proto.write(1133871366146L, this.mKeyguardShowing);
            proto.write(1133871366147L, this.mAodShowing);
            proto.write(1133871366148L, this.mOccluded);
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        KeyguardDisplayState default_state = getDisplayState(0);
        pw.println(prefix + "KeyguardController:");
        pw.println(prefix + "  mKeyguardShowing=" + default_state.mKeyguardShowing);
        pw.println(prefix + "  mAodShowing=" + default_state.mAodShowing);
        pw.println(prefix + "  mKeyguardGoingAway=" + default_state.mKeyguardGoingAway);
        dumpDisplayStates(pw, prefix);
        pw.println(prefix + "  mDismissalRequested=" + default_state.mDismissalRequested);
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        KeyguardDisplayState default_state = getDisplayState(0);
        long token = proto.start(fieldId);
        proto.write(1133871366147L, default_state.mAodShowing);
        proto.write(1133871366145L, default_state.mKeyguardShowing);
        writeDisplayStatesToProto(proto, 2246267895812L);
        proto.end(token);
    }

    private void dumpDisplayStates(PrintWriter pw, String prefix) {
        for (int i = 0; i < this.mDisplayStates.size(); i++) {
            this.mDisplayStates.valueAt(i).dumpStatus(pw, prefix);
        }
    }

    private void writeDisplayStatesToProto(ProtoOutputStream proto, long fieldId) {
        for (int i = 0; i < this.mDisplayStates.size(); i++) {
            this.mDisplayStates.valueAt(i).dumpDebug(proto, fieldId);
        }
    }

    private void hookSetFingerprintStartTime() {
        if (Build.TRANCARE_SUPPORT) {
            if (this.mFingerprintGoingAwayQuickly && this.mIsScreenOn) {
                this.mService.mRootWindowContainer.getDefaultDisplay().mDisplayContent.getDisplayPolicy().setFingerprintStartTime(SystemClock.elapsedRealtime());
            } else {
                this.mService.mRootWindowContainer.getDefaultDisplay().mDisplayContent.getDisplayPolicy().setFingerprintStartTime(0L);
            }
        }
    }

    public void notifyAuthenticateSucceed(boolean isAuthenticateSucceed) {
        if (TranFpUnlockStateController.getInstance().isAuthenticateSucceed() != isAuthenticateSucceed && this.mKeyguardGoingAwayQuickly) {
            TranFpUnlockStateController.getInstance().setAuthenticateSucceed(isAuthenticateSucceed);
            if (TranFpUnlockStateController.getInstance().isAuthenticateSucceed()) {
                try {
                    this.mService.deferWindowLayout();
                    ITranKeyguardController.Instance().hookAuthSuccess();
                    this.mRootWindowContainer.getDefaultDisplay().requestTransitionAndLegacyPrepare(40, 0);
                    this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
                    this.mWindowManager.executeAppTransition();
                } finally {
                    this.mService.continueWindowLayout();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardGoingAwayQuickly() {
        return this.mKeyguardGoingAwayQuickly;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyKeyguardGoingAwayQuickly(boolean goingAwayQuickly, boolean isScreenOn, int pid, int displayId) {
        this.mIsScreenOn = isScreenOn;
        KeyguardDisplayState state = getDisplayState(displayId);
        if (goingAwayQuickly && !state.mKeyguardShowing && isScreenOn) {
            Slog.i(TAG, "notifyKeyguardGoingAwayQuickly keyguard not showing return");
        } else {
            notifyKeyguardGoingAwayQuickly(goingAwayQuickly, pid);
        }
    }

    void notifyKeyguardGoingAwayQuickly(boolean goingAwayQuickly, int pid) {
        boolean goingAwayQuickly2;
        if (pid == -1) {
            this.mFaceUnlockGoingAwayQuickly = false;
            this.mFingerprintGoingAwayQuickly = false;
        } else if (pid != ActivityManagerService.MY_PID) {
            this.mFaceUnlockGoingAwayQuickly = goingAwayQuickly;
            Slog.i(TAG, "notifyKeyguardGoingAwayQuickly support faceunlock pid");
        } else {
            this.mFingerprintGoingAwayQuickly = goingAwayQuickly;
            hookSetFingerprintStartTime();
        }
        if (this.mFaceUnlockGoingAwayQuickly || this.mFingerprintGoingAwayQuickly) {
            goingAwayQuickly2 = true;
        } else {
            goingAwayQuickly2 = false;
        }
        this.mGoingAwayQuicklyHandler.removeCallbacks(this.mResetKeyguardGoingAwayRunnable);
        if (goingAwayQuickly2) {
            this.mGoingAwayQuicklyHandler.postDelayed(this.mResetKeyguardGoingAwayRunnable, 2200L);
        }
        if (this.mKeyguardGoingAwayQuickly != goingAwayQuickly2) {
            this.mKeyguardGoingAwayQuickly = goingAwayQuickly2;
            Slog.i(TAG, "notifyKeyguardGoingAwayQuickly set  mKeyguardGoingAwayQuickly to " + this.mKeyguardGoingAwayQuickly + ", pid = " + pid + ", MY_PID = " + ActivityManagerService.MY_PID);
            if (TranFpUnlockStateController.getInstance().isAuthenticateSucceed()) {
                TranFpUnlockStateController.getInstance().setAuthenticateSucceed(false);
            }
            try {
                this.mService.deferWindowLayout();
                this.mRootWindowContainer.getDefaultDisplay().requestTransitionAndLegacyPrepare(40, 0);
                this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
                this.mWindowManager.executeAppTransition();
                this.mService.continueWindowLayout();
                this.mService.updateSleepIfNeededLocked();
            } catch (Throwable th) {
                this.mService.continueWindowLayout();
                throw th;
            }
        }
    }

    private void hookSetAuthenticateSucceed(boolean keyguardShowing) {
        TranFpUnlockStateController.getInstance().setKeyguardShown(keyguardShowing);
        if (!keyguardShowing) {
            this.mKeyguardGoingAwayQuickly = false;
            this.mFaceUnlockGoingAwayQuickly = false;
            this.mFingerprintGoingAwayQuickly = false;
            this.mService.mRootWindowContainer.getDefaultDisplay().mDisplayContent.getDisplayPolicy().onKeyguardChange();
            if (TranFpUnlockStateController.getInstance().isAuthenticateSucceed()) {
                TranFpUnlockStateController.getInstance().setAuthenticateSucceed(false);
            }
        }
    }

    private void hookInitKeyguardGoingAway(int flags) {
        int appTransitionFlags = convertTransitFlags(flags);
        if (this.mKeyguardGoingAwayQuickly) {
            int i = appTransitionFlags | 512;
        }
        this.mKeyguardGoingAwayQuickly = false;
        this.mFaceUnlockGoingAwayQuickly = false;
        this.mFingerprintGoingAwayQuickly = false;
    }

    public boolean isDisplayOccluded() {
        return getDisplayState(0).mOccluded;
    }

    private void hookUpdateKeyguardSleepToken(KeyguardDisplayState state, int displayId) {
        if (!this.mKeyguardGoingAwayQuickly) {
            state.mSleepTokenAcquirer.acquire(displayId);
        } else {
            state.mSleepTokenAcquirer.release(displayId);
        }
    }
}
