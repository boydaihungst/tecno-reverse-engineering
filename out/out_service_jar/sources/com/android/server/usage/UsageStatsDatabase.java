package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.TimeSparseArray;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.content.res.Configuration;
import android.os.Build;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.controllers.JobStatus;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class UsageStatsDatabase {
    public static final int BACKUP_VERSION = 4;
    private static final String BAK_SUFFIX = ".bak";
    private static final String CHECKED_IN_SUFFIX = "-c";
    private static final boolean DEBUG = false;
    private static final int DEFAULT_CURRENT_VERSION = 5;
    static final boolean KEEP_BACKUP_DIR = false;
    static final String KEY_USAGE_STATS = "usage_stats";
    static final int[] MAX_FILES_PER_INTERVAL_TYPE = {100, 50, 12, 10};
    private static final String RETENTION_LEN_KEY = "ro.usagestats.chooser.retention";
    private static final int SELECTION_LOG_RETENTION_LEN = SystemProperties.getInt(RETENTION_LEN_KEY, 14);
    private static final String TAG = "UsageStatsDatabase";
    private int lastSize;
    private final File mBackupsDir;
    private final UnixCalendar mCal;
    private int mCurrentVersion;
    private boolean mFirstUpdate;
    private final File[] mIntervalDirs;
    private final Object mLock;
    private boolean mNewUpdate;
    private final File mPackageMappingsFile;
    final PackagesTokenData mPackagesTokenData;
    final TimeSparseArray<AtomicFile>[] mSortedStatFiles;
    private final File mUpdateBreadcrumb;
    private boolean mUpgradePerformed;
    private final File mVersionFile;

    /* loaded from: classes2.dex */
    public interface CheckinAction {
        boolean checkin(IntervalStats intervalStats);
    }

    /* loaded from: classes2.dex */
    public interface StatCombiner<T> {
        boolean combine(IntervalStats intervalStats, boolean z, List<T> list);
    }

    public UsageStatsDatabase(File dir, int version) {
        this.mLock = new Object();
        this.mPackagesTokenData = new PackagesTokenData();
        this.lastSize = 0;
        File[] fileArr = {new File(dir, "daily"), new File(dir, "weekly"), new File(dir, "monthly"), new File(dir, "yearly")};
        this.mIntervalDirs = fileArr;
        this.mCurrentVersion = version;
        this.mVersionFile = new File(dir, "version");
        this.mBackupsDir = new File(dir, "backups");
        this.mUpdateBreadcrumb = new File(dir, "breadcrumb");
        this.mSortedStatFiles = new TimeSparseArray[fileArr.length];
        this.mPackageMappingsFile = new File(dir, "mappings");
        this.mCal = new UnixCalendar(0L);
    }

    public UsageStatsDatabase(File dir) {
        this(dir, 5);
    }

    public void init(long currentTimeMillis) {
        File[] fileArr;
        TimeSparseArray<AtomicFile>[] timeSparseArrayArr;
        synchronized (this.mLock) {
            for (File f : this.mIntervalDirs) {
                f.mkdirs();
                if (!f.exists()) {
                    throw new IllegalStateException("Failed to create directory " + f.getAbsolutePath());
                }
            }
            checkVersionAndBuildLocked();
            indexFilesLocked();
            for (TimeSparseArray<AtomicFile> files : this.mSortedStatFiles) {
                int startIndex = files.closestIndexOnOrAfter(currentTimeMillis);
                if (startIndex >= 0) {
                    int fileCount = files.size();
                    for (int i = startIndex; i < fileCount; i++) {
                        ((AtomicFile) files.valueAt(i)).delete();
                    }
                    for (int i2 = startIndex; i2 < fileCount; i2++) {
                        files.removeAt(i2);
                    }
                }
            }
        }
    }

    public boolean checkinDailyFiles(CheckinAction checkinAction) {
        synchronized (this.mLock) {
            TimeSparseArray<AtomicFile> files = this.mSortedStatFiles[0];
            int fileCount = files.size();
            int lastCheckin = -1;
            for (int i = 0; i < fileCount - 1; i++) {
                if (((AtomicFile) files.valueAt(i)).getBaseFile().getPath().endsWith(CHECKED_IN_SUFFIX)) {
                    lastCheckin = i;
                }
            }
            int i2 = lastCheckin + 1;
            if (i2 == fileCount - 1) {
                return true;
            }
            for (int i3 = i2; i3 < fileCount - 1; i3++) {
                try {
                    IntervalStats stats = new IntervalStats();
                    readLocked((AtomicFile) files.valueAt(i3), stats);
                    if (!checkinAction.checkin(stats)) {
                        return false;
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "Failed to check-in", e);
                    return false;
                }
            }
            for (int i4 = i2; i4 < fileCount - 1; i4++) {
                AtomicFile file = (AtomicFile) files.valueAt(i4);
                File checkedInFile = new File(file.getBaseFile().getPath() + CHECKED_IN_SUFFIX);
                if (!file.getBaseFile().renameTo(checkedInFile)) {
                    Slog.e(TAG, "Failed to mark file " + file.getBaseFile().getPath() + " as checked-in");
                    return true;
                }
                files.setValueAt(i4, new AtomicFile(checkedInFile));
            }
            return true;
        }
    }

    void forceIndexFiles() {
        synchronized (this.mLock) {
            indexFilesLocked();
        }
    }

    private void indexFilesLocked() {
        FilenameFilter backupFileFilter = new FilenameFilter() { // from class: com.android.server.usage.UsageStatsDatabase.1
            @Override // java.io.FilenameFilter
            public boolean accept(File dir, String name) {
                return !name.endsWith(UsageStatsDatabase.BAK_SUFFIX);
            }
        };
        int i = 0;
        while (true) {
            TimeSparseArray<AtomicFile>[] timeSparseArrayArr = this.mSortedStatFiles;
            if (i < timeSparseArrayArr.length) {
                TimeSparseArray<AtomicFile> timeSparseArray = timeSparseArrayArr[i];
                if (timeSparseArray == null) {
                    timeSparseArrayArr[i] = new TimeSparseArray<>();
                } else {
                    timeSparseArray.clear();
                }
                File[] files = this.mIntervalDirs[i].listFiles(backupFileFilter);
                if (files != null) {
                    for (File f : files) {
                        AtomicFile af = new AtomicFile(f);
                        try {
                            this.mSortedStatFiles[i].put(parseBeginTime(af), af);
                        } catch (IOException e) {
                            Slog.e(TAG, "failed to index file: " + f, e);
                        }
                    }
                    int toDelete = this.mSortedStatFiles[i].size() - MAX_FILES_PER_INTERVAL_TYPE[i];
                    if (toDelete > 0) {
                        for (int j = 0; j < toDelete; j++) {
                            ((AtomicFile) this.mSortedStatFiles[i].valueAt(0)).delete();
                            this.mSortedStatFiles[i].removeAt(0);
                        }
                        Slog.d(TAG, "Deleted " + toDelete + " stat files for interval " + i);
                    }
                }
                i++;
            } else {
                return;
            }
        }
    }

    boolean isFirstUpdate() {
        return this.mFirstUpdate;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isNewUpdate() {
        return this.mNewUpdate;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean wasUpgradePerformed() {
        return this.mUpgradePerformed;
    }

    private void checkVersionAndBuildLocked() {
        int version;
        String currentFingerprint = getBuildFingerprint();
        this.mFirstUpdate = true;
        this.mNewUpdate = true;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.mVersionFile));
            version = Integer.parseInt(reader.readLine());
            String buildFingerprint = reader.readLine();
            if (buildFingerprint != null) {
                this.mFirstUpdate = false;
            }
            if (currentFingerprint.equals(buildFingerprint)) {
                this.mNewUpdate = false;
            }
            reader.close();
        } catch (IOException | NumberFormatException e) {
            version = 0;
        }
        if (version != this.mCurrentVersion) {
            Slog.i(TAG, "Upgrading from version " + version + " to " + this.mCurrentVersion);
            if (!this.mUpdateBreadcrumb.exists()) {
                try {
                    doUpgradeLocked(version);
                } catch (Exception e2) {
                    Slog.e(TAG, "Failed to upgrade from version " + version + " to " + this.mCurrentVersion, e2);
                    this.mCurrentVersion = version;
                    return;
                }
            } else {
                Slog.i(TAG, "Version upgrade breadcrumb found on disk! Continuing version upgrade");
            }
        }
        if (this.mUpdateBreadcrumb.exists()) {
            try {
                BufferedReader reader2 = new BufferedReader(new FileReader(this.mUpdateBreadcrumb));
                long token = Long.parseLong(reader2.readLine());
                int previousVersion = Integer.parseInt(reader2.readLine());
                reader2.close();
                if (this.mCurrentVersion >= 4) {
                    continueUpgradeLocked(previousVersion, token);
                } else {
                    Slog.wtf(TAG, "Attempting to upgrade to an unsupported version: " + this.mCurrentVersion);
                }
            } catch (IOException | NumberFormatException e3) {
                Slog.e(TAG, "Failed read version upgrade breadcrumb");
                throw new RuntimeException(e3);
            }
        }
        if (version != this.mCurrentVersion || this.mNewUpdate) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(this.mVersionFile));
                writer.write(Integer.toString(this.mCurrentVersion));
                writer.write("\n");
                writer.write(currentFingerprint);
                writer.write("\n");
                writer.flush();
                writer.close();
            } catch (IOException e4) {
                Slog.e(TAG, "Failed to write new version");
                throw new RuntimeException(e4);
            }
        }
        if (this.mUpdateBreadcrumb.exists()) {
            this.mUpdateBreadcrumb.delete();
            this.mUpgradePerformed = true;
        }
        if (this.mBackupsDir.exists()) {
            this.mUpgradePerformed = true;
            deleteDirectory(this.mBackupsDir);
        }
    }

    private String getBuildFingerprint() {
        return Build.VERSION.RELEASE + ";" + Build.VERSION.CODENAME + ";" + Build.VERSION.INCREMENTAL;
    }

    private void doUpgradeLocked(int thisVersion) {
        boolean z = false;
        if (thisVersion < 2) {
            Slog.i(TAG, "Deleting all usage stats files");
            int i = 0;
            while (true) {
                File[] fileArr = this.mIntervalDirs;
                if (i < fileArr.length) {
                    File[] files = fileArr[i].listFiles();
                    if (files != null) {
                        for (File f : files) {
                            f.delete();
                        }
                    }
                    i++;
                } else {
                    return;
                }
            }
        } else {
            long token = System.currentTimeMillis();
            File backupDir = new File(this.mBackupsDir, Long.toString(token));
            backupDir.mkdirs();
            if (!backupDir.exists()) {
                throw new IllegalStateException("Failed to create backup directory " + backupDir.getAbsolutePath());
            }
            try {
                Files.copy(this.mVersionFile.toPath(), new File(backupDir, this.mVersionFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                int i2 = 0;
                while (i2 < this.mIntervalDirs.length) {
                    File backupIntervalDir = new File(backupDir, this.mIntervalDirs[i2].getName());
                    backupIntervalDir.mkdir();
                    if (!backupIntervalDir.exists()) {
                        throw new IllegalStateException("Failed to create interval backup directory " + backupIntervalDir.getAbsolutePath());
                    }
                    File[] files2 = this.mIntervalDirs[i2].listFiles();
                    if (files2 != null) {
                        int j = 0;
                        while (j < files2.length) {
                            File backupFile = new File(backupIntervalDir, files2[j].getName());
                            try {
                                Files.move(files2[j].toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                j++;
                                z = false;
                            } catch (IOException e) {
                                Slog.e(TAG, "Failed to back up file : " + files2[j].toString());
                                throw new RuntimeException(e);
                            }
                        }
                        continue;
                    }
                    i2++;
                    z = z;
                }
                BufferedWriter writer = null;
                try {
                    try {
                        writer = new BufferedWriter(new FileWriter(this.mUpdateBreadcrumb));
                        writer.write(Long.toString(token));
                        writer.write("\n");
                        writer.write(Integer.toString(thisVersion));
                        writer.write("\n");
                        writer.flush();
                    } catch (IOException e2) {
                        Slog.e(TAG, "Failed to write new version upgrade breadcrumb");
                        throw new RuntimeException(e2);
                    }
                } finally {
                    IoUtils.closeQuietly(writer);
                }
            } catch (IOException e3) {
                Slog.e(TAG, "Failed to back up version file : " + this.mVersionFile.toString());
                throw new RuntimeException(e3);
            }
        }
    }

    private void continueUpgradeLocked(int version, long token) {
        if (version <= 3) {
            Slog.w(TAG, "Reading UsageStats as XML; current database version: " + this.mCurrentVersion);
        }
        File backupDir = new File(this.mBackupsDir, Long.toString(token));
        if (version >= 5) {
            readMappingsLocked();
        }
        for (int i = 0; i < this.mIntervalDirs.length; i++) {
            File backedUpInterval = new File(backupDir, this.mIntervalDirs[i].getName());
            File[] files = backedUpInterval.listFiles();
            if (files != null) {
                for (int j = 0; j < files.length; j++) {
                    try {
                        IntervalStats stats = new IntervalStats();
                        readLocked(new AtomicFile(files[j]), stats, version, this.mPackagesTokenData);
                        if (this.mCurrentVersion >= 5) {
                            stats.obfuscateData(this.mPackagesTokenData);
                        }
                        writeLocked(new AtomicFile(new File(this.mIntervalDirs[i], Long.toString(stats.beginTime))), stats, this.mCurrentVersion, this.mPackagesTokenData);
                    } catch (Exception e) {
                        Slog.e(TAG, "Failed to upgrade backup file : " + files[j].toString());
                    }
                }
            }
        }
        if (this.mCurrentVersion >= 5) {
            try {
                writeMappingsLocked();
            } catch (IOException e2) {
                Slog.e(TAG, "Failed to write the tokens mappings file.");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int onPackageRemoved(String packageName, long timeRemoved) {
        int tokenRemoved;
        synchronized (this.mLock) {
            tokenRemoved = this.mPackagesTokenData.removePackage(packageName, timeRemoved);
            try {
                writeMappingsLocked();
            } catch (Exception e) {
                Slog.w(TAG, "Unable to update package mappings on disk after removing token " + tokenRemoved);
            }
        }
        return tokenRemoved;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean pruneUninstalledPackagesData() {
        synchronized (this.mLock) {
            int i = 0;
            while (true) {
                File[] fileArr = this.mIntervalDirs;
                if (i < fileArr.length) {
                    File[] files = fileArr[i].listFiles();
                    if (files != null) {
                        for (int j = 0; j < files.length; j++) {
                            try {
                                IntervalStats stats = new IntervalStats();
                                AtomicFile atomicFile = new AtomicFile(files[j]);
                                if (readLocked(atomicFile, stats, this.mCurrentVersion, this.mPackagesTokenData)) {
                                    writeLocked(atomicFile, stats, this.mCurrentVersion, this.mPackagesTokenData);
                                }
                            } catch (Exception e) {
                                Slog.e(TAG, "Failed to prune data from: " + files[j].toString());
                                return false;
                            }
                        }
                        continue;
                    }
                    i++;
                } else {
                    try {
                        writeMappingsLocked();
                    } catch (IOException e2) {
                        Slog.e(TAG, "Failed to write package mappings after pruning data.");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prunePackagesDataOnUpgrade(HashMap<String, Long> installedPackages) {
        if (ArrayUtils.isEmpty(installedPackages)) {
            return;
        }
        synchronized (this.mLock) {
            int i = 0;
            while (true) {
                File[] fileArr = this.mIntervalDirs;
                if (i < fileArr.length) {
                    File[] files = fileArr[i].listFiles();
                    if (files != null) {
                        for (int j = 0; j < files.length; j++) {
                            try {
                                IntervalStats stats = new IntervalStats();
                                AtomicFile atomicFile = new AtomicFile(files[j]);
                                readLocked(atomicFile, stats, this.mCurrentVersion, this.mPackagesTokenData);
                                if (pruneStats(installedPackages, stats)) {
                                    writeLocked(atomicFile, stats, this.mCurrentVersion, this.mPackagesTokenData);
                                }
                            } catch (Exception e) {
                                Slog.e(TAG, "Failed to prune data from: " + files[j].toString());
                            }
                        }
                    }
                    i++;
                }
            }
        }
    }

    private boolean pruneStats(HashMap<String, Long> installedPackages, IntervalStats stats) {
        boolean dataPruned = false;
        for (int i = stats.packageStats.size() - 1; i >= 0; i--) {
            UsageStats usageStats = stats.packageStats.valueAt(i);
            Long timeInstalled = installedPackages.get(usageStats.mPackageName);
            if (timeInstalled == null || timeInstalled.longValue() > usageStats.mEndTimeStamp) {
                stats.packageStats.removeAt(i);
                dataPruned = true;
            }
        }
        if (dataPruned) {
            stats.packageStatsObfuscated.clear();
        }
        for (int i2 = stats.events.size() - 1; i2 >= 0; i2--) {
            UsageEvents.Event event = stats.events.get(i2);
            Long timeInstalled2 = installedPackages.get(event.mPackage);
            if (timeInstalled2 == null || timeInstalled2.longValue() > event.mTimeStamp) {
                stats.events.remove(i2);
                dataPruned = true;
            }
        }
        return dataPruned;
    }

    public void onTimeChanged(long timeDiffMillis) {
        long j = timeDiffMillis;
        synchronized (this.mLock) {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("Time changed by ");
            TimeUtils.formatDuration(j, logBuilder);
            logBuilder.append(".");
            int filesDeleted = 0;
            int filesDeleted2 = 0;
            TimeSparseArray<AtomicFile>[] timeSparseArrayArr = this.mSortedStatFiles;
            int length = timeSparseArrayArr.length;
            int i = 0;
            while (i < length) {
                TimeSparseArray<AtomicFile> files = timeSparseArrayArr[i];
                int fileCount = files.size();
                int i2 = 0;
                int filesMoved = filesDeleted2;
                int filesDeleted3 = filesDeleted;
                while (i2 < fileCount) {
                    AtomicFile file = (AtomicFile) files.valueAt(i2);
                    long newTime = files.keyAt(i2) + j;
                    if (newTime < 0) {
                        filesDeleted3++;
                        file.delete();
                    } else {
                        try {
                            file.openRead().close();
                        } catch (IOException e) {
                        }
                        String newName = Long.toString(newTime);
                        if (file.getBaseFile().getName().endsWith(CHECKED_IN_SUFFIX)) {
                            newName = newName + CHECKED_IN_SUFFIX;
                        }
                        File newFile = new File(file.getBaseFile().getParentFile(), newName);
                        filesMoved++;
                        file.getBaseFile().renameTo(newFile);
                    }
                    i2++;
                    j = timeDiffMillis;
                }
                files.clear();
                i++;
                j = timeDiffMillis;
                filesDeleted = filesDeleted3;
                filesDeleted2 = filesMoved;
            }
            logBuilder.append(" files deleted: ").append(filesDeleted);
            logBuilder.append(" files moved: ").append(filesDeleted2);
            Slog.i(TAG, logBuilder.toString());
            indexFilesLocked();
        }
    }

    public IntervalStats getLatestUsageStats(int intervalType) {
        synchronized (this.mLock) {
            if (intervalType >= 0) {
                if (intervalType < this.mIntervalDirs.length) {
                    int fileCount = this.mSortedStatFiles[intervalType].size();
                    if (fileCount == 0) {
                        return null;
                    }
                    try {
                        AtomicFile f = (AtomicFile) this.mSortedStatFiles[intervalType].valueAt(fileCount - 1);
                        IntervalStats stats = new IntervalStats();
                        readLocked(f, stats);
                        return stats;
                    } catch (Exception e) {
                        Slog.e(TAG, "Failed to read usage stats file", e);
                        return null;
                    }
                }
            }
            throw new IllegalArgumentException("Bad interval type " + intervalType);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void filterStats(IntervalStats stats) {
        if (this.mPackagesTokenData.removedPackagesMap.isEmpty()) {
            return;
        }
        ArrayMap<String, Long> removedPackagesMap = this.mPackagesTokenData.removedPackagesMap;
        int removedPackagesSize = removedPackagesMap.size();
        for (int i = 0; i < removedPackagesSize; i++) {
            String removedPackage = removedPackagesMap.keyAt(i);
            UsageStats usageStats = stats.packageStats.get(removedPackage);
            if (usageStats != null && usageStats.mEndTimeStamp < removedPackagesMap.valueAt(i).longValue()) {
                stats.packageStats.remove(removedPackage);
            }
        }
        for (int i2 = stats.events.size() - 1; i2 >= 0; i2--) {
            UsageEvents.Event event = stats.events.get(i2);
            Long timeRemoved = removedPackagesMap.get(event.mPackage);
            if (timeRemoved != null && timeRemoved.longValue() > event.mTimeStamp) {
                stats.events.remove(i2);
            }
        }
    }

    public <T> List<T> queryUsageStats(int intervalType, long beginTime, long endTime, StatCombiner<T> combiner) {
        int endIndex;
        int startIndex;
        AtomicFile f;
        IntervalStats stats;
        UsageStatsDatabase usageStatsDatabase = this;
        if (intervalType < 0 || intervalType >= usageStatsDatabase.mIntervalDirs.length) {
            throw new IllegalArgumentException("Bad interval type " + intervalType);
        }
        if (endTime <= beginTime) {
            return null;
        }
        synchronized (usageStatsDatabase.mLock) {
            try {
                TimeSparseArray<AtomicFile> intervalStats = usageStatsDatabase.mSortedStatFiles[intervalType];
                int endIndex2 = intervalStats.closestIndexOnOrBefore(endTime);
                if (endIndex2 < 0) {
                    return null;
                }
                if (intervalStats.keyAt(endIndex2) != endTime) {
                    endIndex = endIndex2;
                } else {
                    int endIndex3 = endIndex2 - 1;
                    if (endIndex3 < 0) {
                        return null;
                    }
                    endIndex = endIndex3;
                }
                int startIndex2 = intervalStats.closestIndexOnOrBefore(beginTime);
                if (startIndex2 >= 0) {
                    startIndex = startIndex2;
                } else {
                    startIndex = 0;
                }
                ArrayList<T> results = new ArrayList<>();
                int i = startIndex;
                while (i <= endIndex) {
                    try {
                        f = (AtomicFile) intervalStats.valueAt(i);
                        stats = new IntervalStats();
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        usageStatsDatabase.readLocked(f, stats);
                        if (beginTime < stats.endTime) {
                            try {
                                if (!combiner.combine(stats, false, results)) {
                                    break;
                                }
                            } catch (Exception e) {
                                e = e;
                                Slog.e(TAG, "Failed to read usage stats file", e);
                                i++;
                                usageStatsDatabase = this;
                            }
                        }
                    } catch (Exception e2) {
                        e = e2;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                    i++;
                    usageStatsDatabase = this;
                }
                int querySize = results.size();
                if (querySize / 1000 >= 1) {
                    if (Build.IS_DEBUG_ENABLE) {
                        Slog.d(TAG, "UsageStatsDatabase.queryUsageStats results :" + querySize);
                    }
                    if (this.lastSize != querySize / 1000) {
                        UsageStatsProtoV2.writeTranLog(0, querySize);
                        this.lastSize = querySize / 1000;
                    }
                }
                return results;
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public int findBestFitBucket(long beginTimeStamp, long endTimeStamp) {
        int bestBucket;
        synchronized (this.mLock) {
            bestBucket = -1;
            long smallestDiff = JobStatus.NO_LATEST_RUNTIME;
            for (int i = this.mSortedStatFiles.length - 1; i >= 0; i--) {
                int index = this.mSortedStatFiles[i].closestIndexOnOrBefore(beginTimeStamp);
                int size = this.mSortedStatFiles[i].size();
                if (index >= 0 && index < size) {
                    long diff = Math.abs(this.mSortedStatFiles[i].keyAt(index) - beginTimeStamp);
                    if (diff < smallestDiff) {
                        smallestDiff = diff;
                        bestBucket = i;
                    }
                }
            }
        }
        return bestBucket;
    }

    public void prune(long currentTimeMillis) {
        synchronized (this.mLock) {
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addYears(-3);
            pruneFilesOlderThan(this.mIntervalDirs[3], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addMonths(-6);
            pruneFilesOlderThan(this.mIntervalDirs[2], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addWeeks(-4);
            pruneFilesOlderThan(this.mIntervalDirs[1], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addDays(-10);
            pruneFilesOlderThan(this.mIntervalDirs[0], this.mCal.getTimeInMillis());
            this.mCal.setTimeInMillis(currentTimeMillis);
            this.mCal.addDays(-SELECTION_LOG_RETENTION_LEN);
            int i = 0;
            while (true) {
                File[] fileArr = this.mIntervalDirs;
                if (i < fileArr.length) {
                    pruneChooserCountsOlderThan(fileArr[i], this.mCal.getTimeInMillis());
                    i++;
                } else {
                    indexFilesLocked();
                }
            }
        }
    }

    private static void pruneFilesOlderThan(File dir, long expiryTime) {
        long beginTime;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                try {
                    beginTime = parseBeginTime(f);
                } catch (IOException e) {
                    beginTime = 0;
                }
                if (beginTime < expiryTime) {
                    new AtomicFile(f).delete();
                }
            }
        }
    }

    private void pruneChooserCountsOlderThan(File dir, long expiryTime) {
        long beginTime;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                try {
                    beginTime = parseBeginTime(f);
                } catch (IOException e) {
                    beginTime = 0;
                }
                if (beginTime < expiryTime) {
                    try {
                        AtomicFile af = new AtomicFile(f);
                        IntervalStats stats = new IntervalStats();
                        readLocked(af, stats);
                        int pkgCount = stats.packageStats.size();
                        for (int i = 0; i < pkgCount; i++) {
                            UsageStats pkgStats = stats.packageStats.valueAt(i);
                            if (pkgStats.mChooserCounts != null) {
                                pkgStats.mChooserCounts.clear();
                            }
                        }
                        writeLocked(af, stats);
                    } catch (Exception e2) {
                        Slog.e(TAG, "Failed to delete chooser counts from usage stats file", e2);
                    }
                }
            }
        }
    }

    private static long parseBeginTime(AtomicFile file) throws IOException {
        return parseBeginTime(file.getBaseFile());
    }

    private static long parseBeginTime(File file) throws IOException {
        String name = file.getName();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < '0' || c > '9') {
                name = name.substring(0, i);
                break;
            }
        }
        try {
            return Long.parseLong(name);
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }

    private void writeLocked(AtomicFile file, IntervalStats stats) throws IOException, RuntimeException {
        int i = this.mCurrentVersion;
        if (i <= 3) {
            Slog.wtf(TAG, "Attempting to write UsageStats as XML with version " + this.mCurrentVersion);
        } else {
            writeLocked(file, stats, i, this.mPackagesTokenData);
        }
    }

    private static void writeLocked(AtomicFile file, IntervalStats stats, int version, PackagesTokenData packagesTokenData) throws IOException, RuntimeException {
        FileOutputStream fos = file.startWrite();
        try {
            try {
                writeLocked(fos, stats, version, packagesTokenData);
                file.finishWrite(fos);
                fos = null;
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    String error = e.toString();
                    Throwable outCause = e.getCause();
                    String causeBy = outCause != null ? outCause.toString() : "";
                    if (!error.contains("No space left on device") && !causeBy.contains("No space left on device")) {
                        if (error.contains("Unhandled UsageStatsDatabase version:")) {
                            throw e;
                        }
                    }
                    Slog.w(TAG, "Usage flush is eroor, No space left on device");
                }
                Slog.e(TAG, "Unable to write interval stats to proto.", e);
            }
        } finally {
            file.failWrite(fos);
        }
    }

    private static void writeLocked(OutputStream out, IntervalStats stats, int version, PackagesTokenData packagesTokenData) throws RuntimeException, IOException {
        switch (version) {
            case 1:
            case 2:
            case 3:
                Slog.wtf(TAG, "Attempting to write UsageStats as XML with version " + version);
                return;
            case 4:
                UsageStatsProto.write(out, stats);
                return;
            case 5:
                stats.obfuscateData(packagesTokenData);
                UsageStatsProtoV2.write(out, stats);
                return;
            default:
                throw new RuntimeException("Unhandled UsageStatsDatabase version: " + Integer.toString(version) + " on write.");
        }
    }

    private void readLocked(AtomicFile file, IntervalStats statsOut) throws IOException, RuntimeException {
        if (this.mCurrentVersion <= 3) {
            Slog.wtf(TAG, "Reading UsageStats as XML; current database version: " + this.mCurrentVersion);
        }
        readLocked(file, statsOut, this.mCurrentVersion, this.mPackagesTokenData);
    }

    private static boolean readLocked(AtomicFile file, IntervalStats statsOut, int version, PackagesTokenData packagesTokenData) throws IOException, RuntimeException {
        try {
            FileInputStream in = file.openRead();
            statsOut.beginTime = parseBeginTime(file);
            boolean dataOmitted = readLocked(in, statsOut, version, packagesTokenData);
            statsOut.lastTimeSaved = file.getLastModifiedTime();
            try {
                in.close();
            } catch (IOException e) {
            }
            return dataOmitted;
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, TAG, e2);
            throw e2;
        }
    }

    private static boolean readLocked(InputStream in, IntervalStats statsOut, int version, PackagesTokenData packagesTokenData) throws RuntimeException {
        switch (version) {
            case 1:
            case 2:
            case 3:
                Slog.w(TAG, "Reading UsageStats as XML; database version: " + version);
                try {
                    UsageStatsXml.read(in, statsOut);
                    return false;
                } catch (Exception e) {
                    Slog.e(TAG, "Unable to read interval stats from XML", e);
                    return false;
                }
            case 4:
                try {
                    UsageStatsProto.read(in, statsOut);
                    return false;
                } catch (Exception e2) {
                    Slog.e(TAG, "Unable to read interval stats from proto.", e2);
                    return false;
                }
            case 5:
                try {
                    UsageStatsProtoV2.read(in, statsOut);
                } catch (Exception e3) {
                    Slog.e(TAG, "Unable to read interval stats from proto.", e3);
                }
                boolean dataOmitted = statsOut.deobfuscateData(packagesTokenData);
                return dataOmitted;
            default:
                throw new RuntimeException("Unhandled UsageStatsDatabase version: " + Integer.toString(version) + " on read.");
        }
    }

    public void readMappingsLocked() {
        if (!this.mPackageMappingsFile.exists()) {
            return;
        }
        try {
            FileInputStream in = new AtomicFile(this.mPackageMappingsFile).openRead();
            UsageStatsProtoV2.readObfuscatedData(in, this.mPackagesTokenData);
            if (in != null) {
                in.close();
            }
            SparseArray<ArrayList<String>> tokensToPackagesMap = this.mPackagesTokenData.tokensToPackagesMap;
            int tokensToPackagesMapSize = tokensToPackagesMap.size();
            for (int i = 0; i < tokensToPackagesMapSize; i++) {
                int packageToken = tokensToPackagesMap.keyAt(i);
                ArrayList<String> tokensMap = tokensToPackagesMap.valueAt(i);
                ArrayMap<String, Integer> packageStringsMap = new ArrayMap<>();
                int tokensMapSize = tokensMap.size();
                packageStringsMap.put(tokensMap.get(0), Integer.valueOf(packageToken));
                for (int j = 1; j < tokensMapSize; j++) {
                    packageStringsMap.put(tokensMap.get(j), Integer.valueOf(j));
                }
                this.mPackagesTokenData.packagesToTokensMap.put(tokensMap.get(0), packageStringsMap);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read the obfuscated packages mapping file.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeMappingsLocked() throws IOException {
        AtomicFile file = new AtomicFile(this.mPackageMappingsFile);
        FileOutputStream fos = file.startWrite();
        try {
            try {
                UsageStatsProtoV2.writeObfuscatedData(fos, this.mPackagesTokenData);
                file.finishWrite(fos);
                fos = null;
            } catch (Exception e) {
                Slog.e(TAG, "Unable to write obfuscated data to proto.", e);
            }
        } finally {
            file.failWrite(fos);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void obfuscateCurrentStats(IntervalStats[] currentStats) {
        if (this.mCurrentVersion < 5) {
            return;
        }
        for (IntervalStats stats : currentStats) {
            stats.obfuscateData(this.mPackagesTokenData);
        }
    }

    public void putUsageStats(int intervalType, IntervalStats stats) throws IOException {
        if (stats == null) {
            return;
        }
        synchronized (this.mLock) {
            if (intervalType >= 0) {
                if (intervalType < this.mIntervalDirs.length) {
                    AtomicFile f = (AtomicFile) this.mSortedStatFiles[intervalType].get(stats.beginTime);
                    if (f == null) {
                        f = new AtomicFile(new File(this.mIntervalDirs[intervalType], Long.toString(stats.beginTime)));
                        this.mSortedStatFiles[intervalType].put(stats.beginTime, f);
                    }
                    writeLocked(f, stats);
                    stats.lastTimeSaved = f.getLastModifiedTime();
                }
            }
            throw new IllegalArgumentException("Bad interval type " + intervalType);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public byte[] getBackupPayload(String key) {
        return getBackupPayload(key, 4);
    }

    public byte[] getBackupPayload(String key, int version) {
        byte[] byteArray;
        if (version >= 1 && version <= 3) {
            Slog.wtf(TAG, "Attempting to backup UsageStats as XML with version " + version);
            return null;
        }
        synchronized (this.mLock) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (KEY_USAGE_STATS.equals(key)) {
                prune(System.currentTimeMillis());
                DataOutputStream out = new DataOutputStream(baos);
                try {
                    out.writeInt(version);
                    out.writeInt(this.mSortedStatFiles[0].size());
                    for (int i = 0; i < this.mSortedStatFiles[0].size(); i++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[0].valueAt(i), version);
                    }
                    out.writeInt(this.mSortedStatFiles[1].size());
                    for (int i2 = 0; i2 < this.mSortedStatFiles[1].size(); i2++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[1].valueAt(i2), version);
                    }
                    out.writeInt(this.mSortedStatFiles[2].size());
                    for (int i3 = 0; i3 < this.mSortedStatFiles[2].size(); i3++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[2].valueAt(i3), version);
                    }
                    out.writeInt(this.mSortedStatFiles[3].size());
                    for (int i4 = 0; i4 < this.mSortedStatFiles[3].size(); i4++) {
                        writeIntervalStatsToStream(out, (AtomicFile) this.mSortedStatFiles[3].valueAt(i4), version);
                    }
                } catch (IOException ioe) {
                    Slog.d(TAG, "Failed to write data to output stream", ioe);
                    baos.reset();
                }
            }
            byteArray = baos.toByteArray();
        }
        return byteArray;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:54:0x00e3
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1403=4] */
    public void applyRestoredPayload(java.lang.String r18, byte[] r19) {
        /*
            r17 = this;
            r1 = r17
            java.lang.Object r2 = r1.mLock
            monitor-enter(r2)
            java.lang.String r0 = "usage_stats"
            r3 = r18
            boolean r0 = r0.equals(r3)     // Catch: java.lang.Throwable -> Le1
            if (r0 == 0) goto Ldd
        L11:
            r0 = 0
            com.android.server.usage.IntervalStats r4 = r1.getLatestUsageStats(r0)     // Catch: java.lang.Throwable -> Le1
            r5 = 1
            com.android.server.usage.IntervalStats r6 = r1.getLatestUsageStats(r5)     // Catch: java.lang.Throwable -> Le1
            r7 = 2
            com.android.server.usage.IntervalStats r8 = r1.getLatestUsageStats(r7)     // Catch: java.lang.Throwable -> Le1
            r9 = 3
            com.android.server.usage.IntervalStats r10 = r1.getLatestUsageStats(r9)     // Catch: java.lang.Throwable -> Le1
            java.io.DataInputStream r11 = new java.io.DataInputStream     // Catch: java.lang.Throwable -> Lc5 java.io.IOException -> Lc9
            java.io.ByteArrayInputStream r12 = new java.io.ByteArrayInputStream     // Catch: java.lang.Throwable -> Lc5 java.io.IOException -> Lc9
            r13 = r19
            r12.<init>(r13)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            r11.<init>(r12)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            int r12 = r11.readInt()     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            if (r12 < r5) goto Lbe
            r14 = 4
            if (r12 <= r14) goto L3f
            goto Lbe
        L3f:
            r14 = 0
        L40:
            java.io.File[] r15 = r1.mIntervalDirs     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            int r9 = r15.length     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            if (r14 >= r9) goto L4e
            r9 = r15[r14]     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            deleteDirectoryContents(r9)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            int r14 = r14 + 1
            r9 = 3
            goto L40
        L4e:
            int r9 = r11.readInt()     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            r14 = 0
        L53:
            if (r14 >= r9) goto L69
            byte[] r15 = getIntervalStatsBytes(r11)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            com.android.server.usage.IntervalStats r15 = r1.deserializeIntervalStats(r15, r12)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            com.android.server.usage.IntervalStats r16 = r1.mergeStats(r15, r4)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            r15 = r16
            r1.putUsageStats(r0, r15)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            int r14 = r14 + 1
            goto L53
        L69:
            int r0 = r11.readInt()     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            r9 = 0
        L6e:
            if (r9 >= r0) goto L83
            byte[] r14 = getIntervalStatsBytes(r11)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            com.android.server.usage.IntervalStats r14 = r1.deserializeIntervalStats(r14, r12)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            com.android.server.usage.IntervalStats r15 = r1.mergeStats(r14, r6)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            r14 = r15
            r1.putUsageStats(r5, r14)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            int r9 = r9 + 1
            goto L6e
        L83:
            int r5 = r11.readInt()     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            r0 = r5
            r5 = 0
        L89:
            if (r5 >= r0) goto L9e
            byte[] r9 = getIntervalStatsBytes(r11)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            com.android.server.usage.IntervalStats r9 = r1.deserializeIntervalStats(r9, r12)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            com.android.server.usage.IntervalStats r14 = r1.mergeStats(r9, r8)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            r9 = r14
            r1.putUsageStats(r7, r9)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            int r5 = r5 + 1
            goto L89
        L9e:
            int r5 = r11.readInt()     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            r0 = r5
            r5 = 0
        La4:
            if (r5 >= r0) goto Lba
            byte[] r7 = getIntervalStatsBytes(r11)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            com.android.server.usage.IntervalStats r7 = r1.deserializeIntervalStats(r7, r12)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            com.android.server.usage.IntervalStats r9 = r1.mergeStats(r7, r10)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            r7 = r9
            r9 = 3
            r1.putUsageStats(r9, r7)     // Catch: java.io.IOException -> Lc3 java.lang.Throwable -> Ld7
            int r5 = r5 + 1
            goto La4
        Lba:
            r17.indexFilesLocked()     // Catch: java.lang.Throwable -> Lea
            goto Ld6
        Lbe:
            r17.indexFilesLocked()     // Catch: java.lang.Throwable -> Lea
            monitor-exit(r2)     // Catch: java.lang.Throwable -> Lea
            return
        Lc3:
            r0 = move-exception
            goto Lcc
        Lc5:
            r0 = move-exception
            r13 = r19
            goto Ld8
        Lc9:
            r0 = move-exception
            r13 = r19
        Lcc:
            java.lang.String r5 = "UsageStatsDatabase"
            java.lang.String r7 = "Failed to read data from input stream"
            android.util.Slog.d(r5, r7, r0)     // Catch: java.lang.Throwable -> Ld7
            r17.indexFilesLocked()     // Catch: java.lang.Throwable -> Lea
        Ld6:
            goto Ldf
        Ld7:
            r0 = move-exception
        Ld8:
            r17.indexFilesLocked()     // Catch: java.lang.Throwable -> Lea
            throw r0     // Catch: java.lang.Throwable -> Lea
        Ldd:
            r13 = r19
        Ldf:
            monitor-exit(r2)     // Catch: java.lang.Throwable -> Lea
            return
        Le1:
            r0 = move-exception
            goto Le6
        Le3:
            r0 = move-exception
            r3 = r18
        Le6:
            r13 = r19
        Le8:
            monitor-exit(r2)     // Catch: java.lang.Throwable -> Lea
            throw r0
        Lea:
            r0 = move-exception
            goto Le8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.usage.UsageStatsDatabase.applyRestoredPayload(java.lang.String, byte[]):void");
    }

    private IntervalStats mergeStats(IntervalStats beingRestored, IntervalStats onDevice) {
        if (onDevice == null) {
            return beingRestored;
        }
        if (beingRestored == null) {
            return null;
        }
        beingRestored.activeConfiguration = onDevice.activeConfiguration;
        beingRestored.configurations.putAll((ArrayMap<? extends Configuration, ? extends ConfigurationStats>) onDevice.configurations);
        beingRestored.events.clear();
        beingRestored.events.merge(onDevice.events);
        return beingRestored;
    }

    private void writeIntervalStatsToStream(DataOutputStream out, AtomicFile statsFile, int version) throws IOException {
        IntervalStats stats = new IntervalStats();
        try {
            readLocked(statsFile, stats);
            sanitizeIntervalStatsForBackup(stats);
            byte[] data = serializeIntervalStats(stats, version);
            out.writeInt(data.length);
            out.write(data);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read usage stats file", e);
            out.writeInt(0);
        }
    }

    private static byte[] getIntervalStatsBytes(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
        return buffer;
    }

    private static void sanitizeIntervalStatsForBackup(IntervalStats stats) {
        if (stats == null) {
            return;
        }
        stats.activeConfiguration = null;
        stats.configurations.clear();
        stats.events.clear();
    }

    private byte[] serializeIntervalStats(IntervalStats stats, int version) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        try {
            out.writeLong(stats.beginTime);
            writeLocked(out, stats, version, this.mPackagesTokenData);
        } catch (Exception ioe) {
            Slog.d(TAG, "Serializing IntervalStats Failed", ioe);
            baos.reset();
        }
        return baos.toByteArray();
    }

    private IntervalStats deserializeIntervalStats(byte[] data, int version) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        IntervalStats stats = new IntervalStats();
        try {
            stats.beginTime = in.readLong();
            readLocked(in, stats, version, this.mPackagesTokenData);
            return stats;
        } catch (Exception e) {
            Slog.d(TAG, "DeSerializing IntervalStats Failed", e);
            return null;
        }
    }

    private static void deleteDirectoryContents(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            deleteDirectory(file);
        }
    }

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    file.delete();
                } else {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }

    public void dump(IndentingPrintWriter pw, boolean compact) {
        synchronized (this.mLock) {
            pw.println();
            pw.println("UsageStatsDatabase:");
            pw.increaseIndent();
            dumpMappings(pw);
            pw.decreaseIndent();
            pw.println("Database Summary:");
            pw.increaseIndent();
            int i = 0;
            while (true) {
                TimeSparseArray<AtomicFile>[] timeSparseArrayArr = this.mSortedStatFiles;
                if (i < timeSparseArrayArr.length) {
                    TimeSparseArray<AtomicFile> files = timeSparseArrayArr[i];
                    int size = files.size();
                    pw.print(UserUsageStatsService.intervalToString(i));
                    pw.print(" stats files: ");
                    pw.print(size);
                    pw.println(", sorted list of files:");
                    pw.increaseIndent();
                    for (int f = 0; f < size; f++) {
                        long fileName = files.keyAt(f);
                        if (compact) {
                            pw.print(UserUsageStatsService.formatDateTime(fileName, false));
                        } else {
                            pw.printPair(Long.toString(fileName), UserUsageStatsService.formatDateTime(fileName, true));
                        }
                        pw.println();
                    }
                    pw.decreaseIndent();
                    i++;
                } else {
                    pw.decreaseIndent();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpMappings(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("Obfuscated Packages Mappings:");
            pw.increaseIndent();
            pw.println("Counter: " + this.mPackagesTokenData.counter);
            pw.println("Tokens Map Size: " + this.mPackagesTokenData.tokensToPackagesMap.size());
            if (!this.mPackagesTokenData.removedPackageTokens.isEmpty()) {
                pw.println("Removed Package Tokens: " + Arrays.toString(this.mPackagesTokenData.removedPackageTokens.toArray()));
            }
            for (int i = 0; i < this.mPackagesTokenData.tokensToPackagesMap.size(); i++) {
                int packageToken = this.mPackagesTokenData.tokensToPackagesMap.keyAt(i);
                String packageStrings = String.join(", ", this.mPackagesTokenData.tokensToPackagesMap.valueAt(i));
                pw.println("Token " + packageToken + ": [" + packageStrings + "]");
            }
            pw.println();
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IntervalStats readIntervalStatsForFile(int interval, long fileName) {
        IntervalStats stats;
        synchronized (this.mLock) {
            stats = new IntervalStats();
            try {
                readLocked((AtomicFile) this.mSortedStatFiles[interval].get(fileName, (Object) null), stats);
            } catch (Exception e) {
                return null;
            }
        }
        return stats;
    }
}
