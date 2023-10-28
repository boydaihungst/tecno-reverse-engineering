package com.android.server;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetd;
import android.net.INetdUnsolicitedEventListener;
import android.net.INetworkManagementEventObserver;
import android.net.ITetheringStatsProvider;
import android.net.InetAddresses;
import android.net.InterfaceConfiguration;
import android.net.InterfaceConfigurationParcel;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.Network;
import android.net.NetworkStack;
import android.net.NetworkStats;
import android.net.RouteInfo;
import android.net.UidRangeParcel;
import android.net.util.NetdService;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemClock;
import android.os.Trace;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.HexDump;
import com.android.internal.util.Preconditions;
import com.android.net.module.util.NetdUtils;
import com.android.server.NetworkManagementService;
import com.google.android.collect.Maps;
import com.transsion.hubcore.server.net.ITranNetworkManagementService;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/* loaded from: classes.dex */
public class NetworkManagementService extends INetworkManagementService.Stub {
    static final int DAEMON_MSG_MOBILE_CONN_REAL_TIME_INFO = 1;
    private static final int MAX_UID_RANGES_PER_COMMAND = 10;
    static final boolean MODIFY_OPERATION_ADD = true;
    static final boolean MODIFY_OPERATION_REMOVE = false;
    private HashMap<String, Long> mActiveAlerts;
    private HashMap<String, Long> mActiveQuotas;
    private IBatteryStats mBatteryStats;
    private final Context mContext;
    private final Handler mDaemonHandler;
    private volatile boolean mDataSaverMode;
    private final Dependencies mDeps;
    final SparseBooleanArray mFirewallChainStates;
    private volatile boolean mFirewallEnabled;
    private INetd mNetdService;
    private final NetdUnsolicitedEventListener mNetdUnsolicitedEventListener;
    private final RemoteCallbackList<INetworkManagementEventObserver> mObservers;
    private final Object mQuotaLock;
    private final Object mRulesLock;
    private volatile boolean mStrictEnabled;
    private final HashMap<ITetheringStatsProvider, String> mTetheringStatsProviders;
    private SparseBooleanArray mUidAllowOnMetered;
    private SparseIntArray mUidCleartextPolicy;
    private SparseIntArray mUidFirewallDozableRules;
    private SparseIntArray mUidFirewallLowPowerStandbyRules;
    private SparseIntArray mUidFirewallPowerSaveRules;
    private SparseIntArray mUidFirewallRestrictedRules;
    private SparseIntArray mUidFirewallRules;
    private SparseIntArray mUidFirewallStandbyRules;
    private SparseBooleanArray mUidRejectOnMetered;
    private static final String TAG = "NetworkManagement";
    private static final boolean DBG = Log.isLoggable(TAG, 3);

    /* JADX INFO: Access modifiers changed from: private */
    @FunctionalInterface
    /* loaded from: classes.dex */
    public interface NetworkManagementEventCallback {
        void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) throws RemoteException;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Dependencies {
        Dependencies() {
        }

        public IBinder getService(String name) {
            return ServiceManager.getService(name);
        }

        public void registerLocalService(NetworkManagementInternal nmi) {
            LocalServices.addService(NetworkManagementInternal.class, nmi);
        }

        public INetd getNetd() {
            return NetdService.get();
        }

        public int getCallingUid() {
            return Binder.getCallingUid();
        }
    }

    private NetworkManagementService(Context context, Dependencies deps) {
        this.mObservers = new RemoteCallbackList<>();
        HashMap<ITetheringStatsProvider, String> newHashMap = Maps.newHashMap();
        this.mTetheringStatsProviders = newHashMap;
        this.mQuotaLock = new Object();
        this.mRulesLock = new Object();
        this.mActiveQuotas = Maps.newHashMap();
        this.mActiveAlerts = Maps.newHashMap();
        this.mUidRejectOnMetered = new SparseBooleanArray();
        this.mUidAllowOnMetered = new SparseBooleanArray();
        this.mUidCleartextPolicy = new SparseIntArray();
        this.mUidFirewallRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mUidFirewallRestrictedRules = new SparseIntArray();
        this.mUidFirewallLowPowerStandbyRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mContext = context;
        this.mDeps = deps;
        this.mDaemonHandler = new Handler(FgThread.get().getLooper());
        this.mNetdUnsolicitedEventListener = new NetdUnsolicitedEventListener();
        deps.registerLocalService(new LocalService());
        synchronized (newHashMap) {
            newHashMap.put(new NetdTetheringStatsProvider(), "netd");
        }
    }

    private NetworkManagementService() {
        this.mObservers = new RemoteCallbackList<>();
        this.mTetheringStatsProviders = Maps.newHashMap();
        this.mQuotaLock = new Object();
        this.mRulesLock = new Object();
        this.mActiveQuotas = Maps.newHashMap();
        this.mActiveAlerts = Maps.newHashMap();
        this.mUidRejectOnMetered = new SparseBooleanArray();
        this.mUidAllowOnMetered = new SparseBooleanArray();
        this.mUidCleartextPolicy = new SparseIntArray();
        this.mUidFirewallRules = new SparseIntArray();
        this.mUidFirewallStandbyRules = new SparseIntArray();
        this.mUidFirewallDozableRules = new SparseIntArray();
        this.mUidFirewallPowerSaveRules = new SparseIntArray();
        this.mUidFirewallRestrictedRules = new SparseIntArray();
        this.mUidFirewallLowPowerStandbyRules = new SparseIntArray();
        this.mFirewallChainStates = new SparseBooleanArray();
        this.mContext = null;
        this.mDaemonHandler = null;
        this.mDeps = null;
        this.mNetdUnsolicitedEventListener = null;
    }

    static NetworkManagementService create(Context context, Dependencies deps) throws InterruptedException {
        NetworkManagementService service = new NetworkManagementService(context, deps);
        boolean z = DBG;
        if (z) {
            Slog.d(TAG, "Creating NetworkManagementService");
        }
        if (z) {
            Slog.d(TAG, "Connecting native netd service");
        }
        service.connectNativeNetdService();
        ITranNetworkManagementService.Instance().create(context, deps.getNetd());
        if (z) {
            Slog.d(TAG, "Connected");
        }
        return service;
    }

    public static NetworkManagementService create(Context context) throws InterruptedException {
        return create(context, new Dependencies());
    }

    public void systemReady() {
        if (DBG) {
            long start = System.currentTimeMillis();
            prepareNativeDaemon();
            long delta = System.currentTimeMillis() - start;
            Slog.d(TAG, "Prepared in " + delta + "ms");
            return;
        }
        prepareNativeDaemon();
    }

