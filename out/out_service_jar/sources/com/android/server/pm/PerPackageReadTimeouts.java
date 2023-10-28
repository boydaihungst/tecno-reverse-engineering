package com.android.server.pm;

import android.text.TextUtils;
import com.android.internal.util.HexDump;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class PerPackageReadTimeouts {
    public final String packageName;
    public final byte[] sha256certificate;
    public final Timeouts timeouts;
    public final VersionCodes versionCodes;

    static long tryParseLong(String str, long defaultValue) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static byte[] tryParseSha256(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            return HexDump.hexStringToByteArray(str);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class Timeouts {
        public static final Timeouts DEFAULT = new Timeouts(3600000000L, 3600000000L, 3600000000L);
        public final long maxPendingTimeUs;
        public final long minPendingTimeUs;
        public final long minTimeUs;

        private Timeouts(long minTimeUs, long minPendingTimeUs, long maxPendingTimeUs) {
            this.minTimeUs = minTimeUs;
            this.minPendingTimeUs = minPendingTimeUs;
            this.maxPendingTimeUs = maxPendingTimeUs;
        }

        static Timeouts parse(String timeouts) {
            String[] splits = timeouts.split(":", 3);
            if (splits.length != 3) {
                return DEFAULT;
            }
            String str = splits[0];
            Timeouts timeouts2 = DEFAULT;
            long minTimeUs = PerPackageReadTimeouts.tryParseLong(str, timeouts2.minTimeUs);
            long minPendingTimeUs = PerPackageReadTimeouts.tryParseLong(splits[1], timeouts2.minPendingTimeUs);
            long maxPendingTimeUs = PerPackageReadTimeouts.tryParseLong(splits[2], timeouts2.maxPendingTimeUs);
            if (0 <= minTimeUs && minTimeUs <= minPendingTimeUs && minPendingTimeUs <= maxPendingTimeUs) {
                return new Timeouts(minTimeUs, minPendingTimeUs, maxPendingTimeUs);
            }
            return timeouts2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static class VersionCodes {
        public static final VersionCodes ALL_VERSION_CODES = new VersionCodes(Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME);
        public final long maxVersionCode;
        public final long minVersionCode;

        private VersionCodes(long minVersionCode, long maxVersionCode) {
            this.minVersionCode = minVersionCode;
            this.maxVersionCode = maxVersionCode;
        }

        static VersionCodes parse(String codes) {
            if (TextUtils.isEmpty(codes)) {
                return ALL_VERSION_CODES;
            }
            String[] splits = codes.split("-", 2);
            switch (splits.length) {
                case 1:
                    try {
                        long versionCode = Long.parseLong(splits[0]);
                        return new VersionCodes(versionCode, versionCode);
                    } catch (NumberFormatException e) {
                        return ALL_VERSION_CODES;
                    }
                case 2:
                    String str = splits[0];
                    VersionCodes versionCodes = ALL_VERSION_CODES;
                    long minVersionCode = PerPackageReadTimeouts.tryParseLong(str, versionCodes.minVersionCode);
                    long maxVersionCode = PerPackageReadTimeouts.tryParseLong(splits[1], versionCodes.maxVersionCode);
                    if (minVersionCode <= maxVersionCode) {
                        return new VersionCodes(minVersionCode, maxVersionCode);
                    }
                    break;
            }
            return ALL_VERSION_CODES;
        }
    }

    private PerPackageReadTimeouts(String packageName, byte[] sha256certificate, VersionCodes versionCodes, Timeouts timeouts) {
        this.packageName = packageName;
        this.sha256certificate = sha256certificate;
        this.versionCodes = versionCodes;
        this.timeouts = timeouts;
    }

    static PerPackageReadTimeouts parse(String timeoutsStr, VersionCodes defaultVersionCodes, Timeouts defaultTimeouts) {
        byte[] sha256certificate = null;
        VersionCodes versionCodes = defaultVersionCodes;
        Timeouts timeouts = defaultTimeouts;
        String[] splits = timeoutsStr.split(":", 4);
        switch (splits.length) {
            case 1:
                break;
            case 4:
                timeouts = Timeouts.parse(splits[3]);
            case 3:
                versionCodes = VersionCodes.parse(splits[2]);
            case 2:
                sha256certificate = tryParseSha256(splits[1]);
                break;
            default:
                return null;
        }
        String packageName = splits[0];
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        return new PerPackageReadTimeouts(packageName, sha256certificate, versionCodes, timeouts);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static List<PerPackageReadTimeouts> parseDigestersList(String defaultTimeoutsStr, String knownDigestersList) {
        if (TextUtils.isEmpty(knownDigestersList)) {
            return Collections.emptyList();
        }
        VersionCodes defaultVersionCodes = VersionCodes.ALL_VERSION_CODES;
        Timeouts defaultTimeouts = Timeouts.parse(defaultTimeoutsStr);
        String[] packages = knownDigestersList.split(",");
        List<PerPackageReadTimeouts> result = new ArrayList<>(packages.length);
        for (String str : packages) {
            PerPackageReadTimeouts timeouts = parse(str, defaultVersionCodes, defaultTimeouts);
            if (timeouts != null) {
                result.add(timeouts);
            }
        }
        return result;
    }
}
