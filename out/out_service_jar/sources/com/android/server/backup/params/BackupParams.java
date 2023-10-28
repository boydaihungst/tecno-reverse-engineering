package com.android.server.backup.params;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportConnection;
import com.android.server.backup.utils.BackupEligibilityRules;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class BackupParams {
    public String dirName;
    public ArrayList<String> fullPackages;
    public ArrayList<String> kvPackages;
    public OnTaskFinishedListener listener;
    public BackupEligibilityRules mBackupEligibilityRules;
    public TransportConnection mTransportConnection;
    public IBackupManagerMonitor monitor;
    public boolean nonIncrementalBackup;
    public IBackupObserver observer;
    public boolean userInitiated;

    public BackupParams(TransportConnection transportConnection, String dirName, ArrayList<String> kvPackages, ArrayList<String> fullPackages, IBackupObserver observer, IBackupManagerMonitor monitor, OnTaskFinishedListener listener, boolean userInitiated, boolean nonIncrementalBackup, BackupEligibilityRules backupEligibilityRules) {
        this.mTransportConnection = transportConnection;
        this.dirName = dirName;
        this.kvPackages = kvPackages;
        this.fullPackages = fullPackages;
        this.observer = observer;
        this.monitor = monitor;
        this.listener = listener;
        this.userInitiated = userInitiated;
        this.nonIncrementalBackup = nonIncrementalBackup;
        this.mBackupEligibilityRules = backupEligibilityRules;
    }
}
