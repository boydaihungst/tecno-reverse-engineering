package com.android.server.wm;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public interface WindowContainerListener extends ConfigurationContainerListener {
    default void onDisplayChanged(DisplayContent dc) {
    }

    default void onRemoved() {
    }
}
