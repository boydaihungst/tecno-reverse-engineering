package com.android.server.locksettings;

import android.app.AlarmManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.IStrongAuthTracker;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.widget.LockPatternUtils;
/* loaded from: classes.dex */
public class LockSettingsStrongAuth {
    private static final boolean DEBUG = false;
    public static final long DEFAULT_NON_STRONG_BIOMETRIC_IDLE_TIMEOUT_MS = 14400000;
    public static final long DEFAULT_NON_STRONG_BIOMETRIC_TIMEOUT_MS = 86400000;
    private static final int MSG_NO_LONGER_REQUIRE_STRONG_AUTH = 6;
    private static final int MSG_REFRESH_STRONG_AUTH_TIMEOUT = 10;
    private static final int MSG_REGISTER_TRACKER = 2;
    private static final int MSG_REMOVE_USER = 4;
    private static final int MSG_REQUIRE_STRONG_AUTH = 1;
    private static final int MSG_SCHEDULE_NON_STRONG_BIOMETRIC_IDLE_TIMEOUT = 9;
    private static final int MSG_SCHEDULE_NON_STRONG_BIOMETRIC_TIMEOUT = 7;
    private static final int MSG_SCHEDULE_STRONG_AUTH_TIMEOUT = 5;
    private static final int MSG_STRONG_BIOMETRIC_UNLOCK = 8;
    private static final int MSG_UNREGISTER_TRACKER = 3;
    protected static final String NON_STRONG_BIOMETRIC_IDLE_TIMEOUT_ALARM_TAG = "LockSettingsPrimaryAuth.nonStrongBiometricIdleTimeoutForUser";
    protected static final String NON_STRONG_BIOMETRIC_TIMEOUT_ALARM_TAG = "LockSettingsPrimaryAuth.nonStrongBiometricTimeoutForUser";
    protected static final String STRONG_AUTH_TIMEOUT_ALARM_TAG = "LockSettingsStrongAuth.timeoutForUser";
    private static final String TAG = "LockSettings";
    private final AlarmManager mAlarmManager;
    private final Context mContext;
    private final boolean mDefaultIsNonStrongBiometricAllowed;
    private final int mDefaultStrongAuthFlags;
    protected final Handler mHandler;
    private final Injector mInjector;
    protected final SparseBooleanArray mIsNonStrongBiometricAllowedForUser;
    protected final ArrayMap<Integer, NonStrongBiometricIdleTimeoutAlarmListener> mNonStrongBiometricIdleTimeoutAlarmListener;
    protected final ArrayMap<Integer, NonStrongBiometricTimeoutAlarmListener> mNonStrongBiometricTimeoutAlarmListener;
    protected final SparseIntArray mStrongAuthForUser;
    protected final ArrayMap<Integer, StrongAuthTimeoutAlarmListener> mStrongAuthTimeoutAlarmListenerForUser;
    private final RemoteCallbackList<IStrongAuthTracker> mTrackers;

    public LockSettingsStrongAuth(Context context) {
        this(context, new Injector());
    }

