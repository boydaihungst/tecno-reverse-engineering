package com.android.server.timedetector;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.timedetector.ConfigurationInternal;
import com.android.server.timedetector.TimeDetectorStrategyImpl;
import com.android.server.timezonedetector.ConfigurationChangeListener;
import java.time.Instant;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class EnvironmentImpl implements TimeDetectorStrategyImpl.Environment {
    private static final String LOG_TAG = "time_detector";
    private final AlarmManager mAlarmManager;
    private ConfigurationChangeListener mConfigChangeListener;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final Handler mHandler;
    private final ServiceConfigAccessor mServiceConfigAccessor;
    private final UserManager mUserManager;
    private final PowerManager.WakeLock mWakeLock;

    /* JADX INFO: Access modifiers changed from: package-private */
    public EnvironmentImpl(Context context, Handler handler, ServiceConfigAccessor serviceConfigAccessor) {
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mContentResolver = (ContentResolver) Objects.requireNonNull(context.getContentResolver());
        Handler handler2 = (Handler) Objects.requireNonNull(handler);
        this.mHandler = handler2;
        ServiceConfigAccessor serviceConfigAccessor2 = (ServiceConfigAccessor) Objects.requireNonNull(serviceConfigAccessor);
        this.mServiceConfigAccessor = serviceConfigAccessor2;
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mWakeLock = (PowerManager.WakeLock) Objects.requireNonNull(powerManager.newWakeLock(1, LOG_TAG));
        this.mAlarmManager = (AlarmManager) Objects.requireNonNull((AlarmManager) context.getSystemService(AlarmManager.class));
        this.mUserManager = (UserManager) Objects.requireNonNull((UserManager) context.getSystemService(UserManager.class));
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(Settings.Global.getUriFor("auto_time"), true, new ContentObserver(handler2) { // from class: com.android.server.timedetector.EnvironmentImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                EnvironmentImpl.this.handleAutoTimeDetectionChangedOnHandlerThread();
            }
        });
        serviceConfigAccessor2.addListener(new ConfigurationChangeListener() { // from class: com.android.server.timedetector.EnvironmentImpl$$ExternalSyntheticLambda0
            @Override // com.android.server.timezonedetector.ConfigurationChangeListener
            public final void onChange() {
                EnvironmentImpl.this.m6856lambda$new$0$comandroidservertimedetectorEnvironmentImpl();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-timedetector-EnvironmentImpl  reason: not valid java name */
    public /* synthetic */ void m6856lambda$new$0$comandroidservertimedetectorEnvironmentImpl() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.timedetector.EnvironmentImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                EnvironmentImpl.this.handleAutoTimeDetectionChangedOnHandlerThread();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAutoTimeDetectionChangedOnHandlerThread() {
        synchronized (this) {
            if (this.mConfigChangeListener == null) {
                Slog.wtf(LOG_TAG, "mConfigChangeListener is unexpectedly null");
            }
            this.mConfigChangeListener.onChange();
        }
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public void setConfigChangeListener(ConfigurationChangeListener listener) {
        synchronized (this) {
            this.mConfigChangeListener = (ConfigurationChangeListener) Objects.requireNonNull(listener);
        }
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public int systemClockUpdateThresholdMillis() {
        return this.mServiceConfigAccessor.systemClockUpdateThresholdMillis();
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public boolean isAutoTimeDetectionEnabled() {
        try {
            return Settings.Global.getInt(this.mContentResolver, "auto_time") != 0;
        } catch (Settings.SettingNotFoundException e) {
            return true;
        }
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public Instant autoTimeLowerBound() {
        return this.mServiceConfigAccessor.autoTimeLowerBound();
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public int[] autoOriginPriorities() {
        return this.mServiceConfigAccessor.getOriginPriorities();
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public ConfigurationInternal configurationInternal(int userId) {
        return new ConfigurationInternal.Builder(userId).setUserConfigAllowed(isUserConfigAllowed(userId)).setAutoDetectionEnabled(isAutoTimeDetectionEnabled()).build();
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public void acquireWakeLock() {
        if (this.mWakeLock.isHeld()) {
            Slog.wtf(LOG_TAG, "WakeLock " + this.mWakeLock + " already held");
        }
        this.mWakeLock.acquire();
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public long elapsedRealtimeMillis() {
        return SystemClock.elapsedRealtime();
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public long systemClockMillis() {
        return System.currentTimeMillis();
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public void setSystemClock(long newTimeMillis) {
        checkWakeLockHeld();
        this.mAlarmManager.setTime(newTimeMillis);
    }

    @Override // com.android.server.timedetector.TimeDetectorStrategyImpl.Environment
    public void releaseWakeLock() {
        checkWakeLockHeld();
        this.mWakeLock.release();
    }

    private void checkWakeLockHeld() {
        if (!this.mWakeLock.isHeld()) {
            Slog.wtf(LOG_TAG, "WakeLock " + this.mWakeLock + " not held");
        }
    }

    private boolean isUserConfigAllowed(int userId) {
        UserHandle userHandle = UserHandle.of(userId);
        return !this.mUserManager.hasUserRestriction("no_config_date_time", userHandle);
    }
}
