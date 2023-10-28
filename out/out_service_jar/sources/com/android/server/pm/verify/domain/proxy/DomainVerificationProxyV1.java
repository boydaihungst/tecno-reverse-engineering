package com.android.server.pm.verify.domain.proxy;

import android.app.BroadcastOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationInfo;
import android.os.Process;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.verify.domain.DomainVerificationCollector;
import com.android.server.pm.verify.domain.DomainVerificationManagerInternal;
import com.android.server.pm.verify.domain.proxy.DomainVerificationProxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
/* loaded from: classes2.dex */
public class DomainVerificationProxyV1 implements DomainVerificationProxy {
    private static final boolean DEBUG_BROADCASTS = false;
    private static final String TAG = "DomainVerificationProxyV1";
    private final DomainVerificationCollector mCollector;
    private final Connection mConnection;
    private final Context mContext;
    private final DomainVerificationManagerInternal mManager;
    private final ComponentName mVerifierComponent;
    private final Object mLock = new Object();
    private final ArrayMap<Integer, Pair<UUID, String>> mRequests = new ArrayMap<>();
    private int mVerificationToken = 0;

    /* loaded from: classes2.dex */
    public interface Connection extends DomainVerificationProxy.BaseConnection {
        AndroidPackage getPackage(String str);
    }

    public DomainVerificationProxyV1(Context context, DomainVerificationManagerInternal manager, DomainVerificationCollector collector, Connection connection, ComponentName verifierComponent) {
        this.mContext = context;
        this.mConnection = connection;
        this.mVerifierComponent = verifierComponent;
        this.mManager = manager;
        this.mCollector = collector;
    }

