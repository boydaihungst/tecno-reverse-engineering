package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.WindowConfiguration;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.view.RemoteAnimationAdapter;
import android.window.WindowContainerToken;
import com.android.internal.util.ArrayUtils;
/* loaded from: classes2.dex */
public class SafeActivityOptions {
    private static final String TAG = "ActivityTaskManager";
    private ActivityOptions mCallerOptions;
    private final int mOriginalCallingPid;
    private final int mOriginalCallingUid;
    private final ActivityOptions mOriginalOptions;
    private int mRealCallingPid;
    private int mRealCallingUid;

    public static SafeActivityOptions fromBundle(Bundle bOptions) {
        if (bOptions != null) {
            return new SafeActivityOptions(ActivityOptions.fromBundle(bOptions));
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static SafeActivityOptions fromBundle(Bundle bOptions, int callingPid, int callingUid) {
        if (bOptions != null) {
            return new SafeActivityOptions(ActivityOptions.fromBundle(bOptions), callingPid, callingUid);
        }
        return null;
    }

    public SafeActivityOptions(ActivityOptions options) {
        this.mOriginalCallingPid = Binder.getCallingPid();
        this.mOriginalCallingUid = Binder.getCallingUid();
        this.mOriginalOptions = options;
    }

    private SafeActivityOptions(ActivityOptions options, int callingPid, int callingUid) {
        this.mOriginalCallingPid = callingPid;
        this.mOriginalCallingUid = callingUid;
        this.mOriginalOptions = options;
    }

    public void setCallerOptions(ActivityOptions options) {
        this.mRealCallingPid = Binder.getCallingPid();
        this.mRealCallingUid = Binder.getCallingUid();
        this.mCallerOptions = options;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityOptions getOptions(ActivityRecord r) throws SecurityException {
        return getOptions(r.intent, r.info, r.app, r.mTaskSupervisor);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityOptions getOptions(ActivityTaskSupervisor supervisor) throws SecurityException {
        return getOptions(null, null, null, supervisor);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityOptions getOptions(Intent intent, ActivityInfo aInfo, WindowProcessController callerApp, ActivityTaskSupervisor supervisor) throws SecurityException {
        ActivityOptions activityOptions = this.mOriginalOptions;
        if (activityOptions != null) {
            checkPermissions(intent, aInfo, callerApp, supervisor, activityOptions, this.mOriginalCallingPid, this.mOriginalCallingUid);
            setCallingPidUidForRemoteAnimationAdapter(this.mOriginalOptions, this.mOriginalCallingPid, this.mOriginalCallingUid);
        }
        ActivityOptions activityOptions2 = this.mCallerOptions;
        if (activityOptions2 != null) {
            checkPermissions(intent, aInfo, callerApp, supervisor, activityOptions2, this.mRealCallingPid, this.mRealCallingUid);
            setCallingPidUidForRemoteAnimationAdapter(this.mCallerOptions, this.mRealCallingPid, this.mRealCallingUid);
        }
        return mergeActivityOptions(this.mOriginalOptions, this.mCallerOptions);
    }

    private void setCallingPidUidForRemoteAnimationAdapter(ActivityOptions options, int callingPid, int callingUid) {
        RemoteAnimationAdapter adapter = options.getRemoteAnimationAdapter();
        if (adapter == null) {
            return;
        }
        if (callingPid == Process.myPid()) {
            Slog.wtf(TAG, "Safe activity options constructed after clearing calling id");
        } else {
            adapter.setCallingPidUid(callingPid, callingUid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityOptions getOriginalOptions() {
        return this.mOriginalOptions;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Bundle popAppVerificationBundle() {
        ActivityOptions activityOptions = this.mOriginalOptions;
        if (activityOptions != null) {
            return activityOptions.popAppVerificationBundle();
        }
        return null;
    }

    private void abort() {
        ActivityOptions activityOptions = this.mOriginalOptions;
        if (activityOptions != null) {
            ActivityOptions.abort(activityOptions);
        }
        ActivityOptions activityOptions2 = this.mCallerOptions;
        if (activityOptions2 != null) {
            ActivityOptions.abort(activityOptions2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void abort(SafeActivityOptions options) {
        if (options != null) {
            options.abort();
        }
    }

    ActivityOptions mergeActivityOptions(ActivityOptions options1, ActivityOptions options2) {
        if (options1 == null) {
            return options2;
        }
        if (options2 == null) {
            return options1;
        }
        Bundle b1 = options1.toBundle();
        Bundle b2 = options2.toBundle();
        b1.putAll(b2);
        return ActivityOptions.fromBundle(b1);
    }

    private void checkPermissions(Intent intent, ActivityInfo aInfo, WindowProcessController callerApp, ActivityTaskSupervisor supervisor, ActivityOptions options, int callingPid, int callingUid) {
        if (options.getLaunchTaskId() != -1 && !supervisor.mRecentTasks.isCallerRecents(callingUid)) {
            int startInTaskPerm = ActivityTaskManagerService.checkPermission("android.permission.START_TASKS_FROM_RECENTS", callingPid, callingUid);
            if (startInTaskPerm == -1) {
                String msg = "Permission Denial: starting " + getIntentString(intent) + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with launchTaskId=" + options.getLaunchTaskId();
                Slog.w(TAG, msg);
                throw new SecurityException(msg);
            }
        }
        WindowContainerToken daToken = options.getLaunchTaskDisplayArea();
        TaskDisplayArea taskDisplayArea = daToken != null ? (TaskDisplayArea) WindowContainer.fromBinder(daToken.asBinder()) : null;
        if (aInfo != null && taskDisplayArea != null && !supervisor.isCallerAllowedToLaunchOnTaskDisplayArea(callingPid, callingUid, taskDisplayArea, aInfo)) {
            String msg2 = "Permission Denial: starting " + getIntentString(intent) + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with launchTaskDisplayArea=" + taskDisplayArea;
            Slog.w(TAG, msg2);
            throw new SecurityException(msg2);
        }
        int launchDisplayId = options.getLaunchDisplayId();
        if (aInfo != null && launchDisplayId != -1 && !supervisor.isCallerAllowedToLaunchOnDisplay(callingPid, callingUid, launchDisplayId, aInfo)) {
            String msg3 = "Permission Denial: starting " + getIntentString(intent) + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with launchDisplayId=" + launchDisplayId;
            Slog.w(TAG, msg3);
            throw new SecurityException(msg3);
        }
        boolean lockTaskMode = options.getLockTaskMode();
        if (aInfo != null && lockTaskMode) {
            if (!supervisor.mService.getLockTaskController().isPackageAllowlisted(UserHandle.getUserId(callingUid), aInfo.packageName)) {
                String msg4 = "Permission Denial: starting " + getIntentString(intent) + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with lockTaskMode=true";
                Slog.w(TAG, msg4);
                throw new SecurityException(msg4);
            }
        }
        boolean overrideTaskTransition = options.getOverrideTaskTransition();
        if (aInfo != null && overrideTaskTransition) {
            int startTasksFromRecentsPerm = ActivityTaskManagerService.checkPermission("android.permission.START_TASKS_FROM_RECENTS", callingPid, callingUid);
            if (startTasksFromRecentsPerm != 0) {
                String msg5 = "Permission Denial: starting " + getIntentString(intent) + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with overrideTaskTransition=true";
                Slog.w(TAG, msg5);
                throw new SecurityException(msg5);
            }
        }
        RemoteAnimationAdapter adapter = options.getRemoteAnimationAdapter();
        if (adapter != null) {
            ActivityTaskManagerService activityTaskManagerService = supervisor.mService;
            if (ActivityTaskManagerService.checkPermission("android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS", callingPid, callingUid) != 0) {
                String msg6 = "Permission Denial: starting " + getIntentString(intent) + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with remoteAnimationAdapter";
                Slog.w(TAG, msg6);
                throw new SecurityException(msg6);
            }
        }
        if (options.getLaunchedFromBubble() && !isSystemOrSystemUI(callingPid, callingUid) && !"com.transsion.applock".equals(getPackageNameForUid(callingUid))) {
            String msg7 = "Permission Denial: starting " + getIntentString(intent) + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with launchedFromBubble=true";
            Slog.w(TAG, msg7);
            throw new SecurityException(msg7);
        }
        int activityType = options.getLaunchActivityType();
        if (activityType != 0 && !isSystemOrSystemUI(callingPid, callingUid)) {
            boolean activityTypeGranted = false;
            if (activityType == 4 && isAssistant(supervisor.mService, callingUid)) {
                activityTypeGranted = true;
            }
            if (!activityTypeGranted) {
                String msg8 = "Permission Denial: starting " + getIntentString(intent) + " from " + callerApp + " (pid=" + callingPid + ", uid=" + callingUid + ") with launchActivityType=" + WindowConfiguration.activityTypeToString(options.getLaunchActivityType());
                Slog.w(TAG, msg8);
                throw new SecurityException(msg8);
            }
        }
    }

    private String getPackageNameForUid(int uid) {
        int version;
        try {
            IPackageManager mPackageManager = AppGlobals.getPackageManager();
            String[] packageNames = mPackageManager.getPackagesForUid(uid);
            if (ArrayUtils.isEmpty(packageNames)) {
                return null;
            }
            String packageName = packageNames[0];
            if (packageNames.length == 1) {
                return packageName;
            }
            int oldestVersion = Integer.MAX_VALUE;
            for (String name : packageNames) {
                ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(name, 0L, 0);
                if (applicationInfo != null && (version = applicationInfo.targetSdkVersion) < oldestVersion) {
                    oldestVersion = version;
                    packageName = name;
                }
            }
            return packageName;
        } catch (RemoteException e) {
            Slog.w(TAG, e);
            return null;
        }
    }

    private boolean isAssistant(ActivityTaskManagerService atmService, int callingUid) {
        int uid;
        if (atmService.mActiveVoiceInteractionServiceComponent == null) {
            return false;
        }
        String assistantPackage = atmService.mActiveVoiceInteractionServiceComponent.getPackageName();
        try {
            uid = AppGlobals.getPackageManager().getPackageUid(assistantPackage, 268435456L, UserHandle.getUserId(callingUid));
        } catch (RemoteException e) {
        }
        return uid == callingUid;
    }

    private boolean isSystemOrSystemUI(int callingPid, int callingUid) {
        if (callingUid == 1000) {
            return true;
        }
        int statusBarPerm = ActivityTaskManagerService.checkPermission("android.permission.STATUS_BAR_SERVICE", callingPid, callingUid);
        return statusBarPerm == 0;
    }

    private String getIntentString(Intent intent) {
        return intent != null ? intent.toString() : "(no intent)";
    }
}
