package com.android.server.twilight;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.util.Calendar;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.SystemService;
import com.ibm.icu.impl.CalendarAstronomer;
import java.util.Objects;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public final class TwilightService extends SystemService implements AlarmManager.OnAlarmListener, Handler.Callback, LocationListener {
    private static final String ATTRIBUTION_TAG = "TwilightService";
    private static final boolean DEBUG = false;
    private static final int MSG_START_LISTENING = 1;
    private static final int MSG_STOP_LISTENING = 2;
    private static final String TAG = "TwilightService";
    protected AlarmManager mAlarmManager;
    private boolean mBootCompleted;
    private final Handler mHandler;
    private boolean mHasListeners;
    protected Location mLastLocation;
    protected TwilightState mLastTwilightState;
    private final ArrayMap<TwilightListener, Handler> mListeners;
    private LocationManager mLocationManager;
    private BroadcastReceiver mTimeChangedReceiver;

    public TwilightService(Context context) {
        super(context.createAttributionContext("TwilightService"));
        this.mListeners = new ArrayMap<>();
        this.mHandler = new Handler(Looper.getMainLooper(), this);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishLocalService(TwilightManager.class, new TwilightManager() { // from class: com.android.server.twilight.TwilightService.1
            @Override // com.android.server.twilight.TwilightManager
            public void registerListener(TwilightListener listener, Handler handler) {
                synchronized (TwilightService.this.mListeners) {
                    boolean wasEmpty = TwilightService.this.mListeners.isEmpty();
                    TwilightService.this.mListeners.put(listener, handler);
                    if (wasEmpty && !TwilightService.this.mListeners.isEmpty()) {
                        TwilightService.this.mHandler.sendEmptyMessage(1);
                    }
                }
            }

            @Override // com.android.server.twilight.TwilightManager
            public void unregisterListener(TwilightListener listener) {
                synchronized (TwilightService.this.mListeners) {
                    boolean wasEmpty = TwilightService.this.mListeners.isEmpty();
                    TwilightService.this.mListeners.remove(listener);
                    if (!wasEmpty && TwilightService.this.mListeners.isEmpty()) {
                        TwilightService.this.mHandler.sendEmptyMessage(2);
                    }
                }
            }

            @Override // com.android.server.twilight.TwilightManager
            public TwilightState getLastTwilightState() {
                TwilightState twilightState;
                synchronized (TwilightService.this.mListeners) {
                    twilightState = TwilightService.this.mLastTwilightState;
                }
                return twilightState;
            }
        });
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 1000) {
            Context c = getContext();
            this.mAlarmManager = (AlarmManager) c.getSystemService("alarm");
            this.mLocationManager = (LocationManager) c.getSystemService("location");
            this.mBootCompleted = true;
            if (this.mHasListeners) {
                startListening();
            }
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                if (!this.mHasListeners) {
                    this.mHasListeners = true;
                    if (this.mBootCompleted) {
                        startListening();
                    }
                }
                return true;
            case 2:
                if (this.mHasListeners) {
                    this.mHasListeners = false;
                    if (this.mBootCompleted) {
                        stopListening();
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private void startListening() {
        Slog.d("TwilightService", "startListening");
        this.mLocationManager.requestLocationUpdates((LocationRequest) null, this, Looper.getMainLooper());
        if (this.mLocationManager.getLastLocation() == null) {
            if (this.mLocationManager.isProviderEnabled("network")) {
                this.mLocationManager.getCurrentLocation("network", null, getContext().getMainExecutor(), new Consumer() { // from class: com.android.server.twilight.TwilightService$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        TwilightService.this.onLocationChanged((Location) obj);
                    }
                });
            } else if (this.mLocationManager.isProviderEnabled("gps")) {
                this.mLocationManager.getCurrentLocation("gps", null, getContext().getMainExecutor(), new Consumer() { // from class: com.android.server.twilight.TwilightService$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        TwilightService.this.onLocationChanged((Location) obj);
                    }
                });
            }
        }
        if (this.mTimeChangedReceiver == null) {
            this.mTimeChangedReceiver = new BroadcastReceiver() { // from class: com.android.server.twilight.TwilightService.2
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    Slog.d("TwilightService", "onReceive: " + intent);
                    TwilightService.this.updateTwilightState();
                }
            };
            IntentFilter intentFilter = new IntentFilter("android.intent.action.TIME_SET");
            intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
            getContext().registerReceiver(this.mTimeChangedReceiver, intentFilter);
        }
        updateTwilightState();
    }

    private void stopListening() {
        Slog.d("TwilightService", "stopListening");
        if (this.mTimeChangedReceiver != null) {
            getContext().unregisterReceiver(this.mTimeChangedReceiver);
            this.mTimeChangedReceiver = null;
        }
        if (this.mLastTwilightState != null) {
            this.mAlarmManager.cancel(this);
        }
        this.mLocationManager.removeUpdates(this);
        this.mLastLocation = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTwilightState() {
        long currentTimeMillis = System.currentTimeMillis();
        Location location = this.mLastLocation;
        if (location == null) {
            location = this.mLocationManager.getLastLocation();
        }
        final TwilightState state = calculateTwilightState(location, currentTimeMillis);
        synchronized (this.mListeners) {
            if (!Objects.equals(this.mLastTwilightState, state)) {
                this.mLastTwilightState = state;
                for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                    final TwilightListener listener = this.mListeners.keyAt(i);
                    Handler handler = this.mListeners.valueAt(i);
                    handler.post(new Runnable() { // from class: com.android.server.twilight.TwilightService$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            TwilightListener.this.onTwilightStateChanged(state);
                        }
                    });
                }
            }
        }
        if (state != null) {
            long triggerAtMillis = state.isNight() ? state.sunriseTimeMillis() : state.sunsetTimeMillis();
            this.mAlarmManager.setExact(1, triggerAtMillis, "TwilightService", this, this.mHandler);
        }
    }

    @Override // android.app.AlarmManager.OnAlarmListener
    public void onAlarm() {
        Slog.d("TwilightService", "onAlarm");
        updateTwilightState();
    }

    @Override // android.location.LocationListener
    public void onLocationChanged(Location location) {
        if (location != null) {
            Slog.d("TwilightService", "onLocationChanged: provider=" + location.getProvider() + " accuracy=" + location.getAccuracy() + " time=" + location.getTime());
            this.mLastLocation = location;
            updateTwilightState();
        }
    }

    @Override // android.location.LocationListener
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override // android.location.LocationListener
    public void onProviderEnabled(String provider) {
    }

    @Override // android.location.LocationListener
    public void onProviderDisabled(String provider) {
    }

    private static TwilightState calculateTwilightState(Location location, long timeMillis) {
        if (location == null) {
            return null;
        }
        CalendarAstronomer ca = new CalendarAstronomer(location.getLongitude(), location.getLatitude());
        Calendar noon = Calendar.getInstance();
        noon.setTimeInMillis(timeMillis);
        noon.set(11, 12);
        noon.set(12, 0);
        noon.set(13, 0);
        noon.set(14, 0);
        ca.setTime(noon.getTimeInMillis());
        long sunriseTimeMillis = ca.getSunRiseSet(true);
        long sunsetTimeMillis = ca.getSunRiseSet(false);
        if (sunsetTimeMillis < timeMillis) {
            noon.add(5, 1);
            ca.setTime(noon.getTimeInMillis());
            sunriseTimeMillis = ca.getSunRiseSet(true);
        } else if (sunriseTimeMillis > timeMillis) {
            noon.add(5, -1);
            ca.setTime(noon.getTimeInMillis());
            sunsetTimeMillis = ca.getSunRiseSet(false);
        }
        return new TwilightState(sunriseTimeMillis, sunsetTimeMillis);
    }
}
