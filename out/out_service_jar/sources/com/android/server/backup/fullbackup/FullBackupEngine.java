package com.android.server.backup.fullbackup;

import android.app.IBackupAgent;
import android.app.backup.FullBackupDataOutput;
import android.app.backup.IBackupCallback;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.remote.RemoteCall;
import com.android.server.backup.remote.RemoteCallable;
import com.android.server.backup.utils.BackupEligibilityRules;
import com.android.server.backup.utils.FullBackupUtils;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
/* loaded from: classes.dex */
public class FullBackupEngine {
    private UserBackupManagerService backupManagerService;
    private IBackupAgent mAgent;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private final BackupEligibilityRules mBackupEligibilityRules;
    private boolean mIncludeApks;
    private final int mOpToken;
    private OutputStream mOutput;
    private PackageInfo mPkg;
    private FullBackupPreflight mPreflightHook;
    private final long mQuota;
    private BackupRestoreTask mTimeoutMonitor;
    private final int mTransportFlags;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class FullBackupRunner implements Runnable {
        private final IBackupAgent mAgent;
        private final File mFilesDir;
        private final boolean mIncludeApks;
        private final PackageInfo mPackage;
        private final PackageManager mPackageManager;
        private final ParcelFileDescriptor mPipe;
        private final int mToken;
        private final int mUserId;

        FullBackupRunner(UserBackupManagerService userBackupManagerService, PackageInfo packageInfo, IBackupAgent agent, ParcelFileDescriptor pipe, int token, boolean includeApks) throws IOException {
            this.mUserId = userBackupManagerService.getUserId();
            this.mPackageManager = FullBackupEngine.this.backupManagerService.getPackageManager();
            this.mPackage = packageInfo;
            this.mAgent = agent;
            this.mPipe = ParcelFileDescriptor.dup(pipe.getFileDescriptor());
            this.mToken = token;
            this.mIncludeApks = includeApks;
            this.mFilesDir = userBackupManagerService.getDataDir();
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [166=4] */
        @Override // java.lang.Runnable
        public void run() {
            try {
                try {
                    try {
                        FullBackupDataOutput output = new FullBackupDataOutput(this.mPipe, -1L, FullBackupEngine.this.mTransportFlags);
                        AppMetadataBackupWriter appMetadataBackupWriter = new AppMetadataBackupWriter(output, this.mPackageManager);
                        String packageName = this.mPackage.packageName;
                        boolean isSharedStorage = UserBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE.equals(packageName);
                        boolean writeApk = shouldWriteApk(this.mPackage.applicationInfo, this.mIncludeApks, isSharedStorage);
                        if (!isSharedStorage) {
                            File manifestFile = new File(this.mFilesDir, UserBackupManagerService.BACKUP_MANIFEST_FILENAME);
                            appMetadataBackupWriter.backupManifest(this.mPackage, manifestFile, this.mFilesDir, writeApk);
                            manifestFile.delete();
                            byte[] widgetData = AppWidgetBackupBridge.getWidgetState(packageName, this.mUserId);
                            if (widgetData != null && widgetData.length > 0) {
                                File metadataFile = new File(this.mFilesDir, UserBackupManagerService.BACKUP_METADATA_FILENAME);
                                appMetadataBackupWriter.backupWidget(this.mPackage, metadataFile, this.mFilesDir, widgetData);
                                metadataFile.delete();
                            }
                        }
                        if (writeApk) {
                            appMetadataBackupWriter.backupApk(this.mPackage);
                            appMetadataBackupWriter.backupObb(this.mUserId, this.mPackage);
                        }
                        Slog.d(BackupManagerService.TAG, "Calling doFullBackup() on " + packageName);
                        long timeout = isSharedStorage ? FullBackupEngine.this.mAgentTimeoutParameters.getSharedBackupAgentTimeoutMillis() : FullBackupEngine.this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
                        FullBackupEngine.this.backupManagerService.prepareOperationTimeout(this.mToken, timeout, FullBackupEngine.this.mTimeoutMonitor, 0);
                        this.mAgent.doFullBackup(this.mPipe, FullBackupEngine.this.mQuota, this.mToken, FullBackupEngine.this.backupManagerService.getBackupManagerBinder(), FullBackupEngine.this.mTransportFlags);
                        this.mPipe.close();
                    } catch (IOException e) {
                    }
                } catch (RemoteException e2) {
                    Slog.e(BackupManagerService.TAG, "Remote agent vanished during full backup of " + this.mPackage.packageName, e2);
                    this.mPipe.close();
                } catch (IOException e3) {
                    Slog.e(BackupManagerService.TAG, "Error running full backup for " + this.mPackage.packageName, e3);
                    this.mPipe.close();
                }
            } catch (Throwable th) {
                try {
                    this.mPipe.close();
                } catch (IOException e4) {
                }
                throw th;
            }
        }

        private boolean shouldWriteApk(ApplicationInfo applicationInfo, boolean includeApks, boolean isSharedStorage) {
            boolean isSystemApp = (applicationInfo.flags & 1) != 0;
            boolean isUpdatedSystemApp = (applicationInfo.flags & 128) != 0;
            return includeApks && !isSharedStorage && (!isSystemApp || isUpdatedSystemApp);
        }
    }

    public FullBackupEngine(UserBackupManagerService backupManagerService, OutputStream output, FullBackupPreflight preflightHook, PackageInfo pkg, boolean alsoApks, BackupRestoreTask timeoutMonitor, long quota, int opToken, int transportFlags, BackupEligibilityRules backupEligibilityRules) {
        this.backupManagerService = backupManagerService;
        this.mOutput = output;
        this.mPreflightHook = preflightHook;
        this.mPkg = pkg;
        this.mIncludeApks = alsoApks;
        this.mTimeoutMonitor = timeoutMonitor;
        this.mQuota = quota;
        this.mOpToken = opToken;
        this.mTransportFlags = transportFlags;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Objects.requireNonNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        this.mBackupEligibilityRules = backupEligibilityRules;
    }

    public int preflightCheck() throws RemoteException {
        if (this.mPreflightHook == null) {
            return 0;
        }
        if (initializeAgent()) {
            int result = this.mPreflightHook.preflightFullBackup(this.mPkg, this.mAgent);
            return result;
        }
        Slog.w(BackupManagerService.TAG, "Unable to bind to full agent for " + this.mPkg.packageName);
        return -1003;
    }

    public int backupOnePackage() throws RemoteException {
        int result = -1003;
        if (initializeAgent()) {
            ParcelFileDescriptor[] pipes = null;
            try {
                try {
                    try {
                        pipes = ParcelFileDescriptor.createPipe();
                        FullBackupRunner runner = new FullBackupRunner(this.backupManagerService, this.mPkg, this.mAgent, pipes[1], this.mOpToken, this.mIncludeApks);
                        pipes[1].close();
                        pipes[1] = null;
                        Thread t = new Thread(runner, "app-data-runner");
                        t.start();
                        FullBackupUtils.routeSocketDataToOutput(pipes[0], this.mOutput);
                        if (!this.backupManagerService.waitUntilOperationComplete(this.mOpToken)) {
                            Slog.e(BackupManagerService.TAG, "Full backup failed on package " + this.mPkg.packageName);
                        } else {
                            result = 0;
                        }
                        this.mOutput.flush();
                        if (pipes != null) {
                            if (pipes[0] != null) {
                                pipes[0].close();
                            }
                            if (pipes[1] != null) {
                                pipes[1].close();
                            }
                        }
                    } catch (Throwable th) {
                        try {
                            this.mOutput.flush();
                            if (pipes != null) {
                                if (pipes[0] != null) {
                                    pipes[0].close();
                                }
                                if (pipes[1] != null) {
                                    pipes[1].close();
                                }
                            }
                        } catch (IOException e) {
                            Slog.w(BackupManagerService.TAG, "Error bringing down backup stack");
                        }
                        throw th;
                    }
                } catch (IOException e2) {
                    Slog.e(BackupManagerService.TAG, "Error backing up " + this.mPkg.packageName + ": " + e2.getMessage());
                    result = -1003;
                    this.mOutput.flush();
                    if (pipes != null) {
                        if (pipes[0] != null) {
                            pipes[0].close();
                        }
                        if (pipes[1] != null) {
                            pipes[1].close();
                        }
                    }
                }
            } catch (IOException e3) {
                Slog.w(BackupManagerService.TAG, "Error bringing down backup stack");
                result = -1000;
            }
        } else {
            Slog.w(BackupManagerService.TAG, "Unable to bind to full agent for " + this.mPkg.packageName);
        }
        tearDown();
        return result;
    }

    public void sendQuotaExceeded(final long backupDataBytes, final long quotaBytes) {
        if (initializeAgent()) {
            try {
                RemoteCall.execute(new RemoteCallable() { // from class: com.android.server.backup.fullbackup.FullBackupEngine$$ExternalSyntheticLambda0
                    @Override // com.android.server.backup.remote.RemoteCallable
                    public final void call(Object obj) {
                        FullBackupEngine.this.m2216xb400cdd2(backupDataBytes, quotaBytes, (IBackupCallback) obj);
                    }
                }, this.mAgentTimeoutParameters.getQuotaExceededTimeoutMillis());
            } catch (RemoteException e) {
                Slog.e(BackupManagerService.TAG, "Remote exception while telling agent about quota exceeded");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendQuotaExceeded$0$com-android-server-backup-fullbackup-FullBackupEngine  reason: not valid java name */
    public /* synthetic */ void m2216xb400cdd2(long backupDataBytes, long quotaBytes, IBackupCallback callback) throws RemoteException {
        this.mAgent.doQuotaExceeded(backupDataBytes, quotaBytes, callback);
    }

    private boolean initializeAgent() {
        if (this.mAgent == null) {
            this.mAgent = this.backupManagerService.bindToAgentSynchronous(this.mPkg.applicationInfo, 1, this.mBackupEligibilityRules.getOperationType());
        }
        return this.mAgent != null;
    }

    private void tearDown() {
        PackageInfo packageInfo = this.mPkg;
        if (packageInfo != null) {
            this.backupManagerService.tearDownAgentAndKill(packageInfo.applicationInfo);
        }
    }
}
