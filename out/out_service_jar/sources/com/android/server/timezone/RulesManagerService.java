package com.android.server.timezone;

import android.app.timezone.DistroFormatVersion;
import android.app.timezone.DistroRulesVersion;
import android.app.timezone.ICallback;
import android.app.timezone.IRulesManager;
import android.app.timezone.RulesState;
import android.content.Context;
import android.icu.util.TimeZone;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.i18n.timezone.TimeZoneDataFiles;
import com.android.i18n.timezone.TimeZoneFinder;
import com.android.i18n.timezone.TzDataSetVersion;
import com.android.i18n.timezone.ZoneInfoDb;
import com.android.server.EventLogTags;
import com.android.server.SystemService;
import com.android.timezone.distro.DistroException;
import com.android.timezone.distro.DistroVersion;
import com.android.timezone.distro.StagedDistroOperation;
import com.android.timezone.distro.TimeZoneDistro;
import com.android.timezone.distro.installer.TimeZoneDistroInstaller;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
/* loaded from: classes2.dex */
public final class RulesManagerService extends IRulesManager.Stub {
    static final DistroFormatVersion DISTRO_FORMAT_VERSION_SUPPORTED = new DistroFormatVersion(TzDataSetVersion.currentFormatMajorVersion(), TzDataSetVersion.currentFormatMinorVersion());
    static final String REQUIRED_QUERY_PERMISSION = "android.permission.QUERY_TIME_ZONE_RULES";
    static final String REQUIRED_UPDATER_PERMISSION = "android.permission.UPDATE_TIME_ZONE_RULES";
    private static final String TAG = "timezone.RulesManagerService";
    private final Executor mExecutor;
    private final TimeZoneDistroInstaller mInstaller;
    private final RulesManagerIntentHelper mIntentHelper;
    private final AtomicBoolean mOperationInProgress = new AtomicBoolean(false);
    private final PackageTracker mPackageTracker;
    private final PermissionHelper mPermissionHelper;

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.timezone.RulesManagerService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v1, types: [com.android.server.timezone.RulesManagerService, java.lang.Object, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            ?? create = RulesManagerService.create(getContext());
            create.start();
            publishBinderService("timezone", create);
            publishLocalService(RulesManagerService.class, create);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static RulesManagerService create(Context context) {
        RulesManagerServiceHelperImpl helper = new RulesManagerServiceHelperImpl(context);
        File baseVersionFile = new File(TimeZoneDataFiles.getTimeZoneModuleTzVersionFile());
        File tzDataDir = new File(TimeZoneDataFiles.getDataTimeZoneRootDir());
        return new RulesManagerService(helper, helper, helper, PackageTracker.create(context), new TimeZoneDistroInstaller(TAG, baseVersionFile, tzDataDir));
    }

    RulesManagerService(PermissionHelper permissionHelper, Executor executor, RulesManagerIntentHelper intentHelper, PackageTracker packageTracker, TimeZoneDistroInstaller timeZoneDistroInstaller) {
        this.mPermissionHelper = permissionHelper;
        this.mExecutor = executor;
        this.mIntentHelper = intentHelper;
        this.mPackageTracker = packageTracker;
        this.mInstaller = timeZoneDistroInstaller;
    }

    public void start() {
        this.mPackageTracker.start();
    }

    public RulesState getRulesState() {
        this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_QUERY_PERMISSION);
        return getRulesStateInternal();
    }

