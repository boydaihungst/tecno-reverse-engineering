package android.os;

import android.app.IAlarmManager;
import android.location.ILocationManager;
import android.location.LocationTime;
import android.util.Slog;
import android.util.TimeUtils;
import dalvik.annotation.optimization.CriticalNative;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneOffset;
/* loaded from: classes2.dex */
public final class SystemClock {
    private static final String TAG = "SystemClock";
    private static volatile IAlarmManager sIAlarmManager;

    @CriticalNative
    public static native long currentThreadTimeMicro();

    @CriticalNative
    public static native long currentThreadTimeMillis();

    @CriticalNative
    public static native long currentTimeMicro();

    @CriticalNative
    public static native long elapsedRealtime();

    @CriticalNative
    public static native long elapsedRealtimeNanos();

    @CriticalNative
    public static native long uptimeMillis();

    @CriticalNative
    public static native long uptimeNanos();

    private SystemClock() {
    }

    public static void sleep(long ms) {
        long start = uptimeMillis();
        long duration = ms;
        boolean interrupted = false;
        do {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                interrupted = true;
            }
            duration = (start + ms) - uptimeMillis();
        } while (duration > 0);
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean setCurrentTimeMillis(long millis) {
        IAlarmManager mgr = getIAlarmManager();
        if (mgr == null) {
            Slog.e(TAG, "Unable to set RTC: mgr == null");
            return false;
        }
        try {
            return mgr.setTime(millis);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to set RTC", e);
            return false;
        } catch (SecurityException e2) {
            Slog.e(TAG, "Unable to set RTC", e2);
            return false;
        }
    }

    public static Clock uptimeClock() {
        return new SimpleClock(ZoneOffset.UTC) { // from class: android.os.SystemClock.1
            @Override // android.os.SimpleClock, java.time.Clock
            public long millis() {
                return SystemClock.uptimeMillis();
            }
        };
    }

    public static Clock elapsedRealtimeClock() {
        return new SimpleClock(ZoneOffset.UTC) { // from class: android.os.SystemClock.2
            @Override // android.os.SimpleClock, java.time.Clock
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };
    }

    public static long currentNetworkTimeMillis() {
        IAlarmManager mgr = getIAlarmManager();
        if (mgr != null) {
            try {
                return mgr.currentNetworkTimeMillis();
            } catch (ParcelableException e) {
                e.maybeRethrow(DateTimeException.class);
                throw new RuntimeException(e);
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException(new DeadSystemException());
    }

    private static IAlarmManager getIAlarmManager() {
        if (sIAlarmManager == null) {
            sIAlarmManager = IAlarmManager.Stub.asInterface(ServiceManager.getService("alarm"));
        }
        return sIAlarmManager;
    }

    public static Clock currentNetworkTimeClock() {
        return new SimpleClock(ZoneOffset.UTC) { // from class: android.os.SystemClock.3
            @Override // android.os.SimpleClock, java.time.Clock
            public long millis() {
                return SystemClock.currentNetworkTimeMillis();
            }
        };
    }

    public static Clock currentGnssTimeClock() {
        return new SimpleClock(ZoneOffset.UTC) { // from class: android.os.SystemClock.4
            private final ILocationManager mMgr = ILocationManager.Stub.asInterface(ServiceManager.getService("location"));

            @Override // android.os.SimpleClock, java.time.Clock
            public long millis() {
                try {
                    LocationTime time = this.mMgr.getGnssTimeMillis();
                    if (time == null) {
                        throw new DateTimeException("Gnss based time is not available.");
                    }
                    long currentNanos = SystemClock.elapsedRealtimeNanos();
                    long deltaMs = (currentNanos - time.getElapsedRealtimeNanos()) / TimeUtils.NANOS_PER_MS;
                    return time.getTime() + deltaMs;
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        };
    }
}