    private IBatteryStats getBatteryStats() {
        synchronized (this) {
            IBatteryStats iBatteryStats = this.mBatteryStats;
            if (iBatteryStats != null) {
                return iBatteryStats;
            }
            IBatteryStats asInterface = IBatteryStats.Stub.asInterface(this.mDeps.getService("batterystats"));
            this.mBatteryStats = asInterface;
            return asInterface;
        }
    }

    public void registerObserver(INetworkManagementEventObserver observer) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        this.mObservers.register(observer);
    }

    public void unregisterObserver(INetworkManagementEventObserver observer) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        this.mObservers.unregister(observer);
    }

    private void invokeForAllObservers(NetworkManagementEventCallback eventCallback) {
        int length = this.mObservers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            try {
                eventCallback.sendCallback(this.mObservers.getBroadcastItem(i));
            } catch (RemoteException | RuntimeException e) {
            } catch (Throwable th) {
                this.mObservers.finishBroadcast();
                throw th;
            }
        }
        this.mObservers.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyInterfaceStatusChanged(final String iface, final boolean up) {
        invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda3
            @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceStatusChanged(iface, up);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyInterfaceLinkStateChanged(final String iface, final boolean up) {
        invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda2
            @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceLinkStateChanged(iface, up);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyInterfaceAdded(final String iface) {
        invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda1
            @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceAdded(iface);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyInterfaceRemoved(final String iface) {
        this.mActiveAlerts.remove(iface);
        this.mActiveQuotas.remove(iface);
        invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda6
            @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceRemoved(iface);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyLimitReached(final String limitName, final String iface) {
        invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda5
            @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.limitReached(limitName, iface);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyInterfaceClassActivity(final int type, final boolean isActive, final long tsNanos, final int uid) {
        invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda7
            @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceClassDataActivityChanged(type, isActive, tsNanos, uid);
            }
        });
    }

    public void registerTetheringStatsProvider(ITetheringStatsProvider provider, String name) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        Objects.requireNonNull(provider);
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.put(provider, name);
        }
    }

    public void unregisterTetheringStatsProvider(ITetheringStatsProvider provider) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        synchronized (this.mTetheringStatsProviders) {
            this.mTetheringStatsProviders.remove(provider);
        }
    }

    public void tetherLimitReached(ITetheringStatsProvider provider) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        synchronized (this.mTetheringStatsProviders) {
            if (this.mTetheringStatsProviders.containsKey(provider)) {
                this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        NetworkManagementService.this.m251xffab7ad8();
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$tetherLimitReached$6$com-android-server-NetworkManagementService  reason: not valid java name */
    public /* synthetic */ void m251xffab7ad8() {
        notifyLimitReached("globalAlert", null);
    }

    private void syncFirewallChainLocked(int chain, String name) {
        SparseIntArray rules;
        synchronized (this.mRulesLock) {
            SparseIntArray uidFirewallRules = getUidFirewallRulesLR(chain);
            rules = uidFirewallRules.clone();
            uidFirewallRules.clear();
        }
        if (rules.size() > 0) {
            if (DBG) {
                Slog.d(TAG, "Pushing " + rules.size() + " active firewall " + name + "UID rules");
            }
            for (int i = 0; i < rules.size(); i++) {
                setFirewallUidRuleLocked(chain, rules.keyAt(i), rules.valueAt(i));
            }
        }
    }

    private void connectNativeNetdService() {
        INetd netd = this.mDeps.getNetd();
        this.mNetdService = netd;
        try {
            netd.registerUnsolicitedEventListener(this.mNetdUnsolicitedEventListener);
            if (DBG) {
                Slog.d(TAG, "Register unsolicited event listener");
            }
        } catch (RemoteException | ServiceSpecificException e) {
            Slog.e(TAG, "Failed to set Netd unsolicited event listener " + e);
        }
    }

    private void prepareNativeDaemon() {
        synchronized (this.mQuotaLock) {
            this.mStrictEnabled = true;
            setDataSaverModeEnabled(this.mDataSaverMode);
            int size = this.mActiveQuotas.size();
            if (size > 0) {
                if (DBG) {
                    Slog.d(TAG, "Pushing " + size + " active quota rules");
                }
                HashMap<String, Long> activeQuotas = this.mActiveQuotas;
                this.mActiveQuotas = Maps.newHashMap();
                for (Map.Entry<String, Long> entry : activeQuotas.entrySet()) {
                    setInterfaceQuota(entry.getKey(), entry.getValue().longValue());
                }
            }
            int size2 = this.mActiveAlerts.size();
            if (size2 > 0) {
                if (DBG) {
                    Slog.d(TAG, "Pushing " + size2 + " active alert rules");
                }
                HashMap<String, Long> activeAlerts = this.mActiveAlerts;
                this.mActiveAlerts = Maps.newHashMap();
                for (Map.Entry<String, Long> entry2 : activeAlerts.entrySet()) {
                    setInterfaceAlert(entry2.getKey(), entry2.getValue().longValue());
                }
            }
            SparseBooleanArray uidRejectOnQuota = null;
            SparseBooleanArray uidAcceptOnQuota = null;
            synchronized (this.mRulesLock) {
                int size3 = this.mUidRejectOnMetered.size();
                if (size3 > 0) {
                    if (DBG) {
                        Slog.d(TAG, "Pushing " + size3 + " UIDs to metered denylist rules");
                    }
                    uidRejectOnQuota = this.mUidRejectOnMetered;
                    this.mUidRejectOnMetered = new SparseBooleanArray();
                }
                int size4 = this.mUidAllowOnMetered.size();
                if (size4 > 0) {
                    if (DBG) {
                        Slog.d(TAG, "Pushing " + size4 + " UIDs to metered allowlist rules");
                    }
                    uidAcceptOnQuota = this.mUidAllowOnMetered;
                    this.mUidAllowOnMetered = new SparseBooleanArray();
                }
            }
            if (uidRejectOnQuota != null) {
                for (int i = 0; i < uidRejectOnQuota.size(); i++) {
                    setUidOnMeteredNetworkDenylist(uidRejectOnQuota.keyAt(i), uidRejectOnQuota.valueAt(i));
                }
            }
            if (uidAcceptOnQuota != null) {
                for (int i2 = 0; i2 < uidAcceptOnQuota.size(); i2++) {
                    setUidOnMeteredNetworkAllowlist(uidAcceptOnQuota.keyAt(i2), uidAcceptOnQuota.valueAt(i2));
                }
            }
            int size5 = this.mUidCleartextPolicy.size();
            if (size5 > 0) {
                if (DBG) {
                    Slog.d(TAG, "Pushing " + size5 + " active UID cleartext policies");
                }
                SparseIntArray local = this.mUidCleartextPolicy;
                this.mUidCleartextPolicy = new SparseIntArray();
                for (int i3 = 0; i3 < local.size(); i3++) {
                    setUidCleartextNetworkPolicy(local.keyAt(i3), local.valueAt(i3));
                }
            }
            setFirewallEnabled(this.mFirewallEnabled);
            syncFirewallChainLocked(0, "");
            syncFirewallChainLocked(2, "standby ");
            syncFirewallChainLocked(1, "dozable ");
            syncFirewallChainLocked(3, "powersave ");
            syncFirewallChainLocked(4, "restricted ");
            syncFirewallChainLocked(5, "low power standby ");
            int[] chains = {2, 1, 3, 4, 5};
            for (int chain : chains) {
                if (getFirewallChainState(chain)) {
                    setFirewallChainEnabled(chain, true);
                }
            }
        }
        try {
            getBatteryStats().noteNetworkStatsEnabled();
        } catch (RemoteException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAddressUpdated(final String iface, final LinkAddress address) {
        invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda4
            @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.addressUpdated(iface, address);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAddressRemoved(final String iface, final LinkAddress address) {
        invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda10
            @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.addressRemoved(iface, address);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyInterfaceDnsServerInfo(final String iface, final long lifetime, final String[] addresses) {
        invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda11
            @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
            public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                iNetworkManagementEventObserver.interfaceDnsServerInfo(iface, lifetime, addresses);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyRouteChange(boolean updated, final RouteInfo route) {
        if (updated) {
            invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda8
                @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
                public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                    iNetworkManagementEventObserver.routeUpdated(route);
                }
            });
        } else {
            invokeForAllObservers(new NetworkManagementEventCallback() { // from class: com.android.server.NetworkManagementService$$ExternalSyntheticLambda9
                @Override // com.android.server.NetworkManagementService.NetworkManagementEventCallback
                public final void sendCallback(INetworkManagementEventObserver iNetworkManagementEventObserver) {
                    iNetworkManagementEventObserver.routeRemoved(route);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class NetdUnsolicitedEventListener extends INetdUnsolicitedEventListener.Stub {
        private NetdUnsolicitedEventListener() {
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceClassActivityChanged(final boolean isActive, final int label, long timestamp, final int uid) throws RemoteException {
            long timestampNanos;
            if (timestamp <= 0) {
                timestampNanos = SystemClock.elapsedRealtimeNanos();
            } else {
                timestampNanos = timestamp;
            }
            final long j = timestampNanos;
            NetworkManagementService.this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$NetdUnsolicitedEventListener$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    NetworkManagementService.NetdUnsolicitedEventListener.this.m256xca464388(label, isActive, j, uid);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInterfaceClassActivityChanged$0$com-android-server-NetworkManagementService$NetdUnsolicitedEventListener  reason: not valid java name */
        public /* synthetic */ void m256xca464388(int label, boolean isActive, long timestampNanos, int uid) {
            NetworkManagementService.this.notifyInterfaceClassActivity(label, isActive, timestampNanos, uid);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onQuotaLimitReached$1$com-android-server-NetworkManagementService$NetdUnsolicitedEventListener  reason: not valid java name */
        public /* synthetic */ void m260xd59ad23e(String alertName, String ifName) {
            NetworkManagementService.this.notifyLimitReached(alertName, ifName);
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onQuotaLimitReached(final String alertName, final String ifName) throws RemoteException {
            NetworkManagementService.this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$NetdUnsolicitedEventListener$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    NetworkManagementService.NetdUnsolicitedEventListener.this.m260xd59ad23e(alertName, ifName);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInterfaceDnsServerInfo$2$com-android-server-NetworkManagementService$NetdUnsolicitedEventListener  reason: not valid java name */
        public /* synthetic */ void m257xaa3c2173(String ifName, long lifetime, String[] servers) {
            NetworkManagementService.this.notifyInterfaceDnsServerInfo(ifName, lifetime, servers);
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceDnsServerInfo(final String ifName, final long lifetime, final String[] servers) throws RemoteException {
            NetworkManagementService.this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$NetdUnsolicitedEventListener$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    NetworkManagementService.NetdUnsolicitedEventListener.this.m257xaa3c2173(ifName, lifetime, servers);
                }
            });
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceAddressUpdated(String addr, final String ifName, int flags, int scope) throws RemoteException {
            final LinkAddress address = new LinkAddress(addr, flags, scope);
            NetworkManagementService.this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$NetdUnsolicitedEventListener$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    NetworkManagementService.NetdUnsolicitedEventListener.this.m254x96d71ecb(ifName, address);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInterfaceAddressUpdated$3$com-android-server-NetworkManagementService$NetdUnsolicitedEventListener  reason: not valid java name */
        public /* synthetic */ void m254x96d71ecb(String ifName, LinkAddress address) {
            NetworkManagementService.this.notifyAddressUpdated(ifName, address);
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceAddressRemoved(String addr, final String ifName, int flags, int scope) throws RemoteException {
            final LinkAddress address = new LinkAddress(addr, flags, scope);
            NetworkManagementService.this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$NetdUnsolicitedEventListener$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    NetworkManagementService.NetdUnsolicitedEventListener.this.m253xe3cabf25(ifName, address);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInterfaceAddressRemoved$4$com-android-server-NetworkManagementService$NetdUnsolicitedEventListener  reason: not valid java name */
        public /* synthetic */ void m253xe3cabf25(String ifName, LinkAddress address) {
            NetworkManagementService.this.notifyAddressRemoved(ifName, address);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInterfaceAdded$5$com-android-server-NetworkManagementService$NetdUnsolicitedEventListener  reason: not valid java name */
        public /* synthetic */ void m252x36f0ddaa(String ifName) {
            NetworkManagementService.this.notifyInterfaceAdded(ifName);
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceAdded(final String ifName) throws RemoteException {
            NetworkManagementService.this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$NetdUnsolicitedEventListener$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    NetworkManagementService.NetdUnsolicitedEventListener.this.m252x36f0ddaa(ifName);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInterfaceRemoved$6$com-android-server-NetworkManagementService$NetdUnsolicitedEventListener  reason: not valid java name */
        public /* synthetic */ void m259x755c0029(String ifName) {
            NetworkManagementService.this.notifyInterfaceRemoved(ifName);
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceRemoved(final String ifName) throws RemoteException {
            NetworkManagementService.this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$NetdUnsolicitedEventListener$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    NetworkManagementService.NetdUnsolicitedEventListener.this.m259x755c0029(ifName);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInterfaceChanged$7$com-android-server-NetworkManagementService$NetdUnsolicitedEventListener  reason: not valid java name */
        public /* synthetic */ void m255x8f2e5274(String ifName, boolean up) {
            NetworkManagementService.this.notifyInterfaceStatusChanged(ifName, up);
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceChanged(final String ifName, final boolean up) throws RemoteException {
            NetworkManagementService.this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$NetdUnsolicitedEventListener$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    NetworkManagementService.NetdUnsolicitedEventListener.this.m255x8f2e5274(ifName, up);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onInterfaceLinkStateChanged$8$com-android-server-NetworkManagementService$NetdUnsolicitedEventListener  reason: not valid java name */
        public /* synthetic */ void m258xa990ea10(String ifName, boolean up) {
            NetworkManagementService.this.notifyInterfaceLinkStateChanged(ifName, up);
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onInterfaceLinkStateChanged(final String ifName, final boolean up) throws RemoteException {
            NetworkManagementService.this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$NetdUnsolicitedEventListener$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    NetworkManagementService.NetdUnsolicitedEventListener.this.m258xa990ea10(ifName, up);
                }
            });
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onRouteChanged(final boolean updated, String route, String gateway, String ifName) throws RemoteException {
            final RouteInfo processRoute = new RouteInfo(new IpPrefix(route), "".equals(gateway) ? null : InetAddresses.parseNumericAddress(gateway), ifName, 1);
            NetworkManagementService.this.mDaemonHandler.post(new Runnable() { // from class: com.android.server.NetworkManagementService$NetdUnsolicitedEventListener$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    NetworkManagementService.NetdUnsolicitedEventListener.this.m261xea23c7c2(updated, processRoute);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onRouteChanged$9$com-android-server-NetworkManagementService$NetdUnsolicitedEventListener  reason: not valid java name */
        public /* synthetic */ void m261xea23c7c2(boolean updated, RouteInfo processRoute) {
            NetworkManagementService.this.notifyRouteChange(updated, processRoute);
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public void onStrictCleartextDetected(int uid, String hex) throws RemoteException {
            ActivityManager.getService().notifyCleartextNetwork(uid, HexDump.hexStringToByteArray(hex));
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public int getInterfaceVersion() {
            return 10;
        }

        @Override // android.net.INetdUnsolicitedEventListener
        public String getInterfaceHash() {
            return "3943383e838f39851675e3640fcdf27b42f8c9fc";
        }
    }

    public String[] listInterfaces() {
        NetworkStack.checkNetworkStackPermissionOr(this.mContext, new String[]{"android.permission.CONNECTIVITY_INTERNAL"});
        try {
            return this.mNetdService.interfaceGetList();
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    private static InterfaceConfigurationParcel toStableParcel(InterfaceConfiguration cfg, String iface) {
        InterfaceConfigurationParcel cfgParcel = new InterfaceConfigurationParcel();
        cfgParcel.ifName = iface;
        String hwAddr = cfg.getHardwareAddress();
        if (!TextUtils.isEmpty(hwAddr)) {
            cfgParcel.hwAddr = hwAddr;
        } else {
            cfgParcel.hwAddr = "";
        }
        cfgParcel.ipv4Addr = cfg.getLinkAddress().getAddress().getHostAddress();
        cfgParcel.prefixLength = cfg.getLinkAddress().getPrefixLength();
        ArrayList<String> flags = new ArrayList<>();
        for (String flag : cfg.getFlags()) {
            flags.add(flag);
        }
        cfgParcel.flags = (String[]) flags.toArray(new String[0]);
        return cfgParcel;
    }

    public static InterfaceConfiguration fromStableParcel(InterfaceConfigurationParcel p) {
        String[] strArr;
        InterfaceConfiguration cfg = new InterfaceConfiguration();
        cfg.setHardwareAddress(p.hwAddr);
        InetAddress addr = InetAddresses.parseNumericAddress(p.ipv4Addr);
        cfg.setLinkAddress(new LinkAddress(addr, p.prefixLength));
        for (String flag : p.flags) {
            cfg.setFlag(flag);
        }
        return cfg;
    }

    public InterfaceConfiguration getInterfaceConfig(String iface) {
        NetworkStack.checkNetworkStackPermissionOr(this.mContext, new String[]{"android.permission.CONNECTIVITY_INTERNAL"});
        try {
            InterfaceConfigurationParcel result = this.mNetdService.interfaceGetCfg(iface);
            try {
                InterfaceConfiguration cfg = fromStableParcel(result);
                return cfg;
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException("Invalid InterfaceConfigurationParcel", iae);
            }
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setInterfaceConfig(String iface, InterfaceConfiguration cfg) {
        NetworkStack.checkNetworkStackPermissionOr(this.mContext, new String[]{"android.permission.CONNECTIVITY_INTERNAL"});
        LinkAddress linkAddr = cfg.getLinkAddress();
        if (linkAddr == null || linkAddr.getAddress() == null) {
            throw new IllegalStateException("Null LinkAddress given");
        }
        InterfaceConfigurationParcel cfgParcel = toStableParcel(cfg, iface);
        try {
            this.mNetdService.interfaceSetCfg(cfgParcel);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setInterfaceDown(String iface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        InterfaceConfiguration ifcg = getInterfaceConfig(iface);
        ifcg.setInterfaceDown();
        setInterfaceConfig(iface, ifcg);
    }

    public void setInterfaceUp(String iface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        InterfaceConfiguration ifcg = getInterfaceConfig(iface);
        ifcg.setInterfaceUp();
        setInterfaceConfig(iface, ifcg);
    }

    public void setInterfaceIpv6PrivacyExtensions(String iface, boolean enable) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.interfaceSetIPv6PrivacyExtensions(iface, enable);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void clearInterfaceAddresses(String iface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.interfaceClearAddrs(iface);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void enableIpv6(String iface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.interfaceSetEnableIPv6(iface, true);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setIPv6AddrGenMode(String iface, int mode) throws ServiceSpecificException {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.setIPv6AddrGenMode(iface, mode);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public void disableIpv6(String iface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.interfaceSetEnableIPv6(iface, false);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void addRoute(int netId, RouteInfo route) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        NetdUtils.modifyRoute(this.mNetdService, NetdUtils.ModifyOperation.ADD, netId, route);
    }

    public void removeRoute(int netId, RouteInfo route) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        NetdUtils.modifyRoute(this.mNetdService, NetdUtils.ModifyOperation.REMOVE, netId, route);
    }

    private ArrayList<String> readRouteList(String filename) {
        FileInputStream fstream = null;
        ArrayList<String> list = new ArrayList<>();
        try {
            try {
                fstream = new FileInputStream(filename);
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                while (true) {
                    String s = br.readLine();
                    if (s == null || s.length() == 0) {
                        break;
                    }
                    list.add(s);
                }
                fstream.close();
            } catch (IOException e) {
                if (fstream != null) {
                    fstream.close();
                }
            } catch (Throwable th) {
                if (fstream != null) {
                    try {
                        fstream.close();
                    } catch (IOException e2) {
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
        }
        return list;
    }

    public void shutdown() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SHUTDOWN", TAG);
        Slog.i(TAG, "Shutting down");
    }

    public boolean getIpForwardingEnabled() throws IllegalStateException {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            boolean isEnabled = this.mNetdService.ipfwdEnabled();
            return isEnabled;
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setIpForwardingEnabled(boolean enable) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            if (enable) {
                this.mNetdService.ipfwdEnableForwarding("tethering");
            } else {
                this.mNetdService.ipfwdDisableForwarding("tethering");
            }
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void startTethering(String[] dhcpRange) {
        startTetheringWithConfiguration(true, dhcpRange);
    }

    public void startTetheringWithConfiguration(boolean usingLegacyDnsProxy, String[] dhcpRange) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            NetdUtils.tetherStart(this.mNetdService, usingLegacyDnsProxy, dhcpRange);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void stopTethering() {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.tetherStop();
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isTetheringStarted() {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            boolean isEnabled = this.mNetdService.tetherIsEnabled();
            return isEnabled;
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void tetherInterface(String iface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            LinkAddress addr = getInterfaceConfig(iface).getLinkAddress();
            IpPrefix dest = new IpPrefix(addr.getAddress(), addr.getPrefixLength());
            NetdUtils.tetherInterface(this.mNetdService, iface, dest);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void untetherInterface(String iface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            NetdUtils.untetherInterface(this.mNetdService, iface);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public String[] listTetheredInterfaces() {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            return this.mNetdService.tetherInterfaceList();
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public String[] getDnsForwarders() {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            return this.mNetdService.tetherDnsList();
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<InterfaceAddress> excludeLinkLocal(List<InterfaceAddress> addresses) {
        ArrayList<InterfaceAddress> filtered = new ArrayList<>(addresses.size());
        for (InterfaceAddress ia : addresses) {
            if (!ia.getAddress().isLinkLocalAddress()) {
                filtered.add(ia);
            }
        }
        return filtered;
    }

    private void modifyInterfaceForward(boolean add, String fromIface, String toIface) {
        try {
            if (add) {
                this.mNetdService.ipfwdAddInterfaceForward(fromIface, toIface);
            } else {
                this.mNetdService.ipfwdRemoveInterfaceForward(fromIface, toIface);
            }
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void startInterfaceForwarding(String fromIface, String toIface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        modifyInterfaceForward(true, fromIface, toIface);
    }

    public void stopInterfaceForwarding(String fromIface, String toIface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        modifyInterfaceForward(false, fromIface, toIface);
    }

    public void enableNat(String internalInterface, String externalInterface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.tetherAddForward(internalInterface, externalInterface);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void disableNat(String internalInterface, String externalInterface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.tetherRemoveForward(internalInterface, externalInterface);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setInterfaceQuota(String iface, long quotaBytes) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        synchronized (this.mQuotaLock) {
            if (this.mActiveQuotas.containsKey(iface)) {
                throw new IllegalStateException("iface " + iface + " already has quota");
            }
            try {
                this.mNetdService.bandwidthSetInterfaceQuota(iface, quotaBytes);
                this.mActiveQuotas.put(iface, Long.valueOf(quotaBytes));
                synchronized (this.mTetheringStatsProviders) {
                    for (ITetheringStatsProvider provider : this.mTetheringStatsProviders.keySet()) {
                        try {
                            provider.setInterfaceQuota(iface, quotaBytes);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Problem setting tethering data limit on provider " + this.mTetheringStatsProviders.get(provider) + ": " + e);
                        }
                    }
                }
            } catch (RemoteException | ServiceSpecificException e2) {
                throw new IllegalStateException(e2);
            }
        }
    }

    public void removeInterfaceQuota(String iface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        synchronized (this.mQuotaLock) {
            if (this.mActiveQuotas.containsKey(iface)) {
                this.mActiveQuotas.remove(iface);
                this.mActiveAlerts.remove(iface);
                try {
                    this.mNetdService.bandwidthRemoveInterfaceQuota(iface);
                    synchronized (this.mTetheringStatsProviders) {
                        for (ITetheringStatsProvider provider : this.mTetheringStatsProviders.keySet()) {
                            try {
                                provider.setInterfaceQuota(iface, -1L);
                            } catch (RemoteException e) {
                                Log.e(TAG, "Problem removing tethering data limit on provider " + this.mTetheringStatsProviders.get(provider) + ": " + e);
                            }
                        }
                    }
                } catch (RemoteException | ServiceSpecificException e2) {
                    throw new IllegalStateException(e2);
                }
            }
        }
    }

    public void setInterfaceAlert(String iface, long alertBytes) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        if (!this.mActiveQuotas.containsKey(iface)) {
            throw new IllegalStateException("setting alert requires existing quota on iface");
        }
        synchronized (this.mQuotaLock) {
            if (this.mActiveAlerts.containsKey(iface)) {
                throw new IllegalStateException("iface " + iface + " already has alert");
            }
            try {
                this.mNetdService.bandwidthSetInterfaceAlert(iface, alertBytes);
                this.mActiveAlerts.put(iface, Long.valueOf(alertBytes));
            } catch (RemoteException | ServiceSpecificException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void removeInterfaceAlert(String iface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        synchronized (this.mQuotaLock) {
            if (this.mActiveAlerts.containsKey(iface)) {
                try {
                    this.mNetdService.bandwidthRemoveInterfaceAlert(iface);
                    this.mActiveAlerts.remove(iface);
                } catch (RemoteException | ServiceSpecificException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    public void setGlobalAlert(long alertBytes) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.bandwidthSetGlobalAlert(alertBytes);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setUidOnMeteredNetworkList(int uid, boolean allowlist, boolean enable) {
        SparseBooleanArray quotaList;
        boolean oldEnable;
        NetworkStack.checkNetworkStackPermission(this.mContext);
        synchronized (this.mQuotaLock) {
            synchronized (this.mRulesLock) {
                quotaList = allowlist ? this.mUidAllowOnMetered : this.mUidRejectOnMetered;
                oldEnable = quotaList.get(uid, false);
            }
            if (oldEnable == enable) {
                return;
            }
            Trace.traceBegin(2097152L, "inetd bandwidth");
            ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
            try {
                if (allowlist) {
                    if (enable) {
                        cm.addUidToMeteredNetworkAllowList(uid);
                    } else {
                        cm.removeUidFromMeteredNetworkAllowList(uid);
                    }
                } else if (enable) {
                    cm.addUidToMeteredNetworkDenyList(uid);
                } else {
                    cm.removeUidFromMeteredNetworkDenyList(uid);
                }
                synchronized (this.mRulesLock) {
                    if (enable) {
                        quotaList.put(uid, true);
                    } else {
                        quotaList.delete(uid);
                    }
                }
                Trace.traceEnd(2097152L);
            } catch (RuntimeException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void setUidOnMeteredNetworkDenylist(int uid, boolean enable) {
        setUidOnMeteredNetworkList(uid, false, enable);
    }

    public void setUidOnMeteredNetworkAllowlist(int uid, boolean enable) {
        setUidOnMeteredNetworkList(uid, true, enable);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [1237=4] */
    public boolean setDataSaverModeEnabled(boolean enable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", TAG);
        if (DBG) {
            Log.d(TAG, "setDataSaverMode: " + enable);
        }
        synchronized (this.mQuotaLock) {
            if (this.mDataSaverMode == enable) {
                Log.w(TAG, "setDataSaverMode(): already " + this.mDataSaverMode);
                return true;
            }
            Trace.traceBegin(2097152L, "bandwidthEnableDataSaver");
            try {
                boolean changed = this.mNetdService.bandwidthEnableDataSaver(enable);
                if (changed) {
                    this.mDataSaverMode = enable;
                } else {
                    Log.w(TAG, "setDataSaverMode(" + enable + "): netd command silently failed");
                }
                Trace.traceEnd(2097152L);
                return changed;
            } catch (RemoteException e) {
                Log.w(TAG, "setDataSaverMode(" + enable + "): netd command failed", e);
                Trace.traceEnd(2097152L);
                return false;
            }
        }
    }

    private void applyUidCleartextNetworkPolicy(int uid, int policy) {
        int policyValue;
        switch (policy) {
            case 0:
                policyValue = 1;
                break;
            case 1:
                policyValue = 2;
                break;
            case 2:
                policyValue = 3;
                break;
            default:
                throw new IllegalArgumentException("Unknown policy " + policy);
        }
        try {
            this.mNetdService.strictUidCleartextPenalty(uid, policyValue);
            this.mUidCleartextPolicy.put(uid, policy);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setUidCleartextNetworkPolicy(int uid, int policy) {
        if (this.mDeps.getCallingUid() != uid) {
            NetworkStack.checkNetworkStackPermission(this.mContext);
        }
        synchronized (this.mQuotaLock) {
            int oldPolicy = this.mUidCleartextPolicy.get(uid, 0);
            if (oldPolicy == policy) {
                return;
            }
            if (!this.mStrictEnabled) {
                this.mUidCleartextPolicy.put(uid, policy);
                return;
            }
            if (oldPolicy != 0 && policy != 0) {
                applyUidCleartextNetworkPolicy(uid, 0);
            }
            applyUidCleartextNetworkPolicy(uid, policy);
        }
    }

    public boolean isBandwidthControlEnabled() {
        return true;
    }

    /* loaded from: classes.dex */
    private class NetdTetheringStatsProvider extends ITetheringStatsProvider.Stub {
        private NetdTetheringStatsProvider() {
        }

        public NetworkStats getTetherStats(int how) {
            throw new UnsupportedOperationException();
        }

        public void setInterfaceQuota(String iface, long quotaBytes) {
        }
    }

    public NetworkStats getNetworkStatsTethering(int how) {
        throw new UnsupportedOperationException();
    }

    public void setFirewallEnabled(boolean enabled) {
        enforceSystemUid();
        try {
            this.mNetdService.firewallSetFirewallType(enabled ? 0 : 1);
            this.mFirewallEnabled = enabled;
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isFirewallEnabled() {
        enforceSystemUid();
        return this.mFirewallEnabled;
    }

    public void setFirewallInterfaceRule(String iface, boolean allow) {
        enforceSystemUid();
        Preconditions.checkState(this.mFirewallEnabled);
        try {
            this.mNetdService.firewallSetInterfaceRule(iface, allow ? 1 : 2);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    private void closeSocketsForFirewallChainLocked(int chain, String chainName) {
        UidRangeParcel[] ranges;
        UidRangeParcel[] ranges2;
        int[] exemptUids;
        int numUids = 0;
        if (DBG) {
            Slog.d(TAG, "Closing sockets after enabling chain " + chainName);
        }
        if (getFirewallType(chain) == 0) {
            ranges2 = new UidRangeParcel[]{new UidRangeParcel(10000, Integer.MAX_VALUE)};
            synchronized (this.mRulesLock) {
                SparseIntArray rules = getUidFirewallRulesLR(chain);
                exemptUids = new int[rules.size()];
                for (int i = 0; i < exemptUids.length; i++) {
                    if (rules.valueAt(i) == 1) {
                        exemptUids[numUids] = rules.keyAt(i);
                        numUids++;
                    }
                }
            }
            if (numUids != exemptUids.length) {
                exemptUids = Arrays.copyOf(exemptUids, numUids);
            }
        } else {
            synchronized (this.mRulesLock) {
                SparseIntArray rules2 = getUidFirewallRulesLR(chain);
                ranges = new UidRangeParcel[rules2.size()];
                for (int i2 = 0; i2 < ranges.length; i2++) {
                    if (rules2.valueAt(i2) == 2) {
                        int uid = rules2.keyAt(i2);
                        ranges[numUids] = new UidRangeParcel(uid, uid);
                        numUids++;
                    }
                }
            }
            if (numUids == ranges.length) {
                ranges2 = ranges;
            } else {
                ranges2 = (UidRangeParcel[]) Arrays.copyOf(ranges, numUids);
            }
            exemptUids = new int[0];
        }
        try {
            this.mNetdService.socketDestroy(ranges2, exemptUids);
        } catch (RemoteException | ServiceSpecificException e) {
            Slog.e(TAG, "Error closing sockets after enabling chain " + chainName + ": " + e);
        }
    }

    public void setFirewallChainEnabled(int chain, boolean enable) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            synchronized (this.mRulesLock) {
                if (getFirewallChainState(chain) == enable) {
                    return;
                }
                setFirewallChainState(chain, enable);
                String chainName = getFirewallChainName(chain);
                if (chain == 0) {
                    throw new IllegalArgumentException("Bad child chain: " + chainName);
                }
                ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
                try {
                    cm.setFirewallChainEnabled(chain, enable);
                    if (enable) {
                        closeSocketsForFirewallChainLocked(chain, chainName);
                    }
                } catch (RuntimeException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private String getFirewallChainName(int chain) {
        switch (chain) {
            case 1:
                return "dozable";
            case 2:
                return "standby";
            case 3:
                return "powersave";
            case 4:
                return "restricted";
            case 5:
                return "low_power_standby";
            default:
                throw new IllegalArgumentException("Bad child chain: " + chain);
        }
    }

    private int getFirewallType(int chain) {
        switch (chain) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 0;
            case 4:
                return 0;
            case 5:
                return 0;
            default:
                return 1 ^ isFirewallEnabled();
        }
    }

    public void setFirewallUidRules(int chain, int[] uids, int[] rules) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            synchronized (this.mRulesLock) {
                SparseIntArray uidFirewallRules = getUidFirewallRulesLR(chain);
                SparseIntArray newRules = new SparseIntArray();
                for (int index = uids.length - 1; index >= 0; index--) {
                    int uid = uids[index];
                    int rule = rules[index];
                    updateFirewallUidRuleLocked(chain, uid, rule);
                    newRules.put(uid, rule);
                }
                SparseIntArray rulesToRemove = new SparseIntArray();
                for (int index2 = uidFirewallRules.size() - 1; index2 >= 0; index2--) {
                    int uid2 = uidFirewallRules.keyAt(index2);
                    if (newRules.indexOfKey(uid2) < 0) {
                        rulesToRemove.put(uid2, 0);
                    }
                }
                int index3 = rulesToRemove.size();
                for (int index4 = index3 - 1; index4 >= 0; index4--) {
                    updateFirewallUidRuleLocked(chain, rulesToRemove.keyAt(index4), 0);
                }
            }
            ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
            try {
                cm.replaceFirewallChain(chain, uids);
            } catch (RuntimeException e) {
                Slog.w(TAG, "Error flushing firewall chain " + chain, e);
            }
        }
    }

    public void setFirewallUidRule(int chain, int uid, int rule) {
        enforceSystemUid();
        synchronized (this.mQuotaLock) {
            setFirewallUidRuleLocked(chain, uid, rule);
        }
    }

    private void setFirewallUidRuleLocked(int chain, int uid, int rule) {
        if (updateFirewallUidRuleLocked(chain, uid, rule)) {
            ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
            try {
                cm.setUidFirewallRule(chain, uid, rule);
            } catch (RuntimeException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private boolean updateFirewallUidRuleLocked(int chain, int uid, int rule) {
        synchronized (this.mRulesLock) {
            SparseIntArray uidFirewallRules = getUidFirewallRulesLR(chain);
            int oldUidFirewallRule = uidFirewallRules.get(uid, 0);
            boolean z = DBG;
            if (z) {
                Slog.d(TAG, "oldRule = " + oldUidFirewallRule + ", newRule=" + rule + " for uid=" + uid + " on chain " + chain);
            }
            if (oldUidFirewallRule == rule) {
                if (z) {
                    Slog.d(TAG, "!!!!! Skipping change");
                }
                return false;
            }
            String ruleName = getFirewallRuleName(chain, rule);
            String oldRuleName = getFirewallRuleName(chain, oldUidFirewallRule);
            if (rule == 0) {
                uidFirewallRules.delete(uid);
            } else {
                uidFirewallRules.put(uid, rule);
            }
            return ruleName.equals(oldRuleName) ? false : true;
        }
    }

    private String getFirewallRuleName(int chain, int rule) {
        if (getFirewallType(chain) == 0) {
            if (rule == 1) {
                return "allow";
            }
            return "deny";
        } else if (rule == 2) {
            return "deny";
        } else {
            return "allow";
        }
    }

    private SparseIntArray getUidFirewallRulesLR(int chain) {
        switch (chain) {
            case 0:
                return this.mUidFirewallRules;
            case 1:
                return this.mUidFirewallDozableRules;
            case 2:
                return this.mUidFirewallStandbyRules;
            case 3:
                return this.mUidFirewallPowerSaveRules;
            case 4:
                return this.mUidFirewallRestrictedRules;
            case 5:
                return this.mUidFirewallLowPowerStandbyRules;
            default:
                throw new IllegalArgumentException("Unknown chain:" + chain);
        }
    }

    private void enforceSystemUid() {
        int uid = this.mDeps.getCallingUid();
        if (uid != 1000) {
            throw new SecurityException("Only available to AID_SYSTEM");
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            synchronized (this.mQuotaLock) {
                pw.print("Active quota ifaces: ");
                pw.println(this.mActiveQuotas.toString());
                pw.print("Active alert ifaces: ");
                pw.println(this.mActiveAlerts.toString());
                pw.print("Data saver mode: ");
                pw.println(this.mDataSaverMode);
                synchronized (this.mRulesLock) {
                    dumpUidRuleOnQuotaLocked(pw, "denied UIDs", this.mUidRejectOnMetered);
                    dumpUidRuleOnQuotaLocked(pw, "allowed UIDs", this.mUidAllowOnMetered);
                }
            }
            synchronized (this.mRulesLock) {
                dumpUidFirewallRule(pw, "", this.mUidFirewallRules);
                pw.print("UID firewall standby chain enabled: ");
                pw.println(getFirewallChainState(2));
                dumpUidFirewallRule(pw, "standby", this.mUidFirewallStandbyRules);
                pw.print("UID firewall dozable chain enabled: ");
                pw.println(getFirewallChainState(1));
                dumpUidFirewallRule(pw, "dozable", this.mUidFirewallDozableRules);
                pw.print("UID firewall powersave chain enabled: ");
                pw.println(getFirewallChainState(3));
                dumpUidFirewallRule(pw, "powersave", this.mUidFirewallPowerSaveRules);
                pw.print("UID firewall restricted mode chain enabled: ");
                pw.println(getFirewallChainState(4));
                dumpUidFirewallRule(pw, "restricted", this.mUidFirewallRestrictedRules);
                pw.print("UID firewall low power standby chain enabled: ");
                pw.println(getFirewallChainState(5));
                dumpUidFirewallRule(pw, "low_power_standby", this.mUidFirewallLowPowerStandbyRules);
            }
            pw.print("Firewall enabled: ");
            pw.println(this.mFirewallEnabled);
            pw.print("Netd service status: ");
            INetd iNetd = this.mNetdService;
            if (iNetd == null) {
                pw.println("disconnected");
                return;
            }
            try {
                boolean alive = iNetd.isAlive();
                pw.println(alive ? "alive" : "dead");
            } catch (RemoteException e) {
                pw.println(INetd.NEXTHOP_UNREACHABLE);
            }
        }
    }

    private void dumpUidRuleOnQuotaLocked(PrintWriter pw, String name, SparseBooleanArray list) {
        pw.print("UID bandwith control ");
        pw.print(name);
        pw.print(": [");
        int size = list.size();
        for (int i = 0; i < size; i++) {
            pw.print(list.keyAt(i));
            if (i < size - 1) {
                pw.print(",");
            }
        }
        pw.println("]");
    }

    private void dumpUidFirewallRule(PrintWriter pw, String name, SparseIntArray rules) {
        pw.print("UID firewall ");
        pw.print(name);
        pw.print(" rule: [");
        int size = rules.size();
        for (int i = 0; i < size; i++) {
            pw.print(rules.keyAt(i));
            pw.print(":");
            pw.print(rules.valueAt(i));
            if (i < size - 1) {
                pw.print(",");
            }
        }
        pw.println("]");
    }

    private void modifyInterfaceInNetwork(boolean add, int netId, String iface) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            if (add) {
                this.mNetdService.networkAddInterface(netId, iface);
            } else {
                this.mNetdService.networkRemoveInterface(netId, iface);
            }
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void allowProtect(int uid) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.networkSetProtectAllow(uid);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void denyProtect(int uid) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        try {
            this.mNetdService.networkSetProtectDeny(uid);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    public void addInterfaceToLocalNetwork(String iface, List<RouteInfo> routes) {
        modifyInterfaceInNetwork(true, 99, iface);
        NetdUtils.addRoutesToLocalNetwork(this.mNetdService, iface, routes);
    }

    public void removeInterfaceFromLocalNetwork(String iface) {
        modifyInterfaceInNetwork(false, 99, iface);
    }

    public int removeRoutesFromLocalNetwork(List<RouteInfo> routes) {
        NetworkStack.checkNetworkStackPermission(this.mContext);
        return NetdUtils.removeRoutesFromLocalNetwork(this.mNetdService, routes);
    }

    public boolean isNetworkRestricted(int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.OBSERVE_NETWORK_POLICY", TAG);
        return isNetworkRestrictedInternal(uid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isNetworkRestrictedInternal(int uid) {
        synchronized (this.mRulesLock) {
            if (getFirewallChainState(2) && this.mUidFirewallStandbyRules.get(uid) == 2) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + uid + " restricted because of app standby mode");
                }
                return true;
            } else if (getFirewallChainState(1) && this.mUidFirewallDozableRules.get(uid) != 1) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + uid + " restricted because of device idle mode");
                }
                return true;
            } else if (getFirewallChainState(3) && this.mUidFirewallPowerSaveRules.get(uid) != 1) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + uid + " restricted because of power saver mode");
                }
                return true;
            } else if (getFirewallChainState(4) && this.mUidFirewallRestrictedRules.get(uid) != 1) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + uid + " restricted because of restricted mode");
                }
                return true;
            } else if (getFirewallChainState(5) && this.mUidFirewallLowPowerStandbyRules.get(uid) != 1) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + uid + " restricted because of low power standby");
                }
                return true;
            } else if (this.mUidRejectOnMetered.get(uid)) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + uid + " restricted because of no metered data in the background");
                }
                return true;
            } else if (this.mDataSaverMode && !this.mUidAllowOnMetered.get(uid)) {
                if (DBG) {
                    Slog.d(TAG, "Uid " + uid + " restricted because of data saver mode");
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private void setFirewallChainState(int chain, boolean state) {
        synchronized (this.mRulesLock) {
            this.mFirewallChainStates.put(chain, state);
        }
    }

    private boolean getFirewallChainState(int chain) {
        boolean z;
        synchronized (this.mRulesLock) {
            z = this.mFirewallChainStates.get(chain);
        }
        return z;
    }

    /* loaded from: classes.dex */
    private class LocalService extends NetworkManagementInternal {
        private LocalService() {
        }

        @Override // com.android.server.NetworkManagementInternal
        public boolean isNetworkRestrictedForUid(int uid) {
            return NetworkManagementService.this.isNetworkRestrictedInternal(uid);
        }
    }

    public boolean bindAppUidToNetwork(int uid, Network network) {
        return ITranNetworkManagementService.Instance().bindAppUidToNetwork(this.mContext, this.mDeps.getNetd(), uid, network);
    }

    public void destorySocketByUid(int uid) {
        ITranNetworkManagementService.Instance().destorySocketByUid(uid, this.mDeps.getNetd());
    }

    public boolean setMultiLink(int uid, Network network, boolean isMultiLink) {
        return ITranNetworkManagementService.Instance().setMultiLink(uid, network, isMultiLink, this.mDeps.getNetd());
    }

    public boolean isAppNetWorkAccelerated() {
        return ITranNetworkManagementService.Instance().isAppNetWorkAccelerated();
    }

    public boolean switchAppNetWorkAccelerated(boolean isOpen, String pkgName) {
        return ITranNetworkManagementService.Instance().switchAppNetWorkAccelerated(isOpen, pkgName);
    }

    public boolean isAcceleratedEnabled(String pkgName) {
        return ITranNetworkManagementService.Instance().isAcceleratedEnabled(pkgName);
    }

    public boolean setAcceleratedEnabledByPhone(boolean enable) {
        return ITranNetworkManagementService.Instance().setAcceleratedEnabledByPhone(enable);
    }
}
