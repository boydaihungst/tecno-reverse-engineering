package com.transsion.hubcore.server.am;

import com.android.server.am.ProcessRecord;
import com.transsion.hubcore.server.am.ITranProcessServiceRecord;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranProcessServiceRecord {
    public static final TranClassInfo<ITranProcessServiceRecord> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranProcessServiceRecordImpl", ITranProcessServiceRecord.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranProcessServiceRecord$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranProcessServiceRecord.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranProcessServiceRecord {
    }

    static ITranProcessServiceRecord instance() {
        return (ITranProcessServiceRecord) classInfo.getImpl();
    }

    default void hookConstructor() {
    }

    default void hookSetHasForegroundServices(ProcessRecord app, boolean hasForegroundServices, int fgServiceTypes) {
    }
}
