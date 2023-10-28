package com.transsion.hubcore.server.am;

import android.os.Bundle;
import com.transsion.hubcore.server.am.ITranPendingIntentRecord;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranPendingIntentRecord {
    public static final TranClassInfo<ITranPendingIntentRecord> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranPendingIntentRecordImpl", ITranPendingIntentRecord.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranPendingIntentRecord$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranPendingIntentRecord.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranPendingIntentRecord {
    }

    static ITranPendingIntentRecord Instance() {
        return (ITranPendingIntentRecord) classInfo.getImpl();
    }

    default void initMultiWindowFromOption(String tag, String packageName, Bundle options) {
    }
}
