package com.transsion.hubcore.server.content;

import android.content.ComponentName;
import com.transsion.hubcore.server.content.ITranSyncManager;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranSyncManager {
    public static final String IMPL_CLASS_NAME = "com.transsion.hubcore.server.content.TranSyncManagerImpl";
    public static final TranClassInfo<ITranSyncManager> classInfo = new TranClassInfo<>(IMPL_CLASS_NAME, ITranSyncManager.class, new Supplier() { // from class: com.transsion.hubcore.server.content.ITranSyncManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranSyncManager.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranSyncManager {
    }

    static ITranSyncManager Instance() {
        return (ITranSyncManager) classInfo.getImpl();
    }

    default boolean hookComputeSyncable(ComponentName componentName, int owningUid) {
        return false;
    }
}
