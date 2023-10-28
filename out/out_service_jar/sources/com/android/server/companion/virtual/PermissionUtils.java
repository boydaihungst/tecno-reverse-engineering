package com.android.server.companion.virtual;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.UserHandle;
import android.util.Slog;
/* loaded from: classes.dex */
class PermissionUtils {
    private static final String LOG_TAG = "VDM.PermissionUtils";

    PermissionUtils() {
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [56=5] */
    public static boolean validateCallingPackageName(Context context, String callingPackage) {
        int callingUid = Binder.getCallingUid();
        long token = Binder.clearCallingIdentity();
        try {
            int packageUid = context.getPackageManager().getPackageUidAsUser(callingPackage, UserHandle.getUserId(callingUid));
            if (packageUid != callingUid) {
                Slog.e(LOG_TAG, "validatePackageName: App with package name " + callingPackage + " is UID " + packageUid + " but caller is " + callingUid);
                return false;
            }
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(LOG_TAG, "validatePackageName: App with package name " + callingPackage + " does not exist");
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
