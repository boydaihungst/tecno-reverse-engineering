package com.transsion.hubcore.server.am;

import com.android.server.am.ProcessStateRecord;
import com.transsion.hubcore.server.am.ITranProcessStateRecord;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranProcessStateRecord {
    public static final TranClassInfo<ITranProcessStateRecord> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranProcessStateRecordImpl", ITranProcessStateRecord.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranProcessStateRecord$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranProcessStateRecord.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranProcessStateRecord {
    }

    static ITranProcessStateRecord Instance() {
        return (ITranProcessStateRecord) classInfo.getImpl();
    }

    default int hookGetSetSchedGroup(ProcessStateRecord mProcessStateRecord) {
        return 2;
    }
}
