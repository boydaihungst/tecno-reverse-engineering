package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.IActivityClientController;
import android.app.IActivityController;
import android.app.IActivityTaskManager;
import android.app.IAppTask;
import android.app.IApplicationThread;
import android.app.IAssistDataReceiver;
import android.app.INotificationManager;
import android.app.ITaskAnimation;
import android.app.ITaskSplitManager;
import android.app.ITaskSplitManagerListener;
import android.app.ITaskStackListener;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.PictureInPictureUiState;
import android.app.ProfilerInfo;
import android.app.ThunderbackConfig;
import android.app.WaitResult;
import android.app.admin.DevicePolicyCache;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.app.compat.CompatChanges;
import android.app.usage.UsageStatsManagerInternal;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.LocusId;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.InputConstants;
import android.os.LocaleList;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UpdateLock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.service.dreams.DreamActivity;
import android.service.dreams.DreamManagerInternal;
import android.service.voice.IVoiceInteractionSession;
import android.service.voice.VoiceInteractionManagerInternal;
import android.sysprop.DisplayProperties;
import android.telecom.TelecomManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.IRecentsAnimationRunner;
import android.view.IWindow;
import android.view.MotionEvent;
import android.view.MultiTaskRemoteAnimationAdapter;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationDefinition;
import android.view.SurfaceControl;
import android.window.BackNavigationInfo;
import android.window.IWindowContainerTransactionCallback;
import android.window.IWindowContainerTransactionCallbackSync;
import android.window.IWindowOrganizerController;
import android.window.SplashScreenView;
import android.window.TaskSnapshot;
import android.window.WindowContainerToken;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.app.ProcessMap;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.TransferPipe;
import com.android.internal.policy.AttributeCache;
import com.android.internal.policy.KeyguardDismissCallback;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.function.HeptConsumer;
import com.android.internal.util.function.HexConsumer;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalManagerRegistry;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;
import com.android.server.UiThread;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.AppTimeTracker;
import com.android.server.am.AssistDataRequester;
import com.android.server.am.BaseErrorDialog;
import com.android.server.am.PendingIntentController;
import com.android.server.am.PendingIntentRecord;
import com.android.server.am.UserState;
import com.android.server.firewall.IntentFirewall;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.policy.PermissionPolicyInternal;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.sdksandbox.SdkSandboxManagerLocal;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.uri.NeededUriGrants;
import com.android.server.uri.UriGrantsManagerInternal;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.RootWindowContainer;
import com.android.server.wm.Task;
import com.mediatek.server.MtkSystemServer;
import com.mediatek.server.MtkSystemServiceFactory;
import com.mediatek.server.am.AmsExt;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.multiwindow.ITranMultiWindow;
import com.transsion.hubcore.server.wm.ITranActivityTaskManagerService;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import com.transsion.hubcore.sourceconnect.ITranSourceConnectManager;
import com.transsion.hubcore.sru.ITranSruManager;
import defpackage.CompanionAppsPermissions;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class ActivityTaskManagerService extends IActivityTaskManager.Stub {
    static final long ACTIVITY_BG_START_GRACE_PERIOD_MS = 10000;
    static final boolean ANIMATE = true;
    static final int APP_SWITCH_ALLOW = 2;
    static final int APP_SWITCH_DISALLOW = 0;
    static final int APP_SWITCH_FG_ONLY = 1;
    private static final long DOZE_ANIMATING_STATE_RETAIN_TIME_MS = 2000;
    public static final String DUMP_ACTIVITIES_CMD = "activities";
    public static final String DUMP_ACTIVITIES_SHORT_CMD = "a";
    public static final String DUMP_CONTAINERS_CMD = "containers";
    public static final String DUMP_LASTANR_CMD = "lastanr";
    public static final String DUMP_LASTANR_TRACES_CMD = "lastanr-traces";
    public static final String DUMP_RECENTS_CMD = "recents";
    public static final String DUMP_RECENTS_SHORT_CMD = "r";
    public static final String DUMP_STARTER_CMD = "starter";
    public static final String DUMP_TOP_RESUMED_ACTIVITY = "top-resumed";
    static final long INSTRUMENTATION_KEY_DISPATCHING_TIMEOUT_MILLIS = 60000;
    static final int LAYOUT_REASON_CONFIG_CHANGED = 1;
    static final int LAYOUT_REASON_VISIBILITY_CHANGED = 2;
    private static final boolean LIGHTNING_MULTI_WINDOW_SUPPORT_V3_0;
    private static final int PENDING_ASSIST_EXTRAS_LONG_TIMEOUT = 2000;
    private static final int PENDING_ASSIST_EXTRAS_TIMEOUT = 500;
    private static final int PENDING_AUTOFILL_ASSIST_STRUCTURE_TIMEOUT = 2000;
    static final int POWER_MODE_REASON_ALL = 3;
    static final int POWER_MODE_REASON_CHANGE_DISPLAY = 2;
    static final int POWER_MODE_REASON_START_ACTIVITY = 1;
    static final int POWER_MODE_REASON_UNKNOWN_VISIBILITY = 4;
    private static final long POWER_MODE_UNKNOWN_VISIBILITY_TIMEOUT_MS = 1000;
    public static final int RELAUNCH_REASON_FREE_RESIZE = 2;
    public static final int RELAUNCH_REASON_NONE = 0;
    public static final int RELAUNCH_REASON_WINDOWING_MODE_RESIZE = 1;
    private static final long RESUME_FG_APP_SWITCH_MS = 500;
    private static final boolean THUNDERBACK_SUPPORT_TECNO_V2_1;
    private static final boolean THUNDERBACK_SUPPORT_V2_1;
    final int GL_ES_VERSION;
    private int[] mAccessibilityServiceUids;
    private final Runnable mAcquireMultiWindowFocusRunnbale;
    final MirrorActiveUids mActiveUids;
    ComponentName mActiveVoiceInteractionServiceComponent;
    ActivityClientController mActivityClientController;
    private SparseArray<ActivityInterceptorCallback> mActivityInterceptorCallbacks;
    private ActivityStartController mActivityStartController;
    final SparseArray<ArrayMap<String, Integer>> mAllowAppSwitchUids;
    ActivityManagerInternal mAmInternal;
    public AmsExt mAmsExt;
    private final List<android.app.AnrController> mAnrController;
    private AppOpsManager mAppOpsManager;
    private volatile int mAppSwitchesState;
    AppWarnings mAppWarnings;
    private final BackNavigationController mBackNavigationController;
    private BackgroundActivityStartCallback mBackgroundActivityStartCallback;
    private final Map<Integer, Set<Integer>> mCompanionAppUidsMap;
    CompatModePackages mCompatModePackages;
    private int mConfigurationSeq;
    Context mContext;
    public IActivityController mController;
    boolean mControllerIsAMonkey;
    AppTimeTracker mCurAppTimeTracker;
    boolean mDevEnableNonResizableMultiWindow;
    private int mDeviceOwnerUid;
    private volatile boolean mDreaming;
    final int mFactoryTest;
    boolean mForceResizableActivities;
    private int mGlobalAssetsSeq;
    final WindowManagerGlobalLock mGlobalLock;
    final Object mGlobalLockWithoutBoost;
    H mH;
    boolean mHasHeavyWeightFeature;
    boolean mHasLeanbackFeature;
    volatile WindowProcessController mHeavyWeightProcess;
    volatile WindowProcessController mHomeProcess;
    IntentFirewall mIntentFirewall;
    final ActivityTaskManagerInternal mInternal;
    KeyguardController mKeyguardController;
    private boolean mKeyguardShown;
    int mLargeScreenSmallestScreenWidthDp;
    String mLastANRState;
    ActivityRecord mLastResumedActivity;
    private volatile long mLastStopAppSwitchesTime;
    private int mLaunchPowerModeReasons;
    private int mLayoutReasons;
    private final ClientLifecycleManager mLifecycleManager;
    private LockTaskController mLockTaskController;
    float mMinPercentageMultiWindowSupportHeight;
    float mMinPercentageMultiWindowSupportWidth;
    public boolean mNotAllowKeyguardGoingAwayQuickly;
    PackageConfigPersister mPackageConfigPersister;
    private final ArrayList<PendingAssistExtras> mPendingAssistExtras;
    PendingIntentController mPendingIntentController;
    private PermissionPolicyInternal mPermissionPolicyInternal;
    private PackageManagerInternal mPmInternal;
    private PowerManagerInternal mPowerManagerInternal;
    volatile WindowProcessController mPreviousProcess;
    private long mPreviousProcessVisibleTime;
    final WindowProcessControllerMap mProcessMap;
    final ProcessMap<WindowProcessController> mProcessNames;
    String mProfileApp;
    WindowProcessController mProfileProc;
    ProfilerInfo mProfilerInfo;
    private RecentTasks mRecentTasks;
    int mRespectsActivityMinWidthHeightMultiWindow;
    private volatile boolean mRetainPowerModeAndTopProcessState;
    RootWindowContainer mRootWindowContainer;
    IVoiceInteractionSession mRunningVoice;
    final List<ActivityTaskManagerInternal.ScreenObserver> mScreenObservers;
    private SettingObserver mSettingsObserver;
    private boolean mShowDialogs;
    boolean mShuttingDown;
    private volatile boolean mSleeping;
    private Object mStartActivityLock;
    private StatusBarManagerInternal mStatusBarManagerInternal;
    private String[] mSupportedSystemLocales;
    boolean mSupportsExpandedPictureInPicture;
    boolean mSupportsFreeformWindowManagement;
    boolean mSupportsMultiDisplay;
    boolean mSupportsMultiWindow;
    int mSupportsNonResizableMultiWindow;
    boolean mSupportsPictureInPicture;
    boolean mSupportsSplitScreenMultiWindow;
    boolean mSuppressResizeConfigChanges;
    private ComponentName mSysUiServiceComponent;
    final ActivityThread mSystemThread;
    private TaskChangeNotificationController mTaskChangeNotificationController;
    TaskFragmentOrganizerController mTaskFragmentOrganizerController;
    TaskOrganizerController mTaskOrganizerController;
    private ITaskSplitManager mTaskSplitManager;
    ActivityTaskSupervisor mTaskSupervisor;
    private Configuration mTempConfig;
    private int mThumbnailHeight;
    private int mThumbnailWidth;
    final UpdateConfigurationResult mTmpUpdateConfigurationResult;
    String mTopAction;
    volatile WindowProcessController mTopApp;
    ComponentName mTopComponent;
    String mTopData;
    volatile int mTopProcessState;
    private ActivityRecord mTracedResumedActivity;
    UriGrantsManagerInternal mUgmInternal;
    final Context mUiContext;
    UiHandler mUiHandler;
    private final UpdateLock mUpdateLock;
    private final Runnable mUpdateOomAdjRunnable;
    private UsageStatsManagerInternal mUsageStatsInternal;
    private UserManagerService mUserManager;
    private int mViSessionId;
    final VisibleActivityProcessTracker mVisibleActivityProcessTracker;
    PowerManager.WakeLock mVoiceWakeLock;
    int mVr2dDisplayId;
    VrController mVrController;
    WindowManagerService mWindowManager;
    WindowOrganizerController mWindowOrganizerController;
    String[] threadSchedulerWhilelist;
    private static final String TAG = "ActivityTaskManager";
    static final String TAG_ROOT_TASK = TAG + ActivityTaskManagerDebugConfig.POSTFIX_ROOT_TASK;
    static final String TAG_SWITCH = TAG + ActivityTaskManagerDebugConfig.POSTFIX_SWITCH;
    private static final boolean mIsLucidDisabled = SystemProperties.get("ro.lucid.disabled").equals("1");
    public static boolean AJUST_LAUNCH_TIME = SystemProperties.getBoolean("persist.sys.ajustlaunch", false);
    private static final boolean FINGERPRINT_WAKEUP_PERFORMANCE_OPT = "1".equals(SystemProperties.get("ro.fingerprint_wakeup_performance_opt", ""));
    private static final LauncherApps.Callback mPackageCallback = new LauncherApps.Callback() { // from class: com.android.server.wm.ActivityTaskManagerService.4
        @Override // android.content.pm.LauncherApps.Callback
        public void onPackageRemoved(String packageName, UserHandle user) {
            ActivityStarter.onPackageCallback(packageName, user, "android.intent.action.PACKAGE_REMOVED");
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackageAdded(String packageName, UserHandle user) {
            ActivityStarter.onPackageCallback(packageName, user, "android.intent.action.PACKAGE_ADDED");
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackageChanged(String packageName, UserHandle user) {
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
        }

        @Override // android.content.pm.LauncherApps.Callback
        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface AppSwitchState {
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface HotPath {
        public static final int LRU_UPDATE = 2;
        public static final int NONE = 0;
        public static final int OOM_ADJUSTMENT = 1;
        public static final int PROCESS_CHANGE = 3;
        public static final int START_SERVICE = 4;

        int caller() default 0;
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface LayoutReason {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface PowerModeReason {
    }

    static native void setLucidFgApp(String str);

    static {
        THUNDERBACK_SUPPORT_V2_1 = 1 == SystemProperties.getInt("ro.product.tranthunderback.support", 0);
        THUNDERBACK_SUPPORT_TECNO_V2_1 = 1 == SystemProperties.getInt("ro.product.tecno.thunderback.support", 0);
        LIGHTNING_MULTI_WINDOW_SUPPORT_V3_0 = 1 == SystemProperties.getInt("ro.product.lightning_multi_window.support", 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class UpdateConfigurationResult {
        boolean activityRelaunched;
        int changes;

        UpdateConfigurationResult() {
        }

        void reset() {
            this.changes = 0;
            this.activityRelaunched = false;
        }
    }

    /* loaded from: classes2.dex */
    private final class SettingObserver extends ContentObserver {
        private final Uri mFontScaleUri;
        private final Uri mFontWeightAdjustmentUri;
        private final Uri mHideErrorDialogsUri;

        SettingObserver() {
            super(ActivityTaskManagerService.this.mH);
            Uri uriFor = Settings.System.getUriFor("font_scale");
            this.mFontScaleUri = uriFor;
            Uri uriFor2 = Settings.Global.getUriFor("hide_error_dialogs");
            this.mHideErrorDialogsUri = uriFor2;
            Uri uriFor3 = Settings.Secure.getUriFor("font_weight_adjustment");
            this.mFontWeightAdjustmentUri = uriFor3;
            ContentResolver resolver = ActivityTaskManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(uriFor, false, this, -1);
            resolver.registerContentObserver(uriFor2, false, this, -1);
            resolver.registerContentObserver(uriFor3, false, this, -1);
        }

        public void onChange(boolean selfChange, Collection<Uri> uris, int flags, int userId) {
            for (Uri uri : uris) {
                if (this.mFontScaleUri.equals(uri)) {
                    ActivityTaskManagerService.this.updateFontScaleIfNeeded(userId);
                } else if (this.mHideErrorDialogsUri.equals(uri)) {
                    synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            ActivityTaskManagerService activityTaskManagerService = ActivityTaskManagerService.this;
                            activityTaskManagerService.updateShouldShowDialogsLocked(activityTaskManagerService.getGlobalConfiguration());
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else if (this.mFontWeightAdjustmentUri.equals(uri)) {
                    ActivityTaskManagerService.this.updateFontWeightAdjustmentIfNeeded(userId);
                }
            }
        }
    }

    public ActivityTaskManagerService(Context context) {
        WindowManagerGlobalLock windowManagerGlobalLock = new WindowManagerGlobalLock();
        this.mGlobalLock = windowManagerGlobalLock;
        this.mGlobalLockWithoutBoost = windowManagerGlobalLock;
        this.mActiveUids = new MirrorActiveUids();
        this.mProcessNames = new ProcessMap<>();
        this.mProcessMap = new WindowProcessControllerMap();
        this.mKeyguardShown = false;
        this.mViSessionId = 1000;
        this.mPendingAssistExtras = new ArrayList<>();
        this.mCompanionAppUidsMap = new ArrayMap();
        this.mActivityInterceptorCallbacks = new SparseArray<>();
        this.mTmpUpdateConfigurationResult = new UpdateConfigurationResult();
        this.mSupportedSystemLocales = null;
        this.mTempConfig = new Configuration();
        this.mAppSwitchesState = 2;
        this.mAnrController = new ArrayList();
        this.mController = null;
        this.mControllerIsAMonkey = false;
        this.mTopAction = "android.intent.action.MAIN";
        this.mProfileApp = null;
        this.mProfileProc = null;
        this.mProfilerInfo = null;
        this.mUpdateLock = new UpdateLock("immersive");
        this.mAllowAppSwitchUids = new SparseArray<>();
        this.mScreenObservers = new ArrayList();
        this.mVr2dDisplayId = -1;
        this.mTopProcessState = 2;
        this.mShowDialogs = true;
        this.mShuttingDown = false;
        this.mAccessibilityServiceUids = new int[0];
        this.mDeviceOwnerUid = -1;
        this.mAmsExt = MtkSystemServiceFactory.getInstance().makeAmsExt();
        this.mUpdateOomAdjRunnable = new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                ActivityTaskManagerService.this.mAmInternal.updateOomAdj();
            }
        };
        this.threadSchedulerWhilelist = null;
        this.mAcquireMultiWindowFocusRunnbale = new AnonymousClass3();
        this.mStartActivityLock = new Object();
        this.mContext = context;
        this.mFactoryTest = FactoryTest.getMode();
        ActivityThread currentActivityThread = ActivityThread.currentActivityThread();
        this.mSystemThread = currentActivityThread;
        this.mUiContext = currentActivityThread.getSystemUiContext();
        this.mLifecycleManager = new ClientLifecycleManager();
        this.mVisibleActivityProcessTracker = new VisibleActivityProcessTracker(this);
        this.mInternal = new LocalService();
        this.GL_ES_VERSION = SystemProperties.getInt("ro.opengles.version", 0);
        WindowOrganizerController windowOrganizerController = new WindowOrganizerController(this);
        this.mWindowOrganizerController = windowOrganizerController;
        this.mTaskOrganizerController = windowOrganizerController.mTaskOrganizerController;
        this.mTaskFragmentOrganizerController = this.mWindowOrganizerController.mTaskFragmentOrganizerController;
        this.mBackNavigationController = BackNavigationController.isEnabled() ? new BackNavigationController() : null;
        ITranActivityTaskManagerService.Instance().onConstruct(context, this, this.mWindowManager, TAG);
    }

    public void onSystemReady() {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                PackageManager pm = this.mContext.getPackageManager();
                this.mHasHeavyWeightFeature = pm.hasSystemFeature("android.software.cant_save_state");
                this.mHasLeanbackFeature = pm.hasSystemFeature("android.software.leanback");
                this.mVrController.onSystemReady();
                this.mRecentTasks.onSystemReadyLocked();
                this.mTaskSupervisor.onSystemReady();
                this.mActivityClientController.onSystemReady();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void onInitPowerManagement() {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mTaskSupervisor.initPowerManagement();
                PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
                this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
                PowerManager.WakeLock newWakeLock = pm.newWakeLock(1, "*voice*");
                this.mVoiceWakeLock = newWakeLock;
                newWakeLock.setReferenceCounted(false);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void installSystemProviders() {
        this.mSettingsObserver = new SettingObserver();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1034=4] */
    /* JADX WARN: Removed duplicated region for block: B:66:0x0166 A[Catch: all -> 0x01ad, TRY_LEAVE, TryCatch #5 {all -> 0x01ad, blocks: (B:64:0x015b, B:66:0x0166), top: B:98:0x015b }] */
    /* JADX WARN: Removed duplicated region for block: B:73:0x0188  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void retrieveSettings(ContentResolver resolver) {
        boolean supportsExpandedPictureInPicture;
        boolean freeformWindowManagement = this.mContext.getPackageManager().hasSystemFeature("android.software.freeform_window_management") || Settings.Global.getInt(resolver, "enable_freeform_support", 0) != 0;
        boolean supportsMultiWindow = ActivityTaskManager.supportsMultiWindow(this.mContext);
        boolean supportsPictureInPicture = supportsMultiWindow && this.mContext.getPackageManager().hasSystemFeature("android.software.picture_in_picture");
        boolean supportsExpandedPictureInPicture2 = supportsPictureInPicture && this.mContext.getPackageManager().hasSystemFeature("android.software.expanded_picture_in_picture");
        boolean supportsSplitScreenMultiWindow = ActivityTaskManager.supportsSplitScreenMultiWindow(this.mContext);
        boolean supportsMultiDisplay = this.mContext.getPackageManager().hasSystemFeature("android.software.activities_on_secondary_displays");
        boolean forceRtl = Settings.Global.getInt(resolver, "debug.force_rtl", 0) != 0;
        boolean forceResizable = Settings.Global.getInt(resolver, "force_resizable_activities", 0) != 0;
        boolean devEnableNonResizableMultiWindow = Settings.Global.getInt(resolver, "enable_non_resizable_multi_window", 0) != 0;
        int supportsNonResizableMultiWindow = this.mContext.getResources().getInteger(17694956);
        int respectsActivityMinWidthHeightMultiWindow = this.mContext.getResources().getInteger(17694926);
        float minPercentageMultiWindowSupportHeight = this.mContext.getResources().getFloat(17105087);
        float minPercentageMultiWindowSupportWidth = this.mContext.getResources().getFloat(17105088);
        boolean supportsExpandedPictureInPicture3 = supportsExpandedPictureInPicture2;
        int largeScreenSmallestScreenWidthDp = this.mContext.getResources().getInteger(17694847);
        boolean freeformWindowManagement2 = ITranWindowManagerService.Instance().fixUpFreeformWindowManagement(freeformWindowManagement, supportsMultiWindow, forceResizable);
        DisplayProperties.debug_force_rtl(Boolean.valueOf(forceRtl));
        Configuration configuration = new Configuration();
        Settings.System.getConfiguration(resolver, configuration);
        if (forceRtl) {
            configuration.setLayoutDirection(configuration.locale);
        }
        synchronized (this.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    this.mForceResizableActivities = forceResizable;
                    this.mDevEnableNonResizableMultiWindow = devEnableNonResizableMultiWindow;
                    this.mSupportsNonResizableMultiWindow = supportsNonResizableMultiWindow;
                    this.mRespectsActivityMinWidthHeightMultiWindow = respectsActivityMinWidthHeightMultiWindow;
                    this.mMinPercentageMultiWindowSupportHeight = minPercentageMultiWindowSupportHeight;
                    this.mMinPercentageMultiWindowSupportWidth = minPercentageMultiWindowSupportWidth;
                    this.mLargeScreenSmallestScreenWidthDp = largeScreenSmallestScreenWidthDp;
                    boolean multiWindowFormEnabled = freeformWindowManagement2 || supportsSplitScreenMultiWindow || supportsPictureInPicture || supportsMultiDisplay;
                    try {
                        try {
                            if (!supportsMultiWindow && !forceResizable) {
                                supportsExpandedPictureInPicture = supportsExpandedPictureInPicture3;
                            } else if (multiWindowFormEnabled) {
                                try {
                                    this.mSupportsMultiWindow = true;
                                    this.mSupportsFreeformWindowManagement = freeformWindowManagement2;
                                    this.mSupportsSplitScreenMultiWindow = supportsSplitScreenMultiWindow;
                                    this.mSupportsPictureInPicture = supportsPictureInPicture;
                                    supportsExpandedPictureInPicture = supportsExpandedPictureInPicture3;
                                } catch (Throwable th) {
                                    th = th;
                                }
                                try {
                                    this.mSupportsExpandedPictureInPicture = supportsExpandedPictureInPicture;
                                    this.mSupportsMultiDisplay = supportsMultiDisplay;
                                    this.mWindowManager.mRoot.onSettingsRetrieved();
                                    updateConfigurationLocked(configuration, null, true);
                                    Configuration globalConfig = getGlobalConfiguration();
                                    if (!ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                                        String protoLogParam0 = String.valueOf(globalConfig);
                                        ProtoLogGroup protoLogGroup = ProtoLogGroup.WM_DEBUG_CONFIGURATION;
                                        Object[] objArr = new Object[1];
                                        try {
                                            objArr[0] = protoLogParam0;
                                            ProtoLogImpl.v(protoLogGroup, -1305755880, 0, (String) null, objArr);
                                        } catch (Throwable th2) {
                                            th = th2;
                                            WindowManagerService.resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                    }
                                    Resources res = this.mContext.getResources();
                                    this.mThumbnailWidth = res.getDimensionPixelSize(17104898);
                                    this.mThumbnailHeight = res.getDimensionPixelSize(17104897);
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return;
                                } catch (Throwable th3) {
                                    th = th3;
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    throw th;
                                }
                            } else {
                                supportsExpandedPictureInPicture = supportsExpandedPictureInPicture3;
                            }
                            updateConfigurationLocked(configuration, null, true);
                            Configuration globalConfig2 = getGlobalConfiguration();
                            if (!ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                            }
                            Resources res2 = this.mContext.getResources();
                            this.mThumbnailWidth = res2.getDimensionPixelSize(17104898);
                            this.mThumbnailHeight = res2.getDimensionPixelSize(17104897);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        } catch (Throwable th4) {
                            th = th4;
                        }
                        this.mSupportsMultiWindow = false;
                        this.mSupportsFreeformWindowManagement = false;
                        this.mSupportsSplitScreenMultiWindow = false;
                        this.mSupportsPictureInPicture = false;
                        this.mSupportsExpandedPictureInPicture = false;
                        this.mSupportsMultiDisplay = false;
                        this.mWindowManager.mRoot.onSettingsRetrieved();
                    } catch (Throwable th5) {
                        th = th5;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th6) {
                    th = th6;
                }
            } catch (Throwable th7) {
                th = th7;
            }
        }
    }

    public WindowManagerGlobalLock getGlobalLock() {
        return this.mGlobalLock;
    }

    public ActivityTaskManagerInternal getAtmInternal() {
        return this.mInternal;
    }

    public void initialize(IntentFirewall intentFirewall, PendingIntentController intentController, Looper looper) {
        this.mH = new H(looper);
        this.mUiHandler = new UiHandler();
        this.mIntentFirewall = intentFirewall;
        File systemDir = SystemServiceManager.ensureSystemDir();
        this.mAppWarnings = createAppWarnings(this.mUiContext, this.mH, this.mUiHandler, systemDir);
        this.mCompatModePackages = new CompatModePackages(this, systemDir, this.mH);
        this.mPendingIntentController = intentController;
        this.mTaskSupervisor = createTaskSupervisor();
        this.mActivityClientController = new ActivityClientController(this);
        this.mTaskChangeNotificationController = new TaskChangeNotificationController(this.mTaskSupervisor, this.mH);
        this.mLockTaskController = new LockTaskController(this.mContext, this.mTaskSupervisor, this.mH, this.mTaskChangeNotificationController);
        this.mActivityStartController = new ActivityStartController(this);
        setRecentTasks(new RecentTasks(this, this.mTaskSupervisor));
        this.mVrController = new VrController(this.mGlobalLock);
        this.mKeyguardController = this.mTaskSupervisor.getKeyguardController();
        this.mPackageConfigPersister = new PackageConfigPersister(this.mTaskSupervisor.mPersisterQueue, this);
        this.threadSchedulerWhilelist = this.mContext.getResources().getStringArray(17236193);
    }

    public void onActivityManagerInternalAdded() {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
                this.mUgmInternal = (UriGrantsManagerInternal) LocalServices.getService(UriGrantsManagerInternal.class);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int increaseConfigurationSeqLocked() {
        int i = this.mConfigurationSeq + 1;
        this.mConfigurationSeq = i;
        int max = Math.max(i, 1);
        this.mConfigurationSeq = max;
        return max;
    }

    protected ActivityTaskSupervisor createTaskSupervisor() {
        ActivityTaskSupervisor supervisor = new ActivityTaskSupervisor(this, this.mH.getLooper());
        supervisor.initialize();
        return supervisor;
    }

    protected AppWarnings createAppWarnings(Context uiContext, Handler handler, Handler uiHandler, File systemDir) {
        return new AppWarnings(this, uiContext, handler, uiHandler, systemDir);
    }

    public void setWindowManager(WindowManagerService wm) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mWindowManager = wm;
                this.mRootWindowContainer = wm.mRoot;
                this.mWindowOrganizerController.setWindowManager(wm);
                this.mTempConfig.setToDefaults();
                this.mTempConfig.setLocales(LocaleList.getDefault());
                this.mTempConfig.seq = 1;
                this.mConfigurationSeq = 1;
                this.mRootWindowContainer.onConfigurationChanged(this.mTempConfig);
                this.mLockTaskController.setWindowManager(wm);
                this.mTaskSupervisor.setWindowManager(wm);
                this.mRootWindowContainer.setWindowManager(wm);
                BackNavigationController backNavigationController = this.mBackNavigationController;
                if (backNavigationController != null) {
                    backNavigationController.setTaskSnapshotController(wm.mTaskSnapshotController);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setUsageStatsManager(UsageStatsManagerInternal usageStatsManager) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mUsageStatsInternal = usageStatsManager;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserManagerService getUserManager() {
        if (this.mUserManager == null) {
            IBinder b = ServiceManager.getService("user");
            this.mUserManager = IUserManager.Stub.asInterface(b);
        }
        return this.mUserManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppOpsManager getAppOpsManager() {
        if (this.mAppOpsManager == null) {
            this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        }
        return this.mAppOpsManager;
    }

    boolean hasUserRestriction(String restriction, int userId) {
        return getUserManager().hasUserRestriction(restriction, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSystemAlertWindowPermission(int callingUid, int callingPid, String callingPackage) {
        int mode = getAppOpsManager().noteOpNoThrow(24, callingUid, callingPackage, (String) null, "");
        return mode == 3 ? checkPermission("android.permission.SYSTEM_ALERT_WINDOW", callingPid, callingUid) == 0 : mode == 0;
    }

    protected void setRecentTasks(RecentTasks recentTasks) {
        this.mRecentTasks = recentTasks;
        this.mTaskSupervisor.setRecentTasks(recentTasks);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RecentTasks getRecentTasks() {
        return this.mRecentTasks;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ClientLifecycleManager getLifecycleManager() {
        return this.mLifecycleManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStartController getActivityStartController() {
        return this.mActivityStartController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskChangeNotificationController getTaskChangeNotificationController() {
        return this.mTaskChangeNotificationController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LockTaskController getLockTaskController() {
        return this.mLockTaskController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TransitionController getTransitionController() {
        return this.mWindowOrganizerController.getTransitionController();
    }

    Configuration getGlobalConfigurationForCallingPid() {
        int pid = Binder.getCallingPid();
        return getGlobalConfigurationForPid(pid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Configuration getGlobalConfigurationForPid(int pid) {
        if (pid == ActivityManagerService.MY_PID || pid < 0) {
            return getGlobalConfiguration();
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowProcessController app = this.mProcessMap.getProcess(pid);
                if (UserHandle.getCallingUserId() != 0 && app != null && "com.android.settings".equals(app.mName)) {
                    Configuration globalConfiguration = getGlobalConfiguration();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return globalConfiguration;
                }
                Configuration configuration = app != null ? app.getConfiguration() : getGlobalConfiguration();
                WindowManagerService.resetPriorityAfterLockedSection();
                return configuration;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public ConfigurationInfo getDeviceConfigurationInfo() {
        ConfigurationInfo config = new ConfigurationInfo();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Configuration globalConfig = getGlobalConfigurationForCallingPid();
                config.reqTouchScreen = globalConfig.touchscreen;
                config.reqKeyboardType = globalConfig.keyboard;
                config.reqNavigation = globalConfig.navigation;
                if (globalConfig.navigation == 2 || globalConfig.navigation == 3) {
                    config.reqInputFeatures |= 2;
                }
                if (globalConfig.keyboard != 0 && globalConfig.keyboard != 1) {
                    config.reqInputFeatures |= 1;
                }
                config.reqGlEsVersion = this.GL_ES_VERSION;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return config;
    }

    public BackgroundActivityStartCallback getBackgroundActivityStartCallback() {
        return this.mBackgroundActivityStartCallback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SparseArray<ActivityInterceptorCallback> getActivityInterceptorCallbacks() {
        return this.mActivityInterceptorCallbacks;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void start() {
        LocalServices.addService(ActivityTaskManagerInternal.class, this.mInternal);
    }

    /* loaded from: classes2.dex */
    public static final class Lifecycle extends SystemService {
        private final ActivityTaskManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new ActivityTaskManagerService(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            publishBinderService("activity_task", this.mService);
            this.mService.start();
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocked(SystemService.TargetUser user) {
            synchronized (this.mService.getGlobalLock()) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    this.mService.mTaskSupervisor.onUserUnlocked(user.getUserIdentifier());
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.SystemService
        public void onUserStopped(SystemService.TargetUser user) {
            synchronized (this.mService.getGlobalLock()) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    this.mService.mTaskSupervisor.mLaunchParamsPersister.onCleanupUser(user.getUserIdentifier());
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        public ActivityTaskManagerService getService() {
            return this.mService;
        }
    }

    public final int startActivity(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions) {
        if (ITranGriffinFeature.Instance().isGriffinSupport()) {
            int callerUid = Binder.getCallingUid();
            int callerPid = Binder.getCallingPid();
            String action = intent.getAction();
            String pkg = intent.getPackage();
            ComponentName cmp = intent.getComponent();
            if (ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
                Slog.d("TranGriffin/startActivity", "caller:" + callingPackage + ",uid=" + callerUid + ",pid=" + callerPid + ",action=" + action + ",pkg=" + pkg + ",cmp=" + cmp);
            }
            boolean limited = ITranActivityTaskManagerService.Instance().hookStartActivity(callingPackage, intent);
            if (limited) {
                Slog.d(TAG, "Limit startActivity for " + intent + " from pkg:" + callingPackage + ",uid:" + callerUid + ",pid:" + callerPid);
                return 0;
            }
        }
        return startActivityAsUser(caller, callingPackage, callingFeatureId, intent, resolvedType, resultTo, resultWho, requestCode, startFlags, profilerInfo, bOptions, UserHandle.getCallingUserId());
    }

    public final int startActivities(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent[] intents, String[] resolvedTypes, IBinder resultTo, Bundle bOptions, int userId) {
        assertPackageMatchesCallingUid(callingPackage);
        enforceNotIsolatedCaller("startActivities");
        return getActivityStartController().startActivities(caller, -1, 0, -1, callingPackage, callingFeatureId, intents, resolvedTypes, resultTo, SafeActivityOptions.fromBundle(bOptions), handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, "startActivities"), "startActivities", null, false);
    }

    public int startActivityAsUser(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int userId) {
        return startActivityAsUser(caller, callingPackage, callingFeatureId, intent, resolvedType, resultTo, resultWho, requestCode, startFlags, profilerInfo, bOptions, userId, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int startActivityAsUser(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int userId, boolean validateIncomingUser) {
        assertPackageMatchesCallingUid(callingPackage);
        enforceNotIsolatedCaller("startActivityAsUser");
        if (ITranGriffinFeature.Instance().isGriffinDebugOpen() && ITranGriffinFeature.Instance().isGriffinSupport()) {
            int callerUid = Binder.getCallingUid();
            int callerPid = Binder.getCallingPid();
            String action = intent.getAction();
            String pkg = intent.getPackage();
            ComponentName cmp = intent.getComponent();
            Slog.d("TranGriffin/startActivityAsUser2", "caller:" + callingPackage + ",uid=" + callerUid + ",pid=" + callerPid + ",action=" + action + ",pkg=" + pkg + ",cmp=" + cmp);
        }
        ITranActivityTaskManagerService.Instance().startActivityHook(callingPackage, intent);
        if (Process.isSdkSandboxUid(Binder.getCallingUid())) {
            SdkSandboxManagerLocal sdkSandboxManagerLocal = (SdkSandboxManagerLocal) LocalManagerRegistry.getManager(SdkSandboxManagerLocal.class);
            if (sdkSandboxManagerLocal == null) {
                throw new IllegalStateException("SdkSandboxManagerLocal not found when starting an activity from an SDK sandbox uid.");
            }
            sdkSandboxManagerLocal.enforceAllowedToStartActivity(intent);
        }
        Integer ret = ITranWindowManagerService.Instance().onStartActivityAsUser(this.mContext, intent, bOptions, userId);
        if (ret != null) {
            return ret.intValue();
        }
        return getActivityStartController().obtainStarter(intent, "startActivityAsUser").setCaller(caller).setCallingPackage(callingPackage).setCallingFeatureId(callingFeatureId).setResolvedType(resolvedType).setResultTo(resultTo).setResultWho(resultWho).setRequestCode(requestCode).setStartFlags(startFlags).setProfilerInfo(profilerInfo).setActivityOptions(bOptions).setUserId(getActivityStartController().checkTargetUser(userId, validateIncomingUser, Binder.getCallingPid(), Binder.getCallingUid(), "startActivityAsUser")).execute();
    }

    public int startActivityIntentSender(IApplicationThread caller, IIntentSender target, IBinder allowlistToken, Intent fillInIntent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int flagsMask, int flagsValues, Bundle bOptions) {
        enforceNotIsolatedCaller("startActivityIntentSender");
        if (fillInIntent != null && fillInIntent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        if (!(target instanceof PendingIntentRecord)) {
            throw new IllegalArgumentException("Bad PendingIntent object");
        }
        PendingIntentRecord pir = (PendingIntentRecord) target;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task topFocusedRootTask = getTopDisplayFocusedRootTask();
                if (topFocusedRootTask != null && topFocusedRootTask.getTopResumedActivity() != null && topFocusedRootTask.getTopResumedActivity().info.applicationInfo.uid == Binder.getCallingUid()) {
                    this.mAppSwitchesState = 2;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (ITranGriffinFeature.Instance().isGriffinDebugOpen() && ITranGriffinFeature.Instance().isGriffinSupport()) {
            int callerUid = Binder.getCallingUid();
            int callerPid = Binder.getCallingPid();
            String action = fillInIntent != null ? fillInIntent.getAction() : "null";
            String pkg = fillInIntent != null ? fillInIntent.getPackage() : "null";
            ComponentName cmp = fillInIntent != null ? fillInIntent.getComponent() : null;
            Slog.d("TranGriffin/startActivityIntentSender", "caller:uid=" + callerUid + ",pid=" + callerPid + ",action=" + action + ",pkg=" + pkg + ",cmp=" + cmp);
        }
        return pir.sendInner(0, fillInIntent, resolvedType, allowlistToken, null, null, resultTo, resultWho, requestCode, flagsMask, flagsValues, bOptions);
    }

    /* JADX WARN: Code restructure failed: missing block: B:41:0x00a6, code lost:
        r8 = r8 + r14;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x00a7, code lost:
        if (r8 >= r7) goto L81;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00a9, code lost:
        r10 = r0.get(r8).activityInfo;
     */
    /* JADX WARN: Code restructure failed: missing block: B:45:0x00b4, code lost:
        r10 = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00b6, code lost:
        if (r15 == false) goto L46;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x00b8, code lost:
        android.util.Slog.v(com.android.server.wm.ActivityTaskManagerService.TAG, "Next matching activity: found current " + r0.packageName + com.android.server.slice.SliceClientPermissions.SliceAuthority.DELIMITER + r0.info.name);
        r12 = new java.lang.StringBuilder().append("Next matching activity: next is ");
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x00ed, code lost:
        if (r10 != null) goto L76;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00ef, code lost:
        r13 = "null";
     */
    /* JADX WARN: Code restructure failed: missing block: B:50:0x00f3, code lost:
        r13 = r10.packageName + com.android.server.slice.SliceClientPermissions.SliceAuthority.DELIMITER + r10.name;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x010e, code lost:
        android.util.Slog.v(com.android.server.wm.ActivityTaskManagerService.TAG, r12.append(r13).toString());
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x011b, code lost:
        r16 = r10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x011e, code lost:
        r16 = r10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:59:0x012b, code lost:
        r0 = r16;
     */
    /* JADX WARN: Removed duplicated region for block: B:61:0x012f A[Catch: all -> 0x01e3, TRY_ENTER, TryCatch #0 {all -> 0x01e3, blocks: (B:25:0x0043, B:31:0x0065, B:33:0x007d, B:37:0x0086, B:39:0x0098, B:41:0x00a6, B:43:0x00a9, B:47:0x00b8, B:51:0x010e, B:50:0x00f3, B:61:0x012f, B:63:0x0134, B:64:0x013b, B:67:0x0140, B:69:0x0169, B:70:0x016c, B:72:0x0191, B:74:0x0195, B:76:0x01d1, B:79:0x01d7, B:83:0x01de), top: B:88:0x001c }] */
    /* JADX WARN: Removed duplicated region for block: B:67:0x0140 A[Catch: all -> 0x01e3, TRY_ENTER, TryCatch #0 {all -> 0x01e3, blocks: (B:25:0x0043, B:31:0x0065, B:33:0x007d, B:37:0x0086, B:39:0x0098, B:41:0x00a6, B:43:0x00a9, B:47:0x00b8, B:51:0x010e, B:50:0x00f3, B:61:0x012f, B:63:0x0134, B:64:0x013b, B:67:0x0140, B:69:0x0169, B:70:0x016c, B:72:0x0191, B:74:0x0195, B:76:0x01d1, B:79:0x01d7, B:83:0x01de), top: B:88:0x001c }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean startNextMatchingActivity(IBinder callingActivity, Intent intent, Bundle bOptions) {
        ActivityInfo aInfo;
        if (intent != null && intent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        SafeActivityOptions options = SafeActivityOptions.fromBundle(bOptions);
        synchronized (this.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInRootTaskLocked(callingActivity);
                    if (r == null) {
                        SafeActivityOptions.abort(options);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    } else if (r.attachedToProcess()) {
                        Intent intent2 = new Intent(intent);
                        intent2.setDataAndType(r.intent.getData(), r.intent.getType());
                        intent2.setComponent(null);
                        int i = 1;
                        boolean debug = (intent2.getFlags() & 8) != 0;
                        ActivityInfo aInfo2 = null;
                        try {
                            List<ResolveInfo> resolves = AppGlobals.getPackageManager().queryIntentActivities(intent2, r.resolvedType, 66560L, UserHandle.getCallingUserId()).getList();
                            int N = resolves != null ? resolves.size() : 0;
                            int i2 = 0;
                            while (true) {
                                if (i2 >= N) {
                                    break;
                                }
                                ResolveInfo rInfo = resolves.get(i2);
                                if (rInfo.activityInfo.packageName.equals(r.packageName) && rInfo.activityInfo.name.equals(r.info.name)) {
                                    break;
                                }
                                i2++;
                                i = 1;
                            }
                            aInfo = aInfo2;
                        } catch (RemoteException e) {
                        }
                        if (aInfo != null) {
                            SafeActivityOptions.abort(options);
                            if (debug) {
                                Slog.d(TAG, "Next matching activity: nothing found");
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return false;
                        }
                        intent2.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
                        intent2.setFlags(intent2.getFlags() & (-503316481));
                        boolean wasFinishing = r.finishing;
                        r.finishing = true;
                        ActivityRecord resultTo = r.resultTo;
                        String resultWho = r.resultWho;
                        int requestCode = r.requestCode;
                        r.resultTo = null;
                        if (resultTo != null) {
                            resultTo.removeResultsLocked(r, resultWho, requestCode);
                        }
                        long origId = Binder.clearCallingIdentity();
                        int res = getActivityStartController().obtainStarter(intent2, "startNextMatchingActivity").setCaller(r.app.getThread()).setResolvedType(r.resolvedType).setActivityInfo(aInfo).setResultTo(resultTo != null ? resultTo.token : null).setResultWho(resultWho).setRequestCode(requestCode).setCallingPid(-1).setCallingUid(r.launchedFromUid).setCallingPackage(r.launchedFromPackage).setCallingFeatureId(r.launchedFromFeatureId).setRealCallingPid(-1).setRealCallingUid(r.launchedFromUid).setActivityOptions(options).execute();
                        Binder.restoreCallingIdentity(origId);
                        r.finishing = wasFinishing;
                        if (res != 0) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return false;
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return true;
                    } else {
                        SafeActivityOptions.abort(options);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                } catch (Throwable th) {
                    th = th;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDreaming() {
        return this.mDreaming;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canLaunchDreamActivity(String packageName) {
        if (!this.mDreaming || packageName == null) {
            return false;
        }
        DreamManagerInternal dreamManager = (DreamManagerInternal) LocalServices.getService(DreamManagerInternal.class);
        ComponentName activeDream = dreamManager.getActiveDreamComponent(false);
        if (activeDream != null && packageName.equals(activeDream.getPackageName())) {
            return true;
        }
        ComponentName activeDoze = dreamManager.getActiveDreamComponent(true);
        return activeDoze != null && packageName.equals(activeDoze.getPackageName());
    }

    private void enforceCallerIsDream(String callerPackageName) {
        long origId = Binder.clearCallingIdentity();
        try {
            if (!canLaunchDreamActivity(callerPackageName)) {
                throw new SecurityException("The dream activity can be started only when the device is dreaming and only by the active dream package.");
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean startDreamActivity(Intent intent) {
        assertPackageMatchesCallingUid(intent.getPackage());
        enforceCallerIsDream(intent.getPackage());
        ActivityInfo a = new ActivityInfo();
        a.theme = 16974872;
        a.exported = true;
        a.name = DreamActivity.class.getName();
        a.enabled = true;
        a.launchMode = 3;
        a.persistableMode = 1;
        a.screenOrientation = -1;
        a.colorMode = 0;
        a.flags |= 32;
        a.resizeMode = 0;
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchActivityType(5);
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowProcessController process = this.mProcessMap.getProcess(Binder.getCallingPid());
                a.packageName = process.mInfo.packageName;
                a.applicationInfo = process.mInfo;
                a.processName = process.mName;
                a.uiOptions = process.mInfo.uiOptions;
                a.taskAffinity = "android:" + a.packageName + "/dream";
                int callingUid = Binder.getCallingUid();
                int callingPid = Binder.getCallingPid();
                long origId = Binder.clearCallingIdentity();
                getActivityStartController().obtainStarter(intent, "dream").setCallingUid(callingUid).setCallingPid(callingPid).setCallingPackage(intent.getPackage()).setActivityInfo(a).setActivityOptions(options.toBundle()).setRealCallingUid(Binder.getCallingUid()).setAllowBackgroundActivityStart(true).execute();
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return true;
    }

    public final WaitResult startActivityAndWait(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int userId) {
        assertPackageMatchesCallingUid(callingPackage);
        WaitResult res = new WaitResult();
        enforceNotIsolatedCaller("startActivityAndWait");
        if (ITranGriffinFeature.Instance().isGriffinDebugOpen() && ITranGriffinFeature.Instance().isGriffinSupport()) {
            int callerUid = Binder.getCallingUid();
            int callerPid = Binder.getCallingPid();
            String action = intent.getAction();
            String pkg = intent.getPackage();
            ComponentName cmp = intent.getComponent();
            Slog.d("TranGriffin/startActivityAndWait", "caller:" + callingPackage + ",uid=" + callerUid + ",pid=" + callerPid + ",action=" + action + ",pkg=" + pkg + ",cmp=" + cmp);
        }
        ITranActivityTaskManagerService.Instance().startActivityHook(callingPackage, intent);
        getActivityStartController().obtainStarter(intent, "startActivityAndWait").setCaller(caller).setCallingPackage(callingPackage).setCallingFeatureId(callingFeatureId).setResolvedType(resolvedType).setResultTo(resultTo).setResultWho(resultWho).setRequestCode(requestCode).setStartFlags(startFlags).setActivityOptions(bOptions).setUserId(handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, "startActivityAndWait")).setProfilerInfo(profilerInfo).setWaitResult(res).execute();
        return res;
    }

    public final int startActivityWithConfig(IApplicationThread caller, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, Configuration config, Bundle bOptions, int userId) {
        assertPackageMatchesCallingUid(callingPackage);
        enforceNotIsolatedCaller("startActivityWithConfig");
        if (ITranGriffinFeature.Instance().isGriffinDebugOpen() && ITranGriffinFeature.Instance().isGriffinSupport()) {
            int callerUid = Binder.getCallingUid();
            int callerPid = Binder.getCallingPid();
            String action = intent.getAction();
            String pkg = intent.getPackage();
            ComponentName cmp = intent.getComponent();
            Slog.d("TranGriffin/startActivityWithConfig", "caller:" + callingPackage + ",uid=" + callerUid + ",pid=" + callerPid + ",action=" + action + ",pkg=" + pkg + ",cmp=" + cmp);
        }
        int callerUid2 = Binder.getCallingPid();
        return getActivityStartController().obtainStarter(intent, "startActivityWithConfig").setCaller(caller).setCallingPackage(callingPackage).setCallingFeatureId(callingFeatureId).setResolvedType(resolvedType).setResultTo(resultTo).setResultWho(resultWho).setRequestCode(requestCode).setStartFlags(startFlags).setGlobalConfiguration(config).setActivityOptions(bOptions).setUserId(handleIncomingUser(callerUid2, Binder.getCallingUid(), userId, "startActivityWithConfig")).execute();
    }

    /* JADX WARN: Code restructure failed: missing block: B:25:0x0079, code lost:
        if (r20.getComponent() == null) goto L33;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x007f, code lost:
        if (r20.getSelector() != null) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x0089, code lost:
        throw new java.lang.SecurityException("Selector not allowed with ignoreTargetSecurity");
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x0091, code lost:
        throw new java.lang.SecurityException("Component must be specified with ignoreTargetSecurity");
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public final int startActivityAsCaller(IApplicationThread caller, String callingPackage, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, boolean ignoreTargetSecurity, int userId) {
        synchronized (this.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (resultTo != null) {
                        ActivityRecord sourceRecord = ActivityRecord.isInAnyTask(resultTo);
                        if (sourceRecord == null) {
                            throw new SecurityException("Called with bad activity token: " + resultTo);
                        }
                        if (sourceRecord.app != null) {
                            if (checkCallingPermission("android.permission.START_ACTIVITY_AS_CALLER") != 0) {
                                if (!sourceRecord.info.packageName.equals(PackageManagerService.PLATFORM_PACKAGE_NAME)) {
                                    throw new SecurityException("Must be called from an activity that is declared in the android package");
                                }
                                if (UserHandle.getAppId(sourceRecord.app.mUid) != 1000 && sourceRecord.app.mUid != sourceRecord.launchedFromUid) {
                                    throw new SecurityException("Calling activity in uid " + sourceRecord.app.mUid + " must be system uid or original calling uid " + sourceRecord.launchedFromUid);
                                }
                            }
                            int targetUid = sourceRecord.launchedFromUid;
                            String targetPackage = sourceRecord.launchedFromPackage;
                            String targetFeatureId = sourceRecord.launchedFromFeatureId;
                            boolean isResolver = sourceRecord.isResolverOrChildActivity();
                            WindowManagerService.resetPriorityAfterLockedSection();
                            int userId2 = userId;
                            if (userId2 == -10000) {
                                userId2 = UserHandle.getUserId(sourceRecord.app.mUid);
                            }
                            try {
                                try {
                                    try {
                                        try {
                                        } catch (SecurityException e) {
                                            e = e;
                                            throw e;
                                        }
                                    } catch (SecurityException e2) {
                                        e = e2;
                                        throw e;
                                    }
                                } catch (SecurityException e3) {
                                    e = e3;
                                    throw e;
                                }
                            } catch (SecurityException e4) {
                                e = e4;
                            }
                            try {
                                try {
                                    try {
                                        return getActivityStartController().obtainStarter(intent, "startActivityAsCaller").setCallingUid(targetUid).setCallingPackage(targetPackage).setCallingFeatureId(targetFeatureId).setResolvedType(resolvedType).setResultTo(resultTo).setResultWho(resultWho).setRequestCode(requestCode).setStartFlags(startFlags).setActivityOptions(bOptions).setUserId(userId2).setIgnoreTargetSecurity(ignoreTargetSecurity).setFilterCallingUid(isResolver ? 0 : targetUid).setAllowBackgroundActivityStart(true).execute();
                                    } catch (SecurityException e5) {
                                        throw e5;
                                    }
                                } catch (SecurityException e6) {
                                    e = e6;
                                    throw e;
                                }
                            } catch (SecurityException e7) {
                                e = e7;
                                throw e;
                            }
                        }
                        throw new SecurityException("Called without a process attached to activity");
                    }
                    throw new SecurityException("Must be called from an activity");
                } catch (Throwable th) {
                    th = th;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int handleIncomingUser(int callingPid, int callingUid, int userId, String name) {
        return this.mAmInternal.handleIncomingUser(callingPid, callingUid, userId, false, 0, name, (String) null);
    }

    public int startVoiceActivity(String callingPackage, String callingFeatureId, int callingPid, int callingUid, Intent intent, String resolvedType, IVoiceInteractionSession session, IVoiceInteractor interactor, int startFlags, ProfilerInfo profilerInfo, Bundle bOptions, int userId) {
        assertPackageMatchesCallingUid(callingPackage);
        this.mAmInternal.enforceCallingPermission("android.permission.BIND_VOICE_INTERACTION", "startVoiceActivity()");
        if (session == null || interactor == null) {
            throw new NullPointerException("null session or interactor");
        }
        if (ITranGriffinFeature.Instance().isGriffinDebugOpen() && ITranGriffinFeature.Instance().isGriffinSupport()) {
            int callerUid = Binder.getCallingUid();
            int callerPid = Binder.getCallingPid();
            String action = intent.getAction();
            String pkg = intent.getPackage();
            ComponentName cmp = intent.getComponent();
            Slog.d("TranGriffin/startVoiceActivity", "caller:" + callingPackage + ",uid=" + callerUid + ",pid=" + callerPid + ",action=" + action + ",pkg=" + pkg + ",cmp=" + cmp);
        }
        return getActivityStartController().obtainStarter(intent, "startVoiceActivity").setCallingUid(callingUid).setCallingPackage(callingPackage).setCallingFeatureId(callingFeatureId).setResolvedType(resolvedType).setVoiceSession(session).setVoiceInteractor(interactor).setStartFlags(startFlags).setProfilerInfo(profilerInfo).setActivityOptions(bOptions).setUserId(handleIncomingUser(callingPid, callingUid, userId, "startVoiceActivity")).setAllowBackgroundActivityStart(true).execute();
    }

    public String getVoiceInteractorPackageName(IBinder callingVoiceInteractor) {
        return ((VoiceInteractionManagerInternal) LocalServices.getService(VoiceInteractionManagerInternal.class)).getVoiceInteractorPackageName(callingVoiceInteractor);
    }

    public int startAssistantActivity(String callingPackage, String callingFeatureId, int callingPid, int callingUid, Intent intent, String resolvedType, Bundle bOptions, int userId) {
        assertPackageMatchesCallingUid(callingPackage);
        this.mAmInternal.enforceCallingPermission("android.permission.BIND_VOICE_INTERACTION", "startAssistantActivity()");
        if (ITranGriffinFeature.Instance().isGriffinDebugOpen() && ITranGriffinFeature.Instance().isGriffinSupport()) {
            int callerUid = Binder.getCallingUid();
            int callerPid = Binder.getCallingPid();
            String action = intent.getAction();
            String pkg = intent.getPackage();
            ComponentName cmp = intent.getComponent();
            Slog.d("TranGriffin/startAssistantActivity", "caller:" + callingPackage + ",uid=" + callerUid + ",pid=" + callerPid + ",action=" + action + ",pkg=" + pkg + ",cmp=" + cmp);
        }
        int userId2 = handleIncomingUser(callingPid, callingUid, userId, "startAssistantActivity");
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    try {
                        try {
                            try {
                                int execute = getActivityStartController().obtainStarter(intent, "startAssistantActivity").setCallingUid(callingUid).setCallingPackage(callingPackage).setCallingFeatureId(callingFeatureId).setResolvedType(resolvedType).setActivityOptions(bOptions).setUserId(userId2).setAllowBackgroundActivityStart(true).execute();
                                Binder.restoreCallingIdentity(origId);
                                return execute;
                            } catch (Throwable th) {
                                th = th;
                                Binder.restoreCallingIdentity(origId);
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            Binder.restoreCallingIdentity(origId);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        Binder.restoreCallingIdentity(origId);
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    Binder.restoreCallingIdentity(origId);
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
            }
        } catch (Throwable th6) {
            th = th6;
        }
    }

    public void startRecentsActivity(Intent intent, long eventTime, IRecentsAnimationRunner recentsAnimationRunner) {
        WindowManagerGlobalLock windowManagerGlobalLock;
        enforceTaskPermission("startRecentsActivity()");
        if (ITranGriffinFeature.Instance().isGriffinDebugOpen() && ITranGriffinFeature.Instance().isGriffinSupport()) {
            int callerUid = Binder.getCallingUid();
            int callerPid = Binder.getCallingPid();
            String action = intent.getAction();
            String pkg = intent.getPackage();
            ComponentName cmp = intent.getComponent();
            Slog.d("TranGriffin/startRecentsActivity", "caller:uid=" + callerUid + ",pid=" + callerPid + ",action=" + action + ",pkg=" + pkg + ",cmp=" + cmp);
        }
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            WindowManagerGlobalLock windowManagerGlobalLock2 = this.mGlobalLock;
            try {
                try {
                    synchronized (windowManagerGlobalLock2) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            ComponentName recentsComponent = this.mRecentTasks.getRecentsComponent();
                            String recentsFeatureId = this.mRecentTasks.getRecentsComponentFeatureId();
                            int recentsUid = this.mRecentTasks.getRecentsComponentUid();
                            WindowProcessController caller = getProcessController(callingPid, callingUid);
                            windowManagerGlobalLock = windowManagerGlobalLock2;
                            try {
                                RecentsAnimation anim = new RecentsAnimation(this, this.mTaskSupervisor, getActivityStartController(), this.mWindowManager, intent, recentsComponent, recentsFeatureId, recentsUid, caller);
                                if (recentsAnimationRunner == null) {
                                    anim.preloadRecentsActivity();
                                } else {
                                    anim.startRecentsActivity(recentsAnimationRunner, eventTime);
                                }
                                WindowManagerService.resetPriorityAfterLockedSection();
                                Binder.restoreCallingIdentity(origId);
                            } catch (Throwable th) {
                                th = th;
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            windowManagerGlobalLock = windowManagerGlobalLock2;
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

    public final int startActivityFromRecents(int taskId, Bundle bOptions) {
        this.mAmInternal.enforceCallingPermission("android.permission.START_TASKS_FROM_RECENTS", "startActivityFromRecents()");
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        SafeActivityOptions safeOptions = SafeActivityOptions.fromBundle(bOptions);
        long origId = Binder.clearCallingIdentity();
        try {
            return this.mTaskSupervisor.startActivityFromRecents(callingPid, callingUid, taskId, safeOptions, true);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int startActivityFromGameSession(IApplicationThread caller, String callingPackage, String callingFeatureId, int callingPid, int callingUid, Intent intent, int taskId, int userId) {
        if (checkCallingPermission("android.permission.MANAGE_GAME_ACTIVITY") != 0) {
            String msg = "Permission Denial: startActivityFromGameSession() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.MANAGE_GAME_ACTIVITY";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        assertPackageMatchesCallingUid(callingPackage);
        ActivityOptions activityOptions = ActivityOptions.makeBasic();
        activityOptions.setLaunchTaskId(taskId);
        int userId2 = handleIncomingUser(callingPid, callingUid, userId, "startActivityFromGameSession");
        long origId = Binder.clearCallingIdentity();
        try {
            return getActivityStartController().obtainStarter(intent, "startActivityFromGameSession").setCaller(caller).setCallingUid(callingUid).setCallingPid(callingPid).setCallingPackage(intent.getPackage()).setCallingFeatureId(callingFeatureId).setUserId(userId2).setActivityOptions(activityOptions.toBundle()).setRealCallingUid(Binder.getCallingUid()).execute();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public BackNavigationInfo startBackNavigation(boolean requestAnimation) {
        this.mAmInternal.enforceCallingPermission("android.permission.START_TASKS_FROM_RECENTS", "startBackNavigation()");
        BackNavigationController backNavigationController = this.mBackNavigationController;
        if (backNavigationController == null) {
            return null;
        }
        return backNavigationController.startBackNavigation(this.mWindowManager, requestAnimation);
    }

    public final boolean isActivityStartAllowedOnDisplay(int displayId, Intent intent, String resolvedType, int userId) {
        boolean canPlaceEntityOnDisplay;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long origId = Binder.clearCallingIdentity();
        try {
            ActivityInfo aInfo = resolveActivityInfoForIntent(intent, resolvedType, userId, callingUid);
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                canPlaceEntityOnDisplay = this.mTaskSupervisor.canPlaceEntityOnDisplay(displayId, callingPid, callingUid, aInfo);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return canPlaceEntityOnDisplay;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityInfo resolveActivityInfoForIntent(Intent intent, String resolvedType, int userId, int callingUid) {
        ActivityInfo aInfo = this.mTaskSupervisor.resolveActivity(intent, resolvedType, 0, null, userId, ActivityStarter.computeResolveFilterUid(callingUid, callingUid, -10000));
        return this.mAmInternal.getActivityInfoForUser(aInfo, userId);
    }

    public IActivityClientController getActivityClientController() {
        return this.mActivityClientController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyUpdateLockStateLocked(final ActivityRecord r) {
        final boolean nextState = r != null && r.immersive;
        this.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskManagerService.this.m7826xcff07ed5(nextState, r);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyUpdateLockStateLocked$0$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ void m7826xcff07ed5(boolean nextState, ActivityRecord r) {
        if (this.mUpdateLock.isHeld() != nextState) {
            if (ProtoLogCache.WM_DEBUG_IMMERSIVE_enabled) {
                String protoLogParam0 = String.valueOf(nextState);
                String protoLogParam1 = String.valueOf(r);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_IMMERSIVE, 556758086, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
            }
            if (nextState) {
                this.mUpdateLock.acquire();
            } else {
                this.mUpdateLock.release();
            }
        }
    }

    public boolean isTopActivityImmersive() {
        enforceNotIsolatedCaller("isTopActivityImmersive");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task topFocusedRootTask = getTopDisplayFocusedRootTask();
                boolean z = false;
                if (topFocusedRootTask == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                ActivityRecord r = topFocusedRootTask.topRunningActivity();
                if (r != null && r.immersive) {
                    z = true;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return z;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getFrontActivityScreenCompatMode() {
        enforceNotIsolatedCaller("getFrontActivityScreenCompatMode");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task rootTask = getTopDisplayFocusedRootTask();
                ActivityRecord r = rootTask != null ? rootTask.topRunningActivity() : null;
                if (r == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return -3;
                }
                int computeCompatModeLocked = this.mCompatModePackages.computeCompatModeLocked(r.info.applicationInfo);
                WindowManagerService.resetPriorityAfterLockedSection();
                return computeCompatModeLocked;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setFrontActivityScreenCompatMode(int mode) {
        this.mAmInternal.enforceCallingPermission("android.permission.SET_SCREEN_COMPATIBILITY", "setFrontActivityScreenCompatMode");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task rootTask = getTopDisplayFocusedRootTask();
                ActivityRecord r = rootTask != null ? rootTask.topRunningActivity() : null;
                if (r == null) {
                    Slog.w(TAG, "setFrontActivityScreenCompatMode failed: no top activity");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ApplicationInfo ai = r.info.applicationInfo;
                this.mCompatModePackages.setPackageScreenCompatModeLocked(ai, mode);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public ActivityTaskManager.RootTaskInfo getFocusedRootTaskInfo() throws RemoteException {
        enforceTaskPermission("getFocusedRootTaskInfo()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Task focusedRootTask = getTopDisplayFocusedRootTask();
                if (focusedRootTask != null) {
                    ActivityTaskManager.RootTaskInfo rootTaskInfo = this.mRootWindowContainer.getRootTaskInfo(focusedRootTask.mTaskId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return rootTaskInfo;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void setFocusedRootTask(int taskId) {
        enforceTaskPermission("setFocusedRootTask()");
        if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
            long protoLogParam0 = taskId;
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_FOCUS, 255339989, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = this.mRootWindowContainer.getRootTask(taskId);
                if (task == null) {
                    Slog.w(TAG, "setFocusedRootTask: No task with id=" + taskId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ActivityRecord r = task.topRunningActivity();
                if (r != null && r.moveFocusableActivityToTop("setFocusedRootTask")) {
                    this.mRootWindowContainer.resumeFocusedTasksTopActivities();
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void setFocusedTask(int taskId) {
        enforceTaskPermission("setFocusedTask()");
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                setFocusedTask(taskId, null);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFocusedTask(int taskId, ActivityRecord touchedActivity) {
        ActivityRecord r;
        TaskFragment parent;
        if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
            long protoLogParam0 = taskId;
            String protoLogParam1 = String.valueOf(touchedActivity);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_FOCUS, -55185509, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
        }
        Task task = this.mRootWindowContainer.anyTaskForId(taskId, 0);
        if (task == null || (r = task.topRunningActivityLocked()) == null) {
            return;
        }
        if (r.moveFocusableActivityToTop("setFocusedTask")) {
            this.mRootWindowContainer.resumeFocusedTasksTopActivities();
        } else if (touchedActivity != null && touchedActivity.isFocusable() && (parent = touchedActivity.getTaskFragment()) != null && parent.isEmbedded()) {
            DisplayContent displayContent = touchedActivity.getDisplayContent();
            displayContent.setFocusedApp(touchedActivity);
            this.mWindowManager.updateFocusedWindowLocked(0, true);
        }
    }

    public boolean removeTask(int taskId) {
        this.mAmInternal.enforceCallingPermission("android.permission.REMOVE_TASKS", "removeTask()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                Task task = this.mRootWindowContainer.anyTaskForId(taskId, 1);
                if (task == null) {
                    Slog.w(TAG, "removeTask: No task remove with id=" + taskId);
                    Binder.restoreCallingIdentity(ident);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                if (task.isLeafTask()) {
                    this.mTaskSupervisor.removeTask(task, true, true, "remove-task");
                } else {
                    this.mTaskSupervisor.removeRootTask(task);
                }
                Binder.restoreCallingIdentity(ident);
                WindowManagerService.resetPriorityAfterLockedSection();
                return true;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void removeAllVisibleRecentTasks() {
        this.mAmInternal.enforceCallingPermission("android.permission.REMOVE_TASKS", "removeAllVisibleRecentTasks()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                getRecentTasks().removeAllVisibleTasks(this.mAmInternal.getCurrentUserId());
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public Rect getTaskBounds(int taskId) {
        enforceTaskPermission("getTaskBounds()");
        long ident = Binder.clearCallingIdentity();
        Rect rect = new Rect();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = this.mRootWindowContainer.anyTaskForId(taskId, 1);
                if (task == null) {
                    Slog.w(TAG, "getTaskBounds: taskId=" + taskId + " not found");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return rect;
                }
                if (task.getParent() != null) {
                    rect.set(task.getBounds());
                } else if (task.mLastNonFullscreenBounds != null) {
                    rect.set(task.mLastNonFullscreenBounds);
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return rect;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public ActivityManager.TaskDescription getTaskDescription(int id) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                enforceTaskPermission("getTaskDescription()");
                Task tr = this.mRootWindowContainer.anyTaskForId(id, 1);
                if (tr != null) {
                    ActivityManager.TaskDescription taskDescription = tr.getTaskDescription();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return taskDescription;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setLocusId(LocusId locusId, IBinder appToken) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(appToken);
                if (r != null) {
                    r.setLocusId(locusId);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public NeededUriGrants collectGrants(Intent intent, ActivityRecord target) {
        if (target != null) {
            return this.mUgmInternal.checkGrantUriPermissionFromIntent(intent, Binder.getCallingUid(), target.packageName, target.mUserId);
        }
        return null;
    }

    public void unhandledBack() {
        this.mAmInternal.enforceCallingPermission("android.permission.FORCE_BACK", "unhandledBack()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                Task topFocusedRootTask = getTopDisplayFocusedRootTask();
                if (topFocusedRootTask != null) {
                    topFocusedRootTask.unhandledBackLocked();
                }
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void moveTaskToFront(IApplicationThread appThread, String callingPackage, int taskId, int flags, Bundle bOptions) {
        this.mAmInternal.enforceCallingPermission("android.permission.REORDER_TASKS", "moveTaskToFront()");
        if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
            long protoLogParam0 = taskId;
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 2117696413, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                moveTaskToFrontLocked(appThread, callingPackage, taskId, flags, SafeActivityOptions.fromBundle(bOptions));
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2392=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveTaskToFrontLocked(IApplicationThread appThread, String callingPackage, int taskId, int flags, SafeActivityOptions options) {
        WindowProcessController callerApp;
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        assertPackageMatchesCallingUid(callingPackage);
        long origId = Binder.clearCallingIdentity();
        if (appThread != null) {
            WindowProcessController callerApp2 = getProcessController(appThread);
            callerApp = callerApp2;
        } else {
            callerApp = null;
        }
        ActivityStarter starter = getActivityStartController().obtainStarter(null, "moveTaskToFront");
        if (!starter.shouldAbortBackgroundActivityStart(callingUid, callingPid, callingPackage, -1, -1, callerApp, null, false, null, null) || isBackgroundActivityStartsEnabled()) {
            try {
                Task task = this.mRootWindowContainer.anyTaskForId(taskId);
                if (task == null) {
                    if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                        long protoLogParam0 = taskId;
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1474292612, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
                    }
                    SafeActivityOptions.abort(options);
                } else if (getLockTaskController().isLockTaskModeViolation(task)) {
                    Slog.e(TAG, "moveTaskToFront: Attempt to violate Lock Task Mode");
                    SafeActivityOptions.abort(options);
                } else {
                    ActivityOptions realOptions = options != null ? options.getOptions(this.mTaskSupervisor) : null;
                    this.mTaskSupervisor.findTaskToMoveToFront(task, flags, realOptions, "moveTaskToFront", false);
                    ActivityRecord topActivity = task.getTopNonFinishingActivity();
                    if (topActivity != null) {
                        topActivity.showStartingWindow(true);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    private boolean isSameApp(int callingUid, String packageName) {
        if (callingUid != 0 && callingUid != 1000) {
            return this.mPmInternal.isSameApp(packageName, callingUid, UserHandle.getUserId(callingUid));
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void assertPackageMatchesCallingUid(String packageName) {
        int callingUid = Binder.getCallingUid();
        if (isSameApp(callingUid, packageName)) {
            return;
        }
        String msg = "Permission Denial: package=" + packageName + " does not belong to uid=" + callingUid;
        Slog.w(TAG, msg);
        throw new SecurityException(msg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getBalAppSwitchesState() {
        return this.mAppSwitchesState;
    }

    public void registerAnrController(android.app.AnrController controller) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mAnrController.add(controller);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void unregisterAnrController(android.app.AnrController controller) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mAnrController.remove(controller);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public android.app.AnrController getAnrController(ApplicationInfo info) {
        ArrayList<android.app.AnrController> controllers;
        if (info == null || info.packageName == null) {
            return null;
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                controllers = new ArrayList<>(this.mAnrController);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        String packageName = info.packageName;
        int uid = info.uid;
        long maxDelayMs = 0;
        android.app.AnrController controllerWithMaxDelay = null;
        Iterator<android.app.AnrController> it = controllers.iterator();
        while (it.hasNext()) {
            android.app.AnrController controller = it.next();
            long delayMs = controller.getAnrDelayMillis(packageName, uid);
            if (delayMs > 0 && delayMs > maxDelayMs) {
                controllerWithMaxDelay = controller;
                maxDelayMs = delayMs;
            }
        }
        return controllerWithMaxDelay;
    }

    public void setActivityController(IActivityController controller, boolean imAMonkey) {
        this.mAmInternal.enforceCallingPermission("android.permission.SET_ACTIVITY_WATCHER", "setActivityController()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mController = controller;
                this.mControllerIsAMonkey = imAMonkey;
                Watchdog.getInstance().setActivityController(controller);
                if (controller != null && imAMonkey && "1".equals(SystemProperties.get("persist.sys.audio.premonkeycontrl", "0"))) {
                    SystemProperties.set("sys.audio.monkeycontrl", "1");
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean isControllerAMonkey() {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Log.i(TAG, "isControllerAMonkey mController = " + this.mController + ",mControllerIsAMonkey = " + this.mControllerIsAMonkey);
                z = this.mController != null && this.mControllerIsAMonkey;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public List<ActivityManager.RunningTaskInfo> getTasks(int maxNum) {
        return getTasks(maxNum, false, false);
    }

    public List<ActivityManager.RunningTaskInfo> getTasks(int maxNum, boolean filterOnlyVisibleRecents, boolean keepIntentExtra) {
        boolean allowed;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        int flags = (filterOnlyVisibleRecents ? 1 : 0) | (keepIntentExtra ? 8 : 0);
        boolean crossUser = isCrossUserAllowed(callingPid, callingUid);
        int flags2 = (crossUser ? 4 : 0) | flags;
        int[] profileIds = getUserManager().getProfileIds(UserHandle.getUserId(callingUid), true);
        ArraySet<Integer> callingProfileIds = new ArraySet<>();
        for (int i : profileIds) {
            callingProfileIds.add(Integer.valueOf(i));
        }
        ArrayList<ActivityManager.RunningTaskInfo> list = new ArrayList<>();
        synchronized (this.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ActivityTaskManagerDebugConfig.DEBUG_ALL) {
                        Slog.v(TAG, "getTasks: max=" + maxNum);
                    }
                    allowed = isGetTasksAllowed("getTasks", callingPid, callingUid);
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    this.mRootWindowContainer.getRunningTasks(maxNum, list, flags2 | (allowed ? 2 : 0), callingUid, callingProfileIds);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return list;
                } catch (Throwable th2) {
                    th = th2;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public void moveTaskToRootTask(int taskId, int rootTaskId, boolean toTop) {
        enforceTaskPermission("moveTaskToRootTask()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                Task task = this.mRootWindowContainer.anyTaskForId(taskId);
                if (task == null) {
                    Slog.w(TAG, "moveTaskToRootTask: No task for id=" + taskId);
                    Binder.restoreCallingIdentity(ident);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    long protoLogParam0 = taskId;
                    long protoLogParam1 = rootTaskId;
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -677449371, 53, (String) null, new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1), Boolean.valueOf(toTop)});
                }
                Task rootTask = this.mRootWindowContainer.getRootTask(rootTaskId);
                if (rootTask == null) {
                    throw new IllegalStateException("moveTaskToRootTask: No rootTask for rootTaskId=" + rootTaskId);
                }
                if (!rootTask.isActivityTypeStandardOrUndefined()) {
                    throw new IllegalArgumentException("moveTaskToRootTask: Attempt to move task " + taskId + " to rootTask " + rootTaskId);
                }
                task.reparent(rootTask, toTop, 1, true, false, "moveTaskToRootTask");
                Binder.restoreCallingIdentity(ident);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void removeRootTasksInWindowingModes(int[] windowingModes) {
        enforceTaskPermission("removeRootTasksInWindowingModes()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                this.mRootWindowContainer.removeRootTasksInWindowingModes(windowingModes);
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void removeRootTasksWithActivityTypes(int[] activityTypes) {
        enforceTaskPermission("removeRootTasksWithActivityTypes()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                this.mRootWindowContainer.removeRootTasksWithActivityTypes(activityTypes);
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags, int userId) {
        ParceledListSlice<ActivityManager.RecentTaskInfo> recentTasks;
        int callingUid = Binder.getCallingUid();
        int userId2 = handleIncomingUser(Binder.getCallingPid(), callingUid, userId, "getRecentTasks");
        boolean allowed = isGetTasksAllowed("getRecentTasks", Binder.getCallingPid(), callingUid);
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                recentTasks = this.mRecentTasks.getRecentTasks(maxNum, flags, allowed, userId2, callingUid);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return recentTasks;
    }

    public List<ActivityTaskManager.RootTaskInfo> getAllRootTaskInfos() {
        ArrayList<ActivityTaskManager.RootTaskInfo> allRootTaskInfos;
        enforceTaskPermission("getAllRootTaskInfos()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                allRootTaskInfos = this.mRootWindowContainer.getAllRootTaskInfos(-1);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return allRootTaskInfos;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public ActivityTaskManager.RootTaskInfo getRootTaskInfo(int windowingMode, int activityType) {
        ActivityTaskManager.RootTaskInfo rootTaskInfo;
        enforceTaskPermission("getRootTaskInfo()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                rootTaskInfo = this.mRootWindowContainer.getRootTaskInfo(windowingMode, activityType);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return rootTaskInfo;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public List<ActivityTaskManager.RootTaskInfo> getAllRootTaskInfosOnDisplay(int displayId) {
        ArrayList<ActivityTaskManager.RootTaskInfo> allRootTaskInfos;
        enforceTaskPermission("getAllRootTaskInfosOnDisplay()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                allRootTaskInfos = this.mRootWindowContainer.getAllRootTaskInfos(displayId);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return allRootTaskInfos;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public ActivityTaskManager.RootTaskInfo getRootTaskInfoOnDisplay(int windowingMode, int activityType, int displayId) {
        ActivityTaskManager.RootTaskInfo rootTaskInfo;
        enforceTaskPermission("getRootTaskInfoOnDisplay()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                rootTaskInfo = this.mRootWindowContainer.getRootTaskInfo(windowingMode, activityType, displayId);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return rootTaskInfo;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void cancelRecentsAnimation(boolean restoreHomeRootTaskPosition) {
        int i;
        enforceTaskPermission("cancelRecentsAnimation()");
        long callingUid = Binder.getCallingUid();
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                WindowManagerService windowManagerService = this.mWindowManager;
                if (restoreHomeRootTaskPosition) {
                    i = 2;
                } else {
                    i = 0;
                }
                windowManagerService.cancelRecentsAnimation(i, "cancelRecentsAnimation/uid=" + callingUid);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void startSystemLockTaskMode(int taskId) {
        enforceTaskPermission("startSystemLockTaskMode");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = this.mRootWindowContainer.anyTaskForId(taskId, 0);
                if (task != null) {
                    task.getRootTask().moveToFront("startSystemLockTaskMode");
                    startLockTaskMode(task, true);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void stopSystemLockTaskMode() throws RemoteException {
        enforceTaskPermission("stopSystemLockTaskMode");
        stopLockTaskModeInternal(null, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startLockTaskMode(Task task, boolean isSystemCaller) {
        if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
            String protoLogParam0 = String.valueOf(task);
            ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_LOCKTASK, 295861935, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (task == null || task.mLockTaskAuth == 0) {
            return;
        }
        Task rootTask = this.mRootWindowContainer.getTopDisplayFocusedRootTask();
        if (rootTask == null || task != rootTask.getTopMostTask()) {
            throw new IllegalArgumentException("Invalid task, not in foreground");
        }
        int callingUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            this.mRootWindowContainer.removeRootTasksInWindowingModes(2);
            getLockTaskController().startLockTaskMode(task, isSystemCaller, callingUid);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopLockTaskModeInternal(IBinder token, boolean isSystemCaller) {
        int callingUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = null;
                if (token != null) {
                    ActivityRecord r = ActivityRecord.forTokenLocked(token);
                    if (r != null) {
                        task = r.getTask();
                    } else {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                }
                getLockTaskController().stopLockTaskMode(task, isSystemCaller, callingUid);
                WindowManagerService.resetPriorityAfterLockedSection();
                TelecomManager tm = (TelecomManager) this.mContext.getSystemService("telecom");
                if (tm != null) {
                    tm.showInCallScreen(false);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void updateLockTaskPackages(int userId, String[] packages) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 1000) {
            this.mAmInternal.enforceCallingPermission("android.permission.UPDATE_LOCK_TASK_PACKAGES", "updateLockTaskPackages()");
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
                    long protoLogParam0 = userId;
                    String protoLogParam1 = String.valueOf(Arrays.toString(packages));
                    ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_LOCKTASK, 715749922, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
                }
                getLockTaskController().updateLockTaskPackages(userId, packages);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean isInLockTaskMode() {
        return getLockTaskModeState() != 0;
    }

    public int getLockTaskModeState() {
        return getLockTaskController().getLockTaskModeState();
    }

    public List<IBinder> getAppTasks(String callingPackage) {
        assertPackageMatchesCallingUid(callingPackage);
        return getAppTasks(callingPackage, Binder.getCallingUid());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public List<IBinder> getAppTasks(String pkgName, int uid) {
        ArrayList<IBinder> appTasksList;
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                appTasksList = this.mRecentTasks.getAppTasksList(uid, pkgName);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return appTasksList;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public IBinder getAppTaskByTaskId(int taskId) {
        IBinder appTaskByTaskId;
        int callingUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                appTaskByTaskId = this.mRecentTasks.getAppTaskByTaskId(callingUid, taskId);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return appTaskByTaskId;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void finishVoiceTask(IVoiceInteractionSession session) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                this.mRootWindowContainer.finishVoiceTask(session);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void reportAssistContextExtras(IBinder assistToken, Bundle extras, AssistStructure structure, AssistContent content, Uri referrer) {
        PendingAssistExtras pae = (PendingAssistExtras) assistToken;
        synchronized (pae) {
            pae.result = extras;
            pae.structure = structure;
            pae.content = content;
            if (referrer != null) {
                pae.extras.putParcelable("android.intent.extra.REFERRER", referrer);
            }
            if (pae.activity.isAttached()) {
                if (structure != null) {
                    structure.setTaskId(pae.activity.getTask().mTaskId);
                    structure.setActivityComponent(pae.activity.mActivityComponent);
                    structure.setHomeActivity(pae.isHome);
                }
                pae.haveResult = true;
                pae.notifyAll();
                if (pae.intent == null && pae.receiver == null) {
                    return;
                }
                Bundle sendBundle = null;
                synchronized (this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        buildAssistBundleLocked(pae, extras);
                        boolean exists = this.mPendingAssistExtras.remove(pae);
                        this.mUiHandler.removeCallbacks(pae);
                        if (!exists) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        IAssistDataReceiver sendReceiver = pae.receiver;
                        if (sendReceiver != null) {
                            sendBundle = new Bundle();
                            sendBundle.putInt(ActivityTaskManagerInternal.ASSIST_TASK_ID, pae.activity.getTask().mTaskId);
                            sendBundle.putBinder(ActivityTaskManagerInternal.ASSIST_ACTIVITY_ID, pae.activity.assistToken);
                            sendBundle.putBundle("data", pae.extras);
                            sendBundle.putParcelable(ActivityTaskManagerInternal.ASSIST_KEY_STRUCTURE, pae.structure);
                            sendBundle.putParcelable(ActivityTaskManagerInternal.ASSIST_KEY_CONTENT, pae.content);
                            sendBundle.putBundle(ActivityTaskManagerInternal.ASSIST_KEY_RECEIVER_EXTRAS, pae.receiverExtras);
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        if (sendReceiver != null) {
                            try {
                                sendReceiver.onHandleAssistData(sendBundle);
                                return;
                            } catch (RemoteException e) {
                                return;
                            }
                        }
                        long ident = Binder.clearCallingIdentity();
                        try {
                            pae.intent.replaceExtras(pae.extras);
                            pae.intent.setFlags(872415232);
                            this.mInternal.closeSystemDialogs(PhoneWindowManager.SYSTEM_DIALOG_REASON_ASSIST);
                            try {
                                this.mContext.startActivityAsUser(pae.intent, new UserHandle(pae.userHandle));
                            } catch (ActivityNotFoundException e2) {
                                Slog.w(TAG, "No activity to handle assist action.", e2);
                            }
                        } finally {
                            Binder.restoreCallingIdentity(ident);
                        }
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2997=4] */
    public int addAppTask(IBinder activityToken, Intent intent, ActivityManager.TaskDescription description, Bitmap thumbnail) throws RemoteException {
        int callingUid = Binder.getCallingUid();
        long callingIdent = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(activityToken);
                if (r != null) {
                    ComponentName comp = intent.getComponent();
                    if (comp != null) {
                        if (thumbnail.getWidth() == this.mThumbnailWidth && thumbnail.getHeight() == this.mThumbnailHeight) {
                            if (intent.getSelector() != null) {
                                intent.setSelector(null);
                            }
                            if (intent.getSourceBounds() != null) {
                                intent.setSourceBounds(null);
                            }
                            if ((intent.getFlags() & 524288) != 0 && (intent.getFlags() & 8192) == 0) {
                                intent.addFlags(8192);
                            }
                            ActivityInfo ainfo = AppGlobals.getPackageManager().getActivityInfo(comp, (long) GadgetFunction.NCM, UserHandle.getUserId(callingUid));
                            if (ainfo != null && ainfo.applicationInfo.uid == callingUid) {
                                Task rootTask = r.getRootTask();
                                Task task = new Task.Builder(this).setWindowingMode(rootTask.getWindowingMode()).setActivityType(rootTask.getActivityType()).setActivityInfo(ainfo).setIntent(intent).setTaskId(rootTask.getDisplayArea().getNextRootTaskId()).build();
                                if (!this.mRecentTasks.addToBottom(task)) {
                                    rootTask.removeChild(task, "addAppTask");
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return -1;
                                }
                                task.getTaskDescription().copyFrom(description);
                                int i = task.mTaskId;
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return i;
                            }
                            Slog.e(TAG, "Can't add task for another application: target uid=" + (ainfo == null ? -1 : ainfo.applicationInfo.uid) + ", calling uid=" + callingUid);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return -1;
                        }
                        throw new IllegalArgumentException("Bad thumbnail size: got " + thumbnail.getWidth() + "x" + thumbnail.getHeight() + ", require " + this.mThumbnailWidth + "x" + this.mThumbnailHeight);
                    }
                    throw new IllegalArgumentException("Intent " + intent + " must specify explicit component");
                }
                throw new IllegalArgumentException("Activity does not exist; token=" + activityToken);
            }
        } finally {
            Binder.restoreCallingIdentity(callingIdent);
        }
    }

    public Point getAppTaskThumbnailSize() {
        Point point;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                point = new Point(this.mThumbnailWidth, this.mThumbnailHeight);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return point;
    }

    public void setTaskResizeable(int taskId, int resizeableMode) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = this.mRootWindowContainer.anyTaskForId(taskId, 1);
                if (task == null) {
                    Slog.w(TAG, "setTaskResizeable: taskId=" + taskId + " not found");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                task.setResizeMode(resizeableMode);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [3046=4] */
    public boolean resizeTask(int taskId, Rect bounds, int resizeMode) {
        enforceTaskPermission("resizeTask()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = this.mRootWindowContainer.anyTaskForId(taskId, 0);
                if (task == null) {
                    Slog.w(TAG, "resizeTask: taskId=" + taskId + " not found");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (!task.getWindowConfiguration().canResizeTask()) {
                    Slog.w(TAG, "resizeTask not allowed on task=" + task);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else {
                    boolean preserveWindow = (resizeMode & 1) != 0;
                    boolean resize = task.resize(bounds, resizeMode, preserveWindow);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return resize;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void releaseSomeActivities(IApplicationThread appInt) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                WindowProcessController app = getProcessController(appInt);
                app.releaseSomeActivities("low-mem");
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void notifyKeyguardGoingAwayQuickly(final boolean goingAwayQuickly) {
        if (checkCallingPermission("android.permission.DEVICE_POWER") != 0) {
            throw new SecurityException("Requires permission android.permission.DEVICE_POWER");
        }
        if (!FINGERPRINT_WAKEUP_PERFORMANCE_OPT) {
            Slog.d(TAG, "notifyKeyguardGoingAwayQuickly: not support FINGERPRINT_WAKEUP_PERFORMANCE_OPT");
            return;
        }
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        final boolean isScreenOn = pm.isScreenOn();
        final int callingPid = Binder.getCallingPid();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                if (this.mNotAllowKeyguardGoingAwayQuickly && goingAwayQuickly) {
                    Slog.d(TAG, "Bouncer showing not allow KeyguardGoingAwayQuickly");
                    Binder.restoreCallingIdentity(ident);
                }
                this.mRootWindowContainer.forAllDisplays(new Consumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda7
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ActivityTaskManagerService.this.m7831x8909292c(goingAwayQuickly, isScreenOn, callingPid, (DisplayContent) obj);
                    }
                });
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyKeyguardGoingAwayQuickly$1$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ void m7831x8909292c(boolean goingAwayQuickly, boolean isScreenOn, int callingPid, DisplayContent displayContent) {
        this.mKeyguardController.notifyKeyguardGoingAwayQuickly(goingAwayQuickly, isScreenOn, callingPid, displayContent.getDisplayId());
    }

    public boolean isKeyguardLocking() {
        boolean isKeyguardLocked;
        if (checkCallingPermission("android.permission.DEVICE_POWER") != 0) {
            throw new SecurityException("Requires permission android.permission.DEVICE_POWER");
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                isKeyguardLocked = this.mKeyguardController.isKeyguardLocked();
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return isKeyguardLocked;
    }

    public void notifyAuthenticateSucceed(boolean isSucceed) {
        if (checkCallingPermission("android.permission.DEVICE_POWER") != 0) {
            throw new SecurityException("Requires permission android.permission.DEVICE_POWER");
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                this.mKeyguardController.notifyAuthenticateSucceed(isSucceed);
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void notAllowKeyguardGoingAwayQuickly(boolean notAllow) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mNotAllowKeyguardGoingAwayQuickly != notAllow) {
                    this.mNotAllowKeyguardGoingAwayQuickly = notAllow;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setLockScreenShown(final boolean keyguardShowing, final boolean aodShowing) {
        if (checkCallingPermission("android.permission.DEVICE_POWER") != 0) {
            throw new SecurityException("Requires permission android.permission.DEVICE_POWER");
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                if (this.mKeyguardShown != keyguardShowing) {
                    this.mKeyguardShown = keyguardShowing;
                    Message msg = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda18
                        @Override // java.util.function.BiConsumer
                        public final void accept(Object obj, Object obj2) {
                            ((ActivityManagerInternal) obj).reportCurKeyguardUsageEvent(((Boolean) obj2).booleanValue());
                        }
                    }, this.mAmInternal, Boolean.valueOf(keyguardShowing));
                    this.mH.sendMessage(msg);
                }
                ITranWindowManagerService.Instance().finishCurrentActivityKeepAwake(keyguardShowing);
                this.mRootWindowContainer.forAllDisplays(new Consumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda19
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ActivityTaskManagerService.this.m7835x97f21094(keyguardShowing, aodShowing, (DisplayContent) obj);
                    }
                });
                Binder.restoreCallingIdentity(ident);
                if (!mIsLucidDisabled && (keyguardShowing || aodShowing)) {
                    setLucidFgApp("lucid.lockscreen");
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        this.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskManagerService.this.m7836x899bb6b3(keyguardShowing);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setLockScreenShown$2$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ void m7835x97f21094(boolean keyguardShowing, boolean aodShowing, DisplayContent displayContent) {
        this.mKeyguardController.setKeyguardShown(displayContent.getDisplayId(), keyguardShowing, aodShowing);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setLockScreenShown$3$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ void m7836x899bb6b3(boolean keyguardShowing) {
        for (int i = this.mScreenObservers.size() - 1; i >= 0; i--) {
            this.mScreenObservers.get(i).onKeyguardStateChanged(keyguardShowing);
        }
    }

    public void onScreenAwakeChanged(final boolean isAwake) {
        WindowProcessController proc;
        if (isAwake) {
            ITranWindowManagerService.Instance().finishCurrentActivityKeepAwake(isKeyguardLocked(0));
        }
        this.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskManagerService.this.m7832x34e9c9b0(isAwake);
            }
        });
        if (isAwake) {
            return;
        }
        synchronized (this.mGlobalLockWithoutBoost) {
            WindowState notificationShade = this.mRootWindowContainer.getDefaultDisplay().getDisplayPolicy().getNotificationShade();
            proc = notificationShade != null ? this.mProcessMap.getProcess(notificationShade.mSession.mPid) : null;
        }
        if (proc == null) {
            return;
        }
        proc.setRunningAnimationUnsafe();
        this.mH.removeMessages(2, proc);
        H h = this.mH;
        h.sendMessageDelayed(h.obtainMessage(2, proc), DOZE_ANIMATING_STATE_RETAIN_TIME_MS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onScreenAwakeChanged$4$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ void m7832x34e9c9b0(boolean isAwake) {
        for (int i = this.mScreenObservers.size() - 1; i >= 0; i--) {
            this.mScreenObservers.get(i).onAwakeStateChanged(isAwake);
        }
    }

    public Bitmap getTaskDescriptionIcon(String filePath, int userId) {
        int userId2 = handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTaskDescriptionIcon");
        File passedIconFile = new File(filePath);
        File legitIconFile = new File(TaskPersister.getUserImagesDir(userId2), passedIconFile.getName());
        if (!legitIconFile.getPath().equals(filePath) || !filePath.contains("_activity_icon_")) {
            throw new IllegalArgumentException("Bad file path: " + filePath + " passed for userId " + userId2);
        }
        return this.mRecentTasks.getTaskDescriptionIcon(filePath);
    }

    public void moveRootTaskToDisplay(int taskId, int displayId) {
        this.mAmInternal.enforceCallingPermission("android.permission.INTERNAL_SYSTEM_WINDOW", "moveRootTaskToDisplay()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long ident = Binder.clearCallingIdentity();
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    long protoLogParam0 = taskId;
                    long protoLogParam1 = displayId;
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1419762046, 5, (String) null, new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1)});
                }
                this.mRootWindowContainer.moveRootTaskToDisplay(taskId, displayId, true);
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void registerTaskStackListener(ITaskStackListener listener) {
        enforceTaskPermission("registerTaskStackListener()");
        this.mTaskChangeNotificationController.registerTaskStackListener(listener);
    }

    public void unregisterTaskStackListener(ITaskStackListener listener) {
        enforceTaskPermission("unregisterTaskStackListener()");
        this.mTaskChangeNotificationController.unregisterTaskStackListener(listener);
    }

    public boolean requestAssistContextExtras(int requestType, IAssistDataReceiver receiver, Bundle receiverExtras, IBinder activityToken, boolean checkActivityIsTop, boolean newSessionId) {
        return enqueueAssistContext(requestType, null, null, receiver, receiverExtras, activityToken, checkActivityIsTop, newSessionId, UserHandle.getCallingUserId(), null, DOZE_ANIMATING_STATE_RETAIN_TIME_MS, 0) != null;
    }

    public boolean requestAssistDataForTask(IAssistDataReceiver receiver, int taskId, String callingPackageName) {
        this.mAmInternal.enforceCallingPermission("android.permission.GET_TOP_ACTIVITY_INFO", "requestAssistDataForTask()");
        long callingId = Binder.clearCallingIdentity();
        try {
            ActivityTaskManagerInternal.ActivityTokens tokens = this.mInternal.getAttachedNonFinishingActivityForTask(taskId, null);
            if (tokens == null) {
                Log.e(TAG, "Could not find activity for task " + taskId);
                return false;
            }
            AssistDataReceiverProxy proxy = new AssistDataReceiverProxy(receiver, callingPackageName);
            Object lock = new Object();
            AssistDataRequester requester = new AssistDataRequester(this.mContext, this.mWindowManager, getAppOpsManager(), proxy, lock, 49, -1);
            List<IBinder> topActivityToken = new ArrayList<>();
            topActivityToken.add(tokens.getActivityToken());
            requester.requestAssistData(topActivityToken, true, false, false, true, false, true, Binder.getCallingUid(), callingPackageName);
            return true;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public boolean requestAutofillData(IAssistDataReceiver receiver, Bundle receiverExtras, IBinder activityToken, int flags) {
        return enqueueAssistContext(2, null, null, receiver, receiverExtras, activityToken, true, true, UserHandle.getCallingUserId(), null, DOZE_ANIMATING_STATE_RETAIN_TIME_MS, flags) != null;
    }

    public Bundle getAssistContextExtras(int requestType) {
        PendingAssistExtras pae = enqueueAssistContext(requestType, null, null, null, null, null, true, true, UserHandle.getCallingUserId(), null, 500L, 0);
        if (pae == null) {
            return null;
        }
        synchronized (pae) {
            while (!pae.haveResult) {
                try {
                    pae.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                buildAssistBundleLocked(pae, pae.result);
                this.mPendingAssistExtras.remove(pae);
                this.mUiHandler.removeCallbacks(pae);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return pae.extras;
    }

    private static int checkCallingPermission(String permission) {
        return checkPermission(permission, Binder.getCallingPid(), Binder.getCallingUid());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkCanCloseSystemDialogs(int pid, int uid, String packageName) {
        WindowProcessController process;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                process = this.mProcessMap.getProcess(pid);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (packageName == null && process != null) {
            packageName = process.mInfo.packageName;
        }
        String caller = "(pid=" + pid + ", uid=" + uid + ")";
        if (packageName != null) {
            caller = packageName + " " + caller;
        }
        if (canCloseSystemDialogs(pid, uid)) {
            return true;
        }
        if (CompatChanges.isChangeEnabled(174664365L, uid)) {
            throw new SecurityException("Permission Denial: android.intent.action.CLOSE_SYSTEM_DIALOGS broadcast from " + caller + " requires android.permission.BROADCAST_CLOSE_SYSTEM_DIALOGS.");
        }
        if (CompatChanges.isChangeEnabled(174664120L, uid)) {
            Slog.e(TAG, "Permission Denial: android.intent.action.CLOSE_SYSTEM_DIALOGS broadcast from " + caller + " requires android.permission.BROADCAST_CLOSE_SYSTEM_DIALOGS, dropping broadcast.");
            return false;
        }
        Slog.w(TAG, "android.intent.action.CLOSE_SYSTEM_DIALOGS broadcast from " + caller + " will require android.permission.BROADCAST_CLOSE_SYSTEM_DIALOGS in future builds.");
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean canCloseSystemDialogs(int pid, int uid) {
        if (checkPermission("android.permission.BROADCAST_CLOSE_SYSTEM_DIALOGS", pid, uid) == 0) {
            return true;
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ArraySet<WindowProcessController> processes = this.mProcessMap.getProcesses(uid);
                if (processes != null) {
                    int n = processes.size();
                    for (int i = 0; i < n; i++) {
                        WindowProcessController process = processes.valueAt(i);
                        int sourceUid = process.getInstrumentationSourceUid();
                        if (process.isInstrumenting() && sourceUid != -1 && checkPermission("android.permission.BROADCAST_CLOSE_SYSTEM_DIALOGS", -1, sourceUid) == 0) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return true;
                        } else if (process.canCloseSystemDialogsByToken()) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return true;
                        }
                    }
                }
                if (!CompatChanges.isChangeEnabled(174664365L, uid)) {
                    if (this.mRootWindowContainer.hasVisibleWindowAboveButDoesNotOwnNotificationShade(uid)) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return true;
                    } else if (ArrayUtils.contains(this.mAccessibilityServiceUids, uid)) {
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

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enforceTaskPermission(String func) {
        if (checkCallingPermission("android.permission.MANAGE_ACTIVITY_TASKS") == 0) {
            return;
        }
        if (checkCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS") == 0) {
            Slog.w(TAG, "MANAGE_ACTIVITY_STACKS is deprecated, please use alternative permission: MANAGE_ACTIVITY_TASKS");
            return;
        }
        String msg = "Permission Denial: " + func + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.MANAGE_ACTIVITY_TASKS";
        Slog.w(TAG, msg);
        throw new SecurityException(msg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int checkPermission(String permission, int pid, int uid) {
        if (permission == null) {
            return -1;
        }
        return checkComponentPermission(permission, pid, uid, -1, true);
    }

    public static int checkComponentPermission(String permission, int pid, int uid, int owningUid, boolean exported) {
        return ActivityManagerService.checkComponentPermission(permission, pid, uid, owningUid, exported);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCallerRecents(int callingUid) {
        return this.mRecentTasks.isCallerRecents(callingUid);
    }

    boolean isGetTasksAllowed(String caller, int callingPid, int callingUid) {
        if (isCallerRecents(callingUid)) {
            return true;
        }
        boolean allowed = checkPermission("android.permission.REAL_GET_TASKS", callingPid, callingUid) == 0;
        if (!allowed) {
            if (checkPermission("android.permission.GET_TASKS", callingPid, callingUid) == 0) {
                try {
                    if (AppGlobals.getPackageManager().isUidPrivileged(callingUid)) {
                        allowed = true;
                        if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                            String protoLogParam0 = String.valueOf(caller);
                            long protoLogParam1 = callingUid;
                            ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_TASKS, -917215012, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
                        }
                    }
                } catch (RemoteException e) {
                }
            }
            if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                String protoLogParam02 = String.valueOf(caller);
                long protoLogParam12 = callingUid;
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_TASKS, -401029526, 4, (String) null, new Object[]{protoLogParam02, Long.valueOf(protoLogParam12)});
            }
        }
        return allowed;
    }

    boolean isCrossUserAllowed(int pid, int uid) {
        return checkPermission("android.permission.INTERACT_ACROSS_USERS", pid, uid) == 0 || checkPermission("android.permission.INTERACT_ACROSS_USERS_FULL", pid, uid) == 0;
    }

    private PendingAssistExtras enqueueAssistContext(int requestType, Intent intent, String hint, IAssistDataReceiver receiver, Bundle receiverExtras, IBinder activityToken, boolean checkActivityIsTop, boolean newSessionId, int userHandle, Bundle args, long timeout, int flags) {
        ActivityRecord activity;
        ActivityRecord caller;
        this.mAmInternal.enforceCallingPermission("android.permission.GET_TOP_ACTIVITY_INFO", "enqueueAssistContext()");
        synchronized (this.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    Task rootTask = getTopDisplayFocusedRootTask();
                    ActivityRecord activity2 = rootTask != null ? rootTask.getTopNonFinishingActivity() : null;
                    if (activity2 == null) {
                        Slog.w(TAG, "getAssistContextExtras failed: no top activity");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    } else if (!activity2.attachedToProcess()) {
                        Slog.w(TAG, "getAssistContextExtras failed: no process for " + activity2);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    } else {
                        if (checkActivityIsTop) {
                            if (activityToken != null && activity2 != (caller = ActivityRecord.forTokenLocked(activityToken))) {
                                Slog.w(TAG, "enqueueAssistContext failed: caller " + caller + " is not current top " + activity2);
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return null;
                            }
                            activity = activity2;
                        } else {
                            ActivityRecord activity3 = ActivityRecord.forTokenLocked(activityToken);
                            if (activity3 == null) {
                                Slog.w(TAG, "enqueueAssistContext failed: activity for token=" + activityToken + " couldn't be found");
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return null;
                            } else if (activity3.attachedToProcess()) {
                                activity = activity3;
                            } else {
                                Slog.w(TAG, "enqueueAssistContext failed: no process for " + activity3);
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return null;
                            }
                        }
                        Bundle extras = new Bundle();
                        if (args != null) {
                            extras.putAll(args);
                        }
                        extras.putString("android.intent.extra.ASSIST_PACKAGE", activity.packageName);
                        extras.putInt("android.intent.extra.ASSIST_UID", activity.app.mUid);
                        ActivityRecord activity4 = activity;
                        PendingAssistExtras pae = new PendingAssistExtras(activity, extras, intent, hint, receiver, receiverExtras, userHandle);
                        pae.isHome = activity4.isActivityTypeHome();
                        if (newSessionId) {
                            this.mViSessionId++;
                        }
                        try {
                            activity4.app.getThread().requestAssistContextExtras(activity4.token, pae, requestType, this.mViSessionId, flags);
                            this.mPendingAssistExtras.add(pae);
                            try {
                                this.mUiHandler.postDelayed(pae, timeout);
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return pae;
                            } catch (RemoteException e) {
                                Slog.w(TAG, "getAssistContextExtras failed: crash calling " + activity4);
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return null;
                            }
                        } catch (RemoteException e2) {
                        }
                    }
                } catch (Throwable th) {
                    e = th;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw e;
                }
            } catch (Throwable th2) {
                e = th2;
            }
        }
    }

    private void buildAssistBundleLocked(PendingAssistExtras pae, Bundle result) {
        if (result != null) {
            pae.extras.putBundle("android.intent.extra.ASSIST_CONTEXT", result);
        }
        if (pae.hint != null) {
            pae.extras.putBoolean(pae.hint, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pendingAssistExtrasTimedOut(PendingAssistExtras pae) {
        IAssistDataReceiver receiver;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mPendingAssistExtras.remove(pae);
                receiver = pae.receiver;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (receiver != null) {
            Bundle sendBundle = new Bundle();
            sendBundle.putBundle(ActivityTaskManagerInternal.ASSIST_KEY_RECEIVER_EXTRAS, pae.receiverExtras);
            try {
                pae.receiver.onHandleAssistData(sendBundle);
            } catch (RemoteException e) {
            }
        }
    }

    public ComponentName getTopActivityComponent() {
        ActivityRecord topRecord;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task rootTask = getTopDisplayFocusedRootTask();
                if (rootTask != null && (topRecord = rootTask.topRunningActivity()) != null) {
                    ComponentName componentName = topRecord.mActivityComponent;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return componentName;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private String getProcessName(int uid, int pid) {
        try {
            List<ActivityManager.RunningAppProcessInfo> procs = ActivityManager.getService().getRunningAppProcesses();
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                ActivityManager.RunningAppProcessInfo proc = procs.get(i);
                if (proc.pid == pid && proc.uid == uid) {
                    return proc.processName;
                }
            }
            return null;
        } catch (RemoteException e) {
            Slog.v(TAG, "am.getRunningAppProcesses() failed");
            return null;
        }
    }

    public void setThreadScheduler(int tid, int policy, int priority) {
        String processName = getProcessName(Binder.getCallingUid(), Binder.getCallingPid());
        String[] strArr = this.threadSchedulerWhilelist;
        if (strArr != null && Arrays.asList(strArr).contains(processName)) {
            Slog.v(TAG, "setThreadScheduler + tid  = " + tid);
            try {
                Process.setThreadScheduler(tid, policy, priority);
                return;
            } catch (IllegalArgumentException e) {
                Slog.v(TAG, "setThreadScheduler e = " + e.toString());
                return;
            }
        }
        throw new SecurityException("No permission call...");
    }

    /* loaded from: classes2.dex */
    public class PendingAssistExtras extends Binder implements Runnable {
        public final ActivityRecord activity;
        public final Bundle extras;
        public final String hint;
        public final Intent intent;
        public boolean isHome;
        public final IAssistDataReceiver receiver;
        public Bundle receiverExtras;
        public final int userHandle;
        public boolean haveResult = false;
        public Bundle result = null;
        public AssistStructure structure = null;
        public AssistContent content = null;

        public PendingAssistExtras(ActivityRecord _activity, Bundle _extras, Intent _intent, String _hint, IAssistDataReceiver _receiver, Bundle _receiverExtras, int _userHandle) {
            this.activity = _activity;
            this.extras = _extras;
            this.intent = _intent;
            this.hint = _hint;
            this.receiver = _receiver;
            this.receiverExtras = _receiverExtras;
            this.userHandle = _userHandle;
        }

        @Override // java.lang.Runnable
        public void run() {
            Slog.w(ActivityTaskManagerService.TAG, "getAssistContextExtras failed: timeout retrieving from " + this.activity);
            synchronized (this) {
                this.haveResult = true;
                notifyAll();
            }
            ActivityTaskManagerService.this.pendingAssistExtrasTimedOut(this);
        }
    }

    public boolean isAssistDataAllowedOnCurrentActivity() {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task focusedRootTask = getTopDisplayFocusedRootTask();
                if (focusedRootTask != null && !focusedRootTask.isActivityTypeAssistant()) {
                    ActivityRecord activity = focusedRootTask.getTopNonFinishingActivity();
                    if (activity == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    int userId = activity.mUserId;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return DevicePolicyCache.getInstance().isScreenCaptureAllowed(userId);
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return false;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLocalVoiceInteractionStartedLocked(IBinder activity, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor) {
        ActivityRecord activityToCallback = ActivityRecord.forTokenLocked(activity);
        if (activityToCallback == null) {
            return;
        }
        activityToCallback.setVoiceSessionLocked(voiceSession);
        try {
            activityToCallback.app.getThread().scheduleLocalVoiceInteractionStarted(activity, voiceInteractor);
            long token = Binder.clearCallingIdentity();
            startRunningVoiceLocked(voiceSession, activityToCallback.info.applicationInfo.uid);
            Binder.restoreCallingIdentity(token);
        } catch (RemoteException e) {
            activityToCallback.clearVoiceSessionLocked();
        }
    }

    private void startRunningVoiceLocked(IVoiceInteractionSession session, int targetUid) {
        Slog.d(TAG, "<<<  startRunningVoiceLocked()");
        this.mVoiceWakeLock.setWorkSource(new WorkSource(targetUid));
        IVoiceInteractionSession iVoiceInteractionSession = this.mRunningVoice;
        if (iVoiceInteractionSession == null || iVoiceInteractionSession.asBinder() != session.asBinder()) {
            boolean wasRunningVoice = this.mRunningVoice != null;
            this.mRunningVoice = session;
            if (!wasRunningVoice) {
                this.mVoiceWakeLock.acquire();
                updateSleepIfNeededLocked();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishRunningVoiceLocked() {
        if (this.mRunningVoice != null) {
            this.mRunningVoice = null;
            this.mVoiceWakeLock.release();
            updateSleepIfNeededLocked();
        }
    }

    public void setVoiceKeepAwake(IVoiceInteractionSession session, boolean keepAwake) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                IVoiceInteractionSession iVoiceInteractionSession = this.mRunningVoice;
                if (iVoiceInteractionSession != null && iVoiceInteractionSession.asBinder() == session.asBinder()) {
                    if (keepAwake) {
                        this.mVoiceWakeLock.acquire();
                    } else {
                        this.mVoiceWakeLock.release();
                    }
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void keyguardGoingAway(final int flags) {
        enforceNotIsolatedCaller("keyguardGoingAway");
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                if ((flags & 22) != 0) {
                    this.mActivityClientController.invalidateHomeTaskSnapshot(null);
                }
                this.mRootWindowContainer.forAllDisplays(new Consumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda6
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ActivityTaskManagerService.this.m7830x2c9d7ea1(flags, (DisplayContent) obj);
                    }
                });
                if (!mIsLucidDisabled) {
                    setLucidFgApp("lucid.lockscreen");
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$keyguardGoingAway$5$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ void m7830x2c9d7ea1(int flags, DisplayContent displayContent) {
        ITranWindowManagerService.Instance().finishCurrentActivityKeepAwake(false);
        this.mKeyguardController.keyguardGoingAway(displayContent.getDisplayId(), flags);
    }

    public boolean isSplitScreen() {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord mResumedActivity = this.mRootWindowContainer.getDefaultTaskDisplayArea().getFocusedActivity();
                if (mResumedActivity == null) {
                    Slog.d(TAG, "isSplitScreen ResumeActiviy == null");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                Task task = mResumedActivity.getTask();
                if (task == null) {
                    Slog.d(TAG, "isSplitScreen task == null");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (task.getWindowingMode() != 6) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setConnectBlackListToSystem(List<String> list) {
        ITranActivityTaskManagerService.Instance().setConnectBlackListToSystem(list);
    }

    public List<String> getConnectBlackList() {
        return ITranActivityTaskManagerService.Instance().getConnectBlackList();
    }

    public void suppressResizeConfigChanges(boolean suppress) throws RemoteException {
        this.mAmInternal.enforceCallingPermission("android.permission.MANAGE_ACTIVITY_TASKS", "suppressResizeConfigChanges()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mSuppressResizeConfigChanges = suppress;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void onSplashScreenViewCopyFinished(int taskId, SplashScreenView.SplashScreenViewParcelable parcelable) throws RemoteException {
        ActivityRecord r;
        this.mAmInternal.enforceCallingPermission("android.permission.MANAGE_ACTIVITY_TASKS", "copySplashScreenViewFinish()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = this.mRootWindowContainer.anyTaskForId(taskId, 0);
                if (task != null && (r = task.getTopWaitSplashScreenActivity()) != null) {
                    r.onCopySplashScreenFinish(parcelable);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean enterPictureInPictureMode(final ActivityRecord r, final PictureInPictureParams params) {
        if (r.inPinnedWindowingMode()) {
            return true;
        }
        if (r.checkEnterPictureInPictureState("enterPictureInPictureMode", false)) {
            if (ITranActivityTaskManagerService.Instance().inMultiWindow(ITranActivityTaskManagerService.Instance().inMultiWindow(r) || (this.mRootWindowContainer.getMultiDisplayArea() != null && this.mRootWindowContainer.getMultiDisplayArea().getTopRootTask() == null)) || activityInMultiWindow(r.packageName)) {
                return false;
            }
            final Runnable enterPipRunnable = new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    ActivityTaskManagerService.this.m7828xdc90424c(r, params);
                }
            };
            if (r.isKeyguardLocked()) {
                this.mActivityClientController.dismissKeyguard(r.token, new KeyguardDismissCallback() { // from class: com.android.server.wm.ActivityTaskManagerService.2
                    public void onDismissSucceeded() {
                        ActivityTaskManagerService.this.mH.post(enterPipRunnable);
                    }
                }, null);
            } else {
                enterPipRunnable.run();
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$enterPictureInPictureMode$6$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ void m7828xdc90424c(ActivityRecord r, PictureInPictureParams params) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (r.getParent() == null) {
                    Slog.e(TAG, "Skip enterPictureInPictureMode, destroyed " + r);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                r.setPictureInPictureParams(params);
                this.mRootWindowContainer.moveActivityToPinnedRootTask(r, null, "enterPictureInPictureMode");
                Task task = r.getTask();
                if (task.getPausingActivity() == r) {
                    task.schedulePauseActivity(r, false, false, "auto-pip");
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setSplitScreenResizing(boolean resizing) {
        enforceTaskPermission("setSplitScreenResizing()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                this.mTaskSupervisor.setSplitScreenResizing(resizing);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public IWindowOrganizerController getWindowOrganizerController() {
        return this.mWindowOrganizerController;
    }

    public void enforceSystemHasVrFeature() {
        if (!this.mContext.getPackageManager().hasSystemFeature("android.hardware.vr.high_performance")) {
            throw new UnsupportedOperationException("VR mode not supported on this device!");
        }
    }

    public boolean supportsLocalVoiceInteraction() {
        return ((VoiceInteractionManagerInternal) LocalServices.getService(VoiceInteractionManagerInternal.class)).supportsLocalVoiceInteraction();
    }

    public boolean updateConfiguration(Configuration values) {
        this.mAmInternal.enforceCallingPermission("android.permission.CHANGE_CONFIGURATION", "updateConfiguration()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowManagerService windowManagerService = this.mWindowManager;
                if (windowManagerService == null) {
                    Slog.w(TAG, "Skip updateConfiguration because mWindowManager isn't set");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                if (values == null) {
                    values = windowManagerService.computeNewConfiguration(0);
                }
                this.mH.sendMessage(PooledLambda.obtainMessage(new ActivityTaskManagerService$$ExternalSyntheticLambda16(), this.mAmInternal, 0));
                long origId = Binder.clearCallingIdentity();
                if (values != null) {
                    Settings.System.clearConfiguration(values);
                }
                updateConfigurationLocked(values, null, false, false, -10000, false, this.mTmpUpdateConfigurationResult);
                boolean z = this.mTmpUpdateConfigurationResult.changes != 0;
                Binder.restoreCallingIdentity(origId);
                WindowManagerService.resetPriorityAfterLockedSection();
                return z;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void cancelTaskWindowTransition(int taskId) {
        enforceTaskPermission("cancelTaskWindowTransition()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = this.mRootWindowContainer.anyTaskForId(taskId, 0);
                if (task == null) {
                    Slog.w(TAG, "cancelTaskWindowTransition: taskId=" + taskId + " not found");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                task.cancelTaskWindowTransition();
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public TaskSnapshot getTaskSnapshot(int taskId, boolean isLowResolution) {
        this.mAmInternal.enforceCallingPermission("android.permission.READ_FRAME_BUFFER", "getTaskSnapshot()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = this.mRootWindowContainer.anyTaskForId(taskId, 1);
                if (task == null) {
                    Slog.w(TAG, "getTaskSnapshot: taskId=" + taskId + " not found");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return this.mWindowManager.mTaskSnapshotController.getSnapshot(taskId, task.mUserId, true, isLowResolution);
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public TaskSnapshot takeTaskSnapshot(int taskId) {
        this.mAmInternal.enforceCallingPermission("android.permission.READ_FRAME_BUFFER", "takeTaskSnapshot()");
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Task task = this.mRootWindowContainer.anyTaskForId(taskId, 1);
                if (task != null && task.isVisible()) {
                    TaskSnapshot captureTaskSnapshot = this.mWindowManager.mTaskSnapshotController.captureTaskSnapshot(task, false);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return captureTaskSnapshot;
                }
                Slog.w(TAG, "takeTaskSnapshot: taskId=" + taskId + " not found or not visible");
                WindowManagerService.resetPriorityAfterLockedSection();
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int getLastResumedActivityUserId() {
        this.mAmInternal.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "getLastResumedActivityUserId()");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord activityRecord = this.mLastResumedActivity;
                if (activityRecord == null) {
                    int currentUserId = getCurrentUserId();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return currentUserId;
                }
                int i = activityRecord.mUserId;
                WindowManagerService.resetPriorityAfterLockedSection();
                return i;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void updateLockTaskFeatures(int userId, int flags) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && callingUid != 1000) {
            this.mAmInternal.enforceCallingPermission("android.permission.UPDATE_LOCK_TASK_PACKAGES", "updateLockTaskFeatures()");
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
                    long protoLogParam0 = userId;
                    String protoLogParam1 = String.valueOf(Integer.toHexString(flags));
                    ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_LOCKTASK, -168799453, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0), protoLogParam1});
                }
                getLockTaskController().updateLockTaskFeatures(userId, flags);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void registerRemoteAnimationForNextActivityStart(String packageName, RemoteAnimationAdapter adapter, IBinder launchCookie) {
        this.mAmInternal.enforceCallingPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", "registerRemoteAnimationForNextActivityStart");
        adapter.setCallingPidUid(Binder.getCallingPid(), Binder.getCallingUid());
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                getActivityStartController().registerRemoteAnimationForNextActivityStart(packageName, adapter, launchCookie);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void registerRemoteAnimationsForDisplay(int displayId, RemoteAnimationDefinition definition) {
        this.mAmInternal.enforceCallingPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", "registerRemoteAnimations");
        definition.setCallingPidUid(Binder.getCallingPid(), Binder.getCallingUid());
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent display = this.mRootWindowContainer.getDisplayContent(displayId);
                if (display == null) {
                    Slog.e(TAG, "Couldn't find display with id: " + displayId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                long origId = Binder.clearCallingIdentity();
                display.registerRemoteAnimations(definition);
                Binder.restoreCallingIdentity(origId);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void alwaysShowUnsupportedCompileSdkWarning(ComponentName activity) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                this.mAppWarnings.alwaysShowUnsupportedCompileSdkWarning(activity);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setVrThread(int tid) {
        enforceSystemHasVrFeature();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int pid = Binder.getCallingPid();
                WindowProcessController wpc = this.mProcessMap.getProcess(pid);
                this.mVrController.setVrThreadLocked(tid, pid, wpc);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setPersistentVrThread(int tid) {
        if (checkCallingPermission("android.permission.RESTRICTED_VR_ACCESS") != 0) {
            String msg = "Permission Denial: setPersistentVrThread() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.RESTRICTED_VR_ACCESS";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        enforceSystemHasVrFeature();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int pid = Binder.getCallingPid();
                WindowProcessController proc = this.mProcessMap.getProcess(pid);
                this.mVrController.setPersistentVrThreadLocked(tid, pid, proc);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void stopAppSwitches() {
        this.mAmInternal.enforceCallingPermission("android.permission.STOP_APP_SWITCHES", "stopAppSwitches");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mAppSwitchesState = 0;
                this.mLastStopAppSwitchesTime = SystemClock.uptimeMillis();
                this.mH.removeMessages(4);
                this.mH.sendEmptyMessageDelayed(4, 500L);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void resumeAppSwitches() {
        this.mAmInternal.enforceCallingPermission("android.permission.STOP_APP_SWITCHES", "resumeAppSwitches");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mAppSwitchesState = 2;
                this.mH.removeMessages(4);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastStopAppSwitchesTime() {
        return this.mLastStopAppSwitchesTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldDisableNonVrUiLocked() {
        return this.mVrController.shouldDisableNonVrUiLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyUpdateVrModeLocked(final ActivityRecord r) {
        if (r.requestedVrComponent != null && r.getDisplayId() != 0) {
            Slog.i(TAG, "Moving " + r.shortComponentName + " from display " + r.getDisplayId() + " to main display for VR");
            this.mRootWindowContainer.moveRootTaskToDisplay(r.getRootTaskId(), 0, true);
        }
        this.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskManagerService.this.m7827x3bff8181(r);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$applyUpdateVrModeLocked$7$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ void m7827x3bff8181(ActivityRecord r) {
        if (!this.mVrController.onVrModeChanged(r)) {
            return;
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                boolean disableNonVrUi = this.mVrController.shouldDisableNonVrUiLocked();
                this.mWindowManager.disableNonVrUi(disableNonVrUi);
                if (disableNonVrUi) {
                    this.mRootWindowContainer.removeRootTasksInWindowingModes(2);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public int getPackageScreenCompatMode(String packageName) {
        int packageScreenCompatModeLocked;
        enforceNotIsolatedCaller("getPackageScreenCompatMode");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                packageScreenCompatModeLocked = this.mCompatModePackages.getPackageScreenCompatModeLocked(packageName);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return packageScreenCompatModeLocked;
    }

    public void setPackageScreenCompatMode(String packageName, int mode) {
        this.mAmInternal.enforceCallingPermission("android.permission.SET_SCREEN_COMPATIBILITY", "setPackageScreenCompatMode");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mCompatModePackages.setPackageScreenCompatModeLocked(packageName, mode);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean getPackageAskScreenCompat(String packageName) {
        boolean packageAskCompatModeLocked;
        enforceNotIsolatedCaller("getPackageAskScreenCompat");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                packageAskCompatModeLocked = this.mCompatModePackages.getPackageAskCompatModeLocked(packageName);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return packageAskCompatModeLocked;
    }

    public void setPackageAskScreenCompat(String packageName, boolean ask) {
        this.mAmInternal.enforceCallingPermission("android.permission.SET_SCREEN_COMPATIBILITY", "setPackageAskScreenCompat");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mCompatModePackages.setPackageAskCompatModeLocked(packageName, ask);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public static String relaunchReasonToString(int relaunchReason) {
        switch (relaunchReason) {
            case 1:
                return "window_resize";
            case 2:
                return "free_resize";
            default:
                return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTopDisplayFocusedRootTask() {
        return this.mRootWindowContainer.getTopDisplayFocusedRootTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskPersisterLocked(Task task, boolean flush) {
        this.mRecentTasks.notifyTaskPersisterLocked(task, flush);
    }

    boolean isKeyguardLocked(int displayId) {
        return this.mKeyguardController.isKeyguardLocked(displayId);
    }

    public void clearLaunchParamsForPackages(List<String> packageNames) {
        enforceTaskPermission("clearLaunchParamsForPackages");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (int i = 0; i < packageNames.size(); i++) {
                    this.mTaskSupervisor.mLaunchParamsPersister.removeRecordForPackage(packageNames.get(i));
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void onPictureInPictureStateChanged(PictureInPictureUiState pipState) {
        enforceTaskPermission("onPictureInPictureStateChanged");
        Task rootPinnedTask = this.mRootWindowContainer.getDefaultTaskDisplayArea().getRootPinnedTask();
        if (rootPinnedTask != null && rootPinnedTask.getTopMostActivity() != null) {
            this.mWindowManager.mAtmService.mActivityClientController.onPictureInPictureStateChanged(rootPinnedTask.getTopMostActivity(), pipState);
        }
    }

    public void detachNavigationBarFromApp(IBinder transition) {
        this.mAmInternal.enforceCallingPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", "detachNavigationBarFromApp");
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                getTransitionController().legacyDetachNavigationBarFromApp(transition);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void dumpLastANRLocked(PrintWriter pw) {
        pw.println("ACTIVITY MANAGER LAST ANR (dumpsys activity lastanr)");
        String str = this.mLastANRState;
        if (str == null) {
            pw.println("  <no ANR has occurred since boot>");
        } else {
            pw.println(str);
        }
    }

    void dumpLastANRTracesLocked(PrintWriter pw) {
        pw.println("ACTIVITY MANAGER LAST ANR TRACES (dumpsys activity lastanr-traces)");
        File[] files = new File(ActivityManagerService.ANR_TRACE_DIR).listFiles();
        if (ArrayUtils.isEmpty(files)) {
            pw.println("  <no ANR has occurred since boot>");
            return;
        }
        File latest = null;
        for (File f : files) {
            if (latest == null || latest.lastModified() < f.lastModified()) {
                latest = f;
            }
        }
        pw.print("File: ");
        pw.print(latest.getName());
        pw.println();
        try {
            BufferedReader in = new BufferedReader(new FileReader(latest));
            while (true) {
                String line = in.readLine();
                if (line != null) {
                    pw.println(line);
                } else {
                    in.close();
                    return;
                }
            }
        } catch (IOException e) {
            pw.print("Unable to read: ");
            pw.print(e);
            pw.println();
        }
    }

    void dumpTopResumedActivityLocked(PrintWriter pw) {
        pw.println("ACTIVITY MANAGER TOP-RESUMED (dumpsys activity top-resumed)");
        ActivityRecord topRecord = this.mRootWindowContainer.getTopResumedActivity();
        if (topRecord != null) {
            topRecord.dump(pw, "", true);
        }
    }

    void dumpActivitiesLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, boolean dumpClient, String dumpPackage) {
        dumpActivitiesLocked(fd, pw, args, opti, dumpAll, dumpClient, dumpPackage, "ACTIVITY MANAGER ACTIVITIES (dumpsys activity activities)");
    }

    void dumpActivitiesLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, boolean dumpClient, String dumpPackage, String header) {
        boolean needSep;
        pw.println(header);
        boolean printedAnything = this.mRootWindowContainer.dumpActivities(fd, pw, dumpAll, dumpClient, dumpPackage);
        boolean printed = ActivityTaskSupervisor.printThisActivity(pw, this.mRootWindowContainer.getTopResumedActivity(), dumpPackage, printedAnything, "  ResumedActivity: ", null);
        if (!printed) {
            needSep = printedAnything;
        } else {
            printedAnything = true;
            needSep = false;
        }
        if (dumpPackage == null) {
            if (needSep) {
                pw.println();
            }
            printedAnything = true;
            this.mTaskSupervisor.dump(pw, "  ");
            this.mTaskOrganizerController.dump(pw, "  ");
            this.mVisibleActivityProcessTracker.dump(pw, "  ");
            this.mActiveUids.dump(pw, "  ");
        }
        if (!printedAnything) {
            pw.println("  (nothing)");
        }
    }

    void dumpActivityContainersLocked(PrintWriter pw) {
        pw.println("ACTIVITY MANAGER CONTAINERS (dumpsys activity containers)");
        this.mRootWindowContainer.dumpChildrenNames(pw, " ");
        pw.println(" ");
    }

    void dumpActivityStarterLocked(PrintWriter pw, String dumpPackage) {
        pw.println("ACTIVITY MANAGER STARTER (dumpsys activity starter)");
        getActivityStartController().dump(pw, "", dumpPackage);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpInstalledPackagesConfig(PrintWriter pw) {
        this.mPackageConfigPersister.dump(pw, getCurrentUserId());
    }

    protected boolean dumpActivity(FileDescriptor fd, PrintWriter pw, String name, String[] args, int opti, boolean dumpAll, boolean dumpVisibleRootTasksOnly, boolean dumpFocusedRootTaskOnly, int userId) {
        Task lastTask;
        synchronized (this.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ArrayList<ActivityRecord> activities = this.mRootWindowContainer.getDumpActivities(name, dumpVisibleRootTasksOnly, dumpFocusedRootTaskOnly, userId);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (activities.size() <= 0) {
                        return false;
                    }
                    String[] newArgs = new String[args.length - opti];
                    System.arraycopy(args, opti, newArgs, 0, args.length - opti);
                    int i = activities.size() - 1;
                    Task lastTask2 = null;
                    Task lastTask3 = null;
                    while (i >= 0) {
                        ActivityRecord r = activities.get(i);
                        if (lastTask3 != null) {
                            pw.println();
                        }
                        synchronized (this.mGlobalLock) {
                            try {
                                WindowManagerService.boostPriorityForLockedSection();
                                Task task = r.getTask();
                                if (lastTask2 == task) {
                                    lastTask = lastTask2;
                                } else {
                                    try {
                                        pw.print("TASK ");
                                        pw.print(task.affinity);
                                        pw.print(" id=");
                                        pw.print(task.mTaskId);
                                        pw.print(" userId=");
                                        pw.println(task.mUserId);
                                        if (dumpAll) {
                                            task.dump(pw, "  ");
                                        }
                                        lastTask = task;
                                    } catch (Throwable th) {
                                        th = th;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th2) {
                                                th = th2;
                                            }
                                        }
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                }
                            } catch (Throwable th3) {
                                th = th3;
                            }
                            try {
                            } catch (Throwable th4) {
                                th = th4;
                                while (true) {
                                    break;
                                    break;
                                }
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                        dumpActivity("  ", fd, pw, activities.get(i), newArgs, dumpAll);
                        i--;
                        lastTask3 = 1;
                        lastTask2 = lastTask;
                        activities = activities;
                    }
                    return true;
                } catch (Throwable th5) {
                    th = th5;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void dumpActivity(String prefix, FileDescriptor fd, PrintWriter pw, ActivityRecord r, String[] args, boolean dumpAll) {
        String innerPrefix = prefix + "  ";
        IApplicationThread appThread = null;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                pw.print(prefix);
                pw.print("ACTIVITY ");
                pw.print(r.shortComponentName);
                pw.print(" ");
                pw.print(Integer.toHexString(System.identityHashCode(r)));
                pw.print(" pid=");
                if (r.hasProcess()) {
                    pw.println(r.app.getPid());
                    appThread = r.app.getThread();
                } else {
                    pw.println("(not running)");
                }
                if (dumpAll) {
                    r.dump(pw, innerPrefix, true);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (appThread != null) {
            pw.flush();
            try {
                TransferPipe tp = new TransferPipe();
                try {
                    appThread.dumpActivity(tp.getWriteFd(), r.token, innerPrefix, args);
                    tp.go(fd);
                    tp.close();
                } catch (Throwable th2) {
                    try {
                        tp.close();
                    } catch (Throwable th3) {
                        th2.addSuppressed(th3);
                    }
                    throw th2;
                }
            } catch (RemoteException e) {
                pw.println(innerPrefix + "Got a RemoteException while dumping the activity");
            } catch (IOException e2) {
                pw.println(innerPrefix + "Failure while dumping the activity: " + e2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeSleepStateToProto(ProtoOutputStream proto, int wakeFullness, boolean testPssMode) {
        long sleepToken = proto.start(1146756268059L);
        proto.write(1159641169921L, PowerManagerInternal.wakefulnessToProtoEnum(wakeFullness));
        int tokenSize = this.mRootWindowContainer.mSleepTokens.size();
        for (int i = 0; i < tokenSize; i++) {
            RootWindowContainer.SleepToken st = this.mRootWindowContainer.mSleepTokens.valueAt(i);
            proto.write(2237677961218L, st.toString());
        }
        proto.write(1133871366147L, this.mSleeping);
        proto.write(1133871366148L, this.mShuttingDown);
        proto.write(1133871366149L, testPssMode);
        proto.end(sleepToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentUserId() {
        return this.mAmInternal.getCurrentUserId();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enforceNotIsolatedCaller(String caller) {
        if (UserHandle.isIsolated(Binder.getCallingUid())) {
            throw new SecurityException("Isolated process not allowed to call " + caller);
        }
    }

    public Configuration getConfiguration() {
        Configuration ci;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ci = new Configuration(getGlobalConfigurationForCallingPid());
                ci.userSetLocale = false;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return ci;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Configuration getGlobalConfiguration() {
        RootWindowContainer rootWindowContainer = this.mRootWindowContainer;
        return rootWindowContainer != null ? rootWindowContainer.getConfiguration() : new Configuration();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateConfigurationLocked(Configuration values, ActivityRecord starting, boolean initLocale) {
        return updateConfigurationLocked(values, starting, initLocale, false);
    }

    boolean updateConfigurationLocked(Configuration values, ActivityRecord starting, boolean initLocale, boolean deferResume) {
        return updateConfigurationLocked(values, starting, initLocale, false, -10000, deferResume);
    }

    public void updatePersistentConfiguration(Configuration values, int userId) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                values.windowConfiguration.setToDefaults();
                updateConfigurationLocked(values, null, false, true, userId, false);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean updateConfigurationLocked(Configuration values, ActivityRecord starting, boolean initLocale, boolean persistent, int userId, boolean deferResume) {
        return updateConfigurationLocked(values, starting, initLocale, persistent, userId, deferResume, null);
    }

    boolean updateConfigurationLocked(Configuration values, ActivityRecord starting, boolean initLocale, boolean persistent, int userId, boolean deferResume, UpdateConfigurationResult result) {
        int changes = 0;
        boolean kept = true;
        deferWindowLayout();
        if (values != null) {
            try {
                changes = updateGlobalConfigurationLocked(values, initLocale, persistent, userId);
            } catch (Throwable th) {
                continueWindowLayout();
                throw th;
            }
        }
        if (!deferResume) {
            kept = ensureConfigAndVisibilityAfterUpdate(starting, changes);
        }
        continueWindowLayout();
        if (result != null) {
            result.changes = changes;
            result.activityRelaunched = !kept;
        }
        return kept;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int updateGlobalConfigurationLocked(Configuration values, boolean initLocale, boolean persistent, int userId) {
        this.mTempConfig.setTo(getGlobalConfiguration());
        ITranWindowManagerService.Instance().onUpdateGlobalConfigurationLocked(this.mContext, this.mTempConfig, values);
        int changes = this.mTempConfig.updateFrom(values);
        if (changes == 0) {
            return 0;
        }
        if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
            String protoLogParam0 = String.valueOf(values);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -927199900, 0, (String) null, new Object[]{protoLogParam0});
        }
        com.android.server.am.EventLogTags.writeConfigurationChanged(changes);
        FrameworkStatsLog.write(66, values.colorMode, values.densityDpi, values.fontScale, values.hardKeyboardHidden, values.keyboard, values.keyboardHidden, values.mcc, values.mnc, values.navigation, values.navigationHidden, values.orientation, values.screenHeightDp, values.screenLayout, values.screenWidthDp, values.smallestScreenWidthDp, values.touchscreen, values.uiMode);
        if (!initLocale && !values.getLocales().isEmpty() && values.userSetLocale) {
            LocaleList locales = values.getLocales();
            int bestLocaleIndex = 0;
            if (locales.size() > 1) {
                if (this.mSupportedSystemLocales == null) {
                    this.mSupportedSystemLocales = Resources.getSystem().getAssets().getLocales();
                }
                bestLocaleIndex = Math.max(0, locales.getFirstMatchIndex(this.mSupportedSystemLocales));
            }
            SystemProperties.set("persist.sys.locale", locales.get(bestLocaleIndex).toLanguageTag());
            LocaleList.setDefault(locales, bestLocaleIndex);
        }
        this.mTempConfig.seq = increaseConfigurationSeqLocked();
        Slog.i(TAG, "Config changes=" + Integer.toHexString(changes) + " " + this.mTempConfig);
        this.mUsageStatsInternal.reportConfigurationChange(this.mTempConfig, this.mAmInternal.getCurrentUserId());
        updateShouldShowDialogsLocked(this.mTempConfig);
        AttributeCache ac = AttributeCache.instance();
        if (ac != null) {
            ac.updateConfiguration(this.mTempConfig);
        }
        this.mSystemThread.applyConfigurationToResources(this.mTempConfig);
        if (persistent && Settings.System.hasInterestingConfigurationChanges(changes)) {
            Message msg = PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda9
                public final void accept(Object obj, Object obj2, Object obj3) {
                    ((ActivityTaskManagerService) obj).sendPutConfigurationForUserMsg(((Integer) obj2).intValue(), (Configuration) obj3);
                }
            }, this, Integer.valueOf(userId), new Configuration(this.mTempConfig));
            this.mH.sendMessage(msg);
        }
        SparseArray<WindowProcessController> pidMap = this.mProcessMap.getPidMap();
        for (int i = pidMap.size() - 1; i >= 0; i--) {
            int pid = pidMap.keyAt(i);
            WindowProcessController app = pidMap.get(pid);
            if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                String protoLogParam02 = String.valueOf(app.mName);
                String protoLogParam1 = String.valueOf(this.mTempConfig);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -503656156, 0, (String) null, new Object[]{protoLogParam02, protoLogParam1});
            }
            app.onConfigurationChanged(this.mTempConfig);
        }
        Message msg2 = PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda10
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((ActivityManagerInternal) obj).broadcastGlobalConfigurationChanged(((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue());
            }
        }, this.mAmInternal, Integer.valueOf(changes), Boolean.valueOf(initLocale));
        this.mH.sendMessage(msg2);
        this.mRootWindowContainer.onConfigurationChanged(this.mTempConfig);
        return changes;
    }

    private int increaseAssetConfigurationSeq() {
        int i = this.mGlobalAssetsSeq + 1;
        this.mGlobalAssetsSeq = i;
        int max = Math.max(i, 1);
        this.mGlobalAssetsSeq = max;
        return max;
    }

    public void updateAssetConfiguration(List<WindowProcessController> processes, boolean updateFrameworkRes) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int assetSeq = increaseAssetConfigurationSeq();
                if (updateFrameworkRes) {
                    Configuration newConfig = new Configuration();
                    newConfig.assetsSeq = assetSeq;
                    updateConfiguration(newConfig);
                }
                for (int i = processes.size() - 1; i >= 0; i--) {
                    WindowProcessController wpc = processes.get(i);
                    wpc.updateAssetConfiguration(assetSeq);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startLaunchPowerMode(int reason) {
        ITranActivityTaskManagerService.Instance().hookStartLaunchPowerMode(reason);
        PowerManagerInternal powerManagerInternal = this.mPowerManagerInternal;
        if (powerManagerInternal != null) {
            powerManagerInternal.setPowerMode(5, true);
        }
        this.mLaunchPowerModeReasons |= reason;
        if ((reason & 4) != 0) {
            if (this.mRetainPowerModeAndTopProcessState) {
                this.mH.removeMessages(3);
            }
            this.mRetainPowerModeAndTopProcessState = true;
            this.mH.sendEmptyMessageDelayed(3, 1000L);
            Slog.d(TAG, "Temporarily retain top process state for launching app");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void endLaunchPowerMode(int reason) {
        PowerManagerInternal powerManagerInternal;
        int i = this.mLaunchPowerModeReasons;
        if (i == 0) {
            return;
        }
        int i2 = i & (~reason);
        this.mLaunchPowerModeReasons = i2;
        if ((i2 & 4) != 0) {
            boolean allResolved = true;
            for (int i3 = this.mRootWindowContainer.getChildCount() - 1; i3 >= 0; i3--) {
                allResolved &= ((DisplayContent) this.mRootWindowContainer.getChildAt(i3)).mUnknownAppVisibilityController.allResolved();
            }
            if (allResolved) {
                this.mLaunchPowerModeReasons &= -5;
                this.mRetainPowerModeAndTopProcessState = false;
                this.mH.removeMessages(3);
            }
        }
        if (this.mLaunchPowerModeReasons == 0 && (powerManagerInternal = this.mPowerManagerInternal) != null) {
            powerManagerInternal.setPowerMode(5, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void deferWindowLayout() {
        if (!this.mWindowManager.mWindowPlacerLocked.isLayoutDeferred()) {
            this.mLayoutReasons = 0;
        }
        this.mWindowManager.mWindowPlacerLocked.deferLayout();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void continueWindowLayout() {
        this.mWindowManager.mWindowPlacerLocked.continueLayout(this.mLayoutReasons != 0);
        if (ActivityTaskManagerDebugConfig.DEBUG_ALL && !this.mWindowManager.mWindowPlacerLocked.isLayoutDeferred()) {
            Slog.i(TAG, "continueWindowLayout reason=" + this.mLayoutReasons);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addWindowLayoutReasons(int reasons) {
        this.mLayoutReasons |= reasons;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateEventDispatchingLocked(boolean booted) {
        this.mWindowManager.setEventDispatching(booted && !this.mShuttingDown);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendPutConfigurationForUserMsg(int userId, Configuration config) {
        ContentResolver resolver = this.mContext.getContentResolver();
        Settings.System.putConfigurationForUser(resolver, config, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isActivityStartsLoggingEnabled() {
        return this.mAmInternal.isActivityStartsLoggingEnabled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBackgroundActivityStartsEnabled() {
        return this.mAmInternal.isBackgroundActivityStartsEnabled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static long getInputDispatchingTimeoutMillisLocked(ActivityRecord r) {
        if (r == null || !r.hasProcess()) {
            return InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
        }
        return getInputDispatchingTimeoutMillisLocked(r.app);
    }

    private static long getInputDispatchingTimeoutMillisLocked(WindowProcessController r) {
        if (r == null) {
            return InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
        }
        return r.getInputDispatchingTimeoutMillis();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateShouldShowDialogsLocked(Configuration config) {
        boolean z = false;
        boolean inputMethodExists = (config.keyboard == 1 && config.touchscreen == 1 && config.navigation == 1) ? false : true;
        boolean hideDialogsSet = Settings.Global.getInt(this.mContext.getContentResolver(), "hide_error_dialogs", 0) != 0;
        if (inputMethodExists && ActivityTaskManager.currentUiModeSupportsErrorDialogs(config) && !hideDialogsSet) {
            z = true;
        }
        this.mShowDialogs = z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFontScaleIfNeeded(int userId) {
        if (userId != getCurrentUserId()) {
            return;
        }
        float scaleFactor = Settings.System.getFloatForUser(this.mContext.getContentResolver(), "font_scale", 1.0f, userId);
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (getGlobalConfiguration().fontScale == scaleFactor) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Configuration configuration = this.mWindowManager.computeNewConfiguration(0);
                configuration.fontScale = scaleFactor;
                updatePersistentConfiguration(configuration, userId);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFontWeightAdjustmentIfNeeded(int userId) {
        if (userId != getCurrentUserId()) {
            return;
        }
        int fontWeightAdjustment = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "font_weight_adjustment", Integer.MAX_VALUE, userId);
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (getGlobalConfiguration().fontWeightAdjustment == fontWeightAdjustment) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Configuration configuration = this.mWindowManager.computeNewConfiguration(0);
                configuration.fontWeightAdjustment = fontWeightAdjustment;
                updatePersistentConfiguration(configuration, userId);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSleepingOrShuttingDownLocked() {
        return isSleepingLocked() || this.mShuttingDown;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSleepingLocked() {
        return this.mSleeping;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setResumedActivityUncheckLocked(ActivityRecord r, String reason) {
        IVoiceInteractionSession session;
        Task task = r.getTask();
        if (task.isActivityTypeStandard()) {
            if (this.mCurAppTimeTracker != r.appTimeTracker) {
                AppTimeTracker appTimeTracker = this.mCurAppTimeTracker;
                if (appTimeTracker != null) {
                    appTimeTracker.stop();
                    this.mH.obtainMessage(1, this.mCurAppTimeTracker).sendToTarget();
                    this.mRootWindowContainer.clearOtherAppTimeTrackers(r.appTimeTracker);
                    this.mCurAppTimeTracker = null;
                }
                if (r.appTimeTracker != null) {
                    this.mCurAppTimeTracker = r.appTimeTracker;
                    startTimeTrackingFocusedActivityLocked();
                }
            } else {
                startTimeTrackingFocusedActivityLocked();
            }
        } else {
            r.appTimeTracker = null;
        }
        if (task.voiceInteractor != null) {
            startRunningVoiceLocked(task.voiceSession, r.info.applicationInfo.uid);
        } else {
            finishRunningVoiceLocked();
            ActivityRecord activityRecord = this.mLastResumedActivity;
            if (activityRecord != null) {
                Task lastResumedActivityTask = activityRecord.getTask();
                if (lastResumedActivityTask != null && lastResumedActivityTask.voiceSession != null) {
                    session = lastResumedActivityTask.voiceSession;
                } else {
                    session = this.mLastResumedActivity.voiceSession;
                }
                if (session != null) {
                    finishVoiceTask(session);
                }
            }
        }
        if (this.mLastResumedActivity != null && r.mUserId != this.mLastResumedActivity.mUserId) {
            this.mAmInternal.sendForegroundProfileChanged(r.mUserId);
        }
        ActivityRecord activityRecord2 = this.mLastResumedActivity;
        Task prevTask = activityRecord2 != null ? activityRecord2.getTask() : null;
        updateResumedAppTrace(r);
        this.mLastResumedActivity = r;
        ITranWindowManagerService.Instance().finishCurrentActivityKeepAwake(false);
        boolean changed = r.mDisplayContent.setFocusedApp(r);
        if (changed) {
            this.mWindowManager.updateFocusedWindowLocked(0, true);
        }
        if (prevTask == null || task != prevTask) {
            if (prevTask != null) {
                this.mTaskChangeNotificationController.notifyTaskFocusChanged(prevTask.mTaskId, false);
            }
            this.mTaskChangeNotificationController.notifyTaskFocusChanged(task.mTaskId, true);
        }
        applyUpdateLockStateLocked(r);
        applyUpdateVrModeLocked(r);
        if (r != null && r.getDisplayArea().mFeatureId == 1) {
            ITranSourceConnectManager.Instance().hookDisplayResumedActivityChanged(r.getDisplayId(), r.packageName);
        }
        EventLogTags.writeWmSetResumedActivity(r == null ? -1 : r.mUserId, r == null ? "NULL" : r.shortComponentName, reason);
    }

    /* loaded from: classes2.dex */
    final class SleepTokenAcquirerImpl implements ActivityTaskManagerInternal.SleepTokenAcquirer {
        private final SparseArray<RootWindowContainer.SleepToken> mSleepTokens = new SparseArray<>();
        private final String mTag;

        /* JADX INFO: Access modifiers changed from: package-private */
        public SleepTokenAcquirerImpl(String tag) {
            this.mTag = tag;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal.SleepTokenAcquirer
        public void acquire(int displayId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (!this.mSleepTokens.contains(displayId)) {
                        this.mSleepTokens.append(displayId, ActivityTaskManagerService.this.mRootWindowContainer.createSleepToken(this.mTag, displayId));
                        ActivityTaskManagerService.this.updateSleepIfNeededLocked();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal.SleepTokenAcquirer
        public void release(int displayId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    RootWindowContainer.SleepToken token = this.mSleepTokens.get(displayId);
                    if (token != null) {
                        ActivityTaskManagerService.this.mRootWindowContainer.removeSleepToken(token);
                        this.mSleepTokens.remove(displayId);
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSleepIfNeededLocked() {
        boolean shouldSleep = !this.mRootWindowContainer.hasAwakeDisplay();
        boolean wasSleeping = this.mSleeping;
        boolean updateOomAdj = false;
        if (!shouldSleep) {
            if (wasSleeping) {
                this.mSleeping = false;
                FrameworkStatsLog.write(14, 2);
                startTimeTrackingFocusedActivityLocked();
                this.mTopProcessState = 2;
                Slog.d(TAG, "Top Process State changed to PROCESS_STATE_TOP");
                this.mTaskSupervisor.comeOutOfSleepIfNeededLocked();
            }
            this.mRootWindowContainer.applySleepTokens(true);
            if (wasSleeping) {
                updateOomAdj = true;
            }
        } else if (!this.mSleeping && shouldSleep) {
            this.mSleeping = true;
            FrameworkStatsLog.write(14, 1);
            AppTimeTracker appTimeTracker = this.mCurAppTimeTracker;
            if (appTimeTracker != null) {
                appTimeTracker.stop();
            }
            this.mTopProcessState = 12;
            Slog.d(TAG, "Top Process State changed to PROCESS_STATE_TOP_SLEEPING");
            this.mTaskSupervisor.goingToSleepLocked();
            updateResumedAppTrace(null);
            updateOomAdj = true;
        }
        if (updateOomAdj) {
            updateOomAdj();
        }
        this.mAmsExt.onUpdateSleep(wasSleeping, this.mSleeping);
    }

    /* renamed from: com.android.server.wm.ActivityTaskManagerService$3  reason: invalid class name */
    /* loaded from: classes2.dex */
    class AnonymousClass3 implements Runnable {
        AnonymousClass3() {
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    DisplayContent defaultDisplayContent = ActivityTaskManagerService.this.mRootWindowContainer.getDefaultDisplay();
                    if (defaultDisplayContent == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    TaskDisplayArea tds = (TaskDisplayArea) defaultDisplayContent.getItemFromTaskDisplayAreas(new Function() { // from class: com.android.server.wm.ActivityTaskManagerService$3$$ExternalSyntheticLambda0
                        @Override // java.util.function.Function
                        public final Object apply(Object obj) {
                            return ActivityTaskManagerService.AnonymousClass3.lambda$run$0((TaskDisplayArea) obj);
                        }
                    });
                    if (tds != null && tds.getTopRootTask() != null) {
                        tds.getTopRootTask().moveToFront("AcquireFocus");
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ TaskDisplayArea lambda$run$0(TaskDisplayArea taskDisplayArea) {
            if (taskDisplayArea.isMultiWindow() && (taskDisplayArea.getMultiWindowingMode() & 64) == 0) {
                return taskDisplayArea;
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateMultiWindowMoveToFront() {
        this.mH.removeCallbacks(this.mAcquireMultiWindowFocusRunnbale);
        this.mH.post(this.mAcquireMultiWindowFocusRunnbale);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateOomAdj() {
        this.mH.removeCallbacks(this.mUpdateOomAdjRunnable);
        this.mH.post(this.mUpdateOomAdjRunnable);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateCpuStats() {
        H h = this.mH;
        final ActivityManagerInternal activityManagerInternal = this.mAmInternal;
        Objects.requireNonNull(activityManagerInternal);
        h.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                activityManagerInternal.updateCpuStats();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateBatteryStats(ActivityRecord component, boolean resumed) {
        Message m = PooledLambda.obtainMessage(new QuintConsumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda5
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                ((ActivityManagerInternal) obj).updateBatteryStats((ComponentName) obj2, ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), ((Boolean) obj5).booleanValue());
            }
        }, this.mAmInternal, component.mActivityComponent, Integer.valueOf(component.app.mUid), Integer.valueOf(component.mUserId), Boolean.valueOf(resumed));
        this.mH.sendMessage(m);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTopApp(ActivityRecord topResumedActivity) {
        ActivityRecord top = topResumedActivity != null ? topResumedActivity : this.mRootWindowContainer.getTopResumedActivity();
        this.mTopApp = top != null ? top.app : null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updatePreviousProcess(ActivityRecord stoppedActivity) {
        if (stoppedActivity.app != null && this.mTopApp != null && stoppedActivity.app != this.mTopApp && stoppedActivity.lastVisibleTime > this.mPreviousProcessVisibleTime && stoppedActivity.app != this.mHomeProcess) {
            this.mPreviousProcess = stoppedActivity.app;
            this.mPreviousProcessVisibleTime = stoppedActivity.lastVisibleTime;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateActivityUsageStats(ActivityRecord activity, int event) {
        ActivityRecord rootActivity;
        ComponentName taskRoot = null;
        Task task = activity.getTask();
        if (task != null && (rootActivity = task.getRootActivity()) != null) {
            taskRoot = rootActivity.mActivityComponent;
        }
        ITranWindowManagerService.Instance().onUpdateActivityUsageStats(event, activity);
        Message m = PooledLambda.obtainMessage(new HexConsumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda17
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
                ((ActivityManagerInternal) obj).updateActivityUsageStats((ComponentName) obj2, ((Integer) obj3).intValue(), ((Integer) obj4).intValue(), (IBinder) obj5, (ComponentName) obj6);
            }
        }, this.mAmInternal, activity.mActivityComponent, Integer.valueOf(activity.mUserId), Integer.valueOf(event), activity.token, taskRoot);
        this.mH.sendMessage(m);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startProcessAsync(ActivityRecord activity, boolean knownToBeDead, boolean isTop, String hostingType) {
        try {
            if (Trace.isTagEnabled(32L)) {
                Trace.traceBegin(32L, "dispatchingStartProcess:" + activity.processName);
            }
            Message m = PooledLambda.obtainMessage(new HeptConsumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda0
                public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7) {
                    ((ActivityManagerInternal) obj).startProcess((String) obj2, (ApplicationInfo) obj3, ((Boolean) obj4).booleanValue(), ((Boolean) obj5).booleanValue(), (String) obj6, (ComponentName) obj7);
                }
            }, this.mAmInternal, activity.processName, activity.info.applicationInfo, Boolean.valueOf(knownToBeDead), Boolean.valueOf(isTop), hostingType, activity.intent.getComponent());
            this.mH.sendMessage(m);
        } finally {
            Trace.traceEnd(32L);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBooting(boolean booting) {
        this.mAmInternal.setBooting(booting);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBooting() {
        return this.mAmInternal.isBooting();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setBooted(boolean booted) {
        this.mAmInternal.setBooted(booted);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBooted() {
        return this.mAmInternal.isBooted();
    }

    public void onPackagesReady() {
        ActivityStarter.initDualprofileAppList(this.mContext);
        ((LauncherApps) this.mContext.getSystemService("launcherapps")).registerCallback(mPackageCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postFinishBooting(final boolean finishBooting, final boolean enableScreen) {
        this.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskManagerService.this.m7833xe2cc9943(finishBooting, enableScreen);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$postFinishBooting$8$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ void m7833xe2cc9943(boolean finishBooting, boolean enableScreen) {
        if (finishBooting) {
            this.mAmInternal.finishBooting();
        }
        if (enableScreen) {
            this.mInternal.enableScreenAfterBoot(isBooted());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHeavyWeightProcess(ActivityRecord root) {
        this.mHeavyWeightProcess = root.app;
        Message m = PooledLambda.obtainMessage(new QuadConsumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda3
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                ((ActivityTaskManagerService) obj).postHeavyWeightProcessNotification((WindowProcessController) obj2, (Intent) obj3, ((Integer) obj4).intValue());
            }
        }, this, root.app, root.intent, Integer.valueOf(root.mUserId));
        this.mH.sendMessage(m);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearHeavyWeightProcessIfEquals(WindowProcessController proc) {
        if (this.mHeavyWeightProcess == null || this.mHeavyWeightProcess != proc) {
            return;
        }
        this.mHeavyWeightProcess = null;
        Message m = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda12
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ActivityTaskManagerService) obj).cancelHeavyWeightProcessNotification(((Integer) obj2).intValue());
            }
        }, this, Integer.valueOf(proc.mUserId));
        this.mH.sendMessage(m);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelHeavyWeightProcessNotification(int userId) {
        INotificationManager inm = NotificationManager.getService();
        if (inm == null) {
            return;
        }
        try {
            inm.cancelNotificationWithTag(PackageManagerService.PLATFORM_PACKAGE_NAME, PackageManagerService.PLATFORM_PACKAGE_NAME, (String) null, 11, userId);
        } catch (RemoteException e) {
        } catch (RuntimeException e2) {
            Slog.w(TAG, "Error canceling notification for service", e2);
        }
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:11:0x0092 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:20:0x000c */
    /* JADX DEBUG: Multi-variable search result rejected for r0v1, resolved type: java.lang.String */
    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v0, types: [java.lang.String] */
    /* JADX WARN: Type inference failed for: r0v2, types: [android.os.RemoteException] */
    /* JADX WARN: Type inference failed for: r0v3 */
    public void postHeavyWeightProcessNotification(WindowProcessController proc, Intent intent, int userId) {
        INotificationManager inm;
        String e = TAG;
        if (proc == null || (inm = NotificationManager.getService()) == null) {
            return;
        }
        try {
            Context context = this.mContext.createPackageContext(proc.mInfo.packageName, 0);
            String text = this.mContext.getString(17040440, context.getApplicationInfo().loadLabel(context.getPackageManager()));
            Notification notification = new Notification.Builder(context, SystemNotificationChannels.HEAVY_WEIGHT_APP).setSmallIcon(17303614).setWhen(0L).setOngoing(true).setTicker(text).setColor(this.mContext.getColor(17170460)).setContentTitle(text).setContentText(this.mContext.getText(17040441)).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, intent, AudioFormat.AAC_ADIF, null, new UserHandle(userId))).build();
            try {
                inm.enqueueNotificationWithTag(PackageManagerService.PLATFORM_PACKAGE_NAME, PackageManagerService.PLATFORM_PACKAGE_NAME, (String) null, 11, notification, userId);
            } catch (RemoteException e2) {
                e = e2;
            } catch (RuntimeException e3) {
                Slog.w(TAG, "Error showing notification for heavy-weight app", e3);
            }
        } catch (PackageManager.NameNotFoundException e4) {
            Slog.w(e, "Unable to create context for heavy notification", e4);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IIntentSender getIntentSenderLocked(int type, String packageName, String featureId, int callingUid, int userId, IBinder token, String resultWho, int requestCode, Intent[] intents, String[] resolvedTypes, int flags, Bundle bOptions) {
        ActivityRecord activity;
        if (type == 3) {
            ActivityRecord activity2 = ActivityRecord.isInRootTaskLocked(token);
            if (activity2 == null) {
                Slog.w(TAG, "Failed createPendingResult: activity " + token + " not in any root task");
                return null;
            } else if (!activity2.finishing) {
                activity = activity2;
            } else {
                Slog.w(TAG, "Failed createPendingResult: activity " + activity2 + " is finishing");
                return null;
            }
        } else {
            activity = null;
        }
        ActivityRecord activity3 = activity;
        PendingIntentRecord rec = this.mPendingIntentController.getIntentSender(type, packageName, featureId, callingUid, userId, token, resultWho, requestCode, intents, resolvedTypes, flags, bOptions);
        boolean noCreate = (flags & 536870912) != 0;
        if (noCreate) {
            return rec;
        }
        if (type == 3) {
            if (activity3.pendingResults == null) {
                activity3.pendingResults = new HashSet<>();
            }
            activity3.pendingResults.add(rec.ref);
        }
        return rec;
    }

    private void startTimeTrackingFocusedActivityLocked() {
        AppTimeTracker appTimeTracker;
        ActivityRecord resumedActivity = this.mRootWindowContainer.getTopResumedActivity();
        if (!this.mSleeping && (appTimeTracker = this.mCurAppTimeTracker) != null && resumedActivity != null) {
            appTimeTracker.start(resumedActivity.packageName);
        }
    }

    private void updateResumedAppTrace(ActivityRecord resumed) {
        ActivityRecord activityRecord = this.mTracedResumedActivity;
        if (activityRecord != null) {
            Trace.asyncTraceEnd(32L, constructResumedTraceName(activityRecord.packageName), 0);
        }
        if (resumed != null) {
            Trace.asyncTraceBegin(32L, constructResumedTraceName(resumed.packageName), 0);
        }
        this.mTracedResumedActivity = resumed;
    }

    private String constructResumedTraceName(String packageName) {
        return "focused app: " + packageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean ensureConfigAndVisibilityAfterUpdate(ActivityRecord starting, int changes) {
        Task mainRootTask = this.mRootWindowContainer.getTopDisplayFocusedRootTask();
        if (mainRootTask == null) {
            return true;
        }
        if (changes != 0 && starting == null) {
            starting = mainRootTask.topRunningActivity();
        }
        if (starting == null) {
            return true;
        }
        boolean kept = starting.ensureActivityConfiguration(changes, false);
        this.mRootWindowContainer.ensureActivitiesVisible(starting, changes, false);
        return kept;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleAppGcsLocked$9$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ void m7834x424da94e() {
        this.mAmInternal.scheduleAppGcs();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleAppGcsLocked() {
        this.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskManagerService.this.m7834x424da94e();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CompatibilityInfo compatibilityInfoForPackageLocked(ApplicationInfo ai) {
        return this.mCompatModePackages.compatibilityInfoForPackageLocked(ai);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IPackageManager getPackageManager() {
        return AppGlobals.getPackageManager();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageManagerInternal getPackageManagerInternalLocked() {
        if (this.mPmInternal == null) {
            this.mPmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }
        return this.mPmInternal;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getSysUiServiceComponentLocked() {
        if (this.mSysUiServiceComponent == null) {
            PackageManagerInternal pm = getPackageManagerInternalLocked();
            this.mSysUiServiceComponent = pm.getSystemUiServiceComponent();
        }
        return this.mSysUiServiceComponent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PermissionPolicyInternal getPermissionPolicyInternal() {
        if (this.mPermissionPolicyInternal == null) {
            this.mPermissionPolicyInternal = (PermissionPolicyInternal) LocalServices.getService(PermissionPolicyInternal.class);
        }
        return this.mPermissionPolicyInternal;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public StatusBarManagerInternal getStatusBarManagerInternal() {
        if (this.mStatusBarManagerInternal == null) {
            this.mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        }
        return this.mStatusBarManagerInternal;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AppWarnings getAppWarningsLocked() {
        return this.mAppWarnings;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Intent getHomeIntent() {
        String str = this.mTopAction;
        String str2 = this.mTopData;
        Intent intent = new Intent(str, str2 != null ? Uri.parse(str2) : null);
        intent.setComponent(this.mTopComponent);
        intent.addFlags(256);
        if (this.mFactoryTest != 1) {
            intent.addCategory("android.intent.category.HOME");
        }
        return intent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Intent getSecondaryHomeIntent(String preferredPackage) {
        String str = this.mTopAction;
        String str2 = this.mTopData;
        Intent intent = new Intent(str, str2 != null ? Uri.parse(str2) : null);
        boolean useSystemProvidedLauncher = this.mContext.getResources().getBoolean(17891816);
        if (preferredPackage == null || useSystemProvidedLauncher) {
            String secondaryHomePackage = this.mContext.getResources().getString(17040033);
            intent.setPackage(secondaryHomePackage);
        } else {
            intent.setPackage(preferredPackage);
        }
        intent.addFlags(256);
        if (this.mFactoryTest != 1) {
            intent.addCategory("android.intent.category.SECONDARY_HOME");
        }
        return intent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ApplicationInfo getAppInfoForUser(ApplicationInfo info, int userId) {
        if (info == null) {
            return null;
        }
        ApplicationInfo newInfo = new ApplicationInfo(info);
        newInfo.initForUser(userId);
        return newInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowProcessController getProcessController(String processName, int uid) {
        if (uid == 1000) {
            SparseArray<WindowProcessController> procs = (SparseArray) this.mProcessNames.getMap().get(processName);
            if (procs == null) {
                return null;
            }
            int procCount = procs.size();
            for (int i = 0; i < procCount; i++) {
                int procUid = procs.keyAt(i);
                if (!UserHandle.isApp(procUid) && UserHandle.isSameUser(procUid, uid)) {
                    return procs.valueAt(i);
                }
            }
        }
        return (WindowProcessController) this.mProcessNames.get(processName, uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowProcessController getProcessController(IApplicationThread thread) {
        if (thread == null) {
            return null;
        }
        IBinder threadBinder = thread.asBinder();
        ArrayMap<String, SparseArray<WindowProcessController>> pmap = this.mProcessNames.getMap();
        for (int i = pmap.size() - 1; i >= 0; i--) {
            SparseArray<WindowProcessController> procs = pmap.valueAt(i);
            for (int j = procs.size() - 1; j >= 0; j--) {
                WindowProcessController proc = procs.valueAt(j);
                if (proc.hasThread() && proc.getThread().asBinder() == threadBinder) {
                    return proc;
                }
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowProcessController getProcessController(int pid, int uid) {
        WindowProcessController proc = this.mProcessMap.getProcess(pid);
        if (proc == null || !UserHandle.isApp(uid) || proc.mUid != uid) {
            return null;
        }
        return proc;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasActiveVisibleWindow(int uid) {
        if (this.mVisibleActivityProcessTracker.hasVisibleActivity(uid)) {
            return true;
        }
        return this.mActiveUids.hasNonAppVisibleWindow(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDeviceOwner(int uid) {
        return uid >= 0 && this.mDeviceOwnerUid == uid;
    }

    void setDeviceOwnerUid(int uid) {
        this.mDeviceOwnerUid = uid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveANRState(String reason) {
        StringWriter sw = new StringWriter();
        FastPrintWriter fastPrintWriter = new FastPrintWriter(sw, false, 1024);
        fastPrintWriter.println("  ANR time: " + DateFormat.getDateTimeInstance().format(new Date()));
        if (reason != null) {
            fastPrintWriter.println("  Reason: " + reason);
        }
        fastPrintWriter.println();
        getActivityStartController().dump(fastPrintWriter, "  ", null);
        fastPrintWriter.println();
        fastPrintWriter.println("-------------------------------------------------------------------------------");
        dumpActivitiesLocked(null, fastPrintWriter, null, 0, true, false, null, "");
        fastPrintWriter.println();
        fastPrintWriter.close();
        this.mLastANRState = sw.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logAppTooSlow(WindowProcessController app, long startTime, String msg) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAssociatedCompanionApp(int userId, int uid) {
        Set<Integer> allUids = this.mCompanionAppUidsMap.get(Integer.valueOf(userId));
        if (allUids == null) {
            return false;
        }
        return allUids.contains(Integer.valueOf(uid));
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            if (super.onTransact(code, data, reply, flags)) {
                return true;
            }
            Boolean res = ITranWindowManagerService.Instance().onTransactIActivityTaskManager(this, code, data, reply, flags);
            if (res == null) {
                return false;
            }
            return res.booleanValue();
        } catch (RuntimeException e) {
            throw logAndRethrowRuntimeExceptionOnTransact(TAG, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static RuntimeException logAndRethrowRuntimeExceptionOnTransact(String name, RuntimeException e) {
        if (!(e instanceof SecurityException)) {
            Slog.w(TAG, name + " onTransact aborts UID:" + Binder.getCallingUid() + " PID:" + Binder.getCallingPid(), e);
        }
        throw e;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onImeWindowSetOnDisplayArea(int pid, DisplayArea imeContainer) {
        if (pid == ActivityManagerService.MY_PID || pid < 0) {
            if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -1810446914, 0, (String) null, (Object[]) null);
                return;
            }
            return;
        }
        WindowProcessController process = this.mProcessMap.getProcess(pid);
        if (process == null) {
            if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                long protoLogParam0 = pid;
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -449118559, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
                return;
            }
            return;
        }
        process.registerDisplayAreaConfigurationListener(imeContainer);
    }

    public void setRunningRemoteTransitionDelegate(IApplicationThread caller) {
        this.mAmInternal.enforceCallingPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", "setRunningRemoteTransition");
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowProcessController callingProc = getProcessController(callingPid, callingUid);
                if (callingProc == null || !callingProc.isRunningRemoteTransition()) {
                    String msg = "Can't call setRunningRemoteTransition from a process (pid=" + callingPid + " uid=" + callingUid + ") which isn't itself running a remote transition.";
                    Slog.e(TAG, msg);
                    throw new SecurityException(msg);
                }
                WindowProcessController wpc = getProcessController(caller);
                if (wpc == null) {
                    Slog.w(TAG, "Unable to find process for application " + caller);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                wpc.setRunningRemoteAnimation(true);
                callingProc.addRemoteAnimationDelegate(wpc);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean instrumentationSourceHasPermission(int pid, String permission) {
        WindowProcessController process;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                process = this.mProcessMap.getProcess(pid);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (process == null || !process.isInstrumenting()) {
            return false;
        }
        int sourceUid = process.getInstrumentationSourceUid();
        return checkPermission(permission, -1, sourceUid) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class H extends Handler {
        static final int END_POWER_MODE_UNKNOWN_VISIBILITY_MSG = 3;
        static final int FIRST_ACTIVITY_TASK_MSG = 100;
        static final int FIRST_SUPERVISOR_TASK_MSG = 200;
        static final int REPORT_TIME_TRACKER_MSG = 1;
        static final int RESUME_FG_APP_SWITCH_MSG = 4;
        static final int UPDATE_PROCESS_ANIMATING_STATE = 2;

        H(Looper looper) {
            super(looper);
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AppTimeTracker tracker = (AppTimeTracker) msg.obj;
                    tracker.deliverResult(ActivityTaskManagerService.this.mContext);
                    return;
                case 2:
                    WindowProcessController proc = (WindowProcessController) msg.obj;
                    synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            proc.updateRunningRemoteOrRecentsAnimation();
                        } finally {
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                case 3:
                    synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            ActivityTaskManagerService.this.mRetainPowerModeAndTopProcessState = false;
                            ActivityTaskManagerService.this.endLaunchPowerMode(4);
                            if (ActivityTaskManagerService.this.mTopApp != null && ActivityTaskManagerService.this.mTopProcessState == 12) {
                                ActivityTaskManagerService.this.mTopApp.updateProcessInfo(false, false, true, false);
                            }
                        } finally {
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                case 4:
                    synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (ActivityTaskManagerService.this.mAppSwitchesState == 0) {
                                ActivityTaskManagerService.this.mAppSwitchesState = 1;
                            }
                        } finally {
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class UiHandler extends Handler {
        static final int DISMISS_DIALOG_UI_MSG = 1;

        public UiHandler() {
            super(UiThread.get().getLooper(), null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Dialog d = (Dialog) msg.obj;
                    d.dismiss();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class LocalService extends ActivityTaskManagerInternal {
        LocalService() {
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public ActivityTaskManagerInternal.SleepTokenAcquirer createSleepTokenAcquirer(String tag) {
            Objects.requireNonNull(tag);
            return new SleepTokenAcquirerImpl(tag);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public ComponentName getHomeActivityForUser(int userId) {
            ComponentName componentName;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityRecord homeActivity = ActivityTaskManagerService.this.mRootWindowContainer.getDefaultDisplayHomeActivityForUser(userId);
                    componentName = homeActivity == null ? null : homeActivity.mActivityComponent;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return componentName;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onLocalVoiceInteractionStarted(IBinder activity, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.onLocalVoiceInteractionStartedLocked(activity, voiceSession, voiceInteractor);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public List<ActivityAssistInfo> getTopVisibleActivities() {
            List<ActivityAssistInfo> topVisibleActivities;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    topVisibleActivities = ActivityTaskManagerService.this.mRootWindowContainer.getTopVisibleActivities();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return topVisibleActivities;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean hasResumedActivity(int uid) {
            return ActivityTaskManagerService.this.mVisibleActivityProcessTracker.hasResumedActivity(uid);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setBackgroundActivityStartCallback(BackgroundActivityStartCallback backgroundActivityStartCallback) {
            ActivityTaskManagerService.this.mBackgroundActivityStartCallback = backgroundActivityStartCallback;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setAccessibilityServiceUids(IntArray uids) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mAccessibilityServiceUids = uids.toArray();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [5957=4] */
        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public int startActivitiesAsPackage(String packageName, String featureId, int userId, Intent[] intents, Bundle bOptions) {
            int packageUid;
            Objects.requireNonNull(intents, "intents");
            String[] resolvedTypes = new String[intents.length];
            long ident = Binder.clearCallingIdentity();
            for (int i = 0; i < intents.length; i++) {
                try {
                    resolvedTypes[i] = intents[i].resolveTypeIfNeeded(ActivityTaskManagerService.this.mContext.getContentResolver());
                } catch (RemoteException e) {
                } catch (Throwable th) {
                    th = th;
                }
            }
            try {
                packageUid = AppGlobals.getPackageManager().getPackageUid(packageName, 268435456L, userId);
                Binder.restoreCallingIdentity(ident);
            } catch (RemoteException e2) {
                Binder.restoreCallingIdentity(ident);
                packageUid = 0;
                return ActivityTaskManagerService.this.getActivityStartController().startActivitiesInPackage(packageUid, packageName, featureId, intents, resolvedTypes, null, SafeActivityOptions.fromBundle(bOptions), userId, false, null, false);
            } catch (Throwable th2) {
                th = th2;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
            return ActivityTaskManagerService.this.getActivityStartController().startActivitiesInPackage(packageUid, packageName, featureId, intents, resolvedTypes, null, SafeActivityOptions.fromBundle(bOptions), userId, false, null, false);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public int startActivitiesInPackage(int uid, int realCallingPid, int realCallingUid, String callingPackage, String callingFeatureId, Intent[] intents, String[] resolvedTypes, IBinder resultTo, SafeActivityOptions options, int userId, boolean validateIncomingUser, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart) {
            ActivityTaskManagerService.this.assertPackageMatchesCallingUid(callingPackage);
            return ActivityTaskManagerService.this.getActivityStartController().startActivitiesInPackage(uid, realCallingPid, realCallingUid, callingPackage, callingFeatureId, intents, resolvedTypes, resultTo, options, userId, validateIncomingUser, originatingPendingIntent, allowBackgroundActivityStart);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public int startActivityInPackage(int uid, int realCallingPid, int realCallingUid, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, IBinder resultTo, String resultWho, int requestCode, int startFlags, SafeActivityOptions options, int userId, Task inTask, String reason, boolean validateIncomingUser, PendingIntentRecord originatingPendingIntent, boolean allowBackgroundActivityStart) {
            ActivityTaskManagerService.this.assertPackageMatchesCallingUid(callingPackage);
            return ActivityTaskManagerService.this.getActivityStartController().startActivityInPackage(uid, realCallingPid, realCallingUid, callingPackage, callingFeatureId, intent, resolvedType, resultTo, resultWho, requestCode, startFlags, options, userId, inTask, reason, validateIncomingUser, originatingPendingIntent, allowBackgroundActivityStart);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public int startActivityAsUser(IApplicationThread caller, String callerPackage, String callerFeatureId, Intent intent, IBinder resultTo, int startFlags, Bundle options, int userId) {
            ActivityTaskManagerService activityTaskManagerService = ActivityTaskManagerService.this;
            return activityTaskManagerService.startActivityAsUser(caller, callerPackage, callerFeatureId, intent, intent.resolveTypeIfNeeded(activityTaskManagerService.mContext.getContentResolver()), resultTo, null, 0, startFlags, null, options, userId, false);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setVr2dDisplayId(int vr2dDisplayId) {
            if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                long protoLogParam0 = vr2dDisplayId;
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1679411993, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
            }
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mVr2dDisplayId = vr2dDisplayId;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setFocusedActivity(IBinder token) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.forTokenLocked(token);
                    if (r == null) {
                        throw new IllegalArgumentException("setFocusedActivity: No activity record matching token=" + token);
                    }
                    if (r.moveFocusableActivityToTop("setFocusedActivity")) {
                        ActivityTaskManagerService.this.mRootWindowContainer.resumeFocusedTasksTopActivities();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void registerScreenObserver(ActivityTaskManagerInternal.ScreenObserver observer) {
            ActivityTaskManagerService.this.mScreenObservers.add(observer);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean isCallerRecents(int callingUid) {
            return ActivityTaskManagerService.this.isCallerRecents(callingUid);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean isRecentsComponentHomeActivity(int userId) {
            return ActivityTaskManagerService.this.getRecentTasks().isRecentsComponentHomeActivity(userId);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean checkCanCloseSystemDialogs(int pid, int uid, String packageName) {
            return ActivityTaskManagerService.this.checkCanCloseSystemDialogs(pid, uid, packageName);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean canCloseSystemDialogs(int pid, int uid) {
            return ActivityTaskManagerService.this.canCloseSystemDialogs(pid, uid);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void notifyActiveVoiceInteractionServiceChanged(ComponentName component) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mActiveVoiceInteractionServiceComponent = component;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void notifyDreamStateChanged(boolean dreaming) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mDreaming = dreaming;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setAllowAppSwitches(String type, int uid, int userId) {
            if (!ActivityTaskManagerService.this.mAmInternal.isUserRunning(userId, 1)) {
                return;
            }
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ArrayMap<String, Integer> types = ActivityTaskManagerService.this.mAllowAppSwitchUids.get(userId);
                    if (types == null) {
                        if (uid < 0) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        } else {
                            types = new ArrayMap<>();
                            ActivityTaskManagerService.this.mAllowAppSwitchUids.put(userId, types);
                        }
                    }
                    if (uid < 0) {
                        types.remove(type);
                    } else {
                        types.put(type, Integer.valueOf(uid));
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onUserStopped(int userId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.getRecentTasks().unloadUserDataFromMemoryLocked(userId);
                    ActivityTaskManagerService.this.mAllowAppSwitchUids.remove(userId);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean isGetTasksAllowed(String caller, int callingPid, int callingUid) {
            return ActivityTaskManagerService.this.isGetTasksAllowed(caller, callingPid, callingUid);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onProcessAdded(WindowProcessController proc) {
            synchronized (ActivityTaskManagerService.this.mGlobalLockWithoutBoost) {
                ActivityTaskManagerService.this.mProcessNames.put(proc.mName, proc.mUid, proc);
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onProcessRemoved(String name, int uid) {
            synchronized (ActivityTaskManagerService.this.mGlobalLockWithoutBoost) {
                ActivityTaskManagerService.this.mProcessNames.remove(name, uid);
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onCleanUpApplicationRecord(WindowProcessController proc) {
            synchronized (ActivityTaskManagerService.this.mGlobalLockWithoutBoost) {
                if (proc == ActivityTaskManagerService.this.mHomeProcess) {
                    ActivityTaskManagerService.this.mHomeProcess = null;
                    if (ITranGriffinFeature.Instance().isGriffinSupport()) {
                        if (ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
                            Slog.d("TranGriffin/AppSwitch", "Home changed to null");
                        }
                        ITranActivityTaskManagerService.Instance().hookOnCleanUpApplicationRecord();
                    }
                }
                if (proc == ActivityTaskManagerService.this.mPreviousProcess) {
                    ActivityTaskManagerService.this.mPreviousProcess = null;
                    if (ITranGriffinFeature.Instance().isGriffinDebugOpen() && ITranGriffinFeature.Instance().isGriffinSupport()) {
                        Slog.d("TranGriffin/cleanUpApplicationRecordLocked", "Previous changed to null");
                    }
                }
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public int getTopProcessState() {
            if (ActivityTaskManagerService.this.mRetainPowerModeAndTopProcessState) {
                return 2;
            }
            return ActivityTaskManagerService.this.mTopProcessState;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void clearHeavyWeightProcessIfEquals(WindowProcessController proc) {
            synchronized (ActivityTaskManagerService.this.mGlobalLockWithoutBoost) {
                ActivityTaskManagerService.this.clearHeavyWeightProcessIfEquals(proc);
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void finishHeavyWeightApp() {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ActivityTaskManagerService.this.mHeavyWeightProcess != null) {
                        ActivityTaskManagerService.this.mHeavyWeightProcess.finishActivities();
                    }
                    ActivityTaskManagerService activityTaskManagerService = ActivityTaskManagerService.this;
                    activityTaskManagerService.clearHeavyWeightProcessIfEquals(activityTaskManagerService.mHeavyWeightProcess);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean isSleeping() {
            return ActivityTaskManagerService.this.mSleeping;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean isShuttingDown() {
            boolean z;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    z = ActivityTaskManagerService.this.mShuttingDown;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return z;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean shuttingDown(boolean booted, int timeout) {
            boolean shutdownLocked;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mShuttingDown = true;
                    ActivityTaskManagerService.this.mRootWindowContainer.prepareForShutdown();
                    ActivityTaskManagerService.this.updateEventDispatchingLocked(booted);
                    ActivityTaskManagerService.this.notifyTaskPersisterLocked(null, true);
                    shutdownLocked = ActivityTaskManagerService.this.mTaskSupervisor.shutdownLocked(timeout);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return shutdownLocked;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void enableScreenAfterBoot(boolean booted) {
            com.android.server.am.EventLogTags.writeBootProgressEnableScreen(SystemClock.uptimeMillis());
            MtkSystemServer.getInstance().addBootEvent("AMS:ENABLE_SCREEN");
            ActivityTaskManagerService.this.mWindowManager.enableScreenAfterBoot();
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.updateEventDispatchingLocked(booted);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean showStrictModeViolationDialog() {
            boolean z;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    z = (!ActivityTaskManagerService.this.mShowDialogs || ActivityTaskManagerService.this.mSleeping || ActivityTaskManagerService.this.mShuttingDown) ? false : true;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return z;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void showSystemReadyErrorDialogsIfNeeded() {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (AppGlobals.getPackageManager().hasSystemUidErrors()) {
                            Slog.e(ActivityTaskManagerService.TAG, "UIDs on the system are inconsistent, you need to wipe your data partition or your device will be unstable.");
                            ActivityTaskManagerService.this.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$LocalService$$ExternalSyntheticLambda0
                                @Override // java.lang.Runnable
                                public final void run() {
                                    ActivityTaskManagerService.LocalService.this.m7837xa8c46072();
                                }
                            });
                        }
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (RemoteException e) {
                }
                if (!Build.isBuildConsistent()) {
                    Slog.e(ActivityTaskManagerService.TAG, "Build fingerprint is not consistent, warning user");
                    ActivityTaskManagerService.this.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$LocalService$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            ActivityTaskManagerService.LocalService.this.m7838x9c53e4b3();
                        }
                    });
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$showSystemReadyErrorDialogsIfNeeded$0$com-android-server-wm-ActivityTaskManagerService$LocalService  reason: not valid java name */
        public /* synthetic */ void m7837xa8c46072() {
            if (ActivityTaskManagerService.this.mShowDialogs) {
                AlertDialog d = new BaseErrorDialog(ActivityTaskManagerService.this.mUiContext);
                d.getWindow().setType(2010);
                d.setCancelable(false);
                d.setTitle(ActivityTaskManagerService.this.mUiContext.getText(17039665));
                d.setMessage(ActivityTaskManagerService.this.mUiContext.getText(17041617));
                d.setButton(-1, ActivityTaskManagerService.this.mUiContext.getText(17039370), ActivityTaskManagerService.this.mUiHandler.obtainMessage(1, d));
                d.show();
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$showSystemReadyErrorDialogsIfNeeded$1$com-android-server-wm-ActivityTaskManagerService$LocalService  reason: not valid java name */
        public /* synthetic */ void m7838x9c53e4b3() {
            if (ActivityTaskManagerService.this.mShowDialogs) {
                AlertDialog d = new BaseErrorDialog(ActivityTaskManagerService.this.mUiContext);
                d.getWindow().setType(2010);
                d.setCancelable(false);
                d.setTitle(ActivityTaskManagerService.this.mUiContext.getText(17039665));
                d.setMessage(ActivityTaskManagerService.this.mUiContext.getText(17041616));
                d.setButton(-1, ActivityTaskManagerService.this.mUiContext.getText(17039370), ActivityTaskManagerService.this.mUiHandler.obtainMessage(1, d));
                d.show();
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onProcessMapped(int pid, WindowProcessController proc) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mProcessMap.put(pid, proc);
                    ITranWindowManagerService.Instance().onProcessMapped(pid, proc);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onProcessUnMapped(int pid) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mProcessMap.remove(pid);
                    ITranWindowManagerService.Instance().onProcessUnMapped(pid);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onPackageDataCleared(String name, int userId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mCompatModePackages.handlePackageDataClearedLocked(name);
                    ActivityTaskManagerService.this.mAppWarnings.onPackageDataCleared(name);
                    ActivityTaskManagerService.this.mPackageConfigPersister.onPackageDataCleared(name, userId);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onPackageUninstalled(String name, int userId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mAppWarnings.onPackageUninstalled(name);
                    ActivityTaskManagerService.this.mCompatModePackages.handlePackageUninstalledLocked(name);
                    ActivityTaskManagerService.this.mPackageConfigPersister.onPackageUninstall(name, userId);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onPackageAdded(String name, boolean replacing) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mCompatModePackages.handlePackageAddedLocked(name, replacing);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onPackageReplaced(ApplicationInfo aInfo) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRootWindowContainer.updateActivityApplicationInfo(aInfo);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public CompatibilityInfo compatibilityInfoForPackage(ApplicationInfo ai) {
            CompatibilityInfo compatibilityInfoForPackageLocked;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    compatibilityInfoForPackageLocked = ActivityTaskManagerService.this.compatibilityInfoForPackageLocked(ai);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return compatibilityInfoForPackageLocked;
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void sendActivityResult(int callingUid, IBinder activityToken, String resultWho, int requestCode, int resultCode, Intent data) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        ActivityRecord r = ActivityRecord.isInRootTaskLocked(activityToken);
                        if (r != null && r.getRootTask() != null) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            NeededUriGrants dataGrants = ActivityTaskManagerService.this.collectGrants(data, r);
                            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                                try {
                                    WindowManagerService.boostPriorityForLockedSection();
                                    r.sendResult(callingUid, resultWho, requestCode, resultCode, data, dataGrants);
                                } finally {
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                }
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                    } catch (Throwable th) {
                        th = th;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void clearPendingResultForActivity(IBinder activityToken, WeakReference<PendingIntentRecord> pir) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInRootTaskLocked(activityToken);
                    if (r != null && r.pendingResults != null) {
                        r.pendingResults.remove(pir);
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public ComponentName getActivityName(IBinder activityToken) {
            ComponentName component;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInRootTaskLocked(activityToken);
                    component = r != null ? r.intent.getComponent() : null;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return component;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public ActivityTaskManagerInternal.ActivityTokens getAttachedNonFinishingActivityForTask(int taskId, IBinder token) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        Task task = ActivityTaskManagerService.this.mRootWindowContainer.anyTaskForId(taskId, 0);
                        if (task == null) {
                            Slog.w(ActivityTaskManagerService.TAG, "getApplicationThreadForTopActivity failed: Requested task not found");
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return null;
                        }
                        final List<ActivityRecord> list = new ArrayList<>();
                        task.forAllActivities(new Consumer() { // from class: com.android.server.wm.ActivityTaskManagerService$LocalService$$ExternalSyntheticLambda4
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                ActivityTaskManagerService.LocalService.lambda$getAttachedNonFinishingActivityForTask$2(list, (ActivityRecord) obj);
                            }
                        });
                        if (list.size() <= 0) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return null;
                        } else if (token == null && list.get(0).attachedToProcess()) {
                            ActivityRecord topRecord = list.get(0);
                            ActivityTaskManagerInternal.ActivityTokens activityTokens = new ActivityTaskManagerInternal.ActivityTokens(topRecord.token, topRecord.assistToken, topRecord.app.getThread(), topRecord.shareableActivityToken, topRecord.getUid());
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return activityTokens;
                        } else {
                            for (int i = 0; i < list.size(); i++) {
                                ActivityRecord record = list.get(i);
                                if (record.shareableActivityToken == token && record.attachedToProcess()) {
                                    ActivityTaskManagerInternal.ActivityTokens activityTokens2 = new ActivityTaskManagerInternal.ActivityTokens(record.token, record.assistToken, record.app.getThread(), record.shareableActivityToken, record.getUid());
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return activityTokens2;
                                }
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return null;
                        }
                    } catch (Throwable th) {
                        th = th;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$getAttachedNonFinishingActivityForTask$2(List list, ActivityRecord r) {
            if (!r.finishing) {
                list.add(r);
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public IIntentSender getIntentSender(int type, String packageName, String featureId, int callingUid, int userId, IBinder token, String resultWho, int requestCode, Intent[] intents, String[] resolvedTypes, int flags, Bundle bOptions) {
            IIntentSender intentSenderLocked;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    intentSenderLocked = ActivityTaskManagerService.this.getIntentSenderLocked(type, packageName, featureId, callingUid, userId, token, resultWho, requestCode, intents, resolvedTypes, flags, bOptions);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return intentSenderLocked;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public ActivityServiceConnectionsHolder getServiceConnectionsHolder(IBinder token) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                    if (r == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return null;
                    }
                    if (r.mServiceConnectionsHolder == null) {
                        r.mServiceConnectionsHolder = new ActivityServiceConnectionsHolder(ActivityTaskManagerService.this, r);
                    }
                    ActivityServiceConnectionsHolder activityServiceConnectionsHolder = r.mServiceConnectionsHolder;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return activityServiceConnectionsHolder;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public Intent getHomeIntent() {
            Intent homeIntent;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    homeIntent = ActivityTaskManagerService.this.getHomeIntent();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return homeIntent;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean startHomeActivity(int userId, String reason) {
            boolean startHomeOnDisplay;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    startHomeOnDisplay = ActivityTaskManagerService.this.mRootWindowContainer.startHomeOnDisplay(userId, reason, 0);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return startHomeOnDisplay;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean startHomeOnDisplay(int userId, String reason, int displayId, boolean allowInstrumenting, boolean fromHomeKey) {
            boolean startHomeOnDisplay;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    startHomeOnDisplay = ActivityTaskManagerService.this.mRootWindowContainer.startHomeOnDisplay(userId, reason, displayId, allowInstrumenting, fromHomeKey);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return startHomeOnDisplay;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean startHomeOnAllDisplays(int userId, String reason) {
            boolean startHomeOnAllDisplays;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    startHomeOnAllDisplays = ActivityTaskManagerService.this.mRootWindowContainer.startHomeOnAllDisplays(userId, reason);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return startHomeOnAllDisplays;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean isFactoryTestProcess(WindowProcessController wpc) {
            synchronized (ActivityTaskManagerService.this.mGlobalLockWithoutBoost) {
                boolean z = false;
                if (ActivityTaskManagerService.this.mFactoryTest == 0) {
                    return false;
                }
                if (ActivityTaskManagerService.this.mFactoryTest == 1 && ActivityTaskManagerService.this.mTopComponent != null && wpc.mName.equals(ActivityTaskManagerService.this.mTopComponent.getPackageName())) {
                    return true;
                }
                if (ActivityTaskManagerService.this.mFactoryTest == 2 && (wpc.mInfo.flags & 16) != 0) {
                    z = true;
                }
                return z;
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void updateTopComponentForFactoryTest() {
            final CharSequence errorMsg;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ActivityTaskManagerService.this.mFactoryTest != 1) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    ResolveInfo ri = ActivityTaskManagerService.this.mContext.getPackageManager().resolveActivity(new Intent("android.intent.action.FACTORY_TEST"), 1024);
                    if (ri != null) {
                        ActivityInfo ai = ri.activityInfo;
                        ApplicationInfo app = ai.applicationInfo;
                        if ((1 & app.flags) != 0) {
                            ActivityTaskManagerService.this.mTopAction = "android.intent.action.FACTORY_TEST";
                            ActivityTaskManagerService.this.mTopData = null;
                            ActivityTaskManagerService.this.mTopComponent = new ComponentName(app.packageName, ai.name);
                            errorMsg = null;
                        } else {
                            errorMsg = ActivityTaskManagerService.this.mContext.getResources().getText(17040319);
                        }
                    } else {
                        errorMsg = ActivityTaskManagerService.this.mContext.getResources().getText(17040318);
                    }
                    if (errorMsg == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    ActivityTaskManagerService.this.mTopAction = null;
                    ActivityTaskManagerService.this.mTopData = null;
                    ActivityTaskManagerService.this.mTopComponent = null;
                    ActivityTaskManagerService.this.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerService$LocalService$$ExternalSyntheticLambda3
                        @Override // java.lang.Runnable
                        public final void run() {
                            ActivityTaskManagerService.LocalService.this.m7839xb65fec87(errorMsg);
                        }
                    });
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$updateTopComponentForFactoryTest$3$com-android-server-wm-ActivityTaskManagerService$LocalService  reason: not valid java name */
        public /* synthetic */ void m7839xb65fec87(CharSequence errorMsg) {
            Dialog d = new FactoryErrorDialog(ActivityTaskManagerService.this.mUiContext, errorMsg);
            d.show();
            ActivityTaskManagerService.this.mAmInternal.ensureBootCompleted();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void handleAppDied(WindowProcessController wpc, boolean restarting, Runnable finishInstrumentationCallback) {
            synchronized (ActivityTaskManagerService.this.mGlobalLockWithoutBoost) {
                ActivityTaskManagerService.this.mTaskSupervisor.beginDeferResume();
                boolean hasVisibleActivities = wpc.handleAppDied();
                ActivityTaskManagerService.this.mTaskSupervisor.endDeferResume();
                if (!restarting && hasVisibleActivities) {
                    ActivityTaskManagerService.this.deferWindowLayout();
                    if (!ActivityTaskManagerService.this.mRootWindowContainer.resumeFocusedTasksTopActivities()) {
                        ActivityTaskManagerService.this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
                    }
                    ActivityTaskManagerService.this.continueWindowLayout();
                }
            }
            if (wpc.isInstrumenting()) {
                finishInstrumentationCallback.run();
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void closeSystemDialogs(String reason) {
            ActivityTaskManagerService.enforceNotIsolatedCaller("closeSystemDialogs");
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            if (!checkCanCloseSystemDialogs(pid, uid, null)) {
                return;
            }
            long origId = Binder.clearCallingIdentity();
            try {
                synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (uid >= 10000) {
                        WindowProcessController proc = ActivityTaskManagerService.this.mProcessMap.getProcess(pid);
                        if (!proc.isPerceptible()) {
                            Slog.w(ActivityTaskManagerService.TAG, "Ignoring closeSystemDialogs " + reason + " from background process " + proc);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                    }
                    ActivityTaskManagerService.this.mWindowManager.closeSystemDialogs(reason);
                    ActivityTaskManagerService.this.mRootWindowContainer.closeSystemDialogActivities(reason);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    ActivityTaskManagerService.this.mAmInternal.broadcastCloseSystemDialogs(reason);
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void cleanupDisabledPackageComponents(String packageName, Set<String> disabledClasses, int userId, boolean booted) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ActivityTaskManagerService.this.mRootWindowContainer.finishDisabledPackageActivities(packageName, disabledClasses, true, false, userId, false) && booted) {
                        ActivityTaskManagerService.this.mRootWindowContainer.resumeFocusedTasksTopActivities();
                        ActivityTaskManagerService.this.mTaskSupervisor.scheduleIdle();
                    }
                    ActivityTaskManagerService.this.getRecentTasks().cleanupDisabledPackageTasksLocked(packageName, disabledClasses, userId);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean onForceStopPackage(String packageName, boolean doit, boolean evenPersistent, int userId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ActivityTaskManagerService.this.mRootWindowContainer == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    boolean finishDisabledPackageActivities = ActivityTaskManagerService.this.mRootWindowContainer.finishDisabledPackageActivities(packageName, null, doit, evenPersistent, userId, true);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return finishDisabledPackageActivities;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void resumeTopActivities(boolean scheduleIdle) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRootWindowContainer.resumeFocusedTasksTopActivities();
                    if (scheduleIdle) {
                        ActivityTaskManagerService.this.mTaskSupervisor.scheduleIdle();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void preBindApplication(WindowProcessController wpc) {
            synchronized (ActivityTaskManagerService.this.mGlobalLockWithoutBoost) {
                ActivityTaskManagerService.this.mTaskSupervisor.getActivityMetricsLogger().notifyBindApplication(wpc.mInfo);
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean attachApplication(WindowProcessController wpc) throws RemoteException {
            boolean attachApplication;
            synchronized (ActivityTaskManagerService.this.mGlobalLockWithoutBoost) {
                if (Trace.isTagEnabled(32L)) {
                    Trace.traceBegin(32L, "attachApplication:" + wpc.mName);
                }
                attachApplication = ActivityTaskManagerService.this.mRootWindowContainer.attachApplication(wpc);
                Trace.traceEnd(32L);
            }
            return attachApplication;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void notifyLockedProfile(int userId, int currentUserId) {
            try {
                if (!AppGlobals.getPackageManager().isUidPrivileged(Binder.getCallingUid())) {
                    throw new SecurityException("Only privileged app can call notifyLockedProfile");
                }
                synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        long ident = Binder.clearCallingIdentity();
                        if (ActivityTaskManagerService.this.mAmInternal.shouldConfirmCredentials(userId)) {
                            if (ActivityTaskManagerService.this.mKeyguardController.isKeyguardLocked(0)) {
                                startHomeActivity(currentUserId, "notifyLockedProfile");
                            }
                            ActivityTaskManagerService.this.mRootWindowContainer.lockAllProfileTasks(userId);
                        }
                        Binder.restoreCallingIdentity(ident);
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (RemoteException ex) {
                throw new SecurityException("Fail to check is caller a privileged app", ex);
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void startConfirmDeviceCredentialIntent(Intent intent, Bundle options) {
            ActivityTaskManagerService.enforceTaskPermission("startConfirmDeviceCredentialIntent");
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    long ident = Binder.clearCallingIdentity();
                    intent.addFlags(276824064);
                    ActivityOptions activityOptions = options != null ? new ActivityOptions(options) : ActivityOptions.makeBasic();
                    ActivityTaskManagerService.this.mContext.startActivityAsUser(intent, activityOptions.toBundle(), UserHandle.CURRENT);
                    Binder.restoreCallingIdentity(ident);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void writeActivitiesToProto(ProtoOutputStream proto) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRootWindowContainer.dumpDebug(proto, 1146756268034L, 0);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void dump(String cmd, FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, boolean dumpClient, String dumpPackage) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (!ActivityTaskManagerService.DUMP_ACTIVITIES_CMD.equals(cmd) && !ActivityTaskManagerService.DUMP_ACTIVITIES_SHORT_CMD.equals(cmd)) {
                            if (ActivityTaskManagerService.DUMP_LASTANR_CMD.equals(cmd)) {
                                ActivityTaskManagerService.this.dumpLastANRLocked(pw);
                            } else if (ActivityTaskManagerService.DUMP_LASTANR_TRACES_CMD.equals(cmd)) {
                                ActivityTaskManagerService.this.dumpLastANRTracesLocked(pw);
                            } else if (ActivityTaskManagerService.DUMP_STARTER_CMD.equals(cmd)) {
                                ActivityTaskManagerService.this.dumpActivityStarterLocked(pw, dumpPackage);
                            } else if (ActivityTaskManagerService.DUMP_CONTAINERS_CMD.equals(cmd)) {
                                ActivityTaskManagerService.this.dumpActivityContainersLocked(pw);
                            } else {
                                if (!ActivityTaskManagerService.DUMP_RECENTS_CMD.equals(cmd) && !ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD.equals(cmd)) {
                                    if (ActivityTaskManagerService.DUMP_TOP_RESUMED_ACTIVITY.equals(cmd)) {
                                        ActivityTaskManagerService.this.dumpTopResumedActivityLocked(pw);
                                    }
                                }
                                if (ActivityTaskManagerService.this.getRecentTasks() != null) {
                                    ActivityTaskManagerService.this.getRecentTasks().dump(pw, dumpAll, dumpPackage);
                                }
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                        }
                        ActivityTaskManagerService.this.dumpActivitiesLocked(fd, pw, args, opti, dumpAll, dumpClient, dumpPackage);
                        WindowManagerService.resetPriorityAfterLockedSection();
                    } catch (Throwable th) {
                        th = th;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        /* JADX WARN: Code restructure failed: missing block: B:115:0x0396, code lost:
            r0 = th;
         */
        /* JADX WARN: Code restructure failed: missing block: B:90:0x02c7, code lost:
            r15.println();
            r5 = false;
         */
        /* JADX WARN: Removed duplicated region for block: B:119:0x01a3 A[EXC_TOP_SPLITTER, SYNTHETIC] */
        /* JADX WARN: Removed duplicated region for block: B:24:0x0067 A[Catch: all -> 0x0384, TryCatch #3 {all -> 0x0384, blocks: (B:16:0x0034, B:18:0x0051, B:21:0x0059, B:24:0x0067, B:25:0x006c, B:27:0x0088, B:30:0x0090, B:32:0x009c, B:33:0x00b4, B:36:0x00bc, B:39:0x00ca, B:40:0x00cf, B:42:0x00eb, B:44:0x0112, B:47:0x011c, B:48:0x0134, B:50:0x0142, B:51:0x0153, B:53:0x0159, B:55:0x0171, B:59:0x017a, B:60:0x0180, B:65:0x01df), top: B:123:0x0034 }] */
        /* JADX WARN: Removed duplicated region for block: B:39:0x00ca A[Catch: all -> 0x0384, TryCatch #3 {all -> 0x0384, blocks: (B:16:0x0034, B:18:0x0051, B:21:0x0059, B:24:0x0067, B:25:0x006c, B:27:0x0088, B:30:0x0090, B:32:0x009c, B:33:0x00b4, B:36:0x00bc, B:39:0x00ca, B:40:0x00cf, B:42:0x00eb, B:44:0x0112, B:47:0x011c, B:48:0x0134, B:50:0x0142, B:51:0x0153, B:53:0x0159, B:55:0x0171, B:59:0x017a, B:60:0x0180, B:65:0x01df), top: B:123:0x0034 }] */
        /* JADX WARN: Removed duplicated region for block: B:42:0x00eb A[Catch: all -> 0x0384, TryCatch #3 {all -> 0x0384, blocks: (B:16:0x0034, B:18:0x0051, B:21:0x0059, B:24:0x0067, B:25:0x006c, B:27:0x0088, B:30:0x0090, B:32:0x009c, B:33:0x00b4, B:36:0x00bc, B:39:0x00ca, B:40:0x00cf, B:42:0x00eb, B:44:0x0112, B:47:0x011c, B:48:0x0134, B:50:0x0142, B:51:0x0153, B:53:0x0159, B:55:0x0171, B:59:0x017a, B:60:0x0180, B:65:0x01df), top: B:123:0x0034 }] */
        /* JADX WARN: Removed duplicated region for block: B:44:0x0112 A[Catch: all -> 0x0384, TryCatch #3 {all -> 0x0384, blocks: (B:16:0x0034, B:18:0x0051, B:21:0x0059, B:24:0x0067, B:25:0x006c, B:27:0x0088, B:30:0x0090, B:32:0x009c, B:33:0x00b4, B:36:0x00bc, B:39:0x00ca, B:40:0x00cf, B:42:0x00eb, B:44:0x0112, B:47:0x011c, B:48:0x0134, B:50:0x0142, B:51:0x0153, B:53:0x0159, B:55:0x0171, B:59:0x017a, B:60:0x0180, B:65:0x01df), top: B:123:0x0034 }] */
        /* JADX WARN: Removed duplicated region for block: B:71:0x0275  */
        /* JADX WARN: Removed duplicated region for block: B:74:0x027d A[Catch: all -> 0x0380, TryCatch #0 {all -> 0x0380, blocks: (B:68:0x024a, B:72:0x0277, B:74:0x027d, B:75:0x0287, B:78:0x0293, B:80:0x029d, B:81:0x02a8, B:84:0x02b0), top: B:117:0x024a }] */
        /* JADX WARN: Removed duplicated region for block: B:77:0x0291  */
        /* JADX WARN: Removed duplicated region for block: B:97:0x0311  */
        /* JADX WARN: Removed duplicated region for block: B:99:0x0315 A[Catch: all -> 0x0396, TryCatch #4 {all -> 0x0396, blocks: (B:94:0x0306, B:90:0x02c7, B:92:0x02cd, B:93:0x02d3, B:112:0x0391, B:95:0x0309, B:99:0x0315, B:101:0x031b, B:102:0x0343, B:103:0x037b), top: B:125:0x02c7 }] */
        @Override // com.android.server.wm.ActivityTaskManagerInternal
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public boolean dumpForProcesses(FileDescriptor fd, PrintWriter pw, boolean dumpAll, String dumpPackage, int dumpAppId, boolean needSep, boolean testPssMode, int wakefulness) {
            boolean needSep2;
            int j;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ActivityTaskManagerService.this.mHomeProcess != null) {
                        if (dumpPackage != null) {
                            try {
                                if (ActivityTaskManagerService.this.mHomeProcess.mPkgList.contains(dumpPackage)) {
                                }
                            } catch (Throwable th) {
                                th = th;
                                WindowManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        if (!needSep) {
                            needSep2 = needSep;
                        } else {
                            pw.println();
                            needSep2 = false;
                        }
                        try {
                            pw.println("  mHomeProcess: " + ActivityTaskManagerService.this.mHomeProcess);
                            if (ActivityTaskManagerService.this.mPreviousProcess != null && (dumpPackage == null || ActivityTaskManagerService.this.mPreviousProcess.mPkgList.contains(dumpPackage))) {
                                if (needSep2) {
                                    pw.println();
                                    needSep2 = false;
                                }
                                pw.println("  mPreviousProcess: " + ActivityTaskManagerService.this.mPreviousProcess);
                            }
                            if (dumpAll && (ActivityTaskManagerService.this.mPreviousProcess == null || dumpPackage == null || ActivityTaskManagerService.this.mPreviousProcess.mPkgList.contains(dumpPackage))) {
                                StringBuilder sb = new StringBuilder(128);
                                sb.append("  mPreviousProcessVisibleTime: ");
                                TimeUtils.formatDuration(ActivityTaskManagerService.this.mPreviousProcessVisibleTime, sb);
                                pw.println(sb);
                            }
                            if (ActivityTaskManagerService.this.mHeavyWeightProcess != null && (dumpPackage == null || ActivityTaskManagerService.this.mHeavyWeightProcess.mPkgList.contains(dumpPackage))) {
                                if (needSep2) {
                                    pw.println();
                                    needSep2 = false;
                                }
                                pw.println("  mHeavyWeightProcess: " + ActivityTaskManagerService.this.mHeavyWeightProcess);
                            }
                            if (dumpPackage == null) {
                                pw.println("  mGlobalConfiguration: " + ActivityTaskManagerService.this.getGlobalConfiguration());
                                ActivityTaskManagerService.this.mRootWindowContainer.dumpDisplayConfigs(pw, "  ");
                            }
                            if (dumpAll) {
                                Task topFocusedRootTask = ActivityTaskManagerService.this.getTopDisplayFocusedRootTask();
                                if (dumpPackage == null && topFocusedRootTask != null) {
                                    pw.println("  mConfigWillChange: " + topFocusedRootTask.mConfigWillChange);
                                }
                                if (ActivityTaskManagerService.this.mCompatModePackages.getPackages().size() > 0) {
                                    boolean printed = false;
                                    for (Map.Entry<String, Integer> entry : ActivityTaskManagerService.this.mCompatModePackages.getPackages().entrySet()) {
                                        String pkg = entry.getKey();
                                        int mode = entry.getValue().intValue();
                                        if (dumpPackage == null || dumpPackage.equals(pkg)) {
                                            if (!printed) {
                                                pw.println("  mScreenCompatPackages:");
                                                printed = true;
                                            }
                                            pw.println("    " + pkg + ": " + mode);
                                        }
                                    }
                                }
                            }
                            if (dumpPackage != null) {
                                try {
                                    pw.println("  mWakefulness=" + PowerManagerInternal.wakefulnessToString(wakefulness));
                                    pw.println("  mSleepTokens=" + ActivityTaskManagerService.this.mRootWindowContainer.mSleepTokens);
                                    if (ActivityTaskManagerService.this.mRunningVoice != null) {
                                        pw.println("  mRunningVoice=" + ActivityTaskManagerService.this.mRunningVoice);
                                        pw.println("  mVoiceWakeLock" + ActivityTaskManagerService.this.mVoiceWakeLock);
                                    }
                                    pw.println("  mSleeping=" + ActivityTaskManagerService.this.mSleeping);
                                    try {
                                        pw.println("  mShuttingDown=" + ActivityTaskManagerService.this.mShuttingDown + " mTestPssMode=" + testPssMode);
                                        pw.println("  mVrController=" + ActivityTaskManagerService.this.mVrController);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    throw th;
                                }
                            }
                            if (ActivityTaskManagerService.this.mCurAppTimeTracker != null) {
                                ActivityTaskManagerService.this.mCurAppTimeTracker.dumpWithHeader(pw, "  ", true);
                            }
                            if (ActivityTaskManagerService.this.mAllowAppSwitchUids.size() <= 0) {
                                boolean printed2 = false;
                                for (int i = 0; i < ActivityTaskManagerService.this.mAllowAppSwitchUids.size(); i++) {
                                    ArrayMap<String, Integer> types = ActivityTaskManagerService.this.mAllowAppSwitchUids.valueAt(i);
                                    while (j < types.size()) {
                                        j = (dumpPackage == null || UserHandle.getAppId(types.valueAt(j).intValue()) == dumpAppId) ? 0 : j + 1;
                                        if (!printed2) {
                                            pw.println("  mAllowAppSwitchUids:");
                                            printed2 = true;
                                        }
                                        pw.print("    User ");
                                        pw.print(ActivityTaskManagerService.this.mAllowAppSwitchUids.keyAt(i));
                                        pw.print(": Type ");
                                        pw.print(types.keyAt(j));
                                        pw.print(" = ");
                                        UserHandle.formatUid(pw, types.valueAt(j).intValue());
                                        pw.println();
                                    }
                                }
                            }
                            if (dumpPackage == null) {
                                if (ActivityTaskManagerService.this.mController != null) {
                                    pw.println("  mController=" + ActivityTaskManagerService.this.mController + " mControllerIsAMonkey=" + ActivityTaskManagerService.this.mControllerIsAMonkey);
                                }
                                pw.println("  mGoingToSleepWakeLock=" + ActivityTaskManagerService.this.mTaskSupervisor.mGoingToSleepWakeLock);
                                pw.println("  mLaunchingActivityWakeLock=" + ActivityTaskManagerService.this.mTaskSupervisor.mLaunchingActivityWakeLock);
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return needSep2;
                        } catch (Throwable th4) {
                            th = th4;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    needSep2 = needSep;
                    if (ActivityTaskManagerService.this.mPreviousProcess != null) {
                        if (needSep2) {
                        }
                        pw.println("  mPreviousProcess: " + ActivityTaskManagerService.this.mPreviousProcess);
                    }
                    if (dumpAll) {
                        StringBuilder sb2 = new StringBuilder(128);
                        sb2.append("  mPreviousProcessVisibleTime: ");
                        TimeUtils.formatDuration(ActivityTaskManagerService.this.mPreviousProcessVisibleTime, sb2);
                        pw.println(sb2);
                    }
                    if (ActivityTaskManagerService.this.mHeavyWeightProcess != null) {
                        if (needSep2) {
                        }
                        pw.println("  mHeavyWeightProcess: " + ActivityTaskManagerService.this.mHeavyWeightProcess);
                    }
                    if (dumpPackage == null) {
                    }
                    if (dumpAll) {
                    }
                    if (dumpPackage != null) {
                    }
                    if (ActivityTaskManagerService.this.mCurAppTimeTracker != null) {
                    }
                    if (ActivityTaskManagerService.this.mAllowAppSwitchUids.size() <= 0) {
                    }
                    if (dumpPackage == null) {
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return needSep2;
                } catch (Throwable th5) {
                    th = th5;
                }
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void writeProcessesToProto(ProtoOutputStream proto, String dumpPackage, int wakeFullness, boolean testPssMode) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (dumpPackage == null) {
                        ActivityTaskManagerService.this.getGlobalConfiguration().dumpDebug(proto, 1146756268051L);
                        Task topFocusedRootTask = ActivityTaskManagerService.this.getTopDisplayFocusedRootTask();
                        if (topFocusedRootTask != null) {
                            proto.write(1133871366165L, topFocusedRootTask.mConfigWillChange);
                        }
                        ActivityTaskManagerService.this.writeSleepStateToProto(proto, wakeFullness, testPssMode);
                        if (ActivityTaskManagerService.this.mRunningVoice != null) {
                            long vrToken = proto.start(1146756268060L);
                            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, ActivityTaskManagerService.this.mRunningVoice.toString());
                            ActivityTaskManagerService.this.mVoiceWakeLock.dumpDebug(proto, 1146756268034L);
                            proto.end(vrToken);
                        }
                        ActivityTaskManagerService.this.mVrController.dumpDebug(proto, 1146756268061L);
                        if (ActivityTaskManagerService.this.mController != null) {
                            long token = proto.start(1146756268069L);
                            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, ActivityTaskManagerService.this.mController.toString());
                            proto.write(1133871366146L, ActivityTaskManagerService.this.mControllerIsAMonkey);
                            proto.end(token);
                        }
                        ActivityTaskManagerService.this.mTaskSupervisor.mGoingToSleepWakeLock.dumpDebug(proto, 1146756268079L);
                        ActivityTaskManagerService.this.mTaskSupervisor.mLaunchingActivityWakeLock.dumpDebug(proto, 1146756268080L);
                    }
                    if (ActivityTaskManagerService.this.mHomeProcess != null && (dumpPackage == null || ActivityTaskManagerService.this.mHomeProcess.mPkgList.contains(dumpPackage))) {
                        ActivityTaskManagerService.this.mHomeProcess.dumpDebug(proto, 1146756268047L);
                    }
                    if (ActivityTaskManagerService.this.mPreviousProcess != null && (dumpPackage == null || ActivityTaskManagerService.this.mPreviousProcess.mPkgList.contains(dumpPackage))) {
                        ActivityTaskManagerService.this.mPreviousProcess.dumpDebug(proto, 1146756268048L);
                        proto.write(1112396529681L, ActivityTaskManagerService.this.mPreviousProcessVisibleTime);
                    }
                    if (ActivityTaskManagerService.this.mHeavyWeightProcess != null && (dumpPackage == null || ActivityTaskManagerService.this.mHeavyWeightProcess.mPkgList.contains(dumpPackage))) {
                        ActivityTaskManagerService.this.mHeavyWeightProcess.dumpDebug(proto, 1146756268050L);
                    }
                    for (Map.Entry<String, Integer> entry : ActivityTaskManagerService.this.mCompatModePackages.getPackages().entrySet()) {
                        String pkg = entry.getKey();
                        int mode = entry.getValue().intValue();
                        if (dumpPackage == null || dumpPackage.equals(pkg)) {
                            long compatToken = proto.start(2246267895830L);
                            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, pkg);
                            proto.write(1120986464258L, mode);
                            proto.end(compatToken);
                        }
                    }
                    if (ActivityTaskManagerService.this.mCurAppTimeTracker != null) {
                        ActivityTaskManagerService.this.mCurAppTimeTracker.dumpDebug(proto, 1146756268063L, true);
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean dumpActivity(FileDescriptor fd, PrintWriter pw, String name, String[] args, int opti, boolean dumpAll, boolean dumpVisibleRootTasksOnly, boolean dumpFocusedRootTaskOnly, int userId) {
            return ActivityTaskManagerService.this.dumpActivity(fd, pw, name, args, opti, dumpAll, dumpVisibleRootTasksOnly, dumpFocusedRootTaskOnly, userId);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void dumpForOom(PrintWriter pw) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    pw.println("  mHomeProcess: " + ActivityTaskManagerService.this.mHomeProcess);
                    pw.println("  mPreviousProcess: " + ActivityTaskManagerService.this.mPreviousProcess);
                    if (ActivityTaskManagerService.this.mHeavyWeightProcess != null) {
                        pw.println("  mHeavyWeightProcess: " + ActivityTaskManagerService.this.mHeavyWeightProcess);
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean canGcNow() {
            boolean z;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    z = isSleeping() || ActivityTaskManagerService.this.mRootWindowContainer.allResumedActivitiesIdle();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return z;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public WindowProcessController getTopApp() {
            return ActivityTaskManagerService.this.mTopApp;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean inTopVisiblePackages(String packageName) {
            boolean inTopVisiblePackages;
            if (packageName == null) {
                return false;
            }
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    inTopVisiblePackages = ActivityTaskManagerService.this.mRootWindowContainer.inTopVisiblePackages(packageName);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return inTopVisiblePackages;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public String getTopActivity() {
            String topActivity;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    topActivity = ActivityTaskManagerService.this.mRootWindowContainer.getTopActivity();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return topActivity;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void scheduleDestroyAllActivities(String reason) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRootWindowContainer.scheduleDestroyAllActivities(reason);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void removeUser(int userId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRootWindowContainer.removeUser(userId);
                    ActivityTaskManagerService.this.mPackageConfigPersister.removeUser(userId);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean switchUser(int userId, UserState userState) {
            boolean switchUser;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    switchUser = ActivityTaskManagerService.this.mRootWindowContainer.switchUser(userId, userState);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return switchUser;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onHandleAppCrash(WindowProcessController wpc) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRootWindowContainer.handleAppCrash(wpc);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public int finishTopCrashedActivities(WindowProcessController crashedApp, String reason) {
            int finishTopCrashedActivities;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    finishTopCrashedActivities = ActivityTaskManagerService.this.mRootWindowContainer.finishTopCrashedActivities(crashedApp, reason);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return finishTopCrashedActivities;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onUidActive(int uid, int procState) {
            ActivityTaskManagerService.this.mActiveUids.onUidActive(uid, procState);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onUidInactive(int uid) {
            ActivityTaskManagerService.this.mActiveUids.onUidInactive(uid);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onUidProcStateChanged(int uid, int procState) {
            ActivityTaskManagerService.this.mActiveUids.onUidProcStateChanged(uid, procState);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean handleAppCrashInActivityController(String processName, int pid, String shortMsg, String longMsg, long timeMillis, String stackTrace, Runnable killCrashingAppCallback) {
            Runnable targetRunnable = null;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ActivityTaskManagerService.this.mController == null) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    try {
                        if (!ActivityTaskManagerService.this.mController.appCrashed(processName, pid, shortMsg, longMsg, timeMillis, stackTrace)) {
                            targetRunnable = killCrashingAppCallback;
                        }
                    } catch (RemoteException e) {
                        ActivityTaskManagerService.this.mController = null;
                        Watchdog.getInstance().setActivityController(null);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (targetRunnable != null) {
                        targetRunnable.run();
                        return true;
                    }
                    return false;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void removeRecentTasksByPackageName(String packageName, int userId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRecentTasks.removeTasksByPackageName(packageName, userId);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void removeRecentTasksByPackageName(String packageName, int userId, boolean killProcess, boolean removeFromRecents, String reason) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRecentTasks.removeTasksByPackageName(packageName, userId, killProcess, removeFromRecents, reason);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void cleanupRecentTasksForUser(int userId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRecentTasks.cleanupLocked(userId);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void loadRecentTasksForUser(int userId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRecentTasks.loadUserRecentsLocked(userId);
                    ActivityTaskManagerService.this.mPackageConfigPersister.loadUserPackages(userId);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void onPackagesSuspendedChanged(String[] packages, boolean suspended, int userId) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mRecentTasks.onPackagesSuspendedChanged(packages, suspended, userId);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void flushRecentTasks() {
            ActivityTaskManagerService.this.mRecentTasks.flush();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void clearLockedTasks(String reason) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.getLockTaskController().clearLockedTasks(reason);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void updateUserConfiguration() {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    Configuration configuration = new Configuration(ActivityTaskManagerService.this.getGlobalConfiguration());
                    int currentUserId = ActivityTaskManagerService.this.mAmInternal.getCurrentUserId();
                    Settings.System.adjustConfigurationForUser(ActivityTaskManagerService.this.mContext.getContentResolver(), configuration, currentUserId, Settings.System.canWrite(ActivityTaskManagerService.this.mContext));
                    ActivityTaskManagerService.this.updateConfigurationLocked(configuration, null, false, false, currentUserId, false);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean canShowErrorDialogs() {
            boolean z;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    z = false;
                    if (ActivityTaskManagerService.this.mShowDialogs && !ActivityTaskManagerService.this.mSleeping && !ActivityTaskManagerService.this.mShuttingDown && !ActivityTaskManagerService.this.mKeyguardController.isKeyguardOrAodShowing(0)) {
                        ActivityTaskManagerService activityTaskManagerService = ActivityTaskManagerService.this;
                        if (!activityTaskManagerService.hasUserRestriction("no_system_error_dialogs", activityTaskManagerService.mAmInternal.getCurrentUserId()) && (!UserManager.isDeviceInDemoMode(ActivityTaskManagerService.this.mContext) || !ActivityTaskManagerService.this.mAmInternal.getCurrentUser().isDemo())) {
                            z = true;
                        }
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return z;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setProfileApp(String profileApp) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mProfileApp = profileApp;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setProfileProc(WindowProcessController wpc) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mProfileProc = wpc;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setProfilerInfo(ProfilerInfo profilerInfo) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mProfilerInfo = profilerInfo;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public ActivityMetricsLaunchObserverRegistry getLaunchObserverRegistry() {
            ActivityMetricsLaunchObserverRegistry launchObserverRegistry;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    launchObserverRegistry = ActivityTaskManagerService.this.mTaskSupervisor.getActivityMetricsLogger().getLaunchObserverRegistry();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return launchObserverRegistry;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public IBinder getUriPermissionOwnerForActivity(IBinder activityToken) {
            Binder externalToken;
            ActivityTaskManagerService.enforceNotIsolatedCaller("getUriPermissionOwnerForActivity");
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInRootTaskLocked(activityToken);
                    externalToken = r == null ? null : r.getUriPermissionsLocked().getExternalToken();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return externalToken;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public TaskSnapshot getTaskSnapshotBlocking(int taskId, boolean isLowResolution) {
            return ActivityTaskManagerService.this.getTaskSnapshot(taskId, isLowResolution);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean isUidForeground(int uid) {
            return ActivityTaskManagerService.this.hasActiveVisibleWindow(uid);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setDeviceOwnerUid(int uid) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.setDeviceOwnerUid(uid);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setCompanionAppUids(int userId, Set<Integer> companionAppUids) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.mCompanionAppUidsMap.put(Integer.valueOf(userId), companionAppUids);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean isBaseOfLockedTask(String packageName) {
            boolean isBaseOfLockedTask;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    isBaseOfLockedTask = ActivityTaskManagerService.this.getLockTaskController().isBaseOfLockedTask(packageName);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return isBaseOfLockedTask;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public ActivityTaskManagerInternal.PackageConfigurationUpdater createPackageConfigurationUpdater() {
            return new PackageConfigurationUpdaterImpl(Binder.getCallingPid(), ActivityTaskManagerService.this);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public ActivityTaskManagerInternal.PackageConfigurationUpdater createPackageConfigurationUpdater(String packageName, int userId) {
            return new PackageConfigurationUpdaterImpl(packageName, userId, ActivityTaskManagerService.this);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public ActivityTaskManagerInternal.PackageConfig getApplicationConfig(String packageName, int userId) {
            return ActivityTaskManagerService.this.mPackageConfigPersister.findPackageConfiguration(packageName, userId);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean hasSystemAlertWindowPermission(int callingUid, int callingPid, String callingPackage) {
            return ActivityTaskManagerService.this.hasSystemAlertWindowPermission(callingUid, callingPid, callingPackage);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public boolean isSplitScreen() {
            return ActivityTaskManagerService.this.isSplitScreen();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void notifyWakingUp() {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityTaskManagerService.this.getTransitionController().requestTransitionIfNeeded(11, 0, null, ActivityTaskManagerService.this.mRootWindowContainer.getDefaultDisplay());
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void registerActivityStartInterceptor(int id, ActivityInterceptorCallback callback) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ActivityTaskManagerService.this.mActivityInterceptorCallbacks.contains(id)) {
                        throw new IllegalArgumentException("Duplicate id provided: " + id);
                    }
                    if (id > 4 || id < 0) {
                        throw new IllegalArgumentException("Provided id " + id + " is not in range of valid ids [0,4]");
                    }
                    ActivityTaskManagerService.this.mActivityInterceptorCallbacks.put(id, callback);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public ActivityManager.RecentTaskInfo getMostRecentTaskFromBackground() {
            List<ActivityManager.RunningTaskInfo> runningTaskInfoList = ActivityTaskManagerService.this.getTasks(1);
            if (runningTaskInfoList.size() > 0) {
                ActivityManager.RunningTaskInfo runningTaskInfo = runningTaskInfoList.get(0);
                ActivityTaskManagerService activityTaskManagerService = ActivityTaskManagerService.this;
                List<ActivityManager.RecentTaskInfo> recentTaskInfoList = activityTaskManagerService.getRecentTasks(2, 2, activityTaskManagerService.mContext.getUserId()).getList();
                for (ActivityManager.RecentTaskInfo info : recentTaskInfoList) {
                    if (info.id != runningTaskInfo.id) {
                        return info;
                    }
                }
                return null;
            }
            Slog.i(ActivityTaskManagerService.TAG, "No running task found!");
            return null;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public List<ActivityManager.AppTask> getAppTasks(String pkgName, int uid) {
            ArrayList<ActivityManager.AppTask> tasks = new ArrayList<>();
            List<IBinder> appTasks = ActivityTaskManagerService.this.getAppTasks(pkgName, uid);
            int numAppTasks = appTasks.size();
            for (int i = 0; i < numAppTasks; i++) {
                tasks.add(new ActivityManager.AppTask(IAppTask.Stub.asInterface(appTasks.get(i))));
            }
            return tasks;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public int getTaskToShowPermissionDialogOn(String pkgName, int uid) {
            int taskToShowPermissionDialogOn;
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    taskToShowPermissionDialogOn = ActivityTaskManagerService.this.mRootWindowContainer.getTaskToShowPermissionDialogOn(pkgName, uid);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return taskToShowPermissionDialogOn;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void restartTaskActivityProcessIfVisible(int taskId, final String packageName) {
            synchronized (ActivityTaskManagerService.this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    Task task = ActivityTaskManagerService.this.mRootWindowContainer.anyTaskForId(taskId, 0);
                    if (task == null) {
                        Slog.w(ActivityTaskManagerService.TAG, "Failed to restart Activity. No task found for id: " + taskId);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    ActivityRecord activity = task.getActivity(new Predicate() { // from class: com.android.server.wm.ActivityTaskManagerService$LocalService$$ExternalSyntheticLambda2
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            return ActivityTaskManagerService.LocalService.lambda$restartTaskActivityProcessIfVisible$4(packageName, (ActivityRecord) obj);
                        }
                    });
                    if (activity == null) {
                        Slog.w(ActivityTaskManagerService.TAG, "Failed to restart Activity. No Activity found for package name: " + packageName + " in task: " + taskId);
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    activity.restartProcessIfVisible();
                    WindowManagerService.resetPriorityAfterLockedSection();
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$restartTaskActivityProcessIfVisible$4(String packageName, ActivityRecord activityRecord) {
            return packageName.equals(activityRecord.packageName) && !activityRecord.finishing;
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void hookShowMultiDisplayWindow() {
            ActivityTaskManagerService.this.hookShowMultiDisplayWindow(-1);
        }

        @Override // com.android.server.wm.ActivityTaskManagerInternal
        public void setStartInMultiWindow(String pkgName, int type, int direction, int startType) {
            ActivityTaskManagerService.this.setStartInMultiWindow(pkgName, type, direction, startType);
        }
    }

    public String getFocusedWinPkgName() {
        return ITranWindowManagerService.Instance().getFocusedWinPkgName();
    }

    public boolean isSecureWindow() {
        return ITranWindowManagerService.Instance().isSecureWindow();
    }

    public boolean isIMEShowing() {
        return ITranWindowManagerService.Instance().isIMEShowing();
    }

    public boolean isPinnedMode() {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task rootPinnedTask = this.mRootWindowContainer.getDefaultTaskDisplayArea().getRootPinnedTask();
                z = rootPinnedTask != null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public boolean inMultiWindowMode() {
        return ITranWindowManagerService.Instance().inMultiWindowMode();
    }

    public ActivityManagerInternal getAmInternal() {
        return this.mAmInternal;
    }

    public void setMultiWindowAcquireFocus(int multiWindowId, boolean acquireFocus) {
        Slog.i(TAG, " request for acquire focus for multiWindowId " + multiWindowId);
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                TaskDisplayArea tds = acquireFocus ? this.mRootWindowContainer.getMultiDisplayArea(0, multiWindowId) : this.mRootWindowContainer.getDefaultTaskDisplayArea();
                if (tds != null && tds.getTopRootTask() != null) {
                    tds.getTopRootTask().moveToFront("AcquireFocus");
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void moveToBottomForMultiWindowV3(String reason) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                TaskDisplayArea tds = this.mRootWindowContainer.getMultiDisplayArea();
                if (tds != null && tds.getTopRootTask() != null) {
                    tds.getTopRootTask().moveToBottomForMultiWindow(reason);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean isSupportMultiUser() {
        return this.mAmInternal.getCurrentUserId() == 0;
    }

    public void hookShowMultiDisplayWindow(int startType) {
        if (!isSupportMultiUser()) {
            Slog.i(TAG, "not support in multi-user mode");
            return;
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (!isStandardActivity()) {
                    Slog.i(TAG, "app not support multiwindow !");
                    ITranActivityTaskManagerService.Instance().showUnSupportMultiToast();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (isSpecialScenario() && isAppOverLayOrSecureLayer()) {
                    if (ThunderbackConfig.isVersion3() && hasMultiWindow()) {
                        ITranActivityTaskManagerService.Instance().showSceneUnSupportMultiToast();
                        Slog.i(TAG, "already has multiwindow !");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (ThunderbackConfig.isVersion3()) {
                        this.mRootWindowContainer.hookShowMultiDisplayWindow();
                    } else {
                        this.mRootWindowContainer.hookShowMultiDisplayWindow(startType);
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Slog.i(TAG, "scene not support multiwindow !");
                ITranActivityTaskManagerService.Instance().showSceneUnSupportMultiToast();
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void hookShowMultiDisplayWindow(Task task) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (!isSpecialScenario()) {
                    ITranActivityTaskManagerService.Instance().showUnSupportMultiToast();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                String packageName = task.getTopNonFinishingActivity() != null ? task.getTopNonFinishingActivity().packageName : "null";
                if (isInBlackList(packageName)) {
                    Slog.i(TAG, "not support multiwindow due to in blacklist!");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                this.mRootWindowContainer.hookShowMultiDisplayWindow(task);
                WindowManagerService.resetPriorityAfterLockedSection();
                synchronized (this.mStartActivityLock) {
                    try {
                        this.mStartActivityLock.wait(300L);
                    } catch (InterruptedException e) {
                        Slog.e(TAG, "Timeout to wait for the reparent activity.");
                    }
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setTaskSplitManagerProxy(ITaskSplitManager taskSplitManager) {
        this.mTaskSplitManager = taskSplitManager;
        ITranActivityTaskManagerService.Instance().hookSetTaskSplitManagerProxy(taskSplitManager);
    }

    public void registerTaskSplitListener(ITaskSplitManagerListener taskSplitListener) {
        ITranActivityTaskManagerService.Instance().hookRegisterTaskSplitListener(taskSplitListener);
    }

    public void unregisterTaskSplitListener(ITaskSplitManagerListener taskSplitListener) {
        ITranActivityTaskManagerService.Instance().hookUnregisterTaskSplitListener(taskSplitListener);
    }

    public void notifyTaskAnimationResult(int taskId, ITaskAnimation taskAnimation) {
        ITranActivityTaskManagerService.Instance().hookNotifyTaskAnimationResult(taskId, taskAnimation);
    }

    public ITaskAnimation createTaskAnimation(IBinder token, int taskId, MultiTaskRemoteAnimationAdapter callback) {
        return ITranActivityTaskManagerService.Instance().hookCreateTaskAnimation(token, taskId, callback);
    }

    public void hookReparentToDefaultDisplay(int multiWindowMode, int multiWindowId) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                hookReparentToDefaultDisplay(multiWindowMode, multiWindowId, true);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookReparentToDefaultDisplay(int multiWindowMode, int multiWindowId, boolean anim) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRootWindowContainer.hookReparentToDefaultDisplay(multiWindowMode, multiWindowId, anim);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookMultiWindowToMaxV3(IWindow window) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int displayAreaId = getDisplayAreaId(window);
                this.mRootWindowContainer.hookMultiWindowToMax(displayAreaId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookMultiWindowToMaxV3(int displayAreaId) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRootWindowContainer.hookMultiWindowToMax(displayAreaId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookMultiWindowToMinV3(IWindow window) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int displayAreaId = getDisplayAreaId(window);
                if (displayAreaId != -1) {
                    this.mRootWindowContainer.hookMultiWindowToMinV3(displayAreaId);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookMultiWindowToCloseV3(IWindow window) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int displayAreaId = getDisplayAreaId(window);
                if (displayAreaId != -1) {
                    this.mRootWindowContainer.hookMultiWindowToCloseV3(displayAreaId);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookMultiWindowToSmallV3(IWindow window) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int displayAreaId = getDisplayAreaId(window);
                if (displayAreaId != -1) {
                    this.mRootWindowContainer.hookMultiWindowToSmallV3(displayAreaId);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookMultiWindowToLargeV3(IWindow window) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int displayAreaId = getDisplayAreaId(window);
                if (displayAreaId != -1) {
                    this.mRootWindowContainer.hookMultiWindowToLargeV3(displayAreaId);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookMultiWindowFlingV3(IWindow window, MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int displayAreaId = getDisplayAreaId(window);
                if (displayAreaId != -1) {
                    this.mRootWindowContainer.hookMultiWindowFlingV3(displayAreaId, e1, e2, velocityX, velocityY);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void hookMultiWindowToCloseV4(int multiWindowMode, int multiWindowId) {
        this.mRootWindowContainer.hookMultiWindowToCloseV4(multiWindowMode, multiWindowId);
    }

    public void hookMultiWindowToSmallV4(int multiWindowMode, int multiWindowId) {
        this.mRootWindowContainer.hookMultiWindowToSmallV4(multiWindowMode, multiWindowId);
    }

    public void hookMultiWindowToSplit(int multiWindowMode, int multiWindowId) {
        this.mRootWindowContainer.hookMultiWindowToSplit(multiWindowMode, multiWindowId);
    }

    public void hookMultiWindowVisible() {
        ITranActivityTaskManagerService.Instance().hookMultiWindowVisible();
    }

    public void hookMultiWindowInvisible() {
        ITranActivityTaskManagerService.Instance().hookMultiWindowInvisible();
    }

    public void hookStartActivityResult(int result, Rect location) {
        ITranActivityTaskManagerService.Instance().hookStartActivityResult(this.mStartActivityLock, result, location);
    }

    public void hookFinishMovingLocationV3(IWindow window) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int displayAreaId = getDisplayAreaId(window);
                if (displayAreaId != -1) {
                    this.mRootWindowContainer.hookFinishMovingLocationV3(displayAreaId);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean getMuteState() {
        return ITranActivityTaskManagerService.Instance().getMuteState(-999);
    }

    public void setMuteState(boolean state) {
        ITranActivityTaskManagerService.Instance().setMuteState(state, -999);
    }

    public boolean getMuteStateV4(int multiWindowId) {
        return ITranActivityTaskManagerService.Instance().getMuteState(multiWindowId);
    }

    public void setMuteStateV4(boolean state, int multiWindowId) {
        ITranActivityTaskManagerService.Instance().setMuteState(state, multiWindowId);
    }

    public void hookMultiWindowMuteAethenV4(int type) {
        ITranActivityTaskManagerService.Instance().hookMultiWindowMuteAethenV4(type);
    }

    public void removeFromMuteState(int multiWindowId) {
        ITranActivityTaskManagerService.Instance().removeFromMuteState(multiWindowId);
    }

    public void hookMultiWindowMute(IWindow window) {
    }

    public Rect getMultiWindowDefaultRect() {
        return ITranActivityTaskManagerService.Instance().getMultiWindowDefaultRect();
    }

    public void hookMultiWindowLocation(IWindow window, int x, int y, int touchX, int touchY) {
        ITranActivityTaskManagerService.Instance().hookMultiWindowLocation(getDisplayAreaId(window), x, y, touchX, touchY);
    }

    public int getDisplayAreaId(IWindow window) {
        WindowState win;
        if (window != null && (win = this.mWindowManager.windowForClientLocked((Session) null, window, false)) != null) {
            DisplayArea displayArea = win.getDisplayArea();
            if (ThunderbackConfig.isVersion3()) {
                if (displayArea != null && displayArea.isMultiWindow()) {
                    return displayArea.mDisplayAreaAppearedInfo.getTranMultiDisplayAreaId();
                }
            } else if (ThunderbackConfig.isVersion4() && displayArea != null && displayArea.isMultiWindow()) {
                return displayArea.getMultiWindowingId();
            }
        }
        return -1;
    }

    public void setTranMultiWindowModeV3(int mode) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRootWindowContainer.setTranMultiWindowModeV3(mode);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setStartInMultiWindow(String pkgName, int type, int direction, int startType) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                TaskDisplayArea perferDisplayArea = this.mRootWindowContainer.getDefaultDisplay().getPreferDisplayArea(pkgName);
                if (perferDisplayArea != null && perferDisplayArea == this.mRootWindowContainer.getDefaultTaskDisplayArea() && startType != 8 && startType != 6) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                ITranActivityTaskManagerService.Instance().setStartInMultiWindow(isSpecialScenario(pkgName), pkgName, type, direction, startType);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean hasMultiWindow() {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                z = this.mRootWindowContainer.getMultiDisplayArea() != null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public boolean activityInMultiWindow(String pkgName) {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                z = hasMultiWindow() && this.mRootWindowContainer.activityInMultiWindow(pkgName);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public String getMulitWindowTopPackage() {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (hasMultiWindow()) {
                    String mulitWindowTopPackage = this.mRootWindowContainer.getMulitWindowTopPackage();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return mulitWindowTopPackage;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void startCurrentAppInMultiWindow(boolean anim, int startType) {
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord mResumedActivity = this.mRootWindowContainer.getTopResumedActivity();
                String pkgName = mResumedActivity != null ? mResumedActivity.packageName : null;
                ITranActivityTaskManagerService.Instance().hookMultiWindowStartAethen(startType, pkgName);
                Slog.d(TAG, "startCurrentAppInMultiWindow : startType " + startType);
                hookShowMultiDisplayWindow(startType);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } catch (Exception e) {
            Slog.w(TAG, "TranMultiWindow: ignore exception due to main thread not pepared");
        }
    }

    public boolean isSupportMultiWindow() {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                z = isStandardActivity() && isSpecialScenario() && isAppOverLayOrSecureLayer();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public boolean isSplitScreenSupportMultiWindowV4(int type, ActivityManager.RunningTaskInfo info) {
        if (ITranMultiWindow.Instance().isPayTriggerOrDLCMode(this.mContext, type)) {
            return false;
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (!isSupportMultiUser()) {
                    Slog.i(TAG, "multi-user not support for SplitScreen-To-MultiWindow");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (!checkMultiWindowFeatureOn()) {
                    Slog.i(TAG, "setting tb off");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (!isAppOverLayOrSecureLayer()) {
                    Slog.i(TAG, "the scene not support multiwindow !");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else {
                    boolean isSupport = true;
                    switch (type) {
                        case 11:
                        case 12:
                            if (info == null) {
                                Slog.i(TAG, "TaskInfo is null");
                                isSupport = false;
                                break;
                            } else {
                                String pacakgeName = info.realActivity != null ? info.realActivity.getPackageName() : null;
                                if (pacakgeName != null && !isInBlackList(pacakgeName)) {
                                    if (!isSpecialScenario()) {
                                        Slog.i(TAG, "current scene not support multiwindow !");
                                        isSupport = false;
                                        break;
                                    }
                                }
                                Slog.i(TAG, "package in blacklist");
                                isSupport = false;
                            }
                            break;
                        case 13:
                            if (!isStandardActivity()) {
                                Slog.i(TAG, "the app not support multiwindow !");
                                isSupport = false;
                                break;
                            }
                            break;
                        default:
                            isSupport = false;
                            break;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return isSupport;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void setMultiWindowBlackListToSystem(List<String> list) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRootWindowContainer.setMultiWindowBlackListToSystem(list);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setMultiWindowWhiteListToSystem(List<String> list) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRootWindowContainer.setMultiWindowWhiteListToSystem(list);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setMultiWindowConfigToSystem(String key, List<String> list) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRootWindowContainer.setMultiWindowConfigToSystem(key, list);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean isStandardActivity() {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent defaultDisplayContent = this.mRootWindowContainer.getDefaultDisplay();
                if (defaultDisplayContent != null && defaultDisplayContent.mCurrentFocus != null && defaultDisplayContent.mCurrentFocus.getOwningPackage().equals("com.android.systemui") && defaultDisplayContent.mCurrentFocus.mAttrs.type == 2021) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                ActivityRecord mResumedActivity = this.mRootWindowContainer.getDefaultTaskDisplayArea().getFocusedActivity();
                if (mResumedActivity == null) {
                    Slog.d(TAG, "ResumeActiviy not support thunderback : mResumedActivity = " + mResumedActivity);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (mResumedActivity.mFloatOrTranslucent) {
                    Slog.d(TAG, "not support thunderback due to mFloatOrTranslucent = true");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (mResumedActivity.getActivityType() == 2) {
                    Slog.d(TAG, "not support thunderback due to ActivityType = ACTIVITY_TYPE_HOME");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (isInBlackList(mResumedActivity.packageName)) {
                    Slog.d(TAG, "not support thunderback due to ResumedActivity in blacklists : " + mResumedActivity.packageName);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else {
                    Task task = mResumedActivity.getTask();
                    if (task != null) {
                        if (task.affinity != null && task.affinity.contains("com.transsion.resolver")) {
                            Slog.d(TAG, "not support thunderback, ResumeActivity's affinity is resolver pkg");
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return false;
                        }
                        ActivityRecord visibleAndInBlackList = task.getActivity(new Predicate() { // from class: com.android.server.wm.ActivityTaskManagerService$$ExternalSyntheticLambda2
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj) {
                                return ActivityTaskManagerService.this.m7829x25998267((ActivityRecord) obj);
                            }
                        });
                        if (visibleAndInBlackList != null) {
                            Slog.d(TAG, "not support thunderback due to in blacklist : " + visibleAndInBlackList.packageName);
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return false;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isStandardActivity$10$com-android-server-wm-ActivityTaskManagerService  reason: not valid java name */
    public /* synthetic */ boolean m7829x25998267(ActivityRecord r) {
        if (r.isVisible() && isInBlackList(r.packageName)) {
            return true;
        }
        return false;
    }

    private boolean isAppOverLayOrSecureLayer() {
        DisplayContent defaultDisplayContent = this.mRootWindowContainer.getDefaultDisplay();
        if (defaultDisplayContent != null && defaultDisplayContent.hasOverlayWindow()) {
            Slog.w(TAG, "defaultDisplayContent.hasOverlayDialog() = " + defaultDisplayContent.hasOverlayWindow());
            return false;
        } else if (ThunderbackConfig.isVersion3()) {
            ActivityRecord mResumedActivity = this.mRootWindowContainer.getTopResumedActivity();
            if (mResumedActivity == null) {
                Slog.d(TAG, "ResumeActiviy not support thunderback : mResumedActivity = " + mResumedActivity);
                return false;
            }
            WindowState windowState = mResumedActivity.findMainWindow();
            if (windowState != null && (windowState.mAttrs.flags & 8192) != 0) {
                Slog.d(TAG, "not support thunderback due to secure layer");
                return false;
            }
            return true;
        } else {
            return true;
        }
    }

    public boolean isInBlackList(String pkgName) {
        return ITranActivityTaskManagerService.Instance().isInBlackList(pkgName);
    }

    public boolean isSpecialScenario() {
        return isSpecialScenario(null);
    }

    public boolean isSpecialScenario(String pkgName) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mKeyguardController.isKeyguardLocked()) {
                    Slog.d(TAG, "not support thunderback due to keyguard showing");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                ActivityRecord mResumedActivity = this.mRootWindowContainer.getDefaultTaskDisplayArea().getFocusedActivity();
                if (mResumedActivity == null) {
                    Slog.d(TAG, "ResumeActiviy not support thunderback : mResumedActivity = " + mResumedActivity);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                Task task = mResumedActivity.getTask();
                if (task == null) {
                    Slog.d(TAG, "ResumeActiviy not support thunderback : task  = null");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                int mRotation = this.mWindowManager.getDefaultDisplayRotation();
                if (("com.transsion.calculator".equals(pkgName) || "com.transsion.calculator".equals(mResumedActivity.packageName)) && mRotation != 0 && mRotation != 2 && !ITranMultiWindow.Instance().inLargeScreen(this.mRootWindowContainer.getDefaultDisplay().getDisplayMetrics())) {
                    Slog.d(TAG, "The horizontal screen calculator does not support thunderback");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                if (ThunderbackConfig.isVersion3()) {
                    if (task.getWindowingMode() != 1) {
                        Slog.d(TAG, "not support thunderback due to not WINDOWING_MODE_FULLSCREEN");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                } else if (task.getWindowingMode() != 1 && task.getWindowingMode() != 6) {
                    Slog.d(TAG, "not support thunderback due to not WINDOWING_MODE_FULLSCREEN");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                boolean isTaskLocked = getLockTaskController().isTaskLocked(task);
                if (!isTaskLocked) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                }
                Slog.d(TAG, "not support thunderback due to isTaskLocked mode");
                WindowManagerService.resetPriorityAfterLockedSection();
                return false;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public String getMultiWindowVersion() {
        return ITranActivityTaskManagerService.Instance().getMultiWindowVersion();
    }

    public List<String> getMultiWindowBlackList() {
        List<String> multiWindowBlackList;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                multiWindowBlackList = this.mRootWindowContainer.getMultiWindowBlackList();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return multiWindowBlackList;
    }

    public void setFinishFixedRotationWithTransaction(SurfaceControl leash, float[] transFloat9, float[] cropFloat4, int rotation) {
        if (leash != null) {
            synchronized (this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    this.mRootWindowContainer.setFinishFixedRotationWithTransaction(leash, transFloat9, cropFloat4, rotation);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
        ITranActivityTaskManagerService.Instance().hookNotifyDefaultDisplayRotationResult();
    }

    public void clearFinishFixedRotationWithTransaction() {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRootWindowContainer.clearFinishFixedRotationWithTransaction();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setFinishFixedRotationEnterMultiWindowTransactionV3(SurfaceControl leash, int x, int y, int rotation, float scale) {
        if (leash != null) {
            synchronized (this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    this.mRootWindowContainer.setFinishFixedRotationEnterMultiWindowTransaction(leash, x, y, rotation, scale);
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
        ITranActivityTaskManagerService.Instance().hookNotifyDefaultDisplayRotationResult();
    }

    public SurfaceControl getWeltWindowLeash(int width, int height, int x, int y, boolean hidden) {
        SurfaceControl weltWindowLeash;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                weltWindowLeash = this.mRootWindowContainer.getWeltWindowLeash(width, height, x, y, hidden);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return weltWindowLeash;
    }

    public void removeWeltWindowLeash(SurfaceControl leash) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRootWindowContainer.removeWeltWindowLeash(leash);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public SurfaceControl getDragAndZoomBgLeash(int width, int height, int x, int y, boolean hidden) {
        SurfaceControl dragAndZoomBgLeash;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                dragAndZoomBgLeash = this.mRootWindowContainer.getDragAndZoomBgLeash(width, height, x, y, hidden);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return dragAndZoomBgLeash;
    }

    public void hookActiveMultiWindowStartToMove(IBinder token, int multiWindowMode, int multiWindowId, MotionEvent e, Point downPoint) {
        ITranActivityTaskManagerService.Instance().hookActiveMultiWindowStartToMove(token, multiWindowMode, multiWindowId, e, downPoint);
    }

    public void hookActiveMultiWindowMove(int multiWindowMode, int multiWindowId, MotionEvent e) {
        ITranActivityTaskManagerService.Instance().hookActiveMultiWindowMove(multiWindowMode, multiWindowId, e);
    }

    public void hookActiveMultiWindowEndMove(int multiWindowMode, int multiWindowId, MotionEvent e) {
        ITranActivityTaskManagerService.Instance().hookActiveMultiWindowEndMove(multiWindowMode, multiWindowId, e);
    }

    public void hookActiveMultiWindowMoveStartV4(int multiWindowMode, int multiWindowId) {
        ITranActivityTaskManagerService.Instance().hookActiveMultiWindowMoveStartV4(multiWindowMode, multiWindowId);
    }

    public void hookActiveMultiWindowMoveStartV3() {
        ITranActivityTaskManagerService.Instance().hookActiveMultiWindowMoveStartV3();
    }

    public boolean checkMultiWindowFeatureOn() {
        return 1 == Settings.Global.getInt(this.mContext.getContentResolver(), "tranthunderback_enable", 1);
    }

    public String getMultiDisplayAreaTopPackageV4(int multiWindowMode, int multiWindowId) {
        String multiDisplayAreaTopPackage;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                multiDisplayAreaTopPackage = this.mRootWindowContainer.getMultiDisplayAreaTopPackage(multiWindowMode, multiWindowId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return multiDisplayAreaTopPackage;
    }

    public String getMultiDisplayAreaTopPackage() {
        String multiDisplayAreaTopPackageV3;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                multiDisplayAreaTopPackageV3 = this.mRootWindowContainer.getMultiDisplayAreaTopPackageV3();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return multiDisplayAreaTopPackageV3;
    }

    public List<String> getMultiWindowTopPackages() {
        List<String> topPackages;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                topPackages = this.mRootWindowContainer.getTopPackages();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return topPackages;
    }

    public ActivityManager.RunningTaskInfo getTopTask(int displayId) {
        ActivityManager.RunningTaskInfo topTask;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                topTask = this.mRootWindowContainer.getTopTask(displayId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return topTask;
    }

    public ActivityManager.RunningTaskInfo getMultiWinTopTask(int winMode, int winId) {
        ActivityManager.RunningTaskInfo multiWinTopTask;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                multiWinTopTask = this.mRootWindowContainer.getMultiWinTopTask(winMode, winId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return multiWinTopTask;
    }

    public int getTaskOrientation(int taskId) {
        int taskOrientation;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                taskOrientation = this.mRootWindowContainer.getTaskOrientation(taskId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return taskOrientation;
    }

    public void minimizeMultiWinToEdge(int taskId, boolean toEdge) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRootWindowContainer.minimizeMultiWinToEdge(taskId, toEdge);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public Rect getMultiWindowContentRegion(int taskId) {
        Rect multiWindowContentRegion;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                multiWindowContentRegion = this.mRootWindowContainer.getMultiWindowContentRegion(taskId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return multiWindowContentRegion;
    }

    public void updateZBoostTaskIdWhenToSplit(int taskId) {
        ITranMultiWindow.Instance().updateZBoostTaskIdWhenToSplit(taskId);
    }

    public SurfaceControl getDefaultRootLeash() {
        SurfaceControl defaultRootLeash;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                defaultRootLeash = this.mRootWindowContainer.getDefaultRootLeash();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return defaultRootLeash;
    }

    public void hookExitSplitScreenToMultiWindow(int multiWindowMode) {
        Slog.i(TAG, "hookExitSplitScreenToMultiWindow");
        ITranActivityTaskManagerService.Instance().hookExitSplitScreenToMultiWindow(multiWindowMode);
    }

    public Rect hookGetMultiWindowDefaultRect(int orientationType) {
        return ITranActivityTaskManagerService.Instance().hookGetMultiWindowDefaultRect(orientationType);
    }

    public Rect hookGetMultiWindowDefaultRectByTask(int taskId) {
        return ITranActivityTaskManagerService.Instance().hookGetMultiWindowDefaultRectByTask(taskId);
    }

    public void hookSetMultiWindowDefaultRectResult(Rect rect) {
        ITranActivityTaskManagerService.Instance().hookSetMultiWindowDefaultRectResult(rect);
    }

    public void hookStartMultiWindowFromSplitScreen(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback) {
        Slog.i(TAG, "hookStartMultiWindowFromSplitScreen");
        ITranActivityTaskManagerService.Instance().hookStartMultiWindowFromSplitScreen(fullTaskId, multiTaskToken, multiWindowRegion, syncCallback, 0);
    }

    public void hookStartMultiWindowFromSplitScreenV4(int fullTaskId, WindowContainerToken multiTaskToken, Rect multiWindowRegion, IWindowContainerTransactionCallbackSync syncCallback, int type) {
        Slog.i(TAG, "hookStartMultiWindowFromSplitScreen");
        ITranActivityTaskManagerService.Instance().hookStartMultiWindowFromSplitScreen(fullTaskId, multiTaskToken, multiWindowRegion, syncCallback, type);
        ITranActivityTaskManagerService.Instance().hookMultiWindowStartAethen(type, "SplitTempPkg");
    }

    public Bundle getMultiWindowParams(String pkgName) {
        Bundle multiWindowParams;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                multiWindowParams = this.mRootWindowContainer.getMultiWindowParams(pkgName);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return multiWindowParams;
    }

    public void setMultiWindowParams(Bundle params) {
        ITranActivityTaskManagerService.Instance().setMultiWindowParams(params);
    }

    public void hookStartMultiWindow(int taskId, Rect multiWindowRegion, IWindowContainerTransactionCallback callback) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Slog.i(TAG, "hookStartMultiWindow");
                Task task = this.mRootWindowContainer.anyTaskForId(taskId);
                if (task == null) {
                    Slog.i(TAG, "hookStartMultiWindow fail, task is null");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ActivityManager.RunningTaskInfo info = task.getTaskInfo();
                String pacakgeName = info.realActivity != null ? info.realActivity.getPackageName() : "";
                ITranActivityTaskManagerService.Instance().hookStartMultiWindow(info.token, pacakgeName, multiWindowRegion, callback);
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void hookReserveMultiWindowNumber(int reserveNum, long showDelayTime) {
        ITranActivityTaskManagerService.Instance().hookReserveMultiWindowNumber(reserveNum, showDelayTime);
    }

    public void hookShowBlurLayerFinish() {
        this.mTaskSupervisor.hookShowBlurLayerFinish();
    }

    public boolean taskInMultiWindowById(int taskId) {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                z = hasMultiWindow() && this.mRootWindowContainer.taskInMultiWindowById(taskId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public void addAnimationIconLayer(SurfaceControl sc) {
        ITranActivityTaskManagerService.Instance().hookAddAnimationIconLayer(sc);
    }

    public void removeAnimationIconLayer(SurfaceControl sc) {
        ITranActivityTaskManagerService.Instance().hookRemoveAnimationIconLayer(sc);
    }

    public void boostSceneStart(int scene) {
        ITranActivityTaskManagerService.Instance().boostSceneStart(scene);
    }

    public void boostSceneStartDuration(int scene, long durationMs) {
        ITranActivityTaskManagerService.Instance().boostSceneStartDuration(scene, durationMs);
    }

    public void boostSceneEnd(int scene) {
        ITranActivityTaskManagerService.Instance().boostSceneEnd(scene);
    }

    public void boostSceneEndDelay(int scene, long delayMs) {
        ITranActivityTaskManagerService.Instance().boostSceneEndDelay(scene, delayMs);
    }

    public void boostStartForLauncher() {
        ITranActivityTaskManagerService.Instance().boostStartForLauncher();
    }

    public void boostEndForLauncher() {
        ITranActivityTaskManagerService.Instance().boostEndForLauncher();
    }

    public void boostStartInLauncher(int type) {
        ITranActivityTaskManagerService.Instance().boostStartInLauncher(type);
    }

    public void boostEndInLauncher(int type) {
        ITranActivityTaskManagerService.Instance().boostEndInLauncher(type);
    }

    public void handleDrawThreadsBoost(boolean handleAllThreads, boolean bindBigCore, List<String> handleActName) {
        ITranActivityTaskManagerService.Instance().handleDrawThreadsBoost(handleAllThreads, bindBigCore, handleActName);
    }

    public void resetBoostThreads() {
        ITranActivityTaskManagerService.Instance().resetBoostThreads();
    }

    public void reparentActivity(int fromDisplayId, int destDisplayId, boolean onTop) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mRootWindowContainer.reparentActivity(fromDisplayId, destDisplayId, onTop);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setSruWhiteListToSystem(List<String> list) {
        ITranSruManager.Instance().setWhiteList(list);
    }
}
