package com.android.server.pm.dex;

import android.os.SystemClock;
import android.util.Slog;
import android.util.jar.StrictJarFile;
import com.android.internal.art.ArtStatsLog;
import com.android.server.UiModeManagerService;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
/* loaded from: classes2.dex */
public class ArtStatsLogUtils {
    private static final int ART_COMPILATION_FILTER_FAKE_RUN_FROM_APK_FALLBACK = 14;
    private static final int ART_COMPILATION_FILTER_FAKE_RUN_FROM_VDEX_FALLBACK = 15;
    private static final int ART_COMPILATION_REASON_INSTALL_BULK_DOWNGRADED = 15;
    private static final int ART_COMPILATION_REASON_INSTALL_BULK_SECONDARY = 14;
    private static final int ART_COMPILATION_REASON_INSTALL_BULK_SECONDARY_DOWNGRADED = 16;
    private static final Map<Integer, Integer> COMPILATION_REASON_MAP;
    private static final Map<String, Integer> COMPILE_FILTER_MAP;
    private static final Map<String, Integer> ISA_MAP;
    private static final String PROFILE_DEX_METADATA = "primary.prof";
    private static final Map<Integer, Integer> STATUS_MAP;
    private static final String TAG = ArtStatsLogUtils.class.getSimpleName();
    private static final String VDEX_DEX_METADATA = "primary.vdex";

    static {
        HashMap hashMap = new HashMap();
        COMPILATION_REASON_MAP = hashMap;
        hashMap.put(0, 3);
        hashMap.put(1, 4);
        hashMap.put(2, 11);
        hashMap.put(3, 5);
        hashMap.put(4, 12);
        hashMap.put(5, 13);
        hashMap.put(6, 14);
        hashMap.put(7, 15);
        hashMap.put(8, 16);
        hashMap.put(9, 6);
        hashMap.put(10, 7);
        hashMap.put(11, 8);
        hashMap.put(12, 19);
        hashMap.put(13, 9);
        HashMap hashMap2 = new HashMap();
        COMPILE_FILTER_MAP = hashMap2;
        hashMap2.put("error", 1);
        hashMap2.put(UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN, 2);
        hashMap2.put("assume-verified", 3);
        hashMap2.put("extract", 4);
        hashMap2.put("verify", 5);
        hashMap2.put("quicken", 6);
        hashMap2.put("space-profile", 7);
        hashMap2.put("space", 8);
        hashMap2.put("speed-profile", 9);
        hashMap2.put("speed", 10);
        hashMap2.put("everything-profile", 11);
        hashMap2.put("everything", 12);
        hashMap2.put("run-from-apk", 13);
        hashMap2.put("run-from-apk-fallback", 14);
        hashMap2.put("run-from-vdex-fallback", 15);
        HashMap hashMap3 = new HashMap();
        ISA_MAP = hashMap3;
        hashMap3.put("arm", 1);
        hashMap3.put("arm64", 2);
        hashMap3.put("x86", 3);
        hashMap3.put("x86_64", 4);
        hashMap3.put("mips", 5);
        hashMap3.put("mips64", 6);
        STATUS_MAP = Map.of(0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 1);
    }

    public static void writeStatsLog(ArtStatsLogger logger, long sessionId, String compilerFilter, int uid, long compileTime, String dexMetadataPath, int compilationReason, int result, int apkType, String isa, String apkPath) {
        int dexMetadataType = getDexMetadataType(dexMetadataPath);
        logger.write(sessionId, uid, compilationReason, compilerFilter, 10, result, dexMetadataType, apkType, isa);
        logger.write(sessionId, uid, compilationReason, compilerFilter, 11, getDexBytes(apkPath), dexMetadataType, apkType, isa);
        logger.write(sessionId, uid, compilationReason, compilerFilter, 12, compileTime, dexMetadataType, apkType, isa);
    }

