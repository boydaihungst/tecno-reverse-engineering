package com.android.server.pm.verify.domain.proxy;

import android.content.ComponentName;
import android.content.Context;
import com.android.server.DeviceIdleInternal;
import com.android.server.pm.verify.domain.DomainVerificationCollector;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxyV1;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxyV2;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public interface DomainVerificationProxy {
    public static final boolean DEBUG_PROXIES = false;
    public static final String TAG = "DomainVerificationProxy";

    /* loaded from: classes2.dex */
    public interface BaseConnection {
        DeviceIdleInternal getDeviceIdleInternal();

        long getPowerSaveTempWhitelistAppDuration();

        boolean isCallerPackage(int i, String str);

        void schedule(int i, Object obj);
    }

    ComponentName getComponentName();

    boolean isCallerVerifier(int i);

    boolean runMessage(int i, Object obj);

    void sendBroadcastForPackages(Set<String> set);

    static <ConnectionType extends DomainVerificationProxyV1.Connection & DomainVerificationProxyV2.Connection> DomainVerificationProxy makeProxy(ComponentName componentV1, ComponentName componentV2, Context context, DomainVerificationManagerInternal manager, DomainVerificationCollector collector, ConnectionType connection) {
        if (componentV2 != null && componentV1 != null && !Objects.equals(componentV2.getPackageName(), componentV1.getPackageName())) {
            componentV1 = null;
        }
        DomainVerificationProxy proxyV1 = null;
        DomainVerificationProxy proxyV2 = null;
        if (componentV1 != null) {
            proxyV1 = new DomainVerificationProxyV1(context, manager, collector, connection, componentV1);
        }
        if (componentV2 != null) {
            proxyV2 = new DomainVerificationProxyV2(context, connection, componentV2);
        }
        if (proxyV1 != null && proxyV2 != null) {
            return new DomainVerificationProxyCombined(proxyV1, proxyV2);
        }
        if (proxyV1 != null) {
            return proxyV1;
        }
        if (proxyV2 != null) {
            return proxyV2;
        }
        return new DomainVerificationProxyUnavailable();
    }
}
