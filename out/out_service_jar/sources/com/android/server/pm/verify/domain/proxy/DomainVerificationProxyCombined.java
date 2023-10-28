package com.android.server.pm.verify.domain.proxy;

import android.content.ComponentName;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class DomainVerificationProxyCombined implements DomainVerificationProxy {
    private final DomainVerificationProxy mProxyV1;
    private final DomainVerificationProxy mProxyV2;

    /* JADX INFO: Access modifiers changed from: package-private */
    public DomainVerificationProxyCombined(DomainVerificationProxy proxyV1, DomainVerificationProxy proxyV2) {
        this.mProxyV1 = proxyV1;
        this.mProxyV2 = proxyV2;
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public void sendBroadcastForPackages(Set<String> packageNames) {
        this.mProxyV2.sendBroadcastForPackages(packageNames);
        this.mProxyV1.sendBroadcastForPackages(packageNames);
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public boolean runMessage(int messageCode, Object object) {
        boolean resultV2 = this.mProxyV2.runMessage(messageCode, object);
        boolean resultV1 = this.mProxyV1.runMessage(messageCode, object);
        return resultV2 || resultV1;
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public boolean isCallerVerifier(int callingUid) {
        return this.mProxyV2.isCallerVerifier(callingUid) || this.mProxyV1.isCallerVerifier(callingUid);
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public ComponentName getComponentName() {
        return this.mProxyV2.getComponentName();
    }
}
