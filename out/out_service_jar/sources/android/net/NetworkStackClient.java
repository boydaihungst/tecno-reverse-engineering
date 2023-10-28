package android.net;

import android.net.ConnectivityModuleConnector;
import android.net.INetworkStackConnector;
import android.net.dhcp.DhcpServingParamsParcel;
import android.net.dhcp.IDhcpServerCallbacks;
import android.net.ip.IIpClientCallbacks;
import android.net.util.SharedLog;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public class NetworkStackClient {
    private static final int NETWORKSTACK_TIMEOUT_MS = 10000;
    private static final String TAG = NetworkStackClient.class.getSimpleName();
    private static NetworkStackClient sInstance;
    private INetworkStackConnector mConnector;
    private final Dependencies mDependencies;
    private final SharedLog mLog;
    private final ArrayList<NetworkStackCallback> mPendingNetStackRequests;
    private volatile boolean mWasSystemServerInitialized;

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public interface Dependencies {
        void addToServiceManager(IBinder iBinder);

        void checkCallerUid();

        ConnectivityModuleConnector getConnectivityModuleConnector();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface NetworkStackCallback {
        void onNetworkStackConnected(INetworkStackConnector iNetworkStackConnector);
    }

    protected NetworkStackClient(Dependencies dependencies) {
        this.mPendingNetStackRequests = new ArrayList<>();
        this.mLog = new SharedLog(TAG);
        this.mWasSystemServerInitialized = false;
        this.mDependencies = dependencies;
    }

    private NetworkStackClient() {
        this(new DependenciesImpl());
    }

    /* loaded from: classes.dex */
    private static class DependenciesImpl implements Dependencies {
        private DependenciesImpl() {
        }

        @Override // android.net.NetworkStackClient.Dependencies
        public void addToServiceManager(IBinder service) {
            ServiceManager.addService("network_stack", service, false, 6);
        }

        @Override // android.net.NetworkStackClient.Dependencies
        public void checkCallerUid() {
            int caller = Binder.getCallingUid();
            if (caller != 1000 && caller != 1073 && UserHandle.getAppId(caller) != 1002) {
                throw new SecurityException("Only the system server should try to bind to the network stack.");
            }
        }

        @Override // android.net.NetworkStackClient.Dependencies
        public ConnectivityModuleConnector getConnectivityModuleConnector() {
            return ConnectivityModuleConnector.getInstance();
        }
    }

    public static synchronized NetworkStackClient getInstance() {
        NetworkStackClient networkStackClient;
        synchronized (NetworkStackClient.class) {
            if (sInstance == null) {
                sInstance = new NetworkStackClient();
            }
            networkStackClient = sInstance;
        }
        return networkStackClient;
    }

    public void makeDhcpServer(final String ifName, final DhcpServingParamsParcel params, final IDhcpServerCallbacks cb) {
        requestConnector(new NetworkStackCallback() { // from class: android.net.NetworkStackClient$$ExternalSyntheticLambda2
            @Override // android.net.NetworkStackClient.NetworkStackCallback
            public final void onNetworkStackConnected(INetworkStackConnector iNetworkStackConnector) {
                NetworkStackClient.lambda$makeDhcpServer$0(ifName, params, cb, iNetworkStackConnector);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$makeDhcpServer$0(String ifName, DhcpServingParamsParcel params, IDhcpServerCallbacks cb, INetworkStackConnector connector) {
        try {
            connector.makeDhcpServer(ifName, params, cb);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void makeIpClient(final String ifName, final IIpClientCallbacks cb) {
        requestConnector(new NetworkStackCallback() { // from class: android.net.NetworkStackClient$$ExternalSyntheticLambda1
            @Override // android.net.NetworkStackClient.NetworkStackCallback
            public final void onNetworkStackConnected(INetworkStackConnector iNetworkStackConnector) {
                NetworkStackClient.lambda$makeIpClient$1(ifName, cb, iNetworkStackConnector);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$makeIpClient$1(String ifName, IIpClientCallbacks cb, INetworkStackConnector connector) {
        try {
            connector.makeIpClient(ifName, cb);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void makeNetworkMonitor(final Network network, final String name, final INetworkMonitorCallbacks cb) {
        requestConnector(new NetworkStackCallback() { // from class: android.net.NetworkStackClient$$ExternalSyntheticLambda0
            @Override // android.net.NetworkStackClient.NetworkStackCallback
            public final void onNetworkStackConnected(INetworkStackConnector iNetworkStackConnector) {
                NetworkStackClient.lambda$makeNetworkMonitor$2(network, name, cb, iNetworkStackConnector);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$makeNetworkMonitor$2(Network network, String name, INetworkMonitorCallbacks cb, INetworkStackConnector connector) {
        try {
            connector.makeNetworkMonitor(network, name, cb);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    public void fetchIpMemoryStore(final IIpMemoryStoreCallbacks cb) {
        requestConnector(new NetworkStackCallback() { // from class: android.net.NetworkStackClient$$ExternalSyntheticLambda3
            @Override // android.net.NetworkStackClient.NetworkStackCallback
            public final void onNetworkStackConnected(INetworkStackConnector iNetworkStackConnector) {
                NetworkStackClient.lambda$fetchIpMemoryStore$3(IIpMemoryStoreCallbacks.this, iNetworkStackConnector);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$fetchIpMemoryStore$3(IIpMemoryStoreCallbacks cb, INetworkStackConnector connector) {
        try {
            connector.fetchIpMemoryStore(cb);
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
    }

    /* loaded from: classes.dex */
    private class NetworkStackConnection implements ConnectivityModuleConnector.ModuleServiceCallback {
        private NetworkStackConnection() {
        }

        @Override // android.net.ConnectivityModuleConnector.ModuleServiceCallback
        public void onModuleServiceConnected(IBinder service) {
            NetworkStackClient.this.logi("Network stack service connected");
            NetworkStackClient.this.registerNetworkStackService(service);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerNetworkStackService(IBinder service) {
        ArrayList<NetworkStackCallback> requests;
        INetworkStackConnector connector = INetworkStackConnector.Stub.asInterface(service);
        this.mDependencies.addToServiceManager(service);
        log("Network stack service registered");
        synchronized (this.mPendingNetStackRequests) {
            requests = new ArrayList<>(this.mPendingNetStackRequests);
            this.mPendingNetStackRequests.clear();
            this.mConnector = connector;
        }
        Iterator<NetworkStackCallback> it = requests.iterator();
        while (it.hasNext()) {
            NetworkStackCallback r = it.next();
            r.onNetworkStackConnected(connector);
        }
    }

    public void init() {
        log("Network stack init");
        this.mWasSystemServerInitialized = true;
    }

    public void start() {
        this.mDependencies.getConnectivityModuleConnector().startModuleService(INetworkStackConnector.class.getName(), "android.permission.MAINLINE_NETWORK_STACK", new NetworkStackConnection());
        log("Network stack service start requested");
    }

    private void log(String message) {
        synchronized (this.mLog) {
            this.mLog.log(message);
        }
    }

    private void logWtf(String message, Throwable e) {
        Slog.wtf(TAG, message);
        synchronized (this.mLog) {
            this.mLog.e(message, e);
        }
    }

    private void loge(String message, Throwable e) {
        synchronized (this.mLog) {
            this.mLog.e(message, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logi(String message) {
        synchronized (this.mLog) {
            this.mLog.i(message);
        }
    }

    private INetworkStackConnector getRemoteConnector() {
        try {
            long before = System.currentTimeMillis();
            do {
                IBinder connector = ServiceManager.getService("network_stack");
                if (connector == null) {
                    Thread.sleep(20L);
                } else {
                    return INetworkStackConnector.Stub.asInterface(connector);
                }
            } while (System.currentTimeMillis() - before <= JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            loge("Timeout waiting for NetworkStack connector", null);
            return null;
        } catch (InterruptedException e) {
            loge("Error waiting for NetworkStack connector", e);
            return null;
        }
    }

    private void requestConnector(NetworkStackCallback request) {
        this.mDependencies.checkCallerUid();
        if (!this.mWasSystemServerInitialized) {
            INetworkStackConnector connector = getRemoteConnector();
            synchronized (this.mPendingNetStackRequests) {
                this.mConnector = connector;
            }
            request.onNetworkStackConnected(connector);
            return;
        }
        synchronized (this.mPendingNetStackRequests) {
            INetworkStackConnector connector2 = this.mConnector;
            if (connector2 == null) {
                this.mPendingNetStackRequests.add(request);
            } else {
                request.onNetworkStackConnected(connector2);
            }
        }
    }

    public void dump(PrintWriter pw) {
        int requestsQueueLength;
        this.mLog.dump(null, pw, null);
        ConnectivityModuleConnector.getInstance().dump(pw);
        synchronized (this.mPendingNetStackRequests) {
            requestsQueueLength = this.mPendingNetStackRequests.size();
        }
        pw.println();
        pw.println("pendingNetStackRequests length: " + requestsQueueLength);
    }
}
