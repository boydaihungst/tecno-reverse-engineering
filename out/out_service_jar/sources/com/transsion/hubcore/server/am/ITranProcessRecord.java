package com.transsion.hubcore.server.am;

import android.content.pm.ApplicationInfo;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.PackageList;
import com.android.server.am.ProcessRecord;
import com.transsion.hubcore.griffin.lib.app.TranProcessInfo;
import com.transsion.hubcore.server.am.ITranProcessRecord;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranProcessRecord {
    public static final TranClassInfo<ITranProcessRecord> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranProcessRecordImpl", ITranProcessRecord.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranProcessRecord$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranProcessRecord.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranProcessRecord {
    }

    static ITranProcessRecord Instance() {
        return (ITranProcessRecord) classInfo.getImpl();
    }

    default void inMuteProcessList(ActivityManagerService mService, String packageName, int pid, int uid) {
    }

    default void removeFromMuteProcessList(ActivityManagerService mService, String packageName, int pid, int multiWindowId) {
    }

    default TranProcessInfo initMyProcessWrapper(String processName, int uid, PackageList pkgList, ApplicationInfo info) {
        return null;
    }

    default boolean canSkipPendingStart(boolean mPersistent, ApplicationInfo info) {
        return false;
    }

    default boolean hookIsPersistent(ProcessRecord mProcessRecord) {
        return false;
    }
}
