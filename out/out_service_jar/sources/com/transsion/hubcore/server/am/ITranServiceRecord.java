package com.transsion.hubcore.server.am;

import com.transsion.hubcore.griffin.lib.app.TranAppInfo;
import com.transsion.hubcore.server.am.ITranServiceRecord;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranServiceRecord {
    public static final TranClassInfo<ITranServiceRecord> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranServiceRecordImpl", ITranServiceRecord.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranServiceRecord$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranServiceRecord.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranServiceRecord {
    }

    static ITranServiceRecord Instance() {
        return (ITranServiceRecord) classInfo.getImpl();
    }

    default TranAppInfo initServiceRecord(String packageName) {
        return null;
    }
}
