package com.android.server.notification;

import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
/* loaded from: classes2.dex */
public class GroupHelper {
    protected static final String AUTOGROUP_KEY = "ranker_group";
    private final int mAutoGroupAtCount;
    private final Callback mCallback;
    final ArrayMap<String, ArraySet<String>> mOngoingGroupCount = new ArrayMap<>();
    Map<Integer, Map<String, LinkedHashSet<String>>> mUngroupedNotifications = new HashMap();
    private static final String TAG = "GroupHelper";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes2.dex */
    public interface Callback {
        void addAutoGroup(String str);

        void addAutoGroupSummary(int i, String str, String str2, boolean z);

        void removeAutoGroup(String str);

        void removeAutoGroupSummary(int i, String str);

        void updateAutogroupSummary(int i, String str, boolean z);
    }

    public GroupHelper(int autoGroupAtCount, Callback callback) {
        this.mAutoGroupAtCount = autoGroupAtCount;
        this.mCallback = callback;
    }

    private String generatePackageKey(int userId, String pkg) {
        return userId + "|" + pkg;
    }

    protected int getOngoingGroupCount(int userId, String pkg) {
        String key = generatePackageKey(userId, pkg);
        return this.mOngoingGroupCount.getOrDefault(key, new ArraySet<>(0)).size();
    }

    private void updateOngoingGroupCount(StatusBarNotification sbn, boolean add) {
        if (sbn.getNotification().isGroupSummary()) {
            return;
        }
        String key = generatePackageKey(sbn.getUserId(), sbn.getPackageName());
        ArraySet<String> notifications = this.mOngoingGroupCount.getOrDefault(key, new ArraySet<>(0));
        if (add) {
            notifications.add(sbn.getKey());
            this.mOngoingGroupCount.put(key, notifications);
        } else {
            notifications.remove(sbn.getKey());
        }
        boolean needsOngoingFlag = notifications.size() > 0;
        this.mCallback.updateAutogroupSummary(sbn.getUserId(), sbn.getPackageName(), needsOngoingFlag);
    }

    public void onNotificationUpdated(StatusBarNotification childSbn) {
        updateOngoingGroupCount(childSbn, childSbn.isOngoing() && !childSbn.isAppGroup());
    }

    /* JADX WARN: Removed duplicated region for block: B:27:0x008a A[Catch: all -> 0x00b1, TryCatch #1 {Exception -> 0x00bc, blocks: (B:2:0x0000, B:4:0x0008, B:8:0x0011, B:10:0x001f, B:11:0x0021, B:33:0x0096, B:35:0x009c, B:40:0x00b4, B:12:0x0022, B:14:0x0034, B:15:0x003a, B:17:0x0054, B:18:0x005a, B:20:0x0074, B:25:0x0084, B:27:0x008a, B:32:0x0095, B:31:0x0092), top: B:45:0x0000 }] */
    /* JADX WARN: Removed duplicated region for block: B:28:0x008d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void onNotificationPosted(StatusBarNotification sbn, boolean autogroupSummaryExists) {
        boolean isGoogleNotification;
        try {
            updateOngoingGroupCount(sbn, sbn.isOngoing() && !sbn.isAppGroup());
            List<String> notificationsToGroup = new ArrayList<>();
            if (!sbn.isAppGroup()) {
                synchronized (this.mUngroupedNotifications) {
                    Map<String, LinkedHashSet<String>> ungroupedNotificationsByUser = this.mUngroupedNotifications.get(Integer.valueOf(sbn.getUserId()));
                    if (ungroupedNotificationsByUser == null) {
                        ungroupedNotificationsByUser = new HashMap();
                    }
                    this.mUngroupedNotifications.put(Integer.valueOf(sbn.getUserId()), ungroupedNotificationsByUser);
                    LinkedHashSet<String> notificationsForPackage = ungroupedNotificationsByUser.get(sbn.getPackageName());
                    if (notificationsForPackage == null) {
                        notificationsForPackage = new LinkedHashSet<>();
                    }
                    notificationsForPackage.add(sbn.getKey());
                    ungroupedNotificationsByUser.put(sbn.getPackageName(), notificationsForPackage);
                    if (!sbn.getPackageName().contains("google") && !sbn.getPackageName().contains("cts")) {
                        isGoogleNotification = false;
                        if (notificationsForPackage.size() < (!isGoogleNotification ? this.mAutoGroupAtCount : 2) || autogroupSummaryExists) {
                            notificationsToGroup.addAll(notificationsForPackage);
                        }
                    }
                    isGoogleNotification = true;
                    if (notificationsForPackage.size() < (!isGoogleNotification ? this.mAutoGroupAtCount : 2)) {
                    }
                    notificationsToGroup.addAll(notificationsForPackage);
                }
                if (notificationsToGroup.size() > 0) {
                    adjustAutogroupingSummary(sbn.getUserId(), sbn.getPackageName(), notificationsToGroup.get(0), true);
                    adjustNotificationBundling(notificationsToGroup, true);
                    return;
                }
                return;
            }
            maybeUngroup(sbn, false, sbn.getUserId());
        } catch (Exception e) {
            Slog.e(TAG, "Failure processing new notification", e);
        }
    }

    public void onNotificationRemoved(StatusBarNotification sbn) {
        try {
            updateOngoingGroupCount(sbn, false);
            maybeUngroup(sbn, true, sbn.getUserId());
        } catch (Exception e) {
            Slog.e(TAG, "Error processing canceled notification", e);
        }
    }

    private void maybeUngroup(StatusBarNotification sbn, boolean notificationGone, int userId) {
        List<String> notificationsToUnAutogroup = new ArrayList<>();
        boolean removeSummary = false;
        synchronized (this.mUngroupedNotifications) {
            Map<String, LinkedHashSet<String>> ungroupedNotificationsByUser = this.mUngroupedNotifications.get(Integer.valueOf(sbn.getUserId()));
            if (ungroupedNotificationsByUser != null && ungroupedNotificationsByUser.size() != 0) {
                LinkedHashSet<String> notificationsForPackage = ungroupedNotificationsByUser.get(sbn.getPackageName());
                if (notificationsForPackage != null && notificationsForPackage.size() != 0) {
                    if (notificationsForPackage.remove(sbn.getKey()) && !notificationGone) {
                        notificationsToUnAutogroup.add(sbn.getKey());
                    }
                    if (notificationsForPackage.size() == 0) {
                        ungroupedNotificationsByUser.remove(sbn.getPackageName());
                        removeSummary = true;
                    }
                    if (removeSummary) {
                        adjustAutogroupingSummary(userId, sbn.getPackageName(), null, false);
                    }
                    if (notificationsToUnAutogroup.size() > 0) {
                        adjustNotificationBundling(notificationsToUnAutogroup, false);
                    }
                }
            }
        }
    }

    private void adjustAutogroupingSummary(int userId, String packageName, String triggeringKey, boolean summaryNeeded) {
        if (summaryNeeded) {
            this.mCallback.addAutoGroupSummary(userId, packageName, triggeringKey, getOngoingGroupCount(userId, packageName) > 0);
        } else {
            this.mCallback.removeAutoGroupSummary(userId, packageName);
        }
    }

    private void adjustNotificationBundling(List<String> keys, boolean group) {
        for (String key : keys) {
            if (DEBUG) {
                Log.i(TAG, "Sending grouping adjustment for: " + key + " group? " + group);
            }
            if (group) {
                this.mCallback.addAutoGroup(key);
            } else {
                this.mCallback.removeAutoGroup(key);
            }
        }
    }
}
