package com.transsion.hubcore.content.res;

import android.content.res.ApkAssets;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranAssetManager {
    public static final TranClassInfo<ITranAssetManager> classInfo = new TranClassInfo<>("com.transsion.hubcore.content.res.TranAssetManagerImpl", ITranAssetManager.class, new Supplier() { // from class: com.transsion.hubcore.content.res.ITranAssetManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranAssetManager.lambda$static$0();
        }
    });

    static /* synthetic */ ITranAssetManager lambda$static$0() {
        return new ITranAssetManager() { // from class: com.transsion.hubcore.content.res.ITranAssetManager.1
        };
    }

    static ITranAssetManager Instance() {
        return classInfo.getImpl();
    }

    default void onAddSystemAsstetsInZygote(List<ApkAssets> apkAssets) {
    }
}
