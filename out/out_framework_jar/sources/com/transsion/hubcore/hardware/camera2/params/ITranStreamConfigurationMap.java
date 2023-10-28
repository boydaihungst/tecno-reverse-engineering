package com.transsion.hubcore.hardware.camera2.params;

import android.util.Size;
import com.transsion.hubcore.hardware.camera2.params.ITranStreamConfigurationMap;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranStreamConfigurationMap {
    public static final TranClassInfo<ITranStreamConfigurationMap> classInfo = new TranClassInfo<>("com.transsion.hubcore.hardware.camera2.params.TranStreamConfigurationMapImpl", ITranStreamConfigurationMap.class, new Supplier() { // from class: com.transsion.hubcore.hardware.camera2.params.ITranStreamConfigurationMap$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranStreamConfigurationMap.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranStreamConfigurationMap {
    }

    static ITranStreamConfigurationMap Instance() {
        return classInfo.getImpl();
    }

    default boolean filterFormatSize(Size size, boolean definedFormat, boolean blobFormat) {
        return true;
    }

    default boolean isFilterListApp() {
        return false;
    }
}
