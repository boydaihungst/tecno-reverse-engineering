package com.android.server.pm;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.BroadcastOptions;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;
/* loaded from: classes2.dex */
public final class BroadcastHelper {
    private static final boolean DEBUG_BROADCASTS = false;
    private static final String[] INSTANT_APP_BROADCAST_PERMISSION = {"android.permission.ACCESS_INSTANT_APPS"};
    private final ActivityManagerInternal mAmInternal;
    private final Context mContext;
    private final UserManagerInternal mUmInternal;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastHelper(PackageManagerServiceInjector injector) {
        this.mUmInternal = injector.getUserManagerInternal();
        this.mAmInternal = injector.getActivityManagerInternal();
        this.mContext = injector.getContext();
    }

    public void sendPackageBroadcast(String action, String pkg, Bundle extras, int flags, String targetPkg, IIntentReceiver finishedReceiver, int[] userIds, int[] instantUserIds, SparseArray<int[]> broadcastAllowList, Bundle bOptions) {
        int[] resolvedUserIds;
        try {
            IActivityManager am = ActivityManager.getService();
            if (am == null) {
                return;
            }
            if (userIds == null) {
                resolvedUserIds = am.getRunningUserIds();
            } else {
                resolvedUserIds = userIds;
            }
            if (ArrayUtils.isEmpty(instantUserIds)) {
                doSendBroadcast(action, pkg, extras, flags, targetPkg, finishedReceiver, resolvedUserIds, false, broadcastAllowList, bOptions);
            } else {
                doSendBroadcast(action, pkg, extras, flags, targetPkg, finishedReceiver, instantUserIds, true, null, bOptions);
            }
        } catch (RemoteException e) {
        }
    }

    public void doSendBroadcast(String action, String pkg, Bundle extras, int flags, String targetPkg, IIntentReceiver finishedReceiver, int[] userIds, boolean isInstantApp, SparseArray<int[]> broadcastAllowList, Bundle bOptions) {
        Uri uri;
        for (int userId : userIds) {
            int[] iArr = null;
            if (pkg != null) {
                uri = Uri.fromParts("package", pkg, null);
            } else {
                uri = null;
            }
            Intent intent = new Intent(action, uri);
            String[] requiredPermissions = isInstantApp ? INSTANT_APP_BROADCAST_PERMISSION : null;
            if (extras != null) {
                intent.putExtras(extras);
            }
            if (targetPkg != null) {
                intent.setPackage(targetPkg);
            }
            int uid = intent.getIntExtra("android.intent.extra.UID", -1);
            if (uid >= 0 && UserHandle.getUserId(uid) != userId) {
                intent.putExtra("android.intent.extra.UID", UserHandle.getUid(userId, UserHandle.getAppId(uid)));
            }
            if (broadcastAllowList != null && PackageManagerService.PLATFORM_PACKAGE_NAME.equals(targetPkg)) {
                intent.putExtra("android.intent.extra.VISIBILITY_ALLOW_LIST", broadcastAllowList.get(userId));
            }
            intent.putExtra("android.intent.extra.user_handle", userId);
            intent.addFlags(flags | 67108864);
            ActivityManagerInternal activityManagerInternal = this.mAmInternal;
            boolean z = finishedReceiver != null;
            if (broadcastAllowList != null) {
                iArr = broadcastAllowList.get(userId);
            }
            activityManagerInternal.broadcastIntent(intent, finishedReceiver, requiredPermissions, z, userId, iArr, bOptions);
        }
    }

    public void sendResourcesChangedBroadcast(boolean mediaStatus, boolean replacing, ArrayList<String> pkgList, int[] uidArr, IIntentReceiver finishedReceiver) {
        sendResourcesChangedBroadcast(mediaStatus, replacing, (String[]) pkgList.toArray(new String[pkgList.size()]), uidArr, finishedReceiver);
    }

    public void sendResourcesChangedBroadcast(boolean mediaStatus, boolean replacing, String[] pkgList, int[] uidArr, IIntentReceiver finishedReceiver) {
        int size = pkgList.length;
        if (size > 0) {
            Bundle extras = new Bundle();
            extras.putStringArray("android.intent.extra.changed_package_list", pkgList);
            if (uidArr != null) {
                extras.putIntArray("android.intent.extra.changed_uid_list", uidArr);
            }
            if (replacing) {
                extras.putBoolean("android.intent.extra.REPLACING", replacing);
            }
            String action = mediaStatus ? "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE" : "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";
            sendPackageBroadcast(action, null, extras, 0, null, finishedReceiver, null, null, null, null);
        }
    }

    public void sendBootCompletedBroadcastToSystemApp(String packageName, boolean includeStopped, int userId) {
        if (!this.mUmInternal.isUserRunning(userId)) {
            return;
        }
        IActivityManager am = ActivityManager.getService();
        try {
            Intent lockedBcIntent = new Intent("android.intent.action.LOCKED_BOOT_COMPLETED").setPackage(packageName);
            if (includeStopped) {
                lockedBcIntent.addFlags(32);
            }
            String[] requiredPermissions = {"android.permission.RECEIVE_BOOT_COMPLETED"};
            BroadcastOptions bOptions = getTemporaryAppAllowlistBroadcastOptions(202);
            try {
                am.broadcastIntentWithFeature((IApplicationThread) null, (String) null, lockedBcIntent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, requiredPermissions, (String[]) null, (String[]) null, -1, bOptions.toBundle(), false, false, userId);
                try {
                    if (this.mUmInternal.isUserUnlockingOrUnlocked(userId)) {
                        Intent bcIntent = new Intent("android.intent.action.BOOT_COMPLETED").setPackage(packageName);
                        if (includeStopped) {
                            bcIntent.addFlags(32);
                        }
                        am.broadcastIntentWithFeature((IApplicationThread) null, (String) null, bcIntent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, requiredPermissions, (String[]) null, (String[]) null, -1, bOptions.toBundle(), false, false, userId);
                    }
                } catch (RemoteException e) {
                    e = e;
                    throw e.rethrowFromSystemServer();
                }
            } catch (RemoteException e2) {
                e = e2;
            }
        } catch (RemoteException e3) {
            e = e3;
        }
    }

