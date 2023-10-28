package com.android.server.biometrics.sensors.fingerprint.hidl;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.server.biometrics.sensors.LockoutTracker;
/* loaded from: classes.dex */
public class LockoutFrameworkImpl implements LockoutTracker {
    private static final String ACTION_LOCKOUT_RESET = "com.android.server.biometrics.sensors.fingerprint.ACTION_LOCKOUT_RESET";
    private static final long FAIL_LOCKOUT_TIMEOUT_MS = 30000;
    private static final String KEY_LOCKOUT_RESET_USER = "lockout_reset_user";
    private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_PERMANENT = 20;
    private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_TIMED = 5;
    private static final boolean OS_FINGERPRINT_SUPPORT = SystemProperties.get("ro.fingerprint_support").equals("1");
    private static final String TAG = "LockoutTracker";
    private final AlarmManager mAlarmManager;
    private final Context mContext;
    private final LockoutReceiver mLockoutReceiver;
    private final LockoutResetCallback mLockoutResetCallback;
    private final SparseBooleanArray mTimedLockoutCleared = new SparseBooleanArray();
    private final SparseIntArray mFailedAttempts = new SparseIntArray();

    /* loaded from: classes.dex */
    public interface LockoutResetCallback {
        void onLockoutReset(int i);
    }

    /* loaded from: classes.dex */
    private final class LockoutReceiver extends BroadcastReceiver {
        private LockoutReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Slog.v(LockoutFrameworkImpl.TAG, "Resetting lockout: " + intent.getAction());
            if (LockoutFrameworkImpl.ACTION_LOCKOUT_RESET.equals(intent.getAction())) {
                int user = intent.getIntExtra(LockoutFrameworkImpl.KEY_LOCKOUT_RESET_USER, 0);
                LockoutFrameworkImpl.this.resetFailedAttemptsForUser(false, user);
            }
        }
    }

    public LockoutFrameworkImpl(Context context, LockoutResetCallback lockoutResetCallback) {
        this.mContext = context;
        this.mLockoutResetCallback = lockoutResetCallback;
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        LockoutReceiver lockoutReceiver = new LockoutReceiver();
        this.mLockoutReceiver = lockoutReceiver;
        context.registerReceiver(lockoutReceiver, new IntentFilter(ACTION_LOCKOUT_RESET), "android.permission.RESET_FINGERPRINT_LOCKOUT", null, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetFailedAttemptsForUser(boolean clearAttemptCounter, int userId) {
        if (getLockoutModeForUser(userId) != 0) {
            Slog.v(TAG, "Reset biometric lockout for user: " + userId + ", clearAttemptCounter: " + clearAttemptCounter);
        }
        if (clearAttemptCounter) {
            this.mFailedAttempts.put(userId, 0);
        }
        this.mTimedLockoutCleared.put(userId, true);
        cancelLockoutResetForUser(userId);
        this.mLockoutResetCallback.onLockoutReset(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addFailedAttemptForUser(int userId) {
        if (!OS_FINGERPRINT_SUPPORT) {
            SparseIntArray sparseIntArray = this.mFailedAttempts;
            sparseIntArray.put(userId, sparseIntArray.get(userId, 0) + 1);
        }
        this.mTimedLockoutCleared.put(userId, false);
        if (getLockoutModeForUser(userId) != 0) {
            scheduleLockoutResetForUser(userId);
        }
    }

    @Override // com.android.server.biometrics.sensors.LockoutTracker
    public int getLockoutModeForUser(int userId) {
        int failedAttempts = this.mFailedAttempts.get(userId, 0);
        if (failedAttempts >= 20) {
            return 2;
        }
        return (failedAttempts <= 0 || this.mTimedLockoutCleared.get(userId, false) || failedAttempts % 5 != 0) ? 0 : 1;
    }

    private void cancelLockoutResetForUser(int userId) {
        this.mAlarmManager.cancel(getLockoutResetIntentForUser(userId));
    }

    private void scheduleLockoutResetForUser(int userId) {
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + 30000, getLockoutResetIntentForUser(userId));
    }

    private PendingIntent getLockoutResetIntentForUser(int userId) {
        return PendingIntent.getBroadcast(this.mContext, userId, new Intent(ACTION_LOCKOUT_RESET).putExtra(KEY_LOCKOUT_RESET_USER, userId), AudioFormat.DTS_HD);
    }
}