    private RulesState getRulesStateInternal() {
        RulesState rulesState;
        synchronized (this) {
            try {
                try {
                    TzDataSetVersion baseVersion = this.mInstaller.readBaseVersion();
                    int distroStatus = 0;
                    DistroRulesVersion installedDistroRulesVersion = null;
                    try {
                        DistroVersion installedDistroVersion = this.mInstaller.getInstalledDistroVersion();
                        if (installedDistroVersion == null) {
                            distroStatus = 1;
                            installedDistroRulesVersion = null;
                        } else {
                            distroStatus = 2;
                            installedDistroRulesVersion = new DistroRulesVersion(installedDistroVersion.rulesVersion, installedDistroVersion.revision);
                        }
                    } catch (DistroException | IOException e) {
                        Slog.w(TAG, "Failed to read installed distro.", e);
                    }
                    boolean operationInProgress = this.mOperationInProgress.get();
                    DistroRulesVersion stagedDistroRulesVersion = null;
                    int stagedOperationStatus = 0;
                    if (!operationInProgress) {
                        try {
                            StagedDistroOperation stagedDistroOperation = this.mInstaller.getStagedDistroOperation();
                            if (stagedDistroOperation == null) {
                                stagedOperationStatus = 1;
                            } else if (stagedDistroOperation.isUninstall) {
                                stagedOperationStatus = 2;
                            } else {
                                stagedOperationStatus = 3;
                                DistroVersion stagedDistroVersion = stagedDistroOperation.distroVersion;
                                stagedDistroRulesVersion = new DistroRulesVersion(stagedDistroVersion.rulesVersion, stagedDistroVersion.revision);
                            }
                        } catch (DistroException | IOException e2) {
                            Slog.w(TAG, "Failed to read staged distro.", e2);
                        }
                    }
                    rulesState = new RulesState(baseVersion.getRulesVersion(), DISTRO_FORMAT_VERSION_SUPPORTED, operationInProgress, stagedOperationStatus, stagedDistroRulesVersion, distroStatus, installedDistroRulesVersion);
                } catch (IOException e3) {
                    Slog.w(TAG, "Failed to read base rules version", e3);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return rulesState;
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IF]}, finally: {[IF, CONST_STR, CONST_STR, INVOKE, MOVE_EXCEPTION, INVOKE, CONST_STR, CONST_STR, INVOKE, MOVE_EXCEPTION, IF] complete} */
    public int requestInstall(ParcelFileDescriptor distroParcelFileDescriptor, byte[] checkTokenBytes, ICallback callback) {
        try {
            this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_UPDATER_PERMISSION);
            CheckToken checkToken = null;
            if (checkTokenBytes != null) {
                checkToken = createCheckTokenOrThrow(checkTokenBytes);
            }
            EventLogTags.writeTimezoneRequestInstall(toStringOrNull(checkToken));
            synchronized (this) {
                if (distroParcelFileDescriptor == null) {
                    throw new NullPointerException("distroParcelFileDescriptor == null");
                }
                if (callback == null) {
                    throw new NullPointerException("observer == null");
                }
                if (this.mOperationInProgress.get()) {
                    return 1;
                }
                this.mOperationInProgress.set(true);
                this.mExecutor.execute(new InstallRunnable(distroParcelFileDescriptor, checkToken, callback));
                if (distroParcelFileDescriptor != null && 0 != 0) {
                    try {
                        distroParcelFileDescriptor.close();
                    } catch (IOException e) {
                        Slog.w(TAG, "Failed to close distroParcelFileDescriptor", e);
                    }
                }
                return 0;
            }
        } finally {
            if (distroParcelFileDescriptor != null && 1 != 0) {
                try {
                    distroParcelFileDescriptor.close();
                } catch (IOException e2) {
                    Slog.w(TAG, "Failed to close distroParcelFileDescriptor", e2);
                }
            }
        }
    }

    /* loaded from: classes2.dex */
    private class InstallRunnable implements Runnable {
        private final ICallback mCallback;
        private final CheckToken mCheckToken;
        private final ParcelFileDescriptor mDistroParcelFileDescriptor;

        InstallRunnable(ParcelFileDescriptor distroParcelFileDescriptor, CheckToken checkToken, ICallback callback) {
            this.mDistroParcelFileDescriptor = distroParcelFileDescriptor;
            this.mCheckToken = checkToken;
            this.mCallback = callback;
        }

        @Override // java.lang.Runnable
        public void run() {
            EventLogTags.writeTimezoneInstallStarted(RulesManagerService.toStringOrNull(this.mCheckToken));
            boolean success = false;
            try {
                try {
                    ParcelFileDescriptor pfd = this.mDistroParcelFileDescriptor;
                    try {
                        InputStream is = new FileInputStream(pfd.getFileDescriptor(), false);
                        TimeZoneDistro distro = new TimeZoneDistro(is);
                        int installerResult = RulesManagerService.this.mInstaller.stageInstallWithErrorCode(distro);
                        sendInstallNotificationIntentIfRequired(installerResult);
                        int resultCode = mapInstallerResultToApiCode(installerResult);
                        EventLogTags.writeTimezoneInstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), resultCode);
                        RulesManagerService.this.sendFinishedStatus(this.mCallback, resultCode);
                        success = true;
                        if (pfd != null) {
                            pfd.close();
                        }
                    } catch (Throwable th) {
                        if (pfd != null) {
                            try {
                                pfd.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                        }
                        throw th;
                    }
                } finally {
                    RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, false);
                    RulesManagerService.this.mOperationInProgress.set(false);
                }
            } catch (Exception e) {
                Slog.w(RulesManagerService.TAG, "Failed to install distro.", e);
                EventLogTags.writeTimezoneInstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), 1);
                RulesManagerService.this.sendFinishedStatus(this.mCallback, 1);
            }
        }

        private void sendInstallNotificationIntentIfRequired(int installerResult) {
            if (installerResult == 0) {
                RulesManagerService.this.mIntentHelper.sendTimeZoneOperationStaged();
            }
        }

        private int mapInstallerResultToApiCode(int installerResult) {
            switch (installerResult) {
                case 0:
                    return 0;
                case 1:
                    return 2;
                case 2:
                    return 3;
                case 3:
                    return 4;
                case 4:
                    return 5;
                default:
                    return 1;
            }
        }
    }

    public int requestUninstall(byte[] checkTokenBytes, ICallback callback) {
        this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_UPDATER_PERMISSION);
        CheckToken checkToken = null;
        if (checkTokenBytes != null) {
            checkToken = createCheckTokenOrThrow(checkTokenBytes);
        }
        EventLogTags.writeTimezoneRequestUninstall(toStringOrNull(checkToken));
        synchronized (this) {
            try {
                if (callback == null) {
                    throw new NullPointerException("callback == null");
                }
                if (this.mOperationInProgress.get()) {
                    return 1;
                }
                this.mOperationInProgress.set(true);
                this.mExecutor.execute(new UninstallRunnable(checkToken, callback));
                return 0;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    /* loaded from: classes2.dex */
    private class UninstallRunnable implements Runnable {
        private final ICallback mCallback;
        private final CheckToken mCheckToken;

        UninstallRunnable(CheckToken checkToken, ICallback callback) {
            this.mCheckToken = checkToken;
            this.mCallback = callback;
        }

        /* JADX WARN: Removed duplicated region for block: B:11:0x0024  */
        /* JADX WARN: Removed duplicated region for block: B:12:0x0026  */
        @Override // java.lang.Runnable
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void run() {
            int uninstallResult;
            boolean z;
            EventLogTags.writeTimezoneUninstallStarted(RulesManagerService.toStringOrNull(this.mCheckToken));
            boolean packageTrackerStatus = false;
            try {
                try {
                    uninstallResult = RulesManagerService.this.mInstaller.stageUninstall();
                    sendUninstallNotificationIntentIfRequired(uninstallResult);
                } catch (Exception e) {
                    EventLogTags.writeTimezoneUninstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), 1);
                    Slog.w(RulesManagerService.TAG, "Failed to uninstall distro.", e);
                    RulesManagerService.this.sendFinishedStatus(this.mCallback, 1);
                }
                if (uninstallResult != 0 && uninstallResult != 1) {
                    z = false;
                    packageTrackerStatus = z;
                    int callbackResultCode = !packageTrackerStatus ? 0 : 1;
                    EventLogTags.writeTimezoneUninstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), callbackResultCode);
                    RulesManagerService.this.sendFinishedStatus(this.mCallback, callbackResultCode);
                }
                z = true;
                packageTrackerStatus = z;
                if (!packageTrackerStatus) {
                }
                EventLogTags.writeTimezoneUninstallComplete(RulesManagerService.toStringOrNull(this.mCheckToken), callbackResultCode);
                RulesManagerService.this.sendFinishedStatus(this.mCallback, callbackResultCode);
            } finally {
                RulesManagerService.this.mPackageTracker.recordCheckResult(this.mCheckToken, packageTrackerStatus);
                RulesManagerService.this.mOperationInProgress.set(false);
            }
        }

        private void sendUninstallNotificationIntentIfRequired(int uninstallResult) {
            switch (uninstallResult) {
                case 0:
                    RulesManagerService.this.mIntentHelper.sendTimeZoneOperationStaged();
                    return;
                case 1:
                    RulesManagerService.this.mIntentHelper.sendTimeZoneOperationUnstaged();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendFinishedStatus(ICallback callback, int resultCode) {
        try {
            callback.onFinished(resultCode);
        } catch (RemoteException e) {
            Slog.e(TAG, "Unable to notify observer of result", e);
        }
    }

    public void requestNothing(byte[] checkTokenBytes, boolean success) {
        this.mPermissionHelper.enforceCallerHasPermission(REQUIRED_UPDATER_PERMISSION);
        CheckToken checkToken = null;
        if (checkTokenBytes != null) {
            checkToken = createCheckTokenOrThrow(checkTokenBytes);
        }
        EventLogTags.writeTimezoneRequestNothing(toStringOrNull(checkToken));
        this.mPackageTracker.recordCheckResult(checkToken, success);
        EventLogTags.writeTimezoneNothingComplete(toStringOrNull(checkToken));
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        char[] charArray;
        if (!this.mPermissionHelper.checkDumpPermission(TAG, pw)) {
            return;
        }
        RulesState rulesState = getRulesStateInternal();
        if (args != null && args.length == 2) {
            if ("-format_state".equals(args[0]) && args[1] != null) {
                for (char c : args[1].toCharArray()) {
                    switch (c) {
                        case 'a':
                            pw.println("Active rules version (ICU, ZoneInfoDb, TimeZoneFinder): " + TimeZone.getTZDataVersion() + "," + ZoneInfoDb.getInstance().getVersion() + "," + TimeZoneFinder.getInstance().getIanaVersion());
                            break;
                        case 'b':
                            String value = "Unknown";
                            if (rulesState != null) {
                                value = rulesState.getBaseRulesVersion();
                            }
                            pw.println("Base rules version: " + value);
                            break;
                        case 'c':
                            String value2 = "Unknown";
                            if (rulesState != null) {
                                value2 = distroStatusToString(rulesState.getDistroStatus());
                            }
                            pw.println("Current install state: " + value2);
                            break;
                        case 'i':
                            String value3 = "Unknown";
                            if (rulesState != null) {
                                DistroRulesVersion installedRulesVersion = rulesState.getInstalledDistroRulesVersion();
                                if (installedRulesVersion == null) {
                                    value3 = "<None>";
                                } else {
                                    value3 = installedRulesVersion.toDumpString();
                                }
                            }
                            pw.println("Installed rules version: " + value3);
                            break;
                        case 'o':
                            String value4 = "Unknown";
                            if (rulesState != null) {
                                int stagedOperationType = rulesState.getStagedOperationType();
                                value4 = stagedOperationToString(stagedOperationType);
                            }
                            pw.println("Staged operation: " + value4);
                            break;
                        case 'p':
                            String value5 = "Unknown";
                            if (rulesState != null) {
                                value5 = Boolean.toString(rulesState.isOperationInProgress());
                            }
                            pw.println("Operation in progress: " + value5);
                            break;
                        case 't':
                            String value6 = "Unknown";
                            if (rulesState != null) {
                                DistroRulesVersion stagedDistroRulesVersion = rulesState.getStagedDistroRulesVersion();
                                if (stagedDistroRulesVersion == null) {
                                    value6 = "<None>";
                                } else {
                                    value6 = stagedDistroRulesVersion.toDumpString();
                                }
                            }
                            pw.println("Staged rules version: " + value6);
                            break;
                        default:
                            pw.println("Unknown option: " + c);
                            break;
                    }
                }
                return;
            }
        }
        pw.println("RulesManagerService state: " + toString());
        pw.println("Active rules version (ICU, ZoneInfoDB, TimeZoneFinder): " + TimeZone.getTZDataVersion() + "," + ZoneInfoDb.getInstance().getVersion() + "," + TimeZoneFinder.getInstance().getIanaVersion());
        pw.println("Distro state: " + rulesState.toString());
        this.mPackageTracker.dump(pw);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyIdle() {
        this.mPackageTracker.triggerUpdateIfNeeded(false);
    }

    public String toString() {
        return "RulesManagerService{mOperationInProgress=" + this.mOperationInProgress + '}';
    }

    private static CheckToken createCheckTokenOrThrow(byte[] checkTokenBytes) {
        try {
            CheckToken checkToken = CheckToken.fromByteArray(checkTokenBytes);
            return checkToken;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read token bytes " + Arrays.toString(checkTokenBytes), e);
        }
    }

    private static String distroStatusToString(int distroStatus) {
        switch (distroStatus) {
            case 1:
                return "None";
            case 2:
                return "Installed";
            default:
                return "Unknown";
        }
    }

    private static String stagedOperationToString(int stagedOperationType) {
        switch (stagedOperationType) {
            case 1:
                return "None";
            case 2:
                return "Uninstall";
            case 3:
                return "Install";
            default:
                return "Unknown";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String toStringOrNull(Object obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }
}
