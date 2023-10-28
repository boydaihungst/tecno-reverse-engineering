package com.android.server.om;

import android.content.om.OverlayInfo;
import android.content.om.OverlayableInfo;
import android.os.FabricatedOverlayInfo;
import android.os.FabricatedOverlayInternal;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.io.IOException;
import java.util.List;
/* loaded from: classes2.dex */
final class IdmapManager {
    private static final boolean VENDOR_IS_Q_OR_LATER;
    private final String mConfigSignaturePackage;
    private final IdmapDaemon mIdmapDaemon;
    private final PackageManagerHelper mPackageManager;

    static {
        boolean isQOrLater;
        String value = SystemProperties.get("ro.vndk.version", "29");
        try {
            isQOrLater = Integer.parseInt(value) >= 29;
        } catch (NumberFormatException e) {
            isQOrLater = true;
        }
        VENDOR_IS_Q_OR_LATER = isQOrLater;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IdmapManager(IdmapDaemon idmapDaemon, PackageManagerHelper packageManager) {
        this.mPackageManager = packageManager;
        this.mIdmapDaemon = idmapDaemon;
        this.mConfigSignaturePackage = packageManager.getConfigSignaturePackage();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean createIdmap(AndroidPackage targetPackage, AndroidPackage overlayPackage, String overlayBasePath, String overlayName, int userId) {
        String targetPath;
        String targetPath2 = targetPackage.getBaseApkPath();
        try {
            int policies = calculateFulfilledPolicies(targetPackage, overlayPackage, userId);
            boolean enforce = enforceOverlayable(overlayPackage);
            if (this.mIdmapDaemon.verifyIdmap(targetPath2, overlayBasePath, overlayName, policies, enforce, userId)) {
                return false;
            }
            targetPath = targetPath2;
            try {
                return this.mIdmapDaemon.createIdmap(targetPath2, overlayBasePath, overlayName, policies, enforce, userId) != null;
            } catch (Exception e) {
                e = e;
                Slog.w("OverlayManager", "failed to generate idmap for " + targetPath + " and " + overlayBasePath, e);
                return false;
            }
        } catch (Exception e2) {
            e = e2;
            targetPath = targetPath2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeIdmap(OverlayInfo oi, int userId) {
        try {
            return this.mIdmapDaemon.removeIdmap(oi.baseCodePath, userId);
        } catch (Exception e) {
            Slog.w("OverlayManager", "failed to remove idmap for " + oi.baseCodePath, e);
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean idmapExists(OverlayInfo oi) {
        return this.mIdmapDaemon.idmapExists(oi.baseCodePath, oi.userId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<FabricatedOverlayInfo> getFabricatedOverlayInfos() {
        return this.mIdmapDaemon.getFabricatedOverlayInfos();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FabricatedOverlayInfo createFabricatedOverlay(FabricatedOverlayInternal overlay) {
        return this.mIdmapDaemon.createFabricatedOverlay(overlay);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean deleteFabricatedOverlay(String path) {
        return this.mIdmapDaemon.deleteFabricatedOverlay(path);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String dumpIdmap(String overlayPath) {
        return this.mIdmapDaemon.dumpIdmap(overlayPath);
    }

    private boolean enforceOverlayable(AndroidPackage overlayPackage) {
        if (overlayPackage.getTargetSdkVersion() >= 29) {
            return true;
        }
        if (overlayPackage.isVendor()) {
            return VENDOR_IS_Q_OR_LATER;
        }
        return (overlayPackage.isSystem() || overlayPackage.isSignedWithPlatformKey()) ? false : true;
    }

    private int calculateFulfilledPolicies(AndroidPackage targetPackage, AndroidPackage overlayPackage, int userId) {
        int fulfilledPolicies = 1;
        if (this.mPackageManager.signaturesMatching(targetPackage.getPackageName(), overlayPackage.getPackageName(), userId)) {
            fulfilledPolicies = 1 | 16;
        }
        if (matchesActorSignature(targetPackage, overlayPackage, userId)) {
            fulfilledPolicies |= 128;
        }
        if (!TextUtils.isEmpty(this.mConfigSignaturePackage) && this.mPackageManager.signaturesMatching(this.mConfigSignaturePackage, overlayPackage.getPackageName(), userId)) {
            fulfilledPolicies |= 256;
        }
        if (overlayPackage.isVendor()) {
            return fulfilledPolicies | 4;
        }
        if (overlayPackage.isProduct()) {
            return fulfilledPolicies | 8;
        }
        if (overlayPackage.isOdm()) {
            return fulfilledPolicies | 32;
        }
        if (overlayPackage.isOem()) {
            return fulfilledPolicies | 64;
        }
        if (overlayPackage.isSystem() || overlayPackage.isSystemExt()) {
            return fulfilledPolicies | 2;
        }
        return fulfilledPolicies;
    }

    private boolean matchesActorSignature(AndroidPackage targetPackage, AndroidPackage overlayPackage, int userId) {
        String targetOverlayableName = overlayPackage.getOverlayTargetOverlayableName();
        if (targetOverlayableName != null) {
            try {
                OverlayableInfo overlayableInfo = this.mPackageManager.getOverlayableForTarget(targetPackage.getPackageName(), targetOverlayableName, userId);
                if (overlayableInfo != null && overlayableInfo.actor != null) {
                    String actorPackageName = (String) OverlayActorEnforcer.getPackageNameForActor(overlayableInfo.actor, this.mPackageManager.getNamedActors()).first;
                    if (this.mPackageManager.signaturesMatching(actorPackageName, overlayPackage.getPackageName(), userId)) {
                        return true;
                    }
                    return false;
                }
                return false;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }
}