    public static void queueLegacyVerifyResult(Context context, Connection connection, int verificationId, int verificationCode, List<String> failedDomains, int callingUid) {
        context.enforceCallingOrSelfPermission("android.permission.INTENT_FILTER_VERIFICATION_AGENT", "Only the intent filter verification agent can verify applications");
        connection.schedule(3, new Response(callingUid, verificationId, verificationCode, failedDomains));
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public void sendBroadcastForPackages(Set<String> packageNames) {
        synchronized (this.mLock) {
            int size = this.mRequests.size();
            for (int index = size - 1; index >= 0; index--) {
                Pair<UUID, String> pair = this.mRequests.valueAt(index);
                if (packageNames.contains(pair.second)) {
                    this.mRequests.removeAt(index);
                }
            }
        }
        this.mConnection.schedule(2, packageNames);
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public boolean runMessage(int messageCode, Object object) {
        switch (messageCode) {
            case 2:
                Set<String> packageNames = (Set) object;
                ArrayMap<Integer, Pair<UUID, String>> newRequests = new ArrayMap<>(packageNames.size());
                synchronized (this.mLock) {
                    for (String packageName : packageNames) {
                        UUID domainSetId = this.mManager.getDomainVerificationInfoId(packageName);
                        if (domainSetId != null) {
                            int i = this.mVerificationToken;
                            this.mVerificationToken = i + 1;
                            newRequests.put(Integer.valueOf(i), Pair.create(domainSetId, packageName));
                        }
                    }
                    this.mRequests.putAll((ArrayMap<? extends Integer, ? extends Pair<UUID, String>>) newRequests);
                }
                sendBroadcasts(newRequests);
                return true;
            case 3:
                Response response = (Response) object;
                Pair<UUID, String> pair = this.mRequests.get(Integer.valueOf(response.verificationId));
                if (pair == null) {
                    return true;
                }
                UUID domainSetId2 = (UUID) pair.first;
                String packageName2 = (String) pair.second;
                try {
                    DomainVerificationInfo info = this.mManager.getDomainVerificationInfo(packageName2);
                    if (info != null && Objects.equals(domainSetId2, info.getIdentifier())) {
                        AndroidPackage pkg = this.mConnection.getPackage(packageName2);
                        if (pkg == null) {
                            return true;
                        }
                        ArraySet<? extends String> failedDomains = new ArraySet<>(response.failedDomains);
                        Map<String, Integer> hostToStateMap = info.getHostToStateMap();
                        Set<String> hostKeySet = hostToStateMap.keySet();
                        ArraySet<String> successfulDomains = new ArraySet<>(hostKeySet);
                        successfulDomains.removeAll(failedDomains);
                        int size = successfulDomains.size();
                        for (int index = size - 1; index >= 0; index--) {
                            String domain = successfulDomains.valueAt(index);
                            if (domain.startsWith("*.")) {
                                String nonWildcardDomain = domain.substring(2);
                                if (failedDomains.contains(nonWildcardDomain)) {
                                    failedDomains.add(domain);
                                    successfulDomains.removeAt(index);
                                    if (!hostKeySet.contains(nonWildcardDomain)) {
                                        failedDomains.remove(nonWildcardDomain);
                                    }
                                }
                            }
                        }
                        int callingUid = response.callingUid;
                        if (!successfulDomains.isEmpty()) {
                            try {
                                if (this.mManager.setDomainVerificationStatusInternal(callingUid, domainSetId2, successfulDomains, 1) != 0) {
                                    Slog.e(TAG, "Failure reporting successful domains for " + packageName2);
                                }
                            } catch (Exception e) {
                                Slog.e(TAG, "Failure reporting successful domains for " + packageName2, e);
                            }
                        }
                        if (!failedDomains.isEmpty()) {
                            try {
                                if (this.mManager.setDomainVerificationStatusInternal(callingUid, domainSetId2, failedDomains, 6) != 0) {
                                    Slog.e(TAG, "Failure reporting failed domains for " + packageName2);
                                    return true;
                                }
                                return true;
                            } catch (Exception e2) {
                                Slog.e(TAG, "Failure reporting failed domains for " + packageName2, e2);
                                return true;
                            }
                        }
                        return true;
                    }
                    return true;
                } catch (PackageManager.NameNotFoundException e3) {
                    return true;
                }
            default:
                return false;
        }
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public boolean isCallerVerifier(int callingUid) {
        return this.mConnection.isCallerPackage(callingUid, this.mVerifierComponent.getPackageName());
    }

    private void sendBroadcasts(ArrayMap<Integer, Pair<UUID, String>> verifications) {
        ArrayMap<Integer, Pair<UUID, String>> arrayMap = verifications;
        long allowListTimeout = this.mConnection.getPowerSaveTempWhitelistAppDuration();
        this.mConnection.getDeviceIdleInternal().addPowerSaveTempWhitelistApp(Process.myUid(), this.mVerifierComponent.getPackageName(), allowListTimeout, 0, true, (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_DOMAIN_VERIFICATION_V1, "domain verification agent");
        int size = verifications.size();
        int index = 0;
        while (index < size) {
            int verificationId = arrayMap.keyAt(index).intValue();
            String packageName = (String) arrayMap.valueAt(index).second;
            AndroidPackage pkg = this.mConnection.getPackage(packageName);
            String hostsString = buildHostsString(pkg);
            Intent intent = new Intent("android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION").setComponent(this.mVerifierComponent).putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_ID", verificationId).putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_URI_SCHEME", "https").putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_HOSTS", hostsString).putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_PACKAGE_NAME", packageName).addFlags(268435456);
            BroadcastOptions options = BroadcastOptions.makeBasic();
            options.setTemporaryAppAllowlist(allowListTimeout, 0, (int) FrameworkStatsLog.APP_BACKGROUND_RESTRICTIONS_INFO__EXEMPTION_REASON__REASON_DOMAIN_VERIFICATION_V1, "");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM, null, options.toBundle());
            index++;
            arrayMap = verifications;
        }
    }

    private String buildHostsString(AndroidPackage pkg) {
        ArraySet<String> domains = this.mCollector.collectValidAutoVerifyDomains(pkg);
        StringBuilder builder = new StringBuilder();
        int size = domains.size();
        for (int index = 0; index < size; index++) {
            if (index > 0) {
                builder.append(" ");
            }
            String domain = domains.valueAt(index);
            if (domain.startsWith("*.")) {
                domain = domain.substring(2);
            }
            builder.append(domain);
        }
        return builder.toString();
    }

    @Override // com.android.server.pm.verify.domain.proxy.DomainVerificationProxy
    public ComponentName getComponentName() {
        return this.mVerifierComponent;
    }

    /* loaded from: classes2.dex */
    private static class Response {
        public final int callingUid;
        public final List<String> failedDomains;
        public final int verificationCode;
        public final int verificationId;

        private Response(int callingUid, int verificationId, int verificationCode, List<String> failedDomains) {
            this.callingUid = callingUid;
            this.verificationId = verificationId;
            this.verificationCode = verificationCode;
            this.failedDomains = failedDomains == null ? Collections.emptyList() : failedDomains;
        }
    }
}
