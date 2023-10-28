package com.android.server.storage;

import android.app.usage.ExternalStorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.ArrayMap;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
/* loaded from: classes2.dex */
public class FileCollector {
    private static final int AUDIO = 2;
    private static final Map<String, Integer> EXTENSION_MAP;
    private static final int IMAGES = 0;
    private static final int UNRECOGNIZED = -1;
    private static final int VIDEO = 1;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    private @interface FileTypes {
    }

    static {
        ArrayMap arrayMap = new ArrayMap();
        EXTENSION_MAP = arrayMap;
        arrayMap.put("aac", 2);
        arrayMap.put("amr", 2);
        arrayMap.put("awb", 2);
        arrayMap.put("snd", 2);
        arrayMap.put("flac", 2);
        arrayMap.put("mp3", 2);
        arrayMap.put("mpga", 2);
        arrayMap.put("mpega", 2);
        arrayMap.put("mp2", 2);
        arrayMap.put("m4a", 2);
        arrayMap.put("aif", 2);
        arrayMap.put("aiff", 2);
        arrayMap.put("aifc", 2);
        arrayMap.put("gsm", 2);
        arrayMap.put("mka", 2);
        arrayMap.put("m3u", 2);
        arrayMap.put("wma", 2);
        arrayMap.put("wax", 2);
        arrayMap.put("ra", 2);
        arrayMap.put("rm", 2);
        arrayMap.put("ram", 2);
        arrayMap.put("pls", 2);
        arrayMap.put("sd2", 2);
        arrayMap.put("wav", 2);
        arrayMap.put("ogg", 2);
        arrayMap.put("oga", 2);
        arrayMap.put("3gpp", 1);
        arrayMap.put("3gp", 1);
        arrayMap.put("3gpp2", 1);
        arrayMap.put("3g2", 1);
        arrayMap.put("avi", 1);
        arrayMap.put("dl", 1);
        arrayMap.put("dif", 1);
        arrayMap.put("dv", 1);
        arrayMap.put("fli", 1);
        arrayMap.put("m4v", 1);
        arrayMap.put("ts", 1);
        arrayMap.put("mpeg", 1);
        arrayMap.put("mpg", 1);
        arrayMap.put("mpe", 1);
        arrayMap.put("mp4", 1);
        arrayMap.put("vob", 1);
        arrayMap.put("qt", 1);
        arrayMap.put("mov", 1);
        arrayMap.put("mxu", 1);
        arrayMap.put("webm", 1);
        arrayMap.put("lsf", 1);
        arrayMap.put("lsx", 1);
        arrayMap.put("mkv", 1);
        arrayMap.put("mng", 1);
        arrayMap.put("asf", 1);
        arrayMap.put("asx", 1);
        arrayMap.put("wm", 1);
        arrayMap.put("wmv", 1);
        arrayMap.put("wmx", 1);
        arrayMap.put("wvx", 1);
        arrayMap.put("movie", 1);
        arrayMap.put("wrf", 1);
        arrayMap.put("bmp", 0);
        arrayMap.put("gif", 0);
        arrayMap.put("jpg", 0);
        arrayMap.put("jpeg", 0);
        arrayMap.put("jpe", 0);
        arrayMap.put("pcx", 0);
        arrayMap.put("png", 0);
        arrayMap.put("svg", 0);
        arrayMap.put("svgz", 0);
        arrayMap.put("tiff", 0);
        arrayMap.put("tif", 0);
        arrayMap.put("wbmp", 0);
        arrayMap.put("webp", 0);
        arrayMap.put("dng", 0);
        arrayMap.put("cr2", 0);
        arrayMap.put("ras", 0);
        arrayMap.put("art", 0);
        arrayMap.put("jng", 0);
        arrayMap.put("nef", 0);
        arrayMap.put("nrw", 0);
        arrayMap.put("orf", 0);
        arrayMap.put("rw2", 0);
        arrayMap.put("pef", 0);
        arrayMap.put("psd", 0);
        arrayMap.put("pnm", 0);
        arrayMap.put("pbm", 0);
        arrayMap.put("pgm", 0);
        arrayMap.put("ppm", 0);
        arrayMap.put("srw", 0);
        arrayMap.put("arw", 0);
        arrayMap.put("rgb", 0);
        arrayMap.put("xbm", 0);
        arrayMap.put("xpm", 0);
        arrayMap.put("xwd", 0);
    }

    public static MeasurementResult getMeasurementResult(File path) {
        return collectFiles(StorageManager.maybeTranslateEmulatedPathToInternal(path), new MeasurementResult());
    }

    public static MeasurementResult getMeasurementResult(Context context) {
        MeasurementResult result = new MeasurementResult();
        StorageStatsManager ssm = (StorageStatsManager) context.getSystemService("storagestats");
        try {
            ExternalStorageStats stats = ssm.queryExternalStatsForUser(StorageManager.UUID_PRIVATE_INTERNAL, UserHandle.of(context.getUserId()));
            result.imagesSize = stats.getImageBytes();
            result.videosSize = stats.getVideoBytes();
            result.audioSize = stats.getAudioBytes();
            result.miscSize = ((stats.getTotalBytes() - result.imagesSize) - result.videosSize) - result.audioSize;
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Could not query storage");
        }
    }

    public static long getSystemSize(Context context) {
        File sharedPath;
        PackageManager pm = context.getPackageManager();
        VolumeInfo primaryVolume = pm.getPrimaryStorageCurrentVolume();
        StorageManager sm = (StorageManager) context.getSystemService("storage");
        VolumeInfo shared = sm.findEmulatedForPrivate(primaryVolume);
        if (shared == null || (sharedPath = shared.getPath()) == null) {
            return 0L;
        }
        long sharedDataSize = sharedPath.getTotalSpace();
        long systemSize = sm.getPrimaryStorageSize() - sharedDataSize;
        if (systemSize <= 0) {
            return 0L;
        }
        return systemSize;
    }

    private static MeasurementResult collectFiles(File file, MeasurementResult result) {
        File[] files = file.listFiles();
        if (files == null) {
            return result;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                try {
                    collectFiles(f, result);
                } catch (StackOverflowError e) {
                    return result;
                }
            } else {
                handleFile(result, f);
            }
        }
        return result;
    }

    private static void handleFile(MeasurementResult result, File f) {
        long fileSize = f.length();
        int fileType = EXTENSION_MAP.getOrDefault(getExtensionForFile(f), -1).intValue();
        switch (fileType) {
            case 0:
                result.imagesSize += fileSize;
                return;
            case 1:
                result.videosSize += fileSize;
                return;
            case 2:
                result.audioSize += fileSize;
                return;
            default:
                result.miscSize += fileSize;
                return;
        }
    }

    private static String getExtensionForFile(File file) {
        String fileName = file.getName();
        int index = fileName.lastIndexOf(46);
        if (index == -1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase();
    }

    /* loaded from: classes2.dex */
    public static class MeasurementResult {
        public long audioSize;
        public long imagesSize;
        public long miscSize;
        public long videosSize;

        public long totalAccountedSize() {
            return this.imagesSize + this.videosSize + this.miscSize + this.audioSize;
        }
    }
}
