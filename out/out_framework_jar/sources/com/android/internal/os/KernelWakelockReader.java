package com.android.internal.os;

import android.bluetooth.hci.BluetoothHciProtoEnums;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.system.suspend.internal.ISuspendControlServiceInternal;
import android.system.suspend.internal.WakeLockInfo;
import android.util.Slog;
import com.android.internal.os.KernelWakelockStats;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
/* loaded from: classes4.dex */
public class KernelWakelockReader {
    private static final String TAG = "KernelWakelockReader";
    private static final String sSysClassWakeupDir = "/sys/class/wakeup";
    private static final String sWakelockFile = "/proc/wakelocks";
    private static final String sWakeupSourceFile = "/d/wakeup_sources";
    private static int sKernelWakelockUpdateVersion = 0;
    private static final int[] PROC_WAKELOCKS_FORMAT = {BluetoothHciProtoEnums.CMD_READ_LOCAL_AMP_INFO, 8201, 9, 9, 9, 8201};
    private static final int[] WAKEUP_SOURCES_FORMAT = {4105, 8457, 265, 265, 265, 265, 8457};
    private final String[] mProcWakelocksName = new String[3];
    private final long[] mProcWakelocksData = new long[3];
    private ISuspendControlServiceInternal mSuspendControlService = null;
    private byte[] mKernelWakelockBuffer = new byte[32768];

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [125=5] */
    public final KernelWakelockStats readKernelWakelockStats(KernelWakelockStats staleStats) {
        FileInputStream is;
        boolean wakeup_sources;
        int i;
        KernelWakelockStats removeOldStats;
        boolean useSystemSuspend = new File(sSysClassWakeupDir).exists();
        if (useSystemSuspend) {
            synchronized (KernelWakelockReader.class) {
                updateVersion(staleStats);
                if (getWakelockStatsFromSystemSuspend(staleStats) == null) {
                    Slog.w(TAG, "Failed to get wakelock stats from SystemSuspend");
                    return null;
                }
                return removeOldStats(staleStats);
            }
        }
        Arrays.fill(this.mKernelWakelockBuffer, (byte) 0);
        int len = 0;
        long startTime = SystemClock.uptimeMillis();
        int oldMask = StrictMode.allowThreadDiskReadsMask();
        try {
            try {
                is = new FileInputStream(sWakelockFile);
                wakeup_sources = false;
            } catch (FileNotFoundException e) {
                try {
                    FileInputStream is2 = new FileInputStream(sWakeupSourceFile);
                    is = is2;
                    wakeup_sources = true;
                } catch (FileNotFoundException e2) {
                    Slog.wtf(TAG, "neither /proc/wakelocks nor /d/wakeup_sources exists");
                    return null;
                }
            }
            while (true) {
                byte[] bArr = this.mKernelWakelockBuffer;
                int cnt = is.read(bArr, len, bArr.length - len);
                if (cnt <= 0) {
                    break;
                }
                len += cnt;
            }
            is.close();
            StrictMode.setThreadPolicyMask(oldMask);
            long readTime = SystemClock.uptimeMillis() - startTime;
            if (readTime > 100) {
                Slog.w(TAG, "Reading wakelock stats took " + readTime + "ms");
            }
            if (len > 0) {
                if (len >= this.mKernelWakelockBuffer.length) {
                    Slog.wtf(TAG, "Kernel wake locks exceeded mKernelWakelockBuffer size " + this.mKernelWakelockBuffer.length);
                }
                i = 0;
                while (i < len) {
                    if (this.mKernelWakelockBuffer[i] == 0) {
                        break;
                    }
                    i++;
                }
            }
            i = len;
            synchronized (KernelWakelockReader.class) {
                updateVersion(staleStats);
                if (getWakelockStatsFromSystemSuspend(staleStats) == null) {
                    Slog.w(TAG, "Failed to get Native wakelock stats from SystemSuspend");
                }
                parseProcWakelocks(this.mKernelWakelockBuffer, i, wakeup_sources, staleStats);
                removeOldStats = removeOldStats(staleStats);
            }
            return removeOldStats;
        } catch (IOException e3) {
            Slog.wtf(TAG, "failed to read kernel wakelocks", e3);
            return null;
        } finally {
            StrictMode.setThreadPolicyMask(oldMask);
        }
    }

    private ISuspendControlServiceInternal waitForSuspendControlService() throws ServiceManager.ServiceNotFoundException {
        for (int i = 0; i < 5; i++) {
            ISuspendControlServiceInternal asInterface = ISuspendControlServiceInternal.Stub.asInterface(ServiceManager.getService("suspend_control_internal"));
            this.mSuspendControlService = asInterface;
            if (asInterface != null) {
                return asInterface;
            }
        }
        throw new ServiceManager.ServiceNotFoundException("suspend_control_internal");
    }

