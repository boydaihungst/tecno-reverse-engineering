package com.transsion.hubcore.griffin;

import com.transsion.hubcore.griffin.lib.app.TranAppInfo;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.griffin.lib.monitor.TranHooker;
import com.transsion.hubcore.griffin.lib.pm.TranProcessManager;
import com.transsion.hubcore.griffin.lib.provider.TranConfigProvider;
import com.transsion.hubcore.griffin.lib.provider.TranStateProvider;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranGriffinFeature {
    public static final int APP_TYPE_GMS = 2;
    public static final int APP_TYPE_SYS = 1;
    public static final int BIND_SERVICE = 2;
    public static final int RESTART_SERVICE = 3;
    public static final int START_SERVICE = 1;
    public static final int UID_ACTIVE = 2;
    public static final int UID_IDLE = 3;
    public static final int UID_RUNNING = 1;
    public static final int UID_STOPPED = 4;
    public static final TranClassInfo<ITranGriffinFeature> classInfo = new TranClassInfo<>("com.transsion.hubcore.griffin.TranGriffinFeatureImpl", ITranGriffinFeature.class, new Supplier() { // from class: com.transsion.hubcore.griffin.ITranGriffinFeature$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranGriffinFeature.lambda$static$0();
        }
    });

    static /* synthetic */ ITranGriffinFeature lambda$static$0() {
        return new ITranGriffinFeature() { // from class: com.transsion.hubcore.griffin.ITranGriffinFeature.1
        };
    }

    static ITranGriffinFeature Instance() {
        return (ITranGriffinFeature) classInfo.getImpl();
    }

    default int getStartService() {
        return 1;
    }

    default int getBindService() {
        return 2;
    }

    default int getAppTypeSys() {
        return 1;
    }

    default int getAppTypeGms() {
        return 2;
    }

    default boolean isGriffinSupport() {
        return false;
    }

    default void hookScreenSplitPackageChanged(int displayId, String packageName) {
    }

    default TranAppInfo getAppInfo(String packageName) {
        return null;
    }

    default TranProcessWrapper getProcessWrapper(String processName, int uid) {
        return null;
    }

    default TranConfigProvider getConfigProvider() {
        return new TranConfigProvider();
    }

    default TranHooker getHooker() {
        return new TranHooker();
    }

    default TranStateProvider getStateProvider() {
        return new TranStateProvider();
    }

    default TranProcessManager getPM() {
        return new TranProcessManager();
    }

    default boolean isKeepAliveEnable() {
        return true;
    }

    default boolean getFlagGriffinKeepAliveSupport() {
        return false;
    }

    default boolean isGriffinDebugOpen() {
        return false;
    }

    default int registrationExemptions(HashMap<String, Long> map) {
        return -1;
    }

    default int unRegistrationExemption(List<String> pkgList) {
        return -1;
    }

    default boolean ensureExemptionProc(String procName, long pss) {
        return false;
    }

    default boolean setScreenOffBlacklist(String packageName, boolean add) {
        return false;
    }
}
