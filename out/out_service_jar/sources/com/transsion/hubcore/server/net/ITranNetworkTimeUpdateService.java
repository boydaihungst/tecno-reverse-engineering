package com.transsion.hubcore.server.net;

import android.content.Context;
import android.util.NtpTrustedTime;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranNetworkTimeUpdateService {
    public static final TranClassInfo<ITranNetworkTimeUpdateService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.net.TranNetworkTimeUpdateServiceImpl", ITranNetworkTimeUpdateService.class, new Supplier() { // from class: com.transsion.hubcore.server.net.ITranNetworkTimeUpdateService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranNetworkTimeUpdateService.lambda$static$0();
        }
    });

    static /* synthetic */ ITranNetworkTimeUpdateService lambda$static$0() {
        return new ITranNetworkTimeUpdateService() { // from class: com.transsion.hubcore.server.net.ITranNetworkTimeUpdateService.1
        };
    }

    static ITranNetworkTimeUpdateService Instance() {
        return (ITranNetworkTimeUpdateService) classInfo.getImpl();
    }

    default boolean forceRefresh(Context context, NtpTrustedTime time, int counter) {
        return false;
    }
}
