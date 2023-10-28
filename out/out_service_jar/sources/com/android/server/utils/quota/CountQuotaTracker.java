package com.android.server.utils.quota;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.IndentingPrintWriter;
import android.util.LongArrayQueue;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usage.UnixCalendar;
import com.android.server.utils.quota.CountQuotaTracker;
import com.android.server.utils.quota.QuotaTracker;
import com.android.server.utils.quota.UptcMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
/* loaded from: classes2.dex */
public class CountQuotaTracker extends QuotaTracker {
    private static final String ALARM_TAG_CLEANUP;
    private static final boolean DEBUG = false;
    private static final int MSG_CLEAN_UP_EVENTS = 1;
    private static final String TAG;
    private final ArrayMap<Category, Long> mCategoryCountWindowSizesMs;
    private Function<Void, ExecutionStats> mCreateExecutionStats;
    private Function<Void, LongArrayQueue> mCreateLongArrayQueue;
    private final DeleteEventTimesFunctor mDeleteOldEventTimesFunctor;
    private final EarliestEventTimeFunctor mEarliestEventTimeFunctor;
    private final AlarmManager.OnAlarmListener mEventCleanupAlarmListener;
    private final UptcMap<LongArrayQueue> mEventTimes;
    private final UptcMap<ExecutionStats> mExecutionStatsCache;
    private final Handler mHandler;
    private final ArrayMap<Category, Integer> mMaxCategoryCounts;
    private long mMaxPeriodMs;
    private long mNextCleanupTimeElapsed;

