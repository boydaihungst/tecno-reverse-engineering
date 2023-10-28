package com.android.server.pm;

import android.content.pm.SigningDetails;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.LegacyPermissionState;
import com.android.server.pm.pkg.PackageStateInternal;
import com.android.server.pm.pkg.SharedUserApi;
import com.android.server.pm.pkg.component.ComponentMutateUtils;
import com.android.server.pm.pkg.component.ParsedProcess;
import com.android.server.pm.pkg.component.ParsedProcessImpl;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.utils.SnapshotCache;
import com.android.server.utils.Watchable;
import com.android.server.utils.WatchedArraySet;
import com.android.server.utils.Watcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import libcore.util.EmptyArray;
/* loaded from: classes2.dex */
public final class SharedUserSetting extends SettingBase implements SharedUserApi {
    int mAppId;
    final WatchedArraySet<PackageSetting> mDisabledPackages;
    private final SnapshotCache<WatchedArraySet<PackageSetting>> mDisabledPackagesSnapshot;
    private final Watcher mObserver;
    private final WatchedArraySet<PackageSetting> mPackages;
    private final SnapshotCache<WatchedArraySet<PackageSetting>> mPackagesSnapshot;
    private final SnapshotCache<SharedUserSetting> mSnapshot;
    final String name;
    final ArrayMap<String, ParsedProcess> processes;
    int seInfoTargetSdkVersion;
    final PackageSignatures signatures;
    Boolean signaturesChanged;
    int uidFlags;
    int uidPrivateFlags;

