package com.android.server.appbinding;

import android.content.Context;
import com.android.server.appbinding.IAppBindingServiceLice;
import com.android.server.appbinding.finders.AppServiceFinder;
import com.transsion.annotation.OSBridge;
import com.transsion.lice.LiceInfo;
import java.util.ArrayList;
import java.util.function.Supplier;
@OSBridge(client = OSBridge.Client.LICE_INTERFACE_SERVICES)
/* loaded from: classes.dex */
public interface IAppBindingServiceLice {
    public static final boolean DEBUG = true;
    public static final String TAG = "IAppBindingServiceLice";
    public static final LiceInfo<IAppBindingServiceLice> sLiceInfo = new LiceInfo<>("com.transsion.server.appbinding.AppBindingServiceLice", IAppBindingServiceLice.class, new Supplier() { // from class: com.android.server.appbinding.IAppBindingServiceLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new IAppBindingServiceLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements IAppBindingServiceLice {
    }

    static IAppBindingServiceLice Instance() {
        return (IAppBindingServiceLice) sLiceInfo.getImpl();
    }

    default void onConstruct(ArrayList<AppServiceFinder> apps, Context context) {
    }
}
