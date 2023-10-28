package com.android.server.location.countrydetector;

import android.content.Context;
import android.location.Country;
import android.location.CountryListener;
import android.location.Geocoder;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.location.LocationManagerService;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
/* loaded from: classes.dex */
public class ComprehensiveCountryDetector extends CountryDetectorBase {
    static final boolean DEBUG = LocationManagerService.D;
    private static final long LOCATION_REFRESH_INTERVAL = 86400000;
    private static final int MAX_LENGTH_DEBUG_LOGS = 20;
    private static final String TAG = "CountryDetector";
    private int mCountServiceStateChanges;
    private Country mCountry;
    private Country mCountryFromLocation;
    private final ConcurrentLinkedQueue<Country> mDebugLogs;
    private Country mLastCountryAddedToLogs;
    private CountryListener mLocationBasedCountryDetectionListener;
    protected CountryDetectorBase mLocationBasedCountryDetector;
    protected Timer mLocationRefreshTimer;
    private final Object mObject;
    private PhoneStateListener mPhoneStateListener;
    private long mStartTime;
    private long mStopTime;
    private boolean mStopped;
    private final TelephonyManager mTelephonyManager;
    private int mTotalCountServiceStateChanges;
    private long mTotalTime;

    public ComprehensiveCountryDetector(Context context) {
        super(context);
        this.mStopped = false;
        this.mDebugLogs = new ConcurrentLinkedQueue<>();
        this.mObject = new Object();
        this.mLocationBasedCountryDetectionListener = new CountryListener() { // from class: com.android.server.location.countrydetector.ComprehensiveCountryDetector.1
            public void onCountryDetected(Country country) {
                if (ComprehensiveCountryDetector.DEBUG) {
                    Slog.d(ComprehensiveCountryDetector.TAG, "Country detected via LocationBasedCountryDetector");
                }
                ComprehensiveCountryDetector.this.mCountryFromLocation = country;
                ComprehensiveCountryDetector.this.detectCountry(true, false);
                ComprehensiveCountryDetector.this.stopLocationBasedDetector();
            }
        };
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
    }

    @Override // com.android.server.location.countrydetector.CountryDetectorBase
    public Country detectCountry() {
        return detectCountry(false, !this.mStopped);
    }

    @Override // com.android.server.location.countrydetector.CountryDetectorBase
    public void stop() {
        Slog.i(TAG, "Stop the detector.");
        cancelLocationRefresh();
        removePhoneStateListener();
        stopLocationBasedDetector();
        this.mListener = null;
        this.mStopped = true;
    }

    private Country getCountry() {
        Country result = getNetworkBasedCountry();
        if (result == null) {
            result = getLastKnownLocationBasedCountry();
        }
        if (result == null) {
            result = getSimBasedCountry();
        }
        if (result == null) {
            result = getLocaleCountry();
        }
        addToLogs(result);
        return result;
    }

