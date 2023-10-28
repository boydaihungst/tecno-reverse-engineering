package com.android.server.people.prediction;

import android.app.usage.UsageEvents;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Range;
import com.android.internal.app.ChooserActivity;
import com.android.server.people.data.AppUsageStatsData;
import com.android.server.people.data.DataManager;
import com.android.server.people.data.Event;
import com.android.server.people.prediction.ShareTargetPredictor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.ToLongFunction;
/* loaded from: classes2.dex */
class SharesheetModelScorer {
    private static final boolean DEBUG = false;
    static final float FOREGROUND_APP_WEIGHT = 0.0f;
    private static final float FREQUENTLY_USED_APP_SCORE_INITIAL_DECAY = 0.3f;
    private static final float RECENCY_INITIAL_BASE_SCORE = 0.4f;
    private static final float RECENCY_SCORE_INITIAL_DECAY = 0.05f;
    private static final float RECENCY_SCORE_SUBSEQUENT_DECAY = 0.02f;
    private static final String TAG = "SharesheetModelScorer";
    private static final float USAGE_STATS_CHOOSER_SCORE_INITIAL_DECAY = 0.9f;
    private static final Integer RECENCY_SCORE_COUNT = 6;
    private static final long ONE_MONTH_WINDOW = TimeUnit.DAYS.toMillis(30);
    private static final long FOREGROUND_APP_PROMO_TIME_WINDOW = TimeUnit.MINUTES.toMillis(10);
    static final String CHOOSER_ACTIVITY = ChooserActivity.class.getSimpleName();

