package com.android.server.location;

import android.location.Location;
import com.android.server.location.ILocationLice;
import com.transsion.lice.LiceInfo;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public interface ILocationLice {
    public static final LiceInfo<ILocationLice> sLiceInfo = new LiceInfo<>("com.transsion.server.location.LocationLice", ILocationLice.class, new Supplier() { // from class: com.android.server.location.ILocationLice$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ILocationLice.DefaultImpl();
        }
    });

    /* loaded from: classes.dex */
    public static class DefaultImpl implements ILocationLice {
    }

    static ILocationLice Instance() {
        return (ILocationLice) sLiceInfo.getImpl();
    }

    default void wz1(Location wzs) {
    }
}
