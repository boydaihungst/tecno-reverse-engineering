package com.android.server.backup.keyvalue;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.EventLogTags;
import com.android.server.backup.DataChangedJournal;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.remote.RemoteResult;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.backup.utils.BackupObserverUtils;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
/* loaded from: classes.dex */
public class KeyValueBackupReporter {
    private static final boolean DEBUG = true;
    static final boolean MORE_DEBUG = false;
    static final String TAG = "KeyValueBackupTask";
    private final UserBackupManagerService mBackupManagerService;
    private IBackupManagerMonitor mMonitor;
    private final IBackupObserver mObserver;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void onNewThread(String threadName) {
        Slog.d(TAG, "Spinning thread " + threadName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public KeyValueBackupReporter(UserBackupManagerService backupManagerService, IBackupObserver observer, IBackupManagerMonitor monitor) {
        this.mBackupManagerService = backupManagerService;
        this.mObserver = observer;
        this.mMonitor = monitor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBackupManagerMonitor getMonitor() {
        return this.mMonitor;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IBackupObserver getObserver() {
        return this.mObserver;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSkipBackup() {
        Slog.d(TAG, "Skipping backup since one is already in progress");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onEmptyQueueAtStart() {
        Slog.w(TAG, "Backup begun with an empty queue, nothing to do");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onQueueReady(List<String> queue) {
        Slog.v(TAG, "Beginning backup of " + queue.size() + " targets");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTransportReady(String transportName) {
        EventLog.writeEvent((int) EventLogTags.BACKUP_START, transportName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onInitializeTransport(String transportName) {
        Slog.i(TAG, "Initializing transport and resetting backup state");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTransportInitialized(int status) {
        if (status == 0) {
            EventLog.writeEvent((int) EventLogTags.BACKUP_INITIALIZE, new Object[0]);
            return;
        }
        EventLog.writeEvent((int) EventLogTags.BACKUP_TRANSPORT_FAILURE, "(initialize)");
        Slog.e(TAG, "Transport error in initializeDevice()");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onInitializeTransportError(Exception e) {
        Slog.e(TAG, "Error during initialization", e);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSkipPm() {
        Slog.d(TAG, "Skipping backup of PM metadata");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onExtractPmAgentDataError(Exception e) {
        Slog.e(TAG, "Error during PM metadata backup", e);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onStartPackageBackup(String packageName) {
        Slog.d(TAG, "Starting key-value backup of " + packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageNotEligibleForBackup(String packageName) {
        Slog.i(TAG, "Package " + packageName + " no longer supports backup, skipping");
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -2001);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageEligibleForFullBackup(String packageName) {
        Slog.i(TAG, "Package " + packageName + " performs full-backup rather than key-value, skipping");
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -2001);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageStopped(String packageName) {
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -2001);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAgentUnknown(String packageName) {
        Slog.d(TAG, "Package does not exist, skipping");
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -2002);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBindAgentError(String packageName, SecurityException e) {
        Slog.d(TAG, "Error in bind/backup", e);
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -1003);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAgentError(String packageName) {
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -1003);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onExtractAgentData(String packageName) {
        Slog.d(TAG, "Invoking agent on " + packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAgentFilesReady(File backupDataFile) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onRestoreconFailed(File backupDataFile) {
        Slog.e(TAG, "SELinux restorecon failed on " + backupDataFile);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCallAgentDoBackupError(String packageName, boolean callingAgent, Exception e) {
        if (callingAgent) {
            Slog.e(TAG, "Error invoking agent on " + packageName + ": " + e);
            BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -1003);
        } else {
            Slog.e(TAG, "Error before invoking agent on " + packageName + ": " + e);
        }
        EventLog.writeEvent((int) EventLogTags.BACKUP_AGENT_FAILURE, packageName, e.toString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onFailAgentError(String packageName) {
        Slog.w(TAG, "Error conveying failure to " + packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAgentIllegalKey(PackageInfo packageInfo, String key) {
        String packageName = packageInfo.packageName;
        EventLog.writeEvent((int) EventLogTags.BACKUP_AGENT_FAILURE, packageName, "bad key");
        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 5, packageInfo, 3, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_ILLEGAL_KEY", key));
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -1003);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAgentDataError(String packageName, IOException e) {
        Slog.w(TAG, "Unable to read/write agent data for " + packageName + ": " + e);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onDigestError(NoSuchAlgorithmException e) {
        Slog.e(TAG, "Unable to use SHA-1!");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onWriteWidgetData(boolean priorStateExists, byte[] widgetState) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTransportPerformBackup(String packageName) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onEmptyData(PackageInfo packageInfo) {
        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 7, packageInfo, 3, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageBackupComplete(String packageName, long size) {
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, 0);
        EventLog.writeEvent((int) EventLogTags.BACKUP_PACKAGE, packageName, Long.valueOf(size));
        this.mBackupManagerService.logBackupComplete(packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageBackupRejected(String packageName) {
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS);
        EventLogTags.writeBackupAgentFailure(packageName, "Transport rejected");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageBackupQuotaExceeded(String packageName) {
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -1005);
        EventLog.writeEvent((int) EventLogTags.BACKUP_QUOTA_EXCEEDED, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAgentDoQuotaExceededError(Exception e) {
        Slog.e(TAG, "Unable to notify about quota exceeded: " + e);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageBackupNonIncrementalRequired(PackageInfo packageInfo) {
        Slog.i(TAG, "Transport lost data, retrying package");
        BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 51, packageInfo, 1, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageBackupNonIncrementalAndNonIncrementalRequired(String packageName) {
        Slog.e(TAG, "Transport requested non-incremental but already the case");
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -1000);
        EventLog.writeEvent((int) EventLogTags.BACKUP_TRANSPORT_FAILURE, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageBackupTransportFailure(String packageName) {
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -1000);
        EventLog.writeEvent((int) EventLogTags.BACKUP_TRANSPORT_FAILURE, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageBackupTransportError(String packageName, Exception e) {
        Slog.e(TAG, "Transport error backing up " + packageName, e);
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -1000);
        EventLog.writeEvent((int) EventLogTags.BACKUP_TRANSPORT_FAILURE, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCloseFileDescriptorError(String logName) {
        Slog.w(TAG, "Error closing " + logName + " file-descriptor");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onCancel() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAgentTimedOut(PackageInfo packageInfo) {
        String packageName = getPackageName(packageInfo);
        Slog.i(TAG, "Agent " + packageName + " timed out");
        EventLog.writeEvent((int) EventLogTags.BACKUP_AGENT_FAILURE, packageName);
        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 21, packageInfo, 2, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_CANCEL_ALL", false));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAgentCancelled(PackageInfo packageInfo) {
        String packageName = getPackageName(packageInfo);
        Slog.i(TAG, "Cancel backing up " + packageName);
        EventLog.writeEvent((int) EventLogTags.BACKUP_AGENT_FAILURE, packageName);
        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 21, packageInfo, 2, BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_CANCEL_ALL", true));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAgentResultError(PackageInfo packageInfo) {
        String packageName = getPackageName(packageInfo);
        BackupObserverUtils.sendBackupOnPackageResult(this.mObserver, packageName, -1003);
        EventLog.writeEvent((int) EventLogTags.BACKUP_AGENT_FAILURE, packageName, "result error");
        Slog.w(TAG, "Agent " + packageName + " error in onBackup()");
    }

    private String getPackageName(PackageInfo packageInfo) {
        return packageInfo != null ? packageInfo.packageName : "no_package_yet";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onRevertTask() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTransportRequestBackupTimeError(Exception e) {
        Slog.w(TAG, "Unable to contact transport for recommended backoff: " + e);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onRemoteCallReturned(RemoteResult result, String logIdentifier) {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onJournalDeleteFailed(DataChangedJournal journal) {
        Slog.e(TAG, "Unable to remove backup journal file " + journal);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onSetCurrentTokenError(Exception e) {
        Slog.e(TAG, "Transport threw reporting restore set: " + e);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTransportNotInitialized(String transportName) {
        EventLog.writeEvent((int) EventLogTags.BACKUP_RESET, transportName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPendingInitializeTransportError(Exception e) {
        Slog.w(TAG, "Failed to query transport name for pending init: " + e);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBackupFinished(int status) {
        BackupObserverUtils.sendBackupFinished(this.mObserver, status);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onStartFullBackup(List<String> pendingFullBackups) {
        Slog.d(TAG, "Starting full backups for: " + pendingFullBackups);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onTaskFinished() {
        Slog.i(TAG, "K/V backup pass finished");
    }
}
