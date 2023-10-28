package com.android.server.devicepolicy;

import android.app.admin.ConnectEvent;
import android.app.admin.DnsEvent;
import android.app.admin.NetworkEvent;
import android.content.pm.PackageManagerInternal;
import android.net.IIpConnectivityMetrics;
import android.net.INetdEventCallback;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.server.ServiceThread;
import com.android.server.net.BaseNetdEventCallback;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class NetworkLogger {
    private static final String TAG = NetworkLogger.class.getSimpleName();
    private final DevicePolicyManagerService mDpm;
    private ServiceThread mHandlerThread;
    private IIpConnectivityMetrics mIpConnectivityMetrics;
    private final AtomicBoolean mIsLoggingEnabled = new AtomicBoolean(false);
    private final INetdEventCallback mNetdEventCallback = new BaseNetdEventCallback() { // from class: com.android.server.devicepolicy.NetworkLogger.1
        public void onDnsEvent(int netId, int eventType, int returnCode, String hostname, String[] ipAddresses, int ipAddressesCount, long timestamp, int uid) {
            if (!NetworkLogger.this.mIsLoggingEnabled.get() || !shouldLogNetworkEvent(uid)) {
                return;
            }
            DnsEvent dnsEvent = new DnsEvent(hostname, ipAddresses, ipAddressesCount, NetworkLogger.this.mPm.getNameForUid(uid), timestamp);
            sendNetworkEvent(dnsEvent);
        }

        public void onConnectEvent(String ipAddr, int port, long timestamp, int uid) {
            if (!NetworkLogger.this.mIsLoggingEnabled.get() || !shouldLogNetworkEvent(uid)) {
                return;
            }
            ConnectEvent connectEvent = new ConnectEvent(ipAddr, port, NetworkLogger.this.mPm.getNameForUid(uid), timestamp);
            sendNetworkEvent(connectEvent);
        }

        private void sendNetworkEvent(NetworkEvent event) {
            Message msg = NetworkLogger.this.mNetworkLoggingHandler.obtainMessage(1);
            Bundle bundle = new Bundle();
            bundle.putParcelable("network_event", event);
            msg.setData(bundle);
            NetworkLogger.this.mNetworkLoggingHandler.sendMessage(msg);
        }

        private boolean shouldLogNetworkEvent(int uid) {
            return NetworkLogger.this.mTargetUserId == -1 || NetworkLogger.this.mTargetUserId == UserHandle.getUserId(uid);
        }
    };
    private NetworkLoggingHandler mNetworkLoggingHandler;
    private final PackageManagerInternal mPm;
    private final int mTargetUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public NetworkLogger(DevicePolicyManagerService dpm, PackageManagerInternal pm, int targetUserId) {
        this.mDpm = dpm;
        this.mPm = pm;
        this.mTargetUserId = targetUserId;
    }

    private boolean checkIpConnectivityMetricsService() {
        if (this.mIpConnectivityMetrics != null) {
            return true;
        }
        IIpConnectivityMetrics service = this.mDpm.mInjector.getIIpConnectivityMetrics();
        if (service == null) {
            return false;
        }
        this.mIpConnectivityMetrics = service;
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean startNetworkLogging() {
        String str = TAG;
        Log.d(str, "Starting network logging.");
        if (!checkIpConnectivityMetricsService()) {
            Slog.wtf(str, "Failed to register callback with IIpConnectivityMetrics.");
            return false;
        }
        try {
            if (this.mIpConnectivityMetrics.addNetdEventCallback(1, this.mNetdEventCallback)) {
                ServiceThread serviceThread = new ServiceThread(str, 10, false);
                this.mHandlerThread = serviceThread;
                serviceThread.start();
                NetworkLoggingHandler networkLoggingHandler = new NetworkLoggingHandler(this.mHandlerThread.getLooper(), this.mDpm, this.mTargetUserId);
                this.mNetworkLoggingHandler = networkLoggingHandler;
                networkLoggingHandler.scheduleBatchFinalization();
                this.mIsLoggingEnabled.set(true);
                return true;
            }
            return false;
        } catch (RemoteException re) {
            Slog.wtf(TAG, "Failed to make remote calls to register the callback", re);
            return false;
        }
    }

    /* JADX DEBUG: Another duplicated slice has different insns count: {[IGET]}, finally: {[IGET, INVOKE, IF] complete} */
    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [170=5, 171=4] */
    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean stopNetworkLogging() {
        String str = TAG;
        Log.d(str, "Stopping network logging");
        this.mIsLoggingEnabled.set(false);
        discardLogs();
        try {
            try {
                if (checkIpConnectivityMetricsService()) {
                    boolean removeNetdEventCallback = this.mIpConnectivityMetrics.removeNetdEventCallback(1);
                    ServiceThread serviceThread = this.mHandlerThread;
                    if (serviceThread != null) {
                        serviceThread.quitSafely();
                    }
                    return removeNetdEventCallback;
                }
                Slog.wtf(str, "Failed to unregister callback with IIpConnectivityMetrics.");
                ServiceThread serviceThread2 = this.mHandlerThread;
                if (serviceThread2 != null) {
                    serviceThread2.quitSafely();
                }
                return true;
            } catch (RemoteException re) {
                Slog.wtf(TAG, "Failed to make remote calls to unregister the callback", re);
                ServiceThread serviceThread3 = this.mHandlerThread;
                if (serviceThread3 != null) {
                    serviceThread3.quitSafely();
                }
                return true;
            }
        } catch (Throwable th) {
            ServiceThread serviceThread4 = this.mHandlerThread;
            if (serviceThread4 != null) {
                serviceThread4.quitSafely();
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void pause() {
        NetworkLoggingHandler networkLoggingHandler = this.mNetworkLoggingHandler;
        if (networkLoggingHandler != null) {
            networkLoggingHandler.pause();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resume() {
        NetworkLoggingHandler networkLoggingHandler = this.mNetworkLoggingHandler;
        if (networkLoggingHandler != null) {
            networkLoggingHandler.resume();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void discardLogs() {
        NetworkLoggingHandler networkLoggingHandler = this.mNetworkLoggingHandler;
        if (networkLoggingHandler != null) {
            networkLoggingHandler.discardLogs();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<NetworkEvent> retrieveLogs(long batchToken) {
        return this.mNetworkLoggingHandler.retrieveFullLogBatch(batchToken);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long forceBatchFinalization() {
        return this.mNetworkLoggingHandler.forceBatchFinalization();
    }
}
