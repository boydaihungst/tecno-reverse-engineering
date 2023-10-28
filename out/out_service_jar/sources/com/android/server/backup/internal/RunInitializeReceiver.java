package com.android.server.backup.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.UserBackupManagerService;
import java.util.Set;
/* loaded from: classes.dex */
public class RunInitializeReceiver extends BroadcastReceiver {
    private final UserBackupManagerService mUserBackupManagerService;

    public RunInitializeReceiver(UserBackupManagerService userBackupManagerService) {
        this.mUserBackupManagerService = userBackupManagerService;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (!UserBackupManagerService.RUN_INITIALIZE_ACTION.equals(intent.getAction())) {
            return;
        }
        synchronized (this.mUserBackupManagerService.getQueueLock()) {
            Set<String> pendingInits = this.mUserBackupManagerService.getPendingInits();
            Slog.v(BackupManagerService.TAG, "Running a device init; " + pendingInits.size() + " pending");
            if (pendingInits.size() > 0) {
                String[] transports = (String[]) pendingInits.toArray(new String[pendingInits.size()]);
                this.mUserBackupManagerService.clearPendingInits();
                this.mUserBackupManagerService.initializeTransports(transports, null);
            }
        }
    }
}