    private SharesheetModelScorer() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void computeScore(List<ShareTargetPredictor.ShareTarget> shareTargets, int shareEventType, long now) {
        Float avgFreq;
        double frequencyScore;
        if (shareTargets.isEmpty()) {
            return;
        }
        float totalFreqScore = FOREGROUND_APP_WEIGHT;
        int freqScoreCount = 0;
        float totalMimeFreqScore = FOREGROUND_APP_WEIGHT;
        int mimeFreqScoreCount = 0;
        PriorityQueue<Pair<ShareTargetRankingScore, Range<Long>>> recencyMinHeap = new PriorityQueue<>(RECENCY_SCORE_COUNT.intValue(), Comparator.comparingLong(new ToLongFunction() { // from class: com.android.server.people.prediction.SharesheetModelScorer$$ExternalSyntheticLambda2
            @Override // java.util.function.ToLongFunction
            public final long applyAsLong(Object obj) {
                long longValue;
                longValue = ((Long) ((Range) ((Pair) obj).second).getUpper()).longValue();
                return longValue;
            }
        }));
        List<ShareTargetRankingScore> scoreList = new ArrayList<>(shareTargets.size());
        for (ShareTargetPredictor.ShareTarget target : shareTargets) {
            ShareTargetRankingScore shareTargetScore = new ShareTargetRankingScore();
            scoreList.add(shareTargetScore);
            if (target.getEventHistory() != null) {
                List<Range<Long>> timeSlots = target.getEventHistory().getEventIndex(Event.SHARE_EVENT_TYPES).getActiveTimeSlots();
                if (!timeSlots.isEmpty()) {
                    for (Range<Long> timeSlot : timeSlots) {
                        shareTargetScore.incrementFrequencyScore(getFreqDecayedOnElapsedTime(now - timeSlot.getLower().longValue()));
                    }
                    totalFreqScore += shareTargetScore.getFrequencyScore();
                    freqScoreCount++;
                }
                List<Range<Long>> timeSlotsOfSameType = target.getEventHistory().getEventIndex(shareEventType).getActiveTimeSlots();
                if (!timeSlotsOfSameType.isEmpty()) {
                    for (Range<Long> timeSlot2 : timeSlotsOfSameType) {
                        shareTargetScore.incrementMimeFrequencyScore(getFreqDecayedOnElapsedTime(now - timeSlot2.getLower().longValue()));
                    }
                    totalMimeFreqScore += shareTargetScore.getMimeFrequencyScore();
                    mimeFreqScoreCount++;
                }
                Range<Long> mostRecentTimeSlot = target.getEventHistory().getEventIndex(Event.SHARE_EVENT_TYPES).getMostRecentActiveTimeSlot();
                if (mostRecentTimeSlot != null) {
                    int size = recencyMinHeap.size();
                    Integer num = RECENCY_SCORE_COUNT;
                    if (size < num.intValue() || mostRecentTimeSlot.getUpper().longValue() > ((Long) ((Range) recencyMinHeap.peek().second).getUpper()).longValue()) {
                        if (recencyMinHeap.size() == num.intValue()) {
                            recencyMinHeap.poll();
                        }
                        recencyMinHeap.offer(new Pair<>(shareTargetScore, mostRecentTimeSlot));
                    }
                }
            }
        }
        while (!recencyMinHeap.isEmpty()) {
            float recencyScore = RECENCY_INITIAL_BASE_SCORE;
            if (recencyMinHeap.size() > 1) {
                recencyScore = 0.35f - ((recencyMinHeap.size() - 2) * RECENCY_SCORE_SUBSEQUENT_DECAY);
            }
            ((ShareTargetRankingScore) recencyMinHeap.poll().first).setRecencyScore(recencyScore);
        }
        float f = FOREGROUND_APP_WEIGHT;
        Float avgFreq2 = Float.valueOf(freqScoreCount != 0 ? totalFreqScore / freqScoreCount : 0.0f);
        Float avgMimeFreq = Float.valueOf(mimeFreqScoreCount != 0 ? totalMimeFreqScore / mimeFreqScoreCount : 0.0f);
        int i = 0;
        while (i < scoreList.size()) {
            ShareTargetPredictor.ShareTarget target2 = shareTargets.get(i);
            ShareTargetRankingScore targetScore = scoreList.get(i);
            double d = 0.0d;
            if (avgFreq2.equals(Float.valueOf(f))) {
                avgFreq = avgFreq2;
                frequencyScore = 0.0d;
            } else {
                avgFreq = avgFreq2;
                frequencyScore = targetScore.getFrequencyScore() / avgFreq2.floatValue();
            }
            targetScore.setFrequencyScore(normalizeFreqScore(frequencyScore));
            f = FOREGROUND_APP_WEIGHT;
            if (!avgMimeFreq.equals(Float.valueOf((float) FOREGROUND_APP_WEIGHT))) {
                d = targetScore.getMimeFrequencyScore() / avgMimeFreq.floatValue();
            }
            targetScore.setMimeFrequencyScore(normalizeMimeFreqScore(d));
            targetScore.setTotalScore(probOR(probOR(targetScore.getRecencyScore(), targetScore.getFrequencyScore()), targetScore.getMimeFrequencyScore()));
            target2.setScore(targetScore.getTotalScore());
            i++;
            avgFreq2 = avgFreq;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void computeScoreForAppShare(List<ShareTargetPredictor.ShareTarget> shareTargets, int shareEventType, int targetsLimit, long now, DataManager dataManager, int callingUserId) {
        computeScore(shareTargets, shareEventType, now);
        postProcess(shareTargets, targetsLimit, dataManager, callingUserId);
    }

    private static void postProcess(List<ShareTargetPredictor.ShareTarget> shareTargets, int targetsLimit, DataManager dataManager, int callingUserId) {
        Map<String, List<ShareTargetPredictor.ShareTarget>> shareTargetMap = new ArrayMap<>();
        for (ShareTargetPredictor.ShareTarget shareTarget : shareTargets) {
            String packageName = shareTarget.getAppTarget().getPackageName();
            shareTargetMap.computeIfAbsent(packageName, new Function() { // from class: com.android.server.people.prediction.SharesheetModelScorer$$ExternalSyntheticLambda3
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return SharesheetModelScorer.lambda$postProcess$1((String) obj);
                }
            });
            List<ShareTargetPredictor.ShareTarget> targetsList = shareTargetMap.get(packageName);
            int index = 0;
            while (index < targetsList.size() && shareTarget.getScore() <= targetsList.get(index).getScore()) {
                index++;
            }
            targetsList.add(index, shareTarget);
        }
        promoteForegroundApp(shareTargetMap, dataManager, callingUserId);
        promoteMostChosenAndFrequentlyUsedApps(shareTargetMap, targetsLimit, dataManager, callingUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ List lambda$postProcess$1(String key) {
        return new ArrayList();
    }

    private static void promoteMostChosenAndFrequentlyUsedApps(Map<String, List<ShareTargetPredictor.ShareTarget>> shareTargetMap, int targetsLimit, DataManager dataManager, int callingUserId) {
        int validPredictionNum = 0;
        float minValidScore = 1.0f;
        for (List<ShareTargetPredictor.ShareTarget> targets : shareTargetMap.values()) {
            for (ShareTargetPredictor.ShareTarget target : targets) {
                if (target.getScore() > FOREGROUND_APP_WEIGHT) {
                    validPredictionNum++;
                    minValidScore = Math.min(target.getScore(), minValidScore);
                }
            }
        }
        if (validPredictionNum >= targetsLimit) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, AppUsageStatsData> appStatsMap = dataManager.queryAppUsageStats(callingUserId, now - ONE_MONTH_WINDOW, now, shareTargetMap.keySet());
        float minValidScore2 = promoteApp(shareTargetMap, appStatsMap, new Function() { // from class: com.android.server.people.prediction.SharesheetModelScorer$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return Integer.valueOf(((AppUsageStatsData) obj).getChosenCount());
            }
        }, USAGE_STATS_CHOOSER_SCORE_INITIAL_DECAY * minValidScore, minValidScore);
        promoteApp(shareTargetMap, appStatsMap, new Function() { // from class: com.android.server.people.prediction.SharesheetModelScorer$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return Integer.valueOf(((AppUsageStatsData) obj).getLaunchCount());
            }
        }, FREQUENTLY_USED_APP_SCORE_INITIAL_DECAY * minValidScore2, minValidScore2);
    }

    private static float promoteApp(Map<String, List<ShareTargetPredictor.ShareTarget>> shareTargetMap, Map<String, AppUsageStatsData> appStatsMap, Function<AppUsageStatsData, Integer> countFunc, float baseScore, float minValidScore) {
        int maxCount = 0;
        for (AppUsageStatsData data : appStatsMap.values()) {
            maxCount = Math.max(maxCount, countFunc.apply(data).intValue());
        }
        if (maxCount > 0) {
            for (Map.Entry<String, AppUsageStatsData> entry : appStatsMap.entrySet()) {
                if (shareTargetMap.containsKey(entry.getKey())) {
                    ShareTargetPredictor.ShareTarget target = shareTargetMap.get(entry.getKey()).get(0);
                    if (target.getScore() <= FOREGROUND_APP_WEIGHT) {
                        float curScore = (countFunc.apply(entry.getValue()).intValue() * baseScore) / maxCount;
                        target.setScore(curScore);
                        if (curScore > FOREGROUND_APP_WEIGHT) {
                            minValidScore = Math.min(minValidScore, curScore);
                        }
                    }
                }
            }
        }
        return minValidScore;
    }

    private static void promoteForegroundApp(Map<String, List<ShareTargetPredictor.ShareTarget>> shareTargetMap, DataManager dataManager, int callingUserId) {
        String sharingForegroundApp = findSharingForegroundApp(shareTargetMap, dataManager, callingUserId);
        if (sharingForegroundApp != null) {
            ShareTargetPredictor.ShareTarget target = shareTargetMap.get(sharingForegroundApp).get(0);
            target.setScore(probOR(target.getScore(), FOREGROUND_APP_WEIGHT));
        }
    }

    private static String findSharingForegroundApp(Map<String, List<ShareTargetPredictor.ShareTarget>> shareTargetMap, DataManager dataManager, int callingUserId) {
        long now = System.currentTimeMillis();
        List<UsageEvents.Event> events = dataManager.queryAppMovingToForegroundEvents(callingUserId, now - FOREGROUND_APP_PROMO_TIME_WINDOW, now);
        String sourceApp = null;
        for (int i = events.size() - 1; i >= 0; i--) {
            String className = events.get(i).getClassName();
            String packageName = events.get(i).getPackageName();
            if (packageName != null && ((className == null || !className.contains(CHOOSER_ACTIVITY)) && !packageName.contains(CHOOSER_ACTIVITY))) {
                if (sourceApp == null) {
                    sourceApp = packageName;
                } else if (!packageName.equals(sourceApp) && shareTargetMap.containsKey(packageName)) {
                    return packageName;
                }
            }
        }
        return null;
    }

    private static float probOR(float a, float b) {
        return 1.0f - ((1.0f - a) * (1.0f - b));
    }

    private static float getFreqDecayedOnElapsedTime(long elapsedTimeMillis) {
        Duration duration = Duration.ofMillis(elapsedTimeMillis);
        if (duration.compareTo(Duration.ofDays(1L)) <= 0) {
            return 1.0f;
        }
        if (duration.compareTo(Duration.ofDays(3L)) <= 0) {
            return USAGE_STATS_CHOOSER_SCORE_INITIAL_DECAY;
        }
        if (duration.compareTo(Duration.ofDays(7L)) <= 0) {
            return 0.8f;
        }
        if (duration.compareTo(Duration.ofDays(14L)) <= 0) {
            return 0.7f;
        }
        return 0.6f;
    }

    private static float normalizeFreqScore(double freqRatio) {
        if (freqRatio >= 2.5d) {
            return 0.2f;
        }
        if (freqRatio >= 1.5d) {
            return 0.15f;
        }
        if (freqRatio >= 1.0d) {
            return 0.1f;
        }
        if (freqRatio >= 0.75d) {
            return RECENCY_SCORE_INITIAL_DECAY;
        }
        return FOREGROUND_APP_WEIGHT;
    }

    private static float normalizeMimeFreqScore(double freqRatio) {
        if (freqRatio >= 2.0d) {
            return 0.2f;
        }
        if (freqRatio >= 1.2d) {
            return 0.15f;
        }
        if (freqRatio > 0.0d) {
            return 0.1f;
        }
        return FOREGROUND_APP_WEIGHT;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ShareTargetRankingScore {
        private float mFrequencyScore;
        private float mMimeFrequencyScore;
        private float mRecencyScore;
        private float mTotalScore;

        private ShareTargetRankingScore() {
            this.mRecencyScore = SharesheetModelScorer.FOREGROUND_APP_WEIGHT;
            this.mFrequencyScore = SharesheetModelScorer.FOREGROUND_APP_WEIGHT;
            this.mMimeFrequencyScore = SharesheetModelScorer.FOREGROUND_APP_WEIGHT;
            this.mTotalScore = SharesheetModelScorer.FOREGROUND_APP_WEIGHT;
        }

        float getTotalScore() {
            return this.mTotalScore;
        }

        void setTotalScore(float totalScore) {
            this.mTotalScore = totalScore;
        }

        float getRecencyScore() {
            return this.mRecencyScore;
        }

        void setRecencyScore(float recencyScore) {
            this.mRecencyScore = recencyScore;
        }

        float getFrequencyScore() {
            return this.mFrequencyScore;
        }

        void setFrequencyScore(float frequencyScore) {
            this.mFrequencyScore = frequencyScore;
        }

        void incrementFrequencyScore(float incremental) {
            this.mFrequencyScore += incremental;
        }

        float getMimeFrequencyScore() {
            return this.mMimeFrequencyScore;
        }

        void setMimeFrequencyScore(float mimeFrequencyScore) {
            this.mMimeFrequencyScore = mimeFrequencyScore;
        }

        void incrementMimeFrequencyScore(float incremental) {
            this.mMimeFrequencyScore += incremental;
        }
    }
}
