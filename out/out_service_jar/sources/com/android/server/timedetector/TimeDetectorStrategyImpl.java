package com.android.server.timedetector;

import android.app.time.ExternalTimeSuggestion;
import android.app.timedetector.GnssTimeSuggestion;
import android.app.timedetector.ManualTimeSuggestion;
import android.app.timedetector.NetworkTimeSuggestion;
import android.app.timedetector.TelephonyTimeSuggestion;
import android.content.Context;
import android.os.Handler;
import android.os.TimestampedValue;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.Slog;
import com.android.server.timezonedetector.ArrayMapWithHistory;
import com.android.server.timezonedetector.ConfigurationChangeListener;
import com.android.server.timezonedetector.ReferenceWithHistory;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
/* loaded from: classes2.dex */
public final class TimeDetectorStrategyImpl implements TimeDetectorStrategy {
    private static final boolean DBG = false;
    private static final int KEEP_SUGGESTION_HISTORY_SIZE = 10;
    private static final String LOG_TAG = "time_detector";
    static final long MAX_SUGGESTION_TIME_AGE_MILLIS = 86400000;
    private static final long SYSTEM_CLOCK_PARANOIA_THRESHOLD_MILLIS = 2000;
    private static final int TELEPHONY_BUCKET_COUNT = 24;
    static final int TELEPHONY_BUCKET_SIZE_MILLIS = 3600000;
    private static final int TELEPHONY_INVALID_SCORE = -1;
    private final Environment mEnvironment;
    private TimestampedValue<Long> mLastAutoSystemClockTimeSet;
    private final LocalLog mTimeChangesLog = new LocalLog(30, false);
    private final ArrayMapWithHistory<Integer, TelephonyTimeSuggestion> mSuggestionBySlotIndex = new ArrayMapWithHistory<>(10);
    private final ReferenceWithHistory<NetworkTimeSuggestion> mLastNetworkSuggestion = new ReferenceWithHistory<>(10);
    private final ReferenceWithHistory<GnssTimeSuggestion> mLastGnssSuggestion = new ReferenceWithHistory<>(10);
    private final ReferenceWithHistory<ExternalTimeSuggestion> mLastExternalSuggestion = new ReferenceWithHistory<>(10);

    /* loaded from: classes2.dex */
    public interface Environment {
        void acquireWakeLock();

        int[] autoOriginPriorities();

        Instant autoTimeLowerBound();

        ConfigurationInternal configurationInternal(int i);

        long elapsedRealtimeMillis();

        boolean isAutoTimeDetectionEnabled();

        void releaseWakeLock();

        void setConfigChangeListener(ConfigurationChangeListener configurationChangeListener);

        void setSystemClock(long j);

        long systemClockMillis();

        int systemClockUpdateThresholdMillis();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static TimeDetectorStrategy create(Context context, Handler handler, ServiceConfigAccessor serviceConfigAccessor) {
        Environment environment = new EnvironmentImpl(context, handler, serviceConfigAccessor);
        return new TimeDetectorStrategyImpl(environment);
    }

