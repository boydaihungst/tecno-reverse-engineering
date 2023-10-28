package com.android.server;

import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import java.util.concurrent.Executor;
/* loaded from: classes.dex */
public final class UserspaceRebootLogger {
    private static final String LAST_BOOT_REASON_PROPERTY = "sys.boot.reason.last";
    private static final String TAG = "UserspaceRebootLogger";
    private static final String USERSPACE_REBOOT_LAST_FINISHED_PROPERTY = "sys.userspace_reboot.log.last_finished";
    private static final String USERSPACE_REBOOT_LAST_STARTED_PROPERTY = "sys.userspace_reboot.log.last_started";
    private static final String USERSPACE_REBOOT_SHOULD_LOG_PROPERTY = "persist.sys.userspace_reboot.log.should_log";

    private UserspaceRebootLogger() {
    }

    public static void noteUserspaceRebootWasRequested() {
        if (!PowerManager.isRebootingUserspaceSupportedImpl()) {
            Slog.wtf(TAG, "noteUserspaceRebootWasRequested: Userspace reboot is not supported.");
            return;
        }
        SystemProperties.set(USERSPACE_REBOOT_SHOULD_LOG_PROPERTY, "1");
        SystemProperties.set(USERSPACE_REBOOT_LAST_STARTED_PROPERTY, String.valueOf(SystemClock.elapsedRealtime()));
    }

    public static void noteUserspaceRebootSuccess() {
        if (!PowerManager.isRebootingUserspaceSupportedImpl()) {
            Slog.wtf(TAG, "noteUserspaceRebootSuccess: Userspace reboot is not supported.");
        } else {
            SystemProperties.set(USERSPACE_REBOOT_LAST_FINISHED_PROPERTY, String.valueOf(SystemClock.elapsedRealtime()));
        }
    }

    public static boolean shouldLogUserspaceRebootEvent() {
        if (PowerManager.isRebootingUserspaceSupportedImpl()) {
            return SystemProperties.getBoolean(USERSPACE_REBOOT_SHOULD_LOG_PROPERTY, false);
        }
        return false;
    }

    public static void logEventAsync(boolean userUnlocked, Executor executor) {
        final long durationMillis;
        if (!PowerManager.isRebootingUserspaceSupportedImpl()) {
            Slog.wtf(TAG, "logEventAsync: Userspace reboot is not supported.");
            return;
        }
        final int outcome = computeOutcome();
        final int encryptionState = 1;
        if (outcome == 1) {
            durationMillis = SystemProperties.getLong(USERSPACE_REBOOT_LAST_FINISHED_PROPERTY, 0L) - SystemProperties.getLong(USERSPACE_REBOOT_LAST_STARTED_PROPERTY, 0L);
        } else {
            durationMillis = 0;
        }
        if (!userUnlocked) {
            encryptionState = 2;
        }
        executor.execute(new Runnable() { // from class: com.android.server.UserspaceRebootLogger$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                UserspaceRebootLogger.lambda$logEventAsync$0(outcome, durationMillis, encryptionState);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$logEventAsync$0(int outcome, long durationMillis, int encryptionState) {
        Slog.i(TAG, "Logging UserspaceRebootReported atom: { outcome: " + outcome + " durationMillis: " + durationMillis + " encryptionState: " + encryptionState + " }");
        FrameworkStatsLog.write((int) FrameworkStatsLog.USERSPACE_REBOOT_REPORTED, outcome, durationMillis, encryptionState);
        SystemProperties.set(USERSPACE_REBOOT_SHOULD_LOG_PROPERTY, "");
    }

    private static int computeOutcome() {
        if (SystemProperties.getLong(USERSPACE_REBOOT_LAST_STARTED_PROPERTY, -1L) != -1) {
            return 1;
        }
        String reason = TextUtils.emptyIfNull(SystemProperties.get(LAST_BOOT_REASON_PROPERTY, ""));
        if (reason.startsWith("reboot,")) {
            reason = reason.substring("reboot".length());
        }
        if (reason.startsWith("userspace_failed,watchdog_fork") || reason.startsWith("userspace_failed,shutdown_aborted")) {
            return 2;
        }
        if (reason.startsWith("mount_userdata_failed") || reason.startsWith("userspace_failed,init_user0") || reason.startsWith("userspace_failed,enablefilecrypto")) {
            return 3;
        }
        if (reason.startsWith("userspace_failed,watchdog_triggered")) {
            return 4;
        }
        return 0;
    }
}
