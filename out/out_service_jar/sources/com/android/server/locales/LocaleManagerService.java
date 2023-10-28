package com.android.server.locales;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ILocaleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.LocaleList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.android.server.wm.ActivityTaskManagerInternal;
import java.io.FileDescriptor;
import java.util.Objects;
/* loaded from: classes.dex */
public class LocaleManagerService extends SystemService {
    public static final boolean DEBUG = false;
    private static final String TAG = "LocaleManagerService";
    private ActivityManagerInternal mActivityManagerInternal;
    private ActivityTaskManagerInternal mActivityTaskManagerInternal;
    private LocaleManagerBackupHelper mBackupHelper;
    private final LocaleManagerBinderService mBinderService;
    final Context mContext;
    private PackageManager mPackageManager;
    private final PackageMonitor mPackageMonitor;

    public LocaleManagerService(Context context) {
        super(context);
        this.mContext = context;
        this.mBinderService = new LocaleManagerBinderService();
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mPackageManager = context.getPackageManager();
        HandlerThread broadcastHandlerThread = new HandlerThread(TAG, 10);
        broadcastHandlerThread.start();
        final SystemAppUpdateTracker systemAppUpdateTracker = new SystemAppUpdateTracker(this);
        broadcastHandlerThread.getThreadHandler().postAtFrontOfQueue(new Runnable() { // from class: com.android.server.locales.LocaleManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                systemAppUpdateTracker.init();
            }
        });
        this.mBackupHelper = new LocaleManagerBackupHelper(this, this.mPackageManager, broadcastHandlerThread);
        LocaleManagerServicePackageMonitor localeManagerServicePackageMonitor = new LocaleManagerServicePackageMonitor(this.mBackupHelper, systemAppUpdateTracker);
        this.mPackageMonitor = localeManagerServicePackageMonitor;
        localeManagerServicePackageMonitor.register(context, broadcastHandlerThread.getLooper(), UserHandle.ALL, true);
    }

    LocaleManagerService(Context context, ActivityTaskManagerInternal activityTaskManagerInternal, ActivityManagerInternal activityManagerInternal, PackageManager packageManager, LocaleManagerBackupHelper localeManagerBackupHelper, PackageMonitor packageMonitor) {
        super(context);
        this.mContext = context;
        this.mBinderService = new LocaleManagerBinderService();
        this.mActivityTaskManagerInternal = activityTaskManagerInternal;
        this.mActivityManagerInternal = activityManagerInternal;
        this.mPackageManager = packageManager;
        this.mBackupHelper = localeManagerBackupHelper;
        this.mPackageMonitor = packageMonitor;
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService(DatabaseHelper.SoundModelContract.KEY_LOCALE, this.mBinderService);
        LocalServices.addService(LocaleManagerInternal.class, new LocaleManagerInternalImpl());
    }

    /* loaded from: classes.dex */
    private final class LocaleManagerInternalImpl extends LocaleManagerInternal {
        private LocaleManagerInternalImpl() {
        }

        @Override // com.android.server.locales.LocaleManagerInternal
        public byte[] getBackupPayload(int userId) {
            checkCallerIsSystem();
            return LocaleManagerService.this.mBackupHelper.getBackupPayload(userId);
        }

        @Override // com.android.server.locales.LocaleManagerInternal
        public void stageAndApplyRestoredPayload(byte[] payload, int userId) {
            LocaleManagerService.this.mBackupHelper.stageAndApplyRestoredPayload(payload, userId);
        }

        private void checkCallerIsSystem() {
            if (Binder.getCallingUid() != 1000) {
                throw new SecurityException("Caller is not system.");
            }
        }
    }

    /* loaded from: classes.dex */
    private final class LocaleManagerBinderService extends ILocaleManager.Stub {
        private LocaleManagerBinderService() {
        }

        public void setApplicationLocales(String appPackageName, int userId, LocaleList locales) throws RemoteException {
            LocaleManagerService.this.setApplicationLocales(appPackageName, userId, locales);
        }

        public LocaleList getApplicationLocales(String appPackageName, int userId) throws RemoteException {
            return LocaleManagerService.this.getApplicationLocales(appPackageName, userId);
        }

        public LocaleList getSystemLocales() throws RemoteException {
            return LocaleManagerService.this.getSystemLocales();
        }

        /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.locales.LocaleManagerService$LocaleManagerBinderService */
        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new LocaleManagerShellCommand(LocaleManagerService.this.mBinderService).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    public void setApplicationLocales(String appPackageName, int userId, LocaleList locales) throws RemoteException, IllegalArgumentException {
        AppLocaleChangedAtomRecord atomRecordForMetrics = new AppLocaleChangedAtomRecord(Binder.getCallingUid());
        try {
            Objects.requireNonNull(appPackageName);
            Objects.requireNonNull(locales);
            atomRecordForMetrics.setNewLocales(locales.toLanguageTags());
            int userId2 = this.mActivityManagerInternal.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 0, "setApplicationLocales", (String) null);
            boolean isCallerOwner = isPackageOwnedByCaller(appPackageName, userId2, atomRecordForMetrics);
            if (!isCallerOwner) {
                enforceChangeConfigurationPermission(atomRecordForMetrics);
            }
            long token = Binder.clearCallingIdentity();
            setApplicationLocalesUnchecked(appPackageName, userId2, locales, atomRecordForMetrics);
            Binder.restoreCallingIdentity(token);
        } finally {
            logMetric(atomRecordForMetrics);
        }
    }

    private void setApplicationLocalesUnchecked(String appPackageName, int userId, LocaleList locales, AppLocaleChangedAtomRecord atomRecordForMetrics) {
        atomRecordForMetrics.setPrevLocales(getApplicationLocalesUnchecked(appPackageName, userId).toLanguageTags());
        ActivityTaskManagerInternal.PackageConfigurationUpdater updater = this.mActivityTaskManagerInternal.createPackageConfigurationUpdater(appPackageName, userId);
        boolean isConfigChanged = updater.setLocales(locales).commit();
        if (isConfigChanged) {
            notifyAppWhoseLocaleChanged(appPackageName, userId, locales);
            notifyInstallerOfAppWhoseLocaleChanged(appPackageName, userId, locales);
            notifyRegisteredReceivers(appPackageName, userId, locales);
            this.mBackupHelper.notifyBackupManager();
            atomRecordForMetrics.setStatus(1);
            return;
        }
        atomRecordForMetrics.setStatus(2);
    }

    private void notifyRegisteredReceivers(String appPackageName, int userId, LocaleList locales) {
        Intent intent = createBaseIntent("android.intent.action.APPLICATION_LOCALE_CHANGED", appPackageName, locales);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId), "android.permission.READ_APP_SPECIFIC_LOCALES");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyInstallerOfAppWhoseLocaleChanged(String appPackageName, int userId, LocaleList locales) {
        String installingPackageName = getInstallingPackageName(appPackageName);
        if (installingPackageName != null) {
            Intent intent = createBaseIntent("android.intent.action.APPLICATION_LOCALE_CHANGED", appPackageName, locales);
            intent.setPackage(installingPackageName);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
        }
    }

    private void notifyAppWhoseLocaleChanged(String appPackageName, int userId, LocaleList locales) {
        Intent intent = createBaseIntent("android.intent.action.LOCALE_CHANGED", appPackageName, locales);
        intent.setPackage(appPackageName);
        intent.addFlags(2097152);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.of(userId));
    }

    static Intent createBaseIntent(String intentAction, String appPackageName, LocaleList locales) {
        return new Intent(intentAction).putExtra("android.intent.extra.PACKAGE_NAME", appPackageName).putExtra("android.intent.extra.LOCALE_LIST", locales).addFlags(AudioFormat.EVRCB);
    }

    private boolean isPackageOwnedByCaller(String appPackageName, int userId) {
        return isPackageOwnedByCaller(appPackageName, userId, null);
    }

    private boolean isPackageOwnedByCaller(String appPackageName, int userId, AppLocaleChangedAtomRecord atomRecordForMetrics) {
        int uid = getPackageUid(appPackageName, userId);
        if (uid < 0) {
            Slog.w(TAG, "Unknown package " + appPackageName + " for user " + userId);
            if (atomRecordForMetrics != null) {
                atomRecordForMetrics.setStatus(3);
            }
            throw new IllegalArgumentException("Unknown package: " + appPackageName + " for user " + userId);
        }
        if (atomRecordForMetrics != null) {
            atomRecordForMetrics.setTargetUid(uid);
        }
        return UserHandle.isSameApp(Binder.getCallingUid(), uid);
    }

    private void enforceChangeConfigurationPermission(AppLocaleChangedAtomRecord atomRecordForMetrics) {
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_CONFIGURATION", "setApplicationLocales");
        } catch (SecurityException e) {
            atomRecordForMetrics.setStatus(4);
            throw e;
        }
    }

    public LocaleList getApplicationLocales(String appPackageName, int userId) throws RemoteException, IllegalArgumentException {
        Objects.requireNonNull(appPackageName);
        int userId2 = this.mActivityManagerInternal.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), userId, false, 0, "getApplicationLocales", (String) null);
        if (!isPackageOwnedByCaller(appPackageName, userId2) && !isCallerInstaller(appPackageName, userId2)) {
            enforceReadAppSpecificLocalesPermission();
        }
        long token = Binder.clearCallingIdentity();
        try {
            return getApplicationLocalesUnchecked(appPackageName, userId2);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private LocaleList getApplicationLocalesUnchecked(String appPackageName, int userId) {
        ActivityTaskManagerInternal.PackageConfig appConfig = this.mActivityTaskManagerInternal.getApplicationConfig(appPackageName, userId);
        if (appConfig == null) {
            return LocaleList.getEmptyLocaleList();
        }
        LocaleList locales = appConfig.mLocales;
        return locales != null ? locales : LocaleList.getEmptyLocaleList();
    }

    private boolean isCallerInstaller(String appPackageName, int userId) {
        int installerUid;
        String installingPackageName = getInstallingPackageName(appPackageName);
        return installingPackageName != null && (installerUid = getPackageUid(installingPackageName, userId)) >= 0 && UserHandle.isSameApp(Binder.getCallingUid(), installerUid);
    }

    private void enforceReadAppSpecificLocalesPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_APP_SPECIFIC_LOCALES", "getApplicationLocales");
    }

    private int getPackageUid(String appPackageName, int userId) {
        try {
            return this.mPackageManager.getPackageUidAsUser(appPackageName, PackageManager.PackageInfoFlags.of(0L), userId);
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getInstallingPackageName(String packageName) {
        try {
            return this.mContext.getPackageManager().getInstallSourceInfo(packageName).getInstallingPackageName();
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Package not found " + packageName);
            return null;
        }
    }

    public LocaleList getSystemLocales() throws RemoteException {
        long token = Binder.clearCallingIdentity();
        try {
            return getSystemLocalesUnchecked();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private LocaleList getSystemLocalesUnchecked() throws RemoteException {
        LocaleList systemLocales = null;
        Configuration conf = ActivityManager.getService().getConfiguration();
        if (conf != null) {
            systemLocales = conf.getLocales();
        }
        if (systemLocales == null) {
            LocaleList systemLocales2 = LocaleList.getEmptyLocaleList();
            return systemLocales2;
        }
        return systemLocales;
    }

    private void logMetric(AppLocaleChangedAtomRecord atomRecordForMetrics) {
        FrameworkStatsLog.write((int) FrameworkStatsLog.APPLICATION_LOCALES_CHANGED, atomRecordForMetrics.mCallingUid, atomRecordForMetrics.mTargetUid, atomRecordForMetrics.mNewLocales, atomRecordForMetrics.mPrevLocales, atomRecordForMetrics.mStatus);
    }
}
