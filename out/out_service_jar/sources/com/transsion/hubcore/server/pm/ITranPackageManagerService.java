package com.transsion.hubcore.server.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.VersionedPackage;
import android.content.pm.parsing.PackageLite;
import android.os.Handler;
import android.os.Message;
import com.android.server.pm.Computer;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerException;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.PackageManagerServiceInjector;
import com.android.server.pm.Settings;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.utils.WatchedArrayMap;
import com.mediatek.server.pm.PmsExt;
import com.transsion.hubcore.server.pm.ITranPackageManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranPackageManagerService extends ITranIconExt, ITranPkmsExt {
    public static final TranClassInfo<ITranPackageManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.pm.TranPackageManagerServiceImpl", ITranPackageManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.pm.ITranPackageManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranPackageManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranPackageManagerService {
    }

    static ITranPackageManagerService Instance() {
        return (ITranPackageManagerService) classInfo.getImpl();
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

    default void handlerPISessionInstalled(PackageLite packageLite, PackageInstaller.SessionParams params, int sessionId, String iPkg, String oPkg, String pkg, long versionCode, IntentSender receiver, int code, String message) {
    }

    default void handlerPackageInstallerSession(Message msg, int sessionId) {
    }

    default void addToRemovableSystemAppSet(PmsExt pmsExt, boolean isDeviceUpgrading) {
    }

    default void removeAppPrivateVolume(PackageManagerServiceInjector injector, Installer mInstaller) {
    }

    default void deletePackageAsOOBE(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId, int flags, WatchedArrayMap<String, AndroidPackage> packages) {
    }

    default void setApplicationNotifyScreenOn(String packageName, int newState, int userId, Settings settings) {
    }

    default int getApplicationNotifyScreenOn(String packageName, int userId, Settings settings) {
        return 0;
    }

    default void checkDefaultGaller() {
    }

    default boolean setDefaultGallerPackageName(Context mContext, String gallerPkg, int userId) {
        return false;
    }

    default String getDefaultGallerPackageName(Context mContext, int userId) {
        return "";
    }

    default void checkDefaultMusic() {
    }

    default boolean setDefaultMusicPackageName(Context mContext, String musicPkg, int userId) {
        return false;
    }

    default String getDefaultMusicPackageName(Context mContext, int userId) {
        return "";
    }

    default void setGriffinPackages(AndroidPackage pkg) {
    }

    default void hookPMSScanEnd() {
    }

    default String setApplicationHideTemp(String packageName, boolean hidden, int userId, int callingUid, Computer snapshot) {
        return packageName;
    }

    default boolean isTmpUnHideApp() {
        return false;
    }

    default boolean setApplicationHiddenAsUser(String packageName, boolean hidden, int userId) {
        return true;
    }

    default String getAppProfileName(String packageName) {
        return "";
    }

    default boolean checkPckName(Context mContext, String packageName) {
        return false;
    }
}
