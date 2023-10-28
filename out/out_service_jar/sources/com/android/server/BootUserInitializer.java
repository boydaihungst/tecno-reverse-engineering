package com.android.server;

import android.content.ContentResolver;
import android.content.pm.UserInfo;
import android.provider.Settings;
import com.android.server.am.ActivityManagerService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.utils.Slogf;
import com.android.server.utils.TimingsTraceAndSlog;
import java.util.List;
/* loaded from: classes.dex */
final class BootUserInitializer {
    private static final boolean DEBUG = true;
    private static final String TAG = BootUserInitializer.class.getSimpleName();
    private final ActivityManagerService mAms;
    private final ContentResolver mContentResolver;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BootUserInitializer(ActivityManagerService am, ContentResolver contentResolver) {
        this.mAms = am;
        this.mContentResolver = contentResolver;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [96=4] */
    public void init(TimingsTraceAndSlog t) {
        String str = TAG;
        Slogf.i(str, "init())");
        provisionHeadlessSystemUser();
        UserManagerInternal um = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        t.traceBegin("get-existing-users");
        List<UserInfo> existingUsers = um.getUsers(true);
        t.traceEnd();
        Slogf.d(str, "%d existing users", Integer.valueOf(existingUsers.size()));
        int initialUserId = -10000;
        int i = 0;
        while (true) {
            if (i >= existingUsers.size()) {
                break;
            }
            UserInfo user = existingUsers.get(i);
            String str2 = TAG;
            Slogf.d(str2, "User at position %d: %s", Integer.valueOf(i), user.toFullString());
            if (user.id != 0 && user.isFull()) {
                Slogf.d(str2, "Found initial user: %d", Integer.valueOf(user.id));
                initialUserId = user.id;
                break;
            }
            i++;
        }
        if (initialUserId == -10000) {
            String str3 = TAG;
            Slogf.d(str3, "Creating initial user");
            t.traceBegin("create-initial-user");
            try {
                UserInfo newUser = um.createUserEvenWhenDisallowed("Real User", "android.os.usertype.full.SECONDARY", 2, null, null);
                Slogf.i(str3, "Created initial user: %s", newUser.toFullString());
                initialUserId = newUser.id;
            } catch (Exception e) {
                Slogf.wtf(TAG, "failed to created initial user", e);
                return;
            } finally {
                t.traceEnd();
            }
        }
        unlockSystemUser(t);
        switchToInitialUser(initialUserId);
    }

    private void provisionHeadlessSystemUser() {
        if (isDeviceProvisioned()) {
            Slogf.d(TAG, "provisionHeadlessSystemUser(): already provisioned");
            return;
        }
        String str = TAG;
        Slogf.i(str, "Marking USER_SETUP_COMPLETE for system user");
        Settings.Secure.putInt(this.mContentResolver, "user_setup_complete", 1);
        Slogf.i(str, "Marking DEVICE_PROVISIONED for system user");
        Settings.Global.putInt(this.mContentResolver, "device_provisioned", 1);
    }

    private boolean isDeviceProvisioned() {
        try {
            return Settings.Global.getInt(this.mContentResolver, "device_provisioned") == 1;
        } catch (Exception e) {
            Slogf.wtf(TAG, "DEVICE_PROVISIONED setting not found.", e);
            return false;
        }
    }

    private void unlockSystemUser(TimingsTraceAndSlog t) {
        String str = TAG;
        Slogf.i(str, "Unlocking system user");
        t.traceBegin("unlock-system-user");
        try {
            t.traceBegin("am.startUser");
            boolean started = this.mAms.startUserInBackgroundWithListener(0, null);
            t.traceEnd();
            if (!started) {
                Slogf.w(str, "could not restart system user in background; trying unlock instead");
                t.traceBegin("am.unlockUser");
                boolean unlocked = this.mAms.unlockUser(0, null, null, null);
                t.traceEnd();
                if (!unlocked) {
                    Slogf.w(str, "could not unlock system user either");
                }
            }
        } finally {
            t.traceEnd();
        }
    }

    private void switchToInitialUser(int initialUserId) {
        String str = TAG;
        Slogf.i(str, "Switching to initial user %d", Integer.valueOf(initialUserId));
        boolean started = this.mAms.startUserInForegroundWithListener(initialUserId, null);
        if (!started) {
            Slogf.wtf(str, "Failed to start user %d in foreground", Integer.valueOf(initialUserId));
        }
    }
}
