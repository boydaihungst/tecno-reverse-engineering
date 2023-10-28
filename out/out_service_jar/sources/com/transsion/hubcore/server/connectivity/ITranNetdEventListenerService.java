package com.transsion.hubcore.server.connectivity;

import android.content.Context;
import com.transsion.hubcore.utils.TranClassInfo;
import java.util.function.Supplier;
/* loaded from: classes2.dex */
public interface ITranNetdEventListenerService {
    public static final TranClassInfo<ITranNetdEventListenerService> classInfo = new TranClassInfo<>("com.transsion.hubcore.server.connectivity.TranNetdEventListenerServiceImpl", ITranNetdEventListenerService.class, new Supplier() { // from class: com.transsion.hubcore.server.connectivity.ITranNetdEventListenerService$$ExternalSyntheticLambda0
        @Override // java.util.function.Supplier
        public final Object get() {
            return ITranNetdEventListenerService.lambda$static$0();
        }
    });

    static /* synthetic */ ITranNetdEventListenerService lambda$static$0() {
        return new ITranNetdEventListenerService() { // from class: com.transsion.hubcore.server.connectivity.ITranNetdEventListenerService.1
        };
    }

    static ITranNetdEventListenerService Instance() {
        return (ITranNetdEventListenerService) classInfo.getImpl();
    }

    default void create(Context context) {
    }

    default void onConnectEvent(int netId, int error, int latencyMs, String ipAddr, int port, int uid) {
    }

    default void onDnsEvent(int netId, int eventType, int returnCode, int latencyMs, String hostname, String[] ipAddresses, int ipAddressesCount, int uid) {
    }

    default void onNat64PrefixEvent(int netId, boolean added, String prefixString, int prefixLength) {
    }

    default void onPrivateDnsValidationEvent(int netId, String ipAddress, String hostname, boolean validated) {
    }

    default void onWakeupEvent(String prefix, int uid, int ethertype, int ipNextHeader, byte[] dstHw, String srcIp, String dstIp, int srcPort, int dstPort, long timestampNs) {
    }

    default void onTcpSocketStatsEvent(int netId, int sent, int lost, int rttUs, int sentAckDiffMs) {
    }
}
