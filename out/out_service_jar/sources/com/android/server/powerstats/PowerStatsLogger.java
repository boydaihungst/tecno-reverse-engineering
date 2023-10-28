package com.android.server.powerstats;

import android.content.Context;
import android.hardware.power.stats.Channel;
import android.hardware.power.stats.EnergyConsumer;
import android.hardware.power.stats.EnergyConsumerResult;
import android.hardware.power.stats.EnergyMeasurement;
import android.hardware.power.stats.PowerEntity;
import android.hardware.power.stats.StateResidencyResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import com.android.server.powerstats.PowerStatsDataStorage;
import com.android.server.powerstats.PowerStatsHALWrapper;
import com.android.server.powerstats.ProtoStreamUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
/* loaded from: classes2.dex */
public final class PowerStatsLogger extends Handler {
    private static final boolean DEBUG = false;
    protected static final int MSG_LOG_TO_DATA_STORAGE_BATTERY_DROP = 0;
    protected static final int MSG_LOG_TO_DATA_STORAGE_HIGH_FREQUENCY = 2;
    protected static final int MSG_LOG_TO_DATA_STORAGE_LOW_FREQUENCY = 1;
    private static final String TAG = PowerStatsLogger.class.getSimpleName();
    private File mDataStoragePath;
    private boolean mDeleteMeterDataOnBoot;
    private boolean mDeleteModelDataOnBoot;
    private boolean mDeleteResidencyDataOnBoot;
    private final PowerStatsHALWrapper.IPowerStatsHALWrapper mPowerStatsHALWrapper;
    private final PowerStatsDataStorage mPowerStatsMeterStorage;
    private final PowerStatsDataStorage mPowerStatsModelStorage;
    private final PowerStatsDataStorage mPowerStatsResidencyStorage;
    private final long mStartWallTime;

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                StateResidencyResult[] stateResidencyResults = this.mPowerStatsHALWrapper.getStateResidency(new int[0]);
                ProtoStreamUtils.StateResidencyResultUtils.adjustTimeSinceBootToEpoch(stateResidencyResults, this.mStartWallTime);
                this.mPowerStatsResidencyStorage.write(ProtoStreamUtils.StateResidencyResultUtils.getProtoBytes(stateResidencyResults));
                return;
            case 1:
                EnergyConsumerResult[] ecrAttribution = this.mPowerStatsHALWrapper.getEnergyConsumed(new int[0]);
                ProtoStreamUtils.EnergyConsumerResultUtils.adjustTimeSinceBootToEpoch(ecrAttribution, this.mStartWallTime);
                this.mPowerStatsModelStorage.write(ProtoStreamUtils.EnergyConsumerResultUtils.getProtoBytes(ecrAttribution, true));
                return;
            case 2:
                EnergyMeasurement[] energyMeasurements = this.mPowerStatsHALWrapper.readEnergyMeter(new int[0]);
                ProtoStreamUtils.EnergyMeasurementUtils.adjustTimeSinceBootToEpoch(energyMeasurements, this.mStartWallTime);
                this.mPowerStatsMeterStorage.write(ProtoStreamUtils.EnergyMeasurementUtils.getProtoBytes(energyMeasurements));
                EnergyConsumerResult[] ecrNoAttribution = this.mPowerStatsHALWrapper.getEnergyConsumed(new int[0]);
                ProtoStreamUtils.EnergyConsumerResultUtils.adjustTimeSinceBootToEpoch(ecrNoAttribution, this.mStartWallTime);
                this.mPowerStatsModelStorage.write(ProtoStreamUtils.EnergyConsumerResultUtils.getProtoBytes(ecrNoAttribution, false));
                return;
            default:
                return;
        }
    }

    public void writeMeterDataToFile(FileDescriptor fd) {
        final ProtoOutputStream pos = new ProtoOutputStream(fd);
        try {
            Channel[] channel = this.mPowerStatsHALWrapper.getEnergyMeterInfo();
            ProtoStreamUtils.ChannelUtils.packProtoMessage(channel, pos);
            this.mPowerStatsMeterStorage.read(new PowerStatsDataStorage.DataElementReadCallback() { // from class: com.android.server.powerstats.PowerStatsLogger.1
                @Override // com.android.server.powerstats.PowerStatsDataStorage.DataElementReadCallback
                public void onReadDataElement(byte[] data) {
                    try {
                        new ProtoInputStream(new ByteArrayInputStream(data));
                        EnergyMeasurement[] energyMeasurement = ProtoStreamUtils.EnergyMeasurementUtils.unpackProtoMessage(data);
                        ProtoStreamUtils.EnergyMeasurementUtils.packProtoMessage(energyMeasurement, pos);
                    } catch (IOException e) {
                        Slog.e(PowerStatsLogger.TAG, "Failed to write energy meter data to incident report.");
                    }
                }
            });
        } catch (IOException e) {
            Slog.e(TAG, "Failed to write energy meter info to incident report.");
        }
        pos.flush();
    }

    public void writeModelDataToFile(FileDescriptor fd) {
        final ProtoOutputStream pos = new ProtoOutputStream(fd);
        try {
            EnergyConsumer[] energyConsumer = this.mPowerStatsHALWrapper.getEnergyConsumerInfo();
            ProtoStreamUtils.EnergyConsumerUtils.packProtoMessage(energyConsumer, pos);
            this.mPowerStatsModelStorage.read(new PowerStatsDataStorage.DataElementReadCallback() { // from class: com.android.server.powerstats.PowerStatsLogger.2
                @Override // com.android.server.powerstats.PowerStatsDataStorage.DataElementReadCallback
                public void onReadDataElement(byte[] data) {
                    try {
                        new ProtoInputStream(new ByteArrayInputStream(data));
                        EnergyConsumerResult[] energyConsumerResult = ProtoStreamUtils.EnergyConsumerResultUtils.unpackProtoMessage(data);
                        ProtoStreamUtils.EnergyConsumerResultUtils.packProtoMessage(energyConsumerResult, pos, true);
                    } catch (IOException e) {
                        Slog.e(PowerStatsLogger.TAG, "Failed to write energy model data to incident report.");
                    }
                }
            });
        } catch (IOException e) {
            Slog.e(TAG, "Failed to write energy model info to incident report.");
        }
        pos.flush();
    }

    public void writeResidencyDataToFile(FileDescriptor fd) {
        final ProtoOutputStream pos = new ProtoOutputStream(fd);
        try {
            PowerEntity[] powerEntity = this.mPowerStatsHALWrapper.getPowerEntityInfo();
            ProtoStreamUtils.PowerEntityUtils.packProtoMessage(powerEntity, pos);
            this.mPowerStatsResidencyStorage.read(new PowerStatsDataStorage.DataElementReadCallback() { // from class: com.android.server.powerstats.PowerStatsLogger.3
                @Override // com.android.server.powerstats.PowerStatsDataStorage.DataElementReadCallback
                public void onReadDataElement(byte[] data) {
                    try {
                        new ProtoInputStream(new ByteArrayInputStream(data));
                        StateResidencyResult[] stateResidencyResult = ProtoStreamUtils.StateResidencyResultUtils.unpackProtoMessage(data);
                        ProtoStreamUtils.StateResidencyResultUtils.packProtoMessage(stateResidencyResult, pos);
                    } catch (IOException e) {
                        Slog.e(PowerStatsLogger.TAG, "Failed to write residency data to incident report.");
                    }
                }
            });
        } catch (IOException e) {
            Slog.e(TAG, "Failed to write residency data to incident report.");
        }
        pos.flush();
    }

    private boolean dataChanged(String cachedFilename, byte[] dataCurrent) {
        if (!this.mDataStoragePath.exists() && !this.mDataStoragePath.mkdirs()) {
            return false;
        }
        File cachedFile = new File(this.mDataStoragePath, cachedFilename);
        if (cachedFile.exists()) {
            byte[] dataCached = new byte[(int) cachedFile.length()];
            try {
                FileInputStream fis = new FileInputStream(cachedFile.getPath());
                fis.read(dataCached);
            } catch (IOException e) {
                Slog.e(TAG, "Failed to read cached data from file");
            }
            boolean dataChanged = !Arrays.equals(dataCached, dataCurrent);
            return dataChanged;
        }
        return true;
    }

    private void updateCacheFile(String cacheFilename, byte[] data) {
        try {
            AtomicFile atomicCachedFile = new AtomicFile(new File(this.mDataStoragePath, cacheFilename));
            FileOutputStream fos = atomicCachedFile.startWrite();
            fos.write(data);
            atomicCachedFile.finishWrite(fos);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to write current data to cached file");
        }
    }

    public boolean getDeleteMeterDataOnBoot() {
        return this.mDeleteMeterDataOnBoot;
    }

    public boolean getDeleteModelDataOnBoot() {
        return this.mDeleteModelDataOnBoot;
    }

    public boolean getDeleteResidencyDataOnBoot() {
        return this.mDeleteResidencyDataOnBoot;
    }

    public long getStartWallTime() {
        return this.mStartWallTime;
    }

    public PowerStatsLogger(Context context, Looper looper, File dataStoragePath, String meterFilename, String meterCacheFilename, String modelFilename, String modelCacheFilename, String residencyFilename, String residencyCacheFilename, PowerStatsHALWrapper.IPowerStatsHALWrapper powerStatsHALWrapper) {
        super(looper);
        this.mStartWallTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        this.mPowerStatsHALWrapper = powerStatsHALWrapper;
        this.mDataStoragePath = dataStoragePath;
        PowerStatsDataStorage powerStatsDataStorage = new PowerStatsDataStorage(context, this.mDataStoragePath, meterFilename);
        this.mPowerStatsMeterStorage = powerStatsDataStorage;
        PowerStatsDataStorage powerStatsDataStorage2 = new PowerStatsDataStorage(context, this.mDataStoragePath, modelFilename);
        this.mPowerStatsModelStorage = powerStatsDataStorage2;
        PowerStatsDataStorage powerStatsDataStorage3 = new PowerStatsDataStorage(context, this.mDataStoragePath, residencyFilename);
        this.mPowerStatsResidencyStorage = powerStatsDataStorage3;
        Channel[] channels = powerStatsHALWrapper.getEnergyMeterInfo();
        byte[] channelBytes = ProtoStreamUtils.ChannelUtils.getProtoBytes(channels);
        boolean dataChanged = dataChanged(meterCacheFilename, channelBytes);
        this.mDeleteMeterDataOnBoot = dataChanged;
        if (dataChanged) {
            powerStatsDataStorage.deleteLogs();
            updateCacheFile(meterCacheFilename, channelBytes);
        }
        EnergyConsumer[] energyConsumers = powerStatsHALWrapper.getEnergyConsumerInfo();
        byte[] energyConsumerBytes = ProtoStreamUtils.EnergyConsumerUtils.getProtoBytes(energyConsumers);
        boolean dataChanged2 = dataChanged(modelCacheFilename, energyConsumerBytes);
        this.mDeleteModelDataOnBoot = dataChanged2;
        if (dataChanged2) {
            powerStatsDataStorage2.deleteLogs();
            updateCacheFile(modelCacheFilename, energyConsumerBytes);
        }
        PowerEntity[] powerEntities = powerStatsHALWrapper.getPowerEntityInfo();
        byte[] powerEntityBytes = ProtoStreamUtils.PowerEntityUtils.getProtoBytes(powerEntities);
        boolean dataChanged3 = dataChanged(residencyCacheFilename, powerEntityBytes);
        this.mDeleteResidencyDataOnBoot = dataChanged3;
        if (dataChanged3) {
            powerStatsDataStorage3.deleteLogs();
            updateCacheFile(residencyCacheFilename, powerEntityBytes);
        }
    }
}
