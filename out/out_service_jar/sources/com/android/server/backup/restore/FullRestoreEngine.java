package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.Settings;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.am.HostingRecord;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.KeyValueAdbRestoreEngine;
import com.android.server.backup.OperationStorage;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.fullbackup.FullBackupObbConnection;
import com.android.server.backup.utils.BackupEligibilityRules;
import com.android.server.backup.utils.BytesReadListener;
import com.android.server.backup.utils.FullBackupRestoreObserverUtils;
import com.android.server.backup.utils.RestoreUtils;
import com.android.server.backup.utils.TarBackupReader;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
/* loaded from: classes.dex */
public class FullRestoreEngine extends RestoreEngine {
    private IBackupAgent mAgent;
    private String mAgentPackage;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    final boolean mAllowApks;
    private long mAppVersion;
    private final BackupEligibilityRules mBackupEligibilityRules;
    private final UserBackupManagerService mBackupManagerService;
    final byte[] mBuffer;
    private final HashSet<String> mClearedPackages;
    private final RestoreDeleteObserver mDeleteObserver;
    final int mEphemeralOpToken;
    private final boolean mIsAdbRestore;
    private final HashMap<String, Signature[]> mManifestSignatures;
    final IBackupManagerMonitor mMonitor;
    private final BackupRestoreTask mMonitorTask;
    private FullBackupObbConnection mObbConnection;
    private IFullBackupRestoreObserver mObserver;
    final PackageInfo mOnlyPackage;
    private final OperationStorage mOperationStorage;
    private final HashMap<String, String> mPackageInstallers;
    private final HashMap<String, RestorePolicy> mPackagePolicies;
    private ParcelFileDescriptor[] mPipes;
    private boolean mPipesClosed;
    private final Object mPipesLock;
    private FileMetadata mReadOnlyParent;
    private ApplicationInfo mTargetApp;
    private final int mUserId;
    private byte[] mWidgetData;

    public FullRestoreEngine(UserBackupManagerService backupManagerService, OperationStorage operationStorage, BackupRestoreTask monitorTask, IFullBackupRestoreObserver observer, IBackupManagerMonitor monitor, PackageInfo onlyPackage, boolean allowApks, int ephemeralOpToken, boolean isAdbRestore, BackupEligibilityRules backupEligibilityRules) {
        this.mDeleteObserver = new RestoreDeleteObserver();
        this.mObbConnection = null;
        this.mPackagePolicies = new HashMap<>();
        this.mPackageInstallers = new HashMap<>();
        this.mManifestSignatures = new HashMap<>();
        this.mClearedPackages = new HashSet<>();
        this.mPipes = null;
        this.mPipesLock = new Object();
        this.mWidgetData = null;
        this.mReadOnlyParent = null;
        this.mBackupManagerService = backupManagerService;
        this.mOperationStorage = operationStorage;
        this.mEphemeralOpToken = ephemeralOpToken;
        this.mMonitorTask = monitorTask;
        this.mObserver = observer;
        this.mMonitor = monitor;
        this.mOnlyPackage = onlyPackage;
        this.mAllowApks = allowApks;
        this.mBuffer = new byte[32768];
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Objects.requireNonNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        this.mIsAdbRestore = isAdbRestore;
        this.mUserId = backupManagerService.getUserId();
        this.mBackupEligibilityRules = backupEligibilityRules;
    }

    FullRestoreEngine() {
        this.mDeleteObserver = new RestoreDeleteObserver();
        this.mObbConnection = null;
        this.mPackagePolicies = new HashMap<>();
        this.mPackageInstallers = new HashMap<>();
        this.mManifestSignatures = new HashMap<>();
        this.mClearedPackages = new HashSet<>();
        this.mPipes = null;
        this.mPipesLock = new Object();
        this.mWidgetData = null;
        this.mReadOnlyParent = null;
        this.mIsAdbRestore = false;
        this.mAllowApks = false;
        this.mEphemeralOpToken = 0;
        this.mUserId = 0;
        this.mBackupEligibilityRules = null;
        this.mAgentTimeoutParameters = null;
        this.mBuffer = null;
        this.mBackupManagerService = null;
        this.mOperationStorage = null;
        this.mMonitor = null;
        this.mMonitorTask = null;
        this.mOnlyPackage = null;
    }

    public IBackupAgent getAgent() {
        return this.mAgent;
    }

