package com.android.server.people.data;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.Person;
import android.app.people.ConversationChannel;
import android.app.people.ConversationStatus;
import android.app.prediction.AppTarget;
import android.app.prediction.AppTargetEvent;
import android.app.usage.UsageEvents;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.pm.ShortcutServiceInternal;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.telephony.SmsApplication;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.notification.NotificationManagerInternal;
import com.android.server.notification.ShortcutHelper;
import com.android.server.people.PeopleService;
import com.android.server.people.data.ConversationInfo;
import com.android.server.people.data.DataManager;
import com.android.server.people.data.UsageStatsQueryHelper;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;
/* loaded from: classes2.dex */
public class DataManager {
    private static final boolean DEBUG = false;
    static final int MAX_CACHED_RECENT_SHORTCUTS = 30;
    private static final long QUERY_EVENTS_MAX_AGE_MS = 300000;
    private static final long RECENT_NOTIFICATIONS_MAX_AGE_MS = 864000000;
    private static final String TAG = "DataManager";
    private static final long USAGE_STATS_QUERY_INTERVAL_SEC = 120;
    private final SparseArray<BroadcastReceiver> mBroadcastReceivers;
    private ContentObserver mCallLogContentObserver;
    private final SparseArray<ContentObserver> mContactsContentObservers;
    private final Context mContext;
    private final List<PeopleService.ConversationsListener> mConversationsListeners;
    private final Handler mHandler;
    private final Injector mInjector;
    private final Object mLock;
    private ContentObserver mMmsSmsContentObserver;
    private final SparseArray<NotificationListener> mNotificationListeners;
    private NotificationManagerInternal mNotificationManagerInternal;
    private PackageManagerInternal mPackageManagerInternal;
    private final SparseArray<PackageMonitor> mPackageMonitors;
    private final ScheduledExecutorService mScheduledExecutor;
    private ShortcutServiceInternal mShortcutServiceInternal;
    private ConversationStatusExpirationBroadcastReceiver mStatusExpReceiver;
    private final SparseArray<ScheduledFuture<?>> mUsageStatsQueryFutures;
    private final SparseArray<UserData> mUserDataArray;
    private UserManager mUserManager;

    public DataManager(Context context) {
        this(context, new Injector(), BackgroundThread.get().getLooper());
    }

    DataManager(Context context, Injector injector, Looper looper) {
        this.mLock = new Object();
        this.mUserDataArray = new SparseArray<>();
        this.mBroadcastReceivers = new SparseArray<>();
        this.mContactsContentObservers = new SparseArray<>();
        this.mUsageStatsQueryFutures = new SparseArray<>();
        this.mNotificationListeners = new SparseArray<>();
        this.mPackageMonitors = new SparseArray<>();
        this.mConversationsListeners = new ArrayList(1);
        this.mContext = context;
        this.mInjector = injector;
        this.mScheduledExecutor = injector.createScheduledExecutor();
        this.mHandler = new Handler(looper);
    }

    public void initialize() {
        this.mShortcutServiceInternal = (ShortcutServiceInternal) LocalServices.getService(ShortcutServiceInternal.class);
        this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mNotificationManagerInternal = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        this.mShortcutServiceInternal.addShortcutChangeCallback(new ShortcutServiceCallback());
        ConversationStatusExpirationBroadcastReceiver conversationStatusExpirationBroadcastReceiver = new ConversationStatusExpirationBroadcastReceiver();
        this.mStatusExpReceiver = conversationStatusExpirationBroadcastReceiver;
        this.mContext.registerReceiver(conversationStatusExpirationBroadcastReceiver, ConversationStatusExpirationBroadcastReceiver.getFilter(), 4);
        IntentFilter shutdownIntentFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
        BroadcastReceiver shutdownBroadcastReceiver = new ShutdownBroadcastReceiver();
        this.mContext.registerReceiver(shutdownBroadcastReceiver, shutdownIntentFilter);
    }

