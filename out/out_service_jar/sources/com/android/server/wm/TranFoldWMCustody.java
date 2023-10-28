package com.android.server.wm;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.SyncFence;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Trace;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.DisplayAddress;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.window.ClientWindowFrames;
import android.window.WindowContainerTransaction;
import com.android.internal.util.ToBooleanFunction;
import com.android.server.am.HostingRecord;
import com.android.server.display.TranFoldDisplayCustody;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.TranFoldWMCustody;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
/* loaded from: classes2.dex */
public final class TranFoldWMCustody {
    public static final String TAG = "os.fold.wm";
    private static final int WAITING_FOR_DRAWN_TIMEOUT = 1500;
    private static final int WAITING_FOR_SWITCH_USER_TIMEOUT = 5000;
    private final AnimationCustody mAnimationCustody;
    private final DisplayUniqueIdMap mDisplayUniqueIdMap;
    private final RedrawWindow mRedrawWindow;
    private final WaitingDrawnWindow mWaitingDrawnWindow;
    private final WallPaperGuard mWallPaperGuard;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class Holder {
        static final TranFoldWMCustody instance = new TranFoldWMCustody();

        private Holder() {
        }
    }

    public static TranFoldWMCustody instance() {
        return Holder.instance;
    }

    private TranFoldWMCustody() {
        RedrawWindow redrawWindow = new RedrawWindow();
        this.mRedrawWindow = redrawWindow;
        AnimationCustody animationCustody = new AnimationCustody();
        this.mAnimationCustody = animationCustody;
        this.mDisplayUniqueIdMap = new DisplayUniqueIdMap();
        this.mWaitingDrawnWindow = new WaitingDrawnWindow(0, animationCustody);
        this.mWallPaperGuard = new WallPaperGuard(redrawWindow);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class WMSHolder {
        static final WindowManagerService instance = (WindowManagerService) ServiceManager.getService("window");

        private WMSHolder() {
        }
    }

    public static WindowManagerService getWindowManagerService() {
        return WMSHolder.instance;
    }

    public int getWaitingForDrawnTimeout(int timeout) {
        if (TranFoldDisplayCustody.disable()) {
            return timeout;
        }
        return this.mWaitingDrawnWindow.getWaitingForDrawnTimeout(timeout);
    }

    public void keyguardDrawnTimeout() {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.keyguardDrawnTimeout();
    }

    public void setDeviceStateLockedBegin() {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.setDeviceStateLockedBegin();
    }

    public void transitionToPendingStateLockedBegin() {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.transitionToPendingStateLockedBegin();
    }

    public void updateDisplayLayout(int displayId) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.updateDisplayLayout(displayId);
    }

