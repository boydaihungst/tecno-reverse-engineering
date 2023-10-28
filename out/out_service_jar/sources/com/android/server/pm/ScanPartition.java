package com.android.server.pm;

import android.content.pm.PackagePartitions;
import java.io.File;
/* loaded from: classes2.dex */
public class ScanPartition extends PackagePartitions.SystemPartition {
    public final int scanFlag;

    public ScanPartition(PackagePartitions.SystemPartition partition) {
        super(partition);
        this.scanFlag = scanFlagForPartition(partition);
    }

    public ScanPartition(File folder, ScanPartition original, int additionalScanFlag) {
        super(folder, original);
        this.scanFlag = original.scanFlag | additionalScanFlag;
    }

    private static int scanFlagForPartition(PackagePartitions.SystemPartition partition) {
        switch (partition.type) {
            case 0:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
                return 0;
            case 1:
                return 524288;
            case 2:
                return 4194304;
            case 3:
                return 262144;
            case 4:
                return 1048576;
            case 5:
                return 2097152;
            default:
                throw new IllegalStateException("Unable to determine scan flag for " + partition.getFolder());
        }
    }

    public String toString() {
        return getFolder().getAbsolutePath() + ":" + this.scanFlag;
    }
}
