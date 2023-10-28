package android.view.inspector;

import android.view.View;
import android.view.WindowManagerGlobal;
import java.util.List;
/* loaded from: classes3.dex */
public final class WindowInspector {
    private WindowInspector() {
    }

    public static List<View> getGlobalWindowViews() {
        return WindowManagerGlobal.getInstance().getWindowViews();
    }
}
