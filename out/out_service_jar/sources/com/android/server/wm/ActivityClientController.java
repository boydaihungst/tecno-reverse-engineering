package com.android.server.wm;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.IActivityClientController;
import android.app.ICompatCameraControlCallback;
import android.app.IRequestFinishCallback;
import android.app.PictureInPictureParams;
import android.app.PictureInPictureUiState;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.EnterPipRequestedItem;
import android.app.servertransaction.PipStateTransactionItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.service.voice.VoiceInteractionManagerInternal;
import android.util.Slog;
import android.view.RemoteAnimationDefinition;
import android.window.SizeConfigurationBuckets;
import android.window.TransitionInfo;
import com.android.internal.app.AssistUtils;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.internal.protolog.ProtoLogImpl;
import com.android.server.LocalServices;
import com.android.server.Watchdog;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.uri.NeededUriGrants;
import com.android.server.vr.VrManagerInternal;
import com.android.server.wm.ActivityRecord;
import com.transsion.hubcore.server.wm.ITranActivityClientController;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ActivityClientController extends IActivityClientController.Stub {
    private static final String TAG = "ActivityTaskManager";
    private AssistUtils mAssistUtils;
    private final Context mContext;
    private final WindowManagerGlobalLock mGlobalLock;
    private final ActivityTaskManagerService mService;
    private final ActivityTaskSupervisor mTaskSupervisor;
    private static final boolean mIsLucidDisabled = SystemProperties.get("persist.product.lucid.disabled").equals("1");
    private static final boolean TRAN_LUCID_OPTIMIZATION_SUPPORT = "1".equals(SystemProperties.get("persist.transsion.lucid.optimization", "0"));
    private boolean mIsInFreeformProcess = false;
    private Handler mH = new Handler();

    static native void setLucidFgApp(String str);

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityClientController(ActivityTaskManagerService service) {
        this.mService = service;
        this.mGlobalLock = service.mGlobalLock;
        this.mTaskSupervisor = service.mTaskSupervisor;
        this.mContext = service.mContext;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemReady() {
        this.mAssistUtils = new AssistUtils(this.mContext);
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            throw ActivityTaskManagerService.logAndRethrowRuntimeExceptionOnTransact("ActivityClientController", e);
        }
    }

    public void activityIdle(IBinder token, Configuration config, boolean stopProfiling) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                Trace.traceBegin(32L, "activityIdle");
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r != null) {
                    this.mTaskSupervisor.activityIdleInternal(r, false, false, config);
                    if (stopProfiling && r.hasProcess()) {
                        r.app.clearProfilerIfNeeded();
                    }
                    ITranActivityClientController.Instance().hookActivityIdle(r.app != null ? r.app.getProcessWrapper() : null, r.info, r.intent);
                    this.mService.mAmsExt.onEndOfActivityIdle(this.mContext, ActivityRecord.forTokenLocked(token));
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        } finally {
            Trace.traceEnd(32L);
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void activityResumed(IBinder token, boolean handleSplashScreenExit) {
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord.activityResumedLocked(token, handleSplashScreenExit);
                if (!mIsLucidDisabled) {
                    final ActivityRecord lucid_r = ActivityRecord.forTokenLocked(token);
                    this.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityClientController.1
                        @Override // java.lang.Runnable
                        public void run() {
                            ActivityRecord activityRecord = lucid_r;
                            if (activityRecord != null && activityRecord.packageName != null) {
                                if (ActivityClientController.TRAN_LUCID_OPTIMIZATION_SUPPORT && ActivityClientController.this.isLucidIgnored(lucid_r.info)) {
                                    Slog.e(ActivityClientController.TAG, "[LUCID]  ignore lucid_r.packageNamge: " + lucid_r.packageName);
                                } else if (ActivityRecord.State.RESUMED == lucid_r.getState()) {
                                    ActivityClientController.setLucidFgApp(lucid_r.packageName);
                                } else {
                                    Slog.e(ActivityClientController.TAG, "[LUCID] lucid_r.packageNamge: " + lucid_r.packageName + " lucid_r.state:" + lucid_r.getState() + " ignored!");
                                }
                            } else if (lucid_r == null) {
                                Slog.e(ActivityClientController.TAG, "[LUCID] lucid_r in activityResumed() is null");
                            } else {
                                Slog.e(ActivityClientController.TAG, "[LUCID] lucid_r.packageNamge in activityResumed() is null");
                            }
                        }
                    });
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
    }

    public void activityTopResumedStateLost() {
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                this.mTaskSupervisor.handleTopResumedStateReleased(false);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
    }

    public void activityPaused(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Trace.traceBegin(32L, "activityPaused");
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r != null) {
                    r.activityPaused(false);
                    this.mService.mAmsExt.onActivityStateChanged(r, false);
                }
                Trace.traceEnd(32L);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
    }

    public void activityStopped(IBinder token, Bundle icicle, PersistableBundle persistentState, CharSequence description) {
        ActivityRecord r;
        if (ActivityTaskManagerDebugConfig.DEBUG_ALL) {
            Slog.v(TAG, "Activity stopped: token=" + token);
        }
        if (icicle != null && icicle.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Bundle");
        }
        long origId = Binder.clearCallingIdentity();
        String restartingName = null;
        int restartingUid = 0;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Trace.traceBegin(32L, "activityStopped");
                r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    if (r.attachedToProcess() && r.isState(ActivityRecord.State.RESTARTING_PROCESS)) {
                        restartingName = r.app.mName;
                        restartingUid = r.app.mUid;
                    }
                    r.activityStopped(icicle, persistentState, description);
                }
                Trace.traceEnd(32L);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (restartingName != null) {
            this.mTaskSupervisor.removeRestartTimeouts(r);
            this.mService.mAmInternal.killProcess(restartingName, restartingUid, "restartActivityProcess");
        }
        this.mService.mAmInternal.trimApplications();
        Binder.restoreCallingIdentity(origId);
    }

    public void activityDestroyed(IBinder token) {
        if (ActivityTaskManagerDebugConfig.DEBUG_SWITCH) {
            Slog.v(ActivityTaskManagerService.TAG_SWITCH, "ACTIVITY DESTROYED: " + token);
        }
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Trace.traceBegin(32L, "activityDestroyed");
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r != null) {
                    r.destroyed("activityDestroyed");
                }
                Trace.traceEnd(32L);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void activityLocalRelaunch(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r != null) {
                    r.startRelaunching();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
    }

    public void activityRelaunched(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r != null) {
                    r.finishRelaunching();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
    }

    public void reportSizeConfigurations(IBinder token, SizeConfigurationBuckets sizeConfigurations) {
        if (ProtoLogCache.WM_DEBUG_CONFIGURATION_enabled) {
            String protoLogParam0 = String.valueOf(token);
            String protoLogParam1 = String.valueOf(sizeConfigurations);
            ProtoLogImpl.v(ProtoLogGroup.WM_DEBUG_CONFIGURATION, 1305412562, 0, (String) null, new Object[]{protoLogParam0, protoLogParam1});
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.setSizeConfigurations(sizeConfigurations);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean moveActivityTaskToBack(IBinder token, boolean nonRoot) {
        ActivityTaskManagerService.enforceNotIsolatedCaller("moveActivityTaskToBack");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                int taskId = ActivityRecord.getTaskForActivityLocked(token, !nonRoot);
                Task task = this.mService.mRootWindowContainer.anyTaskForId(taskId);
                if (task == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                boolean moveTaskToBack = ActivityRecord.getRootTask(token).moveTaskToBack(task);
                WindowManagerService.resetPriorityAfterLockedSection();
                return moveTaskToBack;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean shouldUpRecreateTask(IBinder token, String destAffinity) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord srec = ActivityRecord.forTokenLocked(token);
                if (srec != null) {
                    boolean shouldUpRecreateTaskLocked = srec.getRootTask().shouldUpRecreateTaskLocked(srec, destAffinity);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return shouldUpRecreateTaskLocked;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return false;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[]}, finally: {[INVOKE] complete} */
    public boolean navigateUpTo(IBinder token, Intent destIntent, int resultCode, Intent resultData) {
        boolean navigateUpTo;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r == null) {
                    return false;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                NeededUriGrants destGrants = this.mService.collectGrants(destIntent, r);
                NeededUriGrants resultGrants = this.mService.collectGrants(resultData, r.resultTo);
                synchronized (this.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        navigateUpTo = r.getRootTask().navigateUpTo(r, destIntent, destGrants, resultCode, resultData, resultGrants);
                    } finally {
                        WindowManagerService.resetPriorityAfterLockedSection();
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return navigateUpTo;
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
    }

    public boolean releaseActivityInstance(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null && r.isDestroyable()) {
                    r.destroyImmediately("app-req");
                    boolean isState = r.isState(ActivityRecord.State.DESTROYING, ActivityRecord.State.DESTROYED);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return isState;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean finishActivity(IBinder token, int resultCode, Intent resultData, int finishTask) {
        long j;
        boolean embededFinishWithRootActivity;
        boolean res;
        if (resultData != null && resultData.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                NeededUriGrants resultGrants = this.mService.collectGrants(resultData, r.resultTo);
                synchronized (this.mGlobalLock) {
                    try {
                        try {
                            WindowManagerService.boostPriorityForLockedSection();
                            if (!r.isInHistory()) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return true;
                            }
                            Task tr = r.getTask();
                            ActivityRecord rootR = tr.getRootActivity();
                            if (rootR == null) {
                                Slog.w(TAG, "Finishing task with all activities already finished");
                            }
                            if (this.mService.getLockTaskController().activityBlockedFromFinish(r)) {
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return false;
                            }
                            if (this.mService.mController != null) {
                                ActivityRecord next = r.getRootTask().topRunningActivity(token, -1);
                                if (next != null) {
                                    boolean resumeOK = true;
                                    try {
                                        resumeOK = this.mService.mController.activityResuming(next.packageName);
                                    } catch (RemoteException e) {
                                        this.mService.mController = null;
                                        Watchdog.getInstance().setActivityController(null);
                                    }
                                    if (!resumeOK) {
                                        Slog.i(TAG, "Not finishing activity because controller resumed");
                                        WindowManagerService.resetPriorityAfterLockedSection();
                                        return false;
                                    }
                                }
                            }
                            if (r.app != null) {
                                r.app.setLastActivityFinishTimeIfNeeded(SystemClock.uptimeMillis());
                            }
                            long origId = Binder.clearCallingIdentity();
                            Trace.traceBegin(32L, "finishActivity");
                            boolean finishWithRootActivity = finishTask == 1;
                            try {
                                embededFinishWithRootActivity = finishingEmbeddingTask(r, rootR);
                                if (embededFinishWithRootActivity && r == rootR) {
                                    try {
                                        Slog.d(TAG, "finishActivity() r=" + r + ", embededFinishWithRootActivity =" + embededFinishWithRootActivity);
                                    } catch (Throwable th) {
                                        th = th;
                                        j = 32;
                                        Trace.traceEnd(j);
                                        Binder.restoreCallingIdentity(origId);
                                        throw th;
                                    }
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                j = 32;
                            }
                            try {
                                if (finishTask == 2) {
                                    j = 32;
                                } else if ((!finishWithRootActivity || r != rootR) && !embededFinishWithRootActivity) {
                                    j = 32;
                                    r.finishIfPossible(resultCode, resultData, resultGrants, "app-request", true);
                                    res = r.finishing;
                                    if (!res) {
                                        Slog.i(TAG, "Failed to finish by app-request");
                                    }
                                    Trace.traceEnd(j);
                                    Binder.restoreCallingIdentity(origId);
                                    WindowManagerService.resetPriorityAfterLockedSection();
                                    return res;
                                } else {
                                    j = 32;
                                }
                                this.mTaskSupervisor.removeTask(tr, false, finishWithRootActivity, "finish-activity");
                                r.mRelaunchReason = 0;
                                res = true;
                                Trace.traceEnd(j);
                                Binder.restoreCallingIdentity(origId);
                                WindowManagerService.resetPriorityAfterLockedSection();
                                return res;
                            } catch (Throwable th3) {
                                th = th3;
                                Trace.traceEnd(j);
                                Binder.restoreCallingIdentity(origId);
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            WindowManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            } catch (Throwable th6) {
                th = th6;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th7) {
                        th = th7;
                    }
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [568=4] */
    public boolean finishActivityAffinity(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                final ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (this.mService.getLockTaskController().activityBlockedFromFinish(r)) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else {
                    r.getTask().forAllActivities(new Predicate() { // from class: com.android.server.wm.ActivityClientController$$ExternalSyntheticLambda1
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            boolean finishIfSameAffinity;
                            finishIfSameAffinity = ActivityRecord.this.finishIfSameAffinity((ActivityRecord) obj);
                            return finishIfSameAffinity;
                        }
                    }, r, true, true);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return true;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void finishSubActivity(IBinder token, final String resultWho, final int requestCode) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                final ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.getRootTask().forAllActivities(new Consumer() { // from class: com.android.server.wm.ActivityClientController$$ExternalSyntheticLambda3
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            ((ActivityRecord) obj).finishIfSubActivity(ActivityRecord.this, resultWho, requestCode);
                        }
                    }, true);
                    this.mService.updateOomAdj();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean isTopOfTask(IBinder token) {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                z = r != null && r.getTask().getTopNonFinishingActivity() == r;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public boolean willActivityBeVisible(IBinder token) {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task rootTask = ActivityRecord.getRootTask(token);
                z = rootTask != null && rootTask.willActivityBeVisible(token);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public int getDisplayId(IBinder activityToken) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task rootTask = ActivityRecord.getRootTask(activityToken);
                if (rootTask == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return 0;
                }
                int displayId = rootTask.getDisplayId();
                int i = displayId != -1 ? displayId : 0;
                WindowManagerService.resetPriorityAfterLockedSection();
                return i;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int getTaskForActivity(IBinder token, boolean onlyRoot) {
        int taskForActivityLocked;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                taskForActivityLocked = ActivityRecord.getTaskForActivityLocked(token, onlyRoot);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return taskForActivityLocked;
    }

    public int getTaskSize(IBinder token) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r != null && r.getTask() != null) {
                    int childCount = r.getTask().getChildCount();
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return childCount;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return -1;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [659=4] */
    public IBinder getActivityTokenBelow(IBinder activityToken) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord ar = ActivityRecord.isInAnyTask(activityToken);
                if (ar == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                ActivityRecord below = ar.getTask().getActivity(new Predicate() { // from class: com.android.server.wm.ActivityClientController$$ExternalSyntheticLambda2
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return ActivityClientController.lambda$getActivityTokenBelow$2((ActivityRecord) obj);
                    }
                }, ar, false, true);
                if (below == null || below.getUid() != ar.getUid()) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return null;
                }
                IBinder iBinder = below.token;
                WindowManagerService.resetPriorityAfterLockedSection();
                return iBinder;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getActivityTokenBelow$2(ActivityRecord r) {
        return !r.finishing;
    }

    public ComponentName getCallingActivity(IBinder token) {
        ComponentName component;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = getCallingRecord(token);
                component = r != null ? r.intent.getComponent() : null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return component;
    }

    public String getCallingPackage(IBinder token) {
        String str;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = getCallingRecord(token);
                str = r != null ? r.info.packageName : null;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return str;
    }

    private static ActivityRecord getCallingRecord(IBinder token) {
        ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
        if (r != null) {
            return r.resultTo;
        }
        return null;
    }

    public int getLaunchedFromUid(IBinder token) {
        int i;
        if (canGetLaunchedFrom()) {
            synchronized (this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.forTokenLocked(token);
                    i = r != null ? r.launchedFromUid : -1;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return i;
        }
        return -1;
    }

    public String getLaunchedFromPackage(IBinder token) {
        String str;
        if (canGetLaunchedFrom()) {
            synchronized (this.mGlobalLock) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.forTokenLocked(token);
                    str = r != null ? r.launchedFromPackage : null;
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return str;
        }
        return null;
    }

    private boolean canGetLaunchedFrom() {
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) == 1000) {
            return true;
        }
        PackageManagerInternal pm = this.mService.mWindowManager.mPmInternal;
        AndroidPackage callingPkg = pm.getPackage(uid);
        if (callingPkg == null) {
            return false;
        }
        if (callingPkg.isSignedWithPlatformKey()) {
            return true;
        }
        String[] installerNames = pm.getKnownPackageNames(2, UserHandle.getUserId(uid));
        return installerNames.length > 0 && callingPkg.getPackageName().equals(installerNames[0]);
    }

    public void setRequestedOrientation(IBinder token, int requestedOrientation) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.setRequestedOrientation(requestedOrientation);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int getRequestedOrientation(IBinder token) {
        int requestedOrientation;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                requestedOrientation = r != null ? r.getRequestedOrientation() : -1;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return requestedOrientation;
    }

    public boolean convertFromTranslucent(IBinder token) {
        boolean z;
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                z = true;
                if (r == null || !r.setOccludesParent(true)) {
                    z = false;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return z;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean convertToTranslucent(IBinder token, Bundle options) {
        SafeActivityOptions safeOptions = SafeActivityOptions.fromBundle(options);
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    ActivityRecord under = r.getTask().getActivityBelow(r);
                    if (under != null) {
                        under.returningOptions = safeOptions != null ? safeOptions.getOptions(r) : null;
                    }
                    boolean occludesParent = r.setOccludesParent(false);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return occludesParent;
                }
                WindowManagerService.resetPriorityAfterLockedSection();
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public boolean isImmersive(IBinder token) {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r == null) {
                    throw new IllegalArgumentException();
                }
                z = r.immersive;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public void setImmersive(IBinder token, boolean immersive) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r == null) {
                    throw new IllegalArgumentException();
                }
                r.immersive = immersive;
                if (r.isFocusedActivityOnDisplay()) {
                    if (ProtoLogCache.WM_DEBUG_IMMERSIVE_enabled) {
                        String protoLogParam0 = String.valueOf(r);
                        ProtoLogImpl.d(ProtoLogGroup.WM_DEBUG_IMMERSIVE, -655104359, 0, (String) null, new Object[]{protoLogParam0});
                    }
                    this.mService.applyUpdateLockStateLocked(r);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public boolean enterPictureInPictureMode(IBinder token, PictureInPictureParams params) {
        boolean enterPictureInPictureMode;
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ensureValidPictureInPictureActivityParams("enterPictureInPictureMode", token, params);
                enterPictureInPictureMode = this.mService.enterPictureInPictureMode(r, params);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return enterPictureInPictureMode;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void setPictureInPictureParams(IBinder token, PictureInPictureParams params) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ensureValidPictureInPictureActivityParams("setPictureInPictureParams", token, params);
                r.setPictureInPictureParams(params);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void setShouldDockBigOverlays(IBinder token, boolean shouldDockBigOverlays) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                r.setShouldDockBigOverlays(shouldDockBigOverlays);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void splashScreenAttached(IBinder token) {
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord.splashScreenAttachedLocked(token);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        Binder.restoreCallingIdentity(origId);
    }

    public void requestCompatCameraControl(IBinder token, boolean showControl, boolean transformationApplied, ICompatCameraControlCallback callback) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.updateCameraCompatState(showControl, transformationApplied, callback);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private ActivityRecord ensureValidPictureInPictureActivityParams(String caller, IBinder token, PictureInPictureParams params) {
        if (!this.mService.mSupportsPictureInPicture) {
            throw new IllegalStateException(caller + ": Device doesn't support picture-in-picture mode.");
        }
        ActivityRecord r = ActivityRecord.forTokenLocked(token);
        if (r == null) {
            throw new IllegalStateException(caller + ": Can't find activity for token=" + token);
        }
        if (!r.supportsPictureInPicture()) {
            throw new IllegalStateException(caller + ": Current activity does not support picture-in-picture.");
        }
        float minAspectRatio = this.mContext.getResources().getFloat(17105095);
        float maxAspectRatio = this.mContext.getResources().getFloat(17105094);
        if (params.hasSetAspectRatio() && !this.mService.mWindowManager.isValidPictureInPictureAspectRatio(r.mDisplayContent, params.getAspectRatioFloat())) {
            throw new IllegalArgumentException(String.format(caller + ": Aspect ratio is too extreme (must be between %f and %f).", Float.valueOf(minAspectRatio), Float.valueOf(maxAspectRatio)));
        }
        if (this.mService.mSupportsExpandedPictureInPicture && params.hasSetExpandedAspectRatio() && !this.mService.mWindowManager.isValidExpandedPictureInPictureAspectRatio(r.mDisplayContent, params.getExpandedAspectRatioFloat())) {
            throw new IllegalArgumentException(String.format(caller + ": Expanded aspect ratio is not extreme enough (must not be between %f and %f).", Float.valueOf(minAspectRatio), Float.valueOf(maxAspectRatio)));
        }
        params.truncateActions(ActivityTaskManager.getMaxNumPictureInPictureActions(this.mContext));
        return r;
    }

    void requestPictureInPictureMode(ActivityRecord r) {
        if (r.inPinnedWindowingMode()) {
            throw new IllegalStateException("Activity is already in PIP mode");
        }
        boolean canEnterPictureInPicture = r.checkEnterPictureInPictureState("requestPictureInPictureMode", false);
        if (!canEnterPictureInPicture) {
            throw new IllegalStateException("Requested PIP on an activity that doesn't support it");
        }
        if (r.pictureInPictureArgs.isAutoEnterEnabled()) {
            this.mService.enterPictureInPictureMode(r, r.pictureInPictureArgs);
            return;
        }
        try {
            ClientTransaction transaction = ClientTransaction.obtain(r.app.getThread(), r.token);
            transaction.addCallback(EnterPipRequestedItem.obtain());
            this.mService.getLifecycleManager().scheduleTransaction(transaction);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to send enter pip requested item: " + r.intent.getComponent(), e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPictureInPictureStateChanged(ActivityRecord r, PictureInPictureUiState pipState) {
        if (!r.inPinnedWindowingMode()) {
            throw new IllegalStateException("Activity is not in PIP mode");
        }
        try {
            ClientTransaction transaction = ClientTransaction.obtain(r.app.getThread(), r.token);
            transaction.addCallback(PipStateTransactionItem.obtain(pipState));
            this.mService.getLifecycleManager().scheduleTransaction(transaction);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to send pip state transaction item: " + r.intent.getComponent(), e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldDisablePipInSpecialCase() {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mIsInFreeformProcess) {
                    Slog.d(TAG, "disable pip in freeform toggle process");
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

    public void toggleFreeformWindowingMode(IBinder token) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r == null) {
                    throw new IllegalArgumentException("toggleFreeformWindowingMode: No activity record matching token=" + token);
                }
                Task rootTask = r.getRootTask();
                if (rootTask == null) {
                    throw new IllegalStateException("toggleFreeformWindowingMode: the activity doesn't have a root task");
                }
                if (!rootTask.inFreeformWindowingMode() && rootTask.getWindowingMode() != 1) {
                    throw new IllegalStateException("toggleFreeformWindowingMode: You can only toggle between fullscreen and freeform.");
                }
                if (rootTask.inFreeformWindowingMode()) {
                    this.mIsInFreeformProcess = true;
                    rootTask.setWindowingMode(1);
                    this.mIsInFreeformProcess = false;
                } else if (!r.supportsFreeform()) {
                    throw new IllegalStateException("This activity is currently not freeform-enabled");
                } else {
                    if (rootTask.getParent().inFreeformWindowingMode()) {
                        rootTask.setWindowingMode(0);
                    } else {
                        rootTask.setWindowingMode(5);
                    }
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void startLockTaskModeByToken(IBinder token) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.forTokenLocked(token);
                if (r != null) {
                    this.mService.startLockTaskMode(r.getTask(), false);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void stopLockTaskModeByToken(IBinder token) {
        this.mService.stopLockTaskModeInternal(token, false);
    }

    public void showLockTaskEscapeMessage(IBinder token) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (ActivityRecord.forTokenLocked(token) != null) {
                    this.mService.getLockTaskController().showLockTaskToast();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void setTaskDescription(IBinder token, ActivityManager.TaskDescription td) {
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.setTaskDescription(td);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1105=4] */
    public boolean showAssistFromActivity(IBinder token, Bundle args) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord caller = ActivityRecord.forTokenLocked(token);
                Task topRootTask = this.mService.getTopDisplayFocusedRootTask();
                ActivityRecord top = topRootTask != null ? topRootTask.getTopNonFinishingActivity() : null;
                if (top != caller) {
                    Slog.w(TAG, "showAssistFromActivity failed: caller " + caller + " is not current top " + top);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                } else if (top.nowVisible) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return this.mAssistUtils.showSessionForActiveService(args, 8, (IVoiceInteractionSessionShowCallback) null, token);
                } else {
                    Slog.w(TAG, "showAssistFromActivity failed: caller " + caller + " is not visible");
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean isRootVoiceInteraction(IBinder token) {
        boolean z;
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                z = r != null && r.rootVoiceInteraction;
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return z;
    }

    public void startLocalVoiceInteraction(IBinder callingActivity, Bundle options) {
        Slog.i(TAG, "Activity tried to startLocalVoiceInteraction");
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                Task topRootTask = this.mService.getTopDisplayFocusedRootTask();
                ActivityRecord activity = topRootTask != null ? topRootTask.getTopNonFinishingActivity() : null;
                if (ActivityRecord.forTokenLocked(callingActivity) != activity) {
                    throw new SecurityException("Only focused activity can call startVoiceInteraction");
                }
                if (this.mService.mRunningVoice == null && activity.getTask().voiceSession == null && activity.voiceSession == null) {
                    if (activity.pendingVoiceInteractionStart) {
                        Slog.w(TAG, "Pending start of voice interaction already.");
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    activity.pendingVoiceInteractionStart = true;
                    WindowManagerService.resetPriorityAfterLockedSection();
                    ((VoiceInteractionManagerInternal) LocalServices.getService(VoiceInteractionManagerInternal.class)).startLocalVoiceInteraction(callingActivity, options);
                    return;
                }
                Slog.w(TAG, "Already in a voice interaction, cannot start new voice interaction");
                WindowManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void stopLocalVoiceInteraction(IBinder callingActivity) {
        ((VoiceInteractionManagerInternal) LocalServices.getService(VoiceInteractionManagerInternal.class)).stopLocalVoiceInteraction(callingActivity);
    }

    public void setShowWhenLocked(IBinder token, boolean showWhenLocked) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.setShowWhenLocked(showWhenLocked);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void setInheritShowWhenLocked(IBinder token, boolean inheritShowWhenLocked) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.setInheritShowWhenLocked(inheritShowWhenLocked);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void setTurnScreenOn(IBinder token, boolean turnScreenOn) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.setTurnScreenOn(turnScreenOn);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void reportActivityFullyDrawn(IBinder token, boolean restoredFromBundle) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.reportFullyDrawnLocked(restoredFromBundle);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:18:0x0043, code lost:
        if (r0.isForceEnableCustomTransition != false) goto L25;
     */
    /* JADX WARN: Removed duplicated region for block: B:17:0x0041 A[Catch: all -> 0x0067, TRY_LEAVE, TryCatch #1 {all -> 0x0067, blocks: (B:4:0x0009, B:6:0x0012, B:8:0x001c, B:10:0x0028, B:15:0x0030, B:17:0x0041), top: B:33:0x0009 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void overridePendingTransition(IBinder token, String packageName, int enterAnim, int exitAnim, int backgroundColor) {
        boolean z;
        long origId = Binder.clearCallingIdentity();
        synchronized (this.mGlobalLock) {
            try {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                    if (r != null && r.isState(ActivityRecord.State.RESUMED, ActivityRecord.State.PAUSING)) {
                        AppTransition appTransition = r.mDisplayContent.mAppTransition;
                        boolean z2 = false;
                        if (!r.mOverrideTaskTransition && !r.isForceEnableCustomTransition) {
                            z = false;
                            appTransition.overridePendingAppTransition(packageName, enterAnim, exitAnim, backgroundColor, null, null, z);
                            TransitionController transitionController = r.mTransitionController;
                            if (!r.mOverrideTaskTransition) {
                            }
                            z2 = true;
                            transitionController.setOverrideAnimation(TransitionInfo.AnimationOptions.makeCustomAnimOptions(packageName, enterAnim, exitAnim, backgroundColor, z2), null, null);
                        }
                        z = true;
                        appTransition.overridePendingAppTransition(packageName, enterAnim, exitAnim, backgroundColor, null, null, z);
                        TransitionController transitionController2 = r.mTransitionController;
                        if (!r.mOverrideTaskTransition) {
                        }
                        z2 = true;
                        transitionController2.setOverrideAnimation(TransitionInfo.AnimationOptions.makeCustomAnimOptions(packageName, enterAnim, exitAnim, backgroundColor, z2), null, null);
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
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public int setVrMode(IBinder token, boolean enabled, ComponentName packageName) {
        ActivityRecord r;
        this.mService.enforceSystemHasVrFeature();
        VrManagerInternal vrService = (VrManagerInternal) LocalServices.getService(VrManagerInternal.class);
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                r = ActivityRecord.isInRootTaskLocked(token);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        if (r == null) {
            throw new IllegalArgumentException();
        }
        int err = vrService.hasVrPackage(packageName, r.mUserId);
        if (err != 0) {
            return err;
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                r.requestedVrComponent = enabled ? packageName : null;
                if (r.isFocusedActivityOnDisplay()) {
                    this.mService.applyUpdateVrModeLocked(r);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
            return 0;
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void setRecentsScreenshotEnabled(IBinder token, boolean enabled) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.setRecentsScreenshotEnabled(enabled);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void restartActivityProcessIfVisible(IBinder token) {
        ActivityTaskManagerService.enforceTaskPermission("restartActivityProcess");
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.restartProcessIfVisible();
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void invalidateHomeTaskSnapshot(IBinder token) {
        ActivityRecord r;
        if (token == null) {
            ActivityTaskManagerService.enforceTaskPermission("invalidateHomeTaskSnapshot");
        }
        synchronized (this.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (token == null) {
                    Task rootTask = this.mService.mRootWindowContainer.getDefaultTaskDisplayArea().getRootHomeTask();
                    r = rootTask != null ? rootTask.topRunningActivity() : null;
                } else {
                    r = ActivityRecord.isInRootTaskLocked(token);
                }
                if (r != null && r.isActivityTypeHome()) {
                    this.mService.mWindowManager.mTaskSnapshotController.removeSnapshotCache(r.getTask().mTaskId);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void dismissKeyguard(IBinder token, IKeyguardDismissCallback callback, CharSequence message) {
        if (message != null) {
            this.mService.mAmInternal.enforceCallingPermission("android.permission.SHOW_KEYGUARD_MESSAGE", "dismissKeyguard");
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                this.mService.mKeyguardController.dismissKeyguard(token, callback, message);
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void registerRemoteAnimations(IBinder token, RemoteAnimationDefinition definition) {
        this.mService.mAmInternal.enforceCallingPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", "registerRemoteAnimations");
        definition.setCallingPidUid(Binder.getCallingPid(), Binder.getCallingUid());
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.registerRemoteAnimations(definition);
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void unregisterRemoteAnimations(IBinder token) {
        this.mService.mAmInternal.enforceCallingPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", "unregisterRemoteAnimations");
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null) {
                    r.unregisterRemoteAnimations();
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private boolean finishingEmbeddingTask(ActivityRecord r, ActivityRecord taskRoot) {
        TaskFragment taskFragment = r.getTaskFragment();
        if (r == taskRoot && taskFragment != null && taskFragment.getCompanionTaskFragment() != null && "in.mohalla.sharechat/.home.main.HomeActivity".equals(r.shortComponentName)) {
            return true;
        }
        return false;
    }

    private boolean isRelativeTaskRootActivity(final ActivityRecord r, ActivityRecord taskRoot) {
        TaskFragment taskFragment = r.getTaskFragment();
        return (taskFragment == null || taskRoot == null || taskRoot.getTaskFragment() == null || r != taskFragment.getActivity(new Predicate() { // from class: com.android.server.wm.ActivityClientController$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ActivityClientController.lambda$isRelativeTaskRootActivity$3(ActivityRecord.this, (ActivityRecord) obj);
            }
        }, false) || taskRoot.getTaskFragment().getCompanionTaskFragment() != taskFragment) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$isRelativeTaskRootActivity$3(ActivityRecord r, ActivityRecord ar) {
        return !ar.finishing || ar == r;
    }

    private boolean isTopActivityInTaskFragment(ActivityRecord activity) {
        return activity.getTaskFragment().topRunningActivity() == activity;
    }

    private void requestCallbackFinish(IRequestFinishCallback callback) {
        try {
            callback.requestFinish();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to invoke request finish callback", e);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1496=6] */
    public void onBackPressedOnTaskRoot(IBinder token, IRequestFinishCallback callback) {
        long origId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Task task = r.getTask();
                ActivityRecord root = task.getRootActivity(false, true);
                boolean isTaskRoot = r == root;
                if (isTaskRoot) {
                    if (this.mService.mWindowOrganizerController.mTaskOrganizerController.handleInterceptBackPressedOnTaskRoot(r.getRootTask())) {
                        WindowManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                } else if (!isRelativeTaskRootActivity(r, root)) {
                    requestCallbackFinish(callback);
                    WindowManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                boolean isLastRunningActivity = isTopActivityInTaskFragment(isTaskRoot ? root : r);
                boolean isBaseActivity = root.mActivityComponent.equals(task.realActivity);
                Intent baseActivityIntent = isBaseActivity ? root.intent : null;
                boolean launchedFromHome = root.isLaunchSourceType(2);
                WindowManagerService.resetPriorityAfterLockedSection();
                ActivityRecord r2 = ActivityRecord.isInRootTaskLocked(token);
                Task task2 = r2 != null ? r2.getTask() : null;
                if ((task2 == null || !task2.getConfiguration().windowConfiguration.isThunderbackWindow()) && baseActivityIntent != null && isLastRunningActivity && ((launchedFromHome && ActivityRecord.isMainIntent(baseActivityIntent)) || isLauncherActivity(baseActivityIntent.getComponent()))) {
                    moveActivityTaskToBack(token, true);
                } else {
                    requestCallbackFinish(callback);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private boolean isLauncherActivity(ComponentName activity) {
        ParceledListSlice<ResolveInfo> resolved;
        Intent queryIntent = new Intent("android.intent.action.MAIN");
        queryIntent.addCategory("android.intent.category.LAUNCHER");
        queryIntent.setPackage(activity.getPackageName());
        try {
            resolved = this.mService.getPackageManager().queryIntentActivities(queryIntent, (String) null, 0L, this.mContext.getUserId());
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to query intent activities", e);
        }
        if (resolved == null) {
            return false;
        }
        for (ResolveInfo ri : resolved.getList()) {
            if (ri.getComponentInfo().getComponentName().equals(activity)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isLucidIgnored(ActivityInfo aInfo) {
        if (aInfo == null) {
            return false;
        }
        int theme = aInfo.getThemeResource();
        if (theme != 16974877) {
            return false;
        }
        return true;
    }
}
