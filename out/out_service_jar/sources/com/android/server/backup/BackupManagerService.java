package com.android.server.backup;

import android.app.backup.IBackupManager;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.app.backup.IRestoreSession;
import android.app.backup.ISelectBackupTransportCallback;
import android.app.compat.CompatChanges;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.utils.RandomAccessFileUtils;
import com.android.server.pm.verify.domain.DomainVerificationPersistence;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes.dex */
public class BackupManagerService extends IBackupManager.Stub {
    private static final String BACKUP_ACTIVATED_FILENAME = "backup-activated";
    private static final String BACKUP_DISABLE_PROPERTY = "ro.backup.disable";
    private static final String BACKUP_SUPPRESS_FILENAME = "backup-suppress";
    private static final String BACKUP_THREAD = "backup";
    public static final boolean DEBUG = true;
    public static final boolean DEBUG_SCHEDULING = true;
    static final String DUMP_RUNNING_USERS_MESSAGE = "Backup Manager is running for users:";
    public static final boolean MORE_DEBUG = false;
    private static final String REMEMBER_ACTIVATED_FILENAME = "backup-remember-activated";
    public static final String TAG = "BackupManagerService";
    static BackupManagerService sInstance;
    private final Context mContext;
    private final boolean mGlobalDisable;
    private final Handler mHandler;
    private final Object mStateLock;
    private final Set<ComponentName> mTransportWhitelist;
    private final UserManager mUserManager;
    private final BroadcastReceiver mUserRemovedReceiver;
    private final SparseArray<UserBackupManagerService> mUserServices;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static BackupManagerService getInstance() {
        return (BackupManagerService) Objects.requireNonNull(sInstance);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.backup.BackupManagerService$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            final int userId;
            if ("android.intent.action.USER_REMOVED".equals(intent.getAction()) && (userId = intent.getIntExtra("android.intent.extra.user_handle", -10000)) > 0) {
                BackupManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.backup.BackupManagerService$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        BackupManagerService.AnonymousClass1.this.m2153x811a3df3(userId);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onReceive$0$com-android-server-backup-BackupManagerService$1  reason: not valid java name */
        public /* synthetic */ void m2153x811a3df3(int userId) {
            BackupManagerService.this.onRemovedNonSystemUser(userId);
        }
    }

    public BackupManagerService(Context context) {
        this(context, new SparseArray());
    }

    BackupManagerService(Context context, SparseArray<UserBackupManagerService> userServices) {
        this.mStateLock = new Object();
        AnonymousClass1 anonymousClass1 = new AnonymousClass1();
        this.mUserRemovedReceiver = anonymousClass1;
        this.mContext = context;
        this.mGlobalDisable = isBackupDisabled();
        HandlerThread handlerThread = new HandlerThread("backup", 10);
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
        this.mUserManager = UserManager.get(context);
        this.mUserServices = userServices;
        Set<ComponentName> transportWhitelist = SystemConfig.getInstance().getBackupTransportWhitelist();
        this.mTransportWhitelist = transportWhitelist == null ? Collections.emptySet() : transportWhitelist;
        context.registerReceiver(anonymousClass1, new IntentFilter("android.intent.action.USER_REMOVED"));
    }

    Handler getBackupHandler() {
        return this.mHandler;
    }

    protected boolean isBackupDisabled() {
        return SystemProperties.getBoolean(BACKUP_DISABLE_PROPERTY, false);
    }

    protected int binderGetCallingUserId() {
        return Binder.getCallingUserHandle().getIdentifier();
    }

    protected int binderGetCallingUid() {
        return Binder.getCallingUid();
    }

    protected File getSuppressFileForSystemUser() {
        return new File(UserBackupManagerFiles.getBaseStateDir(0), BACKUP_SUPPRESS_FILENAME);
    }

    protected File getRememberActivatedFileForNonSystemUser(int userId) {
        return UserBackupManagerFiles.getStateFileInSystemDir(REMEMBER_ACTIVATED_FILENAME, userId);
    }

    protected File getActivatedFileForNonSystemUser(int userId) {
        return UserBackupManagerFiles.getStateFileInSystemDir(BACKUP_ACTIVATED_FILENAME, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRemovedNonSystemUser(int userId) {
        Slog.i(TAG, "Removing state for non system user " + userId);
        File dir = UserBackupManagerFiles.getStateDirInSystemDir(userId);
        if (!FileUtils.deleteContentsAndDir(dir)) {
            Slog.w(TAG, "Failed to delete state dir for removed user: " + userId);
        }
    }

    private void createFile(File file) throws IOException {
        if (file.exists()) {
            return;
        }
        file.getParentFile().mkdirs();
        if (!file.createNewFile()) {
            Slog.w(TAG, "Failed to create file " + file.getPath());
        }
    }

    private void deleteFile(File file) {
        if (file.exists() && !file.delete()) {
            Slog.w(TAG, "Failed to delete file " + file.getPath());
        }
    }

    private void deactivateBackupForUserLocked(int userId) throws IOException {
        if (userId == 0) {
            createFile(getSuppressFileForSystemUser());
        } else {
            deleteFile(getActivatedFileForNonSystemUser(userId));
        }
    }

    private void activateBackupForUserLocked(int userId) throws IOException {
        if (userId == 0) {
            deleteFile(getSuppressFileForSystemUser());
        } else {
            createFile(getActivatedFileForNonSystemUser(userId));
        }
    }

    public boolean isUserReadyForBackup(int userId) {
        return (this.mUserServices.get(0) == null || this.mUserServices.get(userId) == null) ? false : true;
    }

    private boolean isBackupActivatedForUser(int userId) {
        if (getSuppressFileForSystemUser().exists()) {
            return false;
        }
        return userId == 0 || getActivatedFileForNonSystemUser(userId).exists();
    }

    protected Context getContext() {
        return this.mContext;
    }

    protected UserManager getUserManager() {
        return this.mUserManager;
    }

    protected void postToHandler(Runnable runnable) {
        this.mHandler.post(runnable);
    }

    void onUnlockUser(final int userId) {
        postToHandler(new Runnable() { // from class: com.android.server.backup.BackupManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                BackupManagerService.this.m2152x534420a2(userId);
            }
        });
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: startServiceForUser */
    public void m2152x534420a2(int userId) {
        if (this.mGlobalDisable) {
            Slog.i(TAG, "Backup service not supported");
        } else if (!isBackupActivatedForUser(userId)) {
            Slog.i(TAG, "Backup not activated for user " + userId);
        } else if (this.mUserServices.get(userId) != null) {
            Slog.i(TAG, "userId " + userId + " already started, so not starting again");
        } else {
            Slog.i(TAG, "Starting service for user: " + userId);
            UserBackupManagerService userBackupManagerService = UserBackupManagerService.createAndInitializeService(userId, this.mContext, this, this.mTransportWhitelist);
            startServiceForUser(userId, userBackupManagerService);
        }
    }

    void startServiceForUser(int userId, UserBackupManagerService userBackupManagerService) {
        this.mUserServices.put(userId, userBackupManagerService);
        Trace.traceBegin(64L, "backup enable");
        userBackupManagerService.initializeBackupEnableState();
        Trace.traceEnd(64L);
    }

    protected void stopServiceForUser(int userId) {
        UserBackupManagerService userBackupManagerService = (UserBackupManagerService) this.mUserServices.removeReturnOld(userId);
        if (userBackupManagerService != null) {
            userBackupManagerService.tearDownService();
            KeyValueBackupJob.cancel(userId, this.mContext);
            FullBackupJob.cancel(userId, this.mContext);
        }
    }

    SparseArray<UserBackupManagerService> getUserServices() {
        return this.mUserServices;
    }

    void onStopUser(final int userId) {
        postToHandler(new Runnable() { // from class: com.android.server.backup.BackupManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                BackupManagerService.this.m2151x97056583(userId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onStopUser$1$com-android-server-backup-BackupManagerService  reason: not valid java name */
    public /* synthetic */ void m2151x97056583(int userId) {
        if (!this.mGlobalDisable) {
            Slog.i(TAG, "Stopping service for user: " + userId);
            stopServiceForUser(userId);
        }
    }

    public UserBackupManagerService getUserService(int userId) {
        return this.mUserServices.get(userId);
    }

    private void enforcePermissionsOnUser(int userId) throws SecurityException {
        boolean isRestrictedUser = userId == 0 || getUserManager().getUserInfo(userId).isManagedProfile();
        if (!isRestrictedUser) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "No permission to configure backup activity");
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "No permission to configure backup activity");
            return;
        }
        int caller = binderGetCallingUid();
        if (caller != 1000 && caller != 0) {
            throw new SecurityException("No permission to configure backup activity");
        }
    }

    public void setBackupServiceActive(int userId, boolean makeActive) {
        enforcePermissionsOnUser(userId);
        if (userId != 0) {
            try {
                File rememberFile = getRememberActivatedFileForNonSystemUser(userId);
                createFile(rememberFile);
                RandomAccessFileUtils.writeBoolean(rememberFile, makeActive);
            } catch (IOException e) {
                Slog.e(TAG, "Unable to persist backup service activity", e);
            }
        }
        if (this.mGlobalDisable) {
            Slog.i(TAG, "Backup service not supported");
            return;
        }
        synchronized (this.mStateLock) {
            Slog.i(TAG, "Making backup " + (makeActive ? "" : "in") + DomainVerificationPersistence.TAG_ACTIVE);
            if (makeActive) {
                try {
                    activateBackupForUserLocked(userId);
                } catch (IOException e2) {
                    Slog.e(TAG, "Unable to persist backup service activity");
                }
                if (getUserManager().isUserUnlocked(userId)) {
                    long oldId = Binder.clearCallingIdentity();
                    m2152x534420a2(userId);
                    Binder.restoreCallingIdentity(oldId);
                }
            } else {
                try {
                    deactivateBackupForUserLocked(userId);
                } catch (IOException e3) {
                    Slog.e(TAG, "Unable to persist backup service inactivity");
                }
                onStopUser(userId);
            }
        }
    }

    public boolean isBackupServiceActive(int userId) {
        boolean z;
        int callingUid = Binder.getCallingUid();
        if (CompatChanges.isChangeEnabled(158482162L, callingUid)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.BACKUP", "isBackupServiceActive");
        }
        synchronized (this.mStateLock) {
            z = !this.mGlobalDisable && isBackupActivatedForUser(userId);
        }
        return z;
    }

    public void dataChangedForUser(int userId, String packageName) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            dataChanged(userId, packageName);
        }
    }

    public void dataChanged(String packageName) throws RemoteException {
        dataChangedForUser(binderGetCallingUserId(), packageName);
    }

    public void dataChanged(int userId, String packageName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "dataChanged()");
        if (userBackupManagerService != null) {
            userBackupManagerService.dataChanged(packageName);
        }
    }

