package com.android.server.am;

import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import java.io.DataInputStream;
import java.io.IOException;
/* loaded from: classes.dex */
public final class LmkdStatsReporter {
    private static final int DIRECT_RECL_AND_THRASHING = 5;
    public static final int KILL_OCCURRED_MSG_SIZE = 80;
    private static final int LOW_FILECACHE_AFTER_THRASHING = 7;
    private static final int LOW_MEM_AND_SWAP = 3;
    private static final int LOW_MEM_AND_SWAP_UTIL = 6;
    private static final int LOW_MEM_AND_THRASHING = 4;
    private static final int LOW_SWAP_AND_THRASHING = 2;
    private static final int NOT_RESPONDING = 1;
    private static final int PRESSURE_AFTER_KILL = 0;
    public static final int STATE_CHANGED_MSG_SIZE = 8;
    static final String TAG = "ActivityManager";

    public static void logKillOccurred(DataInputStream inputData) {
        try {
            long pgFault = inputData.readLong();
            long pgMajFault = inputData.readLong();
            long rssInBytes = inputData.readLong();
            long cacheInBytes = inputData.readLong();
            long swapInBytes = inputData.readLong();
            long processStartTimeNS = inputData.readLong();
            int uid = inputData.readInt();
            int oomScore = inputData.readInt();
            int minOomScore = inputData.readInt();
            int freeMemKb = inputData.readInt();
            int freeSwapKb = inputData.readInt();
            int killReason = inputData.readInt();
            int thrashing = inputData.readInt();
            int maxThrashing = inputData.readInt();
            String procName = inputData.readUTF();
            FrameworkStatsLog.write(51, uid, procName, oomScore, pgFault, pgMajFault, rssInBytes, cacheInBytes, swapInBytes, processStartTimeNS, minOomScore, freeMemKb, freeSwapKb, mapKillReason(killReason), thrashing, maxThrashing);
        } catch (IOException e) {
            Slog.e(TAG, "Invalid buffer data. Failed to log LMK_KILL_OCCURRED");
        }
    }

    public static void logStateChanged(int state) {
        FrameworkStatsLog.write(54, state);
    }

    private static int mapKillReason(int reason) {
        switch (reason) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
                return 7;
            case 7:
                return 8;
            default:
                return 0;
        }
    }
}
