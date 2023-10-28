package com.transsion.hubcore.server.display;

import com.transsion.hubcore.server.display.ITranDisplayDeviceRepository;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranDisplayDeviceRepository {
    public static final TranClassInfo<ITranDisplayDeviceRepository> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.display.TranDisplayDeviceRepositoryImpl", ITranDisplayDeviceRepository.class, new Supplier() { // from class: com.transsion.hubcore.server.display.ITranDisplayDeviceRepository$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDisplayDeviceRepository.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranDisplayDeviceRepository {
    }

    static ITranDisplayDeviceRepository Instance() {
        return (ITranDisplayDeviceRepository) classInfo.getImpl();
    }

    default void hookVirtualDisplayChanged(int uid, String packageName, int packageState) {
    }
}
