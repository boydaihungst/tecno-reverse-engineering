package com.android.server.pm;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageInstaller;
import android.content.pm.PackagePartitions;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.PackageLite;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.FileUtils;
import android.os.SystemProperties;
import android.os.incremental.IncrementalManager;
import android.os.incremental.IncrementalStorage;
import android.os.incremental.V4Signature;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Base64;
import android.util.LogPrinter;
import android.util.Printer;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.content.InstallLocationUtils;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.HexDump;
import com.android.server.EventLogTags;
import com.android.server.IntentResolver;
import com.android.server.Watchdog;
import com.android.server.am.HostingRecord;
import com.android.server.compat.PlatformCompat;
import com.android.server.pm.dex.PackageDexUsage;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.component.ParsedIntentInfo;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.resolution.ComponentResolverApi;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import dalvik.system.VMRuntime;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import libcore.io.IoUtils;
/* loaded from: classes2.dex */
public class PackageManagerServiceUtils {
    private static final boolean DEFAULT_PACKAGE_PARSER_CACHE_ENABLED = true;
    private static final long ENFORCE_INTENTS_TO_MATCH_INTENT_FILTERS = 161252188;
    private static final boolean FORCE_PACKAGE_PARSED_CACHE_ENABLED = false;
    private static final int FSVERITY_DISABLED = 0;
    private static final int FSVERITY_ENABLED = 2;
    private static final long MAX_CRITICAL_INFO_DUMP_SIZE = 3000000;
    private static final boolean DEBUG = Build.IS_DEBUGGABLE;
    public static final Predicate<PackageStateInternal> REMOVE_IF_NULL_PKG = new Predicate() { // from class: com.android.server.pm.PackageManagerServiceUtils$$ExternalSyntheticLambda2
        @Override // java.util.function.Predicate
        public final boolean test(Object obj) {
            return PackageManagerServiceUtils.lambda$static$0((PackageStateInternal) obj);
        }
    };
    static int sCustomResolverUid = -1;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$static$0(PackageStateInternal pkgSetting) {
        return pkgSetting.getPkg() == null;
    }

    public static boolean isUnusedSinceTimeInMillis(long firstInstallTime, long currentTimeInMillis, long thresholdTimeinMillis, PackageDexUsage.PackageUseInfo packageUseInfo, long latestPackageUseTimeInMillis, long latestForegroundPackageUseTimeInMillis) {
        if (currentTimeInMillis - firstInstallTime < thresholdTimeinMillis) {
            return false;
        }
        boolean isActiveInForeground = currentTimeInMillis - latestForegroundPackageUseTimeInMillis < thresholdTimeinMillis;
        if (isActiveInForeground) {
            return false;
        }
        boolean isActiveInBackgroundAndUsedByOtherPackages = currentTimeInMillis - latestPackageUseTimeInMillis < thresholdTimeinMillis && packageUseInfo.isAnyCodePathUsedByOtherApps();
        return !isActiveInBackgroundAndUsedByOtherPackages;
    }

    public static String realpath(File path) throws IOException {
        try {
            return Os.realpath(path.getAbsolutePath());
        } catch (ErrnoException ee) {
            throw ee.rethrowAsIOException();
        }
    }

    public static boolean checkISA(String isa) {
        String[] strArr;
        for (String abi : Build.SUPPORTED_ABIS) {
            if (VMRuntime.getInstructionSet(abi).equals(isa)) {
                return true;
            }
        }
        return false;
    }

    public static long getLastModifiedTime(AndroidPackage pkg) {
        if (Build.IS_DEBUG_ENABLE && pkg != null) {
            try {
                if (pkg.getPath() == null) {
                    Slog.i("PackageManager", "Scanning base APK: " + pkg.toString() + ",path =" + pkg.getPath() + ",base path=" + pkg.getBaseApkPath());
                }
            } catch (Exception e) {
                Slog.e("PackageManager", "a error has happened :" + e.toString());
            }
        }
        File srcFile = new File(pkg.getPath());
        if (!srcFile.isDirectory()) {
            return srcFile.lastModified();
        }
        File baseFile = new File(pkg.getBaseApkPath());
        long maxModifiedTime = baseFile.lastModified();
        for (int i = pkg.getSplitCodePaths().length - 1; i >= 0; i--) {
            File splitFile = new File(pkg.getSplitCodePaths()[i]);
            maxModifiedTime = Math.max(maxModifiedTime, splitFile.lastModified());
        }
        return maxModifiedTime;
    }

