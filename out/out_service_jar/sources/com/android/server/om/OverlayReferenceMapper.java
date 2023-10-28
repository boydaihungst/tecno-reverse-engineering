package com.android.server.om;

import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.util.CollectionUtils;
import com.android.server.SystemConfig;
import com.android.server.om.OverlayActorEnforcer;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/* loaded from: classes2.dex */
public class OverlayReferenceMapper {
    private static final String TAG = "OverlayReferenceMapper";
    private boolean mDeferRebuild;
    private final Provider mProvider;
    private final Object mLock = new Object();
    private final ArrayMap<String, ArrayMap<String, ArraySet<String>>> mActorToTargetToOverlays = new ArrayMap<>();
    private final ArrayMap<String, Set<String>> mActorPkgToPkgs = new ArrayMap<>();

    /* loaded from: classes2.dex */
    public interface Provider {
        String getActorPkg(String str);

        Map<String, Set<String>> getTargetToOverlayables(AndroidPackage androidPackage);
    }

    public OverlayReferenceMapper(boolean deferRebuild, Provider provider) {
        this.mDeferRebuild = deferRebuild;
        this.mProvider = provider != null ? provider : new Provider() { // from class: com.android.server.om.OverlayReferenceMapper.1
            @Override // com.android.server.om.OverlayReferenceMapper.Provider
            public String getActorPkg(String actor) {
                Map<String, Map<String, String>> namedActors = SystemConfig.getInstance().getNamedActors();
                Pair<String, OverlayActorEnforcer.ActorState> actorPair = OverlayActorEnforcer.getPackageNameForActor(actor, namedActors);
                return (String) actorPair.first;
            }

            @Override // com.android.server.om.OverlayReferenceMapper.Provider
            public Map<String, Set<String>> getTargetToOverlayables(AndroidPackage pkg) {
                String target = pkg.getOverlayTarget();
                if (TextUtils.isEmpty(target)) {
                    return Collections.emptyMap();
                }
                String overlayable = pkg.getOverlayTargetOverlayableName();
                Map<String, Set<String>> targetToOverlayables = new HashMap<>();
                Set<String> overlayables = new HashSet<>();
                overlayables.add(overlayable);
                targetToOverlayables.put(target, overlayables);
                return targetToOverlayables;
            }
        };
    }

    public Map<String, Set<String>> getActorPkgToPkgs() {
        return this.mActorPkgToPkgs;
    }

    public boolean isValidActor(String targetName, String actorPackageName) {
        boolean z;
        synchronized (this.mLock) {
            ensureMapBuilt();
            Set<String> validSet = this.mActorPkgToPkgs.get(actorPackageName);
            z = validSet != null && validSet.contains(targetName);
        }
        return z;
    }

    public ArraySet<String> addPkg(AndroidPackage pkg, Map<String, AndroidPackage> otherPkgs) {
        ArraySet<String> changed;
        synchronized (this.mLock) {
            changed = new ArraySet<>();
            if (!pkg.getOverlayables().isEmpty()) {
                addTarget(pkg, otherPkgs, changed);
            }
            if (!this.mProvider.getTargetToOverlayables(pkg).isEmpty()) {
                addOverlay(pkg, otherPkgs, changed);
            }
            if (!this.mDeferRebuild) {
                rebuild();
            }
        }
        return changed;
    }

    public ArraySet<String> removePkg(String pkgName) {
        ArraySet<String> changedPackages;
        synchronized (this.mLock) {
            changedPackages = new ArraySet<>();
            removeTarget(pkgName, changedPackages);
            removeOverlay(pkgName, changedPackages);
            if (!this.mDeferRebuild) {
                rebuild();
            }
        }
        return changedPackages;
    }

    private void removeTarget(String target, Collection<String> changedPackages) {
        synchronized (this.mLock) {
            int size = this.mActorToTargetToOverlays.size();
            for (int index = size - 1; index >= 0; index--) {
                ArrayMap<String, ArraySet<String>> targetToOverlays = this.mActorToTargetToOverlays.valueAt(index);
                if (targetToOverlays.containsKey(target)) {
                    targetToOverlays.remove(target);
                    String actor = this.mActorToTargetToOverlays.keyAt(index);
                    changedPackages.add(this.mProvider.getActorPkg(actor));
                    if (targetToOverlays.isEmpty()) {
                        this.mActorToTargetToOverlays.removeAt(index);
                    }
                }
            }
        }
    }

    private void addTarget(AndroidPackage targetPkg, Map<String, AndroidPackage> otherPkgs, Collection<String> changedPackages) {
        synchronized (this.mLock) {
            String target = targetPkg.getPackageName();
            removeTarget(target, changedPackages);
            Map<String, String> overlayablesToActors = targetPkg.getOverlayables();
            for (String overlayable : overlayablesToActors.keySet()) {
                String actor = overlayablesToActors.get(overlayable);
                addTargetToMap(actor, target, changedPackages);
                for (AndroidPackage overlayPkg : otherPkgs.values()) {
                    Map<String, Set<String>> targetToOverlayables = this.mProvider.getTargetToOverlayables(overlayPkg);
                    Set<String> overlayables = targetToOverlayables.get(target);
                    if (!CollectionUtils.isEmpty(overlayables)) {
                        if (overlayables.contains(overlayable)) {
                            String overlay = overlayPkg.getPackageName();
                            addOverlayToMap(actor, target, overlay, changedPackages);
                        }
                    }
                }
            }
        }
    }

