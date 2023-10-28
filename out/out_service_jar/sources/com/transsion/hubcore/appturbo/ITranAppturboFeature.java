package com.transsion.hubcore.appturbo;

import com.transsion.hubcore.utils.TranClassInfo;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAppturboFeature {
    public static final int BOOT_TURBO = 2;
    public static final int SWITCH_TURBO = 3;
    public static final int TP_TURBO = 1;
    public static final TranClassInfo<ITranAppturboFeature> classInfo = new TranClassInfo<>("com.transsion.hubcore.appturbo.TranAppturboFeatureImpl", ITranAppturboFeature.class, new Supplier() { // from class: com.transsion.hubcore.appturbo.ITranAppturboFeature$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranAppturboFeature.lambda$static$0();
        }
    });

    static /* synthetic */ ITranAppturboFeature lambda$static$0() {
        return new ITranAppturboFeature() { // from class: com.transsion.hubcore.appturbo.ITranAppturboFeature.1
        };
    }

    static ITranAppturboFeature Instance() {
        return (ITranAppturboFeature) classInfo.getImpl();
    }

    default void onCloudChanged(int type, int version, List<String> configs) {
    }

    default int getTpTurbo() {
        return 1;
    }

    default int getBootTurbo() {
        return 2;
    }
}
