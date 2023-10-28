package com.android.server.pm;

import android.content.pm.IPackageMoveObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.pm.Installer;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUtils;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/* loaded from: classes2.dex */
public final class MovePackageHelper {
    final PackageManagerService mPm;

    public MovePackageHelper(PackageManagerService pm) {
        this.mPm = pm;
    }

    /* JADX WARN: Code restructure failed: missing block: B:100:0x0318, code lost:
        r1 = 16 | 2;
        r5 = com.android.server.pm.OriginInfo.fromExistingFile(r0);
        r6 = android.content.pm.parsing.result.ParseTypeImpl.forDefaultParsing();
        r7 = android.content.pm.parsing.ApkLiteParseUtils.parsePackageLite(r6, new java.io.File(r5.mResolvedPath), 0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:101:0x0332, code lost:
        if (r7.isSuccess() == false) goto L109;
     */
    /* JADX WARN: Code restructure failed: missing block: B:102:0x0334, code lost:
        r26 = (android.content.pm.parsing.PackageLite) r7.getResult();
     */
    /* JADX WARN: Code restructure failed: missing block: B:103:0x033d, code lost:
        r26 = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:104:0x033f, code lost:
        r8 = new com.android.server.pm.InstallParams(r5, r0, r0, r1, r33, r59, r62, r34, 0, r26, r57.mPm);
        r8.movePackage();
     */
    /* JADX WARN: Code restructure failed: missing block: B:105:0x035d, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:106:0x035e, code lost:
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:107:0x0369, code lost:
        throw new com.android.server.pm.PackageManagerException(-6, "Not enough free space to move");
     */
    /* JADX WARN: Code restructure failed: missing block: B:108:0x036a, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:77:0x0213, code lost:
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:80:0x0222, code lost:
        throw new com.android.server.pm.PackageManagerException(-6, "Failed to measure package size");
     */
    /* JADX WARN: Code restructure failed: missing block: B:81:0x0223, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:88:0x0260, code lost:
        if (com.android.server.pm.PackageManagerService.DEBUG_INSTALL == false) goto L96;
     */
    /* JADX WARN: Code restructure failed: missing block: B:89:0x0262, code lost:
        android.util.Slog.d("PackageManager", "Measured code size " + r0.codeSize + ", data size " + r0.dataSize);
     */
    /* JADX WARN: Code restructure failed: missing block: B:90:0x0288, code lost:
        r40 = r2.getUsableSpace();
     */
    /* JADX WARN: Code restructure failed: missing block: B:91:0x028c, code lost:
        if (r39 == false) goto L113;
     */
    /* JADX WARN: Code restructure failed: missing block: B:92:0x028e, code lost:
        r42 = r0.codeSize + r0.dataSize;
     */
    /* JADX WARN: Code restructure failed: missing block: B:93:0x0296, code lost:
        r0 = r0.codeSize;
        r42 = r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:95:0x02a0, code lost:
        if (r42 > r12.getStorageBytesUntilLow(r2)) goto L111;
     */
    /* JADX WARN: Code restructure failed: missing block: B:96:0x02a2, code lost:
        r57.mPm.mMoveCallbacks.notifyStatusChanged(r60, 10);
        r4 = new java.util.concurrent.CountDownLatch(1);
        r48 = r2;
        r0 = new com.android.server.pm.MovePackageHelper.AnonymousClass1(r57);
     */
    /* JADX WARN: Code restructure failed: missing block: B:97:0x02d3, code lost:
        if (r39 == false) goto L110;
     */
    /* JADX WARN: Code restructure failed: missing block: B:98:0x02d5, code lost:
        r11 = r42;
        new java.lang.Thread(new com.android.server.pm.MovePackageHelper$$ExternalSyntheticLambda0(r57, r4, r40, r48, r11, r60)).start();
        r0 = new com.android.server.pm.MoveInfo(r60, r5, r59, r58, r35, r36, r37, r38);
     */
    /* JADX WARN: Code restructure failed: missing block: B:99:0x030e, code lost:
        r0 = null;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void movePackageInternal(final String packageName, String volumeUuid, final int moveId, int callingUid, UserHandle user) throws PackageManagerException {
        String fromCodePath;
        PackageManagerTracedLock packageManagerTracedLock;
        Bundle extras;
        boolean moveCompleteApp;
        File measurePath;
        String label;
        AndroidPackage pkg;
        int length;
        int i;
        StorageManager storage = (StorageManager) this.mPm.mInjector.getSystemService(StorageManager.class);
        PackageManager pm = this.mPm.mContext.getPackageManager();
        Computer snapshot = this.mPm.snapshotComputer();
        PackageStateInternal packageState = snapshot.getPackageStateInternal(packageName);
        if (packageState != null && packageState.getPkg() != null && !snapshot.shouldFilterApplication(packageState, callingUid, user.getIdentifier())) {
            AndroidPackage pkg2 = packageState.getPkg();
            if (pkg2.isSystem()) {
                throw new PackageManagerException(-3, "Cannot move system application");
            }
            boolean isInternalStorage = "private".equals(volumeUuid);
            boolean allow3rdPartyOnInternal = this.mPm.mContext.getResources().getBoolean(17891344);
            if (isInternalStorage && !allow3rdPartyOnInternal) {
                throw new PackageManagerException(-9, "3rd party apps are not allowed on internal storage");
            }
            String currentVolumeUuid = packageState.getVolumeUuid();
            File probe = new File(pkg2.getPath());
            File probeOat = new File(probe, "oat");
            if (!probe.isDirectory() || !probeOat.isDirectory()) {
                throw new PackageManagerException(-6, "Move only supported for modern cluster style installs");
            }
            if (Objects.equals(currentVolumeUuid, volumeUuid)) {
                throw new PackageManagerException(-6, "Package already moved to " + volumeUuid);
            }
            if (!pkg2.isExternalStorage() && this.mPm.isPackageDeviceAdminOnAnyUser(snapshot, packageName)) {
                throw new PackageManagerException(-8, "Device admin cannot be moved");
            }
            if (snapshot.getFrozenPackages().containsKey(packageName)) {
                throw new PackageManagerException(-7, "Failed to move already frozen package");
            }
            final boolean isCurrentLocationExternal = pkg2.isExternalStorage();
            File codeFile = new File(pkg2.getPath());
            InstallSource installSource = packageState.getInstallSource();
            String packageAbiOverride = packageState.getCpuAbiOverride();
            int appId = UserHandle.getAppId(pkg2.getUid());
            String seinfo = AndroidPackageUtils.getSeInfo(pkg2, packageState);
            String label2 = String.valueOf(pm.getApplicationLabel(AndroidPackageUtils.generateAppInfoWithoutState(pkg2)));
            int targetSdkVersion = pkg2.getTargetSdkVersion();
            int[] installedUserIds = PackageStateUtils.queryInstalledUsers(packageState, this.mPm.mUserManager.getUserIds(), true);
            if (codeFile.getParentFile().getName().startsWith("~~")) {
                fromCodePath = codeFile.getParentFile().getAbsolutePath();
            } else {
                String fromCodePath2 = codeFile.getAbsolutePath();
                fromCodePath = fromCodePath2;
            }
            PackageManagerTracedLock packageManagerTracedLock2 = this.mPm.mLock;
            synchronized (packageManagerTracedLock2) {
                try {
                    try {
                        final PackageFreezer freezer = this.mPm.freezePackage(packageName, "movePackageInternal");
                        packageManagerTracedLock = packageManagerTracedLock2;
                        try {
                            Bundle extras2 = new Bundle();
                            extras2.putString("android.intent.extra.PACKAGE_NAME", packageName);
                            extras2.putString("android.intent.extra.TITLE", label2);
                            this.mPm.mMoveCallbacks.notifyCreated(moveId, extras2);
                            if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid)) {
                                moveCompleteApp = true;
                                extras = extras2;
                                measurePath = Environment.getDataAppDirectory(volumeUuid);
                            } else if (Objects.equals("primary_physical", volumeUuid)) {
                                moveCompleteApp = false;
                                extras = extras2;
                                measurePath = storage.getPrimaryPhysicalVolume().getPath();
                            } else {
                                VolumeInfo volume = storage.findVolumeByUuid(volumeUuid);
                                if (volume != null) {
                                    extras = extras2;
                                    if (volume.getType() == 1 && volume.isMountedWritable()) {
                                        moveCompleteApp = true;
                                        measurePath = Environment.getDataAppDirectory(volumeUuid);
                                    }
                                }
                                freezer.close();
                                throw new PackageManagerException(-6, "Move location not mounted private volume");
                            }
                            if (!moveCompleteApp) {
                                label = label2;
                                pkg = pkg2;
                            } else {
                                int length2 = installedUserIds.length;
                                int i2 = 0;
                                while (i2 < length2) {
                                    int i3 = length2;
                                    int userId = installedUserIds[i2];
                                    if (!StorageManager.isFileEncryptedNativeOrEmulated() || StorageManager.isUserKeyUnlocked(userId)) {
                                        i2++;
                                        length2 = i3;
                                        label2 = label2;
                                        pkg2 = pkg2;
                                    } else {
                                        freezer.close();
                                        throw new PackageManagerException(-10, "User " + userId + " must be unlocked");
                                    }
                                }
                                label = label2;
                                pkg = pkg2;
                            }
                            PackageStats stats = new PackageStats(null, -1);
                            synchronized (this.mPm.mInstallLock) {
                                try {
                                    length = installedUserIds.length;
                                    i = 0;
                                } catch (Throwable th) {
                                    th = th;
                                }
                                while (true) {
                                    if (i >= length) {
                                        break;
                                    }
                                    try {
                                        int i4 = length;
                                        if (!getPackageSizeInfoLI(packageName, installedUserIds[i], stats)) {
                                            break;
                                        }
                                        i++;
                                        length = i4;
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                    }
                                    throw th;
                                }
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th5) {
                                    th = th5;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        packageManagerTracedLock = packageManagerTracedLock2;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    packageManagerTracedLock = packageManagerTracedLock2;
                }
            }
        }
        throw new PackageManagerException(-2, "Missing package");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$movePackageInternal$0$com-android-server-pm-MovePackageHelper  reason: not valid java name */
    public /* synthetic */ void m5452x44d1fd50(CountDownLatch installedLatch, long startFreeBytes, File measurePath, long sizeBytes, int moveId) {
        while (!installedLatch.await(1L, TimeUnit.SECONDS)) {
            long deltaFreeBytes = startFreeBytes - measurePath.getUsableSpace();
            int progress = ((int) MathUtils.constrain((80 * deltaFreeBytes) / sizeBytes, 0L, 80L)) + 10;
            this.mPm.mMoveCallbacks.notifyStatusChanged(moveId, progress);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logAppMovedStorage(String packageName, boolean isPreviousLocationExternal) {
        AndroidPackage pkg;
        synchronized (this.mPm.mLock) {
            pkg = this.mPm.mPackages.get(packageName);
        }
        if (pkg == null) {
            return;
        }
        StorageManager storage = (StorageManager) this.mPm.mInjector.getSystemService(StorageManager.class);
        VolumeInfo volume = storage.findVolumeByUuid(StorageManager.convert(pkg.getVolumeUuid()).toString());
        int packageExternalStorageType = PackageManagerServiceUtils.getPackageExternalStorageType(volume, pkg.isExternalStorage());
        if (!isPreviousLocationExternal && pkg.isExternalStorage()) {
            FrameworkStatsLog.write(183, packageExternalStorageType, 1, packageName);
        } else if (isPreviousLocationExternal && !pkg.isExternalStorage()) {
            FrameworkStatsLog.write(183, packageExternalStorageType, 2, packageName);
        }
    }

    private boolean getPackageSizeInfoLI(String packageName, int userId, PackageStats stats) {
        synchronized (this.mPm.mLock) {
            PackageSetting ps = this.mPm.mSettings.getPackageLPr(packageName);
            if (ps == null) {
                Slog.w("PackageManager", "Failed to find settings for " + packageName);
                return false;
            }
            String[] packageNames = {packageName};
            long[] ceDataInodes = {ps.getCeDataInode(userId)};
            String[] codePaths = {ps.getPathString()};
            try {
                this.mPm.mInstaller.getAppSize(ps.getVolumeUuid(), packageNames, userId, 0, ps.getAppId(), ceDataInodes, codePaths, stats);
                if (PackageManagerServiceUtils.isSystemApp(ps) && !PackageManagerServiceUtils.isUpdatedSystemApp(ps)) {
                    stats.codeSize = 0L;
                }
                stats.dataSize -= stats.cacheSize;
                return true;
            } catch (Installer.InstallerException e) {
                Slog.w("PackageManager", String.valueOf(e));
                return false;
            }
        }
    }

    /* loaded from: classes2.dex */
    public static class MoveCallbacks extends Handler {
        private static final int MSG_CREATED = 1;
        private static final int MSG_STATUS_CHANGED = 2;
        private final RemoteCallbackList<IPackageMoveObserver> mCallbacks;
        public final SparseIntArray mLastStatus;

        public MoveCallbacks(Looper looper) {
            super(looper);
            this.mCallbacks = new RemoteCallbackList<>();
            this.mLastStatus = new SparseIntArray();
        }

        public void register(IPackageMoveObserver callback) {
            this.mCallbacks.register(callback);
        }

        public void unregister(IPackageMoveObserver callback) {
            this.mCallbacks.unregister(callback);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            SomeArgs args = (SomeArgs) msg.obj;
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                IPackageMoveObserver callback = this.mCallbacks.getBroadcastItem(i);
                try {
                    invokeCallback(callback, msg.what, args);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
            args.recycle();
        }

        private void invokeCallback(IPackageMoveObserver callback, int what, SomeArgs args) throws RemoteException {
            switch (what) {
                case 1:
                    callback.onCreated(args.argi1, (Bundle) args.arg2);
                    return;
                case 2:
                    callback.onStatusChanged(args.argi1, args.argi2, ((Long) args.arg3).longValue());
                    return;
                default:
                    return;
            }
        }

        public void notifyCreated(int moveId, Bundle extras) {
            Slog.v("PackageManager", "Move " + moveId + " created " + extras.toString());
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = moveId;
            args.arg2 = extras;
            obtainMessage(1, args).sendToTarget();
        }

        public void notifyStatusChanged(int moveId, int status) {
            notifyStatusChanged(moveId, status, -1L);
        }

        public void notifyStatusChanged(int moveId, int status, long estMillis) {
            Slog.v("PackageManager", "Move " + moveId + " status " + status);
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = moveId;
            args.argi2 = status;
            args.arg3 = Long.valueOf(estMillis);
            obtainMessage(2, args).sendToTarget();
            synchronized (this.mLastStatus) {
                this.mLastStatus.put(moveId, status);
            }
        }
    }
}
