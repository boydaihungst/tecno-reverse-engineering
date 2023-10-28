package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.UserBackupManagerService;
import java.io.IOException;
/* loaded from: classes.dex */
class RestoreFileRunnable implements Runnable {
    private final IBackupAgent mAgent;
    private final UserBackupManagerService mBackupManagerService;
    private final FileMetadata mInfo;
    private final ParcelFileDescriptor mSocket;
    private final int mToken;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RestoreFileRunnable(UserBackupManagerService backupManagerService, IBackupAgent agent, FileMetadata info, ParcelFileDescriptor socket, int token) throws IOException {
        this.mAgent = agent;
        this.mInfo = info;
        this.mToken = token;
        this.mSocket = ParcelFileDescriptor.dup(socket.getFileDescriptor());
        this.mBackupManagerService = backupManagerService;
    }

    @Override // java.lang.Runnable
    public void run() {
        try {
            this.mAgent.doRestoreFile(this.mSocket, this.mInfo.size, this.mInfo.type, this.mInfo.domain, this.mInfo.path, this.mInfo.mode, this.mInfo.mtime, this.mToken, this.mBackupManagerService.getBackupManagerBinder());
        } catch (RemoteException e) {
        }
    }
}
