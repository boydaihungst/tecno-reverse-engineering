package com.android.server.storage;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageStats;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.UserHandle;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.util.Log;
import com.android.server.pm.PackageManagerService;
import com.android.server.storage.FileCollector;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public class DiskStatsLoggingService extends JobService {
    public static final String DUMPSYS_CACHE_PATH = "/data/system/diskstats_cache.json";
    private static final int JOB_DISKSTATS_LOGGING = 1145656139;
    private static final String TAG = "DiskStatsLogService";
    private static ComponentName sDiskStatsLoggingService = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, DiskStatsLoggingService.class.getName());

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        if (!isCharging(this) || !isDumpsysTaskEnabled(getContentResolver())) {
            jobFinished(params, true);
            return false;
        }
        VolumeInfo volume = getPackageManager().getPrimaryStorageCurrentVolume();
        if (volume == null) {
            return false;
        }
        AppCollector collector = new AppCollector(this, volume);
        int userId = UserHandle.myUserId();
        Environment.UserEnvironment environment = new Environment.UserEnvironment(userId);
        LogRunnable task = new LogRunnable();
        task.setDownloadsDirectory(environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        task.setSystemSize(FileCollector.getSystemSize(this));
        task.setLogOutputFile(new File(DUMPSYS_CACHE_PATH));
        task.setAppCollector(collector);
        task.setJobService(this, params);
        task.setContext(this);
        AsyncTask.execute(task);
        return true;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void schedule(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService("jobscheduler");
        js.schedule(new JobInfo.Builder(JOB_DISKSTATS_LOGGING, sDiskStatsLoggingService).setRequiresDeviceIdle(true).setRequiresCharging(true).setPeriodic(TimeUnit.DAYS.toMillis(1L)).build());
    }

    private static boolean isCharging(Context context) {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService("batterymanager");
        if (batteryManager != null) {
            return batteryManager.isCharging();
        }
        return false;
    }

    static boolean isDumpsysTaskEnabled(ContentResolver resolver) {
        return Settings.Global.getInt(resolver, "enable_diskstats_logging", 1) != 0;
    }

    /* loaded from: classes2.dex */
    static class LogRunnable implements Runnable {
        private static final long TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(10);
        private AppCollector mCollector;
        private Context mContext;
        private File mDownloadsDirectory;
        private JobService mJobService;
        private File mOutputFile;
        private JobParameters mParams;
        private long mSystemSize;

        LogRunnable() {
        }

        public void setDownloadsDirectory(File file) {
            this.mDownloadsDirectory = file;
        }

        public void setAppCollector(AppCollector collector) {
            this.mCollector = collector;
        }

        public void setLogOutputFile(File file) {
            this.mOutputFile = file;
        }

        public void setSystemSize(long size) {
            this.mSystemSize = size;
        }

        public void setContext(Context context) {
            this.mContext = context;
        }

        public void setJobService(JobService jobService, JobParameters params) {
            this.mJobService = jobService;
            this.mParams = params;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                FileCollector.MeasurementResult mainCategories = FileCollector.getMeasurementResult(this.mContext);
                FileCollector.MeasurementResult downloads = FileCollector.getMeasurementResult(this.mDownloadsDirectory);
                boolean needsReschedule = true;
                List<PackageStats> stats = this.mCollector.getPackageStats(TIMEOUT_MILLIS);
                if (stats == null) {
                    Log.w(DiskStatsLoggingService.TAG, "Timed out while fetching package stats.");
                } else {
                    logToFile(mainCategories, downloads, stats, this.mSystemSize);
                    needsReschedule = false;
                }
                finishJob(needsReschedule);
            } catch (IllegalStateException e) {
                Log.e(DiskStatsLoggingService.TAG, "Error while measuring storage", e);
                finishJob(true);
            }
        }

        private void logToFile(FileCollector.MeasurementResult mainCategories, FileCollector.MeasurementResult downloads, List<PackageStats> stats, long systemSize) {
            DiskStatsFileLogger logger = new DiskStatsFileLogger(mainCategories, downloads, stats, systemSize);
            try {
                this.mOutputFile.createNewFile();
                logger.dumpToFile(this.mOutputFile);
            } catch (IOException e) {
                Log.e(DiskStatsLoggingService.TAG, "Exception while writing opportunistic disk file cache.", e);
            }
        }

        private void finishJob(boolean needsReschedule) {
            JobService jobService = this.mJobService;
            if (jobService != null) {
                jobService.jobFinished(this.mParams, needsReschedule);
            }
        }
    }
}
