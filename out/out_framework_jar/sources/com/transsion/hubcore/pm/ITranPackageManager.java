package com.transsion.hubcore.pm;

import com.transsion.hubcore.iconcrop.ITranNonAdaptiveIconExt;
import com.transsion.hubcore.pm.ITranPackageManager;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranPackageManager extends ITranNonAdaptiveIconExt {
    public static final TranClassInfo<ITranPackageManager> classInfo = new TranClassInfo<>("com.transsion.hubcore.pm.TranPackageManagerImpl", ITranPackageManager.class, new Supplier() { // from class: com.transsion.hubcore.pm.ITranPackageManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranPackageManager.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranPackageManager {
    }

    static ITranPackageManager Instance() {
        return classInfo.getImpl();
    }
}
