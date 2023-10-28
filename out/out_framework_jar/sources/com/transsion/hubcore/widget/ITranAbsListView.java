package com.transsion.hubcore.widget;

import com.transsion.hubcore.utils.TranClassInfo;
import com.transsion.hubcore.widget.ITranAbsListView;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranAbsListView {
    public static final TranClassInfo<ITranAbsListView> classInfo = new TranClassInfo<>("com.transsion.hubcore.widget.TranAbsListViewImpl", ITranAbsListView.class, new Supplier() { // from class: com.transsion.hubcore.widget.ITranAbsListView$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAbsListView.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranAbsListView {
    }

    static ITranAbsListView Instance() {
        return classInfo.getImpl();
    }

    default void reportScrollStateChange(int newState) {
    }
}