    public void initializeTransportsForUser(int userId, String[] transportNames, IBackupObserver observer) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            initializeTransports(userId, transportNames, observer);
        }
    }

    public void initializeTransports(int userId, String[] transportNames, IBackupObserver observer) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "initializeTransports()");
        if (userBackupManagerService != null) {
            userBackupManagerService.initializeTransports(transportNames, observer);
        }
    }

    public void clearBackupDataForUser(int userId, String transportName, String packageName) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            clearBackupData(userId, transportName, packageName);
        }
    }

    public void clearBackupData(int userId, String transportName, String packageName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "clearBackupData()");
        if (userBackupManagerService != null) {
            userBackupManagerService.clearBackupData(transportName, packageName);
        }
    }

    public void clearBackupData(String transportName, String packageName) throws RemoteException {
        clearBackupDataForUser(binderGetCallingUserId(), transportName, packageName);
    }

    public void agentConnectedForUser(int userId, String packageName, IBinder agent) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            agentConnected(userId, packageName, agent);
        }
    }

    public void agentConnected(String packageName, IBinder agent) throws RemoteException {
        agentConnectedForUser(binderGetCallingUserId(), packageName, agent);
    }

    public void agentConnected(int userId, String packageName, IBinder agentBinder) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "agentConnected()");
        if (userBackupManagerService != null) {
            userBackupManagerService.agentConnected(packageName, agentBinder);
        }
    }

    public void agentDisconnectedForUser(int userId, String packageName) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            agentDisconnected(userId, packageName);
        }
    }

    public void agentDisconnected(String packageName) throws RemoteException {
        agentDisconnectedForUser(binderGetCallingUserId(), packageName);
    }

    public void agentDisconnected(int userId, String packageName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "agentDisconnected()");
        if (userBackupManagerService != null) {
            userBackupManagerService.agentDisconnected(packageName);
        }
    }

    public void restoreAtInstallForUser(int userId, String packageName, int token) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            restoreAtInstall(userId, packageName, token);
        }
    }

    public void restoreAtInstall(String packageName, int token) throws RemoteException {
        restoreAtInstallForUser(binderGetCallingUserId(), packageName, token);
    }

    public void restoreAtInstall(int userId, String packageName, int token) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "restoreAtInstall()");
        if (userBackupManagerService != null) {
            userBackupManagerService.restoreAtInstall(packageName, token);
        }
    }

    public void setBackupEnabledForUser(int userId, boolean isEnabled) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            setBackupEnabled(userId, isEnabled);
        }
    }

    public void setBackupEnabled(boolean isEnabled) throws RemoteException {
        setBackupEnabledForUser(binderGetCallingUserId(), isEnabled);
    }

    public void setBackupEnabled(int userId, boolean enable) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "setBackupEnabled()");
        if (userBackupManagerService != null) {
            userBackupManagerService.setBackupEnabled(enable);
        }
    }

    public void setAutoRestoreForUser(int userId, boolean doAutoRestore) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            setAutoRestore(userId, doAutoRestore);
        }
    }

    public void setAutoRestore(boolean doAutoRestore) throws RemoteException {
        setAutoRestoreForUser(binderGetCallingUserId(), doAutoRestore);
    }

    public void setAutoRestore(int userId, boolean autoRestore) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "setAutoRestore()");
        if (userBackupManagerService != null) {
            userBackupManagerService.setAutoRestore(autoRestore);
        }
    }

    public boolean isBackupEnabledForUser(int userId) throws RemoteException {
        return isUserReadyForBackup(userId) && isBackupEnabled(userId);
    }

    public boolean isBackupEnabled() throws RemoteException {
        return isBackupEnabledForUser(binderGetCallingUserId());
    }

    public boolean isBackupEnabled(int userId) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "isBackupEnabled()");
        return userBackupManagerService != null && userBackupManagerService.isBackupEnabled();
    }

    public boolean setBackupPassword(String currentPassword, String newPassword) {
        UserBackupManagerService userBackupManagerService;
        int userId = binderGetCallingUserId();
        return isUserReadyForBackup(userId) && (userBackupManagerService = getServiceForUserIfCallerHasPermission(0, "setBackupPassword()")) != null && userBackupManagerService.setBackupPassword(currentPassword, newPassword);
    }

    public boolean hasBackupPassword() throws RemoteException {
        UserBackupManagerService userBackupManagerService;
        int userId = binderGetCallingUserId();
        return isUserReadyForBackup(userId) && (userBackupManagerService = getServiceForUserIfCallerHasPermission(0, "hasBackupPassword()")) != null && userBackupManagerService.hasBackupPassword();
    }

    public void backupNowForUser(int userId) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            backupNow(userId);
        }
    }

    public void backupNow() throws RemoteException {
        backupNowForUser(binderGetCallingUserId());
    }

    public void backupNow(int userId) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "backupNow()");
        if (userBackupManagerService != null) {
            userBackupManagerService.backupNow();
        }
    }

    public void adbBackup(int userId, ParcelFileDescriptor fd, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, boolean doAllApps, boolean includeSystem, boolean doCompress, boolean doKeyValue, String[] packageNames) {
        UserBackupManagerService userBackupManagerService;
        if (isUserReadyForBackup(userId) && (userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "adbBackup()")) != null) {
            userBackupManagerService.adbBackup(fd, includeApks, includeObbs, includeShared, doWidgets, doAllApps, includeSystem, doCompress, doKeyValue, packageNames);
        }
    }

    public void fullTransportBackupForUser(int userId, String[] packageNames) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            fullTransportBackup(userId, packageNames);
        }
    }

    public void fullTransportBackup(int userId, String[] packageNames) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "fullTransportBackup()");
        if (userBackupManagerService != null) {
            userBackupManagerService.fullTransportBackup(packageNames);
        }
    }

    public void adbRestore(int userId, ParcelFileDescriptor fd) {
        UserBackupManagerService userBackupManagerService;
        if (isUserReadyForBackup(userId) && (userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "adbRestore()")) != null) {
            userBackupManagerService.adbRestore(fd);
        }
    }

    public void acknowledgeFullBackupOrRestoreForUser(int userId, int token, boolean allow, String curPassword, String encryptionPassword, IFullBackupRestoreObserver observer) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            acknowledgeAdbBackupOrRestore(userId, token, allow, curPassword, encryptionPassword, observer);
        }
    }

    public void acknowledgeAdbBackupOrRestore(int userId, int token, boolean allow, String currentPassword, String encryptionPassword, IFullBackupRestoreObserver observer) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "acknowledgeAdbBackupOrRestore()");
        if (userBackupManagerService != null) {
            userBackupManagerService.acknowledgeAdbBackupOrRestore(token, allow, currentPassword, encryptionPassword, observer);
        }
    }

    public void acknowledgeFullBackupOrRestore(int token, boolean allow, String curPassword, String encryptionPassword, IFullBackupRestoreObserver observer) throws RemoteException {
        acknowledgeFullBackupOrRestoreForUser(binderGetCallingUserId(), token, allow, curPassword, encryptionPassword, observer);
    }

    public String getCurrentTransportForUser(int userId) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            return getCurrentTransport(userId);
        }
        return null;
    }

    public String getCurrentTransport() throws RemoteException {
        return getCurrentTransportForUser(binderGetCallingUserId());
    }

    public String getCurrentTransport(int userId) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "getCurrentTransport()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.getCurrentTransport();
    }

    public ComponentName getCurrentTransportComponentForUser(int userId) {
        if (isUserReadyForBackup(userId)) {
            return getCurrentTransportComponent(userId);
        }
        return null;
    }

    public ComponentName getCurrentTransportComponent(int userId) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "getCurrentTransportComponent()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.getCurrentTransportComponent();
    }

    public String[] listAllTransportsForUser(int userId) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            return listAllTransports(userId);
        }
        return null;
    }

    public String[] listAllTransports(int userId) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "listAllTransports()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.listAllTransports();
    }

    public String[] listAllTransports() throws RemoteException {
        return listAllTransportsForUser(binderGetCallingUserId());
    }

    public ComponentName[] listAllTransportComponentsForUser(int userId) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            return listAllTransportComponents(userId);
        }
        return null;
    }

    public ComponentName[] listAllTransportComponents(int userId) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "listAllTransportComponents()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.listAllTransportComponents();
    }

    public String[] getTransportWhitelist() {
        int userId = binderGetCallingUserId();
        if (!isUserReadyForBackup(userId)) {
            return null;
        }
        String[] whitelistedTransports = new String[this.mTransportWhitelist.size()];
        int i = 0;
        for (ComponentName component : this.mTransportWhitelist) {
            whitelistedTransports[i] = component.flattenToShortString();
            i++;
        }
        return whitelistedTransports;
    }

    public void updateTransportAttributesForUser(int userId, ComponentName transportComponent, String name, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, CharSequence dataManagementLabel) {
        if (isUserReadyForBackup(userId)) {
            updateTransportAttributes(userId, transportComponent, name, configurationIntent, currentDestinationString, dataManagementIntent, dataManagementLabel);
        }
    }

    public void updateTransportAttributes(int userId, ComponentName transportComponent, String name, Intent configurationIntent, String currentDestinationString, Intent dataManagementIntent, CharSequence dataManagementLabel) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "updateTransportAttributes()");
        if (userBackupManagerService != null) {
            userBackupManagerService.updateTransportAttributes(transportComponent, name, configurationIntent, currentDestinationString, dataManagementIntent, dataManagementLabel);
        }
    }

    public String selectBackupTransportForUser(int userId, String transport) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            return selectBackupTransport(userId, transport);
        }
        return null;
    }

    public String selectBackupTransport(String transport) throws RemoteException {
        return selectBackupTransportForUser(binderGetCallingUserId(), transport);
    }

    @Deprecated
    public String selectBackupTransport(int userId, String transportName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "selectBackupTransport()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.selectBackupTransport(transportName);
    }

    public void selectBackupTransportAsyncForUser(int userId, ComponentName transport, ISelectBackupTransportCallback listener) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            selectBackupTransportAsync(userId, transport, listener);
        } else if (listener != null) {
            try {
                listener.onFailure(-2001);
            } catch (RemoteException e) {
            }
        }
    }

    public void selectBackupTransportAsync(int userId, ComponentName transportComponent, ISelectBackupTransportCallback listener) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "selectBackupTransportAsync()");
        if (userBackupManagerService != null) {
            userBackupManagerService.selectBackupTransportAsync(transportComponent, listener);
        }
    }

    public Intent getConfigurationIntentForUser(int userId, String transport) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            return getConfigurationIntent(userId, transport);
        }
        return null;
    }

    public Intent getConfigurationIntent(String transport) throws RemoteException {
        return getConfigurationIntentForUser(binderGetCallingUserId(), transport);
    }

    public Intent getConfigurationIntent(int userId, String transportName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "getConfigurationIntent()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.getConfigurationIntent(transportName);
    }

    public String getDestinationStringForUser(int userId, String transport) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            return getDestinationString(userId, transport);
        }
        return null;
    }

    public String getDestinationString(String transport) throws RemoteException {
        return getDestinationStringForUser(binderGetCallingUserId(), transport);
    }

    public String getDestinationString(int userId, String transportName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "getDestinationString()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.getDestinationString(transportName);
    }

    public Intent getDataManagementIntentForUser(int userId, String transport) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            return getDataManagementIntent(userId, transport);
        }
        return null;
    }

    public Intent getDataManagementIntent(String transport) throws RemoteException {
        return getDataManagementIntentForUser(binderGetCallingUserId(), transport);
    }

    public Intent getDataManagementIntent(int userId, String transportName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "getDataManagementIntent()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.getDataManagementIntent(transportName);
    }

    public CharSequence getDataManagementLabelForUser(int userId, String transport) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            return getDataManagementLabel(userId, transport);
        }
        return null;
    }

    public CharSequence getDataManagementLabel(int userId, String transportName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "getDataManagementLabel()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.getDataManagementLabel(transportName);
    }

    public IRestoreSession beginRestoreSessionForUser(int userId, String packageName, String transportID) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            return beginRestoreSession(userId, packageName, transportID);
        }
        return null;
    }

    public IRestoreSession beginRestoreSession(int userId, String packageName, String transportName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "beginRestoreSession()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.beginRestoreSession(packageName, transportName);
    }

    public void opCompleteForUser(int userId, int token, long result) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            opComplete(userId, token, result);
        }
    }

    public void opComplete(int token, long result) throws RemoteException {
        opCompleteForUser(binderGetCallingUserId(), token, result);
    }

    public void opComplete(int userId, int token, long result) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "opComplete()");
        if (userBackupManagerService != null) {
            userBackupManagerService.opComplete(token, result);
        }
    }

    public long getAvailableRestoreTokenForUser(int userId, String packageName) {
        if (isUserReadyForBackup(userId)) {
            return getAvailableRestoreToken(userId, packageName);
        }
        return 0L;
    }

    public long getAvailableRestoreToken(int userId, String packageName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "getAvailableRestoreToken()");
        if (userBackupManagerService == null) {
            return 0L;
        }
        return userBackupManagerService.getAvailableRestoreToken(packageName);
    }

    public boolean isAppEligibleForBackupForUser(int userId, String packageName) {
        return isUserReadyForBackup(userId) && isAppEligibleForBackup(userId, packageName);
    }

    public boolean isAppEligibleForBackup(int userId, String packageName) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "isAppEligibleForBackup()");
        return userBackupManagerService != null && userBackupManagerService.isAppEligibleForBackup(packageName);
    }

    public String[] filterAppsEligibleForBackupForUser(int userId, String[] packages) {
        if (isUserReadyForBackup(userId)) {
            return filterAppsEligibleForBackup(userId, packages);
        }
        return null;
    }

    public String[] filterAppsEligibleForBackup(int userId, String[] packages) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "filterAppsEligibleForBackup()");
        if (userBackupManagerService == null) {
            return null;
        }
        return userBackupManagerService.filterAppsEligibleForBackup(packages);
    }

    public int requestBackupForUser(int userId, String[] packages, IBackupObserver observer, IBackupManagerMonitor monitor, int flags) throws RemoteException {
        if (!isUserReadyForBackup(userId)) {
            return -2001;
        }
        return requestBackup(userId, packages, observer, monitor, flags);
    }

    public int requestBackup(String[] packages, IBackupObserver observer, IBackupManagerMonitor monitor, int flags) throws RemoteException {
        return requestBackup(binderGetCallingUserId(), packages, observer, monitor, flags);
    }

    public int requestBackup(int userId, String[] packages, IBackupObserver observer, IBackupManagerMonitor monitor, int flags) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "requestBackup()");
        if (userBackupManagerService == null) {
            return -2001;
        }
        return userBackupManagerService.requestBackup(packages, observer, monitor, flags);
    }

    public void cancelBackupsForUser(int userId) throws RemoteException {
        if (isUserReadyForBackup(userId)) {
            cancelBackups(userId);
        }
    }

    public void cancelBackups() throws RemoteException {
        cancelBackupsForUser(binderGetCallingUserId());
    }

    public void cancelBackups(int userId) {
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "cancelBackups()");
        if (userBackupManagerService != null) {
            userBackupManagerService.cancelBackups();
        }
    }

    public UserHandle getUserForAncestralSerialNumber(long ancestralSerialNumber) {
        if (this.mGlobalDisable) {
            return null;
        }
        int callingUserId = Binder.getCallingUserHandle().getIdentifier();
        long oldId = Binder.clearCallingIdentity();
        try {
            int[] userIds = getUserManager().getProfileIds(callingUserId, false);
            Binder.restoreCallingIdentity(oldId);
            for (int userId : userIds) {
                UserBackupManagerService userBackupManagerService = this.mUserServices.get(userId);
                if (userBackupManagerService != null && userBackupManagerService.getAncestralSerialNumber() == ancestralSerialNumber) {
                    return UserHandle.of(userId);
                }
            }
            return null;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(oldId);
            throw th;
        }
    }

    public void setAncestralSerialNumber(long ancestralSerialNumber) {
        UserBackupManagerService userBackupManagerService;
        if (!this.mGlobalDisable && (userBackupManagerService = getServiceForUserIfCallerHasPermission(Binder.getCallingUserHandle().getIdentifier(), "setAncestralSerialNumber()")) != null) {
            userBackupManagerService.setAncestralSerialNumber(ancestralSerialNumber);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            return;
        }
        dumpWithoutCheckingPermission(fd, pw, args);
    }

    void dumpWithoutCheckingPermission(FileDescriptor fd, PrintWriter pw, String[] args) {
        int userId = binderGetCallingUserId();
        if (!isUserReadyForBackup(userId)) {
            pw.println("Inactive");
            return;
        }
        if (args != null) {
            for (String arg : args) {
                if ("-h".equals(arg)) {
                    pw.println("'dumpsys backup' optional arguments:");
                    pw.println("  -h       : this help text");
                    pw.println("  a[gents] : dump information about defined backup agents");
                    pw.println("  transportclients : dump information about transport clients");
                    pw.println("  transportstats : dump transport statts");
                    pw.println("  users    : dump the list of users for which backup service is running");
                    return;
                } else if (DatabaseHelper.SoundModelContract.KEY_USERS.equals(arg.toLowerCase())) {
                    pw.print(DUMP_RUNNING_USERS_MESSAGE);
                    for (int i = 0; i < this.mUserServices.size(); i++) {
                        pw.print(" " + this.mUserServices.keyAt(i));
                    }
                    pw.println();
                    return;
                }
            }
        }
        for (int i2 = 0; i2 < this.mUserServices.size(); i2++) {
            UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(this.mUserServices.keyAt(i2), "dump()");
            if (userBackupManagerService != null) {
                userBackupManagerService.dump(fd, pw, args);
            }
        }
    }

    public boolean beginFullBackup(int userId, FullBackupJob scheduledJob) {
        UserBackupManagerService userBackupManagerService;
        return isUserReadyForBackup(userId) && (userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "beginFullBackup()")) != null && userBackupManagerService.beginFullBackup(scheduledJob);
    }

    public void endFullBackup(int userId) {
        UserBackupManagerService userBackupManagerService;
        if (isUserReadyForBackup(userId) && (userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "endFullBackup()")) != null) {
            userBackupManagerService.endFullBackup();
        }
    }

    public void excludeKeysFromRestore(String packageName, List<String> keys) {
        int userId = Binder.getCallingUserHandle().getIdentifier();
        if (!isUserReadyForBackup(userId)) {
            Slog.w(TAG, "Returning from excludeKeysFromRestore as backup for user" + userId + " is not initialized yet");
            return;
        }
        UserBackupManagerService userBackupManagerService = getServiceForUserIfCallerHasPermission(userId, "excludeKeysFromRestore()");
        if (userBackupManagerService != null) {
            userBackupManagerService.excludeKeysFromRestore(packageName, keys);
        }
    }

    UserBackupManagerService getServiceForUserIfCallerHasPermission(int userId, String caller) {
        enforceCallingPermissionOnUserId(userId, caller);
        UserBackupManagerService userBackupManagerService = this.mUserServices.get(userId);
        if (userBackupManagerService == null) {
            Slog.w(TAG, "Called " + caller + " for unknown user: " + userId);
        }
        return userBackupManagerService;
    }

    void enforceCallingPermissionOnUserId(int userId, String message) {
        if (Binder.getCallingUserHandle().getIdentifier() != userId) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", message);
        }
    }

    /* loaded from: classes.dex */
    public static class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            this(context, new BackupManagerService(context));
        }

        Lifecycle(Context context, BackupManagerService backupManagerService) {
            super(context);
            BackupManagerService.sInstance = backupManagerService;
        }

        @Override // com.android.server.SystemService
        public void onStart() {
            publishService("backup", BackupManagerService.sInstance);
        }

        @Override // com.android.server.SystemService
        public void onUserUnlocking(SystemService.TargetUser user) {
            BackupManagerService.sInstance.onUnlockUser(user.getUserIdentifier());
        }

        @Override // com.android.server.SystemService
        public void onUserStopping(SystemService.TargetUser user) {
            BackupManagerService.sInstance.onStopUser(user.getUserIdentifier());
        }

        void publishService(String name, IBinder service) {
            publishBinderService(name, service);
        }
    }
}
