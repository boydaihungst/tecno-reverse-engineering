package com.android.server.pm.dex;

import android.util.Slog;
import com.android.internal.art.ArtStatsLog;
import com.android.internal.os.BackgroundThread;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class OdsignStatsLogger {
    private static final String COMPOS_METRIC_NAME = "comp_os_artifacts_check_record";
    private static final String METRICS_FILE = "/data/misc/odsign/metrics/odsign-metrics.txt";
    private static final String TAG = "OdsignStatsLogger";

    public static void triggerStatsWrite() {
        BackgroundThread.getExecutor().execute(new Runnable() { // from class: com.android.server.pm.dex.OdsignStatsLogger$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                OdsignStatsLogger.writeStats();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void writeStats() {
        String[] split;
        try {
            String lines = IoUtils.readFileAsString(METRICS_FILE);
            if (!new File(METRICS_FILE).delete()) {
                Slog.w(TAG, "Failed to delete metrics file");
            }
            for (String line : lines.split("\n")) {
                String[] metrics = line.split(" ");
                if (metrics.length == 4 && metrics[0].equals(COMPOS_METRIC_NAME)) {
                    boolean currentArtifactsOk = metrics[1].equals("1");
                    boolean compOsPendingArtifactsExists = metrics[2].equals("1");
                    boolean useCompOsGeneratedArtifacts = metrics[3].equals("1");
                    ArtStatsLog.write(ArtStatsLog.EARLY_BOOT_COMP_OS_ARTIFACTS_CHECK_REPORTED, currentArtifactsOk, compOsPendingArtifactsExists, useCompOsGeneratedArtifacts);
                }
                Slog.w(TAG, "Malformed metrics file");
                return;
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
            Slog.w(TAG, "Reading metrics file failed", e2);
        }
    }
}
