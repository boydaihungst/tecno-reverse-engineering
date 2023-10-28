package com.android.server.wm;

import android.view.InsetsState;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public interface InsetsControlTarget {
    default void notifyInsetsControlChanged() {
    }

    default WindowState getWindow() {
        return null;
    }

    default boolean getRequestedVisibility(int type) {
        return InsetsState.getDefaultVisibility(type);
    }

    default void showInsets(int types, boolean fromIme) {
    }

    default void hideInsets(int types, boolean fromIme) {
    }

    default boolean canShowTransient() {
        return false;
    }

    static WindowState asWindowOrNull(InsetsControlTarget target) {
        if (target != null) {
            return target.getWindow();
        }
        return null;
    }

    default boolean canShowStatusBar() {
        return true;
    }
}
