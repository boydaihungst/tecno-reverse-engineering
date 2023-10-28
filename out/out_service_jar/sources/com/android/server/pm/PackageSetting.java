package com.android.server.pm;

import android.content.ComponentName;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.SigningDetails;
import android.content.pm.SigningInfo;
import android.content.pm.UserInfo;
import android.content.pm.overlay.OverlayPaths;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.CollectionUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.LegacyPermissionDataProvider;
import com.android.server.pm.permission.LegacyPermissionState;
import com.android.server.pm.pkg.AndroidPackageApi;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.PackageStateUnserialized;
import com.android.server.pm.pkg.PackageUserStateImpl;
import com.android.server.pm.pkg.PackageUserStateInternal;
import com.android.server.pm.pkg.SuspendParams;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.utils.SnapshotCache;
import com.android.server.utils.WatchedArraySet;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import defpackage.CompanionAppsPermissions;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public class PackageSetting extends SettingBase implements PackageStateInternal {
    private int categoryOverride;
    private boolean forceQueryableOverride;
    private boolean installPermissionsFixed;
    private InstallSource installSource;
    private PackageKeySetData keySetData;
    private long lastUpdateTime;
    @Deprecated
    private String legacyNativeLibraryPath;
    private int mAppId;
    private String mCpuAbiOverride;
    private UUID mDomainSetId;
    private long mLastModifiedTime;
    private float mLoadingProgress;
    private String mName;
    @Deprecated
    private Set<String> mOldCodePaths;
    private File mPath;
    private String mPathString;
    private String mPrimaryCpuAbi;
    private String mRealName;
    private String mSecondaryCpuAbi;
    private int mSharedUserAppId;
    private final SnapshotCache<PackageSetting> mSnapshot;
    private final SparseArray<PackageUserStateImpl> mUserStates;
    private Map<String, Set<String>> mimeGroups;
    public AndroidPackage pkg;
    private final PackageStateUnserialized pkgState;
    private PackageSignatures signatures;
    private boolean updateAvailable;
    private String[] usesSdkLibraries;
    private long[] usesSdkLibrariesVersionsMajor;
    private String[] usesStaticLibraries;
    private long[] usesStaticLibrariesVersions;
    private long versionCode;
    private String volumeUuid;

    private SnapshotCache<PackageSetting> makeCache() {
        return new SnapshotCache<PackageSetting>(this, this) { // from class: com.android.server.pm.PackageSetting.1
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public PackageSetting createSnapshot() {
                return new PackageSetting((PackageSetting) this.mSource, true);
            }
        };
    }

    public PackageSetting(String name, String realName, File path, String legacyNativeLibraryPath, String primaryCpuAbi, String secondaryCpuAbi, String cpuAbiOverride, long longVersionCode, int pkgFlags, int pkgPrivateFlags, int sharedUserAppId, String[] usesSdkLibraries, long[] usesSdkLibrariesVersionsMajor, String[] usesStaticLibraries, long[] usesStaticLibrariesVersions, Map<String, Set<String>> mimeGroups, UUID domainSetId) {
        super(pkgFlags, pkgPrivateFlags);
        this.keySetData = new PackageKeySetData();
        this.mUserStates = new SparseArray<>();
        this.categoryOverride = -1;
        this.pkgState = new PackageStateUnserialized(this);
        this.mName = name;
        this.mRealName = realName;
        this.usesSdkLibraries = usesSdkLibraries;
        this.usesSdkLibrariesVersionsMajor = usesSdkLibrariesVersionsMajor;
        this.usesStaticLibraries = usesStaticLibraries;
        this.usesStaticLibrariesVersions = usesStaticLibrariesVersions;
        this.mPath = path;
        this.mPathString = path.toString();
        this.legacyNativeLibraryPath = legacyNativeLibraryPath;
        this.mPrimaryCpuAbi = primaryCpuAbi;
        this.mSecondaryCpuAbi = secondaryCpuAbi;
        this.mCpuAbiOverride = cpuAbiOverride;
        this.versionCode = longVersionCode;
        this.signatures = new PackageSignatures();
        this.installSource = InstallSource.EMPTY;
        this.mSharedUserAppId = sharedUserAppId;
        this.mDomainSetId = domainSetId;
        copyMimeGroups(mimeGroups);
        this.mSnapshot = makeCache();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSetting(PackageSetting orig) {
        this(orig, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSetting(PackageSetting base, String realPkgName) {
        this(base, false);
        this.mRealName = realPkgName;
    }

    PackageSetting(PackageSetting original, boolean sealedSnapshot) {
        super(original);
        this.keySetData = new PackageKeySetData();
        this.mUserStates = new SparseArray<>();
        this.categoryOverride = -1;
        this.pkgState = new PackageStateUnserialized(this);
        copyPackageSetting(original, sealedSnapshot);
        if (sealedSnapshot) {
            this.mSnapshot = new SnapshotCache.Sealed();
        } else {
            this.mSnapshot = makeCache();
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public PackageSetting snapshot() {
        return this.mSnapshot.snapshot();
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId, List<UserInfo> users, LegacyPermissionDataProvider dataProvider) {
        long packageToken = proto.start(fieldId);
        String str = this.mRealName;
        if (str == null) {
            str = this.mName;
        }
        proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, str);
        proto.write(1120986464258L, this.mAppId);
        proto.write(1120986464259L, this.versionCode);
        proto.write(1112396529670L, this.lastUpdateTime);
        proto.write(1138166333447L, this.installSource.installerPackageName);
        AndroidPackage androidPackage = this.pkg;
        if (androidPackage != null) {
            proto.write(1138166333444L, androidPackage.getVersionName());
            long splitToken = proto.start(2246267895816L);
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, "base");
            proto.write(1120986464258L, this.pkg.getBaseRevisionCode());
            proto.end(splitToken);
            for (int i = 0; i < this.pkg.getSplitNames().length; i++) {
                long splitToken2 = proto.start(2246267895816L);
                proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.pkg.getSplitNames()[i]);
                proto.write(1120986464258L, this.pkg.getSplitRevisionCodes()[i]);
                proto.end(splitToken2);
            }
            long sourceToken = proto.start(1146756268042L);
            proto.write(CompanionAppsPermissions.AppPermissions.PACKAGE_NAME, this.installSource.initiatingPackageName);
            proto.write(1138166333442L, this.installSource.originatingPackageName);
            proto.end(sourceToken);
        }
        proto.write(1133871366146L, isLoading());
        writeUsersInfoToProto(proto, 2246267895817L);
        writePackageUserPermissionsProto(proto, 2246267895820L, users, dataProvider);
        proto.end(packageToken);
    }

    public PackageSetting setAppId(int appId) {
        this.mAppId = appId;
        onChanged();
        return this;
    }

    public PackageSetting setCpuAbiOverride(String cpuAbiOverrideString) {
        this.mCpuAbiOverride = cpuAbiOverrideString;
        onChanged();
        return this;
    }

    public PackageSetting setFirstInstallTimeFromReplaced(PackageStateInternal replacedPkgSetting, int[] userIds) {
        for (int userId = 0; userId < userIds.length; userId++) {
            long previousFirstInstallTime = replacedPkgSetting.getUserStateOrDefault(userId).getFirstInstallTime();
            if (previousFirstInstallTime != 0) {
                modifyUserState(userId).setFirstInstallTime(previousFirstInstallTime);
            }
        }
        onChanged();
        return this;
    }

    public PackageSetting setFirstInstallTime(long firstInstallTime, int userId) {
        if (userId == -1) {
            int userStateCount = this.mUserStates.size();
            for (int i = 0; i < userStateCount; i++) {
                this.mUserStates.valueAt(i).setFirstInstallTime(firstInstallTime);
            }
        } else {
            modifyUserState(userId).setFirstInstallTime(firstInstallTime);
        }
        onChanged();
        return this;
    }

    public PackageSetting setForceQueryableOverride(boolean forceQueryableOverride) {
        this.forceQueryableOverride = forceQueryableOverride;
        onChanged();
        return this;
    }

    public PackageSetting setInstallerPackageName(String packageName) {
        this.installSource = this.installSource.setInstallerPackage(packageName);
        onChanged();
        return this;
    }

    public PackageSetting setInstallSource(InstallSource installSource) {
        this.installSource = (InstallSource) Objects.requireNonNull(installSource);
        onChanged();
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSetting removeInstallerPackage(String packageName) {
        this.installSource = this.installSource.removeInstallerPackage(packageName);
        onChanged();
        return this;
    }

    public PackageSetting setIsOrphaned(boolean isOrphaned) {
        this.installSource = this.installSource.setIsOrphaned(isOrphaned);
        onChanged();
        return this;
    }

    public PackageSetting setKeySetData(PackageKeySetData keySetData) {
        this.keySetData = keySetData;
        onChanged();
        return this;
    }

    public PackageSetting setLastModifiedTime(long timeStamp) {
        this.mLastModifiedTime = timeStamp;
        onChanged();
        return this;
    }

    public PackageSetting setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
        onChanged();
        return this;
    }

    public PackageSetting setLongVersionCode(long versionCode) {
        this.versionCode = versionCode;
        onChanged();
        return this;
    }

    public boolean setMimeGroup(String mimeGroup, ArraySet<String> newMimeTypes) {
        Map<String, Set<String>> map = this.mimeGroups;
        Set<String> oldMimeTypes = map == null ? null : map.get(mimeGroup);
        if (oldMimeTypes == null) {
            throw new IllegalArgumentException("Unknown MIME group " + mimeGroup + " for package " + this.mName);
        }
        boolean hasChanges = !newMimeTypes.equals(oldMimeTypes);
        this.mimeGroups.put(mimeGroup, newMimeTypes);
        if (hasChanges) {
            onChanged();
        }
        return hasChanges;
    }

    public PackageSetting setPkg(AndroidPackage pkg) {
        this.pkg = pkg;
        onChanged();
        return this;
    }

    public PackageSetting setPkgStateLibraryFiles(Collection<String> usesLibraryFiles) {
        Collection<String> oldUsesLibraryFiles = getUsesLibraryFiles();
        if (oldUsesLibraryFiles.size() != usesLibraryFiles.size() || !oldUsesLibraryFiles.containsAll(usesLibraryFiles)) {
            this.pkgState.setUsesLibraryFiles(new ArrayList(usesLibraryFiles));
            onChanged();
        }
        return this;
    }

    public PackageSetting setPrimaryCpuAbi(String primaryCpuAbiString) {
        this.mPrimaryCpuAbi = primaryCpuAbiString;
        onChanged();
        return this;
    }

    public PackageSetting setSecondaryCpuAbi(String secondaryCpuAbiString) {
        this.mSecondaryCpuAbi = secondaryCpuAbiString;
        onChanged();
        return this;
    }

    public PackageSetting setSignatures(PackageSignatures signatures) {
        this.signatures = signatures;
        onChanged();
        return this;
    }

    public PackageSetting setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
        onChanged();
        return this;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isExternalStorage() {
        return (getFlags() & 262144) != 0;
    }

    public PackageSetting setUpdateAvailable(boolean updateAvailable) {
        this.updateAvailable = updateAvailable;
        onChanged();
        return this;
    }

    public void setSharedUserAppId(int sharedUserAppId) {
        this.mSharedUserAppId = sharedUserAppId;
        onChanged();
    }

    @Override // com.android.server.pm.pkg.PackageState
    public int getSharedUserAppId() {
        return this.mSharedUserAppId;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean hasSharedUser() {
        return this.mSharedUserAppId > 0;
    }

    public String toString() {
        return "PackageSetting{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.mName + SliceClientPermissions.SliceAuthority.DELIMITER + this.mAppId + "}";
    }

    protected void copyMimeGroups(Map<String, Set<String>> newMimeGroups) {
        if (newMimeGroups == null) {
            this.mimeGroups = null;
            return;
        }
        this.mimeGroups = new ArrayMap(newMimeGroups.size());
        for (String mimeGroup : newMimeGroups.keySet()) {
            Set<String> mimeTypes = newMimeGroups.get(mimeGroup);
            if (mimeTypes != null) {
                this.mimeGroups.put(mimeGroup, new ArraySet(mimeTypes));
            } else {
                this.mimeGroups.put(mimeGroup, new ArraySet());
            }
        }
    }

    public void updateFrom(PackageSetting other) {
        copyPackageSetting(other, false);
        Map<String, Set<String>> map = other.mimeGroups;
        Set<String> mimeGroupNames = map != null ? map.keySet() : null;
        updateMimeGroups(mimeGroupNames);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSetting updateMimeGroups(Set<String> newMimeGroupNames) {
        if (newMimeGroupNames == null) {
            this.mimeGroups = null;
            return this;
        }
        if (this.mimeGroups == null) {
            this.mimeGroups = Collections.emptyMap();
        }
        ArrayMap<String, Set<String>> updatedMimeGroups = new ArrayMap<>(newMimeGroupNames.size());
        for (String mimeGroup : newMimeGroupNames) {
            if (this.mimeGroups.containsKey(mimeGroup)) {
                updatedMimeGroups.put(mimeGroup, this.mimeGroups.get(mimeGroup));
            } else {
                updatedMimeGroups.put(mimeGroup, new ArraySet<>());
            }
        }
        onChanged();
        this.mimeGroups = updatedMimeGroups;
        return this;
    }

    @Override // com.android.server.pm.SettingBase, com.android.server.pm.pkg.PackageStateInternal
    @Deprecated
    public LegacyPermissionState getLegacyPermissionState() {
        return super.getLegacyPermissionState();
    }

    public PackageSetting setInstallPermissionsFixed(boolean installPermissionsFixed) {
        this.installPermissionsFixed = installPermissionsFixed;
        return this;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isPrivileged() {
        return (getPrivateFlags() & 8) != 0;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isOem() {
        return (getPrivateFlags() & 131072) != 0;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isVendor() {
        return (getPrivateFlags() & 262144) != 0;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isProduct() {
        return (getPrivateFlags() & 524288) != 0;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isRequiredForSystemUser() {
        return (getPrivateFlags() & 512) != 0;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isSystemExt() {
        return (getPrivateFlags() & 2097152) != 0;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isOdm() {
        return (getPrivateFlags() & 1073741824) != 0;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isSystem() {
        return (getFlags() & 1) != 0;
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal
    public SigningDetails getSigningDetails() {
        return this.signatures.mSigningDetails;
    }

    public PackageSetting setSigningDetails(SigningDetails signingDetails) {
        this.signatures.mSigningDetails = signingDetails;
        onChanged();
        return this;
    }

    public void copyPackageSetting(PackageSetting other, boolean sealedSnapshot) {
        String[] strArr;
        long[] jArr;
        String[] strArr2;
        long[] jArr2;
        super.copySettingBase(other);
        this.mSharedUserAppId = other.mSharedUserAppId;
        this.mLoadingProgress = other.mLoadingProgress;
        this.legacyNativeLibraryPath = other.legacyNativeLibraryPath;
        this.mName = other.mName;
        this.mRealName = other.mRealName;
        this.mAppId = other.mAppId;
        this.pkg = other.pkg;
        this.mPath = other.mPath;
        this.mPathString = other.mPathString;
        this.mPrimaryCpuAbi = other.mPrimaryCpuAbi;
        this.mSecondaryCpuAbi = other.mSecondaryCpuAbi;
        this.mCpuAbiOverride = other.mCpuAbiOverride;
        this.mLastModifiedTime = other.mLastModifiedTime;
        this.lastUpdateTime = other.lastUpdateTime;
        this.versionCode = other.versionCode;
        this.signatures = other.signatures;
        this.installPermissionsFixed = other.installPermissionsFixed;
        this.keySetData = new PackageKeySetData(other.keySetData);
        this.installSource = other.installSource;
        this.volumeUuid = other.volumeUuid;
        this.categoryOverride = other.categoryOverride;
        this.updateAvailable = other.updateAvailable;
        this.forceQueryableOverride = other.forceQueryableOverride;
        this.mDomainSetId = other.mDomainSetId;
        String[] strArr3 = other.usesSdkLibraries;
        if (strArr3 != null) {
            strArr = (String[]) Arrays.copyOf(strArr3, strArr3.length);
        } else {
            strArr = null;
        }
        this.usesSdkLibraries = strArr;
        long[] jArr3 = other.usesSdkLibrariesVersionsMajor;
        if (jArr3 != null) {
            jArr = Arrays.copyOf(jArr3, jArr3.length);
        } else {
            jArr = null;
        }
        this.usesSdkLibrariesVersionsMajor = jArr;
        String[] strArr4 = other.usesStaticLibraries;
        if (strArr4 != null) {
            strArr2 = (String[]) Arrays.copyOf(strArr4, strArr4.length);
        } else {
            strArr2 = null;
        }
        this.usesStaticLibraries = strArr2;
        long[] jArr4 = other.usesStaticLibrariesVersions;
        if (jArr4 != null) {
            jArr2 = Arrays.copyOf(jArr4, jArr4.length);
        } else {
            jArr2 = null;
        }
        this.usesStaticLibrariesVersions = jArr2;
        this.mUserStates.clear();
        for (int i = 0; i < other.mUserStates.size(); i++) {
            if (sealedSnapshot) {
                this.mUserStates.put(other.mUserStates.keyAt(i), other.mUserStates.valueAt(i).snapshot());
            } else {
                this.mUserStates.put(other.mUserStates.keyAt(i), other.mUserStates.valueAt(i));
            }
        }
        Set<String> set = this.mOldCodePaths;
        if (set != null) {
            if (other.mOldCodePaths != null) {
                set.clear();
                this.mOldCodePaths.addAll(other.mOldCodePaths);
            } else {
                this.mOldCodePaths = null;
            }
        }
        copyMimeGroups(other.mimeGroups);
        this.pkgState.updateFrom(other.pkgState);
        onChanged();
    }

    PackageUserStateImpl modifyUserState(int userId) {
        PackageUserStateImpl state = this.mUserStates.get(userId);
        if (state == null) {
            PackageUserStateImpl state2 = new PackageUserStateImpl(this);
            this.mUserStates.put(userId, state2);
            onChanged();
            return state2;
        }
        return state;
    }

    public PackageUserStateImpl getOrCreateUserState(int userId) {
        PackageUserStateImpl state = this.mUserStates.get(userId);
        if (state == null) {
            PackageUserStateImpl state2 = new PackageUserStateImpl(this);
            this.mUserStates.put(userId, state2);
            return state2;
        }
        return state;
    }

    public PackageUserStateInternal readUserState(int userId) {
        PackageUserStateInternal state = this.mUserStates.get(userId);
        if (state == null) {
            return PackageUserStateInternal.DEFAULT;
        }
        return state;
    }

    public void setEnabled(int state, int userId, String callingPackage) {
        boolean isXhidden = IPackageManagerServiceLice.Instance().setApplicationHide(this.mName, state, 0, userId, callingPackage, getOrCreateUserState(userId).getLastDisableAppCaller());
        if (!isXhidden) {
            Slog.d("PackageManager", "Xhide hidden this app, must use xhide enable it, callingPackage=" + callingPackage + ", name=" + this.mName);
            return;
        }
        modifyUserState(userId).setEnabledState(state).setLastDisableAppCaller(callingPackage);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getEnabled(int userId) {
        return readUserState(userId).getEnabledState();
    }

    public void setInstalled(boolean inst, int userId) {
        modifyUserState(userId).setInstalled(inst);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getInstalled(int userId) {
        return readUserState(userId).isInstalled();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getInstallReason(int userId) {
        return readUserState(userId).getInstallReason();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInstallReason(int installReason, int userId) {
        modifyUserState(userId).setInstallReason(installReason);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getUninstallReason(int userId) {
        return readUserState(userId).getUninstallReason();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUninstallReason(int uninstallReason, int userId) {
        modifyUserState(userId).setUninstallReason(uninstallReason);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverlayPaths getOverlayPaths(int userId) {
        return readUserState(userId).getOverlayPaths();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setOverlayPathsForLibrary(String libName, OverlayPaths overlayPaths, int userId) {
        boolean changed = modifyUserState(userId).setSharedLibraryOverlayPaths(libName, overlayPaths);
        onChanged();
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAnyInstalled(int[] users) {
        for (int user : users) {
            if (readUserState(user).isInstalled()) {
                return true;
            }
        }
        return false;
    }

    public int[] queryInstalledUsers(int[] users, boolean installed) {
        int num = 0;
        for (int user : users) {
            if (getInstalled(user) == installed) {
                num++;
            }
        }
        int[] res = new int[num];
        int num2 = 0;
        for (int user2 : users) {
            if (getInstalled(user2) == installed) {
                res[num2] = user2;
                num2++;
            }
        }
        return res;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getCeDataInode(int userId) {
        return readUserState(userId).getCeDataInode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setCeDataInode(long ceDataInode, int userId) {
        modifyUserState(userId).setCeDataInode(ceDataInode);
        onChanged();
    }

    boolean getStopped(int userId) {
        return readUserState(userId).isStopped();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStopped(boolean stop, int userId) {
        modifyUserState(userId).setStopped(stop);
        onChanged();
    }

    boolean getNotLaunched(int userId) {
        return readUserState(userId).isNotLaunched();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setNotLaunched(boolean stop, int userId) {
        modifyUserState(userId).setNotLaunched(stop);
        onChanged();
    }

    boolean getHidden(int userId) {
        return readUserState(userId).isHidden();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setHidden(boolean hidden, int userId) {
        modifyUserState(userId).setHidden(hidden);
        onChanged();
    }

    int getDistractionFlags(int userId) {
        return readUserState(userId).getDistractionFlags();
    }

    void setDistractionFlags(int distractionFlags, int userId) {
        modifyUserState(userId).setDistractionFlags(distractionFlags);
        onChanged();
    }

    public boolean getInstantApp(int userId) {
        return readUserState(userId).isInstantApp();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setInstantApp(boolean instantApp, int userId) {
        modifyUserState(userId).setInstantApp(instantApp);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getVirtualPreload(int userId) {
        return readUserState(userId).isVirtualPreload();
    }

    void setVirtualPreload(boolean virtualPreload, int userId) {
        modifyUserState(userId).setVirtualPreload(virtualPreload);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setUserState(int userId, long ceDataInode, int enabled, boolean installed, boolean stopped, boolean notLaunched, boolean hidden, int distractionFlags, ArrayMap<String, SuspendParams> suspendParams, boolean instantApp, boolean virtualPreload, String lastDisableAppCaller, ArraySet<String> enabledComponents, ArraySet<String> disabledComponents, int installReason, int uninstallReason, String harmfulAppWarning, String splashScreenTheme, long firstInstallTime) {
        modifyUserState(userId).setSuspendParams(suspendParams).setCeDataInode(ceDataInode).setEnabledState(enabled).setInstalled(installed).setStopped(stopped).setNotLaunched(notLaunched).setHidden(hidden).setDistractionFlags(distractionFlags).setLastDisableAppCaller(lastDisableAppCaller).setEnabledComponents(enabledComponents).setDisabledComponents(disabledComponents).setInstallReason(installReason).setUninstallReason(uninstallReason).setInstantApp(instantApp).setVirtualPreload(virtualPreload).setHarmfulAppWarning(harmfulAppWarning).setSplashScreenTheme(splashScreenTheme).setFirstInstallTime(firstInstallTime);
        onChanged();
        ITranPackageManagerService.Instance().setApplicationHide(this.mName, enabled, 0, userId, "fromSetUserState", lastDisableAppCaller);
    }

    void setUserState(int userId, PackageUserStateInternal otherState) {
        setUserState(userId, otherState.getCeDataInode(), otherState.getEnabledState(), otherState.isInstalled(), otherState.isStopped(), otherState.isNotLaunched(), otherState.isHidden(), otherState.getDistractionFlags(), otherState.getSuspendParams() == null ? null : otherState.getSuspendParams().untrackedStorage(), otherState.isInstantApp(), otherState.isVirtualPreload(), otherState.getLastDisableAppCaller(), otherState.getEnabledComponentsNoCopy() == null ? null : otherState.getEnabledComponentsNoCopy().untrackedStorage(), otherState.getDisabledComponentsNoCopy() == null ? null : otherState.getDisabledComponentsNoCopy().untrackedStorage(), otherState.getInstallReason(), otherState.getUninstallReason(), otherState.getHarmfulAppWarning(), otherState.getSplashScreenTheme(), otherState.getFirstInstallTime());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WatchedArraySet<String> getEnabledComponents(int userId) {
        return readUserState(userId).getEnabledComponentsNoCopy();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WatchedArraySet<String> getDisabledComponents(int userId) {
        return readUserState(userId).getDisabledComponentsNoCopy();
    }

    void setEnabledComponents(WatchedArraySet<String> components, int userId) {
        modifyUserState(userId).setEnabledComponents(components);
        onChanged();
    }

    void setDisabledComponents(WatchedArraySet<String> components, int userId) {
        modifyUserState(userId).setDisabledComponents(components);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setEnabledComponentsCopy(WatchedArraySet<String> components, int userId) {
        modifyUserState(userId).setEnabledComponents(components != null ? components.untrackedStorage() : null);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisabledComponentsCopy(WatchedArraySet<String> components, int userId) {
        modifyUserState(userId).setDisabledComponents(components != null ? components.untrackedStorage() : null);
        onChanged();
    }

    PackageUserStateImpl modifyUserStateComponents(int userId, boolean disabled, boolean enabled) {
        PackageUserStateImpl state = modifyUserState(userId);
        boolean changed = false;
        if (disabled && state.getDisabledComponentsNoCopy() == null) {
            state.setDisabledComponents(new ArraySet<>(1));
            changed = true;
        }
        if (enabled && state.getEnabledComponentsNoCopy() == null) {
            state.setEnabledComponents(new ArraySet<>(1));
            changed = true;
        }
        if (changed) {
            onChanged();
        }
        return state;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addDisabledComponent(String componentClassName, int userId) {
        modifyUserStateComponents(userId, true, false).getDisabledComponentsNoCopy().add(componentClassName);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addEnabledComponent(String componentClassName, int userId) {
        modifyUserStateComponents(userId, false, true).getEnabledComponentsNoCopy().add(componentClassName);
        onChanged();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean enableComponentLPw(String componentClassName, int userId) {
        boolean changed = false;
        PackageUserStateImpl state = modifyUserStateComponents(userId, false, true);
        if (state.getDisabledComponentsNoCopy() != null) {
            changed = state.getDisabledComponentsNoCopy().remove(componentClassName);
        }
        boolean changed2 = changed | state.getEnabledComponentsNoCopy().add(componentClassName);
        if (changed2) {
            onChanged();
        }
        return changed2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean disableComponentLPw(String componentClassName, int userId) {
        PackageUserStateImpl state = modifyUserStateComponents(userId, true, false);
        boolean changed = (state.getEnabledComponentsNoCopy() != null ? state.getEnabledComponentsNoCopy().remove(componentClassName) : false) | state.getDisabledComponentsNoCopy().add(componentClassName);
        if (changed) {
            onChanged();
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean restoreComponentLPw(String componentClassName, int userId) {
        PackageUserStateImpl state = modifyUserStateComponents(userId, true, true);
        boolean changed = (state.getDisabledComponentsNoCopy() != null ? state.getDisabledComponentsNoCopy().remove(componentClassName) : false) | (state.getEnabledComponentsNoCopy() != null ? state.getEnabledComponentsNoCopy().remove(componentClassName) : false);
        if (changed) {
            onChanged();
        }
        return changed;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getCurrentEnabledStateLPr(String componentName, int userId) {
        PackageUserStateInternal state = readUserState(userId);
        if (state.getEnabledComponentsNoCopy() != null && state.getEnabledComponentsNoCopy().contains(componentName)) {
            return 1;
        }
        if (state.getDisabledComponentsNoCopy() != null && state.getDisabledComponentsNoCopy().contains(componentName)) {
            return 2;
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeUser(int userId) {
        this.mUserStates.delete(userId);
        onChanged();
    }

    public int[] getNotInstalledUserIds() {
        int count = 0;
        int userStateCount = this.mUserStates.size();
        for (int i = 0; i < userStateCount; i++) {
            if (!this.mUserStates.valueAt(i).isInstalled()) {
                count++;
            }
        }
        if (count == 0) {
            return EmptyArray.INT;
        }
        int[] excludedUserIds = new int[count];
        int idx = 0;
        for (int i2 = 0; i2 < userStateCount; i2++) {
            if (!this.mUserStates.valueAt(i2).isInstalled()) {
                excludedUserIds[idx] = this.mUserStates.keyAt(i2);
                idx++;
            }
        }
        return excludedUserIds;
    }

    void writePackageUserPermissionsProto(ProtoOutputStream proto, long fieldId, List<UserInfo> users, LegacyPermissionDataProvider dataProvider) {
        for (UserInfo user : users) {
            long permissionsToken = proto.start(2246267895820L);
            proto.write(CompanionMessage.MESSAGE_ID, user.id);
            Collection<LegacyPermissionState.PermissionState> runtimePermissionStates = dataProvider.getLegacyPermissionState(this.mAppId).getPermissionStates(user.id);
            for (LegacyPermissionState.PermissionState permission : runtimePermissionStates) {
                if (permission.isGranted()) {
                    proto.write(2237677961218L, permission.getName());
                }
            }
            proto.end(permissionsToken);
        }
    }

    protected void writeUsersInfoToProto(ProtoOutputStream proto, long fieldId) {
        int installType;
        int count = this.mUserStates.size();
        for (int i = 0; i < count; i++) {
            long userToken = proto.start(fieldId);
            int userId = this.mUserStates.keyAt(i);
            PackageUserStateInternal state = this.mUserStates.valueAt(i);
            proto.write(CompanionMessage.MESSAGE_ID, userId);
            if (state.isInstantApp()) {
                installType = 2;
            } else if (state.isInstalled()) {
                installType = 1;
            } else {
                installType = 0;
            }
            proto.write(CompanionMessage.TYPE, installType);
            proto.write(1133871366147L, state.isHidden());
            proto.write(1120986464266L, state.getDistractionFlags());
            proto.write(1133871366148L, state.isSuspended());
            if (state.isSuspended()) {
                for (int j = 0; j < state.getSuspendParams().size(); j++) {
                    proto.write(2237677961225L, state.getSuspendParams().keyAt(j));
                }
            }
            proto.write(1133871366149L, state.isStopped());
            proto.write(1133871366150L, !state.isNotLaunched());
            proto.write(1159641169927L, state.getEnabledState());
            proto.write(1138166333448L, state.getLastDisableAppCaller());
            proto.write(1120986464267L, state.getFirstInstallTime());
            proto.end(userToken);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSetting setPath(File path) {
        this.mPath = path;
        this.mPathString = path.toString();
        onChanged();
        return this;
    }

    public boolean overrideNonLocalizedLabelAndIcon(ComponentName component, String label, Integer icon, int userId) {
        boolean changed = modifyUserState(userId).overrideLabelAndIcon(component, label, icon);
        onChanged();
        return changed;
    }

    public void resetOverrideComponentLabelIcon(int userId) {
        modifyUserState(userId).resetOverrideComponentLabelIcon();
        onChanged();
    }

    public String getSplashScreenTheme(int userId) {
        return readUserState(userId).getSplashScreenTheme();
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal
    public boolean isLoading() {
        return Math.abs(1.0f - this.mLoadingProgress) >= 1.0E-8f;
    }

    public PackageSetting setLoadingProgress(float progress) {
        this.mLoadingProgress = progress;
        onChanged();
        return this;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long getVersionCode() {
        return this.versionCode;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public Map<String, Set<String>> getMimeGroups() {
        return CollectionUtils.isEmpty(this.mimeGroups) ? Collections.emptyMap() : Collections.unmodifiableMap(this.mimeGroups);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String getPackageName() {
        return this.mName;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public AndroidPackageApi getAndroidPackage() {
        return getPkg();
    }

    @Override // com.android.server.pm.pkg.PackageState
    public SigningInfo getSigningInfo() {
        return new SigningInfo(this.signatures.mSigningDetails);
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String[] getUsesSdkLibraries() {
        String[] strArr = this.usesSdkLibraries;
        return strArr == null ? EmptyArray.STRING : strArr;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long[] getUsesSdkLibrariesVersionsMajor() {
        long[] jArr = this.usesSdkLibrariesVersionsMajor;
        return jArr == null ? EmptyArray.LONG : jArr;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String[] getUsesStaticLibraries() {
        String[] strArr = this.usesStaticLibraries;
        return strArr == null ? EmptyArray.STRING : strArr;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long[] getUsesStaticLibrariesVersions() {
        long[] jArr = this.usesStaticLibrariesVersions;
        return jArr == null ? EmptyArray.LONG : jArr;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public List<SharedLibraryInfo> getUsesLibraryInfos() {
        return this.pkgState.getUsesLibraryInfos();
    }

    @Override // com.android.server.pm.pkg.PackageState
    public List<String> getUsesLibraryFiles() {
        return this.pkgState.getUsesLibraryFiles();
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isHiddenUntilInstalled() {
        return this.pkgState.isHiddenUntilInstalled();
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long[] getLastPackageUsageTime() {
        return this.pkgState.getLastPackageUsageTimeInMills();
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isUpdatedSystemApp() {
        return this.pkgState.isUpdatedSystemApp();
    }

    public PackageSetting setDomainSetId(UUID domainSetId) {
        this.mDomainSetId = domainSetId;
        onChanged();
        return this;
    }

    public PackageSetting setCategoryOverride(int categoryHint) {
        this.categoryOverride = categoryHint;
        onChanged();
        return this;
    }

    public PackageSetting setLegacyNativeLibraryPath(String legacyNativeLibraryPathString) {
        this.legacyNativeLibraryPath = legacyNativeLibraryPathString;
        onChanged();
        return this;
    }

    public PackageSetting setMimeGroups(Map<String, Set<String>> mimeGroups) {
        this.mimeGroups = mimeGroups;
        onChanged();
        return this;
    }

    public PackageSetting setOldCodePaths(Set<String> oldCodePaths) {
        this.mOldCodePaths = oldCodePaths;
        onChanged();
        return this;
    }

    public PackageSetting setUsesSdkLibraries(String[] usesSdkLibraries) {
        this.usesSdkLibraries = usesSdkLibraries;
        onChanged();
        return this;
    }

    public PackageSetting setUsesSdkLibrariesVersionsMajor(long[] usesSdkLibrariesVersions) {
        this.usesSdkLibrariesVersionsMajor = usesSdkLibrariesVersions;
        onChanged();
        return this;
    }

    public PackageSetting setUsesStaticLibraries(String[] usesStaticLibraries) {
        this.usesStaticLibraries = usesStaticLibraries;
        onChanged();
        return this;
    }

    public PackageSetting setUsesStaticLibrariesVersions(long[] usesStaticLibrariesVersions) {
        this.usesStaticLibrariesVersions = usesStaticLibrariesVersions;
        onChanged();
        return this;
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal
    public PackageStateUnserialized getTransientState() {
        return this.pkgState;
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal, com.android.server.pm.pkg.PackageState
    public SparseArray<? extends PackageUserStateInternal> getUserStates() {
        return this.mUserStates;
    }

    public PackageSetting addMimeTypes(String mimeGroup, Set<String> mimeTypes) {
        if (this.mimeGroups == null) {
            this.mimeGroups = new ArrayMap();
        }
        Set<String> existingMimeTypes = this.mimeGroups.get(mimeGroup);
        if (existingMimeTypes == null) {
            existingMimeTypes = new ArraySet();
            this.mimeGroups.put(mimeGroup, existingMimeTypes);
        }
        existingMimeTypes.addAll(mimeTypes);
        return this;
    }

    @Deprecated
    public Set<String> getOldCodePaths() {
        return this.mOldCodePaths;
    }

    @Deprecated
    public String getLegacyNativeLibraryPath() {
        return this.legacyNativeLibraryPath;
    }

    public String getName() {
        return this.mName;
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal
    public String getRealName() {
        return this.mRealName;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public int getAppId() {
        return this.mAppId;
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal
    public AndroidPackage getPkg() {
        return this.pkg;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public File getPath() {
        return this.mPath;
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal
    public String getPathString() {
        return this.mPathString;
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal
    public float getLoadingProgress() {
        return this.mLoadingProgress;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String getPrimaryCpuAbi() {
        return this.mPrimaryCpuAbi;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String getSecondaryCpuAbi() {
        return this.mSecondaryCpuAbi;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String getCpuAbiOverride() {
        return this.mCpuAbiOverride;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long getLastModifiedTime() {
        return this.mLastModifiedTime;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public long getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    public PackageSignatures getSignatures() {
        return this.signatures;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isInstallPermissionsFixed() {
        return this.installPermissionsFixed;
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal
    public PackageKeySetData getKeySetData() {
        return this.keySetData;
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal
    public InstallSource getInstallSource() {
        return this.installSource;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public String getVolumeUuid() {
        return this.volumeUuid;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public int getCategoryOverride() {
        return this.categoryOverride;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isUpdateAvailable() {
        return this.updateAvailable;
    }

    @Override // com.android.server.pm.pkg.PackageState
    public boolean isForceQueryableOverride() {
        return this.forceQueryableOverride;
    }

    public PackageStateUnserialized getPkgState() {
        return this.pkgState;
    }

    @Override // com.android.server.pm.pkg.PackageStateInternal
    public UUID getDomainSetId() {
        return this.mDomainSetId;
    }

    @Deprecated
    private void __metadata() {
    }

    public void setNotifyScreenOn(int isNotifyScreenOn, int userId) {
        PackageUserStateImpl st = modifyUserState(userId);
        st.isNotifyScreenOn = isNotifyScreenOn;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getNotifyScreenOn(int userId) {
        return readUserState(userId).isNotifyScreenOn();
    }
}
