package com.android.server.companion;

import android.util.ArrayMap;
import com.android.internal.util.FrameworkStatsLog;
import java.util.Collections;
import java.util.Map;
/* loaded from: classes.dex */
final class MetricUtils {
    private static final Map<String, Integer> METRIC_DEVICE_PROFILE;

    MetricUtils() {
    }

    static {
        Map<String, Integer> map = new ArrayMap<>();
        map.put(null, 0);
        map.put("android.app.role.COMPANION_DEVICE_WATCH", 1);
        map.put("android.app.role.COMPANION_DEVICE_APP_STREAMING", 2);
        map.put("android.app.role.SYSTEM_AUTOMOTIVE_PROJECTION", 3);
        map.put("android.app.role.COMPANION_DEVICE_COMPUTER", 4);
        METRIC_DEVICE_PROFILE = Collections.unmodifiableMap(map);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void logCreateAssociation(String profile) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.CDM_ASSOCIATION_ACTION, 1, METRIC_DEVICE_PROFILE.get(profile).intValue());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void logRemoveAssociation(String profile) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.CDM_ASSOCIATION_ACTION, 2, METRIC_DEVICE_PROFILE.get(profile).intValue());
    }
}
