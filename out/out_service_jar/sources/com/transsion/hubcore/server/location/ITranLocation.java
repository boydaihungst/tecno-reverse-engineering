package com.transsion.hubcore.server.location;

import android.location.Location;
import com.transsion.hubcore.server.location.ITranLocation;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranLocation {
    public static final TranClassInfo<ITranLocation> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.location.TranLocationImpl", ITranLocation.class, new Supplier() { // from class: com.transsion.hubcore.server.location.ITranLocation$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranLocation.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranLocation {
    }

    static ITranLocation Instance() {
        return (ITranLocation) classInfo.getImpl();
    }

    default void wz1(Location wzs) {
    }
}
