package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.KeyguardManager;
import android.app.TaskInfo;
import android.app.admin.DevicePolicyManagerInternal;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.SuspendDialogInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.SparseArray;
import com.android.internal.app.BlockedAppActivity;
import com.android.internal.app.HarmfulAppWarningActivity;
import com.android.internal.app.SuspendedAppActivity;
import com.android.internal.app.UnlaunchableAppActivity;
import com.android.server.LocalServices;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.ActivityInterceptorCallback;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class ActivityStartInterceptor {
    ActivityInfo mAInfo;
    ActivityOptions mActivityOptions;
    private String mCallingFeatureId;
    private String mCallingPackage;
    int mCallingPid;
    int mCallingUid;
    Task mInTask;
    Intent mIntent;
    ResolveInfo mRInfo;
    private int mRealCallingPid;
    private int mRealCallingUid;
    String mResolvedType;
    private final RootWindowContainer mRootWindowContainer;
    private final ActivityTaskManagerService mService;
    private final Context mServiceContext;
    private int mStartFlags;
    private final ActivityTaskSupervisor mSupervisor;
    private int mUserId;
    private UserManager mUserManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityStartInterceptor(ActivityTaskManagerService service, ActivityTaskSupervisor supervisor) {
        this(service, supervisor, service.mRootWindowContainer, service.mContext);
    }

    ActivityStartInterceptor(ActivityTaskManagerService service, ActivityTaskSupervisor supervisor, RootWindowContainer root, Context context) {
        this.mService = service;
        this.mSupervisor = supervisor;
        this.mRootWindowContainer = root;
        this.mServiceContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStates(int userId, int realCallingPid, int realCallingUid, int startFlags, String callingPackage, String callingFeatureId) {
        this.mRealCallingPid = realCallingPid;
        this.mRealCallingUid = realCallingUid;
        this.mUserId = userId;
        this.mStartFlags = startFlags;
        this.mCallingPackage = callingPackage;
        this.mCallingFeatureId = callingFeatureId;
    }

    private IntentSender createIntentSenderForOriginalIntent(int callingUid, int flags) {
        Bundle activityOptions = deferCrossProfileAppsAnimationIfNecessary();
        IIntentSender target = this.mService.getIntentSenderLocked(2, this.mCallingPackage, this.mCallingFeatureId, callingUid, this.mUserId, null, null, 0, new Intent[]{this.mIntent}, new String[]{this.mResolvedType}, flags, activityOptions);
        return new IntentSender(target);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean intercept(Intent intent, ResolveInfo rInfo, ActivityInfo aInfo, String resolvedType, Task inTask, int callingPid, int callingUid, ActivityOptions activityOptions) {
        this.mUserManager = UserManager.get(this.mServiceContext);
        this.mIntent = intent;
        this.mCallingPid = callingPid;
        this.mCallingUid = callingUid;
        this.mRInfo = rInfo;
        this.mAInfo = aInfo;
        this.mResolvedType = resolvedType;
        this.mInTask = inTask;
        this.mActivityOptions = activityOptions;
        if (interceptQuietProfileIfNeeded() || interceptSuspendedPackageIfNeeded() || interceptLockTaskModeViolationPackageIfNeeded() || interceptHarmfulAppIfNeeded() || interceptLockedManagedProfileIfNeeded()) {
            return true;
        }
        SparseArray<ActivityInterceptorCallback> callbacks = this.mService.getActivityInterceptorCallbacks();
        ActivityInterceptorCallback.ActivityInterceptorInfo interceptorInfo = getInterceptorInfo(null);
        for (int i = 0; i < callbacks.size(); i++) {
            ActivityInterceptorCallback callback = callbacks.valueAt(i);
            ActivityInterceptorCallback.ActivityInterceptResult interceptResult = callback.intercept(interceptorInfo);
            if (interceptResult != null) {
                this.mIntent = interceptResult.intent;
                this.mActivityOptions = interceptResult.activityOptions;
                this.mCallingPid = this.mRealCallingPid;
                int i2 = this.mRealCallingUid;
                this.mCallingUid = i2;
                ResolveInfo resolveIntent = this.mSupervisor.resolveIntent(this.mIntent, null, this.mUserId, 0, i2);
                this.mRInfo = resolveIntent;
                this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, resolveIntent, this.mStartFlags, null);
                return true;
            }
        }
        return false;
    }

    private boolean hasCrossProfileAnimation() {
        ActivityOptions activityOptions = this.mActivityOptions;
        return activityOptions != null && activityOptions.getAnimationType() == 12;
    }

    private Bundle deferCrossProfileAppsAnimationIfNecessary() {
        if (hasCrossProfileAnimation()) {
            this.mActivityOptions = null;
            return ActivityOptions.makeOpenCrossProfileAppsAnimation().toBundle();
        }
        return null;
    }

    private boolean interceptQuietProfileIfNeeded() {
        if (!this.mUserManager.isQuietModeEnabled(UserHandle.of(this.mUserId))) {
            return false;
        }
        IntentSender target = createIntentSenderForOriginalIntent(this.mCallingUid, 1342177280);
        this.mIntent = UnlaunchableAppActivity.createInQuietModeDialogIntent(this.mUserId, target);
        this.mCallingPid = this.mRealCallingPid;
        this.mCallingUid = this.mRealCallingUid;
        this.mResolvedType = null;
        UserInfo parent = this.mUserManager.getProfileParent(this.mUserId);
        ResolveInfo resolveIntent = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, parent.id, 0, this.mRealCallingUid);
        this.mRInfo = resolveIntent;
        this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, resolveIntent, this.mStartFlags, null);
        return true;
    }

    private boolean interceptSuspendedByAdminPackage() {
        DevicePolicyManagerInternal devicePolicyManager = (DevicePolicyManagerInternal) LocalServices.getService(DevicePolicyManagerInternal.class);
        if (devicePolicyManager == null) {
            return false;
        }
        Intent createShowAdminSupportIntent = devicePolicyManager.createShowAdminSupportIntent(this.mUserId, true);
        this.mIntent = createShowAdminSupportIntent;
        createShowAdminSupportIntent.putExtra("android.app.extra.RESTRICTION", "policy_suspend_packages");
        this.mCallingPid = this.mRealCallingPid;
        this.mCallingUid = this.mRealCallingUid;
        this.mResolvedType = null;
        UserInfo parent = this.mUserManager.getProfileParent(this.mUserId);
        if (parent != null) {
            this.mRInfo = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, parent.id, 0, this.mRealCallingUid);
        } else {
            this.mRInfo = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, this.mUserId, 0, this.mRealCallingUid);
        }
        this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, this.mRInfo, this.mStartFlags, null);
        return true;
    }

    private boolean interceptSuspendedPackageIfNeeded() {
        PackageManagerInternal pmi;
        Bundle crossProfileOptions;
        ActivityInfo activityInfo = this.mAInfo;
        if (activityInfo == null || activityInfo.applicationInfo == null || (this.mAInfo.applicationInfo.flags & 1073741824) == 0 || (pmi = this.mService.getPackageManagerInternalLocked()) == null) {
            return false;
        }
        String suspendedPackage = this.mAInfo.applicationInfo.packageName;
        String suspendingPackage = pmi.getSuspendingPackage(suspendedPackage, this.mUserId);
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(suspendingPackage)) {
            return interceptSuspendedByAdminPackage();
        }
        SuspendDialogInfo dialogInfo = pmi.getSuspendedDialogInfo(suspendedPackage, suspendingPackage, this.mUserId);
        if (hasCrossProfileAnimation()) {
            crossProfileOptions = ActivityOptions.makeOpenCrossProfileAppsAnimation().toBundle();
        } else {
            crossProfileOptions = null;
        }
        IntentSender target = createIntentSenderForOriginalIntent(this.mCallingUid, 67108864);
        Intent createSuspendedAppInterceptIntent = SuspendedAppActivity.createSuspendedAppInterceptIntent(suspendedPackage, suspendingPackage, dialogInfo, crossProfileOptions, target, this.mUserId);
        this.mIntent = createSuspendedAppInterceptIntent;
        this.mCallingPid = this.mRealCallingPid;
        int i = this.mRealCallingUid;
        this.mCallingUid = i;
        this.mResolvedType = null;
        ResolveInfo resolveIntent = this.mSupervisor.resolveIntent(createSuspendedAppInterceptIntent, null, this.mUserId, 0, i);
        this.mRInfo = resolveIntent;
        this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, resolveIntent, this.mStartFlags, null);
        return true;
    }

    private boolean interceptLockTaskModeViolationPackageIfNeeded() {
        ActivityInfo activityInfo = this.mAInfo;
        if (activityInfo == null || activityInfo.applicationInfo == null) {
            return false;
        }
        LockTaskController controller = this.mService.getLockTaskController();
        String packageName = this.mAInfo.applicationInfo.packageName;
        int lockTaskLaunchMode = ActivityRecord.getLockTaskLaunchMode(this.mAInfo, this.mActivityOptions);
        if (controller.isActivityAllowed(this.mUserId, packageName, lockTaskLaunchMode)) {
            return false;
        }
        Intent createIntent = BlockedAppActivity.createIntent(this.mUserId, this.mAInfo.applicationInfo.packageName);
        this.mIntent = createIntent;
        this.mCallingPid = this.mRealCallingPid;
        int i = this.mRealCallingUid;
        this.mCallingUid = i;
        this.mResolvedType = null;
        ResolveInfo resolveIntent = this.mSupervisor.resolveIntent(createIntent, null, this.mUserId, 0, i);
        this.mRInfo = resolveIntent;
        this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, resolveIntent, this.mStartFlags, null);
        return true;
    }

    private boolean interceptLockedManagedProfileIfNeeded() {
        Intent interceptingIntent = interceptWithConfirmCredentialsIfNeeded(this.mAInfo, this.mUserId);
        if (interceptingIntent == null) {
            return false;
        }
        this.mIntent = interceptingIntent;
        this.mCallingPid = this.mRealCallingPid;
        this.mCallingUid = this.mRealCallingUid;
        this.mResolvedType = null;
        Task task = this.mInTask;
        if (task != null) {
            interceptingIntent.putExtra("android.intent.extra.TASK_ID", task.mTaskId);
            this.mInTask = null;
        }
        if (this.mActivityOptions == null) {
            this.mActivityOptions = ActivityOptions.makeBasic();
        }
        UserInfo parent = this.mUserManager.getProfileParent(this.mUserId);
        ResolveInfo resolveIntent = this.mSupervisor.resolveIntent(this.mIntent, this.mResolvedType, parent.id, 0, this.mRealCallingUid);
        this.mRInfo = resolveIntent;
        this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, resolveIntent, this.mStartFlags, null);
        return true;
    }

    private Intent interceptWithConfirmCredentialsIfNeeded(ActivityInfo aInfo, int userId) {
        if ((aInfo.flags & 8388608) == 0 && this.mService.mAmInternal.shouldConfirmCredentials(userId)) {
            IntentSender target = createIntentSenderForOriginalIntent(this.mCallingUid, 1409286144);
            KeyguardManager km = (KeyguardManager) this.mServiceContext.getSystemService("keyguard");
            Intent newIntent = km.createConfirmDeviceCredentialIntent(null, null, userId, true);
            if (newIntent == null) {
                return null;
            }
            newIntent.setFlags(276840448);
            newIntent.putExtra("android.intent.extra.PACKAGE_NAME", aInfo.packageName);
            newIntent.putExtra("android.intent.extra.INTENT", target);
            return newIntent;
        }
        return null;
    }

    private boolean interceptHarmfulAppIfNeeded() {
        try {
            CharSequence harmfulAppWarning = this.mService.getPackageManager().getHarmfulAppWarning(this.mAInfo.packageName, this.mUserId);
            if (harmfulAppWarning == null) {
                return false;
            }
            IntentSender target = createIntentSenderForOriginalIntent(this.mCallingUid, 1409286144);
            Intent createHarmfulAppWarningIntent = HarmfulAppWarningActivity.createHarmfulAppWarningIntent(this.mServiceContext, this.mAInfo.packageName, target, harmfulAppWarning);
            this.mIntent = createHarmfulAppWarningIntent;
            this.mCallingPid = this.mRealCallingPid;
            int i = this.mRealCallingUid;
            this.mCallingUid = i;
            this.mResolvedType = null;
            ResolveInfo resolveIntent = this.mSupervisor.resolveIntent(createHarmfulAppWarningIntent, null, this.mUserId, 0, i);
            this.mRInfo = resolveIntent;
            this.mAInfo = this.mSupervisor.resolveActivity(this.mIntent, resolveIntent, this.mStartFlags, null);
            return true;
        } catch (RemoteException | IllegalArgumentException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onActivityLaunched(TaskInfo taskInfo, final ActivityRecord r) {
        SparseArray<ActivityInterceptorCallback> callbacks = this.mService.getActivityInterceptorCallbacks();
        Objects.requireNonNull(r);
        ActivityInterceptorCallback.ActivityInterceptorInfo info = getInterceptorInfo(new Runnable() { // from class: com.android.server.wm.ActivityStartInterceptor$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ActivityRecord.this.clearOptionsAnimationForSiblings();
            }
        });
        for (int i = 0; i < callbacks.size(); i++) {
            ActivityInterceptorCallback callback = callbacks.valueAt(i);
            callback.onActivityLaunched(taskInfo, r.info, info);
        }
    }

    private ActivityInterceptorCallback.ActivityInterceptorInfo getInterceptorInfo(Runnable clearOptionsAnimation) {
        return new ActivityInterceptorCallback.ActivityInterceptorInfo(this.mRealCallingUid, this.mRealCallingPid, this.mUserId, this.mCallingPackage, this.mCallingFeatureId, this.mIntent, this.mRInfo, this.mAInfo, this.mResolvedType, this.mCallingPid, this.mCallingUid, this.mActivityOptions, clearOptionsAnimation);
    }
}
