package com.android.server.pm;

import android.content.pm.SharedLibraryInfo;
import android.content.pm.SigningDetails;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.os.Build;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.apk.ApkSignatureVerifier;
import android.util.jar.StrictJarFile;
import com.android.internal.util.ArrayUtils;
import com.android.server.SystemConfig;
import com.android.server.am.HostingRecord;
import com.android.server.location.gnss.hal.GnssNative;
import com.android.server.pm.PackageAbiHelper;
import com.android.server.pm.Settings;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.library.PackageBackwardCompatibility;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUtils;
import com.android.server.pm.pkg.component.ComponentMutateUtils;
import com.android.server.pm.pkg.component.ParsedActivity;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.android.server.pm.pkg.component.ParsedProcess;
import com.android.server.pm.pkg.component.ParsedProvider;
import com.android.server.pm.pkg.component.ParsedService;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import com.android.server.utils.WatchedArraySet;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class ScanPackageUtils {
    ScanPackageUtils() {
    }

    public static ScanResult scanPackageOnlyLI(ScanRequest request, PackageManagerServiceInjector injector, boolean isUnderFactoryTest, long currentTime) throws PackageManagerException {
        String secondaryCpuAbiFromSettings;
        String secondaryCpuAbiFromSettings2;
        PackageSetting pkgSetting;
        String primaryCpuAbiFromSettings;
        String[] usesSdkLibraries;
        String[] usesStaticLibraries;
        String primaryCpuAbiFromSettings2;
        PackageSetting pkgSetting2;
        String primaryCpuAbiFromSettings3;
        String secondaryCpuAbiFromSettings3;
        long existingFirstInstallTime;
        SharedLibraryInfo sdkLibraryInfo;
        SharedLibraryInfo staticSharedLibraryInfo;
        List<SharedLibraryInfo> dynamicSharedLibraryInfos;
        String str;
        PackageAbiHelper packageAbiHelper = injector.getAbiHelper();
        ParsedPackage parsedPackage = request.mParsedPackage;
        PackageSetting pkgSetting3 = request.mPkgSetting;
        PackageSetting disabledPkgSetting = request.mDisabledPkgSetting;
        PackageSetting originalPkgSetting = request.mOriginalPkgSetting;
        int parseFlags = request.mParseFlags;
        int scanFlags = request.mScanFlags;
        String realPkgName = request.mRealPkgName;
        SharedUserSetting oldSharedUserSetting = request.mOldSharedUserSetting;
        SharedUserSetting sharedUserSetting = request.mSharedUserSetting;
        UserHandle user = request.mUser;
        boolean isPlatformPackage = request.mIsPlatformPackage;
        List<String> changedAbiCodePath = null;
        if (PackageManagerService.DEBUG_PACKAGE_SCANNING && (Integer.MIN_VALUE & parseFlags) != 0) {
            Log.d("PackageManager", "Scanning package " + parsedPackage.getPackageName());
        }
        File destCodeFile = new File(parsedPackage.getPath());
        boolean needToDeriveAbi = (scanFlags & 4096) != 0;
        if (needToDeriveAbi) {
            secondaryCpuAbiFromSettings = null;
            secondaryCpuAbiFromSettings2 = null;
        } else if (pkgSetting3 != null) {
            if (pkgSetting3.getPkg() != null && pkgSetting3.getPkg().isStub()) {
                needToDeriveAbi = true;
                secondaryCpuAbiFromSettings = null;
                secondaryCpuAbiFromSettings2 = null;
            } else {
                String primaryCpuAbiFromSettings4 = pkgSetting3.getPrimaryCpuAbi();
                String secondaryCpuAbiFromSettings4 = pkgSetting3.getSecondaryCpuAbi();
                secondaryCpuAbiFromSettings = secondaryCpuAbiFromSettings4;
                secondaryCpuAbiFromSettings2 = primaryCpuAbiFromSettings4;
            }
        } else {
            needToDeriveAbi = true;
            secondaryCpuAbiFromSettings = null;
            secondaryCpuAbiFromSettings2 = null;
        }
        if (pkgSetting3 != null && oldSharedUserSetting != sharedUserSetting) {
            StringBuilder append = new StringBuilder().append("Package ").append(parsedPackage.getPackageName()).append(" shared user changed from ");
            String str2 = "<nothing>";
            if (oldSharedUserSetting != null) {
                str = "<nothing>";
                str2 = oldSharedUserSetting.name;
            } else {
                str = "<nothing>";
            }
            PackageManagerService.reportSettingsProblem(5, append.append(str2).append(" to ").append(sharedUserSetting != null ? sharedUserSetting.name : str).append("; replacing with new").toString());
            pkgSetting = null;
        } else {
            pkgSetting = pkgSetting3;
        }
        if (parsedPackage.getUsesSdkLibraries().isEmpty()) {
            primaryCpuAbiFromSettings = secondaryCpuAbiFromSettings2;
            usesSdkLibraries = null;
        } else {
            String[] usesSdkLibraries2 = new String[parsedPackage.getUsesSdkLibraries().size()];
            primaryCpuAbiFromSettings = secondaryCpuAbiFromSettings2;
            parsedPackage.getUsesSdkLibraries().toArray(usesSdkLibraries2);
            usesSdkLibraries = usesSdkLibraries2;
        }
        if (parsedPackage.getUsesStaticLibraries().isEmpty()) {
            usesStaticLibraries = null;
        } else {
            String[] usesStaticLibraries2 = new String[parsedPackage.getUsesStaticLibraries().size()];
            parsedPackage.getUsesStaticLibraries().toArray(usesStaticLibraries2);
            usesStaticLibraries = usesStaticLibraries2;
        }
        UUID newDomainSetId = injector.getDomainVerificationManagerInternal().generateNewId();
        boolean createNewPackage = pkgSetting == null;
        if (createNewPackage) {
            boolean instantApp = (scanFlags & 8192) != 0;
            boolean virtualPreload = (32768 & scanFlags) != 0;
            int pkgFlags = PackageInfoUtils.appInfoFlags(parsedPackage, (PackageStateInternal) null);
            int pkgPrivateFlags = PackageInfoUtils.appInfoPrivateFlags(parsedPackage, (PackageStateInternal) null);
            primaryCpuAbiFromSettings2 = primaryCpuAbiFromSettings;
            pkgSetting2 = Settings.createNewSetting(parsedPackage.getPackageName(), originalPkgSetting, disabledPkgSetting, realPkgName, sharedUserSetting, destCodeFile, parsedPackage.getNativeLibraryRootDir(), AndroidPackageUtils.getRawPrimaryCpuAbi(parsedPackage), AndroidPackageUtils.getRawSecondaryCpuAbi(parsedPackage), parsedPackage.getLongVersionCode(), pkgFlags, pkgPrivateFlags, user, true, instantApp, virtualPreload, UserManagerService.getInstance(), usesSdkLibraries, parsedPackage.getUsesSdkLibrariesVersionsMajor(), usesStaticLibraries, parsedPackage.getUsesStaticLibrariesVersions(), parsedPackage.getMimeGroups(), newDomainSetId);
        } else {
            primaryCpuAbiFromSettings2 = primaryCpuAbiFromSettings;
            pkgSetting2 = new PackageSetting(pkgSetting);
            pkgSetting2.setPkg(parsedPackage);
            Settings.updatePackageSetting(pkgSetting2, disabledPkgSetting, oldSharedUserSetting, sharedUserSetting, destCodeFile, parsedPackage.getNativeLibraryDir(), AndroidPackageUtils.getPrimaryCpuAbi(parsedPackage, pkgSetting2), AndroidPackageUtils.getSecondaryCpuAbi(parsedPackage, pkgSetting2), PackageInfoUtils.appInfoFlags(parsedPackage, pkgSetting2), PackageInfoUtils.appInfoPrivateFlags(parsedPackage, pkgSetting2), UserManagerService.getInstance(), usesSdkLibraries, parsedPackage.getUsesSdkLibrariesVersionsMajor(), usesStaticLibraries, parsedPackage.getUsesStaticLibrariesVersions(), parsedPackage.getMimeGroups(), newDomainSetId);
        }
        if (createNewPackage && originalPkgSetting != null) {
            parsedPackage.setPackageName(originalPkgSetting.getPackageName());
            String msg = "New package " + pkgSetting2.getRealName() + " renamed to replace old package " + pkgSetting2.getPackageName();
            PackageManagerService.reportSettingsProblem(5, msg);
        }
        int userId = user == null ? 0 : user.getIdentifier();
        if (!createNewPackage) {
            boolean instantApp2 = (scanFlags & 8192) != 0;
            boolean fullApp = (scanFlags & 16384) != 0;
            setInstantAppForUser(injector, pkgSetting2, userId, instantApp2, fullApp);
        }
        if (disabledPkgSetting != null || ((scanFlags & 4) != 0 && pkgSetting2 != null && pkgSetting2.isSystem())) {
            pkgSetting2.getPkgState().setUpdatedSystemApp(true);
        }
        parsedPackage.setSeInfo(SELinuxMMAC.getSeInfo(parsedPackage, sharedUserSetting, injector.getCompatibility()));
        if (parsedPackage.isSystem()) {
            configurePackageComponents(parsedPackage);
        }
        String cpuAbiOverride = PackageManagerServiceUtils.deriveAbiOverride(request.mCpuAbiOverride);
        boolean isUpdatedSystemApp = pkgSetting2.getPkgState().isUpdatedSystemApp();
        File appLib32InstallDir = getAppLib32InstallDir();
        if ((scanFlags & 4) == 0) {
            if (needToDeriveAbi) {
                Trace.traceBegin(262144L, "derivePackageAbi");
                Pair<PackageAbiHelper.Abis, PackageAbiHelper.NativeLibraryPaths> derivedAbi = packageAbiHelper.derivePackageAbi(parsedPackage, isUpdatedSystemApp, cpuAbiOverride, appLib32InstallDir);
                ((PackageAbiHelper.Abis) derivedAbi.first).applyTo(parsedPackage);
                ((PackageAbiHelper.NativeLibraryPaths) derivedAbi.second).applyTo(parsedPackage);
                Trace.traceEnd(262144L);
                String pkgRawPrimaryCpuAbi = AndroidPackageUtils.getRawPrimaryCpuAbi(parsedPackage);
                if (parsedPackage.isSystem() && !isUpdatedSystemApp && pkgRawPrimaryCpuAbi == null) {
                    PackageAbiHelper.Abis abis = packageAbiHelper.getBundledAppAbis(parsedPackage);
                    abis.applyTo(parsedPackage);
                    abis.applyTo(pkgSetting2);
                    PackageAbiHelper.NativeLibraryPaths nativeLibraryPaths = packageAbiHelper.deriveNativeLibraryPaths(parsedPackage, isUpdatedSystemApp, appLib32InstallDir);
                    nativeLibraryPaths.applyTo(parsedPackage);
                }
                primaryCpuAbiFromSettings3 = primaryCpuAbiFromSettings2;
                secondaryCpuAbiFromSettings3 = secondaryCpuAbiFromSettings;
            } else {
                secondaryCpuAbiFromSettings3 = secondaryCpuAbiFromSettings;
                parsedPackage.setPrimaryCpuAbi(primaryCpuAbiFromSettings2).setSecondaryCpuAbi(secondaryCpuAbiFromSettings3);
                PackageAbiHelper.NativeLibraryPaths nativeLibraryPaths2 = packageAbiHelper.deriveNativeLibraryPaths(parsedPackage, isUpdatedSystemApp, appLib32InstallDir);
                nativeLibraryPaths2.applyTo(parsedPackage);
                if (!PackageManagerService.DEBUG_ABI_SELECTION) {
                    primaryCpuAbiFromSettings3 = primaryCpuAbiFromSettings2;
                } else {
                    primaryCpuAbiFromSettings3 = primaryCpuAbiFromSettings2;
                    Slog.i("PackageManager", "Using ABIS and native lib paths from settings : " + parsedPackage.getPackageName() + " " + AndroidPackageUtils.getRawPrimaryCpuAbi(parsedPackage) + ", " + AndroidPackageUtils.getRawSecondaryCpuAbi(parsedPackage));
                }
            }
        } else {
            primaryCpuAbiFromSettings3 = primaryCpuAbiFromSettings2;
            secondaryCpuAbiFromSettings3 = secondaryCpuAbiFromSettings;
            if ((scanFlags & 256) != 0) {
                parsedPackage.setPrimaryCpuAbi(pkgSetting2.getPrimaryCpuAbi()).setSecondaryCpuAbi(pkgSetting2.getSecondaryCpuAbi());
            }
            PackageAbiHelper.NativeLibraryPaths nativeLibraryPaths3 = packageAbiHelper.deriveNativeLibraryPaths(parsedPackage, isUpdatedSystemApp, appLib32InstallDir);
            nativeLibraryPaths3.applyTo(parsedPackage);
        }
        if (isPlatformPackage) {
            parsedPackage.setPrimaryCpuAbi(VMRuntime.getRuntime().is64Bit() ? Build.SUPPORTED_64_BIT_ABIS[0] : Build.SUPPORTED_32_BIT_ABIS[0]);
        }
        if ((scanFlags & 1) == 0 && (scanFlags & 4) != 0 && cpuAbiOverride == null) {
            Slog.w("PackageManager", "Ignoring persisted ABI override for package " + parsedPackage.getPackageName());
        }
        pkgSetting2.setPrimaryCpuAbi(AndroidPackageUtils.getRawPrimaryCpuAbi(parsedPackage)).setSecondaryCpuAbi(AndroidPackageUtils.getRawSecondaryCpuAbi(parsedPackage)).setCpuAbiOverride(cpuAbiOverride);
        if (PackageManagerService.DEBUG_ABI_SELECTION) {
            Slog.d("PackageManager", "Resolved nativeLibraryRoot for " + parsedPackage.getPackageName() + " to root=" + parsedPackage.getNativeLibraryRootDir() + ", to dir=" + parsedPackage.getNativeLibraryDir() + ", isa=" + parsedPackage.isNativeLibraryRootRequiresIsa());
        }
        pkgSetting2.setLegacyNativeLibraryPath(parsedPackage.getNativeLibraryRootDir());
        if (PackageManagerService.DEBUG_ABI_SELECTION) {
            Log.d("PackageManager", "Abis for package[" + parsedPackage.getPackageName() + "] are primary=" + pkgSetting2.getPrimaryCpuAbi() + " secondary=" + pkgSetting2.getSecondaryCpuAbi() + " abiOverride=" + pkgSetting2.getCpuAbiOverride());
        }
        if ((scanFlags & 16) == 0 && oldSharedUserSetting != null) {
            changedAbiCodePath = applyAdjustedAbiToSharedUser(oldSharedUserSetting, parsedPackage, packageAbiHelper.getAdjustedAbiForSharedUser(oldSharedUserSetting.getPackageStates(), parsedPackage));
        }
        parsedPackage.setFactoryTest(isUnderFactoryTest && parsedPackage.getRequestedPermissions().contains("android.permission.FACTORY_TEST"));
        if (parsedPackage.isSystem()) {
            pkgSetting2.setIsOrphaned(true);
        }
        long scanFileTime = PackageManagerServiceUtils.getLastModifiedTime(parsedPackage);
        if (userId == -1) {
            existingFirstInstallTime = PackageStateUtils.getEarliestFirstInstallTime(pkgSetting2.getUserStates());
        } else {
            existingFirstInstallTime = pkgSetting2.readUserState(userId).getFirstInstallTime();
        }
        if (currentTime == 0) {
            if (existingFirstInstallTime == 0) {
                pkgSetting2.setFirstInstallTime(scanFileTime, userId).setLastUpdateTime(scanFileTime);
            } else if ((parseFlags & 16) != 0 && scanFileTime != pkgSetting2.getLastModifiedTime()) {
                pkgSetting2.setLastUpdateTime(scanFileTime);
            }
        } else if (existingFirstInstallTime == 0) {
            pkgSetting2.setFirstInstallTime(currentTime, userId).setLastUpdateTime(currentTime);
        } else if ((scanFlags & 8) != 0) {
            pkgSetting2.setLastUpdateTime(currentTime);
        }
        pkgSetting2.setLastModifiedTime(scanFileTime);
        pkgSetting2.setPkg(parsedPackage).setPkgFlags(PackageInfoUtils.appInfoFlags(parsedPackage, pkgSetting2), PackageInfoUtils.appInfoPrivateFlags(parsedPackage, pkgSetting2));
        if (parsedPackage.getLongVersionCode() != pkgSetting2.getVersionCode()) {
            pkgSetting2.setLongVersionCode(parsedPackage.getLongVersionCode());
        }
        String volumeUuid = parsedPackage.getVolumeUuid();
        if (!Objects.equals(volumeUuid, pkgSetting2.getVolumeUuid())) {
            Slog.i("PackageManager", "Update" + (pkgSetting2.isSystem() ? " system" : "") + " package " + parsedPackage.getPackageName() + " volume from " + pkgSetting2.getVolumeUuid() + " to " + volumeUuid);
            pkgSetting2.setVolumeUuid(volumeUuid);
        }
        if (TextUtils.isEmpty(parsedPackage.getSdkLibName())) {
            sdkLibraryInfo = null;
        } else {
            SharedLibraryInfo sdkLibraryInfo2 = AndroidPackageUtils.createSharedLibraryForSdk(parsedPackage);
            sdkLibraryInfo = sdkLibraryInfo2;
        }
        if (TextUtils.isEmpty(parsedPackage.getStaticSharedLibName())) {
            staticSharedLibraryInfo = null;
        } else {
            SharedLibraryInfo staticSharedLibraryInfo2 = AndroidPackageUtils.createSharedLibraryForStatic(parsedPackage);
            staticSharedLibraryInfo = staticSharedLibraryInfo2;
        }
        if (ArrayUtils.isEmpty(parsedPackage.getLibraryNames())) {
            dynamicSharedLibraryInfos = null;
        } else {
            List<SharedLibraryInfo> dynamicSharedLibraryInfos2 = new ArrayList<>(parsedPackage.getLibraryNames().size());
            for (String name : parsedPackage.getLibraryNames()) {
                dynamicSharedLibraryInfos2.add(AndroidPackageUtils.createSharedLibraryForDynamic(parsedPackage, name));
            }
            dynamicSharedLibraryInfos = dynamicSharedLibraryInfos2;
        }
        return new ScanResult(request, true, pkgSetting2, changedAbiCodePath, !createNewPackage, -1, sdkLibraryInfo, staticSharedLibraryInfo, dynamicSharedLibraryInfos);
    }

    public static int adjustScanFlagsWithPackageSetting(int scanFlags, PackageSetting pkgSetting, PackageSetting disabledPkgSetting, UserHandle user) {
        PackageSetting systemPkgSetting;
        if ((scanFlags & 4) != 0 && disabledPkgSetting == null && pkgSetting != null && pkgSetting.isSystem()) {
            systemPkgSetting = pkgSetting;
        } else {
            systemPkgSetting = disabledPkgSetting;
        }
        if (systemPkgSetting != null) {
            scanFlags |= 65536;
            if ((systemPkgSetting.getPrivateFlags() & 8) != 0) {
                scanFlags |= 131072;
            }
            if ((systemPkgSetting.getPrivateFlags() & 131072) != 0) {
                scanFlags |= 262144;
            }
            if ((systemPkgSetting.getPrivateFlags() & 262144) != 0) {
                scanFlags |= 524288;
            }
            if ((systemPkgSetting.getPrivateFlags() & 524288) != 0) {
                scanFlags |= 1048576;
            }
            if ((systemPkgSetting.getPrivateFlags() & 2097152) != 0) {
                scanFlags |= 2097152;
            }
            if ((systemPkgSetting.getPrivateFlags() & 1073741824) != 0) {
                scanFlags |= 4194304;
            }
        }
        if (pkgSetting != null) {
            int userId = user == null ? 0 : user.getIdentifier();
            if (pkgSetting.getInstantApp(userId)) {
                scanFlags |= 8192;
            }
            if (pkgSetting.getVirtualPreload(userId)) {
                return scanFlags | 32768;
            }
            return scanFlags;
        }
        return scanFlags;
    }

    public static void assertCodePolicy(AndroidPackage pkg) throws PackageManagerException {
        boolean shouldHaveCode = pkg.isHasCode();
        if (shouldHaveCode && !apkHasCode(pkg.getBaseApkPath())) {
            throw new PackageManagerException(-2, "Package " + pkg.getBaseApkPath() + " code is missing");
        }
        if (!ArrayUtils.isEmpty(pkg.getSplitCodePaths())) {
            for (int i = 0; i < pkg.getSplitCodePaths().length; i++) {
                boolean splitShouldHaveCode = (pkg.getSplitFlags()[i] & 4) != 0;
                if (splitShouldHaveCode && !apkHasCode(pkg.getSplitCodePaths()[i])) {
                    throw new PackageManagerException(-2, "Package " + pkg.getSplitCodePaths()[i] + " code is missing");
                }
            }
        }
    }

    public static void assertStaticSharedLibraryIsValid(AndroidPackage pkg, int scanFlags) throws PackageManagerException {
        if (pkg.getTargetSdkVersion() < 26) {
            throw new PackageManagerException("Packages declaring static-shared libs must target O SDK or higher");
        }
        if ((scanFlags & 8192) != 0) {
            throw new PackageManagerException("Packages declaring static-shared libs cannot be instant apps");
        }
        if (!ArrayUtils.isEmpty(pkg.getOriginalPackages())) {
            throw new PackageManagerException("Packages declaring static-shared libs cannot be renamed");
        }
        if (!ArrayUtils.isEmpty(pkg.getLibraryNames())) {
            throw new PackageManagerException("Packages declaring static-shared libs cannot declare dynamic libs");
        }
        if (pkg.getSharedUserId() != null) {
            throw new PackageManagerException("Packages declaring static-shared libs cannot declare shared users");
        }
        if (!pkg.getActivities().isEmpty()) {
            throw new PackageManagerException("Static shared libs cannot declare activities");
        }
        if (!pkg.getServices().isEmpty()) {
            throw new PackageManagerException("Static shared libs cannot declare services");
        }
        if (!pkg.getProviders().isEmpty()) {
            throw new PackageManagerException("Static shared libs cannot declare content providers");
        }
        if (!pkg.getReceivers().isEmpty()) {
            throw new PackageManagerException("Static shared libs cannot declare broadcast receivers");
        }
        if (!pkg.getPermissionGroups().isEmpty()) {
            throw new PackageManagerException("Static shared libs cannot declare permission groups");
        }
        if (!pkg.getAttributions().isEmpty()) {
            throw new PackageManagerException("Static shared libs cannot declare features");
        }
        if (!pkg.getPermissions().isEmpty()) {
            throw new PackageManagerException("Static shared libs cannot declare permissions");
        }
        if (!pkg.getProtectedBroadcasts().isEmpty()) {
            throw new PackageManagerException("Static shared libs cannot declare protected broadcasts");
        }
        if (pkg.getOverlayTarget() != null) {
            throw new PackageManagerException("Static shared libs cannot be overlay targets");
        }
    }

    public static void assertProcessesAreValid(AndroidPackage pkg) throws PackageManagerException {
        Map<String, ParsedProcess> procs = pkg.getProcesses();
        if (!procs.isEmpty()) {
            if (!procs.containsKey(pkg.getProcessName())) {
                throw new PackageManagerException(-122, "Can't install because application tag's process attribute " + pkg.getProcessName() + " (in package " + pkg.getPackageName() + ") is not included in the <processes> list");
            }
            assertPackageProcesses(pkg, pkg.getActivities(), procs, HostingRecord.HOSTING_TYPE_ACTIVITY);
            assertPackageProcesses(pkg, pkg.getServices(), procs, HostingRecord.HOSTING_TYPE_SERVICE);
            assertPackageProcesses(pkg, pkg.getReceivers(), procs, ParsingPackageUtils.TAG_RECEIVER);
            assertPackageProcesses(pkg, pkg.getProviders(), procs, "provider");
        }
    }

    private static <T extends ParsedMainComponent> void assertPackageProcesses(AndroidPackage pkg, List<T> components, Map<String, ParsedProcess> procs, String compName) throws PackageManagerException {
        if (components == null) {
            return;
        }
        for (int i = components.size() - 1; i >= 0; i--) {
            ParsedMainComponent component = components.get(i);
            if (!procs.containsKey(component.getProcessName())) {
                throw new PackageManagerException(-122, "Can't install because " + compName + " " + component.getClassName() + "'s process attribute " + component.getProcessName() + " (in package " + pkg.getPackageName() + ") is not included in the <processes> list");
            }
        }
    }

    public static void assertMinSignatureSchemeIsValid(AndroidPackage pkg, int parseFlags) throws PackageManagerException {
        int minSignatureSchemeVersion = ApkSignatureVerifier.getMinimumSignatureSchemeVersionForTargetSdk(pkg.getTargetSdkVersion());
        if (pkg.getSigningDetails().getSignatureSchemeVersion() < minSignatureSchemeVersion) {
            throw new PackageManagerException(GnssNative.GeofenceCallbacks.GEOFENCE_STATUS_ERROR_INVALID_TRANSITION, "No signature found in package of version " + minSignatureSchemeVersion + " or newer for package " + pkg.getPackageName());
        }
    }

    public static String getRealPackageName(AndroidPackage pkg, String renamedPkgName) {
        if (isPackageRenamed(pkg, renamedPkgName)) {
            return AndroidPackageUtils.getRealPackageOrNull(pkg);
        }
        return null;
    }

    public static boolean isPackageRenamed(AndroidPackage pkg, String renamedPkgName) {
        return pkg.getOriginalPackages().contains(renamedPkgName);
    }

    public static void ensurePackageRenamed(ParsedPackage parsedPackage, String renamedPackageName) {
        if (!parsedPackage.getOriginalPackages().contains(renamedPackageName) || parsedPackage.getPackageName().equals(renamedPackageName)) {
            return;
        }
        parsedPackage.setPackageName(renamedPackageName);
    }

    public static boolean apkHasCode(String fileName) {
        StrictJarFile jarFile = null;
        try {
            jarFile = new StrictJarFile(fileName, false, false);
            boolean z = jarFile.findEntry("classes.dex") != null;
            try {
                jarFile.close();
            } catch (IOException e) {
            }
            return z;
        } catch (IOException e2) {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e3) {
                }
            }
            return false;
        } catch (Throwable th) {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
    }

    public static void configurePackageComponents(AndroidPackage pkg) {
        ArrayMap<String, Boolean> componentsEnabledStates = SystemConfig.getInstance().getComponentsEnabledStates(pkg.getPackageName());
        if (componentsEnabledStates == null) {
            return;
        }
        for (int i = ArrayUtils.size(pkg.getActivities()) - 1; i >= 0; i--) {
            ParsedActivity component = pkg.getActivities().get(i);
            Boolean enabled = componentsEnabledStates.get(component.getName());
            if (enabled != null) {
                ComponentMutateUtils.setEnabled(component, enabled.booleanValue());
            }
        }
        for (int i2 = ArrayUtils.size(pkg.getReceivers()) - 1; i2 >= 0; i2--) {
            ParsedActivity component2 = pkg.getReceivers().get(i2);
            Boolean enabled2 = componentsEnabledStates.get(component2.getName());
            if (enabled2 != null) {
                ComponentMutateUtils.setEnabled(component2, enabled2.booleanValue());
            }
        }
        for (int i3 = ArrayUtils.size(pkg.getProviders()) - 1; i3 >= 0; i3--) {
            ParsedProvider component3 = pkg.getProviders().get(i3);
            Boolean enabled3 = componentsEnabledStates.get(component3.getName());
            if (enabled3 != null) {
                ComponentMutateUtils.setEnabled(component3, enabled3.booleanValue());
            }
        }
        for (int i4 = ArrayUtils.size(pkg.getServices()) - 1; i4 >= 0; i4--) {
            ParsedService component4 = pkg.getServices().get(i4);
            Boolean enabled4 = componentsEnabledStates.get(component4.getName());
            if (enabled4 != null) {
                ComponentMutateUtils.setEnabled(component4, enabled4.booleanValue());
            }
        }
    }

    public static int getVendorPartitionVersion() {
        String version = SystemProperties.get("ro.vndk.version");
        if (!version.isEmpty()) {
            try {
                return Integer.parseInt(version);
            } catch (NumberFormatException e) {
                if (ArrayUtils.contains(Build.VERSION.ACTIVE_CODENAMES, version)) {
                    return 10000;
                }
                return 28;
            }
        }
        return 28;
    }

    public static void applyPolicy(ParsedPackage parsedPackage, int scanFlags, AndroidPackage platformPkg, boolean isUpdatedSystemApp) {
        boolean z = true;
        if ((65536 & scanFlags) != 0) {
            parsedPackage.setSystem(true);
            if (parsedPackage.isDirectBootAware()) {
                parsedPackage.setAllComponentsDirectBootAware(true);
            }
            if (PackageManagerServiceUtils.compressedFileExists(parsedPackage.getPath())) {
                parsedPackage.setStub(true);
            }
        } else {
            parsedPackage.clearProtectedBroadcasts().setCoreApp(false).setPersistent(false).setDefaultToDeviceProtectedStorage(false).setDirectBootAware(false).capPermissionPriorities();
        }
        if ((scanFlags & 131072) == 0) {
            parsedPackage.markNotActivitiesAsNotExportedIfSingleUser();
        }
        parsedPackage.setPrivileged((131072 & scanFlags) != 0).setOem((262144 & scanFlags) != 0).setVendor((524288 & scanFlags) != 0).setProduct((1048576 & scanFlags) != 0).setSystemExt((2097152 & scanFlags) != 0).setOdm((4194304 & scanFlags) != 0);
        if (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(parsedPackage.getPackageName()) && (platformPkg == null || PackageManagerServiceUtils.compareSignatures(platformPkg.getSigningDetails().getSignatures(), parsedPackage.getSigningDetails().getSignatures()) != 0)) {
            z = false;
        }
        parsedPackage.setSignedWithPlatformKey(z);
        if (!parsedPackage.isSystem()) {
            parsedPackage.clearOriginalPackages().clearAdoptPermissions();
        }
        PackageBackwardCompatibility.modifySharedLibraries(parsedPackage, isUpdatedSystemApp);
    }

    public static List<String> applyAdjustedAbiToSharedUser(SharedUserSetting sharedUserSetting, ParsedPackage scannedPackage, String adjustedAbi) {
        if (scannedPackage != null) {
            scannedPackage.setPrimaryCpuAbi(adjustedAbi);
        }
        List<String> changedAbiCodePath = null;
        WatchedArraySet<PackageSetting> sharedUserPackageSettings = sharedUserSetting.getPackageSettings();
        for (int i = 0; i < sharedUserPackageSettings.size(); i++) {
            PackageSetting ps = sharedUserPackageSettings.valueAt(i);
            if ((scannedPackage == null || !scannedPackage.getPackageName().equals(ps.getPackageName())) && ps.getPrimaryCpuAbi() == null) {
                ps.setPrimaryCpuAbi(adjustedAbi);
                ps.onChanged();
                if (ps.getPkg() != null && !TextUtils.equals(adjustedAbi, AndroidPackageUtils.getRawPrimaryCpuAbi(ps.getPkg()))) {
                    if (PackageManagerService.DEBUG_ABI_SELECTION) {
                        Slog.i("PackageManager", "Adjusting ABI for " + ps.getPackageName() + " to " + adjustedAbi + " (scannedPackage=" + (scannedPackage != null ? scannedPackage : "null") + ")");
                    }
                    if (changedAbiCodePath == null) {
                        changedAbiCodePath = new ArrayList<>();
                    }
                    changedAbiCodePath.add(ps.getPathString());
                }
            }
        }
        return changedAbiCodePath;
    }

    public static void collectCertificatesLI(PackageSetting ps, ParsedPackage parsedPackage, Settings.VersionInfo settingsVersionForPackage, boolean forceCollect, boolean skipVerify, boolean isPreNMR1Upgrade) throws PackageManagerException {
        long lastModifiedTime;
        if (isPreNMR1Upgrade) {
            lastModifiedTime = new File(parsedPackage.getPath()).lastModified();
        } else {
            lastModifiedTime = PackageManagerServiceUtils.getLastModifiedTime(parsedPackage);
        }
        if (ps != null && !forceCollect && ps.getPathString().equals(parsedPackage.getPath()) && ps.getLastModifiedTime() == lastModifiedTime && !ReconcilePackageUtils.isCompatSignatureUpdateNeeded(settingsVersionForPackage) && !ReconcilePackageUtils.isRecoverSignatureUpdateNeeded(settingsVersionForPackage)) {
            if (ps.getSigningDetails().getSignatures() == null || ps.getSigningDetails().getSignatures().length == 0 || ps.getSigningDetails().getSignatureSchemeVersion() == 0) {
                Slog.w("PackageManager", "PackageSetting for " + ps.getPackageName() + " is missing signatures.  Collecting certs again to recover them.");
            } else {
                parsedPackage.setSigningDetails(new SigningDetails(ps.getSigningDetails()));
                return;
            }
        } else {
            Slog.i("PackageManager", parsedPackage.getPath() + " changed; collecting certs" + (forceCollect ? " (forced)" : ""));
        }
        try {
            Trace.traceBegin(262144L, "collectCertificates");
            ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
            ParseResult<SigningDetails> result = ParsingPackageUtils.getSigningDetails(input, parsedPackage, skipVerify);
            if (result.isError()) {
                throw new PackageManagerException(result.getErrorCode(), result.getErrorMessage(), result.getException());
            }
            parsedPackage.setSigningDetails((SigningDetails) result.getResult());
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    public static void setInstantAppForUser(PackageManagerServiceInjector injector, PackageSetting pkgSetting, int userId, boolean instantApp, boolean fullApp) {
        int[] userIds;
        if (!instantApp && !fullApp) {
            return;
        }
        if (userId != -1) {
            if (instantApp && !pkgSetting.getInstantApp(userId)) {
                pkgSetting.setInstantApp(true, userId);
                return;
            } else if (fullApp && pkgSetting.getInstantApp(userId)) {
                pkgSetting.setInstantApp(false, userId);
                return;
            } else {
                return;
            }
        }
        for (int currentUserId : injector.getUserManagerInternal().getUserIds()) {
            if (instantApp && !pkgSetting.getInstantApp(currentUserId)) {
                pkgSetting.setInstantApp(true, currentUserId);
            } else if (fullApp && pkgSetting.getInstantApp(currentUserId)) {
                pkgSetting.setInstantApp(false, currentUserId);
            }
        }
    }

    public static File getAppLib32InstallDir() {
        return new File(Environment.getDataDirectory(), "app-lib");
    }
}
