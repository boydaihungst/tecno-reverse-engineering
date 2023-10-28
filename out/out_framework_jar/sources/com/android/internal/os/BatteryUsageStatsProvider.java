package com.android.internal.os;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.BatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;
import android.os.Parcel;
import android.os.SystemClock;
import android.os.UidBatteryConsumer;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes4.dex */
public class BatteryUsageStatsProvider {
    private static final String TAG = "BatteryUsageStatsProv";
    private static boolean sErrorReported;
    private final BatteryUsageStatsStore mBatteryUsageStatsStore;
    private final Context mContext;
    private final Object mLock;
    private List<PowerCalculator> mPowerCalculators;
    private final PowerProfile mPowerProfile;
    private final BatteryStats mStats;

    public BatteryUsageStatsProvider(Context context, BatteryStats stats) {
        this(context, stats, null);
    }

    public BatteryUsageStatsProvider(Context context, BatteryStats stats, BatteryUsageStatsStore batteryUsageStatsStore) {
        PowerProfile powerProfile;
        this.mLock = new Object();
        this.mContext = context;
        this.mStats = stats;
        this.mBatteryUsageStatsStore = batteryUsageStatsStore;
        if (stats instanceof BatteryStatsImpl) {
            powerProfile = ((BatteryStatsImpl) stats).getPowerProfile();
        } else {
            powerProfile = new PowerProfile(context);
        }
        this.mPowerProfile = powerProfile;
    }

    private List<PowerCalculator> getPowerCalculators() {
        synchronized (this.mLock) {
            if (this.mPowerCalculators == null) {
                ArrayList arrayList = new ArrayList();
                this.mPowerCalculators = arrayList;
                arrayList.add(new BatteryChargeCalculator());
                this.mPowerCalculators.add(new CpuPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new MemoryPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new WakelockPowerCalculator(this.mPowerProfile));
                if (!BatteryStats.checkWifiOnly(this.mContext)) {
                    this.mPowerCalculators.add(new MobileRadioPowerCalculator(this.mPowerProfile));
                }
                this.mPowerCalculators.add(new WifiPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new BluetoothPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new SensorPowerCalculator((SensorManager) this.mContext.getSystemService(SensorManager.class)));
                this.mPowerCalculators.add(new GnssPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new CameraPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new FlashlightPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new AudioPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new VideoPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new PhonePowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new ScreenPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new AmbientDisplayPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new IdlePowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new CustomMeasuredPowerCalculator(this.mPowerProfile));
                this.mPowerCalculators.add(new UserPowerCalculator());
                this.mPowerCalculators.add(new SystemServicePowerCalculator(this.mPowerProfile));
            }
        }
        return this.mPowerCalculators;
    }

    public boolean shouldUpdateStats(List<BatteryUsageStatsQuery> queries, long lastUpdateTimeStampMs) {
        long allowableStatsAge = Long.MAX_VALUE;
        for (int i = queries.size() - 1; i >= 0; i--) {
            BatteryUsageStatsQuery query = queries.get(i);
            allowableStatsAge = Math.min(allowableStatsAge, query.getMaxStatsAge());
        }
        return elapsedRealtime() - lastUpdateTimeStampMs > allowableStatsAge;
    }

    public List<BatteryUsageStats> getBatteryUsageStats(List<BatteryUsageStatsQuery> queries) {
        ArrayList<BatteryUsageStats> results = new ArrayList<>(queries.size());
        synchronized (this.mStats) {
            this.mStats.prepareForDumpLocked();
            long currentTimeMillis = currentTimeMillis();
            for (int i = 0; i < queries.size(); i++) {
                results.add(getBatteryUsageStats(queries.get(i), currentTimeMillis));
            }
        }
        return results;
    }

    public BatteryUsageStats getBatteryUsageStats(BatteryUsageStatsQuery query) {
        BatteryUsageStats batteryUsageStats;
        synchronized (this.mStats) {
            batteryUsageStats = getBatteryUsageStats(query, currentTimeMillis());
        }
        return batteryUsageStats;
    }

    private BatteryUsageStats getBatteryUsageStats(BatteryUsageStatsQuery query, long currentTimeMs) {
        if (query.getToTimestamp() == 0) {
            return getCurrentBatteryUsageStats(query, currentTimeMs);
        }
        return getAggregatedBatteryUsageStats(query);
    }

