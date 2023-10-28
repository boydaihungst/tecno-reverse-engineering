package com.android.server.backup;

import android.app.IBackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FullBackup;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.backup.keyvalue.KeyValueBackupTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public class KeyValueAdbRestoreEngine implements Runnable {
    private static final boolean DEBUG = false;
    private static final String TAG = "KeyValueAdbRestoreEngine";
    private final IBackupAgent mAgent;
    private final UserBackupManagerService mBackupManagerService;
    private final File mDataDir;
    private final ParcelFileDescriptor mInFD;
    private final FileMetadata mInfo;
    private final int mToken;

    public KeyValueAdbRestoreEngine(UserBackupManagerService backupManagerService, File dataDir, FileMetadata info, ParcelFileDescriptor inFD, IBackupAgent agent, int token) {
        this.mBackupManagerService = backupManagerService;
        this.mDataDir = dataDir;
        this.mInfo = info;
        this.mInFD = inFD;
        this.mAgent = agent;
        this.mToken = token;
    }

    @Override // java.lang.Runnable
    public void run() {
        try {
            File restoreData = prepareRestoreData(this.mInfo, this.mInFD);
            invokeAgentForAdbRestore(this.mAgent, this.mInfo, restoreData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File prepareRestoreData(FileMetadata info, ParcelFileDescriptor inFD) throws IOException {
        String pkg = info.packageName;
        File restoreDataName = new File(this.mDataDir, pkg + ".restore");
        File sortedDataName = new File(this.mDataDir, pkg + ".sorted");
        FullBackup.restoreFile(inFD, info.size, info.type, info.mode, info.mtime, restoreDataName);
        sortKeyValueData(restoreDataName, sortedDataName);
        return sortedDataName;
    }

    private void invokeAgentForAdbRestore(IBackupAgent agent, FileMetadata info, File restoreData) throws IOException {
        String pkg = info.packageName;
        File newStateName = new File(this.mDataDir, pkg + KeyValueBackupTask.NEW_STATE_FILE_SUFFIX);
        try {
            ParcelFileDescriptor backupData = ParcelFileDescriptor.open(restoreData, 268435456);
            ParcelFileDescriptor newState = ParcelFileDescriptor.open(newStateName, 1006632960);
            agent.doRestore(backupData, info.version, newState, this.mToken, this.mBackupManagerService.getBackupManagerBinder());
        } catch (RemoteException e) {
            Slog.e(TAG, "Exception calling doRestore on agent: " + e);
        } catch (IOException e2) {
            Slog.e(TAG, "Exception opening file. " + e2);
        }
    }

    private void sortKeyValueData(File restoreData, File sortedData) throws IOException {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(restoreData);
            outputStream = new FileOutputStream(sortedData);
            BackupDataInput reader = new BackupDataInput(inputStream.getFD());
            BackupDataOutput writer = new BackupDataOutput(outputStream.getFD());
            copyKeysInLexicalOrder(reader, writer);
            IoUtils.closeQuietly(inputStream);
            IoUtils.closeQuietly(outputStream);
        } catch (Throwable th) {
            if (inputStream != null) {
                IoUtils.closeQuietly(inputStream);
            }
            if (outputStream != null) {
                IoUtils.closeQuietly(outputStream);
            }
            throw th;
        }
    }

    private void copyKeysInLexicalOrder(BackupDataInput in, BackupDataOutput out) throws IOException {
        Map<String, byte[]> data = new HashMap<>();
        while (in.readNextHeader()) {
            String key = in.getKey();
            int size = in.getDataSize();
            if (size < 0) {
                in.skipEntityData();
            } else {
                byte[] value = new byte[size];
                in.readEntityData(value, 0, size);
                data.put(key, value);
            }
        }
        List<String> keys = new ArrayList<>(data.keySet());
        Collections.sort(keys);
        for (String key2 : keys) {
            byte[] value2 = data.get(key2);
            out.writeEntityHeader(key2, value2.length);
            out.writeEntityData(value2, value2.length);
        }
    }
}
