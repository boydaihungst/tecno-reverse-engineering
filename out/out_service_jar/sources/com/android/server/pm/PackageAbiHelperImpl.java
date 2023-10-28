package com.android.server.pm;

import android.content.pm.parsing.ApkLiteParseUtils;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Trace;
import android.os.incremental.IncrementalManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.PackageAbiHelper;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.pkg.PackageStateInternal;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import libcore.io.IoUtils;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class PackageAbiHelperImpl implements PackageAbiHelper {
    private static String calculateBundledApkRoot(String codePathString) {
        File f;
        File codePath = new File(codePathString);
        if (FileUtils.contains(Environment.getRootDirectory(), codePath)) {
            f = Environment.getRootDirectory();
        } else {
            File codeRoot = Environment.getOemDirectory();
            if (FileUtils.contains(codeRoot, codePath)) {
                f = Environment.getOemDirectory();
            } else {
                File codeRoot2 = Environment.getVendorDirectory();
                if (FileUtils.contains(codeRoot2, codePath)) {
                    f = Environment.getVendorDirectory();
                } else {
                    File codeRoot3 = Environment.getOdmDirectory();
                    if (FileUtils.contains(codeRoot3, codePath)) {
                        f = Environment.getOdmDirectory();
                    } else {
                        File codeRoot4 = Environment.getProductDirectory();
                        if (FileUtils.contains(codeRoot4, codePath)) {
                            f = Environment.getProductDirectory();
                        } else {
                            File codeRoot5 = Environment.getSystemExtDirectory();
                            if (FileUtils.contains(codeRoot5, codePath)) {
                                f = Environment.getSystemExtDirectory();
                            } else {
                                File codeRoot6 = Environment.getOdmDirectory();
                                if (FileUtils.contains(codeRoot6, codePath)) {
                                    f = Environment.getOdmDirectory();
                                } else {
                                    File codeRoot7 = Environment.getApexDirectory();
                                    if (FileUtils.contains(codeRoot7, codePath)) {
                                        String fullPath = codePath.getAbsolutePath();
                                        String[] parts = fullPath.split(File.separator);
                                        if (parts.length > 2) {
                                            f = new File(parts[1] + File.separator + parts[2]);
                                        } else {
                                            Slog.w("PackageManager", "Can't canonicalize code path " + codePath);
                                            f = Environment.getApexDirectory();
                                        }
                                    } else {
                                        try {
                                            File f2 = codePath.getCanonicalFile();
                                            File parent = f2.getParentFile();
                                            while (true) {
                                                File tmp = parent.getParentFile();
                                                if (tmp == null) {
                                                    break;
                                                }
                                                f2 = parent;
                                                parent = tmp;
                                            }
                                            File codeRoot8 = f2;
                                            Slog.w("PackageManager", "Unrecognized code path " + codePath + " - using " + codeRoot8);
                                            f = codeRoot8;
                                        } catch (IOException e) {
                                            Slog.w("PackageManager", "Can't canonicalize code path " + codePath);
                                            return Environment.getRootDirectory().getPath();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return f.getPath();
    }

    private static String deriveCodePathName(String codePath) {
        if (codePath == null) {
            return null;
        }
        File codeFile = new File(codePath);
        String name = codeFile.getName();
        if (codeFile.isDirectory()) {
            return name;
        }
        if (name.endsWith(".apk") || name.endsWith(".tmp")) {
            int lastDot = name.lastIndexOf(46);
            return name.substring(0, lastDot);
        }
        Slog.w("PackageManager", "Odd, " + codePath + " doesn't look like an APK");
        return null;
    }

    private static void maybeThrowExceptionForMultiArchCopy(String message, int copyRet) throws PackageManagerException {
        if (copyRet < 0 && copyRet != -114 && copyRet != -113) {
            throw new PackageManagerException(copyRet, message);
        }
    }

    @Override // com.android.server.pm.PackageAbiHelper
    public PackageAbiHelper.NativeLibraryPaths deriveNativeLibraryPaths(AndroidPackage pkg, boolean isUpdatedSystemApp, File appLib32InstallDir) {
        return deriveNativeLibraryPaths(new PackageAbiHelper.Abis(AndroidPackageUtils.getRawPrimaryCpuAbi(pkg), AndroidPackageUtils.getRawSecondaryCpuAbi(pkg)), appLib32InstallDir, pkg.getPath(), pkg.getBaseApkPath(), pkg.isSystem(), isUpdatedSystemApp);
    }

    private static PackageAbiHelper.NativeLibraryPaths deriveNativeLibraryPaths(PackageAbiHelper.Abis abis, File appLib32InstallDir, String codePath, String sourceDir, boolean isSystemApp, boolean isUpdatedSystemApp) {
        String nativeLibraryRootDir;
        boolean nativeLibraryRootRequiresIsa;
        String nativeLibraryDir;
        String secondaryNativeLibraryDir;
        File codeFile = new File(codePath);
        boolean bundledApp = isSystemApp && !isUpdatedSystemApp;
        if (ApkLiteParseUtils.isApkFile(codeFile)) {
            if (bundledApp) {
                String apkRoot = calculateBundledApkRoot(sourceDir);
                boolean is64Bit = VMRuntime.is64BitInstructionSet(InstructionSets.getPrimaryInstructionSet(abis));
                String apkName = deriveCodePathName(codePath);
                String libDir = is64Bit ? "lib64" : "lib";
                nativeLibraryRootDir = Environment.buildPath(new File(apkRoot), new String[]{libDir, apkName}).getAbsolutePath();
                if (abis.secondary != null) {
                    String secondaryLibDir = is64Bit ? "lib" : "lib64";
                    secondaryNativeLibraryDir = Environment.buildPath(new File(apkRoot), new String[]{secondaryLibDir, apkName}).getAbsolutePath();
                } else {
                    secondaryNativeLibraryDir = null;
                }
            } else {
                String apkName2 = deriveCodePathName(codePath);
                nativeLibraryRootDir = new File(appLib32InstallDir, apkName2).getAbsolutePath();
                secondaryNativeLibraryDir = null;
            }
            nativeLibraryRootRequiresIsa = false;
            nativeLibraryDir = nativeLibraryRootDir;
        } else if (codePath != null && (codePath.contains("vendor/operator/app") || codePath.contains("product/operator/app"))) {
            String apkName3 = deriveCodePathName(codePath);
            nativeLibraryRootDir = new File("/data/preload-app-lib", apkName3).getAbsolutePath();
            nativeLibraryDir = nativeLibraryRootDir;
            secondaryNativeLibraryDir = null;
            nativeLibraryRootRequiresIsa = false;
        } else {
            nativeLibraryRootDir = new File(codeFile, "lib").getAbsolutePath();
            nativeLibraryRootRequiresIsa = true;
            nativeLibraryDir = new File(nativeLibraryRootDir, InstructionSets.getPrimaryInstructionSet(abis)).getAbsolutePath();
            if (abis.secondary != null) {
                secondaryNativeLibraryDir = new File(nativeLibraryRootDir, VMRuntime.getInstructionSet(abis.secondary)).getAbsolutePath();
            } else {
                secondaryNativeLibraryDir = null;
            }
        }
        return new PackageAbiHelper.NativeLibraryPaths(nativeLibraryRootDir, nativeLibraryRootRequiresIsa, nativeLibraryDir, secondaryNativeLibraryDir);
    }

    @Override // com.android.server.pm.PackageAbiHelper
    public PackageAbiHelper.Abis getBundledAppAbis(AndroidPackage pkg) {
        String apkName = deriveCodePathName(pkg.getPath());
        String apkRoot = calculateBundledApkRoot(pkg.getBaseApkPath());
        PackageAbiHelper.Abis abis = getBundledAppAbi(pkg, apkRoot, apkName);
        return abis;
    }

    private PackageAbiHelper.Abis getBundledAppAbi(AndroidPackage pkg, String apkRoot, String apkName) {
        boolean has64BitLibs;
        boolean has64BitLibs2;
        boolean has64BitLibs3;
        String primaryCpuAbi;
        String secondaryCpuAbi;
        File codeFile = new File(pkg.getPath());
        if (ApkLiteParseUtils.isApkFile(codeFile)) {
            has64BitLibs2 = new File(apkRoot, new File("lib64", apkName).getPath()).exists();
            has64BitLibs3 = new File(apkRoot, new File("lib", apkName).getPath()).exists();
        } else {
            File rootDir = new File(codeFile, "lib");
            if (!ArrayUtils.isEmpty(Build.SUPPORTED_64_BIT_ABIS) && !TextUtils.isEmpty(Build.SUPPORTED_64_BIT_ABIS[0])) {
                String isa = VMRuntime.getInstructionSet(Build.SUPPORTED_64_BIT_ABIS[0]);
                has64BitLibs = new File(rootDir, isa).exists();
            } else {
                has64BitLibs = false;
            }
            if (!ArrayUtils.isEmpty(Build.SUPPORTED_32_BIT_ABIS) && !TextUtils.isEmpty(Build.SUPPORTED_32_BIT_ABIS[0])) {
                String isa2 = VMRuntime.getInstructionSet(Build.SUPPORTED_32_BIT_ABIS[0]);
                boolean has32BitLibs = new File(rootDir, isa2).exists();
                has64BitLibs2 = has64BitLibs;
                has64BitLibs3 = has32BitLibs;
            } else {
                has64BitLibs2 = has64BitLibs;
                has64BitLibs3 = false;
            }
        }
        if (has64BitLibs2 && !has64BitLibs3) {
            primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
            secondaryCpuAbi = null;
        } else if (has64BitLibs3 && !has64BitLibs2) {
            primaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
            secondaryCpuAbi = null;
        } else if (has64BitLibs3 && has64BitLibs2) {
            if (!pkg.isMultiArch()) {
                Slog.e("PackageManager", "Package " + pkg + " has multiple bundled libs, but is not multiarch.");
            }
            if (VMRuntime.is64BitInstructionSet(InstructionSets.getPreferredInstructionSet())) {
                String primaryCpuAbi2 = Build.SUPPORTED_64_BIT_ABIS[0];
                secondaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
                primaryCpuAbi = primaryCpuAbi2;
            } else {
                String primaryCpuAbi3 = Build.SUPPORTED_32_BIT_ABIS[0];
                secondaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
                primaryCpuAbi = primaryCpuAbi3;
            }
        } else {
            primaryCpuAbi = null;
            secondaryCpuAbi = null;
        }
        return new PackageAbiHelper.Abis(primaryCpuAbi, secondaryCpuAbi);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [472=4] */
    /* JADX WARN: Code restructure failed: missing block: B:77:0x017f, code lost:
        if (com.android.server.pm.parsing.pkg.AndroidPackageUtils.isLibrary(r25) != false) goto L87;
     */
    /* JADX WARN: Code restructure failed: missing block: B:78:0x0181, code lost:
        r8 = r2[r1];
     */
    /* JADX WARN: Code restructure failed: missing block: B:80:0x018e, code lost:
        throw new com.android.server.pm.PackageManagerException(android.hardware.biometrics.fingerprint.V2_1.RequestStatus.SYS_ETIMEDOUT, "Shared library with native libs must be multiarch");
     */
    @Override // com.android.server.pm.PackageAbiHelper
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public Pair<PackageAbiHelper.Abis, PackageAbiHelper.NativeLibraryPaths> derivePackageAbi(AndroidPackage pkg, boolean isUpdatedSystemApp, String cpuAbiOverride, File appLib32InstallDir) throws PackageManagerException {
        int copyRet;
        int abi64;
        int abi642;
        String pkgRawPrimaryCpuAbi = AndroidPackageUtils.getRawPrimaryCpuAbi(pkg);
        String pkgRawSecondaryCpuAbi = AndroidPackageUtils.getRawSecondaryCpuAbi(pkg);
        PackageAbiHelper.NativeLibraryPaths initialLibraryPaths = deriveNativeLibraryPaths(new PackageAbiHelper.Abis(pkgRawPrimaryCpuAbi, pkgRawSecondaryCpuAbi), appLib32InstallDir, pkg.getPath(), pkg.getBaseApkPath(), pkg.isSystem(), isUpdatedSystemApp);
        boolean extractLibs = shouldExtractLibs(pkg, isUpdatedSystemApp);
        String nativeLibraryRootStr = initialLibraryPaths.nativeLibraryRootDir;
        boolean useIsaSpecificSubdirs = initialLibraryPaths.nativeLibraryRootRequiresIsa;
        boolean onIncremental = IncrementalManager.isIncrementalPath(pkg.getPath());
        String primaryCpuAbi = null;
        String secondaryCpuAbi = null;
        NativeLibraryHelper.Handle handle = null;
        try {
            try {
                handle = AndroidPackageUtils.createNativeLibraryHandle(pkg);
                File nativeLibraryRoot = new File(nativeLibraryRootStr);
                primaryCpuAbi = null;
                secondaryCpuAbi = null;
                try {
                    if (pkg.isMultiArch()) {
                        int abi32 = -114;
                        try {
                            if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                                if (extractLibs) {
                                    Trace.traceBegin(262144L, "copyNativeBinaries");
                                    abi32 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handle, nativeLibraryRoot, Build.SUPPORTED_32_BIT_ABIS, useIsaSpecificSubdirs, onIncremental);
                                } else {
                                    Trace.traceBegin(262144L, "findSupportedAbi");
                                    abi32 = NativeLibraryHelper.findSupportedAbi(handle, Build.SUPPORTED_32_BIT_ABIS);
                                }
                                Trace.traceEnd(262144L);
                            }
                            if (abi32 >= 0 && AndroidPackageUtils.isLibrary(pkg) && extractLibs) {
                                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Shared library native lib extraction not supported");
                            }
                            maybeThrowExceptionForMultiArchCopy("Error unpackaging 32 bit native libs for multiarch app.", abi32);
                            if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                                if (extractLibs) {
                                    Trace.traceBegin(262144L, "copyNativeBinaries");
                                    abi642 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handle, nativeLibraryRoot, Build.SUPPORTED_64_BIT_ABIS, useIsaSpecificSubdirs, onIncremental);
                                } else {
                                    Trace.traceBegin(262144L, "findSupportedAbi");
                                    int abi643 = NativeLibraryHelper.findSupportedAbi(handle, Build.SUPPORTED_64_BIT_ABIS);
                                    abi642 = abi643;
                                }
                                Trace.traceEnd(262144L);
                                abi64 = abi642;
                            } else {
                                abi64 = -114;
                            }
                            maybeThrowExceptionForMultiArchCopy("Error unpackaging 64 bit native libs for multiarch app.", abi64);
                            if (abi64 >= 0) {
                                if (extractLibs && AndroidPackageUtils.isLibrary(pkg)) {
                                    throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Shared library native lib extraction not supported");
                                }
                                primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[abi64];
                            }
                            if (abi32 >= 0) {
                                String abi = Build.SUPPORTED_32_BIT_ABIS[abi32];
                                if (abi64 < 0) {
                                    primaryCpuAbi = abi;
                                } else if (pkg.isUse32BitAbi()) {
                                    secondaryCpuAbi = primaryCpuAbi;
                                    primaryCpuAbi = abi;
                                } else {
                                    secondaryCpuAbi = abi;
                                }
                            }
                        } catch (IOException e) {
                            ioe = e;
                            Slog.e("PackageManager", "Unable to get canonical file " + ioe.toString());
                            IoUtils.closeQuietly(handle);
                            PackageAbiHelper.Abis abis = new PackageAbiHelper.Abis(primaryCpuAbi, secondaryCpuAbi);
                            return new Pair<>(abis, deriveNativeLibraryPaths(abis, appLib32InstallDir, pkg.getPath(), pkg.getBaseApkPath(), pkg.isSystem(), isUpdatedSystemApp));
                        } catch (Throwable th) {
                            th = th;
                            IoUtils.closeQuietly(handle);
                            throw th;
                        }
                    } else {
                        String[] abiList = cpuAbiOverride != null ? new String[]{cpuAbiOverride} : Build.SUPPORTED_ABIS;
                        boolean needsRenderScriptOverride = false;
                        if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && cpuAbiOverride == null && NativeLibraryHelper.hasRenderscriptBitcode(handle)) {
                            if (Build.SUPPORTED_32_BIT_ABIS.length <= 0) {
                                throw new PackageManagerException(-16, "Apps that contain RenderScript with target API level < 21 are not supported on 64-bit only platforms");
                            }
                            abiList = Build.SUPPORTED_32_BIT_ABIS;
                            needsRenderScriptOverride = true;
                        }
                        if (extractLibs) {
                            Trace.traceBegin(262144L, "copyNativeBinaries");
                            copyRet = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(handle, nativeLibraryRoot, abiList, useIsaSpecificSubdirs, onIncremental);
                        } else {
                            Trace.traceBegin(262144L, "findSupportedAbi");
                            abiList = abiList;
                            copyRet = NativeLibraryHelper.findSupportedAbi(handle, abiList);
                        }
                        Trace.traceEnd(262144L);
                        if (copyRet < 0 && copyRet != -114) {
                            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Error unpackaging native libs for app, errorCode=" + copyRet);
                        }
                        if (copyRet == -114 && cpuAbiOverride != null) {
                            primaryCpuAbi = cpuAbiOverride;
                        } else if (needsRenderScriptOverride) {
                            primaryCpuAbi = abiList[0];
                        }
                    }
                } catch (IOException e2) {
                    ioe = e2;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e3) {
            ioe = e3;
        } catch (Throwable th3) {
            th = th3;
        }
        IoUtils.closeQuietly(handle);
        PackageAbiHelper.Abis abis2 = new PackageAbiHelper.Abis(primaryCpuAbi, secondaryCpuAbi);
        return new Pair<>(abis2, deriveNativeLibraryPaths(abis2, appLib32InstallDir, pkg.getPath(), pkg.getBaseApkPath(), pkg.isSystem(), isUpdatedSystemApp));
    }

    private boolean shouldExtractLibs(AndroidPackage pkg, boolean isUpdatedSystemApp) {
        boolean extractLibs = !AndroidPackageUtils.isLibrary(pkg) && pkg.isExtractNativeLibs();
        if (pkg.isSystem() && !isUpdatedSystemApp) {
            return false;
        }
        return extractLibs;
    }

    @Override // com.android.server.pm.PackageAbiHelper
    public String getAdjustedAbiForSharedUser(ArraySet<? extends PackageStateInternal> packagesForUser, AndroidPackage scannedPackage) {
        String pkgRawPrimaryCpuAbi;
        String requiredInstructionSet = null;
        if (scannedPackage != null && (pkgRawPrimaryCpuAbi = AndroidPackageUtils.getRawPrimaryCpuAbi(scannedPackage)) != null) {
            requiredInstructionSet = VMRuntime.getInstructionSet(pkgRawPrimaryCpuAbi);
        }
        PackageStateInternal requirer = null;
        Iterator<? extends PackageStateInternal> it = packagesForUser.iterator();
        while (it.hasNext()) {
            PackageStateInternal ps = it.next();
            if (scannedPackage == null || !scannedPackage.getPackageName().equals(ps.getPackageName())) {
                if (ps.getPrimaryCpuAbi() != null) {
                    String instructionSet = VMRuntime.getInstructionSet(ps.getPrimaryCpuAbi());
                    if (requiredInstructionSet != null && !requiredInstructionSet.equals(instructionSet)) {
                        String errorMessage = "Instruction set mismatch, " + (requirer == null ? "[caller]" : requirer) + " requires " + requiredInstructionSet + " whereas " + ps + " requires " + instructionSet;
                        Slog.w("PackageManager", errorMessage);
                    }
                    if (requiredInstructionSet == null) {
                        requiredInstructionSet = instructionSet;
                        requirer = ps;
                    }
                }
            }
        }
        if (requiredInstructionSet == null) {
            return null;
        }
        if (requirer != null) {
            String adjustedAbi = requirer.getPrimaryCpuAbi();
            return adjustedAbi;
        }
        String adjustedAbi2 = AndroidPackageUtils.getRawPrimaryCpuAbi(scannedPackage);
        return adjustedAbi2;
    }
}
