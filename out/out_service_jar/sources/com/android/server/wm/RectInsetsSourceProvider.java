package com.android.server.wm;

import android.graphics.Rect;
import android.util.Slog;
import android.view.InsetsSource;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class RectInsetsSourceProvider extends InsetsSourceProvider {
    private static final String TAG = WmsExt.TAG;

    @Override // com.android.server.wm.InsetsSourceProvider
    public /* bridge */ /* synthetic */ void dump(PrintWriter printWriter, String str) {
        super.dump(printWriter, str);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RectInsetsSourceProvider(InsetsSource source, InsetsStateController stateController, DisplayContent displayContent) {
        super(source, stateController, displayContent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setRect(Rect rect) {
        this.mSource.setFrame(rect);
        this.mSource.setVisible(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.android.server.wm.InsetsSourceProvider
    public void onPostLayout() {
        if (WindowManagerDebugConfig.DEBUG) {
            Slog.d(TAG, "onPostLayout(), not calling super.onPostLayout().");
        }
    }
}
