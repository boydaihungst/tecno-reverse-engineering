package com.transsion.hubcore.server.appbinding;

import android.content.Context;
import com.android.server.appbinding.finders.AppServiceFinder;
import com.transsion.hubcore.server.appbinding.ITranAppBindingService;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.ArrayList;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranAppBindingService {
    public static final boolean DEBUG = true;
    public static final String TAG = "ITranAppBindingService";
    public static final TranClassInfo<ITranAppBindingService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.appbinding.TranAppBindingServiceImpl", ITranAppBindingService.class, new Supplier() { // from class: com.transsion.hubcore.server.appbinding.ITranAppBindingService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranAppBindingService.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranAppBindingService {
    }

    static ITranAppBindingService Instance() {
        return (ITranAppBindingService) classInfo.getImpl();
    }

    default void onConstruct(ArrayList<AppServiceFinder> apps, Context context) {
    }
}
