package android.view;

import android.graphics.Region;
import android.view.SurfaceControl;
/* loaded from: classes3.dex */
public interface AttachedSurfaceControl {

    /* loaded from: classes3.dex */
    public interface OnBufferTransformHintChangedListener {
        void onBufferTransformHintChanged(int i);
    }

    boolean applyTransactionOnDraw(SurfaceControl.Transaction transaction);

    SurfaceControl.Transaction buildReparentTransaction(SurfaceControl surfaceControl);

    default int getBufferTransformHint() {
        return 0;
    }

    default void addOnBufferTransformHintChangedListener(OnBufferTransformHintChangedListener listener) {
    }

    default void removeOnBufferTransformHintChangedListener(OnBufferTransformHintChangedListener listener) {
    }

    default void setTouchableRegion(Region r) {
    }
}
