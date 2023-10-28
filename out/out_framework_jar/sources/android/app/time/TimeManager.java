package android.app.time;

import android.annotation.SystemApi;
import android.app.time.ITimeZoneDetectorListener;
import android.app.time.TimeManager;
import android.app.timedetector.ITimeDetectorService;
import android.app.timezonedetector.ITimeZoneDetectorService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import java.util.Objects;
import java.util.concurrent.Executor;
@SystemApi
/* loaded from: classes.dex */
public final class TimeManager {
    private static final boolean DEBUG = false;
    private static final String TAG = "time.TimeManager";
    private ArrayMap<TimeZoneDetectorListener, TimeZoneDetectorListener> mTimeZoneDetectorListeners;
    private ITimeZoneDetectorListener mTimeZoneDetectorReceiver;
    private final Object mLock = new Object();
    private final ITimeZoneDetectorService mITimeZoneDetectorService = ITimeZoneDetectorService.Stub.asInterface(ServiceManager.getServiceOrThrow("time_zone_detector"));
    private final ITimeDetectorService mITimeDetectorService = ITimeDetectorService.Stub.asInterface(ServiceManager.getServiceOrThrow("time_detector"));

    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface TimeZoneDetectorListener {
        void onChange();
    }

    public TimeZoneCapabilitiesAndConfig getTimeZoneCapabilitiesAndConfig() {
        try {
            return this.mITimeZoneDetectorService.getCapabilitiesAndConfig();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public TimeCapabilitiesAndConfig getTimeCapabilitiesAndConfig() {
        try {
            return this.mITimeDetectorService.getCapabilitiesAndConfig();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean updateTimeConfiguration(TimeConfiguration configuration) {
        try {
            return this.mITimeDetectorService.updateConfiguration(configuration);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean updateTimeZoneConfiguration(TimeZoneConfiguration configuration) {
        try {
            return this.mITimeZoneDetectorService.updateConfiguration(configuration);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addTimeZoneDetectorListener(final Executor executor, final TimeZoneDetectorListener listener) {
        synchronized (this.mLock) {
            ArrayMap<TimeZoneDetectorListener, TimeZoneDetectorListener> arrayMap = this.mTimeZoneDetectorListeners;
            if (arrayMap == null) {
                this.mTimeZoneDetectorListeners = new ArrayMap<>();
            } else if (arrayMap.containsKey(listener)) {
                return;
            }
            if (this.mTimeZoneDetectorReceiver == null) {
                ITimeZoneDetectorListener iListener = new ITimeZoneDetectorListener.Stub() { // from class: android.app.time.TimeManager.1
                    @Override // android.app.time.ITimeZoneDetectorListener
                    public void onChange() {
                        TimeManager.this.notifyTimeZoneDetectorListeners();
                    }
                };
                this.mTimeZoneDetectorReceiver = iListener;
                try {
                    this.mITimeZoneDetectorService.addListener(iListener);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            this.mTimeZoneDetectorListeners.put(listener, new TimeZoneDetectorListener() { // from class: android.app.time.TimeManager$$ExternalSyntheticLambda0
                @Override // android.app.time.TimeManager.TimeZoneDetectorListener
                public final void onChange() {
                    TimeManager.lambda$addTimeZoneDetectorListener$0(executor, listener);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$addTimeZoneDetectorListener$0(Executor executor, final TimeZoneDetectorListener listener) {
        Objects.requireNonNull(listener);
        executor.execute(new Runnable() { // from class: android.app.time.TimeManager$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                TimeManager.TimeZoneDetectorListener.this.onChange();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyTimeZoneDetectorListeners() {
        synchronized (this.mLock) {
            ArrayMap<TimeZoneDetectorListener, TimeZoneDetectorListener> arrayMap = this.mTimeZoneDetectorListeners;
            if (arrayMap != null && !arrayMap.isEmpty()) {
                ArrayMap<TimeZoneDetectorListener, TimeZoneDetectorListener> timeZoneDetectorListeners = new ArrayMap<>(this.mTimeZoneDetectorListeners);
                int size = timeZoneDetectorListeners.size();
                for (int i = 0; i < size; i++) {
                    timeZoneDetectorListeners.valueAt(i).onChange();
                }
            }
        }
    }

    public void removeTimeZoneDetectorListener(TimeZoneDetectorListener listener) {
        synchronized (this.mLock) {
            ArrayMap<TimeZoneDetectorListener, TimeZoneDetectorListener> arrayMap = this.mTimeZoneDetectorListeners;
            if (arrayMap != null && !arrayMap.isEmpty()) {
                this.mTimeZoneDetectorListeners.remove(listener);
                if (this.mTimeZoneDetectorListeners.isEmpty()) {
                    try {
                        this.mITimeZoneDetectorService.removeListener(this.mTimeZoneDetectorReceiver);
                        this.mTimeZoneDetectorReceiver = null;
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
            }
        }
    }

    public void suggestExternalTime(ExternalTimeSuggestion timeSuggestion) {
        try {
            this.mITimeDetectorService.suggestExternalTime(timeSuggestion);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
