package com.android.server.timezonedetector;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import com.android.server.timezonedetector.TimeZoneDetectorStrategyImpl;
import java.util.Objects;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class EnvironmentImpl implements TimeZoneDetectorStrategyImpl.Environment {
    private static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    private final Context mContext;
    private final Handler mHandler;
    private final ServiceConfigAccessor mServiceConfigAccessor;

    /* JADX INFO: Access modifiers changed from: package-private */
    public EnvironmentImpl(Context context, Handler handler, ServiceConfigAccessor serviceConfigAccessor) {
        this.mContext = (Context) Objects.requireNonNull(context);
        this.mHandler = (Handler) Objects.requireNonNull(handler);
        this.mServiceConfigAccessor = (ServiceConfigAccessor) Objects.requireNonNull(serviceConfigAccessor);
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategyImpl.Environment
    public void setConfigurationInternalChangeListener(final ConfigurationChangeListener listener) {
        ConfigurationChangeListener configurationChangeListener = new ConfigurationChangeListener() { // from class: com.android.server.timezonedetector.EnvironmentImpl$$ExternalSyntheticLambda1
            @Override // com.android.server.timezonedetector.ConfigurationChangeListener
            public final void onChange() {
                EnvironmentImpl.this.m6900xf4d29f(listener);
            }
        };
        this.mServiceConfigAccessor.addConfigurationInternalChangeListener(configurationChangeListener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setConfigurationInternalChangeListener$0$com-android-server-timezonedetector-EnvironmentImpl  reason: not valid java name */
    public /* synthetic */ void m6900xf4d29f(final ConfigurationChangeListener listener) {
        Handler handler = this.mHandler;
        Objects.requireNonNull(listener);
        handler.post(new Runnable() { // from class: com.android.server.timezonedetector.EnvironmentImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ConfigurationChangeListener.this.onChange();
            }
        });
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategyImpl.Environment
    public ConfigurationInternal getCurrentUserConfigurationInternal() {
        return this.mServiceConfigAccessor.getCurrentUserConfigurationInternal();
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategyImpl.Environment
    public boolean isDeviceTimeZoneInitialized() {
        String timeZoneId = getDeviceTimeZone();
        return (timeZoneId == null || timeZoneId.length() <= 0 || timeZoneId.equals("GMT")) ? false : true;
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategyImpl.Environment
    public String getDeviceTimeZone() {
        return SystemProperties.get(TIMEZONE_PROPERTY);
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategyImpl.Environment
    public void setDeviceTimeZone(String zoneId) {
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        alarmManager.setTimeZone(zoneId);
    }

    @Override // com.android.server.timezonedetector.TimeZoneDetectorStrategyImpl.Environment
    public long elapsedRealtimeMillis() {
        return SystemClock.elapsedRealtime();
    }
}
