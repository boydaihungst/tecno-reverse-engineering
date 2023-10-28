package android.view;

import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
/* loaded from: classes3.dex */
public interface InsetsAnimationControlRunner {
    void cancel();

    void dumpDebug(ProtoOutputStream protoOutputStream, long j);

    WindowInsetsAnimation getAnimation();

    int getAnimationType();

    int getControllingTypes();

    int getTypes();

    void notifyControlRevoked(int i);

    void updateSurfacePosition(SparseArray<InsetsSourceControl> sparseArray);

    default boolean controlsInternalType(int type) {
        return InsetsState.toInternalType(getTypes()).contains(Integer.valueOf(type));
    }
}
