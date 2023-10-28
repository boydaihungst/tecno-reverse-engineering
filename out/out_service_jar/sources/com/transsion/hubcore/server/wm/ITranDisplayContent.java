package com.transsion.hubcore.server.wm;

import android.view.Display;
import android.view.DisplayInfo;
import android.view.WindowManagerPolicyConstants;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.AppTransition;
import com.android.server.wm.WindowState;
import com.transsion.hubcore.server.wm.ITranDisplayContent;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranDisplayContent {
    public static final TranClassInfo<ITranDisplayContent> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.wm.TranDisplayContentImpl", ITranDisplayContent.class, new Supplier() { // from class: com.transsion.hubcore.server.wm.ITranDisplayContent$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDisplayContent.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranDisplayContent {
    }

    static ITranDisplayContent Instance() {
        return (ITranDisplayContent) classInfo.getImpl();
    }

    default void updateCaptionViewStatusIfNeeded(WindowState w) {
    }

    default boolean isAreaRate(WindowState w, DisplayInfo displayInfo) {
        return false;
    }

    default boolean isInMultiWindow(WindowState w) {
        return false;
    }

    default boolean shouldPreferRotationScreen(boolean isDefaultDisplay, AppTransition appTransition) {
        return false;
    }

    default boolean isThunderBackTransition(AppTransition appTransition) {
        return false;
    }

    default void hookFixedRotationLaunchOrDefaultDisplayFixRotation(ActivityRecord r, boolean isDefaultDisplay, boolean isThunderBackTransition, int rotation, int currentRotation, AppTransition appTransition, boolean needHookDefaultDisplayFixRotation) {
    }

    default boolean isFocusOnThunderbackWindow(String tag, WindowState newFocus, boolean mIsFocusOnThunderbackWindow) {
        return false;
    }

    default boolean isMultiWindow(boolean isMultiWindow) {
        return false;
    }

    default boolean shouldImeAttachedToApp(WindowState mImeLayeringTarget, boolean mDettachImeWithActivity) {
        return false;
    }

    default WindowManagerPolicyConstants.PointerEventListener getPointerEventListenerMagellan(Display display) {
        return null;
    }

    default boolean isAgaresDisplay(int displayId) {
        return false;
    }

    default int updatePendingLayoutChanges(int pendingLayoutChanges, WindowState w, String tag) {
        return -1;
    }

    default void printDisplayContentAndPreferredModeId(WindowState w, String tag, int preferredModeId) {
    }

    default boolean needRedoLayout(WindowState windowState) {
        return false;
    }
}