    private KernelWakelockStats getWakelockStatsFromSystemSuspend(KernelWakelockStats staleStats) {
        try {
            ISuspendControlServiceInternal waitForSuspendControlService = waitForSuspendControlService();
            this.mSuspendControlService = waitForSuspendControlService;
            try {
                WakeLockInfo[] wlStats = waitForSuspendControlService.getWakeLockStats();
                updateWakelockStats(wlStats, staleStats);
                return staleStats;
            } catch (RemoteException e) {
                Slog.wtf(TAG, "Failed to obtain wakelock stats from ISuspendControlService", e);
                return null;
            }
        } catch (ServiceManager.ServiceNotFoundException e2) {
            Slog.wtf(TAG, "Required service suspend_control not available", e2);
            return null;
        }
    }

    public KernelWakelockStats updateWakelockStats(WakeLockInfo[] wlStats, KernelWakelockStats staleStats) {
        for (WakeLockInfo info : wlStats) {
            if (!staleStats.containsKey(info.name)) {
                staleStats.put(info.name, new KernelWakelockStats.Entry((int) info.activeCount, info.totalTime * 1000, sKernelWakelockUpdateVersion));
            } else {
                KernelWakelockStats.Entry kwlStats = staleStats.get(info.name);
                kwlStats.mCount = (int) info.activeCount;
                kwlStats.mTotalTime = info.totalTime * 1000;
                kwlStats.mVersion = sKernelWakelockUpdateVersion;
            }
        }
        return staleStats;
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:61:0x00f2 -> B:59:0x00f0). Please submit an issue!!! */
    public KernelWakelockStats parseProcWakelocks(byte[] wlBuffer, int len, boolean wakeup_sources, KernelWakelockStats staleStats) {
        byte b;
        long totalTime;
        int i = 0;
        while (true) {
            b = 10;
            if (i >= len || wlBuffer[i] == 10 || wlBuffer[i] == 0) {
                break;
            }
            i++;
        }
        int startIndex = i + 1;
        int endIndex = startIndex;
        synchronized (this) {
            int startIndex2 = startIndex;
            while (endIndex < len) {
                int endIndex2 = startIndex2;
                while (endIndex2 < len) {
                    try {
                        if (wlBuffer[endIndex2] == b || wlBuffer[endIndex2] == 0) {
                            break;
                        }
                        endIndex2++;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                if (endIndex2 > len - 1) {
                    endIndex = endIndex2;
                    break;
                }
                String[] nameStringArray = this.mProcWakelocksName;
                long[] wlData = this.mProcWakelocksData;
                for (int j = startIndex2; j < endIndex2; j++) {
                    if ((wlBuffer[j] & 128) != 0) {
                        wlBuffer[j] = 63;
                    }
                }
                boolean parsed = Process.parseProcLine(wlBuffer, startIndex2, endIndex2, wakeup_sources ? WAKEUP_SOURCES_FORMAT : PROC_WAKELOCKS_FORMAT, nameStringArray, wlData, null);
                String name = nameStringArray[0].trim();
                int count = (int) wlData[1];
                if (wakeup_sources) {
                    totalTime = wlData[2] * 1000;
                } else {
                    long totalTime2 = wlData[2];
                    totalTime = (totalTime2 + 500) / 1000;
                }
                if (parsed && name.length() > 0) {
                    if (!staleStats.containsKey(name)) {
                        staleStats.put(name, new KernelWakelockStats.Entry(count, totalTime, sKernelWakelockUpdateVersion));
                    } else {
                        KernelWakelockStats.Entry kwlStats = (KernelWakelockStats.Entry) staleStats.get(name);
                        if (kwlStats.mVersion == sKernelWakelockUpdateVersion) {
                            kwlStats.mCount += count;
                            kwlStats.mTotalTime += totalTime;
                        } else {
                            kwlStats.mCount = count;
                            kwlStats.mTotalTime = totalTime;
                            kwlStats.mVersion = sKernelWakelockUpdateVersion;
                        }
                    }
                } else if (!parsed) {
                    try {
                        Slog.wtf(TAG, "Failed to parse proc line: " + new String(wlBuffer, startIndex2, endIndex2 - startIndex2));
                    } catch (Exception e) {
                        Slog.wtf(TAG, "Failed to parse proc line!");
                    }
                }
                startIndex2 = endIndex2 + 1;
                endIndex = endIndex2;
                b = 10;
            }
            try {
                return staleStats;
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public KernelWakelockStats updateVersion(KernelWakelockStats staleStats) {
        int i = sKernelWakelockUpdateVersion + 1;
        sKernelWakelockUpdateVersion = i;
        staleStats.kernelWakelockVersion = i;
        return staleStats;
    }

    public KernelWakelockStats removeOldStats(KernelWakelockStats staleStats) {
        Iterator<KernelWakelockStats.Entry> itr = staleStats.values().iterator();
        while (itr.hasNext()) {
            if (itr.next().mVersion != sKernelWakelockUpdateVersion) {
                itr.remove();
            }
        }
        return staleStats;
    }
}