    public static int getApkType(final String path, String baseApkPath, String[] splitApkPaths) {
        if (path.equals(baseApkPath)) {
            return 1;
        }
        if (Arrays.stream(splitApkPaths).anyMatch(new Predicate() { // from class: com.android.server.pm.dex.ArtStatsLogUtils$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean equals;
                equals = ((String) obj).equals(path);
                return equals;
            }
        })) {
            return 2;
        }
        return 0;
    }

    private static long getDexBytes(String apkPath) {
        StrictJarFile jarFile = null;
        long dexBytes = 0;
        try {
            try {
                jarFile = new StrictJarFile(apkPath, false, false);
                Iterator<ZipEntry> it = jarFile.iterator();
                Pattern p = Pattern.compile("classes(\\d)*[.]dex");
                Matcher m = p.matcher("");
                while (it.hasNext()) {
                    ZipEntry entry = it.next();
                    m.reset(entry.getName());
                    if (m.matches()) {
                        dexBytes += entry.getSize();
                    }
                }
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
                return dexBytes;
            } catch (IOException e2) {
                Slog.e(TAG, "Error when parsing APK " + apkPath);
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e3) {
                    }
                }
                return -1L;
            }
        } catch (Throwable th) {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [257=6, 259=6, 260=4] */
    private static int getDexMetadataType(String dexMetadataPath) {
        if (dexMetadataPath == null) {
            return 4;
        }
        StrictJarFile jarFile = null;
        try {
            try {
                jarFile = new StrictJarFile(dexMetadataPath, false, false);
                boolean hasProfile = findFileName(jarFile, PROFILE_DEX_METADATA);
                boolean hasVdex = findFileName(jarFile, VDEX_DEX_METADATA);
                if (hasProfile && hasVdex) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                    }
                    return 3;
                } else if (hasProfile) {
                    try {
                        jarFile.close();
                    } catch (IOException e2) {
                    }
                    return 1;
                } else if (hasVdex) {
                    try {
                        jarFile.close();
                    } catch (IOException e3) {
                    }
                    return 2;
                } else {
                    try {
                        jarFile.close();
                    } catch (IOException e4) {
                    }
                    return 0;
                }
            } catch (Throwable th) {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e5) {
                    }
                }
                throw th;
            }
        } catch (IOException e6) {
            Slog.e(TAG, "Error when parsing dex metadata " + dexMetadataPath);
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e7) {
                }
            }
            return 5;
        }
    }

    private static boolean findFileName(StrictJarFile jarFile, String filename) throws IOException {
        Iterator<ZipEntry> it = jarFile.iterator();
        while (it.hasNext()) {
            ZipEntry entry = it.next();
            if (entry.getName().equals(filename)) {
                return true;
            }
        }
        return false;
    }

    /* loaded from: classes2.dex */
    public static class ArtStatsLogger {
        public void write(long sessionId, int uid, int compilationReason, String compilerFilter, int kind, long value, int dexMetadataType, int apkType, String isa) {
            ArtStatsLog.write(ArtStatsLog.ART_DATUM_REPORTED, sessionId, uid, ((Integer) ArtStatsLogUtils.COMPILE_FILTER_MAP.getOrDefault(compilerFilter, 2)).intValue(), ((Integer) ArtStatsLogUtils.COMPILATION_REASON_MAP.getOrDefault(Integer.valueOf(compilationReason), 2)).intValue(), SystemClock.uptimeMillis(), 1, kind, value, dexMetadataType, apkType, ((Integer) ArtStatsLogUtils.ISA_MAP.getOrDefault(isa, 0)).intValue());
        }
    }

    /* loaded from: classes2.dex */
    public static class BackgroundDexoptJobStatsLogger {
        public void write(int status, int cancellationReason, long durationMs, long durationIncludingSleepMs) {
            ArtStatsLog.write(ArtStatsLog.BACKGROUND_DEXOPT_JOB_ENDED, ((Integer) ArtStatsLogUtils.STATUS_MAP.getOrDefault(Integer.valueOf(status), 0)).intValue(), cancellationReason, durationMs, durationIncludingSleepMs);
        }
    }
}
