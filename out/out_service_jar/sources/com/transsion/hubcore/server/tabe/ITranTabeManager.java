package com.transsion.hubcore.server.tabe;

import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.parsing.PackageLite;
import android.os.Message;
import android.os.SystemProperties;
import com.transsion.hubcore.server.tabe.ITranTabeManager;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranTabeManager {
    public static final boolean IS_TABE_SUPPORT = "1".equals(SystemProperties.get("ro.product.trantabe.support", "0"));
    public static final boolean IS_TABE_INSTALL_SUPPORT = "1".equals(SystemProperties.get("ro.product.trantabe.install.support", "0"));
    public static final TranClassInfo<ITranTabeManager> sClassInfo = new TranClassInfo<>("com.transsion.hubcore.server.tabe.TranTabeManagerImpl", ITranTabeManager.class, new Supplier() { // from class: com.transsion.hubcore.server.tabe.ITranTabeManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranTabeManager.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranTabeManager {
    }

    static ITranTabeManager Instance() {
        return (ITranTabeManager) sClassInfo.getImpl();
    }

    default void init(Context context) {
    }

    default void onSystemReady(Context context) {
    }

    default boolean isPermissionCrash(ApplicationErrorReport.CrashInfo crashInfo, String packageName, boolean isForeApp) {
        return false;
    }

    default void addErrorInfoToTabe(boolean systemReady, boolean isForeApp, String processName, String eventType, ApplicationErrorReport.CrashInfo crashInfo, String processClass, int setadj, int setSchedGroup) {
    }

    default boolean isErrorDialogNotShow(boolean showFirstCrashDevOption, String packageName) {
        return false;
    }

    default void addGoUnsupportService(boolean systemReady, Intent service, String callingPackage) {
    }

    default void setGoUnsupportServiceCallerAppName(int setAdj, int foreAppAdj, int setSchdGroup, int schedGroupTopApp, int flags, String processName) {
    }

    default void handlerBugreport(Long crashTimePersistent, String processName) {
    }

    default void handlerPackageInstallerSession(Message msg, int sessionId) {
    }

    default void handlerPISessionInstalled(PackageLite packageLite, PackageInstaller.SessionParams params, int sessionId, String iPkg, String oPkg, String pkg, long versionCode, IntentSender receiver, int code, String message) {
    }
}
