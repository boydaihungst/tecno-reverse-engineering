package com.android.server.wm;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Debug;
import android.os.Trace;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.policy.WindowManagerPolicy;
import com.transsion.hubcore.server.wm.ITranWindowState;
import java.io.PrintWriter;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowStateAnimator {
    static final int COMMIT_DRAW_PENDING = 2;
    static final int DRAW_PENDING = 1;
    static final int HAS_DRAWN = 4;
    static final int NO_SURFACE = 0;
    static final int PRESERVED_SURFACE_LAYER = 1;
    static final int READY_TO_SHOW = 3;
    static final int ROOT_TASK_CLIP_AFTER_ANIM = 0;
    static final int ROOT_TASK_CLIP_NONE = 1;
    static final String TAG = "WindowManager";
    boolean mAnimationIsEntrance;
    final WindowAnimator mAnimator;
    int mAttrType;
    final Context mContext;
    int mDrawState;
    boolean mEnterAnimationPending;
    boolean mEnteringAnimation;
    final boolean mIsWallpaper;
    boolean mLastHidden;
    final WindowManagerPolicy mPolicy;
    final WindowManagerService mService;
    final Session mSession;
    WindowSurfaceController mSurfaceController;
    private final WallpaperController mWallpaperControllerLocked;
    final WindowState mWin;
    float mShownAlpha = 0.0f;
    float mAlpha = 0.0f;
    float mLastAlpha = 0.0f;
    private final Rect mSystemDecorRect = new Rect();
    private final SurfaceControl.Transaction mPostDrawTransaction = new SurfaceControl.Transaction();

    /* JADX INFO: Access modifiers changed from: package-private */
    public String drawStateToString() {
        int i = this.mDrawState;
        switch (i) {
            case 0:
                return "NO_SURFACE";
            case 1:
                return "DRAW_PENDING";
            case 2:
                return "COMMIT_DRAW_PENDING";
            case 3:
                return "READY_TO_SHOW";
            case 4:
                return "HAS_DRAWN";
            default:
                return Integer.toString(i);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowStateAnimator(WindowState win) {
        WindowManagerService service = win.mWmService;
        this.mService = service;
        this.mAnimator = service.mAnimator;
        this.mPolicy = service.mPolicy;
        this.mContext = service.mContext;
        this.mWin = win;
        this.mSession = win.mSession;
        this.mAttrType = win.mAttrs.type;
        this.mIsWallpaper = win.mIsWallpaper;
        this.mWallpaperControllerLocked = win.getDisplayContent().mWallpaperController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAnimationFinished() {
        Trace.traceBegin(32L, "win animation done");
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(this);
            boolean protoLogParam1 = this.mWin.mAnimatingExit;
            boolean protoLogParam2 = this.mWin.mActivityRecord != null && this.mWin.mActivityRecord.reportedVisible;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, 1810209625, 60, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2)});
        }
        this.mWin.checkPolicyVisibilityChange();
        DisplayContent displayContent = this.mWin.getDisplayContent();
        int i = this.mAttrType;
        if ((i == 2000 || i == 2040) && this.mWin.isVisibleByPolicy()) {
            displayContent.setLayoutNeeded();
        }
        this.mWin.onExitAnimationDone();
        displayContent.pendingLayoutChanges |= 8;
        if (displayContent.mWallpaperController.isWallpaperTarget(this.mWin)) {
            displayContent.pendingLayoutChanges |= 4;
        }
        if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
            this.mService.mWindowPlacerLocked.debugLayoutRepeats("WindowStateAnimator", displayContent.pendingLayoutChanges);
        }
        if (this.mWin.mActivityRecord != null) {
            this.mWin.mActivityRecord.updateReportedVisibilityLocked();
        }
        Trace.traceEnd(32L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hide(SurfaceControl.Transaction transaction, String reason) {
        if (!this.mLastHidden) {
            this.mLastHidden = true;
            WindowSurfaceController windowSurfaceController = this.mSurfaceController;
            if (windowSurfaceController != null) {
                windowSurfaceController.hide(transaction, reason);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean finishDrawingLocked(SurfaceControl.Transaction postDrawTransaction, boolean forceApplyNow) {
        boolean startingWindow = this.mWin.mAttrs.type == 3;
        if (startingWindow && ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
            String protoLogParam0 = String.valueOf(this.mWin);
            String protoLogParam1 = String.valueOf(drawStateToString());
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -344488673, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        boolean layoutNeeded = false;
        if (this.mDrawState == 1) {
            if (ProtoLogCache.WM_DEBUG_DRAW_enabled) {
                String protoLogParam02 = String.valueOf(this.mWin);
                String protoLogParam12 = String.valueOf(this.mSurfaceController);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_DRAW, -993378225, 0, (String) null, new Object[]{protoLogParam02, protoLogParam12});
            }
            if (startingWindow && ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                String protoLogParam03 = String.valueOf(this.mWin);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 829434921, 0, (String) null, new Object[]{protoLogParam03});
            }
            this.mDrawState = 2;
            layoutNeeded = true;
        }
        if (postDrawTransaction != null) {
            if (this.mLastHidden && this.mDrawState != 0 && !forceApplyNow) {
                this.mPostDrawTransaction.merge(postDrawTransaction);
            } else {
                this.mWin.getSyncTransaction().merge(postDrawTransaction);
            }
            return true;
        }
        return layoutNeeded;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean commitFinishDrawingLocked() {
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE && this.mWin.mAttrs.type == 3) {
            Slog.i("WindowManager", "commitFinishDrawingLocked: " + this.mWin + " cur mDrawState=" + drawStateToString());
        }
        int i = this.mDrawState;
        if (i == 2 || i == 3) {
            if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                String protoLogParam0 = String.valueOf(this.mSurfaceController);
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ANIM, -203358733, 0, (String) null, new Object[]{protoLogParam0});
            }
            this.mDrawState = 3;
            boolean result = false;
            ActivityRecord activity = this.mWin.mActivityRecord;
            if (Build.IS_DEBUG_ENABLE) {
                Slog.d("WindowManager", "System monitor commitFinishDrawingLocked activity=" + activity + " canShowWindows=" + (activity != null ? Boolean.valueOf(activity.canShowWindows()) : null) + " type=" + this.mWin.mAttrs.type);
            }
            if (activity == null || activity.canShowWindows() || this.mWin.mAttrs.type == 3) {
                if (WindowState.DEBUG_ADB) {
                    this.mWin.setPerformShowLogFlag(true);
                }
                result = this.mWin.performShowLocked();
                if (WindowState.DEBUG_ADB) {
                    this.mWin.setPerformShowLogFlag(false);
                }
            }
            return result;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetDrawState() {
        this.mDrawState = 1;
        if (this.mWin.mActivityRecord != null && !this.mWin.mActivityRecord.isAnimating(1)) {
            this.mWin.mActivityRecord.clearAllDrawn();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowSurfaceController createSurfaceLocked() {
        int flags;
        WindowState w = this.mWin;
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            return windowSurfaceController;
        }
        w.setHasSurface(false);
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ANIM, 1335791109, 0, (String) null, new Object[]{protoLogParam0});
        }
        resetDrawState();
        this.mService.makeWindowFreezingScreenIfNeededLocked(w);
        int flags2 = 4;
        WindowManager.LayoutParams attrs = w.mAttrs;
        if (w.isSecureLocked()) {
            flags2 = 4 | 128;
        }
        if ((this.mWin.mAttrs.privateFlags & 1048576) == 0) {
            flags = flags2;
        } else {
            flags = flags2 | 64;
        }
        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v("WindowManager", "Creating surface in session " + this.mSession.mSurfaceSession + " window " + this + " format=" + attrs.format + " flags=" + flags);
        }
        try {
            boolean isHwAccelerated = (attrs.flags & 16777216) != 0;
            int format = isHwAccelerated ? -3 : attrs.format;
            WindowSurfaceController windowSurfaceController2 = new WindowSurfaceController(attrs.getTitle().toString(), format, flags, this, attrs.type);
            this.mSurfaceController = windowSurfaceController2;
            windowSurfaceController2.setColorSpaceAgnostic((attrs.privateFlags & 16777216) != 0);
            w.setHasSurface(true);
            w.mInputWindowHandle.forceChange();
            if (ProtoLogCache.WM_SHOW_SURFACE_ALLOC_enabled) {
                String protoLogParam02 = String.valueOf(this.mSurfaceController);
                String protoLogParam1 = String.valueOf(this.mSession.mSurfaceSession);
                long protoLogParam2 = this.mSession.mPid;
                long protoLogParam3 = attrs.format;
                long protoLogParam4 = flags;
                try {
                    String protoLogParam5 = String.valueOf(this);
                    ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_SURFACE_ALLOC, 745391677, 336, (String) null, new Object[]{protoLogParam02, protoLogParam1, Long.valueOf(protoLogParam2), Long.valueOf(protoLogParam3), Long.valueOf(protoLogParam4), protoLogParam5});
                } catch (Surface.OutOfResourcesException e) {
                    Slog.w("WindowManager", "OutOfResourcesException creating surface");
                    this.mService.mRoot.reclaimSomeSurfaceMemory(this, "create", true);
                    this.mDrawState = 0;
                    return null;
                } catch (Exception e2) {
                    e = e2;
                    Slog.e("WindowManager", "Exception creating surface (parent dead?)", e);
                    this.mDrawState = 0;
                    return null;
                }
            }
            if (WindowManagerDebugConfig.DEBUG) {
                Slog.v("WindowManager", "Got surface: " + this.mSurfaceController + ", set left=" + w.getFrame().left + " top=" + w.getFrame().top);
            }
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                Slog.i("WindowManager", ">>> OPEN TRANSACTION createSurfaceLocked");
                WindowManagerService.logSurface(w, "CREATE pos=(" + w.getFrame().left + "," + w.getFrame().top + ") HIDE", false);
            }
            this.mLastHidden = true;
            TranFoldWMCustody.instance().createWindowStateAnimatorSurface(this);
            if (WindowManagerDebugConfig.DEBUG) {
                Slog.v("WindowManager", "Created surface " + this);
            }
            return this.mSurfaceController;
        } catch (Surface.OutOfResourcesException e3) {
        } catch (Exception e4) {
            e = e4;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSurface() {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        return windowSurfaceController != null && windowSurfaceController.hasSurface();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroySurfaceLocked(SurfaceControl.Transaction t) {
        if (this.mSurfaceController == null) {
            return;
        }
        this.mWin.mHidden = true;
        try {
            if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                WindowManagerService.logWithStack("WindowManager", "Window " + this + " destroying surface " + this.mSurfaceController + ", session " + this.mSession);
            }
            if (ProtoLogCache.WM_SHOW_SURFACE_ALLOC_enabled) {
                String protoLogParam0 = String.valueOf(this.mWin);
                String protoLogParam1 = String.valueOf(new RuntimeException().fillInStackTrace());
                ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_SURFACE_ALLOC, -1391944764, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
            }
            destroySurface(t);
            this.mWallpaperControllerLocked.hideWallpapers(this.mWin);
        } catch (RuntimeException e) {
            Slog.w("WindowManager", "Exception thrown when destroying Window " + this + " surface " + this.mSurfaceController + " session " + this.mSession + ": " + e.toString());
        }
        this.mWin.setHasSurface(false);
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            windowSurfaceController.setShown(false);
        }
        this.mSurfaceController = null;
        this.mDrawState = 0;
    }

    void computeShownFrameLocked() {
        if ((this.mIsWallpaper && this.mService.mRoot.mWallpaperActionPending) || this.mWin.isDragResizeChanged()) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG) {
            Slog.v("WindowManager", "computeShownFrameLocked: " + this + " not attached, mAlpha=" + this.mAlpha);
        }
        this.mShownAlpha = this.mAlpha;
    }

    private boolean isInBlastSync() {
        return this.mService.useBLASTSync() && this.mWin.useBLASTSync();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prepareSurfaceLocked(SurfaceControl.Transaction t) {
        WindowState w = this.mWin;
        if (!hasSurface()) {
            if (w.getOrientationChanging() && w.isGoneForLayout()) {
                if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                    String protoLogParam0 = String.valueOf(w);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1756082882, 0, (String) null, new Object[]{protoLogParam0});
                }
                w.setOrientationChanging(false);
                return;
            }
            return;
        }
        computeShownFrameLocked();
        if (WindowManagerDebugConfig.SHOW_TRANSACTIONS) {
            this.mService.mWindowManagerDebugger.debugPrepareSurfaceLocked("WindowManager", this.mIsWallpaper, this.mWin, w.isParentWindowHidden(), w.isOnScreen(), w.mPolicyVisibility, w.mHasSurface, w.mDestroying, this.mLastHidden);
        }
        if (w.isParentWindowHidden() || !w.isOnScreen()) {
            boolean hasHidden = this.mLastHidden;
            hide(t, "prepareSurfaceLocked");
            if (!hasHidden) {
                this.mWallpaperControllerLocked.hideWallpapers(w);
            }
            if (w.getOrientationChanging() && w.isGoneForLayout()) {
                w.setOrientationChanging(false);
                if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                    String protoLogParam02 = String.valueOf(w);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1756082882, 0, (String) null, new Object[]{protoLogParam02});
                }
            }
        } else {
            float f = this.mLastAlpha;
            float f2 = this.mShownAlpha;
            if (f != f2 || this.mLastHidden) {
                this.mLastAlpha = f2;
                if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
                    String protoLogParam03 = String.valueOf(this.mSurfaceController);
                    double protoLogParam1 = this.mShownAlpha;
                    double protoLogParam2 = w.mHScale;
                    double protoLogParam3 = w.mVScale;
                    String protoLogParam4 = String.valueOf(w);
                    ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, -1906387645, 168, (String) null, new Object[]{protoLogParam03, Double.valueOf(protoLogParam1), Double.valueOf(protoLogParam2), Double.valueOf(protoLogParam3), protoLogParam4});
                }
                boolean prepared = this.mSurfaceController.prepareToShowInTransaction(t, this.mShownAlpha);
                if (prepared && this.mDrawState == 4 && this.mLastHidden) {
                    if (showSurfaceRobustlyLocked(t)) {
                        this.mAnimator.requestRemovalOfReplacedWindows(w);
                        this.mLastHidden = false;
                        DisplayContent displayContent = w.getDisplayContent();
                        if (!displayContent.getLastHasContent()) {
                            displayContent.pendingLayoutChanges |= 8;
                            if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                                this.mService.mWindowPlacerLocked.debugLayoutRepeats("showSurfaceRobustlyLocked " + w, displayContent.pendingLayoutChanges);
                            }
                        }
                    } else {
                        w.setOrientationChanging(false);
                    }
                }
            } else if (this.mWin.isAnimating(3) && ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                String protoLogParam04 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, 1878927091, 0, (String) null, new Object[]{protoLogParam04});
            }
        }
        if (w.getOrientationChanging()) {
            if (!w.isDrawn()) {
                if (w.mDisplayContent.shouldSyncRotationChange(w)) {
                    w.mWmService.mRoot.mOrientationChangeComplete = false;
                    this.mAnimator.mLastWindowFreezeSource = w;
                }
                if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                    String protoLogParam05 = String.valueOf(w);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1130891072, 0, (String) null, new Object[]{protoLogParam05});
                    return;
                }
                return;
            }
            w.setOrientationChanging(false);
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                String protoLogParam06 = String.valueOf(w);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 916191774, 0, (String) null, new Object[]{protoLogParam06});
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideLayerLocked() {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController == null) {
            return;
        }
        windowSurfaceController.hideLayer();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetLayerLocked() {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController == null) {
            return;
        }
        windowSurfaceController.resetLayer();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOpaqueLocked(boolean isOpaque) {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController == null) {
            return;
        }
        windowSurfaceController.setOpaque(isOpaque);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSecureLocked(boolean isSecure) {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController == null) {
            return;
        }
        windowSurfaceController.setSecure(isSecure);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setColorSpaceAgnosticLocked(boolean agnostic) {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController == null) {
            return;
        }
        windowSurfaceController.setColorSpaceAgnostic(agnostic);
    }

    private boolean showSurfaceRobustlyLocked(SurfaceControl.Transaction t) {
        boolean shown = this.mSurfaceController.showRobustly(t);
        if (!shown) {
            return false;
        }
        t.merge(this.mPostDrawTransaction);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyEnterAnimationLocked() {
        applyEnterAnimationLocked(ITranWindowState.Instance().getAnimationTypeNormal());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyEnterAnimationLocked(int animationType) {
        int transit;
        if (this.mWin.mSkipEnterAnimationForSeamlessReplacement) {
            return;
        }
        if (this.mEnterAnimationPending) {
            this.mEnterAnimationPending = false;
            transit = 1;
        } else {
            transit = 3;
        }
        if (this.mAttrType != 1 && !this.mIsWallpaper) {
            applyAnimationLocked(transit, true, animationType);
        }
        if (this.mService.mAccessibilityController.hasCallbacks()) {
            this.mService.mAccessibilityController.onWindowTransition(this.mWin, transit);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean applyAnimationLocked(int transit, boolean isEntrance) {
        return applyAnimationLocked(transit, isEntrance, ITranWindowState.Instance().getAnimationTypeNormal());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean applyAnimationLocked(int transit, boolean isEntrance, int animationType) {
        Slog.i("WindowManager", "  isAnimating=" + this.mWin.isAnimating() + "  okToAnimate=" + this.mWin.mToken.okToAnimate() + "  win=" + this.mWin);
        if (this.mWin.isAnimating() && this.mAnimationIsEntrance == isEntrance) {
            return true;
        }
        boolean isImeWindow = this.mWin.mAttrs.type == 2011;
        if (isEntrance && isImeWindow) {
            this.mWin.getDisplayContent().adjustForImeIfNeeded();
            this.mWin.setDisplayLayoutNeeded();
            this.mService.mWindowPlacerLocked.requestTraversal();
        }
        if (this.mWin.mToken.okToAnimate()) {
            int anim = ITranWindowState.Instance().selectAnimation(animationType, transit);
            if (anim == -1) {
                anim = this.mWin.getDisplayContent().getDisplayPolicy().selectAnimation(this.mWin, transit);
            }
            int attr = -1;
            Animation a = null;
            if (anim != 0) {
                if (anim != -1) {
                    Trace.traceBegin(32L, "WSA#loadAnimation");
                    a = AnimationUtils.loadAnimation(this.mContext, anim);
                    Trace.traceEnd(32L);
                }
            } else {
                switch (transit) {
                    case 1:
                        attr = 0;
                        break;
                    case 2:
                        attr = 1;
                        break;
                    case 3:
                        attr = 2;
                        break;
                    case 4:
                        attr = 3;
                        break;
                }
                if (attr >= 0) {
                    a = this.mWin.getDisplayContent().mAppTransition.loadAnimationAttr(this.mWin.mAttrs, attr, 0);
                }
            }
            if (ProtoLogImpl.isEnabled(ProtoLogGroup.WM_DEBUG_ANIM) && ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                String protoLogParam0 = String.valueOf(this);
                long protoLogParam1 = anim;
                long protoLogParam2 = attr;
                String protoLogParam3 = String.valueOf(a);
                long protoLogParam4 = transit;
                long protoLogParam5 = this.mAttrType;
                String protoLogParam7 = String.valueOf(Debug.getCallers(20));
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, -1318478129, 13588, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2), protoLogParam3, Long.valueOf(protoLogParam4), Long.valueOf(protoLogParam5), Boolean.valueOf(isEntrance), protoLogParam7});
            }
            if (a != null) {
                Trace.traceBegin(32L, "WSA#startAnimation");
                this.mWin.startAnimation(a);
                Trace.traceEnd(32L);
                this.mAnimationIsEntrance = isEntrance;
            }
        } else if (!isImeWindow) {
            this.mWin.cancelAnimation();
        }
        if (!isEntrance && isImeWindow) {
            this.mWin.getDisplayContent().adjustForImeIfNeeded();
        }
        return this.mWin.isAnimating(0, 16);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            windowSurfaceController.dumpDebug(proto, 1146756268034L);
        }
        proto.write(1159641169923L, this.mDrawState);
        this.mSystemDecorRect.dumpDebug(proto, 1146756268036L);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        if (this.mAnimationIsEntrance) {
            pw.print(prefix);
            pw.print(" mAnimationIsEntrance=");
            pw.print(this.mAnimationIsEntrance);
        }
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            windowSurfaceController.dump(pw, prefix, dumpAll);
        }
        if (dumpAll) {
            pw.print(prefix);
            pw.print("mDrawState=");
            pw.print(drawStateToString());
            pw.print(prefix);
            pw.print(" mLastHidden=");
            pw.println(this.mLastHidden);
            pw.print(prefix);
            pw.print("mEnterAnimationPending=" + this.mEnterAnimationPending);
            pw.print(prefix);
            pw.print("mSystemDecorRect=");
            this.mSystemDecorRect.printShortString(pw);
            pw.println();
        }
        if (this.mShownAlpha != 1.0f || this.mAlpha != 1.0f || this.mLastAlpha != 1.0f) {
            pw.print(prefix);
            pw.print("mShownAlpha=");
            pw.print(this.mShownAlpha);
            pw.print(" mAlpha=");
            pw.print(this.mAlpha);
            pw.print(" mLastAlpha=");
            pw.println(this.mLastAlpha);
        }
        if (this.mWin.mGlobalScale != 1.0f) {
            pw.print(prefix);
            pw.print("mGlobalScale=");
            pw.print(this.mWin.mGlobalScale);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("WindowStateAnimator{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.mWin.mAttrs.getTitle());
        sb.append('}');
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getShown() {
        WindowSurfaceController windowSurfaceController = this.mSurfaceController;
        if (windowSurfaceController != null) {
            return windowSurfaceController.getShown();
        }
        return false;
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:16:0x0007 */
    /* JADX DEBUG: Multi-variable search result rejected for r0v2, resolved type: com.android.server.wm.WindowSurfaceController */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v1, types: [com.android.server.wm.WindowSurfaceController] */
    /* JADX WARN: Type inference failed for: r0v4, types: [com.android.server.wm.TranFoldWMCustody] */
    /* JADX WARN: Type inference failed for: r0v5 */
    public void destroySurface(SurfaceControl.Transaction t) {
        t.merge(this.mPostDrawTransaction);
        WindowSurfaceController windowSurfaceController = 0;
        windowSurfaceController = 0;
        try {
            try {
                WindowSurfaceController windowSurfaceController2 = this.mSurfaceController;
                if (windowSurfaceController2 != null) {
                    windowSurfaceController2.destroy(t);
                }
            } catch (RuntimeException e) {
                Slog.w("WindowManager", "Exception thrown when destroying surface " + this + " surface " + this.mSurfaceController + " session " + this.mSession + ": " + e);
            }
        } finally {
            this.mWin.setHasSurface(false);
            this.mSurfaceController = windowSurfaceController;
            this.mDrawState = 0;
            TranFoldWMCustody.instance().destroyWindowStateAnimatorSurface(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl getSurfaceControl() {
        if (!hasSurface()) {
            return null;
        }
        return this.mSurfaceController.mSurfaceControl;
    }
}
