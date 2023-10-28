package com.android.server.pm;

import android.content.pm.SharedLibraryInfo;
import android.content.pm.Signature;
import android.content.pm.SigningDetails;
import android.content.pm.VersionedPackage;
import android.os.storage.StorageManager;
import android.util.ArraySet;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ArrayUtils;
import com.android.server.SystemConfig;
import com.android.server.compat.PlatformCompat;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.utils.Snappable;
import com.android.server.utils.SnapshotCache;
import com.android.server.utils.Watchable;
import com.android.server.utils.WatchableImpl;
import com.android.server.utils.Watched;
import com.android.server.utils.WatchedArrayMap;
import com.android.server.utils.WatchedLongSparseArray;
import com.android.server.utils.Watcher;
import defpackage.CompanionAppsPermissions;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import libcore.util.HexEncoding;
/* loaded from: classes2.dex */
public final class SharedLibrariesImpl implements SharedLibrariesRead, Watchable, Snappable {
    private static final boolean DEBUG_SHARED_LIBRARIES = false;
    private static final long ENFORCE_NATIVE_SHARED_LIBRARY_DEPENDENCIES = 142191088;
    private DeletePackageHelper mDeletePackageHelper;
    private final PackageManagerServiceInjector mInjector;
    private final Watcher mObserver;
    private final PackageManagerService mPm;
    @Watched
    private final WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> mSharedLibraries;
    private final SnapshotCache<WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>>> mSharedLibrariesSnapshot;
    private final SnapshotCache<SharedLibrariesImpl> mSnapshot;
    @Watched
    private final WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> mStaticLibsByDeclaringPackage;
    private final SnapshotCache<WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>>> mStaticLibsByDeclaringPackageSnapshot;
    private final WatchableImpl mWatchable;

