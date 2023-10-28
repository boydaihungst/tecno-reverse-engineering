package com.android.server.wm;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DragResizeMode {
    static final int DRAG_RESIZE_MODE_DOCKED_DIVIDER = 1;
    static final int DRAG_RESIZE_MODE_FREEFORM = 0;

    DragResizeMode() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isModeAllowedForRootTask(Task rootTask, int mode) {
        switch (mode) {
            case 0:
                return rootTask.getWindowingMode() == 5;
            default:
                return false;
        }
    }
}
