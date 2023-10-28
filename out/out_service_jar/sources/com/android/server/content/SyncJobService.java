package com.android.server.content;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;
import com.android.server.slice.SliceClientPermissions;
/* loaded from: classes.dex */
public class SyncJobService extends JobService {
    private static final String TAG = "SyncManager";
    private static SyncJobService sInstance;
    private static final Object sLock = new Object();
    private static final SparseArray<JobParameters> sJobParamsMap = new SparseArray<>();
    private static final SparseBooleanArray sStartedSyncs = new SparseBooleanArray();
    private static final SparseLongArray sJobStartUptimes = new SparseLongArray();
    private static final SyncLogger sLogger = SyncLogger.getInstance();

    private void updateInstance() {
        synchronized (SyncJobService.class) {
            sInstance = this;
        }
    }

    private static SyncJobService getInstance() {
        SyncJobService syncJobService;
        synchronized (sLock) {
            if (sInstance == null) {
                Slog.wtf("SyncManager", "sInstance == null");
            }
            syncJobService = sInstance;
        }
        return syncJobService;
    }

    public static boolean isReady() {
        boolean z;
        synchronized (sLock) {
            z = sInstance != null;
        }
        return z;
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        updateInstance();
        SyncLogger syncLogger = sLogger;
        syncLogger.purgeOldLogs();
        SyncOperation op = SyncOperation.maybeCreateFromJobExtras(params.getExtras());
        if (op == null) {
            Slog.wtf("SyncManager", "Got invalid job " + params.getJobId());
            return false;
        }
        boolean readyToSync = SyncManager.readyToSync(op.target.userId);
        syncLogger.log("onStartJob() jobid=", Integer.valueOf(params.getJobId()), " op=", op, " readyToSync", Boolean.valueOf(readyToSync));
        if (!readyToSync) {
            boolean wantsReschedule = !op.isPeriodic;
            jobFinished(params, wantsReschedule);
            return true;
        }
        boolean isLoggable = Log.isLoggable("SyncManager", 2);
        synchronized (sLock) {
            int jobId = params.getJobId();
            sJobParamsMap.put(jobId, params);
            sStartedSyncs.delete(jobId);
            sJobStartUptimes.put(jobId, SystemClock.uptimeMillis());
        }
        Message m = Message.obtain();
        m.what = 10;
        if (isLoggable) {
            Slog.v("SyncManager", "Got start job message " + op.target);
        }
        m.obj = op;
        SyncManager.sendMessage(m);
        return true;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        int i;
        int i2;
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "onStopJob called " + params.getJobId() + ", reason: " + params.getInternalStopReasonCode());
        }
        SyncOperation op = SyncOperation.maybeCreateFromJobExtras(params.getExtras());
        if (op == null) {
            Slog.wtf("SyncManager", "Got invalid job " + params.getJobId());
            return false;
        }
        boolean readyToSync = SyncManager.readyToSync(op.target.userId);
        SyncLogger syncLogger = sLogger;
        syncLogger.log("onStopJob() ", syncLogger.jobParametersToString(params), " readyToSync=", Boolean.valueOf(readyToSync));
        synchronized (sLock) {
            int jobId = params.getJobId();
            sJobParamsMap.remove(jobId);
            SparseLongArray sparseLongArray = sJobStartUptimes;
            long startUptime = sparseLongArray.get(jobId);
            long nowUptime = SystemClock.uptimeMillis();
            long runtime = nowUptime - startUptime;
            if (runtime > 60000 && readyToSync && !sStartedSyncs.get(jobId)) {
                wtf("Job " + jobId + " didn't start:  startUptime=" + startUptime + " nowUptime=" + nowUptime + " params=" + jobParametersToString(params));
            }
            sStartedSyncs.delete(jobId);
            sparseLongArray.delete(jobId);
        }
        Message m = Message.obtain();
        m.what = 11;
        m.obj = op;
        if (params.getInternalStopReasonCode() == 0) {
            i = 0;
        } else {
            i = 1;
        }
        m.arg1 = i;
        if (params.getInternalStopReasonCode() != 3) {
            i2 = 0;
        } else {
            i2 = 1;
        }
        m.arg2 = i2;
        SyncManager.sendMessage(m);
        return false;
    }

    public static void callJobFinished(int jobId, boolean needsReschedule, String why) {
        SyncJobService instance = getInstance();
        if (instance != null) {
            instance.callJobFinishedInner(jobId, needsReschedule, why);
        }
    }

    public void callJobFinishedInner(int jobId, boolean needsReschedule, String why) {
        synchronized (sLock) {
            SparseArray<JobParameters> sparseArray = sJobParamsMap;
            JobParameters params = sparseArray.get(jobId);
            SyncLogger syncLogger = sLogger;
            syncLogger.log("callJobFinished()", " jobid=", Integer.valueOf(jobId), " needsReschedule=", Boolean.valueOf(needsReschedule), " ", syncLogger.jobParametersToString(params), " why=", why);
            if (params != null) {
                jobFinished(params, needsReschedule);
                sparseArray.remove(jobId);
            } else {
                Slog.e("SyncManager", "Job params not found for " + String.valueOf(jobId));
            }
        }
    }

    public static void markSyncStarted(int jobId) {
        synchronized (sLock) {
            sStartedSyncs.put(jobId, true);
        }
    }

    public static String jobParametersToString(JobParameters params) {
        if (params == null) {
            return "job:null";
        }
        return "job:#" + params.getJobId() + ":sr=[" + params.getInternalStopReasonCode() + SliceClientPermissions.SliceAuthority.DELIMITER + params.getDebugStopReason() + "]:" + SyncOperation.maybeCreateFromJobExtras(params.getExtras());
    }

    private static void wtf(String message) {
        sLogger.log(message);
        Slog.wtf("SyncManager", message);
    }
}
