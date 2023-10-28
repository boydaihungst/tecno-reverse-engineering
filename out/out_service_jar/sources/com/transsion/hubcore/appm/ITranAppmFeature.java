package com.transsion.hubcore.appm;

import android.content.Context;
import com.android.server.UiModeManagerService;
import com.transsion.hubcore.appm.ITranAppmFeature;
import com.transsion.hubcore.utils.TranClassInfo;
import java.io.PrintWriter;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAppmFeature {
    public static final TranClassInfo<ITranAppmFeature> classInfo = new TranClassInfo<>("com.transsion.hubcore.appm.TranAppmFeatureImpl", ITranAppmFeature.class, new Supplier() { // from class: com.transsion.hubcore.appm.ITranAppmFeature$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAppmFeature.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAppmFeature {
    }

    static ITranAppmFeature Instance() {
        return (ITranAppmFeature) classInfo.getImpl();
    }

    default void systemReady(Context context) {
    }

    default void doCommand(PrintWriter pw, String[] args) {
    }

    default boolean isContainsPackage(String key, String pkgName) {
        return false;
    }

    default void sendEvent(String key, String value) {
    }

    default String getPolicyVersion(String key) {
        return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
    }

    default boolean getETControl(String featureType) {
        return false;
    }
}
