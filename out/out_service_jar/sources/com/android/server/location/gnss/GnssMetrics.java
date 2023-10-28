package com.android.server.location.gnss;

import android.app.StatsManager;
import android.content.Context;
import android.location.GnssStatus;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.connectivity.GpsBatteryStats;
import android.util.Base64;
import android.util.Log;
import android.util.StatsEvent;
import android.util.TimeUtils;
import com.android.internal.app.IBatteryStats;
import com.android.internal.location.nano.GnssLogsProto;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.location.gnss.hal.GnssNative;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
/* loaded from: classes.dex */
public class GnssMetrics {
    private static final int CONVERT_MILLI_TO_MICRO = 1000;
    private static final int DEFAULT_TIME_BETWEEN_FIXES_MILLISECS = 1000;
    private static final double L5_CARRIER_FREQ_RANGE_HIGH_HZ = 1.189E9d;
    private static final double L5_CARRIER_FREQ_RANGE_LOW_HZ = 1.164E9d;
    private static final String TAG = "GnssMetrics";
    private static final int VENDOR_SPECIFIC_POWER_MODES_SIZE = 10;
    private boolean[] mConstellationTypes;
    private final GnssNative mGnssNative;
    GnssPowerMetrics mGnssPowerMetrics;
    long mL5SvStatusReports;
    long mL5SvStatusReportsUsedInFix;
    Statistics mL5TopFourAverageCn0DbmHzReportsStatistics;
    Statistics mLocationFailureReportsStatistics;
    private long mLogStartInElapsedRealtimeMs;
    private int mNumL5SvStatus;
    private int mNumL5SvStatusUsedInFix;
    private int mNumSvStatus;
    private int mNumSvStatusUsedInFix;
    Statistics mPositionAccuracyMetersReportsStatistics;
    private final StatsManager mStatsManager;
    long mSvStatusReports;
    long mSvStatusReportsUsedInFix;
    Statistics mTimeToFirstFixMilliSReportsStatistics;
    Statistics mTopFourAverageCn0DbmHzReportsStatistics;
    private final Statistics mLocationFailureStatistics = new Statistics();
    private final Statistics mTimeToFirstFixSecStatistics = new Statistics();
    private final Statistics mPositionAccuracyMeterStatistics = new Statistics();
    private final Statistics mTopFourAverageCn0Statistics = new Statistics();
    private final Statistics mTopFourAverageCn0StatisticsL5 = new Statistics();

    public GnssMetrics(Context context, IBatteryStats stats, GnssNative gnssNative) {
        this.mGnssNative = gnssNative;
        this.mGnssPowerMetrics = new GnssPowerMetrics(stats);
        reset();
        this.mLocationFailureReportsStatistics = new Statistics();
        this.mTimeToFirstFixMilliSReportsStatistics = new Statistics();
        this.mPositionAccuracyMetersReportsStatistics = new Statistics();
        this.mTopFourAverageCn0DbmHzReportsStatistics = new Statistics();
        this.mL5TopFourAverageCn0DbmHzReportsStatistics = new Statistics();
        this.mStatsManager = (StatsManager) context.getSystemService("stats");
        registerGnssStats();
    }

    public void logReceivedLocationStatus(boolean isSuccessful) {
        if (!isSuccessful) {
            this.mLocationFailureStatistics.addItem(1.0d);
            this.mLocationFailureReportsStatistics.addItem(1.0d);
            return;
        }
        this.mLocationFailureStatistics.addItem(0.0d);
        this.mLocationFailureReportsStatistics.addItem(0.0d);
    }

    public void logMissedReports(int desiredTimeBetweenFixesMilliSeconds, int actualTimeBetweenFixesMilliSeconds) {
        int numReportMissed = (actualTimeBetweenFixesMilliSeconds / Math.max(1000, desiredTimeBetweenFixesMilliSeconds)) - 1;
        if (numReportMissed > 0) {
            for (int i = 0; i < numReportMissed; i++) {
                this.mLocationFailureStatistics.addItem(1.0d);
                this.mLocationFailureReportsStatistics.addItem(1.0d);
            }
        }
    }

