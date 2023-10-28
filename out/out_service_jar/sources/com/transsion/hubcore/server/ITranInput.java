package com.transsion.hubcore.server;

import android.content.Context;
import android.hardware.input.IGestureListener;
import android.os.Bundle;
import android.view.MotionEvent;
import com.transsion.hubcore.server.ITranInput;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranInput {
    public static final TranClassInfo<ITranInput> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.TranInputImpl", ITranInput.class, new Supplier() { // from class: com.transsion.hubcore.server.ITranInput$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranInput.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranInput {
    }

    static ITranInput Instance() {
        return (ITranInput) classInfo.getImpl();
    }

    default void init(Context context) {
    }

    default boolean checkMotionEvent(MotionEvent event) {
        return false;
    }

    default int checkEdgeMotionEvent(MotionEvent event) {
        return 0;
    }

    default void registerGestureListener(IGestureListener listener) {
    }

    default void unRegisterGestureListener(IGestureListener listener) {
    }

    default String getMagellanConfig() {
        return "";
    }

    default boolean updateTrackingData(int type, Bundle trackingData) {
        return false;
    }
}
