package com.android.server.wm;

import android.util.proto.ProtoOutputStream;
import android.view.IWindow;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public interface InputTarget {
    boolean canScreenshotIme();

    void dumpProto(ProtoOutputStream protoOutputStream, long j, int i);

    ActivityRecord getActivityRecord();

    DisplayContent getDisplayContent();

    int getDisplayId();

    IWindow getIWindow();

    InsetsControlTarget getImeControlTarget();

    int getPid();

    int getUid();

    WindowState getWindowState();

    void handleTapOutsideFocusInsideSelf();

    void handleTapOutsideFocusOutsideSelf();

    boolean isInputMethodClientFocus(int i, int i2);

    boolean receiveFocusFromTapOutside();

    boolean shouldControlIme();

    void unfreezeInsetsAfterStartInput();
}
