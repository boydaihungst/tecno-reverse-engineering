package com.transsion.hubcore.server;

import android.content.Context;
import com.android.server.SystemService;
import com.android.server.am.ActivityManagerService;
import com.android.server.utils.TimingsTraceAndSlog;
import com.transsion.hubcore.server.ITranSystemServer;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranSystemServer {
    public static final TranClassInfo<ITranSystemServer> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.TranSystemServerImpl", ITranSystemServer.class, new Supplier() { // from class: com.transsion.hubcore.server.ITranSystemServer$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return new ITranSystemServer.DefaultImpl();
        }
    });

    /* loaded from: classes2.dex */
    public static class DefaultImpl implements ITranSystemServer {
    }

    static ITranSystemServer Instance() {
        return (ITranSystemServer) classInfo.getImpl();
    }

    default void onStartSystemService(SystemService service) {
    }

    default void onStartBootPhase(int phase) {
    }

    default void onSystemServiceManagerCreated(Object systemServer) {
    }

    default void startOSCSAll(TimingsTraceAndSlog t, Context context, ActivityManagerService activityManagerService) {
    }

    default void startOSCSAiAndMultiWindow() {
    }

    default void startOSCSEnd(TimingsTraceAndSlog t, Context context) {
    }

    default void startMagellan(Context context) {
    }

    default void startDefaultFileBackup(Context context) {
    }
}
