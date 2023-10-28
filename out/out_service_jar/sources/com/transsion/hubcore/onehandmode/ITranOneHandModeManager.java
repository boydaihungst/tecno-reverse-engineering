package com.transsion.hubcore.onehandmode;

import com.android.server.wm.WindowState;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranOneHandModeManager {
    public static final TranClassInfo<ITranOneHandModeManager> classInfo = new TranClassInfo<>("com.transsion.hubcore.onehandmode.TranOneHandModeManagerImpl", ITranOneHandModeManager.class, new Supplier() { // from class: com.transsion.hubcore.onehandmode.ITranOneHandModeManager$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranOneHandModeManager.lambda$static$0();
        }
    });

    static /* synthetic */ ITranOneHandModeManager lambda$static$0() {
        return new ITranOneHandModeManager() { // from class: com.transsion.hubcore.onehandmode.ITranOneHandModeManager.1
        };
    }

    static ITranOneHandModeManager Instance() {
        return (ITranOneHandModeManager) classInfo.getImpl();
    }

    default void exitOneHandMode() {
    }

    default void exitOneHandMode(String reason) {
    }

    default int getOneHandCurrentState() {
        return 0;
    }

    default boolean isOneHandModeContainerWindow(WindowState windowState) {
        return false;
    }
}