    private void removeOverlay(String overlay, Collection<String> changedPackages) {
        synchronized (this.mLock) {
            int actorsSize = this.mActorToTargetToOverlays.size();
            for (int actorIndex = actorsSize - 1; actorIndex >= 0; actorIndex--) {
                ArrayMap<String, ArraySet<String>> targetToOverlays = this.mActorToTargetToOverlays.valueAt(actorIndex);
                int targetsSize = targetToOverlays.size();
                for (int targetIndex = targetsSize - 1; targetIndex >= 0; targetIndex--) {
                    Set<String> overlays = targetToOverlays.valueAt(targetIndex);
                    if (overlays.remove(overlay)) {
                        String actor = this.mActorToTargetToOverlays.keyAt(actorIndex);
                        changedPackages.add(this.mProvider.getActorPkg(actor));
                    }
                }
                if (targetToOverlays.isEmpty()) {
                    this.mActorToTargetToOverlays.removeAt(actorIndex);
                }
            }
        }
    }

    private void addOverlay(AndroidPackage overlayPkg, Map<String, AndroidPackage> otherPkgs, Collection<String> changedPackages) {
        String overlay;
        synchronized (this.mLock) {
            try {
                try {
                    overlay = overlayPkg.getPackageName();
                    removeOverlay(overlay, changedPackages);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
                try {
                    Map<String, Set<String>> targetToOverlayables = this.mProvider.getTargetToOverlayables(overlayPkg);
                    for (Map.Entry<String, Set<String>> entry : targetToOverlayables.entrySet()) {
                        String target = entry.getKey();
                        Set<String> overlayables = entry.getValue();
                        AndroidPackage targetPkg = otherPkgs.get(target);
                        if (targetPkg != null) {
                            String targetPkgName = targetPkg.getPackageName();
                            Map<String, String> overlayableToActor = targetPkg.getOverlayables();
                            for (String overlayable : overlayables) {
                                String actor = overlayableToActor.get(overlayable);
                                if (!TextUtils.isEmpty(actor)) {
                                    addOverlayToMap(actor, targetPkgName, overlay, changedPackages);
                                    targetToOverlayables = targetToOverlayables;
                                }
                            }
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public void rebuildIfDeferred() {
        synchronized (this.mLock) {
            if (this.mDeferRebuild) {
                rebuild();
                this.mDeferRebuild = false;
            }
        }
    }

    private void ensureMapBuilt() {
        if (this.mDeferRebuild) {
            rebuildIfDeferred();
            Slog.w(TAG, "The actor map was queried before the system was ready, which mayresult in decreased performance.");
        }
    }

    private void rebuild() {
        synchronized (this.mLock) {
            this.mActorPkgToPkgs.clear();
            for (String actor : this.mActorToTargetToOverlays.keySet()) {
                String actorPkg = this.mProvider.getActorPkg(actor);
                if (!TextUtils.isEmpty(actorPkg)) {
                    ArrayMap<String, ArraySet<String>> targetToOverlays = this.mActorToTargetToOverlays.get(actor);
                    Set<String> pkgs = new HashSet<>();
                    for (String target : targetToOverlays.keySet()) {
                        Set<String> overlays = targetToOverlays.get(target);
                        pkgs.add(target);
                        pkgs.addAll(overlays);
                    }
                    this.mActorPkgToPkgs.put(actorPkg, pkgs);
                }
            }
        }
    }

    private void addTargetToMap(String actor, String target, Collection<String> changedPackages) {
        ArrayMap<String, ArraySet<String>> targetToOverlays = this.mActorToTargetToOverlays.get(actor);
        if (targetToOverlays == null) {
            targetToOverlays = new ArrayMap<>();
            this.mActorToTargetToOverlays.put(actor, targetToOverlays);
        }
        ArraySet<String> overlays = targetToOverlays.get(target);
        if (overlays == null) {
            ArraySet<String> overlays2 = new ArraySet<>();
            targetToOverlays.put(target, overlays2);
        }
        changedPackages.add(this.mProvider.getActorPkg(actor));
    }

    private void addOverlayToMap(String actor, String target, String overlay, Collection<String> changedPackages) {
        synchronized (this.mLock) {
            ArrayMap<String, ArraySet<String>> targetToOverlays = this.mActorToTargetToOverlays.get(actor);
            if (targetToOverlays == null) {
                targetToOverlays = new ArrayMap<>();
                this.mActorToTargetToOverlays.put(actor, targetToOverlays);
            }
            ArraySet<String> overlays = targetToOverlays.get(target);
            if (overlays == null) {
                overlays = new ArraySet<>();
                targetToOverlays.put(target, overlays);
            }
            overlays.add(overlay);
        }
        changedPackages.add(this.mProvider.getActorPkg(actor));
    }
}
