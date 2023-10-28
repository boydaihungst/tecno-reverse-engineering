package com.android.server.wm;

import android.app.ActivityManager;
import android.app.IAppTask;
import android.app.IApplicationThread;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class AppTaskImpl extends IAppTask.Stub {
    private static final String TAG = "AppTaskImpl";
    private final int mCallingUid;
    private final ActivityTaskManagerService mService;
    private final int mTaskId;

    public AppTaskImpl(ActivityTaskManagerService service, int taskId, int callingUid) {
        this.mService = service;
        this.mTaskId = taskId;
        this.mCallingUid = callingUid;
    }

    private void checkCallerOrSystemOrRoot() {
        if (this.mCallingUid != Binder.getCallingUid() && 1000 != Binder.getCallingUid() && Binder.getCallingUid() != 0) {
            throw new SecurityException("Caller " + this.mCallingUid + " does not match caller of getAppTasks(): " + Binder.getCallingUid());
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            throw ActivityTaskManagerService.logAndRethrowRuntimeExceptionOnTransact(TAG, e);
        }
    }

    public void finishAndRemoveTask() {
        checkCallerOrSystemOrRoot();
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                if (!this.mService.mTaskSupervisor.removeTaskById(this.mTaskId, false, true, "finish-and-remove-task")) {
                    throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                }
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public ActivityManager.RecentTaskInfo getTaskInfo() {
        ActivityManager.RecentTaskInfo createRecentTaskInfo;
        checkCallerOrSystemOrRoot();
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                Task task = this.mService.mRootWindowContainer.anyTaskForId(this.mTaskId, 1);
                if (task == null) {
                    throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                }
                createRecentTaskInfo = this.mService.getRecentTasks().createRecentTaskInfo(task, false, true);
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return createRecentTaskInfo;
    }

    public void moveToFront(IApplicationThread appThread, String callingPackage) {
        checkCallerOrSystemOrRoot();
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        this.mService.assertPackageMatchesCallingUid(callingPackage);
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                synchronized (this.mService.mGlobalLock) {
                    try {
                        WindowManagerService.boostPriorityForLockedSection();
                        WindowProcessController callerApp = null;
                        if (appThread != null) {
                            callerApp = this.mService.getProcessController(appThread);
                        }
                        ActivityStarter starter = this.mService.getActivityStartController().obtainStarter(null, "moveToFront");
                        if (!starter.shouldAbortBackgroundActivityStart(callingUid, callingPid, callingPackage, -1, -1, callerApp, null, false, null, null) || this.mService.isBackgroundActivityStartsEnabled()) {
                            WindowManagerService.resetPriorityAfterLockedSection();
                            this.mService.mTaskSupervisor.startActivityFromRecents(callingPid, callingUid, this.mTaskId, null);
                            return;
                        }
                        WindowManagerService.resetPriorityAfterLockedSection();
                    } catch (Throwable th) {
                        th = th;
                        WindowManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public int startActivity(IBinder whoThread, String callingPackage, String callingFeatureId, Intent intent, String resolvedType, Bundle bOptions) {
        Task task;
        IApplicationThread appThread;
        checkCallerOrSystemOrRoot();
        this.mService.assertPackageMatchesCallingUid(callingPackage);
        int callingUser = UserHandle.getCallingUserId();
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                task = this.mService.mRootWindowContainer.anyTaskForId(this.mTaskId, 1);
                if (task == null) {
                    throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                }
                appThread = IApplicationThread.Stub.asInterface(whoThread);
                if (appThread == null) {
                    throw new IllegalArgumentException("Bad app thread " + appThread);
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        return this.mService.getActivityStartController().obtainStarter(intent, TAG).setCaller(appThread).setCallingPackage(callingPackage).setCallingFeatureId(callingFeatureId).setResolvedType(resolvedType).setActivityOptions(bOptions).setUserId(callingUser).setInTask(task).execute();
    }

    public void setExcludeFromRecents(boolean exclude) {
        checkCallerOrSystemOrRoot();
        synchronized (this.mService.mGlobalLock) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                long origId = Binder.clearCallingIdentity();
                Task task = this.mService.mRootWindowContainer.anyTaskForId(this.mTaskId, 1);
                if (task == null) {
                    throw new IllegalArgumentException("Unable to find task ID " + this.mTaskId);
                }
                Intent intent = task.getBaseIntent();
                if (exclude) {
                    intent.addFlags(8388608);
                } else {
                    intent.setFlags(intent.getFlags() & (-8388609));
                }
                Binder.restoreCallingIdentity(origId);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }
}
