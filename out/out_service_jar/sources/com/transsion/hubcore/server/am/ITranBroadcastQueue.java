package com.transsion.hubcore.server.am;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import com.transsion.hubcore.griffin.lib.app.TranProcessWrapper;
import com.transsion.hubcore.server.am.ITranBroadcastQueue;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranBroadcastQueue {
    public static final TranClassInfo<ITranBroadcastQueue> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.am.TranBroadcastQueueImpl", ITranBroadcastQueue.class, new Supplier() { // from class: com.transsion.hubcore.server.am.ITranBroadcastQueue$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranBroadcastQueue.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranBroadcastQueue {
    }

    static ITranBroadcastQueue Instance() {
        return (ITranBroadcastQueue) classInfo.getImpl();
    }

    default boolean hookPmLimitStartProcessForReceiver(int callerPid, int callerUid, String callerPackage, TranProcessWrapper callerProc, Intent intent, ActivityInfo curReceiver, ComponentName curComponent, boolean ordered) {
        return false;
    }
}
