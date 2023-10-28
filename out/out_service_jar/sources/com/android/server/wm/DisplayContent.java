package com.android.server.wm;

import android.app.ActivityManagerInternal;
import android.app.ThunderbackConfig;
import android.content.Context;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.HardwareBuffer;
import android.hardware.display.DisplayManagerInternal;
import android.metrics.LogMaker;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.DisplayUtils;
import android.util.IntArray;
import android.util.RotationUtils;
import android.util.Size;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.proto.ProtoOutputStream;
import android.view.ContentRecordingSession;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.IDisplayWindowInsetsController;
import android.view.ISystemGestureExclusionListener;
import android.view.IWindow;
import android.view.InputChannel;
import android.view.InputDevice;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.InsetsVisibilities;
import android.view.MagnificationSpec;
import android.view.PrivacyIndicatorBounds;
import android.view.RemoteAnimationDefinition;
import android.view.RoundedCorners;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.ViewRootImpl;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.window.IDisplayAreaOrganizer;
import android.window.TransitionRequestInfo;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.ToBooleanFunction;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.UiModeManagerService;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.DisplayArea;
import com.android.server.wm.RootWindowContainer;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.utils.RegionUtils;
import com.android.server.wm.utils.RotationCache;
import com.android.server.wm.utils.WmDisplayCutout;
import com.transsion.hubcore.server.wm.ITranDisplayContent;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import com.transsion.hubcore.sourceconnect.ITranSourceConnectManager;
import com.transsion.server.foldable.TranFoldingScreenController;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DisplayContent extends RootDisplayArea implements WindowManagerPolicy.DisplayContentInfo {
    private static final long FIXED_ROTATION_HIDE_ANIMATION_DEBOUNCE_DELAY_MS = 250;
    static final int FORCE_SCALING_MODE_AUTO = 0;
    static final int FORCE_SCALING_MODE_DISABLED = 1;
    static final int IME_TARGET_CONTROL = 2;
    static final int IME_TARGET_LAYERING = 0;
    private static final String TAG = "WindowManager";
    private static final String[] sRequestPreferDisplayAreaList = {"com.google.android.permissioncontroller"};
    boolean isDefaultDisplay;
    private Set<ActivityRecord> mActiveSizeCompatActivities;
    final ArrayList<RootWindowContainer.SleepToken> mAllSleepTokens;
    final AppTransition mAppTransition;
    final AppTransitionController mAppTransitionController;
    private final Consumer<WindowState> mApplyPostLayoutPolicy;
    private final Consumer<WindowState> mApplySurfaceChangesTransaction;
    private AsyncRotationController mAsyncRotationController;
    final ActivityTaskManagerService mAtmService;
    DisplayCutout mBaseDisplayCutout;
    int mBaseDisplayDensity;
    int mBaseDisplayHeight;
    float mBaseDisplayPhysicalXDpi;
    float mBaseDisplayPhysicalYDpi;
    int mBaseDisplayWidth;
    RoundedCorners mBaseRoundedCorners;
    final ArraySet<WindowContainer> mChangingContainers;
    final float mCloseToSquareMaxAspectRatio;
    final ArraySet<ActivityRecord> mClosingApps;
    private final DisplayMetrics mCompatDisplayMetrics;
    float mCompatibleScreenScale;
    private final Predicate<WindowState> mComputeImeTargetPredicate;
    private ContentRecorder mContentRecorder;
    WindowState mCurrentFocus;
    private int mCurrentOverrideConfigurationChanges;
    PrivacyIndicatorBounds mCurrentPrivacyIndicatorBounds;
    String mCurrentUniqueDisplayId;
    private int mDeferUpdateImeTargetCount;
    private boolean mDeferredRemoval;
    boolean mDettachImeWithActivity;
    final Display mDisplay;
    private IntArray mDisplayAccessUIDs;
    DisplayAreaPolicy mDisplayAreaPolicy;
    private final RotationCache<DisplayCutout, WmDisplayCutout> mDisplayCutoutCache;
    DisplayFrames mDisplayFrames;
    final int mDisplayId;
    private final DisplayInfo mDisplayInfo;
    private final DisplayMetrics mDisplayMetrics;
    private final RotationCache<DisplayCutout, WmDisplayCutout> mDisplayNoLandCutoutCache;
    private final DisplayPolicy mDisplayPolicy;
    private boolean mDisplayReady;
    private final DisplayRotation mDisplayRotation;
    boolean mDisplayScalingDisabled;
    private PhysicalDisplaySwitchTransitionLauncher mDisplaySwitchTransitionLauncher;
    final DockedTaskDividerController mDividerControllerLocked;
    boolean mDontMoveToTop;
    DisplayWindowPolicyControllerHelper mDwpcHelper;
    private final ToBooleanFunction<WindowState> mFindFocusedWindow;
    private ActivityRecord mFixedRotationLaunchingApp;
    final FixedRotationTransitionListener mFixedRotationTransitionListener;
    private int mFocusMultiWindowId;
    ActivityRecord mFocusedApp;
    private boolean mHasFindSecureWindow;
    boolean mHasOverlayWindow;
    boolean mIgnoreDisplayCutout;
    private InsetsControlTarget mImeControlTarget;
    private InputTarget mImeInputTarget;
    private WindowState mImeLayeringTarget;
    ImeScreenshot mImeScreenshot;
    private final ImeContainer mImeWindowsContainer;
    private boolean mInEnsureActivitiesVisible;
    DisplayCutout mInitialDisplayCutout;
    int mInitialDisplayDensity;
    int mInitialDisplayHeight;
    int mInitialDisplayWidth;
    float mInitialPhysicalXDpi;
    float mInitialPhysicalYDpi;
    RoundedCorners mInitialRoundedCorners;
    SurfaceControl mInputMethodSurfaceParent;
    WindowState mInputMethodWindow;
    private InputMonitor mInputMonitor;
    private final InsetsPolicy mInsetsPolicy;
    private final InsetsStateController mInsetsStateController;
    boolean mIsDensityForced;
    private boolean mIsFocusOnThunderbackWindow;
    boolean mIsSizeForced;
    private boolean mLastHasContent;
    private boolean mLastHasFindSecureWindow;
    private InputTarget mLastImeInputTarget;
    private boolean mLastWallpaperVisible;
    boolean mLayoutAndAssignWindowLayersScheduled;
    private boolean mLayoutNeeded;
    int mLayoutSeq;
    private MagnificationSpec mMagnificationSpec;
    private int mMaxUiWidth;
    private MetricsLogger mMetricsLogger;
    int mMinSizeOfResizeableTaskDp;
    final List<IBinder> mNoAnimationNotifyOnTransitionFinished;
    private final ActivityTaskManagerInternal.SleepTokenAcquirer mOffTokenAcquirer;
    final ArraySet<ActivityRecord> mOpeningApps;
    private TaskDisplayArea mOrientationRequestingTaskDisplayArea;
    boolean mOverlayDialog;
    private SurfaceControl mOverlayLayer;
    private ActivityRecord mPendingFinishFixedRotationApp;
    private final Consumer<WindowState> mPerformLayout;
    private final Consumer<WindowState> mPerformLayoutAttached;
    private Point mPhysicalDisplaySize;
    final PinnedTaskController mPinnedTaskController;
    private final PointerEventDispatcher mPointerEventDispatcher;
    private final RotationCache<PrivacyIndicatorBounds, PrivacyIndicatorBounds> mPrivacyIndicatorBoundsCache;
    final DisplayMetrics mRealDisplayMetrics;
    RemoteInsetsControlTarget mRemoteInsetsControlTarget;
    private final IBinder.DeathRecipient mRemoteInsetsDeath;
    private boolean mRemoved;
    private boolean mRemoving;
    private Set<Rect> mRestrictedKeepClearAreas;
    private RootWindowContainer mRootWindowContainer;
    private final RotationCache<RoundedCorners, RoundedCorners> mRoundedCornerCache;
    private boolean mSandboxDisplayApis;
    private final Consumer<WindowState> mScheduleToastTimeout;
    private ScreenRotationAnimation mScreenRotationAnimation;
    private WindowState mSecureWindow;
    private final SurfaceSession mSession;
    final SparseArray<ShellRoot> mShellRoots;
    boolean mSkipAppTransitionAnimation;
    private boolean mSleeping;
    private boolean mSourceConnectDisplay;
    private final Region mSystemGestureExclusion;
    private int mSystemGestureExclusionLimit;
    private final RemoteCallbackList<ISystemGestureExclusionListener> mSystemGestureExclusionListeners;
    private final Region mSystemGestureExclusionUnrestricted;
    private boolean mSystemGestureExclusionWasRestricted;
    final TaskTapPointerEventListener mTapDetector;
    final ArraySet<WindowState> mTapExcludeProvidingWindows;
    final ArrayList<WindowState> mTapExcludedWindows;
    private final Configuration mTempConfig;
    private final ApplySurfaceChangesTransactionState mTmpApplySurfaceChangesTransactionState;
    private final Configuration mTmpConfiguration;
    private final DisplayMetrics mTmpDisplayMetrics;
    private Point mTmpDisplaySize;
    private boolean mTmpInitial;
    private final Rect mTmpRect;
    private final Rect mTmpRect2;
    private final Region mTmpRegion;
    private final TaskForResizePointSearchResult mTmpTaskForResizePointSearchResult;
    private final LinkedList<ActivityRecord> mTmpUpdateAllDrawn;
    private WindowState mTmpWindow;
    private final HashMap<IBinder, WindowToken> mTokenMap;
    private Region mTouchExcludeRegion;
    final UnknownAppVisibilityController mUnknownAppVisibilityController;
    private Set<Rect> mUnrestrictedKeepClearAreas;
    private boolean mUpdateImeRequestedWhileDeferred;
    private boolean mUpdateImeTarget;
    private final Consumer<WindowState> mUpdateWindowsForAnimator;
    boolean mWaitingForConfig;
    WallpaperController mWallpaperController;
    boolean mWallpaperMayChange;
    final ArrayList<WindowState> mWinAddedSinceNullFocus;
    final ArrayList<WindowState> mWinRemovedSinceNullFocus;
    private final float mWindowCornerRadius;
    private SurfaceControl mWindowingLayer;
    int pendingLayoutChanges;
    TaskDisplayArea preferDisplay;
    List<String> topPackages;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface ForceScalingMode {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface InputMethodTarget {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7923lambda$new$0$comandroidserverwmDisplayContent() {
        synchronized (this.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRemoteInsetsControlTarget = null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7924lambda$new$1$comandroidserverwmDisplayContent(WindowState w) {
        WindowStateAnimator winAnimator = w.mWinAnimator;
        ActivityRecord activity = w.mActivityRecord;
        if (winAnimator.mDrawState == 3) {
            if ((activity == null || activity.canShowWindows()) && w.performShowLocked()) {
                this.pendingLayoutChanges |= 8;
                if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                    this.mWmService.mWindowPlacerLocked.debugLayoutRepeats("updateWindowsAndWallpaperLocked 5", this.pendingLayoutChanges);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$2$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7925lambda$new$2$comandroidserverwmDisplayContent(WindowState w) {
        int lostFocusUid = this.mTmpWindow.mOwnerUid;
        Handler handler = this.mWmService.mH;
        if (w.mAttrs.type == 2005 && w.mOwnerUid == lostFocusUid && !handler.hasMessages(52, w)) {
            handler.sendMessageDelayed(handler.obtainMessage(52, w), w.mAttrs.hideTimeoutMilliseconds);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$3$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ boolean m7926lambda$new$3$comandroidserverwmDisplayContent(WindowState w) {
        WindowState windowState;
        WindowState windowState2;
        ActivityRecord focusedApp = this.mFocusedApp;
        if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
            String protoLogParam0 = String.valueOf(w);
            long protoLogParam1 = w.mAttrs.flags;
            boolean protoLogParam2 = w.canReceiveKeys();
            String protoLogParam3 = String.valueOf(w.canReceiveKeysReason(false));
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS, -1142279614, 52, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), protoLogParam3});
        }
        if (w.canReceiveKeys()) {
            if (w.mIsImWindow && w.isChildWindow() && ((windowState2 = this.mImeLayeringTarget) == null || !windowState2.getRequestedVisibility(19))) {
                return false;
            }
            if (w.mAttrs.type != 2012 || (windowState = this.mImeLayeringTarget) == null || windowState.getRequestedVisibility(19) || !this.mImeLayeringTarget.isAnimating(3, 1)) {
                ActivityRecord activity = w.mActivityRecord;
                if (focusedApp == null) {
                    if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                        String protoLogParam02 = String.valueOf(w);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, -87705714, 0, (String) null, new Object[]{protoLogParam02});
                    }
                    this.mTmpWindow = w;
                    return true;
                } else if (!focusedApp.windowsAreFocusable()) {
                    if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                        String protoLogParam03 = String.valueOf(w);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, 1430336882, 0, (String) null, new Object[]{protoLogParam03});
                    }
                    this.mTmpWindow = w;
                    return true;
                } else {
                    if (activity != null && w.mAttrs.type != 3) {
                        if (focusedApp.compareTo((WindowContainer) activity) > 0) {
                            if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                                String protoLogParam04 = String.valueOf(focusedApp);
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, -809771899, 0, (String) null, new Object[]{protoLogParam04});
                            }
                            this.mTmpWindow = null;
                            return true;
                        }
                        TaskFragment parent = activity.getTaskFragment();
                        if (parent != null && parent.isEmbedded()) {
                            Task hostTask = focusedApp.getTask();
                            if (hostTask.isEmbedded()) {
                                hostTask = hostTask.getParent().asTaskFragment().getTask();
                            }
                            if (activity.isDescendantOf(hostTask) && activity.getTaskFragment() != focusedApp.getTaskFragment()) {
                                return false;
                            }
                        }
                    }
                    if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                        String protoLogParam05 = String.valueOf(w);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, -415865166, 0, (String) null, new Object[]{protoLogParam05});
                    }
                    this.mTmpWindow = w;
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$4$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7927lambda$new$4$comandroidserverwmDisplayContent(WindowState w) {
        if (w.mLayoutAttached) {
            return;
        }
        boolean gone = w.isGoneForLayout();
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.v("WindowManager", "1ST PASS " + w + ": gone=" + gone + " mHaveFrame=" + w.mHaveFrame + " config reported=" + w.isLastConfigReportedToClient());
            ActivityRecord activity = w.mActivityRecord;
            if (gone) {
                Slog.v("WindowManager", "  GONE: mViewVisibility=" + w.mViewVisibility + " mRelayoutCalled=" + w.mRelayoutCalled + " visible=" + w.mToken.isVisible() + " visibleRequested=" + (activity != null && activity.mVisibleRequested) + " parentHidden=" + w.isParentWindowHidden());
            } else {
                Slog.v("WindowManager", "  VIS: mViewVisibility=" + w.mViewVisibility + " mRelayoutCalled=" + w.mRelayoutCalled + " visible=" + w.mToken.isVisible() + " visibleRequested=" + (activity != null && activity.mVisibleRequested) + " parentHidden=" + w.isParentWindowHidden());
            }
        }
        if (!gone || !w.mHaveFrame || w.mLayoutNeeded) {
            if (this.mTmpInitial) {
                w.resetContentChanged();
            }
            w.mSurfacePlacementNeeded = true;
            w.mLayoutNeeded = false;
            boolean firstLayout = !w.isLaidOut();
            w.layoutForART();
            getDisplayPolicy().layoutWindowLw(w, null, this.mDisplayFrames);
            w.mLayoutSeq = this.mLayoutSeq;
            if (firstLayout) {
                if (!w.getFrame().isEmpty()) {
                    w.updateLastFrames();
                }
                w.onResizeHandled();
            }
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v("WindowManager", "  LAYOUT: mFrame=" + w.getFrame() + " mParentFrame=" + w.getParentFrame() + " mDisplayFrame=" + w.getDisplayFrame());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$5$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7928lambda$new$5$comandroidserverwmDisplayContent(WindowState w) {
        if (!w.mLayoutAttached) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.v("WindowManager", "2ND PASS " + w + " mHaveFrame=" + w.mHaveFrame + " mViewVisibility=" + w.mViewVisibility + " mRelayoutCalled=" + w.mRelayoutCalled);
        }
        if ((w.mViewVisibility != 8 && w.mRelayoutCalled) || !w.mHaveFrame || w.mLayoutNeeded) {
            if (this.mTmpInitial) {
                w.resetContentChanged();
            }
            w.mSurfacePlacementNeeded = true;
            w.mLayoutNeeded = false;
            w.layoutForART();
            getDisplayPolicy().layoutWindowLw(w, w.getParentWindow(), this.mDisplayFrames);
            w.mLayoutSeq = this.mLayoutSeq;
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v("WindowManager", " LAYOUT: mFrame=" + w.getFrame() + " mParentFrame=" + w.getParentFrame() + " mDisplayFrame=" + w.getDisplayFrame());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$6$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ boolean m7929lambda$new$6$comandroidserverwmDisplayContent(WindowState w) {
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD && this.mUpdateImeTarget) {
            Slog.i("WindowManager", "Checking window @" + w + " fl=0x" + Integer.toHexString(w.mAttrs.flags));
        }
        return w.canBeImeTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$7$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7930lambda$new$7$comandroidserverwmDisplayContent(WindowState w) {
        getDisplayPolicy().applyPostLayoutPolicyLw(w, w.mAttrs, w.getParentWindow(), this.mImeLayeringTarget);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$8$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7931lambda$new$8$comandroidserverwmDisplayContent(WindowState w) {
        WindowSurfacePlacer surfacePlacer = this.mWmService.mWindowPlacerLocked;
        boolean obscuredChanged = w.mObscured != this.mTmpApplySurfaceChangesTransactionState.obscured;
        RootWindowContainer root = this.mWmService.mRoot;
        w.mObscured = this.mTmpApplySurfaceChangesTransactionState.obscured;
        if (!this.mTmpApplySurfaceChangesTransactionState.obscured || ITranDisplayContent.Instance().isInMultiWindow(w)) {
            boolean isDisplayed = w.isDisplayed();
            if (isDisplayed && w.isObscuringDisplay()) {
                root.mObscuringWindow = w;
                this.mTmpApplySurfaceChangesTransactionState.obscured = true;
            }
            boolean displayHasContent = root.handleNotObscuredLocked(w, this.mTmpApplySurfaceChangesTransactionState.obscured, this.mTmpApplySurfaceChangesTransactionState.syswin);
            if (!this.mTmpApplySurfaceChangesTransactionState.displayHasContent && !getDisplayPolicy().isWindowExcludedFromContent(w)) {
                this.mTmpApplySurfaceChangesTransactionState.displayHasContent |= displayHasContent;
            }
            if (w.mHasSurface && isDisplayed) {
                int type = w.mAttrs.type;
                if (type == 2008 || type == 2010 || (type == 2040 && this.mWmService.mPolicy.isKeyguardShowing())) {
                    this.mTmpApplySurfaceChangesTransactionState.syswin = true;
                }
                if (this.mTmpApplySurfaceChangesTransactionState.preferredRefreshRate == 0.0f && w.mAttrs.preferredRefreshRate != 0.0f) {
                    this.mTmpApplySurfaceChangesTransactionState.preferredRefreshRate = w.mAttrs.preferredRefreshRate;
                }
                this.mTmpApplySurfaceChangesTransactionState.preferMinimalPostProcessing |= w.mAttrs.preferMinimalPostProcessing;
                int preferredModeId = getDisplayPolicy().getRefreshRatePolicy().getPreferredModeId(w);
                if (this.mTmpApplySurfaceChangesTransactionState.preferredModeId == 0 && preferredModeId != 0) {
                    ITranDisplayContent.Instance().printDisplayContentAndPreferredModeId(w, "WindowManager", preferredModeId);
                    this.mTmpApplySurfaceChangesTransactionState.preferredModeId = preferredModeId;
                }
                float preferredMinRefreshRate = getDisplayPolicy().getRefreshRatePolicy().getPreferredMinRefreshRate(w);
                if (this.mTmpApplySurfaceChangesTransactionState.preferredMinRefreshRate == 0.0f && preferredMinRefreshRate != 0.0f) {
                    this.mTmpApplySurfaceChangesTransactionState.preferredMinRefreshRate = preferredMinRefreshRate;
                }
                float preferredMaxRefreshRate = getDisplayPolicy().getRefreshRatePolicy().getPreferredMaxRefreshRate(w);
                if (this.mTmpApplySurfaceChangesTransactionState.preferredMaxRefreshRate == 0.0f && preferredMaxRefreshRate != 0.0f) {
                    this.mTmpApplySurfaceChangesTransactionState.preferredMaxRefreshRate = preferredMaxRefreshRate;
                }
            }
        }
        if (obscuredChanged && w.isVisible() && this.mWallpaperController.isWallpaperTarget(w)) {
            this.mWallpaperController.updateWallpaperVisibility();
        }
        markSecureWindow(w);
        w.handleWindowMovedIfNeeded();
        if (ThunderbackConfig.isVersion3()) {
            ITranDisplayContent.Instance().updateCaptionViewStatusIfNeeded(w);
        }
        WindowStateAnimator winAnimator = w.mWinAnimator;
        w.resetContentChanged();
        if (w.mHasSurface) {
            boolean committed = winAnimator.commitFinishDrawingLocked();
            if (this.isDefaultDisplay && committed) {
                if (w.hasWallpaper()) {
                    if (ProtoLogCache.WM_DEBUG_WALLPAPER_enabled) {
                        String protoLogParam0 = String.valueOf(w);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WALLPAPER, 422634333, 0, (String) null, new Object[]{protoLogParam0});
                    }
                    this.mWallpaperMayChange = true;
                    this.pendingLayoutChanges |= 4;
                    if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                        surfacePlacer.debugLayoutRepeats("wallpaper and commitFinishDrawingLocked true", this.pendingLayoutChanges);
                    }
                }
                this.pendingLayoutChanges = ITranDisplayContent.Instance().updatePendingLayoutChanges(this.pendingLayoutChanges, w, "WindowManager");
            }
        }
        ActivityRecord activity = w.mActivityRecord;
        if (activity != null && activity.isVisibleRequested()) {
            activity.updateLetterboxSurface(w);
            boolean updateAllDrawn = activity.updateDrawnWindowStates(w);
            if (updateAllDrawn && !this.mTmpUpdateAllDrawn.contains(activity)) {
                this.mTmpUpdateAllDrawn.add(activity);
            }
        }
        w.updateResizingWindowIfNeeded();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayContent(Display display, RootWindowContainer root) {
        super(root.mWindowManager, "DisplayContent", 0);
        this.mMinSizeOfResizeableTaskDp = -1;
        this.mImeWindowsContainer = new ImeContainer(this.mWmService);
        this.mSkipAppTransitionAnimation = false;
        this.mOpeningApps = new ArraySet<>();
        this.mClosingApps = new ArraySet<>();
        this.mChangingContainers = new ArraySet<>();
        this.mNoAnimationNotifyOnTransitionFinished = new ArrayList();
        this.mTokenMap = new HashMap<>();
        this.mInitialDisplayWidth = 0;
        this.mInitialDisplayHeight = 0;
        this.mInitialDisplayDensity = 0;
        this.mInitialPhysicalXDpi = 0.0f;
        this.mInitialPhysicalYDpi = 0.0f;
        this.mDisplayCutoutCache = new RotationCache<>(new RotationCache.RotationDependentComputation() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda17
            @Override // com.android.server.wm.utils.RotationCache.RotationDependentComputation
            public final Object compute(Object obj, int i) {
                WmDisplayCutout calculateDisplayCutoutForRotationUncached;
                calculateDisplayCutoutForRotationUncached = DisplayContent.this.calculateDisplayCutoutForRotationUncached((DisplayCutout) obj, i);
                return calculateDisplayCutoutForRotationUncached;
            }
        });
        this.mDisplayNoLandCutoutCache = new RotationCache<>(new RotationCache.RotationDependentComputation() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda21
            @Override // com.android.server.wm.utils.RotationCache.RotationDependentComputation
            public final Object compute(Object obj, int i) {
                WmDisplayCutout calculateDisplayNoLandCutoutForRotationUncached;
                calculateDisplayNoLandCutoutForRotationUncached = DisplayContent.this.calculateDisplayNoLandCutoutForRotationUncached((DisplayCutout) obj, i);
                return calculateDisplayNoLandCutoutForRotationUncached;
            }
        });
        this.mRoundedCornerCache = new RotationCache<>(new RotationCache.RotationDependentComputation() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda22
            @Override // com.android.server.wm.utils.RotationCache.RotationDependentComputation
            public final Object compute(Object obj, int i) {
                RoundedCorners calculateRoundedCornersForRotationUncached;
                calculateRoundedCornersForRotationUncached = DisplayContent.this.calculateRoundedCornersForRotationUncached((RoundedCorners) obj, i);
                return calculateRoundedCornersForRotationUncached;
            }
        });
        this.mCurrentPrivacyIndicatorBounds = new PrivacyIndicatorBounds();
        this.mPrivacyIndicatorBoundsCache = new RotationCache<>(new RotationCache.RotationDependentComputation() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda23
            @Override // com.android.server.wm.utils.RotationCache.RotationDependentComputation
            public final Object compute(Object obj, int i) {
                PrivacyIndicatorBounds calculatePrivacyIndicatorBoundsForRotationUncached;
                calculatePrivacyIndicatorBoundsForRotationUncached = DisplayContent.this.calculatePrivacyIndicatorBoundsForRotationUncached((PrivacyIndicatorBounds) obj, i);
                return calculatePrivacyIndicatorBoundsForRotationUncached;
            }
        });
        this.mBaseDisplayWidth = 0;
        this.mBaseDisplayHeight = 0;
        this.mIsSizeForced = false;
        this.mSandboxDisplayApis = true;
        this.mBaseDisplayDensity = 0;
        this.mIsDensityForced = false;
        this.mBaseDisplayPhysicalXDpi = 0.0f;
        this.mBaseDisplayPhysicalYDpi = 0.0f;
        DisplayInfo displayInfo = new DisplayInfo();
        this.mDisplayInfo = displayInfo;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mDisplayMetrics = displayMetrics;
        this.mSystemGestureExclusionListeners = new RemoteCallbackList<>();
        this.mSystemGestureExclusion = new Region();
        this.mSystemGestureExclusionWasRestricted = false;
        this.mSystemGestureExclusionUnrestricted = new Region();
        this.mRestrictedKeepClearAreas = new ArraySet();
        this.mUnrestrictedKeepClearAreas = new ArraySet();
        this.mRealDisplayMetrics = new DisplayMetrics();
        this.mTmpDisplayMetrics = new DisplayMetrics();
        this.mCompatDisplayMetrics = new DisplayMetrics();
        this.mLastWallpaperVisible = false;
        this.mTouchExcludeRegion = new Region();
        this.mTmpRect = new Rect();
        this.mTmpRect2 = new Rect();
        this.mTmpRegion = new Region();
        this.mTmpConfiguration = new Configuration();
        this.mTapExcludedWindows = new ArrayList<>();
        this.mTapExcludeProvidingWindows = new ArraySet<>();
        this.mTmpUpdateAllDrawn = new LinkedList<>();
        this.mTmpTaskForResizePointSearchResult = new TaskForResizePointSearchResult();
        this.mTmpApplySurfaceChangesTransactionState = new ApplySurfaceChangesTransactionState();
        this.mDisplayReady = false;
        this.mWallpaperMayChange = false;
        this.mSession = new SurfaceSession();
        this.mCurrentFocus = null;
        this.mFocusedApp = null;
        this.mOrientationRequestingTaskDisplayArea = null;
        FixedRotationTransitionListener fixedRotationTransitionListener = new FixedRotationTransitionListener();
        this.mFixedRotationTransitionListener = fixedRotationTransitionListener;
        this.mWinAddedSinceNullFocus = new ArrayList<>();
        this.mWinRemovedSinceNullFocus = new ArrayList<>();
        this.mLayoutSeq = 0;
        this.mShellRoots = new SparseArray<>();
        this.mRemoteInsetsControlTarget = null;
        this.mRemoteInsetsDeath = new IBinder.DeathRecipient() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda24
            @Override // android.os.IBinder.DeathRecipient
            public final void binderDied() {
                DisplayContent.this.m7923lambda$new$0$comandroidserverwmDisplayContent();
            }
        };
        this.mDisplayAccessUIDs = new IntArray();
        this.mAllSleepTokens = new ArrayList<>();
        this.mActiveSizeCompatActivities = new ArraySet();
        this.mTmpDisplaySize = new Point();
        this.mTempConfig = new Configuration();
        this.mInEnsureActivitiesVisible = false;
        this.mUpdateWindowsForAnimator = new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda25
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7924lambda$new$1$comandroidserverwmDisplayContent((WindowState) obj);
            }
        };
        this.mScheduleToastTimeout = new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda26
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7925lambda$new$2$comandroidserverwmDisplayContent((WindowState) obj);
            }
        };
        this.mFindFocusedWindow = new ToBooleanFunction() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda27
            public final boolean apply(Object obj) {
                return DisplayContent.this.m7926lambda$new$3$comandroidserverwmDisplayContent((WindowState) obj);
            }
        };
        this.mPerformLayout = new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda28
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7927lambda$new$4$comandroidserverwmDisplayContent((WindowState) obj);
            }
        };
        this.mPerformLayoutAttached = new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda29
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7928lambda$new$5$comandroidserverwmDisplayContent((WindowState) obj);
            }
        };
        this.mComputeImeTargetPredicate = new Predicate() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda18
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DisplayContent.this.m7929lambda$new$6$comandroidserverwmDisplayContent((WindowState) obj);
            }
        };
        this.mApplyPostLayoutPolicy = new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda19
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7930lambda$new$7$comandroidserverwmDisplayContent((WindowState) obj);
            }
        };
        this.mApplySurfaceChangesTransaction = new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda20
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7931lambda$new$8$comandroidserverwmDisplayContent((WindowState) obj);
            }
        };
        this.mFocusMultiWindowId = -1;
        this.topPackages = new ArrayList();
        this.mSourceConnectDisplay = false;
        this.mHasFindSecureWindow = false;
        this.mLastHasFindSecureWindow = false;
        this.mSecureWindow = null;
        if (this.mWmService.mRoot.getDisplayContent(display.getDisplayId()) != null) {
            throw new IllegalArgumentException("Display with ID=" + display.getDisplayId() + " already exists=" + this.mWmService.mRoot.getDisplayContent(display.getDisplayId()) + " new=" + display);
        }
        this.mRootWindowContainer = root;
        this.mAtmService = this.mWmService.mAtmService;
        this.mDisplay = display;
        int displayId = display.getDisplayId();
        this.mDisplayId = displayId;
        this.mCurrentUniqueDisplayId = display.getUniqueId();
        TranFoldWMCustody.instance().updateCurrentUniqueDisplayId(this);
        this.mOffTokenAcquirer = this.mRootWindowContainer.mDisplayOffTokenAcquirer;
        this.mWallpaperController = new WallpaperController(this.mWmService, this);
        display.getDisplayInfo(displayInfo);
        display.getMetrics(displayMetrics);
        this.mSystemGestureExclusionLimit = (this.mWmService.mConstants.mSystemGestureExclusionLimitDp * displayMetrics.densityDpi) / 160;
        this.isDefaultDisplay = displayId == 0;
        InsetsStateController insetsStateController = new InsetsStateController(this);
        this.mInsetsStateController = insetsStateController;
        this.mDisplayFrames = new DisplayFrames(displayId, insetsStateController.getRawInsetsState(), displayInfo, calculateDisplayCutoutForRotation(displayInfo.rotation), calculateRoundedCornersForRotation(displayInfo.rotation), calculatePrivacyIndicatorBoundsForRotation(displayInfo.rotation));
        initializeDisplayBaseInfo();
        AppTransition appTransition = new AppTransition(this.mWmService.mContext, this.mWmService, this);
        this.mAppTransition = appTransition;
        appTransition.registerListenerLocked(this.mWmService.mActivityManagerAppTransitionNotifier);
        appTransition.registerListenerLocked(fixedRotationTransitionListener);
        this.mAppTransitionController = new AppTransitionController(this.mWmService, this);
        this.mTransitionController.registerLegacyListener(fixedRotationTransitionListener);
        this.mUnknownAppVisibilityController = new UnknownAppVisibilityController(this.mWmService, this);
        this.mDisplaySwitchTransitionLauncher = new PhysicalDisplaySwitchTransitionLauncher(this, this.mTransitionController);
        InputChannel inputChannel = this.mWmService.mInputManager.monitorInput("PointerEventDispatcher" + displayId, displayId);
        this.mPointerEventDispatcher = new PointerEventDispatcher(inputChannel);
        TaskTapPointerEventListener taskTapPointerEventListener = new TaskTapPointerEventListener(this.mWmService, this);
        this.mTapDetector = taskTapPointerEventListener;
        registerPointerEventListener(taskTapPointerEventListener);
        registerPointerEventListener(this.mWmService.mMousePositionTracker);
        registerPointerEventListenerMagellan(display);
        if (this.mWmService.mAtmService.getRecentTasks() != null) {
            registerPointerEventListener(this.mWmService.mAtmService.getRecentTasks().getInputListener());
        }
        DisplayPolicy displayPolicy = new DisplayPolicy(this.mWmService, this);
        this.mDisplayPolicy = displayPolicy;
        this.mDisplayRotation = new DisplayRotation(this.mWmService, this);
        this.mCloseToSquareMaxAspectRatio = this.mWmService.mContext.getResources().getFloat(17105069);
        if (this.isDefaultDisplay) {
            this.mWmService.mPolicy.setDefaultDisplay(this);
        }
        if (this.mWmService.mDisplayReady) {
            displayPolicy.onConfigurationChanged();
        }
        if (this.mWmService.mSystemReady) {
            displayPolicy.systemReady();
        }
        this.mWindowCornerRadius = displayPolicy.getWindowCornerRadius();
        this.mDividerControllerLocked = new DockedTaskDividerController(this);
        this.mPinnedTaskController = new PinnedTaskController(this.mWmService, this);
        SurfaceControl.Transaction pendingTransaction = getPendingTransaction();
        configureSurfaces(pendingTransaction);
        pendingTransaction.apply();
        onDisplayChanged(this);
        updateDisplayAreaOrganizers();
        this.mInputMonitor = new InputMonitor(this.mWmService, this);
        this.mInsetsPolicy = new InsetsPolicy(insetsStateController, this);
        this.mMinSizeOfResizeableTaskDp = getMinimalTaskSizeDp();
        if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
            Slog.v("WindowManager", "Creating display=" + display);
        }
        this.mWmService.mDisplayWindowSettings.applySettingsToDisplayLocked(this);
    }

    @Override // com.android.server.wm.WindowContainer
    void migrateToNewSurfaceControl(SurfaceControl.Transaction t) {
        t.remove(this.mSurfaceControl);
        this.mLastSurfacePosition.set(0, 0);
        this.mLastDeltaRotation = 0;
        configureSurfaces(t);
        for (int i = 0; i < this.mChildren.size(); i++) {
            SurfaceControl sc = ((DisplayArea) this.mChildren.get(i)).getSurfaceControl();
            if (sc != null) {
                t.reparent(sc, this.mSurfaceControl);
            }
        }
        scheduleAnimation();
    }

    private void configureSurfaces(SurfaceControl.Transaction transaction) {
        SurfaceControl.Builder b = this.mWmService.makeSurfaceBuilder(this.mSession).setOpaque(true).setContainerLayer().setCallsite("DisplayContent");
        this.mSurfaceControl = b.setName(getName()).setContainerLayer().build();
        if (this.mDisplayAreaPolicy == null) {
            this.mDisplayAreaPolicy = this.mWmService.getDisplayAreaPolicyProvider().instantiate(this.mWmService, this, this, this.mImeWindowsContainer);
        }
        List<DisplayArea<? extends WindowContainer>> areas = this.mDisplayAreaPolicy.getDisplayAreas(4);
        DisplayArea<?> area = areas.size() == 1 ? areas.get(0) : null;
        if (area != null && area.getParent() == this) {
            SurfaceControl surfaceControl = area.mSurfaceControl;
            this.mWindowingLayer = surfaceControl;
            transaction.reparent(surfaceControl, this.mSurfaceControl);
        } else {
            this.mWindowingLayer = this.mSurfaceControl;
            this.mSurfaceControl = b.setName("RootWrapper").build();
            transaction.reparent(this.mWindowingLayer, this.mSurfaceControl).show(this.mWindowingLayer);
        }
        SurfaceControl surfaceControl2 = this.mOverlayLayer;
        if (surfaceControl2 == null) {
            this.mOverlayLayer = b.setName("Display Overlays").setParent(this.mSurfaceControl).build();
        } else {
            transaction.reparent(surfaceControl2, this.mSurfaceControl);
        }
        transaction.setLayer(this.mSurfaceControl, 0).setLayerStack(this.mSurfaceControl, this.mDisplayId).show(this.mSurfaceControl).setLayer(this.mOverlayLayer, Integer.MAX_VALUE).show(this.mOverlayLayer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isReady() {
        return this.mWmService.mDisplayReady && this.mDisplayReady;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDisplayId() {
        return this.mDisplayId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getWindowCornerRadius() {
        return this.mWindowCornerRadius;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowToken getWindowToken(IBinder binder) {
        return this.mTokenMap.get(binder);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getActivityRecord(IBinder binder) {
        WindowToken token = getWindowToken(binder);
        if (token == null) {
            return null;
        }
        return token.asActivityRecord();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addWindowToken(IBinder binder, WindowToken token) {
        DisplayContent dc = this.mWmService.mRoot.getWindowTokenDisplay(token);
        if (dc != null) {
            throw new IllegalArgumentException("Can't map token=" + token + " to display=" + getName() + " already mapped to display=" + dc + " tokens=" + dc.mTokenMap);
        }
        if (binder == null) {
            throw new IllegalArgumentException("Can't map token=" + token + " to display=" + getName() + " binder is null");
        }
        if (token == null) {
            throw new IllegalArgumentException("Can't map null token to display=" + getName() + " binder=" + binder);
        }
        this.mTokenMap.put(binder, token);
        if (token.asActivityRecord() == null) {
            token.mDisplayContent = this;
            DisplayArea.Tokens da = findAreaForToken(token).asTokens();
            da.addChild(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowToken removeWindowToken(IBinder binder, boolean animateExit) {
        WindowToken token = this.mTokenMap.remove(binder);
        if (token != null && token.asActivityRecord() == null) {
            token.setExiting(animateExit);
        }
        return token;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl addShellRoot(IWindow client, int shellRootLayer) {
        ShellRoot root = this.mShellRoots.get(shellRootLayer);
        if (root != null) {
            if (root.getClient() == client) {
                return root.getSurfaceControl();
            }
            root.clear();
            this.mShellRoots.remove(shellRootLayer);
        }
        ShellRoot root2 = new ShellRoot(client, this, shellRootLayer);
        SurfaceControl rootLeash = root2.getSurfaceControl();
        if (rootLeash == null) {
            root2.clear();
            return null;
        }
        this.mShellRoots.put(shellRootLayer, root2);
        SurfaceControl out = new SurfaceControl(rootLeash, "DisplayContent.addShellRoot");
        return out;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeShellRoot(int windowType) {
        synchronized (this.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ShellRoot root = this.mShellRoots.get(windowType);
                if (root == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                root.clear();
                this.mShellRoots.remove(windowType);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRemoteInsetsController(IDisplayWindowInsetsController controller) {
        RemoteInsetsControlTarget remoteInsetsControlTarget = this.mRemoteInsetsControlTarget;
        if (remoteInsetsControlTarget != null) {
            remoteInsetsControlTarget.mRemoteInsetsController.asBinder().unlinkToDeath(this.mRemoteInsetsDeath, 0);
            this.mRemoteInsetsControlTarget = null;
        }
        if (controller != null) {
            try {
                controller.asBinder().linkToDeath(this.mRemoteInsetsDeath, 0);
                this.mRemoteInsetsControlTarget = new RemoteInsetsControlTarget(controller);
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reParentWindowToken(WindowToken token) {
        DisplayContent prevDc = token.getDisplayContent();
        if (prevDc == this) {
            return;
        }
        if (prevDc != null && prevDc.mTokenMap.remove(token.token) != null && token.asActivityRecord() == null) {
            token.getParent().removeChild(token);
        }
        addWindowToken(token.token, token);
        if (this.mWmService.mAccessibilityController.hasCallbacks()) {
            int prevDisplayId = prevDc != null ? prevDc.getDisplayId() : -1;
            this.mWmService.mAccessibilityController.onSomeWindowResizedOrMoved(prevDisplayId, getDisplayId());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAppToken(IBinder binder) {
        WindowToken token = removeWindowToken(binder, true);
        if (token == null) {
            Slog.w("WindowManager", "removeAppToken: Attempted to remove non-existing token: " + binder);
            return;
        }
        ActivityRecord activity = token.asActivityRecord();
        if (activity == null) {
            Slog.w("WindowManager", "Attempted to remove non-App token: " + binder + " token=" + token);
            return;
        }
        activity.onRemovedFromDisplay();
        Iterator<ActivityRecord> it = this.mOpeningApps.iterator();
        while (it.hasNext()) {
            ActivityRecord ar = it.next();
            if (ar.finishing && ar.shortComponentName.equals(activity.shortComponentName) && ar.getTaskFragment() != null && ar.getTaskFragment().isEmbedding()) {
                Slog.d("WindowManager", "DisplayContent::removeAppToken() ar=" + ar);
                this.mOpeningApps.remove(ar);
            }
        }
        if (activity == this.mFixedRotationLaunchingApp) {
            activity.finishFixedRotationTransform();
            setFixedRotationLaunchingAppUnchecked(null);
        }
    }

    @Override // com.android.server.policy.WindowManagerPolicy.DisplayContentInfo
    public Display getDisplay() {
        return this.mDisplay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayInfo getDisplayInfo() {
        return this.mDisplayInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayMetrics getDisplayMetrics() {
        return this.mDisplayMetrics;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayPolicy getDisplayPolicy() {
        return this.mDisplayPolicy;
    }

    @Override // com.android.server.policy.WindowManagerPolicy.DisplayContentInfo
    public DisplayRotation getDisplayRotation() {
        return this.mDisplayRotation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInsetProvider(int type, WindowContainer win, TriConsumer<DisplayFrames, WindowContainer, Rect> frameProvider) {
        setInsetProvider(type, win, frameProvider, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInsetProvider(int type, WindowContainer win, TriConsumer<DisplayFrames, WindowContainer, Rect> frameProvider, TriConsumer<DisplayFrames, WindowContainer, Rect> imeFrameProvider) {
        this.mInsetsStateController.getSourceProvider(type).setWindowContainer(win, frameProvider, imeFrameProvider);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsStateController getInsetsStateController() {
        return this.mInsetsStateController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsPolicy getInsetsPolicy() {
        return this.mInsetsPolicy;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRotation() {
        return this.mDisplayRotation.getRotation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLastOrientation() {
        return this.mDisplayRotation.getLastOrientation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerRemoteAnimations(RemoteAnimationDefinition definition) {
        this.mAppTransitionController.registerRemoteAnimations(definition);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reconfigureDisplayLocked() {
        if (!isReady()) {
            return;
        }
        configureDisplayPolicy();
        setLayoutNeeded();
        boolean configChanged = updateOrientation();
        if (configChanged | updateScreenConfiguration()) {
            sendNewConfiguration();
        }
        this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateScreenConfiguration() {
        Configuration currentDisplayConfig = getConfiguration();
        this.mTmpConfiguration.setTo(currentDisplayConfig);
        computeScreenConfiguration(this.mTmpConfiguration);
        int changes = currentDisplayConfig.diff(this.mTmpConfiguration);
        if (changes == 0) {
            return false;
        }
        this.mWaitingForConfig = true;
        if (this.mTransitionController.isShellTransitionsEnabled()) {
            requestChangeTransitionIfNeeded(changes, null);
        } else if (this.mLastHasContent) {
            this.mWmService.startFreezingDisplay(0, 0, this);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendNewConfiguration() {
        if (!isReady() || this.mDisplayRotation.isWaitingForRemoteRotation()) {
            return;
        }
        boolean configUpdated = updateDisplayOverrideConfigurationLocked();
        if (configUpdated) {
            return;
        }
        clearFixedRotationLaunchingApp();
        if (this.mWaitingForConfig) {
            this.mWaitingForConfig = false;
            this.mWmService.mLastFinishedFreezeSource = "config-unchanged";
            setLayoutNeeded();
            this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    boolean onDescendantOrientationChanged(WindowContainer requestingContainer) {
        Configuration config = updateOrientation(requestingContainer, false);
        boolean handled = handlesOrientationChangeFromDescendant();
        if (config == null) {
            return handled;
        }
        if (handled && (requestingContainer instanceof ActivityRecord)) {
            ActivityRecord activityRecord = (ActivityRecord) requestingContainer;
            boolean kept = updateDisplayOverrideConfigurationLocked(config, activityRecord, false, null);
            activityRecord.frozenBeforeDestroy = true;
            if (!kept) {
                this.mRootWindowContainer.resumeFocusedTasksTopActivities();
            }
        } else {
            updateDisplayOverrideConfigurationLocked(config, null, false, null);
        }
        return handled;
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    boolean handlesOrientationChangeFromDescendant() {
        return (getIgnoreOrientationRequest() || getDisplayRotation().isFixedToUserRotation()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateOrientation() {
        return updateOrientation(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Configuration updateOrientation(WindowContainer<?> freezeDisplayWindow, boolean forceUpdate) {
        ActivityRecord activity;
        if (!this.mDisplayReady) {
            return null;
        }
        if (updateOrientation(forceUpdate)) {
            if (freezeDisplayWindow != null && !this.mWmService.mRoot.mOrientationChangeComplete && (activity = freezeDisplayWindow.asActivityRecord()) != null && activity.mayFreezeScreenLocked()) {
                activity.startFreezingScreen();
            }
            Configuration config = new Configuration();
            computeScreenConfiguration(config);
            return config;
        } else if (this.mTransitionController.isCollecting(this) || this.mDisplayRotation.isWaitingForRemoteRotation()) {
            return null;
        } else {
            Configuration currentConfig = getRequestedOverrideConfiguration();
            this.mTmpConfiguration.unset();
            this.mTmpConfiguration.updateFrom(currentConfig);
            computeScreenConfiguration(this.mTmpConfiguration);
            if (currentConfig.diff(this.mTmpConfiguration) == 0) {
                return null;
            }
            this.mWaitingForConfig = true;
            setLayoutNeeded();
            this.mDisplayRotation.prepareNormalRotationAnimation();
            return new Configuration(this.mTmpConfiguration);
        }
    }

    private int getMinimalTaskSizeDp() {
        Context displayConfigurationContext = this.mAtmService.mContext.createConfigurationContext(getConfiguration());
        float minimalSize = displayConfigurationContext.getResources().getDimension(17105185);
        if (Double.compare(this.mDisplayMetrics.density, 0.0d) == 0) {
            throw new IllegalArgumentException("Display with ID=" + getDisplayId() + "has invalid DisplayMetrics.density= 0.0");
        }
        return (int) (minimalSize / this.mDisplayMetrics.density);
    }

    private boolean updateOrientation(boolean forceUpdate) {
        int orientation = getOrientation();
        WindowContainer orientationSource = getLastOrientationSource();
        ActivityRecord r = orientationSource != null ? orientationSource.asActivityRecord() : null;
        if (r != null) {
            Task task = r.getTask();
            if (task != null && orientation != task.mLastReportedRequestedOrientation) {
                task.mLastReportedRequestedOrientation = orientation;
                this.mAtmService.getTaskChangeNotificationController().notifyTaskRequestedOrientationChanged(task.mTaskId, orientation);
            }
            if (handleTopActivityLaunchingInDifferentOrientation(r, true)) {
                return false;
            }
        }
        TaskDisplayArea taskDisplayArea = orientationSource != null ? orientationSource.asTaskDisplayArea() : null;
        if (orientation == 1 && r == null && taskDisplayArea != null && orientationSource.getTopMostActivity() != null && orientationSource.getTopMostActivity().getTaskFragment() != null && orientationSource.getTopMostActivity().getTaskFragment().isEmbedding()) {
            Slog.w("WindowManager", "wm_rotation wrong, os wxd reset orientation to -1!!!");
            orientation = -1;
        }
        return this.mDisplayRotation.updateOrientation(orientation, forceUpdate);
    }

    @Override // com.android.server.wm.WindowContainer
    boolean isSyncFinished() {
        return !this.mDisplayRotation.isWaitingForRemoteRotation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int rotationForActivityInDifferentOrientation(ActivityRecord r) {
        int currentRotation;
        int rotation;
        if (this.mTransitionController.useShellTransitionsRotation() || !WindowManagerService.ENABLE_FIXED_ROTATION_TRANSFORM || r.inMultiWindowMode() || r.getRequestedConfigurationOrientation(true) == getConfiguration().orientation || (rotation = this.mDisplayRotation.rotationForOrientation(r.getRequestedOrientation(), (currentRotation = getRotation()))) == currentRotation) {
            return -1;
        }
        return rotation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleTopActivityLaunchingInDifferentOrientation(ActivityRecord r, boolean checkOpening) {
        int rotation;
        boolean shouldPreferRotationScreen = ITranDisplayContent.Instance().shouldPreferRotationScreen(this.isDefaultDisplay, this.mAppTransition);
        boolean isThunderBackTransition = ITranDisplayContent.Instance().isThunderBackTransition(this.mAppTransition);
        if (WindowManagerService.ENABLE_FIXED_ROTATION_TRANSFORM && !r.isFinishingFixedRotationTransform()) {
            if (r.hasFixedRotationTransform()) {
                return true;
            }
            if (ThunderbackConfig.isVersion3()) {
                if (!r.occludesParent() || (r.isReportedDrawn() && shouldPreferRotationScreen)) {
                    return false;
                }
            } else if ((!r.occludesParent() || r.isReportedDrawn()) && !isThunderBackTransition) {
                return false;
            }
            if (checkOpening) {
                if (this.mTransitionController.isShellTransitionsEnabled()) {
                    if (!this.mTransitionController.isCollecting(r)) {
                        return false;
                    }
                } else if (!this.mAppTransition.isTransitionSet() || !this.mOpeningApps.contains(r)) {
                    return false;
                }
                if (r.isState(ActivityRecord.State.RESUMED) && !r.getRootTask().mInResumeTopActivity && shouldPreferRotationScreen) {
                    return false;
                }
            } else if (r != topRunningActivity()) {
                return false;
            }
            boolean checkActivityType = true;
            if (ThunderbackConfig.isVersion4()) {
                checkActivityType = r.getActivityType() != 2;
            }
            if ((this.mLastWallpaperVisible && r.windowsCanBeWallpaperTarget() && checkActivityType && this.mFixedRotationTransitionListener.mAnimatingRecents == null) || (rotation = rotationForActivityInDifferentOrientation(r)) == -1 || !r.getDisplayArea().matchParentBounds()) {
                return false;
            }
            setFixedRotationLaunchingApp(r, rotation);
            if (ThunderbackConfig.isVersion4()) {
                ITranDisplayContent.Instance().hookFixedRotationLaunchOrDefaultDisplayFixRotation(r, this.isDefaultDisplay, isThunderBackTransition, rotation, getRotation(), this.mAppTransition, (getTopMultiDisplayArea() == null || rotation == getRotation()) ? false : true);
            } else {
                ITranDisplayContent.Instance().hookFixedRotationLaunchOrDefaultDisplayFixRotation(r, this.isDefaultDisplay, isThunderBackTransition, rotation, getRotation(), this.mAppTransition, (getMultiDisplayArea() == null || rotation == getRotation()) ? false : true);
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean mayImeShowOnLaunchingActivity(ActivityRecord r) {
        WindowState win = r.findMainWindow();
        if (win == null) {
            return false;
        }
        int softInputMode = win.mAttrs.softInputMode;
        switch (softInputMode & 15) {
            case 2:
            case 3:
                return false;
            default:
                return r.mLastImeShown;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasTopFixedRotationLaunchingApp() {
        ActivityRecord activityRecord = this.mFixedRotationLaunchingApp;
        return (activityRecord == null || activityRecord == this.mFixedRotationTransitionListener.mAnimatingRecents) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFixedRotationLaunchingApp(ActivityRecord r) {
        return this.mFixedRotationLaunchingApp == r;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AsyncRotationController getAsyncRotationController() {
        return this.mAsyncRotationController;
    }

    void setFixedRotationLaunchingAppUnchecked(ActivityRecord r) {
        setFixedRotationLaunchingAppUnchecked(r, -1);
    }

    void setFixedRotationLaunchingAppUnchecked(ActivityRecord r, int rotation) {
        ActivityRecord activityRecord = this.mFixedRotationLaunchingApp;
        if (activityRecord == null && r != null) {
            this.mWmService.mDisplayNotificationController.dispatchFixedRotationStarted(this, rotation);
            boolean shouldDebounce = r == this.mFixedRotationTransitionListener.mAnimatingRecents || this.mTransitionController.isTransientLaunch(r);
            startAsyncRotation(shouldDebounce);
        } else if (activityRecord != null && r == null) {
            this.mWmService.mDisplayNotificationController.dispatchFixedRotationFinished(this);
            if (!this.mTransitionController.isCollecting(this)) {
                finishAsyncRotationIfPossible();
            }
        }
        this.mFixedRotationLaunchingApp = r;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFixedRotationLaunchingApp(ActivityRecord r, int rotation) {
        WindowToken prevRotatedLaunchingApp = this.mFixedRotationLaunchingApp;
        if (prevRotatedLaunchingApp == r && r.getWindowConfiguration().getRotation() == rotation) {
            return;
        }
        if (prevRotatedLaunchingApp != null && prevRotatedLaunchingApp.getWindowConfiguration().getRotation() == rotation && (prevRotatedLaunchingApp.isAnimating(3) || this.mTransitionController.inTransition(prevRotatedLaunchingApp))) {
            r.linkFixedRotationTransform(prevRotatedLaunchingApp);
            if (r != this.mFixedRotationTransitionListener.mAnimatingRecents) {
                setFixedRotationLaunchingAppUnchecked(r, rotation);
                return;
            }
            return;
        }
        if (!r.hasFixedRotationTransform()) {
            startFixedRotationTransform(r, rotation);
        }
        setFixedRotationLaunchingAppUnchecked(r, rotation);
        if (prevRotatedLaunchingApp != null) {
            prevRotatedLaunchingApp.finishFixedRotationTransform();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void continueUpdateOrientationForDiffOrienLaunchingApp() {
        if (this.mFixedRotationLaunchingApp == null || this.mPinnedTaskController.shouldDeferOrientationChange()) {
            return;
        }
        if (this.mDisplayRotation.updateOrientation(getOrientation(), false)) {
            sendNewConfiguration();
        } else if (this.mDisplayRotation.isWaitingForRemoteRotation()) {
        } else {
            clearFixedRotationLaunchingApp();
        }
    }

    private void clearFixedRotationLaunchingApp() {
        ActivityRecord activityRecord = this.mFixedRotationLaunchingApp;
        if (activityRecord == null) {
            return;
        }
        activityRecord.finishFixedRotationTransform();
        setFixedRotationLaunchingAppUnchecked(null);
    }

    private void startFixedRotationTransform(WindowToken token, int rotation) {
        this.mTmpConfiguration.unset();
        DisplayInfo info = computeScreenConfiguration(this.mTmpConfiguration, rotation);
        WmDisplayCutout cutout = calculateDisplayCutoutForRotation(rotation);
        RoundedCorners roundedCorners = calculateRoundedCornersForRotation(rotation);
        PrivacyIndicatorBounds indicatorBounds = calculatePrivacyIndicatorBoundsForRotation(rotation);
        DisplayFrames displayFrames = new DisplayFrames(this.mDisplayId, new InsetsState(), info, cutout, roundedCorners, indicatorBounds);
        token.applyFixedRotationTransform(info, displayFrames, this.mTmpConfiguration);
    }

    void rotateInDifferentOrientationIfNeeded(ActivityRecord activityRecord) {
        int rotation = rotationForActivityInDifferentOrientation(activityRecord);
        if (rotation != -1) {
            startFixedRotationTransform(activityRecord, rotation);
        }
    }

    private boolean isRotationChanging() {
        return this.mDisplayRotation.getRotation() != getWindowConfiguration().getRotation();
    }

    private void startAsyncRotationIfNeeded() {
        if (isRotationChanging()) {
            startAsyncRotation(false);
        }
    }

    private boolean startAsyncRotation(boolean shouldDebounce) {
        if (shouldDebounce) {
            this.mWmService.mH.postDelayed(new Runnable() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda47
                @Override // java.lang.Runnable
                public final void run() {
                    DisplayContent.this.m7937lambda$startAsyncRotation$9$comandroidserverwmDisplayContent();
                }
            }, FIXED_ROTATION_HIDE_ANIMATION_DEBOUNCE_DELAY_MS);
            return false;
        } else if (this.mAsyncRotationController != null) {
            return false;
        } else {
            AsyncRotationController asyncRotationController = new AsyncRotationController(this);
            this.mAsyncRotationController = asyncRotationController;
            asyncRotationController.start();
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$startAsyncRotation$9$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7937lambda$startAsyncRotation$9$comandroidserverwmDisplayContent() {
        synchronized (this.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mFixedRotationLaunchingApp != null && startAsyncRotation(false)) {
                    getPendingTransaction().apply();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishAsyncRotationIfPossible() {
        AsyncRotationController controller = this.mAsyncRotationController;
        if (controller != null && !this.mDisplayRotation.hasSeamlessRotatingWindow()) {
            controller.completeAll();
            this.mAsyncRotationController = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishAsyncRotation(WindowToken windowToken) {
        AsyncRotationController controller = this.mAsyncRotationController;
        if (controller != null && controller.completeRotation(windowToken)) {
            this.mAsyncRotationController = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldSyncRotationChange(WindowState w) {
        AsyncRotationController controller = this.mAsyncRotationController;
        return controller == null || !controller.isAsync(w);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyInsetsChanged(Consumer<WindowState> dispatchInsetsChanged) {
        InsetsState rotatedState;
        ActivityRecord activityRecord = this.mFixedRotationLaunchingApp;
        if (activityRecord != null && (rotatedState = activityRecord.getFixedRotationTransformInsetsState()) != null) {
            InsetsState state = this.mInsetsStateController.getRawInsetsState();
            for (int i = 0; i < 24; i++) {
                InsetsSource source = state.peekSource(i);
                if (source != null) {
                    rotatedState.setSourceVisible(i, source.isVisible());
                }
            }
        }
        boolean isImeShow = true;
        forAllWindows(dispatchInsetsChanged, true);
        RemoteInsetsControlTarget remoteInsetsControlTarget = this.mRemoteInsetsControlTarget;
        if (remoteInsetsControlTarget != null) {
            remoteInsetsControlTarget.notifyInsetsChanged();
        }
        if (this.mWmService.mAccessibilityController.hasCallbacks()) {
            InsetsControlTarget insetsControlTarget = this.mImeControlTarget;
            isImeShow = (insetsControlTarget == null || !insetsControlTarget.getRequestedVisibility(19)) ? false : false;
            this.mWmService.mAccessibilityController.updateImeVisibilityIfNeeded(this.mDisplayId, isImeShow);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateRotationUnchecked() {
        return this.mDisplayRotation.updateRotationUnchecked(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canShowTasksInRecents() {
        DisplayWindowPolicyControllerHelper displayWindowPolicyControllerHelper = this.mDwpcHelper;
        if (displayWindowPolicyControllerHelper == null) {
            return true;
        }
        return displayWindowPolicyControllerHelper.canShowTasksInRecents();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: applyRotation */
    public void m7916xf847ed3f(final int oldRotation, final int rotation) {
        this.mDisplayRotation.applyCurrentRotation(rotation);
        final boolean rotateSeamlessly = false;
        boolean shellTransitions = this.mTransitionController.getTransitionPlayer() != null;
        if (this.mDisplayRotation.isRotatingSeamlessly() && !shellTransitions) {
            rotateSeamlessly = true;
        }
        final SurfaceControl.Transaction transaction = shellTransitions ? getSyncTransaction() : getPendingTransaction();
        ScreenRotationAnimation screenRotationAnimation = rotateSeamlessly ? null : getRotationAnimation();
        updateDisplayAndOrientation(getConfiguration().uiMode, null);
        if (screenRotationAnimation != null && screenRotationAnimation.hasScreenshot()) {
            screenRotationAnimation.setRotation(transaction, rotation);
        }
        if (!shellTransitions) {
            forAllWindows(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda32
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((WindowState) obj).seamlesslyRotateIfAllowed(transaction, oldRotation, rotation, rotateSeamlessly);
                }
            }, true);
            this.mPinnedTaskController.startSeamlessRotationIfNeeded(transaction, oldRotation, rotation);
        }
        this.mWmService.mDisplayManagerInternal.performTraversal(transaction);
        scheduleAnimation();
        forAllWindows(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda33
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.lambda$applyRotation$11(rotateSeamlessly, (WindowState) obj);
            }
        }, true);
        for (int i = this.mWmService.mRotationWatchers.size() - 1; i >= 0; i--) {
            WindowManagerService.RotationWatcher rotationWatcher = this.mWmService.mRotationWatchers.get(i);
            if (rotationWatcher.mDisplayId == this.mDisplayId) {
                try {
                    rotationWatcher.mWatcher.onRotationChanged(rotation);
                } catch (RemoteException e) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$applyRotation$11(boolean rotateSeamlessly, WindowState w) {
        if (w.mHasSurface) {
            if (!rotateSeamlessly) {
                if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                    String protoLogParam0 = String.valueOf(w);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 2083556954, 0, (String) null, new Object[]{protoLogParam0});
                }
                w.setOrientationChanging(true);
            }
            w.mReportOrientationChanged = true;
        }
    }

    void configureDisplayPolicy() {
        this.mRootWindowContainer.updateDisplayImePolicyCache();
        this.mDisplayPolicy.updateConfigurationAndScreenSizeDependentBehaviors();
        this.mDisplayRotation.configure(this.mBaseDisplayWidth, this.mBaseDisplayHeight);
    }

    private DisplayInfo updateDisplayAndOrientation(int uiMode, Configuration outConfig) {
        int rotation = getRotation();
        boolean z = true;
        if (rotation != 1 && rotation != 3) {
            z = false;
        }
        boolean rotated = z;
        int dw = rotated ? this.mBaseDisplayHeight : this.mBaseDisplayWidth;
        int dh = rotated ? this.mBaseDisplayWidth : this.mBaseDisplayHeight;
        WmDisplayCutout wmDisplayCutout = calculateDisplayCutoutForRotation(rotation);
        DisplayCutout displayCutout = wmDisplayCutout.getDisplayCutout();
        RoundedCorners roundedCorners = calculateRoundedCornersForRotation(rotation);
        int appWidth = this.mDisplayPolicy.getNonDecorDisplayWidth(dw, dh, rotation, uiMode, displayCutout);
        int appHeight = this.mDisplayPolicy.getNonDecorDisplayHeight(dh, rotation, displayCutout);
        this.mDisplayInfo.rotation = rotation;
        this.mDisplayInfo.logicalWidth = dw;
        this.mDisplayInfo.logicalHeight = dh;
        this.mDisplayInfo.logicalDensityDpi = this.mBaseDisplayDensity;
        this.mDisplayInfo.physicalXDpi = this.mBaseDisplayPhysicalXDpi;
        this.mDisplayInfo.physicalYDpi = this.mBaseDisplayPhysicalYDpi;
        this.mDisplayInfo.appWidth = appWidth;
        this.mDisplayInfo.appHeight = appHeight;
        if (this.isDefaultDisplay) {
            this.mDisplayInfo.getLogicalMetrics(this.mRealDisplayMetrics, CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO, (Configuration) null);
        }
        this.mDisplayInfo.displayCutout = displayCutout.isEmpty() ? null : displayCutout;
        this.mDisplayInfo.roundedCorners = roundedCorners;
        this.mDisplayInfo.getAppMetrics(this.mDisplayMetrics);
        if (this.mDisplayScalingDisabled) {
            this.mDisplayInfo.flags |= 1073741824;
        } else {
            this.mDisplayInfo.flags &= -1073741825;
        }
        computeSizeRangesAndScreenLayout(this.mDisplayInfo, rotated, uiMode, dw, dh, this.mDisplayMetrics.density, outConfig);
        this.mWmService.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(this.mDisplayId, this.mDisplayInfo);
        if (this.isDefaultDisplay) {
            this.mCompatibleScreenScale = CompatibilityInfo.computeCompatibleScaling(this.mDisplayMetrics, this.mCompatDisplayMetrics);
        }
        onDisplayInfoChanged();
        return this.mDisplayInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WmDisplayCutout calculateDisplayCutoutForRotation(int rotation) {
        if (WallpaperController.OS_FOLEABLE_SCREEN_SUPPORT) {
            ActivityRecord activityRecord = this.mFocusedApp;
            if (!TranFoldingScreenController.isPkgInUseDefaultNotchSizeList(activityRecord != null ? activityRecord.packageName : "")) {
                return this.mDisplayNoLandCutoutCache.getOrCompute(this.mIsSizeForced ? this.mBaseDisplayCutout : this.mInitialDisplayCutout, rotation);
            }
        }
        return this.mDisplayCutoutCache.getOrCompute(this.mIsSizeForced ? this.mBaseDisplayCutout : this.mInitialDisplayCutout, rotation);
    }

    static WmDisplayCutout calculateDisplayCutoutForRotationAndDisplaySizeUncached(DisplayCutout cutout, int rotation, int displayWidth, int displayHeight) {
        return calculateDisplayCutoutForRotationAndDisplaySizeUncached(cutout, rotation, displayWidth, displayHeight, false);
    }

    static WmDisplayCutout calculateDisplayCutoutForRotationAndDisplaySizeUncached(DisplayCutout cutout, int rotation, int displayWidth, int displayHeight, boolean noLandCutout) {
        if (cutout == null || cutout == DisplayCutout.NO_CUTOUT) {
            return WmDisplayCutout.NO_CUTOUT;
        }
        if (rotation == 0) {
            return WmDisplayCutout.computeSafeInsets(cutout, displayWidth, displayHeight);
        }
        boolean rotated = false;
        DisplayCutout rotatedCutout = cutout.getRotated(displayWidth, displayHeight, 0, rotation);
        if (rotation == 1 || rotation == 3) {
            rotated = true;
        }
        return new WmDisplayCutout((WallpaperController.OS_FOLEABLE_SCREEN_SUPPORT && noLandCutout) ? DisplayCutout.NO_CUTOUT : rotatedCutout, new Size(rotated ? displayHeight : displayWidth, rotated ? displayWidth : displayHeight));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public WmDisplayCutout calculateDisplayCutoutForRotationUncached(DisplayCutout cutout, int rotation) {
        boolean z = this.mIsSizeForced;
        return calculateDisplayCutoutForRotationAndDisplaySizeUncached(cutout, rotation, z ? this.mBaseDisplayWidth : this.mInitialDisplayWidth, z ? this.mBaseDisplayHeight : this.mInitialDisplayHeight);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public WmDisplayCutout calculateDisplayNoLandCutoutForRotationUncached(DisplayCutout cutout, int rotation) {
        boolean z = this.mIsSizeForced;
        return calculateDisplayCutoutForRotationAndDisplaySizeUncached(cutout, rotation, z ? this.mBaseDisplayWidth : this.mInitialDisplayWidth, z ? this.mBaseDisplayHeight : this.mInitialDisplayHeight, true);
    }

    RoundedCorners calculateRoundedCornersForRotation(int rotation) {
        return this.mRoundedCornerCache.getOrCompute(this.mIsSizeForced ? this.mBaseRoundedCorners : this.mInitialRoundedCorners, rotation);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public RoundedCorners calculateRoundedCornersForRotationUncached(RoundedCorners roundedCorners, int rotation) {
        if (roundedCorners == null || roundedCorners == RoundedCorners.NO_ROUNDED_CORNERS) {
            return RoundedCorners.NO_ROUNDED_CORNERS;
        }
        if (rotation == 0) {
            return roundedCorners;
        }
        boolean z = this.mIsSizeForced;
        return roundedCorners.rotate(rotation, z ? this.mBaseDisplayWidth : this.mInitialDisplayWidth, z ? this.mBaseDisplayHeight : this.mInitialDisplayHeight);
    }

    PrivacyIndicatorBounds calculatePrivacyIndicatorBoundsForRotation(int rotation) {
        return this.mPrivacyIndicatorBoundsCache.getOrCompute(this.mCurrentPrivacyIndicatorBounds, rotation);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PrivacyIndicatorBounds calculatePrivacyIndicatorBoundsForRotationUncached(PrivacyIndicatorBounds bounds, int rotation) {
        if (bounds == null) {
            return new PrivacyIndicatorBounds(new Rect[4], rotation);
        }
        return bounds.rotate(rotation);
    }

    DisplayInfo computeScreenConfiguration(Configuration outConfig, int rotation) {
        boolean z = true;
        if (rotation != 1 && rotation != 3) {
            z = false;
        }
        boolean rotated = z;
        int dw = rotated ? this.mBaseDisplayHeight : this.mBaseDisplayWidth;
        int dh = rotated ? this.mBaseDisplayWidth : this.mBaseDisplayHeight;
        outConfig.windowConfiguration.setMaxBounds(0, 0, dw, dh);
        outConfig.windowConfiguration.setBounds(outConfig.windowConfiguration.getMaxBounds());
        int uiMode = getConfiguration().uiMode;
        DisplayCutout displayCutout = calculateDisplayCutoutForRotation(rotation).getDisplayCutout();
        computeScreenAppConfiguration(outConfig, dw, dh, rotation, uiMode, displayCutout);
        DisplayInfo displayInfo = new DisplayInfo(this.mDisplayInfo);
        displayInfo.rotation = rotation;
        displayInfo.logicalWidth = dw;
        displayInfo.logicalHeight = dh;
        Rect appBounds = outConfig.windowConfiguration.getAppBounds();
        displayInfo.appWidth = appBounds.width();
        displayInfo.appHeight = appBounds.height();
        displayInfo.displayCutout = displayCutout.isEmpty() ? null : displayCutout;
        computeSizeRangesAndScreenLayout(displayInfo, rotated, uiMode, dw, dh, this.mDisplayMetrics.density, outConfig);
        return displayInfo;
    }

    private void computeScreenAppConfiguration(Configuration outConfig, int dw, int dh, int rotation, int uiMode, DisplayCutout displayCutout) {
        int appWidth = this.mDisplayPolicy.getNonDecorDisplayWidth(dw, dh, rotation, uiMode, displayCutout);
        int appHeight = this.mDisplayPolicy.getNonDecorDisplayHeight(dh, rotation, displayCutout);
        this.mDisplayPolicy.getNonDecorInsetsLw(rotation, displayCutout, this.mTmpRect);
        int leftInset = this.mTmpRect.left;
        int topInset = this.mTmpRect.top;
        outConfig.windowConfiguration.setAppBounds(leftInset, topInset, leftInset + appWidth, topInset + appHeight);
        outConfig.windowConfiguration.setRotation(rotation);
        outConfig.orientation = dw <= dh ? 1 : 2;
        float density = this.mDisplayMetrics.density;
        outConfig.screenWidthDp = (int) (this.mDisplayPolicy.getConfigDisplayWidth(dw, dh, rotation, uiMode, displayCutout) / density);
        outConfig.screenHeightDp = (int) (this.mDisplayPolicy.getConfigDisplayHeight(dw, dh, rotation, uiMode, displayCutout) / density);
        outConfig.compatScreenWidthDp = (int) (outConfig.screenWidthDp / this.mCompatibleScreenScale);
        outConfig.compatScreenHeightDp = (int) (outConfig.screenHeightDp / this.mCompatibleScreenScale);
        boolean rotated = rotation == 1 || rotation == 3;
        outConfig.compatSmallestScreenWidthDp = computeCompatSmallestWidth(rotated, uiMode, dw, dh);
        outConfig.windowConfiguration.setDisplayRotation(rotation);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void computeScreenConfiguration(Configuration config) {
        int i;
        int i2;
        int i3;
        int i4;
        DisplayInfo displayInfo = updateDisplayAndOrientation(config.uiMode, config);
        int dw = displayInfo.logicalWidth;
        int dh = displayInfo.logicalHeight;
        this.mTmpRect.set(0, 0, dw, dh);
        config.windowConfiguration.setBounds(this.mTmpRect);
        config.windowConfiguration.setMaxBounds(this.mTmpRect);
        config.windowConfiguration.setWindowingMode(getWindowingMode());
        config.windowConfiguration.setDisplayWindowingMode(getWindowingMode());
        computeScreenAppConfiguration(config, dw, dh, displayInfo.rotation, config.uiMode, displayInfo.displayCutout);
        int i5 = config.screenLayout & (-769);
        if ((displayInfo.flags & 16) != 0) {
            i = 512;
        } else {
            i = 256;
        }
        config.screenLayout = i5 | i;
        config.densityDpi = displayInfo.logicalDensityDpi;
        if (displayInfo.isHdr() && this.mWmService.hasHdrSupport()) {
            i2 = 8;
        } else {
            i2 = 4;
        }
        int i6 = 1;
        if (displayInfo.isWideColorGamut() && this.mWmService.hasWideColorGamutSupport()) {
            i3 = 2;
        } else {
            i3 = 1;
        }
        config.colorMode = i2 | i3;
        config.touchscreen = 1;
        config.keyboard = 1;
        config.navigation = 1;
        int keyboardPresence = 0;
        int navigationPresence = 0;
        InputDevice[] devices = this.mWmService.mInputManager.getInputDevices();
        int len = devices != null ? devices.length : 0;
        int i7 = 0;
        while (i7 < len) {
            InputDevice device = devices[i7];
            if (!device.isVirtual() && this.mWmService.mInputManager.canDispatchToDisplay(device.getId(), this.mDisplayId)) {
                int sources = device.getSources();
                int presenceFlag = device.isExternal() ? 2 : i6;
                if (this.mWmService.mIsTouchDevice) {
                    if ((sources & UsbACInterface.FORMAT_II_AC3) == 4098) {
                        config.touchscreen = 3;
                    }
                } else {
                    config.touchscreen = 1;
                }
                if ((sources & 65540) == 65540) {
                    config.navigation = 3;
                    navigationPresence |= presenceFlag;
                    i4 = 2;
                } else if ((sources & UsbTerminalTypes.TERMINAL_IN_MIC) != 513 || config.navigation != 1) {
                    i4 = 2;
                } else {
                    i4 = 2;
                    config.navigation = 2;
                    navigationPresence |= presenceFlag;
                }
                if (device.getKeyboardType() == i4) {
                    config.keyboard = i4;
                    keyboardPresence |= presenceFlag;
                }
            }
            i7++;
            i6 = 1;
        }
        if (config.navigation == 1 && this.mWmService.mHasPermanentDpad) {
            config.navigation = 2;
            navigationPresence |= 1;
        }
        boolean hardKeyboardAvailable = config.keyboard != 1;
        if (hardKeyboardAvailable != this.mWmService.mHardKeyboardAvailable) {
            this.mWmService.mHardKeyboardAvailable = hardKeyboardAvailable;
            this.mWmService.mH.removeMessages(22);
            this.mWmService.mH.sendEmptyMessage(22);
        }
        this.mDisplayPolicy.updateConfigurationAndScreenSizeDependentBehaviors();
        config.keyboardHidden = 1;
        config.hardKeyboardHidden = 1;
        config.navigationHidden = 1;
        TranFoldWMCustody.instance().computeScreenConfiguration(config, displayInfo);
        this.mWmService.mPolicy.adjustConfigurationLw(config, keyboardPresence, navigationPresence);
    }

    private int computeCompatSmallestWidth(boolean rotated, int uiMode, int dw, int dh) {
        int unrotDw;
        int unrotDh;
        this.mTmpDisplayMetrics.setTo(this.mDisplayMetrics);
        DisplayMetrics tmpDm = this.mTmpDisplayMetrics;
        if (rotated) {
            unrotDw = dh;
            unrotDh = dw;
        } else {
            unrotDw = dw;
            unrotDh = dh;
        }
        int sw = reduceCompatConfigWidthSize(0, 0, uiMode, tmpDm, unrotDw, unrotDh);
        return reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(reduceCompatConfigWidthSize(sw, 1, uiMode, tmpDm, unrotDh, unrotDw), 2, uiMode, tmpDm, unrotDw, unrotDh), 3, uiMode, tmpDm, unrotDh, unrotDw);
    }

    private int reduceCompatConfigWidthSize(int curSize, int rotation, int uiMode, DisplayMetrics dm, int dw, int dh) {
        DisplayCutout displayCutout = calculateDisplayCutoutForRotation(rotation).getDisplayCutout();
        dm.noncompatWidthPixels = this.mDisplayPolicy.getNonDecorDisplayWidth(dw, dh, rotation, uiMode, displayCutout);
        dm.noncompatHeightPixels = this.mDisplayPolicy.getNonDecorDisplayHeight(dh, rotation, displayCutout);
        float scale = CompatibilityInfo.computeCompatibleScaling(dm, (DisplayMetrics) null);
        int size = (int) (((dm.noncompatWidthPixels / scale) / dm.density) + 0.5f);
        if (curSize == 0 || size < curSize) {
            return size;
        }
        return curSize;
    }

    private void computeSizeRangesAndScreenLayout(DisplayInfo displayInfo, boolean rotated, int uiMode, int dw, int dh, float density, Configuration outConfig) {
        int unrotDw;
        int unrotDh;
        if (rotated) {
            unrotDw = dh;
            unrotDh = dw;
        } else {
            unrotDw = dw;
            unrotDh = dh;
        }
        displayInfo.smallestNominalAppWidth = 1073741824;
        displayInfo.smallestNominalAppHeight = 1073741824;
        displayInfo.largestNominalAppWidth = 0;
        displayInfo.largestNominalAppHeight = 0;
        adjustDisplaySizeRanges(displayInfo, 0, uiMode, unrotDw, unrotDh);
        adjustDisplaySizeRanges(displayInfo, 1, uiMode, unrotDh, unrotDw);
        adjustDisplaySizeRanges(displayInfo, 2, uiMode, unrotDw, unrotDh);
        adjustDisplaySizeRanges(displayInfo, 3, uiMode, unrotDh, unrotDw);
        if (outConfig == null) {
            return;
        }
        int sl = Configuration.resetScreenLayout(outConfig.screenLayout);
        int sl2 = reduceConfigLayout(reduceConfigLayout(reduceConfigLayout(reduceConfigLayout(sl, 0, density, unrotDw, unrotDh, uiMode), 1, density, unrotDh, unrotDw, uiMode), 2, density, unrotDw, unrotDh, uiMode), 3, density, unrotDh, unrotDw, uiMode);
        outConfig.smallestScreenWidthDp = (int) (displayInfo.smallestNominalAppWidth / density);
        outConfig.screenLayout = sl2;
    }

    private int reduceConfigLayout(int curLayout, int rotation, float density, int dw, int dh, int uiMode) {
        DisplayCutout displayCutout = calculateDisplayCutoutForRotation(rotation).getDisplayCutout();
        int w = this.mDisplayPolicy.getNonDecorDisplayWidth(dw, dh, rotation, uiMode, displayCutout);
        int h = this.mDisplayPolicy.getNonDecorDisplayHeight(dh, rotation, displayCutout);
        int longSize = w;
        int shortSize = h;
        if (longSize < shortSize) {
            longSize = shortSize;
            shortSize = longSize;
        }
        return Configuration.reduceScreenLayout(curLayout, (int) (longSize / density), (int) (shortSize / density));
    }

    private void adjustDisplaySizeRanges(DisplayInfo displayInfo, int rotation, int uiMode, int dw, int dh) {
        DisplayCutout displayCutout = calculateDisplayCutoutForRotation(rotation).getDisplayCutout();
        int width = this.mDisplayPolicy.getConfigDisplayWidth(dw, dh, rotation, uiMode, displayCutout);
        if (width < displayInfo.smallestNominalAppWidth) {
            displayInfo.smallestNominalAppWidth = width;
        }
        if (width > displayInfo.largestNominalAppWidth) {
            displayInfo.largestNominalAppWidth = width;
        }
        int height = this.mDisplayPolicy.getConfigDisplayHeight(dw, dh, rotation, uiMode, displayCutout);
        if (height < displayInfo.smallestNominalAppHeight) {
            displayInfo.smallestNominalAppHeight = height;
        }
        if (height > displayInfo.largestNominalAppHeight) {
            displayInfo.largestNominalAppHeight = height;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPreferredOptionsPanelGravity() {
        int rotation = getRotation();
        if (this.mInitialDisplayWidth < this.mInitialDisplayHeight) {
            switch (rotation) {
                case 1:
                    return 85;
                case 2:
                    return 81;
                case 3:
                    return 8388691;
                default:
                    return 81;
            }
        }
        switch (rotation) {
            case 1:
                return 81;
            case 2:
                return 8388691;
            case 3:
                return 81;
            default:
                return 85;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DockedTaskDividerController getDockedDividerController() {
        return this.mDividerControllerLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PinnedTaskController getPinnedTaskController() {
        return this.mPinnedTaskController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAccess(int uid) {
        return this.mDisplay.hasAccess(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPrivate() {
        return (this.mDisplay.getFlags() & 4) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTrusted() {
        return this.mDisplay.isTrusted();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootTask(final int windowingMode, final int activityType) {
        return (Task) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda52
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Task rootTask;
                rootTask = ((TaskDisplayArea) obj).getRootTask(windowingMode, activityType);
                return rootTask;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getRootTask$13(int rootTaskId, Task rootTask) {
        return rootTask.getRootTaskId() == rootTaskId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootTask(final int rootTaskId) {
        return getRootTask(new Predicate() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda13
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DisplayContent.lambda$getRootTask$13(rootTaskId, (Task) obj);
            }
        });
    }

    int getRootTaskCount() {
        final int[] count = new int[1];
        forAllRootTasks(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda39
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.lambda$getRootTaskCount$14(count, (Task) obj);
            }
        });
        return count[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getRootTaskCount$14(int[] count, Task task) {
        count[0] = count[0] + 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopRootTask$15(Task t) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTopRootTask() {
        return getRootTask(new Predicate() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda10
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DisplayContent.lambda$getTopRootTask$15((Task) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentOverrideConfigurationChanges() {
        return this.mCurrentOverrideConfigurationChanges;
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void onConfigurationChanged(Configuration newParentConfig) {
        int lastOrientation = getConfiguration().orientation;
        super.onConfigurationChanged(newParentConfig);
        DisplayPolicy displayPolicy = this.mDisplayPolicy;
        if (displayPolicy != null) {
            displayPolicy.onConfigurationChanged();
            this.mPinnedTaskController.onPostDisplayConfigurationChanged();
        }
        updateImeParent();
        ContentRecorder contentRecorder = this.mContentRecorder;
        if (contentRecorder != null) {
            contentRecorder.onConfigurationChanged(lastOrientation);
        }
        if (lastOrientation != getConfiguration().orientation) {
            getMetricsLogger().write(new LogMaker(1659).setSubtype(getConfiguration().orientation).addTaggedData(1660, Integer.valueOf(getDisplayId())));
        }
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    boolean fillsParent() {
        return true;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean isVisible() {
        return true;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean isVisibleRequested() {
        return (!isVisible() || this.mRemoved || this.mRemoving) ? false : true;
    }

    @Override // com.android.server.wm.WindowContainer
    void onAppTransitionDone() {
        super.onAppTransitionDone();
        this.mWmService.mWindowsChanged = true;
        ActivityRecord activityRecord = this.mFixedRotationLaunchingApp;
        if (activityRecord != null && !activityRecord.mVisibleRequested && !this.mFixedRotationLaunchingApp.isVisible() && !this.mDisplayRotation.isRotatingSeamlessly()) {
            clearFixedRotationLaunchingApp();
        }
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public void setWindowingMode(int windowingMode) {
        this.mTmpConfiguration.setTo(getRequestedOverrideConfiguration());
        this.mTmpConfiguration.windowConfiguration.setWindowingMode(windowingMode);
        this.mTmpConfiguration.windowConfiguration.setDisplayWindowingMode(windowingMode);
        onRequestedOverrideConfigurationChanged(this.mTmpConfiguration);
    }

    @Override // com.android.server.wm.ConfigurationContainer
    void setDisplayWindowingMode(int windowingMode) {
        setWindowingMode(windowingMode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forAllImeWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        return this.mImeWindowsContainer.forAllWindowForce(callback, traverseTopToBottom);
    }

    @Override // com.android.server.wm.WindowContainer
    int getOrientation() {
        this.mLastOrientationSource = null;
        if (!handlesOrientationChangeFromDescendant()) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                long protoLogParam0 = this.mDisplayId;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1648338379, 5, (String) null, new Object[]{Long.valueOf(protoLogParam0), -1L});
            }
            return -1;
        } else if (this.mWmService.mDisplayFrozen && this.mWmService.mPolicy.isKeyguardLocked()) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                long protoLogParam02 = this.mDisplayId;
                long protoLogParam1 = getLastOrientation();
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1710206702, 5, (String) null, new Object[]{Long.valueOf(protoLogParam02), Long.valueOf(protoLogParam1)});
            }
            return getLastOrientation();
        } else {
            int orientation = super.getOrientation();
            if (orientation == -2) {
                if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                    long protoLogParam12 = this.mDisplayId;
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1480772131, 5, (String) null, new Object[]{-1L, Long.valueOf(protoLogParam12)});
                }
                return -1;
            }
            return orientation;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateDisplayInfo() {
        updateBaseDisplayMetricsIfNeeded();
        this.mDisplay.getDisplayInfo(this.mDisplayInfo);
        this.mDisplay.getMetrics(this.mDisplayMetrics);
        onDisplayInfoChanged();
        onDisplayChanged(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updatePrivacyIndicatorBounds(Rect[] staticBounds) {
        PrivacyIndicatorBounds oldBounds = this.mCurrentPrivacyIndicatorBounds;
        PrivacyIndicatorBounds updateStaticBounds = this.mCurrentPrivacyIndicatorBounds.updateStaticBounds(staticBounds);
        this.mCurrentPrivacyIndicatorBounds = updateStaticBounds;
        if (!Objects.equals(oldBounds, updateStaticBounds)) {
            updateDisplayFrames(false, true);
        }
    }

    void onDisplayInfoChanged() {
        updateDisplayFrames(ViewRootImpl.LOCAL_LAYOUT, ViewRootImpl.LOCAL_LAYOUT);
        this.mMinSizeOfResizeableTaskDp = getMinimalTaskSizeDp();
        this.mInputMonitor.layoutInputConsumers(this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight);
        this.mDisplayPolicy.onDisplayInfoChanged(this.mDisplayInfo);
    }

    private void updateDisplayFrames(boolean insetsSourceMayChange, boolean notifyInsetsChange) {
        DisplayFrames displayFrames = this.mDisplayFrames;
        DisplayInfo displayInfo = this.mDisplayInfo;
        if (displayFrames.update(displayInfo, calculateDisplayCutoutForRotation(displayInfo.rotation), calculateRoundedCornersForRotation(this.mDisplayInfo.rotation), calculatePrivacyIndicatorBoundsForRotation(this.mDisplayInfo.rotation))) {
            if (insetsSourceMayChange) {
                this.mDisplayPolicy.updateInsetsSourceFramesExceptIme(this.mDisplayFrames);
            }
            this.mInsetsStateController.onDisplayFramesUpdated(notifyInsetsChange);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void onDisplayChanged(DisplayContent dc) {
        super.onDisplayChanged(dc);
        updateSystemGestureExclusionLimit();
        updateKeepClearAreas();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSystemGestureExclusionLimit() {
        this.mSystemGestureExclusionLimit = (this.mWmService.mConstants.mSystemGestureExclusionLimitDp * this.mDisplayMetrics.densityDpi) / 160;
        updateSystemGestureExclusion();
    }

    void initializeDisplayBaseInfo() {
        DisplayManagerInternal displayManagerInternal = this.mWmService.mDisplayManagerInternal;
        if (displayManagerInternal != null) {
            DisplayInfo newDisplayInfo = displayManagerInternal.getDisplayInfo(this.mDisplayId);
            if (newDisplayInfo != null) {
                this.mDisplayInfo.copyFrom(newDisplayInfo);
            }
            this.mDwpcHelper = new DisplayWindowPolicyControllerHelper(this);
        }
        updateBaseDisplayMetrics(this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight, this.mDisplayInfo.logicalDensityDpi, this.mDisplayInfo.physicalXDpi, this.mDisplayInfo.physicalYDpi);
        this.mInitialDisplayWidth = this.mDisplayInfo.logicalWidth;
        this.mInitialDisplayHeight = this.mDisplayInfo.logicalHeight;
        this.mInitialDisplayDensity = this.mDisplayInfo.logicalDensityDpi;
        this.mInitialPhysicalXDpi = this.mDisplayInfo.physicalXDpi;
        this.mInitialPhysicalYDpi = this.mDisplayInfo.physicalYDpi;
        this.mInitialDisplayCutout = this.mDisplayInfo.displayCutout;
        this.mInitialRoundedCorners = this.mDisplayInfo.roundedCorners;
        this.mCurrentPrivacyIndicatorBounds = new PrivacyIndicatorBounds(new Rect[4], this.mDisplayInfo.rotation);
        Display.Mode maxDisplayMode = DisplayUtils.getMaximumResolutionDisplayMode(this.mDisplayInfo.supportedModes);
        this.mPhysicalDisplaySize = new Point(maxDisplayMode == null ? this.mInitialDisplayWidth : maxDisplayMode.getPhysicalWidth(), maxDisplayMode == null ? this.mInitialDisplayHeight : maxDisplayMode.getPhysicalHeight());
    }

    private void updateBaseDisplayMetricsIfNeeded() {
        DisplayCutout displayCutout;
        RoundedCorners newRoundedCorners;
        String newUniqueId;
        DisplayCutout newCutout;
        this.mWmService.mDisplayManagerInternal.getNonOverrideDisplayInfo(this.mDisplayId, this.mDisplayInfo);
        int orientation = this.mDisplayInfo.rotation;
        boolean rotated = orientation == 1 || orientation == 3;
        DisplayInfo displayInfo = this.mDisplayInfo;
        int newWidth = rotated ? displayInfo.logicalHeight : displayInfo.logicalWidth;
        DisplayInfo displayInfo2 = this.mDisplayInfo;
        int newHeight = rotated ? displayInfo2.logicalWidth : displayInfo2.logicalHeight;
        int newDensity = this.mDisplayInfo.logicalDensityDpi;
        float newXDpi = this.mDisplayInfo.physicalXDpi;
        float newYDpi = this.mDisplayInfo.physicalYDpi;
        if (!this.mIgnoreDisplayCutout) {
            displayCutout = this.mDisplayInfo.displayCutout;
        } else {
            displayCutout = DisplayCutout.NO_CUTOUT;
        }
        DisplayCutout newCutout2 = displayCutout;
        String newUniqueId2 = this.mDisplayInfo.uniqueId;
        RoundedCorners newRoundedCorners2 = this.mDisplayInfo.roundedCorners;
        boolean displayMetricsChanged = (this.mInitialDisplayWidth == newWidth && this.mInitialDisplayHeight == newHeight && this.mInitialDisplayDensity == newDensity && this.mInitialPhysicalXDpi == newXDpi && this.mInitialPhysicalYDpi == newYDpi && Objects.equals(this.mInitialDisplayCutout, newCutout2) && Objects.equals(this.mInitialRoundedCorners, newRoundedCorners2)) ? false : true;
        boolean physicalDisplayChanged = true ^ newUniqueId2.equals(this.mCurrentUniqueDisplayId);
        if (displayMetricsChanged || physicalDisplayChanged) {
            if (physicalDisplayChanged) {
                this.mWmService.mDisplayWindowSettings.applySettingsToDisplayLocked(this, false);
                newRoundedCorners = newRoundedCorners2;
                newUniqueId = newUniqueId2;
                newCutout = newCutout2;
                this.mDisplaySwitchTransitionLauncher.requestDisplaySwitchTransitionIfNeeded(this.mDisplayId, this.mInitialDisplayWidth, this.mInitialDisplayHeight, newWidth, newHeight);
            } else {
                newRoundedCorners = newRoundedCorners2;
                newUniqueId = newUniqueId2;
                newCutout = newCutout2;
            }
            boolean z = this.mIsSizeForced;
            String newUniqueId3 = newUniqueId;
            RoundedCorners newRoundedCorners3 = newRoundedCorners;
            updateBaseDisplayMetrics(z ? this.mBaseDisplayWidth : newWidth, z ? this.mBaseDisplayHeight : newHeight, this.mIsDensityForced ? this.mBaseDisplayDensity : newDensity, z ? this.mBaseDisplayPhysicalXDpi : newXDpi, z ? this.mBaseDisplayPhysicalYDpi : newYDpi);
            configureDisplayPolicy();
            if (physicalDisplayChanged) {
                this.mWmService.mDisplayWindowSettings.applyRotationSettingsToDisplayLocked(this);
            }
            this.mInitialDisplayWidth = newWidth;
            this.mInitialDisplayHeight = newHeight;
            this.mInitialDisplayDensity = newDensity;
            this.mInitialPhysicalXDpi = newXDpi;
            this.mInitialPhysicalYDpi = newYDpi;
            this.mInitialDisplayCutout = newCutout;
            this.mInitialRoundedCorners = newRoundedCorners3;
            this.mCurrentUniqueDisplayId = newUniqueId3;
            TranFoldWMCustody.instance().updateCurrentUniqueDisplayId(this);
            reconfigureDisplayLocked();
            if (physicalDisplayChanged) {
                this.mDisplaySwitchTransitionLauncher.onDisplayUpdated();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMaxUiWidth(int width) {
        if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
            Slog.v("WindowManager", "Setting max ui width:" + width + " on display:" + getDisplayId());
        }
        this.mMaxUiWidth = width;
        updateBaseDisplayMetrics(this.mBaseDisplayWidth, this.mBaseDisplayHeight, this.mBaseDisplayDensity, this.mBaseDisplayPhysicalXDpi, this.mBaseDisplayPhysicalYDpi);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateBaseDisplayMetrics(int baseWidth, int baseHeight, int baseDensity, float baseXDpi, float baseYDpi) {
        int i;
        this.mBaseDisplayWidth = baseWidth;
        this.mBaseDisplayHeight = baseHeight;
        this.mBaseDisplayDensity = baseDensity;
        this.mBaseDisplayPhysicalXDpi = baseXDpi;
        this.mBaseDisplayPhysicalYDpi = baseYDpi;
        if (this.mIsSizeForced) {
            this.mBaseDisplayCutout = loadDisplayCutout(baseWidth, baseHeight);
            this.mBaseRoundedCorners = loadRoundedCorners(baseWidth, baseHeight);
        }
        int i2 = this.mMaxUiWidth;
        if (i2 > 0 && (i = this.mBaseDisplayWidth) > i2) {
            float ratio = i2 / i;
            this.mBaseDisplayHeight = (int) (this.mBaseDisplayHeight * ratio);
            this.mBaseDisplayWidth = i2;
            this.mBaseDisplayPhysicalXDpi *= ratio;
            this.mBaseDisplayPhysicalYDpi *= ratio;
            if (!this.mIsDensityForced) {
                this.mBaseDisplayDensity = (int) (this.mBaseDisplayDensity * ratio);
            }
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                Slog.v("WindowManager", "Applying config restraints:" + this.mBaseDisplayWidth + "x" + this.mBaseDisplayHeight + " on display:" + getDisplayId());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForcedDensity(int density, int userId) {
        this.mIsDensityForced = density != this.mInitialDisplayDensity;
        boolean updateCurrent = userId == -2;
        if (this.mWmService.mCurrentUserId == userId || updateCurrent) {
            this.mBaseDisplayDensity = density;
            reconfigureDisplayLocked();
        }
        if (updateCurrent) {
            return;
        }
        if (density == this.mInitialDisplayDensity) {
            density = 0;
        }
        this.mWmService.mDisplayWindowSettings.setForcedDensity(this, density, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForcedScalingMode(int mode) {
        if (mode != 1) {
            mode = 0;
        }
        this.mDisplayScalingDisabled = mode != 0;
        Slog.i("WindowManager", "Using display scaling mode: " + (this.mDisplayScalingDisabled ? "off" : UiModeManagerService.Shell.NIGHT_MODE_STR_AUTO));
        reconfigureDisplayLocked();
        this.mWmService.mDisplayWindowSettings.setForcedScalingMode(this, mode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setForcedSize(int width, int height) {
        int i = this.mMaxUiWidth;
        if (i > 0 && width > i) {
            float ratio = i / width;
            height = (int) (height * ratio);
            width = this.mMaxUiWidth;
        }
        boolean z = (this.mInitialDisplayWidth == width && this.mInitialDisplayHeight == height) ? false : true;
        this.mIsSizeForced = z;
        if (z) {
            width = Math.min(Math.max(width, 200), this.mInitialDisplayWidth * 2);
            height = Math.min(Math.max(height, 200), this.mInitialDisplayHeight * 2);
        }
        Slog.i("WindowManager", "Using new display size: " + width + "x" + height);
        updateBaseDisplayMetrics(width, height, this.mBaseDisplayDensity, this.mBaseDisplayPhysicalXDpi, this.mBaseDisplayPhysicalYDpi);
        reconfigureDisplayLocked();
        if (!this.mIsSizeForced) {
            height = 0;
            width = 0;
        }
        this.mWmService.mDisplayWindowSettings.setForcedSize(this, width, height);
    }

    DisplayCutout loadDisplayCutout(int displayWidth, int displayHeight) {
        DisplayPolicy displayPolicy = this.mDisplayPolicy;
        if (displayPolicy == null || this.mInitialDisplayCutout == null) {
            return null;
        }
        return DisplayCutout.fromResourcesRectApproximation(displayPolicy.getSystemUiContext().getResources(), this.mDisplayInfo.uniqueId, this.mPhysicalDisplaySize.x, this.mPhysicalDisplaySize.y, displayWidth, displayHeight);
    }

    RoundedCorners loadRoundedCorners(int displayWidth, int displayHeight) {
        DisplayPolicy displayPolicy = this.mDisplayPolicy;
        if (displayPolicy == null || this.mInitialRoundedCorners == null) {
            return null;
        }
        return RoundedCorners.fromResources(displayPolicy.getSystemUiContext().getResources(), this.mDisplayInfo.uniqueId, this.mPhysicalDisplaySize.x, this.mPhysicalDisplaySize.y, displayWidth, displayHeight);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.DisplayArea
    public void getStableRect(Rect out) {
        InsetsState state = this.mDisplayContent.getInsetsStateController().getRawInsetsState();
        out.set(state.getDisplayFrame());
        out.inset(state.calculateInsets(out, WindowInsets.Type.systemBars(), true));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskDisplayArea getDefaultTaskDisplayArea() {
        return this.mDisplayAreaPolicy.getDefaultTaskDisplayArea();
    }

    void updateDisplayAreaOrganizers() {
        if (!isTrusted()) {
            return;
        }
        forAllDisplayAreas(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7938x2ece7b76((DisplayArea) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateDisplayAreaOrganizers$16$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7938x2ece7b76(DisplayArea displayArea) {
        IDisplayAreaOrganizer organizer;
        if (!displayArea.isOrganized() && (organizer = this.mAtmService.mWindowOrganizerController.mDisplayAreaOrganizerController.getOrganizerByFeature(displayArea.mFeatureId)) != null) {
            displayArea.setOrganizer(organizer);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean pointWithinAppWindow(final int x, final int y) {
        final int[] targetWindowType = {-1};
        PooledConsumer fn = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda64
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                DisplayContent.lambda$pointWithinAppWindow$17(targetWindowType, x, y, (WindowState) obj, (Rect) obj2);
            }
        }, PooledLambda.__(WindowState.class), this.mTmpRect);
        forAllWindows((Consumer<WindowState>) fn, true);
        fn.recycle();
        return 1 <= targetWindowType[0] && targetWindowType[0] <= 99;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$pointWithinAppWindow$17(int[] targetWindowType, int x, int y, WindowState w, Rect nonArg) {
        if (targetWindowType[0] == -1 && w.isOnScreen() && w.isVisible() && w.getFrame().contains(x, y)) {
            targetWindowType[0] = w.mAttrs.type;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task findTaskForResizePoint(final int x, final int y) {
        final int delta = WindowManagerService.dipToPixel(30, this.mDisplayMetrics);
        return (Task) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda38
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DisplayContent.this.m7918xf73a2815(x, y, delta, (TaskDisplayArea) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$findTaskForResizePoint$18$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ Task m7918xf73a2815(int x, int y, int delta, TaskDisplayArea taskDisplayArea) {
        return this.mTmpTaskForResizePointSearchResult.process(taskDisplayArea, x, y, delta);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTouchExcludeRegion() {
        ActivityRecord activityRecord = this.mFocusedApp;
        Task focusedTask = activityRecord != null ? activityRecord.getTask() : null;
        if (focusedTask == null) {
            this.mTouchExcludeRegion.setEmpty();
        } else {
            this.mTouchExcludeRegion.set(0, 0, this.mDisplayInfo.logicalWidth, this.mDisplayInfo.logicalHeight);
            int delta = WindowManagerService.dipToPixel(30, this.mDisplayMetrics);
            this.mTmpRect.setEmpty();
            this.mTmpRect2.setEmpty();
            PooledConsumer c = PooledLambda.obtainConsumer(new QuadConsumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda54
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                    ((DisplayContent) obj).processTaskForTouchExcludeRegion((Task) obj2, (Task) obj3, ((Integer) obj4).intValue());
                }
            }, this, PooledLambda.__(Task.class), focusedTask, Integer.valueOf(delta));
            forAllTasks((Consumer<Task>) c);
            c.recycle();
            if (!this.mTmpRect2.isEmpty()) {
                this.mTouchExcludeRegion.op(this.mTmpRect2, Region.Op.UNION);
            }
        }
        WindowState windowState = this.mInputMethodWindow;
        if (windowState != null && windowState.isVisible()) {
            this.mInputMethodWindow.getTouchableRegion(this.mTmpRegion);
            this.mTouchExcludeRegion.op(this.mTmpRegion, Region.Op.UNION);
        }
        for (int i = this.mTapExcludedWindows.size() - 1; i >= 0; i--) {
            WindowState win = this.mTapExcludedWindows.get(i);
            if (win.isVisible()) {
                win.getTouchableRegion(this.mTmpRegion);
                this.mTouchExcludeRegion.op(this.mTmpRegion, Region.Op.UNION);
            }
        }
        amendWindowTapExcludeRegion(this.mTouchExcludeRegion);
        this.mTapDetector.setTouchExcludeRegion(this.mTouchExcludeRegion);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processTaskForTouchExcludeRegion(Task task, Task focusedTask, int delta) {
        ActivityRecord topVisibleActivity = task.getTopVisibleActivity();
        if (topVisibleActivity == null || !topVisibleActivity.hasContentToDisplay()) {
            return;
        }
        if (task.isActivityTypeHome() && task.isVisible() && task.isResizeable()) {
            task.getDisplayArea().getBounds(this.mTmpRect);
        } else {
            task.getDimBounds(this.mTmpRect);
        }
        if (task == focusedTask) {
            this.mTmpRect2.set(this.mTmpRect);
        }
        boolean isFreeformed = task.inFreeformWindowingMode();
        if (task != focusedTask || isFreeformed) {
            if (isFreeformed) {
                this.mTmpRect.inset(-delta, -delta);
                this.mTmpRect.inset(getInsetsStateController().getRawInsetsState().calculateInsets(this.mTmpRect, WindowInsets.Type.systemBars() | WindowInsets.Type.ime(), false));
            }
            this.mTouchExcludeRegion.op(this.mTmpRect, Region.Op.DIFFERENCE);
        }
    }

    private void amendWindowTapExcludeRegion(Region inOutRegion) {
        Region region = Region.obtain();
        for (int i = this.mTapExcludeProvidingWindows.size() - 1; i >= 0; i--) {
            WindowState win = this.mTapExcludeProvidingWindows.valueAt(i);
            win.getTapExcludeRegion(region);
            inOutRegion.op(region, Region.Op.UNION);
        }
        region.recycle();
    }

    @Override // com.android.server.wm.WindowContainer
    void switchUser(int userId) {
        super.switchUser(userId);
        this.mWmService.mWindowsChanged = true;
        this.mDisplayPolicy.switchUser();
    }

    private boolean shouldDeferRemoval() {
        return isAnimating(3) || this.mTransitionController.isTransitionOnDisplay(this);
    }

    @Override // com.android.server.wm.WindowContainer
    void removeIfPossible() {
        if (shouldDeferRemoval()) {
            this.mDeferredRemoval = true;
        } else {
            removeImmediately();
        }
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    void removeImmediately() {
        this.mDeferredRemoval = false;
        try {
            this.mOpeningApps.clear();
            this.mClosingApps.clear();
            this.mChangingContainers.clear();
            this.mUnknownAppVisibilityController.clear();
            this.mAppTransition.removeAppTransitionTimeoutCallbacks();
            this.mTransitionController.unregisterLegacyListener(this.mFixedRotationTransitionListener);
            handleAnimatingStoppedAndTransition();
            this.mWmService.stopFreezingDisplayLocked();
            this.mDisplaySwitchTransitionLauncher.destroy();
            super.removeImmediately();
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                Slog.v("WindowManager", "Removing display=" + this);
            }
            this.mPointerEventDispatcher.dispose();
            if (this.mScreenRotationAnimation != null) {
                Slog.d("WindowManager", "remove window and kill screen rotation anim ......... ");
                this.mScreenRotationAnimation.kill();
            }
            setRotationAnimation(null);
            setRemoteInsetsController(null);
            this.mWmService.mAnimator.removeDisplayLocked(this.mDisplayId);
            this.mOverlayLayer.release();
            this.mWindowingLayer.release();
            this.mInputMonitor.onDisplayRemoved();
            this.mWmService.mDisplayNotificationController.dispatchDisplayRemoved(this);
            this.mWmService.mAccessibilityController.onDisplayRemoved(this.mDisplayId);
            this.mRootWindowContainer.mTaskSupervisor.getKeyguardController().onDisplayRemoved(this.mDisplayId);
            this.mDisplayReady = false;
            getPendingTransaction().apply();
            this.mWmService.mWindowPlacerLocked.requestTraversal();
        } catch (Throwable th) {
            this.mDisplayReady = false;
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean handleCompleteDeferredRemoval() {
        boolean stillDeferringRemoval = super.handleCompleteDeferredRemoval() || shouldDeferRemoval();
        if (!stillDeferringRemoval && this.mDeferredRemoval) {
            removeImmediately();
            return false;
        }
        return stillDeferringRemoval;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void adjustForImeIfNeeded() {
        WindowState imeWin = this.mInputMethodWindow;
        boolean imeVisible = imeWin != null && imeWin.isVisible() && imeWin.isDisplayed();
        int imeHeight = getInputMethodWindowVisibleHeight();
        this.mPinnedTaskController.setAdjustedForIme(imeVisible, imeHeight);
        ITranWindowManagerService.Instance().onSetAdjustedForIme(imeVisible, imeHeight);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getInputMethodWindowVisibleHeight() {
        InsetsState state = getInsetsStateController().getRawInsetsState();
        InsetsSource imeSource = state.peekSource(19);
        if (imeSource == null || !imeSource.isVisible()) {
            return 0;
        }
        Rect imeFrame = imeSource.getVisibleFrame() != null ? imeSource.getVisibleFrame() : imeSource.getFrame();
        Rect dockFrame = this.mTmpRect;
        dockFrame.set(state.getDisplayFrame());
        dockFrame.inset(state.calculateInsets(dockFrame, WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout(), false));
        return dockFrame.bottom - imeFrame.top;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void rotateBounds(int oldRotation, int newRotation, Rect inOutBounds) {
        getBounds(this.mTmpRect, oldRotation);
        RotationUtils.rotateBounds(inOutBounds, this.mTmpRect, oldRotation, newRotation);
    }

    public void setRotationAnimation(ScreenRotationAnimation screenRotationAnimation) {
        ScreenRotationAnimation screenRotationAnimation2 = this.mScreenRotationAnimation;
        if (screenRotationAnimation2 != null) {
            screenRotationAnimation2.kill();
        }
        this.mScreenRotationAnimation = screenRotationAnimation;
        if (screenRotationAnimation != null && screenRotationAnimation.hasScreenshot()) {
            startAsyncRotationIfNeeded();
        }
    }

    public ScreenRotationAnimation getRotationAnimation() {
        return this.mScreenRotationAnimation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestChangeTransitionIfNeeded(int changes, TransitionRequestInfo.DisplayChange displayChange) {
        if (this.mLastHasContent) {
            TransitionController controller = this.mTransitionController;
            if (controller.isCollecting()) {
                if (displayChange != null) {
                    throw new IllegalArgumentException("Provided displayChange for non-new transition");
                }
                if (!controller.isCollecting(this)) {
                    controller.collect(this);
                    startAsyncRotationIfNeeded();
                    return;
                }
                return;
            }
            Transition t = controller.requestTransitionIfNeeded(6, 0, this, this, null, displayChange);
            if (t != null) {
                this.mAtmService.startLaunchPowerMode(2);
                if (this.mFixedRotationLaunchingApp != null) {
                    t.setSeamlessRotation(this);
                    AsyncRotationController asyncRotationController = this.mAsyncRotationController;
                    if (asyncRotationController != null) {
                        asyncRotationController.keepAppearanceInPreviousRotation();
                    }
                } else if (isRotationChanging()) {
                    this.mWmService.mLatencyTracker.onActionStart(6);
                    controller.mTransitionMetricsReporter.associate(t, new LongConsumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda1
                        @Override // java.util.function.LongConsumer
                        public final void accept(long j) {
                            DisplayContent.this.m7935xd5cc814f(j);
                        }
                    });
                    startAsyncRotation(false);
                }
                t.setKnownConfigChanges(this, changes);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$requestChangeTransitionIfNeeded$19$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7935xd5cc814f(long startTime) {
        this.mWmService.mLatencyTracker.onActionEnd(6);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean inTransition() {
        return this.mScreenRotationAnimation != null || super.inTransition();
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        if (logLevel == 2 && !isVisible()) {
            return;
        }
        long token = proto.start(fieldId);
        super.dumpDebug(proto, 1146756268053L, logLevel);
        proto.write(1120986464258L, this.mDisplayId);
        proto.write(1120986464265L, this.mBaseDisplayDensity);
        this.mDisplayInfo.dumpDebug(proto, 1146756268042L);
        this.mDisplayRotation.dumpDebug(proto, 1146756268065L);
        ScreenRotationAnimation screenRotationAnimation = getRotationAnimation();
        if (screenRotationAnimation != null) {
            screenRotationAnimation.dumpDebug(proto, 1146756268044L);
        }
        this.mDisplayFrames.dumpDebug(proto, 1146756268045L);
        proto.write(1120986464295L, this.mMinSizeOfResizeableTaskDp);
        if (this.mTransitionController.isShellTransitionsEnabled()) {
            this.mTransitionController.dumpDebugLegacy(proto, 1146756268048L);
        } else {
            this.mAppTransition.dumpDebug(proto, 1146756268048L);
        }
        ActivityRecord activityRecord = this.mFocusedApp;
        if (activityRecord != null) {
            activityRecord.writeNameToProto(proto, 1138166333455L);
        }
        for (int i = this.mOpeningApps.size() - 1; i >= 0; i--) {
            this.mOpeningApps.valueAt(i).writeIdentifierToProto(proto, 2246267895825L);
        }
        for (int i2 = this.mClosingApps.size() - 1; i2 >= 0; i2--) {
            this.mClosingApps.valueAt(i2).writeIdentifierToProto(proto, 2246267895826L);
        }
        Task focusedRootTask = getFocusedRootTask();
        if (focusedRootTask != null) {
            proto.write(1120986464279L, focusedRootTask.getRootTaskId());
            ActivityRecord focusedActivity = focusedRootTask.getDisplayArea().getFocusedActivity();
            if (focusedActivity != null) {
                focusedActivity.writeIdentifierToProto(proto, 1146756268056L);
            }
        } else {
            proto.write(1120986464279L, -1);
        }
        proto.write(1133871366170L, isReady());
        proto.write(1133871366180L, isSleeping());
        for (int i3 = 0; i3 < this.mAllSleepTokens.size(); i3++) {
            this.mAllSleepTokens.get(i3).writeTagToProto(proto, 2237677961253L);
        }
        WindowState windowState = this.mImeLayeringTarget;
        if (windowState != null) {
            windowState.dumpDebug(proto, 1146756268059L, logLevel);
        }
        InputTarget inputTarget = this.mImeInputTarget;
        if (inputTarget != null) {
            inputTarget.dumpProto(proto, 1146756268060L, logLevel);
        }
        InsetsControlTarget insetsControlTarget = this.mImeControlTarget;
        if (insetsControlTarget != null && insetsControlTarget.getWindow() != null) {
            this.mImeControlTarget.getWindow().dumpDebug(proto, 1146756268061L, logLevel);
        }
        WindowState windowState2 = this.mCurrentFocus;
        if (windowState2 != null) {
            windowState2.dumpDebug(proto, 1146756268062L, logLevel);
        }
        if (this.mInsetsStateController != null) {
            int type = 0;
            while (type < 24) {
                WindowContainerInsetsSourceProvider provider = this.mInsetsStateController.peekSourceProvider(type);
                if (provider != null) {
                    provider.dumpDebug(proto, type == 19 ? 1146756268063L : 2246267895843L, logLevel);
                }
                type++;
            }
        }
        proto.write(1120986464290L, getImePolicy());
        for (Rect r : getKeepClearAreas()) {
            r.dumpDebug(proto, 2246267895846L);
        }
        proto.end(token);
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    long getProtoFieldId() {
        return 1146756268035L;
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
    public void dump(final PrintWriter pw, final String prefix, final boolean dumpAll) {
        super.dump(pw, prefix, dumpAll);
        pw.print(prefix);
        pw.println("Display: mDisplayId=" + this.mDisplayId + " rootTasks=" + getRootTaskCount());
        String subPrefix = "  " + prefix;
        pw.print(subPrefix);
        pw.print("init=");
        pw.print(this.mInitialDisplayWidth);
        pw.print("x");
        pw.print(this.mInitialDisplayHeight);
        pw.print(" ");
        pw.print(this.mInitialDisplayDensity);
        pw.print("dpi");
        pw.print(" mMinSizeOfResizeableTaskDp=");
        pw.print(this.mMinSizeOfResizeableTaskDp);
        if (this.mInitialDisplayWidth != this.mBaseDisplayWidth || this.mInitialDisplayHeight != this.mBaseDisplayHeight || this.mInitialDisplayDensity != this.mBaseDisplayDensity) {
            pw.print(" base=");
            pw.print(this.mBaseDisplayWidth);
            pw.print("x");
            pw.print(this.mBaseDisplayHeight);
            pw.print(" ");
            pw.print(this.mBaseDisplayDensity);
            pw.print("dpi");
        }
        if (this.mDisplayScalingDisabled) {
            pw.println(" noscale");
        }
        pw.print(" cur=");
        pw.print(this.mDisplayInfo.logicalWidth);
        pw.print("x");
        pw.print(this.mDisplayInfo.logicalHeight);
        pw.print(" app=");
        pw.print(this.mDisplayInfo.appWidth);
        pw.print("x");
        pw.print(this.mDisplayInfo.appHeight);
        pw.print(" rng=");
        pw.print(this.mDisplayInfo.smallestNominalAppWidth);
        pw.print("x");
        pw.print(this.mDisplayInfo.smallestNominalAppHeight);
        pw.print("-");
        pw.print(this.mDisplayInfo.largestNominalAppWidth);
        pw.print("x");
        pw.println(this.mDisplayInfo.largestNominalAppHeight);
        pw.print(subPrefix + "deferred=" + this.mDeferredRemoval + " mLayoutNeeded=" + this.mLayoutNeeded);
        pw.println(" mTouchExcludeRegion=" + this.mTouchExcludeRegion);
        pw.println();
        pw.print(prefix);
        pw.print("mLayoutSeq=");
        pw.println(this.mLayoutSeq);
        pw.print("  mCurrentFocus=");
        pw.println(this.mCurrentFocus);
        pw.print("  mFocusedApp=");
        pw.println(this.mFocusedApp);
        if (this.mFixedRotationLaunchingApp != null) {
            pw.println("  mFixedRotationLaunchingApp=" + this.mFixedRotationLaunchingApp);
        }
        pw.println();
        this.mWallpaperController.dump(pw, "  ");
        if (this.mSystemGestureExclusionListeners.getRegisteredCallbackCount() > 0) {
            pw.println();
            pw.print("  mSystemGestureExclusion=");
            pw.println(this.mSystemGestureExclusion);
        }
        Set<Rect> keepClearAreas = getKeepClearAreas();
        if (!keepClearAreas.isEmpty()) {
            pw.println();
            pw.print("  keepClearAreas=");
            pw.println(keepClearAreas);
        }
        pw.println();
        pw.println(prefix + "Display areas in top down Z order:");
        dumpChildDisplayArea(pw, subPrefix, dumpAll);
        pw.println();
        pw.println(prefix + "Task display areas in top down Z order:");
        forAllTaskDisplayAreas(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda31
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskDisplayArea taskDisplayArea = (TaskDisplayArea) obj;
                taskDisplayArea.dump(pw, prefix + "  ", dumpAll);
            }
        });
        pw.println();
        ScreenRotationAnimation rotationAnimation = getRotationAnimation();
        if (rotationAnimation != null) {
            pw.println("  mScreenRotationAnimation:");
            rotationAnimation.printTo(subPrefix, pw);
        } else if (dumpAll) {
            pw.println("  no ScreenRotationAnimation ");
        }
        pw.println();
        Task rootHomeTask = getDefaultTaskDisplayArea().getRootHomeTask();
        if (rootHomeTask != null) {
            pw.println(prefix + "rootHomeTask=" + rootHomeTask.getName());
        }
        Task rootPinnedTask = getDefaultTaskDisplayArea().getRootPinnedTask();
        if (rootPinnedTask != null) {
            pw.println(prefix + "rootPinnedTask=" + rootPinnedTask.getName());
        }
        Task rootRecentsTask = getDefaultTaskDisplayArea().getRootTask(0, 3);
        if (rootRecentsTask != null) {
            pw.println(prefix + "rootRecentsTask=" + rootRecentsTask.getName());
        }
        Task rootDreamTask = getRootTask(0, 5);
        if (rootDreamTask != null) {
            pw.println(prefix + "rootDreamTask=" + rootDreamTask.getName());
        }
        pw.println();
        this.mPinnedTaskController.dump(prefix, pw);
        pw.println();
        this.mDisplayFrames.dump(prefix, pw);
        pw.println();
        this.mDisplayPolicy.dump(prefix, pw);
        pw.println();
        this.mDisplayRotation.dump(prefix, pw);
        pw.println();
        this.mInputMonitor.dump(pw, "  ");
        pw.println();
        this.mInsetsStateController.dump(prefix, pw);
        this.mDwpcHelper.dump(prefix, pw);
    }

    @Override // com.android.server.wm.DisplayArea
    public String toString() {
        return "Display{#" + this.mDisplayId + " state=" + Display.stateToString(this.mDisplayInfo.state) + " size=" + this.mDisplayInfo.logicalWidth + "x" + this.mDisplayInfo.logicalHeight + " " + Surface.rotationToString(this.mDisplayInfo.rotation) + "}";
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.ConfigurationContainer
    String getName() {
        return "Display " + this.mDisplayId + " name=\"" + this.mDisplayInfo.name + "\"";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getTouchableWinAtPointLocked(float xf, float yf) {
        final int x = (int) xf;
        final int y = (int) yf;
        WindowState touchedWin = getWindow(new Predicate() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda37
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DisplayContent.this.m7921x5d2eb99a(x, y, (WindowState) obj);
            }
        });
        return touchedWin;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTouchableWinAtPointLocked$21$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ boolean m7921x5d2eb99a(int x, int y, WindowState w) {
        int flags = w.mAttrs.flags;
        if (w.isVisible() && (flags & 16) == 0) {
            w.getVisibleBounds(this.mTmpRect);
            if (this.mTmpRect.contains(x, y)) {
                w.getTouchableRegion(this.mTmpRegion);
                int touchFlags = flags & 40;
                return this.mTmpRegion.contains(x, y) || touchFlags == 0;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canAddToastWindowForUid(final int uid) {
        WindowState focusedWindowForUid = getWindow(new Predicate() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda50
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DisplayContent.lambda$canAddToastWindowForUid$22(uid, (WindowState) obj);
            }
        });
        if (focusedWindowForUid != null) {
            return true;
        }
        WindowState win = getWindow(new Predicate() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda51
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DisplayContent.lambda$canAddToastWindowForUid$23(uid, (WindowState) obj);
            }
        });
        return win == null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$canAddToastWindowForUid$22(int uid, WindowState w) {
        return w.mOwnerUid == uid && w.isFocused();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$canAddToastWindowForUid$23(int uid, WindowState w) {
        return w.mAttrs.type == 2005 && w.mOwnerUid == uid && !w.mPermanentlyHidden && !w.mWindowRemovalAllowed;
    }

    void scheduleToastWindowsTimeoutIfNeededLocked(WindowState oldFocus, WindowState newFocus) {
        if (oldFocus != null) {
            if (newFocus != null && newFocus.mOwnerUid == oldFocus.mOwnerUid) {
                return;
            }
            this.mTmpWindow = oldFocus;
            forAllWindows(this.mScheduleToastTimeout, false);
        }
    }

    WindowState findFocusedWindowIfNeeded(int topFocusedDisplayId) {
        if (this.mWmService.mPerDisplayFocusEnabled || topFocusedDisplayId == -1) {
            return findFocusedWindow();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState findFocusedWindow() {
        this.mTmpWindow = null;
        forAllWindows(this.mFindFocusedWindow, true);
        WindowState windowState = this.mTmpWindow;
        if (windowState == null) {
            if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                long protoLogParam0 = getDisplayId();
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, 620519522, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            return null;
        }
        return windowState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateFocusedWindowLocked(int mode, boolean updateInputWindows, int topFocusedDisplayId) {
        WindowState newFocus = findFocusedWindowIfNeeded(topFocusedDisplayId);
        if (this.mCurrentFocus == newFocus) {
            return false;
        }
        this.mIsFocusOnThunderbackWindow = ITranDisplayContent.Instance().isFocusOnThunderbackWindow("WindowManager", newFocus, this.mIsFocusOnThunderbackWindow);
        boolean imWindowChanged = false;
        WindowState imWindow = this.mInputMethodWindow;
        if (imWindow != null) {
            WindowState prevTarget = this.mImeLayeringTarget;
            WindowState newTarget = computeImeTarget(true);
            imWindowChanged = prevTarget != newTarget;
            if (mode != 1 && mode != 3) {
                assignWindowLayers(false);
            }
            if (imWindowChanged) {
                this.mWmService.mWindowsChanged = true;
                setLayoutNeeded();
                newFocus = findFocusedWindowIfNeeded(topFocusedDisplayId);
            }
        }
        if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
            String protoLogParam0 = String.valueOf(this.mCurrentFocus);
            String protoLogParam1 = String.valueOf(newFocus);
            long protoLogParam2 = getDisplayId();
            String protoLogParam3 = String.valueOf(Debug.getCallers(4));
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, 872933199, 16, (String) null, new Object[]{protoLogParam0, protoLogParam1, Long.valueOf(protoLogParam2), protoLogParam3});
        }
        Slog.w("WindowManager", "Changing focus from " + this.mCurrentFocus + " to " + newFocus + " displayId=" + getDisplayId() + " Callers=" + Debug.getCallers(4));
        WindowState oldFocus = this.mCurrentFocus;
        this.mCurrentFocus = newFocus;
        if (newFocus != null) {
            this.mWinAddedSinceNullFocus.clear();
            this.mWinRemovedSinceNullFocus.clear();
            if (newFocus.canReceiveKeys()) {
                newFocus.mToken.paused = false;
            }
        }
        getDisplayPolicy().focusChangedLw(oldFocus, newFocus);
        if (imWindowChanged && oldFocus != this.mInputMethodWindow) {
            if (mode == 2) {
                performLayout(true, updateInputWindows);
            } else if (mode == 3) {
                assignWindowLayers(false);
            }
        }
        if (mode != 1) {
            getInputMonitor().setInputFocusLw(newFocus, updateInputWindows);
        }
        adjustForImeIfNeeded();
        updateKeepClearAreas();
        scheduleToastWindowsTimeoutIfNeededLocked(oldFocus, newFocus);
        if (mode == 2) {
            this.pendingLayoutChanges |= 8;
        }
        if (this.mWmService.mAccessibilityController.hasCallbacks()) {
            this.mWmService.mH.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda30
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DisplayContent.this.updateAccessibilityOnWindowFocusChanged((AccessibilityController) obj);
                }
            }, this.mWmService.mAccessibilityController));
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAccessibilityOnWindowFocusChanged(AccessibilityController accessibilityController) {
        accessibilityController.onWindowFocusChangedNot(getDisplayId());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setFocusedApp(ActivityRecord newFocus) {
        if (newFocus != null) {
            DisplayContent appDisplay = newFocus.getDisplayContent();
            if (appDisplay != this) {
                throw new IllegalStateException(newFocus + " is not on " + getName() + " but " + (appDisplay != null ? appDisplay.getName() : "none"));
            }
            onLastFocusedTaskDisplayAreaChanged(newFocus.getDisplayArea());
        }
        if (this.mFocusedApp == newFocus) {
            return false;
        }
        if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
            String protoLogParam0 = String.valueOf(newFocus);
            long protoLogParam1 = getDisplayId();
            String protoLogParam2 = String.valueOf(Debug.getCallers(4));
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, -639217716, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), protoLogParam2});
        }
        ActivityRecord activityRecord = this.mFocusedApp;
        Task oldTask = activityRecord != null ? activityRecord.getTask() : null;
        Task newTask = newFocus != null ? newFocus.getTask() : null;
        this.mFocusedApp = newFocus;
        if (oldTask != newTask) {
            if (oldTask != null) {
                oldTask.onAppFocusChanged(false);
            }
            if (newTask != null) {
                newTask.onAppFocusChanged(true);
            }
            ActivityRecord newRecord = newTask != null ? newTask.topRunningActivityLocked() : null;
            ActivityRecord oldRecord = oldTask != null ? oldTask.topRunningActivityLocked() : null;
            ITranWindowManagerService.Instance().onUpdateFocusedApp(oldRecord != null ? oldRecord.packageName : null, oldRecord != null ? oldRecord.mActivityComponent : null, newRecord != null ? newRecord.packageName : null, newRecord != null ? newRecord.mActivityComponent : null);
        }
        getInputMonitor().setFocusedAppLw(newFocus);
        updateTouchExcludeRegion();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onRunningActivityChanged() {
        this.mDwpcHelper.onRunningActivityChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onLastFocusedTaskDisplayAreaChanged(TaskDisplayArea taskDisplayArea) {
        if (ITranDisplayContent.Instance().isMultiWindow(taskDisplayArea != null && taskDisplayArea.isMultiWindow())) {
            return;
        }
        if (taskDisplayArea != null && taskDisplayArea.handlesOrientationChangeFromDescendant()) {
            this.mOrientationRequestingTaskDisplayArea = taskDisplayArea;
            return;
        }
        TaskDisplayArea taskDisplayArea2 = this.mOrientationRequestingTaskDisplayArea;
        if (taskDisplayArea2 != null && !taskDisplayArea2.handlesOrientationChangeFromDescendant()) {
            this.mOrientationRequestingTaskDisplayArea = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskDisplayArea getOrientationRequestingTaskDisplayArea() {
        return this.mOrientationRequestingTaskDisplayArea;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assignWindowLayers(boolean setLayoutNeeded) {
        Trace.traceBegin(32L, "assignWindowLayers");
        assignChildLayers(getSyncTransaction());
        if (setLayoutNeeded) {
            setLayoutNeeded();
        }
        scheduleAnimation();
        Trace.traceEnd(32L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void layoutAndAssignWindowLayersIfNeeded() {
        this.mWmService.mWindowsChanged = true;
        setLayoutNeeded();
        if (!this.mWmService.updateFocusedWindowLocked(3, false)) {
            assignWindowLayers(false);
        }
        this.mInputMonitor.setUpdateInputWindowsNeededLw();
        this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
        this.mInputMonitor.updateInputWindowsLw(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean destroyLeakedSurfaces() {
        this.mTmpWindow = null;
        final SurfaceControl.Transaction t = this.mWmService.mTransactionFactory.get();
        forAllWindows(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7917xda5af571(t, (WindowState) obj);
            }
        }, false);
        t.apply();
        return this.mTmpWindow != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$destroyLeakedSurfaces$24$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7917xda5af571(SurfaceControl.Transaction t, WindowState w) {
        WindowStateAnimator wsa = w.mWinAnimator;
        if (wsa.mSurfaceController == null) {
            return;
        }
        if (!this.mWmService.mSessions.contains(wsa.mSession)) {
            Slog.w("WindowManager", "LEAKED SURFACE (session doesn't exist): " + w + " surface=" + wsa.mSurfaceController + " token=" + w.mToken + " pid=" + w.mSession.mPid + " uid=" + w.mSession.mUid);
            wsa.destroySurface(t);
            this.mWmService.mForceRemoves.add(w);
            this.mTmpWindow = w;
        } else if (w.mActivityRecord != null && !w.mActivityRecord.isClientVisible()) {
            Slog.w("WindowManager", "LEAKED SURFACE (app token hidden): " + w + " surface=" + wsa.mSurfaceController + " token=" + w.mActivityRecord);
            if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
                String protoLogParam0 = String.valueOf(w);
                ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, -1938839202, 0, (String) null, new Object[]{protoLogParam0});
            }
            wsa.destroySurface(t);
            this.mTmpWindow = w;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasAlertWindowSurfaces() {
        for (int i = this.mWmService.mSessions.size() - 1; i >= 0; i--) {
            if (this.mWmService.mSessions.valueAt(i).hasAlertWindowSurfaces(this)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInputMethodWindowLocked(WindowState win) {
        this.mInputMethodWindow = win;
        if (win != null) {
            int imePid = win.mSession.mPid;
            this.mAtmService.onImeWindowSetOnDisplayArea(imePid, this.mImeWindowsContainer);
        }
        this.mInsetsStateController.getSourceProvider(19).setWindowContainer(win, this.mDisplayPolicy.getImeSourceFrameProvider(), null);
        computeImeTarget(true);
        updateImeControlTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState computeImeTarget(boolean updateImeTarget) {
        if (this.mInputMethodWindow == null) {
            if (updateImeTarget) {
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    Slog.w("WindowManager", "Moving IM target from " + this.mImeLayeringTarget + " to null since mInputMethodWindow is null");
                }
                setImeLayeringTargetInner(null);
            }
            return null;
        }
        WindowState curTarget = this.mImeLayeringTarget;
        if (!canUpdateImeTarget()) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.w("WindowManager", "Defer updating IME target");
            }
            this.mUpdateImeRequestedWhileDeferred = true;
            return curTarget;
        }
        this.mUpdateImeTarget = updateImeTarget;
        WindowState target = getWindow(this.mComputeImeTargetPredicate);
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD && updateImeTarget) {
            Slog.v("WindowManager", "Proposed new IME target: " + target + " for display: " + getDisplayId());
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.v("WindowManager", "Desired input method target=" + target + " updateImeTarget=" + updateImeTarget);
        }
        if (target == null) {
            if (updateImeTarget) {
                if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                    Slog.w("WindowManager", "Moving IM target from " + curTarget + " to null." + (WindowManagerDebugConfig.SHOW_STACK_CRAWLS ? " Callers=" + Debug.getCallers(4) : ""));
                }
                setImeLayeringTargetInner(null);
            }
            return null;
        }
        if (updateImeTarget) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.w("WindowManager", "Moving IM target from " + curTarget + " to " + target + (WindowManagerDebugConfig.SHOW_STACK_CRAWLS ? " Callers=" + Debug.getCallers(4) : ""));
            }
            setImeLayeringTargetInner(target);
        }
        return target;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void computeImeTargetIfNeeded(ActivityRecord candidate) {
        WindowState windowState = this.mImeLayeringTarget;
        if (windowState != null && windowState.mActivityRecord == candidate) {
            computeImeTarget(true);
        }
    }

    private boolean isImeControlledByApp() {
        InputTarget inputTarget = this.mImeInputTarget;
        return inputTarget != null && inputTarget.shouldControlIme();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldImeAttachedToApp() {
        WindowState windowState;
        if (this.mImeWindowsContainer.isOrganized()) {
            return false;
        }
        boolean allowAttachToApp = this.mMagnificationSpec == null;
        boolean checkMultiDisplayArea = false;
        if (ThunderbackConfig.isVersion4()) {
            checkMultiDisplayArea = hasMultiDisplayArea();
        }
        return allowAttachToApp && isImeControlledByApp() && (windowState = this.mImeLayeringTarget) != null && windowState.mActivityRecord != null && this.mImeLayeringTarget.getWindowingMode() == 1 && ITranDisplayContent.Instance().shouldImeAttachedToApp(this.mImeLayeringTarget, this.mDettachImeWithActivity) && !checkMultiDisplayArea;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isImeAttachedToApp() {
        SurfaceControl surfaceControl;
        return shouldImeAttachedToApp() && (surfaceControl = this.mInputMethodSurfaceParent) != null && surfaceControl.isSameSurface(this.mImeLayeringTarget.mActivityRecord.getSurfaceControl());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsControlTarget getImeHostOrFallback(WindowState target) {
        if (target != null && target.getDisplayContent().getImePolicy() == 0) {
            return target;
        }
        return getImeFallback();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsControlTarget getImeFallback() {
        DisplayContent defaultDc = this.mWmService.getDefaultDisplayContentLocked();
        WindowState statusBar = defaultDc.getDisplayPolicy().getStatusBar();
        return statusBar != null ? statusBar : defaultDc.mRemoteInsetsControlTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InsetsControlTarget getImeTarget(int type) {
        switch (type) {
            case 0:
                return this.mImeLayeringTarget;
            case 1:
            default:
                return null;
            case 2:
                return this.mImeControlTarget;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputTarget getImeInputTarget() {
        return this.mImeInputTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getImePolicy() {
        if (isTrusted()) {
            int imePolicy = this.mWmService.mDisplayWindowSettings.getImePolicyLocked(this);
            if (imePolicy == 1 && forceDesktopMode()) {
                return 0;
            }
            return imePolicy;
        }
        return 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean forceDesktopMode() {
        return (!this.mWmService.mForceDesktopModeOnExternalDisplays || this.isDefaultDisplay || isPrivate()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onShowImeRequested() {
        WindowState windowState = this.mImeLayeringTarget;
        if (windowState != null && this.mInputMethodWindow != null && windowState.mToken.isFixedRotationTransforming()) {
            this.mInputMethodWindow.mToken.linkFixedRotationTransform(this.mImeLayeringTarget.mToken);
            AsyncRotationController asyncRotationController = this.mAsyncRotationController;
            if (asyncRotationController != null) {
                asyncRotationController.hideImmediately(this.mInputMethodWindow.mToken);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setImeLayeringTarget(WindowState target) {
        this.mImeLayeringTarget = target;
    }

    private void setImeLayeringTargetInner(WindowState target) {
        RootDisplayArea targetRoot;
        WindowState windowState = this.mImeLayeringTarget;
        if (target == windowState && this.mLastImeInputTarget == this.mImeInputTarget) {
            return;
        }
        InputTarget inputTarget = this.mImeInputTarget;
        this.mLastImeInputTarget = inputTarget;
        if (windowState != null && windowState == inputTarget) {
            boolean nonAppImeTargetAnimatingExit = windowState.mAnimatingExit && this.mImeLayeringTarget.mAttrs.type != 1 && this.mImeLayeringTarget.isSelfAnimating(0, 16);
            if (this.mImeLayeringTarget.inTransitionSelfOrParent() || nonAppImeTargetAnimatingExit) {
                showImeScreenshot();
            }
        }
        boolean nonAppImeTargetAnimatingExit2 = ProtoLogCache.WM_DEBUG_IME_enabled;
        if (nonAppImeTargetAnimatingExit2) {
            String protoLogParam0 = String.valueOf(target);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_IME, 2119122320, 0, (String) null, new Object[]{protoLogParam0});
        }
        boolean layeringTargetChanged = target != this.mImeLayeringTarget;
        this.mImeLayeringTarget = target;
        if (target != null && !this.mImeWindowsContainer.isOrganized() && (targetRoot = target.getRootDisplayArea()) != null && targetRoot != this.mImeWindowsContainer.getRootDisplayArea()) {
            targetRoot.placeImeContainer(this.mImeWindowsContainer);
            WindowState windowState2 = this.mInputMethodWindow;
            if (windowState2 != null) {
                windowState2.hide(false, false);
            }
        }
        assignWindowLayers(true);
        InsetsStateController insetsStateController = this.mInsetsStateController;
        insetsStateController.updateAboveInsetsState(insetsStateController.getRawInsetsState().getSourceOrDefaultVisibility(19));
        updateImeControlTarget(layeringTargetChanged);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setImeInputTarget(InputTarget target) {
        this.mImeInputTarget = target;
        if (refreshImeSecureFlag(getPendingTransaction())) {
            this.mWmService.requestTraversal();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean refreshImeSecureFlag(SurfaceControl.Transaction t) {
        InputTarget inputTarget = this.mImeInputTarget;
        boolean canScreenshot = inputTarget == null || inputTarget.canScreenshotIme();
        return this.mImeWindowsContainer.setCanScreenshot(t, canScreenshot);
    }

    void setImeControlTarget(InsetsControlTarget target) {
        this.mImeControlTarget = target;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class ImeScreenshot {
        private SurfaceControl mImeSurface;
        private WindowState mImeTarget;
        private SurfaceControl.Builder mSurfaceBuilder;

        ImeScreenshot(SurfaceControl.Builder surfaceBuilder, WindowState imeTarget) {
            this.mSurfaceBuilder = surfaceBuilder;
            this.mImeTarget = imeTarget;
        }

        WindowState getImeTarget() {
            return this.mImeTarget;
        }

        private SurfaceControl createImeSurface(SurfaceControl.ScreenshotHardwareBuffer b, SurfaceControl.Transaction t) {
            SurfaceControl imeParent;
            HardwareBuffer buffer = b.getHardwareBuffer();
            if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                String protoLogParam0 = String.valueOf(this.mImeTarget);
                String protoLogParam1 = String.valueOf(buffer.getWidth());
                String protoLogParam2 = String.valueOf(buffer.getHeight());
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_IME, -1777010776, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
            }
            WindowState imeWindow = this.mImeTarget.getDisplayContent().mInputMethodWindow;
            ActivityRecord activity = this.mImeTarget.mActivityRecord;
            if (this.mImeTarget.mAttrs.type == 1) {
                imeParent = activity.getSurfaceControl();
            } else {
                imeParent = this.mImeTarget.getSurfaceControl();
            }
            SurfaceControl imeSurface = this.mSurfaceBuilder.setName("IME-snapshot-surface").setBLASTLayer().setFormat(buffer.getFormat()).setParent(imeParent).setCallsite("DisplayContent.attachAndShowImeScreenshotOnTarget").build();
            InputMonitor.setTrustedOverlayInputInfo(imeSurface, t, imeWindow.getDisplayId(), "IME-snapshot-surface");
            t.setBuffer(imeSurface, buffer);
            t.setColorSpace(activity.mSurfaceControl, ColorSpace.get(ColorSpace.Named.SRGB));
            t.setLayer(imeSurface, 1);
            Point surfacePosition = new Point(imeWindow.getFrame().left, imeWindow.getFrame().top);
            if (imeParent != activity.getSurfaceControl()) {
                surfacePosition.offset(-this.mImeTarget.getFrame().left, -this.mImeTarget.getFrame().top);
                surfacePosition.offset(this.mImeTarget.mAttrs.surfaceInsets.left, this.mImeTarget.mAttrs.surfaceInsets.top);
                t.setPosition(imeSurface, surfacePosition.x, surfacePosition.y);
            } else {
                t.setPosition(imeSurface, surfacePosition.x, surfacePosition.y);
            }
            if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                long protoLogParam02 = surfacePosition.x;
                long protoLogParam12 = surfacePosition.y;
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_IME, -1814361639, 5, (String) null, new Object[]{Long.valueOf(protoLogParam02), Long.valueOf(protoLogParam12)});
            }
            return imeSurface;
        }

        private void removeImeSurface(SurfaceControl.Transaction t) {
            if (this.mImeSurface != null) {
                if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                    String protoLogParam0 = String.valueOf(Debug.getCallers(6));
                    ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_IME, -2111539867, 0, (String) null, new Object[]{protoLogParam0});
                }
                t.remove(this.mImeSurface);
                this.mImeSurface = null;
            }
        }

        void attachAndShow(SurfaceControl.Transaction t) {
            SurfaceControl.ScreenshotHardwareBuffer imeBuffer;
            DisplayContent dc = this.mImeTarget.getDisplayContent();
            Task task = this.mImeTarget.getTask();
            SurfaceControl surfaceControl = this.mImeSurface;
            boolean renewImeSurface = (surfaceControl != null && surfaceControl.getWidth() == dc.mInputMethodWindow.getFrame().width() && this.mImeSurface.getHeight() == dc.mInputMethodWindow.getFrame().height()) ? false : true;
            if (task != null && !task.isActivityTypeHomeOrRecents()) {
                if (renewImeSurface) {
                    imeBuffer = dc.mWmService.mTaskSnapshotController.snapshotImeFromAttachedTask(task);
                } else {
                    imeBuffer = null;
                }
                if (imeBuffer != null) {
                    removeImeSurface(t);
                    this.mImeSurface = createImeSurface(imeBuffer, t);
                }
            }
            SurfaceControl surfaceControl2 = this.mImeSurface;
            boolean isValidSnapshot = surfaceControl2 != null && surfaceControl2.isValid();
            if (isValidSnapshot && dc.getInsetsStateController().getImeSourceProvider().isImeShowing()) {
                if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                    String protoLogParam0 = String.valueOf(this.mImeTarget);
                    String protoLogParam1 = String.valueOf(Debug.getCallers(6));
                    ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_IME, -57750640, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
                }
                t.show(this.mImeSurface);
            } else if (!isValidSnapshot) {
                removeImeSurface(t);
            }
        }

        void detach(SurfaceControl.Transaction t) {
            removeImeSurface(t);
        }
    }

    private void attachAndShowImeScreenshotOnTarget() {
        if (!shouldImeAttachedToApp() || !this.mWmService.mPolicy.isScreenOn()) {
            return;
        }
        SurfaceControl.Transaction t = getPendingTransaction();
        WindowState windowState = this.mInputMethodWindow;
        if (windowState != null && windowState.isVisible()) {
            removeImeSurfaceImmediately();
            ImeScreenshot imeScreenshot = new ImeScreenshot(this.mWmService.mSurfaceControlFactory.apply(null), this.mImeLayeringTarget);
            this.mImeScreenshot = imeScreenshot;
            imeScreenshot.attachAndShow(t);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showImeScreenshot() {
        attachAndShowImeScreenshotOnTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeImeScreenshotIfPossible() {
        WindowState windowState = this.mImeLayeringTarget;
        if (windowState == null || (windowState.mAttrs.type != 3 && !this.mImeLayeringTarget.inTransitionSelfOrParent())) {
            removeImeSurfaceImmediately();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeImeSurfaceImmediately() {
        ImeScreenshot imeScreenshot = this.mImeScreenshot;
        if (imeScreenshot != null) {
            imeScreenshot.detach(getSyncTransaction());
            this.mImeScreenshot = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateImeInputAndControlTarget(InputTarget target) {
        SurfaceControl surfaceControl;
        if (this.mImeInputTarget != target) {
            boolean z = true;
            if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                String protoLogParam0 = String.valueOf(target);
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_IME, -322743468, 0, (String) null, new Object[]{protoLogParam0});
            }
            setImeInputTarget(target);
            InsetsStateController insetsStateController = this.mInsetsStateController;
            insetsStateController.updateAboveInsetsState(insetsStateController.getRawInsetsState().getSourceOrDefaultVisibility(19));
            if (this.mImeControlTarget != this.mRemoteInsetsControlTarget || (surfaceControl = this.mInputMethodSurfaceParent) == null || surfaceControl.isSameSurface(this.mImeWindowsContainer.getParent().mSurfaceControl)) {
                z = false;
            }
            boolean forceUpdateImeParent = z;
            updateImeControlTarget(forceUpdateImeParent);
        }
        if (target != null) {
            target.unfreezeInsetsAfterStartInput();
        }
    }

    void updateImeControlTarget() {
        updateImeControlTarget(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateImeControlTarget(boolean forceUpdateImeParent) {
        InsetsControlTarget prevImeControlTarget = this.mImeControlTarget;
        InsetsControlTarget computeImeControlTarget = computeImeControlTarget();
        this.mImeControlTarget = computeImeControlTarget;
        this.mInsetsStateController.onImeControlTargetChanged(computeImeControlTarget);
        boolean imeControlChanged = prevImeControlTarget != this.mImeControlTarget;
        if (imeControlChanged || forceUpdateImeParent) {
            updateImeParent();
        }
        WindowState win = InsetsControlTarget.asWindowOrNull(this.mImeControlTarget);
        final IBinder token = win != null ? win.mClient.asBinder() : null;
        this.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                InputMethodManagerInternal.get().reportImeControl(token);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateImeParent() {
        if (this.mImeWindowsContainer.isOrganized()) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.i("WindowManager", "ImeContainer is organized. Skip updateImeParent.");
            }
            this.mInputMethodSurfaceParent = null;
            return;
        }
        SurfaceControl newParent = computeImeParent();
        if (newParent != null && newParent != this.mInputMethodSurfaceParent) {
            this.mInputMethodSurfaceParent = newParent;
            getSyncTransaction().reparent(this.mImeWindowsContainer.mSurfaceControl, newParent);
            assignRelativeLayerForIme(getSyncTransaction(), true);
            scheduleAnimation();
            this.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    InputMethodManagerInternal.get().onImeParentChanged();
                }
            });
        }
    }

    InsetsControlTarget computeImeControlTarget() {
        InputTarget inputTarget;
        if ((!isImeControlledByApp() && this.mRemoteInsetsControlTarget != null) || ((inputTarget = this.mImeInputTarget) != null && getImeHostOrFallback(inputTarget.getWindowState()) == this.mRemoteInsetsControlTarget)) {
            return this.mRemoteInsetsControlTarget;
        }
        InputTarget inputTarget2 = this.mImeInputTarget;
        if (inputTarget2 != null) {
            return inputTarget2.getWindowState();
        }
        return null;
    }

    SurfaceControl computeImeParent() {
        WindowState windowState = this.mImeLayeringTarget;
        if (windowState != null) {
            boolean imeLayeringTargetMayUseIme = WindowManager.LayoutParams.mayUseInputMethod(windowState.mAttrs.flags) || this.mImeLayeringTarget.mAttrs.type == 3;
            if (imeLayeringTargetMayUseIme && this.mImeInputTarget != null && this.mImeLayeringTarget.mActivityRecord != this.mImeInputTarget.getActivityRecord()) {
                return null;
            }
        }
        boolean imeLayeringTargetMayUseIme2 = shouldImeAttachedToApp();
        if (imeLayeringTargetMayUseIme2) {
            return this.mImeLayeringTarget.mActivityRecord.getSurfaceControl();
        }
        if (this.mImeWindowsContainer.getParent() != null) {
            return this.mImeWindowsContainer.getParent().getSurfaceControl();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLayoutNeeded() {
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.w("WindowManager", "setLayoutNeeded: callers=" + Debug.getCallers(3));
        }
        this.mLayoutNeeded = true;
    }

    private void clearLayoutNeeded() {
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.w("WindowManager", "clearLayoutNeeded: callers=" + Debug.getCallers(3));
        }
        this.mLayoutNeeded = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLayoutNeeded() {
        return this.mLayoutNeeded;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpTokens(PrintWriter pw, boolean dumpAll) {
        if (this.mTokenMap.isEmpty()) {
            return;
        }
        pw.println("  Display #" + this.mDisplayId);
        for (WindowToken token : this.mTokenMap.values()) {
            pw.print("  ");
            pw.print(token);
            if (dumpAll) {
                pw.println(':');
                token.dump(pw, "    ", dumpAll);
            } else {
                pw.println();
            }
        }
        if (!this.mOpeningApps.isEmpty() || !this.mClosingApps.isEmpty() || !this.mChangingContainers.isEmpty()) {
            pw.println();
            if (this.mOpeningApps.size() > 0) {
                pw.print("  mOpeningApps=");
                pw.println(this.mOpeningApps);
            }
            if (this.mClosingApps.size() > 0) {
                pw.print("  mClosingApps=");
                pw.println(this.mClosingApps);
            }
            if (this.mChangingContainers.size() > 0) {
                pw.print("  mChangingApps=");
                pw.println(this.mChangingContainers);
            }
        }
        this.mUnknownAppVisibilityController.dump(pw, "  ");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpWindowAnimators(final PrintWriter pw, final String subPrefix) {
        final int[] index = new int[1];
        forAllWindows(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda65
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.lambda$dumpWindowAnimators$27(pw, subPrefix, index, (WindowState) obj);
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpWindowAnimators$27(PrintWriter pw, String subPrefix, int[] index, WindowState w) {
        WindowStateAnimator wAnim = w.mWinAnimator;
        pw.println(subPrefix + "Window #" + index[0] + ": " + wAnim);
        index[0] = index[0] + 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startKeyguardExitOnNonAppWindows(final boolean onWallpaper, final boolean goingToShade, final boolean subtle) {
        final WindowManagerPolicy policy = this.mWmService.mPolicy;
        forAllWindows(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.lambda$startKeyguardExitOnNonAppWindows$28(WindowManagerPolicy.this, onWallpaper, goingToShade, subtle, (WindowState) obj);
            }
        }, true);
        for (int i = this.mShellRoots.size() - 1; i >= 0; i--) {
            this.mShellRoots.valueAt(i).startAnimation(policy.createHiddenByKeyguardExit(onWallpaper, goingToShade, subtle));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$startKeyguardExitOnNonAppWindows$28(WindowManagerPolicy policy, boolean onWallpaper, boolean goingToShade, boolean subtle, WindowState w) {
        if (w.mActivityRecord == null && w.canBeHiddenByKeyguard() && w.wouldBeVisibleIfPolicyIgnored() && !w.isVisible()) {
            w.startAnimation(policy.createHiddenByKeyguardExit(onWallpaper, goingToShade, subtle));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldWaitForSystemDecorWindowsOnBoot() {
        if (this.isDefaultDisplay || supportsSystemDecorations()) {
            final SparseBooleanArray drawnWindowTypes = new SparseBooleanArray();
            drawnWindowTypes.put(2040, true);
            WindowState visibleNotDrawnWindow = getWindow(new Predicate() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda56
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return DisplayContent.this.m7936x8ee8e429(drawnWindowTypes, (WindowState) obj);
                }
            });
            if (visibleNotDrawnWindow != null) {
                return true;
            }
            boolean wallpaperEnabled = this.mWmService.mContext.getResources().getBoolean(17891651) && this.mWmService.mContext.getResources().getBoolean(17891578) && !this.mWmService.mOnlyCore;
            boolean haveBootMsg = drawnWindowTypes.get(2021);
            boolean haveApp = drawnWindowTypes.get(1);
            boolean haveWallpaper = drawnWindowTypes.get(2013);
            boolean haveKeyguard = drawnWindowTypes.get(2040);
            if (ProtoLogCache.WM_DEBUG_SCREEN_ON_enabled) {
                boolean protoLogParam0 = this.mWmService.mSystemBooted;
                boolean protoLogParam1 = this.mWmService.mShowingBootMessages;
                boolean protoLogParam5 = wallpaperEnabled;
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_SCREEN_ON, -635082269, 16383, (String) null, new Object[]{Boolean.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1), Boolean.valueOf(haveBootMsg), Boolean.valueOf(haveApp), Boolean.valueOf(haveWallpaper), Boolean.valueOf(protoLogParam5), Boolean.valueOf(haveKeyguard)});
            }
            if (!this.mWmService.mSystemBooted && !haveBootMsg) {
                return true;
            }
            if (this.mWmService.mSystemBooted) {
                if (haveApp || haveKeyguard) {
                    if (wallpaperEnabled && !haveWallpaper) {
                        return true;
                    }
                    return false;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$shouldWaitForSystemDecorWindowsOnBoot$29$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ boolean m7936x8ee8e429(SparseBooleanArray drawnWindowTypes, WindowState w) {
        boolean isVisible = w.isVisible() && !w.mObscured;
        boolean isDrawn = w.isDrawn();
        if (isVisible && !isDrawn) {
            if (ProtoLogCache.WM_DEBUG_BOOT_enabled) {
                long protoLogParam0 = w.mAttrs.type;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BOOT, -381475323, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            return true;
        }
        if (isDrawn) {
            switch (w.mAttrs.type) {
                case 1:
                case 2013:
                case 2021:
                    drawnWindowTypes.put(w.mAttrs.type, true);
                    break;
                case 2040:
                    drawnWindowTypes.put(2040, this.mWmService.mPolicy.isKeyguardDrawnLw());
                    break;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateWindowsForAnimator() {
        forAllWindows(this.mUpdateWindowsForAnimator, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInputMethodClientFocus(int uid, int pid) {
        WindowState imFocus = computeImeTarget(false);
        if (imFocus == null) {
            return false;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.i("WindowManager", "Desired input method target: " + imFocus);
            Slog.i("WindowManager", "Current focus: " + this.mCurrentFocus + " displayId=" + this.mDisplayId);
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
            Slog.i("WindowManager", "IM target uid/pid: " + imFocus.mSession.mUid + SliceClientPermissions.SliceAuthority.DELIMITER + imFocus.mSession.mPid);
            Slog.i("WindowManager", "Requesting client uid/pid: " + uid + SliceClientPermissions.SliceAuthority.DELIMITER + pid);
        }
        return imFocus.mSession.mUid == uid && imFocus.mSession.mPid == pid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$hasSecureWindowOnScreen$30(WindowState w) {
        return w.isOnScreen() && w.isSecureLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSecureWindowOnScreen() {
        WindowState win = getWindow(new Predicate() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda53
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DisplayContent.lambda$hasSecureWindowOnScreen$30((WindowState) obj);
            }
        });
        return win != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWindowFreezeTimeout() {
        Slog.w("WindowManager", "Window freeze timeout expired.");
        this.mWmService.mWindowsFreezingScreen = 2;
        forAllWindows(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda43
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7933xb86e64ce((WindowState) obj);
            }
        }, true);
        this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onWindowFreezeTimeout$31$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7933xb86e64ce(WindowState w) {
        if (!w.getOrientationChanging()) {
            return;
        }
        w.orientationChangeTimedOut();
        w.mLastFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mWmService.mDisplayFreezeTime);
        Slog.w("WindowManager", "Force clearing orientation change: " + w);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWindowAnimationFinished(WindowContainer wc, int type) {
        if (this.mImeScreenshot != null && ProtoLogCache.WM_DEBUG_IME_enabled) {
            String protoLogParam0 = String.valueOf(wc);
            String protoLogParam1 = String.valueOf(SurfaceAnimator.animationTypeToString(type));
            String protoLogParam2 = String.valueOf(this.mImeScreenshot);
            String protoLogParam3 = String.valueOf(this.mImeScreenshot.getImeTarget());
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_IME, -658964693, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2, protoLogParam3});
        }
        ImeScreenshot imeScreenshot = this.mImeScreenshot;
        if (imeScreenshot != null) {
            if ((wc == imeScreenshot.getImeTarget() || wc.getWindow(new Predicate() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda55
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return DisplayContent.this.m7932x6b2004b9(obj);
                }
            }) != null) && (type & 25) != 0) {
                removeImeSurfaceImmediately();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onWindowAnimationFinished$32$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ boolean m7932x6b2004b9(Object w) {
        return w == this.mImeScreenshot.getImeTarget();
    }

    private void checkTime(long startTime, String where) {
        long now = SystemClock.uptimeMillis();
        if (now - startTime > 20) {
            Slog.d("WindowManager", "System monitor slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applySurfaceChangesTransaction() {
        WindowSurfacePlacer surfacePlacer = this.mWmService.mWindowPlacerLocked;
        this.mTmpUpdateAllDrawn.clear();
        if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
            surfacePlacer.debugLayoutRepeats("On entry to LockedInner", this.pendingLayoutChanges);
        }
        long applyPostStartTime = 0;
        long applySurfaceStartTime = 0;
        if ((this.pendingLayoutChanges & 4) != 0) {
            this.mWallpaperController.adjustWallpaperWindows();
        }
        if ((this.pendingLayoutChanges & 2) != 0) {
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.v("WindowManager", "Computing new config from layout");
            }
            if (updateOrientation()) {
                setLayoutNeeded();
                sendNewConfiguration();
            }
        }
        if ((this.pendingLayoutChanges & 1) != 0) {
            setLayoutNeeded();
        }
        performLayout(true, false);
        this.pendingLayoutChanges = 0;
        if (Build.IS_DEBUG_ENABLE) {
            applyPostStartTime = SystemClock.uptimeMillis();
        }
        Trace.traceBegin(32L, "applyPostLayoutPolicy");
        try {
            this.mDisplayPolicy.beginPostLayoutPolicyLw();
            forAllWindows(this.mApplyPostLayoutPolicy, true);
            this.mDisplayPolicy.finishPostLayoutPolicyLw();
            Trace.traceEnd(32L);
            if (Build.IS_DEBUG_ENABLE) {
                checkTime(applyPostStartTime, "applySurfaceChangesTransaction after applyPostLayoutPolicy pendingLayoutChanges=" + this.pendingLayoutChanges);
            }
            this.mInsetsStateController.onPostLayout();
            this.mTmpApplySurfaceChangesTransactionState.reset();
            if (Build.IS_DEBUG_ENABLE) {
                applySurfaceStartTime = SystemClock.uptimeMillis();
            }
            Trace.traceBegin(32L, "applyWindowSurfaceChanges");
            try {
                this.mOverlayDialog = false;
                resetSecureState();
                forAllWindows(this.mApplySurfaceChangesTransaction, true);
                hookSecureWindowVisible();
                Trace.traceEnd(32L);
                if (Build.IS_DEBUG_ENABLE) {
                    checkTime(applySurfaceStartTime, "applySurfaceChangesTransaction after applyWindowSurfaceChanges");
                }
                prepareSurfaces();
                this.mInsetsStateController.getImeSourceProvider().checkShowImePostLayout();
                this.mLastHasContent = this.mTmpApplySurfaceChangesTransactionState.displayHasContent;
                if (!this.mWmService.mDisplayFrozen) {
                    this.mWmService.mDisplayManagerInternal.setDisplayProperties(this.mDisplayId, this.mLastHasContent, this.mTmpApplySurfaceChangesTransactionState.preferredRefreshRate, this.mTmpApplySurfaceChangesTransactionState.preferredModeId, this.mTmpApplySurfaceChangesTransactionState.preferredMinRefreshRate, this.mTmpApplySurfaceChangesTransactionState.preferredMaxRefreshRate, this.mTmpApplySurfaceChangesTransactionState.preferMinimalPostProcessing, true);
                }
                updateRecording();
                boolean wallpaperVisible = this.mWallpaperController.isWallpaperVisible();
                if (wallpaperVisible != this.mLastWallpaperVisible) {
                    this.mLastWallpaperVisible = wallpaperVisible;
                    this.mWmService.mWallpaperVisibilityListeners.notifyWallpaperVisibilityChanged(this);
                }
                while (!this.mTmpUpdateAllDrawn.isEmpty()) {
                    ActivityRecord activity = this.mTmpUpdateAllDrawn.removeLast();
                    activity.updateAllDrawn();
                }
            } finally {
            }
        } finally {
        }
    }

    private void getBounds(Rect out, int rotation) {
        getBounds(out);
        int currentRotation = this.mDisplayInfo.rotation;
        int rotationDelta = RotationUtils.deltaRotation(currentRotation, rotation);
        if (rotationDelta == 1 || rotationDelta == 3) {
            out.set(0, 0, out.height(), out.width());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getNaturalOrientation() {
        return this.mBaseDisplayWidth < this.mBaseDisplayHeight ? 1 : 2;
    }

    void performLayout(boolean initial, boolean updateInputWindows) {
        Trace.traceBegin(32L, "performLayout");
        try {
            performLayoutNoTrace(initial, updateInputWindows);
        } finally {
            Trace.traceEnd(32L);
        }
    }

    private void performLayoutNoTrace(boolean initial, boolean updateInputWindows) {
        if (!isLayoutNeeded()) {
            return;
        }
        clearLayoutNeeded();
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.v("WindowManager", "-------------------------------------");
            Slog.v("WindowManager", "performLayout: dw=" + this.mDisplayInfo.logicalWidth + " dh=" + this.mDisplayInfo.logicalHeight);
        }
        int seq = this.mLayoutSeq + 1;
        if (seq < 0) {
            seq = 0;
        }
        this.mLayoutSeq = seq;
        this.mTmpInitial = initial;
        forAllWindows(this.mPerformLayout, true);
        forAllWindows(this.mPerformLayoutAttached, true);
        this.mInputMonitor.setUpdateInputWindowsNeededLw();
        if (updateInputWindows) {
            this.mInputMonitor.updateInputWindowsLw(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bitmap screenshotDisplayLocked() {
        if (!this.mWmService.mPolicy.isScreenOn()) {
            if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                Slog.i("WindowManager", "Attempted to take screenshot while display was off.");
            }
            return null;
        }
        boolean inRotation = false;
        ScreenRotationAnimation screenRotationAnimation = this.mWmService.mRoot.getDisplayContent(0).getRotationAnimation();
        if (screenRotationAnimation != null && screenRotationAnimation.isAnimating()) {
            inRotation = true;
        }
        if (WindowManagerDebugConfig.DEBUG_SCREENSHOT && inRotation) {
            Slog.v("WindowManager", "Taking screenshot while rotating");
        }
        IBinder displayToken = SurfaceControl.getInternalDisplayToken();
        SurfaceControl.DisplayCaptureArgs captureArgs = new SurfaceControl.DisplayCaptureArgs.Builder(displayToken).setUseIdentityTransform(inRotation).build();
        SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer = SurfaceControl.captureDisplay(captureArgs);
        Bitmap bitmap = screenshotBuffer == null ? null : screenshotBuffer.asBitmap();
        if (bitmap == null) {
            Slog.w("WindowManager", "Failed to take screenshot");
            return null;
        }
        Bitmap ret = bitmap.asShared();
        if (ret != bitmap) {
            bitmap.recycle();
        }
        return ret;
    }

    @Override // com.android.server.wm.WindowContainer
    void onDescendantOverrideConfigurationChanged() {
        setLayoutNeeded();
        this.mWmService.requestTraversal();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean okToDisplay() {
        return okToDisplay(false, false);
    }

    boolean okToDisplay(boolean ignoreFrozen, boolean ignoreScreenOn) {
        return this.mDisplayId == 0 ? (!this.mWmService.mDisplayFrozen || ignoreFrozen) && this.mWmService.mDisplayEnabled && (ignoreScreenOn || this.mWmService.mPolicy.isScreenOn()) : this.mDisplayInfo.state == 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean okToAnimate(boolean ignoreFrozen, boolean ignoreScreenOn) {
        return okToDisplay(ignoreFrozen, ignoreScreenOn) && (this.mDisplayId != 0 || this.mWmService.mPolicy.okToAnimate(ignoreScreenOn)) && getDisplayPolicy().isScreenOnFully();
    }

    /* loaded from: classes2.dex */
    static final class TaskForResizePointSearchResult implements Predicate<Task> {
        private int delta;
        private Rect mTmpRect = new Rect();
        private Task taskForResize;
        private int x;
        private int y;

        TaskForResizePointSearchResult() {
        }

        Task process(WindowContainer root, int x, int y, int delta) {
            this.taskForResize = null;
            this.x = x;
            this.y = y;
            this.delta = delta;
            this.mTmpRect.setEmpty();
            root.forAllTasks(this);
            return this.taskForResize;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(Task task) {
            if (task.getRootTask().getWindowConfiguration().canResizeTask() && task.getWindowingMode() != 1) {
                if (!task.isOrganized() || task.getWindowingMode() == 5) {
                    task.getDimBounds(this.mTmpRect);
                    Rect rect = this.mTmpRect;
                    int i = this.delta;
                    rect.inset(-i, -i);
                    if (this.mTmpRect.contains(this.x, this.y)) {
                        Rect rect2 = this.mTmpRect;
                        int i2 = this.delta;
                        rect2.inset(i2, i2);
                        if (this.mTmpRect.contains(this.x, this.y)) {
                            return true;
                        }
                        this.taskForResize = task;
                        return true;
                    }
                    return false;
                }
                return true;
            }
            return true;
        }
    }

    /* loaded from: classes2.dex */
    private static final class ApplySurfaceChangesTransactionState {
        public boolean displayHasContent;
        public boolean obscured;
        public boolean preferMinimalPostProcessing;
        public float preferredMaxRefreshRate;
        public float preferredMinRefreshRate;
        public int preferredModeId;
        public float preferredRefreshRate;
        public boolean syswin;

        private ApplySurfaceChangesTransactionState() {
        }

        void reset() {
            this.displayHasContent = false;
            this.obscured = false;
            this.syswin = false;
            this.preferMinimalPostProcessing = false;
            this.preferredRefreshRate = 0.0f;
            this.preferredModeId = 0;
            this.preferredMinRefreshRate = 0.0f;
            this.preferredMaxRefreshRate = 0.0f;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ImeContainer extends DisplayArea.Tokens {
        boolean mNeedsLayer;

        ImeContainer(WindowManagerService wms) {
            super(wms, DisplayArea.Type.ABOVE_TASKS, "ImeContainer", 8);
            this.mNeedsLayer = false;
        }

        public void setNeedsLayer() {
            this.mNeedsLayer = true;
        }

        @Override // com.android.server.wm.DisplayArea.Tokens, com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer
        int getOrientation(int candidate) {
            if (getIgnoreOrientationRequest()) {
                return -2;
            }
            return candidate;
        }

        @Override // com.android.server.wm.WindowContainer
        void updateAboveInsetsState(InsetsState aboveInsetsState, SparseArray<InsetsSourceProvider> localInsetsSourceProvidersFromParent, ArraySet<WindowState> insetsChangedWindows) {
            if (skipImeWindowsDuringTraversal(this.mDisplayContent)) {
                return;
            }
            super.updateAboveInsetsState(aboveInsetsState, localInsetsSourceProvidersFromParent, insetsChangedWindows);
        }

        @Override // com.android.server.wm.WindowContainer
        boolean forAllWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
            DisplayContent dc = this.mDisplayContent;
            if (skipImeWindowsDuringTraversal(dc)) {
                return false;
            }
            return super.forAllWindows(callback, traverseTopToBottom);
        }

        private static boolean skipImeWindowsDuringTraversal(DisplayContent dc) {
            return (dc.mImeLayeringTarget == null || dc.mWmService.mDisplayFrozen) ? false : true;
        }

        boolean forAllWindowForce(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
            return super.forAllWindows(callback, traverseTopToBottom);
        }

        @Override // com.android.server.wm.WindowContainer
        void assignLayer(SurfaceControl.Transaction t, int layer) {
            if (!this.mNeedsLayer) {
                return;
            }
            super.assignLayer(t, layer);
            this.mNeedsLayer = false;
        }

        @Override // com.android.server.wm.WindowContainer
        void assignRelativeLayer(SurfaceControl.Transaction t, SurfaceControl relativeTo, int layer, boolean forceUpdate) {
            if (!this.mNeedsLayer) {
                return;
            }
            super.assignRelativeLayer(t, relativeTo, layer, forceUpdate);
            this.mNeedsLayer = false;
        }

        @Override // com.android.server.wm.DisplayArea
        void setOrganizer(IDisplayAreaOrganizer organizer, boolean skipDisplayAreaAppeared) {
            super.setOrganizer(organizer, skipDisplayAreaAppeared);
            this.mDisplayContent.updateImeParent();
            if (organizer != null) {
                SurfaceControl imeParentSurfaceControl = getParentSurfaceControl();
                if (this.mSurfaceControl != null && imeParentSurfaceControl != null) {
                    if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                        String protoLogParam0 = String.valueOf(imeParentSurfaceControl);
                        ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_IME, 1175495463, 0, (String) null, new Object[]{protoLogParam0});
                    }
                    getPendingTransaction().reparent(this.mSurfaceControl, imeParentSurfaceControl);
                } else if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                    String protoLogParam02 = String.valueOf(this.mSurfaceControl);
                    String protoLogParam1 = String.valueOf(imeParentSurfaceControl);
                    ProtoLogImpl.e(ProtoLogGroup.WM_DEBUG_IME, -81121442, 0, (String) null, new Object[]{protoLogParam02, protoLogParam1});
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public SurfaceSession getSession() {
        return this.mSession;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public SurfaceControl.Builder makeChildSurface(WindowContainer child) {
        SurfaceSession s = child != null ? child.getSession() : getSession();
        SurfaceControl.Builder b = this.mWmService.makeSurfaceBuilder(s).setContainerLayer();
        if (child == null) {
            return b;
        }
        return b.setName(child.getName()).setParent(this.mSurfaceControl);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl.Builder makeOverlay() {
        return this.mWmService.makeSurfaceBuilder(this.mSession).setParent(getOverlayLayer());
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl.Builder makeAnimationLeash() {
        return this.mWmService.makeSurfaceBuilder(this.mSession).setParent(this.mSurfaceControl).setContainerLayer();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reparentToOverlay(SurfaceControl.Transaction transaction, SurfaceControl surface) {
        transaction.reparent(surface, getOverlayLayer());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyMagnificationSpec(MagnificationSpec spec) {
        if (spec.scale != 1.0d) {
            this.mMagnificationSpec = spec;
        } else {
            this.mMagnificationSpec = null;
        }
        updateImeParent();
        if (spec.scale != 1.0d) {
            applyMagnificationSpec(getPendingTransaction(), spec);
        } else {
            clearMagnificationSpec(getPendingTransaction());
        }
        getPendingTransaction().apply();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reapplyMagnificationSpec() {
        if (this.mMagnificationSpec != null) {
            applyMagnificationSpec(getPendingTransaction(), this.mMagnificationSpec);
        }
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    void onParentChanged(ConfigurationContainer newParent, ConfigurationContainer oldParent) {
        if (!isReady()) {
            this.mDisplayReady = true;
            this.mWmService.mAnimator.addDisplayLocked(this.mDisplayId);
            if (this.mWmService.mDisplayManagerInternal != null) {
                this.mWmService.mDisplayManagerInternal.setDisplayInfoOverrideFromWindowManager(this.mDisplayId, getDisplayInfo());
                configureDisplayPolicy();
            }
            reconfigureDisplayLocked();
            onRequestedOverrideConfigurationChanged(getRequestedOverrideConfiguration());
            this.mWmService.mDisplayNotificationController.dispatchDisplayAdded(this);
            this.mWmService.mWindowContextListenerController.registerWindowContainerListener(getDisplayUiContext().getWindowContextToken(), this, 1000, -1, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void assignChildLayers(SurfaceControl.Transaction t) {
        assignRelativeLayerForIme(t, false);
        super.assignChildLayers(t);
    }

    private void assignRelativeLayerForIme(SurfaceControl.Transaction t, boolean forceUpdate) {
        if (this.mImeWindowsContainer.isOrganized()) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.i("WindowManager", "ImeContainer is organized. Skip assignRelativeLayerForIme.");
                return;
            }
            return;
        }
        this.mImeWindowsContainer.setNeedsLayer();
        WindowState imeTarget = this.mImeLayeringTarget;
        if (imeTarget != null && (imeTarget.mActivityRecord == null || !imeTarget.mActivityRecord.hasStartingWindow())) {
            InsetsControlTarget insetsControlTarget = this.mImeControlTarget;
            WindowToken imeControlTargetToken = (insetsControlTarget == null || insetsControlTarget.getWindow() == null) ? null : this.mImeControlTarget.getWindow().mToken;
            boolean checkMultiDisplayArea = false;
            boolean canImeTargetSetRelativeLayer = false;
            int targetLayerLevel = this.mWmService.mPolicy.getWindowLayerFromTypeLw(imeTarget.mAttrs.type, false);
            int imeLayerLevel = this.mWmService.mPolicy.getWindowLayerFromTypeLw(2011);
            if (ThunderbackConfig.isVersion4()) {
                checkMultiDisplayArea = hasMultiDisplayArea() && targetLayerLevel < imeLayerLevel;
            }
            if (imeTarget.getSurfaceControl() != null && imeTarget.mToken == imeControlTargetToken && !imeTarget.inMultiWindowMode() && !checkMultiDisplayArea) {
                canImeTargetSetRelativeLayer = true;
            }
            if (canImeTargetSetRelativeLayer) {
                this.mImeWindowsContainer.assignRelativeLayer(t, imeTarget.getSurfaceControl(), 1, forceUpdate);
                return;
            }
        }
        SurfaceControl surfaceControl = this.mInputMethodSurfaceParent;
        if (surfaceControl != null) {
            this.mImeWindowsContainer.assignRelativeLayer(t, surfaceControl, 1, forceUpdate);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assignRelativeLayerForImeTargetChild(SurfaceControl.Transaction t, WindowContainer child) {
        child.assignRelativeLayer(t, this.mImeWindowsContainer.getSurfaceControl(), 1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.DisplayArea.Dimmable, com.android.server.wm.WindowContainer
    public void prepareSurfaces() {
        Trace.traceBegin(32L, "prepareSurfaces");
        try {
            SurfaceControl.Transaction transaction = getPendingTransaction();
            super.prepareSurfaces();
            SurfaceControl.mergeToGlobalTransaction(transaction);
        } finally {
            Trace.traceEnd(32L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void deferUpdateImeTarget() {
        int i = this.mDeferUpdateImeTargetCount;
        if (i == 0) {
            this.mUpdateImeRequestedWhileDeferred = false;
        }
        this.mDeferUpdateImeTargetCount = i + 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void continueUpdateImeTarget() {
        int i = this.mDeferUpdateImeTargetCount;
        if (i == 0) {
            return;
        }
        int i2 = i - 1;
        this.mDeferUpdateImeTargetCount = i2;
        if (i2 == 0 && this.mUpdateImeRequestedWhileDeferred) {
            computeImeTarget(true);
        }
    }

    private boolean canUpdateImeTarget() {
        return this.mDeferUpdateImeTargetCount == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputMonitor getInputMonitor() {
        return this.mInputMonitor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getLastHasContent() {
        return this.mLastHasContent;
    }

    void setLastHasContent() {
        this.mLastHasContent = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerPointerEventListener(WindowManagerPolicyConstants.PointerEventListener listener) {
        this.mPointerEventDispatcher.registerInputEventListener(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterPointerEventListener(WindowManagerPolicyConstants.PointerEventListener listener) {
        this.mPointerEventDispatcher.unregisterInputEventListener(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void transferAppTransitionFrom(DisplayContent from) {
        boolean prepared = this.mAppTransition.transferFrom(from.mAppTransition);
        if (prepared && okToAnimate()) {
            this.mSkipAppTransitionAnimation = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Deprecated
    public void prepareAppTransition(int transit) {
        prepareAppTransition(transit, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Deprecated
    public void prepareAppTransition(int transit, int flags) {
        boolean prepared = this.mAppTransition.prepareAppTransition(transit, flags);
        if (prepared && okToAnimate() && transit != 0) {
            this.mSkipAppTransitionAnimation = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestTransitionAndLegacyPrepare(int transit, int flags) {
        prepareAppTransition(transit, flags);
        this.mTransitionController.requestTransitionIfNeeded(transit, flags, null, this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestTransitionAndLegacyPrepare(int transit, WindowContainer trigger) {
        prepareAppTransition(transit);
        this.mTransitionController.requestTransitionIfNeeded(transit, 0, trigger, this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void executeAppTransition() {
        this.mTransitionController.setReady(this);
        if (this.mAppTransition.isTransitionSet()) {
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                String protoLogParam0 = String.valueOf(this.mAppTransition);
                long protoLogParam1 = this.mDisplayId;
                String protoLogParam2 = String.valueOf(Debug.getCallers(5));
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 1166381079, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), protoLogParam2});
            }
            this.mAppTransition.setReady();
            this.mWmService.mWindowPlacerLocked.requestTraversal();
        }
    }

    void cancelAppTransition() {
        if (!this.mAppTransition.isTransitionSet() || this.mAppTransition.isRunning()) {
            return;
        }
        this.mAppTransition.abort();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleAnimatingStoppedAndTransition() {
        this.mAppTransition.setIdle();
        for (int i = this.mNoAnimationNotifyOnTransitionFinished.size() - 1; i >= 0; i--) {
            IBinder token = this.mNoAnimationNotifyOnTransitionFinished.get(i);
            this.mAppTransition.notifyAppTransitionFinishedLocked(token);
        }
        this.mNoAnimationNotifyOnTransitionFinished.clear();
        this.mWallpaperController.hideDeferredWallpapersIfNeededLegacy();
        onAppTransitionDone();
        int changes = 0 | 1;
        if (ProtoLogCache.WM_DEBUG_WALLPAPER_enabled) {
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WALLPAPER, -182877285, 0, (String) null, (Object[]) null);
        }
        computeImeTarget(true);
        this.mWallpaperMayChange = true;
        this.mWmService.mFocusMayChange = true;
        this.pendingLayoutChanges |= changes;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNextTransitionForward() {
        if (!this.mTransitionController.isShellTransitionsEnabled()) {
            return this.mAppTransition.containsTransitRequest(1) || this.mAppTransition.containsTransitRequest(3);
        }
        int type = this.mTransitionController.getCollectingTransitionType();
        return type == 1 || type == 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsSystemDecorations() {
        return (this.mWmService.mDisplayWindowSettings.shouldShowSystemDecorsLocked(this) || (this.mDisplay.getFlags() & 64) != 0 || forceDesktopMode()) && this.mDisplayId != this.mWmService.mVr2dDisplayId && isTrusted();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl getWindowingLayer() {
        return this.mWindowingLayer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayArea.Tokens getImeContainer() {
        return this.mImeWindowsContainer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl getOverlayLayer() {
        return this.mOverlayLayer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl[] findRoundedCornerOverlays() {
        List<SurfaceControl> roundedCornerOverlays = new ArrayList<>();
        for (WindowToken token : this.mTokenMap.values()) {
            if (token.mRoundedCornerOverlay) {
                roundedCornerOverlays.add(token.mSurfaceControl);
            }
        }
        return (SurfaceControl[]) roundedCornerOverlays.toArray(new SurfaceControl[0]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateSystemGestureExclusion() {
        if (this.mSystemGestureExclusionListeners.getRegisteredCallbackCount() == 0) {
            return false;
        }
        Region systemGestureExclusion = Region.obtain();
        this.mSystemGestureExclusionWasRestricted = calculateSystemGestureExclusion(systemGestureExclusion, this.mSystemGestureExclusionUnrestricted);
        try {
            if (this.mSystemGestureExclusion.equals(systemGestureExclusion)) {
                return false;
            }
            this.mSystemGestureExclusion.set(systemGestureExclusion);
            Region unrestrictedOrNull = this.mSystemGestureExclusionWasRestricted ? this.mSystemGestureExclusionUnrestricted : null;
            for (int i = this.mSystemGestureExclusionListeners.beginBroadcast() - 1; i >= 0; i--) {
                try {
                    this.mSystemGestureExclusionListeners.getBroadcastItem(i).onSystemGestureExclusionChanged(this.mDisplayId, systemGestureExclusion, unrestrictedOrNull);
                } catch (RemoteException e) {
                    Slog.e("WindowManager", "Failed to notify SystemGestureExclusionListener", e);
                }
            }
            this.mSystemGestureExclusionListeners.finishBroadcast();
            return true;
        } finally {
            systemGestureExclusion.recycle();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean calculateSystemGestureExclusion(final Region outExclusion, final Region outExclusionUnrestricted) {
        outExclusion.setEmpty();
        if (outExclusionUnrestricted != null) {
            outExclusionUnrestricted.setEmpty();
        }
        final Region unhandled = Region.obtain();
        unhandled.set(0, 0, this.mDisplayFrames.mDisplayWidth, this.mDisplayFrames.mDisplayHeight);
        final Rect leftEdge = this.mInsetsStateController.getSourceProvider(5).getSource().getFrame();
        final Rect rightEdge = this.mInsetsStateController.getSourceProvider(6).getSource().getFrame();
        final Region touchableRegion = Region.obtain();
        final Region local = Region.obtain();
        int i = this.mSystemGestureExclusionLimit;
        final int[] remainingLeftRight = {i, i};
        final RecentsAnimationController recentsAnimationController = this.mWmService.getRecentsAnimationController();
        forAllWindows(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda34
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.lambda$calculateSystemGestureExclusion$33(RecentsAnimationController.this, unhandled, touchableRegion, local, remainingLeftRight, outExclusion, leftEdge, rightEdge, outExclusionUnrestricted, (WindowState) obj);
            }
        }, true);
        local.recycle();
        touchableRegion.recycle();
        unhandled.recycle();
        int i2 = remainingLeftRight[0];
        int i3 = this.mSystemGestureExclusionLimit;
        return i2 < i3 || remainingLeftRight[1] < i3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$calculateSystemGestureExclusion$33(RecentsAnimationController recentsAnimationController, Region unhandled, Region touchableRegion, Region local, int[] remainingLeftRight, Region outExclusion, Rect leftEdge, Rect rightEdge, Region outExclusionUnrestricted, WindowState w) {
        boolean ignoreRecentsAnimationTarget = recentsAnimationController != null && recentsAnimationController.shouldApplyInputConsumer(w.getActivityRecord());
        if (w.canReceiveTouchInput() && w.isVisible()) {
            if ((w.getAttrs().flags & 16) == 0 && !unhandled.isEmpty() && !ignoreRecentsAnimationTarget) {
                w.getEffectiveTouchableRegion(touchableRegion);
                touchableRegion.op(unhandled, Region.Op.INTERSECT);
                if (w.isImplicitlyExcludingAllSystemGestures()) {
                    local.set(touchableRegion);
                } else {
                    RegionUtils.rectListToRegion(w.getSystemGestureExclusion(), local);
                    local.scale(w.mGlobalScale);
                    Rect frame = w.getWindowFrames().mFrame;
                    local.translate(frame.left, frame.top);
                    local.op(touchableRegion, Region.Op.INTERSECT);
                }
                if (needsGestureExclusionRestrictions(w, false)) {
                    remainingLeftRight[0] = addToGlobalAndConsumeLimit(local, outExclusion, leftEdge, remainingLeftRight[0], w, 0);
                    remainingLeftRight[1] = addToGlobalAndConsumeLimit(local, outExclusion, rightEdge, remainingLeftRight[1], w, 1);
                    Region middle = Region.obtain(local);
                    middle.op(leftEdge, Region.Op.DIFFERENCE);
                    middle.op(rightEdge, Region.Op.DIFFERENCE);
                    outExclusion.op(middle, Region.Op.UNION);
                    middle.recycle();
                } else {
                    boolean loggable = needsGestureExclusionRestrictions(w, true);
                    if (loggable) {
                        addToGlobalAndConsumeLimit(local, outExclusion, leftEdge, Integer.MAX_VALUE, w, 0);
                        addToGlobalAndConsumeLimit(local, outExclusion, rightEdge, Integer.MAX_VALUE, w, 1);
                    }
                    outExclusion.op(local, Region.Op.UNION);
                }
                if (outExclusionUnrestricted != null) {
                    outExclusionUnrestricted.op(local, Region.Op.UNION);
                }
                unhandled.op(touchableRegion, Region.Op.DIFFERENCE);
            }
        }
    }

    private static boolean needsGestureExclusionRestrictions(WindowState win, boolean ignoreRequest) {
        int type = win.mAttrs.type;
        boolean stickyHideNav = !win.getRequestedVisibility(1) && win.mAttrs.insetsFlags.behavior == 2;
        return ((stickyHideNav && !ignoreRequest) || type == 2011 || type == 2040 || win.getActivityType() == 2) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean logsGestureExclusionRestrictions(WindowState win) {
        WindowManager.LayoutParams attrs;
        int type;
        return win.mWmService.mConstants.mSystemGestureExclusionLogDebounceTimeoutMillis > 0 && (type = (attrs = win.getAttrs()).type) != 2013 && type != 3 && type != 2019 && (attrs.flags & 16) == 0 && needsGestureExclusionRestrictions(win, true) && win.getDisplayContent().mDisplayPolicy.hasSideGestures();
    }

    private static int addToGlobalAndConsumeLimit(Region local, final Region global, Rect edge, int limit, WindowState win, int side) {
        Region r = Region.obtain(local);
        r.op(edge, Region.Op.INTERSECT);
        final int[] remaining = {limit};
        final int[] requestedExclusion = {0};
        RegionUtils.forEachRectReverse(r, new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda44
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.lambda$addToGlobalAndConsumeLimit$34(remaining, requestedExclusion, global, (Rect) obj);
            }
        });
        int grantedExclusion = limit - remaining[0];
        win.setLastExclusionHeights(side, requestedExclusion[0], grantedExclusion);
        r.recycle();
        return remaining[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addToGlobalAndConsumeLimit$34(int[] remaining, int[] requestedExclusion, Region global, Rect rect) {
        if (remaining[0] <= 0) {
            return;
        }
        int height = rect.height();
        requestedExclusion[0] = requestedExclusion[0] + height;
        if (height > remaining[0]) {
            rect.top = rect.bottom - remaining[0];
        }
        remaining[0] = remaining[0] - height;
        global.op(rect, Region.Op.UNION);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerSystemGestureExclusionListener(ISystemGestureExclusionListener listener) {
        boolean changed;
        this.mSystemGestureExclusionListeners.register(listener);
        if (this.mSystemGestureExclusionListeners.getRegisteredCallbackCount() == 1) {
            changed = updateSystemGestureExclusion();
        } else {
            changed = false;
        }
        if (!changed) {
            Region unrestrictedOrNull = this.mSystemGestureExclusionWasRestricted ? this.mSystemGestureExclusionUnrestricted : null;
            try {
                listener.onSystemGestureExclusionChanged(this.mDisplayId, this.mSystemGestureExclusion, unrestrictedOrNull);
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Failed to notify SystemGestureExclusionListener during register", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterSystemGestureExclusionListener(ISystemGestureExclusionListener listener) {
        this.mSystemGestureExclusionListeners.unregister(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateKeepClearAreas() {
        Set<Rect> restrictedKeepClearAreas = new ArraySet<>();
        Set<Rect> unrestrictedKeepClearAreas = new ArraySet<>();
        getKeepClearAreas(restrictedKeepClearAreas, unrestrictedKeepClearAreas);
        if (!this.mRestrictedKeepClearAreas.equals(restrictedKeepClearAreas) || !this.mUnrestrictedKeepClearAreas.equals(unrestrictedKeepClearAreas)) {
            this.mRestrictedKeepClearAreas = restrictedKeepClearAreas;
            this.mUnrestrictedKeepClearAreas = unrestrictedKeepClearAreas;
            this.mWmService.mDisplayNotificationController.dispatchKeepClearAreasChanged(this, restrictedKeepClearAreas, unrestrictedKeepClearAreas);
        }
    }

    void getKeepClearAreas(final Set<Rect> outRestricted, final Set<Rect> outUnrestricted) {
        final Matrix tmpMatrix = new Matrix();
        final float[] tmpFloat9 = new float[9];
        forAllWindows(new ToBooleanFunction() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda40
            public final boolean apply(Object obj) {
                return DisplayContent.lambda$getKeepClearAreas$35(outRestricted, outUnrestricted, tmpMatrix, tmpFloat9, (WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getKeepClearAreas$35(Set outRestricted, Set outUnrestricted, Matrix tmpMatrix, float[] tmpFloat9, WindowState w) {
        if (w.isVisible() && !w.inPinnedWindowingMode()) {
            w.getKeepClearAreas(outRestricted, outUnrestricted, tmpMatrix, tmpFloat9);
        }
        return w.getWindowType() == 1 && w.getWindowingMode() == 1;
    }

    Set<Rect> getKeepClearAreas() {
        Set<Rect> keepClearAreas = new ArraySet<>();
        getKeepClearAreas(keepClearAreas, keepClearAreas);
        return keepClearAreas;
    }

    protected MetricsLogger getMetricsLogger() {
        if (this.mMetricsLogger == null) {
            this.mMetricsLogger = new MetricsLogger();
        }
        return this.mMetricsLogger;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayChanged() {
        int lastDisplayState = this.mDisplayInfo.state;
        updateDisplayInfo();
        int displayId = this.mDisplay.getDisplayId();
        int displayState = this.mDisplayInfo.state;
        if (displayId != 0) {
            if (displayState == 1) {
                this.mOffTokenAcquirer.acquire(this.mDisplayId);
            } else if (displayState == 2) {
                this.mOffTokenAcquirer.release(this.mDisplayId);
            }
            if (ProtoLogCache.WM_DEBUG_CONTENT_RECORDING_enabled) {
                long protoLogParam0 = this.mDisplayId;
                long protoLogParam1 = displayState;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING, -381522987, 5, "Display %d state is now (%d), so update recording?", new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1)});
            }
            if (lastDisplayState != displayState) {
                updateRecording();
            }
        }
        if (Display.isSuspendedState(lastDisplayState) && !Display.isSuspendedState(displayState) && displayState != 0) {
            this.mWmService.mWindowContextListenerController.dispatchPendingConfigurationIfNeeded(this.mDisplayId);
        }
        this.mWmService.requestTraversal();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean alwaysCreateRootTask(int windowingMode, int activityType) {
        return activityType == 1 && (windowingMode == 1 || windowingMode == 5 || windowingMode == 2 || windowingMode == 6);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getFocusedRootTask() {
        return (Task) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda61
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((TaskDisplayArea) obj).getFocusedRootTask();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRootTasksInWindowingModes(final int... windowingModes) {
        if (windowingModes == null || windowingModes.length == 0) {
            return;
        }
        final ArrayList<Task> rootTasks = new ArrayList<>();
        forAllRootTasks(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda62
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.lambda$removeRootTasksInWindowingModes$36(windowingModes, rootTasks, (Task) obj);
            }
        });
        for (int i = rootTasks.size() - 1; i >= 0; i--) {
            this.mRootWindowContainer.mTaskSupervisor.removeRootTask(rootTasks.get(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$removeRootTasksInWindowingModes$36(int[] windowingModes, ArrayList rootTasks, Task rootTask) {
        for (int windowingMode : windowingModes) {
            if (!rootTask.mCreatedByOrganizer && rootTask.getWindowingMode() == windowingMode && rootTask.isActivityTypeStandardOrUndefined()) {
                rootTasks.add(rootTask);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRootTasksWithActivityTypes(final int... activityTypes) {
        if (activityTypes == null || activityTypes.length == 0) {
            return;
        }
        final ArrayList<Task> rootTasks = new ArrayList<>();
        forAllRootTasks(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda36
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.lambda$removeRootTasksWithActivityTypes$37(activityTypes, rootTasks, (Task) obj);
            }
        });
        for (int i = rootTasks.size() - 1; i >= 0; i--) {
            this.mRootWindowContainer.mTaskSupervisor.removeRootTask(rootTasks.get(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$removeRootTasksWithActivityTypes$37(int[] activityTypes, ArrayList rootTasks, Task rootTask) {
        for (int activityType : activityTypes) {
            if (rootTask.mCreatedByOrganizer) {
                for (int k = rootTask.getChildCount() - 1; k >= 0; k--) {
                    Task task = (Task) rootTask.getChildAt(k);
                    if (task.getActivityType() == activityType) {
                        rootTasks.add(task);
                    }
                }
            } else if (rootTask.getActivityType() == activityType) {
                rootTasks.add(rootTask);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topRunningActivity() {
        return topRunningActivity(false);
    }

    ActivityRecord topRunningActivity(final boolean considerKeyguardState) {
        return (ActivityRecord) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda63
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                ActivityRecord activityRecord;
                activityRecord = ((TaskDisplayArea) obj).topRunningActivity(considerKeyguardState);
                return activityRecord;
            }
        });
    }

    boolean updateDisplayOverrideConfigurationLocked() {
        RecentsAnimationController recentsAnimationController = this.mWmService.getRecentsAnimationController();
        if (recentsAnimationController != null) {
            recentsAnimationController.cancelAnimationForDisplayChange();
        }
        Configuration values = new Configuration();
        computeScreenConfiguration(values);
        this.mAtmService.mH.sendMessage(PooledLambda.obtainMessage(new ActivityTaskManagerService$$ExternalSyntheticLambda16(), this.mAtmService.mAmInternal, Integer.valueOf(this.mDisplayId)));
        Settings.System.clearConfiguration(values);
        updateDisplayOverrideConfigurationLocked(values, null, false, this.mAtmService.mTmpUpdateConfigurationResult);
        return this.mAtmService.mTmpUpdateConfigurationResult.changes != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateDisplayOverrideConfigurationLocked(Configuration values, ActivityRecord starting, boolean deferResume, ActivityTaskManagerService.UpdateConfigurationResult result) {
        int changes = 0;
        boolean kept = true;
        this.mAtmService.deferWindowLayout();
        if (values != null) {
            try {
                if (this.mDisplayId == 0) {
                    changes = this.mAtmService.updateGlobalConfigurationLocked(values, false, false, -10000);
                } else {
                    changes = performDisplayOverrideConfigUpdate(values);
                }
            } catch (Throwable th) {
                this.mAtmService.continueWindowLayout();
                throw th;
            }
        }
        if (!deferResume) {
            kept = this.mAtmService.ensureConfigAndVisibilityAfterUpdate(starting, changes);
        }
        this.mAtmService.continueWindowLayout();
        if (result != null) {
            result.changes = changes;
            result.activityRelaunched = kept ? false : true;
        }
        return kept;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int performDisplayOverrideConfigUpdate(Configuration values) {
        this.mTempConfig.setTo(getRequestedOverrideConfiguration());
        int changes = this.mTempConfig.updateFrom(values);
        if (changes != 0) {
            Slog.i("WindowManager", "Override config changes=" + Integer.toHexString(changes) + " " + this.mTempConfig + " for displayId=" + this.mDisplayId);
            onRequestedOverrideConfigurationChanged(this.mTempConfig);
            boolean isDensityChange = (changes & 4096) != 0;
            if (isDensityChange && this.mDisplayId == 0) {
                this.mAtmService.mAppWarnings.onDensityChanged();
                Message msg = PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda35
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((ActivityManagerInternal) obj).killAllBackgroundProcessesExcept(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
                    }
                }, this.mAtmService.mAmInternal, 24, 6);
                this.mAtmService.mH.sendMessage(msg);
            }
            this.mWmService.mDisplayNotificationController.dispatchDisplayChanged(this, getConfiguration());
            if (isReady() && this.mTransitionController.isShellTransitionsEnabled()) {
                requestChangeTransitionIfNeeded(changes, null);
            }
        }
        return changes;
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void onRequestedOverrideConfigurationChanged(Configuration overrideConfiguration) {
        Configuration currOverrideConfig = getRequestedOverrideConfiguration();
        int currRotation = currOverrideConfig.windowConfiguration.getRotation();
        int overrideRotation = overrideConfiguration.windowConfiguration.getRotation();
        if (currRotation != -1 && overrideRotation != -1 && currRotation != overrideRotation) {
            applyRotationAndFinishFixedRotation(currRotation, overrideRotation);
        }
        this.mCurrentOverrideConfigurationChanges = currOverrideConfig.diff(overrideConfiguration);
        super.onRequestedOverrideConfigurationChanged(overrideConfiguration);
        this.mCurrentOverrideConfigurationChanges = 0;
        if (this.mWaitingForConfig) {
            this.mWaitingForConfig = false;
            this.mWmService.mLastFinishedFreezeSource = "new-config";
        }
        this.mAtmService.addWindowLayoutReasons(1);
    }

    @Override // com.android.server.wm.WindowContainer
    void onResize() {
        super.onResize();
        if (this.mWmService.mAccessibilityController.hasCallbacks()) {
            this.mWmService.mAccessibilityController.onDisplaySizeChanged(this);
        }
    }

    private void applyRotationAndFinishFixedRotation(final int oldRotation, final int newRotation) {
        WindowToken rotatedLaunchingApp = this.mFixedRotationLaunchingApp;
        if (rotatedLaunchingApp == null) {
            m7916xf847ed3f(oldRotation, newRotation);
            return;
        }
        rotatedLaunchingApp.finishFixedRotationTransform(new Runnable() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda48
            @Override // java.lang.Runnable
            public final void run() {
                DisplayContent.this.m7916xf847ed3f(oldRotation, newRotation);
            }
        });
        setFixedRotationLaunchingAppUnchecked(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleActivitySizeCompatModeIfNeeded(ActivityRecord r) {
        Task organizedTask = r.getOrganizedTask();
        if (organizedTask == null) {
            this.mActiveSizeCompatActivities.remove(r);
        } else if (r.isState(ActivityRecord.State.RESUMED) && r.inSizeCompatMode()) {
            if (this.mActiveSizeCompatActivities.add(r)) {
                organizedTask.onSizeCompatActivityChanged();
            }
        } else if (this.mActiveSizeCompatActivities.remove(r)) {
            organizedTask.onSizeCompatActivityChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUidPresent(int uid) {
        Predicate<ActivityRecord> obtainPredicate = PooledLambda.obtainPredicate(new DisplayContent$$ExternalSyntheticLambda11(), PooledLambda.__(ActivityRecord.class), Integer.valueOf(uid));
        boolean isUidPresent = this.mDisplayContent.getActivity(obtainPredicate) != null;
        obtainPredicate.recycle();
        return isUidPresent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRemoved() {
        return this.mRemoved;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRemoving() {
        return this.mRemoving;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void remove() {
        this.mRemoving = true;
        this.mRootWindowContainer.mTaskSupervisor.beginDeferResume();
        try {
            Task lastReparentedRootTask = (Task) reduceOnAllTaskDisplayAreas(new BiFunction() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda5
                @Override // java.util.function.BiFunction
                public final Object apply(Object obj, Object obj2) {
                    return DisplayContent.lambda$remove$40((TaskDisplayArea) obj, (Task) obj2);
                }
            }, null, false);
            this.mRootWindowContainer.mTaskSupervisor.endDeferResume();
            this.mRemoved = true;
            ContentRecorder contentRecorder = this.mContentRecorder;
            if (contentRecorder != null) {
                contentRecorder.remove();
            }
            if (lastReparentedRootTask != null) {
                lastReparentedRootTask.resumeNextFocusAfterReparent();
            }
            setRemoteInsetsController(null);
            releaseSelfIfNeeded();
            this.mDisplayPolicy.release();
            if (!this.mAllSleepTokens.isEmpty()) {
                this.mAllSleepTokens.forEach(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda6
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        DisplayContent.this.m7934lambda$remove$41$comandroidserverwmDisplayContent((RootWindowContainer.SleepToken) obj);
                    }
                });
                this.mAllSleepTokens.clear();
                this.mAtmService.updateSleepIfNeededLocked();
            }
        } catch (Throwable th) {
            this.mRootWindowContainer.mTaskSupervisor.endDeferResume();
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Task lambda$remove$40(TaskDisplayArea taskDisplayArea, Task rootTask) {
        Task lastReparentedRootTaskFromArea = taskDisplayArea.remove();
        if (lastReparentedRootTaskFromArea != null) {
            return lastReparentedRootTaskFromArea;
        }
        return rootTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$remove$41$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7934lambda$remove$41$comandroidserverwmDisplayContent(RootWindowContainer.SleepToken token) {
        this.mRootWindowContainer.mSleepTokens.remove(token.mHashKey);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void releaseSelfIfNeeded() {
        if (!this.mRemoved) {
            return;
        }
        boolean hasNonEmptyHomeRootTask = forAllRootTasks(new Predicate() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda59
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DisplayContent.lambda$releaseSelfIfNeeded$42((Task) obj);
            }
        });
        if (!hasNonEmptyHomeRootTask && getRootTaskCount() > 0) {
            forAllRootTasks(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda60
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((Task) obj).removeIfPossible("releaseSelfIfNeeded");
                }
            });
        } else if (getTopRootTask() == null) {
            removeIfPossible();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$releaseSelfIfNeeded$42(Task rootTask) {
        return !rootTask.isActivityTypeHome() || rootTask.hasChild();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IntArray getPresentUIDs() {
        this.mDisplayAccessUIDs.clear();
        Consumer<ActivityRecord> obtainConsumer = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda9
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                DisplayContent.addActivityUid((ActivityRecord) obj, (IntArray) obj2);
            }
        }, PooledLambda.__(ActivityRecord.class), this.mDisplayAccessUIDs);
        this.mDisplayContent.forAllActivities(obtainConsumer);
        obtainConsumer.recycle();
        return this.mDisplayAccessUIDs;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void addActivityUid(ActivityRecord r, IntArray uids) {
        uids.add(r.getUid());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldDestroyContentOnRemove() {
        return this.mDisplay.getRemoveMode() == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldSleep() {
        return (getRootTaskCount() == 0 || !(this.mAllSleepTokens.isEmpty() || this.mRootWindowContainer.mTaskSupervisor.getKeyguardController().isKeyguardGoingAwayQuickly())) && this.mAtmService.mRunningVoice == null && !ITranWindowManagerService.Instance().isCurrentActivityKeepAwake(null, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void ensureActivitiesVisible(final ActivityRecord starting, final int configChanges, final boolean preserveWindows, final boolean notifyClients) {
        if (this.mInEnsureActivitiesVisible) {
            return;
        }
        this.mInEnsureActivitiesVisible = true;
        this.mAtmService.mTaskSupervisor.beginActivityVisibilityUpdate();
        try {
            forAllRootTasks(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda46
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((Task) obj).ensureActivitiesVisible(ActivityRecord.this, configChanges, preserveWindows, notifyClients);
                }
            });
            if (this.mTransitionController.isCollecting() && this.mWallpaperController.getWallpaperTarget() != null) {
                this.mWallpaperController.adjustWallpaperWindows();
            }
        } finally {
            this.mAtmService.mTaskSupervisor.endActivityVisibilityUpdate();
            this.mInEnsureActivitiesVisible = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSleeping() {
        return this.mSleeping;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsSleeping(boolean asleep) {
        this.mSleeping = asleep;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyKeyguardFlagsChanged() {
        if (!isKeyguardLocked()) {
            return;
        }
        boolean wasTransitionSet = this.mAppTransition.isTransitionSet();
        if (!wasTransitionSet) {
            prepareAppTransition(0);
        }
        this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
        if (!wasTransitionSet) {
            executeAppTransition();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canShowWithInsecureKeyguard() {
        int flags = this.mDisplay.getFlags();
        return (flags & 32) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardLocked() {
        return this.mRootWindowContainer.mTaskSupervisor.getKeyguardController().isKeyguardLocked(this.mDisplayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardGoingAway() {
        return this.mRootWindowContainer.mTaskSupervisor.getKeyguardController().isKeyguardGoingAway(this.mDisplayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardAlwaysUnlocked() {
        return (this.mDisplayInfo.flags & 512) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAodShowing() {
        return this.mRootWindowContainer.mTaskSupervisor.getKeyguardController().isAodShowing(this.mDisplayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardOccluded() {
        return this.mRootWindowContainer.mTaskSupervisor.getKeyguardController().isDisplayOccluded(this.mDisplayId);
    }

    void removeAllTasks() {
        forAllTasks(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda42
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                r1.getRootTask().removeChild((Task) obj, "removeAllTasks");
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Context getDisplayUiContext() {
        return this.mDisplayPolicy.getSystemUiContext();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.DisplayArea
    public boolean setIgnoreOrientationRequest(boolean ignoreOrientationRequest) {
        if (this.mSetIgnoreOrientationRequest == ignoreOrientationRequest) {
            return false;
        }
        boolean rotationChanged = super.setIgnoreOrientationRequest(ignoreOrientationRequest);
        this.mWmService.mDisplayWindowSettings.setIgnoreOrientationRequest(this, this.mSetIgnoreOrientationRequest);
        return rotationChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onIsIgnoreOrientationRequestDisabledChanged() {
        ActivityRecord activityRecord = this.mFocusedApp;
        if (activityRecord != null) {
            onLastFocusedTaskDisplayAreaChanged(activityRecord.getDisplayArea());
        }
        if (this.mSetIgnoreOrientationRequest) {
            updateOrientation();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState findScrollCaptureTargetWindow(WindowState searchBehind, int taskId) {
        return getWindow(new Predicate<WindowState>(searchBehind, taskId) { // from class: com.android.server.wm.DisplayContent.1
            boolean behindTopWindow;
            final /* synthetic */ WindowState val$searchBehind;
            final /* synthetic */ int val$taskId;

            {
                this.val$searchBehind = searchBehind;
                this.val$taskId = taskId;
                this.behindTopWindow = searchBehind == null;
            }

            /* JADX DEBUG: Method merged with bridge method */
            @Override // java.util.function.Predicate
            public boolean test(WindowState nextWindow) {
                if (!this.behindTopWindow) {
                    if (nextWindow == this.val$searchBehind) {
                        this.behindTopWindow = true;
                    }
                    return false;
                }
                if (this.val$taskId == -1) {
                    if (!nextWindow.canReceiveKeys()) {
                        return false;
                    }
                } else {
                    Task task = nextWindow.getTask();
                    if (task == null || !task.isTaskId(this.val$taskId)) {
                        return false;
                    }
                }
                return !nextWindow.isSecureLocked() && nextWindow.isVisible();
            }
        });
    }

    @Override // com.android.server.wm.DisplayArea, com.android.server.wm.ConfigurationContainer
    public boolean providesMaxBounds() {
        return true;
    }

    void setSandboxDisplayApis(boolean sandboxDisplayApis) {
        this.mSandboxDisplayApis = sandboxDisplayApis;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean sandboxDisplayApis() {
        return this.mSandboxDisplayApis;
    }

    private ContentRecorder getContentRecorder() {
        if (this.mContentRecorder == null) {
            this.mContentRecorder = new ContentRecorder(this);
        }
        return this.mContentRecorder;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pauseRecording() {
        ContentRecorder contentRecorder = this.mContentRecorder;
        if (contentRecorder != null) {
            contentRecorder.pauseRecording();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setContentRecordingSession(ContentRecordingSession session) {
        getContentRecorder().setContentRecordingSession(session);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateRecording() {
        getContentRecorder().updateRecording();
    }

    boolean isCurrentlyRecording() {
        ContentRecorder contentRecorder = this.mContentRecorder;
        return contentRecorder != null && contentRecorder.isCurrentlyRecording();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class FixedRotationTransitionListener extends WindowManagerInternal.AppTransitionListener {
        private ActivityRecord mAnimatingRecents;
        private boolean mRecentsWillBeTop;

        FixedRotationTransitionListener() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void onStartRecentsAnimation(ActivityRecord r) {
            this.mAnimatingRecents = r;
            if (r.isVisible() && DisplayContent.this.mFocusedApp != null && !DisplayContent.this.mFocusedApp.occludesParent()) {
                return;
            }
            DisplayContent.this.rotateInDifferentOrientationIfNeeded(r);
            if (r.hasFixedRotationTransform()) {
                DisplayContent.this.setFixedRotationLaunchingApp(r, r.getWindowConfiguration().getRotation());
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void onFinishRecentsAnimation() {
            ActivityRecord animatingRecents = this.mAnimatingRecents;
            boolean recentsWillBeTop = this.mRecentsWillBeTop;
            this.mAnimatingRecents = null;
            this.mRecentsWillBeTop = false;
            if (recentsWillBeTop) {
                return;
            }
            if (animatingRecents != null && animatingRecents == DisplayContent.this.mFixedRotationLaunchingApp && animatingRecents.isVisible() && animatingRecents != DisplayContent.this.topRunningActivity()) {
                DisplayContent.this.mPendingFinishFixedRotationApp = animatingRecents;
                DisplayContent.this.setFixedRotationLaunchingAppUnchecked(null);
                return;
            }
            DisplayContent.this.continueUpdateOrientationForDiffOrienLaunchingApp();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void notifyRecentsWillBeTop() {
            this.mRecentsWillBeTop = true;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public boolean shouldDeferRotation() {
            ActivityRecord source = null;
            if (DisplayContent.this.mTransitionController.isShellTransitionsEnabled()) {
                ActivityRecord r = DisplayContent.this.mFixedRotationLaunchingApp;
                if (r != null && DisplayContent.this.mTransitionController.isTransientLaunch(r)) {
                    source = r;
                }
            } else if (this.mAnimatingRecents != null && !DisplayContent.this.hasTopFixedRotationLaunchingApp()) {
                source = this.mAnimatingRecents;
            }
            if (source == null || source.getRequestedConfigurationOrientation(true) == 0) {
                return false;
            }
            return DisplayContent.this.mWmService.mPolicy.okToAnimate(false);
        }

        @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
        public void onAppTransitionFinishedLocked(IBinder token) {
            ActivityRecord activityRecord;
            ActivityRecord r = DisplayContent.this.getActivityRecord(token);
            if (DisplayContent.this.mPendingFinishFixedRotationApp != null) {
                ActivityRecord pendingApp = DisplayContent.this.mPendingFinishFixedRotationApp;
                DisplayContent.this.mPendingFinishFixedRotationApp = null;
                if (DisplayContent.this.mFixedRotationLaunchingApp != null && r == pendingApp && r != this.mAnimatingRecents) {
                    Slog.d("WindowManager", "finish fixed rotation transform for the pending app: " + pendingApp);
                    r.finishFixedRotationTransform();
                    return;
                }
            }
            if (r == null || r == (activityRecord = this.mAnimatingRecents)) {
                return;
            }
            if (activityRecord != null && this.mRecentsWillBeTop) {
                return;
            }
            if (DisplayContent.this.mFixedRotationLaunchingApp == null) {
                r.finishFixedRotationTransform();
                return;
            }
            if (DisplayContent.this.mFixedRotationLaunchingApp.hasFixedRotationTransform(r)) {
                if (DisplayContent.this.mFixedRotationLaunchingApp.hasAnimatingFixedRotationTransition()) {
                    return;
                }
            } else {
                Task task = r.getTask();
                if (task == null || task != DisplayContent.this.mFixedRotationLaunchingApp.getTask() || task.isAppTransitioning()) {
                    return;
                }
            }
            Slog.d("WindowManager", "AppTransitionFinished done ,the activityRecord is " + r);
            DisplayContent.this.continueUpdateOrientationForDiffOrienLaunchingApp();
        }

        @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
        public void onAppTransitionCancelledLocked(boolean keyguardGoingAway) {
            if (DisplayContent.this.mTransitionController.isShellTransitionsEnabled()) {
                return;
            }
            DisplayContent.this.continueUpdateOrientationForDiffOrienLaunchingApp();
        }

        @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
        public void onAppTransitionTimeoutLocked() {
            DisplayContent.this.continueUpdateOrientationForDiffOrienLaunchingApp();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class RemoteInsetsControlTarget implements InsetsControlTarget {
        private final IDisplayWindowInsetsController mRemoteInsetsController;
        private final InsetsVisibilities mRequestedVisibilities = new InsetsVisibilities();

        RemoteInsetsControlTarget(IDisplayWindowInsetsController controller) {
            this.mRemoteInsetsController = controller;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void topFocusedWindowChanged(String packageName, InsetsVisibilities requestedVisibilities) {
            try {
                this.mRemoteInsetsController.topFocusedWindowChanged(packageName, requestedVisibilities);
            } catch (RemoteException e) {
                Slog.w("WindowManager", "Failed to deliver package in top focused window change", e);
            }
        }

        void notifyInsetsChanged() {
            try {
                this.mRemoteInsetsController.insetsChanged(DisplayContent.this.getInsetsStateController().getRawInsetsState());
            } catch (RemoteException e) {
                Slog.w("WindowManager", "Failed to deliver inset state change", e);
            }
        }

        @Override // com.android.server.wm.InsetsControlTarget
        public void notifyInsetsControlChanged() {
            InsetsStateController stateController = DisplayContent.this.getInsetsStateController();
            try {
                this.mRemoteInsetsController.insetsControlChanged(stateController.getRawInsetsState(), stateController.getControlsForDispatch(this));
            } catch (RemoteException e) {
                Slog.w("WindowManager", "Failed to deliver inset state change", e);
            }
        }

        @Override // com.android.server.wm.InsetsControlTarget
        public void showInsets(int types, boolean fromIme) {
            try {
                this.mRemoteInsetsController.showInsets(types, fromIme);
            } catch (RemoteException e) {
                Slog.w("WindowManager", "Failed to deliver showInsets", e);
            }
        }

        @Override // com.android.server.wm.InsetsControlTarget
        public void hideInsets(int types, boolean fromIme) {
            try {
                this.mRemoteInsetsController.hideInsets(types, fromIme);
            } catch (RemoteException e) {
                Slog.w("WindowManager", "Failed to deliver showInsets", e);
            }
        }

        @Override // com.android.server.wm.InsetsControlTarget
        public boolean getRequestedVisibility(int type) {
            if (type == 19) {
                return DisplayContent.this.getInsetsStateController().getImeSourceProvider().isImeShowing();
            }
            return this.mRequestedVisibilities.getVisibility(type);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void setRequestedVisibilities(InsetsVisibilities requestedVisibilities) {
            this.mRequestedVisibilities.set(requestedVisibilities);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MagnificationSpec getMagnificationSpec() {
        return this.mMagnificationSpec;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayArea findAreaForWindowType(int windowType, Bundle options, boolean ownerCanManageAppToken, boolean roundedCornerOverlay) {
        if (windowType >= 1 && windowType <= 99) {
            return this.mDisplayAreaPolicy.getTaskDisplayArea(options);
        }
        if (windowType == 2011 || windowType == 2012) {
            return getImeContainer();
        }
        return this.mDisplayAreaPolicy.findAreaForWindowType(windowType, options, ownerCanManageAppToken, roundedCornerOverlay);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayArea findAreaForToken(WindowToken windowToken) {
        return findAreaForWindowType(windowToken.getWindowType(), windowToken.mOptions, windowToken.mOwnerCanManageAppTokens, windowToken.mRoundedCornerOverlay);
    }

    @Override // com.android.server.wm.WindowContainer
    DisplayContent asDisplayContent() {
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSkipAppTransitionAnim(boolean skip) {
        this.mAppTransition.setSkipAppTransitionAnim(skip);
    }

    public TaskDisplayArea getPreferMultiDisplayArea(final ActivityRecord r) {
        if (r == null || r.packageName == null) {
            return null;
        }
        TaskDisplayArea multiDisplay = (TaskDisplayArea) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda49
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DisplayContent.lambda$getPreferMultiDisplayArea$46(ActivityRecord.this, (TaskDisplayArea) obj);
            }
        });
        return multiDisplay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TaskDisplayArea lambda$getPreferMultiDisplayArea$46(ActivityRecord r, TaskDisplayArea taskDisplayArea) {
        ActivityRecord topResumedActivity;
        Task task = taskDisplayArea.getTopRootTask();
        if (task == null || (topResumedActivity = task.getTopResumedActivity()) == null) {
            return null;
        }
        String taskName = topResumedActivity.packageName;
        if (!r.packageName.equals(taskName)) {
            return null;
        }
        return taskDisplayArea;
    }

    public TaskDisplayArea getMultiDisplayArea() {
        TaskDisplayArea multiDisplay = (TaskDisplayArea) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda7
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DisplayContent.lambda$getMultiDisplayArea$47((TaskDisplayArea) obj);
            }
        });
        return multiDisplay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TaskDisplayArea lambda$getMultiDisplayArea$47(TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea.isMultiWindow()) {
            return taskDisplayArea;
        }
        return null;
    }

    public TaskDisplayArea getMultiDisplayArea(int multiWindowMode, final int multiWindowId) {
        TaskDisplayArea multiDisplay = (TaskDisplayArea) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda14
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DisplayContent.lambda$getMultiDisplayArea$48(multiWindowId, (TaskDisplayArea) obj);
            }
        });
        return multiDisplay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TaskDisplayArea lambda$getMultiDisplayArea$48(int multiWindowId, TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea.getMultiWindowingId() == multiWindowId) {
            return taskDisplayArea;
        }
        return null;
    }

    private void setOverlayDialog(boolean overlayDialog) {
        this.mOverlayDialog = overlayDialog;
    }

    public boolean hasOverlayDialog() {
        return this.mOverlayDialog;
    }

    public boolean hasMultiDisplayArea() {
        TaskDisplayArea multiDisplay = (TaskDisplayArea) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda57
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DisplayContent.lambda$hasMultiDisplayArea$49((TaskDisplayArea) obj);
            }
        });
        return multiDisplay != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TaskDisplayArea lambda$hasMultiDisplayArea$49(TaskDisplayArea taskDisplayArea) {
        if ((taskDisplayArea.getMultiWindowingMode() & 2) != 0) {
            return taskDisplayArea;
        }
        return null;
    }

    public TaskDisplayArea getTopMultiDisplayArea() {
        TaskDisplayArea multiDisplay = (TaskDisplayArea) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda2
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DisplayContent.lambda$getTopMultiDisplayArea$50((TaskDisplayArea) obj);
            }
        });
        return multiDisplay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TaskDisplayArea lambda$getTopMultiDisplayArea$50(TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea.isMultiWindow()) {
            return taskDisplayArea;
        }
        return null;
    }

    public TaskDisplayArea getPreferMultiDisplayArea(final int multiWindowId) {
        TaskDisplayArea multiDisplay = (TaskDisplayArea) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda15
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DisplayContent.lambda$getPreferMultiDisplayArea$51(multiWindowId, (TaskDisplayArea) obj);
            }
        });
        return multiDisplay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TaskDisplayArea lambda$getPreferMultiDisplayArea$51(int multiWindowId, TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea.getConfiguration().windowConfiguration.getMultiWindowingId() == multiWindowId) {
            return taskDisplayArea;
        }
        return null;
    }

    public List<String> getTopPackages() {
        if (this.topPackages.size() != 0) {
            this.topPackages.clear();
        }
        forAllTaskDisplayAreas(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda41
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7920lambda$getTopPackages$52$comandroidserverwmDisplayContent((TaskDisplayArea) obj);
            }
        });
        Slog.d("WindowManager", "getTopPackages# topPackages = " + this.topPackages);
        return this.topPackages;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTopPackages$52$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7920lambda$getTopPackages$52$comandroidserverwmDisplayContent(TaskDisplayArea taskDisplayArea) {
        if (taskDisplayArea.getConfiguration().windowConfiguration.isThunderbackWindow()) {
            ActivityRecord r = taskDisplayArea.getFocusedActivity();
            Slog.d("WindowManager", "getTopPackages : r = " + r);
            if (r != null && r.packageName != null) {
                this.topPackages.add(r.packageName);
            }
        }
    }

    public TaskDisplayArea getPreferMultiDisplayArea(final String pkgName) {
        TaskDisplayArea multiDisplay = (TaskDisplayArea) getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda58
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return DisplayContent.lambda$getPreferMultiDisplayArea$53(pkgName, (TaskDisplayArea) obj);
            }
        });
        return multiDisplay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TaskDisplayArea lambda$getPreferMultiDisplayArea$53(String pkgName, TaskDisplayArea taskDisplayArea) {
        ActivityRecord topResumedActivity;
        Task task = taskDisplayArea.getTopRootTask();
        boolean includeInRequestList = false;
        String[] strArr = sRequestPreferDisplayAreaList;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String requestList = strArr[i];
            if (!requestList.equals(pkgName)) {
                i++;
            } else {
                includeInRequestList = true;
                break;
            }
        }
        if (task == null || (topResumedActivity = task.getTopResumedActivity()) == null) {
            return null;
        }
        String taskName = topResumedActivity.packageName;
        if (!pkgName.equals(taskName) || includeInRequestList) {
            return null;
        }
        return taskDisplayArea;
    }

    public TaskDisplayArea getPreferDisplayArea(final String pkgName) {
        if (pkgName == null) {
            return null;
        }
        this.preferDisplay = null;
        forAllActivities(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda16
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7919x6f847e35(pkgName, (ActivityRecord) obj);
            }
        });
        return this.preferDisplay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getPreferDisplayArea$54$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7919x6f847e35(String pkgName, ActivityRecord r) {
        if (pkgName.equals(r.packageName)) {
            Slog.i("WindowManager", "getPreferDisplayArea r: " + r + " isVisible: " + r.isVisible() + " isFocusable: " + r.isFocusable() + " displayarea: " + r.getDisplayArea());
        }
        if (r.isVisible() && r.isFocusable() && r.packageName.equals(pkgName)) {
            this.preferDisplay = r.getDisplayArea();
        }
    }

    public boolean hasOverlayWindow() {
        this.mHasOverlayWindow = false;
        forAllWindows(new Consumer() { // from class: com.android.server.wm.DisplayContent$$ExternalSyntheticLambda45
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DisplayContent.this.m7922lambda$hasOverlayWindow$55$comandroidserverwmDisplayContent((WindowState) obj);
            }
        }, true);
        return this.mHasOverlayWindow;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hasOverlayWindow$55$com-android-server-wm-DisplayContent  reason: not valid java name */
    public /* synthetic */ void m7922lambda$hasOverlayWindow$55$comandroidserverwmDisplayContent(WindowState w) {
        boolean isAreaRate = ITranDisplayContent.Instance().isAreaRate(w, this.mDisplayInfo);
        this.mHasOverlayWindow = isAreaRate;
        if (isAreaRate) {
            Slog.d("WindowManager", "AppOverLayWindow : w = " + w);
        }
    }

    private void registerPointerEventListenerMagellan(Display display) {
        WindowManagerPolicyConstants.PointerEventListener listener = ITranDisplayContent.Instance().getPointerEventListenerMagellan(display);
        if (listener != null) {
            registerPointerEventListener(listener);
        }
    }

    public void setSourceConnectDisplay() {
        this.mSourceConnectDisplay = true;
    }

    public boolean isSourceConnectDisplay() {
        return this.mSourceConnectDisplay;
    }

    private void resetSecureState() {
        this.mHasFindSecureWindow = false;
        this.mSecureWindow = null;
    }

    private void hookSecureWindowVisible() {
        boolean z = this.mLastHasFindSecureWindow;
        boolean z2 = this.mHasFindSecureWindow;
        if (z != z2) {
            if (z2) {
                ITranSourceConnectManager.Instance().hookSecureWindowVisible(getDisplayId(), this.mSecureWindow.mAttrs);
            } else {
                ITranSourceConnectManager.Instance().hookSecureWindowVisible(getDisplayId(), null);
            }
            this.mLastHasFindSecureWindow = this.mHasFindSecureWindow;
        }
    }

    private void markSecureWindow(WindowState w) {
        if (!this.mHasFindSecureWindow && w.isVisible()) {
            if ((w.mAttrs.flags & 8192) != 0 || this.mWmService.mAtmService.getConnectBlackList().contains(w.getWindowTag())) {
                this.mHasFindSecureWindow = true;
                this.mSecureWindow = w;
            }
        }
    }
}
