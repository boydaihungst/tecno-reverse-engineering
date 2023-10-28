package com.transsion.hubcore.view;

import android.content.Context;
import android.view.SurfaceControl;
import android.view.SurfaceView;
import com.transsion.hubcore.utils.TranClassInfo;
import com.transsion.hubcore.view.ITranSurface;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranSurface {
    public static final TranClassInfo<ITranSurface> classInfo = new TranClassInfo<>("com.transsion.hubcore.view.TranSurfaceImpl", ITranSurface.class, new Supplier() { // from class: com.transsion.hubcore.view.ITranSurface$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranSurface.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranSurface {
    }

    static ITranSurface Instance() {
        return classInfo.getImpl();
    }

    default void constructSurface(Context context) {
    }

    default void onDetachedFromWindow(SurfaceView.TranSurfaceView tranSurfaceView) {
    }

    default void updateSurface(SurfaceView.TranSurfaceView tranSurfaceView) {
    }

    default void surfaceCreated(SurfaceControl.Transaction t, SurfaceView.TranSurfaceView tranSurfaceView) {
    }

    default void surfaceDestroyed(SurfaceView.TranSurfaceView tranSurfaceView) {
    }

    default void hookGetFps(float fps) {
    }

    default boolean debugFullScreen() {
        return false;
    }
}
