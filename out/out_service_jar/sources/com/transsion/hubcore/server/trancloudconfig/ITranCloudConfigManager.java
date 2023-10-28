package com.transsion.hubcore.server.trancloudconfig;

import android.content.Context;
import com.transsion.hubcore.server.trancloudconfig.ITranCloudConfigManager;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranCloudConfigManager {
    public static final String IMPL_CLASS_NAME = "com.transsion.hubcore.server.trancloudconfig.TranCloudConfigManagerImpl";
    public static final TranClassInfo<ITranCloudConfigManager> classInfo = new TranClassInfo<>(IMPL_CLASS_NAME, ITranCloudConfigManager.class, new Supplier() { // from class: com.transsion.hubcore.server.trancloudconfig.ITranCloudConfigManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranCloudConfigManager.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranCloudConfigManager {
    }

    static ITranCloudConfigManager Instance() {
        return (ITranCloudConfigManager) classInfo.getImpl();
    }

    default void init(Context context) {
    }

    default void onSystemReady() {
    }

    default void destroy() {
    }
}
