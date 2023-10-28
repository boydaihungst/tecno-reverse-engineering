package com.android.server.backup.restore;

import android.util.Slog;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.OperationStorage;
import com.android.server.backup.UserBackupManagerService;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class AdbRestoreFinishedLatch implements BackupRestoreTask {
    private static final String TAG = "AdbRestoreFinishedLatch";
    private UserBackupManagerService backupManagerService;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private final int mCurrentOpToken;
    final CountDownLatch mLatch = new CountDownLatch(1);
    private final OperationStorage mOperationStorage;

    public AdbRestoreFinishedLatch(UserBackupManagerService backupManagerService, OperationStorage operationStorage, int currentOpToken) {
        this.backupManagerService = backupManagerService;
        this.mOperationStorage = operationStorage;
        this.mCurrentOpToken = currentOpToken;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Objects.requireNonNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void await() {
        long fullBackupAgentTimeoutMillis = this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
        try {
            this.mLatch.await(fullBackupAgentTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Slog.w(TAG, "Interrupted!");
        }
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void execute() {
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void operationComplete(long result) {
        this.mLatch.countDown();
        this.mOperationStorage.removeOperation(this.mCurrentOpToken);
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void handleCancel(boolean cancelAll) {
        Slog.w(TAG, "adb onRestoreFinished() timed out");
        this.mLatch.countDown();
        this.mOperationStorage.removeOperation(this.mCurrentOpToken);
    }
}
