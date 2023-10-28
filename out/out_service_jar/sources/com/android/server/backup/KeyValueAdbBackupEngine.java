package com.android.server.backup;

import android.app.IBackupAgent;
import android.app.backup.FullBackup;
import android.app.backup.FullBackupDataOutput;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SELinux;
import android.util.Slog;
import com.android.server.backup.fullbackup.AppMetadataBackupWriter;
import com.android.server.backup.remote.ServiceBackupCallback;
import com.android.server.backup.utils.FullBackupUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public class KeyValueAdbBackupEngine {
    private static final String BACKUP_KEY_VALUE_BACKUP_DATA_FILENAME_SUFFIX = ".data";
    private static final String BACKUP_KEY_VALUE_BLANK_STATE_FILENAME = "blank_state";
    private static final String BACKUP_KEY_VALUE_DIRECTORY_NAME = "key_value_dir";
    private static final String BACKUP_KEY_VALUE_NEW_STATE_FILENAME_SUFFIX = ".new";
    private static final boolean DEBUG = false;
    private static final String TAG = "KeyValueAdbBackupEngine";
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private ParcelFileDescriptor mBackupData;
    private final File mBackupDataName;
    private UserBackupManagerService mBackupManagerService;
    private final File mBlankStateName;
    private final PackageInfo mCurrentPackage;
    private final File mDataDir;
    private final File mManifestFile;
    private ParcelFileDescriptor mNewState;
    private final File mNewStateName;
    private final OutputStream mOutput;
    private final PackageManager mPackageManager;
    private ParcelFileDescriptor mSavedState;
    private final File mStateDir;

    public KeyValueAdbBackupEngine(OutputStream output, PackageInfo packageInfo, UserBackupManagerService backupManagerService, PackageManager packageManager, File baseStateDir, File dataDir) {
        this.mOutput = output;
        this.mCurrentPackage = packageInfo;
        this.mBackupManagerService = backupManagerService;
        this.mPackageManager = packageManager;
        this.mDataDir = dataDir;
        File file = new File(baseStateDir, BACKUP_KEY_VALUE_DIRECTORY_NAME);
        this.mStateDir = file;
        file.mkdirs();
        String pkg = packageInfo.packageName;
        this.mBlankStateName = new File(file, BACKUP_KEY_VALUE_BLANK_STATE_FILENAME);
        this.mBackupDataName = new File(dataDir, pkg + ".data");
        this.mNewStateName = new File(file, pkg + ".new");
        this.mManifestFile = new File(dataDir, UserBackupManagerService.BACKUP_MANIFEST_FILENAME);
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Objects.requireNonNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [125=4] */
    public void backupOnePackage() throws IOException {
        IBackupAgent agent;
        ApplicationInfo targetApp = this.mCurrentPackage.applicationInfo;
        try {
            try {
                prepareBackupFiles(this.mCurrentPackage.packageName);
                agent = bindToAgent(targetApp);
            } catch (FileNotFoundException e) {
                Slog.e(TAG, "Failed creating files for package " + this.mCurrentPackage.packageName + " will ignore package. " + e);
            }
            if (agent == null) {
                Slog.e(TAG, "Failed binding to BackupAgent for package " + this.mCurrentPackage.packageName);
            } else if (invokeAgentForAdbBackup(this.mCurrentPackage.packageName, agent)) {
                writeBackupData();
            } else {
                Slog.e(TAG, "Backup Failed for package " + this.mCurrentPackage.packageName);
            }
        } finally {
            cleanup();
        }
    }

    private void prepareBackupFiles(String packageName) throws FileNotFoundException {
        this.mSavedState = ParcelFileDescriptor.open(this.mBlankStateName, AudioFormat.MP2);
        this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
        if (!SELinux.restorecon(this.mBackupDataName)) {
            Slog.e(TAG, "SELinux restorecon failed on " + this.mBackupDataName);
        }
        this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
    }

    private IBackupAgent bindToAgent(ApplicationInfo targetApp) {
        try {
            return this.mBackupManagerService.bindToAgentSynchronous(targetApp, 0, 0);
        } catch (SecurityException e) {
            Slog.e(TAG, "error in binding to agent for package " + targetApp.packageName + ". " + e);
            return null;
        }
    }

    private boolean invokeAgentForAdbBackup(String packageName, IBackupAgent agent) {
        int token = this.mBackupManagerService.generateRandomIntegerToken();
        long kvBackupAgentTimeoutMillis = this.mAgentTimeoutParameters.getKvBackupAgentTimeoutMillis();
        try {
            this.mBackupManagerService.prepareOperationTimeout(token, kvBackupAgentTimeoutMillis, null, 0);
            agent.doBackup(this.mSavedState, this.mBackupData, this.mNewState, (long) JobStatus.NO_LATEST_RUNTIME, new ServiceBackupCallback(this.mBackupManagerService.getBackupManagerBinder(), token), 0);
            if (!this.mBackupManagerService.waitUntilOperationComplete(token)) {
                Slog.e(TAG, "Key-value backup failed on package " + packageName);
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Slog.e(TAG, "Error invoking agent for backup on " + packageName + ". " + e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class KeyValueAdbBackupDataCopier implements Runnable {
        private final PackageInfo mPackage;
        private final ParcelFileDescriptor mPipe;
        private final int mToken;

        KeyValueAdbBackupDataCopier(PackageInfo pack, ParcelFileDescriptor pipe, int token) throws IOException {
            this.mPackage = pack;
            this.mPipe = ParcelFileDescriptor.dup(pipe.getFileDescriptor());
            this.mToken = token;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                try {
                    FullBackupDataOutput output = new FullBackupDataOutput(this.mPipe);
                    AppMetadataBackupWriter writer = new AppMetadataBackupWriter(output, KeyValueAdbBackupEngine.this.mPackageManager);
                    writer.backupManifest(this.mPackage, KeyValueAdbBackupEngine.this.mManifestFile, KeyValueAdbBackupEngine.this.mDataDir, "k", null, false);
                    KeyValueAdbBackupEngine.this.mManifestFile.delete();
                    FullBackup.backupToTar(this.mPackage.packageName, "k", (String) null, KeyValueAdbBackupEngine.this.mDataDir.getAbsolutePath(), KeyValueAdbBackupEngine.this.mBackupDataName.getAbsolutePath(), output);
                    try {
                        FileOutputStream out = new FileOutputStream(this.mPipe.getFileDescriptor());
                        byte[] buf = new byte[4];
                        out.write(buf);
                    } catch (IOException e) {
                        Slog.e(KeyValueAdbBackupEngine.TAG, "Unable to finalize backup stream!");
                    }
                    try {
                        KeyValueAdbBackupEngine.this.mBackupManagerService.getBackupManagerBinder().opComplete(this.mToken, 0L);
                    } catch (RemoteException e2) {
                    }
                } catch (IOException e3) {
                    Slog.e(KeyValueAdbBackupEngine.TAG, "Error running full backup for " + this.mPackage.packageName + ". " + e3);
                }
            } finally {
                IoUtils.closeQuietly(this.mPipe);
            }
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [293=4] */
    /* JADX WARN: Removed duplicated region for block: B:27:0x00bc  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void writeBackupData() throws IOException {
        ParcelFileDescriptor[] pipes;
        IOException e;
        ParcelFileDescriptor parcelFileDescriptor;
        int token = this.mBackupManagerService.generateRandomIntegerToken();
        long kvBackupAgentTimeoutMillis = this.mAgentTimeoutParameters.getKvBackupAgentTimeoutMillis();
        ParcelFileDescriptor[] pipes2 = null;
        try {
            pipes = ParcelFileDescriptor.createPipe();
            try {
                try {
                    this.mBackupManagerService.prepareOperationTimeout(token, kvBackupAgentTimeoutMillis, null, 0);
                    KeyValueAdbBackupDataCopier runner = new KeyValueAdbBackupDataCopier(this.mCurrentPackage, pipes[1], token);
                    pipes[1].close();
                    pipes[1] = null;
                    Thread t = new Thread(runner, "key-value-app-data-runner");
                    t.start();
                    FullBackupUtils.routeSocketDataToOutput(pipes[0], this.mOutput);
                    if (!this.mBackupManagerService.waitUntilOperationComplete(token)) {
                        Slog.e(TAG, "Full backup failed on package " + this.mCurrentPackage.packageName);
                    }
                    this.mOutput.flush();
                } catch (IOException e2) {
                    e = e2;
                    Slog.e(TAG, "Error backing up " + this.mCurrentPackage.packageName + ": " + e);
                    this.mOutput.flush();
                    if (pipes != null) {
                        IoUtils.closeQuietly(pipes[0]);
                        parcelFileDescriptor = pipes[1];
                        IoUtils.closeQuietly(parcelFileDescriptor);
                    }
                    return;
                }
            } catch (Throwable th) {
                th = th;
                pipes2 = pipes;
                this.mOutput.flush();
                if (pipes2 != null) {
                    IoUtils.closeQuietly(pipes2[0]);
                    IoUtils.closeQuietly(pipes2[1]);
                }
                throw th;
            }
        } catch (IOException e3) {
            pipes = null;
            e = e3;
        } catch (Throwable th2) {
            th = th2;
            this.mOutput.flush();
            if (pipes2 != null) {
            }
            throw th;
        }
        if (pipes != null) {
            IoUtils.closeQuietly(pipes[0]);
            parcelFileDescriptor = pipes[1];
            IoUtils.closeQuietly(parcelFileDescriptor);
        }
    }

    private void cleanup() {
        this.mBackupManagerService.tearDownAgentAndKill(this.mCurrentPackage.applicationInfo);
        this.mBlankStateName.delete();
        this.mNewStateName.delete();
        this.mBackupDataName.delete();
    }
}
