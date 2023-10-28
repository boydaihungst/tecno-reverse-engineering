package com.transsion.hubcore.agares;

import com.android.server.UiModeManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.PrintWriter;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAgaresFeature {
    public static final TranClassInfo<ITranAgaresFeature> classInfo = new TranClassInfo<>("com.transsion.hubcore.agares.TranAgaresFeatureImpl", ITranAgaresFeature.class, new Supplier() { // from class: com.transsion.hubcore.agares.ITranAgaresFeature$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranAgaresFeature.lambda$static$0();
        }
    });

    static /* synthetic */ ITranAgaresFeature lambda$static$0() {
        return new ITranAgaresFeature() { // from class: com.transsion.hubcore.agares.ITranAgaresFeature.1
        };
    }

    static ITranAgaresFeature Instance() {
        return (ITranAgaresFeature) classInfo.getImpl();
    }

    default boolean isEnable(String caller) {
        return false;
    }

    default boolean isEnable() {
        return false;
    }

    default boolean isBootEnable(String caller) {
        return false;
    }

    default boolean isUserEnable(String caller) {
        return false;
    }

    default boolean isDebug() {
        return false;
    }

    default void onAppLaunch(String pkg) {
    }

    default void onActivityStart(String pkg, String activity, int launchType, boolean isAgares) {
    }

    default boolean doCommand(PrintWriter pw, String cmd) {
        return false;
    }

    default long getAvailMemThreshold() {
        return 800L;
    }

    default boolean isCustomerEnable() {
        return false;
    }

    default boolean isAgaresProtectApp(String packageName) {
        return false;
    }

    default int getMaxCachedProc() {
        return -1;
    }

    default int getMaxRecent() {
        return -1;
    }

    default String getVersion() {
        return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
    }

    default boolean filterAppRestrictedInBg(String pkg) {
        return false;
    }
}