    public BroadcastOptions getTemporaryAppAllowlistBroadcastOptions(int reasonCode) {
        long duration = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        ActivityManagerInternal activityManagerInternal = this.mAmInternal;
        if (activityManagerInternal != null) {
            duration = activityManagerInternal.getBootTimeTempAllowListDuration();
        }
        BroadcastOptions bOptions = BroadcastOptions.makeBasic();
        bOptions.setTemporaryAppAllowlist(duration, 0, reasonCode, "");
        return bOptions;
    }

    public void sendPackageChangedBroadcast(String packageName, boolean dontKillApp, ArrayList<String> componentNames, int packageUid, String reason, int[] userIds, int[] instantUserIds, SparseArray<int[]> broadcastAllowList) {
        if (PackageManagerService.DEBUG_INSTALL) {
            Log.v("PackageManager", "Sending package changed: package=" + packageName + " components=" + componentNames);
        }
        Bundle extras = new Bundle(4);
        int i = 0;
        extras.putString("android.intent.extra.changed_component_name", componentNames.get(0));
        String[] nameList = new String[componentNames.size()];
        componentNames.toArray(nameList);
        extras.putStringArray("android.intent.extra.changed_component_name_list", nameList);
        extras.putBoolean("android.intent.extra.DONT_KILL_APP", dontKillApp);
        extras.putInt("android.intent.extra.UID", packageUid);
        if (reason != null) {
            extras.putString("android.intent.extra.REASON", reason);
        }
        if (!componentNames.contains(packageName)) {
            i = 1073741824;
        }
        int flags = i;
        sendPackageBroadcast("android.intent.action.PACKAGE_CHANGED", packageName, extras, flags, null, null, userIds, instantUserIds, broadcastAllowList, null);
    }

    public static void sendDeviceCustomizationReadyBroadcast() {
        Intent intent = new Intent("android.intent.action.DEVICE_CUSTOMIZATION_READY");
        intent.setFlags(16777216);
        IActivityManager am = ActivityManager.getService();
        String[] requiredPermissions = {"android.permission.RECEIVE_DEVICE_CUSTOMIZATION_READY"};
        try {
            am.broadcastIntentWithFeature((IApplicationThread) null, (String) null, intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, requiredPermissions, (String[]) null, (String[]) null, -1, (Bundle) null, false, false, -1);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void sendSessionCommitBroadcast(PackageInstaller.SessionInfo sessionInfo, int userId, int launcherUid, ComponentName launcherComponent, String appPredictionServicePackage) {
        if (launcherComponent != null) {
            Intent launcherIntent = new Intent("android.content.pm.action.SESSION_COMMITTED").putExtra("android.content.pm.extra.SESSION", sessionInfo).putExtra("android.intent.extra.USER", UserHandle.of(userId)).setPackage(launcherComponent.getPackageName());
            this.mContext.sendBroadcastAsUser(launcherIntent, UserHandle.of(launcherUid));
        }
        if (appPredictionServicePackage != null) {
            Intent predictorIntent = new Intent("android.content.pm.action.SESSION_COMMITTED").putExtra("android.content.pm.extra.SESSION", sessionInfo).putExtra("android.intent.extra.USER", UserHandle.of(userId)).setPackage(appPredictionServicePackage);
            this.mContext.sendBroadcastAsUser(predictorIntent, UserHandle.of(launcherUid));
        }
    }

    public void sendPreferredActivityChangedBroadcast(int userId) {
        IActivityManager am = ActivityManager.getService();
        if (am == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED");
        intent.putExtra("android.intent.extra.user_handle", userId);
        intent.addFlags(67108864);
        try {
            am.broadcastIntentWithFeature((IApplicationThread) null, (String) null, intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, (String[]) null, (String[]) null, (String[]) null, -1, (Bundle) null, false, false, userId);
        } catch (RemoteException e) {
        }
    }

    public void sendPackageAddedForNewUsers(String packageName, int appId, int[] userIds, int[] instantUserIds, int dataLoaderType, SparseArray<int[]> broadcastAllowlist) {
        Bundle extras = new Bundle(1);
        int uid = UserHandle.getUid(ArrayUtils.isEmpty(userIds) ? instantUserIds[0] : userIds[0], appId);
        extras.putInt("android.intent.extra.UID", uid);
        extras.putInt("android.content.pm.extra.DATA_LOADER_TYPE", dataLoaderType);
        sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, 0, null, null, userIds, instantUserIds, broadcastAllowlist, null);
    }

    public void sendFirstLaunchBroadcast(String pkgName, String installerPkg, int[] userIds, int[] instantUserIds) {
        sendPackageBroadcast("android.intent.action.PACKAGE_FIRST_LAUNCH", pkgName, null, 0, installerPkg, null, userIds, instantUserIds, null, null);
    }
}
