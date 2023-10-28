package com.android.server.om;

import android.content.om.CriticalOverlayInfo;
import android.content.om.OverlayIdentifier;
import android.content.om.OverlayInfo;
import android.content.pm.overlay.OverlayPaths;
import android.content.pm.parsing.FrameworkParsingPackageUtils;
import android.os.FabricatedOverlayInfo;
import android.os.FabricatedOverlayInternal;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.content.om.OverlayConfig;
import com.android.internal.util.CollectionUtils;
import com.android.server.om.OverlayManagerSettings;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class OverlayManagerServiceImpl {
    private static final int FLAG_OVERLAY_IS_BEING_REPLACED = 2;
    @Deprecated
    private static final int FLAG_TARGET_IS_BEING_REPLACED = 1;
    private final String[] mDefaultOverlays;
    private final IdmapManager mIdmapManager;
    private final OverlayConfig mOverlayConfig;
    private final PackageManagerHelper mPackageManager;
    private final OverlayManagerSettings mSettings;

    private boolean mustReinitializeOverlay(AndroidPackage theTruth, OverlayInfo oldSettings) {
        boolean isMutable;
        if (oldSettings == null || !Objects.equals(theTruth.getOverlayTarget(), oldSettings.targetPackageName) || !Objects.equals(theTruth.getOverlayTargetOverlayableName(), oldSettings.targetOverlayableName) || oldSettings.isFabricated || (isMutable = isPackageConfiguredMutable(theTruth)) != oldSettings.isMutable) {
            return true;
        }
        if (!isMutable && isPackageConfiguredEnabled(theTruth) != oldSettings.isEnabled()) {
            return true;
        }
        return false;
    }

    private boolean mustReinitializeOverlay(FabricatedOverlayInfo theTruth, OverlayInfo oldSettings) {
        if (oldSettings == null || !Objects.equals(theTruth.targetPackageName, oldSettings.targetPackageName) || !Objects.equals(theTruth.targetOverlayable, oldSettings.targetOverlayableName)) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverlayManagerServiceImpl(PackageManagerHelper packageManager, IdmapManager idmapManager, OverlayManagerSettings settings, OverlayConfig overlayConfig, String[] defaultOverlays) {
        this.mPackageManager = packageManager;
        this.mIdmapManager = idmapManager;
        this.mSettings = settings;
        this.mOverlayConfig = overlayConfig;
        this.mDefaultOverlays = defaultOverlays;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArraySet<PackageAndUser> updateOverlaysForUser(int newUserId) {
        ArrayMap<String, AndroidPackage> userPackages;
        ArraySet<PackageAndUser> updatedTargets = new ArraySet<>();
        final ArrayMap<String, AndroidPackage> userPackages2 = this.mPackageManager.initializeForUser(newUserId);
        CollectionUtils.addAll(updatedTargets, removeOverlaysForUser(new Predicate() { // from class: com.android.server.om.OverlayManagerServiceImpl$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return OverlayManagerServiceImpl.lambda$updateOverlaysForUser$0(userPackages2, (OverlayInfo) obj);
            }
        }, newUserId));
        int n = userPackages2.size();
        for (int i = 0; i < n; i++) {
            AndroidPackage pkg = userPackages2.valueAt(i);
            try {
                CollectionUtils.addAll(updatedTargets, updatePackageOverlays(pkg, newUserId, 0));
                updatedTargets.add(new PackageAndUser(pkg.getPackageName(), newUserId));
            } catch (OperationFailedException e) {
                Slog.e("OverlayManager", "failed to initialize overlays of '" + pkg.getPackageName() + "' for user " + newUserId + "", e);
            }
        }
        for (FabricatedOverlayInfo info : getFabricatedOverlayInfos()) {
            try {
                CollectionUtils.addAll(updatedTargets, registerFabricatedOverlay(info, newUserId));
            } catch (OperationFailedException e2) {
                Slog.e("OverlayManager", "failed to initialize fabricated overlay of '" + info.path + "' for user " + newUserId + "", e2);
            }
        }
        ArraySet<String> enabledCategories = new ArraySet<>();
        ArrayMap<String, List<OverlayInfo>> userOverlays = this.mSettings.getOverlaysForUser(newUserId);
        int userOverlayTargetCount = userOverlays.size();
        for (int i2 = 0; i2 < userOverlayTargetCount; i2++) {
            List<OverlayInfo> overlayList = userOverlays.valueAt(i2);
            int overlayCount = overlayList != null ? overlayList.size() : 0;
            for (int j = 0; j < overlayCount; j++) {
                OverlayInfo oi = overlayList.get(j);
                if (oi.isEnabled()) {
                    enabledCategories.add(oi.category);
                }
            }
        }
        String[] strArr = this.mDefaultOverlays;
        int length = strArr.length;
        int i3 = 0;
        while (i3 < length) {
            String defaultOverlay = strArr[i3];
            try {
                OverlayIdentifier overlay = new OverlayIdentifier(defaultOverlay);
                OverlayInfo oi2 = this.mSettings.getOverlayInfo(overlay, newUserId);
                if (enabledCategories.contains(oi2.category)) {
                    userPackages = userPackages2;
                } else {
                    userPackages = userPackages2;
                    try {
                        Slog.w("OverlayManager", "Enabling default overlay '" + defaultOverlay + "' for target '" + oi2.targetPackageName + "' in category '" + oi2.category + "' for user " + newUserId);
                        this.mSettings.setEnabled(overlay, newUserId, true);
                        if (updateState(oi2, newUserId, 0)) {
                            CollectionUtils.add(updatedTargets, new PackageAndUser(oi2.targetPackageName, oi2.userId));
                        }
                    } catch (OverlayManagerSettings.BadKeyException e3) {
                        e = e3;
                        Slog.e("OverlayManager", "Failed to set default overlay '" + defaultOverlay + "' for user " + newUserId, e);
                        i3++;
                        userPackages2 = userPackages;
                    }
                }
            } catch (OverlayManagerSettings.BadKeyException e4) {
                e = e4;
                userPackages = userPackages2;
            }
            i3++;
            userPackages2 = userPackages;
        }
        cleanStaleResourceCache();
        return updatedTargets;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$updateOverlaysForUser$0(ArrayMap userPackages, OverlayInfo info) {
        return !userPackages.containsKey(info.packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onUserRemoved(int userId) {
        this.mSettings.removeUser(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<PackageAndUser> onPackageAdded(String pkgName, int userId) throws OperationFailedException {
        Set<PackageAndUser> updatedTargets = new ArraySet<>();
        updatedTargets.add(new PackageAndUser(pkgName, userId));
        updatedTargets.addAll(reconcileSettingsForPackage(pkgName, userId, 0));
        return updatedTargets;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<PackageAndUser> onPackageChanged(String pkgName, int userId) throws OperationFailedException {
        return reconcileSettingsForPackage(pkgName, userId, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<PackageAndUser> onPackageReplacing(String pkgName, int userId) throws OperationFailedException {
        return reconcileSettingsForPackage(pkgName, userId, 2);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<PackageAndUser> onPackageReplaced(String pkgName, int userId) throws OperationFailedException {
        return reconcileSettingsForPackage(pkgName, userId, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<PackageAndUser> onPackageRemoved(final String pkgName, int userId) {
        Set<PackageAndUser> targets = updateOverlaysForTarget(pkgName, userId, 0);
        return CollectionUtils.addAll(targets, removeOverlaysForUser(new Predicate() { // from class: com.android.server.om.OverlayManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean equals;
                equals = pkgName.equals(((OverlayInfo) obj).packageName);
                return equals;
            }
        }, userId));
    }

    private Set<PackageAndUser> removeOverlaysForUser(final Predicate<OverlayInfo> condition, final int userId) {
        List<OverlayInfo> overlays = this.mSettings.removeIf(new Predicate() { // from class: com.android.server.om.OverlayManagerServiceImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return OverlayManagerServiceImpl.lambda$removeOverlaysForUser$2(userId, condition, (OverlayInfo) obj);
            }
        });
        Set<PackageAndUser> targets = Collections.emptySet();
        int n = overlays.size();
        for (int i = 0; i < n; i++) {
            OverlayInfo info = overlays.get(i);
            targets = CollectionUtils.add(targets, new PackageAndUser(info.targetPackageName, userId));
            removeIdmapIfPossible(info);
        }
        return targets;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removeOverlaysForUser$2(int userId, Predicate condition, OverlayInfo io) {
        return userId == io.userId && condition.test(io);
    }

    private Set<PackageAndUser> updateOverlaysForTarget(String targetPackage, int userId, int flags) {
        boolean modified = false;
        List<OverlayInfo> overlays = this.mSettings.getOverlaysForTarget(targetPackage, userId);
        int n = overlays.size();
        for (int i = 0; i < n; i++) {
            OverlayInfo oi = overlays.get(i);
            try {
                modified |= updateState(oi, userId, flags);
            } catch (OverlayManagerSettings.BadKeyException e) {
                Slog.e("OverlayManager", "failed to update settings", e);
                modified |= this.mSettings.remove(oi.getOverlayIdentifier(), userId);
            }
        }
        if (!modified) {
            return Collections.emptySet();
        }
        return Set.of(new PackageAndUser(targetPackage, userId));
    }

    private Set<PackageAndUser> updatePackageOverlays(AndroidPackage pkg, int userId, int flags) throws OperationFailedException {
        Set<PackageAndUser> updatedTargets;
        if (pkg.getOverlayTarget() == null) {
            return Collections.emptySet();
        }
        Set<PackageAndUser> updatedTargets2 = Collections.emptySet();
        OverlayIdentifier overlay = new OverlayIdentifier(pkg.getPackageName());
        int priority = getPackageConfiguredPriority(pkg);
        try {
            CriticalOverlayInfo nullableOverlayInfo = this.mSettings.getNullableOverlayInfo(overlay, userId);
            if (mustReinitializeOverlay(pkg, (OverlayInfo) nullableOverlayInfo)) {
                if (nullableOverlayInfo == null) {
                    updatedTargets = updatedTargets2;
                } else {
                    updatedTargets = CollectionUtils.add(updatedTargets2, new PackageAndUser(((OverlayInfo) nullableOverlayInfo).targetPackageName, userId));
                }
                try {
                    nullableOverlayInfo = this.mSettings.init(overlay, userId, pkg.getOverlayTarget(), pkg.getOverlayTargetOverlayableName(), pkg.getBaseApkPath(), isPackageConfiguredMutable(pkg), isPackageConfiguredEnabled(pkg), getPackageConfiguredPriority(pkg), pkg.getOverlayCategory(), false);
                    updatedTargets2 = updatedTargets;
                } catch (OverlayManagerSettings.BadKeyException e) {
                    e = e;
                    throw new OperationFailedException("failed to update settings", e);
                }
            } else if (priority != ((OverlayInfo) nullableOverlayInfo).priority) {
                this.mSettings.setPriority(overlay, userId, priority);
                updatedTargets2 = CollectionUtils.add(updatedTargets2, new PackageAndUser(((OverlayInfo) nullableOverlayInfo).targetPackageName, userId));
            }
            try {
                if (updateState(nullableOverlayInfo, userId, flags)) {
                    return CollectionUtils.add(updatedTargets2, new PackageAndUser(((OverlayInfo) nullableOverlayInfo).targetPackageName, userId));
                }
                return updatedTargets2;
            } catch (OverlayManagerSettings.BadKeyException e2) {
                e = e2;
                throw new OperationFailedException("failed to update settings", e);
            }
        } catch (OverlayManagerSettings.BadKeyException e3) {
            e = e3;
        }
    }

    private Set<PackageAndUser> reconcileSettingsForPackage(String pkgName, int userId, int flags) throws OperationFailedException {
        Set<PackageAndUser> updatedTargets = Collections.emptySet();
        Set<PackageAndUser> updatedTargets2 = CollectionUtils.addAll(updatedTargets, updateOverlaysForTarget(pkgName, userId, flags));
        AndroidPackage pkg = this.mPackageManager.getPackageForUser(pkgName, userId);
        if (pkg == null) {
            return onPackageRemoved(pkgName, userId);
        }
        return CollectionUtils.addAll(updatedTargets2, updatePackageOverlays(pkg, userId, flags));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverlayInfo getOverlayInfo(OverlayIdentifier packageName, int userId) {
        try {
            return this.mSettings.getOverlayInfo(packageName, userId);
        } catch (OverlayManagerSettings.BadKeyException e) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<OverlayInfo> getOverlayInfosForTarget(String targetPackageName, int userId) {
        return this.mSettings.getOverlaysForTarget(targetPackageName, userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Map<String, List<OverlayInfo>> getOverlaysForUser(int userId) {
        return this.mSettings.getOverlaysForUser(userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<PackageAndUser> setEnabled(OverlayIdentifier overlay, boolean enable, int userId) throws OperationFailedException {
        try {
            OverlayInfo oi = this.mSettings.getOverlayInfo(overlay, userId);
            if (!oi.isMutable) {
                throw new OperationFailedException("cannot enable immutable overlay packages in runtime");
            }
            boolean modified = this.mSettings.setEnabled(overlay, userId, enable);
            if (modified | updateState(oi, userId, 0)) {
                return Set.of(new PackageAndUser(oi.targetPackageName, userId));
            }
            return Set.of();
        } catch (OverlayManagerSettings.BadKeyException e) {
            throw new OperationFailedException("failed to update settings", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Optional<PackageAndUser> setEnabledExclusive(OverlayIdentifier overlay, boolean withinCategory, int userId) throws OperationFailedException {
        try {
            OverlayInfo enabledInfo = this.mSettings.getOverlayInfo(overlay, userId);
            if (!enabledInfo.isMutable) {
                throw new OperationFailedException("cannot enable immutable overlay packages in runtime");
            }
            List<OverlayInfo> allOverlays = getOverlayInfosForTarget(enabledInfo.targetPackageName, userId);
            allOverlays.remove(enabledInfo);
            boolean modified = false;
            for (int i = 0; i < allOverlays.size(); i++) {
                OverlayInfo disabledInfo = allOverlays.get(i);
                OverlayIdentifier disabledOverlay = disabledInfo.getOverlayIdentifier();
                if (disabledInfo.isMutable && (!withinCategory || Objects.equals(disabledInfo.category, enabledInfo.category))) {
                    modified = modified | this.mSettings.setEnabled(disabledOverlay, userId, false) | updateState(disabledInfo, userId, 0);
                }
            }
            if (modified | this.mSettings.setEnabled(overlay, userId, true) | updateState(enabledInfo, userId, 0)) {
                return Optional.of(new PackageAndUser(enabledInfo.targetPackageName, userId));
            }
            return Optional.empty();
        } catch (OverlayManagerSettings.BadKeyException e) {
            throw new OperationFailedException("failed to update settings", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<PackageAndUser> registerFabricatedOverlay(FabricatedOverlayInternal overlay) throws OperationFailedException {
        int[] users;
        if (FrameworkParsingPackageUtils.validateName(overlay.overlayName, false, true) != null) {
            throw new OperationFailedException("overlay name can only consist of alphanumeric characters, '_', and '.'");
        }
        FabricatedOverlayInfo info = this.mIdmapManager.createFabricatedOverlay(overlay);
        if (info == null) {
            throw new OperationFailedException("failed to create fabricated overlay");
        }
        Set<PackageAndUser> updatedTargets = new ArraySet<>();
        for (int userId : this.mSettings.getUsers()) {
            updatedTargets.addAll(registerFabricatedOverlay(info, userId));
        }
        return updatedTargets;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [545=4] */
    private Set<PackageAndUser> registerFabricatedOverlay(FabricatedOverlayInfo info, int userId) throws OperationFailedException {
        Set<PackageAndUser> updatedTargets;
        CriticalOverlayInfo criticalOverlayInfo;
        OverlayIdentifier overlayIdentifier = new OverlayIdentifier(info.packageName, info.overlayName);
        Set<PackageAndUser> arraySet = new ArraySet<>();
        CriticalOverlayInfo nullableOverlayInfo = this.mSettings.getNullableOverlayInfo(overlayIdentifier, userId);
        if (nullableOverlayInfo != null && !((OverlayInfo) nullableOverlayInfo).isFabricated) {
            throw new OperationFailedException("non-fabricated overlay with name '" + ((OverlayInfo) nullableOverlayInfo).overlayName + "' already present in '" + ((OverlayInfo) nullableOverlayInfo).packageName + "'");
        }
        try {
            if (mustReinitializeOverlay(info, (OverlayInfo) nullableOverlayInfo)) {
                if (nullableOverlayInfo != null) {
                    try {
                        arraySet.add(new PackageAndUser(((OverlayInfo) nullableOverlayInfo).targetPackageName, userId));
                    } catch (OverlayManagerSettings.BadKeyException e) {
                        e = e;
                        throw new OperationFailedException("failed to update settings", e);
                    }
                }
                try {
                    updatedTargets = arraySet;
                    try {
                        criticalOverlayInfo = this.mSettings.init(overlayIdentifier, userId, info.targetPackageName, info.targetOverlayable, info.path, true, false, Integer.MAX_VALUE, null, true);
                    } catch (OverlayManagerSettings.BadKeyException e2) {
                        e = e2;
                        throw new OperationFailedException("failed to update settings", e);
                    }
                } catch (OverlayManagerSettings.BadKeyException e3) {
                    e = e3;
                }
            } else {
                updatedTargets = arraySet;
                try {
                    this.mSettings.setBaseCodePath(overlayIdentifier, userId, info.path);
                    criticalOverlayInfo = nullableOverlayInfo;
                } catch (OverlayManagerSettings.BadKeyException e4) {
                    e = e4;
                    throw new OperationFailedException("failed to update settings", e);
                }
            }
            try {
                if (updateState(criticalOverlayInfo, userId, 0)) {
                    Set<PackageAndUser> updatedTargets2 = updatedTargets;
                    try {
                        updatedTargets2.add(new PackageAndUser(((OverlayInfo) criticalOverlayInfo).targetPackageName, userId));
                        return updatedTargets2;
                    } catch (OverlayManagerSettings.BadKeyException e5) {
                        e = e5;
                        throw new OperationFailedException("failed to update settings", e);
                    }
                }
                return updatedTargets;
            } catch (OverlayManagerSettings.BadKeyException e6) {
                e = e6;
            }
        } catch (OverlayManagerSettings.BadKeyException e7) {
            e = e7;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<PackageAndUser> unregisterFabricatedOverlay(OverlayIdentifier overlay) {
        int[] users;
        Set<PackageAndUser> updatedTargets = new ArraySet<>();
        for (int userId : this.mSettings.getUsers()) {
            updatedTargets.addAll(unregisterFabricatedOverlay(overlay, userId));
        }
        return updatedTargets;
    }

    private Set<PackageAndUser> unregisterFabricatedOverlay(OverlayIdentifier overlay, int userId) {
        OverlayInfo oi = this.mSettings.getNullableOverlayInfo(overlay, userId);
        if (oi != null) {
            this.mSettings.remove(overlay, userId);
            if (oi.isEnabled()) {
                return Set.of(new PackageAndUser(oi.targetPackageName, userId));
            }
        }
        return Set.of();
    }

    private void cleanStaleResourceCache() {
        Set<String> fabricatedPaths = this.mSettings.getAllBaseCodePaths();
        for (FabricatedOverlayInfo info : this.mIdmapManager.getFabricatedOverlayInfos()) {
            if (!fabricatedPaths.contains(info.path)) {
                this.mIdmapManager.deleteFabricatedOverlay(info.path);
            }
        }
    }

    private List<FabricatedOverlayInfo> getFabricatedOverlayInfos() {
        final Set<String> fabricatedPaths = this.mSettings.getAllBaseCodePaths();
        ArrayList<FabricatedOverlayInfo> infos = new ArrayList<>(this.mIdmapManager.getFabricatedOverlayInfos());
        infos.removeIf(new Predicate() { // from class: com.android.server.om.OverlayManagerServiceImpl$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return OverlayManagerServiceImpl.lambda$getFabricatedOverlayInfos$3(fabricatedPaths, (FabricatedOverlayInfo) obj);
            }
        });
        return infos;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$getFabricatedOverlayInfos$3(Set fabricatedPaths, FabricatedOverlayInfo info) {
        return !fabricatedPaths.contains(info.path);
    }

    private boolean isPackageConfiguredMutable(AndroidPackage overlay) {
        return this.mOverlayConfig.isMutable(overlay.getPackageName());
    }

    private int getPackageConfiguredPriority(AndroidPackage overlay) {
        return this.mOverlayConfig.getPriority(overlay.getPackageName());
    }

    private boolean isPackageConfiguredEnabled(AndroidPackage overlay) {
        return this.mOverlayConfig.isEnabled(overlay.getPackageName());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Optional<PackageAndUser> setPriority(OverlayIdentifier overlay, OverlayIdentifier newParentOverlay, int userId) throws OperationFailedException {
        try {
            OverlayInfo overlayInfo = this.mSettings.getOverlayInfo(overlay, userId);
            if (!overlayInfo.isMutable) {
                throw new OperationFailedException("cannot change priority of an immutable overlay package at runtime");
            }
            if (this.mSettings.setPriority(overlay, newParentOverlay, userId)) {
                return Optional.of(new PackageAndUser(overlayInfo.targetPackageName, userId));
            }
            return Optional.empty();
        } catch (OverlayManagerSettings.BadKeyException e) {
            throw new OperationFailedException("failed to update settings", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<PackageAndUser> setHighestPriority(OverlayIdentifier overlay, int userId) throws OperationFailedException {
        try {
            OverlayInfo overlayInfo = this.mSettings.getOverlayInfo(overlay, userId);
            if (!overlayInfo.isMutable) {
                throw new OperationFailedException("cannot change priority of an immutable overlay package at runtime");
            }
            if (this.mSettings.setHighestPriority(overlay, userId)) {
                return Set.of(new PackageAndUser(overlayInfo.targetPackageName, userId));
            }
            return Set.of();
        } catch (OverlayManagerSettings.BadKeyException e) {
            throw new OperationFailedException("failed to update settings", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Optional<PackageAndUser> setLowestPriority(OverlayIdentifier overlay, int userId) throws OperationFailedException {
        try {
            OverlayInfo overlayInfo = this.mSettings.getOverlayInfo(overlay, userId);
            if (!overlayInfo.isMutable) {
                throw new OperationFailedException("cannot change priority of an immutable overlay package at runtime");
            }
            if (this.mSettings.setLowestPriority(overlay, userId)) {
                return Optional.of(new PackageAndUser(overlayInfo.targetPackageName, userId));
            }
            return Optional.empty();
        } catch (OverlayManagerSettings.BadKeyException e) {
            throw new OperationFailedException("failed to update settings", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, DumpState dumpState) {
        OverlayIdentifier id;
        OverlayInfo oi;
        Pair<OverlayIdentifier, String> overlayIdmap = null;
        if (dumpState.getPackageName() != null && (oi = this.mSettings.getNullableOverlayInfo((id = new OverlayIdentifier(dumpState.getPackageName(), dumpState.getOverlayName())), 0)) != null) {
            overlayIdmap = new Pair<>(id, oi.baseCodePath);
        }
        this.mSettings.dump(pw, dumpState);
        if (dumpState.getField() == null) {
            Set<Pair<OverlayIdentifier, String>> allIdmaps = overlayIdmap != null ? Set.of(overlayIdmap) : this.mSettings.getAllIdentifiersAndBaseCodePaths();
            for (Pair<OverlayIdentifier, String> pair : allIdmaps) {
                pw.println("IDMAP OF " + pair.first);
                String dump = this.mIdmapManager.dumpIdmap((String) pair.second);
                if (dump != null) {
                    pw.println(dump);
                } else {
                    OverlayInfo oi2 = this.mSettings.getNullableOverlayInfo((OverlayIdentifier) pair.first, 0);
                    pw.println((oi2 == null || this.mIdmapManager.idmapExists(oi2)) ? "<internal error>" : "<missing idmap>");
                }
            }
        }
        if (overlayIdmap == null) {
            pw.println("Default overlays: " + TextUtils.join(";", this.mDefaultOverlays));
        }
        if (dumpState.getPackageName() == null) {
            this.mOverlayConfig.dump(pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String[] getDefaultOverlayPackages() {
        return this.mDefaultOverlays;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeIdmapForOverlay(OverlayIdentifier overlay, int userId) throws OperationFailedException {
        try {
            OverlayInfo oi = this.mSettings.getOverlayInfo(overlay, userId);
            removeIdmapIfPossible(oi);
        } catch (OverlayManagerSettings.BadKeyException e) {
            throw new OperationFailedException("failed to update settings", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverlayPaths getEnabledOverlayPaths(String targetPackageName, int userId) {
        List<OverlayInfo> overlays = this.mSettings.getOverlaysForTarget(targetPackageName, userId);
        OverlayPaths.Builder paths = new OverlayPaths.Builder();
        int n = overlays.size();
        for (int i = 0; i < n; i++) {
            OverlayInfo oi = overlays.get(i);
            if (oi.isEnabled()) {
                if (oi.isFabricated()) {
                    paths.addNonApkPath(oi.baseCodePath);
                } else {
                    paths.addApkPath(oi.baseCodePath);
                }
            }
        }
        return paths.build();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverlayPaths getEnabledOverlayPathsExcludeSpRRO(String targetPackageName, int userId, String excludeSpOverlayName) {
        List<OverlayInfo> overlays = this.mSettings.getOverlaysForTarget(targetPackageName, userId);
        OverlayPaths.Builder paths = new OverlayPaths.Builder();
        int n = overlays.size();
        for (int i = 0; i < n; i++) {
            OverlayInfo oi = overlays.get(i);
            if (oi.isEnabled() && !excludeSpOverlayName.equals(oi.getPackageName())) {
                if (oi.isFabricated()) {
                    paths.addNonApkPath(oi.baseCodePath);
                } else {
                    paths.addApkPath(oi.baseCodePath);
                }
            }
        }
        return paths.build();
    }

    private boolean updateState(CriticalOverlayInfo info, int userId, int flags) throws OverlayManagerSettings.BadKeyException {
        boolean modified;
        OverlayIdentifier overlay = info.getOverlayIdentifier();
        AndroidPackage targetPackage = this.mPackageManager.getPackageForUser(info.getTargetPackageName(), userId);
        AndroidPackage overlayPackage = this.mPackageManager.getPackageForUser(info.getPackageName(), userId);
        if (overlayPackage != null) {
            boolean modified2 = false | this.mSettings.setCategory(overlay, userId, overlayPackage.getOverlayCategory());
            if (info.isFabricated()) {
                modified = modified2;
            } else {
                modified = modified2 | this.mSettings.setBaseCodePath(overlay, userId, overlayPackage.getBaseApkPath());
            }
            OverlayInfo updatedOverlayInfo = this.mSettings.getOverlayInfo(overlay, userId);
            if (targetPackage != null && (!PackageManagerService.PLATFORM_PACKAGE_NAME.equals(info.getTargetPackageName()) || isPackageConfiguredMutable(overlayPackage))) {
                modified |= this.mIdmapManager.createIdmap(targetPackage, overlayPackage, updatedOverlayInfo.baseCodePath, overlay.getOverlayName(), userId);
            }
            int currentState = this.mSettings.getState(overlay, userId);
            int newState = calculateNewState(updatedOverlayInfo, targetPackage, userId, flags);
            if (currentState != newState) {
                return modified | this.mSettings.setState(overlay, userId, newState);
            }
            return modified;
        }
        removeIdmapIfPossible(this.mSettings.getOverlayInfo(overlay, userId));
        return this.mSettings.remove(overlay, userId);
    }

    private int calculateNewState(OverlayInfo info, AndroidPackage targetPackage, int userId, int flags) throws OverlayManagerSettings.BadKeyException {
        if ((flags & 1) != 0) {
            return 4;
        }
        if ((flags & 2) != 0) {
            return 5;
        }
        if (targetPackage == null) {
            return 0;
        }
        if (!this.mIdmapManager.idmapExists(info)) {
            return 1;
        }
        boolean enabled = this.mSettings.getEnabled(info.getOverlayIdentifier(), userId);
        return enabled ? 3 : 2;
    }

    private void removeIdmapIfPossible(OverlayInfo oi) {
        if (!this.mIdmapManager.idmapExists(oi)) {
            return;
        }
        int[] userIds = this.mSettings.getUsers();
        for (int userId : userIds) {
            try {
                OverlayInfo tmp = this.mSettings.getOverlayInfo(oi.getOverlayIdentifier(), userId);
                if (tmp != null && tmp.isEnabled()) {
                    return;
                }
            } catch (OverlayManagerSettings.BadKeyException e) {
            }
        }
        this.mIdmapManager.removeIdmap(oi, oi.userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public static final class OperationFailedException extends Exception {
        OperationFailedException(String message) {
            super(message);
        }

        OperationFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