    private SnapshotCache<SharedLibrariesImpl> makeCache() {
        return new SnapshotCache<SharedLibrariesImpl>(this, this) { // from class: com.android.server.pm.SharedLibrariesImpl.2
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public SharedLibrariesImpl createSnapshot() {
                SharedLibrariesImpl sharedLibrariesImpl = new SharedLibrariesImpl();
                sharedLibrariesImpl.mWatchable.seal();
                return sharedLibrariesImpl;
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SharedLibrariesImpl(PackageManagerService pm, PackageManagerServiceInjector injector) {
        this.mWatchable = new WatchableImpl();
        Watcher watcher = new Watcher() { // from class: com.android.server.pm.SharedLibrariesImpl.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                SharedLibrariesImpl.this.dispatchChange(what);
            }
        };
        this.mObserver = watcher;
        this.mPm = pm;
        this.mInjector = injector;
        WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> watchedArrayMap = new WatchedArrayMap<>();
        this.mSharedLibraries = watchedArrayMap;
        this.mSharedLibrariesSnapshot = new SnapshotCache.Auto(watchedArrayMap, watchedArrayMap, "SharedLibrariesImpl.mSharedLibraries");
        WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> watchedArrayMap2 = new WatchedArrayMap<>();
        this.mStaticLibsByDeclaringPackage = watchedArrayMap2;
        this.mStaticLibsByDeclaringPackageSnapshot = new SnapshotCache.Auto(watchedArrayMap2, watchedArrayMap2, "SharedLibrariesImpl.mStaticLibsByDeclaringPackage");
        registerObservers();
        Watchable.verifyWatchedAttributes(this, watcher);
        this.mSnapshot = makeCache();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDeletePackageHelper(DeletePackageHelper deletePackageHelper) {
        this.mDeletePackageHelper = deletePackageHelper;
    }

    private void registerObservers() {
        this.mSharedLibraries.registerObserver(this.mObserver);
        this.mStaticLibsByDeclaringPackage.registerObserver(this.mObserver);
    }

    private SharedLibrariesImpl(SharedLibrariesImpl source) {
        this.mWatchable = new WatchableImpl();
        this.mObserver = new Watcher() { // from class: com.android.server.pm.SharedLibrariesImpl.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                SharedLibrariesImpl.this.dispatchChange(what);
            }
        };
        this.mPm = source.mPm;
        this.mInjector = source.mInjector;
        this.mSharedLibraries = source.mSharedLibrariesSnapshot.snapshot();
        this.mSharedLibrariesSnapshot = new SnapshotCache.Sealed();
        this.mStaticLibsByDeclaringPackage = source.mStaticLibsByDeclaringPackageSnapshot.snapshot();
        this.mStaticLibsByDeclaringPackageSnapshot = new SnapshotCache.Sealed();
        this.mSnapshot = new SnapshotCache.Sealed();
    }

    @Override // com.android.server.utils.Watchable
    public void registerObserver(Watcher observer) {
        this.mWatchable.registerObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public void unregisterObserver(Watcher observer) {
        this.mWatchable.unregisterObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public boolean isRegisteredObserver(Watcher observer) {
        return this.mWatchable.isRegisteredObserver(observer);
    }

    @Override // com.android.server.utils.Watchable
    public void dispatchChange(Watchable what) {
        this.mWatchable.dispatchChange(what);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public SharedLibrariesRead snapshot() {
        return this.mSnapshot.snapshot();
    }

    @Override // com.android.server.pm.SharedLibrariesRead
    public WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> getAll() {
        return this.mSharedLibraries;
    }

    @Override // com.android.server.pm.SharedLibrariesRead
    public WatchedLongSparseArray<SharedLibraryInfo> getSharedLibraryInfos(String libName) {
        return this.mSharedLibraries.get(libName);
    }

    public WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> getSharedLibraries() {
        return this.mSharedLibraries;
    }

    @Override // com.android.server.pm.SharedLibrariesRead
    public SharedLibraryInfo getSharedLibraryInfo(String libName, long version) {
        WatchedLongSparseArray<SharedLibraryInfo> versionedLib = this.mSharedLibraries.get(libName);
        if (versionedLib == null) {
            return null;
        }
        return versionedLib.get(version);
    }

    @Override // com.android.server.pm.SharedLibrariesRead
    public WatchedLongSparseArray<SharedLibraryInfo> getStaticLibraryInfos(String declaringPackageName) {
        return this.mStaticLibsByDeclaringPackage.get(declaringPackageName);
    }

    private PackageStateInternal getLibraryPackage(Computer computer, SharedLibraryInfo libInfo) {
        VersionedPackage declaringPackage = libInfo.getDeclaringPackage();
        if (libInfo.isStatic()) {
            String internalPackageName = computer.resolveInternalPackageName(declaringPackage.getPackageName(), declaringPackage.getLongVersionCode());
            return computer.getPackageStateInternal(internalPackageName);
        } else if (libInfo.isSdk()) {
            return computer.getPackageStateInternal(declaringPackage.getPackageName());
        } else {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean pruneUnusedStaticSharedLibraries(Computer computer, long neededSpace, long maxCachePeriod) throws IOException {
        StorageManager storage;
        long now;
        StorageManager storage2 = (StorageManager) this.mInjector.getSystemService(StorageManager.class);
        File volume = storage2.findPathForUuid(StorageManager.UUID_PRIVATE_INTERNAL);
        ArrayList<VersionedPackage> packagesToDelete = new ArrayList<>();
        long now2 = System.currentTimeMillis();
        WatchedArrayMap<String, WatchedLongSparseArray<SharedLibraryInfo>> sharedLibraries = computer.getSharedLibraries();
        int libCount = sharedLibraries.size();
        int i = 0;
        while (i < libCount) {
            WatchedLongSparseArray<SharedLibraryInfo> versionedLib = sharedLibraries.valueAt(i);
            if (versionedLib != null) {
                int versionCount = versionedLib.size();
                int j = 0;
                while (j < versionCount) {
                    SharedLibraryInfo libInfo = versionedLib.valueAt(j);
                    PackageStateInternal ps = getLibraryPackage(computer, libInfo);
                    if (ps == null) {
                        storage = storage2;
                        now = now2;
                    } else if (now2 - ps.getLastUpdateTime() < maxCachePeriod) {
                        storage = storage2;
                        now = now2;
                    } else if (ps.getPkg().isSystem()) {
                        storage = storage2;
                        now = now2;
                    } else {
                        storage = storage2;
                        String packageName = ps.getPkg().getPackageName();
                        now = now2;
                        long now3 = libInfo.getDeclaringPackage().getLongVersionCode();
                        packagesToDelete.add(new VersionedPackage(packageName, now3));
                    }
                    j++;
                    storage2 = storage;
                    now2 = now;
                }
            }
            i++;
            storage2 = storage2;
            now2 = now2;
        }
        int packageCount = packagesToDelete.size();
        for (int i2 = 0; i2 < packageCount; i2++) {
            VersionedPackage pkgToDelete = packagesToDelete.get(i2);
            if (this.mDeletePackageHelper.deletePackageX(pkgToDelete.getPackageName(), pkgToDelete.getLongVersionCode(), 0, 2, true) == 1 && volume.getUsableSpace() >= neededSpace) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SharedLibraryInfo getLatestStaticSharedLibraVersionLPr(AndroidPackage pkg) {
        WatchedLongSparseArray<SharedLibraryInfo> versionedLib = this.mSharedLibraries.get(pkg.getStaticSharedLibName());
        if (versionedLib == null) {
            return null;
        }
        long previousLibVersion = -1;
        int versionCount = versionedLib.size();
        for (int i = 0; i < versionCount; i++) {
            long libVersion = versionedLib.keyAt(i);
            if (libVersion < pkg.getStaticSharedLibVersion()) {
                previousLibVersion = Math.max(previousLibVersion, libVersion);
            }
        }
        if (previousLibVersion < 0) {
            return null;
        }
        return versionedLib.get(previousLibVersion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSetting getStaticSharedLibLatestVersionSetting(ScanResult scanResult) {
        PackageSetting sharedLibPackage = null;
        synchronized (this.mPm.mLock) {
            SharedLibraryInfo latestSharedLibraVersionLPr = getLatestStaticSharedLibraVersionLPr(scanResult.mRequest.mParsedPackage);
            if (latestSharedLibraVersionLPr != null) {
                sharedLibPackage = this.mPm.mSettings.getPackageLPr(latestSharedLibraVersionLPr.getPackageName());
            }
        }
        return sharedLibPackage;
    }

    private void applyDefiningSharedLibraryUpdateLPr(AndroidPackage pkg, SharedLibraryInfo libInfo, BiConsumer<SharedLibraryInfo, SharedLibraryInfo> action) {
        if (AndroidPackageUtils.isLibrary(pkg)) {
            if (pkg.getSdkLibName() != null) {
                SharedLibraryInfo definedLibrary = getSharedLibraryInfo(pkg.getSdkLibName(), pkg.getSdkLibVersionMajor());
                if (definedLibrary != null) {
                    action.accept(definedLibrary, libInfo);
                }
            } else if (pkg.getStaticSharedLibName() != null) {
                SharedLibraryInfo definedLibrary2 = getSharedLibraryInfo(pkg.getStaticSharedLibName(), pkg.getStaticSharedLibVersion());
                if (definedLibrary2 != null) {
                    action.accept(definedLibrary2, libInfo);
                }
            } else {
                for (String libraryName : pkg.getLibraryNames()) {
                    SharedLibraryInfo definedLibrary3 = getSharedLibraryInfo(libraryName, -1L);
                    if (definedLibrary3 != null) {
                        action.accept(definedLibrary3, libInfo);
                    }
                }
            }
        }
    }

    private void addSharedLibraryLPr(AndroidPackage pkg, Set<String> usesLibraryFiles, SharedLibraryInfo libInfo, AndroidPackage changingLib, PackageSetting changingLibSetting) {
        if (libInfo.getPath() != null) {
            usesLibraryFiles.add(libInfo.getPath());
            return;
        }
        AndroidPackage pkgForCodePaths = this.mPm.mPackages.get(libInfo.getPackageName());
        PackageSetting pkgSetting = this.mPm.mSettings.getPackageLPr(libInfo.getPackageName());
        if (changingLib != null && changingLib.getPackageName().equals(libInfo.getPackageName()) && (pkgForCodePaths == null || pkgForCodePaths.getPackageName().equals(changingLib.getPackageName()))) {
            pkgForCodePaths = changingLib;
            pkgSetting = changingLibSetting;
        }
        if (pkgForCodePaths != null) {
            usesLibraryFiles.addAll(AndroidPackageUtils.getAllCodePaths(pkgForCodePaths));
            applyDefiningSharedLibraryUpdateLPr(pkg, libInfo, new BiConsumer() { // from class: com.android.server.pm.SharedLibrariesImpl$$ExternalSyntheticLambda1
                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    ((SharedLibraryInfo) obj).addDependency((SharedLibraryInfo) obj2);
                }
            });
            if (pkgSetting != null) {
                usesLibraryFiles.addAll(pkgSetting.getPkgState().getUsesLibraryFiles());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateSharedLibrariesLPw(AndroidPackage pkg, PackageSetting pkgSetting, AndroidPackage changingLib, PackageSetting changingLibSetting, Map<String, AndroidPackage> availablePackages) throws PackageManagerException {
        ArrayList<SharedLibraryInfo> sharedLibraryInfos = collectSharedLibraryInfos(pkg, availablePackages, null);
        executeSharedLibrariesUpdateLPw(pkg, pkgSetting, changingLib, changingLibSetting, sharedLibraryInfos, this.mPm.mUserManager.getUserIds());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void executeSharedLibrariesUpdateLPw(AndroidPackage pkg, PackageSetting pkgSetting, AndroidPackage changingLib, PackageSetting changingLibSetting, ArrayList<SharedLibraryInfo> usesLibraryInfos, int[] allUsers) {
        applyDefiningSharedLibraryUpdateLPr(pkg, null, new BiConsumer() { // from class: com.android.server.pm.SharedLibrariesImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                SharedLibraryInfo sharedLibraryInfo = (SharedLibraryInfo) obj2;
                ((SharedLibraryInfo) obj).clearDependencies();
            }
        });
        if (usesLibraryInfos != null) {
            pkgSetting.getPkgState().setUsesLibraryInfos(usesLibraryInfos);
            Set<String> usesLibraryFiles = new LinkedHashSet<>();
            Iterator<SharedLibraryInfo> it = usesLibraryInfos.iterator();
            while (it.hasNext()) {
                SharedLibraryInfo libInfo = it.next();
                addSharedLibraryLPr(pkg, usesLibraryFiles, libInfo, changingLib, changingLibSetting);
            }
            pkgSetting.setPkgStateLibraryFiles(usesLibraryFiles);
            int[] installedUsers = new int[allUsers.length];
            int installedUserCount = 0;
            for (int u = 0; u < allUsers.length; u++) {
                if (pkgSetting.getInstalled(allUsers[u])) {
                    installedUsers[installedUserCount] = allUsers[u];
                    installedUserCount++;
                }
            }
            Iterator<SharedLibraryInfo> it2 = usesLibraryInfos.iterator();
            while (it2.hasNext()) {
                SharedLibraryInfo sharedLibraryInfo = it2.next();
                if (sharedLibraryInfo.isStatic()) {
                    PackageSetting staticLibPkgSetting = this.mPm.getPackageSettingForMutation(sharedLibraryInfo.getPackageName());
                    if (staticLibPkgSetting == null) {
                        Slog.wtf("PackageManager", "Shared lib without setting: " + sharedLibraryInfo);
                    } else {
                        for (int u2 = 0; u2 < installedUserCount; u2++) {
                            staticLibPkgSetting.setInstalled(true, installedUsers[u2]);
                        }
                    }
                }
            }
            return;
        }
        pkgSetting.getPkgState().setUsesLibraryInfos(Collections.emptyList()).setUsesLibraryFiles(Collections.emptyList());
    }

    private static boolean hasString(List<String> list, List<String> which) {
        if (list == null || which == null) {
            return false;
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            for (int j = which.size() - 1; j >= 0; j--) {
                if (which.get(j).equals(list.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<AndroidPackage> updateAllSharedLibrariesLPw(AndroidPackage updatedPkg, PackageSetting updatedPkgSetting, Map<String, AndroidPackage> availablePackages) {
        List<Pair<AndroidPackage, PackageSetting>> needsUpdating;
        ArrayList<AndroidPackage> resultList;
        ArraySet<String> descendants;
        ArrayList<AndroidPackage> resultList2 = null;
        ArraySet<String> descendants2 = null;
        if (updatedPkg != null && updatedPkgSetting != null) {
            List<Pair<AndroidPackage, PackageSetting>> needsUpdating2 = new ArrayList<>(1);
            needsUpdating2.add(Pair.create(updatedPkg, updatedPkgSetting));
            needsUpdating = needsUpdating2;
        } else {
            needsUpdating = null;
        }
        do {
            Pair<AndroidPackage, PackageSetting> changingPkgPair = needsUpdating == null ? null : needsUpdating.remove(0);
            AndroidPackage changingPkg = changingPkgPair != null ? (AndroidPackage) changingPkgPair.first : null;
            PackageSetting changingPkgSetting = changingPkgPair != null ? (PackageSetting) changingPkgPair.second : null;
            for (int i = this.mPm.mPackages.size() - 1; i >= 0; i--) {
                AndroidPackage pkg = this.mPm.mPackages.valueAt(i);
                PackageSetting pkgSetting = this.mPm.mSettings.getPackageLPr(pkg.getPackageName());
                if (changingPkg == null || hasString(pkg.getUsesLibraries(), changingPkg.getLibraryNames()) || hasString(pkg.getUsesOptionalLibraries(), changingPkg.getLibraryNames()) || ArrayUtils.contains(pkg.getUsesStaticLibraries(), changingPkg.getStaticSharedLibName()) || ArrayUtils.contains(pkg.getUsesSdkLibraries(), changingPkg.getSdkLibName())) {
                    if (resultList2 != null) {
                        resultList = resultList2;
                    } else {
                        ArrayList<AndroidPackage> resultList3 = new ArrayList<>();
                        resultList = resultList3;
                    }
                    resultList.add(pkg);
                    if (changingPkg == null) {
                        descendants = descendants2;
                    } else {
                        if (descendants2 == null) {
                            descendants2 = new ArraySet<>();
                        }
                        if (!descendants2.contains(pkg.getPackageName())) {
                            descendants2.add(pkg.getPackageName());
                            needsUpdating.add(Pair.create(pkg, pkgSetting));
                        }
                        descendants = descendants2;
                    }
                    ArrayList<AndroidPackage> resultList4 = resultList;
                    try {
                        updateSharedLibrariesLPw(pkg, pkgSetting, changingPkg, changingPkgSetting, availablePackages);
                    } catch (PackageManagerException e) {
                        if (!pkg.isSystem() || pkgSetting.getPkgState().isUpdatedSystemApp()) {
                            int flags = pkgSetting.getPkgState().isUpdatedSystemApp() ? 1 : 0;
                            this.mDeletePackageHelper.deletePackageLIF(pkg.getPackageName(), null, true, this.mPm.mUserManager.getUserIds(), flags, null, true);
                        }
                        Slog.e("PackageManager", "updateAllSharedLibrariesLPw failed: " + e.getMessage());
                    }
                    descendants2 = descendants;
                    resultList2 = resultList4;
                }
            }
            if (needsUpdating == null) {
                break;
            }
        } while (needsUpdating.size() > 0);
        return resultList2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addBuiltInSharedLibraryLPw(SystemConfig.SharedLibraryEntry entry) {
        if (getSharedLibraryInfo(entry.name, -1L) != null) {
            return;
        }
        SharedLibraryInfo libraryInfo = new SharedLibraryInfo(entry.filename, null, null, entry.name, -1L, 0, new VersionedPackage(PackageManagerService.PLATFORM_PACKAGE_NAME, 0L), null, null, entry.isNative);
        commitSharedLibraryInfoLPw(libraryInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void commitSharedLibraryInfoLPw(SharedLibraryInfo libraryInfo) {
        String name = libraryInfo.getName();
        WatchedLongSparseArray<SharedLibraryInfo> versionedLib = this.mSharedLibraries.get(name);
        if (versionedLib == null) {
            versionedLib = new WatchedLongSparseArray<>();
            this.mSharedLibraries.put(name, versionedLib);
        }
        String declaringPackageName = libraryInfo.getDeclaringPackage().getPackageName();
        if (libraryInfo.getType() == 2) {
            this.mStaticLibsByDeclaringPackage.put(declaringPackageName, versionedLib);
        }
        versionedLib.put(libraryInfo.getLongVersion(), libraryInfo);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeSharedLibraryLPw(String libName, long version) {
        int libIdx;
        int libIdx2;
        int libIdx3;
        int libIdx4;
        WatchedLongSparseArray<SharedLibraryInfo> versionedLib = this.mSharedLibraries.get(libName);
        int i = 0;
        if (versionedLib == null || (libIdx = versionedLib.indexOfKey(version)) < 0) {
            return false;
        }
        SharedLibraryInfo libraryInfo = versionedLib.valueAt(libIdx);
        Computer snapshot = this.mPm.snapshotComputer();
        int[] userIds = this.mPm.mUserManager.getUserIds();
        int length = userIds.length;
        while (i < length) {
            int currentUserId = userIds[i];
            int i2 = length;
            List<VersionedPackage> dependents = snapshot.getPackagesUsingSharedLibrary(libraryInfo, 0L, 1000, currentUserId);
            if (dependents == null) {
                libIdx2 = libIdx;
            } else {
                for (VersionedPackage dependentPackage : dependents) {
                    PackageSetting ps = this.mPm.mSettings.getPackageLPr(dependentPackage.getPackageName());
                    if (ps == null) {
                        libIdx3 = libIdx;
                        libIdx4 = currentUserId;
                    } else {
                        libIdx3 = libIdx;
                        libIdx4 = currentUserId;
                        ps.setOverlayPathsForLibrary(libraryInfo.getName(), null, libIdx4);
                    }
                    currentUserId = libIdx4;
                    libIdx = libIdx3;
                }
                libIdx2 = libIdx;
            }
            i++;
            length = i2;
            libIdx = libIdx2;
        }
        versionedLib.remove(version);
        if (versionedLib.size() <= 0) {
            this.mSharedLibraries.remove(libName);
            if (libraryInfo.getType() == 2) {
                this.mStaticLibsByDeclaringPackage.remove(libraryInfo.getDeclaringPackage().getPackageName());
                return true;
            }
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<SharedLibraryInfo> getAllowedSharedLibInfos(ScanResult scanResult) {
        PackageSetting updatedSystemPs;
        ParsedPackage parsedPackage = scanResult.mRequest.mParsedPackage;
        if (scanResult.mSdkSharedLibraryInfo == null && scanResult.mStaticSharedLibraryInfo == null && scanResult.mDynamicSharedLibraryInfos == null) {
            return null;
        }
        if (scanResult.mSdkSharedLibraryInfo != null) {
            return Collections.singletonList(scanResult.mSdkSharedLibraryInfo);
        }
        if (scanResult.mStaticSharedLibraryInfo != null) {
            return Collections.singletonList(scanResult.mStaticSharedLibraryInfo);
        }
        boolean hasDynamicLibraries = parsedPackage.isSystem() && scanResult.mDynamicSharedLibraryInfos != null;
        if (hasDynamicLibraries) {
            boolean isUpdatedSystemApp = scanResult.mPkgSetting.getPkgState().isUpdatedSystemApp();
            if (isUpdatedSystemApp) {
                if (scanResult.mRequest.mDisabledPkgSetting == null) {
                    updatedSystemPs = scanResult.mRequest.mOldPkgSetting;
                } else {
                    updatedSystemPs = scanResult.mRequest.mDisabledPkgSetting;
                }
            } else {
                updatedSystemPs = null;
            }
            if (isUpdatedSystemApp && (updatedSystemPs.getPkg() == null || updatedSystemPs.getPkg().getLibraryNames() == null)) {
                Slog.w("PackageManager", "Package " + parsedPackage.getPackageName() + " declares libraries that are not declared on the system image; skipping");
                return null;
            }
            ArrayList<SharedLibraryInfo> infos = new ArrayList<>(scanResult.mDynamicSharedLibraryInfos.size());
            for (SharedLibraryInfo info : scanResult.mDynamicSharedLibraryInfos) {
                String name = info.getName();
                if (isUpdatedSystemApp && !updatedSystemPs.getPkg().getLibraryNames().contains(name)) {
                    Slog.w("PackageManager", "Package " + parsedPackage.getPackageName() + " declares library " + name + " that is not declared on system image; skipping");
                } else {
                    synchronized (this.mPm.mLock) {
                        if (getSharedLibraryInfo(name, -1L) != null) {
                            Slog.w("PackageManager", "Package " + parsedPackage.getPackageName() + " declares library " + name + " that already exists; skipping");
                        } else {
                            infos.add(info);
                        }
                    }
                }
            }
            return infos;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<SharedLibraryInfo> collectSharedLibraryInfos(AndroidPackage pkg, Map<String, AndroidPackage> availablePackages, Map<String, WatchedLongSparseArray<SharedLibraryInfo>> newLibraries) throws PackageManagerException {
        if (pkg == null) {
            return null;
        }
        PlatformCompat platformCompat = this.mInjector.getCompatibility();
        ArrayList<SharedLibraryInfo> usesLibraryInfos = null;
        if (!pkg.getUsesLibraries().isEmpty()) {
            usesLibraryInfos = collectSharedLibraryInfos(pkg.getUsesLibraries(), null, null, pkg.getPackageName(), "shared", true, pkg.getTargetSdkVersion(), null, availablePackages, newLibraries);
        }
        if (!pkg.getUsesStaticLibraries().isEmpty()) {
            usesLibraryInfos = collectSharedLibraryInfos(pkg.getUsesStaticLibraries(), pkg.getUsesStaticLibrariesVersions(), pkg.getUsesStaticLibrariesCertDigests(), pkg.getPackageName(), "static shared", true, pkg.getTargetSdkVersion(), usesLibraryInfos, availablePackages, newLibraries);
        }
        if (!pkg.getUsesOptionalLibraries().isEmpty()) {
            usesLibraryInfos = collectSharedLibraryInfos(pkg.getUsesOptionalLibraries(), null, null, pkg.getPackageName(), "shared", false, pkg.getTargetSdkVersion(), usesLibraryInfos, availablePackages, newLibraries);
        }
        if (platformCompat.isChangeEnabledInternal(ENFORCE_NATIVE_SHARED_LIBRARY_DEPENDENCIES, pkg.getPackageName(), pkg.getTargetSdkVersion())) {
            if (!pkg.getUsesNativeLibraries().isEmpty()) {
                usesLibraryInfos = collectSharedLibraryInfos(pkg.getUsesNativeLibraries(), null, null, pkg.getPackageName(), "native shared", true, pkg.getTargetSdkVersion(), usesLibraryInfos, availablePackages, newLibraries);
            }
            if (!pkg.getUsesOptionalNativeLibraries().isEmpty()) {
                usesLibraryInfos = collectSharedLibraryInfos(pkg.getUsesOptionalNativeLibraries(), null, null, pkg.getPackageName(), "native shared", false, pkg.getTargetSdkVersion(), usesLibraryInfos, availablePackages, newLibraries);
            }
        }
        if (!pkg.getUsesSdkLibraries().isEmpty()) {
            return collectSharedLibraryInfos(pkg.getUsesSdkLibraries(), pkg.getUsesSdkLibrariesVersionsMajor(), pkg.getUsesSdkLibrariesCertDigests(), pkg.getPackageName(), "sdk", true, pkg.getTargetSdkVersion(), usesLibraryInfos, availablePackages, newLibraries);
        }
        return usesLibraryInfos;
    }

    private ArrayList<SharedLibraryInfo> collectSharedLibraryInfos(List<String> requestedLibraries, long[] requiredVersions, String[][] requiredCertDigests, String packageName, String libraryType, boolean required, int targetSdk, ArrayList<SharedLibraryInfo> outUsedLibraries, Map<String, AndroidPackage> availablePackages, Map<String, WatchedLongSparseArray<SharedLibraryInfo>> newLibraries) throws PackageManagerException {
        int libCount;
        String[] libCertDigests;
        SharedLibrariesImpl sharedLibrariesImpl = this;
        String str = packageName;
        String str2 = libraryType;
        int libCount2 = requestedLibraries.size();
        ArrayList<SharedLibraryInfo> outUsedLibraries2 = outUsedLibraries;
        int i = 0;
        while (i < libCount2) {
            String libName = requestedLibraries.get(i);
            long libVersion = requiredVersions != null ? requiredVersions[i] : -1L;
            synchronized (sharedLibrariesImpl.mPm.mLock) {
                try {
                    try {
                        SharedLibraryInfo libraryInfo = SharedLibraryUtils.getSharedLibraryInfo(libName, libVersion, sharedLibrariesImpl.mSharedLibraries, newLibraries);
                        if (libraryInfo == null) {
                            if (required) {
                                throw new PackageManagerException(-9, "Package " + str + " requires unavailable " + str2 + " library " + libName + "; failing!");
                            }
                            libCount = libCount2;
                        } else {
                            if (requiredVersions == null || requiredCertDigests == null) {
                                libCount = libCount2;
                            } else if (libraryInfo.getLongVersion() != requiredVersions[i]) {
                                throw new PackageManagerException(-9, "Package " + str + " requires unavailable " + str2 + " library " + libName + " version " + libraryInfo.getLongVersion() + "; failing!");
                            } else {
                                AndroidPackage pkg = availablePackages.get(libraryInfo.getPackageName());
                                SigningDetails libPkg = pkg == null ? null : pkg.getSigningDetails();
                                if (libPkg == null) {
                                    throw new PackageManagerException(-9, "Package " + str + " requires unavailable " + str2 + " library; failing!");
                                }
                                String[] expectedCertDigests = requiredCertDigests[i];
                                libCount = libCount2;
                                if (expectedCertDigests.length > 1) {
                                    if (targetSdk >= 27) {
                                        libCertDigests = PackageUtils.computeSignaturesSha256Digests(libPkg.getSignatures());
                                    } else {
                                        libCertDigests = PackageUtils.computeSignaturesSha256Digests(new Signature[]{libPkg.getSignatures()[0]});
                                    }
                                    if (expectedCertDigests.length != libCertDigests.length) {
                                        throw new PackageManagerException(-9, "Package " + str + " requires differently signed " + str2 + " library; failing!");
                                    }
                                    Arrays.sort(libCertDigests);
                                    Arrays.sort(expectedCertDigests);
                                    int certCount = libCertDigests.length;
                                    int j = 0;
                                    while (j < certCount) {
                                        int certCount2 = certCount;
                                        String[] libCertDigests2 = libCertDigests;
                                        if (libCertDigests[j].equalsIgnoreCase(expectedCertDigests[j])) {
                                            j++;
                                            certCount = certCount2;
                                            libCertDigests = libCertDigests2;
                                        } else {
                                            throw new PackageManagerException(-9, "Package " + str + " requires differently signed " + str2 + " library; failing!");
                                        }
                                    }
                                } else {
                                    byte[] digestBytes = HexEncoding.decode(expectedCertDigests[0], false);
                                    if (!libPkg.hasSha256Certificate(digestBytes)) {
                                        throw new PackageManagerException(-9, "Package " + str + " requires differently signed " + str2 + " library; failing!");
                                    }
                                }
                            }
                            if (outUsedLibraries2 == null) {
                                outUsedLibraries2 = new ArrayList<>();
                            }
                            outUsedLibraries2.add(libraryInfo);
                        }
                        i++;
                        sharedLibrariesImpl = this;
                        str = packageName;
                        str2 = libraryType;
                        libCount2 = libCount;
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            }
        }
        return outUsedLibraries2;
    }

    @Override // com.android.server.pm.SharedLibrariesRead
    public void dump(PrintWriter pw, DumpState dumpState) {
        boolean checkin = dumpState.isCheckIn();
        boolean printedHeader = false;
        int numSharedLibraries = this.mSharedLibraries.size();
        for (int index = 0; index < numSharedLibraries; index++) {
            String libName = this.mSharedLibraries.keyAt(index);
            WatchedLongSparseArray<SharedLibraryInfo> versionedLib = this.mSharedLibraries.get(libName);
            if (versionedLib != null) {
                int versionCount = versionedLib.size();
                for (int i = 0; i < versionCount; i++) {
                    SharedLibraryInfo libraryInfo = versionedLib.valueAt(i);
                    if (!checkin) {
                        if (!printedHeader) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            pw.println("Libraries:");
                            printedHeader = true;
                        }
                        pw.print("  ");
                    } else {
                        pw.print("lib,");
                    }
                    pw.print(libraryInfo.getName());
                    if (libraryInfo.isStatic()) {
                        pw.print(" version=" + libraryInfo.getLongVersion());
                    }
                    if (!checkin) {
                        pw.print(" -> ");
                    }
                    if (libraryInfo.getPath() != null) {
                        if (libraryInfo.isNative()) {
                            pw.print(" (so) ");
                        } else {
                            pw.print(" (jar) ");
                        }
                        pw.print(libraryInfo.getPath());
                    } else {
                        pw.print(" (apk) ");
                        pw.print(libraryInfo.getPackageName());
                    }
                    pw.println();
                }
            }
        }
    }

    @Override // com.android.server.pm.SharedLibrariesRead
    public void dumpProto(ProtoOutputStream proto) {
        int count = this.mSharedLibraries.size();
        for (int i = 0; i < count; i++) {
            String libName = this.mSharedLibraries.keyAt(i);
            WatchedLongSparseArray<SharedLibraryInfo> versionedLib = this.mSharedLibraries.get(libName);
            if (versionedLib != null) {
                int versionCount = versionedLib.size();
                for (int j = 0; j < versionCount; j++) {
                    SharedLibraryInfo libraryInfo = versionedLib.valueAt(j);
                    long sharedLibraryToken = proto.start(2246267895811L);
                    proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, libraryInfo.getName());
                    boolean isJar = libraryInfo.getPath() != null;
                    proto.write(1133871366146L, isJar);
                    if (isJar) {
                        proto.write(1138166333443L, libraryInfo.getPath());
                    } else {
                        proto.write(1138166333444L, libraryInfo.getPackageName());
                    }
                    proto.end(sharedLibraryToken);
                }
            }
        }
    }
}
