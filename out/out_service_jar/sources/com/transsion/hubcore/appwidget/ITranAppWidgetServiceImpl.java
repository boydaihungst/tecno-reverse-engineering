package com.transsion.hubcore.appwidget;

import com.transsion.hubcore.appwidget.ITranAppWidgetServiceImpl;
import com.transsion.hubcore.griffin.lib.app.TranWidgetInfo;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAppWidgetServiceImpl {
    public static final TranClassInfo<ITranAppWidgetServiceImpl> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.appwidget.TranAppWidgetServiceImpl", ITranAppWidgetServiceImpl.class, new Supplier() { // from class: com.transsion.hubcore.appwidget.ITranAppWidgetServiceImpl$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAppWidgetServiceImpl.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAppWidgetServiceImpl {
    }

    static ITranAppWidgetServiceImpl Instance() {
        return (ITranAppWidgetServiceImpl) classInfo.getImpl();
    }

    default void hookAppWidgetChanged(TranWidgetInfo widgetInfo, int type) {
    }

    default void hookClearWidgetsLocked() {
    }
}
