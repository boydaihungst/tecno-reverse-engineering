package com.android.server.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.server.connectivity.Vpn;
import java.util.List;
import java.util.Objects;
/* loaded from: classes2.dex */
public class LockdownVpnTracker {
    public static final String ACTION_LOCKDOWN_RESET = "com.android.server.action.LOCKDOWN_RESET";
    private static final String TAG = "LockdownVpnTracker";
    private String mAcceptedEgressIface;
    private final ConnectivityManager mCm;
    private final PendingIntent mConfigIntent;
    private final Context mContext;
    private final Handler mHandler;
    private final NotificationManager mNotificationManager;
    private final VpnProfile mProfile;
    private final PendingIntent mResetIntent;
    private final Vpn mVpn;
    private final Object mStateLock = new Object();
    private final NetworkCallback mDefaultNetworkCallback = new NetworkCallback();
    private final VpnNetworkCallback mVpnNetworkCallback = new VpnNetworkCallback();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class NetworkCallback extends ConnectivityManager.NetworkCallback {
        private LinkProperties mLinkProperties;
        private Network mNetwork;

        private NetworkCallback() {
            this.mNetwork = null;
            this.mLinkProperties = null;
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLinkPropertiesChanged(Network network, LinkProperties lp) {
            boolean networkChanged = false;
            if (!network.equals(this.mNetwork)) {
                this.mNetwork = network;
                networkChanged = true;
            }
            this.mLinkProperties = lp;
            if (networkChanged) {
                synchronized (LockdownVpnTracker.this.mStateLock) {
                    LockdownVpnTracker.this.handleStateChangedLocked();
                }
            }
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            this.mNetwork = null;
            this.mLinkProperties = null;
            synchronized (LockdownVpnTracker.this.mStateLock) {
                LockdownVpnTracker.this.handleStateChangedLocked();
            }
        }

        public Network getNetwork() {
            return this.mNetwork;
        }

        public LinkProperties getLinkProperties() {
            return this.mLinkProperties;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class VpnNetworkCallback extends NetworkCallback {
        private VpnNetworkCallback() {
            super();
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            synchronized (LockdownVpnTracker.this.mStateLock) {
                LockdownVpnTracker.this.handleStateChangedLocked();
            }
        }

        @Override // com.android.server.net.LockdownVpnTracker.NetworkCallback, android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            onAvailable(network);
        }
    }

    public LockdownVpnTracker(Context context, Handler handler, Vpn vpn, VpnProfile profile) {
        Context context2 = (Context) Objects.requireNonNull(context);
        this.mContext = context2;
        this.mCm = (ConnectivityManager) context2.getSystemService(ConnectivityManager.class);
        this.mHandler = (Handler) Objects.requireNonNull(handler);
        this.mVpn = (Vpn) Objects.requireNonNull(vpn);
        this.mProfile = (VpnProfile) Objects.requireNonNull(profile);
        this.mNotificationManager = (NotificationManager) context2.getSystemService(NotificationManager.class);
        Intent configIntent = new Intent("android.settings.VPN_SETTINGS");
        this.mConfigIntent = PendingIntent.getActivity(context2, 0, configIntent, 67108864);
        Intent resetIntent = new Intent(ACTION_LOCKDOWN_RESET);
        resetIntent.addFlags(1073741824);
        this.mResetIntent = PendingIntent.getBroadcast(context2, 0, resetIntent, 67108864);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleStateChangedLocked() {
        Network network = this.mDefaultNetworkCallback.getNetwork();
        LinkProperties egressProp = this.mDefaultNetworkCallback.getLinkProperties();
        NetworkInfo vpnInfo = this.mVpn.getNetworkInfo();
        VpnConfig vpnConfig = this.mVpn.getLegacyVpnConfig();
        boolean egressChanged = true;
        boolean egressDisconnected = network == null;
        if (egressProp != null && TextUtils.equals(this.mAcceptedEgressIface, egressProp.getInterfaceName())) {
            egressChanged = false;
        }
        String egressIface = egressProp == null ? null : egressProp.getInterfaceName();
        Log.d(TAG, "handleStateChanged: egress=" + this.mAcceptedEgressIface + "->" + egressIface);
        if (egressDisconnected || egressChanged) {
            this.mAcceptedEgressIface = null;
            this.mVpn.stopVpnRunnerPrivileged();
        }
        if (egressDisconnected) {
            hideNotification();
        } else if (!vpnInfo.isConnectedOrConnecting()) {
            if (!this.mProfile.isValidLockdownProfile()) {
                Log.e(TAG, "Invalid VPN profile; requires IP-based server and DNS");
                showNotification(17041715, 17303871);
                return;
            }
            Log.d(TAG, "Active network connected; starting VPN");
            showNotification(17041713, 17303871);
            this.mAcceptedEgressIface = egressIface;
            try {
                this.mVpn.startLegacyVpnPrivileged(this.mProfile, network, egressProp);
            } catch (IllegalStateException e) {
                this.mAcceptedEgressIface = null;
                Log.e(TAG, "Failed to start VPN", e);
                showNotification(17041715, 17303871);
            }
        } else if (vpnInfo.isConnected() && vpnConfig != null) {
            String iface = vpnConfig.interfaze;
            List<LinkAddress> sourceAddrs = vpnConfig.addresses;
            Log.d(TAG, "VPN connected using iface=" + iface + ", sourceAddr=" + sourceAddrs.toString());
            showNotification(17041712, 17303870);
        }
    }

    public void init() {
        synchronized (this.mStateLock) {
            initLocked();
        }
    }

    private void initLocked() {
        Log.d(TAG, "initLocked()");
        this.mVpn.setEnableTeardown(false);
        this.mVpn.setLockdown(true);
        this.mCm.setLegacyLockdownVpnEnabled(true);
        handleStateChangedLocked();
        this.mCm.registerSystemDefaultNetworkCallback(this.mDefaultNetworkCallback, this.mHandler);
        NetworkRequest vpnRequest = new NetworkRequest.Builder().clearCapabilities().addTransportType(4).build();
        this.mCm.registerNetworkCallback(vpnRequest, this.mVpnNetworkCallback, this.mHandler);
    }

    public void shutdown() {
        synchronized (this.mStateLock) {
            shutdownLocked();
        }
    }

    private void shutdownLocked() {
        Log.d(TAG, "shutdownLocked()");
        this.mAcceptedEgressIface = null;
        this.mVpn.stopVpnRunnerPrivileged();
        this.mVpn.setLockdown(false);
        this.mCm.setLegacyLockdownVpnEnabled(false);
        hideNotification();
        this.mVpn.setEnableTeardown(true);
        this.mCm.unregisterNetworkCallback(this.mDefaultNetworkCallback);
        this.mCm.unregisterNetworkCallback(this.mVpnNetworkCallback);
    }

    public void reset() {
        Log.d(TAG, "reset()");
        synchronized (this.mStateLock) {
            shutdownLocked();
            initLocked();
            handleStateChangedLocked();
        }
    }

    private void showNotification(int titleRes, int iconRes) {
        Notification.Builder builder = new Notification.Builder(this.mContext, "VPN").setWhen(0L).setSmallIcon(iconRes).setContentTitle(this.mContext.getString(titleRes)).setContentText(this.mContext.getString(17041711)).setContentIntent(this.mConfigIntent).setOngoing(true).addAction(17302784, this.mContext.getString(17041391), this.mResetIntent).setColor(this.mContext.getColor(17170460));
        this.mNotificationManager.notify(null, 20, builder.build());
    }

    private void hideNotification() {
        this.mNotificationManager.cancel(null, 20);
    }
}
