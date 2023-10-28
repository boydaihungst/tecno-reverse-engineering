package com.transsion.hubcore.server.wm;

import android.app.IApplicationThread;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerService;
import com.transsion.hubcore.griffin.lib.app.TranAppInfo;
import com.transsion.hubcore.server.wm.ITranActivityRecord;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranActivityRecord {
    public static final TranClassInfo<ITranActivityRecord> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranActivityRecordImpl", ITranActivityRecord.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranActivityRecord$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranActivityRecord.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranActivityRecord {
    }

    static ITranActivityRecord Instance() {
        return (ITranActivityRecord) classInfo.getImpl();
    }

    default void hookOnFirstWindowDrawnV3(ActivityRecord record) {
    }

    default void hookOnFirstWindowDrawnV4(ActivityRecord record, int multiWindowMode, int multiWindowId) {
    }

    default boolean inMultiWindow(ActivityRecord record) {
        return false;
    }

    default void hookConfigurationChanged(ActivityRecord record, Configuration newConfig, ActivityTaskManagerService atmService) {
    }

    default void setMultiWindowConfigAndMode(ActivityRecord record, Configuration tmpConfig) {
    }

    default int getConfigChange(int configChanges, String packageName) {
        return 0;
    }

    default boolean isMultiWindowModeConfiged() {
        return false;
    }

    default boolean isInMultiWindowMode() {
        return false;
    }

    default TranAppInfo hookInitial(String packageName) {
        return null;
    }

    default void hookFinishIfPossible(ActivityInfo lastResumedActivity, ActivityInfo nextResumedActivity, boolean pausing, int nextResumedActivityType) {
    }

    default void hookRelaunchActivityLocked(ActivityRecord activityRecord) {
    }

    default boolean shouldForceRelaunch(String pkg, DisplayMetrics dm) {
        return false;
    }

    default boolean shouldForceNonRelaunch(String pkg) {
        return false;
    }

    default boolean isCurAppThreadNull(IApplicationThread appThread) {
        return false;
    }

    default void activityResumeHook(ActivityInfo activityInfo) {
    }

    default int getSourceConnectConfig() {
        return 0;
    }

    default boolean isSkipSnapshot(String packageName) {
        return false;
    }
}
