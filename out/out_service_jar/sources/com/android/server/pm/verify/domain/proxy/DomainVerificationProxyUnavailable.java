package com.android.server.pm.verify.domain.proxy;

import android.content.ComponentName;
import java.util.Set;
/* loaded from: classes2.dex */
public class DomainVerificationProxyUnavailable implements DomainVerificationProxy {
    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public void sendBroadcastForPackages(Set<String> packageNames) {
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public boolean runMessage(int messageCode, Object object) {
        return false;
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public boolean isCallerVerifier(int callingUid) {
        return false;
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public ComponentName getComponentName() {
        return null;
    }
}
