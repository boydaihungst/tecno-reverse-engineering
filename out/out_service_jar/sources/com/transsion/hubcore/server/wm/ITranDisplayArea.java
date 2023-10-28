package com.transsion.hubcore.server.wm;

import android.window.DisplayAreaAppearedInfo;
import com.android.server.wm.DisplayArea;
import com.android.server.wm.WindowManagerService;
import com.transsion.hubcore.server.wm.ITranDisplayArea;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranDisplayArea {
    public static final TranClassInfo<ITranDisplayArea> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranDisplayAreaImpl", ITranDisplayArea.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranDisplayArea$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDisplayArea.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranDisplayArea {
    }

    static ITranDisplayArea Instance() {
        return (ITranDisplayArea) classInfo.getImpl();
    }

    default void onConstruct(WindowManagerService wmService) {
    }

    default boolean isNonInterativeThunderback(DisplayArea child, boolean childTaskDisplayAreaType) {
        return false;
    }

    default void setMultiWindow(DisplayArea displayArea, boolean isMultiWindow, DisplayAreaAppearedInfo tdaInfo, int multiWindowState) {
    }
}
