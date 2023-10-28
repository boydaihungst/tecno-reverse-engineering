package com.android.server.pm;

import android.content.pm.PackageManagerInternal;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.SparseArrayMap;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.mutate.PackageStateMutator;
import com.android.server.pm.pkg.mutate.PackageUserStateWrite;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class UserSystemPackageInstaller {
    private static final boolean DEBUG = false;
    static final String PACKAGE_WHITELIST_MODE_PROP = "persist.debug.user.package_whitelist_mode";
    private static final String TAG = UserSystemPackageInstaller.class.getSimpleName();
    static final int USER_TYPE_PACKAGE_WHITELIST_MODE_DEVICE_DEFAULT = -1;
    public static final int USER_TYPE_PACKAGE_WHITELIST_MODE_DISABLE = 0;
    public static final int USER_TYPE_PACKAGE_WHITELIST_MODE_ENFORCE = 1;
    public static final int USER_TYPE_PACKAGE_WHITELIST_MODE_IGNORE_OTA = 16;
    public static final int USER_TYPE_PACKAGE_WHITELIST_MODE_IMPLICIT_WHITELIST = 4;
    public static final int USER_TYPE_PACKAGE_WHITELIST_MODE_IMPLICIT_WHITELIST_SYSTEM = 8;
    public static final int USER_TYPE_PACKAGE_WHITELIST_MODE_LOG = 2;
    static final int USER_TYPE_PACKAGE_WHITELIST_MODE_NONE = -1000;
    private final UserManagerService mUm;
    private final String[] mUserTypes;
    private final ArrayMap<String, Long> mWhitelistedPackagesForUserTypes;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface PackageWhitelistMode {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserSystemPackageInstaller(UserManagerService um, ArrayMap<String, UserTypeDetails> userTypes) {
        this.mUm = um;
        String[] andSortKeysFromMap = getAndSortKeysFromMap(userTypes);
        this.mUserTypes = andSortKeysFromMap;
        if (andSortKeysFromMap.length > 64) {
            throw new IllegalArgumentException("Device contains " + userTypes.size() + " user types. However, UserSystemPackageInstaller does not work if there are more than 64 user types.");
        }
        this.mWhitelistedPackagesForUserTypes = determineWhitelistedPackagesForUserTypes(SystemConfig.getInstance());
    }

    UserSystemPackageInstaller(UserManagerService ums, ArrayMap<String, Long> whitelist, String[] sortedUserTypes) {
        this.mUm = ums;
        this.mUserTypes = sortedUserTypes;
        this.mWhitelistedPackagesForUserTypes = whitelist;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean installWhitelistedSystemPackages(final boolean isFirstBoot, boolean isUpgrade, final ArraySet<String> preExistingPackages) {
        int[] userIds;
        boolean install;
        int mode = getWhitelistMode();
        checkWhitelistedSystemPackages(mode);
        final boolean isConsideredUpgrade = isUpgrade && !isIgnoreOtaMode(mode);
        if (!isConsideredUpgrade && !isFirstBoot) {
            return false;
        }
        if (isFirstBoot && !isEnforceMode(mode)) {
            return false;
        }
        Slog.i(TAG, "Reviewing whitelisted packages due to " + (isFirstBoot ? "[firstBoot]" : "") + (isConsideredUpgrade ? "[upgrade]" : ""));
        PackageManagerInternal pmInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        final SparseArrayMap<String, Boolean> changesToCommit = new SparseArrayMap<>();
        for (final int userId : this.mUm.getUserIds()) {
            Set<String> userWhitelist = getInstallablePackagesForUserId(userId);
            if (userWhitelist == null) {
                pmInt.forEachPackageState(new Consumer() { // from class: com.android.server.pm.UserSystemPackageInstaller$$ExternalSyntheticLambda2
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        UserSystemPackageInstaller.lambda$installWhitelistedSystemPackages$0(userId, isFirstBoot, isConsideredUpgrade, preExistingPackages, changesToCommit, (PackageStateInternal) obj);
                    }
                });
            } else {
                for (String packageName : userWhitelist) {
                    PackageStateInternal packageState = pmInt.getPackageStateInternal(packageName);
                    if (packageState.getPkg() != null && packageState.getUserStateOrDefault(userId).isInstalled() != (!packageState.getTransientState().isHiddenUntilInstalled()) && shouldChangeInstallationState(packageState, install, userId, isFirstBoot, isConsideredUpgrade, preExistingPackages)) {
                        changesToCommit.add(userId, packageState.getPackageName(), Boolean.valueOf(install));
                    }
                }
            }
        }
        pmInt.commitPackageStateMutation(null, new Consumer() { // from class: com.android.server.pm.UserSystemPackageInstaller$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                UserSystemPackageInstaller.lambda$installWhitelistedSystemPackages$1(changesToCommit, (PackageStateMutator) obj);
            }
        });
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$installWhitelistedSystemPackages$0(int userId, boolean isFirstBoot, boolean isConsideredUpgrade, ArraySet preExistingPackages, SparseArrayMap changesToCommit, PackageStateInternal packageState) {
        boolean install;
        if (packageState.getPkg() != null && packageState.getUserStateOrDefault(userId).isInstalled() != (!packageState.getTransientState().isHiddenUntilInstalled()) && shouldChangeInstallationState(packageState, install, userId, isFirstBoot, isConsideredUpgrade, preExistingPackages)) {
            changesToCommit.add(userId, packageState.getPackageName(), Boolean.valueOf(install));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$installWhitelistedSystemPackages$1(SparseArrayMap changesToCommit, PackageStateMutator packageStateMutator) {
        int i;
        for (int userIndex = 0; userIndex < changesToCommit.numMaps(); userIndex++) {
            int userId = changesToCommit.keyAt(userIndex);
            int packagesSize = changesToCommit.numElementsForKey(userId);
            for (int packageIndex = 0; packageIndex < packagesSize; packageIndex++) {
                String packageName = (String) changesToCommit.keyAt(userIndex, packageIndex);
                boolean installed = ((Boolean) changesToCommit.valueAt(userIndex, packageIndex)).booleanValue();
                PackageUserStateWrite installed2 = packageStateMutator.forPackage(packageName).userState(userId).setInstalled(installed);
                if (installed) {
                    i = 0;
                } else {
                    i = 1;
                }
                installed2.setUninstallReason(i);
                Slog.i(TAG + "CommitDebug", (installed ? "Installed " : "Uninstalled ") + packageName + " for user " + userId);
            }
        }
    }

    private static boolean shouldChangeInstallationState(PackageStateInternal packageState, boolean install, int userId, boolean isFirstBoot, boolean isUpgrade, ArraySet<String> preOtaPkgs) {
        return install ? packageState.getUserStateOrDefault(userId).getUninstallReason() == 1 : isFirstBoot || (isUpgrade && !preOtaPkgs.contains(packageState.getPackageName()));
    }

    private void checkWhitelistedSystemPackages(int mode) {
        if (!isLogMode(mode) && !isEnforceMode(mode)) {
            return;
        }
        String str = TAG;
        Slog.v(str, "Checking that all system packages are whitelisted.");
        List<String> warnings = getPackagesWhitelistWarnings();
        int numberWarnings = warnings.size();
        if (numberWarnings != 0) {
            Slog.w(str, "checkWhitelistedSystemPackages(mode=" + modeToString(mode) + ") has " + numberWarnings + " warnings:");
            for (int i = 0; i < numberWarnings; i++) {
                Slog.w(TAG, warnings.get(i));
            }
        } else {
            Slog.v(str, "checkWhitelistedSystemPackages(mode=" + modeToString(mode) + ") has no warnings");
        }
        if (isImplicitWhitelistMode(mode) && !isLogMode(mode)) {
            return;
        }
        List<String> errors = getPackagesWhitelistErrors(mode);
        int numberErrors = errors.size();
        if (numberErrors != 0) {
            Slog.e(TAG, "checkWhitelistedSystemPackages(mode=" + modeToString(mode) + ") has " + numberErrors + " errors:");
            boolean doWtf = !isImplicitWhitelistMode(mode);
            for (int i2 = 0; i2 < numberErrors; i2++) {
                String msg = errors.get(i2);
                if (doWtf) {
                    Slog.wtf(TAG, msg);
                } else {
                    Slog.e(TAG, msg);
                }
            }
            return;
        }
        Slog.v(TAG, "checkWhitelistedSystemPackages(mode=" + modeToString(mode) + ") has no errors");
    }

    private List<String> getPackagesWhitelistWarnings() {
        Set<String> allWhitelistedPackages = getWhitelistedSystemPackages();
        List<String> warnings = new ArrayList<>();
        PackageManagerInternal pmInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        for (String pkgName : allWhitelistedPackages) {
            AndroidPackage pkg = pmInt.getPackage(pkgName);
            if (pkg == null) {
                warnings.add(String.format("%s is allowlisted but not present.", pkgName));
            } else if (!pkg.isSystem()) {
                warnings.add(String.format("%s is allowlisted and present but not a system package.", pkgName));
            } else if (shouldUseOverlayTargetName(pkg)) {
                warnings.add(String.format("%s is allowlisted unnecessarily since it's a static overlay.", pkgName));
            }
        }
        return warnings;
    }

    private List<String> getPackagesWhitelistErrors(int mode) {
        if ((!isEnforceMode(mode) || isImplicitWhitelistMode(mode)) && !isLogMode(mode)) {
            return Collections.emptyList();
        }
        final List<String> errors = new ArrayList<>();
        final Set<String> allWhitelistedPackages = getWhitelistedSystemPackages();
        final PackageManagerInternal pmInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        pmInt.forEachPackage(new Consumer() { // from class: com.android.server.pm.UserSystemPackageInstaller$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                UserSystemPackageInstaller.lambda$getPackagesWhitelistErrors$2(allWhitelistedPackages, pmInt, errors, (AndroidPackage) obj);
            }
        });
        return errors;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$getPackagesWhitelistErrors$2(Set allWhitelistedPackages, PackageManagerInternal pmInt, List errors, AndroidPackage pkg) {
        if (pkg.isSystem()) {
            String pkgName = pkg.getManifestPackageName();
            if (allWhitelistedPackages.contains(pkgName) || shouldUseOverlayTargetName(pmInt.getPackage(pkgName))) {
                return;
            }
            errors.add(String.format("System package %s is not whitelisted using 'install-in-user-type' in SystemConfig for any user types!", pkgName));
        }
    }

    boolean isEnforceMode() {
        return isEnforceMode(getWhitelistMode());
    }

    boolean isIgnoreOtaMode() {
        return isIgnoreOtaMode(getWhitelistMode());
    }

    boolean isLogMode() {
        return isLogMode(getWhitelistMode());
    }

    boolean isImplicitWhitelistMode() {
        return isImplicitWhitelistMode(getWhitelistMode());
    }

    boolean isImplicitWhitelistSystemMode() {
        return isImplicitWhitelistSystemMode(getWhitelistMode());
    }

    private static boolean shouldUseOverlayTargetName(AndroidPackage pkg) {
        return pkg.isOverlayIsStatic();
    }

    private static boolean isEnforceMode(int whitelistMode) {
        return (whitelistMode & 1) != 0;
    }

    private static boolean isIgnoreOtaMode(int whitelistMode) {
        return (whitelistMode & 16) != 0;
    }

    private static boolean isLogMode(int whitelistMode) {
        return (whitelistMode & 2) != 0;
    }

    private static boolean isImplicitWhitelistMode(int whitelistMode) {
        return (whitelistMode & 4) != 0;
    }

    private static boolean isImplicitWhitelistSystemMode(int whitelistMode) {
        return (whitelistMode & 8) != 0;
    }

    private int getWhitelistMode() {
        int runtimeMode = SystemProperties.getInt(PACKAGE_WHITELIST_MODE_PROP, -1);
        if (runtimeMode != -1) {
            return runtimeMode;
        }
        return getDeviceDefaultWhitelistMode();
    }

    private int getDeviceDefaultWhitelistMode() {
        return Resources.getSystem().getInteger(17694966);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String modeToString(int mode) {
        switch (mode) {
            case -1000:
                return "NONE";
            case -1:
                return "DEVICE_DEFAULT";
            default:
                return DebugUtils.flagsToString(UserSystemPackageInstaller.class, "USER_TYPE_PACKAGE_WHITELIST_MODE_", mode);
        }
    }

    private Set<String> getInstallablePackagesForUserId(int userId) {
        return getInstallablePackagesForUserType(this.mUm.getUserInfo(userId).userType);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<String> getInstallablePackagesForUserType(String userType) {
        int mode = getWhitelistMode();
        if (!isEnforceMode(mode)) {
            return null;
        }
        final boolean implicitlyWhitelist = isImplicitWhitelistMode(mode) || (isImplicitWhitelistSystemMode(mode) && this.mUm.isUserTypeSubtypeOfSystem(userType));
        final Set<String> whitelistedPackages = getWhitelistedPackagesForUserType(userType);
        final Set<String> installPackages = new ArraySet<>();
        PackageManagerInternal pmInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        pmInt.forEachPackage(new Consumer() { // from class: com.android.server.pm.UserSystemPackageInstaller$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                UserSystemPackageInstaller.this.m5748x49e47b7c(whitelistedPackages, implicitlyWhitelist, installPackages, (AndroidPackage) obj);
            }
        });
        return installPackages;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getInstallablePackagesForUserType$3$com-android-server-pm-UserSystemPackageInstaller  reason: not valid java name */
    public /* synthetic */ void m5748x49e47b7c(Set whitelistedPackages, boolean implicitlyWhitelist, Set installPackages, AndroidPackage pkg) {
        if (pkg.isSystem() && shouldInstallPackage(pkg, this.mWhitelistedPackagesForUserTypes, whitelistedPackages, implicitlyWhitelist)) {
            installPackages.add(pkg.getPackageName());
        }
    }

    static boolean shouldInstallPackage(AndroidPackage sysPkg, ArrayMap<String, Long> userTypeWhitelist, Set<String> userWhitelist, boolean implicitlyWhitelist) {
        String pkgName = shouldUseOverlayTargetName(sysPkg) ? sysPkg.getOverlayTarget() : sysPkg.getManifestPackageName();
        return (implicitlyWhitelist && !userTypeWhitelist.containsKey(pkgName)) || userWhitelist.contains(pkgName);
    }

    Set<String> getWhitelistedPackagesForUserType(String userType) {
        long userTypeMask = getUserTypeMask(userType);
        Set<String> installablePkgs = new ArraySet<>(this.mWhitelistedPackagesForUserTypes.size());
        for (int i = 0; i < this.mWhitelistedPackagesForUserTypes.size(); i++) {
            String pkgName = this.mWhitelistedPackagesForUserTypes.keyAt(i);
            long whitelistedUserTypes = this.mWhitelistedPackagesForUserTypes.valueAt(i).longValue();
            if ((userTypeMask & whitelistedUserTypes) != 0) {
                installablePkgs.add(pkgName);
            }
        }
        return installablePkgs;
    }

    private Set<String> getWhitelistedSystemPackages() {
        return this.mWhitelistedPackagesForUserTypes.keySet();
    }

    ArrayMap<String, Long> determineWhitelistedPackagesForUserTypes(SystemConfig sysConfig) {
        Map<String, Long> baseTypeBitSets = getBaseTypeBitSets();
        ArrayMap<String, Set<String>> whitelist = sysConfig.getAndClearPackageToUserTypeWhitelist();
        ArrayMap<String, Long> result = new ArrayMap<>(whitelist.size() + 1);
        for (int i = 0; i < whitelist.size(); i++) {
            String pkgName = whitelist.keyAt(i).intern();
            long typesBitSet = getTypesBitSet(whitelist.valueAt(i), baseTypeBitSets);
            if (typesBitSet != 0) {
                result.put(pkgName, Long.valueOf(typesBitSet));
            }
        }
        ArrayMap<String, Set<String>> blacklist = sysConfig.getAndClearPackageToUserTypeBlacklist();
        for (int i2 = 0; i2 < blacklist.size(); i2++) {
            String pkgName2 = blacklist.keyAt(i2).intern();
            long nonTypesBitSet = getTypesBitSet(blacklist.valueAt(i2), baseTypeBitSets);
            Long typesBitSet2 = result.get(pkgName2);
            if (typesBitSet2 != null) {
                result.put(pkgName2, Long.valueOf(typesBitSet2.longValue() & (~nonTypesBitSet)));
            } else if (nonTypesBitSet != 0) {
                result.put(pkgName2, 0L);
            }
        }
        result.put(PackageManagerService.PLATFORM_PACKAGE_NAME, -1L);
        return result;
    }

    long getUserTypeMask(String userType) {
        int userTypeIndex = Arrays.binarySearch(this.mUserTypes, userType);
        if (userTypeIndex >= 0) {
            long userTypeMask = 1 << userTypeIndex;
            return userTypeMask;
        }
        return 0L;
    }

    private Map<String, Long> getBaseTypeBitSets() {
        long typesBitSetFull = 0;
        long typesBitSetSystem = 0;
        long typesBitSetProfile = 0;
        int idx = 0;
        while (true) {
            String[] strArr = this.mUserTypes;
            if (idx < strArr.length) {
                if (this.mUm.isUserTypeSubtypeOfFull(strArr[idx])) {
                    typesBitSetFull |= 1 << idx;
                }
                if (this.mUm.isUserTypeSubtypeOfSystem(this.mUserTypes[idx])) {
                    typesBitSetSystem |= 1 << idx;
                }
                if (this.mUm.isUserTypeSubtypeOfProfile(this.mUserTypes[idx])) {
                    typesBitSetProfile |= 1 << idx;
                }
                idx++;
            } else {
                Map<String, Long> result = new ArrayMap<>(3);
                result.put("FULL", Long.valueOf(typesBitSetFull));
                result.put("SYSTEM", Long.valueOf(typesBitSetSystem));
                result.put("PROFILE", Long.valueOf(typesBitSetProfile));
                return result;
            }
        }
    }

    private long getTypesBitSet(Iterable<String> userTypes, Map<String, Long> baseTypeBitSets) {
        long resultBitSet = 0;
        for (String type : userTypes) {
            Long baseTypeBitSet = baseTypeBitSets.get(type);
            if (baseTypeBitSet != null) {
                resultBitSet |= baseTypeBitSet.longValue();
            } else {
                long userTypeBitSet = getUserTypeMask(type);
                if (userTypeBitSet != 0) {
                    resultBitSet |= userTypeBitSet;
                } else {
                    Slog.w(TAG, "SystemConfig contained an invalid user type: " + type);
                }
            }
        }
        return resultBitSet;
    }

    private static String[] getAndSortKeysFromMap(ArrayMap<String, ?> map) {
        String[] userTypeList = new String[map.size()];
        for (int i = 0; i < map.size(); i++) {
            userTypeList[i] = map.keyAt(i);
        }
        Arrays.sort(userTypeList);
        return userTypeList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(IndentingPrintWriter pw) {
        int mode = getWhitelistMode();
        pw.println("Whitelisted packages per user type");
        pw.increaseIndent();
        pw.print("Mode: ");
        pw.print(mode);
        pw.print(isEnforceMode(mode) ? " (enforced)" : "");
        pw.print(isLogMode(mode) ? " (logged)" : "");
        pw.print(isImplicitWhitelistMode(mode) ? " (implicit)" : "");
        pw.print(isIgnoreOtaMode(mode) ? " (ignore OTAs)" : "");
        pw.println();
        pw.decreaseIndent();
        pw.increaseIndent();
        pw.println("Legend");
        pw.increaseIndent();
        for (int idx = 0; idx < this.mUserTypes.length; idx++) {
            pw.println(idx + " -> " + this.mUserTypes[idx]);
        }
        pw.decreaseIndent();
        pw.decreaseIndent();
        pw.increaseIndent();
        int size = this.mWhitelistedPackagesForUserTypes.size();
        if (size == 0) {
            pw.println("No packages");
            pw.decreaseIndent();
            return;
        }
        pw.print(size);
        pw.println(" packages:");
        pw.increaseIndent();
        for (int pkgIdx = 0; pkgIdx < size; pkgIdx++) {
            String pkgName = this.mWhitelistedPackagesForUserTypes.keyAt(pkgIdx);
            pw.print(pkgName);
            pw.print(": ");
            long userTypesBitSet = this.mWhitelistedPackagesForUserTypes.valueAt(pkgIdx).longValue();
            for (int idx2 = 0; idx2 < this.mUserTypes.length; idx2++) {
                if (((1 << idx2) & userTypesBitSet) != 0) {
                    pw.print(idx2);
                    pw.print(" ");
                }
            }
            pw.println();
        }
        pw.decreaseIndent();
        pw.decreaseIndent();
        pw.increaseIndent();
        dumpPackageWhitelistProblems(pw, mode, true, false);
        pw.decreaseIndent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpPackageWhitelistProblems(IndentingPrintWriter pw, int mode, boolean verbose, boolean criticalOnly) {
        if (mode == -1000) {
            mode = getWhitelistMode();
        } else if (mode == -1) {
            mode = getDeviceDefaultWhitelistMode();
        }
        if (criticalOnly) {
            mode &= -3;
        }
        Slog.v(TAG, "dumpPackageWhitelistProblems(): using mode " + modeToString(mode));
        List<String> errors = getPackagesWhitelistErrors(mode);
        showIssues(pw, verbose, errors, "errors");
        if (criticalOnly) {
            return;
        }
        List<String> warnings = getPackagesWhitelistWarnings();
        showIssues(pw, verbose, warnings, "warnings");
    }

    private static void showIssues(IndentingPrintWriter pw, boolean verbose, List<String> issues, String issueType) {
        int size = issues.size();
        if (size == 0) {
            if (verbose) {
                pw.print("No ");
                pw.println(issueType);
                return;
            }
            return;
        }
        if (verbose) {
            pw.print(size);
            pw.print(' ');
            pw.println(issueType);
            pw.increaseIndent();
        }
        for (int i = 0; i < size; i++) {
            pw.println(issues.get(i));
        }
        if (verbose) {
            pw.decreaseIndent();
        }
    }
}
