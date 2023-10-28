package com.transsion.hubcore.server.display;

import com.transsion.hubcore.server.display.ITranVirtualDisplayAdapter;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranVirtualDisplayAdapter {
    public static final TranClassInfo<ITranVirtualDisplayAdapter> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.display.TranVirtualDisplayAdapterImpl", ITranVirtualDisplayAdapter.class, new Supplier() { // from class: com.transsion.hubcore.server.display.ITranVirtualDisplayAdapter$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranVirtualDisplayAdapter.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranVirtualDisplayAdapter {
    }

    static ITranVirtualDisplayAdapter Instance() {
        return (ITranVirtualDisplayAdapter) classInfo.getImpl();
    }

    default int updateDeviceInfoFlag(int flag) {
        return 0;
    }
}
