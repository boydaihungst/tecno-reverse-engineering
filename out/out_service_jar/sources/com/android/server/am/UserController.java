package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.BroadcastOptions;
import android.app.Dialog;
import android.app.IStopUserCallback;
import android.app.IUserSwitchObserver;
import android.app.KeyguardManager;
import android.appwidget.AppWidgetManagerInternal;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.PermissionChecker;
import android.content.pm.IPackageManager;
import android.content.pm.PackagePartitions;
import android.content.pm.UserInfo;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.IProgressListener;
import android.os.IRemoteCallback;
import android.os.IUserManager;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.IntArray;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.FactoryResetter;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;
import com.android.server.am.UserController;
import com.android.server.am.UserState;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.UserManagerInternal;
import com.android.server.pm.UserManagerService;
import com.android.server.utils.Slogf;
import com.android.server.utils.TimingsTraceAndSlog;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowManagerService;
import com.transsion.hubcore.server.am.ITranActivityManagerService;
import com.transsion.server.userswitch.ITranUserSwitchDialogLice;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class UserController implements Handler.Callback {
    static final int CLEAR_USER_JOURNEY_SESSION_MSG = 200;
    static final int COMPLETE_USER_SWITCH_MSG = 130;
    static final int CONTINUE_USER_SWITCH_MSG = 20;
    private static final boolean DEBUG_Fans = "1".equals(SystemProperties.get("persist.sys.fans.support", "0"));
    static final int FOREGROUND_PROFILE_CHANGED_MSG = 70;
    private static final long INVALID_SESSION_ID = 0;
    private static final int LONG_USER_SWITCH_OBSERVER_WARNING_TIME_MS = 500;
    static final int REPORT_LOCKED_BOOT_COMPLETE_MSG = 110;
    static final int REPORT_USER_SWITCH_COMPLETE_MSG = 80;
    static final int REPORT_USER_SWITCH_MSG = 10;
    static final int START_PROFILES_MSG = 40;
    static final int START_USER_SWITCH_FG_MSG = 120;
    static final int START_USER_SWITCH_UI_MSG = 1000;
    private static final String TAG = "ActivityManager";
    private static final int USER_COMPLETED_EVENT_DELAY_MS = 5000;
    static final int USER_COMPLETED_EVENT_MSG = 140;
    static final int USER_CURRENT_MSG = 60;
    private static final int USER_JOURNEY_TIMEOUT_MS = 90000;
    private static final int USER_JOURNEY_UNKNOWN = 0;
    private static final int USER_JOURNEY_USER_CREATE = 4;
    private static final int USER_JOURNEY_USER_START = 3;
    private static final int USER_JOURNEY_USER_STOP = 5;
    private static final int USER_JOURNEY_USER_SWITCH_FG = 2;
    private static final int USER_JOURNEY_USER_SWITCH_UI = 1;
    private static final int USER_LIFECYCLE_EVENT_CREATE_USER = 3;
    private static final int USER_LIFECYCLE_EVENT_START_USER = 2;
    private static final int USER_LIFECYCLE_EVENT_STATE_BEGIN = 1;
    private static final int USER_LIFECYCLE_EVENT_STATE_FINISH = 2;
    private static final int USER_LIFECYCLE_EVENT_STATE_NONE = 0;
    private static final int USER_LIFECYCLE_EVENT_STOP_USER = 7;
    private static final int USER_LIFECYCLE_EVENT_SWITCH_USER = 1;
    private static final int USER_LIFECYCLE_EVENT_UNKNOWN = 0;
    private static final int USER_LIFECYCLE_EVENT_UNLOCKED_USER = 6;
    private static final int USER_LIFECYCLE_EVENT_UNLOCKING_USER = 5;
    private static final int USER_LIFECYCLE_EVENT_USER_RUNNING_LOCKED = 4;
    static final int USER_START_MSG = 50;
    private static final int USER_SWITCH_CALLBACKS_TIMEOUT_MS = 5000;
    static final int USER_SWITCH_CALLBACKS_TIMEOUT_MSG = 90;
    static final int USER_SWITCH_MOVEUSERTOFOREGROUND_MSG = 150;
    static final int USER_SWITCH_MOVEUSERTOFOREGROUND_MSG_TIMEOUT_MS = 1000;
    static final int USER_SWITCH_TIMEOUT_MS = 3000;
    static final int USER_SWITCH_TIMEOUT_MSG = 30;
    static final int USER_UNLOCKED_MSG = 105;
    static final int USER_UNLOCK_MSG = 100;
    volatile boolean mBootCompleted;
    private final SparseIntArray mCompletedEventTypes;
    private volatile ArraySet<String> mCurWaitingUserSwitchCallbacks;
    private int[] mCurrentProfileIds;
    private volatile int mCurrentUserId;
    private boolean mDelayUserDataLocking;
    private final Handler mHandler;
    private boolean mInitialized;
    private final Injector mInjector;
    private final ArrayList<Integer> mLastActiveUsers;
    private volatile long mLastUserUnlockingUptime;
    private final Object mLock;
    private final LockPatternUtils mLockPatternUtils;
    private int mMaxRunningUsers;
    private int[] mStartedUserArray;
    private final SparseArray<UserState> mStartedUsers;
    private int mStopUserOnSwitch;
    private String mSwitchingFromSystemUserMessage;
    private String mSwitchingToSystemUserMessage;
    private volatile int mTargetUserId;
    private ArraySet<String> mTimeoutUserSwitchCallbacks;
    private final Handler mUiHandler;
    private final SparseArray<UserJourneySession> mUserIdToUserJourneyMap;
    private final ArrayList<Integer> mUserLru;
    private final SparseIntArray mUserProfileGroupIds;
    private final RemoteCallbackList<IUserSwitchObserver> mUserSwitchObservers;
    private boolean mUserSwitchUiEnabled;
    private int switchNum;
    private long switchTime;

    /* loaded from: classes.dex */
    @interface UserJourney {
    }

    /* loaded from: classes.dex */
    @interface UserLifecycleEvent {
    }

    /* loaded from: classes.dex */
    @interface UserLifecycleEventState {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserController(ActivityManagerService service) {
        this(new Injector(service));
    }

    UserController(Injector injector) {
        this.mLock = new Object();
        this.mCurrentUserId = 0;
        this.mTargetUserId = -10000;
        SparseArray<UserState> sparseArray = new SparseArray<>();
        this.mStartedUsers = sparseArray;
        ArrayList<Integer> arrayList = new ArrayList<>();
        this.mUserLru = arrayList;
        this.mStartedUserArray = new int[]{0};
        this.mCurrentProfileIds = new int[0];
        this.mUserProfileGroupIds = new SparseIntArray();
        this.mUserSwitchObservers = new RemoteCallbackList<>();
        this.mUserSwitchUiEnabled = true;
        this.mLastActiveUsers = new ArrayList<>();
        this.mUserIdToUserJourneyMap = new SparseArray<>();
        this.mCompletedEventTypes = new SparseIntArray();
        this.mStopUserOnSwitch = -1;
        this.mLastUserUnlockingUptime = 0L;
        this.switchNum = 0;
        this.switchTime = 0L;
        this.mInjector = injector;
        this.mHandler = injector.getHandler(this);
        this.mUiHandler = injector.getUiHandler(this);
        UserState uss = new UserState(UserHandle.SYSTEM);
        uss.mUnlockProgress.addListener(new UserProgressListener());
        sparseArray.put(0, uss);
        arrayList.add(0);
        this.mLockPatternUtils = injector.getLockPatternUtils();
        updateStartedUserArrayLU();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInitialConfig(boolean userSwitchUiEnabled, int maxRunningUsers, boolean delayUserDataLocking) {
        synchronized (this.mLock) {
            this.mUserSwitchUiEnabled = userSwitchUiEnabled;
            this.mMaxRunningUsers = maxRunningUsers;
            this.mDelayUserDataLocking = delayUserDataLocking;
            this.mInitialized = true;
        }
    }

    private boolean isUserSwitchUiEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mUserSwitchUiEnabled;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getMaxRunningUsers() {
        int i;
        synchronized (this.mLock) {
            i = this.mMaxRunningUsers;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStopUserOnSwitch(int value) {
        if (this.mInjector.checkCallingPermission("android.permission.MANAGE_USERS") == -1 && this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS") == -1) {
            throw new SecurityException("You either need MANAGE_USERS or INTERACT_ACROSS_USERS permission to call setStopUserOnSwitch()");
        }
        synchronized (this.mLock) {
            Slogf.i(TAG, "setStopUserOnSwitch(): %d -> %d", Integer.valueOf(this.mStopUserOnSwitch), Integer.valueOf(value));
            this.mStopUserOnSwitch = value;
        }
    }

    private boolean shouldStopUserOnSwitch() {
        synchronized (this.mLock) {
            int i = this.mStopUserOnSwitch;
            if (i != -1) {
                boolean value = i == 1;
                Slogf.i(TAG, "shouldStopUserOnSwitch(): returning overridden value (%b)", Boolean.valueOf(value));
                return value;
            }
            int property = SystemProperties.getInt("fw.stop_bg_users_on_switch", -1);
            return property == -1 ? this.mDelayUserDataLocking : property == 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishUserSwitch(final UserState uss) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                UserController.this.m1516lambda$finishUserSwitch$0$comandroidserveramUserController(uss);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishUserSwitch$0$com-android-server-am-UserController  reason: not valid java name */
    public /* synthetic */ void m1516lambda$finishUserSwitch$0$comandroidserveramUserController(UserState uss) {
        finishUserBoot(uss);
        startProfiles();
        synchronized (this.mLock) {
            stopRunningUsersLU(this.mMaxRunningUsers);
        }
    }

    List<Integer> getRunningUsersLU() {
        ArrayList<Integer> runningUsers = new ArrayList<>();
        Iterator<Integer> it = this.mUserLru.iterator();
        while (it.hasNext()) {
            Integer userId = it.next();
            UserState uss = this.mStartedUsers.get(userId.intValue());
            if (uss != null && uss.state != 4 && uss.state != 5 && (userId.intValue() != 0 || !UserInfo.isSystemOnly(userId.intValue()))) {
                runningUsers.add(userId);
            }
        }
        return runningUsers;
    }

    private void stopRunningUsersLU(int maxRunningUsers) {
        List<Integer> currentlyRunning = getRunningUsersLU();
        Iterator<Integer> iterator = currentlyRunning.iterator();
        while (currentlyRunning.size() > maxRunningUsers && iterator.hasNext()) {
            Integer userId = iterator.next();
            if (userId.intValue() != 0 && userId.intValue() != this.mCurrentUserId && stopUsersLU(userId.intValue(), false, true, null, null) == 0) {
                iterator.remove();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean canStartMoreUsers() {
        boolean z;
        synchronized (this.mLock) {
            z = getRunningUsersLU().size() < this.mMaxRunningUsers;
        }
        return z;
    }

    private void finishUserBoot(UserState uss) {
        finishUserBoot(uss, null);
    }

    private void finishUserBoot(UserState uss, IIntentReceiver resultTo) {
        int userId = uss.mHandle.getIdentifier();
        EventLog.writeEvent((int) EventLogTags.UC_FINISH_USER_BOOT, userId);
        synchronized (this.mLock) {
            if (this.mStartedUsers.get(userId) != uss) {
                return;
            }
            if (uss.setState(0, 1)) {
                logUserLifecycleEvent(userId, 4, 0);
                this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
                if (userId == 0 && !this.mInjector.isRuntimeRestarted() && !this.mInjector.isFirstBootOrUpgrade()) {
                    long elapsedTimeMs = SystemClock.elapsedRealtime();
                    FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED, 12, elapsedTimeMs);
                    if (elapsedTimeMs > 120000) {
                        if ("user".equals(Build.TYPE)) {
                            Slogf.wtf(TimingsTraceAndSlog.SYSTEM_SERVER_TIMING_TAG, "finishUserBoot took too long. elapsedTimeMs=" + elapsedTimeMs);
                        } else {
                            Slogf.w(TimingsTraceAndSlog.SYSTEM_SERVER_TIMING_TAG, "finishUserBoot took too long. elapsedTimeMs=" + elapsedTimeMs);
                        }
                    }
                }
                if (!this.mInjector.getUserManager().isPreCreated(userId)) {
                    Handler handler = this.mHandler;
                    handler.sendMessage(handler.obtainMessage(110, userId, 0));
                    if (!UserManager.isHeadlessSystemUserMode() || !uss.mHandle.isSystem()) {
                        sendLockedBootCompletedBroadcast(resultTo, userId);
                    }
                }
            }
            if (this.mInjector.getUserManager().isProfile(userId)) {
                UserInfo parent = this.mInjector.getUserManager().getProfileParent(userId);
                if (parent != null && isUserRunning(parent.id, 4)) {
                    Slogf.d(TAG, "User " + userId + " (parent " + parent.id + "): attempting unlock because parent is unlocked");
                    maybeUnlockUser(userId);
                    return;
                }
                String parentId = parent == null ? "<null>" : String.valueOf(parent.id);
                Slogf.d(TAG, "User " + userId + " (parent " + parentId + "): delaying unlock because parent is locked");
                return;
            }
            maybeUnlockUser(userId);
        }
    }

    private void sendLockedBootCompletedBroadcast(IIntentReceiver receiver, int userId) {
        Intent intent = new Intent("android.intent.action.LOCKED_BOOT_COMPLETED", (Uri) null);
        intent.putExtra("android.intent.extra.user_handle", userId);
        intent.addFlags(-1996488704);
        this.mInjector.broadcastIntent(intent, null, receiver, 0, null, null, new String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, getTemporaryAppAllowlistBroadcastOptions(202).toBundle(), true, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), userId);
    }

    private boolean finishUserUnlocking(final UserState uss) {
        final int userId = uss.mHandle.getIdentifier();
        EventLog.writeEvent((int) EventLogTags.UC_FINISH_USER_UNLOCKING, userId);
        logUserLifecycleEvent(userId, 5, 1);
        if (StorageManager.isUserKeyUnlocked(userId)) {
            synchronized (this.mLock) {
                if (this.mStartedUsers.get(userId) == uss && uss.state == 1) {
                    uss.mUnlockProgress.start();
                    uss.mUnlockProgress.setProgress(5, this.mInjector.getContext().getString(17039664));
                    FgThread.getHandler().post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda5
                        @Override // java.lang.Runnable
                        public final void run() {
                            UserController.this.m1520xcd7b79fc(userId, uss);
                        }
                    });
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishUserUnlocking$1$com-android-server-am-UserController  reason: not valid java name */
    public /* synthetic */ void m1520xcd7b79fc(int userId, UserState uss) {
        if (!StorageManager.isUserKeyUnlocked(userId)) {
            Slogf.w(TAG, "User key got locked unexpectedly, leaving user locked.");
            return;
        }
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        t.traceBegin("UM.onBeforeUnlockUser-" + userId);
        this.mInjector.getUserManager().onBeforeUnlockUser(userId);
        t.traceEnd();
        synchronized (this.mLock) {
            if (uss.setState(1, 2)) {
                this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
                uss.mUnlockProgress.setProgress(20);
                this.mLastUserUnlockingUptime = SystemClock.uptimeMillis();
                this.mHandler.obtainMessage(100, userId, 0, uss).sendToTarget();
            }
        }
    }

    private void finishUserUnlocked(final UserState uss) {
        int userId;
        UserInfo parent;
        int userId2 = uss.mHandle.getIdentifier();
        EventLog.writeEvent((int) EventLogTags.UC_FINISH_USER_UNLOCKED, userId2);
        if (!StorageManager.isUserKeyUnlocked(userId2)) {
            return;
        }
        synchronized (this.mLock) {
            try {
                try {
                    if (this.mStartedUsers.get(uss.mHandle.getIdentifier()) != uss) {
                        return;
                    }
                    if (uss.setState(2, 3)) {
                        this.mInjector.getUserManagerInternal().setUserState(userId2, uss.state);
                        uss.mUnlockProgress.finish();
                        if (userId2 == 0) {
                            this.mInjector.startPersistentApps(262144);
                        }
                        this.mInjector.installEncryptionUnawareProviders(userId2);
                        if (this.mInjector.getUserManager().isPreCreated(userId2)) {
                            userId = userId2;
                        } else {
                            Intent unlockedIntent = new Intent("android.intent.action.USER_UNLOCKED");
                            unlockedIntent.putExtra("android.intent.extra.user_handle", userId2);
                            unlockedIntent.addFlags(1342177280);
                            userId = userId2;
                            this.mInjector.broadcastIntent(unlockedIntent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), userId);
                        }
                        int userId3 = userId;
                        UserInfo userInfo = getUserInfo(userId3);
                        if (userInfo.isProfile() && (parent = this.mInjector.getUserManager().getProfileParent(userId3)) != null) {
                            broadcastProfileAccessibleStateChanged(userId3, parent.id, "android.intent.action.PROFILE_ACCESSIBLE");
                            if (userInfo.isManagedProfile()) {
                                Intent profileUnlockedIntent = new Intent("android.intent.action.MANAGED_PROFILE_UNLOCKED");
                                profileUnlockedIntent.putExtra("android.intent.extra.USER", UserHandle.of(userId3));
                                profileUnlockedIntent.addFlags(1342177280);
                                this.mInjector.broadcastIntent(profileUnlockedIntent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), parent.id);
                            }
                        }
                        UserInfo info = getUserInfo(userId3);
                        if (!Objects.equals(info.lastLoggedInFingerprint, PackagePartitions.FINGERPRINT) || SystemProperties.getBoolean("persist.pm.mock-upgrade", false)) {
                            boolean quiet = info.isManagedProfile();
                            this.mInjector.sendPreBootBroadcast(userId3, quiet, new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda7
                                @Override // java.lang.Runnable
                                public final void run() {
                                    UserController.this.m1517lambda$finishUserUnlocked$2$comandroidserveramUserController(uss);
                                }
                            });
                            return;
                        }
                        m1517lambda$finishUserUnlocked$2$comandroidserveramUserController(uss);
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
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: finishUserUnlockedCompleted */
    public void m1517lambda$finishUserUnlocked$2$comandroidserveramUserController(UserState uss) {
        int userId;
        int userId2 = uss.mHandle.getIdentifier();
        EventLog.writeEvent((int) EventLogTags.UC_FINISH_USER_UNLOCKED_COMPLETED, userId2);
        synchronized (this.mLock) {
            try {
                if (this.mStartedUsers.get(uss.mHandle.getIdentifier()) != uss) {
                    try {
                        return;
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                }
                final UserInfo userInfo = getUserInfo(userId2);
                if (userInfo == null || !StorageManager.isUserKeyUnlocked(userId2)) {
                    return;
                }
                this.mInjector.getUserManager().onUserLoggedIn(userId2);
                final Runnable initializeUser = new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        UserController.this.m1518x11e70870(userInfo);
                    }
                };
                if (!userInfo.isInitialized()) {
                    Slogf.d(TAG, "Initializing user #" + userId2);
                    if (userInfo.preCreated) {
                        initializeUser.run();
                    } else if (userId2 != 0) {
                        Intent intent = new Intent("android.intent.action.USER_INITIALIZE");
                        intent.addFlags(AudioFormat.EVRCB);
                        this.mInjector.broadcastIntent(intent, null, new IIntentReceiver.Stub() { // from class: com.android.server.am.UserController.1
                            public void performReceive(Intent intent2, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                                initializeUser.run();
                            }
                        }, 0, null, null, null, -1, null, true, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), userId2);
                    }
                }
                if (userInfo.preCreated) {
                    Slogf.i(TAG, "Stopping pre-created user " + userInfo.toFullString());
                    stopUser(userInfo.id, true, false, null, null);
                    return;
                }
                Runnable initializeUser2 = initializeUser;
                int userId3 = userId2;
                this.mInjector.startUserWidgets(userId3);
                synchronized (this.mLock) {
                    try {
                        int[] userIds = new int[this.mStartedUsers.size()];
                        for (int i = 0; i < userIds.length; i++) {
                            try {
                                userIds[i] = this.mStartedUsers.keyAt(i);
                            } catch (Throwable th3) {
                                th = th3;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th4) {
                                        th = th4;
                                    }
                                }
                                throw th;
                            }
                        }
                        for (int testUserId : userIds) {
                            UserInfo parent = this.mInjector.getUserManager().getProfileParent(testUserId);
                            if (parent != null && parent.id == userId3 && testUserId != userId3 && !isUserRunning(testUserId, 4)) {
                                Slogf.d(TAG, "User " + testUserId + " (parent " + parent.id + "): attempting to unlock because parent finished unlocking");
                                maybeUnlockUser(testUserId);
                            }
                        }
                        this.mHandler.obtainMessage(105, userId3, 0).sendToTarget();
                        if (!SystemProperties.get("ro.os_securitycom_support").equals("1")) {
                            userId = userId3;
                        } else if (!"1".equals(SystemProperties.get("ro.os_sc_auto_repair"))) {
                            userId = userId3;
                        } else {
                            IBinder scBinder = ServiceManager.getService("securitycom_access");
                            IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
                            if (scBinder == null) {
                                Slogf.e(TAG, "WARNING!!! Shutdown! securityCom access isn't started");
                                try {
                                    pm.shutdown(false, "securitycom", false);
                                } catch (RemoteException re) {
                                    Slogf.e(TAG, "shutdown exception: " + re);
                                }
                                userId = userId3;
                            } else {
                                String version = null;
                                try {
                                    Class scStubCls = Class.forName("com.scorpio.service.securitycom.ISecurityComAccessService$Stub");
                                    Method asInterfaceMethod = scStubCls.getMethod("asInterface", IBinder.class);
                                    Object ips = asInterfaceMethod.invoke(null, scBinder);
                                    Method getVersionMethod = scStubCls.getMethod("getVersion", new Class[0]);
                                    version = (String) getVersionMethod.invoke(ips, new Object[0]);
                                } catch (Throwable e) {
                                    Slogf.e(TAG, "WARNING!!! Shutdown! securityCom exception: " + e);
                                    try {
                                        pm.shutdown(false, "securitycom", false);
                                    } catch (RemoteException re2) {
                                        Slogf.e(TAG, "shutdown exception: " + re2);
                                    }
                                }
                                if (version != null) {
                                    long versionCode = 0;
                                    int i2 = 0;
                                    for (String[] split = version.split("\\."); i2 < split.length; split = split) {
                                        versionCode = (long) (versionCode + (Integer.parseInt(split[i2]) * Math.pow(10.0d, (split.length - 1) - i2)));
                                        i2++;
                                        userId3 = userId3;
                                        initializeUser2 = initializeUser2;
                                        userIds = userIds;
                                    }
                                    userId = userId3;
                                    Slogf.i(TAG, "SecurityCom access is started, version: " + version + ", versionCode: " + versionCode);
                                    if (versionCode < 2211) {
                                        Slogf.e(TAG, "WARNING!!! Shutdown! securityCom is too old");
                                        try {
                                            pm.shutdown(false, "securitycom", false);
                                        } catch (RemoteException re3) {
                                            Slogf.e(TAG, "shutdown exception: " + re3);
                                        }
                                    }
                                } else {
                                    userId = userId3;
                                    Slogf.w(TAG, "WARNING!!! Shutdown! securityCom version is null");
                                    try {
                                        pm.shutdown(false, "securitycom", false);
                                    } catch (RemoteException re4) {
                                        Slogf.e(TAG, "shutdown exception: " + re4);
                                    }
                                }
                            }
                        }
                        final int userId4 = userId;
                        Slogf.i(TAG, "Posting BOOT_COMPLETED user #" + userId4);
                        if (userId4 == 0 && !this.mInjector.isRuntimeRestarted() && !this.mInjector.isFirstBootOrUpgrade()) {
                            long elapsedTimeMs = SystemClock.elapsedRealtime();
                            FrameworkStatsLog.write((int) FrameworkStatsLog.BOOT_TIME_EVENT_ELAPSED_TIME_REPORTED, 13, elapsedTimeMs);
                        }
                        final Intent bootIntent = new Intent("android.intent.action.BOOT_COMPLETED", (Uri) null);
                        bootIntent.putExtra("android.intent.extra.user_handle", userId4);
                        bootIntent.addFlags(-1996488704);
                        final int callingUid = Binder.getCallingUid();
                        final int callingPid = Binder.getCallingPid();
                        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda1
                            @Override // java.lang.Runnable
                            public final void run() {
                                UserController.this.m1519x7339a50f(bootIntent, userId4, callingUid, callingPid);
                            }
                        });
                    } catch (Throwable th5) {
                        th = th5;
                    }
                }
            } catch (Throwable th6) {
                th = th6;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishUserUnlockedCompleted$3$com-android-server-am-UserController  reason: not valid java name */
    public /* synthetic */ void m1518x11e70870(UserInfo userInfo) {
        this.mInjector.getUserManager().makeInitialized(userInfo.id);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishUserUnlockedCompleted$4$com-android-server-am-UserController  reason: not valid java name */
    public /* synthetic */ void m1519x7339a50f(Intent bootIntent, final int userId, int callingUid, int callingPid) {
        this.mInjector.broadcastIntent(bootIntent, null, new IIntentReceiver.Stub() { // from class: com.android.server.am.UserController.2
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
                Slogf.i(UserController.TAG, "Finished processing BOOT_COMPLETED for u" + userId);
                UserController.this.mBootCompleted = true;
            }
        }, 0, null, null, new String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, getTemporaryAppAllowlistBroadcastOptions(200).toBundle(), true, false, ActivityManagerService.MY_PID, 1000, callingUid, callingPid, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.am.UserController$3  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 implements UserState.KeyEvictedCallback {
        final /* synthetic */ boolean val$foreground;

        AnonymousClass3(boolean z) {
            this.val$foreground = z;
        }

        @Override // com.android.server.am.UserState.KeyEvictedCallback
        public void keyEvicted(final int userId) {
            Handler handler = UserController.this.mHandler;
            final boolean z = this.val$foreground;
            handler.post(new Runnable() { // from class: com.android.server.am.UserController$3$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    UserController.AnonymousClass3.this.m1527lambda$keyEvicted$0$comandroidserveramUserController$3(userId, z);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$keyEvicted$0$com-android-server-am-UserController$3  reason: not valid java name */
        public /* synthetic */ void m1527lambda$keyEvicted$0$comandroidserveramUserController$3(int userId, boolean foreground) {
            UserController.this.startUser(userId, foreground);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int restartUser(int userId, boolean foreground) {
        return stopUser(userId, true, false, null, new AnonymousClass3(foreground));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean stopProfile(int userId) {
        boolean z;
        if (this.mInjector.checkCallingPermission("android.permission.MANAGE_USERS") == -1 && this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == -1) {
            throw new SecurityException("You either need MANAGE_USERS or INTERACT_ACROSS_USERS_FULL permission to stop a profile");
        }
        UserInfo userInfo = getUserInfo(userId);
        if (userInfo == null || !userInfo.isProfile()) {
            throw new IllegalArgumentException("User " + userId + " is not a profile");
        }
        enforceShellRestriction("no_debugging_features", userId);
        synchronized (this.mLock) {
            z = stopUsersLU(userId, true, false, null, null) == 0;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int stopUser(int userId, boolean force, boolean allowDelayedLocking, IStopUserCallback stopUserCallback, UserState.KeyEvictedCallback keyEvictedCallback) {
        int stopUsersLU;
        checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "stopUser");
        if (userId < 0 || userId == 0) {
            throw new IllegalArgumentException("Can't stop system user " + userId);
        }
        enforceShellRestriction("no_debugging_features", userId);
        synchronized (this.mLock) {
            stopUsersLU = stopUsersLU(userId, force, allowDelayedLocking, stopUserCallback, keyEvictedCallback);
        }
        return stopUsersLU;
    }

    private int stopUsersLU(int userId, boolean force, boolean allowDelayedLocking, IStopUserCallback stopUserCallback, UserState.KeyEvictedCallback keyEvictedCallback) {
        if (userId == 0) {
            return -3;
        }
        if (isCurrentUserLU(userId)) {
            return -2;
        }
        int[] usersToStop = getUsersToStopLU(userId);
        for (int relatedUserId : usersToStop) {
            if (relatedUserId == 0 || isCurrentUserLU(relatedUserId)) {
                if (ActivityManagerDebugConfig.DEBUG_MU) {
                    Slogf.i(TAG, "stopUsersLocked cannot stop related user " + relatedUserId);
                }
                if (force) {
                    Slogf.i(TAG, "Force stop user " + userId + ". Related users will not be stopped");
                    stopSingleUserLU(userId, allowDelayedLocking, stopUserCallback, keyEvictedCallback);
                    return 0;
                }
                return -4;
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slogf.i(TAG, "stopUsersLocked usersToStop=" + Arrays.toString(usersToStop));
        }
        int length = usersToStop.length;
        for (int i = 0; i < length; i++) {
            int userIdToStop = usersToStop[i];
            UserState.KeyEvictedCallback keyEvictedCallback2 = null;
            IStopUserCallback iStopUserCallback = userIdToStop == userId ? stopUserCallback : null;
            if (userIdToStop == userId) {
                keyEvictedCallback2 = keyEvictedCallback;
            }
            stopSingleUserLU(userIdToStop, allowDelayedLocking, iStopUserCallback, keyEvictedCallback2);
        }
        return 0;
    }

    private void stopSingleUserLU(final int userId, final boolean allowDelayedLocking, final IStopUserCallback stopUserCallback, UserState.KeyEvictedCallback keyEvictedCallback) {
        ArrayList<UserState.KeyEvictedCallback> keyEvictedCallbacks;
        Slogf.i(TAG, "stopSingleUserLU userId=" + userId);
        final UserState uss = this.mStartedUsers.get(userId);
        if (uss == null) {
            if (this.mDelayUserDataLocking) {
                if (allowDelayedLocking && keyEvictedCallback != null) {
                    Slogf.wtf(TAG, "allowDelayedLocking set with KeyEvictedCallback, ignore it and lock user:" + userId, new RuntimeException());
                    allowDelayedLocking = false;
                }
                if (!allowDelayedLocking && this.mLastActiveUsers.remove(Integer.valueOf(userId))) {
                    if (keyEvictedCallback != null) {
                        keyEvictedCallbacks = new ArrayList<>(1);
                        keyEvictedCallbacks.add(keyEvictedCallback);
                    } else {
                        keyEvictedCallbacks = null;
                    }
                    dispatchUserLocking(userId, keyEvictedCallbacks);
                }
            }
            if (stopUserCallback != null) {
                this.mHandler.post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda8
                    @Override // java.lang.Runnable
                    public final void run() {
                        stopUserCallback.userStopped(userId);
                    }
                });
                return;
            }
            return;
        }
        logUserJourneyInfo(null, getUserInfo(userId), 5);
        logUserLifecycleEvent(userId, 7, 1);
        if (stopUserCallback != null) {
            uss.mStopCallbacks.add(stopUserCallback);
        }
        if (keyEvictedCallback != null) {
            uss.mKeyEvictedCallbacks.add(keyEvictedCallback);
        }
        if (uss.state != 4 && uss.state != 5) {
            uss.setState(4);
            this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
            updateStartedUserArrayLU();
            final Runnable finishUserStoppingAsync = new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    UserController.this.m1525lambda$stopSingleUserLU$7$comandroidserveramUserController(userId, uss, allowDelayedLocking);
                }
            };
            if (this.mInjector.getUserManager().isPreCreated(userId)) {
                finishUserStoppingAsync.run();
            } else {
                this.mHandler.post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda10
                    @Override // java.lang.Runnable
                    public final void run() {
                        UserController.this.m1526lambda$stopSingleUserLU$8$comandroidserveramUserController(userId, finishUserStoppingAsync);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$stopSingleUserLU$7$com-android-server-am-UserController  reason: not valid java name */
    public /* synthetic */ void m1525lambda$stopSingleUserLU$7$comandroidserveramUserController(final int userId, final UserState uss, final boolean allowDelayedLockingCopied) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                UserController.this.m1524lambda$stopSingleUserLU$6$comandroidserveramUserController(userId, uss, allowDelayedLockingCopied);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$stopSingleUserLU$8$com-android-server-am-UserController  reason: not valid java name */
    public /* synthetic */ void m1526lambda$stopSingleUserLU$8$comandroidserveramUserController(int userId, final Runnable finishUserStoppingAsync) {
        Intent stoppingIntent = new Intent("android.intent.action.USER_STOPPING");
        stoppingIntent.addFlags(1073741824);
        stoppingIntent.putExtra("android.intent.extra.user_handle", userId);
        stoppingIntent.putExtra("android.intent.extra.SHUTDOWN_USERSPACE_ONLY", true);
        IIntentReceiver stoppingReceiver = new IIntentReceiver.Stub() { // from class: com.android.server.am.UserController.4
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                finishUserStoppingAsync.run();
            }
        };
        this.mInjector.clearBroadcastQueueForUser(userId);
        this.mInjector.broadcastIntent(stoppingIntent, null, stoppingReceiver, 0, null, null, new String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), -1);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: finishUserStopping */
    public void m1524lambda$stopSingleUserLU$6$comandroidserveramUserController(int userId, final UserState uss, final boolean allowDelayedLocking) {
        EventLog.writeEvent((int) EventLogTags.UC_FINISH_USER_STOPPING, userId);
        synchronized (this.mLock) {
            if (uss.state != 4) {
                logUserLifecycleEvent(userId, 7, 0);
                clearSessionId(userId);
                return;
            }
            uss.setState(5);
            this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
            this.mInjector.batteryStatsServiceNoteEvent(16391, Integer.toString(userId), userId);
            this.mInjector.getSystemServiceManager().onUserStopping(userId);
            final Runnable finishUserStoppedAsync = new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    UserController.this.m1514x50c5c758(uss, allowDelayedLocking);
                }
            };
            if (this.mInjector.getUserManager().isPreCreated(userId)) {
                finishUserStoppedAsync.run();
                return;
            }
            Intent shutdownIntent = new Intent("android.intent.action.ACTION_SHUTDOWN");
            IIntentReceiver shutdownReceiver = new IIntentReceiver.Stub() { // from class: com.android.server.am.UserController.5
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                    finishUserStoppedAsync.run();
                }
            };
            this.mInjector.broadcastIntent(shutdownIntent, null, shutdownReceiver, 0, null, null, null, -1, null, true, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$finishUserStopping$10$com-android-server-am-UserController  reason: not valid java name */
    public /* synthetic */ void m1514x50c5c758(final UserState uss, final boolean allowDelayedLocking) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                UserController.this.m1515lambda$finishUserStopping$9$comandroidserveramUserController(uss, allowDelayedLocking);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: finishUserStopped */
    public void m1515lambda$finishUserStopping$9$comandroidserveramUserController(UserState uss, boolean allowDelayedLocking) {
        ArrayList<IStopUserCallback> stopCallbacks;
        ArrayList<UserState.KeyEvictedCallback> keyEvictedCallbacks;
        boolean stopped;
        int userId = uss.mHandle.getIdentifier();
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slogf.i(TAG, "finishUserStopped(%d): allowDelayedLocking=%b", Integer.valueOf(userId), Boolean.valueOf(allowDelayedLocking));
        }
        EventLog.writeEvent((int) EventLogTags.UC_FINISH_USER_STOPPED, userId);
        boolean lockUser = true;
        int userIdToLock = userId;
        UserInfo userInfo = getUserInfo(userId);
        synchronized (this.mLock) {
            stopCallbacks = new ArrayList<>(uss.mStopCallbacks);
            keyEvictedCallbacks = new ArrayList<>(uss.mKeyEvictedCallbacks);
            if (this.mStartedUsers.get(userId) == uss && uss.state == 5) {
                stopped = true;
                this.mStartedUsers.remove(userId);
                this.mUserLru.remove(Integer.valueOf(userId));
                updateStartedUserArrayLU();
                if (allowDelayedLocking && !keyEvictedCallbacks.isEmpty()) {
                    Slogf.wtf(TAG, "Delayed locking enabled while KeyEvictedCallbacks not empty, userId:" + userId + " callbacks:" + keyEvictedCallbacks);
                    allowDelayedLocking = false;
                }
                userIdToLock = updateUserToLockLU(userId, allowDelayedLocking);
                if (userIdToLock == -10000) {
                    lockUser = false;
                }
            }
            stopped = false;
        }
        if (stopped) {
            this.mInjector.getUserManagerInternal().removeUserState(userId);
            this.mInjector.activityManagerOnUserStopped(userId);
            forceStopUser(userId, "finish user");
        }
        Iterator<IStopUserCallback> it = stopCallbacks.iterator();
        while (it.hasNext()) {
            IStopUserCallback callback = it.next();
            if (stopped) {
                try {
                    callback.userStopped(userId);
                } catch (RemoteException e) {
                }
            } else {
                callback.userStopAborted(userId);
            }
        }
        if (stopped) {
            this.mInjector.systemServiceManagerOnUserStopped(userId);
            this.mInjector.taskSupervisorRemoveUser(userId);
            if (userInfo.isEphemeral() && !userInfo.preCreated) {
                this.mInjector.getUserManager().removeUserEvenWhenDisallowed(userId);
            }
            logUserLifecycleEvent(userId, 7, 2);
            clearSessionId(userId);
            if (!lockUser) {
                return;
            }
            dispatchUserLocking(userIdToLock, keyEvictedCallbacks);
            return;
        }
        logUserLifecycleEvent(userId, 7, 0);
        clearSessionId(userId);
    }

    private void dispatchUserLocking(final int userId, final List<UserState.KeyEvictedCallback> keyEvictedCallbacks) {
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                UserController.this.m1513x38006fbd(userId, keyEvictedCallbacks);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dispatchUserLocking$11$com-android-server-am-UserController  reason: not valid java name */
    public /* synthetic */ void m1513x38006fbd(int userId, List keyEvictedCallbacks) {
        synchronized (this.mLock) {
            if (this.mStartedUsers.get(userId) != null) {
                Slogf.w(TAG, "User was restarted, skipping key eviction");
                return;
            }
            try {
                this.mInjector.getStorageManager().lockUserKey(userId);
                if (keyEvictedCallbacks == null) {
                    return;
                }
                for (int i = 0; i < keyEvictedCallbacks.size(); i++) {
                    ((UserState.KeyEvictedCallback) keyEvictedCallbacks.get(i)).keyEvicted(userId);
                }
            } catch (RemoteException re) {
                throw re.rethrowAsRuntimeException();
            }
        }
    }

    private int updateUserToLockLU(int userId, boolean allowDelayedLocking) {
        if (!this.mDelayUserDataLocking || !allowDelayedLocking || getUserInfo(userId).isEphemeral() || hasUserRestriction("no_run_in_background", userId)) {
            return userId;
        }
        this.mLastActiveUsers.remove(Integer.valueOf(userId));
        this.mLastActiveUsers.add(0, Integer.valueOf(userId));
        int totalUnlockedUsers = this.mStartedUsers.size() + this.mLastActiveUsers.size();
        if (totalUnlockedUsers > this.mMaxRunningUsers) {
            ArrayList<Integer> arrayList = this.mLastActiveUsers;
            int userIdToLock = arrayList.get(arrayList.size() - 1).intValue();
            ArrayList<Integer> arrayList2 = this.mLastActiveUsers;
            arrayList2.remove(arrayList2.size() - 1);
            Slogf.i(TAG, "finishUserStopped, stopping user:" + userId + " lock user:" + userIdToLock);
            return userIdToLock;
        }
        Slogf.i(TAG, "finishUserStopped, user:" + userId + ", skip locking");
        return -10000;
    }

    private int[] getUsersToStopLU(int userId) {
        int startedUsersSize = this.mStartedUsers.size();
        IntArray userIds = new IntArray();
        userIds.add(userId);
        int userGroupId = this.mUserProfileGroupIds.get(userId, -10000);
        for (int i = 0; i < startedUsersSize; i++) {
            UserState uss = this.mStartedUsers.valueAt(i);
            int startedUserId = uss.mHandle.getIdentifier();
            int startedUserGroupId = this.mUserProfileGroupIds.get(startedUserId, -10000);
            boolean sameGroup = userGroupId != -10000 && userGroupId == startedUserGroupId;
            boolean sameUserId = startedUserId == userId;
            if (sameGroup && !sameUserId) {
                userIds.add(startedUserId);
            }
        }
        return userIds.toArray();
    }

    private void forceStopUser(int userId, String reason) {
        UserInfo parent;
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slogf.i(TAG, "forceStopUser(%d): %s", Integer.valueOf(userId), reason);
        }
        this.mInjector.activityManagerForceStopPackage(userId, reason);
        if (this.mInjector.getUserManager().isPreCreated(userId)) {
            return;
        }
        Intent intent = new Intent("android.intent.action.USER_STOPPED");
        intent.addFlags(1342177280);
        intent.putExtra("android.intent.extra.user_handle", userId);
        this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), -1);
        UserInfo userInfo = getUserInfo(userId);
        if (userInfo.isProfile() && (parent = this.mInjector.getUserManager().getProfileParent(userId)) != null) {
            broadcastProfileAccessibleStateChanged(userId, parent.id, "android.intent.action.PROFILE_INACCESSIBLE");
        }
    }

    private void stopGuestOrEphemeralUserIfBackground(int oldUserId) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slogf.i(TAG, "Stop guest or ephemeral user if background: " + oldUserId);
        }
        synchronized (this.mLock) {
            UserState oldUss = this.mStartedUsers.get(oldUserId);
            if (oldUserId != 0 && oldUserId != this.mCurrentUserId && oldUss != null && oldUss.state != 4 && oldUss.state != 5) {
                UserInfo userInfo = getUserInfo(oldUserId);
                if (userInfo.isEphemeral()) {
                    ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).onEphemeralUserStop(oldUserId);
                }
                if (userInfo.isGuest() || userInfo.isEphemeral()) {
                    synchronized (this.mLock) {
                        stopUsersLU(oldUserId, true, false, null, null);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleStartProfiles() {
        FgThread.getHandler().post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                UserController.this.m1522x6d8259ff();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleStartProfiles$12$com-android-server-am-UserController  reason: not valid java name */
    public /* synthetic */ void m1522x6d8259ff() {
        if (!this.mHandler.hasMessages(40)) {
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(40), 1000L);
        }
    }

    private void startProfiles() {
        int currentUserId = getCurrentUserId();
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slogf.i(TAG, "startProfilesLocked");
        }
        List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(currentUserId, false);
        List<UserInfo> profilesToStart = new ArrayList<>(profiles.size());
        for (UserInfo user : profiles) {
            if ((user.flags & 16) == 16 && user.id != currentUserId && !user.isQuietModeEnabled()) {
                profilesToStart.add(user);
            }
        }
        int profilesToStartSize = profilesToStart.size();
        int i = 0;
        while (i < profilesToStartSize && i < getMaxRunningUsers() - 1) {
            startUser(profilesToStart.get(i).id, false);
            i++;
        }
        if (i < profilesToStartSize) {
            Slogf.w(TAG, "More profiles than MAX_RUNNING_USERS");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startProfile(int userId) {
        if (this.mInjector.checkCallingPermission("android.permission.MANAGE_USERS") == -1 && this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == -1) {
            throw new SecurityException("You either need MANAGE_USERS or INTERACT_ACROSS_USERS_FULL permission to start a profile");
        }
        UserInfo userInfo = getUserInfo(userId);
        if (userInfo == null || !userInfo.isProfile()) {
            throw new IllegalArgumentException("User " + userId + " is not a profile");
        }
        if (!userInfo.isEnabled()) {
            Slogf.w(TAG, "Cannot start disabled profile #" + userId);
            return false;
        }
        return startUserNoChecks(userId, false, null);
    }

    boolean startUser(int userId, boolean foreground) {
        return m1523lambda$startUserInternal$13$comandroidserveramUserController(userId, foreground, null);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: startUser */
    public boolean m1523lambda$startUserInternal$13$comandroidserveramUserController(int userId, boolean foreground, IProgressListener unlockListener) {
        checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "startUser");
        return startUserNoChecks(userId, foreground, unlockListener);
    }

    private boolean startUserNoChecks(int userId, boolean foreground, IProgressListener unlockListener) {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        t.traceBegin("UserController.startUser-" + userId + "-" + (foreground ? "fg" : "bg"));
        try {
            return startUserInternal(userId, foreground, unlockListener, t);
        } finally {
            t.traceEnd();
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1808=10] */
    private boolean startUserInternal(final int userId, final boolean foreground, final IProgressListener unlockListener, TimingsTraceAndSlog t) {
        boolean updateUmState;
        UserState uss;
        UserState uss2;
        int oldUserId;
        boolean z;
        int i;
        TimingsTraceAndSlog timingsTraceAndSlog;
        boolean userSwitchUiEnabled;
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Object[] objArr = new Object[2];
            objArr[0] = Integer.valueOf(userId);
            objArr[1] = foreground ? " in foreground" : "";
            Slogf.i(TAG, "Starting user %d%s", objArr);
        }
        EventLog.writeEvent((int) EventLogTags.UC_START_USER_INTERNAL, userId);
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long ident = Binder.clearCallingIdentity();
        try {
            t.traceBegin("getStartedUserState");
            int oldUserId2 = getCurrentUserId();
            if (oldUserId2 == userId) {
                UserState state = getStartedUserState(userId);
                if (state == null) {
                    Slogf.wtf(TAG, "Current user has no UserState");
                } else if (userId != 0 || state.state != 0) {
                    if (state.state == 3) {
                        notifyFinished(userId, unlockListener);
                    }
                    t.traceEnd();
                    Binder.restoreCallingIdentity(ident);
                    return true;
                }
            }
            t.traceEnd();
            if (foreground) {
                t.traceBegin("clearAllLockedTasks");
                this.mInjector.clearAllLockedTasks("startUser");
                t.traceEnd();
            }
            t.traceBegin("getUserInfo");
            UserInfo userInfo = getUserInfo(userId);
            t.traceEnd();
            if (userInfo == null) {
                Slogf.w(TAG, "No user info for user #" + userId);
                Binder.restoreCallingIdentity(ident);
                return false;
            } else if (foreground && userInfo.isProfile()) {
                Slogf.w(TAG, "Cannot switch to User #" + userId + ": not a full user");
                Binder.restoreCallingIdentity(ident);
                return false;
            } else if (foreground && userInfo.preCreated) {
                Slogf.w(TAG, "Cannot start pre-created user #" + userId + " as foreground");
                Binder.restoreCallingIdentity(ident);
                return false;
            } else {
                if (foreground && isUserSwitchUiEnabled()) {
                    t.traceBegin("startFreezingScreen");
                    this.mInjector.getWindowManager().startFreezingScreen(17432743, 17432742);
                    t.traceEnd();
                }
                boolean needStart = false;
                t.traceBegin("updateStartedUserArrayStarting");
                try {
                    synchronized (this.mLock) {
                        try {
                            UserState uss3 = this.mStartedUsers.get(userId);
                            try {
                                if (uss3 == null) {
                                    UserState uss4 = new UserState(UserHandle.of(userId));
                                    uss4.mUnlockProgress.addListener(new UserProgressListener());
                                    this.mStartedUsers.put(userId, uss4);
                                    updateStartedUserArrayLU();
                                    needStart = true;
                                    updateUmState = true;
                                    uss = uss4;
                                } else if (uss3.state == 5 && !isCallingOnHandlerThread()) {
                                    Slogf.i(TAG, "User #" + userId + " is shutting down - will start after full stop");
                                    this.mHandler.post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda6
                                        @Override // java.lang.Runnable
                                        public final void run() {
                                            UserController.this.m1523lambda$startUserInternal$13$comandroidserveramUserController(userId, foreground, unlockListener);
                                        }
                                    });
                                    t.traceEnd();
                                    Binder.restoreCallingIdentity(ident);
                                    return true;
                                } else {
                                    updateUmState = false;
                                    uss = uss3;
                                }
                                try {
                                    Integer userIdInt = Integer.valueOf(userId);
                                    this.mUserLru.remove(userIdInt);
                                    this.mUserLru.add(userIdInt);
                                    if (unlockListener != null) {
                                        uss.mUnlockProgress.addListener(unlockListener);
                                    }
                                    t.traceEnd();
                                    if (updateUmState) {
                                        t.traceBegin("setUserState");
                                        this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
                                        t.traceEnd();
                                    }
                                    t.traceBegin("updateConfigurationAndProfileIds");
                                    if (foreground) {
                                        this.mInjector.reportGlobalUsageEvent(16);
                                        synchronized (this.mLock) {
                                            this.mCurrentUserId = userId;
                                            this.mTargetUserId = -10000;
                                            userSwitchUiEnabled = this.mUserSwitchUiEnabled;
                                        }
                                        this.mInjector.updateUserConfiguration();
                                        updateCurrentProfileIds();
                                        this.mInjector.getWindowManager().setCurrentUser(userId, getCurrentProfileIds());
                                        this.mInjector.reportCurWakefulnessUsageEvent();
                                        if (userSwitchUiEnabled) {
                                            this.mInjector.getWindowManager().setSwitchingUser(true);
                                            if (this.mInjector.getKeyguardManager().isDeviceSecure(userId)) {
                                                this.mInjector.getWindowManager().lockNow(null);
                                            }
                                        }
                                    } else {
                                        Integer currentUserIdInt = Integer.valueOf(this.mCurrentUserId);
                                        updateCurrentProfileIds();
                                        this.mInjector.getWindowManager().setCurrentProfileIds(getCurrentProfileIds());
                                        synchronized (this.mLock) {
                                            this.mUserLru.remove(currentUserIdInt);
                                            this.mUserLru.add(currentUserIdInt);
                                        }
                                    }
                                    t.traceEnd();
                                    if (uss.state == 4) {
                                        t.traceBegin("updateStateStopping");
                                        uss.setState(uss.lastState);
                                        this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
                                        synchronized (this.mLock) {
                                            updateStartedUserArrayLU();
                                        }
                                        needStart = true;
                                        t.traceEnd();
                                    } else if (uss.state == 5) {
                                        t.traceBegin("updateStateShutdown");
                                        uss.setState(0);
                                        this.mInjector.getUserManagerInternal().setUserState(userId, uss.state);
                                        synchronized (this.mLock) {
                                            updateStartedUserArrayLU();
                                        }
                                        needStart = true;
                                        t.traceEnd();
                                    }
                                    if (uss.state == 0) {
                                        t.traceBegin("updateStateBooting");
                                        this.mInjector.getUserManager().onBeforeStartUser(userId);
                                        Handler handler = this.mHandler;
                                        handler.sendMessage(handler.obtainMessage(50, userId, 0));
                                        t.traceEnd();
                                    }
                                    t.traceBegin("sendMessages");
                                    if (foreground) {
                                        Handler handler2 = this.mHandler;
                                        handler2.sendMessage(handler2.obtainMessage(60, userId, oldUserId2));
                                        this.mHandler.removeMessages(10);
                                        this.mHandler.removeMessages(30);
                                        Handler handler3 = this.mHandler;
                                        handler3.sendMessage(handler3.obtainMessage(10, oldUserId2, userId, uss));
                                        Handler handler4 = this.mHandler;
                                        handler4.sendMessageDelayed(handler4.obtainMessage(30, oldUserId2, userId, uss), BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS);
                                    }
                                    boolean needStart2 = userInfo.preCreated ? false : needStart;
                                    if (needStart2) {
                                        try {
                                            Intent intent = new Intent("android.intent.action.USER_STARTED");
                                            intent.addFlags(1342177280);
                                            intent.putExtra("android.intent.extra.user_handle", userId);
                                            uss2 = uss;
                                            oldUserId = oldUserId2;
                                            z = true;
                                            this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, callingUid, callingPid, userId);
                                        } catch (Throwable th) {
                                            th = th;
                                            Binder.restoreCallingIdentity(ident);
                                            throw th;
                                        }
                                    } else {
                                        uss2 = uss;
                                        oldUserId = oldUserId2;
                                        z = true;
                                    }
                                    t.traceEnd();
                                    if (foreground) {
                                        timingsTraceAndSlog = t;
                                        try {
                                            timingsTraceAndSlog.traceBegin("moveUserToForeground");
                                            this.mHandler.removeMessages(150);
                                            Handler handler5 = this.mHandler;
                                            i = userId;
                                            handler5.sendMessageDelayed(handler5.obtainMessage(150, oldUserId, i, uss2), 1000L);
                                            t.traceEnd();
                                        } catch (Throwable th2) {
                                            th = th2;
                                            Binder.restoreCallingIdentity(ident);
                                            throw th;
                                        }
                                    } else {
                                        i = userId;
                                        timingsTraceAndSlog = t;
                                        timingsTraceAndSlog.traceBegin("finishUserBoot");
                                        finishUserBoot(uss2);
                                        t.traceEnd();
                                    }
                                    if (needStart2) {
                                        timingsTraceAndSlog.traceBegin("sendRestartBroadcast");
                                        Intent intent2 = new Intent("android.intent.action.USER_STARTING");
                                        intent2.addFlags(1073741824);
                                        intent2.putExtra("android.intent.extra.user_handle", i);
                                        this.mInjector.broadcastIntent(intent2, null, new IIntentReceiver.Stub() { // from class: com.android.server.am.UserController.6
                                            public void performReceive(Intent intent3, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
                                            }
                                        }, 0, null, null, new String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, callingUid, callingPid, -1);
                                        t.traceEnd();
                                    }
                                    Binder.restoreCallingIdentity(ident);
                                    return z;
                                } catch (Throwable th3) {
                                    th = th3;
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th4) {
                                            th = th4;
                                        }
                                    }
                                    throw th;
                                }
                            } catch (Throwable th5) {
                                th = th5;
                            }
                        } catch (Throwable th6) {
                            th = th6;
                        }
                    }
                } catch (Throwable th7) {
                    th = th7;
                }
            }
        } catch (Throwable th8) {
            th = th8;
        }
    }

    private boolean isCallingOnHandlerThread() {
        return Looper.myLooper() == this.mHandler.getLooper();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startUserInForeground(int targetUserId) {
        boolean success = startUser(targetUserId, true);
        if (!success) {
            this.mInjector.getWindowManager().setSwitchingUser(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean unlockUser(int userId, byte[] secret, IProgressListener listener) {
        checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "unlockUser");
        EventLog.writeEvent((int) EventLogTags.UC_UNLOCK_USER, userId);
        long binderToken = Binder.clearCallingIdentity();
        try {
            return unlockUserCleared(userId, secret, listener);
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    private boolean maybeUnlockUser(int userId) {
        return unlockUserCleared(userId, null, null);
    }

    private static void notifyFinished(int userId, IProgressListener listener) {
        if (listener == null) {
            return;
        }
        try {
            listener.onFinished(userId, (Bundle) null);
        } catch (RemoteException e) {
        }
    }

    private boolean unlockUserCleared(int userId, byte[] secret, IProgressListener listener) {
        UserState uss;
        int[] userIds;
        if (!StorageManager.isUserKeyUnlocked(userId)) {
            UserInfo userInfo = getUserInfo(userId);
            IStorageManager storageManager = this.mInjector.getStorageManager();
            try {
                storageManager.unlockUserKey(userId, userInfo.serialNumber, secret);
            } catch (RemoteException | RuntimeException e) {
                Slogf.w(TAG, "Failed to unlock: " + e.getMessage());
            }
        }
        synchronized (this.mLock) {
            uss = this.mStartedUsers.get(userId);
            if (uss != null) {
                uss.mUnlockProgress.addListener(listener);
            }
        }
        if (uss == null) {
            notifyFinished(userId, listener);
            return false;
        }
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        t.traceBegin("finishUserUnlocking-" + userId);
        boolean finishUserUnlockingResult = finishUserUnlocking(uss);
        t.traceEnd();
        if (!finishUserUnlockingResult) {
            notifyFinished(userId, listener);
            return false;
        }
        synchronized (this.mLock) {
            userIds = new int[this.mStartedUsers.size()];
            for (int i = 0; i < userIds.length; i++) {
                userIds[i] = this.mStartedUsers.keyAt(i);
            }
        }
        for (int testUserId : userIds) {
            UserInfo parent = this.mInjector.getUserManager().getProfileParent(testUserId);
            if (parent != null && parent.id == userId && testUserId != userId) {
                Slogf.d(TAG, "User " + testUserId + " (parent " + parent.id + "): attempting unlock because parent was just unlocked");
                maybeUnlockUser(testUserId);
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean switchUser(int targetUserId) {
        enforceShellRestriction("no_debugging_features", targetUserId);
        EventLog.writeEvent((int) EventLogTags.UC_SWITCH_USER, targetUserId);
        if (DEBUG_Fans) {
            int i = this.switchNum + 1;
            this.switchNum = i;
            if (i >= 2 && SystemClock.uptimeMillis() - this.switchTime < 60000) {
                try {
                    if (ActivityManager.getService() != null) {
                        Log.d(TAG, "switch user too frequent, switch time duration = " + (SystemClock.uptimeMillis() - this.switchTime));
                        ActivityManager.getService().startTNE("0x007a0031", 512L, 0, "");
                    }
                } catch (RemoteException e) {
                    Log.d(TAG, "Can't call IActivityManager");
                }
                this.switchNum = 1;
            }
            this.switchTime = SystemClock.uptimeMillis();
        }
        int currentUserId = getCurrentUserId();
        UserInfo targetUserInfo = getUserInfo(targetUserId);
        if (targetUserId == currentUserId) {
            Slogf.i(TAG, "user #" + targetUserId + " is already the current user");
            return true;
        } else if (targetUserInfo == null) {
            Slogf.w(TAG, "No user info for user #" + targetUserId);
            return false;
        } else if (!targetUserInfo.supportsSwitchTo()) {
            Slogf.w(TAG, "Cannot switch to User #" + targetUserId + ": not supported");
            return false;
        } else if (targetUserInfo.isProfile()) {
            Slogf.w(TAG, "Cannot switch to User #" + targetUserId + ": not a full user");
            return false;
        } else if (FactoryResetter.isFactoryResetting()) {
            Slogf.w(TAG, "Cannot switch to User #" + targetUserId + ": factory reset in progress");
            return false;
        } else {
            synchronized (this.mLock) {
                if (!this.mInitialized) {
                    Slogf.e(TAG, "Cannot switch to User #" + targetUserId + ": UserController not ready yet");
                    return false;
                }
                this.mTargetUserId = targetUserId;
                boolean userSwitchUiEnabled = this.mUserSwitchUiEnabled;
                if (userSwitchUiEnabled) {
                    UserInfo currentUserInfo = getUserInfo(currentUserId);
                    Pair<UserInfo, UserInfo> userNames = new Pair<>(currentUserInfo, targetUserInfo);
                    this.mUiHandler.removeMessages(1000);
                    Handler handler = this.mUiHandler;
                    handler.sendMessage(handler.obtainMessage(1000, userNames));
                } else {
                    this.mHandler.removeMessages(120);
                    Handler handler2 = this.mHandler;
                    handler2.sendMessage(handler2.obtainMessage(120, targetUserId, 0));
                }
                return true;
            }
        }
    }

    private void showUserSwitchDialog(Pair<UserInfo, UserInfo> fromToUserPair) {
        this.mInjector.showUserSwitchingDialog((UserInfo) fromToUserPair.first, (UserInfo) fromToUserPair.second, getSwitchingFromSystemUserMessageUnchecked(), getSwitchingToSystemUserMessageUnchecked());
    }

    private void dispatchForegroundProfileChanged(int userId) {
        ITranActivityManagerService.Instance().onForegroundProfileSwitch(userId);
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        for (int i = 0; i < observerCount; i++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i).onForegroundProfileSwitch(userId);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    void dispatchUserSwitchComplete(int userId) {
        ITranActivityManagerService.Instance().onUserSwitchComplete(userId);
        this.mInjector.getWindowManager().setSwitchingUser(false);
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        for (int i = 0; i < observerCount; i++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i).onUserSwitchComplete(userId);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    private void dispatchLockedBootComplete(int userId) {
        ITranActivityManagerService.Instance().onLockedBootComplete(userId);
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        for (int i = 0; i < observerCount; i++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i).onLockedBootComplete(userId);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x001f A[Catch: all -> 0x001a, TryCatch #0 {all -> 0x001a, blocks: (B:8:0x0011, B:16:0x001f, B:18:0x0023, B:19:0x0033, B:21:0x0035, B:23:0x0039, B:24:0x0049, B:25:0x0053), top: B:29:0x0011 }] */
    /* JADX WARN: Removed duplicated region for block: B:21:0x0035 A[Catch: all -> 0x001a, TryCatch #0 {all -> 0x001a, blocks: (B:8:0x0011, B:16:0x001f, B:18:0x0023, B:19:0x0033, B:21:0x0035, B:23:0x0039, B:24:0x0049, B:25:0x0053), top: B:29:0x0011 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void stopUserOnSwitchIfEnforced(int oldUserId) {
        boolean disallowRunInBg;
        if (oldUserId == 0) {
            return;
        }
        boolean hasRestriction = hasUserRestriction("no_run_in_background", oldUserId);
        synchronized (this.mLock) {
            if (!hasRestriction) {
                try {
                    if (!shouldStopUserOnSwitch()) {
                        disallowRunInBg = false;
                        if (disallowRunInBg) {
                            if (ActivityManagerDebugConfig.DEBUG_MU) {
                                Slogf.i(TAG, "stopUserOnSwitchIfEnforced() NOT stopping %d and related users", Integer.valueOf(oldUserId));
                            }
                            return;
                        }
                        if (ActivityManagerDebugConfig.DEBUG_MU) {
                            Slogf.i(TAG, "stopUserOnSwitchIfEnforced() stopping %d and related users", Integer.valueOf(oldUserId));
                        }
                        stopUsersLU(oldUserId, false, true, null, null);
                        return;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            disallowRunInBg = true;
            if (disallowRunInBg) {
            }
        }
    }

    private void timeoutUserSwitch(UserState uss, int oldUserId, int newUserId) {
        synchronized (this.mLock) {
            Slogf.e(TAG, "User switch timeout: from " + oldUserId + " to " + newUserId);
            this.mTimeoutUserSwitchCallbacks = this.mCurWaitingUserSwitchCallbacks;
            this.mHandler.removeMessages(90);
            sendContinueUserSwitchLU(uss, oldUserId, newUserId);
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(90, oldUserId, newUserId), 5000L);
        }
    }

    private void timeoutUserSwitchCallbacks(int oldUserId, int newUserId) {
        synchronized (this.mLock) {
            ArraySet<String> arraySet = this.mTimeoutUserSwitchCallbacks;
            if (arraySet != null && !arraySet.isEmpty()) {
                Slogf.wtf(TAG, "User switch timeout: from " + oldUserId + " to " + newUserId + ". Observers that didn't respond: " + this.mTimeoutUserSwitchCallbacks);
                this.mTimeoutUserSwitchCallbacks = null;
            }
        }
    }

    void dispatchUserSwitch(final UserState uss, final int oldUserId, final int newUserId) {
        TimingsTraceAndSlog t;
        int i;
        ArraySet<String> curWaitingUserSwitchCallbacks;
        int observerCount;
        TimingsTraceAndSlog t2;
        TimingsTraceAndSlog t3 = new TimingsTraceAndSlog();
        t3.traceBegin("dispatchUserSwitch-" + oldUserId + "-to-" + newUserId);
        EventLog.writeEvent((int) EventLogTags.UC_DISPATCH_USER_SWITCH, Integer.valueOf(oldUserId), Integer.valueOf(newUserId));
        ITranActivityManagerService.Instance().onUserSwitching(oldUserId, newUserId);
        int observerCount2 = this.mUserSwitchObservers.beginBroadcast();
        if (observerCount2 <= 0) {
            t = t3;
            synchronized (this.mLock) {
                sendContinueUserSwitchLU(uss, oldUserId, newUserId);
            }
        } else {
            ArraySet<String> curWaitingUserSwitchCallbacks2 = new ArraySet<>();
            synchronized (this.mLock) {
                try {
                    uss.switching = true;
                    this.mCurWaitingUserSwitchCallbacks = curWaitingUserSwitchCallbacks2;
                } catch (Throwable th) {
                    th = th;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    throw th;
                }
            }
            final AtomicInteger waitingCallbacksCount = new AtomicInteger(observerCount2);
            final long dispatchStartedTime = SystemClock.elapsedRealtime();
            int i2 = 0;
            while (i2 < observerCount2) {
                final long dispatchStartedTimeForObserver = SystemClock.elapsedRealtime();
                try {
                    final String name = "#" + i2 + " " + this.mUserSwitchObservers.getBroadcastCookie(i2);
                    synchronized (this.mLock) {
                        curWaitingUserSwitchCallbacks2.add(name);
                    }
                    i = i2;
                    final ArraySet<String> arraySet = curWaitingUserSwitchCallbacks2;
                    curWaitingUserSwitchCallbacks = curWaitingUserSwitchCallbacks2;
                    observerCount = observerCount2;
                    t2 = t3;
                    try {
                        this.mUserSwitchObservers.getBroadcastItem(i).onUserSwitching(newUserId, new IRemoteCallback.Stub() { // from class: com.android.server.am.UserController.7
                            public void sendResult(Bundle data) throws RemoteException {
                                synchronized (UserController.this.mLock) {
                                    long delayForObserver = SystemClock.elapsedRealtime() - dispatchStartedTimeForObserver;
                                    if (delayForObserver > 500) {
                                        Slogf.w(UserController.TAG, "User switch slowed down by observer " + name + ": result took " + delayForObserver + " ms to process.");
                                    }
                                    long totalDelay = SystemClock.elapsedRealtime() - dispatchStartedTime;
                                    if (totalDelay > BackupAgentTimeoutParameters.DEFAULT_QUOTA_EXCEEDED_TIMEOUT_MILLIS) {
                                        Slogf.e(UserController.TAG, "User switch timeout: observer " + name + "'s result was received " + totalDelay + " ms after dispatchUserSwitch.");
                                    }
                                    arraySet.remove(name);
                                    if (waitingCallbacksCount.decrementAndGet() == 0 && arraySet == UserController.this.mCurWaitingUserSwitchCallbacks) {
                                        UserController.this.sendContinueUserSwitchLU(uss, oldUserId, newUserId);
                                    }
                                }
                            }
                        });
                    } catch (RemoteException e) {
                    }
                } catch (RemoteException e2) {
                    i = i2;
                    curWaitingUserSwitchCallbacks = curWaitingUserSwitchCallbacks2;
                    observerCount = observerCount2;
                    t2 = t3;
                }
                i2 = i + 1;
                curWaitingUserSwitchCallbacks2 = curWaitingUserSwitchCallbacks;
                observerCount2 = observerCount;
                t3 = t2;
            }
            t = t3;
        }
        this.mUserSwitchObservers.finishBroadcast();
        t.traceEnd();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendContinueUserSwitchLU(UserState uss, int oldUserId, int newUserId) {
        this.mCurWaitingUserSwitchCallbacks = null;
        this.mHandler.removeMessages(30);
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(20, oldUserId, newUserId, uss));
    }

    void continueUserSwitch(UserState uss, int oldUserId, int newUserId) {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        t.traceBegin("continueUserSwitch-" + oldUserId + "-to-" + newUserId);
        EventLog.writeEvent((int) EventLogTags.UC_CONTINUE_USER_SWITCH, Integer.valueOf(oldUserId), Integer.valueOf(newUserId));
        this.mHandler.removeMessages(130);
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(130, newUserId, 0));
        uss.switching = false;
        this.mHandler.removeMessages(80);
        Handler handler2 = this.mHandler;
        handler2.sendMessage(handler2.obtainMessage(80, newUserId, 0));
        stopGuestOrEphemeralUserIfBackground(oldUserId);
        stopUserOnSwitchIfEnforced(oldUserId);
        t.traceEnd();
    }

    void completeUserSwitch(int newUserId) {
        if (isUserSwitchUiEnabled()) {
            if (!this.mInjector.getKeyguardManager().isDeviceSecure(newUserId)) {
                this.mInjector.dismissKeyguard(new Runnable() { // from class: com.android.server.am.UserController.8
                    @Override // java.lang.Runnable
                    public void run() {
                        UserController.this.unfreezeScreen();
                    }
                }, "User Switch");
            } else {
                unfreezeScreen();
            }
        }
    }

    void unfreezeScreen() {
        TimingsTraceAndSlog t = new TimingsTraceAndSlog();
        t.traceBegin("stopFreezingScreen");
        this.mInjector.getWindowManager().stopFreezingScreen();
        t.traceEnd();
    }

    private void moveUserToForeground(UserState uss, int oldUserId, int newUserId) {
        boolean homeInFront = this.mInjector.taskSupervisorSwitchUser(newUserId, uss);
        if (homeInFront) {
            this.mInjector.startHomeActivity(newUserId, "moveUserToForeground");
        } else {
            this.mInjector.taskSupervisorResumeFocusedStackTopActivity();
        }
        EventLogTags.writeAmSwitchUser(newUserId);
        sendUserSwitchBroadcasts(oldUserId, newUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendUserSwitchBroadcasts(int oldUserId, int newUserId) {
        String str;
        String str2;
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        long ident = Binder.clearCallingIdentity();
        String str3 = "android.intent.extra.USER";
        String str4 = "android.intent.extra.user_handle";
        int i = 1342177280;
        if (oldUserId >= 0) {
            try {
                List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(oldUserId, false);
                int count = profiles.size();
                int i2 = 0;
                while (i2 < count) {
                    int profileUserId = profiles.get(i2).id;
                    Intent intent = new Intent("android.intent.action.USER_BACKGROUND");
                    intent.addFlags(i);
                    intent.putExtra(str4, profileUserId);
                    intent.putExtra(str3, UserHandle.of(profileUserId));
                    this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, callingUid, callingPid, profileUserId);
                    i2++;
                    count = count;
                    profiles = profiles;
                    str4 = str4;
                    str3 = str3;
                    i = 1342177280;
                }
                str = str4;
                str2 = str3;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        } else {
            str = "android.intent.extra.user_handle";
            str2 = "android.intent.extra.USER";
        }
        if (newUserId >= 0) {
            List<UserInfo> profiles2 = this.mInjector.getUserManager().getProfiles(newUserId, false);
            int count2 = profiles2.size();
            int i3 = 0;
            while (i3 < count2) {
                int profileUserId2 = profiles2.get(i3).id;
                Intent intent2 = new Intent("android.intent.action.USER_FOREGROUND");
                intent2.addFlags(1342177280);
                String str5 = str;
                intent2.putExtra(str5, profileUserId2);
                String str6 = str2;
                intent2.putExtra(str6, UserHandle.of(profileUserId2));
                this.mInjector.broadcastIntent(intent2, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, callingUid, callingPid, profileUserId2);
                i3++;
                count2 = count2;
                str2 = str6;
                str = str5;
            }
            Intent intent3 = new Intent("android.intent.action.USER_SWITCHED");
            intent3.addFlags(1342177280);
            intent3.putExtra(str, newUserId);
            intent3.putExtra(str2, UserHandle.of(newUserId));
            this.mInjector.broadcastIntent(intent3, null, null, 0, null, null, new String[]{"android.permission.MANAGE_USERS"}, -1, null, false, false, ActivityManagerService.MY_PID, 1000, callingUid, callingPid, -1);
        }
        Binder.restoreCallingIdentity(ident);
    }

    private void broadcastProfileAccessibleStateChanged(int userId, int parentId, String intentAction) {
        Intent intent = new Intent(intentAction);
        intent.putExtra("android.intent.extra.USER", UserHandle.of(userId));
        intent.addFlags(1342177280);
        this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, Binder.getCallingUid(), Binder.getCallingPid(), parentId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int handleIncomingUser(int callingPid, int callingUid, int userId, boolean allowAll, int allowMode, String name, String callerPackage) {
        int i;
        boolean allow;
        int callingUserId = UserHandle.getUserId(callingUid);
        if (callingUserId == userId) {
            return userId;
        }
        int targetUserId = unsafeConvertIncomingUser(userId);
        if (callingUid != 0 && callingUid != 1000 && !this.mInjector.getUserManager().isDualProfile(callingUserId)) {
            boolean isSameProfileGroup = isSameProfileGroup(callingUserId, targetUserId);
            if (this.mInjector.isCallerRecents(callingUid) && isSameProfileGroup) {
                allow = true;
                i = 2;
            } else {
                i = 2;
                if (this.mInjector.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS_FULL", callingPid, callingUid, -1, true) == 0) {
                    allow = true;
                } else if (allowMode == 2) {
                    allow = false;
                } else if (canInteractWithAcrossProfilesPermission(allowMode, isSameProfileGroup, callingPid, callingUid, callerPackage)) {
                    allow = true;
                } else if (this.mInjector.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS", callingPid, callingUid, -1, true) != 0) {
                    allow = false;
                } else if (allowMode == 0 || allowMode == 3) {
                    allow = true;
                } else if (allowMode == 1) {
                    allow = isSameProfileGroup;
                } else {
                    throw new IllegalArgumentException("Unknown mode: " + allowMode);
                }
            }
            if (!allow) {
                if (userId == -3) {
                    targetUserId = callingUserId;
                } else {
                    StringBuilder builder = new StringBuilder(128);
                    builder.append("Permission Denial: ");
                    builder.append(name);
                    if (callerPackage != null) {
                        builder.append(" from ");
                        builder.append(callerPackage);
                    }
                    builder.append(" asks to run as user ");
                    builder.append(userId);
                    builder.append(" but is calling from uid ");
                    UserHandle.formatUid(builder, callingUid);
                    builder.append("; this requires ");
                    builder.append("android.permission.INTERACT_ACROSS_USERS_FULL");
                    if (allowMode != i) {
                        if (allowMode == 0 || allowMode == 3 || (allowMode == 1 && isSameProfileGroup)) {
                            builder.append(" or ");
                            builder.append("android.permission.INTERACT_ACROSS_USERS");
                        }
                        if (isSameProfileGroup && allowMode == 3) {
                            builder.append(" or ");
                            builder.append("android.permission.INTERACT_ACROSS_PROFILES");
                        }
                    }
                    String msg = builder.toString();
                    Slogf.w(TAG, msg);
                    throw new SecurityException(msg);
                }
            }
        }
        if (!allowAll) {
            ensureNotSpecialUser(targetUserId);
        }
        if (callingUid == 2000 && targetUserId >= 0 && hasUserRestriction("no_debugging_features", targetUserId)) {
            throw new SecurityException("Shell does not have permission to access user " + targetUserId + "\n " + Debug.getCallers(3));
        }
        return targetUserId;
    }

    private boolean canInteractWithAcrossProfilesPermission(int allowMode, boolean isSameProfileGroup, int callingPid, int callingUid, String callingPackage) {
        if (allowMode != 3 || !isSameProfileGroup) {
            return false;
        }
        return this.mInjector.checkPermissionForPreflight("android.permission.INTERACT_ACROSS_PROFILES", callingPid, callingUid, callingPackage);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int unsafeConvertIncomingUser(int userId) {
        return (userId == -2 || userId == -3) ? getCurrentUserId() : userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void ensureNotSpecialUser(int userId) {
        if (userId >= 0) {
            return;
        }
        throw new IllegalArgumentException("Call does not support special user #" + userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerUserSwitchObserver(IUserSwitchObserver observer, String name) {
        Objects.requireNonNull(name, "Observer name cannot be null");
        checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "registerUserSwitchObserver");
        this.mUserSwitchObservers.register(observer, name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendForegroundProfileChanged(int userId) {
        this.mHandler.removeMessages(70);
        this.mHandler.obtainMessage(70, userId, 0).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterUserSwitchObserver(IUserSwitchObserver observer) {
        this.mUserSwitchObservers.unregister(observer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserState getStartedUserState(int userId) {
        UserState userState;
        synchronized (this.mLock) {
            userState = this.mStartedUsers.get(userId);
        }
        return userState;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasStartedUserState(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mStartedUsers.get(userId) != null;
        }
        return z;
    }

    private void updateStartedUserArrayLU() {
        int num = 0;
        for (int i = 0; i < this.mStartedUsers.size(); i++) {
            UserState uss = this.mStartedUsers.valueAt(i);
            if (uss.state != 4 && uss.state != 5) {
                num++;
            }
        }
        this.mStartedUserArray = new int[num];
        int num2 = 0;
        for (int i2 = 0; i2 < this.mStartedUsers.size(); i2++) {
            UserState uss2 = this.mStartedUsers.valueAt(i2);
            if (uss2.state != 4 && uss2.state != 5) {
                this.mStartedUserArray[num2] = this.mStartedUsers.keyAt(i2);
                num2++;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendBootCompleted(IIntentReceiver resultTo) {
        SparseArray<UserState> startedUsers;
        synchronized (this.mLock) {
            startedUsers = this.mStartedUsers.clone();
        }
        for (int i = 0; i < startedUsers.size(); i++) {
            UserState uss = startedUsers.valueAt(i);
            if (!UserManager.isHeadlessSystemUserMode()) {
                finishUserBoot(uss, resultTo);
            } else if (uss.mHandle.isSystem()) {
                sendLockedBootCompletedBroadcast(resultTo, uss.mHandle.getIdentifier());
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        updateCurrentProfileIds();
        this.mInjector.reportCurWakefulnessUsageEvent();
    }

    private void updateCurrentProfileIds() {
        List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(getCurrentUserId(), false);
        int[] currentProfileIds = new int[profiles.size()];
        for (int i = 0; i < currentProfileIds.length; i++) {
            currentProfileIds[i] = profiles.get(i).id;
        }
        List<UserInfo> users = this.mInjector.getUserManager().getUsers(false);
        synchronized (this.mLock) {
            this.mCurrentProfileIds = currentProfileIds;
            this.mUserProfileGroupIds.clear();
            for (int i2 = 0; i2 < users.size(); i2++) {
                UserInfo user = users.get(i2);
                if (user.profileGroupId != -10000) {
                    this.mUserProfileGroupIds.put(user.id, user.profileGroupId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] getStartedUserArray() {
        int[] iArr;
        synchronized (this.mLock) {
            iArr = this.mStartedUserArray;
        }
        return iArr;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUserRunning(int userId, int flags) {
        UserState state = getStartedUserState(userId);
        if (state == null) {
            return false;
        }
        if ((flags & 1) != 0) {
            return true;
        }
        if ((flags & 2) != 0) {
            switch (state.state) {
                case 0:
                case 1:
                    return true;
                default:
                    return false;
            }
        } else if ((flags & 8) != 0) {
            switch (state.state) {
                case 2:
                case 3:
                    return true;
                case 4:
                case 5:
                    return StorageManager.isUserKeyUnlocked(userId);
                default:
                    return false;
            }
        } else if ((flags & 4) == 0) {
            return (state.state == 4 || state.state == 5) ? false : true;
        } else {
            switch (state.state) {
                case 3:
                    return true;
                case 4:
                case 5:
                    return StorageManager.isUserKeyUnlocked(userId);
                default:
                    return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSystemUserStarted() {
        synchronized (this.mLock) {
            boolean z = false;
            UserState uss = this.mStartedUsers.get(0);
            if (uss == null) {
                return false;
            }
            if (uss.state == 1 || uss.state == 2 || uss.state == 3) {
                z = true;
            }
            return z;
        }
    }

    private void checkGetCurrentUserPermissions() {
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS") != 0 && this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String msg = "Permission Denial: getCurrentUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS";
            Slogf.w(TAG, msg);
            throw new SecurityException(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserInfo getCurrentUser() {
        UserInfo currentUserLU;
        checkGetCurrentUserPermissions();
        if (this.mTargetUserId == -10000) {
            return getUserInfo(this.mCurrentUserId);
        }
        synchronized (this.mLock) {
            currentUserLU = getCurrentUserLU();
        }
        return currentUserLU;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentUserIdChecked() {
        checkGetCurrentUserPermissions();
        if (this.mTargetUserId == -10000) {
            return this.mCurrentUserId;
        }
        return getCurrentOrTargetUserId();
    }

    private UserInfo getCurrentUserLU() {
        int userId = getCurrentOrTargetUserIdLU();
        return getUserInfo(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentOrTargetUserId() {
        int currentOrTargetUserIdLU;
        synchronized (this.mLock) {
            currentOrTargetUserIdLU = getCurrentOrTargetUserIdLU();
        }
        return currentOrTargetUserIdLU;
    }

    private int getCurrentOrTargetUserIdLU() {
        return this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId;
    }

    private int getCurrentUserIdLU() {
        return this.mCurrentUserId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentUserId() {
        int i;
        synchronized (this.mLock) {
            i = this.mCurrentUserId;
        }
        return i;
    }

    private boolean isCurrentUserLU(int userId) {
        return userId == getCurrentOrTargetUserIdLU();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] getUsers() {
        UserManagerService ums = this.mInjector.getUserManager();
        return ums != null ? ums.getUserIds() : new int[]{0};
    }

    private UserInfo getUserInfo(int userId) {
        return this.mInjector.getUserManager().getUserInfo(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] getUserIds() {
        return this.mInjector.getUserManager().getUserIds();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] expandUserId(int userId) {
        return userId != -1 ? new int[]{userId} : getUsers();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean exists(int userId) {
        return this.mInjector.getUserManager().exists(userId);
    }

    private void checkCallingPermission(String permission, String methodName) {
        if (this.mInjector.checkCallingPermission(permission) != 0) {
            String msg = "Permission denial: " + methodName + "() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + permission;
            Slogf.w(TAG, msg);
            throw new SecurityException(msg);
        }
    }

    private void enforceShellRestriction(String restriction, int userId) {
        if (Binder.getCallingUid() == 2000) {
            if (userId < 0 || hasUserRestriction(restriction, userId)) {
                throw new SecurityException("Shell does not have permission to access user " + userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasUserRestriction(String restriction, int userId) {
        return this.mInjector.getUserManager().hasUserRestriction(restriction, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSameProfileGroup(int callingUserId, int targetUserId) {
        boolean z = true;
        if (callingUserId == targetUserId) {
            return true;
        }
        synchronized (this.mLock) {
            int callingProfile = this.mUserProfileGroupIds.get(callingUserId, -10000);
            int targetProfile = this.mUserProfileGroupIds.get(targetUserId, -10000);
            if (callingProfile == -10000 || callingProfile != targetProfile) {
                z = false;
            }
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUserOrItsParentRunning(int userId) {
        synchronized (this.mLock) {
            if (isUserRunning(userId, 0)) {
                return true;
            }
            int parentUserId = this.mUserProfileGroupIds.get(userId, -10000);
            if (parentUserId == -10000) {
                return false;
            }
            return isUserRunning(parentUserId, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCurrentProfile(int userId) {
        boolean contains;
        synchronized (this.mLock) {
            contains = ArrayUtils.contains(this.mCurrentProfileIds, userId);
        }
        return contains;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int[] getCurrentProfileIds() {
        int[] iArr;
        synchronized (this.mLock) {
            iArr = this.mCurrentProfileIds;
        }
        return iArr;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            int size = this.mUserProfileGroupIds.size();
            for (int i = size - 1; i >= 0; i--) {
                if (this.mUserProfileGroupIds.keyAt(i) == userId || this.mUserProfileGroupIds.valueAt(i) == userId) {
                    this.mUserProfileGroupIds.removeAt(i);
                }
            }
            this.mCurrentProfileIds = ArrayUtils.removeInt(this.mCurrentProfileIds, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean shouldConfirmCredentials(int userId) {
        if (getStartedUserState(userId) != null && this.mInjector.getUserManager().isCredentialSharableWithParent(userId)) {
            if (this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId)) {
                KeyguardManager km = this.mInjector.getKeyguardManager();
                return km.isDeviceLocked(userId) && km.isDeviceSecure(userId);
            }
            return isUserRunning(userId, 2);
        }
        return false;
    }

    boolean isLockScreenDisabled(int userId) {
        return this.mLockPatternUtils.isLockScreenDisabled(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSwitchingFromSystemUserMessage(String switchingFromSystemUserMessage) {
        synchronized (this.mLock) {
            this.mSwitchingFromSystemUserMessage = switchingFromSystemUserMessage;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSwitchingToSystemUserMessage(String switchingToSystemUserMessage) {
        synchronized (this.mLock) {
            this.mSwitchingToSystemUserMessage = switchingToSystemUserMessage;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getSwitchingFromSystemUserMessage() {
        checkHasManageUsersPermission("getSwitchingFromSystemUserMessage()");
        return getSwitchingFromSystemUserMessageUnchecked();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getSwitchingToSystemUserMessage() {
        checkHasManageUsersPermission("getSwitchingToSystemUserMessage()");
        return getSwitchingToSystemUserMessageUnchecked();
    }

    private String getSwitchingFromSystemUserMessageUnchecked() {
        String str;
        synchronized (this.mLock) {
            str = this.mSwitchingFromSystemUserMessage;
        }
        return str;
    }

    private String getSwitchingToSystemUserMessageUnchecked() {
        String str;
        synchronized (this.mLock) {
            str = this.mSwitchingToSystemUserMessage;
        }
        return str;
    }

    private void checkHasManageUsersPermission(String operation) {
        if (this.mInjector.checkCallingPermission("android.permission.MANAGE_USERS") == -1) {
            throw new SecurityException("You need MANAGE_USERS permission to call " + operation);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        synchronized (this.mLock) {
            long token = proto.start(fieldId);
            for (int i = 0; i < this.mStartedUsers.size(); i++) {
                UserState uss = this.mStartedUsers.valueAt(i);
                long uToken = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
                proto.write(CompanionMessage.MESSAGE_ID, uss.mHandle.getIdentifier());
                uss.dumpDebug(proto, 1146756268034L);
                proto.end(uToken);
            }
            int i2 = 0;
            while (true) {
                int[] iArr = this.mStartedUserArray;
                if (i2 >= iArr.length) {
                    break;
                }
                proto.write(2220498092034L, iArr[i2]);
                i2++;
            }
            for (int i3 = 0; i3 < this.mUserLru.size(); i3++) {
                proto.write(2220498092035L, this.mUserLru.get(i3).intValue());
            }
            if (this.mUserProfileGroupIds.size() > 0) {
                for (int i4 = 0; i4 < this.mUserProfileGroupIds.size(); i4++) {
                    long uToken2 = proto.start(2246267895812L);
                    proto.write(CompanionMessage.MESSAGE_ID, this.mUserProfileGroupIds.keyAt(i4));
                    proto.write(1120986464258L, this.mUserProfileGroupIds.valueAt(i4));
                    proto.end(uToken2);
                }
            }
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("  mStartedUsers:");
            for (int i = 0; i < this.mStartedUsers.size(); i++) {
                UserState uss = this.mStartedUsers.valueAt(i);
                pw.print("    User #");
                pw.print(uss.mHandle.getIdentifier());
                pw.print(": ");
                uss.dump("", pw);
            }
            pw.print("  mStartedUserArray: [");
            for (int i2 = 0; i2 < this.mStartedUserArray.length; i2++) {
                if (i2 > 0) {
                    pw.print(", ");
                }
                pw.print(this.mStartedUserArray[i2]);
            }
            pw.println("]");
            pw.print("  mUserLru: [");
            for (int i3 = 0; i3 < this.mUserLru.size(); i3++) {
                if (i3 > 0) {
                    pw.print(", ");
                }
                pw.print(this.mUserLru.get(i3));
            }
            pw.println("]");
            if (this.mUserProfileGroupIds.size() > 0) {
                pw.println("  mUserProfileGroupIds:");
                for (int i4 = 0; i4 < this.mUserProfileGroupIds.size(); i4++) {
                    pw.print("    User #");
                    pw.print(this.mUserProfileGroupIds.keyAt(i4));
                    pw.print(" -> profile #");
                    pw.println(this.mUserProfileGroupIds.valueAt(i4));
                }
            }
            pw.println("  mCurrentUserId:" + this.mCurrentUserId);
            pw.println("  mTargetUserId:" + this.mTargetUserId);
            pw.println("  mLastActiveUsers:" + this.mLastActiveUsers);
            pw.println("  mDelayUserDataLocking:" + this.mDelayUserDataLocking);
            pw.println("  shouldStopUserOnSwitch():" + shouldStopUserOnSwitch());
            pw.println("  mStopUserOnSwitch:" + this.mStopUserOnSwitch);
            pw.println("  mMaxRunningUsers:" + this.mMaxRunningUsers);
            pw.println("  mUserSwitchUiEnabled:" + this.mUserSwitchUiEnabled);
            pw.println("  mInitialized:" + this.mInitialized);
            if (this.mSwitchingFromSystemUserMessage != null) {
                pw.println("  mSwitchingFromSystemUserMessage: " + this.mSwitchingFromSystemUserMessage);
            }
            if (this.mSwitchingToSystemUserMessage != null) {
                pw.println("  mSwitchingToSystemUserMessage: " + this.mSwitchingToSystemUserMessage);
            }
            pw.println("  mLastUserUnlockingUptime:" + this.mLastUserUnlockingUptime);
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 10:
                dispatchUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                return false;
            case 20:
                continueUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                return false;
            case 30:
                timeoutUserSwitch((UserState) msg.obj, msg.arg1, msg.arg2);
                return false;
            case 40:
                startProfiles();
                return false;
            case 50:
                this.mInjector.batteryStatsServiceNoteEvent(32775, Integer.toString(msg.arg1), msg.arg1);
                logUserJourneyInfo(null, getUserInfo(msg.arg1), 3);
                logUserLifecycleEvent(msg.arg1, 2, 1);
                this.mInjector.getSystemServiceManager().onUserStarting(TimingsTraceAndSlog.newAsyncLog(), msg.arg1);
                scheduleOnUserCompletedEvent(msg.arg1, 1, 5000);
                logUserLifecycleEvent(msg.arg1, 2, 2);
                clearSessionId(msg.arg1, 3);
                return false;
            case 60:
                this.mInjector.batteryStatsServiceNoteEvent(16392, Integer.toString(msg.arg2), msg.arg2);
                this.mInjector.batteryStatsServiceNoteEvent(32776, Integer.toString(msg.arg1), msg.arg1);
                this.mInjector.getSystemServiceManager().onUserSwitching(msg.arg2, msg.arg1);
                scheduleOnUserCompletedEvent(msg.arg1, 4, 5000);
                return false;
            case 70:
                dispatchForegroundProfileChanged(msg.arg1);
                return false;
            case 80:
                dispatchUserSwitchComplete(msg.arg1);
                logUserLifecycleEvent(msg.arg1, 1, 2);
                return false;
            case 90:
                timeoutUserSwitchCallbacks(msg.arg1, msg.arg2);
                return false;
            case 100:
                final int userId = msg.arg1;
                this.mInjector.getSystemServiceManager().onUserUnlocking(userId);
                FgThread.getHandler().post(new Runnable() { // from class: com.android.server.am.UserController$$ExternalSyntheticLambda11
                    @Override // java.lang.Runnable
                    public final void run() {
                        UserController.this.m1521lambda$handleMessage$14$comandroidserveramUserController(userId);
                    }
                });
                logUserLifecycleEvent(msg.arg1, 5, 2);
                logUserLifecycleEvent(msg.arg1, 6, 1);
                TimingsTraceAndSlog t = new TimingsTraceAndSlog();
                t.traceBegin("finishUserUnlocked-" + userId);
                finishUserUnlocked((UserState) msg.obj);
                t.traceEnd();
                return false;
            case 105:
                this.mInjector.getSystemServiceManager().onUserUnlocked(msg.arg1);
                scheduleOnUserCompletedEvent(msg.arg1, 2, this.mCurrentUserId != msg.arg1 ? 1000 : 5000);
                logUserLifecycleEvent(msg.arg1, 6, 2);
                clearSessionId(msg.arg1);
                return false;
            case 110:
                dispatchLockedBootComplete(msg.arg1);
                return false;
            case 120:
                logUserJourneyInfo(getUserInfo(getCurrentUserId()), getUserInfo(msg.arg1), 2);
                logUserLifecycleEvent(msg.arg1, 1, 1);
                startUserInForeground(msg.arg1);
                return false;
            case 130:
                completeUserSwitch(msg.arg1);
                return false;
            case 140:
                reportOnUserCompletedEvent((Integer) msg.obj);
                return false;
            case 150:
                moveUserToForeground((UserState) msg.obj, msg.arg1, msg.arg2);
                return false;
            case 200:
                logAndClearSessionId(msg.arg1);
                return false;
            case 1000:
                Pair<UserInfo, UserInfo> fromToUserPair = (Pair) msg.obj;
                logUserJourneyInfo((UserInfo) fromToUserPair.first, (UserInfo) fromToUserPair.second, 1);
                logUserLifecycleEvent(((UserInfo) fromToUserPair.second).id, 1, 1);
                showUserSwitchDialog(fromToUserPair);
                return false;
            default:
                return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleMessage$14$com-android-server-am-UserController  reason: not valid java name */
    public /* synthetic */ void m1521lambda$handleMessage$14$comandroidserveramUserController(int userId) {
        this.mInjector.loadUserRecents(userId);
    }

    void scheduleOnUserCompletedEvent(int userId, int eventType, int delayMs) {
        if (eventType != 0) {
            synchronized (this.mCompletedEventTypes) {
                SparseIntArray sparseIntArray = this.mCompletedEventTypes;
                sparseIntArray.put(userId, sparseIntArray.get(userId, 0) | eventType);
            }
        }
        Object msgObj = Integer.valueOf(userId);
        this.mHandler.removeEqualMessages(140, msgObj);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(140, msgObj), delayMs);
    }

    void reportOnUserCompletedEvent(Integer userId) {
        int eventTypes;
        this.mHandler.removeEqualMessages(140, userId);
        synchronized (this.mCompletedEventTypes) {
            eventTypes = this.mCompletedEventTypes.get(userId.intValue(), 0);
            this.mCompletedEventTypes.delete(userId.intValue());
        }
        int eligibleEventTypes = 0;
        synchronized (this.mLock) {
            UserState uss = this.mStartedUsers.get(userId.intValue());
            if (uss != null && uss.state != 5) {
                eligibleEventTypes = 0 | 1;
            }
            if (uss != null && uss.state == 3) {
                eligibleEventTypes |= 2;
            }
            if (userId.intValue() == this.mCurrentUserId) {
                eligibleEventTypes |= 4;
            }
        }
        Slogf.i(TAG, "reportOnUserCompletedEvent(%d): stored=%s, eligible=%s", userId, Integer.toBinaryString(eventTypes), Integer.toBinaryString(eligibleEventTypes));
        this.mInjector.systemServiceManagerOnUserCompletedEvent(userId.intValue(), eventTypes & eligibleEventTypes);
    }

    private void logUserJourneyInfo(UserInfo origin, UserInfo target, int journey) {
        long newSessionId = ThreadLocalRandom.current().nextLong(1L, JobStatus.NO_LATEST_RUNTIME);
        synchronized (this.mUserIdToUserJourneyMap) {
            UserJourneySession userJourneySession = this.mUserIdToUserJourneyMap.get(target.id);
            if (userJourneySession != null) {
                if ((userJourneySession.mJourney != 1 && userJourneySession.mJourney != 2) || (journey != 3 && journey != 5)) {
                    FrameworkStatsLog.write((int) FrameworkStatsLog.USER_LIFECYCLE_EVENT_OCCURRED, userJourneySession.mSessionId, target.id, 0, 0);
                }
                if (ActivityManagerDebugConfig.DEBUG_MU) {
                    Slogf.d(TAG, journey + " not logged as it is expected to be part of " + userJourneySession.mJourney);
                }
                return;
            }
            if (ActivityManagerDebugConfig.DEBUG_MU) {
                Slogf.d(TAG, "Starting a new journey: " + journey + " with session id: " + newSessionId);
            }
            this.mUserIdToUserJourneyMap.put(target.id, new UserJourneySession(newSessionId, journey));
            this.mHandler.removeMessages(200);
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(200, target.id, 0), 90000L);
            FrameworkStatsLog.write(264, newSessionId, journey, origin != null ? origin.id : -1, target.id, UserManager.getUserTypeForStatsd(target.userType), target.flags);
        }
    }

    private void logUserLifecycleEvent(int userId, int event, int eventState) {
        synchronized (this.mUserIdToUserJourneyMap) {
            UserJourneySession userJourneySession = this.mUserIdToUserJourneyMap.get(userId);
            if (userJourneySession != null && userJourneySession.mSessionId != 0) {
                long sessionId = userJourneySession.mSessionId;
                FrameworkStatsLog.write((int) FrameworkStatsLog.USER_LIFECYCLE_EVENT_OCCURRED, sessionId, userId, event, eventState);
                return;
            }
            Slogf.w(TAG, "UserLifecycleEvent " + event + " received without an active userJourneySession.");
        }
    }

    private void clearSessionId(int userId, int journey) {
        synchronized (this.mUserIdToUserJourneyMap) {
            UserJourneySession userJourneySession = this.mUserIdToUserJourneyMap.get(userId);
            if (userJourneySession != null && userJourneySession.mJourney == journey) {
                clearSessionId(userId);
            }
        }
    }

    private void clearSessionId(int userId) {
        synchronized (this.mUserIdToUserJourneyMap) {
            this.mHandler.removeMessages(200);
            this.mUserIdToUserJourneyMap.delete(userId);
        }
    }

    private void logAndClearSessionId(int userId) {
        synchronized (this.mUserIdToUserJourneyMap) {
            UserJourneySession userJourneySession = this.mUserIdToUserJourneyMap.get(userId);
            if (userJourneySession != null) {
                FrameworkStatsLog.write((int) FrameworkStatsLog.USER_LIFECYCLE_EVENT_OCCURRED, userJourneySession.mSessionId, userId, 0, 0);
            }
            clearSessionId(userId);
        }
    }

    private BroadcastOptions getTemporaryAppAllowlistBroadcastOptions(int reasonCode) {
        long duration = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        ActivityManagerInternal amInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        if (amInternal != null) {
            duration = amInternal.getBootTimeTempAllowListDuration();
        }
        BroadcastOptions bOptions = BroadcastOptions.makeBasic();
        bOptions.setTemporaryAppAllowlist(duration, 0, reasonCode, "");
        return bOptions;
    }

    public long getLastUserUnlockingUptime() {
        return this.mLastUserUnlockingUptime;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class UserJourneySession {
        final int mJourney;
        final long mSessionId;

        UserJourneySession(long sessionId, int journey) {
            this.mJourney = journey;
            this.mSessionId = sessionId;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class UserProgressListener extends IProgressListener.Stub {
        private volatile long mUnlockStarted;

        private UserProgressListener() {
        }

        public void onStarted(int id, Bundle extras) throws RemoteException {
            Slogf.d(UserController.TAG, "Started unlocking user " + id);
            this.mUnlockStarted = SystemClock.uptimeMillis();
        }

        public void onProgress(int id, int progress, Bundle extras) throws RemoteException {
            Slogf.d(UserController.TAG, "Unlocking user " + id + " progress " + progress);
        }

        public void onFinished(int id, Bundle extras) throws RemoteException {
            long unlockTime = SystemClock.uptimeMillis() - this.mUnlockStarted;
            if (id == 0) {
                new TimingsTraceAndSlog().logDuration("SystemUserUnlock", unlockTime);
            } else {
                new TimingsTraceAndSlog().logDuration("User" + id + "Unlock", unlockTime);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Injector {
        private Handler mHandler;
        private final ActivityManagerService mService;
        private UserManagerService mUserManager;
        private UserManagerInternal mUserManagerInternal;

        Injector(ActivityManagerService service) {
            this.mService = service;
        }

        protected Handler getHandler(Handler.Callback callback) {
            Handler handler = new Handler(this.mService.mHandlerThread.getLooper(), callback);
            this.mHandler = handler;
            return handler;
        }

        protected Handler getUiHandler(Handler.Callback callback) {
            return new Handler(this.mService.mUiHandler.getLooper(), callback);
        }

        protected Context getContext() {
            return this.mService.mContext;
        }

        protected LockPatternUtils getLockPatternUtils() {
            return new LockPatternUtils(getContext());
        }

        protected int broadcastIntent(Intent intent, String resolvedType, IIntentReceiver resultTo, int resultCode, String resultData, Bundle resultExtras, String[] requiredPermissions, int appOp, Bundle bOptions, boolean ordered, boolean sticky, int callingPid, int callingUid, int realCallingUid, int realCallingPid, int userId) {
            int logUserId;
            int logUserId2 = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (logUserId2 != -10000) {
                logUserId = logUserId2;
            } else {
                logUserId = userId;
            }
            EventLog.writeEvent((int) EventLogTags.UC_SEND_USER_BROADCAST, Integer.valueOf(logUserId), intent.getAction());
            synchronized (this.mService) {
                try {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        int broadcastIntentLocked = this.mService.broadcastIntentLocked(null, null, null, intent, resolvedType, resultTo, resultCode, resultData, resultExtras, requiredPermissions, null, null, appOp, bOptions, ordered, sticky, callingPid, callingUid, realCallingUid, realCallingPid, userId);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return broadcastIntentLocked;
                    } catch (Throwable th) {
                        th = th;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        int checkCallingPermission(String permission) {
            return this.mService.checkCallingPermission(permission);
        }

        WindowManagerService getWindowManager() {
            return this.mService.mWindowManager;
        }

        void activityManagerOnUserStopped(int userId) {
            ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).onUserStopped(userId);
        }

        void systemServiceManagerOnUserStopped(int userId) {
            getSystemServiceManager().onUserStopped(userId);
        }

        void systemServiceManagerOnUserCompletedEvent(int userId, int eventTypes) {
            getSystemServiceManager().onUserCompletedEvent(userId, eventTypes);
        }

        protected UserManagerService getUserManager() {
            if (this.mUserManager == null) {
                IBinder b = ServiceManager.getService("user");
                this.mUserManager = IUserManager.Stub.asInterface(b);
            }
            return this.mUserManager;
        }

        UserManagerInternal getUserManagerInternal() {
            if (this.mUserManagerInternal == null) {
                this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            }
            return this.mUserManagerInternal;
        }

        KeyguardManager getKeyguardManager() {
            return (KeyguardManager) this.mService.mContext.getSystemService(KeyguardManager.class);
        }

        void batteryStatsServiceNoteEvent(int code, String name, int uid) {
            this.mService.mBatteryStatsService.noteEvent(code, name, uid);
        }

        boolean isRuntimeRestarted() {
            return getSystemServiceManager().isRuntimeRestarted();
        }

        SystemServiceManager getSystemServiceManager() {
            return this.mService.mSystemServiceManager;
        }

        boolean isFirstBootOrUpgrade() {
            IPackageManager pm = AppGlobals.getPackageManager();
            try {
                if (!pm.isFirstBoot()) {
                    if (!pm.isDeviceUpgrading()) {
                        return false;
                    }
                }
                return true;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        void sendPreBootBroadcast(int userId, boolean quiet, final Runnable onFinish) {
            EventLog.writeEvent((int) EventLogTags.UC_SEND_USER_BROADCAST, Integer.valueOf(userId), "android.intent.action.PRE_BOOT_COMPLETED");
            new PreBootBroadcaster(this.mService, userId, null, quiet) { // from class: com.android.server.am.UserController.Injector.1
                @Override // com.android.server.am.PreBootBroadcaster
                public void onFinished() {
                    onFinish.run();
                }
            }.sendNext();
        }

        void activityManagerForceStopPackage(int userId, String reason) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.forceStopPackageLocked(null, -1, false, false, true, false, false, userId, reason);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        int checkComponentPermission(String permission, int pid, int uid, int owningUid, boolean exported) {
            return ActivityManagerService.checkComponentPermission(permission, pid, uid, owningUid, exported);
        }

        boolean checkPermissionForPreflight(String permission, int pid, int uid, String pkg) {
            return PermissionChecker.checkPermissionForPreflight(getContext(), permission, pid, uid, pkg) == 0;
        }

        protected void startHomeActivity(int userId, String reason) {
            this.mService.mAtmInternal.startHomeActivity(userId, reason);
        }

        void startUserWidgets(final int userId) {
            final AppWidgetManagerInternal awm = (AppWidgetManagerInternal) LocalServices.getService(AppWidgetManagerInternal.class);
            if (awm != null) {
                FgThread.getHandler().post(new Runnable() { // from class: com.android.server.am.UserController$Injector$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        awm.unlockUser(userId);
                    }
                });
            }
        }

        void updateUserConfiguration() {
            this.mService.mAtmInternal.updateUserConfiguration();
        }

        void clearBroadcastQueueForUser(int userId) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.clearBroadcastQueueForUserLocked(userId);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        void loadUserRecents(int userId) {
            this.mService.mAtmInternal.loadRecentTasksForUser(userId);
        }

        void startPersistentApps(int matchFlags) {
            this.mService.startPersistentApps(matchFlags);
        }

        void installEncryptionUnawareProviders(int userId) {
            this.mService.mCpHelper.installEncryptionUnawareProviders(userId);
        }

        void showUserSwitchingDialog(UserInfo fromUser, UserInfo toUser, String switchingFromSystemUserMessage, String switchingToSystemUserMessage) {
            Dialog userSwitchingDialog;
            if (this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                Slogf.w(UserController.TAG, "Showing user switch dialog on UserController, it could cause a race condition if it's shown by CarSystemUI as well");
            }
            ITranUserSwitchDialogLice Instance = ITranUserSwitchDialogLice.Instance();
            ActivityManagerService activityManagerService = this.mService;
            Dialog tranDialog = Instance.instanceUserSwitchDialog(activityManagerService, activityManagerService.mContext, fromUser, toUser, true, switchingFromSystemUserMessage, switchingToSystemUserMessage);
            if (tranDialog != null) {
                userSwitchingDialog = tranDialog;
            } else {
                ActivityManagerService activityManagerService2 = this.mService;
                userSwitchingDialog = new UserSwitchingDialog(activityManagerService2, activityManagerService2.mContext, fromUser, toUser, true, switchingFromSystemUserMessage, switchingToSystemUserMessage);
            }
            Dialog d = userSwitchingDialog;
            d.show();
        }

        void reportGlobalUsageEvent(int event) {
            this.mService.reportGlobalUsageEvent(event);
        }

        void reportCurWakefulnessUsageEvent() {
            this.mService.reportCurWakefulnessUsageEvent();
        }

        void taskSupervisorRemoveUser(int userId) {
            this.mService.mAtmInternal.removeUser(userId);
        }

        protected boolean taskSupervisorSwitchUser(int userId, UserState uss) {
            return this.mService.mAtmInternal.switchUser(userId, uss);
        }

        protected void taskSupervisorResumeFocusedStackTopActivity() {
            this.mService.mAtmInternal.resumeTopActivities(false);
        }

        protected void clearAllLockedTasks(String reason) {
            this.mService.mAtmInternal.clearLockedTasks(reason);
        }

        boolean isCallerRecents(int callingUid) {
            return this.mService.mAtmInternal.isCallerRecents(callingUid);
        }

        protected IStorageManager getStorageManager() {
            return IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
        }

        protected void dismissKeyguard(final Runnable runnable, String reason) {
            getWindowManager().dismissKeyguard(new IKeyguardDismissCallback.Stub() { // from class: com.android.server.am.UserController.Injector.2
                public void onDismissError() throws RemoteException {
                    Injector.this.mHandler.post(runnable);
                }

                public void onDismissSucceeded() throws RemoteException {
                    Injector.this.mHandler.post(runnable);
                }

                public void onDismissCancelled() throws RemoteException {
                    Injector.this.mHandler.post(runnable);
                }
            }, reason);
        }
    }
}
