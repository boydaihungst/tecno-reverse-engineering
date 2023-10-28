package com.transsion.hubcore.server.display;

import android.os.Bundle;
import com.android.server.wm.WindowManagerInternal;
import com.transsion.hubcore.server.display.ITranDisplayManagerService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranDisplayManagerService {
    public static final TranClassInfo<ITranDisplayManagerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.display.TranDisplayManagerServiceImpl", ITranDisplayManagerService.class, new Supplier() { // from class: com.transsion.hubcore.server.display.ITranDisplayManagerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDisplayManagerService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranDisplayManagerService {
    }

    static ITranDisplayManagerService instance() {
        return (ITranDisplayManagerService) classInfo.getImpl();
    }

    default void updateRefreshRateForScene(WindowManagerInternal windowManagerInternal, Bundle b) {
    }

    default void updateRefreshRateForVideoScene(WindowManagerInternal windowManagerInternal, int videoState, int videoFps, int videoSessionId) {
    }
}
