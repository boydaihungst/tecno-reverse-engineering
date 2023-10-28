package com.transsion.hubcore.server.power.powerhub;

import android.content.Context;
import android.os.Bundle;
import com.transsion.hubcore.server.power.powerhub.ITranPowerhubService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranPowerhubService {
    public static final String IMPL_CLASS_NAME = "com.transsion.hubcore.server.power.powerhub.TranPowerhubServiceImpl";
    public static final TranClassInfo<ITranPowerhubService> classInfo = new TranClassInfo<>(IMPL_CLASS_NAME, ITranPowerhubService.class, new Supplier() { // from class: com.transsion.hubcore.server.power.powerhub.ITranPowerhubService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranPowerhubService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranPowerhubService {
    }

    static ITranPowerhubService Instance() {
        return (ITranPowerhubService) classInfo.getImpl();
    }

    default void init(Context context) {
    }

    default void systemReady() {
    }

    default Bundle getSkipInfo(Bundle viewInfo) {
        return null;
    }

    default void updateCollectData(Bundle dataInfo) {
    }
}
