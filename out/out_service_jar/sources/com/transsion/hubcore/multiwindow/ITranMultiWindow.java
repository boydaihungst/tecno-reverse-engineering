package com.transsion.hubcore.multiwindow;

import android.content.Context;
import android.util.DisplayMetrics;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.List;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranMultiWindow {
    public static final TranClassInfo<ITranMultiWindow> classInfo = new TranClassInfo<>("com.transsion.hubcore.multiwindow.TranMultiWindowImpl", ITranMultiWindow.class, new Supplier() { // from class: com.transsion.hubcore.multiwindow.ITranMultiWindow$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranMultiWindow.lambda$static$0();
        }
    });

    static /* synthetic */ ITranMultiWindow lambda$static$0() {
        return new ITranMultiWindow() { // from class: com.transsion.hubcore.multiwindow.ITranMultiWindow.1
        };
    }

    static ITranMultiWindow Instance() {
        return (ITranMultiWindow) classInfo.getImpl();
    }

    default void postExecute(Runnable runnable, long delay) {
    }

    default void setStartInMultiWindow(String pkgName, int type, int direction, int startType) {
    }

    default String getReadyStartInMultiWindowPackageName() {
        return null;
    }

    default int getReadyStartInMultiWindowId() {
        return -1;
    }

    default void resetReadyStartInMultiWindowPackage() {
    }

    default int getReadyStartInMultiWindowPackageStartType() {
        return -1;
    }

    default void setMultiWindowConfigToSystem(String key, List<String> list) {
    }

    default boolean shouldForceRelaunch(String pkgName, DisplayMetrics dm) {
        return false;
    }

    default boolean shouldForceNonRelaunch(String pkgName) {
        return false;
    }

    default boolean inLargeScreen(DisplayMetrics dm) {
        return false;
    }

    default void updateZBoostTaskIdWhenToSplit(int taskId) {
    }

    default int getZBoostTaskIdWhenToSplit() {
        return -1;
    }

    default boolean isPayTriggerOrDLCMode(Context context, int startType) {
        return false;
    }
}
