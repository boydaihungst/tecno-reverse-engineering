package com.android.server.backup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
/* loaded from: classes.dex */
public interface OperationStorage {

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface OpState {
        public static final int ACKNOWLEDGED = 1;
        public static final int PENDING = 0;
        public static final int TIMEOUT = -1;
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface OpType {
        public static final int BACKUP = 2;
        public static final int BACKUP_WAIT = 0;
        public static final int RESTORE_WAIT = 1;
    }

    boolean isBackupOperationInProgress();

    int numOperations();

    Set<Integer> operationTokensForOpState(int i);

    Set<Integer> operationTokensForOpType(int i);

    Set<Integer> operationTokensForPackage(String str);

    void registerOperation(int i, int i2, BackupRestoreTask backupRestoreTask, int i3);

    void registerOperationForPackages(int i, int i2, Set<String> set, BackupRestoreTask backupRestoreTask, int i3);

    void removeOperation(int i);
}
