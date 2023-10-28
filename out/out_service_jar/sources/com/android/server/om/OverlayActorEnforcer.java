package com.android.server.om;

import android.content.om.OverlayInfo;
import android.content.om.OverlayableInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.pkg.parsing.ParsingPackageUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
/* loaded from: classes2.dex */
public class OverlayActorEnforcer {
    private final PackageManagerHelper mPackageManager;

    /* loaded from: classes2.dex */
    public enum ActorState {
        TARGET_NOT_FOUND,
        NO_PACKAGES_FOR_UID,
        MISSING_TARGET_OVERLAYABLE_NAME,
        MISSING_LEGACY_PERMISSION,
        ERROR_READING_OVERLAYABLE,
        UNABLE_TO_GET_TARGET_OVERLAYABLE,
        MISSING_OVERLAYABLE,
        INVALID_OVERLAYABLE_ACTOR_NAME,
        NO_NAMED_ACTORS,
        MISSING_NAMESPACE,
        MISSING_ACTOR_NAME,
        ACTOR_NOT_FOUND,
        ACTOR_NOT_PREINSTALLED,
        INVALID_ACTOR,
        ALLOWED
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Pair<String, ActorState> getPackageNameForActor(String actorUriString, Map<String, Map<String, String>> namedActors) {
        Uri actorUri = Uri.parse(actorUriString);
        String actorScheme = actorUri.getScheme();
        List<String> actorPathSegments = actorUri.getPathSegments();
        if (!ParsingPackageUtils.TAG_OVERLAY.equals(actorScheme) || CollectionUtils.size(actorPathSegments) != 1) {
            return Pair.create(null, ActorState.INVALID_OVERLAYABLE_ACTOR_NAME);
        }
        if (namedActors.isEmpty()) {
            return Pair.create(null, ActorState.NO_NAMED_ACTORS);
        }
        String actorNamespace = actorUri.getAuthority();
        Map<String, String> namespace = namedActors.get(actorNamespace);
        if (ArrayUtils.isEmpty(namespace)) {
            return Pair.create(null, ActorState.MISSING_NAMESPACE);
        }
        String actorName = actorPathSegments.get(0);
        String packageName = namespace.get(actorName);
        if (TextUtils.isEmpty(packageName)) {
            return Pair.create(null, ActorState.MISSING_ACTOR_NAME);
        }
        return Pair.create(packageName, ActorState.ALLOWED);
    }

    public OverlayActorEnforcer(PackageManagerHelper packageManager) {
        this.mPackageManager = packageManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enforceActor(OverlayInfo overlayInfo, String methodName, int callingUid, int userId) throws SecurityException {
        ActorState actorState = isAllowedActor(methodName, overlayInfo, callingUid, userId);
        if (actorState == ActorState.ALLOWED) {
            return;
        }
        String targetOverlayableName = overlayInfo.targetOverlayableName;
        String errorMessage = "UID" + callingUid + " is not allowed to call " + methodName + " for " + (TextUtils.isEmpty(targetOverlayableName) ? "" : targetOverlayableName + " in ") + overlayInfo.targetPackageName + " for user " + userId;
        Slog.w("OverlayManager", errorMessage + " because " + actorState);
        throw new SecurityException(errorMessage);
    }

    public ActorState isAllowedActor(String methodName, OverlayInfo overlayInfo, int callingUid, int userId) {
        switch (callingUid) {
            case 0:
            case 1000:
                return ActorState.ALLOWED;
            default:
                String targetPackageName = overlayInfo.targetPackageName;
                AndroidPackage targetPkgInfo = this.mPackageManager.getPackageForUser(targetPackageName, userId);
                if (targetPkgInfo == null) {
                    return ActorState.TARGET_NOT_FOUND;
                }
                if (targetPkgInfo.isDebuggable()) {
                    return ActorState.ALLOWED;
                }
                String[] callingPackageNames = this.mPackageManager.getPackagesForUid(callingUid);
                if (ArrayUtils.isEmpty(callingPackageNames)) {
                    return ActorState.NO_PACKAGES_FOR_UID;
                }
                if (ArrayUtils.contains(callingPackageNames, targetPackageName)) {
                    return ActorState.ALLOWED;
                }
                String targetOverlayableName = overlayInfo.targetOverlayableName;
                if (TextUtils.isEmpty(targetOverlayableName)) {
                    try {
                        if (this.mPackageManager.doesTargetDefineOverlayable(targetPackageName, userId)) {
                            return ActorState.MISSING_TARGET_OVERLAYABLE_NAME;
                        }
                        try {
                            this.mPackageManager.enforcePermission("android.permission.CHANGE_OVERLAY_PACKAGES", methodName);
                            return ActorState.ALLOWED;
                        } catch (SecurityException e) {
                            return ActorState.MISSING_LEGACY_PERMISSION;
                        }
                    } catch (IOException e2) {
                        return ActorState.ERROR_READING_OVERLAYABLE;
                    }
                }
                try {
                    OverlayableInfo targetOverlayable = this.mPackageManager.getOverlayableForTarget(targetPackageName, targetOverlayableName, userId);
                    if (targetOverlayable == null) {
                        return ActorState.MISSING_OVERLAYABLE;
                    }
                    String actor = targetOverlayable.actor;
                    if (TextUtils.isEmpty(actor)) {
                        try {
                            this.mPackageManager.enforcePermission("android.permission.CHANGE_OVERLAY_PACKAGES", methodName);
                            return ActorState.ALLOWED;
                        } catch (SecurityException e3) {
                            return ActorState.MISSING_LEGACY_PERMISSION;
                        }
                    }
                    Map<String, Map<String, String>> namedActors = this.mPackageManager.getNamedActors();
                    Pair<String, ActorState> actorUriPair = getPackageNameForActor(actor, namedActors);
                    ActorState actorUriState = (ActorState) actorUriPair.second;
                    if (actorUriState != ActorState.ALLOWED) {
                        return actorUriState;
                    }
                    String actorPackageName = (String) actorUriPair.first;
                    AndroidPackage actorPackage = this.mPackageManager.getPackageForUser(actorPackageName, userId);
                    if (actorPackage == null) {
                        return ActorState.ACTOR_NOT_FOUND;
                    }
                    if (!actorPackage.isSystem()) {
                        return ActorState.ACTOR_NOT_PREINSTALLED;
                    }
                    if (ArrayUtils.contains(callingPackageNames, actorPackageName)) {
                        return ActorState.ALLOWED;
                    }
                    return ActorState.INVALID_ACTOR;
                } catch (IOException e4) {
                    return ActorState.UNABLE_TO_GET_TARGET_OVERLAYABLE;
                }
        }
    }
}
