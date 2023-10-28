package com.android.server.pm;

import android.app.ActivityManagerInternal;
import android.app.BroadcastOptions;
import android.os.Bundle;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PackageRemovedInfo {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    SparseArray<int[]> mBroadcastAllowList;
    boolean mDataRemoved;
    SparseArray<Integer> mInstallReasons;
    String mInstallerPackageName;
    boolean mIsExternal;
    boolean mIsStaticSharedLib;
    boolean mIsUpdate;
    int[] mOrigUsers;
    final PackageSender mPackageSender;
    boolean mRemovedForAllUsers;
    String mRemovedPackage;
    SparseArray<Integer> mUninstallReasons;
    int mUid = -1;
    int mRemovedAppId = -1;
    int[] mRemovedUsers = null;
    int[] mBroadcastUsers = null;
    int[] mInstantUserIds = null;
    boolean mIsRemovedPackageSystemUpdate = false;
    InstallArgs mArgs = null;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageRemovedInfo(PackageSender packageSender) {
        this.mPackageSender = packageSender;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendPackageRemovedBroadcasts(boolean killApp, boolean removedBySystem) {
        sendPackageRemovedBroadcastInternal(killApp, removedBySystem);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendSystemPackageUpdatedBroadcasts() {
        if (this.mIsRemovedPackageSystemUpdate) {
            sendSystemPackageUpdatedBroadcastsInternal();
        }
    }

    private void sendSystemPackageUpdatedBroadcastsInternal() {
        Bundle extras = new Bundle(2);
        int i = this.mRemovedAppId;
        if (i < 0) {
            i = this.mUid;
        }
        extras.putInt("android.intent.extra.UID", i);
        extras.putBoolean("android.intent.extra.REPLACING", true);
        this.mPackageSender.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", this.mRemovedPackage, extras, 0, null, null, null, null, this.mBroadcastAllowList, null);
        String str = this.mInstallerPackageName;
        if (str != null) {
            this.mPackageSender.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", this.mRemovedPackage, extras, 0, str, null, null, null, null, null);
            this.mPackageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", this.mRemovedPackage, extras, 0, this.mInstallerPackageName, null, null, null, null, null);
        }
        this.mPackageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", this.mRemovedPackage, extras, 0, null, null, null, null, this.mBroadcastAllowList, null);
        this.mPackageSender.sendPackageBroadcast("android.intent.action.MY_PACKAGE_REPLACED", null, null, 0, this.mRemovedPackage, null, null, null, null, getTemporaryAppAllowlistBroadcastOptions(FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_PACKAGE_REPLACED).toBundle());
    }

    private static BroadcastOptions getTemporaryAppAllowlistBroadcastOptions(int reasonCode) {
        long duration = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        ActivityManagerInternal amInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        if (amInternal != null) {
            duration = amInternal.getBootTimeTempAllowListDuration();
        }
        BroadcastOptions bOptions = BroadcastOptions.makeBasic();
        bOptions.setTemporaryAppAllowlist(duration, 0, reasonCode, "");
        return bOptions;
    }

    private void sendPackageRemovedBroadcastInternal(boolean killApp, boolean removedBySystem) {
        if (this.mIsStaticSharedLib) {
            return;
        }
        Bundle extras = new Bundle();
        int i = this.mRemovedAppId;
        if (i < 0) {
            i = this.mUid;
        }
        int removedUid = i;
        extras.putInt("android.intent.extra.UID", removedUid);
        extras.putBoolean("android.intent.extra.DATA_REMOVED", this.mDataRemoved);
        extras.putBoolean("android.intent.extra.DONT_KILL_APP", !killApp);
        extras.putBoolean("android.intent.extra.USER_INITIATED", !removedBySystem);
        boolean isReplace = this.mIsUpdate || this.mIsRemovedPackageSystemUpdate;
        if (isReplace) {
            extras.putBoolean("android.intent.extra.REPLACING", true);
        }
        extras.putBoolean("android.intent.extra.REMOVED_FOR_ALL_USERS", this.mRemovedForAllUsers);
        String str = this.mRemovedPackage;
        if (str != null) {
            this.mPackageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REMOVED", str, extras, 0, null, null, this.mBroadcastUsers, this.mInstantUserIds, this.mBroadcastAllowList, null);
            String str2 = this.mInstallerPackageName;
            if (str2 != null) {
                this.mPackageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REMOVED", this.mRemovedPackage, extras, 0, str2, null, this.mBroadcastUsers, this.mInstantUserIds, null, null);
            }
            this.mPackageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REMOVED_INTERNAL", this.mRemovedPackage, extras, 0, PackageManagerService.PLATFORM_PACKAGE_NAME, null, this.mBroadcastUsers, this.mInstantUserIds, this.mBroadcastAllowList, null);
            if (this.mDataRemoved && !this.mIsRemovedPackageSystemUpdate) {
                this.mPackageSender.sendPackageBroadcast("android.intent.action.PACKAGE_FULLY_REMOVED", this.mRemovedPackage, extras, 16777216, null, null, this.mBroadcastUsers, this.mInstantUserIds, this.mBroadcastAllowList, null);
                this.mPackageSender.notifyPackageRemoved(this.mRemovedPackage, removedUid);
            }
        }
        if (this.mRemovedAppId >= 0) {
            if (isReplace) {
                extras.putString("android.intent.extra.PACKAGE_NAME", this.mRemovedPackage);
            }
            this.mPackageSender.sendPackageBroadcast("android.intent.action.UID_REMOVED", null, extras, 16777216, null, null, this.mBroadcastUsers, this.mInstantUserIds, this.mBroadcastAllowList, null);
        }
    }

    public void populateUsers(int[] userIds, PackageSetting deletedPackageSetting) {
        this.mRemovedUsers = userIds;
        if (userIds == null) {
            this.mBroadcastUsers = null;
            return;
        }
        int[] iArr = EMPTY_INT_ARRAY;
        this.mBroadcastUsers = iArr;
        this.mInstantUserIds = iArr;
        for (int i = userIds.length - 1; i >= 0; i--) {
            int userId = userIds[i];
            if (deletedPackageSetting.getInstantApp(userId)) {
                this.mInstantUserIds = ArrayUtils.appendInt(this.mInstantUserIds, userId);
            } else {
                this.mBroadcastUsers = ArrayUtils.appendInt(this.mBroadcastUsers, userId);
            }
        }
    }
}
