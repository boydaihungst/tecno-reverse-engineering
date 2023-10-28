package com.transsion.hubcore.apm;

import com.transsion.hubcore.utils.TranClassInfo;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranApmFeature {
    public static final TranClassInfo<ITranApmFeature> classInfo = new TranClassInfo<>("com.transsion.hubcore.apm.TranApmFeatureImpl", ITranApmFeature.class, new Supplier() { // from class: com.transsion.hubcore.apm.ITranApmFeature$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranApmFeature.lambda$static$0();
        }
    });

    static /* synthetic */ ITranApmFeature lambda$static$0() {
        return new ITranApmFeature() { // from class: com.transsion.hubcore.apm.ITranApmFeature.1
        };
    }

    static ITranApmFeature Instance() {
        return (ITranApmFeature) classInfo.getImpl();
    }

    default Set<String> getTopApp(int num) {
        return Collections.EMPTY_SET;
    }
}
