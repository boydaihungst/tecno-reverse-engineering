package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.EventList;
import android.app.usage.EventStats;
import android.app.usage.TimeSparseArray;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArrayMap;
import android.util.SparseIntArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.am.AssistDataRequester;
import com.android.server.pm.PackageManagerService;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.usage.IntervalStats;
import com.android.server.usage.UsageStatsDatabase;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class UserUsageStatsService {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsageStatsService";
    private static final int sDateFormatFlags = 131093;
    private final Context mContext;
    private final UsageStatsDatabase mDatabase;
    private String mLastBackgroundedPackage;
    private final StatsUpdatedListener mListener;
    private final String mLogPrefix;
    private final int mUserId;
    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final long[] INTERVAL_LENGTH = {86400000, UnixCalendar.WEEK_IN_MILLIS, UnixCalendar.MONTH_IN_MILLIS, 31536000000L};
    private static final UsageStatsDatabase.StatCombiner<UsageStats> sUsageStatsCombiner = new UsageStatsDatabase.StatCombiner<UsageStats>() { // from class: com.android.server.usage.UserUsageStatsService.1
        @Override // com.android.server.usage.UsageStatsDatabase.StatCombiner
        public boolean combine(IntervalStats stats, boolean mutable, List<UsageStats> accResult) {
            if (!mutable) {
                accResult.addAll(stats.packageStats.values());
                return true;
            }
            int statCount = stats.packageStats.size();
            for (int i = 0; i < statCount; i++) {
                accResult.add(new UsageStats(stats.packageStats.valueAt(i)));
            }
            return true;
        }
    };
    private static final UsageStatsDatabase.StatCombiner<ConfigurationStats> sConfigStatsCombiner = new UsageStatsDatabase.StatCombiner<ConfigurationStats>() { // from class: com.android.server.usage.UserUsageStatsService.2
        @Override // com.android.server.usage.UsageStatsDatabase.StatCombiner
        public boolean combine(IntervalStats stats, boolean mutable, List<ConfigurationStats> accResult) {
            if (!mutable) {
                accResult.addAll(stats.configurations.values());
                return true;
            }
            int configCount = stats.configurations.size();
            for (int i = 0; i < configCount; i++) {
                accResult.add(new ConfigurationStats(stats.configurations.valueAt(i)));
            }
            return true;
        }
    };
    private static final UsageStatsDatabase.StatCombiner<EventStats> sEventStatsCombiner = new UsageStatsDatabase.StatCombiner<EventStats>() { // from class: com.android.server.usage.UserUsageStatsService.3
        @Override // com.android.server.usage.UsageStatsDatabase.StatCombiner
        public boolean combine(IntervalStats stats, boolean mutable, List<EventStats> accResult) {
            stats.addEventStatsTo(accResult);
            return true;
        }
    };
    private boolean mStatsChanged = false;
    private final SparseArrayMap<String, CachedEarlyEvents> mCachedEarlyEvents = new SparseArrayMap<>();
    private final UnixCalendar mDailyExpiryDate = new UnixCalendar(0);
    private final IntervalStats[] mCurrentStats = new IntervalStats[4];
    private long mRealTimeSnapshot = SystemClock.elapsedRealtime();
    private long mSystemTimeSnapshot = System.currentTimeMillis();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface StatsUpdatedListener {
        void onNewUpdate(int i);

        void onStatsReloaded();

        void onStatsUpdated();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class CachedEarlyEvents {
        public long eventTime;
        public List<UsageEvents.Event> events;
        public long searchBeginTime;

        private CachedEarlyEvents() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserUsageStatsService(Context context, int userId, File usageStatsDir, StatsUpdatedListener listener) {
        this.mContext = context;
        this.mDatabase = new UsageStatsDatabase(usageStatsDir);
        this.mListener = listener;
        this.mLogPrefix = "User[" + Integer.toString(userId) + "] ";
        this.mUserId = userId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(long currentTimeMillis, HashMap<String, Long> installedPackages, boolean deleteObsoleteData) {
        IntervalStats[] intervalStatsArr;
        readPackageMappingsLocked(installedPackages, deleteObsoleteData);
        this.mDatabase.init(currentTimeMillis);
        if (this.mDatabase.wasUpgradePerformed()) {
            this.mDatabase.prunePackagesDataOnUpgrade(installedPackages);
        }
        int nullCount = 0;
        int i = 0;
        while (true) {
            intervalStatsArr = this.mCurrentStats;
            if (i >= intervalStatsArr.length) {
                break;
            }
            intervalStatsArr[i] = this.mDatabase.getLatestUsageStats(i);
            if (this.mCurrentStats[i] == null) {
                nullCount++;
            }
            i++;
        }
        if (nullCount > 0) {
            if (nullCount != intervalStatsArr.length) {
                Slog.w(TAG, this.mLogPrefix + "Some stats have no latest available");
            }
            loadActiveStats(currentTimeMillis);
        } else {
            updateRolloverDeadline();
        }
        IntervalStats currentDailyStats = this.mCurrentStats[0];
        if (currentDailyStats != null) {
            UsageEvents.Event shutdownEvent = new UsageEvents.Event(26, Math.max(currentDailyStats.lastTimeSaved, currentDailyStats.endTime));
            shutdownEvent.mPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
            currentDailyStats.addEvent(shutdownEvent);
            UsageEvents.Event startupEvent = new UsageEvents.Event(27, System.currentTimeMillis());
            startupEvent.mPackage = PackageManagerService.PLATFORM_PACKAGE_NAME;
            currentDailyStats.addEvent(startupEvent);
        }
        if (this.mDatabase.isNewUpdate()) {
            notifyNewUpdate();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void userStopped() {
        persistActiveStats();
        this.mCachedEarlyEvents.clear();
    }

    int onPackageRemoved(String packageName, long timeRemoved) {
        for (int i = this.mCachedEarlyEvents.numMaps() - 1; i >= 0; i--) {
            int eventType = this.mCachedEarlyEvents.keyAt(i);
            this.mCachedEarlyEvents.delete(eventType, packageName);
        }
        return this.mDatabase.onPackageRemoved(packageName, timeRemoved);
    }

    private void readPackageMappingsLocked(HashMap<String, Long> installedPackages, boolean deleteObsoleteData) {
        this.mDatabase.readMappingsLocked();
        if (this.mUserId != 0 && deleteObsoleteData) {
            updatePackageMappingsLocked(installedPackages);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean updatePackageMappingsLocked(HashMap<String, Long> installedPackages) {
        if (ArrayUtils.isEmpty(installedPackages)) {
            return true;
        }
        long timeNow = System.currentTimeMillis();
        ArrayList<String> removedPackages = new ArrayList<>();
        for (int i = this.mDatabase.mPackagesTokenData.packagesToTokensMap.size() - 1; i >= 0; i--) {
            String packageName = this.mDatabase.mPackagesTokenData.packagesToTokensMap.keyAt(i);
            if (!installedPackages.containsKey(packageName)) {
                removedPackages.add(packageName);
            }
        }
        if (removedPackages.isEmpty()) {
            return true;
        }
        for (int i2 = removedPackages.size() - 1; i2 >= 0; i2--) {
            this.mDatabase.mPackagesTokenData.removePackage(removedPackages.get(i2), timeNow);
        }
        try {
            this.mDatabase.writeMappingsLocked();
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Unable to write updated package mappings file on service initialization.");
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean pruneUninstalledPackagesData() {
        return this.mDatabase.pruneUninstalledPackagesData();
    }

    private void onTimeChanged(long oldTime, long newTime) {
        this.mCachedEarlyEvents.clear();
        persistActiveStats();
        this.mDatabase.onTimeChanged(newTime - oldTime);
        loadActiveStats(newTime);
    }

    private long checkAndGetTimeLocked() {
        long actualSystemTime = System.currentTimeMillis();
        if (!UsageStatsService.ENABLE_TIME_CHANGE_CORRECTION) {
            return actualSystemTime;
        }
        long actualRealtime = SystemClock.elapsedRealtime();
        long expectedSystemTime = (actualRealtime - this.mRealTimeSnapshot) + this.mSystemTimeSnapshot;
        long diffSystemTime = actualSystemTime - expectedSystemTime;
        if (Math.abs(diffSystemTime) > 2000) {
            Slog.i(TAG, this.mLogPrefix + "Time changed in by " + (diffSystemTime / 1000) + " seconds");
            onTimeChanged(expectedSystemTime, actualSystemTime);
            this.mRealTimeSnapshot = actualRealtime;
            this.mSystemTimeSnapshot = actualSystemTime;
        }
        return actualSystemTime;
    }

    private void convertToSystemTimeLocked(UsageEvents.Event event) {
        event.mTimeStamp = Math.max(0L, event.mTimeStamp - this.mRealTimeSnapshot) + this.mSystemTimeSnapshot;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportEvent(UsageEvents.Event event) {
        if (event.mEventType != 7 && event.mEventType != 31) {
            checkAndGetTimeLocked();
            convertToSystemTimeLocked(event);
        }
        if (event.mTimeStamp >= this.mDailyExpiryDate.getTimeInMillis()) {
            rolloverStats(event.mTimeStamp);
        }
        int i = 0;
        IntervalStats currentDailyStats = this.mCurrentStats[0];
        Configuration newFullConfig = event.mConfiguration;
        if (event.mEventType == 5 && currentDailyStats.activeConfiguration != null) {
            event.mConfiguration = Configuration.generateDelta(currentDailyStats.activeConfiguration, newFullConfig);
        }
        if (event.mEventType != 6 && event.mEventType != 24 && event.mEventType != 25 && event.mEventType != 26 && event.mEventType != 31) {
            currentDailyStats.addEvent(event);
        }
        boolean incrementAppLaunch = false;
        if (event.mEventType == 1) {
            if (event.mPackage != null && !event.mPackage.equals(this.mLastBackgroundedPackage)) {
                incrementAppLaunch = true;
            }
        } else if (event.mEventType == 2 && event.mPackage != null) {
            this.mLastBackgroundedPackage = event.mPackage;
        }
        IntervalStats[] intervalStatsArr = this.mCurrentStats;
        int length = intervalStatsArr.length;
        int i2 = 0;
        while (i2 < length) {
            IntervalStats stats = intervalStatsArr[i2];
            switch (event.mEventType) {
                case 5:
                    stats.updateConfigurationStats(newFullConfig, event.mTimeStamp);
                    break;
                case 9:
                    stats.updateChooserCounts(event.mPackage, event.mContentType, event.mAction);
                    String[] annotations = event.mContentAnnotations;
                    if (annotations != null) {
                        int length2 = annotations.length;
                        for (int i3 = i; i3 < length2; i3++) {
                            String annotation = annotations[i3];
                            stats.updateChooserCounts(event.mPackage, annotation, event.mAction);
                        }
                        break;
                    } else {
                        break;
                    }
                case 15:
                    stats.updateScreenInteractive(event.mTimeStamp);
                    break;
                case 16:
                    stats.updateScreenNonInteractive(event.mTimeStamp);
                    break;
                case 17:
                    stats.updateKeyguardShown(event.mTimeStamp);
                    break;
                case 18:
                    stats.updateKeyguardHidden(event.mTimeStamp);
                    break;
                default:
                    stats.update(event.mPackage, event.getClassName(), event.mTimeStamp, event.mEventType, event.mInstanceId);
                    if (incrementAppLaunch) {
                        stats.incrementAppLaunchCount(event.mPackage);
                        break;
                    } else {
                        break;
                    }
            }
            i2++;
            i = 0;
        }
        notifyStatsChanged();
    }

    private static boolean validRange(long currentTime, long beginTime, long endTime) {
        return beginTime <= currentTime && beginTime < endTime;
    }

    private <T> List<T> queryStats(int intervalType, long beginTime, long endTime, UsageStatsDatabase.StatCombiner<T> combiner) {
        int intervalType2;
        if (intervalType != 4) {
            intervalType2 = intervalType;
        } else {
            int intervalType3 = this.mDatabase.findBestFitBucket(beginTime, endTime);
            if (intervalType3 >= 0) {
                intervalType2 = intervalType3;
            } else {
                intervalType2 = 0;
            }
        }
        if (intervalType2 >= 0) {
            IntervalStats[] intervalStatsArr = this.mCurrentStats;
            if (intervalType2 < intervalStatsArr.length) {
                IntervalStats currentStats = intervalStatsArr[intervalType2];
                if (beginTime >= currentStats.endTime) {
                    return null;
                }
                long truncatedEndTime = Math.min(currentStats.beginTime, endTime);
                List<T> results = this.mDatabase.queryUsageStats(intervalType2, beginTime, truncatedEndTime, combiner);
                if (beginTime < currentStats.endTime && endTime > currentStats.beginTime) {
                    if (results == null) {
                        results = new ArrayList();
                    }
                    this.mDatabase.filterStats(currentStats);
                    combiner.combine(currentStats, true, results);
                }
                return results;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<UsageStats> queryUsageStats(int bucketType, long beginTime, long endTime) {
        if (!validRange(checkAndGetTimeLocked(), beginTime, endTime)) {
            return null;
        }
        return queryStats(bucketType, beginTime, endTime, sUsageStatsCombiner);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ConfigurationStats> queryConfigurationStats(int bucketType, long beginTime, long endTime) {
        if (!validRange(checkAndGetTimeLocked(), beginTime, endTime)) {
            return null;
        }
        return queryStats(bucketType, beginTime, endTime, sConfigStatsCombiner);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<EventStats> queryEventStats(int bucketType, long beginTime, long endTime) {
        if (!validRange(checkAndGetTimeLocked(), beginTime, endTime)) {
            return null;
        }
        return queryStats(bucketType, beginTime, endTime, sEventStatsCombiner);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsageEvents queryEvents(final long beginTime, final long endTime, final int flags) {
        if (validRange(checkAndGetTimeLocked(), beginTime, endTime)) {
            final ArraySet<String> names = new ArraySet<>();
            List<UsageEvents.Event> results = queryStats(0, beginTime, endTime, new UsageStatsDatabase.StatCombiner<UsageEvents.Event>() { // from class: com.android.server.usage.UserUsageStatsService.4
                @Override // com.android.server.usage.UsageStatsDatabase.StatCombiner
                public boolean combine(IntervalStats stats, boolean mutable, List<UsageEvents.Event> accumulatedResult) {
                    int startIndex = stats.events.firstIndexOnOrAfter(beginTime);
                    int size = stats.events.size();
                    for (int i = startIndex; i < size; i++) {
                        UsageEvents.Event event = stats.events.get(i);
                        if (event.mTimeStamp >= endTime) {
                            return false;
                        }
                        int eventType = event.mEventType;
                        if ((eventType != 8 || (flags & 2) != 2) && (eventType != 30 || (flags & 8) != 8)) {
                            if ((eventType == 10 || eventType == 12) && (flags & 4) == 4) {
                                event = event.getObfuscatedNotificationEvent();
                            }
                            if ((flags & 1) == 1) {
                                event = event.getObfuscatedIfInstantApp();
                            }
                            if (event.mPackage != null) {
                                names.add(event.mPackage);
                            }
                            if (event.mClass != null) {
                                names.add(event.mClass);
                            }
                            if (event.mTaskRootPackage != null) {
                                names.add(event.mTaskRootPackage);
                            }
                            if (event.mTaskRootClass != null) {
                                names.add(event.mTaskRootClass);
                            }
                            accumulatedResult.add(event);
                        }
                    }
                    return true;
                }
            });
            if (results == null || results.isEmpty()) {
                return null;
            }
            String[] table = (String[]) names.toArray(new String[names.size()]);
            Arrays.sort(table);
            return new UsageEvents(results, table, true);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsageEvents queryEarliestAppEvents(final long beginTime, final long endTime, final int eventType) {
        if (validRange(checkAndGetTimeLocked(), beginTime, endTime)) {
            final ArraySet<String> names = new ArraySet<>();
            final ArraySet<String> eventSuccess = new ArraySet<>();
            List<UsageEvents.Event> results = queryStats(0, beginTime, endTime, new UsageStatsDatabase.StatCombiner() { // from class: com.android.server.usage.UserUsageStatsService$$ExternalSyntheticLambda1
                @Override // com.android.server.usage.UsageStatsDatabase.StatCombiner
                public final boolean combine(IntervalStats intervalStats, boolean z, List list) {
                    return UserUsageStatsService.lambda$queryEarliestAppEvents$0(beginTime, endTime, eventSuccess, names, eventType, intervalStats, z, list);
                }
            });
            if (results == null || results.isEmpty()) {
                return null;
            }
            String[] table = (String[]) names.toArray(new String[names.size()]);
            Arrays.sort(table);
            return new UsageEvents(results, table, false);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$queryEarliestAppEvents$0(long beginTime, long endTime, ArraySet eventSuccess, ArraySet names, int eventType, IntervalStats stats, boolean mutable, List accumulatedResult) {
        int startIndex = stats.events.firstIndexOnOrAfter(beginTime);
        int size = stats.events.size();
        for (int i = startIndex; i < size; i++) {
            UsageEvents.Event event = stats.events.get(i);
            if (event.getTimeStamp() >= endTime) {
                return false;
            }
            if (event.getPackageName() != null && !eventSuccess.contains(event.getPackageName())) {
                boolean firstEvent = names.add(event.getPackageName());
                if (event.getEventType() == eventType) {
                    accumulatedResult.add(event);
                    eventSuccess.add(event.getPackageName());
                } else if (firstEvent) {
                    accumulatedResult.add(event);
                }
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsageEvents queryEventsForPackage(final long beginTime, final long endTime, final String packageName, final boolean includeTaskRoot) {
        if (validRange(checkAndGetTimeLocked(), beginTime, endTime)) {
            final ArraySet<String> names = new ArraySet<>();
            names.add(packageName);
            List<UsageEvents.Event> results = queryStats(0, beginTime, endTime, new UsageStatsDatabase.StatCombiner() { // from class: com.android.server.usage.UserUsageStatsService$$ExternalSyntheticLambda2
                @Override // com.android.server.usage.UsageStatsDatabase.StatCombiner
                public final boolean combine(IntervalStats intervalStats, boolean z, List list) {
                    return UserUsageStatsService.lambda$queryEventsForPackage$1(beginTime, endTime, packageName, names, includeTaskRoot, intervalStats, z, list);
                }
            });
            if (results != null && !results.isEmpty()) {
                String[] table = (String[]) names.toArray(new String[names.size()]);
                Arrays.sort(table);
                return new UsageEvents(results, table, includeTaskRoot);
            }
            return null;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$queryEventsForPackage$1(long beginTime, long endTime, String packageName, ArraySet names, boolean includeTaskRoot, IntervalStats stats, boolean mutable, List accumulatedResult) {
        int startIndex = stats.events.firstIndexOnOrAfter(beginTime);
        int size = stats.events.size();
        for (int i = startIndex; i < size; i++) {
            UsageEvents.Event event = stats.events.get(i);
            if (event.mTimeStamp >= endTime) {
                return false;
            }
            if (packageName.equals(event.mPackage)) {
                if (event.mClass != null) {
                    names.add(event.mClass);
                }
                if (includeTaskRoot && event.mTaskRootPackage != null) {
                    names.add(event.mTaskRootPackage);
                }
                if (includeTaskRoot && event.mTaskRootClass != null) {
                    names.add(event.mTaskRootClass);
                }
                accumulatedResult.add(event);
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UsageEvents queryEarliestEventsForPackage(long beginTime, final long endTime, final String packageName, final int eventType) {
        boolean z;
        CachedEarlyEvents cachedEvents;
        long beginTime2;
        long currentTime = checkAndGetTimeLocked();
        if (validRange(currentTime, beginTime, endTime)) {
            CachedEarlyEvents cachedEvents2 = (CachedEarlyEvents) this.mCachedEarlyEvents.get(eventType, packageName);
            if (cachedEvents2 != null) {
                if (cachedEvents2.searchBeginTime > beginTime || beginTime > cachedEvents2.eventTime) {
                    z = false;
                    cachedEvents2.searchBeginTime = beginTime;
                    cachedEvents = cachedEvents2;
                    beginTime2 = beginTime;
                } else {
                    int numEvents = cachedEvents2.events == null ? 0 : cachedEvents2.events.size();
                    if ((numEvents == 0 || cachedEvents2.events.get(numEvents - 1).getEventType() != eventType) && cachedEvents2.eventTime < endTime) {
                        beginTime2 = Math.min(currentTime, cachedEvents2.eventTime);
                        z = false;
                        cachedEvents = cachedEvents2;
                    } else if (cachedEvents2.eventTime > endTime || cachedEvents2.events == null) {
                        return null;
                    } else {
                        return new UsageEvents(cachedEvents2.events, new String[]{packageName}, false);
                    }
                }
            } else {
                z = false;
                CachedEarlyEvents cachedEvents3 = new CachedEarlyEvents();
                cachedEvents3.searchBeginTime = beginTime;
                this.mCachedEarlyEvents.add(eventType, packageName, cachedEvents3);
                cachedEvents = cachedEvents3;
                beginTime2 = beginTime;
            }
            boolean z2 = z;
            final long finalBeginTime = beginTime2;
            CachedEarlyEvents cachedEvents4 = cachedEvents;
            List<UsageEvents.Event> results = queryStats(0, beginTime2, endTime, new UsageStatsDatabase.StatCombiner() { // from class: com.android.server.usage.UserUsageStatsService$$ExternalSyntheticLambda0
                @Override // com.android.server.usage.UsageStatsDatabase.StatCombiner
                public final boolean combine(IntervalStats intervalStats, boolean z3, List list) {
                    return UserUsageStatsService.lambda$queryEarliestEventsForPackage$2(finalBeginTime, endTime, packageName, eventType, intervalStats, z3, list);
                }
            });
            if (results != null && !results.isEmpty()) {
                cachedEvents4.eventTime = results.get(results.size() - 1).getTimeStamp();
                cachedEvents4.events = results;
                String[] strArr = new String[1];
                strArr[z2 ? 1 : 0] = packageName;
                return new UsageEvents(results, strArr, z2);
            }
            cachedEvents4.eventTime = Math.min(currentTime, endTime);
            cachedEvents4.events = null;
            return null;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$queryEarliestEventsForPackage$2(long finalBeginTime, long endTime, String packageName, int eventType, IntervalStats stats, boolean mutable, List accumulatedResult) {
        int startIndex = stats.events.firstIndexOnOrAfter(finalBeginTime);
        int size = stats.events.size();
        for (int i = startIndex; i < size; i++) {
            UsageEvents.Event event = stats.events.get(i);
            if (event.getTimeStamp() >= endTime) {
                return false;
            }
            if (packageName.equals(event.getPackageName())) {
                if (event.getEventType() == eventType) {
                    accumulatedResult.add(event);
                    return false;
                } else if (accumulatedResult.size() == 0) {
                    accumulatedResult.add(event);
                }
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void persistActiveStats() {
        if (this.mStatsChanged) {
            Slog.i(TAG, this.mLogPrefix + "Flushing usage stats to disk");
            try {
                this.mDatabase.obfuscateCurrentStats(this.mCurrentStats);
                this.mDatabase.writeMappingsLocked();
                int i = 0;
                while (true) {
                    IntervalStats[] intervalStatsArr = this.mCurrentStats;
                    if (i < intervalStatsArr.length) {
                        this.mDatabase.putUsageStats(i, intervalStatsArr[i]);
                        i++;
                    } else {
                        this.mStatsChanged = false;
                        return;
                    }
                }
            } catch (IOException e) {
                Slog.e(TAG, this.mLogPrefix + "Failed to persist active stats", e);
            }
        }
    }

    private void rolloverStats(long currentTimeMillis) {
        int continueCount;
        ArraySet<String> continuePkgs;
        ArrayMap<String, SparseIntArray> continueActivity;
        IntervalStats[] intervalStatsArr;
        int i;
        IntervalStats stat;
        long beginTime;
        int i2;
        IntervalStats[] intervalStatsArr2;
        UsageStats pkgStats;
        int i3;
        int pkgCount;
        IntervalStats stat2;
        long startTime = SystemClock.elapsedRealtime();
        Slog.i(TAG, this.mLogPrefix + "Rolling over usage stats");
        Configuration previousConfig = this.mCurrentStats[0].activeConfiguration;
        ArraySet<String> continuePkgs2 = new ArraySet<>();
        ArrayMap<String, SparseIntArray> continueActivity2 = new ArrayMap<>();
        ArrayMap<String, ArrayMap<String, Integer>> continueForegroundService = new ArrayMap<>();
        IntervalStats[] intervalStatsArr3 = this.mCurrentStats;
        int length = intervalStatsArr3.length;
        int i4 = 0;
        while (i4 < length) {
            IntervalStats stat3 = intervalStatsArr3[i4];
            int pkgCount2 = stat3.packageStats.size();
            int i5 = 0;
            while (i5 < pkgCount2) {
                UsageStats pkgStats2 = stat3.packageStats.valueAt(i5);
                if (pkgStats2.mActivities.size() > 0 || !pkgStats2.mForegroundServices.isEmpty()) {
                    if (pkgStats2.mActivities.size() <= 0) {
                        intervalStatsArr2 = intervalStatsArr3;
                        pkgStats = pkgStats2;
                        i3 = i5;
                        pkgCount = pkgCount2;
                        stat2 = stat3;
                    } else {
                        intervalStatsArr2 = intervalStatsArr3;
                        continueActivity2.put(pkgStats2.mPackageName, pkgStats2.mActivities);
                        pkgStats = pkgStats2;
                        i3 = i5;
                        pkgCount = pkgCount2;
                        stat2 = stat3;
                        stat3.update(pkgStats2.mPackageName, null, this.mDailyExpiryDate.getTimeInMillis() - 1, 3, 0);
                    }
                    UsageStats pkgStats3 = pkgStats;
                    if (!pkgStats3.mForegroundServices.isEmpty()) {
                        continueForegroundService.put(pkgStats3.mPackageName, pkgStats3.mForegroundServices);
                        stat2.update(pkgStats3.mPackageName, null, this.mDailyExpiryDate.getTimeInMillis() - 1, 22, 0);
                    }
                    continuePkgs2.add(pkgStats3.mPackageName);
                    notifyStatsChanged();
                } else {
                    intervalStatsArr2 = intervalStatsArr3;
                    i3 = i5;
                    pkgCount = pkgCount2;
                    stat2 = stat3;
                }
                i5 = i3 + 1;
                pkgCount2 = pkgCount;
                intervalStatsArr3 = intervalStatsArr2;
                stat3 = stat2;
            }
            IntervalStats stat4 = stat3;
            stat4.updateConfigurationStats(null, this.mDailyExpiryDate.getTimeInMillis() - 1);
            stat4.commitTime(this.mDailyExpiryDate.getTimeInMillis() - 1);
            i4++;
            intervalStatsArr3 = intervalStatsArr3;
        }
        persistActiveStats();
        this.mDatabase.prune(currentTimeMillis);
        loadActiveStats(currentTimeMillis);
        int continueCount2 = continuePkgs2.size();
        int i6 = 0;
        while (i6 < continueCount2) {
            String pkgName = continuePkgs2.valueAt(i6);
            long beginTime2 = this.mCurrentStats[0].beginTime;
            IntervalStats[] intervalStatsArr4 = this.mCurrentStats;
            long beginTime3 = beginTime2;
            int i7 = 0;
            for (int length2 = intervalStatsArr4.length; i7 < length2; length2 = i2) {
                int i8 = length2;
                IntervalStats stat5 = intervalStatsArr4[i7];
                if (!continueActivity2.containsKey(pkgName)) {
                    continueCount = continueCount2;
                    continuePkgs = continuePkgs2;
                    continueActivity = continueActivity2;
                    intervalStatsArr = intervalStatsArr4;
                    i = i7;
                    stat = stat5;
                    beginTime = beginTime3;
                    i2 = i8;
                } else {
                    continueCount = continueCount2;
                    SparseIntArray eventMap = continueActivity2.get(pkgName);
                    continuePkgs = continuePkgs2;
                    int size = eventMap.size();
                    continueActivity = continueActivity2;
                    int j = 0;
                    while (j < size) {
                        stat5.update(pkgName, null, beginTime3, eventMap.valueAt(j), eventMap.keyAt(j));
                        j++;
                        intervalStatsArr4 = intervalStatsArr4;
                        i7 = i7;
                        i8 = i8;
                        stat5 = stat5;
                    }
                    intervalStatsArr = intervalStatsArr4;
                    i = i7;
                    stat = stat5;
                    beginTime = beginTime3;
                    i2 = i8;
                }
                if (continueForegroundService.containsKey(pkgName)) {
                    ArrayMap<String, Integer> eventMap2 = continueForegroundService.get(pkgName);
                    int size2 = eventMap2.size();
                    for (int j2 = 0; j2 < size2; j2++) {
                        stat.update(pkgName, eventMap2.keyAt(j2), beginTime, eventMap2.valueAt(j2).intValue(), 0);
                    }
                }
                long beginTime4 = beginTime;
                stat.updateConfigurationStats(previousConfig, beginTime4);
                notifyStatsChanged();
                i7 = i + 1;
                beginTime3 = beginTime4;
                continueCount2 = continueCount;
                continuePkgs2 = continuePkgs;
                continueActivity2 = continueActivity;
                intervalStatsArr4 = intervalStatsArr;
            }
            i6++;
            continueCount2 = continueCount2;
            continuePkgs2 = continuePkgs2;
        }
        persistActiveStats();
        long totalTime = SystemClock.elapsedRealtime() - startTime;
        Slog.i(TAG, this.mLogPrefix + "Rolling over usage stats complete. Took " + totalTime + " milliseconds");
    }

    private void notifyStatsChanged() {
        if (!this.mStatsChanged) {
            this.mStatsChanged = true;
            this.mListener.onStatsUpdated();
        }
    }

    private void notifyNewUpdate() {
        this.mListener.onNewUpdate(this.mUserId);
    }

    private void loadActiveStats(long currentTimeMillis) {
        for (int intervalType = 0; intervalType < this.mCurrentStats.length; intervalType++) {
            IntervalStats stats = this.mDatabase.getLatestUsageStats(intervalType);
            if (stats != null && currentTimeMillis < stats.beginTime + INTERVAL_LENGTH[intervalType]) {
                this.mCurrentStats[intervalType] = stats;
            } else {
                this.mCurrentStats[intervalType] = new IntervalStats();
                this.mCurrentStats[intervalType].beginTime = currentTimeMillis;
                this.mCurrentStats[intervalType].endTime = 1 + currentTimeMillis;
            }
        }
        this.mStatsChanged = false;
        updateRolloverDeadline();
        this.mListener.onStatsReloaded();
    }

    private void updateRolloverDeadline() {
        this.mDailyExpiryDate.setTimeInMillis(this.mCurrentStats[0].beginTime);
        this.mDailyExpiryDate.addDays(1);
        Slog.i(TAG, this.mLogPrefix + "Rollover scheduled @ " + sDateFormat.format(Long.valueOf(this.mDailyExpiryDate.getTimeInMillis())) + "(" + this.mDailyExpiryDate.getTimeInMillis() + ")");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkin(final IndentingPrintWriter pw) {
        this.mDatabase.checkinDailyFiles(new UsageStatsDatabase.CheckinAction() { // from class: com.android.server.usage.UserUsageStatsService.5
            @Override // com.android.server.usage.UsageStatsDatabase.CheckinAction
            public boolean checkin(IntervalStats stats) {
                UserUsageStatsService.this.printIntervalStats(pw, stats, false, false, null);
                return true;
            }
        });
    }

    void dump(IndentingPrintWriter pw, List<String> pkgs) {
        dump(pw, pkgs, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw, List<String> pkgs, boolean compact) {
        printLast24HrEvents(pw, !compact, pkgs);
        for (int interval = 0; interval < this.mCurrentStats.length; interval++) {
            pw.print("In-memory ");
            pw.print(intervalToString(interval));
            pw.println(" stats");
            printIntervalStats(pw, this.mCurrentStats[interval], !compact, true, pkgs);
        }
        if (CollectionUtils.isEmpty(pkgs)) {
            this.mDatabase.dump(pw, compact);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDatabaseInfo(IndentingPrintWriter ipw) {
        this.mDatabase.dump(ipw, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpMappings(IndentingPrintWriter ipw) {
        this.mDatabase.dumpMappings(ipw);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpFile(IndentingPrintWriter ipw, String[] args) {
        int interval;
        if (args == null || args.length == 0) {
            int numIntervals = this.mDatabase.mSortedStatFiles.length;
            for (int interval2 = 0; interval2 < numIntervals; interval2++) {
                ipw.println("interval=" + intervalToString(interval2));
                ipw.increaseIndent();
                dumpFileDetailsForInterval(ipw, interval2);
                ipw.decreaseIndent();
            }
            return;
        }
        try {
            int intervalValue = stringToInterval(args[0]);
            if (intervalValue == -1) {
                interval = Integer.valueOf(args[0]).intValue();
            } else {
                interval = intervalValue;
            }
            if (interval < 0 || interval >= this.mDatabase.mSortedStatFiles.length) {
                ipw.println("the specified interval does not exist.");
            } else if (args.length == 1) {
                dumpFileDetailsForInterval(ipw, interval);
            } else {
                try {
                    long filename = Long.valueOf(args[1]).longValue();
                    IntervalStats stats = this.mDatabase.readIntervalStatsForFile(interval, filename);
                    if (stats == null) {
                        ipw.println("the specified filename does not exist.");
                    } else {
                        dumpFileDetails(ipw, stats, Long.valueOf(args[1]).longValue());
                    }
                } catch (NumberFormatException e) {
                    ipw.println("invalid filename specified.");
                }
            }
        } catch (NumberFormatException e2) {
            ipw.println("invalid interval specified.");
        }
    }

    private void dumpFileDetailsForInterval(IndentingPrintWriter ipw, int interval) {
        TimeSparseArray<AtomicFile> files = this.mDatabase.mSortedStatFiles[interval];
        int numFiles = files.size();
        for (int i = 0; i < numFiles; i++) {
            long filename = files.keyAt(i);
            IntervalStats stats = this.mDatabase.readIntervalStatsForFile(interval, filename);
            dumpFileDetails(ipw, stats, filename);
            ipw.println();
        }
    }

    private void dumpFileDetails(IndentingPrintWriter ipw, IntervalStats stats, long filename) {
        ipw.println("file=" + filename);
        ipw.increaseIndent();
        printIntervalStats(ipw, stats, false, false, null);
        ipw.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String formatDateTime(long dateTime, boolean pretty) {
        if (pretty) {
            return "\"" + sDateFormat.format(Long.valueOf(dateTime)) + "\"";
        }
        return Long.toString(dateTime);
    }

    private String formatElapsedTime(long elapsedTime, boolean pretty) {
        if (pretty) {
            return "\"" + DateUtils.formatElapsedTime(elapsedTime / 1000) + "\"";
        }
        return Long.toString(elapsedTime);
    }

    void printEvent(IndentingPrintWriter pw, UsageEvents.Event event, boolean prettyDates) {
        pw.printPair("time", formatDateTime(event.mTimeStamp, prettyDates));
        pw.printPair(DatabaseHelper.SoundModelContract.KEY_TYPE, eventToString(event.mEventType));
        pw.printPair("package", event.mPackage);
        if (event.mClass != null) {
            pw.printPair("class", event.mClass);
        }
        if (event.mConfiguration != null) {
            pw.printPair("config", Configuration.resourceQualifierString(event.mConfiguration));
        }
        if (event.mShortcutId != null) {
            pw.printPair("shortcutId", event.mShortcutId);
        }
        if (event.mEventType == 11) {
            pw.printPair("standbyBucket", Integer.valueOf(event.getAppStandbyBucket()));
            pw.printPair(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY, UsageStatsManager.reasonToString(event.getStandbyReason()));
        } else if (event.mEventType == 1 || event.mEventType == 2 || event.mEventType == 23) {
            pw.printPair("instanceId", Integer.valueOf(event.getInstanceId()));
        }
        if (event.getTaskRootPackageName() != null) {
            pw.printPair("taskRootPackage", event.getTaskRootPackageName());
        }
        if (event.getTaskRootClassName() != null) {
            pw.printPair("taskRootClass", event.getTaskRootClassName());
        }
        if (event.mNotificationChannelId != null) {
            pw.printPair("channelId", event.mNotificationChannelId);
        }
        pw.printHexPair("flags", event.mFlags);
        pw.println();
    }

    void printLast24HrEvents(IndentingPrintWriter pw, boolean prettyDates, final List<String> pkgs) {
        final long endTime = System.currentTimeMillis();
        UnixCalendar yesterday = new UnixCalendar(endTime);
        yesterday.addDays(-1);
        final long beginTime = yesterday.getTimeInMillis();
        List<UsageEvents.Event> events = queryStats(0, beginTime, endTime, new UsageStatsDatabase.StatCombiner<UsageEvents.Event>() { // from class: com.android.server.usage.UserUsageStatsService.6
            @Override // com.android.server.usage.UsageStatsDatabase.StatCombiner
            public boolean combine(IntervalStats stats, boolean mutable, List<UsageEvents.Event> accumulatedResult) {
                int startIndex = stats.events.firstIndexOnOrAfter(beginTime);
                int size = stats.events.size();
                for (int i = startIndex; i < size; i++) {
                    if (stats.events.get(i).mTimeStamp >= endTime) {
                        return false;
                    }
                    UsageEvents.Event event = stats.events.get(i);
                    if (CollectionUtils.isEmpty(pkgs) || pkgs.contains(event.mPackage)) {
                        accumulatedResult.add(event);
                    }
                }
                return true;
            }
        });
        pw.print("Last 24 hour events (");
        if (prettyDates) {
            pw.printPair("timeRange", "\"" + DateUtils.formatDateRange(this.mContext, beginTime, endTime, sDateFormatFlags) + "\"");
        } else {
            pw.printPair("beginTime", Long.valueOf(beginTime));
            pw.printPair("endTime", Long.valueOf(endTime));
        }
        pw.println(")");
        if (events != null) {
            pw.increaseIndent();
            for (UsageEvents.Event event : events) {
                printEvent(pw, event, prettyDates);
            }
            pw.decreaseIndent();
        }
    }

    void printEventAggregation(IndentingPrintWriter pw, String label, IntervalStats.EventTracker tracker, boolean prettyDates) {
        if (tracker.count != 0 || tracker.duration != 0) {
            pw.print(label);
            pw.print(": ");
            pw.print(tracker.count);
            pw.print("x for ");
            pw.print(formatElapsedTime(tracker.duration, prettyDates));
            if (tracker.curStartTime != 0) {
                pw.print(" (now running, started at ");
                formatDateTime(tracker.curStartTime, prettyDates);
                pw.print(")");
            }
            pw.println();
        }
    }

    void printIntervalStats(IndentingPrintWriter pw, IntervalStats stats, boolean prettyDates, boolean skipEvents, List<String> pkgs) {
        String str;
        String str2;
        Iterator<UsageStats> it;
        if (prettyDates) {
            pw.printPair("timeRange", "\"" + DateUtils.formatDateRange(this.mContext, stats.beginTime, stats.endTime, sDateFormatFlags) + "\"");
        } else {
            pw.printPair("beginTime", Long.valueOf(stats.beginTime));
            pw.printPair("endTime", Long.valueOf(stats.endTime));
        }
        pw.println();
        pw.increaseIndent();
        pw.println("packages");
        pw.increaseIndent();
        ArrayMap<String, UsageStats> pkgStats = stats.packageStats;
        int pkgCount = pkgStats.size();
        int i = 0;
        while (true) {
            str = "package";
            if (i >= pkgCount) {
                break;
            }
            UsageStats usageStats = pkgStats.valueAt(i);
            if (CollectionUtils.isEmpty(pkgs) || pkgs.contains(usageStats.mPackageName)) {
                pw.printPair("package", usageStats.mPackageName);
                pw.printPair("totalTimeUsed", formatElapsedTime(usageStats.mTotalTimeInForeground, prettyDates));
                pw.printPair("lastTimeUsed", formatDateTime(usageStats.mLastTimeUsed, prettyDates));
                pw.printPair("totalTimeVisible", formatElapsedTime(usageStats.mTotalTimeVisible, prettyDates));
                pw.printPair("lastTimeVisible", formatDateTime(usageStats.mLastTimeVisible, prettyDates));
                pw.printPair("lastTimeComponentUsed", formatDateTime(usageStats.mLastTimeComponentUsed, prettyDates));
                pw.printPair("totalTimeFS", formatElapsedTime(usageStats.mTotalTimeForegroundServiceUsed, prettyDates));
                pw.printPair("lastTimeFS", formatDateTime(usageStats.mLastTimeForegroundServiceUsed, prettyDates));
                pw.printPair("appLaunchCount", Integer.valueOf(usageStats.mAppLaunchCount));
                pw.println();
            }
            i++;
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("ChooserCounts");
        pw.increaseIndent();
        Iterator<UsageStats> it2 = pkgStats.values().iterator();
        while (it2.hasNext()) {
            UsageStats usageStats2 = it2.next();
            if (CollectionUtils.isEmpty(pkgs) || pkgs.contains(usageStats2.mPackageName)) {
                pw.printPair(str, usageStats2.mPackageName);
                if (usageStats2.mChooserCounts != null) {
                    int chooserCountSize = usageStats2.mChooserCounts.size();
                    int i2 = 0;
                    while (i2 < chooserCountSize) {
                        String action = (String) usageStats2.mChooserCounts.keyAt(i2);
                        ArrayMap<String, Integer> counts = (ArrayMap) usageStats2.mChooserCounts.valueAt(i2);
                        int annotationSize = counts.size();
                        ArrayMap<String, UsageStats> pkgStats2 = pkgStats;
                        int j = 0;
                        while (j < annotationSize) {
                            int pkgCount2 = pkgCount;
                            String key = counts.keyAt(j);
                            int count = counts.valueAt(j).intValue();
                            if (count == 0) {
                                str2 = str;
                                it = it2;
                            } else {
                                str2 = str;
                                it = it2;
                                pw.printPair("ChooserCounts", action + ":" + key + " is " + Integer.toString(count));
                                pw.println();
                            }
                            j++;
                            pkgCount = pkgCount2;
                            str = str2;
                            it2 = it;
                        }
                        i2++;
                        pkgStats = pkgStats2;
                    }
                }
                pw.println();
                pkgStats = pkgStats;
                pkgCount = pkgCount;
                str = str;
                it2 = it2;
            }
        }
        pw.decreaseIndent();
        if (CollectionUtils.isEmpty(pkgs)) {
            pw.println("configurations");
            pw.increaseIndent();
            ArrayMap<Configuration, ConfigurationStats> configStats = stats.configurations;
            int configCount = configStats.size();
            for (int i3 = 0; i3 < configCount; i3++) {
                ConfigurationStats config = configStats.valueAt(i3);
                pw.printPair("config", Configuration.resourceQualifierString(config.mConfiguration));
                pw.printPair("totalTime", formatElapsedTime(config.mTotalTimeActive, prettyDates));
                pw.printPair("lastTime", formatDateTime(config.mLastTimeActive, prettyDates));
                pw.printPair(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.valueOf(config.mActivationCount));
                pw.println();
            }
            pw.decreaseIndent();
            pw.println("event aggregations");
            pw.increaseIndent();
            printEventAggregation(pw, "screen-interactive", stats.interactiveTracker, prettyDates);
            printEventAggregation(pw, "screen-non-interactive", stats.nonInteractiveTracker, prettyDates);
            printEventAggregation(pw, "keyguard-shown", stats.keyguardShownTracker, prettyDates);
            printEventAggregation(pw, "keyguard-hidden", stats.keyguardHiddenTracker, prettyDates);
            pw.decreaseIndent();
        }
        if (!skipEvents) {
            pw.println("events");
            pw.increaseIndent();
            EventList events = stats.events;
            int eventCount = events != null ? events.size() : 0;
            for (int i4 = 0; i4 < eventCount; i4++) {
                UsageEvents.Event event = events.get(i4);
                if (CollectionUtils.isEmpty(pkgs) || pkgs.contains(event.mPackage)) {
                    printEvent(pw, event, prettyDates);
                }
            }
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }

    public static String intervalToString(int interval) {
        switch (interval) {
            case 0:
                return "daily";
            case 1:
                return "weekly";
            case 2:
                return "monthly";
            case 3:
                return "yearly";
            default:
                return "?";
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static int stringToInterval(String interval) {
        boolean z;
        String lowerCase = interval.toLowerCase();
        switch (lowerCase.hashCode()) {
            case -791707519:
                if (lowerCase.equals("weekly")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -734561654:
                if (lowerCase.equals("yearly")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 95346201:
                if (lowerCase.equals("daily")) {
                    z = false;
                    break;
                }
                z = true;
                break;
            case 1236635661:
                if (lowerCase.equals("monthly")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            default:
                z = true;
                break;
        }
        switch (z) {
            case false:
                return 0;
            case true:
                return 1;
            case true:
                return 2;
            case true:
                return 3;
            default:
                return -1;
        }
    }

    private static String eventToString(int eventType) {
        switch (eventType) {
            case 0:
                return "NONE";
            case 1:
                return "ACTIVITY_RESUMED";
            case 2:
                return "ACTIVITY_PAUSED";
            case 3:
                return "END_OF_DAY";
            case 4:
                return "CONTINUE_PREVIOUS_DAY";
            case 5:
                return "CONFIGURATION_CHANGE";
            case 6:
                return "SYSTEM_INTERACTION";
            case 7:
                return "USER_INTERACTION";
            case 8:
                return "SHORTCUT_INVOCATION";
            case 9:
                return "CHOOSER_ACTION";
            case 10:
                return "NOTIFICATION_SEEN";
            case 11:
                return "STANDBY_BUCKET_CHANGED";
            case 12:
                return "NOTIFICATION_INTERRUPTION";
            case 13:
                return "SLICE_PINNED_PRIV";
            case 14:
                return "SLICE_PINNED";
            case 15:
                return "SCREEN_INTERACTIVE";
            case 16:
                return "SCREEN_NON_INTERACTIVE";
            case 17:
                return "KEYGUARD_SHOWN";
            case 18:
                return "KEYGUARD_HIDDEN";
            case 19:
                return "FOREGROUND_SERVICE_START";
            case 20:
                return "FOREGROUND_SERVICE_STOP";
            case 21:
                return "CONTINUING_FOREGROUND_SERVICE";
            case 22:
                return "ROLLOVER_FOREGROUND_SERVICE";
            case 23:
                return "ACTIVITY_STOPPED";
            case 24:
            case 25:
            default:
                return "UNKNOWN_TYPE_" + eventType;
            case 26:
                return "DEVICE_SHUTDOWN";
            case 27:
                return "DEVICE_STARTUP";
            case 28:
                return "USER_UNLOCKED";
            case 29:
                return "USER_STOPPED";
            case 30:
                return "LOCUS_ID_SET";
            case 31:
                return "APP_COMPONENT_USED";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public byte[] getBackupPayload(String key) {
        checkAndGetTimeLocked();
        persistActiveStats();
        return this.mDatabase.getBackupPayload(key);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void applyRestoredPayload(String key, byte[] payload) {
        checkAndGetTimeLocked();
        this.mDatabase.applyRestoredPayload(key, payload);
    }
}
