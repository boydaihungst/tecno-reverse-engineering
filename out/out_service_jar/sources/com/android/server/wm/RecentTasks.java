package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.wm.RecentTasks;
import com.google.android.collect.Sets;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import com.transsion.hubcore.server.wm.ITranRecentTasks;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RecentTasks {
    private static final int DEFAULT_INITIAL_CAPACITY = 5;
    public static final int LAUNCH_NOT_RECENT = -1;
    public static final int LAUNCH_SAME_APP = -2;
    public static final int LAUNCH_UNSUPPORT = -3;
    public static final int MAX_V_RECENT_AGARES = 22;
    public static final int MAX_V_RECENT_DEFAULT = 20;
    public static final int MAX_V_RECENT_MEMFUSION = 21;
    public static final int MSG_UPDATE_AGARES_RECENT = 22;
    public static final int MSG_UPDATE_DEFAULT_RECENT = 20;
    public static final int MSG_UPDATE_MEMFUSION_RECENT = 21;
    public static final int TYPE_KEEP_ALIVE = 2;
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_PRELOAD = 1;
    private long mActiveTasksSessionDurationMs;
    private final ArrayList<Callbacks> mCallbacks;
    private boolean mCheckTrimmableTasksOnIdle;
    private String mFeatureId;
    private boolean mFreezeTaskListReordering;
    private long mFreezeTaskListTimeoutMs;
    private int mGlobalMaxNumTasks;
    private boolean mHasVisibleRecentTasks;
    private final ArrayList<Task> mHiddenTasks;
    private final WindowManagerPolicyConstants.PointerEventListener mListener;
    private int mMaxNumVisibleTasks;
    private int mMinNumVisibleTasks;
    private final SparseArray<SparseBooleanArray> mPersistedTaskIds;
    private ComponentName mRecentsComponent;
    private int mRecentsUid;
    private final Runnable mResetFreezeTaskListOnTimeoutRunnable;
    private final ActivityTaskManagerService mService;
    private final ActivityTaskSupervisor mSupervisor;
    private TaskChangeNotificationController mTaskNotificationController;
    private final TaskPersister mTaskPersister;
    private final ArrayList<Task> mTasks;
    private final HashMap<ComponentName, ActivityInfo> mTmpAvailActCache;
    private final HashMap<String, ApplicationInfo> mTmpAvailAppCache;
    private final SparseBooleanArray mTmpQuietProfileUserIds;
    private final ArrayList<Task> mTmpRecents;
    private final SparseBooleanArray mUsersWithRecentsLoaded;
    private static final String TAG = "ActivityTaskManager";
    private static final String TAG_RECENTS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_RECENTS;
    private static final String TAG_TASKS = TAG + ActivityTaskManagerDebugConfig.POSTFIX_TASKS;
    private static final long FREEZE_TASK_LIST_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
    private static final Comparator<Task> TASK_ID_COMPARATOR = new Comparator() { // from class: com.android.server.wm.RecentTasks$$ExternalSyntheticLambda1
        @Override // java.util.Comparator
        public final int compare(Object obj, Object obj2) {
            return RecentTasks.lambda$static$0((Task) obj, (Task) obj2);
        }
    };
    private static final ActivityInfo NO_ACTIVITY_INFO_TOKEN = new ActivityInfo();
    private static final ApplicationInfo NO_APPLICATION_INFO_TOKEN = new ApplicationInfo();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface Callbacks {
        void onRecentTaskAdded(Task task);

        void onRecentTaskRemoved(Task task, boolean z, boolean z2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$static$0(Task lhs, Task rhs) {
        return rhs.mTaskId - lhs.mTaskId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.wm.RecentTasks$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 implements WindowManagerPolicyConstants.PointerEventListener {
        AnonymousClass1() {
        }

        public void onPointerEvent(MotionEvent ev) {
            if (!RecentTasks.this.mFreezeTaskListReordering || ev.getAction() != 0) {
                return;
            }
            final int displayId = ev.getDisplayId();
            final int x = (int) ev.getX();
            final int y = (int) ev.getY();
            RecentTasks.this.mService.mH.post(PooledLambda.obtainRunnable(new Consumer() { // from class: com.android.server.wm.RecentTasks$1$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    RecentTasks.AnonymousClass1.this.m8142lambda$onPointerEvent$0$comandroidserverwmRecentTasks$1(displayId, x, y, obj);
                }
            }, (Object) null).recycleOnUse());
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onPointerEvent$0$com-android-server-wm-RecentTasks$1  reason: not valid java name */
        public /* synthetic */ void m8142lambda$onPointerEvent$0$comandroidserverwmRecentTasks$1(int displayId, int x, int y, Object nonArg) {
            synchronized (RecentTasks.this.mService.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    RootWindowContainer rac = RecentTasks.this.mService.mRootWindowContainer;
                    DisplayContent dc = rac.getDisplayContent(displayId).mDisplayContent;
                    if (dc.pointWithinAppWindow(x, y)) {
                        Task stack = RecentTasks.this.mService.getTopDisplayFocusedRootTask();
                        Task topTask = stack != null ? stack.getTopMostTask() : null;
                        RecentTasks.this.resetFreezeTaskListReordering(topTask);
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    RecentTasks(ActivityTaskManagerService service, TaskPersister taskPersister) {
        this.mRecentsUid = -1;
        this.mRecentsComponent = null;
        this.mUsersWithRecentsLoaded = new SparseBooleanArray(5);
        this.mPersistedTaskIds = new SparseArray<>(5);
        this.mTasks = new ArrayList<>();
        this.mCallbacks = new ArrayList<>();
        this.mHiddenTasks = new ArrayList<>();
        this.mFreezeTaskListTimeoutMs = FREEZE_TASK_LIST_TIMEOUT_MS;
        this.mTmpRecents = new ArrayList<>();
        this.mTmpAvailActCache = new HashMap<>();
        this.mTmpAvailAppCache = new HashMap<>();
        this.mTmpQuietProfileUserIds = new SparseBooleanArray();
        this.mListener = new AnonymousClass1();
        this.mResetFreezeTaskListOnTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.RecentTasks$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RecentTasks.this.resetFreezeTaskListReorderingOnTimeout();
            }
        };
        this.mService = service;
        this.mSupervisor = service.mTaskSupervisor;
        this.mTaskPersister = taskPersister;
        this.mGlobalMaxNumTasks = ActivityTaskManager.getMaxRecentTasksStatic();
        this.mHasVisibleRecentTasks = true;
        this.mTaskNotificationController = service.getTaskChangeNotificationController();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RecentTasks(ActivityTaskManagerService service, ActivityTaskSupervisor taskSupervisor) {
        this.mRecentsUid = -1;
        this.mRecentsComponent = null;
        this.mUsersWithRecentsLoaded = new SparseBooleanArray(5);
        this.mPersistedTaskIds = new SparseArray<>(5);
        this.mTasks = new ArrayList<>();
        this.mCallbacks = new ArrayList<>();
        this.mHiddenTasks = new ArrayList<>();
        this.mFreezeTaskListTimeoutMs = FREEZE_TASK_LIST_TIMEOUT_MS;
        this.mTmpRecents = new ArrayList<>();
        this.mTmpAvailActCache = new HashMap<>();
        this.mTmpAvailAppCache = new HashMap<>();
        this.mTmpQuietProfileUserIds = new SparseBooleanArray();
        this.mListener = new AnonymousClass1();
        this.mResetFreezeTaskListOnTimeoutRunnable = new Runnable() { // from class: com.android.server.wm.RecentTasks$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                RecentTasks.this.resetFreezeTaskListReorderingOnTimeout();
            }
        };
        File systemDir = Environment.getDataSystemDirectory();
        Resources res = service.mContext.getResources();
        this.mService = service;
        this.mSupervisor = service.mTaskSupervisor;
        this.mTaskPersister = new TaskPersister(systemDir, taskSupervisor, service, this, taskSupervisor.mPersisterQueue);
        this.mGlobalMaxNumTasks = ActivityTaskManager.getMaxRecentTasksStatic();
        this.mTaskNotificationController = service.getTaskChangeNotificationController();
        this.mHasVisibleRecentTasks = res.getBoolean(17891678);
        loadParametersFromResources(res);
    }

    void setParameters(int minNumVisibleTasks, int maxNumVisibleTasks, long activeSessionDurationMs) {
        this.mMinNumVisibleTasks = minNumVisibleTasks;
        this.mMaxNumVisibleTasks = maxNumVisibleTasks;
        if (ITranActivityManagerService.Instance().isAppLaunchEnable()) {
            ITranRecentTasks.Instance().setMaxVisableRecent(20, this.mMaxNumVisibleTasks);
        }
        this.mActiveTasksSessionDurationMs = activeSessionDurationMs;
    }

    void setGlobalMaxNumTasks(int globalMaxNumTasks) {
        this.mGlobalMaxNumTasks = globalMaxNumTasks;
    }

    void setFreezeTaskListTimeout(long timeoutMs) {
        this.mFreezeTaskListTimeoutMs = timeoutMs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WindowManagerPolicyConstants.PointerEventListener getInputListener() {
        return this.mListener;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFreezeTaskListReordering() {
        if (!this.mFreezeTaskListReordering) {
            this.mTaskNotificationController.notifyTaskListFrozen(true);
            this.mFreezeTaskListReordering = true;
        }
        this.mService.mH.removeCallbacks(this.mResetFreezeTaskListOnTimeoutRunnable);
        this.mService.mH.postDelayed(this.mResetFreezeTaskListOnTimeoutRunnable, this.mFreezeTaskListTimeoutMs);
    }

    void resetFreezeTaskListReordering(Task topTask) {
        if (!this.mFreezeTaskListReordering) {
            return;
        }
        this.mFreezeTaskListReordering = false;
        this.mService.mH.removeCallbacks(this.mResetFreezeTaskListOnTimeoutRunnable);
        if (topTask != null) {
            this.mTasks.remove(topTask);
            this.mTasks.add(0, topTask);
        }
        trimInactiveRecentTasks();
        this.mTaskNotificationController.notifyTaskStackChanged();
        this.mTaskNotificationController.notifyTaskListFrozen(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetFreezeTaskListReorderingOnTimeout() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task focusedStack = this.mService.getTopDisplayFocusedRootTask();
                Task topTask = focusedStack != null ? focusedStack.getTopMostTask() : null;
                resetFreezeTaskListReordering(topTask);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFreezeTaskListReorderingSet() {
        return this.mFreezeTaskListReordering;
    }

    void loadParametersFromResources(Resources res) {
        long j;
        if (ActivityManager.isLowRamDeviceStatic()) {
            this.mMinNumVisibleTasks = res.getInteger(17694878);
            this.mMaxNumVisibleTasks = res.getInteger(17694870);
        } else if (SystemProperties.getBoolean("ro.recents.grid", false)) {
            this.mMinNumVisibleTasks = res.getInteger(17694877);
            this.mMaxNumVisibleTasks = res.getInteger(17694869);
        } else {
            this.mMinNumVisibleTasks = res.getInteger(17694876);
            this.mMaxNumVisibleTasks = res.getInteger(17694868);
        }
        if (ITranActivityManagerService.Instance().isAppLaunchEnable()) {
            ITranRecentTasks.Instance().setMaxVisableRecent(20, this.mMaxNumVisibleTasks);
        }
        this.mMaxNumVisibleTasks = ITranRecentTasks.Instance().getMaxNumVisibleTasks(this.mService.mContext, this.mMaxNumVisibleTasks);
        int sessionDurationHrs = res.getInteger(17694729);
        if (sessionDurationHrs > 0) {
            j = TimeUnit.HOURS.toMillis(sessionDurationHrs);
        } else {
            j = -1;
        }
        this.mActiveTasksSessionDurationMs = j;
    }

    void loadRecentsComponent(Resources res) {
        ComponentName cn;
        String rawRecentsComponent = res.getString(17040025);
        if (!TextUtils.isEmpty(rawRecentsComponent) && (cn = ComponentName.unflattenFromString(rawRecentsComponent)) != null) {
            try {
                ApplicationInfo appInfo = AppGlobals.getPackageManager().getApplicationInfo(cn.getPackageName(), 8704L, this.mService.mContext.getUserId());
                if (appInfo != null) {
                    this.mRecentsUid = appInfo.uid;
                    this.mRecentsComponent = cn;
                }
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not load application info for recents component: " + cn);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCallerRecents(int callingUid) {
        return UserHandle.isSameApp(callingUid, this.mRecentsUid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRecentsComponent(ComponentName cn, int uid) {
        return cn.equals(this.mRecentsComponent) && UserHandle.isSameApp(uid, this.mRecentsUid);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRecentsComponentHomeActivity(int userId) {
        ComponentName defaultHomeActivity = this.mService.getPackageManagerInternalLocked().getDefaultHomeActivity(userId);
        return (defaultHomeActivity == null || this.mRecentsComponent == null || !defaultHomeActivity.getPackageName().equals(this.mRecentsComponent.getPackageName())) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ComponentName getRecentsComponent() {
        return this.mRecentsComponent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getRecentsComponentFeatureId() {
        return this.mFeatureId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getRecentsComponentUid() {
        return this.mRecentsUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerCallback(Callbacks callback) {
        this.mCallbacks.add(callback);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterCallback(Callbacks callback) {
        this.mCallbacks.remove(callback);
    }

    private void notifyTaskAdded(Task task) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onRecentTaskAdded(task);
        }
        this.mTaskNotificationController.notifyTaskListUpdated();
    }

    private void notifyTaskRemoved(Task task, boolean wasTrimmed, boolean killProcess) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onRecentTaskRemoved(task, wasTrimmed, killProcess);
        }
        this.mTaskNotificationController.notifyTaskListUpdated();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadUserRecentsLocked(int userId) {
        if (this.mUsersWithRecentsLoaded.get(userId)) {
            return;
        }
        loadPersistedTaskIdsForUserLocked(userId);
        SparseBooleanArray preaddedTasks = new SparseBooleanArray();
        Iterator<Task> it = this.mTasks.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.mUserId == userId && shouldPersistTaskLocked(task)) {
                preaddedTasks.put(task.mTaskId, true);
            }
        }
        Slog.i(TAG, "Loading recents for user " + userId + " into memory.");
        List<Task> tasks = this.mTaskPersister.restoreTasksForUserLocked(userId, preaddedTasks);
        this.mTasks.addAll(tasks);
        cleanupLocked(userId);
        this.mUsersWithRecentsLoaded.put(userId, true);
        if (preaddedTasks.size() > 0) {
            syncPersistentTaskIdsLocked();
        }
    }

    private void loadPersistedTaskIdsForUserLocked(int userId) {
        if (this.mPersistedTaskIds.get(userId) == null) {
            this.mPersistedTaskIds.put(userId, this.mTaskPersister.loadPersistedTaskIdsForUser(userId));
            Slog.i(TAG, "Loaded persisted task ids for user " + userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsTaskId(int taskId, int userId) {
        loadPersistedTaskIdsForUserLocked(userId);
        return this.mPersistedTaskIds.get(userId).get(taskId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SparseBooleanArray getTaskIdsForUser(int userId) {
        loadPersistedTaskIdsForUserLocked(userId);
        return this.mPersistedTaskIds.get(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyTaskPersisterLocked(Task task, boolean flush) {
        Task rootTask = task != null ? task.getRootTask() : null;
        if (rootTask != null && rootTask.isActivityTypeHomeOrRecents()) {
            return;
        }
        syncPersistentTaskIdsLocked();
        this.mTaskPersister.wakeup(task, flush);
    }

    private void syncPersistentTaskIdsLocked() {
        for (int i = this.mPersistedTaskIds.size() - 1; i >= 0; i--) {
            int userId = this.mPersistedTaskIds.keyAt(i);
            if (this.mUsersWithRecentsLoaded.get(userId)) {
                this.mPersistedTaskIds.valueAt(i).clear();
            }
        }
        for (int i2 = this.mTasks.size() - 1; i2 >= 0; i2--) {
            Task task = this.mTasks.get(i2);
            if (shouldPersistTaskLocked(task)) {
                if (this.mPersistedTaskIds.get(task.mUserId) == null) {
                    Slog.wtf(TAG, "No task ids found for userId " + task.mUserId + ". task=" + task + " mPersistedTaskIds=" + this.mPersistedTaskIds);
                    this.mPersistedTaskIds.put(task.mUserId, new SparseBooleanArray());
                }
                this.mPersistedTaskIds.get(task.mUserId).put(task.mTaskId, true);
            }
        }
    }

    private static boolean shouldPersistTaskLocked(Task task) {
        Task rootTask = task.getRootTask();
        return task.isPersistable && (rootTask == null || !rootTask.isActivityTypeHomeOrRecents());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReadyLocked() {
        loadRecentsComponent(this.mService.mContext.getResources());
        this.mTasks.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bitmap getTaskDescriptionIcon(String path) {
        return this.mTaskPersister.getTaskDescriptionIcon(path);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveImage(Bitmap image, String path) {
        this.mTaskPersister.saveImage(image, path);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void flush() {
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                syncPersistentTaskIdsLocked();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        this.mTaskPersister.flush();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] usersWithRecentsLoadedLocked() {
        int[] usersWithRecentsLoaded = new int[this.mUsersWithRecentsLoaded.size()];
        int len = 0;
        for (int i = 0; i < usersWithRecentsLoaded.length; i++) {
            int userId = this.mUsersWithRecentsLoaded.keyAt(i);
            if (this.mUsersWithRecentsLoaded.valueAt(i)) {
                usersWithRecentsLoaded[len] = userId;
                len++;
            }
        }
        int i2 = usersWithRecentsLoaded.length;
        if (len < i2) {
            return Arrays.copyOf(usersWithRecentsLoaded, len);
        }
        return usersWithRecentsLoaded;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unloadUserDataFromMemoryLocked(int userId) {
        if (this.mUsersWithRecentsLoaded.get(userId)) {
            Slog.i(TAG, "Unloading recents for user " + userId + " from memory.");
            this.mUsersWithRecentsLoaded.delete(userId);
            removeTasksForUserLocked(userId);
        }
        this.mPersistedTaskIds.delete(userId);
        this.mTaskPersister.unloadUserDataFromMemory(userId);
    }

    private void removeTasksForUserLocked(int userId) {
        if (userId <= 0) {
            Slog.i(TAG, "Can't remove recent task on user " + userId);
            return;
        }
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            Task task = this.mTasks.get(i);
            if (task.mUserId == userId) {
                if (ProtoLogCache.WM_DEBUG_TASKS_enabled) {
                    String protoLogParam0 = String.valueOf(task);
                    long protoLogParam1 = userId;
                    ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_TASKS, -1647332198, 4, (String) null, new Object[]{protoLogParam0, Long.valueOf(protoLogParam1)});
                }
                remove(task);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackagesSuspendedChanged(String[] packages, boolean suspended, int userId) {
        Set<String> packageNames = Sets.newHashSet(packages);
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            Task task = this.mTasks.get(i);
            if (task.realActivity != null && packageNames.contains(task.realActivity.getPackageName()) && task.mUserId == userId && task.realActivitySuspended != suspended) {
                task.realActivitySuspended = suspended;
                if (suspended) {
                    this.mSupervisor.removeTask(task, false, true, "suspended-package");
                }
                notifyTaskPersisterLocked(task, false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onLockTaskModeStateChanged(int lockTaskModeState, int userId) {
        if (lockTaskModeState == 1) {
            for (int i = this.mTasks.size() - 1; i >= 0; i--) {
                Task task = this.mTasks.get(i);
                if (task.mUserId == userId) {
                    this.mService.getLockTaskController();
                    if (!LockTaskController.isTaskAuthAllowlisted(task.mLockTaskAuth)) {
                        remove(task);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeTasksByPackageName(String packageName, int userId) {
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            Task task = this.mTasks.get(i);
            String taskPackageName = task.getBaseIntent().getComponent().getPackageName();
            if (task.mUserId == userId && taskPackageName.equals(packageName)) {
                this.mSupervisor.removeTask(task, true, true, "remove-package-task");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeTasksByPackageName(String packageName, int userId, boolean killProcess, boolean removeFromRecents, String reason) {
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            Task task = this.mTasks.get(i);
            String taskPackageName = task.getBaseIntent().getComponent().getPackageName();
            if (task.mUserId == userId && taskPackageName.equals(packageName)) {
                this.mSupervisor.removeTask(task, killProcess, removeFromRecents, reason);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeAllVisibleTasks(int userId) {
        Set<Integer> profileIds = getProfileIds(userId);
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            Task task = this.mTasks.get(i);
            if (profileIds.contains(Integer.valueOf(task.mUserId)) && isVisibleRecentTask(task)) {
                this.mTasks.remove(i);
                notifyTaskRemoved(task, true, true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanupDisabledPackageTasksLocked(String packageName, Set<String> filterByClasses, int userId) {
        for (int i = this.mTasks.size() - 1; i >= 0; i--) {
            Task task = this.mTasks.get(i);
            if (userId == -1 || task.mUserId == userId) {
                ComponentName cn = task.intent != null ? task.intent.getComponent() : null;
                boolean sameComponent = cn != null && cn.getPackageName().equals(packageName) && (filterByClasses == null || filterByClasses.contains(cn.getClassName()));
                if (sameComponent) {
                    this.mSupervisor.removeTask(task, false, true, "disabled-package");
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void cleanupLocked(int userId) {
        int recentsCount = this.mTasks.size();
        if (recentsCount == 0) {
            return;
        }
        this.mTmpAvailActCache.clear();
        this.mTmpAvailAppCache.clear();
        IPackageManager pm = AppGlobals.getPackageManager();
        for (int i = recentsCount - 1; i >= 0; i--) {
            Task task = this.mTasks.get(i);
            if (userId == -1 || task.mUserId == userId) {
                if (task.autoRemoveRecents && task.getTopNonFinishingActivity() == null) {
                    remove(task);
                    Slog.w(TAG, "Removing auto-remove without activity: " + task);
                } else if (task.realActivity != null) {
                    ActivityInfo ai = this.mTmpAvailActCache.get(task.realActivity);
                    if (ai == null) {
                        try {
                            ai = pm.getActivityInfo(task.realActivity, 268436480L, userId);
                            if (ai == null) {
                                ai = NO_ACTIVITY_INFO_TOKEN;
                            }
                            this.mTmpAvailActCache.put(task.realActivity, ai);
                        } catch (RemoteException e) {
                        }
                    }
                    if (ai == NO_ACTIVITY_INFO_TOKEN) {
                        ApplicationInfo app = this.mTmpAvailAppCache.get(task.realActivity.getPackageName());
                        if (app == null) {
                            try {
                                app = pm.getApplicationInfo(task.realActivity.getPackageName(), 8192L, userId);
                                if (app == null) {
                                    app = NO_APPLICATION_INFO_TOKEN;
                                }
                                this.mTmpAvailAppCache.put(task.realActivity.getPackageName(), app);
                            } catch (RemoteException e2) {
                            }
                        }
                        if (app == NO_APPLICATION_INFO_TOKEN || (8388608 & app.flags) == 0) {
                            remove(task);
                            Slog.w(TAG, "Removing no longer valid recent: " + task);
                        } else {
                            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS && task.isAvailable) {
                                Slog.d(TAG_RECENTS, "Making recent unavailable: " + task);
                            }
                            task.isAvailable = false;
                        }
                    } else if (!ai.enabled || !ai.applicationInfo.enabled || (ai.applicationInfo.flags & 8388608) == 0) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS && task.isAvailable) {
                            Slog.d(TAG_RECENTS, "Making recent unavailable: " + task + " (enabled=" + ai.enabled + SliceClientPermissions.SliceAuthority.DELIMITER + ai.applicationInfo.enabled + " flags=" + Integer.toHexString(ai.applicationInfo.flags) + ")");
                        }
                        task.isAvailable = false;
                    } else {
                        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS && !task.isAvailable) {
                            Slog.d(TAG_RECENTS, "Making recent available: " + task);
                        }
                        task.isAvailable = true;
                    }
                }
            }
        }
        int i2 = 0;
        int recentsCount2 = this.mTasks.size();
        while (i2 < recentsCount2) {
            i2 = processNextAffiliateChainLocked(i2);
        }
    }

    private boolean canAddTaskWithoutTrim(Task task) {
        return findRemoveIndexForAddTask(task) == -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<IBinder> getAppTasksList(int callingUid, String callingPackage) {
        Intent intent;
        ArrayList<IBinder> list = new ArrayList<>();
        int size = this.mTasks.size();
        for (int i = 0; i < size; i++) {
            Task task = this.mTasks.get(i);
            if (task.effectiveUid == callingUid && (intent = task.getBaseIntent()) != null && callingPackage.equals(intent.getComponent().getPackageName())) {
                AppTaskImpl taskImpl = new AppTaskImpl(this.mService, task.mTaskId, callingUid);
                list.add(taskImpl.asBinder());
            }
        }
        return list;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBinder getAppTaskByTaskId(int callingUid, int taskId) {
        int size = this.mTasks.size();
        for (int i = 0; i < size; i++) {
            Task task = this.mTasks.get(i);
            if (task.mTaskId == taskId) {
                AppTaskImpl taskImpl = new AppTaskImpl(this.mService, task.mTaskId, callingUid);
                return taskImpl.asBinder();
            }
        }
        return null;
    }

    Set<Integer> getProfileIds(int userId) {
        Set<Integer> userIds = new ArraySet<>();
        int[] profileIds = this.mService.getUserManager().getProfileIds(userId, false);
        for (int i : profileIds) {
            userIds.add(Integer.valueOf(i));
        }
        return userIds;
    }

    UserInfo getUserInfo(int userId) {
        return this.mService.getUserManager().getUserInfo(userId);
    }

    int[] getCurrentProfileIds() {
        return this.mService.mAmInternal.getCurrentProfileIds();
    }

    boolean isUserRunning(int userId, int flags) {
        return this.mService.mAmInternal.isUserRunning(userId, flags);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ParceledListSlice<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags, boolean getTasksAllowed, int userId, int callingUid) {
        return new ParceledListSlice<>(getRecentTasksImpl(maxNum, flags, getTasksAllowed, userId, callingUid));
    }

    private ArrayList<ActivityManager.RecentTaskInfo> getRecentTasksImpl(int maxNum, int flags, boolean getTasksAllowed, int userId, int callingUid) {
        boolean withExcluded = (flags & 1) != 0;
        if (!isUserRunning(userId, 4)) {
            Slog.i(TAG, "user " + userId + " is still locked. Cannot load recents");
            return new ArrayList<>();
        }
        loadUserRecentsLocked(userId);
        Set<Integer> includedUsers = getProfileIds(userId);
        includedUsers.add(Integer.valueOf(userId));
        ArrayList<ActivityManager.RecentTaskInfo> res = new ArrayList<>();
        int size = this.mTasks.size();
        int numVisibleTasks = 0;
        for (int i = 0; i < size; i++) {
            Task task = this.mTasks.get(i);
            if (!isForceInvisibleRecentTask(task) && isVisibleRecentTask(task)) {
                numVisibleTasks++;
                if (isInVisibleRange(task, i, numVisibleTasks, withExcluded) && res.size() < maxNum) {
                    if (!includedUsers.contains(Integer.valueOf(task.mUserId))) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                            Slog.d(TAG_RECENTS, "Skipping, not user: " + task);
                        }
                    } else if (task.realActivitySuspended) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                            Slog.d(TAG_RECENTS, "Skipping, activity suspended: " + task);
                        }
                    } else if (!getTasksAllowed && !task.isActivityTypeHome() && task.effectiveUid != callingUid) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                            Slog.d(TAG_RECENTS, "Skipping, not allowed: " + task);
                        }
                    } else if (task.autoRemoveRecents && task.getTopNonFinishingActivity() == null) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                            Slog.d(TAG_RECENTS, "Skipping, auto-remove without activity: " + task);
                        }
                    } else if ((flags & 2) != 0 && !task.isAvailable) {
                        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                            Slog.d(TAG_RECENTS, "Skipping, unavail real act: " + task);
                        }
                    } else if (task.mUserSetupComplete) {
                        res.add(createRecentTaskInfo(task, true, getTasksAllowed));
                    } else if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "Skipping, user setup not complete: " + task);
                    }
                }
            }
        }
        Slog.d(TAG_RECENTS, "getRecentTasksImpl call by " + callingUid + " flags = " + flags + ", " + res.size() + " task(s):");
        for (int i2 = 0; i2 < res.size(); i2++) {
            ActivityManager.RecentTaskInfo rti = res.get(i2);
            Slog.d(TAG_RECENTS, "Recent Task " + i2 + ": taskId = " + rti.taskId + ", comp = " + rti.origActivity);
        }
        return res;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getPersistableTaskIds(ArraySet<Integer> persistentTaskIds) {
        int size = this.mTasks.size();
        for (int i = 0; i < size; i++) {
            Task task = this.mTasks.get(i);
            Task rootTask = task.getRootTask();
            if ((task.isPersistable || task.inRecents) && (rootTask == null || !rootTask.isActivityTypeHomeOrRecents())) {
                persistentTaskIds.add(Integer.valueOf(task.mTaskId));
            }
        }
    }

    ArrayList<Task> getRawTasks() {
        return this.mTasks;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SparseBooleanArray getRecentTaskIds() {
        SparseBooleanArray res = new SparseBooleanArray();
        int size = this.mTasks.size();
        int numVisibleTasks = 0;
        for (int i = 0; i < size; i++) {
            Task task = this.mTasks.get(i);
            if (isVisibleRecentTask(task)) {
                numVisibleTasks++;
                if (isInVisibleRange(task, i, numVisibleTasks, false)) {
                    res.put(task.mTaskId, true);
                }
            }
        }
        return res;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getTask(int id) {
        int recentsCount = this.mTasks.size();
        for (int i = 0; i < recentsCount; i++) {
            Task task = this.mTasks.get(i);
            if (task.mTaskId == id) {
                return task;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void add(Task task) {
        int taskIndex;
        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
            Slog.d(TAG, "add: task=" + task);
        }
        boolean isAffiliated = (task.mAffiliatedTaskId == task.mTaskId && task.mNextAffiliateTaskId == -1 && task.mPrevAffiliateTaskId == -1) ? false : true;
        if (task.inFreeformWindowingMode()) {
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: not adding freeform task " + task);
                return;
            }
            return;
        }
        int recentsCount = this.mTasks.size();
        if (task.voiceSession != null) {
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: not adding voice interaction " + task);
            }
        } else if (!isAffiliated && recentsCount > 0 && this.mTasks.get(0) == task) {
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: already at top: " + task);
            }
        } else if (isAffiliated && recentsCount > 0 && task.inRecents && task.mAffiliatedTaskId == this.mTasks.get(0).mAffiliatedTaskId) {
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: affiliated " + this.mTasks.get(0) + " at top when adding " + task);
            }
        } else {
            boolean needAffiliationFix = false;
            if (task.inRecents) {
                int taskIndex2 = this.mTasks.indexOf(task);
                if (taskIndex2 >= 0) {
                    if (!isAffiliated) {
                        if (!this.mFreezeTaskListReordering) {
                            this.mTasks.remove(taskIndex2);
                            this.mTasks.add(0, task);
                            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                                Slog.d(TAG_RECENTS, "addRecent: moving to top " + task + " from " + taskIndex2);
                            }
                        }
                        notifyTaskPersisterLocked(task, false);
                        return;
                    }
                } else {
                    Slog.wtf(TAG, "Task with inRecent not in recents: " + task);
                    needAffiliationFix = true;
                }
            }
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: trimming tasks for " + task);
            }
            int removedIndex = removeForAddTask(task);
            task.inRecents = true;
            if (!isAffiliated || needAffiliationFix) {
                int indexToAdd = (!this.mFreezeTaskListReordering || removedIndex == -1) ? 0 : removedIndex;
                this.mTasks.add(indexToAdd, task);
                notifyTaskAdded(task);
                if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "addRecent: adding " + task);
                }
            } else if (isAffiliated) {
                Task other = task.mNextAffiliate;
                if (other == null) {
                    other = task.mPrevAffiliate;
                }
                if (other != null) {
                    int otherIndex = this.mTasks.indexOf(other);
                    if (otherIndex >= 0) {
                        if (other == task.mNextAffiliate) {
                            taskIndex = otherIndex + 1;
                        } else {
                            taskIndex = otherIndex;
                        }
                        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                            Slog.d(TAG_RECENTS, "addRecent: new affiliated task added at " + taskIndex + ": " + task);
                        }
                        this.mTasks.add(taskIndex, task);
                        notifyTaskAdded(task);
                        if (moveAffiliatedTasksToFront(task, taskIndex)) {
                            return;
                        }
                        needAffiliationFix = true;
                    } else {
                        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                            Slog.d(TAG_RECENTS, "addRecent: couldn't find other affiliation " + other);
                        }
                        needAffiliationFix = true;
                    }
                } else {
                    if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "addRecent: adding affiliated task without next/prev:" + task);
                    }
                    needAffiliationFix = true;
                }
            }
            if (needAffiliationFix) {
                if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "addRecent: regrouping affiliations");
                }
                cleanupLocked(task.mUserId);
            }
            this.mCheckTrimmableTasksOnIdle = true;
            notifyTaskPersisterLocked(task, false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean addToBottom(Task task) {
        if (!canAddTaskWithoutTrim(task)) {
            return false;
        }
        add(task);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void remove(Task task) {
        this.mTasks.remove(task);
        notifyTaskRemoved(task, false, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityIdle(ActivityRecord r) {
        if (!this.mHiddenTasks.isEmpty() && r.isActivityTypeHome()) {
            removeUnreachableHiddenTasks(r.getWindowingMode());
        }
        if (this.mCheckTrimmableTasksOnIdle) {
            this.mCheckTrimmableTasksOnIdle = false;
            trimInactiveRecentTasks();
        }
    }

    private void trimInactiveRecentTasks() {
        if (this.mFreezeTaskListReordering) {
            return;
        }
        int recentsCount = this.mTasks.size();
        while (recentsCount > this.mGlobalMaxNumTasks) {
            Task task = this.mTasks.remove(recentsCount - 1);
            notifyTaskRemoved(task, true, false);
            recentsCount--;
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "Trimming over max-recents task=" + task + " max=" + this.mGlobalMaxNumTasks);
            }
        }
        int[] profileUserIds = getCurrentProfileIds();
        this.mTmpQuietProfileUserIds.clear();
        for (int userId : profileUserIds) {
            UserInfo userInfo = getUserInfo(userId);
            if (userInfo != null && userInfo.isManagedProfile() && userInfo.isQuietModeEnabled()) {
                this.mTmpQuietProfileUserIds.put(userId, true);
            }
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "User: " + userInfo + " quiet=" + this.mTmpQuietProfileUserIds.get(userId));
            }
        }
        int numVisibleTasks = 0;
        int i = 0;
        while (i < this.mTasks.size()) {
            Task task2 = this.mTasks.get(i);
            if (isActiveRecentTask(task2, this.mTmpQuietProfileUserIds)) {
                if (!this.mHasVisibleRecentTasks) {
                    i++;
                } else if (!isVisibleRecentTask(task2)) {
                    i++;
                } else {
                    numVisibleTasks++;
                    if (isInVisibleRange(task2, i, numVisibleTasks, false) || !isTrimmable(task2)) {
                        i++;
                    } else if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                        Slog.d(TAG, "Trimming out-of-range visible task=" + task2);
                    }
                }
            } else if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "Trimming inactive task=" + task2);
            }
            this.mTasks.remove(task2);
            notifyTaskRemoved(task2, true, false);
            notifyTaskPersisterLocked(task2, false);
        }
    }

    private boolean isActiveRecentTask(Task task, SparseBooleanArray quietProfileUserIds) {
        Task affiliatedTask;
        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
            Slog.d(TAG, "isActiveRecentTask: task=" + task + " globalMax=" + this.mGlobalMaxNumTasks);
        }
        if (quietProfileUserIds.get(task.mUserId)) {
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "\tisQuietProfileTask=true");
            }
            return false;
        } else if (task.mAffiliatedTaskId != -1 && task.mAffiliatedTaskId != task.mTaskId && (affiliatedTask = getTask(task.mAffiliatedTaskId)) != null && !isActiveRecentTask(affiliatedTask, quietProfileUserIds)) {
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "\taffiliatedWithTask=" + affiliatedTask + " is not active");
            }
            return false;
        } else {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVisibleRecentTask(Task task) {
        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
            Slog.d(TAG, "isVisibleRecentTask: task=" + task + " minVis=" + this.mMinNumVisibleTasks + " maxVis=" + this.mMaxNumVisibleTasks + " sessionDuration=" + this.mActiveTasksSessionDurationMs + " inactiveDuration=" + task.getInactiveDuration() + " activityType=" + task.getActivityType() + " windowingMode=" + task.getWindowingMode() + " isAlwaysOnTopWhenVisible=" + task.isAlwaysOnTopWhenVisible() + " intentFlags=" + task.getBaseIntent().getFlags() + " isEmbedded=" + task.isEmbedded());
        }
        switch (task.getActivityType()) {
            case 2:
            case 3:
            case 5:
                return false;
            case 4:
                if ((task.getBaseIntent().getFlags() & 8388608) == 8388608) {
                    return false;
                }
                break;
        }
        switch (task.getWindowingMode()) {
            case 2:
                return false;
            case 3:
            case 4:
            default:
                if (task == this.mService.getLockTaskController().getRootTask() && !task.isEmbedded()) {
                    return task.getDisplayContent() == null || task.getDisplayContent().canShowTasksInRecents();
                }
                return false;
            case 5:
                if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                    Slog.d(TAG, "Ignore free from task " + task);
                }
                return false;
            case 6:
                if (task.isAlwaysOnTopWhenVisible()) {
                    return false;
                }
                if (task == this.mService.getLockTaskController().getRootTask()) {
                    return false;
                }
                if (task.getDisplayContent() == null) {
                    return true;
                }
        }
    }

    private boolean isInVisibleRange(Task task, int taskIndex, int numVisibleTasks, boolean skipExcludedCheck) {
        if (!skipExcludedCheck) {
            boolean isExcludeFromRecents = (task.getBaseIntent().getFlags() & 8388608) == 8388608;
            if (isExcludeFromRecents) {
                if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                    Slog.d(TAG, "\texcludeFromRecents=true, taskIndex = " + taskIndex + ", isOnHomeDisplay: " + task.isOnHomeDisplay());
                }
                return task.isOnHomeDisplay() && taskIndex == 0;
            }
        }
        int i = this.mMinNumVisibleTasks;
        if ((i < 0 || numVisibleTasks > i) && task.mChildPipActivity == null) {
            int i2 = this.mMaxNumVisibleTasks;
            return i2 >= 0 ? numVisibleTasks <= i2 : this.mActiveTasksSessionDurationMs > 0 && task.getInactiveDuration() <= this.mActiveTasksSessionDurationMs;
        }
        return true;
    }

    protected boolean isTrimmable(Task task) {
        Task rootHomeTask;
        if (task.isAttached()) {
            return task.isOnHomeDisplay() && (rootHomeTask = task.getDisplayArea().getRootHomeTask()) != null && task.compareTo((WindowContainer) rootHomeTask) < 0;
        }
        return true;
    }

    private void removeUnreachableHiddenTasks(int windowingMode) {
        for (int i = this.mHiddenTasks.size() - 1; i >= 0; i--) {
            Task hiddenTask = this.mHiddenTasks.get(i);
            if (!hiddenTask.hasChild() || hiddenTask.inRecents) {
                this.mHiddenTasks.remove(i);
            } else if (hiddenTask.getWindowingMode() == windowingMode && hiddenTask.getTopVisibleActivity() == null) {
                this.mHiddenTasks.remove(i);
                this.mSupervisor.removeTask(hiddenTask, false, false, "remove-hidden-task");
            }
        }
    }

    private int removeForAddTask(Task task) {
        this.mHiddenTasks.remove(task);
        int removeIndex = findRemoveIndexForAddTask(task);
        if (removeIndex == -1) {
            return removeIndex;
        }
        Task removedTask = this.mTasks.remove(removeIndex);
        if (removedTask != task) {
            if (removedTask.hasChild()) {
                Slog.i(TAG, "Add " + removedTask + " to hidden list because adding " + task);
                this.mHiddenTasks.add(removedTask);
            }
            notifyTaskRemoved(removedTask, false, false);
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS) {
                Slog.d(TAG, "Trimming task=" + removedTask + " for addition of task=" + task);
            }
        }
        notifyTaskPersisterLocked(removedTask, false);
        return removeIndex;
    }

    private int findRemoveIndexForAddTask(Task task) {
        int recentsCount = this.mTasks.size();
        Intent intent = task.intent;
        boolean z = true;
        boolean document = intent != null && intent.isDocument();
        int maxRecents = task.maxRecents - 1;
        int i = 0;
        while (i < recentsCount) {
            Task t = this.mTasks.get(i);
            if (task != t) {
                if (hasCompatibleActivityTypeAndWindowingMode(task, t) && task.mUserId == t.mUserId) {
                    Intent trIntent = t.intent;
                    boolean sameAffinity = (task.affinity == null || !task.affinity.equals(t.affinity)) ? false : z;
                    boolean sameIntent = (intent == null || !intent.filterEquals(trIntent)) ? false : z;
                    boolean multiTasksAllowed = false;
                    int flags = intent.getFlags();
                    if ((268959744 & flags) != 0 && (134217728 & flags) != 0) {
                        multiTasksAllowed = true;
                    }
                    boolean trIsDocument = (trIntent == null || !trIntent.isDocument()) ? false : z;
                    boolean bothDocuments = (document && trIsDocument) ? z : false;
                    if (sameAffinity || sameIntent || bothDocuments) {
                        if (bothDocuments) {
                            boolean sameActivity = (task.realActivity == null || t.realActivity == null || !task.realActivity.equals(t.realActivity)) ? false : true;
                            if (!sameActivity) {
                                continue;
                            } else if (maxRecents > 0) {
                                maxRecents--;
                                if (sameIntent && !multiTasksAllowed) {
                                }
                            }
                        } else if (!document && !trIsDocument && !multiTasksAllowed) {
                        }
                    }
                }
                i++;
                z = true;
            }
            return i;
        }
        return -1;
    }

    private int processNextAffiliateChainLocked(int start) {
        Task startTask = this.mTasks.get(start);
        int affiliateId = startTask.mAffiliatedTaskId;
        if (startTask.mTaskId == affiliateId && startTask.mPrevAffiliate == null && startTask.mNextAffiliate == null) {
            startTask.inRecents = true;
            return start + 1;
        }
        this.mTmpRecents.clear();
        for (int i = this.mTasks.size() - 1; i >= start; i--) {
            Task task = this.mTasks.get(i);
            if (task.mAffiliatedTaskId == affiliateId) {
                this.mTasks.remove(i);
                this.mTmpRecents.add(task);
            }
        }
        Collections.sort(this.mTmpRecents, TASK_ID_COMPARATOR);
        Task first = this.mTmpRecents.get(0);
        first.inRecents = true;
        if (first.mNextAffiliate != null) {
            Slog.w(TAG, "Link error 1 first.next=" + first.mNextAffiliate);
            first.setNextAffiliate(null);
            notifyTaskPersisterLocked(first, false);
        }
        int tmpSize = this.mTmpRecents.size();
        for (int i2 = 0; i2 < tmpSize - 1; i2++) {
            Task next = this.mTmpRecents.get(i2);
            Task prev = this.mTmpRecents.get(i2 + 1);
            if (next.mPrevAffiliate != prev) {
                Slog.w(TAG, "Link error 2 next=" + next + " prev=" + next.mPrevAffiliate + " setting prev=" + prev);
                next.setPrevAffiliate(prev);
                notifyTaskPersisterLocked(next, false);
            }
            if (prev.mNextAffiliate != next) {
                Slog.w(TAG, "Link error 3 prev=" + prev + " next=" + prev.mNextAffiliate + " setting next=" + next);
                prev.setNextAffiliate(next);
                notifyTaskPersisterLocked(prev, false);
            }
            prev.inRecents = true;
        }
        Task last = this.mTmpRecents.get(tmpSize - 1);
        if (last.mPrevAffiliate != null) {
            Slog.w(TAG, "Link error 4 last.prev=" + last.mPrevAffiliate);
            last.setPrevAffiliate(null);
            notifyTaskPersisterLocked(last, false);
        }
        this.mTasks.addAll(start, this.mTmpRecents);
        this.mTmpRecents.clear();
        return start + tmpSize;
    }

    /* JADX WARN: Code restructure failed: missing block: B:26:0x008d, code lost:
        android.util.Slog.wtf(com.android.server.wm.RecentTasks.TAG, "Bad chain @" + r7 + ": first task has next affiliate: " + r10);
        r6 = false;
     */
    /* JADX WARN: Removed duplicated region for block: B:40:0x0111  */
    /* JADX WARN: Removed duplicated region for block: B:73:0x00c8 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private boolean moveAffiliatedTasksToFront(Task task, int taskIndex) {
        Task cur;
        int recentsCount = this.mTasks.size();
        Task top = task;
        int topIndex = taskIndex;
        while (top.mNextAffiliate != null && topIndex > 0) {
            top = top.mNextAffiliate;
            topIndex--;
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
            Slog.d(TAG_RECENTS, "addRecent: adding affiliates starting at " + topIndex + " from initial " + taskIndex);
        }
        boolean isValid = top.mAffiliatedTaskId == task.mAffiliatedTaskId;
        int endIndex = topIndex;
        Task prev = top;
        while (true) {
            if (endIndex >= recentsCount) {
                break;
            }
            cur = this.mTasks.get(endIndex);
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: looking at next chain @" + endIndex + " " + cur);
            }
            if (cur == top) {
                if (cur.mNextAffiliate != null || cur.mNextAffiliateTaskId != -1) {
                    break;
                }
                Task top2 = top;
                if (cur.mPrevAffiliateTaskId != -1) {
                    if (cur.mPrevAffiliate != null) {
                        Slog.wtf(TAG, "Bad chain @" + endIndex + ": last task " + cur + " has previous affiliate " + cur.mPrevAffiliate);
                        isValid = false;
                    }
                    if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                        Slog.d(TAG_RECENTS, "addRecent: end of chain @" + endIndex);
                    }
                } else if (cur.mPrevAffiliate == null) {
                    Slog.wtf(TAG, "Bad chain @" + endIndex + ": task " + cur + " has previous affiliate " + cur.mPrevAffiliate + " but should be id " + cur.mPrevAffiliate);
                    isValid = false;
                    break;
                } else if (cur.mAffiliatedTaskId != task.mAffiliatedTaskId) {
                    Slog.wtf(TAG, "Bad chain @" + endIndex + ": task " + cur + " has affiliated id " + cur.mAffiliatedTaskId + " but should be " + task.mAffiliatedTaskId);
                    isValid = false;
                    break;
                } else {
                    prev = cur;
                    endIndex++;
                    if (endIndex >= recentsCount) {
                        Slog.wtf(TAG, "Bad chain ran off index " + endIndex + ": last task " + prev);
                        isValid = false;
                        break;
                    }
                    top = top2;
                }
            } else if (cur.mNextAffiliate != prev) {
                break;
            } else {
                if (cur.mNextAffiliateTaskId != prev.mTaskId) {
                    break;
                }
                Task top22 = top;
                if (cur.mPrevAffiliateTaskId != -1) {
                }
            }
        }
        Slog.wtf(TAG, "Bad chain @" + endIndex + ": middle task " + cur + " @" + endIndex + " has bad next affiliate " + cur.mNextAffiliate + " id " + cur.mNextAffiliateTaskId + ", expected " + prev);
        isValid = false;
        if (isValid && endIndex < taskIndex) {
            Slog.wtf(TAG, "Bad chain @" + endIndex + ": did not extend to task " + task + " @" + taskIndex);
            isValid = false;
        }
        if (isValid) {
            for (int i = topIndex; i <= endIndex; i++) {
                if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                    Slog.d(TAG_RECENTS, "addRecent: moving affiliated " + task + " from " + i + " to " + (i - topIndex));
                }
                this.mTasks.add(i - topIndex, this.mTasks.remove(i));
            }
            if (ActivityTaskManagerDebugConfig.DEBUG_RECENTS) {
                Slog.d(TAG_RECENTS, "addRecent: done moving tasks  " + topIndex + " to " + endIndex);
                return true;
            }
            return true;
        }
        return false;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1941=6, 1942=5, 1944=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Code restructure failed: missing block: B:78:0x0189, code lost:
        if (com.transsion.hubcore.server.am.ITranActivityManagerService.Instance().isAppLaunchDebugEnable() == false) goto L79;
     */
    /* JADX WARN: Code restructure failed: missing block: B:79:0x018b, code lost:
        r0 = new java.lang.StringBuilder();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int getTaskIndex(Task task) {
        Intent intent;
        RecentTasks recentTasks = this;
        Task task2 = task;
        if (!ITranActivityManagerService.Instance().isAppLaunchEnable()) {
            return -3;
        }
        if (task2 == null) {
            Slog.e(TAG, "AppLaunchTracker getTaskIndex error: task is null.");
            return -1;
        }
        long start = ITranActivityManagerService.Instance().isAppLaunchDebugEnable() ? System.currentTimeMillis() : 0L;
        if (!recentTasks.mHasVisibleRecentTasks) {
            Slog.d(TAG, "AppLaunchTracker getTaskIndex: no visible recent");
            return -1;
        }
        try {
            try {
                Trace.traceBegin(32L, "getTaskIndex");
                Intent intent2 = task2.intent;
                ComponentName componentName = intent2 != null ? intent2.getComponent() : null;
                String pkg = componentName != null ? componentName.getPackageName() : null;
                if (pkg != null) {
                    ArrayList<Task> arrayList = recentTasks.mTasks;
                    int recentsCount = arrayList != null ? arrayList.size() : 0;
                    int numVisibleTasks = 0;
                    int i = 0;
                    while (true) {
                        if (i >= recentsCount) {
                            break;
                        }
                        Task item = recentTasks.mTasks.get(i);
                        if (ITranActivityManagerService.Instance().isAppLaunchDebugEnable()) {
                            Slog.d(TAG, "AppLaunchTracker getTaskIndex item=,index=" + i + ",isVisibleRecentTask=" + recentTasks.isVisibleRecentTask(item) + ",isInVisibleRange=" + recentTasks.isInVisibleRange(item, i, numVisibleTasks, true) + ",userId=" + item.mUserId + ",isSameUser=" + (item.mUserId == task2.mUserId) + ",item=" + item);
                        }
                        if (item.mUserId != task2.mUserId) {
                            intent = intent2;
                        } else if (recentTasks.isVisibleRecentTask(item)) {
                            numVisibleTasks++;
                            if (!recentTasks.isInVisibleRange(item, i, numVisibleTasks, true)) {
                                Slog.d(TAG, "AppLaunchTracker getTaskIndex item is not in visible range");
                                break;
                            }
                            Intent itemItent = item.intent;
                            ComponentName itemComponentName = itemItent != null ? itemItent.getComponent() : null;
                            String itemPkg = itemComponentName != null ? itemComponentName.getPackageName() : null;
                            if (pkg.equals(itemPkg)) {
                                int i2 = numVisibleTasks + (-1) >= 0 ? numVisibleTasks - 1 : -1;
                                if (ITranActivityManagerService.Instance().isAppLaunchDebugEnable()) {
                                    Slog.d(TAG, "AppLaunchTracker getTaskIndex consume=" + (System.currentTimeMillis() - start));
                                }
                                Trace.traceEnd(32L);
                                return i2;
                            }
                            intent = intent2;
                        } else {
                            intent = intent2;
                        }
                        i++;
                        recentTasks = this;
                        task2 = task;
                        intent2 = intent;
                    }
                } else {
                    Slog.e(TAG, "AppLaunchTracker getTaskIndex error: param task pkg is null.");
                    if (ITranActivityManagerService.Instance().isAppLaunchDebugEnable()) {
                        Slog.d(TAG, "AppLaunchTracker getTaskIndex consume=" + (System.currentTimeMillis() - start));
                    }
                    Trace.traceEnd(32L);
                    return -1;
                }
            } catch (Exception e) {
                Slog.e(TAG, "AppLaunchTracker getTaskIndex error=" + e.getMessage());
                if (ITranActivityManagerService.Instance().isAppLaunchDebugEnable()) {
                    StringBuilder sb = new StringBuilder();
                    Slog.d(TAG, sb.append("AppLaunchTracker getTaskIndex consume=").append(System.currentTimeMillis() - start).toString());
                }
                Trace.traceEnd(32L);
                return -1;
            }
        } catch (Throwable th) {
            if (ITranActivityManagerService.Instance().isAppLaunchDebugEnable()) {
                Slog.d(TAG, "AppLaunchTracker getTaskIndex consume=" + (System.currentTimeMillis() - start));
            }
            Trace.traceEnd(32L);
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, boolean dumpAll, String dumpPackage) {
        int i;
        int i2;
        pw.println("ACTIVITY MANAGER RECENT TASKS (dumpsys activity recents)");
        pw.println("mRecentsUid=" + this.mRecentsUid);
        pw.println("mRecentsComponent=" + this.mRecentsComponent);
        pw.println("mFreezeTaskListReordering=" + this.mFreezeTaskListReordering);
        pw.println("mFreezeTaskListReorderingPendingTimeout=" + this.mService.mH.hasCallbacks(this.mResetFreezeTaskListOnTimeoutRunnable));
        if (!this.mHiddenTasks.isEmpty()) {
            pw.println("mHiddenTasks=" + this.mHiddenTasks);
        }
        if (this.mTasks.isEmpty()) {
            return;
        }
        boolean printedHeader = false;
        int size = this.mTasks.size();
        boolean printedAnything = false;
        while (true) {
            boolean z = false;
            if (i >= size) {
                break;
            }
            Task task = this.mTasks.get(i);
            if (dumpPackage != null) {
                boolean match = (task.intent == null || task.intent.getComponent() == null || !dumpPackage.equals(task.intent.getComponent().getPackageName())) ? false : true;
                if (!match) {
                    match |= (task.affinityIntent == null || task.affinityIntent.getComponent() == null || !dumpPackage.equals(task.affinityIntent.getComponent().getPackageName())) ? false : true;
                }
                if (!match) {
                    match |= task.origActivity != null && dumpPackage.equals(task.origActivity.getPackageName());
                }
                if (!match) {
                    if (task.realActivity != null && dumpPackage.equals(task.realActivity.getPackageName())) {
                        z = true;
                    }
                    match |= z;
                }
                if (!match) {
                    match |= dumpPackage.equals(task.mCallingPackage);
                }
                i = match ? 0 : i + 1;
            }
            if (!printedHeader) {
                pw.println("  Recent tasks:");
                printedHeader = true;
                printedAnything = true;
            }
            pw.print("  * Recent #");
            pw.print(i);
            pw.print(": ");
            pw.println(task);
            if (dumpAll) {
                task.dump(pw, "    ");
            }
        }
        if (this.mHasVisibleRecentTasks) {
            boolean printedHeader2 = false;
            ArrayList<ActivityManager.RecentTaskInfo> tasks = getRecentTasksImpl(Integer.MAX_VALUE, 0, true, this.mService.getCurrentUserId(), 1000);
            while (i2 < tasks.size()) {
                ActivityManager.RecentTaskInfo taskInfo = tasks.get(i2);
                if (dumpPackage != null) {
                    boolean match2 = (taskInfo.baseIntent == null || taskInfo.baseIntent.getComponent() == null || !dumpPackage.equals(taskInfo.baseIntent.getComponent().getPackageName())) ? false : true;
                    if (!match2) {
                        match2 |= taskInfo.baseActivity != null && dumpPackage.equals(taskInfo.baseActivity.getPackageName());
                    }
                    if (!match2) {
                        match2 |= taskInfo.topActivity != null && dumpPackage.equals(taskInfo.topActivity.getPackageName());
                    }
                    if (!match2) {
                        match2 |= taskInfo.origActivity != null && dumpPackage.equals(taskInfo.origActivity.getPackageName());
                    }
                    if (!match2) {
                        match2 |= taskInfo.realActivity != null && dumpPackage.equals(taskInfo.realActivity.getPackageName());
                    }
                    i2 = match2 ? 0 : i2 + 1;
                }
                if (!printedHeader2) {
                    if (printedAnything) {
                        pw.println();
                    }
                    pw.println("  Visible recent tasks (most recent first):");
                    printedHeader2 = true;
                    printedAnything = true;
                }
                pw.print("  * RecentTaskInfo #");
                pw.print(i2);
                pw.print(": ");
                taskInfo.dump(pw, "    ");
            }
        }
        if (!printedAnything) {
            pw.println("  (nothing)");
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.wm.Task */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    public ActivityManager.RecentTaskInfo createRecentTaskInfo(Task tr, boolean stripExtras, boolean getTasksAllowed) {
        TaskDisplayArea tda;
        ActivityManager.RecentTaskInfo rti = new ActivityManager.RecentTaskInfo();
        if (tr.isAttached()) {
            tda = tr.getDisplayArea();
        } else {
            tda = this.mService.mRootWindowContainer.getDefaultTaskDisplayArea();
        }
        tr.fillTaskInfo(rti, stripExtras, tda);
        rti.id = rti.isRunning ? rti.taskId : -1;
        rti.persistentId = rti.taskId;
        rti.lastSnapshotData.set(tr.mLastTaskSnapshotData);
        if (!getTasksAllowed) {
            Task.trimIneffectiveInfo(tr, rti);
        }
        if (tr.mCreatedByOrganizer) {
            for (int i = tr.getChildCount() - 1; i >= 0; i--) {
                Task childTask = tr.getChildAt(i).asTask();
                if (childTask != null && childTask.isOrganized()) {
                    ActivityManager.RecentTaskInfo cti = new ActivityManager.RecentTaskInfo();
                    childTask.fillTaskInfo(cti, true, tda);
                    rti.childrenTaskInfos.add(cti);
                }
            }
        }
        return rti;
    }

    private boolean hasCompatibleActivityTypeAndWindowingMode(Task t1, Task t2) {
        int activityType = t1.getActivityType();
        int windowingMode = t1.getWindowingMode();
        boolean isUndefinedType = activityType == 0;
        boolean isUndefinedMode = windowingMode == 0;
        int otherActivityType = t2.getActivityType();
        int otherWindowingMode = t2.getWindowingMode();
        boolean isOtherUndefinedType = otherActivityType == 0;
        boolean isOtherUndefinedMode = otherWindowingMode == 0;
        boolean isCompatibleType = activityType == otherActivityType || isUndefinedType || isOtherUndefinedType;
        boolean freeFormPresent = windowingMode == 5 || otherWindowingMode == 5;
        boolean isCompatibleMode = windowingMode == otherWindowingMode || isUndefinedMode || isOtherUndefinedMode || freeFormPresent;
        return isCompatibleType && isCompatibleMode;
    }

    private boolean isForceInvisibleRecentTask(Task task) {
        ComponentName activity = task.getBaseIntent().getComponent();
        if (activity != null && "com.transsion.minilauncher.MainActivity".equals(activity.getClassName()) && "com.transsion.ossettingsext".equals(activity.getPackageName())) {
            return true;
        }
        return false;
    }
}