    public void onFadeOnAnimatorUpdate(int displayId, ValueAnimator animation) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWallPaperGuard.onFadeOnAnimatorUpdate(displayId, animation);
    }

    public boolean addWaitingDrawnCallback(int displayId, String name, long timeout, Choreographer.FrameCallback callback, Looper looper) {
        if (TranFoldDisplayCustody.disable()) {
            return false;
        }
        return this.mWaitingDrawnWindow.addCallback(displayId, name, timeout, callback, looper, true);
    }

    public void setWallpaperRedrawPolicy(ToBooleanFunction<ValueAnimator> policy) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWallPaperGuard.setRedrawPolicy(policy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateCurrentUniqueDisplayId(DisplayContent displayContent) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mDisplayUniqueIdMap.setDisplayUniqueId(displayContent.getDisplayId(), displayContent.mCurrentUniqueDisplayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void computeScreenConfiguration(Configuration config, DisplayInfo displayInfo) {
        if (!TranFoldDisplayCustody.disable() && (displayInfo.address instanceof DisplayAddress.Physical)) {
            setDisplayPhysicalId(config, displayInfo.address.getPhysicalDisplayId());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startWaitForWindowsDrawn(WindowContainer<?> container) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        Slog.d(TAG, "startWaitForWindowsDrawn " + getName(container) + ((String) container.mWaitingForDrawn.stream().map(new Function() { // from class: com.android.server.wm.TranFoldWMCustody$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((WindowState) obj).toString();
            }
        }).collect(Collectors.joining(", "))));
        this.mWaitingDrawnWindow.startWaitForWindowsDrawn(container);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceAllWindowsDrawnDone(WindowContainer<?> container) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.forceAllWindowsDrawnDone(container);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeImmediately(WindowState w) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mRedrawWindow.removeImmediately(w);
        this.mWaitingDrawnWindow.removeImmediately(w);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean notifyAllWindowsDrawnDone(WindowContainer<?> container) {
        if (TranFoldDisplayCustody.disable()) {
            return false;
        }
        return this.mWaitingDrawnWindow.notifyAllWindowsDrawnDone(container);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLastReportedConfiguration(WindowState win, ClientWindowFrames outFrames, MergedConfiguration oldReportedConfiguration, Configuration globalConfig, Configuration overrideConfig, boolean useLatestConfig, boolean relayoutVisible, boolean shouldSendRedrawForSync) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        Configuration oldMergedConfiguration = oldReportedConfiguration.getMergedConfiguration();
        Configuration newMergedConfiguration = new MergedConfiguration(globalConfig, overrideConfig).getMergedConfiguration();
        Slog.d(TAG, "updateLastReportedConfiguration " + getName(win) + " display:" + win.getDisplayId() + " useLatestConfig:" + useLatestConfig + " relayoutVisible:" + relayoutVisible + " shouldSendRedrawForSync:" + shouldSendRedrawForSync + " outFrames:" + outFrames + " oldMergedConfiguration:" + oldMergedConfiguration + " newMergedConfiguration:" + newMergedConfiguration);
        this.mWaitingDrawnWindow.updateLastReportedConfiguration(win, oldMergedConfiguration, newMergedConfiguration, useLatestConfig, shouldSendRedrawForSync);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resized(WindowState win, boolean reportDraw, boolean forceRelayout, DisplayContent displayContent, ClientWindowFrames clientWindowFrames, int syncSeqId) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mRedrawWindow.resized(win, reportDraw, syncSeqId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishDrawing(WindowState win, SurfaceControl.Transaction postDrawTransaction, int syncSeqId) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mRedrawWindow.finishDrawing(win);
        this.mWaitingDrawnWindow.finishDrawing(win, postDrawTransaction, syncSeqId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void createWindowStateAnimatorSurface(WindowStateAnimator windowStateAnimator) {
        TranFoldDisplayCustody.disable();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroyWindowStateAnimatorSurface(WindowStateAnimator windowStateAnimator) {
        TranFoldDisplayCustody.disable();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void doStartFreezingDisplay(int exitAnim, int enterAnim, DisplayContent displayContent, int overrideOriginalRotation) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.doStartFreezingDisplay(exitAnim, enterAnim, displayContent, overrideOriginalRotation);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void doStopFreezingDisplayLocked(boolean updateRotation, boolean configChanged, DisplayContent displayContent) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.doStopFreezingDisplay(displayContent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyWindowOrganizerConfigChanges(WindowContainer<?> container, WindowContainerTransaction.Change change, int configMask, int windowMask) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.applyWindowOrganizerConfigChanges(container, change);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityRecordSetVisible(ActivityRecord ar, boolean visible) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.onActivityRecordSetVisible(ar, visible);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onConfigurationChanged(WindowState w, Configuration newParentConfig) {
        TranFoldDisplayCustody.disable();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean disableCreateSnapshotBufferInner(SurfaceControl target, Rect bounds) {
        if (TranFoldDisplayCustody.disable()) {
            return false;
        }
        return this.mWaitingDrawnWindow.disableCreateSnapshotBufferInner(target, bounds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onStartActivityUnchecked(int result, ActivityRecord r) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.onStartActivityUnchecked(result, r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startMoveAnimation(WindowState w, Point oldPosition, Point newPosition) {
        if (TranFoldDisplayCustody.disable()) {
            return false;
        }
        return this.mWaitingDrawnWindow.startMoveAnimation(w, oldPosition, newPosition);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean disableWindowAnimations() {
        if (TranFoldDisplayCustody.disable()) {
            return false;
        }
        return this.mAnimationCustody.disableWindowAnimations();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean disableTransitionAnimations() {
        if (TranFoldDisplayCustody.disable()) {
            return false;
        }
        return this.mAnimationCustody.disableTransitionAnimations();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean disableViewAnimations() {
        if (TranFoldDisplayCustody.disable()) {
            return false;
        }
        return this.mAnimationCustody.disableViewAnimations();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prepareRelayout(WindowState w) {
        if (TranFoldDisplayCustody.disable()) {
            return;
        }
        this.mWaitingDrawnWindow.prepareRelayout(w);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class WallPaperGuard {
        private static final String TAG = "os.fold.wm.wallpaper";
        private static final DefaultPolicy sDefaultPolicy = new DefaultPolicy();
        private ToBooleanFunction<ValueAnimator> mPolicy;
        private final Object mPolicyLock = new Object();
        private final RedrawWindow mRedrawWindow;

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public static class DefaultPolicy {
            private static final float DEFAULT_FADE_VALUE_TRIGGER_WALLPAPER_REDRAW = 60.0f;
            private boolean mRunning;
            private boolean mTrigger;

            private DefaultPolicy() {
            }

            public boolean apply(ValueAnimator animator) {
                float value = ((Float) animator.getAnimatedValue()).floatValue();
                boolean z = true;
                if (Float.compare(value, 1.0f) == 0) {
                    if (!this.mRunning || this.mTrigger) {
                        z = false;
                    }
                    boolean res = z;
                    this.mRunning = false;
                    this.mTrigger = false;
                    if (res) {
                        Slog.d(WallPaperGuard.TAG, "redrawing default policy trigger value:" + value + " current:" + animator.getCurrentPlayTime() + " duration:" + animator.getDuration());
                    }
                    return res;
                }
                this.mRunning = true;
                if (!this.mTrigger) {
                    long current = animator.getCurrentPlayTime();
                    long duration = animator.getDuration();
                    long left = current > duration ? 0L : duration - current;
                    if (((float) left) <= DEFAULT_FADE_VALUE_TRIGGER_WALLPAPER_REDRAW) {
                        Slog.d(WallPaperGuard.TAG, "redrawing default policy trigger value:" + value + " current:" + current + " duration:" + duration);
                        this.mTrigger = true;
                        return true;
                    }
                }
                return false;
            }
        }

        public WallPaperGuard(RedrawWindow redrawWindow) {
            this.mRedrawWindow = redrawWindow;
        }

        public void setRedrawPolicy(ToBooleanFunction<ValueAnimator> policy) {
            synchronized (this.mPolicyLock) {
                Slog.d(TAG, "set policy from " + this.mPolicy + " to " + policy);
                this.mPolicy = policy;
            }
        }

        public void onFadeOnAnimatorUpdate(final int displayId, ValueAnimator animation) {
            if (disable(displayId)) {
                return;
            }
            if (needRedraw(animation)) {
                final WindowManagerService service = TranFoldWMCustody.getWindowManagerService();
                if (service == null) {
                    return;
                }
                service.mH.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.TranFoldWMCustody$WallPaperGuard$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        TranFoldWMCustody.WallPaperGuard.this.m8438x8e83abf(displayId, service);
                    }
                });
            }
            if (this.mPolicy != null && Float.compare(((Float) animation.getAnimatedValue()).floatValue(), 1.0f) == 0) {
                Slog.d(TAG, "reset policy.");
                this.mPolicy = null;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onFadeOnAnimatorUpdate$1$com-android-server-wm-TranFoldWMCustody$WallPaperGuard  reason: not valid java name */
        public /* synthetic */ void m8438x8e83abf(int displayId, WindowManagerService service) {
            redrawWallpaper(displayId, service, new Runnable() { // from class: com.android.server.wm.TranFoldWMCustody$WallPaperGuard$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    Slog.d(TranFoldWMCustody.WallPaperGuard.TAG, "redrawing start.");
                }
            });
        }

        private void redrawWallpaper(int displayId, WindowManagerService service, Runnable finishedCallback) {
            WindowState wallpaper;
            if (disable(displayId) || service == null) {
                return;
            }
            synchronized (service.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    DisplayContent dc = service.mRoot.getDisplayContent(displayId);
                    if (dc != null && (wallpaper = dc.mWallpaperController.getTopVisibleWallpaper()) != null && wallpaper.mWinAnimator != null) {
                        String name = wallpaper.getName();
                        if (this.mRedrawWindow.redraw(wallpaper, finishedCallback)) {
                            Slog.d(TAG, "redrawing prepare " + name);
                        }
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        private boolean needRedraw(ValueAnimator animation) {
            ToBooleanFunction<ValueAnimator> policy;
            synchronized (this.mPolicyLock) {
                policy = this.mPolicy;
            }
            if (policy != null) {
                return policy.apply(animation);
            }
            return sDefaultPolicy.apply(animation);
        }

        private static boolean disable(int displayId) {
            return TranFoldDisplayCustody.disableWallpaperRedraw() || displayId != 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class DisplayUniqueIdMap {
        private final SparseArray<String> mDisplayUniqueId;
        private final Object mDisplayUniqueIdLock;

        private DisplayUniqueIdMap() {
            this.mDisplayUniqueIdLock = new Object();
            this.mDisplayUniqueId = new SparseArray<>();
        }

        public void setDisplayUniqueId(int displayId, String uniqueDisplayId) {
            synchronized (this.mDisplayUniqueIdLock) {
                Slog.d(TranFoldWMCustody.TAG, "display content " + displayId + " update mCurrentUniqueDisplayId from " + this.mDisplayUniqueId.get(displayId, null) + " to " + uniqueDisplayId);
                this.mDisplayUniqueId.put(displayId, uniqueDisplayId);
            }
        }

        public boolean needUpdate(int displayId, String uniqueDisplayId) {
            boolean res;
            synchronized (this.mDisplayUniqueIdLock) {
                res = false;
                String oldId = this.mDisplayUniqueId.get(displayId, null);
                if (oldId == null) {
                    Slog.e(TranFoldWMCustody.TAG, "display content " + displayId + " not found by uniqueDisplayId " + uniqueDisplayId);
                } else {
                    res = !Objects.equals(oldId, uniqueDisplayId);
                    if (res) {
                        Slog.d(TranFoldWMCustody.TAG, "display content " + displayId + " need update mCurrentUniqueDisplayId from " + oldId + " to " + uniqueDisplayId);
                    }
                }
            }
            return res;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class WaitingDrawnWindow {
        private static final int STATE_DISPLAY_INFO_UPDATING = 2;
        private static final int STATE_DONE = 5;
        private static final int STATE_UNKNOWN = 0;
        private static final int STATE_WM_PENDING = 3;
        private static final int STATE_WM_READY = 4;
        private static final String TAG = "os.fold.wm.wait_drawn";
        private static final String WAITING_STATE_DEVICE_STATE = "basic";
        private final AnimationCustody mAnimationCustody;
        private final int mDisplayId;
        private boolean mDisplayTransiting;
        private boolean mDisplayTransitingPending;
        private final HandlerExecutor mExecutor;
        private String mFakePackageNameAnimation;
        private String mFakePackageNameDisplayPending;
        private String mFakePackageNameUserSwitching;
        private String mFakePackageNameWMPending;
        private final Handler mHandler;
        private final String mName;
        private final WaitingDrawnPolicy mPolicy;
        private boolean mSwapping;
        private volatile boolean mUpdatingConfig;
        private boolean mUpdatingLayout;
        private int mWMState;
        private final Choreographer.FrameCallback mCallbackRequestTraversal = new Choreographer.FrameCallback() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda12
            @Override // android.view.Choreographer.FrameCallback
            public final void doFrame(long j) {
                TranFoldWMCustody.WaitingDrawnWindow.lambda$new$0(j);
            }
        };
        private boolean mUserSwitching = false;
        private final Object mWaitingStatesLock = new Object();
        private final ArrayMap<String, WaitingState> mWaitingStates = new ArrayMap<>();

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public static class WaitingDrawnPolicy {
            private boolean mDisableAll;
            private final int mDisplayId;
            private final boolean mOnlyActivity;
            private final ArraySet<String> mPackageBlackList;
            private int mTimeout;
            private final boolean mWaitPresent;
            private final ArraySet<String> mWindowNameWhiteList;
            private final ArraySet<Integer> mWindowTypeBlackList;
            private final ArraySet<Integer> mWindowTypeWhiteList;

            public WaitingDrawnPolicy(int displayId, int timeout, List<Integer> windowTypeBlackList, List<Integer> windowTypeWhiteList, List<String> packageBlackList, boolean onlyActivity, boolean waitPresent) {
                this.mDisplayId = displayId;
                this.mTimeout = timeout;
                ArraySet<Integer> arraySet = new ArraySet<>();
                this.mWindowTypeWhiteList = arraySet;
                if (windowTypeWhiteList != null) {
                    arraySet.addAll(windowTypeWhiteList);
                }
                ArraySet<Integer> arraySet2 = new ArraySet<>();
                this.mWindowTypeBlackList = arraySet2;
                if (windowTypeBlackList != null) {
                    arraySet2.addAll(windowTypeBlackList);
                }
                ArraySet<String> arraySet3 = new ArraySet<>();
                this.mPackageBlackList = arraySet3;
                if (packageBlackList != null) {
                    arraySet3.addAll(packageBlackList);
                }
                this.mOnlyActivity = onlyActivity;
                this.mWaitPresent = waitPresent;
                this.mWindowNameWhiteList = new ArraySet<>();
            }

            public void setDisableAll(boolean disableAll) {
                if (this.mDisableAll != disableAll) {
                    Slog.d(WaitingDrawnWindow.TAG, "setDisableAll " + disableAll);
                    this.mDisableAll = disableAll;
                }
            }

            public void addWindowNameToWhiteList(String windowName) {
                this.mWindowNameWhiteList.add(windowName);
            }

            public void clearOverlay() {
                setDisableAll(false);
                this.mWindowNameWhiteList.clear();
            }

            public boolean needWaiting(WindowState windowState) {
                return this.mDisplayId == windowState.getDisplayId() && (inWhiteList(windowState) || !inBlackList(windowState));
            }

            public boolean inWhiteList(WindowState windowState) {
                return !this.mDisableAll && (this.mWindowNameWhiteList.contains(windowState.getName()) || this.mWindowTypeWhiteList.contains(Integer.valueOf(windowState.getWindowType())));
            }

            private boolean inBlackList(WindowState windowState) {
                if (this.mWindowTypeBlackList.contains(Integer.valueOf(windowState.getWindowType())) || this.mPackageBlackList.contains(TranFoldWMCustody.getPackageName(windowState))) {
                    return true;
                }
                return this.mOnlyActivity && windowState.getActivityRecord() == null;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$new$0(long time) {
            WindowManagerService service = TranFoldWMCustody.getWindowManagerService();
            synchronized (service.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    service.requestTraversal();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        WaitingDrawnWindow(int displayId, AnimationCustody animationCustody) {
            this.mDisplayId = displayId;
            String str = "basic_" + displayId;
            this.mName = str;
            Handler handler = new Handler(TranFoldWMCustody.getWindowManagerService().mH.getLooper());
            this.mHandler = handler;
            this.mExecutor = new HandlerExecutor(handler);
            this.mPolicy = new WaitingDrawnPolicy(displayId, getMaxWaitingForDrawnTimeout(), Arrays.asList(2019, 2040), Arrays.asList(1, 2, 3, 4, 1000, 1001, 1002, 1003, 1004, 1005), null, true, false);
            this.mAnimationCustody = animationCustody;
            getOrCreateNewWaitingStateLocked(displayId, str);
            setWMState(0);
        }

        public int getWaitingForDrawnTimeout(int timeout) {
            int res;
            if (disable()) {
                return timeout;
            }
            synchronized (this.mWaitingStatesLock) {
                res = Math.max(getWaitingForDrawnTimeoutLocked(), timeout);
                if (res > this.mPolicy.mTimeout) {
                    this.mPolicy.mTimeout = res;
                    Slog.d(TAG, "update timeout to " + res);
                }
                if (res != timeout) {
                    Slog.d(TAG, "reset timeout to " + res);
                }
            }
            return res;
        }

        public void keyguardDrawnTimeout() {
            if (disable()) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                Slog.d(TAG, "keyguard drawn is time out.");
                wmAllDrawnDoneLocked();
            }
        }

        public void setDeviceStateLockedBegin() {
            if (disable()) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                if (this.mDisplayTransiting) {
                    Slog.e(TAG, "setDeviceStateLockedBegin invalid state.");
                } else {
                    this.mDisplayTransiting = true;
                }
            }
        }

        public void transitionToPendingStateLockedBegin() {
            if (disable()) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                this.mDisplayTransitingPending = true;
            }
        }

        public void updateDisplayLayout(int displayId) {
            if (disable()) {
                return;
            }
            final WindowManagerService service = TranFoldWMCustody.getWindowManagerService();
            synchronized (this.mWaitingStatesLock) {
                if (displayId == this.mDisplayId) {
                    if (service != null) {
                        if (service.mH.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda19
                            @Override // java.lang.Runnable
                            public final void run() {
                                TranFoldWMCustody.WaitingDrawnWindow.this.m8422xfa4035ea(service);
                            }
                        })) {
                            this.mUpdatingLayout = true;
                            this.mAnimationCustody.disableAnimations(true, service);
                            Slog.d(TAG, "post updating display layout.");
                        }
                    } else {
                        Slog.e(TAG, "failed to post updating display layout!");
                    }
                }
                if (!this.mUpdatingLayout) {
                    final String fakePackageDisplay = getFakePackageNameDisplayPendingLocked();
                    forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda20
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).removePackage(fakePackageDisplay);
                        }
                    });
                }
                if (!this.mDisplayTransiting) {
                    Slog.e(TAG, "updateDisplayLayout invalid state.");
                    return;
                }
                this.mDisplayTransitingPending = false;
                this.mDisplayTransiting = false;
            }
        }

        public void doStartFreezingDisplay(int exitAnim, int enterAnim, DisplayContent displayContent, int overrideOriginalRotation) {
            if (!disable(displayContent) && isUserSwitchAnim(exitAnim, enterAnim, overrideOriginalRotation)) {
                synchronized (this.mWaitingStatesLock) {
                    Slog.d(TAG, (this.mUserSwitching ? "re " : "") + "freezing screen for user switch");
                    this.mUserSwitching = true;
                }
            }
        }

        public void doStopFreezingDisplay(DisplayContent displayContent) {
            if (disable(displayContent)) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                if (this.mUserSwitching) {
                    Slog.d(TAG, "stop freezing screen for user switch.");
                    this.mUserSwitching = false;
                    final String fakePackage = getFakePackageNameUserSwitching();
                    forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda22
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).removePackage(fakePackage);
                        }
                    });
                }
            }
        }

        public void applyWindowOrganizerConfigChanges(final WindowContainer<?> container, final WindowContainerTransaction.Change change) {
            if (disable()) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).applyWindowOrganizerConfigChanges(WindowContainer.this, change);
                    }
                });
            }
        }

        public void onActivityRecordSetVisible(final ActivityRecord ar, final boolean visible) {
            if (disable(ar.getDisplayId())) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                if (this.mUpdatingConfig) {
                    Slog.d(TAG, "ignore " + TranFoldWMCustody.getName(ar) + " visible to " + visible + " when updating config");
                } else {
                    forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda16
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).onActivityRecordSetVisible(ActivityRecord.this, visible);
                        }
                    });
                }
            }
        }

        public boolean disableCreateSnapshotBufferInner(SurfaceControl target, Rect bounds) {
            if (this.mUpdatingConfig && TranFoldDisplayCustody.instance().isSettingPowerMode()) {
                Slog.w(TAG, "not ready to capture layers when setting power mode  target:" + target + " bounds:" + bounds.toShortString(), new Throwable());
                return true;
            }
            return false;
        }

        public void onStartActivityUnchecked(int result, final ActivityRecord ar) {
            if (disable(ar.getDisplayId())) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                if (result != 0) {
                    return;
                }
                forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda14
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).onStartActivityUnchecked(ActivityRecord.this);
                    }
                });
            }
        }

        public boolean startMoveAnimation(WindowState w, Point oldPosition, Point newPosition) {
            if (disable(w) || oldPosition.equals(newPosition) || w.mSurfaceControl == null || !w.mSurfaceControl.isValid()) {
                return false;
            }
            synchronized (this.mWaitingStatesLock) {
                if (this.mPolicy.needWaiting(w) && (this.mUpdatingLayout || this.mSwapping)) {
                    Slog.d(TAG, TranFoldWMCustody.getName(w) + " skip move animation from " + oldPosition + " to " + newPosition);
                    final String fakePackage = getFakePackageNameAnimation(w.getName());
                    forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda6
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).addPackage(fakePackage);
                        }
                    });
                    w.getPendingTransaction().setPosition(w.mSurfaceControl, newPosition.x, newPosition.y);
                    w.getPendingTransaction().addTransactionCommittedListener(this.mExecutor, new SurfaceControl.TransactionCommittedListener() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda7
                        public final void onTransactionCommitted() {
                            TranFoldWMCustody.WaitingDrawnWindow.this.m8420x9bbb745a(fakePackage);
                        }
                    });
                    return true;
                }
                return false;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$startMoveAnimation$9$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow  reason: not valid java name */
        public /* synthetic */ void m8420x9bbb745a(final String fakePackage) {
            synchronized (this.mWaitingStatesLock) {
                forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda15
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).removePackage(fakePackage);
                    }
                });
            }
        }

        public void prepareRelayout(WindowState w) {
            if (disable(w)) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                if (this.mSwapping && !w.mLayoutNeeded) {
                    w.mLayoutNeeded = true;
                    Slog.d(TAG, TranFoldWMCustody.getName(w) + " need layout when display swapping.");
                }
            }
        }

        public void startWaitForWindowsDrawn(WindowContainer<?> container) {
            List<WindowState> whiteList;
            if (disable(container)) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                boolean restart = (this.mSwapping || this.mUpdatingLayout) ? false : true;
                setWMState(3);
                if (restart) {
                    List<WindowState> whiteList2 = new ArrayList<>();
                    getWindowListNoSwapping(container, whiteList2);
                    if (whiteList2.isEmpty()) {
                        this.mPolicy.setDisableAll(true);
                    } else {
                        for (WindowState w : whiteList2) {
                            this.mPolicy.addWindowNameToWhiteList(w.getName());
                        }
                    }
                    whiteList = whiteList2;
                } else {
                    whiteList = new ArrayList<>(container.mWaitingForDrawn);
                }
                final String fakePackageDisplay = getFakePackageNameDisplayPendingLocked();
                final String fakePackageWM = getFakePackageNameWMPendingLocked();
                final String reason = stateToString(3);
                final boolean z = restart;
                final List<WindowState> list = whiteList;
                forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda8
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        TranFoldWMCustody.WaitingDrawnWindow.this.m8421x9f2991b0(z, reason, fakePackageDisplay, fakePackageWM, list, (TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj);
                    }
                });
                addCallbackToNotifyWM(null);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$startWaitForWindowsDrawn$10$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow  reason: not valid java name */
        public /* synthetic */ void m8421x9f2991b0(boolean restart, String reason, String fakePackageDisplay, String fakePackageWM, List whiteList, WaitingState ws) {
            ws.start(true, restart, reason);
            if (this.mUpdatingLayout || (restart && this.mDisplayTransitingPending)) {
                ws.addPackage(fakePackageDisplay);
            }
            ws.addPackage(fakePackageWM);
            Iterator it = whiteList.iterator();
            while (it.hasNext()) {
                WindowState w = (WindowState) it.next();
                ws.add(w, false, reason);
            }
        }

        public boolean addCallback(int displayId, String name, long timeout, Choreographer.FrameCallback callback, Looper looper, boolean immediately) {
            String name2;
            if (!disable(displayId) && callback != null) {
                if (!TextUtils.isEmpty(name)) {
                    name2 = name;
                } else {
                    name2 = this.mName;
                }
                synchronized (this.mWaitingStatesLock) {
                    WaitingState ws = getOrCreateNewWaitingStateLocked(displayId, name2);
                    return ws.addCallback(callback, timeout, looper, !this.mUpdatingLayout && immediately);
                }
            }
            return false;
        }

        public boolean notifyAllWindowsDrawnDone(WindowContainer<?> container) {
            boolean z;
            if (disable(container)) {
                return false;
            }
            synchronized (this.mWaitingStatesLock) {
                z = wmAllDrawnDoneLocked() ? false : true;
            }
            return z;
        }

        public void forceAllWindowsDrawnDone(WindowContainer<?> container) {
            if (disable(container)) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda23
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).end(true, "wm timeout");
                    }
                });
            }
        }

        public void removeImmediately(final WindowState w) {
            if (disable()) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda13
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).removeImmediately(WindowState.this);
                    }
                });
            }
        }

        public void updateLastReportedConfiguration(final WindowState w, final Configuration oldMergedConfiguration, final Configuration newMergedConfiguration, final boolean useLatestConfig, final boolean shouldSendRedrawForSync) {
            if (disable(w) || !w.isVisible()) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda21
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).updateLastReportedConfiguration(WindowState.this, oldMergedConfiguration, newMergedConfiguration, useLatestConfig, shouldSendRedrawForSync);
                    }
                });
            }
        }

        public void finishDrawing(final WindowState w, final SurfaceControl.Transaction postDrawTransaction, final int syncSeqId) {
            if (disable(w)) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                if (postDrawTransaction == null && syncSeqId == Integer.MAX_VALUE) {
                    forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda17
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).removeWaitingDrawn(WindowState.this.getName(), null, "die");
                        }
                    });
                } else {
                    forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda18
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).finishDrawing(WindowState.this, postDrawTransaction, syncSeqId);
                        }
                    });
                }
            }
        }

        public void addPackage(final String name) {
            if (disable()) {
                return;
            }
            synchronized (this.mWaitingStatesLock) {
                forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda24
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).addPackage(name);
                    }
                });
            }
        }

        private boolean disable() {
            return TranFoldDisplayCustody.disableWaitingDrawn() || !TranFoldWMCustody.getWindowManagerService().useBLASTSync();
        }

        private boolean disable(int displayId) {
            return disable() || displayId != this.mDisplayId;
        }

        private boolean disable(WindowState w) {
            return disable(w.getDisplayId());
        }

        private boolean disable(WindowContainer<?> container) {
            return disable() || !isWindowContainerValid(container);
        }

        private int getWaitingForDrawnTimeoutLocked() {
            if (this.mUserSwitching) {
                return getMaxWaitingForDrawnTimeout();
            }
            return 1500;
        }

        private static int getMaxWaitingForDrawnTimeout() {
            return Math.max(1500, 5000);
        }

        private void addCallbackToNotifyWM(WindowManagerService s) {
            if (s == null) {
                s = TranFoldWMCustody.getWindowManagerService();
            }
            if (s == null) {
                return;
            }
            WindowManagerService service = s;
            addCallback(this.mDisplayId, this.mName, this.mPolicy.mTimeout, this.mCallbackRequestTraversal, service.mH.getLooper(), false);
        }

        private boolean isWindowContainerValid(WindowContainer<?> container) {
            return (container instanceof RootWindowContainer) || ((container instanceof DisplayContent) && ((DisplayContent) container).getDisplayId() == this.mDisplayId);
        }

        private boolean wmAllDrawnDoneLocked() {
            if (this.mWMState < 4) {
                setWMState(4);
                forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda9
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).handleWMReady();
                    }
                });
            }
            return checkAllDrawnDoneLocked();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean checkAllDrawnDoneLocked() {
            boolean done = true;
            for (WaitingState ws : this.mWaitingStates.values()) {
                if (!ws.end(false, "checkAllDrawnDoneLocked")) {
                    done = false;
                }
            }
            if (done && this.mWMState == 4) {
                setWMState(5);
                this.mSwapping = false;
                this.mAnimationCustody.disableAnimations(false, (WindowManagerService) null);
                this.mPolicy.clearOverlay();
            }
            return done;
        }

        private WaitingState getOrCreateNewWaitingStateLocked(final int displayId, final String name) {
            WaitingState ws = this.mWaitingStates.computeIfAbsent(name, new Function() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda10
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return TranFoldWMCustody.WaitingDrawnWindow.this.m8418x743b4ce9(displayId, name, (String) obj);
                }
            });
            if (!this.mName.equals(name)) {
                WaitingState prima = this.mWaitingStates.computeIfAbsent(this.mName, new Function() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda11
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return TranFoldWMCustody.WaitingDrawnWindow.this.m8419x7a3f1848(displayId, (String) obj);
                    }
                });
                if (prima.isWaitingLocked()) {
                    ws.start(false, false, "getOrCreateNewWaitingStateLocked");
                }
            }
            return ws;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getOrCreateNewWaitingStateLocked$17$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow  reason: not valid java name */
        public /* synthetic */ WaitingState m8418x743b4ce9(int displayId, String name, String key) {
            return new WaitingState(displayId, name, this.mHandler.getLooper());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$getOrCreateNewWaitingStateLocked$18$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow  reason: not valid java name */
        public /* synthetic */ WaitingState m8419x7a3f1848(int displayId, String key) {
            return new WaitingState(displayId, this.mName, this.mHandler.getLooper());
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: doUpdateDisplayLayout */
        public void m8422xfa4035ea(WindowManagerService service) {
            Trace.traceBegin(32L, "os.fold.wm.wait_drawn.updateDisplayLayout");
            try {
                synchronized (service.mGlobalLock) {
                    WindowManagerService.boostPriorityForLockedSection();
                    final DisplayContent displayContent = service.mRoot.getDisplayContent(this.mDisplayId);
                    final ArrayList<WindowState> windows = new ArrayList<>();
                    displayContent.forAllWindows(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda2
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            TranFoldWMCustody.WaitingDrawnWindow.this.m8416xc3148f4c(windows, (WindowState) obj);
                        }
                    }, true);
                    synchronized (this.mWaitingStatesLock) {
                        if (!this.mUpdatingLayout) {
                            Slog.w(TAG, "skip updating display info");
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        this.mUpdatingLayout = false;
                        forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda3
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                TranFoldWMCustody.WaitingDrawnWindow.this.m8417x47680976((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj);
                            }
                        });
                        addCallbackToNotifyWM(service);
                        forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda4
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).preAdd(DisplayContent.this, windows);
                            }
                        });
                        if (!this.mSwapping) {
                            this.mSwapping = true;
                        }
                        if (this.mUserSwitching) {
                            Slog.d(TAG, "should pending for user switch.");
                            displayContent.setRotationAnimation(null);
                            addPackage(getFakePackageNameUserSwitching());
                        }
                        final String fakePackageDisplay = getFakePackageNameDisplayPendingLocked();
                        forAllWaitingStateLocked(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda5
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                ((TranFoldWMCustody.WaitingDrawnWindow.WaitingState) obj).removePackage(fakePackageDisplay);
                            }
                        });
                        Trace.traceBegin(32L, "displayContent.updateDisplayInfo");
                        this.mUpdatingConfig = true;
                        displayContent.updateDisplayInfo();
                        this.mUpdatingConfig = false;
                        Trace.traceEnd(32L);
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } finally {
                Trace.traceEnd(32L);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$doUpdateDisplayLayout$19$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow  reason: not valid java name */
        public /* synthetic */ void m8416xc3148f4c(ArrayList windows, WindowState w) {
            if (w.isOnScreen() && w.isVisible() && w.getDisplayId() == this.mDisplayId && w.mAttrs.alpha != 0.0f && this.mPolicy.inWhiteList(w)) {
                windows.add(w);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$doUpdateDisplayLayout$20$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow  reason: not valid java name */
        public /* synthetic */ void m8417x47680976(WaitingState ws) {
            ws.start(true, !this.mSwapping, "updateDisplayLayout" + (this.mSwapping ? " with last swapping" : ""));
        }

        private void forAllWaitingStateLocked(Consumer<WaitingState> func) {
            if (func == null) {
                return;
            }
            for (WaitingState ws : this.mWaitingStates.values()) {
                func.accept(ws);
            }
        }

        private void setWMState(int state) {
            if (this.mWMState == state) {
                return;
            }
            Slog.d(TAG, "state from " + stateToString(this.mWMState) + " to " + stateToString(state));
            this.mWMState = state;
        }

        private String getFakePackageNameDisplayPendingLocked() {
            if (this.mFakePackageNameDisplayPending == null) {
                this.mFakePackageNameDisplayPending = "fake_update_display_layout_" + hashCode();
            }
            return this.mFakePackageNameDisplayPending;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String getFakePackageNameWMPendingLocked() {
            if (this.mFakePackageNameWMPending == null) {
                this.mFakePackageNameWMPending = "fake_" + stateToString(3) + "_" + hashCode();
            }
            return this.mFakePackageNameWMPending;
        }

        private String getFakePackageNameUserSwitching() {
            if (this.mFakePackageNameUserSwitching == null) {
                this.mFakePackageNameUserSwitching = "fake_user_switch_ui_" + hashCode();
            }
            return this.mFakePackageNameUserSwitching;
        }

        private String getFakePackageNameAnimation(String info) {
            if (this.mFakePackageNameAnimation == null) {
                this.mFakePackageNameAnimation = "fake_animation_" + hashCode() + "_";
            }
            return this.mFakePackageNameAnimation + info;
        }

        private void getWindowListNoSwapping(WindowContainer<?> container, List<WindowState> whiteList) {
            for (int j = container.mWaitingForDrawn.size() - 1; j >= 0; j--) {
                WindowState w = container.mWaitingForDrawn.get(j);
                if (w.mDisplayContent.mWallpaperController.isWallpaperTarget(w)) {
                    if (this.mPolicy.needWaiting(w)) {
                        whiteList.add(w);
                        return;
                    } else {
                        Slog.d(TAG, "ignore wallpaper target " + w);
                        return;
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static String stateToString(int state) {
            switch (state) {
                case 2:
                    return "STATE_DISPLAY_INFO_UPDATING";
                case 3:
                    return "STATE_WM_PENDING";
                case 4:
                    return "STATE_WM_READY";
                case 5:
                    return "STATE_DONE";
                default:
                    return "STATE_UNKNOWN";
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class WaitingState {
            private final int mDisplayId;
            private final HandlerExecutor mExecutor;
            private String mFakePackageNamePresent;
            private final H mHandler;
            private long mLastSyncFenceId;
            private final String mName;
            private final PendingWindows mPendingWindows;
            private boolean mRunning;
            private SyncFence mSyncFence;
            private long mSyncFenceId;
            private long mWaitingStartTime;
            private final Object mWindowsLock = new Object();
            private final ArraySet<String> mPackages = new ArraySet<>();
            private final ArrayMap<String, WindowInfo> mWindows = new ArrayMap<>();
            private final ArraySet<String> mFinishedWindows = new ArraySet<>();
            private final ArrayMap<Choreographer.FrameCallback, CommittedCallback> mCommittedCallback = new ArrayMap<>();
            private final ArrayList<ActivityRecord> mEmbeddedTemp = new ArrayList<>();

            /* JADX INFO: Access modifiers changed from: private */
            /* loaded from: classes2.dex */
            public final class H extends Handler {
                private static final int MSG_COMMIT_CALLBACK_TIMEOUT = 1;

                public H(Looper looper) {
                    super(looper);
                }

                @Override // android.os.Handler
                public void handleMessage(Message msg) {
                    if (msg.what == 1) {
                        handleTimeoutCallback((Choreographer.FrameCallback) msg.obj);
                    }
                }

                private void handleTimeoutCallback(Choreographer.FrameCallback callback) {
                    CommittedCallback cc;
                    synchronized (WaitingState.this.mWindowsLock) {
                        if (WaitingState.this.isWaitingLocked() && (cc = (CommittedCallback) WaitingState.this.mCommittedCallback.remove(callback)) != null) {
                            long pass = SystemClock.elapsedRealtime() - WaitingState.this.mWaitingStartTime;
                            cc.call(pass);
                            Slog.d(WaitingDrawnWindow.TAG, "callback:" + callback + " is timeout:" + pass + "ms");
                            if (WaitingState.this.mCommittedCallback.isEmpty()) {
                                WaitingState.this.endLocked(false, "hold no callback.");
                            }
                        }
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            /* loaded from: classes2.dex */
            public final class CommittedCallback {
                final Choreographer.FrameCallback mCallback;
                final H mH;
                final long mTimeout;

                public CommittedCallback(Choreographer.FrameCallback callback, Looper looper, long timeout) {
                    this.mCallback = callback;
                    this.mH = looper == null ? null : new H(looper);
                    this.mTimeout = timeout;
                }

                H getH() {
                    H h = this.mH;
                    return h == null ? WaitingState.this.mHandler : h;
                }

                void call(final long time) {
                    getH().post(new Runnable() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$WaitingState$CommittedCallback$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            TranFoldWMCustody.WaitingDrawnWindow.WaitingState.CommittedCallback.this.m8437xd06b066f(time);
                        }
                    });
                }

                /* JADX INFO: Access modifiers changed from: package-private */
                /* renamed from: lambda$call$0$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow$WaitingState$CommittedCallback  reason: not valid java name */
                public /* synthetic */ void m8437xd06b066f(long time) {
                    this.mCallback.doFrame(time);
                }

                void removeTimeout() {
                    getH().removeMessages(1, this.mCallback);
                }

                void sendTimeout() {
                    sendTimeout(this.mTimeout);
                }

                void sendTimeout(long timeout) {
                    if (timeout > 0) {
                        getH().sendMessageDelayed(Message.obtain(getH(), 1, this.mCallback), timeout);
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: private */
            /* loaded from: classes2.dex */
            public class WindowInfo {
                static final int STATE_ADD = 1;
                static final int STATE_CLIENT = 2;
                final long mAddTime = SystemClock.elapsedRealtime();
                final String mName;
                int mState;
                int mSyncId;

                WindowInfo(WindowState w, int state) {
                    this.mName = w.getName();
                    this.mState = state;
                    this.mSyncId = w.mSyncSeqId;
                }

                public String toString() {
                    return "WindowInfo{mState=" + this.mState + ", mName='" + this.mName + "', mSyncId=" + this.mSyncId + '}';
                }
            }

            WaitingState(int displayId, String name, Looper looper) {
                this.mDisplayId = displayId;
                this.mName = name;
                H h = new H(looper == null ? Looper.myLooper() : looper);
                this.mHandler = h;
                this.mExecutor = new HandlerExecutor(h);
                this.mPendingWindows = new PendingWindows(name);
            }

            public boolean addCallback(Choreographer.FrameCallback callback, long timeout, Looper looper, boolean immediately) {
                CommittedCallback cc;
                if (callback == null) {
                    return false;
                }
                synchronized (this.mWindowsLock) {
                    boolean isWaiting = isWaitingLocked();
                    if (immediately && isWaiting) {
                        immediately = false;
                    }
                    cc = addCallbackLocked(callback, timeout, looper, isWaiting, immediately);
                }
                if (immediately) {
                    Slog.d(WaitingDrawnWindow.TAG, this.mName + " invoke callback:" + callback + " immediately timeout:" + timeout);
                    cc.call(0L);
                    return true;
                }
                return true;
            }

            public void preAdd(DisplayContent displayContent, ArrayList<WindowState> windows) {
                if (windows == null || windows.isEmpty()) {
                    return;
                }
                synchronized (this.mWindowsLock) {
                    if (isWaitingLocked()) {
                        if (!WaitingDrawnWindow.this.mSwapping || (this.mWindows.isEmpty() && this.mPendingWindows.isEmpty() && this.mFinishedWindows.isEmpty())) {
                            Iterator<WindowState> it = windows.iterator();
                            while (it.hasNext()) {
                                WindowState w = it.next();
                                this.mPendingWindows.add(displayContent, w, "preAdd");
                            }
                        }
                    }
                }
            }

            public void addPackage(String packageName) {
                addPackage(packageName, null);
            }

            public void addPackage(String packageName, String reason) {
                synchronized (this.mWindowsLock) {
                    if (isWaitingLocked()) {
                        addPackageLocked(packageName, reason);
                    }
                }
            }

            public void removePackage(String packageName) {
                removePackage(packageName, null);
            }

            public void removePackage(String packageName, String reason) {
                synchronized (this.mWindowsLock) {
                    removePackageLocked(packageName, reason);
                }
            }

            public void onStartActivityUnchecked(ActivityRecord ar) {
                synchronized (this.mWindowsLock) {
                    if (isWaitingLocked() && TranFoldWMCustody.isEmbeddedLocked(ar) && ar.getWindowingMode() == 6 && !ar.getDisplayContent().isKeyguardLocked()) {
                        this.mPendingWindows.add(ar, "embedded-start");
                    }
                }
            }

            public void onActivityRecordSetVisible(ActivityRecord ar, boolean visible) {
                synchronized (this.mWindowsLock) {
                    if (isWaitingLocked() && TranFoldWMCustody.isEmbeddedLocked(ar) && !visible && this.mPendingWindows.remove(ar, "invisible")) {
                        Slog.d(WaitingDrawnWindow.TAG, "set " + TranFoldWMCustody.getName(ar) + " invisible.");
                    }
                }
            }

            public void applyWindowOrganizerConfigChanges(WindowContainer<?> container, WindowContainerTransaction.Change change) {
                TaskFragment tf;
                synchronized (this.mWindowsLock) {
                    if (isWaitingLocked() && (tf = container.asTaskFragment()) != null && tf.isEmbedded()) {
                        int oldWindowingMode = container.getWindowingMode();
                        int newWindowingMode = change.getWindowingMode();
                        if (oldWindowingMode != newWindowingMode && newWindowingMode == 6) {
                            ActivityRecord ar = tf.getTopNonFinishingActivity(true);
                            if (TranFoldWMCustody.isEmbeddedLocked(ar)) {
                                WindowState w = ar.findMainWindow();
                                if (TranFoldWMCustody.isEmbeddedLocked(w)) {
                                    pendingEmbeddedLocked(tf, w);
                                }
                            }
                        }
                    }
                }
            }

            public void handleWMReady() {
                String fakePackage = WaitingDrawnWindow.this.getFakePackageNameWMPendingLocked();
                synchronized (this.mWindowsLock) {
                    removePackageLocked(fakePackage);
                }
            }

            public void updateLastReportedConfiguration(WindowState w, Configuration oldMergedConfiguration, Configuration newMergedConfiguration, boolean useLatestConfig, boolean shouldSendRedrawForSync) {
                synchronized (this.mWindowsLock) {
                    try {
                        try {
                            if (isWaitingLocked()) {
                                long oldPhysicalId = WaitingDrawnWindow.getDisplayPhysicalId(oldMergedConfiguration);
                                long newPhysicalId = WaitingDrawnWindow.getDisplayPhysicalId(newMergedConfiguration);
                                Rect oldBounds = WaitingDrawnWindow.getBounds(oldMergedConfiguration);
                                Rect newBounds = WaitingDrawnWindow.getBounds(newMergedConfiguration);
                                boolean boundsChanged = true;
                                boolean physicalDisplayIdChanged = (oldPhysicalId == 0 || oldPhysicalId == newPhysicalId) ? false : true;
                                if (oldBounds.equals(newBounds)) {
                                    boundsChanged = false;
                                }
                                boolean needAdd = false;
                                if (this.mPendingWindows.contains(w)) {
                                    needAdd = this.mPendingWindows.boundsChanged(w);
                                    if (!needAdd) {
                                        return;
                                    }
                                } else if (physicalDisplayIdChanged) {
                                    needAdd = true;
                                }
                                if (needAdd) {
                                    if (!add(w, useLatestConfig, shouldSendRedrawForSync) && boundsChanged) {
                                    }
                                }
                                handleConfigBoundsChangedLocked(w);
                            }
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            }

            public boolean add(WindowState w, boolean needReport, String reason) {
                synchronized (this.mWindowsLock) {
                    if (ignoreLocked(w)) {
                        return false;
                    }
                    WindowInfo info = this.mWindows.get(w.getName());
                    if (info != null || this.mFinishedWindows.contains(w.getName())) {
                        return false;
                    }
                    if (needReport) {
                        w.applyWithNextDraw(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$WaitingState$$ExternalSyntheticLambda1
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                TranFoldWMCustody.WaitingDrawnWindow.WaitingState.lambda$add$0((SurfaceControl.Transaction) obj);
                            }
                        });
                    }
                    addLocked(w, 1, w.mSyncSeqId, reason);
                    return true;
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public static /* synthetic */ void lambda$add$0(SurfaceControl.Transaction t) {
            }

            public void finishDrawing(WindowState w, SurfaceControl.Transaction postDrawTransaction, int syncSeqId) {
                if (postDrawTransaction == null) {
                    return;
                }
                final String name = w.getName();
                synchronized (this.mWindowsLock) {
                    if (!ignoreLocked(w) && !this.mFinishedWindows.contains(name)) {
                        WindowInfo info = this.mWindows.get(name);
                        boolean goOn = false;
                        if (info == null) {
                            if (!this.mPendingWindows.contains(w) || this.mPendingWindows.boundsChanged(w)) {
                                info = addLocked(w, 2, syncSeqId, "finishDrawing");
                                goOn = true;
                            }
                        } else if (info.mState == 1 && info.mSyncId <= syncSeqId) {
                            info.mState = 2;
                            Slog.d(WaitingDrawnWindow.TAG, this.mName + " update commit " + TranFoldWMCustody.getName(w) + " waitSyncId:" + info.mSyncId + " reportSyncId:" + syncSeqId + " size:" + this.mWindows.size() + " pass " + (SystemClock.elapsedRealtime() - info.mAddTime) + "ms");
                            goOn = true;
                        }
                        if (goOn) {
                            final int committedSyncId = info.mSyncId;
                            postDrawTransaction.addTransactionCommittedListener(this.mExecutor, new SurfaceControl.TransactionCommittedListener() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$WaitingState$$ExternalSyntheticLambda2
                                public final void onTransactionCommitted() {
                                    TranFoldWMCustody.WaitingDrawnWindow.WaitingState.this.m8433xa65dfee6(name, committedSyncId);
                                }
                            });
                        }
                    }
                }
            }

            public void start(boolean restart, boolean force, String reason) {
                synchronized (this.mWindowsLock) {
                    if (isWaitingLocked()) {
                        if (restart) {
                            if (force) {
                                endLocked(true, HostingRecord.HOSTING_TYPE_RESTART);
                            }
                            Slog.d(WaitingDrawnWindow.TAG, this.mName + " restart " + this.mDisplayId + " for " + reason);
                        } else {
                            long pass = SystemClock.elapsedRealtime() - this.mWaitingStartTime;
                            if (pass > 100) {
                                Slog.w(WaitingDrawnWindow.TAG, this.mName + " start blocking screen from transition finished pass " + pass + "ms.");
                            }
                            return;
                        }
                    } else {
                        Slog.d(WaitingDrawnWindow.TAG, this.mName + " start " + this.mDisplayId + " for " + reason);
                    }
                    this.mWaitingStartTime = SystemClock.elapsedRealtime();
                    this.mRunning = true;
                    for (CommittedCallback cc : this.mCommittedCallback.values()) {
                        cc.removeTimeout();
                        cc.sendTimeout();
                    }
                }
            }

            public boolean end(boolean force, String reason) {
                boolean endLocked;
                synchronized (this.mWindowsLock) {
                    endLocked = endLocked(force, reason);
                }
                return endLocked;
            }

            public void removeImmediately(WindowState w) {
                synchronized (this.mWindowsLock) {
                    if (isWaitingLocked()) {
                        boolean pending = false;
                        ActivityRecord ar = w.getActivityRecord();
                        if (ar != null && ar.isRelaunching() && (this.mWindows.isEmpty() || !ar.forAllWindows(new ToBooleanFunction() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$WaitingState$$ExternalSyntheticLambda3
                            public final boolean apply(Object obj) {
                                return TranFoldWMCustody.WaitingDrawnWindow.WaitingState.this.m8434x98c67b76((WindowState) obj);
                            }
                        }, true))) {
                            pending = true;
                        }
                        removeWaitingDrawnLocked(w.getName(), null, "removeImmediately", false);
                        if (pending) {
                            this.mFinishedWindows.remove(w.getName());
                            this.mPendingWindows.add(ar, "Relaunching");
                        } else {
                            removePackageLocked(TranFoldWMCustody.getPackageName(w), "removeImmediately");
                            this.mPendingWindows.remove(w, "removeImmediately");
                            checkPresentLocked();
                        }
                    }
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$removeImmediately$2$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow$WaitingState  reason: not valid java name */
            public /* synthetic */ boolean m8434x98c67b76(WindowState cw) {
                return this.mWindows.containsKey(cw.getName());
            }

            /* JADX DEBUG: Method merged with bridge method */
            /* JADX DEBUG: Multi-variable search result rejected for r1v3, resolved type: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$WaitingState$WindowInfo */
            /* JADX WARN: Multi-variable type inference failed */
            /* renamed from: windowCommitted */
            public void m8433xa65dfee6(String winName, int syncId) {
                synchronized (this.mWindowsLock) {
                    WindowInfo info = this.mWindows.get(winName);
                    if (info != 0 && info.mState == 2 && info.mSyncId == syncId) {
                        removeWaitingDrawnLocked(winName, null, "committed");
                    } else {
                        Slog.d(WaitingDrawnWindow.TAG, "ignore " + ((Object) (info == 0 ? winName : info)) + " committed with syncID " + syncId);
                    }
                }
            }

            public void removeWaitingDrawn(String winName, SyncFence presentFence, String reason) {
                synchronized (this.mWindowsLock) {
                    removeWaitingDrawnLocked(winName, presentFence, reason);
                }
            }

            private void removeWaitingDrawnLocked(String winName, SyncFence presentFence, String reason, boolean finish) {
                WindowInfo info = this.mWindows.remove(winName);
                if (info != null) {
                    Slog.d(WaitingDrawnWindow.TAG, this.mName + " remove Window " + winName + " for " + reason + " size:" + this.mWindows.size() + " pass " + (SystemClock.elapsedRealtime() - info.mAddTime) + "ms");
                    this.mFinishedWindows.add(winName);
                    this.mSyncFence = presentFence;
                    if (finish) {
                        checkPresentLocked();
                    }
                }
            }

            private void removeWaitingDrawnLocked(String winName, SyncFence presentFence, String reason) {
                removeWaitingDrawnLocked(winName, presentFence, reason, true);
            }

            private boolean ignoreLocked(WindowState w) {
                return (isWaitingLocked() && this.mSyncFenceId == this.mLastSyncFenceId && WaitingDrawnWindow.this.mPolicy.needWaiting(w)) ? false : true;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public boolean isWaitingLocked() {
                return this.mRunning;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public boolean endLocked(boolean force, String reason) {
                if (isWaitingLocked()) {
                    if (force || noPendingLocked()) {
                        clearLocked(force, reason);
                        return true;
                    }
                    return false;
                }
                return true;
            }

            private boolean noPendingLocked() {
                return this.mWindows.isEmpty() && this.mPackages.isEmpty() && this.mPendingWindows.isEmpty();
            }

            private void clearLocked(boolean force, String reason) {
                if (!this.mPackages.isEmpty() || !this.mWindows.isEmpty() || !this.mPendingWindows.isEmpty()) {
                    Slog.d(WaitingDrawnWindow.TAG, this.mName + " left packages:" + Arrays.toString(this.mPackages.toArray()) + " left windows:" + Arrays.toString(this.mWindows.values().toArray()) + " left " + this.mPendingWindows.toString() + " mSwapping:" + WaitingDrawnWindow.this.mSwapping + " mUpdatingLayout:" + WaitingDrawnWindow.this.mUpdatingLayout + " state:" + WaitingDrawnWindow.stateToString(WaitingDrawnWindow.this.mWMState));
                }
                this.mPackages.clear();
                this.mWindows.clear();
                this.mPendingWindows.clear();
                this.mFinishedWindows.clear();
                SyncFence syncFence = this.mSyncFence;
                if (syncFence != null) {
                    syncFence.close();
                    this.mSyncFence = null;
                }
                this.mLastSyncFenceId = this.mSyncFenceId;
                this.mHandler.removeMessages(1);
                clearCallbackLocked(force);
                this.mRunning = false;
                Slog.d(WaitingDrawnWindow.TAG, this.mName + " end for " + reason + (force ? " by force." : ".") + " pass " + (SystemClock.elapsedRealtime() - this.mWaitingStartTime) + "ms");
                this.mHandler.post(new Runnable() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$WaitingState$$ExternalSyntheticLambda7
                    @Override // java.lang.Runnable
                    public final void run() {
                        TranFoldWMCustody.WaitingDrawnWindow.WaitingState.this.m8432xb7119654();
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$clearLocked$3$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow$WaitingState  reason: not valid java name */
            public /* synthetic */ void m8432xb7119654() {
                synchronized (WaitingDrawnWindow.this.mWaitingStatesLock) {
                    WaitingDrawnWindow.this.checkAllDrawnDoneLocked();
                }
            }

            private CommittedCallback addCallbackLocked(Choreographer.FrameCallback callback, long timeout, Looper looper, boolean isWaiting, boolean immediately) {
                CommittedCallback oldCallback = this.mCommittedCallback.get(callback);
                if (immediately) {
                    if (oldCallback != null) {
                        oldCallback.removeTimeout();
                        this.mCommittedCallback.remove(callback);
                        Slog.d(WaitingDrawnWindow.TAG, this.mName + " remove old callback:" + callback);
                        return oldCallback;
                    }
                    return new CommittedCallback(callback, looper, timeout);
                }
                return addCallbackLocked(callback, timeout, looper, isWaiting, oldCallback);
            }

            private WindowInfo addLocked(WindowState w, int state, int syncId, String reason) {
                WindowInfo info = new WindowInfo(w, state);
                info.mSyncId = syncId;
                this.mWindows.put(w.getName(), info);
                this.mPendingWindows.remove(w, "add");
                Slog.d(WaitingDrawnWindow.TAG, this.mName + " add " + TranFoldWMCustody.getName(w) + " syncId:" + syncId + " for " + reason + " size:" + this.mWindows.size() + " pass " + (SystemClock.elapsedRealtime() - this.mWaitingStartTime) + "ms");
                removePackageLocked(TranFoldWMCustody.getPackageName(w), "window added");
                return info;
            }

            private void reAddLocked(WindowState w, String reason) {
                String name = w.getName();
                if (this.mFinishedWindows.remove(name)) {
                    reason = reason + " re add already committed";
                } else {
                    WindowInfo info = this.mWindows.remove(name);
                    if (info != null) {
                        Slog.d(WaitingDrawnWindow.TAG, "remove " + info + " for " + reason);
                        reason = reason + " re add drawing";
                    }
                }
                add(w, true, reason);
            }

            private boolean add(WindowState w, boolean useLatestConfig, boolean shouldSendRedrawForSync) {
                if (useLatestConfig) {
                    return add(w, !shouldSendRedrawForSync, "resized");
                }
                return add(w, w.mSyncSeqId <= w.mLastSeqIdSentToRelayout, "relayout");
            }

            private void handleConfigBoundsChangedLocked(WindowState w) {
                ActivityRecord ar = w.getActivityRecord();
                if (ar == null || ar.finishing) {
                    return;
                }
                reAddLocked(w, "bounds of config changed.");
            }

            private void clearCallbackLocked(boolean force) {
                if (this.mCommittedCallback.isEmpty()) {
                    return;
                }
                final ArrayList<CommittedCallback> callback = new ArrayList<>(this.mCommittedCallback.size());
                final long startTime = this.mWaitingStartTime;
                for (final CommittedCallback cc : this.mCommittedCallback.values()) {
                    Slog.d(WaitingDrawnWindow.TAG, this.mName + (force ? " force " : "") + " invoke callback:" + cc.mCallback);
                    cc.removeTimeout();
                    if (cc.mH != null) {
                        cc.mH.post(new Runnable() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$WaitingState$$ExternalSyntheticLambda5
                            @Override // java.lang.Runnable
                            public final void run() {
                                TranFoldWMCustody.WaitingDrawnWindow.WaitingState.CommittedCallback.this.mCallback.doFrame(SystemClock.elapsedRealtime() - startTime);
                            }
                        });
                    } else {
                        callback.add(cc);
                    }
                }
                this.mCommittedCallback.clear();
                this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$WaitingState$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        TranFoldWMCustody.WaitingDrawnWindow.WaitingState.lambda$clearCallbackLocked$5(startTime, callback);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            public static /* synthetic */ void lambda$clearCallbackLocked$5(long startTime, ArrayList callback) {
                long passTime = SystemClock.elapsedRealtime() - startTime;
                Iterator it = callback.iterator();
                while (it.hasNext()) {
                    CommittedCallback cc = (CommittedCallback) it.next();
                    cc.mCallback.doFrame(passTime);
                }
            }

            private CommittedCallback addCallbackLocked(Choreographer.FrameCallback callback, long timeout, Looper looper, boolean isWaiting, CommittedCallback oldCallback) {
                if (oldCallback != null) {
                    if (looper == (oldCallback.mH == null ? null : oldCallback.mH.getLooper()) && timeout == oldCallback.mTimeout) {
                        return oldCallback;
                    }
                }
                CommittedCallback newCallback = new CommittedCallback(callback, looper, timeout);
                this.mCommittedCallback.put(callback, newCallback);
                Slog.d(WaitingDrawnWindow.TAG, this.mName + " add callback:" + callback + " timeout:" + timeout);
                if (oldCallback != null) {
                    oldCallback.removeTimeout();
                }
                newCallback.sendTimeout();
                return newCallback;
            }

            private boolean addPackageLocked(String packageName) {
                return addPackageLocked(packageName, null);
            }

            private boolean addPackageLocked(String packageName, String reason) {
                if (this.mSyncFenceId == this.mLastSyncFenceId && this.mPackages.add(packageName)) {
                    Slog.d(WaitingDrawnWindow.TAG, this.mName + " add package:" + packageName + (TextUtils.isEmpty(reason) ? "" : " for " + reason));
                    return true;
                }
                return false;
            }

            private boolean removePackageLocked(String packageName) {
                return removePackageLocked(packageName, null);
            }

            private boolean removePackageLocked(String packageName, String reason) {
                if (this.mPackages.remove(packageName)) {
                    Slog.d(WaitingDrawnWindow.TAG, this.mName + " remove package:" + packageName + (!TextUtils.isEmpty(reason) ? " for " + reason : ""));
                    checkPresentLocked();
                    return true;
                }
                return false;
            }

            private void checkPresentLocked() {
                if (WaitingDrawnWindow.this.mWMState > 3 && noPendingLocked() && this.mSyncFenceId == this.mLastSyncFenceId) {
                    if (WaitingDrawnWindow.this.mPolicy.mWaitPresent && WaitingDrawnWindow.this.mSwapping && !this.mFinishedWindows.isEmpty()) {
                        waitForPresentLocked();
                    } else {
                        endLocked(false, "no present");
                    }
                }
            }

            private void waitForPresentLocked() {
                final String nextFrame = getFakePackageNamePresentLocked();
                if (addPackageLocked(nextFrame)) {
                    Slog.d(WaitingDrawnWindow.TAG, this.mName + " waiting for present.");
                    this.mSyncFenceId++;
                    this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$WaitingState$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            TranFoldWMCustody.WaitingDrawnWindow.WaitingState.this.m8436x6df9f0ac(nextFrame);
                        }
                    });
                }
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$waitForPresentLocked$7$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow$WaitingState  reason: not valid java name */
            public /* synthetic */ void m8436x6df9f0ac(final String nextFrame) {
                WaitingDrawnWindow.postSFFrameCallback(new Choreographer.FrameCallback() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$WaitingState$$ExternalSyntheticLambda4
                    @Override // android.view.Choreographer.FrameCallback
                    public final void doFrame(long j) {
                        TranFoldWMCustody.WaitingDrawnWindow.WaitingState.this.m8435xa6ee09ab(nextFrame, j);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$waitForPresentLocked$6$com-android-server-wm-TranFoldWMCustody$WaitingDrawnWindow$WaitingState  reason: not valid java name */
            public /* synthetic */ void m8435xa6ee09ab(String nextFrame, long t) {
                synchronized (this.mWindowsLock) {
                    if (removePackageLocked(nextFrame) && !endLocked(false, "waiting for present")) {
                        Slog.d(WaitingDrawnWindow.TAG, this.mName + " need re waiting for present");
                        this.mLastSyncFenceId = this.mSyncFenceId;
                    }
                }
            }

            private String getFakePackageNamePresentLocked() {
                if (this.mFakePackageNamePresent == null) {
                    this.mFakePackageNamePresent = "fake_present_" + hashCode();
                }
                return this.mFakePackageNamePresent;
            }

            private void pendingEmbeddedLocked(TaskFragment tf, WindowState w) {
                String wName = w.getName();
                if (this.mWindows.containsKey(wName) || this.mFinishedWindows.contains(wName)) {
                    Slog.d(WaitingDrawnWindow.TAG, this.mName + " skip embedded " + TranFoldWMCustody.getName(w) + " which already in processing.");
                } else if (w.getDisplayContent().isKeyguardLocked()) {
                    Slog.d(WaitingDrawnWindow.TAG, this.mName + " skip embedded " + TranFoldWMCustody.getName(w) + " when keyguard locked.");
                } else {
                    if (!this.mPendingWindows.isEmpty()) {
                        WaitingDrawnWindow.getOtherEmbedded(tf, this.mEmbeddedTemp);
                        if (!this.mEmbeddedTemp.isEmpty()) {
                            boolean hasVisible = false;
                            Iterator<ActivityRecord> it = this.mEmbeddedTemp.iterator();
                            while (it.hasNext()) {
                                ActivityRecord ar = it.next();
                                if (ar.isVisible()) {
                                    hasVisible = true;
                                }
                            }
                            if (!hasVisible) {
                                Iterator<ActivityRecord> it2 = this.mEmbeddedTemp.iterator();
                                while (it2.hasNext()) {
                                    ActivityRecord ar2 = it2.next();
                                    this.mPendingWindows.remove(ar2, "embedded-invisible");
                                }
                                Slog.d(WaitingDrawnWindow.TAG, this.mName + " skip embedded " + TranFoldWMCustody.getName(w) + " for none visible embedded.");
                                return;
                            }
                        }
                    }
                    this.mPendingWindows.add(w.getDisplayContent(), w, "embedded-organizer");
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static Rect getBounds(Configuration configuration) {
            return configuration.windowConfiguration.getBounds();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static long getDisplayPhysicalId(Configuration configuration) {
            return configuration.windowConfiguration.getDisplayPhysicalId();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static void postSFFrameCallback(Choreographer.FrameCallback callback) {
            Choreographer.getSfInstance().postFrameCallback(callback);
        }

        private static boolean isUserSwitchAnim(int exitAnim, int enterAnim, int overrideOriginalRotation) {
            return exitAnim == 17432743 && enterAnim == 17432742 && overrideOriginalRotation == -1;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static void getOtherEmbedded(final TaskFragment tf, final ArrayList<ActivityRecord> embedded) {
            final Task task;
            embedded.clear();
            if (tf != null && (task = tf.getTask()) != null) {
                task.forAllTaskFragments(new Consumer() { // from class: com.android.server.wm.TranFoldWMCustody$WaitingDrawnWindow$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        TranFoldWMCustody.WaitingDrawnWindow.lambda$getOtherEmbedded$23(TaskFragment.this, task, embedded, (TaskFragment) obj);
                    }
                }, true);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getOtherEmbedded$23(TaskFragment tf, Task task, ArrayList embedded, TaskFragment taskFragment) {
            ActivityRecord ar;
            if (taskFragment != tf && taskFragment != task && TranFoldWMCustody.isMultiWindowingMode(taskFragment) && taskFragment.isEmbedded() && (ar = taskFragment.getTopNonFinishingActivity(true)) != null) {
                embedded.add(ar);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class PendingWindows {
        private final String mName;
        private final ArrayMap<CharSequence, PendingInfo> mPendingInfo = new ArrayMap<>();
        private final ArraySet<ComponentName> mPendingActivities = new ArraySet<>();

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public static class PendingInfo {
            final Rect mLastBounds;
            final String mPackage;
            final CharSequence mTag;

            PendingInfo(WindowState w) {
                this.mTag = w.getWindowTag();
                this.mPackage = TranFoldWMCustody.getPackageName(w);
                this.mLastBounds = new Rect(w.getBounds());
            }

            public String toString() {
                return ((Object) this.mTag) + SliceClientPermissions.SliceAuthority.DELIMITER + this.mPackage + this.mLastBounds.toShortString();
            }
        }

        public PendingWindows(String name) {
            this.mName = name;
        }

        synchronized boolean add(DisplayContent dc, WindowState w, String reason) {
            if (boundsWillChange(w, dc.getDefaultTaskDisplayArea().getBounds())) {
                PendingInfo info = new PendingInfo(w);
                this.mPendingInfo.put(w.getWindowTag(), info);
                Slog.d(TranFoldWMCustody.TAG, this.mName + " add pending " + info + " for " + reason);
                return true;
            }
            return false;
        }

        synchronized boolean add(ActivityRecord ar, String reason) {
            if (this.mPendingActivities.add(ar.mActivityComponent)) {
                Slog.d(TranFoldWMCustody.TAG, this.mName + " add pending " + TranFoldWMCustody.getName(ar) + " for " + reason);
                return true;
            }
            return false;
        }

        synchronized boolean contains(ActivityRecord ar) {
            WindowState w = ar.findMainWindow();
            if (w != null) {
                return contains(w);
            }
            return false;
        }

        /* JADX WARN: Code restructure failed: missing block: B:11:0x001e, code lost:
            if (r4.mPendingActivities.contains(r0.mActivityComponent) != false) goto L13;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        synchronized boolean contains(WindowState w) {
            boolean z = true;
            if (this.mPendingInfo.containsKey(w.getWindowTag())) {
                return true;
            }
            ActivityRecord ar = w.getActivityRecord();
            if (ar != null) {
            }
            z = false;
            return z;
        }

        synchronized boolean boundsChanged(WindowState w) {
            PendingInfo info = this.mPendingInfo.get(w.getWindowTag());
            boolean z = true;
            if (info != null) {
                return true ^ w.getBounds().equals(info.mLastBounds);
            }
            ActivityRecord ar = w.getActivityRecord();
            return (ar == null || !this.mPendingActivities.contains(ar.mActivityComponent)) ? false : false;
        }

        synchronized void clear() {
            this.mPendingInfo.clear();
            this.mPendingActivities.clear();
        }

        synchronized boolean remove(WindowState w, String reason) {
            boolean res;
            res = false;
            PendingInfo info = this.mPendingInfo.remove(w.getWindowTag());
            if (info != null) {
                Slog.d(TranFoldWMCustody.TAG, this.mName + " remove pending " + info + " for " + reason);
                res = true;
            }
            if (doRemove(w.getActivityRecord(), reason)) {
                res = true;
            }
            return res;
        }

        synchronized boolean remove(ActivityRecord ar, String reason) {
            WindowState w = ar.findMainWindow();
            if (w != null) {
                return remove(w, reason);
            }
            return doRemove(ar, reason);
        }

        synchronized boolean isEmpty() {
            boolean z;
            if (this.mPendingInfo.isEmpty()) {
                z = this.mPendingActivities.isEmpty();
            }
            return z;
        }

        public String toString() {
            return getClass().getSimpleName() + ":" + Arrays.toString(this.mPendingInfo.values().toArray()) + " " + Arrays.toString(this.mPendingActivities.toArray());
        }

        private boolean boundsWillChange(WindowState w, Rect fullBounds) {
            ActivityRecord ar = w.getActivityRecord();
            return (ar == null || ar.finishing || (!TranFoldWMCustody.isMultiWindowingMode(ar) && !ar.getBounds().equals(fullBounds))) ? false : true;
        }

        private boolean doRemove(ActivityRecord ar, String reason) {
            if (ar != null && this.mPendingActivities.remove(ar.mActivityComponent)) {
                Slog.d(TranFoldWMCustody.TAG, this.mName + " remove pending " + ar.mActivityComponent + " for " + reason);
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class RedrawWindow {
        private final ArrayMap<String, ArrayList<Runnable>> mCallbacks;
        private String mResizingWin;
        private int mSyncId;

        private RedrawWindow() {
            this.mCallbacks = new ArrayMap<>();
        }

        public boolean redraw(WindowState w, Runnable callback) {
            String name;
            int size;
            if (w == null || w.mWinAnimator == null || (size = add((name = w.getName()), callback)) == 0) {
                return false;
            }
            if (size > 1) {
                return true;
            }
            if (!prepareResized(w)) {
                remove(name, "failed to prepare");
                return false;
            }
            w.reportResized();
            return handleResized(name);
        }

        void removeImmediately(WindowState w) {
            remove(w.getName(), "removeImmediately");
        }

        void finishDrawing(WindowState win) {
            remove(win.getName(), "finishDrawing");
        }

        boolean prepareResized(WindowState w) {
            if (w.mWinAnimator == null) {
                return false;
            }
            w.mWinAnimator.mDrawState = 1;
            this.mResizingWin = null;
            this.mSyncId = w.mSyncSeqId;
            return true;
        }

        void resized(WindowState win, boolean reportDraw, int syncSeqId) {
            if (reportDraw) {
                if (syncSeqId == Integer.MAX_VALUE || syncSeqId == this.mSyncId) {
                    this.mResizingWin = win.getName();
                    this.mSyncId = syncSeqId;
                }
            }
        }

        boolean handleResized(String name) {
            if (!name.equals(this.mResizingWin)) {
                remove(name, "reportResized failed");
                return false;
            }
            return true;
        }

        private int add(String name, Runnable callback) {
            int size;
            if (TextUtils.isEmpty(name)) {
                return 0;
            }
            synchronized (this.mCallbacks) {
                ArrayList<Runnable> callbacks = this.mCallbacks.computeIfAbsent(name, new Function() { // from class: com.android.server.wm.TranFoldWMCustody$RedrawWindow$$ExternalSyntheticLambda0
                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return TranFoldWMCustody.RedrawWindow.lambda$add$0((String) obj);
                    }
                });
                boolean added = callbacks.add(callback);
                size = added ? callbacks.size() : 0;
            }
            return size;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ ArrayList lambda$add$0(String k) {
            return new ArrayList();
        }

        private void remove(String name, String reason) {
            ArrayList<Runnable> callbacks;
            synchronized (this.mCallbacks) {
                callbacks = this.mCallbacks.remove(name);
            }
            if (callbacks == null) {
                return;
            }
            Iterator<Runnable> it = callbacks.iterator();
            while (it.hasNext()) {
                Runnable callback = it.next();
                if (callback != null) {
                    callback.run();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class AnimationCustody {
        static final int MASK_TRANSITION_ANIMATION_DISABLED = 2;
        static final int MASK_VIEW_ANIMATION_DISABLED = 4;
        static final int MASK_WINDOW_ANIMATION_DISABLED = 1;
        private int mFlags;

        private AnimationCustody() {
        }

        public synchronized boolean disableWindowAnimations() {
            if (TranFoldDisplayCustody.disableDisableAllAnimations()) {
                return false;
            }
            return (this.mFlags & 1) != 0;
        }

        public synchronized boolean disableTransitionAnimations() {
            if (TranFoldDisplayCustody.disableDisableAllAnimations()) {
                return false;
            }
            return (this.mFlags & 2) != 0;
        }

        public synchronized boolean disableViewAnimations() {
            if (TranFoldDisplayCustody.disableDisableAllAnimations()) {
                return false;
            }
            return (this.mFlags & 4) != 0;
        }

        public void disableAnimations(boolean disable, WindowManagerService service) {
            if (TranFoldDisplayCustody.disableDisableAllAnimations()) {
                return;
            }
            if (service == null) {
                service = TranFoldWMCustody.getWindowManagerService();
            }
            if (service == null) {
                return;
            }
            disableAnimations(0, service);
        }

        private synchronized void disableAnimations(int flags, WindowManagerService service) {
            if (this.mFlags == flags) {
                return;
            }
            Slog.d(TranFoldWMCustody.TAG, "disableAnimations from " + Integer.toHexString(this.mFlags) + " to " + Integer.toHexString(flags));
            if ((this.mFlags & 4) != (flags & 4)) {
                service.mH.obtainMessage(34, null).sendToTarget();
            }
            this.mFlags = flags;
        }
    }

    private static void setDisplayPhysicalId(Configuration configuration, long physicalDisplayId) {
        configuration.windowConfiguration.setDisplayPhysicalId(physicalDisplayId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getName(WindowContainer<?> container) {
        return container + "(" + container.getBounds().toShortString() + ")";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isEmbeddedLocked(WindowState w) {
        return w != null && isEmbeddedLocked(w.getActivityRecord());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isEmbeddedLocked(ActivityRecord ar) {
        TaskFragment tf;
        return ar != null && (tf = ar.getTaskFragment()) != null && tf.isOrganized() && tf.isEmbedded();
    }

    private static int getWindowingMode(Configuration configuration) {
        return configuration.windowConfiguration.getWindowingMode();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isMultiWindowingMode(WindowContainer<?> container) {
        return container != null && container.getWindowingMode() == 6;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getPackageName(WindowState w) {
        return w.mAttrs.packageName;
    }
}
