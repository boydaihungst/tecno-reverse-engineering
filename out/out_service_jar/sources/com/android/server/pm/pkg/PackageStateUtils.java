package com.android.server.pm.pkg;

import android.content.pm.ComponentInfo;
import android.util.SparseArray;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.component.ParsedMainComponent;
/* loaded from: classes2.dex */
public class PackageStateUtils {
    public static boolean isMatch(PackageState packageState, long flags) {
        if ((1048576 & flags) != 0) {
            return packageState.isSystem();
        }
        return true;
    }

    public static int[] queryInstalledUsers(PackageStateInternal pkgState, int[] users, boolean installed) {
        int num = 0;
        for (int user : users) {
            if (pkgState.getUserStateOrDefault(user).isInstalled() == installed) {
                num++;
            }
        }
        int[] res = new int[num];
        int num2 = 0;
        for (int user2 : users) {
            if (pkgState.getUserStateOrDefault(user2).isInstalled() == installed) {
                res[num2] = user2;
                num2++;
            }
        }
        return res;
    }

    public static boolean isEnabledAndMatches(PackageStateInternal packageState, ComponentInfo componentInfo, long flags, int userId) {
        if (packageState == null) {
            return false;
        }
        PackageUserStateInternal userState = packageState.getUserStateOrDefault(userId);
        return PackageUserStateUtils.isMatch(userState, componentInfo, flags);
    }

    public static boolean isEnabledAndMatches(PackageStateInternal packageState, ParsedMainComponent component, long flags, int userId) {
        AndroidPackage pkg;
        if (packageState == null || (pkg = packageState.getPkg()) == null) {
            return false;
        }
        PackageUserStateInternal userState = packageState.getUserStateOrDefault(userId);
        return PackageUserStateUtils.isMatch(userState, packageState.isSystem(), pkg.isEnabled(), component, flags);
    }

    public static long getEarliestFirstInstallTime(SparseArray<? extends PackageUserStateInternal> userStatesInternal) {
        if (userStatesInternal == null || userStatesInternal.size() == 0) {
            return 0L;
        }
        long earliestFirstInstallTime = JobStatus.NO_LATEST_RUNTIME;
        for (int i = 0; i < userStatesInternal.size(); i++) {
            long firstInstallTime = userStatesInternal.valueAt(i).getFirstInstallTime();
            if (firstInstallTime != 0 && firstInstallTime < earliestFirstInstallTime) {
                earliestFirstInstallTime = firstInstallTime;
            }
        }
        if (earliestFirstInstallTime == JobStatus.NO_LATEST_RUNTIME) {
            return 0L;
        }
        return earliestFirstInstallTime;
    }
}
