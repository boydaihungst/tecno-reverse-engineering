package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.LoadedApk;
import android.app.ResourcesManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.InsetsFlags;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.InsetsVisibilities;
import android.view.ViewDebug;
import android.view.WindowInsets;
import android.view.WindowLayout;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.view.accessibility.AccessibilityManager;
import android.window.ClientWindowFrames;
import com.android.internal.policy.ForceShowNavBarSettingsObserver;
import com.android.internal.policy.GestureNavigationSettingsObserver;
import com.android.internal.policy.ScreenDecorationsUtils;
import com.android.internal.policy.SystemBarUtils;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.ScreenshotHelper;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.view.AppearanceRegion;
import com.android.internal.widget.PointerLocationView;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wallpaper.WallpaperManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.DisplayPolicy;
import com.android.server.wm.RefreshRatePolicy;
import com.android.server.wm.SystemGesturesPointerEventListener;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.utils.TranFpUnlockStateController;
import com.transsion.hubcore.server.biometrics.fingerprint.ITranFingerprintService;
import com.transsion.hubcore.server.wm.ITranDisplayPolicy;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import com.transsion.hubcore.server.wm.tranrefreshrate.TranRefreshRatePolicy;
import com.transsion.hubcore.server.wm.tranrefreshrate.TranRefreshRatePolicy120;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class DisplayPolicy {
    static final int ANIMATION_NONE = -1;
    static final int ANIMATION_STYLEABLE = 0;
    private static final int MSG_DISABLE_POINTER_LOCATION = 5;
    private static final int MSG_ENABLE_POINTER_LOCATION = 4;
    private static final int MSG_REMOVE_POINTER_LOCATION = 99;
    private static final int MSG_REQUEST_TRANSIENT_BARS = 2;
    private static final int MSG_REQUEST_TRANSIENT_BARS_ARG_NAVIGATION = 1;
    private static final int MSG_REQUEST_TRANSIENT_BARS_ARG_STATUS = 0;
    private static final int MSG_UPDATE_DREAMING_SLEEP_TOKEN = 1;
    private static final int NAV_BAR_FORCE_TRANSPARENT = 2;
    private static final int NAV_BAR_OPAQUE_WHEN_FREEFORM_OR_DOCKED = 0;
    private static final int NAV_BAR_TRANSLUCENT_WHEN_FREEFORM_OPAQUE_OTHERWISE = 1;
    private static final long PANIC_GESTURE_EXPIRATION = 30000;
    private static final int STATE_HIDE_STATE = 1;
    private static final int STATE_READY_TO_SHOW_STATE = 3;
    private static final int STATE_SHOW_STATE = 2;
    private static final int STATE_WAIT_TO_SHOW_STATE = 4;
    private static final String TAG = "WindowManager";
    private final AccessibilityManager mAccessibilityManager;
    private ActivityTaskManagerInternal mActivityTaskManagerInternal;
    public boolean mAllDrawnCanHideKeyguard;
    private boolean mAllowLockscreenWhenOn;
    private final WindowManagerInternal.AppTransitionListener mAppTransitionListener;
    private volatile boolean mAwake;
    private int mBottomGestureAdditionalInset;
    private final boolean mCarDockEnablesAccelerometer;
    private final Context mContext;
    private Resources mCurrentUserResources;
    private final boolean mDeskDockEnablesAccelerometer;
    private final DisplayContent mDisplayContent;
    private int mDisplayCutoutTouchableRegionSize;
    private int mDreamWindowExistedCount;
    private boolean mDreamingLockscreen;
    private ActivityTaskManagerInternal.SleepTokenAcquirer mDreamingSleepTokenAcquirer;
    private boolean mDreamingSleepTokenNeeded;
    private long mFingerprintStartTime;
    private String mFocusedApp;
    private WindowState mFocusedWindow;
    private final ForceShowNavBarSettingsObserver mForceShowNavBarSettingsObserver;
    private boolean mForceShowNavigationBarEnabled;
    private boolean mForceShowSystemBars;
    private final GestureNavigationSettingsObserver mGestureNavigationSettingsObserver;
    private boolean mHadHideAod;
    private boolean mHadHideKeyguard;
    private final Handler mHandler;
    private volatile boolean mHasNavigationBar;
    private volatile boolean mHasStatusBar;
    private volatile boolean mHdmiPlugged;
    private final ImmersiveModeConfirmation mImmersiveModeConfirmation;
    private boolean mIsFreeformWindowOverlappingWithNavBar;
    private boolean mIsKeyguardReadyToGo;
    private volatile boolean mKeyguardDrawComplete;
    public boolean mKeyguardVisible;
    private int mLastAppearance;
    private int mLastBehavior;
    private volatile int mLastDisableFlags;
    private WindowState mLastFocusedWindow;
    private boolean mLastImmersiveMode;
    private boolean mLastShowingDream;
    private AppearanceRegion[] mLastStatusBarAppearanceRegions;
    private int mLeftGestureInset;
    private final Object mLock;
    private WindowState mNavBarBackgroundWindow;
    private WindowState mNavBarColorWindowCandidate;
    private boolean mNavButtonForcedVisible;
    private volatile boolean mNavigationBarAlwaysShowOnSideGesture;
    private volatile boolean mNavigationBarCanMove;
    private volatile boolean mNavigationBarLetsThroughTaps;
    private long mPendingPanicGestureUptime;
    private volatile boolean mPersistentVrModeEnabled;
    private PointerLocationView mPointerLocationView;
    private boolean mPreWakeupInProgress;
    private long mPreWakeupRelayoutToStatusBarTimeStamp;
    private long mPreWakeupStartTimeStamp;
    private RefreshRatePolicy mRefreshRatePolicy;
    private int mRightGestureInset;
    private volatile boolean mScreenOnEarly;
    private volatile boolean mScreenOnFully;
    private volatile WindowManagerPolicy.ScreenOnListener mScreenOnListener;
    private final ScreenshotHelper mScreenshotHelper;
    private final WindowManagerService mService;
    private boolean mShouldAttachNavBarToAppDuringTransition;
    private boolean mShowingDream;
    private boolean mShowingWindowDream;
    private StatusBarManagerInternal mStatusBarManagerInternal;
    private int mStatusBarRequestedHeightPrev;
    private final SystemGesturesPointerEventListener mSystemGestures;
    private WindowState mSystemUiControllingWindow;
    private WindowState mTopFullscreenOpaqueWindowState;
    private boolean mTopIsFullscreen;
    private final Context mUiContext;
    private volatile boolean mWindowManagerDrawComplete;
    private static final int[] SHOW_TYPES_FOR_SWIPE = {1, 0, 20, 21};
    private static final int[] SHOW_TYPES_FOR_PANIC = {1};
    private static final Rect sTmpRect = new Rect();
    private static final Rect sTmpRect2 = new Rect();
    private static final Rect sTmpLastParentFrame = new Rect();
    private static final Rect sTmpDisplayCutoutSafe = new Rect();
    private static final ClientWindowFrames sTmpClientFrames = new ClientWindowFrames();
    public static boolean TRAN_AIPOWERLAB_SUPPORT = SystemProperties.getBoolean("ro.tran.aipowerlab.support", false);
    public static boolean TRAN_HIGH_REFRESH_NOT_SUPPORT = SystemProperties.getBoolean("ro.tran_high_refresh_not_support", false);
    private final Object mServiceAcquireLock = new Object();
    private volatile int mLidState = -1;
    private volatile int mDockMode = 0;
    private WindowState mStatusBar = null;
    private WindowState mNotificationShade = null;
    private final int[] mStatusBarHeightForRotation = new int[4];
    private WindowState mNavigationBar = null;
    private final int[] mNavBarHeightForRotation = new int[4];
    private final int[] mNavBarWidthForRotation = new int[4];
    private int mNavigationBarPosition = 4;
    private WindowState mStatusBarAlt = null;
    private int mStatusBarAltPosition = -1;
    private WindowState mNavigationBarAlt = null;
    private int mNavigationBarAltPosition = -1;
    private WindowState mClimateBarAlt = null;
    private int mClimateBarAltPosition = -1;
    private WindowState mExtraNavBarAlt = null;
    private int mExtraNavBarAltPosition = -1;
    private final ArraySet<WindowState> mInsetsSourceWindowsExceptIme = new ArraySet<>();
    private final ArrayList<AppearanceRegion> mStatusBarAppearanceRegionList = new ArrayList<>();
    private final ArrayList<WindowState> mStatusBarBackgroundWindows = new ArrayList<>();
    private InsetsVisibilities mRequestedVisibilities = new InsetsVisibilities();
    private final Rect mStatusBarColorCheckedBounds = new Rect();
    private final Rect mStatusBarBackgroundCheckedBounds = new Rect();
    private boolean mLastFocusIsFullscreen = false;
    private final WindowLayout mWindowLayout = new WindowLayout();
    private int mNavBarOpacityMode = 0;
    private WindowState mAodWindow = null;
    private WindowState mWindowForDreamAnimation = null;
    private WindowState mGLWallpaperWindow = null;
    private boolean mNeedHideWallpaperWin = false;
    private boolean mNeedDelayShowWallpaper = true;
    private final Runnable mResetWallpaperVisible = new Runnable() { // from class: com.android.server.wm.DisplayPolicy.3
        @Override // java.lang.Runnable
        public void run() {
            DisplayPolicy.this.mNeedHideWallpaperWin = false;
            DisplayPolicy.this.mNeedDelayShowWallpaper = true;
            DisplayPolicy.this.mService.mWindowPlacerLocked.requestTraversal();
        }
    };
    private final Runnable mHiddenNavPanic = new Runnable() { // from class: com.android.server.wm.DisplayPolicy.4
        @Override // java.lang.Runnable
        public void run() {
            synchronized (DisplayPolicy.this.mLock) {
                if (DisplayPolicy.this.mService.mPolicy.isUserSetupComplete()) {
                    DisplayPolicy.this.mPendingPanicGestureUptime = SystemClock.uptimeMillis();
                    DisplayPolicy.this.updateSystemBarAttributes();
                }
            }
        }
    };
    private int mStatusBarLayerState = 2;
    private int mNotificationLayerState = 2;
    public boolean mAodHideByFingerprint = false;
    private final Runnable mResetNotifactionShadeVisible = new Runnable() { // from class: com.android.server.wm.DisplayPolicy.5
        @Override // java.lang.Runnable
        public void run() {
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.i("WindowManager", " delay to reset keyguard layer.");
            }
            TranFpUnlockStateController.getInstance().setFingerprintHideState(false);
            DisplayPolicy.this.mService.mWindowPlacerLocked.requestTraversal();
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public StatusBarManagerInternal getStatusBarManagerInternal() {
        StatusBarManagerInternal statusBarManagerInternal;
        synchronized (this.mServiceAcquireLock) {
            if (this.mStatusBarManagerInternal == null) {
                this.mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            }
            statusBarManagerInternal = this.mStatusBarManagerInternal;
        }
        return statusBarManagerInternal;
    }

    /* loaded from: classes2.dex */
    private class PolicyHandler extends Handler {
        PolicyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    DisplayPolicy.this.updateDreamingSleepToken(msg.arg1 != 0);
                    return;
                case 2:
                    synchronized (DisplayPolicy.this.mLock) {
                        WindowState targetBar = msg.arg1 == 0 ? DisplayPolicy.this.getStatusBar() : DisplayPolicy.this.getNavigationBar();
                        if (targetBar != null) {
                            DisplayPolicy.this.requestTransientBars(targetBar, true);
                        }
                    }
                    return;
                case 4:
                    DisplayPolicy.this.enablePointerLocation();
                    return;
                case 5:
                    DisplayPolicy.this.disablePointerLocation();
                    return;
                case 99:
                    DisplayPolicy.this.removePointerLocation();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayPolicy(WindowManagerService service, DisplayContent displayContent) {
        this.mService = service;
        Context createDisplayContext = displayContent.isDefaultDisplay ? service.mContext : service.mContext.createDisplayContext(displayContent.getDisplay());
        this.mContext = createDisplayContext;
        Context systemUiContext = displayContent.isDefaultDisplay ? service.mAtmService.mUiContext : service.mAtmService.mSystemThread.getSystemUiContext(displayContent.getDisplayId());
        this.mUiContext = systemUiContext;
        this.mDisplayContent = displayContent;
        this.mLock = service.getWindowManagerLock();
        int displayId = displayContent.getDisplayId();
        Resources r = createDisplayContext.getResources();
        this.mCarDockEnablesAccelerometer = r.getBoolean(17891401);
        this.mDeskDockEnablesAccelerometer = r.getBoolean(17891590);
        this.mAccessibilityManager = (AccessibilityManager) createDisplayContext.getSystemService("accessibility");
        ITranDisplayPolicy.Instance().initSupportMaxRefreshRate(createDisplayContext);
        ActivityTaskManagerInternal activityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mActivityTaskManagerInternal = activityTaskManagerInternal;
        this.mDreamingSleepTokenAcquirer = activityTaskManagerInternal.createSleepTokenAcquirer("Dream");
        if (!displayContent.isDefaultDisplay) {
            this.mAwake = true;
            this.mScreenOnEarly = true;
            this.mScreenOnFully = true;
        }
        Looper looper = UiThread.getHandler().getLooper();
        PolicyHandler policyHandler = new PolicyHandler(looper);
        this.mHandler = policyHandler;
        SystemGesturesPointerEventListener systemGesturesPointerEventListener = new SystemGesturesPointerEventListener(systemUiContext, policyHandler, new SystemGesturesPointerEventListener.Callbacks() { // from class: com.android.server.wm.DisplayPolicy.1
            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onSwipeFromTop() {
                WindowState bar;
                synchronized (DisplayPolicy.this.mLock) {
                    if (isGestureIsolated()) {
                        return;
                    }
                    if (DisplayPolicy.this.mStatusBar != null) {
                        bar = DisplayPolicy.this.mStatusBar;
                    } else {
                        bar = DisplayPolicy.this.findAltBarMatchingPosition(8);
                    }
                    DisplayPolicy.this.requestTransientBars(bar, true);
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onSwipeFromBottom() {
                WindowState bar;
                synchronized (DisplayPolicy.this.mLock) {
                    if (isGestureIsolated()) {
                        return;
                    }
                    if (DisplayPolicy.this.mNavigationBar != null && DisplayPolicy.this.mNavigationBarPosition == 4) {
                        bar = DisplayPolicy.this.mNavigationBar;
                    } else {
                        bar = DisplayPolicy.this.findAltBarMatchingPosition(4);
                    }
                    DisplayPolicy.this.requestTransientBars(bar, true);
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onSwipeFromRight() {
                if (isGestureIsolated()) {
                    return;
                }
                Region excludedRegion = Region.obtain();
                synchronized (DisplayPolicy.this.mLock) {
                    DisplayPolicy.this.mDisplayContent.calculateSystemGestureExclusion(excludedRegion, null);
                    requestTransientBarsForSideSwipe(excludedRegion, 2, 2);
                }
                excludedRegion.recycle();
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onSwipeFromLeft() {
                if (isGestureIsolated()) {
                    return;
                }
                Region excludedRegion = Region.obtain();
                synchronized (DisplayPolicy.this.mLock) {
                    DisplayPolicy.this.mDisplayContent.calculateSystemGestureExclusion(excludedRegion, null);
                    requestTransientBarsForSideSwipe(excludedRegion, 1, 1);
                }
                excludedRegion.recycle();
            }

            private void requestTransientBarsForSideSwipe(Region excludedRegion, int navBarSide, int altBarSide) {
                WindowState barMatchingSide;
                WindowState bar;
                if (DisplayPolicy.this.mNavigationBar != null && DisplayPolicy.this.mNavigationBarPosition == navBarSide) {
                    barMatchingSide = DisplayPolicy.this.mNavigationBar;
                } else {
                    barMatchingSide = DisplayPolicy.this.findAltBarMatchingPosition(altBarSide);
                }
                boolean allowSideSwipe = DisplayPolicy.this.mNavigationBarAlwaysShowOnSideGesture && !DisplayPolicy.this.mSystemGestures.currentGestureStartedInRegion(excludedRegion);
                if (barMatchingSide == null && !allowSideSwipe) {
                    return;
                }
                boolean isGestureOnSystemBar = barMatchingSide != null;
                if (barMatchingSide != null) {
                    bar = barMatchingSide;
                } else {
                    bar = DisplayPolicy.this.findTransientNavOrAltBar();
                }
                DisplayPolicy.this.requestTransientBars(bar, isGestureOnSystemBar);
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onFling(int duration) {
                if (DisplayPolicy.this.mService.mPowerManagerInternal != null) {
                    DisplayPolicy.this.mService.mPowerManagerInternal.setPowerBoost(0, duration);
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onDebug() {
            }

            private WindowOrientationListener getOrientationListener() {
                DisplayRotation rotation = DisplayPolicy.this.mDisplayContent.getDisplayRotation();
                if (rotation != null) {
                    return rotation.getOrientationListener();
                }
                return null;
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onDown() {
                WindowOrientationListener listener = getOrientationListener();
                if (listener != null) {
                    listener.onTouchStart();
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onUpOrCancel() {
                WindowOrientationListener listener = getOrientationListener();
                if (listener != null) {
                    listener.onTouchEnd();
                }
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onMouseHoverAtTop() {
                DisplayPolicy.this.mHandler.removeMessages(2);
                Message msg = DisplayPolicy.this.mHandler.obtainMessage(2);
                msg.arg1 = 0;
                DisplayPolicy.this.mHandler.sendMessageDelayed(msg, 500L);
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onMouseHoverAtBottom() {
                DisplayPolicy.this.mHandler.removeMessages(2);
                Message msg = DisplayPolicy.this.mHandler.obtainMessage(2);
                msg.arg1 = 1;
                DisplayPolicy.this.mHandler.sendMessageDelayed(msg, 500L);
            }

            @Override // com.android.server.wm.SystemGesturesPointerEventListener.Callbacks
            public void onMouseLeaveFromEdge() {
                DisplayPolicy.this.mHandler.removeMessages(2);
            }

            private boolean isGestureIsolated() {
                WindowState win = DisplayPolicy.this.mFocusedWindow != null ? DisplayPolicy.this.mFocusedWindow : DisplayPolicy.this.mTopFullscreenOpaqueWindowState;
                if (win != null && (win.getDisableFlags() & 16777216) != 0) {
                    return true;
                }
                return false;
            }
        });
        this.mSystemGestures = systemGesturesPointerEventListener;
        displayContent.registerPointerEventListener(systemGesturesPointerEventListener);
        AnonymousClass2 anonymousClass2 = new AnonymousClass2(displayId);
        this.mAppTransitionListener = anonymousClass2;
        displayContent.mAppTransition.registerListenerLocked(anonymousClass2);
        displayContent.mTransitionController.registerLegacyListener(anonymousClass2);
        this.mImmersiveModeConfirmation = new ImmersiveModeConfirmation(createDisplayContext, looper, service.mVrModeEnabled);
        this.mScreenshotHelper = displayContent.isDefaultDisplay ? new ScreenshotHelper(createDisplayContext) : null;
        if (displayContent.isDefaultDisplay) {
            this.mHasStatusBar = true;
            this.mHasNavigationBar = createDisplayContext.getResources().getBoolean(17891750);
            String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                this.mHasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                this.mHasNavigationBar = true;
            }
        } else {
            this.mHasStatusBar = false;
            this.mHasNavigationBar = displayContent.supportsSystemDecorations();
        }
        hookInitRefreshRatePolicy();
        final GestureNavigationSettingsObserver gestureNavigationSettingsObserver = new GestureNavigationSettingsObserver(policyHandler, createDisplayContext, new Runnable() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPolicy.this.m7984lambda$new$0$comandroidserverwmDisplayPolicy();
            }
        });
        this.mGestureNavigationSettingsObserver = gestureNavigationSettingsObserver;
        Objects.requireNonNull(gestureNavigationSettingsObserver);
        policyHandler.post(new Runnable() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                gestureNavigationSettingsObserver.register();
            }
        });
        final ForceShowNavBarSettingsObserver forceShowNavBarSettingsObserver = new ForceShowNavBarSettingsObserver(policyHandler, createDisplayContext);
        this.mForceShowNavBarSettingsObserver = forceShowNavBarSettingsObserver;
        forceShowNavBarSettingsObserver.setOnChangeRunnable(new Runnable() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPolicy.this.updateForceShowNavBarSettings();
            }
        });
        this.mForceShowNavigationBarEnabled = forceShowNavBarSettingsObserver.isEnabled();
        Objects.requireNonNull(forceShowNavBarSettingsObserver);
        policyHandler.post(new Runnable() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                forceShowNavBarSettingsObserver.register();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.wm.DisplayPolicy$2  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass2 extends WindowManagerInternal.AppTransitionListener {
        private Runnable mAppTransitionCancelled;
        private Runnable mAppTransitionFinished;
        private Runnable mAppTransitionPending;
        final /* synthetic */ int val$displayId;

        AnonymousClass2(final int i) {
            this.val$displayId = i;
            this.mAppTransitionPending = new Runnable() { // from class: com.android.server.wm.DisplayPolicy$2$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DisplayPolicy.AnonymousClass2.this.m7986lambda$$0$comandroidserverwmDisplayPolicy$2(i);
                }
            };
            this.mAppTransitionCancelled = new Runnable() { // from class: com.android.server.wm.DisplayPolicy$2$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    DisplayPolicy.AnonymousClass2.this.m7987lambda$$1$comandroidserverwmDisplayPolicy$2(i);
                }
            };
            this.mAppTransitionFinished = new Runnable() { // from class: com.android.server.wm.DisplayPolicy$2$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    DisplayPolicy.AnonymousClass2.this.m7988lambda$$2$comandroidserverwmDisplayPolicy$2(i);
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$$0$com-android-server-wm-DisplayPolicy$2  reason: not valid java name */
        public /* synthetic */ void m7986lambda$$0$comandroidserverwmDisplayPolicy$2(int displayId) {
            StatusBarManagerInternal statusBar = DisplayPolicy.this.getStatusBarManagerInternal();
            if (statusBar != null) {
                statusBar.appTransitionPending(displayId);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$$1$com-android-server-wm-DisplayPolicy$2  reason: not valid java name */
        public /* synthetic */ void m7987lambda$$1$comandroidserverwmDisplayPolicy$2(int displayId) {
            StatusBarManagerInternal statusBar = DisplayPolicy.this.getStatusBarManagerInternal();
            if (statusBar != null) {
                statusBar.appTransitionCancelled(displayId);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$$2$com-android-server-wm-DisplayPolicy$2  reason: not valid java name */
        public /* synthetic */ void m7988lambda$$2$comandroidserverwmDisplayPolicy$2(int displayId) {
            StatusBarManagerInternal statusBar = DisplayPolicy.this.getStatusBarManagerInternal();
            if (statusBar != null) {
                statusBar.appTransitionFinished(displayId);
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
        public void onAppTransitionPendingLocked() {
            DisplayPolicy.this.mHandler.post(this.mAppTransitionPending);
        }

        @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
        public int onAppTransitionStartingLocked(boolean keyguardGoingAway, boolean keyguardOccluding, long duration, final long statusBarAnimationStartTime, final long statusBarAnimationDuration) {
            DisplayPolicy.this.mHandler.post(new Runnable() { // from class: com.android.server.wm.DisplayPolicy$2$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DisplayPolicy.AnonymousClass2.this.m7989x83945c3a(statusBarAnimationStartTime, statusBarAnimationDuration);
                }
            });
            return 0;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onAppTransitionStartingLocked$3$com-android-server-wm-DisplayPolicy$2  reason: not valid java name */
        public /* synthetic */ void m7989x83945c3a(long statusBarAnimationStartTime, long statusBarAnimationDuration) {
            StatusBarManagerInternal statusBar = DisplayPolicy.this.getStatusBarManagerInternal();
            if (statusBar != null) {
                statusBar.appTransitionStarting(DisplayPolicy.this.mContext.getDisplayId(), statusBarAnimationStartTime, statusBarAnimationDuration);
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
        public void onAppTransitionCancelledLocked(boolean keyguardGoingAway) {
            DisplayPolicy.this.mHandler.post(this.mAppTransitionCancelled);
        }

        @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
        public void onAppTransitionFinishedLocked(IBinder token) {
            DisplayPolicy.this.mHandler.post(this.mAppTransitionFinished);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7984lambda$new$0$comandroidserverwmDisplayPolicy() {
        synchronized (this.mLock) {
            onConfigurationChanged();
            this.mSystemGestures.onConfigurationChanged();
            this.mDisplayContent.updateSystemGestureExclusion();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForceShowNavBarSettings() {
        synchronized (this.mLock) {
            this.mForceShowNavigationBarEnabled = this.mForceShowNavBarSettingsObserver.isEnabled();
            updateSystemBarAttributes();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public WindowState findAltBarMatchingPosition(int pos) {
        WindowState windowState = this.mStatusBarAlt;
        if (windowState != null && this.mStatusBarAltPosition == pos) {
            return windowState;
        }
        WindowState windowState2 = this.mNavigationBarAlt;
        if (windowState2 != null && this.mNavigationBarAltPosition == pos) {
            return windowState2;
        }
        WindowState windowState3 = this.mClimateBarAlt;
        if (windowState3 != null && this.mClimateBarAltPosition == pos) {
            return windowState3;
        }
        WindowState windowState4 = this.mExtraNavBarAlt;
        if (windowState4 != null && this.mExtraNavBarAltPosition == pos) {
            return windowState4;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public WindowState findTransientNavOrAltBar() {
        WindowState windowState = this.mNavigationBar;
        if (windowState != null) {
            return windowState;
        }
        WindowState windowState2 = this.mNavigationBarAlt;
        if (windowState2 != null) {
            return windowState2;
        }
        WindowState windowState3 = this.mExtraNavBarAlt;
        if (windowState3 != null) {
            return windowState3;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemReady() {
        this.mSystemGestures.systemReady();
        if (this.mService.mPointerLocationEnabled) {
            setPointerLocationEnabled(true);
        }
    }

    private int getDisplayId() {
        return this.mDisplayContent.getDisplayId();
    }

    public void setHdmiPlugged(boolean plugged) {
        setHdmiPlugged(plugged, false);
    }

    public void setHdmiPlugged(boolean plugged, boolean force) {
        if (force || this.mHdmiPlugged != plugged) {
            this.mHdmiPlugged = plugged;
            this.mService.updateRotation(true, true);
            Intent intent = new Intent("android.intent.action.HDMI_PLUGGED");
            intent.addFlags(67108864);
            intent.putExtra("state", plugged);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isHdmiPlugged() {
        return this.mHdmiPlugged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCarDockEnablesAccelerometer() {
        return this.mCarDockEnablesAccelerometer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDeskDockEnablesAccelerometer() {
        return this.mDeskDockEnablesAccelerometer;
    }

    public void setPersistentVrModeEnabled(boolean persistentVrModeEnabled) {
        this.mPersistentVrModeEnabled = persistentVrModeEnabled;
    }

    public boolean isPersistentVrModeEnabled() {
        return this.mPersistentVrModeEnabled;
    }

    public void setDockMode(int dockMode) {
        this.mDockMode = dockMode;
    }

    public int getDockMode() {
        return this.mDockMode;
    }

    public boolean hasNavigationBar() {
        return this.mHasNavigationBar;
    }

    public boolean hasStatusBar() {
        return this.mHasStatusBar;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSideGestures() {
        return this.mHasNavigationBar && (this.mLeftGestureInset > 0 || this.mRightGestureInset > 0);
    }

    public boolean navigationBarCanMove() {
        return this.mNavigationBarCanMove;
    }

    public void setLidState(int lidState) {
        this.mLidState = lidState;
    }

    public int getLidState() {
        return this.mLidState;
    }

    public void setAwake(boolean awake) {
        this.mAwake = awake;
    }

    public boolean isAwake() {
        return this.mAwake;
    }

    public boolean isScreenOnEarly() {
        return this.mScreenOnEarly;
    }

    public boolean isScreenOnFully() {
        return this.mScreenOnFully;
    }

    public boolean isKeyguardDrawComplete() {
        return this.mKeyguardDrawComplete;
    }

    public boolean isWindowManagerDrawComplete() {
        return this.mWindowManagerDrawComplete;
    }

    public boolean isForceShowNavigationBarEnabled() {
        return this.mForceShowNavigationBarEnabled;
    }

    public WindowManagerPolicy.ScreenOnListener getScreenOnListener() {
        return this.mScreenOnListener;
    }

    public void screenTurnedOn(WindowManagerPolicy.ScreenOnListener screenOnListener) {
        synchronized (this.mLock) {
            this.mScreenOnEarly = true;
            this.mScreenOnFully = false;
            this.mKeyguardDrawComplete = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = screenOnListener;
        }
    }

    public void screenTurnedOff() {
        synchronized (this.mLock) {
            this.mScreenOnEarly = false;
            this.mScreenOnFully = false;
            this.mKeyguardDrawComplete = false;
            this.mWindowManagerDrawComplete = false;
            this.mScreenOnListener = null;
        }
        if (this.mGLWallpaperWindow != null) {
            this.mNeedHideWallpaperWin = true;
            this.mService.mWindowPlacerLocked.requestTraversal();
        }
    }

    public boolean finishKeyguardDrawn() {
        synchronized (this.mLock) {
            if (this.mScreenOnEarly && !this.mKeyguardDrawComplete) {
                this.mKeyguardDrawComplete = true;
                this.mWindowManagerDrawComplete = false;
                return true;
            }
            return false;
        }
    }

    public boolean finishWindowsDrawn() {
        synchronized (this.mLock) {
            if (this.mScreenOnEarly && !this.mWindowManagerDrawComplete) {
                this.mWindowManagerDrawComplete = true;
                return true;
            }
            return false;
        }
    }

    public boolean finishScreenTurningOn() {
        synchronized (this.mLock) {
            if (ProtoLogCache.WM_DEBUG_SCREEN_ON_enabled) {
                boolean protoLogParam0 = this.mAwake;
                boolean protoLogParam1 = this.mScreenOnEarly;
                boolean protoLogParam2 = this.mScreenOnFully;
                boolean protoLogParam3 = this.mKeyguardDrawComplete;
                boolean protoLogParam4 = this.mWindowManagerDrawComplete;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_SCREEN_ON, 1865125884, 1023, (String) null, new Object[]{Boolean.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), Boolean.valueOf(protoLogParam4)});
            }
            boolean protoLogParam02 = this.mScreenOnFully;
            if (!protoLogParam02 && this.mScreenOnEarly && this.mWindowManagerDrawComplete && (!this.mAwake || this.mKeyguardDrawComplete)) {
                if (ProtoLogCache.WM_DEBUG_SCREEN_ON_enabled) {
                    Object[] objArr = null;
                    ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_SCREEN_ON, 1140424002, 0, (String) null, (Object[]) null);
                }
                this.mScreenOnListener = null;
                this.mScreenOnFully = true;
                return true;
            }
            return false;
        }
    }

    public void adjustWindowParamsLw(WindowState win, WindowManager.LayoutParams attrs) {
        switch (attrs.type) {
            case 1:
                if (attrs.isFullscreen() && win.mActivityRecord != null && win.mActivityRecord.fillsParent() && (win.mAttrs.privateFlags & 131072) != 0 && attrs.getFitInsetsTypes() != 0) {
                    throw new IllegalArgumentException("Illegal attributes: Main activity window that isn't translucent trying to fit insets: " + attrs.getFitInsetsTypes() + " attrs=" + attrs);
                }
                break;
            case 2005:
                if (attrs.hideTimeoutMilliseconds < 0 || attrs.hideTimeoutMilliseconds > 4100) {
                    attrs.hideTimeoutMilliseconds = 4100L;
                }
                attrs.hideTimeoutMilliseconds = this.mAccessibilityManager.getRecommendedTimeoutMillis((int) attrs.hideTimeoutMilliseconds, 2);
                attrs.flags |= 16;
                break;
            case 2006:
            case 2015:
                attrs.flags |= 24;
                attrs.flags &= -262145;
                break;
            case 2013:
                attrs.layoutInDisplayCutoutMode = 3;
                break;
        }
        if (WindowManager.LayoutParams.isSystemAlertWindowType(attrs.type)) {
            float maxOpacity = this.mService.mMaximumObscuringOpacityForTouch;
            if (attrs.alpha > maxOpacity && (attrs.flags & 16) != 0 && (attrs.privateFlags & 536870912) == 0) {
                Slog.w("WindowManager", String.format("App %s has a system alert window (type = %d) with FLAG_NOT_TOUCHABLE and LayoutParams.alpha = %.2f > %.2f, setting alpha to %.2f to let touches pass through (if this is isn't desirable, remove flag FLAG_NOT_TOUCHABLE).", attrs.packageName, Integer.valueOf(attrs.type), Float.valueOf(attrs.alpha), Float.valueOf(maxOpacity), Float.valueOf(maxOpacity)));
                attrs.alpha = maxOpacity;
                win.mWinAnimator.mAlpha = maxOpacity;
            }
        }
        if (this.mStatusBarAlt == win) {
            this.mStatusBarAltPosition = getAltBarPosition(attrs);
        }
        if (this.mNavigationBarAlt == win) {
            this.mNavigationBarAltPosition = getAltBarPosition(attrs);
        }
        if (this.mClimateBarAlt == win) {
            this.mClimateBarAltPosition = getAltBarPosition(attrs);
        }
        if (this.mExtraNavBarAlt == win) {
            this.mExtraNavBarAltPosition = getAltBarPosition(attrs);
        }
        InsetsSourceProvider provider = win.getControllableInsetProvider();
        if (provider != null && provider.getSource().getInsetsRoundedCornerFrame() != attrs.insetsRoundedCornerFrame) {
            provider.getSource().setInsetsRoundedCornerFrame(attrs.insetsRoundedCornerFrame);
        }
        ITranWindowManagerService.Instance().onAdjustWindowParamsLw(this, win, attrs);
    }

    public void setDropInputModePolicy(WindowState win, WindowManager.LayoutParams attrs) {
        if (attrs.type == 2005 && (attrs.privateFlags & 536870912) == 0) {
            this.mService.mTransactionFactory.get().setDropInputMode(win.getSurfaceControl(), 1).apply();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int validateAddingWindowLw(WindowManager.LayoutParams attrs, int callingPid, int callingUid) {
        int[] iArr;
        WindowState windowState;
        WindowState windowState2;
        WindowState windowState3;
        WindowState windowState4;
        if ((attrs.privateFlags & 536870912) != 0) {
            this.mContext.enforcePermission("android.permission.INTERNAL_SYSTEM_WINDOW", callingPid, callingUid, "DisplayPolicy");
        }
        if ((attrs.privateFlags & Integer.MIN_VALUE) != 0) {
            ActivityTaskManagerService.enforceTaskPermission("DisplayPolicy");
        }
        switch (attrs.type) {
            case 2000:
                this.mContext.enforcePermission("android.permission.STATUS_BAR_SERVICE", callingPid, callingUid, "DisplayPolicy");
                WindowState windowState5 = this.mStatusBar;
                if ((windowState5 != null && windowState5.isAlive()) || ((windowState3 = this.mStatusBarAlt) != null && windowState3.isAlive())) {
                    return -7;
                }
                break;
            case 2014:
                return -10;
            case 2017:
            case 2033:
            case 2041:
                this.mContext.enforcePermission("android.permission.STATUS_BAR_SERVICE", callingPid, callingUid, "DisplayPolicy");
                break;
            case 2019:
                this.mContext.enforcePermission("android.permission.STATUS_BAR_SERVICE", callingPid, callingUid, "DisplayPolicy");
                WindowState windowState6 = this.mNavigationBar;
                if ((windowState6 != null && windowState6.isAlive()) || ((windowState4 = this.mNavigationBarAlt) != null && windowState4.isAlive())) {
                    return -7;
                }
                break;
            case 2024:
                if (!this.mService.mAtmService.isCallerRecents(callingUid)) {
                    this.mContext.enforcePermission("android.permission.STATUS_BAR_SERVICE", callingPid, callingUid, "DisplayPolicy");
                    break;
                }
                break;
            case 2040:
                this.mContext.enforcePermission("android.permission.STATUS_BAR_SERVICE", callingPid, callingUid, "DisplayPolicy");
                WindowState windowState7 = this.mNotificationShade;
                if (windowState7 != null && windowState7.isAlive()) {
                    return -7;
                }
                break;
        }
        if (attrs.providesInsetsTypes != null) {
            if (!this.mService.mAtmService.isCallerRecents(callingUid)) {
                this.mContext.enforcePermission("android.permission.STATUS_BAR_SERVICE", callingPid, callingUid, "DisplayPolicy");
            }
            enforceSingleInsetsTypeCorrespondingToWindowType(attrs.providesInsetsTypes);
            for (int insetType : attrs.providesInsetsTypes) {
                switch (insetType) {
                    case 0:
                        WindowState windowState8 = this.mStatusBar;
                        if ((windowState8 != null && windowState8.isAlive()) || ((windowState = this.mStatusBarAlt) != null && windowState.isAlive())) {
                            return -7;
                        }
                        break;
                    case 1:
                        WindowState windowState9 = this.mNavigationBar;
                        if ((windowState9 != null && windowState9.isAlive()) || ((windowState2 = this.mNavigationBarAlt) != null && windowState2.isAlive())) {
                            return -7;
                        }
                        break;
                    case 20:
                        WindowState windowState10 = this.mClimateBarAlt;
                        if (windowState10 != null && windowState10.isAlive()) {
                            return -7;
                        }
                        break;
                    case 21:
                        WindowState windowState11 = this.mExtraNavBarAlt;
                        if (windowState11 != null && windowState11.isAlive()) {
                            return -7;
                        }
                        break;
                }
            }
        }
        return 0;
    }

    public boolean isDreamWindowExisted() {
        return this.mDreamWindowExistedCount > 0;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r9v0, resolved type: com.android.server.wm.DisplayContent */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Multi-variable type inference failed */
    public void addWindowLw(final WindowState win, WindowManager.LayoutParams attrs) {
        int[] iArr;
        TriConsumer<DisplayFrames, WindowContainer, Rect> imeFrameProvider;
        switch (attrs.type) {
            case 2000:
                this.mStatusBar = win;
                TriConsumer<DisplayFrames, WindowContainer, Rect> gestureFrameProvider = new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda3
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        DisplayPolicy.this.m7973lambda$addWindowLw$1$comandroidserverwmDisplayPolicy((DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                    }
                };
                this.mDisplayContent.setInsetProvider(0, win, null);
                this.mDisplayContent.setInsetProvider(7, win, gestureFrameProvider);
                this.mDisplayContent.setInsetProvider(16, win, null);
                this.mInsetsSourceWindowsExceptIme.add(win);
                return;
            case 2019:
                this.mNavigationBar = win;
                this.mDisplayContent.setInsetProvider(1, win, new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda5
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        DisplayPolicy.this.m7976lambda$addWindowLw$2$comandroidserverwmDisplayPolicy(win, (DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                    }
                }, new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda6
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        DisplayPolicy.lambda$addWindowLw$3((DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                    }
                });
                this.mDisplayContent.setInsetProvider(8, win, new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda7
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        DisplayPolicy.this.m7977lambda$addWindowLw$4$comandroidserverwmDisplayPolicy((DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                    }
                });
                this.mDisplayContent.setInsetProvider(5, win, new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda8
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        DisplayPolicy.this.m7978lambda$addWindowLw$5$comandroidserverwmDisplayPolicy((DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                    }
                });
                this.mDisplayContent.setInsetProvider(6, win, new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda9
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        DisplayPolicy.this.m7979lambda$addWindowLw$6$comandroidserverwmDisplayPolicy((DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                    }
                });
                this.mDisplayContent.setInsetProvider(18, win, new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda10
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        DisplayPolicy.this.m7980lambda$addWindowLw$7$comandroidserverwmDisplayPolicy(win, (DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                    }
                });
                this.mInsetsSourceWindowsExceptIme.add(win);
                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                    Slog.i("WindowManager", "NAVIGATION BAR: " + this.mNavigationBar);
                    return;
                }
                return;
            case 2040:
                this.mNotificationShade = win;
                return;
            case 2044:
                this.mDreamWindowExistedCount++;
                break;
        }
        if (attrs.type == 2044) {
            Slog.i("WindowManager", "set mAodWindow=" + win);
            this.mAodWindow = win;
        } else if (ITranDisplayPolicy.Instance().currentWindowIsShowWallpaperWindow(win)) {
            Slog.i("WindowManager", "set dreamAnimationWindow=" + win);
            this.mWindowForDreamAnimation = win;
        } else if (attrs.type == 2013 && win.toString().contains("com.transsion.livewallpaper.magictouch.GLWallpaperService")) {
            Slog.i("WindowManager", "addWindow mGLWallpaperWindow=" + win);
            this.mGLWallpaperWindow = win;
        }
        if (attrs.providesInsetsTypes != null) {
            for (final int insetsType : attrs.providesInsetsTypes) {
                if (win.getAttrs().providedInternalImeInsets != null) {
                    imeFrameProvider = new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda11
                        public final void accept(Object obj, Object obj2, Object obj3) {
                            DisplayPolicy.lambda$addWindowLw$8(WindowState.this, insetsType, (DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                        }
                    };
                } else {
                    imeFrameProvider = null;
                }
                switch (insetsType) {
                    case 0:
                        this.mStatusBarAlt = win;
                        this.mStatusBarAltPosition = getAltBarPosition(attrs);
                        break;
                    case 1:
                        this.mNavigationBarAlt = win;
                        this.mNavigationBarAltPosition = getAltBarPosition(attrs);
                        break;
                    case 20:
                        this.mClimateBarAlt = win;
                        this.mClimateBarAltPosition = getAltBarPosition(attrs);
                        break;
                    case 21:
                        this.mExtraNavBarAlt = win;
                        this.mExtraNavBarAltPosition = getAltBarPosition(attrs);
                        break;
                }
                this.mDisplayContent.setInsetProvider(insetsType, win, win.getAttrs().providedInternalInsets != null ? new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda12
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        DisplayPolicy.lambda$addWindowLw$9(WindowState.this, insetsType, (DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                    }
                } : null, imeFrameProvider);
                if (this.mNavigationBar == null && (insetsType == 1 || insetsType == 21)) {
                    this.mDisplayContent.setInsetProvider(5, win, new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda13
                        public final void accept(Object obj, Object obj2, Object obj3) {
                            DisplayPolicy.this.m7974lambda$addWindowLw$10$comandroidserverwmDisplayPolicy((DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                        }
                    });
                    this.mDisplayContent.setInsetProvider(6, win, new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda4
                        public final void accept(Object obj, Object obj2, Object obj3) {
                            DisplayPolicy.this.m7975lambda$addWindowLw$11$comandroidserverwmDisplayPolicy((DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
                        }
                    });
                }
                this.mInsetsSourceWindowsExceptIme.add(win);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addWindowLw$1$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7973lambda$addWindowLw$1$comandroidserverwmDisplayPolicy(DisplayFrames displayFrames, WindowContainer windowContainer, Rect rect) {
        rect.bottom = rect.top + getStatusBarHeight(displayFrames);
        DisplayCutout cutout = displayFrames.mInsetsState.getDisplayCutout();
        if (cutout != null) {
            Rect top = cutout.getBoundingRectTop();
            if (!top.isEmpty()) {
                rect.bottom = Math.max(rect.bottom, top.bottom + this.mDisplayCutoutTouchableRegionSize);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addWindowLw$2$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7976lambda$addWindowLw$2$comandroidserverwmDisplayPolicy(WindowState win, DisplayFrames displayFrames, WindowContainer windowContainer, Rect inOutFrame) {
        if (!this.mNavButtonForcedVisible) {
            Insets[] providedInternalInsets = win.getLayoutingAttrs(displayFrames.mRotation).providedInternalInsets;
            if (providedInternalInsets != null && providedInternalInsets.length > 1 && providedInternalInsets[1] != null) {
                inOutFrame.inset(providedInternalInsets[1]);
            }
            inOutFrame.inset(win.mGivenContentInsets);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addWindowLw$3(DisplayFrames displayFrames, WindowContainer windowContainer, Rect inOutFrame) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addWindowLw$4$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7977lambda$addWindowLw$4$comandroidserverwmDisplayPolicy(DisplayFrames displayFrames, WindowContainer windowContainer, Rect inOutFrame) {
        inOutFrame.top -= this.mBottomGestureAdditionalInset;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addWindowLw$5$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7978lambda$addWindowLw$5$comandroidserverwmDisplayPolicy(DisplayFrames displayFrames, WindowContainer windowContainer, Rect inOutFrame) {
        int leftSafeInset = Math.max(displayFrames.mDisplayCutoutSafe.left, 0);
        inOutFrame.left = 0;
        inOutFrame.top = 0;
        inOutFrame.bottom = displayFrames.mDisplayHeight;
        inOutFrame.right = this.mLeftGestureInset + leftSafeInset;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addWindowLw$6$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7979lambda$addWindowLw$6$comandroidserverwmDisplayPolicy(DisplayFrames displayFrames, WindowContainer windowContainer, Rect inOutFrame) {
        int rightSafeInset = Math.min(displayFrames.mDisplayCutoutSafe.right, displayFrames.mUnrestricted.right);
        inOutFrame.left = rightSafeInset - this.mRightGestureInset;
        inOutFrame.top = 0;
        inOutFrame.bottom = displayFrames.mDisplayHeight;
        inOutFrame.right = displayFrames.mDisplayWidth;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addWindowLw$7$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7980lambda$addWindowLw$7$comandroidserverwmDisplayPolicy(WindowState win, DisplayFrames displayFrames, WindowContainer windowContainer, Rect inOutFrame) {
        if ((win.getAttrs().flags & 16) != 0 || this.mNavigationBarLetsThroughTaps) {
            inOutFrame.setEmpty();
        } else if (navigationBarPosition(displayFrames.mRotation) == 4 && !this.mNavButtonForcedVisible) {
            inOutFrame.top = inOutFrame.bottom - getNavigationBarHeight(displayFrames.mRotation);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addWindowLw$8(WindowState win, int insetsType, DisplayFrames displayFrames, WindowContainer windowContainer, Rect inOutFrame) {
        Insets[] providedInternalImeInsets = win.getLayoutingAttrs(displayFrames.mRotation).providedInternalImeInsets;
        if (providedInternalImeInsets != null && providedInternalImeInsets.length > insetsType && providedInternalImeInsets[insetsType] != null) {
            inOutFrame.inset(providedInternalImeInsets[insetsType]);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addWindowLw$9(WindowState win, int insetsType, DisplayFrames displayFrames, WindowContainer windowContainer, Rect inOutFrame) {
        Insets[] providedInternalInsets = win.getLayoutingAttrs(displayFrames.mRotation).providedInternalInsets;
        if (providedInternalInsets != null && providedInternalInsets.length > insetsType && providedInternalInsets[insetsType] != null) {
            inOutFrame.inset(providedInternalInsets[insetsType]);
        }
        inOutFrame.inset(win.mGivenContentInsets);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addWindowLw$10$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7974lambda$addWindowLw$10$comandroidserverwmDisplayPolicy(DisplayFrames displayFrames, WindowContainer windowState, Rect inOutFrame) {
        int leftSafeInset = Math.max(displayFrames.mDisplayCutoutSafe.left, 0);
        inOutFrame.left = 0;
        inOutFrame.top = 0;
        inOutFrame.bottom = displayFrames.mDisplayHeight;
        inOutFrame.right = this.mLeftGestureInset + leftSafeInset;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addWindowLw$11$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7975lambda$addWindowLw$11$comandroidserverwmDisplayPolicy(DisplayFrames displayFrames, WindowContainer windowState, Rect inOutFrame) {
        int rightSafeInset = Math.min(displayFrames.mDisplayCutoutSafe.right, displayFrames.mUnrestricted.right);
        inOutFrame.left = rightSafeInset - this.mRightGestureInset;
        inOutFrame.top = 0;
        inOutFrame.bottom = displayFrames.mDisplayHeight;
        inOutFrame.right = displayFrames.mDisplayWidth;
    }

    private int getAltBarPosition(WindowManager.LayoutParams params) {
        switch (params.gravity) {
            case 3:
                return 1;
            case 5:
                return 2;
            case 48:
                return 8;
            case 80:
                return 4;
            default:
                return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TriConsumer<DisplayFrames, WindowContainer, Rect> getImeSourceFrameProvider() {
        return new TriConsumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda23
            public final void accept(Object obj, Object obj2, Object obj3) {
                DisplayPolicy.this.m7982x9bdf3c23((DisplayFrames) obj, (WindowContainer) obj2, (Rect) obj3);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getImeSourceFrameProvider$12$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7982x9bdf3c23(DisplayFrames displayFrames, WindowContainer windowContainer, Rect inOutFrame) {
        WindowState windowState = windowContainer.asWindowState();
        if (windowState == null) {
            throw new IllegalArgumentException("IME insets must be provided by a window.");
        }
        if (this.mNavigationBar != null && navigationBarPosition(displayFrames.mRotation) == 4) {
            Rect rect = sTmpRect;
            rect.set(inOutFrame);
            rect.intersectUnchecked(this.mNavigationBar.getFrame());
            inOutFrame.inset(windowState.mGivenContentInsets);
            inOutFrame.union(rect);
            return;
        }
        inOutFrame.inset(windowState.mGivenContentInsets);
    }

    private static void enforceSingleInsetsTypeCorrespondingToWindowType(int[] insetsTypes) {
        int count = 0;
        for (int insetsType : insetsTypes) {
            switch (insetsType) {
                case 0:
                case 1:
                case 2:
                case 20:
                case 21:
                    count++;
                    if (count > 1) {
                        throw new IllegalArgumentException("Multiple InsetsTypes corresponding to Window type");
                    }
                    break;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeWindowLw(WindowState win) {
        if (this.mStatusBar == win || this.mStatusBarAlt == win) {
            this.mStatusBar = null;
            this.mStatusBarAlt = null;
            this.mDisplayContent.setInsetProvider(0, null, null);
        } else if (this.mNavigationBar == win || this.mNavigationBarAlt == win) {
            this.mNavigationBar = null;
            this.mNavigationBarAlt = null;
            this.mDisplayContent.setInsetProvider(1, null, null);
        } else if (this.mNotificationShade == win) {
            this.mNotificationShade = null;
        } else if (this.mClimateBarAlt == win) {
            this.mClimateBarAlt = null;
            this.mDisplayContent.setInsetProvider(20, null, null);
        } else if (this.mExtraNavBarAlt == win) {
            this.mExtraNavBarAlt = null;
            this.mDisplayContent.setInsetProvider(21, null, null);
        } else if (this.mWindowForDreamAnimation == win) {
            Slog.i("WindowManager", "set dreamAnimationWindow=" + win);
            this.mWindowForDreamAnimation = null;
        } else if (this.mAodWindow == win) {
            this.mDreamWindowExistedCount = 0;
        } else if (this.mGLWallpaperWindow == win) {
            Slog.i("WindowManager", "removeWindow mGLWallpaperWindow=" + win);
            this.mGLWallpaperWindow = null;
        }
        if (this.mLastFocusedWindow == win) {
            this.mLastFocusedWindow = null;
        }
        this.mInsetsSourceWindowsExceptIme.remove(win);
    }

    private int getStatusBarHeight(DisplayFrames displayFrames) {
        int statusBarHeight;
        WindowState windowState = this.mStatusBar;
        if (windowState != null) {
            statusBarHeight = windowState.getLayoutingAttrs(displayFrames.mRotation).height;
        } else {
            statusBarHeight = 0;
        }
        return Math.max(statusBarHeight, displayFrames.mDisplayCutoutSafe.top);
    }

    int getStatusBarHeightForRotation(int rotation) {
        return SystemBarUtils.getStatusBarHeightForRotation(this.mUiContext, rotation);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getStatusBar() {
        WindowState windowState = this.mStatusBar;
        return windowState != null ? windowState : this.mStatusBarAlt;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getNotificationShade() {
        return this.mNotificationShade;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getNavigationBar() {
        WindowState windowState = this.mNavigationBar;
        return windowState != null ? windowState : this.mNavigationBarAlt;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int selectAnimation(WindowState win, int transit) {
        boolean z = true;
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(win);
            long protoLogParam1 = transit;
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ANIM, 341360111, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
        }
        if (win == this.mStatusBar) {
            if (transit == 2 || transit == 4) {
                return 17432622;
            }
            if (transit == 1 || transit == 3) {
                return 17432621;
            }
        } else if (win == this.mNavigationBar) {
            if (win.getAttrs().windowAnimations != 0) {
                return 0;
            }
            int i = this.mNavigationBarPosition;
            if (i == 4) {
                if (transit == 2 || transit == 4) {
                    return this.mService.mPolicy.isKeyguardShowingAndNotOccluded() ? 17432616 : 17432615;
                } else if (transit == 1 || transit == 3) {
                    return 17432614;
                }
            } else if (i == 2) {
                if (transit == 2 || transit == 4) {
                    return 17432620;
                }
                if (transit == 1 || transit == 3) {
                    return 17432619;
                }
            } else if (i == 1) {
                if (transit == 2 || transit == 4) {
                    return 17432618;
                }
                if (transit == 1 || transit == 3) {
                    return 17432617;
                }
            }
        } else if (win == this.mStatusBarAlt || win == this.mNavigationBarAlt || win == this.mClimateBarAlt || win == this.mExtraNavBarAlt) {
            if (win.getAttrs().windowAnimations == 0) {
                int pos = win == this.mStatusBarAlt ? this.mStatusBarAltPosition : this.mNavigationBarAltPosition;
                boolean isExitOrHide = transit == 2 || transit == 4;
                if (transit != 1 && transit != 3) {
                    z = false;
                }
                boolean isEnterOrShow = z;
                switch (pos) {
                    case 1:
                        if (isExitOrHide) {
                            return 17432618;
                        }
                        if (isEnterOrShow) {
                            return 17432617;
                        }
                        break;
                    case 2:
                        if (isExitOrHide) {
                            return 17432620;
                        }
                        if (isEnterOrShow) {
                            return 17432619;
                        }
                        break;
                    case 4:
                        if (isExitOrHide) {
                            return 17432615;
                        }
                        if (isEnterOrShow) {
                            return 17432614;
                        }
                        break;
                    case 8:
                        if (isExitOrHide) {
                            return 17432622;
                        }
                        if (isEnterOrShow) {
                            return 17432621;
                        }
                        break;
                }
            } else {
                return 0;
            }
        }
        if (transit == 5 && win.hasAppShownWindows()) {
            if (win.isActivityTypeHome()) {
                return -1;
            }
            if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ANIM, -1303628829, 0, (String) null, (Object[]) null);
                return 17432595;
            }
            return 17432595;
        }
        return 0;
    }

    public boolean areSystemBarsForcedShownLw() {
        return this.mForceShowSystemBars;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void simulateLayoutDisplay(DisplayFrames displayFrames) {
        InsetsStateController controller = this.mDisplayContent.getInsetsStateController();
        for (int i = this.mInsetsSourceWindowsExceptIme.size() - 1; i >= 0; i--) {
            WindowState win = this.mInsetsSourceWindowsExceptIme.valueAt(i);
            this.mWindowLayout.computeFrames(win.getLayoutingAttrs(displayFrames.mRotation), displayFrames.mInsetsState, displayFrames.mDisplayCutoutSafe, displayFrames.mUnrestricted, win.getWindowingMode(), -1, -1, win.getRequestedVisibilities(), (Rect) null, win.mGlobalScale, sTmpClientFrames);
            SparseArray<InsetsSource> sources = win.getProvidedInsetsSources();
            InsetsState state = displayFrames.mInsetsState;
            for (int index = sources.size() - 1; index >= 0; index--) {
                int type = sources.keyAt(index);
                state.addSource(controller.getSourceProvider(type).createSimulatedSource(displayFrames, sTmpClientFrames.frame));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateInsetsSourceFramesExceptIme(DisplayFrames displayFrames) {
        for (int i = this.mInsetsSourceWindowsExceptIme.size() - 1; i >= 0; i--) {
            WindowState win = this.mInsetsSourceWindowsExceptIme.valueAt(i);
            WindowLayout windowLayout = this.mWindowLayout;
            WindowManager.LayoutParams layoutingAttrs = win.getLayoutingAttrs(displayFrames.mRotation);
            InsetsState insetsState = displayFrames.mInsetsState;
            Rect rect = displayFrames.mDisplayCutoutSafe;
            Rect rect2 = displayFrames.mUnrestricted;
            int windowingMode = win.getWindowingMode();
            InsetsVisibilities requestedVisibilities = win.getRequestedVisibilities();
            float f = win.mGlobalScale;
            ClientWindowFrames clientWindowFrames = sTmpClientFrames;
            windowLayout.computeFrames(layoutingAttrs, insetsState, rect, rect2, windowingMode, -1, -1, requestedVisibilities, (Rect) null, f, clientWindowFrames);
            win.updateSourceFrame(clientWindowFrames.frame);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDisplayInfoChanged(DisplayInfo info) {
        this.mSystemGestures.onDisplayInfoChanged(info);
    }

    public void layoutWindowLw(WindowState win, WindowState attached, DisplayFrames displayFrames) {
        int i;
        if (!win.skipLayout()) {
            DisplayFrames displayFrames2 = win.getDisplayFrames(displayFrames);
            WindowManager.LayoutParams attrs = win.getLayoutingAttrs(displayFrames2.mRotation);
            Rect attachedWindowFrame = attached != null ? attached.getFrame() : null;
            boolean trustedSize = attrs == win.mAttrs;
            int i2 = -1;
            if (trustedSize) {
                i = win.mRequestedWidth;
            } else {
                i = -1;
            }
            int requestedWidth = i;
            if (trustedSize) {
                i2 = win.mRequestedHeight;
            }
            int requestedHeight = i2;
            if (requestedWidth > 65536 || requestedHeight > 65536) {
                Slog.v("WindowManager", "win  = " + win + " ,and win.getInsetsState()" + win.getInsetsState() + " ,and the mDisplayCutoutSafe = " + displayFrames2.mDisplayCutoutSafe + " ,and the win.getBounds() = " + win.getBounds().toShortString() + " ,and the requestedWidth = " + requestedWidth + " ,and the requestedHeight = " + requestedHeight + " ,and the win.mGlobalScale = " + win.mGlobalScale);
            }
            WindowLayout windowLayout = this.mWindowLayout;
            InsetsState insetsState = win.getInsetsState();
            Rect rect = displayFrames2.mDisplayCutoutSafe;
            Rect bounds = win.getBounds();
            int windowingMode = win.getWindowingMode();
            InsetsVisibilities requestedVisibilities = win.getRequestedVisibilities();
            float f = win.mGlobalScale;
            ClientWindowFrames clientWindowFrames = sTmpClientFrames;
            windowLayout.computeFrames(attrs, insetsState, rect, bounds, windowingMode, requestedWidth, requestedHeight, requestedVisibilities, attachedWindowFrame, f, clientWindowFrames, win.getConfiguration().windowConfiguration.isThunderbackWindow());
            win.setFrames(clientWindowFrames, win.mRequestedWidth, win.mRequestedHeight);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getTopFullscreenOpaqueWindow() {
        return this.mTopFullscreenOpaqueWindowState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTopLayoutFullscreen() {
        return this.mTopIsFullscreen;
    }

    public void beginPostLayoutPolicyLw() {
        this.mTopFullscreenOpaqueWindowState = null;
        this.mNavBarColorWindowCandidate = null;
        this.mNavBarBackgroundWindow = null;
        this.mStatusBarAppearanceRegionList.clear();
        this.mStatusBarBackgroundWindows.clear();
        this.mStatusBarColorCheckedBounds.setEmpty();
        this.mStatusBarBackgroundCheckedBounds.setEmpty();
        this.mAllowLockscreenWhenOn = false;
        this.mShowingDream = false;
        this.mShowingWindowDream = false;
        this.mIsFreeformWindowOverlappingWithNavBar = false;
    }

    public boolean isWindwoForDreamAnimation(WindowManagerPolicy.WindowState win) {
        return win == this.mWindowForDreamAnimation;
    }

    public void startKeyguardWindowAnimation(int transit, boolean isEntrance, int animationType) {
        WindowState windowState = this.mNotificationShade;
        if (windowState != null) {
            windowState.mWinAnimator.applyAnimationLocked(transit, isEntrance, animationType);
        }
    }

    void resetKeyguardWindowLayer() {
        WindowState windowState = this.mNotificationShade;
        if (windowState != null) {
            windowState.resetWithDreamAnimation();
        }
    }

    public void hideKeyguardWindowWithAnimation() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowState windowState = this.mNotificationShade;
                if (windowState != null) {
                    windowState.dreamAnimationHideLw();
                    FgThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda16
                        @Override // java.lang.Runnable
                        public final void run() {
                            DisplayPolicy.this.m7983xa6929696();
                        }
                    }, 1000L);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hideKeyguardWindowWithAnimation$13$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7983xa6929696() {
        Slog.i("WindowManager", " delay to reset keyguard layer.");
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                resetKeyguardWindowLayer();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hideAodWindowWithAnimation() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowState windowState = this.mAodWindow;
                if (windowState != null) {
                    windowState.dreamAnimationHideLw();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void applyPostLayoutPolicyLw(WindowState win, WindowManager.LayoutParams attrs, WindowState attached, WindowState imeTarget) {
        if (attrs.type == 2019) {
            DisplayFrames displayFrames = this.mDisplayContent.mDisplayFrames;
            this.mNavigationBarPosition = navigationBarPosition(displayFrames.mRotation);
        }
        boolean affectsSystemUi = win.canAffectSystemUiFlags();
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.i("WindowManager", "Win " + win + ": affectsSystemUi=" + affectsSystemUi);
        }
        if (ITranDisplayPolicy.Instance().isDreamAnimationFeatureEnable()) {
            applyDreamAnimationPolicyLw(win, imeTarget);
        } else {
            applyKeyguardPolicy(win, imeTarget);
        }
        if (attrs.type == 2044) {
            this.mService.mPolicy.applyAodPolicyLw(win);
            if (win.isDreamWindow()) {
                this.mShowingDream = true;
                this.mShowingWindowDream = true;
            }
        }
        if (attrs.type == 2013) {
            applyWallpaperPolicyLw(win);
        }
        boolean isOverlappingWithNavBar = isOverlappingWithNavBar(win);
        if (isOverlappingWithNavBar && !this.mIsFreeformWindowOverlappingWithNavBar && win.inFreeformWindowingMode()) {
            this.mIsFreeformWindowOverlappingWithNavBar = true;
        }
        if (!affectsSystemUi) {
            return;
        }
        boolean appWindow = attrs.type >= 1 && attrs.type < 2000;
        if (this.mTopFullscreenOpaqueWindowState == null) {
            int fl = attrs.flags;
            if (win.isDreamWindow() && (!this.mDreamingLockscreen || (win.isVisible() && win.hasDrawn()))) {
                this.mShowingDream = true;
                appWindow = true;
            }
            if (appWindow && attached == null && attrs.isFullscreen() && (fl & 1) != 0) {
                this.mAllowLockscreenWhenOn = true;
            }
        }
        if ((appWindow && attached == null && attrs.isFullscreen()) || attrs.type == 2031) {
            if (this.mTopFullscreenOpaqueWindowState == null) {
                this.mTopFullscreenOpaqueWindowState = win;
            }
            if (this.mStatusBar != null) {
                Rect rect = sTmpRect;
                if (rect.setIntersect(win.getFrame(), this.mStatusBar.getFrame()) && !this.mStatusBarBackgroundCheckedBounds.contains(rect)) {
                    this.mStatusBarBackgroundWindows.add(win);
                    this.mStatusBarBackgroundCheckedBounds.union(rect);
                    if (!this.mStatusBarColorCheckedBounds.contains(rect)) {
                        this.mStatusBarAppearanceRegionList.add(new AppearanceRegion(win.mAttrs.insetsFlags.appearance & 8, new Rect(win.getFrame())));
                        this.mStatusBarColorCheckedBounds.union(rect);
                    }
                }
            }
            if (isOverlappingWithNavBar) {
                if (this.mNavBarColorWindowCandidate == null) {
                    this.mNavBarColorWindowCandidate = win;
                }
                if (this.mNavBarBackgroundWindow == null) {
                    this.mNavBarBackgroundWindow = win;
                }
            }
        } else if (win.isDimming()) {
            if (this.mStatusBar != null) {
                addStatusBarAppearanceRegionsForDimmingWindow(win.mAttrs.insetsFlags.appearance & 8, this.mStatusBar.getFrame(), win.getBounds(), win.getFrame());
            }
            if (isOverlappingWithNavBar && this.mNavBarColorWindowCandidate == null) {
                this.mNavBarColorWindowCandidate = win;
            }
        }
        if (ITranWindowManagerService.Instance().shouldHideWinForKeepAwake(win)) {
            Slog.d("WindowManager", "hide win for keepAwake,win=" + win);
            win.mWinAnimator.hideLayerLocked();
        } else if (ITranWindowManagerService.Instance().shouldResetWinForKeepAwake(win)) {
            Slog.d("WindowManager", "reset win for keepAwake,win=" + win);
            win.mWinAnimator.resetLayerLocked();
        }
    }

    private void addStatusBarAppearanceRegionsForDimmingWindow(int appearance, Rect statusBarFrame, Rect winBounds, Rect winFrame) {
        Rect rect = sTmpRect;
        if (!rect.setIntersect(winBounds, statusBarFrame) || this.mStatusBarColorCheckedBounds.contains(rect)) {
            return;
        }
        if (appearance != 0) {
            Rect rect2 = sTmpRect2;
            if (rect2.setIntersect(winFrame, statusBarFrame)) {
                this.mStatusBarAppearanceRegionList.add(new AppearanceRegion(appearance, new Rect(winFrame)));
                if (!rect.equals(rect2) && rect.height() == rect2.height()) {
                    if (rect.left != rect2.left) {
                        this.mStatusBarAppearanceRegionList.add(new AppearanceRegion(0, new Rect(winBounds.left, winBounds.top, rect2.left, winBounds.bottom)));
                    }
                    if (rect.right != rect2.right) {
                        this.mStatusBarAppearanceRegionList.add(new AppearanceRegion(0, new Rect(rect2.right, winBounds.top, winBounds.right, winBounds.bottom)));
                    }
                }
                this.mStatusBarColorCheckedBounds.union(rect);
                return;
            }
        }
        this.mStatusBarAppearanceRegionList.add(new AppearanceRegion(0, new Rect(winBounds)));
        this.mStatusBarColorCheckedBounds.union(rect);
    }

    public void finishPostLayoutPolicyLw() {
        if (!this.mShowingDream) {
            this.mDreamingLockscreen = this.mService.mPolicy.isKeyguardShowingAndNotOccluded();
            if (this.mDreamingSleepTokenNeeded) {
                this.mDreamingSleepTokenNeeded = false;
                this.mHandler.obtainMessage(1, 0, 1).sendToTarget();
            }
        } else if (!this.mDreamingSleepTokenNeeded && this.mShowingWindowDream) {
            boolean isAwakeState = ITranDisplayPolicy.Instance().isAwakeState();
            if (!isAwakeState) {
                this.mDreamingSleepTokenNeeded = true;
                this.mHandler.obtainMessage(1, 1, 1).sendToTarget();
            }
        }
        updateSystemBarAttributes();
        boolean z = this.mShowingDream;
        if (z != this.mLastShowingDream) {
            this.mLastShowingDream = z;
            this.mDisplayContent.notifyKeyguardFlagsChanged();
        }
        this.mService.mPolicy.setAllowLockscreenWhenOn(getDisplayId(), this.mAllowLockscreenWhenOn);
    }

    public void applyDreamAnimationPolicyLw(WindowState win, WindowState imeTarget) {
        boolean hideByAod = false;
        boolean hideByKeygaurd = false;
        boolean notingtodoByAod = false;
        boolean notingtodoByKeyguard = false;
        boolean needAodTransparent = ITranDisplayPolicy.Instance().isNeedAodTransparent();
        boolean shouldHideByFingerprintPolicy = TranFpUnlockStateController.getInstance().canHideByFingerprint();
        if (win.canBeHiddenByAOD()) {
            if (shouldBeHiddenByAOD(win, imeTarget) && needAodTransparent) {
                hideByAod = true;
            }
        } else {
            notingtodoByAod = true;
        }
        if (win.canBeHiddenByKeyguard()) {
            if (shouldBeHiddenByKeyguard(win, imeTarget)) {
                hideByKeygaurd = true;
            }
        } else {
            notingtodoByKeyguard = true;
        }
        if (!notingtodoByAod || !notingtodoByKeyguard) {
            if (!isWindwoForDreamAnimation(win)) {
                if (hideByKeygaurd || hideByAod || (this.mNotificationShade == win && shouldHideByFingerprintPolicy)) {
                    win.hide(false, true);
                } else {
                    win.show(false, true);
                }
            }
        } else {
            hookUpdateFingerprintUnlockCount(win, shouldHideByFingerprintPolicy);
        }
        if (ITranDisplayPolicy.Instance().isAodWallpaperFeatureEnabled()) {
            Boolean dreamAnimationIsShowing = Boolean.valueOf(ITranDisplayPolicy.Instance().isDreamAnimationShowingState());
            if (isWindwoForDreamAnimation(win)) {
                if (dreamAnimationIsShowing.booleanValue()) {
                    win.show(false, true);
                } else {
                    win.hide(false, true);
                }
            }
        }
    }

    private boolean shouldBeHiddenByAOD(WindowState win, WindowState imeTarget) {
        return isDreamWindowExisted();
    }

    private void applyWallpaperPolicyLw(WindowState win) {
        if (win == this.mGLWallpaperWindow) {
            if (this.mNeedHideWallpaperWin) {
                win.hide(false, true);
                if (this.mNeedDelayShowWallpaper) {
                    this.mHandler.removeCallbacks(this.mResetWallpaperVisible);
                    this.mHandler.postDelayed(this.mResetWallpaperVisible, 600L);
                    this.mNeedDelayShowWallpaper = false;
                    return;
                }
                return;
            }
            win.show(false, true);
        }
    }

    private void applyKeyguardPolicy(WindowState win, WindowState imeTarget) {
        if (win.canBeHiddenByKeyguard()) {
            if (shouldBeHiddenByKeyguard(win, imeTarget)) {
                win.hide(false, true);
            } else {
                win.show(false, true);
            }
        } else if (this.mNotificationShade == win) {
            if (TranFpUnlockStateController.getInstance().canHideByFingerprint()) {
                win.hide(false, true);
                updateFingerprintUnlockCount();
                return;
            }
            win.show(false, true);
        }
    }

    private boolean shouldBeHiddenByKeyguard(WindowState win, WindowState imeTarget) {
        boolean hideIme = win.mIsImWindow && (this.mDisplayContent.isAodShowing() || (this.mDisplayContent.isDefaultDisplay && !this.mWindowManagerDrawComplete));
        if (hideIme) {
            return true;
        }
        if (this.mDisplayContent.isDefaultDisplay && isKeyguardShowing()) {
            boolean showImeOverKeyguard = imeTarget != null && imeTarget.isVisible() && win.mIsImWindow && (imeTarget.canShowWhenLocked() || !imeTarget.canBeHiddenByKeyguard());
            if (showImeOverKeyguard) {
                return false;
            }
            boolean allowShowWhenLocked = isKeyguardOccluded() && (win.canShowWhenLocked() || (win.mAttrs.privateFlags & 256) != 0);
            return !allowShowWhenLocked;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean topAppHidesStatusBar() {
        WindowState windowState = this.mTopFullscreenOpaqueWindowState;
        if (windowState == null || this.mForceShowSystemBars) {
            return false;
        }
        return !windowState.getRequestedVisibility(0);
    }

    public void switchUser() {
        updateCurrentUserResources();
        updateForceShowNavBarSettings();
        onConfigurationChanged();
    }

    public void onOverlayChangedLw() {
        updateCurrentUserResources();
        onConfigurationChanged();
        this.mSystemGestures.onConfigurationChanged();
    }

    public void onConfigurationChanged() {
        DisplayRotation displayRotation = this.mDisplayContent.getDisplayRotation();
        Resources res = getCurrentUserResources();
        int portraitRotation = displayRotation.getPortraitRotation();
        int upsideDownRotation = displayRotation.getUpsideDownRotation();
        int landscapeRotation = displayRotation.getLandscapeRotation();
        int seascapeRotation = displayRotation.getSeascapeRotation();
        if (hasStatusBar()) {
            int[] iArr = this.mStatusBarHeightForRotation;
            int statusBarHeightForRotation = getStatusBarHeightForRotation(portraitRotation);
            iArr[upsideDownRotation] = statusBarHeightForRotation;
            iArr[portraitRotation] = statusBarHeightForRotation;
            this.mStatusBarHeightForRotation[landscapeRotation] = getStatusBarHeightForRotation(landscapeRotation);
            this.mStatusBarHeightForRotation[seascapeRotation] = getStatusBarHeightForRotation(seascapeRotation);
            this.mDisplayCutoutTouchableRegionSize = res.getDimensionPixelSize(17105202);
        } else {
            int[] iArr2 = this.mStatusBarHeightForRotation;
            iArr2[seascapeRotation] = 0;
            iArr2[landscapeRotation] = 0;
            iArr2[upsideDownRotation] = 0;
            iArr2[portraitRotation] = 0;
            this.mDisplayCutoutTouchableRegionSize = 0;
        }
        this.mNavBarOpacityMode = res.getInteger(17694885);
        this.mLeftGestureInset = this.mGestureNavigationSettingsObserver.getLeftSensitivity(res);
        this.mRightGestureInset = this.mGestureNavigationSettingsObserver.getRightSensitivity(res);
        this.mNavButtonForcedVisible = this.mGestureNavigationSettingsObserver.areNavigationButtonForcedVisible();
        this.mNavigationBarLetsThroughTaps = res.getBoolean(17891711);
        this.mNavigationBarAlwaysShowOnSideGesture = res.getBoolean(17891708);
        this.mBottomGestureAdditionalInset = res.getDimensionPixelSize(17105360) - getNavigationBarFrameHeight(portraitRotation);
        updateConfigurationAndScreenSizeDependentBehaviors();
        boolean shouldAttach = res.getBoolean(17891372);
        if (this.mShouldAttachNavBarToAppDuringTransition != shouldAttach) {
            this.mShouldAttachNavBarToAppDuringTransition = shouldAttach;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateConfigurationAndScreenSizeDependentBehaviors() {
        Resources res = getCurrentUserResources();
        this.mNavigationBarCanMove = this.mDisplayContent.mBaseDisplayWidth != this.mDisplayContent.mBaseDisplayHeight && res.getBoolean(17891709);
        this.mDisplayContent.getDisplayRotation().updateUserDependentConfiguration(res);
    }

    private void updateCurrentUserResources() {
        int userId = this.mService.mAmInternal.getCurrentUserId();
        Context uiContext = getSystemUiContext();
        if (userId == 0) {
            this.mCurrentUserResources = uiContext.getResources();
            return;
        }
        LoadedApk pi = ActivityThread.currentActivityThread().getPackageInfo(uiContext.getPackageName(), (CompatibilityInfo) null, 0, userId);
        this.mCurrentUserResources = ResourcesManager.getInstance().getResources(uiContext.getWindowContextToken(), pi.getResDir(), (String[]) null, pi.getOverlayDirs(), pi.getOverlayPaths(), pi.getApplicationInfo().sharedLibraryFiles, Integer.valueOf(this.mDisplayContent.getDisplayId()), (Configuration) null, uiContext.getResources().getCompatibilityInfo(), (ClassLoader) null, (List) null);
    }

    Resources getCurrentUserResources() {
        if (this.mCurrentUserResources == null) {
            updateCurrentUserResources();
        }
        return this.mCurrentUserResources;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Context getContext() {
        return this.mContext;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Context getSystemUiContext() {
        return this.mUiContext;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyDisplayReady() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPolicy.this.m7985lambda$notifyDisplayReady$14$comandroidserverwmDisplayPolicy();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyDisplayReady$14$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7985lambda$notifyDisplayReady$14$comandroidserverwmDisplayPolicy() {
        int displayId = getDisplayId();
        StatusBarManagerInternal statusBar = getStatusBarManagerInternal();
        if (statusBar != null) {
            statusBar.onDisplayReady(displayId);
        }
        WallpaperManagerInternal wpMgr = (WallpaperManagerInternal) LocalServices.getService(WallpaperManagerInternal.class);
        if (wpMgr != null) {
            wpMgr.onDisplayReady(displayId);
        }
    }

    public int getNonDecorDisplayWidth(int fullWidth, int fullHeight, int rotation, int uiMode, DisplayCutout displayCutout) {
        int navBarPosition;
        int width = fullWidth;
        if (hasNavigationBar() && ((navBarPosition = navigationBarPosition(rotation)) == 1 || navBarPosition == 2)) {
            width -= this.mNavBarWidthForRotation[rotation];
        }
        if (displayCutout != null) {
            return width - (displayCutout.getSafeInsetLeft() + displayCutout.getSafeInsetRight());
        }
        return width;
    }

    int getNavigationBarHeight(int rotation) {
        return this.mNavBarHeightForRotation[rotation];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateScreenByDecorIfNeeded(WindowState win) {
        int[] iArr;
        Insets providedInternalInsets;
        if (win != this.mNavigationBar) {
            return false;
        }
        int currentRotation = this.mDisplayContent.getRotation();
        int prevHeight = this.mNavBarHeightForRotation[currentRotation];
        int prevWidth = this.mNavBarWidthForRotation[currentRotation];
        int rotation = 0;
        while (true) {
            iArr = this.mNavBarHeightForRotation;
            if (rotation >= iArr.length) {
                break;
            }
            WindowManager.LayoutParams lp = this.mNavigationBar.getLayoutingAttrs(rotation);
            if (lp.providedInternalInsets != null && lp.providedInternalInsets.length > 1 && lp.providedInternalInsets[1] != null) {
                providedInternalInsets = lp.providedInternalInsets[1];
            } else {
                providedInternalInsets = Insets.NONE;
            }
            if (lp.height < providedInternalInsets.top) {
                this.mNavBarHeightForRotation[rotation] = 0;
            } else {
                this.mNavBarHeightForRotation[rotation] = lp.height - providedInternalInsets.top;
            }
            if (lp.gravity == 3) {
                if (lp.width > providedInternalInsets.right) {
                    this.mNavBarWidthForRotation[rotation] = lp.width - providedInternalInsets.right;
                } else {
                    this.mNavBarWidthForRotation[rotation] = 0;
                }
            } else if (lp.gravity == 5) {
                if (lp.width > providedInternalInsets.left) {
                    this.mNavBarWidthForRotation[rotation] = lp.width - providedInternalInsets.left;
                } else {
                    this.mNavBarWidthForRotation[rotation] = 0;
                }
            } else {
                this.mNavBarWidthForRotation[rotation] = Math.max(lp.width, 0);
            }
            rotation++;
        }
        int rotation2 = iArr[currentRotation];
        if (!(prevHeight == rotation2 && prevWidth == this.mNavBarWidthForRotation[currentRotation]) && this.mService.mDisplayEnabled) {
            return this.mDisplayContent.updateScreenConfiguration();
        }
        return false;
    }

    private int getNavigationBarFrameHeight(int rotation) {
        WindowState windowState = this.mNavigationBar;
        if (windowState == null) {
            return 0;
        }
        return windowState.getLayoutingAttrs(rotation).height;
    }

    public int getNonDecorDisplayHeight(int fullHeight, int rotation, DisplayCutout displayCutout) {
        int height = fullHeight;
        int navBarPosition = navigationBarPosition(rotation);
        if (navBarPosition == 4) {
            height -= getNavigationBarHeight(rotation);
        }
        if (displayCutout != null) {
            return height - (displayCutout.getSafeInsetTop() + displayCutout.getSafeInsetBottom());
        }
        return height;
    }

    public int getConfigDisplayWidth(int fullWidth, int fullHeight, int rotation, int uiMode, DisplayCutout displayCutout) {
        return getNonDecorDisplayWidth(fullWidth, fullHeight, rotation, uiMode, displayCutout);
    }

    public int getConfigDisplayHeight(int fullWidth, int fullHeight, int rotation, int uiMode, DisplayCutout displayCutout) {
        int statusBarHeight = this.mStatusBarHeightForRotation[rotation];
        if (displayCutout != null) {
            statusBarHeight = Math.max(0, statusBarHeight - displayCutout.getSafeInsetTop());
        }
        return getNonDecorDisplayHeight(fullHeight, rotation, displayCutout) - statusBarHeight;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getWindowCornerRadius() {
        if (this.mDisplayContent.getDisplay().getType() == 1) {
            return ScreenDecorationsUtils.getWindowCornerRadius(this.mContext);
        }
        return 0.0f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isShowingDreamLw() {
        return this.mShowingDream;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void convertNonDecorInsetsToStableInsets(Rect inOutInsets, int rotation) {
        inOutInsets.top = Math.max(inOutInsets.top, this.mStatusBarHeightForRotation[rotation]);
    }

    public void getStableInsetsLw(int displayRotation, DisplayCutout displayCutout, Rect outInsets) {
        outInsets.setEmpty();
        getNonDecorInsetsLw(displayRotation, displayCutout, outInsets);
        convertNonDecorInsetsToStableInsets(outInsets, displayRotation);
    }

    public void getNonDecorInsetsLw(int displayRotation, DisplayCutout displayCutout, Rect outInsets) {
        outInsets.setEmpty();
        if (hasNavigationBar()) {
            int position = navigationBarPosition(displayRotation);
            if (position == 4) {
                outInsets.bottom = getNavigationBarHeight(displayRotation);
            } else if (position == 2) {
                outInsets.right = this.mNavBarWidthForRotation[displayRotation];
            } else if (position == 1) {
                outInsets.left = this.mNavBarWidthForRotation[displayRotation];
            }
        }
        if (displayCutout != null) {
            outInsets.left += displayCutout.getSafeInsetLeft();
            outInsets.top += displayCutout.getSafeInsetTop();
            outInsets.right += displayCutout.getSafeInsetRight();
            outInsets.bottom += displayCutout.getSafeInsetBottom();
        }
    }

    int navigationBarPosition(int displayRotation) {
        WindowState windowState = this.mNavigationBar;
        if (windowState != null) {
            int gravity = windowState.getLayoutingAttrs(displayRotation).gravity;
            switch (gravity) {
                case 3:
                    return 1;
                case 4:
                default:
                    return 4;
                case 5:
                    return 2;
            }
        }
        return -1;
    }

    public int getNavBarPosition() {
        return this.mNavigationBarPosition;
    }

    public void focusChangedLw(WindowState lastFocus, WindowState newFocus) {
        this.mFocusedWindow = newFocus;
        this.mLastFocusedWindow = lastFocus;
        if (this.mDisplayContent.isDefaultDisplay) {
            this.mService.mPolicy.onDefaultDisplayFocusChangedLw(newFocus);
        }
        updateSystemBarAttributes();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDreamingSleepToken(boolean acquire) {
        int displayId = getDisplayId();
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.d("WindowManager", "updateDreamingSleepToken acquire = " + acquire);
        }
        if (acquire) {
            ActivityTaskManagerInternal.SleepTokenAcquirer sleepTokenAcquirer = this.mDreamingSleepTokenAcquirer;
            if (sleepTokenAcquirer != null) {
                sleepTokenAcquirer.acquire(displayId);
                return;
            }
            return;
        }
        ActivityTaskManagerInternal.SleepTokenAcquirer sleepTokenAcquirer2 = this.mDreamingSleepTokenAcquirer;
        if (sleepTokenAcquirer2 != null) {
            sleepTokenAcquirer2.release(displayId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestTransientBars(WindowState swipeTarget, boolean isGestureOnSystemBar) {
        if (swipeTarget == null || !this.mService.mPolicy.isUserSetupComplete()) {
            return;
        }
        InsetsSourceProvider provider = swipeTarget.getControllableInsetProvider();
        InsetsControlTarget controlTarget = provider != null ? provider.getControlTarget() : null;
        if (controlTarget == null || controlTarget == getNotificationShade()) {
            return;
        }
        int restorePositionTypes = (controlTarget.getRequestedVisibility(1) ? WindowInsets.Type.navigationBars() : 0) | (controlTarget.getRequestedVisibility(0) ? WindowInsets.Type.statusBars() : 0) | ((this.mExtraNavBarAlt == null || !controlTarget.getRequestedVisibility(21)) ? 0 : WindowInsets.Type.navigationBars()) | ((this.mClimateBarAlt == null || !controlTarget.getRequestedVisibility(20)) ? 0 : WindowInsets.Type.statusBars());
        if (swipeTarget == this.mNavigationBar && (WindowInsets.Type.navigationBars() & restorePositionTypes) != 0) {
            controlTarget.showInsets(WindowInsets.Type.navigationBars(), false);
            return;
        }
        boolean onlyShowNavigationBar = false;
        if (ITranWindowManagerService.Instance().requestTransientBars(swipeTarget, swipeTarget == this.mNavigationBar, this.mNavigationBarPosition, controlTarget, this.mDisplayContent.getInsetsPolicy())) {
            restorePositionTypes &= ~WindowInsets.Type.statusBars();
            onlyShowNavigationBar = true;
        }
        if (controlTarget.canShowTransient()) {
            this.mDisplayContent.getInsetsPolicy().showTransient(onlyShowNavigationBar ? SHOW_TYPES_FOR_PANIC : SHOW_TYPES_FOR_SWIPE, isGestureOnSystemBar);
            controlTarget.showInsets(restorePositionTypes, false);
        } else {
            controlTarget.showInsets(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars(), false);
            WindowState windowState = this.mStatusBar;
            if (swipeTarget == windowState && this.mLastImmersiveMode) {
                boolean transferred = windowState.transferTouch();
                if (!transferred) {
                    Slog.i("WindowManager", "Could not transfer touch to the status bar");
                }
            }
        }
        this.mImmersiveModeConfirmation.confirmCurrentPrompt();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardShowing() {
        return this.mService.mPolicy.isKeyguardShowing();
    }

    private boolean isKeyguardOccluded() {
        return this.mService.mPolicy.isKeyguardOccluded();
    }

    InsetsPolicy getInsetsPolicy() {
        return this.mDisplayContent.getInsetsPolicy();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetSystemBarAttributes() {
        this.mLastDisableFlags = 0;
        updateSystemBarAttributes();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSystemBarAttributes() {
        WindowState windowState;
        WindowState windowState2;
        WindowState winCandidate = this.mFocusedWindow;
        if (this.mFocusedWindow != null && ITranWindowManagerService.Instance().useTopFullscreenWindowAsCandidate(this.mFocusedWindow.getWindowingMode(), this.mFocusedWindow.getOwningPackage(), this.mFocusedWindow.getAttrs().type)) {
            winCandidate = null;
        }
        if (winCandidate == null && (windowState2 = this.mTopFullscreenOpaqueWindowState) != null && (windowState2.mAttrs.flags & 8) == 0) {
            winCandidate = this.mTopFullscreenOpaqueWindowState;
        }
        if (winCandidate == null || winCandidate.getWindowingMode() == 2) {
            return;
        }
        if (winCandidate != null && winCandidate.getConfiguration().windowConfiguration.isThunderbackWindow()) {
            WindowState windowState3 = this.mTopFullscreenOpaqueWindowState;
            if (windowState3 != null && windowState3 != winCandidate && windowState3.getWindowingMode() != 2) {
                winCandidate = this.mTopFullscreenOpaqueWindowState;
            } else {
                return;
            }
        }
        boolean z = false;
        if (winCandidate.getAttrs().token == this.mImmersiveModeConfirmation.getWindowToken()) {
            Slog.d("WindowManager", "Ims confirm:" + this.mImmersiveModeConfirmation.getWindowToken());
            WindowState windowState4 = this.mLastFocusedWindow;
            boolean lastFocusCanReceiveKeys = windowState4 != null && windowState4.canReceiveKeys();
            if (isKeyguardShowing() && !isKeyguardOccluded()) {
                windowState = this.mNotificationShade;
            } else {
                windowState = lastFocusCanReceiveKeys ? this.mLastFocusedWindow : this.mTopFullscreenOpaqueWindowState;
            }
            winCandidate = windowState;
            if (winCandidate == null) {
                return;
            }
        }
        if (winCandidate.getRootDisplayArea() == null) {
            Slog.d("WindowManager", "====updateSystemUiVisibilityLw start====");
            Slog.d("WindowManager", "winCandidate:" + winCandidate, new Throwable());
            Slog.d("WindowManager", "mFocusedWindow:" + this.mFocusedWindow);
            Slog.d("WindowManager", "mTopFullscreenOpaqueWindowState:" + this.mTopFullscreenOpaqueWindowState);
            Slog.d("WindowManager", "mNotificationShade:" + this.mNotificationShade);
            Slog.d("WindowManager", "mLastFocusedWindow:" + this.mLastFocusedWindow);
            Slog.d("WindowManager", "====updateSystemUiVisibilityLw end====");
        }
        WindowState win = winCandidate;
        this.mSystemUiControllingWindow = win;
        final int displayId = getDisplayId();
        final int disableFlags = win.getDisableFlags();
        int opaqueAppearance = updateSystemBarsLw(win, disableFlags);
        WindowState navColorWin = chooseNavigationColorWindowLw(this.mNavBarColorWindowCandidate, this.mDisplayContent.mInputMethodWindow, this.mNavigationBarPosition);
        final boolean isNavbarColorManagedByIme = navColorWin != null && navColorWin == this.mDisplayContent.mInputMethodWindow;
        final int appearance = updateLightNavigationBarLw(win.mAttrs.insetsFlags.appearance, navColorWin) | opaqueAppearance;
        final int behavior = win.mAttrs.insetsFlags.behavior;
        final String focusedApp = win.mAttrs.packageName;
        boolean isFullscreen = (win.getRequestedVisibility(0) && win.getRequestedVisibility(1)) ? false : true;
        final AppearanceRegion[] statusBarAppearanceRegions = new AppearanceRegion[this.mStatusBarAppearanceRegionList.size()];
        this.mStatusBarAppearanceRegionList.toArray(statusBarAppearanceRegions);
        if (this.mLastDisableFlags != disableFlags) {
            this.mLastDisableFlags = disableFlags;
            final String cause = win.toString();
            callStatusBarSafely(new Consumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((StatusBarManagerInternal) obj).setDisableFlags(displayId, disableFlags, cause);
                }
            });
        }
        if (this.mLastAppearance == appearance && this.mLastBehavior == behavior && this.mRequestedVisibilities.equals(win.getRequestedVisibilities()) && Objects.equals(this.mFocusedApp, focusedApp) && this.mLastFocusIsFullscreen == isFullscreen && Arrays.equals(this.mLastStatusBarAppearanceRegions, statusBarAppearanceRegions)) {
            return;
        }
        if (this.mDisplayContent.isDefaultDisplay && this.mLastFocusIsFullscreen != isFullscreen && ((this.mLastAppearance ^ appearance) & 4) != 0) {
            this.mService.mInputManager.setSystemUiLightsOut((isFullscreen || (appearance & 4) != 0) ? true : true);
        }
        final InsetsVisibilities requestedVisibilities = new InsetsVisibilities(win.getRequestedVisibilities());
        this.mLastAppearance = appearance;
        this.mLastBehavior = behavior;
        this.mRequestedVisibilities = requestedVisibilities;
        this.mFocusedApp = focusedApp;
        this.mLastFocusIsFullscreen = isFullscreen;
        this.mLastStatusBarAppearanceRegions = statusBarAppearanceRegions;
        callStatusBarSafely(new Consumer() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((StatusBarManagerInternal) obj).onSystemBarAttributesChanged(displayId, appearance, statusBarAppearanceRegions, isNavbarColorManagedByIme, behavior, requestedVisibilities, focusedApp);
            }
        });
    }

    private void callStatusBarSafely(final Consumer<StatusBarManagerInternal> consumer) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                DisplayPolicy.this.m7981x503da5a3(consumer);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$callStatusBarSafely$17$com-android-server-wm-DisplayPolicy  reason: not valid java name */
    public /* synthetic */ void m7981x503da5a3(Consumer consumer) {
        StatusBarManagerInternal statusBar = getStatusBarManagerInternal();
        if (statusBar != null) {
            consumer.accept(statusBar);
        }
    }

    static WindowState chooseNavigationColorWindowLw(WindowState candidate, WindowState imeWindow, int navBarPosition) {
        boolean imeWindowCanNavColorWindow = imeWindow != null && imeWindow.isVisible() && navBarPosition == 4 && (imeWindow.mAttrs.flags & Integer.MIN_VALUE) != 0;
        if (!imeWindowCanNavColorWindow) {
            return candidate;
        }
        if (candidate != null && candidate.isDimming()) {
            if (WindowManager.LayoutParams.mayUseInputMethod(candidate.mAttrs.flags)) {
                return imeWindow;
            }
            return candidate;
        }
        return imeWindow;
    }

    int updateLightNavigationBarLw(int appearance, WindowState navColorWin) {
        if (navColorWin == null || !isLightBarAllowed(navColorWin, WindowInsets.Type.navigationBars())) {
            return appearance & (-17);
        }
        return (appearance & (-17)) | (navColorWin.mAttrs.insetsFlags.appearance & 16);
    }

    private int updateSystemBarsLw(WindowState win, int disableFlags) {
        WindowState windowState;
        StatusBarManagerInternal statusBar;
        TaskDisplayArea defaultTaskDisplayArea = this.mDisplayContent.getDefaultTaskDisplayArea();
        boolean pendingPanic = false;
        boolean multiWindowTaskVisible = defaultTaskDisplayArea.getRootTask(new Predicate() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda22
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return DisplayPolicy.lambda$updateSystemBarsLw$18((Task) obj);
            }
        }) != null;
        boolean freeformRootTaskVisible = defaultTaskDisplayArea.isRootTaskVisible(5);
        this.mForceShowSystemBars = multiWindowTaskVisible || freeformRootTaskVisible;
        this.mDisplayContent.getInsetsPolicy().updateBarControlTarget(win);
        boolean topAppHidesStatusBar = topAppHidesStatusBar();
        if (getStatusBar() != null && (statusBar = getStatusBarManagerInternal()) != null) {
            statusBar.setTopAppHidesStatusBar(topAppHidesStatusBar);
        }
        this.mTopIsFullscreen = topAppHidesStatusBar && ((windowState = this.mNotificationShade) == null || !windowState.isVisible());
        int appearance = configureNavBarOpacity(configureStatusBarOpacity(3), multiWindowTaskVisible, freeformRootTaskVisible);
        boolean requestHideNavBar = !win.getRequestedVisibility(1);
        long now = SystemClock.uptimeMillis();
        long j = this.mPendingPanicGestureUptime;
        if (j != 0 && now - j <= 30000) {
            pendingPanic = true;
        }
        DisplayPolicy defaultDisplayPolicy = this.mService.getDefaultDisplayContentLocked().getDisplayPolicy();
        if (pendingPanic && requestHideNavBar && win != this.mNotificationShade && getInsetsPolicy().isHidden(1) && defaultDisplayPolicy.isKeyguardDrawComplete()) {
            this.mPendingPanicGestureUptime = 0L;
            if (!isNavBarEmpty(disableFlags)) {
                this.mDisplayContent.getInsetsPolicy().showTransient(SHOW_TYPES_FOR_PANIC, true);
            }
        }
        boolean oldImmersiveMode = this.mLastImmersiveMode;
        boolean newImmersiveMode = isImmersiveMode(win);
        if (oldImmersiveMode != newImmersiveMode) {
            Slog.d("WindowManager", "update navigation bar window=" + win + ", disableFlags=" + disableFlags + ", appearance=" + appearance + ", this=" + this);
            this.mLastImmersiveMode = newImmersiveMode;
            RootDisplayArea root = win.getRootDisplayArea();
            int rootDisplayAreaId = root == null ? -1 : root.mFeatureId;
            this.mImmersiveModeConfirmation.immersiveModeChangedLw(rootDisplayAreaId, newImmersiveMode, this.mService.mPolicy.isUserSetupComplete(), isNavBarEmpty(disableFlags));
        }
        return appearance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$updateSystemBarsLw$18(Task task) {
        return task.isVisible() && task.getTopLeafTask().getWindowingMode() == 6;
    }

    private static boolean isLightBarAllowed(WindowState win, int type) {
        if (win == null) {
            return false;
        }
        if (ITranDisplayPolicy.Instance().isThunderbackWindow(win)) {
            return true;
        }
        return intersectsAnyInsets(win.getFrame(), win.getInsetsState(), type);
    }

    private Rect getBarContentFrameForWindow(WindowState win, int type) {
        DisplayFrames displayFrames = win.getDisplayFrames(this.mDisplayContent.mDisplayFrames);
        InsetsState state = displayFrames.mInsetsState;
        Rect tmpRect = new Rect();
        Rect rect = sTmpDisplayCutoutSafe;
        rect.set(displayFrames.mDisplayCutoutSafe);
        if (type == 0) {
            rect.top = Math.max(state.getDisplayCutout().getWaterfallInsets().top, 0);
        }
        InsetsSource source = state.peekSource(type);
        if (source != null) {
            tmpRect.set(source.getFrame());
            tmpRect.intersect(rect);
        }
        return tmpRect;
    }

    boolean isFullyTransparentAllowed(WindowState win, int type) {
        if (win == null) {
            return true;
        }
        return win.isFullyTransparentBarAllowed(getBarContentFrameForWindow(win, type));
    }

    private boolean drawsBarBackground(WindowState win) {
        if (win == null) {
            return true;
        }
        boolean drawsSystemBars = (win.getAttrs().flags & Integer.MIN_VALUE) != 0;
        boolean forceDrawsSystemBars = (win.getAttrs().privateFlags & 131072) != 0;
        return forceDrawsSystemBars || drawsSystemBars;
    }

    private int configureStatusBarOpacity(int appearance) {
        boolean drawBackground = true;
        boolean isFullyTransparentAllowed = true;
        for (int i = this.mStatusBarBackgroundWindows.size() - 1; i >= 0; i--) {
            WindowState window = this.mStatusBarBackgroundWindows.get(i);
            drawBackground &= drawsBarBackground(window);
            isFullyTransparentAllowed &= isFullyTransparentAllowed(window, 0);
        }
        if (drawBackground) {
            appearance &= -2;
        }
        if (!isFullyTransparentAllowed) {
            return appearance | 32;
        }
        return appearance;
    }

    private int configureNavBarOpacity(int appearance, boolean multiWindowTaskVisible, boolean freeformRootTaskVisible) {
        boolean drawBackground = drawsBarBackground(this.mNavBarBackgroundWindow);
        int i = this.mNavBarOpacityMode;
        if (i == 2) {
            if (drawBackground) {
                appearance = clearNavBarOpaqueFlag(appearance);
            }
        } else if (i == 0) {
            if (multiWindowTaskVisible || freeformRootTaskVisible) {
                if (this.mIsFreeformWindowOverlappingWithNavBar) {
                    appearance = clearNavBarOpaqueFlag(appearance);
                }
            } else if (drawBackground) {
                appearance = clearNavBarOpaqueFlag(appearance);
            }
        } else if (i == 1 && freeformRootTaskVisible) {
            appearance = clearNavBarOpaqueFlag(appearance);
        }
        if (!isFullyTransparentAllowed(this.mNavBarBackgroundWindow, 1)) {
            return appearance | 64;
        }
        return appearance;
    }

    private int clearNavBarOpaqueFlag(int appearance) {
        return appearance & (-3);
    }

    private boolean isImmersiveMode(WindowState win) {
        return (win == null || getNavigationBar() == null || !canHideNavigationBar() || !getInsetsPolicy().isHidden(1) || win == getNotificationShade() || win.isActivityTypeDream()) ? false : true;
    }

    private boolean canHideNavigationBar() {
        return hasNavigationBar();
    }

    private static boolean isNavBarEmpty(int systemUiFlags) {
        return (systemUiFlags & 23068672) == 23068672;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPowerKeyDown(boolean isScreenOn) {
        boolean panic = this.mImmersiveModeConfirmation.onPowerKeyDown(isScreenOn, SystemClock.elapsedRealtime(), isImmersiveMode(this.mSystemUiControllingWindow), isNavBarEmpty(this.mLastDisableFlags));
        if (panic) {
            this.mHandler.post(this.mHiddenNavPanic);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onVrStateChangedLw(boolean enabled) {
        this.mImmersiveModeConfirmation.onVrStateChangedLw(enabled);
    }

    public void onLockTaskStateChangedLw(int lockTaskState) {
        this.mImmersiveModeConfirmation.onLockTaskModeChangedLw(lockTaskState);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onSystemUiSettingsChanged() {
        return this.mImmersiveModeConfirmation.onSettingChanged(this.mService.mCurrentUserId);
    }

    public void takeScreenshot(int screenshotType, int source) {
        ScreenshotHelper screenshotHelper = this.mScreenshotHelper;
        if (screenshotHelper != null) {
            boolean z = false;
            boolean z2 = getStatusBar() != null && getStatusBar().isVisible();
            if (getNavigationBar() != null && getNavigationBar().isVisible()) {
                z = true;
            }
            screenshotHelper.takeScreenshot(screenshotType, z2, z, source, this.mHandler, (Consumer) null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RefreshRatePolicy getRefreshRatePolicy() {
        return this.mRefreshRatePolicy;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.println("DisplayPolicy");
        String prefix2 = prefix + "  ";
        String prefixInner = prefix2 + "  ";
        pw.print(prefix2);
        pw.print("mCarDockEnablesAccelerometer=");
        pw.print(this.mCarDockEnablesAccelerometer);
        pw.print(" mDeskDockEnablesAccelerometer=");
        pw.println(this.mDeskDockEnablesAccelerometer);
        pw.print(prefix2);
        pw.print("mDockMode=");
        pw.print(Intent.dockStateToString(this.mDockMode));
        pw.print(" mLidState=");
        pw.println(WindowManagerPolicy.WindowManagerFuncs.lidStateToString(this.mLidState));
        pw.print(prefix2);
        pw.print("mAwake=");
        pw.print(this.mAwake);
        pw.print(" mScreenOnEarly=");
        pw.print(this.mScreenOnEarly);
        pw.print(" mScreenOnFully=");
        pw.println(this.mScreenOnFully);
        pw.print(prefix2);
        pw.print("mKeyguardDrawComplete=");
        pw.print(this.mKeyguardDrawComplete);
        pw.print(" mWindowManagerDrawComplete=");
        pw.println(this.mWindowManagerDrawComplete);
        pw.print(prefix2);
        pw.print("mHdmiPlugged=");
        pw.println(this.mHdmiPlugged);
        if (this.mLastDisableFlags != 0) {
            pw.print(prefix2);
            pw.print("mLastDisableFlags=0x");
            pw.println(Integer.toHexString(this.mLastDisableFlags));
        }
        if (this.mLastAppearance != 0) {
            pw.print(prefix2);
            pw.print("mLastAppearance=");
            pw.println(ViewDebug.flagsToString(InsetsFlags.class, "appearance", this.mLastAppearance));
        }
        if (this.mLastBehavior != 0) {
            pw.print(prefix2);
            pw.print("mLastBehavior=");
            pw.println(ViewDebug.flagsToString(InsetsFlags.class, "behavior", this.mLastBehavior));
        }
        pw.print(prefix2);
        pw.print("mShowingDream=");
        pw.print(this.mShowingDream);
        pw.print(" mDreamingLockscreen=");
        pw.println(this.mDreamingLockscreen);
        if (this.mStatusBar != null) {
            pw.print(prefix2);
            pw.print("mStatusBar=");
            pw.println(this.mStatusBar);
        }
        if (this.mStatusBarAlt != null) {
            pw.print(prefix2);
            pw.print("mStatusBarAlt=");
            pw.println(this.mStatusBarAlt);
            pw.print(prefix2);
            pw.print("mStatusBarAltPosition=");
            pw.println(this.mStatusBarAltPosition);
        }
        if (this.mNotificationShade != null) {
            pw.print(prefix2);
            pw.print("mExpandedPanel=");
            pw.println(this.mNotificationShade);
        }
        pw.print(prefix2);
        pw.print("isKeyguardShowing=");
        pw.println(isKeyguardShowing());
        if (this.mNavigationBar != null) {
            pw.print(prefix2);
            pw.print("mNavigationBar=");
            pw.println(this.mNavigationBar);
            pw.print(prefix2);
            pw.print("mNavBarOpacityMode=");
            pw.println(this.mNavBarOpacityMode);
            pw.print(prefix2);
            pw.print("mNavigationBarCanMove=");
            pw.println(this.mNavigationBarCanMove);
            pw.print(prefix2);
            pw.print("mNavigationBarPosition=");
            pw.println(this.mNavigationBarPosition);
        }
        if (this.mNavigationBarAlt != null) {
            pw.print(prefix2);
            pw.print("mNavigationBarAlt=");
            pw.println(this.mNavigationBarAlt);
            pw.print(prefix2);
            pw.print("mNavigationBarAltPosition=");
            pw.println(this.mNavigationBarAltPosition);
        }
        if (this.mClimateBarAlt != null) {
            pw.print(prefix2);
            pw.print("mClimateBarAlt=");
            pw.println(this.mClimateBarAlt);
            pw.print(prefix2);
            pw.print("mClimateBarAltPosition=");
            pw.println(this.mClimateBarAltPosition);
        }
        if (this.mExtraNavBarAlt != null) {
            pw.print(prefix2);
            pw.print("mExtraNavBarAlt=");
            pw.println(this.mExtraNavBarAlt);
            pw.print(prefix2);
            pw.print("mExtraNavBarAltPosition=");
            pw.println(this.mExtraNavBarAltPosition);
        }
        if (this.mFocusedWindow != null) {
            pw.print(prefix2);
            pw.print("mFocusedWindow=");
            pw.println(this.mFocusedWindow);
        }
        if (this.mTopFullscreenOpaqueWindowState != null) {
            pw.print(prefix2);
            pw.print("mTopFullscreenOpaqueWindowState=");
            pw.println(this.mTopFullscreenOpaqueWindowState);
        }
        if (this.mNavBarColorWindowCandidate != null) {
            pw.print(prefix2);
            pw.print("mNavBarColorWindowCandidate=");
            pw.println(this.mNavBarColorWindowCandidate);
        }
        if (this.mNavBarBackgroundWindow != null) {
            pw.print(prefix2);
            pw.print("mNavBarBackgroundWindow=");
            pw.println(this.mNavBarBackgroundWindow);
        }
        if (this.mLastStatusBarAppearanceRegions != null) {
            pw.print(prefix2);
            pw.println("mLastStatusBarAppearanceRegions=");
            for (int i = this.mLastStatusBarAppearanceRegions.length - 1; i >= 0; i--) {
                pw.print(prefixInner);
                pw.println(this.mLastStatusBarAppearanceRegions[i]);
            }
        }
        if (!this.mStatusBarBackgroundWindows.isEmpty()) {
            pw.print(prefix2);
            pw.println("mStatusBarBackgroundWindows=");
            for (int i2 = this.mStatusBarBackgroundWindows.size() - 1; i2 >= 0; i2--) {
                WindowState win = this.mStatusBarBackgroundWindows.get(i2);
                pw.print(prefixInner);
                pw.println(win);
            }
        }
        pw.print(prefix2);
        pw.print("mTopIsFullscreen=");
        pw.println(this.mTopIsFullscreen);
        pw.print(prefix2);
        pw.print("mForceShowNavigationBarEnabled=");
        pw.print(this.mForceShowNavigationBarEnabled);
        pw.print(" mAllowLockscreenWhenOn=");
        pw.println(this.mAllowLockscreenWhenOn);
        pw.print(prefix2);
        pw.print("mRemoteInsetsControllerControlsSystemBars=");
        pw.println(this.mDisplayContent.getInsetsPolicy().getRemoteInsetsControllerControlsSystemBars());
        this.mSystemGestures.dump(pw, prefix2);
        pw.print(prefix2);
        pw.println("Looper state:");
        this.mHandler.getLooper().dump(new PrintWriterPrinter(pw), prefix2 + "  ");
    }

    private boolean supportsPointerLocation() {
        return this.mDisplayContent.isDefaultDisplay || !this.mDisplayContent.isPrivate();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPointerLocationEnabled(boolean pointerLocationEnabled) {
        if (!supportsPointerLocation()) {
            return;
        }
        this.mHandler.sendEmptyMessage(pointerLocationEnabled ? 4 : 5);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enablePointerLocation() {
        if (this.mPointerLocationView != null) {
            return;
        }
        PointerLocationView pointerLocationView = new PointerLocationView(this.mContext);
        this.mPointerLocationView = pointerLocationView;
        pointerLocationView.setPrintCoords(false);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = 2015;
        lp.flags = FrameworkStatsLog.TV_CAS_SESSION_OPEN_STATUS;
        lp.setFitInsetsTypes(0);
        lp.layoutInDisplayCutoutMode = 3;
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
            lp.privateFlags |= 2;
        }
        lp.format = -3;
        lp.setTitle("PointerLocation - display " + getDisplayId());
        lp.inputFeatures |= 1;
        WindowManager wm = (WindowManager) this.mContext.getSystemService(WindowManager.class);
        wm.addView(this.mPointerLocationView, lp);
        this.mDisplayContent.registerPointerEventListener(this.mPointerLocationView);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void disablePointerLocation() {
        WindowManagerPolicyConstants.PointerEventListener pointerEventListener = this.mPointerLocationView;
        if (pointerEventListener == null) {
            return;
        }
        this.mDisplayContent.unregisterPointerEventListener(pointerEventListener);
        WindowManager wm = (WindowManager) this.mContext.getSystemService(WindowManager.class);
        wm.removeView(this.mPointerLocationView);
        this.mPointerLocationView = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removePointerLocation() {
        if (this.mPointerLocationView != null) {
            WindowManager wm = (WindowManager) this.mContext.getSystemService(WindowManager.class);
            wm.removeView(this.mPointerLocationView);
            this.mPointerLocationView = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isWindowExcludedFromContent(WindowState w) {
        return (w == null || this.mPointerLocationView == null || w.mClient != this.mPointerLocationView.getWindowToken()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void release() {
        this.mDisplayContent.mTransitionController.unregisterLegacyListener(this.mAppTransitionListener);
        Handler handler = this.mHandler;
        final GestureNavigationSettingsObserver gestureNavigationSettingsObserver = this.mGestureNavigationSettingsObserver;
        Objects.requireNonNull(gestureNavigationSettingsObserver);
        handler.post(new Runnable() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                gestureNavigationSettingsObserver.unregister();
            }
        });
        Handler handler2 = this.mHandler;
        final ForceShowNavBarSettingsObserver forceShowNavBarSettingsObserver = this.mForceShowNavBarSettingsObserver;
        Objects.requireNonNull(forceShowNavBarSettingsObserver);
        handler2.post(new Runnable() { // from class: com.android.server.wm.DisplayPolicy$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                forceShowNavBarSettingsObserver.unregister();
            }
        });
        this.mImmersiveModeConfirmation.release();
        this.mHandler.sendEmptyMessage(99);
    }

    static boolean isOverlappingWithNavBar(WindowState win) {
        if (win.mActivityRecord == null || !win.isVisible()) {
            return false;
        }
        return intersectsAnyInsets(win.isDimming() ? win.getBounds() : win.getFrame(), win.getInsetsState(), WindowInsets.Type.navigationBars());
    }

    private static boolean intersectsAnyInsets(Rect bounds, InsetsState insetsState, int insetsType) {
        ArraySet<Integer> internalTypes = InsetsState.toInternalType(insetsType);
        for (int i = 0; i < internalTypes.size(); i++) {
            InsetsSource source = insetsState.peekSource(internalTypes.valueAt(i).intValue());
            if (source != null && source.isVisible() && Rect.intersects(bounds, source.getFrame())) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldAttachNavBarToAppDuringTransition() {
        return this.mShouldAttachNavBarToAppDuringTransition && this.mNavigationBar != null;
    }

    private void hookInitRefreshRatePolicy() {
        this.mRefreshRatePolicy = new RefreshRatePolicy(this.mService, this.mDisplayContent.getDisplayInfo(), this.mService.mHighRefreshRateDenylist);
    }

    /* loaded from: classes2.dex */
    public static class TranDisplayPolicyProxy {
        private DisplayPolicy mPolicy;

        public TranDisplayPolicyProxy(DisplayPolicy policy) {
            this.mPolicy = policy;
        }

        public TranRefreshRatePolicy getTranRefreshRatePolicy() {
            DisplayPolicy displayPolicy = this.mPolicy;
            if (displayPolicy == null || displayPolicy.getRefreshRatePolicy() == null) {
                return null;
            }
            return new RefreshRatePolicy.TranRefreshRatePolicyProxyImpl(this.mPolicy.getRefreshRatePolicy()).getTranRefreshRatePolicy();
        }

        public TranRefreshRatePolicy120 getTranRefreshRatePolicy120() {
            DisplayPolicy displayPolicy = this.mPolicy;
            if (displayPolicy == null || displayPolicy.getRefreshRatePolicy() == null) {
                return null;
            }
            return new RefreshRatePolicy.TranRefreshRatePolicyProxyImpl(this.mPolicy.getRefreshRatePolicy()).getTranRefreshRatePolicy120();
        }

        public RefreshRatePolicy.TranRefreshRatePolicyProxyImpl getTranRefreshRatePolicyProxy() {
            DisplayPolicy displayPolicy = this.mPolicy;
            if (displayPolicy == null || displayPolicy.getRefreshRatePolicy() == null) {
                return null;
            }
            return new RefreshRatePolicy.TranRefreshRatePolicyProxyImpl(this.mPolicy.getRefreshRatePolicy());
        }
    }

    public void setPreWakeupInProgress(boolean newValue) {
        this.mPreWakeupInProgress = newValue;
    }

    public boolean isPrewakeupInProgress() {
        return this.mPreWakeupInProgress;
    }

    public void notifyKeyguardReadyToGo(boolean isReadyToGo, long duration) {
        this.mIsKeyguardReadyToGo = isReadyToGo;
        ITranDisplayPolicy.Instance().notifyKeyguardReadyToGo(this.mResetNotifactionShadeVisible, isReadyToGo, duration, "WindowManager");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hideKeyguardWindow() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ITranDisplayPolicy.Instance().hideKeyguardWindow(this.mNotificationShade, this.mService.mPolicy, this.mResetNotifactionShadeVisible, "WindowManager");
                this.mAodHideByFingerprint = true;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onKeyguardChange() {
        this.mService.mWindowPlacerLocked.requestTraversal();
    }

    public void setFingerprintStartTime(long startTime) {
        this.mFingerprintStartTime = startTime;
    }

    private void updateFingerprintUnlockCount() {
        if (Build.TRANCARE_SUPPORT && this.mFingerprintStartTime > 0) {
            long fingerprintUnlockTime = SystemClock.elapsedRealtime() - this.mFingerprintStartTime;
            Slog.d("WindowManager", " updateFingerprintUnlockCount fingerprintUnlockTime = " + fingerprintUnlockTime);
            ITranFingerprintService.Instance().updateScreenOnUnlockCount(fingerprintUnlockTime);
            this.mFingerprintStartTime = 0L;
        }
    }

    private void hookUpdateFingerprintUnlockCount(WindowState win, boolean shouldHideByFingerprintPolicy) {
        if (this.mNotificationShade == win) {
            if (shouldHideByFingerprintPolicy) {
                win.hide(false, true);
                updateFingerprintUnlockCount();
                return;
            }
            win.show(false, true);
        }
    }
}