    public void logTimeToFirstFixMilliSecs(int timeToFirstFixMilliSeconds) {
        this.mTimeToFirstFixSecStatistics.addItem(timeToFirstFixMilliSeconds / 1000.0d);
        this.mTimeToFirstFixMilliSReportsStatistics.addItem(timeToFirstFixMilliSeconds);
    }

    public void logPositionAccuracyMeters(float positionAccuracyMeters) {
        this.mPositionAccuracyMeterStatistics.addItem(positionAccuracyMeters);
        this.mPositionAccuracyMetersReportsStatistics.addItem(positionAccuracyMeters);
    }

    public void logCn0(GnssStatus gnssStatus) {
        logCn0L5(gnssStatus);
        if (gnssStatus.getSatelliteCount() == 0) {
            this.mGnssPowerMetrics.reportSignalQuality(null);
            return;
        }
        float[] cn0DbHzs = new float[gnssStatus.getSatelliteCount()];
        for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
            cn0DbHzs[i] = gnssStatus.getCn0DbHz(i);
        }
        Arrays.sort(cn0DbHzs);
        this.mGnssPowerMetrics.reportSignalQuality(cn0DbHzs);
        if (cn0DbHzs.length >= 4 && cn0DbHzs[cn0DbHzs.length - 4] > 0.0d) {
            double top4AvgCn0 = 0.0d;
            for (int i2 = cn0DbHzs.length - 4; i2 < cn0DbHzs.length; i2++) {
                top4AvgCn0 += cn0DbHzs[i2];
            }
            double top4AvgCn02 = top4AvgCn0 / 4.0d;
            this.mTopFourAverageCn0Statistics.addItem(top4AvgCn02);
            this.mTopFourAverageCn0DbmHzReportsStatistics.addItem(1000.0d * top4AvgCn02);
        }
    }

    private static boolean isL5Sv(float carrierFreq) {
        return ((double) carrierFreq) >= L5_CARRIER_FREQ_RANGE_LOW_HZ && ((double) carrierFreq) <= L5_CARRIER_FREQ_RANGE_HIGH_HZ;
    }

    public void logSvStatus(GnssStatus status) {
        for (int i = 0; i < status.getSatelliteCount(); i++) {
            if (status.hasCarrierFrequencyHz(i)) {
                this.mNumSvStatus++;
                this.mSvStatusReports++;
                boolean isL5 = isL5Sv(status.getCarrierFrequencyHz(i));
                if (isL5) {
                    this.mNumL5SvStatus++;
                    this.mL5SvStatusReports++;
                }
                if (status.usedInFix(i)) {
                    this.mNumSvStatusUsedInFix++;
                    this.mSvStatusReportsUsedInFix++;
                    if (isL5) {
                        this.mNumL5SvStatusUsedInFix++;
                        this.mL5SvStatusReportsUsedInFix++;
                    }
                }
            }
        }
    }

    private void logCn0L5(GnssStatus gnssStatus) {
        if (gnssStatus.getSatelliteCount() == 0) {
            return;
        }
        ArrayList<Float> l5Cn0DbHzs = new ArrayList<>(gnssStatus.getSatelliteCount());
        for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
            if (isL5Sv(gnssStatus.getCarrierFrequencyHz(i))) {
                l5Cn0DbHzs.add(Float.valueOf(gnssStatus.getCn0DbHz(i)));
            }
        }
        int i2 = l5Cn0DbHzs.size();
        if (i2 < 4) {
            return;
        }
        Collections.sort(l5Cn0DbHzs);
        if (l5Cn0DbHzs.get(l5Cn0DbHzs.size() - 4).floatValue() > 0.0d) {
            double top4AvgCn0 = 0.0d;
            for (int i3 = l5Cn0DbHzs.size() - 4; i3 < l5Cn0DbHzs.size(); i3++) {
                top4AvgCn0 += l5Cn0DbHzs.get(i3).floatValue();
            }
            double top4AvgCn02 = top4AvgCn0 / 4.0d;
            this.mTopFourAverageCn0StatisticsL5.addItem(top4AvgCn02);
            this.mL5TopFourAverageCn0DbmHzReportsStatistics.addItem(1000.0d * top4AvgCn02);
        }
    }

    public void logConstellationType(int constellationType) {
        boolean[] zArr = this.mConstellationTypes;
        if (constellationType >= zArr.length) {
            Log.e(TAG, "Constellation type " + constellationType + " is not valid.");
        } else {
            zArr[constellationType] = true;
        }
    }

    public String dumpGnssMetricsAsProtoString() {
        GnssLogsProto.GnssLog msg = new GnssLogsProto.GnssLog();
        if (this.mLocationFailureStatistics.getCount() > 0) {
            msg.numLocationReportProcessed = this.mLocationFailureStatistics.getCount();
            msg.percentageLocationFailure = (int) (this.mLocationFailureStatistics.getMean() * 100.0d);
        }
        if (this.mTimeToFirstFixSecStatistics.getCount() > 0) {
            msg.numTimeToFirstFixProcessed = this.mTimeToFirstFixSecStatistics.getCount();
            msg.meanTimeToFirstFixSecs = (int) this.mTimeToFirstFixSecStatistics.getMean();
            msg.standardDeviationTimeToFirstFixSecs = (int) this.mTimeToFirstFixSecStatistics.getStandardDeviation();
        }
        if (this.mPositionAccuracyMeterStatistics.getCount() > 0) {
            msg.numPositionAccuracyProcessed = this.mPositionAccuracyMeterStatistics.getCount();
            msg.meanPositionAccuracyMeters = (int) this.mPositionAccuracyMeterStatistics.getMean();
            msg.standardDeviationPositionAccuracyMeters = (int) this.mPositionAccuracyMeterStatistics.getStandardDeviation();
        }
        if (this.mTopFourAverageCn0Statistics.getCount() > 0) {
            msg.numTopFourAverageCn0Processed = this.mTopFourAverageCn0Statistics.getCount();
            msg.meanTopFourAverageCn0DbHz = this.mTopFourAverageCn0Statistics.getMean();
            msg.standardDeviationTopFourAverageCn0DbHz = this.mTopFourAverageCn0Statistics.getStandardDeviation();
        }
        int i = this.mNumSvStatus;
        if (i > 0) {
            msg.numSvStatusProcessed = i;
        }
        int i2 = this.mNumL5SvStatus;
        if (i2 > 0) {
            msg.numL5SvStatusProcessed = i2;
        }
        int i3 = this.mNumSvStatusUsedInFix;
        if (i3 > 0) {
            msg.numSvStatusUsedInFix = i3;
        }
        int i4 = this.mNumL5SvStatusUsedInFix;
        if (i4 > 0) {
            msg.numL5SvStatusUsedInFix = i4;
        }
        if (this.mTopFourAverageCn0StatisticsL5.getCount() > 0) {
            msg.numL5TopFourAverageCn0Processed = this.mTopFourAverageCn0StatisticsL5.getCount();
            msg.meanL5TopFourAverageCn0DbHz = this.mTopFourAverageCn0StatisticsL5.getMean();
            msg.standardDeviationL5TopFourAverageCn0DbHz = this.mTopFourAverageCn0StatisticsL5.getStandardDeviation();
        }
        msg.powerMetrics = this.mGnssPowerMetrics.buildProto();
        msg.hardwareRevision = SystemProperties.get("ro.boot.revision", "");
        String s = Base64.encodeToString(GnssLogsProto.GnssLog.toByteArray(msg), 0);
        reset();
        return s;
    }

    public String dumpGnssMetricsAsText() {
        StringBuilder s = new StringBuilder();
        s.append("GNSS_KPI_START").append('\n');
        s.append("  KPI logging start time: ");
        TimeUtils.formatDuration(this.mLogStartInElapsedRealtimeMs, s);
        s.append("\n");
        s.append("  KPI logging end time: ");
        TimeUtils.formatDuration(SystemClock.elapsedRealtime(), s);
        s.append("\n");
        s.append("  Number of location reports: ").append(this.mLocationFailureStatistics.getCount()).append("\n");
        if (this.mLocationFailureStatistics.getCount() > 0) {
            s.append("  Percentage location failure: ").append(this.mLocationFailureStatistics.getMean() * 100.0d).append("\n");
        }
        s.append("  Number of TTFF reports: ").append(this.mTimeToFirstFixSecStatistics.getCount()).append("\n");
        if (this.mTimeToFirstFixSecStatistics.getCount() > 0) {
            s.append("  TTFF mean (sec): ").append(this.mTimeToFirstFixSecStatistics.getMean()).append("\n");
            s.append("  TTFF standard deviation (sec): ").append(this.mTimeToFirstFixSecStatistics.getStandardDeviation()).append("\n");
        }
        s.append("  Number of position accuracy reports: ").append(this.mPositionAccuracyMeterStatistics.getCount()).append("\n");
        if (this.mPositionAccuracyMeterStatistics.getCount() > 0) {
            s.append("  Position accuracy mean (m): ").append(this.mPositionAccuracyMeterStatistics.getMean()).append("\n");
            s.append("  Position accuracy standard deviation (m): ").append(this.mPositionAccuracyMeterStatistics.getStandardDeviation()).append("\n");
        }
        s.append("  Number of CN0 reports: ").append(this.mTopFourAverageCn0Statistics.getCount()).append("\n");
        if (this.mTopFourAverageCn0Statistics.getCount() > 0) {
            s.append("  Top 4 Avg CN0 mean (dB-Hz): ").append(this.mTopFourAverageCn0Statistics.getMean()).append("\n");
            s.append("  Top 4 Avg CN0 standard deviation (dB-Hz): ").append(this.mTopFourAverageCn0Statistics.getStandardDeviation()).append("\n");
        }
        s.append("  Total number of sv status messages processed: ").append(this.mNumSvStatus).append("\n");
        s.append("  Total number of L5 sv status messages processed: ").append(this.mNumL5SvStatus).append("\n");
        s.append("  Total number of sv status messages processed, where sv is used in fix: ").append(this.mNumSvStatusUsedInFix).append("\n");
        s.append("  Total number of L5 sv status messages processed, where sv is used in fix: ").append(this.mNumL5SvStatusUsedInFix).append("\n");
        s.append("  Number of L5 CN0 reports: ").append(this.mTopFourAverageCn0StatisticsL5.getCount()).append("\n");
        if (this.mTopFourAverageCn0StatisticsL5.getCount() > 0) {
            s.append("  L5 Top 4 Avg CN0 mean (dB-Hz): ").append(this.mTopFourAverageCn0StatisticsL5.getMean()).append("\n");
            s.append("  L5 Top 4 Avg CN0 standard deviation (dB-Hz): ").append(this.mTopFourAverageCn0StatisticsL5.getStandardDeviation()).append("\n");
        }
        s.append("  Used-in-fix constellation types: ");
        int i = 0;
        while (true) {
            boolean[] zArr = this.mConstellationTypes;
            if (i >= zArr.length) {
                break;
            }
            if (zArr[i]) {
                s.append(GnssStatus.constellationTypeToString(i)).append(" ");
            }
            i++;
        }
        s.append("\n");
        s.append("GNSS_KPI_END").append("\n");
        GpsBatteryStats stats = this.mGnssPowerMetrics.getGpsBatteryStats();
        if (stats != null) {
            s.append("Power Metrics").append("\n");
            s.append("  Time on battery (min): ").append(stats.getLoggingDurationMs() / 60000.0d).append("\n");
            long[] t = stats.getTimeInGpsSignalQualityLevel();
            if (t != null && t.length == 2) {
                s.append("  Amount of time (while on battery) Top 4 Avg CN0 > 20.0 dB-Hz (min): ").append(t[1] / 60000.0d).append("\n");
                s.append("  Amount of time (while on battery) Top 4 Avg CN0 <= 20.0 dB-Hz (min): ").append(t[0] / 60000.0d).append("\n");
            }
            s.append("  Energy consumed while on battery (mAh): ").append(stats.getEnergyConsumedMaMs() / 3600000.0d).append("\n");
        }
        s.append("Hardware Version: ").append(SystemProperties.get("ro.boot.revision", "")).append("\n");
        return s.toString();
    }

    private void reset() {
        this.mLogStartInElapsedRealtimeMs = SystemClock.elapsedRealtime();
        this.mLocationFailureStatistics.reset();
        this.mTimeToFirstFixSecStatistics.reset();
        this.mPositionAccuracyMeterStatistics.reset();
        this.mTopFourAverageCn0Statistics.reset();
        resetConstellationTypes();
        this.mTopFourAverageCn0StatisticsL5.reset();
        this.mNumSvStatus = 0;
        this.mNumL5SvStatus = 0;
        this.mNumSvStatusUsedInFix = 0;
        this.mNumL5SvStatusUsedInFix = 0;
    }

    public void resetConstellationTypes() {
        this.mConstellationTypes = new boolean[8];
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Statistics {
        private int mCount;
        private long mLongSum;
        private double mSum;
        private double mSumSquare;

        Statistics() {
        }

        public synchronized void reset() {
            this.mCount = 0;
            this.mSum = 0.0d;
            this.mSumSquare = 0.0d;
            this.mLongSum = 0L;
        }

        public synchronized void addItem(double item) {
            this.mCount++;
            this.mSum += item;
            this.mSumSquare += item * item;
            this.mLongSum = (long) (this.mLongSum + item);
        }

        public synchronized int getCount() {
            return this.mCount;
        }

        public synchronized double getMean() {
            return this.mSum / this.mCount;
        }

        public synchronized double getStandardDeviation() {
            double d = this.mSum;
            int i = this.mCount;
            double m = d / i;
            double m2 = m * m;
            double v = this.mSumSquare / i;
            if (v > m2) {
                return Math.sqrt(v - m2);
            }
            return 0.0d;
        }

        public synchronized long getLongSum() {
            return this.mLongSum;
        }
    }

    /* loaded from: classes.dex */
    private class GnssPowerMetrics {
        public static final double POOR_TOP_FOUR_AVG_CN0_THRESHOLD_DB_HZ = 20.0d;
        private static final double REPORTING_THRESHOLD_DB_HZ = 1.0d;
        private final IBatteryStats mBatteryStats;
        private double mLastAverageCn0 = -100.0d;
        private int mLastSignalLevel = -1;

        GnssPowerMetrics(IBatteryStats stats) {
            this.mBatteryStats = stats;
        }

        public GnssLogsProto.PowerMetrics buildProto() {
            GnssLogsProto.PowerMetrics p = new GnssLogsProto.PowerMetrics();
            GpsBatteryStats stats = GnssMetrics.this.mGnssPowerMetrics.getGpsBatteryStats();
            if (stats != null) {
                p.loggingDurationMs = stats.getLoggingDurationMs();
                p.energyConsumedMah = stats.getEnergyConsumedMaMs() / 3600000.0d;
                long[] t = stats.getTimeInGpsSignalQualityLevel();
                p.timeInSignalQualityLevelMs = new long[t.length];
                System.arraycopy(t, 0, p.timeInSignalQualityLevelMs, 0, t.length);
            }
            return p;
        }

        public GpsBatteryStats getGpsBatteryStats() {
            try {
                return this.mBatteryStats.getGpsBatteryStats();
            } catch (RemoteException e) {
                Log.w(GnssMetrics.TAG, e);
                return null;
            }
        }

        public void reportSignalQuality(float[] sortedCn0DbHzs) {
            double avgCn0 = 0.0d;
            if (sortedCn0DbHzs != null && sortedCn0DbHzs.length > 0) {
                for (int i = Math.max(0, sortedCn0DbHzs.length - 4); i < sortedCn0DbHzs.length; i++) {
                    avgCn0 += sortedCn0DbHzs[i];
                }
                int i2 = sortedCn0DbHzs.length;
                avgCn0 /= Math.min(i2, 4);
            }
            if (Math.abs(avgCn0 - this.mLastAverageCn0) < REPORTING_THRESHOLD_DB_HZ) {
                return;
            }
            int signalLevel = getSignalLevel(avgCn0);
            if (signalLevel != this.mLastSignalLevel) {
                FrameworkStatsLog.write(69, signalLevel);
                this.mLastSignalLevel = signalLevel;
            }
            try {
                this.mBatteryStats.noteGpsSignalQuality(signalLevel);
                this.mLastAverageCn0 = avgCn0;
            } catch (RemoteException e) {
                Log.w(GnssMetrics.TAG, e);
            }
        }

        private int getSignalLevel(double cn0) {
            if (cn0 > 20.0d) {
                return 1;
            }
            return 0;
        }
    }

    private void registerGnssStats() {
        StatsPullAtomCallbackImpl pullAtomCallback = new StatsPullAtomCallbackImpl();
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.GNSS_STATS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, pullAtomCallback);
        this.mStatsManager.setPullAtomCallback((int) FrameworkStatsLog.GNSS_POWER_STATS, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, pullAtomCallback);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class StatsPullAtomCallbackImpl implements StatsManager.StatsPullAtomCallback {
        StatsPullAtomCallbackImpl() {
        }

        public int onPullAtom(int atomTag, List<StatsEvent> data) {
            if (atomTag == 10074) {
                data.add(FrameworkStatsLog.buildStatsEvent(atomTag, GnssMetrics.this.mLocationFailureReportsStatistics.getCount(), GnssMetrics.this.mLocationFailureReportsStatistics.getLongSum(), GnssMetrics.this.mTimeToFirstFixMilliSReportsStatistics.getCount(), GnssMetrics.this.mTimeToFirstFixMilliSReportsStatistics.getLongSum(), GnssMetrics.this.mPositionAccuracyMetersReportsStatistics.getCount(), GnssMetrics.this.mPositionAccuracyMetersReportsStatistics.getLongSum(), GnssMetrics.this.mTopFourAverageCn0DbmHzReportsStatistics.getCount(), GnssMetrics.this.mTopFourAverageCn0DbmHzReportsStatistics.getLongSum(), GnssMetrics.this.mL5TopFourAverageCn0DbmHzReportsStatistics.getCount(), GnssMetrics.this.mL5TopFourAverageCn0DbmHzReportsStatistics.getLongSum(), GnssMetrics.this.mSvStatusReports, GnssMetrics.this.mSvStatusReportsUsedInFix, GnssMetrics.this.mL5SvStatusReports, GnssMetrics.this.mL5SvStatusReportsUsedInFix));
                return 0;
            } else if (atomTag != 10101) {
                throw new UnsupportedOperationException("Unknown tagId = " + atomTag);
            } else {
                GnssMetrics.this.mGnssNative.requestPowerStats();
                GnssPowerStats gnssPowerStats = GnssMetrics.this.mGnssNative.getPowerStats();
                if (gnssPowerStats == null) {
                    return 1;
                }
                double[] otherModesEnergyMilliJoule = new double[10];
                double[] tempGnssPowerStatsOtherModes = gnssPowerStats.getOtherModesEnergyMilliJoule();
                if (tempGnssPowerStatsOtherModes.length >= 10) {
                    System.arraycopy(tempGnssPowerStatsOtherModes, 0, otherModesEnergyMilliJoule, 0, 10);
                } else {
                    System.arraycopy(tempGnssPowerStatsOtherModes, 0, otherModesEnergyMilliJoule, 0, tempGnssPowerStatsOtherModes.length);
                }
                data.add(FrameworkStatsLog.buildStatsEvent(atomTag, (long) gnssPowerStats.getElapsedRealtimeUncertaintyNanos(), (long) (gnssPowerStats.getTotalEnergyMilliJoule() * 1000.0d), (long) (gnssPowerStats.getSinglebandTrackingModeEnergyMilliJoule() * 1000.0d), (long) (gnssPowerStats.getMultibandTrackingModeEnergyMilliJoule() * 1000.0d), (long) (gnssPowerStats.getSinglebandAcquisitionModeEnergyMilliJoule() * 1000.0d), (long) (gnssPowerStats.getMultibandAcquisitionModeEnergyMilliJoule() * 1000.0d), (long) (otherModesEnergyMilliJoule[0] * 1000.0d), (long) (otherModesEnergyMilliJoule[1] * 1000.0d), (long) (otherModesEnergyMilliJoule[2] * 1000.0d), (long) (otherModesEnergyMilliJoule[3] * 1000.0d), (long) (otherModesEnergyMilliJoule[4] * 1000.0d), (long) (otherModesEnergyMilliJoule[5] * 1000.0d), (long) (otherModesEnergyMilliJoule[6] * 1000.0d), (long) (otherModesEnergyMilliJoule[7] * 1000.0d), (long) (otherModesEnergyMilliJoule[8] * 1000.0d), (long) (otherModesEnergyMilliJoule[9] * 1000.0d)));
                return 0;
            }
        }
    }
}
