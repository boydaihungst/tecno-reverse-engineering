package com.mediatek.location;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.mediatek.boostfwk.policy.refreshrate.RefreshRateInfo;
import com.mediatek.cta.CtaManager;
import com.mediatek.cta.CtaManagerFactory;
import com.mediatek.internal.R;
import java.util.Calendar;
/* loaded from: classes.dex */
public class MtkLocationExt {
    private static final boolean DEBUG = true;
    private static final String TAG = "MtkLocationExt";

    /* loaded from: classes.dex */
    public static class GnssLocationProvider {
        private static final int EVENT_GPS_TIME_SYNC_CHANGED = 4;
        private static final int EVENT_SEND_BLUESKY_BROADCAST = 5;
        private final Context mContext;
        private Handler mGpsHandler;
        private GpsTimeSyncObserver mGpsTimeSyncObserver;
        private Thread mGpsTimerThread;
        private final Handler mHandler;
        private Location mLastLocation;
        private LocationManager mLocationManager;
        private boolean mIsGpsTimeSyncRunning = false;
        private LocationListener mPassiveLocationListener = new LocationListener() { // from class: com.mediatek.location.MtkLocationExt.GnssLocationProvider.2
            @Override // android.location.LocationListener
            public void onLocationChanged(Location location) {
                if ("gps".equals(location.getProvider())) {
                    boolean hasLatLong = (location.getLatitude() == 0.0d || location.getLongitude() == 0.0d) ? false : MtkLocationExt.DEBUG;
                    GnssLocationProvider.this.doSystemTimeSyncByGps(hasLatLong, location.getTime());
                }
            }

            @Override // android.location.LocationListener
            public void onProviderDisabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onProviderEnabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };
        private Handler mGpsToastHandler = new Handler() { // from class: com.mediatek.location.MtkLocationExt.GnssLocationProvider.4
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                String timeoutMsg = (String) msg.obj;
                Toast.makeText(GnssLocationProvider.this.mContext, timeoutMsg, 1).show();
            }
        };
        private LocationListener mLocationListener = new LocationListener() { // from class: com.mediatek.location.MtkLocationExt.GnssLocationProvider.5
            @Override // android.location.LocationListener
            public void onLocationChanged(Location location) {
                GnssLocationProvider.this.mGpsTimerThread.interrupt();
            }

            @Override // android.location.LocationListener
            public void onProviderDisabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onProviderEnabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        };

        public GnssLocationProvider(Context context, Handler handler) {
            Log.d(MtkLocationExt.TAG, "MtkLocationExt GnssLocationProvider()");
            this.mContext = context;
            this.mHandler = handler;
            registerIntentReceiver();
            Log.d(MtkLocationExt.TAG, "add GPS time sync handler and looper");
            this.mGpsHandler = new MyHandler(handler.getLooper());
            this.mLocationManager = (LocationManager) context.getSystemService("location");
            GpsTimeSyncObserver gpsTimeSyncObserver = new GpsTimeSyncObserver(this.mGpsHandler, 4);
            this.mGpsTimeSyncObserver = gpsTimeSyncObserver;
            gpsTimeSyncObserver.observe(context);
        }

