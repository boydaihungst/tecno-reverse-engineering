package com.android.server.inputmethod;

import android.app.ActivityThread;
import android.content.Context;
import android.view.ContextThemeWrapper;
/* loaded from: classes.dex */
public final class InputMethodDialogWindowContext {
    private Context mDialogWindowContext;

    public Context get(int displayId) {
        Context context = this.mDialogWindowContext;
        if (context == null || context.getDisplayId() != displayId) {
            Context windowContext = ActivityThread.currentActivityThread().getSystemUiContext(displayId).createWindowContext(2012, null);
            this.mDialogWindowContext = new ContextThemeWrapper(windowContext, 16974371);
        }
        Context systemUiContext = this.mDialogWindowContext;
        return systemUiContext;
    }
}
