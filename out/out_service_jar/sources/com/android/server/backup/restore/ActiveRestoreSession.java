package com.android.server.backup.restore;

import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.app.backup.IRestoreSession;
import android.app.backup.RestoreSet;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.util.Slog;
import com.android.server.backup.TransportManager;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.params.RestoreGetSetsParams;
import com.android.server.backup.params.RestoreParams;
import com.android.server.backup.transport.TransportConnection;
import com.android.server.backup.utils.BackupEligibilityRules;
import java.util.function.BiFunction;
/* loaded from: classes.dex */
public class ActiveRestoreSession extends IRestoreSession.Stub {
    private static final String DEVICE_NAME_FOR_D2D_SET = "D2D";
    private static final String TAG = "RestoreSession";
    private final BackupEligibilityRules mBackupEligibilityRules;
    private final UserBackupManagerService mBackupManagerService;
    private final String mPackageName;
    private final TransportManager mTransportManager;
    private final String mTransportName;
    private final int mUserId;
    public RestoreSet[] mRestoreSets = null;
    boolean mEnded = false;
    boolean mTimedOut = false;

    public ActiveRestoreSession(UserBackupManagerService backupManagerService, String packageName, String transportName, BackupEligibilityRules backupEligibilityRules) {
        this.mBackupManagerService = backupManagerService;
        this.mPackageName = packageName;
        this.mTransportManager = backupManagerService.getTransportManager();
        this.mTransportName = transportName;
        this.mUserId = backupManagerService.getUserId();
        this.mBackupEligibilityRules = backupEligibilityRules;
    }