        private void registerIntentReceiver() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
            this.mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.mediatek.location.MtkLocationExt.GnssLocationProvider.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                        boolean gpsTimeSyncStatus = GnssLocationProvider.this.getGpsTimeSyncState();
                        Log.d(MtkLocationExt.TAG, "BOOT_COMPLETED, GPS Time sync is set to " + gpsTimeSyncStatus);
                        GnssLocationProvider.this.setGpsTimeSyncFlag(gpsTimeSyncStatus);
                        GnssLocationProvider.this.mGpsHandler.obtainMessage(5).sendToTarget();
                    }
                }
            }, UserHandle.ALL, intentFilter, null, this.mHandler);
        }

        /* loaded from: classes.dex */
        private class MyHandler extends Handler {
            public MyHandler(Looper l) {
                super(l);
            }

            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 4:
                        boolean gpsTimeSyncStatus = GnssLocationProvider.this.getGpsTimeSyncState();
                        Log.d(MtkLocationExt.TAG, "GPS Time sync is changed to " + gpsTimeSyncStatus);
                        GnssLocationProvider.this.onGpsTimeChanged(gpsTimeSyncStatus);
                        return;
                    case 5:
                        BlueskyUtility.sendBlueskyBroadcast(GnssLocationProvider.this.mContext);
                        Log.d(MtkLocationExt.TAG, "Finish Bluesky broadcast");
                        return;
                    default:
                        return;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean getGpsTimeSyncState() {
            try {
                if (Settings.Global.getInt(this.mContext.getContentResolver(), "auto_time_gps") > 0) {
                    return MtkLocationExt.DEBUG;
                }
                return false;
            } catch (Settings.SettingNotFoundException e) {
                return false;
            }
        }

        /* loaded from: classes.dex */
        private static class GpsTimeSyncObserver extends ContentObserver {
            private Handler mHandler;
            private int mMsg;

            GpsTimeSyncObserver(Handler handler, int msg) {
                super(handler);
                this.mHandler = handler;
                this.mMsg = msg;
            }

            void observe(Context context) {
                ContentResolver resolver = context.getContentResolver();
                resolver.registerContentObserver(Settings.Global.getUriFor("auto_time_gps"), false, this);
            }

            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                this.mHandler.obtainMessage(this.mMsg).sendToTarget();
            }
        }

        public void onGpsTimeChanged(boolean enable) {
            if (enable) {
                startUsingGpsWithTimeout(180000, this.mContext.getString(R.string.gps_time_sync_fail_str));
            } else {
                Thread thread = this.mGpsTimerThread;
                if (thread != null) {
                    thread.interrupt();
                }
            }
            setGpsTimeSyncFlag(enable);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setGpsTimeSyncFlag(boolean flag) {
            Log.d(MtkLocationExt.TAG, "setGpsTimeSyncFlag: " + flag);
            if (flag) {
                this.mLocationManager.requestLocationUpdates("passive", 0L, RefreshRateInfo.DECELERATION_RATE, this.mPassiveLocationListener);
            } else {
                this.mLocationManager.removeUpdates(this.mPassiveLocationListener);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void doSystemTimeSyncByGps(boolean hasLatLong, long timestamp) {
            if (hasLatLong) {
                Log.d(MtkLocationExt.TAG, " ########## Auto-sync time with GPS: timestamp = " + timestamp + " ########## ");
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(timestamp);
                long when = c.getTimeInMillis();
                if (when / 1000 < 2147483647L) {
                    SystemClock.setCurrentTimeMillis(when);
                }
                this.mLocationManager.removeUpdates(this.mPassiveLocationListener);
            }
        }

        public void startUsingGpsWithTimeout(final int milliseconds, final String timeoutMsg) {
            if (this.mIsGpsTimeSyncRunning) {
                Log.d(MtkLocationExt.TAG, "WARNING: Gps Time Sync is already run");
                return;
            }
            this.mIsGpsTimeSyncRunning = MtkLocationExt.DEBUG;
            Log.d(MtkLocationExt.TAG, "start using GPS for GPS time sync timeout=" + milliseconds + " timeoutMsg=" + timeoutMsg);
            this.mLocationManager.requestLocationUpdates("gps", 1000L, RefreshRateInfo.DECELERATION_RATE, this.mLocationListener);
            Thread thread = new Thread() { // from class: com.mediatek.location.MtkLocationExt.GnssLocationProvider.3
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    boolean isTimeout = false;
                    try {
                        Thread.sleep(milliseconds);
                        isTimeout = MtkLocationExt.DEBUG;
                    } catch (InterruptedException e) {
                    }
                    Log.d(MtkLocationExt.TAG, "isTimeout=" + isTimeout);
                    if (isTimeout) {
                        Message m = new Message();
                        m.obj = timeoutMsg;
                        GnssLocationProvider.this.mGpsToastHandler.sendMessage(m);
                    }
                    GnssLocationProvider.this.mLocationManager.removeUpdates(GnssLocationProvider.this.mLocationListener);
                    GnssLocationProvider.this.mIsGpsTimeSyncRunning = false;
                }
            };
            this.mGpsTimerThread = thread;
            thread.start();
        }
    }

    /* loaded from: classes.dex */
    public static class LocationManagerService {
        private final Context mContext;
        private CtaManager mCtaManager;
        private final Handler mHandler;
        private LocationManager mLocationManager;

        public LocationManagerService(Context context, Handler handler) {
            Log.d(MtkLocationExt.TAG, "MtkLocationExt LocationManagerService()");
            this.mContext = context;
            this.mHandler = handler;
            this.mCtaManager = CtaManagerFactory.getInstance().makeCtaManager();
            this.mLocationManager = (LocationManager) context.getSystemService("location");
        }

        public boolean isCtaFeatureSupport() {
            return this.mCtaManager.isCtaSupported();
        }

        public void printCtaLog(int callingPid, int callingUid, String functionName, String strActionType, String parameter) {
            CtaManager.ActionType actionType = CtaManager.ActionType.USE_LOCATION;
            if ("USE_LOCATION".equals(strActionType)) {
                actionType = CtaManager.ActionType.USE_LOCATION;
            } else if ("READ_LOCATION_INFO".equals(strActionType)) {
                actionType = CtaManager.ActionType.READ_LOCATION_INFO;
            }
            this.mCtaManager.printCtaInfor(callingPid, callingUid, CtaManager.KeywordType.LOCATION, functionName, actionType, parameter);
        }

        public void showNlpNotInstalledToast(String provider) {
            try {
                Log.d(MtkLocationExt.TAG, "showNlpNotInstalledToast provider: " + provider);
                if ("network".equals(provider) && !Build.IS_DEBUG_ENABLE) {
                    Toast.makeText(this.mContext, "No Network Location Provider is installed!NLP is necessary for network location fixes.", 1).show();
                }
            } catch (Exception e) {
                Log.w(MtkLocationExt.TAG, "Failed to show toast ", e);
            }
        }
    }
}
