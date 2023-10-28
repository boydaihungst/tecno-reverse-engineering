package com.transsion.hubcore.server.am;

import com.transsion.hubcore.griffin.lib.app.TranAppInfo;
import com.transsion.hubcore.server.am.ITranContentProviderRecord;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranContentProviderRecord {
    public static final TranClassInfo<ITranContentProviderRecord> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranContentProviderRecordImpl", ITranContentProviderRecord.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranContentProviderRecord$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranContentProviderRecord.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranContentProviderRecord {
    }

    static ITranContentProviderRecord Instance() {
        return (ITranContentProviderRecord) classInfo.getImpl();
    }

    default TranAppInfo hookAppInfoInstance(String appInfo) {
        return null;
    }

    default TranAppInfo hookAppInfoCopy(String appInfo) {
        return null;
    }
}
