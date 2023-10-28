package com.transsion.hubcore.memfusion;

import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranMemfusionFeature {
    public static final TranClassInfo<ITranMemfusionFeature> classInfo = new TranClassInfo<>("com.transsion.hubcore.memfusion.TranMemfusionFeatureImpl", ITranMemfusionFeature.class, new Supplier() { // from class: com.transsion.hubcore.memfusion.ITranMemfusionFeature$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranMemfusionFeature.lambda$static$0();
        }
    });

    static /* synthetic */ ITranMemfusionFeature lambda$static$0() {
        return new ITranMemfusionFeature() { // from class: com.transsion.hubcore.memfusion.ITranMemfusionFeature.1
        };
    }

    static ITranMemfusionFeature Instance() {
        return (ITranMemfusionFeature) classInfo.getImpl();
    }

    default int getParams(String paramKey) {
        return -1;
    }

    default boolean isMemFusionEnabled() {
        return false;
    }

    default Boolean isMemfusionProtected(String packageName) {
        return false;
    }
}
