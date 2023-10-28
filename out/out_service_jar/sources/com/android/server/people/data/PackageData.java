package com.android.server.people.data;

import android.content.LocusId;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.ArrayMap;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes2.dex */
public class PackageData {
    private final ConversationStore mConversationStore;
    private final EventStore mEventStore;
    private final Predicate<String> mIsDefaultDialerPredicate;
    private final Predicate<String> mIsDefaultSmsAppPredicate;
    private final File mPackageDataDir;
    private final String mPackageName;
    private final int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageData(String packageName, int userId, Predicate<String> isDefaultDialerPredicate, Predicate<String> isDefaultSmsAppPredicate, ScheduledExecutorService scheduledExecutorService, File perUserPeopleDataDir) {
        this.mPackageName = packageName;
        this.mUserId = userId;
        File file = new File(perUserPeopleDataDir, packageName);
        this.mPackageDataDir = file;
        file.mkdirs();
        this.mConversationStore = new ConversationStore(file, scheduledExecutorService);
        this.mEventStore = new EventStore(file, scheduledExecutorService);
        this.mIsDefaultDialerPredicate = isDefaultDialerPredicate;
        this.mIsDefaultSmsAppPredicate = isDefaultSmsAppPredicate;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Map<String, PackageData> packagesDataFromDisk(int userId, Predicate<String> isDefaultDialerPredicate, Predicate<String> isDefaultSmsAppPredicate, ScheduledExecutorService scheduledExecutorService, File perUserPeopleDataDir) {
        Map<String, PackageData> results = new ArrayMap<>();
        File[] packageDirs = perUserPeopleDataDir.listFiles(new EventHistoryImpl$$ExternalSyntheticLambda1());
        if (packageDirs == null) {
            return results;
        }
        for (File packageDir : packageDirs) {
            PackageData packageData = new PackageData(packageDir.getName(), userId, isDefaultDialerPredicate, isDefaultSmsAppPredicate, scheduledExecutorService, perUserPeopleDataDir);
            packageData.loadFromDisk();
            results.put(packageDir.getName(), packageData);
        }
        return results;
    }

    private void loadFromDisk() {
        this.mConversationStore.loadConversationsFromDisk();
        this.mEventStore.loadFromDisk();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveToDisk() {
        this.mConversationStore.saveConversationsToDisk();
        this.mEventStore.saveToDisk();
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public void forAllConversations(Consumer<ConversationInfo> consumer) {
        this.mConversationStore.forAllConversations(consumer);
    }

    public ConversationInfo getConversationInfo(String shortcutId) {
        return getConversationStore().getConversation(shortcutId);
    }

    public EventHistory getEventHistory(String shortcutId) {
        EventHistory smsEventHistory;
        EventHistory callEventHistory;
        EventHistory locusEventHistory;
        AggregateEventHistoryImpl result = new AggregateEventHistoryImpl();
        ConversationInfo conversationInfo = this.mConversationStore.getConversation(shortcutId);
        if (conversationInfo == null) {
            return result;
        }
        EventHistory shortcutEventHistory = getEventStore().getEventHistory(0, shortcutId);
        if (shortcutEventHistory != null) {
            result.addEventHistory(shortcutEventHistory);
        }
        LocusId locusId = conversationInfo.getLocusId();
        if (locusId != null && (locusEventHistory = getEventStore().getEventHistory(1, locusId.getId())) != null) {
            result.addEventHistory(locusEventHistory);
        }
        String phoneNumber = conversationInfo.getContactPhoneNumber();
        if (TextUtils.isEmpty(phoneNumber)) {
            return result;
        }
        if (isDefaultDialer() && (callEventHistory = getEventStore().getEventHistory(2, phoneNumber)) != null) {
            result.addEventHistory(callEventHistory);
        }
        if (isDefaultSmsApp() && (smsEventHistory = getEventStore().getEventHistory(3, phoneNumber)) != null) {
            result.addEventHistory(smsEventHistory);
        }
        return result;
    }

    public EventHistory getClassLevelEventHistory(String className) {
        EventHistory eventHistory = getEventStore().getEventHistory(4, className);
        return eventHistory != null ? eventHistory : new AggregateEventHistoryImpl();
    }

    public boolean isDefaultDialer() {
        return this.mIsDefaultDialerPredicate.test(this.mPackageName);
    }

    public boolean isDefaultSmsApp() {
        return this.mIsDefaultSmsAppPredicate.test(this.mPackageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ConversationStore getConversationStore() {
        return this.mConversationStore;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public EventStore getEventStore() {
        return this.mEventStore;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void deleteDataForConversation(String shortcutId) {
        ConversationInfo conversationInfo = this.mConversationStore.deleteConversation(shortcutId);
        if (conversationInfo == null) {
            return;
        }
        this.mEventStore.deleteEventHistory(0, shortcutId);
        if (conversationInfo.getLocusId() != null) {
            this.mEventStore.deleteEventHistory(1, conversationInfo.getLocusId().getId());
        }
        String phoneNumber = conversationInfo.getContactPhoneNumber();
        if (!TextUtils.isEmpty(phoneNumber)) {
            if (isDefaultDialer()) {
                this.mEventStore.deleteEventHistory(2, phoneNumber);
            }
            if (isDefaultSmsApp()) {
                this.mEventStore.deleteEventHistory(3, phoneNumber);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pruneOrphanEvents() {
        this.mEventStore.pruneOrphanEventHistories(0, new Predicate() { // from class: com.android.server.people.data.PackageData$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return PackageData.this.m5357x2e1f4c15((String) obj);
            }
        });
        this.mEventStore.pruneOrphanEventHistories(1, new Predicate() { // from class: com.android.server.people.data.PackageData$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return PackageData.this.m5358xf10bb574((String) obj);
            }
        });
        if (isDefaultDialer()) {
            this.mEventStore.pruneOrphanEventHistories(2, new Predicate() { // from class: com.android.server.people.data.PackageData$$ExternalSyntheticLambda2
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return PackageData.this.m5359xb3f81ed3((String) obj);
                }
            });
        }
        if (isDefaultSmsApp()) {
            this.mEventStore.pruneOrphanEventHistories(3, new Predicate() { // from class: com.android.server.people.data.PackageData$$ExternalSyntheticLambda3
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return PackageData.this.m5360x76e48832((String) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pruneOrphanEvents$0$com-android-server-people-data-PackageData  reason: not valid java name */
    public /* synthetic */ boolean m5357x2e1f4c15(String key) {
        return this.mConversationStore.getConversation(key) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pruneOrphanEvents$1$com-android-server-people-data-PackageData  reason: not valid java name */
    public /* synthetic */ boolean m5358xf10bb574(String key) {
        return this.mConversationStore.getConversationByLocusId(new LocusId(key)) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pruneOrphanEvents$2$com-android-server-people-data-PackageData  reason: not valid java name */
    public /* synthetic */ boolean m5359xb3f81ed3(String key) {
        return this.mConversationStore.getConversationByPhoneNumber(key) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$pruneOrphanEvents$3$com-android-server-people-data-PackageData  reason: not valid java name */
    public /* synthetic */ boolean m5360x76e48832(String key) {
        return this.mConversationStore.getConversationByPhoneNumber(key) != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDestroy() {
        this.mEventStore.onDestroy();
        this.mConversationStore.onDestroy();
        FileUtils.deleteContentsAndDir(this.mPackageDataDir);
    }
}
