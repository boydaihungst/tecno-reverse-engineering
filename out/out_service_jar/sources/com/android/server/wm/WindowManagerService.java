package com.android.server.wm;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IAssistDataReceiver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.TestUtilityService;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.configstore.V1_0.OptionalBool;
import android.hardware.configstore.V1_1.ISurfaceFlingerConfigs;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.InputConstants;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemService;
import android.os.Trace;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.DeviceConfigInterface;
import android.provider.Settings;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.sysprop.SurfaceFlingerProperties;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.MergedConfiguration;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import android.util.TypedValue;
import android.util.proto.ProtoOutputStream;
import android.view.Choreographer;
import android.view.ContentRecordingSession;
import android.view.DisplayInfo;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.ICrossWindowBlurEnabledListener;
import android.view.IDisplayFoldListener;
import android.view.IDisplayWindowInsetsController;
import android.view.IDisplayWindowListener;
import android.view.IDisplayWindowRotationController;
import android.view.IInputFilter;
import android.view.IOnKeyguardExitResult;
import android.view.IPinnedTaskListener;
import android.view.IRecentsAnimationRunner;
import android.view.IRotationWatcher;
import android.view.IScrollCaptureResponseListener;
import android.view.ISystemGestureExclusionListener;
import android.view.IWallpaperVisibilityListener;
import android.view.IWindow;
import android.view.IWindowId;
import android.view.IWindowManager;
import android.view.IWindowSession;
import android.view.IWindowSessionCallback;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputWindowHandle;
import android.view.InsetsSourceControl;
import android.view.InsetsState;
import android.view.InsetsVisibilities;
import android.view.MagnificationSpec;
import android.view.MotionEvent;
import android.view.RemoteAnimationAdapter;
import android.view.ScrollCaptureResponse;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceSession;
import android.view.TaskTransitionSpec;
import android.view.WindowContentFrameStats;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.view.displayhash.DisplayHash;
import android.view.displayhash.VerifiedDisplayHash;
import android.window.ClientWindowFrames;
import android.window.ITaskFpsCallback;
import android.window.TaskSnapshot;
import com.android.internal.R;
import com.android.internal.os.IResultReceiver;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IKeyguardLockedStateListener;
import com.android.internal.policy.IShortcutService;
import com.android.internal.policy.KeyInterceptionInfo;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.LatencyTracker;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.view.WindowManagerPolicyThread;
import com.android.server.AnimationThread;
import com.android.server.DisplayThread;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.NVUtils;
import com.android.server.UiThread;
import com.android.server.Watchdog;
import com.android.server.input.InputManagerService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.notification.NotificationShellCmd;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.ShutdownThread;
import com.android.server.usage.AppStandbyController;
import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.android.server.utils.PriorityDump;
import com.android.server.wm.AccessibilityController;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.DisplayAreaPolicy;
import com.android.server.wm.EmbeddedWindowController;
import com.android.server.wm.RecentsAnimationController;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowState;
import com.android.server.wm.WindowToken;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.powerhal.PowerHalManager;
import com.mediatek.server.wm.WindowManagerDebugger;
import com.mediatek.server.wm.WmsExt;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import dalvik.annotation.optimization.NeverCompile;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public class WindowManagerService extends IWindowManager.Stub implements Watchdog.Monitor, WindowManagerPolicy.WindowManagerFuncs {
    private static final boolean ALWAYS_KEEP_CURRENT = true;
    private static final int ANIMATION_COMPLETED_TIMEOUT_MS = 5000;
    private static final int ANIMATION_DURATION_SCALE = 2;
    private static final int BOOT_ANIMATION_POLL_INTERVAL = 50;
    private static final String BOOT_ANIMATION_SERVICE = "bootanim";
    public static final String DEBUG_HIGH_REFRESH_BALCK_LIST = "debug.wms.high_refresh_rate_blacklist";
    public static final String DEBUG_MSYNC_LOG = "debug.wms.msync.log";
    private static final String DENSITY_OVERRIDE = "ro.config.density_override";
    static final boolean ENABLE_FIXED_ROTATION_TRANSFORM;
    private static final String ENABLE_REMOTE_KEYGUARD_ANIMATION_PROPERTY = "persist.wm.enable_remote_keyguard_animation";
    private static final int INPUT_DEVICES_READY_FOR_SAFE_MODE_DETECTION_TIMEOUT_MILLIS = 1000;
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    static final int LAYOUT_REPEAT_THRESHOLD = 4;
    static final int LOGTAG_INPUT_FOCUS = 62001;
    static final int MAX_ANIMATION_DURATION = 10000;
    static final boolean PROFILE_ORIENTATION = false;
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    private static final String SIZE_OVERRIDE = "ro.config.size_override";
    private static final String SYSTEM_DEBUGGABLE = "ro.debuggable";
    private static final String SYSTEM_SECURE = "ro.secure";
    private static final String TAG = "WindowManager";
    private static final int TRANSITION_ANIMATION_SCALE = 1;
    static final int UPDATE_FOCUS_NORMAL = 0;
    static final int UPDATE_FOCUS_PLACING_SURFACES = 2;
    static final int UPDATE_FOCUS_REMOVING_FOCUS = 4;
    static final int UPDATE_FOCUS_WILL_ASSIGN_LAYERS = 1;
    static final int UPDATE_FOCUS_WILL_PLACE_SURFACES = 3;
    static final int WINDOWS_FREEZING_SCREENS_ACTIVE = 1;
    static final int WINDOWS_FREEZING_SCREENS_NONE = 0;
    static final int WINDOWS_FREEZING_SCREENS_TIMEOUT = 2;
    private static final int WINDOW_ANIMATION_SCALE = 0;
    static final int WINDOW_FREEZE_TIMEOUT_DURATION = 2000;
    static final int WINDOW_REPLACEMENT_TIMEOUT_DURATION = 2000;
    private static final boolean mIsPnPGameSupport;
    private static final boolean mIsPnPMgrSupport;
    static final boolean mTranScreenRotation;
    private static final int sEnableRemoteKeyguardAnimation;
    public static final boolean sEnableRemoteKeyguardGoingAwayAnimation;
    public static final boolean sEnableRemoteKeyguardOccludeAnimation;
    static WindowManagerThreadPriorityBooster sThreadPriorityBooster;
    public boolean isHighRefreshBlackListOn;
    public boolean isMsyncLogOn;
    public boolean isMsyncOn;
    final AccessibilityController mAccessibilityController;
    final IActivityManager mActivityManager;
    final WindowManagerInternal.AppTransitionListener mActivityManagerAppTransitionNotifier;
    final boolean mAllowAnimationsInLowPowerMode;
    final boolean mAllowBootMessages;
    boolean mAllowTheaterModeWakeFromLayout;
    final ActivityManagerInternal mAmInternal;
    final Handler mAnimationHandler;
    final ArrayMap<AnimationAdapter, SurfaceAnimator> mAnimationTransferMap;
    private boolean mAnimationsDisabled;
    final WindowAnimator mAnimator;
    private float mAnimatorDurationScaleSetting;
    final AnrController mAnrController;
    final ArrayList<AppFreezeListener> mAppFreezeListeners;
    final AppOpsManager mAppOps;
    int mAppsFreezingScreen;
    final boolean mAssistantOnTopOfDream;
    final ActivityTaskManagerService mAtmService;
    final BlurController mBlurController;
    boolean mBootAnimationStopped;
    long mBootWaitForWindowsStartTime;
    private final BroadcastReceiver mBroadcastReceiver;
    boolean mClientFreezingScreen;
    final WindowManagerConstants mConstants;
    final ContentRecordingController mContentRecordingController;
    final Context mContext;
    int[] mCurrentProfileIds;
    int mCurrentUserId;
    final ArrayList<WindowState> mDestroySurface;
    boolean mDisableTransitionAnimation;
    private final DisplayAreaPolicy.Provider mDisplayAreaPolicyProvider;
    boolean mDisplayEnabled;
    long mDisplayFreezeTime;
    boolean mDisplayFrozen;
    private final DisplayHashController mDisplayHashController;
    volatile Map<Integer, Integer> mDisplayImePolicyCache;
    final DisplayManager mDisplayManager;
    final DisplayManagerInternal mDisplayManagerInternal;
    final DisplayWindowListenerController mDisplayNotificationController;
    boolean mDisplayReady;
    IDisplayWindowRotationController mDisplayRotationController;
    private final IBinder.DeathRecipient mDisplayRotationControllerDeath;
    final DisplayWindowSettings mDisplayWindowSettings;
    final DisplayWindowSettingsProvider mDisplayWindowSettingsProvider;
    final DragDropController mDragDropController;
    final long mDrawLockTimeoutMillis;
    final EmbeddedWindowController mEmbeddedWindowController;
    EmulatorDisplayOverlay mEmulatorDisplayOverlay;
    private int mEnterAnimId;
    private boolean mEventDispatchingEnabled;
    private int mExitAnimId;
    boolean mFocusMayChange;
    private InputTarget mFocusedInputTarget;
    boolean mForceDesktopModeOnExternalDisplays;
    boolean mForceDisplayEnabled;
    final ArrayList<WindowState> mForceRemoves;
    private int mFrozenDisplayId;
    final WindowManagerGlobalLock mGlobalLock;
    final H mH;
    boolean mHardKeyboardAvailable;
    WindowManagerInternal.OnHardKeyboardStatusChangeListener mHardKeyboardStatusChangeListener;
    private boolean mHasHdrSupport;
    final boolean mHasPermanentDpad;
    private boolean mHasWideColorGamutSupport;
    private ArrayList<WindowState> mHidingNonSystemOverlayWindows;
    final HighRefreshRateDenylist mHighRefreshRateDenylist;
    private Session mHoldingScreenOn;
    private PowerManager.WakeLock mHoldingScreenWakeLock;
    private boolean mInTouchMode;
    final InputManagerService mInputManager;
    final InputManagerCallback mInputManagerCallback;
    final HashMap<IBinder, WindowState> mInputToWindowMap;
    boolean mIsFakeTouchDevice;
    private boolean mIsIgnoreOrientationRequestDisabled;
    boolean mIsPc;
    boolean mIsTouchDevice;
    private final KeyguardDisableHandler mKeyguardDisableHandler;
    String mLastANRState;
    int mLastDisplayFreezeDuration;
    Object mLastFinishedFreezeSource;
    WindowState mLastWakeLockHoldingWindow;
    WindowState mLastWakeLockObscuringWindow;
    final LatencyTracker mLatencyTracker;
    final LetterboxConfiguration mLetterboxConfiguration;
    final boolean mLimitedAlphaCompositing;
    final int mMaxUiWidth;
    volatile float mMaximumObscuringOpacityForTouch;
    MousePositionTracker mMousePositionTracker;
    final boolean mOnlyCore;
    boolean mPerDisplayFocusEnabled;
    final PackageManagerInternal mPmInternal;
    boolean mPointerLocationEnabled;
    WindowManagerPolicy mPolicy;
    final PossibleDisplayInfoMapper mPossibleDisplayInfoMapper;
    PowerManager mPowerManager;
    PowerManagerInternal mPowerManagerInternal;
    private final PriorityDump.PriorityDumper mPriorityDumper;
    private RecentsAnimationController mRecentsAnimationController;
    final ArrayList<WindowState> mResizingWindows;
    RootWindowContainer mRoot;
    ArrayList<RotationWatcher> mRotationWatchers;
    boolean mSafeMode;
    private final PowerManager.WakeLock mScreenFrozenLock;
    final ArraySet<Session> mSessions;
    SettingsObserver mSettingsObserver;
    boolean mShowAlertWindowNotifications;
    boolean mShowingBootMessages;
    final StartingSurfaceController mStartingSurfaceController;
    StrictModeFlash mStrictModeFlash;
    SurfaceAnimationRunner mSurfaceAnimationRunner;
    Function<SurfaceSession, SurfaceControl.Builder> mSurfaceControlFactory;
    final Supplier<Surface> mSurfaceFactory;
    boolean mSwitchingUser;
    final BLASTSyncEngine mSyncEngine;
    boolean mSystemBooted;
    boolean mSystemReady;
    final TaskFpsCallbackController mTaskFpsCallbackController;
    final TaskPositioningController mTaskPositioningController;
    final TaskSnapshotController mTaskSnapshotController;
    final TaskSystemBarsListenerController mTaskSystemBarsListenerController;
    TaskTransitionSpec mTaskTransitionSpec;
    private WindowContentFrameStats mTempWindowRenderStats;
    private final TestUtilityService mTestUtilityService;
    final Rect mTmpRect;
    private int mTranMultiDisplayAreaId;
    private final SurfaceControl.Transaction mTransaction;
    Supplier<SurfaceControl.Transaction> mTransactionFactory;
    int mTransactionSequence;
    private float mTransitionAnimationScaleSetting;
    final boolean mUseBLAST;
    final boolean mUseBLASTSync;
    private ViewServer mViewServer;
    final HashMap<WindowContainer, Runnable> mWaitingForDrawnCallbacks;
    final WallpaperVisibilityListeners mWallpaperVisibilityListeners;
    Watermark mWatermark;
    private float mWindowAnimationScaleSetting;
    final ArrayList<WindowChangeListener> mWindowChangeListeners;
    final WindowContextListenerController mWindowContextListenerController;
    final HashMap<IBinder, WindowState> mWindowMap;
    final WindowSurfacePlacer mWindowPlacerLocked;
    final ArrayList<ActivityRecord> mWindowReplacementTimeouts;
    final WindowTracing mWindowTracing;
    boolean mWindowsChanged;
    int mWindowsFreezingScreen;
    int mWindowsInsetsChanged;
    private WmsExt mWmsExt;
    public TranWaterMark tranWaterMark;
    public static final String ENABLE_SHELL_TRANSITIONS = "persist.wm.debug.shell_transit";
    public static final boolean sEnableShellTransitions = SystemProperties.getBoolean(ENABLE_SHELL_TRANSITIONS, false);
    private final RemoteCallbackList<IKeyguardLockedStateListener> mKeyguardLockedStateListeners = new RemoteCallbackList<>();
    private boolean mDispatchedKeyguardLockedState = false;
    int mVr2dDisplayId = -1;
    boolean mVrModeEnabled = false;
    final Map<IBinder, KeyInterceptionInfo> mKeyInterceptionInfoForToken = Collections.synchronizedMap(new ArrayMap());
    private final IVrStateCallbacks mVrStateCallbacks = new IVrStateCallbacks.Stub() { // from class: com.android.server.wm.WindowManagerService.1
        public void onVrStateChanged(boolean enabled) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mVrModeEnabled = enabled;
                    Consumer<DisplayPolicy> obtainConsumer = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.WindowManagerService$1$$ExternalSyntheticLambda0
                        @Override // java.util.function.BiConsumer
                        public final void accept(Object obj, Object obj2) {
                            ((DisplayPolicy) obj).onVrStateChangedLw(((Boolean) obj2).booleanValue());
                        }
                    }, PooledLambda.__(), Boolean.valueOf(enabled));
                    WindowManagerService.this.mRoot.forAllDisplayPolicies(obtainConsumer);
                    obtainConsumer.recycle();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    };
    public WindowManagerDebugger mWindowManagerDebugger = MtkSystemServiceFactory.getInstance().makeWindowManagerDebugger();
    public PowerHalManager mPowerHalManager = MtkSystemServiceFactory.getInstance().makePowerHalManager();

    /* loaded from: classes2.dex */
    interface AppFreezeListener {
        void onAppFreezeTimeout();
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    private @interface UpdateAnimationScaleMode {
    }

    /* loaded from: classes2.dex */
    public interface WindowChangeListener {
        void focusChanged();

        void windowsChanged();
    }

    private static native void nativeActivitySetProcessGroup(int i, int i2, int i3, String str);

    private static native void nativePnPMgrModeCtl(String str);

    private static native void nativePnPMgrUpdateAppUsage(String str);

    private static native void nativeUpdateActivity(String str);

    static {
        boolean z = false;
        int i = SystemProperties.getInt(ENABLE_REMOTE_KEYGUARD_ANIMATION_PROPERTY, 2);
        sEnableRemoteKeyguardAnimation = i;
        sEnableRemoteKeyguardGoingAwayAnimation = i >= 1;
        sEnableRemoteKeyguardOccludeAnimation = i >= 2;
        ENABLE_FIXED_ROTATION_TRANSFORM = SystemProperties.getBoolean("persist.wm.fixed_rotation_transform", true);
        mIsPnPMgrSupport = SystemProperties.get("ro.vendor.pnpmgr.support").equals("1");
        mIsPnPGameSupport = SystemProperties.get("ro.vendor.pnpmgr.game.ctl").equals("enable");
        if (1 == SystemProperties.getInt("ro.tran.screenrotation.support", 0) && SystemProperties.getBoolean("persist.sys.traneffect.enable", true)) {
            z = true;
        }
        mTranScreenRotation = z;
        sThreadPriorityBooster = new WindowManagerThreadPriorityBooster();
    }

    /* loaded from: classes2.dex */
    class RotationWatcher {
        final IBinder.DeathRecipient mDeathRecipient;
        final int mDisplayId;
        final IRotationWatcher mWatcher;

        RotationWatcher(IRotationWatcher watcher, IBinder.DeathRecipient deathRecipient, int displayId) {
            this.mWatcher = watcher;
            this.mDeathRecipient = deathRecipient;
            this.mDisplayId = displayId;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-wm-WindowManagerService  reason: not valid java name */
    public /* synthetic */ void m8508lambda$new$0$comandroidserverwmWindowManagerService() {
        this.mDisplayRotationController = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class SettingsObserver extends ContentObserver {
        private final Uri mAnimationDurationScaleUri;
        private final Uri mDevEnableNonResizableMultiWindowUri;
        private final Uri mDisplayInversionEnabledUri;
        private final Uri mDisplaySettingsPathUri;
        private final Uri mForceDesktopModeOnExternalDisplaysUri;
        private final Uri mForceResizableUri;
        private final Uri mFreeformWindowUri;
        private final Uri mImmersiveModeConfirmationsUri;
        private final Uri mMaximumObscuringOpacityForTouchUri;
        private final Uri mPointerLocationUri;
        private final Uri mPolicyControlUri;
        private final Uri mRenderShadowsInCompositorUri;
        private final Uri mTransitionAnimationScaleUri;
        private final Uri mWindowAnimationScaleUri;

        public SettingsObserver() {
            super(new Handler());
            Uri uriFor = Settings.Secure.getUriFor("accessibility_display_inversion_enabled");
            this.mDisplayInversionEnabledUri = uriFor;
            Uri uriFor2 = Settings.Global.getUriFor("window_animation_scale");
            this.mWindowAnimationScaleUri = uriFor2;
            Uri uriFor3 = Settings.Global.getUriFor("transition_animation_scale");
            this.mTransitionAnimationScaleUri = uriFor3;
            Uri uriFor4 = Settings.Global.getUriFor("animator_duration_scale");
            this.mAnimationDurationScaleUri = uriFor4;
            Uri uriFor5 = Settings.Secure.getUriFor("immersive_mode_confirmations");
            this.mImmersiveModeConfirmationsUri = uriFor5;
            Uri uriFor6 = Settings.Global.getUriFor("policy_control");
            this.mPolicyControlUri = uriFor6;
            Uri uriFor7 = Settings.System.getUriFor("pointer_location");
            this.mPointerLocationUri = uriFor7;
            Uri uriFor8 = Settings.Global.getUriFor("force_desktop_mode_on_external_displays");
            this.mForceDesktopModeOnExternalDisplaysUri = uriFor8;
            Uri uriFor9 = Settings.Global.getUriFor("enable_freeform_support");
            this.mFreeformWindowUri = uriFor9;
            Uri uriFor10 = Settings.Global.getUriFor("force_resizable_activities");
            this.mForceResizableUri = uriFor10;
            Uri uriFor11 = Settings.Global.getUriFor("enable_non_resizable_multi_window");
            this.mDevEnableNonResizableMultiWindowUri = uriFor11;
            this.mRenderShadowsInCompositorUri = Settings.Global.getUriFor("render_shadows_in_compositor");
            Uri uriFor12 = Settings.Global.getUriFor("wm_display_settings_path");
            this.mDisplaySettingsPathUri = uriFor12;
            Uri uriFor13 = Settings.Global.getUriFor("maximum_obscuring_opacity_for_touch");
            this.mMaximumObscuringOpacityForTouchUri = uriFor13;
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(uriFor, false, this, -1);
            resolver.registerContentObserver(uriFor2, false, this, -1);
            resolver.registerContentObserver(uriFor3, false, this, -1);
            resolver.registerContentObserver(uriFor4, false, this, -1);
            resolver.registerContentObserver(uriFor5, false, this, -1);
            resolver.registerContentObserver(uriFor6, false, this, -1);
            resolver.registerContentObserver(uriFor7, false, this, -1);
            resolver.registerContentObserver(uriFor8, false, this, -1);
            resolver.registerContentObserver(uriFor9, false, this, -1);
            resolver.registerContentObserver(uriFor10, false, this, -1);
            resolver.registerContentObserver(uriFor11, false, this, -1);
            resolver.registerContentObserver(uriFor12, false, this, -1);
            resolver.registerContentObserver(uriFor13, false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            int mode;
            if (uri == null) {
                return;
            }
            if (this.mImmersiveModeConfirmationsUri.equals(uri) || this.mPolicyControlUri.equals(uri)) {
                updateSystemUiSettings(true);
            } else if (this.mPointerLocationUri.equals(uri)) {
                updatePointerLocation();
            } else if (this.mForceDesktopModeOnExternalDisplaysUri.equals(uri)) {
                updateForceDesktopModeOnExternalDisplays();
            } else if (this.mFreeformWindowUri.equals(uri)) {
                updateFreeformWindowManagement();
            } else if (this.mForceResizableUri.equals(uri)) {
                updateForceResizableTasks();
            } else if (this.mDevEnableNonResizableMultiWindowUri.equals(uri)) {
                updateDevEnableNonResizableMultiWindow();
            } else if (this.mDisplaySettingsPathUri.equals(uri)) {
                updateDisplaySettingsLocation();
            } else if (this.mMaximumObscuringOpacityForTouchUri.equals(uri)) {
                updateMaximumObscuringOpacityForTouch();
            } else {
                if (this.mWindowAnimationScaleUri.equals(uri)) {
                    mode = 0;
                } else if (this.mTransitionAnimationScaleUri.equals(uri)) {
                    mode = 1;
                } else if (this.mAnimationDurationScaleUri.equals(uri)) {
                    mode = 2;
                } else {
                    return;
                }
                Message m = WindowManagerService.this.mH.obtainMessage(51, mode, 0);
                WindowManagerService.this.mH.sendMessage(m);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void loadSettings() {
            updateSystemUiSettings(false);
            updatePointerLocation();
            updateMaximumObscuringOpacityForTouch();
        }

        void updateMaximumObscuringOpacityForTouch() {
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            WindowManagerService.this.mMaximumObscuringOpacityForTouch = Settings.Global.getFloat(resolver, "maximum_obscuring_opacity_for_touch", 0.8f);
        }

        void updateSystemUiSettings(boolean handleChange) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    boolean changed = false;
                    if (handleChange) {
                        changed = WindowManagerService.this.getDefaultDisplayContentLocked().getDisplayPolicy().onSystemUiSettingsChanged();
                    } else {
                        ImmersiveModeConfirmation.loadSetting(WindowManagerService.this.mCurrentUserId, WindowManagerService.this.mContext);
                    }
                    if (changed) {
                        WindowManagerService.this.mWindowPlacerLocked.requestTraversal();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        void updatePointerLocation() {
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            boolean enablePointerLocation = Settings.System.getIntForUser(resolver, "pointer_location", 0, -2) != 0;
            if (WindowManagerService.this.mPointerLocationEnabled == enablePointerLocation) {
                return;
            }
            WindowManagerService.this.mPointerLocationEnabled = enablePointerLocation;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    Consumer<DisplayPolicy> obtainConsumer = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.WindowManagerService$SettingsObserver$$ExternalSyntheticLambda0
                        @Override // java.util.function.BiConsumer
                        public final void accept(Object obj, Object obj2) {
                            ((DisplayPolicy) obj).setPointerLocationEnabled(((Boolean) obj2).booleanValue());
                        }
                    }, PooledLambda.__(), Boolean.valueOf(WindowManagerService.this.mPointerLocationEnabled));
                    WindowManagerService.this.mRoot.forAllDisplayPolicies(obtainConsumer);
                    obtainConsumer.recycle();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        void updateForceDesktopModeOnExternalDisplays() {
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            boolean enableForceDesktopMode = Settings.Global.getInt(resolver, "force_desktop_mode_on_external_displays", 0) != 0;
            if (WindowManagerService.this.mForceDesktopModeOnExternalDisplays == enableForceDesktopMode) {
                return;
            }
            WindowManagerService.this.setForceDesktopModeOnExternalDisplays(enableForceDesktopMode);
        }

        void updateFreeformWindowManagement() {
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            boolean z = false;
            boolean freeformWindowManagement = (WindowManagerService.this.mContext.getPackageManager().hasSystemFeature("android.software.freeform_window_management") || Settings.Global.getInt(resolver, "enable_freeform_support", 0) != 0) ? true : true;
            if (!freeformWindowManagement && "1".equals(SystemProperties.get("ro.os_freeform_support"))) {
                Slog.d("WindowManager", "can't disable freeform mode cause ro.os_freeform_support");
            } else if (WindowManagerService.this.mAtmService.mSupportsFreeformWindowManagement != freeformWindowManagement) {
                WindowManagerService.this.mAtmService.mSupportsFreeformWindowManagement = freeformWindowManagement;
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        WindowManagerService.this.mRoot.onSettingsRetrieved();
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }

        void updateForceResizableTasks() {
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            boolean forceResizable = Settings.Global.getInt(resolver, "force_resizable_activities", 0) != 0;
            WindowManagerService.this.mAtmService.mForceResizableActivities = forceResizable;
        }

        void updateDevEnableNonResizableMultiWindow() {
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            boolean devEnableNonResizableMultiWindow = Settings.Global.getInt(resolver, "enable_non_resizable_multi_window", 0) != 0;
            WindowManagerService.this.mAtmService.mDevEnableNonResizableMultiWindow = devEnableNonResizableMultiWindow;
        }

        void updateDisplaySettingsLocation() {
            ContentResolver resolver = WindowManagerService.this.mContext.getContentResolver();
            String filePath = Settings.Global.getString(resolver, "wm_display_settings_path");
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mDisplayWindowSettingsProvider.setBaseSettingsFilePath(filePath);
                    WindowManagerService.this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerService$SettingsObserver$$ExternalSyntheticLambda1
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            WindowManagerService.SettingsObserver.this.m8513xffa96e6a((DisplayContent) obj);
                        }
                    });
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateDisplaySettingsLocation$0$com-android-server-wm-WindowManagerService$SettingsObserver  reason: not valid java name */
        public /* synthetic */ void m8513xffa96e6a(DisplayContent display) {
            WindowManagerService.this.mDisplayWindowSettings.applySettingsToDisplayLocked(display);
            display.reconfigureDisplayLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void boostPriorityForLockedSection() {
        sThreadPriorityBooster.boost();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void resetPriorityAfterLockedSection() {
        sThreadPriorityBooster.reset();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void openSurfaceTransaction() {
        try {
            Trace.traceBegin(32L, "openSurfaceTransaction");
            SurfaceControl.openTransaction();
        } finally {
            Trace.traceEnd(32L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void closeSurfaceTransaction(String where) {
        try {
            Trace.traceBegin(32L, "closeSurfaceTransaction");
            SurfaceControl.closeTransaction();
            this.mWindowTracing.logState(where);
        } finally {
            Trace.traceEnd(32L);
        }
    }

    public static WindowManagerService main(Context context, InputManagerService im, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy, ActivityTaskManagerService atm) {
        return main(context, im, showBootMsgs, onlyCore, policy, atm, new DisplayWindowSettingsProvider(), new Supplier() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda2
            @Override // java.util.function.Supplier
            public final Object get() {
                return new SurfaceControl.Transaction();
            }
        }, new Supplier() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda3
            @Override // java.util.function.Supplier
            public final Object get() {
                return new Surface();
            }
        }, new Function() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda4
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return new SurfaceControl.Builder((SurfaceSession) obj);
            }
        });
    }

    public static WindowManagerService main(final Context context, final InputManagerService im, final boolean showBootMsgs, final boolean onlyCore, final WindowManagerPolicy policy, final ActivityTaskManagerService atm, final DisplayWindowSettingsProvider displayWindowSettingsProvider, final Supplier<SurfaceControl.Transaction> transactionFactory, final Supplier<Surface> surfaceFactory, final Function<SurfaceSession, SurfaceControl.Builder> surfaceControlFactory) {
        final WindowManagerService[] wms = new WindowManagerService[1];
        DisplayThread.getHandler().runWithScissors(new Runnable() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                WindowManagerService.lambda$main$1(wms, context, im, showBootMsgs, onlyCore, policy, atm, displayWindowSettingsProvider, transactionFactory, surfaceFactory, surfaceControlFactory);
            }
        }, 0L);
        return wms[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$main$1(WindowManagerService[] wms, Context context, InputManagerService im, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy, ActivityTaskManagerService atm, DisplayWindowSettingsProvider displayWindowSettingsProvider, Supplier transactionFactory, Supplier surfaceFactory, Function surfaceControlFactory) {
        wms[0] = new WindowManagerService(context, im, showBootMsgs, onlyCore, policy, atm, displayWindowSettingsProvider, transactionFactory, surfaceFactory, surfaceControlFactory);
    }

    private void initPolicy() {
        UiThread.getHandler().runWithScissors(new Runnable() { // from class: com.android.server.wm.WindowManagerService.5
            @Override // java.lang.Runnable
            public void run() {
                WindowManagerPolicyThread.set(Thread.currentThread(), Looper.myLooper());
                WindowManagerPolicy windowManagerPolicy = WindowManagerService.this.mPolicy;
                Context context = WindowManagerService.this.mContext;
                WindowManagerService windowManagerService = WindowManagerService.this;
                windowManagerPolicy.init(context, windowManagerService, windowManagerService);
            }
        }, 0L);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.wm.WindowManagerService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver result) {
        new WindowManagerShellCommand(this).exec(this, in, out, err, args, callback, result);
    }

    private WindowManagerService(Context context, InputManagerService inputManager, boolean showBootMsgs, boolean onlyCore, WindowManagerPolicy policy, ActivityTaskManagerService atm, DisplayWindowSettingsProvider displayWindowSettingsProvider, Supplier<SurfaceControl.Transaction> transactionFactory, Supplier<Surface> surfaceFactory, Function<SurfaceSession, SurfaceControl.Builder> surfaceControlFactory) {
        boolean z;
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.wm.WindowManagerService.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                char c;
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case 988075300:
                        if (action.equals("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED")) {
                            c = 0;
                            break;
                        }
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        WindowManagerService.this.mKeyguardDisableHandler.updateKeyguardEnabled(getSendingUserId());
                        return;
                    default:
                        return;
                }
            }
        };
        this.mBroadcastReceiver = broadcastReceiver;
        this.mPriorityDumper = new PriorityDump.PriorityDumper() { // from class: com.android.server.wm.WindowManagerService.3
            @Override // com.android.server.utils.PriorityDump.PriorityDumper
            public void dumpCritical(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
                WindowManagerService.this.doDump(fd, pw, new String[]{"-a"}, asProto);
            }

            @Override // com.android.server.utils.PriorityDump.PriorityDumper
            public void dump(FileDescriptor fd, PrintWriter pw, String[] args, boolean asProto) {
                WindowManagerService.this.doDump(fd, pw, args, asProto);
            }
        };
        this.mCurrentProfileIds = new int[0];
        this.mShowAlertWindowNotifications = true;
        this.mSessions = new ArraySet<>();
        this.mWindowMap = new HashMap<>();
        this.mInputToWindowMap = new HashMap<>();
        this.mWindowReplacementTimeouts = new ArrayList<>();
        this.mResizingWindows = new ArrayList<>();
        this.mDisplayImePolicyCache = Collections.unmodifiableMap(new ArrayMap());
        this.mDestroySurface = new ArrayList<>();
        this.mForceRemoves = new ArrayList<>();
        this.mWaitingForDrawnCallbacks = new HashMap<>();
        this.mHidingNonSystemOverlayWindows = new ArrayList<>();
        this.mTmpRect = new Rect();
        this.mDisplayEnabled = false;
        this.mSystemBooted = false;
        this.mForceDisplayEnabled = false;
        this.mShowingBootMessages = false;
        this.mSystemReady = false;
        this.mBootAnimationStopped = false;
        this.mBootWaitForWindowsStartTime = -1L;
        this.mLastWakeLockHoldingWindow = null;
        this.mLastWakeLockObscuringWindow = null;
        this.mUseBLASTSync = true;
        this.mRotationWatchers = new ArrayList<>();
        this.mWallpaperVisibilityListeners = new WallpaperVisibilityListeners();
        this.mDisplayRotationController = null;
        this.mDisplayRotationControllerDeath = new IBinder.DeathRecipient() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda27
            @Override // android.os.IBinder.DeathRecipient
            public final void binderDied() {
                WindowManagerService.this.m8508lambda$new$0$comandroidserverwmWindowManagerService();
            }
        };
        this.mDisplayFrozen = false;
        this.mDisplayFreezeTime = 0L;
        this.mLastDisplayFreezeDuration = 0;
        this.mLastFinishedFreezeSource = null;
        this.mSwitchingUser = false;
        this.mWindowsFreezingScreen = 0;
        this.mClientFreezingScreen = false;
        this.mAppsFreezingScreen = 0;
        this.mWindowsInsetsChanged = 0;
        H h = new H();
        this.mH = h;
        this.mAnimationHandler = new Handler(AnimationThread.getHandler().getLooper());
        this.mMaximumObscuringOpacityForTouch = 0.8f;
        this.mWindowContextListenerController = new WindowContextListenerController();
        this.mContentRecordingController = new ContentRecordingController();
        this.mWindowAnimationScaleSetting = 1.0f;
        this.mTransitionAnimationScaleSetting = 1.0f;
        this.mAnimatorDurationScaleSetting = 1.0f;
        this.mAnimationsDisabled = false;
        this.mPointerLocationEnabled = false;
        this.mAnimationTransferMap = new ArrayMap<>();
        this.mWindowChangeListeners = new ArrayList<>();
        this.mWindowsChanged = false;
        this.mActivityManagerAppTransitionNotifier = new WindowManagerInternal.AppTransitionListener() { // from class: com.android.server.wm.WindowManagerService.4
            @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
            public void onAppTransitionCancelledLocked(boolean keyguardGoingAway) {
            }

            @Override // com.android.server.wm.WindowManagerInternal.AppTransitionListener
            public void onAppTransitionFinishedLocked(IBinder token) {
                ActivityRecord atoken = WindowManagerService.this.mRoot.getActivityRecord(token);
                if (atoken == null) {
                    return;
                }
                if (atoken.mLaunchTaskBehind && !WindowManagerService.this.isRecentsAnimationTarget(atoken)) {
                    WindowManagerService.this.mAtmService.mTaskSupervisor.scheduleLaunchTaskBehindComplete(atoken.token);
                    atoken.mLaunchTaskBehind = false;
                    return;
                }
                atoken.updateReportedVisibilityLocked();
                if (atoken.mEnteringAnimation && !WindowManagerService.this.isRecentsAnimationTarget(atoken)) {
                    atoken.mEnteringAnimation = false;
                    if (atoken.attachedToProcess()) {
                        try {
                            atoken.app.getThread().scheduleEnterAnimationComplete(atoken.token);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
        };
        this.mAppFreezeListeners = new ArrayList<>();
        this.mInputManagerCallback = new InputManagerCallback(this);
        this.isHighRefreshBlackListOn = true;
        if (SystemProperties.getInt("vendor.msync3.enable", 0) != 1) {
            z = false;
        } else {
            z = true;
        }
        this.isMsyncOn = z;
        this.isMsyncLogOn = false;
        this.mMousePositionTracker = new MousePositionTracker();
        this.mWmsExt = MtkSystemServiceFactory.getInstance().makeWmsExt();
        this.mTranMultiDisplayAreaId = 10;
        LockGuard.installLock(this, 5);
        this.mGlobalLock = atm.getGlobalLock();
        this.mAtmService = atm;
        this.mContext = context;
        this.mIsPc = context.getPackageManager().hasSystemFeature("android.hardware.type.pc");
        this.mAllowBootMessages = showBootMsgs;
        this.mOnlyCore = onlyCore;
        this.mLimitedAlphaCompositing = context.getResources().getBoolean(17891744);
        this.mHasPermanentDpad = context.getResources().getBoolean(17891677);
        boolean z2 = context.getResources().getBoolean(17891586);
        this.mInTouchMode = z2;
        inputManager.setInTouchMode(z2, Process.myPid(), Process.myUid(), true);
        this.mDrawLockTimeoutMillis = context.getResources().getInteger(17694823);
        this.mAllowAnimationsInLowPowerMode = context.getResources().getBoolean(17891346);
        this.mMaxUiWidth = context.getResources().getInteger(17694873);
        this.mDisableTransitionAnimation = context.getResources().getBoolean(17891598);
        this.mPerDisplayFocusEnabled = context.getResources().getBoolean(17891332);
        this.mAssistantOnTopOfDream = context.getResources().getBoolean(17891333);
        this.mLetterboxConfiguration = new LetterboxConfiguration(ActivityThread.currentActivityThread().getSystemUiContext());
        this.mInputManager = inputManager;
        DisplayManagerInternal displayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mDisplayManagerInternal = displayManagerInternal;
        this.mPossibleDisplayInfoMapper = new PossibleDisplayInfoMapper(displayManagerInternal);
        this.mSurfaceControlFactory = surfaceControlFactory;
        this.mTransactionFactory = transactionFactory;
        this.mSurfaceFactory = surfaceFactory;
        this.mTransaction = transactionFactory.get();
        this.mPolicy = policy;
        this.mAnimator = new WindowAnimator(this);
        this.mRoot = new RootWindowContainer(this);
        ContentResolver resolver = context.getContentResolver();
        this.mUseBLAST = Settings.Global.getInt(resolver, "use_blast_adapter_vr", 1) == 1;
        this.mSyncEngine = new BLASTSyncEngine(this);
        this.mWindowPlacerLocked = new WindowSurfacePlacer(this);
        this.mTaskSnapshotController = new TaskSnapshotController(this);
        this.mWindowTracing = WindowTracing.createDefaultAndStartLooper(this, Choreographer.getInstance());
        LocalServices.addService(WindowManagerPolicy.class, this.mPolicy);
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mKeyguardDisableHandler = KeyguardDisableHandler.create(context, this.mPolicy, h);
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        PowerManagerInternal powerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mPowerManagerInternal = powerManagerInternal;
        if (powerManagerInternal != null) {
            powerManagerInternal.registerLowPowerModeObserver(new PowerManagerInternal.LowPowerModeListener() { // from class: com.android.server.wm.WindowManagerService.6
                public int getServiceType() {
                    return 3;
                }

                public void onLowPowerModeChanged(PowerSaveState result) {
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            boolean enabled = result.batterySaverEnabled;
                            if (WindowManagerService.this.mAnimationsDisabled != enabled && !WindowManagerService.this.mAllowAnimationsInLowPowerMode) {
                                WindowManagerService.this.mAnimationsDisabled = enabled;
                                WindowManagerService.this.dispatchNewAnimatorScaleLocked(null);
                            }
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            });
            this.mAnimationsDisabled = this.mPowerManagerInternal.getLowPowerState(3).batterySaverEnabled;
        }
        PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(1, "SCREEN_FROZEN");
        this.mScreenFrozenLock = newWakeLock;
        newWakeLock.setReferenceCounted(false);
        ITranWindowManagerService.Instance().initMultWindowManager(context);
        this.mDisplayNotificationController = new DisplayWindowListenerController(this);
        this.mTaskSystemBarsListenerController = new TaskSystemBarsListenerController();
        this.mActivityManager = ActivityManager.getService();
        this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mAppOps = appOpsManager;
        AppOpsManager.OnOpChangedInternalListener opListener = new AppOpsManager.OnOpChangedInternalListener() { // from class: com.android.server.wm.WindowManagerService.7
            public void onOpChanged(int op, String packageName) {
                WindowManagerService.this.updateAppOpsState();
            }
        };
        appOpsManager.startWatchingMode(24, (String) null, (AppOpsManager.OnOpChangedListener) opListener);
        appOpsManager.startWatchingMode(45, (String) null, (AppOpsManager.OnOpChangedListener) opListener);
        this.mPmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mTestUtilityService = (TestUtilityService) LocalServices.getService(TestUtilityService.class);
        IntentFilter suspendPackagesFilter = new IntentFilter();
        suspendPackagesFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        suspendPackagesFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        context.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.wm.WindowManagerService.8
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String[] affectedPackages = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                boolean suspended = "android.intent.action.PACKAGES_SUSPENDED".equals(intent.getAction());
                WindowManagerService.this.updateHiddenWhileSuspendedState(new ArraySet(Arrays.asList(affectedPackages)), suspended);
            }
        }, UserHandle.ALL, suspendPackagesFilter, null, null);
        this.mWindowAnimationScaleSetting = Settings.Global.getFloat(resolver, "window_animation_scale", this.mWindowAnimationScaleSetting);
        this.mTransitionAnimationScaleSetting = Settings.Global.getFloat(resolver, "transition_animation_scale", context.getResources().getFloat(17105065));
        setAnimatorDurationScale(Settings.Global.getFloat(resolver, "animator_duration_scale", this.mAnimatorDurationScaleSetting));
        this.mForceDesktopModeOnExternalDisplays = Settings.Global.getInt(resolver, "force_desktop_mode_on_external_displays", 0) != 0;
        String displaySettingsPath = Settings.Global.getString(resolver, "wm_display_settings_path");
        this.mDisplayWindowSettingsProvider = displayWindowSettingsProvider;
        if (displaySettingsPath != null) {
            displayWindowSettingsProvider.setBaseSettingsFilePath(displaySettingsPath);
        }
        this.mDisplayWindowSettings = new DisplayWindowSettings(this, displayWindowSettingsProvider);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        context.registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, filter, null, null);
        this.mLatencyTracker = LatencyTracker.getInstance(context);
        this.mSettingsObserver = new SettingsObserver();
        PowerManager.WakeLock newWakeLock2 = this.mPowerManager.newWakeLock(536870922, "WindowManager");
        this.mHoldingScreenWakeLock = newWakeLock2;
        newWakeLock2.setReferenceCounted(false);
        this.mSurfaceAnimationRunner = new SurfaceAnimationRunner(this.mTransactionFactory, this.mPowerManagerInternal);
        this.mAllowTheaterModeWakeFromLayout = context.getResources().getBoolean(17891362);
        this.mTaskPositioningController = new TaskPositioningController(this);
        this.mDragDropController = new DragDropController(this, h.getLooper());
        this.mHighRefreshRateDenylist = HighRefreshRateDenylist.create(context.getResources());
        WindowManagerConstants windowManagerConstants = new WindowManagerConstants(this, DeviceConfigInterface.REAL);
        this.mConstants = windowManagerConstants;
        windowManagerConstants.start(new HandlerExecutor(h));
        LocalServices.addService(WindowManagerInternal.class, new LocalService());
        this.mEmbeddedWindowController = new EmbeddedWindowController(atm);
        this.mDisplayAreaPolicyProvider = DisplayAreaPolicy.Provider.fromResources(context.getResources());
        this.mDisplayHashController = new DisplayHashController(context);
        setGlobalShadowSettings();
        this.mAnrController = new AnrController(this);
        this.mStartingSurfaceController = new StartingSurfaceController(this);
        this.mBlurController = new BlurController(context, this.mPowerManager);
        this.mTaskFpsCallbackController = new TaskFpsCallbackController(context);
        this.mAccessibilityController = new AccessibilityController(this);
        ITranWindowManagerService.Instance().onConstruct(this, new ITranWindowManagerService.ConstructParameters() { // from class: com.android.server.wm.WindowManagerService.9
            @Override // com.transsion.hubcore.server.wm.ITranWindowManagerService.ConstructParameters
            public ActivityTaskManagerService getActivityTaskManagerService() {
                return WindowManagerService.this.mAtmService;
            }

            @Override // com.transsion.hubcore.server.wm.ITranWindowManagerService.ConstructParameters
            public WindowManagerGlobalLock getGlobalLock() {
                return WindowManagerService.this.mGlobalLock;
            }

            @Override // com.transsion.hubcore.server.wm.ITranWindowManagerService.ConstructParameters
            public Context getContext() {
                return WindowManagerService.this.mContext;
            }

            @Override // com.transsion.hubcore.server.wm.ITranWindowManagerService.ConstructParameters
            public Looper getDisplayThreadLooper() {
                return DisplayThread.get().getLooper();
            }

            @Override // com.transsion.hubcore.server.wm.ITranWindowManagerService.ConstructParameters
            public DisplayManagerInternal getDisplayManagerInternal() {
                return WindowManagerService.this.mDisplayManagerInternal;
            }

            @Override // com.transsion.hubcore.server.wm.ITranWindowManagerService.ConstructParameters
            public WindowManagerPolicy getWindowManagerPolicy() {
                return WindowManagerService.this.mPolicy;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayAreaPolicy.Provider getDisplayAreaPolicyProvider() {
        return this.mDisplayAreaPolicyProvider;
    }

    private void setGlobalShadowSettings() {
        TypedArray a = this.mContext.obtainStyledAttributes(null, R.styleable.Lighting, 0, 0);
        float lightY = a.getDimension(3, 0.0f);
        float lightZ = a.getDimension(4, 0.0f);
        float lightRadius = a.getDimension(2, 0.0f);
        float ambientShadowAlpha = a.getFloat(0, 0.0f);
        float spotShadowAlpha = a.getFloat(1, 0.0f);
        a.recycle();
        float[] ambientColor = {0.0f, 0.0f, 0.0f, ambientShadowAlpha};
        float[] spotColor = {0.0f, 0.0f, 0.0f, spotShadowAlpha};
        SurfaceControl.setGlobalShadowSettings(ambientColor, spotColor, lightY, lightZ, lightRadius);
    }

    public void onInitReady() {
        initPolicy();
        Watchdog.getInstance().addMonitor(this);
        createWatermark();
        new NVUtils();
        try {
            int key = NVUtils.getDemoPhoneNV();
            Slog.i("WindowManager", "getDemoPhoneNV " + key);
            SystemProperties.set("sys.telephony.kom.enable", String.valueOf(key));
        } catch (Exception e) {
            Slog.e("WindowManager", "Error getDemoPhoneNV: ", e);
        }
        createTranWatermark();
        showEmulatorDisplayOverlayIfNeeded();
        ITranWindowManagerService.Instance().onInitReady();
        if (this.mWmsExt.isAppResolutionTunerSupport() || this.mWmsExt.isAppResolutionTunerAISupport()) {
            this.mWmsExt.loadResolutionTunerAppList();
        }
        if (this.isMsyncOn) {
            this.mWmsExt.loadMSyncCtrlTable();
        }
    }

    public InputManagerCallback getInputManagerCallback() {
        return this.mInputManagerCallback;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException) && ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam0 = String.valueOf(e);
                ProtoLogImpl.wtf(ProtoLogGroup.WM_ERROR, 371641947, 0, "Window Manager Crash %s", new Object[]{protoLogParam0});
            }
            throw e;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean excludeWindowTypeFromTapOutTask(int windowType) {
        switch (windowType) {
            case 2000:
            case 2012:
            case 2019:
            case NotificationShellCmd.NOTIFICATION_ID /* 2020 */:
            case 2040:
                return true;
            default:
                return false;
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:34:0x00ce
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2025=23] */
    public int addWindow(com.android.server.wm.Session r46, android.view.IWindow r47, android.view.WindowManager.LayoutParams r48, int r49, int r50, int r51, android.view.InsetsVisibilities r52, android.view.InputChannel r53, android.view.InsetsState r54, android.view.InsetsSourceControl[] r55) {
        /*
            r45 = this;
            r13 = r45
            r14 = r46
            r15 = r48
            r12 = r50
            r11 = r51
            r10 = r53
            r9 = r55
            r0 = 0
            java.util.Arrays.fill(r9, r0)
            r8 = 1
            int[] r7 = new int[r8]
            int r1 = r15.privateFlags
            r2 = 1048576(0x100000, float:1.469368E-39)
            r1 = r1 & r2
            if (r1 == 0) goto L1e
            r1 = r8
            goto L1f
        L1e:
            r1 = 0
        L1f:
            r5 = r1
            com.android.server.policy.WindowManagerPolicy r1 = r13.mPolicy
            int r2 = r15.type
            java.lang.String r3 = r15.packageName
            int r16 = r1.checkAddPermission(r2, r5, r3, r7)
            java.lang.String r1 = "transsion_fold_screen"
            java.lang.CharSequence r2 = r48.getTitle()
            boolean r1 = r1.equals(r2)
            if (r1 != 0) goto L40
            boolean r1 = r48.getOSFullDialog()
            if (r1 == 0) goto L3e
            goto L40
        L3e:
            r1 = 0
            goto L41
        L40:
            r1 = r8
        L41:
            r4 = r1
            boolean r1 = com.android.server.wm.WindowManagerDebugConfig.DEBUG
            if (r1 == 0) goto L7b
            java.lang.String r1 = "WindowManager"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "onCreate: crossPermission = "
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r4)
            java.lang.String r3 = " attrs.getTitle() = "
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.CharSequence r3 = r48.getTitle()
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.String r3 = ",attrs.getOSFullDialog():"
            java.lang.StringBuilder r2 = r2.append(r3)
            boolean r3 = r48.getOSFullDialog()
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.String r2 = r2.toString()
            android.util.Slog.d(r1, r2)
        L7b:
            if (r16 == 0) goto L80
            if (r4 != 0) goto L80
            return r16
        L80:
            r1 = 0
            int r3 = android.os.Binder.getCallingUid()
            int r2 = android.os.Binder.getCallingPid()
            long r23 = android.os.Binder.clearCallingIdentity()
            r17 = r7
            int r7 = r15.type
            r18 = r5
            com.android.server.wm.WindowManagerGlobalLock r5 = r13.mGlobalLock
            monitor-enter(r5)
            boostPriorityForLockedSection()     // Catch: java.lang.Throwable -> Lb68
            boolean r0 = r13.mDisplayReady     // Catch: java.lang.Throwable -> Lb68
            if (r0 == 0) goto Lb48
            android.os.IBinder r0 = r15.token     // Catch: java.lang.Throwable -> Lb68
            com.android.server.wm.DisplayContent r0 = r13.getDisplayContentOrCreate(r12, r0)     // Catch: java.lang.Throwable -> Lb68
            r26 = r0
            r6 = r26
            if (r6 != 0) goto L135
            boolean r20 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L11e
            if (r20 == 0) goto Lfc
            r20 = r1
            long r0 = (long) r12
            com.android.internal.protolog.ProtoLogGroup r8 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> Le5
            r26 = r2
            java.lang.String r2 = "Attempted to add window to a display that does not exist: %d. Aborting."
            r27 = r3
            r28 = r4
            r3 = 1
            java.lang.Object[] r4 = new java.lang.Object[r3]     // Catch: java.lang.Throwable -> L10b
            java.lang.Long r22 = java.lang.Long.valueOf(r0)     // Catch: java.lang.Throwable -> L10b
            r19 = 0
            r4[r19] = r22     // Catch: java.lang.Throwable -> L10b
            r29 = r0
            r0 = -861859917(0xffffffffcca10fb3, float:-8.444252E7)
            com.android.internal.protolog.ProtoLogImpl.w(r8, r0, r3, r2, r4)     // Catch: java.lang.Throwable -> L10b
            goto L104
        Lce:
            r0 = move-exception
            r27 = r3
            r28 = r4
            r34 = r5
            r21 = r7
            r4 = r9
            r2 = r13
            r38 = r17
            r33 = r18
            r1 = r20
            r17 = r26
            r5 = r54
            goto Lb7d
        Le5:
            r0 = move-exception
            r27 = r3
            r28 = r4
            r34 = r5
            r21 = r7
            r4 = r9
            r38 = r17
            r33 = r18
            r1 = r20
            r5 = r54
            r17 = r2
            r2 = r13
            goto Lb7d
        Lfc:
            r20 = r1
            r26 = r2
            r27 = r3
            r28 = r4
        L104:
            monitor-exit(r5)     // Catch: java.lang.Throwable -> L10b
            resetPriorityAfterLockedSection()
            r0 = -9
            return r0
        L10b:
            r0 = move-exception
            r34 = r5
            r21 = r7
            r4 = r9
            r2 = r13
            r38 = r17
            r33 = r18
            r1 = r20
            r17 = r26
            r5 = r54
            goto Lb7d
        L11e:
            r0 = move-exception
            r20 = r1
            r27 = r3
            r28 = r4
            r34 = r5
            r21 = r7
            r4 = r9
            r38 = r17
            r33 = r18
            r5 = r54
            r17 = r2
            r2 = r13
            goto Lb7d
        L135:
            r20 = r1
            r26 = r2
            r27 = r3
            r28 = r4
            int r0 = r14.mUid     // Catch: java.lang.Throwable -> Lb36
            boolean r0 = r6.hasAccess(r0)     // Catch: java.lang.Throwable -> Lb36
            if (r0 != 0) goto L16c
            boolean r0 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L10b
            if (r0 == 0) goto L165
            int r0 = r6.getDisplayId()     // Catch: java.lang.Throwable -> L10b
            long r0 = (long) r0     // Catch: java.lang.Throwable -> L10b
            com.android.internal.protolog.ProtoLogGroup r2 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L10b
            java.lang.String r4 = "Attempted to add window to a display for which the application does not have access: %d.  Aborting."
            r8 = 1
            java.lang.Object[] r3 = new java.lang.Object[r8]     // Catch: java.lang.Throwable -> L10b
            java.lang.Long r25 = java.lang.Long.valueOf(r0)     // Catch: java.lang.Throwable -> L10b
            r19 = 0
            r3[r19] = r25     // Catch: java.lang.Throwable -> L10b
            r29 = r0
            r0 = 435494046(0x19f51c9e, float:2.5343965E-23)
            com.android.internal.protolog.ProtoLogImpl.w(r2, r0, r8, r4, r3)     // Catch: java.lang.Throwable -> L10b
        L165:
            monitor-exit(r5)     // Catch: java.lang.Throwable -> L10b
            resetPriorityAfterLockedSection()
            r0 = -9
            return r0
        L16c:
            java.util.HashMap<android.os.IBinder, com.android.server.wm.WindowState> r0 = r13.mWindowMap     // Catch: java.lang.Throwable -> Lb36
            android.os.IBinder r1 = r47.asBinder()     // Catch: java.lang.Throwable -> Lb36
            boolean r0 = r0.containsKey(r1)     // Catch: java.lang.Throwable -> Lb36
            r29 = -5
            if (r0 == 0) goto L197
            boolean r0 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L10b
            if (r0 == 0) goto L192
            java.lang.String r0 = java.lang.String.valueOf(r47)     // Catch: java.lang.Throwable -> L10b
            com.android.internal.protolog.ProtoLogGroup r1 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L10b
            r2 = -507657818(0xffffffffe1bdc1a6, float:-4.3754856E20)
            java.lang.String r3 = "Window %s is already added"
            r4 = 1
            java.lang.Object[] r4 = new java.lang.Object[r4]     // Catch: java.lang.Throwable -> L10b
            r8 = 0
            r4[r8] = r0     // Catch: java.lang.Throwable -> L10b
            com.android.internal.protolog.ProtoLogImpl.w(r1, r2, r8, r3, r4)     // Catch: java.lang.Throwable -> L10b
        L192:
            monitor-exit(r5)     // Catch: java.lang.Throwable -> L10b
            resetPriorityAfterLockedSection()
            return r29
        L197:
            r0 = 1000(0x3e8, float:1.401E-42)
            if (r7 < r0) goto L20e
            r1 = 1999(0x7cf, float:2.801E-42)
            if (r7 > r1) goto L20e
            android.os.IBinder r2 = r15.token     // Catch: java.lang.Throwable -> L10b
            r3 = 0
            r4 = 0
            com.android.server.wm.WindowState r2 = r13.windowForClientLocked(r3, r2, r4)     // Catch: java.lang.Throwable -> L10b
            if (r2 != 0) goto L1cb
            boolean r0 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L1fc
            if (r0 == 0) goto L1c5
            android.os.IBinder r0 = r15.token     // Catch: java.lang.Throwable -> L1fc
            java.lang.String r0 = java.lang.String.valueOf(r0)     // Catch: java.lang.Throwable -> L1fc
            com.android.internal.protolog.ProtoLogGroup r1 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L1fc
            java.lang.String r8 = "Attempted to add window with token that is not a window: %s.  Aborting."
            r3 = 1
            java.lang.Object[] r3 = new java.lang.Object[r3]     // Catch: java.lang.Throwable -> L1fc
            r4 = 0
            r3[r4] = r0     // Catch: java.lang.Throwable -> L1fc
            r19 = r0
            r0 = 631792420(0x25a86324, float:2.9210535E-16)
            com.android.internal.protolog.ProtoLogImpl.w(r1, r0, r4, r8, r3)     // Catch: java.lang.Throwable -> L1fc
        L1c5:
            monitor-exit(r5)     // Catch: java.lang.Throwable -> L1fc
            resetPriorityAfterLockedSection()
            r0 = -2
            return r0
        L1cb:
            android.view.WindowManager$LayoutParams r3 = r2.mAttrs     // Catch: java.lang.Throwable -> L1fc
            int r3 = r3.type     // Catch: java.lang.Throwable -> L1fc
            if (r3 < r0) goto L1f9
            android.view.WindowManager$LayoutParams r0 = r2.mAttrs     // Catch: java.lang.Throwable -> L1fc
            int r0 = r0.type     // Catch: java.lang.Throwable -> L1fc
            if (r0 > r1) goto L1f9
            boolean r0 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L1fc
            if (r0 == 0) goto L1f3
            android.os.IBinder r0 = r15.token     // Catch: java.lang.Throwable -> L1fc
            java.lang.String r0 = java.lang.String.valueOf(r0)     // Catch: java.lang.Throwable -> L1fc
            com.android.internal.protolog.ProtoLogGroup r1 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L1fc
            java.lang.String r4 = "Attempted to add window with token that is a sub-window: %s.  Aborting."
            r8 = 1
            java.lang.Object[] r8 = new java.lang.Object[r8]     // Catch: java.lang.Throwable -> L1fc
            r3 = 0
            r8[r3] = r0     // Catch: java.lang.Throwable -> L1fc
            r19 = r0
            r0 = -2072089308(0xffffffff847e6d24, float:-2.9907671E-36)
            com.android.internal.protolog.ProtoLogImpl.w(r1, r0, r3, r4, r8)     // Catch: java.lang.Throwable -> L1fc
        L1f3:
            monitor-exit(r5)     // Catch: java.lang.Throwable -> L1fc
            resetPriorityAfterLockedSection()
            r0 = -2
            return r0
        L1f9:
            r8 = 1
            r4 = r2
            goto L211
        L1fc:
            r0 = move-exception
            r1 = r2
            r34 = r5
            r21 = r7
            r4 = r9
            r2 = r13
            r38 = r17
            r33 = r18
            r17 = r26
            r5 = r54
            goto Lb7d
        L20e:
            r8 = 1
            r4 = r20
        L211:
            r0 = 2030(0x7ee, float:2.845E-42)
            if (r7 != r0) goto L246
            boolean r0 = r6.isPrivate()     // Catch: java.lang.Throwable -> L234
            if (r0 != 0) goto L246
            boolean r0 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L234
            if (r0 == 0) goto L22e
            com.android.internal.protolog.ProtoLogGroup r0 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L234
            r1 = -784959154(0xffffffffd136794e, float:-4.898245E10)
            java.lang.String r2 = "Attempted to add private presentation window to a non-private display.  Aborting."
            r3 = 0
            r8 = r3
            java.lang.Object[] r8 = (java.lang.Object[]) r8     // Catch: java.lang.Throwable -> L234
            r8 = 0
            com.android.internal.protolog.ProtoLogImpl.w(r0, r1, r8, r2, r3)     // Catch: java.lang.Throwable -> L234
        L22e:
            r0 = -8
            monitor-exit(r5)     // Catch: java.lang.Throwable -> L234
            resetPriorityAfterLockedSection()
            return r0
        L234:
            r0 = move-exception
            r1 = r4
            r34 = r5
            r21 = r7
            r4 = r9
            r2 = r13
            r38 = r17
            r33 = r18
            r17 = r26
            r5 = r54
            goto Lb7d
        L246:
            r0 = 2037(0x7f5, float:2.854E-42)
            if (r7 != r0) goto L26e
            android.view.Display r0 = r6.getDisplay()     // Catch: java.lang.Throwable -> L234
            boolean r0 = r0.isPublicPresentation()     // Catch: java.lang.Throwable -> L234
            if (r0 != 0) goto L26e
            boolean r0 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L234
            if (r0 == 0) goto L267
            com.android.internal.protolog.ProtoLogGroup r0 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L234
            r1 = -1670695197(0xffffffff9c6b36e3, float:-7.7825917E-22)
            java.lang.String r2 = "Attempted to add presentation window to a non-suitable display.  Aborting."
            r3 = 0
            r8 = r3
            java.lang.Object[] r8 = (java.lang.Object[]) r8     // Catch: java.lang.Throwable -> L234
            r8 = 0
            com.android.internal.protolog.ProtoLogImpl.w(r0, r1, r8, r2, r3)     // Catch: java.lang.Throwable -> L234
        L267:
            monitor-exit(r5)     // Catch: java.lang.Throwable -> L234
            resetPriorityAfterLockedSection()
            r0 = -9
            return r0
        L26e:
            r19 = 0
            int r0 = r14.mUid     // Catch: java.lang.Throwable -> Lb22
            int r0 = android.os.UserHandle.getUserId(r0)     // Catch: java.lang.Throwable -> Lb22
            r3 = r0
            if (r11 == r3) goto L300
            android.app.ActivityManagerInternal r1 = r13.mAmInternal     // Catch: java.lang.Throwable -> L2ba java.lang.Exception -> L2cf
            r0 = 0
            r20 = 0
            r22 = 0
            r30 = 0
            r31 = r26
            r2 = r31
            r26 = r3
            r3 = r27
            r32 = r4
            r4 = r51
            r34 = r5
            r33 = r18
            r5 = r0
            r35 = r6
            r6 = r20
            r37 = r7
            r38 = r17
            r7 = r22
            r12 = r8
            r8 = r30
            r1.handleIncomingUser(r2, r3, r4, r5, r6, r7, r8)     // Catch: java.lang.Throwable -> L2ab java.lang.Exception -> L2b8
            r3 = r51
            r26 = r3
            r8 = 0
            goto L313
        L2ab:
            r0 = move-exception
            r5 = r54
            r4 = r9
            r2 = r13
            r17 = r31
            r1 = r32
            r21 = r37
            goto Lb7d
        L2b8:
            r0 = move-exception
            goto L2e1
        L2ba:
            r0 = move-exception
            r32 = r4
            r34 = r5
            r38 = r17
            r5 = r54
            r21 = r7
            r4 = r9
            r2 = r13
            r33 = r18
            r17 = r26
            r1 = r32
            goto Lb7d
        L2cf:
            r0 = move-exception
            r32 = r4
            r34 = r5
            r35 = r6
            r37 = r7
            r12 = r8
            r38 = r17
            r33 = r18
            r31 = r26
            r26 = r3
        L2e1:
            boolean r1 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L2ab
            if (r1 == 0) goto L2f9
            long r1 = (long) r11     // Catch: java.lang.Throwable -> L2ab
            com.android.internal.protolog.ProtoLogGroup r3 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L2ab
            r4 = 315395835(0x12cc8efb, float:1.2909457E-27)
            java.lang.String r5 = "Trying to add window with invalid user=%d"
            java.lang.Object[] r6 = new java.lang.Object[r12]     // Catch: java.lang.Throwable -> L2ab
            java.lang.Long r7 = java.lang.Long.valueOf(r1)     // Catch: java.lang.Throwable -> L2ab
            r8 = 0
            r6[r8] = r7     // Catch: java.lang.Throwable -> L2ab
            com.android.internal.protolog.ProtoLogImpl.w(r3, r4, r12, r5, r6)     // Catch: java.lang.Throwable -> L2ab
        L2f9:
            r1 = -11
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L2ab
            resetPriorityAfterLockedSection()
            return r1
        L300:
            r32 = r4
            r34 = r5
            r35 = r6
            r37 = r7
            r12 = r8
            r38 = r17
            r33 = r18
            r8 = r19
            r31 = r26
            r26 = r3
        L313:
            r0 = 0
            r7 = r32
            if (r7 == 0) goto L31a
            r1 = r12
            goto L31b
        L31a:
            r1 = r8
        L31b:
            r30 = r1
            if (r30 == 0) goto L331
            android.view.WindowManager$LayoutParams r1 = r7.mAttrs     // Catch: java.lang.Throwable -> L325
            android.os.IBinder r1 = r1.token     // Catch: java.lang.Throwable -> L325
            goto L333
        L325:
            r0 = move-exception
            r5 = r54
            r1 = r7
            r4 = r9
            r2 = r13
            r17 = r31
            r21 = r37
            goto Lb7d
        L331:
            android.os.IBinder r1 = r15.token     // Catch: java.lang.Throwable -> Lb13
        L333:
            r6 = r35
            com.android.server.wm.WindowToken r1 = r6.getWindowToken(r1)     // Catch: java.lang.Throwable -> Lb13
            r5 = r1
            if (r30 == 0) goto L341
            android.view.WindowManager$LayoutParams r1 = r7.mAttrs     // Catch: java.lang.Throwable -> L325
            int r1 = r1.type     // Catch: java.lang.Throwable -> L325
            goto L343
        L341:
            r1 = r37
        L343:
            r4 = r1
            r17 = 0
            android.os.IBinder r1 = r15.mWindowContextToken     // Catch: java.lang.Throwable -> Lb13
            r3 = r1
            r18 = -4
            r2 = 3
            r1 = 2005(0x7d5, float:2.81E-42)
            r20 = -1
            if (r5 != 0) goto L445
            android.os.IBinder r8 = r15.token     // Catch: java.lang.Throwable -> L438
            java.lang.String r12 = r15.packageName     // Catch: java.lang.Throwable -> L438
            r35 = r0
            r0 = r1
            r1 = r45
            r0 = r2
            r2 = r7
            r0 = r3
            r3 = r27
            r39 = r4
            r4 = r37
            r40 = r5
            r5 = r39
            r11 = r6
            r6 = r8
            r8 = r7
            r7 = r12
            boolean r1 = r1.unprivilegedAppCanCreateTokenWith(r2, r3, r4, r5, r6, r7)     // Catch: java.lang.Throwable -> L42c
            if (r1 != 0) goto L383
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L377
            resetPriorityAfterLockedSection()
            return r20
        L377:
            r0 = move-exception
            r5 = r54
            r1 = r8
            r4 = r9
            r2 = r13
            r17 = r31
            r21 = r37
            goto Lb7d
        L383:
            if (r30 == 0) goto L395
            com.android.server.wm.WindowToken r1 = r8.mToken     // Catch: java.lang.Throwable -> L377
            r5 = r1
            r40 = r5
            r32 = r17
            r7 = r35
            r12 = r37
            r6 = r39
            r5 = 1
            goto L698
        L395:
            com.android.server.wm.WindowContextListenerController r1 = r13.mWindowContextListenerController     // Catch: java.lang.Throwable -> L42c
            boolean r1 = r1.hasListener(r0)     // Catch: java.lang.Throwable -> L42c
            if (r1 == 0) goto L3ea
            android.os.IBinder r1 = r15.token     // Catch: java.lang.Throwable -> L42c
            if (r1 == 0) goto L3a4
            android.os.IBinder r3 = r15.token     // Catch: java.lang.Throwable -> L377
            goto L3a5
        L3a4:
            r3 = r0
        L3a5:
            r1 = r3
            com.android.server.wm.WindowContextListenerController r2 = r13.mWindowContextListenerController     // Catch: java.lang.Throwable -> L42c
            android.os.Bundle r2 = r2.getOptions(r0)     // Catch: java.lang.Throwable -> L42c
            com.android.server.wm.WindowToken$Builder r3 = new com.android.server.wm.WindowToken$Builder     // Catch: java.lang.Throwable -> L42c
            r12 = r37
            r3.<init>(r13, r1, r12)     // Catch: java.lang.Throwable -> L3de
            com.android.server.wm.WindowToken$Builder r3 = r3.setDisplayContent(r11)     // Catch: java.lang.Throwable -> L3de
            boolean r4 = r14.mCanAddInternalSystemWindow     // Catch: java.lang.Throwable -> L3de
            com.android.server.wm.WindowToken$Builder r3 = r3.setOwnerCanManageAppTokens(r4)     // Catch: java.lang.Throwable -> L3de
            r7 = r33
            com.android.server.wm.WindowToken$Builder r3 = r3.setRoundedCornerOverlay(r7)     // Catch: java.lang.Throwable -> L41e
            r4 = 1
            com.android.server.wm.WindowToken$Builder r3 = r3.setFromClientToken(r4)     // Catch: java.lang.Throwable -> L41e
            com.android.server.wm.WindowToken$Builder r3 = r3.setOptions(r2)     // Catch: java.lang.Throwable -> L41e
            com.android.server.wm.WindowToken r3 = r3.build()     // Catch: java.lang.Throwable -> L41e
            r5 = r3
            r40 = r5
            r33 = r7
            r32 = r17
            r7 = r35
            r6 = r39
            r5 = 1
            goto L698
        L3de:
            r0 = move-exception
            r5 = r54
            r1 = r8
            r4 = r9
            r21 = r12
            r2 = r13
            r17 = r31
            goto Lb7d
        L3ea:
            r7 = r33
            r12 = r37
            android.os.IBinder r1 = r15.token     // Catch: java.lang.Throwable -> L41e
            if (r1 == 0) goto L3f5
            android.os.IBinder r1 = r15.token     // Catch: java.lang.Throwable -> L41e
            goto L3f9
        L3f5:
            android.os.IBinder r1 = r47.asBinder()     // Catch: java.lang.Throwable -> L41e
        L3f9:
            com.android.server.wm.WindowToken$Builder r2 = new com.android.server.wm.WindowToken$Builder     // Catch: java.lang.Throwable -> L41e
            r2.<init>(r13, r1, r12)     // Catch: java.lang.Throwable -> L41e
            com.android.server.wm.WindowToken$Builder r2 = r2.setDisplayContent(r11)     // Catch: java.lang.Throwable -> L41e
            boolean r3 = r14.mCanAddInternalSystemWindow     // Catch: java.lang.Throwable -> L41e
            com.android.server.wm.WindowToken$Builder r2 = r2.setOwnerCanManageAppTokens(r3)     // Catch: java.lang.Throwable -> L41e
            com.android.server.wm.WindowToken$Builder r2 = r2.setRoundedCornerOverlay(r7)     // Catch: java.lang.Throwable -> L41e
            com.android.server.wm.WindowToken r2 = r2.build()     // Catch: java.lang.Throwable -> L41e
            r5 = r2
            r40 = r5
            r33 = r7
            r32 = r17
            r7 = r35
            r6 = r39
            r5 = 1
            goto L698
        L41e:
            r0 = move-exception
            r5 = r54
            r33 = r7
            r1 = r8
            r4 = r9
            r21 = r12
            r2 = r13
            r17 = r31
            goto Lb7d
        L42c:
            r0 = move-exception
            r5 = r54
            r1 = r8
            r4 = r9
            r2 = r13
            r17 = r31
            r21 = r37
            goto Lb7d
        L438:
            r0 = move-exception
            r8 = r7
            r5 = r54
            r1 = r8
            r4 = r9
            r2 = r13
            r17 = r31
            r21 = r37
            goto Lb7d
        L445:
            r35 = r0
            r0 = r3
            r39 = r4
            r40 = r5
            r11 = r6
            r8 = r7
            r7 = r33
            r12 = r37
            r6 = r39
            r1 = 1
            if (r6 < r1) goto L4fc
            r1 = 99
            if (r6 > r1) goto L4fc
            com.android.server.wm.ActivityRecord r1 = r40.asActivityRecord()     // Catch: java.lang.Throwable -> L4ee
            if (r1 != 0) goto L486
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L4ee
            if (r2 == 0) goto L47e
            java.lang.String r2 = java.lang.String.valueOf(r40)     // Catch: java.lang.Throwable -> L4ee
            com.android.internal.protolog.ProtoLogGroup r3 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L4ee
            java.lang.String r5 = "Attempted to add window with non-application token .%s Aborting."
            r4 = 1
            java.lang.Object[] r4 = new java.lang.Object[r4]     // Catch: java.lang.Throwable -> L4ee
            r33 = r7
            r7 = 0
            r4[r7] = r2     // Catch: java.lang.Throwable -> L52e
            r19 = r2
            r2 = 246676969(0xeb3fde9, float:4.4371413E-30)
            com.android.internal.protolog.ProtoLogImpl.w(r3, r2, r7, r5, r4)     // Catch: java.lang.Throwable -> L52e
            goto L480
        L47e:
            r33 = r7
        L480:
            r2 = -3
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L52e
            resetPriorityAfterLockedSection()
            return r2
        L486:
            r33 = r7
            com.android.server.wm.WindowContainer r2 = r1.getParent()     // Catch: java.lang.Throwable -> L52e
            if (r2 != 0) goto L4ad
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L52e
            if (r2 == 0) goto L4a8
            java.lang.String r2 = java.lang.String.valueOf(r40)     // Catch: java.lang.Throwable -> L52e
            com.android.internal.protolog.ProtoLogGroup r3 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L52e
            java.lang.String r5 = "Attempted to add window with exiting application token .%s Aborting."
            r7 = 1
            java.lang.Object[] r7 = new java.lang.Object[r7]     // Catch: java.lang.Throwable -> L52e
            r4 = 0
            r7[r4] = r2     // Catch: java.lang.Throwable -> L52e
            r20 = r2
            r2 = -853226675(0xffffffffcd24cb4d, float:-1.72799184E8)
            com.android.internal.protolog.ProtoLogImpl.w(r3, r2, r4, r5, r7)     // Catch: java.lang.Throwable -> L52e
        L4a8:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L52e
            resetPriorityAfterLockedSection()
            return r18
        L4ad:
            r2 = 3
            if (r12 != r2) goto L4e8
            com.android.server.wm.WindowState r2 = r1.mStartingWindow     // Catch: java.lang.Throwable -> L52e
            if (r2 == 0) goto L4cc
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L52e
            if (r2 == 0) goto L4c7
            com.android.internal.protolog.ProtoLogGroup r2 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L52e
            r3 = -167822951(0xfffffffff5ff3999, float:-6.4707223E32)
            java.lang.String r4 = "Attempted to add starting window to token with already existing starting window"
            r5 = 0
            r7 = r5
            java.lang.Object[] r7 = (java.lang.Object[]) r7     // Catch: java.lang.Throwable -> L52e
            r7 = 0
            com.android.internal.protolog.ProtoLogImpl.w(r2, r3, r7, r4, r5)     // Catch: java.lang.Throwable -> L52e
        L4c7:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L52e
            resetPriorityAfterLockedSection()
            return r29
        L4cc:
            com.android.server.wm.StartingData r2 = r1.mStartingData     // Catch: java.lang.Throwable -> L52e
            if (r2 != 0) goto L4e8
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L52e
            if (r2 == 0) goto L4e3
            com.android.internal.protolog.ProtoLogGroup r2 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L52e
            r3 = 1804245629(0x6b8a9a7d, float:3.3512263E26)
            java.lang.String r4 = "Attempted to add starting window to token but already cleaned"
            r5 = 0
            r7 = r5
            java.lang.Object[] r7 = (java.lang.Object[]) r7     // Catch: java.lang.Throwable -> L52e
            r7 = 0
            com.android.internal.protolog.ProtoLogImpl.w(r2, r3, r7, r4, r5)     // Catch: java.lang.Throwable -> L52e
        L4e3:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L52e
            resetPriorityAfterLockedSection()
            return r29
        L4e8:
            r7 = r1
            r32 = r17
            r5 = 1
            goto L698
        L4ee:
            r0 = move-exception
            r5 = r54
            r33 = r7
            r1 = r8
            r4 = r9
            r21 = r12
            r2 = r13
            r17 = r31
            goto Lb7d
        L4fc:
            r33 = r7
            r7 = 2011(0x7db, float:2.818E-42)
            if (r6 != r7) goto L53a
            r1 = r40
            int r2 = r1.windowType     // Catch: java.lang.Throwable -> L52e
            if (r2 == r7) goto L529
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L52e
            if (r2 == 0) goto L524
            android.os.IBinder r2 = r15.token     // Catch: java.lang.Throwable -> L52e
            java.lang.String r2 = java.lang.String.valueOf(r2)     // Catch: java.lang.Throwable -> L52e
            com.android.internal.protolog.ProtoLogGroup r3 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L52e
            java.lang.String r5 = "Attempted to add input method window with bad token %s.  Aborting."
            r7 = 1
            java.lang.Object[] r7 = new java.lang.Object[r7]     // Catch: java.lang.Throwable -> L52e
            r4 = 0
            r7[r4] = r2     // Catch: java.lang.Throwable -> L52e
            r19 = r2
            r2 = -1949279037(0xffffffff8bd05cc3, float:-8.025826E-32)
            com.android.internal.protolog.ProtoLogImpl.w(r3, r2, r4, r5, r7)     // Catch: java.lang.Throwable -> L52e
        L524:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L52e
            resetPriorityAfterLockedSection()
            return r20
        L529:
            r40 = r1
            r5 = 1
            goto L694
        L52e:
            r0 = move-exception
            r5 = r54
            r1 = r8
            r4 = r9
            r21 = r12
            r2 = r13
            r17 = r31
            goto Lb7d
        L53a:
            r1 = r40
            r2 = 2031(0x7ef, float:2.846E-42)
            if (r6 != r2) goto L56a
            int r3 = r1.windowType     // Catch: java.lang.Throwable -> L52e
            if (r3 == r2) goto L565
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L52e
            if (r2 == 0) goto L560
            android.os.IBinder r2 = r15.token     // Catch: java.lang.Throwable -> L52e
            java.lang.String r2 = java.lang.String.valueOf(r2)     // Catch: java.lang.Throwable -> L52e
            com.android.internal.protolog.ProtoLogGroup r3 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L52e
            java.lang.String r5 = "Attempted to add voice interaction window with bad token %s.  Aborting."
            r7 = 1
            java.lang.Object[] r7 = new java.lang.Object[r7]     // Catch: java.lang.Throwable -> L52e
            r4 = 0
            r7[r4] = r2     // Catch: java.lang.Throwable -> L52e
            r19 = r2
            r2 = -1389772804(0xffffffffad29bffc, float:-9.649167E-12)
            com.android.internal.protolog.ProtoLogImpl.w(r3, r2, r4, r5, r7)     // Catch: java.lang.Throwable -> L52e
        L560:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L52e
            resetPriorityAfterLockedSection()
            return r20
        L565:
            r40 = r1
            r5 = 1
            goto L694
        L56a:
            r5 = 2013(0x7dd, float:2.821E-42)
            if (r6 != r5) goto L598
            int r2 = r1.windowType     // Catch: java.lang.Throwable -> L52e
            if (r2 == r5) goto L593
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L52e
            if (r2 == 0) goto L58e
            android.os.IBinder r2 = r15.token     // Catch: java.lang.Throwable -> L52e
            java.lang.String r2 = java.lang.String.valueOf(r2)     // Catch: java.lang.Throwable -> L52e
            com.android.internal.protolog.ProtoLogGroup r3 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L52e
            java.lang.String r5 = "Attempted to add wallpaper window with bad token %s.  Aborting."
            r7 = 1
            java.lang.Object[] r7 = new java.lang.Object[r7]     // Catch: java.lang.Throwable -> L52e
            r4 = 0
            r7[r4] = r2     // Catch: java.lang.Throwable -> L52e
            r19 = r2
            r2 = -1915280162(0xffffffff8dd724de, float:-1.3259273E-30)
            com.android.internal.protolog.ProtoLogImpl.w(r3, r2, r4, r5, r7)     // Catch: java.lang.Throwable -> L52e
        L58e:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L52e
            resetPriorityAfterLockedSection()
            return r20
        L593:
            r40 = r1
            r5 = 1
            goto L694
        L598:
            r2 = 2032(0x7f0, float:2.847E-42)
            if (r6 != r2) goto L5c6
            int r3 = r1.windowType     // Catch: java.lang.Throwable -> L52e
            if (r3 == r2) goto L5c1
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L52e
            if (r2 == 0) goto L5bc
            android.os.IBinder r2 = r15.token     // Catch: java.lang.Throwable -> L52e
            java.lang.String r2 = java.lang.String.valueOf(r2)     // Catch: java.lang.Throwable -> L52e
            com.android.internal.protolog.ProtoLogGroup r3 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L52e
            java.lang.String r5 = "Attempted to add Accessibility overlay window with bad token %s.  Aborting."
            r7 = 1
            java.lang.Object[] r7 = new java.lang.Object[r7]     // Catch: java.lang.Throwable -> L52e
            r4 = 0
            r7[r4] = r2     // Catch: java.lang.Throwable -> L52e
            r19 = r2
            r2 = -1976930686(0xffffffff8a2a6e82, float:-8.2059865E-33)
            com.android.internal.protolog.ProtoLogImpl.w(r3, r2, r4, r5, r7)     // Catch: java.lang.Throwable -> L52e
        L5bc:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L52e
            resetPriorityAfterLockedSection()
            return r20
        L5c1:
            r40 = r1
            r5 = 1
            goto L694
        L5c6:
            r2 = 2005(0x7d5, float:2.81E-42)
            if (r12 != r2) goto L61b
            java.lang.String r2 = r15.packageName     // Catch: java.lang.Throwable -> L52e
            r4 = r27
            boolean r2 = r13.doesAddToastWindowRequireToken(r2, r4, r8)     // Catch: java.lang.Throwable -> L60d
            r17 = r2
            if (r17 == 0) goto L602
            int r2 = r1.windowType     // Catch: java.lang.Throwable -> L60d
            r3 = 2005(0x7d5, float:2.81E-42)
            if (r2 == r3) goto L602
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L60d
            if (r2 == 0) goto L5fb
            android.os.IBinder r2 = r15.token     // Catch: java.lang.Throwable -> L60d
            java.lang.String r2 = java.lang.String.valueOf(r2)     // Catch: java.lang.Throwable -> L60d
            com.android.internal.protolog.ProtoLogGroup r3 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L60d
            java.lang.String r7 = "Attempted to add a toast window with bad token %s.  Aborting."
            r5 = 1
            java.lang.Object[] r5 = new java.lang.Object[r5]     // Catch: java.lang.Throwable -> L60d
            r27 = r4
            r4 = 0
            r5[r4] = r2     // Catch: java.lang.Throwable -> L52e
            r19 = r2
            r2 = 662572728(0x277e0eb8, float:3.525756E-15)
            com.android.internal.protolog.ProtoLogImpl.w(r3, r2, r4, r7, r5)     // Catch: java.lang.Throwable -> L52e
            goto L5fd
        L5fb:
            r27 = r4
        L5fd:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L52e
            resetPriorityAfterLockedSection()
            return r20
        L602:
            r27 = r4
            r40 = r1
            r32 = r17
            r7 = r35
            r5 = 1
            goto L698
        L60d:
            r0 = move-exception
            r5 = r54
            r27 = r4
            r1 = r8
            r4 = r9
            r21 = r12
            r2 = r13
            r17 = r31
            goto Lb7d
        L61b:
            r2 = 2035(0x7f3, float:2.852E-42)
            if (r12 != r2) goto L648
            int r3 = r1.windowType     // Catch: java.lang.Throwable -> L52e
            if (r3 == r2) goto L644
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L52e
            if (r2 == 0) goto L63f
            android.os.IBinder r2 = r15.token     // Catch: java.lang.Throwable -> L52e
            java.lang.String r2 = java.lang.String.valueOf(r2)     // Catch: java.lang.Throwable -> L52e
            com.android.internal.protolog.ProtoLogGroup r3 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L52e
            java.lang.String r5 = "Attempted to add QS dialog window with bad token %s.  Aborting."
            r7 = 1
            java.lang.Object[] r7 = new java.lang.Object[r7]     // Catch: java.lang.Throwable -> L52e
            r4 = 0
            r7[r4] = r2     // Catch: java.lang.Throwable -> L52e
            r19 = r2
            r2 = -1060365734(0xffffffffc0cc1a5a, float:-6.3782167)
            com.android.internal.protolog.ProtoLogImpl.w(r3, r2, r4, r5, r7)     // Catch: java.lang.Throwable -> L52e
        L63f:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L52e
            resetPriorityAfterLockedSection()
            return r20
        L644:
            r40 = r1
            r5 = 1
            goto L694
        L648:
            com.android.server.wm.ActivityRecord r2 = r1.asActivityRecord()     // Catch: java.lang.Throwable -> Lb04
            if (r2 == 0) goto L691
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L52e
            if (r2 == 0) goto L66d
            long r2 = (long) r6     // Catch: java.lang.Throwable -> L52e
            com.android.internal.protolog.ProtoLogGroup r4 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L52e
            java.lang.String r7 = "Non-null activity for system window of rootType=%d"
            r40 = r1
            r5 = 1
            java.lang.Object[] r1 = new java.lang.Object[r5]     // Catch: java.lang.Throwable -> L52e
            java.lang.Long r32 = java.lang.Long.valueOf(r2)     // Catch: java.lang.Throwable -> L52e
            r36 = 0
            r1[r36] = r32     // Catch: java.lang.Throwable -> L52e
            r41 = r2
            r2 = 372792199(0x16385b87, float:1.4892283E-25)
            com.android.internal.protolog.ProtoLogImpl.w(r4, r2, r5, r7, r1)     // Catch: java.lang.Throwable -> L52e
            goto L670
        L66d:
            r40 = r1
            r5 = 1
        L670:
            r1 = 0
            r15.token = r1     // Catch: java.lang.Throwable -> L52e
            com.android.server.wm.WindowToken$Builder r1 = new com.android.server.wm.WindowToken$Builder     // Catch: java.lang.Throwable -> L52e
            android.os.IBinder r2 = r47.asBinder()     // Catch: java.lang.Throwable -> L52e
            r1.<init>(r13, r2, r12)     // Catch: java.lang.Throwable -> L52e
            com.android.server.wm.WindowToken$Builder r1 = r1.setDisplayContent(r11)     // Catch: java.lang.Throwable -> L52e
            boolean r2 = r14.mCanAddInternalSystemWindow     // Catch: java.lang.Throwable -> L52e
            com.android.server.wm.WindowToken$Builder r1 = r1.setOwnerCanManageAppTokens(r2)     // Catch: java.lang.Throwable -> L52e
            com.android.server.wm.WindowToken r1 = r1.build()     // Catch: java.lang.Throwable -> L52e
            r40 = r1
            r32 = r17
            r7 = r35
            goto L698
        L691:
            r40 = r1
            r5 = 1
        L694:
            r32 = r17
            r7 = r35
        L698:
            com.android.server.wm.WindowState r17 = new com.android.server.wm.WindowState     // Catch: java.lang.Throwable -> Lb04
            r20 = 0
            r35 = r38[r20]     // Catch: java.lang.Throwable -> Lb04
            int r4 = r14.mUid     // Catch: java.lang.Throwable -> Lb04
            boolean r3 = r14.mCanAddInternalSystemWindow     // Catch: java.lang.Throwable -> Lb04
            r1 = r17
            r2 = r45
            r36 = r3
            r3 = r46
            r14 = r27
            r27 = r4
            r4 = r47
            r37 = r5
            r19 = 2013(0x7dd, float:2.821E-42)
            r5 = r40
            r39 = r6
            r6 = r8
            r43 = r7
            r22 = 2011(0x7db, float:2.818E-42)
            r7 = r35
            r35 = r8
            r13 = r20
            r8 = r48
            r9 = r49
            r10 = r27
            r27 = r11
            r11 = r26
            r44 = r12
            r13 = r37
            r12 = r36
            r1.<init>(r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12)     // Catch: java.lang.Throwable -> Laf3
            r12 = r17
            if (r28 == 0) goto L6ef
            r12.setOSFullDialog()     // Catch: java.lang.Throwable -> L6de
            goto L6ef
        L6de:
            r0 = move-exception
            r2 = r45
            r5 = r54
            r4 = r55
            r27 = r14
            r17 = r31
            r1 = r35
            r21 = r44
            goto Lb7d
        L6ef:
            com.android.server.wm.WindowState$DeathRecipient r1 = r12.mDeathRecipient     // Catch: java.lang.Throwable -> Laf3
            if (r1 != 0) goto L713
            boolean r1 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L6de
            if (r1 == 0) goto L70e
            android.os.IBinder r1 = r47.asBinder()     // Catch: java.lang.Throwable -> L6de
            java.lang.String r1 = java.lang.String.valueOf(r1)     // Catch: java.lang.Throwable -> L6de
            com.android.internal.protolog.ProtoLogGroup r2 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L6de
            r3 = -1770075711(0xffffffff967ec9c1, float:-2.0581619E-25)
            java.lang.String r4 = "Adding window client %s that is dead, aborting."
            java.lang.Object[] r5 = new java.lang.Object[r13]     // Catch: java.lang.Throwable -> L6de
            r6 = 0
            r5[r6] = r1     // Catch: java.lang.Throwable -> L6de
            com.android.internal.protolog.ProtoLogImpl.w(r2, r3, r6, r4, r5)     // Catch: java.lang.Throwable -> L6de
        L70e:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L6de
            resetPriorityAfterLockedSection()
            return r18
        L713:
            com.android.server.wm.DisplayContent r1 = r12.getDisplayContent()     // Catch: java.lang.Throwable -> Laf3
            if (r1 != 0) goto L733
            boolean r1 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L6de
            if (r1 == 0) goto L72c
            com.android.internal.protolog.ProtoLogGroup r1 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L6de
            r2 = 1720696061(0x668fbcfd, float:3.393923E23)
            java.lang.String r3 = "Adding window to Display that has been removed."
            r4 = 0
            r5 = r4
            java.lang.Object[] r5 = (java.lang.Object[]) r5     // Catch: java.lang.Throwable -> L6de
            r5 = 0
            com.android.internal.protolog.ProtoLogImpl.w(r1, r2, r5, r3, r4)     // Catch: java.lang.Throwable -> L6de
        L72c:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L6de
            resetPriorityAfterLockedSection()
            r1 = -9
            return r1
        L733:
            r5 = 0
            com.android.server.wm.DisplayPolicy r1 = r27.getDisplayPolicy()     // Catch: java.lang.Throwable -> Laf3
            r11 = r1
            android.view.WindowManager$LayoutParams r1 = r12.mAttrs     // Catch: java.lang.Throwable -> Laf3
            r11.adjustWindowParamsLw(r12, r1)     // Catch: java.lang.Throwable -> Laf3
            int r1 = r15.flags     // Catch: java.lang.Throwable -> Laf3
            java.lang.String r2 = r12.getName()     // Catch: java.lang.Throwable -> Laf3
            r10 = r45
            r8 = r5
            r9 = r31
            int r1 = r10.sanitizeFlagSlippery(r1, r2, r14, r9)     // Catch: java.lang.Throwable -> Lae3
            r15.flags = r1     // Catch: java.lang.Throwable -> Lae3
            int r1 = r15.inputFeatures     // Catch: java.lang.Throwable -> Lae3
            java.lang.String r2 = r12.getName()     // Catch: java.lang.Throwable -> Lae3
            int r1 = r10.sanitizeSpyWindow(r1, r2, r14, r9)     // Catch: java.lang.Throwable -> Lae3
            r15.inputFeatures = r1     // Catch: java.lang.Throwable -> Lae3
            r7 = r52
            r12.setRequestedVisibilities(r7)     // Catch: java.lang.Throwable -> Lae3
            int r1 = r11.validateAddingWindowLw(r15, r9, r14)     // Catch: java.lang.Throwable -> Lae3
            r16 = r1
            if (r16 == 0) goto L77d
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L76d
            resetPriorityAfterLockedSection()
            return r16
        L76d:
            r0 = move-exception
            r5 = r54
            r4 = r55
            r17 = r9
            r2 = r10
            r27 = r14
            r1 = r35
            r21 = r44
            goto Lb7d
        L77d:
            r6 = r53
            if (r6 == 0) goto L788
            int r1 = r15.inputFeatures     // Catch: java.lang.Throwable -> L76d
            r1 = r1 & r13
            if (r1 != 0) goto L788
            r1 = r13
            goto L789
        L788:
            r1 = r8
        L789:
            r31 = r1
            if (r31 == 0) goto L790
            r12.openInputChannel(r6)     // Catch: java.lang.Throwable -> L76d
        L790:
            r5 = r44
            r1 = 2005(0x7d5, float:2.81E-42)
            if (r5 != r1) goto L80c
            r4 = r27
            boolean r1 = r4.canAddToastWindowForUid(r14)     // Catch: java.lang.Throwable -> L7fc
            if (r1 != 0) goto L7c6
            boolean r1 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L7b6
            if (r1 == 0) goto L7b1
            com.android.internal.protolog.ProtoLogGroup r1 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L7b6
            r2 = -883738232(0xffffffffcb533988, float:-1.3842824E7)
            java.lang.String r3 = "Adding more than one toast window for UID at a time."
            r13 = 0
            r17 = r13
            java.lang.Object[] r17 = (java.lang.Object[]) r17     // Catch: java.lang.Throwable -> L7b6
            com.android.internal.protolog.ProtoLogImpl.w(r1, r2, r8, r3, r13)     // Catch: java.lang.Throwable -> L7b6
        L7b1:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L7b6
            resetPriorityAfterLockedSection()
            return r29
        L7b6:
            r0 = move-exception
            r4 = r55
            r21 = r5
            r17 = r9
            r2 = r10
            r27 = r14
            r1 = r35
            r5 = r54
            goto Lb7d
        L7c6:
            if (r32 != 0) goto L7dc
            int r1 = r15.flags     // Catch: java.lang.Throwable -> L7b6
            r1 = r1 & 8
            if (r1 == 0) goto L7dc
            com.android.server.wm.WindowState r1 = r4.mCurrentFocus     // Catch: java.lang.Throwable -> L7b6
            if (r1 == 0) goto L7dc
            com.android.server.wm.WindowState r1 = r4.mCurrentFocus     // Catch: java.lang.Throwable -> L7b6
            int r1 = r1.mOwnerUid     // Catch: java.lang.Throwable -> L7b6
            if (r1 == r14) goto L7d9
            goto L7dc
        L7d9:
            r27 = r14
            goto L810
        L7dc:
            com.android.server.wm.WindowManagerService$H r1 = r10.mH     // Catch: java.lang.Throwable -> L7fc
            r2 = 52
            android.os.Message r2 = r1.obtainMessage(r2, r12)     // Catch: java.lang.Throwable -> L7fc
            android.view.WindowManager$LayoutParams r3 = r12.mAttrs     // Catch: java.lang.Throwable -> L7fc
            r27 = r14
            long r13 = r3.hideTimeoutMilliseconds     // Catch: java.lang.Throwable -> L7ee
            r1.sendMessageDelayed(r2, r13)     // Catch: java.lang.Throwable -> L7ee
            goto L810
        L7ee:
            r0 = move-exception
            r4 = r55
            r21 = r5
            r17 = r9
            r2 = r10
            r1 = r35
            r5 = r54
            goto Lb7d
        L7fc:
            r0 = move-exception
            r27 = r14
            r4 = r55
            r21 = r5
            r17 = r9
            r2 = r10
            r1 = r35
            r5 = r54
            goto Lb7d
        L80c:
            r4 = r27
            r27 = r14
        L810:
            boolean r1 = r12.isChildWindow()     // Catch: java.lang.Throwable -> Lad5
            if (r1 != 0) goto L88d
            com.android.server.wm.WindowContextListenerController r1 = r10.mWindowContextListenerController     // Catch: java.lang.Throwable -> L87f
            boolean r1 = r1.hasListener(r0)     // Catch: java.lang.Throwable -> L87f
            if (r1 == 0) goto L87c
            com.android.server.wm.WindowContextListenerController r1 = r10.mWindowContextListenerController     // Catch: java.lang.Throwable -> L87f
            int r1 = r1.getWindowType(r0)     // Catch: java.lang.Throwable -> L87f
            com.android.server.wm.WindowContextListenerController r2 = r10.mWindowContextListenerController     // Catch: java.lang.Throwable -> L87f
            android.os.Bundle r22 = r2.getOptions(r0)     // Catch: java.lang.Throwable -> L87f
            if (r5 == r1) goto L868
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_ERROR_enabled     // Catch: java.lang.Throwable -> L87f
            if (r2 == 0) goto L857
            long r2 = (long) r5
            r29 = r9
            long r8 = (long) r1
            com.android.internal.protolog.ProtoLogGroup r13 = com.android.internal.protolog.ProtoLogGroup.WM_ERROR     // Catch: java.lang.Throwable -> L8a3
            java.lang.String r14 = "Window types in WindowContext and LayoutParams.type should match! Type from LayoutParams is %d, but type from WindowContext is %d"
            r42 = r1
            r1 = 2
            java.lang.Object[] r6 = new java.lang.Object[r1]     // Catch: java.lang.Throwable -> L8a3
            java.lang.Long r1 = java.lang.Long.valueOf(r2)     // Catch: java.lang.Throwable -> L8a3
            r18 = 0
            r6[r18] = r1     // Catch: java.lang.Throwable -> L8a3
            java.lang.Long r1 = java.lang.Long.valueOf(r8)     // Catch: java.lang.Throwable -> L8a3
            r18 = 1
            r6[r18] = r1     // Catch: java.lang.Throwable -> L8a3
            r17 = r2
            r1 = 1252594551(0x4aa91377, float:5540283.5)
            r2 = 5
            com.android.internal.protolog.ProtoLogImpl.w(r13, r1, r2, r14, r6)     // Catch: java.lang.Throwable -> L8a3
            goto L85b
        L857:
            r42 = r1
            r29 = r9
        L85b:
            boolean r1 = android.window.WindowProviderService.isWindowProviderService(r22)     // Catch: java.lang.Throwable -> L8a3
            if (r1 != 0) goto L88f
            r1 = -10
            monitor-exit(r34)     // Catch: java.lang.Throwable -> L8a3
            resetPriorityAfterLockedSection()
            return r1
        L868:
            r42 = r1
            r29 = r9
            com.android.server.wm.WindowContextListenerController r1 = r10.mWindowContextListenerController     // Catch: java.lang.Throwable -> L8a3
            r17 = r1
            r18 = r0
            r19 = r40
            r20 = r27
            r21 = r5
            r17.registerWindowContainerListener(r18, r19, r20, r21, r22)     // Catch: java.lang.Throwable -> L8a3
            goto L88f
        L87c:
            r29 = r9
            goto L88f
        L87f:
            r0 = move-exception
            r4 = r55
            r21 = r5
            r17 = r9
            r2 = r10
            r1 = r35
            r5 = r54
            goto Lb7d
        L88d:
            r29 = r9
        L88f:
            r16 = 0
            boolean r1 = r10.mUseBLAST     // Catch: java.lang.Throwable -> Lac7
            if (r1 == 0) goto L899
            r1 = r16 | 8
            r16 = r1
        L899:
            com.android.server.wm.WindowState r1 = r4.mCurrentFocus     // Catch: java.lang.Throwable -> Lac7
            if (r1 != 0) goto L8b1
            java.util.ArrayList<com.android.server.wm.WindowState> r1 = r4.mWinAddedSinceNullFocus     // Catch: java.lang.Throwable -> L8a3
            r1.add(r12)     // Catch: java.lang.Throwable -> L8a3
            goto L8b1
        L8a3:
            r0 = move-exception
            r4 = r55
            r21 = r5
            r2 = r10
            r17 = r29
            r1 = r35
            r5 = r54
            goto Lb7d
        L8b1:
            boolean r1 = excludeWindowTypeFromTapOutTask(r5)     // Catch: java.lang.Throwable -> Lac7
            if (r1 == 0) goto L8bc
            java.util.ArrayList<com.android.server.wm.WindowState> r1 = r4.mTapExcludedWindows     // Catch: java.lang.Throwable -> L8a3
            r1.add(r12)     // Catch: java.lang.Throwable -> L8a3
        L8bc:
            r12.attach()     // Catch: java.lang.Throwable -> Lac7
            java.util.HashMap<android.os.IBinder, com.android.server.wm.WindowState> r1 = r10.mWindowMap     // Catch: java.lang.Throwable -> Lac7
            android.os.IBinder r2 = r47.asBinder()     // Catch: java.lang.Throwable -> Lac7
            r1.put(r2, r12)     // Catch: java.lang.Throwable -> Lac7
            com.transsion.hubcore.server.wm.ITranWindowManagerService r1 = com.transsion.hubcore.server.wm.ITranWindowManagerService.Instance()     // Catch: java.lang.Throwable -> Lac7
            r1.onWindowAdded(r12)     // Catch: java.lang.Throwable -> Lac7
            r12.initAppOpsState()     // Catch: java.lang.Throwable -> Lac7
            android.content.pm.PackageManagerInternal r1 = r10.mPmInternal     // Catch: java.lang.Throwable -> Lac7
            java.lang.String r2 = r12.getOwningPackage()     // Catch: java.lang.Throwable -> Lac7
            int r3 = r12.getOwningUid()     // Catch: java.lang.Throwable -> Lac7
            int r3 = android.os.UserHandle.getUserId(r3)     // Catch: java.lang.Throwable -> Lac7
            boolean r1 = r1.isPackageSuspended(r2, r3)     // Catch: java.lang.Throwable -> Lac7
            r13 = r1
            r12.setHiddenWhileSuspended(r13)     // Catch: java.lang.Throwable -> Lac7
            java.util.ArrayList<com.android.server.wm.WindowState> r1 = r10.mHidingNonSystemOverlayWindows     // Catch: java.lang.Throwable -> Lac7
            boolean r1 = r1.isEmpty()     // Catch: java.lang.Throwable -> Lac7
            if (r1 != 0) goto L8f2
            r8 = 1
            goto L8f3
        L8f2:
            r8 = 0
        L8f3:
            r14 = r8
            r12.setForceHideNonSystemOverlayWindowIfNeeded(r14)     // Catch: java.lang.Throwable -> Lac7
            r1 = 1
            com.android.server.wm.WindowToken r2 = r12.mToken     // Catch: java.lang.Throwable -> Lac7
            r2.addWindow(r12)     // Catch: java.lang.Throwable -> Lac7
            r11.addWindowLw(r12, r15)     // Catch: java.lang.Throwable -> Lac7
            android.view.WindowManager$LayoutParams r2 = r12.mAttrs     // Catch: java.lang.Throwable -> Lac7
            r11.setDropInputModePolicy(r12, r2)     // Catch: java.lang.Throwable -> Lac7
            r2 = 3
            if (r5 != r2) goto L943
            r9 = r43
            if (r9 == 0) goto L93d
            r9.attachStartingWindow(r12)     // Catch: java.lang.Throwable -> L8a3
            boolean r2 = com.android.server.wm.ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled     // Catch: java.lang.Throwable -> L8a3
            if (r2 == 0) goto L936
            java.lang.String r2 = java.lang.String.valueOf(r9)     // Catch: java.lang.Throwable -> L8a3
            java.lang.String r3 = java.lang.String.valueOf(r12)     // Catch: java.lang.Throwable -> L8a3
            com.android.internal.protolog.ProtoLogGroup r6 = com.android.internal.protolog.ProtoLogGroup.WM_DEBUG_STARTING_WINDOW     // Catch: java.lang.Throwable -> L8a3
            r18 = r0
            r8 = 2
            java.lang.Object[] r0 = new java.lang.Object[r8]     // Catch: java.lang.Throwable -> L8a3
            r8 = 0
            r0[r8] = r2     // Catch: java.lang.Throwable -> L8a3
            r19 = 1
            r0[r19] = r3     // Catch: java.lang.Throwable -> L8a3
            r19 = r1
            r17 = r2
            r1 = 150351993(0x8f63079, float:1.4816982E-33)
            r2 = 0
            com.android.internal.protolog.ProtoLogImpl.v(r6, r1, r8, r2, r0)     // Catch: java.lang.Throwable -> L8a3
            goto L9a4
        L936:
            r18 = r0
            r19 = r1
            r8 = 0
            goto L9a4
        L93d:
            r18 = r0
            r19 = r1
            r8 = 0
            goto L94a
        L943:
            r18 = r0
            r19 = r1
            r9 = r43
            r8 = 0
        L94a:
            r0 = 2011(0x7db, float:2.818E-42)
            if (r5 != r0) goto L95f
            android.view.WindowManager$LayoutParams r0 = r12.getAttrs()     // Catch: java.lang.Throwable -> L8a3
            int r0 = r0.flags     // Catch: java.lang.Throwable -> L8a3
            r0 = r0 & 16
            if (r0 != 0) goto L95f
            r4.setInputMethodWindowLocked(r12)     // Catch: java.lang.Throwable -> L8a3
            r1 = 0
            r19 = r1
            goto L9a4
        L95f:
            r0 = 2012(0x7dc, float:2.82E-42)
            if (r5 != r0) goto L96b
            r1 = 1
            r4.computeImeTarget(r1)     // Catch: java.lang.Throwable -> L8a3
            r1 = 0
            r19 = r1
            goto L9a4
        L96b:
            r0 = 2043(0x7fb, float:2.863E-42)
            if (r5 != r0) goto L979
            android.view.SurfaceControl$Transaction r0 = r12.getPendingTransaction()     // Catch: java.lang.Throwable -> L8a3
            android.view.SurfaceControl r1 = r12.mSurfaceControl     // Catch: java.lang.Throwable -> L8a3
            r4.reparentToOverlay(r0, r1)     // Catch: java.lang.Throwable -> L8a3
            goto L9a4
        L979:
            r0 = 2013(0x7dd, float:2.821E-42)
            if (r5 != r0) goto L989
            com.android.server.wm.WallpaperController r0 = r4.mWallpaperController     // Catch: java.lang.Throwable -> L8a3
            r0.clearLastWallpaperTimeoutTime()     // Catch: java.lang.Throwable -> L8a3
            int r0 = r4.pendingLayoutChanges     // Catch: java.lang.Throwable -> L8a3
            r0 = r0 | 4
            r4.pendingLayoutChanges = r0     // Catch: java.lang.Throwable -> L8a3
            goto L9a4
        L989:
            boolean r0 = r12.hasWallpaper()     // Catch: java.lang.Throwable -> Lac7
            if (r0 == 0) goto L996
            int r0 = r4.pendingLayoutChanges     // Catch: java.lang.Throwable -> L8a3
            r0 = r0 | 4
            r4.pendingLayoutChanges = r0     // Catch: java.lang.Throwable -> L8a3
            goto L9a4
        L996:
            com.android.server.wm.WallpaperController r0 = r4.mWallpaperController     // Catch: java.lang.Throwable -> Lac7
            boolean r0 = r0.isBelowWallpaperTarget(r12)     // Catch: java.lang.Throwable -> Lac7
            if (r0 == 0) goto L9a4
            int r0 = r4.pendingLayoutChanges     // Catch: java.lang.Throwable -> L8a3
            r0 = r0 | 4
            r4.pendingLayoutChanges = r0     // Catch: java.lang.Throwable -> L8a3
        L9a4:
            com.android.server.wm.WindowStateAnimator r0 = r12.mWinAnimator     // Catch: java.lang.Throwable -> Lac7
            r1 = 1
            r0.mEnterAnimationPending = r1     // Catch: java.lang.Throwable -> Lac7
            r0.mEnteringAnimation = r1     // Catch: java.lang.Throwable -> Lac7
            com.android.server.wm.TransitionController r1 = r12.mTransitionController     // Catch: java.lang.Throwable -> Lac7
            boolean r1 = r1.isShellTransitionsEnabled()     // Catch: java.lang.Throwable -> Lac7
            if (r1 != 0) goto L9c4
            if (r9 == 0) goto L9c4
            boolean r1 = r9.isVisible()     // Catch: java.lang.Throwable -> L8a3
            if (r1 == 0) goto L9c4
            boolean r1 = r10.prepareWindowReplacementTransition(r9)     // Catch: java.lang.Throwable -> L8a3
            if (r1 != 0) goto L9c4
            r10.prepareNoneTransitionForRelaunching(r9)     // Catch: java.lang.Throwable -> L8a3
        L9c4:
            boolean r1 = r11.areSystemBarsForcedShownLw()     // Catch: java.lang.Throwable -> Lac7
            if (r1 == 0) goto L9d4
            boolean r1 = com.android.server.wm.WindowState.isNoNeedSetAlwaysCustomSystemBars(r12, r9, r4)     // Catch: java.lang.Throwable -> L8a3
            if (r1 != 0) goto L9d4
            r1 = r16 | 4
            r16 = r1
        L9d4:
            boolean r1 = r10.mInTouchMode     // Catch: java.lang.Throwable -> Lac7
            if (r1 == 0) goto L9dc
            r1 = r16 | 1
            r16 = r1
        L9dc:
            com.android.server.wm.ActivityRecord r1 = r12.mActivityRecord     // Catch: java.lang.Throwable -> Lac7
            if (r1 == 0) goto L9e8
            com.android.server.wm.ActivityRecord r1 = r12.mActivityRecord     // Catch: java.lang.Throwable -> L8a3
            boolean r1 = r1.isClientVisible()     // Catch: java.lang.Throwable -> L8a3
            if (r1 == 0) goto L9ec
        L9e8:
            r1 = r16 | 2
            r16 = r1
        L9ec:
            com.transsion.hubcore.server.wm.ITranWindowManagerService r1 = com.transsion.hubcore.server.wm.ITranWindowManagerService.Instance()     // Catch: java.lang.Throwable -> Lac7
            r17 = 0
            r2 = r16
            r3 = r47
            r6 = r4
            r4 = r48
            r21 = r5
            r5 = r49
            r20 = r6
            r6 = r50
            r7 = r51
            r22 = r13
            r13 = r8
            r8 = r17
            r17 = r29
            r29 = r9
            r9 = r53
            r10 = r54
            r36 = r11
            r11 = r55
            int r1 = r1.checkAddWindowResultLocked(r2, r3, r4, r5, r6, r7, r8, r9, r10, r11)     // Catch: java.lang.Throwable -> Labc
            r16 = r1
            com.android.server.wm.InputMonitor r1 = r20.getInputMonitor()     // Catch: java.lang.Throwable -> Labc
            r1.setUpdateInputWindowsNeededLw()     // Catch: java.lang.Throwable -> Labc
            r1 = 0
            boolean r2 = r12.canReceiveKeys()     // Catch: java.lang.Throwable -> Labc
            if (r2 == 0) goto La38
            r3 = 1
            r2 = r45
            boolean r4 = r2.updateFocusedWindowLocked(r3, r13)     // Catch: java.lang.Throwable -> La35
            r1 = r4
            if (r1 == 0) goto La3a
            r19 = 0
            goto La3a
        La35:
            r0 = move-exception
            goto Labf
        La38:
            r2 = r45
        La3a:
            if (r19 == 0) goto La43
            r3 = r20
            r4 = 1
            r3.computeImeTarget(r4)     // Catch: java.lang.Throwable -> La35
            goto La45
        La43:
            r3 = r20
        La45:
            com.android.server.wm.WindowContainer r4 = r12.getParent()     // Catch: java.lang.Throwable -> La35
            r4.assignChildLayers()     // Catch: java.lang.Throwable -> La35
            if (r1 == 0) goto La57
            com.android.server.wm.InputMonitor r4 = r3.getInputMonitor()     // Catch: java.lang.Throwable -> La35
            com.android.server.wm.WindowState r5 = r3.mCurrentFocus     // Catch: java.lang.Throwable -> La35
            r4.setInputFocusLw(r5, r13)     // Catch: java.lang.Throwable -> La35
        La57:
            com.android.server.wm.InputMonitor r4 = r3.getInputMonitor()     // Catch: java.lang.Throwable -> La35
            r4.updateInputWindowsLw(r13)     // Catch: java.lang.Throwable -> La35
            boolean r4 = com.android.server.wm.ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled     // Catch: java.lang.Throwable -> La35
            if (r4 == 0) goto La8b
            android.os.IBinder r4 = r47.asBinder()     // Catch: java.lang.Throwable -> La35
            java.lang.String r4 = java.lang.String.valueOf(r4)     // Catch: java.lang.Throwable -> La35
            java.lang.String r5 = java.lang.String.valueOf(r12)     // Catch: java.lang.Throwable -> La35
            r6 = 5
            java.lang.String r6 = android.os.Debug.getCallers(r6)     // Catch: java.lang.Throwable -> La35
            java.lang.String r6 = java.lang.String.valueOf(r6)     // Catch: java.lang.Throwable -> La35
            com.android.internal.protolog.ProtoLogGroup r7 = com.android.internal.protolog.ProtoLogGroup.WM_DEBUG_ADD_REMOVE     // Catch: java.lang.Throwable -> La35
            r8 = -1427184084(0xffffffffaaeee62c, float:-4.2437007E-13)
            r9 = 3
            java.lang.Object[] r9 = new java.lang.Object[r9]     // Catch: java.lang.Throwable -> La35
            r9[r13] = r4     // Catch: java.lang.Throwable -> La35
            r10 = 1
            r9[r10] = r5     // Catch: java.lang.Throwable -> La35
            r10 = 2
            r9[r10] = r6     // Catch: java.lang.Throwable -> La35
            r10 = 0
            com.android.internal.protolog.ProtoLogImpl.v(r7, r8, r13, r10, r9)     // Catch: java.lang.Throwable -> La35
        La8b:
            boolean r4 = r12.isVisibleRequestedOrAdding()     // Catch: java.lang.Throwable -> La35
            if (r4 == 0) goto La9a
            boolean r4 = r3.updateOrientation()     // Catch: java.lang.Throwable -> La35
            if (r4 == 0) goto La9a
            r3.sendNewConfiguration()     // Catch: java.lang.Throwable -> La35
        La9a:
            com.android.server.wm.InsetsStateController r4 = r3.getInsetsStateController()     // Catch: java.lang.Throwable -> La35
            r4.updateAboveInsetsState(r13)     // Catch: java.lang.Throwable -> La35
            android.view.InsetsState r4 = r12.getCompatInsetsState()     // Catch: java.lang.Throwable -> La35
            r5 = r54
            r6 = 1
            r5.set(r4, r6)     // Catch: java.lang.Throwable -> Laba
            r4 = r55
            r2.getInsetsSourceControls(r12, r4)     // Catch: java.lang.Throwable -> Lab8
            monitor-exit(r34)     // Catch: java.lang.Throwable -> Lab8
            resetPriorityAfterLockedSection()
            android.os.Binder.restoreCallingIdentity(r23)
            return r16
        Lab8:
            r0 = move-exception
            goto Lac3
        Laba:
            r0 = move-exception
            goto Lac1
        Labc:
            r0 = move-exception
            r2 = r45
        Labf:
            r5 = r54
        Lac1:
            r4 = r55
        Lac3:
            r1 = r35
            goto Lb7d
        Lac7:
            r0 = move-exception
            r4 = r55
            r21 = r5
            r2 = r10
            r17 = r29
            r5 = r54
            r1 = r35
            goto Lb7d
        Lad5:
            r0 = move-exception
            r4 = r55
            r21 = r5
            r17 = r9
            r2 = r10
            r5 = r54
            r1 = r35
            goto Lb7d
        Lae3:
            r0 = move-exception
            r5 = r54
            r4 = r55
            r17 = r9
            r2 = r10
            r27 = r14
            r21 = r44
            r1 = r35
            goto Lb7d
        Laf3:
            r0 = move-exception
            r2 = r45
            r5 = r54
            r4 = r55
            r27 = r14
            r17 = r31
            r21 = r44
            r1 = r35
            goto Lb7d
        Lb04:
            r0 = move-exception
            r5 = r54
            r35 = r8
            r4 = r9
            r21 = r12
            r2 = r13
            r17 = r31
            r1 = r35
            goto Lb7d
        Lb13:
            r0 = move-exception
            r5 = r54
            r35 = r7
            r4 = r9
            r2 = r13
            r17 = r31
            r21 = r37
            r1 = r35
            goto Lb7d
        Lb22:
            r0 = move-exception
            r35 = r4
            r34 = r5
            r21 = r7
            r4 = r9
            r2 = r13
            r38 = r17
            r33 = r18
            r17 = r26
            r5 = r54
            r1 = r35
            goto Lb7d
        Lb36:
            r0 = move-exception
            r34 = r5
            r21 = r7
            r4 = r9
            r2 = r13
            r38 = r17
            r33 = r18
            r17 = r26
            r5 = r54
            r1 = r20
            goto Lb7d
        Lb48:
            r20 = r1
            r27 = r3
            r28 = r4
            r34 = r5
            r21 = r7
            r4 = r9
            r38 = r17
            r33 = r18
            r5 = r54
            r17 = r2
            r2 = r13
            java.lang.IllegalStateException r0 = new java.lang.IllegalStateException     // Catch: java.lang.Throwable -> Lb64
            java.lang.String r1 = "Display has not been initialialized"
            r0.<init>(r1)     // Catch: java.lang.Throwable -> Lb64
            throw r0     // Catch: java.lang.Throwable -> Lb64
        Lb64:
            r0 = move-exception
            r1 = r20
            goto Lb7d
        Lb68:
            r0 = move-exception
            r20 = r1
            r27 = r3
            r28 = r4
            r34 = r5
            r21 = r7
            r4 = r9
            r38 = r17
            r33 = r18
            r5 = r54
            r17 = r2
            r2 = r13
        Lb7d:
            monitor-exit(r34)     // Catch: java.lang.Throwable -> Lb82
            resetPriorityAfterLockedSection()
            throw r0
        Lb82:
            r0 = move-exception
            goto Lb7d
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.WindowManagerService.addWindow(com.android.server.wm.Session, android.view.IWindow, android.view.WindowManager$LayoutParams, int, int, int, android.view.InsetsVisibilities, android.view.InputChannel, android.view.InsetsState, android.view.InsetsSourceControl[]):int");
    }

    private boolean unprivilegedAppCanCreateTokenWith(WindowState parentWindow, int callingUid, int type, int rootType, IBinder tokenForLog, String packageName) {
        if (rootType >= 1 && rootType <= 99) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam0 = String.valueOf(tokenForLog);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1113134997, 0, "Attempted to add application window with unknown token %s.  Aborting.", new Object[]{protoLogParam0});
            }
            return false;
        } else if (rootType == 2011) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam02 = String.valueOf(tokenForLog);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -2039580386, 0, "Attempted to add input method window with unknown token %s.  Aborting.", new Object[]{protoLogParam02});
            }
            return false;
        } else if (rootType == 2031) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam03 = String.valueOf(tokenForLog);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -914253865, 0, "Attempted to add voice interaction window with unknown token %s.  Aborting.", new Object[]{protoLogParam03});
            }
            return false;
        } else if (rootType == 2013) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam04 = String.valueOf(tokenForLog);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 424524729, 0, "Attempted to add wallpaper window with unknown token %s.  Aborting.", new Object[]{protoLogParam04});
            }
            return false;
        } else if (rootType == 2035) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam05 = String.valueOf(tokenForLog);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 898863925, 0, "Attempted to add QS dialog window with unknown token %s.  Aborting.", new Object[]{protoLogParam05});
            }
            return false;
        } else if (rootType == 2032) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam06 = String.valueOf(tokenForLog);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1042574499, 0, "Attempted to add Accessibility overlay window with unknown token %s.  Aborting.", new Object[]{protoLogParam06});
            }
            return false;
        } else if (type != 2005 || !doesAddToastWindowRequireToken(packageName, callingUid, parentWindow)) {
            return true;
        } else {
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam07 = String.valueOf(tokenForLog);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1331177619, 0, "Attempted to add a toast window with unknown token %s.  Aborting.", new Object[]{protoLogParam07});
            }
            return false;
        }
    }

    private DisplayContent getDisplayContentOrCreate(int displayId, IBinder token) {
        WindowToken wToken;
        if (token != null && (wToken = this.mRoot.getWindowToken(token)) != null) {
            return wToken.getDisplayContent();
        }
        return this.mRoot.getDisplayContentOrCreate(displayId);
    }

    private boolean doesAddToastWindowRequireToken(String packageName, int callingUid, WindowState attachedWindow) {
        ApplicationInfo appInfo;
        if (attachedWindow != null) {
            return attachedWindow.mActivityRecord != null && attachedWindow.mActivityRecord.mTargetSdk >= 26;
        }
        try {
            appInfo = this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, 0, UserHandle.getUserId(callingUid));
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (appInfo.uid == callingUid) {
            return appInfo.targetSdkVersion >= 26;
        }
        throw new SecurityException("Package " + packageName + " not in UID " + callingUid);
    }

    private boolean prepareWindowReplacementTransition(ActivityRecord activity) {
        activity.clearAllDrawn();
        WindowState replacedWindow = activity.getReplacingWindow();
        if (replacedWindow == null) {
            return false;
        }
        Rect frame = new Rect(replacedWindow.getFrame());
        WindowManager.LayoutParams attrs = replacedWindow.mAttrs;
        frame.inset(replacedWindow.getInsetsStateWithVisibilityOverride().calculateVisibleInsets(frame, attrs.type, replacedWindow.getWindowingMode(), attrs.softInputMode, attrs.flags));
        DisplayContent dc = activity.getDisplayContent();
        dc.mOpeningApps.add(activity);
        dc.prepareAppTransition(5);
        dc.mAppTransition.overridePendingAppTransitionClipReveal(frame.left, frame.top, frame.width(), frame.height());
        dc.executeAppTransition();
        return true;
    }

    private void prepareNoneTransitionForRelaunching(ActivityRecord activity) {
        DisplayContent dc = activity.getDisplayContent();
        if (this.mDisplayFrozen && !dc.mOpeningApps.contains(activity) && activity.isRelaunching()) {
            dc.mOpeningApps.add(activity);
            dc.prepareAppTransition(0);
            dc.executeAppTransition();
        }
    }

    public void refreshScreenCaptureDisabled() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            throw new SecurityException("Only system can call refreshScreenCaptureDisabled.");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.refreshSecureSurfaceState();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeWindow(Session session, IWindow client) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, client, false);
                if (win != null) {
                    win.removeIfPossible();
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mEmbeddedWindowController.remove(client);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postWindowRemoveCleanupLocked(WindowState win) {
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(win);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -622997754, 0, (String) null, new Object[]{protoLogParam0});
        }
        this.mWindowMap.remove(win.mClient.asBinder());
        ITranWindowManagerService.Instance().onWindowRemoved(win);
        DisplayContent dc = win.getDisplayContent();
        dc.getDisplayRotation().markForSeamlessRotation(win, false);
        win.resetAppOpsState();
        if (dc.mCurrentFocus == null) {
            dc.mWinRemovedSinceNullFocus.add(win);
        }
        this.mEmbeddedWindowController.onWindowRemoved(win);
        this.mResizingWindows.remove(win);
        updateNonSystemOverlayWindowsVisibilityIfNeeded(win, false);
        this.mWindowsChanged = true;
        if (ProtoLogCache.WM_DEBUG_WINDOW_MOVEMENT_enabled) {
            String protoLogParam02 = String.valueOf(win);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_WINDOW_MOVEMENT, -193782861, 0, (String) null, new Object[]{protoLogParam02});
        }
        DisplayContent displayContent = win.getDisplayContent();
        if (displayContent.mInputMethodWindow == win) {
            displayContent.setInputMethodWindowLocked(null);
        }
        WindowToken token = win.mToken;
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam03 = String.valueOf(win);
            String protoLogParam1 = String.valueOf(token);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -1963461591, 0, (String) null, new Object[]{protoLogParam03, protoLogParam1});
        }
        if (token.isEmpty() && !token.mPersistOnEmpty) {
            token.removeImmediately();
        }
        if (win.mActivityRecord != null) {
            win.mActivityRecord.postWindowRemoveStartingWindowCleanup(win);
        }
        if (win.mAttrs.type == 2013) {
            dc.mWallpaperController.clearLastWallpaperTimeoutTime();
            dc.pendingLayoutChanges |= 4;
        } else if (win.hasWallpaper()) {
            dc.pendingLayoutChanges |= 4;
        }
        if (dc != null && !this.mWindowPlacerLocked.isInLayout()) {
            dc.assignWindowLayers(true);
            if (getFocusedWindow() == win) {
                this.mFocusMayChange = true;
            }
            this.mWindowPlacerLocked.performSurfacePlacement();
            if (win.mActivityRecord != null) {
                win.mActivityRecord.updateReportedVisibilityLocked();
            }
        }
        dc.getInputMonitor().updateInputWindowsLw(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateHiddenWhileSuspendedState(ArraySet<String> packages, boolean suspended) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.updateHiddenWhileSuspendedState(packages, suspended);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAppOpsState() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.updateAppOpsState();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void logSurface(WindowState w, String msg, boolean withStackTrace) {
        String str = "  SURFACE " + msg + ": " + w;
        if (withStackTrace) {
            logWithStack("WindowManager", str);
        } else {
            Slog.i("WindowManager", str);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void logWithStack(String tag, String s) {
        RuntimeException e = null;
        if (WindowManagerDebugConfig.SHOW_STACK_CRAWLS) {
            e = new RuntimeException();
            e.fillInStackTrace();
        }
        Slog.i(tag, s, e);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearTouchableRegion(Session session, IWindow client) {
        Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                WindowState w = windowForClientLocked(session, client, false);
                w.clearClientTouchableRegion();
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInsetsWindow(Session session, IWindow client, int touchableInsets, Rect contentInsets, Rect visibleInsets, Region touchableRegion) {
        int uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                WindowState w = windowForClientLocked(session, client, false);
                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                    Slog.d("WindowManager", "setInsetsWindow " + w + ", contentInsets=" + w.mGivenContentInsets + " -> " + contentInsets + ", visibleInsets=" + w.mGivenVisibleInsets + " -> " + visibleInsets + ", touchableRegion=" + w.mGivenTouchableRegion + " -> " + touchableRegion + ", touchableInsets " + w.mTouchableInsets + " -> " + touchableInsets);
                }
                if (w != null) {
                    w.mGivenInsetsPending = false;
                    w.mGivenContentInsets.set(contentInsets);
                    w.mGivenVisibleInsets.set(visibleInsets);
                    w.mGivenTouchableRegion.set(touchableRegion);
                    w.mTouchableInsets = touchableInsets;
                    if (w.mGlobalScale != 1.0f) {
                        w.mGivenContentInsets.scale(w.mGlobalScale);
                        w.mGivenVisibleInsets.scale(w.mGlobalScale);
                        w.mGivenTouchableRegion.scale(w.mGlobalScale);
                    }
                    w.setDisplayLayoutNeeded();
                    w.updateSourceFrame(w.getFrame());
                    this.mWindowPlacerLocked.performSurfacePlacement();
                    w.getDisplayContent().getInputMonitor().updateInputWindowsLw(true);
                    if (this.mAccessibilityController.hasCallbacks()) {
                        this.mAccessibilityController.onSomeWindowResizedOrMovedWithCallingUid(uid, w.getDisplayContent().getDisplayId());
                    }
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void onRectangleOnScreenRequested(IBinder token, Rect rectangle) {
        WindowState window;
        AccessibilityController.AccessibilityControllerInternalImpl a11yControllerInternal = AccessibilityController.getAccessibilityControllerInternal(this);
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (a11yControllerInternal.hasWindowManagerEventDispatcher() && (window = this.mWindowMap.get(token)) != null) {
                    a11yControllerInternal.onRectangleOnScreenRequested(window.getDisplayId(), rectangle);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public IWindowId getWindowId(IBinder token) {
        WindowState.WindowId windowId;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState window = this.mWindowMap.get(token);
                windowId = window != null ? window.mWindowId : null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return windowId;
    }

    public void pokeDrawLock(Session session, IBinder token) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState window = windowForClientLocked(session, token, false);
                if (window != null) {
                    window.pokeDrawLockLw(this.mDrawLockTimeoutMillis);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    private boolean hasStatusBarPermission(int pid, int uid) {
        return this.mContext.checkPermission("android.permission.STATUS_BAR", pid, uid) == 0;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2728=15] */
    /* JADX WARN: Not initialized variable reg: 30, insn: 0x020b: MOVE  (r29 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r30 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY] A[D('pid' int)]), block:B:89:0x0202 */
    /* JADX WARN: Removed duplicated region for block: B:160:0x0383  */
    /* JADX WARN: Removed duplicated region for block: B:161:0x0385  */
    /* JADX WARN: Removed duplicated region for block: B:170:0x03ea  */
    /* JADX WARN: Removed duplicated region for block: B:173:0x03f5  */
    /* JADX WARN: Removed duplicated region for block: B:174:0x03f7  */
    /* JADX WARN: Removed duplicated region for block: B:222:0x04e5  */
    /* JADX WARN: Removed duplicated region for block: B:225:0x04f5 A[Catch: all -> 0x07db, TRY_LEAVE, TryCatch #32 {all -> 0x07db, blocks: (B:204:0x0469, B:223:0x04eb, B:225:0x04f5, B:240:0x0533, B:242:0x0540, B:244:0x0546, B:250:0x0595, B:245:0x0555, B:247:0x0559, B:249:0x0591), top: B:396:0x0469 }] */
    /* JADX WARN: Removed duplicated region for block: B:240:0x0533 A[Catch: all -> 0x07db, TRY_ENTER, TryCatch #32 {all -> 0x07db, blocks: (B:204:0x0469, B:223:0x04eb, B:225:0x04f5, B:240:0x0533, B:242:0x0540, B:244:0x0546, B:250:0x0595, B:245:0x0555, B:247:0x0559, B:249:0x0591), top: B:396:0x0469 }] */
    /* JADX WARN: Removed duplicated region for block: B:260:0x05ae  */
    /* JADX WARN: Removed duplicated region for block: B:261:0x05b0  */
    /* JADX WARN: Removed duplicated region for block: B:264:0x05b4 A[Catch: all -> 0x0526, TryCatch #3 {all -> 0x0526, blocks: (B:199:0x0456, B:230:0x0509, B:232:0x0511, B:234:0x0515, B:236:0x051c, B:254:0x05a0, B:258:0x05aa, B:264:0x05b4, B:266:0x05ba, B:268:0x05c0, B:269:0x05c6, B:271:0x05ca, B:272:0x05d1, B:274:0x05e2, B:276:0x05e6, B:277:0x05ec, B:279:0x05f0, B:280:0x05f5, B:282:0x05fb, B:284:0x0603, B:285:0x0607, B:287:0x060d), top: B:386:0x0456 }] */
    /* JADX WARN: Removed duplicated region for block: B:268:0x05c0 A[Catch: all -> 0x0526, TryCatch #3 {all -> 0x0526, blocks: (B:199:0x0456, B:230:0x0509, B:232:0x0511, B:234:0x0515, B:236:0x051c, B:254:0x05a0, B:258:0x05aa, B:264:0x05b4, B:266:0x05ba, B:268:0x05c0, B:269:0x05c6, B:271:0x05ca, B:272:0x05d1, B:274:0x05e2, B:276:0x05e6, B:277:0x05ec, B:279:0x05f0, B:280:0x05f5, B:282:0x05fb, B:284:0x0603, B:285:0x0607, B:287:0x060d), top: B:386:0x0456 }] */
    /* JADX WARN: Removed duplicated region for block: B:271:0x05ca A[Catch: all -> 0x0526, TryCatch #3 {all -> 0x0526, blocks: (B:199:0x0456, B:230:0x0509, B:232:0x0511, B:234:0x0515, B:236:0x051c, B:254:0x05a0, B:258:0x05aa, B:264:0x05b4, B:266:0x05ba, B:268:0x05c0, B:269:0x05c6, B:271:0x05ca, B:272:0x05d1, B:274:0x05e2, B:276:0x05e6, B:277:0x05ec, B:279:0x05f0, B:280:0x05f5, B:282:0x05fb, B:284:0x0603, B:285:0x0607, B:287:0x060d), top: B:386:0x0456 }] */
    /* JADX WARN: Removed duplicated region for block: B:279:0x05f0 A[Catch: all -> 0x0526, TryCatch #3 {all -> 0x0526, blocks: (B:199:0x0456, B:230:0x0509, B:232:0x0511, B:234:0x0515, B:236:0x051c, B:254:0x05a0, B:258:0x05aa, B:264:0x05b4, B:266:0x05ba, B:268:0x05c0, B:269:0x05c6, B:271:0x05ca, B:272:0x05d1, B:274:0x05e2, B:276:0x05e6, B:277:0x05ec, B:279:0x05f0, B:280:0x05f5, B:282:0x05fb, B:284:0x0603, B:285:0x0607, B:287:0x060d), top: B:386:0x0456 }] */
    /* JADX WARN: Removed duplicated region for block: B:287:0x060d A[Catch: all -> 0x0526, TRY_LEAVE, TryCatch #3 {all -> 0x0526, blocks: (B:199:0x0456, B:230:0x0509, B:232:0x0511, B:234:0x0515, B:236:0x051c, B:254:0x05a0, B:258:0x05aa, B:264:0x05b4, B:266:0x05ba, B:268:0x05c0, B:269:0x05c6, B:271:0x05ca, B:272:0x05d1, B:274:0x05e2, B:276:0x05e6, B:277:0x05ec, B:279:0x05f0, B:280:0x05f5, B:282:0x05fb, B:284:0x0603, B:285:0x0607, B:287:0x060d), top: B:386:0x0456 }] */
    /* JADX WARN: Removed duplicated region for block: B:289:0x0611  */
    /* JADX WARN: Removed duplicated region for block: B:309:0x0699  */
    /* JADX WARN: Removed duplicated region for block: B:313:0x06cf  */
    /* JADX WARN: Removed duplicated region for block: B:316:0x06e0 A[Catch: all -> 0x0795, TryCatch #27 {all -> 0x0795, blocks: (B:312:0x06a5, B:314:0x06dc, B:316:0x06e0, B:318:0x06fe, B:320:0x0702), top: B:432:0x06a5 }] */
    /* JADX WARN: Removed duplicated region for block: B:317:0x06fd  */
    /* JADX WARN: Removed duplicated region for block: B:320:0x0702 A[Catch: all -> 0x0795, TRY_LEAVE, TryCatch #27 {all -> 0x0795, blocks: (B:312:0x06a5, B:314:0x06dc, B:316:0x06e0, B:318:0x06fe, B:320:0x0702), top: B:432:0x06a5 }] */
    /* JADX WARN: Removed duplicated region for block: B:323:0x0729  */
    /* JADX WARN: Removed duplicated region for block: B:330:0x073f A[Catch: all -> 0x0751, TRY_LEAVE, TryCatch #24 {all -> 0x0751, blocks: (B:328:0x0739, B:330:0x073f), top: B:426:0x0739 }] */
    /* JADX WARN: Removed duplicated region for block: B:337:0x0765 A[Catch: all -> 0x0785, TRY_LEAVE, TryCatch #17 {all -> 0x0785, blocks: (B:332:0x074d, B:337:0x0765, B:335:0x075a), top: B:412:0x0733 }] */
    /* JADX WARN: Removed duplicated region for block: B:394:0x03ac A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:396:0x0469 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:422:0x062e A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:432:0x06a5 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int relayoutWindow(Session session, IWindow client, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight, int viewVisibility, int flags, ClientWindowFrames outFrames, MergedConfiguration mergedConfiguration, SurfaceControl outSurfaceControl, InsetsState outInsetsState, InsetsSourceControl[] outActiveControls, Bundle outSyncIdBundle) {
        WindowManagerGlobalLock windowManagerGlobalLock;
        int pid;
        DisplayContent displayContent;
        DisplayPolicy displayPolicy;
        WindowStateAnimator winAnimator;
        int attrChanges;
        int privateFlagChanges;
        boolean configChanged;
        int flagChanges;
        int flagChanges2;
        int attrChanges2;
        DisplayPolicy displayPolicy2;
        WindowState win;
        boolean z;
        boolean wallpaperMayMove;
        boolean imMayMove;
        int attrChanges3;
        boolean shouldRelayout;
        WindowManagerService windowManagerService;
        int result;
        int result2;
        boolean focusMayChange;
        int i;
        int result3;
        boolean imMayMove2;
        boolean imMayMove3;
        boolean toBeDisplayed;
        boolean z2;
        int i2;
        DisplayContent displayContent2;
        char c;
        WindowState win2;
        boolean z3;
        WindowState win3;
        boolean configChanged2;
        int flagChanges3;
        int attrChanges4;
        Arrays.fill(outActiveControls, (Object) null);
        int pid2 = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        WindowManagerGlobalLock windowManagerGlobalLock2 = this.mGlobalLock;
        synchronized (windowManagerGlobalLock2) {
            try {
                boostPriorityForLockedSection();
            } catch (Throwable th) {
                th = th;
                windowManagerGlobalLock = windowManagerGlobalLock2;
            }
            try {
                WindowState win4 = windowForClientLocked(session, client, false);
                try {
                    if (win4 == null) {
                        resetPriorityAfterLockedSection();
                        return 0;
                    }
                    DisplayContent displayContent3 = win4.getDisplayContent();
                    DisplayPolicy displayPolicy3 = displayContent3.getDisplayPolicy();
                    WindowStateAnimator winAnimator2 = win4.mWinAnimator;
                    if (viewVisibility != 8) {
                        win4.setRequestedSize(requestedWidth, requestedHeight);
                    }
                    if (attrs != null) {
                        displayPolicy = displayPolicy3;
                        try {
                            displayPolicy.adjustWindowParamsLw(win4, attrs);
                            try {
                                attrs.flags = sanitizeFlagSlippery(attrs.flags, win4.getName(), uid, pid2);
                                attrs.inputFeatures = sanitizeSpyWindow(attrs.inputFeatures, win4.getName(), uid, pid2);
                                int disableFlags = (attrs.systemUiVisibility | attrs.subtreeSystemUiVisibility) & 134152192;
                                if (disableFlags != 0) {
                                    try {
                                        if (!hasStatusBarPermission(pid2, uid)) {
                                            disableFlags = 0;
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        windowManagerGlobalLock = windowManagerGlobalLock2;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th3) {
                                                th = th3;
                                            }
                                        }
                                        resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                                win4.mDisableFlags = disableFlags;
                                try {
                                    if (win4.mAttrs.type != attrs.type) {
                                        this.mWindowManagerDebugger.debugRelayoutWindow("WindowManager", win4, win4.mAttrs.type, attrs.type);
                                        throw new IllegalArgumentException("Window type can not be changed after the window is added.");
                                    } else if (!Arrays.equals(win4.mAttrs.providesInsetsTypes, attrs.providesInsetsTypes)) {
                                        throw new IllegalArgumentException("Insets types can not be changed after the window is added.");
                                    } else {
                                        int flagChanges4 = win4.mAttrs.flags ^ attrs.flags;
                                        int privateFlagChanges2 = win4.mAttrs.privateFlags ^ attrs.privateFlags;
                                        int attrChanges5 = win4.mAttrs.copyFrom(attrs);
                                        pid = pid2;
                                        if ((attrChanges5 & 16385) != 0) {
                                            try {
                                                win4.mLayoutNeeded = true;
                                                configChanged2 = displayPolicy.updateScreenByDecorIfNeeded(win4);
                                            } catch (Throwable th4) {
                                                th = th4;
                                                windowManagerGlobalLock = windowManagerGlobalLock2;
                                                while (true) {
                                                    break;
                                                    break;
                                                }
                                                resetPriorityAfterLockedSection();
                                                throw th;
                                            }
                                        } else {
                                            configChanged2 = false;
                                        }
                                        configChanged = configChanged2;
                                        try {
                                            if (win4.mActivityRecord != null && ((flagChanges4 & 524288) != 0 || (4194304 & flagChanges4) != 0)) {
                                                try {
                                                    win4.mActivityRecord.checkKeyguardFlagsChanged();
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                    windowManagerGlobalLock = windowManagerGlobalLock2;
                                                    while (true) {
                                                        break;
                                                        break;
                                                    }
                                                    resetPriorityAfterLockedSection();
                                                    throw th;
                                                }
                                            }
                                            if ((33554432 & attrChanges5) != 0 && this.mAccessibilityController.hasCallbacks()) {
                                                this.mAccessibilityController.onSomeWindowResizedOrMovedWithCallingUid(uid, win4.getDisplayContent().getDisplayId());
                                            }
                                            if ((privateFlagChanges2 & 524288) != 0) {
                                                updateNonSystemOverlayWindowsVisibilityIfNeeded(win4, win4.mWinAnimator.getShown());
                                            }
                                            if ((131072 & attrChanges5) != 0) {
                                                winAnimator = winAnimator2;
                                                winAnimator.setColorSpaceAgnosticLocked((win4.mAttrs.privateFlags & 16777216) != 0);
                                            } else {
                                                winAnimator = winAnimator2;
                                            }
                                            if (win4.mActivityRecord != null) {
                                                displayContent = displayContent3;
                                                attrChanges4 = attrChanges5;
                                                if (!displayContent.mDwpcHelper.keepActivityOnWindowFlagsChanged(win4.mActivityRecord.info, flagChanges4, privateFlagChanges2)) {
                                                    H h = this.mH;
                                                    h.sendMessage(h.obtainMessage(65, win4.mActivityRecord.getTask()));
                                                    Slog.w("WindowManager", "Activity " + win4.mActivityRecord + " window flag changed, can't remain on display " + displayContent.getDisplayId());
                                                    resetPriorityAfterLockedSection();
                                                    return 0;
                                                }
                                                flagChanges3 = flagChanges4;
                                            } else {
                                                flagChanges3 = flagChanges4;
                                                displayContent = displayContent3;
                                                attrChanges4 = attrChanges5;
                                            }
                                            attrChanges = attrChanges4;
                                            privateFlagChanges = flagChanges3;
                                        } catch (Throwable th6) {
                                            th = th6;
                                            windowManagerGlobalLock = windowManagerGlobalLock2;
                                        }
                                    }
                                } catch (Throwable th7) {
                                    th = th7;
                                    windowManagerGlobalLock = windowManagerGlobalLock2;
                                }
                            } catch (Throwable th8) {
                                th = th8;
                                windowManagerGlobalLock = windowManagerGlobalLock2;
                            }
                        } catch (Throwable th9) {
                            th = th9;
                            windowManagerGlobalLock = windowManagerGlobalLock2;
                        }
                    } else {
                        pid = pid2;
                        displayContent = displayContent3;
                        displayPolicy = displayPolicy3;
                        winAnimator = winAnimator2;
                        attrChanges = 0;
                        privateFlagChanges = 0;
                        configChanged = false;
                    }
                    try {
                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                            flagChanges = privateFlagChanges;
                            Slog.v("WindowManager", "Relayout " + win4 + ": viewVisibility=" + viewVisibility + " req=" + requestedWidth + "x" + requestedHeight + " " + win4.mAttrs);
                        } else {
                            flagChanges = privateFlagChanges;
                        }
                        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                            this.mWindowManagerDebugger.debugInputAttr("WindowManager", attrs);
                        }
                        if (this.mWmsExt.isAppResolutionTunerSupport()) {
                            try {
                                flagChanges2 = flagChanges;
                                attrChanges2 = attrChanges;
                                displayPolicy2 = displayPolicy;
                                win = win4;
                                windowManagerGlobalLock = windowManagerGlobalLock2;
                            } catch (Throwable th10) {
                                th = th10;
                                windowManagerGlobalLock = windowManagerGlobalLock2;
                                while (true) {
                                    break;
                                    break;
                                }
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                            try {
                                this.mWmsExt.setWindowScaleByWL(win4, win4.getDisplayInfo(), win4.mAttrs, requestedWidth, requestedHeight);
                            } catch (Throwable th11) {
                                th = th11;
                                while (true) {
                                    break;
                                    break;
                                }
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                        } else {
                            attrChanges2 = attrChanges;
                            windowManagerGlobalLock = windowManagerGlobalLock2;
                            flagChanges2 = flagChanges;
                            displayPolicy2 = displayPolicy;
                            win = win4;
                        }
                        if ((attrChanges2 & 128) != 0) {
                            winAnimator.mAlpha = attrs.alpha;
                        }
                        try {
                            win.setWindowScale(win.mRequestedWidth, win.mRequestedHeight);
                            if (win.mAttrs.surfaceInsets.left != 0 || win.mAttrs.surfaceInsets.top != 0 || win.mAttrs.surfaceInsets.right != 0 || win.mAttrs.surfaceInsets.bottom != 0) {
                                winAnimator.setOpaqueLocked(false);
                            }
                            int oldVisibility = win.mViewVisibility;
                            boolean becameVisible = (oldVisibility == 4 || oldVisibility == 8) && viewVisibility == 0;
                            try {
                                try {
                                    try {
                                        try {
                                            if ((131080 & flagChanges2) == 0 && !becameVisible) {
                                                z = false;
                                                boolean imMayMove4 = z;
                                                boolean focusMayChange2 = (win.mViewVisibility != viewVisibility && (flagChanges2 & 8) == 0 && win.mRelayoutCalled) ? false : true;
                                                wallpaperMayMove = (win.mViewVisibility == viewVisibility && win.hasWallpaper()) | ((1048576 & flagChanges2) == 0);
                                                if ((flagChanges2 & 8192) != 0 && winAnimator.mSurfaceController != null) {
                                                    winAnimator.mSurfaceController.setSecure(win.isSecureLocked());
                                                }
                                                TranFoldWMCustody.instance().prepareRelayout(win);
                                                win.mRelayoutCalled = true;
                                                win.mInRelayout = true;
                                                win.setViewVisibility(viewVisibility);
                                                if (ProtoLogCache.WM_DEBUG_SCREEN_ON_enabled) {
                                                    imMayMove = imMayMove4;
                                                    attrChanges3 = attrChanges2;
                                                } else {
                                                    try {
                                                        String protoLogParam0 = String.valueOf(win);
                                                        long protoLogParam1 = oldVisibility;
                                                        long protoLogParam2 = viewVisibility;
                                                        String protoLogParam3 = String.valueOf(new RuntimeException().fillInStackTrace());
                                                        imMayMove = imMayMove4;
                                                        attrChanges3 = attrChanges2;
                                                        ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_SCREEN_ON, -754503024, 20, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2), protoLogParam3});
                                                    } catch (Throwable th12) {
                                                        th = th12;
                                                        while (true) {
                                                            break;
                                                            break;
                                                        }
                                                        resetPriorityAfterLockedSection();
                                                        throw th;
                                                    }
                                                }
                                                win.setDisplayLayoutNeeded();
                                                win.mGivenInsetsPending = (flags & 1) == 0;
                                                shouldRelayout = viewVisibility != 0 && (win.mActivityRecord == null || win.mAttrs.type == 3 || win.mActivityRecord.isClientVisible());
                                                if (shouldRelayout && winAnimator.hasSurface() && !win.mAnimatingExit) {
                                                    if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                                        Slog.i("WindowManager", "Relayout invis " + win + ": mAnimatingExit=" + win.mAnimatingExit);
                                                    }
                                                    int result4 = 0 | 2;
                                                    if (win.mWillReplaceWindow) {
                                                        windowManagerService = this;
                                                        result = result4;
                                                    } else {
                                                        if (wallpaperMayMove) {
                                                            displayContent.mWallpaperController.adjustWallpaperWindows();
                                                        }
                                                        windowManagerService = this;
                                                        try {
                                                            focusMayChange2 = windowManagerService.tryStartExitingAnimation(win, winAnimator, focusMayChange2);
                                                            result = result4;
                                                        } catch (Throwable th13) {
                                                            th = th13;
                                                            while (true) {
                                                                break;
                                                                break;
                                                            }
                                                            resetPriorityAfterLockedSection();
                                                            throw th;
                                                        }
                                                    }
                                                } else {
                                                    windowManagerService = this;
                                                    result = 0;
                                                }
                                                if (shouldRelayout) {
                                                    focusMayChange = focusMayChange2;
                                                    i = 2;
                                                } else {
                                                    try {
                                                        try {
                                                            result = windowManagerService.createSurfaceControl(outSurfaceControl, result, win, winAnimator);
                                                            focusMayChange = focusMayChange2;
                                                            i = 2;
                                                        } catch (Exception e) {
                                                            try {
                                                                displayContent.getInputMonitor().updateInputWindowsLw(true);
                                                                if (ProtoLogCache.WM_ERROR_enabled) {
                                                                    String protoLogParam02 = String.valueOf(client);
                                                                    String protoLogParam12 = String.valueOf(win.mAttrs.getTitle());
                                                                    String protoLogParam22 = String.valueOf(e);
                                                                    result2 = result;
                                                                    try {
                                                                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1750206390, 0, "Exception thrown when creating surface for client %s (%s). %s", new Object[]{protoLogParam02, protoLogParam12, protoLogParam22});
                                                                    } catch (Throwable th14) {
                                                                        th = th14;
                                                                        while (true) {
                                                                            break;
                                                                            break;
                                                                        }
                                                                        resetPriorityAfterLockedSection();
                                                                        throw th;
                                                                    }
                                                                } else {
                                                                    result2 = result;
                                                                }
                                                                Binder.restoreCallingIdentity(origId);
                                                                resetPriorityAfterLockedSection();
                                                                return 0;
                                                            } catch (Throwable th15) {
                                                                th = th15;
                                                            }
                                                        }
                                                    } catch (Throwable th16) {
                                                        th = th16;
                                                        while (true) {
                                                            break;
                                                            break;
                                                        }
                                                        resetPriorityAfterLockedSection();
                                                        throw th;
                                                    }
                                                }
                                                windowManagerService.mWindowPlacerLocked.performSurfacePlacement(true);
                                                if (shouldRelayout) {
                                                    Trace.traceBegin(32L, "relayoutWindow: viewVisibility_2");
                                                    winAnimator.mEnterAnimationPending = false;
                                                    winAnimator.mEnteringAnimation = false;
                                                    if (viewVisibility == 0 && winAnimator.hasSurface()) {
                                                        Trace.traceBegin(32L, "relayoutWindow: getSurface");
                                                        winAnimator.mSurfaceController.getSurfaceControl(outSurfaceControl);
                                                        Trace.traceEnd(32L);
                                                    } else {
                                                        if (WindowManagerDebugConfig.DEBUG_VISIBILITY) {
                                                            Slog.i("WindowManager", "Releasing surface in: " + win);
                                                        }
                                                        try {
                                                            Trace.traceBegin(32L, "wmReleaseOutSurface_" + ((Object) win.mAttrs.getTitle()));
                                                            outSurfaceControl.release();
                                                            Trace.traceEnd(32L);
                                                        } catch (Throwable th17) {
                                                            try {
                                                                Trace.traceEnd(32L);
                                                                throw th17;
                                                            } catch (Throwable th18) {
                                                                th = th18;
                                                                while (true) {
                                                                    break;
                                                                    break;
                                                                }
                                                                resetPriorityAfterLockedSection();
                                                                throw th;
                                                            }
                                                        }
                                                    }
                                                    Trace.traceEnd(32L);
                                                    result3 = result;
                                                    imMayMove2 = imMayMove;
                                                } else {
                                                    Trace.traceBegin(32L, "relayoutWindow: viewVisibility_1");
                                                    result3 = win.relayoutVisibleWindow(result);
                                                    boolean focusMayChange3 = (result3 & 1) != 0 ? true : focusMayChange;
                                                    if (win.mAttrs.type == 2011 && displayContent.mInputMethodWindow == null) {
                                                        displayContent.setInputMethodWindowLocked(win);
                                                        imMayMove2 = true;
                                                    } else {
                                                        imMayMove2 = imMayMove;
                                                    }
                                                    win.adjustStartingWindowFlags();
                                                    Trace.traceEnd(32L);
                                                    focusMayChange = focusMayChange3;
                                                }
                                                imMayMove3 = (focusMayChange || !windowManagerService.updateFocusedWindowLocked(0, true)) ? imMayMove2 : false;
                                                toBeDisplayed = (result3 & 1) == 0;
                                                if (imMayMove3) {
                                                    displayContent.computeImeTarget(true);
                                                    if (toBeDisplayed) {
                                                        displayContent.assignWindowLayers(false);
                                                    }
                                                }
                                                if (wallpaperMayMove) {
                                                    displayContent.pendingLayoutChanges |= 4;
                                                }
                                                if (win.mActivityRecord != null) {
                                                    displayContent.mUnknownAppVisibilityController.notifyRelayouted(win.mActivityRecord);
                                                }
                                                Trace.traceBegin(32L, "relayoutWindow: updateOrientation");
                                                configChanged |= displayContent.updateOrientation();
                                                Trace.traceEnd(32L);
                                                if (toBeDisplayed && win.mIsWallpaper) {
                                                    displayContent.mWallpaperController.updateWallpaperOffset(win, false);
                                                }
                                                if (win.mActivityRecord != null) {
                                                    win.mActivityRecord.updateReportedVisibilityLocked();
                                                }
                                                if (displayPolicy2.areSystemBarsForcedShownLw() && !WindowState.isNoNeedSetAlwaysCustomSystemBars(win, win.mActivityRecord, displayContent)) {
                                                    result3 |= 8;
                                                }
                                                if (win.isGoneForLayout()) {
                                                    z2 = false;
                                                    win.mResizedWhileGone = false;
                                                } else {
                                                    z2 = false;
                                                }
                                                win.fillClientWindowFramesAndConfiguration(outFrames, mergedConfiguration, z2, shouldRelayout);
                                                win.onResizeHandled();
                                                outInsetsState.set(win.getCompatInsetsState(), true);
                                                if (!WindowManagerDebugConfig.DEBUG) {
                                                    try {
                                                        try {
                                                            try {
                                                                Slog.v("WindowManager", "Relayout given client " + client.asBinder() + ", requestedWidth=" + requestedWidth + ", requestedHeight=" + requestedHeight + ", viewVisibility=" + viewVisibility + "\nRelayout returning frame=" + outFrames.frame + ", surface=" + outSurfaceControl);
                                                            } catch (Throwable th19) {
                                                                th = th19;
                                                                while (true) {
                                                                    break;
                                                                    break;
                                                                }
                                                                resetPriorityAfterLockedSection();
                                                                throw th;
                                                            }
                                                        } catch (Throwable th20) {
                                                            th = th20;
                                                            while (true) {
                                                                break;
                                                                break;
                                                            }
                                                            resetPriorityAfterLockedSection();
                                                            throw th;
                                                        }
                                                    } catch (Throwable th21) {
                                                        th = th21;
                                                    }
                                                }
                                                if (WindowManagerDebugger.WMS_DEBUG_USER) {
                                                    i2 = i;
                                                    c = 1;
                                                    displayContent2 = displayContent;
                                                    win2 = win;
                                                } else {
                                                    try {
                                                        i2 = i;
                                                        displayContent2 = displayContent;
                                                        c = 1;
                                                        win2 = win;
                                                        windowManagerService.mWindowManagerDebugger.debugViewVisibility("WindowManager", win, viewVisibility, oldVisibility, focusMayChange, requestedWidth, requestedHeight, outFrames, outSurfaceControl);
                                                    } catch (Throwable th22) {
                                                        th = th22;
                                                        while (true) {
                                                            break;
                                                            break;
                                                        }
                                                        resetPriorityAfterLockedSection();
                                                        throw th;
                                                    }
                                                }
                                                if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
                                                    z3 = false;
                                                } else {
                                                    String protoLogParam03 = String.valueOf(win2);
                                                    boolean protoLogParam13 = focusMayChange;
                                                    ProtoLogGroup protoLogGroup = ProtoLogGroup.WM_DEBUG_FOCUS;
                                                    Object[] objArr = new Object[i2];
                                                    z3 = false;
                                                    objArr[0] = protoLogParam03;
                                                    objArr[c] = Boolean.valueOf(protoLogParam13);
                                                    ProtoLogImpl.v(protoLogGroup, 95902367, 12, (String) null, objArr);
                                                }
                                                if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                                                    win3 = win2;
                                                } else {
                                                    win3 = win2;
                                                    try {
                                                        Slog.v("WindowManager", "Relayout complete " + win3 + ": outFrames=" + outFrames);
                                                    } catch (Throwable th23) {
                                                        th = th23;
                                                        while (true) {
                                                            break;
                                                            break;
                                                        }
                                                        resetPriorityAfterLockedSection();
                                                        throw th;
                                                    }
                                                }
                                                win3.mInRelayout = z3;
                                                if (win3.useBLASTSync() && viewVisibility != 8) {
                                                    try {
                                                        if (win3.mSyncSeqId > win3.mLastSeqIdSentToRelayout) {
                                                            win3.markRedrawForSyncReported();
                                                            win3.mLastSeqIdSentToRelayout = win3.mSyncSeqId;
                                                            outSyncIdBundle.putInt("seqid", win3.mSyncSeqId);
                                                            if (configChanged) {
                                                                Trace.traceBegin(32L, "relayoutWindow: postNewConfigurationToHandler");
                                                                displayContent2.sendNewConfiguration();
                                                                Trace.traceEnd(32L);
                                                            }
                                                            windowManagerService.getInsetsSourceControls(win3, outActiveControls);
                                                            resetPriorityAfterLockedSection();
                                                            Binder.restoreCallingIdentity(origId);
                                                            return result3;
                                                        }
                                                    } catch (Throwable th24) {
                                                        th = th24;
                                                        while (true) {
                                                            break;
                                                            break;
                                                        }
                                                        resetPriorityAfterLockedSection();
                                                        throw th;
                                                    }
                                                }
                                                outSyncIdBundle.putInt("seqid", -1);
                                                if (configChanged) {
                                                }
                                                windowManagerService.getInsetsSourceControls(win3, outActiveControls);
                                                resetPriorityAfterLockedSection();
                                                Binder.restoreCallingIdentity(origId);
                                                return result3;
                                            }
                                            windowManagerService.getInsetsSourceControls(win3, outActiveControls);
                                            resetPriorityAfterLockedSection();
                                            Binder.restoreCallingIdentity(origId);
                                            return result3;
                                        } catch (Throwable th25) {
                                            th = th25;
                                            while (true) {
                                                break;
                                                break;
                                            }
                                            resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                        if (win3.useBLASTSync()) {
                                            if (win3.mSyncSeqId > win3.mLastSeqIdSentToRelayout) {
                                            }
                                        }
                                        outSyncIdBundle.putInt("seqid", -1);
                                        if (configChanged) {
                                        }
                                    } catch (Throwable th26) {
                                        th = th26;
                                    }
                                    outInsetsState.set(win.getCompatInsetsState(), true);
                                    if (!WindowManagerDebugConfig.DEBUG) {
                                    }
                                    if (WindowManagerDebugger.WMS_DEBUG_USER) {
                                    }
                                    if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
                                    }
                                    if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                                    }
                                    win3.mInRelayout = z3;
                                } catch (Throwable th27) {
                                    th = th27;
                                }
                                win.fillClientWindowFramesAndConfiguration(outFrames, mergedConfiguration, z2, shouldRelayout);
                                win.onResizeHandled();
                            } catch (Throwable th28) {
                                th = th28;
                            }
                            z = true;
                            boolean imMayMove42 = z;
                            boolean focusMayChange22 = (win.mViewVisibility != viewVisibility && (flagChanges2 & 8) == 0 && win.mRelayoutCalled) ? false : true;
                            wallpaperMayMove = (win.mViewVisibility == viewVisibility && win.hasWallpaper()) | ((1048576 & flagChanges2) == 0);
                            if ((flagChanges2 & 8192) != 0) {
                                winAnimator.mSurfaceController.setSecure(win.isSecureLocked());
                            }
                            TranFoldWMCustody.instance().prepareRelayout(win);
                            win.mRelayoutCalled = true;
                            win.mInRelayout = true;
                            win.setViewVisibility(viewVisibility);
                            if (ProtoLogCache.WM_DEBUG_SCREEN_ON_enabled) {
                            }
                            win.setDisplayLayoutNeeded();
                            win.mGivenInsetsPending = (flags & 1) == 0;
                            shouldRelayout = viewVisibility != 0 && (win.mActivityRecord == null || win.mAttrs.type == 3 || win.mActivityRecord.isClientVisible());
                            if (shouldRelayout) {
                            }
                            windowManagerService = this;
                            result = 0;
                            if (shouldRelayout) {
                            }
                            windowManagerService.mWindowPlacerLocked.performSurfacePlacement(true);
                            if (shouldRelayout) {
                            }
                            if (focusMayChange) {
                            }
                            toBeDisplayed = (result3 & 1) == 0;
                            if (imMayMove3) {
                            }
                            if (wallpaperMayMove) {
                            }
                            if (win.mActivityRecord != null) {
                            }
                            Trace.traceBegin(32L, "relayoutWindow: updateOrientation");
                            configChanged |= displayContent.updateOrientation();
                            Trace.traceEnd(32L);
                            if (toBeDisplayed) {
                                displayContent.mWallpaperController.updateWallpaperOffset(win, false);
                            }
                            if (win.mActivityRecord != null) {
                            }
                            if (displayPolicy2.areSystemBarsForcedShownLw()) {
                                result3 |= 8;
                            }
                            if (win.isGoneForLayout()) {
                            }
                        } catch (Throwable th29) {
                            th = th29;
                        }
                    } catch (Throwable th30) {
                        th = th30;
                        windowManagerGlobalLock = windowManagerGlobalLock2;
                    }
                } catch (Throwable th31) {
                    th = th31;
                    windowManagerGlobalLock = windowManagerGlobalLock2;
                }
            } catch (Throwable th32) {
                th = th32;
                windowManagerGlobalLock = windowManagerGlobalLock2;
                while (true) {
                    break;
                    break;
                }
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void getInsetsSourceControls(WindowState win, InsetsSourceControl[] outControls) {
        InsetsSourceControl[] controls = win.getDisplayContent().getInsetsStateController().getControlsForDispatch(win);
        if (controls != null) {
            int length = Math.min(controls.length, outControls.length);
            for (int i = 0; i < length; i++) {
                if (controls[i] != null) {
                    outControls[i] = new InsetsSourceControl(controls[i]);
                    outControls[i].setParcelableFlags(1);
                }
            }
        }
    }

    private boolean tryStartExitingAnimation(WindowState win, WindowStateAnimator winAnimator, boolean focusMayChange) {
        int transit = 2;
        if (win.mAttrs.type == 3) {
            transit = 5;
        }
        String reason = null;
        if (win.isWinVisibleLw() && winAnimator.applyAnimationLocked(transit, false)) {
            reason = "applyAnimation";
            focusMayChange = true;
            win.mAnimatingExit = true;
        } else if (win.mDisplayContent.okToAnimate() && win.isExitAnimationRunningSelfOrParent()) {
            win.mAnimatingExit = true;
        } else if (win.mDisplayContent.okToAnimate() && win.mDisplayContent.mWallpaperController.isWallpaperTarget(win) && win.mAttrs.type != 2040) {
            reason = "isWallpaperTarget";
            win.mAnimatingExit = true;
        } else {
            boolean stopped = win.mActivityRecord == null || win.mActivityRecord.mAppStopped;
            win.mDestroying = true;
            win.destroySurface(false, stopped);
        }
        if (reason != null && ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(reason);
            String protoLogParam1 = String.valueOf(win);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ANIM, 2075693141, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        if (this.mAccessibilityController.hasCallbacks()) {
            this.mAccessibilityController.onWindowTransition(win, transit);
        }
        return focusMayChange;
    }

    private int createSurfaceControl(SurfaceControl outSurfaceControl, int result, WindowState win, WindowStateAnimator winAnimator) {
        if (!win.mHasSurface) {
            result |= 2;
        }
        try {
            Trace.traceBegin(32L, "createSurfaceControl");
            WindowSurfaceController surfaceController = winAnimator.createSurfaceLocked();
            Trace.traceEnd(32L);
            if (surfaceController != null) {
                surfaceController.getSurfaceControl(outSurfaceControl);
                if (ProtoLogCache.WM_SHOW_TRANSACTIONS_enabled) {
                    String protoLogParam0 = String.valueOf(outSurfaceControl);
                    ProtoLogImpl.i(ProtoLogGroup.WM_SHOW_TRANSACTIONS, -1257821162, 0, (String) null, new Object[]{protoLogParam0});
                }
            } else {
                if (ProtoLogCache.WM_ERROR_enabled) {
                    String protoLogParam02 = String.valueOf(win);
                    ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 704998117, 0, "Failed to create surface control for %s", new Object[]{protoLogParam02});
                }
                outSurfaceControl.release();
            }
            return result;
        } catch (Throwable th) {
            Trace.traceEnd(32L);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int updateViewVisibility(Session session, IWindow client, WindowManager.LayoutParams attrs, int viewVisibility, MergedConfiguration outMergedConfiguration, SurfaceControl outSurfaceControl, InsetsState outInsetsState, InsetsSourceControl[] outActiveControls) {
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateWindowLayout(Session session, IWindow client, WindowManager.LayoutParams attrs, int flags, ClientWindowFrames clientWindowFrames, int requestedWidth, int requestedHeight) {
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, client, false);
                if (win == null) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                win.setFrames(clientWindowFrames, requestedWidth, requestedHeight);
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean outOfMemoryWindow(Session session, IWindow client) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, client, false);
                if (win != null) {
                    boolean reclaimSomeSurfaceMemory = this.mRoot.reclaimSomeSurfaceMemory(win.mWinAnimator, "from-client", false);
                    resetPriorityAfterLockedSection();
                    return reclaimSomeSurfaceMemory;
                }
                resetPriorityAfterLockedSection();
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishDrawingWindow(Session session, IWindow client, SurfaceControl.Transaction postDrawTransaction, int seqId) {
        if (postDrawTransaction != null) {
            postDrawTransaction.sanitize();
        }
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            try {
                                WindowState win = windowForClientLocked(session, client, false);
                                if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                                    String protoLogParam0 = String.valueOf(win);
                                    String protoLogParam1 = String.valueOf(win != null ? win.mWinAnimator.drawStateToString() : "null");
                                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 1112047265, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
                                }
                                if (Build.IS_DEBUG_ENABLE) {
                                    Slog.d("WindowManager", "System monitor finishDrawingWindow w:  " + win + "; mDrawState = " + (win != null ? win.mWinAnimator.drawStateToString() : "null"));
                                }
                                if (win != null && win.finishDrawing(postDrawTransaction, seqId)) {
                                    if (win.hasWallpaper()) {
                                        win.getDisplayContent().pendingLayoutChanges |= 4;
                                    }
                                    win.setDisplayLayoutNeeded();
                                    this.mWindowPlacerLocked.requestTraversal();
                                }
                                resetPriorityAfterLockedSection();
                                Binder.restoreCallingIdentity(origId);
                            } catch (Throwable th) {
                                th = th;
                                resetPriorityAfterLockedSection();
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    Binder.restoreCallingIdentity(origId);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (Throwable th5) {
            th = th5;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkCallingPermission(String permission, String func) {
        return checkCallingPermission(permission, func, true);
    }

    boolean checkCallingPermission(String permission, String func, boolean printLog) {
        if (Binder.getCallingPid() == Process.myPid() || this.mContext.checkCallingPermission(permission) == 0) {
            return true;
        }
        if (printLog && ProtoLogCache.WM_ERROR_enabled) {
            String protoLogParam0 = String.valueOf(func);
            long protoLogParam1 = Binder.getCallingPid();
            long protoLogParam2 = Binder.getCallingUid();
            String protoLogParam3 = String.valueOf(permission);
            ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1563755163, 20, "Permission Denial: %s from pid=%d, uid=%d requires %s", new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2), protoLogParam3});
        }
        return false;
    }

    public void addWindowToken(IBinder binder, int type, int displayId, Bundle options) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "addWindowToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent dc = getDisplayContentOrCreate(displayId, null);
                    if (dc == null) {
                        if (ProtoLogCache.WM_ERROR_enabled) {
                            String protoLogParam0 = String.valueOf(binder);
                            long protoLogParam1 = displayId;
                            ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1208313423, 4, "addWindowToken: Attempted to add token: %s for non-exiting displayId=%d", new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
                        }
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    WindowToken token = dc.getWindowToken(binder);
                    if (token != null) {
                        if (ProtoLogCache.WM_ERROR_enabled) {
                            String protoLogParam02 = String.valueOf(binder);
                            String protoLogParam12 = String.valueOf(token);
                            long protoLogParam2 = displayId;
                            ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 254883724, 16, "addWindowToken: Attempted to add binder token: %s for already created window token: %s displayId=%d", new Object[]{protoLogParam02, protoLogParam12, Long.valueOf(protoLogParam2)});
                        }
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    if (type == 2013) {
                        new WallpaperWindowToken(this, binder, true, dc, true, options);
                    } else {
                        new WindowToken.Builder(this, binder, type).setDisplayContent(dc).setPersistOnEmpty(true).setOwnerCanManageAppTokens(true).setOptions(options).build();
                    }
                    resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    th = th;
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2983=4] */
    public Configuration attachWindowContextToDisplayArea(IBinder clientToken, int type, int displayId, Bundle options) {
        if (clientToken == null) {
            throw new IllegalArgumentException("clientToken must not be null!");
        }
        boolean callerCanManageAppTokens = checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "attachWindowContextToDisplayArea", false);
        int callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        DisplayContent dc = this.mRoot.getDisplayContentOrCreate(displayId);
                        if (dc == null) {
                            if (ProtoLogCache.WM_ERROR_enabled) {
                                long protoLogParam0 = displayId;
                                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 666937535, 1, "attachWindowContextToDisplayArea: trying to attach to a non-existing display:%d", new Object[]{Long.valueOf(protoLogParam0)});
                            }
                            resetPriorityAfterLockedSection();
                            Binder.restoreCallingIdentity(origId);
                            return null;
                        }
                        DisplayArea<?> da = dc.findAreaForWindowType(type, options, callerCanManageAppTokens, false);
                        this.mWindowContextListenerController.registerWindowContainerListener(clientToken, da, callingUid, type, options, false);
                        Configuration configuration = da.getConfiguration();
                        resetPriorityAfterLockedSection();
                        Binder.restoreCallingIdentity(origId);
                        return configuration;
                    } catch (Throwable th) {
                        th = th;
                        try {
                            resetPriorityAfterLockedSection();
                            throw th;
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(origId);
                            throw th;
                        }
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3019=4] */
    public void attachWindowContextToWindowToken(IBinder clientToken, IBinder token) {
        boolean callerCanManageAppTokens = checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "attachWindowContextToWindowToken", false);
        int callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                WindowToken windowToken = this.mRoot.getWindowToken(token);
                if (windowToken == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        String protoLogParam0 = String.valueOf(token);
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1789321832, 0, "Then token:%s is invalid. It might be removed", new Object[]{protoLogParam0});
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                int type = this.mWindowContextListenerController.getWindowType(clientToken);
                if (type == -1) {
                    throw new IllegalArgumentException("The clientToken:" + clientToken + " should have been attached.");
                }
                if (type != windowToken.windowType) {
                    throw new IllegalArgumentException("The WindowToken's type should match the created WindowContext's type. WindowToken's type is " + windowToken.windowType + ", while WindowContext's is " + type);
                }
                if (!this.mWindowContextListenerController.assertCallerCanModifyListener(clientToken, callerCanManageAppTokens, callingUid)) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mWindowContextListenerController.registerWindowContainerListener(clientToken, windowToken, callingUid, windowToken.windowType, windowToken.mOptions);
                resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void detachWindowContextFromWindowContainer(IBinder clientToken) {
        boolean callerCanManageAppTokens = checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "detachWindowContextFromWindowContainer", false);
        int callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                if (this.mWindowContextListenerController.assertCallerCanModifyListener(clientToken, callerCanManageAppTokens, callingUid)) {
                    WindowContainer wc = this.mWindowContextListenerController.getContainer(clientToken);
                    this.mWindowContextListenerController.unregisterWindowContainerListener(clientToken);
                    WindowToken token = wc.asWindowToken();
                    if (token != null && token.isFromClient()) {
                        removeWindowToken(token.token, token.getDisplayContent().getDisplayId());
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public Configuration attachToDisplayContent(IBinder clientToken, int displayId) {
        if (clientToken == null) {
            throw new IllegalArgumentException("clientToken must not be null!");
        }
        int callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    if (Binder.getCallingPid() != Process.myPid()) {
                        throw new WindowManager.InvalidDisplayException("attachToDisplayContent: trying to attach to a non-existing display:" + displayId);
                    }
                    resetPriorityAfterLockedSection();
                    return null;
                }
                this.mWindowContextListenerController.registerWindowContainerListener(clientToken, dc, callingUid, -1, null, false);
                Configuration configuration = dc.getConfiguration();
                resetPriorityAfterLockedSection();
                return configuration;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean isWindowToken(IBinder binder) {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                z = this.mRoot.getWindowToken(binder) != null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return z;
    }

    void removeWindowToken(IBinder binder, boolean removeWindows, boolean animateExit, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                try {
                    boostPriorityForLockedSection();
                    DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                    if (dc == null) {
                        if (ProtoLogCache.WM_ERROR_enabled) {
                            String protoLogParam0 = String.valueOf(binder);
                            long protoLogParam1 = displayId;
                            ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1739298851, 4, "removeWindowToken: Attempted to remove token: %s for non-exiting displayId=%d", new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
                        }
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    WindowToken token = dc.removeWindowToken(binder, animateExit);
                    if (token == null) {
                        if (ProtoLogCache.WM_ERROR_enabled) {
                            String protoLogParam02 = String.valueOf(binder);
                            ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1518495446, 0, "removeWindowToken: Attempted to remove non-existing token: %s", new Object[]{protoLogParam02});
                        }
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    if (removeWindows) {
                        token.removeAllWindowsIfPossible();
                    }
                    dc.getInputMonitor().updateInputWindowsLw(true);
                    resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    th = th;
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void removeWindowToken(IBinder binder, int displayId) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "removeWindowToken()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            removeWindowToken(binder, false, true, displayId);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void moveWindowTokenToDisplay(IBinder binder, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContentOrCreate(displayId);
                if (dc == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        String protoLogParam0 = String.valueOf(binder);
                        long protoLogParam1 = displayId;
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 2060978050, 4, "moveWindowTokenToDisplay: Attempted to move token: %s to non-exiting displayId=%d", new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                WindowToken token = this.mRoot.getWindowToken(binder);
                if (token == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        String protoLogParam02 = String.valueOf(binder);
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1033274509, 0, "moveWindowTokenToDisplay: Attempted to move non-existing token: %s", new Object[]{protoLogParam02});
                    }
                    resetPriorityAfterLockedSection();
                } else if (token.getDisplayContent() == dc) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        String protoLogParam03 = String.valueOf(binder);
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 781471998, 0, "moveWindowTokenToDisplay: Cannot move to the original display for token: %s", new Object[]{protoLogParam03});
                    }
                    resetPriorityAfterLockedSection();
                } else {
                    dc.reParentWindowToken(token);
                    resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public String getTopFocusedStackPackage() {
        Task rootTask;
        ActivityRecord topRecord;
        String retPkg = null;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                ActivityTaskManagerService activityTaskManagerService = this.mAtmService;
                if (activityTaskManagerService != null && (rootTask = activityTaskManagerService.getTopDisplayFocusedRootTask()) != null && (topRecord = rootTask.topRunningActivity()) != null) {
                    retPkg = topRecord.packageName;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return retPkg;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prepareAppTransitionNone() {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "prepareAppTransition()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        getDefaultDisplayContentLocked().prepareAppTransition(0);
    }

    public void overridePendingAppTransitionMultiThumbFuture(IAppTransitionAnimationSpecsFuture specsFuture, IRemoteCallback callback, boolean scaleUp, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    Slog.w("WindowManager", "Attempted to call overridePendingAppTransitionMultiThumbFuture for the display " + displayId + " that does not exist.");
                    resetPriorityAfterLockedSection();
                    return;
                }
                displayContent.mAppTransition.overridePendingAppTransitionMultiThumbFuture(specsFuture, callback, scaleUp);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void overridePendingAppTransitionRemote(RemoteAnimationAdapter remoteAnimationAdapter, int displayId) {
        if (!checkCallingPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", "overridePendingAppTransitionRemote()")) {
            throw new SecurityException("Requires CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    Slog.w("WindowManager", "Attempted to call overridePendingAppTransitionRemote for the display " + displayId + " that does not exist.");
                    resetPriorityAfterLockedSection();
                    return;
                }
                remoteAnimationAdapter.setCallingPidUid(Binder.getCallingPid(), Binder.getCallingUid());
                displayContent.mAppTransition.overridePendingAppTransitionRemote(remoteAnimationAdapter);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void endProlongedAnimations() {
    }

    public void executeAppTransition() {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "executeAppTransition()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        getDefaultDisplayContentLocked().executeAppTransition();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initializeRecentsAnimation(int targetActivityType, IRecentsAnimationRunner recentsAnimationRunner, RecentsAnimationController.RecentsAnimationCallbacks callbacks, int displayId, SparseBooleanArray recentTaskIds, ActivityRecord targetActivity) {
        this.mRecentsAnimationController = new RecentsAnimationController(this, recentsAnimationRunner, callbacks, displayId);
        this.mRoot.getDisplayContent(displayId).mAppTransition.updateBooster();
        this.mRecentsAnimationController.initialize(targetActivityType, recentTaskIds, targetActivity);
    }

    void setRecentsAnimationController(RecentsAnimationController controller) {
        this.mRecentsAnimationController = controller;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RecentsAnimationController getRecentsAnimationController() {
        return this.mRecentsAnimationController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelRecentsAnimation(int reorderMode, String reason) {
        RecentsAnimationController recentsAnimationController = this.mRecentsAnimationController;
        if (recentsAnimationController != null) {
            recentsAnimationController.cancelAnimation(reorderMode, reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanupRecentsAnimation(int reorderMode) {
        if (this.mRecentsAnimationController != null) {
            RecentsAnimationController controller = this.mRecentsAnimationController;
            this.mRecentsAnimationController = null;
            controller.cleanupAnimation(reorderMode);
            DisplayContent dc = getDefaultDisplayContentLocked();
            if (reorderMode != 2 && dc.mAppTransition.getFirstAppTransition() != 1) {
                if (dc.mAppTransition.isTransitionSet()) {
                    dc.mSkipAppTransitionAnimation = true;
                }
                dc.forAllWindowContainers(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda6
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        WindowManagerService.lambda$cleanupRecentsAnimation$2((WindowContainer) obj);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$cleanupRecentsAnimation$2(WindowContainer wc) {
        if (wc.isAnimating(1, 1)) {
            wc.cancelAnimation();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRecentsAnimationTarget(ActivityRecord r) {
        RecentsAnimationController recentsAnimationController = this.mRecentsAnimationController;
        return recentsAnimationController != null && recentsAnimationController.isTargetApp(r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowOpaqueLocked(IBinder token, boolean isOpaque) {
        ActivityRecord wtoken = this.mRoot.getActivityRecord(token);
        if (wtoken != null) {
            wtoken.setMainWindowOpaque(isOpaque);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isValidPictureInPictureAspectRatio(DisplayContent displayContent, float aspectRatio) {
        return displayContent.getPinnedTaskController().isValidPictureInPictureAspectRatio(aspectRatio);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isValidExpandedPictureInPictureAspectRatio(DisplayContent displayContent, float aspectRatio) {
        return displayContent.getPinnedTaskController().isValidExpandedPictureInPictureAspectRatio(aspectRatio);
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void notifyKeyguardTrustedChanged() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mAtmService.mKeyguardController.isKeyguardShowing(0)) {
                    this.mRoot.ensureActivitiesVisible(null, 0, false);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void screenTurningOff(int displayId, WindowManagerPolicy.ScreenOffListener listener, boolean isTransition) {
        this.mTaskSnapshotController.screenTurningOff(displayId, listener, isTransition);
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void triggerAnimationFailsafe() {
        this.mH.sendEmptyMessage(60);
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void onKeyguardShowingAndNotOccludedChanged() {
        this.mH.sendEmptyMessage(61);
        dispatchKeyguardLockedState();
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void onPowerKeyDown(boolean isScreenOn) {
        Consumer<DisplayPolicy> obtainConsumer = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda19
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((DisplayPolicy) obj).onPowerKeyDown(((Boolean) obj2).booleanValue());
            }
        }, PooledLambda.__(), Boolean.valueOf(isScreenOn));
        this.mRoot.forAllDisplayPolicies(obtainConsumer);
        obtainConsumer.recycle();
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void onUserSwitched() {
        this.mSettingsObserver.updateSystemUiSettings(true);
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.forAllDisplayPolicies(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda20
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((DisplayPolicy) obj).resetSystemBarAttributes();
                    }
                });
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void moveDisplayToTop(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null && this.mRoot.getTopChild() != displayContent) {
                    displayContent.getParent().positionChildAt(Integer.MAX_VALUE, displayContent, true);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        syncInputTransactions(true);
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public boolean isAppTransitionStateIdle() {
        return getDefaultDisplayContentLocked().mAppTransition.isIdle();
    }

    public void startFreezingScreen(int exitAnim, int enterAnim) {
        if (!checkCallingPermission("android.permission.FREEZE_SCREEN", "startFreezingScreen()")) {
            throw new SecurityException("Requires FREEZE_SCREEN permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (!this.mClientFreezingScreen) {
                    this.mClientFreezingScreen = true;
                    long origId = Binder.clearCallingIdentity();
                    startFreezingDisplay(exitAnim, enterAnim);
                    this.mH.removeMessages(30);
                    this.mH.sendEmptyMessageDelayed(30, 5000L);
                    Binder.restoreCallingIdentity(origId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void stopFreezingScreen() {
        if (!checkCallingPermission("android.permission.FREEZE_SCREEN", "stopFreezingScreen()")) {
            throw new SecurityException("Requires FREEZE_SCREEN permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mClientFreezingScreen) {
                    this.mClientFreezingScreen = false;
                    this.mLastFinishedFreezeSource = "client";
                    long origId = Binder.clearCallingIdentity();
                    stopFreezingDisplayLocked();
                    Binder.restoreCallingIdentity(origId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void disableKeyguard(final IBinder token, final String tag, int userId) {
        final int userId2 = this.mAmInternal.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "disableKeyguard", (String) null);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        final int callingUid = Binder.getCallingUid();
        long origIdentity = Binder.clearCallingIdentity();
        try {
            if (!ITranWindowManagerService.Instance().onDisableKeyguard(token, tag, callingUid, userId2, new Runnable() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda17
                @Override // java.lang.Runnable
                public final void run() {
                    WindowManagerService.this.m8505x40abfc73(token, tag, callingUid, userId2);
                }
            })) {
                try {
                    this.mKeyguardDisableHandler.disableKeyguard(token, tag, callingUid, userId2);
                    Binder.restoreCallingIdentity(origIdentity);
                    return;
                } catch (Throwable th) {
                    th = th;
                    Binder.restoreCallingIdentity(origIdentity);
                    throw th;
                }
            }
            Binder.restoreCallingIdentity(origIdentity);
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$disableKeyguard$3$com-android-server-wm-WindowManagerService  reason: not valid java name */
    public /* synthetic */ void m8505x40abfc73(IBinder token, String tag, int callingUid, int fUserId) {
        this.mKeyguardDisableHandler.disableKeyguard(token, tag, callingUid, fUserId);
    }

    public void reenableKeyguard(IBinder token, int userId) {
        int userId2 = this.mAmInternal.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 2, "reenableKeyguard", (String) null);
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        Objects.requireNonNull(token, "token is null");
        int callingUid = Binder.getCallingUid();
        long origIdentity = Binder.clearCallingIdentity();
        try {
            this.mKeyguardDisableHandler.reenableKeyguard(token, callingUid, userId2);
        } finally {
            Binder.restoreCallingIdentity(origIdentity);
        }
    }

    public void exitKeyguardSecurely(final IOnKeyguardExitResult callback) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DISABLE_KEYGUARD") != 0) {
            throw new SecurityException("Requires DISABLE_KEYGUARD permission");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback == null");
        }
        this.mPolicy.exitKeyguardSecurely(new WindowManagerPolicy.OnKeyguardExitResult() { // from class: com.android.server.wm.WindowManagerService.10
            @Override // com.android.server.policy.WindowManagerPolicy.OnKeyguardExitResult
            public void onKeyguardExitResult(boolean success) {
                try {
                    callback.onKeyguardExitResult(success);
                } catch (RemoteException e) {
                }
            }
        });
    }

    public boolean isKeyguardLocked() {
        return this.mPolicy.isKeyguardLocked();
    }

    public boolean isKeyguardShowingAndNotOccluded() {
        return this.mPolicy.isKeyguardShowingAndNotOccluded();
    }

    public boolean isKeyguardSecure(int userId) {
        if (userId != UserHandle.getCallingUserId() && !checkCallingPermission("android.permission.INTERACT_ACROSS_USERS", "isKeyguardSecure")) {
            throw new SecurityException("Requires INTERACT_ACROSS_USERS permission");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            return this.mPolicy.isKeyguardSecure(userId);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void dismissKeyguard(IKeyguardDismissCallback callback, CharSequence message) {
        if (!checkCallingPermission("android.permission.CONTROL_KEYGUARD", "dismissKeyguard")) {
            throw new SecurityException("Requires CONTROL_KEYGUARD permission");
        }
        if (this.mAtmService.mKeyguardController.isShowingDream()) {
            this.mAtmService.mTaskSupervisor.wakeUp("leaveDream");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.dismissKeyguardLw(callback, message);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void addKeyguardLockedStateListener(IKeyguardLockedStateListener listener) {
        enforceSubscribeToKeyguardLockedStatePermission();
        boolean registered = this.mKeyguardLockedStateListeners.register(listener);
        if (!registered) {
            Slog.w("WindowManager", "Failed to register listener: " + listener);
        }
    }

    public void removeKeyguardLockedStateListener(IKeyguardLockedStateListener listener) {
        enforceSubscribeToKeyguardLockedStatePermission();
        this.mKeyguardLockedStateListeners.unregister(listener);
    }

    private void enforceSubscribeToKeyguardLockedStatePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SUBSCRIBE_TO_KEYGUARD_LOCKED_STATE", "android.permission.SUBSCRIBE_TO_KEYGUARD_LOCKED_STATE permission required to subscribe to keyguard locked state changes");
    }

    private void dispatchKeyguardLockedState() {
        this.mH.post(new Runnable() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                WindowManagerService.this.m8506x1d1bd459();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dispatchKeyguardLockedState$4$com-android-server-wm-WindowManagerService  reason: not valid java name */
    public /* synthetic */ void m8506x1d1bd459() {
        boolean isKeyguardLocked = this.mPolicy.isKeyguardShowing();
        if (this.mDispatchedKeyguardLockedState == isKeyguardLocked) {
            return;
        }
        int n = this.mKeyguardLockedStateListeners.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                this.mKeyguardLockedStateListeners.getBroadcastItem(i).onKeyguardLockedStateChanged(isKeyguardLocked);
            } catch (RemoteException e) {
            }
        }
        this.mKeyguardLockedStateListeners.finishBroadcast();
        this.mDispatchedKeyguardLockedState = isKeyguardLocked;
    }

    public void setSwitchingUser(boolean switching) {
        if (!checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "setSwitchingUser()")) {
            throw new SecurityException("Requires INTERACT_ACROSS_USERS_FULL permission");
        }
        this.mPolicy.setSwitchingUser(switching);
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mSwitchingUser = switching;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void showGlobalActions() {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "showGlobalActions()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        this.mPolicy.showGlobalActions();
    }

    public void closeSystemDialogs(String reason) {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        if (!this.mAtmService.checkCanCloseSystemDialogs(callingPid, callingUid, null)) {
            return;
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.closeSystemDialogs(reason);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    static float fixScale(float scale) {
        if (scale < 0.0f) {
            scale = 0.0f;
        } else if (scale > 20.0f) {
            scale = 20.0f;
        }
        return Math.abs(scale);
    }

    public void setAnimationScale(int which, float scale) {
        if (!checkCallingPermission("android.permission.SET_ANIMATION_SCALE", "setAnimationScale()")) {
            throw new SecurityException("Requires SET_ANIMATION_SCALE permission");
        }
        float scale2 = fixScale(scale);
        switch (which) {
            case 0:
                this.mWindowAnimationScaleSetting = scale2;
                break;
            case 1:
                this.mTransitionAnimationScaleSetting = scale2;
                break;
            case 2:
                this.mAnimatorDurationScaleSetting = scale2;
                break;
        }
        this.mH.sendEmptyMessage(14);
    }

    public void setAnimationScales(float[] scales) {
        if (!checkCallingPermission("android.permission.SET_ANIMATION_SCALE", "setAnimationScale()")) {
            throw new SecurityException("Requires SET_ANIMATION_SCALE permission");
        }
        if (scales != null) {
            if (scales.length >= 1) {
                this.mWindowAnimationScaleSetting = fixScale(scales[0]);
            }
            if (scales.length >= 2) {
                this.mTransitionAnimationScaleSetting = fixScale(scales[1]);
            }
            if (scales.length >= 3) {
                this.mAnimatorDurationScaleSetting = fixScale(scales[2]);
                dispatchNewAnimatorScaleLocked(null);
            }
        }
        this.mH.sendEmptyMessage(14);
    }

    private void setAnimatorDurationScale(float scale) {
        this.mAnimatorDurationScaleSetting = scale;
        ValueAnimator.setDurationScale(scale);
    }

    public float getWindowAnimationScaleLocked() {
        if (this.mAnimationsDisabled || TranFoldWMCustody.instance().disableWindowAnimations()) {
            return 0.0f;
        }
        return this.mWindowAnimationScaleSetting;
    }

    public float getTransitionAnimationScaleLocked() {
        if (this.mAnimationsDisabled || TranFoldWMCustody.instance().disableTransitionAnimations()) {
            return 0.0f;
        }
        return this.mTransitionAnimationScaleSetting;
    }

    public float getAnimationScale(int which) {
        switch (which) {
            case 0:
                return this.mWindowAnimationScaleSetting;
            case 1:
                return this.mTransitionAnimationScaleSetting;
            case 2:
                return this.mAnimatorDurationScaleSetting;
            default:
                return 0.0f;
        }
    }

    public float[] getAnimationScales() {
        return new float[]{this.mWindowAnimationScaleSetting, this.mTransitionAnimationScaleSetting, this.mAnimatorDurationScaleSetting};
    }

    public float getCurrentAnimatorScale() {
        float f;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                f = (this.mAnimationsDisabled || TranFoldWMCustody.instance().disableViewAnimations()) ? 0.0f : this.mAnimatorDurationScaleSetting;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchNewAnimatorScaleLocked(Session session) {
        this.mH.obtainMessage(34, session).sendToTarget();
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void registerPointerEventListener(WindowManagerPolicyConstants.PointerEventListener listener, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.registerPointerEventListener(listener);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void unregisterPointerEventListener(WindowManagerPolicyConstants.PointerEventListener listener, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.unregisterPointerEventListener(listener);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public int getLidState() {
        int sw = this.mInputManager.getSwitchState(-1, -256, 0);
        if (sw > 0) {
            return 0;
        }
        return sw == 0 ? 1 : -1;
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void lockDeviceNow() {
        lockNow(null);
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public int getCameraLensCoverState() {
        int sw = this.mInputManager.getSwitchState(-1, -256, 9);
        if (sw > 0) {
            return 1;
        }
        return sw == 0 ? 0 : -1;
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void switchKeyboardLayout(int deviceId, int direction) {
        this.mInputManager.switchKeyboardLayout(deviceId, direction);
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void shutdown(boolean confirm) {
        ShutdownThread.shutdown(ActivityThread.currentActivityThread().getSystemUiContext(), "userrequested", confirm);
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void reboot(boolean confirm) {
        ShutdownThread.reboot(ActivityThread.currentActivityThread().getSystemUiContext(), "userrequested", confirm);
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public void rebootSafeMode(boolean confirm) {
        ShutdownThread.rebootSafeMode(ActivityThread.currentActivityThread().getSystemUiContext(), confirm);
    }

    public void setCurrentProfileIds(int[] currentProfileIds) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mCurrentProfileIds = currentProfileIds;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setCurrentUser(int newUserId, int[] currentProfileIds) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mCurrentUserId = newUserId;
                this.mCurrentProfileIds = currentProfileIds;
                this.mPolicy.setCurrentUserLw(newUserId);
                this.mKeyguardDisableHandler.setCurrentUser(newUserId);
                this.mRoot.switchUser(newUserId);
                this.mWindowPlacerLocked.performSurfacePlacement();
                DisplayContent displayContent = getDefaultDisplayContentLocked();
                if (this.mDisplayReady) {
                    int forcedDensity = getForcedDisplayDensityForUserLocked(newUserId);
                    int targetDensity = forcedDensity != 0 ? forcedDensity : displayContent.mInitialDisplayDensity;
                    displayContent.setForcedDensity(targetDensity, -2);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCurrentProfile(int userId) {
        if (userId == this.mCurrentUserId) {
            return true;
        }
        int i = 0;
        while (true) {
            int[] iArr = this.mCurrentProfileIds;
            if (i < iArr.length) {
                if (iArr[i] == userId) {
                    return true;
                }
                i++;
            } else {
                return false;
            }
        }
    }

    public void enableScreenAfterBoot() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (ProtoLogCache.WM_DEBUG_BOOT_enabled) {
                    boolean protoLogParam0 = this.mDisplayEnabled;
                    boolean protoLogParam1 = this.mForceDisplayEnabled;
                    boolean protoLogParam2 = this.mShowingBootMessages;
                    boolean protoLogParam3 = this.mSystemBooted;
                    String protoLogParam4 = String.valueOf(new RuntimeException("here").fillInStackTrace());
                    ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_BOOT, -1884933373, 255, (String) null, new Object[]{Boolean.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), protoLogParam4});
                }
                boolean protoLogParam02 = this.mSystemBooted;
                if (protoLogParam02) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mSystemBooted = true;
                hideBootMessagesLocked();
                this.mH.sendEmptyMessageDelayed(23, 30000L);
                resetPriorityAfterLockedSection();
                this.mPolicy.systemBooted();
                performEnableScreen();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void enableScreenIfNeeded() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                enableScreenIfNeededLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableScreenIfNeededLocked() {
        if (ProtoLogCache.WM_DEBUG_BOOT_enabled) {
            boolean protoLogParam0 = this.mDisplayEnabled;
            boolean protoLogParam1 = this.mForceDisplayEnabled;
            boolean protoLogParam2 = this.mShowingBootMessages;
            boolean protoLogParam3 = this.mSystemBooted;
            String protoLogParam4 = String.valueOf(new RuntimeException("here").fillInStackTrace());
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_BOOT, -549028919, 255, (String) null, new Object[]{Boolean.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), protoLogParam4});
        }
        boolean protoLogParam02 = this.mDisplayEnabled;
        if (protoLogParam02) {
            return;
        }
        if (!this.mSystemBooted && !this.mShowingBootMessages) {
            return;
        }
        this.mH.sendEmptyMessage(16);
    }

    public void performBootTimeout() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mDisplayEnabled) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (ProtoLogCache.WM_ERROR_enabled) {
                    Object[] objArr = null;
                    ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1001904964, 0, "***** BOOT TIMEOUT: forcing display enabled", (Object[]) null);
                }
                this.mForceDisplayEnabled = true;
                resetPriorityAfterLockedSection();
                performEnableScreen();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void onSystemUiStarted() {
        this.mPolicy.onSystemUiStarted();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performEnableScreen() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (ProtoLogCache.WM_DEBUG_BOOT_enabled) {
                    boolean protoLogParam0 = this.mDisplayEnabled;
                    boolean protoLogParam1 = this.mForceDisplayEnabled;
                    boolean protoLogParam2 = this.mShowingBootMessages;
                    boolean protoLogParam3 = this.mSystemBooted;
                    boolean protoLogParam4 = this.mOnlyCore;
                    String protoLogParam5 = String.valueOf(new RuntimeException("here").fillInStackTrace());
                    ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_BOOT, -618015844, 1023, (String) null, new Object[]{Boolean.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), Boolean.valueOf(protoLogParam4), protoLogParam5});
                }
                boolean protoLogParam02 = this.mDisplayEnabled;
                if (protoLogParam02) {
                    resetPriorityAfterLockedSection();
                } else if (!this.mSystemBooted && !this.mShowingBootMessages) {
                    resetPriorityAfterLockedSection();
                } else if (this.mShowingBootMessages || this.mPolicy.canDismissBootAnimation()) {
                    if (!this.mForceDisplayEnabled) {
                        if (this.mBootWaitForWindowsStartTime < 0) {
                            this.mBootWaitForWindowsStartTime = SystemClock.elapsedRealtime();
                        }
                        for (int i = this.mRoot.getChildCount() - 1; i >= 0; i--) {
                            if (((DisplayContent) this.mRoot.getChildAt(i)).shouldWaitForSystemDecorWindowsOnBoot()) {
                                resetPriorityAfterLockedSection();
                                return;
                            }
                        }
                        long waitTime = SystemClock.elapsedRealtime() - this.mBootWaitForWindowsStartTime;
                        this.mBootWaitForWindowsStartTime = -1L;
                        if (waitTime > 10 && ProtoLogCache.WM_DEBUG_BOOT_enabled) {
                            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_BOOT, 544101314, 1, (String) null, new Object[]{Long.valueOf(waitTime)});
                        }
                    }
                    if (!this.mBootAnimationStopped) {
                        Trace.asyncTraceBegin(32L, "Stop bootanim", 0);
                        SystemProperties.set("service.bootanim.exit", "1");
                        this.mBootAnimationStopped = true;
                    }
                    if (!this.mForceDisplayEnabled && !checkBootAnimationCompleteLocked()) {
                        if (ProtoLogCache.WM_DEBUG_BOOT_enabled) {
                            Object[] objArr = null;
                            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_BOOT, 374972436, 0, (String) null, (Object[]) null);
                        }
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    try {
                        IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
                        if (surfaceFlinger != null) {
                            if (ProtoLogCache.WM_ERROR_enabled) {
                                Object[] objArr2 = null;
                                ProtoLogImpl.i(ProtoLogGroup.WM_ERROR, 620368427, 0, "******* TELLING SURFACE FLINGER WE ARE BOOTED!", (Object[]) null);
                            }
                            Parcel data = Parcel.obtain();
                            data.writeInterfaceToken("android.ui.ISurfaceComposer");
                            surfaceFlinger.transact(1, data, null, 0);
                            data.recycle();
                        }
                    } catch (RemoteException e) {
                        if (ProtoLogCache.WM_ERROR_enabled) {
                            Object[] objArr3 = null;
                            ProtoLogImpl.e(ProtoLogGroup.WM_ERROR, -87703044, 0, "Boot completed: SurfaceFlinger is dead!", (Object[]) null);
                        }
                    }
                    EventLogTags.writeWmBootAnimationDone(SystemClock.uptimeMillis());
                    Trace.asyncTraceEnd(32L, "Stop bootanim", 0);
                    this.mDisplayEnabled = true;
                    if (ProtoLogCache.WM_DEBUG_SCREEN_ON_enabled) {
                        Object[] objArr4 = null;
                        ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_SCREEN_ON, -116086365, 0, (String) null, (Object[]) null);
                    }
                    this.mInputManagerCallback.setEventDispatchingLw(this.mEventDispatchingEnabled);
                    resetPriorityAfterLockedSection();
                    try {
                        this.mActivityManager.bootAnimationComplete();
                    } catch (RemoteException e2) {
                    }
                    this.mPolicy.enableScreenAfterBoot();
                    updateRotationUnchecked(false, false);
                } else {
                    resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkBootAnimationCompleteLocked() {
        if (SystemService.isRunning(BOOT_ANIMATION_SERVICE)) {
            this.mH.removeMessages(37);
            this.mH.sendEmptyMessageDelayed(37, 50L);
            if (ProtoLogCache.WM_DEBUG_BOOT_enabled) {
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_BOOT, 600140673, 0, (String) null, (Object[]) null);
            }
            return false;
        } else if (ProtoLogCache.WM_DEBUG_BOOT_enabled) {
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_BOOT, 1224307091, 0, (String) null, (Object[]) null);
            return true;
        } else {
            return true;
        }
    }

    public void showBootMessage(CharSequence msg, boolean always) {
        boolean first = false;
        synchronized (this.mGlobalLock) {
            try {
                try {
                    boostPriorityForLockedSection();
                    if (ProtoLogCache.WM_DEBUG_BOOT_enabled) {
                        String protoLogParam0 = String.valueOf(msg);
                        boolean protoLogParam2 = this.mAllowBootMessages;
                        boolean protoLogParam3 = this.mShowingBootMessages;
                        boolean protoLogParam4 = this.mSystemBooted;
                        String protoLogParam5 = String.valueOf(new RuntimeException("here").fillInStackTrace());
                        ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_BOOT, -874446906, 1020, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(always), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), Boolean.valueOf(protoLogParam4), protoLogParam5});
                    }
                    if (!this.mAllowBootMessages) {
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    if (!this.mShowingBootMessages) {
                        if (!always) {
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        first = true;
                    }
                    if (this.mSystemBooted) {
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    this.mShowingBootMessages = true;
                    this.mPolicy.showBootMessage(msg, always);
                    resetPriorityAfterLockedSection();
                    if (first) {
                        performEnableScreen();
                    }
                } catch (Throwable th) {
                    th = th;
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void hideBootMessagesLocked() {
        if (ProtoLogCache.WM_DEBUG_BOOT_enabled) {
            boolean protoLogParam0 = this.mDisplayEnabled;
            boolean protoLogParam1 = this.mForceDisplayEnabled;
            boolean protoLogParam2 = this.mShowingBootMessages;
            boolean protoLogParam3 = this.mSystemBooted;
            String protoLogParam4 = String.valueOf(new RuntimeException("here").fillInStackTrace());
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_BOOT, -1350198040, 255, (String) null, new Object[]{Boolean.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), protoLogParam4});
        }
        boolean protoLogParam02 = this.mShowingBootMessages;
        if (protoLogParam02) {
            this.mShowingBootMessages = false;
            this.mPolicy.hideBootMessages();
        }
    }

    public void setInTouchMode(boolean mode) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mInTouchMode == mode) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                int pid = Binder.getCallingPid();
                int uid = Binder.getCallingUid();
                boolean z = false;
                boolean hasPermission = (this.mAtmService.instrumentationSourceHasPermission(pid, "android.permission.MODIFY_TOUCH_MODE_STATE") || checkCallingPermission("android.permission.MODIFY_TOUCH_MODE_STATE", "setInTouchMode()", false)) ? true : true;
                long token = Binder.clearCallingIdentity();
                if (this.mInputManager.setInTouchMode(mode, pid, uid, hasPermission)) {
                    this.mInTouchMode = mode;
                }
                Binder.restoreCallingIdentity(token);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getInTouchMode() {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                z = this.mInTouchMode;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return z;
    }

    public void showEmulatorDisplayOverlayIfNeeded() {
        if (this.mContext.getResources().getBoolean(17891836) && SystemProperties.getBoolean(PROPERTY_EMULATOR_CIRCULAR, false) && Build.IS_EMULATOR) {
            H h = this.mH;
            h.sendMessage(h.obtainMessage(36));
        }
    }

    public void showEmulatorDisplayOverlay() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS) {
                    Slog.i("WindowManager", ">>> showEmulatorDisplayOverlay");
                }
                if (this.mEmulatorDisplayOverlay == null) {
                    this.mEmulatorDisplayOverlay = new EmulatorDisplayOverlay(this.mContext, getDefaultDisplayContentLocked(), (this.mPolicy.getWindowLayerFromTypeLw(2018) * 10000) + 10, this.mTransaction);
                }
                this.mEmulatorDisplayOverlay.setVisibility(true, this.mTransaction);
                this.mTransaction.apply();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void showStrictModeViolation(boolean on) {
        int pid = Binder.getCallingPid();
        if (on) {
            H h = this.mH;
            h.sendMessage(h.obtainMessage(25, 1, pid));
            H h2 = this.mH;
            h2.sendMessageDelayed(h2.obtainMessage(25, 0, pid), 1000L);
            return;
        }
        H h3 = this.mH;
        h3.sendMessage(h3.obtainMessage(25, 0, pid));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showStrictModeViolation(int arg, int pid) {
        boolean on = arg != 0;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (on && !this.mRoot.canShowStrictModeViolation(pid)) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                if (WindowManagerDebugConfig.SHOW_VERBOSE_TRANSACTIONS) {
                    Slog.i("WindowManager", ">>> showStrictModeViolation");
                }
                if (this.mStrictModeFlash == null) {
                    this.mStrictModeFlash = new StrictModeFlash(getDefaultDisplayContentLocked(), this.mTransaction);
                }
                this.mStrictModeFlash.setVisibility(on, this.mTransaction);
                this.mTransaction.apply();
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setStrictModeVisualIndicatorPreference(String value) {
        SystemProperties.set("persist.sys.strictmode.visual", value);
    }

    public Bitmap tranScreenshotWallpaperLocked() {
        return tranScaleScreenshotWallpaperLocked(1.0f);
    }

    public Bitmap tranScaleScreenshotWallpaperLocked(float scale) {
        Bitmap tranScreenshotWallpaperLocked;
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "screenshotWallpaper()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        try {
            Trace.traceBegin(32L, "tansScreenshotWallpaper");
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(0);
                tranScreenshotWallpaperLocked = dc.mWallpaperController.tranScreenshotWallpaperLocked(scale);
            }
            resetPriorityAfterLockedSection();
            return tranScreenshotWallpaperLocked;
        } finally {
            Trace.traceEnd(32L);
        }
    }

    public Bitmap screenshotWallpaper() {
        return screenshotWallpaperInternal(1.0f);
    }

    public Bitmap screenshotScaleWallpaper(float scale) {
        if (scale <= 0.0f || scale > 1.0f) {
            return null;
        }
        return screenshotWallpaperInternal(scale);
    }

    private Bitmap screenshotWallpaperInternal(float scale) {
        Bitmap screenshotWallpaperLocked;
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "screenshotWallpaper()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        try {
            Trace.traceBegin(32L, "screenshotWallpaper");
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(0);
                screenshotWallpaperLocked = scale == 1.0f ? dc.mWallpaperController.screenshotWallpaperLocked() : dc.mWallpaperController.screenshotWallpaperLocked(scale);
            }
            resetPriorityAfterLockedSection();
            return screenshotWallpaperLocked;
        } finally {
            Trace.traceEnd(32L);
        }
    }

    public SurfaceControl mirrorWallpaperSurface(int displayId) {
        SurfaceControl mirrorWallpaperSurface;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                mirrorWallpaperSurface = dc.mWallpaperController.mirrorWallpaperSurface();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return mirrorWallpaperSurface;
    }

    public boolean requestAssistScreenshot(final IAssistDataReceiver receiver) {
        final Bitmap bm;
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "requestAssistScreenshot()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(0);
                if (displayContent == null) {
                    if (WindowManagerDebugConfig.DEBUG_SCREENSHOT) {
                        Slog.i("WindowManager", "Screenshot returning null. No Display for displayId=0");
                    }
                    bm = null;
                } else {
                    bm = displayContent.screenshotDisplayLocked();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                receiver.onHandleAssistScreenshot(bm);
            }
        });
        return true;
    }

    public TaskSnapshot getTaskSnapshot(int taskId, int userId, boolean isLowResolution, boolean restoreFromDisk) {
        return this.mTaskSnapshotController.getSnapshot(taskId, userId, restoreFromDisk, isLowResolution);
    }

    public Bitmap captureTaskBitmap(int taskId, SurfaceControl.LayerCaptureArgs.Builder layerCaptureArgsBuilder) {
        if (this.mTaskSnapshotController.shouldDisableSnapshots()) {
            return null;
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                Task task = this.mRoot.anyTaskForId(taskId);
                if (task == null) {
                    resetPriorityAfterLockedSection();
                    return null;
                }
                task.getBounds(this.mTmpRect);
                this.mTmpRect.offsetTo(0, 0);
                SurfaceControl sc = task.getSurfaceControl();
                SurfaceControl.ScreenshotHardwareBuffer buffer = SurfaceControl.captureLayers(layerCaptureArgsBuilder.setLayer(sc).setSourceCrop(this.mTmpRect).build());
                if (buffer == null) {
                    Slog.w("WindowManager", "Could not get screenshot buffer for taskId: " + taskId);
                    resetPriorityAfterLockedSection();
                    return null;
                }
                Bitmap asBitmap = buffer.asBitmap();
                resetPriorityAfterLockedSection();
                return asBitmap;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void removeObsoleteTaskFiles(ArraySet<Integer> persistentTaskIds, int[] runningUserIds) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mTaskSnapshotController.removeObsoleteTaskFiles(persistentTaskIds, runningUserIds);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setFixedToUserRotation(int displayId, int fixedToUserRotation) {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "setFixedToUserRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display == null) {
                    Slog.w("WindowManager", "Trying to set fixed to user rotation for a missing display.");
                    resetPriorityAfterLockedSection();
                    return;
                }
                display.getDisplayRotation().setFixedToUserRotation(fixedToUserRotation);
                resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getFixedToUserRotation(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display == null) {
                    Slog.w("WindowManager", "Trying to get fixed to user rotation for a missing display.");
                    resetPriorityAfterLockedSection();
                    return -1;
                }
                int fixedToUserRotationMode = display.getDisplayRotation().getFixedToUserRotationMode();
                resetPriorityAfterLockedSection();
                return fixedToUserRotationMode;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setIgnoreOrientationRequest(int displayId, boolean ignoreOrientationRequest) {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "setIgnoreOrientationRequest()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display == null) {
                    Slog.w("WindowManager", "Trying to setIgnoreOrientationRequest() for a missing display.");
                    resetPriorityAfterLockedSection();
                    return;
                }
                display.setIgnoreOrientationRequest(ignoreOrientationRequest);
                resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getIgnoreOrientationRequest(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display == null) {
                    Slog.w("WindowManager", "Trying to getIgnoreOrientationRequest() for a missing display.");
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean ignoreOrientationRequest = display.getIgnoreOrientationRequest();
                resetPriorityAfterLockedSection();
                return ignoreOrientationRequest;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsIgnoreOrientationRequestDisabled(boolean isDisabled) {
        if (isDisabled == this.mIsIgnoreOrientationRequestDisabled) {
            return;
        }
        this.mIsIgnoreOrientationRequestDisabled = isDisabled;
        for (int i = this.mRoot.getChildCount() - 1; i >= 0; i--) {
            ((DisplayContent) this.mRoot.getChildAt(i)).onIsIgnoreOrientationRequestDisabledChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isIgnoreOrientationRequestDisabled() {
        return this.mIsIgnoreOrientationRequestDisabled;
    }

    public void freezeRotation(int rotation) {
        freezeDisplayRotation(0, rotation);
    }

    public void freezeDisplayRotation(int displayId, int rotation) {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "freezeRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
        if (rotation < -1 || rotation > 3) {
            throw new IllegalArgumentException("Rotation argument must be -1 or a valid rotation constant.");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display == null) {
                    Slog.w("WindowManager", "Trying to freeze rotation for a missing display.");
                    resetPriorityAfterLockedSection();
                    return;
                }
                display.getDisplayRotation().freezeRotation(rotation);
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(origId);
                updateRotationUnchecked(false, false);
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void thawRotation() {
        thawDisplayRotation(0);
    }

    public void thawDisplayRotation(int displayId) {
        if (!checkCallingPermission("android.permission.SET_ORIENTATION", "thawRotation()")) {
            throw new SecurityException("Requires SET_ORIENTATION permission");
        }
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            long protoLogParam0 = getDefaultDisplayRotation();
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1076978367, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display == null) {
                    Slog.w("WindowManager", "Trying to thaw rotation for a missing display.");
                    resetPriorityAfterLockedSection();
                    return;
                }
                display.getDisplayRotation().thawRotation();
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(origId);
                updateRotationUnchecked(false, false);
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean isRotationFrozen() {
        return isDisplayRotationFrozen(0);
    }

    public boolean isDisplayRotationFrozen(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display == null) {
                    Slog.w("WindowManager", "Trying to check if rotation is frozen on a missing display.");
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean isRotationFrozen = display.getDisplayRotation().isRotationFrozen();
                resetPriorityAfterLockedSection();
                return isRotationFrozen;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDisplayUserRotation(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display == null) {
                    Slog.w("WindowManager", "Trying to get user rotation of a missing display.");
                    resetPriorityAfterLockedSection();
                    return -1;
                }
                int userRotation = display.getDisplayRotation().getUserRotation();
                resetPriorityAfterLockedSection();
                return userRotation;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void updateRotation(boolean alwaysSendConfiguration, boolean forceRelayout) {
        updateRotationUnchecked(alwaysSendConfiguration, forceRelayout);
    }

    private void updateRotationUnchecked(boolean alwaysSendConfiguration, boolean forceRelayout) {
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -198463978, 15, (String) null, new Object[]{Boolean.valueOf(alwaysSendConfiguration), Boolean.valueOf(forceRelayout)});
        }
        Trace.traceBegin(32L, "updateRotation");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                boolean layoutNeeded = false;
                int displayCount = this.mRoot.mChildren.size();
                for (int i = 0; i < displayCount; i++) {
                    DisplayContent displayContent = (DisplayContent) this.mRoot.mChildren.get(i);
                    Trace.traceBegin(32L, "updateRotation: display");
                    boolean rotationChanged = displayContent.updateRotationUnchecked();
                    Trace.traceEnd(32L);
                    if (rotationChanged) {
                        this.mAtmService.getTaskChangeNotificationController().notifyOnActivityRotation(displayContent.mDisplayId);
                    }
                    boolean pendingRemoteRotation = rotationChanged && (displayContent.getDisplayRotation().isWaitingForRemoteRotation() || displayContent.mTransitionController.isCollecting());
                    if (!pendingRemoteRotation) {
                        if (!rotationChanged || forceRelayout) {
                            displayContent.setLayoutNeeded();
                            layoutNeeded = true;
                        }
                        if (rotationChanged || alwaysSendConfiguration) {
                            displayContent.sendNewConfiguration();
                        }
                    }
                }
                if (layoutNeeded) {
                    Trace.traceBegin(32L, "updateRotation: performSurfacePlacement");
                    this.mWindowPlacerLocked.performSurfacePlacement();
                    Trace.traceEnd(32L);
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
            Trace.traceEnd(32L);
        }
    }

    public int getDefaultDisplayRotation() {
        int rotation;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                rotation = getDefaultDisplayContentLocked().getRotation();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return rotation;
    }

    public void setDisplayWindowRotationController(IDisplayWindowRotationController controller) {
        ActivityTaskManagerService.enforceTaskPermission("setDisplayWindowRotationController");
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                IDisplayWindowRotationController iDisplayWindowRotationController = this.mDisplayRotationController;
                if (iDisplayWindowRotationController != null) {
                    iDisplayWindowRotationController.asBinder().unlinkToDeath(this.mDisplayRotationControllerDeath, 0);
                    this.mDisplayRotationController = null;
                }
                controller.asBinder().linkToDeath(this.mDisplayRotationControllerDeath, 0);
                this.mDisplayRotationController = controller;
            }
            resetPriorityAfterLockedSection();
        } catch (RemoteException e) {
            throw new RuntimeException("Unable to set rotation controller");
        }
    }

    public SurfaceControl addShellRoot(int displayId, IWindow client, int shellRootLayer) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MANAGE_APP_TOKENS") != 0) {
            throw new SecurityException("Must hold permission android.permission.MANAGE_APP_TOKENS");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc != null) {
                    SurfaceControl addShellRoot = dc.addShellRoot(client, shellRootLayer);
                    resetPriorityAfterLockedSection();
                    return addShellRoot;
                }
                resetPriorityAfterLockedSection();
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [4639=4] */
    public void setShellRootAccessibilityWindow(int displayId, int shellRootLayer, IWindow target) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MANAGE_APP_TOKENS") != 0) {
            throw new SecurityException("Must hold permission android.permission.MANAGE_APP_TOKENS");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                ShellRoot root = dc.mShellRoots.get(shellRootLayer);
                if (root == null) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                root.setAccessibilityWindow(target);
                resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void setDisplayWindowInsetsController(int displayId, IDisplayWindowInsetsController insetsController) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MANAGE_APP_TOKENS") != 0) {
            throw new SecurityException("Must hold permission android.permission.MANAGE_APP_TOKENS");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc != null) {
                    dc.setRemoteInsetsController(insetsController);
                    resetPriorityAfterLockedSection();
                    return;
                }
                resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void updateDisplayWindowRequestedVisibilities(int displayId, InsetsVisibilities vis) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MANAGE_APP_TOKENS") != 0) {
            throw new SecurityException("Must hold permission android.permission.MANAGE_APP_TOKENS");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc != null && dc.mRemoteInsetsControlTarget != null) {
                    dc.mRemoteInsetsControlTarget.setRequestedVisibilities(vis);
                    dc.getInsetsStateController().onInsetsModified(dc.mRemoteInsetsControlTarget);
                    resetPriorityAfterLockedSection();
                    return;
                }
                resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    public int watchRotation(IRotationWatcher watcher, int displayId) {
        DisplayContent displayContent;
        int rotation;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                displayContent = this.mRoot.getDisplayContent(displayId);
            } finally {
            }
        }
        resetPriorityAfterLockedSection();
        if (displayContent == null) {
            throw new IllegalArgumentException("Trying to register rotation event for invalid display: " + displayId);
        }
        final IBinder watcherBinder = watcher.asBinder();
        IBinder.DeathRecipient dr = new IBinder.DeathRecipient() { // from class: com.android.server.wm.WindowManagerService.11
            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                synchronized (WindowManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        int i = 0;
                        while (i < WindowManagerService.this.mRotationWatchers.size()) {
                            if (watcherBinder == WindowManagerService.this.mRotationWatchers.get(i).mWatcher.asBinder()) {
                                RotationWatcher removed = WindowManagerService.this.mRotationWatchers.remove(i);
                                IBinder binder = removed.mWatcher.asBinder();
                                if (binder != null) {
                                    binder.unlinkToDeath(this, 0);
                                }
                                i--;
                            }
                            i++;
                        }
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        };
        synchronized (this.mGlobalLock) {
            try {
                try {
                    boostPriorityForLockedSection();
                    watcher.asBinder().linkToDeath(dr, 0);
                    this.mRotationWatchers.add(new RotationWatcher(watcher, dr, displayId));
                } finally {
                }
            } catch (RemoteException e) {
            }
            rotation = displayContent.getRotation();
        }
        resetPriorityAfterLockedSection();
        return rotation;
    }

    public void removeRotationWatcher(IRotationWatcher watcher) {
        IBinder watcherBinder = watcher.asBinder();
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                int i = 0;
                while (i < this.mRotationWatchers.size()) {
                    RotationWatcher rotationWatcher = this.mRotationWatchers.get(i);
                    if (watcherBinder == rotationWatcher.mWatcher.asBinder()) {
                        RotationWatcher removed = this.mRotationWatchers.remove(i);
                        IBinder binder = removed.mWatcher.asBinder();
                        if (binder != null) {
                            binder.unlinkToDeath(removed.mDeathRecipient, 0);
                        }
                        i--;
                    }
                    i++;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean registerWallpaperVisibilityListener(IWallpaperVisibilityListener listener, int displayId) {
        boolean isWallpaperVisible;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    throw new IllegalArgumentException("Trying to register visibility event for invalid display: " + displayId);
                }
                this.mWallpaperVisibilityListeners.registerWallpaperVisibilityListener(listener, displayId);
                isWallpaperVisible = displayContent.mWallpaperController.isWallpaperVisible();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return isWallpaperVisible;
    }

    public void unregisterWallpaperVisibilityListener(IWallpaperVisibilityListener listener, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mWallpaperVisibilityListeners.unregisterWallpaperVisibilityListener(listener, displayId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void registerSystemGestureExclusionListener(ISystemGestureExclusionListener listener, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    throw new IllegalArgumentException("Trying to register visibility event for invalid display: " + displayId);
                }
                displayContent.registerSystemGestureExclusionListener(listener);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void unregisterSystemGestureExclusionListener(ISystemGestureExclusionListener listener, int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    throw new IllegalArgumentException("Trying to register visibility event for invalid display: " + displayId);
                }
                displayContent.unregisterSystemGestureExclusionListener(listener);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportSystemGestureExclusionChanged(Session session, IWindow window, List<Rect> exclusionRects) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, window, true);
                if (win.setSystemGestureExclusion(exclusionRects)) {
                    win.getDisplayContent().updateSystemGestureExclusion();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportKeepClearAreasChanged(Session session, IWindow window, List<Rect> restricted, List<Rect> unrestricted) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState win = windowForClientLocked(session, window, true);
                if (win.setKeepClearAreas(restricted, unrestricted)) {
                    win.getDisplayContent().updateKeepClearAreas();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void registerDisplayFoldListener(IDisplayFoldListener listener) {
        this.mPolicy.registerDisplayFoldListener(listener);
    }

    public void unregisterDisplayFoldListener(IDisplayFoldListener listener) {
        this.mPolicy.unregisterDisplayFoldListener(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setOverrideFoldedArea(Rect area) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                this.mPolicy.setOverrideFoldedArea(area);
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getFoldedArea() {
        Rect foldedArea;
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                foldedArea = this.mPolicy.getFoldedArea();
            }
            resetPriorityAfterLockedSection();
            return foldedArea;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int[] registerDisplayWindowListener(IDisplayWindowListener listener) {
        ActivityTaskManagerService.enforceTaskPermission("registerDisplayWindowListener");
        long ident = Binder.clearCallingIdentity();
        try {
            return this.mDisplayNotificationController.registerListener(listener);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void unregisterDisplayWindowListener(IDisplayWindowListener listener) {
        ActivityTaskManagerService.enforceTaskPermission("unregisterDisplayWindowListener");
        this.mDisplayNotificationController.unregisterListener(listener);
    }

    public int getPreferredOptionsPanelGravity(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    resetPriorityAfterLockedSection();
                    return 81;
                }
                int preferredOptionsPanelGravity = displayContent.getPreferredOptionsPanelGravity();
                resetPriorityAfterLockedSection();
                return preferredOptionsPanelGravity;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean startViewServer(int port) {
        if (!isSystemSecure() && checkCallingPermission("android.permission.DUMP", "startViewServer") && port >= 1024) {
            ViewServer viewServer = this.mViewServer;
            if (viewServer != null) {
                if (!viewServer.isRunning()) {
                    try {
                        return this.mViewServer.start();
                    } catch (IOException e) {
                        if (ProtoLogCache.WM_ERROR_enabled) {
                            ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1545962566, 0, "View server did not start", (Object[]) null);
                        }
                    }
                }
                return false;
            }
            try {
                ViewServer viewServer2 = new ViewServer(this, port);
                this.mViewServer = viewServer2;
                return viewServer2.start();
            } catch (IOException e2) {
                if (ProtoLogCache.WM_ERROR_enabled) {
                    ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1545962566, 0, "View server did not start", (Object[]) null);
                }
                return false;
            }
        }
        return false;
    }

    private boolean isSystemSecure() {
        return "1".equals(SystemProperties.get(SYSTEM_SECURE, "1")) && "0".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
    }

    public boolean stopViewServer() {
        ViewServer viewServer;
        if (isSystemSecure() || !checkCallingPermission("android.permission.DUMP", "stopViewServer") || (viewServer = this.mViewServer) == null) {
            return false;
        }
        return viewServer.stop();
    }

    public boolean isViewServerRunning() {
        ViewServer viewServer;
        return !isSystemSecure() && checkCallingPermission("android.permission.DUMP", "isViewServerRunning") && (viewServer = this.mViewServer) != null && viewServer.isRunning();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean viewServerListWindows(Socket client) {
        if (isSystemSecure()) {
            return false;
        }
        final ArrayList<WindowState> windows = new ArrayList<>();
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda5
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        windows.add((WindowState) obj);
                    }
                }, false);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        BufferedWriter out = null;
        try {
            try {
                OutputStream clientStream = client.getOutputStream();
                out = new BufferedWriter(new OutputStreamWriter(clientStream), 8192);
                int count = windows.size();
                for (int i = 0; i < count; i++) {
                    WindowState w = windows.get(i);
                    out.write(Integer.toHexString(System.identityHashCode(w)));
                    out.write(32);
                    out.append(w.mAttrs.getTitle());
                    out.write(10);
                }
                out.write("DONE.\n");
                out.flush();
                out.close();
                return true;
            } catch (Exception e) {
                if (out == null) {
                    return false;
                }
                out.close();
                return false;
            } catch (Throwable th2) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e2) {
                    }
                }
                throw th2;
            }
        } catch (IOException e3) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean viewServerGetFocusedWindow(Socket client) {
        if (isSystemSecure()) {
            return false;
        }
        WindowState focusedWindow = getFocusedWindow();
        BufferedWriter out = null;
        try {
            try {
                OutputStream clientStream = client.getOutputStream();
                out = new BufferedWriter(new OutputStreamWriter(clientStream), 8192);
                if (focusedWindow != null) {
                    out.write(Integer.toHexString(System.identityHashCode(focusedWindow)));
                    out.write(32);
                    out.append(focusedWindow.mAttrs.getTitle());
                }
                out.write(10);
                out.flush();
                out.close();
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (Exception e2) {
            if (out == null) {
                return false;
            }
            out.close();
            return false;
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e3) {
                }
            }
            throw th;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5167=6, 5168=4, 5170=4, 5171=4, 5173=4, 5175=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean viewServerWindowCommand(Socket client, String command, String parameters) {
        Throwable th;
        int hashCode;
        WindowState window;
        String parameters2 = parameters;
        if (isSystemSecure()) {
            return false;
        }
        boolean success = true;
        Parcel data = null;
        Parcel reply = null;
        BufferedWriter out = null;
        try {
            try {
                try {
                    int index = parameters2.indexOf(32);
                    int index2 = index == -1 ? parameters.length() : index;
                    String code = parameters2.substring(0, index2);
                    hashCode = (int) Long.parseLong(code, 16);
                    if (index2 < parameters.length()) {
                        parameters2 = parameters2.substring(index2 + 1);
                    } else {
                        parameters2 = "";
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
                try {
                    window = findWindow(hashCode);
                } catch (Exception e) {
                    e = e;
                } catch (Throwable th3) {
                    th = th3;
                    if (data != null) {
                        data.recycle();
                    }
                    if (0 != 0) {
                        reply.recycle();
                    }
                    if (0 != 0) {
                        try {
                            out.close();
                        } catch (IOException e2) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e3) {
                e = e3;
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (IOException e4) {
        }
        if (window == null) {
            if (0 != 0) {
                data.recycle();
            }
            if (0 != 0) {
                reply.recycle();
            }
            if (0 != 0) {
                try {
                    out.close();
                } catch (IOException e5) {
                }
            }
            return false;
        }
        data = Parcel.obtain();
        data.writeInterfaceToken("android.view.IWindow");
        try {
            data.writeString(command);
            data.writeString(parameters2);
            data.writeInt(1);
            ParcelFileDescriptor.fromSocket(client).writeToParcel(data, 0);
            reply = Parcel.obtain();
            IBinder binder = window.mClient.asBinder();
            binder.transact(1, data, reply, 0);
            reply.readException();
            if (!client.isOutputShutdown()) {
                out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                out.write("DONE\n");
                out.flush();
            }
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
        } catch (Exception e6) {
            e = e6;
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam0 = String.valueOf(command);
                String protoLogParam1 = String.valueOf(parameters2);
                String protoLogParam2 = String.valueOf(e);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 2086878461, 0, "Could not send command %s with parameters %s. %s", new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
            }
            success = false;
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            if (out != null) {
                out.close();
            }
            return success;
        }
        if (out != null) {
            out.close();
        }
        return success;
    }

    public void addWindowChangeListener(WindowChangeListener listener) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mWindowChangeListeners.add(listener);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void removeWindowChangeListener(WindowChangeListener listener) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mWindowChangeListeners.remove(listener);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyWindowsChanged() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mWindowChangeListeners.isEmpty()) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                WindowChangeListener[] windowChangeListeners = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
                resetPriorityAfterLockedSection();
                for (WindowChangeListener windowChangeListener : windowChangeListeners) {
                    windowChangeListener.windowsChanged();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void notifyFocusChanged() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mWindowChangeListeners.isEmpty()) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                WindowChangeListener[] windowChangeListeners = (WindowChangeListener[]) this.mWindowChangeListeners.toArray(new WindowChangeListener[this.mWindowChangeListeners.size()]);
                resetPriorityAfterLockedSection();
                for (WindowChangeListener windowChangeListener : windowChangeListeners) {
                    windowChangeListener.focusChanged();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private WindowState findWindow(final int hashCode) {
        WindowState window;
        if (hashCode == -1) {
            return getFocusedWindow();
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                window = this.mRoot.getWindow(new Predicate() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda24
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return WindowManagerService.lambda$findWindow$7(hashCode, (WindowState) obj);
                    }
                });
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return window;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findWindow$7(int hashCode, WindowState w) {
        return System.identityHashCode(w) == hashCode;
    }

    public Configuration computeNewConfiguration(int displayId) {
        Configuration computeNewConfigurationLocked;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                computeNewConfigurationLocked = computeNewConfigurationLocked(displayId);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return computeNewConfigurationLocked;
    }

    private Configuration computeNewConfigurationLocked(int displayId) {
        if (!this.mDisplayReady) {
            return null;
        }
        Configuration config = new Configuration();
        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
        displayContent.computeScreenConfiguration(config);
        return config;
    }

    void notifyHardKeyboardStatusChange() {
        WindowManagerInternal.OnHardKeyboardStatusChangeListener listener;
        boolean available;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                listener = this.mHardKeyboardStatusChangeListener;
                available = this.mHardKeyboardAvailable;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        if (listener != null) {
            listener.onHardKeyboardStatusChange(available);
        }
    }

    public void setEventDispatching(boolean enabled) {
        if (!checkCallingPermission("android.permission.MANAGE_APP_TOKENS", "setEventDispatching()")) {
            throw new SecurityException("Requires MANAGE_APP_TOKENS permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mEventDispatchingEnabled = enabled;
                if (this.mDisplayEnabled) {
                    this.mInputManagerCallback.setEventDispatchingLw(enabled);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public String getFocusedWinPkgName() {
        String pkg;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                pkg = null;
                WindowState win = getFocusedWindow();
                if (win != null) {
                    pkg = win.getOwningPackage();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return pkg;
    }

    public boolean isSecureWindow() {
        boolean isSecure;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                isSecure = false;
                WindowState win = getFocusedWindow();
                if (win != null) {
                    isSecure = win.isSecureLocked();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return isSecure;
    }

    public boolean inMultiWindowMode() {
        boolean isMulti;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                isMulti = false;
                WindowState win = getFocusedWindow();
                if (win != null) {
                    isMulti = win.inMultiWindowMode();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return isMulti;
    }

    private WindowState getFocusedWindow() {
        WindowState focusedWindowLocked;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                focusedWindowLocked = getFocusedWindowLocked();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return focusedWindowLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getFocusedWindowLocked() {
        return this.mRoot.getTopFocusedDisplayContent().mCurrentFocus;
    }

    Task getImeFocusRootTaskLocked() {
        DisplayContent topFocusedDisplay = this.mRoot.getTopFocusedDisplayContent();
        ActivityRecord focusedApp = topFocusedDisplay.mFocusedApp;
        if (focusedApp == null || focusedApp.getTask() == null) {
            return null;
        }
        return focusedApp.getTask().getRootTask();
    }

    public boolean detectSafeMode() {
        if (!this.mInputManagerCallback.waitForInputDevicesReady(1000L) && ProtoLogCache.WM_ERROR_enabled) {
            ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1774661765, 1, "Devices still not ready after waiting %d milliseconds before attempting to detect safe mode.", new Object[]{1000L});
        }
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "safe_boot_disallowed", 0) != 0) {
            return false;
        }
        int menuState = this.mInputManager.getKeyCodeState(-1, -256, 82);
        int sState = this.mInputManager.getKeyCodeState(-1, -256, 47);
        int dpadState = this.mInputManager.getKeyCodeState(-1, UsbTerminalTypes.TERMINAL_IN_MIC, 23);
        int trackballState = this.mInputManager.getScanCodeState(-1, 65540, 272);
        int volumeDownState = this.mInputManager.getKeyCodeState(-1, -256, 25);
        this.mSafeMode = menuState > 0 || sState > 0 || dpadState > 0 || trackballState > 0 || volumeDownState > 0;
        try {
            if (SystemProperties.getInt(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, 0) != 0 || SystemProperties.getInt(ShutdownThread.RO_SAFEMODE_PROPERTY, 0) != 0) {
                this.mSafeMode = true;
                SystemProperties.set(ShutdownThread.REBOOT_SAFEMODE_PROPERTY, "");
            }
        } catch (IllegalArgumentException e) {
        }
        if (this.mSafeMode) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                long protoLogParam0 = menuState;
                long protoLogParam1 = sState;
                long protoLogParam2 = dpadState;
                long protoLogParam3 = trackballState;
                ProtoLogImpl.i(ProtoLogGroup.WM_ERROR, -1443029505, 85, "SAFE MODE ENABLED (menu=%d s=%d dpad=%d trackball=%d)", new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2), Long.valueOf(protoLogParam3)});
            }
            if (SystemProperties.getInt(ShutdownThread.RO_SAFEMODE_PROPERTY, 0) == 0) {
                SystemProperties.set(ShutdownThread.RO_SAFEMODE_PROPERTY, "1");
            }
        } else if (ProtoLogCache.WM_ERROR_enabled) {
            ProtoLogImpl.i(ProtoLogGroup.WM_ERROR, 1866772666, 0, "SAFE MODE not enabled", (Object[]) null);
        }
        this.mPolicy.setSafeMode(this.mSafeMode);
        return this.mSafeMode;
    }

    public void displayReady() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                if (this.mMaxUiWidth > 0) {
                    this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda12
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            WindowManagerService.this.m8507lambda$displayReady$8$comandroidserverwmWindowManagerService((DisplayContent) obj);
                        }
                    });
                }
                applyForcedPropertiesForDefaultDisplay();
                this.mAnimator.ready();
                this.mDisplayReady = true;
                this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda13
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((DisplayContent) obj).reconfigureDisplayLocked();
                    }
                });
                this.mIsTouchDevice = this.mContext.getPackageManager().hasSystemFeature("android.hardware.touchscreen");
                this.mIsFakeTouchDevice = this.mContext.getPackageManager().hasSystemFeature("android.hardware.faketouch");
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        this.mAtmService.updateConfiguration(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$displayReady$8$com-android-server-wm-WindowManagerService  reason: not valid java name */
    public /* synthetic */ void m8507lambda$displayReady$8$comandroidserverwmWindowManagerService(DisplayContent displayContent) {
        displayContent.setMaxUiWidth(this.mMaxUiWidth);
    }

    public void systemReady() {
        this.mSystemReady = true;
        this.mPolicy.systemReady();
        this.mRoot.forAllDisplayPolicies(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda9
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((DisplayPolicy) obj).systemReady();
            }
        });
        this.mTaskSnapshotController.systemReady();
        this.mHasWideColorGamutSupport = queryWideColorGamutSupport();
        this.mHasHdrSupport = queryHdrSupport();
        Handler handler = UiThread.getHandler();
        final SettingsObserver settingsObserver = this.mSettingsObserver;
        Objects.requireNonNull(settingsObserver);
        handler.post(new Runnable() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                WindowManagerService.SettingsObserver.this.loadSettings();
            }
        });
        IVrManager vrManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        if (vrManager != null) {
            try {
                boolean vrModeEnabled = vrManager.getVrModeState();
                synchronized (this.mGlobalLock) {
                    boostPriorityForLockedSection();
                    vrManager.registerListener(this.mVrStateCallbacks);
                    if (vrModeEnabled) {
                        this.mVrModeEnabled = vrModeEnabled;
                        this.mVrStateCallbacks.onVrStateChanged(vrModeEnabled);
                    }
                }
                resetPriorityAfterLockedSection();
            } catch (RemoteException e) {
            }
        }
    }

    private static boolean queryWideColorGamutSupport() {
        Optional<Boolean> hasWideColorProp = SurfaceFlingerProperties.has_wide_color_display();
        if (hasWideColorProp.isPresent()) {
            return hasWideColorProp.get().booleanValue();
        }
        try {
            ISurfaceFlingerConfigs surfaceFlinger = ISurfaceFlingerConfigs.getService();
            OptionalBool hasWideColor = surfaceFlinger.hasWideColorDisplay();
            if (hasWideColor != null) {
                return hasWideColor.value;
            }
            return false;
        } catch (RemoteException e) {
            return false;
        } catch (NoSuchElementException e2) {
            return false;
        }
    }

    private static boolean queryHdrSupport() {
        Optional<Boolean> hasHdrProp = SurfaceFlingerProperties.has_HDR_display();
        if (hasHdrProp.isPresent()) {
            return hasHdrProp.get().booleanValue();
        }
        try {
            ISurfaceFlingerConfigs surfaceFlinger = ISurfaceFlingerConfigs.getService();
            OptionalBool hasHdr = surfaceFlinger.hasHDRDisplay();
            if (hasHdr != null) {
                return hasHdr.value;
            }
            return false;
        } catch (RemoteException e) {
            return false;
        } catch (NoSuchElementException e2) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputTarget getInputTargetFromToken(IBinder inputToken) {
        WindowState windowState = this.mInputToWindowMap.get(inputToken);
        if (windowState != null) {
            return windowState;
        }
        EmbeddedWindowController.EmbeddedWindow embeddedWindow = this.mEmbeddedWindowController.get(inputToken);
        if (embeddedWindow != null) {
            return embeddedWindow;
        }
        return null;
    }

    InputTarget getInputTargetFromWindowTokenLocked(IBinder windowToken) {
        InputTarget window = this.mWindowMap.get(windowToken);
        if (window != null) {
            return window;
        }
        return this.mEmbeddedWindowController.getByWindowToken(windowToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportFocusChanged(IBinder oldToken, IBinder newToken) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                InputTarget lastTarget = getInputTargetFromToken(oldToken);
                InputTarget newTarget = getInputTargetFromToken(newToken);
                if (newTarget == null && lastTarget == null) {
                    Slog.v("WindowManager", "Unknown focus tokens, dropping reportFocusChanged");
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mFocusedInputTarget = newTarget;
                this.mAccessibilityController.onFocusChanged(lastTarget, newTarget);
                if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                    String protoLogParam0 = String.valueOf(lastTarget);
                    String protoLogParam1 = String.valueOf(newTarget);
                    ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, 115358443, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
                }
                resetPriorityAfterLockedSection();
                WindowState newFocusedWindow = newTarget != null ? newTarget.getWindowState() : null;
                if (newFocusedWindow != null && newFocusedWindow.mInputChannelToken == newToken) {
                    this.mAnrController.onFocusChanged(newFocusedWindow);
                    newFocusedWindow.reportFocusChangedSerialized(true);
                    notifyFocusChanged();
                }
                WindowState lastFocusedWindow = lastTarget != null ? lastTarget.getWindowState() : null;
                if (lastFocusedWindow != null && lastFocusedWindow.mInputChannelToken == oldToken) {
                    lastFocusedWindow.reportFocusChangedSerialized(false);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class H extends Handler {
        public static final int ALL_WINDOWS_DRAWN = 33;
        public static final int ANIMATION_FAILSAFE = 60;
        public static final int APP_FREEZE_TIMEOUT = 17;
        public static final int BOOT_TIMEOUT = 23;
        public static final int CHECK_IF_BOOT_ANIMATION_FINISHED = 37;
        public static final int CLIENT_FREEZE_TIMEOUT = 30;
        public static final int ENABLE_SCREEN = 16;
        public static final int FORCE_GC = 15;
        public static final int INSETS_CHANGED = 66;
        public static final int LAYOUT_AND_ASSIGN_WINDOW_LAYERS_IF_NEEDED = 63;
        public static final int NEW_ANIMATOR_SCALE = 34;
        public static final int NOTIFY_ACTIVITY_DRAWN = 32;
        public static final int ON_POINTER_DOWN_OUTSIDE_FOCUS = 62;
        public static final int PERSIST_ANIMATION_SCALE = 14;
        public static final int RECOMPUTE_FOCUS = 61;
        public static final int REPARENT_TASK_TO_DEFAULT_DISPLAY = 65;
        public static final int REPORT_HARD_KEYBOARD_STATUS_CHANGE = 22;
        public static final int REPORT_WINDOWS_CHANGE = 19;
        public static final int RESET_ANR_MESSAGE = 38;
        public static final int RESTORE_POINTER_ICON = 55;
        public static final int SET_HAS_OVERLAY_UI = 58;
        public static final int SHOW_EMULATOR_DISPLAY_OVERLAY = 36;
        public static final int SHOW_STRICT_MODE_VIOLATION = 25;
        public static final int UNUSED = 0;
        public static final int UPDATE_ANIMATION_SCALE = 51;
        public static final int UPDATE_MULTI_WINDOW_STACKS = 41;
        public static final int WAITING_FOR_DRAWN_TIMEOUT = 24;
        public static final int WALLPAPER_DRAW_PENDING_TIMEOUT = 39;
        public static final int WINDOW_FREEZE_TIMEOUT = 11;
        public static final int WINDOW_HIDE_TIMEOUT = 52;
        public static final int WINDOW_REPLACEMENT_TIMEOUT = 46;
        public static final int WINDOW_STATE_BLAST_SYNC_TIMEOUT = 64;

        H() {
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            Runnable callback;
            Runnable callback2;
            boolean bootAnimationComplete;
            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                Slog.v("WindowManager", "handleMessage: entry what=" + msg.what);
            }
            switch (msg.what) {
                case 11:
                    DisplayContent displayContent = (DisplayContent) msg.obj;
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            displayContent.onWindowFreezeTimeout();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 14:
                    Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "window_animation_scale", WindowManagerService.this.mWindowAnimationScaleSetting);
                    Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "transition_animation_scale", WindowManagerService.this.mTransitionAnimationScaleSetting);
                    Settings.Global.putFloat(WindowManagerService.this.mContext.getContentResolver(), "animator_duration_scale", WindowManagerService.this.mAnimatorDurationScaleSetting);
                    break;
                case 15:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerService.this.mAnimator.isAnimationScheduled()) {
                                sendEmptyMessageDelayed(15, 2000L);
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            } else if (WindowManagerService.this.mDisplayFrozen) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return;
                            } else {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                Runtime.getRuntime().gc();
                                break;
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                case 16:
                    WindowManagerService.this.performEnableScreen();
                    break;
                case 17:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (ProtoLogCache.WM_ERROR_enabled) {
                                Object[] objArr = null;
                                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -322035974, 0, "App freeze timeout expired.", (Object[]) null);
                            }
                            WindowManagerService.this.mWindowsFreezingScreen = 2;
                            for (int i = WindowManagerService.this.mAppFreezeListeners.size() - 1; i >= 0; i--) {
                                WindowManagerService.this.mAppFreezeListeners.get(i).onAppFreezeTimeout();
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 19:
                    if (WindowManagerService.this.mWindowsChanged) {
                        synchronized (WindowManagerService.this.mGlobalLock) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                WindowManagerService.this.mWindowsChanged = false;
                            } finally {
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        WindowManagerService.this.notifyWindowsChanged();
                        break;
                    }
                    break;
                case 22:
                    WindowManagerService.this.notifyHardKeyboardStatusChange();
                    break;
                case 23:
                    WindowManagerService.this.performBootTimeout();
                    break;
                case 24:
                    WindowContainer container = (WindowContainer) msg.obj;
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (ProtoLogCache.WM_ERROR_enabled) {
                                String protoLogParam0 = String.valueOf(container.mWaitingForDrawn);
                                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1526645239, 0, "Timeout waiting for drawn: undrawn=%s", new Object[]{protoLogParam0});
                            }
                            TranFoldWMCustody.instance().forceAllWindowsDrawnDone(container);
                            container.mWaitingForDrawn.clear();
                            callback = WindowManagerService.this.mWaitingForDrawnCallbacks.remove(container);
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (callback != null) {
                        callback.run();
                        break;
                    }
                    break;
                case 25:
                    WindowManagerService.this.showStrictModeViolation(msg.arg1, msg.arg2);
                    break;
                case 30:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerService.this.mClientFreezingScreen) {
                                WindowManagerService.this.mClientFreezingScreen = false;
                                WindowManagerService.this.mLastFinishedFreezeSource = "client-timeout";
                                WindowManagerService.this.stopFreezingDisplayLocked();
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 32:
                    ActivityRecord activity = (ActivityRecord) msg.obj;
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (activity.isAttached()) {
                                activity.getRootTask().notifyActivityDrawnLocked(activity);
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 33:
                    WindowContainer container2 = (WindowContainer) msg.obj;
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            TranFoldWMCustody.instance().forceAllWindowsDrawnDone(container2);
                            callback2 = WindowManagerService.this.mWaitingForDrawnCallbacks.remove(container2);
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (callback2 != null) {
                        callback2.run();
                        break;
                    }
                    break;
                case 34:
                    float scale = WindowManagerService.this.getCurrentAnimatorScale();
                    ValueAnimator.setDurationScale(scale);
                    Session session = (Session) msg.obj;
                    if (session != null) {
                        try {
                            session.mCallback.onAnimatorScaleChanged(scale);
                            break;
                        } catch (RemoteException e) {
                            break;
                        }
                    } else {
                        ArrayList<IWindowSessionCallback> callbacks = new ArrayList<>();
                        synchronized (WindowManagerService.this.mGlobalLock) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                for (int i2 = 0; i2 < WindowManagerService.this.mSessions.size(); i2++) {
                                    callbacks.add(WindowManagerService.this.mSessions.valueAt(i2).mCallback);
                                }
                            } finally {
                                WindowManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        for (int i3 = 0; i3 < callbacks.size(); i3++) {
                            try {
                                callbacks.get(i3).onAnimatorScaleChanged(scale);
                            } catch (RemoteException e2) {
                            }
                        }
                        break;
                    }
                case 36:
                    WindowManagerService.this.showEmulatorDisplayOverlay();
                    break;
                case 37:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (ProtoLogCache.WM_DEBUG_BOOT_enabled) {
                                Object[] objArr2 = null;
                                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_BOOT, 2034780299, 0, (String) null, (Object[]) null);
                            }
                            bootAnimationComplete = WindowManagerService.this.checkBootAnimationCompleteLocked();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (bootAnimationComplete) {
                        WindowManagerService.this.performEnableScreen();
                        break;
                    }
                    break;
                case 38:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WindowManagerService.this.mLastANRState = null;
                            WindowManagerService.this.mAtmService.mLastANRState = null;
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 39:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WallpaperController wallpaperController = (WallpaperController) msg.obj;
                            if (wallpaperController != null && wallpaperController.processWallpaperDrawPendingTimeout()) {
                                WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 41:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            DisplayContent displayContent2 = (DisplayContent) msg.obj;
                            if (displayContent2 != null) {
                                displayContent2.adjustForImeIfNeeded();
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 46:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            for (int i4 = WindowManagerService.this.mWindowReplacementTimeouts.size() - 1; i4 >= 0; i4--) {
                                WindowManagerService.this.mWindowReplacementTimeouts.get(i4).onWindowReplacementTimeout();
                            }
                            WindowManagerService.this.mWindowReplacementTimeouts.clear();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 51:
                    int mode = msg.arg1;
                    switch (mode) {
                        case 0:
                            WindowManagerService windowManagerService = WindowManagerService.this;
                            windowManagerService.mWindowAnimationScaleSetting = Settings.Global.getFloat(windowManagerService.mContext.getContentResolver(), "window_animation_scale", WindowManagerService.this.mWindowAnimationScaleSetting);
                            break;
                        case 1:
                            WindowManagerService windowManagerService2 = WindowManagerService.this;
                            windowManagerService2.mTransitionAnimationScaleSetting = Settings.Global.getFloat(windowManagerService2.mContext.getContentResolver(), "transition_animation_scale", WindowManagerService.this.mTransitionAnimationScaleSetting);
                            break;
                        case 2:
                            WindowManagerService windowManagerService3 = WindowManagerService.this;
                            windowManagerService3.mAnimatorDurationScaleSetting = Settings.Global.getFloat(windowManagerService3.mContext.getContentResolver(), "animator_duration_scale", WindowManagerService.this.mAnimatorDurationScaleSetting);
                            WindowManagerService.this.dispatchNewAnimatorScaleLocked(null);
                            break;
                    }
                case 52:
                    WindowState window = (WindowState) msg.obj;
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            window.mAttrs.flags &= -129;
                            window.hidePermanentlyLw();
                            window.setDisplayLayoutNeeded();
                            WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 55:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WindowManagerService.this.restorePointerIconLocked((DisplayContent) msg.obj, msg.arg1, msg.arg2);
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 58:
                    WindowManagerService.this.mAmInternal.setHasOverlayUi(msg.arg1, msg.arg2 == 1);
                    break;
                case 60:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerService.this.mRecentsAnimationController != null) {
                                WindowManagerService.this.mRecentsAnimationController.scheduleFailsafe();
                            }
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 61:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WindowManagerService.this.updateFocusedWindowLocked(0, true);
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 62:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            IBinder touchedToken = (IBinder) msg.obj;
                            WindowManagerService.this.onPointerDownOutsideFocusLocked(touchedToken);
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 63:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            DisplayContent displayContent3 = (DisplayContent) msg.obj;
                            displayContent3.mLayoutAndAssignWindowLayersScheduled = false;
                            displayContent3.layoutAndAssignWindowLayersIfNeeded();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 64:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            WindowState ws = (WindowState) msg.obj;
                            Slog.i("WindowManager", "Blast sync timeout: " + ws);
                            ws.immediatelyNotifyBlastSync();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 65:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            Task task = (Task) msg.obj;
                            task.reparent(WindowManagerService.this.mRoot.getDefaultTaskDisplayArea(), true);
                            task.resumeNextFocusAfterReparent();
                        } finally {
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
                case 66:
                    synchronized (WindowManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (WindowManagerService.this.mWindowsInsetsChanged > 0) {
                                WindowManagerService.this.mWindowsInsetsChanged = 0;
                                WindowManagerService.this.mWindowPlacerLocked.performSurfacePlacement();
                            }
                        } finally {
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    break;
            }
            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                Slog.v("WindowManager", "handleMessage: exit");
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void sendNewMessageDelayed(int what, Object obj, long delayMillis) {
            removeMessages(what, obj);
            sendMessageDelayed(obtainMessage(what, obj), delayMillis);
        }
    }

    public IWindowSession openSession(IWindowSessionCallback callback) {
        return new Session(this, callback);
    }

    public boolean useBLAST() {
        return this.mUseBLAST;
    }

    public boolean useBLASTSync() {
        return true;
    }

    public void getInitialDisplaySize(int displayId, Point size) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    size.x = displayContent.mInitialDisplayWidth;
                    size.y = displayContent.mInitialDisplayHeight;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void getBaseDisplaySize(int displayId, Point size) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    size.x = displayContent.mBaseDisplayWidth;
                    size.y = displayContent.mBaseDisplayHeight;
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setForcedDisplaySize(int displayId, int width, int height) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                ITranWindowManagerService.Instance().exitOneHandMode();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.setForcedSize(width, height);
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setForcedDisplayScalingMode(int displayId, int mode) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.setForcedScalingMode(mode);
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean applyForcedPropertiesForDefaultDisplay() {
        int pos;
        boolean changed = false;
        DisplayContent displayContent = getDefaultDisplayContentLocked();
        String sizeStr = Settings.Global.getString(this.mContext.getContentResolver(), "display_size_forced");
        String sizeStr2 = (sizeStr == null || sizeStr.length() == 0) ? SystemProperties.get(SIZE_OVERRIDE, (String) null) : sizeStr;
        if (sizeStr2 != null && sizeStr2.length() > 0 && (pos = sizeStr2.indexOf(44)) > 0 && sizeStr2.lastIndexOf(44) == pos) {
            try {
                int width = Integer.parseInt(sizeStr2.substring(0, pos));
                int height = Integer.parseInt(sizeStr2.substring(pos + 1));
                if (displayContent.mBaseDisplayWidth != width || displayContent.mBaseDisplayHeight != height) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam0 = width;
                        long protoLogParam1 = height;
                        ProtoLogImpl.i(ProtoLogGroup.WM_ERROR, 1115417974, 5, "FORCED DISPLAY SIZE: %dx%d", new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1)});
                    }
                    displayContent.updateBaseDisplayMetrics(width, height, displayContent.mBaseDisplayDensity, displayContent.mBaseDisplayPhysicalXDpi, displayContent.mBaseDisplayPhysicalYDpi);
                    changed = true;
                }
            } catch (NumberFormatException e) {
            }
        }
        int density = getForcedDisplayDensityForUserLocked(this.mCurrentUserId);
        if (density != 0 && density != displayContent.mBaseDisplayDensity) {
            displayContent.mBaseDisplayDensity = density;
            changed = true;
        }
        int mode = Settings.Global.getInt(this.mContext.getContentResolver(), "display_scaling_force", 0);
        if (displayContent.mDisplayScalingDisabled != (mode != 0)) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                ProtoLogImpl.i(ProtoLogGroup.WM_ERROR, 954470154, 0, "FORCED DISPLAY SCALING DISABLED", (Object[]) null);
            }
            displayContent.mDisplayScalingDisabled = true;
            return true;
        }
        return changed;
    }

    public void clearForcedDisplaySize(int displayId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.setForcedSize(displayContent.mInitialDisplayWidth, displayContent.mInitialDisplayHeight);
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getInitialDisplayDensity(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    int i = displayContent.mInitialDisplayDensity;
                    resetPriorityAfterLockedSection();
                    return i;
                }
                resetPriorityAfterLockedSection();
                return -1;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getBaseDisplayDensity(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null && displayContent.hasAccess(Binder.getCallingUid())) {
                    int i = displayContent.mBaseDisplayDensity;
                    resetPriorityAfterLockedSection();
                    return i;
                }
                resetPriorityAfterLockedSection();
                return -1;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setForcedDisplayDensityForUser(int displayId, int density, int userId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        int targetUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "setForcedDisplayDensityForUser", null);
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.setForcedDensity(density, targetUserId);
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void clearForcedDisplayDensityForUser(int displayId, int userId) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("Must hold permission android.permission.WRITE_SECURE_SETTINGS");
        }
        int callingUserId = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, true, "clearForcedDisplayDensityForUser", null);
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.setForcedDensity(displayContent.mInitialDisplayDensity, callingUserId);
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private int getForcedDisplayDensityForUserLocked(int userId) {
        String densityStr = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "display_density_forced", userId);
        if (densityStr == null || densityStr.length() == 0) {
            densityStr = SystemProperties.get(DENSITY_OVERRIDE, (String) null);
        }
        if (densityStr != null && densityStr.length() > 0) {
            try {
                return Integer.parseInt(densityStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    public void startWindowTrace() {
        this.mWindowTracing.startTrace(null);
    }

    public void stopWindowTrace() {
        this.mWindowTracing.stopTrace(null);
    }

    public void saveWindowTraceToFile() {
        this.mWindowTracing.saveForBugreport(null);
    }

    public boolean isWindowTraceEnabled() {
        return this.mWindowTracing.isEnabled();
    }

    public boolean registerCrossWindowBlurEnabledListener(ICrossWindowBlurEnabledListener listener) {
        return this.mBlurController.registerCrossWindowBlurEnabledListener(listener);
    }

    public void unregisterCrossWindowBlurEnabledListener(ICrossWindowBlurEnabledListener listener) {
        this.mBlurController.unregisterCrossWindowBlurEnabledListener(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final WindowState windowForClientLocked(Session session, IWindow client, boolean throwOnError) {
        return windowForClientLocked(session, client.asBinder(), throwOnError);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final WindowState windowForClientLocked(Session session, IBinder client, boolean throwOnError) {
        WindowState win = this.mWindowMap.get(client);
        if (WindowManagerDebugConfig.DEBUG) {
            Slog.v("WindowManager", "Looking up client " + client + ": " + win);
        }
        if (win == null) {
            if (throwOnError) {
                throw new IllegalArgumentException("Requested window " + client + " does not exist");
            }
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam0 = String.valueOf(session);
                String protoLogParam1 = String.valueOf(Debug.getCallers(3));
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -2101985723, 0, "Failed looking up window session=%s callers=%s", new Object[]{protoLogParam0, protoLogParam1});
            }
            return null;
        } else if (session != null && win.mSession != session) {
            if (throwOnError) {
                throw new IllegalArgumentException("Requested window " + client + " is in session " + win.mSession + ", not " + session);
            }
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam02 = String.valueOf(session);
                String protoLogParam12 = String.valueOf(Debug.getCallers(3));
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -2101985723, 0, "Failed looking up window session=%s callers=%s", new Object[]{protoLogParam02, protoLogParam12});
            }
            return null;
        } else {
            return win;
        }
    }

    final WindowState windowForClientLocked(IBinder client) {
        WindowState win = this.mWindowMap.get(client);
        if (WindowManagerDebugConfig.DEBUG) {
            Slog.v("WindowManager", "Looking up client " + client + ": " + win);
        }
        if (win == null) {
            try {
                String inTag = client.getInterfaceDescriptor();
                for (Map.Entry<IBinder, WindowState> entry : this.mWindowMap.entrySet()) {
                    if (entry.getValue().getWindowTag().equals(inTag)) {
                        return entry.getValue();
                    }
                }
                return null;
            } catch (RemoteException e) {
                return null;
            }
        }
        return win;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void makeWindowFreezingScreenIfNeededLocked(WindowState w) {
        if (!w.mToken.okToDisplay() && this.mWindowsFreezingScreen != 2) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                String protoLogParam0 = String.valueOf(w);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -1632122349, 0, (String) null, new Object[]{protoLogParam0});
            }
            w.setOrientationChanging(true);
            if (this.mWindowsFreezingScreen == 0) {
                this.mWindowsFreezingScreen = 1;
                this.mH.sendNewMessageDelayed(11, w.getDisplayContent(), 2000L);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkDrawnWindowsLocked() {
        if (this.mWaitingForDrawnCallbacks.isEmpty()) {
            return;
        }
        this.mWaitingForDrawnCallbacks.forEach(new BiConsumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda11
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                WindowManagerService.this.m8504x8d782270((WindowContainer) obj, (Runnable) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$checkDrawnWindowsLocked$9$com-android-server-wm-WindowManagerService  reason: not valid java name */
    public /* synthetic */ void m8504x8d782270(WindowContainer container, Runnable callback) {
        for (int j = container.mWaitingForDrawn.size() - 1; j >= 0; j--) {
            WindowState win = container.mWaitingForDrawn.get(j);
            if (ProtoLogCache.WM_DEBUG_SCREEN_ON_enabled) {
                String protoLogParam0 = String.valueOf(win);
                boolean protoLogParam1 = win.mRemoved;
                boolean protoLogParam2 = win.isVisible();
                boolean protoLogParam3 = win.mHasSurface;
                long protoLogParam4 = win.mWinAnimator.mDrawState;
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_SCREEN_ON, 892244061, 508, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), Long.valueOf(protoLogParam4)});
            }
            if (win.mRemoved || !win.mHasSurface || !win.isVisibleByPolicy()) {
                if (ProtoLogCache.WM_DEBUG_SCREEN_ON_enabled) {
                    String protoLogParam02 = String.valueOf(win);
                    ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_SCREEN_ON, 463993897, 0, (String) null, new Object[]{protoLogParam02});
                }
                container.mWaitingForDrawn.remove(win);
            } else if (win.hasDrawn()) {
                if (ProtoLogCache.WM_DEBUG_SCREEN_ON_enabled) {
                    String protoLogParam03 = String.valueOf(win);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_SCREEN_ON, 1401700824, 0, (String) null, new Object[]{protoLogParam03});
                }
                container.mWaitingForDrawn.remove(win);
            }
        }
        if (!TranFoldWMCustody.instance().notifyAllWindowsDrawnDone(container) && container.mWaitingForDrawn.isEmpty()) {
            if (ProtoLogCache.WM_DEBUG_SCREEN_ON_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_SCREEN_ON, 665256544, 0, (String) null, (Object[]) null);
            }
            this.mH.removeMessages(24, container);
            H h = this.mH;
            h.sendMessage(h.obtainMessage(33, container));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHoldScreenLocked(Session newHoldScreen) {
        boolean hold = (newHoldScreen == null || ITranWindowManagerService.Instance().getHasHiddedKeepAwake(this.mRoot.mHoldScreenWindow, this.mHoldingScreenOn, newHoldScreen)) ? false : true;
        if (hold && this.mHoldingScreenOn != newHoldScreen) {
            this.mHoldingScreenWakeLock.setWorkSource(new WorkSource(newHoldScreen.mUid));
        }
        this.mHoldingScreenOn = newHoldScreen;
        boolean state = this.mHoldingScreenWakeLock.isHeld();
        if (hold != state) {
            if (hold) {
                if (ProtoLogCache.WM_DEBUG_KEEP_SCREEN_ON_enabled) {
                    String protoLogParam0 = String.valueOf(this.mRoot.mHoldScreenWindow);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_KEEP_SCREEN_ON, 2096635066, 0, (String) null, new Object[]{protoLogParam0});
                }
                if (!ProtoLogImpl.isEnabled(ProtoLogGroup.WM_DEBUG_KEEP_SCREEN_ON) && Build.IS_DEBUG_ENABLE) {
                    Slog.i("WindowManager", "Acquiring screen wakelock due to " + this.mRoot.mHoldScreenWindow);
                }
                this.mLastWakeLockHoldingWindow = this.mRoot.mHoldScreenWindow;
                this.mLastWakeLockObscuringWindow = null;
                this.mHoldingScreenWakeLock.acquire();
                this.mPolicy.keepScreenOnStartedLw();
                return;
            }
            if (ProtoLogCache.WM_DEBUG_KEEP_SCREEN_ON_enabled) {
                String protoLogParam02 = String.valueOf(this.mRoot.mObscuringWindow);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_KEEP_SCREEN_ON, -2146181682, 0, (String) null, new Object[]{protoLogParam02});
            }
            if (!ProtoLogImpl.isEnabled(ProtoLogGroup.WM_DEBUG_KEEP_SCREEN_ON) && Build.IS_DEBUG_ENABLE) {
                Slog.i("WindowManager", "Releasing screen wakelock, obscured by " + this.mRoot.mObscuringWindow);
            }
            this.mLastWakeLockHoldingWindow = null;
            this.mLastWakeLockObscuringWindow = this.mRoot.mObscuringWindow;
            this.mPolicy.keepScreenOnStoppedLw();
            this.mHoldingScreenWakeLock.release();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestTraversal() {
        this.mWindowPlacerLocked.requestTraversal();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleAnimationLocked() {
        WindowAnimator windowAnimator = this.mAnimator;
        if (windowAnimator != null) {
            windowAnimator.scheduleAnimation();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateFocusedWindowLocked(int mode, boolean updateInputWindows) {
        Trace.traceBegin(32L, "wmUpdateFocus");
        boolean changed = this.mRoot.updateFocusedWindowLocked(mode, updateInputWindows);
        Trace.traceEnd(32L);
        return changed;
    }

    void startFreezingDisplay(int exitAnim, int enterAnim) {
        startFreezingDisplay(exitAnim, enterAnim, getDefaultDisplayContentLocked());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startFreezingDisplay(int exitAnim, int enterAnim, DisplayContent displayContent) {
        startFreezingDisplay(exitAnim, enterAnim, displayContent, -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startFreezingDisplay(int exitAnim, int enterAnim, DisplayContent displayContent, int overrideOriginalRotation) {
        if (this.mDisplayFrozen || displayContent.getDisplayRotation().isRotatingSeamlessly() || !displayContent.isReady() || !displayContent.getDisplayPolicy().isScreenOnFully() || displayContent.getDisplayInfo().state == 1 || !displayContent.okToAnimate()) {
            return;
        }
        Trace.traceBegin(32L, "WMS.doStartFreezingDisplay");
        doStartFreezingDisplay(exitAnim, enterAnim, displayContent, overrideOriginalRotation);
        Trace.traceEnd(32L);
    }

    private void doStartFreezingDisplay(int exitAnim, int enterAnim, DisplayContent displayContent, int overrideOriginalRotation) {
        int originalRotation;
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            long protoLogParam0 = exitAnim;
            long protoLogParam1 = enterAnim;
            String protoLogParam2 = String.valueOf(Debug.getCallers(8));
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ORIENTATION, 9803449, 5, (String) null, new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1), protoLogParam2});
        }
        TranFoldWMCustody.instance().doStartFreezingDisplay(exitAnim, enterAnim, displayContent, overrideOriginalRotation);
        this.mScreenFrozenLock.acquire();
        this.mAtmService.startLaunchPowerMode(2);
        this.mDisplayFrozen = true;
        this.mDisplayFreezeTime = SystemClock.elapsedRealtime();
        this.mLastFinishedFreezeSource = null;
        this.mFrozenDisplayId = displayContent.getDisplayId();
        this.mInputManagerCallback.freezeInputDispatchingLw();
        if (displayContent.mAppTransition.isTransitionSet()) {
            displayContent.mAppTransition.freeze();
        }
        this.mLatencyTracker.onActionStart(6);
        this.mExitAnimId = exitAnim;
        this.mEnterAnimId = enterAnim;
        displayContent.updateDisplayInfo();
        if (overrideOriginalRotation != -1) {
            originalRotation = overrideOriginalRotation;
        } else {
            originalRotation = displayContent.getDisplayInfo().rotation;
        }
        displayContent.setRotationAnimation(new ScreenRotationAnimation(displayContent, originalRotation));
        this.mPowerHalManager.setRotationBoost(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopFreezingDisplayLocked() {
        boolean waitingForRemoteRotation;
        boolean waitingForConfig;
        int numOpeningApps;
        if (this.mDisplayFrozen) {
            DisplayContent displayContent = this.mRoot.getDisplayContent(this.mFrozenDisplayId);
            if (displayContent != null) {
                numOpeningApps = displayContent.mOpeningApps.size();
                waitingForConfig = displayContent.mWaitingForConfig;
                waitingForRemoteRotation = displayContent.getDisplayRotation().isWaitingForRemoteRotation();
            } else {
                waitingForRemoteRotation = false;
                waitingForConfig = false;
                numOpeningApps = 0;
            }
            if (!waitingForConfig && !waitingForRemoteRotation && this.mAppsFreezingScreen <= 0 && this.mWindowsFreezingScreen != 1 && !this.mClientFreezingScreen && numOpeningApps <= 0) {
                Trace.traceBegin(32L, "WMS.doStopFreezingDisplayLocked-" + this.mLastFinishedFreezeSource);
                doStopFreezingDisplayLocked(displayContent);
                Trace.traceEnd(32L);
            } else if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                boolean protoLogParam0 = waitingForConfig;
                boolean protoLogParam1 = waitingForRemoteRotation;
                long protoLogParam2 = this.mAppsFreezingScreen;
                long protoLogParam3 = this.mWindowsFreezingScreen;
                boolean protoLogParam4 = this.mClientFreezingScreen;
                long protoLogParam5 = numOpeningApps;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1246035185, 1887, (String) null, new Object[]{Boolean.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1), Long.valueOf(protoLogParam2), Long.valueOf(protoLogParam3), Boolean.valueOf(protoLogParam4), Long.valueOf(protoLogParam5)});
            }
        }
    }

    private void doStopFreezingDisplayLocked(DisplayContent displayContent) {
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ORIENTATION, 355720268, 0, (String) null, (Object[]) null);
        }
        this.mFrozenDisplayId = -1;
        this.mDisplayFrozen = false;
        this.mInputManagerCallback.thawInputDispatchingLw();
        this.mLastDisplayFreezeDuration = (int) (SystemClock.elapsedRealtime() - this.mDisplayFreezeTime);
        StringBuilder sb = new StringBuilder(128);
        sb.append("Screen frozen for ");
        TimeUtils.formatDuration(this.mLastDisplayFreezeDuration, sb);
        if (this.mLastFinishedFreezeSource != null) {
            sb.append(" due to ");
            sb.append(this.mLastFinishedFreezeSource);
        }
        if (ProtoLogCache.WM_ERROR_enabled) {
            String protoLogParam0 = String.valueOf(sb.toString());
            ProtoLogImpl.i(ProtoLogGroup.WM_ERROR, -583031528, 0, "%s", new Object[]{protoLogParam0});
        }
        this.mH.removeMessages(17);
        this.mH.removeMessages(30);
        boolean updateRotation = false;
        ScreenRotationAnimation screenRotationAnimation = displayContent == null ? null : displayContent.getRotationAnimation();
        if (screenRotationAnimation != null && screenRotationAnimation.hasScreenshot()) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1634557978, 0, (String) null, (Object[]) null);
            }
            DisplayInfo displayInfo = displayContent.getDisplayInfo();
            if (!displayContent.getDisplayRotation().validateRotationAnimation(this.mExitAnimId, this.mEnterAnimId, false)) {
                this.mEnterAnimId = 0;
                this.mExitAnimId = 0;
            }
            if (screenRotationAnimation.dismiss(this.mTransaction, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, getTransitionAnimationScaleLocked(), displayInfo.logicalWidth, displayInfo.logicalHeight, this.mExitAnimId, this.mEnterAnimId)) {
                this.mTransaction.apply();
            } else {
                screenRotationAnimation.kill();
                displayContent.setRotationAnimation(null);
                updateRotation = true;
            }
        } else {
            if (screenRotationAnimation != null) {
                screenRotationAnimation.kill();
                displayContent.setRotationAnimation(null);
            }
            updateRotation = true;
        }
        boolean configChanged = displayContent != null && displayContent.updateOrientation();
        this.mH.removeMessages(15);
        this.mH.sendEmptyMessageDelayed(15, 2000L);
        this.mScreenFrozenLock.release();
        if (updateRotation && displayContent != null) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ORIENTATION, -783405930, 0, (String) null, (Object[]) null);
            }
            configChanged |= displayContent.updateRotationUnchecked();
        }
        if (configChanged) {
            displayContent.sendNewConfiguration();
        }
        this.mAtmService.endLaunchPowerMode(2);
        this.mLatencyTracker.onActionEnd(6);
        TranFoldWMCustody.instance().doStopFreezingDisplayLocked(updateRotation, configChanged, displayContent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getPropertyInt(String[] tokens, int index, int defUnits, int defDps, DisplayMetrics dm) {
        String str;
        if (index < tokens.length && (str = tokens[index]) != null && str.length() > 0) {
            try {
                int val = Integer.parseInt(str);
                return val;
            } catch (Exception e) {
            }
        }
        if (defUnits == 0) {
            return defDps;
        }
        int val2 = (int) TypedValue.applyDimension(defUnits, defDps, dm);
        return val2;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [6625=4] */
    void createWatermark() {
        String[] toks;
        if (this.mWatermark != null) {
            return;
        }
        File file = new File("/system/etc/setup.conf");
        FileInputStream in = null;
        DataInputStream ind = null;
        try {
            try {
                try {
                    try {
                        in = new FileInputStream(file);
                        ind = new DataInputStream(in);
                        String line = ind.readLine();
                        if (line != null && (toks = line.split("%")) != null && toks.length > 0) {
                            DisplayContent displayContent = getDefaultDisplayContentLocked();
                            this.mWatermark = new Watermark(displayContent, displayContent.mRealDisplayMetrics, toks, this.mTransaction);
                            this.mTransaction.apply();
                        }
                        ind.close();
                    } catch (IOException e) {
                    }
                } catch (FileNotFoundException e2) {
                    if (ind == null) {
                        if (in != null) {
                            in.close();
                            return;
                        }
                        return;
                    }
                    ind.close();
                }
            } catch (IOException e3) {
                if (ind == null) {
                    if (in != null) {
                        in.close();
                        return;
                    }
                    return;
                }
                ind.close();
            } catch (Throwable th) {
                if (ind != null) {
                    try {
                        ind.close();
                    } catch (IOException e4) {
                    }
                } else if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e5) {
                    }
                }
                throw th;
            }
        } catch (IOException e6) {
        }
    }

    public void setRecentsVisibility(boolean visible) {
        if (!checkCallingPermission("android.permission.STATUS_BAR", "setRecentsVisibility()")) {
            throw new SecurityException("Requires STATUS_BAR permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.setRecentsVisibilityLw(visible);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void hideTransientBars(int displayId) {
        if (!checkCallingPermission("android.permission.STATUS_BAR", "hideTransientBars()")) {
            throw new SecurityException("Requires STATUS_BAR permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.getInsetsPolicy().hideTransient();
                } else {
                    Slog.w("WindowManager", "hideTransientBars with invalid displayId=" + displayId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void updateStaticPrivacyIndicatorBounds(int displayId, Rect[] staticBounds) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent != null) {
                    displayContent.updatePrivacyIndicatorBounds(staticBounds);
                } else {
                    Slog.w("WindowManager", "updateStaticPrivacyIndicatorBounds with invalid displayId=" + displayId);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void setNavBarVirtualKeyHapticFeedbackEnabled(boolean enabled) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR") != 0) {
            throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mPolicy.setNavBarVirtualKeyHapticFeedbackEnabledLw(enabled);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public int getNavBarPosition(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    Slog.w("WindowManager", "getNavBarPosition with invalid displayId=" + displayId + " callers=" + Debug.getCallers(3));
                    resetPriorityAfterLockedSection();
                    return -1;
                }
                int navBarPosition = displayContent.getDisplayPolicy().getNavBarPosition();
                resetPriorityAfterLockedSection();
                return navBarPosition;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void createInputConsumer(IBinder token, String name, int displayId, InputChannel inputChannel) {
        if (!this.mAtmService.isCallerRecents(Binder.getCallingUid()) && this.mContext.checkCallingOrSelfPermission("android.permission.INPUT_CONSUMER") != 0) {
            throw new SecurityException("createInputConsumer requires INPUT_CONSUMER permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display != null) {
                    display.getInputMonitor().createInputConsumer(token, name, inputChannel, Binder.getCallingPid(), Binder.getCallingUserHandle());
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public boolean destroyInputConsumer(String name, int displayId) {
        if (!this.mAtmService.isCallerRecents(Binder.getCallingUid()) && this.mContext.checkCallingOrSelfPermission("android.permission.INPUT_CONSUMER") != 0) {
            throw new SecurityException("destroyInputConsumer requires INPUT_CONSUMER permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent display = this.mRoot.getDisplayContent(displayId);
                if (display != null) {
                    boolean destroyInputConsumer = display.getInputMonitor().destroyInputConsumer(name);
                    resetPriorityAfterLockedSection();
                    return destroyInputConsumer;
                }
                resetPriorityAfterLockedSection();
                return false;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public Region getCurrentImeTouchRegion() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.RESTRICTED_VR_ACCESS") != 0) {
            throw new SecurityException("getCurrentImeTouchRegion is restricted to VR services");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                Region r = new Region();
                for (int i = this.mRoot.mChildren.size() - 1; i >= 0; i--) {
                    DisplayContent displayContent = (DisplayContent) this.mRoot.mChildren.get(i);
                    if (displayContent.mInputMethodWindow != null) {
                        displayContent.mInputMethodWindow.getTouchableRegion(r);
                        resetPriorityAfterLockedSection();
                        return r;
                    }
                }
                resetPriorityAfterLockedSection();
                return r;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean hasNavigationBar(int displayId) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean hasNavigationBar = dc.getDisplayPolicy().hasNavigationBar();
                resetPriorityAfterLockedSection();
                return hasNavigationBar;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void lockNow(Bundle options) {
        this.mPolicy.lockNow(options);
    }

    public void showRecentApps() {
        this.mPolicy.showRecentApps();
    }

    public boolean isSafeModeEnabled() {
        return this.mSafeMode;
    }

    public boolean clearWindowContentFrameStats(IBinder token) {
        if (!checkCallingPermission("android.permission.FRAME_STATS", "clearWindowContentFrameStats()")) {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState windowState = this.mWindowMap.get(token);
                if (windowState == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                WindowSurfaceController surfaceController = windowState.mWinAnimator.mSurfaceController;
                if (surfaceController == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean clearWindowContentFrameStats = surfaceController.clearWindowContentFrameStats();
                resetPriorityAfterLockedSection();
                return clearWindowContentFrameStats;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public WindowContentFrameStats getWindowContentFrameStats(IBinder token) {
        if (!checkCallingPermission("android.permission.FRAME_STATS", "getWindowContentFrameStats()")) {
            throw new SecurityException("Requires FRAME_STATS permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState windowState = this.mWindowMap.get(token);
                if (windowState == null) {
                    resetPriorityAfterLockedSection();
                    return null;
                }
                WindowSurfaceController surfaceController = windowState.mWinAnimator.mSurfaceController;
                if (surfaceController == null) {
                    resetPriorityAfterLockedSection();
                    return null;
                }
                if (this.mTempWindowRenderStats == null) {
                    this.mTempWindowRenderStats = new WindowContentFrameStats();
                }
                WindowContentFrameStats stats = this.mTempWindowRenderStats;
                if (surfaceController.getWindowContentFrameStats(stats)) {
                    resetPriorityAfterLockedSection();
                    return stats;
                }
                resetPriorityAfterLockedSection();
                return null;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void dumpPolicyLocked(PrintWriter pw, String[] args, boolean dumpAll) {
        pw.println("WINDOW MANAGER POLICY STATE (dumpsys window policy)");
        this.mPolicy.dump("    ", pw, args);
    }

    private void dumpAnimatorLocked(PrintWriter pw, String[] args, boolean dumpAll) {
        pw.println("WINDOW MANAGER ANIMATOR STATE (dumpsys window animator)");
        this.mAnimator.dumpLocked(pw, "    ", dumpAll);
    }

    private void dumpTokensLocked(PrintWriter pw, boolean dumpAll) {
        pw.println("WINDOW MANAGER TOKENS (dumpsys window tokens)");
        this.mRoot.dumpTokens(pw, dumpAll);
    }

    private void dumpHighRefreshRateBlacklist(PrintWriter pw) {
        pw.println("WINDOW MANAGER HIGH REFRESH RATE BLACKLIST (dumpsys window refresh)");
        this.mHighRefreshRateDenylist.dump(pw);
    }

    private void dumpTraceStatus(PrintWriter pw) {
        pw.println("WINDOW MANAGER TRACE (dumpsys window trace)");
        pw.print(this.mWindowTracing.getStatus() + "\n");
    }

    private void dumpLogStatus(PrintWriter pw) {
        pw.println("WINDOW MANAGER LOGGING (dumpsys window logging)");
        pw.println(ProtoLogImpl.getSingleInstance().getStatus());
    }

    private void dumpSessionsLocked(PrintWriter pw, boolean dumpAll) {
        pw.println("WINDOW MANAGER SESSIONS (dumpsys window sessions)");
        for (int i = 0; i < this.mSessions.size(); i++) {
            Session s = this.mSessions.valueAt(i);
            pw.print("  Session ");
            pw.print(s);
            pw.println(':');
            s.dump(pw, "    ");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebugLocked(ProtoOutputStream proto, int logLevel) {
        this.mPolicy.dumpDebug(proto, 1146756268033L);
        this.mRoot.dumpDebug(proto, 1146756268034L, logLevel);
        DisplayContent topFocusedDisplayContent = this.mRoot.getTopFocusedDisplayContent();
        if (topFocusedDisplayContent.mCurrentFocus != null) {
            topFocusedDisplayContent.mCurrentFocus.writeIdentifierToProto(proto, 1146756268035L);
        }
        if (topFocusedDisplayContent.mFocusedApp != null) {
            topFocusedDisplayContent.mFocusedApp.writeNameToProto(proto, 1138166333444L);
        }
        WindowState imeWindow = this.mRoot.getCurrentInputMethodWindow();
        if (imeWindow != null) {
            imeWindow.writeIdentifierToProto(proto, 1146756268037L);
        }
        proto.write(1133871366150L, this.mDisplayFrozen);
        proto.write(1120986464265L, topFocusedDisplayContent.getDisplayId());
        proto.write(1133871366154L, this.mHardKeyboardAvailable);
        proto.write(1133871366155L, true);
    }

    private void dumpWindowsLocked(PrintWriter pw, boolean dumpAll, ArrayList<WindowState> windows) {
        pw.println("WINDOW MANAGER WINDOWS (dumpsys window windows)");
        dumpWindowsNoHeaderLocked(pw, dumpAll, windows);
    }

    private void dumpWindowsNoHeaderLocked(final PrintWriter pw, boolean dumpAll, ArrayList<WindowState> windows) {
        this.mRoot.dumpWindowsNoHeader(pw, dumpAll, windows);
        if (!this.mHidingNonSystemOverlayWindows.isEmpty()) {
            pw.println();
            pw.println("  Hiding System Alert Windows:");
            for (int i = this.mHidingNonSystemOverlayWindows.size() - 1; i >= 0; i--) {
                WindowState w = this.mHidingNonSystemOverlayWindows.get(i);
                pw.print("  #");
                pw.print(i);
                pw.print(' ');
                pw.print(w);
                if (dumpAll) {
                    pw.println(":");
                    w.dump(pw, "    ", true);
                } else {
                    pw.println();
                }
            }
        }
        ArrayList<WindowState> arrayList = this.mForceRemoves;
        if (arrayList != null && arrayList.size() > 0) {
            pw.println();
            pw.println("  Windows force removing:");
            for (int i2 = this.mForceRemoves.size() - 1; i2 >= 0; i2--) {
                WindowState w2 = this.mForceRemoves.get(i2);
                pw.print("  Removing #");
                pw.print(i2);
                pw.print(' ');
                pw.print(w2);
                if (dumpAll) {
                    pw.println(":");
                    w2.dump(pw, "    ", true);
                } else {
                    pw.println();
                }
            }
        }
        if (this.mDestroySurface.size() > 0) {
            pw.println();
            pw.println("  Windows waiting to destroy their surface:");
            for (int i3 = this.mDestroySurface.size() - 1; i3 >= 0; i3--) {
                WindowState w3 = this.mDestroySurface.get(i3);
                if (windows == null || windows.contains(w3)) {
                    pw.print("  Destroy #");
                    pw.print(i3);
                    pw.print(' ');
                    pw.print(w3);
                    if (dumpAll) {
                        pw.println(":");
                        w3.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (this.mResizingWindows.size() > 0) {
            pw.println();
            pw.println("  Windows waiting to resize:");
            for (int i4 = this.mResizingWindows.size() - 1; i4 >= 0; i4--) {
                WindowState w4 = this.mResizingWindows.get(i4);
                if (windows == null || windows.contains(w4)) {
                    pw.print("  Resizing #");
                    pw.print(i4);
                    pw.print(' ');
                    pw.print(w4);
                    if (dumpAll) {
                        pw.println(":");
                        w4.dump(pw, "    ", true);
                    } else {
                        pw.println();
                    }
                }
            }
        }
        if (!this.mWaitingForDrawnCallbacks.isEmpty()) {
            pw.println();
            pw.println("  Clients waiting for these windows to be drawn:");
            this.mWaitingForDrawnCallbacks.forEach(new BiConsumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda22
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    WindowManagerService.lambda$dumpWindowsNoHeaderLocked$10(pw, (WindowContainer) obj, (Runnable) obj2);
                }
            });
        }
        pw.println();
        pw.print("  mGlobalConfiguration=");
        pw.println(this.mRoot.getConfiguration());
        pw.print("  mHasPermanentDpad=");
        pw.println(this.mHasPermanentDpad);
        this.mRoot.dumpTopFocusedDisplayId(pw);
        this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda23
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowManagerService.lambda$dumpWindowsNoHeaderLocked$11(pw, (DisplayContent) obj);
            }
        });
        pw.print("  mInTouchMode=");
        pw.println(this.mInTouchMode);
        pw.print("  mBlurEnabled=");
        pw.println(this.mBlurController.getBlurEnabled());
        pw.print("  mLastDisplayFreezeDuration=");
        TimeUtils.formatDuration(this.mLastDisplayFreezeDuration, pw);
        if (this.mLastFinishedFreezeSource != null) {
            pw.print(" due to ");
            pw.print(this.mLastFinishedFreezeSource);
        }
        pw.println();
        pw.print("  mLastWakeLockHoldingWindow=");
        pw.print(this.mLastWakeLockHoldingWindow);
        pw.print(" mLastWakeLockObscuringWindow=");
        pw.print(this.mLastWakeLockObscuringWindow);
        pw.println();
        this.mInputManagerCallback.dump(pw, "  ");
        this.mTaskSnapshotController.dump(pw, "  ");
        if (this.mAccessibilityController.hasCallbacks()) {
            this.mAccessibilityController.dump(pw, "  ");
        }
        if (dumpAll) {
            WindowState imeWindow = this.mRoot.getCurrentInputMethodWindow();
            if (imeWindow != null) {
                pw.print("  mInputMethodWindow=");
                pw.println(imeWindow);
            }
            this.mWindowPlacerLocked.dump(pw, "  ");
            pw.print("  mSystemBooted=");
            pw.print(this.mSystemBooted);
            pw.print(" mDisplayEnabled=");
            pw.println(this.mDisplayEnabled);
            this.mRoot.dumpLayoutNeededDisplayIds(pw);
            pw.print("  mTransactionSequence=");
            pw.println(this.mTransactionSequence);
            pw.print("  mDisplayFrozen=");
            pw.print(this.mDisplayFrozen);
            pw.print(" windows=");
            pw.print(this.mWindowsFreezingScreen);
            pw.print(" client=");
            pw.print(this.mClientFreezingScreen);
            pw.print(" apps=");
            pw.print(this.mAppsFreezingScreen);
            DisplayContent defaultDisplayContent = getDefaultDisplayContentLocked();
            pw.print("  mRotation=");
            pw.print(defaultDisplayContent.getRotation());
            pw.print("  mLastOrientation=");
            pw.println(defaultDisplayContent.getLastOrientation());
            pw.print(" waitingForConfig=");
            pw.println(defaultDisplayContent.mWaitingForConfig);
            pw.print("  Animation settings: disabled=");
            pw.print(this.mAnimationsDisabled);
            pw.print(" window=");
            pw.print(this.mWindowAnimationScaleSetting);
            pw.print(" transition=");
            pw.print(this.mTransitionAnimationScaleSetting);
            pw.print(" animator=");
            pw.println(this.mAnimatorDurationScaleSetting);
            if (this.mRecentsAnimationController != null) {
                pw.print("  mRecentsAnimationController=");
                pw.println(this.mRecentsAnimationController);
                this.mRecentsAnimationController.dump(pw, "    ");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpWindowsNoHeaderLocked$10(PrintWriter pw, WindowContainer wc, Runnable callback) {
        pw.print("  WindowContainer ");
        pw.println(wc.getName());
        for (int i = wc.mWaitingForDrawn.size() - 1; i >= 0; i--) {
            WindowState win = wc.mWaitingForDrawn.get(i);
            pw.print("  Waiting #");
            pw.print(i);
            pw.print(' ');
            pw.print(win);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpWindowsNoHeaderLocked$11(PrintWriter pw, DisplayContent dc) {
        int displayId = dc.getDisplayId();
        InsetsControlTarget imeLayeringTarget = dc.getImeTarget(0);
        InputTarget imeInputTarget = dc.getImeInputTarget();
        InsetsControlTarget imeControlTarget = dc.getImeTarget(2);
        if (imeLayeringTarget != null) {
            pw.print("  imeLayeringTarget in display# ");
            pw.print(displayId);
            pw.print(' ');
            pw.println(imeLayeringTarget);
        }
        if (imeInputTarget != null) {
            pw.print("  imeInputTarget in display# ");
            pw.print(displayId);
            pw.print(' ');
            pw.println(imeInputTarget);
        }
        if (imeControlTarget != null) {
            pw.print("  imeControlTarget in display# ");
            pw.print(displayId);
            pw.print(' ');
            pw.println(imeControlTarget);
        }
        pw.print("  Minimum task size of display#");
        pw.print(displayId);
        pw.print(' ');
        pw.print(dc.mMinSizeOfResizeableTaskDp);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    private boolean dumpWindows(PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        final ArrayList<WindowState> windows = new ArrayList<>();
        if ("apps".equals(name) || "visible".equals(name) || "visible-apps".equals(name)) {
            final boolean appsOnly = name.contains("apps");
            final boolean visibleOnly = name.contains("visible");
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    if (appsOnly) {
                        this.mRoot.dumpDisplayContents(pw);
                    }
                    this.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda26
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            WindowManagerService.lambda$dumpWindows$12(visibleOnly, appsOnly, windows, (WindowState) obj);
                        }
                    }, true);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            resetPriorityAfterLockedSection();
        } else {
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    this.mRoot.getWindowsByName(windows, name);
                } finally {
                }
            }
            resetPriorityAfterLockedSection();
        }
        if (windows.size() <= 0) {
            return false;
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                dumpWindowsLocked(pw, dumpAll, windows);
            } finally {
            }
        }
        resetPriorityAfterLockedSection();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dumpWindows$12(boolean visibleOnly, boolean appsOnly, ArrayList windows, WindowState w) {
        if (!visibleOnly || w.isVisible()) {
            if (!appsOnly || w.mActivityRecord != null) {
                windows.add(w);
            }
        }
    }

    private void dumpLastANRLocked(PrintWriter pw) {
        pw.println("WINDOW MANAGER LAST ANR (dumpsys window lastanr)");
        String str = this.mLastANRState;
        if (str == null) {
            pw.println("  <no ANR has occurred since boot>");
        } else {
            pw.println(str);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveANRStateLocked(ActivityRecord activity, WindowState windowState, String reason) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new FastPrintWriter(sw, false, 1024);
        pw.println("  ANR time: " + DateFormat.getDateTimeInstance().format(new Date()));
        if (activity != null) {
            pw.println("  Application at fault: " + activity.stringName);
        }
        if (windowState != null) {
            pw.println("  Window at fault: " + ((Object) windowState.mAttrs.getTitle()));
        }
        if (reason != null) {
            pw.println("  Reason: " + reason);
        }
        for (int i = this.mRoot.getChildCount() - 1; i >= 0; i--) {
            DisplayContent dc = (DisplayContent) this.mRoot.getChildAt(i);
            int displayId = dc.getDisplayId();
            if (!dc.mWinAddedSinceNullFocus.isEmpty()) {
                pw.println("  Windows added in display #" + displayId + " since null focus: " + dc.mWinAddedSinceNullFocus);
            }
            if (!dc.mWinRemovedSinceNullFocus.isEmpty()) {
                pw.println("  Windows removed in display #" + displayId + " since null focus: " + dc.mWinRemovedSinceNullFocus);
            }
        }
        pw.println();
        dumpWindowsNoHeaderLocked(pw, true, null);
        pw.println();
        pw.println("Last ANR continued");
        this.mRoot.dumpDisplayContents(pw);
        pw.close();
        this.mLastANRState = sw.toString();
        this.mH.removeMessages(38);
        this.mH.sendEmptyMessageDelayed(38, AppStandbyController.ConstantsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        PriorityDump.dump(this.mPriorityDumper, fd, pw, args);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:40:0x0101, code lost:
        r0 = new android.util.proto.ProtoOutputStream(r16);
        r5 = r15.mGlobalLock;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x0109, code lost:
        monitor-enter(r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x010a, code lost:
        boostPriorityForLockedSection();
        dumpDebugLocked(r0, 0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x0110, code lost:
        monitor-exit(r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:44:0x0111, code lost:
        resetPriorityAfterLockedSection();
        r0.flush();
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x0117, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x0118, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x011d, code lost:
        throw r0;
     */
    @NeverCompile
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void doDump(FileDescriptor fd, final PrintWriter pw, String[] args, boolean useProto) {
        String opt;
        if (!SystemProperties.getBoolean(DEBUG_HIGH_REFRESH_BALCK_LIST, true)) {
            this.isHighRefreshBlackListOn = false;
        } else {
            this.isHighRefreshBlackListOn = true;
        }
        if (SystemProperties.getBoolean(DEBUG_MSYNC_LOG, false)) {
            this.isMsyncLogOn = true;
        } else {
            this.isMsyncLogOn = false;
        }
        if (DumpUtils.checkDumpPermission(this.mContext, "WindowManager", pw)) {
            int opti = 0;
            boolean dumpAll = false;
            while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
                opti++;
                if ("-a".equals(opt)) {
                    dumpAll = true;
                } else if ("-h".equals(opt)) {
                    pw.println("Window manager dump options:");
                    pw.println("  [-a] [-h] [cmd] ...");
                    pw.println("  cmd may be one of:");
                    pw.println("    l[astanr]: last ANR information");
                    pw.println("    p[policy]: policy state");
                    pw.println("    a[animator]: animator state");
                    pw.println("    s[essions]: active sessions");
                    pw.println("    surfaces: active surfaces (debugging enabled only)");
                    pw.println("    d[isplays]: active display contents");
                    pw.println("    t[okens]: token list");
                    pw.println("    w[indows]: window list");
                    pw.println("    package-config: installed packages having app-specific config");
                    pw.println("    trace: print trace status and write Winscope trace to file");
                    pw.println("  cmd may also be a NAME to dump windows.  NAME may");
                    pw.println("    be a partial substring in a window name, a");
                    pw.println("    Window hex object identifier, or");
                    pw.println("    \"all\" for all windows, or");
                    pw.println("    \"visible\" for the visible windows.");
                    pw.println("    \"visible-apps\" for the visible app windows.");
                    pw.println("  -a: include all available server state.");
                    pw.println("  --proto: output dump in protocol buffer format.");
                    return;
                } else if ("-d".equals(opt)) {
                    if (ITranWindowManagerService.Instance().ondumpD(pw, args, opti)) {
                        return;
                    }
                    this.mWindowManagerDebugger.runDebug(pw, args, opti);
                    return;
                } else {
                    pw.println("Unknown argument: " + opt + "; use -h for help");
                }
            }
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
            pw.println("Dump time : " + df.format(new Date()));
            if (opti < args.length) {
                String cmd = args[opti];
                int opti2 = opti + 1;
                if (ActivityTaskManagerService.DUMP_LASTANR_CMD.equals(cmd) || "l".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpLastANRLocked(pw);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                } else if ("policy".equals(cmd) || "p".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpPolicyLocked(pw, args, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                } else if ("animator".equals(cmd) || ActivityTaskManagerService.DUMP_ACTIVITIES_SHORT_CMD.equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpAnimatorLocked(pw, args, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                } else if ("sessions".equals(cmd) || "s".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpSessionsLocked(pw, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                } else if ("displays".equals(cmd) || "d".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            this.mRoot.dumpDisplayContents(pw);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                } else if ("tokens".equals(cmd) || "t".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpTokensLocked(pw, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                } else if ("windows".equals(cmd) || "w".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpWindowsLocked(pw, true, null);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                } else if ("all".equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            dumpWindowsLocked(pw, true, null);
                        } finally {
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                } else if (ActivityTaskManagerService.DUMP_CONTAINERS_CMD.equals(cmd)) {
                    synchronized (this.mGlobalLock) {
                        try {
                            boostPriorityForLockedSection();
                            this.mRoot.dumpChildrenNames(pw, " ");
                            pw.println(" ");
                            this.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda14
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    pw.println((WindowState) obj);
                                }
                            }, true);
                        } finally {
                            resetPriorityAfterLockedSection();
                        }
                    }
                    resetPriorityAfterLockedSection();
                    return;
                } else if ("trace".equals(cmd)) {
                    dumpTraceStatus(pw);
                    return;
                } else if ("logging".equals(cmd)) {
                    dumpLogStatus(pw);
                    return;
                } else if ("refresh".equals(cmd)) {
                    dumpHighRefreshRateBlacklist(pw);
                    return;
                } else if ("constants".equals(cmd)) {
                    this.mConstants.dump(pw);
                    return;
                } else if ("package-config".equals(cmd)) {
                    this.mAtmService.dumpInstalledPackagesConfig(pw);
                } else if ("RTon".equals(cmd)) {
                    this.mWmsExt.setRtEnable(pw, true);
                    return;
                } else if ("RToff".equals(cmd)) {
                    this.mWmsExt.setRtEnable(pw, false);
                    return;
                } else if (!ITranWindowManagerService.Instance().onDoDumpData(fd, pw, args, opti2) && !dumpWindows(pw, cmd, args, opti2, dumpAll)) {
                    pw.println("Bad window command, or no windows match: " + cmd);
                    pw.println("Use -h for help.");
                    return;
                } else {
                    return;
                }
            }
            synchronized (this.mGlobalLock) {
                try {
                    boostPriorityForLockedSection();
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpLastANRLocked(pw);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpPolicyLocked(pw, args, dumpAll);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpAnimatorLocked(pw, args, dumpAll);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpSessionsLocked(pw, dumpAll);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    this.mRoot.dumpDisplayContents(pw);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpTokensLocked(pw, dumpAll);
                    pw.println();
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpWindowsLocked(pw, dumpAll, null);
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpTraceStatus(pw);
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpLogStatus(pw);
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    dumpHighRefreshRateBlacklist(pw);
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    this.mAtmService.dumpInstalledPackagesConfig(pw);
                    if (dumpAll) {
                        pw.println("-------------------------------------------------------------------------------");
                    }
                    this.mConstants.dump(pw);
                } finally {
                    resetPriorityAfterLockedSection();
                }
            }
            resetPriorityAfterLockedSection();
        }
    }

    @Override // com.android.server.Watchdog.Monitor
    public void monitor() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public DisplayContent getDefaultDisplayContentLocked() {
        return this.mRoot.getDisplayContent(0);
    }

    public void onOverlayChanged() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda25
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        WindowManagerService.lambda$onOverlayChanged$14((DisplayContent) obj);
                    }
                });
                requestTraversal();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onOverlayChanged$14(DisplayContent displayContent) {
        displayContent.getDisplayPolicy().onOverlayChangedLw();
        displayContent.updateDisplayInfo();
    }

    @Override // com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs
    public Object getWindowManagerLock() {
        return this.mGlobalLock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWillReplaceWindow(IBinder token, boolean animate) {
        ActivityRecord activity = this.mRoot.getActivityRecord(token);
        if (activity == null) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam0 = String.valueOf(token);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1661704580, 0, "Attempted to set replacing window on non-existing app token %s", new Object[]{protoLogParam0});
            }
        } else if (!activity.hasContentToDisplay()) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam02 = String.valueOf(token);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1270731689, 0, "Attempted to set replacing window on app token with no content %s", new Object[]{protoLogParam02});
            }
        } else {
            activity.setWillReplaceWindows(animate);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWillReplaceWindows(IBinder token, boolean childrenOnly) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                ActivityRecord activity = this.mRoot.getActivityRecord(token);
                if (activity == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        String protoLogParam0 = String.valueOf(token);
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1661704580, 0, "Attempted to set replacing window on non-existing app token %s", new Object[]{protoLogParam0});
                    }
                    resetPriorityAfterLockedSection();
                } else if (!activity.hasContentToDisplay()) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        String protoLogParam02 = String.valueOf(token);
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1270731689, 0, "Attempted to set replacing window on app token with no content %s", new Object[]{protoLogParam02});
                    }
                    resetPriorityAfterLockedSection();
                } else {
                    if (childrenOnly) {
                        activity.setWillReplaceChildWindows();
                    } else {
                        activity.setWillReplaceWindows(false);
                    }
                    scheduleClearWillReplaceWindows(token, true);
                    resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleClearWillReplaceWindows(IBinder token, boolean replacing) {
        ActivityRecord activity = this.mRoot.getActivityRecord(token);
        if (activity == null) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                String protoLogParam0 = String.valueOf(token);
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 38267433, 0, "Attempted to reset replacing window on non-existing app token %s", new Object[]{protoLogParam0});
            }
        } else if (replacing) {
            scheduleWindowReplacementTimeouts(activity);
        } else {
            activity.clearWillReplaceWindows();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleWindowReplacementTimeouts(ActivityRecord activity) {
        if (!this.mWindowReplacementTimeouts.contains(activity)) {
            this.mWindowReplacementTimeouts.add(activity);
        }
        this.mH.removeMessages(46);
        this.mH.sendEmptyMessageDelayed(46, 2000L);
    }

    public int getDockedStackSide() {
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDockedRootTaskResizing(boolean resizing) {
        getDefaultDisplayContentLocked().getDockedDividerController().setResizing(resizing);
        requestTraversal();
    }

    public void setDockedTaskDividerTouchRegion(Rect touchRegion) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent dc = getDefaultDisplayContentLocked();
                dc.getDockedDividerController().setTouchRegion(touchRegion);
                dc.updateTouchExcludeRegion();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void setForceDesktopModeOnExternalDisplays(boolean forceDesktopModeOnExternalDisplays) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mForceDesktopModeOnExternalDisplays = forceDesktopModeOnExternalDisplays;
                this.mRoot.updateDisplayImePolicyCache();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void setIsPc(boolean isPc) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mIsPc = isPc;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int dipToPixel(int dip, DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(1, dip, displayMetrics);
    }

    public void registerPinnedTaskListener(int displayId, IPinnedTaskListener listener) {
        if (!checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerPinnedTaskListener()") || !this.mAtmService.mSupportsPictureInPicture) {
            return;
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                displayContent.getPinnedTaskController().registerPinnedTaskListener(listener);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void requestAppKeyboardShortcuts(IResultReceiver receiver, int deviceId) {
        try {
            WindowState focusedWindow = getFocusedWindow();
            if (focusedWindow != null && focusedWindow.mClient != null) {
                getFocusedWindow().mClient.requestAppKeyboardShortcuts(receiver, deviceId);
            }
        } catch (RemoteException e) {
        }
    }

    public void getStableInsets(int displayId, Rect outInsets) throws RemoteException {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                getStableInsetsLocked(displayId, outInsets);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    void getStableInsetsLocked(int displayId, Rect outInsets) {
        outInsets.setEmpty();
        DisplayContent dc = this.mRoot.getDisplayContent(displayId);
        if (dc != null) {
            DisplayInfo di = dc.getDisplayInfo();
            dc.getDisplayPolicy().getStableInsetsLw(di.rotation, di.displayCutout, outInsets);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class MousePositionTracker implements WindowManagerPolicyConstants.PointerEventListener {
        private boolean mLatestEventWasMouse;
        private float mLatestMouseX;
        private float mLatestMouseY;
        private int mPointerDisplayId;

        private MousePositionTracker() {
            this.mPointerDisplayId = -1;
        }

        boolean updatePosition(int displayId, float x, float y) {
            synchronized (this) {
                this.mLatestEventWasMouse = true;
                if (displayId != this.mPointerDisplayId) {
                    return false;
                }
                this.mLatestMouseX = x;
                this.mLatestMouseY = y;
                return true;
            }
        }

        void setPointerDisplayId(int displayId) {
            synchronized (this) {
                this.mPointerDisplayId = displayId;
            }
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            if (motionEvent.isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
                updatePosition(motionEvent.getDisplayId(), motionEvent.getRawX(), motionEvent.getRawY());
                return;
            }
            synchronized (this) {
                this.mLatestEventWasMouse = false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updatePointerIcon(IWindow client) {
        synchronized (this.mMousePositionTracker) {
            if (this.mMousePositionTracker.mLatestEventWasMouse) {
                float mouseX = this.mMousePositionTracker.mLatestMouseX;
                float mouseY = this.mMousePositionTracker.mLatestMouseY;
                int pointerDisplayId = this.mMousePositionTracker.mPointerDisplayId;
                synchronized (this.mGlobalLock) {
                    try {
                        boostPriorityForLockedSection();
                        if (this.mDragDropController.dragDropActiveLocked()) {
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        WindowState callingWin = windowForClientLocked((Session) null, client, false);
                        if (callingWin == null) {
                            if (ProtoLogCache.WM_ERROR_enabled) {
                                String protoLogParam0 = String.valueOf(client);
                                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1325649102, 0, "Bad requesting window %s", new Object[]{protoLogParam0});
                            }
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        DisplayContent displayContent = callingWin.getDisplayContent();
                        if (displayContent == null) {
                            resetPriorityAfterLockedSection();
                        } else if (pointerDisplayId != displayContent.getDisplayId()) {
                            resetPriorityAfterLockedSection();
                        } else {
                            WindowState windowUnderPointer = displayContent.getTouchableWinAtPointLocked(mouseX, mouseY);
                            if (windowUnderPointer != callingWin) {
                                resetPriorityAfterLockedSection();
                                return;
                            }
                            try {
                                windowUnderPointer.mClient.updatePointerIcon(windowUnderPointer.translateToWindowX(mouseX), windowUnderPointer.translateToWindowY(mouseY));
                            } catch (RemoteException e) {
                                if (ProtoLogCache.WM_ERROR_enabled) {
                                    Object[] objArr = null;
                                    ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -393505149, 0, "unable to update pointer icon", (Object[]) null);
                                }
                            }
                            resetPriorityAfterLockedSection();
                        }
                    } catch (Throwable th) {
                        resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void restorePointerIconLocked(DisplayContent displayContent, float latestX, float latestY) {
        if (!this.mMousePositionTracker.updatePosition(displayContent.getDisplayId(), latestX, latestY)) {
            return;
        }
        WindowState windowUnderPointer = displayContent.getTouchableWinAtPointLocked(latestX, latestY);
        if (windowUnderPointer != null) {
            try {
                windowUnderPointer.mClient.updatePointerIcon(windowUnderPointer.translateToWindowX(latestX), windowUnderPointer.translateToWindowY(latestY));
                return;
            } catch (RemoteException e) {
                if (ProtoLogCache.WM_ERROR_enabled) {
                    ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1423418408, 0, "unable to restore pointer icon", (Object[]) null);
                    return;
                }
                return;
            }
        }
        InputManager.getInstance().setPointerIconType(1000);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PointF getLatestMousePosition() {
        PointF pointF;
        synchronized (this.mMousePositionTracker) {
            pointF = new PointF(this.mMousePositionTracker.mLatestMouseX, this.mMousePositionTracker.mLatestMouseY);
        }
        return pointF;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMousePointerDisplayId(int displayId) {
        this.mMousePositionTracker.setPointerDisplayId(displayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTapExcludeRegion(IWindow client, Region region) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState callingWin = windowForClientLocked((Session) null, client, false);
                if (callingWin == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        String protoLogParam0 = String.valueOf(client);
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1325649102, 0, "Bad requesting window %s", new Object[]{protoLogParam0});
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                callingWin.updateTapExcludeRegion(region);
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [7829=6] */
    public void requestScrollCapture(int displayId, IBinder behindClient, int taskId, IScrollCaptureResponseListener listener) {
        ScrollCaptureResponse.Builder responseBuilder;
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "requestScrollCapture()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        long token = Binder.clearCallingIdentity();
        try {
            try {
                responseBuilder = new ScrollCaptureResponse.Builder();
                try {
                    try {
                    } catch (Throwable th) {
                        th = th;
                    }
                } catch (RemoteException e) {
                    e = e;
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        String protoLogParam0 = String.valueOf(e);
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1046922686, 0, "requestScrollCapture: caught exception dispatching callback: %s", new Object[]{protoLogParam0});
                    }
                    Binder.restoreCallingIdentity(token);
                }
            } catch (Throwable th2) {
                th = th2;
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        } catch (RemoteException e2) {
            e = e2;
        } catch (Throwable th3) {
            th = th3;
            Binder.restoreCallingIdentity(token);
            throw th;
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(displayId);
                if (dc == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam02 = displayId;
                        ProtoLogImpl.e(ProtoLogGroup.WM_ERROR, 646981048, 1, "Invalid displayId for requestScrollCapture: %d", new Object[]{Long.valueOf(protoLogParam02)});
                    }
                    responseBuilder.setDescription(String.format("bad displayId: %d", Integer.valueOf(displayId)));
                    listener.onScrollCaptureResponse(responseBuilder.build());
                    resetPriorityAfterLockedSection();
                    Binder.restoreCallingIdentity(token);
                    return;
                }
                for (Map.Entry<IBinder, WindowState> entry : this.mWindowMap.entrySet()) {
                    WindowState win = entry.getValue();
                    if ("transsion_fold_screen".equals(win.getWindowTag()) && win.isVisible()) {
                        responseBuilder.setDescription("findScrollCaptureTargetWindow returned null");
                        Slog.d("WindowManager", "found transsion_fold_screen, return null");
                        listener.onScrollCaptureResponse(responseBuilder.build());
                        resetPriorityAfterLockedSection();
                        Binder.restoreCallingIdentity(token);
                        return;
                    }
                }
                WindowState topWindow = behindClient != null ? windowForClientLocked(behindClient) : null;
                WindowState targetWindow = dc.findScrollCaptureTargetWindow(topWindow, taskId);
                if (targetWindow == null) {
                    responseBuilder.setDescription("findScrollCaptureTargetWindow returned null");
                    listener.onScrollCaptureResponse(responseBuilder.build());
                    resetPriorityAfterLockedSection();
                    Binder.restoreCallingIdentity(token);
                    return;
                }
                try {
                    targetWindow.mClient.requestScrollCapture(listener);
                } catch (RemoteException e3) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        String protoLogParam03 = String.valueOf(targetWindow.mClient.asBinder());
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1517908912, 0, "requestScrollCapture: caught exception dispatching to window.token=%s", new Object[]{protoLogParam03});
                    }
                    responseBuilder.setWindowTitle(targetWindow.getName());
                    responseBuilder.setPackageName(targetWindow.getOwningPackage());
                    responseBuilder.setDescription(String.format("caught exception: %s", e3));
                    listener.onScrollCaptureResponse(responseBuilder.build());
                }
                resetPriorityAfterLockedSection();
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th4) {
                th = th4;
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getWindowingMode(int displayId) {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "getWindowingMode()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam0 = displayId;
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 51628177, 1, "Attempted to get windowing mode of a display that does not exist: %d", new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    resetPriorityAfterLockedSection();
                    return 0;
                }
                int windowingModeLocked = this.mDisplayWindowSettings.getWindowingModeLocked(displayContent);
                resetPriorityAfterLockedSection();
                return windowingModeLocked;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setWindowingMode(int displayId, int mode) {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "setWindowingMode()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent displayContent = getDisplayContentOrCreate(displayId, null);
                if (displayContent == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam0 = displayId;
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -1838803135, 1, "Attempted to set windowing mode to a display that does not exist: %d", new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                int lastWindowingMode = displayContent.getWindowingMode();
                this.mDisplayWindowSettings.setWindowingModeLocked(displayContent, mode);
                displayContent.reconfigureDisplayLocked();
                if (lastWindowingMode != displayContent.getWindowingMode()) {
                    displayContent.sendNewConfiguration();
                    displayContent.executeAppTransition();
                }
                resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int getRemoveContentMode(int displayId) {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "getRemoveContentMode()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam0 = displayId;
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -496681057, 1, "Attempted to get remove mode of a display that does not exist: %d", new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    resetPriorityAfterLockedSection();
                    return 0;
                }
                int removeContentModeLocked = this.mDisplayWindowSettings.getRemoveContentModeLocked(displayContent);
                resetPriorityAfterLockedSection();
                return removeContentModeLocked;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setRemoveContentMode(int displayId, int mode) {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "setRemoveContentMode()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent displayContent = getDisplayContentOrCreate(displayId, null);
                if (displayContent == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam0 = displayId;
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 288485303, 1, "Attempted to set remove mode to a display that does not exist: %d", new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mDisplayWindowSettings.setRemoveContentModeLocked(displayContent, mode);
                displayContent.reconfigureDisplayLocked();
                resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean shouldShowWithInsecureKeyguard(int displayId) {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "shouldShowWithInsecureKeyguard()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam0 = displayId;
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1434383382, 1, "Attempted to get flag of a display that does not exist: %d", new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean shouldShowWithInsecureKeyguardLocked = this.mDisplayWindowSettings.shouldShowWithInsecureKeyguardLocked(displayContent);
                resetPriorityAfterLockedSection();
                return shouldShowWithInsecureKeyguardLocked;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setShouldShowWithInsecureKeyguard(int displayId, boolean shouldShow) {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "setShouldShowWithInsecureKeyguard()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent displayContent = getDisplayContentOrCreate(displayId, null);
                if (displayContent == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam0 = displayId;
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1521476038, 1, "Attempted to set flag to a display that does not exist: %d", new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mDisplayWindowSettings.setShouldShowWithInsecureKeyguardLocked(displayContent, shouldShow);
                displayContent.reconfigureDisplayLocked();
                resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean shouldShowSystemDecors(int displayId) {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "shouldShowSystemDecors()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam0 = displayId;
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 11060725, 1, "Attempted to get system decors flag of a display that does not exist: %d", new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    resetPriorityAfterLockedSection();
                    return false;
                }
                boolean supportsSystemDecorations = displayContent.supportsSystemDecorations();
                resetPriorityAfterLockedSection();
                return supportsSystemDecorations;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setShouldShowSystemDecors(int displayId, boolean shouldShow) {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "setShouldShowSystemDecors()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent displayContent = getDisplayContentOrCreate(displayId, null);
                if (displayContent == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam0 = displayId;
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -386552155, 1, "Attempted to set system decors flag to a display that does not exist: %d", new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    resetPriorityAfterLockedSection();
                } else if (!displayContent.isTrusted()) {
                    throw new SecurityException("Attempted to set system decors flag to an untrusted virtual display: " + displayId);
                } else {
                    this.mDisplayWindowSettings.setShouldShowSystemDecorsLocked(displayContent, shouldShow);
                    displayContent.reconfigureDisplayLocked();
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int getDisplayImePolicy(int displayId) {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "getDisplayImePolicy()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        Map<Integer, Integer> displayImePolicyCache = this.mDisplayImePolicyCache;
        if (!displayImePolicyCache.containsKey(Integer.valueOf(displayId))) {
            if (ProtoLogCache.WM_ERROR_enabled) {
                long protoLogParam0 = displayId;
                ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, 1100065297, 1, "Attempted to get IME policy of a display that does not exist: %d", new Object[]{Long.valueOf(protoLogParam0)});
            }
            return 1;
        }
        return displayImePolicyCache.get(Integer.valueOf(displayId)).intValue();
    }

    public void setDisplayImePolicy(int displayId, int imePolicy) {
        if (!checkCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "setDisplayImePolicy()")) {
            throw new SecurityException("Requires INTERNAL_SYSTEM_WINDOW permission");
        }
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent displayContent = getDisplayContentOrCreate(displayId, null);
                if (displayContent == null) {
                    if (ProtoLogCache.WM_ERROR_enabled) {
                        long protoLogParam0 = displayId;
                        ProtoLogImpl.w(ProtoLogGroup.WM_ERROR, -292790591, 1, "Attempted to set IME policy to a display that does not exist: %d", new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    resetPriorityAfterLockedSection();
                } else if (!displayContent.isTrusted()) {
                    throw new SecurityException("Attempted to set IME policy to an untrusted virtual display: " + displayId);
                } else {
                    this.mDisplayWindowSettings.setDisplayImePolicy(displayContent, imePolicy);
                    displayContent.reconfigureDisplayLocked();
                    resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void registerShortcutKey(long shortcutCode, IShortcutService shortcutKeyReceiver) throws RemoteException {
        if (!checkCallingPermission("android.permission.REGISTER_WINDOW_MANAGER_LISTENERS", "registerShortcutKey")) {
            throw new SecurityException("Requires REGISTER_WINDOW_MANAGER_LISTENERS permission");
        }
        this.mPolicy.registerShortcutKey(shortcutCode, shortcutKeyReceiver);
    }

    /* loaded from: classes2.dex */
    private final class LocalService extends WindowManagerInternal {
        private LocalService() {
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public WindowManagerInternal.AccessibilityControllerInternal getAccessibilityController() {
            return AccessibilityController.getAccessibilityControllerInternal(WindowManagerService.this);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void clearSnapshotCache() {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mTaskSnapshotController.clearSnapshotCache();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void requestTraversalFromDisplayManager() {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.requestTraversal();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setMagnificationSpec(int displayId, MagnificationSpec spec) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController.hasCallbacks()) {
                        WindowManagerService.this.mAccessibilityController.setMagnificationSpec(displayId, spec);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setForceShowMagnifiableBounds(int displayId, boolean show) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController.hasCallbacks()) {
                        WindowManagerService.this.mAccessibilityController.setForceShowMagnifiableBounds(displayId, show);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void getMagnificationRegion(int displayId, Region magnificationRegion) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mAccessibilityController.hasCallbacks()) {
                        WindowManagerService.this.mAccessibilityController.getMagnificationRegion(displayId, magnificationRegion);
                    } else {
                        throw new IllegalStateException("Magnification callbacks not set!");
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean setMagnificationCallbacks(int displayId, WindowManagerInternal.MagnificationCallbacks callbacks) {
            boolean magnificationCallbacks;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    magnificationCallbacks = WindowManagerService.this.mAccessibilityController.setMagnificationCallbacks(displayId, callbacks);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return magnificationCallbacks;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setWindowsForAccessibilityCallback(int displayId, WindowManagerInternal.WindowsForAccessibilityCallback callback) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mAccessibilityController.setWindowsForAccessibilityCallback(displayId, callback);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setInputFilter(IInputFilter filter) {
            WindowManagerService.this.mInputManager.setInputFilter(filter);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public IBinder getFocusedWindowToken() {
            IBinder focusedWindowToken;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    focusedWindowToken = WindowManagerService.this.mAccessibilityController.getFocusedWindowToken();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return focusedWindowToken;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public IBinder getFocusedWindowTokenFromWindowStates() {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = WindowManagerService.this.getFocusedWindowLocked();
                    if (windowState != null) {
                        IBinder asBinder = windowState.mClient.asBinder();
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return asBinder;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isKeyguardLocked() {
            return WindowManagerService.this.isKeyguardLocked();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isKeyguardShowingAndNotOccluded() {
            return WindowManagerService.this.isKeyguardShowingAndNotOccluded();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void showGlobalActions() {
            WindowManagerService.this.showGlobalActions();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void getWindowFrame(IBinder token, Rect outBounds) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState windowState = WindowManagerService.this.mWindowMap.get(token);
                    if (windowState != null) {
                        outBounds.set(windowState.getFrame());
                    } else {
                        outBounds.setEmpty();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public Pair<Matrix, MagnificationSpec> getWindowTransformationMatrixAndMagnificationSpec(IBinder token) {
            return WindowManagerService.this.mAccessibilityController.getWindowTransformationMatrixAndMagnificationSpec(token);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void waitForAllWindowsDrawn(Runnable callback, long timeout, int displayId) {
            WindowContainer container = displayId == -1 ? WindowManagerService.this.mRoot : WindowManagerService.this.mRoot.getDisplayContent(displayId);
            if (container == null) {
                callback.run();
                return;
            }
            boolean allWindowsDrawn = false;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    container.waitForAllWindowsDrawn();
                    WindowManagerService.this.mWindowPlacerLocked.requestTraversal();
                    WindowManagerService.this.mH.removeMessages(24, container);
                    if (container.mWaitingForDrawn.isEmpty() && !TranFoldWMCustody.instance().notifyAllWindowsDrawnDone(container)) {
                        allWindowsDrawn = true;
                    } else {
                        WindowManagerService.this.mWaitingForDrawnCallbacks.put(container, callback);
                        WindowManagerService.this.mH.sendNewMessageDelayed(24, container, timeout);
                        WindowManagerService.this.checkDrawnWindowsLocked();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            if (allWindowsDrawn) {
                callback.run();
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setForcedDisplaySize(int displayId, int width, int height) {
            WindowManagerService.this.setForcedDisplaySize(displayId, width, height);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void clearForcedDisplaySize(int displayId) {
            WindowManagerService.this.clearForcedDisplaySize(displayId);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void addWindowToken(IBinder token, int type, int displayId, Bundle options) {
            WindowManagerService.this.addWindowToken(token, type, displayId, options);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void removeWindowToken(IBinder binder, boolean removeWindows, boolean animateExit, int displayId) {
            WindowManagerService.this.removeWindowToken(binder, removeWindows, animateExit, displayId);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void moveWindowTokenToDisplay(IBinder binder, int displayId) {
            WindowManagerService.this.moveWindowTokenToDisplay(binder, displayId);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void registerAppTransitionListener(WindowManagerInternal.AppTransitionListener listener) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.getDefaultDisplayContentLocked().mAppTransition.registerListenerLocked(listener);
                    WindowManagerService.this.mAtmService.getTransitionController().registerLegacyListener(listener);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void registerTaskSystemBarsListener(WindowManagerInternal.TaskSystemBarsListener listener) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mTaskSystemBarsListenerController.registerListener(listener);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void unregisterTaskSystemBarsListener(WindowManagerInternal.TaskSystemBarsListener listener) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mTaskSystemBarsListenerController.unregisterListener(listener);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void registerKeyguardExitAnimationStartListener(WindowManagerInternal.KeyguardExitAnimationStartListener listener) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.getDefaultDisplayContentLocked().mAppTransition.registerKeygaurdExitAnimationStartListener(listener);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void reportPasswordChanged(int userId) {
            WindowManagerService.this.mKeyguardDisableHandler.updateKeyguardEnabled(userId);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getInputMethodWindowVisibleHeight(int displayId) {
            int inputMethodWindowVisibleHeight;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    DisplayContent dc = WindowManagerService.this.mRoot.getDisplayContent(displayId);
                    inputMethodWindowVisibleHeight = dc.getInputMethodWindowVisibleHeight();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return inputMethodWindowVisibleHeight;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setDismissImeOnBackKeyPressed(boolean dismissImeOnBackKeyPressed) {
            WindowManagerService.this.mPolicy.setDismissImeOnBackKeyPressed(dismissImeOnBackKeyPressed);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void updateInputMethodTargetWindow(IBinder imeToken, IBinder imeTargetWindowToken) {
            if (WindowManagerDebugConfig.DEBUG_INPUT_METHOD) {
                Slog.w("WindowManager", "updateInputMethodTargetWindow: imeToken=" + imeToken + " imeTargetWindowToken=" + imeTargetWindowToken);
            }
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    InputTarget imeTarget = WindowManagerService.this.getInputTargetFromWindowTokenLocked(imeTargetWindowToken);
                    if (imeTarget != null) {
                        imeTarget.getDisplayContent().updateImeInputAndControlTarget(imeTarget);
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isHardKeyboardAvailable() {
            boolean z;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    z = WindowManagerService.this.mHardKeyboardAvailable;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return z;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setOnHardKeyboardStatusChangeListener(WindowManagerInternal.OnHardKeyboardStatusChangeListener listener) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mHardKeyboardStatusChangeListener = listener;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void computeWindowsForAccessibility(int displayId) {
            WindowManagerService.this.mAccessibilityController.performComputeChangedWindowsNot(displayId, true);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setVr2dDisplayId(int vr2dDisplayId) {
            if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                Slog.d("WindowManager", "setVr2dDisplayId called for: " + vr2dDisplayId);
            }
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mVr2dDisplayId = vr2dDisplayId;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void registerDragDropControllerCallback(WindowManagerInternal.IDragDropCallback callback) {
            WindowManagerService.this.mDragDropController.registerCallback(callback);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void lockNow() {
            WindowManagerService.this.lockNow(null);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getWindowOwnerUserId(IBinder token) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState window = WindowManagerService.this.mWindowMap.get(token);
                    if (window != null) {
                        int i = window.mShowUserId;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return i;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return -10000;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isUidFocused(int uid) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    for (int i = WindowManagerService.this.mRoot.getChildCount() - 1; i >= 0; i--) {
                        DisplayContent displayContent = (DisplayContent) WindowManagerService.this.mRoot.getChildAt(i);
                        if (displayContent.mCurrentFocus != null && uid == displayContent.mCurrentFocus.getOwningUid()) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return true;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int hasInputMethodClientFocus(IBinder windowToken, int uid, int pid, int displayId) {
            if (displayId == -1) {
                return -3;
            }
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    DisplayContent displayContent = WindowManagerService.this.mRoot.getTopFocusedDisplayContent();
                    InputTarget target = WindowManagerService.this.getInputTargetFromWindowTokenLocked(windowToken);
                    if (target == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return -1;
                    }
                    int tokenDisplayId = target.getDisplayContent().getDisplayId();
                    if (tokenDisplayId != displayId) {
                        Slog.e("WindowManager", "isInputMethodClientFocus: display ID mismatch. from client: " + displayId + " from window: " + tokenDisplayId);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return -2;
                    }
                    if (displayContent != null && displayContent.getDisplayId() == displayId && displayContent.hasAccess(uid)) {
                        if (target.isInputMethodClientFocus(uid, pid)) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return 0;
                        }
                        WindowState currentFocus = displayContent.mCurrentFocus;
                        if (currentFocus == null || currentFocus.mSession.mUid != uid || currentFocus.mSession.mPid != pid) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return -1;
                        }
                        int i = currentFocus.canBeImeTarget() ? 0 : -1;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return i;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return -3;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void showImePostLayout(IBinder imeTargetWindowToken) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    InputTarget imeTarget = WindowManagerService.this.getInputTargetFromWindowTokenLocked(imeTargetWindowToken);
                    if (imeTarget == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    Trace.asyncTraceBegin(32L, "WMS.showImePostLayout", 0);
                    InsetsControlTarget controlTarget = imeTarget.getImeControlTarget();
                    InputTarget imeTarget2 = controlTarget.getWindow();
                    DisplayContent dc = imeTarget2 != null ? imeTarget2.getDisplayContent() : WindowManagerService.this.getDefaultDisplayContentLocked();
                    dc.getInsetsStateController().getImeSourceProvider().scheduleShowImePostLayout(controlTarget);
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void hideIme(IBinder imeTargetWindowToken, int displayId) {
            Trace.traceBegin(32L, "WMS.hideIme");
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState imeTarget = WindowManagerService.this.mWindowMap.get(imeTargetWindowToken);
                    if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                        String protoLogParam0 = String.valueOf(imeTarget);
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_IME, 95216706, 0, (String) null, new Object[]{protoLogParam0});
                    }
                    DisplayContent dc = WindowManagerService.this.mRoot.getDisplayContent(displayId);
                    if (imeTarget != null) {
                        WindowState imeTarget2 = imeTarget.getImeControlTarget().getWindow();
                        if (imeTarget2 != null) {
                            dc = imeTarget2.getDisplayContent();
                        }
                        dc.getInsetsStateController().getImeSourceProvider().abortShowImePostLayout();
                    }
                    if (dc != null && dc.getImeTarget(2) != null) {
                        if (ProtoLogCache.WM_DEBUG_IME_enabled) {
                            String protoLogParam02 = String.valueOf(dc.getImeTarget(2));
                            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_IME, -547111355, 0, (String) null, new Object[]{protoLogParam02});
                        }
                        dc.getImeTarget(2).hideInsets(WindowInsets.Type.ime(), true);
                    }
                    if (dc != null) {
                        dc.getInsetsStateController().getImeSourceProvider().setImeShowing(false);
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            Trace.traceEnd(32L);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isUidAllowedOnDisplay(int displayId, int uid) {
            boolean z = true;
            if (displayId == 0) {
                return true;
            }
            if (displayId == -1) {
                return false;
            }
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    DisplayContent displayContent = WindowManagerService.this.mRoot.getDisplayContent(displayId);
                    if (displayContent == null || !displayContent.hasAccess(uid)) {
                        z = false;
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return z;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getDisplayIdForWindow(IBinder windowToken) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState window = WindowManagerService.this.mWindowMap.get(windowToken);
                    if (window != null) {
                        int displayId = window.getDisplayContent().getDisplayId();
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return displayId;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return -1;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getTopFocusedDisplayId() {
            int displayId;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    displayId = WindowManagerService.this.mRoot.getTopFocusedDisplayContent().getDisplayId();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return displayId;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public Context getTopFocusedDisplayUiContext() {
            Context displayUiContext;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    displayUiContext = WindowManagerService.this.mRoot.getTopFocusedDisplayContent().getDisplayUiContext();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return displayUiContext;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean shouldShowSystemDecorOnDisplay(int displayId) {
            boolean shouldShowSystemDecors;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    shouldShowSystemDecors = WindowManagerService.this.shouldShowSystemDecors(displayId);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return shouldShowSystemDecors;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public int getDisplayImePolicy(int displayId) {
            return WindowManagerService.this.getDisplayImePolicy(displayId);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void addRefreshRateRangeForPackage(final String packageName, final float minRefreshRate, final float maxRefreshRate) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerService$LocalService$$ExternalSyntheticLambda1
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((DisplayContent) obj).getDisplayPolicy().getRefreshRatePolicy().addRefreshRateRangeForPackage(packageName, minRefreshRate, maxRefreshRate);
                        }
                    });
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void removeRefreshRateRangeForPackage(final String packageName) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerService$LocalService$$ExternalSyntheticLambda2
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((DisplayContent) obj).getDisplayPolicy().getRefreshRatePolicy().removeRefreshRateRangeForPackage(packageName);
                        }
                    });
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isTouchOrFaketouchDevice() {
            boolean z;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (WindowManagerService.this.mIsTouchDevice && !WindowManagerService.this.mIsFakeTouchDevice) {
                        throw new IllegalStateException("touchscreen supported device must report faketouch.");
                    }
                    z = WindowManagerService.this.mIsFakeTouchDevice;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return z;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public KeyInterceptionInfo getKeyInterceptionInfoFromToken(IBinder inputToken) {
            return WindowManagerService.this.mKeyInterceptionInfoForToken.get(inputToken);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setAccessibilityIdToSurfaceMetadata(IBinder windowToken, int accessibilityWindowId) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState state = WindowManagerService.this.mWindowMap.get(windowToken);
                    if (state == null) {
                        Slog.w("WindowManager", "Cannot find window which accessibility connection is added to");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    WindowManagerService.this.mTransaction.setMetadata(state.mSurfaceControl, 5, accessibilityWindowId).apply();
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public String getWindowName(IBinder binder) {
            String name;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState w = WindowManagerService.this.mWindowMap.get(binder);
                    name = w != null ? w.getName() : null;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return name;
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public WindowManagerInternal.ImeTargetInfo onToggleImeRequested(boolean show, IBinder focusedToken, IBinder requestToken, int displayId) {
            String focusedWindowName;
            String requestWindowName;
            String imeLayerTargetName;
            String imeControlTargetName;
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState focusedWin = WindowManagerService.this.mWindowMap.get(focusedToken);
                    focusedWindowName = focusedWin != null ? focusedWin.getName() : "null";
                    WindowState requestWin = WindowManagerService.this.mWindowMap.get(requestToken);
                    requestWindowName = requestWin != null ? requestWin.getName() : "null";
                    DisplayContent dc = WindowManagerService.this.mRoot.getDisplayContent(displayId);
                    if (dc != null) {
                        InsetsControlTarget controlTarget = dc.getImeTarget(2);
                        if (controlTarget != null) {
                            WindowState w = InsetsControlTarget.asWindowOrNull(controlTarget);
                            imeControlTargetName = w != null ? w.getName() : controlTarget.toString();
                        } else {
                            imeControlTargetName = "null";
                        }
                        InsetsControlTarget target = dc.getImeTarget(0);
                        imeLayerTargetName = target != null ? target.getWindow().getName() : "null";
                        if (show) {
                            dc.onShowImeRequested();
                        }
                    } else {
                        imeLayerTargetName = "no-display";
                        imeControlTargetName = "no-display";
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return new WindowManagerInternal.ImeTargetInfo(focusedWindowName, requestWindowName, imeControlTargetName, imeLayerTargetName);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean shouldRestoreImeVisibility(IBinder imeTargetWindowToken) {
            return WindowManagerService.this.shouldRestoreImeVisibility(imeTargetWindowToken);
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void addTrustedTaskOverlay(int taskId, SurfaceControlViewHost.SurfacePackage overlay) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    Task task = WindowManagerService.this.mRoot.getRootTask(taskId);
                    if (task == null) {
                        throw new IllegalArgumentException("no task with taskId" + taskId);
                    }
                    task.addTrustedOverlay(overlay, task.getTopVisibleAppMainWindow());
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void removeTrustedTaskOverlay(int taskId, SurfaceControlViewHost.SurfacePackage overlay) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    Task task = WindowManagerService.this.mRoot.getRootTask(taskId);
                    if (task == null) {
                        throw new IllegalArgumentException("no task with taskId" + taskId);
                    }
                    task.removeTrustedOverlay(overlay);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public SurfaceControl getHandwritingSurfaceForDisplay(int displayId) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    DisplayContent dc = WindowManagerService.this.mRoot.getDisplayContent(displayId);
                    if (dc == null) {
                        Slog.e("WindowManager", "Failed to create a handwriting surface on display: " + displayId + " - DisplayContent not found.");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                    SurfaceControl build = WindowManagerService.this.makeSurfaceBuilder(dc.getSession()).setContainerLayer().setName("IME Handwriting Surface").setCallsite("getHandwritingSurfaceForDisplay").setParent(dc.getOverlayLayer()).build();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return build;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public boolean isPointInsideWindow(IBinder windowToken, int displayId, float displayX, float displayY) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowState w = WindowManagerService.this.mWindowMap.get(windowToken);
                    if (w != null && w.getDisplayId() == displayId) {
                        boolean contains = w.getBounds().contains((int) displayX, (int) displayY);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return contains;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void relayoutWindowForDreamAnimation() {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.requestTraversal();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void setContentRecordingSession(ContentRecordingSession incomingSession) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mContentRecordingController.setContentRecordingSessionLocked(incomingSession, WindowManagerService.this);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void updateRefreshRateForScene(final Bundle b) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerService$LocalService$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((DisplayContent) obj).getDisplayPolicy().getRefreshRatePolicy().updateRefreshRateForScene(b);
                        }
                    });
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.WindowManagerInternal
        public void updateRefreshRateForVideoScene(final int videoState, final int videoFps, final int videoSessionId) {
            synchronized (WindowManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    WindowManagerService.this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerService$LocalService$$ExternalSyntheticLambda3
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((DisplayContent) obj).getDisplayPolicy().getRefreshRatePolicy().updateRefreshRateForVideoScene(videoState, videoFps, videoSessionId);
                        }
                    });
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerAppFreezeListener(AppFreezeListener listener) {
        if (!this.mAppFreezeListeners.contains(listener)) {
            this.mAppFreezeListeners.add(listener);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterAppFreezeListener(AppFreezeListener listener) {
        this.mAppFreezeListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void inSurfaceTransaction(Runnable exec) {
        SurfaceControl.openTransaction();
        try {
            exec.run();
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    public void disableNonVrUi(boolean disable) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                boolean showAlertWindowNotifications = !disable;
                if (showAlertWindowNotifications == this.mShowAlertWindowNotifications) {
                    resetPriorityAfterLockedSection();
                    return;
                }
                this.mShowAlertWindowNotifications = showAlertWindowNotifications;
                for (int i = this.mSessions.size() - 1; i >= 0; i--) {
                    Session s = this.mSessions.valueAt(i);
                    s.setShowingAlertWindowNotificationAllowed(this.mShowAlertWindowNotifications);
                }
                resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasWideColorGamutSupport() {
        return this.mHasWideColorGamutSupport && SystemProperties.getInt("persist.sys.sf.native_mode", 0) != 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasHdrSupport() {
        return this.mHasHdrSupport && hasWideColorGamutSupport();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateNonSystemOverlayWindowsVisibilityIfNeeded(WindowState win, boolean surfaceShown) {
        if (!win.hideNonSystemOverlayWindowsWhenVisible() && !this.mHidingNonSystemOverlayWindows.contains(win)) {
            return;
        }
        boolean systemAlertWindowsHidden = !this.mHidingNonSystemOverlayWindows.isEmpty();
        if (surfaceShown && win.hideNonSystemOverlayWindowsWhenVisible()) {
            if (!this.mHidingNonSystemOverlayWindows.contains(win)) {
                this.mHidingNonSystemOverlayWindows.add(win);
            }
        } else {
            this.mHidingNonSystemOverlayWindows.remove(win);
        }
        final boolean hideSystemAlertWindows = !this.mHidingNonSystemOverlayWindows.isEmpty();
        if (systemAlertWindowsHidden == hideSystemAlertWindows) {
            return;
        }
        this.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda21
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WindowState) obj).setForceHideNonSystemOverlayWindowIfNeeded(hideSystemAlertWindows);
            }
        }, false);
    }

    public void applyMagnificationSpecLocked(int displayId, MagnificationSpec spec) {
        DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
        if (displayContent != null) {
            displayContent.applyMagnificationSpec(spec);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl.Builder makeSurfaceBuilder(SurfaceSession s) {
        return this.mSurfaceControlFactory.apply(s);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onLockTaskStateChanged(int lockTaskState) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                Consumer<DisplayPolicy> obtainConsumer = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda0
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        ((DisplayPolicy) obj).onLockTaskStateChangedLw(((Integer) obj2).intValue());
                    }
                }, PooledLambda.__(), Integer.valueOf(lockTaskState));
                this.mRoot.forAllDisplayPolicies(obtainConsumer);
                obtainConsumer.recycle();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public void syncInputTransactions(boolean waitForAnimations) {
        long token = Binder.clearCallingIdentity();
        if (waitForAnimations) {
            try {
                waitForAnimationsToComplete();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        final SurfaceControl.Transaction t = this.mTransactionFactory.get();
        synchronized (this.mGlobalLock) {
            boostPriorityForLockedSection();
            this.mWindowPlacerLocked.performSurfacePlacementIfScheduled();
            this.mRoot.forAllDisplays(new Consumer() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda8
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((DisplayContent) obj).getInputMonitor().updateInputWindowsImmediately(t);
                }
            });
        }
        resetPriorityAfterLockedSection();
        t.syncInputWindows().apply();
    }

    /* JADX WARN: Can't wrap try/catch for region: R(11:7|(1:46)(1:11)|12|(2:18|(5:37|38|39|41|42)(2:22|23))|45|(1:20)|37|38|39|41|42) */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void waitForAnimationsToComplete() {
        boolean isAnimating;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                long timeoutRemaining = 5000;
                this.mAnimator.mNotifyWhenNoAnimation = true;
                boolean animateStarting = false;
                while (true) {
                    if (timeoutRemaining <= 0) {
                        break;
                    }
                    animateStarting = !this.mAtmService.getTransitionController().isShellTransitionsEnabled() && this.mRoot.forAllActivities(new Predicate() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda16
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return ((ActivityRecord) obj).hasStartingWindow();
                        }
                    });
                    if (!this.mAnimator.isAnimationScheduled() && !this.mRoot.isAnimating(5, -1) && !animateStarting) {
                        isAnimating = false;
                        if (isAnimating && !this.mAtmService.getTransitionController().inTransition()) {
                            break;
                        }
                        long startTime = System.currentTimeMillis();
                        this.mGlobalLock.wait(timeoutRemaining);
                        timeoutRemaining -= System.currentTimeMillis() - startTime;
                    }
                    isAnimating = true;
                    if (isAnimating) {
                    }
                    long startTime2 = System.currentTimeMillis();
                    this.mGlobalLock.wait(timeoutRemaining);
                    timeoutRemaining -= System.currentTimeMillis() - startTime2;
                }
                this.mAnimator.mNotifyWhenNoAnimation = false;
                WindowContainer animatingContainer = this.mRoot.getAnimatingContainer(5, -1);
                if (this.mAnimator.isAnimationScheduled() || animatingContainer != null || animateStarting) {
                    Slog.w("WindowManager", "Timed out waiting for animations to complete, animatingContainer=" + animatingContainer + " animationType=" + SurfaceAnimator.animationTypeToString(animatingContainer != null ? animatingContainer.mSurfaceAnimator.getAnimationType() : 0) + " animateStarting=" + animateStarting);
                }
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAnimationFinished() {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                this.mGlobalLock.notifyAll();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPointerDownOutsideFocusLocked(IBinder touchedToken) {
        InputTarget t = getInputTargetFromToken(touchedToken);
        if (t == null || !t.receiveFocusFromTapOutside()) {
            return;
        }
        RecentsAnimationController recentsAnimationController = this.mRecentsAnimationController;
        if (recentsAnimationController != null && recentsAnimationController.getTargetAppMainWindow() == t) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
            String protoLogParam0 = String.valueOf(t);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, -561092364, 0, (String) null, new Object[]{protoLogParam0});
        }
        InputTarget inputTarget = this.mFocusedInputTarget;
        if (inputTarget != t && inputTarget != null) {
            inputTarget.handleTapOutsideFocusOutsideSelf();
        }
        t.handleTapOutsideFocusInsideSelf();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleTaskFocusChange(Task task, ActivityRecord touchedActivity) {
        if (task == null) {
            return;
        }
        if (task.isActivityTypeHome()) {
            TaskDisplayArea homeTda = task.getDisplayArea();
            WindowState curFocusedWindow = getFocusedWindow();
            if (curFocusedWindow != null && homeTda != null && curFocusedWindow.isDescendantOf(homeTda)) {
                return;
            }
        }
        this.mAtmService.setFocusedTask(task.mTaskId, touchedActivity);
    }

    private int sanitizeFlagSlippery(int flags, String windowName, int callingUid, int callingPid) {
        if ((536870912 & flags) == 0) {
            return flags;
        }
        int permissionResult = this.mContext.checkPermission("android.permission.ALLOW_SLIPPERY_TOUCHES", callingPid, callingUid);
        if (permissionResult != 0) {
            Slog.w("WindowManager", "Removing FLAG_SLIPPERY from '" + windowName + "' because it doesn't have ALLOW_SLIPPERY_TOUCHES permission");
            return (-536870913) & flags;
        }
        return flags;
    }

    private int sanitizeSpyWindow(int inputFeatures, String windowName, int callingUid, int callingPid) {
        if ((inputFeatures & 4) == 0) {
            return inputFeatures;
        }
        int permissionResult = this.mContext.checkPermission("android.permission.MONITOR_INPUT", callingPid, callingUid);
        if (permissionResult != 0) {
            throw new IllegalArgumentException("Cannot use INPUT_FEATURE_SPY from '" + windowName + "' because it doesn't the have MONITOR_INPUT permission");
        }
        return inputFeatures;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void grantInputChannel(Session session, int callingUid, int callingPid, int displayId, SurfaceControl surface, IWindow window, IBinder hostInputToken, int flags, int privateFlags, int type, IBinder focusGrantToken, String inputHandleName, InputChannel outInputChannel) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                try {
                    EmbeddedWindowController.EmbeddedWindow win = new EmbeddedWindowController.EmbeddedWindow(session, this, window, this.mInputToWindowMap.get(hostInputToken), callingUid, callingPid, type, displayId, focusGrantToken, inputHandleName);
                    InputChannel clientChannel = win.openInputChannel();
                    this.mEmbeddedWindowController.add(clientChannel.getToken(), win);
                    InputApplicationHandle applicationHandle = win.getApplicationHandle();
                    String name = win.toString();
                    resetPriorityAfterLockedSection();
                    updateInputChannel(clientChannel.getToken(), callingUid, callingPid, displayId, surface, name, applicationHandle, flags, privateFlags, type, null, window);
                    clientChannel.copyTo(outInputChannel);
                } catch (Throwable th) {
                    th = th;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    private void updateInputChannel(IBinder channelToken, int callingUid, int callingPid, int displayId, SurfaceControl surface, String name, InputApplicationHandle applicationHandle, int flags, int privateFlags, int type, Region region, IWindow window) {
        InputWindowHandle h = new InputWindowHandle(applicationHandle, displayId);
        h.token = channelToken;
        h.setWindowToken(window);
        h.name = name;
        int flags2 = sanitizeFlagSlippery(flags, name, callingUid, callingPid);
        int sanitizedLpFlags = (536870936 & flags2) | 32;
        h.layoutParamsType = type;
        h.layoutParamsFlags = sanitizedLpFlags;
        h.inputConfig = InputConfigAdapter.getInputConfigFromWindowParams(type, sanitizedLpFlags, 0);
        if ((flags2 & 8) != 0) {
            h.inputConfig |= 4;
        }
        if ((privateFlags & 536870912) != 0) {
            h.inputConfig |= 256;
        }
        h.dispatchingTimeoutMillis = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
        h.ownerUid = callingUid;
        h.ownerPid = callingPid;
        if (region == null) {
            h.replaceTouchableRegionWithCrop = true;
        } else {
            h.touchableRegion.set(region);
        }
        h.setTouchableRegionCrop((SurfaceControl) null);
        SurfaceControl.Transaction t = this.mTransactionFactory.get();
        t.setInputWindowInfo(surface, h);
        t.apply();
        t.close();
        surface.release();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateInputChannel(IBinder channelToken, int displayId, SurfaceControl surface, int flags, int privateFlags, Region region) {
        synchronized (this.mGlobalLock) {
            try {
                try {
                    boostPriorityForLockedSection();
                    EmbeddedWindowController.EmbeddedWindow win = this.mEmbeddedWindowController.get(channelToken);
                    if (win == null) {
                        Slog.e("WindowManager", "Couldn't find window for provided channelToken.");
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    String name = win.toString();
                    InputApplicationHandle applicationHandle = win.getApplicationHandle();
                    resetPriorityAfterLockedSection();
                    updateInputChannel(channelToken, win.mOwnerUid, win.mOwnerPid, displayId, surface, name, applicationHandle, flags, privateFlags, win.mWindowType, region, win.mClient);
                } catch (Throwable th) {
                    th = th;
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [9134=5, 9135=4, 9137=4, 9138=4, 9142=4] */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x0044, code lost:
        if (0 != 0) goto L19;
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x0046, code lost:
        r3.recycle();
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x0059, code lost:
        if (r3 != null) goto L19;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x005c, code lost:
        android.os.Binder.restoreCallingIdentity(r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x0060, code lost:
        return false;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean isLayerTracing() {
        if (!checkCallingPermission("android.permission.DUMP", "isLayerTracing()")) {
            throw new SecurityException("Requires DUMP permission");
        }
        long token = Binder.clearCallingIdentity();
        Parcel data = null;
        Parcel reply = null;
        try {
            try {
                IBinder sf = ServiceManager.getService("SurfaceFlinger");
                if (sf != null) {
                    reply = Parcel.obtain();
                    data = Parcel.obtain();
                    data.writeInterfaceToken("android.ui.ISurfaceComposer");
                    sf.transact(UsbTerminalTypes.TERMINAL_BIDIR_HEADSET, data, reply, 0);
                    boolean readBoolean = reply.readBoolean();
                    if (data != null) {
                        data.recycle();
                    }
                    if (reply != null) {
                        reply.recycle();
                    }
                    Binder.restoreCallingIdentity(token);
                    return readBoolean;
                } else if (0 != 0) {
                    data.recycle();
                }
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Failed to get layer tracing");
                if (data != null) {
                    data.recycle();
                }
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [9168=4] */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0031, code lost:
        if (r2 != null) goto L14;
     */
    /* JADX WARN: Code restructure failed: missing block: B:13:0x0033, code lost:
        r2.recycle();
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0041, code lost:
        if (r2 == null) goto L16;
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0048, code lost:
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void setLayerTracing(boolean enabled) {
        if (!checkCallingPermission("android.permission.DUMP", "setLayerTracing()")) {
            throw new SecurityException("Requires DUMP permission");
        }
        long token = Binder.clearCallingIdentity();
        Parcel data = null;
        try {
            try {
                IBinder sf = ServiceManager.getService("SurfaceFlinger");
                if (sf != null) {
                    data = Parcel.obtain();
                    data.writeInterfaceToken("android.ui.ISurfaceComposer");
                    data.writeInt(enabled ? 1 : 0);
                    sf.transact(UsbTerminalTypes.TERMINAL_BIDIR_HANDSET, data, null, 0);
                }
            } catch (RemoteException e) {
                Slog.e("WindowManager", "Failed to set layer tracing");
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [9198=4] */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x003c, code lost:
        if (r2 == null) goto L12;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x0043, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:8:0x002c, code lost:
        if (r2 != null) goto L10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x002e, code lost:
        r2.recycle();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void setLayerTracingFlags(int flags) {
        if (!checkCallingPermission("android.permission.DUMP", "setLayerTracingFlags")) {
            throw new SecurityException("Requires DUMP permission");
        }
        long token = Binder.clearCallingIdentity();
        Parcel data = null;
        try {
            try {
                IBinder sf = ServiceManager.getService("SurfaceFlinger");
                if (sf != null) {
                    data = Parcel.obtain();
                    data.writeInterfaceToken("android.ui.ISurfaceComposer");
                    data.writeInt(flags);
                    sf.transact(1033, data, null, 0);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } catch (RemoteException e) {
            Slog.e("WindowManager", "Failed to set layer tracing flags");
        }
    }

    public void setTranPictureMode(int mode, String packageName) {
        ITranWindowManagerService.Instance().setTranPictureMode(mode, packageName);
    }

    public int[] getTranPictureSupportMode() {
        return ITranWindowManagerService.Instance().getTranPictureSupportMode();
    }

    public List<String> getTranPictureList(int mode) {
        return ITranWindowManagerService.Instance().getTranPictureList(mode);
    }

    public void reloadPQEConfig() {
        ITranWindowManagerService.Instance().reloadPQEConfig();
    }

    public boolean modifyConfigFile(String mode, String packageName, int sharpness, int saturation, int contrast, int brightness) {
        return ITranWindowManagerService.Instance().modifyConfigFile(mode, packageName, sharpness, saturation, contrast, brightness);
    }

    public boolean mirrorDisplay(int displayId, SurfaceControl outSurfaceControl) {
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "mirrorDisplay()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                if (displayContent == null) {
                    Slog.e("WindowManager", "Invalid displayId " + displayId + " for mirrorDisplay");
                    resetPriorityAfterLockedSection();
                    return false;
                }
                SurfaceControl displaySc = displayContent.getWindowingLayer();
                resetPriorityAfterLockedSection();
                SurfaceControl mirror = SurfaceControl.mirrorSurface(displaySc);
                outSurfaceControl.copyFrom(mirror, "WMS.mirrorDisplay");
                return true;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean getWindowInsets(WindowManager.LayoutParams attrs, int displayId, InsetsState outInsetsState) {
        boolean areSystemBarsForcedShownLw;
        float compatScale;
        int uid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                DisplayContent dc = getDisplayContentOrCreate(displayId, attrs.token);
                if (dc == null) {
                    throw new WindowManager.InvalidDisplayException("Display#" + displayId + "could not be found!");
                }
                WindowToken token = dc.getWindowToken(attrs.token);
                float overrideScale = this.mAtmService.mCompatModePackages.getCompatScale(attrs.packageName, uid);
                InsetsState state = dc.getInsetsPolicy().getInsetsForWindowMetrics(attrs);
                outInsetsState.set(state, true);
                if (WindowState.hasCompatScale(attrs, token, overrideScale)) {
                    if (token != null && token.hasSizeCompatBounds()) {
                        compatScale = token.getSizeCompatScale() * overrideScale;
                    } else {
                        compatScale = overrideScale;
                    }
                    outInsetsState.scale(1.0f / compatScale);
                }
                areSystemBarsForcedShownLw = dc.getDisplayPolicy().areSystemBarsForcedShownLw();
            }
            resetPriorityAfterLockedSection();
            return areSystemBarsForcedShownLw;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public List<DisplayInfo> getPossibleDisplayInfo(int displayId, String packageName) {
        int callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                if (packageName != null && isRecentsComponent(packageName, callingUid)) {
                    List<DisplayInfo> copyOf = List.copyOf(this.mPossibleDisplayInfoMapper.getPossibleDisplayInfos(displayId));
                    resetPriorityAfterLockedSection();
                    return copyOf;
                }
                Slog.e("WindowManager", "Unable to verify uid for package " + packageName + " for getPossibleMaximumWindowMetrics");
                ArrayList arrayList = new ArrayList();
                resetPriorityAfterLockedSection();
                return arrayList;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    boolean isRecentsComponent(String callingPackageName, int callingUid) {
        try {
            String recentsComponent = this.mContext.getResources().getString(17040025);
            if (recentsComponent == null) {
                return false;
            }
            String recentsPackage = ComponentName.unflattenFromString(recentsComponent).getPackageName();
            try {
                if (callingUid == this.mContext.getPackageManager().getPackageUid(callingPackageName, 0)) {
                    return callingPackageName.equals(recentsPackage);
                }
                return false;
            } catch (PackageManager.NameNotFoundException e) {
                Slog.e("WindowManager", "Unable to verify if recents component", e);
                return false;
            }
        } catch (Resources.NotFoundException e2) {
            Slog.e("WindowManager", "Unable to verify if recents component", e2);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void grantEmbeddedWindowFocus(Session session, IBinder focusToken, boolean grantFocus) {
        synchronized (this.mGlobalLock) {
            try {
                try {
                    boostPriorityForLockedSection();
                    EmbeddedWindowController.EmbeddedWindow embeddedWindow = this.mEmbeddedWindowController.getByFocusToken(focusToken);
                    if (embeddedWindow == null) {
                        Slog.e("WindowManager", "Embedded window not found");
                        resetPriorityAfterLockedSection();
                    } else if (embeddedWindow.mSession != session) {
                        Slog.e("WindowManager", "Window not in session:" + session);
                        resetPriorityAfterLockedSection();
                    } else {
                        IBinder inputToken = embeddedWindow.getInputChannelToken();
                        if (inputToken == null) {
                            Slog.e("WindowManager", "Focus token found but input channel token not found");
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        SurfaceControl.Transaction t = this.mTransactionFactory.get();
                        int displayId = embeddedWindow.mDisplayId;
                        if (grantFocus) {
                            t.setFocusedWindow(inputToken, embeddedWindow.toString(), displayId).apply();
                            EventLog.writeEvent((int) LOGTAG_INPUT_FOCUS, "Focus request " + embeddedWindow, "reason=grantEmbeddedWindowFocus(true)");
                        } else {
                            DisplayContent displayContent = this.mRoot.getDisplayContent(displayId);
                            WindowState newFocusTarget = displayContent == null ? null : displayContent.findFocusedWindow();
                            if (newFocusTarget == null) {
                                t.setFocusedWindow(null, null, displayId).apply();
                                if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
                                    String protoLogParam0 = String.valueOf(embeddedWindow);
                                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS, 958338552, 0, (String) null, new Object[]{protoLogParam0});
                                }
                                resetPriorityAfterLockedSection();
                                return;
                            }
                            t.setFocusedWindow(newFocusTarget.mInputChannelToken, newFocusTarget.getName(), displayId).apply();
                            EventLog.writeEvent((int) LOGTAG_INPUT_FOCUS, "Focus request " + newFocusTarget, "reason=grantEmbeddedWindowFocus(false)");
                        }
                        if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
                            String protoLogParam02 = String.valueOf(embeddedWindow);
                            String protoLogParam1 = String.valueOf(grantFocus);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS, -2107721178, 0, (String) null, new Object[]{protoLogParam02, protoLogParam1});
                        }
                        resetPriorityAfterLockedSection();
                    }
                } catch (Throwable th) {
                    th = th;
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void grantEmbeddedWindowFocus(Session session, IWindow callingWindow, IBinder targetFocusToken, boolean grantFocus) {
        int i;
        synchronized (this.mGlobalLock) {
            try {
                try {
                    boostPriorityForLockedSection();
                } catch (Throwable th) {
                    th = th;
                    resetPriorityAfterLockedSection();
                    throw th;
                }
                try {
                    WindowState hostWindow = windowForClientLocked(session, callingWindow, false);
                    if (hostWindow == null) {
                        Slog.e("WindowManager", "Host window not found");
                        resetPriorityAfterLockedSection();
                    } else if (hostWindow.mInputChannel == null) {
                        Slog.e("WindowManager", "Host window does not have an input channel");
                        resetPriorityAfterLockedSection();
                    } else {
                        EmbeddedWindowController.EmbeddedWindow embeddedWindow = this.mEmbeddedWindowController.getByFocusToken(targetFocusToken);
                        if (embeddedWindow == null) {
                            Slog.e("WindowManager", "Embedded window not found");
                            resetPriorityAfterLockedSection();
                        } else if (embeddedWindow.mHostWindowState != hostWindow) {
                            Slog.e("WindowManager", "Embedded window does not belong to the host");
                            resetPriorityAfterLockedSection();
                        } else {
                            SurfaceControl.Transaction t = this.mTransactionFactory.get();
                            if (grantFocus) {
                                i = 2;
                                t.requestFocusTransfer(embeddedWindow.getInputChannelToken(), embeddedWindow.toString(), hostWindow.mInputChannel.getToken(), hostWindow.getName(), hostWindow.getDisplayId()).apply();
                                EventLog.writeEvent((int) LOGTAG_INPUT_FOCUS, "Transfer focus request " + embeddedWindow, "reason=grantEmbeddedWindowFocus(true)");
                            } else {
                                i = 2;
                                t.requestFocusTransfer(hostWindow.mInputChannel.getToken(), hostWindow.getName(), embeddedWindow.getInputChannelToken(), embeddedWindow.toString(), hostWindow.getDisplayId()).apply();
                                EventLog.writeEvent((int) LOGTAG_INPUT_FOCUS, "Transfer focus request " + hostWindow, "reason=grantEmbeddedWindowFocus(false)");
                            }
                            if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
                                String protoLogParam0 = String.valueOf(embeddedWindow);
                                String protoLogParam1 = String.valueOf(grantFocus);
                                ProtoLogGroup protoLogGroup = ProtoLogGroup.WM_DEBUG_FOCUS;
                                Object[] objArr = new Object[i];
                                objArr[0] = protoLogParam0;
                                objArr[1] = protoLogParam1;
                                ProtoLogImpl.v(protoLogGroup, -2107721178, 0, (String) null, objArr);
                            }
                            resetPriorityAfterLockedSection();
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public void holdLock(IBinder token, int durationMs) {
        this.mTestUtilityService.verifyHoldLockToken(token);
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                SystemClock.sleep(durationMs);
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
    }

    public String[] getSupportedDisplayHashAlgorithms() {
        return this.mDisplayHashController.getSupportedHashAlgorithms();
    }

    public VerifiedDisplayHash verifyDisplayHash(DisplayHash displayHash) {
        return this.mDisplayHashController.verifyDisplayHash(displayHash);
    }

    public void setDisplayHashThrottlingEnabled(boolean enable) {
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "setDisplayHashThrottle()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        this.mDisplayHashController.setDisplayHashThrottlingEnabled(enable);
    }

    public boolean isTaskSnapshotSupported() {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                z = !this.mTaskSnapshotController.shouldDisableSnapshots();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void generateDisplayHash(Session session, IWindow window, Rect boundsInWindow, String hashAlgorithm, RemoteCallback callback) {
        Rect boundsInDisplay = new Rect(boundsInWindow);
        synchronized (this.mGlobalLock) {
            try {
                try {
                    boostPriorityForLockedSection();
                    WindowState win = windowForClientLocked(session, window, false);
                    if (win == null) {
                        Slog.w("WindowManager", "Failed to generate DisplayHash. Invalid window");
                        this.mDisplayHashController.sendDisplayHashError(callback, -3);
                        resetPriorityAfterLockedSection();
                        return;
                    }
                    if (win.mActivityRecord != null && win.mActivityRecord.isState(ActivityRecord.State.RESUMED)) {
                        DisplayContent displayContent = win.getDisplayContent();
                        if (displayContent == null) {
                            Slog.w("WindowManager", "Failed to generate DisplayHash. Window is not on a display");
                            this.mDisplayHashController.sendDisplayHashError(callback, -4);
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        SurfaceControl displaySurfaceControl = displayContent.getSurfaceControl();
                        this.mDisplayHashController.calculateDisplayHashBoundsLocked(win, boundsInWindow, boundsInDisplay);
                        if (boundsInDisplay.isEmpty()) {
                            Slog.w("WindowManager", "Failed to generate DisplayHash. Bounds are not on screen");
                            this.mDisplayHashController.sendDisplayHashError(callback, -4);
                            resetPriorityAfterLockedSection();
                            return;
                        }
                        resetPriorityAfterLockedSection();
                        int uid = session.mUid;
                        SurfaceControl.LayerCaptureArgs.Builder args = new SurfaceControl.LayerCaptureArgs.Builder(displaySurfaceControl).setUid(uid).setSourceCrop(boundsInDisplay);
                        this.mDisplayHashController.generateDisplayHash(args, boundsInWindow, hashAlgorithm, uid, callback);
                        return;
                    }
                    this.mDisplayHashController.sendDisplayHashError(callback, -3);
                    resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    th = th;
                    resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    boolean shouldRestoreImeVisibility(IBinder imeTargetWindowToken) {
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                WindowState imeTargetWindow = this.mWindowMap.get(imeTargetWindowToken);
                if (imeTargetWindow == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                Task imeTargetWindowTask = imeTargetWindow.getTask();
                if (imeTargetWindowTask == null) {
                    resetPriorityAfterLockedSection();
                    return false;
                }
                resetPriorityAfterLockedSection();
                TaskSnapshot snapshot = getTaskSnapshot(imeTargetWindowTask.mTaskId, imeTargetWindowTask.mUserId, false, false);
                return snapshot != null && snapshot.hasImeSurface();
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getImeDisplayId() {
        int displayId;
        synchronized (this.mGlobalLock) {
            try {
                boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getTopFocusedDisplayContent();
                displayId = dc.getImePolicy() == 0 ? dc.getDisplayId() : 0;
            } catch (Throwable th) {
                resetPriorityAfterLockedSection();
                throw th;
            }
        }
        resetPriorityAfterLockedSection();
        return displayId;
    }

    public void setTaskSnapshotEnabled(boolean enabled) {
        this.mTaskSnapshotController.setTaskSnapshotEnabled(enabled);
    }

    public void setTaskTransitionSpec(TaskTransitionSpec spec) {
        if (!checkCallingPermission("android.permission.MANAGE_ACTIVITY_TASKS", "setTaskTransitionSpec()")) {
            throw new SecurityException("Requires MANAGE_ACTIVITY_TASKS permission");
        }
        this.mTaskTransitionSpec = spec;
    }

    public void clearTaskTransitionSpec() {
        if (!checkCallingPermission("android.permission.MANAGE_ACTIVITY_TASKS", "clearTaskTransitionSpec()")) {
            throw new SecurityException("Requires MANAGE_ACTIVITY_TASKS permission");
        }
        this.mTaskTransitionSpec = null;
    }

    public WmsExt getWmsExt() {
        return this.mWmsExt;
    }

    public void registerTaskFpsCallback(int taskId, ITaskFpsCallback callback) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.ACCESS_FPS_COUNTER") != 0) {
            int pid = Binder.getCallingPid();
            throw new SecurityException("Access denied to process: " + pid + ", must have permission android.permission.ACCESS_FPS_COUNTER");
        } else if (this.mRoot.anyTaskForId(taskId) == null) {
            throw new IllegalArgumentException("no task with taskId: " + taskId);
        } else {
            this.mTaskFpsCallbackController.registerListener(taskId, callback);
        }
    }

    public void unregisterTaskFpsCallback(ITaskFpsCallback callback) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.ACCESS_FPS_COUNTER") != 0) {
            int pid = Binder.getCallingPid();
            throw new SecurityException("Access denied to process: " + pid + ", must have permission android.permission.ACCESS_FPS_COUNTER");
        } else {
            this.mTaskFpsCallbackController.m8335x6c8167ea(callback);
        }
    }

    public Bitmap snapshotTaskForRecents(int taskId) {
        TaskSnapshot taskSnapshot;
        if (!checkCallingPermission("android.permission.READ_FRAME_BUFFER", "snapshotTaskForRecents()")) {
            throw new SecurityException("Requires READ_FRAME_BUFFER permission");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                Task task = this.mRoot.anyTaskForId(taskId, 1);
                if (task == null) {
                    throw new IllegalArgumentException("Failed to find matching task for taskId=" + taskId);
                }
                taskSnapshot = this.mTaskSnapshotController.captureTaskSnapshot(task, false);
            }
            resetPriorityAfterLockedSection();
            if (taskSnapshot == null || taskSnapshot.getHardwareBuffer() == null) {
                return null;
            }
            return Bitmap.wrapHardwareBuffer(taskSnapshot.getHardwareBuffer(), taskSnapshot.getColorSpace());
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void setRecentsAppBehindSystemBars(boolean behindSystemBars) {
        if (!checkCallingPermission("android.permission.START_TASKS_FROM_RECENTS", "setRecentsAppBehindSystemBars()")) {
            throw new SecurityException("Requires START_TASKS_FROM_RECENTS permission");
        }
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                boostPriorityForLockedSection();
                Task recentsApp = this.mRoot.getTask(new Predicate() { // from class: com.android.server.wm.WindowManagerService$$ExternalSyntheticLambda1
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return WindowManagerService.lambda$setRecentsAppBehindSystemBars$17((Task) obj);
                    }
                });
                if (recentsApp != null) {
                    recentsApp.getTask().setCanAffectSystemUiFlags(behindSystemBars);
                    this.mWindowPlacerLocked.requestTraversal();
                }
            }
            resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$setRecentsAppBehindSystemBars$17(Task task) {
        return task.isActivityTypeHomeOrRecents() && task.getTopVisibleActivity() != null;
    }

    public void setKeyguardStatus(boolean keyguardShowing) {
        TranWaterMark tranWaterMark = this.tranWaterMark;
        if (tranWaterMark != null) {
            tranWaterMark.setKeyguardStatus(keyguardShowing);
        }
    }

    private void createTranWatermark() {
        if ((!TranWaterMark.TECNO_DEMO_PHONE_SUPPORT && !TranWaterMark.INFINIX_DEMO_PHONE_SUPPORT && !TranWaterMark.INTERNAL_PHONE_SUPPORT && !TranWaterMark.FANS_SUPPORT) || this.tranWaterMark != null) {
            return;
        }
        this.tranWaterMark = new TranWaterMark(this.mContext);
    }

    public int getNextTranMultiDisplayAreaId() {
        int i = this.mTranMultiDisplayAreaId;
        this.mTranMultiDisplayAreaId = i + 1;
        return i;
    }

    /* loaded from: classes2.dex */
    public static class TranWindowManagerServiceProxy {
        private WindowManagerService mWms;

        public TranWindowManagerServiceProxy(WindowManagerService wms) {
            this.mWms = null;
            this.mWms = wms;
        }

        public void requestTraversal() {
            WindowManagerService windowManagerService = this.mWms;
            if (windowManagerService != null) {
                windowManagerService.requestTraversal();
            }
        }

        public void registerPointerEventListener(WindowManagerPolicyConstants.PointerEventListener listener) {
            WindowManagerService windowManagerService = this.mWms;
            if (windowManagerService != null && listener != null) {
                DisplayContent defaultDisplay = windowManagerService.mRoot.getDisplayContent(0);
                defaultDisplay.registerPointerEventListener(listener);
            }
        }

        public void unregisterPointerEventListener(WindowManagerPolicyConstants.PointerEventListener listener) {
            WindowManagerService windowManagerService = this.mWms;
            if (windowManagerService != null && listener != null) {
                DisplayContent defaultDisplay = windowManagerService.mRoot.getDisplayContent(0);
                defaultDisplay.unregisterPointerEventListener(listener);
            }
        }
    }
}
