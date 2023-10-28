package com.android.server.notification;

import android.app.NotificationHistory;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.util.FunctionalUtils;
import com.android.server.IoThread;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
/* loaded from: classes2.dex */
public class NotificationHistoryManager {
    private static final boolean DEBUG = NotificationManagerService.DBG;
    static final String DIRECTORY_PER_USER = "notification_history";
    private static final String TAG = "NotificationHistory";
    private final Context mContext;
    final SettingsObserver mSettingsObserver;
    private final UserManager mUserManager;
    private final Object mLock = new Object();
    private final SparseArray<NotificationHistoryDatabase> mUserState = new SparseArray<>();
    private final SparseBooleanArray mUserUnlockedStates = new SparseBooleanArray();
    private final SparseArray<List<String>> mUserPendingPackageRemovals = new SparseArray<>();
    private final SparseBooleanArray mHistoryEnabled = new SparseBooleanArray();
    private final SparseBooleanArray mUserPendingHistoryDisables = new SparseBooleanArray();

    public NotificationHistoryManager(Context context, Handler handler) {
        this.mContext = context;
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mSettingsObserver = new SettingsObserver(handler);
    }

    void onDestroy() {
        this.mSettingsObserver.stopObserving();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBootPhaseAppsCanStart() {
        this.mSettingsObserver.observe();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserUnlocked(int userId) {
        synchronized (this.mLock) {
            this.mUserUnlockedStates.put(userId, true);
            NotificationHistoryDatabase userHistory = getUserHistoryAndInitializeIfNeededLocked(userId);
            if (userHistory == null) {
                Slog.i(TAG, "Attempted to unlock gone/disabled user " + userId);
                return;
            }
            List<String> pendingPackageRemovals = this.mUserPendingPackageRemovals.get(userId);
            if (pendingPackageRemovals != null) {
                for (int i = 0; i < pendingPackageRemovals.size(); i++) {
                    userHistory.onPackageRemoved(pendingPackageRemovals.get(i));
                }
                this.mUserPendingPackageRemovals.put(userId, null);
            }
            if (this.mUserPendingHistoryDisables.get(userId)) {
                disableHistory(userHistory, userId);
            }
        }
    }

    public void onUserStopped(int userId) {
        synchronized (this.mLock) {
            this.mUserUnlockedStates.put(userId, false);
            this.mUserState.put(userId, null);
        }
    }

    public void onUserRemoved(int userId) {
        synchronized (this.mLock) {
            this.mUserPendingPackageRemovals.put(userId, null);
            this.mHistoryEnabled.put(userId, false);
            this.mUserPendingHistoryDisables.put(userId, false);
            onUserStopped(userId);
        }
    }

    public void onPackageRemoved(int userId, String packageName) {
        synchronized (this.mLock) {
            if (!this.mUserUnlockedStates.get(userId, false)) {
                if (this.mHistoryEnabled.get(userId, false)) {
                    List<String> userPendingRemovals = this.mUserPendingPackageRemovals.get(userId, new ArrayList());
                    userPendingRemovals.add(packageName);
                    this.mUserPendingPackageRemovals.put(userId, userPendingRemovals);
                }
                return;
            }
            NotificationHistoryDatabase userHistory = this.mUserState.get(userId);
            if (userHistory == null) {
                return;
            }
            userHistory.onPackageRemoved(packageName);
        }
    }

    public void deleteNotificationHistoryItem(String pkg, int uid, long postedTime) {
        synchronized (this.mLock) {
            int userId = UserHandle.getUserId(uid);
            NotificationHistoryDatabase userHistory = getUserHistoryAndInitializeIfNeededLocked(userId);
            if (userHistory == null) {
                Slog.w(TAG, "Attempted to remove notif for locked/gone/disabled user " + userId);
            } else {
                userHistory.deleteNotificationHistoryItem(pkg, postedTime);
            }
        }
    }

    public void deleteConversations(String pkg, int uid, Set<String> conversationIds) {
        synchronized (this.mLock) {
            int userId = UserHandle.getUserId(uid);
            NotificationHistoryDatabase userHistory = getUserHistoryAndInitializeIfNeededLocked(userId);
            if (userHistory == null) {
                Slog.w(TAG, "Attempted to remove conversation for locked/gone/disabled user " + userId);
            } else {
                userHistory.deleteConversations(pkg, conversationIds);
            }
        }
    }

    public void deleteNotificationChannel(String pkg, int uid, String channelId) {
        synchronized (this.mLock) {
            int userId = UserHandle.getUserId(uid);
            NotificationHistoryDatabase userHistory = getUserHistoryAndInitializeIfNeededLocked(userId);
            if (userHistory == null) {
                Slog.w(TAG, "Attempted to remove channel for locked/gone/disabled user " + userId);
            } else {
                userHistory.deleteNotificationChannel(pkg, channelId);
            }
        }
    }

    public void triggerWriteToDisk() {
        NotificationHistoryDatabase userHistory;
        synchronized (this.mLock) {
            int userCount = this.mUserState.size();
            for (int i = 0; i < userCount; i++) {
                int userId = this.mUserState.keyAt(i);
                if (this.mUserUnlockedStates.get(userId) && (userHistory = this.mUserState.get(userId)) != null) {
                    userHistory.forceWriteToDisk();
                }
            }
        }
    }

    public void addNotification(final NotificationHistory.HistoricalNotification notification) {
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.notification.NotificationHistoryManager$$ExternalSyntheticLambda0
            public final void runOrThrow() {
                NotificationHistoryManager.this.m5012x5f84410f(notification);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addNotification$0$com-android-server-notification-NotificationHistoryManager  reason: not valid java name */
    public /* synthetic */ void m5012x5f84410f(NotificationHistory.HistoricalNotification notification) throws Exception {
        synchronized (this.mLock) {
            NotificationHistoryDatabase userHistory = getUserHistoryAndInitializeIfNeededLocked(notification.getUserId());
            if (userHistory == null) {
                Slog.w(TAG, "Attempted to add notif for locked/gone/disabled user " + notification.getUserId());
            } else {
                userHistory.addNotification(notification);
            }
        }
    }

    public NotificationHistory readNotificationHistory(int[] userIds) {
        synchronized (this.mLock) {
            NotificationHistory mergedHistory = new NotificationHistory();
            if (userIds == null) {
                return mergedHistory;
            }
            for (int userId : userIds) {
                NotificationHistoryDatabase userHistory = getUserHistoryAndInitializeIfNeededLocked(userId);
                if (userHistory == null) {
                    Slog.i(TAG, "Attempted to read history for locked/gone/disabled user " + userId);
                } else {
                    mergedHistory.addNotificationsToWrite(userHistory.readNotificationHistory());
                }
            }
            return mergedHistory;
        }
    }

    public NotificationHistory readFilteredNotificationHistory(int userId, String packageName, String channelId, int maxNotifications) {
        synchronized (this.mLock) {
            NotificationHistoryDatabase userHistory = getUserHistoryAndInitializeIfNeededLocked(userId);
            if (userHistory == null) {
                Slog.i(TAG, "Attempted to read history for locked/gone/disabled user " + userId);
                return new NotificationHistory();
            }
            return userHistory.readNotificationHistory(packageName, channelId, maxNotifications);
        }
    }

    boolean isHistoryEnabled(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mHistoryEnabled.get(userId);
        }
        return z;
    }

    void onHistoryEnabledChanged(int userId, boolean historyEnabled) {
        synchronized (this.mLock) {
            if (historyEnabled) {
                this.mHistoryEnabled.put(userId, historyEnabled);
            }
            NotificationHistoryDatabase userHistory = getUserHistoryAndInitializeIfNeededLocked(userId);
            if (userHistory != null) {
                if (!historyEnabled) {
                    disableHistory(userHistory, userId);
                }
            } else {
                this.mUserPendingHistoryDisables.put(userId, !historyEnabled);
            }
        }
    }

    private void disableHistory(NotificationHistoryDatabase userHistory, int userId) {
        userHistory.disableHistory();
        userHistory.unregisterFileCleanupReceiver();
        this.mUserPendingHistoryDisables.put(userId, false);
        this.mHistoryEnabled.put(userId, false);
        this.mUserState.put(userId, null);
    }

    private NotificationHistoryDatabase getUserHistoryAndInitializeIfNeededLocked(int userId) {
        if (!this.mHistoryEnabled.get(userId)) {
            if (DEBUG) {
                Slog.i(TAG, "History disabled for user " + userId);
            }
            this.mUserState.put(userId, null);
            return null;
        }
        NotificationHistoryDatabase userHistory = this.mUserState.get(userId);
        if (userHistory == null) {
            File historyDir = new File(Environment.getDataSystemCeDirectory(userId), DIRECTORY_PER_USER);
            userHistory = NotificationHistoryDatabaseFactory.create(this.mContext, IoThread.getHandler(), historyDir);
            if (this.mUserUnlockedStates.get(userId)) {
                try {
                    userHistory.init();
                    this.mUserState.put(userId, userHistory);
                } catch (Exception e) {
                    if (this.mUserManager.isUserUnlocked(userId)) {
                        throw e;
                    }
                    Slog.w(TAG, "Attempted to initialize service for stopped or removed user " + userId);
                    return null;
                }
            } else {
                Slog.w(TAG, "Attempted to initialize service for stopped or removed user " + userId);
                return null;
            }
        }
        return userHistory;
    }

    boolean isUserUnlocked(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mUserUnlockedStates.get(userId);
        }
        return z;
    }

    boolean doesHistoryExistForUser(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mUserState.get(userId) != null;
        }
        return z;
    }

    void replaceNotificationHistoryDatabase(int userId, NotificationHistoryDatabase replacement) {
        synchronized (this.mLock) {
            if (this.mUserState.get(userId) != null) {
                this.mUserState.put(userId, replacement);
            }
        }
    }

    List<String> getPendingPackageRemovalsForUser(int userId) {
        List<String> list;
        synchronized (this.mLock) {
            list = this.mUserPendingPackageRemovals.get(userId);
        }
        return list;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public final class SettingsObserver extends ContentObserver {
        private final Uri NOTIFICATION_HISTORY_URI;

        SettingsObserver(Handler handler) {
            super(handler);
            this.NOTIFICATION_HISTORY_URI = Settings.Secure.getUriFor("notification_history_enabled");
        }

        void observe() {
            ContentResolver resolver = NotificationHistoryManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.NOTIFICATION_HISTORY_URI, false, this, -1);
            synchronized (NotificationHistoryManager.this.mLock) {
                for (UserInfo userInfo : NotificationHistoryManager.this.mUserManager.getUsers()) {
                    if (!userInfo.isProfile()) {
                        update(null, userInfo.id);
                    }
                }
            }
        }

        void stopObserving() {
            ContentResolver resolver = NotificationHistoryManager.this.mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            update(uri, userId);
        }

        public void update(Uri uri, int userId) {
            ContentResolver resolver = NotificationHistoryManager.this.mContext.getContentResolver();
            if (uri == null || this.NOTIFICATION_HISTORY_URI.equals(uri)) {
                boolean historyEnabled = Settings.Secure.getIntForUser(resolver, "notification_history_enabled", 0, userId) != 0;
                int[] profiles = NotificationHistoryManager.this.mUserManager.getProfileIds(userId, true);
                for (int profileId : profiles) {
                    NotificationHistoryManager.this.onHistoryEnabledChanged(profileId, historyEnabled);
                }
            }
        }
    }
}