    private BatteryUsageStats getCurrentBatteryUsageStats(BatteryUsageStatsQuery query, long currentTimeMs) {
        SparseArray<? extends BatteryStats.Uid> uidStats;
        int i;
        List<PowerCalculator> powerCalculators;
        int count;
        long realtimeUs;
        BatteryUsageStats.Builder batteryUsageStatsBuilder;
        boolean include;
        long realtimeUs2 = elapsedRealtime() * 1000;
        long uptimeUs = 1000 * uptimeMillis();
        boolean includePowerModels = (query.getFlags() & 4) != 0;
        boolean includeProcessStateData = (query.getFlags() & 8) != 0 && this.mStats.isProcessStateDataAvailable();
        boolean includeVirtualUids = (query.getFlags() & 16) != 0;
        BatteryUsageStats.Builder batteryUsageStatsBuilder2 = new BatteryUsageStats.Builder(this.mStats.getCustomEnergyConsumerNames(), includePowerModels, includeProcessStateData);
        batteryUsageStatsBuilder2.setStatsStartTimestamp(this.mStats.getStartClockTime());
        batteryUsageStatsBuilder2.setStatsEndTimestamp(currentTimeMs);
        SparseArray<? extends BatteryStats.Uid> uidStats2 = this.mStats.getUidStats();
        for (int i2 = uidStats2.size() - 1; i2 >= 0; i2--) {
            BatteryStats.Uid uid = uidStats2.valueAt(i2);
            if (includeVirtualUids || uid.getUid() != 1090) {
                batteryUsageStatsBuilder2.getOrCreateUidBatteryConsumerBuilder(uid).setTimeInStateMs(1, getProcessBackgroundTimeMs(uid, realtimeUs2)).setTimeInStateMs(0, getProcessForegroundTimeMs(uid, realtimeUs2));
            }
        }
        int[] powerComponents = query.getPowerComponents();
        List<PowerCalculator> powerCalculators2 = getPowerCalculators();
        int count2 = powerCalculators2.size();
        int i3 = 0;
        while (i3 < count2) {
            PowerCalculator powerCalculator = powerCalculators2.get(i3);
            if (powerComponents != null) {
                boolean include2 = false;
                int j = 0;
                while (true) {
                    boolean include3 = include2;
                    if (j < powerComponents.length) {
                        if (!powerCalculator.isPowerComponentSupported(powerComponents[j])) {
                            j++;
                            include2 = include3;
                        } else {
                            include = true;
                            break;
                        }
                    } else {
                        include = include3;
                        break;
                    }
                }
                if (!include) {
                    realtimeUs = realtimeUs2;
                    uidStats = uidStats2;
                    i = i3;
                    powerCalculators = powerCalculators2;
                    count = count2;
                    batteryUsageStatsBuilder = batteryUsageStatsBuilder2;
                    i3 = i + 1;
                    batteryUsageStatsBuilder2 = batteryUsageStatsBuilder;
                    uidStats2 = uidStats;
                    powerCalculators2 = powerCalculators;
                    count2 = count;
                    realtimeUs2 = realtimeUs;
                }
            }
            uidStats = uidStats2;
            i = i3;
            long j2 = realtimeUs2;
            powerCalculators = powerCalculators2;
            count = count2;
            realtimeUs = realtimeUs2;
            batteryUsageStatsBuilder = batteryUsageStatsBuilder2;
            powerCalculator.calculate(batteryUsageStatsBuilder2, this.mStats, j2, uptimeUs, query);
            i3 = i + 1;
            batteryUsageStatsBuilder2 = batteryUsageStatsBuilder;
            uidStats2 = uidStats;
            powerCalculators2 = powerCalculators;
            count2 = count;
            realtimeUs2 = realtimeUs;
        }
        BatteryUsageStats.Builder batteryUsageStatsBuilder3 = batteryUsageStatsBuilder2;
        if ((query.getFlags() & 2) != 0) {
            BatteryStats batteryStats = this.mStats;
            if (!(batteryStats instanceof BatteryStatsImpl)) {
                throw new UnsupportedOperationException("History cannot be included for " + getClass().getName());
            }
            BatteryStatsImpl batteryStatsImpl = (BatteryStatsImpl) batteryStats;
            Parcel historyBuffer = Parcel.obtain();
            historyBuffer.appendFrom(batteryStatsImpl.mHistoryBuffer, 0, batteryStatsImpl.mHistoryBuffer.dataSize());
            File systemDir = batteryStatsImpl.mBatteryStatsHistory.getHistoryDirectory().getParentFile();
            BatteryStatsHistory batteryStatsHistory = new BatteryStatsHistory(batteryStatsImpl, systemDir, historyBuffer);
            batteryUsageStatsBuilder3.setBatteryHistory(batteryStatsHistory);
        }
        BatteryUsageStats stats = batteryUsageStatsBuilder3.build();
        if (includeProcessStateData) {
            verify(stats);
        }
        return stats;
    }

