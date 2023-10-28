package com.android.server.net.watchlist;

import android.content.Context;
import android.net.IIpConnectivityMetrics;
import android.net.INetdEventCallback;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.net.INetworkWatchlistManager;
import com.android.internal.util.DumpUtils;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import com.android.server.net.BaseNetdEventCallback;
import java.io.FileDescriptor;
import java.io.PrintWriter;
/* loaded from: classes2.dex */
public class NetworkWatchlistService extends INetworkWatchlistManager.Stub {
    static final boolean DEBUG = false;
    private static final int MAX_NUM_OF_WATCHLIST_DIGESTS = 10000;
    private static final String TAG = NetworkWatchlistService.class.getSimpleName();
    private final Context mContext;
    private final ServiceThread mHandlerThread;
    IIpConnectivityMetrics mIpConnectivityMetrics;
    WatchlistLoggingHandler mNetworkWatchlistHandler;
    private volatile boolean mIsLoggingEnabled = false;
    private final Object mLoggingSwitchLock = new Object();
    private final INetdEventCallback mNetdEventCallback = new BaseNetdEventCallback() { // from class: com.android.server.net.watchlist.NetworkWatchlistService.1
        public void onDnsEvent(int netId, int eventType, int returnCode, String hostname, String[] ipAddresses, int ipAddressesCount, long timestamp, int uid) {
            if (!NetworkWatchlistService.this.mIsLoggingEnabled) {
                return;
            }
            NetworkWatchlistService.this.mNetworkWatchlistHandler.asyncNetworkEvent(hostname, ipAddresses, uid);
        }

        public void onConnectEvent(String ipAddr, int port, long timestamp, int uid) {
            if (!NetworkWatchlistService.this.mIsLoggingEnabled) {
                return;
            }
            NetworkWatchlistService.this.mNetworkWatchlistHandler.asyncNetworkEvent(null, new String[]{ipAddr}, uid);
        }
    };
    private final WatchlistConfig mConfig = WatchlistConfig.getInstance();

    /* loaded from: classes2.dex */
    public static class Lifecycle extends SystemService {
        private NetworkWatchlistService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: com.android.server.net.watchlist.NetworkWatchlistService$Lifecycle */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v3, types: [com.android.server.net.watchlist.NetworkWatchlistService, android.os.IBinder] */
        @Override // com.android.server.SystemService
        public void onStart() {
            if (Settings.Global.getInt(getContext().getContentResolver(), "network_watchlist_enabled", 1) == 0) {
                Slog.i(NetworkWatchlistService.TAG, "Network Watchlist service is disabled");
                return;
            }
            ?? networkWatchlistService = new NetworkWatchlistService(getContext());
            this.mService = networkWatchlistService;
            publishBinderService("network_watchlist", networkWatchlistService);
        }

        @Override // com.android.server.SystemService
        public void onBootPhase(int phase) {
            if (phase == 550) {
                if (Settings.Global.getInt(getContext().getContentResolver(), "network_watchlist_enabled", 1) == 0) {
                    Slog.i(NetworkWatchlistService.TAG, "Network Watchlist service is disabled");
                    return;
                }
                try {
                    this.mService.init();
                    this.mService.initIpConnectivityMetrics();
                    this.mService.startWatchlistLogging();
                } catch (RemoteException e) {
                }
                ReportWatchlistJobService.schedule(getContext());
            }
        }
    }

    public NetworkWatchlistService(Context context) {
        this.mContext = context;
        ServiceThread serviceThread = new ServiceThread(TAG, 10, false);
        this.mHandlerThread = serviceThread;
        serviceThread.start();
        WatchlistLoggingHandler watchlistLoggingHandler = new WatchlistLoggingHandler(context, serviceThread.getLooper());
        this.mNetworkWatchlistHandler = watchlistLoggingHandler;
        watchlistLoggingHandler.reportWatchlistIfNecessary();
    }

    NetworkWatchlistService(Context context, ServiceThread handlerThread, WatchlistLoggingHandler handler, IIpConnectivityMetrics ipConnectivityMetrics) {
        this.mContext = context;
        this.mHandlerThread = handlerThread;
        this.mNetworkWatchlistHandler = handler;
        this.mIpConnectivityMetrics = ipConnectivityMetrics;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void init() {
        this.mConfig.removeTestModeConfig();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initIpConnectivityMetrics() {
        this.mIpConnectivityMetrics = IIpConnectivityMetrics.Stub.asInterface(ServiceManager.getService("connmetrics"));
    }

    private boolean isCallerShell() {
        int callingUid = Binder.getCallingUid();
        return callingUid == 2000 || callingUid == 0;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.net.watchlist.NetworkWatchlistService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        if (!isCallerShell()) {
            Slog.w(TAG, "Only shell is allowed to call network watchlist shell commands");
        } else {
            new NetworkWatchlistShellCommand(this, this.mContext).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    protected boolean startWatchlistLoggingImpl() throws RemoteException {
        synchronized (this.mLoggingSwitchLock) {
            if (this.mIsLoggingEnabled) {
                Slog.w(TAG, "Watchlist logging is already running");
                return true;
            }
            try {
                if (!this.mIpConnectivityMetrics.addNetdEventCallback(2, this.mNetdEventCallback)) {
                    return false;
                }
                this.mIsLoggingEnabled = true;
                return true;
            } catch (RemoteException e) {
                return false;
            }
        }
    }

    public boolean startWatchlistLogging() throws RemoteException {
        enforceWatchlistLoggingPermission();
        return startWatchlistLoggingImpl();
    }

    protected boolean stopWatchlistLoggingImpl() {
        synchronized (this.mLoggingSwitchLock) {
            if (!this.mIsLoggingEnabled) {
                Slog.w(TAG, "Watchlist logging is not running");
                return true;
            }
            this.mIsLoggingEnabled = false;
            try {
                return this.mIpConnectivityMetrics.removeNetdEventCallback(2);
            } catch (RemoteException e) {
                return false;
            }
        }
    }

    public boolean stopWatchlistLogging() throws RemoteException {
        enforceWatchlistLoggingPermission();
        return stopWatchlistLoggingImpl();
    }

    public byte[] getWatchlistConfigHash() {
        return this.mConfig.getWatchlistConfigHash();
    }

    private void enforceWatchlistLoggingPermission() {
        int uid = Binder.getCallingUid();
        if (uid != 1000) {
            throw new SecurityException(String.format("Uid %d has no permission to change watchlist setting.", Integer.valueOf(uid)));
        }
    }

    public void reloadWatchlist() throws RemoteException {
        enforceWatchlistLoggingPermission();
        Slog.i(TAG, "Reloading watchlist");
        this.mConfig.reloadConfig();
    }

    public void reportWatchlistIfNecessary() {
        this.mNetworkWatchlistHandler.reportWatchlistIfNecessary();
    }

    public boolean forceReportWatchlistForTest(long lastReportTime) {
        if (this.mConfig.isConfigSecure()) {
            return false;
        }
        this.mNetworkWatchlistHandler.forceReportWatchlistForTest(lastReportTime);
        return true;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            this.mConfig.dump(fd, pw, args);
        }
    }
}
