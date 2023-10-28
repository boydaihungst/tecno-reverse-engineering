package com.android.server.companion;

import android.companion.AssociationInfo;
import android.companion.AssociationRequest;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import com.android.internal.app.IAppOpsService;
import com.android.server.slice.SliceClientPermissions;
import java.util.Collections;
import java.util.Map;
/* loaded from: classes.dex */
final class PermissionsUtils {
    private static final Map<String, String> DEVICE_PROFILE_TO_PERMISSION;
    private static IAppOpsService sAppOpsService;

    static {
        Map<String, String> map = new ArrayMap<>();
        map.put("android.app.role.COMPANION_DEVICE_WATCH", "android.permission.REQUEST_COMPANION_PROFILE_WATCH");
        map.put("android.app.role.COMPANION_DEVICE_APP_STREAMING", "android.permission.REQUEST_COMPANION_PROFILE_APP_STREAMING");
        map.put("android.app.role.SYSTEM_AUTOMOTIVE_PROJECTION", "android.permission.REQUEST_COMPANION_PROFILE_AUTOMOTIVE_PROJECTION");
        map.put("android.app.role.COMPANION_DEVICE_COMPUTER", "android.permission.REQUEST_COMPANION_PROFILE_COMPUTER");
        DEVICE_PROFILE_TO_PERMISSION = Collections.unmodifiableMap(map);
        sAppOpsService = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enforcePermissionsForAssociation(Context context, AssociationRequest request, int packageUid) {
        enforceRequestDeviceProfilePermissions(context, request.getDeviceProfile(), packageUid);
        if (request.isSelfManaged()) {
            enforceRequestSelfManagedPermission(context, packageUid);
        }
    }

    static void enforceRequestDeviceProfilePermissions(Context context, String deviceProfile, int packageUid) {
        if (deviceProfile == null) {
            return;
        }
        Map<String, String> map = DEVICE_PROFILE_TO_PERMISSION;
        if (!map.containsKey(deviceProfile)) {
            throw new IllegalArgumentException("Unsupported device profile: " + deviceProfile);
        }
        String permission = map.get(deviceProfile);
        if (context.checkPermission(permission, Binder.getCallingPid(), packageUid) != 0) {
            throw new SecurityException("Application must hold " + permission + " to associate with a device with " + deviceProfile + " profile.");
        }
    }

    static void enforceRequestSelfManagedPermission(Context context, int packageUid) {
        if (context.checkPermission("android.permission.REQUEST_COMPANION_SELF_MANAGED", Binder.getCallingPid(), packageUid) != 0) {
            throw new SecurityException("Application does not hold android.permission.REQUEST_COMPANION_SELF_MANAGED");
        }
    }

    static boolean checkCallerCanInteractWithUserId(Context context, int userId) {
        return UserHandle.getCallingUserId() == userId || context.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS") == 0;
    }

    static void enforceCallerCanInteractWithUserId(Context context, int userId) {
        if (UserHandle.getCallingUserId() == userId) {
            return;
        }
        context.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS", null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enforceCallerIsSystemOrCanInteractWithUserId(Context context, int userId) {
        if (Binder.getCallingUid() == 1000) {
            return;
        }
        enforceCallerCanInteractWithUserId(context, userId);
    }

    static boolean checkCallerIsSystemOr(int userId, String packageName) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1000) {
            return true;
        }
        return UserHandle.getCallingUserId() == userId && checkPackage(callingUid, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enforceCallerIsSystemOr(int userId, String packageName) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1000) {
            return;
        }
        int callingUserId = UserHandle.getCallingUserId();
        if (UserHandle.getCallingUserId() != userId) {
            throw new SecurityException("Calling UserId (" + callingUserId + ") does not match the expected UserId (" + userId + ")");
        }
        if (!checkPackage(callingUid, packageName)) {
            throw new SecurityException(packageName + " doesn't belong to calling uid (" + callingUid + ")");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean checkCallerCanManageCompanionDevice(Context context) {
        return Binder.getCallingUid() == 1000 || context.checkCallingPermission("android.permission.MANAGE_COMPANION_DEVICES") == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enforceCallerCanManageCompanionDevice(Context context, String message) {
        if (Binder.getCallingUid() == 1000) {
            return;
        }
        context.enforceCallingPermission("android.permission.MANAGE_COMPANION_DEVICES", message);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void enforceCallerCanManageAssociationsForPackage(Context context, int userId, String packageName, String actionDescription) {
        if (checkCallerCanManageAssociationsForPackage(context, userId, packageName)) {
            return;
        }
        throw new SecurityException("Caller (uid=" + Binder.getCallingUid() + ") does not have permissions to " + (actionDescription != null ? actionDescription : "manage associations") + " for u" + userId + SliceClientPermissions.SliceAuthority.DELIMITER + packageName);
    }

    static boolean checkCallerCanManageAssociationsForPackage(Context context, int userId, String packageName) {
        if (checkCallerIsSystemOr(userId, packageName)) {
            return true;
        }
        if (checkCallerCanInteractWithUserId(context, userId)) {
            return checkCallerCanManageCompanionDevice(context);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AssociationInfo sanitizeWithCallerChecks(Context context, AssociationInfo association) {
        if (association == null) {
            return null;
        }
        int userId = association.getUserId();
        String packageName = association.getPackageName();
        if (!checkCallerCanManageAssociationsForPackage(context, userId, packageName)) {
            return null;
        }
        return association;
    }

    private static boolean checkPackage(int uid, String packageName) {
        try {
            return getAppOpsService().checkPackage(uid, packageName) == 0;
        } catch (RemoteException e) {
            return true;
        }
    }

    private static IAppOpsService getAppOpsService() {
        if (sAppOpsService == null) {
            synchronized (PermissionsUtils.class) {
                if (sAppOpsService == null) {
                    sAppOpsService = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
                }
            }
        }
        return sAppOpsService;
    }

    private PermissionsUtils() {
    }
}
