package com.android.server;

import android.annotation.SystemApi;
import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
@SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
/* loaded from: classes.dex */
public abstract class SystemService {
    protected static final boolean DEBUG_USER = false;
    public static final int PHASE_ACTIVITY_MANAGER_READY = 550;
    public static final int PHASE_BOOT_COMPLETED = 1000;
    public static final int PHASE_DEVICE_SPECIFIC_SERVICES_READY = 520;
    public static final int PHASE_LOCK_SETTINGS_READY = 480;
    public static final int PHASE_SYSTEM_SERVICES_READY = 500;
    public static final int PHASE_THIRD_PARTY_APPS_CAN_START = 600;
    public static final int PHASE_WAIT_FOR_DEFAULT_DISPLAY = 100;
    public static final int PHASE_WAIT_FOR_SENSOR_SERVICE = 200;
    private final Context mContext;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface BootPhase {
    }

    public abstract void onStart();

    @SystemApi(client = SystemApi.Client.SYSTEM_SERVER)
    /* loaded from: classes.dex */
    public static final class TargetUser {
        private final boolean mFull;
        private final boolean mManagedProfile;
        private final boolean mPreCreated;
        private final int mUserId;

        public TargetUser(UserInfo userInfo) {
            this.mUserId = userInfo.id;
            this.mFull = userInfo.isFull();
            this.mManagedProfile = userInfo.isManagedProfile();
            this.mPreCreated = userInfo.preCreated;
        }

        public boolean isFull() {
            return this.mFull;
        }

        public boolean isManagedProfile() {
            return this.mManagedProfile;
        }

        public boolean isPreCreated() {
            return this.mPreCreated;
        }

        public UserHandle getUserHandle() {
            return UserHandle.of(this.mUserId);
        }

        public int getUserIdentifier() {
            return this.mUserId;
        }

        public String toString() {
            return Integer.toString(this.mUserId);
        }

        public void dump(PrintWriter pw) {
            pw.print(getUserIdentifier());
            if (isFull() || isManagedProfile()) {
                pw.print('(');
                if (isFull()) {
                    pw.print("full");
                }
                if (isManagedProfile()) {
                    if (0 != 0) {
                        pw.print(',');
                    }
                    pw.print("mp");
                }
                pw.print(')');
            }
        }
    }

    /* loaded from: classes.dex */
    public static final class UserCompletedEventType {
        public static final int EVENT_TYPE_USER_STARTING = 1;
        public static final int EVENT_TYPE_USER_SWITCHING = 4;
        public static final int EVENT_TYPE_USER_UNLOCKED = 2;
        private final int mEventType;

        @Retention(RetentionPolicy.SOURCE)
        /* loaded from: classes.dex */
        public @interface EventTypesFlag {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public UserCompletedEventType(int eventType) {
            this.mEventType = eventType;
        }

        public static UserCompletedEventType newUserCompletedEventTypeForTest(int eventType) {
            return new UserCompletedEventType(eventType);
        }

        public boolean includesOnUserStarting() {
            return (this.mEventType & 1) != 0;
        }

        public boolean includesOnUserUnlocked() {
            return (this.mEventType & 2) != 0;
        }

        public boolean includesOnUserSwitching() {
            return (this.mEventType & 4) != 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            if (includesOnUserSwitching()) {
                sb.append("|Switching");
            }
            if (includesOnUserUnlocked()) {
                sb.append("|Unlocked");
            }
            if (includesOnUserStarting()) {
                sb.append("|Starting");
            }
            if (sb.length() > 1) {
                sb.append("|");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    public SystemService(Context context) {
        this.mContext = context;
    }

    public final Context getContext() {
        return this.mContext;
    }

    public final Context getUiContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }

    public final boolean isSafeMode() {
        return getManager().isSafeMode();
    }

    public void onBootPhase(int phase) {
    }

    public boolean isUserSupported(TargetUser user) {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpSupportedUsers(PrintWriter pw, String prefix) {
        List<UserInfo> allUsers = UserManager.get(this.mContext).getUsers();
        List<Integer> supportedUsers = new ArrayList<>(allUsers.size());
        for (int i = 0; i < allUsers.size(); i++) {
            UserInfo user = allUsers.get(i);
            if (isUserSupported(new TargetUser(user))) {
                supportedUsers.add(Integer.valueOf(user.id));
            }
        }
        if (supportedUsers.isEmpty()) {
            pw.print(prefix);
            pw.println("No supported users");
            return;
        }
        int size = supportedUsers.size();
        pw.print(prefix);
        pw.print(size);
        pw.print(" supported user");
        if (size > 1) {
            pw.print("s");
        }
        pw.print(": ");
        pw.println(supportedUsers);
    }

    public void onUserStarting(TargetUser user) {
    }

    public void onUserUnlocking(TargetUser user) {
    }

    public void onUserUnlocked(TargetUser user) {
    }

    public void onUserSwitching(TargetUser from, TargetUser to) {
    }

    public void onUserStopping(TargetUser user) {
    }

    public void onUserStopped(TargetUser user) {
    }

    public void onUserCompletedEvent(TargetUser user, UserCompletedEventType eventType) {
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void publishBinderService(String name, IBinder service) {
        publishBinderService(name, service, false);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void publishBinderService(String name, IBinder service, boolean allowIsolated) {
        publishBinderService(name, service, allowIsolated, 8);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final void publishBinderService(String name, IBinder service, boolean allowIsolated, int dumpPriority) {
        ServiceManager.addService(name, service, allowIsolated, dumpPriority);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final IBinder getBinderService(String name) {
        return ServiceManager.getService(name);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final <T> void publishLocalService(Class<T> type, T service) {
        LocalServices.addService(type, service);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public final <T> T getLocalService(Class<T> type) {
        return (T) LocalServices.getService(type);
    }

    private SystemServiceManager getManager() {
        return (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
    }
}
