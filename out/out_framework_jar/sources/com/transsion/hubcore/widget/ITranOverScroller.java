package com.transsion.hubcore.widget;

import com.transsion.hubcore.utils.TranClassInfo;
import com.transsion.hubcore.widget.ITranOverScroller;
import java.util.function.Supplier;
/* loaded from: classes4.dex */
public interface ITranOverScroller {
    public static final TranClassInfo<ITranOverScroller> classInfo = new TranClassInfo<>("com.transsion.hubcore.widget.TranOverScrollerImpl", ITranOverScroller.class, new Supplier() { // from class: com.transsion.hubcore.widget.ITranOverScroller$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranOverScroller.DefaultImpl();
        }
    });

    /* loaded from: classes4.dex */
    public static class DefaultImpl implements ITranOverScroller {
    }

    static ITranOverScroller Instance() {
        return classInfo.getImpl();
    }

    default void computeScrollOffsetOnFlingMode(int state, int scrollType, float velocity) {
    }

    default void update(int scrollType, float velocity) {
    }

    default void hookFinish(int scrollType) {
    }
}
