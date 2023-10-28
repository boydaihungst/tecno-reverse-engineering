package com.android.server.rollback;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.VersionedPackage;
import android.content.rollback.PackageRollbackInfo;
import android.content.rollback.RollbackInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.ext.SdkExtensions;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.RescueParty;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class Rollback {
    static final int ROLLBACK_STATE_AVAILABLE = 1;
    static final int ROLLBACK_STATE_COMMITTED = 3;
    static final int ROLLBACK_STATE_DELETED = 4;
    static final int ROLLBACK_STATE_ENABLING = 0;
    private static final String TAG = "RollbackManager";
    public final RollbackInfo info;
    private final File mBackupDir;
    private final SparseIntArray mExtensionVersions;
    private final Handler mHandler;
    private final String mInstallerPackageName;
    private final int mOriginalSessionId;
    private final int[] mPackageSessionIds;
    private boolean mRestoreUserDataInProgress;
    private int mState;
    private String mStateDescription;
    private Instant mTimestamp;
    private final int mUserId;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    @interface RollbackState {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rollback(int rollbackId, File backupDir, int originalSessionId, boolean isStaged, int userId, String installerPackageName, int[] packageSessionIds, SparseIntArray extensionVersions) {
        this.mStateDescription = "";
        this.mRestoreUserDataInProgress = false;
        this.info = new RollbackInfo(rollbackId, new ArrayList(), isStaged, new ArrayList(), -1);
        this.mUserId = userId;
        this.mInstallerPackageName = installerPackageName;
        this.mBackupDir = backupDir;
        this.mOriginalSessionId = originalSessionId;
        this.mState = 0;
        this.mTimestamp = Instant.now();
        this.mPackageSessionIds = packageSessionIds != null ? packageSessionIds : new int[0];
        this.mExtensionVersions = (SparseIntArray) Objects.requireNonNull(extensionVersions);
        this.mHandler = Looper.myLooper() != null ? new Handler(Looper.myLooper()) : null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rollback(RollbackInfo info, File backupDir, Instant timestamp, int originalSessionId, int state, String stateDescription, boolean restoreUserDataInProgress, int userId, String installerPackageName, SparseIntArray extensionVersions) {
        this.mStateDescription = "";
        this.mRestoreUserDataInProgress = false;
        this.info = info;
        this.mUserId = userId;
        this.mInstallerPackageName = installerPackageName;
        this.mBackupDir = backupDir;
        this.mTimestamp = timestamp;
        this.mOriginalSessionId = originalSessionId;
        this.mState = state;
        this.mStateDescription = stateDescription;
        this.mRestoreUserDataInProgress = restoreUserDataInProgress;
        this.mExtensionVersions = (SparseIntArray) Objects.requireNonNull(extensionVersions);
        this.mPackageSessionIds = new int[0];
        this.mHandler = Looper.myLooper() != null ? new Handler(Looper.myLooper()) : null;
    }

    private void assertInWorkerThread() {
        Handler handler = this.mHandler;
        Preconditions.checkState(handler == null || handler.getLooper().isCurrentThread());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isStaged() {
        return this.info.isStaged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getBackupDir() {
        return this.mBackupDir;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Instant getTimestamp() {
        assertInWorkerThread();
        return this.mTimestamp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setTimestamp(Instant timestamp) {
        assertInWorkerThread();
        this.mTimestamp = timestamp;
        RollbackStore.saveRollback(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getOriginalSessionId() {
        return this.mOriginalSessionId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUserId() {
        return this.mUserId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getInstallerPackageName() {
        return this.mInstallerPackageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SparseIntArray getExtensionVersions() {
        return this.mExtensionVersions;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEnabling() {
        assertInWorkerThread();
        return this.mState == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAvailable() {
        assertInWorkerThread();
        return this.mState == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCommitted() {
        assertInWorkerThread();
        return this.mState == 3;
    }

    boolean isDeleted() {
        assertInWorkerThread();
        return this.mState == 4;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveRollback() {
        assertInWorkerThread();
        RollbackStore.saveRollback(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean enableForPackage(String packageName, long newVersion, long installedVersion, boolean isApex, String sourceDir, String[] splitSourceDirs, int rollbackDataPolicy) {
        assertInWorkerThread();
        try {
            RollbackStore.backupPackageCodePath(this, packageName, sourceDir);
            if (!ArrayUtils.isEmpty(splitSourceDirs)) {
                for (String dir : splitSourceDirs) {
                    RollbackStore.backupPackageCodePath(this, packageName, dir);
                }
            }
            PackageRollbackInfo packageRollbackInfo = new PackageRollbackInfo(new VersionedPackage(packageName, newVersion), new VersionedPackage(packageName, installedVersion), new ArrayList(), new ArrayList(), isApex, false, new ArrayList(), rollbackDataPolicy);
            this.info.getPackages().add(packageRollbackInfo);
            return true;
        } catch (IOException e) {
            Slog.e(TAG, "Unable to copy package for rollback for " + packageName, e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean enableForPackageInApex(String packageName, long installedVersion, int rollbackDataPolicy) {
        assertInWorkerThread();
        PackageRollbackInfo packageRollbackInfo = new PackageRollbackInfo(new VersionedPackage(packageName, 0), new VersionedPackage(packageName, installedVersion), new ArrayList(), new ArrayList(), false, true, new ArrayList(), rollbackDataPolicy);
        this.info.getPackages().add(packageRollbackInfo);
        return true;
    }

    private static void addAll(List<Integer> list, int[] arr) {
        for (int i : arr) {
            list.add(Integer.valueOf(i));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void snapshotUserData(String packageName, int[] userIds, AppDataRollbackHelper dataHelper) {
        assertInWorkerThread();
        if (!isEnabling()) {
            return;
        }
        for (PackageRollbackInfo pkgRollbackInfo : this.info.getPackages()) {
            if (pkgRollbackInfo.getPackageName().equals(packageName)) {
                if (pkgRollbackInfo.getRollbackDataPolicy() == 0) {
                    dataHelper.snapshotAppData(this.info.getRollbackId(), pkgRollbackInfo, userIds);
                    addAll(pkgRollbackInfo.getSnapshottedUsers(), userIds);
                    RollbackStore.saveRollback(this);
                    return;
                }
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void commitPendingBackupAndRestoreForUser(int userId, AppDataRollbackHelper dataHelper) {
        assertInWorkerThread();
        if (dataHelper.commitPendingBackupAndRestoreForUser(userId, this)) {
            RollbackStore.saveRollback(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void makeAvailable() {
        assertInWorkerThread();
        if (isDeleted()) {
            Slog.w(TAG, "Cannot make deleted rollback available.");
            return;
        }
        setState(1, "");
        this.mTimestamp = Instant.now();
        RollbackStore.saveRollback(this);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [589=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:134:0x01a2 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:69:0x017f A[Catch: all -> 0x016c, TRY_ENTER, TRY_LEAVE, TryCatch #9 {all -> 0x016c, blocks: (B:57:0x0166, B:69:0x017f), top: B:106:0x0166 }] */
    /* JADX WARN: Removed duplicated region for block: B:77:0x019f A[Catch: IOException -> 0x01fa, TRY_ENTER, TRY_LEAVE, TryCatch #12 {IOException -> 0x01fa, blocks: (B:51:0x0146, B:77:0x019f, B:87:0x01ba, B:88:0x01bb, B:91:0x01d1, B:82:0x01b1), top: B:124:0x0146 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void commit(final Context context, final List<VersionedPackage> causePackages, String callerPackageName, final IntentSender statusReceiver) {
        PackageInstaller.SessionParams params;
        Throwable th;
        int i;
        PackageInstaller.Session session;
        Exception ignore;
        assertInWorkerThread();
        if (!isAvailable()) {
            RollbackManagerServiceImpl.sendFailure(context, statusReceiver, 2, "Rollback unavailable");
            return;
        }
        boolean z = true;
        if (containsApex() && wasCreatedAtLowerExtensionVersion()) {
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            if (extensionVersionReductionWouldViolateConstraint(this.mExtensionVersions, pmi)) {
                RollbackManagerServiceImpl.sendFailure(context, statusReceiver, 1, "Rollback may violate a minExtensionVersion constraint");
                return;
            }
        }
        try {
            Context pkgContext = context.createPackageContextAsUser(callerPackageName, 0, UserHandle.of(this.mUserId));
            PackageManager pm = pkgContext.getPackageManager();
            try {
                PackageInstaller packageInstaller = pm.getPackageInstaller();
                PackageInstaller.SessionParams parentParams = new PackageInstaller.SessionParams(1);
                parentParams.setRequestDowngrade(true);
                parentParams.setMultiPackage();
                if (isStaged()) {
                    try {
                        parentParams.setStaged();
                    } catch (IOException e) {
                        e = e;
                        Slog.e(TAG, "Rollback failed", e);
                        RollbackManagerServiceImpl.sendFailure(context, statusReceiver, 1, "IOException: " + e.toString());
                    }
                }
                parentParams.setInstallReason(5);
                int parentSessionId = packageInstaller.createSession(parentParams);
                PackageInstaller.Session parentSession = packageInstaller.openSession(parentSessionId);
                List<String> packageNames = new ArrayList<>(this.info.getPackages().size());
                for (PackageRollbackInfo pkgRollbackInfo : this.info.getPackages()) {
                    try {
                        packageNames.add(pkgRollbackInfo.getPackageName());
                        if (!pkgRollbackInfo.isApkInApex()) {
                            PackageInstaller.SessionParams params2 = new PackageInstaller.SessionParams(z ? 1 : 0);
                            String installerPackageName = this.mInstallerPackageName;
                            String installerPackageName2 = TextUtils.isEmpty(installerPackageName) ? pm.getInstallerPackageName(pkgRollbackInfo.getPackageName()) : installerPackageName;
                            if (installerPackageName2 != null) {
                                params = params2;
                                params.setInstallerPackageName(installerPackageName2);
                            } else {
                                params = params2;
                            }
                            params.setRequestDowngrade(z);
                            params.setRequiredInstalledVersionCode(pkgRollbackInfo.getVersionRolledBackFrom().getLongVersionCode());
                            params.setInstallReason(5);
                            if (isStaged()) {
                                params.setStaged();
                            }
                            if (pkgRollbackInfo.isApex()) {
                                params.setInstallAsApex();
                            }
                            int sessionId = packageInstaller.createSession(params);
                            PackageInstaller.Session session2 = packageInstaller.openSession(sessionId);
                            File[] packageCodePaths = RollbackStore.getPackageCodePaths(this, pkgRollbackInfo.getPackageName());
                            if (packageCodePaths == null) {
                                RollbackManagerServiceImpl.sendFailure(context, statusReceiver, 1, "Backup copy of package: " + pkgRollbackInfo.getPackageName() + " is inaccessible");
                                return;
                            }
                            int length = packageCodePaths.length;
                            Context pkgContext2 = pkgContext;
                            int i2 = 0;
                            while (i2 < length) {
                                try {
                                    File packageCodePath = packageCodePaths[i2];
                                    File[] packageCodePaths2 = packageCodePaths;
                                    ParcelFileDescriptor fd = ParcelFileDescriptor.open(packageCodePath, 268435456);
                                    try {
                                        long token = Binder.clearCallingIdentity();
                                        try {
                                            i = length;
                                            session = session2;
                                            try {
                                                try {
                                                    session.stageViaHardLink(packageCodePath.getAbsolutePath());
                                                    ignore = null;
                                                } catch (Exception e2) {
                                                    ignore = 1;
                                                    if (ignore != null) {
                                                    }
                                                    Binder.restoreCallingIdentity(token);
                                                    if (fd == null) {
                                                    }
                                                    i2++;
                                                    session2 = session;
                                                    packageCodePaths = packageCodePaths2;
                                                    length = i;
                                                }
                                            } catch (Throwable th2) {
                                                th = th2;
                                                Binder.restoreCallingIdentity(token);
                                                throw th;
                                            }
                                        } catch (Exception e3) {
                                            i = length;
                                            session = session2;
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                        if (ignore != null) {
                                            session.write(packageCodePath.getName(), 0L, packageCodePath.length(), fd);
                                        }
                                        try {
                                            Binder.restoreCallingIdentity(token);
                                            if (fd == null) {
                                                fd.close();
                                            }
                                            i2++;
                                            session2 = session;
                                            packageCodePaths = packageCodePaths2;
                                            length = i;
                                        } catch (Throwable th4) {
                                            th = th4;
                                            if (fd != null) {
                                                fd.close();
                                            }
                                            throw th;
                                        }
                                    } catch (Throwable th5) {
                                        th = th5;
                                    }
                                } catch (IOException e4) {
                                    e = e4;
                                    Slog.e(TAG, "Rollback failed", e);
                                    RollbackManagerServiceImpl.sendFailure(context, statusReceiver, 1, "IOException: " + e.toString());
                                }
                            }
                            parentSession.addChildSessionId(sessionId);
                            pkgContext = pkgContext2;
                            z = true;
                        }
                    } catch (IOException e5) {
                        e = e5;
                    }
                }
                RescueParty.resetDeviceConfigForPackages(packageNames);
                try {
                    Consumer<Intent> onResult = new Consumer() { // from class: com.android.server.rollback.Rollback$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            Rollback.this.m6346lambda$commit$1$comandroidserverrollbackRollback(context, statusReceiver, causePackages, (Intent) obj);
                        }
                    };
                    LocalIntentReceiver receiver = new LocalIntentReceiver(onResult);
                    setState(3, "");
                    this.info.setCommittedSessionId(parentSessionId);
                    this.mRestoreUserDataInProgress = true;
                    parentSession.commit(receiver.getIntentSender());
                } catch (IOException e6) {
                    e = e6;
                    Slog.e(TAG, "Rollback failed", e);
                    RollbackManagerServiceImpl.sendFailure(context, statusReceiver, 1, "IOException: " + e.toString());
                }
            } catch (IOException e7) {
                e = e7;
            }
        } catch (PackageManager.NameNotFoundException e8) {
            RollbackManagerServiceImpl.sendFailure(context, statusReceiver, 1, "Invalid callerPackageName");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$commit$1$com-android-server-rollback-Rollback  reason: not valid java name */
    public /* synthetic */ void m6346lambda$commit$1$comandroidserverrollbackRollback(final Context context, final IntentSender statusReceiver, final List causePackages, final Intent result) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.rollback.Rollback$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                Rollback.this.m6345lambda$commit$0$comandroidserverrollbackRollback(result, context, statusReceiver, causePackages);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$commit$0$com-android-server-rollback-Rollback  reason: not valid java name */
    public /* synthetic */ void m6345lambda$commit$0$comandroidserverrollbackRollback(Intent result, Context context, IntentSender statusReceiver, List causePackages) {
        assertInWorkerThread();
        int status = result.getIntExtra("android.content.pm.extra.STATUS", 1);
        if (status != 0) {
            setState(1, "Commit failed");
            this.mRestoreUserDataInProgress = false;
            this.info.setCommittedSessionId(-1);
            RollbackManagerServiceImpl.sendFailure(context, statusReceiver, 3, "Rollback downgrade install failed: " + result.getStringExtra("android.content.pm.extra.STATUS_MESSAGE"));
            return;
        }
        if (!isStaged()) {
            this.mRestoreUserDataInProgress = false;
        }
        this.info.getCausePackages().addAll(causePackages);
        RollbackStore.deletePackageCodePaths(this);
        RollbackStore.saveRollback(this);
        try {
            Intent fillIn = new Intent();
            fillIn.putExtra("android.content.rollback.extra.STATUS", 0);
            statusReceiver.sendIntent(context, 0, fillIn, null, null);
        } catch (IntentSender.SendIntentException e) {
        }
        Intent broadcast = new Intent("android.intent.action.ROLLBACK_COMMITTED");
        UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
        for (UserHandle user : userManager.getUserHandles(true)) {
            context.sendBroadcastAsUser(broadcast, user, "android.permission.MANAGE_ROLLBACKS");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean restoreUserDataForPackageIfInProgress(String packageName, int[] userIds, int appId, String seInfo, AppDataRollbackHelper dataHelper) {
        assertInWorkerThread();
        if (isRestoreUserDataInProgress()) {
            boolean foundPackage = false;
            Iterator it = this.info.getPackages().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                PackageRollbackInfo pkgRollbackInfo = (PackageRollbackInfo) it.next();
                if (pkgRollbackInfo.getPackageName().equals(packageName)) {
                    foundPackage = true;
                    boolean changedRollback = false;
                    for (int userId : userIds) {
                        changedRollback |= dataHelper.restoreAppData(this.info.getRollbackId(), pkgRollbackInfo, userId, appId, seInfo);
                    }
                    if (changedRollback) {
                        RollbackStore.saveRollback(this);
                    }
                }
            }
            return foundPackage;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void delete(AppDataRollbackHelper dataHelper, String reason) {
        assertInWorkerThread();
        boolean containsApex = false;
        Set<Integer> apexUsers = new ArraySet<>();
        for (PackageRollbackInfo pkgInfo : this.info.getPackages()) {
            List<Integer> snapshottedUsers = pkgInfo.getSnapshottedUsers();
            if (pkgInfo.isApex()) {
                containsApex = true;
                apexUsers.addAll(snapshottedUsers);
            } else {
                for (int i = 0; i < snapshottedUsers.size(); i++) {
                    int userId = snapshottedUsers.get(i).intValue();
                    dataHelper.destroyAppDataSnapshot(this.info.getRollbackId(), pkgInfo, userId);
                }
            }
        }
        if (containsApex) {
            dataHelper.destroyApexDeSnapshots(this.info.getRollbackId());
            for (Integer num : apexUsers) {
                int user = num.intValue();
                dataHelper.destroyApexCeSnapshots(user, this.info.getRollbackId());
            }
        }
        RollbackStore.deleteRollback(this);
        setState(4, reason);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRestoreUserDataInProgress() {
        assertInWorkerThread();
        return this.mRestoreUserDataInProgress;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRestoreUserDataInProgress(boolean restoreUserDataInProgress) {
        assertInWorkerThread();
        this.mRestoreUserDataInProgress = restoreUserDataInProgress;
        RollbackStore.saveRollback(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean includesPackage(String packageName) {
        assertInWorkerThread();
        for (PackageRollbackInfo packageRollbackInfo : this.info.getPackages()) {
            if (packageRollbackInfo.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean includesPackageWithDifferentVersion(String packageName, long versionCode) {
        assertInWorkerThread();
        for (PackageRollbackInfo pkgRollbackInfo : this.info.getPackages()) {
            if (pkgRollbackInfo.getPackageName().equals(packageName) && pkgRollbackInfo.getVersionRolledBackFrom().getLongVersionCode() != versionCode) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<String> getPackageNames() {
        assertInWorkerThread();
        List<String> result = new ArrayList<>();
        for (PackageRollbackInfo pkgRollbackInfo : this.info.getPackages()) {
            result.add(pkgRollbackInfo.getPackageName());
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<String> getApexPackageNames() {
        assertInWorkerThread();
        List<String> result = new ArrayList<>();
        for (PackageRollbackInfo pkgRollbackInfo : this.info.getPackages()) {
            if (pkgRollbackInfo.isApex()) {
                result.add(pkgRollbackInfo.getPackageName());
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsSessionId(int packageSessionId) {
        int[] iArr;
        for (int id : this.mPackageSessionIds) {
            if (id == packageSessionId) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean allPackagesEnabled() {
        assertInWorkerThread();
        int packagesWithoutApkInApex = 0;
        for (PackageRollbackInfo rollbackInfo : this.info.getPackages()) {
            if (!rollbackInfo.isApkInApex()) {
                packagesWithoutApkInApex++;
            }
        }
        return packagesWithoutApkInApex == this.mPackageSessionIds.length;
    }

    static String rollbackStateToString(int state) {
        switch (state) {
            case 0:
                return "enabling";
            case 1:
                return "available";
            case 2:
            default:
                throw new AssertionError("Invalid rollback state: " + state);
            case 3:
                return "committed";
            case 4:
                return "deleted";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static int rollbackStateFromString(String state) throws ParseException {
        char c;
        switch (state.hashCode()) {
            case -1491142788:
                if (state.equals("committed")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -733902135:
                if (state.equals("available")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1550463001:
                if (state.equals("deleted")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1642196352:
                if (state.equals("enabling")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 4;
            default:
                throw new ParseException("Invalid rollback state: " + state, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getStateAsString() {
        assertInWorkerThread();
        return rollbackStateToString(this.mState);
    }

    static boolean extensionVersionReductionWouldViolateConstraint(SparseIntArray rollbackExtVers, PackageManagerInternal pmi) {
        if (rollbackExtVers.size() == 0) {
            return false;
        }
        List<String> packages = pmi.getPackageList().getPackageNames();
        for (int i = 0; i < packages.size(); i++) {
            AndroidPackage pkg = pmi.getPackage(packages.get(i));
            SparseIntArray minExtVers = pkg.getMinExtensionVersions();
            if (minExtVers != null) {
                for (int j = 0; j < rollbackExtVers.size(); j++) {
                    int minExt = minExtVers.get(rollbackExtVers.keyAt(j), -1);
                    if (rollbackExtVers.valueAt(j) < minExt) {
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    private boolean wasCreatedAtLowerExtensionVersion() {
        for (int i = 0; i < this.mExtensionVersions.size(); i++) {
            if (SdkExtensions.getExtensionVersion(this.mExtensionVersions.keyAt(i)) > this.mExtensionVersions.valueAt(i)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsApex() {
        for (PackageRollbackInfo pkgInfo : this.info.getPackages()) {
            if (pkgInfo.isApex()) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter ipw) {
        assertInWorkerThread();
        ipw.println(this.info.getRollbackId() + ":");
        ipw.increaseIndent();
        ipw.println("-state: " + getStateAsString());
        ipw.println("-stateDescription: " + this.mStateDescription);
        ipw.println("-timestamp: " + getTimestamp());
        ipw.println("-isStaged: " + isStaged());
        ipw.println("-originalSessionId: " + getOriginalSessionId());
        ipw.println("-packages:");
        ipw.increaseIndent();
        for (PackageRollbackInfo pkg : this.info.getPackages()) {
            ipw.println(pkg.getPackageName() + " " + pkg.getVersionRolledBackFrom().getLongVersionCode() + " -> " + pkg.getVersionRolledBackTo().getLongVersionCode());
        }
        ipw.decreaseIndent();
        if (isCommitted()) {
            ipw.println("-causePackages:");
            ipw.increaseIndent();
            for (VersionedPackage cPkg : this.info.getCausePackages()) {
                ipw.println(cPkg.getPackageName() + " " + cPkg.getLongVersionCode());
            }
            ipw.decreaseIndent();
            ipw.println("-committedSessionId: " + this.info.getCommittedSessionId());
        }
        if (this.mExtensionVersions.size() > 0) {
            ipw.println("-extensionVersions:");
            ipw.increaseIndent();
            ipw.println(this.mExtensionVersions.toString());
            ipw.decreaseIndent();
        }
        ipw.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getStateDescription() {
        assertInWorkerThread();
        return this.mStateDescription;
    }

    void setState(int state, String description) {
        assertInWorkerThread();
        this.mState = state;
        this.mStateDescription = description;
    }
}
