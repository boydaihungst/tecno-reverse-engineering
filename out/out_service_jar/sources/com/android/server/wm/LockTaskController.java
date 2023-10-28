package com.android.server.wm;

import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telephony.CellBroadcastUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.LockTaskController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class LockTaskController {
    static final int LOCK_TASK_AUTH_ALLOWLISTED = 3;
    static final int LOCK_TASK_AUTH_DONT_LOCK = 0;
    static final int LOCK_TASK_AUTH_LAUNCHABLE = 2;
    static final int LOCK_TASK_AUTH_LAUNCHABLE_PRIV = 4;
    static final int LOCK_TASK_AUTH_PINNABLE = 1;
    private static final String LOCK_TASK_TAG = "Lock-to-App";
    private static final SparseArray<Pair<Integer, Integer>> STATUS_BAR_FLAG_MAP_LOCKED;
    static final int STATUS_BAR_MASK_LOCKED = 128319488;
    static final int STATUS_BAR_MASK_PINNED = 111083520;
    private static final String TAG = "ActivityTaskManager";
    private static final String TAG_LOCKTASK = TAG + ActivityTaskManagerDebugConfig.POSTFIX_LOCKTASK;
    private final Context mContext;
    IDevicePolicyManager mDevicePolicyManager;
    private final Handler mHandler;
    LockPatternUtils mLockPatternUtils;
    IStatusBarService mStatusBarService;
    private final ActivityTaskSupervisor mSupervisor;
    private final TaskChangeNotificationController mTaskChangeNotificationController;
    TelecomManager mTelecomManager;
    WindowManagerService mWindowManager;
    private final IBinder mToken = new LockTaskToken();
    private final ArrayList<Task> mLockTaskModeTasks = new ArrayList<>();
    private final SparseArray<String[]> mLockTaskPackages = new SparseArray<>();
    private final SparseIntArray mLockTaskFeatures = new SparseIntArray();
    private volatile int mLockTaskModeState = 0;
    private int mPendingDisableFromDismiss = -10000;

    static {
        SparseArray<Pair<Integer, Integer>> sparseArray = new SparseArray<>();
        STATUS_BAR_FLAG_MAP_LOCKED = sparseArray;
        sparseArray.append(1, new Pair<>(8388608, 2));
        sparseArray.append(2, new Pair<>(393216, 4));
        sparseArray.append(4, new Pair<>(2097152, 0));
        sparseArray.append(8, new Pair<>(16777216, 0));
        sparseArray.append(16, new Pair<>(0, 8));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LockTaskController(Context context, ActivityTaskSupervisor supervisor, Handler handler, TaskChangeNotificationController taskChangeNotificationController) {
        this.mContext = context;
        this.mSupervisor = supervisor;
        this.mHandler = handler;
        this.mTaskChangeNotificationController = taskChangeNotificationController;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setWindowManager(WindowManagerService windowManager) {
        this.mWindowManager = windowManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLockTaskModeState() {
        return this.mLockTaskModeState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTaskLocked(Task task) {
        return this.mLockTaskModeTasks.contains(task);
    }

    private boolean isRootTask(Task task) {
        return this.mLockTaskModeTasks.indexOf(task) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean activityBlockedFromFinish(final ActivityRecord activity) {
        Task task = activity.getTask();
        if (task.mLockTaskAuth == 4 || !isRootTask(task)) {
            return false;
        }
        ActivityRecord taskTop = task.getTopNonFinishingActivity();
        ActivityRecord taskRoot = task.getRootActivity();
        if (activity != taskRoot || activity != taskTop) {
            TaskFragment taskFragment = activity.getTaskFragment();
            final TaskFragment adjacentTaskFragment = taskFragment.getAdjacentTaskFragment();
            if (taskFragment.asTask() != null || !taskFragment.isDelayLastActivityRemoval() || adjacentTaskFragment == null) {
                return false;
            }
            boolean hasOtherActivityInTaskFragment = taskFragment.getActivity(new Predicate() { // from class: com.android.server.wm.LockTaskController$$ExternalSyntheticLambda3
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return LockTaskController.lambda$activityBlockedFromFinish$0(ActivityRecord.this, (ActivityRecord) obj);
                }
            }) != null;
            if (hasOtherActivityInTaskFragment) {
                return false;
            }
            boolean hasOtherActivityInTask = task.getActivity(new Predicate() { // from class: com.android.server.wm.LockTaskController$$ExternalSyntheticLambda4
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return LockTaskController.lambda$activityBlockedFromFinish$1(ActivityRecord.this, adjacentTaskFragment, (ActivityRecord) obj);
                }
            }) != null;
            if (hasOtherActivityInTask) {
                return false;
            }
        }
        Slog.i(TAG, "Not finishing task in lock task mode");
        showLockTaskToast();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$activityBlockedFromFinish$0(ActivityRecord activity, ActivityRecord a) {
        return (a.finishing || a == activity) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$activityBlockedFromFinish$1(ActivityRecord activity, TaskFragment adjacentTaskFragment, ActivityRecord a) {
        return (a.finishing || a == activity || a.getTaskFragment() == adjacentTaskFragment) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canMoveTaskToBack(Task task) {
        if (isRootTask(task)) {
            showLockTaskToast();
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isTaskAuthAllowlisted(int lockTaskAuth) {
        switch (lockTaskAuth) {
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLockTaskModeViolation(Task task) {
        return isLockTaskModeViolation(task, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLockTaskModeViolation(Task task, boolean isNewClearTask) {
        if ((!isTaskLocked(task) || isNewClearTask) && isLockTaskModeViolationInternal(task, task.mUserId, task.intent, task.mLockTaskAuth)) {
            if (task.intent != null) {
                showLockTaskToast();
                return true;
            }
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNewTaskLockTaskModeViolation(ActivityRecord activity) {
        if (activity.getTask() != null) {
            return isLockTaskModeViolation(activity.getTask());
        }
        int auth = getLockTaskAuth(activity, null);
        if (isLockTaskModeViolationInternal(activity, activity.mUserId, activity.intent, auth)) {
            showLockTaskToast();
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Task getRootTask() {
        if (this.mLockTaskModeTasks.isEmpty()) {
            return null;
        }
        return this.mLockTaskModeTasks.get(0);
    }

    private boolean isLockTaskModeViolationInternal(WindowContainer wc, int userId, Intent intent, int taskAuth) {
        if (wc.isActivityTypeRecents() && isRecentsAllowed(userId)) {
            return false;
        }
        return ((isKeyguardAllowed(userId) && isEmergencyCallIntent(intent)) || wc.isActivityTypeDream() || isWirelessEmergencyAlert(intent) || isTaskAuthAllowlisted(taskAuth) || this.mLockTaskModeTasks.isEmpty()) ? false : true;
    }

    private boolean isRecentsAllowed(int userId) {
        return (getLockTaskFeaturesForUser(userId) & 8) != 0;
    }

    private boolean isKeyguardAllowed(int userId) {
        return (getLockTaskFeaturesForUser(userId) & 32) != 0;
    }

    private boolean isBlockingInTaskEnabled(int userId) {
        return (getLockTaskFeaturesForUser(userId) & 64) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isActivityAllowed(int userId, String packageName, int lockTaskLaunchMode) {
        if (this.mLockTaskModeState == 1 && isBlockingInTaskEnabled(userId)) {
            switch (lockTaskLaunchMode) {
                case 1:
                    return false;
                case 2:
                    return true;
                default:
                    return isPackageAllowlisted(userId, packageName);
            }
        }
        return true;
    }

    private boolean isWirelessEmergencyAlert(Intent intent) {
        ComponentName cellBroadcastAlertDialogComponentName;
        if (intent == null || (cellBroadcastAlertDialogComponentName = CellBroadcastUtils.getDefaultCellBroadcastAlertDialogComponent(this.mContext)) == null || !cellBroadcastAlertDialogComponentName.equals(intent.getComponent())) {
            return false;
        }
        return true;
    }

    private boolean isEmergencyCallIntent(Intent intent) {
        if (intent == null) {
            return false;
        }
        if (TelecomManager.EMERGENCY_DIALER_COMPONENT.equals(intent.getComponent()) || "android.intent.action.CALL_EMERGENCY".equals(intent.getAction())) {
            return true;
        }
        TelecomManager tm = getTelecomManager();
        String dialerPackage = tm != null ? tm.getSystemDialerPackage() : null;
        return dialerPackage != null && dialerPackage.equals(intent.getComponent().getPackageName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopLockTaskMode(Task task, boolean isSystemCaller, int callingUid) {
        if (this.mLockTaskModeState == 0) {
            return;
        }
        if (isSystemCaller) {
            if (this.mLockTaskModeState == 2) {
                clearLockedTasks("stopAppPinning");
                return;
            }
            Slog.e(TAG_LOCKTASK, "Attempted to stop LockTask with isSystemCaller=true");
            showLockTaskToast();
        } else if (task == null) {
            throw new IllegalArgumentException("can't stop LockTask for null task");
        } else {
            if (callingUid != task.mLockTaskUid && (task.mLockTaskUid != 0 || callingUid != task.effectiveUid)) {
                throw new SecurityException("Invalid uid, expected " + task.mLockTaskUid + " callingUid=" + callingUid + " effectiveUid=" + task.effectiveUid);
            }
            clearLockedTask(task);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearLockedTasks(String reason) {
        if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
            String protoLogParam0 = String.valueOf(reason);
            ProtoLogImpl.i(ProtoLogGroup.WM_DEBUG_LOCKTASK, -317194205, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (!this.mLockTaskModeTasks.isEmpty()) {
            clearLockedTask(this.mLockTaskModeTasks.get(0));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearLockedTask(Task task) {
        if (task == null || this.mLockTaskModeTasks.isEmpty()) {
            return;
        }
        if (task == this.mLockTaskModeTasks.get(0)) {
            for (int taskNdx = this.mLockTaskModeTasks.size() - 1; taskNdx > 0; taskNdx--) {
                clearLockedTask(this.mLockTaskModeTasks.get(taskNdx));
            }
        }
        removeLockedTask(task);
        if (this.mLockTaskModeTasks.isEmpty()) {
            return;
        }
        task.performClearTaskForReuse(false);
        this.mSupervisor.mRootWindowContainer.resumeFocusedTasksTopActivities();
    }

    private void removeLockedTask(final Task task) {
        if (!this.mLockTaskModeTasks.remove(task)) {
            return;
        }
        if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
            String protoLogParam0 = String.valueOf(task);
            ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_LOCKTASK, -1630752478, 0, (String) null, new Object[]{protoLogParam0});
        }
        if (this.mLockTaskModeTasks.isEmpty()) {
            if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
                String protoLogParam02 = String.valueOf(task);
                String protoLogParam1 = String.valueOf(Debug.getCallers(3));
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_LOCKTASK, 956374481, 0, (String) null, new Object[]{protoLogParam02, protoLogParam1});
            }
            this.mHandler.post(new Runnable() { // from class: com.android.server.wm.LockTaskController$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    LockTaskController.this.m8115x6875f0a9(task);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeLockedTask$2$com-android-server-wm-LockTaskController  reason: not valid java name */
    public /* synthetic */ void m8115x6875f0a9(Task task) {
        performStopLockTask(task.mUserId);
    }

    private void performStopLockTask(int userId) {
        int oldLockTaskModeState = this.mLockTaskModeState;
        this.mLockTaskModeState = 0;
        this.mTaskChangeNotificationController.notifyLockTaskModeChanged(this.mLockTaskModeState);
        try {
            setStatusBarState(this.mLockTaskModeState, userId);
            setKeyguardState(this.mLockTaskModeState, userId);
            if (oldLockTaskModeState == 2) {
                lockKeyguardIfNeeded(userId);
            }
            if (getDevicePolicyManager() != null) {
                getDevicePolicyManager().notifyLockTaskModeChanged(false, (String) null, userId);
            }
            if (oldLockTaskModeState == 2) {
                getStatusBarService().showPinningEnterExitToast(false);
            }
            this.mWindowManager.onLockTaskStateChanged(this.mLockTaskModeState);
        } catch (RemoteException ex) {
            throw new RuntimeException(ex);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showLockTaskToast() {
        if (this.mLockTaskModeState == 2) {
            try {
                getStatusBarService().showPinningEscapeToast();
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to send pinning escape toast", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startLockTaskMode(Task task, boolean isSystemCaller, int callingUid) {
        if (!isSystemCaller) {
            task.mLockTaskUid = callingUid;
            if (task.mLockTaskAuth == 1) {
                if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
                    ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_LOCKTASK, 1401295262, 0, (String) null, (Object[]) null);
                }
                StatusBarManagerInternal statusBarManager = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
                if (statusBarManager != null) {
                    statusBarManager.showScreenPinningRequest(task.mTaskId);
                    return;
                }
                return;
            }
        }
        if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
            String protoLogParam0 = String.valueOf(isSystemCaller ? "Locking pinned" : "Locking fully");
            ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_LOCKTASK, -2121056984, 0, (String) null, new Object[]{protoLogParam0});
        }
        setLockTaskMode(task, isSystemCaller ? 2 : 1, "startLockTask", true);
    }

    private void setLockTaskMode(final Task task, final int lockTaskModeState, String reason, boolean andResume) {
        if (task.mLockTaskAuth == 0) {
            if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_LOCKTASK, 950074526, 0, (String) null, (Object[]) null);
            }
        } else if (isLockTaskModeViolation(task)) {
            Slog.e(TAG_LOCKTASK, "setLockTaskMode: Attempt to start an unauthorized lock task.");
        } else {
            final Intent taskIntent = task.intent;
            if (this.mLockTaskModeTasks.isEmpty() && taskIntent != null) {
                this.mSupervisor.mRecentTasks.onLockTaskModeStateChanged(lockTaskModeState, task.mUserId);
                this.mHandler.post(new Runnable() { // from class: com.android.server.wm.LockTaskController$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        LockTaskController.this.m8116x1e1b01a0(taskIntent, task, lockTaskModeState);
                    }
                });
            }
            if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
                String protoLogParam0 = String.valueOf(task);
                String protoLogParam1 = String.valueOf(Debug.getCallers(4));
                ProtoLogImpl.w(ProtoLogGroup.WM_DEBUG_LOCKTASK, -548282316, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
            }
            if (!this.mLockTaskModeTasks.contains(task)) {
                this.mLockTaskModeTasks.add(task);
            }
            if (task.mLockTaskUid == -1) {
                task.mLockTaskUid = task.effectiveUid;
            }
            if (andResume) {
                this.mSupervisor.findTaskToMoveToFront(task, 0, null, reason, lockTaskModeState != 0);
                this.mSupervisor.mRootWindowContainer.resumeFocusedTasksTopActivities();
                Task rootTask = task.getRootTask();
                if (rootTask != null) {
                    rootTask.mDisplayContent.executeAppTransition();
                }
            } else if (lockTaskModeState != 0) {
                ActivityTaskSupervisor activityTaskSupervisor = this.mSupervisor;
                activityTaskSupervisor.handleNonResizableTaskIfNeeded(task, 0, activityTaskSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea(), task.getRootTask(), true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setLockTaskMode$3$com-android-server-wm-LockTaskController  reason: not valid java name */
    public /* synthetic */ void m8116x1e1b01a0(Intent taskIntent, Task task, int lockTaskModeState) {
        performStartLockTask(taskIntent.getComponent().getPackageName(), task.mUserId, lockTaskModeState);
    }

    private void performStartLockTask(String packageName, int userId, int lockTaskModeState) {
        if (lockTaskModeState == 2) {
            try {
                getStatusBarService().showPinningEnterExitToast(true);
            } catch (RemoteException ex) {
                throw new RuntimeException(ex);
            }
        }
        this.mWindowManager.onLockTaskStateChanged(lockTaskModeState);
        this.mLockTaskModeState = lockTaskModeState;
        this.mTaskChangeNotificationController.notifyLockTaskModeChanged(this.mLockTaskModeState);
        setStatusBarState(lockTaskModeState, userId);
        setKeyguardState(lockTaskModeState, userId);
        if (getDevicePolicyManager() != null) {
            getDevicePolicyManager().notifyLockTaskModeChanged(true, packageName, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLockTaskPackages(int userId, String[] packages) {
        this.mLockTaskPackages.put(userId, packages);
        boolean taskChanged = false;
        for (int taskNdx = this.mLockTaskModeTasks.size() - 1; taskNdx >= 0; taskNdx--) {
            Task lockedTask = this.mLockTaskModeTasks.get(taskNdx);
            boolean wasAllowlisted = lockedTask.mLockTaskAuth == 2 || lockedTask.mLockTaskAuth == 3;
            lockedTask.setLockTaskAuth();
            boolean isAllowlisted = lockedTask.mLockTaskAuth == 2 || lockedTask.mLockTaskAuth == 3;
            if (this.mLockTaskModeState == 1 && lockedTask.mUserId == userId && wasAllowlisted && !isAllowlisted) {
                if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
                    String protoLogParam0 = String.valueOf(lockedTask);
                    String protoLogParam1 = String.valueOf(lockedTask.lockTaskAuthToString());
                    ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_LOCKTASK, 1829094918, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
                }
                removeLockedTask(lockedTask);
                lockedTask.performClearTaskForReuse(false);
                taskChanged = true;
            }
        }
        this.mSupervisor.mRootWindowContainer.forAllTasks(new Consumer() { // from class: com.android.server.wm.LockTaskController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((Task) obj).setLockTaskAuth();
            }
        });
        ActivityRecord r = this.mSupervisor.mRootWindowContainer.topRunningActivity();
        Task task = r != null ? r.getTask() : null;
        if (this.mLockTaskModeTasks.isEmpty() && task != null && task.mLockTaskAuth == 2) {
            if (ProtoLogCache.WM_DEBUG_LOCKTASK_enabled) {
                String protoLogParam02 = String.valueOf(task);
                ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_LOCKTASK, 1088929964, 0, (String) null, new Object[]{protoLogParam02});
            }
            setLockTaskMode(task, 1, "package updated", false);
            taskChanged = true;
        }
        if (taskChanged) {
            this.mSupervisor.mRootWindowContainer.resumeFocusedTasksTopActivities();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getLockTaskAuth(ActivityRecord rootActivity, Task task) {
        String pkg;
        if (rootActivity == null && task == null) {
            return 0;
        }
        int i = 1;
        if (rootActivity == null) {
            return 1;
        }
        if (task == null || task.realActivity == null) {
            pkg = rootActivity.packageName;
        } else {
            pkg = task.realActivity.getPackageName();
        }
        int userId = task != null ? task.mUserId : rootActivity.mUserId;
        switch (rootActivity.lockTaskLaunchMode) {
            case 0:
                if (isPackageAllowlisted(userId, pkg)) {
                    i = 3;
                }
                int lockTaskAuth = i;
                return lockTaskAuth;
            case 1:
                return 0;
            case 2:
                return 4;
            case 3:
                if (isPackageAllowlisted(userId, pkg)) {
                    i = 2;
                }
                int lockTaskAuth2 = i;
                return lockTaskAuth2;
            default:
                return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPackageAllowlisted(int userId, String pkg) {
        String[] allowlist;
        if (pkg == null || (allowlist = this.mLockTaskPackages.get(userId)) == null) {
            return false;
        }
        for (String allowlistedPkg : allowlist) {
            if (pkg.equals(allowlistedPkg)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLockTaskFeatures(final int userId, int flags) {
        int oldFlags = getLockTaskFeaturesForUser(userId);
        if (flags == oldFlags) {
            return;
        }
        this.mLockTaskFeatures.put(userId, flags);
        if (!this.mLockTaskModeTasks.isEmpty() && userId == this.mLockTaskModeTasks.get(0).mUserId) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.wm.LockTaskController$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    LockTaskController.this.m8117x89cd2b84(userId);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateLockTaskFeatures$4$com-android-server-wm-LockTaskController  reason: not valid java name */
    public /* synthetic */ void m8117x89cd2b84(int userId) {
        if (this.mLockTaskModeState == 1) {
            setStatusBarState(this.mLockTaskModeState, userId);
            setKeyguardState(this.mLockTaskModeState, userId);
        }
    }

    private void setStatusBarState(int lockTaskModeState, int userId) {
        IStatusBarService statusBar = getStatusBarService();
        if (statusBar == null) {
            Slog.e(TAG, "Can't find StatusBarService");
            return;
        }
        int flags1 = 0;
        int flags2 = 0;
        if (lockTaskModeState == 2) {
            flags1 = STATUS_BAR_MASK_PINNED;
        } else if (lockTaskModeState == 1) {
            int lockTaskFeatures = getLockTaskFeaturesForUser(userId);
            Pair<Integer, Integer> statusBarFlags = getStatusBarDisableFlags(lockTaskFeatures);
            flags1 = ((Integer) statusBarFlags.first).intValue();
            flags2 = ((Integer) statusBarFlags.second).intValue();
        }
        try {
            statusBar.disable(flags1, this.mToken, this.mContext.getPackageName());
            statusBar.disable2(flags2, this.mToken, this.mContext.getPackageName());
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to set status bar flags", e);
        }
    }

    private void setKeyguardState(int lockTaskModeState, int userId) {
        this.mPendingDisableFromDismiss = -10000;
        if (lockTaskModeState == 0) {
            this.mWindowManager.reenableKeyguard(this.mToken, userId);
        } else if (lockTaskModeState == 1) {
            if (isKeyguardAllowed(userId)) {
                this.mWindowManager.reenableKeyguard(this.mToken, userId);
            } else if (this.mWindowManager.isKeyguardLocked() && !this.mWindowManager.isKeyguardSecure(userId)) {
                this.mPendingDisableFromDismiss = userId;
                this.mWindowManager.dismissKeyguard(new AnonymousClass1(userId), null);
            } else {
                this.mWindowManager.disableKeyguard(this.mToken, LOCK_TASK_TAG, userId);
            }
        } else {
            this.mWindowManager.disableKeyguard(this.mToken, LOCK_TASK_TAG, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.wm.LockTaskController$1  reason: invalid class name */
    /* loaded from: classes2.dex */
    public class AnonymousClass1 extends IKeyguardDismissCallback.Stub {
        final /* synthetic */ int val$userId;

        AnonymousClass1(int i) {
            this.val$userId = i;
        }

        public void onDismissError() throws RemoteException {
            Slog.i(LockTaskController.TAG, "setKeyguardState: failed to dismiss keyguard");
        }

        public void onDismissSucceeded() throws RemoteException {
            Handler handler = LockTaskController.this.mHandler;
            final int i = this.val$userId;
            handler.post(new Runnable() { // from class: com.android.server.wm.LockTaskController$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    LockTaskController.AnonymousClass1.this.m8118x98fbe4b5(i);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onDismissSucceeded$0$com-android-server-wm-LockTaskController$1  reason: not valid java name */
        public /* synthetic */ void m8118x98fbe4b5(int userId) {
            if (LockTaskController.this.mPendingDisableFromDismiss == userId) {
                LockTaskController.this.mWindowManager.disableKeyguard(LockTaskController.this.mToken, LockTaskController.LOCK_TASK_TAG, userId);
                LockTaskController.this.mPendingDisableFromDismiss = -10000;
            }
        }

        public void onDismissCancelled() throws RemoteException {
            Slog.i(LockTaskController.TAG, "setKeyguardState: dismiss cancelled");
        }
    }

    private void lockKeyguardIfNeeded(int userId) {
        if (shouldLockKeyguard(userId)) {
            this.mWindowManager.lockNow(null);
            this.mWindowManager.dismissKeyguard(null, null);
            getLockPatternUtils().requireCredentialEntry(-1);
        }
    }

    private boolean shouldLockKeyguard(int userId) {
        try {
            return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_to_app_exit_locked", -2) != 0;
        } catch (Settings.SettingNotFoundException e) {
            EventLog.writeEvent(1397638484, "127605586", -1, "");
            return getLockPatternUtils().isSecure(userId);
        }
    }

    Pair<Integer, Integer> getStatusBarDisableFlags(int lockTaskFlags) {
        int flags1 = 134152192;
        int flags2 = 31;
        for (int i = STATUS_BAR_FLAG_MAP_LOCKED.size() - 1; i >= 0; i--) {
            SparseArray<Pair<Integer, Integer>> sparseArray = STATUS_BAR_FLAG_MAP_LOCKED;
            Pair<Integer, Integer> statusBarFlags = sparseArray.valueAt(i);
            if ((sparseArray.keyAt(i) & lockTaskFlags) != 0) {
                flags1 &= ~((Integer) statusBarFlags.first).intValue();
                flags2 &= ~((Integer) statusBarFlags.second).intValue();
            }
        }
        return new Pair<>(Integer.valueOf(flags1 & STATUS_BAR_MASK_LOCKED), Integer.valueOf(flags2));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBaseOfLockedTask(String packageName) {
        for (int i = 0; i < this.mLockTaskModeTasks.size(); i++) {
            Intent bi = this.mLockTaskModeTasks.get(i).getBaseIntent();
            if (bi != null && packageName.equals(bi.getComponent().getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private int getLockTaskFeaturesForUser(int userId) {
        return this.mLockTaskFeatures.get(userId, 0);
    }

    private IStatusBarService getStatusBarService() {
        if (this.mStatusBarService == null) {
            IStatusBarService asInterface = IStatusBarService.Stub.asInterface(ServiceManager.checkService("statusbar"));
            this.mStatusBarService = asInterface;
            if (asInterface == null) {
                Slog.w("StatusBarManager", "warning: no STATUS_BAR_SERVICE");
            }
        }
        return this.mStatusBarService;
    }

    private IDevicePolicyManager getDevicePolicyManager() {
        if (this.mDevicePolicyManager == null) {
            IDevicePolicyManager asInterface = IDevicePolicyManager.Stub.asInterface(ServiceManager.checkService("device_policy"));
            this.mDevicePolicyManager = asInterface;
            if (asInterface == null) {
                Slog.w(TAG, "warning: no DEVICE_POLICY_SERVICE");
            }
        }
        return this.mDevicePolicyManager;
    }

    private LockPatternUtils getLockPatternUtils() {
        LockPatternUtils lockPatternUtils = this.mLockPatternUtils;
        if (lockPatternUtils == null) {
            return new LockPatternUtils(this.mContext);
        }
        return lockPatternUtils;
    }

    private TelecomManager getTelecomManager() {
        TelecomManager telecomManager = this.mTelecomManager;
        if (telecomManager == null) {
            return (TelecomManager) this.mContext.getSystemService(TelecomManager.class);
        }
        return telecomManager;
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "LockTaskController:");
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "mLockTaskModeState=" + lockTaskModeToString());
        pw.println(prefix2 + "mLockTaskModeTasks=");
        for (int i = 0; i < this.mLockTaskModeTasks.size(); i++) {
            pw.println(prefix2 + "  #" + i + " " + this.mLockTaskModeTasks.get(i));
        }
        pw.println(prefix2 + "mLockTaskPackages (userId:packages)=");
        for (int i2 = 0; i2 < this.mLockTaskPackages.size(); i2++) {
            pw.println(prefix2 + "  u" + this.mLockTaskPackages.keyAt(i2) + ":" + Arrays.toString(this.mLockTaskPackages.valueAt(i2)));
        }
        pw.println();
    }

    private String lockTaskModeToString() {
        switch (this.mLockTaskModeState) {
            case 0:
                return "NONE";
            case 1:
                return "LOCKED";
            case 2:
                return "PINNED";
            default:
                return "unknown=" + this.mLockTaskModeState;
        }
    }

    /* loaded from: classes2.dex */
    static class LockTaskToken extends Binder {
        private LockTaskToken() {
        }
    }
}