    protected LockSettingsStrongAuth(Context context, Injector injector) {
        this.mTrackers = new RemoteCallbackList<>();
        this.mStrongAuthForUser = new SparseIntArray();
        this.mIsNonStrongBiometricAllowedForUser = new SparseBooleanArray();
        this.mStrongAuthTimeoutAlarmListenerForUser = new ArrayMap<>();
        this.mNonStrongBiometricTimeoutAlarmListener = new ArrayMap<>();
        this.mNonStrongBiometricIdleTimeoutAlarmListener = new ArrayMap<>();
        this.mDefaultIsNonStrongBiometricAllowed = true;
        this.mHandler = new Handler(Looper.getMainLooper()) { // from class: com.android.server.locksettings.LockSettingsStrongAuth.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        LockSettingsStrongAuth.this.handleRequireStrongAuth(msg.arg1, msg.arg2);
                        return;
                    case 2:
                        LockSettingsStrongAuth.this.handleAddStrongAuthTracker((IStrongAuthTracker) msg.obj);
                        return;
                    case 3:
                        LockSettingsStrongAuth.this.handleRemoveStrongAuthTracker((IStrongAuthTracker) msg.obj);
                        return;
                    case 4:
                        LockSettingsStrongAuth.this.handleRemoveUser(msg.arg1);
                        return;
                    case 5:
                        LockSettingsStrongAuth.this.handleScheduleStrongAuthTimeout(msg.arg1);
                        return;
                    case 6:
                        LockSettingsStrongAuth.this.handleNoLongerRequireStrongAuth(msg.arg1, msg.arg2);
                        return;
                    case 7:
                        LockSettingsStrongAuth.this.handleScheduleNonStrongBiometricTimeout(msg.arg1);
                        return;
                    case 8:
                        LockSettingsStrongAuth.this.handleStrongBiometricUnlock(msg.arg1);
                        return;
                    case 9:
                        LockSettingsStrongAuth.this.handleScheduleNonStrongBiometricIdleTimeout(msg.arg1);
                        return;
                    case 10:
                        LockSettingsStrongAuth.this.handleRefreshStrongAuthTimeout(msg.arg1);
                        return;
                    default:
                        return;
                }
            }
        };
        this.mContext = context;
        this.mInjector = injector;
        this.mDefaultStrongAuthFlags = injector.getDefaultStrongAuthFlags(context);
        this.mAlarmManager = injector.getAlarmManager(context);
    }

    /* loaded from: classes.dex */
    public static class Injector {
        public AlarmManager getAlarmManager(Context context) {
            return (AlarmManager) context.getSystemService(AlarmManager.class);
        }

        public int getDefaultStrongAuthFlags(Context context) {
            return LockPatternUtils.StrongAuthTracker.getDefaultFlags(context);
        }

        public long getNextAlarmTimeMs(long timeout) {
            return SystemClock.elapsedRealtime() + timeout;
        }

        public long getElapsedRealtimeMs() {
            return SystemClock.elapsedRealtime();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAddStrongAuthTracker(IStrongAuthTracker tracker) {
        this.mTrackers.register(tracker);
        for (int i = 0; i < this.mStrongAuthForUser.size(); i++) {
            int key = this.mStrongAuthForUser.keyAt(i);
            int value = this.mStrongAuthForUser.valueAt(i);
            try {
                tracker.onStrongAuthRequiredChanged(value, key);
            } catch (RemoteException e) {
                Slog.e(TAG, "Exception while adding StrongAuthTracker.", e);
            }
        }
        for (int i2 = 0; i2 < this.mIsNonStrongBiometricAllowedForUser.size(); i2++) {
            int key2 = this.mIsNonStrongBiometricAllowedForUser.keyAt(i2);
            boolean value2 = this.mIsNonStrongBiometricAllowedForUser.valueAt(i2);
            try {
                tracker.onIsNonStrongBiometricAllowedChanged(value2, key2);
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while adding StrongAuthTracker: IsNonStrongBiometricAllowedChanged.", e2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRemoveStrongAuthTracker(IStrongAuthTracker tracker) {
        this.mTrackers.unregister(tracker);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRequireStrongAuth(int strongAuthReason, int userId) {
        if (userId == -1) {
            for (int i = 0; i < this.mStrongAuthForUser.size(); i++) {
                int key = this.mStrongAuthForUser.keyAt(i);
                handleRequireStrongAuthOneUser(strongAuthReason, key);
            }
            return;
        }
        handleRequireStrongAuthOneUser(strongAuthReason, userId);
    }

    private void handleRequireStrongAuthOneUser(int strongAuthReason, int userId) {
        int newValue;
        int oldValue = this.mStrongAuthForUser.get(userId, this.mDefaultStrongAuthFlags);
        if (strongAuthReason == 0) {
            newValue = 0;
        } else {
            newValue = oldValue | strongAuthReason;
        }
        if (oldValue != newValue) {
            this.mStrongAuthForUser.put(userId, newValue);
            notifyStrongAuthTrackers(newValue, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNoLongerRequireStrongAuth(int strongAuthReason, int userId) {
        if (userId == -1) {
            for (int i = 0; i < this.mStrongAuthForUser.size(); i++) {
                int key = this.mStrongAuthForUser.keyAt(i);
                handleNoLongerRequireStrongAuthOneUser(strongAuthReason, key);
            }
            return;
        }
        handleNoLongerRequireStrongAuthOneUser(strongAuthReason, userId);
    }

    private void handleNoLongerRequireStrongAuthOneUser(int strongAuthReason, int userId) {
        int oldValue = this.mStrongAuthForUser.get(userId, this.mDefaultStrongAuthFlags);
        int newValue = (~strongAuthReason) & oldValue;
        if (oldValue != newValue) {
            this.mStrongAuthForUser.put(userId, newValue);
            notifyStrongAuthTrackers(newValue, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRemoveUser(int userId) {
        int index = this.mStrongAuthForUser.indexOfKey(userId);
        if (index >= 0) {
            this.mStrongAuthForUser.removeAt(index);
            notifyStrongAuthTrackers(this.mDefaultStrongAuthFlags, userId);
        }
        int index2 = this.mIsNonStrongBiometricAllowedForUser.indexOfKey(userId);
        if (index2 >= 0) {
            this.mIsNonStrongBiometricAllowedForUser.removeAt(index2);
            notifyStrongAuthTrackersForIsNonStrongBiometricAllowed(true, userId);
        }
    }

    private void rescheduleStrongAuthTimeoutAlarm(long strongAuthTime, int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        StrongAuthTimeoutAlarmListener alarm = this.mStrongAuthTimeoutAlarmListenerForUser.get(Integer.valueOf(userId));
        if (alarm != null) {
            this.mAlarmManager.cancel(alarm);
            alarm.setLatestStrongAuthTime(strongAuthTime);
        } else {
            alarm = new StrongAuthTimeoutAlarmListener(strongAuthTime, userId);
            this.mStrongAuthTimeoutAlarmListenerForUser.put(Integer.valueOf(userId), alarm);
        }
        long nextAlarmTime = dpm.getRequiredStrongAuthTimeout(null, userId) + strongAuthTime;
        this.mAlarmManager.setExact(2, nextAlarmTime, STRONG_AUTH_TIMEOUT_ALARM_TAG, alarm, this.mHandler);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScheduleStrongAuthTimeout(int userId) {
        rescheduleStrongAuthTimeoutAlarm(this.mInjector.getElapsedRealtimeMs(), userId);
        cancelNonStrongBiometricAlarmListener(userId);
        cancelNonStrongBiometricIdleAlarmListener(userId);
        setIsNonStrongBiometricAllowed(true, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleRefreshStrongAuthTimeout(int userId) {
        StrongAuthTimeoutAlarmListener alarm = this.mStrongAuthTimeoutAlarmListenerForUser.get(Integer.valueOf(userId));
        if (alarm != null) {
            rescheduleStrongAuthTimeoutAlarm(alarm.getLatestStrongAuthTime(), userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScheduleNonStrongBiometricTimeout(int userId) {
        long nextAlarmTime = this.mInjector.getNextAlarmTimeMs(86400000L);
        if (this.mNonStrongBiometricTimeoutAlarmListener.get(Integer.valueOf(userId)) == null) {
            NonStrongBiometricTimeoutAlarmListener alarm = new NonStrongBiometricTimeoutAlarmListener(userId);
            this.mNonStrongBiometricTimeoutAlarmListener.put(Integer.valueOf(userId), alarm);
            this.mAlarmManager.setExact(2, nextAlarmTime, NON_STRONG_BIOMETRIC_TIMEOUT_ALARM_TAG, alarm, this.mHandler);
        }
        cancelNonStrongBiometricIdleAlarmListener(userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleStrongBiometricUnlock(int userId) {
        cancelNonStrongBiometricAlarmListener(userId);
        cancelNonStrongBiometricIdleAlarmListener(userId);
        setIsNonStrongBiometricAllowed(true, userId);
    }

    private void cancelNonStrongBiometricAlarmListener(int userId) {
        NonStrongBiometricTimeoutAlarmListener alarm = this.mNonStrongBiometricTimeoutAlarmListener.get(Integer.valueOf(userId));
        if (alarm != null) {
            this.mAlarmManager.cancel(alarm);
            this.mNonStrongBiometricTimeoutAlarmListener.remove(Integer.valueOf(userId));
        }
    }

    private void cancelNonStrongBiometricIdleAlarmListener(int userId) {
        NonStrongBiometricIdleTimeoutAlarmListener alarm = this.mNonStrongBiometricIdleTimeoutAlarmListener.get(Integer.valueOf(userId));
        if (alarm != null) {
            this.mAlarmManager.cancel(alarm);
        }
    }

    protected void setIsNonStrongBiometricAllowed(boolean allowed, int userId) {
        if (userId == -1) {
            for (int i = 0; i < this.mIsNonStrongBiometricAllowedForUser.size(); i++) {
                int key = this.mIsNonStrongBiometricAllowedForUser.keyAt(i);
                setIsNonStrongBiometricAllowedOneUser(allowed, key);
            }
            return;
        }
        setIsNonStrongBiometricAllowedOneUser(allowed, userId);
    }

    private void setIsNonStrongBiometricAllowedOneUser(boolean allowed, int userId) {
        boolean oldValue = this.mIsNonStrongBiometricAllowedForUser.get(userId, true);
        if (allowed != oldValue) {
            this.mIsNonStrongBiometricAllowedForUser.put(userId, allowed);
            notifyStrongAuthTrackersForIsNonStrongBiometricAllowed(allowed, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScheduleNonStrongBiometricIdleTimeout(int userId) {
        long nextAlarmTime = this.mInjector.getNextAlarmTimeMs(14400000L);
        NonStrongBiometricIdleTimeoutAlarmListener alarm = this.mNonStrongBiometricIdleTimeoutAlarmListener.get(Integer.valueOf(userId));
        if (alarm != null) {
            this.mAlarmManager.cancel(alarm);
        } else {
            alarm = new NonStrongBiometricIdleTimeoutAlarmListener(userId);
            this.mNonStrongBiometricIdleTimeoutAlarmListener.put(Integer.valueOf(userId), alarm);
        }
        this.mAlarmManager.setExact(2, nextAlarmTime, NON_STRONG_BIOMETRIC_IDLE_TIMEOUT_ALARM_TAG, alarm, this.mHandler);
    }

    private void notifyStrongAuthTrackers(int strongAuthReason, int userId) {
        int i = this.mTrackers.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                try {
                    this.mTrackers.getBroadcastItem(i).onStrongAuthRequiredChanged(strongAuthReason, userId);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Exception while notifying StrongAuthTracker.", e);
                }
            } finally {
                this.mTrackers.finishBroadcast();
            }
        }
    }

    private void notifyStrongAuthTrackersForIsNonStrongBiometricAllowed(boolean allowed, int userId) {
        int i = this.mTrackers.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                try {
                    this.mTrackers.getBroadcastItem(i).onIsNonStrongBiometricAllowedChanged(allowed, userId);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Exception while notifying StrongAuthTracker: IsNonStrongBiometricAllowedChanged.", e);
                }
            } finally {
                this.mTrackers.finishBroadcast();
            }
        }
    }

    public void registerStrongAuthTracker(IStrongAuthTracker tracker) {
        this.mHandler.obtainMessage(2, tracker).sendToTarget();
    }

    public void unregisterStrongAuthTracker(IStrongAuthTracker tracker) {
        this.mHandler.obtainMessage(3, tracker).sendToTarget();
    }

    public void removeUser(int userId) {
        this.mHandler.obtainMessage(4, userId, 0).sendToTarget();
    }

    public void requireStrongAuth(int strongAuthReason, int userId) {
        if (userId == -1 || userId >= 0) {
            this.mHandler.obtainMessage(1, strongAuthReason, userId).sendToTarget();
            return;
        }
        throw new IllegalArgumentException("userId must be an explicit user id or USER_ALL");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void noLongerRequireStrongAuth(int strongAuthReason, int userId) {
        if (userId == -1 || userId >= 0) {
            this.mHandler.obtainMessage(6, strongAuthReason, userId).sendToTarget();
            return;
        }
        throw new IllegalArgumentException("userId must be an explicit user id or USER_ALL");
    }

    public void reportUnlock(int userId) {
        requireStrongAuth(0, userId);
    }

    public void reportSuccessfulStrongAuthUnlock(int userId) {
        this.mHandler.obtainMessage(5, userId, 0).sendToTarget();
    }

    public void refreshStrongAuthTimeout(int userId) {
        this.mHandler.obtainMessage(10, userId, 0).sendToTarget();
    }

    public void reportSuccessfulBiometricUnlock(boolean isStrongBiometric, int userId) {
        if (isStrongBiometric) {
            this.mHandler.obtainMessage(8, userId, 0).sendToTarget();
        } else {
            this.mHandler.obtainMessage(7, userId, 0).sendToTarget();
        }
    }

    public void scheduleNonStrongBiometricIdleTimeout(int userId) {
        this.mHandler.obtainMessage(9, userId, 0).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class StrongAuthTimeoutAlarmListener implements AlarmManager.OnAlarmListener {
        private long mLatestStrongAuthTime;
        private final int mUserId;

        public StrongAuthTimeoutAlarmListener(long latestStrongAuthTime, int userId) {
            this.mLatestStrongAuthTime = latestStrongAuthTime;
            this.mUserId = userId;
        }

        public void setLatestStrongAuthTime(long strongAuthTime) {
            this.mLatestStrongAuthTime = strongAuthTime;
        }

        public long getLatestStrongAuthTime() {
            return this.mLatestStrongAuthTime;
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            LockSettingsStrongAuth.this.requireStrongAuth(16, this.mUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class NonStrongBiometricTimeoutAlarmListener implements AlarmManager.OnAlarmListener {
        private final int mUserId;

        NonStrongBiometricTimeoutAlarmListener(int userId) {
            this.mUserId = userId;
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            LockSettingsStrongAuth.this.requireStrongAuth(128, this.mUserId);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class NonStrongBiometricIdleTimeoutAlarmListener implements AlarmManager.OnAlarmListener {
        private final int mUserId;

        NonStrongBiometricIdleTimeoutAlarmListener(int userId) {
            this.mUserId = userId;
        }

        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            LockSettingsStrongAuth.this.setIsNonStrongBiometricAllowed(false, this.mUserId);
        }
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("PrimaryAuthFlags state:");
        pw.increaseIndent();
        for (int i = 0; i < this.mStrongAuthForUser.size(); i++) {
            int key = this.mStrongAuthForUser.keyAt(i);
            int value = this.mStrongAuthForUser.valueAt(i);
            pw.println("userId=" + key + ", primaryAuthFlags=" + Integer.toHexString(value));
        }
        pw.println();
        pw.decreaseIndent();
        pw.println("NonStrongBiometricAllowed state:");
        pw.increaseIndent();
        for (int i2 = 0; i2 < this.mIsNonStrongBiometricAllowedForUser.size(); i2++) {
            int key2 = this.mIsNonStrongBiometricAllowedForUser.keyAt(i2);
            boolean value2 = this.mIsNonStrongBiometricAllowedForUser.valueAt(i2);
            pw.println("userId=" + key2 + ", allowed=" + value2);
        }
        pw.println();
        pw.decreaseIndent();
    }
}