    public void markTimedOut() {
        this.mTimedOut = true;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [138=5] */
    public synchronized int getAvailableRestoreSets(IRestoreObserver observer, IBackupManagerMonitor monitor) {
        this.mBackupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "getAvailableRestoreSets");
        if (observer != null) {
            if (this.mEnded) {
                throw new IllegalStateException("Restore session already ended");
            }
            if (this.mTimedOut) {
                Slog.i(TAG, "Session already timed out");
                return -1;
            }
            long oldId = Binder.clearCallingIdentity();
            try {
                final TransportConnection transportConnection = this.mTransportManager.getTransportClient(this.mTransportName, "RestoreSession.getAvailableRestoreSets()");
                if (transportConnection == null) {
                    Slog.w(TAG, "Null transport client getting restore sets");
                    Binder.restoreCallingIdentity(oldId);
                    return -1;
                }
                this.mBackupManagerService.getBackupHandler().removeMessages(8);
                final UserBackupManagerService.BackupWakeLock wakelock = this.mBackupManagerService.getWakelock();
                wakelock.acquire();
                final TransportManager transportManager = this.mTransportManager;
                OnTaskFinishedListener listener = new OnTaskFinishedListener() { // from class: com.android.server.backup.restore.ActiveRestoreSession$$ExternalSyntheticLambda2
                    @Override // com.android.server.backup.internal.OnTaskFinishedListener
                    public final void onFinished(String str) {
                        ActiveRestoreSession.lambda$getAvailableRestoreSets$0(TransportManager.this, transportConnection, wakelock, str);
                    }
                };
                Message msg = this.mBackupManagerService.getBackupHandler().obtainMessage(6, new RestoreGetSetsParams(transportConnection, this, observer, monitor, listener));
                this.mBackupManagerService.getBackupHandler().sendMessage(msg);
                Binder.restoreCallingIdentity(oldId);
                return 0;
            } catch (Exception e) {
                Slog.e(TAG, "Error in getAvailableRestoreSets", e);
                Binder.restoreCallingIdentity(oldId);
                return -1;
            }
        }
        throw new IllegalArgumentException("Observer must not be null");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getAvailableRestoreSets$0(TransportManager transportManager, TransportConnection transportConnection, UserBackupManagerService.BackupWakeLock wakelock, String caller) {
        transportManager.disposeOfTransportClient(transportConnection, caller);
        wakelock.release();
    }

    public synchronized int restoreAll(final long token, final IRestoreObserver observer, final IBackupManagerMonitor monitor) {
        this.mBackupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "performRestore");
        Slog.d(TAG, "restoreAll token=" + Long.toHexString(token) + " observer=" + observer);
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        }
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        } else if (this.mRestoreSets == null) {
            Slog.e(TAG, "Ignoring restoreAll() with no restore set");
            return -1;
        } else if (this.mPackageName != null) {
            Slog.e(TAG, "Ignoring restoreAll() on single-package session");
            return -1;
        } else if (!this.mTransportManager.isTransportRegistered(this.mTransportName)) {
            Slog.e(TAG, "Transport " + this.mTransportName + " not registered");
            return -1;
        } else {
            synchronized (this.mBackupManagerService.getQueueLock()) {
                int i = 0;
                while (true) {
                    try {
                        RestoreSet[] restoreSetArr = this.mRestoreSets;
                        if (i < restoreSetArr.length) {
                            try {
                                if (token != restoreSetArr[i].token) {
                                    i++;
                                } else {
                                    long oldId = Binder.clearCallingIdentity();
                                    final RestoreSet restoreSet = this.mRestoreSets[i];
                                    int sendRestoreToHandlerLocked = sendRestoreToHandlerLocked(new BiFunction() { // from class: com.android.server.backup.restore.ActiveRestoreSession$$ExternalSyntheticLambda0
                                        @Override // java.util.function.BiFunction
                                        public final Object apply(Object obj, Object obj2) {
                                            return ActiveRestoreSession.this.m2225xbd024a1f(observer, monitor, token, restoreSet, (TransportConnection) obj, (OnTaskFinishedListener) obj2);
                                        }
                                    }, "RestoreSession.restoreAll()");
                                    Binder.restoreCallingIdentity(oldId);
                                    return sendRestoreToHandlerLocked;
                                }
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } else {
                            Slog.w(TAG, "Restore token " + Long.toHexString(token) + " not found");
                            return -1;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$restoreAll$1$com-android-server-backup-restore-ActiveRestoreSession  reason: not valid java name */
    public /* synthetic */ RestoreParams m2225xbd024a1f(IRestoreObserver observer, IBackupManagerMonitor monitor, long token, RestoreSet restoreSet, TransportConnection transportClient, OnTaskFinishedListener listener) {
        return RestoreParams.createForRestoreAll(transportClient, observer, monitor, token, listener, getBackupEligibilityRules(restoreSet));
    }

    public synchronized int restorePackages(final long token, final IRestoreObserver observer, final String[] packages, final IBackupManagerMonitor monitor) {
        this.mBackupManagerService.getContext().enforceCallingOrSelfPermission("android.permission.BACKUP", "performRestore");
        StringBuilder b = new StringBuilder(128);
        b.append("restorePackages token=");
        b.append(Long.toHexString(token));
        b.append(" observer=");
        if (observer == null) {
            b.append("null");
        } else {
            b.append(observer.toString());
        }
        b.append(" monitor=");
        if (monitor == null) {
            b.append("null");
        } else {
            b.append(monitor.toString());
        }
        b.append(" packages=");
        if (packages == null) {
            b.append("null");
        } else {
            b.append('{');
            boolean first = true;
            for (String s : packages) {
                if (!first) {
                    b.append(", ");
                } else {
                    first = false;
                }
                b.append(s);
            }
            b.append('}');
        }
        Slog.d(TAG, b.toString());
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        }
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        } else if (this.mRestoreSets == null) {
            Slog.e(TAG, "Ignoring restoreAll() with no restore set");
            return -1;
        } else if (this.mPackageName != null) {
            Slog.e(TAG, "Ignoring restoreAll() on single-package session");
            return -1;
        } else if (!this.mTransportManager.isTransportRegistered(this.mTransportName)) {
            Slog.e(TAG, "Transport " + this.mTransportName + " not registered");
            return -1;
        } else {
            synchronized (this.mBackupManagerService.getQueueLock()) {
                int i = 0;
                while (true) {
                    try {
                        RestoreSet[] restoreSetArr = this.mRestoreSets;
                        if (i < restoreSetArr.length) {
                            try {
                                if (token != restoreSetArr[i].token) {
                                    i++;
                                } else {
                                    long oldId = Binder.clearCallingIdentity();
                                    final RestoreSet restoreSet = this.mRestoreSets[i];
                                    int sendRestoreToHandlerLocked = sendRestoreToHandlerLocked(new BiFunction() { // from class: com.android.server.backup.restore.ActiveRestoreSession$$ExternalSyntheticLambda1
                                        @Override // java.util.function.BiFunction
                                        public final Object apply(Object obj, Object obj2) {
                                            return ActiveRestoreSession.this.m2227xadecd330(observer, monitor, token, packages, restoreSet, (TransportConnection) obj, (OnTaskFinishedListener) obj2);
                                        }
                                    }, "RestoreSession.restorePackages(" + packages.length + " packages)");
                                    Binder.restoreCallingIdentity(oldId);
                                    return sendRestoreToHandlerLocked;
                                }
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        } else {
                            Slog.w(TAG, "Restore token " + Long.toHexString(token) + " not found");
                            return -1;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$restorePackages$2$com-android-server-backup-restore-ActiveRestoreSession  reason: not valid java name */
    public /* synthetic */ RestoreParams m2227xadecd330(IRestoreObserver observer, IBackupManagerMonitor monitor, long token, String[] packages, RestoreSet restoreSet, TransportConnection transportClient, OnTaskFinishedListener listener) {
        return RestoreParams.createForRestorePackages(transportClient, observer, monitor, token, packages, packages.length > 1, listener, getBackupEligibilityRules(restoreSet));
    }

    private BackupEligibilityRules getBackupEligibilityRules(RestoreSet restoreSet) {
        int operationType = DEVICE_NAME_FOR_D2D_SET.equals(restoreSet.device) ? 1 : 0;
        return this.mBackupManagerService.getEligibilityRulesForOperation(operationType);
    }

    public synchronized int restorePackage(String packageName, final IRestoreObserver observer, final IBackupManagerMonitor monitor) {
        Slog.v(TAG, "restorePackage pkg=" + packageName + " obs=" + observer + "monitor=" + monitor);
        if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        }
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
            return -1;
        }
        String str = this.mPackageName;
        if (str != null && !str.equals(packageName)) {
            Slog.e(TAG, "Ignoring attempt to restore pkg=" + packageName + " on session for package " + this.mPackageName);
            return -1;
        }
        try {
            final PackageInfo app = this.mBackupManagerService.getPackageManager().getPackageInfoAsUser(packageName, 0, this.mUserId);
            int perm = this.mBackupManagerService.getContext().checkPermission("android.permission.BACKUP", Binder.getCallingPid(), Binder.getCallingUid());
            if (perm == -1 && app.applicationInfo.uid != Binder.getCallingUid()) {
                Slog.w(TAG, "restorePackage: bad packageName=" + packageName + " or calling uid=" + Binder.getCallingUid());
                throw new SecurityException("No permission to restore other packages");
            }
            if (!this.mTransportManager.isTransportRegistered(this.mTransportName)) {
                Slog.e(TAG, "Transport " + this.mTransportName + " not registered");
                return -1;
            }
            long oldId = Binder.clearCallingIdentity();
            final long token = this.mBackupManagerService.getAvailableRestoreToken(packageName);
            Slog.v(TAG, "restorePackage pkg=" + packageName + " token=" + Long.toHexString(token));
            if (token == 0) {
                Slog.w(TAG, "No data available for this package; not restoring");
                Binder.restoreCallingIdentity(oldId);
                return -1;
            }
            int sendRestoreToHandlerLocked = sendRestoreToHandlerLocked(new BiFunction() { // from class: com.android.server.backup.restore.ActiveRestoreSession$$ExternalSyntheticLambda3
                @Override // java.util.function.BiFunction
                public final Object apply(Object obj, Object obj2) {
                    return ActiveRestoreSession.this.m2226x8c604c58(observer, monitor, token, app, (TransportConnection) obj, (OnTaskFinishedListener) obj2);
                }
            }, "RestoreSession.restorePackage(" + packageName + ")");
            Binder.restoreCallingIdentity(oldId);
            return sendRestoreToHandlerLocked;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Asked to restore nonexistent pkg " + packageName);
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$restorePackage$3$com-android-server-backup-restore-ActiveRestoreSession  reason: not valid java name */
    public /* synthetic */ RestoreParams m2226x8c604c58(IRestoreObserver observer, IBackupManagerMonitor monitor, long token, PackageInfo app, TransportConnection transportClient, OnTaskFinishedListener listener) {
        return RestoreParams.createForSinglePackage(transportClient, observer, monitor, token, app, listener, this.mBackupEligibilityRules);
    }

    public void setRestoreSets(RestoreSet[] restoreSets) {
        this.mRestoreSets = restoreSets;
    }

    private int sendRestoreToHandlerLocked(BiFunction<TransportConnection, OnTaskFinishedListener, RestoreParams> restoreParamsBuilder, String callerLogString) {
        final TransportConnection transportConnection = this.mTransportManager.getTransportClient(this.mTransportName, callerLogString);
        if (transportConnection == null) {
            Slog.e(TAG, "Transport " + this.mTransportName + " got unregistered");
            return -1;
        }
        Handler backupHandler = this.mBackupManagerService.getBackupHandler();
        backupHandler.removeMessages(8);
        final UserBackupManagerService.BackupWakeLock wakelock = this.mBackupManagerService.getWakelock();
        wakelock.acquire();
        final TransportManager transportManager = this.mTransportManager;
        OnTaskFinishedListener listener = new OnTaskFinishedListener() { // from class: com.android.server.backup.restore.ActiveRestoreSession$$ExternalSyntheticLambda4
            @Override // com.android.server.backup.internal.OnTaskFinishedListener
            public final void onFinished(String str) {
                ActiveRestoreSession.lambda$sendRestoreToHandlerLocked$4(TransportManager.this, transportConnection, wakelock, str);
            }
        };
        Message msg = backupHandler.obtainMessage(3);
        msg.obj = restoreParamsBuilder.apply(transportConnection, listener);
        backupHandler.sendMessage(msg);
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$sendRestoreToHandlerLocked$4(TransportManager transportManager, TransportConnection transportConnection, UserBackupManagerService.BackupWakeLock wakelock, String caller) {
        transportManager.disposeOfTransportClient(transportConnection, caller);
        wakelock.release();
    }

    /* loaded from: classes.dex */
    public class EndRestoreRunnable implements Runnable {
        UserBackupManagerService mBackupManager;
        ActiveRestoreSession mSession;

        public EndRestoreRunnable(UserBackupManagerService manager, ActiveRestoreSession session) {
            this.mBackupManager = manager;
            this.mSession = session;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (this.mSession) {
                this.mSession.mEnded = true;
            }
            this.mBackupManager.clearRestoreSession(this.mSession);
        }
    }

    public synchronized void endRestoreSession() {
        Slog.d(TAG, "endRestoreSession");
        if (this.mTimedOut) {
            Slog.i(TAG, "Session already timed out");
        } else if (this.mEnded) {
            throw new IllegalStateException("Restore session already ended");
        } else {
            this.mBackupManagerService.getBackupHandler().post(new EndRestoreRunnable(this.mBackupManagerService, this));
        }
    }
}
