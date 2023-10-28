package com.transsion.hubcore.server.display;

import android.view.DisplayInfo;
import com.transsion.hubcore.server.display.ITranLogicalDisplay;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranLogicalDisplay {
    public static final TranClassInfo<ITranLogicalDisplay> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.display.TranLogicalDisplayImpl", ITranLogicalDisplay.class, new Supplier() { // from class: com.transsion.hubcore.server.display.ITranLogicalDisplay$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranLogicalDisplay.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranLogicalDisplay {
    }

    static ITranLogicalDisplay Instance() {
        return (ITranLogicalDisplay) classInfo.getImpl();
    }

    default void updateDisplayInfoFlag(int devicesFlag, DisplayInfo displayInfo) {
    }
}
