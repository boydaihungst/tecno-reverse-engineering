package com.android.server.power;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.DeviceConfig;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import java.util.Set;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public class ScreenUndimDetector {
    private static final boolean DEBUG = false;
    private static final boolean DEFAULT_KEEP_SCREEN_ON_ENABLED = true;
    static final long DEFAULT_KEEP_SCREEN_ON_FOR_MILLIS = TimeUnit.MINUTES.toMillis(10);
    static final long DEFAULT_MAX_DURATION_BETWEEN_UNDIMS_MILLIS = TimeUnit.MINUTES.toMillis(5);
    static final int DEFAULT_UNDIMS_REQUIRED = 2;
    static final String KEY_KEEP_SCREEN_ON_ENABLED = "keep_screen_on_enabled";
    static final String KEY_KEEP_SCREEN_ON_FOR_MILLIS = "keep_screen_on_for_millis";
    static final String KEY_MAX_DURATION_BETWEEN_UNDIMS_MILLIS = "max_duration_between_undims_millis";
    static final String KEY_UNDIMS_REQUIRED = "undims_required";
    private static final int OUTCOME_POWER_BUTTON = 1;
    private static final int OUTCOME_TIMEOUT = 2;
    private static final String TAG = "ScreenUndimDetector";
    private static final String UNDIM_DETECTOR_WAKE_LOCK = "UndimDetectorWakeLock";
    private InternalClock mClock;
    int mCurrentScreenPolicy;
    private long mInteractionAfterUndimTime;
    private boolean mKeepScreenOnEnabled;
    private long mKeepScreenOnForMillis;
    private long mMaxDurationBetweenUndimsMillis;
    int mUndimCounter;
    long mUndimCounterStartedMillis;
    private long mUndimOccurredTime;
    private int mUndimsRequired;
    PowerManager.WakeLock mWakeLock;

    public ScreenUndimDetector() {
        this.mUndimCounter = 0;
        this.mUndimOccurredTime = -1L;
        this.mInteractionAfterUndimTime = -1L;
        this.mClock = new InternalClock();
    }

    ScreenUndimDetector(InternalClock clock) {
        this.mUndimCounter = 0;
        this.mUndimOccurredTime = -1L;
        this.mInteractionAfterUndimTime = -1L;
        this.mClock = clock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class InternalClock {
        InternalClock() {
        }

        public long getCurrentTime() {
            return SystemClock.elapsedRealtime();
        }
    }

    public void systemReady(Context context) {
        readValuesFromDeviceConfig();
        DeviceConfig.addOnPropertiesChangedListener("attention_manager_service", context.getMainExecutor(), new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.power.ScreenUndimDetector$$ExternalSyntheticLambda0
            public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                ScreenUndimDetector.this.m6175xd0d19782(properties);
            }
        });
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mWakeLock = powerManager.newWakeLock(536870922, UNDIM_DETECTOR_WAKE_LOCK);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$systemReady$0$com-android-server-power-ScreenUndimDetector  reason: not valid java name */
    public /* synthetic */ void m6175xd0d19782(DeviceConfig.Properties properties) {
        onDeviceConfigChange(properties.getKeyset());
    }

    public void recordScreenPolicy(int displayGroupId, int newPolicy) {
        if (displayGroupId != 0 || newPolicy == this.mCurrentScreenPolicy) {
            return;
        }
        int currentPolicy = this.mCurrentScreenPolicy;
        this.mCurrentScreenPolicy = newPolicy;
        if (!this.mKeepScreenOnEnabled) {
            return;
        }
        switch (currentPolicy) {
            case 2:
                if (newPolicy == 3) {
                    long now = this.mClock.getCurrentTime();
                    long timeElapsedSinceFirstUndim = now - this.mUndimCounterStartedMillis;
                    if (timeElapsedSinceFirstUndim >= this.mMaxDurationBetweenUndimsMillis) {
                        reset();
                    }
                    int i = this.mUndimCounter;
                    if (i == 0) {
                        this.mUndimCounterStartedMillis = now;
                    }
                    int i2 = i + 1;
                    this.mUndimCounter = i2;
                    if (i2 >= this.mUndimsRequired) {
                        reset();
                        if (this.mWakeLock != null) {
                            this.mUndimOccurredTime = this.mClock.getCurrentTime();
                            this.mWakeLock.acquire(this.mKeepScreenOnForMillis);
                            return;
                        }
                        return;
                    }
                    return;
                }
                if (newPolicy == 0 || newPolicy == 1) {
                    checkAndLogUndim(2);
                }
                reset();
                return;
            case 3:
                if (newPolicy == 0 || newPolicy == 1) {
                    checkAndLogUndim(1);
                }
                if (newPolicy != 2) {
                    reset();
                    return;
                }
                return;
            default:
                return;
        }
    }

    void reset() {
        this.mUndimCounter = 0;
        this.mUndimCounterStartedMillis = 0L;
        PowerManager.WakeLock wakeLock = this.mWakeLock;
        if (wakeLock != null && wakeLock.isHeld()) {
            this.mWakeLock.release();
        }
    }

    private boolean readKeepScreenOnNotificationEnabled() {
        return DeviceConfig.getBoolean("attention_manager_service", KEY_KEEP_SCREEN_ON_ENABLED, true);
    }

    private long readKeepScreenOnForMillis() {
        return DeviceConfig.getLong("attention_manager_service", KEY_KEEP_SCREEN_ON_FOR_MILLIS, DEFAULT_KEEP_SCREEN_ON_FOR_MILLIS);
    }

    private int readUndimsRequired() {
        int undimsRequired = DeviceConfig.getInt("attention_manager_service", KEY_UNDIMS_REQUIRED, 2);
        if (undimsRequired < 1 || undimsRequired > 5) {
            Slog.e(TAG, "Provided undimsRequired=" + undimsRequired + " is not allowed [1, 5]; using the default=2");
            return 2;
        }
        return undimsRequired;
    }

    private long readMaxDurationBetweenUndimsMillis() {
        return DeviceConfig.getLong("attention_manager_service", KEY_MAX_DURATION_BETWEEN_UNDIMS_MILLIS, DEFAULT_MAX_DURATION_BETWEEN_UNDIMS_MILLIS);
    }

    private void onDeviceConfigChange(Set<String> keys) {
        for (String key : keys) {
            Slog.i(TAG, "onDeviceConfigChange; key=" + key);
            char c = 65535;
            switch (key.hashCode()) {
                case -2114725254:
                    if (key.equals(KEY_UNDIMS_REQUIRED)) {
                        c = 2;
                        break;
                    }
                    break;
                case -1871288230:
                    if (key.equals(KEY_KEEP_SCREEN_ON_ENABLED)) {
                        c = 0;
                        break;
                    }
                    break;
                case 352003779:
                    if (key.equals(KEY_KEEP_SCREEN_ON_FOR_MILLIS)) {
                        c = 1;
                        break;
                    }
                    break;
                case 1709324730:
                    if (key.equals(KEY_MAX_DURATION_BETWEEN_UNDIMS_MILLIS)) {
                        c = 3;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                case 2:
                case 3:
                    readValuesFromDeviceConfig();
                    return;
                default:
                    Slog.i(TAG, "Ignoring change on " + key);
            }
        }
    }

    void readValuesFromDeviceConfig() {
        this.mKeepScreenOnEnabled = readKeepScreenOnNotificationEnabled();
        this.mKeepScreenOnForMillis = readKeepScreenOnForMillis();
        this.mUndimsRequired = readUndimsRequired();
        this.mMaxDurationBetweenUndimsMillis = readMaxDurationBetweenUndimsMillis();
        Slog.i(TAG, "readValuesFromDeviceConfig():\nmKeepScreenOnForMillis=" + this.mKeepScreenOnForMillis + "\nmKeepScreenOnNotificationEnabled=" + this.mKeepScreenOnEnabled + "\nmUndimsRequired=" + this.mUndimsRequired);
    }

    public void userActivity(int displayGroupId) {
        if (displayGroupId == 0 && this.mUndimOccurredTime != 1 && this.mInteractionAfterUndimTime == -1) {
            this.mInteractionAfterUndimTime = this.mClock.getCurrentTime();
        }
    }

    private void checkAndLogUndim(int outcome) {
        if (this.mUndimOccurredTime != -1) {
            long now = this.mClock.getCurrentTime();
            long j = now - this.mUndimOccurredTime;
            long j2 = this.mInteractionAfterUndimTime;
            FrameworkStatsLog.write(365, outcome, j, j2 != -1 ? now - j2 : -1L);
            this.mUndimOccurredTime = -1L;
            this.mInteractionAfterUndimTime = -1L;
        }
    }
}
