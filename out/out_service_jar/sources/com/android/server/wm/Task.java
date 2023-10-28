package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.app.IActivityController;
import android.app.PictureInPictureParams;
import android.app.TaskInfo;
import android.app.ThunderbackConfig;
import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.usb.gadget.V1_2.GadgetFunction;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.IVoiceInteractionSession;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import android.util.TypedXmlSerializer;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.InsetsState;
import android.view.RemoteAnimationAdapter;
import android.view.SurfaceControl;
import android.view.TaskTransitionSpec;
import android.view.WindowManager;
import android.window.ITaskOrganizer;
import android.window.PictureInPictureSurfaceTransaction;
import android.window.StartingWindowInfo;
import android.window.TaskSnapshot;
import android.window.WindowContainerToken;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.TriPredicate;
import com.android.internal.util.function.pooled.PooledConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.util.function.pooled.PooledPredicate;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.AppTimeTracker;
import com.android.server.uri.NeededUriGrants;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.SurfaceAnimator;
import com.mediatek.server.wm.WmsExt;
import com.transsion.foldable.TranFoldingScreenManager;
import com.transsion.hubcore.server.wm.ITranTask;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParserException;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class Task extends TaskFragment {
    private static final long ADJACENT_INVDLID_TIME = -1;
    private static final String ATTR_AFFINITY = "affinity";
    private static final String ATTR_ASKEDCOMPATMODE = "asked_compat_mode";
    private static final String ATTR_AUTOREMOVERECENTS = "auto_remove_recents";
    private static final String ATTR_CALLING_FEATURE_ID = "calling_feature_id";
    private static final String ATTR_CALLING_PACKAGE = "calling_package";
    private static final String ATTR_CALLING_UID = "calling_uid";
    private static final String ATTR_EFFECTIVE_UID = "effective_uid";
    private static final String ATTR_KEEP_ADJACENTREASON = "keep_adjacent_reason";
    private static final String ATTR_LASTDESCRIPTION = "last_description";
    private static final String ATTR_LASTTIMEMOVED = "last_time_moved";
    private static final String ATTR_LAST_SNAPSHOT_BUFFER_SIZE = "last_snapshot_buffer_size";
    private static final String ATTR_LAST_SNAPSHOT_CONTENT_INSETS = "last_snapshot_content_insets";
    private static final String ATTR_LAST_SNAPSHOT_TASK_SIZE = "last_snapshot_task_size";
    private static final String ATTR_MIN_HEIGHT = "min_height";
    private static final String ATTR_MIN_WIDTH = "min_width";
    private static final String ATTR_NEVERRELINQUISH = "never_relinquish_identity";
    private static final String ATTR_NEXT_AFFILIATION = "next_affiliation";
    private static final String ATTR_NON_FULLSCREEN_BOUNDS = "non_fullscreen_bounds";
    private static final String ATTR_ORIGACTIVITY = "orig_activity";
    private static final String ATTR_PERSIST_TASK_VERSION = "persist_task_version";
    private static final String ATTR_PREV_AFFILIATION = "prev_affiliation";
    private static final String ATTR_REALACTIVITY = "real_activity";
    private static final String ATTR_REALACTIVITY_SUSPENDED = "real_activity_suspended";
    private static final String ATTR_RESIZE_MODE = "resize_mode";
    private static final String ATTR_ROOTHASRESET = "root_has_reset";
    private static final String ATTR_ROOT_AFFINITY = "root_affinity";
    private static final String ATTR_SUPPORTS_PICTURE_IN_PICTURE = "supports_picture_in_picture";
    private static final String ATTR_TASKID = "task_id";
    @Deprecated
    private static final String ATTR_TASKTYPE = "task_type";
    private static final String ATTR_TASK_AFFILIATION = "task_affiliation";
    private static final String ATTR_USERID = "user_id";
    private static final String ATTR_USER_SETUP_COMPLETE = "user_setup_complete";
    private static final String ATTR_WINDOW_LAYOUT_AFFINITY = "window_layout_affinity";
    private static final int DEFAULT_MIN_TASK_SIZE_DP = 220;
    private static final String ENTER_MULTIWINDOW_ACTION = "android.intent.action.enter.multiwindow";
    private static final String EXIT_MULTIWINDOW_ACTION = "android.intent.action.exit.multiwindow";
    static final int FLAG_FORCE_HIDDEN_FOR_PINNED_TASK = 1;
    static final int FLAG_FORCE_HIDDEN_FOR_TASK_ORG = 2;
    static final int LAYER_RANK_INVISIBLE = -1;
    static final int PERSIST_TASK_VERSION = 1;
    static final int REPARENT_KEEP_ROOT_TASK_AT_FRONT = 1;
    static final int REPARENT_LEAVE_ROOT_TASK_IN_PLACE = 2;
    static final int REPARENT_MOVE_ROOT_TASK_TO_FRONT = 0;
    private static final String SOCIAL_TURBO_NAME = "com.transsion.videocallenhancer";
    private static final String TAG_ACTIVITY = "activity";
    private static final String TAG_AFFINITYINTENT = "affinity_intent";
    private static final String TAG_INTENT = "intent";
    private static final long TRANSLUCENT_CONVERSION_TIMEOUT = 2000;
    private static final int TRANSLUCENT_TIMEOUT_MSG = 101;
    private static final String WHATSAPP_NAME = "com.whatsapp";
    private static Exception sTmpException;
    String affinity;
    Intent affinityIntent;
    boolean askedCompatMode;
    boolean autoRemoveRecents;
    int effectiveUid;
    boolean inRecents;
    Intent intent;
    boolean isAvailable;
    boolean isPersistable;
    long lastActiveTime;
    CharSequence lastDescription;
    Task mAdjacentTask;
    int mAffiliatedTaskId;
    private final AnimatingActivityRegistry mAnimatingActivityRegistry;
    boolean mBackGestureStarted;
    String mCallingFeatureId;
    String mCallingPackage;
    int mCallingUid;
    private boolean mCanAffectSystemUiFlags;
    ActivityRecord mChildPipActivity;
    boolean mConfigWillChange;
    int mCurrentUser;
    private boolean mDeferTaskAppear;
    private int mDragResizeMode;
    private boolean mDragResizing;
    private final FindRootHelper mFindRootHelper;
    private int mForceHiddenFlags;
    private boolean mForceShowForAllUsers;
    private final Handler mHandler;
    private boolean mHasBeenVisible;
    boolean mInRemoveTask;
    boolean mInResumeTopActivity;
    boolean mIsEffectivelySystemApp;
    private boolean mIsPkgInActivityEmbedding;
    int mKeepAdjacentReason;
    long mLastAdjacentActiveTime;
    Rect mLastNonFullscreenBounds;
    SurfaceControl mLastRecentsAnimationOverlay;
    PictureInPictureSurfaceTransaction mLastRecentsAnimationTransaction;
    int mLastReportedRequestedOrientation;
    private int mLastRotationDisplayId;
    boolean mLastSurfaceShowing;
    final ActivityManager.RecentTaskInfo.PersistedTaskSnapshotData mLastTaskSnapshotData;
    long mLastTimeMoved;
    IBinder mLaunchCookie;
    boolean mLauncherFromRecent;
    int mLayerRank;
    int mLockTaskAuth;
    int mLockTaskUid;
    private boolean mNeverRelinquishIdentity;
    Task mNextAffiliate;
    int mNextAffiliateTaskId;
    Task mPrevAffiliate;
    int mPrevAffiliateTaskId;
    int mPrevDisplayId;
    boolean mRemoveWithTaskOrganizer;
    private boolean mRemoving;
    public boolean mReparentFromMultiWindow;
    int mResizeMode;
    final TaskActivitiesReport mReuseActivitiesReport;
    private boolean mReuseTask;
    private WindowProcessController mRootProcess;
    private int mRotation;
    private float mShadowRadius;
    StartingData mSharedStartingData;
    boolean mShouldSkipHookToMax;
    boolean mSupportsPictureInPicture;
    SurfaceControl mTaskAnimationLeash;
    boolean mTaskAppearedSent;
    private ActivityManager.TaskDescription mTaskDescription;
    final int mTaskId;
    private float mTaskMinAspectRatioForUser;
    ITaskOrganizer mTaskOrganizer;
    private Configuration mTmpConfig;
    private final Rect mTmpDimBoundsRect;
    private Rect mTmpRect;
    private Rect mTmpRect2;
    ActivityRecord mTranslucentActivityWaiting;
    ArrayList<ActivityRecord> mUndrawnActivitiesBelowTopTranslucent;
    int mUserId;
    boolean mUserSetupComplete;
    String mWindowLayoutAffinity;
    final WindowManagerService mWindowManager;
    int maxRecents;
    ComponentName origActivity;
    ComponentName realActivity;
    boolean realActivitySuspended;
    String rootAffinity;
    boolean rootWasReset;
    String stringName;
    IVoiceInteractor voiceInteractor;
    IVoiceInteractionSession voiceSession;
    private static final String TAG = "ActivityTaskManager";
    private static final String TAG_RECENTS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_RECENTS;
    static final String TAG_TASKS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_TASKS;
    static final String TAG_CLEANUP = TAG + ActivityTaskManagerDebugConfig.POSTFIX_CLEANUP;
    private static final String TAG_SWITCH = TAG + ActivityTaskManagerDebugConfig.POSTFIX_SWITCH;
    private static final String TAG_TRANSITION = TAG + ActivityTaskManagerDebugConfig.POSTFIX_TRANSITION;
    private static final String TAG_USER_LEAVING = TAG + ActivityTaskManagerDebugConfig.POSTFIX_USER_LEAVING;
    static final String TAG_VISIBILITY = TAG + ActivityTaskManagerDebugConfig.POSTFIX_VISIBILITY;
    private static final Rect sTmpBounds = new Rect();
    private static final ResetTargetTaskHelper sResetTargetTaskHelper = new ResetTargetTaskHelper();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface ReparentMoveRootTaskMode {
    }

    /* loaded from: classes2.dex */
    private class ActivityTaskHandler extends Handler {
        ActivityTaskHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 101:
                    synchronized (Task.this.mAtmService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            Task.this.notifyActivityDrawnLocked(null);
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class FindRootHelper implements Predicate<ActivityRecord> {
        private boolean mIgnoreRelinquishIdentity;
        private ActivityRecord mRoot;
        private boolean mSetToBottomIfNone;

        private FindRootHelper() {
        }

        ActivityRecord findRoot(boolean ignoreRelinquishIdentity, boolean setToBottomIfNone) {
            this.mIgnoreRelinquishIdentity = ignoreRelinquishIdentity;
            this.mSetToBottomIfNone = setToBottomIfNone;
            Task.this.forAllActivities((Predicate<ActivityRecord>) this, false);
            ActivityRecord root = this.mRoot;
            this.mRoot = null;
            return root;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(ActivityRecord r) {
            if (this.mRoot == null && this.mSetToBottomIfNone) {
                this.mRoot = r;
            }
            if (r.finishing) {
                return false;
            }
            ActivityRecord activityRecord = this.mRoot;
            if (activityRecord == null || activityRecord.finishing) {
                this.mRoot = r;
            }
            int uid = this.mRoot == r ? Task.this.effectiveUid : r.info.applicationInfo.uid;
            if (!this.mIgnoreRelinquishIdentity && (this.mRoot.info.flags & 4096) != 0) {
                if (this.mRoot.info.applicationInfo.uid != 1000 && !this.mRoot.info.applicationInfo.isSystemApp() && this.mRoot.info.applicationInfo.uid != uid) {
                    return true;
                }
                this.mRoot = r;
                return false;
            }
            return true;
        }
    }

    private Task(ActivityTaskManagerService atmService, int _taskId, Intent _intent, Intent _affinityIntent, String _affinity, String _rootAffinity, ComponentName _realActivity, ComponentName _origActivity, boolean _rootWasReset, boolean _autoRemoveRecents, boolean _askedCompatMode, int _userId, int _effectiveUid, String _lastDescription, long lastTimeMoved, boolean neverRelinquishIdentity, ActivityManager.TaskDescription _lastTaskDescription, ActivityManager.RecentTaskInfo.PersistedTaskSnapshotData _lastSnapshotData, int taskAffiliation, int prevTaskId, int nextTaskId, int callingUid, String callingPackage, String callingFeatureId, int resizeMode, boolean supportsPictureInPicture, boolean _realActivitySuspended, boolean userSetupComplete, int minWidth, int minHeight, ActivityInfo info, IVoiceInteractionSession _voiceSession, IVoiceInteractor _voiceInteractor, boolean _createdByOrganizer, IBinder _launchCookie, boolean _deferTaskAppear, boolean _removeWithTaskOrganizer) {
        super(atmService, null, _createdByOrganizer, false);
        ActivityManager.TaskDescription taskDescription;
        ActivityManager.RecentTaskInfo.PersistedTaskSnapshotData persistedTaskSnapshotData;
        this.mShadowRadius = 0.0f;
        this.mTranslucentActivityWaiting = null;
        this.mUndrawnActivitiesBelowTopTranslucent = new ArrayList<>();
        this.mTaskMinAspectRatioForUser = -1.0f;
        this.mIsPkgInActivityEmbedding = false;
        this.mInResumeTopActivity = false;
        this.mLockTaskAuth = 1;
        this.mLockTaskUid = -1;
        this.isPersistable = false;
        this.mNeverRelinquishIdentity = true;
        this.mReuseTask = false;
        this.mPrevAffiliateTaskId = -1;
        this.mNextAffiliateTaskId = -1;
        this.mKeepAdjacentReason = 0;
        this.mLastAdjacentActiveTime = -1L;
        this.mLastNonFullscreenBounds = null;
        this.mLayerRank = -1;
        this.mTmpConfig = new Configuration();
        this.mReuseActivitiesReport = new TaskActivitiesReport();
        this.mPrevDisplayId = -1;
        this.mLastRotationDisplayId = -1;
        this.mLastReportedRequestedOrientation = -1;
        this.mTmpRect = new Rect();
        this.mTmpRect2 = new Rect();
        this.mTmpDimBoundsRect = new Rect();
        this.mCanAffectSystemUiFlags = true;
        this.mForceHiddenFlags = 0;
        this.mAnimatingActivityRegistry = new AnimatingActivityRegistry();
        this.mFindRootHelper = new FindRootHelper();
        this.mLastSurfaceShowing = true;
        this.mBackGestureStarted = false;
        this.mTaskId = _taskId;
        this.mUserId = _userId;
        this.mResizeMode = resizeMode;
        this.mSupportsPictureInPicture = supportsPictureInPicture;
        if (_lastTaskDescription != null) {
            taskDescription = _lastTaskDescription;
        } else {
            taskDescription = new ActivityManager.TaskDescription();
        }
        this.mTaskDescription = taskDescription;
        if (_lastSnapshotData != null) {
            persistedTaskSnapshotData = _lastSnapshotData;
        } else {
            persistedTaskSnapshotData = new ActivityManager.RecentTaskInfo.PersistedTaskSnapshotData();
        }
        this.mLastTaskSnapshotData = persistedTaskSnapshotData;
        setOrientation(-2);
        this.affinityIntent = _affinityIntent;
        this.affinity = _affinity;
        this.rootAffinity = _rootAffinity;
        this.voiceSession = _voiceSession;
        this.voiceInteractor = _voiceInteractor;
        this.realActivity = _realActivity;
        this.realActivitySuspended = _realActivitySuspended;
        this.origActivity = _origActivity;
        this.rootWasReset = _rootWasReset;
        this.isAvailable = true;
        this.autoRemoveRecents = _autoRemoveRecents;
        this.askedCompatMode = _askedCompatMode;
        this.mUserSetupComplete = userSetupComplete;
        this.effectiveUid = _effectiveUid;
        touchActiveTime();
        this.lastDescription = _lastDescription;
        this.mLastTimeMoved = lastTimeMoved;
        this.mNeverRelinquishIdentity = neverRelinquishIdentity;
        this.mAffiliatedTaskId = taskAffiliation;
        this.mPrevAffiliateTaskId = prevTaskId;
        this.mNextAffiliateTaskId = nextTaskId;
        this.mCallingUid = callingUid;
        this.mCallingPackage = callingPackage;
        this.mCallingFeatureId = callingFeatureId;
        this.mResizeMode = resizeMode;
        if (info != null) {
            setIntent(_intent, info);
            setMinDimensions(info);
        } else {
            this.intent = _intent;
            this.mMinWidth = minWidth;
            this.mMinHeight = minHeight;
        }
        this.mAtmService.getTaskChangeNotificationController().notifyTaskCreated(_taskId, this.realActivity);
        this.mHandler = new ActivityTaskHandler(this.mTaskSupervisor.mLooper);
        this.mCurrentUser = this.mAtmService.mAmInternal.getCurrentUserId();
        this.mWindowManager = this.mAtmService.mWindowManager;
        this.mLaunchCookie = _launchCookie;
        this.mDeferTaskAppear = _deferTaskAppear;
        this.mRemoveWithTaskOrganizer = _removeWithTaskOrganizer;
        EventLogTags.writeWmTaskCreated(_taskId, isRootTask() ? -1 : getRootTaskId());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Task fromWindowContainerToken(WindowContainerToken token) {
        if (token == null) {
            return null;
        }
        return fromBinder(token.asBinder()).asTask();
    }

    Task reuseAsLeafTask(IVoiceInteractionSession _voiceSession, IVoiceInteractor _voiceInteractor, Intent intent, ActivityInfo info, ActivityRecord activity) {
        this.voiceSession = _voiceSession;
        this.voiceInteractor = _voiceInteractor;
        setIntent(activity, intent, info);
        setMinDimensions(info);
        this.mAtmService.getTaskChangeNotificationController().notifyTaskCreated(this.mTaskId, this.realActivity);
        return this;
    }

    private void cleanUpResourcesForDestroy(WindowContainer<?> oldParent) {
        if (hasChild()) {
            return;
        }
        saveLaunchingStateIfNeeded(oldParent.getDisplayContent());
        IVoiceInteractionSession iVoiceInteractionSession = this.voiceSession;
        boolean isVoiceSession = iVoiceInteractionSession != null;
        if (isVoiceSession) {
            try {
                iVoiceInteractionSession.taskFinished(this.intent, this.mTaskId);
            } catch (RemoteException e) {
            }
        }
        if (autoRemoveFromRecents(oldParent.asTaskFragment()) || isVoiceSession) {
            this.mTaskSupervisor.mRecentTasks.remove(this);
        }
        removeIfPossible("cleanUpResourcesForDestroy");
    }

    @Override // com.android.server.wm.WindowContainer
    void removeIfPossible() {
        removeIfPossible("removeTaskIfPossible");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeIfPossible(String reason) {
        this.mAtmService.getLockTaskController().clearLockedTask(this);
        if (shouldDeferRemoval()) {
            if (WindowManagerDebugConfig.DEBUG_ROOT_TASK) {
                Slog.i(TAG, "removeTask:" + reason + " deferring removing taskId=" + this.mTaskId);
                return;
            }
            return;
        }
        removeImmediately(reason);
        if (ITranTask.Instance().isAdjacentTaskEnable() && getAdjacentTask() != null) {
            ITranTask.Instance().hookResetAdjacentTaskForTask(this);
        }
        if (isLeafTask()) {
            this.mAtmService.getTaskChangeNotificationController().notifyTaskRemoved(this.mTaskId);
            TaskDisplayArea taskDisplayArea = getDisplayArea();
            if (taskDisplayArea != null) {
                taskDisplayArea.onLeafTaskRemoved(this.mTaskId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setResizeMode(int resizeMode) {
        if (this.mResizeMode == resizeMode) {
            return;
        }
        this.mResizeMode = resizeMode;
        this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
        this.mRootWindowContainer.resumeFocusedTasksTopActivities();
        updateTaskDescription();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resize(Rect bounds, int resizeMode, boolean preserveWindow) {
        ActivityRecord r;
        this.mAtmService.deferWindowLayout();
        boolean forced = (resizeMode & 2) != 0;
        try {
            if (getParent() == null) {
                setBounds(bounds);
                if (!inFreeformWindowingMode()) {
                    this.mTaskSupervisor.restoreRecentTaskLocked(this, null, false);
                }
                return true;
            } else if (!canResizeToBounds(bounds)) {
                throw new IllegalArgumentException("resizeTask: Can not resize task=" + this + " to bounds=" + bounds + " resizeMode=" + this.mResizeMode);
            } else {
                Trace.traceBegin(32L, "resizeTask_" + this.mTaskId);
                boolean updatedConfig = false;
                this.mTmpConfig.setTo(getResolvedOverrideConfiguration());
                if (setBounds(bounds) != 0) {
                    updatedConfig = true ^ this.mTmpConfig.equals(getResolvedOverrideConfiguration());
                }
                boolean kept = true;
                if (updatedConfig && (r = topRunningActivityLocked()) != null) {
                    kept = r.ensureActivityConfiguration(0, preserveWindow);
                    this.mRootWindowContainer.ensureActivitiesVisible(r, 0, preserveWindow);
                    if (!kept) {
                        this.mRootWindowContainer.resumeFocusedTasksTopActivities();
                    }
                }
                resize(kept, forced);
                saveLaunchingStateIfNeeded();
                Trace.traceEnd(32L);
                return kept;
            }
        } finally {
            this.mAtmService.continueWindowLayout();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean reparent(Task preferredRootTask, boolean toTop, int moveRootTaskMode, boolean animate, boolean deferResume, String reason) {
        return reparent(preferredRootTask, toTop ? Integer.MAX_VALUE : 0, moveRootTaskMode, animate, deferResume, true, reason);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1014=5] */
    /* JADX WARN: Removed duplicated region for block: B:43:0x008f  */
    /* JADX WARN: Removed duplicated region for block: B:49:0x009c  */
    /* JADX WARN: Removed duplicated region for block: B:58:0x00b7 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:66:0x00d8 A[Catch: all -> 0x00ae, TRY_LEAVE, TryCatch #3 {all -> 0x00ae, blocks: (B:54:0x00aa, B:59:0x00b9, B:61:0x00c6, B:63:0x00ce, B:66:0x00d8), top: B:94:0x00aa }] */
    /* JADX WARN: Removed duplicated region for block: B:69:0x00e7  */
    /* JADX WARN: Removed duplicated region for block: B:71:0x00f0  */
    /* JADX WARN: Removed duplicated region for block: B:76:0x0100  */
    /* JADX WARN: Removed duplicated region for block: B:79:0x0114 A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:80:0x0116  */
    /* JADX WARN: Removed duplicated region for block: B:90:0x0079 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:94:0x00aa A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    boolean reparent(Task preferredRootTask, int position, int moveRootTaskMode, boolean animate, boolean deferResume, boolean schedulePictureInPictureModeChange, String reason) {
        boolean wasFocused;
        boolean wasFront;
        RootWindowContainer root;
        boolean z;
        boolean z2;
        boolean z3;
        ActivityTaskSupervisor supervisor = this.mTaskSupervisor;
        RootWindowContainer root2 = this.mRootWindowContainer;
        WindowManagerService windowManager = this.mAtmService.mWindowManager;
        Task sourceRootTask = getRootTask();
        Task toRootTask = supervisor.getReparentTargetRootTask(this, preferredRootTask, position == Integer.MAX_VALUE);
        if (toRootTask == sourceRootTask || !canBeLaunchedOnDisplay(toRootTask.getDisplayId())) {
            return false;
        }
        int toRootTaskWindowingMode = toRootTask.getWindowingMode();
        ActivityRecord topActivity = getTopNonFinishingActivity();
        boolean mightReplaceWindow = topActivity != null && replaceWindowsOnTaskMove(getWindowingMode(), toRootTaskWindowingMode);
        if (mightReplaceWindow) {
            windowManager.setWillReplaceWindow(topActivity.token, animate);
        }
        this.mAtmService.deferWindowLayout();
        try {
            ActivityRecord r = topRunningActivityLocked();
            try {
                if (r != null) {
                    if (root2.isTopDisplayFocusedRootTask(sourceRootTask)) {
                        try {
                            if (topRunningActivityLocked() == r) {
                                wasFocused = true;
                                if (r != null) {
                                    try {
                                        if (sourceRootTask.isTopRootTaskInDisplayArea()) {
                                            if (sourceRootTask.topRunningActivity() == r) {
                                                wasFront = true;
                                                if (moveRootTaskMode == 0) {
                                                    root = root2;
                                                    z = true;
                                                    if (moveRootTaskMode != 1 || (!wasFocused && !wasFront)) {
                                                        z2 = false;
                                                        boolean moveRootTaskToFront = z2;
                                                        reparent(toRootTask, position, moveRootTaskToFront, reason);
                                                        if (schedulePictureInPictureModeChange) {
                                                            try {
                                                                supervisor.scheduleUpdatePictureInPictureModeIfNeeded(this, sourceRootTask);
                                                            } catch (Throwable th) {
                                                                th = th;
                                                                this.mAtmService.continueWindowLayout();
                                                                throw th;
                                                            }
                                                        }
                                                        if (r == null && moveRootTaskToFront) {
                                                            toRootTask.moveToFront(reason);
                                                            if (r.isState(ActivityRecord.State.RESUMED) && r == this.mRootWindowContainer.getTopResumedActivity()) {
                                                                this.mAtmService.setResumedActivityUncheckLocked(r, reason);
                                                            }
                                                        }
                                                        if (!animate) {
                                                            this.mTaskSupervisor.mNoAnimActivities.add(topActivity);
                                                        }
                                                        this.mAtmService.continueWindowLayout();
                                                        if (mightReplaceWindow) {
                                                            windowManager.scheduleClearWillReplaceWindows(topActivity.token, !true);
                                                        }
                                                        if (deferResume) {
                                                            z3 = false;
                                                        } else {
                                                            RootWindowContainer root3 = root;
                                                            z3 = false;
                                                            root3.ensureActivitiesVisible(null, 0, !mightReplaceWindow);
                                                            root3.resumeFocusedTasksTopActivities();
                                                        }
                                                        supervisor.handleNonResizableTaskIfNeeded(this, preferredRootTask.getWindowingMode(), this.mRootWindowContainer.getDefaultTaskDisplayArea(), toRootTask);
                                                        if (preferredRootTask == toRootTask) {
                                                            return true;
                                                        }
                                                        return z3;
                                                    }
                                                } else {
                                                    root = root2;
                                                    z = true;
                                                }
                                                z2 = z;
                                                boolean moveRootTaskToFront2 = z2;
                                                reparent(toRootTask, position, moveRootTaskToFront2, reason);
                                                if (schedulePictureInPictureModeChange) {
                                                }
                                                if (r == null) {
                                                }
                                                if (!animate) {
                                                }
                                                this.mAtmService.continueWindowLayout();
                                                if (mightReplaceWindow) {
                                                }
                                                if (deferResume) {
                                                }
                                                supervisor.handleNonResizableTaskIfNeeded(this, preferredRootTask.getWindowingMode(), this.mRootWindowContainer.getDefaultTaskDisplayArea(), toRootTask);
                                                if (preferredRootTask == toRootTask) {
                                                }
                                            }
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        this.mAtmService.continueWindowLayout();
                                        throw th;
                                    }
                                }
                                wasFront = false;
                                if (moveRootTaskMode == 0) {
                                }
                                z2 = z;
                                boolean moveRootTaskToFront22 = z2;
                                reparent(toRootTask, position, moveRootTaskToFront22, reason);
                                if (schedulePictureInPictureModeChange) {
                                }
                                if (r == null) {
                                }
                                if (!animate) {
                                }
                                this.mAtmService.continueWindowLayout();
                                if (mightReplaceWindow) {
                                }
                                if (deferResume) {
                                }
                                supervisor.handleNonResizableTaskIfNeeded(this, preferredRootTask.getWindowingMode(), this.mRootWindowContainer.getDefaultTaskDisplayArea(), toRootTask);
                                if (preferredRootTask == toRootTask) {
                                }
                            }
                            wasFocused = false;
                            if (r != null) {
                            }
                            wasFront = false;
                            if (moveRootTaskMode == 0) {
                            }
                            z2 = z;
                            boolean moveRootTaskToFront222 = z2;
                            reparent(toRootTask, position, moveRootTaskToFront222, reason);
                            if (schedulePictureInPictureModeChange) {
                            }
                            if (r == null) {
                            }
                            if (!animate) {
                            }
                            this.mAtmService.continueWindowLayout();
                            if (mightReplaceWindow) {
                            }
                            if (deferResume) {
                            }
                            supervisor.handleNonResizableTaskIfNeeded(this, preferredRootTask.getWindowingMode(), this.mRootWindowContainer.getDefaultTaskDisplayArea(), toRootTask);
                            if (preferredRootTask == toRootTask) {
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            this.mAtmService.continueWindowLayout();
                            throw th;
                        }
                    }
                }
                reparent(toRootTask, position, moveRootTaskToFront222, reason);
                if (schedulePictureInPictureModeChange) {
                }
                if (r == null) {
                }
                if (!animate) {
                }
                this.mAtmService.continueWindowLayout();
                if (mightReplaceWindow) {
                }
                if (deferResume) {
                }
                supervisor.handleNonResizableTaskIfNeeded(this, preferredRootTask.getWindowingMode(), this.mRootWindowContainer.getDefaultTaskDisplayArea(), toRootTask);
                if (preferredRootTask == toRootTask) {
                }
            } catch (Throwable th4) {
                th = th4;
            }
            wasFocused = false;
            if (r != null) {
            }
            wasFront = false;
            if (moveRootTaskMode == 0) {
            }
            z2 = z;
            boolean moveRootTaskToFront2222 = z2;
        } catch (Throwable th5) {
            th = th5;
        }
    }

    private static boolean replaceWindowsOnTaskMove(int sourceWindowingMode, int targetWindowingMode) {
        return sourceWindowingMode == 5 || targetWindowingMode == 5;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void touchActiveTime() {
        this.lastActiveTime = SystemClock.elapsedRealtime();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getInactiveDuration() {
        return SystemClock.elapsedRealtime() - this.lastActiveTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIntent(ActivityRecord r) {
        setIntent(r, null, null);
    }

    void setIntent(ActivityRecord r, Intent intent, ActivityInfo info) {
        boolean updateIdentity = false;
        if (this.intent == null) {
            updateIdentity = true;
        } else if (!this.mNeverRelinquishIdentity) {
            ActivityInfo activityInfo = info != null ? info : r.info;
            int i = this.effectiveUid;
            updateIdentity = i == 1000 || this.mIsEffectivelySystemApp || i == activityInfo.applicationInfo.uid;
        }
        if (updateIdentity) {
            this.mCallingUid = r.launchedFromUid;
            this.mCallingPackage = r.launchedFromPackage;
            this.mCallingFeatureId = r.launchedFromFeatureId;
            setIntent(intent != null ? intent : r.intent, info != null ? info : r.info);
        }
        setLockTaskAuth(r);
    }

    private void setIntent(Intent _intent, ActivityInfo info) {
        if (isLeafTask()) {
            this.mNeverRelinquishIdentity = (info.flags & 4096) == 0;
            String str = info.taskAffinity;
            this.affinity = str;
            if (this.intent == null) {
                this.rootAffinity = str;
            }
            this.effectiveUid = info.applicationInfo.uid;
            this.mIsEffectivelySystemApp = info.applicationInfo.isSystemApp();
            this.stringName = null;
            if (info.targetActivity == null) {
                if (_intent != null && (_intent.getSelector() != null || _intent.getSourceBounds() != null)) {
                    _intent = new Intent(_intent);
                    _intent.setSelector(null);
                    _intent.setSourceBounds(null);
                }
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    String protoLogParam0 = String.valueOf(this);
                    String protoLogParam1 = String.valueOf(_intent);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_TASKS, -2054442123, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
                }
                this.intent = _intent;
                this.realActivity = _intent != null ? _intent.getComponent() : null;
                this.origActivity = null;
            } else {
                ComponentName targetComponent = new ComponentName(info.packageName, info.targetActivity);
                if (_intent != null) {
                    Intent targetIntent = new Intent(_intent);
                    targetIntent.setSelector(null);
                    targetIntent.setSourceBounds(null);
                    if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                        String protoLogParam02 = String.valueOf(this);
                        String protoLogParam12 = String.valueOf(targetIntent);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_TASKS, 674932310, 0, (String) null, new Object[]{protoLogParam02, protoLogParam12});
                    }
                    this.intent = targetIntent;
                    this.realActivity = targetComponent;
                    this.origActivity = _intent.getComponent();
                } else {
                    this.intent = null;
                    this.realActivity = targetComponent;
                    this.origActivity = new ComponentName(info.packageName, info.name);
                }
            }
            if (this.realActivity != null) {
                updateTaskMinAspectRatioForSetting();
            }
            this.mWindowLayoutAffinity = info.windowLayout != null ? info.windowLayout.windowLayoutAffinity : null;
            Intent intent = this.intent;
            int intentFlags = intent == null ? 0 : intent.getFlags();
            if ((2097152 & intentFlags) != 0) {
                this.rootWasReset = true;
            }
            this.mUserId = UserHandle.getUserId(info.applicationInfo.uid);
            this.mUserSetupComplete = Settings.Secure.getIntForUser(this.mAtmService.mContext.getContentResolver(), ATTR_USER_SETUP_COMPLETE, 0, this.mUserId) != 0;
            if ((info.flags & 8192) != 0) {
                this.autoRemoveRecents = true;
            } else if ((532480 & intentFlags) == 524288) {
                if (info.documentLaunchMode != 0) {
                    this.autoRemoveRecents = false;
                } else {
                    this.autoRemoveRecents = true;
                }
            } else {
                this.autoRemoveRecents = false;
            }
            if (this.mResizeMode != info.resizeMode) {
                this.mResizeMode = info.resizeMode;
                updateTaskDescription();
            }
            this.mSupportsPictureInPicture = info.supportsPictureInPicture();
            if (this.inRecents) {
                this.mTaskSupervisor.mRecentTasks.remove(this);
                this.mTaskSupervisor.mRecentTasks.add(this);
            }
        }
    }

    void setMinDimensions(ActivityInfo info) {
        if (info != null && info.windowLayout != null) {
            this.mMinWidth = info.windowLayout.minWidth;
            this.mMinHeight = info.windowLayout.minHeight;
            return;
        }
        this.mMinWidth = -1;
        this.mMinHeight = -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSameIntentFilter(ActivityRecord r) {
        Intent intent;
        Intent intent2 = new Intent(r.intent);
        if (Objects.equals(this.realActivity, r.mActivityComponent) && (intent = this.intent) != null) {
            intent2.setComponent(intent.getComponent());
            final AtomicInteger taskFragmentCount = new AtomicInteger();
            forAllLeafTaskFragments(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda9
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    Task.lambda$isSameIntentFilter$0(taskFragmentCount, (TaskFragment) obj);
                }
            }, true);
            if (taskFragmentCount.get() >= 2) {
                Slog.d(TAG, "Task include two embedding taskFragment, r=" + r);
                return true;
            }
        }
        return intent2.filterEquals(this.intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$isSameIntentFilter$0(AtomicInteger taskFragmentCount, TaskFragment f) {
        if (f.isEmbedded() && f.isEmbedding()) {
            taskFragmentCount.getAndIncrement();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean returnsToHomeRootTask() {
        if (getConfiguration().windowConfiguration.isThunderbackWindow() || inMultiWindowMode() || !hasChild()) {
            return false;
        }
        if (this.intent != null) {
            Task task = getDisplayArea() != null ? getDisplayArea().getRootHomeTask() : null;
            boolean isLockTaskModeViolation = task != null && this.mAtmService.getLockTaskController().isLockTaskModeViolation(task);
            return (this.intent.getFlags() & 268451840) == 268451840 && !isLockTaskModeViolation;
        }
        Task bottomTask = getBottomMostTask();
        return bottomTask != this && bottomTask.returnsToHomeRootTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPrevAffiliate(Task prevAffiliate) {
        this.mPrevAffiliate = prevAffiliate;
        this.mPrevAffiliateTaskId = prevAffiliate == null ? -1 : prevAffiliate.mTaskId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setNextAffiliate(Task nextAffiliate) {
        this.mNextAffiliate = nextAffiliate;
        this.mNextAffiliateTaskId = nextAffiliate == null ? -1 : nextAffiliate.mTaskId;
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    void onParentChanged(ConfigurationContainer rawNewParent, ConfigurationContainer rawOldParent) {
        WindowContainer<?> newParent = (WindowContainer) rawNewParent;
        WindowContainer<?> oldParent = (WindowContainer) rawOldParent;
        DisplayContent display = newParent != null ? newParent.getDisplayContent() : null;
        DisplayContent oldDisplay = oldParent != null ? oldParent.getDisplayContent() : null;
        this.mPrevDisplayId = oldDisplay != null ? oldDisplay.mDisplayId : -1;
        if (oldParent != null && newParent == null) {
            cleanUpResourcesForDestroy(oldParent);
        }
        if (display != null) {
            getConfiguration().windowConfiguration.setRotation(display.getWindowConfiguration().getRotation());
        }
        super.onParentChanged(newParent, oldParent);
        updateTaskOrganizerState();
        if (getParent() == null && this.mDisplayContent != null) {
            this.mDisplayContent = null;
            this.mWmService.mWindowPlacerLocked.requestTraversal();
        }
        if (oldParent != null) {
            Task oldParentTask = oldParent.asTask();
            if (oldParentTask != null) {
                Consumer<ActivityRecord> obtainConsumer = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda41
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        ((Task) obj).cleanUpActivityReferences((ActivityRecord) obj2);
                    }
                }, oldParentTask, PooledLambda.__(ActivityRecord.class));
                forAllActivities(obtainConsumer);
                obtainConsumer.recycle();
            }
            if (oldParent.inPinnedWindowingMode() && (newParent == null || !newParent.inPinnedWindowingMode())) {
                this.mRootWindowContainer.notifyActivityPipModeChanged(this, null);
            }
        }
        if (newParent != null) {
            if (!this.mCreatedByOrganizer && !canBeOrganized()) {
                getSyncTransaction().show(this.mSurfaceControl);
            }
            IVoiceInteractionSession iVoiceInteractionSession = this.voiceSession;
            if (iVoiceInteractionSession != null) {
                try {
                    iVoiceInteractionSession.taskStarted(this.intent, this.mTaskId);
                } catch (RemoteException e) {
                }
            }
        }
        if (oldParent == null && newParent != null) {
            updateOverrideConfigurationFromLaunchBounds();
        }
        adjustBoundsForDisplayChangeIfNeeded(getDisplayContent());
        this.mRootWindowContainer.updateUIDsPresentOnDisplay();
        forAllActivities(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda42
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ActivityRecord) obj).updateAnimatingActivityRegistry();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.TaskFragment
    public ActivityRecord getTopResumedActivity() {
        if (!isLeafTask()) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                Task child = ((WindowContainer) this.mChildren.get(i)).asTask();
                ActivityRecord resumedActivity = child == null ? null : child.getTopResumedActivity();
                if (resumedActivity != null) {
                    return resumedActivity;
                }
            }
        }
        ActivityRecord taskResumedActivity = getResumedActivity();
        ActivityRecord topResumedActivity = null;
        for (int i2 = this.mChildren.size() - 1; i2 >= 0; i2--) {
            WindowContainer child2 = (WindowContainer) this.mChildren.get(i2);
            if (child2.asTaskFragment() != null) {
                topResumedActivity = child2.asTaskFragment().getTopResumedActivity();
            } else if (taskResumedActivity != null && child2.asActivityRecord() == taskResumedActivity) {
                topResumedActivity = taskResumedActivity;
            }
            if (topResumedActivity != null) {
                return topResumedActivity;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.TaskFragment
    public ActivityRecord getTopPausingActivity() {
        if (!isLeafTask()) {
            for (int i = this.mChildren.size() - 1; i >= 0; i--) {
                ActivityRecord pausingActivity = ((WindowContainer) this.mChildren.get(i)).asTask().getTopPausingActivity();
                if (pausingActivity != null) {
                    return pausingActivity;
                }
            }
        }
        ActivityRecord taskPausingActivity = getPausingActivity();
        ActivityRecord topPausingActivity = null;
        for (int i2 = this.mChildren.size() - 1; i2 >= 0; i2--) {
            WindowContainer child = (WindowContainer) this.mChildren.get(i2);
            if (child.asTaskFragment() != null) {
                topPausingActivity = child.asTaskFragment().getTopPausingActivity();
            } else if (taskPausingActivity != null && child.asActivityRecord() == taskPausingActivity) {
                topPausingActivity = taskPausingActivity;
            }
            if (topPausingActivity != null) {
                return topPausingActivity;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTaskMovement(boolean toTop, int position) {
        EventLogTags.writeWmTaskMoved(this.mTaskId, toTop ? 1 : 0, position);
        TaskDisplayArea taskDisplayArea = getDisplayArea();
        if (taskDisplayArea != null && isLeafTask()) {
            taskDisplayArea.onLeafTaskMoved(this, toTop);
        }
        if (this.isPersistable) {
            this.mLastTimeMoved = System.currentTimeMillis();
        }
    }

    private void closeRecentsChain() {
        Task task = this.mPrevAffiliate;
        if (task != null) {
            task.setNextAffiliate(this.mNextAffiliate);
        }
        Task task2 = this.mNextAffiliate;
        if (task2 != null) {
            task2.setPrevAffiliate(this.mPrevAffiliate);
        }
        setPrevAffiliate(null);
        setNextAffiliate(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removedFromRecents() {
        closeRecentsChain();
        if (this.inRecents) {
            this.inRecents = false;
            this.mAtmService.notifyTaskPersisterLocked(this, false);
        }
        clearRootProcess();
        this.mAtmService.mWindowManager.mTaskSnapshotController.notifyTaskRemovedFromRecents(this.mTaskId, this.mUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTaskToAffiliateWith(Task taskToAffiliateWith) {
        closeRecentsChain();
        this.mAffiliatedTaskId = taskToAffiliateWith.mAffiliatedTaskId;
        while (true) {
            if (taskToAffiliateWith.mNextAffiliate == null) {
                break;
            }
            Task nextRecents = taskToAffiliateWith.mNextAffiliate;
            if (nextRecents.mAffiliatedTaskId != this.mAffiliatedTaskId) {
                Slog.e(TAG, "setTaskToAffiliateWith: nextRecents=" + nextRecents + " affilTaskId=" + nextRecents.mAffiliatedTaskId + " should be " + this.mAffiliatedTaskId);
                if (nextRecents.mPrevAffiliate == taskToAffiliateWith) {
                    nextRecents.setPrevAffiliate(null);
                }
                taskToAffiliateWith.setNextAffiliate(null);
            } else {
                taskToAffiliateWith = nextRecents;
            }
        }
        taskToAffiliateWith.setNextAffiliate(this);
        setPrevAffiliate(taskToAffiliateWith);
        setNextAffiliate(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Intent getBaseIntent() {
        Intent intent = this.intent;
        if (intent != null) {
            return intent;
        }
        Intent intent2 = this.affinityIntent;
        if (intent2 != null) {
            return intent2;
        }
        Task topTask = getTopMostTask();
        if (topTask == this || topTask == null) {
            return null;
        }
        return topTask.getBaseIntent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getRootActivity() {
        return getRootActivity(true, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getRootActivity(boolean setToBottomIfNone) {
        return getRootActivity(false, setToBottomIfNone);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getRootActivity(boolean ignoreRelinquishIdentity, boolean setToBottomIfNone) {
        return this.mFindRootHelper.findRoot(ignoreRelinquishIdentity, setToBottomIfNone);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topRunningActivityLocked() {
        if (getParent() == null) {
            return null;
        }
        return getActivity(new Task$$ExternalSyntheticLambda22());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUidPresent(int uid) {
        PooledPredicate p = PooledLambda.obtainPredicate(new DisplayContent$$ExternalSyntheticLambda11(), PooledLambda.__(ActivityRecord.class), Integer.valueOf(uid));
        boolean isUidPresent = getActivity(p) != null;
        p.recycle();
        return isUidPresent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topActivityContainsStartingWindow() {
        if (getParent() == null) {
            return null;
        }
        return getActivity(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.lambda$topActivityContainsStartingWindow$2((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$topActivityContainsStartingWindow$2(ActivityRecord r) {
        return r.getWindow(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda50
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.lambda$topActivityContainsStartingWindow$1((WindowState) obj);
            }
        }) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$topActivityContainsStartingWindow$1(WindowState window) {
        return window.getBaseType() == 3;
    }

    private void getNumRunningActivities(TaskActivitiesReport reportOut) {
        reportOut.reset();
        forAllActivities(reportOut);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void moveActivityToFrontLocked(ActivityRecord newTop) {
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(newTop);
            String protoLogParam1 = String.valueOf(Debug.getCallers(4));
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -463348344, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        positionChildAtTop(newTop);
        updateEffectiveIntent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    public void addChild(WindowContainer child, int index) {
        super.addChild(child, getAdjustedChildPosition(child, index));
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(this);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, 1330804250, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (this.mTaskOrganizer != null && this.mCreatedByOrganizer && child.asTask() != null) {
            getDisplayArea().addRootTaskReferenceIfNeeded((Task) child);
        }
        this.mRootWindowContainer.updateUIDsPresentOnDisplay();
        TaskFragment childTaskFrag = child.asTaskFragment();
        if (childTaskFrag != null && childTaskFrag.asTask() == null) {
            childTaskFrag.setMinDimensions(this.mMinWidth, this.mMinHeight);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDescendantActivityAdded(boolean hadActivity, int activityType, ActivityRecord r) {
        warnForNonLeafTask("onDescendantActivityAdded");
        if (!hadActivity) {
            if (r.getActivityType() == 0) {
                r.setActivityType(1);
            }
            setActivityType(r.getActivityType());
            this.isPersistable = r.isPersistable();
            this.mCallingUid = r.launchedFromUid;
            this.mCallingPackage = r.launchedFromPackage;
            this.mCallingFeatureId = r.launchedFromFeatureId;
            this.maxRecents = Math.min(Math.max(r.info.maxRecents, 1), ActivityTaskManager.getMaxAppRecentsLimitStatic());
        } else {
            r.setActivityType(activityType);
        }
        updateEffectiveIntent();
    }

    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    void removeChild(WindowContainer child) {
        removeChild(child, "removeChild");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeChild(WindowContainer r, String reason) {
        WindowContainer<?> parent;
        if (this.mCreatedByOrganizer && r.asTask() != null) {
            getDisplayArea().removeRootTaskReferenceIfNeeded((Task) r);
        }
        if (!this.mChildren.contains(r)) {
            Slog.e(TAG, "removeChild: r=" + r + " not found in t=" + this);
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_TASK_MOVEMENT) {
            Slog.d(WmsExt.TAG, "removeChild: child=" + r + " reason=" + reason);
        }
        super.removeChild(r, false);
        if (inPinnedWindowingMode()) {
            this.mAtmService.getTaskChangeNotificationController().notifyTaskStackChanged();
        }
        if (hasChild()) {
            updateEffectiveIntent();
            if (onlyHasTaskOverlayActivities(true)) {
                this.mTaskSupervisor.removeTask(this, false, false, reason);
            }
        } else if (!this.mReuseTask && shouldRemoveSelfOnLastChildRemoval()) {
            if (!isRootTask() && (parent = getParent()) != null) {
                parent.asTaskFragment().removeChild(this);
            }
            EventLogTags.writeWmTaskRemoved(this.mTaskId, "removeChild:" + reason + " last r=" + r + " in t=" + this);
            removeIfPossible(reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean onlyHasTaskOverlayActivities(boolean includeFinishing) {
        int count = 0;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            ActivityRecord r = getChildAt(i).asActivityRecord();
            if (r == null) {
                return false;
            }
            if (includeFinishing || !r.finishing) {
                if (!r.isTaskOverlay()) {
                    return false;
                }
                count++;
            }
        }
        return count > 0;
    }

    private boolean autoRemoveFromRecents(TaskFragment oldParentFragment) {
        return this.autoRemoveRecents || !(hasChild() || getHasBeenVisible()) || (oldParentFragment != null && oldParentFragment.isEmbedded());
    }

    private void clearPinnedTaskIfNeed() {
        ActivityRecord activityRecord = this.mChildPipActivity;
        if (activityRecord != null && activityRecord.getTask() != null) {
            this.mTaskSupervisor.removeRootTask(this.mChildPipActivity.getTask());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeActivities(final String reason, final boolean excludingTaskOverlay) {
        clearPinnedTaskIfNeed();
        if (getRootTask() == null) {
            forAllActivities(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda13
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    Task.this.m8275lambda$removeActivities$3$comandroidserverwmTask(excludingTaskOverlay, reason, (ActivityRecord) obj);
                }
            });
        } else {
            forAllActivities(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda14
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    Task.this.m8276lambda$removeActivities$4$comandroidserverwmTask(excludingTaskOverlay, reason, (ActivityRecord) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeActivities$3$com-android-server-wm-Task  reason: not valid java name */
    public /* synthetic */ void m8275lambda$removeActivities$3$comandroidserverwmTask(boolean excludingTaskOverlay, String reason, ActivityRecord r) {
        if (r.finishing) {
            return;
        }
        if (excludingTaskOverlay && r.isTaskOverlay()) {
            return;
        }
        r.takeFromHistory();
        removeChild(r, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeActivities$4$com-android-server-wm-Task  reason: not valid java name */
    public /* synthetic */ void m8276lambda$removeActivities$4$comandroidserverwmTask(boolean excludingTaskOverlay, String reason, ActivityRecord r) {
        if (r.finishing) {
            return;
        }
        if (excludingTaskOverlay && r.isTaskOverlay()) {
            return;
        }
        if (r.isState(ActivityRecord.State.RESUMED) || (r.isVisible() && !this.mDisplayContent.mAppTransition.containsTransitRequest(2))) {
            r.finishIfPossible(reason, false);
        } else {
            r.destroyIfPossible(reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void performClearTaskForReuse(boolean excludingTaskOverlay) {
        this.mReuseTask = true;
        this.mTaskSupervisor.beginDeferResume();
        try {
            removeActivities("clear-task-all", excludingTaskOverlay);
        } finally {
            this.mTaskSupervisor.endDeferResume();
            this.mReuseTask = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord performClearTop(ActivityRecord newR, int launchFlags) {
        this.mReuseTask = true;
        this.mTaskSupervisor.beginDeferResume();
        try {
            ActivityRecord result = clearTopActivities(newR, launchFlags);
            return result;
        } finally {
            this.mTaskSupervisor.endDeferResume();
            this.mReuseTask = false;
        }
    }

    private ActivityRecord clearTopActivities(ActivityRecord newR, int launchFlags) {
        ActivityRecord r = findActivityInHistory(newR.mActivityComponent);
        if (r == null) {
            return null;
        }
        PooledPredicate f = PooledLambda.obtainPredicate(new BiPredicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda7
            @Override // java.util.function.BiPredicate
            public final boolean test(Object obj, Object obj2) {
                boolean finishActivityAbove;
                finishActivityAbove = Task.finishActivityAbove((ActivityRecord) obj, (ActivityRecord) obj2);
                return finishActivityAbove;
            }
        }, PooledLambda.__(ActivityRecord.class), r);
        forAllActivities((Predicate<ActivityRecord>) f);
        f.recycle();
        if (r.launchMode == 0 && (536870912 & launchFlags) == 0 && !ActivityStarter.isDocumentLaunchesIntoExisting(launchFlags) && !r.finishing) {
            r.finishIfPossible("clear-task-top", false);
        }
        return r;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean finishActivityAbove(ActivityRecord r, ActivityRecord boundaryActivity) {
        if (r == boundaryActivity) {
            return true;
        }
        if (!r.finishing && !r.isTaskOverlay()) {
            ActivityOptions opts = r.getOptions();
            if (opts != null) {
                r.clearOptionsAnimation();
                boundaryActivity.updateOptionsLocked(opts);
            }
            r.finishIfPossible("clear-task-stack", false);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String lockTaskAuthToString() {
        switch (this.mLockTaskAuth) {
            case 0:
                return "LOCK_TASK_AUTH_DONT_LOCK";
            case 1:
                return "LOCK_TASK_AUTH_PINNABLE";
            case 2:
                return "LOCK_TASK_AUTH_LAUNCHABLE";
            case 3:
                return "LOCK_TASK_AUTH_ALLOWLISTED";
            case 4:
                return "LOCK_TASK_AUTH_LAUNCHABLE_PRIV";
            default:
                return "unknown=" + this.mLockTaskAuth;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLockTaskAuth() {
        setLockTaskAuth(getRootActivity());
    }

    private void setLockTaskAuth(ActivityRecord r) {
        this.mLockTaskAuth = this.mAtmService.getLockTaskController().getLockTaskAuth(r, this);
        if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
            String protoLogParam0 = String.valueOf(this);
            String protoLogParam1 = String.valueOf(lockTaskAuthToString());
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_LOCKTASK, 1824105730, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public boolean supportsSplitScreenWindowingMode() {
        return supportsSplitScreenWindowingModeInDisplayArea(getDisplayArea());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsSplitScreenWindowingModeInDisplayArea(TaskDisplayArea tda) {
        Task topTask = getTopMostTask();
        return super.supportsSplitScreenWindowingMode() && (topTask == null || topTask.supportsSplitScreenWindowingModeInner(tda));
    }

    private boolean supportsSplitScreenWindowingModeInner(TaskDisplayArea tda) {
        return super.supportsSplitScreenWindowingMode() && this.mAtmService.mSupportsSplitScreenMultiWindow && supportsMultiWindowInDisplayArea(tda);
    }

    boolean supportsFreeform() {
        return supportsFreeformInDisplayArea(getDisplayArea());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsFreeformInDisplayArea(TaskDisplayArea tda) {
        return this.mAtmService.mSupportsFreeformWindowManagement && supportsMultiWindowInDisplayArea(tda);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canBeLaunchedOnDisplay(int displayId) {
        return this.mTaskSupervisor.canPlaceEntityOnDisplay(displayId, -1, -1, this);
    }

    private boolean canResizeToBounds(Rect bounds) {
        if (bounds == null || !inFreeformWindowingMode()) {
            return true;
        }
        boolean landscape = bounds.width() > bounds.height();
        Rect configBounds = getRequestedOverrideBounds();
        int i = this.mResizeMode;
        if (i != 7) {
            return !(i == 6 && landscape) && (i != 5 || landscape);
        } else if (configBounds.isEmpty()) {
            return true;
        } else {
            return landscape == (configBounds.width() > configBounds.height());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isClearingToReuseTask() {
        return this.mReuseTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord findActivityInHistory(ComponentName component) {
        PooledPredicate p = PooledLambda.obtainPredicate(new BiPredicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda24
            @Override // java.util.function.BiPredicate
            public final boolean test(Object obj, Object obj2) {
                boolean matchesActivityInHistory;
                matchesActivityInHistory = Task.matchesActivityInHistory((ActivityRecord) obj, (ComponentName) obj2);
                return matchesActivityInHistory;
            }
        }, PooledLambda.__(ActivityRecord.class), component);
        ActivityRecord r = getActivity(p);
        p.recycle();
        return r;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean matchesActivityInHistory(ActivityRecord r, ComponentName activityComponent) {
        return !r.finishing && r.mActivityComponent.equals(activityComponent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTaskDescription() {
        Task t;
        ActivityRecord root = getRootActivity(true);
        if (root == null) {
            return;
        }
        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription();
        PooledPredicate f = PooledLambda.obtainPredicate(new TriPredicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda37
            public final boolean test(Object obj, Object obj2, Object obj3) {
                boolean taskDescriptionFromActivityAboveRoot;
                taskDescriptionFromActivityAboveRoot = Task.setTaskDescriptionFromActivityAboveRoot((ActivityRecord) obj, (ActivityRecord) obj2, (ActivityManager.TaskDescription) obj3);
                return taskDescriptionFromActivityAboveRoot;
            }
        }, PooledLambda.__(ActivityRecord.class), root, taskDescription);
        forAllActivities((Predicate<ActivityRecord>) f);
        f.recycle();
        taskDescription.setResizeMode(this.mResizeMode);
        taskDescription.setMinWidth(this.mMinWidth);
        taskDescription.setMinHeight(this.mMinHeight);
        setTaskDescription(taskDescription);
        this.mAtmService.getTaskChangeNotificationController().notifyTaskDescriptionChanged(getTaskInfo());
        WindowContainer parent = getParent();
        if (parent != null && (t = parent.asTask()) != null) {
            t.updateTaskDescription();
        }
        dispatchTaskInfoChangedIfNeeded(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean setTaskDescriptionFromActivityAboveRoot(ActivityRecord r, ActivityRecord root, ActivityManager.TaskDescription td) {
        if (!r.isTaskOverlay() && r.taskDescription != null) {
            ActivityManager.TaskDescription atd = r.taskDescription;
            if (td.getLabel() == null) {
                td.setLabel(atd.getLabel());
            }
            if (td.getRawIcon() == null) {
                td.setIcon(atd.getRawIcon());
            }
            if (td.getIconFilename() == null) {
                td.setIconFilename(atd.getIconFilename());
            }
            if (td.getPrimaryColor() == 0) {
                td.setPrimaryColor(atd.getPrimaryColor());
            }
            if (td.getBackgroundColor() == 0) {
                td.setBackgroundColor(atd.getBackgroundColor());
            }
            if (td.getStatusBarColor() == 0) {
                td.setStatusBarColor(atd.getStatusBarColor());
                td.setEnsureStatusBarContrastWhenTransparent(atd.getEnsureStatusBarContrastWhenTransparent());
            }
            if (td.getNavigationBarColor() == 0) {
                td.setNavigationBarColor(atd.getNavigationBarColor());
                td.setEnsureNavigationBarContrastWhenTransparent(atd.getEnsureNavigationBarContrastWhenTransparent());
            }
            if (td.getBackgroundColorFloating() == 0) {
                td.setBackgroundColorFloating(atd.getBackgroundColorFloating());
            }
        }
        return r == root;
    }

    void updateEffectiveIntent() {
        ActivityRecord root = getRootActivity(true);
        if (root != null) {
            setIntent(root);
            updateTaskDescription();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastNonFullscreenBounds(Rect bounds) {
        Rect rect = this.mLastNonFullscreenBounds;
        if (rect == null) {
            this.mLastNonFullscreenBounds = new Rect(bounds);
        } else {
            rect.set(bounds);
        }
    }

    private void onConfigurationChangedInner(Configuration newParentConfig) {
        ActivityRecord r;
        Rect rect;
        boolean prevPersistTaskBounds = getWindowConfiguration().persistTaskBounds();
        boolean nextPersistTaskBounds = getRequestedOverrideConfiguration().windowConfiguration.persistTaskBounds();
        if (getRequestedOverrideWindowingMode() == 0) {
            nextPersistTaskBounds = newParentConfig.windowConfiguration.persistTaskBounds();
        }
        if (!prevPersistTaskBounds && nextPersistTaskBounds && (rect = this.mLastNonFullscreenBounds) != null && !rect.isEmpty()) {
            getRequestedOverrideConfiguration().windowConfiguration.setBounds(this.mLastNonFullscreenBounds);
        }
        int prevWinMode = getWindowingMode();
        this.mTmpPrevBounds.set(getBounds());
        boolean wasInMultiWindowMode = inMultiWindowMode();
        boolean wasInPictureInPicture = inPinnedWindowingMode();
        super.onConfigurationChanged(newParentConfig);
        updateSurfaceSize(getSyncTransaction());
        boolean pipChanging = wasInPictureInPicture != inPinnedWindowingMode();
        if (pipChanging) {
            this.mTaskSupervisor.scheduleUpdatePictureInPictureModeIfNeeded(this, getRootTask());
        } else if (wasInMultiWindowMode != inMultiWindowMode()) {
            this.mTaskSupervisor.scheduleUpdateMultiWindowMode(this);
        }
        int newWinMode = getWindowingMode();
        if (prevWinMode != newWinMode && this.mDisplayContent != null && shouldStartChangeTransition(prevWinMode, newWinMode)) {
            initializeChangeTransition(this.mTmpPrevBounds);
        }
        if (ITranTask.Instance().isAdjacentTaskEnable() && ITranTask.Instance().hookShouldMarkAdjacentTask(prevWinMode, newWinMode, isLeafTask(), this.mCreatedByOrganizer)) {
            ITranTask.Instance().hookMarkAdjacentTask(this);
        }
        if (getWindowConfiguration().persistTaskBounds()) {
            Rect currentBounds = getRequestedOverrideBounds();
            if (!currentBounds.isEmpty()) {
                setLastNonFullscreenBounds(currentBounds);
            }
        }
        if (pipChanging && wasInPictureInPicture && (r = topRunningActivity()) != null && this.mDisplayContent.isFixedRotationLaunchingApp(r)) {
            getSyncTransaction().setWindowCrop(this.mSurfaceControl, null).setCornerRadius(this.mSurfaceControl, 0.0f).setMatrix(this.mSurfaceControl, Matrix.IDENTITY_MATRIX, new float[9]);
        }
        saveLaunchingStateIfNeeded();
        boolean taskOrgChanged = updateTaskOrganizerState();
        if (taskOrgChanged) {
            updateSurfacePosition(getSyncTransaction());
            if (!isOrganized()) {
                updateSurfaceSize(getSyncTransaction());
            }
        }
        if (!taskOrgChanged) {
            dispatchTaskInfoChangedIfNeeded(false);
        }
    }

    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void onConfigurationChanged(Configuration newParentConfig) {
        ITranTask.Instance().showStabilityMultiToast(newParentConfig, getConfiguration(), this.realActivity, this.mRootWindowContainer.getMultiWindowWhiteList());
        if (this.mDisplayContent != null && this.mDisplayContent.mPinnedTaskController.isFreezingTaskConfig(this)) {
            return;
        }
        if (!isRootTask()) {
            onConfigurationChangedInner(newParentConfig);
            return;
        }
        int prevWindowingMode = getWindowingMode();
        boolean prevIsAlwaysOnTop = isAlwaysOnTop();
        int prevRotation = getWindowConfiguration().getRotation();
        Rect newBounds = this.mTmpRect;
        getBounds(newBounds);
        onConfigurationChangedInner(newParentConfig);
        TaskDisplayArea taskDisplayArea = getDisplayArea();
        if (taskDisplayArea == null) {
            return;
        }
        if (prevWindowingMode != getWindowingMode()) {
            taskDisplayArea.onRootTaskWindowingModeChanged(this);
            ITranWindowManagerService.Instance().onTaskWindowingModeChange(this.mAtmService.mUiContext, this, prevWindowingMode, getWindowingMode());
        }
        if (!isOrganized() && !getRequestedOverrideBounds().isEmpty() && this.mDisplayContent != null) {
            int newRotation = getWindowConfiguration().getRotation();
            boolean rotationChanged = prevRotation != newRotation;
            if (rotationChanged) {
                this.mDisplayContent.rotateBounds(prevRotation, newRotation, newBounds);
                setBounds(newBounds);
            }
        }
        if (prevIsAlwaysOnTop != isAlwaysOnTop()) {
            taskDisplayArea.positionChildAt(Integer.MAX_VALUE, this, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resolveLeafTaskOnlyOverrideConfigs(Configuration newParentConfig, Rect previousBounds) {
        if (!isLeafTask()) {
            return;
        }
        int windowingMode = getResolvedOverrideConfiguration().windowConfiguration.getWindowingMode();
        if (windowingMode == 0) {
            windowingMode = newParentConfig.windowConfiguration.getWindowingMode();
        }
        getConfiguration().windowConfiguration.setWindowingMode(windowingMode);
        Rect outOverrideBounds = getResolvedOverrideConfiguration().windowConfiguration.getBounds();
        if (windowingMode == 1) {
            if (!isOrganized()) {
                outOverrideBounds.setEmpty();
                return;
            }
            return;
        }
        adjustForMinimalTaskDimensions(outOverrideBounds, previousBounds, newParentConfig);
        if (windowingMode == 5) {
            computeFreeformBounds(outOverrideBounds, newParentConfig);
        }
    }

    void adjustForMinimalTaskDimensions(Rect bounds, Rect previousBounds, Configuration parentConfig) {
        int minWidth = this.mMinWidth;
        int minHeight = this.mMinHeight;
        if (!inPinnedWindowingMode()) {
            int defaultMinSizeDp = this.mDisplayContent == null ? 220 : this.mDisplayContent.mMinSizeOfResizeableTaskDp;
            float density = parentConfig.densityDpi / 160.0f;
            int defaultMinSize = (int) (defaultMinSizeDp * density);
            if (minWidth == -1) {
                minWidth = defaultMinSize;
            }
            if (minHeight == -1) {
                minHeight = defaultMinSize;
            }
        }
        if (bounds.isEmpty()) {
            Rect parentBounds = parentConfig.windowConfiguration.getBounds();
            if (parentBounds.width() >= minWidth && parentBounds.height() >= minHeight) {
                return;
            }
            bounds.set(parentBounds);
        }
        boolean adjustWidth = minWidth > bounds.width();
        boolean adjustHeight = minHeight > bounds.height();
        if (!adjustWidth && !adjustHeight) {
            return;
        }
        if (adjustWidth) {
            if (!previousBounds.isEmpty() && bounds.right == previousBounds.right) {
                bounds.left = bounds.right - minWidth;
            } else {
                bounds.right = bounds.left + minWidth;
            }
        }
        if (adjustHeight) {
            if (!previousBounds.isEmpty() && bounds.bottom == previousBounds.bottom) {
                bounds.top = bounds.bottom - minHeight;
            } else {
                bounds.bottom = bounds.top + minHeight;
            }
        }
    }

    private void computeFreeformBounds(Rect outBounds, Configuration newParentConfig) {
        float density = newParentConfig.densityDpi / 160.0f;
        Rect parentBounds = new Rect(newParentConfig.windowConfiguration.getBounds());
        DisplayContent display = getDisplayContent();
        if (display != null) {
            Rect stableBounds = new Rect();
            display.getStableRect(stableBounds);
            parentBounds.intersect(stableBounds);
        }
        fitWithinBounds(outBounds, parentBounds, (int) (48.0f * density), (int) (32.0f * density));
        int offsetTop = parentBounds.top - outBounds.top;
        if (offsetTop > 0) {
            outBounds.offset(0, offsetTop);
        }
    }

    private static void fitWithinBounds(Rect bounds, Rect rootTaskBounds, int overlapPxX, int overlapPxY) {
        if (rootTaskBounds == null || rootTaskBounds.isEmpty() || rootTaskBounds.contains(bounds)) {
            return;
        }
        int horizontalDiff = 0;
        int overlapLR = Math.min(overlapPxX, bounds.width());
        if (bounds.right < rootTaskBounds.left + overlapLR) {
            horizontalDiff = overlapLR - (bounds.right - rootTaskBounds.left);
        } else if (bounds.left > rootTaskBounds.right - overlapLR) {
            horizontalDiff = -(overlapLR - (rootTaskBounds.right - bounds.left));
        }
        int verticalDiff = 0;
        int overlapTB = Math.min(overlapPxY, bounds.width());
        if (bounds.bottom < rootTaskBounds.top + overlapTB) {
            verticalDiff = overlapTB - (bounds.bottom - rootTaskBounds.top);
        } else if (bounds.top > rootTaskBounds.bottom - overlapTB) {
            verticalDiff = -(overlapTB - (rootTaskBounds.bottom - bounds.top));
        }
        bounds.offset(horizontalDiff, verticalDiff);
    }

    private boolean shouldStartChangeTransition(int prevWinMode, int newWinMode) {
        if (isLeafTask() && canStartChangeTransition()) {
            return (prevWinMode == 5) != (newWinMode == 5);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void migrateToNewSurfaceControl(SurfaceControl.Transaction t) {
        super.migrateToNewSurfaceControl(t);
        this.mLastSurfaceSize.x = 0;
        this.mLastSurfaceSize.y = 0;
        updateSurfaceSize(t);
    }

    void updateSurfaceSize(SurfaceControl.Transaction transaction) {
        if (this.mSurfaceControl == null || isOrganized() || !this.mSurfaceControl.isValid()) {
            return;
        }
        int width = 0;
        int height = 0;
        if (isRootTask()) {
            Rect taskBounds = getBounds();
            width = taskBounds.width();
            height = taskBounds.height();
        }
        if (width == this.mLastSurfaceSize.x && height == this.mLastSurfaceSize.y) {
            return;
        }
        transaction.setWindowCrop(this.mSurfaceControl, width, height);
        this.mLastSurfaceSize.set(width, height);
    }

    Point getLastSurfaceSize() {
        return this.mLastSurfaceSize;
    }

    boolean isInChangeTransition() {
        return this.mSurfaceFreezer.hasLeash() || AppTransition.isChangeTransitOld(this.mTransit);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceFreezer.Freezable
    public SurfaceControl getFreezeSnapshotTarget() {
        if (this.mDisplayContent.mAppTransition.containsTransitRequest(6)) {
            ArraySet<Integer> activityTypes = new ArraySet<>();
            activityTypes.add(Integer.valueOf(getActivityType()));
            RemoteAnimationAdapter adapter = this.mDisplayContent.mAppTransitionController.getRemoteAnimationOverride(this, 27, activityTypes);
            if (adapter == null || adapter.getChangeNeedsSnapshot()) {
                return getSurfaceControl();
            }
            return null;
        }
        return null;
    }

    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    void writeIdentifierToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, System.identityHashCode(this));
        proto.write(1120986464258L, this.mUserId);
        Intent intent = this.intent;
        proto.write(1138166333443L, (intent == null || intent.getComponent() == null) ? "Task" : this.intent.getComponent().flattenToShortString());
        proto.end(token);
    }

    private void saveLaunchingStateIfNeeded() {
        saveLaunchingStateIfNeeded(getDisplayContent());
    }

    private void saveLaunchingStateIfNeeded(DisplayContent display) {
        if (!isLeafTask() || !getHasBeenVisible()) {
            return;
        }
        int windowingMode = getWindowingMode();
        if ((windowingMode != 1 && windowingMode != 5) || getWindowConfiguration().getDisplayWindowingMode() != 5) {
            return;
        }
        this.mTaskSupervisor.mLaunchParamsPersister.saveTask(this, display);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect updateOverrideConfigurationFromLaunchBounds() {
        Task rootTask = getRootTask();
        Rect bounds = (rootTask == this || !rootTask.isOrganized()) ? getLaunchBounds() : null;
        setBounds(bounds);
        if (bounds != null && !bounds.isEmpty()) {
            bounds.set(getRequestedOverrideBounds());
        }
        return bounds;
    }

    Rect getLaunchBounds() {
        Task rootTask = getRootTask();
        if (rootTask == null) {
            return null;
        }
        int windowingMode = getWindowingMode();
        if (!isActivityTypeStandardOrUndefined() || windowingMode == 1) {
            if (isResizeable()) {
                return rootTask.getRequestedOverrideBounds();
            }
            return null;
        } else if (!getWindowConfiguration().persistTaskBounds()) {
            return rootTask.getRequestedOverrideBounds();
        } else {
            return this.mLastNonFullscreenBounds;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRootProcess(WindowProcessController proc) {
        clearRootProcess();
        Intent intent = this.intent;
        if (intent != null && (intent.getFlags() & 8388608) == 0) {
            this.mRootProcess = proc;
            proc.addRecentTask(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearRootProcess() {
        WindowProcessController windowProcessController = this.mRootProcess;
        if (windowProcessController != null) {
            windowProcessController.removeRecentTask(this);
            this.mRootProcess = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRootTaskId() {
        return getRootTask().mTaskId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getOrganizedTask() {
        Task parentTask;
        if (isOrganized()) {
            return this;
        }
        WindowContainer parent = getParent();
        if (parent == null || (parentTask = parent.asTask()) == null) {
            return null;
        }
        return parentTask.getOrganizedTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getCreatedByOrganizerTask() {
        Task parentTask;
        if (this.mCreatedByOrganizer) {
            return this;
        }
        WindowContainer parent = getParent();
        if (parent == null || (parentTask = parent.asTask()) == null) {
            return null;
        }
        return parentTask.getCreatedByOrganizerTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRootTask() {
        return getRootTask() == this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLeafTask() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((WindowContainer) this.mChildren.get(i)).asTask() != null) {
                return false;
            }
        }
        return true;
    }

    public Task getTopLeafTask() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            Task child = ((WindowContainer) this.mChildren.get(i)).asTask();
            if (child != null) {
                return child.getTopLeafTask();
            }
        }
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDescendantTaskCount() {
        int[] currentCount = {0};
        PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                Task.lambda$getDescendantTaskCount$5((Task) obj, (int[]) obj2);
            }
        }, PooledLambda.__(Task.class), currentCount);
        forAllLeafTasks(c, false);
        c.recycle();
        return currentCount[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getDescendantTaskCount$5(Task t, int[] count) {
        count[0] = count[0] + 1;
    }

    Task adjustFocusToNextFocusableTask(String reason) {
        return adjustFocusToNextFocusableTask(reason, false, true);
    }

    private Task getNextFocusableTask(final boolean allowFocusSelf) {
        WindowContainer parent = getParent();
        if (parent == null) {
            return null;
        }
        Task focusableTask = parent.getTask(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda53
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.this.m8272lambda$getNextFocusableTask$6$comandroidserverwmTask(allowFocusSelf, obj);
            }
        });
        if (focusableTask == null && parent.asTask() != null) {
            return parent.asTask().getNextFocusableTask(allowFocusSelf);
        }
        return focusableTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getNextFocusableTask$6$com-android-server-wm-Task  reason: not valid java name */
    public /* synthetic */ boolean m8272lambda$getNextFocusableTask$6$comandroidserverwmTask(boolean allowFocusSelf, Object task) {
        return (allowFocusSelf || task != this) && ((Task) task).isFocusableAndVisible();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task adjustFocusToNextFocusableTask(String reason, boolean allowFocusSelf, boolean moveDisplayToTop) {
        Task focusableTask = getNextFocusableTask(allowFocusSelf);
        if (focusableTask == null) {
            focusableTask = this.mRootWindowContainer.getNextFocusableRootTask(this, !allowFocusSelf);
        }
        if (focusableTask == null) {
            TaskDisplayArea taskDisplayArea = getDisplayArea();
            if (taskDisplayArea != null) {
                taskDisplayArea.clearPreferredTopFocusableRootTask();
                return null;
            }
            return null;
        }
        Task rootTask = focusableTask.getRootTask();
        if (!moveDisplayToTop) {
            WindowContainer parent = focusableTask.getParent();
            WindowContainer next = focusableTask;
            do {
                parent.positionChildAt(Integer.MAX_VALUE, next, false);
                next = parent;
                parent = next.getParent();
                if (next.asTask() == null) {
                    break;
                }
            } while (parent != null);
            return rootTask;
        }
        String myReason = reason + " adjustFocusToNextFocusableTask";
        ActivityRecord top = focusableTask.topRunningActivity();
        if (focusableTask.isActivityTypeHome() && (top == null || !top.mVisibleRequested)) {
            focusableTask.getDisplayArea().moveHomeActivityToTop(myReason);
            return rootTask;
        }
        focusableTask.moveToFront(myReason);
        if (rootTask.getTopResumedActivity() != null) {
            this.mTaskSupervisor.updateTopResumedActivityIfNeeded();
            ITranWindowManagerService.Instance().onActivityResume(rootTask.getResumedActivity());
            this.mAtmService.setResumedActivityUncheckLocked(rootTask.getTopResumedActivity(), reason);
        }
        return rootTask;
    }

    private int computeMinUserPosition(int minPosition, int size) {
        while (minPosition < size) {
            WindowContainer child = (WindowContainer) this.mChildren.get(minPosition);
            boolean canShow = child.showToCurrentUser();
            if (canShow) {
                break;
            }
            minPosition++;
        }
        return minPosition;
    }

    private int computeMaxUserPosition(int maxPosition) {
        while (maxPosition > 0) {
            WindowContainer child = (WindowContainer) this.mChildren.get(maxPosition);
            boolean canShow = child.showToCurrentUser();
            if (!canShow) {
                break;
            }
            maxPosition--;
        }
        return maxPosition;
    }

    private int getAdjustedChildPosition(WindowContainer wc, int suggestedPosition) {
        boolean canShowChild = wc.showToCurrentUser();
        int size = this.mChildren.size();
        int minPosition = canShowChild ? computeMinUserPosition(0, size) : 0;
        int maxPosition = minPosition;
        if (size > 0) {
            int i = size - 1;
            if (!canShowChild) {
                i = computeMaxUserPosition(i);
            }
            maxPosition = i;
        }
        if (!wc.isAlwaysOnTop()) {
            while (maxPosition > minPosition && ((WindowContainer) this.mChildren.get(maxPosition)).isAlwaysOnTop()) {
                maxPosition--;
            }
        }
        if (suggestedPosition == Integer.MIN_VALUE && minPosition == 0) {
            return Integer.MIN_VALUE;
        }
        if (suggestedPosition == Integer.MAX_VALUE && maxPosition >= size - 1) {
            return Integer.MAX_VALUE;
        }
        if (!hasChild(wc)) {
            maxPosition++;
        }
        return Math.min(Math.max(suggestedPosition, minPosition), maxPosition);
    }

    @Override // com.android.server.wm.WindowContainer
    void positionChildAt(int position, WindowContainer child, boolean includingParents) {
        boolean toTop = position >= this.mChildren.size() - 1;
        int position2 = getAdjustedChildPosition(child, position);
        super.positionChildAt(position2, child, includingParents);
        if (WindowManagerDebugConfig.DEBUG_TASK_MOVEMENT) {
            Slog.d(WmsExt.TAG, "positionChildAt: child=" + child + " position=" + position2 + " parent=" + this);
        }
        Task task = child.asTask();
        if (task != null) {
            task.updateTaskMovement(toTop, position2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    public void removeImmediately() {
        removeImmediately("removeTask");
    }

    @Override // com.android.server.wm.TaskFragment
    void removeImmediately(String reason) {
        if (WindowManagerDebugConfig.DEBUG_ROOT_TASK) {
            Slog.i(TAG, "removeTask:" + reason + " removing taskId=" + this.mTaskId);
        }
        if (this.mRemoving) {
            return;
        }
        this.mRemoving = true;
        EventLogTags.writeWmTaskRemoved(this.mTaskId, reason);
        clearPinnedTaskIfNeed();
        setTaskOrganizer(null);
        if (ThunderbackConfig.isVersion4()) {
            releaseTaskAnimation();
        }
        super.removeImmediately();
        this.mRemoving = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reparent(Task rootTask, int position, boolean moveParents, String reason) {
        if (WindowManagerDebugConfig.DEBUG_ROOT_TASK) {
            Slog.i(TAG, "reParentTask: removing taskId=" + this.mTaskId + " from rootTask=" + getRootTask());
        }
        EventLogTags.writeWmTaskRemoved(this.mTaskId, "reParentTask:" + reason);
        reparent(rootTask, position);
        rootTask.positionChildAt(position, this, moveParents);
    }

    public int setBounds(Rect bounds, boolean forceResize) {
        int boundsChanged = setBounds(bounds);
        if (forceResize && (boundsChanged & 2) != 2) {
            onResize();
            return boundsChanged | 2;
        }
        return boundsChanged;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public int setBounds(Rect bounds) {
        if (isRootTask()) {
            return setBounds(getRequestedOverrideBounds(), bounds);
        }
        int rotation = 0;
        DisplayContent displayContent = getRootTask() != null ? getRootTask().getDisplayContent() : null;
        if (displayContent != null) {
            rotation = displayContent.getDisplayInfo().rotation;
        }
        int boundsChange = super.setBounds(bounds);
        this.mRotation = rotation;
        updateSurfacePositionNonOrganized();
        return boundsChange;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public boolean isCompatible(int windowingMode, int activityType) {
        if (activityType == 0) {
            activityType = 1;
        }
        return super.isCompatible(windowingMode, activityType);
    }

    @Override // com.android.server.wm.WindowContainer
    public boolean onDescendantOrientationChanged(WindowContainer requestingContainer) {
        if (super.onDescendantOrientationChanged(requestingContainer)) {
            return true;
        }
        if (getParent() != null) {
            onConfigurationChanged(getParent().getConfiguration());
            return true;
        }
        return false;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean handlesOrientationChangeFromDescendant() {
        if (super.handlesOrientationChangeFromDescendant()) {
            if (isLeafTask()) {
                return canSpecifyOrientation() && getDisplayArea().canSpecifyOrientation();
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resize(boolean relayout, boolean forced) {
        if (setBounds(getRequestedOverrideBounds(), forced) != 0 && relayout) {
            getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void onDisplayChanged(DisplayContent dc) {
        boolean isRootTask = isRootTask();
        if (!isRootTask && !this.mCreatedByOrganizer) {
            adjustBoundsForDisplayChangeIfNeeded(dc);
        }
        super.onDisplayChanged(dc);
        if (isLeafTask()) {
            int displayId = dc != null ? dc.getDisplayId() : -1;
            this.mWmService.mAtmService.getTaskChangeNotificationController().notifyTaskDisplayChanged(this.mTaskId, displayId);
        }
        if (isRootTask()) {
            updateSurfaceBounds();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isResizeable() {
        return isResizeable(true);
    }

    boolean isResizeable(boolean checkPictureInPictureSupport) {
        boolean forceResizable = this.mAtmService.mForceResizableActivities && getActivityType() == 1;
        return forceResizable || ActivityInfo.isResizeableMode(this.mResizeMode) || (this.mSupportsPictureInPicture && checkPictureInPictureSupport);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean preserveOrientationOnResize() {
        int i = this.mResizeMode;
        return i == 6 || i == 5 || i == 7;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean cropWindowsToRootTaskBounds() {
        if (isActivityTypeHomeOrRecents()) {
            Task rootTask = getRootTask();
            Task topNonOrgTask = rootTask.mCreatedByOrganizer ? rootTask.getTopMostTask() : rootTask;
            if (this == topNonOrgTask || isDescendantOf(topNonOrgTask)) {
                return false;
            }
        }
        return isResizeable();
    }

    @Override // com.android.server.wm.WindowContainer
    void getAnimationFrames(Rect outFrame, Rect outInsets, Rect outStableInsets, Rect outSurfaceInsets) {
        WindowState windowState = getTopVisibleAppMainWindow();
        if (windowState != null) {
            windowState.getAnimationFrames(outFrame, outInsets, outStableInsets, outSurfaceInsets);
        } else {
            super.getAnimationFrames(outFrame, outInsets, outStableInsets, outSurfaceInsets);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void getMaxVisibleBounds(ActivityRecord token, Rect out, boolean[] foundTop) {
        WindowState win;
        if (token.mIsExiting || !token.isClientVisible() || !token.mVisibleRequested || (win = token.findMainWindow()) == null) {
            return;
        }
        if (!foundTop[0]) {
            foundTop[0] = true;
            out.setEmpty();
        }
        Rect visibleFrame = sTmpBounds;
        WindowManager.LayoutParams attrs = win.mAttrs;
        visibleFrame.set(win.getFrame());
        visibleFrame.inset(win.getInsetsStateWithVisibilityOverride().calculateVisibleInsets(visibleFrame, attrs.type, win.getWindowingMode(), attrs.softInputMode, attrs.flags));
        out.union(visibleFrame);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getDimBounds(Rect out) {
        if (isRootTask()) {
            getBounds(out);
            return;
        }
        Task rootTask = getRootTask();
        DisplayContent displayContent = rootTask.getDisplayContent();
        boolean dockedResizing = displayContent != null && displayContent.mDividerControllerLocked.isResizing();
        if (inFreeformWindowingMode()) {
            boolean[] foundTop = {false};
            PooledConsumer c = PooledLambda.obtainConsumer(new TriConsumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda11
                public final void accept(Object obj, Object obj2, Object obj3) {
                    Task.getMaxVisibleBounds((ActivityRecord) obj, (Rect) obj2, (boolean[]) obj3);
                }
            }, PooledLambda.__(ActivityRecord.class), out, foundTop);
            forAllActivities((Consumer<ActivityRecord>) c);
            c.recycle();
            if (foundTop[0]) {
                return;
            }
        }
        if (!matchParentBounds()) {
            if (dockedResizing) {
                rootTask.getBounds(out);
                return;
            }
            rootTask.getBounds(this.mTmpRect);
            this.mTmpRect.intersect(getBounds());
            out.set(this.mTmpRect);
            return;
        }
        out.set(getBounds());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void adjustAnimationBoundsForTransition(Rect animationBounds) {
        TaskTransitionSpec spec = this.mWmService.mTaskTransitionSpec;
        if (spec != null) {
            for (Integer num : spec.animationBoundInsets) {
                int insetType = num.intValue();
                WindowContainerInsetsSourceProvider insetProvider = getDisplayContent().getInsetsStateController().getSourceProvider(insetType);
                Insets insets = insetProvider.getSource().calculateVisibleInsets(animationBounds);
                animationBounds.inset(insets);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDragResizing(boolean dragResizing, int dragResizeMode) {
        if (this.mDragResizing != dragResizing) {
            if (dragResizing && !DragResizeMode.isModeAllowedForRootTask(getRootTask(), dragResizeMode)) {
                throw new IllegalArgumentException("Drag resize mode not allow for root task id=" + getRootTaskId() + " dragResizeMode=" + dragResizeMode);
            }
            this.mDragResizing = dragResizing;
            this.mDragResizeMode = dragResizeMode;
            resetDragResizingChangeReported();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDragResizing() {
        return this.mDragResizing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDragResizeMode() {
        return this.mDragResizeMode;
    }

    void adjustBoundsForDisplayChangeIfNeeded(DisplayContent displayContent) {
        if (displayContent == null || getRequestedOverrideBounds().isEmpty()) {
            return;
        }
        int displayId = displayContent.getDisplayId();
        int newRotation = displayContent.getDisplayInfo().rotation;
        if (displayId != this.mLastRotationDisplayId) {
            this.mLastRotationDisplayId = displayId;
            this.mRotation = newRotation;
        } else if (this.mRotation == newRotation) {
        } else {
            this.mTmpRect2.set(getBounds());
            if (!getWindowConfiguration().canResizeTask()) {
                setBounds(this.mTmpRect2);
                return;
            }
            displayContent.rotateBounds(this.mRotation, newRotation, this.mTmpRect2);
            if (setBounds(this.mTmpRect2) != 0) {
                this.mAtmService.resizeTask(this.mTaskId, getBounds(), 1);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cancelTaskWindowTransition() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).cancelAnimation();
        }
    }

    boolean showForAllUsers() {
        ActivityRecord r;
        return (this.mChildren.isEmpty() || (r = getTopNonFinishingActivity()) == null || !r.mShowForAllUsers) ? false : true;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean showToCurrentUser() {
        return this.mForceShowForAllUsers || showForAllUsers() || this.mWmService.isCurrentProfile(getTopMostTask().mUserId);
    }

    void setForceShowForAllUsers(boolean forceShowForAllUsers) {
        this.mForceShowForAllUsers = forceShowForAllUsers;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getOccludingActivityAbove(final ActivityRecord activity) {
        ActivityRecord top = getActivity(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda51
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.this.m8273lambda$getOccludingActivityAbove$7$comandroidserverwmTask(activity, (ActivityRecord) obj);
            }
        });
        if (top != activity) {
            return top;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getOccludingActivityAbove$7$com-android-server-wm-Task  reason: not valid java name */
    public /* synthetic */ boolean m8273lambda$getOccludingActivityAbove$7$comandroidserverwmTask(ActivityRecord activity, ActivityRecord r) {
        if (r == activity) {
            return true;
        }
        if (r.occludesParent()) {
            TaskFragment parent = r.getTaskFragment();
            if (parent == activity.getTaskFragment() || isSelfOrNonEmbeddedTask(parent.asTask())) {
                return true;
            }
            TaskFragment grandParent = parent.getParent().asTaskFragment();
            while (grandParent != null && parent.getBounds().equals(grandParent.getBounds())) {
                if (isSelfOrNonEmbeddedTask(grandParent.asTask())) {
                    return true;
                }
                parent = grandParent;
                grandParent = parent.getParent().asTaskFragment();
            }
            return false;
        }
        return false;
    }

    private boolean isSelfOrNonEmbeddedTask(Task task) {
        if (task == this) {
            return true;
        }
        return (task == null || task.isEmbedded()) ? false : true;
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public SurfaceControl.Builder makeAnimationLeash() {
        return super.makeAnimationLeash().setMetadata(3, this.mTaskId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldAnimate() {
        if (isOrganized()) {
            return false;
        }
        RecentsAnimationController controller = this.mWmService.getRecentsAnimationController();
        return (controller != null && controller.isAnimatingTask(this) && controller.shouldDeferCancelUntilNextTransition()) ? false : true;
    }

    @Override // com.android.server.wm.WindowContainer
    void setInitialSurfaceControlProperties(SurfaceControl.Builder b) {
        b.setEffectLayer().setMetadata(3, this.mTaskId);
        super.setInitialSurfaceControlProperties(b);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAnimatingByRecents() {
        return isAnimating(4, 8);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowState getTopVisibleAppMainWindow() {
        ActivityRecord activity = getTopVisibleActivity();
        if (activity != null) {
            return activity.findMainWindow();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topRunningNonDelayedActivityLocked(ActivityRecord notTop) {
        PooledPredicate p = PooledLambda.obtainPredicate(new BiPredicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda39
            @Override // java.util.function.BiPredicate
            public final boolean test(Object obj, Object obj2) {
                boolean isTopRunningNonDelayed;
                isTopRunningNonDelayed = Task.isTopRunningNonDelayed((ActivityRecord) obj, (ActivityRecord) obj2);
                return isTopRunningNonDelayed;
            }
        }, PooledLambda.__(ActivityRecord.class), notTop);
        ActivityRecord r = getActivity(p);
        p.recycle();
        return r;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isTopRunningNonDelayed(ActivityRecord r, ActivityRecord notTop) {
        return (r.delayedResume || r == notTop || !r.canBeTopRunning()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topRunningActivity(IBinder token, int taskId) {
        PooledPredicate p = PooledLambda.obtainPredicate(new TriPredicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda8
            public final boolean test(Object obj, Object obj2, Object obj3) {
                boolean isTopRunning;
                isTopRunning = Task.isTopRunning((ActivityRecord) obj, ((Integer) obj2).intValue(), (IBinder) obj3);
                return isTopRunning;
            }
        }, PooledLambda.__(ActivityRecord.class), Integer.valueOf(taskId), token);
        ActivityRecord r = getActivity(p);
        p.recycle();
        return r;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isTopRunning(ActivityRecord r, int taskId, IBinder notTop) {
        return (r.getTask().mTaskId == taskId || r.token == notTop || !r.canBeTopRunning()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopFullscreenActivity() {
        return getActivity(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda48
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.lambda$getTopFullscreenActivity$8((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopFullscreenActivity$8(ActivityRecord r) {
        WindowState win = r.findMainWindow();
        return win != null && win.mAttrs.isFullscreen();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopVisibleActivity$9(ActivityRecord r) {
        return !r.mIsExiting && r.isClientVisible() && r.mVisibleRequested;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopVisibleActivity() {
        return getActivity(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda49
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.lambda$getTopVisibleActivity$9((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopRealVisibleActivity$10(ActivityRecord r) {
        return !r.mIsExiting && r.isClientVisible() && r.isVisible();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopRealVisibleActivity() {
        return getActivity(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.lambda$getTopRealVisibleActivity$10((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopWaitSplashScreenActivity() {
        return getActivity(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda28
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.lambda$getTopWaitSplashScreenActivity$11((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopWaitSplashScreenActivity$11(ActivityRecord r) {
        return r.mHandleExitSplashScreen && r.mTransferringSplashScreenState == 1;
    }

    void positionChildAtTop(ActivityRecord child) {
        positionChildAt(child, Integer.MAX_VALUE);
    }

    void positionChildAt(ActivityRecord child, int position) {
        if (child == null) {
            Slog.w(WmsExt.TAG, "Attempted to position of non-existing app");
        } else {
            positionChildAt(position, child, false);
        }
    }

    void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
        this.mTaskDescription = taskDescription;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSnapshotChanged(TaskSnapshot snapshot) {
        this.mLastTaskSnapshotData.set(snapshot);
        this.mAtmService.getTaskChangeNotificationController().notifyTaskSnapshotChanged(this.mTaskId, snapshot);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManager.TaskDescription getTaskDescription() {
        return this.mTaskDescription;
    }

    @Override // com.android.server.wm.WindowContainer
    int getOrientation(int candidate) {
        if (canSpecifyOrientation()) {
            return super.getOrientation(candidate);
        }
        return -2;
    }

    private boolean canSpecifyOrientation() {
        int windowingMode = getWindowingMode();
        int activityType = getActivityType();
        return windowingMode == 1 || activityType == 2 || activityType == 3 || activityType == 4;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void forAllLeafTasks(Consumer<Task> callback, boolean traverseTopToBottom) {
        int count = this.mChildren.size();
        boolean isLeafTask = true;
        if (traverseTopToBottom) {
            for (int i = count - 1; i >= 0; i--) {
                Task child = ((WindowContainer) this.mChildren.get(i)).asTask();
                if (child != null) {
                    isLeafTask = false;
                    child.forAllLeafTasks(callback, traverseTopToBottom);
                }
            }
        } else {
            for (int i2 = 0; i2 < count; i2++) {
                Task child2 = ((WindowContainer) this.mChildren.get(i2)).asTask();
                if (child2 != null) {
                    isLeafTask = false;
                    child2.forAllLeafTasks(callback, traverseTopToBottom);
                }
            }
        }
        if (isLeafTask) {
            callback.accept(this);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void forAllTasks(Consumer<Task> callback, boolean traverseTopToBottom) {
        super.forAllTasks(callback, traverseTopToBottom);
        callback.accept(this);
    }

    @Override // com.android.server.wm.WindowContainer
    void forAllRootTasks(Consumer<Task> callback, boolean traverseTopToBottom) {
        if (isRootTask()) {
            callback.accept(this);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    boolean forAllTasks(Predicate<Task> callback) {
        if (super.forAllTasks(callback)) {
            return true;
        }
        return callback.test(this);
    }

    @Override // com.android.server.wm.WindowContainer
    boolean forAllLeafTasks(Predicate<Task> callback) {
        boolean isLeafTask = true;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            Task child = ((WindowContainer) this.mChildren.get(i)).asTask();
            if (child != null) {
                isLeafTask = false;
                if (child.forAllLeafTasks(callback)) {
                    return true;
                }
            }
        }
        if (isLeafTask) {
            return callback.test(this);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forAllLeafTasksAndLeafTaskFragments(final Consumer<TaskFragment> callback, final boolean traverseTopToBottom) {
        forAllLeafTasks(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda35
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                Task.lambda$forAllLeafTasksAndLeafTaskFragments$12(callback, traverseTopToBottom, (Task) obj);
            }
        }, traverseTopToBottom);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$forAllLeafTasksAndLeafTaskFragments$12(Consumer callback, boolean traverseTopToBottom, Task task) {
        if (task.isLeafTaskFragment()) {
            callback.accept(task);
            return;
        }
        boolean consumed = false;
        if (traverseTopToBottom) {
            for (int i = task.mChildren.size() - 1; i >= 0; i--) {
                WindowContainer child = (WindowContainer) task.mChildren.get(i);
                if (child.asTaskFragment() != null) {
                    child.forAllLeafTaskFragments(callback, traverseTopToBottom);
                } else if (child.asActivityRecord() != null && !consumed) {
                    callback.accept(task);
                    consumed = true;
                }
            }
            return;
        }
        for (int i2 = 0; i2 < task.mChildren.size(); i2++) {
            WindowContainer child2 = (WindowContainer) task.mChildren.get(i2);
            if (child2.asTaskFragment() != null) {
                child2.forAllLeafTaskFragments(callback, traverseTopToBottom);
            } else if (child2.asActivityRecord() != null && !consumed) {
                callback.accept(task);
                consumed = true;
            }
        }
    }

    @Override // com.android.server.wm.WindowContainer
    boolean forAllRootTasks(Predicate<Task> callback, boolean traverseTopToBottom) {
        if (isRootTask()) {
            return callback.test(this);
        }
        return false;
    }

    @Override // com.android.server.wm.WindowContainer
    Task getTask(Predicate<Task> callback, boolean traverseTopToBottom) {
        Task t = super.getTask(callback, traverseTopToBottom);
        if (t != null) {
            return t;
        }
        if (callback.test(this)) {
            return this;
        }
        return null;
    }

    @Override // com.android.server.wm.WindowContainer
    Task getRootTask(Predicate<Task> callback, boolean traverseTopToBottom) {
        if (isRootTask() && callback.test(this)) {
            return this;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCanAffectSystemUiFlags(boolean canAffectSystemUiFlags) {
        this.mCanAffectSystemUiFlags = canAffectSystemUiFlags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canAffectSystemUiFlags() {
        return this.mCanAffectSystemUiFlags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dontAnimateDimExit() {
        this.mDimmer.dontAnimateExit();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public String getName() {
        return "Task=" + this.mTaskId;
    }

    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    Dimmer getDimmer() {
        if (inMultiWindowMode() || getConfiguration().windowConfiguration.isThunderbackWindow()) {
            return this.mDimmer;
        }
        if (!isRootTask() || isTranslucent(null)) {
            return super.getDimmer();
        }
        return this.mDimmer;
    }

    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    void prepareSurfaces() {
        this.mDimmer.resetDimStates();
        super.prepareSurfaces();
        getDimBounds(this.mTmpDimBoundsRect);
        boolean show = false;
        if (inFreeformWindowingMode()) {
            getBounds(this.mTmpRect);
            Rect rect = this.mTmpDimBoundsRect;
            rect.offsetTo(rect.left - this.mTmpRect.left, this.mTmpDimBoundsRect.top - this.mTmpRect.top);
        } else {
            this.mTmpDimBoundsRect.offsetTo(0, 0);
        }
        SurfaceControl.Transaction t = getSyncTransaction();
        updateShadowsRadius(isFocused(), t);
        if (this.mDimmer.updateDims(t, this.mTmpDimBoundsRect)) {
            scheduleAnimation();
        }
        show = (isVisible() || isAnimating(7)) ? true : true;
        if (this.mSurfaceControl != null && show != this.mLastSurfaceShowing) {
            t.setVisibility(this.mSurfaceControl, show);
        }
        this.mLastSurfaceShowing = show;
    }

    @Override // com.android.server.wm.WindowContainer
    protected void applyAnimationUnchecked(WindowManager.LayoutParams lp, boolean enter, int transit, boolean isVoiceInteraction, final ArrayList<WindowContainer> sources) {
        RecentsAnimationController control = this.mWmService.getRecentsAnimationController();
        if (control != null) {
            if (enter && !isActivityTypeHomeOrRecents()) {
                if (ProtoLogCache.WM_DEBUG_RECENTS_ANIMATIONS_enabled) {
                    String protoLogParam0 = String.valueOf(control);
                    String protoLogParam1 = String.valueOf(asTask());
                    String protoLogParam2 = String.valueOf(AppTransition.appTransitionOldToString(transit));
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_RECENTS_ANIMATIONS, 210750281, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
                }
                control.addTaskToTargets(this, new SurfaceAnimator.OnAnimationFinishedCallback() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda23
                    @Override // com.android.server.wm.SurfaceAnimator.OnAnimationFinishedCallback
                    public final void onAnimationFinished(int i, AnimationAdapter animationAdapter) {
                        Task.lambda$applyAnimationUnchecked$13(sources, i, animationAdapter);
                    }
                });
            } else if (!enter && !isActivityTypeHomeOrRecents() && transit == 25) {
                Slog.d(TAG, "applyAnimationUnchecked " + AppTransition.appTransitionOldToString(transit) + " for " + asTask() + ", enter = false");
                control.mIsTranslucentActivityIsClosing = true;
            }
        } else if (this.mBackGestureStarted) {
            this.mBackGestureStarted = false;
            this.mDisplayContent.mSkipAppTransitionAnimation = true;
            if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
                String protoLogParam02 = String.valueOf(this);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, 1544805551, 0, "Skipping app transition animation. task=%s", new Object[]{protoLogParam02});
            }
        } else {
            super.applyAnimationUnchecked(lp, enter, transit, isVoiceInteraction, sources);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$applyAnimationUnchecked$13(ArrayList sources, int type, AnimationAdapter anim) {
        for (int i = 0; i < sources.size(); i++) {
            ((WindowContainer) sources.get(i)).onAnimationFinished(type, anim);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        super.dump(pw, prefix, dumpAll);
        this.mAnimatingActivityRegistry.dump(pw, "AnimatingApps:", prefix);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void fillTaskInfo(TaskInfo info) {
        fillTaskInfo(info, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void fillTaskInfo(TaskInfo info, boolean stripExtras) {
        fillTaskInfo(info, stripExtras, getDisplayArea());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void fillTaskInfo(final TaskInfo info, boolean stripExtras, TaskDisplayArea tda) {
        Intent cloneFilter;
        ComponentName componentName;
        ComponentName componentName2;
        ActivityInfo activityInfo;
        int i;
        getNumRunningActivities(this.mReuseActivitiesReport);
        info.userId = isLeafTask() ? this.mUserId : this.mCurrentUser;
        info.taskId = this.mTaskId;
        info.displayId = getDisplayId();
        if (tda != null) {
            info.displayAreaFeatureId = tda.mFeatureId;
        }
        boolean z = true;
        info.isRunning = getTopNonFinishingActivity() != null;
        Intent baseIntent = getBaseIntent();
        int baseIntentFlags = baseIntent == null ? 0 : baseIntent.getFlags();
        if (baseIntent == null) {
            cloneFilter = new Intent();
        } else {
            cloneFilter = stripExtras ? baseIntent.cloneFilter() : new Intent(baseIntent);
        }
        info.baseIntent = cloneFilter;
        info.baseIntent.setFlags(baseIntentFlags);
        if (this.mReuseActivitiesReport.base != null) {
            componentName = this.mReuseActivitiesReport.base.intent.getComponent();
        } else {
            componentName = null;
        }
        info.baseActivity = componentName;
        if (this.mReuseActivitiesReport.top != null) {
            componentName2 = this.mReuseActivitiesReport.top.mActivityComponent;
        } else {
            componentName2 = null;
        }
        info.topActivity = componentName2;
        info.origActivity = this.origActivity;
        info.realActivity = this.realActivity;
        info.numActivities = this.mReuseActivitiesReport.numActivities;
        info.lastActiveTime = this.lastActiveTime;
        info.taskDescription = new ActivityManager.TaskDescription(getTaskDescription());
        info.supportsSplitScreenMultiWindow = supportsSplitScreenWindowingModeInDisplayArea(tda);
        info.supportsMultiWindow = supportsMultiWindowInDisplayArea(tda);
        info.configuration.setTo(getConfiguration());
        info.configuration.windowConfiguration.setActivityType(getActivityType());
        info.configuration.windowConfiguration.setWindowingMode(getWindowingMode());
        info.token = this.mRemoteToken.toWindowContainerToken();
        Task top = getTopMostTask();
        info.resizeMode = top != null ? top.mResizeMode : this.mResizeMode;
        info.topActivityType = top.getActivityType();
        info.isResizeable = isResizeable();
        info.minWidth = this.mMinWidth;
        info.minHeight = this.mMinHeight;
        info.defaultMinSize = this.mDisplayContent == null ? 220 : this.mDisplayContent.mMinSizeOfResizeableTaskDp;
        info.positionInParent = getRelativePosition();
        info.pictureInPictureParams = getPictureInPictureParams(top);
        info.shouldDockBigOverlays = shouldDockBigOverlays();
        if (info.pictureInPictureParams != null && info.pictureInPictureParams.isLaunchIntoPip() && top.getTopMostActivity().getLastParentBeforePip() != null) {
            info.launchIntoPipHostTaskId = top.getTopMostActivity().getLastParentBeforePip().mTaskId;
        }
        info.displayCutoutInsets = top != null ? top.getDisplayCutoutInsets() : null;
        if (this.mReuseActivitiesReport.top != null) {
            activityInfo = this.mReuseActivitiesReport.top.info;
        } else {
            activityInfo = null;
        }
        info.topActivityInfo = activityInfo;
        boolean isTopActivityResumed = this.mReuseActivitiesReport.top != null && this.mReuseActivitiesReport.top.getOrganizedTask() == this && this.mReuseActivitiesReport.top.isState(ActivityRecord.State.RESUMED);
        info.topActivityInSizeCompat = isTopActivityResumed && this.mReuseActivitiesReport.top.inSizeCompatMode();
        if (!isTopActivityResumed || !this.mReuseActivitiesReport.top.isEligibleForLetterboxEducation()) {
            z = false;
        }
        info.topActivityEligibleForLetterboxEducation = z;
        info.cameraCompatControlState = isTopActivityResumed ? this.mReuseActivitiesReport.top.getCameraCompatControlState() : 0;
        info.launchCookies.clear();
        info.addLaunchCookie(this.mLaunchCookie);
        forAllActivities(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda27
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                info.addLaunchCookie(((ActivityRecord) obj).mLaunchCookie);
            }
        });
        Task parentTask = getParent() != null ? getParent().asTask() : null;
        if (parentTask != null && parentTask.mCreatedByOrganizer) {
            i = parentTask.mTaskId;
        } else {
            i = -1;
        }
        info.parentTaskId = i;
        info.isFocused = isFocused();
        info.isVisible = hasVisibleChildren();
        info.isSleeping = shouldSleepActivities();
        ActivityRecord topRecord = getTopNonFinishingActivity();
        info.mTopActivityLocusId = topRecord != null ? topRecord.getLocusId() : null;
        ITaskLice.instance().fillTaskInfo(info);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$trimIneffectiveInfo$15(ActivityRecord r) {
        return !r.finishing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void trimIneffectiveInfo(Task task, TaskInfo info) {
        ActivityRecord baseActivity = task.getActivity(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda12
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.lambda$trimIneffectiveInfo$15((ActivityRecord) obj);
            }
        }, false);
        int baseActivityUid = baseActivity != null ? baseActivity.getUid() : task.effectiveUid;
        if (info.topActivityInfo != null && task.effectiveUid != info.topActivityInfo.applicationInfo.uid) {
            info.topActivityInfo = new ActivityInfo(info.topActivityInfo);
            info.topActivityInfo.applicationInfo = new ApplicationInfo(info.topActivityInfo.applicationInfo);
            info.topActivity = new ComponentName("", "");
            info.topActivityInfo.packageName = "";
            info.topActivityInfo.taskAffinity = "";
            info.topActivityInfo.processName = "";
            info.topActivityInfo.name = "";
            info.topActivityInfo.parentActivityName = "";
            info.topActivityInfo.targetActivity = "";
            info.topActivityInfo.splitName = "";
            info.topActivityInfo.applicationInfo.className = "";
            info.topActivityInfo.applicationInfo.credentialProtectedDataDir = "";
            info.topActivityInfo.applicationInfo.dataDir = "";
            info.topActivityInfo.applicationInfo.deviceProtectedDataDir = "";
            info.topActivityInfo.applicationInfo.manageSpaceActivityName = "";
            info.topActivityInfo.applicationInfo.nativeLibraryDir = "";
            info.topActivityInfo.applicationInfo.nativeLibraryRootDir = "";
            info.topActivityInfo.applicationInfo.processName = "";
            info.topActivityInfo.applicationInfo.publicSourceDir = "";
            info.topActivityInfo.applicationInfo.scanPublicSourceDir = "";
            info.topActivityInfo.applicationInfo.scanSourceDir = "";
            info.topActivityInfo.applicationInfo.sourceDir = "";
            info.topActivityInfo.applicationInfo.taskAffinity = "";
            info.topActivityInfo.applicationInfo.name = "";
            info.topActivityInfo.applicationInfo.packageName = "";
        }
        if (task.effectiveUid != baseActivityUid) {
            info.baseActivity = new ComponentName("", "");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PictureInPictureParams getPictureInPictureParams() {
        return getPictureInPictureParams(getTopMostTask());
    }

    private PictureInPictureParams getPictureInPictureParams(Task top) {
        ActivityRecord topMostActivity;
        if (top == null || (topMostActivity = top.getTopMostActivity()) == null || topMostActivity.pictureInPictureArgs.empty()) {
            return null;
        }
        return new PictureInPictureParams(topMostActivity.pictureInPictureArgs);
    }

    private boolean shouldDockBigOverlays() {
        ActivityRecord topMostActivity = getTopMostActivity();
        return topMostActivity != null && topMostActivity.shouldDockBigOverlays;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getDisplayCutoutInsets() {
        int displayCutoutMode;
        if (this.mDisplayContent == null || getDisplayInfo().displayCutout == null) {
            return null;
        }
        WindowState w = getTopVisibleAppMainWindow();
        if (w == null) {
            displayCutoutMode = 0;
        } else {
            displayCutoutMode = w.getAttrs().layoutInDisplayCutoutMode;
        }
        if (displayCutoutMode == 3 || displayCutoutMode == 1) {
            return null;
        }
        return getDisplayInfo().displayCutout.getSafeInsets();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManager.RunningTaskInfo getTaskInfo() {
        ActivityManager.RunningTaskInfo info = new ActivityManager.RunningTaskInfo();
        fillTaskInfo(info);
        return info;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public StartingWindowInfo getStartingWindowInfo(ActivityRecord activity) {
        WindowState topFullscreenOpaqueWindow;
        WindowState topMainWin;
        StartingWindowInfo info = new StartingWindowInfo();
        info.taskInfo = getTaskInfo();
        info.targetActivityInfo = (info.taskInfo.topActivityInfo == null || activity.info == info.taskInfo.topActivityInfo) ? null : activity.info;
        info.isKeyguardOccluded = this.mAtmService.mKeyguardController.isDisplayOccluded(0);
        info.startingWindowTypeParameter = activity.mStartingData.mTypeParams;
        if ((info.startingWindowTypeParameter & 16) != 0 && (topMainWin = getWindow(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda6
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.lambda$getStartingWindowInfo$16((WindowState) obj);
            }
        })) != null) {
            info.mainWindowLayoutParams = topMainWin.getAttrs();
            info.requestedVisibilities.set(topMainWin.getRequestedVisibilities());
        }
        info.taskInfo.configuration.setTo(activity.getConfiguration());
        ActivityRecord topFullscreenActivity = getTopFullscreenActivity();
        if (topFullscreenActivity != null && (topFullscreenOpaqueWindow = topFullscreenActivity.getTopFullscreenOpaqueWindow()) != null) {
            info.topOpaqueWindowInsetsState = topFullscreenOpaqueWindow.getInsetsStateWithVisibilityOverride();
            info.topOpaqueWindowLayoutParams = topFullscreenOpaqueWindow.getAttrs();
        }
        return info;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getStartingWindowInfo$16(WindowState w) {
        return w.mAttrs.type == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTaskId(int taskId) {
        return this.mTaskId == taskId;
    }

    @Override // com.android.server.wm.WindowContainer
    Task asTask() {
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord isInTask(ActivityRecord r) {
        if (r == null || !r.isDescendantOf(this)) {
            return null;
        }
        return r;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("userId=");
        pw.print(this.mUserId);
        pw.print(" effectiveUid=");
        UserHandle.formatUid(pw, this.effectiveUid);
        pw.print(" mCallingUid=");
        UserHandle.formatUid(pw, this.mCallingUid);
        pw.print(" mUserSetupComplete=");
        pw.print(this.mUserSetupComplete);
        pw.print(" mCallingPackage=");
        pw.print(this.mCallingPackage);
        pw.print(" mCallingFeatureId=");
        pw.println(this.mCallingFeatureId);
        if (this.affinity != null || this.rootAffinity != null) {
            pw.print(prefix);
            pw.print("affinity=");
            pw.print(this.affinity);
            String str = this.affinity;
            if (str == null || !str.equals(this.rootAffinity)) {
                pw.print(" root=");
                pw.println(this.rootAffinity);
            } else {
                pw.println();
            }
        }
        if (this.mWindowLayoutAffinity != null) {
            pw.print(prefix);
            pw.print("windowLayoutAffinity=");
            pw.println(this.mWindowLayoutAffinity);
        }
        if (this.voiceSession != null || this.voiceInteractor != null) {
            pw.print(prefix);
            pw.print("VOICE: session=0x");
            pw.print(Integer.toHexString(System.identityHashCode(this.voiceSession)));
            pw.print(" interactor=0x");
            pw.println(Integer.toHexString(System.identityHashCode(this.voiceInteractor)));
        }
        if (this.intent != null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(prefix);
            sb.append("intent={");
            this.intent.toShortString(sb, false, true, false, false);
            sb.append('}');
            pw.println(sb.toString());
        }
        if (this.affinityIntent != null) {
            StringBuilder sb2 = new StringBuilder(128);
            sb2.append(prefix);
            sb2.append("affinityIntent={");
            this.affinityIntent.toShortString(sb2, false, true, false, false);
            sb2.append('}');
            pw.println(sb2.toString());
        }
        if (this.origActivity != null) {
            pw.print(prefix);
            pw.print("origActivity=");
            pw.println(this.origActivity.flattenToShortString());
        }
        if (this.realActivity != null) {
            pw.print(prefix);
            pw.print("mActivityComponent=");
            pw.println(this.realActivity.flattenToShortString());
        }
        if (this.autoRemoveRecents || this.isPersistable || !isActivityTypeStandard()) {
            pw.print(prefix);
            pw.print("autoRemoveRecents=");
            pw.print(this.autoRemoveRecents);
            pw.print(" isPersistable=");
            pw.print(this.isPersistable);
            pw.print(" activityType=");
            pw.println(getActivityType());
        }
        if (this.rootWasReset || this.mNeverRelinquishIdentity || this.mReuseTask || this.mLockTaskAuth != 1) {
            pw.print(prefix);
            pw.print("rootWasReset=");
            pw.print(this.rootWasReset);
            pw.print(" mNeverRelinquishIdentity=");
            pw.print(this.mNeverRelinquishIdentity);
            pw.print(" mReuseTask=");
            pw.print(this.mReuseTask);
            pw.print(" mLockTaskAuth=");
            pw.println(lockTaskAuthToString());
        }
        if (this.mAffiliatedTaskId != this.mTaskId || this.mPrevAffiliateTaskId != -1 || this.mPrevAffiliate != null || this.mNextAffiliateTaskId != -1 || this.mNextAffiliate != null) {
            pw.print(prefix);
            pw.print("affiliation=");
            pw.print(this.mAffiliatedTaskId);
            pw.print(" prevAffiliation=");
            pw.print(this.mPrevAffiliateTaskId);
            pw.print(" (");
            Task task = this.mPrevAffiliate;
            if (task == null) {
                pw.print("null");
            } else {
                pw.print(Integer.toHexString(System.identityHashCode(task)));
            }
            pw.print(") nextAffiliation=");
            pw.print(this.mNextAffiliateTaskId);
            pw.print(" (");
            Task task2 = this.mNextAffiliate;
            if (task2 == null) {
                pw.print("null");
            } else {
                pw.print(Integer.toHexString(System.identityHashCode(task2)));
            }
            pw.println(")");
        }
        pw.print(prefix);
        pw.print("Activities=");
        pw.println(this.mChildren);
        if (!this.askedCompatMode || !this.inRecents || !this.isAvailable) {
            pw.print(prefix);
            pw.print("askedCompatMode=");
            pw.print(this.askedCompatMode);
            pw.print(" inRecents=");
            pw.print(this.inRecents);
            pw.print(" isAvailable=");
            pw.println(this.isAvailable);
        }
        if (this.lastDescription != null) {
            pw.print(prefix);
            pw.print("lastDescription=");
            pw.println(this.lastDescription);
        }
        if (this.mRootProcess != null) {
            pw.print(prefix);
            pw.print("mRootProcess=");
            pw.println(this.mRootProcess);
        }
        if (this.mSharedStartingData != null) {
            pw.println(prefix + "mSharedStartingData=" + this.mSharedStartingData);
        }
        pw.print(prefix);
        pw.print("taskId=" + this.mTaskId);
        pw.println(" rootTaskId=" + getRootTaskId());
        pw.print(prefix);
        pw.println("hasChildPipActivity=" + (this.mChildPipActivity != null));
        pw.print(prefix);
        pw.print("mHasBeenVisible=");
        pw.println(getHasBeenVisible());
        pw.print(prefix);
        pw.print("mResizeMode=");
        pw.print(ActivityInfo.resizeModeToString(this.mResizeMode));
        pw.print(" mSupportsPictureInPicture=");
        pw.print(this.mSupportsPictureInPicture);
        pw.print(" isResizeable=");
        pw.println(isResizeable());
        pw.print(prefix);
        pw.print("lastActiveTime=");
        pw.print(this.lastActiveTime);
        pw.println(" (inactive for " + (getInactiveDuration() / 1000) + "s)");
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        String str = this.stringName;
        if (str != null) {
            sb.append(str);
            sb.append(" U=");
            sb.append(this.mUserId);
            Task rootTask = getRootTask();
            if (rootTask != this) {
                sb.append(" rootTaskId=");
                sb.append(rootTask.mTaskId);
            }
            sb.append(" visible=");
            sb.append(shouldBeVisible(null));
            sb.append(" visibleRequested=");
            sb.append(isVisibleRequested());
            sb.append(" mode=");
            sb.append(WindowConfiguration.windowingModeToString(getWindowingMode()));
            sb.append(" translucent=");
            sb.append(isTranslucent(null));
            sb.append(" sz=");
            sb.append(getChildCount());
            sb.append('}');
            return sb.toString();
        }
        sb.append("Task{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" #");
        sb.append(this.mTaskId);
        sb.append(" type=" + WindowConfiguration.activityTypeToString(getActivityType()));
        if (this.affinity != null) {
            sb.append(" A=");
            sb.append(this.affinity);
        } else {
            Intent intent = this.intent;
            if (intent != null && intent.getComponent() != null) {
                sb.append(" I=");
                sb.append(this.intent.getComponent().flattenToShortString());
            } else {
                Intent intent2 = this.affinityIntent;
                if (intent2 != null && intent2.getComponent() != null) {
                    sb.append(" aI=");
                    sb.append(this.affinityIntent.getComponent().flattenToShortString());
                } else {
                    sb.append(" ??");
                }
            }
        }
        this.stringName = sb.toString();
        return toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class TaskActivitiesReport implements Consumer<ActivityRecord> {
        ActivityRecord base;
        int numActivities;
        int numRunning;
        ActivityRecord top;

        TaskActivitiesReport() {
        }

        void reset() {
            this.numActivities = 0;
            this.numRunning = 0;
            this.base = null;
            this.top = null;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(ActivityRecord r) {
            if (r.finishing) {
                return;
            }
            this.base = r;
            this.numActivities++;
            ActivityRecord activityRecord = this.top;
            if (activityRecord == null || activityRecord.isState(ActivityRecord.State.INITIALIZING)) {
                this.top = r;
                this.numRunning = 0;
            }
            if (r.attachedToProcess()) {
                this.numRunning++;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveToXml(TypedXmlSerializer out) throws Exception {
        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
            Slog.i(TAG_RECENTS, "Saving task=" + this);
        }
        out.attributeInt((String) null, ATTR_TASKID, this.mTaskId);
        ComponentName componentName = this.realActivity;
        if (componentName != null) {
            out.attribute((String) null, ATTR_REALACTIVITY, componentName.flattenToShortString());
        }
        out.attributeBoolean((String) null, ATTR_REALACTIVITY_SUSPENDED, this.realActivitySuspended);
        ComponentName componentName2 = this.origActivity;
        if (componentName2 != null) {
            out.attribute((String) null, ATTR_ORIGACTIVITY, componentName2.flattenToShortString());
        }
        String str = this.affinity;
        if (str != null) {
            out.attribute((String) null, ATTR_AFFINITY, str);
            if (!this.affinity.equals(this.rootAffinity)) {
                String str2 = this.rootAffinity;
                out.attribute((String) null, ATTR_ROOT_AFFINITY, str2 != null ? str2 : "@");
            }
        } else {
            String str3 = this.rootAffinity;
            if (str3 != null) {
                out.attribute((String) null, ATTR_ROOT_AFFINITY, str3 != null ? str3 : "@");
            }
        }
        String str4 = this.mWindowLayoutAffinity;
        if (str4 != null) {
            out.attribute((String) null, ATTR_WINDOW_LAYOUT_AFFINITY, str4);
        }
        out.attributeBoolean((String) null, ATTR_ROOTHASRESET, this.rootWasReset);
        out.attributeBoolean((String) null, ATTR_AUTOREMOVERECENTS, this.autoRemoveRecents);
        out.attributeBoolean((String) null, ATTR_ASKEDCOMPATMODE, this.askedCompatMode);
        out.attributeInt((String) null, ATTR_USERID, this.mUserId);
        out.attributeBoolean((String) null, ATTR_USER_SETUP_COMPLETE, this.mUserSetupComplete);
        out.attributeInt((String) null, ATTR_EFFECTIVE_UID, this.effectiveUid);
        out.attributeLong((String) null, ATTR_LASTTIMEMOVED, this.mLastTimeMoved);
        out.attributeBoolean((String) null, ATTR_NEVERRELINQUISH, this.mNeverRelinquishIdentity);
        CharSequence charSequence = this.lastDescription;
        if (charSequence != null) {
            out.attribute((String) null, ATTR_LASTDESCRIPTION, charSequence.toString());
        }
        if (getTaskDescription() != null) {
            getTaskDescription().saveToXml(out);
        }
        out.attributeInt((String) null, ATTR_TASK_AFFILIATION, this.mAffiliatedTaskId);
        out.attributeInt((String) null, ATTR_PREV_AFFILIATION, this.mPrevAffiliateTaskId);
        out.attributeInt((String) null, ATTR_NEXT_AFFILIATION, this.mNextAffiliateTaskId);
        out.attributeInt((String) null, ATTR_CALLING_UID, this.mCallingUid);
        String str5 = this.mCallingPackage;
        if (str5 == null) {
            str5 = "";
        }
        out.attribute((String) null, ATTR_CALLING_PACKAGE, str5);
        String str6 = this.mCallingFeatureId;
        out.attribute((String) null, ATTR_CALLING_FEATURE_ID, str6 != null ? str6 : "");
        out.attributeInt((String) null, ATTR_RESIZE_MODE, this.mResizeMode);
        out.attributeBoolean((String) null, ATTR_SUPPORTS_PICTURE_IN_PICTURE, this.mSupportsPictureInPicture);
        Rect rect = this.mLastNonFullscreenBounds;
        if (rect != null) {
            out.attribute((String) null, ATTR_NON_FULLSCREEN_BOUNDS, rect.flattenToString());
        }
        out.attributeInt((String) null, ATTR_MIN_WIDTH, this.mMinWidth);
        out.attributeInt((String) null, ATTR_MIN_HEIGHT, this.mMinHeight);
        out.attributeInt((String) null, ATTR_PERSIST_TASK_VERSION, 1);
        if (this.mLastTaskSnapshotData.taskSize != null) {
            out.attribute((String) null, ATTR_LAST_SNAPSHOT_TASK_SIZE, this.mLastTaskSnapshotData.taskSize.flattenToString());
        }
        if (this.mLastTaskSnapshotData.contentInsets != null) {
            out.attribute((String) null, ATTR_LAST_SNAPSHOT_CONTENT_INSETS, this.mLastTaskSnapshotData.contentInsets.flattenToString());
        }
        if (this.mLastTaskSnapshotData.bufferSize != null) {
            out.attribute((String) null, ATTR_LAST_SNAPSHOT_BUFFER_SIZE, this.mLastTaskSnapshotData.bufferSize.flattenToString());
        }
        if (this.affinityIntent != null) {
            out.startTag((String) null, TAG_AFFINITYINTENT);
            this.affinityIntent.saveToXml(out);
            out.endTag((String) null, TAG_AFFINITYINTENT);
        }
        if (this.intent != null) {
            out.startTag((String) null, TAG_INTENT);
            this.intent.saveToXml(out);
            out.endTag((String) null, TAG_INTENT);
        }
        sTmpException = null;
        PooledPredicate f = PooledLambda.obtainPredicate(new TriPredicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda26
            public final boolean test(Object obj, Object obj2, Object obj3) {
                boolean saveActivityToXml;
                saveActivityToXml = Task.saveActivityToXml((ActivityRecord) obj, (ActivityRecord) obj2, (TypedXmlSerializer) obj3);
                return saveActivityToXml;
            }
        }, PooledLambda.__(ActivityRecord.class), getBottomMostActivity(), out);
        forAllActivities((Predicate<ActivityRecord>) f);
        f.recycle();
        Exception exc = sTmpException;
        if (exc != null) {
            throw exc;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean saveActivityToXml(ActivityRecord r, ActivityRecord first, TypedXmlSerializer out) {
        if (r.info.persistableMode == 0 || !r.isPersistable() || (((r.intent.getFlags() & 524288) | 8192) == 524288 && r != first)) {
            return true;
        }
        try {
            out.startTag((String) null, "activity");
            r.saveToXml(out);
            out.endTag((String) null, "activity");
            return false;
        } catch (Exception e) {
            sTmpException = e;
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static Task restoreFromXml(TypedXmlPullParser in, ActivityTaskSupervisor taskSupervisor) throws IOException, XmlPullParserException {
        ActivityManager.TaskDescription taskDescription;
        String rootAffinity;
        int effectiveUid;
        int effectiveUid2;
        boolean supportsPictureInPicture;
        int effectiveUid3;
        char c;
        ComponentName componentName;
        ArrayList<ActivityRecord> activities = new ArrayList<>();
        int effectiveUid4 = -1;
        int outerDepth = in.getDepth();
        ActivityManager.TaskDescription taskDescription2 = new ActivityManager.TaskDescription();
        ActivityManager.RecentTaskInfo.PersistedTaskSnapshotData lastSnapshotData = new ActivityManager.RecentTaskInfo.PersistedTaskSnapshotData();
        boolean supportsPictureInPicture2 = false;
        int taskType = 0;
        int userId = 0;
        boolean userSetupComplete = true;
        String lastDescription = null;
        long lastTimeOnTop = 0;
        boolean neverRelinquishIdentity = true;
        int taskId = -1;
        int taskAffiliation = -1;
        int prevTaskId = -1;
        int nextTaskId = -1;
        int callingUid = -1;
        String callingPackage = "";
        String callingFeatureId = null;
        Rect lastNonFullscreenBounds = null;
        int minWidth = -1;
        int minHeight = -1;
        boolean realActivitySuspended = false;
        String windowLayoutAffinity = null;
        boolean askedCompatMode = false;
        int attrNdx = in.getAttributeCount() - 1;
        ComponentName realActivity = null;
        boolean hasRootAffinity = false;
        boolean autoRemoveRecents = false;
        String rootAffinity2 = null;
        boolean rootHasReset = false;
        int persistTaskVersion = 0;
        String rootAffinity3 = null;
        ComponentName origActivity = null;
        int resizeMode = 4;
        while (true) {
            boolean askedCompatMode2 = askedCompatMode;
            boolean autoRemoveRecents2 = autoRemoveRecents;
            if (attrNdx >= 0) {
                String attrName = in.getAttributeName(attrNdx);
                String attrValue = in.getAttributeValue(attrNdx);
                boolean rootHasReset2 = rootHasReset;
                switch (attrName.hashCode()) {
                    case -2134816935:
                        if (attrName.equals(ATTR_ASKEDCOMPATMODE)) {
                            c = '\t';
                            break;
                        }
                        c = 65535;
                        break;
                    case -1588736338:
                        if (attrName.equals(ATTR_LAST_SNAPSHOT_CONTENT_INSETS)) {
                            c = 30;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1556983798:
                        if (attrName.equals(ATTR_LASTTIMEMOVED)) {
                            c = 15;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1537240555:
                        if (attrName.equals(ATTR_TASKID)) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1494902876:
                        if (attrName.equals(ATTR_NEXT_AFFILIATION)) {
                            c = 19;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1138503444:
                        if (attrName.equals(ATTR_REALACTIVITY_SUSPENDED)) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1124927690:
                        if (attrName.equals(ATTR_TASK_AFFILIATION)) {
                            c = 17;
                            break;
                        }
                        c = 65535;
                        break;
                    case -974080081:
                        if (attrName.equals(ATTR_USER_SETUP_COMPLETE)) {
                            c = 11;
                            break;
                        }
                        c = 65535;
                        break;
                    case -929566280:
                        if (attrName.equals(ATTR_EFFECTIVE_UID)) {
                            c = '\f';
                            break;
                        }
                        c = 65535;
                        break;
                    case -865458610:
                        if (attrName.equals(ATTR_RESIZE_MODE)) {
                            c = 23;
                            break;
                        }
                        c = 65535;
                        break;
                    case -826243148:
                        if (attrName.equals(ATTR_MIN_HEIGHT)) {
                            c = 27;
                            break;
                        }
                        c = 65535;
                        break;
                    case -801863159:
                        if (attrName.equals(ATTR_LAST_SNAPSHOT_TASK_SIZE)) {
                            c = 29;
                            break;
                        }
                        c = 65535;
                        break;
                    case -707249465:
                        if (attrName.equals(ATTR_NON_FULLSCREEN_BOUNDS)) {
                            c = 25;
                            break;
                        }
                        c = 65535;
                        break;
                    case -705269939:
                        if (attrName.equals(ATTR_ORIGACTIVITY)) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case -551322450:
                        if (attrName.equals(ATTR_LAST_SNAPSHOT_BUFFER_SIZE)) {
                            c = 31;
                            break;
                        }
                        c = 65535;
                        break;
                    case -502399667:
                        if (attrName.equals(ATTR_AUTOREMOVERECENTS)) {
                            c = '\b';
                            break;
                        }
                        c = 65535;
                        break;
                    case -360792224:
                        if (attrName.equals(ATTR_SUPPORTS_PICTURE_IN_PICTURE)) {
                            c = 24;
                            break;
                        }
                        c = 65535;
                        break;
                    case -162744347:
                        if (attrName.equals(ATTR_ROOT_AFFINITY)) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    case -147132913:
                        if (attrName.equals(ATTR_USERID)) {
                            c = '\n';
                            break;
                        }
                        c = 65535;
                        break;
                    case -132216235:
                        if (attrName.equals(ATTR_CALLING_UID)) {
                            c = 20;
                            break;
                        }
                        c = 65535;
                        break;
                    case 180927924:
                        if (attrName.equals(ATTR_TASKTYPE)) {
                            c = '\r';
                            break;
                        }
                        c = 65535;
                        break;
                    case 331206372:
                        if (attrName.equals(ATTR_PREV_AFFILIATION)) {
                            c = 18;
                            break;
                        }
                        c = 65535;
                        break;
                    case 394454367:
                        if (attrName.equals(ATTR_CALLING_FEATURE_ID)) {
                            c = 22;
                            break;
                        }
                        c = 65535;
                        break;
                    case 541503897:
                        if (attrName.equals(ATTR_MIN_WIDTH)) {
                            c = 26;
                            break;
                        }
                        c = 65535;
                        break;
                    case 605497640:
                        if (attrName.equals(ATTR_AFFINITY)) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 869221331:
                        if (attrName.equals(ATTR_LASTDESCRIPTION)) {
                            c = 14;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1007873193:
                        if (attrName.equals(ATTR_PERSIST_TASK_VERSION)) {
                            c = 28;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1081438155:
                        if (attrName.equals(ATTR_CALLING_PACKAGE)) {
                            c = 21;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1457608782:
                        if (attrName.equals(ATTR_NEVERRELINQUISH)) {
                            c = 16;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1539554448:
                        if (attrName.equals(ATTR_REALACTIVITY)) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1999609934:
                        if (attrName.equals(ATTR_WINDOW_LAYOUT_AFFINITY)) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case 2023391309:
                        if (attrName.equals(ATTR_ROOTHASRESET)) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        ActivityManager.RecentTaskInfo.PersistedTaskSnapshotData lastSnapshotData2 = lastSnapshotData;
                        if (taskId != -1) {
                            componentName = origActivity;
                            lastSnapshotData = lastSnapshotData2;
                            break;
                        } else {
                            taskId = Integer.parseInt(attrValue);
                            lastSnapshotData = lastSnapshotData2;
                            askedCompatMode = askedCompatMode2;
                            autoRemoveRecents = autoRemoveRecents2;
                            rootHasReset = rootHasReset2;
                            continue;
                            attrNdx--;
                        }
                    case 1:
                        realActivity = ComponentName.unflattenFromString(attrValue);
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 2:
                        boolean realActivitySuspended2 = Boolean.valueOf(attrValue).booleanValue();
                        realActivitySuspended = realActivitySuspended2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 3:
                        origActivity = ComponentName.unflattenFromString(attrValue);
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 4:
                        rootAffinity3 = attrValue;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 5:
                        rootAffinity2 = attrValue;
                        hasRootAffinity = true;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 6:
                        windowLayoutAffinity = attrValue;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 7:
                        rootHasReset = Boolean.parseBoolean(attrValue);
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        continue;
                        attrNdx--;
                    case '\b':
                        boolean autoRemoveRecents3 = Boolean.parseBoolean(attrValue);
                        autoRemoveRecents = autoRemoveRecents3;
                        askedCompatMode = askedCompatMode2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case '\t':
                        boolean askedCompatMode3 = Boolean.parseBoolean(attrValue);
                        autoRemoveRecents = autoRemoveRecents2;
                        askedCompatMode = askedCompatMode3;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case '\n':
                        int userId2 = Integer.parseInt(attrValue);
                        userId = userId2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 11:
                        boolean userSetupComplete2 = Boolean.parseBoolean(attrValue);
                        userSetupComplete = userSetupComplete2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case '\f':
                        int effectiveUid5 = Integer.parseInt(attrValue);
                        effectiveUid4 = effectiveUid5;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case '\r':
                        int taskType2 = Integer.parseInt(attrValue);
                        taskType = taskType2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 14:
                        lastDescription = attrValue;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 15:
                        long lastTimeOnTop2 = Long.parseLong(attrValue);
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        lastTimeOnTop = lastTimeOnTop2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 16:
                        boolean neverRelinquishIdentity2 = Boolean.parseBoolean(attrValue);
                        neverRelinquishIdentity = neverRelinquishIdentity2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 17:
                        int taskAffiliation2 = Integer.parseInt(attrValue);
                        taskAffiliation = taskAffiliation2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 18:
                        int prevTaskId2 = Integer.parseInt(attrValue);
                        prevTaskId = prevTaskId2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 19:
                        int nextTaskId2 = Integer.parseInt(attrValue);
                        nextTaskId = nextTaskId2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 20:
                        int callingUid2 = Integer.parseInt(attrValue);
                        callingUid = callingUid2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 21:
                        callingPackage = attrValue;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 22:
                        callingFeatureId = attrValue;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 23:
                        resizeMode = Integer.parseInt(attrValue);
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 24:
                        boolean supportsPictureInPicture3 = Boolean.parseBoolean(attrValue);
                        supportsPictureInPicture2 = supportsPictureInPicture3;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 25:
                        Rect lastNonFullscreenBounds2 = Rect.unflattenFromString(attrValue);
                        lastNonFullscreenBounds = lastNonFullscreenBounds2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 26:
                        int minWidth2 = Integer.parseInt(attrValue);
                        minWidth = minWidth2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 27:
                        int minHeight2 = Integer.parseInt(attrValue);
                        minHeight = minHeight2;
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 28:
                        persistTaskVersion = Integer.parseInt(attrValue);
                        askedCompatMode = askedCompatMode2;
                        autoRemoveRecents = autoRemoveRecents2;
                        rootHasReset = rootHasReset2;
                        continue;
                        attrNdx--;
                    case 29:
                        lastSnapshotData.taskSize = Point.unflattenFromString(attrValue);
                        componentName = origActivity;
                        break;
                    case 30:
                        lastSnapshotData.contentInsets = Rect.unflattenFromString(attrValue);
                        componentName = origActivity;
                        break;
                    case 31:
                        lastSnapshotData.bufferSize = Point.unflattenFromString(attrValue);
                        componentName = origActivity;
                        break;
                    default:
                        if (attrName.startsWith("task_description_")) {
                            componentName = origActivity;
                            break;
                        } else {
                            componentName = origActivity;
                            Slog.w(TAG, "Task: Unknown attribute=" + attrName);
                            break;
                        }
                }
                askedCompatMode = askedCompatMode2;
                autoRemoveRecents = autoRemoveRecents2;
                origActivity = componentName;
                rootHasReset = rootHasReset2;
                attrNdx--;
            } else {
                ComponentName origActivity2 = origActivity;
                boolean rootHasReset3 = rootHasReset;
                ActivityManager.TaskDescription taskDescription3 = taskDescription2;
                taskDescription3.restoreFromXml(in);
                Intent intent = null;
                Intent affinityIntent = null;
                while (true) {
                    int event = in.next();
                    if (event != 1) {
                        taskDescription = taskDescription3;
                        if (event != 3 || in.getDepth() >= outerDepth) {
                            if (event == 2) {
                                String name = in.getName();
                                if (TAG_AFFINITYINTENT.equals(name)) {
                                    affinityIntent = Intent.restoreFromXml(in);
                                } else if (TAG_INTENT.equals(name)) {
                                    intent = Intent.restoreFromXml(in);
                                } else if ("activity".equals(name)) {
                                    ActivityRecord activity = ActivityRecord.restoreFromXml(in, taskSupervisor);
                                    if (activity != null) {
                                        activities.add(activity);
                                    }
                                } else {
                                    Slog.e(TAG, "restoreTask: Unexpected name=" + name);
                                    XmlUtils.skipCurrentTag(in);
                                }
                                taskDescription3 = taskDescription;
                            } else {
                                taskDescription3 = taskDescription;
                            }
                        }
                    } else {
                        taskDescription = taskDescription3;
                    }
                }
                if (!hasRootAffinity) {
                    rootAffinity2 = rootAffinity3;
                } else if ("@".equals(rootAffinity2)) {
                    rootAffinity2 = null;
                }
                if (effectiveUid4 <= 0) {
                    Intent checkIntent = intent != null ? intent : affinityIntent;
                    if (checkIntent == null) {
                        effectiveUid3 = 0;
                        rootAffinity = rootAffinity2;
                        effectiveUid = userId;
                    } else {
                        IPackageManager pm = AppGlobals.getPackageManager();
                        try {
                            rootAffinity = rootAffinity2;
                            effectiveUid3 = 0;
                            effectiveUid = userId;
                            try {
                                ApplicationInfo ai = pm.getApplicationInfo(checkIntent.getComponent().getPackageName(), 8704L, effectiveUid);
                                if (ai != null) {
                                    effectiveUid3 = ai.uid;
                                }
                                effectiveUid2 = effectiveUid3;
                            } catch (RemoteException e) {
                            }
                        } catch (RemoteException e2) {
                            effectiveUid3 = 0;
                            rootAffinity = rootAffinity2;
                            effectiveUid = userId;
                        }
                        Slog.w(TAG, "Updating task #" + taskId + " for " + checkIntent + ": effectiveUid=" + effectiveUid2);
                    }
                    effectiveUid2 = effectiveUid3;
                    Slog.w(TAG, "Updating task #" + taskId + " for " + checkIntent + ": effectiveUid=" + effectiveUid2);
                } else {
                    rootAffinity = rootAffinity2;
                    effectiveUid = userId;
                    effectiveUid2 = effectiveUid4;
                }
                if (persistTaskVersion < 1) {
                    if (taskType == 1 && resizeMode == 2) {
                        resizeMode = 1;
                        supportsPictureInPicture = supportsPictureInPicture2;
                    }
                    supportsPictureInPicture = supportsPictureInPicture2;
                } else {
                    if (resizeMode == 3) {
                        resizeMode = 2;
                        supportsPictureInPicture = true;
                    }
                    supportsPictureInPicture = supportsPictureInPicture2;
                }
                Builder taskAffiliation3 = new Builder(taskSupervisor.mService).setTaskId(taskId).setIntent(intent).setAffinityIntent(affinityIntent).setAffinity(rootAffinity3).setRootAffinity(rootAffinity).setRealActivity(realActivity).setOrigActivity(origActivity2).setRootWasReset(rootHasReset3).setAutoRemoveRecents(autoRemoveRecents2).setAskedCompatMode(askedCompatMode2).setUserId(effectiveUid).setEffectiveUid(effectiveUid2).setLastDescription(lastDescription).setLastTimeMoved(lastTimeOnTop).setNeverRelinquishIdentity(neverRelinquishIdentity).setLastTaskDescription(taskDescription).setLastSnapshotData(lastSnapshotData).setTaskAffiliation(taskAffiliation);
                int taskAffiliation4 = prevTaskId;
                Builder prevAffiliateTaskId = taskAffiliation3.setPrevAffiliateTaskId(taskAffiliation4);
                int prevTaskId3 = nextTaskId;
                Builder nextAffiliateTaskId = prevAffiliateTaskId.setNextAffiliateTaskId(prevTaskId3);
                int nextTaskId3 = callingUid;
                Builder callingPackage2 = nextAffiliateTaskId.setCallingUid(nextTaskId3).setCallingPackage(callingPackage);
                String callingPackage3 = callingFeatureId;
                Builder supportsPictureInPicture4 = callingPackage2.setCallingFeatureId(callingPackage3).setResizeMode(resizeMode).setSupportsPictureInPicture(supportsPictureInPicture);
                boolean supportsPictureInPicture5 = realActivitySuspended;
                Builder realActivitySuspended3 = supportsPictureInPicture4.setRealActivitySuspended(supportsPictureInPicture5);
                boolean realActivitySuspended4 = userSetupComplete;
                Builder minWidth3 = realActivitySuspended3.setUserSetupComplete(realActivitySuspended4).setMinWidth(minWidth);
                int minWidth4 = minHeight;
                Task task = minWidth3.setMinHeight(minWidth4).buildInner();
                Rect lastNonFullscreenBounds3 = lastNonFullscreenBounds;
                task.mLastNonFullscreenBounds = lastNonFullscreenBounds3;
                task.setBounds(lastNonFullscreenBounds3);
                task.mWindowLayoutAffinity = windowLayoutAffinity;
                if (activities.size() > 0) {
                    DisplayContent dc = taskSupervisor.mRootWindowContainer.getDisplayContent(0);
                    dc.getDefaultTaskDisplayArea().addChild(task, Integer.MIN_VALUE);
                    for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                        task.addChild(activities.get(activityNdx));
                    }
                }
                if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "Restored task=" + task);
                }
                return task;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    public boolean isOrganized() {
        return this.mTaskOrganizer != null;
    }

    private boolean canBeOrganized() {
        if (isRootTask() || this.mCreatedByOrganizer) {
            return true;
        }
        Task parentTask = getParent().asTask();
        return parentTask != null && parentTask.mCreatedByOrganizer;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean showSurfaceOnCreation() {
        if (this.mCreatedByOrganizer) {
            return true;
        }
        return !canBeOrganized();
    }

    @Override // com.android.server.wm.WindowContainer
    protected void reparentSurfaceControl(SurfaceControl.Transaction t, SurfaceControl newParent) {
        if (isOrganized() && isAlwaysOnTop()) {
            return;
        }
        super.reparentSurfaceControl(t, newParent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHasBeenVisible(boolean hasBeenVisible) {
        this.mHasBeenVisible = hasBeenVisible;
        if (hasBeenVisible) {
            if (!this.mDeferTaskAppear) {
                sendTaskAppeared();
            }
            if (!isRootTask()) {
                getRootTask().setHasBeenVisible(true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getHasBeenVisible() {
        return this.mHasBeenVisible;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeferTaskAppear(boolean deferTaskAppear) {
        this.mDeferTaskAppear = deferTaskAppear;
        if (!deferTaskAppear) {
            sendTaskAppeared();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean taskAppearedReady() {
        if (this.mTaskOrganizer == null || this.mDeferTaskAppear) {
            return false;
        }
        if (this.mCreatedByOrganizer) {
            return true;
        }
        return this.mSurfaceControl != null && getHasBeenVisible();
    }

    private void sendTaskAppeared() {
        if (this.mTaskOrganizer != null) {
            this.mAtmService.mTaskOrganizerController.onTaskAppeared(this.mTaskOrganizer, this);
        }
    }

    private void sendTaskVanished(ITaskOrganizer organizer) {
        if (organizer != null) {
            this.mAtmService.mTaskOrganizerController.onTaskVanished(organizer, this);
        }
    }

    boolean setTaskOrganizer(ITaskOrganizer organizer) {
        return setTaskOrganizer(organizer, false);
    }

    boolean setTaskOrganizer(ITaskOrganizer organizer, boolean skipTaskAppeared) {
        if (this.mTaskOrganizer == organizer) {
            return false;
        }
        ITaskOrganizer prevOrganizer = this.mTaskOrganizer;
        this.mTaskOrganizer = organizer;
        sendTaskVanished(prevOrganizer);
        if (this.mTaskOrganizer != null) {
            if (!skipTaskAppeared) {
                sendTaskAppeared();
                return true;
            }
            return true;
        }
        TaskDisplayArea taskDisplayArea = getDisplayArea();
        if (taskDisplayArea != null) {
            taskDisplayArea.removeLaunchRootTask(this);
        }
        setForceHidden(2, false);
        if (this.mCreatedByOrganizer) {
            removeImmediately("setTaskOrganizer");
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateTaskOrganizerState() {
        return updateTaskOrganizerState(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateTaskOrganizerState(boolean skipTaskAppeared) {
        ITaskOrganizer iTaskOrganizer;
        if (getSurfaceControl() == null) {
            return false;
        }
        if (!canBeOrganized()) {
            return setTaskOrganizer(null);
        }
        TaskOrganizerController controller = this.mWmService.mAtmService.mTaskOrganizerController;
        ITaskOrganizer organizer = controller.getTaskOrganizer(this);
        if (!this.mCreatedByOrganizer || (iTaskOrganizer = this.mTaskOrganizer) == null || organizer == null || iTaskOrganizer == organizer) {
            return setTaskOrganizer(organizer, skipTaskAppeared);
        }
        return false;
    }

    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    void setSurfaceControl(SurfaceControl sc) {
        super.setSurfaceControl(sc);
        sendTaskAppeared();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFocused() {
        return (this.mDisplayContent == null || this.mDisplayContent.mFocusedApp == null || this.mDisplayContent.mFocusedApp.getTask() != this) ? false : true;
    }

    private boolean hasVisibleChildren() {
        return (!isAttached() || isForceHidden() || getActivity(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda40
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ((ActivityRecord) obj).isVisible();
            }
        }) == null) ? false : true;
    }

    private float getShadowRadius(boolean taskIsFocused) {
        if (inFreeformWindowingMode()) {
            int elevation = taskIsFocused ? 20 : 5;
            if (hasVisibleChildren()) {
                return WindowManagerService.dipToPixel(elevation, getDisplayContent().getDisplayMetrics());
            }
            return 0.0f;
        }
        return 0.0f;
    }

    private void updateShadowsRadius(boolean taskIsFocused, SurfaceControl.Transaction pendingTransaction) {
        if (isRootTask()) {
            float newShadowRadius = getShadowRadius(taskIsFocused);
            if (this.mShadowRadius != newShadowRadius) {
                this.mShadowRadius = newShadowRadius;
                pendingTransaction.setShadowRadius(getSurfaceControl(), this.mShadowRadius);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAppFocusChanged(boolean hasFocus) {
        updateShadowsRadius(hasFocus, getSyncTransaction());
        dispatchTaskInfoChangedIfNeeded(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPictureInPictureParamsChanged() {
        if (inPinnedWindowingMode()) {
            dispatchTaskInfoChangedIfNeeded(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onShouldDockBigOverlaysChanged() {
        dispatchTaskInfoChangedIfNeeded(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSizeCompatActivityChanged() {
        dispatchTaskInfoChangedIfNeeded(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMainWindowSizeChangeTransaction(SurfaceControl.Transaction t) {
        setMainWindowSizeChangeTransaction(t, this);
        forAllWindows(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WindowState) obj).requestRedrawForSync();
            }
        }, true);
    }

    private void setMainWindowSizeChangeTransaction(final SurfaceControl.Transaction t, Task origin) {
        ActivityRecord topActivity = getTopNonFinishingActivity();
        Task leaf = topActivity != null ? topActivity.getTask() : null;
        if (leaf == null) {
            return;
        }
        if (leaf != this) {
            leaf.setMainWindowSizeChangeTransaction(t, origin);
            return;
        }
        WindowState w = getTopVisibleAppMainWindow();
        if (w != null) {
            w.applyWithNextDraw(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda15
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((SurfaceControl.Transaction) obj).merge(t);
                }
            });
        } else {
            t.apply();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setActivityWindowingMode(int windowingMode) {
        PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda34
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ActivityRecord) obj).setWindowingMode(((Integer) obj2).intValue());
            }
        }, PooledLambda.__(ActivityRecord.class), Integer.valueOf(windowingMode));
        forAllActivities((Consumer<ActivityRecord>) c);
        c.recycle();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setForceHidden(int flags, boolean set) {
        int newFlags;
        int newFlags2 = this.mForceHiddenFlags;
        if (set) {
            newFlags = newFlags2 | flags;
        } else {
            newFlags = newFlags2 & (~flags);
        }
        if (this.mForceHiddenFlags == newFlags) {
            return false;
        }
        boolean wasHidden = isForceHidden();
        boolean wasVisible = isVisible();
        this.mForceHiddenFlags = newFlags;
        boolean nowHidden = isForceHidden();
        if (wasHidden != nowHidden) {
            if (wasVisible && nowHidden) {
                moveToBack("setForceHidden", null);
                return true;
            } else if (isAlwaysOnTop()) {
                moveToFront("setForceHidden");
                return true;
            } else {
                return true;
            }
        }
        return true;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public boolean isAlwaysOnTop() {
        return !isForceHidden() && super.isAlwaysOnTop();
    }

    public boolean isAlwaysOnTopWhenVisible() {
        return super.isAlwaysOnTop();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.wm.TaskFragment
    public boolean isForceHidden() {
        return this.mForceHiddenFlags != 0;
    }

    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    long getProtoFieldId() {
        return 1146756268037L;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public void setWindowingMode(int windowingMode) {
        if (!isRootTask()) {
            super.setWindowingMode(windowingMode);
        } else {
            setWindowingMode(windowingMode, false);
        }
    }

    void setWindowingMode(final int preferredWindowingMode, final boolean creating) {
        this.mWmService.inSurfaceTransaction(new Runnable() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda45
            @Override // java.lang.Runnable
            public final void run() {
                Task.this.m8279lambda$setWindowingMode$18$comandroidserverwmTask(preferredWindowingMode, creating);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: setWindowingModeInSurfaceTransaction */
    public void m8279lambda$setWindowingMode$18$comandroidserverwmTask(int preferredWindowingMode, boolean creating) {
        TaskDisplayArea taskDisplayArea = getDisplayArea();
        if (taskDisplayArea == null) {
            Slog.d(TAG, "taskDisplayArea is null, bail early");
            return;
        }
        int currentMode = getWindowingMode();
        Task topTask = getTopMostTask();
        int windowingMode = preferredWindowingMode;
        if (!creating && !taskDisplayArea.isValidWindowingMode(windowingMode, null, topTask)) {
            windowingMode = 0;
        }
        if (currentMode == windowingMode) {
            getRequestedOverrideConfiguration().windowConfiguration.setWindowingMode(windowingMode);
            return;
        }
        ActivityRecord topActivity = getTopNonFinishingActivity();
        int likelyResolvedMode = windowingMode;
        if (windowingMode == 0) {
            ConfigurationContainer parent = getParent();
            likelyResolvedMode = parent != null ? parent.getWindowingMode() : 1;
        }
        if (currentMode == 2) {
            this.mRootWindowContainer.notifyActivityPipModeChanged(this, null);
        }
        if (likelyResolvedMode == 2) {
            setCanAffectSystemUiFlags(true);
            if (taskDisplayArea.getRootPinnedTask() != null) {
                taskDisplayArea.getRootPinnedTask().dismissPip();
            }
        }
        if (likelyResolvedMode != 1 && topActivity != 0 && !topActivity.noDisplay && topActivity.canForceResizeNonResizable(likelyResolvedMode)) {
            String packageName = topActivity.info.applicationInfo.packageName;
            this.mAtmService.getTaskChangeNotificationController().notifyActivityForcedResizable(topTask.mTaskId, 1, packageName);
        }
        this.mAtmService.deferWindowLayout();
        if (topActivity != null) {
            try {
                if (!this.mReparentFromMultiWindow) {
                    this.mTaskSupervisor.mNoAnimActivities.add(topActivity);
                }
            } finally {
                this.mAtmService.continueWindowLayout();
            }
        }
        super.setWindowingMode(windowingMode);
        if (currentMode == 2 && topActivity != null) {
            if (topActivity.getLastParentBeforePip() != null && !isForceHidden()) {
                Task lastParentBeforePip = topActivity.getLastParentBeforePip();
                if (lastParentBeforePip.isAttached()) {
                    topActivity.reparent(lastParentBeforePip, lastParentBeforePip.getChildCount(), "movePinnedActivityToOriginalTask");
                    lastParentBeforePip.moveToFront("movePinnedActivityToOriginalTask");
                }
            }
            if (topActivity.shouldBeVisible()) {
                this.mAtmService.resumeAppSwitches();
            }
        }
        if (creating) {
            return;
        }
        if (topActivity != null && currentMode == 1 && windowingMode == 2 && !this.mTransitionController.isShellTransitionsEnabled()) {
            this.mDisplayContent.mPinnedTaskController.deferOrientationChangeForEnteringPipFromFullScreenIfNeeded();
        }
        this.mAtmService.continueWindowLayout();
        if (!this.mTaskSupervisor.isRootVisibilityUpdateDeferred()) {
            this.mRootWindowContainer.ensureActivitiesVisible(null, 0, true);
            this.mRootWindowContainer.resumeFocusedTasksTopActivities();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resumeNextFocusAfterReparent() {
        adjustFocusToNextFocusableTask("reparent", true, true);
        this.mRootWindowContainer.resumeFocusedTasksTopActivities();
        this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final boolean isOnHomeDisplay() {
        return getDisplayId() == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveToFront(String reason) {
        moveToFront(reason, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveToFront(String reason, Task task) {
        Task adjacentTask;
        if (this.mMoveAdjacentTogether && getAdjacentTaskFragment() != null && (adjacentTask = getAdjacentTaskFragment().asTask()) != null) {
            adjacentTask.moveToFrontInner(reason + " adjacentTaskToTop", null);
        }
        moveToFrontInner(reason, task);
    }

    void moveToFrontInner(String reason, Task task) {
        if (!isAttached()) {
            return;
        }
        TaskDisplayArea taskDisplayArea = getDisplayArea();
        if (!isActivityTypeHome() && returnsToHomeRootTask()) {
            taskDisplayArea.moveHomeRootTaskToFront(reason + " returnToHome");
        }
        Task lastFocusedTask = isRootTask() ? taskDisplayArea.getFocusedRootTask() : null;
        if (task == null) {
            task = this;
        }
        task.getParent().positionChildAt(Integer.MAX_VALUE, task, true);
        taskDisplayArea.updateLastFocusedRootTask(lastFocusedTask, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveToBottomForMultiWindow(String reason) {
        if (!isAttached()) {
            return;
        }
        TaskDisplayArea displayArea = getDisplayArea();
        Slog.i(TAG, "TranMultiWindow: displayArea = " + displayArea + ", displayArea = " + displayArea.getConfiguration().windowConfiguration.isThunderbackWindowNonInteractive());
        if (displayArea == this.mRootWindowContainer.getMultiDisplayArea() && displayArea.getConfiguration().windowConfiguration.isThunderbackWindowNonInteractive()) {
            Slog.i(TAG, "TranMultiWindow: move to bottom , reason : " + reason);
            displayArea.positionChildAt(Integer.MIN_VALUE, this, true);
        }
    }

    void moveToBack(String reason, Task task) {
        if (!isAttached()) {
            return;
        }
        TaskDisplayArea displayArea = getDisplayArea();
        if (!this.mCreatedByOrganizer) {
            WindowContainer parent = getParent();
            Task parentTask = parent != null ? parent.asTask() : null;
            if (parentTask != null) {
                parentTask.moveToBack(reason, this);
            } else {
                Task lastFocusedTask = displayArea.getFocusedRootTask();
                displayArea.positionChildAt(Integer.MIN_VALUE, this, false);
                displayArea.updateLastFocusedRootTask(lastFocusedTask, reason);
                this.mAtmService.getTaskChangeNotificationController().notifyTaskMovedToBack(getTaskInfo());
            }
            if (task != null && task != this) {
                positionChildAtBottom(task);
                this.mAtmService.getTaskChangeNotificationController().notifyTaskMovedToBack(task.getTaskInfo());
            }
        } else if (task == null || task == this) {
        } else {
            displayArea.positionTaskBehindHome(task);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void switchUser(int userId) {
        if (this.mCurrentUser == userId) {
            return;
        }
        this.mCurrentUser = userId;
        super.switchUser(userId);
        if (!isRootTask() && showToCurrentUser()) {
            getParent().positionChildAt(Integer.MAX_VALUE, this, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void minimalResumeActivityLocked(ActivityRecord r) {
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(r);
            String protoLogParam1 = String.valueOf(Debug.getCallers(5));
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1164930508, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        r.setState(ActivityRecord.State.RESUMED, "minimalResumeActivityLocked");
        r.completeResumeLocked();
        ITranWindowManagerService.Instance().onActivityResume(r);
        ITranTask.Instance().hookMinimalResumeActivityLocked(r);
        if (r != null && r.info != null) {
            ITranWindowManagerService.Instance().setActivityResumedHook(r);
        }
        this.mAtmService.mAmsExt.onAfterActivityResumed(r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkReadyForSleep() {
        if (shouldSleepActivities() && goToSleepIfPossible(false)) {
            this.mTaskSupervisor.checkReadyForSleepLocked(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean goToSleepIfPossible(final boolean shuttingDown) {
        final int[] sleepInProgress = {0};
        forAllLeafTasksAndLeafTaskFragments(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda10
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                Task.lambda$goToSleepIfPossible$19(shuttingDown, sleepInProgress, (TaskFragment) obj);
            }
        }, true);
        return sleepInProgress[0] == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$goToSleepIfPossible$19(boolean shuttingDown, int[] sleepInProgress, TaskFragment taskFragment) {
        if (!taskFragment.sleepIfPossible(shuttingDown)) {
            sleepInProgress[0] = sleepInProgress[0] + 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTopRootTaskInDisplayArea() {
        TaskDisplayArea taskDisplayArea = getDisplayArea();
        return taskDisplayArea != null && taskDisplayArea.isTopRootTask(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFocusedRootTaskOnDisplay() {
        return this.mDisplayContent != null && this == this.mDisplayContent.getFocusedRootTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void ensureActivitiesVisible(ActivityRecord starting, int configChanges, boolean preserveWindows) {
        ensureActivitiesVisible(starting, configChanges, preserveWindows, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void ensureActivitiesVisible(final ActivityRecord starting, final int configChanges, final boolean preserveWindows, final boolean notifyClients) {
        this.mTaskSupervisor.beginActivityVisibilityUpdate();
        try {
            forAllLeafTasks(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda25
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((Task) obj).updateActivityVisibilities(ActivityRecord.this, configChanges, preserveWindows, notifyClients);
                }
            }, true);
            if (this.mTranslucentActivityWaiting != null && this.mUndrawnActivitiesBelowTopTranslucent.isEmpty()) {
                notifyActivityDrawnLocked(null);
            }
        } finally {
            this.mTaskSupervisor.endActivityVisibilityUpdate();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldResizeRootTaskWithLaunchBounds() {
        return inPinnedWindowingMode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkTranslucentActivityWaiting(ActivityRecord top) {
        if (this.mTranslucentActivityWaiting != top) {
            this.mUndrawnActivitiesBelowTopTranslucent.clear();
            if (this.mTranslucentActivityWaiting != null) {
                notifyActivityDrawnLocked(null);
                this.mTranslucentActivityWaiting = null;
            }
            this.mHandler.removeMessages(101);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void convertActivityToTranslucent(ActivityRecord r) {
        this.mTranslucentActivityWaiting = r;
        this.mUndrawnActivitiesBelowTopTranslucent.clear();
        this.mHandler.sendEmptyMessageDelayed(101, TRANSLUCENT_CONVERSION_TIMEOUT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyActivityDrawnLocked(ActivityRecord r) {
        if (r == null || (this.mUndrawnActivitiesBelowTopTranslucent.remove(r) && this.mUndrawnActivitiesBelowTopTranslucent.isEmpty())) {
            ActivityRecord waitingActivity = this.mTranslucentActivityWaiting;
            this.mTranslucentActivityWaiting = null;
            this.mUndrawnActivitiesBelowTopTranslucent.clear();
            this.mHandler.removeMessages(101);
            if (waitingActivity != null) {
                this.mWmService.setWindowOpaqueLocked(waitingActivity.token, false);
                if (waitingActivity.attachedToProcess()) {
                    try {
                        waitingActivity.app.getThread().scheduleTranslucentConversionComplete(waitingActivity.token, r != null);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resumeTopActivityUncheckedLocked(ActivityRecord prev, ActivityOptions options, boolean deferPause) {
        if (this.mInResumeTopActivity) {
            return false;
        }
        boolean someActivityResumed = false;
        try {
            this.mInResumeTopActivity = true;
            if (!isLeafTask()) {
                int idx = this.mChildren.size() - 1;
                while (idx >= 0) {
                    int idx2 = idx - 1;
                    Task child = (Task) getChildAt(idx);
                    if (!child.isTopActivityFocusable()) {
                        idx = idx2;
                    } else if (child.getVisibility(null) != 0) {
                        break;
                    } else {
                        someActivityResumed |= child.resumeTopActivityUncheckedLocked(prev, options, deferPause);
                        if (idx2 >= this.mChildren.size()) {
                            idx = this.mChildren.size() - 1;
                        } else {
                            idx = idx2;
                        }
                    }
                }
            } else if (isFocusableAndVisible()) {
                someActivityResumed = resumeTopActivityInnerLocked(prev, options, deferPause);
            }
            ActivityRecord next = topRunningActivity(true);
            if (next == null || !next.canTurnScreenOn()) {
                checkReadyForSleep();
            }
            return someActivityResumed;
        } finally {
            this.mInResumeTopActivity = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resumeTopActivityUncheckedLocked(ActivityRecord prev, ActivityOptions options) {
        return resumeTopActivityUncheckedLocked(prev, options, false);
    }

    private boolean resumeTopActivityInnerLocked(final ActivityRecord prev, final ActivityOptions options, final boolean deferPause) {
        if (this.mAtmService.isBooting() || this.mAtmService.isBooted()) {
            ActivityRecord topActivity = topRunningActivity(true);
            if (topActivity == null) {
                return resumeNextFocusableActivityWhenRootTaskIsEmpty(prev, options);
            }
            final TaskFragment topFragment = topActivity.getTaskFragment();
            final boolean[] resumed = {topFragment.resumeTopActivity(prev, options, deferPause)};
            forAllLeafTaskFragments(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda38
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    Task.lambda$resumeTopActivityInnerLocked$21(TaskFragment.this, resumed, prev, options, deferPause, (TaskFragment) obj);
                }
            }, true);
            return resumed[0];
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$resumeTopActivityInnerLocked$21(TaskFragment topFragment, boolean[] resumed, ActivityRecord prev, ActivityOptions options, boolean deferPause, TaskFragment f) {
        if (topFragment == f || !f.canBeResumed(null)) {
            return;
        }
        resumed[0] = resumed[0] | f.resumeTopActivity(prev, options, deferPause);
    }

    private boolean resumeNextFocusableActivityWhenRootTaskIsEmpty(ActivityRecord prev, ActivityOptions options) {
        Task nextFocusedTask;
        if (!isActivityTypeHome() && (nextFocusedTask = adjustFocusToNextFocusableTask("noMoreActivities")) != null) {
            return this.mRootWindowContainer.resumeFocusedTasksTopActivities(nextFocusedTask, prev, null);
        }
        ActivityOptions.abort(options);
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf("noMoreActivities");
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -143556958, 0, (String) null, new Object[]{protoLogParam0});
        }
        return this.mRootWindowContainer.resumeHomeActivity(prev, "noMoreActivities", getDisplayArea());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startActivityLocked(ActivityRecord r, Task topTask, boolean newTask, boolean isTaskSwitch, ActivityOptions options, ActivityRecord sourceRecord) {
        int i;
        boolean doShow;
        Task baseTask;
        ActivityRecord pipCandidate = findEnterPipOnTaskSwitchCandidate(topTask);
        Task rTask = r.getTask();
        boolean allowMoveToFront = options == null || !options.getAvoidMoveToFront();
        boolean isOrhasTask = rTask == this || hasChild(rTask);
        if (!r.mLaunchTaskBehind && allowMoveToFront && (!isOrhasTask || newTask)) {
            positionChildAtTop(rTask);
        }
        if (!newTask && isOrhasTask && !r.shouldBeVisible()) {
            ActivityOptions.abort(options);
            return;
        }
        Task activityTask = r.getTask();
        if (null == activityTask && this.mChildren.indexOf(null) != getChildCount() - 1) {
            this.mTaskSupervisor.mUserLeaving = false;
            if (ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING) {
                Slog.v(TAG_USER_LEAVING, "startActivity() behind front, mUserLeaving=false");
            }
        }
        if (ProtoLogCache.WM_DEBUG_ADD_REMOVE_enabled) {
            String protoLogParam0 = String.valueOf(r);
            String protoLogParam1 = String.valueOf(activityTask);
            String protoLogParam2 = String.valueOf(new RuntimeException("here").fillInStackTrace());
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_ADD_REMOVE, -1699018375, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1, protoLogParam2});
        }
        if ((!isActivityTypeHomeOrRecents() || hasActivity()) && allowMoveToFront) {
            DisplayContent dc = this.mDisplayContent;
            if (ActivityTaskManagerDebugConfig.DEBUG_TRANSITION) {
                Slog.v(TAG_TRANSITION, "Prepare open transition: starting " + r);
            }
            if ((r.intent.getFlags() & 65536) != 0) {
                dc.prepareAppTransition(0);
                this.mTaskSupervisor.mNoAnimActivities.add(r);
            } else {
                if (newTask && !r.mLaunchTaskBehind) {
                    enableEnterPipOnTaskSwitch(pipCandidate, null, r, options);
                }
                if (!getConfiguration().windowConfiguration.isThunderbackWindow()) {
                    this.mRootWindowContainer.mStartActivityInDefaultTaskDisplayArea = true;
                } else {
                    this.mRootWindowContainer.mStartActivityInDefaultTaskDisplayArea = false;
                }
                if (getConfiguration().windowConfiguration.isThunderbackWindow()) {
                    i = 1;
                    this.mRootWindowContainer.mStartActivityInMultiTaskDisplayArea = true;
                } else {
                    i = 1;
                    this.mRootWindowContainer.mStartActivityInMultiTaskDisplayArea = false;
                }
                dc.prepareAppTransition(i);
                this.mTaskSupervisor.mNoAnimActivities.remove(r);
            }
            if (newTask) {
                if ((r.intent.getFlags() & 2097152) != 0) {
                    resetTaskIfNeeded(r, r);
                    boolean doShow2 = topRunningNonDelayedActivityLocked(null) == r;
                    doShow = doShow2;
                }
                doShow = true;
            } else {
                if (options != null && options.getAnimationType() == 5) {
                    doShow = false;
                }
                doShow = true;
            }
            boolean doShow3 = r.mLaunchTaskBehind;
            if (doShow3) {
                r.setVisibility(true);
                ensureActivitiesVisible(null, 0, false);
                this.mDisplayContent.executeAppTransition();
                return;
            } else if (doShow) {
                Task baseTask2 = r.getTask();
                if (!baseTask2.isEmbedded()) {
                    baseTask = baseTask2;
                } else {
                    baseTask = baseTask2.getParent().asTaskFragment().getTask();
                }
                ActivityRecord prev = baseTask.getActivity(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda1
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return Task.lambda$startActivityLocked$22((ActivityRecord) obj);
                    }
                });
                this.mWmService.mStartingSurfaceController.showStartingWindow(r, prev, newTask, isTaskSwitch, sourceRecord);
                return;
            } else {
                return;
            }
        }
        ActivityOptions.abort(options);
        if (r.isEmbedded() && topRunningActivity(true) == r) {
            r.abortAndClearOptionsAnimation();
            Slog.d(TAG, "startActivityLocked: abortAndClearOptionsAnimation r=" + r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$startActivityLocked$22(ActivityRecord a) {
        return a.mStartingData != null && a.showToCurrentUser();
    }

    static ActivityRecord findEnterPipOnTaskSwitchCandidate(Task topTask) {
        if (topTask == null) {
            return null;
        }
        final ActivityRecord[] candidate = new ActivityRecord[1];
        topTask.forAllLeafTaskFragments(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda19
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.lambda$findEnterPipOnTaskSwitchCandidate$23(candidate, (TaskFragment) obj);
            }
        });
        return candidate[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$findEnterPipOnTaskSwitchCandidate$23(ActivityRecord[] candidate, TaskFragment tf) {
        ActivityRecord topActivity = tf.getTopNonFinishingActivity();
        if (topActivity == null || !topActivity.isState(ActivityRecord.State.RESUMED, ActivityRecord.State.PAUSING) || !topActivity.supportsPictureInPicture()) {
            return false;
        }
        candidate[0] = topActivity;
        return true;
    }

    private static void enableEnterPipOnTaskSwitch(ActivityRecord pipCandidate, Task toFrontTask, ActivityRecord toFrontActivity, ActivityOptions opts) {
        if (pipCandidate == null) {
            return;
        }
        if ((opts != null && opts.disallowEnterPictureInPictureWhileLaunching()) || pipCandidate.inPinnedWindowingMode()) {
            return;
        }
        boolean isTransient = opts != null && opts.getTransientLaunch();
        Task targetRootTask = toFrontTask != null ? toFrontTask.getRootTask() : toFrontActivity.getRootTask();
        if (targetRootTask != null && (targetRootTask.isActivityTypeAssistant() || isTransient)) {
            return;
        }
        pipCandidate.supportsEnterPipOnTaskSwitch = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord resetTaskIfNeeded(ActivityRecord taskTop, ActivityRecord newActivity) {
        ActivityRecord newTop;
        boolean forceReset = (newActivity.info.flags & 4) != 0;
        Task task = taskTop.getTask();
        ActivityOptions topOptions = sResetTargetTaskHelper.process(task, forceReset);
        if (this.mChildren.contains(task) && (newTop = task.getTopNonFinishingActivity()) != null) {
            taskTop = newTop;
        }
        if (topOptions != null) {
            taskTop.updateOptionsLocked(topOptions);
        }
        return taskTop;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final Task finishTopCrashedActivityLocked(WindowProcessController app, String reason) {
        ActivityRecord r = topRunningActivity();
        if (r == null || r.app != app) {
            return null;
        }
        if (r.isActivityTypeHome() && this.mAtmService.mHomeProcess == app) {
            Slog.w(TAG, "  Not force finishing home activity " + r.intent.getComponent().flattenToShortString());
            return null;
        }
        Slog.w(TAG, "  Force finishing activity " + r.intent.getComponent().flattenToShortString());
        Task finishedTask = r.getTask();
        this.mDisplayContent.requestTransitionAndLegacyPrepare(2, 16);
        r.finishIfPossible(reason, false);
        ActivityRecord activityBelow = getActivityBelow(r);
        if (activityBelow != null && activityBelow.isState(ActivityRecord.State.STARTED, ActivityRecord.State.RESUMED, ActivityRecord.State.PAUSING, ActivityRecord.State.PAUSED) && (!activityBelow.isActivityTypeHome() || this.mAtmService.mHomeProcess != activityBelow.app)) {
            Slog.w(TAG, "  Force finishing activity " + activityBelow.intent.getComponent().flattenToShortString());
            activityBelow.finishIfPossible(reason, false);
        }
        return finishedTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishVoiceTask(IVoiceInteractionSession session) {
        PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda33
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                Task.finishIfVoiceTask((Task) obj, (IBinder) obj2);
            }
        }, PooledLambda.__(Task.class), session.asBinder());
        forAllLeafTasks(c, true);
        c.recycle();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void finishIfVoiceTask(final Task tr, IBinder binder) {
        IVoiceInteractionSession iVoiceInteractionSession = tr.voiceSession;
        if (iVoiceInteractionSession != null && iVoiceInteractionSession.asBinder() == binder) {
            tr.forAllActivities(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda17
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    Task.lambda$finishIfVoiceTask$24(Task.this, (ActivityRecord) obj);
                }
            });
            return;
        }
        PooledPredicate f = PooledLambda.obtainPredicate(new BiPredicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda18
            @Override // java.util.function.BiPredicate
            public final boolean test(Object obj, Object obj2) {
                boolean finishIfVoiceActivity;
                finishIfVoiceActivity = Task.finishIfVoiceActivity((ActivityRecord) obj, (IBinder) obj2);
                return finishIfVoiceActivity;
            }
        }, PooledLambda.__(ActivityRecord.class), binder);
        tr.forAllActivities((Predicate<ActivityRecord>) f);
        f.recycle();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$finishIfVoiceTask$24(Task tr, ActivityRecord r) {
        if (r.finishing) {
            return;
        }
        r.finishIfPossible("finish-voice", false);
        tr.mAtmService.updateOomAdj();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean finishIfVoiceActivity(ActivityRecord r, IBinder binder) {
        if (r.voiceSession == null || r.voiceSession.asBinder() != binder) {
            return false;
        }
        r.clearVoiceSessionLocked();
        try {
            r.app.getThread().scheduleLocalVoiceInteractionStarted(r.token, (IVoiceInteractor) null);
        } catch (RemoteException e) {
        }
        r.mAtmService.finishRunningVoiceLocked();
        return true;
    }

    private boolean inFrontOfStandardRootTask() {
        TaskDisplayArea taskDisplayArea = getDisplayArea();
        if (taskDisplayArea == null) {
            return false;
        }
        final boolean[] hasFound = new boolean[1];
        Task rootTaskBehind = taskDisplayArea.getRootTask(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda16
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Task.this.m8274lambda$inFrontOfStandardRootTask$25$comandroidserverwmTask(hasFound, (Task) obj);
            }
        });
        if (rootTaskBehind == null || !rootTaskBehind.isActivityTypeStandard()) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$inFrontOfStandardRootTask$25$com-android-server-wm-Task  reason: not valid java name */
    public /* synthetic */ boolean m8274lambda$inFrontOfStandardRootTask$25$comandroidserverwmTask(boolean[] hasFound, Task task) {
        if (hasFound[0]) {
            return true;
        }
        if (task == this) {
            hasFound[0] = true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldUpRecreateTaskLocked(ActivityRecord srec, String destAffinity) {
        String affinity = ActivityRecord.computeTaskAffinity(destAffinity, srec.getUid(), srec.launchMode);
        if (srec == null || srec.getTask().affinity == null || !srec.getTask().affinity.equals(affinity)) {
            return true;
        }
        Task task = srec.getTask();
        if (srec.isRootOfTask() && task.getBaseIntent() != null && task.getBaseIntent().isDocument()) {
            if (!inFrontOfStandardRootTask()) {
                return true;
            }
            Task prevTask = getTaskBelow(task);
            if (prevTask == null) {
                Slog.w(TAG, "shouldUpRecreateTask: task not in history for " + srec);
                return false;
            } else if (!task.affinity.equals(prevTask.affinity)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:59:0x01ab  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x01ce  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean navigateUpTo(ActivityRecord srec, Intent destIntent, NeededUriGrants destGrants, int resultCode, Intent resultData, NeededUriGrants resultGrants) {
        ActivityRecord candidate;
        boolean foundParentInTask;
        int callingUid;
        ActivityRecord parent;
        char c;
        char c2;
        int callingUid2;
        char c3;
        boolean abort;
        ActivityRecord next;
        if (srec.attachedToProcess()) {
            Task task = srec.getTask();
            if (srec.isDescendantOf(this)) {
                ActivityRecord parent2 = task.getActivityBelow(srec);
                final ComponentName dest = destIntent.getComponent();
                if (task.getBottomMostActivity() != srec && dest != null && (candidate = task.getActivity(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda20
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return Task.lambda$navigateUpTo$26(dest, (ActivityRecord) obj);
                    }
                }, srec, false, true)) != null) {
                    foundParentInTask = true;
                } else {
                    candidate = parent2;
                    foundParentInTask = false;
                }
                IActivityController controller = this.mAtmService.mController;
                if (controller != null && (next = topRunningActivity(srec.token, -1)) != null) {
                    boolean resumeOK = true;
                    try {
                        resumeOK = controller.activityResuming(next.packageName);
                    } catch (RemoteException e) {
                        this.mAtmService.mController = null;
                        Watchdog.getInstance().setActivityController(null);
                    }
                    if (!resumeOK) {
                        return false;
                    }
                }
                long origId = Binder.clearCallingIdentity();
                final int[] resultCodeHolder = {resultCode};
                final Intent[] resultDataHolder = {resultData};
                final NeededUriGrants[] resultGrantsHolder = {resultGrants};
                final ActivityRecord finalParent = candidate;
                task.forAllActivities(new Predicate() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda21
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return Task.lambda$navigateUpTo$27(ActivityRecord.this, resultCodeHolder, resultDataHolder, resultGrantsHolder, (ActivityRecord) obj);
                    }
                }, srec, true, true);
                int resultCode2 = resultCodeHolder[0];
                Intent resultData2 = resultDataHolder[0];
                if (candidate != null && foundParentInTask) {
                    int callingUid3 = srec.info.applicationInfo.uid;
                    int parentLaunchMode = candidate.info.launchMode;
                    int destIntentFlags = destIntent.getFlags();
                    if (parentLaunchMode == 3 || parentLaunchMode == 2 || parentLaunchMode == 1) {
                        callingUid = callingUid3;
                    } else if ((destIntentFlags & 67108864) != 0) {
                        callingUid = callingUid3;
                    } else {
                        try {
                            ActivityInfo aInfo = AppGlobals.getPackageManager().getActivityInfo(destIntent.getComponent(), (long) GadgetFunction.NCM, srec.mUserId);
                            int res = this.mAtmService.getActivityStartController().obtainStarter(destIntent, "navigateUpTo").setCaller(srec.app.getThread()).setActivityInfo(aInfo).setResultTo(candidate.token).setCallingPid(-1).setCallingUid(callingUid3).setCallingPackage(srec.packageName).setCallingFeatureId(candidate.launchedFromFeatureId).setRealCallingPid(-1).setRealCallingUid(callingUid3).setComponentSpecified(true).execute();
                            boolean foundParentInTask2 = res == 0;
                            foundParentInTask = foundParentInTask2;
                        } catch (RemoteException e2) {
                            foundParentInTask = false;
                        }
                        candidate.finishIfPossible(resultCode2, resultData2, resultGrants, "navigate-top", true);
                    }
                    try {
                        try {
                            parent = candidate;
                            c = 1;
                            c2 = 2;
                            callingUid2 = callingUid;
                            c3 = 0;
                            try {
                                abort = !this.mTaskSupervisor.checkStartAnyActivityPermission(destIntent, candidate.info, null, -1, srec.getPid(), callingUid, srec.info.packageName, null, false, false, srec.app, null, null);
                            } catch (SecurityException e3) {
                                abort = true;
                                if (!abort) {
                                }
                                Binder.restoreCallingIdentity(origId);
                                return foundParentInTask;
                            }
                        } catch (SecurityException e4) {
                            parent = candidate;
                            c = 1;
                            c2 = 2;
                            callingUid2 = callingUid;
                            c3 = 0;
                        }
                    } catch (SecurityException e5) {
                        parent = candidate;
                        c = 1;
                        c2 = 2;
                        callingUid2 = callingUid;
                        c3 = 0;
                    }
                    if (!abort) {
                        Object[] objArr = new Object[3];
                        objArr[c3] = "238605611";
                        objArr[c] = Integer.valueOf(callingUid2);
                        objArr[c2] = "";
                        EventLog.writeEvent(1397638484, objArr);
                        foundParentInTask = false;
                    } else {
                        parent.deliverNewIntentLocked(callingUid2, destIntent, destGrants, srec.packageName);
                    }
                }
                Binder.restoreCallingIdentity(origId);
                return foundParentInTask;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$navigateUpTo$26(ComponentName dest, ActivityRecord ar) {
        return ar.info.packageName.equals(dest.getPackageName()) && ar.info.name.equals(dest.getClassName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$navigateUpTo$27(ActivityRecord finalParent, int[] resultCodeHolder, Intent[] resultDataHolder, NeededUriGrants[] resultGrantsHolder, ActivityRecord ar) {
        if (ar == finalParent) {
            return true;
        }
        ar.finishIfPossible(resultCodeHolder[0], resultDataHolder[0], resultGrantsHolder[0], "navigate-up", true);
        resultCodeHolder[0] = 0;
        resultDataHolder[0] = null;
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeLaunchTickMessages() {
        forAllActivities(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ActivityRecord) obj).removeLaunchTickRunnable();
            }
        });
    }

    private void updateTransitLocked(int transit, ActivityOptions options) {
        if (options != null) {
            ActivityRecord r = topRunningActivity();
            if (r != null && !r.isState(ActivityRecord.State.RESUMED)) {
                r.updateOptionsLocked(options);
            } else {
                ActivityOptions.abort(options);
            }
        }
        this.mDisplayContent.prepareAppTransition(transit);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void moveTaskToFront(Task tr, boolean noAnimation, ActivityOptions options, AppTimeTracker timeTracker, String reason) {
        moveTaskToFront(tr, noAnimation, options, timeTracker, false, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void moveTaskToFront(Task tr, boolean noAnimation, ActivityOptions options, AppTimeTracker timeTracker, boolean deferResume, String reason) {
        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(TAG_SWITCH, "moveTaskToFront: " + tr);
        }
        ActivityRecord pipCandidate = findEnterPipOnTaskSwitchCandidate(getDisplayArea().getTopRootTask());
        boolean z = true;
        if (tr != this && !tr.isDescendantOf(this)) {
            if (noAnimation) {
                ActivityOptions.abort(options);
                return;
            }
            if (!getConfiguration().windowConfiguration.isThunderbackWindow()) {
                this.mRootWindowContainer.mStartActivityInDefaultTaskDisplayArea = true;
            } else {
                this.mRootWindowContainer.mStartActivityInDefaultTaskDisplayArea = false;
            }
            updateTransitLocked(3, options);
            return;
        }
        if (timeTracker != null) {
            PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda43
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((ActivityRecord) obj).setAppTimeTracker((AppTimeTracker) obj2);
                }
            }, PooledLambda.__(ActivityRecord.class), timeTracker);
            tr.forAllActivities((Consumer<ActivityRecord>) c);
            c.recycle();
        }
        try {
            this.mDisplayContent.deferUpdateImeTarget();
            ActivityRecord top = tr.getTopNonFinishingActivity();
            if (top != null && top.showToCurrentUser()) {
                top.moveFocusableActivityToTop(reason);
                if (ActivityTaskManagerDebugConfig.DEBUG_TRANSITION) {
                    Slog.v(TAG_TRANSITION, "Prepare to front transition: task=" + tr);
                }
                if (noAnimation) {
                    this.mDisplayContent.prepareAppTransition(0);
                    this.mTaskSupervisor.mNoAnimActivities.add(top);
                    ActivityOptions.abort(options);
                } else {
                    if (!getConfiguration().windowConfiguration.isThunderbackWindow()) {
                        this.mRootWindowContainer.mStartActivityInDefaultTaskDisplayArea = true;
                    } else {
                        this.mRootWindowContainer.mStartActivityInDefaultTaskDisplayArea = false;
                    }
                    updateTransitLocked(3, options);
                }
                boolean disablePip = top.isActivityTypeRecents();
                if (!disablePip && top.isActivityTypeHome()) {
                    if (options == null || options.getAnimationType() != 13) {
                        z = false;
                    }
                    disablePip = z;
                }
                if (!disablePip && (disablePip = "com.transsion.auto".equals(this.mCallingPackage))) {
                    Slog.d(TAG, "Drive mode task, don't enable enter pip om task switch");
                }
                if (disablePip) {
                    Slog.w(TAG, "Launch recent, don't enable enter pip om task switch");
                    if (pipCandidate != null && pipCandidate.supportsEnterPipOnTaskSwitch) {
                        Slog.w(TAG, "Launch recent, but pre task is enableEnterPipOnTaskSwitch, force disable it");
                        pipCandidate.supportsEnterPipOnTaskSwitch = false;
                    }
                } else {
                    enableEnterPipOnTaskSwitch(pipCandidate, tr, null, options);
                }
                if (!deferResume) {
                    this.mRootWindowContainer.resumeFocusedTasksTopActivities();
                }
                return;
            }
            positionChildAtTop(tr);
            if (top != null) {
                this.mTaskSupervisor.mRecentTasks.add(top.getTask());
            }
            ActivityOptions.abort(options);
        } finally {
            this.mDisplayContent.continueUpdateImeTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean moveTaskToBack(Task tr) {
        Slog.i(TAG, "moveTaskToBack: " + tr);
        if (this.mAtmService.getLockTaskController().canMoveTaskToBack(tr)) {
            if (isTopRootTaskInDisplayArea() && this.mAtmService.mController != null) {
                ActivityRecord next = topRunningActivity((IBinder) null, tr.mTaskId);
                if (next == null) {
                    next = topRunningActivity((IBinder) null, -1);
                }
                if (next != null) {
                    boolean moveOK = true;
                    try {
                        moveOK = this.mAtmService.mController.activityResuming(next.packageName);
                    } catch (RemoteException e) {
                        this.mAtmService.mController = null;
                        Watchdog.getInstance().setActivityController(null);
                    }
                    if (!moveOK) {
                        return false;
                    }
                }
            }
            if (SplitScreenHelper.isInSplitScreenTaskStack(this, tr, 1)) {
                Slog.d(TAG, "moveTaskToBack try move task from split screen, an we just remove it " + tr);
                this.mTaskSupervisor.removeTask(tr, false, false, "remove from split screen");
                return true;
            }
            if (ActivityTaskManagerDebugConfig.DEBUG_TRANSITION) {
                Slog.v(TAG_TRANSITION, "Prepare to back transition: task=" + tr.mTaskId);
            }
            if (!inPinnedWindowingMode()) {
                this.mDisplayContent.requestTransitionAndLegacyPrepare(4, tr);
            }
            moveToBack("moveTaskToBackLocked", tr);
            if (inPinnedWindowingMode()) {
                this.mTaskSupervisor.removeRootTask(this);
                return true;
            }
            this.mRootWindowContainer.ensureVisibilityAndConfig(null, this.mDisplayContent.mDisplayId, false, false);
            ActivityRecord topActivity = getDisplayArea().topRunningActivity();
            Task topRootTask = topActivity.getRootTask();
            if (topRootTask != null && topRootTask != this && topActivity.isState(ActivityRecord.State.RESUMED)) {
                this.mDisplayContent.executeAppTransition();
            } else {
                this.mRootWindowContainer.resumeFocusedTasksTopActivities();
            }
            hookMultiWindowToClose(topRootTask);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resize(Rect displayedBounds, boolean preserveWindows, boolean deferResume) {
        Trace.traceBegin(32L, "task.resize_" + getRootTaskId());
        this.mAtmService.deferWindowLayout();
        try {
            PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda44
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    Task.processTaskResizeBounds((Task) obj, (Rect) obj2);
                }
            }, PooledLambda.__(Task.class), displayedBounds);
            forAllTasks(c, true);
            c.recycle();
            if (!deferResume) {
                ensureVisibleActivitiesConfiguration(topRunningActivity(), preserveWindows);
            }
        } finally {
            this.mAtmService.continueWindowLayout();
            Trace.traceEnd(32L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void processTaskResizeBounds(Task task, Rect displayedBounds) {
        if (task.isResizeable()) {
            task.setBounds(displayedBounds);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean willActivityBeVisible(IBinder token) {
        ActivityRecord r = ActivityRecord.forTokenLocked(token);
        if (r == null || !r.shouldBeVisible()) {
            return false;
        }
        if (r.finishing) {
            Slog.e(TAG, "willActivityBeVisible: Returning false, would have returned true for r=" + r);
        }
        return !r.finishing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unhandledBackLocked() {
        ActivityRecord topActivity = getTopMostActivity();
        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
            Slog.d(TAG_SWITCH, "Performing unhandledBack(): top activity: " + topActivity);
        }
        if (topActivity != null) {
            topActivity.finishIfPossible("unhandled-back", true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dump(FileDescriptor fd, PrintWriter pw, boolean dumpAll, boolean dumpClient, String dumpPackage, boolean needSep) {
        return dump("  ", fd, pw, dumpAll, dumpClient, dumpPackage, needSep, null);
    }

    @Override // com.android.server.wm.TaskFragment
    void dumpInner(String prefix, PrintWriter pw, boolean dumpAll, String dumpPackage) {
        super.dumpInner(prefix, pw, dumpAll, dumpPackage);
        if (this.mCreatedByOrganizer) {
            pw.println(prefix + "  mCreatedByOrganizer=true");
        }
        if (this.mLastNonFullscreenBounds != null) {
            pw.print(prefix);
            pw.print("  mLastNonFullscreenBounds=");
            pw.println(this.mLastNonFullscreenBounds);
        }
        if (isLeafTask()) {
            pw.println(prefix + "  isSleeping=" + shouldSleepActivities());
            ActivityTaskSupervisor.printThisActivity(pw, getTopPausingActivity(), dumpPackage, false, prefix + "  topPausingActivity=", null);
            ActivityTaskSupervisor.printThisActivity(pw, getTopResumedActivity(), dumpPackage, false, prefix + "  topResumedActivity=", null);
            if (this.mMinWidth != -1 || this.mMinHeight != -1) {
                pw.print(prefix);
                pw.print("  mMinWidth=");
                pw.print(this.mMinWidth);
                pw.print(" mMinHeight=");
                pw.println(this.mMinHeight);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<ActivityRecord> getDumpActivitiesLocked(String name, int userId) {
        final ArrayList<ActivityRecord> activities = new ArrayList<>();
        if ("all".equals(name)) {
            Objects.requireNonNull(activities);
            forAllActivities(new Task$$ExternalSyntheticLambda46(activities));
        } else if ("top".equals(name)) {
            ActivityRecord topActivity = getTopMostActivity();
            if (topActivity != null) {
                activities.add(topActivity);
            }
        } else {
            final ActivityManagerService.ItemMatcher matcher = new ActivityManagerService.ItemMatcher();
            matcher.build(name);
            forAllActivities(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda47
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    Task.lambda$getDumpActivitiesLocked$28(ActivityManagerService.ItemMatcher.this, activities, (ActivityRecord) obj);
                }
            });
        }
        if (userId != -1) {
            for (int i = activities.size() - 1; i >= 0; i--) {
                if (activities.get(i).mUserId != userId) {
                    activities.remove(i);
                }
            }
        }
        return activities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getDumpActivitiesLocked$28(ActivityManagerService.ItemMatcher matcher, ArrayList activities, ActivityRecord r) {
        if (matcher.match(r, r.intent.getComponent())) {
            activities.add(r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord restartPackage(String packageName) {
        ActivityRecord starting = topRunningActivity();
        PooledConsumer c = PooledLambda.obtainConsumer(new TriConsumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda52
            public final void accept(Object obj, Object obj2, Object obj3) {
                Task.restartPackage((ActivityRecord) obj, (ActivityRecord) obj2, (String) obj3);
            }
        }, PooledLambda.__(ActivityRecord.class), starting, packageName);
        forAllActivities((Consumer<ActivityRecord>) c);
        c.recycle();
        return starting;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void restartPackage(ActivityRecord r, ActivityRecord starting, String packageName) {
        if (r.info.packageName.equals(packageName)) {
            r.forceNewConfig = true;
            if (starting != null && r == starting && r.mVisibleRequested) {
                r.startFreezingScreenLocked(256);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task reuseOrCreateTask(ActivityInfo info, Intent intent, boolean toTop) {
        return reuseOrCreateTask(info, intent, null, null, toTop, null, null, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task reuseOrCreateTask(ActivityInfo info, Intent intent, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor, boolean toTop, ActivityRecord activity, ActivityRecord source, ActivityOptions options) {
        int taskId;
        Task task;
        if (canReuseAsLeafTask()) {
            task = reuseAsLeafTask(voiceSession, voiceInteractor, intent, info, activity);
        } else {
            if (activity != null) {
                taskId = this.mTaskSupervisor.getNextTaskIdForUser(activity.mUserId);
            } else {
                taskId = this.mTaskSupervisor.getNextTaskIdForUser();
            }
            Task task2 = new Builder(this.mAtmService).setTaskId(taskId).setActivityInfo(info).setActivityOptions(options).setIntent(intent).setVoiceSession(voiceSession).setVoiceInteractor(voiceInteractor).setOnTop(toTop).setParent(this).build();
            Slog.d(TAG, "reuseOrCreateTask task=" + task2 + ", parent task:" + this);
            task = task2;
        }
        int displayId = getDisplayId();
        if (displayId == -1) {
            displayId = 0;
        }
        boolean isLockscreenShown = this.mAtmService.mTaskSupervisor.getKeyguardController().isKeyguardOrAodShowing(displayId);
        if (!this.mTaskSupervisor.getLaunchParamsController().layoutTask(task, info.windowLayout, activity, source, options) && !getRequestedOverrideBounds().isEmpty() && task.isResizeable() && !isLockscreenShown) {
            task.setBounds(getRequestedOverrideBounds());
        }
        return task;
    }

    private boolean canReuseAsLeafTask() {
        if (this.mCreatedByOrganizer || !isLeafTask()) {
            return false;
        }
        int windowingMode = getWindowingMode();
        int activityType = getActivityType();
        return DisplayContent.alwaysCreateRootTask(windowingMode, activityType) || activityType == 5;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addChild(WindowContainer child, boolean toTop, boolean showForAllUsers) {
        Task task = child.asTask();
        if (task != null) {
            try {
                task.setForceShowForAllUsers(showForAllUsers);
            } catch (Throwable th) {
                if (task != null) {
                    task.setForceShowForAllUsers(false);
                }
                throw th;
            }
        }
        addChild(child, toTop ? Integer.MAX_VALUE : 0, toTop);
        if (task != null) {
            task.setForceShowForAllUsers(false);
        }
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        if (super.isAlwaysOnTop() == alwaysOnTop) {
            return;
        }
        super.setAlwaysOnTop(alwaysOnTop);
        if (!isForceHidden()) {
            getDisplayArea().positionChildAt(Integer.MAX_VALUE, this, false);
        }
    }

    void dismissPip() {
        if (!isActivityTypeStandardOrUndefined()) {
            throw new IllegalArgumentException("You can't move tasks from non-standard root tasks.");
        }
        if (getWindowingMode() != 2) {
            throw new IllegalArgumentException("Can't exit pinned mode if it's not pinned already.");
        }
        this.mWmService.inSurfaceTransaction(new Runnable() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda29
            @Override // java.lang.Runnable
            public final void run() {
                Task.this.m8271lambda$dismissPip$29$comandroidserverwmTask();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dismissPip$29$com-android-server-wm-Task  reason: not valid java name */
    public /* synthetic */ void m8271lambda$dismissPip$29$comandroidserverwmTask() {
        Task task = getBottomMostTask();
        setWindowingMode(0);
        if (isAttached()) {
            getDisplayArea().positionChildAt(Integer.MAX_VALUE, this, false);
        }
        this.mTaskSupervisor.scheduleUpdatePictureInPictureModeIfNeeded(task, this);
    }

    private int setBounds(Rect existing, Rect bounds) {
        if (equivalentBounds(existing, bounds)) {
            return 0;
        }
        int result = super.setBounds(!inMultiWindowMode() ? null : bounds);
        updateSurfaceBounds();
        return result;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public void getBounds(Rect bounds) {
        bounds.set(getBounds());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addChild(WindowContainer child, int position, boolean moveParents) {
        addChild((Task) child, (Comparator<Task>) null);
        positionChildAt(position, child, moveParents);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void positionChildAtTop(Task child) {
        if (child == null) {
            return;
        }
        if (child == this) {
            moveToFront("positionChildAtTop");
            return;
        }
        positionChildAt(Integer.MAX_VALUE, child, true);
        DisplayContent displayContent = getDisplayContent();
        displayContent.layoutAndAssignWindowLayersIfNeeded();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void positionChildAtBottom(Task child) {
        Task nextFocusableRootTask = getDisplayArea().getNextFocusableRootTask(child.getRootTask(), true);
        positionChildAtBottom(child, nextFocusableRootTask == null);
    }

    void positionChildAtBottom(Task child, boolean includingParents) {
        if (child == null) {
            return;
        }
        positionChildAt(Integer.MIN_VALUE, child, includingParents);
        getDisplayContent().layoutAndAssignWindowLayersIfNeeded();
    }

    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer
    void onChildPositionChanged(WindowContainer child) {
        dispatchTaskInfoChangedIfNeeded(false);
        if (!this.mChildren.contains(child)) {
            return;
        }
        if (child.asTask() != null) {
            this.mRootWindowContainer.invalidateTaskLayers();
        }
        boolean isTop = getTopChild() == child;
        if (isTop) {
            DisplayContent displayContent = getDisplayContent();
            if (displayContent == null) {
                Slog.w(TAG, "DisplayContent can't be null");
            } else {
                displayContent.layoutAndAssignWindowLayersIfNeeded();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reparent(TaskDisplayArea newParent, boolean onTop) {
        if (newParent == null) {
            throw new IllegalArgumentException("Task can't reparent to null " + this);
        }
        if (getParent() == newParent) {
            throw new IllegalArgumentException("Task=" + this + " already child of " + newParent);
        }
        if (getParent() != null && getParent().getConfiguration().windowConfiguration.isThunderbackWindow()) {
            ComponentName componentName = this.realActivity;
            String packageName = componentName != null ? componentName.getPackageName() : null;
            if (WHATSAPP_NAME.equals(packageName)) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda30
                    @Override // java.lang.Runnable
                    public final void run() {
                        Task.this.m8277lambda$reparent$30$comandroidserverwmTask();
                    }
                });
            }
        }
        if (canBeLaunchedOnDisplay(newParent.getDisplayId())) {
            reparent(newParent, onTop ? Integer.MAX_VALUE : Integer.MIN_VALUE);
            if (isLeafTask()) {
                newParent.onLeafTaskMoved(this, onTop);
            }
        } else {
            Slog.w(TAG, "Task=" + this + " can't reparent to " + newParent);
        }
        if (ThunderbackConfig.isVersion3() || this.mLauncherFromRecent) {
            ITranTask.Instance().reportDescendantOrientationChangeIfNeeded(newParent != null && newParent.isMultiWindow(), getResumedActivity(), TAG);
            this.mLauncherFromRecent = false;
        }
        if (newParent != null && newParent.isMultiWindow() && this.mDisplayContent.mAppTransition.containsTransitRequest(32)) {
            final List<ActivityRecord> allVisibleActivities = new ArrayList<>();
            forAllActivities(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda31
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    Task.lambda$reparent$31(allVisibleActivities, (ActivityRecord) obj);
                }
            });
            if (allVisibleActivities.size() > 0) {
                for (ActivityRecord r : allVisibleActivities) {
                    r.prepareOpenningAction();
                }
            }
        }
        if (newParent != null && newParent.isMultiWindow()) {
            ComponentName componentName2 = this.realActivity;
            String packageName2 = componentName2 != null ? componentName2.getPackageName() : null;
            if (WHATSAPP_NAME.equals(packageName2)) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda32
                    @Override // java.lang.Runnable
                    public final void run() {
                        Task.this.m8278lambda$reparent$32$comandroidserverwmTask();
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reparent$30$com-android-server-wm-Task  reason: not valid java name */
    public /* synthetic */ void m8277lambda$reparent$30$comandroidserverwmTask() {
        Intent intent = new Intent(EXIT_MULTIWINDOW_ACTION);
        intent.setPackage(SOCIAL_TURBO_NAME);
        this.mAtmService.mContext.sendBroadcast(intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$reparent$31(List allVisibleActivities, ActivityRecord r) {
        if (!r.finishing && !r.isTaskOverlay() && r.isVisible()) {
            allVisibleActivities.add(r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$reparent$32$com-android-server-wm-Task  reason: not valid java name */
    public /* synthetic */ void m8278lambda$reparent$32$comandroidserverwmTask() {
        Intent intent = new Intent(ENTER_MULTIWINDOW_ACTION);
        intent.setPackage(SOCIAL_TURBO_NAME);
        this.mAtmService.mContext.sendBroadcast(intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastRecentsAnimationTransaction(PictureInPictureSurfaceTransaction transaction, SurfaceControl overlay) {
        this.mLastRecentsAnimationTransaction = new PictureInPictureSurfaceTransaction(transaction);
        this.mLastRecentsAnimationOverlay = overlay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearLastRecentsAnimationTransaction(boolean forceRemoveOverlay) {
        if (forceRemoveOverlay && this.mLastRecentsAnimationOverlay != null) {
            getPendingTransaction().remove(this.mLastRecentsAnimationOverlay);
        }
        this.mLastRecentsAnimationTransaction = null;
        this.mLastRecentsAnimationOverlay = null;
        getPendingTransaction().setMatrix(this.mSurfaceControl, Matrix.IDENTITY_MATRIX, new float[9]).setWindowCrop(this.mSurfaceControl, null).setCornerRadius(this.mSurfaceControl, 0.0f);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void maybeApplyLastRecentsAnimationTransaction() {
        if (this.mLastRecentsAnimationTransaction != null) {
            SurfaceControl.Transaction tx = getPendingTransaction();
            SurfaceControl surfaceControl = this.mLastRecentsAnimationOverlay;
            if (surfaceControl != null) {
                tx.reparent(surfaceControl, this.mSurfaceControl);
            }
            PictureInPictureSurfaceTransaction.apply(this.mLastRecentsAnimationTransaction, this.mSurfaceControl, tx);
            tx.show(this.mSurfaceControl);
            this.mLastRecentsAnimationTransaction = null;
            this.mLastRecentsAnimationOverlay = null;
        }
    }

    private void updateSurfaceBounds() {
        updateSurfaceSize(getSyncTransaction());
        updateSurfacePositionNonOrganized();
        scheduleAnimation();
    }

    private Point getRelativePosition() {
        Point position = new Point();
        getRelativePosition(position);
        return position;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldIgnoreInput() {
        if (this.mAtmService.mHasLeanbackFeature && inPinnedWindowingMode() && !isFocusedRootTaskOnDisplay()) {
            return true;
        }
        return false;
    }

    private void warnForNonLeafTask(String func) {
        if (!isLeafTask()) {
            Slog.w(TAG, func + " on non-leaf task " + this);
        }
    }

    public DisplayInfo getDisplayInfo() {
        return this.mDisplayContent.getDisplayInfo();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AnimatingActivityRegistry getAnimatingActivityRegistry() {
        return this.mAnimatingActivityRegistry;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.TaskFragment
    public void executeAppTransition(ActivityOptions options) {
        this.mDisplayContent.executeAppTransition();
        ActivityOptions.abort(options);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.TaskFragment
    public boolean shouldSleepActivities() {
        boolean isKeyguardGoingAway;
        DisplayContent display = this.mDisplayContent;
        boolean isKeyguardGoingAwayQuickly = this.mTaskSupervisor.getKeyguardController().isKeyguardGoingAwayQuickly();
        if (isKeyguardGoingAwayQuickly) {
            Slog.d(TAG, "shouldSleepActivities  isKeyguardGoingAwayQuickly = " + isKeyguardGoingAwayQuickly);
            return false;
        }
        if (this.mDisplayContent != null) {
            isKeyguardGoingAway = this.mDisplayContent.isKeyguardGoingAway();
        } else {
            isKeyguardGoingAway = this.mRootWindowContainer.getDefaultDisplay().isKeyguardGoingAway();
        }
        if (isKeyguardGoingAway && isFocusedRootTaskOnDisplay() && display.isDefaultDisplay) {
            return false;
        }
        return display != null ? display.isSleeping() : this.mAtmService.isSleepingLocked();
    }

    private Rect getRawBounds() {
        return super.getBounds();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchTaskInfoChangedIfNeeded(boolean force) {
        if (isOrganized()) {
            this.mAtmService.mTaskOrganizerController.onTaskInfoChanged(this, force);
        }
    }

    @Override // com.android.server.wm.TaskFragment, com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        if (logLevel == 2 && !isVisible()) {
            return;
        }
        long token = proto.start(fieldId);
        proto.write(1120986464258L, this.mTaskId);
        proto.write(1120986464272L, getRootTaskId());
        if (getTopResumedActivity() != null) {
            getTopResumedActivity().writeIdentifierToProto(proto, 1146756268044L);
        }
        ComponentName componentName = this.realActivity;
        if (componentName != null) {
            proto.write(1138166333453L, componentName.flattenToShortString());
        }
        ComponentName componentName2 = this.origActivity;
        if (componentName2 != null) {
            proto.write(1138166333454L, componentName2.flattenToShortString());
        }
        proto.write(1120986464274L, this.mResizeMode);
        proto.write(1133871366148L, matchParentBounds());
        getRawBounds().dumpDebug(proto, 1146756268037L);
        Rect rect = this.mLastNonFullscreenBounds;
        if (rect != null) {
            rect.dumpDebug(proto, 1146756268054L);
        }
        if (this.mSurfaceControl != null) {
            proto.write(1120986464264L, this.mSurfaceControl.getWidth());
            proto.write(1120986464265L, this.mSurfaceControl.getHeight());
        }
        proto.write(1133871366172L, this.mCreatedByOrganizer);
        proto.write(1138166333469L, this.affinity);
        proto.write(1133871366174L, this.mChildPipActivity != null);
        super.dumpDebug(proto, 1146756268063L, logLevel);
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Builder {
        private ActivityInfo mActivityInfo;
        private ActivityOptions mActivityOptions;
        private int mActivityType;
        private String mAffinity;
        private Intent mAffinityIntent;
        private boolean mAskedCompatMode;
        private final ActivityTaskManagerService mAtmService;
        private boolean mAutoRemoveRecents;
        private String mCallingFeatureId;
        private String mCallingPackage;
        private int mCallingUid;
        private boolean mCreatedByOrganizer;
        private boolean mDeferTaskAppear;
        private int mEffectiveUid;
        private boolean mHasBeenVisible;
        private Intent mIntent;
        private String mLastDescription;
        private ActivityManager.RecentTaskInfo.PersistedTaskSnapshotData mLastSnapshotData;
        private ActivityManager.TaskDescription mLastTaskDescription;
        private long mLastTimeMoved;
        private IBinder mLaunchCookie;
        private int mLaunchFlags;
        private boolean mNeverRelinquishIdentity;
        private boolean mOnTop;
        private ComponentName mOrigActivity;
        private WindowContainer mParent;
        private ComponentName mRealActivity;
        private boolean mRealActivitySuspended;
        private boolean mRemoveWithTaskOrganizer;
        private int mResizeMode;
        private String mRootAffinity;
        private boolean mRootWasReset;
        private Task mSourceTask;
        private boolean mSupportsPictureInPicture;
        private int mTaskAffiliation;
        private int mTaskId;
        private int mUserId;
        private boolean mUserSetupComplete;
        private IVoiceInteractor mVoiceInteractor;
        private IVoiceInteractionSession mVoiceSession;
        private int mPrevAffiliateTaskId = -1;
        private int mNextAffiliateTaskId = -1;
        private int mMinWidth = -1;
        private int mMinHeight = -1;
        private int mWindowingMode = 0;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(ActivityTaskManagerService atm) {
            this.mAtmService = atm;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setParent(WindowContainer parent) {
            this.mParent = parent;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setSourceTask(Task sourceTask) {
            this.mSourceTask = sourceTask;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setLaunchFlags(int launchFlags) {
            this.mLaunchFlags = launchFlags;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setTaskId(int taskId) {
            this.mTaskId = taskId;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setIntent(Intent intent) {
            this.mIntent = intent;
            return this;
        }

        Builder setRealActivity(ComponentName realActivity) {
            this.mRealActivity = realActivity;
            return this;
        }

        Builder setEffectiveUid(int effectiveUid) {
            this.mEffectiveUid = effectiveUid;
            return this;
        }

        Builder setMinWidth(int minWidth) {
            this.mMinWidth = minWidth;
            return this;
        }

        Builder setMinHeight(int minHeight) {
            this.mMinHeight = minHeight;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setActivityInfo(ActivityInfo info) {
            this.mActivityInfo = info;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setActivityOptions(ActivityOptions opts) {
            this.mActivityOptions = opts;
            return this;
        }

        Builder setVoiceSession(IVoiceInteractionSession voiceSession) {
            this.mVoiceSession = voiceSession;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setActivityType(int activityType) {
            this.mActivityType = activityType;
            return this;
        }

        int getActivityType() {
            return this.mActivityType;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setWindowingMode(int windowingMode) {
            this.mWindowingMode = windowingMode;
            return this;
        }

        int getWindowingMode() {
            return this.mWindowingMode;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setCreatedByOrganizer(boolean createdByOrganizer) {
            this.mCreatedByOrganizer = createdByOrganizer;
            return this;
        }

        boolean getCreatedByOrganizer() {
            return this.mCreatedByOrganizer;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setDeferTaskAppear(boolean defer) {
            this.mDeferTaskAppear = defer;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setLaunchCookie(IBinder launchCookie) {
            this.mLaunchCookie = launchCookie;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setOnTop(boolean onTop) {
            this.mOnTop = onTop;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setHasBeenVisible(boolean hasBeenVisible) {
            this.mHasBeenVisible = hasBeenVisible;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setUserId(int userId) {
            this.mUserId = userId;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setLastTimeMoved(long lastTimeMoved) {
            this.mLastTimeMoved = lastTimeMoved;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setNeverRelinquishIdentity(boolean neverRelinquishIdentity) {
            this.mNeverRelinquishIdentity = neverRelinquishIdentity;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setCallingUid(int callingUid) {
            this.mCallingUid = callingUid;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setCallingPackage(String callingPackage) {
            this.mCallingPackage = callingPackage;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setResizeMode(int resizeMode) {
            this.mResizeMode = resizeMode;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setSupportsPictureInPicture(boolean supportsPictureInPicture) {
            this.mSupportsPictureInPicture = supportsPictureInPicture;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setUserSetupComplete(boolean userSetupComplete) {
            this.mUserSetupComplete = userSetupComplete;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setTaskAffiliation(int taskAffiliation) {
            this.mTaskAffiliation = taskAffiliation;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setPrevAffiliateTaskId(int prevAffiliateTaskId) {
            this.mPrevAffiliateTaskId = prevAffiliateTaskId;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setNextAffiliateTaskId(int nextAffiliateTaskId) {
            this.mNextAffiliateTaskId = nextAffiliateTaskId;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setCallingFeatureId(String callingFeatureId) {
            this.mCallingFeatureId = callingFeatureId;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setRealActivitySuspended(boolean realActivitySuspended) {
            this.mRealActivitySuspended = realActivitySuspended;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setLastDescription(String lastDescription) {
            this.mLastDescription = lastDescription;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setLastTaskDescription(ActivityManager.TaskDescription lastTaskDescription) {
            this.mLastTaskDescription = lastTaskDescription;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setLastSnapshotData(ActivityManager.RecentTaskInfo.PersistedTaskSnapshotData lastSnapshotData) {
            this.mLastSnapshotData = lastSnapshotData;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setOrigActivity(ComponentName origActivity) {
            this.mOrigActivity = origActivity;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setRootWasReset(boolean rootWasReset) {
            this.mRootWasReset = rootWasReset;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setAutoRemoveRecents(boolean autoRemoveRecents) {
            this.mAutoRemoveRecents = autoRemoveRecents;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setAskedCompatMode(boolean askedCompatMode) {
            this.mAskedCompatMode = askedCompatMode;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setAffinityIntent(Intent affinityIntent) {
            this.mAffinityIntent = affinityIntent;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setAffinity(String affinity) {
            this.mAffinity = affinity;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setRootAffinity(String rootAffinity) {
            this.mRootAffinity = rootAffinity;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setVoiceInteractor(IVoiceInteractor voiceInteractor) {
            this.mVoiceInteractor = voiceInteractor;
            return this;
        }

        private void validateRootTask(TaskDisplayArea tda) {
            Task rootTask;
            if (this.mActivityType == 0 && !this.mCreatedByOrganizer) {
                this.mActivityType = 1;
            }
            int i = this.mActivityType;
            if (i != 1 && i != 0 && (rootTask = tda.getRootTask(0, i)) != null) {
                throw new IllegalArgumentException("Root task=" + rootTask + " of activityType=" + this.mActivityType + " already on display=" + tda + ". Can't have multiple.");
            }
            if (!TaskDisplayArea.isWindowingModeSupported(this.mWindowingMode, this.mAtmService.mSupportsMultiWindow, this.mAtmService.mSupportsFreeformWindowManagement, this.mAtmService.mSupportsPictureInPicture)) {
                throw new IllegalArgumentException("Can't create root task for unsupported windowingMode=" + this.mWindowingMode);
            }
            int i2 = this.mWindowingMode;
            if (i2 == 2 && this.mActivityType != 1) {
                throw new IllegalArgumentException("Root task with pinned windowing mode cannot with non-standard activity type.");
            }
            if (i2 == 2 && tda.getRootPinnedTask() != null) {
                tda.getRootPinnedTask().dismissPip();
            }
            Intent intent = this.mIntent;
            if (intent != null) {
                this.mLaunchFlags = intent.getFlags() | this.mLaunchFlags;
            }
            Task launchRootTask = this.mCreatedByOrganizer ? null : tda.getLaunchRootTask(this.mWindowingMode, this.mActivityType, this.mActivityOptions, this.mSourceTask, this.mLaunchFlags);
            if (launchRootTask != null) {
                this.mWindowingMode = 0;
                this.mParent = launchRootTask;
            }
            this.mTaskId = tda.getNextRootTaskId();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Task build() {
            WindowContainer windowContainer = this.mParent;
            if (windowContainer != null && (windowContainer instanceof TaskDisplayArea)) {
                validateRootTask((TaskDisplayArea) windowContainer);
            }
            if (this.mActivityInfo == null) {
                ActivityInfo activityInfo = new ActivityInfo();
                this.mActivityInfo = activityInfo;
                activityInfo.applicationInfo = new ApplicationInfo();
            }
            this.mUserId = UserHandle.getUserId(this.mActivityInfo.applicationInfo.uid);
            this.mTaskAffiliation = this.mTaskId;
            this.mLastTimeMoved = System.currentTimeMillis();
            this.mNeverRelinquishIdentity = true;
            this.mCallingUid = this.mActivityInfo.applicationInfo.uid;
            this.mCallingPackage = this.mActivityInfo.packageName;
            this.mResizeMode = this.mActivityInfo.resizeMode;
            this.mSupportsPictureInPicture = this.mActivityInfo.supportsPictureInPicture();
            ActivityOptions activityOptions = this.mActivityOptions;
            if (activityOptions != null) {
                this.mRemoveWithTaskOrganizer = activityOptions.getRemoveWithTaskOranizer();
                if (this.mCallingPackage == null) {
                    this.mCallingPackage = this.mActivityOptions.getPackageName();
                }
            }
            Task task = buildInner();
            task.mHasBeenVisible = this.mHasBeenVisible;
            int i = this.mActivityType;
            if (i != 0) {
                task.setActivityType(i);
            }
            WindowContainer windowContainer2 = this.mParent;
            if (windowContainer2 != null) {
                if (windowContainer2 instanceof Task) {
                    Task parentTask = (Task) windowContainer2;
                    parentTask.addChild(task, this.mOnTop ? Integer.MAX_VALUE : Integer.MIN_VALUE, (this.mActivityInfo.flags & 1024) != 0);
                } else {
                    windowContainer2.addChild(task, this.mOnTop ? Integer.MAX_VALUE : Integer.MIN_VALUE);
                }
            }
            int i2 = this.mWindowingMode;
            if (i2 != 0) {
                task.setWindowingMode(i2, true);
                if (task.showSurfaceOnCreation()) {
                    task.getSyncTransaction().show(task.mSurfaceControl);
                }
            }
            return task;
        }

        Task buildInner() {
            return new Task(this.mAtmService, this.mTaskId, this.mIntent, this.mAffinityIntent, this.mAffinity, this.mRootAffinity, this.mRealActivity, this.mOrigActivity, this.mRootWasReset, this.mAutoRemoveRecents, this.mAskedCompatMode, this.mUserId, this.mEffectiveUid, this.mLastDescription, this.mLastTimeMoved, this.mNeverRelinquishIdentity, this.mLastTaskDescription, this.mLastSnapshotData, this.mTaskAffiliation, this.mPrevAffiliateTaskId, this.mNextAffiliateTaskId, this.mCallingUid, this.mCallingPackage, this.mCallingFeatureId, this.mResizeMode, this.mSupportsPictureInPicture, this.mRealActivitySuspended, this.mUserSetupComplete, this.mMinWidth, this.mMinHeight, this.mActivityInfo, this.mVoiceSession, this.mVoiceInteractor, this.mCreatedByOrganizer, this.mLaunchCookie, this.mDeferTaskAppear, this.mRemoveWithTaskOrganizer);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void updateOverlayInsetsState(WindowState originalChange) {
        super.updateOverlayInsetsState(originalChange);
        if (originalChange == getTopVisibleAppMainWindow() && this.mOverlayHost != null) {
            InsetsState s = originalChange.getInsetsState(true);
            getBounds(this.mTmpRect);
            this.mOverlayHost.dispatchInsetsChanged(s, this.mTmpRect);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean markAdjacentTask() {
        if (getParent() == null || getParent().asTask() == null) {
            return false;
        }
        Task parentTask = getParent().asTask();
        if (parentTask.mCreatedByOrganizer && !parentTask.isEmbedded() && parentTask.getWindowingMode() == 6 && parentTask.getAdjacentTaskFragment() != null && parentTask.getAdjacentTaskFragment().asTask() != null) {
            Task adjacentPTask = parentTask.getAdjacentTaskFragment().asTask();
            if (adjacentPTask.mCreatedByOrganizer && !parentTask.isEmbedded() && adjacentPTask.getWindowingMode() == 6 && parentTask.getTopChild() != null && adjacentPTask.getTopChild() != null) {
                boolean parentCreatedByOrganizer = parentTask.getRootTask() != null && parentTask.getRootTask() == adjacentPTask.getRootTask() && parentTask.getRootTask().isOrganized() && parentTask.getRootTask().mCreatedByOrganizer;
                Task p1Task = parentTask.getTopChild().asTask();
                Task p2Task = adjacentPTask.getTopChild().asTask();
                if (parentCreatedByOrganizer && p1Task != null && p2Task != null) {
                    p1Task.setAdjacentTaskForThisTask(p2Task);
                    Slog.d(TAG, "mark adjacentTask, p1Task= " + p1Task + " p2Task= " + p2Task);
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAdjacentTaskForThisTask(Task adjacentTask) {
        if (this.mAdjacentTask == adjacentTask) {
            return;
        }
        resetAdjacentTask(this);
        if (adjacentTask != null) {
            this.mAdjacentTask = adjacentTask;
            this.mKeepAdjacentReason = adjacentTask.getWindowingMode();
            adjacentTask.setAdjacentTaskForThisTask(this);
        }
    }

    void resetAdjacentTask(Task mTask) {
        if (mTask == null) {
            return;
        }
        Task task = mTask.mAdjacentTask;
        if (task != null && task.mAdjacentTask == mTask) {
            Slog.d(TAG, "reset adjacentTask mTask= " + mTask);
            mTask.mAdjacentTask.mAdjacentTask = null;
            Task task2 = mTask.mAdjacentTask;
            task2.mKeepAdjacentReason = 0;
            task2.mLastAdjacentActiveTime = -1L;
        }
        mTask.mAdjacentTask = null;
        mTask.mKeepAdjacentReason = 0;
        mTask.mLastAdjacentActiveTime = -1L;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getAdjacentTask() {
        return this.mAdjacentTask;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void touchAdjacentTaskActiveTime() {
        if (this.mAdjacentTask != null) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mLastAdjacentActiveTime = elapsedRealtime;
            this.mAdjacentTask.mLastAdjacentActiveTime = elapsedRealtime;
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void onSurfaceShown(SurfaceControl.Transaction t) {
        super.onSurfaceShown(t);
        ITranWindowManagerService.Instance().onTaskSurfaceShown(this.mAtmService.mUiContext, this, t);
    }

    private void hookMultiWindowToClose(Task topRootTask) {
        if (getConfiguration().windowConfiguration.isThunderbackWindow() && topRootTask == this) {
            Slog.i(TAG, "move the last task to back,dismiss multi window.");
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.Task.1
                @Override // java.lang.Runnable
                public void run() {
                    if (ThunderbackConfig.isVersion3()) {
                        ITranTask.Instance().hookMultiWindowToCloseV3(-1);
                    }
                }
            }, 300L);
            this.mDisplayContent.setSkipAppTransitionAnim(true);
        }
    }

    private void releaseTaskAnimation() {
        SurfaceControl surfaceControl = this.mTaskAnimationLeash;
        if (surfaceControl != null && surfaceControl.isValid()) {
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            t.remove(this.mTaskAnimationLeash);
            t.apply();
        }
        this.mTaskAnimationLeash = null;
    }

    public TaskFragment getTopTaskFragment() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            TaskFragment top = ((WindowContainer) this.mChildren.get(i)).asTaskFragment();
            if (top != null && top.isVisible()) {
                return top;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTaskMinAspectRatioForSetting() {
        TranFoldingScreenManager.getInstance();
        if (!TranFoldingScreenManager.isFoldableDevice()) {
            return;
        }
        if (this.realActivity == null) {
            Slog.i(TAG, "updateTaskMinAspectRatioForSetting not work due to realActivity is null");
            return;
        }
        boolean isPkgInActivityEmbedding = TranFoldingScreenManager.getInstance().isPkgInActivityEmbedding(this.mAtmService.mUiContext, this.realActivity.getPackageName());
        this.mIsPkgInActivityEmbedding = isPkgInActivityEmbedding;
        if (isPkgInActivityEmbedding) {
            this.mTaskMinAspectRatioForUser = -1.0f;
            return;
        }
        int compatiblemode = TranFoldingScreenManager.getInstance().getCompatibleMode(this.mAtmService.mUiContext, this.realActivity.getPackageName());
        updateTaskMinAspectRatio(compatiblemode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTaskMinAspectRatio(int compatiblemode) {
        float taskMinAspectRatioForUser = getMinAspectRatioForCompatiblemode(compatiblemode);
        if (this.mTaskMinAspectRatioForUser != taskMinAspectRatioForUser) {
            this.mTaskMinAspectRatioForUser = taskMinAspectRatioForUser;
            updateMinAspectRatioToActivities();
        }
    }

    private float getMinAspectRatioForCompatiblemode(int compatiblemode) {
        switch (compatiblemode) {
            case 1:
                return 1.777f;
            case 2:
                return 1.333f;
            default:
                return -1.0f;
        }
    }

    private void updateMinAspectRatioToActivities() {
        forAllActivities(new Consumer() { // from class: com.android.server.wm.Task$$ExternalSyntheticLambda36
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                Task.this.m8280x578166ab((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateMinAspectRatioToActivities$33$com-android-server-wm-Task  reason: not valid java name */
    public /* synthetic */ void m8280x578166ab(ActivityRecord r) {
        r.setMinAspectRatioForUser(this.mTaskMinAspectRatioForUser);
    }

    @Override // com.android.server.wm.TaskFragment
    float getMinAspectRatioForUser() {
        return this.mTaskMinAspectRatioForUser;
    }

    @Override // com.android.server.wm.TaskFragment
    boolean isPkgInActivityEmbedding() {
        return this.mIsPkgInActivityEmbedding;
    }
}
