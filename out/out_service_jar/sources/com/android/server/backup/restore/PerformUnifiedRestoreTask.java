package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IRestoreObserver;
import android.app.backup.RestoreDescription;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.os.Bundle;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.BackupUtils;
import com.android.server.backup.OperationStorage;
import com.android.server.backup.PackageManagerBackupAgent;
import com.android.server.backup.TransportManager;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.keyvalue.KeyValueBackupTask;
import com.android.server.backup.transport.BackupTransportClient;
import com.android.server.backup.transport.TransportConnection;
import com.android.server.backup.utils.BackupEligibilityRules;
import com.android.server.backup.utils.BackupManagerMonitorUtils;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public class PerformUnifiedRestoreTask implements BackupRestoreTask {
    private UserBackupManagerService backupManagerService;
    private List<PackageInfo> mAcceptSet;
    private IBackupAgent mAgent;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private ParcelFileDescriptor mBackupData;
    private File mBackupDataName;
    private final BackupEligibilityRules mBackupEligibilityRules;
    private int mCount;
    private PackageInfo mCurrentPackage;
    private boolean mDidLaunch;
    private final int mEphemeralOpToken;
    private boolean mFinished;
    private boolean mIsSystemRestore;
    private final OnTaskFinishedListener mListener;
    private IBackupManagerMonitor mMonitor;
    private ParcelFileDescriptor mNewState;
    private File mNewStateName;
    private IRestoreObserver mObserver;
    private final OperationStorage mOperationStorage;
    private PackageManagerBackupAgent mPmAgent;
    private int mPmToken;
    private RestoreDescription mRestoreDescription;
    private File mStageName;
    private long mStartRealtime;
    private UnifiedRestoreState mState;
    private File mStateDir;
    private int mStatus;
    private PackageInfo mTargetPackage;
    private long mToken;
    private final TransportConnection mTransportConnection;
    private final TransportManager mTransportManager;
    private final int mUserId;
    private byte[] mWidgetData;

    PerformUnifiedRestoreTask(UserBackupManagerService backupManagerService) {
        this.mListener = null;
        this.mAgentTimeoutParameters = null;
        this.mOperationStorage = null;
        this.mTransportConnection = null;
        this.mTransportManager = null;
        this.mEphemeralOpToken = 0;
        this.mUserId = 0;
        this.mBackupEligibilityRules = null;
        this.backupManagerService = backupManagerService;
    }

    public PerformUnifiedRestoreTask(UserBackupManagerService backupManagerService, OperationStorage operationStorage, TransportConnection transportConnection, IRestoreObserver observer, IBackupManagerMonitor monitor, long restoreSetToken, PackageInfo targetPackage, int pmToken, boolean isFullSystemRestore, String[] filterSet, OnTaskFinishedListener listener, BackupEligibilityRules backupEligibilityRules) {
        String[] filterSet2;
        this.backupManagerService = backupManagerService;
        this.mOperationStorage = operationStorage;
        int userId = backupManagerService.getUserId();
        this.mUserId = userId;
        this.mTransportManager = backupManagerService.getTransportManager();
        this.mEphemeralOpToken = backupManagerService.generateRandomIntegerToken();
        this.mState = UnifiedRestoreState.INITIAL;
        this.mStartRealtime = SystemClock.elapsedRealtime();
        this.mTransportConnection = transportConnection;
        this.mObserver = observer;
        this.mMonitor = monitor;
        this.mToken = restoreSetToken;
        this.mPmToken = pmToken;
        this.mTargetPackage = targetPackage;
        this.mIsSystemRestore = isFullSystemRestore;
        this.mFinished = false;
        this.mDidLaunch = false;
        this.mListener = listener;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Objects.requireNonNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        this.mBackupEligibilityRules = backupEligibilityRules;
        if (targetPackage != null) {
            ArrayList arrayList = new ArrayList();
            this.mAcceptSet = arrayList;
            arrayList.add(targetPackage);
        } else {
            if (filterSet != null) {
                filterSet2 = filterSet;
            } else {
                List<PackageInfo> apps = PackageManagerBackupAgent.getStorableApplications(backupManagerService.getPackageManager(), userId, backupEligibilityRules);
                filterSet2 = packagesToNames(apps);
                Slog.i(BackupManagerService.TAG, "Full restore; asking about " + filterSet2.length + " apps");
            }
            this.mAcceptSet = new ArrayList(filterSet2.length);
            boolean hasSettings = false;
            boolean hasSystem = false;
            for (String str : filterSet2) {
                try {
                    PackageManager pm = backupManagerService.getPackageManager();
                    PackageInfo info = pm.getPackageInfoAsUser(str, 0, this.mUserId);
                    if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(info.packageName)) {
                        hasSystem = true;
                    } else if (UserBackupManagerService.SETTINGS_PACKAGE.equals(info.packageName)) {
                        hasSettings = true;
                    } else if (backupEligibilityRules.appIsEligibleForBackup(info.applicationInfo)) {
                        this.mAcceptSet.add(info);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            if (hasSystem) {
                try {
                    this.mAcceptSet.add(0, backupManagerService.getPackageManager().getPackageInfoAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, this.mUserId));
                } catch (PackageManager.NameNotFoundException e2) {
                }
            }
            if (hasSettings) {
                try {
                    this.mAcceptSet.add(backupManagerService.getPackageManager().getPackageInfoAsUser(UserBackupManagerService.SETTINGS_PACKAGE, 0, this.mUserId));
                } catch (PackageManager.NameNotFoundException e3) {
                }
            }
        }
        this.mAcceptSet = backupManagerService.filterUserFacingPackages(this.mAcceptSet);
    }

    private String[] packagesToNames(List<PackageInfo> apps) {
        int N = apps.size();
        String[] names = new String[N];
        for (int i = 0; i < N; i++) {
            names[i] = apps.get(i).packageName;
        }
        return names;
    }

    /* renamed from: com.android.server.backup.restore.PerformUnifiedRestoreTask$1  reason: invalid class name */
    /* loaded from: classes.dex */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState;

        static {
            int[] iArr = new int[UnifiedRestoreState.values().length];
            $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState = iArr;
            try {
                iArr[UnifiedRestoreState.INITIAL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.RUNNING_QUEUE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.RESTORE_KEYVALUE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.RESTORE_FULL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.RESTORE_FINISHED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[UnifiedRestoreState.FINAL.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void execute() {
        switch (AnonymousClass1.$SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[this.mState.ordinal()]) {
            case 1:
                startRestore();
                return;
            case 2:
                dispatchNextRestore();
                return;
            case 3:
                restoreKeyValue();
                return;
            case 4:
                restoreFull();
                return;
            case 5:
                restoreFinished();
                return;
            case 6:
                if (!this.mFinished) {
                    finalizeRestore();
                } else {
                    Slog.e(BackupManagerService.TAG, "Duplicate finish");
                }
                this.mFinished = true;
                return;
            default:
                return;
        }
    }

    private void startRestore() {
        sendStartRestore(this.mAcceptSet.size());
        if (this.mIsSystemRestore) {
            AppWidgetBackupBridge.systemRestoreStarting(this.mUserId);
        }
        try {
            String transportDirName = this.mTransportManager.getTransportDirName(this.mTransportConnection.getTransportComponent());
            this.mStateDir = new File(this.backupManagerService.getBaseStateDir(), transportDirName);
            PackageInfo pmPackage = new PackageInfo();
            pmPackage.packageName = UserBackupManagerService.PACKAGE_MANAGER_SENTINEL;
            this.mAcceptSet.add(0, pmPackage);
            PackageInfo[] packages = (PackageInfo[]) this.mAcceptSet.toArray(new PackageInfo[0]);
            BackupTransportClient transport = this.mTransportConnection.connectOrThrow("PerformUnifiedRestoreTask.startRestore()");
            int startRestore = transport.startRestore(this.mToken, packages);
            this.mStatus = startRestore;
            if (startRestore != 0) {
                Slog.e(BackupManagerService.TAG, "Transport error " + this.mStatus + "; no restore possible");
                this.mStatus = -1000;
                executeNextState(UnifiedRestoreState.FINAL);
                return;
            }
            RestoreDescription desc = transport.nextRestorePackage();
            if (desc == null) {
                Slog.e(BackupManagerService.TAG, "No restore metadata available; halting");
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 22, this.mCurrentPackage, 3, null);
                this.mStatus = -1000;
                executeNextState(UnifiedRestoreState.FINAL);
            } else if (!UserBackupManagerService.PACKAGE_MANAGER_SENTINEL.equals(desc.getPackageName())) {
                Slog.e(BackupManagerService.TAG, "Required package metadata but got " + desc.getPackageName());
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 23, this.mCurrentPackage, 3, null);
                this.mStatus = -1000;
                executeNextState(UnifiedRestoreState.FINAL);
            } else {
                PackageInfo packageInfo = new PackageInfo();
                this.mCurrentPackage = packageInfo;
                packageInfo.packageName = UserBackupManagerService.PACKAGE_MANAGER_SENTINEL;
                this.mCurrentPackage.applicationInfo = new ApplicationInfo();
                this.mCurrentPackage.applicationInfo.uid = 1000;
                PackageManagerBackupAgent makeMetadataAgent = this.backupManagerService.makeMetadataAgent(null);
                this.mPmAgent = makeMetadataAgent;
                this.mAgent = IBackupAgent.Stub.asInterface(makeMetadataAgent.onBind());
                initiateOneRestore(this.mCurrentPackage, 0L);
                this.backupManagerService.getBackupHandler().removeMessages(18);
                if (!this.mPmAgent.hasMetadata()) {
                    Slog.e(BackupManagerService.TAG, "PM agent has no metadata, so not restoring");
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 24, this.mCurrentPackage, 3, null);
                    EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, UserBackupManagerService.PACKAGE_MANAGER_SENTINEL, "Package manager restore metadata missing");
                    this.mStatus = -1000;
                    this.backupManagerService.getBackupHandler().removeMessages(20, this);
                    executeNextState(UnifiedRestoreState.FINAL);
                }
            }
        } catch (Exception e) {
            Slog.e(BackupManagerService.TAG, "Unable to contact transport for restore: " + e.getMessage());
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 25, null, 1, null);
            this.mStatus = -1000;
            this.backupManagerService.getBackupHandler().removeMessages(20, this);
            executeNextState(UnifiedRestoreState.FINAL);
        }
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [628=10] */
    private void dispatchNextRestore() {
        UnifiedRestoreState nextState;
        UnifiedRestoreState nextState2 = UnifiedRestoreState.FINAL;
        try {
            BackupTransportClient transport = this.mTransportConnection.connectOrThrow("PerformUnifiedRestoreTask.dispatchNextRestore()");
            RestoreDescription nextRestorePackage = transport.nextRestorePackage();
            this.mRestoreDescription = nextRestorePackage;
            String pkgName = nextRestorePackage != null ? nextRestorePackage.getPackageName() : null;
            if (pkgName == null) {
                Slog.e(BackupManagerService.TAG, "Failure getting next package name");
                EventLog.writeEvent((int) EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                nextState2 = UnifiedRestoreState.FINAL;
            } else if (this.mRestoreDescription == RestoreDescription.NO_MORE_PACKAGES) {
                Slog.v(BackupManagerService.TAG, "No more packages; finishing restore");
                int millis = (int) (SystemClock.elapsedRealtime() - this.mStartRealtime);
                EventLog.writeEvent((int) EventLogTags.RESTORE_SUCCESS, Integer.valueOf(this.mCount), Integer.valueOf(millis));
                nextState2 = UnifiedRestoreState.FINAL;
            } else {
                Slog.i(BackupManagerService.TAG, "Next restore package: " + this.mRestoreDescription);
                sendOnRestorePackage(pkgName);
                PackageManagerBackupAgent.Metadata metaInfo = this.mPmAgent.getRestoredMetadata(pkgName);
                if (metaInfo == null) {
                    Slog.e(BackupManagerService.TAG, "No metadata for " + pkgName);
                    EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, pkgName, "Package metadata missing");
                    nextState2 = UnifiedRestoreState.RUNNING_QUEUE;
                    return;
                }
                try {
                    this.mCurrentPackage = this.backupManagerService.getPackageManager().getPackageInfoAsUser(pkgName, 134217728, this.mUserId);
                    if (metaInfo.versionCode > this.mCurrentPackage.getLongVersionCode()) {
                        if ((this.mCurrentPackage.applicationInfo.flags & 131072) == 0) {
                            String message = "Source version " + metaInfo.versionCode + " > installed version " + this.mCurrentPackage.getLongVersionCode();
                            Slog.w(BackupManagerService.TAG, "Package " + pkgName + ": " + message);
                            Bundle monitoringExtras = BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_RESTORE_VERSION", metaInfo.versionCode);
                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 27, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(monitoringExtras, "android.app.backup.extra.LOG_RESTORE_ANYWAY", false));
                            EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, pkgName, message);
                            nextState2 = UnifiedRestoreState.RUNNING_QUEUE;
                            return;
                        }
                        Slog.v(BackupManagerService.TAG, "Source version " + metaInfo.versionCode + " > installed version " + this.mCurrentPackage.getLongVersionCode() + " but restoreAnyVersion");
                        Bundle monitoringExtras2 = BackupManagerMonitorUtils.putMonitoringExtra((Bundle) null, "android.app.backup.extra.LOG_RESTORE_VERSION", metaInfo.versionCode);
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 27, this.mCurrentPackage, 3, BackupManagerMonitorUtils.putMonitoringExtra(monitoringExtras2, "android.app.backup.extra.LOG_RESTORE_ANYWAY", true));
                    }
                    this.mWidgetData = null;
                    int type = this.mRestoreDescription.getDataType();
                    if (type == 1) {
                        nextState = UnifiedRestoreState.RESTORE_KEYVALUE;
                    } else if (type != 2) {
                        Slog.e(BackupManagerService.TAG, "Unrecognized restore type " + type);
                        nextState2 = UnifiedRestoreState.RUNNING_QUEUE;
                    } else {
                        nextState = UnifiedRestoreState.RESTORE_FULL;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(BackupManagerService.TAG, "Package not present: " + pkgName);
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 26, this.mCurrentPackage, 3, null);
                    EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, pkgName, "Package missing on device");
                    nextState2 = UnifiedRestoreState.RUNNING_QUEUE;
                }
            }
        } catch (Exception e2) {
            Slog.e(BackupManagerService.TAG, "Can't get next restore target from transport; halting: " + e2.getMessage());
            EventLog.writeEvent((int) EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
            nextState2 = UnifiedRestoreState.FINAL;
        } finally {
            executeNextState(nextState2);
        }
    }

    private void restoreKeyValue() {
        String packageName = this.mCurrentPackage.packageName;
        if (this.mCurrentPackage.applicationInfo.backupAgentName == null || "".equals(this.mCurrentPackage.applicationInfo.backupAgentName)) {
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 28, this.mCurrentPackage, 2, null);
            EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, packageName, "Package has no agent");
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            return;
        }
        PackageManagerBackupAgent.Metadata metaInfo = this.mPmAgent.getRestoredMetadata(packageName);
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        if (!BackupUtils.signaturesMatch(metaInfo.sigHashes, this.mCurrentPackage, pmi)) {
            Slog.w(BackupManagerService.TAG, "Signature mismatch restoring " + packageName);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 29, this.mCurrentPackage, 3, null);
            EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, packageName, "Signature mismatch");
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            return;
        }
        IBackupAgent bindToAgentSynchronous = this.backupManagerService.bindToAgentSynchronous(this.mCurrentPackage.applicationInfo, 0, this.mBackupEligibilityRules.getOperationType());
        this.mAgent = bindToAgentSynchronous;
        if (bindToAgentSynchronous == null) {
            Slog.w(BackupManagerService.TAG, "Can't find backup agent for " + packageName);
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 30, this.mCurrentPackage, 3, null);
            EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, packageName, "Restore agent missing");
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
            return;
        }
        this.mDidLaunch = true;
        try {
            initiateOneRestore(this.mCurrentPackage, metaInfo.versionCode);
            this.mCount++;
        } catch (Exception e) {
            Slog.e(BackupManagerService.TAG, "Error when attempting restore: " + e.toString());
            keyValueAgentErrorCleanup(false);
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    private void initiateOneRestore(PackageInfo app, long appVersionCode) {
        ParcelFileDescriptor stage;
        String packageName = app.packageName;
        Slog.d(BackupManagerService.TAG, "initiateOneRestore packageName=" + packageName);
        this.mBackupDataName = new File(this.backupManagerService.getDataDir(), packageName + ".restore");
        this.mStageName = new File(this.backupManagerService.getDataDir(), packageName + ".stage");
        this.mNewStateName = new File(this.mStateDir, packageName + KeyValueBackupTask.NEW_STATE_FILE_SUFFIX);
        boolean staging = shouldStageBackupData(packageName);
        File downloadFile = staging ? this.mStageName : this.mBackupDataName;
        try {
            BackupTransportClient transport = this.mTransportConnection.connectOrThrow("PerformUnifiedRestoreTask.initiateOneRestore()");
            ParcelFileDescriptor stage2 = ParcelFileDescriptor.open(downloadFile, 1006632960);
            if (transport.getRestoreData(stage2) != 0) {
                Slog.e(BackupManagerService.TAG, "Error getting restore data for " + packageName);
                EventLog.writeEvent((int) EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                stage2.close();
                downloadFile.delete();
                executeNextState(UnifiedRestoreState.FINAL);
                return;
            }
            if (!staging) {
                stage = stage2;
            } else {
                stage2.close();
                ParcelFileDescriptor stage3 = ParcelFileDescriptor.open(downloadFile, 268435456);
                this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
                BackupDataInput in = new BackupDataInput(stage3.getFileDescriptor());
                BackupDataOutput out = new BackupDataOutput(this.mBackupData.getFileDescriptor());
                filterExcludedKeys(packageName, in, out);
                this.mBackupData.close();
                stage = stage3;
            }
            stage.close();
            this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 268435456);
            this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
            long restoreAgentTimeoutMillis = this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis(app.applicationInfo.uid);
            this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, restoreAgentTimeoutMillis, this, 1);
            this.mAgent.doRestoreWithExcludedKeys(this.mBackupData, appVersionCode, this.mNewState, this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder(), new ArrayList(getExcludedKeysForPackage(packageName)));
        } catch (Exception e) {
            Slog.e(BackupManagerService.TAG, "Unable to call app for restore: " + packageName, e);
            EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, packageName, e.toString());
            keyValueAgentErrorCleanup(false);
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    boolean shouldStageBackupData(String packageName) {
        return (packageName.equals(PackageManagerService.PLATFORM_PACKAGE_NAME) && getExcludedKeysForPackage(PackageManagerService.PLATFORM_PACKAGE_NAME).isEmpty()) ? false : true;
    }

    Set<String> getExcludedKeysForPackage(String packageName) {
        return this.backupManagerService.getExcludedRestoreKeys(packageName);
    }

    void filterExcludedKeys(String packageName, BackupDataInput in, BackupDataOutput out) throws Exception {
        Set<String> excludedKeysForPackage = getExcludedKeysForPackage(packageName);
        byte[] buffer = new byte[8192];
        while (in.readNextHeader()) {
            String key = in.getKey();
            int size = in.getDataSize();
            if (excludedKeysForPackage != null && excludedKeysForPackage.contains(key)) {
                Slog.i(BackupManagerService.TAG, "Skipping blocked key " + key);
                in.skipEntityData();
            } else if (key.equals(UserBackupManagerService.KEY_WIDGET_STATE)) {
                Slog.i(BackupManagerService.TAG, "Restoring widget state for " + packageName);
                byte[] bArr = new byte[size];
                this.mWidgetData = bArr;
                in.readEntityData(bArr, 0, size);
            } else {
                if (size > buffer.length) {
                    buffer = new byte[size];
                }
                in.readEntityData(buffer, 0, size);
                out.writeEntityHeader(key, size);
                out.writeEntityData(buffer, size);
            }
        }
    }

    private void restoreFull() {
        try {
            StreamFeederThread feeder = new StreamFeederThread();
            new Thread(feeder, "unified-stream-feeder").start();
        } catch (IOException e) {
            Slog.e(BackupManagerService.TAG, "Unable to construct pipes for stream restore!");
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    private void restoreFinished() {
        Slog.d(BackupManagerService.TAG, "restoreFinished packageName=" + this.mCurrentPackage.packageName);
        try {
            long restoreAgentFinishedTimeoutMillis = this.mAgentTimeoutParameters.getRestoreAgentFinishedTimeoutMillis();
            this.backupManagerService.prepareOperationTimeout(this.mEphemeralOpToken, restoreAgentFinishedTimeoutMillis, this, 1);
            this.mAgent.doRestoreFinished(this.mEphemeralOpToken, this.backupManagerService.getBackupManagerBinder());
        } catch (Exception e) {
            String packageName = this.mCurrentPackage.packageName;
            Slog.e(BackupManagerService.TAG, "Unable to finalize restore of " + packageName);
            EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, packageName, e.toString());
            keyValueAgentErrorCleanup(true);
            executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class StreamFeederThread extends RestoreEngine implements Runnable, BackupRestoreTask {
        FullRestoreEngine mEngine;
        FullRestoreEngineThread mEngineThread;
        private final int mEphemeralOpToken;
        final String TAG = "StreamFeederThread";
        ParcelFileDescriptor[] mTransportPipes = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor[] mEnginePipes = ParcelFileDescriptor.createPipe();

        public StreamFeederThread() throws IOException {
            this.mEphemeralOpToken = PerformUnifiedRestoreTask.this.backupManagerService.generateRandomIntegerToken();
            setRunning(true);
        }

        /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1027=4, 1029=4, 1033=4, 1036=4, 1041=4, 1042=4, 1043=4, 1044=4, 1047=4, 1048=4, 1053=4, 1057=4, 1058=4, 1011=5, 1012=4, 1013=4, 1016=4, 1019=4, 1023=4] */
        /* JADX WARN: Code restructure failed: missing block: B:53:0x024e, code lost:
            if (r1 == 64536) goto L81;
         */
        /* JADX WARN: Code restructure failed: missing block: B:54:0x0250, code lost:
            r0 = com.android.server.backup.restore.UnifiedRestoreState.FINAL;
         */
        /* JADX WARN: Code restructure failed: missing block: B:55:0x0256, code lost:
            r0 = com.android.server.backup.restore.UnifiedRestoreState.RUNNING_QUEUE;
         */
        /* JADX WARN: Code restructure failed: missing block: B:70:0x0306, code lost:
            if (r1 == 64536) goto L81;
         */
        @Override // java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void run() {
            char c;
            UnifiedRestoreState nextState;
            int status;
            UnifiedRestoreState nextState2;
            int status2;
            BackupTransportClient transport;
            UnifiedRestoreState unifiedRestoreState = UnifiedRestoreState.RUNNING_QUEUE;
            int status3 = 0;
            EventLog.writeEvent((int) EventLogTags.FULL_RESTORE_PACKAGE, PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
            FullRestoreEngine fullRestoreEngine = new FullRestoreEngine(PerformUnifiedRestoreTask.this.backupManagerService, PerformUnifiedRestoreTask.this.mOperationStorage, this, null, PerformUnifiedRestoreTask.this.mMonitor, PerformUnifiedRestoreTask.this.mCurrentPackage, false, this.mEphemeralOpToken, false, PerformUnifiedRestoreTask.this.mBackupEligibilityRules);
            this.mEngine = fullRestoreEngine;
            this.mEngineThread = new FullRestoreEngineThread(fullRestoreEngine, this.mEnginePipes[0]);
            ParcelFileDescriptor eWriteEnd = this.mEnginePipes[1];
            ParcelFileDescriptor[] parcelFileDescriptorArr = this.mTransportPipes;
            ParcelFileDescriptor tReadEnd = parcelFileDescriptorArr[0];
            ParcelFileDescriptor tWriteEnd = parcelFileDescriptorArr[1];
            int bufferSize = 32768;
            byte[] buffer = new byte[32768];
            FileOutputStream engineOut = new FileOutputStream(eWriteEnd.getFileDescriptor());
            FileInputStream transportIn = new FileInputStream(tReadEnd.getFileDescriptor());
            new Thread(this.mEngineThread, "unified-restore-engine").start();
            try {
                try {
                    BackupTransportClient transport2 = PerformUnifiedRestoreTask.this.mTransportConnection.connectOrThrow("PerformUnifiedRestoreTask$StreamFeederThread.run()");
                    while (true) {
                        if (status3 != 0) {
                            break;
                        }
                        int result = transport2.getNextFullRestoreDataChunk(tWriteEnd);
                        if (result > 0) {
                            if (result > bufferSize) {
                                bufferSize = result;
                                buffer = new byte[bufferSize];
                            }
                            int toCopy = result;
                            while (toCopy > 0) {
                                int n = transportIn.read(buffer, 0, toCopy);
                                engineOut.write(buffer, 0, n);
                                toCopy -= n;
                                transport2 = transport2;
                            }
                            transport = transport2;
                        } else {
                            transport = transport2;
                            if (result == -1) {
                                status3 = 0;
                                break;
                            }
                            Slog.e("StreamFeederThread", "Error " + result + " streaming restore for " + PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                            EventLog.writeEvent((int) EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                            status3 = result;
                        }
                        transport2 = transport;
                    }
                    IoUtils.closeQuietly(this.mEnginePipes[1]);
                    IoUtils.closeQuietly(this.mTransportPipes[0]);
                    IoUtils.closeQuietly(this.mTransportPipes[1]);
                    this.mEngineThread.waitForResult();
                    IoUtils.closeQuietly(this.mEnginePipes[0]);
                    PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                    if (status3 == 0) {
                        nextState = UnifiedRestoreState.RESTORE_FINISHED;
                        PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                        PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                    } else {
                        try {
                            BackupTransportClient transport3 = PerformUnifiedRestoreTask.this.mTransportConnection.connectOrThrow("PerformUnifiedRestoreTask$StreamFeederThread.run()");
                            transport3.abortFullRestore();
                            status2 = status3;
                        } catch (Exception e) {
                            Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e.getMessage());
                            status2 = -1000;
                        }
                        PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataAfterRestoreFailure(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                        if (status2 == -1000) {
                            nextState = UnifiedRestoreState.FINAL;
                        } else {
                            UnifiedRestoreState nextState3 = UnifiedRestoreState.RUNNING_QUEUE;
                            nextState = nextState3;
                        }
                    }
                } catch (Throwable th) {
                    IoUtils.closeQuietly(this.mEnginePipes[1]);
                    IoUtils.closeQuietly(this.mTransportPipes[0]);
                    IoUtils.closeQuietly(this.mTransportPipes[1]);
                    this.mEngineThread.waitForResult();
                    IoUtils.closeQuietly(this.mEnginePipes[0]);
                    PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                    if (status3 != 0) {
                        try {
                            BackupTransportClient transport4 = PerformUnifiedRestoreTask.this.mTransportConnection.connectOrThrow("PerformUnifiedRestoreTask$StreamFeederThread.run()");
                            transport4.abortFullRestore();
                            status = status3;
                        } catch (Exception e2) {
                            Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e2.getMessage());
                            status = -1000;
                        }
                        PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataAfterRestoreFailure(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
                        nextState2 = status == -1000 ? UnifiedRestoreState.FINAL : UnifiedRestoreState.RUNNING_QUEUE;
                    } else {
                        nextState2 = UnifiedRestoreState.RESTORE_FINISHED;
                        PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                        PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                    }
                    PerformUnifiedRestoreTask.this.executeNextState(nextState2);
                    setRunning(false);
                    throw th;
                }
            } catch (IOException e3) {
                Slog.e("StreamFeederThread", "Unable to route data for restore");
                EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, PerformUnifiedRestoreTask.this.mCurrentPackage.packageName, "I/O error on pipes");
                c = 64533;
                IoUtils.closeQuietly(this.mEnginePipes[1]);
                IoUtils.closeQuietly(this.mTransportPipes[0]);
                IoUtils.closeQuietly(this.mTransportPipes[1]);
                this.mEngineThread.waitForResult();
                IoUtils.closeQuietly(this.mEnginePipes[0]);
                PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                if (-1003 == 0) {
                    nextState = UnifiedRestoreState.RESTORE_FINISHED;
                    PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                    PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                    PerformUnifiedRestoreTask.this.executeNextState(nextState);
                    setRunning(false);
                }
                try {
                    BackupTransportClient transport5 = PerformUnifiedRestoreTask.this.mTransportConnection.connectOrThrow("PerformUnifiedRestoreTask$StreamFeederThread.run()");
                    transport5.abortFullRestore();
                } catch (Exception e4) {
                    Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e4.getMessage());
                    c = 64536;
                }
                PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataAfterRestoreFailure(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
            } catch (Exception e5) {
                Slog.e("StreamFeederThread", "Transport failed during restore: " + e5.getMessage());
                EventLog.writeEvent((int) EventLogTags.RESTORE_TRANSPORT_FAILURE, new Object[0]);
                c = 64536;
                IoUtils.closeQuietly(this.mEnginePipes[1]);
                IoUtils.closeQuietly(this.mTransportPipes[0]);
                IoUtils.closeQuietly(this.mTransportPipes[1]);
                this.mEngineThread.waitForResult();
                IoUtils.closeQuietly(this.mEnginePipes[0]);
                PerformUnifiedRestoreTask.this.mDidLaunch = this.mEngine.getAgent() != null;
                if (-1000 == 0) {
                    nextState = UnifiedRestoreState.RESTORE_FINISHED;
                    PerformUnifiedRestoreTask.this.mAgent = this.mEngine.getAgent();
                    PerformUnifiedRestoreTask.this.mWidgetData = this.mEngine.getWidgetData();
                    PerformUnifiedRestoreTask.this.executeNextState(nextState);
                    setRunning(false);
                }
                try {
                    BackupTransportClient transport6 = PerformUnifiedRestoreTask.this.mTransportConnection.connectOrThrow("PerformUnifiedRestoreTask$StreamFeederThread.run()");
                    transport6.abortFullRestore();
                } catch (Exception e6) {
                    Slog.e("StreamFeederThread", "Transport threw from abortFullRestore: " + e6.getMessage());
                    c = 64536;
                }
                PerformUnifiedRestoreTask.this.backupManagerService.clearApplicationDataAfterRestoreFailure(PerformUnifiedRestoreTask.this.mCurrentPackage.packageName);
            }
            PerformUnifiedRestoreTask.this.executeNextState(nextState);
            setRunning(false);
        }

        @Override // com.android.server.backup.BackupRestoreTask
        public void execute() {
        }

        @Override // com.android.server.backup.BackupRestoreTask
        public void operationComplete(long result) {
        }

        @Override // com.android.server.backup.BackupRestoreTask
        public void handleCancel(boolean cancelAll) {
            PerformUnifiedRestoreTask.this.mOperationStorage.removeOperation(this.mEphemeralOpToken);
            Slog.w("StreamFeederThread", "Full-data restore target timed out; shutting down");
            PerformUnifiedRestoreTask performUnifiedRestoreTask = PerformUnifiedRestoreTask.this;
            performUnifiedRestoreTask.mMonitor = BackupManagerMonitorUtils.monitorEvent(performUnifiedRestoreTask.mMonitor, 45, PerformUnifiedRestoreTask.this.mCurrentPackage, 2, null);
            this.mEngineThread.handleTimeout();
            IoUtils.closeQuietly(this.mEnginePipes[1]);
            ParcelFileDescriptor[] parcelFileDescriptorArr = this.mEnginePipes;
            parcelFileDescriptorArr[1] = null;
            IoUtils.closeQuietly(parcelFileDescriptorArr[0]);
            this.mEnginePipes[0] = null;
        }
    }

    private void finalizeRestore() {
        PackageManagerBackupAgent packageManagerBackupAgent;
        try {
            BackupTransportClient transport = this.mTransportConnection.connectOrThrow("PerformUnifiedRestoreTask.finalizeRestore()");
            transport.finishRestore();
        } catch (Exception e) {
            Slog.e(BackupManagerService.TAG, "Error finishing restore", e);
        }
        IRestoreObserver iRestoreObserver = this.mObserver;
        if (iRestoreObserver != null) {
            try {
                iRestoreObserver.restoreFinished(this.mStatus);
            } catch (RemoteException e2) {
                Slog.d(BackupManagerService.TAG, "Restore observer died at restoreFinished");
            }
        }
        this.backupManagerService.getBackupHandler().removeMessages(8);
        if (this.mPmToken > 0) {
            try {
                this.backupManagerService.getPackageManagerBinder().finishPackageInstall(this.mPmToken, this.mDidLaunch);
            } catch (RemoteException e3) {
            }
        } else {
            long restoreAgentTimeoutMillis = this.mAgentTimeoutParameters.getRestoreSessionTimeoutMillis();
            this.backupManagerService.getBackupHandler().sendEmptyMessageDelayed(8, restoreAgentTimeoutMillis);
        }
        if (this.mIsSystemRestore) {
            AppWidgetBackupBridge.systemRestoreFinished(this.mUserId);
        }
        if (this.mIsSystemRestore && (packageManagerBackupAgent = this.mPmAgent) != null) {
            this.backupManagerService.setAncestralPackages(packageManagerBackupAgent.getRestoredPackages());
            this.backupManagerService.setAncestralToken(this.mToken);
            this.backupManagerService.setAncestralOperationType(this.mBackupEligibilityRules.getOperationType());
            this.backupManagerService.writeRestoreTokens();
        }
        synchronized (this.backupManagerService.getPendingRestores()) {
            if (this.backupManagerService.getPendingRestores().size() > 0) {
                Slog.d(BackupManagerService.TAG, "Starting next pending restore.");
                PerformUnifiedRestoreTask task = this.backupManagerService.getPendingRestores().remove();
                this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(20, task));
            } else {
                this.backupManagerService.setRestoreInProgress(false);
            }
        }
        Slog.i(BackupManagerService.TAG, "Restore complete.");
        this.mListener.onFinished("PerformUnifiedRestoreTask.finalizeRestore()");
    }

    void keyValueAgentErrorCleanup(boolean clearAppData) {
        if (clearAppData) {
            this.backupManagerService.clearApplicationDataAfterRestoreFailure(this.mCurrentPackage.packageName);
        }
        keyValueAgentCleanup();
    }

    void keyValueAgentCleanup() {
        this.mBackupDataName.delete();
        this.mStageName.delete();
        try {
            ParcelFileDescriptor parcelFileDescriptor = this.mBackupData;
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
        } catch (IOException e) {
        }
        try {
            ParcelFileDescriptor parcelFileDescriptor2 = this.mNewState;
            if (parcelFileDescriptor2 != null) {
                parcelFileDescriptor2.close();
            }
        } catch (IOException e2) {
        }
        this.mNewState = null;
        this.mBackupData = null;
        this.mNewStateName.delete();
        if (this.mCurrentPackage.applicationInfo != null) {
            try {
                this.backupManagerService.getActivityManager().unbindBackupAgent(this.mCurrentPackage.applicationInfo);
                int appFlags = this.mCurrentPackage.applicationInfo.flags;
                boolean killAfterRestore = !UserHandle.isCore(this.mCurrentPackage.applicationInfo.uid) && (this.mRestoreDescription.getDataType() == 2 || (65536 & appFlags) != 0);
                if (this.mTargetPackage == null && killAfterRestore) {
                    Slog.d(BackupManagerService.TAG, "Restore complete, killing host process of " + this.mCurrentPackage.applicationInfo.processName);
                    this.backupManagerService.getActivityManager().killApplicationProcess(this.mCurrentPackage.applicationInfo.processName, this.mCurrentPackage.applicationInfo.uid);
                }
            } catch (RemoteException e3) {
            }
        }
        this.backupManagerService.getBackupHandler().removeMessages(18, this);
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void operationComplete(long unusedResult) {
        UnifiedRestoreState nextState;
        this.mOperationStorage.removeOperation(this.mEphemeralOpToken);
        switch (AnonymousClass1.$SwitchMap$com$android$server$backup$restore$UnifiedRestoreState[this.mState.ordinal()]) {
            case 1:
                nextState = UnifiedRestoreState.RUNNING_QUEUE;
                break;
            case 2:
            default:
                Slog.e(BackupManagerService.TAG, "Unexpected restore callback into state " + this.mState);
                keyValueAgentErrorCleanup(true);
                nextState = UnifiedRestoreState.FINAL;
                break;
            case 3:
            case 4:
                nextState = UnifiedRestoreState.RESTORE_FINISHED;
                break;
            case 5:
                int size = (int) this.mBackupDataName.length();
                EventLog.writeEvent((int) EventLogTags.RESTORE_PACKAGE, this.mCurrentPackage.packageName, Integer.valueOf(size));
                keyValueAgentCleanup();
                if (this.mWidgetData != null) {
                    this.backupManagerService.restoreWidgetData(this.mCurrentPackage.packageName, this.mWidgetData);
                }
                nextState = UnifiedRestoreState.RUNNING_QUEUE;
                break;
        }
        executeNextState(nextState);
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void handleCancel(boolean cancelAll) {
        this.mOperationStorage.removeOperation(this.mEphemeralOpToken);
        Slog.e(BackupManagerService.TAG, "Timeout restoring application " + this.mCurrentPackage.packageName);
        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 31, this.mCurrentPackage, 2, null);
        EventLog.writeEvent((int) EventLogTags.RESTORE_AGENT_FAILURE, this.mCurrentPackage.packageName, "restore timeout");
        keyValueAgentErrorCleanup(true);
        executeNextState(UnifiedRestoreState.RUNNING_QUEUE);
    }

    void executeNextState(UnifiedRestoreState nextState) {
        this.mState = nextState;
        Message msg = this.backupManagerService.getBackupHandler().obtainMessage(20, this);
        this.backupManagerService.getBackupHandler().sendMessage(msg);
    }

    void sendStartRestore(int numPackages) {
        IRestoreObserver iRestoreObserver = this.mObserver;
        if (iRestoreObserver != null) {
            try {
                iRestoreObserver.restoreStarting(numPackages);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "Restore observer went away: startRestore");
                this.mObserver = null;
            }
        }
    }

    void sendOnRestorePackage(String name) {
        IRestoreObserver iRestoreObserver = this.mObserver;
        if (iRestoreObserver != null) {
            try {
                iRestoreObserver.onUpdate(this.mCount, name);
            } catch (RemoteException e) {
                Slog.d(BackupManagerService.TAG, "Restore observer died in onUpdate");
                this.mObserver = null;
            }
        }
    }

    void sendEndRestore() {
        IRestoreObserver iRestoreObserver = this.mObserver;
        if (iRestoreObserver != null) {
            try {
                iRestoreObserver.restoreFinished(this.mStatus);
            } catch (RemoteException e) {
                Slog.w(BackupManagerService.TAG, "Restore observer went away: endRestore");
                this.mObserver = null;
            }
        }
    }
}
