package android.hardware.health;
/* loaded from: classes.dex */
public class Translate {
    public static StorageInfo h2aTranslate(android.hardware.health.V2_0.StorageInfo in) {
        StorageInfo out = new StorageInfo();
        out.eol = in.eol;
        out.lifetimeA = in.lifetimeA;
        out.lifetimeB = in.lifetimeB;
        out.version = in.version;
        return out;
    }

    public static DiskStats h2aTranslate(android.hardware.health.V2_0.DiskStats in) {
        DiskStats out = new DiskStats();
        out.reads = in.reads;
        out.readMerges = in.readMerges;
        out.readSectors = in.readSectors;
        out.readTicks = in.readTicks;
        out.writes = in.writes;
        out.writeMerges = in.writeMerges;
        out.writeSectors = in.writeSectors;
        out.writeTicks = in.writeTicks;
        out.ioInFlight = in.ioInFlight;
        out.ioTicks = in.ioTicks;
        out.ioInQueue = in.ioInQueue;
        return out;
    }

    private static void h2aTranslateInternal(HealthInfo out, android.hardware.health.V1_0.HealthInfo in) {
        out.chargerAcOnline = in.chargerAcOnline;
        out.chargerUsbOnline = in.chargerUsbOnline;
        out.chargerWirelessOnline = in.chargerWirelessOnline;
        out.maxChargingCurrentMicroamps = in.maxChargingCurrent;
        out.maxChargingVoltageMicrovolts = in.maxChargingVoltage;
        out.batteryStatus = in.batteryStatus;
        out.batteryHealth = in.batteryHealth;
        out.batteryPresent = in.batteryPresent;
        out.batteryLevel = in.batteryLevel;
        out.batteryVoltageMillivolts = in.batteryVoltage;
        out.batteryTemperatureTenthsCelsius = in.batteryTemperature;
        out.batteryCurrentMicroamps = in.batteryCurrent;
        out.batteryCycleCount = in.batteryCycleCount;
        out.batteryFullChargeUah = in.batteryFullCharge;
        out.batteryChargeCounterUah = in.batteryChargeCounter;
        out.batteryTechnology = in.batteryTechnology;
    }

    public static HealthInfo h2aTranslate(android.hardware.health.V1_0.HealthInfo in) {
        HealthInfo out = new HealthInfo();
        h2aTranslateInternal(out, in);
        return out;
    }

    public static HealthInfo h2aTranslate(android.hardware.health.V2_1.HealthInfo in) {
        HealthInfo out = new HealthInfo();
        h2aTranslateInternal(out, in.legacy.legacy);
        out.batteryCurrentAverageMicroamps = in.legacy.batteryCurrentAverage;
        out.diskStats = new DiskStats[in.legacy.diskStats.size()];
        for (int i = 0; i < in.legacy.diskStats.size(); i++) {
            out.diskStats[i] = h2aTranslate(in.legacy.diskStats.get(i));
        }
        out.storageInfos = new StorageInfo[in.legacy.storageInfos.size()];
        for (int i2 = 0; i2 < in.legacy.storageInfos.size(); i2++) {
            out.storageInfos[i2] = h2aTranslate(in.legacy.storageInfos.get(i2));
        }
        int i3 = in.batteryCapacityLevel;
        out.batteryCapacityLevel = i3;
        out.batteryChargeTimeToFullNowSeconds = in.batteryChargeTimeToFullNowSeconds;
        out.batteryFullChargeDesignCapacityUah = in.batteryFullChargeDesignCapacityUah;
        return out;
    }
}