    public void onUserUnlocked(final int userId) {
        synchronized (this.mLock) {
            UserData userData = this.mUserDataArray.get(userId);
            if (userData == null) {
                userData = new UserData(userId, this.mScheduledExecutor);
                this.mUserDataArray.put(userId, userData);
            }
            userData.setUserUnlocked();
        }
        this.mScheduledExecutor.execute(new Runnable() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                DataManager.this.m5330xdf5b684c(userId);
            }
        });
    }

    public void onUserStopping(final int userId) {
        synchronized (this.mLock) {
            UserData userData = this.mUserDataArray.get(userId);
            if (userData != null) {
                userData.setUserStopped();
            }
        }
        this.mScheduledExecutor.execute(new Runnable() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                DataManager.this.m5329x2b4a9bba(userId);
            }
        });
    }

    void forPackagesInProfile(int callingUserId, Consumer<PackageData> consumer) {
        List<UserInfo> users = this.mUserManager.getEnabledProfiles(callingUserId);
        for (UserInfo userInfo : users) {
            UserData userData = getUnlockedUserData(userInfo.id);
            if (userData != null) {
                userData.forAllPackages(consumer);
            }
        }
    }

    public PackageData getPackage(String packageName, int userId) {
        UserData userData = getUnlockedUserData(userId);
        if (userData != null) {
            return userData.getPackageData(packageName);
        }
        return null;
    }

    public ShortcutInfo getShortcut(String packageName, int userId, String shortcutId) {
        List<ShortcutInfo> shortcuts = getShortcuts(packageName, userId, Collections.singletonList(shortcutId));
        if (shortcuts != null && !shortcuts.isEmpty()) {
            return shortcuts.get(0);
        }
        return null;
    }

    public List<ShortcutManager.ShareShortcutInfo> getShareShortcuts(IntentFilter intentFilter, int callingUserId) {
        return this.mShortcutServiceInternal.getShareTargets(this.mContext.getPackageName(), intentFilter, callingUserId);
    }

    public ConversationChannel getConversation(String packageName, int userId, String shortcutId) {
        PackageData packageData;
        UserData userData = getUnlockedUserData(userId);
        if (userData != null && (packageData = userData.getPackageData(packageName)) != null) {
            ConversationInfo conversationInfo = packageData.getConversationInfo(shortcutId);
            return getConversationChannel(packageName, userId, shortcutId, conversationInfo);
        }
        return null;
    }

    ConversationInfo getConversationInfo(String packageName, int userId, String shortcutId) {
        PackageData packageData;
        UserData userData = getUnlockedUserData(userId);
        if (userData != null && (packageData = userData.getPackageData(packageName)) != null) {
            return packageData.getConversationInfo(shortcutId);
        }
        return null;
    }

    private ConversationChannel getConversationChannel(String packageName, int userId, String shortcutId, ConversationInfo conversationInfo) {
        ShortcutInfo shortcutInfo = getShortcut(packageName, userId, shortcutId);
        return getConversationChannel(shortcutInfo, conversationInfo);
    }

    private ConversationChannel getConversationChannel(ShortcutInfo shortcutInfo, ConversationInfo conversationInfo) {
        NotificationChannelGroup parentChannelGroup;
        if (conversationInfo == null || conversationInfo.isDemoted()) {
            return null;
        }
        if (shortcutInfo == null) {
            Slog.e(TAG, " Shortcut no longer found");
            return null;
        }
        String packageName = shortcutInfo.getPackage();
        String shortcutId = shortcutInfo.getId();
        int userId = shortcutInfo.getUserId();
        int uid = this.mPackageManagerInternal.getPackageUid(packageName, 0L, userId);
        NotificationChannel parentChannel = this.mNotificationManagerInternal.getNotificationChannel(packageName, uid, conversationInfo.getNotificationChannelId());
        if (parentChannel == null) {
            parentChannelGroup = null;
        } else {
            NotificationChannelGroup parentChannelGroup2 = this.mNotificationManagerInternal.getNotificationChannelGroup(packageName, uid, parentChannel.getId());
            parentChannelGroup = parentChannelGroup2;
        }
        return new ConversationChannel(shortcutInfo, uid, parentChannel, parentChannelGroup, conversationInfo.getLastEventTimestamp(), hasActiveNotifications(packageName, userId, shortcutId), false, getStatuses(conversationInfo));
    }

    public List<ConversationChannel> getRecentConversations(int callingUserId) {
        final List<ConversationChannel> conversationChannels = new ArrayList<>();
        forPackagesInProfile(callingUserId, new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DataManager.this.m5327x2c8fff77(conversationChannels, (PackageData) obj);
            }
        });
        return conversationChannels;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getRecentConversations$3$com-android-server-people-data-DataManager  reason: not valid java name */
    public /* synthetic */ void m5327x2c8fff77(final List conversationChannels, final PackageData packageData) {
        packageData.forAllConversations(new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda10
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DataManager.this.m5326x69a39618(packageData, conversationChannels, (ConversationInfo) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getRecentConversations$2$com-android-server-people-data-DataManager  reason: not valid java name */
    public /* synthetic */ void m5326x69a39618(PackageData packageData, List conversationChannels, ConversationInfo conversationInfo) {
        if (!isCachedRecentConversation(conversationInfo)) {
            return;
        }
        String shortcutId = conversationInfo.getShortcutId();
        ConversationChannel channel = getConversationChannel(packageData.getPackageName(), packageData.getUserId(), shortcutId, conversationInfo);
        if (channel == null || channel.getNotificationChannel() == null) {
            return;
        }
        conversationChannels.add(channel);
    }

    public void removeRecentConversation(String packageName, int userId, String shortcutId, int callingUserId) {
        if (!hasActiveNotifications(packageName, userId, shortcutId)) {
            this.mShortcutServiceInternal.uncacheShortcuts(callingUserId, this.mContext.getPackageName(), packageName, Collections.singletonList(shortcutId), userId, 16384);
        }
    }

    public void removeAllRecentConversations(int callingUserId) {
        pruneOldRecentConversations(callingUserId, JobStatus.NO_LATEST_RUNTIME);
    }

    public void pruneOldRecentConversations(final int callingUserId, final long currentTimeMs) {
        forPackagesInProfile(callingUserId, new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda8
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DataManager.this.m5335x60101b96(currentTimeMs, callingUserId, (PackageData) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pruneOldRecentConversations$5$com-android-server-people-data-DataManager  reason: not valid java name */
    public /* synthetic */ void m5335x60101b96(final long currentTimeMs, int callingUserId, PackageData packageData) {
        final String packageName = packageData.getPackageName();
        final int userId = packageData.getUserId();
        final List<String> idsToUncache = new ArrayList<>();
        packageData.forAllConversations(new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda14
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DataManager.this.m5334x9d23b237(currentTimeMs, packageName, userId, idsToUncache, (ConversationInfo) obj);
            }
        });
        if (!idsToUncache.isEmpty()) {
            this.mShortcutServiceInternal.uncacheShortcuts(callingUserId, this.mContext.getPackageName(), packageName, idsToUncache, userId, 16384);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pruneOldRecentConversations$4$com-android-server-people-data-DataManager  reason: not valid java name */
    public /* synthetic */ void m5334x9d23b237(long currentTimeMs, String packageName, int userId, List idsToUncache, ConversationInfo conversationInfo) {
        String shortcutId = conversationInfo.getShortcutId();
        if (isCachedRecentConversation(conversationInfo) && currentTimeMs - conversationInfo.getLastEventTimestamp() > RECENT_NOTIFICATIONS_MAX_AGE_MS && !hasActiveNotifications(packageName, userId, shortcutId)) {
            idsToUncache.add(shortcutId);
        }
    }

    public void pruneExpiredConversationStatuses(int callingUserId, final long currentTimeMs) {
        forPackagesInProfile(callingUserId, new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda7
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DataManager.this.m5333x49a8f394(currentTimeMs, (PackageData) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pruneExpiredConversationStatuses$7$com-android-server-people-data-DataManager  reason: not valid java name */
    public /* synthetic */ void m5333x49a8f394(final long currentTimeMs, final PackageData packageData) {
        final ConversationStore cs = packageData.getConversationStore();
        packageData.forAllConversations(new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda9
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DataManager.this.m5332x86bc8a35(currentTimeMs, cs, packageData, (ConversationInfo) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pruneExpiredConversationStatuses$6$com-android-server-people-data-DataManager  reason: not valid java name */
    public /* synthetic */ void m5332x86bc8a35(long currentTimeMs, ConversationStore cs, PackageData packageData, ConversationInfo conversationInfo) {
        ConversationInfo.Builder builder = new ConversationInfo.Builder(conversationInfo);
        List<ConversationStatus> newStatuses = new ArrayList<>();
        for (ConversationStatus status : conversationInfo.getStatuses()) {
            if (status.getEndTimeMillis() < 0 || currentTimeMs < status.getEndTimeMillis()) {
                newStatuses.add(status);
            }
        }
        builder.setStatuses(newStatuses);
        updateConversationStoreThenNotifyListeners(cs, builder.build(), packageData.getPackageName(), packageData.getUserId());
    }

    public boolean isConversation(String packageName, int userId, String shortcutId) {
        ConversationChannel channel = getConversation(packageName, userId, shortcutId);
        return (channel == null || channel.getShortcutInfo() == null || TextUtils.isEmpty(channel.getShortcutInfo().getLabel())) ? false : true;
    }

    public long getLastInteraction(String packageName, int userId, String shortcutId) {
        ConversationInfo conversationInfo;
        PackageData packageData = getPackage(packageName, userId);
        if (packageData != null && (conversationInfo = packageData.getConversationInfo(shortcutId)) != null) {
            return conversationInfo.getLastEventTimestamp();
        }
        return 0L;
    }

    public void addOrUpdateStatus(String packageName, int userId, String conversationId, ConversationStatus status) {
        ConversationStore cs = getConversationStoreOrThrow(packageName, userId);
        ConversationInfo convToModify = getConversationInfoOrThrow(cs, conversationId);
        ConversationInfo.Builder builder = new ConversationInfo.Builder(convToModify);
        builder.addOrUpdateStatus(status);
        updateConversationStoreThenNotifyListeners(cs, builder.build(), packageName, userId);
        if (status.getEndTimeMillis() >= 0) {
            this.mStatusExpReceiver.scheduleExpiration(this.mContext, userId, packageName, conversationId, status);
        }
    }

    public void clearStatus(String packageName, int userId, String conversationId, String statusId) {
        ConversationStore cs = getConversationStoreOrThrow(packageName, userId);
        ConversationInfo convToModify = getConversationInfoOrThrow(cs, conversationId);
        ConversationInfo.Builder builder = new ConversationInfo.Builder(convToModify);
        builder.clearStatus(statusId);
        updateConversationStoreThenNotifyListeners(cs, builder.build(), packageName, userId);
    }

    public void clearStatuses(String packageName, int userId, String conversationId) {
        ConversationStore cs = getConversationStoreOrThrow(packageName, userId);
        ConversationInfo convToModify = getConversationInfoOrThrow(cs, conversationId);
        ConversationInfo.Builder builder = new ConversationInfo.Builder(convToModify);
        builder.setStatuses(null);
        updateConversationStoreThenNotifyListeners(cs, builder.build(), packageName, userId);
    }

    public List<ConversationStatus> getStatuses(String packageName, int userId, String conversationId) {
        ConversationStore cs = getConversationStoreOrThrow(packageName, userId);
        ConversationInfo conversationInfo = getConversationInfoOrThrow(cs, conversationId);
        return getStatuses(conversationInfo);
    }

    private List<ConversationStatus> getStatuses(ConversationInfo conversationInfo) {
        Collection<? extends ConversationStatus> statuses = conversationInfo.getStatuses();
        if (statuses != null) {
            ArrayList<ConversationStatus> list = new ArrayList<>(statuses.size());
            list.addAll(statuses);
            return list;
        }
        return new ArrayList();
    }

    private ConversationStore getConversationStoreOrThrow(String packageName, int userId) {
        PackageData packageData = getPackage(packageName, userId);
        if (packageData == null) {
            throw new IllegalArgumentException("No settings exist for package " + packageName);
        }
        ConversationStore cs = packageData.getConversationStore();
        if (cs == null) {
            throw new IllegalArgumentException("No conversations exist for package " + packageName);
        }
        return cs;
    }

    private ConversationInfo getConversationInfoOrThrow(ConversationStore cs, String conversationId) {
        ConversationInfo ci = cs.getConversation(conversationId);
        if (ci == null) {
            throw new IllegalArgumentException("Conversation does not exist");
        }
        return ci;
    }

    public void reportShareTargetEvent(AppTargetEvent event, IntentFilter intentFilter) {
        UserData userData;
        EventHistoryImpl eventHistory;
        AppTarget appTarget = event.getTarget();
        if (appTarget == null || event.getAction() != 1 || (userData = getUnlockedUserData(appTarget.getUser().getIdentifier())) == null) {
            return;
        }
        PackageData packageData = userData.getOrCreatePackageData(appTarget.getPackageName());
        int eventType = mimeTypeToShareEventType(intentFilter.getDataType(0));
        if ("direct_share".equals(event.getLaunchLocation())) {
            if (appTarget.getShortcutInfo() == null) {
                return;
            }
            String shortcutId = appTarget.getShortcutInfo().getId();
            if ("chooser_target".equals(shortcutId)) {
                return;
            }
            if (packageData.getConversationStore().getConversation(shortcutId) == null) {
                addOrUpdateConversationInfo(appTarget.getShortcutInfo());
            }
            eventHistory = packageData.getEventStore().getOrCreateEventHistory(0, shortcutId);
        } else {
            eventHistory = packageData.getEventStore().getOrCreateEventHistory(4, appTarget.getClassName());
        }
        eventHistory.addEvent(new Event(System.currentTimeMillis(), eventType));
    }

    public List<UsageEvents.Event> queryAppMovingToForegroundEvents(int callingUserId, long startTime, long endTime) {
        return UsageStatsQueryHelper.queryAppMovingToForegroundEvents(callingUserId, startTime, endTime);
    }

    public Map<String, AppUsageStatsData> queryAppUsageStats(int callingUserId, long startTime, long endTime, Set<String> packageNameFilter) {
        return UsageStatsQueryHelper.queryAppUsageStats(callingUserId, startTime, endTime, packageNameFilter);
    }

    public void pruneDataForUser(final int userId, final CancellationSignal signal) {
        UserData userData = getUnlockedUserData(userId);
        if (userData == null || signal.isCanceled()) {
            return;
        }
        pruneUninstalledPackageData(userData);
        userData.forAllPackages(new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DataManager.this.m5331x602a8d91(signal, userId, (PackageData) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pruneDataForUser$8$com-android-server-people-data-DataManager  reason: not valid java name */
    public /* synthetic */ void m5331x602a8d91(CancellationSignal signal, int userId, PackageData packageData) {
        if (signal.isCanceled()) {
            return;
        }
        packageData.getEventStore().pruneOldEvents();
        if (!packageData.isDefaultDialer()) {
            packageData.getEventStore().deleteEventHistories(2);
        }
        if (!packageData.isDefaultSmsApp()) {
            packageData.getEventStore().deleteEventHistories(3);
        }
        packageData.pruneOrphanEvents();
        pruneExpiredConversationStatuses(userId, System.currentTimeMillis());
        pruneOldRecentConversations(userId, System.currentTimeMillis());
        cleanupCachedShortcuts(userId, 30);
    }

    public byte[] getBackupPayload(int userId) {
        UserData userData = getUnlockedUserData(userId);
        if (userData == null) {
            return null;
        }
        return userData.getBackupPayload();
    }

    public void restore(int userId, byte[] payload) {
        UserData userData = getUnlockedUserData(userId);
        if (userData == null) {
            return;
        }
        userData.restore(payload);
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: setupUser */
    public void m5330xdf5b684c(int userId) {
        synchronized (this.mLock) {
            UserData userData = getUnlockedUserData(userId);
            if (userData == null) {
                return;
            }
            userData.loadUserData();
            updateDefaultDialer(userData);
            updateDefaultSmsApp(userData);
            ScheduledFuture<?> scheduledFuture = this.mScheduledExecutor.scheduleAtFixedRate(new UsageStatsQueryRunnable(userId), 1L, 120L, TimeUnit.SECONDS);
            this.mUsageStatsQueryFutures.put(userId, scheduledFuture);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.telecom.action.DEFAULT_DIALER_CHANGED");
            intentFilter.addAction("android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED_INTERNAL");
            if (this.mBroadcastReceivers.get(userId) == null) {
                BroadcastReceiver broadcastReceiver = new PerUserBroadcastReceiver(userId);
                this.mBroadcastReceivers.put(userId, broadcastReceiver);
                this.mContext.registerReceiverAsUser(broadcastReceiver, UserHandle.of(userId), intentFilter, null, null);
            }
            ContentObserver contactsContentObserver = new ContactsContentObserver(BackgroundThread.getHandler());
            this.mContactsContentObservers.put(userId, contactsContentObserver);
            this.mContext.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsContentObserver, userId);
            NotificationListener notificationListener = new NotificationListener(userId);
            this.mNotificationListeners.put(userId, notificationListener);
            try {
                notificationListener.registerAsSystemService(this.mContext, new ComponentName(this.mContext, getClass()), userId);
            } catch (RemoteException e) {
            }
            if (this.mPackageMonitors.get(userId) == null) {
                PackageMonitor packageMonitor = new PerUserPackageMonitor();
                packageMonitor.register(this.mContext, (Looper) null, UserHandle.of(userId), true);
                this.mPackageMonitors.put(userId, packageMonitor);
            }
            if (userId == 0) {
                this.mCallLogContentObserver = new CallLogContentObserver(BackgroundThread.getHandler());
                this.mContext.getContentResolver().registerContentObserver(CallLog.CONTENT_URI, true, this.mCallLogContentObserver, 0);
                this.mMmsSmsContentObserver = new MmsSmsContentObserver(BackgroundThread.getHandler());
                this.mContext.getContentResolver().registerContentObserver(Telephony.MmsSms.CONTENT_URI, false, this.mMmsSmsContentObserver, 0);
            }
            DataMaintenanceService.scheduleJob(this.mContext, userId);
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: cleanupUser */
    public void m5329x2b4a9bba(int userId) {
        synchronized (this.mLock) {
            UserData userData = this.mUserDataArray.get(userId);
            if (userData != null && !userData.isUnlocked()) {
                ContentResolver contentResolver = this.mContext.getContentResolver();
                if (this.mUsageStatsQueryFutures.indexOfKey(userId) >= 0) {
                    this.mUsageStatsQueryFutures.get(userId).cancel(true);
                }
                if (this.mBroadcastReceivers.indexOfKey(userId) >= 0) {
                    this.mContext.unregisterReceiver(this.mBroadcastReceivers.get(userId));
                }
                if (this.mContactsContentObservers.indexOfKey(userId) >= 0) {
                    contentResolver.unregisterContentObserver(this.mContactsContentObservers.get(userId));
                }
                if (this.mNotificationListeners.indexOfKey(userId) >= 0) {
                    try {
                        this.mNotificationListeners.get(userId).unregisterAsSystemService();
                    } catch (RemoteException e) {
                    }
                }
                if (this.mPackageMonitors.indexOfKey(userId) >= 0) {
                    this.mPackageMonitors.get(userId).unregister();
                }
                if (userId == 0) {
                    ContentObserver contentObserver = this.mCallLogContentObserver;
                    if (contentObserver != null) {
                        contentResolver.unregisterContentObserver(contentObserver);
                        this.mCallLogContentObserver = null;
                    }
                    ContentObserver contentObserver2 = this.mMmsSmsContentObserver;
                    if (contentObserver2 != null) {
                        contentResolver.unregisterContentObserver(contentObserver2);
                        this.mCallLogContentObserver = null;
                    }
                }
                DataMaintenanceService.cancelJob(this.mContext, userId);
            }
        }
    }

    public int mimeTypeToShareEventType(String mimeType) {
        if (mimeType == null) {
            return 7;
        }
        if (mimeType.startsWith("text/")) {
            return 4;
        }
        if (mimeType.startsWith("image/")) {
            return 5;
        }
        if (!mimeType.startsWith("video/")) {
            return 7;
        }
        return 6;
    }

    private void pruneUninstalledPackageData(UserData userData) {
        final Set<String> installApps = new ArraySet<>();
        this.mPackageManagerInternal.forEachInstalledPackage(new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda11
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                installApps.add(((AndroidPackage) obj).getPackageName());
            }
        }, userData.getUserId());
        final List<String> packagesToDelete = new ArrayList<>();
        userData.forAllPackages(new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda12
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DataManager.lambda$pruneUninstalledPackageData$10(installApps, packagesToDelete, (PackageData) obj);
            }
        });
        for (String packageName : packagesToDelete) {
            userData.deletePackageData(packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$pruneUninstalledPackageData$10(Set installApps, List packagesToDelete, PackageData packageData) {
        if (!installApps.contains(packageData.getPackageName())) {
            packagesToDelete.add(packageData.getPackageName());
        }
    }

    private List<ShortcutInfo> getShortcuts(String packageName, int userId, List<String> shortcutIds) {
        return this.mShortcutServiceInternal.getShortcuts(0, this.mContext.getPackageName(), 0L, packageName, shortcutIds, (List) null, (ComponentName) null, 3091, userId, Process.myPid(), Process.myUid());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forAllUnlockedUsers(Consumer<UserData> consumer) {
        for (int i = 0; i < this.mUserDataArray.size(); i++) {
            int userId = this.mUserDataArray.keyAt(i);
            UserData userData = this.mUserDataArray.get(userId);
            if (userData.isUnlocked()) {
                consumer.accept(userData);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public UserData getUnlockedUserData(int userId) {
        UserData userData = this.mUserDataArray.get(userId);
        if (userData == null || !userData.isUnlocked()) {
            return null;
        }
        return userData;
    }

    private void updateDefaultDialer(UserData userData) {
        String defaultDialer;
        TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService(TelecomManager.class);
        if (telecomManager != null) {
            defaultDialer = telecomManager.getDefaultDialerPackage(new UserHandle(userData.getUserId()));
        } else {
            defaultDialer = null;
        }
        userData.setDefaultDialer(defaultDialer);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDefaultSmsApp(UserData userData) {
        ComponentName component = SmsApplication.getDefaultSmsApplicationAsUser(this.mContext, false, userData.getUserId());
        String defaultSmsApp = component != null ? component.getPackageName() : null;
        userData.setDefaultSmsApp(defaultSmsApp);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PackageData getPackageIfConversationExists(StatusBarNotification sbn, Consumer<ConversationInfo> conversationConsumer) {
        PackageData packageData;
        ConversationInfo conversationInfo;
        Notification notification = sbn.getNotification();
        String shortcutId = notification.getShortcutId();
        if (shortcutId == null || (packageData = getPackage(sbn.getPackageName(), sbn.getUser().getIdentifier())) == null || (conversationInfo = packageData.getConversationStore().getConversation(shortcutId)) == null) {
            return null;
        }
        conversationConsumer.accept(conversationInfo);
        return packageData;
    }

    private boolean isCachedRecentConversation(ConversationInfo conversationInfo) {
        return conversationInfo.isShortcutCachedForNotification() && Objects.equals(conversationInfo.getNotificationChannelId(), conversationInfo.getParentNotificationChannelId()) && conversationInfo.getLastEventTimestamp() > 0;
    }

    private boolean hasActiveNotifications(String packageName, int userId, String shortcutId) {
        NotificationListener notificationListener = this.mNotificationListeners.get(userId);
        return notificationListener != null && notificationListener.hasActiveNotifications(packageName, shortcutId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cleanupCachedShortcuts(int userId, int targetCachedCount) {
        UserData userData = getUnlockedUserData(userId);
        if (userData == null) {
            return;
        }
        final List<Pair<String, ConversationInfo>> cachedConvos = new ArrayList<>();
        userData.forAllPackages(new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DataManager.this.m5325x7d2128a1(cachedConvos, (PackageData) obj);
            }
        });
        if (cachedConvos.size() <= targetCachedCount) {
            return;
        }
        int numToUncache = cachedConvos.size() - targetCachedCount;
        PriorityQueue<Pair<String, ConversationInfo>> maxHeap = new PriorityQueue<>(numToUncache + 1, Comparator.comparingLong(new ToLongFunction() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda2
            @Override // java.util.function.ToLongFunction
            public final long applyAsLong(Object obj) {
                long lastEventTimestamp;
                lastEventTimestamp = ((ConversationInfo) ((Pair) obj).second).getLastEventTimestamp();
                return lastEventTimestamp;
            }
        }).reversed());
        for (Pair<String, ConversationInfo> cached : cachedConvos) {
            if (!hasActiveNotifications((String) cached.first, userId, ((ConversationInfo) cached.second).getShortcutId())) {
                maxHeap.offer(cached);
                if (maxHeap.size() > numToUncache) {
                    maxHeap.poll();
                }
            }
        }
        while (!maxHeap.isEmpty()) {
            Pair<String, ConversationInfo> toUncache = maxHeap.poll();
            this.mShortcutServiceInternal.uncacheShortcuts(userId, this.mContext.getPackageName(), (String) toUncache.first, Collections.singletonList(((ConversationInfo) toUncache.second).getShortcutId()), userId, 16384);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cleanupCachedShortcuts$12$com-android-server-people-data-DataManager  reason: not valid java name */
    public /* synthetic */ void m5325x7d2128a1(final List cachedConvos, final PackageData packageData) {
        packageData.forAllConversations(new Consumer() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda13
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                DataManager.this.m5324xba34bf42(cachedConvos, packageData, (ConversationInfo) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$cleanupCachedShortcuts$11$com-android-server-people-data-DataManager  reason: not valid java name */
    public /* synthetic */ void m5324xba34bf42(List cachedConvos, PackageData packageData, ConversationInfo conversationInfo) {
        if (isCachedRecentConversation(conversationInfo)) {
            cachedConvos.add(Pair.create(packageData.getPackageName(), conversationInfo));
        }
    }

    void addOrUpdateConversationInfo(ShortcutInfo shortcutInfo) {
        ConversationInfo.Builder builder;
        UserData userData = getUnlockedUserData(shortcutInfo.getUserId());
        if (userData == null) {
            return;
        }
        PackageData packageData = userData.getOrCreatePackageData(shortcutInfo.getPackage());
        ConversationStore conversationStore = packageData.getConversationStore();
        ConversationInfo oldConversationInfo = conversationStore.getConversation(shortcutInfo.getId());
        if (oldConversationInfo != null) {
            builder = new ConversationInfo.Builder(oldConversationInfo);
        } else {
            builder = new ConversationInfo.Builder();
        }
        builder.setShortcutId(shortcutInfo.getId());
        builder.setLocusId(shortcutInfo.getLocusId());
        builder.setShortcutFlags(shortcutInfo.getFlags());
        builder.setContactUri(null);
        builder.setContactPhoneNumber(null);
        builder.setContactStarred(false);
        if (shortcutInfo.getPersons() != null && shortcutInfo.getPersons().length != 0) {
            Person person = shortcutInfo.getPersons()[0];
            builder.setPersonImportant(person.isImportant());
            builder.setPersonBot(person.isBot());
            String contactUri = person.getUri();
            if (contactUri != null) {
                ContactsQueryHelper helper = this.mInjector.createContactsQueryHelper(this.mContext);
                if (helper.query(contactUri)) {
                    builder.setContactUri(helper.getContactUri());
                    builder.setContactStarred(helper.isStarred());
                    builder.setContactPhoneNumber(helper.getPhoneNumber());
                }
            }
        }
        updateConversationStoreThenNotifyListeners(conversationStore, builder.build(), shortcutInfo);
    }

    ContentObserver getContactsContentObserverForTesting(int userId) {
        return this.mContactsContentObservers.get(userId);
    }

    ContentObserver getCallLogContentObserverForTesting() {
        return this.mCallLogContentObserver;
    }

    ContentObserver getMmsSmsContentObserverForTesting() {
        return this.mMmsSmsContentObserver;
    }

    NotificationListener getNotificationListenerServiceForTesting(int userId) {
        return this.mNotificationListeners.get(userId);
    }

    PackageMonitor getPackageMonitorForTesting(int userId) {
        return this.mPackageMonitors.get(userId);
    }

    UserData getUserDataForTesting(int userId) {
        return this.mUserDataArray.get(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ContactsContentObserver extends ContentObserver {
        private long mLastUpdatedTimestamp;

        private ContactsContentObserver(Handler handler) {
            super(handler);
            this.mLastUpdatedTimestamp = System.currentTimeMillis();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            ContactsQueryHelper helper = DataManager.this.mInjector.createContactsQueryHelper(DataManager.this.mContext);
            if (!helper.querySince(this.mLastUpdatedTimestamp)) {
                return;
            }
            final Uri contactUri = helper.getContactUri();
            final ConversationSelector conversationSelector = new ConversationSelector();
            UserData userData = DataManager.this.getUnlockedUserData(userId);
            if (userData == null) {
                return;
            }
            userData.forAllPackages(new Consumer() { // from class: com.android.server.people.data.DataManager$ContactsContentObserver$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DataManager.ContactsContentObserver.lambda$onChange$0(contactUri, conversationSelector, (PackageData) obj);
                }
            });
            if (conversationSelector.mConversationInfo == null) {
                return;
            }
            ConversationInfo.Builder builder = new ConversationInfo.Builder(conversationSelector.mConversationInfo);
            builder.setContactStarred(helper.isStarred());
            builder.setContactPhoneNumber(helper.getPhoneNumber());
            DataManager.this.updateConversationStoreThenNotifyListeners(conversationSelector.mConversationStore, builder.build(), conversationSelector.mPackageName, userId);
            this.mLastUpdatedTimestamp = helper.getLastUpdatedTimestamp();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$onChange$0(Uri contactUri, ConversationSelector conversationSelector, PackageData packageData) {
            ConversationInfo ci = packageData.getConversationStore().getConversationByContactUri(contactUri);
            if (ci != null) {
                conversationSelector.mConversationStore = packageData.getConversationStore();
                conversationSelector.mConversationInfo = ci;
                conversationSelector.mPackageName = packageData.getPackageName();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes2.dex */
        public class ConversationSelector {
            private ConversationInfo mConversationInfo;
            private ConversationStore mConversationStore;
            private String mPackageName;

            private ConversationSelector() {
                this.mConversationStore = null;
                this.mConversationInfo = null;
                this.mPackageName = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class CallLogContentObserver extends ContentObserver implements BiConsumer<String, Event> {
        private final CallLogQueryHelper mCallLogQueryHelper;
        private long mLastCallTimestamp;

        private CallLogContentObserver(Handler handler) {
            super(handler);
            this.mCallLogQueryHelper = DataManager.this.mInjector.createCallLogQueryHelper(DataManager.this.mContext, this);
            this.mLastCallTimestamp = System.currentTimeMillis() - 300000;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            if (this.mCallLogQueryHelper.querySince(this.mLastCallTimestamp)) {
                this.mLastCallTimestamp = this.mCallLogQueryHelper.getLastCallTimestamp();
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.BiConsumer
        public void accept(final String phoneNumber, final Event event) {
            DataManager.this.forAllUnlockedUsers(new Consumer() { // from class: com.android.server.people.data.DataManager$CallLogContentObserver$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DataManager.CallLogContentObserver.lambda$accept$0(phoneNumber, event, (UserData) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$accept$0(String phoneNumber, Event event, UserData userData) {
            PackageData defaultDialer = userData.getDefaultDialer();
            if (defaultDialer == null) {
                return;
            }
            ConversationStore conversationStore = defaultDialer.getConversationStore();
            if (conversationStore.getConversationByPhoneNumber(phoneNumber) == null) {
                return;
            }
            EventStore eventStore = defaultDialer.getEventStore();
            eventStore.getOrCreateEventHistory(2, phoneNumber).addEvent(event);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class MmsSmsContentObserver extends ContentObserver implements BiConsumer<String, Event> {
        private long mLastMmsTimestamp;
        private long mLastSmsTimestamp;
        private final MmsQueryHelper mMmsQueryHelper;
        private final SmsQueryHelper mSmsQueryHelper;

        private MmsSmsContentObserver(Handler handler) {
            super(handler);
            this.mMmsQueryHelper = DataManager.this.mInjector.createMmsQueryHelper(DataManager.this.mContext, this);
            this.mSmsQueryHelper = DataManager.this.mInjector.createSmsQueryHelper(DataManager.this.mContext, this);
            long currentTimeMillis = System.currentTimeMillis() - 300000;
            this.mLastMmsTimestamp = currentTimeMillis;
            this.mLastSmsTimestamp = currentTimeMillis;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            if (this.mMmsQueryHelper.querySince(this.mLastMmsTimestamp)) {
                this.mLastMmsTimestamp = this.mMmsQueryHelper.getLastMessageTimestamp();
            }
            if (this.mSmsQueryHelper.querySince(this.mLastSmsTimestamp)) {
                this.mLastSmsTimestamp = this.mSmsQueryHelper.getLastMessageTimestamp();
            }
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.BiConsumer
        public void accept(final String phoneNumber, final Event event) {
            DataManager.this.forAllUnlockedUsers(new Consumer() { // from class: com.android.server.people.data.DataManager$MmsSmsContentObserver$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DataManager.MmsSmsContentObserver.lambda$accept$0(phoneNumber, event, (UserData) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ void lambda$accept$0(String phoneNumber, Event event, UserData userData) {
            PackageData defaultSmsApp = userData.getDefaultSmsApp();
            if (defaultSmsApp == null) {
                return;
            }
            ConversationStore conversationStore = defaultSmsApp.getConversationStore();
            if (conversationStore.getConversationByPhoneNumber(phoneNumber) == null) {
                return;
            }
            EventStore eventStore = defaultSmsApp.getEventStore();
            eventStore.getOrCreateEventHistory(3, phoneNumber).addEvent(event);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ShortcutServiceCallback implements LauncherApps.ShortcutChangeCallback {
        private ShortcutServiceCallback() {
        }

        public void onShortcutsAddedOrUpdated(final String packageName, final List<ShortcutInfo> shortcuts, final UserHandle user) {
            DataManager.this.mInjector.getBackgroundExecutor().execute(new Runnable() { // from class: com.android.server.people.data.DataManager$ShortcutServiceCallback$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DataManager.ShortcutServiceCallback.this.m5344x1516769d(packageName, user, shortcuts);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onShortcutsAddedOrUpdated$0$com-android-server-people-data-DataManager$ShortcutServiceCallback  reason: not valid java name */
        public /* synthetic */ void m5344x1516769d(String packageName, UserHandle user, List shortcuts) {
            PackageData packageData = DataManager.this.getPackage(packageName, user.getIdentifier());
            Iterator it = shortcuts.iterator();
            while (it.hasNext()) {
                ShortcutInfo shortcut = (ShortcutInfo) it.next();
                if (ShortcutHelper.isConversationShortcut(shortcut, DataManager.this.mShortcutServiceInternal, user.getIdentifier())) {
                    if (shortcut.isCached()) {
                        ConversationInfo conversationInfo = packageData != null ? packageData.getConversationInfo(shortcut.getId()) : null;
                        if (conversationInfo == null || !conversationInfo.isShortcutCachedForNotification()) {
                            DataManager.this.cleanupCachedShortcuts(user.getIdentifier(), 29);
                        }
                    }
                    DataManager.this.addOrUpdateConversationInfo(shortcut);
                }
            }
        }

        public void onShortcutsRemoved(final String packageName, final List<ShortcutInfo> shortcuts, final UserHandle user) {
            DataManager.this.mInjector.getBackgroundExecutor().execute(new Runnable() { // from class: com.android.server.people.data.DataManager$ShortcutServiceCallback$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DataManager.ShortcutServiceCallback.this.m5345x67513b16(packageName, user, shortcuts);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onShortcutsRemoved$1$com-android-server-people-data-DataManager$ShortcutServiceCallback  reason: not valid java name */
        public /* synthetic */ void m5345x67513b16(String packageName, UserHandle user, List shortcuts) {
            int uid = -1;
            try {
                uid = DataManager.this.mContext.getPackageManager().getPackageUidAsUser(packageName, user.getIdentifier());
            } catch (PackageManager.NameNotFoundException e) {
                Slog.e(DataManager.TAG, "Package not found: " + packageName, e);
            }
            PackageData packageData = DataManager.this.getPackage(packageName, user.getIdentifier());
            Set<String> shortcutIds = new HashSet<>();
            Iterator it = shortcuts.iterator();
            while (it.hasNext()) {
                ShortcutInfo shortcutInfo = (ShortcutInfo) it.next();
                if (packageData != null) {
                    packageData.deleteDataForConversation(shortcutInfo.getId());
                }
                shortcutIds.add(shortcutInfo.getId());
            }
            if (uid != -1) {
                DataManager.this.mNotificationManagerInternal.onConversationRemoved(packageName, uid, shortcutIds);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class NotificationListener extends NotificationListenerService {
        private final Map<Pair<String, String>, Integer> mActiveNotifCounts;
        private final int mUserId;

        private NotificationListener(int userId) {
            this.mActiveNotifCounts = new ArrayMap();
            this.mUserId = userId;
        }

        @Override // android.service.notification.NotificationListenerService
        public void onNotificationPosted(final StatusBarNotification sbn, NotificationListenerService.RankingMap map) {
            if (sbn.getUser().getIdentifier() != this.mUserId) {
                return;
            }
            final String shortcutId = sbn.getNotification().getShortcutId();
            PackageData packageData = DataManager.this.getPackageIfConversationExists(sbn, new Consumer() { // from class: com.android.server.people.data.DataManager$NotificationListener$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DataManager.NotificationListener.this.m5342x480ffa73(sbn, shortcutId, (ConversationInfo) obj);
                }
            });
            if (packageData != null) {
                NotificationListenerService.Ranking rank = new NotificationListenerService.Ranking();
                map.getRanking(sbn.getKey(), rank);
                ConversationInfo conversationInfo = packageData.getConversationInfo(shortcutId);
                if (conversationInfo == null) {
                    return;
                }
                ConversationInfo.Builder updated = new ConversationInfo.Builder(conversationInfo).setLastEventTimestamp(sbn.getPostTime()).setNotificationChannelId(rank.getChannel().getId());
                if (!TextUtils.isEmpty(rank.getChannel().getParentChannelId())) {
                    updated.setParentNotificationChannelId(rank.getChannel().getParentChannelId());
                } else {
                    updated.setParentNotificationChannelId(sbn.getNotification().getChannelId());
                }
                packageData.getConversationStore().addOrUpdate(updated.build());
                EventHistoryImpl eventHistory = packageData.getEventStore().getOrCreateEventHistory(0, shortcutId);
                eventHistory.addEvent(new Event(sbn.getPostTime(), 2));
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onNotificationPosted$0$com-android-server-people-data-DataManager$NotificationListener  reason: not valid java name */
        public /* synthetic */ void m5342x480ffa73(StatusBarNotification sbn, String shortcutId, ConversationInfo conversationInfo) {
            synchronized (this) {
                this.mActiveNotifCounts.merge(Pair.create(sbn.getPackageName(), shortcutId), 1, new BiFunction() { // from class: com.android.server.people.data.DataManager$NotificationListener$$ExternalSyntheticLambda2
                    @Override // java.util.function.BiFunction
                    public final Object apply(Object obj, Object obj2) {
                        return Integer.valueOf(Integer.sum(((Integer) obj).intValue(), ((Integer) obj2).intValue()));
                    }
                });
            }
        }

        @Override // android.service.notification.NotificationListenerService
        public synchronized void onNotificationRemoved(final StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap, int reason) {
            if (sbn.getUser().getIdentifier() != this.mUserId) {
                return;
            }
            final String shortcutId = sbn.getNotification().getShortcutId();
            PackageData packageData = DataManager.this.getPackageIfConversationExists(sbn, new Consumer() { // from class: com.android.server.people.data.DataManager$NotificationListener$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    DataManager.NotificationListener.this.m5343xd0bf8227(sbn, shortcutId, (ConversationInfo) obj);
                }
            });
            if (reason == 1 && packageData != null) {
                long currentTime = System.currentTimeMillis();
                ConversationInfo conversationInfo = packageData.getConversationInfo(shortcutId);
                if (conversationInfo == null) {
                    return;
                }
                ConversationInfo updated = new ConversationInfo.Builder(conversationInfo).setLastEventTimestamp(currentTime).build();
                packageData.getConversationStore().addOrUpdate(updated);
                EventHistoryImpl eventHistory = packageData.getEventStore().getOrCreateEventHistory(0, shortcutId);
                eventHistory.addEvent(new Event(currentTime, 3));
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onNotificationRemoved$1$com-android-server-people-data-DataManager$NotificationListener  reason: not valid java name */
        public /* synthetic */ void m5343xd0bf8227(StatusBarNotification sbn, String shortcutId, ConversationInfo conversationInfo) {
            Pair<String, String> conversationKey = Pair.create(sbn.getPackageName(), shortcutId);
            synchronized (this) {
                int count = this.mActiveNotifCounts.getOrDefault(conversationKey, 0).intValue() - 1;
                if (count <= 0) {
                    this.mActiveNotifCounts.remove(conversationKey);
                    DataManager.this.cleanupCachedShortcuts(this.mUserId, 30);
                } else {
                    this.mActiveNotifCounts.put(conversationKey, Integer.valueOf(count));
                }
            }
        }

        @Override // android.service.notification.NotificationListenerService
        public void onNotificationChannelModified(String pkg, UserHandle user, NotificationChannel channel, int modificationType) {
            ConversationStore conversationStore;
            ConversationInfo conversationInfo;
            if (user.getIdentifier() != this.mUserId) {
                return;
            }
            PackageData packageData = DataManager.this.getPackage(pkg, user.getIdentifier());
            String shortcutId = channel.getConversationId();
            if (packageData == null || shortcutId == null || (conversationInfo = (conversationStore = packageData.getConversationStore()).getConversation(shortcutId)) == null) {
                return;
            }
            ConversationInfo.Builder builder = new ConversationInfo.Builder(conversationInfo);
            switch (modificationType) {
                case 1:
                case 2:
                    builder.setNotificationChannelId(channel.getId());
                    builder.setImportant(channel.isImportantConversation());
                    builder.setDemoted(channel.isDemoted());
                    builder.setNotificationSilenced(channel.getImportance() <= 2);
                    builder.setBubbled(channel.canBubble());
                    break;
                case 3:
                    builder.setNotificationChannelId(null);
                    builder.setImportant(false);
                    builder.setDemoted(false);
                    builder.setNotificationSilenced(false);
                    builder.setBubbled(false);
                    break;
            }
            DataManager.this.updateConversationStoreThenNotifyListeners(conversationStore, builder.build(), pkg, packageData.getUserId());
        }

        synchronized boolean hasActiveNotifications(String packageName, String shortcutId) {
            return this.mActiveNotifCounts.containsKey(Pair.create(packageName, shortcutId));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class UsageStatsQueryRunnable implements Runnable, UsageStatsQueryHelper.EventListener {
        private long mLastEventTimestamp;
        private final UsageStatsQueryHelper mUsageStatsQueryHelper;

        private UsageStatsQueryRunnable(final int userId) {
            this.mUsageStatsQueryHelper = DataManager.this.mInjector.createUsageStatsQueryHelper(userId, new Function() { // from class: com.android.server.people.data.DataManager$UsageStatsQueryRunnable$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return DataManager.UsageStatsQueryRunnable.this.m5346xa60742d6(userId, (String) obj);
                }
            }, this);
            this.mLastEventTimestamp = System.currentTimeMillis() - 300000;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$new$0$com-android-server-people-data-DataManager$UsageStatsQueryRunnable  reason: not valid java name */
        public /* synthetic */ PackageData m5346xa60742d6(int userId, String packageName) {
            return DataManager.this.getPackage(packageName, userId);
        }

        @Override // java.lang.Runnable
        public void run() {
            if (this.mUsageStatsQueryHelper.querySince(this.mLastEventTimestamp)) {
                this.mLastEventTimestamp = this.mUsageStatsQueryHelper.getLastEventTimestamp();
            }
        }

        @Override // com.android.server.people.data.UsageStatsQueryHelper.EventListener
        public void onEvent(PackageData packageData, ConversationInfo conversationInfo, Event event) {
            if (event.getType() == 13) {
                ConversationInfo updated = new ConversationInfo.Builder(conversationInfo).setLastEventTimestamp(event.getTimestamp()).build();
                DataManager.this.updateConversationStoreThenNotifyListeners(packageData.getConversationStore(), updated, packageData.getPackageName(), packageData.getUserId());
            }
        }
    }

    public void addConversationsListener(PeopleService.ConversationsListener listener) {
        synchronized (this.mConversationsListeners) {
            this.mConversationsListeners.add((PeopleService.ConversationsListener) Objects.requireNonNull(listener));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateConversationStoreThenNotifyListeners(ConversationStore cs, ConversationInfo modifiedConv, String packageName, int userId) {
        cs.addOrUpdate(modifiedConv);
        ConversationChannel channel = getConversationChannel(packageName, userId, modifiedConv.getShortcutId(), modifiedConv);
        if (channel != null) {
            notifyConversationsListeners(Arrays.asList(channel));
        }
    }

    private void updateConversationStoreThenNotifyListeners(ConversationStore cs, ConversationInfo modifiedConv, ShortcutInfo shortcutInfo) {
        cs.addOrUpdate(modifiedConv);
        ConversationChannel channel = getConversationChannel(shortcutInfo, modifiedConv);
        if (channel != null) {
            notifyConversationsListeners(Arrays.asList(channel));
        }
    }

    void notifyConversationsListeners(final List<ConversationChannel> changedConversations) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.people.data.DataManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DataManager.this.m5328x8428bb0(changedConversations);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyConversationsListeners$14$com-android-server-people-data-DataManager  reason: not valid java name */
    public /* synthetic */ void m5328x8428bb0(List changedConversations) {
        List<PeopleService.ConversationsListener> copy;
        try {
            synchronized (this.mLock) {
                copy = new ArrayList<>(this.mConversationsListeners);
            }
            for (PeopleService.ConversationsListener listener : copy) {
                listener.onConversationsUpdate(changedConversations);
            }
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class PerUserBroadcastReceiver extends BroadcastReceiver {
        private final int mUserId;

        private PerUserBroadcastReceiver(int userId) {
            this.mUserId = userId;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            UserData userData = DataManager.this.getUnlockedUserData(this.mUserId);
            if (userData == null) {
                return;
            }
            if ("android.telecom.action.DEFAULT_DIALER_CHANGED".equals(intent.getAction())) {
                String defaultDialer = intent.getStringExtra("android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME");
                userData.setDefaultDialer(defaultDialer);
            } else if ("android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED_INTERNAL".equals(intent.getAction())) {
                DataManager.this.updateDefaultSmsApp(userData);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class PerUserPackageMonitor extends PackageMonitor {
        private PerUserPackageMonitor() {
        }

        public void onPackageRemoved(String packageName, int uid) {
            super.onPackageRemoved(packageName, uid);
            int userId = getChangingUserId();
            UserData userData = DataManager.this.getUnlockedUserData(userId);
            if (userData != null) {
                userData.deletePackageData(packageName);
            }
        }
    }

    /* loaded from: classes2.dex */
    private class ShutdownBroadcastReceiver extends BroadcastReceiver {
        private ShutdownBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            DataManager.this.forAllUnlockedUsers(new Consumer() { // from class: com.android.server.people.data.DataManager$ShutdownBroadcastReceiver$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((UserData) obj).forAllPackages(new Consumer() { // from class: com.android.server.people.data.DataManager$ShutdownBroadcastReceiver$$ExternalSyntheticLambda1
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj2) {
                            ((PackageData) obj2).saveToDisk();
                        }
                    });
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Injector {
        Injector() {
        }

        ScheduledExecutorService createScheduledExecutor() {
            return Executors.newSingleThreadScheduledExecutor();
        }

        Executor getBackgroundExecutor() {
            return BackgroundThread.getExecutor();
        }

        ContactsQueryHelper createContactsQueryHelper(Context context) {
            return new ContactsQueryHelper(context);
        }

        CallLogQueryHelper createCallLogQueryHelper(Context context, BiConsumer<String, Event> eventConsumer) {
            return new CallLogQueryHelper(context, eventConsumer);
        }

        MmsQueryHelper createMmsQueryHelper(Context context, BiConsumer<String, Event> eventConsumer) {
            return new MmsQueryHelper(context, eventConsumer);
        }

        SmsQueryHelper createSmsQueryHelper(Context context, BiConsumer<String, Event> eventConsumer) {
            return new SmsQueryHelper(context, eventConsumer);
        }

        UsageStatsQueryHelper createUsageStatsQueryHelper(int userId, Function<String, PackageData> packageDataGetter, UsageStatsQueryHelper.EventListener eventListener) {
            return new UsageStatsQueryHelper(userId, packageDataGetter, eventListener);
        }
    }
}
