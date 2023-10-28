package com.android.server.stats.pull;

import android.os.Debug;
/* loaded from: classes2.dex */
final class SystemMemoryUtil {
    private SystemMemoryUtil() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Metrics getMetrics() {
        long accountedKb;
        int totalIonKb = (int) Debug.getDmabufHeapTotalExportedKb();
        int gpuTotalUsageKb = (int) Debug.getGpuTotalUsageKb();
        int gpuPrivateAllocationsKb = (int) Debug.getGpuPrivateMemoryKb();
        int dmaBufTotalExportedKb = (int) Debug.getDmabufTotalExportedKb();
        long[] mInfos = new long[19];
        Debug.getMemInfo(mInfos);
        long kReclaimableKb = mInfos[15];
        if (kReclaimableKb == 0) {
            kReclaimableKb = mInfos[6];
        }
        long accountedKb2 = mInfos[1] + mInfos[10] + mInfos[2] + mInfos[16] + mInfos[17] + mInfos[18] + mInfos[7] + kReclaimableKb + mInfos[12] + mInfos[13];
        if (!Debug.isVmapStack()) {
            accountedKb2 += mInfos[14];
        }
        if (dmaBufTotalExportedKb >= 0 && gpuPrivateAllocationsKb >= 0) {
            accountedKb = accountedKb2 + dmaBufTotalExportedKb + gpuPrivateAllocationsKb;
        } else {
            accountedKb = accountedKb2 + Math.max(0, gpuTotalUsageKb);
            if (dmaBufTotalExportedKb >= 0) {
                accountedKb += dmaBufTotalExportedKb;
            } else if (totalIonKb >= 0) {
                accountedKb += totalIonKb;
            }
        }
        Metrics result = new Metrics();
        result.unreclaimableSlabKb = (int) mInfos[7];
        result.vmallocUsedKb = (int) mInfos[12];
        result.pageTablesKb = (int) mInfos[13];
        result.kernelStackKb = (int) mInfos[14];
        result.shmemKb = (int) mInfos[4];
        result.totalIonKb = totalIonKb;
        result.gpuTotalUsageKb = gpuTotalUsageKb;
        result.gpuPrivateAllocationsKb = gpuPrivateAllocationsKb;
        result.dmaBufTotalExportedKb = dmaBufTotalExportedKb;
        result.unaccountedKb = (int) (mInfos[0] - accountedKb);
        return result;
    }

    /* loaded from: classes2.dex */
    static final class Metrics {
        public int dmaBufTotalExportedKb;
        public int gpuPrivateAllocationsKb;
        public int gpuTotalUsageKb;
        public int kernelStackKb;
        public int pageTablesKb;
        public int shmemKb;
        public int totalIonKb;
        public int unaccountedKb;
        public int unreclaimableSlabKb;
        public int vmallocUsedKb;

        Metrics() {
        }
    }
}
