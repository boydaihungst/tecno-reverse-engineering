package com.android.server.location.injector;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.IActivityManager;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import java.io.FileDescriptor;
import java.util.Arrays;
/* loaded from: classes.dex */
public class SystemUserInfoHelper extends UserInfoHelper {
    private IActivityManager mActivityManager;
    private ActivityManagerInternal mActivityManagerInternal;
    private final Context mContext;
    private UserManager mUserManager;

    public SystemUserInfoHelper(Context context) {
        this.mContext = context;
    }

    protected final ActivityManagerInternal getActivityManagerInternal() {
        synchronized (this) {
            if (this.mActivityManagerInternal == null) {
                this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
            }
        }
        return this.mActivityManagerInternal;
    }

    protected final IActivityManager getActivityManager() {
        synchronized (this) {
            if (this.mActivityManager == null) {
                this.mActivityManager = ActivityManager.getService();
            }
        }
        return this.mActivityManager;
    }

    protected final UserManager getUserManager() {
        synchronized (this) {
            if (this.mUserManager == null) {
                this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
            }
        }
        return this.mUserManager;
    }

    @Override // com.android.server.location.injector.UserInfoHelper
    public int[] getRunningUserIds() {
        IActivityManager activityManager = getActivityManager();
        if (activityManager != null) {
            long identity = Binder.clearCallingIdentity();
            try {
                try {
                    return activityManager.getRunningUserIds();
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        return new int[0];
    }

    @Override // com.android.server.location.injector.UserInfoHelper
    public boolean isCurrentUserId(int userId) {
        ActivityManagerInternal activityManagerInternal = getActivityManagerInternal();
        if (activityManagerInternal != null) {
            long identity = Binder.clearCallingIdentity();
            try {
                return activityManagerInternal.isCurrentProfile(userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        return false;
    }

    @Override // com.android.server.location.injector.UserInfoHelper
    public int getCurrentUserId() {
        ActivityManagerInternal activityManagerInternal = getActivityManagerInternal();
        if (activityManagerInternal != null) {
            long identity = Binder.clearCallingIdentity();
            try {
                return activityManagerInternal.getCurrentUserId();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        return -10000;
    }

    @Override // com.android.server.location.injector.UserInfoHelper
    protected int[] getProfileIds(int userId) {
        UserManager userManager = getUserManager();
        Preconditions.checkState(userManager != null);
        long identity = Binder.clearCallingIdentity();
        try {
            return userManager.getEnabledProfileIds(userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override // com.android.server.location.injector.UserInfoHelper
    public void dump(FileDescriptor fd, IndentingPrintWriter pw, String[] args) {
        int[] runningUserIds = getRunningUserIds();
        if (runningUserIds.length > 1) {
            pw.println("running users: u" + Arrays.toString(runningUserIds));
        }
        ActivityManagerInternal activityManagerInternal = getActivityManagerInternal();
        if (activityManagerInternal == null) {
            return;
        }
        int[] currentProfileIds = activityManagerInternal.getCurrentProfileIds();
        pw.println("current users: u" + Arrays.toString(currentProfileIds));
        UserManager userManager = getUserManager();
        if (userManager != null) {
            for (int userId : currentProfileIds) {
                if (userManager.hasUserRestrictionForUser("no_share_location", UserHandle.of(userId))) {
                    pw.increaseIndent();
                    pw.println("u" + userId + " restricted");
                    pw.decreaseIndent();
                }
            }
        }
    }
}
