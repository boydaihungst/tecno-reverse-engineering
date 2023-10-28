package com.android.settingslib;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import java.util.Objects;
/* loaded from: classes2.dex */
public class RestrictedLockUtils {
    public static EnforcedAdmin getProfileOrDeviceOwner(Context context, UserHandle user) {
        return getProfileOrDeviceOwner(context, null, user);
    }

    public static EnforcedAdmin getProfileOrDeviceOwner(Context context, String enforcedRestriction, UserHandle user) {
        DevicePolicyManager dpm;
        ComponentName adminComponent;
        if (user == null || (dpm = (DevicePolicyManager) context.getSystemService("device_policy")) == null) {
            return null;
        }
        try {
            Context userContext = context.createPackageContextAsUser(context.getPackageName(), 0, user);
            ComponentName adminComponent2 = ((DevicePolicyManager) userContext.getSystemService(DevicePolicyManager.class)).getProfileOwner();
            if (adminComponent2 != null) {
                return new EnforcedAdmin(adminComponent2, enforcedRestriction, user);
            }
            if (!Objects.equals(dpm.getDeviceOwnerUser(), user) || (adminComponent = dpm.getDeviceOwnerComponentOnAnyUser()) == null) {
                return null;
            }
            return new EnforcedAdmin(adminComponent, enforcedRestriction, user);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void sendShowAdminSupportDetailsIntent(Context context, EnforcedAdmin admin) {
        Intent intent = getShowAdminSupportDetailsIntent(context, admin);
        int targetUserId = UserHandle.myUserId();
        if (admin != null) {
            if (admin.user != null && isCurrentUserOrProfile(context, admin.user.getIdentifier())) {
                targetUserId = admin.user.getIdentifier();
            }
            intent.putExtra("android.app.extra.RESTRICTION", admin.enforcedRestriction);
        }
        context.startActivityAsUser(intent, UserHandle.of(targetUserId));
    }

    public static Intent getShowAdminSupportDetailsIntent(Context context, EnforcedAdmin admin) {
        Intent intent = new Intent("android.settings.SHOW_ADMIN_SUPPORT_DETAILS");
        if (admin != null) {
            if (admin.component != null) {
                intent.putExtra("android.app.extra.DEVICE_ADMIN", admin.component);
            }
            intent.putExtra("android.intent.extra.USER", admin.user);
        }
        return intent;
    }

    public static boolean isCurrentUserOrProfile(Context context, int userId) {
        UserManager um = (UserManager) context.getSystemService(UserManager.class);
        return um.getUserProfiles().contains(UserHandle.of(userId));
    }

    /* loaded from: classes2.dex */
    public static class EnforcedAdmin {
        public static final EnforcedAdmin MULTIPLE_ENFORCED_ADMIN = new EnforcedAdmin();
        public ComponentName component;
        public String enforcedRestriction;
        public UserHandle user;

        public static EnforcedAdmin createDefaultEnforcedAdminWithRestriction(String enforcedRestriction) {
            EnforcedAdmin enforcedAdmin = new EnforcedAdmin();
            enforcedAdmin.enforcedRestriction = enforcedRestriction;
            return enforcedAdmin;
        }

        public EnforcedAdmin(ComponentName component, UserHandle user) {
            this.component = null;
            this.enforcedRestriction = null;
            this.user = null;
            this.component = component;
            this.user = user;
        }

        public EnforcedAdmin(ComponentName component, String enforcedRestriction, UserHandle user) {
            this.component = null;
            this.enforcedRestriction = null;
            this.user = null;
            this.component = component;
            this.enforcedRestriction = enforcedRestriction;
            this.user = user;
        }

        public EnforcedAdmin(EnforcedAdmin other) {
            this.component = null;
            this.enforcedRestriction = null;
            this.user = null;
            if (other == null) {
                throw new IllegalArgumentException();
            }
            this.component = other.component;
            this.enforcedRestriction = other.enforcedRestriction;
            this.user = other.user;
        }

        public EnforcedAdmin() {
            this.component = null;
            this.enforcedRestriction = null;
            this.user = null;
        }

        public static EnforcedAdmin combine(EnforcedAdmin admin1, EnforcedAdmin admin2) {
            if (admin1 == null) {
                return admin2;
            }
            if (admin2 == null) {
                return admin1;
            }
            if (admin1.equals(admin2)) {
                return admin1;
            }
            if (!admin1.enforcedRestriction.equals(admin2.enforcedRestriction)) {
                throw new IllegalArgumentException("Admins with different restriction cannot be combined");
            }
            return MULTIPLE_ENFORCED_ADMIN;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EnforcedAdmin that = (EnforcedAdmin) o;
            if (Objects.equals(this.user, that.user) && Objects.equals(this.component, that.component) && Objects.equals(this.enforcedRestriction, that.enforcedRestriction)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(this.component, this.enforcedRestriction, this.user);
        }

        public String toString() {
            return "EnforcedAdmin{component=" + this.component + ", enforcedRestriction='" + this.enforcedRestriction + ", user=" + this.user + '}';
        }
    }
}