    private static File getSettingsProblemFile() {
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, HostingRecord.HOSTING_TYPE_SYSTEM);
        File fname = new File(systemDir, "uiderrors.txt");
        return fname;
    }

    public static void dumpCriticalInfo(ProtoOutputStream proto) {
        File file = getSettingsProblemFile();
        long skipSize = file.length() - MAX_CRITICAL_INFO_DUMP_SIZE;
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            if (skipSize > 0) {
                in.skip(skipSize);
            }
            while (true) {
                String line = in.readLine();
                if (line != null) {
                    if (!line.contains("ignored: updated version")) {
                        proto.write(2237677961223L, line);
                    }
                } else {
                    in.close();
                    return;
                }
            }
        } catch (IOException e) {
        }
    }

    public static void dumpCriticalInfo(PrintWriter pw, String msg) {
        File file = getSettingsProblemFile();
        long skipSize = file.length() - MAX_CRITICAL_INFO_DUMP_SIZE;
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            if (skipSize > 0) {
                in.skip(skipSize);
            }
            while (true) {
                String line = in.readLine();
                if (line != null) {
                    if (!line.contains("ignored: updated version")) {
                        if (msg != null) {
                            pw.print(msg);
                        }
                        pw.println(line);
                    }
                } else {
                    in.close();
                    return;
                }
            }
        } catch (IOException e) {
        }
    }

    public static void logCriticalInfo(int priority, String msg) {
        Slog.println(priority, "PackageManager", msg);
        EventLogTags.writePmCriticalInfo(msg);
        try {
            File fname = getSettingsProblemFile();
            FileOutputStream out = new FileOutputStream(fname, true);
            FastPrintWriter fastPrintWriter = new FastPrintWriter(out);
            SimpleDateFormat formatter = new SimpleDateFormat();
            String dateString = formatter.format(new Date(System.currentTimeMillis()));
            fastPrintWriter.println(dateString + ": " + msg);
            fastPrintWriter.close();
            FileUtils.setPermissions(fname.toString(), 508, -1, -1);
        } catch (IOException e) {
        }
    }

    public static void enforceShellRestriction(UserManagerInternal userManager, String restriction, int callingUid, int userHandle) {
        if (callingUid == 2000) {
            if (userHandle >= 0 && userManager.hasUserRestriction(restriction, userHandle)) {
                throw new SecurityException("Shell does not have permission to access user " + userHandle);
            }
            if (userHandle < 0) {
                Slog.e("PackageManager", "Unable to check shell permission for user " + userHandle + "\n\t" + Debug.getCallers(3));
            }
        }
    }

    public static void enforceSystemOrPhoneCaller(String methodName, int callingUid) {
        if (callingUid != 1001 && callingUid != 1000) {
            throw new SecurityException("Cannot call " + methodName + " from UID " + callingUid);
        }
    }

    public static String deriveAbiOverride(String abiOverride) {
        if ("-".equals(abiOverride)) {
            return null;
        }
        return abiOverride;
    }

    public static int compareSignatures(Signature[] s1, Signature[] s2) {
        if (s1 == null) {
            if (s2 == null) {
                return 1;
            }
            return -1;
        } else if (s2 == null) {
            return -2;
        } else {
            if (s1.length != s2.length) {
                return -3;
            }
            if (s1.length == 1) {
                return s1[0].equals(s2[0]) ? 0 : -3;
            }
            ArraySet<Signature> set1 = new ArraySet<>();
            for (Signature sig : s1) {
                set1.add(sig);
            }
            ArraySet<Signature> set2 = new ArraySet<>();
            for (Signature sig2 : s2) {
                set2.add(sig2);
            }
            return set1.equals(set2) ? 0 : -3;
        }
    }

    public static boolean comparePackageSignatures(PackageSetting pkgSetting, Signature[] signatures) {
        SigningDetails signingDetails = pkgSetting.getSigningDetails();
        return signingDetails == SigningDetails.UNKNOWN || compareSignatures(signingDetails.getSignatures(), signatures) == 0;
    }

    private static boolean matchSignaturesCompat(String packageName, PackageSignatures packageSignatures, SigningDetails parsedSignatures) {
        Signature[] signatures;
        ArraySet<Signature> existingSet = new ArraySet<>();
        for (Signature sig : packageSignatures.mSigningDetails.getSignatures()) {
            existingSet.add(sig);
        }
        ArraySet<Signature> scannedCompatSet = new ArraySet<>();
        for (Signature sig2 : parsedSignatures.getSignatures()) {
            try {
                Signature[] chainSignatures = sig2.getChainSignatures();
                for (Signature chainSig : chainSignatures) {
                    scannedCompatSet.add(chainSig);
                }
            } catch (CertificateEncodingException e) {
                scannedCompatSet.add(sig2);
            }
        }
        if (scannedCompatSet.equals(existingSet)) {
            packageSignatures.mSigningDetails = parsedSignatures;
            return true;
        }
        if (parsedSignatures.hasPastSigningCertificates()) {
            logCriticalInfo(4, "Existing package " + packageName + " has flattened signing certificate chain. Unable to install newer version with rotated signing certificate.");
        }
        return false;
    }

    private static boolean matchSignaturesRecover(String packageName, SigningDetails existingSignatures, SigningDetails parsedSignatures, int flags) {
        String msg = null;
        try {
            if (parsedSignatures.checkCapabilityRecover(existingSignatures, flags)) {
                logCriticalInfo(4, "Recovered effectively matching certificates for " + packageName);
                return true;
            }
        } catch (CertificateException e) {
            msg = e.getMessage();
        }
        logCriticalInfo(4, "Failed to recover certificates for " + packageName + ": " + msg);
        return false;
    }

    private static boolean matchSignatureInSystem(String packageName, SigningDetails signingDetails, PackageSetting disabledPkgSetting) {
        if (signingDetails.checkCapability(disabledPkgSetting.getSigningDetails(), 1) || disabledPkgSetting.getSigningDetails().checkCapability(signingDetails, 8)) {
            return true;
        }
        logCriticalInfo(6, "Updated system app mismatches cert on /system: " + packageName);
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isApkVerityEnabled() {
        return Build.VERSION.DEVICE_INITIAL_SDK_INT >= 30 || SystemProperties.getInt("ro.apk_verity.mode", 0) == 2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isApkVerificationForced(PackageSetting ps) {
        return false;
    }

    public static boolean verifySignatures(PackageSetting pkgSetting, SharedUserSetting sharedUserSetting, PackageSetting disabledPkgSetting, SigningDetails parsedSignatures, boolean compareCompat, boolean compareRecover, boolean isRollback) throws PackageManagerException {
        String packageName = pkgSetting.getPackageName();
        boolean compatMatch = false;
        boolean z = false;
        if (pkgSetting.getSigningDetails().getSignatures() != null) {
            boolean match = parsedSignatures.checkCapability(pkgSetting.getSigningDetails(), 1) || pkgSetting.getSigningDetails().checkCapability(parsedSignatures, 8);
            if (!match && compareCompat) {
                match = matchSignaturesCompat(packageName, pkgSetting.getSignatures(), parsedSignatures);
                compatMatch = match;
            }
            if (!match && compareRecover) {
                match = matchSignaturesRecover(packageName, pkgSetting.getSigningDetails(), parsedSignatures, 1) || matchSignaturesRecover(packageName, parsedSignatures, pkgSetting.getSigningDetails(), 8);
            }
            if (!match && isApkVerificationForced(disabledPkgSetting)) {
                match = matchSignatureInSystem(packageName, pkgSetting.getSigningDetails(), disabledPkgSetting);
            }
            if (!match && isRollback) {
                match = pkgSetting.getSigningDetails().hasAncestorOrSelf(parsedSignatures);
            }
            if (!match) {
                throw new PackageManagerException(-7, "Existing package " + packageName + " signatures do not match newer version; ignoring!");
            }
        }
        if (sharedUserSetting != null && sharedUserSetting.getSigningDetails() != SigningDetails.UNKNOWN) {
            boolean match2 = canJoinSharedUserId(parsedSignatures, sharedUserSetting.getSigningDetails());
            ArraySet<? extends PackageStateInternal> packageStates = sharedUserSetting.getPackageStates();
            if (!match2 && packageStates.size() == 1 && packageStates.valueAt(0).getPackageName().equals(packageName)) {
                match2 = true;
            }
            if (!match2 && compareCompat) {
                match2 = matchSignaturesCompat(packageName, sharedUserSetting.signatures, parsedSignatures);
            }
            if (!match2 && compareRecover) {
                match2 = (matchSignaturesRecover(packageName, sharedUserSetting.signatures.mSigningDetails, parsedSignatures, 2) || matchSignaturesRecover(packageName, parsedSignatures, sharedUserSetting.signatures.mSigningDetails, 2)) ? true : true;
                compatMatch |= match2;
            }
            if (!match2) {
                throw new PackageManagerException(-8, "Package " + packageName + " has no signatures that match those in shared user " + sharedUserSetting.name + "; ignoring!");
            }
            if (parsedSignatures.hasPastSigningCertificates()) {
                for (int i = 0; i < packageStates.size(); i++) {
                    PackageStateInternal shUidPkgSetting = packageStates.valueAt(i);
                    if (!packageName.equals(shUidPkgSetting.getPackageName())) {
                        SigningDetails shUidSigningDetails = shUidPkgSetting.getSigningDetails();
                        if (parsedSignatures.hasAncestor(shUidSigningDetails) && !parsedSignatures.checkCapability(shUidSigningDetails, 2)) {
                            throw new PackageManagerException(-8, "Package " + packageName + " revoked the sharedUserId capability from the signing key used to sign " + shUidPkgSetting.getPackageName());
                        }
                    }
                }
            }
            if (!parsedSignatures.hasCommonAncestor(sharedUserSetting.signatures.mSigningDetails)) {
                throw new PackageManagerException(-8, "Package " + packageName + " has a signing lineage that diverges from the lineage of the sharedUserId");
            }
        }
        return compatMatch;
    }

    public static boolean canJoinSharedUserId(SigningDetails packageSigningDetails, SigningDetails sharedUserSigningDetails) {
        return packageSigningDetails.checkCapability(sharedUserSigningDetails, 2) || sharedUserSigningDetails.checkCapability(packageSigningDetails, 2);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [692=4] */
    public static int extractNativeBinaries(File dstCodePath, String packageName) {
        File libraryRoot = new File(dstCodePath, "lib");
        NativeLibraryHelper.Handle handle = null;
        try {
            handle = NativeLibraryHelper.Handle.create(dstCodePath);
            return NativeLibraryHelper.copyNativeBinariesWithOverride(handle, libraryRoot, (String) null, false);
        } catch (IOException e) {
            logCriticalInfo(6, "Failed to extract native libraries; pkg: " + packageName);
            return RequestStatus.SYS_ETIMEDOUT;
        } finally {
            IoUtils.closeQuietly(handle);
        }
    }

    public static void removeNativeBinariesLI(PackageSetting ps) {
        if (ps != null) {
            NativeLibraryHelper.removeNativeBinariesLI(ps.getLegacyNativeLibraryPath());
        }
    }

    public static void waitForNativeBinariesExtractionForIncremental(ArraySet<IncrementalStorage> incrementalStorages) {
        if (!incrementalStorages.isEmpty()) {
            try {
                Watchdog.getInstance().pauseWatchingCurrentThread("native_lib_extract");
                for (int i = 0; i < incrementalStorages.size(); i++) {
                    IncrementalStorage storage = (IncrementalStorage) incrementalStorages.valueAtUnchecked(i);
                    storage.waitForNativeBinariesExtraction();
                }
            } finally {
                Watchdog.getInstance().resumeWatchingCurrentThread("native_lib_extract");
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:7:0x0035, code lost:
        logCriticalInfo(6, "Failed to decompress; pkg: " + r14 + ", file: " + r9);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public static int decompressFiles(String codePath, File dstCodePath, String packageName) {
        File[] compressedFiles = getCompressedFiles(codePath);
        int ret = 1;
        try {
            makeDirRecursive(dstCodePath, 493);
            int length = compressedFiles.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                File srcFile = compressedFiles[i];
                String srcFileName = srcFile.getName();
                String dstFileName = srcFileName.substring(0, srcFileName.length() - PackageManagerService.COMPRESSED_EXTENSION.length());
                File dstFile = new File(dstCodePath, dstFileName);
                ret = decompressFile(srcFile, dstFile);
                if (ret != 1) {
                    break;
                }
                i++;
            }
        } catch (ErrnoException e) {
            logCriticalInfo(6, "Failed to decompress; pkg: " + packageName + ", err: " + e.errno);
        }
        return ret;
    }

    public static int decompressFile(File srcFile, File dstFile) throws ErrnoException {
        if (PackageManagerService.DEBUG_COMPRESSION) {
            Slog.i("PackageManager", "Decompress file; src: " + srcFile.getAbsolutePath() + ", dst: " + dstFile.getAbsolutePath());
        }
        AtomicFile atomicFile = new AtomicFile(dstFile);
        FileOutputStream outputStream = null;
        try {
            InputStream fileIn = new GZIPInputStream(new FileInputStream(srcFile));
            outputStream = atomicFile.startWrite();
            FileUtils.copy(fileIn, outputStream);
            outputStream.flush();
            Os.fchmod(outputStream.getFD(), FrameworkStatsLog.VBMETA_DIGEST_REPORTED);
            atomicFile.finishWrite(outputStream);
            fileIn.close();
            return 1;
        } catch (IOException e) {
            logCriticalInfo(6, "Failed to decompress file; src: " + srcFile.getAbsolutePath() + ", dst: " + dstFile.getAbsolutePath());
            atomicFile.failWrite(outputStream);
            return RequestStatus.SYS_ETIMEDOUT;
        }
    }

    public static File[] getCompressedFiles(String codePath) {
        File stubCodePath = new File(codePath);
        String stubName = stubCodePath.getName();
        int idx = stubName.lastIndexOf(PackageManagerService.STUB_SUFFIX);
        if (idx < 0 || stubName.length() != PackageManagerService.STUB_SUFFIX.length() + idx) {
            return null;
        }
        File stubParentDir = stubCodePath.getParentFile();
        if (stubParentDir == null) {
            Slog.e("PackageManager", "Unable to determine stub parent dir for codePath: " + codePath);
            return null;
        }
        File compressedPath = new File(stubParentDir, stubName.substring(0, idx));
        File[] files = compressedPath.listFiles(new FilenameFilter() { // from class: com.android.server.pm.PackageManagerServiceUtils.1
            @Override // java.io.FilenameFilter
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(PackageManagerService.COMPRESSED_EXTENSION);
            }
        });
        if (PackageManagerService.DEBUG_COMPRESSION && files != null && files.length > 0) {
            Slog.i("PackageManager", "getCompressedFiles[" + codePath + "]: " + Arrays.toString(files));
        }
        return files;
    }

    public static boolean compressedFileExists(String codePath) {
        File[] compressedFiles = getCompressedFiles(codePath);
        return compressedFiles != null && compressedFiles.length > 0;
    }

    public static PackageInfoLite getMinimalPackageInfo(Context context, PackageLite pkg, String packagePath, int flags, String abiOverride) {
        PackageInfoLite ret = new PackageInfoLite();
        if (packagePath == null || pkg == null) {
            Slog.i("PackageManager", "Invalid package file " + packagePath);
            ret.recommendedInstallLocation = -2;
            return ret;
        }
        File packageFile = new File(packagePath);
        try {
            long sizeBytes = InstallLocationUtils.calculateInstalledSize(pkg, abiOverride);
            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(-1);
            sessionParams.appPackageName = pkg.getPackageName();
            sessionParams.installLocation = pkg.getInstallLocation();
            sessionParams.sizeBytes = sizeBytes;
            sessionParams.installFlags = flags;
            try {
                int recommendedInstallLocation = InstallLocationUtils.resolveInstallLocation(context, sessionParams);
                ret.packageName = pkg.getPackageName();
                ret.splitNames = pkg.getSplitNames();
                ret.versionCode = pkg.getVersionCode();
                ret.versionCodeMajor = pkg.getVersionCodeMajor();
                ret.baseRevisionCode = pkg.getBaseRevisionCode();
                ret.splitRevisionCodes = pkg.getSplitRevisionCodes();
                ret.installLocation = pkg.getInstallLocation();
                ret.verifiers = pkg.getVerifiers();
                ret.recommendedInstallLocation = recommendedInstallLocation;
                ret.multiArch = pkg.isMultiArch();
                ret.debuggable = pkg.isDebuggable();
                ret.isSdkLibrary = pkg.isIsSdkLibrary();
                return ret;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } catch (IOException e2) {
            if (!packageFile.exists()) {
                ret.recommendedInstallLocation = -6;
            } else {
                ret.recommendedInstallLocation = -2;
            }
            return ret;
        }
    }

    public static long calculateInstalledSize(String packagePath, String abiOverride) {
        File packageFile = new File(packagePath);
        try {
            ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
            ParseResult<PackageLite> result = ApkLiteParseUtils.parsePackageLite(input.reset(), packageFile, 0);
            if (result.isError()) {
                throw new PackageManagerException(result.getErrorCode(), result.getErrorMessage(), result.getException());
            }
            return InstallLocationUtils.calculateInstalledSize((PackageLite) result.getResult(), abiOverride);
        } catch (PackageManagerException | IOException e) {
            Slog.w("PackageManager", "Failed to calculate installed size: " + e);
            return -1L;
        }
    }

    public static boolean isDowngradePermitted(int installFlags, boolean isAppDebuggable) {
        boolean downgradeRequested = (installFlags & 128) != 0;
        if (downgradeRequested) {
            boolean isDebuggable = Build.IS_DEBUGGABLE || isAppDebuggable;
            return isDebuggable || (1048576 & installFlags) != 0;
        }
        return false;
    }

    public static int copyPackage(String packagePath, File targetDir) {
        if (packagePath == null) {
            return -3;
        }
        try {
            File packageFile = new File(packagePath);
            ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
            ParseResult<PackageLite> result = ApkLiteParseUtils.parsePackageLite(input.reset(), packageFile, 0);
            if (result.isError()) {
                Slog.w("PackageManager", "Failed to parse package at " + packagePath);
                return result.getErrorCode();
            }
            PackageLite pkg = (PackageLite) result.getResult();
            copyFile(pkg.getBaseApkPath(), targetDir, "base.apk");
            if (!ArrayUtils.isEmpty(pkg.getSplitNames())) {
                for (int i = 0; i < pkg.getSplitNames().length; i++) {
                    copyFile(pkg.getSplitApkPaths()[i], targetDir, "split_" + pkg.getSplitNames()[i] + ".apk");
                }
                return 1;
            }
            return 1;
        } catch (ErrnoException | IOException e) {
            Slog.w("PackageManager", "Failed to copy package at " + packagePath + ": " + e);
            return -4;
        }
    }

    private static void copyFile(String sourcePath, File targetDir, String targetName) throws ErrnoException, IOException {
        if (!FileUtils.isValidExtFilename(targetName)) {
            throw new IllegalArgumentException("Invalid filename: " + targetName);
        }
        Slog.d("PackageManager", "Copying " + sourcePath + " to " + targetName);
        File targetFile = new File(targetDir, targetName);
        FileDescriptor targetFd = Os.open(targetFile.getAbsolutePath(), OsConstants.O_RDWR | OsConstants.O_CREAT, FrameworkStatsLog.VBMETA_DIGEST_REPORTED);
        Os.chmod(targetFile.getAbsolutePath(), FrameworkStatsLog.VBMETA_DIGEST_REPORTED);
        FileInputStream source = null;
        try {
            source = new FileInputStream(sourcePath);
            FileUtils.copy(source.getFD(), targetFd);
        } finally {
            IoUtils.closeQuietly(source);
        }
    }

    public static void makeDirRecursive(File targetDir, int mode) throws ErrnoException {
        Path targetDirPath = targetDir.toPath();
        int directoriesCount = targetDirPath.getNameCount();
        for (int i = 1; i <= directoriesCount; i++) {
            File currentDir = targetDirPath.subpath(0, i).toFile();
            if (!currentDir.exists()) {
                Os.mkdir(currentDir.getAbsolutePath(), mode);
                Os.chmod(currentDir.getAbsolutePath(), mode);
            }
        }
    }

    public static String buildVerificationRootHashString(String baseFilename, String[] splitFilenameArray) {
        StringBuilder sb = new StringBuilder();
        String baseFilePath = baseFilename.substring(baseFilename.lastIndexOf(File.separator) + 1);
        sb.append(baseFilePath).append(":");
        byte[] baseRootHash = getRootHash(baseFilename);
        if (baseRootHash == null) {
            sb.append("0");
        } else {
            sb.append(HexDump.toHexString(baseRootHash));
        }
        if (splitFilenameArray == null || splitFilenameArray.length == 0) {
            return sb.toString();
        }
        for (int i = splitFilenameArray.length - 1; i >= 0; i--) {
            String splitFilename = splitFilenameArray[i];
            String splitFilePath = splitFilename.substring(splitFilename.lastIndexOf(File.separator) + 1);
            byte[] splitRootHash = getRootHash(splitFilename);
            sb.append(";").append(splitFilePath).append(":");
            if (splitRootHash == null) {
                sb.append("0");
            } else {
                sb.append(HexDump.toHexString(splitRootHash));
            }
        }
        return sb.toString();
    }

    private static byte[] getRootHash(String filename) {
        try {
            byte[] baseFileSignature = IncrementalManager.unsafeGetFileSignature(filename);
            if (baseFileSignature == null) {
                throw new IOException("File signature not present");
            }
            V4Signature signature = V4Signature.readFrom(baseFileSignature);
            if (signature.hashingInfo == null) {
                throw new IOException("Hashing info not present");
            }
            V4Signature.HashingInfo hashInfo = V4Signature.HashingInfo.fromByteArray(signature.hashingInfo);
            if (ArrayUtils.isEmpty(hashInfo.rawRootHash)) {
                throw new IOException("Root has not present");
            }
            return ApkChecksums.verityHashForFile(new File(filename), hashInfo.rawRootHash);
        } catch (IOException e) {
            Slog.e("PackageManager", "ERROR: could not load root hash from incremental install");
            return null;
        }
    }

    public static boolean isSystemApp(PackageSetting ps) {
        return (ps.getFlags() & 1) != 0;
    }

    public static boolean isUpdatedSystemApp(PackageSetting ps) {
        return (ps.getFlags() & 128) != 0;
    }

    public static void applyEnforceIntentFilterMatching(PlatformCompat compat, ComponentResolverApi resolver, List<ResolveInfo> resolveInfos, boolean isReceiver, final Intent intent, final String resolvedType, int filterCallingUid) {
        final Printer logPrinter;
        ParsedMainComponent comp;
        if (PackageManagerService.DEBUG_INTENT_MATCHING) {
            logPrinter = new LogPrinter(2, "PackageManager", 3);
        } else {
            logPrinter = null;
        }
        if (filterCallingUid != -1 && filterCallingUid == sCustomResolverUid) {
            Slog.d("PackageManager", "skip applyEnforceIntentFilterMatching, cause caller is custom resolver");
            return;
        }
        for (int i = resolveInfos.size() - 1; i >= 0; i--) {
            ComponentInfo info = resolveInfos.get(i).getComponentInfo();
            if (ActivityManager.checkComponentPermission(null, filterCallingUid, info.applicationInfo.uid, false) != 0 && compat.isChangeEnabledInternal(ENFORCE_INTENTS_TO_MATCH_INTENT_FILTERS, info.applicationInfo)) {
                if (info instanceof ActivityInfo) {
                    if (isReceiver) {
                        comp = resolver.getReceiver(info.getComponentName());
                    } else {
                        comp = resolver.getActivity(info.getComponentName());
                    }
                } else if (info instanceof ServiceInfo) {
                    comp = resolver.getService(info.getComponentName());
                } else {
                    throw new IllegalArgumentException("Unsupported component type");
                }
                if (!comp.getIntents().isEmpty()) {
                    boolean match = comp.getIntents().stream().anyMatch(new Predicate() { // from class: com.android.server.pm.PackageManagerServiceUtils$$ExternalSyntheticLambda0
                        @Override // java.util.function.Predicate
                        public final boolean test(Object obj) {
                            boolean intentMatchesFilter;
                            intentMatchesFilter = IntentResolver.intentMatchesFilter(((ParsedIntentInfo) obj).getIntentFilter(), intent, resolvedType);
                            return intentMatchesFilter;
                        }
                    });
                    if (!match) {
                        Slog.w("PackageManager", "Intent does not match component's intent filter: " + intent);
                        Slog.w("PackageManager", "Access blocked: " + comp.getComponentName());
                        if (PackageManagerService.DEBUG_INTENT_MATCHING) {
                            Slog.v("PackageManager", "Component intent filters:");
                            comp.getIntents().forEach(new Consumer() { // from class: com.android.server.pm.PackageManagerServiceUtils$$ExternalSyntheticLambda1
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    ((ParsedIntentInfo) obj).getIntentFilter().dump(logPrinter, "  ");
                                }
                            });
                            Slog.v("PackageManager", "-----------------------------");
                        }
                        resolveInfos.remove(i);
                    }
                }
            }
        }
    }

    public static boolean hasAnyDomainApproval(DomainVerificationManagerInternal manager, PackageStateInternal pkgSetting, Intent intent, long resolveInfoFlags, int userId) {
        return manager.approvalLevelForDomain(pkgSetting, intent, resolveInfoFlags, userId) > 0;
    }

    public static Intent updateIntentForResolve(Intent intent) {
        if (intent.getSelector() != null) {
            intent = intent.getSelector();
        }
        if (PackageManagerService.DEBUG_PREFERRED) {
            intent.addFlags(8);
        }
        return intent;
    }

    public static String arrayToString(int[] array) {
        StringBuilder stringBuilder = new StringBuilder(128);
        stringBuilder.append('[');
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(array[i]);
            }
        }
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    public static File getNextCodePath(File targetDir, String packageName) {
        File firstLevelDir;
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        do {
            random.nextBytes(bytes);
            String firstLevelDirName = "~~" + Base64.encodeToString(bytes, 10);
            firstLevelDir = new File(targetDir, firstLevelDirName);
        } while (firstLevelDir.exists());
        random.nextBytes(bytes);
        String dirName = packageName + '-' + Base64.encodeToString(bytes, 10);
        File result = new File(firstLevelDir, dirName);
        if (DEBUG && !Objects.equals(tryParsePackageName(result.getName()), packageName)) {
            throw new RuntimeException("codepath is off: " + result.getName() + " (" + packageName + ")");
        }
        return result;
    }

    static String tryParsePackageName(String codePath) throws IllegalArgumentException {
        int packageNameEnds = codePath.indexOf(45);
        if (packageNameEnds == -1) {
            throw new IllegalArgumentException("Not a valid package folder name");
        }
        return codePath.substring(0, packageNameEnds);
    }

    public static int getPackageExternalStorageType(VolumeInfo packageVolume, boolean packageIsExternal) {
        DiskInfo disk;
        if (packageVolume != null && (disk = packageVolume.getDisk()) != null) {
            if (disk.isSd()) {
                return 1;
            }
            if (disk.isUsb()) {
                return 2;
            }
            if (packageIsExternal) {
                return 3;
            }
            return 0;
        }
        return 0;
    }

    public static void enforceSystemOrRootOrShell(String message) {
        if (!isSystemOrRootOrShell()) {
            throw new SecurityException(message);
        }
    }

    public static boolean isSystemOrRootOrShell() {
        int uid = Binder.getCallingUid();
        return uid == 1000 || uid == 0 || uid == 2000;
    }

    public static boolean isSystemOrRoot() {
        int uid = Binder.getCallingUid();
        return uid == 1000 || uid == 0;
    }

    public static void enforceSystemOrRoot(String message) {
        if (!isSystemOrRoot()) {
            throw new SecurityException(message);
        }
    }

    public static File preparePackageParserCache(boolean forEngBuild, boolean isUserDebugBuild, String incrementalVersion) {
        File[] listFilesOrEmpty;
        if (forEngBuild) {
            return null;
        }
        if (SystemProperties.getBoolean("pm.boot.disable_package_cache", false)) {
            Slog.i("PackageManager", "Disabling package parser cache due to system property.");
            return null;
        }
        File cacheBaseDir = Environment.getPackageCacheDirectory();
        if (!FileUtils.createDir(cacheBaseDir)) {
            return null;
        }
        String cacheName = PackagePartitions.FINGERPRINT;
        for (File cacheDir : FileUtils.listFilesOrEmpty(cacheBaseDir)) {
            if (Objects.equals(cacheName, cacheDir.getName())) {
                Slog.d("PackageManager", "Keeping known cache " + cacheDir.getName());
            } else {
                Slog.d("PackageManager", "Destroying unknown cache " + cacheDir.getName());
                FileUtils.deleteContentsAndDir(cacheDir);
            }
        }
        File cacheDir2 = FileUtils.createDir(cacheBaseDir, cacheName);
        if (cacheDir2 == null) {
            Slog.wtf("PackageManager", "Cache directory cannot be created - wiping base dir " + cacheBaseDir);
            FileUtils.deleteContentsAndDir(cacheBaseDir);
            return null;
        } else if (isUserDebugBuild && incrementalVersion.startsWith("eng.")) {
            Slog.w("PackageManager", "Wiping cache directory because the system partition changed.");
            File frameworkDir = new File(Environment.getRootDirectory(), "framework");
            if (cacheDir2.lastModified() < frameworkDir.lastModified()) {
                FileUtils.deleteContents(cacheBaseDir);
                return FileUtils.createDir(cacheBaseDir, cacheName);
            }
            return cacheDir2;
        } else {
            return cacheDir2;
        }
    }

    public static void checkDowngrade(AndroidPackage before, PackageInfoLite after) throws PackageManagerException {
        if (after.getLongVersionCode() < before.getLongVersionCode()) {
            throw new PackageManagerException(-25, "Update version code " + after.versionCode + " is older than current " + before.getLongVersionCode());
        }
        if (after.getLongVersionCode() == before.getLongVersionCode()) {
            if (after.baseRevisionCode < before.getBaseRevisionCode()) {
                throw new PackageManagerException(-25, "Update base revision code " + after.baseRevisionCode + " is older than current " + before.getBaseRevisionCode());
            }
            if (!ArrayUtils.isEmpty(after.splitNames)) {
                for (int i = 0; i < after.splitNames.length; i++) {
                    String splitName = after.splitNames[i];
                    int j = ArrayUtils.indexOf(before.getSplitNames(), splitName);
                    if (j != -1 && after.splitRevisionCodes[i] < before.getSplitRevisionCodes()[j]) {
                        throw new PackageManagerException(-25, "Update split " + splitName + " revision code " + after.splitRevisionCodes[i] + " is older than current " + before.getSplitRevisionCodes()[j]);
                    }
                }
            }
        }
    }
}
