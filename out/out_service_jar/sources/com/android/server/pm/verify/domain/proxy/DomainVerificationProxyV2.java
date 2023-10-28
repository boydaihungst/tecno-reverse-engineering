package com.android.server.pm.verify.domain.proxy;

import android.app.BroadcastOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.verify.domain.DomainVerificationRequest;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxy;
import java.util.Set;
/* loaded from: classes2.dex */
public class DomainVerificationProxyV2 implements DomainVerificationProxy {
    private static final boolean DEBUG_BROADCASTS = false;
    private static final String TAG = "DomainVerificationProxyV2";
    private final Connection mConnection;
    private final Context mContext;
    private final ComponentName mVerifierComponent;

    /* loaded from: classes2.dex */
    public interface Connection extends DomainVerificationProxy.BaseConnection {
    }

    public DomainVerificationProxyV2(Context context, Connection connection, ComponentName verifierComponent) {
        this.mContext = context;
        this.mConnection = connection;
        this.mVerifierComponent = verifierComponent;
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public void sendBroadcastForPackages(Set<String> packageNames) {
        this.mConnection.schedule(1, packageNames);
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public boolean runMessage(int messageCode, Object object) {
        switch (messageCode) {
            case 1:
                Set<String> packageNames = (Set) object;
                Parcelable domainVerificationRequest = new DomainVerificationRequest(packageNames);
                long allowListTimeout = this.mConnection.getPowerSaveTempWhitelistAppDuration();
                BroadcastOptions options = BroadcastOptions.makeBasic();
                options.setTemporaryAppAllowlist(allowListTimeout, 0, 308, "");
                this.mConnection.getDeviceIdleInternal().addPowerSaveTempWhitelistApp(Process.myUid(), this.mVerifierComponent.getPackageName(), allowListTimeout, 0, true, 308, "domain verification agent");
                Intent intent = new Intent("android.intent.action.DOMAINS_NEED_VERIFICATION").setComponent(this.mVerifierComponent).putExtra("android.content.pm.verify.domain.extra.VERIFICATION_REQUEST", domainVerificationRequest).addFlags(268435456);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM, null, options.toBundle());
                return true;
            default:
                return false;
        }
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public boolean isCallerVerifier(int callingUid) {
        return this.mConnection.isCallerPackage(callingUid, this.mVerifierComponent.getPackageName());
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public ComponentName getComponentName() {
        return this.mVerifierComponent;
    }
}
