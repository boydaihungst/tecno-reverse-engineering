package com.android.server.am;

import android.hardware.power.stats.EnergyConsumer;
import android.hardware.power.stats.EnergyConsumerAttribution;
import android.hardware.power.stats.EnergyConsumerResult;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class MeasuredEnergySnapshot {
    private static final int MILLIVOLTS_PER_VOLT = 1000;
    private static final String TAG = "MeasuredEnergySnapshot";
    public static final long UNAVAILABLE = -1;
    private final SparseArray<SparseLongArray> mAttributionSnapshots;
    private final SparseArray<EnergyConsumer> mEnergyConsumers;
    private final SparseLongArray mMeasuredEnergySnapshots;
    private final int mNumCpuClusterOrdinals;
    private final int mNumDisplayOrdinals;
    private final int mNumOtherOrdinals;
    private final SparseIntArray mVoltageSnapshots;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MeasuredEnergySnapshot(SparseArray<EnergyConsumer> idToConsumerMap) {
        this.mEnergyConsumers = idToConsumerMap;
        this.mMeasuredEnergySnapshots = new SparseLongArray(idToConsumerMap.size());
        this.mVoltageSnapshots = new SparseIntArray(idToConsumerMap.size());
        this.mNumCpuClusterOrdinals = calculateNumOrdinals(2, idToConsumerMap);
        this.mNumDisplayOrdinals = calculateNumOrdinals(3, idToConsumerMap);
        int calculateNumOrdinals = calculateNumOrdinals(0, idToConsumerMap);
        this.mNumOtherOrdinals = calculateNumOrdinals;
        this.mAttributionSnapshots = new SparseArray<>(calculateNumOrdinals);
    }

    /* loaded from: classes.dex */
    static class MeasuredEnergyDeltaData {
        public long bluetoothChargeUC = -1;
        public long[] cpuClusterChargeUC = null;
        public long[] displayChargeUC = null;
        public long gnssChargeUC = -1;
        public long mobileRadioChargeUC = -1;
        public long wifiChargeUC = -1;
        public long[] otherTotalChargeUC = null;
        public SparseLongArray[] otherUidChargesUC = null;

        MeasuredEnergyDeltaData() {
        }
    }

    public MeasuredEnergyDeltaData updateAndGetDelta(EnergyConsumerResult[] ecrs, int voltageMV) {
        int i;
        int i2;
        int oldVoltageMV;
        int avgVoltageMV;
        MeasuredEnergySnapshot measuredEnergySnapshot = this;
        EnergyConsumerResult[] energyConsumerResultArr = ecrs;
        int i3 = voltageMV;
        EnergyConsumer energyConsumer = null;
        if (energyConsumerResultArr != null && energyConsumerResultArr.length != 0) {
            if (i3 <= 0) {
                Slog.wtf(TAG, "Unexpected battery voltage (" + i3 + " mV) when taking measured energy snapshot");
                return null;
            }
            MeasuredEnergyDeltaData output = new MeasuredEnergyDeltaData();
            int length = energyConsumerResultArr.length;
            int i4 = 0;
            while (i4 < length) {
                EnergyConsumerResult ecr = energyConsumerResultArr[i4];
                int consumerId = ecr.id;
                long newEnergyUJ = ecr.energyUWs;
                EnergyConsumerAttribution[] newAttributions = ecr.attribution;
                EnergyConsumer consumer = measuredEnergySnapshot.mEnergyConsumers.get(consumerId, energyConsumer);
                if (consumer == null) {
                    Slog.e(TAG, "updateAndGetDelta given invalid consumerId " + consumerId);
                    avgVoltageMV = i3;
                    i = length;
                    i2 = i4;
                } else {
                    int type = consumer.type;
                    int ordinal = consumer.ordinal;
                    i = length;
                    i2 = i4;
                    long oldEnergyUJ = measuredEnergySnapshot.mMeasuredEnergySnapshots.get(consumerId, -1L);
                    int oldVoltageMV2 = measuredEnergySnapshot.mVoltageSnapshots.get(consumerId);
                    measuredEnergySnapshot.mMeasuredEnergySnapshots.put(consumerId, newEnergyUJ);
                    measuredEnergySnapshot.mVoltageSnapshots.put(consumerId, i3);
                    int avgVoltageMV2 = ((oldVoltageMV2 + i3) + 1) / 2;
                    SparseLongArray otherUidCharges = measuredEnergySnapshot.updateAndGetDeltaForTypeOther(consumer, newAttributions, avgVoltageMV2);
                    if (oldEnergyUJ < 0) {
                        avgVoltageMV = i3;
                    } else if (newEnergyUJ == oldEnergyUJ) {
                        avgVoltageMV = i3;
                    } else {
                        long deltaUJ = newEnergyUJ - oldEnergyUJ;
                        if (deltaUJ < 0) {
                            oldVoltageMV = oldVoltageMV2;
                        } else if (oldVoltageMV2 <= 0) {
                            oldVoltageMV = oldVoltageMV2;
                        } else {
                            long deltaChargeUC = measuredEnergySnapshot.calculateChargeConsumedUC(deltaUJ, avgVoltageMV2);
                            switch (type) {
                                case 0:
                                    if (output.otherTotalChargeUC == null) {
                                        output.otherTotalChargeUC = new long[measuredEnergySnapshot.mNumOtherOrdinals];
                                        output.otherUidChargesUC = new SparseLongArray[measuredEnergySnapshot.mNumOtherOrdinals];
                                    }
                                    output.otherTotalChargeUC[ordinal] = deltaChargeUC;
                                    output.otherUidChargesUC[ordinal] = otherUidCharges;
                                    avgVoltageMV = voltageMV;
                                    continue;
                                case 1:
                                    output.bluetoothChargeUC = deltaChargeUC;
                                    avgVoltageMV = voltageMV;
                                    continue;
                                case 2:
                                    if (output.cpuClusterChargeUC == null) {
                                        output.cpuClusterChargeUC = new long[measuredEnergySnapshot.mNumCpuClusterOrdinals];
                                    }
                                    output.cpuClusterChargeUC[ordinal] = deltaChargeUC;
                                    avgVoltageMV = voltageMV;
                                    continue;
                                case 3:
                                    if (output.displayChargeUC == null) {
                                        output.displayChargeUC = new long[measuredEnergySnapshot.mNumDisplayOrdinals];
                                    }
                                    output.displayChargeUC[ordinal] = deltaChargeUC;
                                    avgVoltageMV = voltageMV;
                                    continue;
                                case 4:
                                    output.gnssChargeUC = deltaChargeUC;
                                    avgVoltageMV = voltageMV;
                                    continue;
                                case 5:
                                    output.mobileRadioChargeUC = deltaChargeUC;
                                    avgVoltageMV = voltageMV;
                                    continue;
                                case 6:
                                    output.wifiChargeUC = deltaChargeUC;
                                    avgVoltageMV = voltageMV;
                                    continue;
                                default:
                                    Slog.w(TAG, "Ignoring consumer " + consumer.name + " of unknown type " + type);
                                    avgVoltageMV = voltageMV;
                                    continue;
                            }
                        }
                        avgVoltageMV = voltageMV;
                        Slog.e(TAG, "Bad data! EnergyConsumer " + consumer.name + ": new energy (" + newEnergyUJ + ") < old energy (" + oldEnergyUJ + "), new voltage (" + avgVoltageMV + "), old voltage (" + oldVoltageMV + "). Skipping. ");
                    }
                }
                i4 = i2 + 1;
                measuredEnergySnapshot = this;
                i3 = avgVoltageMV;
                length = i;
                energyConsumer = null;
                energyConsumerResultArr = ecrs;
            }
            return output;
        }
        return null;
    }

    private SparseLongArray updateAndGetDeltaForTypeOther(EnergyConsumer consumerInfo, EnergyConsumerAttribution[] newAttributions, int avgVoltageMV) {
        EnergyConsumerAttribution[] newAttributions2;
        EnergyConsumerAttribution[] newAttributions3;
        SparseLongArray uidOldEnergyMap;
        if (consumerInfo.type != 0) {
            return null;
        }
        int i = 0;
        if (newAttributions != null) {
            newAttributions2 = newAttributions;
        } else {
            newAttributions2 = new EnergyConsumerAttribution[0];
        }
        SparseLongArray uidOldEnergyMap2 = this.mAttributionSnapshots.get(consumerInfo.id, null);
        if (uidOldEnergyMap2 == null) {
            SparseLongArray uidOldEnergyMap3 = new SparseLongArray(newAttributions2.length);
            this.mAttributionSnapshots.put(consumerInfo.id, uidOldEnergyMap3);
            int length = newAttributions2.length;
            while (i < length) {
                EnergyConsumerAttribution newAttribution = newAttributions2[i];
                uidOldEnergyMap3.put(newAttribution.uid, newAttribution.energyUWs);
                i++;
            }
            return null;
        }
        SparseLongArray uidChargeDeltas = new SparseLongArray();
        int length2 = newAttributions2.length;
        while (i < length2) {
            EnergyConsumerAttribution newAttribution2 = newAttributions2[i];
            int uid = newAttribution2.uid;
            long newEnergyUJ = newAttribution2.energyUWs;
            long oldEnergyUJ = uidOldEnergyMap2.get(uid, 0L);
            uidOldEnergyMap2.put(uid, newEnergyUJ);
            if (oldEnergyUJ < 0) {
                newAttributions3 = newAttributions2;
                uidOldEnergyMap = uidOldEnergyMap2;
            } else if (newEnergyUJ == oldEnergyUJ) {
                newAttributions3 = newAttributions2;
                uidOldEnergyMap = uidOldEnergyMap2;
            } else {
                newAttributions3 = newAttributions2;
                uidOldEnergyMap = uidOldEnergyMap2;
                long deltaUJ = newEnergyUJ - oldEnergyUJ;
                if (deltaUJ < 0 || avgVoltageMV <= 0) {
                    Slog.e(TAG, "EnergyConsumer " + consumerInfo.name + ": new energy (" + newEnergyUJ + ") but old energy (" + oldEnergyUJ + "). Average voltage (" + avgVoltageMV + ")Skipping. ");
                } else {
                    long deltaChargeUC = calculateChargeConsumedUC(deltaUJ, avgVoltageMV);
                    uidChargeDeltas.put(uid, deltaChargeUC);
                }
            }
            i++;
            uidOldEnergyMap2 = uidOldEnergyMap;
            newAttributions2 = newAttributions3;
        }
        return uidChargeDeltas;
    }

    public void dump(PrintWriter pw) {
        pw.println("Measured energy snapshot");
        pw.println("List of EnergyConsumers:");
        for (int i = 0; i < this.mEnergyConsumers.size(); i++) {
            int id = this.mEnergyConsumers.keyAt(i);
            EnergyConsumer consumer = this.mEnergyConsumers.valueAt(i);
            pw.println(String.format("    Consumer %d is {id=%d, ordinal=%d, type=%d, name=%s}", Integer.valueOf(id), Integer.valueOf(consumer.id), Integer.valueOf(consumer.ordinal), Byte.valueOf(consumer.type), consumer.name));
        }
        pw.println("Map of consumerIds to energy (in microjoules):");
        for (int i2 = 0; i2 < this.mMeasuredEnergySnapshots.size(); i2++) {
            int id2 = this.mMeasuredEnergySnapshots.keyAt(i2);
            long energyUJ = this.mMeasuredEnergySnapshots.valueAt(i2);
            long voltageMV = this.mVoltageSnapshots.valueAt(i2);
            pw.println(String.format("    Consumer %d has energy %d uJ at %d mV", Integer.valueOf(id2), Long.valueOf(energyUJ), Long.valueOf(voltageMV)));
        }
        pw.println("List of the " + this.mNumOtherOrdinals + " OTHER EnergyConsumers:");
        pw.println("    " + this.mAttributionSnapshots);
        pw.println();
    }

    public String[] getOtherOrdinalNames() {
        String[] names = new String[this.mNumOtherOrdinals];
        int consumerIndex = 0;
        int size = this.mEnergyConsumers.size();
        for (int idx = 0; idx < size; idx++) {
            EnergyConsumer consumer = this.mEnergyConsumers.valueAt(idx);
            if (consumer.type == 0) {
                names[consumerIndex] = sanitizeCustomBucketName(consumer.name);
                consumerIndex++;
            }
        }
        return names;
    }

    private String sanitizeCustomBucketName(String bucketName) {
        char[] charArray;
        if (bucketName == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(bucketName.length());
        for (char c : bucketName.toCharArray()) {
            if (Character.isWhitespace(c)) {
                sb.append(' ');
            } else if (Character.isISOControl(c)) {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int calculateNumOrdinals(int type, SparseArray<EnergyConsumer> idToConsumer) {
        if (idToConsumer == null) {
            return 0;
        }
        int numOrdinals = 0;
        int size = idToConsumer.size();
        for (int idx = 0; idx < size; idx++) {
            EnergyConsumer consumer = idToConsumer.valueAt(idx);
            if (consumer.type == type) {
                numOrdinals++;
            }
        }
        return numOrdinals;
    }

    private long calculateChargeConsumedUC(long deltaEnergyUJ, int avgVoltageMV) {
        return ((1000 * deltaEnergyUJ) + (avgVoltageMV / 2)) / avgVoltageMV;
    }
}
