package com.android.server.location.injector;

import android.location.Geofence;
import android.location.LocationRequest;
import android.util.Log;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.LocationManagerService;
import java.time.Instant;
/* loaded from: classes.dex */
public class LocationUsageLogger {
    private static final int API_USAGE_LOG_HOURLY_CAP = 60;
    private static final int ONE_HOUR_IN_MILLIS = 3600000;
    private static final int ONE_MINUTE_IN_MILLIS = 60000;
    private static final int ONE_SEC_IN_MILLIS = 1000;
    private long mLastApiUsageLogHour = 0;
    private int mApiUsageLogHourlyCount = 0;

    /* JADX WARN: Removed duplicated region for block: B:38:0x006e  */
    /* JADX WARN: Removed duplicated region for block: B:39:0x0071 A[Catch: Exception -> 0x008c, TryCatch #0 {Exception -> 0x008c, blocks: (B:2:0x0000, B:36:0x0062, B:40:0x007b, B:39:0x0071, B:33:0x0055, B:28:0x0048, B:24:0x0038, B:21:0x002b, B:18:0x0022, B:15:0x0019), top: B:45:0x0000 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void logLocationApiUsage(int usageType, int apiInUse, String packageName, String attributionTag, String provider, LocationRequest locationRequest, boolean hasListener, boolean hasIntent, Geofence geofence, boolean foreground) {
        int bucketizeProvider;
        int quality;
        int bucketizeInterval;
        int bucketizeDistance;
        int i;
        int bucketizeRadius;
        try {
            if (hitApiUsageLogCap()) {
                return;
            }
            boolean isLocationRequestNull = locationRequest == null;
            boolean isGeofenceNull = geofence == null;
            if (isLocationRequestNull) {
                bucketizeProvider = 0;
            } else {
                bucketizeProvider = bucketizeProvider(provider);
            }
            if (isLocationRequestNull) {
                quality = 0;
            } else {
                quality = locationRequest.getQuality();
            }
            if (isLocationRequestNull) {
                bucketizeInterval = 0;
            } else {
                bucketizeInterval = bucketizeInterval(locationRequest.getIntervalMillis());
            }
            if (isLocationRequestNull) {
                bucketizeDistance = 0;
            } else {
                bucketizeDistance = bucketizeDistance(locationRequest.getMinUpdateDistanceMeters());
            }
            long maxUpdates = isLocationRequestNull ? 0L : locationRequest.getMaxUpdates();
            if (!isLocationRequestNull && usageType != 1) {
                i = bucketizeExpireIn(locationRequest.getDurationMillis());
                int callbackType = getCallbackType(apiInUse, hasListener, hasIntent);
                if (!isGeofenceNull) {
                    bucketizeRadius = 0;
                } else {
                    bucketizeRadius = bucketizeRadius(geofence.getRadius());
                }
                FrameworkStatsLog.write(210, usageType, apiInUse, packageName, bucketizeProvider, quality, bucketizeInterval, bucketizeDistance, maxUpdates, i, callbackType, bucketizeRadius, categorizeActivityImportance(foreground), attributionTag);
            }
            i = 0;
            int callbackType2 = getCallbackType(apiInUse, hasListener, hasIntent);
            if (!isGeofenceNull) {
            }
            FrameworkStatsLog.write(210, usageType, apiInUse, packageName, bucketizeProvider, quality, bucketizeInterval, bucketizeDistance, maxUpdates, i, callbackType2, bucketizeRadius, categorizeActivityImportance(foreground), attributionTag);
        } catch (Exception e) {
            Log.w(LocationManagerService.TAG, "Failed to log API usage to statsd.", e);
        }
    }

    public void logLocationApiUsage(int usageType, int apiInUse, String providerName) {
        try {
            if (hitApiUsageLogCap()) {
                return;
            }
            FrameworkStatsLog.write(210, usageType, apiInUse, (String) null, bucketizeProvider(providerName), 0, 0, 0, 0L, 0, getCallbackType(apiInUse, true, true), 0, 0, (String) null);
        } catch (Exception e) {
            Log.w(LocationManagerService.TAG, "Failed to log API usage to statsd.", e);
        }
    }

    private static int bucketizeProvider(String provider) {
        if ("network".equals(provider)) {
            return 1;
        }
        if ("gps".equals(provider)) {
            return 2;
        }
        if ("passive".equals(provider)) {
            return 3;
        }
        if ("fused".equals(provider)) {
            return 4;
        }
        return 0;
    }

    private static int bucketizeInterval(long interval) {
        if (interval < 1000) {
            return 1;
        }
        if (interval < 5000) {
            return 2;
        }
        if (interval < 60000) {
            return 3;
        }
        if (interval < 600000) {
            return 4;
        }
        if (interval < 3600000) {
            return 5;
        }
        return 6;
    }

    private static int bucketizeDistance(float smallestDisplacement) {
        if (smallestDisplacement <= 0.0f) {
            return 1;
        }
        if (smallestDisplacement > 0.0f && smallestDisplacement <= 100.0f) {
            return 2;
        }
        return 3;
    }

    private static int bucketizeRadius(float radius) {
        if (radius < 0.0f) {
            return 7;
        }
        if (radius < 100.0f) {
            return 1;
        }
        if (radius < 200.0f) {
            return 2;
        }
        if (radius < 300.0f) {
            return 3;
        }
        if (radius < 1000.0f) {
            return 4;
        }
        if (radius < 10000.0f) {
            return 5;
        }
        return 6;
    }

    private static int bucketizeExpireIn(long expireIn) {
        if (expireIn == JobStatus.NO_LATEST_RUNTIME) {
            return 6;
        }
        if (expireIn < 20000) {
            return 1;
        }
        if (expireIn < 60000) {
            return 2;
        }
        if (expireIn < 600000) {
            return 3;
        }
        if (expireIn < 3600000) {
            return 4;
        }
        return 5;
    }

    private static int categorizeActivityImportance(boolean foreground) {
        if (foreground) {
            return 1;
        }
        return 3;
    }

    private static int getCallbackType(int apiType, boolean hasListener, boolean hasIntent) {
        if (apiType == 5) {
            return 1;
        }
        if (hasIntent) {
            return 3;
        }
        if (hasListener) {
            return 2;
        }
        return 0;
    }

    private synchronized boolean hitApiUsageLogCap() {
        long currentHour = Instant.now().toEpochMilli() / 3600000;
        if (currentHour > this.mLastApiUsageLogHour) {
            this.mLastApiUsageLogHour = currentHour;
            this.mApiUsageLogHourlyCount = 0;
            return false;
        }
        int min = Math.min(this.mApiUsageLogHourlyCount + 1, 60);
        this.mApiUsageLogHourlyCount = min;
        return min >= 60;
    }
}
