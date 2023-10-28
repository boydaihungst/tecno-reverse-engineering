package android.view;

import android.view.SurfaceControl;
import android.window.SurfaceSyncer;
import java.util.function.Consumer;
/* compiled from: D8$$SyntheticClass */
/* loaded from: classes3.dex */
public final /* synthetic */ class ViewRootImpl$8$$ExternalSyntheticLambda0 implements Consumer {
    public final /* synthetic */ SurfaceSyncer.SyncBufferCallback f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [android.view.ViewRootImpl.8.onFrameDraw(int, long):android.graphics.HardwareRenderer$FrameCommitCallback] */
    public /* synthetic */ ViewRootImpl$8$$ExternalSyntheticLambda0(SurfaceSyncer.SyncBufferCallback syncBufferCallback) {
        this.f$0 = syncBufferCallback;
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        this.f$0.onBufferReady((SurfaceControl.Transaction) obj);
    }
}
