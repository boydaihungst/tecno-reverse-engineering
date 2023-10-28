package com.android.server.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.VersionedPackage;
import android.os.Handler;
import android.util.Pair;
import com.android.server.pm.IPackageManagerServiceLice;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.List;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes2.dex */
public interface IPackageManagerServiceLice {
    public static final LiceInfo<IPackageManagerServiceLice> sLiceInfo = new LiceInfo<>("com.transsion.server.pm.PackageManagerServiceLice", IPackageManagerServiceLice.class, new Supplier() { // from class: com.android.server.pm.IPackageManagerServiceLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IPackageManagerServiceLice.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements IPackageManagerServiceLice {
    }

    static IPackageManagerServiceLice Instance() {
        return (IPackageManagerServiceLice) sLiceInfo.getImpl();
    }

    default Integer checkUidPermission(String permName, int uid, AndroidPackage pkg) {
        return null;
    }

    default void onPMSInit(PackageManagerService pms, Context context) {
    }

    default void uidAdded(int uid, Object settingBase) {
    }

    default void uidRemoved(int uid, boolean isSharedUserId) {
    }

    default void otherUidAdded(int uid, Object settingBase) {
    }

    default void otherUidRemoved(int uid, boolean isSharedUserId) {
    }

    default void onServiceIntentResolverQuery(List<ResolveInfo> infoList, Intent intent, long flags, int userId) {
    }

    default void setComponentEnabledSetting(ComponentName componentName, int newState, int userId) {
    }

    default void onShutdown() {
    }

    default boolean setApplicationHide(String packageName, int newState, int flags, int userId, String callingPackage, String lastDisableAppCaller) {
        return true;
    }

    default boolean isHiddenByXhide(String packageName, int userId, int callingUid, Computer snapshot) {
        return false;
    }

    default Pair<Boolean, String> isHiddenByXhide(String packageName, int userId, String callingPackage) {
        return new Pair<>(false, packageName);
    }

    default String setApplicationHideTemp(String packageName, boolean hidden, int userId, int callingUid, Computer snapshot) {
        return packageName;
    }

    default boolean isTmpUnHideApp() {
        return false;
    }

    default boolean isTmpUnHideApp(String packageName) {
        return false;
    }

    default boolean setApplicationHiddenAsUser(String packageName, boolean hidden, int userId) {
        return true;
    }

    default int getApplicationHiddenState(String packageName, int userId) {
        return 0;
    }

    default String[] getAllHiddenApps(String pkgName, int userId) {
        return null;
    }

    default boolean needSkipAppInfo(String pkgName, int userId) {
        return false;
    }

    default void onPackageDeleted(String pkgName, int[] users) {
    }

    default void systemReady(Context context, Handler handler) {
    }

    default boolean isOsOverlay(AndroidPackage pkg) {
        return false;
    }

    default boolean shouldShowNotificationWhenPackageInstalled(String installerPkgName, String pkgName) {
        return true;
    }

    default int deletePackageVersionedInternal(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId, int deleteFlags, boolean allowSilentUninstall) {
        return deleteFlags;
    }

    default boolean[] canSuspendPackageForUserInternal(String[] packageNames, int userId, boolean[] originalValue) {
        return originalValue;
    }

    default void setEnabledSettings(List<PackageManager.ComponentEnabledSetting> settings, int userId, String callingPackage) {
    }

    default void preparePackageLI(ParsedPackage parsedPackage) throws PackageManagerException {
    }

    default void setApplicationHiddenSettingAsUser(String packageName, boolean hidden, int userId) {
    }

    default int runUninstall(VersionedPackage versionedPackage, int userId, int deleteFlags) {
        return deleteFlags;
    }

    default boolean isSecureFrpInstallAllowed(PackageInstaller.SessionParams params, int callingUid) {
        return false;
    }

    default boolean ignoreInstallUserRestriction(String installPackageName, int callingUid, boolean installExistingPackage) {
        return false;
    }
}
