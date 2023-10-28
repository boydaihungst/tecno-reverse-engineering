package com.android.server.powerstats;

import android.app.StatsManager;
import android.content.Context;
import android.hardware.power.stats.Channel;
import android.hardware.power.stats.EnergyMeasurement;
import android.hardware.power.stats.PowerEntity;
import android.hardware.power.stats.State;
import android.hardware.power.stats.StateResidency;
import android.hardware.power.stats.StateResidencyResult;
import android.power.PowerStatsInternal;
import android.util.Slog;
import android.util.StatsEvent;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.FrameworkStatsLog;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public class StatsPullAtomCallbackImpl implements StatsManager.StatsPullAtomCallback {
    private static final boolean DEBUG = false;
    private static final int STATS_PULL_TIMEOUT_MILLIS = 2000;
    private static final String TAG = StatsPullAtomCallbackImpl.class.getSimpleName();
    private Context mContext;
    private PowerStatsInternal mPowerStatsInternal;
    private Map<Integer, Channel> mChannels = new HashMap();
    private Map<Integer, String> mEntityNames = new HashMap();
    private Map<Integer, Map<Integer, String>> mStateNames = new HashMap();

    public int onPullAtom(int atomTag, List<StatsEvent> data) {
        switch (atomTag) {
            case FrameworkStatsLog.SUBSYSTEM_SLEEP_STATE /* 10005 */:
                return pullSubsystemSleepState(atomTag, data);
            case FrameworkStatsLog.ON_DEVICE_POWER_MEASUREMENT /* 10038 */:
                return pullOnDevicePowerMeasurement(atomTag, data);
            default:
                throw new UnsupportedOperationException("Unknown tagId=" + atomTag);
        }
    }

    private boolean initPullOnDevicePowerMeasurement() {
        Channel[] channels = this.mPowerStatsInternal.getEnergyMeterInfo();
        if (channels == null || channels.length == 0) {
            Slog.e(TAG, "Failed to init OnDevicePowerMeasurement puller");
            return false;
        }
        for (Channel channel : channels) {
            this.mChannels.put(Integer.valueOf(channel.id), channel);
        }
        return true;
    }

    private int pullOnDevicePowerMeasurement(int atomTag, List<StatsEvent> events) {
        try {
            EnergyMeasurement[] energyMeasurements = this.mPowerStatsInternal.readEnergyMeterAsync(new int[0]).get(2000L, TimeUnit.MILLISECONDS);
            if (energyMeasurements == null) {
                return 1;
            }
            for (EnergyMeasurement energyMeasurement : energyMeasurements) {
                if (energyMeasurement.durationMs == energyMeasurement.timestampMs) {
                    events.add(FrameworkStatsLog.buildStatsEvent(atomTag, this.mChannels.get(Integer.valueOf(energyMeasurement.id)).subsystem, this.mChannels.get(Integer.valueOf(energyMeasurement.id)).name, energyMeasurement.durationMs, energyMeasurement.energyUWs));
                }
            }
            return 0;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to readEnergyMeterAsync", e);
            return 1;
        }
    }

    private boolean initSubsystemSleepState() {
        PowerEntity[] entities = this.mPowerStatsInternal.getPowerEntityInfo();
        if (entities == null || entities.length == 0) {
            Slog.e(TAG, "Failed to init SubsystemSleepState puller");
            return false;
        }
        for (PowerEntity entity : entities) {
            Map<Integer, String> states = new HashMap<>();
            for (int j = 0; j < entity.states.length; j++) {
                State state = entity.states[j];
                states.put(Integer.valueOf(state.id), state.name);
            }
            this.mEntityNames.put(Integer.valueOf(entity.id), entity.name);
            this.mStateNames.put(Integer.valueOf(entity.id), states);
        }
        return true;
    }

    private int pullSubsystemSleepState(int atomTag, List<StatsEvent> events) {
        try {
            StateResidencyResult[] results = this.mPowerStatsInternal.getStateResidencyAsync(new int[0]).get(2000L, TimeUnit.MILLISECONDS);
            if (results == null) {
                return 1;
            }
            for (StateResidencyResult result : results) {
                for (int j = 0; j < result.stateResidencyData.length; j++) {
                    StateResidency stateResidency = result.stateResidencyData[j];
                    events.add(FrameworkStatsLog.buildStatsEvent(atomTag, this.mEntityNames.get(Integer.valueOf(result.id)), this.mStateNames.get(Integer.valueOf(result.id)).get(Integer.valueOf(stateResidency.id)), stateResidency.totalStateEntryCount, stateResidency.totalTimeInStateMs));
                }
            }
            return 0;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to getStateResidencyAsync", e);
            return 1;
        }
    }

    public StatsPullAtomCallbackImpl(Context context, PowerStatsInternal powerStatsInternal) {
        this.mContext = context;
        this.mPowerStatsInternal = powerStatsInternal;
        if (powerStatsInternal == null) {
            Slog.e(TAG, "Failed to start PowerStatsService statsd pullers");
            return;
        }
        StatsManager manager = (StatsManager) context.getSystemService(StatsManager.class);
        if (initPullOnDevicePowerMeasurement()) {
            manager.setPullAtomCallback((int) FrameworkStatsLog.ON_DEVICE_POWER_MEASUREMENT, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this);
        }
        if (initSubsystemSleepState()) {
            manager.setPullAtomCallback((int) FrameworkStatsLog.SUBSYSTEM_SLEEP_STATE, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, this);
        }
    }
}