    private SnapshotCache<SharedUserSetting> makeCache() {
        return new SnapshotCache<SharedUserSetting>(this, this) { // from class: com.android.server.pm.SharedUserSetting.2
            /* JADX DEBUG: Method merged with bridge method */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.android.server.utils.SnapshotCache
            public SharedUserSetting createSnapshot() {
                return new SharedUserSetting();
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SharedUserSetting(String _name, int _pkgFlags, int _pkgPrivateFlags) {
        super(_pkgFlags, _pkgPrivateFlags);
        this.mObserver = new Watcher() { // from class: com.android.server.pm.SharedUserSetting.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                SharedUserSetting.this.onChanged();
            }
        };
        this.signatures = new PackageSignatures();
        this.uidFlags = _pkgFlags;
        this.uidPrivateFlags = _pkgPrivateFlags;
        this.name = _name;
        this.seInfoTargetSdkVersion = 10000;
        WatchedArraySet<PackageSetting> watchedArraySet = new WatchedArraySet<>();
        this.mPackages = watchedArraySet;
        this.mPackagesSnapshot = new SnapshotCache.Auto(watchedArraySet, watchedArraySet, "SharedUserSetting.packages");
        WatchedArraySet<PackageSetting> watchedArraySet2 = new WatchedArraySet<>();
        this.mDisabledPackages = watchedArraySet2;
        this.mDisabledPackagesSnapshot = new SnapshotCache.Auto(watchedArraySet2, watchedArraySet2, "SharedUserSetting.mDisabledPackages");
        this.processes = new ArrayMap<>();
        registerObservers();
        this.mSnapshot = makeCache();
    }

    private SharedUserSetting(SharedUserSetting orig) {
        super(orig);
        this.mObserver = new Watcher() { // from class: com.android.server.pm.SharedUserSetting.1
            @Override // com.android.server.utils.Watcher
            public void onChange(Watchable what) {
                SharedUserSetting.this.onChanged();
            }
        };
        PackageSignatures packageSignatures = new PackageSignatures();
        this.signatures = packageSignatures;
        this.name = orig.name;
        this.mAppId = orig.mAppId;
        this.uidFlags = orig.uidFlags;
        this.uidPrivateFlags = orig.uidPrivateFlags;
        this.mPackages = orig.mPackagesSnapshot.snapshot();
        this.mPackagesSnapshot = new SnapshotCache.Sealed();
        this.mDisabledPackages = orig.mDisabledPackagesSnapshot.snapshot();
        this.mDisabledPackagesSnapshot = new SnapshotCache.Sealed();
        packageSignatures.mSigningDetails = orig.signatures.mSigningDetails;
        this.signaturesChanged = orig.signaturesChanged;
        this.processes = new ArrayMap<>(orig.processes);
        this.mSnapshot = new SnapshotCache.Sealed();
    }

    private void registerObservers() {
        this.mPackages.registerObserver(this.mObserver);
        this.mDisabledPackages.registerObserver(this.mObserver);
    }

    /* JADX DEBUG: Method merged with bridge method */
    @Override // com.android.server.utils.Snappable
    public SharedUserSetting snapshot() {
        return this.mSnapshot.snapshot();
    }

    public String toString() {
        return "SharedUserSetting{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.name + SliceClientPermissions.SliceAuthority.DELIMITER + this.mAppId + "}";
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(CompanionMessage.MESSAGE_ID, this.mAppId);
        proto.write(1138166333442L, this.name);
        proto.end(token);
    }

    void addProcesses(Map<String, ParsedProcess> newProcs) {
        if (newProcs != null) {
            for (String key : newProcs.keySet()) {
                ParsedProcess newProc = newProcs.get(key);
                ParsedProcess proc = this.processes.get(newProc.getName());
                if (proc == null) {
                    this.processes.put(newProc.getName(), new ParsedProcessImpl(newProc));
                } else {
                    ComponentMutateUtils.addStateFrom(proc, newProc);
                }
            }
            onChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removePackage(PackageSetting packageSetting) {
        if (!this.mPackages.remove(packageSetting)) {
            return false;
        }
        if ((getFlags() & packageSetting.getFlags()) != 0) {
            int aggregatedFlags = this.uidFlags;
            for (int i = 0; i < this.mPackages.size(); i++) {
                PackageSetting ps = this.mPackages.valueAt(i);
                aggregatedFlags |= ps.getFlags();
            }
            setFlags(aggregatedFlags);
        }
        int aggregatedFlags2 = getPrivateFlags();
        if ((aggregatedFlags2 & packageSetting.getPrivateFlags()) != 0) {
            int aggregatedPrivateFlags = this.uidPrivateFlags;
            for (int i2 = 0; i2 < this.mPackages.size(); i2++) {
                PackageSetting ps2 = this.mPackages.valueAt(i2);
                aggregatedPrivateFlags |= ps2.getPrivateFlags();
            }
            setPrivateFlags(aggregatedPrivateFlags);
        }
        updateProcesses();
        onChanged();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addPackage(PackageSetting packageSetting) {
        if (this.mPackages.size() == 0 && packageSetting.getPkg() != null) {
            this.seInfoTargetSdkVersion = packageSetting.getPkg().getTargetSdkVersion();
        }
        if (this.mPackages.add(packageSetting)) {
            setFlags(getFlags() | packageSetting.getFlags());
            setPrivateFlags(getPrivateFlags() | packageSetting.getPrivateFlags());
            onChanged();
        }
        if (packageSetting.getPkg() != null) {
            addProcesses(packageSetting.getPkg().getProcesses());
        }
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public List<AndroidPackage> getPackages() {
        WatchedArraySet<PackageSetting> watchedArraySet = this.mPackages;
        if (watchedArraySet == null || watchedArraySet.size() == 0) {
            return Collections.emptyList();
        }
        ArrayList<AndroidPackage> pkgList = new ArrayList<>(this.mPackages.size());
        for (int i = 0; i < this.mPackages.size(); i++) {
            PackageSetting ps = this.mPackages.valueAt(i);
            if (ps != null && ps.getPkg() != null) {
                pkgList.add(ps.getPkg());
            }
        }
        return pkgList;
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public boolean isPrivileged() {
        return (getPrivateFlags() & 8) != 0;
    }

    public boolean isSingleUser() {
        if (this.mPackages.size() == 1 && this.mDisabledPackages.size() <= 1) {
            if (this.mDisabledPackages.size() == 1) {
                AndroidPackage pkg = this.mDisabledPackages.valueAt(0).getPkg();
                return pkg != null && pkg.isLeavingSharedUid();
            }
            return true;
        }
        return false;
    }

    public void fixSeInfoLocked() {
        WatchedArraySet<PackageSetting> watchedArraySet = this.mPackages;
        if (watchedArraySet == null || watchedArraySet.size() == 0) {
            return;
        }
        for (int i = 0; i < this.mPackages.size(); i++) {
            PackageSetting ps = this.mPackages.valueAt(i);
            if (ps != null && ps.getPkg() != null && ps.getPkg().getTargetSdkVersion() < this.seInfoTargetSdkVersion) {
                this.seInfoTargetSdkVersion = ps.getPkg().getTargetSdkVersion();
                onChanged();
            }
        }
        for (int i2 = 0; i2 < this.mPackages.size(); i2++) {
            PackageSetting ps2 = this.mPackages.valueAt(i2);
            if (ps2 != null && ps2.getPkg() != null) {
                boolean isPrivileged = isPrivileged() | ps2.getPkg().isPrivileged();
                ps2.getPkgState().setOverrideSeInfo(SELinuxMMAC.getSeInfo(ps2.getPkg(), isPrivileged, this.seInfoTargetSdkVersion));
                onChanged();
            }
        }
    }

    public void updateProcesses() {
        this.processes.clear();
        for (int i = this.mPackages.size() - 1; i >= 0; i--) {
            AndroidPackage pkg = this.mPackages.valueAt(i).getPkg();
            if (pkg != null) {
                addProcesses(pkg.getProcesses());
            }
        }
    }

    public int[] getNotInstalledUserIds() {
        int[] excludedUserIds = null;
        for (int i = 0; i < this.mPackages.size(); i++) {
            PackageSetting ps = this.mPackages.valueAt(i);
            int[] userIds = ps.getNotInstalledUserIds();
            if (excludedUserIds == null) {
                excludedUserIds = userIds;
            } else {
                int[] excludedUserIds2 = excludedUserIds;
                for (int userId : excludedUserIds) {
                    if (!ArrayUtils.contains(userIds, userId)) {
                        excludedUserIds2 = ArrayUtils.removeInt(excludedUserIds2, userId);
                    }
                }
                excludedUserIds = excludedUserIds2;
            }
        }
        return excludedUserIds == null ? EmptyArray.INT : excludedUserIds;
    }

    public SharedUserSetting updateFrom(SharedUserSetting sharedUser) {
        super.copySettingBase(sharedUser);
        this.mAppId = sharedUser.mAppId;
        this.uidFlags = sharedUser.uidFlags;
        this.uidPrivateFlags = sharedUser.uidPrivateFlags;
        this.seInfoTargetSdkVersion = sharedUser.seInfoTargetSdkVersion;
        this.mPackages.clear();
        this.mPackages.addAll(sharedUser.mPackages);
        this.signaturesChanged = sharedUser.signaturesChanged;
        ArrayMap<String, ParsedProcess> arrayMap = sharedUser.processes;
        if (arrayMap != null) {
            int numProcs = arrayMap.size();
            this.processes.clear();
            this.processes.ensureCapacity(numProcs);
            for (int i = 0; i < numProcs; i++) {
                ParsedProcess proc = new ParsedProcessImpl(sharedUser.processes.valueAt(i));
                this.processes.put(proc.getName(), proc);
            }
        } else {
            this.processes.clear();
        }
        onChanged();
        return this;
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public String getName() {
        return this.name;
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public int getAppId() {
        return this.mAppId;
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public int getUidFlags() {
        return this.uidFlags;
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public int getPrivateUidFlags() {
        return this.uidPrivateFlags;
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public int getSeInfoTargetSdkVersion() {
        return this.seInfoTargetSdkVersion;
    }

    public WatchedArraySet<PackageSetting> getPackageSettings() {
        return this.mPackages;
    }

    public WatchedArraySet<PackageSetting> getDisabledPackageSettings() {
        return this.mDisabledPackages;
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public ArraySet<? extends PackageStateInternal> getPackageStates() {
        return this.mPackages.untrackedStorage();
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public ArraySet<? extends PackageStateInternal> getDisabledPackageStates() {
        return this.mDisabledPackages.untrackedStorage();
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public SigningDetails getSigningDetails() {
        return this.signatures.mSigningDetails;
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public ArrayMap<String, ParsedProcess> getProcesses() {
        return this.processes;
    }

    @Override // com.android.server.pm.pkg.SharedUserApi
    public LegacyPermissionState getSharedUserLegacyPermissionState() {
        return super.getLegacyPermissionState();
    }
}
