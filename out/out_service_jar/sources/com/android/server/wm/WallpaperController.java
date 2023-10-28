package com.android.server.wm;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.Animation;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.ToBooleanFunction;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WallpaperController {
    public static boolean OS_FOLEABLE_SCREEN_SUPPORT = false;
    private static final String TAG = "WindowManager";
    private static final int WALLPAPER_DRAW_NORMAL = 0;
    private static final int WALLPAPER_DRAW_PENDING = 1;
    private static final long WALLPAPER_DRAW_PENDING_TIMEOUT_DURATION = 500;
    private static final int WALLPAPER_DRAW_TIMEOUT = 2;
    private static final long WALLPAPER_TIMEOUT = 150;
    private static final long WALLPAPER_TIMEOUT_RECOVERY = 10000;
    private final DisplayContent mDisplayContent;
    private long mLastWallpaperTimeoutTime;
    private final float mMaxWallpaperScale;
    private WindowManagerService mService;
    private boolean mShouldUpdateZoom;
    private WindowState mTmpTopWallpaper;
    private WindowState mWaitingOnWallpaper;
    private final ArrayList<WallpaperWindowToken> mWallpaperTokens = new ArrayList<>();
    private WindowState mWallpaperTarget = null;
    private WindowState mPrevWallpaperTarget = null;
    private float mLastWallpaperX = -1.0f;
    private float mLastWallpaperY = -1.0f;
    private float mLastWallpaperXStep = -1.0f;
    private float mLastWallpaperYStep = -1.0f;
    private float mLastWallpaperZoomOut = 0.0f;
    private int mLastWallpaperDisplayOffsetX = Integer.MIN_VALUE;
    private int mLastWallpaperDisplayOffsetY = Integer.MIN_VALUE;
    private boolean mLastFrozen = false;
    private int mWallpaperDrawState = 0;
    private final FindWallpaperTargetResult mFindResults = new FindWallpaperTargetResult();
    private final ToBooleanFunction<WindowState> mFindWallpaperTargetFunction = new ToBooleanFunction() { // from class: com.android.server.wm.WallpaperController$$ExternalSyntheticLambda0
        public final boolean apply(Object obj) {
            return WallpaperController.this.m8458lambda$new$0$comandroidserverwmWallpaperController((WindowState) obj);
        }
    };
    private Consumer<WindowState> mComputeMaxZoomOutFunction = new Consumer() { // from class: com.android.server.wm.WallpaperController$$ExternalSyntheticLambda1
        @Override // java.util.function.Consumer
        public final void accept(Object obj) {
            WallpaperController.this.m8459lambda$new$1$comandroidserverwmWallpaperController((WindowState) obj);
        }
    };

    static {
        OS_FOLEABLE_SCREEN_SUPPORT = 1 == SystemProperties.getInt("ro.os_foldable_screen_support", 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-WallpaperController  reason: not valid java name */
    public /* synthetic */ boolean m8458lambda$new$0$comandroidserverwmWallpaperController(WindowState w) {
        if (w.mAttrs.type == 2013) {
            if (this.mFindResults.topWallpaper == null || this.mFindResults.resetTopWallpaper) {
                this.mFindResults.setTopWallpaper(w);
                this.mFindResults.resetTopWallpaper = false;
            }
            return false;
        }
        this.mFindResults.resetTopWallpaper = true;
        if (!w.mTransitionController.isShellTransitionsEnabled()) {
            if (w.mActivityRecord != null && !w.mActivityRecord.isVisible() && !w.mActivityRecord.isAnimating(3)) {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v("WindowManager", "Skipping hidden and not animating token: " + w);
                }
                return false;
            }
        } else {
            ActivityRecord ar = w.mActivityRecord;
            TransitionController tc = w.mTransitionController;
            if (ar != null && !ar.isVisibleRequested() && !tc.inTransition(ar)) {
                return false;
            }
        }
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v("WindowManager", "Win " + w + ": isOnScreen=" + w.isOnScreen() + " mDrawState=" + w.mWinAnimator.mDrawState);
        }
        if (w.mWillReplaceWindow && this.mWallpaperTarget == null && !this.mFindResults.useTopWallpaperAsTarget) {
            this.mFindResults.setUseTopWallpaperAsTarget(true);
        }
        WindowContainer animatingContainer = w.mActivityRecord != null ? w.mActivityRecord.getAnimatingContainer() : null;
        boolean keyguardGoingAwayWithWallpaper = animatingContainer != null && animatingContainer.isAnimating(3) && AppTransition.isKeyguardGoingAwayTransitOld(animatingContainer.mTransit) && (animatingContainer.mTransitFlags & 4) != 0;
        boolean needsShowWhenLockedWallpaper = false;
        if ((w.mAttrs.flags & 524288) != 0 && this.mService.mPolicy.isKeyguardLocked()) {
            TransitionController tc2 = w.mTransitionController;
            boolean isInTransition = tc2.isShellTransitionsEnabled() && tc2.inTransition(w);
            if (this.mService.mPolicy.isKeyguardOccluded() || this.mService.mPolicy.isKeyguardUnoccluding() || isInTransition) {
                needsShowWhenLockedWallpaper = (isFullscreen(w.mAttrs) && (w.mActivityRecord == null || w.mActivityRecord.fillsParent())) ? false : true;
            }
        }
        if (keyguardGoingAwayWithWallpaper || needsShowWhenLockedWallpaper) {
            this.mFindResults.setUseTopWallpaperAsTarget(true);
        }
        RecentsAnimationController recentsAnimationController = this.mService.getRecentsAnimationController();
        boolean animationWallpaper = (animatingContainer == null || animatingContainer.getAnimation() == null || !animatingContainer.getAnimation().getShowWallpaper()) ? false : true;
        boolean hasWallpaper = w.hasWallpaper() || animationWallpaper;
        boolean isRecentsTransitionTarget = recentsAnimationController != null && recentsAnimationController.isWallpaperVisible(w);
        if (isRecentsTransitionTarget) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v("WindowManager", "Found recents animation wallpaper target: " + w);
            }
            this.mFindResults.setWallpaperTarget(w);
            return true;
        } else if (hasWallpaper && w.isOnScreen() && (this.mWallpaperTarget == w || w.isDrawFinishedLw())) {
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v("WindowManager", "Found wallpaper target: " + w);
            }
            this.mFindResults.setWallpaperTarget(w);
            if (w == this.mWallpaperTarget && w.isAnimating(3) && WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v("WindowManager", "Win " + w + ": token animating, looking behind.");
            }
            this.mFindResults.setIsWallpaperTargetForLetterbox(w.hasWallpaperForLetterboxBackground());
            return w.mActivityRecord != null;
        } else {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-wm-WallpaperController  reason: not valid java name */
    public /* synthetic */ void m8459lambda$new$1$comandroidserverwmWallpaperController(WindowState windowState) {
        if (!windowState.mIsWallpaper && Float.compare(windowState.mWallpaperZoomOut, this.mLastWallpaperZoomOut) > 0) {
            this.mLastWallpaperZoomOut = windowState.mWallpaperZoomOut;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WallpaperController(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        this.mDisplayContent = displayContent;
        this.mMaxWallpaperScale = service.mContext.getResources().getFloat(17105121);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getWallpaperTarget() {
        return this.mWallpaperTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isWallpaperTarget(WindowState win) {
        return win == this.mWallpaperTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBelowWallpaperTarget(WindowState win) {
        WindowState windowState = this.mWallpaperTarget;
        return windowState != null && windowState.mLayer >= win.mBaseLayer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isWallpaperVisible() {
        for (int i = this.mWallpaperTokens.size() - 1; i >= 0; i--) {
            if (this.mWallpaperTokens.get(i).isVisible()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startWallpaperAnimation(Animation a) {
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            WallpaperWindowToken token = this.mWallpaperTokens.get(curTokenNdx);
            token.startAnimation(a);
        }
    }

    private boolean shouldWallpaperBeVisible(WindowState wallpaperTarget) {
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v("WindowManager", "Wallpaper vis: target " + wallpaperTarget + " prev=" + this.mPrevWallpaperTarget);
        }
        return (wallpaperTarget == null && this.mPrevWallpaperTarget == null) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isWallpaperTargetAnimating() {
        WindowState windowState = this.mWallpaperTarget;
        return windowState != null && windowState.isAnimating(3) && (this.mWallpaperTarget.mActivityRecord == null || !this.mWallpaperTarget.mActivityRecord.isWaitingForTransitionStart());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateWallpaperVisibility() {
        boolean visible = shouldWallpaperBeVisible(this.mWallpaperTarget);
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            WallpaperWindowToken token = this.mWallpaperTokens.get(curTokenNdx);
            token.setVisibility(visible);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideDeferredWallpapersIfNeededLegacy() {
        for (int i = this.mWallpaperTokens.size() - 1; i >= 0; i--) {
            WallpaperWindowToken token = this.mWallpaperTokens.get(i);
            if (!token.isVisibleRequested()) {
                token.commitVisibility(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideWallpapers(WindowState winGoingAway) {
        WindowState windowState = this.mWallpaperTarget;
        if ((windowState != null && (windowState != winGoingAway || this.mPrevWallpaperTarget != null)) || this.mFindResults.useTopWallpaperAsTarget) {
            return;
        }
        for (int i = this.mWallpaperTokens.size() - 1; i >= 0; i--) {
            WallpaperWindowToken token = this.mWallpaperTokens.get(i);
            token.setVisibility(false);
            if (ProtoLogImpl.isEnabled(ProtoLogGroup.WM_DEBUG_WALLPAPER) && token.isVisible() && ProtoLogCache.WM_DEBUG_WALLPAPER_enabled) {
                String protoLogParam0 = String.valueOf(token);
                String protoLogParam1 = String.valueOf(winGoingAway);
                String protoLogParam2 = String.valueOf(this.mWallpaperTarget);
                String protoLogParam3 = String.valueOf(this.mPrevWallpaperTarget);
                String protoLogParam4 = String.valueOf(Debug.getCallers(5));
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_WALLPAPER, 1984843251, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2, protoLogParam3, protoLogParam4});
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateWallpaperOffset(WindowState wallpaperWin, boolean sync) {
        float wpy;
        int offset;
        int offset2;
        boolean rawChanged;
        Rect bounds = OS_FOLEABLE_SCREEN_SUPPORT ? wallpaperWin.getParentFrame() : wallpaperWin.getLastReportedBounds();
        int dw = bounds.width();
        int dh = bounds.height();
        boolean rawChanged2 = false;
        float defaultWallpaperX = wallpaperWin.isRtl() ? 1.0f : 0.0f;
        float f = this.mLastWallpaperX;
        if (f < 0.0f) {
            f = defaultWallpaperX;
        }
        float wpx = f;
        float f2 = this.mLastWallpaperXStep;
        if (f2 < 0.0f) {
            f2 = -1.0f;
        }
        float wpxs = f2;
        int availw = (wallpaperWin.getFrame().right - wallpaperWin.getFrame().left) - dw;
        int offset3 = availw > 0 ? -((int) ((availw * wpx) + 0.5f)) : 0;
        int i = this.mLastWallpaperDisplayOffsetX;
        if (i != Integer.MIN_VALUE) {
            offset3 += i;
        }
        int xOffset = offset3;
        if (wallpaperWin.mWallpaperX != wpx || wallpaperWin.mWallpaperXStep != wpxs) {
            wallpaperWin.mWallpaperX = wpx;
            wallpaperWin.mWallpaperXStep = wpxs;
            rawChanged2 = true;
        }
        float f3 = this.mLastWallpaperY;
        if (f3 < 0.0f) {
            f3 = 0.5f;
        }
        float wpy2 = f3;
        float f4 = this.mLastWallpaperYStep;
        if (f4 < 0.0f) {
            f4 = -1.0f;
        }
        float wpys = f4;
        int availh = (wallpaperWin.getFrame().bottom - wallpaperWin.getFrame().top) - dh;
        if (availh > 0) {
            wpy = wpy2;
            offset = -((int) ((availh * wpy) + 0.5f));
        } else {
            wpy = wpy2;
            offset = 0;
        }
        int offset4 = this.mLastWallpaperDisplayOffsetY;
        if (offset4 == Integer.MIN_VALUE) {
            offset2 = offset;
        } else {
            offset2 = offset + offset4;
        }
        int yOffset = offset2;
        if (wallpaperWin.mWallpaperY != wpy || wallpaperWin.mWallpaperYStep != wpys) {
            wallpaperWin.mWallpaperY = wpy;
            wallpaperWin.mWallpaperYStep = wpys;
            rawChanged2 = true;
        }
        if (Float.compare(wallpaperWin.mWallpaperZoomOut, this.mLastWallpaperZoomOut) == 0) {
            rawChanged = rawChanged2;
        } else {
            wallpaperWin.mWallpaperZoomOut = this.mLastWallpaperZoomOut;
            rawChanged = true;
        }
        boolean changed = wallpaperWin.setWallpaperOffset(xOffset, yOffset, wallpaperWin.mShouldScaleWallpaper ? zoomOutToScale(wallpaperWin.mWallpaperZoomOut) : 1.0f);
        if (rawChanged && (wallpaperWin.mAttrs.privateFlags & 4) != 0) {
            try {
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    try {
                        Slog.v("WindowManager", "Report new wp offset " + wallpaperWin + " x=" + wallpaperWin.mWallpaperX + " y=" + wallpaperWin.mWallpaperY + " zoom=" + wallpaperWin.mWallpaperZoomOut);
                    } catch (RemoteException e) {
                    }
                }
                if (sync) {
                    this.mWaitingOnWallpaper = wallpaperWin;
                }
                try {
                    try {
                        try {
                            try {
                                try {
                                    wallpaperWin.mClient.dispatchWallpaperOffsets(wallpaperWin.mWallpaperX, wallpaperWin.mWallpaperY, wallpaperWin.mWallpaperXStep, wallpaperWin.mWallpaperYStep, wallpaperWin.mWallpaperZoomOut, sync);
                                    if (sync && this.mWaitingOnWallpaper != null) {
                                        long start = SystemClock.uptimeMillis();
                                        if (this.mLastWallpaperTimeoutTime + 10000 < start) {
                                            try {
                                                if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                                                    Slog.v("WindowManager", "Waiting for offset complete...");
                                                }
                                                this.mService.mGlobalLock.wait(150L);
                                            } catch (InterruptedException e2) {
                                            }
                                            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                                                Slog.v("WindowManager", "Offset complete!");
                                            }
                                            if (150 + start < SystemClock.uptimeMillis()) {
                                                Slog.i("WindowManager", "Timeout waiting for wallpaper to offset: " + wallpaperWin);
                                                this.mLastWallpaperTimeoutTime = start;
                                            }
                                        }
                                        this.mWaitingOnWallpaper = null;
                                    }
                                } catch (RemoteException e3) {
                                }
                            } catch (RemoteException e4) {
                            }
                        } catch (RemoteException e5) {
                        }
                    } catch (RemoteException e6) {
                    }
                } catch (RemoteException e7) {
                }
            } catch (RemoteException e8) {
            }
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowWallpaperPosition(WindowState window, float x, float y, float xStep, float yStep) {
        if (window.mWallpaperX != x || window.mWallpaperY != y) {
            window.mWallpaperX = x;
            window.mWallpaperY = y;
            window.mWallpaperXStep = xStep;
            window.mWallpaperYStep = yStep;
            updateWallpaperOffsetLocked(window, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWallpaperZoomOut(WindowState window, float zoom) {
        if (Float.compare(window.mWallpaperZoomOut, zoom) != 0) {
            window.mWallpaperZoomOut = zoom;
            this.mShouldUpdateZoom = true;
            updateWallpaperOffsetLocked(window, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShouldZoomOutWallpaper(WindowState window, boolean shouldZoom) {
        if (shouldZoom != window.mShouldScaleWallpaper) {
            window.mShouldScaleWallpaper = shouldZoom;
            updateWallpaperOffsetLocked(window, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowWallpaperDisplayOffset(WindowState window, int x, int y) {
        if (window.mWallpaperDisplayOffsetX != x || window.mWallpaperDisplayOffsetY != y) {
            window.mWallpaperDisplayOffsetX = x;
            window.mWallpaperDisplayOffsetY = y;
            updateWallpaperOffsetLocked(window, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle sendWindowWallpaperCommand(WindowState window, String action, int x, int y, int z, Bundle extras, boolean sync) {
        if (window == this.mWallpaperTarget || window == this.mPrevWallpaperTarget) {
            sendWindowWallpaperCommand(action, x, y, z, extras, sync);
            return null;
        }
        return null;
    }

    private void sendWindowWallpaperCommand(String action, int x, int y, int z, Bundle extras, boolean sync) {
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            WallpaperWindowToken token = this.mWallpaperTokens.get(curTokenNdx);
            token.sendWindowWallpaperCommand(action, x, y, z, extras, sync);
        }
    }

    private void updateWallpaperOffsetLocked(WindowState changingTarget, boolean sync) {
        WindowState target = this.mWallpaperTarget;
        if (target == null && changingTarget.mToken.isVisible() && changingTarget.mTransitionController.inTransition()) {
            target = changingTarget;
        }
        if (target != null) {
            if (target.mWallpaperX >= 0.0f) {
                this.mLastWallpaperX = target.mWallpaperX;
            } else if (changingTarget.mWallpaperX >= 0.0f) {
                this.mLastWallpaperX = changingTarget.mWallpaperX;
            }
            if (target.mWallpaperY >= 0.0f) {
                this.mLastWallpaperY = target.mWallpaperY;
            } else if (changingTarget.mWallpaperY >= 0.0f) {
                this.mLastWallpaperY = changingTarget.mWallpaperY;
            }
            computeLastWallpaperZoomOut();
            if (target.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetX = target.mWallpaperDisplayOffsetX;
            } else if (changingTarget.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetX = changingTarget.mWallpaperDisplayOffsetX;
            }
            if (target.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetY = target.mWallpaperDisplayOffsetY;
            } else if (changingTarget.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetY = changingTarget.mWallpaperDisplayOffsetY;
            }
            if (target.mWallpaperXStep >= 0.0f) {
                this.mLastWallpaperXStep = target.mWallpaperXStep;
            } else if (changingTarget.mWallpaperXStep >= 0.0f) {
                this.mLastWallpaperXStep = changingTarget.mWallpaperXStep;
            }
            if (target.mWallpaperYStep >= 0.0f) {
                this.mLastWallpaperYStep = target.mWallpaperYStep;
            } else if (changingTarget.mWallpaperYStep >= 0.0f) {
                this.mLastWallpaperYStep = changingTarget.mWallpaperYStep;
            }
        }
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            this.mWallpaperTokens.get(curTokenNdx).updateWallpaperOffset(sync);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearLastWallpaperTimeoutTime() {
        this.mLastWallpaperTimeoutTime = 0L;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void wallpaperCommandComplete(IBinder window) {
        WindowState windowState = this.mWaitingOnWallpaper;
        if (windowState != null && windowState.mClient.asBinder() == window) {
            this.mWaitingOnWallpaper = null;
            this.mService.mGlobalLock.notifyAll();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void wallpaperOffsetsComplete(IBinder window) {
        WindowState windowState = this.mWaitingOnWallpaper;
        if (windowState != null && windowState.mClient.asBinder() == window) {
            this.mWaitingOnWallpaper = null;
            this.mService.mGlobalLock.notifyAll();
        }
    }

    private void findWallpaperTarget() {
        this.mFindResults.reset();
        if (this.mDisplayContent.getDefaultTaskDisplayArea().isRootTaskVisible(5)) {
            this.mFindResults.setUseTopWallpaperAsTarget(true);
        }
        this.mDisplayContent.forAllWindows(this.mFindWallpaperTargetFunction, true);
        if (this.mFindResults.wallpaperTarget == null && this.mFindResults.useTopWallpaperAsTarget) {
            FindWallpaperTargetResult findWallpaperTargetResult = this.mFindResults;
            findWallpaperTargetResult.setWallpaperTarget(findWallpaperTargetResult.topWallpaper);
        }
    }

    private boolean isFullscreen(WindowManager.LayoutParams attrs) {
        return attrs.x == 0 && attrs.y == 0 && attrs.width == -1 && attrs.height == -1;
    }

    private void updateWallpaperWindowsTarget(FindWallpaperTargetResult result) {
        WindowState windowState;
        WindowState windowState2;
        WindowState wallpaperTarget = result.wallpaperTarget;
        if (this.mWallpaperTarget == wallpaperTarget || ((windowState2 = this.mPrevWallpaperTarget) != null && windowState2 == wallpaperTarget)) {
            WindowState prevWallpaperTarget = this.mPrevWallpaperTarget;
            if (prevWallpaperTarget != null && !prevWallpaperTarget.isAnimatingLw()) {
                if (ProtoLogCache.WM_DEBUG_WALLPAPER_enabled) {
                    windowState = null;
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WALLPAPER, -1478175541, 0, (String) null, (Object[]) null);
                } else {
                    windowState = null;
                }
                this.mPrevWallpaperTarget = windowState;
                this.mWallpaperTarget = wallpaperTarget;
                return;
            }
            return;
        }
        if (ProtoLogCache.WM_DEBUG_WALLPAPER_enabled) {
            String protoLogParam0 = String.valueOf(wallpaperTarget);
            String protoLogParam1 = String.valueOf(this.mWallpaperTarget);
            String protoLogParam2 = String.valueOf(Debug.getCallers(5));
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WALLPAPER, 114070759, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
        }
        this.mPrevWallpaperTarget = null;
        final WindowState prevWallpaperTarget2 = this.mWallpaperTarget;
        this.mWallpaperTarget = wallpaperTarget;
        if (prevWallpaperTarget2 == null && wallpaperTarget != null) {
            updateWallpaperOffsetLocked(wallpaperTarget, false);
        }
        if (wallpaperTarget == null || prevWallpaperTarget2 == null) {
            return;
        }
        boolean oldAnim = prevWallpaperTarget2.isAnimatingLw();
        boolean foundAnim = wallpaperTarget.isAnimatingLw();
        if (ProtoLogCache.WM_DEBUG_WALLPAPER_enabled) {
            String protoLogParam02 = String.valueOf(foundAnim);
            String protoLogParam12 = String.valueOf(oldAnim);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WALLPAPER, -275077723, 0, (String) null, new Object[]{protoLogParam02, protoLogParam12});
        }
        if (!foundAnim || !oldAnim || this.mDisplayContent.getWindow(new Predicate() { // from class: com.android.server.wm.WallpaperController$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return WallpaperController.lambda$updateWallpaperWindowsTarget$2(WindowState.this, (WindowState) obj);
            }
        }) == null) {
            return;
        }
        boolean newTargetHidden = (wallpaperTarget.mActivityRecord == null || wallpaperTarget.mActivityRecord.mVisibleRequested) ? false : true;
        boolean oldTargetHidden = (prevWallpaperTarget2.mActivityRecord == null || prevWallpaperTarget2.mActivityRecord.mVisibleRequested) ? false : true;
        if (ProtoLogCache.WM_DEBUG_WALLPAPER_enabled) {
            String protoLogParam03 = String.valueOf(prevWallpaperTarget2);
            boolean protoLogParam13 = oldTargetHidden;
            String protoLogParam22 = String.valueOf(wallpaperTarget);
            boolean protoLogParam3 = newTargetHidden;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WALLPAPER, -360208282, 204, (String) null, new Object[]{protoLogParam03, Boolean.valueOf(protoLogParam13), protoLogParam22, Boolean.valueOf(protoLogParam3)});
        }
        this.mPrevWallpaperTarget = prevWallpaperTarget2;
        if (newTargetHidden && !oldTargetHidden) {
            if (ProtoLogCache.WM_DEBUG_WALLPAPER_enabled) {
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WALLPAPER, 1178653181, 0, (String) null, (Object[]) null);
            }
            this.mWallpaperTarget = prevWallpaperTarget2;
        } else if (newTargetHidden == oldTargetHidden && !this.mDisplayContent.mOpeningApps.contains(wallpaperTarget.mActivityRecord) && (this.mDisplayContent.mOpeningApps.contains(prevWallpaperTarget2.mActivityRecord) || this.mDisplayContent.mClosingApps.contains(prevWallpaperTarget2.mActivityRecord))) {
            this.mWallpaperTarget = prevWallpaperTarget2;
        }
        result.setWallpaperTarget(wallpaperTarget);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$updateWallpaperWindowsTarget$2(WindowState prevWallpaperTarget, WindowState w) {
        return w == prevWallpaperTarget;
    }

    private void updateWallpaperTokens(boolean visible) {
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            WallpaperWindowToken token = this.mWallpaperTokens.get(curTokenNdx);
            if (token.updateWallpaperWindows(visible)) {
                token.mDisplayContent.assignWindowLayers(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void adjustWallpaperWindows() {
        this.mDisplayContent.mWallpaperMayChange = false;
        findWallpaperTarget();
        updateWallpaperWindowsTarget(this.mFindResults);
        boolean visible = this.mWallpaperTarget != null;
        if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
            Slog.v("WindowManager", "Wallpaper visibility: " + visible + " at display " + this.mDisplayContent.getDisplayId());
        }
        if (visible) {
            if (this.mWallpaperTarget.mWallpaperX >= 0.0f) {
                this.mLastWallpaperX = this.mWallpaperTarget.mWallpaperX;
                this.mLastWallpaperXStep = this.mWallpaperTarget.mWallpaperXStep;
            }
            computeLastWallpaperZoomOut();
            if (this.mWallpaperTarget.mWallpaperY >= 0.0f) {
                this.mLastWallpaperY = this.mWallpaperTarget.mWallpaperY;
                this.mLastWallpaperYStep = this.mWallpaperTarget.mWallpaperYStep;
            }
            if (this.mWallpaperTarget.mWallpaperDisplayOffsetX != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetX = this.mWallpaperTarget.mWallpaperDisplayOffsetX;
            }
            if (this.mWallpaperTarget.mWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
                this.mLastWallpaperDisplayOffsetY = this.mWallpaperTarget.mWallpaperDisplayOffsetY;
            }
        }
        updateWallpaperTokens(visible);
        if (visible && this.mLastFrozen != this.mFindResults.isWallpaperTargetForLetterbox) {
            this.mLastFrozen = this.mFindResults.isWallpaperTargetForLetterbox;
            sendWindowWallpaperCommand(this.mFindResults.isWallpaperTargetForLetterbox ? "android.wallpaper.freeze" : "android.wallpaper.unfreeze", 0, 0, 0, null, false);
        }
        if (ProtoLogCache.WM_DEBUG_WALLPAPER_enabled) {
            String protoLogParam0 = String.valueOf(this.mWallpaperTarget);
            String protoLogParam1 = String.valueOf(this.mPrevWallpaperTarget);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_WALLPAPER, -304728471, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean processWallpaperDrawPendingTimeout() {
        if (this.mWallpaperDrawState == 1) {
            this.mWallpaperDrawState = 2;
            if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                Slog.v("WindowManager", "*** WALLPAPER DRAW TIMEOUT");
            }
            if (this.mService.getRecentsAnimationController() != null) {
                this.mService.getRecentsAnimationController().startAnimation();
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean wallpaperTransitionReady() {
        boolean transitionReady = true;
        boolean wallpaperReady = true;
        int curTokenIndex = this.mWallpaperTokens.size() - 1;
        while (true) {
            if (curTokenIndex < 0 || 1 == 0) {
                break;
            }
            WallpaperWindowToken token = this.mWallpaperTokens.get(curTokenIndex);
            if (!token.hasVisibleNotDrawnWallpaper()) {
                curTokenIndex--;
            } else {
                wallpaperReady = false;
                int i = this.mWallpaperDrawState;
                if (i != 2) {
                    transitionReady = false;
                }
                if (i == 0) {
                    this.mWallpaperDrawState = 1;
                    this.mService.mH.removeMessages(39, this);
                    this.mService.mH.sendMessageDelayed(this.mService.mH.obtainMessage(39, this), 500L);
                }
                if (WindowManagerDebugConfig.DEBUG_WALLPAPER) {
                    Slog.v("WindowManager", "Wallpaper should be visible but has not been drawn yet. mWallpaperDrawState=" + this.mWallpaperDrawState);
                }
            }
        }
        if (wallpaperReady) {
            this.mWallpaperDrawState = 0;
            this.mService.mH.removeMessages(39, this);
        }
        return transitionReady;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void adjustWallpaperWindowsForAppTransitionIfNeeded(ArraySet<ActivityRecord> openingApps) {
        boolean adjust = false;
        if ((this.mDisplayContent.pendingLayoutChanges & 4) != 0) {
            adjust = true;
        } else {
            int i = openingApps.size() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                ActivityRecord activity = openingApps.valueAt(i);
                if (!activity.windowsCanBeWallpaperTarget()) {
                    i--;
                } else {
                    adjust = true;
                    break;
                }
            }
        }
        if (adjust) {
            adjustWallpaperWindows();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addWallpaperToken(WallpaperWindowToken token) {
        this.mWallpaperTokens.add(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeWallpaperToken(WallpaperWindowToken token) {
        this.mWallpaperTokens.remove(token);
    }

    boolean canScreenshotWallpaper() {
        return canScreenshotWallpaper(getTopVisibleWallpaper());
    }

    private boolean canScreenshotWallpaper(WindowState wallpaperWindowState) {
        if (!this.mService.mPolicy.isScreenOn()) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.i("WindowManager", "Attempted to take screenshot while display was off.");
            }
            return false;
        } else if (wallpaperWindowState == null) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.i("WindowManager", "No visible wallpaper to screenshot");
            }
            return false;
        } else {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bitmap tranScreenshotWallpaperLocked(float frameScale) {
        WindowState wallpaperWindowState = getTopVisibleWallpaper();
        if (canScreenshotWallpaper(wallpaperWindowState)) {
            Rect bounds = wallpaperWindowState.getBounds();
            bounds.offsetTo(0, 0);
            Log.d("WindowManager", " tranScreenshotWallpaperLocked :" + bounds + " " + wallpaperWindowState + " mLastHScale:" + wallpaperWindowState.getLastHScale() + " mLastVScale:" + wallpaperWindowState.getLastVScale() + " getSurfacePositon:" + wallpaperWindowState.getSurfacePositon());
            if (wallpaperWindowState.getLastHScale() == 1.0f && wallpaperWindowState.getLastVScale() == 1.0f && wallpaperWindowState.getSurfacePositon().equals(0, 0)) {
                return screenshotWallpaperLocked();
            }
            SurfaceControl.LayerCaptureArgs captureArgs = new SurfaceControl.LayerCaptureArgs.Builder(wallpaperWindowState.getSurfaceControl()).setSourceCrop(bounds).setFrameScale(frameScale).setIsScreenShotWallPaper(true).build();
            SurfaceControl.ScreenshotHardwareBuffer wallpaperBuffer = SurfaceControl.captureLayers(captureArgs);
            if (wallpaperBuffer == null) {
                Slog.w("WindowManager", "Failed to screenshot wallpaper");
                return null;
            }
            return Bitmap.wrapHardwareBuffer(wallpaperBuffer.getHardwareBuffer(), wallpaperBuffer.getColorSpace());
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bitmap screenshotWallpaperLocked() {
        return screenshotWallpaperLocked(1.0f);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bitmap screenshotWallpaperLocked(float frameScale) {
        WindowState wallpaperWindowState = getTopVisibleWallpaper();
        if (canScreenshotWallpaper(wallpaperWindowState)) {
            Rect bounds = wallpaperWindowState.getBounds();
            bounds.offsetTo(0, 0);
            SurfaceControl.ScreenshotHardwareBuffer wallpaperBuffer = SurfaceControl.captureLayers(wallpaperWindowState.getSurfaceControl(), bounds, frameScale);
            if (wallpaperBuffer == null) {
                Slog.w("WindowManager", "Failed to screenshot wallpaper");
                return null;
            }
            return Bitmap.wrapHardwareBuffer(wallpaperBuffer.getHardwareBuffer(), wallpaperBuffer.getColorSpace());
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl mirrorWallpaperSurface() {
        WindowState wallpaperWindowState = getTopVisibleWallpaper();
        if (wallpaperWindowState != null) {
            return SurfaceControl.mirrorSurface(wallpaperWindowState.getSurfaceControl());
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getTopVisibleWallpaper() {
        this.mTmpTopWallpaper = null;
        for (int curTokenNdx = this.mWallpaperTokens.size() - 1; curTokenNdx >= 0; curTokenNdx--) {
            WallpaperWindowToken token = this.mWallpaperTokens.get(curTokenNdx);
            token.forAllWindows(new ToBooleanFunction() { // from class: com.android.server.wm.WallpaperController$$ExternalSyntheticLambda3
                public final boolean apply(Object obj) {
                    return WallpaperController.this.m8457xa66cd4de((WindowState) obj);
                }
            }, true);
        }
        return this.mTmpTopWallpaper;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTopVisibleWallpaper$3$com-android-server-wm-WallpaperController  reason: not valid java name */
    public /* synthetic */ boolean m8457xa66cd4de(WindowState w) {
        WindowStateAnimator winAnim = w.mWinAnimator;
        if (winAnim != null && winAnim.getShown() && winAnim.mLastAlpha > 0.0f) {
            this.mTmpTopWallpaper = w;
            return true;
        }
        return false;
    }

    private void computeLastWallpaperZoomOut() {
        if (this.mShouldUpdateZoom) {
            this.mLastWallpaperZoomOut = 0.0f;
            this.mDisplayContent.forAllWindows(this.mComputeMaxZoomOutFunction, true);
            this.mShouldUpdateZoom = false;
        }
    }

    private float zoomOutToScale(float zoom) {
        return MathUtils.lerp(1.0f, this.mMaxWallpaperScale, 1.0f - zoom);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("displayId=");
        pw.println(this.mDisplayContent.getDisplayId());
        pw.print(prefix);
        pw.print("mWallpaperTarget=");
        pw.println(this.mWallpaperTarget);
        if (this.mPrevWallpaperTarget != null) {
            pw.print(prefix);
            pw.print("mPrevWallpaperTarget=");
            pw.println(this.mPrevWallpaperTarget);
        }
        pw.print(prefix);
        pw.print("mLastWallpaperX=");
        pw.print(this.mLastWallpaperX);
        pw.print(" mLastWallpaperY=");
        pw.println(this.mLastWallpaperY);
        if (this.mLastWallpaperDisplayOffsetX != Integer.MIN_VALUE || this.mLastWallpaperDisplayOffsetY != Integer.MIN_VALUE) {
            pw.print(prefix);
            pw.print("mLastWallpaperDisplayOffsetX=");
            pw.print(this.mLastWallpaperDisplayOffsetX);
            pw.print(" mLastWallpaperDisplayOffsetY=");
            pw.println(this.mLastWallpaperDisplayOffsetY);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class FindWallpaperTargetResult {
        boolean isWallpaperTargetForLetterbox;
        boolean resetTopWallpaper;
        WindowState topWallpaper;
        boolean useTopWallpaperAsTarget;
        WindowState wallpaperTarget;

        private FindWallpaperTargetResult() {
            this.topWallpaper = null;
            this.useTopWallpaperAsTarget = false;
            this.wallpaperTarget = null;
            this.resetTopWallpaper = false;
            this.isWallpaperTargetForLetterbox = false;
        }

        void setTopWallpaper(WindowState win) {
            this.topWallpaper = win;
        }

        void setWallpaperTarget(WindowState win) {
            this.wallpaperTarget = win;
        }

        void setUseTopWallpaperAsTarget(boolean topWallpaperAsTarget) {
            this.useTopWallpaperAsTarget = topWallpaperAsTarget;
        }

        void setIsWallpaperTargetForLetterbox(boolean isWallpaperTargetForLetterbox) {
            this.isWallpaperTargetForLetterbox = isWallpaperTargetForLetterbox;
        }

        void reset() {
            this.topWallpaper = null;
            this.wallpaperTarget = null;
            this.useTopWallpaperAsTarget = false;
            this.resetTopWallpaper = false;
            this.isWallpaperTargetForLetterbox = false;
        }
    }
}
