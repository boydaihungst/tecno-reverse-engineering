package com.android.server.am;

import android.os.Process;
import android.os.SystemClock;
import android.provider.DeviceConfig;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public class CacheOomRanker {
    static final float DEFAULT_OOM_RE_RANKING_LRU_WEIGHT = 0.35f;
    static final int DEFAULT_OOM_RE_RANKING_NUMBER_TO_RE_RANK = 8;
    static final float DEFAULT_OOM_RE_RANKING_RSS_WEIGHT = 0.15f;
    static final float DEFAULT_OOM_RE_RANKING_USES_WEIGHT = 0.5f;
    static final int DEFAULT_PRESERVE_TOP_N_APPS = 3;
    static final long DEFAULT_RSS_UPDATE_RATE_MS = 10000;
    static final boolean DEFAULT_USE_FREQUENT_RSS = true;
    private static final boolean DEFAULT_USE_OOM_RE_RANKING = false;
    static final String KEY_OOM_RE_RANKING_LRU_WEIGHT = "oom_re_ranking_lru_weight";
    static final String KEY_OOM_RE_RANKING_NUMBER_TO_RE_RANK = "oom_re_ranking_number_to_re_rank";
    static final String KEY_OOM_RE_RANKING_PRESERVE_TOP_N_APPS = "oom_re_ranking_preserve_top_n_apps";
    static final String KEY_OOM_RE_RANKING_RSS_UPDATE_RATE_MS = "oom_re_ranking_rss_update_rate_ms";
    static final String KEY_OOM_RE_RANKING_RSS_WEIGHT = "oom_re_ranking_rss_weight";
    static final String KEY_OOM_RE_RANKING_USES_WEIGHT = "oom_re_ranking_uses_weight";
    static final String KEY_OOM_RE_RANKING_USE_FREQUENT_RSS = "oom_re_ranking_rss_use_frequent_rss";
    static final String KEY_USE_OOM_RE_RANKING = "use_oom_re_ranking";
    private int[] mLruPositions;
    float mLruWeight;
    private final DeviceConfig.OnPropertiesChangedListener mOnFlagsChangedListener;
    private final Object mPhenotypeFlagLock;
    int mPreserveTopNApps;
    private final ActivityManagerGlobalLock mProcLock;
    private final ProcessDependencies mProcessDependencies;
    private final Object mProfilerLock;
    long mRssUpdateRateMs;
    float mRssWeight;
    private RankedProcessRecord[] mScoredProcessRecords;
    private final ActivityManagerService mService;
    boolean mUseFrequentRss;
    private boolean mUseOomReRanking;
    float mUsesWeight;
    private static final Comparator<RankedProcessRecord> SCORED_PROCESS_RECORD_COMPARATOR = new ScoreComparator();
    private static final Comparator<RankedProcessRecord> CACHE_USE_COMPARATOR = new CacheUseComparator();
    private static final Comparator<RankedProcessRecord> RSS_COMPARATOR = new RssComparator();
    private static final Comparator<RankedProcessRecord> LAST_RSS_COMPARATOR = new LastRssComparator();
    private static final Comparator<RankedProcessRecord> LAST_ACTIVITY_TIME_COMPARATOR = new LastActivityTimeComparator();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface ProcessDependencies {
        long[] getRss(int i);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CacheOomRanker(ActivityManagerService service) {
        this(service, new ProcessDependenciesImpl());
    }

    CacheOomRanker(ActivityManagerService service, ProcessDependencies processDependencies) {
        this.mPhenotypeFlagLock = new Object();
        this.mUseOomReRanking = false;
        this.mPreserveTopNApps = 3;
        this.mUseFrequentRss = true;
        this.mRssUpdateRateMs = 10000L;
        this.mLruWeight = DEFAULT_OOM_RE_RANKING_LRU_WEIGHT;
        this.mUsesWeight = 0.5f;
        this.mRssWeight = DEFAULT_OOM_RE_RANKING_RSS_WEIGHT;
        this.mOnFlagsChangedListener = new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.am.CacheOomRanker.1
            public void onPropertiesChanged(DeviceConfig.Properties properties) {
                synchronized (CacheOomRanker.this.mPhenotypeFlagLock) {
                    for (String name : properties.getKeyset()) {
                        if (CacheOomRanker.KEY_USE_OOM_RE_RANKING.equals(name)) {
                            CacheOomRanker.this.updateUseOomReranking();
                        } else if (CacheOomRanker.KEY_OOM_RE_RANKING_NUMBER_TO_RE_RANK.equals(name)) {
                            CacheOomRanker.this.updateNumberToReRank();
                        } else if (CacheOomRanker.KEY_OOM_RE_RANKING_PRESERVE_TOP_N_APPS.equals(name)) {
                            CacheOomRanker.this.updatePreserveTopNApps();
                        } else if (CacheOomRanker.KEY_OOM_RE_RANKING_USE_FREQUENT_RSS.equals(name)) {
                            CacheOomRanker.this.updateUseFrequentRss();
                        } else if (CacheOomRanker.KEY_OOM_RE_RANKING_RSS_UPDATE_RATE_MS.equals(name)) {
                            CacheOomRanker.this.updateRssUpdateRateMs();
                        } else if (CacheOomRanker.KEY_OOM_RE_RANKING_LRU_WEIGHT.equals(name)) {
                            CacheOomRanker.this.updateLruWeight();
                        } else if (CacheOomRanker.KEY_OOM_RE_RANKING_USES_WEIGHT.equals(name)) {
                            CacheOomRanker.this.updateUsesWeight();
                        } else if (CacheOomRanker.KEY_OOM_RE_RANKING_RSS_WEIGHT.equals(name)) {
                            CacheOomRanker.this.updateRssWeight();
                        }
                    }
                }
            }
        };
        this.mService = service;
        this.mProcLock = service.mProcLock;
        this.mProfilerLock = service.mAppProfiler.mProfilerLock;
        this.mProcessDependencies = processDependencies;
    }

    public void init(Executor executor) {
        DeviceConfig.addOnPropertiesChangedListener("activity_manager", executor, this.mOnFlagsChangedListener);
        synchronized (this.mPhenotypeFlagLock) {
            updateUseOomReranking();
            updateNumberToReRank();
            updateLruWeight();
            updateUsesWeight();
            updateRssWeight();
        }
    }

    public boolean useOomReranking() {
        boolean z;
        synchronized (this.mPhenotypeFlagLock) {
            z = this.mUseOomReRanking;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateUseOomReranking() {
        this.mUseOomReRanking = DeviceConfig.getBoolean("activity_manager", KEY_USE_OOM_RE_RANKING, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNumberToReRank() {
        int previousNumberToReRank = getNumberToReRank();
        int numberToReRank = DeviceConfig.getInt("activity_manager", KEY_OOM_RE_RANKING_NUMBER_TO_RE_RANK, 8);
        if (previousNumberToReRank != numberToReRank) {
            this.mScoredProcessRecords = new RankedProcessRecord[numberToReRank];
            int i = 0;
            while (true) {
                RankedProcessRecord[] rankedProcessRecordArr = this.mScoredProcessRecords;
                if (i < rankedProcessRecordArr.length) {
                    rankedProcessRecordArr[i] = new RankedProcessRecord();
                    i++;
                } else {
                    this.mLruPositions = new int[numberToReRank];
                    return;
                }
            }
        }
    }

    int getNumberToReRank() {
        RankedProcessRecord[] rankedProcessRecordArr = this.mScoredProcessRecords;
        if (rankedProcessRecordArr == null) {
            return 0;
        }
        return rankedProcessRecordArr.length;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updatePreserveTopNApps() {
        int preserveTopNApps = DeviceConfig.getInt("activity_manager", KEY_OOM_RE_RANKING_PRESERVE_TOP_N_APPS, 3);
        if (preserveTopNApps < 0) {
            Slog.w("OomAdjuster", "Found negative value for preserveTopNApps, setting to default: " + preserveTopNApps);
            preserveTopNApps = 3;
        }
        this.mPreserveTopNApps = preserveTopNApps;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRssUpdateRateMs() {
        this.mRssUpdateRateMs = DeviceConfig.getLong("activity_manager", KEY_OOM_RE_RANKING_RSS_UPDATE_RATE_MS, 10000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateUseFrequentRss() {
        this.mUseFrequentRss = DeviceConfig.getBoolean("activity_manager", KEY_OOM_RE_RANKING_USE_FREQUENT_RSS, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLruWeight() {
        this.mLruWeight = DeviceConfig.getFloat("activity_manager", KEY_OOM_RE_RANKING_LRU_WEIGHT, (float) DEFAULT_OOM_RE_RANKING_LRU_WEIGHT);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateUsesWeight() {
        this.mUsesWeight = DeviceConfig.getFloat("activity_manager", KEY_OOM_RE_RANKING_USES_WEIGHT, 0.5f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRssWeight() {
        this.mRssWeight = DeviceConfig.getFloat("activity_manager", KEY_OOM_RE_RANKING_RSS_WEIGHT, (float) DEFAULT_OOM_RE_RANKING_RSS_WEIGHT);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reRankLruCachedAppsLSP(ArrayList<ProcessRecord> lruList, int lruProcessServiceStart) {
        float lruWeight;
        float usesWeight;
        float rssWeight;
        int preserveTopNApps;
        boolean useFrequentRss;
        long rssUpdateRateMs;
        int[] lruPositions;
        RankedProcessRecord[] scoredProcessRecords;
        int i;
        ArrayList<ProcessRecord> arrayList;
        long rssUpdateRateMs2;
        long nowMs;
        synchronized (this.mPhenotypeFlagLock) {
            try {
                lruWeight = this.mLruWeight;
                usesWeight = this.mUsesWeight;
                rssWeight = this.mRssWeight;
                preserveTopNApps = this.mPreserveTopNApps;
                useFrequentRss = this.mUseFrequentRss;
                rssUpdateRateMs = this.mRssUpdateRateMs;
                lruPositions = this.mLruPositions;
                scoredProcessRecords = this.mScoredProcessRecords;
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        if (lruPositions != null && scoredProcessRecords != null) {
            int numProcessesEvaluated = 0;
            int numProcessesReRanked = 0;
            while (numProcessesEvaluated < lruProcessServiceStart && numProcessesReRanked < scoredProcessRecords.length) {
                ProcessRecord process = lruList.get(numProcessesEvaluated);
                if (appCanBeReRanked(process)) {
                    scoredProcessRecords[numProcessesReRanked].proc = process;
                    scoredProcessRecords[numProcessesReRanked].score = 0.0f;
                    lruPositions[numProcessesReRanked] = numProcessesEvaluated;
                    numProcessesReRanked++;
                }
                numProcessesEvaluated++;
            }
            int numProcessesNotReRanked = 0;
            int numProcessesEvaluated2 = numProcessesEvaluated;
            while (numProcessesEvaluated2 < lruProcessServiceStart && numProcessesNotReRanked < preserveTopNApps) {
                if (appCanBeReRanked(lruList.get(numProcessesEvaluated2))) {
                    numProcessesNotReRanked++;
                }
                numProcessesEvaluated2++;
            }
            if (numProcessesNotReRanked < preserveTopNApps && (numProcessesReRanked = numProcessesReRanked - (preserveTopNApps - numProcessesNotReRanked)) < 0) {
                numProcessesReRanked = 0;
            }
            if (useFrequentRss) {
                long nowMs2 = SystemClock.elapsedRealtime();
                int i2 = 0;
                while (i2 < numProcessesReRanked) {
                    int preserveTopNApps2 = preserveTopNApps;
                    RankedProcessRecord scoredProcessRecord = scoredProcessRecords[i2];
                    int numProcessesEvaluated3 = numProcessesEvaluated2;
                    long sinceUpdateMs = nowMs2 - scoredProcessRecord.proc.mState.getCacheOomRankerRssTimeMs();
                    if (scoredProcessRecord.proc.mState.getCacheOomRankerRss() != 0 && sinceUpdateMs < rssUpdateRateMs) {
                        nowMs = nowMs2;
                        rssUpdateRateMs2 = rssUpdateRateMs;
                    } else {
                        rssUpdateRateMs2 = rssUpdateRateMs;
                        long rssUpdateRateMs3 = nowMs2;
                        long[] rss = this.mProcessDependencies.getRss(scoredProcessRecord.proc.getPid());
                        if (rss != null && rss.length != 0) {
                            scoredProcessRecord.proc.mState.setCacheOomRankerRss(rss[0], rssUpdateRateMs3);
                            ProcessProfileRecord processProfileRecord = scoredProcessRecord.proc.mProfile;
                            nowMs = rssUpdateRateMs3;
                            long nowMs3 = rss[0];
                            processProfileRecord.setLastRss(nowMs3);
                        }
                        Slog.e("OomAdjuster", "Process.getRss returned bad value, not re-ranking: " + Arrays.toString(rss));
                        return;
                    }
                    i2++;
                    preserveTopNApps = preserveTopNApps2;
                    numProcessesEvaluated2 = numProcessesEvaluated3;
                    rssUpdateRateMs = rssUpdateRateMs2;
                    nowMs2 = nowMs;
                }
            }
            if (lruWeight > 0.0f) {
                Arrays.sort(scoredProcessRecords, 0, numProcessesReRanked, LAST_ACTIVITY_TIME_COMPARATOR);
                addToScore(scoredProcessRecords, lruWeight);
            }
            if (rssWeight > 0.0f) {
                if (useFrequentRss) {
                    Arrays.sort(scoredProcessRecords, 0, numProcessesReRanked, RSS_COMPARATOR);
                } else {
                    synchronized (this.mService.mAppProfiler.mProfilerLock) {
                        Arrays.sort(scoredProcessRecords, 0, numProcessesReRanked, LAST_RSS_COMPARATOR);
                    }
                }
                addToScore(scoredProcessRecords, rssWeight);
            }
            if (usesWeight <= 0.0f) {
                i = 0;
            } else {
                i = 0;
                Arrays.sort(scoredProcessRecords, 0, numProcessesReRanked, CACHE_USE_COMPARATOR);
                addToScore(scoredProcessRecords, usesWeight);
            }
            Arrays.sort(scoredProcessRecords, i, numProcessesReRanked, SCORED_PROCESS_RECORD_COMPARATOR);
            if (!ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
                arrayList = lruList;
            } else {
                boolean printedHeader = false;
                for (int i3 = 0; i3 < numProcessesReRanked; i3++) {
                    if (scoredProcessRecords[i3].proc.getPid() != lruList.get(lruPositions[i3]).getPid()) {
                        if (!printedHeader) {
                            Slog.i("OomAdjuster", "reRankLruCachedApps");
                            printedHeader = true;
                        }
                        Slog.i("OomAdjuster", "  newPos=" + lruPositions[i3] + " " + scoredProcessRecords[i3].proc);
                    }
                }
                arrayList = lruList;
            }
            for (int i4 = 0; i4 < numProcessesReRanked; i4++) {
                arrayList.set(lruPositions[i4], scoredProcessRecords[i4].proc);
                scoredProcessRecords[i4].proc = null;
            }
        }
    }

    private static boolean appCanBeReRanked(ProcessRecord process) {
        return (process.isKilledByAm() || process.getThread() == null || process.mState.getCurAdj() < 1001) ? false : true;
    }

    private static void addToScore(RankedProcessRecord[] scores, float weight) {
        for (int i = 1; i < scores.length; i++) {
            scores[i].score += i * weight;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println("CacheOomRanker settings");
        synchronized (this.mPhenotypeFlagLock) {
            pw.println("  use_oom_re_ranking=" + this.mUseOomReRanking);
            pw.println("  oom_re_ranking_number_to_re_rank=" + getNumberToReRank());
            pw.println("  oom_re_ranking_lru_weight=" + this.mLruWeight);
            pw.println("  oom_re_ranking_uses_weight=" + this.mUsesWeight);
            pw.println("  oom_re_ranking_rss_weight=" + this.mRssWeight);
        }
    }

    /* loaded from: classes.dex */
    private static class ScoreComparator implements Comparator<RankedProcessRecord> {
        private ScoreComparator() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(RankedProcessRecord o1, RankedProcessRecord o2) {
            return Float.compare(o1.score, o2.score);
        }
    }

    /* loaded from: classes.dex */
    private static class LastActivityTimeComparator implements Comparator<RankedProcessRecord> {
        private LastActivityTimeComparator() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(RankedProcessRecord o1, RankedProcessRecord o2) {
            return Long.compare(o1.proc.getLastActivityTime(), o2.proc.getLastActivityTime());
        }
    }

    /* loaded from: classes.dex */
    private static class CacheUseComparator implements Comparator<RankedProcessRecord> {
        private CacheUseComparator() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(RankedProcessRecord o1, RankedProcessRecord o2) {
            return Long.compare(o1.proc.mState.getCacheOomRankerUseCount(), o2.proc.mState.getCacheOomRankerUseCount());
        }
    }

    /* loaded from: classes.dex */
    private static class RssComparator implements Comparator<RankedProcessRecord> {
        private RssComparator() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(RankedProcessRecord o1, RankedProcessRecord o2) {
            return Long.compare(o2.proc.mState.getCacheOomRankerRss(), o1.proc.mState.getCacheOomRankerRss());
        }
    }

    /* loaded from: classes.dex */
    private static class LastRssComparator implements Comparator<RankedProcessRecord> {
        private LastRssComparator() {
        }

        /* JADX DEBUG: Method merged with bridge method */
        @Override // java.util.Comparator
        public int compare(RankedProcessRecord o1, RankedProcessRecord o2) {
            return Long.compare(o2.proc.mProfile.getLastRss(), o1.proc.mProfile.getLastRss());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class RankedProcessRecord {
        public ProcessRecord proc;
        public float score;

        private RankedProcessRecord() {
        }
    }

    /* loaded from: classes.dex */
    private static class ProcessDependenciesImpl implements ProcessDependencies {
        private ProcessDependenciesImpl() {
        }

        @Override // com.android.server.am.CacheOomRanker.ProcessDependencies
        public long[] getRss(int pid) {
            return Process.getRss(pid);
        }
    }
}
