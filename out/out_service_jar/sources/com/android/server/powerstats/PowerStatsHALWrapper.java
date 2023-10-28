package com.android.server.powerstats;

import android.hardware.power.stats.Channel;
import android.hardware.power.stats.EnergyConsumer;
import android.hardware.power.stats.EnergyConsumerResult;
import android.hardware.power.stats.EnergyMeasurement;
import android.hardware.power.stats.IPowerStats;
import android.hardware.power.stats.PowerEntity;
import android.hardware.power.stats.StateResidencyResult;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public final class PowerStatsHALWrapper {
    private static final boolean DEBUG = false;
    private static final String TAG = PowerStatsHALWrapper.class.getSimpleName();

    /* loaded from: classes2.dex */
    public interface IPowerStatsHALWrapper {
        EnergyConsumerResult[] getEnergyConsumed(int[] iArr);

        EnergyConsumer[] getEnergyConsumerInfo();

        Channel[] getEnergyMeterInfo();

        PowerEntity[] getPowerEntityInfo();

        StateResidencyResult[] getStateResidency(int[] iArr);

        boolean isInitialized();

        EnergyMeasurement[] readEnergyMeter(int[] iArr);
    }

    /* loaded from: classes2.dex */
    public static final class PowerStatsHAL20WrapperImpl implements IPowerStatsHALWrapper {
        private static Supplier<IPowerStats> sVintfPowerStats;

        public PowerStatsHAL20WrapperImpl() {
            Supplier<IPowerStats> service = new VintfHalCache();
            sVintfPowerStats = null;
            if (service.get() == null) {
                sVintfPowerStats = null;
            } else {
                sVintfPowerStats = service;
            }
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public PowerEntity[] getPowerEntityInfo() {
            Supplier<IPowerStats> supplier = sVintfPowerStats;
            if (supplier == null) {
                return null;
            }
            try {
                PowerEntity[] powerEntityHAL = supplier.get().getPowerEntityInfo();
                return powerEntityHAL;
            } catch (RemoteException e) {
                Slog.w(PowerStatsHALWrapper.TAG, "Failed to get power entity info: ", e);
                return null;
            }
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public StateResidencyResult[] getStateResidency(int[] powerEntityIds) {
            Supplier<IPowerStats> supplier = sVintfPowerStats;
            if (supplier == null) {
                return null;
            }
            try {
                StateResidencyResult[] stateResidencyResultHAL = supplier.get().getStateResidency(powerEntityIds);
                return stateResidencyResultHAL;
            } catch (RemoteException e) {
                Slog.w(PowerStatsHALWrapper.TAG, "Failed to get state residency: ", e);
                return null;
            }
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public EnergyConsumer[] getEnergyConsumerInfo() {
            Supplier<IPowerStats> supplier = sVintfPowerStats;
            if (supplier == null) {
                return null;
            }
            try {
                EnergyConsumer[] energyConsumerHAL = supplier.get().getEnergyConsumerInfo();
                return energyConsumerHAL;
            } catch (RemoteException e) {
                Slog.w(PowerStatsHALWrapper.TAG, "Failed to get energy consumer info: ", e);
                return null;
            }
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public EnergyConsumerResult[] getEnergyConsumed(int[] energyConsumerIds) {
            Supplier<IPowerStats> supplier = sVintfPowerStats;
            if (supplier == null) {
                return null;
            }
            try {
                EnergyConsumerResult[] energyConsumedHAL = supplier.get().getEnergyConsumed(energyConsumerIds);
                return energyConsumedHAL;
            } catch (RemoteException e) {
                Slog.w(PowerStatsHALWrapper.TAG, "Failed to get energy consumer results: ", e);
                return null;
            }
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public Channel[] getEnergyMeterInfo() {
            Supplier<IPowerStats> supplier = sVintfPowerStats;
            if (supplier == null) {
                return null;
            }
            try {
                Channel[] energyMeterInfoHAL = supplier.get().getEnergyMeterInfo();
                return energyMeterInfoHAL;
            } catch (RemoteException e) {
                Slog.w(PowerStatsHALWrapper.TAG, "Failed to get energy meter info: ", e);
                return null;
            }
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public EnergyMeasurement[] readEnergyMeter(int[] channelIds) {
            Supplier<IPowerStats> supplier = sVintfPowerStats;
            if (supplier == null) {
                return null;
            }
            try {
                EnergyMeasurement[] energyMeasurementHAL = supplier.get().readEnergyMeter(channelIds);
                return energyMeasurementHAL;
            } catch (RemoteException e) {
                Slog.w(PowerStatsHALWrapper.TAG, "Failed to get energy measurements: ", e);
                return null;
            }
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public boolean isInitialized() {
            return sVintfPowerStats != null;
        }
    }

    /* loaded from: classes2.dex */
    public static final class PowerStatsHAL10WrapperImpl implements IPowerStatsHALWrapper {
        private boolean mIsInitialized;

        private static native Channel[] nativeGetEnergyMeterInfo();

        private static native PowerEntity[] nativeGetPowerEntityInfo();

        private static native StateResidencyResult[] nativeGetStateResidency(int[] iArr);

        private static native boolean nativeInit();

        private static native EnergyMeasurement[] nativeReadEnergyMeters(int[] iArr);

        public PowerStatsHAL10WrapperImpl() {
            if (nativeInit()) {
                this.mIsInitialized = true;
            } else {
                this.mIsInitialized = false;
            }
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public PowerEntity[] getPowerEntityInfo() {
            return nativeGetPowerEntityInfo();
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public StateResidencyResult[] getStateResidency(int[] powerEntityIds) {
            return nativeGetStateResidency(powerEntityIds);
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public EnergyConsumer[] getEnergyConsumerInfo() {
            return new EnergyConsumer[0];
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public EnergyConsumerResult[] getEnergyConsumed(int[] energyConsumerIds) {
            return new EnergyConsumerResult[0];
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public Channel[] getEnergyMeterInfo() {
            return nativeGetEnergyMeterInfo();
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public EnergyMeasurement[] readEnergyMeter(int[] channelIds) {
            return nativeReadEnergyMeters(channelIds);
        }

        @Override // com.android.server.powerstats.PowerStatsHALWrapper.IPowerStatsHALWrapper
        public boolean isInitialized() {
            return this.mIsInitialized;
        }
    }

    public static IPowerStatsHALWrapper getPowerStatsHalImpl() {
        PowerStatsHAL20WrapperImpl powerStatsHAL20WrapperImpl = new PowerStatsHAL20WrapperImpl();
        if (powerStatsHAL20WrapperImpl.isInitialized()) {
            return powerStatsHAL20WrapperImpl;
        }
        return new PowerStatsHAL10WrapperImpl();
    }

    /* loaded from: classes2.dex */
    private static class VintfHalCache implements Supplier<IPowerStats>, IBinder.DeathRecipient {
        private IPowerStats mInstance;

        private VintfHalCache() {
            this.mInstance = null;
        }

        /* JADX DEBUG: Method merged with bridge method */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // java.util.function.Supplier
        public synchronized IPowerStats get() {
            IBinder binder;
            if (this.mInstance == null && (binder = Binder.allowBlocking(ServiceManager.waitForDeclaredService("android.hardware.power.stats.IPowerStats/default"))) != null) {
                this.mInstance = IPowerStats.Stub.asInterface(binder);
                try {
                    binder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    Slog.e(PowerStatsHALWrapper.TAG, "Unable to register DeathRecipient for " + this.mInstance);
                }
            }
            return this.mInstance;
        }

        @Override // android.os.IBinder.DeathRecipient
        public synchronized void binderDied() {
            Slog.w(PowerStatsHALWrapper.TAG, "PowerStats HAL died");
            this.mInstance = null;
        }
    }
}
