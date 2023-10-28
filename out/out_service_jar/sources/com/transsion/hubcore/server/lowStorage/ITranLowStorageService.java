package com.transsion.hubcore.server.lowStorage;

import android.content.Context;
import android.os.SystemProperties;
import com.transsion.hubcore.server.lowStorage.ITranLowStorageService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranLowStorageService {
    public static final boolean LOW_STORAGE_SUPPORT = SystemProperties.get("ro.tran_low_storage_support", "0").equals("1");
    public static final TranClassInfo<ITranLowStorageService> sClassInfo = new TranClassInfo<>("com.transsion.hubcore.server.lowStorage.TranLowStorageServiceImpl", ITranLowStorageService.class, new Supplier() { // from class: com.transsion.hubcore.server.lowStorage.ITranLowStorageService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranLowStorageService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranLowStorageService {
    }

    static ITranLowStorageService Instance() {
        return (ITranLowStorageService) sClassInfo.getImpl();
    }

    default void init(Context context) {
    }

    default void reportPackageCurrentState(String packageName, String exceptionType) {
    }

    default boolean getPackageCurrentState(String packageName) {
        return false;
    }

    default boolean reprotPkgCurStateForIoExp(String packageName, String exceptionType) {
        return false;
    }

    default void reprotNoSpaceError(String packageName) {
    }

    default void reprotSpaceChange(int storageState) {
    }
}
