package com.android.server.companion;

import android.app.role.RoleManager;
import android.companion.AssociationInfo;
import android.content.Context;
import android.os.UserHandle;
import android.util.Slog;
import java.util.List;
import java.util.function.Consumer;
/* loaded from: classes.dex */
final class RolesUtils {
    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isRoleHolder(Context context, int userId, String packageName, String role) {
        RoleManager roleManager = (RoleManager) context.getSystemService(RoleManager.class);
        List<String> roleHolders = roleManager.getRoleHoldersAsUser(role, UserHandle.of(userId));
        return roleHolders.contains(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void addRoleHolderForAssociation(Context context, AssociationInfo associationInfo) {
        final String deviceProfile = associationInfo.getDeviceProfile();
        if (deviceProfile == null) {
            return;
        }
        RoleManager roleManager = (RoleManager) context.getSystemService(RoleManager.class);
        final String packageName = associationInfo.getPackageName();
        final int userId = associationInfo.getUserId();
        UserHandle userHandle = UserHandle.of(userId);
        roleManager.addRoleHolderAsUser(deviceProfile, packageName, 1, userHandle, context.getMainExecutor(), new Consumer() { // from class: com.android.server.companion.RolesUtils$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RolesUtils.lambda$addRoleHolderForAssociation$0(userId, packageName, deviceProfile, (Boolean) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addRoleHolderForAssociation$0(int userId, String packageName, String deviceProfile, Boolean success) {
        if (!success.booleanValue()) {
            Slog.e("CompanionDeviceManagerService", "Failed to add u" + userId + "\\" + packageName + " to the list of " + deviceProfile + " holders.");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void removeRoleHolderForAssociation(Context context, AssociationInfo associationInfo) {
        final String deviceProfile = associationInfo.getDeviceProfile();
        if (deviceProfile == null) {
            return;
        }
        RoleManager roleManager = (RoleManager) context.getSystemService(RoleManager.class);
        final String packageName = associationInfo.getPackageName();
        final int userId = associationInfo.getUserId();
        UserHandle userHandle = UserHandle.of(userId);
        roleManager.removeRoleHolderAsUser(deviceProfile, packageName, 1, userHandle, context.getMainExecutor(), new Consumer() { // from class: com.android.server.companion.RolesUtils$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                RolesUtils.lambda$removeRoleHolderForAssociation$1(userId, packageName, deviceProfile, (Boolean) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$removeRoleHolderForAssociation$1(int userId, String packageName, String deviceProfile, Boolean success) {
        if (!success.booleanValue()) {
            Slog.e("CompanionDeviceManagerService", "Failed to remove u" + userId + "\\" + packageName + " from the list of " + deviceProfile + " holders.");
        }
    }

    private RolesUtils() {
    }
}