    private void addToLogs(Country country) {
        if (country == null) {
            return;
        }
        synchronized (this.mObject) {
            Country country2 = this.mLastCountryAddedToLogs;
            if (country2 == null || !country2.equals(country)) {
                this.mLastCountryAddedToLogs = country;
                if (this.mDebugLogs.size() >= 20) {
                    this.mDebugLogs.poll();
                }
                if (DEBUG) {
                    Slog.d(TAG, country.toString());
                }
                this.mDebugLogs.add(country);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isNetworkCountryCodeAvailable() {
        int phoneType = this.mTelephonyManager.getPhoneType();
        if (DEBUG) {
            Slog.v(TAG, "    phonetype=" + phoneType);
        }
        return phoneType == 1;
    }

    protected Country getNetworkBasedCountry() {
        if (isNetworkCountryCodeAvailable()) {
            String countryIso = this.mTelephonyManager.getNetworkCountryIso();
            if (!TextUtils.isEmpty(countryIso)) {
                return new Country(countryIso, 0);
            }
            return null;
        }
        return null;
    }

    protected Country getLastKnownLocationBasedCountry() {
        return this.mCountryFromLocation;
    }

    protected Country getSimBasedCountry() {
        String countryIso = this.mTelephonyManager.getSimCountryIso();
        if (!TextUtils.isEmpty(countryIso)) {
            return new Country(countryIso, 2);
        }
        return null;
    }

    protected Country getLocaleCountry() {
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale != null) {
            return new Country(defaultLocale.getCountry(), 3);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Country detectCountry(boolean notifyChange, boolean startLocationBasedDetection) {
        Country country = getCountry();
        Country country2 = this.mCountry;
        if (country2 != null) {
            country2 = new Country(this.mCountry);
        }
        runAfterDetectionAsync(country2, country, notifyChange, startLocationBasedDetection);
        this.mCountry = country;
        return country;
    }

    protected void runAfterDetectionAsync(final Country country, final Country detectedCountry, final boolean notifyChange, final boolean startLocationBasedDetection) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.location.countrydetector.ComprehensiveCountryDetector.2
            @Override // java.lang.Runnable
            public void run() {
                ComprehensiveCountryDetector.this.runAfterDetection(country, detectedCountry, notifyChange, startLocationBasedDetection);
            }
        });
    }

    @Override // com.android.server.location.countrydetector.CountryDetectorBase
    public void setCountryListener(CountryListener listener) {
        CountryListener prevListener = this.mListener;
        this.mListener = listener;
        if (this.mListener == null) {
            removePhoneStateListener();
            stopLocationBasedDetector();
            cancelLocationRefresh();
            long elapsedRealtime = SystemClock.elapsedRealtime();
            this.mStopTime = elapsedRealtime;
            this.mTotalTime += elapsedRealtime;
        } else if (prevListener == null) {
            addPhoneStateListener();
            detectCountry(false, true);
            this.mStartTime = SystemClock.elapsedRealtime();
            this.mStopTime = 0L;
            this.mCountServiceStateChanges = 0;
        }
    }

    void runAfterDetection(Country country, Country detectedCountry, boolean notifyChange, boolean startLocationBasedDetection) {
        notifyIfCountryChanged(country, detectedCountry);
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, "startLocationBasedDetection=" + startLocationBasedDetection + " detectCountry=" + (detectedCountry == null ? null : "(source: " + detectedCountry.getSource() + ", countryISO: " + detectedCountry.getCountryIso() + ")") + " isAirplaneModeOff()=" + isAirplaneModeOff() + " isWifiOn()=" + isWifiOn() + " mListener=" + this.mListener + " isGeoCoderImplemnted()=" + isGeoCoderImplemented());
        }
        if (startLocationBasedDetection && ((detectedCountry == null || detectedCountry.getSource() > 1) && ((isAirplaneModeOff() || isWifiOn()) && this.mListener != null && isGeoCoderImplemented()))) {
            if (z) {
                Slog.d(TAG, "run startLocationBasedDetector()");
            }
            startLocationBasedDetector(this.mLocationBasedCountryDetectionListener);
        }
        if (detectedCountry == null || detectedCountry.getSource() >= 1) {
            scheduleLocationRefresh();
            return;
        }
        cancelLocationRefresh();
        stopLocationBasedDetector();
    }

    private synchronized void startLocationBasedDetector(CountryListener listener) {
        if (this.mLocationBasedCountryDetector != null) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "starts LocationBasedDetector to detect Country code via Location info (e.g. GPS)");
        }
        CountryDetectorBase createLocationBasedCountryDetector = createLocationBasedCountryDetector();
        this.mLocationBasedCountryDetector = createLocationBasedCountryDetector;
        createLocationBasedCountryDetector.setCountryListener(listener);
        this.mLocationBasedCountryDetector.detectCountry();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void stopLocationBasedDetector() {
        if (DEBUG) {
            Slog.d(TAG, "tries to stop LocationBasedDetector (current detector: " + this.mLocationBasedCountryDetector + ")");
        }
        CountryDetectorBase countryDetectorBase = this.mLocationBasedCountryDetector;
        if (countryDetectorBase != null) {
            countryDetectorBase.stop();
            this.mLocationBasedCountryDetector = null;
        }
    }

    protected CountryDetectorBase createLocationBasedCountryDetector() {
        return new LocationBasedCountryDetector(this.mContext);
    }

    protected boolean isAirplaneModeOff() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 0;
    }

    protected boolean isWifiOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_on", 0) != 0;
    }

    private void notifyIfCountryChanged(Country country, Country detectedCountry) {
        if (detectedCountry != null && this.mListener != null) {
            if (country == null || !country.equals(detectedCountry)) {
                if (DEBUG) {
                    Slog.d(TAG, "" + country + " --> " + detectedCountry);
                }
                notifyListener(detectedCountry);
            }
        }
    }

    private synchronized void scheduleLocationRefresh() {
        if (this.mLocationRefreshTimer != null) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "start periodic location refresh timer. Interval: 86400000");
        }
        Timer timer = new Timer();
        this.mLocationRefreshTimer = timer;
        timer.schedule(new TimerTask() { // from class: com.android.server.location.countrydetector.ComprehensiveCountryDetector.3
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                if (ComprehensiveCountryDetector.DEBUG) {
                    Slog.d(ComprehensiveCountryDetector.TAG, "periodic location refresh event. Starts detecting Country code");
                }
                ComprehensiveCountryDetector.this.mLocationRefreshTimer = null;
                ComprehensiveCountryDetector.this.detectCountry(false, true);
            }
        }, 86400000L);
    }

    private synchronized void cancelLocationRefresh() {
        Timer timer = this.mLocationRefreshTimer;
        if (timer != null) {
            timer.cancel();
            this.mLocationRefreshTimer = null;
        }
    }

    protected synchronized void addPhoneStateListener() {
        if (this.mPhoneStateListener == null) {
            PhoneStateListener phoneStateListener = new PhoneStateListener() { // from class: com.android.server.location.countrydetector.ComprehensiveCountryDetector.4
                @Override // android.telephony.PhoneStateListener
                public void onServiceStateChanged(ServiceState serviceState) {
                    ComprehensiveCountryDetector.this.mCountServiceStateChanges++;
                    ComprehensiveCountryDetector.this.mTotalCountServiceStateChanges++;
                    if (!ComprehensiveCountryDetector.this.isNetworkCountryCodeAvailable()) {
                        return;
                    }
                    if (ComprehensiveCountryDetector.DEBUG) {
                        Slog.d(ComprehensiveCountryDetector.TAG, "onServiceStateChanged: " + serviceState.getState());
                    }
                    ComprehensiveCountryDetector.this.detectCountry(true, true);
                }
            };
            this.mPhoneStateListener = phoneStateListener;
            this.mTelephonyManager.listen(phoneStateListener, 1);
        }
    }

    protected synchronized void removePhoneStateListener() {
        PhoneStateListener phoneStateListener = this.mPhoneStateListener;
        if (phoneStateListener != null) {
            this.mTelephonyManager.listen(phoneStateListener, 0);
            this.mPhoneStateListener = null;
        }
    }

    protected boolean isGeoCoderImplemented() {
        return Geocoder.isPresent();
    }

    public String toString() {
        long currentTime = SystemClock.elapsedRealtime();
        long currentSessionLength = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("ComprehensiveCountryDetector{");
        if (this.mStopTime == 0) {
            currentSessionLength = currentTime - this.mStartTime;
            sb.append("timeRunning=" + currentSessionLength + ", ");
        } else {
            sb.append("lastRunTimeLength=" + (this.mStopTime - this.mStartTime) + ", ");
        }
        sb.append("totalCountServiceStateChanges=" + this.mTotalCountServiceStateChanges + ", ");
        sb.append("currentCountServiceStateChanges=" + this.mCountServiceStateChanges + ", ");
        sb.append("totalTime=" + (this.mTotalTime + currentSessionLength) + ", ");
        sb.append("currentTime=" + currentTime + ", ");
        sb.append("countries=");
        Iterator<Country> it = this.mDebugLogs.iterator();
        while (it.hasNext()) {
            Country country = it.next();
            sb.append("\n   " + country.toString());
        }
        sb.append("}");
        return sb.toString();
    }
}
