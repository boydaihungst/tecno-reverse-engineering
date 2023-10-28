package com.android.server.backup.keyvalue;

import android.app.IBackupAgent;
import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.IBackupCallback;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.ConditionVariable;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.WorkSource;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.DataChangedJournal;
import com.android.server.backup.KeyValueBackupJob;
import com.android.server.backup.OperationStorage;
import com.android.server.backup.UserBackupManagerService;
import com.android.server.backup.fullbackup.PerformFullTransportBackupTask;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.remote.RemoteCall;
import com.android.server.backup.remote.RemoteCallable;
import com.android.server.backup.remote.RemoteResult;
import com.android.server.backup.transport.BackupTransportClient;
import com.android.server.backup.transport.TransportConnection;
import com.android.server.backup.transport.TransportNotAvailableException;
import com.android.server.backup.utils.BackupEligibilityRules;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public class KeyValueBackupTask implements BackupRestoreTask, Runnable {
    private static final String BLANK_STATE_FILE_NAME = "blank_state";
    public static final String NEW_STATE_FILE_SUFFIX = ".new";
    static final String NO_DATA_END_SENTINEL = "@end@";
    private static final String PM_PACKAGE = "@pm@";
    public static final String STAGING_FILE_SUFFIX = ".data";
    private static final String SUCCESS_STATE_SUBDIR = "backing-up";
    private static final String TAG = "KVBT";
    private static final AtomicInteger THREAD_COUNT = new AtomicInteger();
    private static final int THREAD_PRIORITY = 10;
    private IBackupAgent mAgent;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private ParcelFileDescriptor mBackupData;
    private File mBackupDataFile;
    private final BackupEligibilityRules mBackupEligibilityRules;
    private final UserBackupManagerService mBackupManagerService;
    private final File mBlankStateFile;
    private final ConditionVariable mCancelAcknowledged = new ConditionVariable(false);
    private volatile boolean mCancelled = false;
    private final int mCurrentOpToken;
    private PackageInfo mCurrentPackage;
    private final File mDataDirectory;
    private PerformFullTransportBackupTask mFullBackupTask;
    private boolean mHasDataToBackup;
    private final DataChangedJournal mJournal;
    private ParcelFileDescriptor mNewState;
    private File mNewStateFile;
    private boolean mNonIncremental;
    private final OperationStorage mOperationStorage;
    private final List<String> mOriginalQueue;
    private final PackageManager mPackageManager;
    private volatile RemoteCall mPendingCall;
    private final List<String> mPendingFullBackups;
    private final List<String> mQueue;
    private final Object mQueueLock;
    private final KeyValueBackupReporter mReporter;
    private ParcelFileDescriptor mSavedState;
    private File mSavedStateFile;
    private final File mStateDirectory;
    private final OnTaskFinishedListener mTaskFinishedListener;
    private final TransportConnection mTransportConnection;
    private final int mUserId;
    private final boolean mUserInitiated;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface StateTransaction {
        public static final int COMMIT_NEW = 0;
        public static final int DISCARD_ALL = 2;
        public static final int DISCARD_NEW = 1;
    }

    public static KeyValueBackupTask start(UserBackupManagerService backupManagerService, OperationStorage operationStorage, TransportConnection transportConnection, String transportDirName, List<String> queue, DataChangedJournal dataChangedJournal, IBackupObserver observer, IBackupManagerMonitor monitor, OnTaskFinishedListener listener, List<String> pendingFullBackups, boolean userInitiated, boolean nonIncremental, BackupEligibilityRules backupEligibilityRules) {
        KeyValueBackupReporter reporter = new KeyValueBackupReporter(backupManagerService, observer, monitor);
        KeyValueBackupTask task = new KeyValueBackupTask(backupManagerService, operationStorage, transportConnection, transportDirName, queue, dataChangedJournal, reporter, listener, pendingFullBackups, userInitiated, nonIncremental, backupEligibilityRules);
        Thread thread = new Thread(task, "key-value-backup-" + THREAD_COUNT.incrementAndGet());
        thread.start();
        KeyValueBackupReporter.onNewThread(thread.getName());
        return task;
    }

    public KeyValueBackupTask(UserBackupManagerService backupManagerService, OperationStorage operationStorage, TransportConnection transportConnection, String transportDirName, List<String> queue, DataChangedJournal journal, KeyValueBackupReporter reporter, OnTaskFinishedListener taskFinishedListener, List<String> pendingFullBackups, boolean userInitiated, boolean nonIncremental, BackupEligibilityRules backupEligibilityRules) {
        this.mBackupManagerService = backupManagerService;
        this.mOperationStorage = operationStorage;
        this.mPackageManager = backupManagerService.getPackageManager();
        this.mTransportConnection = transportConnection;
        this.mOriginalQueue = queue;
        this.mQueue = new ArrayList(queue);
        this.mJournal = journal;
        this.mReporter = reporter;
        this.mTaskFinishedListener = taskFinishedListener;
        this.mPendingFullBackups = pendingFullBackups;
        this.mUserInitiated = userInitiated;
        this.mNonIncremental = nonIncremental;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Objects.requireNonNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        File file = new File(backupManagerService.getBaseStateDir(), transportDirName);
        this.mStateDirectory = file;
        this.mDataDirectory = backupManagerService.getDataDir();
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mQueueLock = backupManagerService.getQueueLock();
        this.mBlankStateFile = new File(file, BLANK_STATE_FILE_NAME);
        this.mUserId = backupManagerService.getUserId();
        this.mBackupEligibilityRules = backupEligibilityRules;
    }

    private void registerTask() {
        this.mOperationStorage.registerOperation(this.mCurrentOpToken, 0, this, 2);
    }

    private void unregisterTask() {
        this.mOperationStorage.removeOperation(this.mCurrentOpToken);
    }

    @Override // java.lang.Runnable
    public void run() {
        Process.setThreadPriority(10);
        this.mHasDataToBackup = false;
        Set<String> backedUpApps = new HashSet<>();
        int status = 0;
        try {
            startTask();
            while (!this.mQueue.isEmpty() && !this.mCancelled) {
                String packageName = this.mQueue.remove(0);
                try {
                    if ("@pm@".equals(packageName)) {
                        backupPm();
                    } else {
                        backupPackage(packageName);
                    }
                    setSuccessState(packageName, true);
                    backedUpApps.add(packageName);
                } catch (AgentException e) {
                    setSuccessState(packageName, false);
                    if (e.isTransitory()) {
                        this.mBackupManagerService.dataChangedImpl(packageName);
                    }
                }
            }
            informTransportOfUnchangedApps(backedUpApps);
        } catch (TaskException e2) {
            if (e2.isStateCompromised()) {
                this.mBackupManagerService.resetBackupState(this.mStateDirectory);
            }
            revertTask();
            status = e2.getStatus();
        }
        finishTask(status);
    }

    private void informTransportOfUnchangedApps(Set<String> appsBackedUp) {
        String[] succeedingPackages = getSucceedingPackages();
        if (succeedingPackages == null) {
            return;
        }
        int flags = 8;
        if (this.mUserInitiated) {
            flags = 8 | 1;
        }
        boolean noDataPackageEncountered = false;
        try {
            BackupTransportClient transport = this.mTransportConnection.connectOrThrow("KVBT.informTransportOfEmptyBackups()");
            for (String packageName : succeedingPackages) {
                if (appsBackedUp.contains(packageName)) {
                    Log.v(TAG, "Skipping package which was backed up this time: " + packageName);
                } else {
                    try {
                        PackageInfo packageInfo = this.mPackageManager.getPackageInfo(packageName, 0);
                        if (!isEligibleForNoDataCall(packageInfo)) {
                            clearStatus(packageName);
                        } else {
                            sendNoDataChangedTo(transport, packageInfo, flags);
                            noDataPackageEncountered = true;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        clearStatus(packageName);
                    }
                }
            }
            if (noDataPackageEncountered) {
                PackageInfo endSentinal = new PackageInfo();
                endSentinal.packageName = NO_DATA_END_SENTINEL;
                sendNoDataChangedTo(transport, endSentinal, flags);
            }
        } catch (RemoteException | TransportNotAvailableException e2) {
            Log.e(TAG, "Could not inform transport of all unchanged apps", e2);
        }
    }

    private boolean isEligibleForNoDataCall(PackageInfo packageInfo) {
        return this.mBackupEligibilityRules.appIsKeyValueOnly(packageInfo) && this.mBackupEligibilityRules.appIsRunningAndEligibleForBackupWithTransport(this.mTransportConnection, packageInfo.packageName);
    }

    private void sendNoDataChangedTo(BackupTransportClient transport, PackageInfo packageInfo, int flags) throws RemoteException {
        try {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(this.mBlankStateFile, AudioFormat.MP2);
            try {
                int result = transport.performBackup(packageInfo, pfd, flags);
                if (result != -1000 && result != -1001) {
                    transport.finishBackup();
                    return;
                }
                Log.w(TAG, "Aborting informing transport of unchanged apps, transport errored");
            } finally {
                IoUtils.closeQuietly(pfd);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to find blank state file, aborting unchanged apps signal.");
        }
    }

    private String[] getSucceedingPackages() {
        File stateDirectory = getTopLevelSuccessStateDirectory(false);
        if (stateDirectory == null) {
            return null;
        }
        return stateDirectory.list();
    }

    private void setSuccessState(String packageName, boolean success) {
        File successStateFile = getSuccessStateFileFor(packageName);
        if (successStateFile != null && successStateFile.exists() != success) {
            if (!success) {
                clearStatus(packageName, successStateFile);
                return;
            }
            try {
                if (!successStateFile.createNewFile()) {
                    Log.w(TAG, "Unable to permanently record success for " + packageName);
                }
            } catch (IOException e) {
                Log.w(TAG, "Unable to permanently record success for " + packageName, e);
            }
        }
    }

    private void clearStatus(String packageName) {
        File successStateFile = getSuccessStateFileFor(packageName);
        if (successStateFile == null) {
            return;
        }
        clearStatus(packageName, successStateFile);
    }

    private void clearStatus(String packageName, File successStateFile) {
        if (successStateFile.exists() && !successStateFile.delete()) {
            Log.w(TAG, "Unable to remove status file for " + packageName);
        }
    }

    private File getSuccessStateFileFor(String packageName) {
        File stateDirectory = getTopLevelSuccessStateDirectory(true);
        if (stateDirectory == null) {
            return null;
        }
        return new File(stateDirectory, packageName);
    }

    private File getTopLevelSuccessStateDirectory(boolean createIfMissing) {
        File directory = new File(this.mStateDirectory, SUCCESS_STATE_SUBDIR);
        if (!directory.exists() && createIfMissing && !directory.mkdirs()) {
            Log.e(TAG, "Unable to create backing-up state directory");
            return null;
        }
        return directory;
    }

    private int sendDataToTransport(PackageInfo packageInfo) throws AgentException, TaskException {
        try {
            return sendDataToTransport();
        } catch (IOException e) {
            this.mReporter.onAgentDataError(packageInfo.packageName, e);
            throw TaskException.causedBy(e);
        }
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void execute() {
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void operationComplete(long unusedResult) {
    }

    private void startTask() throws TaskException {
        if (this.mBackupManagerService.isBackupOperationInProgress()) {
            this.mReporter.onSkipBackup();
            throw TaskException.create();
        }
        this.mFullBackupTask = createFullBackupTask(this.mPendingFullBackups);
        registerTask();
        if (this.mQueue.isEmpty() && this.mPendingFullBackups.isEmpty()) {
            this.mReporter.onEmptyQueueAtStart();
            return;
        }
        boolean backupPm = this.mQueue.remove("@pm@") || !this.mNonIncremental;
        if (backupPm) {
            this.mQueue.add(0, "@pm@");
        } else {
            this.mReporter.onSkipPm();
        }
        this.mReporter.onQueueReady(this.mQueue);
        File pmState = new File(this.mStateDirectory, "@pm@");
        try {
            BackupTransportClient transport = this.mTransportConnection.connectOrThrow("KVBT.startTask()");
            String transportName = transport.name();
            if (transportName.contains("EncryptedLocalTransport")) {
                this.mNonIncremental = true;
            }
            this.mReporter.onTransportReady(transportName);
            if (pmState.length() <= 0) {
                this.mReporter.onInitializeTransport(transportName);
                this.mBackupManagerService.resetBackupState(this.mStateDirectory);
                int status = transport.initializeDevice();
                this.mReporter.onTransportInitialized(status);
                if (status != 0) {
                    throw TaskException.stateCompromised();
                }
            }
        } catch (TaskException e) {
            throw e;
        } catch (Exception e2) {
            this.mReporter.onInitializeTransportError(e2);
            throw TaskException.stateCompromised();
        }
    }

    private PerformFullTransportBackupTask createFullBackupTask(List<String> packages) {
        return new PerformFullTransportBackupTask(this.mBackupManagerService, this.mOperationStorage, this.mTransportConnection, null, (String[]) packages.toArray(new String[packages.size()]), false, null, new CountDownLatch(1), this.mReporter.getObserver(), this.mReporter.getMonitor(), this.mTaskFinishedListener, this.mUserInitiated, this.mBackupEligibilityRules);
    }

    private void backupPm() throws TaskException {
        this.mReporter.onStartPackageBackup("@pm@");
        PackageInfo packageInfo = new PackageInfo();
        this.mCurrentPackage = packageInfo;
        packageInfo.packageName = "@pm@";
        try {
            try {
                extractPmAgentData(this.mCurrentPackage);
                int status = sendDataToTransport(this.mCurrentPackage);
                cleanUpAgentForTransportStatus(status);
            } catch (TaskException e) {
                throw TaskException.stateCompromised(e);
            }
        } catch (AgentException | TaskException e2) {
            this.mReporter.onExtractPmAgentDataError(e2);
            cleanUpAgentForError(e2);
            if (e2 instanceof TaskException) {
                throw ((TaskException) e2);
            }
            throw TaskException.stateCompromised(e2);
        }
    }

    private void backupPackage(String packageName) throws AgentException, TaskException {
        this.mReporter.onStartPackageBackup(packageName);
        PackageInfo packageForBackup = getPackageForBackup(packageName);
        this.mCurrentPackage = packageForBackup;
        try {
            extractAgentData(packageForBackup);
            int status = sendDataToTransport(this.mCurrentPackage);
            cleanUpAgentForTransportStatus(status);
        } catch (AgentException | TaskException e) {
            cleanUpAgentForError(e);
            throw e;
        }
    }

    private PackageInfo getPackageForBackup(String packageName) throws AgentException {
        try {
            PackageInfo packageInfo = this.mPackageManager.getPackageInfoAsUser(packageName, 134217728, this.mUserId);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            if (!this.mBackupEligibilityRules.appIsEligibleForBackup(applicationInfo)) {
                this.mReporter.onPackageNotEligibleForBackup(packageName);
                throw AgentException.permanent();
            } else if (this.mBackupEligibilityRules.appGetsFullBackup(packageInfo)) {
                this.mReporter.onPackageEligibleForFullBackup(packageName);
                throw AgentException.permanent();
            } else if (this.mBackupEligibilityRules.appIsStopped(applicationInfo)) {
                this.mReporter.onPackageStopped(packageName);
                throw AgentException.permanent();
            } else {
                return packageInfo;
            }
        } catch (PackageManager.NameNotFoundException e) {
            this.mReporter.onAgentUnknown(packageName);
            throw AgentException.permanent(e);
        }
    }

    private IBackupAgent bindAgent(PackageInfo packageInfo) throws AgentException {
        String packageName = packageInfo.packageName;
        try {
            IBackupAgent agent = this.mBackupManagerService.bindToAgentSynchronous(packageInfo.applicationInfo, 0, this.mBackupEligibilityRules.getOperationType());
            if (agent == null) {
                this.mReporter.onAgentError(packageName);
                throw AgentException.transitory();
            }
            return agent;
        } catch (SecurityException e) {
            this.mReporter.onBindAgentError(packageName, e);
            throw AgentException.transitory(e);
        }
    }

    private void finishTask(int status) {
        for (String packageName : this.mQueue) {
            this.mBackupManagerService.dataChangedImpl(packageName);
        }
        DataChangedJournal dataChangedJournal = this.mJournal;
        if (dataChangedJournal != null && !dataChangedJournal.delete()) {
            this.mReporter.onJournalDeleteFailed(this.mJournal);
        }
        String transportName = null;
        long currentToken = this.mBackupManagerService.getCurrentToken();
        if (this.mHasDataToBackup && status == 0 && currentToken == 0) {
            try {
                BackupTransportClient transport = this.mTransportConnection.connectOrThrow("KVBT.finishTask()");
                transportName = transport.name();
                this.mBackupManagerService.setCurrentToken(transport.getCurrentRestoreSet());
                this.mBackupManagerService.writeRestoreTokens();
            } catch (Exception e) {
                this.mReporter.onSetCurrentTokenError(e);
            }
        }
        synchronized (this.mQueueLock) {
            this.mBackupManagerService.setBackupRunning(false);
            if (status == -1001) {
                this.mReporter.onTransportNotInitialized(transportName);
                try {
                    triggerTransportInitializationLocked();
                } catch (Exception e2) {
                    this.mReporter.onPendingInitializeTransportError(e2);
                    status = -1000;
                }
            }
        }
        unregisterTask();
        this.mReporter.onTaskFinished();
        if (this.mCancelled) {
            this.mCancelAcknowledged.open();
        }
        if (!this.mCancelled && status == 0 && this.mFullBackupTask != null && !this.mPendingFullBackups.isEmpty()) {
            this.mReporter.onStartFullBackup(this.mPendingFullBackups);
            new Thread(this.mFullBackupTask, "full-transport-requested").start();
            return;
        }
        PerformFullTransportBackupTask performFullTransportBackupTask = this.mFullBackupTask;
        if (performFullTransportBackupTask != null) {
            performFullTransportBackupTask.unregisterTask();
        }
        this.mTaskFinishedListener.onFinished("KVBT.finishTask()");
        this.mReporter.onBackupFinished(getBackupFinishedStatus(this.mCancelled, status));
        this.mBackupManagerService.getWakelock().release();
    }

    private int getBackupFinishedStatus(boolean cancelled, int transportStatus) {
        if (cancelled) {
            return -2003;
        }
        switch (transportStatus) {
            case -1005:
            case JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS /* -1002 */:
            case 0:
                return 0;
            default:
                return -1000;
        }
    }

    private void triggerTransportInitializationLocked() throws Exception {
        BackupTransportClient transport = this.mTransportConnection.connectOrThrow("KVBT.triggerTransportInitializationLocked");
        this.mBackupManagerService.getPendingInits().add(transport.name());
        deletePmStateFile();
        this.mBackupManagerService.backupNow();
    }

    private void deletePmStateFile() {
        new File(this.mStateDirectory, "@pm@").delete();
    }

    private void extractPmAgentData(PackageInfo packageInfo) throws AgentException, TaskException {
        Preconditions.checkArgument(packageInfo.packageName.equals("@pm@"));
        BackupAgent pmAgent = this.mBackupManagerService.makeMetadataAgentWithEligibilityRules(this.mBackupEligibilityRules);
        IBackupAgent asInterface = IBackupAgent.Stub.asInterface(pmAgent.onBind());
        this.mAgent = asInterface;
        extractAgentData(packageInfo, asInterface);
    }

    private void extractAgentData(PackageInfo packageInfo) throws AgentException, TaskException {
        this.mBackupManagerService.setWorkSource(new WorkSource(packageInfo.applicationInfo.uid));
        try {
            IBackupAgent bindAgent = bindAgent(packageInfo);
            this.mAgent = bindAgent;
            extractAgentData(packageInfo, bindAgent);
        } finally {
            this.mBackupManagerService.setWorkSource(null);
        }
    }

    private void extractAgentData(PackageInfo packageInfo, final IBackupAgent agent) throws AgentException, TaskException {
        String packageName = packageInfo.packageName;
        this.mReporter.onExtractAgentData(packageName);
        this.mSavedStateFile = new File(this.mStateDirectory, packageName);
        this.mBackupDataFile = new File(this.mDataDirectory, packageName + STAGING_FILE_SUFFIX);
        this.mNewStateFile = new File(this.mStateDirectory, packageName + NEW_STATE_FILE_SUFFIX);
        this.mReporter.onAgentFilesReady(this.mBackupDataFile);
        boolean callingAgent = false;
        try {
            File savedStateFileForAgent = this.mNonIncremental ? this.mBlankStateFile : this.mSavedStateFile;
            this.mSavedState = ParcelFileDescriptor.open(savedStateFileForAgent, AudioFormat.MP2);
            this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataFile, 1006632960);
            this.mNewState = ParcelFileDescriptor.open(this.mNewStateFile, 1006632960);
            if (this.mUserId == 0 && !SELinux.restorecon(this.mBackupDataFile)) {
                this.mReporter.onRestoreconFailed(this.mBackupDataFile);
            }
            BackupTransportClient transport = this.mTransportConnection.connectOrThrow("KVBT.extractAgentData()");
            final long quota = transport.getBackupQuota(packageName, false);
            final int transportFlags = transport.getTransportFlags();
            callingAgent = true;
            RemoteResult agentResult = remoteCall(new RemoteCallable() { // from class: com.android.server.backup.keyvalue.KeyValueBackupTask$$ExternalSyntheticLambda1
                @Override // com.android.server.backup.remote.RemoteCallable
                public final void call(Object obj) {
                    KeyValueBackupTask.this.m2223x289bd4bc(agent, quota, transportFlags, (IBackupCallback) obj);
                }
            }, this.mAgentTimeoutParameters.getKvBackupAgentTimeoutMillis(), "doBackup()");
            checkAgentResult(packageInfo, agentResult);
        } catch (Exception e) {
            this.mReporter.onCallAgentDoBackupError(packageName, callingAgent, e);
            if (callingAgent) {
                throw AgentException.transitory(e);
            }
            throw TaskException.create();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$extractAgentData$0$com-android-server-backup-keyvalue-KeyValueBackupTask  reason: not valid java name */
    public /* synthetic */ void m2223x289bd4bc(IBackupAgent agent, long quota, int transportFlags, IBackupCallback callback) throws RemoteException {
        agent.doBackup(this.mSavedState, this.mBackupData, this.mNewState, quota, callback, transportFlags);
    }

    private void checkAgentResult(PackageInfo packageInfo, RemoteResult result) throws AgentException, TaskException {
        if (result == RemoteResult.FAILED_THREAD_INTERRUPTED) {
            this.mCancelled = true;
            this.mReporter.onAgentCancelled(packageInfo);
            throw TaskException.create();
        } else if (result == RemoteResult.FAILED_CANCELLED) {
            this.mReporter.onAgentCancelled(packageInfo);
            throw TaskException.create();
        } else if (result == RemoteResult.FAILED_TIMED_OUT) {
            this.mReporter.onAgentTimedOut(packageInfo);
            throw AgentException.transitory();
        } else {
            Preconditions.checkState(result.isPresent());
            long resultCode = result.get();
            if (resultCode == -1) {
                this.mReporter.onAgentResultError(packageInfo);
                throw AgentException.transitory();
            } else {
                Preconditions.checkState(resultCode == 0);
            }
        }
    }

    private void agentFail(IBackupAgent agent, String message) {
        try {
            agent.fail(message);
        } catch (Exception e) {
            this.mReporter.onFailAgentError(this.mCurrentPackage.packageName);
        }
    }

    private String SHA1Checksum(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] checksum = md.digest(input);
            StringBuilder string = new StringBuilder(checksum.length * 2);
            for (byte item : checksum) {
                string.append(Integer.toHexString(item));
            }
            return string.toString();
        } catch (NoSuchAlgorithmException e) {
            this.mReporter.onDigestError(e);
            return "00";
        }
    }

    private void writeWidgetPayloadIfAppropriate(FileDescriptor fd, String pkgName) throws IOException {
        byte[] widgetState = AppWidgetBackupBridge.getWidgetState(pkgName, this.mUserId);
        File widgetFile = new File(this.mStateDirectory, pkgName + "_widget");
        boolean priorStateExists = widgetFile.exists();
        if (!priorStateExists && widgetState == null) {
            return;
        }
        this.mReporter.onWriteWidgetData(priorStateExists, widgetState);
        String newChecksum = null;
        if (widgetState != null) {
            newChecksum = SHA1Checksum(widgetState);
            if (priorStateExists) {
                FileInputStream fin = new FileInputStream(widgetFile);
                try {
                    DataInputStream in = new DataInputStream(fin);
                    String priorChecksum = in.readUTF();
                    in.close();
                    fin.close();
                    if (Objects.equals(newChecksum, priorChecksum)) {
                        return;
                    }
                } catch (Throwable th) {
                    try {
                        fin.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                    throw th;
                }
            }
        }
        BackupDataOutput out = new BackupDataOutput(fd);
        if (widgetState == null) {
            out.writeEntityHeader(UserBackupManagerService.KEY_WIDGET_STATE, -1);
            widgetFile.delete();
            return;
        }
        FileOutputStream fout = new FileOutputStream(widgetFile);
        try {
            DataOutputStream stateOut = new DataOutputStream(fout);
            stateOut.writeUTF(newChecksum);
            stateOut.close();
            fout.close();
            out.writeEntityHeader(UserBackupManagerService.KEY_WIDGET_STATE, widgetState.length);
            out.writeEntityData(widgetState, widgetState.length);
        } catch (Throwable th3) {
            try {
                fout.close();
            } catch (Throwable th4) {
                th3.addSuppressed(th4);
            }
            throw th3;
        }
    }

    private int sendDataToTransport() throws AgentException, TaskException, IOException {
        Preconditions.checkState(this.mBackupData != null);
        checkBackupData(this.mCurrentPackage.applicationInfo, this.mBackupDataFile);
        String packageName = this.mCurrentPackage.packageName;
        writeWidgetPayloadIfAppropriate(this.mBackupData.getFileDescriptor(), packageName);
        boolean nonIncremental = this.mSavedStateFile.length() == 0;
        int status = transportPerformBackup(this.mCurrentPackage, this.mBackupDataFile, nonIncremental);
        handleTransportStatus(status, packageName, this.mBackupDataFile.length());
        return status;
    }

    private int transportPerformBackup(PackageInfo packageInfo, File backupDataFile, boolean nonIncremental) throws TaskException {
        String packageName = packageInfo.packageName;
        long size = backupDataFile.length();
        if (size <= 0) {
            this.mReporter.onEmptyData(packageInfo);
            return 0;
        }
        this.mHasDataToBackup = true;
        try {
            ParcelFileDescriptor backupData = ParcelFileDescriptor.open(backupDataFile, 268435456);
            BackupTransportClient transport = this.mTransportConnection.connectOrThrow("KVBT.transportPerformBackup()");
            this.mReporter.onTransportPerformBackup(packageName);
            int flags = getPerformBackupFlags(this.mUserInitiated, nonIncremental);
            int status = transport.performBackup(packageInfo, backupData, flags);
            if (status == 0) {
                status = transport.finishBackup();
            } else if (status == -1001) {
                this.mReporter.onTransportNotInitialized(transport.name());
            }
            if (backupData != null) {
                backupData.close();
            }
            if (nonIncremental && status == -1006) {
                this.mReporter.onPackageBackupNonIncrementalAndNonIncrementalRequired(packageName);
                throw TaskException.create();
            }
            return status;
        } catch (Exception e) {
            this.mReporter.onPackageBackupTransportError(packageName, e);
            throw TaskException.causedBy(e);
        }
    }

    private void handleTransportStatus(int status, String packageName, long size) throws TaskException, AgentException {
        if (status == 0) {
            this.mReporter.onPackageBackupComplete(packageName, size);
        } else if (status == -1006) {
            this.mReporter.onPackageBackupNonIncrementalRequired(this.mCurrentPackage);
            this.mQueue.add(0, packageName);
        } else if (status == -1002) {
            this.mReporter.onPackageBackupRejected(packageName);
            throw AgentException.permanent();
        } else if (status == -1005) {
            this.mReporter.onPackageBackupQuotaExceeded(packageName);
            agentDoQuotaExceeded(this.mAgent, packageName, size);
            throw AgentException.permanent();
        } else {
            this.mReporter.onPackageBackupTransportFailure(packageName);
            throw TaskException.forStatus(status);
        }
    }

    private void agentDoQuotaExceeded(final IBackupAgent agent, String packageName, final long size) {
        if (agent != null) {
            try {
                BackupTransportClient transport = this.mTransportConnection.connectOrThrow("KVBT.agentDoQuotaExceeded()");
                final long quota = transport.getBackupQuota(packageName, false);
                remoteCall(new RemoteCallable() { // from class: com.android.server.backup.keyvalue.KeyValueBackupTask$$ExternalSyntheticLambda0
                    @Override // com.android.server.backup.remote.RemoteCallable
                    public final void call(Object obj) {
                        agent.doQuotaExceeded(size, quota, (IBackupCallback) obj);
                    }
                }, this.mAgentTimeoutParameters.getQuotaExceededTimeoutMillis(), "doQuotaExceeded()");
            } catch (Exception e) {
                this.mReporter.onAgentDoQuotaExceededError(e);
            }
        }
    }

    private void checkBackupData(ApplicationInfo applicationInfo, File backupDataFile) throws IOException, AgentException {
        if (applicationInfo == null || (applicationInfo.flags & 1) != 0) {
            return;
        }
        ParcelFileDescriptor backupData = ParcelFileDescriptor.open(backupDataFile, 268435456);
        try {
            BackupDataInput backupDataInput = new BackupDataInput(backupData.getFileDescriptor());
            while (backupDataInput.readNextHeader()) {
                String key = backupDataInput.getKey();
                if (key != null && key.charAt(0) >= 65280) {
                    this.mReporter.onAgentIllegalKey(this.mCurrentPackage, key);
                    agentFail(this.mAgent, "Illegal backup key: " + key);
                    throw AgentException.permanent();
                }
                backupDataInput.skipEntityData();
            }
            if (backupData != null) {
                backupData.close();
            }
        } catch (Throwable th) {
            if (backupData != null) {
                try {
                    backupData.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private int getPerformBackupFlags(boolean userInitiated, boolean nonIncremental) {
        int incrementalFlag;
        if (nonIncremental) {
            incrementalFlag = 4;
        } else {
            incrementalFlag = 2;
        }
        return (userInitiated ? 1 : 0) | incrementalFlag;
    }

    @Override // com.android.server.backup.BackupRestoreTask
    public void handleCancel(boolean cancelAll) {
        Preconditions.checkArgument(cancelAll, "Can't partially cancel a key-value backup task");
        markCancel();
        waitCancel();
    }

    public void markCancel() {
        this.mReporter.onCancel();
        this.mCancelled = true;
        RemoteCall pendingCall = this.mPendingCall;
        if (pendingCall != null) {
            pendingCall.cancel();
        }
    }

    public void waitCancel() {
        this.mCancelAcknowledged.block();
    }

    private void revertTask() {
        long delay;
        this.mReporter.onRevertTask();
        try {
            BackupTransportClient transport = this.mTransportConnection.connectOrThrow("KVBT.revertTask()");
            delay = transport.requestBackupTime();
        } catch (Exception e) {
            this.mReporter.onTransportRequestBackupTimeError(e);
            delay = 0;
        }
        KeyValueBackupJob.schedule(this.mBackupManagerService.getUserId(), this.mBackupManagerService.getContext(), delay, this.mBackupManagerService.getConstants());
        for (String packageName : this.mOriginalQueue) {
            this.mBackupManagerService.dataChangedImpl(packageName);
        }
    }

    private void cleanUpAgentForError(BackupException exception) {
        cleanUpAgent(1);
    }

    private void cleanUpAgentForTransportStatus(int status) {
        switch (status) {
            case -1006:
                cleanUpAgent(2);
                return;
            case 0:
                cleanUpAgent(0);
                return;
            default:
                throw new AssertionError();
        }
    }

    private void cleanUpAgent(int stateTransaction) {
        applyStateTransaction(stateTransaction);
        File file = this.mBackupDataFile;
        if (file != null) {
            file.delete();
        }
        this.mBlankStateFile.delete();
        this.mSavedStateFile = null;
        this.mBackupDataFile = null;
        this.mNewStateFile = null;
        tryCloseFileDescriptor(this.mSavedState, "old state");
        tryCloseFileDescriptor(this.mBackupData, "backup data");
        tryCloseFileDescriptor(this.mNewState, "new state");
        this.mSavedState = null;
        this.mBackupData = null;
        this.mNewState = null;
        if (this.mCurrentPackage.applicationInfo != null) {
            this.mBackupManagerService.unbindAgent(this.mCurrentPackage.applicationInfo);
        }
        this.mAgent = null;
    }

    private void applyStateTransaction(int stateTransaction) {
        switch (stateTransaction) {
            case 0:
                this.mNewStateFile.renameTo(this.mSavedStateFile);
                return;
            case 1:
                File file = this.mNewStateFile;
                if (file != null) {
                    file.delete();
                    return;
                }
                return;
            case 2:
                this.mSavedStateFile.delete();
                this.mNewStateFile.delete();
                return;
            default:
                throw new IllegalArgumentException("Unknown state transaction " + stateTransaction);
        }
    }

    private void tryCloseFileDescriptor(Closeable closeable, String logName) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                this.mReporter.onCloseFileDescriptorError(logName);
            }
        }
    }

    private RemoteResult remoteCall(RemoteCallable<IBackupCallback> remoteCallable, long timeoutMs, String logIdentifier) throws RemoteException {
        this.mPendingCall = new RemoteCall(this.mCancelled, remoteCallable, timeoutMs);
        RemoteResult result = this.mPendingCall.call();
        this.mReporter.onRemoteCallReturned(result, logIdentifier);
        this.mPendingCall = null;
        return result;
    }
}
