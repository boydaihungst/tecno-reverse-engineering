package com.android.server.wm;

import android.app.ActivityManagerInternal;
import android.app.ActivityOptions;
import android.app.AppOpsManager;
import android.app.AppOpsManagerInternal;
import android.app.ProfilerInfo;
import android.app.ResultInfo;
import android.app.ThunderbackConfig;
import android.app.WaitResult;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.LaunchActivityItem;
import android.app.servertransaction.PauseActivityItem;
import android.app.servertransaction.ResumeActivityItem;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.SensorPrivacyManagerInternal;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.util.ArrayMap;
import android.util.MergedConfiguration;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Display;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.pooled.PooledConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.am.HostingRecord;
import com.android.server.am.TranAmHooker;
import com.android.server.am.UserState;
import com.android.server.utils.Slogf;
import com.android.server.wm.ActivityMetricsLogger;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.RecentTasks;
import com.transsion.hubcore.griffin.ITranGriffinFeature;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.multiwindow.ITranMultiWindow;
import com.transsion.hubcore.multiwindow.ITranMultiWindowManager;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import com.transsion.hubcore.server.wm.ITranActivityStarter;
import com.transsion.hubcore.server.wm.ITranActivityTaskSupervisor;
import com.transsion.hubcore.server.wm.ITranWindowManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class ActivityTaskSupervisor implements RecentTasks.Callbacks {
    private static final ArrayMap<String, String> ACTION_TO_RUNTIME_PERMISSION;
    private static final int ACTIVITY_RESTRICTION_APPOP = 2;
    private static final int ACTIVITY_RESTRICTION_NONE = 0;
    private static final int ACTIVITY_RESTRICTION_PERMISSION = 1;
    static final boolean DEFER_RESUME = true;
    private static final int IDLE_NOW_MSG = 201;
    private static final int IDLE_TIMEOUT_MSG = 200;
    private static final int LAUNCH_TASK_BEHIND_COMPLETE = 212;
    private static final int LAUNCH_TIMEOUT_MSG = 204;
    private static final int MAX_TASK_IDS_PER_USER = 100000;
    static final boolean ON_TOP = true;
    static final boolean PRESERVE_WINDOWS = true;
    private static final int PROCESS_STOPPING_AND_FINISHING_MSG = 205;
    static final boolean REMOVE_FROM_RECENTS = true;
    private static final int REPORT_MULTI_WINDOW_MODE_CHANGED_MSG = 214;
    private static final int REPORT_PIP_MODE_CHANGED_MSG = 215;
    private static final int RESTART_ACTIVITY_PROCESS_TIMEOUT_MSG = 213;
    private static final int RESUME_TOP_ACTIVITY_MSG = 202;
    private static final int SLEEP_TIMEOUT_MSG = 203;
    private static final int START_HOME_MSG = 216;
    private static final int TOP_RESUMED_STATE_LOSS_TIMEOUT = 500;
    private static final int TOP_RESUMED_STATE_LOSS_TIMEOUT_MSG = 217;
    private static final boolean VALIDATE_WAKE_LOCK_CALLER = false;
    private ActivityMetricsLogger mActivityMetricsLogger;
    private AppOpsManager mAppOpsManager;
    boolean mAppVisibilitiesChangedSinceLastPause;
    private int mDeferResumeCount;
    private boolean mDeferRootVisibilityUpdate;
    private boolean mDockedRootTaskResizing;
    PowerManager.WakeLock mGoingToSleepWakeLock;
    private final ActivityTaskSupervisorHandler mHandler;
    private boolean mInitialized;
    private KeyguardController mKeyguardController;
    private LaunchParamsController mLaunchParamsController;
    LaunchParamsPersister mLaunchParamsPersister;
    PowerManager.WakeLock mLaunchingActivityWakeLock;
    final Looper mLooper;
    PersisterQueue mPersisterQueue;
    private Rect mPipModeChangedTargetRootTaskBounds;
    private PowerManager mPowerManager;
    RecentTasks mRecentTasks;
    RootWindowContainer mRootWindowContainer;
    private RunningTasks mRunningTasks;
    final ActivityTaskManagerService mService;
    private ComponentName mSystemChooserActivity;
    private ActivityRecord mTopResumedActivity;
    private boolean mTopResumedActivityWaitingForPrev;
    private int mVisibilityTransactionDepth;
    private WindowManagerService mWindowManager;
    private static final String TAG = "ActivityTaskManager";
    private static final String TAG_IDLE = TAG + ActivityTaskManagerDebugConfig.POSTFIX_IDLE;
    private static final String TAG_PAUSE = TAG + ActivityTaskManagerDebugConfig.POSTFIX_PAUSE;
    private static final String TAG_RECENTS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_RECENTS;
    private static final String TAG_ROOT_TASK = TAG + ActivityTaskManagerDebugConfig.POSTFIX_ROOT_TASK;
    private static final String TAG_SWITCH = TAG + ActivityTaskManagerDebugConfig.POSTFIX_SWITCH;
    static final String TAG_TASKS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_TASKS;
    private static final int IDLE_TIMEOUT = Build.HW_TIMEOUT_MULTIPLIER * 10000;
    private static final int SLEEP_TIMEOUT = Build.HW_TIMEOUT_MULTIPLIER * 5000;
    private static final int LAUNCH_TIMEOUT = Build.HW_TIMEOUT_MULTIPLIER * 10000;
    private final ArrayList<WindowProcessController> mActivityStateChangedProcs = new ArrayList<>();
    private final SparseIntArray mCurTaskIdForUser = new SparseIntArray(20);
    private final ArrayList<WaitInfo> mWaitingActivityLaunched = new ArrayList<>();
    final ArrayList<ActivityRecord> mStoppingActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mFinishingActivities = new ArrayList<>();
    private Object mReparentFromRecentLock = new Object();
    final ArrayList<ActivityRecord> mNoHistoryActivities = new ArrayList<>();
    private final ArrayList<ActivityRecord> mMultiWindowModeChangedActivities = new ArrayList<>();
    private final ArrayList<ActivityRecord> mPipModeChangedActivities = new ArrayList<>();
    final ArrayList<ActivityRecord> mNoAnimActivities = new ArrayList<>();
    final ArrayList<UserState> mStartingUsers = new ArrayList<>();
    boolean mUserLeaving = false;

    static {
        ArrayMap<String, String> arrayMap = new ArrayMap<>();
        ACTION_TO_RUNTIME_PERMISSION = arrayMap;
        arrayMap.put("android.media.action.IMAGE_CAPTURE", "android.permission.CAMERA");
        arrayMap.put("android.media.action.VIDEO_CAPTURE", "android.permission.CAMERA");
        arrayMap.put("android.intent.action.CALL", "android.permission.CALL_PHONE");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canPlaceEntityOnDisplay(int displayId, int callingPid, int callingUid, ActivityInfo activityInfo) {
        return canPlaceEntityOnDisplay(displayId, callingPid, callingUid, null, activityInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canPlaceEntityOnDisplay(int displayId, int callingPid, int callingUid, Task task) {
        return canPlaceEntityOnDisplay(displayId, callingPid, callingUid, task, null);
    }

    private boolean canPlaceEntityOnDisplay(int displayId, int callingPid, int callingUid, Task task, ActivityInfo activityInfo) {
        if (displayId == 0) {
            return true;
        }
        if (this.mService.mSupportsMultiDisplay && isCallerAllowedToLaunchOnDisplay(callingPid, callingUid, displayId, activityInfo)) {
            DisplayContent displayContent = this.mRootWindowContainer.getDisplayContentOrCreate(displayId);
            if (displayContent != null && displayContent.mDwpcHelper.hasController()) {
                final ArrayList<ActivityInfo> activities = new ArrayList<>();
                if (activityInfo != null) {
                    activities.add(activityInfo);
                }
                if (task != null) {
                    task.forAllActivities(new Consumer() { // from class: com.android.server.wm.ActivityTaskSupervisor$$ExternalSyntheticLambda1
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            activities.add(((ActivityRecord) obj).info);
                        }
                    });
                }
                if (!displayContent.mDwpcHelper.canContainActivities(activities, displayContent.getWindowingMode())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public ActivityTaskSupervisor(ActivityTaskManagerService service, Looper looper) {
        this.mService = service;
        this.mLooper = looper;
        this.mHandler = new ActivityTaskSupervisorHandler(looper);
        ITranActivityTaskSupervisor.Instance().onConstruct(service);
    }

    public void initialize() {
        if (this.mInitialized) {
            return;
        }
        this.mInitialized = true;
        setRunningTasks(new RunningTasks());
        this.mActivityMetricsLogger = new ActivityMetricsLogger(this, this.mHandler.getLooper());
        this.mKeyguardController = new KeyguardController(this.mService, this);
        PersisterQueue persisterQueue = new PersisterQueue();
        this.mPersisterQueue = persisterQueue;
        LaunchParamsPersister launchParamsPersister = new LaunchParamsPersister(persisterQueue, this);
        this.mLaunchParamsPersister = launchParamsPersister;
        LaunchParamsController launchParamsController = new LaunchParamsController(this.mService, launchParamsPersister);
        this.mLaunchParamsController = launchParamsController;
        launchParamsController.registerDefaultModifiers(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        this.mLaunchParamsPersister.onSystemReady();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserUnlocked(int userId) {
        this.mPersisterQueue.startPersisting();
        this.mLaunchParamsPersister.onUnlockUser(userId);
        scheduleStartHome("userUnlocked");
    }

    public ActivityMetricsLogger getActivityMetricsLogger() {
        return this.mActivityMetricsLogger;
    }

    public KeyguardController getKeyguardController() {
        return this.mKeyguardController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getSystemChooserActivity() {
        if (this.mSystemChooserActivity == null) {
            this.mSystemChooserActivity = ComponentName.unflattenFromString(this.mService.mContext.getResources().getString(17039902));
        }
        return this.mSystemChooserActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRecentTasks(RecentTasks recentTasks) {
        RecentTasks recentTasks2 = this.mRecentTasks;
        if (recentTasks2 != null) {
            recentTasks2.unregisterCallback(this);
        }
        this.mRecentTasks = recentTasks;
        recentTasks.registerCallback(this);
    }

    void setRunningTasks(RunningTasks runningTasks) {
        this.mRunningTasks = runningTasks;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunningTasks getRunningTasks() {
        return this.mRunningTasks;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initPowerManagement() {
        PowerManager powerManager = (PowerManager) this.mService.mContext.getSystemService(PowerManager.class);
        this.mPowerManager = powerManager;
        this.mGoingToSleepWakeLock = powerManager.newWakeLock(1, "ActivityManager-Sleep");
        PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(1, "*launch*");
        this.mLaunchingActivityWakeLock = newWakeLock;
        newWakeLock.setReferenceCounted(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowManager(WindowManagerService wm) {
        this.mWindowManager = wm;
        getKeyguardController().setWindowManager(wm);
    }

    void moveRecentsRootTaskToFront(String reason) {
        Task recentsRootTask = this.mRootWindowContainer.getDefaultTaskDisplayArea().getRootTask(0, 3);
        if (recentsRootTask != null) {
            recentsRootTask.moveToFront(reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setNextTaskIdForUser(int taskId, int userId) {
        int currentTaskId = this.mCurTaskIdForUser.get(userId, -1);
        if (taskId > currentTaskId) {
            this.mCurTaskIdForUser.put(userId, taskId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishNoHistoryActivitiesIfNeeded(ActivityRecord next) {
        for (int i = this.mNoHistoryActivities.size() - 1; i >= 0; i--) {
            ActivityRecord noHistoryActivity = this.mNoHistoryActivities.get(i);
            if (!noHistoryActivity.finishing && noHistoryActivity != next && next.occludesParent() && noHistoryActivity.getDisplayId() == next.getDisplayId()) {
                if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                    String protoLogParam0 = String.valueOf(noHistoryActivity);
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_STATES, -484194149, 0, (String) null, new Object[]{protoLogParam0});
                }
                noHistoryActivity.finishIfPossible("resume-no-history", false);
                this.mNoHistoryActivities.remove(noHistoryActivity);
            }
        }
    }

    private static int nextTaskIdForUser(int taskId, int userId) {
        int nextTaskId = taskId + 1;
        if (nextTaskId == (userId + 1) * MAX_TASK_IDS_PER_USER) {
            return nextTaskId - MAX_TASK_IDS_PER_USER;
        }
        return nextTaskId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getNextTaskIdForUser() {
        return getNextTaskIdForUser(this.mRootWindowContainer.mCurrentUser);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getNextTaskIdForUser(int userId) {
        int currentTaskId = this.mCurTaskIdForUser.get(userId, MAX_TASK_IDS_PER_USER * userId);
        int candidateTaskId = nextTaskIdForUser(currentTaskId, userId);
        do {
            if (this.mRecentTasks.containsTaskId(candidateTaskId, userId) || this.mRootWindowContainer.anyTaskForId(candidateTaskId, 1) != null) {
                candidateTaskId = nextTaskIdForUser(candidateTaskId, userId);
            } else {
                this.mCurTaskIdForUser.put(userId, candidateTaskId);
                return candidateTaskId;
            }
        } while (candidateTaskId != currentTaskId);
        throw new IllegalStateException("Cannot get an available task id. Reached limit of 100000 running tasks per user.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void waitActivityVisibleOrLaunched(WaitResult w, ActivityRecord r, ActivityMetricsLogger.LaunchingState launchingState) {
        if (w.result != 2 && w.result != 0) {
            return;
        }
        WaitInfo waitInfo = new WaitInfo(w, r.mActivityComponent, launchingState);
        this.mWaitingActivityLaunched.add(waitInfo);
        do {
            try {
                this.mService.mGlobalLock.wait();
            } catch (InterruptedException e) {
            }
        } while (this.mWaitingActivityLaunched.contains(waitInfo));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanupActivity(ActivityRecord r) {
        this.mFinishingActivities.remove(r);
        stopWaitingForActivityVisible(r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopWaitingForActivityVisible(ActivityRecord r) {
        reportActivityLaunched(false, r, -1L, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportActivityLaunched(boolean timeout, ActivityRecord r, long totalTime, int launchState) {
        boolean changed = false;
        for (int i = this.mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
            WaitInfo info = this.mWaitingActivityLaunched.get(i);
            if (info.matches(r)) {
                WaitResult w = info.mResult;
                w.timeout = timeout;
                w.who = r.mActivityComponent;
                w.totalTime = totalTime;
                w.launchState = launchState;
                this.mWaitingActivityLaunched.remove(i);
                changed = true;
            }
        }
        if (changed) {
            this.mService.mGlobalLock.notifyAll();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportWaitingActivityLaunchedIfNeeded(ActivityRecord r, int result) {
        if (this.mWaitingActivityLaunched.isEmpty()) {
            return;
        }
        if (result != 3 && result != 2) {
            return;
        }
        boolean changed = false;
        for (int i = this.mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
            WaitInfo info = this.mWaitingActivityLaunched.get(i);
            if (info.matches(r)) {
                WaitResult w = info.mResult;
                w.result = result;
                if (result == 3) {
                    w.who = r.mActivityComponent;
                    this.mWaitingActivityLaunched.remove(i);
                    changed = true;
                }
            }
        }
        if (changed) {
            this.mService.mGlobalLock.notifyAll();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityInfo resolveActivity(Intent intent, ResolveInfo rInfo, final int startFlags, final ProfilerInfo profilerInfo) {
        final ActivityInfo aInfo = rInfo != null ? rInfo.activityInfo : null;
        if (aInfo != null) {
            intent.setComponent(new ComponentName(aInfo.applicationInfo.packageName, aInfo.name));
            boolean requestDebug = (startFlags & 14) != 0;
            boolean requestProfile = profilerInfo != null;
            if (requestDebug || requestProfile) {
                boolean debuggable = (Build.IS_DEBUGGABLE || (aInfo.applicationInfo.flags & 2) != 0) && !aInfo.processName.equals(HostingRecord.HOSTING_TYPE_SYSTEM);
                if ((requestDebug && !debuggable) || (requestProfile && !debuggable && !aInfo.applicationInfo.isProfileableByShell())) {
                    Slog.w(TAG, "Ignore debugging for non-debuggable app: " + aInfo.packageName);
                } else {
                    synchronized (this.mService.mGlobalLock) {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            this.mService.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskSupervisor$$ExternalSyntheticLambda6
                                @Override // java.lang.Runnable
                                public final void run() {
                                    ActivityTaskSupervisor.this.m7849xda9afdcc(aInfo, startFlags, profilerInfo);
                                }
                            });
                            try {
                                this.mService.mGlobalLock.wait();
                            } catch (InterruptedException e) {
                            }
                        } catch (Throwable th) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
            String intentLaunchToken = intent.getLaunchToken();
            if (aInfo.launchToken == null && intentLaunchToken != null) {
                aInfo.launchToken = intentLaunchToken;
            }
        }
        return aInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$resolveActivity$1$com-android-server-wm-ActivityTaskSupervisor  reason: not valid java name */
    public /* synthetic */ void m7849xda9afdcc(ActivityInfo aInfo, int startFlags, ProfilerInfo profilerInfo) {
        try {
            this.mService.mAmInternal.setDebugFlagsForStartingActivity(aInfo, startFlags, profilerInfo, this.mService.mGlobalLock);
        } finally {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:17:0x003c A[Catch: all -> 0x006c, TryCatch #2 {all -> 0x006c, blocks: (B:3:0x0002, B:5:0x0014, B:10:0x0023, B:12:0x002a, B:14:0x0032, B:15:0x0034, B:17:0x003c, B:19:0x0041, B:9:0x001f), top: B:36:0x0002 }] */
    /* JADX WARN: Removed duplicated region for block: B:18:0x0040  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId, int flags, int filterCallingUid) {
        int modifiedFlags;
        int privateResolveFlags;
        int privateResolveFlags2;
        try {
            Trace.traceBegin(32L, "resolveIntent");
            int modifiedFlags2 = flags | 65536 | 1024;
            try {
                if (!intent.isWebIntent() && (intent.getFlags() & 2048) == 0) {
                    modifiedFlags = modifiedFlags2;
                    privateResolveFlags = 0;
                    if (intent.isWebIntent() && (intent.getFlags() & 1024) != 0) {
                        privateResolveFlags = 0 | 1;
                    }
                    if ((intent.getFlags() & 512) != 0) {
                        privateResolveFlags2 = privateResolveFlags;
                    } else {
                        privateResolveFlags2 = privateResolveFlags | 2;
                    }
                    long token = Binder.clearCallingIdentity();
                    ResolveInfo resolveIntent = this.mService.getPackageManagerInternalLocked().resolveIntent(intent, resolvedType, modifiedFlags, privateResolveFlags2, userId, true, filterCallingUid);
                    Binder.restoreCallingIdentity(token);
                    Trace.traceEnd(32L);
                    return resolveIntent;
                }
                ResolveInfo resolveIntent2 = this.mService.getPackageManagerInternalLocked().resolveIntent(intent, resolvedType, modifiedFlags, privateResolveFlags2, userId, true, filterCallingUid);
                Binder.restoreCallingIdentity(token);
                Trace.traceEnd(32L);
                return resolveIntent2;
            } catch (Throwable th) {
                th = th;
                Trace.traceEnd(32L);
                throw th;
            }
            modifiedFlags = modifiedFlags2 | 8388608;
            privateResolveFlags = 0;
            if (intent.isWebIntent()) {
                privateResolveFlags = 0 | 1;
            }
            if ((intent.getFlags() & 512) != 0) {
            }
            long token2 = Binder.clearCallingIdentity();
        } catch (Throwable th2) {
            th = th2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityInfo resolveActivity(Intent intent, String resolvedType, int startFlags, ProfilerInfo profilerInfo, int userId, int filterCallingUid) {
        ResolveInfo rInfo = resolveIntent(intent, resolvedType, userId, 0, filterCallingUid);
        return resolveActivity(intent, rInfo, startFlags, profilerInfo);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [999=6, 1016=8] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:161:0x0437  */
    /* JADX WARN: Removed duplicated region for block: B:170:0x0456 A[Catch: all -> 0x0495, TRY_LEAVE, TryCatch #10 {all -> 0x0495, blocks: (B:168:0x0452, B:170:0x0456, B:174:0x048e, B:175:0x0494, B:162:0x0440, B:163:0x0445), top: B:193:0x0440 }] */
    /* JADX WARN: Removed duplicated region for block: B:173:0x048d  */
    /* JADX WARN: Removed duplicated region for block: B:184:0x0059 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0080 A[Catch: all -> 0x0063, TRY_ENTER, TRY_LEAVE, TryCatch #2 {all -> 0x0063, blocks: (B:19:0x0059, B:25:0x0071, B:27:0x0077, B:30:0x0080, B:37:0x0091, B:47:0x0104, B:51:0x0124, B:53:0x0129, B:55:0x012e, B:62:0x0141, B:69:0x0159, B:72:0x01ae, B:74:0x01b8, B:75:0x01c0, B:78:0x0213), top: B:184:0x0059 }] */
    /* JADX WARN: Removed duplicated region for block: B:32:0x0087  */
    /* JADX WARN: Removed duplicated region for block: B:42:0x00e5  */
    /* JADX WARN: Removed duplicated region for block: B:43:0x00e8 A[Catch: all -> 0x0497, TryCatch #13 {all -> 0x0497, blocks: (B:17:0x0054, B:23:0x0069, B:28:0x007a, B:34:0x0089, B:40:0x00df, B:44:0x00ee, B:48:0x0118, B:58:0x0137, B:67:0x0155, B:70:0x019b, B:76:0x01c9, B:80:0x021f, B:82:0x0239, B:84:0x0244, B:86:0x0254, B:57:0x0134, B:43:0x00e8, B:39:0x0099), top: B:202:0x0054 }] */
    /* JADX WARN: Removed duplicated region for block: B:47:0x0104 A[Catch: all -> 0x0063, TRY_ENTER, TRY_LEAVE, TryCatch #2 {all -> 0x0063, blocks: (B:19:0x0059, B:25:0x0071, B:27:0x0077, B:30:0x0080, B:37:0x0091, B:47:0x0104, B:51:0x0124, B:53:0x0129, B:55:0x012e, B:62:0x0141, B:69:0x0159, B:72:0x01ae, B:74:0x01b8, B:75:0x01c0, B:78:0x0213), top: B:184:0x0059 }] */
    /* JADX WARN: Removed duplicated region for block: B:60:0x013d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean realStartActivityLocked(ActivityRecord r, WindowProcessController proc, boolean andResume, boolean checkConfig) throws RemoteException {
        boolean andResume2;
        int applicationInfoUid;
        LockTaskController lockTaskController;
        String str;
        List<ReferrerIntent> newIntents;
        PauseActivityItem obtain;
        Task rootTask;
        if (!this.mRootWindowContainer.allPausedActivitiesComplete()) {
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(r);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -641258376, 0, (String) null, new Object[]{protoLogParam0});
            }
            return false;
        }
        Task task = r.getTask();
        Task rootTask2 = task.getRootTask();
        beginDeferResume();
        proc.pauseConfigurationDispatch();
        try {
            r.startFreezingScreenLocked(proc, 0);
            r.startLaunchTickingLocked();
            r.setProcess(proc);
            try {
                try {
                    if (andResume) {
                        try {
                            if (!r.canResumeByCompat()) {
                                andResume2 = false;
                                r.notifyUnknownVisibilityLaunchedForKeyguardTransition();
                                if (checkConfig) {
                                    try {
                                        this.mRootWindowContainer.ensureVisibilityAndConfig(r, r.getDisplayId(), false, true);
                                    } catch (Throwable th) {
                                        e = th;
                                        endDeferResume();
                                        proc.resumeConfigurationDispatch();
                                        throw e;
                                    }
                                }
                                if (this.mKeyguardController.checkKeyguardVisibility(r) && r.allowMoveToFront()) {
                                    r.setVisibility(true);
                                }
                                applicationInfoUid = r.info.applicationInfo == null ? r.info.applicationInfo.uid : -1;
                                if (r.mUserId == proc.mUserId || r.info.applicationInfo.uid != applicationInfoUid) {
                                    Slog.wtf(TAG, "User ID for activity changing for " + r + " appInfo.uid=" + r.info.applicationInfo.uid + " info.ai.uid=" + applicationInfoUid + " old=" + r.app + " new=" + proc);
                                }
                                ActivityClientController activityClientController = !proc.hasEverLaunchedActivity() ? null : this.mService.mActivityClientController;
                                r.launchCount++;
                                r.lastLaunchTime = SystemClock.uptimeMillis();
                                proc.setLastActivityLaunchTime(r.lastLaunchTime);
                                if (ActivityTaskManagerDebugConfig.DEBUG_ALL) {
                                    Slog.v(TAG, "Launching: " + r);
                                }
                                lockTaskController = this.mService.getLockTaskController();
                                if (task.mLockTaskAuth != 2 || task.mLockTaskAuth == 4 || (task.mLockTaskAuth == 3 && lockTaskController.getLockTaskModeState() == 1)) {
                                    lockTaskController.startLockTaskMode(task, false, 0);
                                }
                                if (proc.hasThread()) {
                                    str = "2nd-crash";
                                    try {
                                        try {
                                            throw new RemoteException();
                                        } catch (RemoteException e) {
                                            e = e;
                                            if (r.launchFailed) {
                                            }
                                        }
                                    } catch (Throwable th2) {
                                        e = th2;
                                        endDeferResume();
                                        proc.resumeConfigurationDispatch();
                                        throw e;
                                    }
                                } else {
                                    List<ResultInfo> results = null;
                                    if (andResume2) {
                                        try {
                                            results = r.results;
                                            newIntents = r.newIntents;
                                        } catch (RemoteException e2) {
                                            e = e2;
                                            str = "2nd-crash";
                                            if (r.launchFailed) {
                                                r.launchFailed = true;
                                                r.detachFromProcess();
                                                throw e;
                                            }
                                            Slog.e(TAG, "Second failure launching " + r.intent.getComponent().flattenToShortString() + ", giving up", e);
                                            String str2 = str;
                                            proc.appDied(str2);
                                            r.finishIfPossible(str2, false);
                                            endDeferResume();
                                            proc.resumeConfigurationDispatch();
                                            return false;
                                        }
                                    } else {
                                        newIntents = null;
                                    }
                                    if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
                                        Slog.v(TAG_SWITCH, "Launching: " + r + " savedState=" + r.getSavedState() + " with results=" + results + " newIntents=" + newIntents + " andResume=" + andResume2);
                                    }
                                    EventLogTags.writeWmRestartActivity(r.mUserId, System.identityHashCode(r), task.mTaskId, r.shortComponentName);
                                    if (r.isActivityTypeHome()) {
                                        if (ITranGriffinFeature.Instance().isGriffinSupport()) {
                                            updateHomeProcess(task.getBottomMostActivity());
                                        } else {
                                            updateHomeProcess(task.getBottomMostActivity().app);
                                        }
                                    }
                                    this.mService.getPackageManagerInternalLocked().notifyPackageUse(r.intent.getComponent().getPackageName(), 0);
                                    r.forceNewConfig = false;
                                    this.mService.getAppWarningsLocked().onStartActivity(r);
                                    r.compat = this.mService.compatibilityInfoForPackageLocked(r.info.applicationInfo);
                                    Configuration procConfig = proc.prepareConfigurationForLaunchingActivity();
                                    MergedConfiguration mergedConfiguration = new MergedConfiguration(procConfig, r.getMergedOverrideConfiguration());
                                    r.setLastReportedConfiguration(mergedConfiguration);
                                    logIfTransactionTooLarge(r.intent, r.getSavedState());
                                    if (r.isEmbedded()) {
                                        this.mService.mTaskFragmentOrganizerController.dispatchPendingInfoChangedEvent(r.getOrganizedTaskFragment());
                                    }
                                    ClientTransaction clientTransaction = ClientTransaction.obtain(proc.getThread(), r.token);
                                    boolean isTransitionForward = r.isTransitionForward();
                                    IBinder fragmentToken = r.getTaskFragment().getFragmentToken();
                                    try {
                                        str = "2nd-crash";
                                        try {
                                            try {
                                                try {
                                                    try {
                                                        try {
                                                            try {
                                                                clientTransaction.addCallback(LaunchActivityItem.obtain(new Intent(r.intent), System.identityHashCode(r), r.info, mergedConfiguration.getGlobalConfiguration(), mergedConfiguration.getOverrideConfiguration(), r.compat, r.getFilteredReferrer(r.launchedFromPackage), task.voiceInteractor, proc.getReportedProcState(), r.getSavedState(), r.getPersistentSavedState(), results, newIntents, r.takeOptions(), isTransitionForward, proc.createProfilerInfoIfNeeded(), r.assistToken, activityClientController, r.shareableActivityToken, r.getLaunchedFromBubble(), fragmentToken));
                                                                if (andResume2) {
                                                                    try {
                                                                        obtain = ResumeActivityItem.obtain(isTransitionForward);
                                                                    } catch (RemoteException e3) {
                                                                        e = e3;
                                                                        if (r.launchFailed) {
                                                                        }
                                                                    } catch (Throwable th3) {
                                                                        e = th3;
                                                                        endDeferResume();
                                                                        proc.resumeConfigurationDispatch();
                                                                        throw e;
                                                                    }
                                                                } else {
                                                                    obtain = PauseActivityItem.obtain();
                                                                }
                                                                clientTransaction.setLifecycleStateRequest(obtain);
                                                                this.mService.getLifecycleManager().scheduleTransaction(clientTransaction);
                                                                ITranWindowManagerService.Instance().onRealStartActivityLocked(r, this.mWindowManager.mAtmService.mKeyguardController.isKeyguardShowing(0));
                                                                if (procConfig.seq > this.mRootWindowContainer.getConfiguration().seq) {
                                                                    proc.setLastReportedConfiguration(procConfig);
                                                                }
                                                                if ((proc.mInfo.privateFlags & 2) != 0 && this.mService.mHasHeavyWeightFeature && proc.mName.equals(proc.mInfo.packageName)) {
                                                                    if (this.mService.mHeavyWeightProcess != null && this.mService.mHeavyWeightProcess != proc) {
                                                                        Slog.w(TAG, "Starting new heavy weight process " + proc + " when already running " + this.mService.mHeavyWeightProcess);
                                                                    }
                                                                    this.mService.setHeavyWeightProcess(r);
                                                                }
                                                                endDeferResume();
                                                                proc.resumeConfigurationDispatch();
                                                                r.launchFailed = false;
                                                                if (andResume2 && readyToResume()) {
                                                                    rootTask = rootTask2;
                                                                    rootTask.minimalResumeActivityLocked(r);
                                                                    if (ITranActivityTaskSupervisor.Instance().isGameBoosterEnable() && r != null && proc.isAgaresProcess() && TranAmHooker.isTargetGameApp(r.packageName)) {
                                                                        int thisUid = r.info.applicationInfo.uid;
                                                                        ITranActivityTaskSupervisor.Instance().boostPreloadStart(thisUid, r.packageName);
                                                                        Slog.d("GameBooster_FW", "boost Preload Start,this UID : " + thisUid + " , this package : " + r.packageName);
                                                                        this.mService.mAmsExt.onStartProcess("preloaded-activity", r.packageName);
                                                                        Slog.d("GameBooster_FW", "this app is Preloaded " + r.packageName + "booster this app!");
                                                                    }
                                                                    if (proc.isAgaresProcess()) {
                                                                        proc.setAgaresProcess(false);
                                                                    }
                                                                } else {
                                                                    rootTask = rootTask2;
                                                                    if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                                                                        String protoLogParam02 = String.valueOf(r);
                                                                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1468740466, 0, (String) null, new Object[]{protoLogParam02});
                                                                    }
                                                                    r.setState(ActivityRecord.State.PAUSED, "realStartActivityLocked");
                                                                    this.mRootWindowContainer.executeAppTransitionForAllDisplay();
                                                                }
                                                                proc.onStartActivity(this.mService.mTopProcessState, r.info);
                                                                if (this.mRootWindowContainer.isTopDisplayFocusedRootTask(rootTask)) {
                                                                    this.mService.getActivityStartController().startSetupActivity();
                                                                }
                                                                if (r.app != null) {
                                                                    r.app.updateServiceConnectionActivities();
                                                                    return true;
                                                                }
                                                                return true;
                                                            } catch (RemoteException e4) {
                                                                e = e4;
                                                            }
                                                        } catch (Throwable th4) {
                                                            e = th4;
                                                        }
                                                    } catch (RemoteException e5) {
                                                        e = e5;
                                                    }
                                                } catch (RemoteException e6) {
                                                    e = e6;
                                                } catch (Throwable th5) {
                                                    e = th5;
                                                }
                                            } catch (RemoteException e7) {
                                                e = e7;
                                            }
                                        } catch (RemoteException e8) {
                                            e = e8;
                                            if (r.launchFailed) {
                                            }
                                        }
                                    } catch (RemoteException e9) {
                                        e = e9;
                                        str = "2nd-crash";
                                    }
                                }
                            }
                        } catch (Throwable th6) {
                            e = th6;
                            endDeferResume();
                            proc.resumeConfigurationDispatch();
                            throw e;
                        }
                    }
                    if (proc.hasThread()) {
                    }
                } catch (RemoteException e10) {
                    e = e10;
                    str = "2nd-crash";
                }
                r.notifyUnknownVisibilityLaunchedForKeyguardTransition();
                if (checkConfig) {
                }
                if (this.mKeyguardController.checkKeyguardVisibility(r)) {
                    r.setVisibility(true);
                }
                applicationInfoUid = r.info.applicationInfo == null ? r.info.applicationInfo.uid : -1;
                if (r.mUserId == proc.mUserId) {
                }
                Slog.wtf(TAG, "User ID for activity changing for " + r + " appInfo.uid=" + r.info.applicationInfo.uid + " info.ai.uid=" + applicationInfoUid + " old=" + r.app + " new=" + proc);
                if (!proc.hasEverLaunchedActivity()) {
                }
                r.launchCount++;
                r.lastLaunchTime = SystemClock.uptimeMillis();
                proc.setLastActivityLaunchTime(r.lastLaunchTime);
                if (ActivityTaskManagerDebugConfig.DEBUG_ALL) {
                }
                lockTaskController = this.mService.getLockTaskController();
                if (task.mLockTaskAuth != 2) {
                }
                lockTaskController.startLockTaskMode(task, false, 0);
            } catch (Throwable th7) {
                e = th7;
            }
            andResume2 = andResume;
        } catch (Throwable th8) {
            e = th8;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateHomeProcess(ActivityRecord r) {
        WindowProcessController app;
        if (r != null && (app = r.app) != null && this.mService.mHomeProcess != app) {
            updateHomeProcess(app);
            if (ITranGriffinFeature.Instance().isGriffinSupport() && ITranGriffinFeature.Instance().isGriffinDebugOpen()) {
                String homePkg = app.mInfo.packageName;
                String homeCls = r.info.name;
                int pid = app.getPid();
                int uid = app.mInfo.uid;
                String homeProcName = app.getName();
                Slog.d("TranGriffin/AppSwitch", "Home changed to " + homePkg + ",cls:" + homeCls + ",pid=" + pid + ",uid=" + uid + ",proc=" + homeProcName);
            }
            ITranActivityTaskSupervisor.Instance().updateHomeProcess(r.info, app.getProcessWrapper());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateHomeProcess(WindowProcessController app) {
        if (app != null && this.mService.mHomeProcess != app) {
            scheduleStartHome("homeChanged");
            this.mService.mHomeProcess = app;
        }
    }

    private void scheduleStartHome(String reason) {
        if (!this.mHandler.hasMessages(START_HOME_MSG)) {
            this.mHandler.obtainMessage(START_HOME_MSG, reason).sendToTarget();
        }
    }

    private void logIfTransactionTooLarge(Intent intent, Bundle icicle) {
        Bundle extras;
        int extrasSize = 0;
        if (intent != null && (extras = intent.getExtras()) != null) {
            extrasSize = extras.getSize();
        }
        int icicleSize = icicle == null ? 0 : icicle.getSize();
        if (extrasSize + icicleSize > 200000) {
            Slog.e(TAG, "Transaction too large, intent: " + intent + ", extras size: " + extrasSize + ", icicle size: " + icicleSize);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startSpecificActivity(ActivityRecord r, boolean andResume, boolean checkConfig) {
        WindowProcessController wpc = this.mService.getProcessController(r.processName, r.info.applicationInfo.uid);
        boolean knownToBeDead = false;
        if (wpc != null && wpc.hasThread()) {
            try {
                realStartActivityLocked(r, wpc, andResume, checkConfig);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception when starting activity " + r.intent.getComponent().flattenToShortString(), e);
                knownToBeDead = true;
                this.mService.mProcessNames.remove(wpc.mName, wpc.mUid);
                this.mService.mProcessMap.remove(wpc.getPid());
            }
        }
        r.notifyUnknownVisibilityLaunchedForKeyguardTransition();
        boolean isTop = andResume && r.isTopRunningActivity();
        this.mService.startProcessAsync(r, knownToBeDead, isTop, isTop ? HostingRecord.HOSTING_TYPE_TOP_ACTIVITY : HostingRecord.HOSTING_TYPE_ACTIVITY);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkStartAnyActivityPermission(Intent intent, ActivityInfo aInfo, String resultWho, int requestCode, int callingPid, int callingUid, String callingPackage, String callingFeatureId, boolean ignoreTargetSecurity, boolean launchingInTask, WindowProcessController callerApp, ActivityRecord resultRecord, Task resultRootTask) {
        String msg;
        boolean isCallerRecents = this.mService.getRecentTasks() != null && this.mService.getRecentTasks().isCallerRecents(callingUid);
        int startAnyPerm = ActivityTaskManagerService.checkPermission("android.permission.START_ANY_ACTIVITY", callingPid, callingUid);
        if (startAnyPerm != 0) {
            if (isCallerRecents && launchingInTask) {
                return true;
            }
            int componentRestriction = getComponentRestrictionForCallingPackage(aInfo, callingPackage, callingFeatureId, callingPid, callingUid, ignoreTargetSecurity);
            int actionRestriction = getActionRestrictionForCallingPackage(intent.getAction(), callingPackage, callingFeatureId, callingPid, callingUid);
            if (componentRestriction == 1 || actionRestriction == 1) {
                if (resultRecord != null) {
                    resultRecord.sendResult(-1, resultWho, requestCode, 0, null, null);
                }
                if (actionRestriction == 1) {
                    msg = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with revoked permission " + ACTION_TO_RUNTIME_PERMISSION.get(intent.getAction());
                } else if (aInfo.exported) {
                    msg = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") requires " + aInfo.permission;
                } else {
                    msg = "Permission Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") not exported from uid " + aInfo.applicationInfo.uid;
                }
                Slog.w(TAG, msg);
                throw new SecurityException(msg);
            } else if (actionRestriction == 2) {
                String message = "Appop Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") requires " + AppOpsManager.permissionToOp(ACTION_TO_RUNTIME_PERMISSION.get(intent.getAction()));
                Slog.w(TAG, message);
                return false;
            } else if (componentRestriction == 2) {
                String message2 = "Appop Denial: starting " + intent.toString() + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") requires appop " + AppOpsManager.permissionToOp(aInfo.permission);
                Slog.w(TAG, message2);
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCallerAllowedToLaunchOnTaskDisplayArea(int callingPid, int callingUid, TaskDisplayArea taskDisplayArea, ActivityInfo aInfo) {
        return isCallerAllowedToLaunchOnDisplay(callingPid, callingUid, taskDisplayArea != null ? taskDisplayArea.getDisplayId() : 0, aInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCallerAllowedToLaunchOnDisplay(int callingPid, int callingUid, int launchDisplayId, ActivityInfo aInfo) {
        if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
            long protoLogParam0 = launchDisplayId;
            long protoLogParam1 = callingPid;
            long protoLogParam2 = callingUid;
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1228653755, 21, (String) null, new Object[]{Long.valueOf(protoLogParam0), Long.valueOf(protoLogParam1), Long.valueOf(protoLogParam2)});
        }
        if (callingPid == -1 && callingUid == -1) {
            if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 1524174282, 0, (String) null, (Object[]) null);
            }
            return true;
        }
        DisplayContent displayContent = this.mRootWindowContainer.getDisplayContentOrCreate(launchDisplayId);
        if (displayContent != null && !displayContent.isRemoved()) {
            int startAnyPerm = ActivityTaskManagerService.checkPermission("android.permission.INTERNAL_SYSTEM_WINDOW", callingPid, callingUid);
            if (startAnyPerm == 0) {
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 431715812, 0, (String) null, (Object[]) null);
                }
                return true;
            }
            boolean uidPresentOnDisplay = displayContent.isUidPresent(callingUid);
            Display display = displayContent.mDisplay;
            if (!display.isTrusted()) {
                if ((aInfo.flags & Integer.MIN_VALUE) != 0) {
                    if (ActivityTaskManagerService.checkPermission("android.permission.ACTIVITY_EMBEDDING", callingPid, callingUid) == -1 && !uidPresentOnDisplay) {
                        if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, 979347997, 0, (String) null, (Object[]) null);
                        }
                        return false;
                    }
                } else {
                    if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1474602871, 0, (String) null, (Object[]) null);
                    }
                    return false;
                }
            }
            if (!displayContent.isPrivate()) {
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1750384749, 0, (String) null, (Object[]) null);
                }
                return true;
            } else if (display.getOwnerUid() == callingUid) {
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -856750101, 0, (String) null, (Object[]) null);
                }
                return true;
            } else if (!uidPresentOnDisplay) {
                Slog.w(TAG, "Launch on display check: denied");
                return false;
            } else {
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_TASKS, -1979455254, 0, (String) null, (Object[]) null);
                }
                return true;
            }
        }
        Slog.w(TAG, "Launch on display check: display not found");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserInfo getUserInfo(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            return UserManager.get(this.mService.mContext).getUserInfo(userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private AppOpsManager getAppOpsManager() {
        if (this.mAppOpsManager == null) {
            this.mAppOpsManager = (AppOpsManager) this.mService.mContext.getSystemService(AppOpsManager.class);
        }
        return this.mAppOpsManager;
    }

    private int getComponentRestrictionForCallingPackage(ActivityInfo activityInfo, String callingPackage, String callingFeatureId, int callingPid, int callingUid, boolean ignoreTargetSecurity) {
        int opCode;
        if (ignoreTargetSecurity || ActivityTaskManagerService.checkComponentPermission(activityInfo.permission, callingPid, callingUid, activityInfo.applicationInfo.uid, activityInfo.exported) != -1) {
            return (activityInfo.permission == null || (opCode = AppOpsManager.permissionToOpCode(activityInfo.permission)) == -1 || getAppOpsManager().noteOpNoThrow(opCode, callingUid, callingPackage, callingFeatureId, "") == 0 || ignoreTargetSecurity) ? 0 : 2;
        }
        return 1;
    }

    private int getActionRestrictionForCallingPackage(String action, String callingPackage, String callingFeatureId, int callingPid, int callingUid) {
        String permission;
        if (action == null || (permission = ACTION_TO_RUNTIME_PERMISSION.get(action)) == null) {
            return 0;
        }
        try {
            PackageInfo packageInfo = this.mService.mContext.getPackageManager().getPackageInfoAsUser(callingPackage, 4096, UserHandle.getUserId(callingUid));
            if (ArrayUtils.contains(packageInfo.requestedPermissions, permission)) {
                if (ActivityTaskManagerService.checkPermission(permission, callingPid, callingUid) == -1) {
                    return 1;
                }
                int opCode = AppOpsManager.permissionToOpCode(permission);
                if (opCode == -1 || getAppOpsManager().noteOpNoThrow(opCode, callingUid, callingPackage, callingFeatureId, "") == 0) {
                    return 0;
                }
                if ("android.permission.CAMERA".equals(permission)) {
                    SensorPrivacyManagerInternal spmi = (SensorPrivacyManagerInternal) LocalServices.getService(SensorPrivacyManagerInternal.class);
                    UserHandle user = UserHandle.getUserHandleForUid(callingUid);
                    boolean cameraPrivacyEnabled = spmi.isSensorPrivacyEnabled(user.getIdentifier(), 2);
                    if (cameraPrivacyEnabled) {
                        AppOpsManagerInternal aomi = (AppOpsManagerInternal) LocalServices.getService(AppOpsManagerInternal.class);
                        int numCameraRestrictions = aomi.getOpRestrictionCount(26, user, callingPackage, (String) null);
                        return numCameraRestrictions == 1 ? 0 : 2;
                    }
                    return 2;
                }
                return 2;
            }
            return 0;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.i(TAG, "Cannot find package info for " + callingPackage);
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLaunchSource(int uid) {
        this.mLaunchingActivityWakeLock.setWorkSource(new WorkSource(uid));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void acquireLaunchWakelock() {
        this.mLaunchingActivityWakeLock.acquire();
        if (!this.mHandler.hasMessages(204)) {
            this.mHandler.sendEmptyMessageDelayed(204, LAUNCH_TIMEOUT);
        }
    }

    private void checkFinishBootingLocked() {
        boolean booting = this.mService.isBooting();
        boolean enableScreen = false;
        this.mService.setBooting(false);
        if (!this.mService.isBooted()) {
            this.mService.setBooted(true);
            enableScreen = true;
        }
        if (booting || enableScreen) {
            this.mService.postFinishBooting(booting, enableScreen);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void activityIdleInternal(ActivityRecord r, boolean fromTimeout, boolean processPausingActivities, Configuration config) {
        if (ActivityTaskManagerDebugConfig.DEBUG_ALL) {
            Slog.v(TAG, "Activity idle: " + r);
        }
        if (r != null) {
            if (ActivityTaskManagerDebugConfig.DEBUG_IDLE) {
                Slog.d(TAG_IDLE, "activityIdleInternal: Callers=" + Debug.getCallers(4));
            }
            this.mHandler.removeMessages(200, r);
            r.finishLaunchTickingLocked();
            if (fromTimeout) {
                reportActivityLaunched(fromTimeout, r, -1L, -1);
            }
            if (config != null) {
                r.setLastReportedGlobalConfiguration(config);
            }
            r.idle = true;
            if ((this.mService.isBooting() && this.mRootWindowContainer.allResumedActivitiesIdle()) || fromTimeout) {
                checkFinishBootingLocked();
            }
            r.mRelaunchReason = 0;
        }
        if (this.mRootWindowContainer.allResumedActivitiesIdle()) {
            if (r != null) {
                this.mService.scheduleAppGcsLocked();
                this.mRecentTasks.onActivityIdle(r);
            }
            if (this.mLaunchingActivityWakeLock.isHeld()) {
                this.mHandler.removeMessages(204);
                this.mLaunchingActivityWakeLock.release();
            }
            this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
        }
        processStoppingAndFinishingActivities(r, processPausingActivities, "idle");
        if (ActivityTaskManagerDebugConfig.DEBUG_IDLE) {
            Slogf.i(TAG, "activityIdleInternal(): r=%s, mStartingUsers=%s", r, this.mStartingUsers);
        }
        if (!this.mStartingUsers.isEmpty()) {
            ArrayList<UserState> startingUsers = new ArrayList<>(this.mStartingUsers);
            this.mStartingUsers.clear();
            for (int i = 0; i < startingUsers.size(); i++) {
                UserState userState = startingUsers.get(i);
                Slogf.i(TAG, "finishing switch of user %d", Integer.valueOf(userState.mHandle.getIdentifier()));
                this.mService.mAmInternal.finishUserSwitch(userState);
            }
        }
        this.mService.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskSupervisor$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskSupervisor.this.m7847x117a8fac();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$activityIdleInternal$2$com-android-server-wm-ActivityTaskSupervisor  reason: not valid java name */
    public /* synthetic */ void m7847x117a8fac() {
        this.mService.mAmInternal.trimApplications();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1582=5] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public void findTaskToMoveToFront(Task task, int flags, ActivityOptions options, String reason, boolean forceNonResizeable) {
        String str;
        boolean z;
        Rect bounds;
        Task currentRootTask = task.getRootTask();
        if (currentRootTask == null) {
            Slog.e(TAG, "findTaskToMoveToFront: can't move task=" + task + " to front. Root task is null");
            return;
        }
        if ((flags & 2) == 0) {
            try {
                this.mUserLeaving = true;
            } catch (Throwable th) {
                th = th;
                z = false;
                this.mUserLeaving = z;
                throw th;
            }
        }
        try {
            task.mTransitionController.requestTransitionIfNeeded(3, 0, task, task, options != null ? options.getRemoteTransition() : null, null);
            str = reason;
            try {
                String reason2 = str + " findTaskToMoveToFront";
                boolean reparented = false;
                try {
                    if (task.isResizeable()) {
                        try {
                            if (canUseActivityOptionsLaunchBounds(options)) {
                                Rect bounds2 = options.getLaunchBounds();
                                task.setBounds(bounds2);
                                Task targetRootTask = this.mRootWindowContainer.getOrCreateRootTask(null, options, task, true);
                                if (targetRootTask != currentRootTask) {
                                    moveHomeRootTaskToFrontIfNeeded(flags, targetRootTask.getDisplayArea(), reason2);
                                    bounds = bounds2;
                                    task.reparent(targetRootTask, true, 1, false, true, reason2);
                                    currentRootTask = targetRootTask;
                                    reparented = true;
                                } else {
                                    bounds = bounds2;
                                }
                                if (targetRootTask.shouldResizeRootTaskWithLaunchBounds()) {
                                    targetRootTask.resize(bounds, false, false);
                                } else {
                                    task.resize(false, false);
                                }
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            z = false;
                            this.mUserLeaving = z;
                            throw th;
                        }
                    }
                    Task currentRootTask2 = currentRootTask;
                    if (!reparented) {
                        try {
                            moveHomeRootTaskToFrontIfNeeded(flags, currentRootTask2.getDisplayArea(), reason2);
                        } catch (Throwable th3) {
                            th = th3;
                            z = false;
                            this.mUserLeaving = z;
                            throw th;
                        }
                    }
                    try {
                        ActivityRecord r = task.getTopNonFinishingActivity();
                        z = false;
                        try {
                            currentRootTask2.moveTaskToFront(task, false, options, r == null ? null : r.appTimeTracker, reason2);
                            ITranWindowManagerService.Instance().onMoveTaskToFront(r);
                            ITranWindowManagerService.Instance().onMoveTaskToFront(r, task.getDisplayId());
                            if (ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK) {
                                Slog.d(TAG_ROOT_TASK, "findTaskToMoveToFront: moved to front of root task=" + currentRootTask2);
                            }
                            handleNonResizableTaskIfNeeded(task, 0, this.mRootWindowContainer.getDefaultTaskDisplayArea(), currentRootTask2, forceNonResizeable);
                            this.mUserLeaving = false;
                        } catch (Throwable th4) {
                            th = th4;
                            this.mUserLeaving = z;
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        z = false;
                    }
                } catch (Throwable th6) {
                    th = th6;
                    z = false;
                }
            } catch (Throwable th7) {
                th = th7;
                z = false;
                this.mUserLeaving = z;
                throw th;
            }
        } catch (Throwable th8) {
            th = th8;
            str = reason;
        }
    }

    private void moveHomeRootTaskToFrontIfNeeded(int flags, TaskDisplayArea taskDisplayArea, String reason) {
        Task focusedRootTask = taskDisplayArea.getFocusedRootTask();
        if ((taskDisplayArea.getWindowingMode() == 1 && (flags & 1) != 0) || (focusedRootTask != null && focusedRootTask.isActivityTypeRecents())) {
            taskDisplayArea.moveHomeRootTaskToFront(reason);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canUseActivityOptionsLaunchBounds(ActivityOptions options) {
        if (options == null || options.getLaunchBounds() == null) {
            return false;
        }
        return (this.mService.mSupportsPictureInPicture && options.getLaunchWindowingMode() == 2) || this.mService.mSupportsFreeformWindowManagement;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LaunchParamsController getLaunchParamsController() {
        return this.mLaunchParamsController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSplitScreenResizing(boolean resizing) {
        if (resizing == this.mDockedRootTaskResizing) {
            return;
        }
        this.mDockedRootTaskResizing = resizing;
        this.mWindowManager.setDockedRootTaskResizing(resizing);
    }

    private void removePinnedRootTaskInSurfaceTransaction(Task rootTask) {
        rootTask.cancelAnimation();
        rootTask.setForceHidden(1, true);
        rootTask.ensureActivitiesVisible(null, 0, true);
        activityIdleInternal(null, false, true, null);
        DisplayContent toDisplay = this.mRootWindowContainer.getDisplayContent(0);
        this.mService.deferWindowLayout();
        try {
            rootTask.setWindowingMode(0);
            if (rootTask.getWindowingMode() != 5) {
                rootTask.setBounds(null);
            }
            toDisplay.getDefaultTaskDisplayArea().positionTaskBehindHome(rootTask);
            rootTask.setForceHidden(1, false);
            this.mRootWindowContainer.ensureActivitiesVisible(null, 0, true);
            this.mRootWindowContainer.resumeFocusedTasksTopActivities();
        } finally {
            this.mService.continueWindowLayout();
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: removeRootTaskInSurfaceTransaction */
    public void m7848x616bed60(Task rootTask) {
        if (rootTask.getWindowingMode() == 2) {
            removePinnedRootTaskInSurfaceTransaction(rootTask);
            return;
        }
        PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.ActivityTaskSupervisor$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ActivityTaskSupervisor) obj).processRemoveTask((Task) obj2);
            }
        }, this, PooledLambda.__(Task.class));
        rootTask.forAllLeafTasks(c, true);
        c.recycle();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processRemoveTask(Task task) {
        removeTask(task, true, true, "remove-root-task");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRootTask(final Task task) {
        this.mWindowManager.inSurfaceTransaction(new Runnable() { // from class: com.android.server.wm.ActivityTaskSupervisor$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskSupervisor.this.m7848x616bed60(task);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeTaskById(int taskId, boolean killProcess, boolean removeFromRecents, String reason) {
        Task task = this.mRootWindowContainer.anyTaskForId(taskId, 1);
        if (task != null) {
            removeTask(task, killProcess, removeFromRecents, reason);
            return true;
        }
        Slog.w(TAG, "Request to remove task ignored for non-existent task " + taskId);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeTask(Task task, boolean killProcess, boolean removeFromRecents, String reason) {
        if (task.mInRemoveTask) {
            return;
        }
        task.mTransitionController.requestCloseTransitionIfNeeded(task);
        task.mInRemoveTask = true;
        boolean killPro = killProcess;
        boolean clear = true;
        try {
            if (TranAmHooker.isKeepAliveSupport() && killPro) {
                ComponentName component = task.getBaseIntent().getComponent();
                ArrayMap<String, SparseArray<WindowProcessController>> pmap = this.mService.mProcessNames.getMap();
                if (component != null) {
                    String pkg = component.getPackageName();
                    killPro = TranWmHooker.ensureIfNeedKill(pkg, pmap, killPro);
                    clear = TranWmHooker.ensureIfNeedClear(pkg, killPro);
                }
                Slog.d(TAG, "Griffin/KeepAlive:removeTask: " + component + ", kill:" + killPro + ", cLear:" + clear);
            }
            if (clear) {
                task.removeActivities(reason, false);
            }
            cleanUpRemovedTaskLocked(task, killPro, removeFromRecents);
            this.mService.getLockTaskController().clearLockedTask(task);
            this.mService.getTaskChangeNotificationController().notifyTaskStackChanged();
            if (task.isPersistable) {
                this.mService.notifyTaskPersisterLocked(null, true);
            }
        } finally {
            task.mInRemoveTask = false;
        }
    }

    void cleanUpRemovedTaskLocked(Task task, boolean killProcess, boolean removeFromRecents) {
        if (removeFromRecents) {
            this.mRecentTasks.remove(task);
        }
        ComponentName component = task.getBaseIntent().getComponent();
        if (component == null) {
            Slog.w(TAG, "No component for base intent of task: " + task);
            return;
        }
        Message msg = PooledLambda.obtainMessage(new QuadConsumer() { // from class: com.android.server.wm.ActivityTaskSupervisor$$ExternalSyntheticLambda4
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                ((ActivityManagerInternal) obj).cleanUpServices(((Integer) obj2).intValue(), (ComponentName) obj3, (Intent) obj4);
            }
        }, this.mService.mAmInternal, Integer.valueOf(task.mUserId), component, new Intent(task.getBaseIntent()));
        this.mService.mH.sendMessage(msg);
        if (!killProcess) {
            ITranActivityTaskSupervisor.Instance().hookCleanUpRemovedTaskNoKill(null, component);
            return;
        }
        String pkg = component.getPackageName();
        ArrayList<Object> procsToKill = new ArrayList<>();
        ArrayMap<String, SparseArray<WindowProcessController>> pmap = this.mService.mProcessNames.getMap();
        ArrayList<TranProcessWrapper> procWrapperToKill = new ArrayList<>();
        for (int i = 0; i < pmap.size(); i++) {
            SparseArray<WindowProcessController> uids = pmap.valueAt(i);
            for (int j = 0; j < uids.size(); j++) {
                WindowProcessController proc = uids.valueAt(j);
                if (proc.mUserId == task.mUserId && proc != this.mService.mHomeProcess && proc.mPkgList.contains(pkg)) {
                    if (!proc.shouldKillProcessForRemovedTask(task) || proc.hasForegroundServices()) {
                        return;
                    }
                    if (TranAmHooker.isAgaresSupport() && TranWmHooker.isAgaresProtectProc(proc)) {
                        Slog.d(TAG, "Agares/KeepAlive: protected this proc:" + proc.mName);
                    } else if (ITranActivityTaskSupervisor.Instance().isGameBoosterEnable() && ITranActivityTaskSupervisor.Instance().isGriffinSupport() && TranWmHooker.isGameBoosterProtectProc(proc)) {
                        Slog.d(TAG, "GameBooster_FW: protected this proc: " + proc.mName);
                    } else {
                        procsToKill.add(proc);
                        ITranActivityTaskSupervisor.Instance().receiveProcInfo(procWrapperToKill, proc);
                    }
                }
            }
        }
        ITranActivityTaskSupervisor.Instance().hookCleanUpRemovedTaskKill(procWrapperToKill, component);
        Message m = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.ActivityTaskSupervisor$$ExternalSyntheticLambda5
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ActivityManagerInternal) obj).killProcessesForRemovedTask((ArrayList) obj2);
            }
        }, this.mService.mAmInternal, procsToKill);
        this.mService.mH.sendMessage(m);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean restoreRecentTaskLocked(Task task, ActivityOptions aOptions, boolean onTop) {
        Task rootTask = this.mRootWindowContainer.getOrCreateRootTask(null, aOptions, task, onTop);
        WindowContainer parent = task.getParent();
        if (parent == rootTask || task == rootTask) {
            return true;
        }
        if (parent != null) {
            task.reparent(rootTask, Integer.MAX_VALUE, true, "restoreRecentTaskLocked");
            return true;
        }
        rootTask.addChild((WindowContainer) task, onTop, true);
        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
            Slog.v(TAG_RECENTS, "Added restored task=" + task + " to root task=" + rootTask);
        }
        return true;
    }

    @Override // com.android.server.wm.RecentTasks.Callbacks
    public void onRecentTaskAdded(Task task) {
        task.touchActiveTime();
    }

    @Override // com.android.server.wm.RecentTasks.Callbacks
    public void onRecentTaskRemoved(Task task, boolean wasTrimmed, boolean killProcess) {
        if (wasTrimmed) {
            removeTaskById(task.mTaskId, killProcess, false, "recent-task-trimmed");
        }
        task.removedFromRecents();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.wm.Task */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public Task getReparentTargetRootTask(Task task, Task rootTask, boolean toTop) {
        Task prevRootTask = task.getRootTask();
        int rootTaskId = rootTask.mTaskId;
        boolean inMultiWindowMode = rootTask.inMultiWindowMode();
        if (prevRootTask != null && prevRootTask.mTaskId == rootTaskId) {
            Slog.w(TAG, "Can not reparent to same root task, task=" + task + " already in rootTaskId=" + rootTaskId);
            return prevRootTask;
        } else if (inMultiWindowMode && !this.mService.mSupportsMultiWindow) {
            throw new IllegalArgumentException("Device doesn't support multi-window, can not reparent task=" + task + " to root-task=" + rootTask);
        } else {
            if (rootTask.getDisplayId() != 0 && !this.mService.mSupportsMultiDisplay) {
                throw new IllegalArgumentException("Device doesn't support multi-display, can not reparent task=" + task + " to rootTaskId=" + rootTaskId);
            }
            if (rootTask.getWindowingMode() == 5 && !this.mService.mSupportsFreeformWindowManagement) {
                throw new IllegalArgumentException("Device doesn't support freeform, can not reparent task=" + task);
            }
            if (rootTask.inPinnedWindowingMode()) {
                throw new IllegalArgumentException("No support to reparent to PIP, task=" + task);
            }
            if (inMultiWindowMode && !task.supportsMultiWindowInDisplayArea(rootTask.getDisplayArea())) {
                Slog.w(TAG, "Can not move unresizeable task=" + task + " to multi-window root task=" + rootTask + " Moving to a fullscreen root task instead.");
                if (prevRootTask != null) {
                    return prevRootTask;
                }
                return rootTask.getDisplayArea().createRootTask(1, rootTask.getActivityType(), toTop);
            }
            return rootTask;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void goingToSleepLocked() {
        scheduleSleepTimeout();
        if (!this.mGoingToSleepWakeLock.isHeld()) {
            this.mGoingToSleepWakeLock.acquire();
            if (this.mLaunchingActivityWakeLock.isHeld()) {
                this.mLaunchingActivityWakeLock.release();
                this.mHandler.removeMessages(204);
            }
        }
        this.mRootWindowContainer.applySleepTokens(false);
        checkReadyForSleepLocked(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shutdownLocked(int timeout) {
        goingToSleepLocked();
        boolean timedout = false;
        long endTime = System.currentTimeMillis() + timeout;
        while (true) {
            if (!this.mRootWindowContainer.putTasksToSleep(true, true)) {
                long timeRemaining = endTime - System.currentTimeMillis();
                if (timeRemaining > 0) {
                    try {
                        this.mService.mGlobalLock.wait(timeRemaining);
                    } catch (InterruptedException e) {
                    }
                } else {
                    Slog.w(TAG, "Activity manager shutdown timed out");
                    timedout = true;
                    break;
                }
            } else {
                break;
            }
        }
        checkReadyForSleepLocked(false);
        return timedout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void comeOutOfSleepIfNeededLocked() {
        removeSleepTimeouts();
        if (this.mGoingToSleepWakeLock.isHeld()) {
            this.mGoingToSleepWakeLock.release();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkReadyForSleepLocked(boolean allowDelay) {
        if (!this.mService.isSleepingOrShuttingDownLocked() || !this.mRootWindowContainer.putTasksToSleep(allowDelay, false)) {
            return;
        }
        this.mService.endLaunchPowerMode(3);
        removeSleepTimeouts();
        if (this.mGoingToSleepWakeLock.isHeld()) {
            this.mGoingToSleepWakeLock.release();
        }
        if (this.mService.mShuttingDown) {
            this.mService.mGlobalLock.notifyAll();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean reportResumedActivityLocked(ActivityRecord r) {
        this.mStoppingActivities.remove(r);
        Task rootTask = r.getRootTask();
        if (rootTask.getDisplayArea().allResumedActivitiesComplete()) {
            this.mRootWindowContainer.ensureActivitiesVisible(null, 0, false);
            this.mRootWindowContainer.executeAppTransitionForAllDisplay();
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLaunchTaskBehindCompleteLocked(ActivityRecord r) {
        Task task = r.getTask();
        Task rootTask = task.getRootTask();
        this.mRecentTasks.add(task);
        this.mService.getTaskChangeNotificationController().notifyTaskStackChanged();
        rootTask.ensureActivitiesVisible(null, 0, false);
        ActivityRecord top = rootTask.getTopNonFinishingActivity();
        if (top != null) {
            top.getTask().touchActiveTime();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleLaunchTaskBehindComplete(IBinder token) {
        this.mHandler.obtainMessage(212, token).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processStoppingAndFinishingActivities(ActivityRecord launchedActivity, boolean processPausingActivities, String reason) {
        ArrayList<ActivityRecord> readyToStopActivities = null;
        for (int i = this.mStoppingActivities.size() - 1; i >= 0; i--) {
            ActivityRecord s = this.mStoppingActivities.get(i);
            boolean animating = s.isAnimating(3, 9) || s.inTransition();
            if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                String protoLogParam0 = String.valueOf(s);
                boolean protoLogParam1 = s.nowVisible;
                boolean protoLogParam2 = animating;
                String protoLogParam3 = String.valueOf(s.finishing);
                ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1740512980, 60, (String) null, new Object[]{protoLogParam0, Boolean.valueOf(protoLogParam1), Boolean.valueOf(protoLogParam2), protoLogParam3});
            }
            if (!animating || this.mService.mShuttingDown) {
                if (!processPausingActivities && s.isState(ActivityRecord.State.PAUSING)) {
                    removeIdleTimeoutForActivity(launchedActivity);
                    scheduleIdleTimeout(launchedActivity);
                } else {
                    if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
                        String protoLogParam02 = String.valueOf(s);
                        ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, -1707370822, 0, (String) null, new Object[]{protoLogParam02});
                    }
                    if (readyToStopActivities == null) {
                        readyToStopActivities = new ArrayList<>();
                    }
                    readyToStopActivities.add(s);
                    this.mStoppingActivities.remove(i);
                }
            }
        }
        int numReadyStops = readyToStopActivities == null ? 0 : readyToStopActivities.size();
        for (int i2 = 0; i2 < numReadyStops; i2++) {
            ActivityRecord r = readyToStopActivities.get(i2);
            if (r.isInHistory()) {
                if (r.finishing) {
                    r.destroyIfPossible(reason);
                } else {
                    r.stopIfPossible();
                }
            }
        }
        int numFinishingActivities = this.mFinishingActivities.size();
        if (numFinishingActivities == 0) {
            return;
        }
        ArrayList<ActivityRecord> finishingActivities = new ArrayList<>(this.mFinishingActivities);
        this.mFinishingActivities.clear();
        for (int i3 = 0; i3 < numFinishingActivities; i3++) {
            ActivityRecord r2 = finishingActivities.get(i3);
            if (r2.isInHistory()) {
                r2.destroyImmediately("finish-" + reason);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeHistoryRecords(WindowProcessController app) {
        removeHistoryRecords(this.mStoppingActivities, app, "mStoppingActivities");
        removeHistoryRecords(this.mFinishingActivities, app, "mFinishingActivities");
        removeHistoryRecords(this.mNoHistoryActivities, app, "mNoHistoryActivities");
    }

    private void removeHistoryRecords(ArrayList<ActivityRecord> list, WindowProcessController app, String listName) {
        int i = list.size();
        if (ActivityTaskManagerDebugConfig.DEBUG_CLEANUP) {
            Slog.v(Task.TAG_CLEANUP, "Removing app " + this + " from list " + listName + " with " + i + " entries");
        }
        while (i > 0) {
            i--;
            ActivityRecord r = list.get(i);
            if (ActivityTaskManagerDebugConfig.DEBUG_CLEANUP) {
                Slog.v(Task.TAG_CLEANUP, "Record #" + i + " " + r);
            }
            if (r.app == app) {
                if (ActivityTaskManagerDebugConfig.DEBUG_CLEANUP) {
                    Slog.v(Task.TAG_CLEANUP, "---> REMOVING this entry!");
                }
                list.remove(i);
                r.removeTimeouts();
            }
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println();
        pw.println("ActivityTaskSupervisor state:");
        this.mRootWindowContainer.dump(pw, prefix, true);
        getKeyguardController().dump(pw, prefix);
        this.mService.getLockTaskController().dump(pw, prefix);
        pw.print(prefix);
        pw.println("mCurTaskIdForUser=" + this.mCurTaskIdForUser);
        pw.println(prefix + "mUserRootTaskInFront=" + this.mRootWindowContainer.mUserRootTaskInFront);
        pw.println(prefix + "mVisibilityTransactionDepth=" + this.mVisibilityTransactionDepth);
        pw.print(prefix);
        pw.print("isHomeRecentsComponent=");
        pw.println(this.mRecentTasks.isRecentsComponentHomeActivity(this.mRootWindowContainer.mCurrentUser));
        if (!this.mWaitingActivityLaunched.isEmpty()) {
            pw.println(prefix + "mWaitingActivityLaunched=");
            for (int i = this.mWaitingActivityLaunched.size() - 1; i >= 0; i--) {
                this.mWaitingActivityLaunched.get(i).dump(pw, prefix + "  ");
            }
        }
        pw.println(prefix + "mNoHistoryActivities=" + this.mNoHistoryActivities);
        pw.println();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean printThisActivity(PrintWriter pw, ActivityRecord activity, String dumpPackage, boolean needSep, String prefix, Runnable header) {
        if (activity != null) {
            if (dumpPackage == null || dumpPackage.equals(activity.packageName)) {
                if (needSep) {
                    pw.println();
                }
                if (header != null) {
                    header.run();
                }
                pw.print(prefix);
                pw.println(activity);
                return true;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean dumpHistoryList(FileDescriptor fd, PrintWriter pw, List<ActivityRecord> list, String prefix, String label, boolean complete, boolean brief, boolean client, String dumpPackage, boolean needNL, Runnable header, Task lastTask) {
        Runnable header2 = header;
        Task lastTask2 = lastTask;
        int i = list.size() - 1;
        boolean needNL2 = needNL;
        while (i >= 0) {
            ActivityRecord r = list.get(i);
            int i2 = i;
            ActivityRecord.dumpActivity(fd, pw, i, r, prefix, label, complete, brief, client, dumpPackage, needNL2, header2, lastTask2);
            lastTask2 = r.getTask();
            header2 = null;
            needNL2 = client && r.attachedToProcess();
            i = i2 - 1;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleIdleTimeout(ActivityRecord next) {
        if (ActivityTaskManagerDebugConfig.DEBUG_IDLE) {
            Slog.d(TAG_IDLE, "scheduleIdleTimeout: Callers=" + Debug.getCallers(4));
        }
        Message msg = this.mHandler.obtainMessage(200, next);
        this.mHandler.sendMessageDelayed(msg, IDLE_TIMEOUT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void scheduleIdle() {
        if (!this.mHandler.hasMessages(201)) {
            if (ActivityTaskManagerDebugConfig.DEBUG_IDLE) {
                Slog.d(TAG_IDLE, "scheduleIdle: Callers=" + Debug.getCallers(4));
            }
            this.mHandler.sendEmptyMessage(201);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTopResumedActivityIfNeeded() {
        ActivityRecord prevTopActivity = this.mTopResumedActivity;
        Task topRootTask = this.mRootWindowContainer.getTopDisplayFocusedRootTask();
        if (topRootTask == null || topRootTask.getTopResumedActivity() == prevTopActivity) {
            if (this.mService.isSleepingLocked()) {
                this.mService.updateTopApp(null);
                return;
            }
            return;
        }
        boolean prevActivityReceivedTopState = (prevTopActivity == null || this.mTopResumedActivityWaitingForPrev) ? false : true;
        if (prevActivityReceivedTopState && prevTopActivity.scheduleTopResumedActivityChanged(false)) {
            scheduleTopResumedStateLossTimeout(prevTopActivity);
            this.mTopResumedActivityWaitingForPrev = true;
        }
        ActivityRecord topResumedActivity = topRootTask.getTopResumedActivity();
        this.mTopResumedActivity = topResumedActivity;
        if (topResumedActivity != null && prevTopActivity != null) {
            if (topResumedActivity.app != null) {
                this.mTopResumedActivity.app.addToPendingTop();
            }
            this.mService.updateOomAdj();
        }
        scheduleTopResumedActivityStateIfNeeded();
        this.mService.updateTopApp(this.mTopResumedActivity);
    }

    private void scheduleTopResumedActivityStateIfNeeded() {
        ActivityRecord activityRecord = this.mTopResumedActivity;
        if (activityRecord != null && !this.mTopResumedActivityWaitingForPrev) {
            activityRecord.scheduleTopResumedActivityChanged(true);
        }
    }

    private void scheduleTopResumedStateLossTimeout(ActivityRecord r) {
        Message msg = this.mHandler.obtainMessage(TOP_RESUMED_STATE_LOSS_TIMEOUT_MSG);
        msg.obj = r;
        r.topResumedStateLossTime = SystemClock.uptimeMillis();
        this.mHandler.sendMessageDelayed(msg, 500L);
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(r);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 74885950, 0, (String) null, new Object[]{protoLogParam0});
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleTopResumedStateReleased(boolean timeout) {
        if (ProtoLogCache.WM_DEBUG_STATES_enabled) {
            String protoLogParam0 = String.valueOf(timeout ? "(due to timeout)" : "(transition complete)");
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_STATES, 708142634, 0, (String) null, new Object[]{protoLogParam0});
        }
        this.mHandler.removeMessages(TOP_RESUMED_STATE_LOSS_TIMEOUT_MSG);
        if (!this.mTopResumedActivityWaitingForPrev) {
            return;
        }
        this.mTopResumedActivityWaitingForPrev = false;
        scheduleTopResumedActivityStateIfNeeded();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeIdleTimeoutForActivity(ActivityRecord r) {
        if (ActivityTaskManagerDebugConfig.DEBUG_IDLE) {
            Slog.d(TAG_IDLE, "removeTimeoutsForActivity: Callers=" + Debug.getCallers(4));
        }
        this.mHandler.removeMessages(200, r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void scheduleResumeTopActivities() {
        if (!this.mHandler.hasMessages(202)) {
            this.mHandler.sendEmptyMessage(202);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleProcessStoppingAndFinishingActivitiesIfNeeded() {
        if (this.mStoppingActivities.isEmpty() && this.mFinishingActivities.isEmpty()) {
            return;
        }
        if (this.mRootWindowContainer.allResumedActivitiesIdle()) {
            scheduleIdle();
        } else if (!this.mHandler.hasMessages(205) && this.mRootWindowContainer.allResumedActivitiesVisible()) {
            this.mHandler.sendEmptyMessage(205);
        }
    }

    void removeSleepTimeouts() {
        this.mHandler.removeMessages(203);
    }

    final void scheduleSleepTimeout() {
        removeSleepTimeouts();
        this.mHandler.sendEmptyMessageDelayed(203, SLEEP_TIMEOUT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRestartTimeouts(ActivityRecord r) {
        this.mHandler.removeMessages(213, r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public final void scheduleRestartTimeout(ActivityRecord r) {
        removeRestartTimeouts(r);
        ActivityTaskSupervisorHandler activityTaskSupervisorHandler = this.mHandler;
        activityTaskSupervisorHandler.sendMessageDelayed(activityTaskSupervisorHandler.obtainMessage(213, r), 2000L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleNonResizableTaskIfNeeded(Task task, int preferredWindowingMode, TaskDisplayArea preferredTaskDisplayArea, Task actualRootTask) {
        handleNonResizableTaskIfNeeded(task, preferredWindowingMode, preferredTaskDisplayArea, actualRootTask, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleNonResizableTaskIfNeeded(Task task, int preferredWindowingMode, TaskDisplayArea preferredTaskDisplayArea, Task actualRootTask, boolean forceNonResizable) {
        boolean isSecondaryDisplayPreferred = (preferredTaskDisplayArea == null || preferredTaskDisplayArea.getDisplayId() == 0) ? false : true;
        if (!task.isActivityTypeStandardOrUndefined()) {
            return;
        }
        if (isSecondaryDisplayPreferred) {
            if (!task.canBeLaunchedOnDisplay(task.getDisplayId())) {
                throw new IllegalStateException("Task resolved to incompatible display");
            }
            DisplayContent preferredDisplay = preferredTaskDisplayArea.mDisplayContent;
            if (preferredDisplay != task.getDisplayContent()) {
                Slog.w(TAG, "Failed to put " + task + " on display " + preferredDisplay.mDisplayId);
                this.mService.getTaskChangeNotificationController().notifyActivityLaunchOnSecondaryDisplayFailed(task.getTaskInfo(), preferredDisplay.mDisplayId);
            } else if (!forceNonResizable) {
                handleForcedResizableTaskIfNeeded(task, 2);
            }
        } else if (!forceNonResizable) {
            handleForcedResizableTaskIfNeeded(task, 1);
        }
    }

    private void handleForcedResizableTaskIfNeeded(Task task, int reason) {
        ActivityRecord topActivity = task.getTopNonFinishingActivity();
        if (topActivity == null || topActivity.noDisplay || !topActivity.canForceResizeNonResizable(task.getWindowingMode())) {
            return;
        }
        this.mService.getTaskChangeNotificationController().notifyActivityForcedResizable(task.mTaskId, reason, topActivity.info.applicationInfo.packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleUpdateMultiWindowMode(Task task) {
        PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.ActivityTaskSupervisor$$ExternalSyntheticLambda3
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ActivityTaskSupervisor) obj).addToMultiWindowModeChangedList((ActivityRecord) obj2);
            }
        }, this, PooledLambda.__(ActivityRecord.class));
        task.forAllActivities((Consumer<ActivityRecord>) c);
        c.recycle();
        if (!this.mHandler.hasMessages(214)) {
            this.mHandler.sendEmptyMessage(214);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addToMultiWindowModeChangedList(ActivityRecord r) {
        if (r.attachedToProcess()) {
            this.mMultiWindowModeChangedActivities.add(r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleUpdatePictureInPictureModeIfNeeded(Task task, Task prevRootTask) {
        Task rootTask = task.getRootTask();
        if (prevRootTask != null) {
            if (prevRootTask != rootTask && !prevRootTask.inPinnedWindowingMode() && !rootTask.inPinnedWindowingMode()) {
                return;
            }
            scheduleUpdatePictureInPictureModeIfNeeded(task, rootTask.getRequestedOverrideBounds());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleUpdatePictureInPictureModeIfNeeded(Task task, Rect targetRootTaskBounds) {
        PooledConsumer c = PooledLambda.obtainConsumer(new BiConsumer() { // from class: com.android.server.wm.ActivityTaskSupervisor$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((ActivityTaskSupervisor) obj).addToPipModeChangedList((ActivityRecord) obj2);
            }
        }, this, PooledLambda.__(ActivityRecord.class));
        task.forAllActivities((Consumer<ActivityRecord>) c);
        c.recycle();
        this.mPipModeChangedTargetRootTaskBounds = targetRootTaskBounds;
        if (!this.mHandler.hasMessages(215)) {
            this.mHandler.sendEmptyMessage(215);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addToPipModeChangedList(ActivityRecord r) {
        if (r.attachedToProcess()) {
            this.mPipModeChangedActivities.add(r);
            this.mMultiWindowModeChangedActivities.remove(r);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void wakeUp(String reason) {
        this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), 2, "android.server.am:TURN_ON:" + reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void beginActivityVisibilityUpdate() {
        if (this.mVisibilityTransactionDepth == 0) {
            getKeyguardController().updateVisibility();
        }
        this.mVisibilityTransactionDepth++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void endActivityVisibilityUpdate() {
        int i = this.mVisibilityTransactionDepth - 1;
        this.mVisibilityTransactionDepth = i;
        if (i == 0) {
            computeProcessActivityStateBatch();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inActivityVisibilityUpdate() {
        return this.mVisibilityTransactionDepth > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeferRootVisibilityUpdate(boolean deferUpdate) {
        this.mDeferRootVisibilityUpdate = deferUpdate;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRootVisibilityUpdateDeferred() {
        return this.mDeferRootVisibilityUpdate;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onProcessActivityStateChanged(WindowProcessController wpc, boolean forceBatch) {
        if (forceBatch || inActivityVisibilityUpdate()) {
            if (!this.mActivityStateChangedProcs.contains(wpc)) {
                this.mActivityStateChangedProcs.add(wpc);
                return;
            }
            return;
        }
        wpc.computeProcessActivityState();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void computeProcessActivityStateBatch() {
        if (this.mActivityStateChangedProcs.isEmpty()) {
            return;
        }
        for (int i = this.mActivityStateChangedProcs.size() - 1; i >= 0; i--) {
            this.mActivityStateChangedProcs.get(i).computeProcessActivityState();
        }
        this.mActivityStateChangedProcs.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void beginDeferResume() {
        this.mDeferResumeCount++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void endDeferResume() {
        this.mDeferResumeCount--;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean readyToResume() {
        return this.mDeferResumeCount == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class ActivityTaskSupervisorHandler extends Handler {
        ActivityTaskSupervisorHandler(Looper looper) {
            super(looper);
        }

        /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            synchronized (ActivityTaskSupervisor.this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (handleMessageInner(msg)) {
                        return;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    switch (msg.what) {
                        case 213:
                            ActivityRecord r = (ActivityRecord) msg.obj;
                            String processName = null;
                            int uid = 0;
                            synchronized (ActivityTaskSupervisor.this.mService.mGlobalLock) {
                                try {
                                    WindowManagerService.boostPriorityForLockedSection();
                                    if (r.attachedToProcess() && r.isState(ActivityRecord.State.RESTARTING_PROCESS)) {
                                        processName = r.app.mName;
                                        uid = r.app.mUid;
                                    }
                                } finally {
                                }
                            }
                            WindowManagerService.resetPriorityAfterLockedSection();
                            if (processName != null) {
                                ActivityTaskSupervisor.this.mService.mAmInternal.killProcess(processName, uid, "restartActivityProcessTimeout");
                                return;
                            }
                            return;
                        default:
                            return;
                    }
                } finally {
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        private void activityIdleFromMessage(ActivityRecord idleActivity, boolean fromTimeout) {
            ActivityTaskSupervisor.this.activityIdleInternal(idleActivity, fromTimeout, fromTimeout, null);
        }

        private boolean handleMessageInner(Message msg) {
            switch (msg.what) {
                case 200:
                    if (ActivityTaskManagerDebugConfig.DEBUG_IDLE) {
                        Slog.d(ActivityTaskSupervisor.TAG_IDLE, "handleMessage: IDLE_TIMEOUT_MSG: r=" + msg.obj);
                    }
                    activityIdleFromMessage((ActivityRecord) msg.obj, true);
                    break;
                case 201:
                    if (ActivityTaskManagerDebugConfig.DEBUG_IDLE) {
                        Slog.d(ActivityTaskSupervisor.TAG_IDLE, "handleMessage: IDLE_NOW_MSG: r=" + msg.obj);
                    }
                    activityIdleFromMessage((ActivityRecord) msg.obj, false);
                    break;
                case 202:
                    ActivityTaskSupervisor.this.mRootWindowContainer.resumeFocusedTasksTopActivities();
                    break;
                case 203:
                    if (ActivityTaskSupervisor.this.mService.isSleepingOrShuttingDownLocked()) {
                        Slog.w(ActivityTaskSupervisor.TAG, "Sleep timeout!  Sleeping now.");
                        ActivityTaskSupervisor.this.checkReadyForSleepLocked(false);
                        break;
                    }
                    break;
                case 204:
                    if (ActivityTaskSupervisor.this.mLaunchingActivityWakeLock.isHeld()) {
                        Slog.w(ActivityTaskSupervisor.TAG, "Launch timeout has expired, giving up wake lock!");
                        ActivityTaskSupervisor.this.mLaunchingActivityWakeLock.release();
                        break;
                    }
                    break;
                case 205:
                    ActivityTaskSupervisor.this.processStoppingAndFinishingActivities(null, false, "transit");
                    break;
                case 206:
                case 207:
                case 208:
                case 209:
                case 210:
                case FrameworkStatsLog.DEVICE_POLICY_EVENT__EVENT_ID__ROLE_HOLDER_UPDATER_UPDATE_RETRY /* 211 */:
                case 213:
                default:
                    return false;
                case 212:
                    ActivityRecord r = ActivityRecord.forTokenLocked((IBinder) msg.obj);
                    if (r != null) {
                        ActivityTaskSupervisor.this.handleLaunchTaskBehindCompleteLocked(r);
                        break;
                    }
                    break;
                case 214:
                    for (int i = ActivityTaskSupervisor.this.mMultiWindowModeChangedActivities.size() - 1; i >= 0; i--) {
                        ((ActivityRecord) ActivityTaskSupervisor.this.mMultiWindowModeChangedActivities.remove(i)).updateMultiWindowMode();
                    }
                    break;
                case 215:
                    for (int i2 = ActivityTaskSupervisor.this.mPipModeChangedActivities.size() - 1; i2 >= 0; i2--) {
                        ((ActivityRecord) ActivityTaskSupervisor.this.mPipModeChangedActivities.remove(i2)).updatePictureInPictureMode(ActivityTaskSupervisor.this.mPipModeChangedTargetRootTaskBounds, false);
                    }
                    break;
                case ActivityTaskSupervisor.START_HOME_MSG /* 216 */:
                    ActivityTaskSupervisor.this.mHandler.removeMessages(ActivityTaskSupervisor.START_HOME_MSG);
                    ActivityTaskSupervisor.this.mRootWindowContainer.startHomeOnEmptyDisplays((String) msg.obj);
                    break;
                case ActivityTaskSupervisor.TOP_RESUMED_STATE_LOSS_TIMEOUT_MSG /* 217 */:
                    ActivityRecord r2 = (ActivityRecord) msg.obj;
                    Slog.w(ActivityTaskSupervisor.TAG, "Activity top resumed state loss timeout for " + r2);
                    if (r2.hasProcess()) {
                        ActivityTaskSupervisor.this.mService.logAppTooSlow(r2.app, r2.topResumedStateLossTime, "top state loss for " + r2);
                    }
                    ActivityTaskSupervisor.this.handleTopResumedStateReleased(true);
                    break;
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int startActivityFromRecents(int callingPid, int callingUid, int taskId, SafeActivityOptions options) {
        return startActivityFromRecents(callingPid, callingUid, taskId, options, false);
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [2829=4, 2875=10, 2876=4, 2885=8, 2904=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't wrap try/catch for region: R(7:(15:(2:104|(2:106|(1:258)(4:109|(1:111)(1:257)|112|(3:132|133|(1:256)(2:137|(1:254)(24:140|141|142|143|(2:246|247)(1:145)|146|147|(1:241)(2:(3:222|223|(5:227|259|233|234|235))|151)|152|153|154|155|156|157|158|159|(6:161|(1:163)(1:195)|(1:165)(1:194)|166|(1:168)(1:193)|169)(1:196)|170|(5:172|(1:174)(2:189|(1:191))|175|(1:177)|178)(1:192)|179|(2:187|188)|181|182|183)))(6:116|(1:118)(2:128|(1:130)(1:131))|(2:126|127)|120|121|122)))(1:259))(1:260)|154|155|156|157|158|159|(0)(0)|170|(0)(0)|179|(0)|181|182|183)|146|147|(0)|241|152|153) */
    /* JADX WARN: Code restructure failed: missing block: B:195:0x03e2, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:196:0x03e3, code lost:
        r8 = r2;
        r3 = r7;
        r9 = r25;
        r19 = -1;
        r7 = r2;
        r2 = r26;
     */
    /* JADX WARN: Removed duplicated region for block: B:122:0x023c  */
    /* JADX WARN: Removed duplicated region for block: B:157:0x02ec A[Catch: all -> 0x043a, TryCatch #9 {all -> 0x043a, blocks: (B:155:0x02df, B:157:0x02ec, B:159:0x02f6, B:162:0x02ff, B:164:0x0306, B:166:0x030a, B:168:0x0313, B:170:0x031d, B:172:0x0335, B:174:0x036d, B:178:0x0382, B:180:0x039c, B:181:0x03a4, B:183:0x03b0, B:175:0x0375, B:177:0x037b, B:197:0x03f0, B:199:0x040c, B:201:0x0414, B:204:0x041d, B:206:0x0424, B:208:0x0428, B:210:0x0431, B:212:0x0439), top: B:292:0x0240 }] */
    /* JADX WARN: Removed duplicated region for block: B:169:0x031b  */
    /* JADX WARN: Removed duplicated region for block: B:172:0x0335 A[Catch: all -> 0x043a, TryCatch #9 {all -> 0x043a, blocks: (B:155:0x02df, B:157:0x02ec, B:159:0x02f6, B:162:0x02ff, B:164:0x0306, B:166:0x030a, B:168:0x0313, B:170:0x031d, B:172:0x0335, B:174:0x036d, B:178:0x0382, B:180:0x039c, B:181:0x03a4, B:183:0x03b0, B:175:0x0375, B:177:0x037b, B:197:0x03f0, B:199:0x040c, B:201:0x0414, B:204:0x041d, B:206:0x0424, B:208:0x0428, B:210:0x0431, B:212:0x0439), top: B:292:0x0240 }] */
    /* JADX WARN: Removed duplicated region for block: B:182:0x03ac  */
    /* JADX WARN: Removed duplicated region for block: B:199:0x040c A[Catch: all -> 0x043a, TryCatch #9 {all -> 0x043a, blocks: (B:155:0x02df, B:157:0x02ec, B:159:0x02f6, B:162:0x02ff, B:164:0x0306, B:166:0x030a, B:168:0x0313, B:170:0x031d, B:172:0x0335, B:174:0x036d, B:178:0x0382, B:180:0x039c, B:181:0x03a4, B:183:0x03b0, B:175:0x0375, B:177:0x037b, B:197:0x03f0, B:199:0x040c, B:201:0x0414, B:204:0x041d, B:206:0x0424, B:208:0x0428, B:210:0x0431, B:212:0x0439), top: B:292:0x0240 }] */
    /* JADX WARN: Removed duplicated region for block: B:276:0x0543 A[Catch: all -> 0x0576, TRY_ENTER, TryCatch #8 {all -> 0x0576, blocks: (B:276:0x0543, B:278:0x0549, B:281:0x0551, B:282:0x0575), top: B:308:0x006b }] */
    /* JADX WARN: Removed duplicated region for block: B:304:0x0221 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:315:0x03b6 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int startActivityFromRecents(int callingPid, int callingUid, int taskId, SafeActivityOptions options, boolean realStartFromRecentApp) {
        boolean moveHomeTaskForward;
        int activityType;
        boolean isWillBootInMultiWindow;
        Task task;
        ActivityMetricsLogger.LaunchingState launchingState;
        ActivityRecord targetActivity;
        String pkgName;
        Task task2;
        ActivityMetricsLogger.LaunchingState launchingState2;
        int i;
        TaskDisplayArea targetDisplayArea;
        boolean isWillBootInMultiWindow2;
        boolean z;
        TaskDisplayArea targetDisplayArea2;
        ActivityMetricsLogger.LaunchingState launchingState3;
        Task task3;
        boolean isWillBootInMultiWindow3;
        ActivityOptions activityOptions = options != null ? options.getOptions(this) : null;
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (activityOptions != null) {
                    try {
                        int activityType2 = activityOptions.getLaunchActivityType();
                        int windowingMode = activityOptions.getLaunchWindowingMode();
                        if (activityOptions.freezeRecentTasksReordering()) {
                            try {
                                if (ActivityTaskManagerService.checkPermission("android.permission.MANAGE_ACTIVITY_TASKS", callingPid, callingUid) == 0) {
                                    this.mRecentTasks.setFreezeTaskListReordering();
                                }
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
                        boolean moveHomeTaskForward2 = activityOptions.getLaunchRootTask() == null;
                        if (ITranActivityTaskSupervisor.Instance().hasMultiWindow(windowingMode)) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            return 102;
                        }
                        moveHomeTaskForward = moveHomeTaskForward2;
                        activityType = activityType2;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } else {
                    moveHomeTaskForward = true;
                    activityType = 0;
                }
                try {
                    if (activityType == 2 || activityType == 3) {
                        throw new IllegalArgumentException("startActivityFromRecents: Task " + taskId + " can't be launch in the home/recents root task.");
                    }
                    try {
                        this.mService.deferWindowLayout();
                        try {
                            Task task4 = this.mRootWindowContainer.anyTaskForId(taskId, 2, activityOptions, true);
                            if (task4 != null) {
                                if (moveHomeTaskForward) {
                                    try {
                                        this.mRootWindowContainer.getDefaultTaskDisplayArea().moveHomeRootTaskToFront("startActivityFromRecents");
                                    } catch (Throwable th4) {
                                        th = th4;
                                    }
                                }
                                try {
                                    if (!this.mService.mAmInternal.shouldConfirmCredentials(task4.mUserId)) {
                                        try {
                                            if (task4.getRootActivity() != null) {
                                                ActivityRecord targetActivity2 = task4.getTopNonFinishingActivity();
                                                ITranWindowManagerService.Instance().onStartActivityFromRecents(targetActivity2, callingPid);
                                                String pkgName2 = ITranMultiWindow.Instance().getReadyStartInMultiWindowPackageName();
                                                int multiWindowId = ITranMultiWindow.Instance().getReadyStartInMultiWindowId();
                                                TaskDisplayArea targetDisplayArea3 = null;
                                                if (ThunderbackConfig.isVersion4() && multiWindowId != -1) {
                                                    targetDisplayArea3 = this.mRootWindowContainer.getMultiDisplayArea(0, multiWindowId);
                                                }
                                                String taskName = targetActivity2 != null ? targetActivity2.packageName : "null";
                                                boolean isWillBootInMultiWindow4 = (taskName == null || pkgName2 == null) ? false : pkgName2.equals(taskName);
                                                Slog.i(TAG, "startFromRecent with taskName=" + taskName + "  pkgName=" + pkgName2 + "  isWillBootInMultiWindow=" + isWillBootInMultiWindow4);
                                                try {
                                                    try {
                                                        try {
                                                            try {
                                                                if (task4.getConfiguration() == null) {
                                                                    isWillBootInMultiWindow = isWillBootInMultiWindow4;
                                                                } else if (task4.getConfiguration().windowConfiguration == null) {
                                                                    isWillBootInMultiWindow = isWillBootInMultiWindow4;
                                                                } else if (!task4.getConfiguration().windowConfiguration.isThunderbackWindow() || isWillBootInMultiWindow4) {
                                                                    isWillBootInMultiWindow = isWillBootInMultiWindow4;
                                                                } else {
                                                                    TaskDisplayArea mTaskDisplayArea = task4.getDisplayArea();
                                                                    Task focusedRootTaskInMulti = mTaskDisplayArea != null ? mTaskDisplayArea.getFocusedRootTask() : null;
                                                                    if (focusedRootTaskInMulti != null && focusedRootTaskInMulti == task4 && realStartFromRecentApp) {
                                                                        if (ThunderbackConfig.isVersion3()) {
                                                                            this.mRootWindowContainer.hookMultiWindowToMax(0);
                                                                        } else if (ThunderbackConfig.isVersion4()) {
                                                                            this.mRootWindowContainer.hookReparentToDefaultDisplay(task4.getConfiguration().windowConfiguration.getMultiWindowingMode(), task4.getConfiguration().windowConfiguration.getMultiWindowingId(), true);
                                                                        }
                                                                        if (0 == 0) {
                                                                            try {
                                                                                this.mService.continueWindowLayout();
                                                                            } catch (Throwable th5) {
                                                                                th = th5;
                                                                                while (true) {
                                                                                    break;
                                                                                    break;
                                                                                }
                                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                                throw th;
                                                                            }
                                                                        }
                                                                        WindowManagerService.resetPriorityAfterLockedSection();
                                                                        return 2;
                                                                    }
                                                                    isWillBootInMultiWindow = isWillBootInMultiWindow4;
                                                                    RootWindowContainer rootWindowContainer = this.mRootWindowContainer;
                                                                    if (rootWindowContainer != null && rootWindowContainer.getDefaultTaskDisplayArea() != null) {
                                                                        if (this.mRootWindowContainer.getDefaultTaskDisplayArea().getDisplayAreaInfo() != null && activityOptions != null) {
                                                                            activityOptions.setLaunchDisplayId(0).setLaunchTaskDisplayArea(this.mRootWindowContainer.getDefaultTaskDisplayArea().getDisplayAreaInfo().token).setLaunchTaskId(taskId);
                                                                            task = this.mRootWindowContainer.anyTaskForId(taskId, 2, activityOptions, true);
                                                                            this.mRootWindowContainer.startPowerModeLaunchIfNeeded(true, targetActivity2);
                                                                            ActivityMetricsLogger activityMetricsLogger = this.mActivityMetricsLogger;
                                                                            Intent intent = task.intent;
                                                                            ActivityMetricsLogger.LaunchingState launchingState4 = activityMetricsLogger.notifyActivityLaunching(intent);
                                                                            if (ITranActivityManagerService.Instance().isAppLaunchEnable()) {
                                                                                launchingState = launchingState4;
                                                                                targetActivity = targetActivity2;
                                                                            } else {
                                                                                try {
                                                                                    ITranActivityStarter Instance = ITranActivityStarter.Instance();
                                                                                    intent = task.intent;
                                                                                    launchingState = launchingState4;
                                                                                    targetActivity = targetActivity2;
                                                                                    Instance.appLaunchStart(intent, task.mCallingPackage, 0L);
                                                                                } catch (Throwable th6) {
                                                                                    th = th6;
                                                                                }
                                                                            }
                                                                            if (ThunderbackConfig.isVersion4() || targetDisplayArea3 == null) {
                                                                                z = true;
                                                                            } else {
                                                                                if (activityOptions != null) {
                                                                                    try {
                                                                                        if (activityOptions.getStartMultiWindowByRecent() == 1 && task.getDisplayContent() != null) {
                                                                                            synchronized (this.mReparentFromRecentLock) {
                                                                                                ITranMultiWindowManager.Instance().hookShowBlurLayer(task.getSurfaceControl(), pkgName2);
                                                                                                try {
                                                                                                    this.mReparentFromRecentLock.wait(2000L);
                                                                                                } catch (InterruptedException e) {
                                                                                                    Slog.e(TAG, "Timeout to wait for the ShowBlurLayerFinish.");
                                                                                                }
                                                                                            }
                                                                                            task.mLauncherFromRecent = true;
                                                                                            task.getDisplayContent().prepareAppTransition(32);
                                                                                            task.reparent(targetDisplayArea3, true);
                                                                                            task.getDisplayContent().executeAppTransition();
                                                                                            task.mLauncherFromRecent = false;
                                                                                            z = true;
                                                                                        }
                                                                                    } catch (Throwable th7) {
                                                                                        th = th7;
                                                                                        pkgName = pkgName2;
                                                                                        task2 = task;
                                                                                        launchingState2 = launchingState;
                                                                                        i = -1;
                                                                                        targetDisplayArea = targetDisplayArea3;
                                                                                        isWillBootInMultiWindow2 = isWillBootInMultiWindow;
                                                                                        ActivityMetricsLogger.LaunchingState launchingState5 = launchingState2;
                                                                                        this.mActivityMetricsLogger.notifyActivityLaunched(launchingState2, 2, false, targetActivity, activityOptions);
                                                                                        if (ITranActivityManagerService.Instance().isAppLaunchEnable()) {
                                                                                        }
                                                                                        throw th;
                                                                                    }
                                                                                }
                                                                                z = true;
                                                                                task.mLauncherFromRecent = true;
                                                                                task.reparent(targetDisplayArea3, true);
                                                                                task.mLauncherFromRecent = false;
                                                                            }
                                                                            targetDisplayArea2 = targetDisplayArea3;
                                                                            launchingState3 = launchingState;
                                                                            i = -1;
                                                                            task3 = task;
                                                                            this.mService.moveTaskToFrontLocked(null, null, task.mTaskId, 0, options);
                                                                            targetActivity.applyOptionsAnimation();
                                                                            isWillBootInMultiWindow3 = isWillBootInMultiWindow;
                                                                            this.mActivityMetricsLogger.notifyActivityLaunched(launchingState3, 2, false, targetActivity, activityOptions);
                                                                            if (!ITranActivityManagerService.Instance().isAppLaunchEnable()) {
                                                                                ITranActivityStarter Instance2 = ITranActivityStarter.Instance();
                                                                                Intent intent2 = task3.intent;
                                                                                int i2 = launchingState3 != null ? launchingState3.launchType : -1;
                                                                                int i3 = launchingState3 != null ? launchingState3.procType : -1;
                                                                                RecentTasks recentTasks = this.mRecentTasks;
                                                                                Instance2.appLaunchEnd(intent2, i2, i3, recentTasks != null ? recentTasks.getTaskIndex(task3) : -1, 0L);
                                                                            }
                                                                            this.mService.getActivityStartController().postStartActivityProcessingForLastStarter(task3.getTopNonFinishingActivity(), 2, task3.getRootTask());
                                                                            this.mService.resumeAppSwitches();
                                                                            if (!isWillBootInMultiWindow3) {
                                                                                Slog.i(TAG, "startFromRecent with multiWindowId=" + multiWindowId + "  pkgName=" + pkgName2 + "  taskDisplayArea=" + targetDisplayArea2);
                                                                                TaskDisplayArea taskDisplayAreaForMultiWindow = null;
                                                                                if (ThunderbackConfig.isVersion4()) {
                                                                                    taskDisplayAreaForMultiWindow = this.mRootWindowContainer.getPerferTaskDisplayAreaFromPackageReady();
                                                                                } else if (ThunderbackConfig.isVersion3()) {
                                                                                    taskDisplayAreaForMultiWindow = this.mRootWindowContainer.getMultiDisplayArea();
                                                                                }
                                                                                Slog.i(TAG, " ActivityTaskSupervisor ::  taskDisplayAreaForMultiWindow = " + taskDisplayAreaForMultiWindow);
                                                                                if (taskDisplayAreaForMultiWindow != null) {
                                                                                    task3.mLauncherFromRecent = true;
                                                                                    this.mService.hookShowMultiDisplayWindow(task3);
                                                                                }
                                                                                ITranMultiWindow.Instance().resetReadyStartInMultiWindowPackage();
                                                                            }
                                                                            sourceConnectNeedReparent(task3);
                                                                            if (0 == 0) {
                                                                                try {
                                                                                    this.mService.continueWindowLayout();
                                                                                } catch (Throwable th8) {
                                                                                    th = th8;
                                                                                    while (true) {
                                                                                        break;
                                                                                        break;
                                                                                    }
                                                                                    WindowManagerService.resetPriorityAfterLockedSection();
                                                                                    throw th;
                                                                                }
                                                                            }
                                                                            WindowManagerService.resetPriorityAfterLockedSection();
                                                                            return 2;
                                                                        }
                                                                    }
                                                                }
                                                                isWillBootInMultiWindow3 = isWillBootInMultiWindow;
                                                                this.mActivityMetricsLogger.notifyActivityLaunched(launchingState3, 2, false, targetActivity, activityOptions);
                                                                if (!ITranActivityManagerService.Instance().isAppLaunchEnable()) {
                                                                }
                                                                this.mService.getActivityStartController().postStartActivityProcessingForLastStarter(task3.getTopNonFinishingActivity(), 2, task3.getRootTask());
                                                                this.mService.resumeAppSwitches();
                                                                if (!isWillBootInMultiWindow3) {
                                                                }
                                                                sourceConnectNeedReparent(task3);
                                                                if (0 == 0) {
                                                                }
                                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                                return 2;
                                                            } catch (Throwable th9) {
                                                                th = th9;
                                                            }
                                                            this.mService.moveTaskToFrontLocked(null, null, task.mTaskId, 0, options);
                                                            targetActivity.applyOptionsAnimation();
                                                        } catch (Throwable th10) {
                                                            th = th10;
                                                            task2 = task3;
                                                            launchingState2 = launchingState3;
                                                            isWillBootInMultiWindow2 = isWillBootInMultiWindow;
                                                            targetDisplayArea = targetDisplayArea2;
                                                            pkgName = pkgName2;
                                                            ActivityMetricsLogger.LaunchingState launchingState52 = launchingState2;
                                                            this.mActivityMetricsLogger.notifyActivityLaunched(launchingState2, 2, false, targetActivity, activityOptions);
                                                            if (ITranActivityManagerService.Instance().isAppLaunchEnable()) {
                                                                ITranActivityStarter Instance3 = ITranActivityStarter.Instance();
                                                                Intent intent3 = task2.intent;
                                                                int i4 = launchingState52 != null ? launchingState52.launchType : i;
                                                                int i5 = launchingState52 != null ? launchingState52.procType : i;
                                                                RecentTasks recentTasks2 = this.mRecentTasks;
                                                                Instance3.appLaunchEnd(intent3, i4, i5, recentTasks2 != null ? recentTasks2.getTaskIndex(task2) : i, 0L);
                                                            }
                                                            throw th;
                                                        }
                                                        if (ThunderbackConfig.isVersion4()) {
                                                        }
                                                        z = true;
                                                        targetDisplayArea2 = targetDisplayArea3;
                                                        launchingState3 = launchingState;
                                                        i = -1;
                                                        task3 = task;
                                                    } catch (Throwable th11) {
                                                        th = th11;
                                                    }
                                                    this.mRootWindowContainer.startPowerModeLaunchIfNeeded(true, targetActivity2);
                                                    ActivityMetricsLogger activityMetricsLogger2 = this.mActivityMetricsLogger;
                                                    Intent intent4 = task.intent;
                                                    ActivityMetricsLogger.LaunchingState launchingState42 = activityMetricsLogger2.notifyActivityLaunching(intent4);
                                                    if (ITranActivityManagerService.Instance().isAppLaunchEnable()) {
                                                    }
                                                } catch (Throwable th12) {
                                                    th = th12;
                                                }
                                                task = task4;
                                            }
                                        } catch (Throwable th13) {
                                            th = th13;
                                        }
                                    }
                                    if (1 == 0) {
                                        try {
                                            this.mService.continueWindowLayout();
                                        } catch (Throwable th14) {
                                            th = th14;
                                            while (true) {
                                                break;
                                                break;
                                            }
                                            WindowManagerService.resetPriorityAfterLockedSection();
                                            throw th;
                                        }
                                    }
                                    try {
                                        int taskCallingUid = task4.mCallingUid;
                                        String callingPackage = task4.mCallingPackage;
                                        String callingFeatureId = task4.mCallingFeatureId;
                                        Intent intent5 = task4.intent;
                                        intent5.addFlags(1048576);
                                        int userId = task4.mUserId;
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        if (task4 != null) {
                                            task4.updateTaskMinAspectRatioForSetting();
                                        }
                                        try {
                                            try {
                                                int startActivityInPackage = this.mService.getActivityStartController().startActivityInPackage(taskCallingUid, callingPid, callingUid, callingPackage, callingFeatureId, intent5, null, null, null, 0, 0, options, userId, task4, "startActivityFromRecents", false, null, false);
                                                synchronized (this.mService.mGlobalLock) {
                                                    try {
                                                        WindowManagerService.boostPriorityForLockedSection();
                                                        this.mService.continueWindowLayout();
                                                    } finally {
                                                    }
                                                }
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                return startActivityInPackage;
                                            } catch (Throwable th15) {
                                                th = th15;
                                                synchronized (this.mService.mGlobalLock) {
                                                    try {
                                                        WindowManagerService.boostPriorityForLockedSection();
                                                        this.mService.continueWindowLayout();
                                                    } finally {
                                                    }
                                                }
                                                WindowManagerService.resetPriorityAfterLockedSection();
                                                throw th;
                                            }
                                        } catch (Throwable th16) {
                                            th = th16;
                                        }
                                    } catch (Throwable th17) {
                                        th = th17;
                                        while (true) {
                                            break;
                                            break;
                                        }
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        throw th;
                                    }
                                } catch (Throwable th18) {
                                    th = th18;
                                }
                            } else {
                                try {
                                    this.mWindowManager.executeAppTransition();
                                    try {
                                        throw new IllegalArgumentException("startActivityFromRecents: Task " + taskId + " not found.");
                                    } catch (Throwable th19) {
                                        th = th19;
                                        if (0 == 0) {
                                        }
                                        throw th;
                                    }
                                } catch (Throwable th20) {
                                    th = th20;
                                }
                            }
                        } catch (Throwable th21) {
                            th = th21;
                        }
                        if (0 == 0) {
                            this.mService.continueWindowLayout();
                        }
                        throw th;
                    } catch (Throwable th22) {
                        th = th22;
                    }
                } catch (Throwable th23) {
                    th = th23;
                }
            } catch (Throwable th24) {
                th = th24;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class WaitInfo {
        final ActivityMetricsLogger.LaunchingState mLaunchingState;
        final WaitResult mResult;
        final ComponentName mTargetComponent;

        WaitInfo(WaitResult result, ComponentName component, ActivityMetricsLogger.LaunchingState launchingState) {
            this.mResult = result;
            this.mTargetComponent = component;
            this.mLaunchingState = launchingState;
        }

        boolean matches(ActivityRecord r) {
            return this.mTargetComponent.equals(r.mActivityComponent) || this.mLaunchingState.contains(r);
        }

        void dump(PrintWriter pw, String prefix) {
            pw.println(prefix + "WaitInfo:");
            pw.println(prefix + "  mTargetComponent=" + this.mTargetComponent);
            pw.println(prefix + "  mResult=");
            this.mResult.dump(pw, prefix + "    ");
        }
    }

    public void sourceConnectNeedReparent(Task task) {
        if (task.getDisplayContent() != null && task.getDisplayContent().isSourceConnectDisplay()) {
            DisplayContent defaultDisplay = this.mRootWindowContainer.getDisplayContentOrCreate(0);
            task.reparent(defaultDisplay.getDefaultTaskDisplayArea(), true);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hookShowBlurLayerFinish() {
        Slog.i("demon_fwk", " ActivityTaskSupervisor::hookShowBlurLayerFinish notifyall start-1.");
        synchronized (this.mReparentFromRecentLock) {
            Slog.i("demon_fwk", " ActivityTaskSupervisor::hookShowBlurLayerFinish notifyall start-1.");
            this.mReparentFromRecentLock.notifyAll();
            Slog.i("demon_fwk", " ActivityTaskSupervisor::hookShowBlurLayerFinish notifyall end.");
        }
    }
}
