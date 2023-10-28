package com.android.server.pm.parsing;

import android.content.pm.PackageParserCacheHelper;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Parcel;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import android.util.Slog;
import com.android.server.pm.ApexManager;
import com.android.server.pm.parsing.pkg.PackageImpl;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class PackageCacher {
    private static final String TAG = "PackageCacher";
    public static final AtomicInteger sCachedPackageReadCount = new AtomicInteger();
    private File mCacheDir;

    public PackageCacher(File cacheDir) {
        this.mCacheDir = cacheDir;
    }

    private String getCacheKey(File packageFile, int flags) {
        return packageFile.getName() + '-' + flags;
    }

    protected ParsedPackage fromCacheEntry(byte[] bytes) {
        return fromCacheEntryStatic(bytes);
    }

    public static ParsedPackage fromCacheEntryStatic(byte[] bytes) {
        Parcel p = Parcel.obtain();
        p.unmarshall(bytes, 0, bytes.length);
        p.setDataPosition(0);
        PackageParserCacheHelper.ReadHelper helper = new PackageParserCacheHelper.ReadHelper(p);
        helper.startAndInstall();
        ParsedPackage pkg = new PackageImpl(p);
        p.recycle();
        sCachedPackageReadCount.incrementAndGet();
        return pkg;
    }

    protected byte[] toCacheEntry(ParsedPackage pkg) {
        return toCacheEntryStatic(pkg);
    }

    public static byte[] toCacheEntryStatic(ParsedPackage pkg) {
        Parcel p = Parcel.obtain();
        PackageParserCacheHelper.WriteHelper helper = new PackageParserCacheHelper.WriteHelper(p);
        ((PackageImpl) pkg).writeToParcel(p, 0);
        helper.finishAndUninstall();
        byte[] serialized = p.marshall();
        p.recycle();
        return serialized;
    }

    private static boolean isCacheUpToDate(File packageFile, File cacheFile) {
        try {
            if (packageFile.toPath().startsWith(Environment.getApexDirectory().toPath())) {
                File backingApexFile = ApexManager.getInstance().getBackingApexFile(packageFile);
                if (backingApexFile == null) {
                    Slog.w(TAG, "Failed to find APEX file backing " + packageFile.getAbsolutePath());
                } else {
                    packageFile = backingApexFile;
                }
            }
            StructStat pkg = Os.stat(packageFile.getAbsolutePath());
            StructStat cache = Os.stat(cacheFile.getAbsolutePath());
            return pkg.st_mtime < cache.st_mtime;
        } catch (ErrnoException ee) {
            if (ee.errno != OsConstants.ENOENT) {
                Slog.w("Error while stating package cache : ", ee);
            }
            return false;
        }
    }

    public ParsedPackage getCachedResult(File packageFile, int flags) {
        String cacheKey = getCacheKey(packageFile, flags);
        File cacheFile = new File(this.mCacheDir, cacheKey);
        try {
            if (!isCacheUpToDate(packageFile, cacheFile)) {
                return null;
            }
            byte[] bytes = IoUtils.readFileAsByteArray(cacheFile.getAbsolutePath());
            return fromCacheEntry(bytes);
        } catch (Throwable e) {
            Slog.w(TAG, "Error reading package cache: ", e);
            cacheFile.delete();
            return null;
        }
    }

    public void cacheResult(File packageFile, int flags, ParsedPackage parsed) {
        try {
            String cacheKey = getCacheKey(packageFile, flags);
            File cacheFile = new File(this.mCacheDir, cacheKey);
            if (cacheFile.exists() && !cacheFile.delete()) {
                Slog.e(TAG, "Unable to delete cache file: " + cacheFile);
            }
            byte[] cacheEntry = toCacheEntry(parsed);
            if (cacheEntry == null) {
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(cacheFile);
                try {
                    fos.write(cacheEntry);
                    fos.close();
                } catch (Throwable th) {
                    try {
                        fos.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            } catch (IOException ioe) {
                Slog.w(TAG, "Error writing cache entry.", ioe);
                cacheFile.delete();
            }
        } catch (Throwable e) {
            Slog.w(TAG, "Error saving package cache.", e);
        }
    }

    public void cleanCachedResult(File packageFile) {
        final String packageName = packageFile.getName();
        File[] files = FileUtils.listFilesOrEmpty(this.mCacheDir, new FilenameFilter() { // from class: com.android.server.pm.parsing.PackageCacher$$ExternalSyntheticLambda0
            @Override // java.io.FilenameFilter
            public final boolean accept(File file, String str) {
                boolean startsWith;
                startsWith = str.startsWith(packageName);
                return startsWith;
            }
        });
        for (File file : files) {
            if (!file.delete()) {
                Slog.e(TAG, "Unable to clean cache file: " + file);
            }
        }
    }
}