    private void verify(BatteryUsageStats stats) {
        if (sErrorReported) {
            return;
        }
        double precision = 2.0d;
        int[] components = {1, 8, 11, 2};
        int[] states = {1, 2, 3, 4};
        for (UidBatteryConsumer ubc : stats.getUidBatteryConsumers()) {
            int length = components.length;
            int i = 0;
            while (i < length) {
                int component = components[i];
                double consumedPower = ubc.getConsumedPower(ubc.getKey(component));
                double sumStates = 0.0d;
                int length2 = states.length;
                int i2 = 0;
                while (i2 < length2) {
                    int state = states[i2];
                    sumStates += ubc.getConsumedPower(ubc.getKey(component, state));
                    i2++;
                    precision = precision;
                }
                double precision2 = precision;
                if (sumStates <= 2.0d + consumedPower) {
                    i++;
                    precision = precision2;
                } else {
                    String error = "Sum of states exceeds total. UID = " + ubc.getUid() + " " + BatteryConsumer.powerComponentIdToString(component) + " total = " + consumedPower + " states = " + sumStates;
                    if (!sErrorReported) {
                        Slog.wtf(TAG, error);
                        sErrorReported = true;
                        return;
                    }
                    Slog.e(TAG, error);
                    return;
                }
            }
        }
    }

    private long getProcessForegroundTimeMs(BatteryStats.Uid uid, long realtimeUs) {
        long topStateDurationUs = uid.getProcessStateTime(0, realtimeUs, 0);
        long foregroundActivityDurationUs = 0;
        BatteryStats.Timer foregroundActivityTimer = uid.getForegroundActivityTimer();
        if (foregroundActivityTimer != null) {
            foregroundActivityDurationUs = foregroundActivityTimer.getTotalTimeLocked(realtimeUs, 0);
        }
        long totalForegroundDurationUs = Math.min(topStateDurationUs, foregroundActivityDurationUs);
        return ((totalForegroundDurationUs + uid.getProcessStateTime(2, realtimeUs, 0)) + uid.getProcessStateTime(1, realtimeUs, 0)) / 1000;
    }

    private long getProcessBackgroundTimeMs(BatteryStats.Uid uid, long realtimeUs) {
        return uid.getProcessStateTime(3, realtimeUs, 0) / 1000;
    }

    private BatteryUsageStats getAggregatedBatteryUsageStats(BatteryUsageStatsQuery query) {
        BatteryUsageStats snapshot;
        boolean includeProcessStateData = true;
        boolean includePowerModels = (query.getFlags() & 4) != 0;
        includeProcessStateData = ((query.getFlags() & 8) == 0 || !this.mStats.isProcessStateDataAvailable()) ? false : false;
        String[] customEnergyConsumerNames = this.mStats.getCustomEnergyConsumerNames();
        BatteryUsageStats.Builder builder = new BatteryUsageStats.Builder(customEnergyConsumerNames, includePowerModels, includeProcessStateData);
        BatteryUsageStatsStore batteryUsageStatsStore = this.mBatteryUsageStatsStore;
        if (batteryUsageStatsStore == null) {
            Log.e(TAG, "BatteryUsageStatsStore is unavailable");
            return builder.build();
        }
        long[] timestamps = batteryUsageStatsStore.listBatteryUsageStatsTimestamps();
        for (long timestamp : timestamps) {
            if (timestamp > query.getFromTimestamp() && timestamp <= query.getToTimestamp() && (snapshot = this.mBatteryUsageStatsStore.loadBatteryUsageStats(timestamp)) != null) {
                if (!Arrays.equals(snapshot.getCustomPowerComponentNames(), customEnergyConsumerNames)) {
                    Log.w(TAG, "Ignoring older BatteryUsageStats snapshot, which has different custom power components: " + Arrays.toString(snapshot.getCustomPowerComponentNames()));
                } else if (includeProcessStateData && !snapshot.isProcessStateDataIncluded()) {
                    Log.w(TAG, "Ignoring older BatteryUsageStats snapshot, which  does not include process state data");
                } else {
                    builder.add(snapshot);
                }
            }
        }
        return builder.build();
    }

    private long elapsedRealtime() {
        BatteryStats batteryStats = this.mStats;
        if (batteryStats instanceof BatteryStatsImpl) {
            return ((BatteryStatsImpl) batteryStats).mClock.elapsedRealtime();
        }
        return SystemClock.elapsedRealtime();
    }

    private long uptimeMillis() {
        BatteryStats batteryStats = this.mStats;
        if (batteryStats instanceof BatteryStatsImpl) {
            return ((BatteryStatsImpl) batteryStats).mClock.uptimeMillis();
        }
        return SystemClock.uptimeMillis();
    }

    private long currentTimeMillis() {
        BatteryStats batteryStats = this.mStats;
        if (batteryStats instanceof BatteryStatsImpl) {
            return ((BatteryStatsImpl) batteryStats).mClock.currentTimeMillis();
        }
        return System.currentTimeMillis();
    }
}
