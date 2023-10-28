package com.transsion.hubcore.server.wm;

import com.transsion.hubcore.server.wm.ITranTaskDisplayArea;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranTaskDisplayArea {
    public static final TranClassInfo<ITranTaskDisplayArea> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranTaskDisplayAreaImpl", ITranTaskDisplayArea.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranTaskDisplayArea$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranTaskDisplayArea.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranTaskDisplayArea {
    }

    static ITranTaskDisplayArea Instance() {
        return (ITranTaskDisplayArea) classInfo.getImpl();
    }

    default void hookDisplayAreaChildCountV3(int displayAreaId, int taskCount) {
    }

    default void hookDisplayAreaChildCountV4(int multiWindowMode, int multiWindowId, int taskCount) {
    }

    default void hookRequestedOrientationV3(int displayAreaId, int orientation) {
    }

    default void hookRequestedOrientationV4(int multiWindowMode, int multiWindowId, int orientation) {
    }

    default void hookChildSizeChange(boolean isSourceConnect, int size, int displayId, boolean add) {
    }
}
