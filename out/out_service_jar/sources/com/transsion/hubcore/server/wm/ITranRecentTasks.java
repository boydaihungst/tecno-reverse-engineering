package com.transsion.hubcore.server.wm;

import android.content.Context;
import com.transsion.hubcore.server.wm.ITranRecentTasks;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranRecentTasks {
    public static final TranClassInfo<ITranRecentTasks> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranRecentTasksImpl", ITranRecentTasks.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranRecentTasks$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranRecentTasks.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranRecentTasks {
    }

    static ITranRecentTasks Instance() {
        return (ITranRecentTasks) classInfo.getImpl();
    }

    default int getMaxNumVisibleTasks(Context context, int maxNumVisibleTasks) {
        return maxNumVisibleTasks;
    }

    default boolean isInFreeformWinMode(boolean inFreeformWindowMode) {
        return false;
    }

    default boolean isPresentInFreeForm(int windowingMode, int otherWindowingMode) {
        return false;
    }

    default boolean isAgaresEnable() {
        return false;
    }

    default void setMaxVisableRecent(int type, int maxVisableRecents) {
    }
}
