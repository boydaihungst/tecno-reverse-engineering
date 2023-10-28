package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.IApplicationThread;
import android.app.ICompatCameraControlCallback;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.ResultInfo;
import android.app.TaskInfo;
import android.app.ThunderbackConfig;
import android.app.WindowConfiguration;
import android.app.admin.DevicePolicyManager;
import android.app.servertransaction.ActivityConfigurationChangeItem;
import android.app.servertransaction.ActivityLifecycleItem;
import android.app.servertransaction.ActivityRelaunchItem;
import android.app.servertransaction.ActivityResultItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.ClientTransactionItem;
import android.app.servertransaction.DestroyActivityItem;
import android.app.servertransaction.MoveToDisplayItem;
import android.app.servertransaction.NewIntentItem;
import android.app.servertransaction.PauseActivityItem;
import android.app.servertransaction.ResumeActivityItem;
import android.app.servertransaction.StartActivityItem;
import android.app.servertransaction.StopActivityItem;
import android.app.servertransaction.TopResumedActivityChangeItem;
import android.app.servertransaction.TransferSplashScreenViewStateItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.LocusId;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConstrainDisplayApisConfig;
import android.content.pm.PackageManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.HardwareBuffer;
import android.hardware.audio.common.V2_0.AudioChannelMask;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.InputConstants;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.dreams.DreamActivity;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.proto.ProtoOutputStream;
import android.view.AppTransitionAnimationSpec;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.InputApplicationHandle;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationDefinition;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.window.ITaskFragmentOrganizer;
import android.window.RemoteTransition;
import android.window.SizeConfigurationBuckets;
import android.window.SplashScreenView;
import android.window.TaskSnapshot;
import android.window.TransitionInfo;
import android.window.WindowContainerToken;
import com.android.internal.R;
import com.android.internal.app.ResolverActivity;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.os.TransferPipe;
import com.android.internal.policy.AttributeCache;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.ToBooleanFunction;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.am.AppTimeTracker;
import com.android.server.am.PendingIntentRecord;
import com.android.server.contentcapture.ContentCaptureManagerInternal;
import com.android.server.display.color.ColorDisplayService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.uri.NeededUriGrants;
import com.android.server.uri.UriPermissionOwner;
import com.android.server.wm.ActivityMetricsLogger;
import com.android.server.wm.RemoteAnimationController;
import com.android.server.wm.StartingSurfaceController;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowState;
import com.android.server.wm.utils.InsetUtils;
import com.android.server.wm.utils.TranFpUnlockStateController;
import com.google.android.collect.Sets;
import com.mediatek.server.wm.WmsExt;
import com.transsion.foldable.TranFoldingScreenManager;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.griffin.lib.app.TranAppInfo;
import com.transsion.hubcore.multiwindow.ITranMultiWindow;
import com.transsion.hubcore.server.am.ITranActivityPerformance;
import com.transsion.hubcore.server.wm.ITranActivityRecord;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import com.transsion.server.foldable.TranFoldingScreenController;
import dalvik.annotation.optimization.NeverCompile;
import defpackage.CompanionAppsPermissions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes2.dex */
public final class ActivityRecord extends WindowToken implements WindowManagerService.AppFreezeListener {
    static final String ACTIVITY_ICON_SUFFIX = "_activity_icon_";
    static final float ASPECT_RATIO_16_9 = 1.777f;
    static final float ASPECT_RATIO_4_3 = 1.333f;
    static final float ASPECT_RATIO_UNSET = -1.0f;
    private static final String ATTR_COMPONENTSPECIFIED = "component_specified";
    private static final String ATTR_ID = "id";
    private static final String ATTR_LAUNCHEDFROMFEATURE = "launched_from_feature";
    private static final String ATTR_LAUNCHEDFROMPACKAGE = "launched_from_package";
    private static final String ATTR_LAUNCHEDFROMUID = "launched_from_uid";
    private static final String ATTR_RESOLVEDTYPE = "resolved_type";
    private static final String ATTR_USERID = "user_id";
    private static final int DESTROY_TIMEOUT = 10000;
    static final long ENTER_PIP_TIMEOUT = 1000;
    static final int FINISH_RESULT_CANCELLED = 0;
    static final int FINISH_RESULT_REMOVED = 2;
    static final int FINISH_RESULT_REQUESTED = 1;
    static final int INVALID_PID = -1;
    static final int LAUNCH_SOURCE_TYPE_APPLICATION = 4;
    static final int LAUNCH_SOURCE_TYPE_HOME = 2;
    static final int LAUNCH_SOURCE_TYPE_SYSTEM = 1;
    static final int LAUNCH_SOURCE_TYPE_SYSTEMUI = 3;
    private static final int LAUNCH_TICK = 500;
    private static final int MAX_STOPPING_TO_FORCE = 3;
    private static final int PAUSE_TIMEOUT = 500;
    private static final ArrayList<String> SPLASHSCREEN_WHITELIST;
    private static final int SPLASH_SCREEN_BEHAVIOR_DEFAULT = 0;
    private static final int SPLASH_SCREEN_BEHAVIOR_ICON_PREFERRED = 1;
    static final int STARTING_WINDOW_TYPE_NONE = 0;
    static final int STARTING_WINDOW_TYPE_SNAPSHOT = 1;
    static final int STARTING_WINDOW_TYPE_SPLASH_SCREEN = 2;
    private static final int STOP_TIMEOUT = 11000;
    private static final String TAG_INTENT = "intent";
    private static final String TAG_PERSISTABLEBUNDLE = "persistable_bundle";
    static final int TRANSFER_SPLASH_SCREEN_ATTACH_TO_CLIENT = 2;
    static final int TRANSFER_SPLASH_SCREEN_COPYING = 1;
    static final int TRANSFER_SPLASH_SCREEN_FINISH = 3;
    static final int TRANSFER_SPLASH_SCREEN_IDLE = 0;
    private static final int TRANSFER_SPLASH_SCREEN_TIMEOUT = 2000;
    static final String TRAN_AUTO_PACKAGE_NAME = "com.transsion.auto";
    private static ConstrainDisplayApisConfig sConstrainDisplayApisConfig;
    boolean allDrawn;
    public WindowProcessController app;
    AppTimeTracker appTimeTracker;
    final Binder assistToken;
    CompatibilityInfo compat;
    private final boolean componentSpecified;
    int configChangeFlags;
    private long createTime;
    boolean deferRelaunchUntilPaused;
    boolean delayedResume;
    boolean finishing;
    boolean firstWindowDrawn;
    boolean forceNewConfig;
    boolean frozenBeforeDestroy;
    public final TranAppInfo grifAppInfo;
    boolean hasBeenLaunched;
    private int icon;
    boolean idle;
    boolean immersive;
    boolean inHistory;
    public final ActivityInfo info;
    public final Intent intent;
    final boolean isForceDisablePreview;
    final boolean isForceEnableCustomTransition;
    final boolean isForceNotRelaunchForCarMode;
    private boolean keysPaused;
    private int labelRes;
    long lastLaunchTime;
    long lastVisibleTime;
    int launchCount;
    boolean launchFailed;
    int launchMode;
    long launchTickTime;
    final String launchedFromFeatureId;
    final String launchedFromPackage;
    final int launchedFromPid;
    final int launchedFromUid;
    int lockTaskLaunchMode;
    final ComponentName mActivityComponent;
    private final ActivityRecordInputSink mActivityRecordInputSink;
    private final AddStartingWindow mAddStartingWindow;
    int mAllowedTouchUid;
    boolean mAlreadyLaunchered;
    AnimatingActivityRegistry mAnimatingActivityRegistry;
    boolean mAppStopped;
    final ActivityTaskManagerService mAtmService;
    private boolean mCameraCompatControlClickedByUser;
    private final boolean mCameraCompatControlEnabled;
    private int mCameraCompatControlState;
    Runnable mCancelDeferShowForPinnedModeRunnable;
    boolean mClientVisibilityDeferred;
    private final ColorDisplayService.ColorTransformController mColorTransformController;
    private ICompatCameraControlCallback mCompatCameraControlCallback;
    private CompatDisplayInsets mCompatDisplayInsets;
    private int mConfigurationSeq;
    private boolean mCurrentLaunchCanTurnScreenOn;
    private boolean mDeferHidingClient;
    boolean mDeferShowForEnteringPinnedMode;
    private final Runnable mDestroyTimeoutRunnable;
    boolean mDismissKeyguardIfInsecure;
    boolean mEnableRecentsScreenshot;
    boolean mEnteringAnimation;
    Drawable mEnterpriseThumbnailDrawable;
    boolean mFloatOrTranslucent;
    private boolean mForceResizeable;
    private boolean mFreezingScreen;
    boolean mHandleExitSplashScreen;
    int mHandoverLaunchDisplayId;
    TaskDisplayArea mHandoverTaskDisplayArea;
    private boolean mHaveState;
    private Bundle mIcicle;
    boolean mImeInsetsFrozenUntilStartInput;
    private boolean mInSizeCompatModeForBounds;
    private boolean mInheritShownWhenLocked;
    private InputApplicationHandle mInputApplicationHandle;
    long mInputDispatchingTimeoutMillis;
    private boolean mIsAspectRatioApplied;
    private boolean mIsEligibleForFixedOrientationLetterbox;
    boolean mIsExiting;
    private boolean mIsInputDroppedForAnimation;
    private boolean mIsPkgInActivityEmbedding;
    private boolean mLastAllDrawn;
    boolean mLastAllReadyAtSync;
    private AppSaturationInfo mLastAppSaturationInfo;
    private boolean mLastContainsDismissKeyguardWindow;
    private boolean mLastContainsShowWhenLockedWindow;
    private boolean mLastContainsTurnScreenOnWindow;
    private boolean mLastDeferHidingClient;
    private int mLastDropInputMode;
    boolean mLastImeShown;
    Intent mLastNewIntent;
    private Task mLastParentBeforePip;
    private MergedConfiguration mLastReportedConfiguration;
    private int mLastReportedDisplayId;
    boolean mLastReportedMultiWindowMode;
    boolean mLastReportedPictureInPictureMode;
    private boolean mLastSurfaceShowing;
    ITaskFragmentOrganizer mLastTaskFragmentOrganizerBeforePip;
    private long mLastTransactionSequence;
    IBinder mLaunchCookie;
    private ActivityRecord mLaunchIntoPipHostActivity;
    WindowContainerToken mLaunchRootTask;
    private final int mLaunchSourceType;
    private final Runnable mLaunchTickRunnable;
    private boolean mLaunchedFromBubble;
    private Rect mLetterboxBoundsForFixedOrientationAndAspectRatio;
    final LetterboxUiController mLetterboxUiController;
    private LocusId mLocusId;
    private float mMinAspectRatioForUser;
    boolean mNeedShowAfterEnterPinnedMode;
    private int mNumDrawnWindows;
    private int mNumInterestingWindows;
    private boolean mOccludesParent;
    boolean mOverrideTaskTransition;
    private final Runnable mPauseTimeoutRunnable;
    private ActivityOptions mPendingOptions;
    private int mPendingRelaunchCount;
    private RemoteAnimationAdapter mPendingRemoteAnimation;
    private RemoteTransition mPendingRemoteTransition;
    private PersistableBundle mPersistentState;
    String mPnpProp;
    int mRelaunchReason;
    long mRelaunchStartTime;
    private RemoteAnimationDefinition mRemoteAnimationDefinition;
    private boolean mRemovingFromDisplay;
    private boolean mReportedDrawn;
    private final WindowState.UpdateReportedVisibilityResults mReportedVisibilityResults;
    boolean mRequestForceTransition;
    final RootWindowContainer mRootWindowContainer;
    int mRotationAnimationHint;
    ActivityServiceConnectionsHolder mServiceConnectionsHolder;
    final boolean mShowForAllUsers;
    private boolean mShowWhenLocked;
    private Rect mSizeCompatBounds;
    private float mSizeCompatScale;
    private SizeConfigurationBuckets mSizeConfigurations;
    boolean mSplashScreenStyleSolidColor;
    StartingData mStartingData;
    StartingSurfaceController.StartingSurface mStartingSurface;
    WindowState mStartingWindow;
    private State mState;
    private final Runnable mStopTimeoutRunnable;
    private final boolean mStyleFillsParent;
    int mTargetSdk;
    private boolean mTaskOverlay;
    final ActivityTaskSupervisor mTaskSupervisor;
    private final Rect mTmpBounds;
    private final Configuration mTmpConfig;
    private final Rect mTmpOutNonDecorBounds;
    private final Runnable mTransferSplashScreenTimeoutRunnable;
    int mTransferringSplashScreenState;
    private boolean mTurnScreenOn;
    boolean mUseTransferredAnimation;
    final int mUserId;
    private boolean mVisible;
    public boolean mVisibleByWakeupOptimizePath;
    boolean mVisibleRequested;
    private boolean mVisibleSetFromTransferredStartingWindow;
    boolean mVoiceInteraction;
    boolean mWaitForEnteringPinnedMode;
    private boolean mWillCloseOrEnterPip;
    boolean mWindowIsFloating;
    ArrayList<ReferrerIntent> newIntents;
    boolean noDisplay;
    private CharSequence nonLocalizedLabel;
    boolean nowVisible;
    public final String packageName;
    long pauseTime;
    HashSet<WeakReference<PendingIntentRecord>> pendingResults;
    boolean pendingVoiceInteractionStart;
    PictureInPictureParams pictureInPictureArgs;
    boolean preserveWindowOnDeferredRelaunch;
    final String processName;
    boolean reportedVisible;
    final int requestCode;
    ComponentName requestedVrComponent;
    final String resolvedType;
    ActivityRecord resultTo;
    final String resultWho;
    ArrayList<ResultInfo> results;
    ActivityOptions returningOptions;
    final boolean rootVoiceInteraction;
    final Binder shareableActivityToken;
    final String shortComponentName;
    boolean shouldDockBigOverlays;
    public boolean shouldHidePreWakeupQ;
    boolean startingDisplayed;
    boolean startingMoved;
    final boolean stateNotNeeded;
    boolean stopped;
    boolean supportsEnterPipOnTaskSwitch;
    private Task task;
    final String taskAffinity;
    ActivityManager.TaskDescription taskDescription;
    private int theme;
    long topResumedStateLossTime;
    UriPermissionOwner uriPermissions;
    boolean visibleIgnoringKeyguard;
    IVoiceInteractionSession voiceSession;
    final WindowManagerService wmsService;
    private static final String TAG = "ActivityTaskManager";
    private static final String TAG_ADD_REMOVE = TAG + ActivityTaskManagerDebugConfig.POSTFIX_ADD_REMOVE;
    private static final String TAG_APP = TAG + ActivityTaskManagerDebugConfig.POSTFIX_APP;
    private static final String TAG_CONFIGURATION = TAG + ActivityTaskManagerDebugConfig.POSTFIX_CONFIGURATION;
    private static final String TAG_CONTAINERS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_CONTAINERS;
    private static final String TAG_FOCUS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_FOCUS;
    private static final String TAG_PAUSE = TAG + ActivityTaskManagerDebugConfig.POSTFIX_PAUSE;
    private static final String TAG_RESULTS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_RESULTS;
    private static final String TAG_SAVED_STATE = TAG + ActivityTaskManagerDebugConfig.POSTFIX_SAVED_STATE;
    private static final String TAG_STATES = TAG + ActivityTaskManagerDebugConfig.POSTFIX_STATES;
    private static final String TAG_SWITCH = TAG + ActivityTaskManagerDebugConfig.POSTFIX_SWITCH;
    private static final String TAG_TRANSITION = TAG + ActivityTaskManagerDebugConfig.POSTFIX_TRANSITION;
    private static final String TAG_USER_LEAVING = TAG + ActivityTaskManagerDebugConfig.POSTFIX_USER_LEAVING;
    private static final String TAG_VISIBILITY = TAG + ActivityTaskManagerDebugConfig.POSTFIX_VISIBILITY;
    static boolean ENABLE_SPLASHSCREEN_WHITELIST = SystemProperties.getBoolean("persist.sys.splash_screen.enable_whitelist", true);
    static String DEBUG_SPLASHSCREEN_WHITELIST = SystemProperties.get("persist.sys.splash_screen.debug_whitelist");

    /* loaded from: classes2.dex */
    @interface FinishRequest {
    }

    /* loaded from: classes2.dex */
    @interface LaunchSourceType {
    }

    /* loaded from: classes2.dex */
    @interface SplashScreenBehavior {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public enum State {
        INITIALIZING,
        STARTED,
        RESUMED,
        PAUSING,
        PAUSED,
        STOPPING,
        STOPPED,
        FINISHING,
        DESTROYING,
        DESTROYED,
        RESTARTING_PROCESS
    }

    /* loaded from: classes2.dex */
    @interface TransferSplashScreenState {
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ void commitPendingTransaction() {
        super.commitPendingTransaction();
    }

    @Override // com.android.server.wm.WindowContainer
    public /* bridge */ /* synthetic */ int compareTo(WindowContainer windowContainer) {
        return super.compareTo(windowContainer);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl getAnimationLeash() {
        return super.getAnimationLeash();
    }

    @Override // com.android.server.wm.WindowContainer
    public /* bridge */ /* synthetic */ DisplayContent getDisplayContent() {
        return super.getDisplayContent();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceFreezer.Freezable
    public /* bridge */ /* synthetic */ SurfaceControl getFreezeSnapshotTarget() {
        return super.getFreezeSnapshotTarget();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl getParentSurfaceControl() {
        return super.getParentSurfaceControl();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl.Transaction getPendingTransaction() {
        return super.getPendingTransaction();
    }

    @Override // com.android.server.wm.WindowContainer
    public /* bridge */ /* synthetic */ SparseArray getProvidedInsetsSources() {
        return super.getProvidedInsetsSources();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl getSurfaceControl() {
        return super.getSurfaceControl();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ int getSurfaceHeight() {
        return super.getSurfaceHeight();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ int getSurfaceWidth() {
        return super.getSurfaceWidth();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl.Transaction getSyncTransaction() {
        return super.getSyncTransaction();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public /* bridge */ /* synthetic */ boolean isOSFullDialog() {
        return super.isOSFullDialog();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ SurfaceControl.Builder makeAnimationLeash() {
        return super.makeAnimationLeash();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public /* bridge */ /* synthetic */ void onAnimationLeashCreated(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl) {
        super.onAnimationLeashCreated(transaction, surfaceControl);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public /* bridge */ /* synthetic */ void onRequestedOverrideConfigurationChanged(Configuration configuration) {
        super.onRequestedOverrideConfigurationChanged(configuration);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceFreezer.Freezable
    public /* bridge */ /* synthetic */ void onUnfrozen() {
        super.onUnfrozen();
    }

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        SPLASHSCREEN_WHITELIST = arrayList;
        arrayList.add("com.whatsapp");
        arrayList.addAll(Arrays.asList(DEBUG_SPLASHSCREEN_WHITELIST.split(";")));
    }

    private void updateEnterpriseThumbnailDrawable(final Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        this.mEnterpriseThumbnailDrawable = dpm.getResources().getDrawable("WORK_PROFILE_ICON", "OUTLINE", "PROFILE_SWITCH_ANIMATION", new Supplier() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda30
            @Override // java.util.function.Supplier
            public final Object get() {
                Drawable drawable;
                drawable = context.getDrawable(17302424);
                return drawable;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$2$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ void m7782lambda$new$2$comandroidserverwmActivityRecord(final float[] matrix, final float[] translation) {
        this.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda29
            @Override // java.lang.Runnable
            public final void run() {
                ActivityRecord.this.m7781lambda$new$1$comandroidserverwmActivityRecord(matrix, translation);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$1$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ void m7781lambda$new$1$comandroidserverwmActivityRecord(float[] matrix, float[] translation) {
        synchronized (this.mWmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mLastAppSaturationInfo == null) {
                    this.mLastAppSaturationInfo = new AppSaturationInfo();
                }
                this.mLastAppSaturationInfo.setSaturation(matrix, translation);
                updateColorTransform();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowToken, com.android.server.wm.WindowContainer
    @NeverCompile
    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        String str;
        long now = SystemClock.uptimeMillis();
        pw.print(prefix);
        pw.print("packageName=");
        pw.print(this.packageName);
        pw.print(" processName=");
        pw.println(this.processName);
        pw.print(prefix);
        pw.print("launchedFromUid=");
        pw.print(this.launchedFromUid);
        pw.print(" launchedFromPackage=");
        pw.print(this.launchedFromPackage);
        pw.print(" launchedFromFeature=");
        pw.print(this.launchedFromFeatureId);
        pw.print(" userId=");
        pw.println(this.mUserId);
        pw.print(prefix);
        pw.print("app=");
        pw.println(this.app);
        pw.print(prefix);
        pw.println(this.intent.toInsecureString());
        pw.print(prefix);
        pw.print("rootOfTask=");
        pw.print(isRootOfTask());
        pw.print(" task=");
        pw.println(this.task);
        pw.print(prefix);
        pw.print("taskAffinity=");
        pw.println(this.taskAffinity);
        pw.print(prefix);
        pw.print("mActivityComponent=");
        pw.println(this.mActivityComponent.flattenToShortString());
        ActivityInfo activityInfo = this.info;
        if (activityInfo != null && activityInfo.applicationInfo != null) {
            ApplicationInfo appInfo = this.info.applicationInfo;
            pw.print(prefix);
            pw.print("baseDir=");
            pw.println(appInfo.sourceDir);
            if (!Objects.equals(appInfo.sourceDir, appInfo.publicSourceDir)) {
                pw.print(prefix);
                pw.print("resDir=");
                pw.println(appInfo.publicSourceDir);
            }
            pw.print(prefix);
            pw.print("dataDir=");
            pw.println(appInfo.dataDir);
            if (appInfo.splitSourceDirs != null) {
                pw.print(prefix);
                pw.print("splitDir=");
                pw.println(Arrays.toString(appInfo.splitSourceDirs));
            }
        }
        pw.print(prefix);
        pw.print("stateNotNeeded=");
        pw.print(this.stateNotNeeded);
        pw.print(prefix);
        pw.print("isForceDisablePreview=");
        pw.print(this.isForceDisablePreview);
        pw.print(" componentSpecified=");
        pw.print(this.componentSpecified);
        pw.print(" mActivityType=");
        pw.println(WindowConfiguration.activityTypeToString(getActivityType()));
        if (this.rootVoiceInteraction) {
            pw.print(prefix);
            pw.print("rootVoiceInteraction=");
            pw.println(this.rootVoiceInteraction);
        }
        pw.print(prefix);
        pw.print("compat=");
        pw.print(this.compat);
        pw.print(" labelRes=0x");
        pw.print(Integer.toHexString(this.labelRes));
        pw.print(" icon=0x");
        pw.print(Integer.toHexString(this.icon));
        pw.print(" theme=0x");
        pw.println(Integer.toHexString(this.theme));
        pw.println(prefix + "mLastReportedConfigurations:");
        this.mLastReportedConfiguration.dump(pw, prefix + "  ");
        pw.print(prefix);
        pw.print("CurrentConfiguration=");
        pw.println(getConfiguration());
        if (!getRequestedOverrideConfiguration().equals(Configuration.EMPTY)) {
            pw.println(prefix + "RequestedOverrideConfiguration=" + getRequestedOverrideConfiguration());
        }
        if (!getResolvedOverrideConfiguration().equals(getRequestedOverrideConfiguration())) {
            pw.println(prefix + "ResolvedOverrideConfiguration=" + getResolvedOverrideConfiguration());
        }
        if (!matchParentBounds()) {
            pw.println(prefix + "bounds=" + getBounds());
        }
        if (this.resultTo != null || this.resultWho != null) {
            pw.print(prefix);
            pw.print("resultTo=");
            pw.print(this.resultTo);
            pw.print(" resultWho=");
            pw.print(this.resultWho);
            pw.print(" resultCode=");
            pw.println(this.requestCode);
        }
        ActivityManager.TaskDescription taskDescription = this.taskDescription;
        if (taskDescription != null) {
            String iconFilename = taskDescription.getIconFilename();
            if (iconFilename != null || this.taskDescription.getLabel() != null || this.taskDescription.getPrimaryColor() != 0) {
                pw.print(prefix);
                pw.print("taskDescription:");
                pw.print(" label=\"");
                pw.print(this.taskDescription.getLabel());
                pw.print("\"");
                pw.print(" icon=");
                if (this.taskDescription.getInMemoryIcon() != null) {
                    str = this.taskDescription.getInMemoryIcon().getByteCount() + " bytes";
                } else {
                    str = "null";
                }
                pw.print(str);
                pw.print(" iconResource=");
                pw.print(this.taskDescription.getIconResourcePackage());
                pw.print(SliceClientPermissions.SliceAuthority.DELIMITER);
                pw.print(this.taskDescription.getIconResource());
                pw.print(" iconFilename=");
                pw.print(this.taskDescription.getIconFilename());
                pw.print(" primaryColor=");
                pw.println(Integer.toHexString(this.taskDescription.getPrimaryColor()));
                pw.print(prefix);
                pw.print("  backgroundColor=");
                pw.print(Integer.toHexString(this.taskDescription.getBackgroundColor()));
                pw.print(" statusBarColor=");
                pw.print(Integer.toHexString(this.taskDescription.getStatusBarColor()));
                pw.print(" navigationBarColor=");
                pw.println(Integer.toHexString(this.taskDescription.getNavigationBarColor()));
                pw.print(prefix);
                pw.print(" backgroundColorFloating=");
                pw.println(Integer.toHexString(this.taskDescription.getBackgroundColorFloating()));
            }
        }
        if (this.results != null) {
            pw.print(prefix);
            pw.print("results=");
            pw.println(this.results);
        }
        HashSet<WeakReference<PendingIntentRecord>> hashSet = this.pendingResults;
        if (hashSet != null && hashSet.size() > 0) {
            pw.print(prefix);
            pw.println("Pending Results:");
            Iterator<WeakReference<PendingIntentRecord>> it = this.pendingResults.iterator();
            while (it.hasNext()) {
                WeakReference<PendingIntentRecord> wpir = it.next();
                PendingIntentRecord pir = wpir != null ? wpir.get() : null;
                pw.print(prefix);
                pw.print("  - ");
                if (pir == null) {
                    pw.println("null");
                } else {
                    pw.println(pir);
                    pir.dump(pw, prefix + "    ");
                }
            }
        }
        ArrayList<ReferrerIntent> arrayList = this.newIntents;
        if (arrayList != null && arrayList.size() > 0) {
            pw.print(prefix);
            pw.println("Pending New Intents:");
            for (int i = 0; i < this.newIntents.size(); i++) {
                Intent intent = this.newIntents.get(i);
                pw.print(prefix);
                pw.print("  - ");
                if (intent == null) {
                    pw.println("null");
                } else {
                    pw.println(intent.toShortString(false, true, false, false));
                }
            }
        }
        if (this.mPendingOptions != null) {
            pw.print(prefix);
            pw.print("pendingOptions=");
            pw.println(this.mPendingOptions);
        }
        if (this.mPendingRemoteAnimation != null) {
            pw.print(prefix);
            pw.print("pendingRemoteAnimationCallingPid=");
            pw.println(this.mPendingRemoteAnimation.getCallingPid());
        }
        if (this.mPendingRemoteTransition != null) {
            pw.print(prefix + " pendingRemoteTransition=" + this.mPendingRemoteTransition.getRemoteTransition());
        }
        AppTimeTracker appTimeTracker = this.appTimeTracker;
        if (appTimeTracker != null) {
            appTimeTracker.dumpWithHeader(pw, prefix, false);
        }
        UriPermissionOwner uriPermissionOwner = this.uriPermissions;
        if (uriPermissionOwner != null) {
            uriPermissionOwner.dump(pw, prefix);
        }
        pw.print(prefix);
        pw.print("launchFailed=");
        pw.print(this.launchFailed);
        pw.print(" launchCount=");
        pw.print(this.launchCount);
        pw.print(" lastLaunchTime=");
        long j = this.lastLaunchTime;
        if (j == 0) {
            pw.print("0");
        } else {
            TimeUtils.formatDuration(j, now, pw);
        }
        pw.println();
        if (this.mLaunchCookie != null) {
            pw.print(prefix);
            pw.print("launchCookie=");
            pw.println(this.mLaunchCookie);
        }
        if (this.mLaunchRootTask != null) {
            pw.print(prefix);
            pw.print("mLaunchRootTask=");
            pw.println(this.mLaunchRootTask);
        }
        pw.print(prefix);
        pw.print("mHaveState=");
        pw.print(this.mHaveState);
        pw.print(" mIcicle=");
        pw.println(this.mIcicle);
        pw.print(prefix);
        pw.print("state=");
        pw.print(this.mState);
        pw.print(" stopped=");
        pw.print(this.stopped);
        pw.print(" delayedResume=");
        pw.print(this.delayedResume);
        pw.print(" finishing=");
        pw.println(this.finishing);
        pw.print(prefix);
        pw.print("keysPaused=");
        pw.print(this.keysPaused);
        pw.print(" inHistory=");
        pw.print(this.inHistory);
        pw.print(" idle=");
        pw.println(this.idle);
        pw.print(prefix);
        pw.print("occludesParent=");
        pw.print(occludesParent());
        pw.print(" noDisplay=");
        pw.print(this.noDisplay);
        pw.print(" immersive=");
        pw.print(this.immersive);
        pw.print(" launchMode=");
        pw.println(this.launchMode);
        pw.print(prefix);
        pw.print("frozenBeforeDestroy=");
        pw.print(this.frozenBeforeDestroy);
        pw.print(" forceNewConfig=");
        pw.println(this.forceNewConfig);
        pw.print(prefix);
        pw.print("mActivityType=");
        pw.println(WindowConfiguration.activityTypeToString(getActivityType()));
        pw.print(prefix);
        pw.print("mImeInsetsFrozenUntilStartInput=");
        pw.println(this.mImeInsetsFrozenUntilStartInput);
        if (this.requestedVrComponent != null) {
            pw.print(prefix);
            pw.print("requestedVrComponent=");
            pw.println(this.requestedVrComponent);
        }
        super.dump(pw, prefix, dumpAll);
        if (this.mVoiceInteraction) {
            pw.println(prefix + "mVoiceInteraction=true");
        }
        pw.print(prefix);
        pw.print("mOccludesParent=");
        pw.println(this.mOccludesParent);
        pw.print(prefix);
        pw.print("mOrientation=");
        pw.println(ActivityInfo.screenOrientationToString(this.mOrientation));
        pw.println(prefix + "mVisibleRequested=" + this.mVisibleRequested + " mVisible=" + this.mVisible + " mClientVisible=" + isClientVisible() + (this.mDeferHidingClient ? " mDeferHidingClient=" + this.mDeferHidingClient : "") + " reportedDrawn=" + this.mReportedDrawn + " reportedVisible=" + this.reportedVisible);
        if (this.paused) {
            pw.print(prefix);
            pw.print("paused=");
            pw.println(this.paused);
        }
        if (this.mAppStopped) {
            pw.print(prefix);
            pw.print("mAppStopped=");
            pw.println(this.mAppStopped);
        }
        if (this.mNumInterestingWindows != 0 || this.mNumDrawnWindows != 0 || this.allDrawn || this.mLastAllDrawn) {
            pw.print(prefix);
            pw.print("mNumInterestingWindows=");
            pw.print(this.mNumInterestingWindows);
            pw.print(" mNumDrawnWindows=");
            pw.print(this.mNumDrawnWindows);
            pw.print(" allDrawn=");
            pw.print(this.allDrawn);
            pw.print(" lastAllDrawn=");
            pw.print(this.mLastAllDrawn);
            pw.println(")");
        }
        if (this.mStartingData != null || this.firstWindowDrawn || this.mIsExiting) {
            pw.print(prefix);
            pw.print("startingData=");
            pw.print(this.mStartingData);
            pw.print(" firstWindowDrawn=");
            pw.print(this.firstWindowDrawn);
            pw.print(" mIsExiting=");
            pw.println(this.mIsExiting);
        }
        if (this.mStartingWindow != null || this.mStartingData != null || this.mStartingSurface != null || this.startingMoved || this.mVisibleSetFromTransferredStartingWindow) {
            pw.print(prefix);
            pw.print("startingWindow=");
            pw.print(this.mStartingWindow);
            pw.print(" startingSurface=");
            pw.print(this.mStartingSurface);
            pw.print(" startingDisplayed=");
            pw.print(isStartingWindowDisplayed());
            pw.print(" startingMoved=");
            pw.print(this.startingMoved);
            pw.println(" mVisibleSetFromTransferredStartingWindow=" + this.mVisibleSetFromTransferredStartingWindow);
        }
        if (this.mPendingRelaunchCount != 0) {
            pw.print(prefix);
            pw.print("mPendingRelaunchCount=");
            pw.println(this.mPendingRelaunchCount);
        }
        if (this.mSizeCompatScale != 1.0f || this.mSizeCompatBounds != null) {
            pw.println(prefix + "mSizeCompatScale=" + this.mSizeCompatScale + " mSizeCompatBounds=" + this.mSizeCompatBounds);
        }
        if (this.mRemovingFromDisplay) {
            pw.println(prefix + "mRemovingFromDisplay=" + this.mRemovingFromDisplay);
        }
        if (this.lastVisibleTime != 0 || this.nowVisible) {
            pw.print(prefix);
            pw.print("nowVisible=");
            pw.print(this.nowVisible);
            pw.print(" lastVisibleTime=");
            long j2 = this.lastVisibleTime;
            if (j2 == 0) {
                pw.print("0");
            } else {
                TimeUtils.formatDuration(j2, now, pw);
            }
            pw.println();
        }
        if (this.mDeferHidingClient) {
            pw.println(prefix + "mDeferHidingClient=" + this.mDeferHidingClient);
        }
        if (this.deferRelaunchUntilPaused || this.configChangeFlags != 0) {
            pw.print(prefix);
            pw.print("deferRelaunchUntilPaused=");
            pw.print(this.deferRelaunchUntilPaused);
            pw.print(" configChangeFlags=");
            pw.println(Integer.toHexString(this.configChangeFlags));
        }
        if (this.mServiceConnectionsHolder != null) {
            pw.print(prefix);
            pw.print("connections=");
            pw.println(this.mServiceConnectionsHolder);
        }
        if (this.info != null) {
            pw.println(prefix + "resizeMode=" + ActivityInfo.resizeModeToString(this.info.resizeMode));
            pw.println(prefix + "mLastReportedMultiWindowMode=" + this.mLastReportedMultiWindowMode + " mLastReportedPictureInPictureMode=" + this.mLastReportedPictureInPictureMode);
            if (this.info.supportsPictureInPicture()) {
                pw.println(prefix + "supportsPictureInPicture=" + this.info.supportsPictureInPicture());
                pw.println(prefix + "supportsEnterPipOnTaskSwitch: " + this.supportsEnterPipOnTaskSwitch);
            }
            if (this.info.getMaxAspectRatio() != 0.0f) {
                pw.println(prefix + "maxAspectRatio=" + this.info.getMaxAspectRatio());
            }
            float minAspectRatio = getMinAspectRatio();
            if (minAspectRatio != 0.0f) {
                pw.println(prefix + "minAspectRatio=" + minAspectRatio);
            }
            if (minAspectRatio != this.info.getManifestMinAspectRatio()) {
                pw.println(prefix + "manifestMinAspectRatio=" + this.info.getManifestMinAspectRatio());
            }
            pw.println(prefix + "supportsSizeChanges=" + ActivityInfo.sizeChangesSupportModeToString(this.info.supportsSizeChanges()));
            if (this.info.configChanges != 0) {
                pw.println(prefix + "configChanges=0x" + Integer.toHexString(this.info.configChanges));
            }
            pw.println(prefix + "neverSandboxDisplayApis=" + this.info.neverSandboxDisplayApis(sConstrainDisplayApisConfig));
            pw.println(prefix + "alwaysSandboxDisplayApis=" + this.info.alwaysSandboxDisplayApis(sConstrainDisplayApisConfig));
        }
        if (this.mLastParentBeforePip != null) {
            pw.println(prefix + "lastParentTaskIdBeforePip=" + this.mLastParentBeforePip.mTaskId);
        }
        if (this.mLaunchIntoPipHostActivity != null) {
            pw.println(prefix + "launchIntoPipHostActivity=" + this.mLaunchIntoPipHostActivity);
        }
        this.mLetterboxUiController.dump(pw, prefix);
        pw.println(prefix + "mCameraCompatControlState=" + TaskInfo.cameraCompatControlStateToString(this.mCameraCompatControlState));
        pw.println(prefix + "mCameraCompatControlEnabled=" + this.mCameraCompatControlEnabled);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean dumpActivity(FileDescriptor fd, PrintWriter pw, int index, ActivityRecord r, String prefix, String label, boolean complete, boolean brief, boolean client, String dumpPackage, boolean needNL, Runnable header, Task lastTask) {
        TransferPipe tp;
        if (dumpPackage == null || dumpPackage.equals(r.packageName)) {
            boolean full = !brief && (complete || !r.isInHistory());
            if (needNL) {
                pw.println("");
            }
            if (header != null) {
                header.run();
            }
            String innerPrefix = prefix + "  ";
            String[] args = new String[0];
            if (lastTask != r.getTask()) {
                Task lastTask2 = r.getTask();
                pw.print(prefix);
                pw.print(full ? "* " : "  ");
                pw.println(lastTask2);
                if (full) {
                    lastTask2.dump(pw, prefix + "  ");
                } else if (complete && lastTask2.intent != null) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.println(lastTask2.intent.toInsecureString());
                }
            }
            pw.print(prefix);
            pw.print(full ? "* " : "    ");
            pw.print(label);
            pw.print(" #");
            pw.print(index);
            pw.print(": ");
            pw.println(r);
            if (full) {
                r.dump(pw, innerPrefix, true);
            } else if (complete) {
                pw.print(innerPrefix);
                pw.println(r.intent.toInsecureString());
                if (r.app != null) {
                    pw.print(innerPrefix);
                    pw.println(r.app);
                }
            }
            if (client && r.attachedToProcess()) {
                pw.flush();
                try {
                    tp = new TransferPipe();
                } catch (RemoteException e) {
                } catch (IOException e2) {
                    e = e2;
                }
                try {
                    try {
                        r.app.getThread().dumpActivity(tp.getWriteFd(), r.token, innerPrefix, args);
                        try {
                            tp.go(fd, 2000L);
                            tp.kill();
                        } catch (Throwable th) {
                            th = th;
                            tp.kill();
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (RemoteException e3) {
                    pw.println(innerPrefix + "Got a RemoteException while dumping the activity");
                    return true;
                } catch (IOException e4) {
                    e = e4;
                    pw.println(innerPrefix + "Failure while dumping the activity: " + e);
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAppTimeTracker(AppTimeTracker att) {
        this.appTimeTracker = att;
    }

    void setSavedState(Bundle savedState) {
        this.mIcicle = savedState;
        this.mHaveState = savedState != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle getSavedState() {
        return this.mIcicle;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSavedState() {
        return this.mHaveState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PersistableBundle getPersistentSavedState() {
        return this.mPersistentState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateApplicationInfo(ApplicationInfo aInfo) {
        this.info.applicationInfo = aInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSizeConfigurations(SizeConfigurationBuckets sizeConfigurations) {
        this.mSizeConfigurations = sizeConfigurations;
    }

    private void scheduleActivityMovedToDisplay(int displayId, Configuration config) {
        if (!attachedToProcess()) {
            if (ProtoLogCache.WM_DEBUG_SWITCH_enabled) {
                String protoLogParam0 = String.valueOf(this);
                long protoLogParam1 = displayId;
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_SWITCH, -1495062622, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
                return;
            }
            return;
        }
        try {
            if (ProtoLogCache.WM_DEBUG_SWITCH_enabled) {
                String protoLogParam02 = String.valueOf(this);
                long protoLogParam12 = displayId;
                String protoLogParam2 = String.valueOf(config);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_SWITCH, 374506950, 4, (String) null, new Object[]{protoLogParam02, Long.valueOf(protoLogParam12), protoLogParam2});
            }
            this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ClientTransactionItem) MoveToDisplayItem.obtain(displayId, config));
        } catch (RemoteException e) {
        }
    }

    private void scheduleConfigurationChanged(Configuration config) {
        if (!attachedToProcess()) {
            if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_CONFIGURATION, 1040675582, 0, (String) null, new Object[]{protoLogParam0});
                return;
            }
            return;
        }
        try {
            if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                String protoLogParam02 = String.valueOf(this);
                String protoLogParam1 = String.valueOf(config);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, 969323241, 0, (String) null, new Object[]{protoLogParam02, protoLogParam1});
            }
            this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ClientTransactionItem) ActivityConfigurationChangeItem.obtain(config));
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean scheduleTopResumedActivityChanged(boolean onTop) {
        if (!attachedToProcess()) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_STATES, -1193946201, 0, (String) null, new Object[]{protoLogParam0});
            }
            return false;
        }
        if (onTop) {
            this.app.addToPendingTop();
        }
        try {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam02 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1305966693, 12, (String) null, new Object[]{protoLogParam02, Boolean.valueOf(onTop)});
            }
            this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ClientTransactionItem) TopResumedActivityChangeItem.obtain(onTop));
            if (onTop) {
                this.mAtmService.mAmsExt.onActivityStateChanged(this, onTop);
            }
            return true;
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to send top-resumed=" + onTop + " to " + this, e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateMultiWindowMode() {
        boolean inMultiWindowMode;
        Task task = this.task;
        if (task == null || task.getRootTask() == null || !attachedToProcess() || (inMultiWindowMode = inMultiWindowMode()) == this.mLastReportedMultiWindowMode) {
            return;
        }
        if (!inMultiWindowMode && this.mLastReportedPictureInPictureMode) {
            updatePictureInPictureMode(null, false);
            return;
        }
        this.mLastReportedMultiWindowMode = inMultiWindowMode;
        ensureActivityConfiguration(0, true, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updatePictureInPictureMode(Rect targetRootTaskBounds, boolean forceUpdate) {
        Task task = this.task;
        if (task == null || task.getRootTask() == null || !attachedToProcess()) {
            return;
        }
        boolean inPictureInPictureMode = inPinnedWindowingMode() && targetRootTaskBounds != null;
        if (inPictureInPictureMode != this.mLastReportedPictureInPictureMode || forceUpdate) {
            this.mLastReportedPictureInPictureMode = inPictureInPictureMode;
            this.mLastReportedMultiWindowMode = inPictureInPictureMode;
            ensureActivityConfiguration(0, true, true);
            if (inPictureInPictureMode && findMainWindow() == null) {
                EventLog.writeEvent(1397638484, "265293293", -1, "");
                removeImmediately();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTask() {
        return this.task;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragment getTaskFragment() {
        WindowContainer parent = getParent();
        if (parent != null) {
            return parent.asTaskFragment();
        }
        return null;
    }

    private boolean shouldStartChangeTransition(TaskFragment newParent, TaskFragment oldParent) {
        return (newParent == null || oldParent == null || !canStartChangeTransition() || !newParent.isOrganizedTaskFragment() || newParent.getBounds().equals(oldParent.getBounds())) ? false : true;
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    void onParentChanged(ConfigurationContainer rawNewParent, ConfigurationContainer rawOldParent) {
        StartingData startingData;
        TaskFragment oldParent = (TaskFragment) rawOldParent;
        TaskFragment newParent = (TaskFragment) rawNewParent;
        Task oldTask = oldParent != null ? oldParent.getTask() : null;
        Task newTask = newParent != null ? newParent.getTask() : null;
        this.task = newTask;
        if (shouldStartChangeTransition(newParent, oldParent)) {
            newParent.initializeChangeTransition(getBounds(), getSurfaceControl());
        }
        super.onParentChanged(newParent, oldParent);
        if (isPersistable()) {
            if (oldTask != null) {
                this.mAtmService.notifyTaskPersisterLocked(oldTask, false);
            }
            if (newTask != null) {
                this.mAtmService.notifyTaskPersisterLocked(newTask, false);
            }
        }
        if (oldParent == null && newParent != null) {
            this.mVoiceInteraction = newTask.voiceSession != null;
            newTask.updateOverrideConfigurationFromLaunchBounds();
            this.mLastReportedMultiWindowMode = inMultiWindowMode();
            this.mLastReportedPictureInPictureMode = inPinnedWindowingMode();
        }
        if (this.task == null && getDisplayContent() != null) {
            getDisplayContent().mClosingApps.remove(this);
        }
        Task rootTask = getRootTask();
        updateAnimatingActivityRegistry();
        Task task = this.task;
        if (task == this.mLastParentBeforePip && task != null) {
            this.mAtmService.mWindowOrganizerController.mTaskFragmentOrganizerController.onActivityReparentToTask(this);
            clearLastParentBeforePip();
        }
        updateColorTransform();
        if (oldParent != null) {
            oldParent.cleanUpActivityReferences(this);
        }
        if (newParent != null && isState(State.RESUMED)) {
            newParent.setResumedActivity(this, "onParentChanged");
            if (this.mStartingWindow != null && (startingData = this.mStartingData) != null && startingData.mAssociatedTask == null && newParent.isEmbedded()) {
                associateStartingDataWithTask();
                attachStartingSurfaceToAssociatedTask();
            }
            this.mImeInsetsFrozenUntilStartInput = false;
        }
        if (rootTask != null && rootTask.topRunningActivity() == this && this.firstWindowDrawn) {
            rootTask.setHasBeenVisible(true);
        }
        updateUntrustedEmbeddingInputProtection();
        if (this.mInSizeCompatModeForBounds && newParent != null && newParent.isEmbedded()) {
            clearSizeCompatMode();
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void setSurfaceControl(SurfaceControl sc) {
        super.setSurfaceControl(sc);
        if (sc != null) {
            this.mLastDropInputMode = 0;
            updateUntrustedEmbeddingInputProtection();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDropInputForAnimation(boolean isInputDroppedForAnimation) {
        if (this.mIsInputDroppedForAnimation == isInputDroppedForAnimation) {
            return;
        }
        this.mIsInputDroppedForAnimation = isInputDroppedForAnimation;
        updateUntrustedEmbeddingInputProtection();
    }

    private void updateUntrustedEmbeddingInputProtection() {
        if (getSurfaceControl() == null) {
            return;
        }
        if (this.mIsInputDroppedForAnimation) {
            setDropInputMode(1);
        } else if (isEmbeddedInUntrustedMode()) {
            setDropInputMode(2);
        } else {
            setDropInputMode(0);
        }
    }

    void setDropInputMode(int mode) {
        if (this.mLastDropInputMode != mode) {
            this.mLastDropInputMode = mode;
            this.mWmService.mTransactionFactory.get().setDropInputMode(getSurfaceControl(), mode).apply();
        }
    }

    private boolean isEmbeddedInUntrustedMode() {
        TaskFragment organizedTaskFragment = getOrganizedTaskFragment();
        if (organizedTaskFragment == null) {
            return false;
        }
        return !organizedTaskFragment.isAllowedToEmbedActivityInTrustedMode(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAnimatingActivityRegistry() {
        AnimatingActivityRegistry registry;
        Task rootTask = getRootTask();
        if (rootTask != null) {
            registry = rootTask.getAnimatingActivityRegistry();
        } else {
            registry = null;
        }
        AnimatingActivityRegistry animatingActivityRegistry = this.mAnimatingActivityRegistry;
        if (animatingActivityRegistry != null && animatingActivityRegistry != registry) {
            animatingActivityRegistry.notifyFinished(this);
        }
        this.mAnimatingActivityRegistry = registry;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastParentBeforePip(ActivityRecord launchIntoPipHostActivity) {
        Task task;
        TaskFragment organizedTf;
        ITaskFragmentOrganizer iTaskFragmentOrganizer;
        if (launchIntoPipHostActivity == null) {
            task = getTask();
        } else {
            task = launchIntoPipHostActivity.getTask();
        }
        this.mLastParentBeforePip = task;
        task.mChildPipActivity = this;
        this.mLaunchIntoPipHostActivity = launchIntoPipHostActivity;
        if (launchIntoPipHostActivity == null) {
            organizedTf = getOrganizedTaskFragment();
        } else {
            organizedTf = launchIntoPipHostActivity.getOrganizedTaskFragment();
        }
        if (organizedTf != null) {
            iTaskFragmentOrganizer = organizedTf.getTaskFragmentOrganizer();
        } else {
            iTaskFragmentOrganizer = null;
        }
        this.mLastTaskFragmentOrganizerBeforePip = iTaskFragmentOrganizer;
    }

    private void clearLastParentBeforePip() {
        Task task = this.mLastParentBeforePip;
        if (task != null) {
            task.mChildPipActivity = null;
            this.mLastParentBeforePip = null;
        }
        this.mLaunchIntoPipHostActivity = null;
        this.mLastTaskFragmentOrganizerBeforePip = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getLastParentBeforePip() {
        return this.mLastParentBeforePip;
    }

    ActivityRecord getLaunchIntoPipHostActivity() {
        return this.mLaunchIntoPipHostActivity;
    }

    private void updateColorTransform() {
        if (this.mSurfaceControl != null && this.mLastAppSaturationInfo != null) {
            getPendingTransaction().setColorTransform(this.mSurfaceControl, this.mLastAppSaturationInfo.mMatrix, this.mLastAppSaturationInfo.mTranslation);
            this.mWmService.scheduleAnimationLocked();
        }
    }

    public void removeOpeningApps() {
        if (this.mDisplayContent != null && this.mDisplayContent.mOpeningApps.contains(this)) {
            this.mDisplayContent.mOpeningApps.remove(this);
        }
    }

    @Override // com.android.server.wm.WindowToken, com.android.server.wm.WindowContainer
    void onDisplayChanged(DisplayContent dc) {
        DisplayContent prevDc = this.mDisplayContent;
        super.onDisplayChanged(dc);
        if (prevDc == this.mDisplayContent) {
            return;
        }
        this.mDisplayContent.onRunningActivityChanged();
        if (prevDc == null) {
            return;
        }
        prevDc.onRunningActivityChanged();
        this.mTransitionController.collect(this);
        if (prevDc.mOpeningApps.remove(this)) {
            this.mDisplayContent.mOpeningApps.add(this);
            this.mDisplayContent.transferAppTransitionFrom(prevDc);
            this.mDisplayContent.executeAppTransition();
        }
        prevDc.mClosingApps.remove(this);
        if (prevDc.mFocusedApp == this) {
            prevDc.setFocusedApp(null);
            if (dc.getTopMostActivity() == this) {
                dc.setFocusedApp(this);
            }
        }
        this.mLetterboxUiController.onMovedToDisplay(this.mDisplayContent.getDisplayId());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void layoutLetterbox(WindowState winHint) {
        this.mLetterboxUiController.layoutLetterbox(winHint);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasWallpaperBackgroudForLetterbox() {
        return this.mLetterboxUiController.hasWallpaperBackgroudForLetterbox();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLetterboxSurface(WindowState winHint) {
        this.mLetterboxUiController.updateLetterboxSurface(winHint);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getLetterboxInsets() {
        return this.mLetterboxUiController.getLetterboxInsets();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getLetterboxInnerBounds(Rect outBounds) {
        this.mLetterboxUiController.getLetterboxInnerBounds(outBounds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateCameraCompatState(boolean showControl, boolean transformationApplied, ICompatCameraControlCallback callback) {
        int newCameraCompatControlState;
        if (!isCameraCompatControlEnabled()) {
            return;
        }
        if (this.mCameraCompatControlClickedByUser && (showControl || this.mCameraCompatControlState == 3)) {
            return;
        }
        this.mCompatCameraControlCallback = callback;
        if (!showControl) {
            newCameraCompatControlState = 0;
        } else if (transformationApplied) {
            newCameraCompatControlState = 2;
        } else {
            newCameraCompatControlState = 1;
        }
        boolean changed = setCameraCompatControlState(newCameraCompatControlState);
        if (!changed) {
            return;
        }
        this.mTaskSupervisor.getActivityMetricsLogger().logCameraCompatControlAppearedEventReported(newCameraCompatControlState, this.info.applicationInfo.uid);
        if (newCameraCompatControlState == 0) {
            this.mCameraCompatControlClickedByUser = false;
            this.mCompatCameraControlCallback = null;
        }
        getTask().dispatchTaskInfoChangedIfNeeded(true);
        getDisplayContent().setLayoutNeeded();
        this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateCameraCompatStateFromUser(int state) {
        if (isCameraCompatControlEnabled()) {
            if (state == 0) {
                Slog.w(TAG, "Unexpected hidden state in updateCameraCompatState");
                return;
            }
            boolean changed = setCameraCompatControlState(state);
            this.mCameraCompatControlClickedByUser = true;
            if (!changed) {
                return;
            }
            this.mTaskSupervisor.getActivityMetricsLogger().logCameraCompatControlClickedEventReported(state, this.info.applicationInfo.uid);
            if (state == 3) {
                this.mCompatCameraControlCallback = null;
                return;
            }
            ICompatCameraControlCallback iCompatCameraControlCallback = this.mCompatCameraControlCallback;
            if (iCompatCameraControlCallback == null) {
                Slog.w(TAG, "Callback for a camera compat control is null");
                return;
            }
            try {
                if (state == 2) {
                    iCompatCameraControlCallback.applyCameraCompatTreatment();
                } else {
                    iCompatCameraControlCallback.revertCameraCompatTreatment();
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Unable to apply or revert camera compat treatment", e);
            }
        }
    }

    private boolean setCameraCompatControlState(int state) {
        if (isCameraCompatControlEnabled() && this.mCameraCompatControlState != state) {
            this.mCameraCompatControlState = state;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCameraCompatControlState() {
        return this.mCameraCompatControlState;
    }

    boolean isCameraCompatControlEnabled() {
        return this.mCameraCompatControlEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFullyTransparentBarAllowed(Rect rect) {
        return this.mLetterboxUiController.isFullyTransparentBarAllowed(rect);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class Token extends Binder {
        WeakReference<ActivityRecord> mActivityRef;

        private Token() {
        }

        public String toString() {
            return "Token{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.mActivityRef.get() + "}";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ActivityRecord forTokenLocked(IBinder token) {
        if (token == null) {
            return null;
        }
        try {
            Token activityToken = (Token) token;
            ActivityRecord r = activityToken.mActivityRef.get();
            if (r == null || r.getRootTask() == null) {
                return null;
            }
            return r;
        } catch (ClassCastException e) {
            Slog.w(TAG, "Bad activity token: " + token, e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isResolverActivity(String className) {
        return ResolverActivity.class.getName().equals(className) || "com.transsion.resolver.ResolverActivity".equals(className);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isResolverOrDelegateActivity() {
        return isResolverActivity(this.mActivityComponent.getClassName()) || Objects.equals(this.mActivityComponent, this.mAtmService.mTaskSupervisor.getSystemChooserActivity());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isResolverOrChildActivity() {
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(this.packageName) || "com.transsion.resolver".equals(this.packageName)) {
            try {
                if (!"com.transsion.resolver.ResolverActivity".equals(this.mActivityComponent.getClassName()) && !"com.transsion.resolver.ChooserActivity".equals(this.mActivityComponent.getClassName())) {
                    if (!ResolverActivity.class.isAssignableFrom(Object.class.getClassLoader().loadClass(this.mActivityComponent.getClassName()))) {
                        return false;
                    }
                }
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:124:0x0392  */
    /* JADX WARN: Removed duplicated region for block: B:127:0x03c6  */
    /* JADX WARN: Removed duplicated region for block: B:128:0x03c9  */
    /* JADX WARN: Removed duplicated region for block: B:131:0x03d1  */
    /* JADX WARN: Removed duplicated region for block: B:132:0x03d3  */
    /* JADX WARN: Removed duplicated region for block: B:135:0x03e3  */
    /* JADX WARN: Removed duplicated region for block: B:145:0x0454  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private ActivityRecord(ActivityTaskManagerService _service, WindowProcessController _caller, int _launchedFromPid, int _launchedFromUid, String _launchedFromPackage, String _launchedFromFeature, Intent _intent, String _resolvedType, ActivityInfo aInfo, Configuration _configuration, ActivityRecord _resultTo, String _resultWho, int _reqCode, boolean _componentSpecified, boolean _rootVoiceInteraction, ActivityTaskSupervisor supervisor, ActivityOptions options, ActivityRecord sourceRecord, PersistableBundle persistentState, ActivityManager.TaskDescription _taskDescription, long _createTime) {
        super(_service.mWindowManager, new Token(), 2, true, null, false);
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        int realTheme;
        String uid;
        Bundle metaData;
        Bundle metaData2;
        boolean z5;
        this.mMinAspectRatioForUser = -1.0f;
        this.mIsPkgInActivityEmbedding = false;
        this.mWindowIsFloating = false;
        this.shouldHidePreWakeupQ = false;
        this.mHandoverLaunchDisplayId = -1;
        this.createTime = System.currentTimeMillis();
        this.mHaveState = true;
        this.pictureInPictureArgs = new PictureInPictureParams.Builder().build();
        this.mSplashScreenStyleSolidColor = false;
        this.mTaskOverlay = false;
        this.mRelaunchReason = 0;
        this.mRemovingFromDisplay = false;
        this.mReportedVisibilityResults = new WindowState.UpdateReportedVisibilityResults();
        this.mCurrentLaunchCanTurnScreenOn = true;
        this.mLastSurfaceShowing = true;
        this.mInputDispatchingTimeoutMillis = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
        this.mLastTransactionSequence = Long.MIN_VALUE;
        this.mLastAllReadyAtSync = false;
        this.mSizeCompatScale = 1.0f;
        this.mInSizeCompatModeForBounds = false;
        this.mIsAspectRatioApplied = false;
        this.mCameraCompatControlState = 0;
        this.mEnableRecentsScreenshot = true;
        this.mLastDropInputMode = 0;
        this.mTransferringSplashScreenState = 0;
        this.mRotationAnimationHint = -1;
        ColorDisplayService.ColorTransformController colorTransformController = new ColorDisplayService.ColorTransformController() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda28
            @Override // com.android.server.display.color.ColorDisplayService.ColorTransformController
            public final void applyAppSaturation(float[] fArr, float[] fArr2) {
                ActivityRecord.this.m7782lambda$new$2$comandroidserverwmActivityRecord(fArr, fArr2);
            }
        };
        this.mColorTransformController = colorTransformController;
        this.mPnpProp = null;
        this.mTmpConfig = new Configuration();
        this.mTmpBounds = new Rect();
        this.mTmpOutNonDecorBounds = new Rect();
        this.assistToken = new Binder();
        this.shareableActivityToken = new Binder();
        this.mPauseTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.ActivityRecord.1
            @Override // java.lang.Runnable
            public void run() {
                Slog.w(ActivityRecord.TAG, "Activity pause timeout for " + ActivityRecord.this);
                synchronized (ActivityRecord.this.mAtmService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (!ActivityRecord.this.hasProcess()) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        ActivityRecord.this.mAtmService.logAppTooSlow(ActivityRecord.this.app, ActivityRecord.this.pauseTime, "pausing " + ActivityRecord.this);
                        ActivityRecord.this.activityPaused(true);
                        WindowManagerService.resetPriorityAfterLockedSection();
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            }
        };
        this.mLaunchTickRunnable = new Runnable() { // from class: com.android.server.wm.ActivityRecord.2
            @Override // java.lang.Runnable
            public void run() {
                synchronized (ActivityRecord.this.mAtmService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        if (ActivityRecord.this.continueLaunchTicking()) {
                            ActivityRecord.this.mAtmService.logAppTooSlow(ActivityRecord.this.app, ActivityRecord.this.launchTickTime, "launching " + ActivityRecord.this);
                        }
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        };
        this.mDestroyTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.ActivityRecord.3
            @Override // java.lang.Runnable
            public void run() {
                synchronized (ActivityRecord.this.mAtmService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        Slog.w(ActivityRecord.TAG, "Activity destroy timeout for " + ActivityRecord.this);
                        ActivityRecord.this.destroyed("destroyTimeout");
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        };
        this.mStopTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.ActivityRecord.4
            @Override // java.lang.Runnable
            public void run() {
                synchronized (ActivityRecord.this.mAtmService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        Slog.w(ActivityRecord.TAG, "Activity stop timeout for " + ActivityRecord.this);
                        if (ActivityRecord.this.isInHistory()) {
                            ActivityRecord.this.activityStopped(null, null, null);
                        }
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        };
        this.mAddStartingWindow = new AddStartingWindow();
        this.mTransferSplashScreenTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.ActivityRecord.5
            @Override // java.lang.Runnable
            public void run() {
                synchronized (ActivityRecord.this.mAtmService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        Slog.w(ActivityRecord.TAG, "Activity transferring splash screen timeout for " + ActivityRecord.this + " state " + ActivityRecord.this.mTransferringSplashScreenState);
                        if (ActivityRecord.this.isTransferringSplashScreen()) {
                            ActivityRecord.this.mTransferringSplashScreenState = 3;
                            ActivityRecord.this.removeStartingWindow();
                        }
                    } catch (Throwable th) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        };
        this.mForceResizeable = false;
        this.mNeedShowAfterEnterPinnedMode = false;
        this.mDeferShowForEnteringPinnedMode = false;
        this.mAtmService = _service;
        ((Token) this.token).mActivityRef = new WeakReference<>(this);
        this.wmsService = _service.mWindowManager;
        this.info = aInfo;
        int userId = UserHandle.getUserId(aInfo.applicationInfo.uid);
        this.mUserId = userId;
        String str = aInfo.applicationInfo.packageName;
        this.packageName = str;
        this.intent = _intent;
        if (aInfo.targetActivity == null || (aInfo.targetActivity.equals(_intent.getComponent().getClassName()) && (aInfo.launchMode == 0 || aInfo.launchMode == 1))) {
            this.mActivityComponent = _intent.getComponent();
        } else {
            this.mActivityComponent = new ComponentName(aInfo.packageName, aInfo.targetActivity);
        }
        this.mTargetSdk = aInfo.applicationInfo.targetSdkVersion;
        if ((aInfo.flags & 1024) == 0) {
            z = false;
        } else {
            z = true;
        }
        this.mShowForAllUsers = z;
        if (options == null) {
            setOrientation(aInfo.screenOrientation);
        } else {
            int overlayOrientation = options.getOverlayOrientation();
            Slog.i(TAG, "overlay orientation " + overlayOrientation);
            if (overlayOrientation == -2) {
                setOrientation(aInfo.screenOrientation);
            } else {
                setOrientation(overlayOrientation);
            }
        }
        this.mRotationAnimationHint = aInfo.rotationAnimation;
        if ((aInfo.flags & 8388608) == 0) {
            z2 = false;
        } else {
            z2 = true;
        }
        this.mShowWhenLocked = z2;
        if ((aInfo.privateFlags & 1) == 0) {
            z3 = false;
        } else {
            z3 = true;
        }
        this.mInheritShownWhenLocked = z3;
        if ((aInfo.flags & 16777216) == 0) {
            z4 = false;
        } else {
            z4 = true;
        }
        this.mTurnScreenOn = z4;
        int realTheme2 = aInfo.getThemeResource();
        if (realTheme2 != 0) {
            realTheme = realTheme2;
        } else {
            realTheme = aInfo.applicationInfo.targetSdkVersion < 11 ? 16973829 : 16973931;
        }
        AttributeCache.Entry ent = AttributeCache.instance().get(str, realTheme, R.styleable.Window, userId);
        if (ent != null) {
            if (ActivityInfo.isTranslucentOrFloating(ent.array) && !ent.array.getBoolean(14, false)) {
                z5 = false;
            } else {
                z5 = true;
            }
            this.mOccludesParent = z5;
            this.mStyleFillsParent = z5;
            this.noDisplay = ent.array.getBoolean(10, false);
            this.mWindowIsFloating = ent.array.getBoolean(4, false);
        } else {
            this.mOccludesParent = true;
            this.mStyleFillsParent = true;
            this.noDisplay = false;
        }
        if (options != null) {
            this.mLaunchTaskBehind = options.getLaunchTaskBehind();
            int rotationAnimation = options.getRotationAnimationHint();
            if (rotationAnimation >= 0) {
                this.mRotationAnimationHint = rotationAnimation;
            }
            if (options.getLaunchIntoPipParams() != null) {
                this.pictureInPictureArgs = options.getLaunchIntoPipParams();
            }
            this.mOverrideTaskTransition = options.getOverrideTaskTransition();
            this.mDismissKeyguardIfInsecure = options.getDismissKeyguardIfInsecure();
        }
        ColorDisplayService.ColorDisplayServiceInternal cds = (ColorDisplayService.ColorDisplayServiceInternal) LocalServices.getService(ColorDisplayService.ColorDisplayServiceInternal.class);
        cds.attachColorTransformController(str, userId, new WeakReference<>(colorTransformController));
        this.mRootWindowContainer = _service.mRootWindowContainer;
        this.launchedFromPid = _launchedFromPid;
        this.launchedFromUid = _launchedFromUid;
        this.launchedFromPackage = _launchedFromPackage;
        this.launchedFromFeatureId = _launchedFromFeature;
        this.mLaunchSourceType = determineLaunchSourceType(_launchedFromUid, _caller);
        this.shortComponentName = _intent.getComponent().flattenToShortString();
        this.resolvedType = _resolvedType;
        this.componentSpecified = _componentSpecified;
        this.rootVoiceInteraction = _rootVoiceInteraction;
        this.mLastReportedConfiguration = new MergedConfiguration(_configuration);
        this.resultTo = _resultTo;
        this.resultWho = _resultWho;
        this.requestCode = _reqCode;
        setState(State.INITIALIZING, "ActivityRecord ctor");
        this.launchFailed = false;
        this.stopped = false;
        this.delayedResume = false;
        this.finishing = false;
        this.deferRelaunchUntilPaused = false;
        this.keysPaused = false;
        this.inHistory = false;
        this.nowVisible = false;
        super.setClientVisible(true);
        this.idle = false;
        this.hasBeenLaunched = false;
        this.mTaskSupervisor = supervisor;
        aInfo.taskAffinity = computeTaskAffinity(aInfo.taskAffinity, aInfo.applicationInfo.uid, this.launchMode);
        this.taskAffinity = aInfo.taskAffinity;
        String uid2 = Integer.toString(aInfo.applicationInfo.uid);
        if (aInfo.windowLayout == null || aInfo.windowLayout.windowLayoutAffinity == null) {
            uid = uid2;
        } else if (aInfo.windowLayout.windowLayoutAffinity.startsWith(uid2)) {
            uid = uid2;
        } else {
            uid = uid2;
            aInfo.windowLayout.windowLayoutAffinity = uid2 + ":" + aInfo.windowLayout.windowLayoutAffinity;
        }
        if (sConstrainDisplayApisConfig == null) {
            sConstrainDisplayApisConfig = new ConstrainDisplayApisConfig();
        }
        this.stateNotNeeded = (aInfo.flags & 16) != 0;
        try {
            metaData = null;
        } catch (PackageManager.NameNotFoundException e) {
            metaData = null;
        }
        try {
            ActivityInfo activityInfo = _service.mContext.getPackageManager().getActivityInfo(this.mActivityComponent, 128);
            Bundle metaData3 = activityInfo.metaData;
            metaData2 = metaData3;
        } catch (PackageManager.NameNotFoundException e2) {
            metaData2 = metaData;
            this.isForceDisablePreview = (metaData2 == null && metaData2.getBoolean("com.transsion.feature.straringwindow.force_disable_preview")) || isInSplashScreenWhiteList(this.mActivityComponent);
            this.isForceEnableCustomTransition = metaData2 == null && metaData2.getBoolean("com.transsion.feature.transition.force_enable_custom_transition");
            this.isForceNotRelaunchForCarMode = metaData2 == null && metaData2.getBoolean("com.transsion.feature.force_not_relaunch_for_carmode");
            this.nonLocalizedLabel = aInfo.nonLocalizedLabel;
            int i = aInfo.labelRes;
            this.labelRes = i;
            if (this.nonLocalizedLabel == null) {
                ApplicationInfo app = aInfo.applicationInfo;
                this.nonLocalizedLabel = app.nonLocalizedLabel;
                this.labelRes = app.labelRes;
            }
            this.icon = aInfo.getIconResource();
            this.theme = aInfo.getThemeResource();
            if ((aInfo.flags & 1) != 0) {
            }
            this.processName = aInfo.processName;
            if ((aInfo.flags & 32) != 0) {
            }
            this.launchMode = aInfo.launchMode;
            this.grifAppInfo = ITranActivityRecord.Instance().hookInitial(this.packageName);
            setActivityType(_componentSpecified, _launchedFromUid, _intent, options, sourceRecord);
            this.immersive = (aInfo.flags & 2048) == 0;
            this.requestedVrComponent = aInfo.requestedVrComponent != null ? null : ComponentName.unflattenFromString(aInfo.requestedVrComponent);
            this.lockTaskLaunchMode = getLockTaskLaunchMode(aInfo, options);
            if (options != null) {
            }
            this.mPersistentState = persistentState;
            this.taskDescription = _taskDescription;
            this.mLetterboxUiController = new LetterboxUiController(this.mWmService, this);
            this.mCameraCompatControlEnabled = this.mWmService.mContext.getResources().getBoolean(17891685);
            this.shouldDockBigOverlays = this.mWmService.mContext.getResources().getBoolean(17891606);
            if (_createTime > 0) {
            }
            this.mAtmService.mPackageConfigPersister.updateConfigIfNeeded(this, this.mUserId, this.packageName);
            this.mActivityRecordInputSink = new ActivityRecordInputSink(this, sourceRecord);
            updateEnterpriseThumbnailDrawable(this.mAtmService.mUiContext);
            IActivityRecordLice.instance().onConstruct(this, options, this.info, sourceRecord, this.mAtmService.mContext);
        }
        this.isForceDisablePreview = (metaData2 == null && metaData2.getBoolean("com.transsion.feature.straringwindow.force_disable_preview")) || isInSplashScreenWhiteList(this.mActivityComponent);
        this.isForceEnableCustomTransition = metaData2 == null && metaData2.getBoolean("com.transsion.feature.transition.force_enable_custom_transition");
        this.isForceNotRelaunchForCarMode = metaData2 == null && metaData2.getBoolean("com.transsion.feature.force_not_relaunch_for_carmode");
        this.nonLocalizedLabel = aInfo.nonLocalizedLabel;
        int i2 = aInfo.labelRes;
        this.labelRes = i2;
        if (this.nonLocalizedLabel == null && i2 == 0) {
            ApplicationInfo app2 = aInfo.applicationInfo;
            this.nonLocalizedLabel = app2.nonLocalizedLabel;
            this.labelRes = app2.labelRes;
        }
        this.icon = aInfo.getIconResource();
        this.theme = aInfo.getThemeResource();
        if ((aInfo.flags & 1) != 0 || _caller == null || (aInfo.applicationInfo.uid != 1000 && aInfo.applicationInfo.uid != _caller.mInfo.uid)) {
            this.processName = aInfo.processName;
        } else {
            this.processName = _caller.mName;
        }
        if ((aInfo.flags & 32) != 0) {
            this.intent.addFlags(8388608);
        }
        this.launchMode = aInfo.launchMode;
        this.grifAppInfo = ITranActivityRecord.Instance().hookInitial(this.packageName);
        setActivityType(_componentSpecified, _launchedFromUid, _intent, options, sourceRecord);
        this.immersive = (aInfo.flags & 2048) == 0;
        this.requestedVrComponent = aInfo.requestedVrComponent != null ? null : ComponentName.unflattenFromString(aInfo.requestedVrComponent);
        this.lockTaskLaunchMode = getLockTaskLaunchMode(aInfo, options);
        if (options != null) {
            setOptions(options);
            PendingIntent usageReport = options.getUsageTimeReport();
            if (usageReport != null) {
                this.appTimeTracker = new AppTimeTracker(usageReport);
            }
            WindowContainerToken daToken = options.getLaunchTaskDisplayArea();
            this.mHandoverTaskDisplayArea = daToken != null ? (TaskDisplayArea) WindowContainer.fromBinder(daToken.asBinder()) : null;
            this.mHandoverLaunchDisplayId = options.getLaunchDisplayId();
            this.mLaunchCookie = options.getLaunchCookie();
            this.mLaunchRootTask = options.getLaunchRootTask();
        }
        this.mPersistentState = persistentState;
        this.taskDescription = _taskDescription;
        this.mLetterboxUiController = new LetterboxUiController(this.mWmService, this);
        this.mCameraCompatControlEnabled = this.mWmService.mContext.getResources().getBoolean(17891685);
        this.shouldDockBigOverlays = this.mWmService.mContext.getResources().getBoolean(17891606);
        if (_createTime > 0) {
            this.createTime = _createTime;
        }
        this.mAtmService.mPackageConfigPersister.updateConfigIfNeeded(this, this.mUserId, this.packageName);
        this.mActivityRecordInputSink = new ActivityRecordInputSink(this, sourceRecord);
        updateEnterpriseThumbnailDrawable(this.mAtmService.mUiContext);
        IActivityRecordLice.instance().onConstruct(this, options, this.info, sourceRecord, this.mAtmService.mContext);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMinAspectRatioForUser(float minAspectRatioForUser) {
        this.mMinAspectRatioForUser = minAspectRatioForUser;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsPkgInActivityEmbedding(boolean isPkgInActivityEmbedding) {
        this.mIsPkgInActivityEmbedding = isPkgInActivityEmbedding;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String computeTaskAffinity(String affinity, int uid, int launchMode) {
        String uidStr = Integer.toString(uid);
        if (affinity == null || affinity.startsWith(uidStr)) {
            return affinity;
        }
        return uidStr + (launchMode == 3 ? "-si:" : ":") + affinity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getLockTaskLaunchMode(ActivityInfo aInfo, ActivityOptions options) {
        int lockTaskLaunchMode = aInfo.lockTaskLaunchMode;
        if (!aInfo.applicationInfo.isPrivilegedApp() && (lockTaskLaunchMode == 2 || lockTaskLaunchMode == 1)) {
            lockTaskLaunchMode = 0;
        }
        if (options != null) {
            boolean useLockTask = options.getLockTaskMode();
            if (useLockTask && lockTaskLaunchMode == 0) {
                return 3;
            }
            return lockTaskLaunchMode;
        }
        return lockTaskLaunchMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InputApplicationHandle getInputApplicationHandle(boolean update) {
        if (this.mInputApplicationHandle == null) {
            this.mInputApplicationHandle = new InputApplicationHandle(this.token, toString(), this.mInputDispatchingTimeoutMillis);
        } else if (update) {
            String name = toString();
            if (this.mInputDispatchingTimeoutMillis != this.mInputApplicationHandle.dispatchingTimeoutMillis || !name.equals(this.mInputApplicationHandle.name)) {
                this.mInputApplicationHandle = new InputApplicationHandle(this.token, name, this.mInputDispatchingTimeoutMillis);
            }
        }
        return this.mInputApplicationHandle;
    }

    @Override // com.android.server.wm.WindowContainer
    ActivityRecord asActivityRecord() {
        return this;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean hasActivity() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setProcess(WindowProcessController proc) {
        this.app = proc;
        Task task = this.task;
        ActivityRecord root = task != null ? task.getRootActivity() : null;
        if (root == this) {
            this.task.setRootProcess(proc);
        }
        proc.addActivityIfNeeded(this);
        this.mInputDispatchingTimeoutMillis = ActivityTaskManagerService.getInputDispatchingTimeoutMillisLocked(this);
        TaskFragment tf = getTaskFragment();
        if (tf != null) {
            tf.sendTaskFragmentInfoChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasProcess() {
        return this.app != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean attachedToProcess() {
        return hasProcess() && this.app.hasThread();
    }

    private int evaluateStartingWindowTheme(ActivityRecord prev, String pkg, int originalTheme, int replaceTheme) {
        if (!validateStartingWindowTheme(prev, pkg, originalTheme)) {
            return 0;
        }
        if (replaceTheme == 0 || !validateStartingWindowTheme(prev, pkg, replaceTheme)) {
            return originalTheme;
        }
        return replaceTheme;
    }

    private boolean launchedFromSystemSurface() {
        int i = this.mLaunchSourceType;
        return i == 1 || i == 2 || i == 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLaunchSourceType(int type) {
        return this.mLaunchSourceType == type;
    }

    private int determineLaunchSourceType(int launchFromUid, WindowProcessController caller) {
        if (launchFromUid == 1000 || launchFromUid == 0) {
            return 1;
        }
        if (caller != null) {
            if (caller.isHomeProcess()) {
                return 2;
            }
            if (this.mAtmService.getSysUiServiceComponentLocked().getPackageName().equals(caller.mInfo.packageName)) {
                return 3;
            }
            return 4;
        }
        return 4;
    }

    private boolean validateStartingWindowTheme(ActivityRecord prev, String pkg, int theme) {
        AttributeCache.Entry ent;
        if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
            long protoLogParam0 = theme;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -1782453012, 1, (String) null, new Object[]{Long.valueOf(protoLogParam0)});
        }
        if (theme == 0 || (ent = AttributeCache.instance().get(pkg, theme, R.styleable.Window, this.mWmService.mCurrentUserId)) == null) {
            return false;
        }
        boolean windowIsTranslucent = ent.array.getBoolean(5, false);
        boolean windowIsFloating = ent.array.getBoolean(4, false);
        boolean windowShowWallpaper = ent.array.getBoolean(14, false);
        boolean windowDisableStarting = ent.array.getBoolean(12, false);
        if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
            String protoLogParam02 = String.valueOf(windowIsTranslucent);
            String protoLogParam1 = String.valueOf(windowIsFloating);
            String protoLogParam2 = String.valueOf(windowShowWallpaper);
            String protoLogParam3 = String.valueOf(windowDisableStarting);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -124316973, 0, (String) null, new Object[]{protoLogParam02, protoLogParam1, protoLogParam2, protoLogParam3});
        }
        this.mFloatOrTranslucent = windowIsFloating;
        if (windowIsTranslucent || windowIsFloating) {
            return false;
        }
        if (!windowShowWallpaper || getDisplayContent().mWallpaperController.getWallpaperTarget() == null) {
            if (windowDisableStarting && this.isForceDisablePreview) {
                return false;
            }
            if (!windowDisableStarting || launchedFromSystemSurface()) {
                return true;
            }
            return prev != null && prev.getActivityType() == 1 && prev.mTransferringSplashScreenState == 0 && !(prev.mStartingData == null && (prev.mStartingWindow == null || prev.mStartingSurface == null));
        }
        return false;
    }

    boolean addStartingWindow(String pkg, int resolvedTheme, ActivityRecord from, boolean newTask, boolean taskSwitch, boolean processRunning, boolean allowTaskSnapshot, boolean activityCreated, boolean isSimple, boolean activityAllDrawn) {
        if (okToDisplay() && this.mStartingData == null) {
            WindowState mainWin = findMainWindow();
            if (mainWin == null || !mainWin.mWinAnimator.getShown()) {
                TaskSnapshot snapshot = this.mWmService.mTaskSnapshotController.getSnapshot(this.task.mTaskId, this.task.mUserId, false, false);
                int type = getStartingWindowType(newTask, taskSwitch, processRunning, allowTaskSnapshot, activityCreated, activityAllDrawn, snapshot);
                boolean useLegacy = type == 2 && this.mWmService.mStartingSurfaceController.isExceptionApp(this.packageName, this.mTargetSdk, new Supplier() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda6
                    @Override // java.util.function.Supplier
                    public final Object get() {
                        return ActivityRecord.this.m7775lambda$addStartingWindow$3$comandroidserverwmActivityRecord();
                    }
                });
                int typeParameter = StartingSurfaceController.makeStartingWindowTypeParameter(newTask, taskSwitch, processRunning, allowTaskSnapshot, activityCreated, isSimple, useLegacy, activityAllDrawn, type, this.packageName, this.mUserId);
                if (type == 1) {
                    if (isActivityTypeHome()) {
                        this.mWmService.mTaskSnapshotController.removeSnapshotCache(this.task.mTaskId);
                        if ((2 & this.mDisplayContent.mAppTransition.getTransitFlags()) == 0) {
                            return false;
                        }
                    }
                    return createSnapshot(snapshot, typeParameter);
                } else if (resolvedTheme != 0 || this.theme == 0) {
                    if (from == null || !transferStartingWindow(from)) {
                        if (type != 2) {
                            return false;
                        }
                        if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 2018852077, 0, (String) null, (Object[]) null);
                        }
                        this.mStartingData = new SplashScreenStartingData(this.mWmService, resolvedTheme, typeParameter);
                        scheduleAddStartingWindow();
                        return true;
                    }
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addStartingWindow$3$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ ApplicationInfo m7775lambda$addStartingWindow$3$comandroidserverwmActivityRecord() {
        ActivityInfo activityInfo = this.intent.resolveActivityInfo(this.mAtmService.mContext.getPackageManager(), 128);
        if (activityInfo != null) {
            return activityInfo.applicationInfo;
        }
        return null;
    }

    private boolean createSnapshot(TaskSnapshot snapshot, int typeParams) {
        if (snapshot == null) {
            return false;
        }
        if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -1340540100, 0, (String) null, (Object[]) null);
        }
        this.mStartingData = new SnapshotStartingData(this.mWmService, snapshot, typeParams);
        if (this.task.forAllLeafTaskFragments(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda31
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((TaskFragment) obj).isEmbeddedWithBoundsOverride();
            }
        })) {
            associateStartingDataWithTask();
        }
        scheduleAddStartingWindow();
        return true;
    }

    void scheduleAddStartingWindow() {
        this.mAddStartingWindow.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class AddStartingWindow implements Runnable {
        private AddStartingWindow() {
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
        @Override // java.lang.Runnable
        public void run() {
            synchronized (ActivityRecord.this.mWmService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (ActivityRecord.this.mStartingData == null) {
                        if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                            String protoLogParam0 = String.valueOf(ActivityRecord.this);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 1836214582, 0, (String) null, new Object[]{protoLogParam0});
                        }
                    } else if (ActivityRecord.this.mStartingSurface != null) {
                        Slog.v(WmsExt.TAG, ActivityRecord.this + "already has a starting surface!!!");
                        WindowManagerService.resetPriorityAfterLockedSection();
                    } else {
                        StartingData startingData = ActivityRecord.this.mStartingData;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                            String protoLogParam02 = String.valueOf(this);
                            String protoLogParam1 = String.valueOf(startingData);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 108170907, 0, (String) null, new Object[]{protoLogParam02, protoLogParam1});
                        }
                        StartingSurfaceController.StartingSurface surface = null;
                        try {
                            surface = startingData.createStartingSurface(ActivityRecord.this);
                        } catch (Exception e) {
                            Slog.w(ActivityRecord.TAG, "Exception when adding starting window", e);
                        }
                        if (surface != null) {
                            boolean abort = false;
                            synchronized (ActivityRecord.this.mWmService.mGlobalLock) {
                                try {
                                    WindowManagerService.boostPriorityForLockedSection();
                                    if (ActivityRecord.this.mStartingData == null) {
                                        if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                                            String protoLogParam03 = String.valueOf(ActivityRecord.this);
                                            String protoLogParam12 = String.valueOf(ActivityRecord.this.mStartingData);
                                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 1822843721, 0, (String) null, new Object[]{protoLogParam03, protoLogParam12});
                                        }
                                        ActivityRecord.this.mStartingWindow = null;
                                        ActivityRecord.this.mStartingData = null;
                                        abort = true;
                                    } else {
                                        ActivityRecord.this.mStartingSurface = surface;
                                    }
                                    if (!abort && ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                                        String protoLogParam04 = String.valueOf(ActivityRecord.this);
                                        String protoLogParam13 = String.valueOf(ActivityRecord.this.mStartingWindow);
                                        String protoLogParam2 = String.valueOf(ActivityRecord.this.mStartingSurface);
                                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -1292329638, 0, (String) null, new Object[]{protoLogParam04, protoLogParam13, protoLogParam2});
                                    }
                                } finally {
                                }
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            if (abort) {
                                surface.remove(false);
                            }
                        } else if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                            String protoLogParam05 = String.valueOf(ActivityRecord.this);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 1254403969, 0, (String) null, new Object[]{protoLogParam05});
                        }
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }

    private int getStartingWindowType(boolean newTask, boolean taskSwitch, boolean processRunning, boolean allowTaskSnapshot, boolean activityCreated, boolean activityAllDrawn, TaskSnapshot snapshot) {
        ActivityRecord topAttached;
        if (!newTask && taskSwitch && processRunning && !activityCreated && this.task.intent != null && this.mActivityComponent.equals(this.task.intent.getComponent()) && (topAttached = this.task.getActivity(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda13
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((ActivityRecord) obj).attachedToProcess();
            }
        })) != null) {
            return (topAttached.isSnapshotCompatible(snapshot) && this.mDisplayContent.getDisplayRotation().rotationForOrientation(this.mOrientation, this.mDisplayContent.getRotation()) == snapshot.getRotation()) ? 1 : 0;
        }
        boolean isActivityHome = isActivityTypeHome();
        if ((newTask || !processRunning || (taskSwitch && !activityCreated)) && !isActivityHome) {
            return 2;
        }
        if (taskSwitch) {
            if (allowTaskSnapshot) {
                if (isSnapshotCompatible(snapshot)) {
                    if (ITranActivityRecord.Instance().isSkipSnapshot(this.packageName)) {
                        return 0;
                    }
                    return (this.newIntents == null || !"com.sh.smart.caller".equals(this.packageName)) ? 1 : 0;
                } else if (!isActivityHome) {
                    return 2;
                }
            }
            if (!activityAllDrawn && !isActivityHome) {
                return 2;
            }
        }
        return 0;
    }

    boolean isSnapshotCompatible(TaskSnapshot snapshot) {
        int targetRotation;
        if (snapshot == null || !snapshot.getTopActivityComponent().equals(this.mActivityComponent)) {
            return false;
        }
        int rotation = this.mDisplayContent.rotationForActivityInDifferentOrientation(this);
        int currentRotation = this.task.getWindowConfiguration().getRotation();
        if (rotation != -1) {
            targetRotation = rotation;
        } else {
            targetRotation = currentRotation;
        }
        if (snapshot.getRotation() != targetRotation) {
            return false;
        }
        Rect taskBounds = this.task.getBounds();
        int w = taskBounds.width();
        int h = taskBounds.height();
        Point taskSize = snapshot.getTaskSize();
        if (Math.abs(currentRotation - targetRotation) % 2 == 1) {
            w = h;
            h = w;
        }
        int t = taskSize.x;
        return Math.abs((((float) t) / ((float) Math.max(taskSize.y, 1))) - (((float) w) / ((float) Math.max(h, 1)))) <= 0.01f;
    }

    void setCustomizeSplashScreenExitAnimation(boolean enable) {
        if (this.mHandleExitSplashScreen == enable) {
            return;
        }
        this.mHandleExitSplashScreen = enable;
    }

    private void scheduleTransferSplashScreenTimeout() {
        this.mAtmService.mH.postDelayed(this.mTransferSplashScreenTimeoutRunnable, 2000L);
    }

    private void removeTransferSplashScreenTimeout() {
        this.mAtmService.mH.removeCallbacks(this.mTransferSplashScreenTimeoutRunnable);
    }

    private boolean transferSplashScreenIfNeeded() {
        if (this.finishing || !this.mHandleExitSplashScreen || this.mStartingSurface == null || this.mStartingWindow == null || this.mTransferringSplashScreenState == 3) {
            return false;
        }
        if (isTransferringSplashScreen()) {
            return true;
        }
        requestCopySplashScreen();
        return isTransferringSplashScreen();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTransferringSplashScreen() {
        int i = this.mTransferringSplashScreenState;
        return i == 2 || i == 1;
    }

    private void requestCopySplashScreen() {
        this.mTransferringSplashScreenState = 1;
        if (!this.mAtmService.mTaskOrganizerController.copySplashScreenView(getTask())) {
            this.mTransferringSplashScreenState = 3;
            removeStartingWindow();
        }
        scheduleTransferSplashScreenTimeout();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCopySplashScreenFinish(SplashScreenView.SplashScreenViewParcelable parcelable) {
        WindowState windowState;
        removeTransferSplashScreenTimeout();
        if (parcelable == null || this.mTransferringSplashScreenState != 1 || (windowState = this.mStartingWindow) == null || this.finishing) {
            if (parcelable != null) {
                parcelable.clearIfNeeded();
            }
            this.mTransferringSplashScreenState = 3;
            removeStartingWindow();
            return;
        }
        SurfaceControl windowAnimationLeash = TaskOrganizerController.applyStartingWindowAnimation(windowState);
        try {
            this.mTransferringSplashScreenState = 2;
            this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ClientTransactionItem) TransferSplashScreenViewStateItem.obtain(parcelable, windowAnimationLeash));
            scheduleTransferSplashScreenTimeout();
        } catch (Exception e) {
            Slog.w(TAG, "onCopySplashScreenComplete fail: " + this);
            this.mStartingWindow.cancelAnimation();
            parcelable.clearIfNeeded();
            this.mTransferringSplashScreenState = 3;
        }
    }

    private void onSplashScreenAttachComplete() {
        removeTransferSplashScreenTimeout();
        WindowState windowState = this.mStartingWindow;
        if (windowState != null) {
            windowState.cancelAnimation();
            this.mStartingWindow.hide(false, false);
        }
        this.mTransferringSplashScreenState = 3;
        removeStartingWindowAnimation(false);
    }

    void cleanUpSplashScreen() {
        if (!this.mHandleExitSplashScreen || this.startingMoved) {
            return;
        }
        int i = this.mTransferringSplashScreenState;
        if (i == 3 || i == 0) {
            if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -1003678883, 0, (String) null, new Object[]{protoLogParam0});
            }
            this.mAtmService.mTaskOrganizerController.onAppSplashScreenViewRemoved(getTask());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isStartingWindowDisplayed() {
        StartingData data = this.mStartingData;
        if (data == null) {
            Task task = this.task;
            data = task != null ? task.mSharedStartingData : null;
        }
        return data != null && data.mIsDisplayed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void attachStartingWindow(WindowState startingWindow) {
        startingWindow.mStartingData = this.mStartingData;
        this.mStartingWindow = startingWindow;
        StartingData startingData = this.mStartingData;
        if (startingData != null) {
            if (startingData.mAssociatedTask != null) {
                attachStartingSurfaceToAssociatedTask();
            } else if (isEmbedded()) {
                associateStartingWindowWithTaskIfNeeded();
            }
        }
    }

    private void attachStartingSurfaceToAssociatedTask() {
        overrideConfigurationPropagation(this.mStartingWindow, this.mStartingData.mAssociatedTask);
        getSyncTransaction().reparent(this.mStartingWindow.mSurfaceControl, this.mStartingData.mAssociatedTask.mSurfaceControl);
    }

    private void associateStartingDataWithTask() {
        this.mStartingData.mAssociatedTask = this.task;
        this.task.mSharedStartingData = this.mStartingData;
    }

    void associateStartingWindowWithTaskIfNeeded() {
        StartingData startingData;
        if (this.mStartingWindow == null || (startingData = this.mStartingData) == null || startingData.mAssociatedTask != null) {
            return;
        }
        associateStartingDataWithTask();
        attachStartingSurfaceToAssociatedTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeStartingWindow() {
        if (ITranActivityRecord.Instance().isSkipSnapshot(this.packageName)) {
            Slog.w(TAG, "removeStartingWindow immediately: " + this.packageName);
            Trace.traceBegin(32L, "Camera#removeStartingWindow");
            removeStartingWindowAnimation(false);
            Trace.traceEnd(32L);
            return;
        }
        boolean prevEligibleForLetterboxEducation = isEligibleForLetterboxEducation();
        if (transferSplashScreenIfNeeded()) {
            return;
        }
        removeStartingWindowAnimation(true);
        Task task = getTask();
        if (prevEligibleForLetterboxEducation != isEligibleForLetterboxEducation() && task != null) {
            task.dispatchTaskInfoChangedIfNeeded(true);
        }
    }

    void removeStartingWindowAnimation(final boolean prepareAnimation) {
        this.mTransferringSplashScreenState = 0;
        Task task = this.task;
        if (task != null) {
            task.mSharedStartingData = null;
        }
        if (this.mStartingWindow == null) {
            if (this.mStartingData != null) {
                if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                    String protoLogParam0 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -2127842445, 0, (String) null, new Object[]{protoLogParam0});
                }
                this.mStartingData = null;
                this.mStartingSurface = null;
                return;
            }
            return;
        }
        final StartingData startingData = this.mStartingData;
        if (this.mStartingData != null) {
            final StartingSurfaceController.StartingSurface surface = this.mStartingSurface;
            this.mStartingData = null;
            this.mStartingSurface = null;
            this.mStartingWindow = null;
            if (surface == null) {
                if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 45285419, 0, (String) null, (Object[]) null);
                    return;
                }
                return;
            }
            if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                String protoLogParam02 = String.valueOf(this);
                String protoLogParam1 = String.valueOf(this.mStartingWindow);
                String protoLogParam2 = String.valueOf(this.mStartingSurface);
                String protoLogParam3 = String.valueOf(Debug.getCallers(5));
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -1128015008, 0, (String) null, new Object[]{protoLogParam02, protoLogParam1, protoLogParam2, protoLogParam3});
            }
            Runnable removeSurface = new Runnable() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    ActivityRecord.lambda$removeStartingWindowAnimation$4(StartingSurfaceController.StartingSurface.this, prepareAnimation, startingData);
                }
            };
            removeSurface.run();
        } else if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
            String protoLogParam03 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 146871307, 0, (String) null, new Object[]{protoLogParam03});
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$removeStartingWindowAnimation$4(StartingSurfaceController.StartingSurface surface, boolean prepareAnimation, StartingData startingData) {
        boolean z = true;
        if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
            String protoLogParam0 = String.valueOf(surface);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 1742235936, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (prepareAnimation) {
            try {
                if (startingData.needRevealAnimation()) {
                    surface.remove(z);
                }
            } catch (Exception e) {
                Slog.w(WmsExt.TAG, "Exception when removing starting window", e);
                return;
            }
        }
        z = false;
        surface.remove(z);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reparent(TaskFragment newTaskFrag, int position, String reason) {
        if (getParent() == null) {
            Slog.w(TAG, "reparent: Attempted to reparent non-existing app token: " + this.token);
            return;
        }
        TaskFragment prevTaskFrag = getTaskFragment();
        if (prevTaskFrag == newTaskFrag) {
            throw new IllegalArgumentException(reason + ": task fragment =" + newTaskFrag + " is already the parent of r=" + this);
        }
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            long protoLogParam1 = this.task.mTaskId;
            long protoLogParam2 = position;
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 573582981, 20, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2)});
        }
        reparent(newTaskFrag, position);
    }

    private boolean isHomeIntent(Intent intent) {
        return "android.intent.action.MAIN".equals(intent.getAction()) && (intent.hasCategory("android.intent.category.HOME") || intent.hasCategory("android.intent.category.SECONDARY_HOME")) && intent.getCategories().size() == 1 && intent.getData() == null && intent.getType() == null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isMainIntent(Intent intent) {
        return "android.intent.action.MAIN".equals(intent.getAction()) && intent.hasCategory("android.intent.category.LAUNCHER") && intent.getCategories().size() == 1 && intent.getData() == null && intent.getType() == null;
    }

    boolean canLaunchHomeActivity(int uid, ActivityRecord sourceRecord) {
        if (uid == Process.myUid() || uid == 0) {
            return true;
        }
        RecentTasks recentTasks = this.mTaskSupervisor.mService.getRecentTasks();
        if (recentTasks == null || !recentTasks.isCallerRecents(uid)) {
            return sourceRecord != null && sourceRecord.isResolverOrDelegateActivity();
        }
        return true;
    }

    private boolean canLaunchAssistActivity(String packageName) {
        ComponentName assistComponent = this.mAtmService.mActiveVoiceInteractionServiceComponent;
        if (assistComponent != null) {
            return assistComponent.getPackageName().equals(packageName);
        }
        return false;
    }

    private void setActivityType(boolean componentSpecified, int launchedFromUid, Intent intent, ActivityOptions options, ActivityRecord sourceRecord) {
        int activityType = 0;
        if ((!componentSpecified || canLaunchHomeActivity(launchedFromUid, sourceRecord)) && isHomeIntent(intent) && !isResolverOrDelegateActivity()) {
            activityType = 2;
            if (this.info.resizeMode == 4 || this.info.resizeMode == 1) {
                this.info.resizeMode = 0;
            }
        } else if (this.mAtmService.getRecentTasks().isRecentsComponent(this.mActivityComponent, this.info.applicationInfo.uid)) {
            activityType = 3;
        } else if (options != null && options.getLaunchActivityType() == 4 && canLaunchAssistActivity(this.launchedFromPackage)) {
            activityType = 4;
        } else if (options != null && options.getLaunchActivityType() == 5 && this.mAtmService.canLaunchDreamActivity(this.launchedFromPackage) && DreamActivity.class.getName() == this.info.name) {
            activityType = 5;
        }
        setActivityType(activityType);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTaskToAffiliateWith(Task taskToAffiliateWith) {
        int i = this.launchMode;
        if (i != 3 && i != 2) {
            this.task.setTaskToAffiliateWith(taskToAffiliateWith);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootTask() {
        Task task = this.task;
        if (task != null) {
            return task.getRootTask();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRootTaskId() {
        Task task = this.task;
        if (task != null) {
            return task.getRootTaskId();
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getOrganizedTask() {
        Task task = this.task;
        if (task != null) {
            return task.getOrganizedTask();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragment getOrganizedTaskFragment() {
        TaskFragment parent = getTaskFragment();
        if (parent != null) {
            return parent.getOrganizedTaskFragment();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isEmbedded() {
        TaskFragment parent = getTaskFragment();
        return parent != null && parent.isEmbedded();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public TaskDisplayArea getDisplayArea() {
        return (TaskDisplayArea) super.getDisplayArea();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean providesOrientation() {
        return this.mStyleFillsParent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean fillsParent() {
        return occludesParent(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean occludesParent() {
        return occludesParent(false);
    }

    boolean occludesParent(boolean includingFinishing) {
        if (includingFinishing || !this.finishing) {
            return this.mOccludesParent || showWallpaper();
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setOccludesParent(boolean occludesParent) {
        boolean changed = occludesParent != this.mOccludesParent;
        this.mOccludesParent = occludesParent;
        setMainWindowOpaque(occludesParent);
        this.mWmService.mWindowPlacerLocked.requestTraversal();
        if (changed && this.task != null && !occludesParent) {
            getRootTask().convertActivityToTranslucent(this);
        }
        if (changed || !occludesParent) {
            this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMainWindowOpaque(boolean isOpaque) {
        WindowState win = findMainWindow();
        if (win == null) {
            return;
        }
        win.mWinAnimator.setOpaqueLocked(isOpaque & (!PixelFormat.formatHasAlpha(win.getAttrs().format)));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void takeFromHistory() {
        if (this.inHistory) {
            this.inHistory = false;
            if (this.task != null && !this.finishing) {
                this.task = null;
            }
            abortAndClearOptionsAnimation();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInHistory() {
        return this.inHistory;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInRootTaskLocked() {
        Task rootTask = getRootTask();
        return (rootTask == null || rootTask.isInTask(this) == null) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPersistable() {
        Intent intent;
        return (this.info.persistableMode == 0 || this.info.persistableMode == 2) && ((intent = this.intent) == null || (intent.getFlags() & 8388608) == 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isFocusable() {
        return super.isFocusable() && (canReceiveKeys() || isAlwaysFocusable());
    }

    boolean canReceiveKeys() {
        Task task;
        return getWindowConfiguration().canReceiveKeys() && ((task = this.task) == null || task.getWindowConfiguration().canReceiveKeys());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isResizeable() {
        return isResizeable(true);
    }

    boolean isResizeable(boolean checkPictureInPictureSupport) {
        return this.mAtmService.mForceResizableActivities || ActivityInfo.isResizeableMode(this.info.resizeMode) || (this.info.supportsPictureInPicture() && checkPictureInPictureSupport) || isEmbedded() || IActivityRecordLice.instance().onIsResizeable(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canForceResizeNonResizable(int windowingMode) {
        boolean supportsMultiWindow;
        if (windowingMode == 2 && this.info.supportsPictureInPicture()) {
            return false;
        }
        Task task = this.task;
        if (task != null) {
            supportsMultiWindow = task.supportsMultiWindow() || supportsMultiWindow();
        } else {
            supportsMultiWindow = supportsMultiWindow();
        }
        return ((WindowConfiguration.inMultiWindowMode(windowingMode) && supportsMultiWindow && !this.mAtmService.mForceResizableActivities) || this.info.resizeMode == 2 || this.info.resizeMode == 1) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsPictureInPicture() {
        return this.mAtmService.mSupportsPictureInPicture && isActivityTypeStandardOrUndefined() && this.info.supportsPictureInPicture();
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public boolean supportsSplitScreenWindowingMode() {
        return supportsSplitScreenWindowingModeInDisplayArea(getDisplayArea());
    }

    boolean supportsSplitScreenWindowingModeInDisplayArea(TaskDisplayArea tda) {
        return super.supportsSplitScreenWindowingMode() && this.mAtmService.mSupportsSplitScreenMultiWindow && supportsMultiWindowInDisplayArea(tda);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsFreeform() {
        return supportsFreeformInDisplayArea(getDisplayArea());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsFreeformInDisplayArea(TaskDisplayArea tda) {
        return this.mAtmService.mSupportsFreeformWindowManagement && supportsMultiWindowInDisplayArea(tda);
    }

    boolean supportsMultiWindow() {
        return supportsMultiWindowInDisplayArea(getDisplayArea());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsMultiWindowInDisplayArea(TaskDisplayArea tda) {
        if (isActivityTypeHome() || !this.mAtmService.mSupportsMultiWindow || tda == null) {
            return false;
        }
        if (isResizeable() || tda.supportsNonResizableMultiWindow()) {
            ActivityInfo.WindowLayout windowLayout = this.info.windowLayout;
            return windowLayout == null || tda.supportsActivityMinWidthHeightMultiWindow(windowLayout.minWidth, windowLayout.minHeight, this.info);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canBeLaunchedOnDisplay(int displayId) {
        return this.mAtmService.mTaskSupervisor.canPlaceEntityOnDisplay(displayId, this.launchedFromPid, this.launchedFromUid, this.info);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public boolean checkEnterPictureInPictureState(String caller, boolean beforeStopping) {
        if (supportsPictureInPicture() && checkEnterPictureInPictureAppOpsState() && !this.mAtmService.shouldDisableNonVrUiLocked()) {
            if (this.mDisplayContent != null && !this.mDisplayContent.mDwpcHelper.isWindowingModeSupported(2)) {
                Slog.w(TAG, "Display " + this.mDisplayContent.getDisplayId() + " doesn't support enter picture-in-picture mode. caller = " + caller);
                return false;
            } else if (this.mAtmService.mActivityClientController.shouldDisablePipInSpecialCase()) {
                return false;
            } else {
                if (getConfiguration().windowConfiguration.isThunderbackWindow() && ITranMultiWindow.Instance().inLargeScreen(this.mRootWindowContainer.getDefaultDisplay().getDisplayMetrics())) {
                    return false;
                }
                boolean isCurrentAppLocked = this.mAtmService.getLockTaskModeState() != 0;
                TaskDisplayArea taskDisplayArea = getDisplayArea();
                boolean hasRootPinnedTask = taskDisplayArea != null && taskDisplayArea.hasPinnedTask();
                boolean isNotLockedOrOnKeyguard = (isKeyguardLocked() || isCurrentAppLocked) ? false : true;
                if (beforeStopping && hasRootPinnedTask) {
                    return false;
                }
                Task task = this.task;
                if (task != null && TRAN_AUTO_PACKAGE_NAME.equals(task.mCallingPackage)) {
                    Slog.d(TAG, "Drive mode task, don't enter pip, " + this.task);
                    return false;
                }
                switch (AnonymousClass6.$SwitchMap$com$android$server$wm$ActivityRecord$State[this.mState.ordinal()]) {
                    case 1:
                        if (isCurrentAppLocked) {
                            return false;
                        }
                        return this.supportsEnterPipOnTaskSwitch || !beforeStopping;
                    case 2:
                    case 3:
                        return isNotLockedOrOnKeyguard && !hasRootPinnedTask && this.supportsEnterPipOnTaskSwitch;
                    case 4:
                        return this.supportsEnterPipOnTaskSwitch && isNotLockedOrOnKeyguard && !hasRootPinnedTask;
                }
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.wm.ActivityRecord$6  reason: invalid class name */
    /* loaded from: classes2.dex */
    public static /* synthetic */ class AnonymousClass6 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$wm$ActivityRecord$State;

        static {
            int[] iArr = new int[State.values().length];
            $SwitchMap$com$android$server$wm$ActivityRecord$State = iArr;
            try {
                iArr[State.RESUMED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$wm$ActivityRecord$State[State.PAUSING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$wm$ActivityRecord$State[State.PAUSED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$wm$ActivityRecord$State[State.STOPPING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$server$wm$ActivityRecord$State[State.STARTED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$server$wm$ActivityRecord$State[State.STOPPED.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$server$wm$ActivityRecord$State[State.DESTROYED.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$server$wm$ActivityRecord$State[State.DESTROYING.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$server$wm$ActivityRecord$State[State.INITIALIZING.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWillCloseOrEnterPip(boolean willCloseOrEnterPip) {
        this.mWillCloseOrEnterPip = willCloseOrEnterPip;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isClosingOrEnteringPip() {
        return (isAnimating(3, 1) && !this.mVisibleRequested) || this.mWillCloseOrEnterPip;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkEnterPictureInPictureAppOpsState() {
        return this.mAtmService.getAppOpsManager().checkOpNoThrow(67, this.info.applicationInfo.uid, this.packageName) == 0;
    }

    private boolean isAlwaysFocusable() {
        return (this.info.flags & 262144) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean windowsAreFocusable() {
        return windowsAreFocusable(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean windowsAreFocusable(boolean fromUserTouch) {
        if (!fromUserTouch && this.mTargetSdk < 29) {
            int pid = getPid();
            ActivityRecord topFocusedAppOfMyProcess = this.mWmService.mRoot.mTopFocusedAppByProcess.get(Integer.valueOf(pid));
            if (topFocusedAppOfMyProcess != null && topFocusedAppOfMyProcess != this) {
                return false;
            }
        }
        return (canReceiveKeys() || isAlwaysFocusable()) && isAttached();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean moveFocusableActivityToTop(String reason) {
        if (!isFocusable()) {
            if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_FOCUS, 240271590, 0, (String) null, new Object[]{protoLogParam0});
            }
            return false;
        }
        Task rootTask = getRootTask();
        if (rootTask == null) {
            Slog.w(TAG, "moveFocusableActivityToTop: invalid root task: activity=" + this + " task=" + this.task);
            return false;
        } else if (this.mRootWindowContainer.getTopResumedActivity() == this && getDisplayContent().mFocusedApp == this) {
            if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
                String protoLogParam02 = String.valueOf(this);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_FOCUS, 1856211951, 0, (String) null, new Object[]{protoLogParam02});
            }
            return !isState(State.RESUMED);
        } else {
            if (ProtoLogCache.WM_DEBUG_FOCUS_enabled) {
                String protoLogParam03 = String.valueOf(this);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_FOCUS, -50336993, 0, (String) null, new Object[]{protoLogParam03});
            }
            rootTask.moveToFront(reason, this.task);
            if (this.mRootWindowContainer.getTopResumedActivity() == this) {
                this.mAtmService.setResumedActivityUncheckLocked(this, reason);
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishIfSubActivity(ActivityRecord parent, String otherResultWho, int otherRequestCode) {
        if (this.resultTo != parent || this.requestCode != otherRequestCode || !Objects.equals(this.resultWho, otherResultWho)) {
            return;
        }
        finishIfPossible("request-sub", false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean finishIfSameAffinity(ActivityRecord r) {
        if (Objects.equals(r.taskAffinity, this.taskAffinity)) {
            r.finishIfPossible("request-affinity", true);
            return false;
        }
        return true;
    }

    private void finishActivityResults(final int resultCode, final Intent resultData, final NeededUriGrants resultGrants) {
        if (this.resultTo != null) {
            if (ActivityTaskManagerDebugConfig.DEBUG_RESULTS) {
                Slog.v(TAG_RESULTS, "Adding result to " + this.resultTo + " who=" + this.resultWho + " req=" + this.requestCode + " res=" + resultCode + " data=" + resultData);
            }
            int i = this.resultTo.mUserId;
            int i2 = this.mUserId;
            if (i != i2 && resultData != null) {
                resultData.prepareToLeaveUser(i2);
            }
            if (this.info.applicationInfo.uid > 0) {
                this.mAtmService.mUgmInternal.grantUriPermissionUncheckedFromIntent(resultGrants, this.resultTo.getUriPermissionsLocked());
            }
            if (this.resultTo.isState(State.RESUMED)) {
                final ActivityRecord resultToActivity = this.resultTo;
                this.mAtmService.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda12
                    @Override // java.lang.Runnable
                    public final void run() {
                        ActivityRecord.this.m7777x20e1a133(resultToActivity, resultCode, resultData, resultGrants);
                    }
                });
            } else {
                this.resultTo.addResultLocked(this, this.resultWho, this.requestCode, resultCode, resultData);
            }
            this.resultTo = null;
        } else if (ActivityTaskManagerDebugConfig.DEBUG_RESULTS) {
            Slog.v(TAG_RESULTS, "No result destination from " + this);
        }
        this.results = null;
        this.pendingResults = null;
        this.newIntents = null;
        setSavedState(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishActivityResults$5$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ void m7777x20e1a133(ActivityRecord resultToActivity, int resultCode, Intent resultData, NeededUriGrants resultGrants) {
        synchronized (this.mAtmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                resultToActivity.sendResult(getUid(), this.resultWho, this.requestCode, resultCode, resultData, resultGrants);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int finishIfPossible(String reason, boolean oomAdj) {
        return finishIfPossible(0, null, null, reason, oomAdj);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int finishIfPossible(int resultCode, Intent resultData, NeededUriGrants resultGrants, String reason, boolean oomAdj) {
        boolean endTask;
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(this);
            long protoLogParam1 = resultCode;
            String protoLogParam2 = String.valueOf(resultData);
            String protoLogParam3 = String.valueOf(reason);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 1047769218, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), protoLogParam2, protoLogParam3});
        }
        if (this.finishing) {
            Slog.w(TAG, "Duplicate finish request for r=" + this);
            return 0;
        } else if (!isInRootTaskLocked()) {
            Slog.w(TAG, "Finish request when not in root task for r=" + this);
            return 0;
        } else {
            Task rootTask = getRootTask();
            boolean mayAdjustTop = (isState(State.RESUMED) || rootTask.getTopResumedActivity() == null) && rootTask.isFocusedRootTaskOnDisplay() && !this.task.isClearingToReuseTask();
            boolean shouldAdjustGlobalFocus = mayAdjustTop && this.mRootWindowContainer.isTopDisplayFocusedRootTask(rootTask);
            this.mAtmService.deferWindowLayout();
            try {
                this.mTaskSupervisor.mNoHistoryActivities.remove(this);
                makeFinishingLocked();
                Task task = getTask();
                try {
                    EventLogTags.writeWmFinishActivity(this.mUserId, System.identityHashCode(this), task.mTaskId, this.shortComponentName, reason);
                    ActivityRecord next = task.getActivityAbove(this);
                    if (next != null && (this.intent.getFlags() & 524288) != 0) {
                        next.intent.addFlags(524288);
                    }
                    pauseKeyDispatchingLocked();
                    if (mayAdjustTop && task.topRunningActivity(true) == null) {
                        task.adjustFocusToNextFocusableTask("finish-top", false, shouldAdjustGlobalFocus);
                    }
                    finishActivityResults(resultCode, resultData, resultGrants);
                    boolean endTask2 = task.getTopNonFinishingActivity() == null && !task.isClearingToReuseTask();
                    boolean ignoreCloseTransition = SplitScreenHelper.isIgnoreCloseTransitionForSplitScreen(task, endTask2);
                    if (!ignoreCloseTransition) {
                        this.mTransitionController.requestCloseTransitionIfNeeded(endTask2 ? task : this);
                    }
                    if (!isState(State.RESUMED)) {
                        if (!isState(State.PAUSING)) {
                            if (this.mVisibleRequested) {
                                prepareActivityHideTransitionAnimation();
                            }
                            boolean removedActivity = completeFinishing("finishIfPossible") == null;
                            if (oomAdj && isState(State.STOPPING)) {
                                this.mAtmService.updateOomAdj();
                            }
                            if (task.onlyHasTaskOverlayActivities(false)) {
                                task.forAllActivities(new Consumer() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda15
                                    @Override // java.util.function.Consumer
                                    public final void accept(Object obj) {
                                        ((ActivityRecord) obj).prepareActivityHideTransitionAnimationIfOvarlay();
                                    }
                                });
                            }
                            int i = removedActivity ? 2 : 1;
                            this.mAtmService.continueWindowLayout();
                            return i;
                        } else if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                            String protoLogParam02 = String.valueOf(this);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 1023413388, 0, (String) null, new Object[]{protoLogParam02});
                        }
                    } else {
                        if (endTask2) {
                            this.mAtmService.getTaskChangeNotificationController().notifyTaskRemovalStarted(task.getTaskInfo());
                        }
                        if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY || ActivityTaskManagerDebugConfig.DEBUG_TRANSITION) {
                            Slog.v(TAG_TRANSITION, "Prepare close transition: finishing " + this);
                        }
                        if (!ignoreCloseTransition) {
                            this.mDisplayContent.prepareAppTransition(2);
                        }
                        if (this.mAtmService.mWindowManager.mTaskSnapshotController != null && !task.isAnimatingByRecents() && !this.mTransitionController.inRecentsTransition(task) && endTask2) {
                            ArraySet<Task> tasks = Sets.newArraySet(new Task[]{task});
                            this.mAtmService.mWindowManager.mTaskSnapshotController.snapshotTasks(tasks);
                            this.mAtmService.mWindowManager.mTaskSnapshotController.addSkipClosingAppSnapshotTasks(tasks);
                        }
                        setVisibility(false);
                        if (getTaskFragment().getPausingActivity() != null) {
                            endTask = endTask2;
                        } else {
                            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                                String protoLogParam03 = String.valueOf(this);
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1003060523, 0, (String) null, new Object[]{protoLogParam03});
                            }
                            if (ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING) {
                                Slog.v(TAG_USER_LEAVING, "finish() => pause with userLeaving=false");
                            }
                            getTaskFragment().startPausing(false, false, null, "finish");
                            logByGriffinWhenFinishIfPossible();
                            if (getTask() == null) {
                                endTask = endTask2;
                            } else {
                                ActivityRecord nextResumedActivity = getTask().topRunningActivityLocked();
                                if (nextResumedActivity == null) {
                                    endTask = endTask2;
                                } else {
                                    endTask = endTask2;
                                    this.mAtmService.mAmsExt.onBeforeActivitySwitch(this, nextResumedActivity, true, nextResumedActivity.getActivityType(), false);
                                }
                            }
                        }
                        if (endTask) {
                            this.mAtmService.getLockTaskController().clearLockedTask(task);
                            if (mayAdjustTop) {
                                this.mNeedsZBoost = true;
                                this.mDisplayContent.assignWindowLayers(false);
                            }
                        }
                    }
                    this.mAtmService.continueWindowLayout();
                    return 1;
                } catch (Throwable th) {
                    th = th;
                    this.mAtmService.continueWindowLayout();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void prepareActivityHideTransitionAnimationIfOvarlay() {
        if (this.mTaskOverlay) {
            prepareActivityHideTransitionAnimation();
        }
    }

    private void prepareActivityHideTransitionAnimation() {
        DisplayContent dc = this.mDisplayContent;
        dc.prepareAppTransition(2);
        setVisibility(false);
        dc.executeAppTransition();
    }

    ActivityRecord completeFinishing(String reason) {
        return completeFinishing(true, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord completeFinishing(boolean updateVisibility, String reason) {
        if (!this.finishing || isState(State.RESUMED)) {
            throw new IllegalArgumentException("Activity must be finishing and not resumed to complete, r=" + this + ", finishing=" + this.finishing + ", state=" + this.mState);
        }
        if (isState(State.PAUSING)) {
            return this;
        }
        boolean isNextNotYetVisible = true;
        boolean isCurrentVisible = this.mVisibleRequested || isState(State.PAUSED, State.STARTED);
        if (updateVisibility && isCurrentVisible) {
            boolean ensureVisibility = false;
            if (occludesParent(true)) {
                ensureVisibility = true;
            } else if (isKeyguardLocked() && this.mTaskSupervisor.getKeyguardController().topActivityOccludesKeyguard(this)) {
                ensureVisibility = true;
            }
            if (ensureVisibility) {
                this.mDisplayContent.ensureActivitiesVisible(null, 0, false, true);
            }
        }
        boolean activityRemoved = false;
        ActivityRecord next = getDisplayArea().topRunningActivity(true);
        boolean delayRemoval = false;
        TaskFragment taskFragment = getTaskFragment();
        if (next != null && taskFragment != null && taskFragment.isEmbeddedWithBoundsOverride()) {
            TaskFragment organized = taskFragment.getOrganizedTaskFragment();
            TaskFragment adjacent = organized != null ? organized.getCompanionTaskFragment() : null;
            if (adjacent != null && next.isDescendantOf(adjacent) && organized.topRunningActivity() == null) {
                delayRemoval = organized.isDelayLastActivityRemoval() || organized.shouldRemoveSelfOnLastChildRemoval();
                Slog.d(TAG, "completeFinishing delayRemoval=" + delayRemoval + " next=" + next);
            }
        }
        if (next == null || (next.nowVisible && next.mVisibleRequested)) {
            isNextNotYetVisible = false;
        }
        if (isNextNotYetVisible && this.mDisplayContent.isSleeping() && next == next.getTaskFragment().mLastPausedActivity) {
            next.getTaskFragment().clearLastPausedActivity();
        }
        if (isCurrentVisible) {
            if (isNextNotYetVisible || delayRemoval) {
                addToStopping(false, false, "completeFinishing");
                setState(State.STOPPING, "completeFinishing");
            } else if (!addToFinishingAndWaitForIdle()) {
                activityRemoved = destroyIfPossible(reason);
            }
        } else {
            addToFinishingAndWaitForIdle();
            activityRemoved = destroyIfPossible(reason);
        }
        if (activityRemoved) {
            return null;
        }
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean destroyIfPossible(String reason) {
        setState(State.FINISHING, "destroyIfPossible");
        this.mTaskSupervisor.mStoppingActivities.remove(this);
        Task rootTask = getRootTask();
        TaskDisplayArea taskDisplayArea = getDisplayArea();
        ActivityRecord next = taskDisplayArea.topRunningActivity();
        boolean isLastRootTaskOverEmptyHome = next == null && rootTask.isFocusedRootTaskOnDisplay() && taskDisplayArea.getOrCreateRootHomeTask() != null;
        if (isLastRootTaskOverEmptyHome) {
            addToFinishingAndWaitForIdle();
            return false;
        }
        makeFinishingLocked();
        boolean activityRemoved = destroyImmediately("finish-imm:" + reason);
        if (next == null) {
            this.mRootWindowContainer.ensureVisibilityAndConfig(next, getDisplayId(), false, true);
        }
        if (activityRemoved) {
            this.mRootWindowContainer.resumeFocusedTasksTopActivities();
        }
        if (ProtoLogCache.WM_DEBUG_CONTAINERS_enabled) {
            String protoLogParam0 = String.valueOf(this);
            String protoLogParam1 = String.valueOf(activityRemoved);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_CONTAINERS, -401282500, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        return activityRemoved;
    }

    boolean addToFinishingAndWaitForIdle() {
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 1610646518, 0, (String) null, new Object[]{protoLogParam0});
        }
        setState(State.FINISHING, "addToFinishingAndWaitForIdle");
        if (!this.mTaskSupervisor.mFinishingActivities.contains(this)) {
            this.mTaskSupervisor.mFinishingActivities.add(this);
        }
        resumeKeyDispatchingLocked();
        return this.mRootWindowContainer.resumeFocusedTasksTopActivities();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean destroyImmediately(String reason) {
        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH || ActivityTaskManagerDebugConfig.DEBUG_CLEANUP) {
            Slog.v(TAG_SWITCH, "Removing activity from " + reason + ": token=" + this + ", app=" + (hasProcess() ? this.app.mName : "(null)"));
        }
        if (isState(State.DESTROYING, State.DESTROYED)) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(this);
                String protoLogParam1 = String.valueOf(reason);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -21399771, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
            }
            return false;
        }
        EventLogTags.writeWmDestroyActivity(this.mUserId, System.identityHashCode(this), this.task.mTaskId, this.shortComponentName, reason);
        boolean removedFromHistory = false;
        cleanUp(false, false);
        if (hasProcess()) {
            this.app.removeActivity(this, true);
            if (!this.app.hasActivities()) {
                this.mAtmService.clearHeavyWeightProcessIfEquals(this.app);
            }
            boolean skipDestroy = false;
            try {
                if (isState(State.FINISHING)) {
                    this.mAtmService.mAmsExt.onActivityStateChanged(this, false);
                }
                if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
                    Slog.i(TAG_SWITCH, "Destroying: " + this);
                }
                this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ActivityLifecycleItem) DestroyActivityItem.obtain(this.finishing, this.configChangeFlags));
            } catch (Exception e) {
                if (this.finishing) {
                    removeFromHistory(reason + " exceptionInScheduleDestroy");
                    removedFromHistory = true;
                    skipDestroy = true;
                }
            }
            this.nowVisible = false;
            if (this.finishing && !skipDestroy) {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam02 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1432963966, 0, (String) null, new Object[]{protoLogParam02});
                }
                setState(State.DESTROYING, "destroyActivityLocked. finishing and not skipping destroy");
                this.mAtmService.mH.postDelayed(this.mDestroyTimeoutRunnable, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            } else {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam03 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 726205185, 0, (String) null, new Object[]{protoLogParam03});
                }
                setState(State.DESTROYED, "destroyActivityLocked. not finishing or skipping destroy");
                if (ActivityTaskManagerDebugConfig.DEBUG_APP) {
                    Slog.v(TAG_APP, "Clearing app during destroy for activity " + this);
                }
                detachFromProcess();
            }
        } else if (this.finishing) {
            removeFromHistory(reason + " hadNoApp");
            removedFromHistory = true;
        } else {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam04 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -729530161, 0, (String) null, new Object[]{protoLogParam04});
            }
            setState(State.DESTROYED, "destroyActivityLocked. not finishing and had no app");
        }
        this.configChangeFlags = 0;
        return removedFromHistory;
    }

    boolean safelyDestroy(String reason) {
        if (isDestroyable()) {
            if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
                Task task = getTask();
                Slog.v(TAG_SWITCH, "Safely destroying " + this + " in state " + getState() + " resumed=" + task.getTopResumedActivity() + " pausing=" + task.getTopPausingActivity() + " for reason " + reason);
            }
            return destroyImmediately(reason);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeFromHistory(String reason) {
        finishActivityResults(0, null, null);
        makeFinishingLocked();
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            String protoLogParam1 = String.valueOf(reason);
            String protoLogParam2 = String.valueOf(Debug.getCallers(5));
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 350168164, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
        }
        takeFromHistory();
        removeTimeouts();
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam02 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 579298675, 0, (String) null, new Object[]{protoLogParam02});
        }
        setState(State.DESTROYED, "removeFromHistory");
        if (ActivityTaskManagerDebugConfig.DEBUG_APP) {
            Slog.v(TAG_APP, "Clearing app during remove for activity " + this);
        }
        detachFromProcess();
        resumeKeyDispatchingLocked();
        this.mDisplayContent.removeAppToken(this.token);
        cleanUpActivityServices();
        removeUriPermissionsLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void detachFromProcess() {
        WindowProcessController windowProcessController = this.app;
        if (windowProcessController != null) {
            windowProcessController.removeActivity(this, false);
        }
        this.app = null;
        this.mInputDispatchingTimeoutMillis = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void makeFinishingLocked() {
        ActivityRecord companionTopActivity;
        Task task;
        ActivityRecord nextCookieTarget;
        if (this.finishing) {
            return;
        }
        this.finishing = true;
        if (this.mLaunchCookie != null && this.mState != State.RESUMED && (task = this.task) != null && !task.mInRemoveTask && !this.task.isClearingToReuseTask() && (nextCookieTarget = this.task.getActivity(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda11
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ActivityRecord.this.m7780xfec7e41a((ActivityRecord) obj);
            }
        }, this, false, false)) != null) {
            nextCookieTarget.mLaunchCookie = this.mLaunchCookie;
            this.mLaunchCookie = null;
        }
        TaskFragment taskFragment = getTaskFragment();
        if (taskFragment != null) {
            Task task2 = taskFragment.getTask();
            if (task2 != null && task2.isClearingToReuseTask() && taskFragment.getTopNonFinishingActivity() == null) {
                taskFragment.mClearedTaskForReuse = true;
                TaskFragment companionTf = taskFragment.getCompanionTaskFragment();
                if (companionTf != null && this.mLaunchCookie != null && (companionTopActivity = companionTf.getTopNonFinishingActivity()) != null && companionTopActivity.mLaunchCookie == null) {
                    companionTopActivity.mLaunchCookie = this.mLaunchCookie;
                    this.mLaunchCookie = null;
                    Slog.d(TAG, "Transfer launch cookie to the companionTf's top activity.");
                }
            }
            taskFragment.sendTaskFragmentInfoChanged();
        }
        if (this.stopped) {
            abortAndClearOptionsAnimation();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$makeFinishingLocked$7$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ boolean m7780xfec7e41a(ActivityRecord r) {
        return r.mLaunchCookie == null && !r.finishing && r.isUid(getUid());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroyed(String reason) {
        removeDestroyTimeout();
        if (ProtoLogCache.WM_DEBUG_CONTAINERS_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_CONTAINERS, -1598452494, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (!isState(State.DESTROYING, State.DESTROYED)) {
            throw new IllegalStateException("Reported destroyed for activity that is not destroying: r=" + this);
        }
        if (isInRootTaskLocked()) {
            cleanUp(true, false);
            removeFromHistory(reason);
        }
        this.mRootWindowContainer.resumeFocusedTasksTopActivities();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanUp(boolean cleanServices, boolean setState) {
        HashSet<WeakReference<PendingIntentRecord>> hashSet;
        Task task = this.task;
        if (task != null) {
            task.cleanUpActivityReferences(this);
        }
        getTaskFragment().cleanUpActivityReferences(this);
        clearLastParentBeforePip();
        cleanUpSplashScreen();
        this.deferRelaunchUntilPaused = false;
        this.frozenBeforeDestroy = false;
        if (setState) {
            setState(State.DESTROYED, "cleanUp");
            if (ActivityTaskManagerDebugConfig.DEBUG_APP) {
                Slog.v(TAG_APP, "Clearing app during cleanUp for activity " + this);
            }
            detachFromProcess();
        }
        this.mTaskSupervisor.cleanupActivity(this);
        if (this.finishing && (hashSet = this.pendingResults) != null) {
            Iterator<WeakReference<PendingIntentRecord>> it = hashSet.iterator();
            while (it.hasNext()) {
                WeakReference<PendingIntentRecord> apr = it.next();
                PendingIntentRecord rec = apr.get();
                if (rec != null) {
                    this.mAtmService.mPendingIntentController.cancelIntentSender(rec, false);
                }
            }
            this.pendingResults = null;
        }
        if (cleanServices) {
            cleanUpActivityServices();
        }
        removeTimeouts();
        clearRelaunching();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRelaunching() {
        return this.mPendingRelaunchCount > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startRelaunching() {
        if (this.mPendingRelaunchCount == 0) {
            this.mRelaunchStartTime = SystemClock.elapsedRealtime();
        }
        clearAllDrawn();
        this.mPendingRelaunchCount++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishRelaunching() {
        this.mTaskSupervisor.getActivityMetricsLogger().notifyActivityRelaunched(this);
        int i = this.mPendingRelaunchCount;
        if (i > 0) {
            int i2 = i - 1;
            this.mPendingRelaunchCount = i2;
            if (i2 == 0 && !isClientVisible()) {
                this.mRelaunchStartTime = 0L;
            }
        } else {
            checkKeyguardFlagsChanged();
        }
        Task rootTask = getRootTask();
        if (rootTask != null && rootTask.shouldSleepOrShutDownActivities()) {
            rootTask.ensureActivitiesVisible(null, 0, false);
        }
    }

    void clearRelaunching() {
        if (this.mPendingRelaunchCount == 0) {
            return;
        }
        this.mPendingRelaunchCount = 0;
        this.mRelaunchStartTime = 0L;
    }

    private void cleanUpActivityServices() {
        ActivityServiceConnectionsHolder activityServiceConnectionsHolder = this.mServiceConnectionsHolder;
        if (activityServiceConnectionsHolder == null) {
            return;
        }
        activityServiceConnectionsHolder.disconnectActivityFromServices();
        this.mServiceConnectionsHolder = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleAppDied() {
        boolean remove;
        Task task;
        ActivityRecord top;
        WindowProcessController windowProcessController;
        int i = this.mRelaunchReason;
        if ((i == 1 || i == 2) && this.launchCount < 3 && !this.finishing) {
            remove = false;
        } else {
            boolean remove2 = this.mHaveState;
            if ((!remove2 && !this.stateNotNeeded && !isState(State.RESTARTING_PROCESS)) || this.finishing) {
                remove = true;
            } else {
                boolean remove3 = this.mVisibleRequested;
                if (!remove3 && this.launchCount > 2 && this.lastLaunchTime > SystemClock.uptimeMillis() - 60000) {
                    remove = true;
                } else {
                    remove = false;
                }
            }
        }
        if (remove) {
            if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                String protoLogParam0 = String.valueOf(this);
                boolean protoLogParam1 = this.mHaveState;
                String protoLogParam2 = String.valueOf(this.stateNotNeeded);
                boolean protoLogParam3 = this.finishing;
                String protoLogParam4 = String.valueOf(this.mState);
                String protoLogParam5 = String.valueOf(Debug.getCallers(5));
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 1789603530, 204, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), protoLogParam2, Boolean.valueOf(protoLogParam3), protoLogParam4, protoLogParam5});
            }
            if (!this.finishing || ((windowProcessController = this.app) != null && windowProcessController.isRemoved())) {
                Slog.w(TAG, "Force removing " + this + ": app died, no saved state");
                int i2 = this.mUserId;
                int identityHashCode = System.identityHashCode(this);
                Task task2 = this.task;
                EventLogTags.writeWmFinishActivity(i2, identityHashCode, task2 != null ? task2.mTaskId : -1, this.shortComponentName, "proc died without state saved");
            }
        } else {
            if (ActivityTaskManagerDebugConfig.DEBUG_APP) {
                Slog.v(TAG_APP, "Keeping entry during removeHistory for activity " + this);
            }
            this.nowVisible = this.mVisibleRequested;
        }
        this.mTransitionController.requestCloseTransitionIfNeeded(this);
        cleanUp(true, true);
        if (remove) {
            if (this.mStartingData != null && this.mVisible && (task = this.task) != null && (top = task.topRunningActivity()) != null && !top.mVisible && top.shouldBeVisible()) {
                top.transferStartingWindow(this);
            }
            removeFromHistory("appDied");
        }
    }

    @Override // com.android.server.wm.WindowToken, com.android.server.wm.WindowContainer
    void removeImmediately() {
        if (this.mState != State.DESTROYED) {
            Slog.w(TAG, "Force remove immediately " + this + " state=" + this.mState);
            destroyImmediately("removeImmediately");
            destroyed("removeImmediately");
        } else {
            onRemovedFromDisplay();
        }
        this.mActivityRecordInputSink.releaseSurfaceControl();
        super.removeImmediately();
    }

    @Override // com.android.server.wm.WindowContainer
    void removeIfPossible() {
        this.mIsExiting = false;
        removeAllWindowsIfPossible();
        removeImmediately();
    }

    @Override // com.android.server.wm.WindowContainer
    boolean handleCompleteDeferredRemoval() {
        if (this.mIsExiting) {
            removeIfPossible();
        }
        return super.handleCompleteDeferredRemoval();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onRemovedFromDisplay() {
        if (!this.mRemovingFromDisplay) {
            this.mRemovingFromDisplay = true;
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -1352076759, 0, (String) null, new Object[]{protoLogParam0});
            }
            getDisplayContent().mOpeningApps.remove(this);
            getDisplayContent().mUnknownAppVisibilityController.appRemovedOrHidden(this);
            this.mWmService.mTaskSnapshotController.onAppRemoved(this);
            this.mTaskSupervisor.getActivityMetricsLogger().notifyActivityRemoved(this);
            this.mTaskSupervisor.mStoppingActivities.remove(this);
            this.waitingToShow = false;
            boolean delayed = isAnimating(7, 17);
            if (getDisplayContent().mClosingApps.contains(this)) {
                delayed = true;
            } else if (getDisplayContent().mAppTransition.isTransitionSet()) {
                getDisplayContent().mClosingApps.add(this);
                delayed = true;
            } else if (this.mTransitionController.inTransition()) {
                delayed = true;
            }
            if (!delayed) {
                commitVisibility(false, true);
            } else {
                setVisibleRequested(false);
            }
            this.mTransitionController.collect(this);
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                String protoLogParam02 = String.valueOf(this);
                boolean protoLogParam1 = delayed;
                String protoLogParam2 = String.valueOf(getAnimation());
                boolean protoLogParam3 = isAnimating(3, 1);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 1653210583, 204, (String) null, new Object[]{protoLogParam02, Boolean.valueOf(protoLogParam1), protoLogParam2, Boolean.valueOf(protoLogParam3)});
            }
            if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                String protoLogParam03 = String.valueOf(this);
                boolean protoLogParam12 = delayed;
                String protoLogParam22 = String.valueOf(Debug.getCallers(4));
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -1539974875, 12, (String) null, new Object[]{protoLogParam03, Boolean.valueOf(protoLogParam12), protoLogParam22});
            }
            if (this.mStartingData != null) {
                removeStartingWindow();
            }
            if (isAnimating(3, 1)) {
                getDisplayContent().mNoAnimationNotifyOnTransitionFinished.add(this.token);
            }
            if (delayed && !isEmpty()) {
                if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                    String protoLogParam04 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -2109936758, 0, (String) null, new Object[]{protoLogParam04});
                }
                this.mIsExiting = true;
            } else {
                cancelAnimation();
                removeIfPossible();
            }
            stopFreezingScreen(true, true);
            DisplayContent dc = getDisplayContent();
            if (dc.mFocusedApp == this) {
                if (ProtoLogCache.WM_DEBUG_FOCUS_LIGHT_enabled) {
                    String protoLogParam05 = String.valueOf(this);
                    long protoLogParam13 = dc.getDisplayId();
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_FOCUS_LIGHT, -771177730, 4, (String) null, new Object[]{protoLogParam05, Long.valueOf(protoLogParam13)});
                }
                dc.setFocusedApp(null);
                this.mWmService.updateFocusedWindowLocked(0, true);
            }
            this.mLetterboxUiController.destroy();
            if (!delayed) {
                updateReportedVisibilityLocked();
            }
            this.mDisplayContent.mPinnedTaskController.onActivityHidden(this.mActivityComponent);
            this.mDisplayContent.onRunningActivityChanged();
            this.mWmService.mEmbeddedWindowController.onActivityRemoved(this);
            this.mRemovingFromDisplay = false;
        }
    }

    @Override // com.android.server.wm.WindowToken
    protected boolean isFirstChildWindowGreaterThanSecond(WindowState newWindow, WindowState existingWindow) {
        int type1 = newWindow.mAttrs.type;
        int type2 = existingWindow.mAttrs.type;
        if (type1 == 1 && type2 != 1) {
            return false;
        }
        if (type1 == 1 || type2 != 1) {
            return (type1 == 3 && type2 != 3) || type1 == 3 || type2 != 3;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasStartingWindow() {
        if (this.mStartingData != null) {
            return true;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((WindowState) getChildAt(i)).mAttrs.type == 3) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLastWindow(WindowState win) {
        return this.mChildren.size() == 1 && this.mChildren.get(0) == win;
    }

    @Override // com.android.server.wm.WindowToken
    void addWindow(WindowState w) {
        super.addWindow(w);
        boolean gotReplacementWindow = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState candidate = (WindowState) this.mChildren.get(i);
            gotReplacementWindow |= candidate.setReplacementWindowIfNeeded(w);
        }
        if (gotReplacementWindow) {
            this.mWmService.scheduleWindowReplacementTimeouts(this);
        }
        checkKeyguardFlagsChanged();
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void removeChild(WindowState child) {
        if (!this.mChildren.contains(child)) {
            return;
        }
        super.removeChild((ActivityRecord) child);
        checkKeyguardFlagsChanged();
        updateLetterboxSurface(child);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWindowReplacementTimeout() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowState) this.mChildren.get(i)).onWindowReplacementTimeout();
        }
    }

    void setAppLayoutChanges(int changes, String reason) {
        if (!this.mChildren.isEmpty()) {
            DisplayContent dc = getDisplayContent();
            dc.pendingLayoutChanges |= changes;
            if (WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS) {
                this.mWmService.mWindowPlacerLocked.debugLayoutRepeats(reason, dc.pendingLayoutChanges);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeReplacedWindowIfNeeded(WindowState replacement) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState win = (WindowState) this.mChildren.get(i);
            if (win.removeReplacedWindowIfNeeded(replacement)) {
                return;
            }
        }
    }

    private boolean transferStartingWindow(ActivityRecord fromActivity) {
        WindowState tStartingWindow = fromActivity.mStartingWindow;
        if (tStartingWindow != null && fromActivity.mStartingSurface != null) {
            if (tStartingWindow.getParent() == null) {
                return false;
            }
            if (this.mStartingSurface != null || this.mStartingData != null) {
                Slog.v(WmsExt.TAG, "transferStartingWindow, fromToken already add a starting window.");
                removeStartingWindow();
            }
            if (fromActivity.mVisible) {
                this.mDisplayContent.mSkipAppTransitionAnimation = true;
            }
            if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                String protoLogParam0 = String.valueOf(tStartingWindow);
                String protoLogParam1 = String.valueOf(fromActivity);
                String protoLogParam2 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -1938204785, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
            }
            long origId = Binder.clearCallingIdentity();
            try {
                if (fromActivity.hasFixedRotationTransform()) {
                    this.mDisplayContent.handleTopActivityLaunchingInDifferentOrientation(this, false);
                }
                this.mStartingData = fromActivity.mStartingData;
                this.mStartingSurface = fromActivity.mStartingSurface;
                this.mStartingWindow = tStartingWindow;
                this.reportedVisible = fromActivity.reportedVisible;
                fromActivity.mStartingData = null;
                fromActivity.mStartingSurface = null;
                fromActivity.mStartingWindow = null;
                fromActivity.startingMoved = true;
                tStartingWindow.mToken = this;
                tStartingWindow.mActivityRecord = this;
                if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                    String protoLogParam02 = String.valueOf(tStartingWindow);
                    String protoLogParam12 = String.valueOf(fromActivity);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -1499134947, 0, (String) null, new Object[]{protoLogParam02, protoLogParam12});
                }
                this.mTransitionController.collect(tStartingWindow);
                tStartingWindow.reparent(this, Integer.MAX_VALUE);
                tStartingWindow.clearFrozenInsetsState();
                if (fromActivity.allDrawn) {
                    this.allDrawn = true;
                }
                if (fromActivity.firstWindowDrawn) {
                    this.firstWindowDrawn = true;
                }
                if (fromActivity.isVisible()) {
                    setVisible(true);
                    setVisibleRequested(true);
                    this.mVisibleSetFromTransferredStartingWindow = true;
                }
                setClientVisible(fromActivity.isClientVisible());
                if (fromActivity.isAnimating()) {
                    transferAnimation(fromActivity);
                    this.mUseTransferredAnimation = true;
                } else if (this.mTransitionController.getTransitionPlayer() != null) {
                    this.mUseTransferredAnimation = true;
                }
                fromActivity.postWindowRemoveStartingWindowCleanup(tStartingWindow);
                fromActivity.mVisibleSetFromTransferredStartingWindow = false;
                this.mWmService.updateFocusedWindowLocked(3, true);
                getDisplayContent().setLayoutNeeded();
                this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
                return true;
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        } else if (fromActivity.mStartingData != null) {
            if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                String protoLogParam03 = String.valueOf(fromActivity);
                String protoLogParam13 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -443173857, 0, (String) null, new Object[]{protoLogParam03, protoLogParam13});
            }
            this.mStartingData = fromActivity.mStartingData;
            fromActivity.mStartingData = null;
            fromActivity.startingMoved = true;
            scheduleAddStartingWindow();
            return true;
        } else {
            return false;
        }
    }

    void transferStartingWindowFromHiddenAboveTokenIfNeeded() {
        this.task.forAllActivities(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda7
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ActivityRecord.this.m7787xf90909ed((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$transferStartingWindowFromHiddenAboveTokenIfNeeded$8$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ boolean m7787xf90909ed(ActivityRecord fromActivity) {
        if (fromActivity == this) {
            return true;
        }
        return !fromActivity.mVisibleRequested && transferStartingWindow(fromActivity);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isKeyguardLocked() {
        return this.mDisplayContent != null ? this.mDisplayContent.isKeyguardLocked() : this.mRootWindowContainer.getDefaultDisplay().isKeyguardLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkKeyguardFlagsChanged() {
        boolean containsDismissKeyguard = containsDismissKeyguardWindow();
        boolean containsShowWhenLocked = containsShowWhenLockedWindow();
        if (containsDismissKeyguard != this.mLastContainsDismissKeyguardWindow || containsShowWhenLocked != this.mLastContainsShowWhenLockedWindow) {
            this.mDisplayContent.notifyKeyguardFlagsChanged();
        }
        this.mLastContainsDismissKeyguardWindow = containsDismissKeyguard;
        this.mLastContainsShowWhenLockedWindow = containsShowWhenLocked;
        this.mLastContainsTurnScreenOnWindow = containsTurnScreenOnWindow();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsDismissKeyguardWindow() {
        Boolean liceRes = ITranWindowManagerService.Instance().onContainsDismissKeyguardWindow(this);
        if (liceRes != null) {
            return liceRes.booleanValue();
        }
        if (isRelaunching()) {
            return this.mLastContainsDismissKeyguardWindow;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if ((((WindowState) this.mChildren.get(i)).mAttrs.flags & 4194304) != 0) {
                return true;
            }
        }
        return false;
    }

    boolean containsShowWhenLockedWindow() {
        if (isRelaunching()) {
            return this.mLastContainsShowWhenLockedWindow;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if ((((WindowState) this.mChildren.get(i)).mAttrs.flags & 524288) != 0) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShowWhenLocked(boolean showWhenLocked) {
        this.mShowWhenLocked = showWhenLocked;
        this.mAtmService.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInheritShowWhenLocked(boolean inheritShowWhenLocked) {
        this.mInheritShownWhenLocked = inheritShowWhenLocked;
        this.mAtmService.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
    }

    private static boolean canShowWhenLocked(ActivityRecord r) {
        ActivityRecord activity;
        if (r == null || r.getTaskFragment() == null) {
            return false;
        }
        if (r.inPinnedWindowingMode() || !(r.mShowWhenLocked || r.containsShowWhenLockedWindow())) {
            if (!r.mInheritShownWhenLocked || (activity = r.getTaskFragment().getActivityBelow(r)) == null || activity.inPinnedWindowingMode()) {
                return false;
            }
            return activity.mShowWhenLocked || activity.containsShowWhenLockedWindow();
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canShowWhenLocked() {
        TaskFragment taskFragment = getTaskFragment();
        if (taskFragment != null && taskFragment.getAdjacentTaskFragment() != null && taskFragment.isEmbedded()) {
            TaskFragment adjacentTaskFragment = taskFragment.getAdjacentTaskFragment();
            ActivityRecord r = adjacentTaskFragment.getTopNonFinishingActivity();
            return canShowWhenLocked(this) && canShowWhenLocked(r);
        }
        return canShowWhenLocked(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canShowWindows() {
        return this.allDrawn && !(isAnimating(2, 1) && hasNonDefaultColorWindow());
    }

    private boolean hasNonDefaultColorWindow() {
        return forAllWindows(new ToBooleanFunction() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda27
            public final boolean apply(Object obj) {
                return ActivityRecord.lambda$hasNonDefaultColorWindow$9((WindowState) obj);
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$hasNonDefaultColorWindow$9(WindowState ws) {
        return ws.mAttrs.getColorMode() != 0;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean forAllActivities(Predicate<ActivityRecord> callback, boolean traverseTopToBottom) {
        return callback.test(this);
    }

    @Override // com.android.server.wm.WindowContainer
    void forAllActivities(Consumer<ActivityRecord> callback, boolean traverseTopToBottom) {
        callback.accept(this);
    }

    @Override // com.android.server.wm.WindowContainer
    ActivityRecord getActivity(Predicate<ActivityRecord> callback, boolean traverseTopToBottom, ActivityRecord boundary) {
        if (callback.test(this)) {
            return this;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void logStartActivity(int tag, Task task) {
        Uri data = this.intent.getData();
        String strData = data != null ? data.toSafeString() : null;
        EventLog.writeEvent(tag, Integer.valueOf(this.mUserId), Integer.valueOf(System.identityHashCode(this)), Integer.valueOf(task.mTaskId), this.shortComponentName, this.intent.getAction(), this.intent.getType(), strData, Integer.valueOf(this.intent.getFlags()));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UriPermissionOwner getUriPermissionsLocked() {
        if (this.uriPermissions == null) {
            this.uriPermissions = new UriPermissionOwner(this.mAtmService.mUgmInternal, this);
        }
        return this.uriPermissions;
    }

    void addResultLocked(ActivityRecord from, String resultWho, int requestCode, int resultCode, Intent resultData) {
        ActivityResult r = new ActivityResult(from, resultWho, requestCode, resultCode, resultData);
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        this.results.add(r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:19:0x002e  */
    /* JADX WARN: Removed duplicated region for block: B:27:0x0033 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void removeResultsLocked(ActivityRecord from, String resultWho, int requestCode) {
        ArrayList<ResultInfo> arrayList = this.results;
        if (arrayList != null) {
            for (int i = arrayList.size() - 1; i >= 0; i--) {
                ActivityResult r = (ActivityResult) this.results.get(i);
                if (r.mFrom == from) {
                    if (r.mResultWho == null) {
                        if (resultWho != null) {
                        }
                        if (r.mRequestCode != requestCode) {
                            this.results.remove(i);
                        }
                    } else {
                        if (!r.mResultWho.equals(resultWho)) {
                        }
                        if (r.mRequestCode != requestCode) {
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendResult(int callingUid, String resultWho, int requestCode, int resultCode, Intent data, NeededUriGrants dataGrants) {
        if (callingUid > 0) {
            this.mAtmService.mUgmInternal.grantUriPermissionUncheckedFromIntent(dataGrants, getUriPermissionsLocked());
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_RESULTS) {
            Slog.v(TAG, "Send activity result to " + this + " : who=" + resultWho + " req=" + requestCode + " res=" + resultCode + " data=" + data);
        }
        if (isState(State.RESUMED) && attachedToProcess()) {
            try {
                ArrayList<ResultInfo> list = new ArrayList<>();
                list.add(new ResultInfo(resultWho, requestCode, resultCode, data));
                this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ClientTransactionItem) ActivityResultItem.obtain(list));
                return;
            } catch (Exception e) {
                Slog.w(TAG, "Exception thrown sending result to " + this, e);
            }
        }
        addResultLocked(null, resultWho, requestCode, resultCode, data);
    }

    private void addNewIntentLocked(ReferrerIntent intent) {
        if (this.newIntents == null) {
            this.newIntents = new ArrayList<>();
        }
        this.newIntents.add(intent);
    }

    final boolean isSleeping() {
        Task rootTask = getRootTask();
        return rootTask != null ? rootTask.shouldSleepActivities() : this.mAtmService.isSleepingLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void deliverNewIntentLocked(int callingUid, Intent intent, NeededUriGrants intentGrants, String referrer) {
        this.mAtmService.mUgmInternal.grantUriPermissionUncheckedFromIntent(intentGrants, getUriPermissionsLocked());
        ReferrerIntent rintent = new ReferrerIntent(intent, getFilteredReferrer(referrer));
        boolean unsent = true;
        boolean isTopActivityWhileSleeping = isTopRunningActivity() && isSleeping();
        if ((this.mState == State.RESUMED || this.mState == State.PAUSED || isTopActivityWhileSleeping) && attachedToProcess()) {
            try {
                ArrayList<ReferrerIntent> ar = new ArrayList<>(1);
                ar.add(rintent);
                this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ClientTransactionItem) NewIntentItem.obtain(ar, this.mState == State.RESUMED));
                unsent = false;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception thrown sending new intent to " + this, e);
            } catch (NullPointerException e2) {
                Slog.w(TAG, "Exception thrown sending new intent to " + this, e2);
            }
        }
        if (unsent) {
            addNewIntentLocked(rintent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateOptionsLocked(ActivityOptions options) {
        if (options != null) {
            if (ActivityTaskManagerDebugConfig.DEBUG_TRANSITION) {
                Slog.i(TAG, "Update options for " + this);
            }
            ActivityOptions activityOptions = this.mPendingOptions;
            if (activityOptions != null) {
                activityOptions.abort();
            }
            setOptions(options);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getLaunchedFromBubble() {
        return this.mLaunchedFromBubble;
    }

    private void setOptions(ActivityOptions options) {
        this.mLaunchedFromBubble = options.getLaunchedFromBubble();
        this.mPendingOptions = options;
        if (options.getAnimationType() == 13) {
            this.mPendingRemoteAnimation = options.getRemoteAnimationAdapter();
        }
        this.mPendingRemoteTransition = options.getRemoteTransition();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyOptionsAnimation() {
        if (ActivityTaskManagerDebugConfig.DEBUG_TRANSITION) {
            Slog.i(TAG, "Applying options for " + this);
        }
        if (this.mPendingRemoteAnimation != null) {
            this.mDisplayContent.mAppTransition.overridePendingAppTransitionRemote(this.mPendingRemoteAnimation);
        } else {
            ActivityOptions activityOptions = this.mPendingOptions;
            if (activityOptions == null || activityOptions.getAnimationType() == 5) {
                return;
            }
            applyOptionsAnimation(this.mPendingOptions, this.intent);
        }
        clearOptionsAnimationForSiblings();
    }

    private void applyOptionsAnimation(ActivityOptions pendingOptions, Intent intent) {
        boolean scaleUp;
        int animationType = pendingOptions.getAnimationType();
        DisplayContent displayContent = getDisplayContent();
        TransitionInfo.AnimationOptions options = null;
        IRemoteCallback startCallback = null;
        IRemoteCallback finishCallback = null;
        switch (animationType) {
            case -1:
            case 0:
                break;
            case 1:
                displayContent.mAppTransition.overridePendingAppTransition(pendingOptions.getPackageName(), pendingOptions.getCustomEnterResId(), pendingOptions.getCustomExitResId(), pendingOptions.getCustomBackgroundColor(), pendingOptions.getAnimationStartedListener(), pendingOptions.getAnimationFinishedListener(), pendingOptions.getOverrideTaskTransition());
                options = TransitionInfo.AnimationOptions.makeCustomAnimOptions(pendingOptions.getPackageName(), pendingOptions.getCustomEnterResId(), pendingOptions.getCustomExitResId(), pendingOptions.getCustomBackgroundColor(), pendingOptions.getOverrideTaskTransition());
                startCallback = pendingOptions.getAnimationStartedListener();
                finishCallback = pendingOptions.getAnimationFinishedListener();
                break;
            case 2:
                displayContent.mAppTransition.overridePendingAppTransitionScaleUp(pendingOptions.getStartX(), pendingOptions.getStartY(), pendingOptions.getWidth(), pendingOptions.getHeight());
                options = TransitionInfo.AnimationOptions.makeScaleUpAnimOptions(pendingOptions.getStartX(), pendingOptions.getStartY(), pendingOptions.getWidth(), pendingOptions.getHeight());
                if (intent.getSourceBounds() == null) {
                    intent.setSourceBounds(new Rect(pendingOptions.getStartX(), pendingOptions.getStartY(), pendingOptions.getStartX() + pendingOptions.getWidth(), pendingOptions.getStartY() + pendingOptions.getHeight()));
                    break;
                }
                break;
            case 3:
            case 4:
                scaleUp = animationType == 3;
                HardwareBuffer buffer = pendingOptions.getThumbnail();
                displayContent.mAppTransition.overridePendingAppTransitionThumb(buffer, pendingOptions.getStartX(), pendingOptions.getStartY(), pendingOptions.getAnimationStartedListener(), scaleUp);
                options = TransitionInfo.AnimationOptions.makeThumbnailAnimOptions(buffer, pendingOptions.getStartX(), pendingOptions.getStartY(), scaleUp);
                startCallback = pendingOptions.getAnimationStartedListener();
                if (intent.getSourceBounds() == null && buffer != null) {
                    intent.setSourceBounds(new Rect(pendingOptions.getStartX(), pendingOptions.getStartY(), pendingOptions.getStartX() + buffer.getWidth(), pendingOptions.getStartY() + buffer.getHeight()));
                    break;
                }
                break;
            case 5:
            case 6:
            case 7:
            case 10:
            default:
                Slog.e(WmsExt.TAG, "applyOptionsLocked: Unknown animationType=" + animationType);
                break;
            case 8:
            case 9:
                AppTransitionAnimationSpec[] specs = pendingOptions.getAnimSpecs();
                IAppTransitionAnimationSpecsFuture specsFuture = pendingOptions.getSpecsFuture();
                if (specsFuture != null) {
                    AppTransition appTransition = displayContent.mAppTransition;
                    IRemoteCallback animationStartedListener = pendingOptions.getAnimationStartedListener();
                    scaleUp = animationType == 8;
                    appTransition.overridePendingAppTransitionMultiThumbFuture(specsFuture, animationStartedListener, scaleUp);
                    break;
                } else if (animationType == 9 && specs != null) {
                    displayContent.mAppTransition.overridePendingAppTransitionMultiThumb(specs, pendingOptions.getAnimationStartedListener(), pendingOptions.getAnimationFinishedListener(), false);
                    break;
                } else {
                    displayContent.mAppTransition.overridePendingAppTransitionAspectScaledThumb(pendingOptions.getThumbnail(), pendingOptions.getStartX(), pendingOptions.getStartY(), pendingOptions.getWidth(), pendingOptions.getHeight(), pendingOptions.getAnimationStartedListener(), animationType == 8);
                    if (intent.getSourceBounds() == null) {
                        intent.setSourceBounds(new Rect(pendingOptions.getStartX(), pendingOptions.getStartY(), pendingOptions.getStartX() + pendingOptions.getWidth(), pendingOptions.getStartY() + pendingOptions.getHeight()));
                        break;
                    }
                }
                break;
            case 11:
                displayContent.mAppTransition.overridePendingAppTransitionClipReveal(pendingOptions.getStartX(), pendingOptions.getStartY(), pendingOptions.getWidth(), pendingOptions.getHeight());
                options = TransitionInfo.AnimationOptions.makeClipRevealAnimOptions(pendingOptions.getStartX(), pendingOptions.getStartY(), pendingOptions.getWidth(), pendingOptions.getHeight());
                if (intent.getSourceBounds() == null) {
                    intent.setSourceBounds(new Rect(pendingOptions.getStartX(), pendingOptions.getStartY(), pendingOptions.getStartX() + pendingOptions.getWidth(), pendingOptions.getStartY() + pendingOptions.getHeight()));
                    break;
                }
                break;
            case 12:
                displayContent.mAppTransition.overridePendingAppTransitionStartCrossProfileApps();
                options = TransitionInfo.AnimationOptions.makeCrossProfileAnimOptions();
                break;
        }
        if (options != null) {
            this.mTransitionController.setOverrideAnimation(options, startCallback, finishCallback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAllDrawn() {
        this.allDrawn = false;
        this.mLastAllDrawn = false;
    }

    private boolean allDrawnStatesConsidered() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState child = (WindowState) this.mChildren.get(i);
            if (child.mightAffectAllDrawn() && !child.getDrawnStateEvaluated()) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAllDrawn() {
        int numInteresting;
        if (!this.allDrawn && (numInteresting = this.mNumInterestingWindows) > 0 && allDrawnStatesConsidered() && this.mNumDrawnWindows >= numInteresting && !isRelaunching()) {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY || Build.IS_DEBUG_ENABLE) {
                Slog.v(TAG, "System monitor allDrawn: " + this + " interesting=" + numInteresting + " drawn=" + this.mNumDrawnWindows);
            }
            this.allDrawn = true;
            if (this.mDisplayContent != null) {
                this.mDisplayContent.setLayoutNeeded();
            }
            this.mWmService.mH.obtainMessage(32, this).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void abortAndClearOptionsAnimation() {
        ActivityOptions activityOptions = this.mPendingOptions;
        if (activityOptions != null) {
            activityOptions.abort();
        }
        clearOptionsAnimation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearOptionsAnimation() {
        this.mPendingOptions = null;
        this.mPendingRemoteAnimation = null;
        this.mPendingRemoteTransition = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearOptionsAnimationForSiblings() {
        Task task = this.task;
        if (task == null) {
            clearOptionsAnimation();
        } else {
            task.forAllActivities(new Consumer() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda19
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ActivityRecord) obj).clearOptionsAnimation();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityOptions getOptions() {
        return this.mPendingOptions;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityOptions takeOptions() {
        if (ActivityTaskManagerDebugConfig.DEBUG_TRANSITION) {
            Slog.i(TAG, "Taking options for " + this + " callers=" + Debug.getCallers(6));
        }
        if (this.mPendingOptions == null) {
            return null;
        }
        ActivityOptions opts = this.mPendingOptions;
        this.mPendingOptions = null;
        opts.setRemoteTransition(null);
        opts.setRemoteAnimationAdapter(null);
        return opts;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RemoteTransition takeRemoteTransition() {
        RemoteTransition out = this.mPendingRemoteTransition;
        this.mPendingRemoteTransition = null;
        return out;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean allowMoveToFront() {
        ActivityOptions activityOptions = this.mPendingOptions;
        return activityOptions == null || !activityOptions.getAvoidMoveToFront();
    }

    void removeUriPermissionsLocked() {
        UriPermissionOwner uriPermissionOwner = this.uriPermissions;
        if (uriPermissionOwner != null) {
            uriPermissionOwner.removeUriPermissions();
            this.uriPermissions = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pauseKeyDispatchingLocked() {
        if (!this.keysPaused) {
            this.keysPaused = true;
            if (getDisplayContent() != null) {
                getDisplayContent().getInputMonitor().pauseDispatchingLw(this);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resumeKeyDispatchingLocked() {
        if (this.keysPaused) {
            this.keysPaused = false;
            if (getDisplayContent() != null) {
                getDisplayContent().getInputMonitor().resumeDispatchingLw(this);
            }
        }
    }

    private void updateTaskDescription(CharSequence description) {
        this.task.lastDescription = description;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeferHidingClient(boolean deferHidingClient) {
        if (this.mDeferHidingClient == deferHidingClient) {
            return;
        }
        this.mDeferHidingClient = deferHidingClient;
        if (!deferHidingClient && !this.mVisibleRequested) {
            setVisibility(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getDeferHidingClient() {
        return this.mDeferHidingClient;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isVisible() {
        return this.mVisible;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isVisibleRequested() {
        return this.mVisibleRequested;
    }

    void setVisible(boolean visible) {
        if (visible != this.mVisible) {
            TranFoldWMCustody.instance().onActivityRecordSetVisible(this, visible);
            this.mVisible = visible;
            WindowProcessController windowProcessController = this.app;
            if (windowProcessController != null) {
                this.mTaskSupervisor.onProcessActivityStateChanged(windowProcessController, false);
            }
            scheduleAnimation();
        }
    }

    private void setVisibleRequested(boolean visible) {
        if (visible == this.mVisibleRequested) {
            return;
        }
        this.mVisibleRequested = visible;
        setInsetsFrozen(!visible);
        WindowProcessController windowProcessController = this.app;
        if (windowProcessController != null) {
            this.mTaskSupervisor.onProcessActivityStateChanged(windowProcessController, false);
        }
        logAppCompatState();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setVisibility(boolean visible) {
        if (getParent() == null) {
            Slog.w(WmsExt.TAG, "Attempted to set visibility of non-existing app token: " + this.token);
        } else if (shouldDeferShowForEnteringPinnedMode(visible)) {
            Slog.d(TAG, "PIP: defer show for entering pip: " + this);
        } else {
            if (visible) {
                this.mDeferHidingClient = false;
            }
            setVisibility(visible, this.mDeferHidingClient);
            this.mAtmService.addWindowLayoutReasons(2);
            this.mTaskSupervisor.getActivityMetricsLogger().notifyVisibilityChanged(this);
            this.mTaskSupervisor.mAppVisibilitiesChangedSinceLastPause = true;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r6v8, resolved type: int */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r6v11 */
    /* JADX WARN: Type inference failed for: r6v21 */
    /* JADX WARN: Type inference failed for: r6v6 */
    /* JADX WARN: Type inference failed for: r6v7 */
    void setVisibility(boolean visible, boolean deferHidingClient) {
        int i;
        WindowState win;
        ActivityRecord focusedActivity;
        AppTransition appTransition = getDisplayContent().mAppTransition;
        if (!visible && !this.mVisibleRequested) {
            if (!deferHidingClient && this.mLastDeferHidingClient) {
                this.mLastDeferHidingClient = deferHidingClient;
                setClientVisible(false);
                return;
            }
            return;
        }
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            String protoLogParam0 = String.valueOf(this.token);
            String protoLogParam2 = String.valueOf(appTransition);
            boolean protoLogParam3 = isVisible();
            boolean protoLogParam4 = this.mVisibleRequested;
            String protoLogParam5 = String.valueOf(Debug.getCallers(6));
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -374767836, 972, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(visible), protoLogParam2, Boolean.valueOf(protoLogParam3), Boolean.valueOf(protoLogParam4), protoLogParam5});
        }
        this.mTransitionController.collect(this);
        onChildVisibilityRequested(visible);
        DisplayContent displayContent = getDisplayContent();
        displayContent.mOpeningApps.remove(this);
        displayContent.mClosingApps.remove(this);
        this.waitingToShow = false;
        setVisibleRequested(visible);
        this.mLastDeferHidingClient = deferHidingClient;
        if (!visible) {
            removeDeadWindows();
            if (this.finishing || isState(State.STOPPED)) {
                displayContent.mUnknownAppVisibilityController.appRemovedOrHidden(this);
            }
        } else {
            if (!appTransition.isTransitionSet() && appTransition.isReady()) {
                displayContent.mOpeningApps.add(this);
            }
            this.startingMoved = false;
            if (!isVisible() || this.mAppStopped) {
                clearAllDrawn();
                if (isVisible()) {
                    i = 1;
                } else {
                    i = 1;
                    i = 1;
                    this.waitingToShow = true;
                    if (!isClientVisible()) {
                        forAllWindows(new Consumer() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda1
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                ActivityRecord.lambda$setVisibility$10((WindowState) obj);
                            }
                        }, true);
                    }
                }
            } else {
                i = 1;
            }
            setClientVisible(i);
            requestUpdateWallpaperIfNeeded();
            if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                String protoLogParam02 = String.valueOf(this);
                ProtoLogGroup protoLogGroup = ProtoLogGroup.WM_DEBUG_ADD_REMOVE;
                Object[] objArr = new Object[i];
                objArr[0] = protoLogParam02;
                ProtoLogImpl.v(protoLogGroup, 1224184681, 0, (String) null, objArr);
            }
            this.mAppStopped = false;
            transferStartingWindowFromHiddenAboveTokenIfNeeded();
        }
        if (visible || !inTransition()) {
            boolean recentsAnimating = isAnimating(2, 8);
            boolean isEnteringPipWithoutVisibleChange = this.mWaitForEnteringPinnedMode && this.mVisible == visible;
            if ((!okToAnimate(true, canTurnScreenOn()) && !this.mVisibleByWakeupOptimizePath) || ((!appTransition.isTransitionSet() && (!recentsAnimating || isActivityTypeHome())) || isEnteringPipWithoutVisibleChange)) {
                commitVisibility(visible, true);
                updateReportedVisibilityLocked();
                return;
            }
            if (visible) {
                displayContent.mOpeningApps.add(this);
                this.mEnteringAnimation = true;
            } else if (this.mVisible) {
                displayContent.mClosingApps.add(this);
                this.mEnteringAnimation = false;
            }
            if ((appTransition.getTransitFlags() & 32) != 0 && (win = getDisplayContent().findFocusedWindow()) != null && (focusedActivity = win.mActivityRecord) != null) {
                if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                    String protoLogParam03 = String.valueOf(focusedActivity);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 1810019902, 0, (String) null, new Object[]{protoLogParam03});
                }
                displayContent.mOpeningApps.add(focusedActivity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setVisibility$10(WindowState w) {
        if (w.mWinAnimator.mDrawState == 4) {
            w.mWinAnimator.resetDrawState();
            w.forceReportingResized();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prepareOpenningAction() {
        if (isVisible()) {
            getDisplayContent().mOpeningApps.add(this);
            clearAllDrawn();
            forAllWindows(new Consumer() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda32
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ActivityRecord.lambda$prepareOpenningAction$11((WindowState) obj);
                }
            }, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$prepareOpenningAction$11(WindowState w) {
        if (w.mWinAnimator.mDrawState == 4) {
            w.mWinAnimator.resetDrawState();
            w.forceReportingResized();
        }
    }

    @Override // com.android.server.wm.WindowContainer
    boolean applyAnimation(WindowManager.LayoutParams lp, int transit, boolean enter, boolean isVoiceInteraction, ArrayList<WindowContainer> sources) {
        if (this.mUseTransferredAnimation) {
            return false;
        }
        this.mRequestForceTransition = false;
        return super.applyAnimation(lp, transit, enter, isVoiceInteraction, sources);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void commitVisibility(boolean visible, boolean performLayout, boolean fromTransition) {
        this.mVisibleSetFromTransferredStartingWindow = false;
        this.mUseTransferredAnimation = false;
        if (visible == isVisible()) {
            return;
        }
        int windowsCount = this.mChildren.size();
        for (int i = 0; i < windowsCount; i++) {
            ((WindowState) this.mChildren.get(i)).onAppVisibilityChanged(visible, isAnimating(2, 1));
        }
        setVisible(visible);
        setVisibleRequested(visible);
        ITranWindowManagerService.Instance().onCommitVisibility(this, visible);
        if (!visible) {
            stopFreezingScreen(true, true);
        } else {
            WindowState windowState = this.mStartingWindow;
            if (windowState != null && !windowState.isDrawn() && (this.firstWindowDrawn || this.allDrawn)) {
                this.mStartingWindow.clearPolicyVisibilityFlag(1);
                this.mStartingWindow.mLegacyPolicyVisibilityAfterAnim = false;
            }
            final WindowManagerService windowManagerService = this.mWmService;
            Objects.requireNonNull(windowManagerService);
            forAllWindows(new Consumer() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda21
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    WindowManagerService.this.makeWindowFreezingScreenIfNeededLocked((WindowState) obj);
                }
            }, true);
        }
        for (Task task = getOrganizedTask(); task != null; task = task.getParent().asTask()) {
            task.dispatchTaskInfoChangedIfNeeded(false);
        }
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
            String protoLogParam0 = String.valueOf(this);
            boolean protoLogParam1 = isVisible();
            boolean protoLogParam2 = this.mVisibleRequested;
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, -1521427940, 60, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2)});
        }
        DisplayContent displayContent = getDisplayContent();
        displayContent.getInputMonitor().setUpdateInputWindowsNeededLw();
        if (performLayout) {
            this.mWmService.updateFocusedWindowLocked(3, false);
            this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
        }
        displayContent.getInputMonitor().updateInputWindowsLw(false);
        postApplyAnimation(visible, fromTransition);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void commitVisibility(boolean visible, boolean performLayout) {
        commitVisibility(visible, performLayout, false);
    }

    private void postApplyAnimation(boolean visible, boolean fromTransition) {
        boolean usingShellTransitions = this.mTransitionController.isShellTransitionsEnabled();
        boolean delayed = isAnimating(6, 25);
        if (!delayed && !usingShellTransitions) {
            onAnimationFinished(1, null);
            if (visible) {
                this.mEnteringAnimation = true;
                this.mWmService.mActivityManagerAppTransitionNotifier.onAppTransitionFinishedLocked(this.token);
            }
        }
        if (visible || !isAnimating(2, 9) || usingShellTransitions) {
            setClientVisible(visible);
        }
        if (!visible) {
            InputTarget imeInputTarget = this.mDisplayContent.getImeInputTarget();
            this.mLastImeShown = (imeInputTarget == null || imeInputTarget.getWindowState() == null || imeInputTarget.getWindowState().mActivityRecord != this || this.mDisplayContent.mInputMethodWindow == null || !this.mDisplayContent.mInputMethodWindow.isVisible()) ? false : true;
            this.mImeInsetsFrozenUntilStartInput = true;
        }
        DisplayContent displayContent = getDisplayContent();
        if (!displayContent.mClosingApps.contains(this) && !displayContent.mOpeningApps.contains(this) && !fromTransition) {
            this.mWmService.mTaskSnapshotController.notifyAppVisibilityChanged(this, visible);
        }
        if (!isVisible() && !delayed && !displayContent.mAppTransition.isTransitionSet()) {
            SurfaceControl.openTransaction();
            try {
                forAllWindows(new Consumer() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda26
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((WindowState) obj).mWinAnimator.hide(SurfaceControl.getGlobalTransaction(), "immediately hidden");
                    }
                }, true);
            } finally {
                SurfaceControl.closeTransaction();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldApplyAnimation(boolean visible) {
        if (isVisible() != visible || this.mRequestForceTransition) {
            return true;
        }
        if (isVisible() || !this.mIsExiting) {
            return visible && forAllWindows(new ToBooleanFunction() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda2
                public final boolean apply(Object obj) {
                    return ((WindowState) obj).waitingForReplacement();
                }
            }, true);
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRecentsScreenshotEnabled(boolean enabled) {
        this.mEnableRecentsScreenshot = enabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldUseAppThemeSnapshot() {
        return !this.mEnableRecentsScreenshot || forAllWindows(new ToBooleanFunction() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda24
            public final boolean apply(Object obj) {
                return ((WindowState) obj).isSecureLocked();
            }
        }, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCurrentLaunchCanTurnScreenOn(boolean currentLaunchCanTurnScreenOn) {
        this.mCurrentLaunchCanTurnScreenOn = currentLaunchCanTurnScreenOn;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean currentLaunchCanTurnScreenOn() {
        return this.mCurrentLaunchCanTurnScreenOn;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:46:0x00f7  */
    /* JADX WARN: Removed duplicated region for block: B:49:0x0104  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void setState(State state, String reason) {
        WindowProcessController windowProcessController;
        ContentCaptureManagerInternal contentCaptureService;
        WindowProcessController windowProcessController2;
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(this);
            String protoLogParam1 = String.valueOf(getState());
            String protoLogParam2 = String.valueOf(state);
            String protoLogParam3 = String.valueOf(reason);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 1316533291, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2, protoLogParam3});
        }
        if (state == this.mState) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam02 = String.valueOf(state);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -926231510, 0, (String) null, new Object[]{protoLogParam02});
                return;
            }
            return;
        }
        State oldState = this.mState;
        this.mState = state;
        if (getTaskFragment() != null) {
            getTaskFragment().onActivityStateChanged(this, state, reason);
        }
        if (state == State.STOPPING && !isSleeping() && getParent() == null) {
            Slog.w(WmsExt.TAG, "Attempted to notify stopping on non-existing app token: " + this.token);
            return;
        }
        WindowProcessController windowProcessController3 = this.app;
        if (windowProcessController3 != null) {
            this.mTaskSupervisor.onProcessActivityStateChanged(windowProcessController3, false);
        }
        switch (AnonymousClass6.$SwitchMap$com$android$server$wm$ActivityRecord$State[state.ordinal()]) {
            case 1:
                this.mAtmService.updateBatteryStats(this, true);
                this.mAtmService.updateActivityUsageStats(this, 1);
                ITranActivityPerformance.Instance().setActivityResumed(this.packageName, this.mActivityComponent.getShortClassName(), reason, System.identityHashCode(this));
                windowProcessController = this.app;
                if (windowProcessController != null) {
                    windowProcessController.updateProcessInfo(false, true, true, true);
                }
                contentCaptureService = (ContentCaptureManagerInternal) LocalServices.getService(ContentCaptureManagerInternal.class);
                if (contentCaptureService != null) {
                    contentCaptureService.notifyActivityEvent(this.mUserId, this.mActivityComponent, 10000);
                    break;
                }
                break;
            case 3:
                this.mAtmService.updateBatteryStats(this, false);
                this.mAtmService.updateActivityUsageStats(this, 2);
                break;
            case 5:
                windowProcessController = this.app;
                if (windowProcessController != null) {
                }
                contentCaptureService = (ContentCaptureManagerInternal) LocalServices.getService(ContentCaptureManagerInternal.class);
                if (contentCaptureService != null) {
                }
                break;
            case 6:
                this.mAtmService.updateActivityUsageStats(this, 23);
                break;
            case 7:
                if (this.app != null && (this.mVisible || this.mVisibleRequested)) {
                    this.mAtmService.updateBatteryStats(this, false);
                }
                this.mAtmService.updateActivityUsageStats(this, 24);
                windowProcessController2 = this.app;
                if (windowProcessController2 != null && !windowProcessController2.hasActivities()) {
                    this.app.updateProcessInfo(true, false, true, false);
                    break;
                }
                break;
            case 8:
                windowProcessController2 = this.app;
                if (windowProcessController2 != null) {
                    this.app.updateProcessInfo(true, false, true, false);
                    break;
                }
                break;
        }
        ITranWindowManagerService.Instance().onSetActivityRecordState(this, oldState == null ? -1 : oldState.ordinal(), state != null ? state.ordinal() : -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public State getState() {
        return this.mState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isState(State state) {
        return state == this.mState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isState(State state1, State state2) {
        State state = this.mState;
        return state1 == state || state2 == state;
    }

    boolean isState(State state1, State state2, State state3) {
        State state = this.mState;
        return state1 == state || state2 == state || state3 == state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isState(State state1, State state2, State state3, State state4) {
        State state = this.mState;
        return state1 == state || state2 == state || state3 == state || state4 == state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isState(State state1, State state2, State state3, State state4, State state5) {
        State state = this.mState;
        return state1 == state || state2 == state || state3 == state || state4 == state || state5 == state;
    }

    boolean isState(State state1, State state2, State state3, State state4, State state5, State state6) {
        State state = this.mState;
        return state1 == state || state2 == state || state3 == state || state4 == state || state5 == state || state6 == state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroySurfaces() {
        destroySurfaces(false);
    }

    private void destroySurfaces(boolean cleanupOnResume) {
        boolean destroyedSomething = false;
        ArrayList<WindowState> children = new ArrayList<>(this.mChildren);
        for (int i = children.size() - 1; i >= 0; i--) {
            WindowState win = children.get(i);
            destroyedSomething |= win.destroySurface(cleanupOnResume, this.mAppStopped);
        }
        if (destroyedSomething) {
            DisplayContent dc = getDisplayContent();
            dc.assignWindowLayers(true);
            updateLetterboxSurface(null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyAppResumed(boolean wasStopped) {
        if (getParent() == null) {
            Slog.w(WmsExt.TAG, "Attempted to notify resumed of non-existing app token: " + this.token);
            return;
        }
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam1 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 1364498663, 3, (String) null, new Object[]{Boolean.valueOf(wasStopped), protoLogParam1});
        }
        this.mAppStopped = false;
        setCurrentLaunchCanTurnScreenOn(true);
        if (!wasStopped) {
            destroySurfaces(true);
        }
    }

    void notifyAppStopped() {
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 1903353011, 0, (String) null, new Object[]{protoLogParam0});
        }
        this.mAppStopped = true;
        this.firstWindowDrawn = false;
        Task task = this.task;
        if (task != null && task.mLastRecentsAnimationTransaction != null) {
            this.task.clearLastRecentsAnimationTransaction(true);
        }
        this.mDisplayContent.mPinnedTaskController.onActivityHidden(this.mActivityComponent);
        this.mDisplayContent.mUnknownAppVisibilityController.appRemovedOrHidden(this);
        if (isClientVisible()) {
            setClientVisible(false);
        }
        destroySurfaces();
        removeStartingWindow();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyUnknownVisibilityLaunchedForKeyguardTransition() {
        if (this.noDisplay || !this.mTaskSupervisor.getKeyguardController().isKeyguardLocked() || this.mTaskSupervisor.getKeyguardController().isKeyguardGoingAwayQuickly()) {
            return;
        }
        this.mDisplayContent.mUnknownAppVisibilityController.notifyLaunched(this);
    }

    private boolean shouldBeVisible(boolean behindOccludedContainer, boolean ignoringKeyguard) {
        updateVisibilityIgnoringKeyguard(behindOccludedContainer);
        if (ignoringKeyguard) {
            return this.visibleIgnoringKeyguard;
        }
        return shouldBeVisibleUnchecked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldBeVisibleUnchecked() {
        Task rootTask = getRootTask();
        if (rootTask == null || !this.visibleIgnoringKeyguard) {
            return false;
        }
        if ((!inPinnedWindowingMode() || !rootTask.isForceHidden()) && !hasOverlayOverUntrustedModeEmbedded()) {
            if (ITranWindowManagerService.Instance().isCurrentActivityKeepAwake(this.mActivityComponent.getClassName(), false)) {
                return this.visibleIgnoringKeyguard;
            }
            if (this.mDisplayContent.isSleeping()) {
                boolean keyguardGoingAwayQuickly = this.mTaskSupervisor.getKeyguardController().isKeyguardGoingAwayQuickly();
                boolean canHideByFingerprint = TranFpUnlockStateController.getInstance().canHideByFingerprint();
                if (!keyguardGoingAwayQuickly && !canHideByFingerprint) {
                    return canTurnScreenOn();
                }
                return this.mTaskSupervisor.getKeyguardController().checkKeyguardVisibility(this);
            }
            return this.mTaskSupervisor.getKeyguardController().checkKeyguardVisibility(this);
        }
        return false;
    }

    boolean hasOverlayOverUntrustedModeEmbedded() {
        if (!isEmbeddedInUntrustedMode() || getTask() == null) {
            return false;
        }
        ActivityRecord differentUidOverlayActivity = getTask().getActivity(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda5
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ActivityRecord.this.m7778x8a662b59((ActivityRecord) obj);
            }
        }, this, false, false);
        return differentUidOverlayActivity != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$hasOverlayOverUntrustedModeEmbedded$13$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ boolean m7778x8a662b59(ActivityRecord a) {
        return a.getUid() != getUid();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateVisibilityIgnoringKeyguard(boolean behindOccludedContainer) {
        this.visibleIgnoringKeyguard = (!behindOccludedContainer || this.mLaunchTaskBehind) && showToCurrentUser();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldBeVisible() {
        Task task = getTask();
        if (task == null) {
            return false;
        }
        boolean behindOccludedContainer = (task.shouldBeVisible(null) && task.getOccludingActivityAbove(this) == null) ? false : true;
        return shouldBeVisible(behindOccludedContainer, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void makeVisibleIfNeeded(ActivityRecord starting, boolean reportToClient) {
        if ((this.mState == State.RESUMED && this.mVisibleRequested) || this == starting) {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.d(TAG_VISIBILITY, "Not making visible, r=" + this + " state=" + this.mState + " starting=" + starting);
                return;
            }
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG_VISIBILITY, "Making visible and scheduling visibility: " + this);
        }
        Task rootTask = getRootTask();
        try {
            if (rootTask.mTranslucentActivityWaiting != null) {
                updateOptionsLocked(this.returningOptions);
                rootTask.mUndrawnActivitiesBelowTopTranslucent.add(this);
            }
            setVisibility(true);
            this.app.postPendingUiCleanMsg(true);
            if (reportToClient) {
                this.mClientVisibilityDeferred = false;
                makeActiveIfNeeded(starting);
            } else {
                this.mClientVisibilityDeferred = true;
            }
            this.mTaskSupervisor.mStoppingActivities.remove(this);
        } catch (Exception e) {
            Slog.w(TAG, "Exception thrown making visible: " + this.intent.getComponent(), e);
        }
        handleAlreadyVisible();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void makeInvisible() {
        boolean deferHidingClient;
        if (!this.mVisibleRequested) {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Already invisible: " + this);
                return;
            }
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG_VISIBILITY, "Making invisible: " + this + ", state=" + getState());
        }
        try {
            boolean canEnterPictureInPicture = checkEnterPictureInPictureState("makeInvisible", true);
            if (canEnterPictureInPicture && !isState(State.STARTED, State.STOPPING, State.STOPPED, State.PAUSED)) {
                deferHidingClient = true;
            } else {
                deferHidingClient = false;
            }
            setDeferHidingClient(deferHidingClient);
            setVisibility(false);
            switch (AnonymousClass6.$SwitchMap$com$android$server$wm$ActivityRecord$State[getState().ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 5:
                case 9:
                    addToStopping(true, canEnterPictureInPicture, "makeInvisible");
                    return;
                case 4:
                case 6:
                    this.supportsEnterPipOnTaskSwitch = false;
                    return;
                case 7:
                case 8:
                default:
                    return;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Exception thrown making hidden: " + this.intent.getComponent(), e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean makeActiveIfNeeded(ActivityRecord activeActivity) {
        if (shouldResumeActivity(activeActivity)) {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Resume visible activity, " + this);
            }
            return getRootTask().resumeTopActivityUncheckedLocked(activeActivity, null);
        }
        if (shouldPauseActivity(activeActivity)) {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Pause visible activity, " + this);
            }
            setState(State.PAUSING, "makeActiveIfNeeded");
            try {
                this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ActivityLifecycleItem) PauseActivityItem.obtain(this.finishing, false, this.configChangeFlags, false));
            } catch (Exception e) {
                Slog.w(TAG, "Exception thrown sending pause: " + this.intent.getComponent(), e);
            }
        } else if (shouldStartActivity()) {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Start visible activity, " + this);
            }
            setState(State.STARTED, "makeActiveIfNeeded");
            try {
                this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ActivityLifecycleItem) StartActivityItem.obtain(takeOptions()));
            } catch (Exception e2) {
                Slog.w(TAG, "Exception thrown sending start: " + this.intent.getComponent(), e2);
            }
            this.mTaskSupervisor.mStoppingActivities.remove(this);
        }
        return false;
    }

    boolean shouldPauseActivity(ActivityRecord activeActivity) {
        return shouldMakeActive(activeActivity) && !isFocusable() && !isState(State.PAUSING, State.PAUSED) && this.results == null;
    }

    boolean shouldResumeActivity(ActivityRecord activeActivity) {
        return shouldBeResumed(activeActivity) && !isState(State.RESUMED);
    }

    private boolean shouldBeResumed(ActivityRecord activeActivity) {
        return shouldMakeActive(activeActivity) && isFocusable() && getTaskFragment().getVisibility(activeActivity) == 0 && canResumeByCompat();
    }

    private boolean shouldStartActivity() {
        return this.mVisibleRequested && (isState(State.STOPPED) || isState(State.STOPPING));
    }

    boolean shouldMakeActive(ActivityRecord activeActivity) {
        if (isState(State.STARTED, State.RESUMED, State.PAUSED, State.STOPPED, State.STOPPING) && getRootTask().mTranslucentActivityWaiting == null && this != activeActivity && this.mTaskSupervisor.readyToResume() && !this.mLaunchTaskBehind) {
            if (this.task.hasChild(this)) {
                return getTaskFragment().topRunningActivity() == this;
            }
            throw new IllegalStateException("Activity not found in its task");
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleAlreadyVisible() {
        stopFreezingScreenLocked(false);
        try {
            if (this.returningOptions != null) {
                this.app.getThread().scheduleOnNewActivityOptions(this.token, this.returningOptions.toBundle());
            }
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void activityResumedLocked(IBinder token, boolean handleSplashScreenExit) {
        ActivityRecord r = forTokenLocked(token);
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(r);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_STATES, 1364126018, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (r == null) {
            return;
        }
        r.setCustomizeSplashScreenExitAnimation(handleSplashScreenExit);
        r.setSavedState(null);
        r.mDisplayContent.handleActivitySizeCompatModeIfNeeded(r);
        r.mDisplayContent.mUnknownAppVisibilityController.notifyAppResumedFinished(r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void splashScreenAttachedLocked(IBinder token) {
        ActivityRecord r = forTokenLocked(token);
        if (r == null) {
            Slog.w(TAG, "splashScreenTransferredLocked cannot find activity");
        } else {
            r.onSplashScreenAttachComplete();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void completeResumeLocked() {
        boolean wasVisible = this.mVisibleRequested;
        setVisibility(true);
        if (!wasVisible) {
            this.mTaskSupervisor.mAppVisibilitiesChangedSinceLastPause = true;
        }
        this.idle = false;
        this.results = null;
        ArrayList<ReferrerIntent> arrayList = this.newIntents;
        if (arrayList != null && arrayList.size() > 0) {
            ArrayList<ReferrerIntent> arrayList2 = this.newIntents;
            this.mLastNewIntent = arrayList2.get(arrayList2.size() - 1);
        }
        this.newIntents = null;
        this.stopped = false;
        if (isActivityTypeHome()) {
            if (ITranGriffinFeature.Instance().isGriffinSupport()) {
                this.mTaskSupervisor.updateHomeProcess(this.task.getBottomMostActivity());
            } else {
                this.mTaskSupervisor.updateHomeProcess(this.task.getBottomMostActivity().app);
            }
        }
        if (this.nowVisible) {
            this.mTaskSupervisor.stopWaitingForActivityVisible(this);
        }
        this.mTaskSupervisor.scheduleIdleTimeout(this);
        this.mTaskSupervisor.reportResumedActivityLocked(this);
        resumeKeyDispatchingLocked();
        Task rootTask = getRootTask();
        this.mTaskSupervisor.mNoAnimActivities.clear();
        this.returningOptions = null;
        if (canTurnScreenOn()) {
            this.mTaskSupervisor.wakeUp("turnScreenOnFlag");
        } else {
            rootTask.checkReadyForSleep();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void activityPaused(boolean timeout) {
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(this.token);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 1068803972, 12, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(timeout)});
        }
        TaskFragment taskFragment = getTaskFragment();
        if (taskFragment != null) {
            removePauseTimeout();
            ActivityRecord pausingActivity = taskFragment.getPausingActivity();
            if (pausingActivity == this) {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam02 = String.valueOf(this);
                    String protoLogParam1 = String.valueOf(timeout ? "(due to timeout)" : " (pause complete)");
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 397382873, 0, (String) null, new Object[]{protoLogParam02, protoLogParam1});
                }
                this.mAtmService.deferWindowLayout();
                try {
                    taskFragment.completePause(true, null);
                    return;
                } finally {
                    this.mAtmService.continueWindowLayout();
                }
            }
            EventLogTags.writeWmFailedToPause(this.mUserId, System.identityHashCode(this), this.shortComponentName, pausingActivity != null ? pausingActivity.shortComponentName : "(none)");
            if (isState(State.PAUSING)) {
                setState(State.PAUSED, "activityPausedLocked");
                if (this.finishing) {
                    if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                        String protoLogParam03 = String.valueOf(this);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -937498525, 0, (String) null, new Object[]{protoLogParam03});
                    }
                    completeFinishing("activityPausedLocked");
                }
            }
        }
        this.mDisplayContent.handleActivitySizeCompatModeIfNeeded(this);
        this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void schedulePauseTimeout() {
        this.pauseTime = SystemClock.uptimeMillis();
        this.mAtmService.mH.postDelayed(this.mPauseTimeoutRunnable, 500L);
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -705939410, 0, (String) null, (Object[]) null);
        }
    }

    private void removePauseTimeout() {
        this.mAtmService.mH.removeCallbacks(this.mPauseTimeoutRunnable);
    }

    private void removeDestroyTimeout() {
        this.mAtmService.mH.removeCallbacks(this.mDestroyTimeoutRunnable);
    }

    private void removeStopTimeout() {
        this.mAtmService.mH.removeCallbacks(this.mStopTimeoutRunnable);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeTimeouts() {
        this.mTaskSupervisor.removeIdleTimeoutForActivity(this);
        removePauseTimeout();
        removeStopTimeout();
        removeDestroyTimeout();
        finishLaunchTickingLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopIfPossible() {
        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
            Slog.d(TAG_SWITCH, "Stopping: " + this);
        }
        Task rootTask = getRootTask();
        if (isNoHistory() && !this.finishing) {
            if (!rootTask.shouldSleepActivities()) {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam0 = String.valueOf(this);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -1136139407, 0, (String) null, new Object[]{protoLogParam0});
                }
                if (finishIfPossible("stop-no-history", false) != 0) {
                    resumeKeyDispatchingLocked();
                    return;
                }
            } else if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam02 = String.valueOf(this);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, 485170982, 0, (String) null, new Object[]{protoLogParam02});
            }
        }
        if (!attachedToProcess()) {
            return;
        }
        resumeKeyDispatchingLocked();
        try {
            this.stopped = false;
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam03 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 189628502, 0, (String) null, new Object[]{protoLogParam03});
            }
            if (isState(State.RESUMED, State.PAUSED)) {
                this.mAtmService.mAmsExt.onActivityStateChanged(this, false);
            }
            setState(State.STOPPING, "stopIfPossible");
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG_VISIBILITY, "Stopping:" + this);
            }
            ITranWindowManagerService.Instance().onStopActivityLocked(this);
            EventLogTags.writeWmStopActivity(this.mUserId, System.identityHashCode(this), this.shortComponentName);
            this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ActivityLifecycleItem) StopActivityItem.obtain(this.configChangeFlags));
            this.mAtmService.mH.postDelayed(this.mStopTimeoutRunnable, 11000L);
        } catch (Exception e) {
            Slog.w(TAG, "Exception thrown during pause", e);
            this.stopped = true;
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam04 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 306524472, 0, (String) null, new Object[]{protoLogParam04});
            }
            setState(State.STOPPED, "stopIfPossible");
            if (this.deferRelaunchUntilPaused) {
                destroyImmediately("stop-except");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void activityStopped(Bundle newIcicle, PersistableBundle newPersistentState, CharSequence description) {
        boolean isStopping = this.mState == State.STOPPING;
        if (!isStopping && this.mState != State.RESTARTING_PROCESS) {
            Slog.i(TAG, "Activity reported stop, but no longer stopping: " + this + " " + this.mState);
            removeStopTimeout();
            return;
        }
        if (newPersistentState != null) {
            this.mPersistentState = newPersistentState;
            this.mAtmService.notifyTaskPersisterLocked(this.task, false);
        }
        if (newIcicle != null) {
            setSavedState(newIcicle);
            this.launchCount = 0;
            updateTaskDescription(description);
        }
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(this);
            String protoLogParam1 = String.valueOf(this.mIcicle);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_STATES, -172326720, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        if (!this.stopped) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam02 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1305791032, 0, (String) null, new Object[]{protoLogParam02});
            }
            removeStopTimeout();
            this.stopped = true;
            if (isStopping) {
                setState(State.STOPPED, "activityStoppedLocked");
            }
            notifyAppStopped();
            if (this.finishing) {
                abortAndClearOptionsAnimation();
            } else if (this.deferRelaunchUntilPaused) {
                destroyImmediately("stop-config");
                this.mRootWindowContainer.resumeFocusedTasksTopActivities();
            } else {
                this.mAtmService.updatePreviousProcess(this);
            }
            this.mTaskSupervisor.checkReadyForSleepLocked(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addToStopping(boolean scheduleIdle, boolean idleDelayed, String reason) {
        if (!this.mTaskSupervisor.mStoppingActivities.contains(this)) {
            EventLogTags.writeWmAddToStopping(this.mUserId, System.identityHashCode(this), this.shortComponentName, reason);
            this.mTaskSupervisor.mStoppingActivities.add(this);
        }
        Task rootTask = getRootTask();
        boolean forceIdle = this.mTaskSupervisor.mStoppingActivities.size() > 3 || (isRootOfTask() && rootTask.getChildCount() <= 1);
        if (scheduleIdle || forceIdle) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                boolean protoLogParam0 = forceIdle;
                boolean protoLogParam1 = !idleDelayed;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 1126328412, 15, (String) null, new Object[]{Boolean.valueOf(protoLogParam0), Boolean.valueOf(protoLogParam1)});
            }
            if (!idleDelayed) {
                this.mTaskSupervisor.scheduleIdle();
                return;
            } else {
                this.mTaskSupervisor.scheduleIdleTimeout(this);
                return;
            }
        }
        if (rootTask != null) {
            boolean hasOrganizedTaskFragment = rootTask.forAllLeafTaskFragments(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda25
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return ActivityRecord.lambda$addToStopping$14((TaskFragment) obj);
                }
            });
            if (hasOrganizedTaskFragment && "com.android.settings/.biometrics.BiometricEnrollActivity".equals(this.shortComponentName)) {
                Slog.d(TAG, "ActivityRecord::addToStopping() shortComponentName=" + this.shortComponentName + ", this=" + this + ", root task hasOrganizedTaskFragment so force invisible");
                setVisibility(false);
            }
        }
        rootTask.checkReadyForSleep();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$addToStopping$14(TaskFragment fragment) {
        if (fragment.isEmbedded() && fragment.isVisible() && fragment.isOrganizedTaskFragment()) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startLaunchTickingLocked() {
        if (!Build.IS_USER && this.launchTickTime == 0) {
            this.launchTickTime = SystemClock.uptimeMillis();
            continueLaunchTicking();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean continueLaunchTicking() {
        Task rootTask;
        if (this.launchTickTime == 0 || (rootTask = getRootTask()) == null) {
            return false;
        }
        rootTask.removeLaunchTickMessages();
        this.mAtmService.mH.postDelayed(this.mLaunchTickRunnable, 500L);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeLaunchTickRunnable() {
        this.mAtmService.mH.removeCallbacks(this.mLaunchTickRunnable);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishLaunchTickingLocked() {
        this.launchTickTime = 0L;
        Task rootTask = getRootTask();
        if (rootTask == null) {
            return;
        }
        rootTask.removeLaunchTickMessages();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean mayFreezeScreenLocked() {
        return mayFreezeScreenLocked(this.app);
    }

    private boolean mayFreezeScreenLocked(WindowProcessController app) {
        return (!hasProcess() || app.isCrashing() || app.isNotResponding()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startFreezingScreenLocked(int configChanges) {
        startFreezingScreenLocked(this.app, configChanges);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startFreezingScreenLocked(WindowProcessController app, int configChanges) {
        if (mayFreezeScreenLocked(app)) {
            if (getParent() == null) {
                Slog.w(WmsExt.TAG, "Attempted to freeze screen with non-existing app token: " + this.token);
                return;
            }
            int freezableConfigChanges = (-536870913) & configChanges;
            if (freezableConfigChanges == 0 && okToDisplay()) {
                if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                    String protoLogParam0 = String.valueOf(this.token);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1836306327, 0, (String) null, new Object[]{protoLogParam0});
                    return;
                }
                return;
            }
            startFreezingScreen();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startFreezingScreen() {
        startFreezingScreen(-1);
    }

    void startFreezingScreen(int overrideOriginalDisplayRotation) {
        if (this.mTransitionController.isShellTransitionsEnabled()) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            String protoLogParam0 = String.valueOf(this.token);
            boolean protoLogParam1 = isVisible();
            boolean protoLogParam2 = this.mFreezingScreen;
            boolean protoLogParam3 = this.mVisibleRequested;
            String protoLogParam4 = String.valueOf(new RuntimeException().fillInStackTrace());
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ORIENTATION, 1746778201, (int) AudioChannelMask.IN_6, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), protoLogParam4});
        }
        if (!this.mVisibleRequested) {
            return;
        }
        boolean forceRotation = overrideOriginalDisplayRotation != -1;
        if (!this.mFreezingScreen) {
            this.mFreezingScreen = true;
            this.mWmService.registerAppFreezeListener(this);
            this.mWmService.mAppsFreezingScreen++;
            if (this.mWmService.mAppsFreezingScreen == 1) {
                if (forceRotation) {
                    this.mDisplayContent.getDisplayRotation().cancelSeamlessRotation();
                }
                this.mWmService.startFreezingDisplay(0, 0, this.mDisplayContent, overrideOriginalDisplayRotation);
                this.mWmService.mH.removeMessages(17);
                this.mWmService.mH.sendEmptyMessageDelayed(17, 2000L);
            }
        }
        if (forceRotation) {
            return;
        }
        int count = this.mChildren.size();
        for (int i = 0; i < count; i++) {
            WindowState w = (WindowState) this.mChildren.get(i);
            w.onStartFreezingScreen();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFreezingScreen() {
        return this.mFreezingScreen;
    }

    @Override // com.android.server.wm.WindowManagerService.AppFreezeListener
    public void onAppFreezeTimeout() {
        Slog.w(WmsExt.TAG, "Force clearing freeze: " + this);
        stopFreezingScreen(true, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopFreezingScreenLocked(boolean force) {
        if (force || this.frozenBeforeDestroy) {
            this.frozenBeforeDestroy = false;
            if (getParent() == null) {
                return;
            }
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                String protoLogParam0 = String.valueOf(this.token);
                boolean protoLogParam1 = isVisible();
                boolean protoLogParam2 = isFreezingScreen();
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 466506262, 60, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2)});
            }
            stopFreezingScreen(true, force);
        }
    }

    void stopFreezingScreen(boolean unfreezeSurfaceNow, boolean force) {
        if (!this.mFreezingScreen) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, 539077569, 12, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(force)});
        }
        int count = this.mChildren.size();
        boolean unfrozeWindows = false;
        for (int i = 0; i < count; i++) {
            WindowState w = (WindowState) this.mChildren.get(i);
            unfrozeWindows |= w.onStopFreezingScreen();
        }
        if (force || unfrozeWindows) {
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                String protoLogParam02 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ORIENTATION, -251259736, 0, (String) null, new Object[]{protoLogParam02});
            }
            this.mFreezingScreen = false;
            this.mWmService.unregisterAppFreezeListener(this);
            this.mWmService.mAppsFreezingScreen--;
            this.mWmService.mLastFinishedFreezeSource = this;
        }
        if (unfreezeSurfaceNow) {
            if (unfrozeWindows) {
                this.mWmService.mWindowPlacerLocked.performSurfacePlacement();
            }
            this.mWmService.stopFreezingDisplayLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportFullyDrawnLocked(boolean restoredFromBundle) {
        ActivityMetricsLogger.TransitionInfoSnapshot info = this.mTaskSupervisor.getActivityMetricsLogger().logAppTransitionReportedDrawn(this, restoredFromBundle);
        if (info != null) {
            this.mTaskSupervisor.reportActivityLaunched(false, this, info.windowsFullyDrawnDelayMs, info.getLaunchState());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onFirstWindowDrawn(WindowState win) {
        ActivityRecord r;
        this.firstWindowDrawn = true;
        this.mSplashScreenStyleSolidColor = true;
        removeDeadWindows();
        if (ITranActivityRecord.Instance().inMultiWindow(this)) {
            if (ThunderbackConfig.isVersion4()) {
                ITranActivityRecord.Instance().hookOnFirstWindowDrawnV4(this, getWindowConfiguration().getMultiWindowingMode(), getWindowConfiguration().getMultiWindowingId());
            } else if (ThunderbackConfig.isVersion3()) {
                ITranActivityRecord.Instance().hookOnFirstWindowDrawnV3(this);
            }
            if (getDisplayArea() != null) {
                getDisplayArea().mAppTransitionReady = true;
            }
        }
        if (this.mStartingWindow != null) {
            if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                String protoLogParam0 = String.valueOf(win.mToken);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 1677260366, 0, (String) null, new Object[]{protoLogParam0});
            }
            win.cancelAnimation();
        }
        Task associatedTask = this.task.mSharedStartingData != null ? this.task : null;
        if (associatedTask == null) {
            removeStartingWindow();
        } else if (associatedTask.getActivity(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda33
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ActivityRecord.lambda$onFirstWindowDrawn$15((ActivityRecord) obj);
            }
        }) == null && (r = associatedTask.topActivityContainsStartingWindow()) != null) {
            r.removeStartingWindow();
        }
        updateReportedVisibilityLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onFirstWindowDrawn$15(ActivityRecord r) {
        return r.mVisibleRequested && !r.firstWindowDrawn;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onStartingWindowDrawn() {
        boolean wasTaskVisible = false;
        Task task = this.task;
        if (task != null) {
            this.mSplashScreenStyleSolidColor = true;
            wasTaskVisible = task.getHasBeenVisible();
            this.task.setHasBeenVisible(true);
        }
        if (!wasTaskVisible && this.mStartingData != null && !this.finishing && !this.mLaunchedFromBubble && this.mVisibleRequested && !this.mDisplayContent.mAppTransition.isReady() && !this.mDisplayContent.mAppTransition.isRunning() && this.mDisplayContent.isNextTransitionForward()) {
            this.mStartingData.mIsTransitionForward = true;
            if (this != this.mDisplayContent.getLastOrientationSource()) {
                this.mDisplayContent.updateOrientation();
            }
            this.mDisplayContent.executeAppTransition();
        }
        if (ThunderbackConfig.isVersion4()) {
            ITranActivityRecord.Instance().hookOnFirstWindowDrawnV4(this, getWindowConfiguration().getMultiWindowingMode(), getWindowConfiguration().getMultiWindowingId());
        } else if (ThunderbackConfig.isVersion3()) {
            ITranActivityRecord.Instance().hookOnFirstWindowDrawnV3(this);
        }
    }

    private void onWindowsDrawn(long timestampNs) {
        ActivityMetricsLogger.TransitionInfoSnapshot info = this.mTaskSupervisor.getActivityMetricsLogger().notifyWindowsDrawn(this, timestampNs);
        boolean validInfo = info != null;
        int windowsDrawnDelayMs = validInfo ? info.windowsDrawnDelayMs : -1;
        int launchState = validInfo ? info.getLaunchState() : 0;
        if (validInfo || this == getDisplayArea().topRunningActivity()) {
            this.mTaskSupervisor.reportActivityLaunched(false, this, windowsDrawnDelayMs, launchState);
        }
        finishLaunchTickingLocked();
        Task task = this.task;
        if (task != null) {
            task.setHasBeenVisible(true);
        }
        this.mLaunchRootTask = null;
        ITranWindowManagerService.Instance().onActivityRecordWindowsDrawn(this, windowsDrawnDelayMs, launchState);
    }

    void onWindowsVisible() {
        if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(WmsExt.TAG, "Reporting visible in " + this.token);
        }
        this.mTaskSupervisor.stopWaitingForActivityVisible(this);
        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
            Log.v(TAG_SWITCH, "windowsVisibleLocked(): " + this);
        }
        if (!this.nowVisible) {
            this.nowVisible = true;
            this.lastVisibleTime = SystemClock.uptimeMillis();
            this.mAtmService.scheduleAppGcsLocked();
            this.mTaskSupervisor.scheduleProcessStoppingAndFinishingActivitiesIfNeeded();
            if (this.mImeInsetsFrozenUntilStartInput && getWindow(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda16
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean mayUseInputMethod;
                    mayUseInputMethod = WindowManager.LayoutParams.mayUseInputMethod(((WindowState) obj).mAttrs.flags);
                    return mayUseInputMethod;
                }
            }) == null) {
                this.mImeInsetsFrozenUntilStartInput = false;
            }
        }
    }

    void onWindowsGone() {
        if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(WmsExt.TAG, "Reporting gone in " + this.token);
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
            Log.v(TAG_SWITCH, "windowsGone(): " + this);
        }
        this.nowVisible = false;
    }

    @Override // com.android.server.wm.WindowContainer
    void checkAppWindowsReadyToShow() {
        boolean z = this.allDrawn;
        if (z == this.mLastAllDrawn) {
            return;
        }
        this.mLastAllDrawn = z;
        if (!z) {
            return;
        }
        if (this.mFreezingScreen) {
            showAllWindowsLocked();
            stopFreezingScreen(false, true);
            if (ProtoLogCache.WM_DEBUG_ORIENTATION_enabled) {
                String protoLogParam0 = String.valueOf(this);
                long protoLogParam1 = this.mNumInterestingWindows;
                long protoLogParam2 = this.mNumDrawnWindows;
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ORIENTATION, 806891543, 20, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2)});
            }
            setAppLayoutChanges(4, "checkAppWindowsReadyToShow: freezingScreen");
            return;
        }
        setAppLayoutChanges(8, "checkAppWindowsReadyToShow");
        if (!getDisplayContent().mOpeningApps.contains(this) && canShowWindows()) {
            showAllWindowsLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showAllWindowsLocked() {
        forAllWindows(new Consumer() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ActivityRecord.lambda$showAllWindowsLocked$17((WindowState) obj);
            }
        }, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$showAllWindowsLocked$17(WindowState windowState) {
        if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG, "performing show on: " + windowState);
        }
        windowState.performShowLocked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateReportedVisibilityLocked() {
        if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG, "Update reported visibility: " + this);
        }
        int count = this.mChildren.size();
        this.mReportedVisibilityResults.reset();
        for (int i = 0; i < count; i++) {
            WindowState win = (WindowState) this.mChildren.get(i);
            win.updateReportedVisibility(this.mReportedVisibilityResults);
        }
        int numInteresting = this.mReportedVisibilityResults.numInteresting;
        int numVisible = this.mReportedVisibilityResults.numVisible;
        int numDrawn = this.mReportedVisibilityResults.numDrawn;
        boolean nowGone = this.mReportedVisibilityResults.nowGone;
        boolean nowVisible = false;
        boolean nowDrawn = numInteresting > 0 && numDrawn >= numInteresting;
        if (numInteresting > 0 && numVisible >= numInteresting && isVisible()) {
            nowVisible = true;
        }
        if (!nowGone) {
            if (!nowDrawn) {
                nowDrawn = this.mReportedDrawn;
            }
            if (!nowVisible) {
                nowVisible = this.reportedVisible;
            }
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
            Slog.v(TAG, "VIS " + this + ": interesting=" + numInteresting + " visible=" + numVisible);
        }
        if (nowDrawn != this.mReportedDrawn) {
            if (nowDrawn) {
                onWindowsDrawn(SystemClock.elapsedRealtimeNanos());
            }
            this.mReportedDrawn = nowDrawn;
        } else if (ActivityTaskManagerService.AJUST_LAUNCH_TIME && !nowDrawn && !this.mReportedDrawn && numDrawn > 0 && isVisible()) {
            onWindowsDrawn(SystemClock.elapsedRealtimeNanos());
        }
        if (nowVisible != this.reportedVisible) {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.v(TAG, "Visibility changed in " + this + ": vis=" + nowVisible);
            }
            this.reportedVisible = nowVisible;
            if (nowVisible) {
                onWindowsVisible();
            } else {
                onWindowsGone();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isReportedDrawn() {
        return this.mReportedDrawn;
    }

    @Override // com.android.server.wm.WindowToken
    void setClientVisible(boolean clientVisible) {
        if (clientVisible || !this.mDeferHidingClient) {
            super.setClientVisible(clientVisible);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateDrawnWindowStates(WindowState w) {
        w.setDrawnStateEvaluated(true);
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE && w == this.mStartingWindow) {
            Slog.d(TAG, "updateWindows: starting " + w + " isOnScreen=" + w.isOnScreen() + " allDrawn=" + this.allDrawn + " freezingScreen=" + this.mFreezingScreen);
        }
        if (!this.allDrawn || this.mFreezingScreen) {
            if (this.mLastTransactionSequence != this.mWmService.mTransactionSequence) {
                this.mLastTransactionSequence = this.mWmService.mTransactionSequence;
                this.mNumDrawnWindows = 0;
                this.mNumInterestingWindows = findMainWindow(false) != null ? 1 : 0;
            }
            WindowStateAnimator winAnimator = w.mWinAnimator;
            if (this.allDrawn || !w.mightAffectAllDrawn()) {
                return false;
            }
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY || ProtoLogGroup.WM_DEBUG_ORIENTATION.isLogToLogcat()) {
                boolean isAnimationSet = isAnimating(3, 1);
                Slog.v(TAG, "Eval win " + w + ": isDrawn=" + w.isDrawn() + ", isAnimationSet=" + isAnimationSet);
                if (!w.isDrawn()) {
                    Slog.v(TAG, "Not displayed: s=" + winAnimator.mSurfaceController + " pv=" + w.isVisibleByPolicy() + " mDrawState=" + winAnimator.drawStateToString() + " ph=" + w.isParentWindowHidden() + " th=" + this.mVisibleRequested + " a=" + isAnimationSet);
                }
            }
            if (w != this.mStartingWindow) {
                if (!w.isInteresting()) {
                    return false;
                }
                if (findMainWindow(false) != w) {
                    this.mNumInterestingWindows++;
                }
                if (w.isDrawn()) {
                    this.mNumDrawnWindows++;
                    if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY || ProtoLogGroup.WM_DEBUG_ORIENTATION.isLogToLogcat()) {
                        Slog.v(TAG, "tokenMayBeDrawn: " + this + " w=" + w + " numInteresting=" + this.mNumInterestingWindows + " freezingScreen=" + this.mFreezingScreen + " mAppFreezing=" + w.mAppFreezing);
                    }
                    return true;
                }
                return false;
            } else if (this.mStartingData != null && w.isDrawn()) {
                this.mStartingData.mIsDisplayed = true;
                return false;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean inputDispatchingTimedOut(String reason, int windowPid) {
        ActivityRecord anrActivity;
        WindowProcessController anrApp;
        boolean blameActivityProcess;
        synchronized (this.mAtmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                anrActivity = getWaitingHistoryRecordLocked();
                anrApp = this.app;
                blameActivityProcess = hasProcess() && (this.app.getPid() == windowPid || windowPid == -1);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (blameActivityProcess) {
            return this.mAtmService.mAmInternal.inputDispatchingTimedOut(anrApp.mOwner, anrActivity.shortComponentName, anrActivity.info.applicationInfo, this.shortComponentName, this.app, false, reason);
        }
        long timeoutMillis = this.mAtmService.mAmInternal.inputDispatchingTimedOut(windowPid, false, reason);
        return timeoutMillis <= 0;
    }

    private ActivityRecord getWaitingHistoryRecordLocked() {
        if (this.stopped) {
            Task rootTask = this.mRootWindowContainer.getTopDisplayFocusedRootTask();
            if (rootTask == null) {
                return this;
            }
            ActivityRecord r = rootTask.getTopResumedActivity();
            if (r == null) {
                r = rootTask.getTopPausingActivity();
            }
            if (r != null) {
                return r;
            }
        }
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canBeTopRunning() {
        return !this.finishing && showToCurrentUser();
    }

    public boolean isInterestingToUserLocked() {
        return this.mVisibleRequested || this.nowVisible || this.mState == State.PAUSING || this.mState == State.RESUMED;
    }

    public boolean isForegroundToUserLocked() {
        return this.nowVisible || this.mState == State.RESUMED;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getTaskForActivityLocked(IBinder token, boolean onlyRoot) {
        ActivityRecord r = forTokenLocked(token);
        if (r == null || r.getParent() == null) {
            return -1;
        }
        return getTaskForActivityLocked(r, onlyRoot);
    }

    static int getTaskForActivityLocked(ActivityRecord r, boolean onlyRoot) {
        Task task = r.task;
        if (onlyRoot && r.compareTo((WindowContainer) task.getRootActivity(false, true)) > 0) {
            return -1;
        }
        return task.mTaskId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ActivityRecord isInRootTaskLocked(IBinder token) {
        ActivityRecord r = forTokenLocked(token);
        if (r != null) {
            return r.getRootTask().isInTask(r);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Task getRootTask(IBinder token) {
        ActivityRecord r = isInRootTaskLocked(token);
        if (r != null) {
            return r.getRootTask();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ActivityRecord isInAnyTask(IBinder token) {
        ActivityRecord r = forTokenLocked(token);
        if (r == null || !r.isAttached()) {
            return null;
        }
        return r;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDisplayId() {
        Task rootTask = getRootTask();
        if (rootTask == null) {
            return -1;
        }
        return rootTask.getDisplayId();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final boolean isDestroyable() {
        return (this.finishing || !hasProcess() || isState(State.RESUMED) || getRootTask() == null || this == getTaskFragment().getPausingActivity() || !this.mHaveState || !this.stopped || this.mVisibleRequested) ? false : true;
    }

    private static String createImageFilename(long createTime, int taskId) {
        return String.valueOf(taskId) + ACTIVITY_ICON_SUFFIX + createTime + ".png";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTaskDescription(ActivityManager.TaskDescription _taskDescription) {
        Bitmap icon;
        if (_taskDescription.getIconFilename() == null && (icon = _taskDescription.getIcon()) != null) {
            String iconFilename = createImageFilename(this.createTime, this.task.mTaskId);
            File iconFile = new File(TaskPersister.getUserImagesDir(this.task.mUserId), iconFilename);
            String iconFilePath = iconFile.getAbsolutePath();
            this.mAtmService.getRecentTasks().saveImage(icon, iconFilePath);
            _taskDescription.setIconFilename(iconFilePath);
        }
        this.taskDescription = _taskDescription;
        getTask().updateTaskDescription();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLocusId(LocusId locusId) {
        if (Objects.equals(locusId, this.mLocusId)) {
            return;
        }
        this.mLocusId = locusId;
        Task task = getTask();
        if (task != null) {
            getTask().dispatchTaskInfoChangedIfNeeded(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocusId getLocusId() {
        return this.mLocusId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setVoiceSessionLocked(IVoiceInteractionSession session) {
        this.voiceSession = session;
        this.pendingVoiceInteractionStart = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearVoiceSessionLocked() {
        this.voiceSession = null;
        this.pendingVoiceInteractionStart = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showStartingWindow(boolean taskSwitch) {
        showStartingWindow(null, false, taskSwitch, false, null);
    }

    private ActivityRecord searchCandidateLaunchingActivity() {
        ActivityRecord below = this.task.getActivityBelow(this);
        if (below == null) {
            below = this.task.getParent().getActivityBelow(this);
        }
        if (below == null || below.isActivityTypeHome()) {
            return null;
        }
        WindowProcessController myProcess = this.app;
        if (myProcess == null) {
            myProcess = (WindowProcessController) this.mAtmService.mProcessNames.get(this.processName, this.info.applicationInfo.uid);
        }
        WindowProcessController candidateProcess = below.app;
        if (candidateProcess == null) {
            candidateProcess = (WindowProcessController) this.mAtmService.mProcessNames.get(below.processName, below.info.applicationInfo.uid);
        }
        if (candidateProcess != myProcess && !this.mActivityComponent.getPackageName().equals(below.mActivityComponent.getPackageName())) {
            return null;
        }
        return below;
    }

    private boolean isIconStylePreferred(int theme) {
        AttributeCache.Entry ent;
        return theme != 0 && (ent = AttributeCache.instance().get(this.packageName, theme, R.styleable.Window, this.mWmService.mCurrentUserId)) != null && ent.array.hasValue(61) && ent.array.getInt(61, 0) == 1;
    }

    private boolean shouldUseSolidColorSplashScreen(ActivityRecord sourceRecord, boolean startActivity, ActivityOptions options, int resolvedTheme) {
        int i;
        if (sourceRecord == null && !startActivity) {
            ActivityRecord above = this.task.getActivityAbove(this);
            if (above != null) {
                return true;
            }
        }
        if (options != null) {
            int optionsStyle = options.getSplashScreenStyle();
            if (optionsStyle == 0) {
                return true;
            }
            if (optionsStyle == 1 || isIconStylePreferred(resolvedTheme) || (i = this.mLaunchSourceType) == 2 || this.launchedFromUid == 2000) {
                return false;
            }
            if (i == 3) {
                return true;
            }
        } else if (isIconStylePreferred(resolvedTheme)) {
            return false;
        }
        if (sourceRecord == null) {
            sourceRecord = searchCandidateLaunchingActivity();
        }
        if (sourceRecord != null && !sourceRecord.isActivityTypeHome()) {
            return sourceRecord.mSplashScreenStyleSolidColor;
        }
        if (startActivity) {
            int i2 = this.mLaunchSourceType;
            return (i2 == 1 || i2 == 2 || this.launchedFromUid == 2000) ? false : true;
        }
        return true;
    }

    private int getSplashscreenTheme(ActivityOptions options) {
        String splashScreenThemeResName;
        if (options == null) {
            splashScreenThemeResName = null;
        } else {
            splashScreenThemeResName = options.getSplashScreenThemeResName();
        }
        if (splashScreenThemeResName == null || splashScreenThemeResName.isEmpty()) {
            try {
                splashScreenThemeResName = this.mAtmService.getPackageManager().getSplashScreenTheme(this.packageName, this.mUserId);
            } catch (RemoteException e) {
            }
        }
        if (splashScreenThemeResName == null || splashScreenThemeResName.isEmpty()) {
            return 0;
        }
        try {
            Context packageContext = this.mAtmService.mContext.createPackageContext(this.packageName, 0);
            int splashScreenThemeResId = packageContext.getResources().getIdentifier(splashScreenThemeResName, null, null);
            return splashScreenThemeResId;
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e2) {
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showStartingWindow(ActivityRecord prev, boolean newTask, boolean taskSwitch, boolean startActivity, ActivityRecord sourceRecord) {
        showStartingWindow(prev, newTask, taskSwitch, isProcessRunning(), startActivity, sourceRecord, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showStartingWindow(ActivityRecord prev, boolean newTask, boolean taskSwitch, boolean processRunning, boolean startActivity, ActivityRecord sourceRecord, ActivityOptions candidateOptions) {
        ActivityOptions activityOptions;
        int i;
        if (this.mTaskOverlay) {
            return;
        }
        if (candidateOptions == null) {
            activityOptions = this.mPendingOptions;
        } else {
            activityOptions = candidateOptions;
        }
        ActivityOptions startOptions = activityOptions;
        if (startOptions != null && startOptions.getAnimationType() == 5) {
            return;
        }
        if (this.isForceDisablePreview) {
            Slog.d(TAG, "disable splash screen cause isForceDisablePreview is " + this.isForceDisablePreview);
            return;
        }
        if (startActivity) {
            i = getSplashscreenTheme(startOptions);
        } else {
            i = 0;
        }
        int splashScreenTheme = i;
        int resolvedTheme = evaluateStartingWindowTheme(prev, this.packageName, this.theme, splashScreenTheme);
        this.mSplashScreenStyleSolidColor = shouldUseSolidColorSplashScreen(sourceRecord, startActivity, startOptions, resolvedTheme);
        boolean activityCreated = this.mState.ordinal() >= State.STARTED.ordinal() && this.mState.ordinal() <= State.STOPPED.ordinal();
        boolean newSingleActivity = (newTask || activityCreated || this.task.getActivity(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ActivityRecord.this.m7786x6b979afa((ActivityRecord) obj);
            }
        }) != null) ? false : true;
        boolean scheduled = addStartingWindow(this.packageName, resolvedTheme, prev, newTask || newSingleActivity, taskSwitch, processRunning, allowTaskSnapshot(), activityCreated, this.mSplashScreenStyleSolidColor, this.allDrawn);
        if (WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE && scheduled) {
            Slog.d(TAG, "Scheduled starting window for " + this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showStartingWindow$18$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ boolean m7786x6b979afa(ActivityRecord r) {
        return (r.finishing || r == this) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelInitializing() {
        if (this.mStartingData != null) {
            if (ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY) {
                Slog.w(TAG_VISIBILITY, "Found orphaned starting window " + this);
            }
            removeStartingWindowAnimation(false);
        }
        if (!this.mDisplayContent.mUnknownAppVisibilityController.allResolved()) {
            this.mDisplayContent.mUnknownAppVisibilityController.appRemovedOrHidden(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postWindowRemoveStartingWindowCleanup(WindowState win) {
        if (this.mStartingWindow == win) {
            if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                String protoLogParam0 = String.valueOf(win);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 1853793312, 0, (String) null, new Object[]{protoLogParam0});
            }
            removeStartingWindow();
        } else if (this.mChildren.size() == 0) {
            if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, 1671994402, 0, (String) null, (Object[]) null);
            }
            this.mStartingData = null;
            if (this.mVisibleSetFromTransferredStartingWindow) {
                setVisible(false);
            }
        } else if (this.mChildren.size() == 1 && this.mStartingSurface != null && !isRelaunching()) {
            if (ProtoLogCache.WM_DEBUG_STARTING_WINDOW_enabled) {
                String protoLogParam02 = String.valueOf(win);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STARTING_WINDOW, -1715268616, 0, (String) null, new Object[]{protoLogParam02});
            }
            removeStartingWindow();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeDeadWindows() {
        for (int winNdx = this.mChildren.size() - 1; winNdx >= 0; winNdx--) {
            WindowState win = (WindowState) this.mChildren.get(winNdx);
            if (win.mAppDied) {
                if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
                    String protoLogParam0 = String.valueOf(win);
                    ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 1515161239, 0, (String) null, new Object[]{protoLogParam0});
                }
                win.mDestroying = true;
                win.removeIfPossible();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWillReplaceWindows(boolean animate) {
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -1878839956, 0, (String) null, new Object[]{protoLogParam0});
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState w = (WindowState) this.mChildren.get(i);
            w.setWillReplaceWindow(animate);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWillReplaceChildWindows() {
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -1471946192, 0, (String) null, new Object[]{protoLogParam0});
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState w = (WindowState) this.mChildren.get(i);
            w.setWillReplaceChildWindows();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearWillReplaceWindows() {
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -1698815688, 0, (String) null, new Object[]{protoLogParam0});
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState w = (WindowState) this.mChildren.get(i);
            w.clearWillReplaceWindow();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void requestUpdateWallpaperIfNeeded() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState w = (WindowState) this.mChildren.get(i);
            w.requestUpdateWallpaperIfNeeded();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getTopFullscreenOpaqueWindow() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState win = (WindowState) this.mChildren.get(i);
            if (win != null && win.mAttrs.isFullscreen() && !win.isFullyTransparent()) {
                return win;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState findMainWindow() {
        return findMainWindow(true);
    }

    public WindowState findMainWindow(boolean includeStartingApp) {
        WindowState candidate = null;
        for (int j = this.mChildren.size() - 1; j >= 0; j--) {
            WindowState win = (WindowState) this.mChildren.get(j);
            int type = win.mAttrs.type;
            if (type == 1 || (includeStartingApp && type == 3)) {
                if (win.mAnimatingExit) {
                    candidate = win;
                } else {
                    return win;
                }
            }
        }
        return candidate;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean needsZBoost() {
        return this.mNeedsZBoost || super.needsZBoost();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl getAnimationLeashParent() {
        if (inPinnedWindowingMode()) {
            return getRootTask().getSurfaceControl();
        }
        return super.getAnimationLeashParent();
    }

    boolean shouldAnimate() {
        Task task = this.task;
        return task == null || task.shouldAnimate();
    }

    private SurfaceControl createAnimationBoundsLayer(SurfaceControl.Transaction t) {
        if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_ANIM_enabled) {
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS_ANIM, 1720229827, 0, (String) null, (Object[]) null);
        }
        SurfaceControl.Builder builder = makeAnimationLeash().setParent(getAnimationLeashParent()).setName(getSurfaceControl() + " - animation-bounds").setCallsite("ActivityRecord.createAnimationBoundsLayer");
        SurfaceControl boundsLayer = builder.build();
        t.show(boundsLayer);
        return boundsLayer;
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public boolean shouldDeferAnimationFinish(Runnable endDeferFinishCallback) {
        AnimatingActivityRegistry animatingActivityRegistry = this.mAnimatingActivityRegistry;
        return animatingActivityRegistry != null && animatingActivityRegistry.notifyAboutToFinish(this, endDeferFinishCallback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isWaitingForTransitionStart() {
        DisplayContent dc = getDisplayContent();
        return dc != null && dc.mAppTransition.isTransitionSet() && (dc.mOpeningApps.contains(this) || dc.mClosingApps.contains(this) || dc.mChangingContainers.contains(this));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTransitionForward() {
        StartingData startingData = this.mStartingData;
        return (startingData != null && startingData.mIsTransitionForward) || this.mDisplayContent.isNextTransitionForward();
    }

    @Override // com.android.server.wm.WindowToken, com.android.server.wm.WindowContainer
    void resetSurfacePositionForAnimationLeash(SurfaceControl.Transaction t) {
    }

    @Override // com.android.server.wm.SurfaceAnimator.Animatable
    public void onLeashAnimationStarting(SurfaceControl.Transaction t, SurfaceControl leash) {
        AnimatingActivityRegistry animatingActivityRegistry = this.mAnimatingActivityRegistry;
        if (animatingActivityRegistry != null) {
            animatingActivityRegistry.notifyStarting(this);
        }
        if (this.mNeedsAnimationBoundsLayer) {
            this.mTmpRect.setEmpty();
            if (getDisplayContent().mAppTransitionController.isTransitWithinTask(getTransit(), this.task)) {
                this.task.getBounds(this.mTmpRect);
            } else {
                Task rootTask = getRootTask();
                if (rootTask == null) {
                    return;
                }
                rootTask.getBounds(this.mTmpRect);
            }
            this.mAnimationBoundsLayer = createAnimationBoundsLayer(t);
            t.setLayer(leash, 0);
            t.setLayer(this.mAnimationBoundsLayer, getLastLayer());
            t.reparent(leash, this.mAnimationBoundsLayer);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void prepareSurfaces() {
        boolean show = isVisible() || isAnimating(2, 9);
        if (this.mSurfaceControl != null) {
            if (show && !this.mLastSurfaceShowing) {
                getSyncTransaction().show(this.mSurfaceControl);
            } else if (!show && this.mLastSurfaceShowing) {
                getSyncTransaction().hide(this.mSurfaceControl);
            }
            this.mActivityRecordInputSink.applyChangesToSurfaceIfChanged(getSyncTransaction());
        }
        if (this.mThumbnail != null) {
            this.mThumbnail.setShowing(getPendingTransaction(), show);
        }
        this.mLastSurfaceShowing = show;
        super.prepareSurfaces();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSurfaceShowing() {
        return this.mLastSurfaceShowing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void attachThumbnailAnimation() {
        if (!isAnimating(2, 1)) {
            return;
        }
        HardwareBuffer thumbnailHeader = getDisplayContent().mAppTransition.getAppTransitionThumbnailHeader(this.task);
        if (thumbnailHeader == null) {
            if (ProtoLogCache.WM_DEBUG_APP_TRANSITIONS_enabled) {
                String protoLogParam0 = String.valueOf(this.task);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_APP_TRANSITIONS, 1528528509, 0, (String) null, new Object[]{protoLogParam0});
                return;
            }
            return;
        }
        clearThumbnail();
        SurfaceControl.Transaction transaction = getAnimatingContainer().getPendingTransaction();
        this.mThumbnail = new WindowContainerThumbnail(transaction, getAnimatingContainer(), thumbnailHeader);
        this.mThumbnail.startAnimation(transaction, loadThumbnailAnimation(thumbnailHeader));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void attachCrossProfileAppsThumbnailAnimation() {
        Drawable thumbnailDrawable;
        if (!isAnimating(2, 1)) {
            return;
        }
        clearThumbnail();
        WindowState win = findMainWindow();
        if (win == null) {
            return;
        }
        Rect frame = win.getRelativeFrame();
        if (this.task.mUserId == this.mWmService.mCurrentUserId) {
            thumbnailDrawable = this.mAtmService.mUiContext.getDrawable(17302334);
        } else {
            thumbnailDrawable = this.mEnterpriseThumbnailDrawable;
        }
        HardwareBuffer thumbnail = getDisplayContent().mAppTransition.createCrossProfileAppsThumbnail(thumbnailDrawable, frame);
        if (thumbnail == null) {
            return;
        }
        SurfaceControl.Transaction transaction = getPendingTransaction();
        this.mThumbnail = new WindowContainerThumbnail(transaction, getTask(), thumbnail);
        Animation animation = getDisplayContent().mAppTransition.createCrossProfileAppsThumbnailAnimationLocked(frame);
        this.mThumbnail.startAnimation(transaction, animation, new Point(frame.left, frame.top));
    }

    private Animation loadThumbnailAnimation(HardwareBuffer thumbnailHeader) {
        Rect appRect;
        Rect insets;
        DisplayInfo displayInfo = this.mDisplayContent.getDisplayInfo();
        WindowState win = findMainWindow();
        if (win != null) {
            insets = win.getInsetsStateWithVisibilityOverride().calculateInsets(win.getFrame(), WindowInsets.Type.systemBars(), false).toRect();
            appRect = new Rect(win.getFrame());
            appRect.inset(insets);
        } else {
            appRect = new Rect(0, 0, displayInfo.appWidth, displayInfo.appHeight);
            insets = null;
        }
        Configuration displayConfig = this.mDisplayContent.getConfiguration();
        return getDisplayContent().mAppTransition.createThumbnailAspectScaleAnimationLocked(appRect, insets, thumbnailHeader, this.task, displayConfig.orientation);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public void onAnimationLeashLost(SurfaceControl.Transaction t) {
        super.onAnimationLeashLost(t);
        if (this.mAnimationBoundsLayer != null) {
            t.remove(this.mAnimationBoundsLayer);
            this.mAnimationBoundsLayer = null;
        }
        AnimatingActivityRegistry animatingActivityRegistry = this.mAnimatingActivityRegistry;
        if (animatingActivityRegistry != null) {
            animatingActivityRegistry.notifyFinished(this);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    protected void onAnimationFinished(int type, AnimationAdapter anim) {
        WindowState transferredStarting;
        super.onAnimationFinished(type, anim);
        Trace.traceBegin(32L, "AR#onAnimationFinished");
        this.mTransit = -1;
        this.mTransitFlags = 0;
        this.mNeedsAnimationBoundsLayer = false;
        setAppLayoutChanges(12, "ActivityRecord");
        clearThumbnail();
        setClientVisible(isVisible() || this.mVisibleRequested);
        getDisplayContent().computeImeTargetIfNeeded(this);
        if (ProtoLogCache.WM_DEBUG_ANIM_enabled) {
            String protoLogParam0 = String.valueOf(this);
            boolean protoLogParam1 = this.reportedVisible;
            boolean protoLogParam2 = okToDisplay();
            boolean protoLogParam3 = okToAnimate();
            boolean protoLogParam4 = isStartingWindowDisplayed();
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ANIM, 2010476671, 1020, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), Boolean.valueOf(protoLogParam3), Boolean.valueOf(protoLogParam4)});
        }
        if (this.mThumbnail != null) {
            this.mThumbnail.destroy();
            this.mThumbnail = null;
        }
        ArrayList<WindowState> children = new ArrayList<>(this.mChildren);
        children.forEach(new Consumer() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda9
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WindowState) obj).onExitAnimationDone();
            }
        });
        Task task = this.task;
        if (task != null && this.startingMoved && (transferredStarting = task.getWindow(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda10
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ActivityRecord.lambda$onAnimationFinished$19((WindowState) obj);
            }
        })) != null && transferredStarting.mAnimatingExit && !transferredStarting.isSelfAnimating(0, 16)) {
            transferredStarting.onExitAnimationDone();
        }
        getDisplayContent().mAppTransition.notifyAppTransitionFinishedLocked(this.token);
        scheduleAnimation();
        this.mTaskSupervisor.scheduleProcessStoppingAndFinishingActivitiesIfNeeded();
        Trace.traceEnd(32L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$onAnimationFinished$19(WindowState w) {
        return w.mAttrs.type == 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearAnimatingFlags() {
        boolean wallpaperMightChange = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState win = (WindowState) this.mChildren.get(i);
            wallpaperMightChange |= win.clearAnimatingFlags();
        }
        if (wallpaperMightChange) {
            requestUpdateWallpaperIfNeeded();
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void cancelAnimation() {
        super.cancelAnimation();
        clearThumbnail();
    }

    private void clearThumbnail() {
        if (this.mThumbnail == null) {
            return;
        }
        this.mThumbnail.destroy();
        this.mThumbnail = null;
    }

    public int getTransit() {
        return this.mTransit;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerRemoteAnimations(RemoteAnimationDefinition definition) {
        this.mRemoteAnimationDefinition = definition;
        if (definition != null) {
            definition.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda0
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    ActivityRecord.this.unregisterRemoteAnimations();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterRemoteAnimations() {
        this.mRemoteAnimationDefinition = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public RemoteAnimationDefinition getRemoteAnimationDefinition() {
        return this.mRemoteAnimationDefinition;
    }

    @Override // com.android.server.wm.WindowToken
    void applyFixedRotationTransform(DisplayInfo info, DisplayFrames displayFrames, Configuration config) {
        super.applyFixedRotationTransform(info, displayFrames, config);
        ensureActivityConfiguration(0, false);
    }

    @Override // com.android.server.wm.WindowToken
    void onCancelFixedRotationTransform(int originalDisplayRotation) {
        if (this != this.mDisplayContent.getLastOrientationSource()) {
            return;
        }
        int requestedOrientation = getRequestedConfigurationOrientation();
        if (requestedOrientation != 0 && requestedOrientation != this.mDisplayContent.getConfiguration().orientation) {
            return;
        }
        this.mDisplayContent.mPinnedTaskController.onCancelFixedRotationTransform();
        startFreezingScreen(originalDisplayRotation);
        ensureActivityConfiguration(0, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRequestedOrientation(int requestedOrientation) {
        Slog.d(TAG, "the activityRecord will setRequestedOrientation, and the requestedOrientation " + requestedOrientation + ", this :" + this);
        setOrientation(requestedOrientation, this);
        if (!getMergedOverrideConfiguration().equals(this.mLastReportedConfiguration.getMergedConfiguration())) {
            ensureActivityConfiguration(0, false);
        }
        this.mAtmService.getTaskChangeNotificationController().notifyActivityRequestedOrientationChanged(this.task.mTaskId, requestedOrientation);
    }

    public void reportDescendantOrientationChangeIfNeeded() {
        if (getRequestedOrientation() != -2 && onDescendantOrientationChanged(this)) {
            this.task.dispatchTaskInfoChangedIfNeeded(true);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    int getOrientation(int candidate) {
        if (candidate == 3) {
            return this.mOrientation;
        }
        if (!getDisplayContent().mClosingApps.contains(this)) {
            if (isVisibleRequested() || getDisplayContent().mOpeningApps.contains(this)) {
                return this.mOrientation;
            }
            return -2;
        }
        return -2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRequestedOrientation() {
        return this.mOrientation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastReportedGlobalConfiguration(Configuration config) {
        this.mLastReportedConfiguration.setGlobalConfiguration(config);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastReportedConfiguration(MergedConfiguration config) {
        setLastReportedConfiguration(config.getGlobalConfiguration(), config.getOverrideConfiguration());
    }

    private void setLastReportedConfiguration(Configuration global, Configuration override) {
        this.mLastReportedConfiguration.setConfiguration(global, override);
    }

    CompatDisplayInsets getCompatDisplayInsets() {
        return this.mCompatDisplayInsets;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inSizeCompatMode() {
        WindowContainer parent;
        if (this.mInSizeCompatModeForBounds) {
            return true;
        }
        if (this.mCompatDisplayInsets == null || !shouldCreateCompatDisplayInsets() || isFixedRotationTransforming()) {
            return false;
        }
        Rect appBounds = getConfiguration().windowConfiguration.getAppBounds();
        if (appBounds == null || (parent = getParent()) == null) {
            return false;
        }
        Configuration parentConfig = parent.getConfiguration();
        return parentConfig.densityDpi != getConfiguration().densityDpi;
    }

    boolean shouldCreateCompatDisplayInsets() {
        boolean isResizeable;
        if (this.info.resizeMode != -1 || this.mForceResizeable) {
            switch (this.info.supportsSizeChanges()) {
                case 1:
                    return true;
                case 2:
                case 3:
                    return false;
                default:
                    if (inMultiWindowMode() || getWindowConfiguration().hasWindowDecorCaption()) {
                        Task task = this.task;
                        ActivityRecord root = task != null ? task.getRootActivity() : null;
                        if (root != null && root != this && !root.shouldCreateCompatDisplayInsets()) {
                            return false;
                        }
                    }
                    if (this.mForceResizeable) {
                        return false;
                    }
                    Task task2 = this.task;
                    if (task2 != null) {
                        isResizeable = task2.isResizeable() || isResizeable();
                    } else {
                        isResizeable = isResizeable();
                    }
                    return !isResizeable && (this.info.isFixedOrientation() || hasFixedAspectRatio() || (TranFoldingScreenManager.isFoldableDevice() && this.info.applicationInfo.isActivitiesUnResizeMode())) && isActivityTypeStandardOrUndefined();
            }
        }
        return true;
    }

    @Override // com.android.server.wm.WindowToken
    boolean hasSizeCompatBounds() {
        return this.mSizeCompatBounds != null;
    }

    private void updateCompatDisplayInsets() {
        if (this.mCompatDisplayInsets != null || !shouldCreateCompatDisplayInsets()) {
            return;
        }
        Configuration overrideConfig = getRequestedOverrideConfiguration();
        Configuration fullConfig = getConfiguration();
        overrideConfig.colorMode = fullConfig.colorMode;
        overrideConfig.densityDpi = fullConfig.densityDpi;
        overrideConfig.smallestScreenWidthDp = fullConfig.smallestScreenWidthDp;
        if (this.info.isFixedOrientation()) {
            overrideConfig.windowConfiguration.setRotation(fullConfig.windowConfiguration.getRotation());
        }
        this.mCompatDisplayInsets = new CompatDisplayInsets(this.mDisplayContent, this, this.mLetterboxBoundsForFixedOrientationAndAspectRatio);
    }

    void clearSizeCompatMode() {
        float lastSizeCompatScale = this.mSizeCompatScale;
        this.mInSizeCompatModeForBounds = false;
        this.mSizeCompatScale = 1.0f;
        this.mSizeCompatBounds = null;
        this.mCompatDisplayInsets = null;
        if (1.0f != lastSizeCompatScale) {
            forAllWindows((ToBooleanFunction<WindowState>) new ActivityRecord$$ExternalSyntheticLambda20(), false);
        }
        onRequestedOverrideConfigurationChanged(Configuration.EMPTY);
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public boolean matchParentBounds() {
        WindowContainer parent;
        Rect overrideBounds = getResolvedOverrideBounds();
        return overrideBounds.isEmpty() || (parent = getParent()) == null || parent.getBounds().equals(overrideBounds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowToken
    public float getSizeCompatScale() {
        return hasSizeCompatBounds() ? this.mSizeCompatScale : super.getSizeCompatScale();
    }

    @Override // com.android.server.wm.WindowToken, com.android.server.wm.ConfigurationContainer
    void resolveOverrideConfiguration(Configuration newParentConfiguration) {
        Configuration newParentConfiguration2;
        boolean isResizeable;
        int resizable;
        int resizable2;
        Configuration requestedOverrideConfig = getRequestedOverrideConfiguration();
        if (requestedOverrideConfig.assetsSeq != 0 && newParentConfiguration.assetsSeq > requestedOverrideConfig.assetsSeq) {
            requestedOverrideConfig.assetsSeq = 0;
        }
        super.resolveOverrideConfiguration(newParentConfiguration);
        Configuration resolvedConfig = getResolvedOverrideConfiguration();
        if (!isFixedRotationTransforming()) {
            newParentConfiguration2 = newParentConfiguration;
        } else {
            this.mTmpConfig.setTo(newParentConfiguration);
            this.mTmpConfig.updateFrom(resolvedConfig);
            newParentConfiguration2 = this.mTmpConfig;
        }
        this.mIsAspectRatioApplied = false;
        int parentWindowingMode = newParentConfiguration2.windowConfiguration.getWindowingMode();
        boolean isFixedOrientationLetterboxAllowed = parentWindowingMode == 6 || parentWindowingMode == 1;
        if (isFixedOrientationLetterboxAllowed) {
            resolveFixedOrientationConfiguration(newParentConfiguration2, parentWindowingMode);
        }
        if (this.mCompatDisplayInsets != null) {
            resolveSizeCompatModeConfiguration(newParentConfiguration2);
        } else if (inMultiWindowMode() && !isFixedOrientationLetterboxAllowed) {
            resolvedConfig.orientation = 0;
            if (!matchParentBounds()) {
                getTaskFragment().computeConfigResourceOverrides(resolvedConfig, newParentConfiguration2);
            }
        } else if (!isLetterboxedForFixedOrientationAndAspectRatio()) {
            resolveAspectRatioRestriction(newParentConfiguration2);
        }
        if ((isFixedOrientationLetterboxAllowed || this.mCompatDisplayInsets != null || !inMultiWindowMode()) && !newParentConfiguration2.windowConfiguration.isThunderbackWindow()) {
            updateResolvedBoundsHorizontalPosition(newParentConfiguration2);
        }
        if (this.mVisibleRequested) {
            updateCompatDisplayInsets();
        }
        int i = this.mConfigurationSeq + 1;
        this.mConfigurationSeq = i;
        this.mConfigurationSeq = Math.max(i, 1);
        getResolvedOverrideConfiguration().seq = this.mConfigurationSeq;
        if (providesMaxBounds()) {
            this.mTmpBounds.set(resolvedConfig.windowConfiguration.getBounds());
            if (this.mTmpBounds.isEmpty()) {
                this.mTmpBounds.set(newParentConfiguration2.windowConfiguration.getBounds());
            }
            if (WindowManagerDebugConfig.DEBUG_CONFIGURATION && ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                String protoLogParam0 = String.valueOf(getUid());
                String protoLogParam1 = String.valueOf(this.mTmpBounds);
                String protoLogParam2 = String.valueOf(this.info.neverSandboxDisplayApis(sConstrainDisplayApisConfig));
                String protoLogParam3 = String.valueOf(this.info.alwaysSandboxDisplayApis(sConstrainDisplayApisConfig));
                String protoLogParam4 = String.valueOf(!matchParentBounds());
                String protoLogParam5 = String.valueOf(this.mCompatDisplayInsets != null);
                String protoLogParam6 = String.valueOf(shouldCreateCompatDisplayInsets());
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -108977760, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2, protoLogParam3, protoLogParam4, protoLogParam5, protoLogParam6});
            }
            resolvedConfig.windowConfiguration.setMaxBounds(this.mTmpBounds);
        }
        Bundle metaData = null;
        int showCaption = 1;
        try {
            ActivityInfo activityInfo = this.mAtmService.mContext.getPackageManager().getActivityInfo(this.mActivityComponent, 128);
            metaData = activityInfo.metaData;
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (metaData != null) {
            showCaption = metaData.getInt("com.transsion.multiwindow.showcaption", -1);
        }
        if (getTask() != null && getResolvedOverrideConfiguration() != null) {
            ActivityManager.RunningTaskInfo taskInfo = getTask().getTaskInfo();
            if (taskInfo != null) {
                isResizeable = taskInfo.supportsSplitScreenMultiWindow;
            } else {
                isResizeable = getTask().isResizeable();
            }
            if (isResizeable) {
                resizable = 0 | 2;
            } else {
                resizable = 0 & (-3);
            }
            if (showCaption == 0) {
                resizable2 = resizable & (-5);
            } else {
                resizable2 = resizable | 4;
            }
            getResolvedOverrideConfiguration().windowConfiguration.setWindowResizable(resizable2);
        }
        logAppCompatState();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean areBoundsLetterboxed() {
        return getAppCompatState(true) != 2;
    }

    private void logAppCompatState() {
        this.mTaskSupervisor.getActivityMetricsLogger().logAppCompatState(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAppCompatState() {
        return getAppCompatState(false);
    }

    private int getAppCompatState(boolean ignoreVisibility) {
        if (!ignoreVisibility && !this.mVisibleRequested) {
            return 1;
        }
        if (this.mInSizeCompatModeForBounds) {
            return 3;
        }
        if (isLetterboxedForFixedOrientationAndAspectRatio()) {
            return 4;
        }
        if (this.mIsAspectRatioApplied) {
            return 5;
        }
        return 2;
    }

    private void updateResolvedBoundsHorizontalPosition(Configuration newParentConfiguration) {
        int offsetX;
        int offsetY;
        Configuration resolvedConfig = getResolvedOverrideConfiguration();
        Rect resolvedBounds = resolvedConfig.windowConfiguration.getBounds();
        Rect screenResolvedBounds = this.mSizeCompatBounds;
        if (screenResolvedBounds == null) {
            screenResolvedBounds = resolvedBounds;
        }
        Rect parentAppBounds = newParentConfiguration.windowConfiguration.getAppBounds();
        Rect parentBounds = newParentConfiguration.windowConfiguration.getBounds();
        boolean needOffsetY = false;
        if (resolvedBounds.isEmpty() || parentBounds.width() == screenResolvedBounds.width()) {
            if (TranFoldingScreenManager.isFoldableDevice() && !resolvedBounds.isEmpty() && parentBounds.height() != screenResolvedBounds.height()) {
                needOffsetY = true;
            } else {
                return;
            }
        }
        if (screenResolvedBounds.width() >= parentAppBounds.width()) {
            offsetX = getHorizontalCenterOffset(parentAppBounds.width(), screenResolvedBounds.width());
        } else {
            float positionMultiplier = this.mLetterboxUiController.getHorizontalPositionMultiplier(newParentConfiguration);
            offsetX = (int) Math.ceil((parentAppBounds.width() - screenResolvedBounds.width()) * positionMultiplier);
        }
        Rect rect = this.mSizeCompatBounds;
        if (rect != null) {
            rect.offset(offsetX, 0);
            int dx = this.mSizeCompatBounds.left - resolvedBounds.left;
            offsetBounds(resolvedConfig, dx, 0);
        } else {
            offsetBounds(resolvedConfig, offsetX, 0);
        }
        if (needOffsetY) {
            if (screenResolvedBounds.height() >= parentAppBounds.height()) {
                offsetY = getHorizontalCenterOffset(parentAppBounds.height(), screenResolvedBounds.height());
            } else {
                float positionMultiplier2 = this.mLetterboxUiController.getHorizontalPositionMultiplier(newParentConfiguration);
                offsetY = (int) Math.ceil((parentAppBounds.height() - screenResolvedBounds.height()) * positionMultiplier2);
            }
            int offsetY2 = offsetY - (screenResolvedBounds.top - parentAppBounds.top);
            Rect rect2 = this.mSizeCompatBounds;
            if (rect2 != null) {
                rect2.offset(0, offsetY2);
                int dy = this.mSizeCompatBounds.top - resolvedBounds.top;
                offsetBounds(resolvedConfig, 0, dy);
            } else {
                offsetBounds(resolvedConfig, 0, offsetY2);
            }
        }
        getTaskFragment().computeConfigResourceOverrides(resolvedConfig, newParentConfiguration);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void recomputeConfiguration() {
        onRequestedOverrideConfigurationChanged(getRequestedOverrideConfiguration());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInTransition() {
        return this.mTransitionController.inTransition() || isAnimating(3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLetterboxedForFixedOrientationAndAspectRatio() {
        return this.mLetterboxBoundsForFixedOrientationAndAspectRatio != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEligibleForLetterboxEducation() {
        return this.mWmService.mLetterboxConfiguration.getIsEducationEnabled() && this.mIsEligibleForFixedOrientationLetterbox && getWindowingMode() == 1 && getRequestedConfigurationOrientation() == 1 && this.mStartingWindow == null;
    }

    private boolean orientationRespectedWithInsets(Rect parentBounds, Rect outStableBounds) {
        int requestedOrientation;
        outStableBounds.setEmpty();
        boolean orientationRespectedWithInsets = true;
        if (this.mDisplayContent == null || (requestedOrientation = getRequestedConfigurationOrientation()) == 0) {
            return true;
        }
        int orientation = parentBounds.height() >= parentBounds.width() ? 1 : 2;
        Task task = getTask();
        task.calculateInsetFrames(this.mTmpOutNonDecorBounds, outStableBounds, parentBounds, this.mDisplayContent.getDisplayInfo());
        int orientationWithInsets = outStableBounds.height() >= outStableBounds.width() ? 1 : 2;
        if (orientation != orientationWithInsets && orientationWithInsets != requestedOrientation) {
            orientationRespectedWithInsets = false;
        }
        if (orientationRespectedWithInsets) {
            outStableBounds.setEmpty();
        }
        return orientationRespectedWithInsets;
    }

    private void resolveFixedOrientationConfiguration(Configuration newParentConfig, int windowingMode) {
        boolean z;
        int forcedOrientation;
        this.mLetterboxBoundsForFixedOrientationAndAspectRatio = null;
        this.mIsEligibleForFixedOrientationLetterbox = false;
        Rect parentBounds = newParentConfig.windowConfiguration.getBounds();
        Rect stableBounds = new Rect();
        boolean orientationRespectedWithInsets = orientationRespectedWithInsets(parentBounds, stableBounds);
        if (handlesOrientationChangeFromDescendant() && orientationRespectedWithInsets) {
            return;
        }
        TaskFragment organizedTf = getOrganizedTaskFragment();
        if ((organizedTf != null && !organizedTf.fillsParent()) || windowingMode == 2 || ITranActivityRecord.Instance().inMultiWindow(this)) {
            return;
        }
        Rect resolvedBounds = getResolvedOverrideConfiguration().windowConfiguration.getBounds();
        int parentOrientation = newParentConfig.orientation;
        int forcedOrientation2 = getRequestedConfigurationOrientation();
        if (forcedOrientation2 == 0 || forcedOrientation2 == parentOrientation) {
            z = false;
        } else {
            z = true;
        }
        this.mIsEligibleForFixedOrientationLetterbox = z;
        if (!z && (forcedOrientation2 == 0 || orientationRespectedWithInsets)) {
            return;
        }
        CompatDisplayInsets compatDisplayInsets = this.mCompatDisplayInsets;
        if (compatDisplayInsets != null && !compatDisplayInsets.mIsInFixedOrientationLetterbox) {
            return;
        }
        Rect parentBoundsWithInsets = orientationRespectedWithInsets ? newParentConfig.windowConfiguration.getAppBounds() : stableBounds;
        Rect containingBounds = new Rect();
        Rect containingBoundsWithInsets = new Rect();
        if (forcedOrientation2 == 2) {
            int bottom = Math.min((parentBoundsWithInsets.top + parentBounds.width()) - 1, parentBoundsWithInsets.bottom);
            forcedOrientation = forcedOrientation2;
            containingBounds.set(parentBounds.left, parentBoundsWithInsets.top, parentBounds.right, bottom);
            containingBoundsWithInsets.set(parentBoundsWithInsets.left, parentBoundsWithInsets.top, parentBoundsWithInsets.right, bottom);
        } else {
            forcedOrientation = forcedOrientation2;
            int right = Math.min(parentBoundsWithInsets.left + parentBounds.height(), parentBoundsWithInsets.right);
            containingBounds.set(parentBoundsWithInsets.left, parentBounds.top, right, parentBounds.bottom);
            containingBoundsWithInsets.set(parentBoundsWithInsets.left, parentBoundsWithInsets.top, right, parentBoundsWithInsets.bottom);
        }
        Rect prevResolvedBounds = new Rect(resolvedBounds);
        resolvedBounds.set(containingBounds);
        float letterboxAspectRatioOverride = this.mLetterboxUiController.getFixedOrientationLetterboxAspectRatio(newParentConfig);
        float desiredAspectRatio = letterboxAspectRatioOverride > 1.0f ? letterboxAspectRatioOverride : computeAspectRatio(parentBounds);
        int forcedOrientation3 = forcedOrientation;
        this.mIsAspectRatioApplied = applyAspectRatio(resolvedBounds, containingBoundsWithInsets, containingBounds, desiredAspectRatio, true);
        if (forcedOrientation3 == 2) {
            int offsetY = parentBoundsWithInsets.centerY() - resolvedBounds.centerY();
            resolvedBounds.offset(0, offsetY);
        }
        CompatDisplayInsets compatDisplayInsets2 = this.mCompatDisplayInsets;
        if (compatDisplayInsets2 != null) {
            compatDisplayInsets2.getBoundsByRotation(this.mTmpBounds, newParentConfig.windowConfiguration.getRotation());
            if (resolvedBounds.width() != this.mTmpBounds.width() || resolvedBounds.height() != this.mTmpBounds.height()) {
                resolvedBounds.set(prevResolvedBounds);
                return;
            }
        }
        getTaskFragment().computeConfigResourceOverrides(getResolvedOverrideConfiguration(), newParentConfig);
        this.mLetterboxBoundsForFixedOrientationAndAspectRatio = new Rect(resolvedBounds);
    }

    private void resolveAspectRatioRestriction(Configuration newParentConfiguration) {
        Configuration resolvedConfig = getResolvedOverrideConfiguration();
        Rect parentAppBounds = newParentConfiguration.windowConfiguration.getAppBounds();
        Rect parentBounds = newParentConfiguration.windowConfiguration.getBounds();
        Rect resolvedBounds = resolvedConfig.windowConfiguration.getBounds();
        this.mTmpBounds.setEmpty();
        if ((newParentConfiguration.windowConfiguration.isThunderbackWindow() || newParentConfiguration.windowConfiguration.getWindowingMode() == 6) && this.mMinAspectRatioForUser != -1.0f) {
            this.mIsAspectRatioApplied = false;
        } else {
            this.mIsAspectRatioApplied = applyAspectRatio(this.mTmpBounds, parentAppBounds, parentBounds);
        }
        if (!this.mTmpBounds.isEmpty()) {
            resolvedBounds.set(this.mTmpBounds);
        }
        if (!resolvedBounds.isEmpty() && !resolvedBounds.equals(parentBounds)) {
            getTaskFragment().computeConfigResourceOverrides(resolvedConfig, newParentConfiguration, getFixedRotationTransformDisplayInfo());
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:42:0x00be  */
    /* JADX WARN: Removed duplicated region for block: B:45:0x00e4  */
    /* JADX WARN: Removed duplicated region for block: B:53:0x012d  */
    /* JADX WARN: Removed duplicated region for block: B:54:0x0130  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x0147  */
    /* JADX WARN: Removed duplicated region for block: B:66:0x016f  */
    /* JADX WARN: Removed duplicated region for block: B:69:0x017e  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x0181  */
    /* JADX WARN: Removed duplicated region for block: B:73:0x0189  */
    /* JADX WARN: Removed duplicated region for block: B:74:0x0198  */
    /* JADX WARN: Removed duplicated region for block: B:82:0x01b6  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void resolveSizeCompatModeConfiguration(Configuration newParentConfiguration) {
        Rect containerBounds;
        Rect containerAppBounds;
        int orientation;
        int rotation;
        float lastSizeCompatScale;
        int screenPosX;
        int height;
        int screenPosY;
        Rect rect;
        Configuration resolvedConfig = getResolvedOverrideConfiguration();
        Rect resolvedBounds = resolvedConfig.windowConfiguration.getBounds();
        if (isLetterboxedForFixedOrientationAndAspectRatio()) {
            containerBounds = new Rect(resolvedBounds);
        } else {
            containerBounds = newParentConfiguration.windowConfiguration.getBounds();
        }
        if (isLetterboxedForFixedOrientationAndAspectRatio()) {
            containerAppBounds = new Rect(getResolvedOverrideConfiguration().windowConfiguration.getAppBounds());
        } else {
            containerAppBounds = newParentConfiguration.windowConfiguration.getAppBounds();
        }
        int requestedOrientation = getRequestedConfigurationOrientation();
        if (newParentConfiguration.orientation != requestedOrientation && this.mVisible) {
            requestedOrientation = newParentConfiguration.orientation;
        }
        boolean orientationRequested = requestedOrientation != 0;
        if (orientationRequested) {
            orientation = requestedOrientation;
        } else if (this.mCompatDisplayInsets.mOriginalRequestedOrientation != 0) {
            orientation = this.mCompatDisplayInsets.mOriginalRequestedOrientation;
        } else {
            orientation = newParentConfiguration.orientation;
        }
        int rotation2 = newParentConfiguration.windowConfiguration.getRotation();
        boolean isFixedToUserRotation = this.mDisplayContent == null || this.mDisplayContent.getDisplayRotation().isFixedToUserRotation();
        if (!isFixedToUserRotation && !this.mCompatDisplayInsets.mIsFloating) {
            resolvedConfig.windowConfiguration.setRotation(rotation2);
        } else {
            int overrideRotation = resolvedConfig.windowConfiguration.getRotation();
            if (overrideRotation != -1) {
                rotation = overrideRotation;
                Rect containingAppBounds = new Rect();
                Rect containingBounds = this.mTmpBounds;
                this.mCompatDisplayInsets.getContainerBounds(containingAppBounds, containingBounds, rotation, orientation, orientationRequested, isFixedToUserRotation);
                resolvedBounds.set(containingBounds);
                if (!this.mCompatDisplayInsets.mIsFloating) {
                    this.mIsAspectRatioApplied = applyAspectRatio(resolvedBounds, containingAppBounds, containingBounds);
                }
                getTaskFragment().computeConfigResourceOverrides(resolvedConfig, newParentConfiguration, this.mCompatDisplayInsets);
                resolvedConfig.screenLayout = TaskFragment.computeScreenLayoutOverride(getConfiguration().screenLayout, resolvedConfig.screenWidthDp, resolvedConfig.screenHeightDp);
                if (resolvedConfig.screenWidthDp == resolvedConfig.screenHeightDp) {
                    resolvedConfig.orientation = newParentConfiguration.orientation;
                }
                Rect resolvedAppBounds = resolvedConfig.windowConfiguration.getAppBounds();
                int contentW = resolvedAppBounds.width();
                int contentH = resolvedAppBounds.height();
                int viewportW = containerAppBounds.width();
                int viewportH = containerAppBounds.height();
                lastSizeCompatScale = this.mSizeCompatScale;
                this.mSizeCompatScale = (contentW <= viewportW || contentH > viewportH) ? Math.min(viewportW / contentW, viewportH / contentH) : 1.0f;
                int containerTopInset = containerAppBounds.top - containerBounds.top;
                boolean topNotAligned = containerTopInset == resolvedAppBounds.top - resolvedBounds.top;
                if (this.mSizeCompatScale != 1.0f && !topNotAligned) {
                    this.mSizeCompatBounds = null;
                } else {
                    if (this.mSizeCompatBounds == null) {
                        this.mSizeCompatBounds = new Rect();
                    }
                    this.mSizeCompatBounds.set(resolvedAppBounds);
                    this.mSizeCompatBounds.offsetTo(0, 0);
                    this.mSizeCompatBounds.scale(this.mSizeCompatScale);
                    this.mSizeCompatBounds.bottom += containerTopInset;
                }
                if (this.mSizeCompatScale != lastSizeCompatScale) {
                    forAllWindows(new ActivityRecord$$ExternalSyntheticLambda20(), false);
                }
                boolean fillContainer = resolvedBounds.equals(containingBounds);
                screenPosX = !fillContainer ? containerBounds.left : containerAppBounds.left;
                if (this.mSizeCompatBounds == null) {
                    height = (containerBounds.height() - this.mSizeCompatBounds.height()) / 2;
                } else {
                    height = (containerBounds.height() - resolvedBounds.height()) / 2;
                }
                screenPosY = height + containerBounds.top;
                if (screenPosX == 0 || screenPosY != 0) {
                    rect = this.mSizeCompatBounds;
                    if (rect != null) {
                        rect.offset(screenPosX, screenPosY);
                    }
                    int dx = screenPosX - resolvedBounds.left;
                    int dy = screenPosY - resolvedBounds.top;
                    offsetBounds(resolvedConfig, dx, dy);
                }
                this.mInSizeCompatModeForBounds = isInSizeCompatModeForBounds(resolvedAppBounds, containerAppBounds);
            }
        }
        rotation = rotation2;
        Rect containingAppBounds2 = new Rect();
        Rect containingBounds2 = this.mTmpBounds;
        this.mCompatDisplayInsets.getContainerBounds(containingAppBounds2, containingBounds2, rotation, orientation, orientationRequested, isFixedToUserRotation);
        resolvedBounds.set(containingBounds2);
        if (!this.mCompatDisplayInsets.mIsFloating) {
        }
        getTaskFragment().computeConfigResourceOverrides(resolvedConfig, newParentConfiguration, this.mCompatDisplayInsets);
        resolvedConfig.screenLayout = TaskFragment.computeScreenLayoutOverride(getConfiguration().screenLayout, resolvedConfig.screenWidthDp, resolvedConfig.screenHeightDp);
        if (resolvedConfig.screenWidthDp == resolvedConfig.screenHeightDp) {
        }
        Rect resolvedAppBounds2 = resolvedConfig.windowConfiguration.getAppBounds();
        int contentW2 = resolvedAppBounds2.width();
        int contentH2 = resolvedAppBounds2.height();
        int viewportW2 = containerAppBounds.width();
        int viewportH2 = containerAppBounds.height();
        lastSizeCompatScale = this.mSizeCompatScale;
        this.mSizeCompatScale = (contentW2 <= viewportW2 || contentH2 > viewportH2) ? Math.min(viewportW2 / contentW2, viewportH2 / contentH2) : 1.0f;
        int containerTopInset2 = containerAppBounds.top - containerBounds.top;
        boolean topNotAligned2 = containerTopInset2 == resolvedAppBounds2.top - resolvedBounds.top;
        if (this.mSizeCompatScale != 1.0f) {
        }
        if (this.mSizeCompatBounds == null) {
        }
        this.mSizeCompatBounds.set(resolvedAppBounds2);
        this.mSizeCompatBounds.offsetTo(0, 0);
        this.mSizeCompatBounds.scale(this.mSizeCompatScale);
        this.mSizeCompatBounds.bottom += containerTopInset2;
        if (this.mSizeCompatScale != lastSizeCompatScale) {
        }
        boolean fillContainer2 = resolvedBounds.equals(containingBounds2);
        if (!fillContainer2) {
        }
        if (this.mSizeCompatBounds == null) {
        }
        screenPosY = height + containerBounds.top;
        if (screenPosX == 0) {
        }
        rect = this.mSizeCompatBounds;
        if (rect != null) {
        }
        int dx2 = screenPosX - resolvedBounds.left;
        int dy2 = screenPosY - resolvedBounds.top;
        offsetBounds(resolvedConfig, dx2, dy2);
        this.mInSizeCompatModeForBounds = isInSizeCompatModeForBounds(resolvedAppBounds2, containerAppBounds);
    }

    private boolean isInSizeCompatModeForBounds(Rect appBounds, Rect containerBounds) {
        int appWidth = appBounds.width();
        int appHeight = appBounds.height();
        int containerAppWidth = containerBounds.width();
        int containerAppHeight = containerBounds.height();
        if (containerAppWidth == appWidth && containerAppHeight == appHeight) {
            return false;
        }
        if ((containerAppWidth > appWidth && containerAppHeight > appHeight) || containerAppWidth < appWidth || containerAppHeight < appHeight) {
            return true;
        }
        if (this.info.getMaxAspectRatio() > 0.0f) {
            float aspectRatio = (Math.max(appWidth, appHeight) + 0.5f) / Math.min(appWidth, appHeight);
            if (aspectRatio >= this.info.getMaxAspectRatio()) {
                return false;
            }
        }
        float minAspectRatio = getMinAspectRatio();
        if (minAspectRatio > 0.0f) {
            float containerAspectRatio = (Math.max(containerAppWidth, containerAppHeight) + 0.5f) / Math.min(containerAppWidth, containerAppHeight);
            if (containerAspectRatio <= minAspectRatio) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getHorizontalCenterOffset(int viewportW, int contentW) {
        return (int) (((viewportW - contentW) + 1) * 0.5f);
    }

    private static void offsetBounds(Configuration inOutConfig, int offsetX, int offsetY) {
        inOutConfig.windowConfiguration.getBounds().offset(offsetX, offsetY);
        inOutConfig.windowConfiguration.getAppBounds().offset(offsetX, offsetY);
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public Rect getBounds() {
        Rect rect = this.mSizeCompatBounds;
        if (rect != null) {
            return rect;
        }
        return super.getBounds();
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public boolean providesMaxBounds() {
        if (getUid() == 1000) {
            return false;
        }
        if ((this.mDisplayContent == null || this.mDisplayContent.sandboxDisplayApis()) && !this.info.neverSandboxDisplayApis(sConstrainDisplayApisConfig)) {
            return this.info.alwaysSandboxDisplayApis(sConstrainDisplayApisConfig) || this.mCompatDisplayInsets != null || shouldCreateCompatDisplayInsets();
        }
        return false;
    }

    @Override // com.android.server.wm.WindowContainer
    Rect getAnimationBounds(int appRootTaskClipMode) {
        TaskFragment taskFragment = getTaskFragment();
        return taskFragment != null ? taskFragment.getBounds() : getBounds();
    }

    @Override // com.android.server.wm.WindowContainer
    void getAnimationPosition(Point outPosition) {
        outPosition.set(0, 0);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void onConfigurationChanged(Configuration newParentConfig) {
        int rotation;
        int projectedWindowingMode;
        boolean currentThunderbackWindowMode = newParentConfig.windowConfiguration.isThunderbackWindow();
        boolean prevThunderbackWindowMode = getConfiguration().windowConfiguration.isThunderbackWindow();
        boolean hasNonOrienSizeChanged = true;
        if (currentThunderbackWindowMode != prevThunderbackWindowMode) {
            this.mCompatDisplayInsets = null;
            this.mSizeCompatBounds = null;
            this.mInSizeCompatModeForBounds = false;
            this.mForceResizeable = true;
            if (currentThunderbackWindowMode) {
                int multiWindowId = -999;
                if (ThunderbackConfig.isVersion4()) {
                    multiWindowId = newParentConfig.windowConfiguration.getMultiWindowingId();
                }
                final int winId = multiWindowId;
                ITranMultiWindow.Instance().postExecute(new Runnable() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda22
                    @Override // java.lang.Runnable
                    public final void run() {
                        ActivityRecord.this.m7783x57e59773(winId);
                    }
                }, -1L);
            }
            if (prevThunderbackWindowMode) {
                int multiWindowId2 = -999;
                if (ThunderbackConfig.isVersion4()) {
                    multiWindowId2 = newParentConfig.windowConfiguration.getMultiWindowingId();
                }
                final int winId2 = multiWindowId2;
                ITranMultiWindow.Instance().postExecute(new Runnable() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda23
                    @Override // java.lang.Runnable
                    public final void run() {
                        ActivityRecord.this.m7784xb9383412(winId2);
                    }
                }, 1000L);
            }
        }
        if (this.mTransitionController.isShellTransitionsEnabled() && isVisible() && isVisibleRequested()) {
            if (getRequestedOverrideWindowingMode() == 0) {
                projectedWindowingMode = newParentConfig.windowConfiguration.getWindowingMode();
            } else {
                projectedWindowingMode = getRequestedOverrideWindowingMode();
            }
            if (getWindowingMode() != projectedWindowingMode) {
                this.mTransitionController.collect(this);
            }
        }
        if (this.mCompatDisplayInsets != null) {
            Configuration overrideConfig = getRequestedOverrideConfiguration();
            boolean wasFixedOrient = overrideConfig.windowConfiguration.getRotation() != -1;
            int requestedOrient = getRequestedConfigurationOrientation();
            if (requestedOrient != 0 && requestedOrient != getConfiguration().orientation && requestedOrient == getParent().getConfiguration().orientation && overrideConfig.windowConfiguration.getRotation() != getParent().getWindowConfiguration().getRotation()) {
                overrideConfig.windowConfiguration.setRotation(getParent().getWindowConfiguration().getRotation());
                onRequestedOverrideConfigurationChanged(overrideConfig);
                return;
            } else if (wasFixedOrient && requestedOrient == 0 && overrideConfig.windowConfiguration.getRotation() != -1) {
                overrideConfig.windowConfiguration.setRotation(-1);
                onRequestedOverrideConfigurationChanged(overrideConfig);
                return;
            }
        }
        boolean wasInPictureInPicture = inPinnedWindowingMode();
        DisplayContent display = this.mDisplayContent;
        if (wasInPictureInPicture && attachedToProcess() && display != null) {
            try {
                this.app.pauseConfigurationDispatch();
                super.onConfigurationChanged(newParentConfig);
                if (this.mVisibleRequested && !inMultiWindowMode() && (rotation = display.rotationForActivityInDifferentOrientation(this)) != -1) {
                    this.app.resumeConfigurationDispatch();
                    display.setFixedRotationLaunchingApp(this, rotation);
                }
            } finally {
                if (this.app.resumeConfigurationDispatch()) {
                    WindowProcessController windowProcessController = this.app;
                    windowProcessController.dispatchConfiguration(windowProcessController.getConfiguration());
                }
            }
        } else {
            super.onConfigurationChanged(newParentConfig);
        }
        if (getMergedOverrideConfiguration().seq != getResolvedOverrideConfiguration().seq) {
            onMergedOverrideConfigurationChanged();
        }
        if (!wasInPictureInPicture && inPinnedWindowingMode() && this.task != null) {
            this.mWaitForEnteringPinnedMode = false;
            cancelDeferShowForPinnedMode();
            ActivityTaskSupervisor activityTaskSupervisor = this.mTaskSupervisor;
            Task task = this.task;
            activityTaskSupervisor.scheduleUpdatePictureInPictureModeIfNeeded(task, task.getBounds());
        }
        if (display == null) {
            return;
        }
        if (this.mVisibleRequested) {
            display.handleActivitySizeCompatModeIfNeeded(this);
        } else if (this.mCompatDisplayInsets != null && !this.visibleIgnoringKeyguard) {
            WindowProcessController windowProcessController2 = this.app;
            if (windowProcessController2 == null || !windowProcessController2.hasVisibleActivities()) {
                int displayChanges = display.getCurrentOverrideConfigurationChanges();
                if (!hasResizeChange(displayChanges) || (displayChanges & 536872064) == 536872064) {
                    hasNonOrienSizeChanged = false;
                }
                if (hasNonOrienSizeChanged || (displayChanges & 4096) != 0) {
                    if (TranFoldingScreenManager.isFoldableDevice()) {
                        if (TranFoldingScreenController.shouldRestartPackage(this.packageName)) {
                            return;
                        }
                        WindowProcessController windowProcessController3 = this.app;
                        if (windowProcessController3 != null && windowProcessController3.hasActivityInVisibleTask()) {
                            return;
                        }
                    }
                    restartProcessIfVisible();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onConfigurationChanged$20$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ void m7783x57e59773(int winId) {
        this.mAtmService.mAmInternal.updateMultiWindowProcessList(getPid(), this.packageName, winId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onConfigurationChanged$21$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ void m7784xb9383412(int winId) {
        this.mAtmService.mAmInternal.removeFromMuteProcessList(this.packageName, getPid(), winId);
    }

    @Override // com.android.server.wm.WindowContainer
    void onResize() {
        this.mImeInsetsFrozenUntilStartInput = false;
        super.onResize();
    }

    private boolean applyAspectRatio(Rect outBounds, Rect containingAppBounds, Rect containingBounds) {
        return applyAspectRatio(outBounds, containingAppBounds, containingBounds, 0.0f, false);
    }

    private boolean applyAspectRatio(Rect outBounds, Rect containingAppBounds, Rect containingBounds, float desiredAspectRatio, boolean fixedOrientationLetterboxed) {
        float desiredAspectRatio2;
        boolean adjustWidth;
        float maxAspectRatio = this.info.getMaxAspectRatio();
        Task rootTask = getRootTask();
        float minAspectRatio = getMinAspectRatio();
        if (this.task != null && rootTask != null) {
            if (!inMultiWindowMode() || !isResizeable(false) || fixedOrientationLetterboxed || this.mMinAspectRatioForUser != -1.0f) {
                if ((maxAspectRatio < 1.0f && minAspectRatio < 1.0f && desiredAspectRatio < 1.0f) || isInVrUiMode(getConfiguration())) {
                    return false;
                }
                int containingAppWidth = containingAppBounds.width();
                int containingAppHeight = containingAppBounds.height();
                float containingRatio = computeAspectRatio(containingAppBounds);
                if (desiredAspectRatio >= 1.0f) {
                    desiredAspectRatio2 = desiredAspectRatio;
                } else {
                    desiredAspectRatio2 = containingRatio;
                }
                if (maxAspectRatio >= 1.0f && desiredAspectRatio2 > maxAspectRatio) {
                    desiredAspectRatio2 = maxAspectRatio;
                } else if (minAspectRatio >= 1.0f && desiredAspectRatio2 < minAspectRatio) {
                    desiredAspectRatio2 = minAspectRatio;
                }
                int activityWidth = containingAppWidth;
                int activityHeight = containingAppHeight;
                if (containingRatio > desiredAspectRatio2) {
                    if (containingAppWidth < containingAppHeight) {
                        activityHeight = (int) ((activityWidth * desiredAspectRatio2) + 0.5f);
                    } else {
                        activityWidth = (int) ((activityHeight * desiredAspectRatio2) + 0.5f);
                    }
                } else if (containingRatio < desiredAspectRatio2) {
                    switch (getRequestedConfigurationOrientation()) {
                        case 1:
                            adjustWidth = true;
                            break;
                        case 2:
                            adjustWidth = false;
                            break;
                        default:
                            if (containingAppWidth < containingAppHeight) {
                                adjustWidth = true;
                                break;
                            } else {
                                adjustWidth = false;
                                break;
                            }
                    }
                    if (adjustWidth) {
                        activityWidth = (int) ((activityHeight / desiredAspectRatio2) + 0.5f);
                    } else {
                        activityHeight = (int) ((activityWidth / desiredAspectRatio2) + 0.5f);
                    }
                }
                if (containingAppWidth <= activityWidth && containingAppHeight <= activityHeight) {
                    return false;
                }
                int right = containingAppBounds.left + activityWidth;
                if (right >= containingAppBounds.right) {
                    right += containingBounds.right - containingAppBounds.right;
                }
                int bottom = containingAppBounds.top + activityHeight;
                if (bottom >= containingAppBounds.bottom) {
                    bottom += containingBounds.bottom - containingAppBounds.bottom;
                }
                outBounds.set(containingBounds.left, containingBounds.top, right, bottom);
                if (!outBounds.equals(containingBounds)) {
                    outBounds.left = containingAppBounds.left;
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    private float getMinAspectRatio() {
        float f = this.mMinAspectRatioForUser;
        if (f != -1.0f) {
            return f;
        }
        return this.info.getMinAspectRatio(getRequestedOrientation());
    }

    private boolean hasFixedAspectRatio() {
        return this.info.hasFixedAspectRatio(getRequestedOrientation());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static float computeAspectRatio(Rect rect) {
        int width = rect.width();
        int height = rect.height();
        if (width == 0 || height == 0) {
            return 0.0f;
        }
        return Math.max(width, height) / Math.min(width, height);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldUpdateConfigForDisplayChanged() {
        return this.mLastReportedDisplayId != getDisplayId();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean ensureActivityConfiguration(int globalChanges, boolean preserveWindow) {
        return ensureActivityConfiguration(globalChanges, preserveWindow, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean ensureActivityConfiguration(int globalChanges, boolean preserveWindow, boolean ignoreVisibility) {
        boolean preserveWindow2;
        Task rootTask = getRootTask();
        if (rootTask.mConfigWillChange) {
            if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                String protoLogParam0 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -804217032, 0, (String) null, new Object[]{protoLogParam0});
            }
            return true;
        } else if (!this.finishing) {
            if (isState(State.DESTROYED)) {
                if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                    String protoLogParam02 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, 1105210816, 0, (String) null, new Object[]{protoLogParam02});
                }
                return true;
            } else if (!ignoreVisibility && (this.mState == State.STOPPING || this.mState == State.STOPPED || !shouldBeVisible())) {
                if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                    String protoLogParam03 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, 1635062046, 0, (String) null, new Object[]{protoLogParam03});
                }
                return true;
            } else {
                if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                    String protoLogParam04 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -1791031393, 0, (String) null, new Object[]{protoLogParam04});
                }
                int newDisplayId = getDisplayId();
                boolean displayChanged = this.mLastReportedDisplayId != newDisplayId;
                if (displayChanged) {
                    this.mLastReportedDisplayId = newDisplayId;
                }
                this.mTmpConfig.setTo(this.mLastReportedConfiguration.getMergedConfiguration());
                if (!getConfiguration().equals(this.mTmpConfig) || this.forceNewConfig || displayChanged) {
                    int changes = getConfigurationChanges(this.mTmpConfig);
                    ITranActivityRecord.Instance().setMultiWindowConfigAndMode(this, this.mTmpConfig);
                    Configuration newMergedOverrideConfig = getMergedOverrideConfiguration();
                    setLastReportedConfiguration(getProcessGlobalConfiguration(), newMergedOverrideConfig);
                    if (this.mState == State.INITIALIZING) {
                        if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                            String protoLogParam05 = String.valueOf(this);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -235225312, 0, (String) null, new Object[]{protoLogParam05});
                        }
                        return true;
                    } else if (changes == 0 && !this.forceNewConfig) {
                        if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                            String protoLogParam06 = String.valueOf(this);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -743431900, 0, (String) null, new Object[]{protoLogParam06});
                        }
                        if (displayChanged) {
                            scheduleActivityMovedToDisplay(newDisplayId, newMergedOverrideConfig);
                        } else {
                            scheduleConfigurationChanged(newMergedOverrideConfig);
                        }
                        return true;
                    } else {
                        if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                            String protoLogParam07 = String.valueOf(this);
                            String protoLogParam1 = String.valueOf(Configuration.configurationDiffToString(changes));
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -929676529, 0, (String) null, new Object[]{protoLogParam07, protoLogParam1});
                        }
                        if (!attachedToProcess()) {
                            if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                                String protoLogParam08 = String.valueOf(this);
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, 1679569477, 0, (String) null, new Object[]{protoLogParam08});
                            }
                            stopFreezingScreenLocked(false);
                            this.forceNewConfig = false;
                            return true;
                        }
                        if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                            String protoLogParam09 = String.valueOf(this.info.name);
                            String protoLogParam12 = String.valueOf(Integer.toHexString(changes));
                            String protoLogParam2 = String.valueOf(Integer.toHexString(this.info.getRealConfigChanged()));
                            String protoLogParam3 = String.valueOf(this.mLastReportedConfiguration);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, 1995093920, 0, (String) null, new Object[]{protoLogParam09, protoLogParam12, protoLogParam2, protoLogParam3});
                        }
                        if (shouldRelaunchLocked(changes, this.mTmpConfig) || this.forceNewConfig) {
                            this.configChangeFlags |= changes;
                            startFreezingScreenLocked(globalChanges);
                            this.forceNewConfig = false;
                            if (ThunderbackConfig.isVersion4()) {
                                preserveWindow2 = preserveWindow & ((!isResizeOnlyChange(changes) || this.mFreezingScreen || ITranActivityRecord.Instance().isMultiWindowModeConfiged()) ? false : true);
                            } else if (!ThunderbackConfig.isVersion3()) {
                                preserveWindow2 = preserveWindow;
                            } else {
                                preserveWindow2 = preserveWindow & ((!isResizeOnlyChange(changes) || this.mFreezingScreen || ITranActivityRecord.Instance().isInMultiWindowMode() || ITranActivityRecord.Instance().isMultiWindowModeConfiged()) ? false : true);
                            }
                            boolean hasResizeChange = hasResizeChange((~this.info.getRealConfigChanged()) & changes);
                            if (hasResizeChange) {
                                boolean isDragResizing = this.task.isDragResizing();
                                this.mRelaunchReason = isDragResizing ? 2 : 1;
                            } else {
                                this.mRelaunchReason = 0;
                            }
                            if (this.mState == State.PAUSING) {
                                if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                                    String protoLogParam010 = String.valueOf(this);
                                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -90559682, 0, (String) null, new Object[]{protoLogParam010});
                                }
                                this.deferRelaunchUntilPaused = true;
                                this.preserveWindowOnDeferredRelaunch = preserveWindow2;
                                return true;
                            }
                            if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                                String protoLogParam011 = String.valueOf(this);
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, 736692676, 0, (String) null, new Object[]{protoLogParam011});
                            }
                            if (!this.mVisibleRequested && ProtoLogCache.WM_DEBUG_STATES_enabled) {
                                String protoLogParam012 = String.valueOf(this);
                                String protoLogParam13 = String.valueOf(Debug.getCallers(4));
                                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1558137010, 0, (String) null, new Object[]{protoLogParam012, protoLogParam13});
                            }
                            relaunchActivityLocked(preserveWindow2);
                            return false;
                        }
                        if (displayChanged) {
                            scheduleActivityMovedToDisplay(newDisplayId, newMergedOverrideConfig);
                        } else {
                            scheduleConfigurationChanged(newMergedOverrideConfig);
                        }
                        stopFreezingScreenLocked(false);
                        return true;
                    }
                }
                if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                    String protoLogParam013 = String.valueOf(this);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -1115019498, 0, (String) null, new Object[]{protoLogParam013});
                }
                if (this.mVisibleRequested) {
                    updateCompatDisplayInsets();
                }
                return true;
            }
        } else {
            if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
                String protoLogParam014 = String.valueOf(this);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, -846078709, 0, (String) null, new Object[]{protoLogParam014});
            }
            stopFreezingScreenLocked(false);
            return true;
        }
    }

    private Configuration getProcessGlobalConfiguration() {
        WindowProcessController windowProcessController = this.app;
        return windowProcessController != null ? windowProcessController.getConfiguration() : this.mAtmService.getGlobalConfiguration();
    }

    private boolean shouldRelaunchLocked(int changes, Configuration changesConfig) {
        int configChanged = this.info.getRealConfigChanged();
        boolean onlyVrUiModeChanged = onlyVrUiModeChanged(changes, changesConfig);
        if (ITranActivityRecord.Instance().isMultiWindowModeConfiged() || ITranActivityRecord.Instance().isInMultiWindowMode()) {
            configChanged |= ITranActivityRecord.Instance().getConfigChange(changes, this.packageName);
            if (ITranMultiWindow.Instance().shouldForceRelaunch(this.packageName, this.mRootWindowContainer.getDefaultDisplay().getDisplayMetrics()) && ITranActivityRecord.Instance().isMultiWindowModeConfiged()) {
                Slog.d(TAG, "forceRelaunch, pkg:" + this.packageName);
                return true;
            } else if (ITranActivityRecord.Instance().isMultiWindowModeConfiged() && ITranMultiWindow.Instance().shouldForceNonRelaunch(this.packageName)) {
                Slog.d(TAG, "forceNonRelaunch, pkg:" + this.packageName);
                return false;
            }
        }
        if (getDisplayContent() != null && getDisplayContent().isSourceConnectDisplay()) {
            configChanged |= ITranActivityRecord.Instance().getSourceConnectConfig();
        }
        if (this.info.applicationInfo.targetSdkVersion < 26 && this.requestedVrComponent != null && onlyVrUiModeChanged) {
            configChanged |= 512;
        }
        if (this.isForceNotRelaunchForCarMode && onlyCarUiModeChanged(changes, changesConfig)) {
            configChanged |= 512;
        }
        if (isEmbedded() && onlyNavBarHeightChanged(changes, changesConfig)) {
            configChanged |= 1024;
        }
        if (isEmbedded() && Objects.equals(this.mActivityComponent, ComponentName.unflattenFromString("com.alibaba.intl.android.apps.poseidon/.app.activity.ActivityMainMaterial"))) {
            configChanged |= 2048;
        }
        return ((~configChanged) & changes) != 0;
    }

    private boolean onlyVrUiModeChanged(int changes, Configuration lastReportedConfig) {
        Configuration currentConfig = getConfiguration();
        return changes == 512 && isInVrUiMode(currentConfig) != isInVrUiMode(lastReportedConfig);
    }

    private boolean onlyCarUiModeChanged(int changes, Configuration lastReportedConfig) {
        Configuration currentConfig = getConfiguration();
        return changes == 512 && isInCarUiMode(currentConfig) != isInCarUiMode(lastReportedConfig);
    }

    private boolean onlyNavBarHeightChanged(int changes, Configuration lastReportedConfig) {
        Configuration currentConfig = getConfiguration();
        return changes == 1024 && currentConfig.screenWidthDp == lastReportedConfig.screenWidthDp && Math.abs(currentConfig.screenHeightDp - lastReportedConfig.screenHeightDp) == 40;
    }

    private int getConfigurationChanges(Configuration lastReportedConfig) {
        int changes = SizeConfigurationBuckets.filterDiff(lastReportedConfig.diff(getConfiguration()), lastReportedConfig, getConfiguration(), this.mSizeConfigurations);
        if ((536870912 & changes) != 0) {
            return changes & (-536870913);
        }
        return changes;
    }

    private static boolean isResizeOnlyChange(int change) {
        return (change & (-3457)) == 0;
    }

    private static boolean hasResizeChange(int change) {
        return (change & 3456) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void relaunchActivityLocked(boolean preserveWindow) {
        ResumeActivityItem obtain;
        if (this.mAtmService.mSuppressResizeConfigChanges && preserveWindow) {
            this.configChangeFlags = 0;
            return;
        }
        Task rootTask = getRootTask();
        if (rootTask != null && rootTask.mTranslucentActivityWaiting == this) {
            rootTask.checkTranslucentActivityWaiting(null);
        }
        boolean andResume = shouldBeResumed(null);
        List<ResultInfo> pendingResults = null;
        List<ReferrerIntent> pendingNewIntents = null;
        if (andResume) {
            pendingResults = this.results;
            pendingNewIntents = this.newIntents;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(TAG_SWITCH, "Relaunching: " + this + " with results=" + pendingResults + " newIntents=" + pendingNewIntents + " andResume=" + andResume + " preserveWindow=" + preserveWindow);
        }
        if (andResume) {
            EventLogTags.writeWmRelaunchResumeActivity(this.mUserId, System.identityHashCode(this), this.task.mTaskId, this.shortComponentName);
        } else {
            EventLogTags.writeWmRelaunchActivity(this.mUserId, System.identityHashCode(this), this.task.mTaskId, this.shortComponentName);
        }
        startFreezingScreenLocked(0);
        try {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(andResume ? "RESUMED" : "PAUSED");
                String protoLogParam1 = String.valueOf(this);
                String protoLogParam2 = String.valueOf(Debug.getCallers(6));
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_STATES, -1016578046, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
            }
            this.forceNewConfig = false;
            startRelaunching();
            ActivityRelaunchItem obtain2 = ActivityRelaunchItem.obtain(pendingResults, pendingNewIntents, this.configChangeFlags, new MergedConfiguration(getProcessGlobalConfiguration(), getMergedOverrideConfiguration()), preserveWindow);
            if (andResume) {
                obtain = ResumeActivityItem.obtain(isTransitionForward());
            } else {
                obtain = PauseActivityItem.obtain();
            }
            IApplicationThread tempThread = this.app.getThread();
            if (tempThread == null) {
                Slog.w(TAG, this.app.mName + ": app thread isnull");
            } else {
                ClientTransaction transaction = ClientTransaction.obtain(tempThread, this.token);
                transaction.addCallback(obtain2);
                transaction.setLifecycleStateRequest(obtain);
                this.mAtmService.getLifecycleManager().scheduleTransaction(transaction);
            }
        } catch (RemoteException e) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam02 = String.valueOf(e);
                ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_STATES, -262984451, 0, (String) null, new Object[]{protoLogParam02});
            }
        }
        if (andResume) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam03 = String.valueOf(this);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, 1270792394, 0, (String) null, new Object[]{protoLogParam03});
            }
            this.results = null;
            this.newIntents = null;
            this.mAtmService.getAppWarningsLocked().onResumeActivity(this);
            ITranWindowManagerService.Instance().onActivityResume(this);
            ITranActivityRecord.Instance().activityResumeHook(this.info);
            ITranActivityRecord.Instance().hookRelaunchActivityLocked(this);
            this.mAtmService.mAmsExt.onAfterActivityResumed(this);
            if (this.info != null) {
                ITranWindowManagerService.Instance().setActivityResumedHook(this);
                if (this.mPnpProp == null) {
                    SystemProperties.set("persist.sys.pnp.relaunch.optimize", "on");
                    this.mPnpProp = "on";
                }
            }
        } else {
            removePauseTimeout();
            setState(State.PAUSED, "relaunchActivityLocked");
        }
        this.mTaskSupervisor.mStoppingActivities.remove(this);
        this.configChangeFlags = 0;
        this.deferRelaunchUntilPaused = false;
        this.preserveWindowOnDeferredRelaunch = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void restartProcessIfVisible() {
        Slog.i(TAG, "Request to restart process of " + this);
        clearSizeCompatMode();
        if (!attachedToProcess() || isState(State.DESTROYING, State.DESTROYED)) {
            return;
        }
        setState(State.RESTARTING_PROCESS, "restartActivityProcess");
        boolean z = true;
        if (!this.mVisibleRequested || this.mHaveState) {
            boolean isResizeable = TranFoldingScreenManager.isFoldableDevice();
            if (isResizeable) {
                this.forceNewConfig = true;
                return;
            } else {
                this.mAtmService.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda17
                    @Override // java.lang.Runnable
                    public final void run() {
                        ActivityRecord.this.m7785xafffd90f();
                    }
                });
                return;
            }
        }
        Task task = this.task;
        if (task != null) {
            if (!task.isResizeable() && !isResizeable()) {
                z = false;
            }
        } else {
            z = isResizeable();
        }
        boolean isResizeable2 = z;
        if (isResizeable2) {
            return;
        }
        if (getParent() != null) {
            startFreezingScreen();
        }
        try {
            this.mAtmService.getLifecycleManager().scheduleTransaction(this.app.getThread(), this.token, (ActivityLifecycleItem) StopActivityItem.obtain(0));
        } catch (RemoteException e) {
            Slog.w(TAG, "Exception thrown during restart " + this, e);
        }
        this.mTaskSupervisor.scheduleRestartTimeout(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$restartProcessIfVisible$22$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ void m7785xafffd90f() {
        synchronized (this.mAtmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (hasProcess() && this.app.getReportedProcState() > 6) {
                    WindowProcessController wpc = this.app;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    this.mAtmService.mAmInternal.killProcess(wpc.mName, wpc.mUid, "resetConfig");
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isProcessRunning() {
        WindowProcessController proc = this.app;
        if (proc == null) {
            proc = (WindowProcessController) this.mAtmService.mProcessNames.get(this.processName, this.info.applicationInfo.uid);
        }
        return proc != null && proc.hasThread();
    }

    private boolean allowTaskSnapshot() {
        ArrayList<ReferrerIntent> arrayList = this.newIntents;
        if (arrayList == null) {
            return true;
        }
        for (int i = arrayList.size() - 1; i >= 0; i--) {
            Intent intent = this.newIntents.get(i);
            if (intent != null && !isMainIntent(intent)) {
                Intent intent2 = this.mLastNewIntent;
                boolean sameIntent = intent2 != null ? intent2.filterEquals(intent) : this.intent.filterEquals(intent);
                if (!sameIntent || intent.getExtras() != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNoHistory() {
        return ((this.intent.getFlags() & 1073741824) == 0 && (this.info.flags & 128) == 0) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveToXml(TypedXmlSerializer out) throws IOException, XmlPullParserException {
        out.attributeLong((String) null, ATTR_ID, this.createTime);
        out.attributeInt((String) null, ATTR_LAUNCHEDFROMUID, this.launchedFromUid);
        String str = this.launchedFromPackage;
        if (str != null) {
            out.attribute((String) null, ATTR_LAUNCHEDFROMPACKAGE, str);
        }
        String str2 = this.launchedFromFeatureId;
        if (str2 != null) {
            out.attribute((String) null, ATTR_LAUNCHEDFROMFEATURE, str2);
        }
        String str3 = this.resolvedType;
        if (str3 != null) {
            out.attribute((String) null, ATTR_RESOLVEDTYPE, str3);
        }
        out.attributeBoolean((String) null, ATTR_COMPONENTSPECIFIED, this.componentSpecified);
        out.attributeInt((String) null, ATTR_USERID, this.mUserId);
        ActivityManager.TaskDescription taskDescription = this.taskDescription;
        if (taskDescription != null) {
            taskDescription.saveToXml(out);
        }
        out.startTag((String) null, TAG_INTENT);
        this.intent.saveToXml(out);
        out.endTag((String) null, TAG_INTENT);
        if (isPersistable() && this.mPersistentState != null) {
            out.startTag((String) null, TAG_PERSISTABLEBUNDLE);
            this.mPersistentState.saveToXml(out);
            out.endTag((String) null, TAG_PERSISTABLEBUNDLE);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ActivityRecord restoreFromXml(TypedXmlPullParser in, ActivityTaskSupervisor taskSupervisor) throws IOException, XmlPullParserException {
        Intent intent = null;
        PersistableBundle persistentState = null;
        int launchedFromUid = in.getAttributeInt((String) null, ATTR_LAUNCHEDFROMUID, 0);
        String launchedFromPackage = in.getAttributeValue((String) null, ATTR_LAUNCHEDFROMPACKAGE);
        String launchedFromFeature = in.getAttributeValue((String) null, ATTR_LAUNCHEDFROMFEATURE);
        String resolvedType = in.getAttributeValue((String) null, ATTR_RESOLVEDTYPE);
        boolean componentSpecified = in.getAttributeBoolean((String) null, ATTR_COMPONENTSPECIFIED, false);
        int userId = in.getAttributeInt((String) null, ATTR_USERID, 0);
        long createTime = in.getAttributeLong((String) null, ATTR_ID, -1L);
        int outerDepth = in.getDepth();
        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription();
        taskDescription.restoreFromXml(in);
        while (true) {
            int event = in.next();
            if (event == 1 || (event == 3 && in.getDepth() < outerDepth)) {
                break;
            } else if (event == 2) {
                String name = in.getName();
                if (TAG_INTENT.equals(name)) {
                    intent = Intent.restoreFromXml(in);
                } else if (TAG_PERSISTABLEBUNDLE.equals(name)) {
                    persistentState = PersistableBundle.restoreFromXml(in);
                } else {
                    Slog.w(TAG, "restoreActivity: unexpected name=" + name);
                    XmlUtils.skipCurrentTag(in);
                }
            }
        }
        if (intent != null) {
            ActivityTaskManagerService service = taskSupervisor.mService;
            ActivityInfo aInfo = taskSupervisor.resolveActivity(intent, resolvedType, 0, null, userId, Binder.getCallingUid());
            if (aInfo == null) {
                throw new XmlPullParserException("restoreActivity resolver error. Intent=" + intent + " resolvedType=" + resolvedType);
            }
            return new Builder(service).setLaunchedFromUid(launchedFromUid).setLaunchedFromPackage(launchedFromPackage).setLaunchedFromFeature(launchedFromFeature).setIntent(intent).setResolvedType(resolvedType).setActivityInfo(aInfo).setComponentSpecified(componentSpecified).setPersistentState(persistentState).setTaskDescription(taskDescription).setCreateTime(createTime).build();
        }
        throw new XmlPullParserException("restoreActivity error intent=" + intent);
    }

    private static boolean isInVrUiMode(Configuration config) {
        return (config.uiMode & 15) == 7;
    }

    private static boolean isInCarUiMode(Configuration config) {
        return (config.uiMode & 15) == 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getProcessName() {
        return this.info.applicationInfo.processName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUid() {
        return this.info.applicationInfo.uid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUid(int uid) {
        return this.info.applicationInfo.uid == uid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getPid() {
        WindowProcessController windowProcessController = this.app;
        if (windowProcessController != null) {
            return windowProcessController.getPid();
        }
        return 0;
    }

    int getLaunchedFromPid() {
        return this.launchedFromPid;
    }

    int getLaunchedFromUid() {
        return this.launchedFromUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getFilteredReferrer(String referrerPackage) {
        if (referrerPackage != null) {
            if (!referrerPackage.equals(this.packageName) && this.mWmService.mPmInternal.filterAppAccess(referrerPackage, this.info.applicationInfo.uid, this.mUserId)) {
                return null;
            }
            return referrerPackage;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canTurnScreenOn() {
        if (getTurnScreenOnFlag()) {
            Task rootTask = getRootTask();
            return this.mCurrentLaunchCanTurnScreenOn && rootTask != null && this.mTaskSupervisor.getKeyguardController().checkKeyguardVisibility(this);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTurnScreenOn(boolean turnScreenOn) {
        this.mTurnScreenOn = turnScreenOn;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getTurnScreenOnFlag() {
        return this.mTurnScreenOn || containsTurnScreenOnWindow();
    }

    private boolean containsTurnScreenOnWindow() {
        if (isRelaunching()) {
            return this.mLastContainsTurnScreenOnWindow;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if ((((WindowState) this.mChildren.get(i)).mAttrs.flags & 2097152) != 0) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canResumeByCompat() {
        WindowProcessController windowProcessController = this.app;
        return windowProcessController == null || windowProcessController.updateTopResumingActivityInProcessIfNeeded(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTopRunningActivity() {
        return this.mRootWindowContainer.topRunningActivity() == this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFocusedActivityOnDisplay() {
        return this.mDisplayContent.forAllTaskDisplayAreas(new Predicate() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda18
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ActivityRecord.this.m7779x6a495f9a((TaskDisplayArea) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isFocusedActivityOnDisplay$23$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ boolean m7779x6a495f9a(TaskDisplayArea taskDisplayArea) {
        return taskDisplayArea.getFocusedActivity() == this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRootOfTask() {
        Task task = this.task;
        if (task == null) {
            return false;
        }
        ActivityRecord rootActivity = task.getRootActivity(true);
        return this == rootActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTaskOverlay(boolean taskOverlay) {
        this.mTaskOverlay = taskOverlay;
        setAlwaysOnTop(taskOverlay);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTaskOverlay() {
        return this.mTaskOverlay;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public boolean isAlwaysOnTop() {
        return this.mTaskOverlay || super.isAlwaysOnTop();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean showToCurrentUser() {
        return this.mShowForAllUsers || this.mWmService.isCurrentProfile(this.mUserId);
    }

    @Override // com.android.server.wm.WindowContainer
    boolean canCustomizeAppTransition() {
        return true;
    }

    @Override // com.android.server.wm.WindowToken
    public String toString() {
        if (this.stringName != null) {
            StringBuilder append = new StringBuilder().append(this.stringName).append(" t");
            Task task = this.task;
            return append.append(task == null ? -1 : task.mTaskId).append(this.finishing ? " f}" : "").append(this.mIsExiting ? " isExiting" : "").append("}").toString();
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ActivityRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" u");
        sb.append(this.mUserId);
        sb.append(' ');
        sb.append(this.intent.getComponent().flattenToShortString());
        sb.append("}");
        this.stringName = sb.toString();
        return this.stringName;
    }

    void dumpDebug(ProtoOutputStream proto, int logLevel) {
        writeNameToProto(proto, CompanionAppsPermissions.AppPermissions.PACKAGE_NAME);
        super.dumpDebug(proto, 1146756268034L, logLevel);
        proto.write(1133871366147L, this.mLastSurfaceShowing);
        proto.write(1133871366148L, isWaitingForTransitionStart());
        proto.write(1133871366149L, isAnimating(2, 1));
        if (this.mThumbnail != null) {
            this.mThumbnail.dumpDebug(proto, 1146756268038L);
        }
        proto.write(1133871366151L, fillsParent());
        proto.write(1133871366152L, this.mAppStopped);
        proto.write(1133871366174L, !occludesParent());
        proto.write(1133871366168L, this.mVisible);
        proto.write(1133871366153L, this.mVisibleRequested);
        proto.write(1133871366154L, isClientVisible());
        proto.write(1133871366155L, this.mDeferHidingClient);
        proto.write(1133871366156L, this.mReportedDrawn);
        proto.write(1133871366157L, this.reportedVisible);
        proto.write(1120986464270L, this.mNumInterestingWindows);
        proto.write(1120986464271L, this.mNumDrawnWindows);
        proto.write(1133871366160L, this.allDrawn);
        proto.write(1133871366161L, this.mLastAllDrawn);
        WindowState windowState = this.mStartingWindow;
        if (windowState != null) {
            windowState.writeIdentifierToProto(proto, 1146756268051L);
        }
        proto.write(1133871366164L, isStartingWindowDisplayed());
        proto.write(1133871366345L, this.startingMoved);
        proto.write(1133871366166L, this.mVisibleSetFromTransferredStartingWindow);
        proto.write(1138166333467L, this.mState.toString());
        proto.write(1133871366172L, isRootOfTask());
        if (hasProcess()) {
            proto.write(1120986464285L, this.app.getPid());
        }
        proto.write(1133871366175L, this.pictureInPictureArgs.isAutoEnterEnabled());
        proto.write(1133871366176L, inSizeCompatMode());
        proto.write(1108101562401L, getMinAspectRatio());
        proto.write(1133871366178L, providesMaxBounds());
        proto.write(1133871366179L, this.mEnableRecentsScreenshot);
        proto.write(1120986464292L, this.mLastDropInputMode);
    }

    @Override // com.android.server.wm.WindowToken, com.android.server.wm.WindowContainer
    long getProtoFieldId() {
        return 1146756268038L;
    }

    @Override // com.android.server.wm.WindowToken, com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        if (logLevel == 2 && !isVisible()) {
            return;
        }
        long token = proto.start(fieldId);
        dumpDebug(proto, logLevel);
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeNameToProto(ProtoOutputStream proto, long fieldId) {
        proto.write(fieldId, this.shortComponentName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void writeIdentifierToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, System.identityHashCode(this));
        proto.write(1120986464258L, this.mUserId);
        proto.write(1138166333443L, this.intent.getComponent().flattenToShortString());
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class CompatDisplayInsets {
        private final int mHeight;
        final boolean mIsFloating;
        final boolean mIsInFixedOrientationLetterbox;
        final int mOriginalRequestedOrientation;
        final int mOriginalRotation;
        private final int mWidth;
        final Rect[] mNonDecorInsets = new Rect[4];
        final Rect[] mStableInsets = new Rect[4];

        CompatDisplayInsets(DisplayContent display, ActivityRecord container, Rect fixedOrientationBounds) {
            boolean z;
            Rect filledContainerBounds;
            int filledContainerRotation;
            int i = 4;
            this.mOriginalRotation = display.getRotation();
            boolean tasksAreFloating = container.getWindowConfiguration().tasksAreFloating();
            this.mIsFloating = tasksAreFloating;
            this.mOriginalRequestedOrientation = container.getRequestedConfigurationOrientation();
            boolean z2 = false;
            if (tasksAreFloating) {
                Rect containerBounds = container.getWindowConfiguration().getBounds();
                this.mWidth = containerBounds.width();
                this.mHeight = containerBounds.height();
                Rect emptyRect = new Rect();
                for (int rotation = 0; rotation < 4; rotation++) {
                    this.mNonDecorInsets[rotation] = emptyRect;
                    this.mStableInsets[rotation] = emptyRect;
                }
                this.mIsInFixedOrientationLetterbox = false;
                return;
            }
            Task task = container.getTask();
            if (fixedOrientationBounds == null) {
                z = false;
            } else {
                z = true;
            }
            this.mIsInFixedOrientationLetterbox = z;
            if (z) {
                filledContainerBounds = fixedOrientationBounds;
            } else {
                filledContainerBounds = task != null ? task.getBounds() : display.getBounds();
            }
            if (container.getRequestedOverrideConfiguration().windowConfiguration.getWindowingMode() == 0 && container.inMultiWindowMode() && (filledContainerBounds.height() < display.getBounds().height() || filledContainerBounds.width() < display.getBounds().width())) {
                filledContainerBounds.left = 0;
                filledContainerBounds.top = 0;
                filledContainerBounds.right = display.getBounds().width();
                filledContainerBounds.bottom = display.getBounds().height();
            }
            if (task != null) {
                filledContainerRotation = task.getConfiguration().windowConfiguration.getRotation();
            } else {
                filledContainerRotation = display.getConfiguration().windowConfiguration.getRotation();
            }
            Point dimensions = getRotationZeroDimensions(filledContainerBounds, filledContainerRotation);
            this.mWidth = dimensions.x;
            this.mHeight = dimensions.y;
            Rect unfilledContainerBounds = filledContainerBounds.equals(display.getBounds()) ? null : new Rect();
            DisplayPolicy policy = display.getDisplayPolicy();
            int rotation2 = 0;
            while (rotation2 < i) {
                this.mNonDecorInsets[rotation2] = new Rect();
                this.mStableInsets[rotation2] = new Rect();
                boolean rotated = (rotation2 == 1 || rotation2 == 3) ? true : z2;
                int dw = rotated ? display.mBaseDisplayHeight : display.mBaseDisplayWidth;
                int dh = rotated ? display.mBaseDisplayWidth : display.mBaseDisplayHeight;
                DisplayCutout cutout = display.calculateDisplayCutoutForRotation(rotation2).getDisplayCutout();
                policy.getNonDecorInsetsLw(rotation2, cutout, this.mNonDecorInsets[rotation2]);
                this.mStableInsets[rotation2].set(this.mNonDecorInsets[rotation2]);
                policy.convertNonDecorInsetsToStableInsets(this.mStableInsets[rotation2], rotation2);
                if (unfilledContainerBounds != null) {
                    unfilledContainerBounds.set(filledContainerBounds);
                    display.rotateBounds(filledContainerRotation, rotation2, unfilledContainerBounds);
                    updateInsetsForBounds(unfilledContainerBounds, dw, dh, this.mNonDecorInsets[rotation2]);
                    updateInsetsForBounds(unfilledContainerBounds, dw, dh, this.mStableInsets[rotation2]);
                }
                rotation2++;
                i = 4;
                z2 = false;
            }
        }

        private static Point getRotationZeroDimensions(Rect bounds, int rotation) {
            boolean rotated = true;
            if (rotation != 1 && rotation != 3) {
                rotated = false;
            }
            int width = bounds.width();
            int height = bounds.height();
            return rotated ? new Point(height, width) : new Point(width, height);
        }

        private static void updateInsetsForBounds(Rect bounds, int displayWidth, int displayHeight, Rect inset) {
            inset.left = Math.max(0, inset.left - bounds.left);
            inset.top = Math.max(0, inset.top - bounds.top);
            inset.right = Math.max(0, (bounds.right - displayWidth) + inset.right);
            inset.bottom = Math.max(0, (bounds.bottom - displayHeight) + inset.bottom);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public void getBoundsByRotation(Rect outBounds, int rotation) {
            boolean rotated = true;
            if (rotation != 1 && rotation != 3) {
                rotated = false;
            }
            int dw = rotated ? this.mHeight : this.mWidth;
            int dh = rotated ? this.mWidth : this.mHeight;
            outBounds.set(0, 0, dw, dh);
        }

        void getFrameByOrientation(Rect outBounds, int orientation) {
            int longSide = Math.max(this.mWidth, this.mHeight);
            int shortSide = Math.min(this.mWidth, this.mHeight);
            boolean isLandscape = orientation == 2;
            outBounds.set(0, 0, isLandscape ? longSide : shortSide, isLandscape ? shortSide : longSide);
        }

        void getContainerBounds(Rect outAppBounds, Rect outBounds, int rotation, int orientation, boolean orientationRequested, boolean isFixedToUserRotation) {
            getFrameByOrientation(outBounds, orientation);
            if (this.mIsFloating) {
                outAppBounds.set(outBounds);
                return;
            }
            getBoundsByRotation(outAppBounds, rotation);
            int dW = outAppBounds.width();
            int dH = outAppBounds.height();
            boolean isOrientationMismatched = (outBounds.width() > outBounds.height()) != (dW > dH);
            if (isOrientationMismatched && isFixedToUserRotation && orientationRequested) {
                if (orientation == 2) {
                    outBounds.bottom = (int) ((dW * dW) / dH);
                    outBounds.right = dW;
                } else {
                    outBounds.bottom = dH;
                    outBounds.right = (int) ((dH * dH) / dW);
                }
                outBounds.offset(ActivityRecord.getHorizontalCenterOffset(this.mWidth, outBounds.width()), 0);
            }
            outAppBounds.set(outBounds);
            if (isOrientationMismatched) {
                Rect insets = this.mNonDecorInsets[rotation];
                outBounds.offset(insets.left, insets.top);
                outAppBounds.offset(insets.left, insets.top);
            } else if (rotation != -1) {
                TaskFragment.intersectWithInsetsIfFits(outAppBounds, outBounds, this.mNonDecorInsets[rotation]);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class AppSaturationInfo {
        float[] mMatrix;
        float[] mTranslation;

        private AppSaturationInfo() {
            this.mMatrix = new float[9];
            this.mTranslation = new float[3];
        }

        void setSaturation(float[] matrix, float[] translation) {
            float[] fArr = this.mMatrix;
            System.arraycopy(matrix, 0, fArr, 0, fArr.length);
            float[] fArr2 = this.mTranslation;
            System.arraycopy(translation, 0, fArr2, 0, fArr2.length);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public RemoteAnimationTarget createRemoteAnimationTarget(RemoteAnimationController.RemoteAnimationRecord record) {
        WindowState mainWindow = findMainWindow();
        if (this.task == null || mainWindow == null) {
            return null;
        }
        Rect insets = mainWindow.getInsetsStateWithVisibilityOverride().calculateInsets(this.task.getBounds(), WindowInsets.Type.systemBars(), false).toRect();
        InsetUtils.addInsets(insets, getLetterboxInsets());
        RemoteAnimationTarget target = new RemoteAnimationTarget(this.task.mTaskId, record.getMode(), record.mAdapter.mCapturedLeash, !fillsParent(), new Rect(), insets, getPrefixOrderIndex(), record.mAdapter.mPosition, record.mAdapter.mLocalBounds, record.mAdapter.mEndBounds, this.task.getWindowConfiguration(), false, record.mThumbnailAdapter != null ? record.mThumbnailAdapter.mCapturedLeash : null, record.mStartBounds, this.task.getTaskInfo(), checkEnterPictureInPictureAppOpsState());
        target.hasAnimatingParent = record.hasAnimatingParent();
        return target;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean canCreateRemoteAnimationTarget() {
        return true;
    }

    @Override // com.android.server.wm.WindowContainer
    void getAnimationFrames(Rect outFrame, Rect outInsets, Rect outStableInsets, Rect outSurfaceInsets) {
        WindowState win = findMainWindow();
        if (win == null) {
            return;
        }
        win.getAnimationFrames(outFrame, outInsets, outStableInsets, outSurfaceInsets);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPictureInPictureParams(PictureInPictureParams p) {
        this.pictureInPictureArgs.copyOnlySet(p);
        getTask().getRootTask().onPictureInPictureParamsChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShouldDockBigOverlays(boolean shouldDockBigOverlays) {
        this.shouldDockBigOverlays = shouldDockBigOverlays;
        getTask().getRootTask().onShouldDockBigOverlaysChanged();
    }

    @Override // com.android.server.wm.WindowContainer
    boolean isSyncFinished() {
        if (super.isSyncFinished()) {
            if (isVisibleRequested()) {
                if (isAttached()) {
                    for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                        if (((WindowState) this.mChildren.get(i)).isVisibleRequested()) {
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    @Override // com.android.server.wm.WindowContainer
    void finishSync(SurfaceControl.Transaction outMergedTransaction, boolean cancel) {
        this.mLastAllReadyAtSync = allSyncFinished();
        super.finishSync(outMergedTransaction, cancel);
    }

    @Override // com.android.server.wm.WindowContainer
    boolean canBeAnimationTarget() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Point getMinDimensions() {
        ActivityInfo.WindowLayout windowLayout = this.info.windowLayout;
        if (windowLayout == null) {
            return null;
        }
        return new Point(windowLayout.minWidth, windowLayout.minHeight);
    }

    /* loaded from: classes2.dex */
    static class Builder {
        private ActivityInfo mActivityInfo;
        private final ActivityTaskManagerService mAtmService;
        private WindowProcessController mCallerApp;
        private boolean mComponentSpecified;
        private Configuration mConfiguration;
        private long mCreateTime;
        private Intent mIntent;
        private String mLaunchedFromFeature;
        private String mLaunchedFromPackage;
        private int mLaunchedFromPid;
        private int mLaunchedFromUid;
        private ActivityOptions mOptions;
        private PersistableBundle mPersistentState;
        private int mRequestCode;
        private String mResolvedType;
        private ActivityRecord mResultTo;
        private String mResultWho;
        private boolean mRootVoiceInteraction;
        private ActivityRecord mSourceRecord;
        private ActivityManager.TaskDescription mTaskDescription;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(ActivityTaskManagerService service) {
            this.mAtmService = service;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setCaller(WindowProcessController caller) {
            this.mCallerApp = caller;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setLaunchedFromPid(int pid) {
            this.mLaunchedFromPid = pid;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setLaunchedFromUid(int uid) {
            this.mLaunchedFromUid = uid;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setLaunchedFromPackage(String fromPackage) {
            this.mLaunchedFromPackage = fromPackage;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setLaunchedFromFeature(String fromFeature) {
            this.mLaunchedFromFeature = fromFeature;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setIntent(Intent intent) {
            this.mIntent = intent;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setResolvedType(String resolvedType) {
            this.mResolvedType = resolvedType;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setActivityInfo(ActivityInfo activityInfo) {
            this.mActivityInfo = activityInfo;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setResultTo(ActivityRecord resultTo) {
            this.mResultTo = resultTo;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setResultWho(String resultWho) {
            this.mResultWho = resultWho;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setRequestCode(int reqCode) {
            this.mRequestCode = reqCode;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setComponentSpecified(boolean componentSpecified) {
            this.mComponentSpecified = componentSpecified;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setRootVoiceInteraction(boolean rootVoiceInteraction) {
            this.mRootVoiceInteraction = rootVoiceInteraction;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setActivityOptions(ActivityOptions options) {
            this.mOptions = options;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setConfiguration(Configuration config) {
            this.mConfiguration = config;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setSourceRecord(ActivityRecord source) {
            this.mSourceRecord = source;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setPersistentState(PersistableBundle persistentState) {
            this.mPersistentState = persistentState;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setTaskDescription(ActivityManager.TaskDescription taskDescription) {
            this.mTaskDescription = taskDescription;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setCreateTime(long createTime) {
            this.mCreateTime = createTime;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ActivityRecord build() {
            if (this.mConfiguration == null) {
                this.mConfiguration = this.mAtmService.getConfiguration();
            }
            ActivityTaskManagerService activityTaskManagerService = this.mAtmService;
            return new ActivityRecord(activityTaskManagerService, this.mCallerApp, this.mLaunchedFromPid, this.mLaunchedFromUid, this.mLaunchedFromPackage, this.mLaunchedFromFeature, this.mIntent, this.mResolvedType, this.mActivityInfo, this.mConfiguration, this.mResultTo, this.mResultWho, this.mRequestCode, this.mComponentSpecified, this.mRootVoiceInteraction, activityTaskManagerService.mTaskSupervisor, this.mOptions, this.mSourceRecord, this.mPersistentState, this.mTaskDescription, this.mCreateTime);
        }
    }

    public void setCompatDisplayInsets(CompatDisplayInsets compatDisplayInsets) {
        this.mCompatDisplayInsets = compatDisplayInsets;
    }

    public void setSizeCompatBounds(Rect sizeCompatBounds) {
        this.mSizeCompatBounds = sizeCompatBounds;
    }

    public void setInSizeCompatModeForBounds(boolean inSizeCompatModeForBounds) {
        this.mInSizeCompatModeForBounds = inSizeCompatModeForBounds;
    }

    public void setForceResizeable(boolean forceResizeable) {
        this.mForceResizeable = forceResizeable;
    }

    public int getRealPid() {
        return getPid();
    }

    public boolean inMultiWindow() {
        return ITranActivityRecord.Instance().inMultiWindow(this);
    }

    public boolean isAlreadyLaunchered() {
        return this.mAlreadyLaunchered;
    }

    public void setAlreadyLaunchered(boolean isAlreadyLaunchered) {
        this.mAlreadyLaunchered = isAlreadyLaunchered;
    }

    private void logByGriffinWhenFinishIfPossible() {
        ActivityInfo activityInfo;
        if (ITranGriffinFeature.Instance().isGriffinSupport()) {
            ActivityRecord nextActivity = (getDisplayContent() == null || getDisplayContent().getFocusedRootTask() == null) ? null : getDisplayContent().getFocusedRootTask().topRunningActivityLocked();
            if (ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
                String lastResumedPkg = "null";
                String lastResumedCls = "null";
                String nextPkg = "null";
                String nextCls = "null";
                int nextActivityType = 0;
                ActivityInfo activityInfo2 = this.info;
                if (activityInfo2 != null) {
                    lastResumedPkg = this.packageName;
                    lastResumedCls = activityInfo2.name;
                }
                if (nextActivity != null && (activityInfo = nextActivity.info) != null) {
                    nextPkg = nextActivity.packageName;
                    nextCls = activityInfo.name;
                    nextActivityType = nextActivity.getActivityType();
                }
                if (lastResumedPkg.equals(nextPkg) && lastResumedCls.equals(nextCls)) {
                    Slog.d("TranGriffin/AppSwitch", "Before app switch from:" + lastResumedCls + " to " + nextCls + ", pausing=true,type=" + nextActivityType + ",(same activity)");
                } else {
                    Slog.d("TranGriffin/AppSwitch", "Before app switch from:" + lastResumedCls + ",pkg=" + lastResumedPkg + " to " + nextCls + ",pkg=" + nextPkg + ", pausing=true,type=" + nextActivityType);
                    String recentPkg = this.mTaskSupervisor.mRecentTasks.getRecentsComponent().getPackageName();
                    String homePkg = (this.mAtmService.mHomeProcess == null || this.mAtmService.mHomeProcess.mInfo == null) ? "null" : this.mAtmService.mHomeProcess.mInfo.packageName;
                    if (nextPkg.equals(homePkg)) {
                        if (lastResumedPkg.equals(recentPkg)) {
                            Slog.d("TranGriffin/AppSwitch", "App switch from " + lastResumedPkg + " to " + nextPkg + " (Recent to Home)");
                        } else if (lastResumedPkg.equals(homePkg)) {
                            Slog.d("TranGriffin/AppSwitch", "App switch from " + lastResumedPkg + " to " + nextPkg + " (Home to Home)");
                        } else {
                            Slog.d("TranGriffin/AppSwitch", "App switch from " + lastResumedPkg + " to " + nextPkg + " (App to Home)");
                        }
                    } else if (nextPkg.equals(recentPkg)) {
                        if (lastResumedPkg.equals(homePkg)) {
                            Slog.d("TranGriffin/AppSwitch", "App switch from " + lastResumedPkg + " to " + nextPkg + " (Home to Recent)");
                        } else if (lastResumedPkg.equals(recentPkg)) {
                            Slog.d("TranGriffin/AppSwitch", "App switch from " + lastResumedPkg + " to " + nextPkg + " (Recent to Recent)");
                        } else {
                            Slog.d("TranGriffin/AppSwitch", "App switch from " + lastResumedPkg + " to " + nextPkg + " (App to Recent)");
                        }
                    } else if (lastResumedPkg.equals(homePkg)) {
                        Slog.d("TranGriffin/AppSwitch", "App switch from " + lastResumedPkg + " to " + nextPkg + " (Home to App)");
                    } else if (lastResumedPkg.equals(recentPkg)) {
                        Slog.d("TranGriffin/AppSwitch", "App switch from " + lastResumedPkg + " to " + nextPkg + " (Recent to App)");
                    } else {
                        Slog.d("TranGriffin/AppSwitch", "App switch from " + lastResumedPkg + " to " + nextPkg + " (App to App)");
                    }
                }
            }
            ITranActivityRecord.Instance().hookFinishIfPossible(this.info, nextActivity != null ? nextActivity.info : null, true, nextActivity != null ? nextActivity.getActivityType() : 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBeforeEnterPinnedMode() {
        if (!isGesturalNavMode()) {
            return;
        }
        if (this.mDeferShowForEnteringPinnedMode) {
            Slog.d(TAG, "PIP: is defering");
            return;
        }
        ActivityRecord resumedActivity = this.mRootWindowContainer.getTopResumedActivity();
        boolean needHide = false;
        if (resumedActivity != null) {
            if (resumedActivity.isActivityTypeHome()) {
                if (isVisible()) {
                    Slog.d(TAG, "PIP: home resumed and auto enter pip, hide a while: " + resumedActivity);
                    needHide = true;
                }
            } else {
                Slog.d(TAG, "PIP: resumed activity is not home activity, ignore");
                return;
            }
        }
        if (!isVisible() || needHide) {
            this.mDeferShowForEnteringPinnedMode = true;
            Slog.d(TAG, "PIP: set defer show for entering pip: " + this);
        }
        if (needHide) {
            setVisibility(false);
        }
    }

    boolean shouldDeferShowForEnteringPinnedMode(boolean visible) {
        if (this.mWaitForEnteringPinnedMode && visible && this.mDeferShowForEnteringPinnedMode) {
            if (!this.mNeedShowAfterEnterPinnedMode) {
                cancelDeferShowForPinnedMode(1000L);
            }
            this.mNeedShowAfterEnterPinnedMode = true;
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelDeferShowForPinnedMode() {
        cancelDeferShowForPinnedMode(-1L);
    }

    void cancelDeferShowForPinnedMode(long delay) {
        synchronized (this.mAtmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mCancelDeferShowForPinnedModeRunnable == null) {
                    this.mCancelDeferShowForPinnedModeRunnable = new Runnable() { // from class: com.android.server.wm.ActivityRecord$$ExternalSyntheticLambda14
                        @Override // java.lang.Runnable
                        public final void run() {
                            ActivityRecord.this.m7776x7743dde1();
                        }
                    };
                }
                this.mAtmService.mH.removeCallbacks(this.mCancelDeferShowForPinnedModeRunnable);
                if (delay != -1) {
                    this.mAtmService.mH.postDelayed(this.mCancelDeferShowForPinnedModeRunnable, delay);
                } else {
                    this.mCancelDeferShowForPinnedModeRunnable.run();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cancelDeferShowForPinnedMode$24$com-android-server-wm-ActivityRecord  reason: not valid java name */
    public /* synthetic */ void m7776x7743dde1() {
        synchronized (this.mAtmService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mNeedShowAfterEnterPinnedMode) {
                    this.mNeedShowAfterEnterPinnedMode = false;
                    this.mDeferShowForEnteringPinnedMode = false;
                    Slog.d(TAG, "PIP: show after enter pip: " + this);
                    if (this.mWaitForEnteringPinnedMode) {
                        Slog.d(TAG, "PIP: enter pip timeout: " + this);
                    }
                    setVisibility(true);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    boolean isGesturalNavMode() {
        return Settings.Secure.getInt(this.mAtmService.mContext.getContentResolver(), "navigation_mode", 0) == 2;
    }

    static boolean isInSplashScreenWhiteList(ComponentName componentName) {
        if (componentName == null || !ENABLE_SPLASHSCREEN_WHITELIST) {
            return false;
        }
        ArrayList<String> arrayList = SPLASHSCREEN_WHITELIST;
        if (!arrayList.contains(componentName.flattenToString()) && !arrayList.contains(componentName.flattenToShortString()) && !arrayList.contains(componentName.getPackageName())) {
            return false;
        }
        Slog.d("SplashScreen", "white list member:" + componentName);
        return true;
    }
}
