package com.transsion.hubcore.server.wm;

import android.util.ArraySet;
import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationDefinition;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.AppTransition;
import com.android.server.wm.WindowState;
import com.transsion.hubcore.server.wm.ITranAppTransitionController;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAppTransitionController {
    public static final TranClassInfo<ITranAppTransitionController> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranAppTransitionControllerImpl", ITranAppTransitionController.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranAppTransitionController$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAppTransitionController.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAppTransitionController {
    }

    static ITranAppTransitionController Instance() {
        return (ITranAppTransitionController) classInfo.getImpl();
    }

    default void registerRemoteAnimations(RemoteAnimationDefinition definition, RemoteAnimationDefinition remoteAnimationDefinitionForMultiWin, RemoteAnimationDefinition remoteAnimationDefinition) {
    }

    default boolean isThunderbackWindow(ActivityTaskManagerService mAtmService, ActivityRecord topOpeningApp, boolean mAppTransitionReady) {
        return false;
    }

    default int getTransitOldThunderStatus(AppTransition appTransition) {
        return -1;
    }

    default boolean isContained(AppTransition appTransition, int value) {
        return false;
    }

    default boolean isIntegerEqual(int param1, int param2) {
        return false;
    }

    default boolean isTransitOldNone(String tag, ActivityRecord topOpeningApp, WindowState wallpaperTarget, ArraySet<ActivityRecord> openingApps, ActivityRecord topClosingApp) {
        return false;
    }

    default RemoteAnimationAdapter getRemoteAnimationAdapter(RemoteAnimationDefinition remoteAnimationDefinitionForMultiWin, int transit, ArraySet<Integer> activityTypes, RemoteAnimationAdapter thunderbackRemoteAnimationAdapter) {
        return null;
    }
}