    public byte[] getWidgetData() {
        return this.mWidgetData;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [582=11, 489=7, 494=7] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:158:0x048d */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:160:0x048f */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:303:0x0488 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:304:0x0326 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Not initialized variable reg: 38, insn: 0x0137: MOVE  (r15 I:??[OBJECT, ARRAY]) = (r38 I:??[OBJECT, ARRAY]), block:B:42:0x0132 */
    /* JADX WARN: Removed duplicated region for block: B:176:0x04cd A[Catch: IOException -> 0x0590, TRY_LEAVE, TryCatch #18 {IOException -> 0x0590, blocks: (B:176:0x04cd, B:174:0x04c3), top: B:279:0x04c3 }] */
    /* JADX WARN: Removed duplicated region for block: B:207:0x0540  */
    /* JADX WARN: Removed duplicated region for block: B:209:0x0548 A[Catch: IOException -> 0x05d5, TryCatch #12 {IOException -> 0x05d5, blocks: (B:206:0x053a, B:209:0x0548, B:211:0x0585, B:222:0x05a8, B:225:0x05b6, B:227:0x05bc), top: B:275:0x053a }] */
    /* JADX WARN: Removed duplicated region for block: B:213:0x058e  */
    /* JADX WARN: Removed duplicated region for block: B:222:0x05a8 A[Catch: IOException -> 0x05d5, TryCatch #12 {IOException -> 0x05d5, blocks: (B:206:0x053a, B:209:0x0548, B:211:0x0585, B:222:0x05a8, B:225:0x05b6, B:227:0x05bc), top: B:275:0x053a }] */
    /* JADX WARN: Removed duplicated region for block: B:241:0x05d7  */
    /* JADX WARN: Removed duplicated region for block: B:255:0x0622  */
    /* JADX WARN: Removed duplicated region for block: B:258:0x0633  */
    /* JADX WARN: Removed duplicated region for block: B:260:0x0636  */
    /* JADX WARN: Removed duplicated region for block: B:261:0x0639  */
    /* JADX WARN: Type inference failed for: r46v11 */
    /* JADX WARN: Type inference failed for: r46v12 */
    /* JADX WARN: Type inference failed for: r46v13 */
    /* JADX WARN: Type inference failed for: r46v14 */
    /* JADX WARN: Type inference failed for: r46v15 */
    /* JADX WARN: Type inference failed for: r46v16 */
    /* JADX WARN: Type inference failed for: r46v17 */
    /* JADX WARN: Type inference failed for: r46v5 */
    /* JADX WARN: Type inference failed for: r46v6 */
    /* JADX WARN: Type inference failed for: r46v7 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public boolean restoreOneFile(InputStream instream, boolean mustKillAgent, byte[] buffer, PackageInfo onlyPackage, boolean allowApks, int token, IBackupManagerMonitor monitor) {
        FileMetadata info;
        boolean z;
        FileMetadata info2;
        String str;
        String pkg;
        TarBackupReader tarBackupReader;
        long toCopy;
        TarBackupReader tarBackupReader2;
        boolean agentSuccess;
        long toCopy2;
        TarBackupReader tarBackupReader3;
        boolean agentSuccess2;
        boolean okay;
        boolean isRunning = isRunning();
        String str2 = BackupManagerService.TAG;
        if (!isRunning) {
            Slog.w(BackupManagerService.TAG, "Restore engine used after halting");
            return false;
        }
        BytesReadListener bytesReadListener = new BytesReadListener() { // from class: com.android.server.backup.restore.FullRestoreEngine$$ExternalSyntheticLambda0
            @Override // com.android.server.backup.utils.BytesReadListener
            public final void onBytesRead(long j) {
                FullRestoreEngine.lambda$restoreOneFile$0(j);
            }
        };
        IBackupManagerMonitor monitor2 = monitor;
        TarBackupReader tarBackupReader4 = new TarBackupReader(instream, bytesReadListener, monitor2);
        try {
            info2 = tarBackupReader4.readTarHeaders();
        } catch (IOException e) {
            e = e;
        }
        if (info2 != null) {
            String pkg2 = info2.packageName;
            if (!pkg2.equals(this.mAgentPackage)) {
                if (onlyPackage != null) {
                    try {
                        if (!pkg2.equals(onlyPackage.packageName)) {
                            Slog.w(BackupManagerService.TAG, "Expected data for " + onlyPackage + " but saw " + pkg2);
                            setResult(-3);
                            setRunning(false);
                            return false;
                        }
                    } catch (IOException e2) {
                        e = e2;
                    }
                }
                if (!this.mPackagePolicies.containsKey(pkg2)) {
                    this.mPackagePolicies.put(pkg2, RestorePolicy.IGNORE);
                }
                if (this.mAgent != null) {
                    Slog.d(BackupManagerService.TAG, "Saw new package; finalizing old one");
                    tearDownPipes();
                    tearDownAgent(this.mTargetApp, this.mIsAdbRestore);
                    this.mTargetApp = null;
                    this.mAgentPackage = null;
                }
            }
            try {
            } catch (IOException e3) {
                e = e3;
                str2 = str;
            }
            if (info2.path.equals(UserBackupManagerService.BACKUP_MANIFEST_FILENAME)) {
                Signature[] signatures = tarBackupReader4.readAppManifestAndReturnSignatures(info2);
                this.mAppVersion = info2.version;
                PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                RestorePolicy restorePolicy = tarBackupReader4.chooseRestorePolicy(this.mBackupManagerService.getPackageManager(), allowApks, info2, signatures, pmi, this.mUserId, this.mBackupEligibilityRules);
                this.mManifestSignatures.put(info2.packageName, signatures);
                this.mPackagePolicies.put(pkg2, restorePolicy);
                this.mPackageInstallers.put(pkg2, info2.installerPackageName);
                tarBackupReader4.skipTarPadding(info2.size);
                this.mObserver = FullBackupRestoreObserverUtils.sendOnRestorePackage(this.mObserver, pkg2);
                info = info2;
            } else {
                try {
                } catch (IOException e4) {
                    e = e4;
                    str2 = BackupManagerService.TAG;
                    monitor2 = monitor;
                    Slog.w(str2, "io exception on restore socket read: " + e.getMessage());
                    setResult(-3);
                    info = null;
                    if (info == null) {
                    }
                    if (info != null) {
                    }
                }
                if (info2.path.equals(UserBackupManagerService.BACKUP_METADATA_FILENAME)) {
                    tarBackupReader4.readMetadata(info2);
                    this.mWidgetData = tarBackupReader4.getWidgetData();
                    IBackupManagerMonitor monitor3 = tarBackupReader4.getMonitor();
                    try {
                        tarBackupReader4.skipTarPadding(info2.size);
                        info = info2;
                    } catch (IOException e5) {
                        e = e5;
                        monitor2 = monitor3;
                        str2 = BackupManagerService.TAG;
                    }
                    if (info == null) {
                        tearDownPipes();
                        z = false;
                        setRunning(false);
                        if (mustKillAgent) {
                            tearDownAgent(this.mTargetApp, this.mIsAdbRestore);
                        }
                    } else {
                        z = false;
                    }
                    if (info != null) {
                        return true;
                    }
                    return z;
                }
                boolean okay2 = true;
                RestorePolicy policy = this.mPackagePolicies.get(pkg2);
                try {
                    switch (AnonymousClass1.$SwitchMap$com$android$server$backup$restore$RestorePolicy[policy.ordinal()]) {
                        case 1:
                            pkg = pkg2;
                            info = info2;
                            tarBackupReader = tarBackupReader4;
                            str2 = BackupManagerService.TAG;
                            okay2 = false;
                            break;
                        case 2:
                            str2 = BackupManagerService.TAG;
                            try {
                                if (!info2.domain.equals(ActivityTaskManagerService.DUMP_ACTIVITIES_SHORT_CMD)) {
                                    pkg = pkg2;
                                    info = info2;
                                    tarBackupReader = tarBackupReader4;
                                    this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                                    okay2 = false;
                                    break;
                                } else {
                                    Slog.d(str2, "APK file; installing");
                                    String installerPackageName = this.mPackageInstallers.get(pkg2);
                                    boolean isSuccessfullyInstalled = RestoreUtils.installApk(instream, this.mBackupManagerService.getContext(), this.mDeleteObserver, this.mManifestSignatures, this.mPackagePolicies, info2, installerPackageName, bytesReadListener, this.mUserId);
                                    this.mPackagePolicies.put(pkg2, isSuccessfullyInstalled ? RestorePolicy.ACCEPT : RestorePolicy.IGNORE);
                                    tarBackupReader4.skipTarPadding(info2.size);
                                    return true;
                                }
                            } catch (IOException e6) {
                                e = e6;
                                monitor2 = monitor;
                                break;
                            }
                        case 3:
                            try {
                                if (!info2.domain.equals(ActivityTaskManagerService.DUMP_ACTIVITIES_SHORT_CMD)) {
                                    str2 = BackupManagerService.TAG;
                                    pkg = pkg2;
                                    info = info2;
                                    tarBackupReader = tarBackupReader4;
                                    break;
                                } else {
                                    str2 = BackupManagerService.TAG;
                                    Slog.d(str2, "apk present but ACCEPT");
                                    okay2 = false;
                                    pkg = pkg2;
                                    info = info2;
                                    tarBackupReader = tarBackupReader4;
                                    break;
                                }
                            } catch (IOException e7) {
                                e = e7;
                                str2 = BackupManagerService.TAG;
                                break;
                            }
                        default:
                            pkg = pkg2;
                            info = info2;
                            tarBackupReader = tarBackupReader4;
                            str2 = BackupManagerService.TAG;
                            Slog.e(str2, "Invalid policy from manifest");
                            okay2 = false;
                            this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                            break;
                    }
                    boolean okay3 = (isRestorableFile(info) && isCanonicalFilePath(info.path)) ? okay2 : false;
                    if (okay3 && this.mAgent == null) {
                        try {
                            this.mTargetApp = this.mBackupManagerService.getPackageManager().getApplicationInfoAsUser(pkg, 0, this.mUserId);
                            if (!this.mClearedPackages.contains(pkg)) {
                                boolean forceClear = shouldForceClearAppDataOnFullRestore(this.mTargetApp.packageName);
                                if (this.mTargetApp.backupAgentName == null || forceClear) {
                                    Slog.d(str2, "Clearing app data preparatory to full restore");
                                    this.mBackupManagerService.clearApplicationDataBeforeRestore(pkg);
                                }
                                this.mClearedPackages.add(pkg);
                            }
                            setUpPipes();
                            this.mAgent = this.mBackupManagerService.bindToAgentSynchronous(this.mTargetApp, "k".equals(info.domain) ? 0 : 3, this.mBackupEligibilityRules.getOperationType());
                            this.mAgentPackage = pkg;
                        } catch (PackageManager.NameNotFoundException | IOException e8) {
                        }
                        if (this.mAgent == null) {
                            Slog.e(str2, "Unable to create agent for " + pkg);
                            okay3 = false;
                            tearDownPipes();
                            this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                        }
                    }
                    if (okay3 && !pkg.equals(this.mAgentPackage)) {
                        Slog.e(str2, "Restoring data for " + pkg + " but agent is for " + this.mAgentPackage);
                        okay3 = false;
                    }
                    if (shouldSkipReadOnlyDir(info)) {
                        okay3 = false;
                    }
                    if (okay3) {
                        try {
                            long toCopy3 = info.size;
                            boolean isSharedStorage = pkg.equals(UserBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
                            long timeout = isSharedStorage ? this.mAgentTimeoutParameters.getSharedBackupAgentTimeoutMillis() : this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis(this.mTargetApp.uid);
                            try {
                                toCopy = 1;
                                toCopy = 1;
                                this.mBackupManagerService.prepareOperationTimeout(token, timeout, this.mMonitorTask, 1);
                            } catch (RemoteException e9) {
                                toCopy = toCopy3;
                                tarBackupReader2 = tarBackupReader;
                            } catch (IOException e10) {
                                toCopy = toCopy3;
                                tarBackupReader2 = tarBackupReader;
                            }
                            if ("obb".equals(info.domain)) {
                                try {
                                    Slog.d(str2, "Restoring OBB file for " + pkg + " : " + info.path);
                                    agentSuccess2 = true;
                                    toCopy = toCopy3;
                                    try {
                                        tarBackupReader2 = tarBackupReader;
                                        try {
                                            okay = okay3;
                                            this.mObbConnection.restoreObbFile(pkg, this.mPipes[0], info.size, info.type, info.path, info.mode, info.mtime, token, this.mBackupManagerService.getBackupManagerBinder());
                                            toCopy = toCopy;
                                        } catch (RemoteException e11) {
                                            try {
                                                Slog.e(str2, "Agent crashed during full restore");
                                                agentSuccess = false;
                                                okay3 = false;
                                                toCopy2 = toCopy;
                                                if (!okay3) {
                                                }
                                                if (agentSuccess) {
                                                }
                                                if (!okay3) {
                                                }
                                            } catch (IOException e12) {
                                                e = e12;
                                                monitor2 = monitor;
                                            }
                                            if (info == null) {
                                            }
                                            if (info != null) {
                                            }
                                        } catch (IOException e13) {
                                            try {
                                                Slog.d(str2, "Couldn't establish restore");
                                                agentSuccess = false;
                                                okay3 = false;
                                                toCopy2 = toCopy;
                                                if (!okay3) {
                                                }
                                                if (agentSuccess) {
                                                }
                                                if (!okay3) {
                                                }
                                            } catch (IOException e14) {
                                                e = e14;
                                                monitor2 = monitor;
                                                Slog.w(str2, "io exception on restore socket read: " + e.getMessage());
                                                setResult(-3);
                                                info = null;
                                                if (info == null) {
                                                }
                                                if (info != null) {
                                                }
                                            }
                                            if (info == null) {
                                            }
                                            if (info != null) {
                                            }
                                        }
                                    } catch (RemoteException e15) {
                                        tarBackupReader2 = tarBackupReader;
                                    } catch (IOException e16) {
                                        tarBackupReader2 = tarBackupReader;
                                    }
                                } catch (RemoteException e17) {
                                    toCopy = toCopy3;
                                    tarBackupReader2 = tarBackupReader;
                                } catch (IOException e18) {
                                    toCopy = toCopy3;
                                    tarBackupReader2 = tarBackupReader;
                                }
                            } else {
                                okay = okay3;
                                agentSuccess2 = true;
                                toCopy = toCopy3;
                                tarBackupReader2 = tarBackupReader;
                                if ("k".equals(info.domain)) {
                                    Slog.d(str2, "Restoring key-value file for " + pkg + " : " + info.path);
                                    info.version = this.mAppVersion;
                                    UserBackupManagerService userBackupManagerService = this.mBackupManagerService;
                                    KeyValueAdbRestoreEngine restoreEngine = new KeyValueAdbRestoreEngine(userBackupManagerService, userBackupManagerService.getDataDir(), info, this.mPipes[0], this.mAgent, token);
                                    new Thread(restoreEngine, "restore-key-value-runner").start();
                                    toCopy = toCopy;
                                } else if (this.mTargetApp.processName.equals(HostingRecord.HOSTING_TYPE_SYSTEM)) {
                                    Slog.d(str2, "system process agent - spinning a thread");
                                    RestoreFileRunnable runner = new RestoreFileRunnable(this.mBackupManagerService, this.mAgent, info, this.mPipes[0], token);
                                    new Thread(runner, "restore-sys-runner").start();
                                    toCopy = toCopy;
                                } else {
                                    try {
                                        this.mAgent.doRestoreFile(this.mPipes[0], info.size, info.type, info.domain, info.path, info.mode, info.mtime, token, this.mBackupManagerService.getBackupManagerBinder());
                                        agentSuccess = agentSuccess2;
                                        okay3 = okay;
                                        toCopy2 = toCopy;
                                    } catch (RemoteException e19) {
                                        Slog.e(str2, "Agent crashed during full restore");
                                        agentSuccess = false;
                                        okay3 = false;
                                        toCopy2 = toCopy;
                                        if (!okay3) {
                                        }
                                        if (agentSuccess) {
                                        }
                                        if (!okay3) {
                                        }
                                        if (info == null) {
                                        }
                                        if (info != null) {
                                        }
                                    } catch (IOException e20) {
                                        Slog.d(str2, "Couldn't establish restore");
                                        agentSuccess = false;
                                        okay3 = false;
                                        toCopy2 = toCopy;
                                        if (!okay3) {
                                        }
                                        if (agentSuccess) {
                                        }
                                        if (!okay3) {
                                        }
                                        if (info == null) {
                                        }
                                        if (info != null) {
                                        }
                                    }
                                    if (!okay3) {
                                        FileOutputStream pipe = new FileOutputStream(this.mPipes[1].getFileDescriptor());
                                        boolean pipeOkay = true;
                                        long toCopy4 = toCopy2;
                                        while (true) {
                                            if (toCopy4 > 0) {
                                                try {
                                                    int toRead = toCopy4 > ((long) buffer.length) ? buffer.length : (int) toCopy4;
                                                    tarBackupReader3 = tarBackupReader2;
                                                    int nRead = instream.read(buffer, 0, toRead);
                                                    if (nRead > 0) {
                                                        toCopy4 -= nRead;
                                                        if (pipeOkay) {
                                                            try {
                                                                pipe.write(buffer, 0, nRead);
                                                            } catch (IOException e21) {
                                                                Slog.e(str2, "Failed to write to restore pipe: " + e21.getMessage());
                                                                pipeOkay = false;
                                                            }
                                                        }
                                                        tarBackupReader2 = tarBackupReader3;
                                                    }
                                                } catch (IOException e22) {
                                                    e = e22;
                                                    monitor2 = monitor;
                                                    Slog.w(str2, "io exception on restore socket read: " + e.getMessage());
                                                    setResult(-3);
                                                    info = null;
                                                    if (info == null) {
                                                    }
                                                    if (info != null) {
                                                    }
                                                }
                                            } else {
                                                tarBackupReader3 = tarBackupReader2;
                                            }
                                        }
                                        tarBackupReader3.skipTarPadding(info.size);
                                        try {
                                            agentSuccess = this.mBackupManagerService.waitUntilOperationComplete(token);
                                        } catch (IOException e23) {
                                            e = e23;
                                            monitor2 = monitor;
                                            Slog.w(str2, "io exception on restore socket read: " + e.getMessage());
                                            setResult(-3);
                                            info = null;
                                            if (info == null) {
                                            }
                                            if (info != null) {
                                            }
                                        }
                                    }
                                    if (agentSuccess) {
                                        Slog.w(str2, "Agent failure restoring " + pkg + "; ending restore");
                                        this.mBackupManagerService.getBackupHandler().removeMessages(18);
                                        tearDownPipes();
                                        tearDownAgent(this.mTargetApp, false);
                                        this.mAgent = null;
                                        this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                                        if (onlyPackage != null) {
                                            setResult(-2);
                                            setRunning(false);
                                            return false;
                                        }
                                    }
                                }
                            }
                            agentSuccess = agentSuccess2;
                            okay3 = okay;
                            toCopy2 = toCopy;
                            if (!okay3) {
                            }
                            if (agentSuccess) {
                            }
                        } catch (IOException e24) {
                            e = e24;
                        }
                    }
                    if (!okay3) {
                        long bytesToConsume = (info.size + 511) & (-512);
                        while (bytesToConsume > 0) {
                            int toRead2 = bytesToConsume > ((long) buffer.length) ? buffer.length : (int) bytesToConsume;
                            try {
                                long nRead2 = instream.read(buffer, 0, toRead2);
                                if (nRead2 > 0) {
                                    bytesToConsume -= nRead2;
                                }
                            } catch (IOException e25) {
                                e = e25;
                                monitor2 = monitor;
                                Slog.w(str2, "io exception on restore socket read: " + e.getMessage());
                                setResult(-3);
                                info = null;
                                if (info == null) {
                                }
                                if (info != null) {
                                }
                            }
                        }
                    }
                } catch (IOException e26) {
                    e = e26;
                    monitor2 = monitor;
                    Slog.w(str2, "io exception on restore socket read: " + e.getMessage());
                    setResult(-3);
                    info = null;
                    if (info == null) {
                    }
                    if (info != null) {
                    }
                }
                Slog.w(str2, "io exception on restore socket read: " + e.getMessage());
                setResult(-3);
                info = null;
                if (info == null) {
                }
                if (info != null) {
                }
            }
        } else {
            info = info2;
        }
        if (info == null) {
        }
        if (info != null) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$restoreOneFile$0(long bytesRead) {
    }

    /* renamed from: com.android.server.backup.restore.FullRestoreEngine$1  reason: invalid class name */
    /* loaded from: classes.dex */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$backup$restore$RestorePolicy;

        static {
            int[] iArr = new int[RestorePolicy.values().length];
            $SwitchMap$com$android$server$backup$restore$RestorePolicy = iArr;
            try {
                iArr[RestorePolicy.IGNORE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$RestorePolicy[RestorePolicy.ACCEPT_IF_APK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$RestorePolicy[RestorePolicy.ACCEPT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    boolean shouldSkipReadOnlyDir(FileMetadata info) {
        if (isValidParent(this.mReadOnlyParent, info)) {
            return true;
        }
        if (isReadOnlyDir(info)) {
            this.mReadOnlyParent = info;
            Slog.w(BackupManagerService.TAG, "Skipping restore of " + info.path + " and its contents as read-only dirs are currently not supported.");
            return true;
        }
        this.mReadOnlyParent = null;
        return false;
    }

    private static boolean isValidParent(FileMetadata parentDir, FileMetadata childDir) {
        return parentDir != null && childDir.packageName.equals(parentDir.packageName) && childDir.domain.equals(parentDir.domain) && childDir.path.startsWith(getPathWithTrailingSeparator(parentDir.path));
    }

    private static String getPathWithTrailingSeparator(String path) {
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    private static boolean isReadOnlyDir(FileMetadata file) {
        return file.type == 2 && (file.mode & ((long) OsConstants.S_IWUSR)) == 0;
    }

    private void setUpPipes() throws IOException {
        synchronized (this.mPipesLock) {
            this.mPipes = ParcelFileDescriptor.createPipe();
            this.mPipesClosed = false;
        }
    }

    private void tearDownPipes() {
        ParcelFileDescriptor[] parcelFileDescriptorArr;
        synchronized (this.mPipesLock) {
            if (!this.mPipesClosed && (parcelFileDescriptorArr = this.mPipes) != null) {
                try {
                    parcelFileDescriptorArr[0].close();
                    this.mPipes[1].close();
                    this.mPipesClosed = true;
                } catch (IOException e) {
                    Slog.w(BackupManagerService.TAG, "Couldn't close agent pipes", e);
                }
            }
        }
    }

    private void tearDownAgent(ApplicationInfo app, boolean doRestoreFinished) {
        if (this.mAgent != null) {
            if (doRestoreFinished) {
                try {
                    int token = this.mBackupManagerService.generateRandomIntegerToken();
                    long fullBackupAgentTimeoutMillis = this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
                    AdbRestoreFinishedLatch latch = new AdbRestoreFinishedLatch(this.mBackupManagerService, this.mOperationStorage, token);
                    this.mBackupManagerService.prepareOperationTimeout(token, fullBackupAgentTimeoutMillis, latch, 1);
                    if (this.mTargetApp.processName.equals(HostingRecord.HOSTING_TYPE_SYSTEM)) {
                        Runnable runner = new AdbRestoreFinishedRunnable(this.mAgent, token, this.mBackupManagerService);
                        new Thread(runner, "restore-sys-finished-runner").start();
                    } else {
                        this.mAgent.doRestoreFinished(token, this.mBackupManagerService.getBackupManagerBinder());
                    }
                    latch.await();
                } catch (RemoteException e) {
                    Slog.d(BackupManagerService.TAG, "Lost app trying to shut down");
                }
            }
            this.mBackupManagerService.tearDownAgentAndKill(app);
            this.mAgent = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleTimeout() {
        tearDownPipes();
        setResult(-2);
        setRunning(false);
    }

    private boolean isRestorableFile(FileMetadata info) {
        if (this.mBackupEligibilityRules.getOperationType() == 1) {
            return true;
        }
        if ("c".equals(info.domain)) {
            return false;
        }
        return (ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD.equals(info.domain) && info.path.startsWith("no_backup/")) ? false : true;
    }

    private static boolean isCanonicalFilePath(String path) {
        if (path.contains("..") || path.contains("//")) {
            return false;
        }
        return true;
    }

    private boolean shouldForceClearAppDataOnFullRestore(String packageName) {
        String packageListString = Settings.Secure.getStringForUser(this.mBackupManagerService.getContext().getContentResolver(), "packages_to_clear_data_before_full_restore", this.mUserId);
        if (TextUtils.isEmpty(packageListString)) {
            return false;
        }
        List<String> packages = Arrays.asList(packageListString.split(";"));
        return packages.contains(packageName);
    }

    void sendOnRestorePackage(String name) {
        IFullBackupRestoreObserver iFullBackupRestoreObserver = this.mObserver;
        if (iFullBackupRestoreObserver != null) {
            try {
                iFullBackupRestoreObserver.onRestorePackage(name);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "full restore observer went away: restorePackage");
                this.mObserver = null;
            }
        }
    }
}