    @Override // com.android.server.utils.quota.QuotaTracker
    public /* bridge */ /* synthetic */ void clear() {
        super.clear();
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    public /* bridge */ /* synthetic */ void dump(IndentingPrintWriter indentingPrintWriter) {
        super.dump(indentingPrintWriter);
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    public /* bridge */ /* synthetic */ boolean isWithinQuota(int i, String str, String str2) {
        return super.isWithinQuota(i, str, str2);
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    public /* bridge */ /* synthetic */ void registerQuotaChangeListener(QuotaChangeListener quotaChangeListener) {
        super.registerQuotaChangeListener(quotaChangeListener);
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    public /* bridge */ /* synthetic */ void setEnabled(boolean z) {
        super.setEnabled(z);
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    public /* bridge */ /* synthetic */ void setQuotaFree(int i, String str, boolean z) {
        super.setQuotaFree(i, str, z);
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    public /* bridge */ /* synthetic */ void setQuotaFree(boolean z) {
        super.setQuotaFree(z);
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    public /* bridge */ /* synthetic */ void unregisterQuotaChangeListener(QuotaChangeListener quotaChangeListener) {
        super.unregisterQuotaChangeListener(quotaChangeListener);
    }

    static {
        String simpleName = CountQuotaTracker.class.getSimpleName();
        TAG = simpleName;
        ALARM_TAG_CLEANUP = "*" + simpleName + ".cleanup*";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class ExecutionStats {
        public int countInWindow;
        public int countLimit;
        public long expirationTimeElapsed;
        public long inQuotaTimeElapsed;
        public long windowSizeMs;

        ExecutionStats() {
        }

        public String toString() {
            return "expirationTime=" + this.expirationTimeElapsed + ", windowSizeMs=" + this.windowSizeMs + ", countLimit=" + this.countLimit + ", countInWindow=" + this.countInWindow + ", inQuotaTime=" + this.inQuotaTimeElapsed;
        }

        public boolean equals(Object obj) {
            if (obj instanceof ExecutionStats) {
                ExecutionStats other = (ExecutionStats) obj;
                return this.expirationTimeElapsed == other.expirationTimeElapsed && this.windowSizeMs == other.windowSizeMs && this.countLimit == other.countLimit && this.countInWindow == other.countInWindow && this.inQuotaTimeElapsed == other.inQuotaTimeElapsed;
            }
            return false;
        }

        public int hashCode() {
            int result = (0 * 31) + Long.hashCode(this.expirationTimeElapsed);
            return (((((((result * 31) + Long.hashCode(this.windowSizeMs)) * 31) + this.countLimit) * 31) + this.countInWindow) * 31) + Long.hashCode(this.inQuotaTimeElapsed);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-utils-quota-CountQuotaTracker  reason: not valid java name */
    public /* synthetic */ void m7444lambda$new$0$comandroidserverutilsquotaCountQuotaTracker() {
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    public CountQuotaTracker(Context context, Categorizer categorizer) {
        this(context, categorizer, new QuotaTracker.Injector());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CountQuotaTracker(Context context, Categorizer categorizer, QuotaTracker.Injector injector) {
        super(context, categorizer, injector);
        this.mEventTimes = new UptcMap<>();
        this.mExecutionStatsCache = new UptcMap<>();
        this.mNextCleanupTimeElapsed = 0L;
        this.mEventCleanupAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.utils.quota.CountQuotaTracker$$ExternalSyntheticLambda5
            @Override // android.app.AlarmManager.OnAlarmListener
            public final void onAlarm() {
                CountQuotaTracker.this.m7444lambda$new$0$comandroidserverutilsquotaCountQuotaTracker();
            }
        };
        this.mCategoryCountWindowSizesMs = new ArrayMap<>();
        this.mMaxCategoryCounts = new ArrayMap<>();
        this.mMaxPeriodMs = 0L;
        this.mEarliestEventTimeFunctor = new EarliestEventTimeFunctor();
        this.mDeleteOldEventTimesFunctor = new DeleteEventTimesFunctor();
        this.mCreateLongArrayQueue = new Function() { // from class: com.android.server.utils.quota.CountQuotaTracker$$ExternalSyntheticLambda6
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return CountQuotaTracker.lambda$new$4((Void) obj);
            }
        };
        this.mCreateExecutionStats = new Function() { // from class: com.android.server.utils.quota.CountQuotaTracker$$ExternalSyntheticLambda7
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return CountQuotaTracker.lambda$new$5((Void) obj);
            }
        };
        this.mHandler = new CqtHandler(context.getMainLooper());
    }

    public boolean noteEvent(int userId, String packageName, String tag) {
        synchronized (this.mLock) {
            if (isEnabledLocked() && !isQuotaFreeLocked(userId, packageName)) {
                long nowElapsed = this.mInjector.getElapsedRealtime();
                LongArrayQueue times = this.mEventTimes.getOrCreate(userId, packageName, tag, this.mCreateLongArrayQueue);
                times.addLast(nowElapsed);
                ExecutionStats stats = getExecutionStatsLocked(userId, packageName, tag);
                stats.countInWindow++;
                stats.expirationTimeElapsed = Math.min(stats.expirationTimeElapsed, stats.windowSizeMs + nowElapsed);
                if (stats.countInWindow == stats.countLimit) {
                    long windowEdgeElapsed = nowElapsed - stats.windowSizeMs;
                    while (times.size() > 0 && times.peekFirst() < windowEdgeElapsed) {
                        times.removeFirst();
                    }
                    stats.inQuotaTimeElapsed = times.peekFirst() + stats.windowSizeMs;
                    postQuotaStatusChanged(userId, packageName, tag);
                } else if (stats.countLimit > 9 && stats.countInWindow == (stats.countLimit * 4) / 5) {
                    Slog.w(TAG, Uptc.string(userId, packageName, tag) + " has reached 80% of it's count limit of " + stats.countLimit);
                }
                maybeScheduleCleanupAlarmLocked();
                return isWithinQuotaLocked(stats);
            }
            return true;
        }
    }

    public void setCountLimit(Category category, int limit, long timeWindowMs) {
        if (limit < 0 || timeWindowMs < 0) {
            throw new IllegalArgumentException("Limit and window size must be nonnegative.");
        }
        synchronized (this.mLock) {
            Integer oldLimit = this.mMaxCategoryCounts.put(category, Integer.valueOf(limit));
            long newWindowSizeMs = Math.max(20000L, Math.min(timeWindowMs, (long) UnixCalendar.MONTH_IN_MILLIS));
            Long oldWindowSizeMs = this.mCategoryCountWindowSizesMs.put(category, Long.valueOf(newWindowSizeMs));
            if (oldLimit == null || oldWindowSizeMs == null || oldLimit.intValue() != limit || oldWindowSizeMs.longValue() != newWindowSizeMs) {
                this.mDeleteOldEventTimesFunctor.updateMaxPeriod();
                this.mMaxPeriodMs = this.mDeleteOldEventTimesFunctor.mMaxPeriodMs;
                invalidateAllExecutionStatsLocked();
                scheduleQuotaCheck();
            }
        }
    }

    public int getLimit(Category category) {
        int intValue;
        synchronized (this.mLock) {
            Integer limit = this.mMaxCategoryCounts.get(category);
            if (limit == null) {
                throw new IllegalArgumentException("Limit for " + category + " not defined");
            }
            intValue = limit.intValue();
        }
        return intValue;
    }

    public long getWindowSizeMs(Category category) {
        long longValue;
        synchronized (this.mLock) {
            Long limitMs = this.mCategoryCountWindowSizesMs.get(category);
            if (limitMs == null) {
                throw new IllegalArgumentException("Limit for " + category + " not defined");
            }
            longValue = limitMs.longValue();
        }
        return longValue;
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    void dropEverythingLocked() {
        this.mExecutionStatsCache.clear();
        this.mEventTimes.clear();
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    Handler getHandler() {
        return this.mHandler;
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    long getInQuotaTimeElapsedLocked(int userId, String packageName, String tag) {
        return getExecutionStatsLocked(userId, packageName, tag).inQuotaTimeElapsed;
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    void handleRemovedAppLocked(int userId, String packageName) {
        if (packageName == null) {
            Slog.wtf(TAG, "Told app removed but given null package name.");
            return;
        }
        this.mEventTimes.delete(userId, packageName);
        this.mExecutionStatsCache.delete(userId, packageName);
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    void handleRemovedUserLocked(int userId) {
        this.mEventTimes.delete(userId);
        this.mExecutionStatsCache.delete(userId);
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    boolean isWithinQuotaLocked(int userId, String packageName, String tag) {
        if (isEnabledLocked() && !isQuotaFreeLocked(userId, packageName)) {
            return isWithinQuotaLocked(getExecutionStatsLocked(userId, packageName, tag));
        }
        return true;
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    void maybeUpdateAllQuotaStatusLocked() {
        final UptcMap<Boolean> doneMap = new UptcMap<>();
        this.mEventTimes.forEach(new UptcMap.UptcDataConsumer() { // from class: com.android.server.utils.quota.CountQuotaTracker$$ExternalSyntheticLambda8
            @Override // com.android.server.utils.quota.UptcMap.UptcDataConsumer
            public final void accept(int i, String str, String str2, Object obj) {
                CountQuotaTracker.this.m7442xcf963e8e(doneMap, i, str, str2, (LongArrayQueue) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$maybeUpdateAllQuotaStatusLocked$1$com-android-server-utils-quota-CountQuotaTracker  reason: not valid java name */
    public /* synthetic */ void m7442xcf963e8e(UptcMap doneMap, int userId, String packageName, String tag, LongArrayQueue events) {
        if (!doneMap.contains(userId, packageName, tag)) {
            maybeUpdateStatusForUptcLocked(userId, packageName, tag);
            doneMap.add(userId, packageName, tag, Boolean.TRUE);
        }
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    void maybeUpdateQuotaStatus(int userId, String packageName, String tag) {
        synchronized (this.mLock) {
            maybeUpdateStatusForUptcLocked(userId, packageName, tag);
        }
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    void onQuotaFreeChangedLocked(boolean isFree) {
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    void onQuotaFreeChangedLocked(int userId, String packageName, boolean isFree) {
        maybeUpdateStatusForPkgLocked(userId, packageName);
    }

    private boolean isWithinQuotaLocked(ExecutionStats stats) {
        return isUnderCountQuotaLocked(stats);
    }

    private boolean isUnderCountQuotaLocked(ExecutionStats stats) {
        return stats.countInWindow < stats.countLimit;
    }

    ExecutionStats getExecutionStatsLocked(int userId, String packageName, String tag) {
        return getExecutionStatsLocked(userId, packageName, tag, true);
    }

    private ExecutionStats getExecutionStatsLocked(int userId, String packageName, String tag, boolean refreshStatsIfOld) {
        ExecutionStats stats = this.mExecutionStatsCache.getOrCreate(userId, packageName, tag, this.mCreateExecutionStats);
        if (refreshStatsIfOld) {
            Category category = this.mCategorizer.getCategory(userId, packageName, tag);
            long countWindowSizeMs = this.mCategoryCountWindowSizesMs.getOrDefault(category, Long.valueOf((long) JobStatus.NO_LATEST_RUNTIME)).longValue();
            int countLimit = this.mMaxCategoryCounts.getOrDefault(category, Integer.MAX_VALUE).intValue();
            if (stats.expirationTimeElapsed <= this.mInjector.getElapsedRealtime() || stats.windowSizeMs != countWindowSizeMs || stats.countLimit != countLimit) {
                stats.windowSizeMs = countWindowSizeMs;
                stats.countLimit = countLimit;
                updateExecutionStatsLocked(userId, packageName, tag, stats);
            }
        }
        return stats;
    }

    void updateExecutionStatsLocked(int userId, String packageName, String tag, ExecutionStats stats) {
        LongArrayQueue events;
        long emptyTimeMs;
        stats.countInWindow = 0;
        if (stats.countLimit == 0) {
            stats.inQuotaTimeElapsed = JobStatus.NO_LATEST_RUNTIME;
        } else {
            stats.inQuotaTimeElapsed = 0L;
        }
        long nowElapsed = this.mInjector.getElapsedRealtime();
        stats.expirationTimeElapsed = this.mMaxPeriodMs + nowElapsed;
        LongArrayQueue events2 = this.mEventTimes.get(userId, packageName, tag);
        if (events2 == null) {
            return;
        }
        long emptyTimeMs2 = JobStatus.NO_LATEST_RUNTIME - nowElapsed;
        long eventStartWindowElapsed = nowElapsed - stats.windowSizeMs;
        int i = events2.size() - 1;
        while (i >= 0) {
            long eventTimeElapsed = events2.get(i);
            if (eventTimeElapsed < eventStartWindowElapsed) {
                break;
            }
            stats.countInWindow++;
            long emptyTimeMs3 = Math.min(emptyTimeMs2, eventTimeElapsed - eventStartWindowElapsed);
            if (stats.countInWindow < stats.countLimit) {
                events = events2;
                emptyTimeMs = emptyTimeMs3;
            } else {
                events = events2;
                emptyTimeMs = emptyTimeMs3;
                stats.inQuotaTimeElapsed = Math.max(stats.inQuotaTimeElapsed, stats.windowSizeMs + eventTimeElapsed);
            }
            i--;
            events2 = events;
            emptyTimeMs2 = emptyTimeMs;
        }
        stats.expirationTimeElapsed = nowElapsed + emptyTimeMs2;
    }

    private void invalidateAllExecutionStatsLocked() {
        final long nowElapsed = this.mInjector.getElapsedRealtime();
        this.mExecutionStatsCache.forEach(new Consumer() { // from class: com.android.server.utils.quota.CountQuotaTracker$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                CountQuotaTracker.lambda$invalidateAllExecutionStatsLocked$2(nowElapsed, (CountQuotaTracker.ExecutionStats) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$invalidateAllExecutionStatsLocked$2(long nowElapsed, ExecutionStats appStats) {
        if (appStats != null) {
            appStats.expirationTimeElapsed = nowElapsed;
        }
    }

    private void invalidateAllExecutionStatsLocked(int userId, String packageName) {
        ArrayMap<String, ExecutionStats> appStats = this.mExecutionStatsCache.get(userId, packageName);
        if (appStats != null) {
            long nowElapsed = this.mInjector.getElapsedRealtime();
            int numStats = appStats.size();
            for (int i = 0; i < numStats; i++) {
                ExecutionStats stats = appStats.valueAt(i);
                if (stats != null) {
                    stats.expirationTimeElapsed = nowElapsed;
                }
            }
        }
    }

    private void invalidateExecutionStatsLocked(int userId, String packageName, String tag) {
        ExecutionStats stats = this.mExecutionStatsCache.get(userId, packageName, tag);
        if (stats != null) {
            stats.expirationTimeElapsed = this.mInjector.getElapsedRealtime();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class EarliestEventTimeFunctor implements Consumer<LongArrayQueue> {
        long earliestTimeElapsed;

        private EarliestEventTimeFunctor() {
            this.earliestTimeElapsed = JobStatus.NO_LATEST_RUNTIME;
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(LongArrayQueue events) {
            if (events != null && events.size() > 0) {
                this.earliestTimeElapsed = Math.min(this.earliestTimeElapsed, events.get(0));
            }
        }

        void reset() {
            this.earliestTimeElapsed = JobStatus.NO_LATEST_RUNTIME;
        }
    }

    void maybeScheduleCleanupAlarmLocked() {
        if (this.mNextCleanupTimeElapsed > this.mInjector.getElapsedRealtime()) {
            return;
        }
        this.mEarliestEventTimeFunctor.reset();
        this.mEventTimes.forEach(this.mEarliestEventTimeFunctor);
        long earliestEndElapsed = this.mEarliestEventTimeFunctor.earliestTimeElapsed;
        if (earliestEndElapsed == JobStatus.NO_LATEST_RUNTIME) {
            return;
        }
        long nextCleanupElapsed = this.mMaxPeriodMs + earliestEndElapsed;
        if (nextCleanupElapsed - this.mNextCleanupTimeElapsed <= 600000) {
            nextCleanupElapsed += 600000;
        }
        this.mNextCleanupTimeElapsed = nextCleanupElapsed;
        scheduleAlarm(3, nextCleanupElapsed, ALARM_TAG_CLEANUP, this.mEventCleanupAlarmListener);
    }

    private boolean maybeUpdateStatusForPkgLocked(final int userId, final String packageName) {
        final UptcMap<Boolean> done = new UptcMap<>();
        if (this.mEventTimes.contains(userId, packageName)) {
            ArrayMap<String, LongArrayQueue> events = this.mEventTimes.get(userId, packageName);
            if (events == null) {
                Slog.wtf(TAG, "Events map was null even though mEventTimes said it contained " + Uptc.string(userId, packageName, null));
                return false;
            }
            final boolean[] changed = {false};
            events.forEach(new BiConsumer() { // from class: com.android.server.utils.quota.CountQuotaTracker$$ExternalSyntheticLambda1
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    CountQuotaTracker.this.m7443xc21d04a0(done, userId, packageName, changed, (String) obj, (LongArrayQueue) obj2);
                }
            });
            return changed[0];
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$maybeUpdateStatusForPkgLocked$3$com-android-server-utils-quota-CountQuotaTracker  reason: not valid java name */
    public /* synthetic */ void m7443xc21d04a0(UptcMap done, int userId, String packageName, boolean[] changed, String tag, LongArrayQueue eventList) {
        if (!done.contains(userId, packageName, tag)) {
            changed[0] = changed[0] | maybeUpdateStatusForUptcLocked(userId, packageName, tag);
            done.add(userId, packageName, tag, Boolean.TRUE);
        }
    }

    private boolean maybeUpdateStatusForUptcLocked(int userId, String packageName, String tag) {
        boolean newInQuota;
        boolean oldInQuota = isWithinQuotaLocked(getExecutionStatsLocked(userId, packageName, tag, false));
        if (!isEnabledLocked() || isQuotaFreeLocked(userId, packageName)) {
            newInQuota = true;
        } else {
            newInQuota = isWithinQuotaLocked(getExecutionStatsLocked(userId, packageName, tag, true));
        }
        if (!newInQuota) {
            maybeScheduleStartAlarmLocked(userId, packageName, tag);
        } else {
            cancelScheduledStartAlarmLocked(userId, packageName, tag);
        }
        if (oldInQuota == newInQuota) {
            return false;
        }
        postQuotaStatusChanged(userId, packageName, tag);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class DeleteEventTimesFunctor implements Consumer<LongArrayQueue> {
        private long mMaxPeriodMs;

        private DeleteEventTimesFunctor() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.function.Consumer
        public void accept(LongArrayQueue times) {
            if (times != null) {
                while (times.size() > 0 && times.peekFirst() <= CountQuotaTracker.this.mInjector.getElapsedRealtime() - this.mMaxPeriodMs) {
                    times.removeFirst();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateMaxPeriod() {
            long maxPeriodMs = 0;
            for (int i = CountQuotaTracker.this.mCategoryCountWindowSizesMs.size() - 1; i >= 0; i--) {
                maxPeriodMs = Long.max(maxPeriodMs, ((Long) CountQuotaTracker.this.mCategoryCountWindowSizesMs.valueAt(i)).longValue());
            }
            this.mMaxPeriodMs = maxPeriodMs;
        }
    }

    void deleteObsoleteEventsLocked() {
        this.mEventTimes.forEach(this.mDeleteOldEventTimesFunctor);
    }

    /* loaded from: classes2.dex */
    private class CqtHandler extends Handler {
        CqtHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            synchronized (CountQuotaTracker.this.mLock) {
                switch (msg.what) {
                    case 1:
                        CountQuotaTracker.this.deleteObsoleteEventsLocked();
                        CountQuotaTracker.this.maybeScheduleCleanupAlarmLocked();
                        break;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ LongArrayQueue lambda$new$4(Void aVoid) {
        return new LongArrayQueue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ ExecutionStats lambda$new$5(Void aVoid) {
        return new ExecutionStats();
    }

    LongArrayQueue getEvents(int userId, String packageName, String tag) {
        return this.mEventTimes.get(userId, packageName, tag);
    }

    public void dump(final com.android.internal.util.IndentingPrintWriter pw) {
        pw.print(TAG);
        pw.println(":");
        pw.increaseIndent();
        synchronized (this.mLock) {
            super.dump((IndentingPrintWriter) pw);
            pw.println();
            pw.println("Instantaneous events:");
            pw.increaseIndent();
            this.mEventTimes.forEach(new UptcMap.UptcDataConsumer() { // from class: com.android.server.utils.quota.CountQuotaTracker$$ExternalSyntheticLambda2
                @Override // com.android.server.utils.quota.UptcMap.UptcDataConsumer
                public final void accept(int i, String str, String str2, Object obj) {
                    CountQuotaTracker.lambda$dump$6(pw, i, str, str2, (LongArrayQueue) obj);
                }
            });
            pw.decreaseIndent();
            pw.println();
            pw.println("Cached execution stats:");
            pw.increaseIndent();
            this.mExecutionStatsCache.forEach(new UptcMap.UptcDataConsumer() { // from class: com.android.server.utils.quota.CountQuotaTracker$$ExternalSyntheticLambda3
                @Override // com.android.server.utils.quota.UptcMap.UptcDataConsumer
                public final void accept(int i, String str, String str2, Object obj) {
                    CountQuotaTracker.lambda$dump$7(pw, i, str, str2, (CountQuotaTracker.ExecutionStats) obj);
                }
            });
            pw.decreaseIndent();
            pw.println();
            pw.println("Limits:");
            pw.increaseIndent();
            int numCategories = this.mCategoryCountWindowSizesMs.size();
            for (int i = 0; i < numCategories; i++) {
                Category category = this.mCategoryCountWindowSizesMs.keyAt(i);
                pw.print(category);
                pw.print(": ");
                pw.print(this.mMaxCategoryCounts.get(category));
                pw.print(" events in ");
                pw.println(TimeUtils.formatDuration(this.mCategoryCountWindowSizesMs.get(category).longValue()));
            }
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dump$6(com.android.internal.util.IndentingPrintWriter pw, int userId, String pkgName, String tag, LongArrayQueue events) {
        if (events.size() > 0) {
            pw.print(Uptc.string(userId, pkgName, tag));
            pw.println(":");
            pw.increaseIndent();
            pw.print(events.get(0));
            for (int i = 1; i < events.size(); i++) {
                pw.print(", ");
                pw.print(events.get(i));
            }
            pw.decreaseIndent();
            pw.println();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dump$7(com.android.internal.util.IndentingPrintWriter pw, int userId, String pkgName, String tag, ExecutionStats stats) {
        if (stats != null) {
            pw.print(Uptc.string(userId, pkgName, tag));
            pw.println(":");
            pw.increaseIndent();
            pw.println(stats);
            pw.decreaseIndent();
        }
    }

    @Override // com.android.server.utils.quota.QuotaTracker
    public void dump(final ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        synchronized (this.mLock) {
            super.dump(proto, 1146756268033L);
            for (int i = 0; i < this.mCategoryCountWindowSizesMs.size(); i++) {
                Category category = this.mCategoryCountWindowSizesMs.keyAt(i);
                long clToken = proto.start(2246267895810L);
                category.dumpDebug(proto, 1146756268033L);
                proto.write(1120986464258L, this.mMaxCategoryCounts.get(category).intValue());
                proto.write(1112396529667L, this.mCategoryCountWindowSizesMs.get(category).longValue());
                proto.end(clToken);
            }
            this.mExecutionStatsCache.forEach(new UptcMap.UptcDataConsumer() { // from class: com.android.server.utils.quota.CountQuotaTracker$$ExternalSyntheticLambda4
                @Override // com.android.server.utils.quota.UptcMap.UptcDataConsumer
                public final void accept(int i2, String str, String str2, Object obj) {
                    CountQuotaTracker.this.m7441lambda$dump$8$comandroidserverutilsquotaCountQuotaTracker(proto, i2, str, str2, (CountQuotaTracker.ExecutionStats) obj);
                }
            });
            proto.end(token);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dump$8$com-android-server-utils-quota-CountQuotaTracker  reason: not valid java name */
    public /* synthetic */ void m7441lambda$dump$8$comandroidserverutilsquotaCountQuotaTracker(ProtoOutputStream proto, int userId, String pkgName, String tag, ExecutionStats stats) {
        boolean isQuotaFree = isIndividualQuotaFreeLocked(userId, pkgName);
        long j = 2246267895811L;
        long usToken = proto.start(2246267895811L);
        new Uptc(userId, pkgName, tag).dumpDebug(proto, 1146756268033L);
        proto.write(1133871366146L, isQuotaFree);
        LongArrayQueue events = this.mEventTimes.get(userId, pkgName, tag);
        if (events != null) {
            int j2 = events.size() - 1;
            while (j2 >= 0) {
                long eToken = proto.start(j);
                proto.write(1112396529665L, events.get(j2));
                proto.end(eToken);
                j2--;
                j = 2246267895811L;
            }
        }
        long statsToken = proto.start(2246267895812L);
        proto.write(1112396529665L, stats.expirationTimeElapsed);
        proto.write(1112396529666L, stats.windowSizeMs);
        proto.write(1120986464259L, stats.countLimit);
        proto.write(1120986464260L, stats.countInWindow);
        proto.write(1112396529669L, stats.inQuotaTimeElapsed);
        proto.end(statsToken);
        proto.end(usToken);
    }
}