    TimeDetectorStrategyImpl(Environment environment) {
        Environment environment2 = (Environment) Objects.requireNonNull(environment);
        this.mEnvironment = environment2;
        environment2.setConfigChangeListener(new ConfigurationChangeListener() { // from class: com.android.server.timedetector.TimeDetectorStrategyImpl$$ExternalSyntheticLambda0
            @Override // com.android.server.timezonedetector.ConfigurationChangeListener
            public final void onChange() {
                TimeDetectorStrategyImpl.this.handleAutoTimeConfigChanged();
            }
        });
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy
    public synchronized void suggestExternalTime(ExternalTimeSuggestion timeSuggestion) {
        TimestampedValue<Long> newUnixEpochTime = timeSuggestion.getUnixEpochTime();
        if (validateAutoSuggestionTime(newUnixEpochTime, timeSuggestion)) {
            this.mLastExternalSuggestion.set(timeSuggestion);
            String reason = "External time suggestion received: suggestion=" + timeSuggestion;
            doAutoTimeDetection(reason);
        }
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy
    public synchronized void suggestGnssTime(GnssTimeSuggestion timeSuggestion) {
        TimestampedValue<Long> newUnixEpochTime = timeSuggestion.getUnixEpochTime();
        if (validateAutoSuggestionTime(newUnixEpochTime, timeSuggestion)) {
            this.mLastGnssSuggestion.set(timeSuggestion);
            String reason = "GNSS time suggestion received: suggestion=" + timeSuggestion;
            doAutoTimeDetection(reason);
        }
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy
    public synchronized boolean suggestManualTime(ManualTimeSuggestion suggestion) {
        TimestampedValue<Long> newUnixEpochTime = suggestion.getUnixEpochTime();
        if (!validateSuggestionTime(newUnixEpochTime, suggestion)) {
            return false;
        }
        String cause = "Manual time suggestion received: suggestion=" + suggestion;
        return setSystemClockIfRequired(2, newUnixEpochTime, cause);
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy
    public synchronized void suggestNetworkTime(NetworkTimeSuggestion timeSuggestion) {
        if (validateAutoSuggestionTime(timeSuggestion.getUnixEpochTime(), timeSuggestion)) {
            NetworkTimeSuggestion lastNetworkSuggestion = this.mLastNetworkSuggestion.get();
            if (lastNetworkSuggestion == null || !lastNetworkSuggestion.equals(timeSuggestion)) {
                this.mLastNetworkSuggestion.set(timeSuggestion);
            }
            String reason = "New network time suggested. timeSuggestion=" + timeSuggestion;
            doAutoTimeDetection(reason);
        }
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy
    public synchronized void suggestTelephonyTime(TelephonyTimeSuggestion timeSuggestion) {
        if (timeSuggestion.getUnixEpochTime() == null) {
            return;
        }
        if (validateAutoSuggestionTime(timeSuggestion.getUnixEpochTime(), timeSuggestion)) {
            if (storeTelephonySuggestion(timeSuggestion)) {
                String reason = "New telephony time suggested. timeSuggestion=" + timeSuggestion;
                doAutoTimeDetection(reason);
            }
        }
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategy
    public ConfigurationInternal getConfigurationInternal(int userId) {
        return this.mEnvironment.configurationInternal(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void handleAutoTimeConfigChanged() {
        boolean enabled = this.mEnvironment.isAutoTimeDetectionEnabled();
        if (enabled) {
            doAutoTimeDetection("Auto time zone detection config changed.");
        } else {
            this.mLastAutoSystemClockTimeSet = null;
        }
    }

    @Override // com.android.server.timezonedetector.Dumpable
    public synchronized void dump(IndentingPrintWriter ipw, String[] args) {
        ipw.println("TimeDetectorStrategy:");
        ipw.increaseIndent();
        ipw.println("mLastAutoSystemClockTimeSet=" + this.mLastAutoSystemClockTimeSet);
        ipw.println("mEnvironment.isAutoTimeDetectionEnabled()=" + this.mEnvironment.isAutoTimeDetectionEnabled());
        long elapsedRealtimeMillis = this.mEnvironment.elapsedRealtimeMillis();
        ipw.printf("mEnvironment.elapsedRealtimeMillis()=%s (%s)\n", new Object[]{Duration.ofMillis(elapsedRealtimeMillis), Long.valueOf(elapsedRealtimeMillis)});
        long systemClockMillis = this.mEnvironment.systemClockMillis();
        ipw.printf("mEnvironment.systemClockMillis()=%s (%s)\n", new Object[]{Instant.ofEpochMilli(systemClockMillis), Long.valueOf(systemClockMillis)});
        ipw.println("mEnvironment.systemClockUpdateThresholdMillis()=" + this.mEnvironment.systemClockUpdateThresholdMillis());
        Instant autoTimeLowerBound = this.mEnvironment.autoTimeLowerBound();
        ipw.printf("mEnvironment.autoTimeLowerBound()=%s (%s)\n", new Object[]{autoTimeLowerBound, Long.valueOf(autoTimeLowerBound.toEpochMilli())});
        String priorities = (String) Arrays.stream(this.mEnvironment.autoOriginPriorities()).mapToObj(new IntFunction() { // from class: com.android.server.timedetector.TimeDetectorStrategyImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.IntFunction
            public final Object apply(int i) {
                return TimeDetectorStrategy.originToString(i);
            }
        }).collect(Collectors.joining(",", "[", "]"));
        ipw.println("mEnvironment.autoOriginPriorities()=" + priorities);
        ipw.println("Time change log:");
        ipw.increaseIndent();
        this.mTimeChangesLog.dump(ipw);
        ipw.decreaseIndent();
        ipw.println("Telephony suggestion history:");
        ipw.increaseIndent();
        this.mSuggestionBySlotIndex.dump(ipw);
        ipw.decreaseIndent();
        ipw.println("Network suggestion history:");
        ipw.increaseIndent();
        this.mLastNetworkSuggestion.dump(ipw);
        ipw.decreaseIndent();
        ipw.println("Gnss suggestion history:");
        ipw.increaseIndent();
        this.mLastGnssSuggestion.dump(ipw);
        ipw.decreaseIndent();
        ipw.println("External suggestion history:");
        ipw.increaseIndent();
        this.mLastExternalSuggestion.dump(ipw);
        ipw.decreaseIndent();
        ipw.decreaseIndent();
    }

    private boolean storeTelephonySuggestion(TelephonyTimeSuggestion suggestion) {
        TimestampedValue<Long> newUnixEpochTime = suggestion.getUnixEpochTime();
        int slotIndex = suggestion.getSlotIndex();
        TelephonyTimeSuggestion previousSuggestion = this.mSuggestionBySlotIndex.get(Integer.valueOf(slotIndex));
        if (previousSuggestion != null) {
            if (previousSuggestion.getUnixEpochTime() == null || previousSuggestion.getUnixEpochTime().getValue() == null) {
                Slog.w(LOG_TAG, "Previous suggestion is null or has a null time. previousSuggestion=" + previousSuggestion + ", suggestion=" + suggestion);
                return false;
            }
            long referenceTimeDifference = TimestampedValue.referenceTimeDifference(newUnixEpochTime, previousSuggestion.getUnixEpochTime());
            if (referenceTimeDifference < 0) {
                Slog.w(LOG_TAG, "Out of order telephony suggestion received. referenceTimeDifference=" + referenceTimeDifference + " previousSuggestion=" + previousSuggestion + " suggestion=" + suggestion);
                return false;
            }
        }
        this.mSuggestionBySlotIndex.put(Integer.valueOf(slotIndex), suggestion);
        return true;
    }

    private boolean validateSuggestionTime(TimestampedValue<Long> newUnixEpochTime, Object suggestion) {
        if (newUnixEpochTime.getValue() == null) {
            Slog.w(LOG_TAG, "Suggested time value is null. suggestion=" + suggestion);
            return false;
        }
        long elapsedRealtimeMillis = this.mEnvironment.elapsedRealtimeMillis();
        if (elapsedRealtimeMillis < newUnixEpochTime.getReferenceTimeMillis()) {
            Slog.w(LOG_TAG, "New reference time is in the future? Ignoring. elapsedRealtimeMillis=" + elapsedRealtimeMillis + ", suggestion=" + suggestion);
            return false;
        }
        return true;
    }

    private boolean validateAutoSuggestionTime(TimestampedValue<Long> newUnixEpochTime, Object suggestion) {
        return validateSuggestionTime(newUnixEpochTime, suggestion) && validateSuggestionAgainstLowerBound(newUnixEpochTime, suggestion);
    }

    private boolean validateSuggestionAgainstLowerBound(TimestampedValue<Long> newUnixEpochTime, Object suggestion) {
        Instant lowerBound = this.mEnvironment.autoTimeLowerBound();
        if (lowerBound.isAfter(Instant.ofEpochMilli(((Long) newUnixEpochTime.getValue()).longValue()))) {
            Slog.w(LOG_TAG, "Suggestion points to time before lower bound, skipping it. suggestion=" + suggestion + ", lower bound=" + lowerBound);
            return false;
        }
        return true;
    }

    private void doAutoTimeDetection(String detectionReason) {
        if (!this.mEnvironment.isAutoTimeDetectionEnabled()) {
            return;
        }
        int[] originPriorities = this.mEnvironment.autoOriginPriorities();
        for (int origin : originPriorities) {
            TimestampedValue<Long> newUnixEpochTime = null;
            String cause = null;
            if (origin == 1) {
                TelephonyTimeSuggestion bestTelephonySuggestion = findBestTelephonySuggestion();
                if (bestTelephonySuggestion != null) {
                    newUnixEpochTime = bestTelephonySuggestion.getUnixEpochTime();
                    cause = "Found good telephony suggestion., bestTelephonySuggestion=" + bestTelephonySuggestion + ", detectionReason=" + detectionReason;
                }
            } else if (origin == 3) {
                NetworkTimeSuggestion networkSuggestion = findLatestValidNetworkSuggestion();
                if (networkSuggestion != null) {
                    newUnixEpochTime = networkSuggestion.getUnixEpochTime();
                    cause = "Found good network suggestion., networkSuggestion=" + networkSuggestion + ", detectionReason=" + detectionReason;
                }
            } else if (origin == 4) {
                GnssTimeSuggestion gnssTimeSuggestion = findLatestValidGnssSuggestion();
                if (gnssTimeSuggestion != null) {
                    newUnixEpochTime = gnssTimeSuggestion.getUnixEpochTime();
                    cause = "Found good gnss suggestion., gnssTimeSuggestion=" + gnssTimeSuggestion + ", detectionReason=" + detectionReason;
                }
            } else if (origin == 5) {
                ExternalTimeSuggestion externalTimeSuggestion = findLatestValidExternalSuggestion();
                if (externalTimeSuggestion != null) {
                    newUnixEpochTime = externalTimeSuggestion.getUnixEpochTime();
                    cause = "Found good external suggestion., externalTimeSuggestion=" + externalTimeSuggestion + ", detectionReason=" + detectionReason;
                }
            } else {
                Slog.w(LOG_TAG, "Unknown or unsupported origin=" + origin + " in " + Arrays.toString(originPriorities) + ": Skipping");
            }
            if (newUnixEpochTime != null) {
                setSystemClockIfRequired(origin, newUnixEpochTime, cause);
                return;
            }
        }
    }

    private TelephonyTimeSuggestion findBestTelephonySuggestion() {
        long elapsedRealtimeMillis = this.mEnvironment.elapsedRealtimeMillis();
        TelephonyTimeSuggestion bestSuggestion = null;
        int bestScore = -1;
        for (int i = 0; i < this.mSuggestionBySlotIndex.size(); i++) {
            Integer slotIndex = this.mSuggestionBySlotIndex.keyAt(i);
            TelephonyTimeSuggestion candidateSuggestion = this.mSuggestionBySlotIndex.valueAt(i);
            if (candidateSuggestion == null) {
                Slog.w(LOG_TAG, "Latest suggestion unexpectedly null for slotIndex. slotIndex=" + slotIndex);
            } else if (candidateSuggestion.getUnixEpochTime() == null) {
                Slog.w(LOG_TAG, "Latest suggestion unexpectedly empty.  candidateSuggestion=" + candidateSuggestion);
            } else {
                int candidateScore = scoreTelephonySuggestion(elapsedRealtimeMillis, candidateSuggestion);
                if (candidateScore != -1) {
                    if (bestSuggestion == null || bestScore < candidateScore) {
                        bestSuggestion = candidateSuggestion;
                        bestScore = candidateScore;
                    } else if (bestScore == candidateScore) {
                        int candidateSlotIndex = candidateSuggestion.getSlotIndex();
                        int bestSlotIndex = bestSuggestion.getSlotIndex();
                        if (candidateSlotIndex < bestSlotIndex) {
                            bestSuggestion = candidateSuggestion;
                        }
                    }
                }
            }
        }
        return bestSuggestion;
    }

    private static int scoreTelephonySuggestion(long elapsedRealtimeMillis, TelephonyTimeSuggestion timeSuggestion) {
        TimestampedValue<Long> unixEpochTime = timeSuggestion.getUnixEpochTime();
        if (!validateSuggestionUnixEpochTime(elapsedRealtimeMillis, unixEpochTime)) {
            Slog.w(LOG_TAG, "Existing suggestion found to be invalid elapsedRealtimeMillis=" + elapsedRealtimeMillis + ", timeSuggestion=" + timeSuggestion);
            return -1;
        }
        long ageMillis = elapsedRealtimeMillis - unixEpochTime.getReferenceTimeMillis();
        int bucketIndex = (int) (ageMillis / 3600000);
        if (bucketIndex >= 24) {
            return -1;
        }
        return 24 - bucketIndex;
    }

    private NetworkTimeSuggestion findLatestValidNetworkSuggestion() {
        NetworkTimeSuggestion networkSuggestion = this.mLastNetworkSuggestion.get();
        if (networkSuggestion == null) {
            return null;
        }
        TimestampedValue<Long> unixEpochTime = networkSuggestion.getUnixEpochTime();
        long elapsedRealTimeMillis = this.mEnvironment.elapsedRealtimeMillis();
        if (!validateSuggestionUnixEpochTime(elapsedRealTimeMillis, unixEpochTime)) {
            return null;
        }
        return networkSuggestion;
    }

    private GnssTimeSuggestion findLatestValidGnssSuggestion() {
        GnssTimeSuggestion gnssTimeSuggestion = this.mLastGnssSuggestion.get();
        if (gnssTimeSuggestion == null) {
            return null;
        }
        TimestampedValue<Long> unixEpochTime = gnssTimeSuggestion.getUnixEpochTime();
        long elapsedRealTimeMillis = this.mEnvironment.elapsedRealtimeMillis();
        if (!validateSuggestionUnixEpochTime(elapsedRealTimeMillis, unixEpochTime)) {
            return null;
        }
        return gnssTimeSuggestion;
    }

    private ExternalTimeSuggestion findLatestValidExternalSuggestion() {
        ExternalTimeSuggestion externalTimeSuggestion = this.mLastExternalSuggestion.get();
        if (externalTimeSuggestion == null) {
            return null;
        }
        TimestampedValue<Long> unixEpochTime = externalTimeSuggestion.getUnixEpochTime();
        long elapsedRealTimeMillis = this.mEnvironment.elapsedRealtimeMillis();
        if (!validateSuggestionUnixEpochTime(elapsedRealTimeMillis, unixEpochTime)) {
            return null;
        }
        return externalTimeSuggestion;
    }

    private boolean setSystemClockIfRequired(int origin, TimestampedValue<Long> time, String cause) {
        boolean isOriginAutomatic = isOriginAutomatic(origin);
        if (isOriginAutomatic) {
            if (!this.mEnvironment.isAutoTimeDetectionEnabled()) {
                return false;
            }
        } else if (this.mEnvironment.isAutoTimeDetectionEnabled()) {
            return false;
        }
        this.mEnvironment.acquireWakeLock();
        try {
            return setSystemClockUnderWakeLock(origin, time, cause);
        } finally {
            this.mEnvironment.releaseWakeLock();
        }
    }

    private static boolean isOriginAutomatic(int origin) {
        return origin != 2;
    }

    private boolean setSystemClockUnderWakeLock(int origin, TimestampedValue<Long> newTime, String cause) {
        TimestampedValue<Long> timestampedValue;
        long elapsedRealtimeMillis = this.mEnvironment.elapsedRealtimeMillis();
        boolean isOriginAutomatic = isOriginAutomatic(origin);
        long actualSystemClockMillis = this.mEnvironment.systemClockMillis();
        if (isOriginAutomatic && (timestampedValue = this.mLastAutoSystemClockTimeSet) != null) {
            long expectedTimeMillis = TimeDetectorStrategy.getTimeAt(timestampedValue, elapsedRealtimeMillis);
            long absSystemClockDifference = Math.abs(expectedTimeMillis - actualSystemClockMillis);
            if (absSystemClockDifference > SYSTEM_CLOCK_PARANOIA_THRESHOLD_MILLIS) {
                Slog.w(LOG_TAG, "System clock has not tracked elapsed real time clock. A clock may be inaccurate or something unexpectedly set the system clock. elapsedRealtimeMillis=" + elapsedRealtimeMillis + " expectedTimeMillis=" + expectedTimeMillis + " actualTimeMillis=" + actualSystemClockMillis + " cause=" + cause);
            }
        }
        long newSystemClockMillis = TimeDetectorStrategy.getTimeAt(newTime, elapsedRealtimeMillis);
        long absTimeDifference = Math.abs(newSystemClockMillis - actualSystemClockMillis);
        long systemClockUpdateThreshold = this.mEnvironment.systemClockUpdateThresholdMillis();
        if (absTimeDifference < systemClockUpdateThreshold) {
            return true;
        }
        this.mEnvironment.setSystemClock(newSystemClockMillis);
        String logMsg = "Set system clock using time=" + newTime + " cause=" + cause + " elapsedRealtimeMillis=" + elapsedRealtimeMillis + " (old) actualSystemClockMillis=" + actualSystemClockMillis + " newSystemClockMillis=" + newSystemClockMillis;
        this.mTimeChangesLog.log(logMsg);
        if (!isOriginAutomatic(origin)) {
            this.mLastAutoSystemClockTimeSet = null;
        } else {
            this.mLastAutoSystemClockTimeSet = newTime;
        }
        return true;
    }

    public synchronized TelephonyTimeSuggestion findBestTelephonySuggestionForTests() {
        return findBestTelephonySuggestion();
    }

    public synchronized NetworkTimeSuggestion findLatestValidNetworkSuggestionForTests() {
        return findLatestValidNetworkSuggestion();
    }

    public synchronized GnssTimeSuggestion findLatestValidGnssSuggestionForTests() {
        return findLatestValidGnssSuggestion();
    }

    public synchronized ExternalTimeSuggestion findLatestValidExternalSuggestionForTests() {
        return findLatestValidExternalSuggestion();
    }

    public synchronized TelephonyTimeSuggestion getLatestTelephonySuggestion(int slotIndex) {
        return this.mSuggestionBySlotIndex.get(Integer.valueOf(slotIndex));
    }

    public synchronized NetworkTimeSuggestion getLatestNetworkSuggestion() {
        return this.mLastNetworkSuggestion.get();
    }

    public synchronized GnssTimeSuggestion getLatestGnssSuggestion() {
        return this.mLastGnssSuggestion.get();
    }

    public synchronized ExternalTimeSuggestion getLatestExternalSuggestion() {
        return this.mLastExternalSuggestion.get();
    }

    private static boolean validateSuggestionUnixEpochTime(long elapsedRealtimeMillis, TimestampedValue<Long> unixEpochTime) {
        long referenceTimeMillis = unixEpochTime.getReferenceTimeMillis();
        if (referenceTimeMillis > elapsedRealtimeMillis) {
            return false;
        }
        long ageMillis = elapsedRealtimeMillis - referenceTimeMillis;
        return ageMillis <= 86400000;
    }
}
