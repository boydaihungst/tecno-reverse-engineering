package com.android.server.pm.dex;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.dex.ArtManager;
import android.content.pm.dex.ArtManagerInternal;
import android.content.pm.dex.DexMetadataHelper;
import android.content.pm.dex.IArtManager;
import android.content.pm.dex.ISnapshotRuntimeProfileCallback;
import android.content.pm.dex.PackageOptimizationInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.system.Os;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.RoSystemProperties;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.UiModeManagerService;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerServiceCompilerMapping;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.parsing.PackageInfoWithoutStateUtils;
import dalvik.system.DexFile;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class ArtManagerService extends IArtManager.Stub {
    private static final String BOOT_IMAGE_ANDROID_PACKAGE = "android";
    private static final String BOOT_IMAGE_PROFILE_NAME = "android.prof";
    public static final String DEXOPT_REASON_WITH_DEX_METADATA_ANNOTATION = "-dm";
    private static final int TRON_COMPILATION_FILTER_ASSUMED_VERIFIED = 2;
    private static final int TRON_COMPILATION_FILTER_ASSUMED_VERIFIED_IORAP = 15;
    private static final int TRON_COMPILATION_FILTER_ERROR = 0;
    private static final int TRON_COMPILATION_FILTER_EVERYTHING = 11;
    private static final int TRON_COMPILATION_FILTER_EVERYTHING_IORAP = 24;
    private static final int TRON_COMPILATION_FILTER_EVERYTHING_PROFILE = 10;
    private static final int TRON_COMPILATION_FILTER_EVERYTHING_PROFILE_IORAP = 23;
    private static final int TRON_COMPILATION_FILTER_EXTRACT = 3;
    private static final int TRON_COMPILATION_FILTER_EXTRACT_IORAP = 16;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_APK = 12;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_APK_FALLBACK = 13;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_APK_FALLBACK_IORAP = 26;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_APK_IORAP = 25;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_VDEX_FALLBACK = 14;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_VDEX_FALLBACK_IORAP = 27;
    private static final int TRON_COMPILATION_FILTER_QUICKEN = 5;
    private static final int TRON_COMPILATION_FILTER_QUICKEN_IORAP = 18;
    private static final int TRON_COMPILATION_FILTER_SPACE = 7;
    private static final int TRON_COMPILATION_FILTER_SPACE_IORAP = 20;
    private static final int TRON_COMPILATION_FILTER_SPACE_PROFILE = 6;
    private static final int TRON_COMPILATION_FILTER_SPACE_PROFILE_IORAP = 19;
    private static final int TRON_COMPILATION_FILTER_SPEED = 9;
    private static final int TRON_COMPILATION_FILTER_SPEED_IORAP = 22;
    private static final int TRON_COMPILATION_FILTER_SPEED_PROFILE = 8;
    private static final int TRON_COMPILATION_FILTER_SPEED_PROFILE_IORAP = 21;
    private static final int TRON_COMPILATION_FILTER_UNKNOWN = 1;
    private static final int TRON_COMPILATION_FILTER_VERIFY = 4;
    private static final int TRON_COMPILATION_FILTER_VERIFY_IORAP = 17;
    private static final int TRON_COMPILATION_REASON_AB_OTA = 6;
    private static final int TRON_COMPILATION_REASON_BG_DEXOPT = 5;
    private static final int TRON_COMPILATION_REASON_BOOT_AFTER_OTA = 20;
    private static final int TRON_COMPILATION_REASON_BOOT_DEPRECATED_SINCE_S = 3;
    private static final int TRON_COMPILATION_REASON_CMDLINE = 22;
    private static final int TRON_COMPILATION_REASON_ERROR = 0;
    private static final int TRON_COMPILATION_REASON_FIRST_BOOT = 2;
    private static final int TRON_COMPILATION_REASON_INACTIVE = 7;
    private static final int TRON_COMPILATION_REASON_INSTALL = 4;
    private static final int TRON_COMPILATION_REASON_INSTALL_BULK = 11;
    private static final int TRON_COMPILATION_REASON_INSTALL_BULK_DOWNGRADED = 13;
    private static final int TRON_COMPILATION_REASON_INSTALL_BULK_DOWNGRADED_WITH_DM = 18;
    private static final int TRON_COMPILATION_REASON_INSTALL_BULK_SECONDARY = 12;
    private static final int TRON_COMPILATION_REASON_INSTALL_BULK_SECONDARY_DOWNGRADED = 14;
    private static final int TRON_COMPILATION_REASON_INSTALL_BULK_SECONDARY_DOWNGRADED_WITH_DM = 19;
    private static final int TRON_COMPILATION_REASON_INSTALL_BULK_SECONDARY_WITH_DM = 17;
    private static final int TRON_COMPILATION_REASON_INSTALL_BULK_WITH_DM = 16;
    private static final int TRON_COMPILATION_REASON_INSTALL_FAST = 10;
    private static final int TRON_COMPILATION_REASON_INSTALL_FAST_WITH_DM = 15;
    private static final int TRON_COMPILATION_REASON_INSTALL_WITH_DM = 9;
    private static final int TRON_COMPILATION_REASON_POST_BOOT = 21;
    private static final int TRON_COMPILATION_REASON_PREBUILT = 23;
    private static final int TRON_COMPILATION_REASON_SHARED = 8;
    private static final int TRON_COMPILATION_REASON_UNKNOWN = 1;
    private static final int TRON_COMPILATION_REASON_VDEX = 24;
    private final Context mContext;
    private final Handler mHandler = new Handler(BackgroundThread.getHandler().getLooper());
    private final Installer mInstaller;
    private IPackageManager mPackageManager;
    private static final String TAG = "ArtManagerService";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    static {
        verifyTronLoggingConstants();
    }

    public ArtManagerService(Context context, Installer installer, Object ignored) {
        this.mContext = context;
        this.mInstaller = installer;
        LocalServices.addService(ArtManagerInternal.class, new ArtManagerInternalImpl());
    }

    private IPackageManager getPackageManager() {
        if (this.mPackageManager == null) {
            this.mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        }
        return this.mPackageManager;
    }

    private boolean checkAndroidPermissions(int callingUid, String callingPackage) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_RUNTIME_PROFILES", TAG);
        switch (((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).noteOp(43, callingUid, callingPackage)) {
            case 0:
                return true;
            case 3:
                this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", TAG);
                return true;
            default:
                return false;
        }
    }

    private boolean checkShellPermissions(int profileType, String packageName, int callingUid) {
        if (callingUid != 2000) {
            return false;
        }
        if (RoSystemProperties.DEBUGGABLE) {
            return true;
        }
        if (profileType == 1) {
            return false;
        }
        PackageInfo info = null;
        try {
            info = getPackageManager().getPackageInfo(packageName, 0L, 0);
        } catch (RemoteException e) {
        }
        return info != null && (info.applicationInfo.flags & 2) == 2;
    }

    public void snapshotRuntimeProfile(int profileType, String packageName, String codePath, ISnapshotRuntimeProfileCallback callback, String callingPackage) {
        int callingUid = Binder.getCallingUid();
        if (!checkShellPermissions(profileType, packageName, callingUid) && !checkAndroidPermissions(callingUid, callingPackage)) {
            try {
                callback.onError(2);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        Objects.requireNonNull(callback);
        boolean bootImageProfile = profileType == 1;
        if (!bootImageProfile) {
            Preconditions.checkStringNotEmpty(codePath);
            Preconditions.checkStringNotEmpty(packageName);
        }
        if (!isRuntimeProfilingEnabled(profileType, callingPackage)) {
            throw new IllegalStateException("Runtime profiling is not enabled for " + profileType);
        }
        if (DEBUG) {
            Slog.d(TAG, "Requested snapshot for " + packageName + ":" + codePath);
        }
        if (bootImageProfile) {
            snapshotBootImageProfile(callback);
        } else {
            snapshotAppProfile(packageName, codePath, callback);
        }
    }

    private void snapshotAppProfile(String packageName, String codePath, ISnapshotRuntimeProfileCallback callback) {
        PackageInfo info = null;
        try {
            info = getPackageManager().getPackageInfo(packageName, 0L, 0);
        } catch (RemoteException e) {
        }
        if (info == null) {
            postError(callback, packageName, 0);
            return;
        }
        boolean pathFound = info.applicationInfo.getBaseCodePath().equals(codePath);
        String splitName = null;
        String[] splitCodePaths = info.applicationInfo.getSplitCodePaths();
        if (!pathFound && splitCodePaths != null) {
            int i = splitCodePaths.length - 1;
            while (true) {
                if (i < 0) {
                    break;
                } else if (!splitCodePaths[i].equals(codePath)) {
                    i--;
                } else {
                    pathFound = true;
                    splitName = info.applicationInfo.splitNames[i];
                    break;
                }
            }
        }
        if (!pathFound) {
            postError(callback, packageName, 1);
            return;
        }
        int appId = UserHandle.getAppId(info.applicationInfo.uid);
        if (appId < 0) {
            postError(callback, packageName, 2);
            Slog.wtf(TAG, "AppId is -1 for package: " + packageName);
            return;
        }
        createProfileSnapshot(packageName, ArtManager.getProfileName(splitName), codePath, appId, callback);
        destroyProfileSnapshot(packageName, ArtManager.getProfileName(splitName));
    }

    private void createProfileSnapshot(String packageName, String profileName, String classpath, int appId, ISnapshotRuntimeProfileCallback callback) {
        try {
            if (!this.mInstaller.createProfileSnapshot(appId, packageName, profileName, classpath)) {
                postError(callback, packageName, 2);
                return;
            }
            File snapshotProfile = ArtManager.getProfileSnapshotFileForName(packageName, profileName);
            try {
                ParcelFileDescriptor fd = ParcelFileDescriptor.open(snapshotProfile, 268435456);
                if (fd != null && fd.getFileDescriptor().valid()) {
                    postSuccess(packageName, fd, callback);
                }
                postError(callback, packageName, 2);
            } catch (FileNotFoundException e) {
                Slog.w(TAG, "Could not open snapshot profile for " + packageName + ":" + snapshotProfile, e);
                postError(callback, packageName, 2);
            }
        } catch (Installer.InstallerException e2) {
            postError(callback, packageName, 2);
        }
    }

    private void destroyProfileSnapshot(String packageName, String profileName) {
        if (DEBUG) {
            Slog.d(TAG, "Destroying profile snapshot for" + packageName + ":" + profileName);
        }
        try {
            this.mInstaller.destroyProfileSnapshot(packageName, profileName);
        } catch (Installer.InstallerException e) {
            Slog.e(TAG, "Failed to destroy profile snapshot for " + packageName + ":" + profileName, e);
        }
    }

    public boolean isRuntimeProfilingEnabled(int profileType, String callingPackage) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 2000 && !checkAndroidPermissions(callingUid, callingPackage)) {
            return false;
        }
        switch (profileType) {
            case 0:
                return SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false);
            case 1:
                boolean profileBootClassPath = SystemProperties.getBoolean("persist.device_config.runtime_native_boot.profilebootclasspath", SystemProperties.getBoolean("dalvik.vm.profilebootclasspath", false));
                return (Build.IS_USERDEBUG || Build.IS_ENG) && SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false) && profileBootClassPath;
            default:
                throw new IllegalArgumentException("Invalid profile type:" + profileType);
        }
    }

    private void snapshotBootImageProfile(ISnapshotRuntimeProfileCallback callback) {
        String classpath = String.join(":", Os.getenv("BOOTCLASSPATH"), Os.getenv("SYSTEMSERVERCLASSPATH"));
        createProfileSnapshot("android", BOOT_IMAGE_PROFILE_NAME, classpath, -1, callback);
        destroyProfileSnapshot("android", BOOT_IMAGE_PROFILE_NAME);
    }

    private void postError(final ISnapshotRuntimeProfileCallback callback, final String packageName, final int errCode) {
        if (DEBUG) {
            Slog.d(TAG, "Failed to snapshot profile for " + packageName + " with error: " + errCode);
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.dex.ArtManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ArtManagerService.lambda$postError$0(callback, errCode, packageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$postError$0(ISnapshotRuntimeProfileCallback callback, int errCode, String packageName) {
        try {
            callback.onError(errCode);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to callback after profile snapshot for " + packageName, e);
        }
    }

    private void postSuccess(final String packageName, final ParcelFileDescriptor fd, final ISnapshotRuntimeProfileCallback callback) {
        if (DEBUG) {
            Slog.d(TAG, "Successfully snapshot profile for " + packageName);
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.pm.dex.ArtManagerService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ArtManagerService.lambda$postSuccess$1(fd, callback, packageName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$postSuccess$1(ParcelFileDescriptor fd, ISnapshotRuntimeProfileCallback callback, String packageName) {
        try {
            try {
                if (!fd.getFileDescriptor().valid()) {
                    Slog.wtf(TAG, "The snapshot FD became invalid before posting the result for " + packageName);
                    callback.onError(2);
                } else {
                    callback.onSuccess(fd);
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to call onSuccess after profile snapshot for " + packageName, e);
            }
        } finally {
            IoUtils.closeQuietly(fd);
        }
    }

    public void prepareAppProfiles(AndroidPackage pkg, int user, boolean updateReferenceProfileContent) {
        String dexMetadataPath;
        int appId = UserHandle.getAppId(pkg.getUid());
        if (user < 0) {
            Slog.wtf(TAG, "Invalid user id: " + user);
        } else if (appId < 0) {
            Slog.wtf(TAG, "Invalid app id: " + appId);
        } else {
            try {
                ArrayMap<String, String> codePathsProfileNames = getPackageProfileNames(pkg);
                for (int i = codePathsProfileNames.size() - 1; i >= 0; i--) {
                    String codePath = codePathsProfileNames.keyAt(i);
                    String profileName = codePathsProfileNames.valueAt(i);
                    if (!updateReferenceProfileContent) {
                        dexMetadataPath = null;
                    } else {
                        File dexMetadata = DexMetadataHelper.findDexMetadataForFile(new File(codePath));
                        String dexMetadataPath2 = dexMetadata == null ? null : dexMetadata.getAbsolutePath();
                        dexMetadataPath = dexMetadataPath2;
                    }
                    synchronized (this.mInstaller) {
                        boolean result = this.mInstaller.prepareAppProfile(pkg.getPackageName(), user, appId, profileName, codePath, dexMetadataPath);
                        if (!result) {
                            Slog.e(TAG, "Failed to prepare profile for " + pkg.getPackageName() + ":" + codePath);
                        }
                    }
                }
            } catch (Installer.InstallerException e) {
                Slog.e(TAG, "Failed to prepare profile for " + pkg.getPackageName(), e);
            }
        }
    }

    public void prepareAppProfiles(AndroidPackage pkg, int[] user, boolean updateReferenceProfileContent) {
        for (int i : user) {
            prepareAppProfiles(pkg, i, updateReferenceProfileContent);
        }
    }

    public void clearAppProfiles(AndroidPackage pkg) {
        try {
            ArrayMap<String, String> packageProfileNames = getPackageProfileNames(pkg);
            for (int i = packageProfileNames.size() - 1; i >= 0; i--) {
                String profileName = packageProfileNames.valueAt(i);
                this.mInstaller.clearAppProfiles(pkg.getPackageName(), profileName);
            }
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, String.valueOf(e));
        }
    }

    public void dumpProfiles(AndroidPackage pkg, boolean dumpClassesAndMethods) {
        int sharedGid = UserHandle.getSharedAppGid(pkg.getUid());
        try {
            ArrayMap<String, String> packageProfileNames = getPackageProfileNames(pkg);
            for (int i = packageProfileNames.size() - 1; i >= 0; i--) {
                String codePath = packageProfileNames.keyAt(i);
                String profileName = packageProfileNames.valueAt(i);
                this.mInstaller.dumpProfiles(sharedGid, pkg.getPackageName(), profileName, codePath, dumpClassesAndMethods);
            }
        } catch (Installer.InstallerException e) {
            Slog.w(TAG, "Failed to dump profiles", e);
        }
    }

    public boolean compileLayouts(AndroidPackage pkg) {
        try {
            String packageName = pkg.getPackageName();
            String apkPath = pkg.getBaseApkPath();
            File dataDir = PackageInfoWithoutStateUtils.getDataDir(pkg, UserHandle.myUserId());
            String outDexFile = dataDir.getAbsolutePath() + "/code_cache/compiled_view.dex";
            if (!pkg.isPrivileged() && !pkg.isUseEmbeddedDex() && !pkg.isDefaultToDeviceProtectedStorage()) {
                Log.i("PackageManager", "Compiling layouts in " + packageName + " (" + apkPath + ") to " + outDexFile);
                long callingId = Binder.clearCallingIdentity();
                boolean compileLayouts = this.mInstaller.compileLayouts(apkPath, packageName, outDexFile, pkg.getUid());
                Binder.restoreCallingIdentity(callingId);
                return compileLayouts;
            }
            return false;
        } catch (Throwable e) {
            Log.e("PackageManager", "Failed to compile layouts", e);
            return false;
        }
    }

    private ArrayMap<String, String> getPackageProfileNames(AndroidPackage pkg) {
        ArrayMap<String, String> result = new ArrayMap<>();
        if (pkg.isHasCode()) {
            result.put(pkg.getBaseApkPath(), ArtManager.getProfileName((String) null));
        }
        String[] splitCodePaths = pkg.getSplitCodePaths();
        int[] splitFlags = pkg.getSplitFlags();
        String[] splitNames = pkg.getSplitNames();
        if (!ArrayUtils.isEmpty(splitCodePaths)) {
            for (int i = 0; i < splitCodePaths.length; i++) {
                if ((splitFlags[i] & 4) != 0) {
                    result.put(splitCodePaths[i], ArtManager.getProfileName(splitNames[i]));
                }
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static int getCompilationReasonTronValue(String compilationReason) {
        char c;
        switch (compilationReason.hashCode()) {
            case -1968171580:
                if (compilationReason.equals("bg-dexopt")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -1836520088:
                if (compilationReason.equals("install-fast-dm")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case -1425983632:
                if (compilationReason.equals("ab-ota")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -1291894341:
                if (compilationReason.equals("prebuilt")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -1125526357:
                if (compilationReason.equals("install-bulk-secondary-dm")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case -903566235:
                if (compilationReason.equals("shared")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -587828592:
                if (compilationReason.equals("boot-after-ota")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -525717262:
                if (compilationReason.equals("install-bulk-dm")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case -207505425:
                if (compilationReason.equals("first-boot")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 3614689:
                if (compilationReason.equals("vdex")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 17118443:
                if (compilationReason.equals("install-bulk-secondary")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case 24665195:
                if (compilationReason.equals("inactive")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 96784904:
                if (compilationReason.equals("error")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 884802606:
                if (compilationReason.equals("cmdline")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 900392443:
                if (compilationReason.equals("install-dm")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case 1558537393:
                if (compilationReason.equals("install-bulk-secondary-downgraded")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case 1756645502:
                if (compilationReason.equals("install-bulk-downgraded-dm")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case 1791051557:
                if (compilationReason.equals("install-bulk-secondary-downgraded-dm")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case 1956259839:
                if (compilationReason.equals("post-boot")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1957569947:
                if (compilationReason.equals("install")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 1988662788:
                if (compilationReason.equals("install-bulk")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case 1988762958:
                if (compilationReason.equals("install-fast")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 2005174776:
                if (compilationReason.equals("install-bulk-downgraded")) {
                    c = 15;
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
                return 22;
            case 1:
                return 0;
            case 2:
                return 2;
            case 3:
                return 20;
            case 4:
                return 21;
            case 5:
                return 4;
            case 6:
                return 5;
            case 7:
                return 6;
            case '\b':
                return 7;
            case '\t':
                return 8;
            case '\n':
                return 23;
            case 11:
                return 24;
            case '\f':
                return 10;
            case '\r':
                return 11;
            case 14:
                return 12;
            case 15:
                return 13;
            case 16:
                return 14;
            case 17:
                return 9;
            case 18:
                return 15;
            case 19:
                return 16;
            case 20:
                return 17;
            case 21:
                return 18;
            case 22:
                return 19;
            default:
                return 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static int getCompilationFilterTronValue(String compilationFilter) {
        char c;
        switch (compilationFilter.hashCode()) {
            case -2111392495:
                if (compilationFilter.equals("speed-profile-iorap")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case -1957514039:
                if (compilationFilter.equals("assume-verified")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1803365233:
                if (compilationFilter.equals("everything-profile")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -1707970841:
                if (compilationFilter.equals("verify-iorap")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case -1704485649:
                if (compilationFilter.equals("extract-iorap")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case -1305289599:
                if (compilationFilter.equals("extract")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -1129892317:
                if (compilationFilter.equals("speed-profile")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -1079751646:
                if (compilationFilter.equals("run-from-apk-fallback-iorap")) {
                    c = 26;
                    break;
                }
                c = 65535;
                break;
            case -902315795:
                if (compilationFilter.equals("run-from-vdex-fallback")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case -819951495:
                if (compilationFilter.equals("verify")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -701043824:
                if (compilationFilter.equals("space-profile-iorap")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case -284840886:
                if (compilationFilter.equals(UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -44924837:
                if (compilationFilter.equals("run-from-vdex-fallback-iorap")) {
                    c = 27;
                    break;
                }
                c = 65535;
                break;
            case 50732855:
                if (compilationFilter.equals("assume-verified-iorap")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 96784904:
                if (compilationFilter.equals("error")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 109637894:
                if (compilationFilter.equals("space")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 109641799:
                if (compilationFilter.equals("speed")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 256996201:
                if (compilationFilter.equals("run-from-apk-iorap")) {
                    c = 25;
                    break;
                }
                c = 65535;
                break;
            case 348518370:
                if (compilationFilter.equals("space-profile")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 401590963:
                if (compilationFilter.equals("everything")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 590454177:
                if (compilationFilter.equals("everything-iorap")) {
                    c = 24;
                    break;
                }
                c = 65535;
                break;
            case 658336598:
                if (compilationFilter.equals("quicken")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 863294077:
                if (compilationFilter.equals("everything-profile-iorap")) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case 922064507:
                if (compilationFilter.equals("run-from-apk")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 979981365:
                if (compilationFilter.equals("speed-iorap")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case 1316714932:
                if (compilationFilter.equals("space-iorap")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case 1482618884:
                if (compilationFilter.equals("quicken-iorap")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case 1906552308:
                if (compilationFilter.equals("run-from-apk-fallback")) {
                    c = '\r';
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
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            case '\b':
                return 8;
            case '\t':
                return 9;
            case '\n':
                return 10;
            case 11:
                return 11;
            case '\f':
                return 12;
            case '\r':
                return 13;
            case 14:
                return 14;
            case 15:
                return 15;
            case 16:
                return 16;
            case 17:
                return 17;
            case 18:
                return 18;
            case 19:
                return 19;
            case 20:
                return 20;
            case 21:
                return 21;
            case 22:
                return 22;
            case 23:
                return 23;
            case 24:
                return 24;
            case 25:
                return 25;
            case 26:
                return 26;
            case 27:
                return 27;
            default:
                return 1;
        }
    }

    private static void verifyTronLoggingConstants() {
        for (int i = 0; i < PackageManagerServiceCompilerMapping.REASON_STRINGS.length; i++) {
            String reason = PackageManagerServiceCompilerMapping.REASON_STRINGS[i];
            int value = getCompilationReasonTronValue(reason);
            if (value == 0 || value == 1) {
                throw new IllegalArgumentException("Compilation reason not configured for TRON logging: " + reason);
            }
        }
    }

    /* loaded from: classes2.dex */
    private class ArtManagerInternalImpl extends ArtManagerInternal {
        private static final String IORAP_DIR = "/data/misc/iorapd";
        private static final String TAG = "ArtManagerInternalImpl";

        private ArtManagerInternalImpl() {
        }

        public PackageOptimizationInfo getPackageOptimizationInfo(ApplicationInfo info, String abi, String activityName) {
            String compilationFilter;
            String compilationReason;
            try {
                String isa = VMRuntime.getInstructionSet(abi);
                DexFile.OptimizationInfo optInfo = DexFile.getDexFileOptimizationInfo(info.getBaseCodePath(), isa);
                compilationFilter = optInfo.getStatus();
                compilationReason = optInfo.getReason();
            } catch (FileNotFoundException e) {
                Slog.e(TAG, "Could not get optimizations status for " + info.getBaseCodePath(), e);
                compilationFilter = "error";
                compilationReason = "error";
            } catch (IllegalArgumentException e2) {
                Slog.wtf(TAG, "Requested optimization status for " + info.getBaseCodePath() + " due to an invalid abi " + abi, e2);
                compilationFilter = "error";
                compilationReason = "error";
            }
            if (checkIorapCompiledTrace(info.packageName, activityName, info.longVersionCode)) {
                compilationFilter = compilationFilter + "-iorap";
            }
            int compilationFilterTronValue = ArtManagerService.getCompilationFilterTronValue(compilationFilter);
            int compilationReasonTronValue = ArtManagerService.getCompilationReasonTronValue(compilationReason);
            return new PackageOptimizationInfo(compilationFilterTronValue, compilationReasonTronValue);
        }

        private boolean checkIorapCompiledTrace(String packageName, String activityName, long version) {
            Path tracePath = Paths.get(IORAP_DIR, packageName, Long.toString(version), activityName, "compiled_traces", "compiled_trace.pb");
            try {
                boolean exists = Files.exists(tracePath, new LinkOption[0]);
                if (ArtManagerService.DEBUG) {
                    Log.d(TAG, tracePath.toString() + (exists ? " exists" : " doesn't exist"));
                }
                if (exists) {
                    long bytes = Files.size(tracePath);
                    if (ArtManagerService.DEBUG) {
                        Log.d(TAG, tracePath.toString() + " size is " + Long.toString(bytes));
                    }
                    return bytes > 0;
                }
                return exists;
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
                return false;
            }
        }
    }
}
