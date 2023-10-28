package com.transsion.hubcore.server.display;

import android.content.Context;
import com.transsion.hubcore.server.display.ITranColorDisplayService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranColorDisplayService {
    public static final TranClassInfo<ITranColorDisplayService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.display.TranColorDisplayServiceImpl", ITranColorDisplayService.class, new Supplier() { // from class: com.transsion.hubcore.server.display.ITranColorDisplayService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranColorDisplayService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranColorDisplayService {
    }

    static ITranColorDisplayService Instance() {
        return (ITranColorDisplayService) classInfo.getImpl();
    }

    default void init(Context context, int userHandle) {
    }
}
