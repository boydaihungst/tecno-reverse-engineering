package com.android.server.wm;

import android.app.ActivityThread;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.app.servertransaction.ConfigurationChangeItem;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.InputConstants;
import android.os.LocaleList;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.QuadConsumer;
import com.android.internal.util.function.QuintConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.ActivityRecord;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import com.transsion.hubcore.server.wm.ITranWindowProcessController;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class WindowProcessController extends ConfigurationContainer<ConfigurationContainer> implements ConfigurationContainerListener {
    private static final int ACTIVITY_STATE_FLAG_HAS_ACTIVITY_IN_VISIBLE_TASK = 4194304;
    private static final int ACTIVITY_STATE_FLAG_HAS_RESUMED = 2097152;
    private static final int ACTIVITY_STATE_FLAG_IS_PAUSING_OR_PAUSED = 131072;
    private static final int ACTIVITY_STATE_FLAG_IS_STOPPING = 262144;
    private static final int ACTIVITY_STATE_FLAG_IS_STOPPING_FINISHING = 524288;
    private static final int ACTIVITY_STATE_FLAG_IS_VISIBLE = 65536;
    private static final int ACTIVITY_STATE_FLAG_IS_WINDOW_VISIBLE = 1048576;
    private static final int ACTIVITY_STATE_FLAG_MASK_MIN_TASK_LAYER = 65535;
    private boolean agaresProcess;
    private final ActivityTaskManagerService mAtm;
    private final BackgroundLaunchProcessController mBgLaunchController;
    private ActivityRecord mConfigActivityRecord;
    private volatile boolean mCrashing;
    private volatile int mCurSchedGroup;
    private volatile boolean mDebugging;
    private DisplayArea mDisplayArea;
    private volatile long mFgInteractionTime;
    private volatile boolean mHasActivities;
    private volatile boolean mHasClientActivities;
    private volatile boolean mHasForegroundServices;
    private volatile boolean mHasImeService;
    private volatile boolean mHasOverlayUi;
    private boolean mHasPendingConfigurationChange;
    private volatile boolean mHasRecentTasks;
    private volatile boolean mHasTopUi;
    private ArrayList<ActivityRecord> mInactiveActivities;
    final ApplicationInfo mInfo;
    private volatile boolean mInstrumenting;
    private volatile boolean mInstrumentingWithBackgroundActivityStartPrivileges;
    private volatile long mInteractionEventTime;
    private volatile boolean mIsActivityConfigOverrideAllowed;
    private volatile long mLastActivityFinishTime;
    private volatile long mLastActivityLaunchTime;
    private final WindowProcessListener mListener;
    final String mName;
    private volatile boolean mNotResponding;
    public final Object mOwner;
    private int mPauseConfigurationDispatchCount;
    private volatile boolean mPendingUiClean;
    private volatile boolean mPerceptible;
    private volatile boolean mPersistent;
    public volatile int mPid;
    private TranProcessWrapper mProcessWrapper;
    private volatile String mRequiredAbi;
    private boolean mRunningRecentsAnimation;
    private boolean mRunningRemoteAnimation;
    private IApplicationThread mThread;
    public final int mUid;
    final int mUserId;
    private volatile boolean mUsingWrapper;
    int mVrThreadTid;
    private volatile long mWhenUnimportant;
    private int procType;
    private static final String TAG = "ActivityTaskManager";
    private static final String TAG_RELEASE = TAG + ActivityTaskManagerDebugConfig.POSTFIX_RELEASE;
    private static final String TAG_CONFIGURATION = TAG + ActivityTaskManagerDebugConfig.POSTFIX_CONFIGURATION;
    final ArraySet<String> mPkgList = new ArraySet<>();
    private volatile int mCurProcState = 20;
    private volatile int mRepProcState = 20;
    private volatile int mInstrumentationSourceUid = -1;
    private final ArrayList<ActivityRecord> mActivities = new ArrayList<>();
    private final ArrayList<Task> mRecentTasks = new ArrayList<>();
    private ActivityRecord mPreQTopResumedActivity = null;
    private final Configuration mLastReportedConfiguration = new Configuration();
    private final ArrayList<ActivityRecord> mHostActivities = new ArrayList<>();
    private final ArrayList<WeakReference<WindowProcessController>> mRemoteAnimationDelegates = new ArrayList<>();
    private volatile int mActivityStateFlags = 65535;

    /* loaded from: classes2.dex */
    public interface ComputeOomAdjCallback {
        void onOtherActivity();

        void onPausedActivity();

        void onStoppingActivity(boolean z);

        void onVisibleActivity();
    }

    public boolean isAgaresProcess() {
        return this.agaresProcess;
    }

    public void setAgaresProcess(boolean agaresProcess) {
        this.agaresProcess = agaresProcess;
    }

    public void setPreloadProcType() {
        if (ITranActivityManagerService.Instance().isAppLaunchDebugEnable()) {
            Slog.d(TAG, "AppLaunchTracker setPreloadProcType");
        }
        this.procType = 1;
    }

    public void setKeepLiveProcType() {
        if (ITranActivityManagerService.Instance().isAppLaunchDebugEnable()) {
            Slog.d(TAG, "setKeepLiveProcType");
        }
        this.procType = 2;
    }

    public void setNormalProcType() {
        if (ITranActivityManagerService.Instance().isAppLaunchDebugEnable()) {
            Slog.d(TAG, "setNormalProcType");
        }
        this.procType = 0;
    }

    public int getProcType() {
        return this.procType;
    }

    public WindowProcessController(final ActivityTaskManagerService atm, ApplicationInfo info, String name, int uid, int userId, Object owner, WindowProcessListener listener) {
        this.mIsActivityConfigOverrideAllowed = true;
        this.mInfo = info;
        this.mName = name;
        this.mUid = uid;
        this.mUserId = userId;
        this.mOwner = owner;
        this.mListener = listener;
        this.mAtm = atm;
        Objects.requireNonNull(atm);
        this.mBgLaunchController = new BackgroundLaunchProcessController(new IntPredicate() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda6
            @Override // java.util.function.IntPredicate
            public final boolean test(int i) {
                return ActivityTaskManagerService.this.hasActiveVisibleWindow(i);
            }
        }, atm.getBackgroundActivityStartCallback());
        boolean isSysUiPackage = info.packageName.equals(atm.getSysUiServiceComponentLocked().getPackageName());
        if (isSysUiPackage || uid == 1000) {
            this.mIsActivityConfigOverrideAllowed = false;
        }
        onConfigurationChanged(atm.getGlobalConfiguration());
        atm.mPackageConfigPersister.updateConfigIfNeeded(this, userId, info.packageName);
    }

    public void setPid(int pid) {
        this.mPid = pid;
    }

    public int getPid() {
        return this.mPid;
    }

    public void setThread(IApplicationThread thread) {
        synchronized (this.mAtm.mGlobalLockWithoutBoost) {
            this.mThread = thread;
            if (thread != null) {
                setLastReportedConfiguration(getConfiguration());
            } else {
                this.mAtm.mVisibleActivityProcessTracker.removeProcess(this);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IApplicationThread getThread() {
        return this.mThread;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasThread() {
        return this.mThread != null;
    }

    public void setCurrentSchedulingGroup(int curSchedGroup) {
        this.mCurSchedGroup = curSchedGroup;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentSchedulingGroup() {
        return this.mCurSchedGroup;
    }

    public void setCurrentProcState(int curProcState) {
        this.mCurProcState = curProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentProcState() {
        return this.mCurProcState;
    }

    public void setReportedProcState(int repProcState) {
        this.mRepProcState = repProcState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getReportedProcState() {
        return this.mRepProcState;
    }

    public void setCrashing(boolean crashing) {
        this.mCrashing = crashing;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCrashing() {
        return this.mCrashing;
    }

    public void setNotResponding(boolean notResponding) {
        this.mNotResponding = notResponding;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNotResponding() {
        return this.mNotResponding;
    }

    public void setPersistent(boolean persistent) {
        this.mPersistent = persistent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPersistent() {
        return this.mPersistent;
    }

    public void setHasForegroundServices(boolean hasForegroundServices) {
        this.mHasForegroundServices = hasForegroundServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundServices() {
        return this.mHasForegroundServices;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasForegroundActivities() {
        return this.mAtm.mTopApp == this || (this.mActivityStateFlags & 458752) != 0;
    }

    public void setHasClientActivities(boolean hasClientActivities) {
        this.mHasClientActivities = hasClientActivities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasClientActivities() {
        return this.mHasClientActivities;
    }

    public void setHasTopUi(boolean hasTopUi) {
        this.mHasTopUi = hasTopUi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasTopUi() {
        return this.mHasTopUi;
    }

    public void setHasOverlayUi(boolean hasOverlayUi) {
        this.mHasOverlayUi = hasOverlayUi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasOverlayUi() {
        return this.mHasOverlayUi;
    }

    public void setPendingUiClean(boolean hasPendingUiClean) {
        this.mPendingUiClean = hasPendingUiClean;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPendingUiClean() {
        return this.mPendingUiClean;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean registeredForDisplayAreaConfigChanges() {
        return this.mDisplayArea != null;
    }

    boolean registeredForActivityConfigChanges() {
        return this.mConfigActivityRecord != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void postPendingUiCleanMsg(boolean pendingUiClean) {
        Message m = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda7
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((WindowProcessListener) obj).setPendingUiClean(((Boolean) obj2).booleanValue());
            }
        }, this.mListener, Boolean.valueOf(pendingUiClean));
        this.mAtm.mH.sendMessage(m);
    }

    public void setInteractionEventTime(long interactionEventTime) {
        this.mInteractionEventTime = interactionEventTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getInteractionEventTime() {
        return this.mInteractionEventTime;
    }

    public void setFgInteractionTime(long fgInteractionTime) {
        this.mFgInteractionTime = fgInteractionTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getFgInteractionTime() {
        return this.mFgInteractionTime;
    }

    public void setWhenUnimportant(long whenUnimportant) {
        this.mWhenUnimportant = whenUnimportant;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getWhenUnimportant() {
        return this.mWhenUnimportant;
    }

    public void setRequiredAbi(String requiredAbi) {
        this.mRequiredAbi = requiredAbi;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getRequiredAbi() {
        return this.mRequiredAbi;
    }

    DisplayArea getDisplayArea() {
        return this.mDisplayArea;
    }

    public void setDebugging(boolean debugging) {
        this.mDebugging = debugging;
    }

    boolean isDebugging() {
        return this.mDebugging;
    }

    public void setUsingWrapper(boolean usingWrapper) {
        this.mUsingWrapper = usingWrapper;
    }

    boolean isUsingWrapper() {
        return this.mUsingWrapper;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasEverLaunchedActivity() {
        return this.mLastActivityLaunchTime > 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastActivityLaunchTime(long launchTime) {
        if (launchTime <= this.mLastActivityLaunchTime) {
            if (launchTime < this.mLastActivityLaunchTime) {
                Slog.w(TAG, "Tried to set launchTime (" + launchTime + ") < mLastActivityLaunchTime (" + this.mLastActivityLaunchTime + ")");
                return;
            }
            return;
        }
        this.mLastActivityLaunchTime = launchTime;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastActivityFinishTimeIfNeeded(long finishTime) {
        if (finishTime <= this.mLastActivityFinishTime || !hasActivityInVisibleTask()) {
            return;
        }
        this.mLastActivityFinishTime = finishTime;
    }

    public void addOrUpdateAllowBackgroundActivityStartsToken(Binder entity, IBinder originatingToken) {
        this.mBgLaunchController.addOrUpdateAllowBackgroundActivityStartsToken(entity, originatingToken);
    }

    public void removeAllowBackgroundActivityStartsToken(Binder entity) {
        this.mBgLaunchController.removeAllowBackgroundActivityStartsToken(entity);
    }

    public boolean areBackgroundFgsStartsAllowed() {
        return areBackgroundActivityStartsAllowed(this.mAtm.getBalAppSwitchesState(), true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean areBackgroundActivityStartsAllowed(int appSwitchState) {
        return areBackgroundActivityStartsAllowed(appSwitchState, false);
    }

    private boolean areBackgroundActivityStartsAllowed(int appSwitchState, boolean isCheckingForFgsStart) {
        return this.mBgLaunchController.areBackgroundActivityStartsAllowed(this.mPid, this.mUid, this.mInfo.packageName, appSwitchState, isCheckingForFgsStart, hasActivityInVisibleTask(), this.mInstrumentingWithBackgroundActivityStartPrivileges, this.mAtm.getLastStopAppSwitchesTime(), this.mLastActivityLaunchTime, this.mLastActivityFinishTime);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canCloseSystemDialogsByToken() {
        return this.mBgLaunchController.canCloseSystemDialogsByToken(this.mUid);
    }

    public void setBoundClientUids(ArraySet<Integer> boundClientUids) {
        this.mBgLaunchController.setBoundClientUids(boundClientUids);
    }

    public void setInstrumenting(boolean instrumenting, int sourceUid, boolean hasBackgroundActivityStartPrivileges) {
        Preconditions.checkArgument(instrumenting || sourceUid == -1);
        this.mInstrumenting = instrumenting;
        this.mInstrumentationSourceUid = sourceUid;
        this.mInstrumentingWithBackgroundActivityStartPrivileges = hasBackgroundActivityStartPrivileges;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInstrumenting() {
        return this.mInstrumenting;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getInstrumentationSourceUid() {
        return this.mInstrumentationSourceUid;
    }

    public void setPerceptible(boolean perceptible) {
        this.mPerceptible = perceptible;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPerceptible() {
        return this.mPerceptible;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    protected int getChildCount() {
        return 0;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    protected ConfigurationContainer getChildAt(int index) {
        return null;
    }

    @Override // com.android.server.wm.ConfigurationContainer
    protected ConfigurationContainer getParent() {
        return this.mAtm.mRootWindowContainer;
    }

    public void addPackage(String packageName) {
        synchronized (this.mAtm.mGlobalLockWithoutBoost) {
            this.mPkgList.add(packageName);
        }
    }

    public void clearPackageList() {
        synchronized (this.mAtm.mGlobalLockWithoutBoost) {
            this.mPkgList.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addActivityIfNeeded(ActivityRecord r) {
        setLastActivityLaunchTime(r.lastLaunchTime);
        if (this.mActivities.contains(r)) {
            return;
        }
        this.mActivities.add(r);
        this.mHasActivities = true;
        ArrayList<ActivityRecord> arrayList = this.mInactiveActivities;
        if (arrayList != null) {
            arrayList.remove(r);
        }
        updateActivityConfigurationListener();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeActivity(ActivityRecord r, boolean keepAssociation) {
        if (keepAssociation) {
            ArrayList<ActivityRecord> arrayList = this.mInactiveActivities;
            if (arrayList == null) {
                ArrayList<ActivityRecord> arrayList2 = new ArrayList<>();
                this.mInactiveActivities = arrayList2;
                arrayList2.add(r);
            } else if (!arrayList.contains(r)) {
                this.mInactiveActivities.add(r);
            }
        } else {
            ArrayList<ActivityRecord> arrayList3 = this.mInactiveActivities;
            if (arrayList3 != null) {
                arrayList3.remove(r);
            }
        }
        this.mActivities.remove(r);
        this.mHasActivities = !this.mActivities.isEmpty();
        updateActivityConfigurationListener();
    }

    void clearActivities() {
        this.mInactiveActivities = null;
        this.mActivities.clear();
        this.mHasActivities = false;
        updateActivityConfigurationListener();
    }

    public boolean hasActivities() {
        return this.mHasActivities;
    }

    public boolean hasVisibleActivities() {
        return (this.mActivityStateFlags & 65536) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasActivityInVisibleTask() {
        return (this.mActivityStateFlags & 4194304) != 0;
    }

    public boolean hasActivitiesOrRecentTasks() {
        return this.mHasActivities || this.mHasRecentTasks;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TaskDisplayArea getTopActivityDisplayArea() {
        if (this.mActivities.isEmpty()) {
            return null;
        }
        int lastIndex = this.mActivities.size() - 1;
        ActivityRecord topRecord = this.mActivities.get(lastIndex);
        TaskDisplayArea displayArea = topRecord.getDisplayArea();
        for (int index = lastIndex - 1; index >= 0; index--) {
            ActivityRecord nextRecord = this.mActivities.get(index);
            TaskDisplayArea nextDisplayArea = nextRecord.getDisplayArea();
            if (nextRecord.compareTo((WindowContainer) topRecord) > 0 && nextDisplayArea != null) {
                topRecord = nextRecord;
                displayArea = nextDisplayArea;
            }
        }
        return displayArea;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updateTopResumingActivityInProcessIfNeeded(final ActivityRecord activity) {
        DisplayContent topDisplay;
        TaskFragment taskFrag;
        ActivityRecord ar;
        if (this.mInfo.targetSdkVersion >= 29 || this.mPreQTopResumedActivity == activity) {
            return true;
        }
        if (activity.isAttached()) {
            boolean canUpdate = false;
            ActivityRecord activityRecord = this.mPreQTopResumedActivity;
            if (activityRecord != null && activityRecord.isAttached()) {
                topDisplay = this.mPreQTopResumedActivity.mDisplayContent;
            } else {
                topDisplay = null;
            }
            canUpdate = (topDisplay != null && this.mPreQTopResumedActivity.mVisibleRequested && this.mPreQTopResumedActivity.isFocusable()) ? true : true;
            DisplayContent display = activity.mDisplayContent;
            if (!canUpdate && topDisplay.compareTo((WindowContainer) display) < 0) {
                canUpdate = true;
            }
            if (!canUpdate && (ar = topDisplay.getActivity(new Predicate() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda4
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return WindowProcessController.lambda$updateTopResumingActivityInProcessIfNeeded$0(ActivityRecord.this, (ActivityRecord) obj);
                }
            }, true, this.mPreQTopResumedActivity)) != null && ar != this.mPreQTopResumedActivity) {
                canUpdate = true;
            }
            if (canUpdate) {
                ActivityRecord activityRecord2 = this.mPreQTopResumedActivity;
                if (activityRecord2 != null && activityRecord2.isState(ActivityRecord.State.RESUMED) && (taskFrag = this.mPreQTopResumedActivity.getTaskFragment()) != null) {
                    boolean userLeaving = taskFrag.shouldBeVisible(null);
                    taskFrag.startPausing(userLeaving, false, activity, "top-resumed-changed");
                }
                this.mPreQTopResumedActivity = activity;
            }
            return canUpdate;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$updateTopResumingActivityInProcessIfNeeded$0(ActivityRecord activity, ActivityRecord r) {
        return r == activity;
    }

    public void stopFreezingActivities() {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int i = this.mActivities.size();
                while (i > 0) {
                    i--;
                    this.mActivities.get(i).stopFreezingScreenLocked(true);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishActivities() {
        ArrayList<ActivityRecord> activities = new ArrayList<>(this.mActivities);
        for (int i = 0; i < activities.size(); i++) {
            ActivityRecord r = activities.get(i);
            if (!r.finishing && r.isInRootTaskLocked()) {
                r.finishIfPossible("finish-heavy", true);
            }
        }
    }

    public boolean isInterestingToUser() {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int size = this.mActivities.size();
                for (int i = 0; i < size; i++) {
                    ActivityRecord r = this.mActivities.get(i);
                    if (r.isInterestingToUserLocked()) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return true;
                    }
                }
                if (isEmbedded()) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return false;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public boolean isForegroundToUser() {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int size = this.mActivities.size();
                for (int i = 0; i < size; i++) {
                    ActivityRecord r = this.mActivities.get(i);
                    if (r.isForegroundToUserLocked()) {
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

    private boolean isEmbedded() {
        for (int i = this.mHostActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = this.mHostActivities.get(i);
            if (r.isInterestingToUserLocked()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRunningActivity(String packageName) {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (int i = this.mActivities.size() - 1; i >= 0; i--) {
                    ActivityRecord r = this.mActivities.get(i);
                    if (packageName.equals(r.packageName)) {
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
    public void updateAppSpecificSettingsForAllActivitiesInPackage(String packageName, Integer nightMode, LocaleList localesOverride) {
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = this.mActivities.get(i);
            if (packageName.equals(r.packageName) && r.applyAppSpecificConfig(nightMode, localesOverride) && r.mVisibleRequested) {
                r.ensureActivityConfiguration(0, true);
            }
        }
    }

    public boolean hasRunningActivityInMultiWindow(String packageName) {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (int i = this.mActivities.size() - 1; i >= 0; i--) {
                    ActivityRecord r = this.mActivities.get(i);
                    if (ITranWindowProcessController.Instance().inMultiWindow(packageName, r)) {
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

    public void clearPackagePreferredForHomeActivities() {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                for (int i = this.mActivities.size() - 1; i >= 0; i--) {
                    ActivityRecord r = this.mActivities.get(i);
                    if (r.isActivityTypeHome()) {
                        Log.i(TAG, "Clearing package preferred activities from " + r.packageName);
                        try {
                            ActivityThread.getPackageManager().clearPackagePreferredActivities(r.packageName);
                        } catch (RemoteException e) {
                        }
                    }
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasStartedActivity(ActivityRecord launchedActivity) {
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord activity = this.mActivities.get(i);
            if (launchedActivity != activity && !activity.stopped) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasResumedActivity() {
        return (this.mActivityStateFlags & 2097152) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateIntentForHeavyWeightActivity(Intent intent) {
        if (this.mActivities.isEmpty()) {
            return;
        }
        ActivityRecord hist = this.mActivities.get(0);
        intent.putExtra("cur_app", hist.packageName);
        intent.putExtra("cur_task", hist.getTask().mTaskId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldKillProcessForRemovedTask(Task task) {
        for (int k = 0; k < this.mActivities.size(); k++) {
            ActivityRecord activity = this.mActivities.get(k);
            if (!activity.stopped) {
                return false;
            }
            Task otherTask = activity.getTask();
            if (task.mTaskId != otherTask.mTaskId && otherTask.inRecents) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void releaseSomeActivities(String reason) {
        ArrayList<ActivityRecord> candidates = null;
        if (ActivityTaskManagerDebugConfig.DEBUG_RELEASE) {
            Slog.d(TAG_RELEASE, "Trying to release some activities in " + this);
        }
        for (int i = 0; i < this.mActivities.size(); i++) {
            ActivityRecord r = this.mActivities.get(i);
            if (r.finishing || r.isState(ActivityRecord.State.DESTROYING, ActivityRecord.State.DESTROYED)) {
                if (ActivityTaskManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.d(TAG_RELEASE, "Abort release; already destroying: " + r);
                    return;
                }
                return;
            }
            if (r.mVisibleRequested || !r.stopped || !r.hasSavedState() || !r.isDestroyable() || r.isState(ActivityRecord.State.STARTED, ActivityRecord.State.RESUMED, ActivityRecord.State.PAUSING, ActivityRecord.State.PAUSED, ActivityRecord.State.STOPPING)) {
                if (ActivityTaskManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.d(TAG_RELEASE, "Not releasing in-use activity: " + r);
                }
            } else if (r.getParent() != null) {
                if (candidates == null) {
                    candidates = new ArrayList<>();
                }
                candidates.add(r);
            }
        }
        if (candidates != null) {
            candidates.sort(new Comparator() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda9
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return ((ActivityRecord) obj).compareTo((WindowContainer) ((ActivityRecord) obj2));
                }
            });
            int maxRelease = Math.max(candidates.size(), 1);
            do {
                ActivityRecord r2 = candidates.remove(0);
                if (ActivityTaskManagerDebugConfig.DEBUG_RELEASE) {
                    Slog.v(TAG_RELEASE, "Destroying " + r2 + " in state " + r2.getState() + " for reason " + reason);
                }
                r2.destroyImmediately(reason);
                maxRelease--;
            } while (maxRelease > 0);
        }
    }

    public void getDisplayContextsWithErrorDialogs(List<Context> displayContexts) {
        if (displayContexts == null) {
            return;
        }
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                RootWindowContainer root = this.mAtm.mWindowManager.mRoot;
                root.getDisplayContextsWithNonToastVisibleWindows(this.mPid, displayContexts);
                for (int i = this.mActivities.size() - 1; i >= 0; i--) {
                    ActivityRecord r = this.mActivities.get(i);
                    int displayId = r.getDisplayId();
                    Context c = root.getDisplayUiContext(displayId);
                    if (c != null && r.mVisibleRequested && !displayContexts.contains(c)) {
                        displayContexts.add(c);
                    }
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addHostActivity(ActivityRecord r) {
        if (this.mHostActivities.contains(r)) {
            return;
        }
        this.mHostActivities.add(r);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeHostActivity(ActivityRecord r) {
        this.mHostActivities.remove(r);
    }

    public int computeOomAdjFromActivities(ComputeOomAdjCallback callback) {
        int flags = this.mActivityStateFlags;
        if ((65536 & flags) != 0) {
            callback.onVisibleActivity();
        } else if ((131072 & flags) != 0) {
            callback.onPausedActivity();
        } else if ((262144 & flags) != 0) {
            callback.onStoppingActivity((524288 & flags) != 0);
        } else {
            callback.onOtherActivity();
        }
        return 65535 & flags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void computeProcessActivityState() {
        int layer;
        ActivityRecord.State bestInvisibleState = ActivityRecord.State.DESTROYED;
        boolean allStoppingFinishing = true;
        boolean visible = false;
        int minTaskLayer = Integer.MAX_VALUE;
        int stateFlags = 0;
        boolean wasResumed = hasResumedActivity();
        boolean wasAnyVisible = (this.mActivityStateFlags & 1114112) != 0;
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = this.mActivities.get(i);
            if (r.isVisible()) {
                stateFlags |= 1048576;
            }
            Task task = r.getTask();
            if (task != null && task.mLayerRank != -1) {
                stateFlags |= 4194304;
            }
            if (r.mVisibleRequested) {
                if (r.isState(ActivityRecord.State.RESUMED)) {
                    stateFlags |= 2097152;
                }
                if (task != null && minTaskLayer > 0 && (layer = task.mLayerRank) >= 0 && minTaskLayer > layer) {
                    minTaskLayer = layer;
                }
                visible = true;
            } else if (!visible && bestInvisibleState != ActivityRecord.State.PAUSING) {
                if (r.isState(ActivityRecord.State.PAUSING, ActivityRecord.State.PAUSED)) {
                    bestInvisibleState = ActivityRecord.State.PAUSING;
                } else if (r.isState(ActivityRecord.State.STOPPING)) {
                    bestInvisibleState = ActivityRecord.State.STOPPING;
                    allStoppingFinishing &= r.finishing;
                }
            }
        }
        int stateFlags2 = stateFlags | (65535 & minTaskLayer);
        if (visible) {
            stateFlags2 |= 65536;
        } else if (bestInvisibleState == ActivityRecord.State.PAUSING) {
            stateFlags2 |= 131072;
        } else if (bestInvisibleState == ActivityRecord.State.STOPPING) {
            stateFlags2 |= 262144;
            if (allStoppingFinishing) {
                stateFlags2 |= 524288;
            }
        }
        this.mActivityStateFlags = stateFlags2;
        boolean anyVisible = (1114112 & stateFlags2) != 0;
        if (!wasAnyVisible && anyVisible) {
            this.mAtm.mVisibleActivityProcessTracker.onAnyActivityVisible(this);
        } else if (wasAnyVisible && !anyVisible) {
            this.mAtm.mVisibleActivityProcessTracker.onAllActivitiesInvisible(this);
        } else if (wasAnyVisible && !wasResumed && hasResumedActivity()) {
            this.mAtm.mVisibleActivityProcessTracker.onActivityResumedWhileVisible(this);
        }
    }

    private void prepareOomAdjustment() {
        this.mAtm.mRootWindowContainer.rankTaskLayers();
        this.mAtm.mTaskSupervisor.computeProcessActivityStateBatch();
    }

    public int computeRelaunchReason() {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                int activitiesSize = this.mActivities.size();
                for (int i = activitiesSize - 1; i >= 0; i--) {
                    ActivityRecord r = this.mActivities.get(i);
                    if (r.mRelaunchReason != 0) {
                        int i2 = r.mRelaunchReason;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return i2;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return 0;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public long getInputDispatchingTimeoutMillis() {
        long j;
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (!isInstrumenting() && !isUsingWrapper()) {
                    j = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
                }
                j = 60000;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return j;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearProfilerIfNeeded() {
        this.mAtm.mH.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WindowProcessListener) obj).clearProfilerIfNeeded();
            }
        }, this.mListener));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateProcessInfo(boolean updateServiceConnectionActivities, boolean activityChange, boolean updateOomAdj, boolean addPendingTopUid) {
        if (addPendingTopUid) {
            addToPendingTop();
        }
        if (updateOomAdj) {
            prepareOomAdjustment();
        }
        Message m = PooledLambda.obtainMessage(new QuadConsumer() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda8
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
                ((WindowProcessListener) obj).updateProcessInfo(((Boolean) obj2).booleanValue(), ((Boolean) obj3).booleanValue(), ((Boolean) obj4).booleanValue());
            }
        }, this.mListener, Boolean.valueOf(updateServiceConnectionActivities), Boolean.valueOf(activityChange), Boolean.valueOf(updateOomAdj));
        this.mAtm.mH.sendMessage(m);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addToPendingTop() {
        this.mAtm.mAmInternal.addPendingTopUid(this.mUid, this.mPid, this.mThread);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateServiceConnectionActivities() {
        this.mAtm.mH.sendMessage(PooledLambda.obtainMessage(new Consumer() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WindowProcessListener) obj).updateServiceConnectionActivities();
            }
        }, this.mListener));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPendingUiCleanAndForceProcessStateUpTo(int newState) {
        Message m = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda10
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((WindowProcessListener) obj).setPendingUiCleanAndForceProcessStateUpTo(((Integer) obj2).intValue());
            }
        }, this.mListener, Integer.valueOf(newState));
        this.mAtm.mH.sendMessage(m);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRemoved() {
        return this.mListener.isRemoved();
    }

    private boolean shouldSetProfileProc() {
        return this.mAtm.mProfileApp != null && this.mAtm.mProfileApp.equals(this.mName) && (this.mAtm.mProfileProc == null || this.mAtm.mProfileProc == this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ProfilerInfo createProfilerInfoIfNeeded() {
        ProfilerInfo currentProfilerInfo = this.mAtm.mProfilerInfo;
        if (currentProfilerInfo == null || currentProfilerInfo.profileFile == null || !shouldSetProfileProc()) {
            return null;
        }
        if (currentProfilerInfo.profileFd != null) {
            try {
                currentProfilerInfo.profileFd = currentProfilerInfo.profileFd.dup();
            } catch (IOException e) {
                currentProfilerInfo.closeFd();
            }
        }
        return new ProfilerInfo(currentProfilerInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onStartActivity(int topProcessState, ActivityInfo info) {
        String packageName = null;
        if ((info.flags & 1) == 0 || !PackageManagerService.PLATFORM_PACKAGE_NAME.equals(info.packageName)) {
            packageName = info.packageName;
        }
        if (topProcessState == 2) {
            this.mAtm.mAmInternal.addPendingTopUid(this.mUid, this.mPid, this.mThread);
        }
        prepareOomAdjustment();
        Message m = PooledLambda.obtainMessage(new QuintConsumer() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda0
            public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                ((WindowProcessListener) obj).onStartActivity(((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue(), (String) obj4, ((Long) obj5).longValue());
            }
        }, this.mListener, Integer.valueOf(topProcessState), Boolean.valueOf(shouldSetProfileProc()), packageName, Long.valueOf(info.applicationInfo.longVersionCode));
        this.mAtm.mH.sendMessageAtFrontOfQueue(m);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void appDied(String reason) {
        Message m = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda2
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((WindowProcessListener) obj).appDied((String) obj2);
            }
        }, this.mListener, reason);
        this.mAtm.mH.sendMessage(m);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleAppDied() {
        this.mAtm.mTaskSupervisor.removeHistoryRecords(this);
        boolean hasVisibleActivities = false;
        ArrayList<ActivityRecord> arrayList = this.mInactiveActivities;
        boolean hasInactiveActivities = (arrayList == null || arrayList.isEmpty()) ? false : true;
        ArrayList<ActivityRecord> activities = (this.mHasActivities || hasInactiveActivities) ? new ArrayList<>() : this.mActivities;
        if (this.mHasActivities) {
            activities.addAll(this.mActivities);
        }
        if (hasInactiveActivities) {
            activities.addAll(this.mInactiveActivities);
        }
        if (isRemoved()) {
            for (int i = activities.size() - 1; i >= 0; i--) {
                activities.get(i).makeFinishingLocked();
            }
        }
        int i2 = activities.size();
        for (int i3 = i2 - 1; i3 >= 0; i3--) {
            ActivityRecord r = activities.get(i3);
            hasVisibleActivities = (r.mVisibleRequested || r.isVisible()) ? true : true;
            TaskFragment taskFragment = r.getTaskFragment();
            if (taskFragment != null) {
                hasVisibleActivities |= taskFragment.handleAppDied(this);
            }
            r.handleAppDied();
        }
        clearRecentTasks();
        clearActivities();
        return hasVisibleActivities;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerDisplayAreaConfigurationListener(DisplayArea displayArea) {
        if (displayArea == null || displayArea.containsListener(this)) {
            return;
        }
        unregisterConfigurationListeners();
        this.mDisplayArea = displayArea;
        displayArea.registerConfigurationChangeListener(this);
    }

    void unregisterDisplayAreaConfigurationListener() {
        DisplayArea displayArea = this.mDisplayArea;
        if (displayArea == null) {
            return;
        }
        displayArea.unregisterConfigurationChangeListener(this);
        this.mDisplayArea = null;
        onMergedOverrideConfigurationChanged(Configuration.EMPTY);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerActivityConfigurationListener(ActivityRecord activityRecord) {
        if (activityRecord == null || activityRecord.containsListener(this) || !this.mIsActivityConfigOverrideAllowed) {
            return;
        }
        unregisterConfigurationListeners();
        this.mConfigActivityRecord = activityRecord;
        activityRecord.registerConfigurationChangeListener(this);
    }

    private void unregisterActivityConfigurationListener() {
        ActivityRecord activityRecord = this.mConfigActivityRecord;
        if (activityRecord == null) {
            return;
        }
        activityRecord.unregisterConfigurationChangeListener(this);
        this.mConfigActivityRecord = null;
        onMergedOverrideConfigurationChanged(Configuration.EMPTY);
    }

    private void unregisterConfigurationListeners() {
        unregisterActivityConfigurationListener();
        unregisterDisplayAreaConfigurationListener();
    }

    private void updateActivityConfigurationListener() {
        if (!this.mIsActivityConfigOverrideAllowed) {
            return;
        }
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord activityRecord = this.mActivities.get(i);
            if (!activityRecord.finishing) {
                registerActivityConfigurationListener(activityRecord);
                return;
            }
        }
        unregisterActivityConfigurationListener();
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public void onConfigurationChanged(Configuration newGlobalConfig) {
        super.onConfigurationChanged(newGlobalConfig);
        updateConfiguration();
    }

    @Override // com.android.server.wm.ConfigurationContainer
    public void onRequestedOverrideConfigurationChanged(Configuration overrideConfiguration) {
        super.onRequestedOverrideConfigurationChanged(overrideConfiguration);
    }

    @Override // com.android.server.wm.ConfigurationContainerListener
    public void onMergedOverrideConfigurationChanged(Configuration mergedOverrideConfig) {
        super.onRequestedOverrideConfigurationChanged(mergedOverrideConfig);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.ConfigurationContainer
    public void resolveOverrideConfiguration(Configuration newParentConfig) {
        Configuration requestedOverrideConfig = getRequestedOverrideConfiguration();
        if (requestedOverrideConfig.assetsSeq != 0 && newParentConfig.assetsSeq > requestedOverrideConfig.assetsSeq) {
            requestedOverrideConfig.assetsSeq = 0;
        }
        super.resolveOverrideConfiguration(newParentConfig);
        Configuration resolvedConfig = getResolvedOverrideConfiguration();
        resolvedConfig.windowConfiguration.setActivityType(0);
        resolvedConfig.seq = newParentConfig.seq;
    }

    private void updateConfiguration() {
        Configuration config = getConfiguration();
        if (this.mLastReportedConfiguration.diff(config) == 0) {
            if (Build.IS_DEBUGGABLE && this.mHasImeService) {
                Slog.w(TAG_CONFIGURATION, "Current config: " + config + " unchanged for IME proc " + this.mName);
            }
        } else if (this.mPauseConfigurationDispatchCount > 0) {
            this.mHasPendingConfigurationChange = true;
        } else {
            dispatchConfiguration(config);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchConfiguration(Configuration config) {
        this.mHasPendingConfigurationChange = false;
        if (this.mThread == null) {
            if (Build.IS_DEBUGGABLE && this.mHasImeService) {
                Slog.w(TAG_CONFIGURATION, "Unable to send config for IME proc " + this.mName + ": no app thread");
                return;
            }
            return;
        }
        if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
            String protoLogParam0 = String.valueOf(this.mName);
            String protoLogParam1 = String.valueOf(config);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, 1049367566, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        if (Build.IS_DEBUGGABLE && this.mHasImeService) {
            Slog.v(TAG_CONFIGURATION, "Sending to IME proc " + this.mName + " new config " + config);
        }
        try {
            config.seq = this.mAtm.increaseConfigurationSeqLocked();
            this.mAtm.getLifecycleManager().scheduleTransaction(this.mThread, ConfigurationChangeItem.obtain(config));
            setLastReportedConfiguration(config);
        } catch (Exception e) {
            Slog.e(TAG_CONFIGURATION, "Failed to schedule configuration change", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastReportedConfiguration(Configuration config) {
        this.mLastReportedConfiguration.setTo(config);
    }

    Configuration getLastReportedConfiguration() {
        return this.mLastReportedConfiguration;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pauseConfigurationDispatch() {
        this.mPauseConfigurationDispatchCount++;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean resumeConfigurationDispatch() {
        int i = this.mPauseConfigurationDispatchCount;
        if (i == 0) {
            return false;
        }
        this.mPauseConfigurationDispatchCount = i - 1;
        return this.mHasPendingConfigurationChange;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateAssetConfiguration(int assetSeq) {
        if (!this.mHasActivities || !this.mIsActivityConfigOverrideAllowed) {
            Configuration overrideConfig = new Configuration(getRequestedOverrideConfiguration());
            overrideConfig.assetsSeq = assetSeq;
            onRequestedOverrideConfigurationChanged(overrideConfig);
            return;
        }
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = this.mActivities.get(i);
            Configuration overrideConfig2 = new Configuration(r.getRequestedOverrideConfiguration());
            overrideConfig2.assetsSeq = assetSeq;
            r.onRequestedOverrideConfigurationChanged(overrideConfig2);
            if (r.mVisibleRequested) {
                r.ensureActivityConfiguration(0, true);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Configuration prepareConfigurationForLaunchingActivity() {
        Configuration config = getConfiguration();
        if (this.mHasPendingConfigurationChange) {
            this.mHasPendingConfigurationChange = false;
            config.seq = this.mAtm.increaseConfigurationSeqLocked();
        }
        return config;
    }

    public long getCpuTime() {
        return this.mListener.getCpuTime();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addRecentTask(Task task) {
        this.mRecentTasks.add(task);
        this.mHasRecentTasks = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeRecentTask(Task task) {
        this.mRecentTasks.remove(task);
        this.mHasRecentTasks = !this.mRecentTasks.isEmpty();
    }

    public boolean hasRecentTasks() {
        return this.mHasRecentTasks;
    }

    void clearRecentTasks() {
        for (int i = this.mRecentTasks.size() - 1; i >= 0; i--) {
            this.mRecentTasks.get(i).clearRootProcess();
        }
        this.mRecentTasks.clear();
        this.mHasRecentTasks = false;
    }

    public void appEarlyNotResponding(String annotation, Runnable killAppCallback) {
        Runnable targetRunnable = null;
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mAtm.mController == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                try {
                    int res = this.mAtm.mController.appEarlyNotResponding(this.mName, this.mPid, annotation);
                    if (res < 0) {
                        if (this.mPid != ActivityManagerService.MY_PID) {
                            targetRunnable = killAppCallback;
                        }
                    }
                } catch (RemoteException e) {
                    this.mAtm.mController = null;
                    Watchdog.getInstance().setActivityController(null);
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                if (targetRunnable != null) {
                    targetRunnable.run();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    public boolean appNotResponding(String info, Runnable killAppCallback, Runnable serviceTimeoutCallback) {
        Runnable targetRunnable = null;
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mAtm.mController == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                try {
                    int res = this.mAtm.mController.appNotResponding(this.mName, this.mPid, info);
                    if (res != 0) {
                        if (res < 0) {
                            if (this.mPid != ActivityManagerService.MY_PID) {
                                targetRunnable = killAppCallback;
                            }
                        }
                        targetRunnable = serviceTimeoutCallback;
                    }
                    WindowManagerService.resetPriorityAfterLockedSection();
                    if (targetRunnable != null) {
                        targetRunnable.run();
                        return true;
                    }
                    return false;
                } catch (RemoteException e) {
                    this.mAtm.mController = null;
                    Watchdog.getInstance().setActivityController(null);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
            } catch (Throwable e2) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw e2;
            }
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void onServiceStarted(ServiceInfo serviceInfo) {
        String permission = serviceInfo.permission;
        if (permission == null) {
            return;
        }
        char c = 65535;
        switch (permission.hashCode()) {
            case -769871357:
                if (permission.equals("android.permission.BIND_VOICE_INTERACTION")) {
                    c = 2;
                    break;
                }
                break;
            case 1412417858:
                if (permission.equals("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
                    c = 1;
                    break;
                }
                break;
            case 1448369304:
                if (permission.equals("android.permission.BIND_INPUT_METHOD")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                this.mHasImeService = true;
                break;
            case 1:
            case 2:
                break;
            default:
                return;
        }
        this.mIsActivityConfigOverrideAllowed = false;
        unregisterActivityConfigurationListener();
    }

    public void onTopProcChanged() {
        if (this.mAtm.mVrController.isInterestingToSchedGroup()) {
            this.mAtm.mH.post(new Runnable() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    WindowProcessController.this.m8530xb9663df2();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onTopProcChanged$1$com-android-server-wm-WindowProcessController  reason: not valid java name */
    public /* synthetic */ void m8530xb9663df2() {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mAtm.mVrController.onTopProcChangedLocked(this);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean isHomeProcess() {
        return this == this.mAtm.mHomeProcess;
    }

    public boolean isPreviousProcess() {
        return this == this.mAtm.mPreviousProcess;
    }

    public boolean isHeavyWeightProcess() {
        return this == this.mAtm.mHeavyWeightProcess;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRunningRecentsAnimation(boolean running) {
        if (this.mRunningRecentsAnimation == running) {
            return;
        }
        this.mRunningRecentsAnimation = running;
        updateRunningRemoteOrRecentsAnimation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRunningRemoteAnimation(boolean running) {
        if (this.mRunningRemoteAnimation == running) {
            return;
        }
        this.mRunningRemoteAnimation = running;
        updateRunningRemoteOrRecentsAnimation();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addRemoteAnimationDelegate(WindowProcessController delegate) {
        if (!isRunningRemoteTransition()) {
            throw new IllegalStateException("Can't add a delegate to a process which isn't itself running a remote animation");
        }
        this.mRemoteAnimationDelegates.add(new WeakReference<>(delegate));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateRunningRemoteOrRecentsAnimation() {
        if (!isRunningRemoteTransition()) {
            for (int i = 0; i < this.mRemoteAnimationDelegates.size(); i++) {
                WindowProcessController delegate = this.mRemoteAnimationDelegates.get(i).get();
                if (delegate != null) {
                    delegate.setRunningRemoteAnimation(false);
                    delegate.setRunningRecentsAnimation(false);
                }
            }
            this.mRemoteAnimationDelegates.clear();
        }
        this.mAtm.mH.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.WindowProcessController$$ExternalSyntheticLambda5
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                ((WindowProcessListener) obj).setRunningRemoteAnimation(((Boolean) obj2).booleanValue());
            }
        }, this.mListener, Boolean.valueOf(isRunningRemoteTransition())));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRunningRemoteTransition() {
        return this.mRunningRecentsAnimation || this.mRunningRemoteAnimation;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRunningAnimationUnsafe() {
        this.mListener.setRunningRemoteAnimation(true);
    }

    public String toString() {
        Object obj = this.mOwner;
        if (obj != null) {
            return obj.toString();
        }
        return null;
    }

    public void dump(PrintWriter pw, String prefix) {
        synchronized (this.mAtm.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mActivities.size() > 0) {
                    pw.print(prefix);
                    pw.println("Activities:");
                    for (int i = 0; i < this.mActivities.size(); i++) {
                        pw.print(prefix);
                        pw.print("  - ");
                        pw.println(this.mActivities.get(i));
                    }
                }
                if (this.mRecentTasks.size() > 0) {
                    pw.println(prefix + "Recent Tasks:");
                    for (int i2 = 0; i2 < this.mRecentTasks.size(); i2++) {
                        pw.println(prefix + "  - " + this.mRecentTasks.get(i2));
                    }
                }
                int i3 = this.mVrThreadTid;
                if (i3 != 0) {
                    pw.print(prefix);
                    pw.print("mVrThreadTid=");
                    pw.println(this.mVrThreadTid);
                }
                this.mBgLaunchController.dump(pw, prefix);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        pw.println(prefix + " Configuration=" + getConfiguration());
        pw.println(prefix + " OverrideConfiguration=" + getRequestedOverrideConfiguration());
        pw.println(prefix + " mLastReportedConfiguration=" + this.mLastReportedConfiguration);
        int stateFlags = this.mActivityStateFlags;
        if (stateFlags != 65535) {
            pw.print(prefix + " mActivityStateFlags=");
            if ((1048576 & stateFlags) != 0) {
                pw.print("W|");
            }
            if ((65536 & stateFlags) != 0) {
                pw.print("V|");
                if ((2097152 & stateFlags) != 0) {
                    pw.print("R|");
                }
            } else if ((131072 & stateFlags) != 0) {
                pw.print("P|");
            } else if ((262144 & stateFlags) != 0) {
                pw.print("S|");
                if ((524288 & stateFlags) != 0) {
                    pw.print("F|");
                }
            }
            int taskLayer = stateFlags & 65535;
            if (taskLayer != 65535) {
                pw.print("taskLayer=" + taskLayer);
            }
            pw.println();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        this.mListener.dumpDebug(proto, fieldId);
    }

    public void setProcessWrapper(TranProcessWrapper processWrapper) {
        this.mProcessWrapper = processWrapper;
    }

    public TranProcessWrapper getProcessWrapper() {
        return this.mProcessWrapper;
    }

    public ArrayList<ActivityRecord> getActivities() {
        return new ArrayList<>(this.mActivities);
    }
}
