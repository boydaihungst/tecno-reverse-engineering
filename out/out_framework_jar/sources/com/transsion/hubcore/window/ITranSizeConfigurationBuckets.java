package com.transsion.hubcore.window;

import android.content.res.Configuration;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranSizeConfigurationBuckets {
    public static final TranClassInfo<ITranSizeConfigurationBuckets> classInfo = new TranClassInfo<>("com.transsion.hubcore.window.TranSizeConfigurationBucketsImpl", ITranSizeConfigurationBuckets.class, new Supplier() { // from class: com.transsion.hubcore.window.ITranSizeConfigurationBuckets$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranSizeConfigurationBuckets.lambda$static$0();
        }
    });

    static /* synthetic */ ITranSizeConfigurationBuckets lambda$static$0() {
        return new ITranSizeConfigurationBuckets() { // from class: com.transsion.hubcore.window.ITranSizeConfigurationBuckets.1
        };
    }

    static ITranSizeConfigurationBuckets Instance() {
        return classInfo.getImpl();
    }

    default boolean filterDiff(boolean crosses, Configuration oldConfig, Configuration newConfig) {
        return false;
    }
}
