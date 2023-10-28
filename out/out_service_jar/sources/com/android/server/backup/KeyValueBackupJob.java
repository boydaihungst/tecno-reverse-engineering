package com.android.server.backup;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import com.android.server.pm.PackageManagerService;
import java.util.Random;
/* loaded from: classes.dex */
public class KeyValueBackupJob extends JobService {
    private static final long MAX_DEFERRAL = 86400000;
    public static final int MAX_JOB_ID = 52418896;
    public static final int MIN_JOB_ID = 52417896;
    private static final String TAG = "KeyValueBackupJob";
    private static final String USER_ID_EXTRA_KEY = "userId";
    private static ComponentName sKeyValueJobService = new ComponentName(PackageManagerService.PLATFORM_PACKAGE_NAME, KeyValueBackupJob.class.getName());
    private static final SparseBooleanArray sScheduledForUserId = new SparseBooleanArray();
    private static final SparseLongArray sNextScheduledForUserId = new SparseLongArray();

    public static void schedule(int userId, Context ctx, BackupManagerConstants constants) {
        schedule(userId, ctx, 0L, constants);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [112=4] */
    public static void schedule(int userId, Context ctx, long delay, BackupManagerConstants constants) {
        long interval;
        long fuzz;
        int networkType;
        boolean needsCharging;
        synchronized (KeyValueBackupJob.class) {
            try {
                try {
                    SparseBooleanArray sparseBooleanArray = sScheduledForUserId;
                    if (sparseBooleanArray.get(userId)) {
                        return;
                    }
                    synchronized (constants) {
                        try {
                            interval = constants.getKeyValueBackupIntervalMilliseconds();
                            fuzz = constants.getKeyValueBackupFuzzMilliseconds();
                            networkType = constants.getKeyValueBackupRequiredNetworkType();
                            needsCharging = constants.getKeyValueBackupRequireCharging();
                        } catch (Throwable th) {
                            th = th;
                            while (true) {
                                try {
                                    try {
                                        break;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                            throw th;
                        }
                    }
                    long delay2 = delay <= 0 ? new Random().nextInt((int) fuzz) + interval : delay;
                    try {
                        Slog.v(TAG, "Scheduling k/v pass in " + ((delay2 / 1000) / 60) + " minutes");
                        JobInfo.Builder builder = new JobInfo.Builder(getJobIdForUserId(userId), sKeyValueJobService).setMinimumLatency(delay2).setRequiredNetworkType(networkType).setRequiresCharging(needsCharging).setOverrideDeadline(86400000L);
                        Bundle extraInfo = new Bundle();
                        extraInfo.putInt("userId", userId);
                        builder.setTransientExtras(extraInfo);
                        JobScheduler js = (JobScheduler) ctx.getSystemService("jobscheduler");
                        js.schedule(builder.build());
                        sparseBooleanArray.put(userId, true);
                        SparseLongArray sparseLongArray = sNextScheduledForUserId;
                        long interval2 = System.currentTimeMillis() + delay2;
                        sparseLongArray.put(userId, interval2);
                    } catch (Throwable th4) {
                        th = th4;
                        throw th;
                    }
                } catch (Throwable th5) {
                    th = th5;
                }
            } catch (Throwable th6) {
                th = th6;
            }
        }
    }

    public static void cancel(int userId, Context ctx) {
        synchronized (KeyValueBackupJob.class) {
            JobScheduler js = (JobScheduler) ctx.getSystemService("jobscheduler");
            js.cancel(getJobIdForUserId(userId));
            clearScheduledForUserId(userId);
        }
    }

    public static long nextScheduled(int userId) {
        long j;
        synchronized (KeyValueBackupJob.class) {
            j = sNextScheduledForUserId.get(userId);
        }
        return j;
    }

    public static boolean isScheduled(int userId) {
        boolean z;
        synchronized (KeyValueBackupJob.class) {
            z = sScheduledForUserId.get(userId);
        }
        return z;
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        int userId = params.getTransientExtras().getInt("userId");
        synchronized (KeyValueBackupJob.class) {
            clearScheduledForUserId(userId);
        }
        BackupManagerService service = BackupManagerService.getInstance();
        try {
            service.backupNowForUser(userId);
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private static void clearScheduledForUserId(int userId) {
        sScheduledForUserId.delete(userId);
        sNextScheduledForUserId.delete(userId);
    }

    private static int getJobIdForUserId(int userId) {
        return JobIdManager.getJobIdForUserId(MIN_JOB_ID, 52418896, userId);
    }
}
