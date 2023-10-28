package com.transsion.hubcore.server.wm;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.ActivityTaskSupervisor;
import com.android.server.wm.WindowProcessController;
import com.transsion.hubcore.server.wm.ITranActivityStarter;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranActivityStarter {
    public static final TranClassInfo<ITranActivityStarter> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranActivityStarterImpl", ITranActivityStarter.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranActivityStarter$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranActivityStarter.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranActivityStarter {
    }

    static ITranActivityStarter Instance() {
        return (ITranActivityStarter) classInfo.getImpl();
    }

    default void onActivityStarterConstruct(Context context, int userId, ActivityTaskManagerService service, ActivityTaskSupervisor supervisor) {
    }

    default boolean canStartAppInLowStorage(ActivityInfo aInfo, int userId) {
        return true;
    }

    default void needHookStartActivity(String tag, Object mRequest, ActivityOptions activityOptions) {
    }

    default boolean isInMultiWindow(WindowProcessController callerApp, String callingPackage) {
        return false;
    }

    default boolean needHookMultiWindowToMax(String tag, String mulitWindowTopPackage, ActivityRecord sourceRecord, ActivityRecord r) {
        return false;
    }

    default boolean needHookReparentToDefaultDisplay(String tag, ActivityRecord sourceRecord, ActivityRecord r, int userId) {
        return false;
    }

    default int hookGetOrCreateMultiWindow(String pkg, int direction, int startType, int displayId, boolean needCreate) {
        return -1;
    }

    default void setMultiWindowId(int id) {
    }

    default int getMultiWindowId() {
        return 0;
    }

    default void computeOptions(ActivityOptions checkedOptions) {
    }

    default boolean inMultiWindow(ActivityRecord sourceRecord) {
        return false;
    }

    default void showSceneUnSupportMultiToast() {
    }

    default boolean isAgaresEnable() {
        return false;
    }

    default boolean isAgaresPreload(Intent intent) {
        return intent != null && intent.getPreload();
    }

    default boolean isAgaresDebug() {
        return false;
    }

    default boolean getAgaresEnable() {
        return false;
    }

    default boolean isAppLaunch(Intent intent, String caller) {
        return false;
    }

    default void appLaunchStart(Intent intent, String caller, long start) {
    }

    default void appLaunchEnd(Intent intent, int launchType, int procType, int recentIndex, long end) {
    }
}
