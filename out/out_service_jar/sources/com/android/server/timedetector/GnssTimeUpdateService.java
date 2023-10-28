package com.android.server.timedetector;

import android.app.AlarmManager;
import android.app.timedetector.GnssTimeSuggestion;
import android.app.timedetector.TimeDetector;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationManagerInternal;
import android.location.LocationRequest;
import android.location.LocationTime;
import android.os.Binder;
import android.os.SystemClock;
import android.os.TimestampedValue;
import android.util.Log;
import com.android.internal.util.DumpUtils;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Duration;
/* loaded from: classes2.dex */
public final class GnssTimeUpdateService extends Binder {
    private static final String ATTRIBUTION_TAG = "GnssTimeUpdateService";
    private static final boolean D = Log.isLoggable("GnssTimeUpdateService", 3);
    private static final Duration GNSS_TIME_UPDATE_ALARM_INTERVAL = Duration.ofHours(4);
    private static final String TAG = "GnssTimeUpdateService";
    private AlarmManager.OnAlarmListener mAlarmListener;
    private final AlarmManager mAlarmManager;
    private final Context mContext;
    private TimestampedValue<Long> mLastSuggestedGnssTime;
    private LocationListener mLocationListener;
    private final LocationManager mLocationManager;
    private final LocationManagerInternal mLocationManagerInternal;
    private final TimeDetector mTimeDetector;

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        private GnssTimeUpdateService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            GnssTimeUpdateService gnssTimeUpdateService = new GnssTimeUpdateService(getContext());
            this.mService = gnssTimeUpdateService;
            publishBinderService("gnss_time_update_service", gnssTimeUpdateService);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 600) {
                this.mService.requestGnssTimeUpdates();
            }
        }
    }

    GnssTimeUpdateService(Context context) {
        Context createAttributionContext = context.createAttributionContext("GnssTimeUpdateService");
        this.mContext = createAttributionContext;
        this.mTimeDetector = (TimeDetector) createAttributionContext.getSystemService(TimeDetector.class);
        this.mLocationManager = (LocationManager) createAttributionContext.getSystemService(LocationManager.class);
        this.mAlarmManager = (AlarmManager) createAttributionContext.getSystemService(AlarmManager.class);
        this.mLocationManagerInternal = (LocationManagerInternal) LocalServices.getService(LocationManagerInternal.class);
    }

    void requestGnssTimeUpdates() {
        if (D) {
            Log.d("GnssTimeUpdateService", "requestGnssTimeUpdates()");
        }
        if (!this.mLocationManager.hasProvider("gps")) {
            Log.e("GnssTimeUpdateService", "GPS provider does not exist on this device");
            return;
        }
        this.mLocationListener = new LocationListener() { // from class: com.android.server.timedetector.GnssTimeUpdateService.1
            @Override // android.location.LocationListener
            public void onLocationChanged(Location location) {
                if (GnssTimeUpdateService.D) {
                    Log.d("GnssTimeUpdateService", "onLocationChanged()");
                }
                LocationTime locationTime = GnssTimeUpdateService.this.mLocationManagerInternal.getGnssTimeMillis();
                if (locationTime != null) {
                    GnssTimeUpdateService.this.suggestGnssTime(locationTime);
                } else if (GnssTimeUpdateService.D) {
                    Log.d("GnssTimeUpdateService", "getGnssTimeMillis() returned null");
                }
                GnssTimeUpdateService.this.mLocationManager.removeUpdates(GnssTimeUpdateService.this.mLocationListener);
                GnssTimeUpdateService.this.mLocationListener = null;
                GnssTimeUpdateService.this.mAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.timedetector.GnssTimeUpdateService.1.1
                    @Override // android.app.AlarmManager.OnAlarmListener
                    public void onAlarm() {
                        if (GnssTimeUpdateService.D) {
                            Log.d("GnssTimeUpdateService", "onAlarm()");
                        }
                        GnssTimeUpdateService.this.mAlarmListener = null;
                        GnssTimeUpdateService.this.requestGnssTimeUpdates();
                    }
                };
                long next = SystemClock.elapsedRealtime() + GnssTimeUpdateService.GNSS_TIME_UPDATE_ALARM_INTERVAL.toMillis();
                GnssTimeUpdateService.this.mAlarmManager.set(2, next, "GnssTimeUpdateService", GnssTimeUpdateService.this.mAlarmListener, FgThread.getHandler());
            }
        };
        this.mLocationManager.requestLocationUpdates("gps", new LocationRequest.Builder((long) JobStatus.NO_LATEST_RUNTIME).setMinUpdateIntervalMillis(0L).build(), FgThread.getExecutor(), this.mLocationListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void suggestGnssTime(LocationTime locationTime) {
        if (D) {
            Log.d("GnssTimeUpdateService", "suggestGnssTime()");
        }
        long gnssTime = locationTime.getTime();
        long elapsedRealtimeMs = locationTime.getElapsedRealtimeNanos() / 1000000;
        TimestampedValue<Long> timeSignal = new TimestampedValue<>(elapsedRealtimeMs, Long.valueOf(gnssTime));
        this.mLastSuggestedGnssTime = timeSignal;
        GnssTimeSuggestion timeSuggestion = new GnssTimeSuggestion(timeSignal);
        this.mTimeDetector.suggestGnssTime(timeSuggestion);
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, "GnssTimeUpdateService", pw)) {
            pw.println("mLastSuggestedGnssTime: " + this.mLastSuggestedGnssTime);
            pw.print("state: ");
            if (this.mLocationListener != null) {
                pw.println("time updates enabled");
            } else {
                pw.println("alarm enabled");
            }
        }
    }
}
