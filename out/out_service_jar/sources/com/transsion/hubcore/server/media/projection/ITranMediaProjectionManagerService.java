package com.transsion.hubcore.server.media.projection;

import com.transsion.hubcore.server.media.projection.ITranMediaProjectionManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranMediaProjectionManagerService {
    public static final TranClassInfo<ITranMediaProjectionManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.media.projection.TranMediaProjectionManagerServiceImpl", ITranMediaProjectionManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.media.projection.ITranMediaProjectionManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranMediaProjectionManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranMediaProjectionManagerService {
    }

    static ITranMediaProjectionManagerService Instance() {
        return (ITranMediaProjectionManagerService) classInfo.getImpl();
    }

    default void hookScreenRecordingChanged(int uid, String packageName, int packageState) {
    }
}
