package com.android.server.backup.params;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.content.pm.PackageInfo;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportConnection;
import com.android.server.backup.utils.BackupEligibilityRules;
/* loaded from: classes.dex */
public class RestoreParams {
    public final BackupEligibilityRules backupEligibilityRules;
    public final String[] filterSet;
    public final boolean isSystemRestore;
    public final OnTaskFinishedListener listener;
    public final TransportConnection mTransportConnection;
    public final IBackupManagerMonitor monitor;
    public final IRestoreObserver observer;
    public final PackageInfo packageInfo;
    public final int pmToken;
    public final long token;

    public static RestoreParams createForSinglePackage(TransportConnection transportConnection, IRestoreObserver observer, IBackupManagerMonitor monitor, long token, PackageInfo packageInfo, OnTaskFinishedListener listener, BackupEligibilityRules eligibilityRules) {
        return new RestoreParams(transportConnection, observer, monitor, token, packageInfo, 0, false, null, listener, eligibilityRules);
    }

    public static RestoreParams createForRestoreAtInstall(TransportConnection transportConnection, IRestoreObserver observer, IBackupManagerMonitor monitor, long token, String packageName, int pmToken, OnTaskFinishedListener listener, BackupEligibilityRules backupEligibilityRules) {
        String[] filterSet = {packageName};
        return new RestoreParams(transportConnection, observer, monitor, token, null, pmToken, false, filterSet, listener, backupEligibilityRules);
    }

    public static RestoreParams createForRestoreAll(TransportConnection transportConnection, IRestoreObserver observer, IBackupManagerMonitor monitor, long token, OnTaskFinishedListener listener, BackupEligibilityRules backupEligibilityRules) {
        return new RestoreParams(transportConnection, observer, monitor, token, null, 0, true, null, listener, backupEligibilityRules);
    }

    public static RestoreParams createForRestorePackages(TransportConnection transportConnection, IRestoreObserver observer, IBackupManagerMonitor monitor, long token, String[] filterSet, boolean isSystemRestore, OnTaskFinishedListener listener, BackupEligibilityRules backupEligibilityRules) {
        return new RestoreParams(transportConnection, observer, monitor, token, null, 0, isSystemRestore, filterSet, listener, backupEligibilityRules);
    }

    private RestoreParams(TransportConnection transportConnection, IRestoreObserver observer, IBackupManagerMonitor monitor, long token, PackageInfo packageInfo, int pmToken, boolean isSystemRestore, String[] filterSet, OnTaskFinishedListener listener, BackupEligibilityRules backupEligibilityRules) {
        this.mTransportConnection = transportConnection;
        this.observer = observer;
        this.monitor = monitor;
        this.token = token;
        this.packageInfo = packageInfo;
        this.pmToken = pmToken;
        this.isSystemRestore = isSystemRestore;
        this.filterSet = filterSet;
        this.listener = listener;
        this.backupEligibilityRules = backupEligibilityRules;
    }
}
