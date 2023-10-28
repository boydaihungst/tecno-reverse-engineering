package com.android.server.people.data;

import android.net.Uri;
import android.util.ArrayMap;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class EventStore {
    static final int CATEGORY_CALL = 2;
    static final int CATEGORY_CLASS_BASED = 4;
    static final int CATEGORY_LOCUS_ID_BASED = 1;
    static final int CATEGORY_SHORTCUT_BASED = 0;
    static final int CATEGORY_SMS = 3;
    private final List<Map<String, EventHistoryImpl>> mEventHistoryMaps;
    private final List<File> mEventsCategoryDirs;
    private final ScheduledExecutorService mScheduledExecutorService;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface EventCategory {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public EventStore(File packageDir, ScheduledExecutorService scheduledExecutorService) {
        ArrayList arrayList = new ArrayList();
        this.mEventHistoryMaps = arrayList;
        ArrayList arrayList2 = new ArrayList();
        this.mEventsCategoryDirs = arrayList2;
        arrayList.add(0, new ArrayMap());
        arrayList.add(1, new ArrayMap());
        arrayList.add(2, new ArrayMap());
        arrayList.add(3, new ArrayMap());
        arrayList.add(4, new ArrayMap());
        File eventDir = new File(packageDir, "event");
        arrayList2.add(0, new File(eventDir, "shortcut"));
        arrayList2.add(1, new File(eventDir, "locus"));
        arrayList2.add(2, new File(eventDir, "call"));
        arrayList2.add(3, new File(eventDir, "sms"));
        arrayList2.add(4, new File(eventDir, "class"));
        this.mScheduledExecutorService = scheduledExecutorService;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void loadFromDisk() {
        for (int category = 0; category < this.mEventsCategoryDirs.size(); category++) {
            File categoryDir = this.mEventsCategoryDirs.get(category);
            Map<String, EventHistoryImpl> existingEventHistoriesImpl = EventHistoryImpl.eventHistoriesImplFromDisk(categoryDir, this.mScheduledExecutorService);
            this.mEventHistoryMaps.get(category).putAll(existingEventHistoriesImpl);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void saveToDisk() {
        for (Map<String, EventHistoryImpl> map : this.mEventHistoryMaps) {
            for (EventHistoryImpl eventHistory : map.values()) {
                eventHistory.saveToDisk();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized EventHistory getEventHistory(int category, String key) {
        return this.mEventHistoryMaps.get(category).get(key);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized EventHistoryImpl getOrCreateEventHistory(final int category, final String key) {
        return this.mEventHistoryMaps.get(category).computeIfAbsent(key, new Function() { // from class: com.android.server.people.data.EventStore$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return EventStore.this.m5356xd3dd966(category, key, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getOrCreateEventHistory$0$com-android-server-people-data-EventStore  reason: not valid java name */
    public /* synthetic */ EventHistoryImpl m5356xd3dd966(int category, String key, String k) {
        return new EventHistoryImpl(new File(this.mEventsCategoryDirs.get(category), Uri.encode(key)), this.mScheduledExecutorService);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void deleteEventHistory(int category, String key) {
        EventHistoryImpl eventHistory = this.mEventHistoryMaps.get(category).remove(key);
        if (eventHistory != null) {
            eventHistory.onDestroy();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void deleteEventHistories(int category) {
        for (EventHistoryImpl eventHistory : this.mEventHistoryMaps.get(category).values()) {
            eventHistory.onDestroy();
        }
        this.mEventHistoryMaps.get(category).clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void pruneOldEvents() {
        for (Map<String, EventHistoryImpl> map : this.mEventHistoryMaps) {
            for (EventHistoryImpl eventHistory : map.values()) {
                eventHistory.pruneOldEvents();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void pruneOrphanEventHistories(int category, Predicate<String> keyChecker) {
        Set<String> keys = this.mEventHistoryMaps.get(category).keySet();
        List<String> keysToDelete = new ArrayList<>();
        for (String key : keys) {
            if (!keyChecker.test(key)) {
                keysToDelete.add(key);
            }
        }
        Map<String, EventHistoryImpl> eventHistoryMap = this.mEventHistoryMaps.get(category);
        for (String key2 : keysToDelete) {
            EventHistoryImpl eventHistory = eventHistoryMap.remove(key2);
            if (eventHistory != null) {
                eventHistory.onDestroy();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void onDestroy() {
        for (Map<String, EventHistoryImpl> map : this.mEventHistoryMaps) {
            for (EventHistoryImpl eventHistory : map.values()) {
                eventHistory.onDestroy();
            }
        }
    }
}
