package com.transsion.hubcore.content.om;

import com.transsion.hubcore.utils.TranClassInfo;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranOverlayConfig {
    public static final TranClassInfo<ITranOverlayConfig> classInfo = new TranClassInfo<>("com.transsion.hubcore.content.om.TranOverlayConfigImpl", ITranOverlayConfig.class, new Supplier() { // from class: com.transsion.hubcore.content.om.ITranOverlayConfig$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranOverlayConfig.lambda$static$0();
        }
    });

    static /* synthetic */ ITranOverlayConfig lambda$static$0() {
        return new ITranOverlayConfig() { // from class: com.transsion.hubcore.content.om.ITranOverlayConfig.1
        };
    }

    static ITranOverlayConfig Instance() {
        return classInfo.getImpl();
    }

    default void getIdmapPath(ArrayList<String> idmapPaths, ITranOverlayConfigTroy tranOverlayConfigTroy) {
    }
}
