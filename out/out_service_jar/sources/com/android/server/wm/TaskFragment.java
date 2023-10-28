package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.ResultInfo;
import android.app.WindowConfiguration;
import android.app.servertransaction.ActivityLifecycleItem;
import android.app.servertransaction.ActivityResultItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.NewIntentItem;
import android.app.servertransaction.PauseActivityItem;
import android.app.servertransaction.ResumeActivityItem;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.window.ITaskFragmentOrganizer;
import android.window.TaskFragmentInfo;
import android.window.TaskFragmentOrganizerToken;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.util.function.pooled.PooledPredicate;
import com.android.server.am.HostingRecord;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.RemoteAnimationController;
import com.android.server.wm.WindowContainer;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.server.wm.ITranAppTransition;
import com.transsion.hubcore.server.wm.ITranTaskFragment;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import com.transsion.hubcore.sourceconnect.ITranSourceConnectManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class TaskFragment extends WindowContainer<WindowContainer> {
    static final int EMBEDDING_ALLOWED = 0;
    static final int EMBEDDING_DISALLOWED_MIN_DIMENSION_VIOLATION = 2;
    static final int EMBEDDING_DISALLOWED_NEW_TASK = 3;
    static final int EMBEDDING_DISALLOWED_UNTRUSTED_HOST = 1;
    static final int INVALID_MIN_SIZE = -1;
    static final boolean SHOW_APP_STARTING_PREVIEW = true;
    static final int TASK_FRAGMENT_VISIBILITY_INVISIBLE = 2;
    static final int TASK_FRAGMENT_VISIBILITY_VISIBLE = 0;
    static final int TASK_FRAGMENT_VISIBILITY_VISIBLE_BEHIND_TRANSLUCENT = 1;
    private TaskFragment mAdjacentTaskFragment;
    final ActivityTaskManagerService mAtmService;
    HashMap<String, SurfaceControl.ScreenshotHardwareBuffer> mBackScreenshots;
    boolean mClearedTaskForReuse;
    boolean mClearedTaskFragmentForPip;
    private TaskFragment mCompanionTaskFragment;
    boolean mCreatedByOrganizer;
    private boolean mDelayLastActivityRemoval;
    Dimmer mDimmer;
    private final EnsureActivitiesVisibleHelper mEnsureActivitiesVisibleHelper;
    private final EnsureVisibleActivitiesConfigHelper mEnsureVisibleActivitiesConfigHelper;
    private final IBinder mFragmentToken;
    private final boolean mIsEmbedded;
    private boolean mIsRemovalRequested;
    ActivityRecord mLastPausedActivity;
    final Point mLastSurfaceSize;
    int mMinHeight;
    int mMinWidth;
    boolean mMoveAdjacentTogether;
    private ActivityRecord mPausingActivity;
    private ActivityRecord mResumedActivity;
    final RootWindowContainer mRootWindowContainer;
    boolean mTaskFragmentAppearedSent;
    private ITaskFragmentOrganizer mTaskFragmentOrganizer;
    private final TaskFragmentOrganizerController mTaskFragmentOrganizerController;
    private String mTaskFragmentOrganizerProcessName;
    private int mTaskFragmentOrganizerUid;
    final ActivityTaskSupervisor mTaskSupervisor;
    private final Rect mTmpBounds;
    private final Rect mTmpFullBounds;
    private final Rect mTmpInsets;
    private final Rect mTmpNonDecorBounds;
    private final Rect mTmpStableBounds;
    private static final String TAG = "ActivityTaskManager";
    private static final String TAG_SWITCH = TAG + ActivityTaskManagerDebugConfig.POSTFIX_SWITCH;
    private static final String TAG_RESULTS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_RESULTS;
    private static final String TAG_TRANSITION = TAG + ActivityTaskManagerDebugConfig.POSTFIX_TRANSITION;

    /* loaded from: classes2.dex */
    @interface EmbeddingCheckResult {
    }

    /* loaded from: classes2.dex */
    @interface TaskFragmentVisibility {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class EnsureVisibleActivitiesConfigHelper implements Predicate<ActivityRecord> {
        private boolean mBehindFullscreen;
        private boolean mPreserveWindow;
        private boolean mUpdateConfig;

        private EnsureVisibleActivitiesConfigHelper() {
        }

        void reset(boolean preserveWindow) {
            this.mPreserveWindow = preserveWindow;
            this.mUpdateConfig = false;
            this.mBehindFullscreen = false;
        }

        void process(ActivityRecord start, boolean preserveWindow) {
            if (start == null || !start.mVisibleRequested) {
                return;
            }
            reset(preserveWindow);
            TaskFragment.this.forAllActivities(this, start, true, true);
            if (this.mUpdateConfig) {
                TaskFragment.this.mRootWindowContainer.resumeFocusedTasksTopActivities();
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Predicate
        public boolean test(ActivityRecord r) {
            this.mUpdateConfig |= r.ensureActivityConfiguration(0, this.mPreserveWindow);
            boolean occludesParent = this.mBehindFullscreen | r.occludesParent();
            this.mBehindFullscreen = occludesParent;
            return occludesParent;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragment(ActivityTaskManagerService atmService, IBinder fragmentToken, boolean createdByOrganizer) {
        this(atmService, fragmentToken, createdByOrganizer, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragment(ActivityTaskManagerService atmService, IBinder fragmentToken, boolean createdByOrganizer, boolean isEmbedded) {
        super(atmService.mWindowManager);
        this.mDimmer = new Dimmer(this);
        this.mPausingActivity = null;
        this.mLastPausedActivity = null;
        this.mResumedActivity = null;
        this.mTaskFragmentOrganizerUid = -1;
        this.mLastSurfaceSize = new Point();
        this.mTmpInsets = new Rect();
        this.mTmpBounds = new Rect();
        this.mTmpFullBounds = new Rect();
        this.mTmpStableBounds = new Rect();
        this.mTmpNonDecorBounds = new Rect();
        this.mBackScreenshots = new HashMap<>();
        this.mEnsureActivitiesVisibleHelper = new EnsureActivitiesVisibleHelper(this);
        this.mEnsureVisibleActivitiesConfigHelper = new EnsureVisibleActivitiesConfigHelper();
        this.mAtmService = atmService;
        this.mTaskSupervisor = atmService.mTaskSupervisor;
        this.mRootWindowContainer = atmService.mRootWindowContainer;
        this.mCreatedByOrganizer = createdByOrganizer;
        this.mIsEmbedded = isEmbedded;
        this.mTaskFragmentOrganizerController = atmService.mWindowOrganizerController.mTaskFragmentOrganizerController;
        this.mFragmentToken = fragmentToken;
        this.mRemoteToken = new WindowContainer.RemoteToken(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static TaskFragment fromTaskFragmentToken(IBinder token, ActivityTaskManagerService service) {
        if (token == null) {
            return null;
        }
        return service.mWindowOrganizerController.getTaskFragment(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAdjacentTaskFragment(TaskFragment taskFragment, boolean moveTogether) {
        if (this.mAdjacentTaskFragment == taskFragment) {
            return;
        }
        resetAdjacentTaskFragment();
        if (taskFragment != null) {
            this.mAdjacentTaskFragment = taskFragment;
            this.mMoveAdjacentTogether = moveTogether;
            taskFragment.setAdjacentTaskFragment(this, moveTogether);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCompanionTaskFragment(TaskFragment companionTaskFragment) {
        this.mCompanionTaskFragment = companionTaskFragment;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragment getCompanionTaskFragment() {
        return this.mCompanionTaskFragment;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetAdjacentTaskFragment() {
        TaskFragment taskFragment = this.mAdjacentTaskFragment;
        if (taskFragment != null && taskFragment.mAdjacentTaskFragment == this) {
            taskFragment.mAdjacentTaskFragment = null;
            TaskFragment taskFragment2 = this.mAdjacentTaskFragment;
            taskFragment2.mDelayLastActivityRemoval = false;
            taskFragment2.mMoveAdjacentTogether = false;
        }
        this.mAdjacentTaskFragment = null;
        this.mDelayLastActivityRemoval = false;
        this.mMoveAdjacentTogether = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTaskFragmentOrganizer(TaskFragmentOrganizerToken organizer, int uid, String processName) {
        this.mTaskFragmentOrganizer = ITaskFragmentOrganizer.Stub.asInterface(organizer.asBinder());
        this.mTaskFragmentOrganizerUid = uid;
        this.mTaskFragmentOrganizerProcessName = processName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasTaskFragmentOrganizer(ITaskFragmentOrganizer organizer) {
        return (organizer == null || this.mTaskFragmentOrganizer == null || !organizer.asBinder().equals(this.mTaskFragmentOrganizer.asBinder())) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragment getAdjacentTaskFragment() {
        return this.mAdjacentTaskFragment;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopResumedActivity() {
        WindowContainer<?> taskFragResumedActivity = getResumedActivity();
        for (int i = getChildCount() - 1; i >= 0; i--) {
            WindowContainer<?> child = getChildAt(i);
            ActivityRecord topResumedActivity = null;
            if (taskFragResumedActivity != null && child == taskFragResumedActivity) {
                topResumedActivity = child.asActivityRecord();
            } else if (child.asTaskFragment() != null) {
                topResumedActivity = child.asTaskFragment().getTopResumedActivity();
            }
            if (topResumedActivity != null) {
                return topResumedActivity;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getResumedActivity() {
        return this.mResumedActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setResumedActivity(ActivityRecord r, String reason) {
        warnForNonLeafTaskFragment("setResumedActivity");
        if (this.mResumedActivity == r) {
            return;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK) {
            Slog.d(TAG, "setResumedActivity taskFrag:" + this + " + from: " + this.mResumedActivity + " to:" + r + " reason:" + reason);
        }
        if (r != null && this.mResumedActivity == null) {
            getTask().touchActiveTime();
        }
        ActivityRecord prevR = this.mResumedActivity;
        this.mResumedActivity = r;
        this.mTaskSupervisor.updateTopResumedActivityIfNeeded();
        if (r == null && prevR.mDisplayContent != null && prevR.mDisplayContent.getFocusedRootTask() == null) {
            prevR.mDisplayContent.onRunningActivityChanged();
        } else if (r != null) {
            r.mDisplayContent.onRunningActivityChanged();
        }
    }

    void setPausingActivity(ActivityRecord pausing) {
        this.mPausingActivity = pausing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopPausingActivity() {
        WindowContainer<?> taskFragPausingActivity = getPausingActivity();
        for (int i = getChildCount() - 1; i >= 0; i--) {
            WindowContainer<?> child = getChildAt(i);
            ActivityRecord topPausingActivity = null;
            if (taskFragPausingActivity != null && child == taskFragPausingActivity) {
                topPausingActivity = child.asActivityRecord();
            } else if (child.asTaskFragment() != null) {
                topPausingActivity = child.asTaskFragment().getTopPausingActivity();
            }
            if (topPausingActivity != null) {
                return topPausingActivity;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getPausingActivity() {
        return this.mPausingActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getDisplayId() {
        DisplayContent dc = getDisplayContent();
        if (dc != null) {
            return dc.mDisplayId;
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTask() {
        if (asTask() != null) {
            return asTask();
        }
        TaskFragment parent = getParent() != null ? getParent().asTaskFragment() : null;
        if (parent != null) {
            return parent.getTask();
        }
        return null;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public TaskDisplayArea getDisplayArea() {
        return (TaskDisplayArea) super.getDisplayArea();
    }

    @Override // com.android.server.wm.WindowContainer
    public boolean isAttached() {
        TaskDisplayArea taskDisplayArea = getDisplayArea();
        return (taskDisplayArea == null || taskDisplayArea.isRemoved()) ? false : true;
    }

    TaskFragment getRootTaskFragment() {
        TaskFragment parentTaskFragment;
        WindowContainer parent = getParent();
        if (parent != null && (parentTaskFragment = parent.asTaskFragment()) != null) {
            return parentTaskFragment.getRootTaskFragment();
        }
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootTask() {
        return getRootTaskFragment().asTask();
    }

    @Override // com.android.server.wm.WindowContainer
    TaskFragment asTaskFragment() {
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isEmbedded() {
        if (this.mIsEmbedded) {
            return true;
        }
        WindowContainer<?> parent = getParent();
        if (parent == null) {
            return false;
        }
        TaskFragment taskFragment = parent.asTaskFragment();
        return taskFragment != null && taskFragment.isEmbedded();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEmbeddedWithBoundsOverride() {
        Task task;
        if (this.mIsEmbedded && (task = getTask()) != null) {
            Rect taskBounds = task.getBounds();
            Rect taskFragBounds = getBounds();
            return !taskBounds.equals(taskFragBounds) && taskBounds.contains(taskFragBounds);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int isAllowedToEmbedActivity(ActivityRecord a) {
        return isAllowedToEmbedActivity(a, this.mTaskFragmentOrganizerUid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int isAllowedToEmbedActivity(ActivityRecord a, int uid) {
        if (!isAllowedToEmbedActivityInUntrustedMode(a) && !isAllowedToEmbedActivityInTrustedMode(a, uid)) {
            return 1;
        }
        if (smallerThanMinDimension(a)) {
            return 2;
        }
        return 0;
    }

    boolean smallerThanMinDimension(ActivityRecord activity) {
        Point minDimensions;
        Rect taskFragBounds = getBounds();
        Task task = getTask();
        if (task == null || taskFragBounds.equals(task.getBounds()) || (minDimensions = activity.getMinDimensions()) == null) {
            return false;
        }
        int minWidth = minDimensions.x;
        int minHeight = minDimensions.y;
        return taskFragBounds.width() < minWidth || taskFragBounds.height() < minHeight;
    }

    boolean isAllowedToEmbedActivityInUntrustedMode(ActivityRecord a) {
        WindowContainer parent = getParent();
        return parent != null && parent.getBounds().contains(getBounds()) && (a.info.flags & 268435456) == 268435456;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAllowedToEmbedActivityInTrustedMode(ActivityRecord a) {
        return isAllowedToEmbedActivityInTrustedMode(a, this.mTaskFragmentOrganizerUid);
    }

    boolean isAllowedToEmbedActivityInTrustedMode(ActivityRecord a, int uid) {
        AndroidPackage hostPackage;
        if (ITaskFragmentLice.instance().onIsAllowedToEmbedActivityInTrustedMode(a, uid, 0)) {
            return false;
        }
        if (isFullyTrustedEmbedding(a, uid) || ITaskFragmentLice.instance().onIsAllowedToEmbedActivityInTrustedMode(a, uid, 1)) {
            return true;
        }
        Set<String> knownActivityEmbeddingCerts = a.info.getKnownActivityEmbeddingCerts();
        return (knownActivityEmbeddingCerts.isEmpty() || (hostPackage = this.mAtmService.getPackageManagerInternalLocked().getPackage(uid)) == null || !hostPackage.getSigningDetails().hasAncestorOrSelfWithDigest(knownActivityEmbeddingCerts)) ? false : true;
    }

    private static boolean isFullyTrustedEmbedding(ActivityRecord a, int uid) {
        return UserHandle.getAppId(uid) == 1000 || a.isUid(uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$isFullyTrustedEmbedding$0(int uid, ActivityRecord r) {
        return !isFullyTrustedEmbedding(r, uid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFullyTrustedEmbedding(final int uid) {
        return !forAllActivities(new Predicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda8
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TaskFragment.lambda$isFullyTrustedEmbedding$0(uid, (ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAllowedToBeEmbeddedInTrustedMode() {
        return !forAllActivities(new Predicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda13
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TaskFragment.this.m8340xe2169c97((ActivityRecord) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$isAllowedToBeEmbeddedInTrustedMode$1$com-android-server-wm-TaskFragment  reason: not valid java name */
    public /* synthetic */ boolean m8340xe2169c97(ActivityRecord r) {
        return !isAllowedToEmbedActivityInTrustedMode(r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragment getOrganizedTaskFragment() {
        if (this.mTaskFragmentOrganizer != null) {
            return this;
        }
        TaskFragment parentTaskFragment = getParent() != null ? getParent().asTaskFragment() : null;
        if (parentTaskFragment != null) {
            return parentTaskFragment.getOrganizedTaskFragment();
        }
        return null;
    }

    private void warnForNonLeafTaskFragment(String func) {
        if (!isLeafTaskFragment()) {
            Slog.w(TAG, func + " on non-leaf task fragment " + this);
        }
    }

    boolean hasDirectChildActivities() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((WindowContainer) this.mChildren.get(i)).asActivityRecord() != null) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanUpActivityReferences(ActivityRecord r) {
        ActivityRecord activityRecord = this.mPausingActivity;
        if (activityRecord != null && activityRecord == r) {
            this.mPausingActivity = null;
        }
        ActivityRecord activityRecord2 = this.mResumedActivity;
        if (activityRecord2 != null && activityRecord2 == r) {
            setResumedActivity(null, "cleanUpActivityReferences");
        }
        r.removeTimeouts();
    }

    protected boolean isForceHidden() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLeafTaskFragment() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((WindowContainer) this.mChildren.get(i)).asTaskFragment() != null) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityStateChanged(ActivityRecord record, ActivityRecord.State state, String reason) {
        warnForNonLeafTaskFragment("onActivityStateChanged");
        if (record == this.mResumedActivity && state != ActivityRecord.State.RESUMED) {
            setResumedActivity(null, reason + " - onActivityStateChanged");
        }
        if (state == ActivityRecord.State.RESUMED) {
            if (ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK) {
                Slog.v(TAG, "set resumed activity to:" + record + " reason:" + reason);
            }
            setResumedActivity(record, reason + " - onActivityStateChanged");
            if (record == this.mRootWindowContainer.getTopResumedActivity()) {
                this.mAtmService.setResumedActivityUncheckLocked(record, reason);
            }
            this.mTaskSupervisor.mRecentTasks.add(record.getTask());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleAppDied(WindowProcessController app) {
        warnForNonLeafTaskFragment("handleAppDied");
        boolean isPausingDied = false;
        ActivityRecord activityRecord = this.mPausingActivity;
        if (activityRecord != null && activityRecord.app == app) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(this.mPausingActivity);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1564228464, 0, (String) null, new Object[]{protoLogParam0});
            }
            this.mPausingActivity = null;
            isPausingDied = true;
        }
        ActivityRecord activityRecord2 = this.mLastPausedActivity;
        if (activityRecord2 != null && activityRecord2.app == app) {
            this.mLastPausedActivity = null;
        }
        return isPausingDied;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void awakeFromSleeping() {
        if (this.mPausingActivity != null) {
            Slog.d(TAG, "awakeFromSleeping: previously pausing activity didn't pause");
            this.mPausingActivity.activityPaused(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean sleepIfPossible(boolean shuttingDown) {
        boolean shouldSleep = true;
        if (this.mResumedActivity != null) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(this.mResumedActivity);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 987903142, 0, (String) null, new Object[]{protoLogParam0});
            }
            startPausing(false, true, null, "sleep");
            shouldSleep = false;
        } else if (this.mPausingActivity != null) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam02 = String.valueOf(this.mPausingActivity);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 1912291550, 0, (String) null, new Object[]{protoLogParam02});
            }
            shouldSleep = false;
        }
        if (!shuttingDown && containsStoppingActivity()) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                long protoLogParam03 = this.mTaskSupervisor.mStoppingActivities.size();
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 669361121, 1, (String) null, new Object[]{Long.valueOf(protoLogParam03)});
            }
            this.mTaskSupervisor.scheduleIdle();
            shouldSleep = false;
        }
        if (shouldSleep) {
            updateActivityVisibilities(null, 0, false, true);
        }
        return shouldSleep;
    }

    private boolean containsStoppingActivity() {
        for (int i = this.mTaskSupervisor.mStoppingActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = this.mTaskSupervisor.mStoppingActivities.get(i);
            if (r.getTaskFragment() == this) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTranslucent(ActivityRecord starting) {
        if (!isAttached() || isForceHidden()) {
            return true;
        }
        PooledPredicate p = PooledLambda.obtainPredicate(new BiPredicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda2
            @Override // java.util.function.BiPredicate
            public final boolean test(Object obj, Object obj2) {
                boolean isOpaqueActivity;
                isOpaqueActivity = TaskFragment.isOpaqueActivity((ActivityRecord) obj, (ActivityRecord) obj2);
                return isOpaqueActivity;
            }
        }, PooledLambda.__(ActivityRecord.class), starting);
        ActivityRecord opaque = getActivity(p);
        p.recycle();
        return opaque == null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isOpaqueActivity(ActivityRecord r, ActivityRecord starting) {
        if (r.finishing) {
            return false;
        }
        return (r.visibleIgnoringKeyguard || r == starting) && r.occludesParent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopNonFinishingActivity() {
        return getTopNonFinishingActivity(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopNonFinishingActivity(boolean includeOverlays) {
        return getTopNonFinishingActivity(includeOverlays, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getTopNonFinishingActivity(boolean includeOverlays, boolean includingEmbeddedTask) {
        if (includeOverlays) {
            if (includingEmbeddedTask) {
                return getActivity(new Predicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda3
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return TaskFragment.lambda$getTopNonFinishingActivity$2((ActivityRecord) obj);
                    }
                });
            }
            return getActivity(new Predicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda4
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return TaskFragment.this.m8338x404e9654((ActivityRecord) obj);
                }
            });
        } else if (includingEmbeddedTask) {
            return getActivity(new Predicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda5
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return TaskFragment.lambda$getTopNonFinishingActivity$4((ActivityRecord) obj);
                }
            });
        } else {
            return getActivity(new Predicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda6
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return TaskFragment.this.m8339x28cd0712((ActivityRecord) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopNonFinishingActivity$2(ActivityRecord r) {
        return !r.finishing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTopNonFinishingActivity$3$com-android-server-wm-TaskFragment  reason: not valid java name */
    public /* synthetic */ boolean m8338x404e9654(ActivityRecord r) {
        return !r.finishing && r.getTask() == getTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getTopNonFinishingActivity$4(ActivityRecord r) {
        return (r.finishing || r.isTaskOverlay()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getTopNonFinishingActivity$5$com-android-server-wm-TaskFragment  reason: not valid java name */
    public /* synthetic */ boolean m8339x28cd0712(ActivityRecord r) {
        return (r.finishing || r.isTaskOverlay() || r.getTask() != getTask()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topRunningActivity() {
        return topRunningActivity(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topRunningActivity(boolean focusableOnly) {
        return topRunningActivity(focusableOnly, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord topRunningActivity(boolean focusableOnly, boolean includingEmbeddedTask) {
        if (focusableOnly) {
            if (includingEmbeddedTask) {
                return getActivity(new Predicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda10
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return TaskFragment.lambda$topRunningActivity$6((ActivityRecord) obj);
                    }
                });
            }
            return getActivity(new Predicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda11
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return TaskFragment.this.m8341lambda$topRunningActivity$7$comandroidserverwmTaskFragment((ActivityRecord) obj);
                }
            });
        } else if (includingEmbeddedTask) {
            return getActivity(new Task$$ExternalSyntheticLambda22());
        } else {
            return getActivity(new Predicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda12
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return TaskFragment.this.m8342lambda$topRunningActivity$8$comandroidserverwmTaskFragment((ActivityRecord) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$topRunningActivity$6(ActivityRecord r) {
        return r.canBeTopRunning() && r.isFocusable();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$topRunningActivity$7$com-android-server-wm-TaskFragment  reason: not valid java name */
    public /* synthetic */ boolean m8341lambda$topRunningActivity$7$comandroidserverwmTaskFragment(ActivityRecord r) {
        return r.canBeTopRunning() && r.isFocusable() && r.getTask() == getTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$topRunningActivity$8$com-android-server-wm-TaskFragment  reason: not valid java name */
    public /* synthetic */ boolean m8342lambda$topRunningActivity$8$comandroidserverwmTaskFragment(ActivityRecord r) {
        return r.canBeTopRunning() && r.getTask() == getTask();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getNonFinishingActivityCount() {
        final int[] runningActivityCount = new int[1];
        forAllActivities(new Consumer() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskFragment.lambda$getNonFinishingActivityCount$9(runningActivityCount, (ActivityRecord) obj);
            }
        });
        return runningActivityCount[0];
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getNonFinishingActivityCount$9(int[] runningActivityCount, ActivityRecord a) {
        if (!a.finishing) {
            runningActivityCount[0] = runningActivityCount[0] + 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTopActivityFocusable() {
        ActivityRecord r = topRunningActivity();
        return r != null ? r.isFocusable() : isFocusable() && getWindowConfiguration().canReceiveKeys();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:105:?, code lost:
        return r9;
     */
    /* JADX WARN: Code restructure failed: missing block: B:80:0x00fb, code lost:
        if (r6 != false) goto L91;
     */
    /* JADX WARN: Code restructure failed: missing block: B:81:0x00fd, code lost:
        return 2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:82:0x00fe, code lost:
        if (r2 == false) goto L94;
     */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x0102, code lost:
        return 0;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int getVisibility(ActivityRecord starting) {
        TaskFragment taskFragment;
        if (!isAttached() || isForceHidden()) {
            return 2;
        }
        if (isTopActivityLaunchedBehind()) {
            return 0;
        }
        boolean gotTranslucentFullscreen = false;
        boolean gotTranslucentAdjacent = false;
        boolean shouldBeVisible = true;
        WindowContainer parent = getParent();
        int i = 1;
        if (parent.asTaskFragment() != null) {
            int parentVisibility = parent.asTaskFragment().getVisibility(starting);
            if (parentVisibility == 2) {
                return 2;
            }
            if (parentVisibility == 1) {
                gotTranslucentFullscreen = true;
            }
        }
        List<TaskFragment> adjacentTaskFragments = new ArrayList<>();
        int i2 = parent.getChildCount() - 1;
        while (true) {
            if (i2 < 0) {
                break;
            }
            WindowContainer other = parent.getChildAt(i2);
            if (other != null) {
                boolean hasRunningActivities = hasRunningActivity(other);
                if (other == this) {
                    if (!adjacentTaskFragments.isEmpty() && !gotTranslucentAdjacent) {
                        this.mTmpRect.set(getBounds());
                        for (int j = adjacentTaskFragments.size() - i; j >= 0; j--) {
                            TaskFragment taskFragment2 = adjacentTaskFragments.get(j);
                            TaskFragment adjacentTaskFragment = taskFragment2.mAdjacentTaskFragment;
                            if (adjacentTaskFragment != this && (this.mTmpRect.intersect(taskFragment2.getBounds()) || this.mTmpRect.intersect(adjacentTaskFragment.getBounds()))) {
                                return 2;
                            }
                        }
                    }
                    shouldBeVisible = hasRunningActivities || (starting != null && starting.isDescendantOf(this)) || isActivityTypeHome();
                    i = 1;
                } else if (!hasRunningActivities) {
                    i = 1;
                } else {
                    int otherWindowingMode = other.getWindowingMode();
                    i = 1;
                    if (otherWindowingMode == 1) {
                        if (!isTranslucent(other, starting)) {
                            return 2;
                        }
                        gotTranslucentFullscreen = true;
                    } else if (otherWindowingMode == 6 && other.matchParentBounds()) {
                        if (!isTranslucent(other, starting)) {
                            return 2;
                        }
                        gotTranslucentFullscreen = true;
                    } else {
                        TaskFragment otherTaskFrag = other.asTaskFragment();
                        if (otherTaskFrag != null && (taskFragment = otherTaskFrag.mAdjacentTaskFragment) != null) {
                            if (adjacentTaskFragments.contains(taskFragment)) {
                                if (!otherTaskFrag.isTranslucent(starting) && !otherTaskFrag.mAdjacentTaskFragment.isTranslucent(starting)) {
                                    return 2;
                                }
                                gotTranslucentFullscreen = true;
                                gotTranslucentAdjacent = true;
                            } else {
                                adjacentTaskFragments.add(otherTaskFrag);
                            }
                        }
                    }
                }
            }
            i2--;
        }
    }

    private static boolean hasRunningActivity(WindowContainer wc) {
        return wc.asTaskFragment() != null ? wc.asTaskFragment().topRunningActivity() != null : (wc.asActivityRecord() == null || wc.asActivityRecord().finishing) ? false : true;
    }

    private static boolean isTranslucent(WindowContainer wc, ActivityRecord starting) {
        if (wc.asTaskFragment() != null) {
            return wc.asTaskFragment().isTranslucent(starting);
        }
        if (wc.asActivityRecord() != null) {
            return !wc.asActivityRecord().occludesParent();
        }
        return false;
    }

    private boolean isTopActivityLaunchedBehind() {
        ActivityRecord top = topRunningActivity();
        return top != null && top.mLaunchTaskBehind;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void updateActivityVisibilities(ActivityRecord starting, int configChanges, boolean preserveWindows, boolean notifyClients) {
        this.mTaskSupervisor.beginActivityVisibilityUpdate();
        try {
            this.mEnsureActivitiesVisibleHelper.process(starting, configChanges, preserveWindows, notifyClients);
        } finally {
            this.mTaskSupervisor.endActivityVisibilityUpdate();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Type inference failed for: r13v1, types: [java.lang.Object[], java.lang.String] */
    public final boolean resumeTopActivity(ActivityRecord prev, ActivityOptions options, boolean deferPause) {
        ActivityRecord lastResumed;
        Task lastFocusedRootTask;
        String str;
        Object obj;
        boolean pausing;
        String str2;
        boolean anim;
        boolean z;
        boolean notUpdated;
        boolean z2;
        boolean z3;
        ActivityRecord activityRecord;
        boolean z4;
        boolean z5;
        ActivityRecord next = topRunningActivity(true);
        if (next == null || !next.canResumeByCompat()) {
            return false;
        }
        next.delayedResume = false;
        TaskDisplayArea taskDisplayArea = getDisplayArea();
        if (this.mResumedActivity == next && next.isState(ActivityRecord.State.RESUMED) && taskDisplayArea.allResumedActivitiesComplete()) {
            TaskDisplayArea defTda = this.mRootWindowContainer.getDefaultTaskDisplayArea();
            Task defTopRootTask = defTda.getFocusedRootTask();
            boolean isThundbackCloseTransit = ITranAppTransition.Instance().isThunderbackCloseTransition(defTda.mDisplayContent.mAppTransition.getNextAppTransitionRequests());
            if (isThundbackCloseTransit && defTopRootTask != null) {
                ActivityRecord defResumeRecord = defTopRootTask.getResumedActivity();
                ActivityRecord homeRecord = defTda.getHomeActivity();
                if (defTopRootTask.isTranslucent(defResumeRecord) && defResumeRecord != null && defResumeRecord.getActivityType() != 2 && homeRecord != null && homeRecord.isState(ActivityRecord.State.RESUMED) && homeRecord.getRootTask() != null && defTda.pauseBackTasks(defResumeRecord)) {
                    Slog.d(TAG, "resumeTopActivity, call pauseBackTasks, resuming:" + defResumeRecord);
                    homeRecord.getRootTask().startPausing(this.mTaskSupervisor.mUserLeaving, false, defResumeRecord, "resumeTopActivity");
                }
            }
            taskDisplayArea.ensureActivitiesVisible(null, 0, false, true);
            executeAppTransition(options);
            if (taskDisplayArea.inMultiWindowMode() && taskDisplayArea.mDisplayContent != null && taskDisplayArea.mDisplayContent.mFocusedApp != next) {
                taskDisplayArea.mDisplayContent.setFocusedApp(next);
            }
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(next);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, 675705156, 0, (String) null, new Object[]{protoLogParam0});
            }
            return false;
        }
        boolean allPausedComplete = this.mRootWindowContainer.allPausedActivitiesComplete();
        if (!allPausedComplete) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 935418348, 0, (String) null, (Object[]) null);
            }
            return false;
        } else if (this.mLastPausedActivity == next && shouldSleepOrShutDownActivities()) {
            executeAppTransition(options);
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -1886145147, 0, (String) null, (Object[]) null);
            }
            return false;
        } else if (getConfiguration().windowConfiguration.isThunderbackWindow() && this.mLastPausedActivity == next && getRootTask() != null && (getRootTask().shouldSleepOrShutDownActivities() || this.mRootWindowContainer.getDefaultDisplay().isKeyguardLocked())) {
            executeAppTransition(options);
            Slog.i(TAG, "resumeTopActivity: thunderback activity Going to sleep and all paused");
            return false;
        } else if (!this.mAtmService.mAmInternal.hasStartedUserState(next.mUserId)) {
            Slog.w(TAG, "Skipping resume of top activity " + next + ": user " + next.mUserId + " is stopped");
            return false;
        } else {
            this.mTaskSupervisor.mStoppingActivities.remove(next);
            if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
                Slog.v(TAG_SWITCH, "Resuming " + next);
            }
            this.mTaskSupervisor.setLaunchSource(next.info.applicationInfo.uid);
            Task lastFocusedRootTask2 = taskDisplayArea.getLastFocusedRootTask();
            if (lastFocusedRootTask2 != null && lastFocusedRootTask2 != getRootTaskFragment().asTask()) {
                ActivityRecord lastResumed2 = lastFocusedRootTask2.getTopResumedActivity();
                lastResumed = lastResumed2;
            } else {
                lastResumed = null;
            }
            ActivityRecord lastResumedBeforeActivitySwitch = lastResumed != null ? lastResumed : this.mResumedActivity;
            boolean pausing2 = !deferPause && taskDisplayArea.pauseBackTasks(next);
            if (this.mResumedActivity != null) {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam02 = String.valueOf(this.mResumedActivity);
                    lastFocusedRootTask = lastFocusedRootTask2;
                    str = TAG;
                    obj = null;
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, 102618780, 0, (String) null, new Object[]{protoLogParam02});
                } else {
                    lastFocusedRootTask = lastFocusedRootTask2;
                    str = TAG;
                    obj = null;
                }
                pausing = pausing2 | startPausing(this.mTaskSupervisor.mUserLeaving, false, next, "resumeTopActivity");
            } else {
                lastFocusedRootTask = lastFocusedRootTask2;
                str = TAG;
                obj = null;
                pausing = pausing2;
            }
            debugGriffinWhenResumeTopActivity(lastResumed, next, pausing);
            ITranTaskFragment.Instance().hookResumeTopActivity(lastResumed != null ? lastResumed : this.mResumedActivity, next, pausing);
            boolean isKeyguardShowing = getDisplayContent().getDisplayPolicy().isKeyguardShowing();
            ActivityRecord lastResumed3 = lastResumed;
            ?? r13 = obj;
            String str3 = str;
            this.mAtmService.mAmsExt.onBeforeActivitySwitch(lastResumedBeforeActivitySwitch, next, pausing, next.getActivityType(), isKeyguardShowing);
            if (!pausing) {
                if (this.mResumedActivity == next && next.isState(ActivityRecord.State.RESUMED) && taskDisplayArea.allResumedActivitiesComplete()) {
                    executeAppTransition(options);
                    if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                        String protoLogParam03 = String.valueOf(next);
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -2010331310, 0, (String) r13, new Object[]{protoLogParam03});
                        return true;
                    }
                    return true;
                }
                if (shouldSleepActivities()) {
                    this.mTaskSupervisor.finishNoHistoryActivitiesIfNeeded(next);
                }
                if (prev != null && prev != next && next.nowVisible) {
                    if (prev.finishing) {
                        prev.setVisibility(false);
                        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
                            Slog.v(TAG_SWITCH, "Not waiting for visible to hide: " + prev + ", nowVisible=" + next.nowVisible);
                        }
                    } else if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
                        Slog.v(TAG_SWITCH, "Previous already visible but still waiting to hide: " + prev + ", nowVisible=" + next.nowVisible);
                    }
                }
                try {
                    this.mTaskSupervisor.getActivityMetricsLogger().notifyBeforePackageUnstopped(next.packageName);
                    this.mAtmService.getPackageManager().setPackageStoppedState(next.packageName, false, next.mUserId);
                    str2 = str3;
                } catch (RemoteException e) {
                    str2 = str3;
                } catch (IllegalArgumentException e2) {
                    str2 = str3;
                    Slog.w(str2, "Failed trying to unstop package " + next.packageName + ": " + e2);
                }
                boolean anim2 = true;
                DisplayContent dc = taskDisplayArea.mDisplayContent;
                if (prev != null) {
                    if (prev.finishing) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_TRANSITION) {
                            Slog.v(TAG_TRANSITION, "Prepare close transition: prev=" + prev);
                        }
                        if (this.mTaskSupervisor.mNoAnimActivities.contains(prev)) {
                            anim2 = false;
                            z4 = false;
                            dc.prepareAppTransition(0);
                        } else {
                            z4 = false;
                            dc.prepareAppTransition(2);
                        }
                        prev.setVisibility(z4);
                        anim = anim2;
                    } else {
                        if (ActivityTaskManagerDebugConfig.DEBUG_TRANSITION) {
                            Slog.v(TAG_TRANSITION, "Prepare open transition: prev=" + prev);
                        }
                        if (this.mTaskSupervisor.mNoAnimActivities.contains(next)) {
                            dc.prepareAppTransition(0);
                            anim = false;
                        } else {
                            dc.prepareAppTransition(1, next.mLaunchTaskBehind ? 32 : 0);
                            anim = true;
                        }
                    }
                } else {
                    if (ActivityTaskManagerDebugConfig.DEBUG_TRANSITION) {
                        Slog.v(TAG_TRANSITION, "Prepare open transition: no previous");
                    }
                    if (this.mTaskSupervisor.mNoAnimActivities.contains(next)) {
                        dc.prepareAppTransition(0);
                        anim = false;
                    } else {
                        dc.prepareAppTransition(1);
                        anim = true;
                    }
                }
                boolean locked = ITranWindowManagerService.Instance().onActivityResume(next, getDisplayId());
                if (locked) {
                    return false;
                }
                if (anim) {
                    next.applyOptionsAnimation();
                } else {
                    next.abortAndClearOptionsAnimation();
                }
                this.mTaskSupervisor.mNoAnimActivities.clear();
                if (next.attachedToProcess()) {
                    if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
                        Slog.v(TAG_SWITCH, "Resume running: " + next + " stopped=" + next.stopped + " visibleRequested=" + next.mVisibleRequested);
                    }
                    boolean lastActivityTranslucent = inMultiWindowMode() || !((activityRecord = this.mLastPausedActivity) == null || activityRecord.occludesParent());
                    next.cancelDeferShowForPinnedMode();
                    if (!next.mVisibleRequested || next.stopped || lastActivityTranslucent) {
                        next.app.addToPendingTop();
                        next.setVisibility(true);
                    }
                    next.startLaunchTickingLocked();
                    ActivityRecord lastResumedActivity = lastFocusedRootTask == null ? null : lastFocusedRootTask.getTopResumedActivity();
                    ActivityRecord.State lastState = next.getState();
                    this.mAtmService.updateCpuStats();
                    if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                        String protoLogParam04 = String.valueOf(next);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1421296808, 0, (String) null, new Object[]{protoLogParam04});
                    }
                    next.setState(ActivityRecord.State.RESUMED, "resumeTopActivity");
                    ITranWindowManagerService.Instance().onActivityResume(next);
                    debugGriffinBeforeResumeTopActivity(next);
                    ITranTaskFragment.Instance().hookResumeTopActivityEnd(next);
                    if (next != null && next.info != null) {
                        ITranWindowManagerService.Instance().setActivityResumedHook(next);
                    }
                    this.mAtmService.mAmsExt.onAfterActivityResumed(next);
                    if (!shouldBeVisible(next)) {
                        notUpdated = true;
                    } else {
                        notUpdated = !this.mRootWindowContainer.ensureVisibilityAndConfig(next, getDisplayId(), true, false);
                    }
                    if (notUpdated) {
                        ActivityRecord nextNext = topRunningActivity();
                        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                            String protoLogParam05 = String.valueOf(next);
                            String protoLogParam1 = String.valueOf(nextNext);
                            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_STATES, -310337305, 0, (String) null, new Object[]{protoLogParam05, protoLogParam1});
                        }
                        if (nextNext != next) {
                            this.mTaskSupervisor.scheduleResumeTopActivities();
                        }
                        if (!next.mVisibleRequested || next.stopped) {
                            z3 = true;
                            next.setVisibility(true);
                        } else {
                            z3 = true;
                        }
                        next.completeResumeLocked();
                        return z3;
                    }
                    try {
                        ClientTransaction transaction = ClientTransaction.obtain(next.app.getThread(), next.token);
                        ArrayList<ResultInfo> a = next.results;
                        if (a != null) {
                            int size = a.size();
                            if (!next.finishing && size > 0) {
                                if (ActivityTaskManagerDebugConfig.DEBUG_RESULTS) {
                                    Slog.v(TAG_RESULTS, "Delivering results to " + next + ": " + a);
                                }
                                transaction.addCallback(ActivityResultItem.obtain(a));
                            }
                        }
                        if (next.newIntents != null) {
                            transaction.addCallback(NewIntentItem.obtain(next.newIntents, true));
                        }
                        next.notifyAppResumed(next.stopped);
                        if (next != null && next.getDisplayArea().mFeatureId == 1) {
                            ITranSourceConnectManager.Instance().hookDisplayResumedActivityChanged(next.getDisplayId(), next.packageName);
                        }
                        EventLogTags.writeWmResumeActivity(next.mUserId, System.identityHashCode(next), next.getTask().mTaskId, next.shortComponentName);
                        this.mAtmService.getAppWarningsLocked().onResumeActivity(next);
                        next.app.setPendingUiCleanAndForceProcessStateUpTo(this.mAtmService.mTopProcessState);
                        next.abortAndClearOptionsAnimation();
                        transaction.setLifecycleStateRequest(ResumeActivityItem.obtain(next.app.getReportedProcState(), dc.isNextTransitionForward()));
                        this.mAtmService.getLifecycleManager().scheduleTransaction(transaction);
                        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                            String protoLogParam06 = String.valueOf(next);
                            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -1419461256, 0, (String) null, new Object[]{protoLogParam06});
                        }
                        try {
                            next.completeResumeLocked();
                            return true;
                        } catch (Exception e3) {
                            Slog.w(str2, "Exception thrown during resume of " + next, e3);
                            next.finishIfPossible("resume-exception", true);
                            return true;
                        }
                    } catch (Exception e4) {
                        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                            String protoLogParam07 = String.valueOf(lastState);
                            String protoLogParam12 = String.valueOf(next);
                            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -80004683, 0, (String) null, new Object[]{protoLogParam07, protoLogParam12});
                        }
                        next.setState(lastState, "resumeTopActivityInnerLocked");
                        if (lastResumedActivity != null) {
                            lastResumedActivity.setState(ActivityRecord.State.RESUMED, "resumeTopActivityInnerLocked");
                        }
                        Slog.i(str2, "Restarting because process died: " + next);
                        if (!next.hasBeenLaunched) {
                            next.hasBeenLaunched = true;
                            z2 = false;
                        } else if (lastFocusedRootTask == null) {
                            z2 = false;
                        } else if (!lastFocusedRootTask.isTopRootTaskInDisplayArea()) {
                            z2 = false;
                        } else {
                            z2 = false;
                            next.showStartingWindow(false);
                        }
                        this.mTaskSupervisor.startSpecificActivity(next, true, z2);
                        return true;
                    }
                }
                if (!next.hasBeenLaunched) {
                    next.hasBeenLaunched = true;
                } else {
                    next.showStartingWindow(false);
                    if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
                        Slog.v(TAG_SWITCH, "Restarting: " + next);
                    }
                }
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam08 = String.valueOf(next);
                    z = true;
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, 1856783490, 0, (String) null, new Object[]{protoLogParam08});
                } else {
                    z = true;
                }
                this.mTaskSupervisor.startSpecificActivity(next, z, z);
                return z;
            }
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                Object[] objArr = (Object[]) r13;
                z5 = false;
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 341055768, 0, (String) r13, (Object[]) r13);
            } else {
                z5 = false;
            }
            if (next.attachedToProcess()) {
                next.app.updateProcessInfo(z5, true, z5, z5);
            } else if (!next.isProcessRunning()) {
                boolean isTop = this == taskDisplayArea.getFocusedRootTask();
                this.mAtmService.startProcessAsync(next, false, isTop, isTop ? HostingRecord.HOSTING_TYPE_NEXT_TOP_ACTIVITY : HostingRecord.HOSTING_TYPE_NEXT_ACTIVITY);
            }
            if (lastResumed3 != null) {
                lastResumed3.setWillCloseOrEnterPip(true);
                return true;
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldSleepOrShutDownActivities() {
        return shouldSleepActivities() || this.mAtmService.mShuttingDown;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldBeVisible(ActivityRecord starting) {
        return getVisibility(starting) != 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canBeResumed(ActivityRecord starting) {
        return isTopActivityFocusable() && getVisibility(starting) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFocusableAndVisible() {
        return isTopActivityFocusable() && shouldBeVisible(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final boolean startPausing(boolean uiSleeping, ActivityRecord resuming, String reason) {
        return startPausing(this.mTaskSupervisor.mUserLeaving, uiSleeping, resuming, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startPausing(boolean userLeaving, boolean uiSleeping, ActivityRecord resuming, String reason) {
        if (hasDirectChildActivities()) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(this);
                String protoLogParam1 = String.valueOf(this.mResumedActivity);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -248761393, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
            }
            if (this.mPausingActivity != null) {
                Slog.w(TAG, "Going to pause when pause is already pending for " + this.mPausingActivity + " state=" + this.mPausingActivity.getState());
                if (!shouldSleepActivities()) {
                    completePause(false, resuming);
                }
            }
            ActivityRecord prev = this.mResumedActivity;
            if (prev == null) {
                if (resuming == null) {
                    Slog.wtf(TAG, "Trying to pause when nothing is resumed");
                    this.mRootWindowContainer.resumeFocusedTasksTopActivities();
                }
                return false;
            } else if (prev == resuming) {
                Slog.wtf(TAG, "Trying to pause activity that is in process of being resumed");
                return false;
            } else {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam02 = String.valueOf(prev);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -957060823, 0, (String) null, new Object[]{protoLogParam02});
                }
                this.mPausingActivity = prev;
                this.mLastPausedActivity = prev;
                if (!prev.finishing && prev.isNoHistory() && !this.mTaskSupervisor.mNoHistoryActivities.contains(prev)) {
                    this.mTaskSupervisor.mNoHistoryActivities.add(prev);
                }
                prev.setState(ActivityRecord.State.PAUSING, "startPausingLocked");
                prev.getTask().touchActiveTime();
                this.mAtmService.updateCpuStats();
                boolean pauseImmediately = false;
                boolean shouldAutoPip = false;
                if (resuming != null) {
                    boolean lastResumedCanPip = prev.checkEnterPictureInPictureState("shouldAutoPipWhilePausing", userLeaving);
                    if (userLeaving && lastResumedCanPip && prev.pictureInPictureArgs.isAutoEnterEnabled()) {
                        shouldAutoPip = true;
                    } else if (!lastResumedCanPip) {
                        pauseImmediately = (resuming.info.flags & 16384) != 0;
                    }
                }
                if (!prev.attachedToProcess()) {
                    this.mPausingActivity = null;
                    this.mLastPausedActivity = null;
                    this.mTaskSupervisor.mNoHistoryActivities.remove(prev);
                } else if (!shouldAutoPip) {
                    schedulePauseActivity(prev, userLeaving, pauseImmediately, reason);
                } else {
                    boolean didAutoPip = this.mAtmService.enterPictureInPictureMode(prev, prev.pictureInPictureArgs);
                    if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                        String protoLogParam03 = String.valueOf(prev);
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -1101551167, 12, (String) null, new Object[]{protoLogParam03, Boolean.valueOf(didAutoPip)});
                    }
                }
                if (!uiSleeping && !this.mAtmService.isSleepingOrShuttingDownLocked()) {
                    this.mTaskSupervisor.acquireLaunchWakelock();
                }
                if (this.mPausingActivity != null) {
                    if (!uiSleeping) {
                        prev.pauseKeyDispatchingLocked();
                    } else if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1633115609, 0, (String) null, (Object[]) null);
                    }
                    if (pauseImmediately) {
                        completePause(false, resuming);
                        return false;
                    }
                    prev.schedulePauseTimeout();
                    this.mTransitionController.setReady(this, false);
                    return true;
                }
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -648891906, 0, (String) null, (Object[]) null);
                }
                if (resuming == null) {
                    this.mRootWindowContainer.resumeFocusedTasksTopActivities();
                }
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void schedulePauseActivity(ActivityRecord prev, boolean userLeaving, boolean pauseImmediately, String reason) {
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(prev);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 378825104, 0, (String) null, new Object[]{protoLogParam0});
        }
        try {
            EventLogTags.writeWmPauseActivity(prev.mUserId, System.identityHashCode(prev), prev.shortComponentName, "userLeaving=" + userLeaving, reason);
            this.mAtmService.getLifecycleManager().scheduleTransaction(prev.app.getThread(), prev.token, (ActivityLifecycleItem) PauseActivityItem.obtain(prev.finishing, userLeaving, prev.configChangeFlags, pauseImmediately));
        } catch (Exception e) {
            Slog.w(TAG, "Exception thrown during pause", e);
            this.mPausingActivity = null;
            this.mLastPausedActivity = null;
            this.mTaskSupervisor.mNoHistoryActivities.remove(prev);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void completePause(boolean resumeNext, ActivityRecord resuming) {
        ActivityRecord prev = this.mPausingActivity;
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(prev);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 327461496, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (prev != null) {
            prev.setWillCloseOrEnterPip(false);
            boolean wasStopping = prev.isState(ActivityRecord.State.STOPPING);
            prev.setState(ActivityRecord.State.PAUSED, "completePausedLocked");
            if (prev.finishing) {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam02 = String.valueOf(prev);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -312353598, 0, (String) null, new Object[]{protoLogParam02});
                }
                prev = prev.completeFinishing(false, "completePausedLocked");
            } else if (prev.hasProcess()) {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam03 = String.valueOf(prev);
                    boolean protoLogParam2 = prev.mVisibleRequested;
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1187377055, 60, (String) null, new Object[]{protoLogParam03, Boolean.valueOf(wasStopping), Boolean.valueOf(protoLogParam2)});
                }
                if (prev.deferRelaunchUntilPaused) {
                    if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                        String protoLogParam04 = String.valueOf(prev);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 1011462000, 0, (String) null, new Object[]{protoLogParam04});
                    }
                    prev.relaunchActivityLocked(prev.preserveWindowOnDeferredRelaunch);
                } else if (wasStopping) {
                    prev.setState(ActivityRecord.State.STOPPING, "completePausedLocked");
                } else if (!prev.mVisibleRequested || shouldSleepOrShutDownActivities()) {
                    prev.setDeferHidingClient(false);
                    prev.addToStopping(true, false, "completePauseLocked");
                }
            } else {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam05 = String.valueOf(prev);
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -521613870, 0, (String) null, new Object[]{protoLogParam05});
                }
                prev = null;
            }
            if (prev != null) {
                prev.stopFreezingScreenLocked(true);
            }
            this.mPausingActivity = null;
        }
        if (resumeNext) {
            Task topRootTask = this.mRootWindowContainer.getTopDisplayFocusedRootTask();
            if (topRootTask != null && !topRootTask.shouldSleepOrShutDownActivities()) {
                this.mRootWindowContainer.resumeFocusedTasksTopActivities(topRootTask, prev, null);
            } else {
                ActivityRecord top = topRootTask != null ? topRootTask.topRunningActivity() : null;
                if (top == null || (prev != null && top != prev)) {
                    this.mRootWindowContainer.resumeFocusedTasksTopActivities();
                }
            }
        }
        if (prev != null) {
            prev.resumeKeyDispatchingLocked();
        }
        this.mRootWindowContainer.ensureActivitiesVisible(resuming, 0, false);
        if (this.mTaskSupervisor.mAppVisibilitiesChangedSinceLastPause || (getDisplayArea() != null && getDisplayArea().hasPinnedTask())) {
            this.mAtmService.getTaskChangeNotificationController().notifyTaskStackChanged();
            this.mTaskSupervisor.mAppVisibilitiesChangedSinceLastPause = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void forAllTaskFragments(Consumer<TaskFragment> callback, boolean traverseTopToBottom) {
        super.forAllTaskFragments(callback, traverseTopToBottom);
        callback.accept(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void forAllLeafTaskFragments(Consumer<TaskFragment> callback, boolean traverseTopToBottom) {
        int count = this.mChildren.size();
        boolean isLeafTaskFrag = true;
        if (traverseTopToBottom) {
            for (int i = count - 1; i >= 0; i--) {
                TaskFragment child = ((WindowContainer) this.mChildren.get(i)).asTaskFragment();
                if (child != null) {
                    isLeafTaskFrag = false;
                    child.forAllLeafTaskFragments(callback, traverseTopToBottom);
                }
            }
        } else {
            for (int i2 = 0; i2 < count; i2++) {
                TaskFragment child2 = ((WindowContainer) this.mChildren.get(i2)).asTaskFragment();
                if (child2 != null) {
                    isLeafTaskFrag = false;
                    child2.forAllLeafTaskFragments(callback, traverseTopToBottom);
                }
            }
        }
        if (isLeafTaskFrag) {
            callback.accept(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean forAllLeafTaskFragments(Predicate<TaskFragment> callback) {
        boolean isLeafTaskFrag = true;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            TaskFragment child = ((WindowContainer) this.mChildren.get(i)).asTaskFragment();
            if (child != null) {
                isLeafTaskFrag = false;
                if (child.forAllLeafTaskFragments(callback)) {
                    return true;
                }
            }
        }
        if (isLeafTaskFrag) {
            return callback.test(this);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addChild(ActivityRecord r) {
        addChild(r, Integer.MAX_VALUE);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void addChild(WindowContainer child, int index) {
        boolean isAddingActivity;
        boolean taskHadActivity;
        ActivityRecord r = topRunningActivity();
        this.mClearedTaskForReuse = false;
        this.mClearedTaskFragmentForPip = false;
        ActivityRecord addingActivity = child.asActivityRecord();
        if (addingActivity == null) {
            isAddingActivity = false;
        } else {
            isAddingActivity = true;
        }
        Task task = isAddingActivity ? getTask() : null;
        if (isAddingActivity && task != null) {
            child.asActivityRecord().setMinAspectRatioForUser(getMinAspectRatioForUser());
            child.asActivityRecord().setIsPkgInActivityEmbedding(isPkgInActivityEmbedding());
        }
        if (task == null || task.getTopMostActivity() == null) {
            taskHadActivity = false;
        } else {
            taskHadActivity = true;
        }
        int activityType = task != null ? task.getActivityType() : 0;
        super.addChild((TaskFragment) child, index);
        if (isAddingActivity && task != null) {
            if (r != null && BackNavigationController.isScreenshotEnabled()) {
                if (ProtoLogCache.WM_DEBUG_BACK_PREVIEW_enabled) {
                    String protoLogParam0 = String.valueOf(r.mActivityComponent.flattenToString());
                    ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_BACK_PREVIEW, -134091882, 0, "Screenshotting Activity %s", new Object[]{protoLogParam0});
                }
                Rect outBounds = r.getBounds();
                SurfaceControl.ScreenshotHardwareBuffer backBuffer = SurfaceControl.captureLayers(r.mSurfaceControl, new Rect(0, 0, outBounds.width(), outBounds.height()), 1.0f);
                this.mBackScreenshots.put(r.mActivityComponent.flattenToString(), backBuffer);
            }
            child.asActivityRecord().inHistory = true;
            task.onDescendantActivityAdded(taskHadActivity, activityType, addingActivity);
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void onChildPositionChanged(WindowContainer child) {
        super.onChildPositionChanged(child);
        sendTaskFragmentInfoChanged();
    }

    void executeAppTransition(ActivityOptions options) {
    }

    @Override // com.android.server.wm.WindowContainer
    RemoteAnimationTarget createRemoteAnimationTarget(RemoteAnimationController.RemoteAnimationRecord record) {
        ActivityRecord activity;
        if (record.getMode() == 0) {
            activity = getActivity(new Predicate() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda7
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return TaskFragment.lambda$createRemoteAnimationTarget$10((ActivityRecord) obj);
                }
            });
        } else {
            activity = getTopMostActivity();
        }
        if (activity != null) {
            return activity.createRemoteAnimationTarget(record);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$createRemoteAnimationTarget$10(ActivityRecord r) {
        return !r.finishing && r.hasChild();
    }

    @Override // com.android.server.wm.WindowContainer
    boolean canCreateRemoteAnimationTarget() {
        return true;
    }

    boolean shouldSleepActivities() {
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public void resolveOverrideConfiguration(Configuration newParentConfig) {
        this.mTmpBounds.set(getResolvedOverrideConfiguration().windowConfiguration.getBounds());
        super.resolveOverrideConfiguration(newParentConfig);
        int windowingMode = getResolvedOverrideConfiguration().windowConfiguration.getWindowingMode();
        int parentWindowingMode = newParentConfig.windowConfiguration.getWindowingMode();
        if (getActivityType() == 2 && windowingMode == 0) {
            windowingMode = 1;
            getResolvedOverrideConfiguration().windowConfiguration.setWindowingMode(1);
        }
        if (!supportsMultiWindow()) {
            int candidateWindowingMode = windowingMode != 0 ? windowingMode : parentWindowingMode;
            if (WindowConfiguration.inMultiWindowMode(candidateWindowingMode) && candidateWindowingMode != 2) {
                getResolvedOverrideConfiguration().windowConfiguration.setWindowingMode(1);
            }
        }
        Task thisTask = asTask();
        if (thisTask != null && !thisTask.isEmbedded()) {
            thisTask.resolveLeafTaskOnlyOverrideConfigs(newParentConfig, this.mTmpBounds);
        }
        computeConfigResourceOverrides(getResolvedOverrideConfiguration(), newParentConfig);
        if (this.mIsEmbedded && newParentConfig.windowConfiguration.isThunderbackWindow()) {
            getResolvedOverrideConfiguration().windowConfiguration.getBounds().setEmpty();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsMultiWindow() {
        return supportsMultiWindowInDisplayArea(getDisplayArea());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean supportsMultiWindowInDisplayArea(TaskDisplayArea tda) {
        Task task;
        if (!this.mAtmService.mSupportsMultiWindow || tda == null || (task = getTask()) == null) {
            return false;
        }
        if (task.isResizeable() || tda.supportsNonResizableMultiWindow()) {
            ActivityRecord rootActivity = task.getRootActivity();
            return tda.supportsActivityMinWidthHeightMultiWindow(this.mMinWidth, this.mMinHeight, rootActivity != null ? rootActivity.info : null);
        }
        return false;
    }

    private int getTaskId() {
        if (getTask() != null) {
            return getTask().mTaskId;
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void ensureVisibleActivitiesConfiguration(ActivityRecord start, boolean preserveWindow) {
        this.mEnsureVisibleActivitiesConfigHelper.process(start, preserveWindow);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void computeConfigResourceOverrides(Configuration inOutConfig, Configuration parentConfig) {
        computeConfigResourceOverrides(inOutConfig, parentConfig, null, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void computeConfigResourceOverrides(Configuration inOutConfig, Configuration parentConfig, DisplayInfo overrideDisplayInfo) {
        if (overrideDisplayInfo != null) {
            inOutConfig.screenLayout = 0;
            invalidateAppBoundsConfig(inOutConfig);
        }
        computeConfigResourceOverrides(inOutConfig, parentConfig, overrideDisplayInfo, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void computeConfigResourceOverrides(Configuration inOutConfig, Configuration parentConfig, ActivityRecord.CompatDisplayInsets compatInsets) {
        if (compatInsets != null) {
            invalidateAppBoundsConfig(inOutConfig);
        }
        computeConfigResourceOverrides(inOutConfig, parentConfig, null, compatInsets);
    }

    private static void invalidateAppBoundsConfig(Configuration inOutConfig) {
        Rect appBounds = inOutConfig.windowConfiguration.getAppBounds();
        if (appBounds != null) {
            appBounds.setEmpty();
        }
        inOutConfig.screenWidthDp = 0;
        inOutConfig.screenHeightDp = 0;
    }

    void computeConfigResourceOverrides(Configuration inOutConfig, Configuration parentConfig, DisplayInfo overrideDisplayInfo, ActivityRecord.CompatDisplayInsets compatInsets) {
        boolean insideParentBounds;
        Rect containingAppBounds;
        DisplayInfo di;
        int i;
        int i2;
        int windowingMode = inOutConfig.windowConfiguration.getWindowingMode();
        if (windowingMode == 0) {
            windowingMode = parentConfig.windowConfiguration.getWindowingMode();
        }
        float density = inOutConfig.densityDpi;
        if (density == 0.0f) {
            density = parentConfig.densityDpi;
        }
        float density2 = density * 0.00625f;
        Rect parentBounds = parentConfig.windowConfiguration.getBounds();
        Rect resolvedBounds = inOutConfig.windowConfiguration.getBounds();
        if (resolvedBounds == null || resolvedBounds.isEmpty()) {
            this.mTmpFullBounds.set(parentBounds);
            insideParentBounds = true;
        } else {
            this.mTmpFullBounds.set(resolvedBounds);
            insideParentBounds = parentBounds.contains(resolvedBounds);
        }
        boolean customContainerPolicy = compatInsets != null;
        Rect outAppBounds = inOutConfig.windowConfiguration.getAppBounds();
        if (outAppBounds == null || outAppBounds.isEmpty()) {
            inOutConfig.windowConfiguration.setAppBounds(this.mTmpFullBounds);
            outAppBounds = inOutConfig.windowConfiguration.getAppBounds();
            if (!customContainerPolicy && windowingMode != 5) {
                if (insideParentBounds) {
                    containingAppBounds = parentConfig.windowConfiguration.getAppBounds();
                } else {
                    TaskDisplayArea displayArea = getDisplayArea();
                    containingAppBounds = displayArea != null ? displayArea.getWindowConfiguration().getAppBounds() : null;
                }
                if (containingAppBounds != null && !containingAppBounds.isEmpty()) {
                    outAppBounds.intersect(containingAppBounds);
                }
            }
        }
        if (inOutConfig.screenWidthDp == 0 || inOutConfig.screenHeightDp == 0) {
            if (!customContainerPolicy && WindowConfiguration.isFloating(windowingMode)) {
                this.mTmpNonDecorBounds.set(this.mTmpFullBounds);
                this.mTmpStableBounds.set(this.mTmpFullBounds);
            } else if (!customContainerPolicy && (overrideDisplayInfo != null || getDisplayContent() != null)) {
                if (overrideDisplayInfo != null) {
                    di = overrideDisplayInfo;
                } else {
                    di = getDisplayContent().getDisplayInfo();
                }
                calculateInsetFrames(this.mTmpNonDecorBounds, this.mTmpStableBounds, this.mTmpFullBounds, di);
            } else {
                int rotation = inOutConfig.windowConfiguration.getRotation();
                if (rotation == -1) {
                    rotation = parentConfig.windowConfiguration.getRotation();
                }
                if (rotation != -1 && customContainerPolicy) {
                    this.mTmpNonDecorBounds.set(this.mTmpFullBounds);
                    this.mTmpStableBounds.set(this.mTmpFullBounds);
                    compatInsets.getBoundsByRotation(this.mTmpBounds, rotation);
                    intersectWithInsetsIfFits(this.mTmpNonDecorBounds, this.mTmpBounds, compatInsets.mNonDecorInsets[rotation]);
                    intersectWithInsetsIfFits(this.mTmpStableBounds, this.mTmpBounds, compatInsets.mStableInsets[rotation]);
                    outAppBounds.set(this.mTmpNonDecorBounds);
                } else {
                    this.mTmpNonDecorBounds.set(outAppBounds);
                    this.mTmpStableBounds.set(outAppBounds);
                }
            }
            if (inOutConfig.screenWidthDp == 0) {
                int overrideScreenWidthDp = (int) (this.mTmpStableBounds.width() / density2);
                if (insideParentBounds && !customContainerPolicy) {
                    i2 = Math.min(overrideScreenWidthDp, parentConfig.screenWidthDp);
                } else {
                    i2 = overrideScreenWidthDp;
                }
                inOutConfig.screenWidthDp = i2;
            }
            if (inOutConfig.screenHeightDp == 0) {
                int overrideScreenHeightDp = (int) (this.mTmpStableBounds.height() / density2);
                if (insideParentBounds && !customContainerPolicy) {
                    i = Math.min(overrideScreenHeightDp, parentConfig.screenHeightDp);
                } else {
                    i = overrideScreenHeightDp;
                }
                inOutConfig.screenHeightDp = i;
            }
            if (inOutConfig.smallestScreenWidthDp == 0) {
                boolean inPipTransition = windowingMode == 2 && !this.mTmpFullBounds.isEmpty() && this.mTmpFullBounds.equals(parentBounds);
                if (WindowConfiguration.isFloating(windowingMode) && !inPipTransition) {
                    inOutConfig.smallestScreenWidthDp = (int) (Math.min(this.mTmpFullBounds.width(), this.mTmpFullBounds.height()) / density2);
                } else if (isEmbedding() || getMinAspectRatioForUser() != -1.0f) {
                    inOutConfig.smallestScreenWidthDp = Math.min(inOutConfig.screenWidthDp, inOutConfig.screenHeightDp);
                }
            }
        }
        if (inOutConfig.orientation == 0) {
            inOutConfig.orientation = inOutConfig.screenWidthDp <= inOutConfig.screenHeightDp ? 1 : 2;
        }
        if (inOutConfig.screenLayout == 0) {
            int compatScreenWidthDp = (int) (this.mTmpNonDecorBounds.width() / density2);
            int compatScreenHeightDp = (int) (this.mTmpNonDecorBounds.height() / density2);
            if (inOutConfig.screenWidthDp != 0) {
                compatScreenWidthDp = inOutConfig.screenWidthDp;
            }
            if (inOutConfig.screenHeightDp != 0) {
                compatScreenHeightDp = inOutConfig.screenHeightDp;
            }
            inOutConfig.screenLayout = computeScreenLayoutOverride(parentConfig.screenLayout, compatScreenWidthDp, compatScreenHeightDp);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void calculateInsetFrames(Rect outNonDecorBounds, Rect outStableBounds, Rect bounds, DisplayInfo displayInfo) {
        outNonDecorBounds.set(bounds);
        outStableBounds.set(bounds);
        Task rootTask = getRootTaskFragment().asTask();
        if (rootTask == null || rootTask.mDisplayContent == null) {
            return;
        }
        this.mTmpBounds.set(0, 0, displayInfo.logicalWidth, displayInfo.logicalHeight);
        DisplayPolicy policy = rootTask.mDisplayContent.getDisplayPolicy();
        policy.getNonDecorInsetsLw(displayInfo.rotation, displayInfo.displayCutout, this.mTmpInsets);
        intersectWithInsetsIfFits(outNonDecorBounds, this.mTmpBounds, this.mTmpInsets);
        policy.convertNonDecorInsetsToStableInsets(this.mTmpInsets, displayInfo.rotation);
        intersectWithInsetsIfFits(outStableBounds, this.mTmpBounds, this.mTmpInsets);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void intersectWithInsetsIfFits(Rect inOutBounds, Rect intersectBounds, Rect intersectInsets) {
        if (inOutBounds.right <= intersectBounds.right) {
            inOutBounds.right = Math.min(intersectBounds.right - intersectInsets.right, inOutBounds.right);
        }
        if (inOutBounds.bottom <= intersectBounds.bottom) {
            inOutBounds.bottom = Math.min(intersectBounds.bottom - intersectInsets.bottom, inOutBounds.bottom);
        }
        if (inOutBounds.left >= intersectBounds.left) {
            inOutBounds.left = Math.max(intersectBounds.left + intersectInsets.left, inOutBounds.left);
        }
        if (inOutBounds.top >= intersectBounds.top) {
            inOutBounds.top = Math.max(intersectBounds.top + intersectInsets.top, inOutBounds.top);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int computeScreenLayoutOverride(int sourceScreenLayout, int screenWidthDp, int screenHeightDp) {
        int longSize = Math.max(screenWidthDp, screenHeightDp);
        int shortSize = Math.min(screenWidthDp, screenHeightDp);
        return Configuration.reduceScreenLayout(sourceScreenLayout & 63, longSize, shortSize);
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public int getActivityType() {
        int applicationType = super.getActivityType();
        if (applicationType != 0 || !hasChild()) {
            return applicationType;
        }
        ActivityRecord activity = getTopMostActivity();
        return activity != null ? activity.getActivityType() : getTopChild().getActivityType();
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void onConfigurationChanged(Configuration newParentConfig) {
        if (this.mTaskFragmentOrganizer != null) {
            this.mTmpPrevBounds.set(getBounds());
        }
        super.onConfigurationChanged(newParentConfig);
        boolean shouldStartChangeTransition = shouldStartChangeTransition(this.mTmpPrevBounds);
        if (shouldStartChangeTransition) {
            if (!isEmbedding() && getConfiguration().windowConfiguration != null) {
                initializeChangeTransition(getConfiguration().windowConfiguration.getBounds());
            } else {
                initializeChangeTransition(this.mTmpPrevBounds);
            }
        }
        if (this.mTaskFragmentOrganizer != null) {
            if (this.mTransitionController.isShellTransitionsEnabled() && !this.mTransitionController.isCollecting(this)) {
                updateOrganizedTaskFragmentSurface();
            } else if (!this.mTransitionController.isShellTransitionsEnabled() && !shouldStartChangeTransition) {
                updateOrganizedTaskFragmentSurface();
            }
        }
        sendTaskFragmentInfoChanged();
    }

    private void updateOrganizedTaskFragmentSurface() {
        SurfaceControl.Transaction t = getSyncTransaction();
        updateSurfacePosition(t);
        updateOrganizedTaskFragmentSurfaceSize(t, false);
    }

    private void updateOrganizedTaskFragmentSurfaceSize(SurfaceControl.Transaction t, boolean forceUpdate) {
        if (this.mTaskFragmentOrganizer == null || this.mSurfaceControl == null || this.mSurfaceAnimator.hasLeash() || this.mSurfaceFreezer.hasLeash()) {
            return;
        }
        Rect bounds = getBounds();
        int width = bounds.width();
        int height = bounds.height();
        if (!forceUpdate && width == this.mLastSurfaceSize.x && height == this.mLastSurfaceSize.y) {
            return;
        }
        t.setWindowCrop(this.mSurfaceControl, width, height);
        this.mLastSurfaceSize.set(width, height);
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public void onAnimationLeashCreated(SurfaceControl.Transaction t, SurfaceControl leash) {
        super.onAnimationLeashCreated(t, leash);
        if (this.mTaskFragmentOrganizer != null) {
            if (this.mLastSurfaceSize.x != 0 || this.mLastSurfaceSize.y != 0) {
                t.setWindowCrop(this.mSurfaceControl, 0, 0);
                this.mLastSurfaceSize.set(0, 0);
            }
        }
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.SurfaceAnimator.Animatable
    public void onAnimationLeashLost(SurfaceControl.Transaction t) {
        super.onAnimationLeashLost(t);
        if (this.mTaskFragmentOrganizer != null) {
            updateOrganizedTaskFragmentSurfaceSize(t, true);
        }
    }

    private boolean shouldStartChangeTransition(Rect startBounds) {
        if (this.mTaskFragmentOrganizer == null || !canStartChangeTransition()) {
            return false;
        }
        Rect endBounds = getConfiguration().windowConfiguration.getBounds();
        if (isEmbedding() || startBounds.width() != endBounds.height() || startBounds.height() != endBounds.width()) {
            return (endBounds.width() == startBounds.width() && endBounds.height() == startBounds.height()) ? false : true;
        }
        Slog.d(TAG, "shouldStartChangeTransition() return false!");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void setSurfaceControl(SurfaceControl sc) {
        super.setSurfaceControl(sc);
        if (this.mTaskFragmentOrganizer != null) {
            SurfaceControl.Transaction t = getSyncTransaction();
            updateSurfacePosition(t);
            updateOrganizedTaskFragmentSurfaceSize(t, false);
            sendTaskFragmentAppeared();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendTaskFragmentInfoChanged() {
        ITaskFragmentOrganizer iTaskFragmentOrganizer = this.mTaskFragmentOrganizer;
        if (iTaskFragmentOrganizer != null) {
            this.mTaskFragmentOrganizerController.onTaskFragmentInfoChanged(iTaskFragmentOrganizer, this);
        }
    }

    private void sendTaskFragmentAppeared() {
        ITaskFragmentOrganizer iTaskFragmentOrganizer = this.mTaskFragmentOrganizer;
        if (iTaskFragmentOrganizer != null) {
            this.mTaskFragmentOrganizerController.onTaskFragmentAppeared(iTaskFragmentOrganizer, this);
        }
    }

    private void sendTaskFragmentVanished() {
        ITaskFragmentOrganizer iTaskFragmentOrganizer = this.mTaskFragmentOrganizer;
        if (iTaskFragmentOrganizer != null) {
            this.mTaskFragmentOrganizerController.onTaskFragmentVanished(iTaskFragmentOrganizer, this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskFragmentInfo getTaskFragmentInfo() {
        List<IBinder> childActivities = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            WindowContainer<?> wc = getChildAt(i);
            ActivityRecord ar = wc.asActivityRecord();
            if (this.mTaskFragmentOrganizerUid != -1 && ar != null && ar.info.processName.equals(this.mTaskFragmentOrganizerProcessName) && ar.getUid() == this.mTaskFragmentOrganizerUid && !ar.finishing) {
                childActivities.add(ar.token);
            }
        }
        Point positionInParent = new Point();
        getRelativePosition(positionInParent);
        return new TaskFragmentInfo(this.mFragmentToken, this.mRemoteToken.toWindowContainerToken(), getConfiguration(), getNonFinishingActivityCount(), isVisibleRequested(), childActivities, positionInParent, this.mClearedTaskForReuse, this.mClearedTaskFragmentForPip, calculateMinDimension());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Point calculateMinDimension() {
        final int[] maxMinWidth = new int[1];
        final int[] maxMinHeight = new int[1];
        forAllActivities(new Consumer() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TaskFragment.lambda$calculateMinDimension$11(maxMinWidth, maxMinHeight, (ActivityRecord) obj);
            }
        });
        return new Point(maxMinWidth[0], maxMinHeight[0]);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$calculateMinDimension$11(int[] maxMinWidth, int[] maxMinHeight, ActivityRecord a) {
        Point minDimensions;
        if (a.finishing || (minDimensions = a.getMinDimensions()) == null) {
            return;
        }
        maxMinWidth[0] = Math.max(maxMinWidth[0], minDimensions.x);
        maxMinHeight[0] = Math.max(maxMinHeight[0], minDimensions.y);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBinder getFragmentToken() {
        return this.mFragmentToken;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ITaskFragmentOrganizer getTaskFragmentOrganizer() {
        return this.mTaskFragmentOrganizer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean isOrganized() {
        return this.mTaskFragmentOrganizer != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final boolean isOrganizedTaskFragment() {
        return this.mTaskFragmentOrganizer != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTaskVisibleRequested() {
        Task task = getTask();
        return task != null && task.isVisibleRequested();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isReadyToTransit() {
        if (isOrganizedTaskFragment() && getTopNonFinishingActivity() == null && !this.mIsRemovalRequested && !isEmbeddedTaskFragmentInPip()) {
            return this.mClearedTaskFragmentForPip && !isTaskVisibleRequested();
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearLastPausedActivity() {
        forAllTaskFragments(new Consumer() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda9
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((TaskFragment) obj).mLastPausedActivity = null;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMinDimensions(int minWidth, int minHeight) {
        if (asTask() != null) {
            throw new UnsupportedOperationException("This method must not be used to Task. The  minimum dimension of Task should be passed from Task constructor.");
        }
        this.mMinWidth = minWidth;
        this.mMinHeight = minHeight;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEmbeddedTaskFragmentInPip() {
        return isOrganizedTaskFragment() && getTask() != null && getTask().inPinnedWindowingMode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldRemoveSelfOnLastChildRemoval() {
        return !this.mCreatedByOrganizer || this.mIsRemovalRequested;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void removeChild(WindowContainer child) {
        removeChild(child, true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeChild(WindowContainer child, boolean removeSelfIfPossible) {
        ActivityRecord r;
        super.removeChild(child);
        if (BackNavigationController.isScreenshotEnabled() && (r = child.asActivityRecord()) != null) {
            this.mBackScreenshots.remove(r.mActivityComponent.flattenToString());
        }
        if (removeSelfIfPossible && shouldRemoveSelfOnLastChildRemoval() && !hasChild()) {
            removeImmediately("removeLastChild " + child);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void remove(boolean withTransition, String reason) {
        if (!hasChild()) {
            removeImmediately(reason);
            return;
        }
        this.mIsRemovalRequested = true;
        ArrayList<ActivityRecord> removingActivities = new ArrayList<>();
        Objects.requireNonNull(removingActivities);
        forAllActivities(new Task$$ExternalSyntheticLambda46(removingActivities));
        for (int i = removingActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = removingActivities.get(i);
            if (withTransition && r.isVisible()) {
                r.finishIfPossible(reason, false);
            } else {
                r.destroyIfPossible(reason);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDelayLastActivityRemoval(boolean delay) {
        if (!this.mIsEmbedded) {
            Slog.w(TAG, "Set delaying last activity removal on a non-embedded TF.");
        }
        this.mDelayLastActivityRemoval = delay;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDelayLastActivityRemoval() {
        return this.mDelayLastActivityRemoval;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldDeferRemoval() {
        if (hasChild()) {
            return isExitAnimationRunningSelfOrChild() || inTransition();
        }
        return false;
    }

    @Override // com.android.server.wm.WindowContainer
    boolean handleCompleteDeferredRemoval() {
        if (shouldDeferRemoval()) {
            return true;
        }
        return super.handleCompleteDeferredRemoval();
    }

    void removeImmediately(String reason) {
        Slog.d(TAG, "Remove task fragment: " + reason);
        removeImmediately();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void removeImmediately() {
        boolean shouldExecuteAppTransition = false;
        this.mIsRemovalRequested = false;
        resetAdjacentTaskFragment();
        cleanUp();
        if (this.mClearedTaskFragmentForPip && isTaskVisibleRequested()) {
            shouldExecuteAppTransition = true;
        }
        super.removeImmediately();
        sendTaskFragmentVanished();
        if (shouldExecuteAppTransition && this.mDisplayContent != null) {
            this.mAtmService.addWindowLayoutReasons(2);
            this.mDisplayContent.executeAppTransition();
        }
    }

    private void cleanUp() {
        if (this.mIsEmbedded) {
            this.mAtmService.mWindowOrganizerController.cleanUpEmbeddedTaskFragment(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public Dimmer getDimmer() {
        if (asTask() == null && !hasChildOSFullDialog()) {
            return this.mDimmer;
        }
        return super.getDimmer();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void prepareSurfaces() {
        if (asTask() != null) {
            super.prepareSurfaces();
            return;
        }
        this.mDimmer.resetDimStates();
        super.prepareSurfaces();
        Rect dimBounds = getBounds();
        dimBounds.offsetTo(0, 0);
        if (this.mDimmer.updateDims(getSyncTransaction(), dimBounds)) {
            scheduleAnimation();
        }
    }

    @Override // com.android.server.wm.WindowContainer
    boolean canBeAnimationTarget() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public boolean fillsParent() {
        return getWindowingMode() == 1 || matchParentBounds();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dump(final String prefix, FileDescriptor fd, final PrintWriter pw, final boolean dumpAll, boolean dumpClient, final String dumpPackage, final boolean needSep, final Runnable header) {
        boolean printed = false;
        Runnable headerPrinter = new Runnable() { // from class: com.android.server.wm.TaskFragment$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                TaskFragment.this.m8337lambda$dump$13$comandroidserverwmTaskFragment(needSep, pw, header, prefix, dumpAll, dumpPackage);
            }
        };
        if (dumpPackage == null) {
            headerPrinter.run();
            headerPrinter = null;
            printed = true;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer child = (WindowContainer) this.mChildren.get(i);
            if (child.asTaskFragment() != null) {
                printed = child.asTaskFragment().dump(prefix + "  ", fd, pw, dumpAll, dumpClient, dumpPackage, needSep, headerPrinter) | printed;
            } else if (child.asActivityRecord() != null) {
                ActivityRecord.dumpActivity(fd, pw, i, child.asActivityRecord(), prefix + "  ", "Hist ", true, !dumpAll, dumpClient, dumpPackage, false, headerPrinter, getTask());
            }
        }
        return printed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dump$13$com-android-server-wm-TaskFragment  reason: not valid java name */
    public /* synthetic */ void m8337lambda$dump$13$comandroidserverwmTaskFragment(boolean needSep, PrintWriter pw, Runnable header, String prefix, boolean dumpAll, String dumpPackage) {
        if (needSep) {
            pw.println();
        }
        if (header != null) {
            header.run();
        }
        dumpInner(prefix, pw, dumpAll, dumpPackage);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpInner(String prefix, PrintWriter pw, boolean dumpAll, String dumpPackage) {
        pw.print(prefix);
        pw.print("* ");
        pw.println(this);
        Rect bounds = getRequestedOverrideBounds();
        if (!bounds.isEmpty()) {
            pw.println(prefix + "  mBounds=" + bounds);
        }
        if (this.mIsRemovalRequested) {
            pw.println(prefix + "  mIsRemovalRequested=true");
        }
        if (dumpAll) {
            ActivityTaskSupervisor.printThisActivity(pw, this.mLastPausedActivity, dumpPackage, false, prefix + "  mLastPausedActivity: ", null);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.WindowContainer
    public void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        super.dump(pw, prefix, dumpAll);
        pw.println(prefix + "bounds=" + getBounds().toShortString());
        String doublePrefix = prefix + "  ";
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer<?> child = this.mChildren.get(i);
            pw.println(prefix + "* " + child);
            if (child.asActivityRecord() == null) {
                child.dump(pw, doublePrefix, dumpAll);
            }
        }
    }

    @Override // com.android.server.wm.WindowContainer
    void writeIdentifierToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, System.identityHashCode(this));
        ActivityRecord topActivity = topRunningActivity();
        proto.write(1120986464258L, topActivity != null ? topActivity.mUserId : -10000);
        proto.write(1138166333443L, topActivity != null ? topActivity.intent.getComponent().flattenToShortString() : "TaskFragment");
        proto.end(token);
    }

    @Override // com.android.server.wm.WindowContainer
    long getProtoFieldId() {
        return 1146756268041L;
    }

    @Override // com.android.server.wm.WindowContainer, com.android.server.wm.ConfigurationContainer
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        if (logLevel == 2 && !isVisible()) {
            return;
        }
        long token = proto.start(fieldId);
        super.dumpDebug(proto, 1146756268033L, logLevel);
        proto.write(1120986464258L, getDisplayId());
        proto.write(1120986464259L, getActivityType());
        proto.write(1120986464260L, this.mMinWidth);
        proto.write(1120986464261L, this.mMinHeight);
        proto.end(token);
    }

    private void debugGriffinBeforeResumeTopActivity(ActivityRecord next) {
        if (ITranGriffinFeature.Instance().isGriffinSupport() && ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
            if (next == null) {
                Slog.d("Griffin/AppResumed", "Activity: null");
                return;
            }
            String thisPkg = next.info.packageName;
            String thisCls = next.info.name;
            String thisProc = next.info.processName;
            int thisPid = next.app != null ? next.app.getPid() : -2;
            int thisUid = next.info.applicationInfo.uid;
            Slog.d("Griffin/AppResumed", "Activity:" + thisCls + ", pkg=" + thisPkg + ",uid=" + thisUid + ",pid=" + thisPid + ",proc=" + thisProc);
        }
    }

    private void debugGriffinWhenResumeTopActivity(ActivityRecord lastResumed, ActivityRecord next, boolean pausing) {
        if (ITranGriffinFeature.Instance().isGriffinSupport() && ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
            ActivityRecord last = lastResumed != null ? lastResumed : this.mResumedActivity;
            String lastResumedPkg = "null";
            String lastResumedCls = "null";
            String nextPkg = "null";
            String nextCls = "null";
            int nextActivityType = 0;
            if (last != null && last.info != null) {
                lastResumedPkg = last.packageName;
                lastResumedCls = last.info.name;
            }
            if (next != null && next.info != null) {
                nextPkg = next.packageName;
                nextCls = next.info.name;
                nextActivityType = next.getActivityType();
            }
            Slog.d("TranGriffin/AppSwitch", "curt:" + lastResumedPkg + SliceClientPermissions.SliceAuthority.DELIMITER + lastResumedCls + ", next:" + nextPkg + SliceClientPermissions.SliceAuthority.DELIMITER + nextCls + " pausing=" + pausing);
            if (lastResumedPkg.equals(nextPkg) && lastResumedCls.equals(nextCls)) {
                Slog.d("TranGriffin/AppSwitch", "Before app switch from:" + lastResumedCls + " to " + nextCls + ", pausing=" + pausing + ",type=" + nextActivityType + ",(same activity)");
                return;
            }
            Slog.d("TranGriffin/AppSwitch", "Before app switch from:" + lastResumedCls + ",pkg=" + lastResumedPkg + " to " + nextCls + ",pkg=" + nextPkg + ", pausing=" + pausing + ",type=" + nextActivityType);
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

    @Override // com.android.server.wm.WindowContainer
    boolean hasChildOSFullDialog() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((WindowContainer) this.mChildren.get(i)).asActivityRecord() != null && ((WindowContainer) this.mChildren.get(i)).hasChildOSFullDialog()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEmbedding() {
        if (asTask() == null && this.mIsEmbedded && matchParentBounds() && !WindowConfiguration.inMultiWindowMode(getResolvedOverrideConfiguration().windowConfiguration.getWindowingMode())) {
            return false;
        }
        return isEmbedded();
    }

    float getMinAspectRatioForUser() {
        return -1.0f;
    }

    boolean isPkgInActivityEmbedding() {
        return false;
    }
}
