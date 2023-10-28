package com.android.server.usage;

import android.app.role.OnRoleHoldersChangedListener;
import android.app.role.RoleManager;
import android.app.usage.BroadcastResponseStats;
import android.content.Context;
import android.os.SystemClock;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.LongArrayQueue;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.IndentingPrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class BroadcastResponseStatsTracker {
    static final int NOTIFICATION_EVENT_TYPE_CANCELLED = 2;
    static final int NOTIFICATION_EVENT_TYPE_POSTED = 0;
    static final int NOTIFICATION_EVENT_TYPE_UPDATED = 1;
    static final String TAG = "ResponseStatsTracker";
    private AppStandbyInternal mAppStandby;
    private RoleManager mRoleManager;
    private final Object mLock = new Object();
    private SparseArray<UserBroadcastEvents> mUserBroadcastEvents = new SparseArray<>();
    private SparseArray<SparseArray<UserBroadcastResponseStats>> mUserResponseStats = new SparseArray<>();
    private SparseArray<ArrayMap<String, List<String>>> mExemptedRoleHoldersCache = new SparseArray<>();
    private final OnRoleHoldersChangedListener mRoleHoldersChangedListener = new OnRoleHoldersChangedListener() { // from class: com.android.server.usage.BroadcastResponseStatsTracker$$ExternalSyntheticLambda0
        public final void onRoleHoldersChanged(String str, UserHandle userHandle) {
            BroadcastResponseStatsTracker.this.onRoleHoldersChanged(str, userHandle);
        }
    };
    private BroadcastResponseStatsLogger mLogger = new BroadcastResponseStatsLogger();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface NotificationEventType {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastResponseStatsTracker(AppStandbyInternal appStandby) {
        this.mAppStandby = appStandby;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSystemServicesReady(Context context) {
        RoleManager roleManager = (RoleManager) context.getSystemService(RoleManager.class);
        this.mRoleManager = roleManager;
        roleManager.addOnRoleHoldersChangedListenerAsUser(BackgroundThread.getExecutor(), this.mRoleHoldersChangedListener, UserHandle.ALL);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportBroadcastDispatchEvent(int sourceUid, String targetPackage, UserHandle targetUser, long idForResponseEvent, long timestampMs, int targetUidProcState) {
        this.mLogger.logBroadcastDispatchEvent(sourceUid, targetPackage, targetUser, idForResponseEvent, timestampMs, targetUidProcState);
        if (targetUidProcState <= this.mAppStandby.getBroadcastResponseFgThresholdState() || doesPackageHoldExemptedRole(targetPackage, targetUser) || doesPackageHoldExemptedPermission(targetPackage, targetUser)) {
            return;
        }
        synchronized (this.mLock) {
            try {
                try {
                    ArraySet<BroadcastEvent> broadcastEvents = getOrCreateBroadcastEventsLocked(targetPackage, targetUser);
                    BroadcastEvent broadcastEvent = getOrCreateBroadcastEvent(broadcastEvents, sourceUid, targetPackage, targetUser.getIdentifier(), idForResponseEvent);
                    broadcastEvent.addTimestampMs(timestampMs);
                    recordAndPruneOldBroadcastDispatchTimestamps(broadcastEvent);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportNotificationPosted(String packageName, UserHandle user, long timestampMs) {
        reportNotificationEvent(0, packageName, user, timestampMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportNotificationUpdated(String packageName, UserHandle user, long timestampMs) {
        reportNotificationEvent(1, packageName, user, timestampMs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportNotificationCancelled(String packageName, UserHandle user, long timestampMs) {
        reportNotificationEvent(2, packageName, user, timestampMs);
    }

    private void reportNotificationEvent(int event, String packageName, UserHandle user, long timestampMs) {
        BroadcastResponseStatsTracker broadcastResponseStatsTracker = this;
        broadcastResponseStatsTracker.mLogger.logNotificationEvent(event, packageName, user, timestampMs);
        synchronized (broadcastResponseStatsTracker.mLock) {
            ArraySet<BroadcastEvent> broadcastEvents = broadcastResponseStatsTracker.getBroadcastEventsLocked(packageName, user);
            if (broadcastEvents == null) {
                return;
            }
            long broadcastResponseWindowDurationMs = broadcastResponseStatsTracker.mAppStandby.getBroadcastResponseWindowDurationMs();
            long broadcastsSessionWithResponseDurationMs = broadcastResponseStatsTracker.mAppStandby.getBroadcastSessionsWithResponseDurationMs();
            boolean recordAllBroadcastsSessionsWithinResponseWindow = broadcastResponseStatsTracker.mAppStandby.shouldNoteResponseEventForAllBroadcastSessions();
            int i = 1;
            int i2 = broadcastEvents.size() - 1;
            while (i2 >= 0) {
                BroadcastEvent broadcastEvent = broadcastEvents.valueAt(i2);
                broadcastResponseStatsTracker.recordAndPruneOldBroadcastDispatchTimestamps(broadcastEvent);
                LongArrayQueue dispatchTimestampsMs = broadcastEvent.getTimestampsMs();
                long broadcastsSessionEndTimestampMs = 0;
                while (dispatchTimestampsMs.size() > 0 && dispatchTimestampsMs.peekFirst() < timestampMs) {
                    long dispatchTimestampMs = dispatchTimestampsMs.peekFirst();
                    long elapsedDurationMs = timestampMs - dispatchTimestampMs;
                    if (elapsedDurationMs <= broadcastResponseWindowDurationMs && dispatchTimestampMs >= broadcastsSessionEndTimestampMs) {
                        if (broadcastsSessionEndTimestampMs == 0 || recordAllBroadcastsSessionsWithinResponseWindow) {
                            BroadcastResponseStats responseStats = broadcastResponseStatsTracker.getOrCreateBroadcastResponseStats(broadcastEvent);
                            responseStats.incrementBroadcastsDispatchedCount(i);
                            broadcastsSessionEndTimestampMs = dispatchTimestampMs + broadcastsSessionWithResponseDurationMs;
                            switch (event) {
                                case 0:
                                    responseStats.incrementNotificationsPostedCount(i);
                                    break;
                                case 1:
                                    responseStats.incrementNotificationsUpdatedCount(i);
                                    break;
                                case 2:
                                    responseStats.incrementNotificationsCancelledCount(i);
                                    break;
                                default:
                                    Slog.wtf(TAG, "Unknown event: " + event);
                                    break;
                            }
                        }
                    }
                    dispatchTimestampsMs.removeFirst();
                    i = 1;
                    broadcastResponseStatsTracker = this;
                }
                if (dispatchTimestampsMs.size() == 0) {
                    broadcastEvents.removeAt(i2);
                }
                i2--;
                i = 1;
                broadcastResponseStatsTracker = this;
            }
        }
    }

    private void recordAndPruneOldBroadcastDispatchTimestamps(BroadcastEvent broadcastEvent) {
        LongArrayQueue timestampsMs = broadcastEvent.getTimestampsMs();
        long broadcastResponseWindowDurationMs = this.mAppStandby.getBroadcastResponseWindowDurationMs();
        long broadcastsSessionDurationMs = this.mAppStandby.getBroadcastSessionsDurationMs();
        long nowElapsedMs = SystemClock.elapsedRealtime();
        long broadcastsSessionEndTimestampMs = 0;
        while (timestampsMs.size() > 0 && timestampsMs.peekFirst() < nowElapsedMs - broadcastResponseWindowDurationMs) {
            long eventTimestampMs = timestampsMs.peekFirst();
            if (eventTimestampMs >= broadcastsSessionEndTimestampMs) {
                BroadcastResponseStats responseStats = getOrCreateBroadcastResponseStats(broadcastEvent);
                responseStats.incrementBroadcastsDispatchedCount(1);
                broadcastsSessionEndTimestampMs = eventTimestampMs + broadcastsSessionDurationMs;
            }
            timestampsMs.removeFirst();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<BroadcastResponseStats> queryBroadcastResponseStats(int callingUid, String packageName, long id, int userId) {
        List<BroadcastResponseStats> broadcastResponseStatsList = new ArrayList<>();
        synchronized (this.mLock) {
            SparseArray<UserBroadcastResponseStats> responseStatsForCaller = this.mUserResponseStats.get(callingUid);
            if (responseStatsForCaller == null) {
                return broadcastResponseStatsList;
            }
            UserBroadcastResponseStats responseStatsForUser = responseStatsForCaller.get(userId);
            if (responseStatsForUser == null) {
                return broadcastResponseStatsList;
            }
            responseStatsForUser.populateAllBroadcastResponseStats(broadcastResponseStatsList, packageName, id);
            return broadcastResponseStatsList;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearBroadcastResponseStats(int callingUid, String packageName, long id, int userId) {
        synchronized (this.mLock) {
            SparseArray<UserBroadcastResponseStats> responseStatsForCaller = this.mUserResponseStats.get(callingUid);
            if (responseStatsForCaller == null) {
                return;
            }
            UserBroadcastResponseStats responseStatsForUser = responseStatsForCaller.get(userId);
            if (responseStatsForUser == null) {
                return;
            }
            responseStatsForUser.clearBroadcastResponseStats(packageName, id);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clearBroadcastEvents(int callingUid, int userId) {
        synchronized (this.mLock) {
            UserBroadcastEvents userBroadcastEvents = this.mUserBroadcastEvents.get(userId);
            if (userBroadcastEvents == null) {
                return;
            }
            userBroadcastEvents.clear(callingUid);
        }
    }

    boolean doesPackageHoldExemptedRole(String packageName, UserHandle user) {
        List<String> exemptedRoles = this.mAppStandby.getBroadcastResponseExemptedRoles();
        synchronized (this.mLock) {
            for (int i = exemptedRoles.size() - 1; i >= 0; i--) {
                String roleName = exemptedRoles.get(i);
                List<String> roleHolders = getRoleHoldersLocked(roleName, user);
                if (CollectionUtils.contains(roleHolders, packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    boolean doesPackageHoldExemptedPermission(String packageName, UserHandle user) {
        List<String> exemptedPermissions = this.mAppStandby.getBroadcastResponseExemptedPermissions();
        for (int i = exemptedPermissions.size() - 1; i >= 0; i--) {
            String permissionName = exemptedPermissions.get(i);
            if (PermissionManager.checkPackageNamePermission(permissionName, packageName, user.getIdentifier()) == 0) {
                return true;
            }
        }
        return false;
    }

    private List<String> getRoleHoldersLocked(String roleName, UserHandle user) {
        RoleManager roleManager;
        ArrayMap<String, List<String>> roleHoldersForUser = this.mExemptedRoleHoldersCache.get(user.getIdentifier());
        if (roleHoldersForUser == null) {
            roleHoldersForUser = new ArrayMap<>();
            this.mExemptedRoleHoldersCache.put(user.getIdentifier(), roleHoldersForUser);
        }
        List<String> roleHolders = roleHoldersForUser.get(roleName);
        if (roleHolders == null && (roleManager = this.mRoleManager) != null) {
            List<String> roleHolders2 = roleManager.getRoleHoldersAsUser(roleName, user);
            roleHoldersForUser.put(roleName, roleHolders2);
            return roleHolders2;
        }
        return roleHolders;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRoleHoldersChanged(String roleName, UserHandle user) {
        synchronized (this.mLock) {
            ArrayMap<String, List<String>> roleHoldersForUser = this.mExemptedRoleHoldersCache.get(user.getIdentifier());
            if (roleHoldersForUser == null) {
                return;
            }
            roleHoldersForUser.remove(roleName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            this.mUserBroadcastEvents.remove(userId);
            for (int i = this.mUserResponseStats.size() - 1; i >= 0; i--) {
                this.mUserResponseStats.valueAt(i).remove(userId);
            }
            this.mExemptedRoleHoldersCache.remove(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageRemoved(String packageName, int userId) {
        synchronized (this.mLock) {
            UserBroadcastEvents userBroadcastEvents = this.mUserBroadcastEvents.get(userId);
            if (userBroadcastEvents != null) {
                userBroadcastEvents.onPackageRemoved(packageName);
            }
            for (int i = this.mUserResponseStats.size() - 1; i >= 0; i--) {
                UserBroadcastResponseStats userResponseStats = this.mUserResponseStats.valueAt(i).get(userId);
                if (userResponseStats != null) {
                    userResponseStats.onPackageRemoved(packageName);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUidRemoved(int uid) {
        synchronized (this.mLock) {
            for (int i = this.mUserBroadcastEvents.size() - 1; i >= 0; i--) {
                this.mUserBroadcastEvents.valueAt(i).onUidRemoved(uid);
            }
            this.mUserResponseStats.remove(uid);
        }
    }

    private ArraySet<BroadcastEvent> getBroadcastEventsLocked(String packageName, UserHandle user) {
        UserBroadcastEvents userBroadcastEvents = this.mUserBroadcastEvents.get(user.getIdentifier());
        if (userBroadcastEvents == null) {
            return null;
        }
        return userBroadcastEvents.getBroadcastEvents(packageName);
    }

    private ArraySet<BroadcastEvent> getOrCreateBroadcastEventsLocked(String packageName, UserHandle user) {
        UserBroadcastEvents userBroadcastEvents = this.mUserBroadcastEvents.get(user.getIdentifier());
        if (userBroadcastEvents == null) {
            userBroadcastEvents = new UserBroadcastEvents();
            this.mUserBroadcastEvents.put(user.getIdentifier(), userBroadcastEvents);
        }
        return userBroadcastEvents.getOrCreateBroadcastEvents(packageName);
    }

    private BroadcastResponseStats getBroadcastResponseStats(SparseArray<UserBroadcastResponseStats> responseStatsForUid, BroadcastEvent broadcastEvent) {
        UserBroadcastResponseStats userResponseStats;
        if (responseStatsForUid == null || (userResponseStats = responseStatsForUid.get(broadcastEvent.getTargetUserId())) == null) {
            return null;
        }
        return userResponseStats.getBroadcastResponseStats(broadcastEvent);
    }

    private BroadcastResponseStats getOrCreateBroadcastResponseStats(BroadcastEvent broadcastEvent) {
        int sourceUid = broadcastEvent.getSourceUid();
        SparseArray<UserBroadcastResponseStats> userResponseStatsForUid = this.mUserResponseStats.get(sourceUid);
        if (userResponseStatsForUid == null) {
            userResponseStatsForUid = new SparseArray<>();
            this.mUserResponseStats.put(sourceUid, userResponseStatsForUid);
        }
        UserBroadcastResponseStats userResponseStats = userResponseStatsForUid.get(broadcastEvent.getTargetUserId());
        if (userResponseStats == null) {
            userResponseStats = new UserBroadcastResponseStats();
            userResponseStatsForUid.put(broadcastEvent.getTargetUserId(), userResponseStats);
        }
        return userResponseStats.getOrCreateBroadcastResponseStats(broadcastEvent);
    }

    private static BroadcastEvent getOrCreateBroadcastEvent(ArraySet<BroadcastEvent> broadcastEvents, int sourceUid, String targetPackage, int targetUserId, long idForResponseEvent) {
        BroadcastEvent broadcastEvent = new BroadcastEvent(sourceUid, targetPackage, targetUserId, idForResponseEvent);
        int index = broadcastEvents.indexOf(broadcastEvent);
        if (index >= 0) {
            return broadcastEvents.valueAt(index);
        }
        broadcastEvents.add(broadcastEvent);
        return broadcastEvent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter ipw) {
        ipw.println("Broadcast response stats:");
        ipw.increaseIndent();
        synchronized (this.mLock) {
            dumpBroadcastEventsLocked(ipw);
            ipw.println();
            dumpResponseStatsLocked(ipw);
            ipw.println();
            dumpRoleHoldersLocked(ipw);
            ipw.println();
            this.mLogger.dumpLogs(ipw);
        }
        ipw.decreaseIndent();
    }

    private void dumpBroadcastEventsLocked(IndentingPrintWriter ipw) {
        ipw.println("Broadcast events:");
        ipw.increaseIndent();
        for (int i = 0; i < this.mUserBroadcastEvents.size(); i++) {
            int userId = this.mUserBroadcastEvents.keyAt(i);
            UserBroadcastEvents userBroadcastEvents = this.mUserBroadcastEvents.valueAt(i);
            ipw.println("User " + userId + ":");
            ipw.increaseIndent();
            userBroadcastEvents.dump(ipw);
            ipw.decreaseIndent();
        }
        ipw.decreaseIndent();
    }

    private void dumpResponseStatsLocked(IndentingPrintWriter ipw) {
        ipw.println("Response stats:");
        ipw.increaseIndent();
        for (int i = 0; i < this.mUserResponseStats.size(); i++) {
            int sourceUid = this.mUserResponseStats.keyAt(i);
            SparseArray<UserBroadcastResponseStats> userBroadcastResponseStats = this.mUserResponseStats.valueAt(i);
            ipw.println("Uid " + sourceUid + ":");
            ipw.increaseIndent();
            for (int j = 0; j < userBroadcastResponseStats.size(); j++) {
                int userId = userBroadcastResponseStats.keyAt(j);
                UserBroadcastResponseStats broadcastResponseStats = userBroadcastResponseStats.valueAt(j);
                ipw.println("User " + userId + ":");
                ipw.increaseIndent();
                broadcastResponseStats.dump(ipw);
                ipw.decreaseIndent();
            }
            ipw.decreaseIndent();
        }
        ipw.decreaseIndent();
    }

    private void dumpRoleHoldersLocked(IndentingPrintWriter ipw) {
        ipw.println("Role holders:");
        ipw.increaseIndent();
        for (int userIdx = 0; userIdx < this.mExemptedRoleHoldersCache.size(); userIdx++) {
            int userId = this.mExemptedRoleHoldersCache.keyAt(userIdx);
            ArrayMap<String, List<String>> roleHoldersForUser = this.mExemptedRoleHoldersCache.valueAt(userIdx);
            ipw.println("User " + userId + ":");
            ipw.increaseIndent();
            for (int roleIdx = 0; roleIdx < roleHoldersForUser.size(); roleIdx++) {
                String roleName = roleHoldersForUser.keyAt(roleIdx);
                List<String> holders = roleHoldersForUser.valueAt(roleIdx);
                ipw.print(roleName + ": ");
                for (int holderIdx = 0; holderIdx < holders.size(); holderIdx++) {
                    if (holderIdx > 0) {
                        ipw.print(", ");
                    }
                    ipw.print(holders.get(holderIdx));
                }
                ipw.println();
            }
            ipw.decreaseIndent();
        }
        ipw.decreaseIndent();
    }
}
