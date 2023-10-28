package com.android.server.pm;

import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Pair;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.pm.verify.domain.DomainVerificationLegacySettings;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public class SilentUpdatePolicy {
    private static final long SILENT_UPDATE_THROTTLE_TIME_MS = TimeUnit.SECONDS.toMillis(30);
    private String mAllowUnlimitedSilentUpdatesInstaller;
    private final ArrayMap<Pair<String, String>, Long> mSilentUpdateInfos = new ArrayMap<>();
    private long mSilentUpdateThrottleTimeMs = SILENT_UPDATE_THROTTLE_TIME_MS;

    public boolean isSilentUpdateAllowed(String installerPackageName, String packageName) {
        long throttleTimeMs;
        if (installerPackageName == null) {
            return true;
        }
        long lastSilentUpdatedMs = getTimestampMs(installerPackageName, packageName);
        synchronized (this.mSilentUpdateInfos) {
            throttleTimeMs = this.mSilentUpdateThrottleTimeMs;
        }
        return SystemClock.uptimeMillis() - lastSilentUpdatedMs > throttleTimeMs;
    }

    public void track(String installerPackageName, String packageName) {
        if (installerPackageName == null) {
            return;
        }
        synchronized (this.mSilentUpdateInfos) {
            String str = this.mAllowUnlimitedSilentUpdatesInstaller;
            if (str == null || !str.equals(installerPackageName)) {
                long uptime = SystemClock.uptimeMillis();
                pruneLocked(uptime);
                Pair<String, String> key = Pair.create(installerPackageName, packageName);
                this.mSilentUpdateInfos.put(key, Long.valueOf(uptime));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAllowUnlimitedSilentUpdates(String installerPackageName) {
        synchronized (this.mSilentUpdateInfos) {
            if (installerPackageName == null) {
                this.mSilentUpdateInfos.clear();
            }
            this.mAllowUnlimitedSilentUpdatesInstaller = installerPackageName;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSilentUpdatesThrottleTime(long throttleTimeInSeconds) {
        long j;
        synchronized (this.mSilentUpdateInfos) {
            if (throttleTimeInSeconds >= 0) {
                j = TimeUnit.SECONDS.toMillis(throttleTimeInSeconds);
            } else {
                j = SILENT_UPDATE_THROTTLE_TIME_MS;
            }
            this.mSilentUpdateThrottleTimeMs = j;
        }
    }

    private void pruneLocked(long uptime) {
        int size = this.mSilentUpdateInfos.size();
        for (int i = size - 1; i >= 0; i--) {
            long lastSilentUpdatedMs = this.mSilentUpdateInfos.valueAt(i).longValue();
            if (uptime - lastSilentUpdatedMs > this.mSilentUpdateThrottleTimeMs) {
                this.mSilentUpdateInfos.removeAt(i);
            }
        }
    }

    private long getTimestampMs(String installerPackageName, String packageName) {
        Long timestampMs;
        Pair<String, String> key = Pair.create(installerPackageName, packageName);
        synchronized (this.mSilentUpdateInfos) {
            timestampMs = this.mSilentUpdateInfos.get(key);
        }
        if (timestampMs != null) {
            return timestampMs.longValue();
        }
        return -1L;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        synchronized (this.mSilentUpdateInfos) {
            if (this.mSilentUpdateInfos.isEmpty()) {
                return;
            }
            pw.println("Last silent updated Infos:");
            pw.increaseIndent();
            int size = this.mSilentUpdateInfos.size();
            for (int i = 0; i < size; i++) {
                Pair<String, String> key = this.mSilentUpdateInfos.keyAt(i);
                if (key != null) {
                    pw.printPair("installerPackageName", key.first);
                    pw.printPair(DomainVerificationLegacySettings.ATTR_PACKAGE_NAME, key.second);
                    pw.printPair("silentUpdatedMillis", this.mSilentUpdateInfos.valueAt(i));
                    pw.println();
                }
            }
            pw.decreaseIndent();
        }
    }
}
