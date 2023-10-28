package com.transsion.hubcore.view;

import android.content.Context;
import android.view.IScrollCaptureResponseListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranView {
    public static final TranClassInfo<ITranView> classInfo = new TranClassInfo<>("com.transsion.hubcore.view.TranViewImpl", ITranView.class, new Supplier() { // from class: com.transsion.hubcore.view.ITranView$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranView.lambda$static$0();
        }
    });

    static /* synthetic */ ITranView lambda$static$0() {
        return new ITranView() { // from class: com.transsion.hubcore.view.ITranView.1
        };
    }

    static ITranView Instance() {
        return classInfo.getImpl();
    }

    default void initViewGroup(Context context) {
    }

    default void computeScroll(ViewGroup viewgroup) {
    }

    default void dispatchTouch(ViewGroup viewgroup, MotionEvent ev) {
    }

    default void checkInterruptScreenShot(Context context, ViewGroup viewgroup) {
    }

    default void setStartInMultiWindow(int viewId, String pkg) {
    }

    default void collapsePanels(int viewId) {
    }

    default boolean handleScrollCaptureRequest(Context context, View view, IScrollCaptureResponseListener listener) {
        return false;
    }

    default float getScrollFriction(float oriScrollFriction) {
        return oriScrollFriction;
    }

    default int getScaledTouchSlop(int oriTouchSlop) {
        return oriTouchSlop;
    }

    default boolean isAppturboSupport() {
        return false;
    }

    default float getVelocity(boolean isXVelocity, float velocity) {
        return velocity;
    }
}
