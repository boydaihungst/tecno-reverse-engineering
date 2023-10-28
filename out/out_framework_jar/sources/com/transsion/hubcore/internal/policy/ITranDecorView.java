package com.transsion.hubcore.internal.policy;

import android.content.res.Configuration;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.widget.DecorCaptionView;
import com.transsion.hubcore.internal.policy.ITranDecorView;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranDecorView {
    public static final TranClassInfo<ITranDecorView> classInfo = new TranClassInfo<>("com.transsion.hubcore.internal.policy.TranDecorViewImpl", ITranDecorView.class, new Supplier() { // from class: com.transsion.hubcore.internal.policy.ITranDecorView$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranDecorView.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranDecorView {
    }

    static ITranDecorView Instance() {
        return classInfo.getImpl();
    }

    default void multiWindowOnLayout(MotionEvent event, ViewGroup viewGroup, DecorCaptionView decorCaptionView) {
    }

    default boolean multiWindowUpdateColorViews(boolean isThunderbackWindow, DecorCaptionView decorCaptionView, int statusBarColor) {
        return false;
    }

    default boolean multiWindowUpdateThunderbackActivity(View view, Configuration config, boolean isCaptionShowByViewRootImplSet) {
        return false;
    }

    default boolean multiWindowAddView(ViewGroup decorView, DecorCaptionView decorCaptionView, Configuration config) {
        return false;
    }

    default void multiWindowRemoveView(ViewGroup decorView, ViewGroup contentRoot, DecorCaptionView decorCaptionView, Configuration config) {
    }

    default boolean multiWindowOnResourcesLoadedUpdateView(View root, ViewGroup decorView, DecorCaptionView decorCaptionView) {
        return false;
    }

    default boolean multiWindowIsThunderbackWindow(View view) {
        return false;
    }
}
