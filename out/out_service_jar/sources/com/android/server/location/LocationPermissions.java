package com.android.server.location;

import android.content.Context;
import android.os.Binder;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/* loaded from: classes.dex */
public final class LocationPermissions {
    public static final int PERMISSION_COARSE = 1;
    public static final int PERMISSION_FINE = 2;
    public static final int PERMISSION_NONE = 0;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface PermissionLevel {
    }

    public static String asPermission(int permissionLevel) {
        switch (permissionLevel) {
            case 1:
                return "android.permission.ACCESS_COARSE_LOCATION";
            case 2:
                return "android.permission.ACCESS_FINE_LOCATION";
            default:
                throw new IllegalArgumentException();
        }
    }

    public static int asAppOp(int permissionLevel) {
        switch (permissionLevel) {
            case 1:
                return 0;
            case 2:
                return 1;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static void enforceCallingOrSelfLocationPermission(Context context, int requiredPermissionLevel) {
        enforceLocationPermission(Binder.getCallingUid(), getPermissionLevel(context, Binder.getCallingUid(), Binder.getCallingPid()), requiredPermissionLevel);
    }

    public static void enforceLocationPermission(Context context, int uid, int pid, int requiredPermissionLevel) {
        enforceLocationPermission(uid, getPermissionLevel(context, uid, pid), requiredPermissionLevel);
    }

    public static void enforceLocationPermission(int uid, int permissionLevel, int requiredPermissionLevel) {
        if (checkLocationPermission(permissionLevel, requiredPermissionLevel)) {
            return;
        }
        if (requiredPermissionLevel == 1) {
            throw new SecurityException("uid " + uid + " does not have android.permission.ACCESS_COARSE_LOCATION or android.permission.ACCESS_FINE_LOCATION.");
        }
        if (requiredPermissionLevel == 2) {
            throw new SecurityException("uid " + uid + " does not have android.permission.ACCESS_FINE_LOCATION.");
        }
    }

    public static void enforceCallingOrSelfBypassPermission(Context context) {
        enforceBypassPermission(context, Binder.getCallingUid(), Binder.getCallingPid());
    }

    public static void enforceBypassPermission(Context context, int uid, int pid) {
        if (context.checkPermission("android.permission.LOCATION_BYPASS", pid, uid) == 0) {
            return;
        }
        throw new SecurityException(WatchlistLoggingHandler.WatchlistEventKeys.UID + uid + " does not have android.permission.LOCATION_BYPASS.");
    }

    public static boolean checkCallingOrSelfLocationPermission(Context context, int requiredPermissionLevel) {
        return checkLocationPermission(getCallingOrSelfPermissionLevel(context), requiredPermissionLevel);
    }

    public static boolean checkLocationPermission(Context context, int uid, int pid, int requiredPermissionLevel) {
        return checkLocationPermission(getPermissionLevel(context, uid, pid), requiredPermissionLevel);
    }

    public static boolean checkLocationPermission(int permissionLevel, int requiredPermissionLevel) {
        return permissionLevel >= requiredPermissionLevel;
    }

    public static int getCallingOrSelfPermissionLevel(Context context) {
        return getPermissionLevel(context, Binder.getCallingUid(), Binder.getCallingPid());
    }

    public static int getPermissionLevel(Context context, int uid, int pid) {
        if (context.checkPermission("android.permission.ACCESS_FINE_LOCATION", pid, uid) == 0) {
            return 2;
        }
        if (context.checkPermission("android.permission.ACCESS_COARSE_LOCATION", pid, uid) == 0) {
            return 1;
        }
        return 0;
    }

    private LocationPermissions() {
    }
}
