package com.android.server.location.provider;

import android.location.Location;
import android.location.LocationResult;
import android.location.provider.ProviderRequest;
import android.os.SystemClock;
import android.util.Log;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.Preconditions;
import com.android.server.DeviceIdleInternal;
import com.android.server.FgThread;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.LocationManagerService;
import com.android.server.location.eventlog.LocationEventLog;
import com.android.server.location.injector.DeviceIdleHelper;
import com.android.server.location.injector.DeviceStationaryHelper;
import com.android.server.location.injector.Injector;
import com.android.server.location.provider.AbstractLocationProvider;
import java.io.FileDescriptor;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public final class StationaryThrottlingLocationProvider extends DelegateLocationProvider implements DeviceIdleHelper.DeviceIdleListener, DeviceIdleInternal.StationaryListener {
    private static final long MAX_STATIONARY_LOCATION_AGE_MS = 30000;
    private static final long MIN_INTERVAL_MS = 1000;
    DeliverLastLocationRunnable mDeliverLastLocationCallback;
    private boolean mDeviceIdle;
    private final DeviceIdleHelper mDeviceIdleHelper;
    private boolean mDeviceStationary;
    private final DeviceStationaryHelper mDeviceStationaryHelper;
    private long mDeviceStationaryRealtimeMs;
    private ProviderRequest mIncomingRequest;
    Location mLastLocation;
    final Object mLock;
    private final String mName;
    private ProviderRequest mOutgoingRequest;
    long mThrottlingIntervalMs;

    @Override // com.android.server.location.provider.DelegateLocationProvider, com.android.server.location.provider.AbstractLocationProvider.Listener
    public /* bridge */ /* synthetic */ void onStateChanged(AbstractLocationProvider.State state, AbstractLocationProvider.State state2) {
        super.onStateChanged(state, state2);
    }

    public StationaryThrottlingLocationProvider(String name, Injector injector, AbstractLocationProvider delegate) {
        super(ConcurrentUtils.DIRECT_EXECUTOR, delegate);
        this.mLock = new Object();
        this.mDeviceIdle = false;
        this.mDeviceStationary = false;
        this.mDeviceStationaryRealtimeMs = Long.MIN_VALUE;
        this.mIncomingRequest = ProviderRequest.EMPTY_REQUEST;
        this.mOutgoingRequest = ProviderRequest.EMPTY_REQUEST;
        this.mThrottlingIntervalMs = JobStatus.NO_LATEST_RUNTIME;
        this.mDeliverLastLocationCallback = null;
        this.mName = name;
        this.mDeviceIdleHelper = injector.getDeviceIdleHelper();
        this.mDeviceStationaryHelper = injector.getDeviceStationaryHelper();
        initializeDelegate();
    }

    @Override // com.android.server.location.provider.DelegateLocationProvider, com.android.server.location.provider.AbstractLocationProvider.Listener
    public void onReportLocation(LocationResult locationResult) {
        super.onReportLocation(locationResult);
        synchronized (this.mLock) {
            this.mLastLocation = locationResult.getLastLocation();
            onThrottlingChangedLocked(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.provider.DelegateLocationProvider, com.android.server.location.provider.AbstractLocationProvider
    public void onStart() {
        this.mDelegate.getController().start();
        synchronized (this.mLock) {
            this.mDeviceIdleHelper.addListener(this);
            this.mDeviceIdle = this.mDeviceIdleHelper.isDeviceIdle();
            this.mDeviceStationaryHelper.addListener(this);
            this.mDeviceStationary = false;
            this.mDeviceStationaryRealtimeMs = Long.MIN_VALUE;
            onThrottlingChangedLocked(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.location.provider.DelegateLocationProvider, com.android.server.location.provider.AbstractLocationProvider
    public void onStop() {
        synchronized (this.mLock) {
            this.mDeviceStationaryHelper.removeListener(this);
            this.mDeviceIdleHelper.removeListener(this);
            this.mIncomingRequest = ProviderRequest.EMPTY_REQUEST;
            this.mOutgoingRequest = ProviderRequest.EMPTY_REQUEST;
            this.mThrottlingIntervalMs = JobStatus.NO_LATEST_RUNTIME;
            if (this.mDeliverLastLocationCallback != null) {
                FgThread.getHandler().removeCallbacks(this.mDeliverLastLocationCallback);
                this.mDeliverLastLocationCallback = null;
            }
            this.mLastLocation = null;
        }
        this.mDelegate.getController().stop();
    }

    @Override // com.android.server.location.provider.DelegateLocationProvider, com.android.server.location.provider.AbstractLocationProvider
    protected void onSetRequest(ProviderRequest request) {
        synchronized (this.mLock) {
            this.mIncomingRequest = request;
            onThrottlingChangedLocked(true);
        }
    }

    @Override // com.android.server.location.injector.DeviceIdleHelper.DeviceIdleListener
    public void onDeviceIdleChanged(boolean deviceIdle) {
        synchronized (this.mLock) {
            if (deviceIdle == this.mDeviceIdle) {
                return;
            }
            this.mDeviceIdle = deviceIdle;
            onThrottlingChangedLocked(false);
        }
    }

    public void onDeviceStationaryChanged(boolean deviceStationary) {
        synchronized (this.mLock) {
            if (this.mDeviceStationary == deviceStationary) {
                return;
            }
            this.mDeviceStationary = deviceStationary;
            if (deviceStationary) {
                this.mDeviceStationaryRealtimeMs = SystemClock.elapsedRealtime();
            } else {
                this.mDeviceStationaryRealtimeMs = Long.MIN_VALUE;
            }
            onThrottlingChangedLocked(false);
        }
    }

    private void onThrottlingChangedLocked(boolean deliverImmediate) {
        ProviderRequest newRequest;
        Location location;
        long throttlingIntervalMs = JobStatus.NO_LATEST_RUNTIME;
        if (this.mDeviceStationary && this.mDeviceIdle && !this.mIncomingRequest.isLocationSettingsIgnored() && (location = this.mLastLocation) != null && location.getElapsedRealtimeAgeMillis(this.mDeviceStationaryRealtimeMs) <= 30000) {
            throttlingIntervalMs = Math.max(this.mIncomingRequest.getIntervalMillis(), 1000L);
        }
        if (throttlingIntervalMs != JobStatus.NO_LATEST_RUNTIME) {
            newRequest = ProviderRequest.EMPTY_REQUEST;
        } else {
            newRequest = this.mIncomingRequest;
        }
        if (!newRequest.equals(this.mOutgoingRequest)) {
            this.mOutgoingRequest = newRequest;
            this.mDelegate.getController().setRequest(this.mOutgoingRequest);
        }
        if (throttlingIntervalMs == this.mThrottlingIntervalMs) {
            return;
        }
        long oldThrottlingIntervalMs = this.mThrottlingIntervalMs;
        this.mThrottlingIntervalMs = throttlingIntervalMs;
        if (throttlingIntervalMs != JobStatus.NO_LATEST_RUNTIME) {
            if (oldThrottlingIntervalMs == JobStatus.NO_LATEST_RUNTIME) {
                Log.d(LocationManagerService.TAG, this.mName + " provider stationary throttled");
                LocationEventLog.EVENT_LOG.logProviderStationaryThrottled(this.mName, true, this.mOutgoingRequest);
            }
            if (this.mDeliverLastLocationCallback != null) {
                FgThread.getHandler().removeCallbacks(this.mDeliverLastLocationCallback);
            }
            this.mDeliverLastLocationCallback = new DeliverLastLocationRunnable();
            Preconditions.checkState(this.mLastLocation != null);
            if (deliverImmediate) {
                FgThread.getHandler().post(this.mDeliverLastLocationCallback);
                return;
            }
            long delayMs = this.mThrottlingIntervalMs - this.mLastLocation.getElapsedRealtimeAgeMillis();
            FgThread.getHandler().postDelayed(this.mDeliverLastLocationCallback, delayMs);
            return;
        }
        if (oldThrottlingIntervalMs != JobStatus.NO_LATEST_RUNTIME) {
            LocationEventLog.EVENT_LOG.logProviderStationaryThrottled(this.mName, false, this.mOutgoingRequest);
            Log.d(LocationManagerService.TAG, this.mName + " provider stationary unthrottled");
        }
        FgThread.getHandler().removeCallbacks(this.mDeliverLastLocationCallback);
        this.mDeliverLastLocationCallback = null;
    }

    @Override // com.android.server.location.provider.DelegateLocationProvider, com.android.server.location.provider.AbstractLocationProvider
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mThrottlingIntervalMs != JobStatus.NO_LATEST_RUNTIME) {
            pw.println("stationary throttled=" + this.mLastLocation);
        } else {
            pw.print("stationary throttled=false");
            if (!this.mDeviceIdle) {
                pw.print(" (not idle)");
            }
            if (!this.mDeviceStationary) {
                pw.print(" (not stationary)");
            }
            pw.println();
        }
        this.mDelegate.dump(fd, pw, args);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DeliverLastLocationRunnable implements Runnable {
        DeliverLastLocationRunnable() {
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (StationaryThrottlingLocationProvider.this.mLock) {
                if (StationaryThrottlingLocationProvider.this.mDeliverLastLocationCallback != this) {
                    return;
                }
                if (StationaryThrottlingLocationProvider.this.mLastLocation == null) {
                    return;
                }
                Location location = new Location(StationaryThrottlingLocationProvider.this.mLastLocation);
                location.setTime(System.currentTimeMillis());
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                if (location.hasSpeed()) {
                    location.removeSpeed();
                    if (location.hasSpeedAccuracy()) {
                        location.removeSpeedAccuracy();
                    }
                }
                if (location.hasBearing()) {
                    location.removeBearing();
                    if (location.hasBearingAccuracy()) {
                        location.removeBearingAccuracy();
                    }
                }
                StationaryThrottlingLocationProvider.this.mLastLocation = location;
                FgThread.getHandler().postDelayed(this, StationaryThrottlingLocationProvider.this.mThrottlingIntervalMs);
                StationaryThrottlingLocationProvider.this.reportLocation(LocationResult.wrap(new Location[]{location}));
            }
        }
    }
}
