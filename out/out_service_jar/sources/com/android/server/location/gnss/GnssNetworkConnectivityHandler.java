package com.android.server.location.gnss;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.location.GpsNetInitiatedHandler;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class GnssNetworkConnectivityHandler {
    private static final int AGNSS_NET_CAPABILITY_NOT_METERED = 1;
    private static final int AGNSS_NET_CAPABILITY_NOT_ROAMING = 2;
    private static final int AGPS_DATA_CONNECTION_CLOSED = 0;
    private static final int AGPS_DATA_CONNECTION_OPEN = 2;
    private static final int AGPS_DATA_CONNECTION_OPENING = 1;
    public static final int AGPS_TYPE_C2K = 2;
    private static final int AGPS_TYPE_EIMS = 3;
    private static final int AGPS_TYPE_IMS = 4;
    public static final int AGPS_TYPE_SUPL = 1;
    private static final int APN_INVALID = 0;
    private static final int APN_IPV4 = 1;
    private static final int APN_IPV4V6 = 3;
    private static final int APN_IPV6 = 2;
    private static final int GPS_AGPS_DATA_CONNECTED = 3;
    private static final int GPS_AGPS_DATA_CONN_DONE = 4;
    private static final int GPS_AGPS_DATA_CONN_FAILED = 5;
    private static final int GPS_RELEASE_AGPS_DATA_CONN = 2;
    private static final int GPS_REQUEST_AGPS_DATA_CONN = 1;
    private static final int HASH_MAP_INITIAL_CAPACITY_TO_TRACK_CONNECTED_NETWORKS = 5;
    private static final int SUPL_NETWORK_REQUEST_TIMEOUT_MILLIS = 20000;
    static final String TAG = "GnssNetworkConnectivityHandler";
    private static final String WAKELOCK_KEY = "GnssNetworkConnectivityHandler";
    private static final long WAKELOCK_TIMEOUT_MILLIS = 60000;
    private InetAddress mAGpsDataConnectionIpAddr;
    private int mAGpsDataConnectionState;
    private int mAGpsType;
    private final ConnectivityManager mConnMgr;
    private final Context mContext;
    private final GnssNetworkListener mGnssNetworkListener;
    private final Handler mHandler;
    private ConnectivityManager.NetworkCallback mNetworkConnectivityCallback;
    private final GpsNetInitiatedHandler mNiHandler;
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener;
    private HashMap<Integer, SubIdPhoneStateListener> mPhoneStateListeners;
    private ConnectivityManager.NetworkCallback mSuplConnectivityCallback;
    private final PowerManager.WakeLock mWakeLock;
    private static final boolean DEBUG = Log.isLoggable("GnssNetworkConnectivityHandler", 3);
    private static final boolean VERBOSE = Log.isLoggable("GnssNetworkConnectivityHandler", 2);
    private HashMap<Network, NetworkAttributes> mAvailableNetworkAttributes = new HashMap<>(5);
    private int mActiveSubId = -1;

    /* loaded from: classes.dex */
    interface GnssNetworkListener {
        void onNetworkAvailable();
    }

    private native void native_agps_data_conn_closed();

    private native void native_agps_data_conn_failed();

    private native void native_agps_data_conn_open(long j, String str, int i);

    private static native boolean native_is_agps_ril_supported();

    private native void native_update_network_state(boolean z, int i, boolean z2, boolean z3, String str, long j, short s);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class NetworkAttributes {
        private String mApn;
        private NetworkCapabilities mCapabilities;
        private int mType;

        private NetworkAttributes() {
            this.mType = -1;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static boolean hasCapabilitiesChanged(NetworkCapabilities curCapabilities, NetworkCapabilities newCapabilities) {
            return curCapabilities == null || newCapabilities == null || hasCapabilityChanged(curCapabilities, newCapabilities, 18) || hasCapabilityChanged(curCapabilities, newCapabilities, 11);
        }

        private static boolean hasCapabilityChanged(NetworkCapabilities curCapabilities, NetworkCapabilities newCapabilities, int capability) {
            return curCapabilities.hasCapability(capability) != newCapabilities.hasCapability(capability);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static short getCapabilityFlags(NetworkCapabilities capabilities) {
            short capabilityFlags = 0;
            if (capabilities.hasCapability(18)) {
                capabilityFlags = (short) (0 | 2);
            }
            if (capabilities.hasCapability(11)) {
                return (short) (capabilityFlags | 1);
            }
            return capabilityFlags;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssNetworkConnectivityHandler(Context context, GnssNetworkListener gnssNetworkListener, Looper looper, GpsNetInitiatedHandler niHandler) {
        SubscriptionManager.OnSubscriptionsChangedListener onSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() { // from class: com.android.server.location.gnss.GnssNetworkConnectivityHandler.1
            @Override // android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
            public void onSubscriptionsChanged() {
                TelephonyManager subIdTelManager;
                if (GnssNetworkConnectivityHandler.this.mPhoneStateListeners == null) {
                    GnssNetworkConnectivityHandler.this.mPhoneStateListeners = new HashMap(2, 1.0f);
                }
                SubscriptionManager subManager = (SubscriptionManager) GnssNetworkConnectivityHandler.this.mContext.getSystemService(SubscriptionManager.class);
                TelephonyManager telManager = (TelephonyManager) GnssNetworkConnectivityHandler.this.mContext.getSystemService(TelephonyManager.class);
                if (subManager != null && telManager != null) {
                    List<SubscriptionInfo> subscriptionInfoList = subManager.getActiveSubscriptionInfoList();
                    HashSet<Integer> activeSubIds = new HashSet<>();
                    if (subscriptionInfoList != null) {
                        if (GnssNetworkConnectivityHandler.DEBUG) {
                            Log.d("GnssNetworkConnectivityHandler", "Active Sub List size: " + subscriptionInfoList.size());
                        }
                        for (SubscriptionInfo subInfo : subscriptionInfoList) {
                            activeSubIds.add(Integer.valueOf(subInfo.getSubscriptionId()));
                            if (!GnssNetworkConnectivityHandler.this.mPhoneStateListeners.containsKey(Integer.valueOf(subInfo.getSubscriptionId())) && (subIdTelManager = telManager.createForSubscriptionId(subInfo.getSubscriptionId())) != null) {
                                if (GnssNetworkConnectivityHandler.DEBUG) {
                                    Log.d("GnssNetworkConnectivityHandler", "Listener sub" + subInfo.getSubscriptionId());
                                }
                                SubIdPhoneStateListener subIdPhoneStateListener = new SubIdPhoneStateListener(Integer.valueOf(subInfo.getSubscriptionId()));
                                GnssNetworkConnectivityHandler.this.mPhoneStateListeners.put(Integer.valueOf(subInfo.getSubscriptionId()), subIdPhoneStateListener);
                                subIdTelManager.listen(subIdPhoneStateListener, 2048);
                            }
                        }
                    }
                    Iterator<Map.Entry<Integer, SubIdPhoneStateListener>> iterator = GnssNetworkConnectivityHandler.this.mPhoneStateListeners.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<Integer, SubIdPhoneStateListener> element = iterator.next();
                        if (!activeSubIds.contains(element.getKey())) {
                            TelephonyManager subIdTelManager2 = telManager.createForSubscriptionId(element.getKey().intValue());
                            if (subIdTelManager2 == null) {
                                Log.e("GnssNetworkConnectivityHandler", "Telephony Manager for Sub " + element.getKey() + " null");
                            } else {
                                if (GnssNetworkConnectivityHandler.DEBUG) {
                                    Log.d("GnssNetworkConnectivityHandler", "unregister listener sub " + element.getKey());
                                }
                                subIdTelManager2.listen(element.getValue(), 0);
                                iterator.remove();
                            }
                        }
                    }
                    if (!activeSubIds.contains(Integer.valueOf(GnssNetworkConnectivityHandler.this.mActiveSubId))) {
                        GnssNetworkConnectivityHandler.this.mActiveSubId = -1;
                    }
                }
            }
        };
        this.mOnSubscriptionsChangeListener = onSubscriptionsChangedListener;
        this.mContext = context;
        this.mGnssNetworkListener = gnssNetworkListener;
        SubscriptionManager subManager = (SubscriptionManager) context.getSystemService(SubscriptionManager.class);
        if (subManager != null) {
            subManager.addOnSubscriptionsChangedListener(onSubscriptionsChangedListener);
        }
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, "GnssNetworkConnectivityHandler");
        this.mHandler = new Handler(looper);
        this.mNiHandler = niHandler;
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mSuplConnectivityCallback = null;
    }

    /* loaded from: classes.dex */
    private final class SubIdPhoneStateListener extends PhoneStateListener {
        private Integer mSubId;

        SubIdPhoneStateListener(Integer subId) {
            this.mSubId = subId;
        }

        public void onPreciseCallStateChanged(PreciseCallState state) {
            if (1 == state.getForegroundCallState() || 3 == state.getForegroundCallState()) {
                GnssNetworkConnectivityHandler.this.mActiveSubId = this.mSubId.intValue();
                if (GnssNetworkConnectivityHandler.DEBUG) {
                    Log.d("GnssNetworkConnectivityHandler", "mActiveSubId: " + GnssNetworkConnectivityHandler.this.mActiveSubId);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerNetworkCallbacks() {
        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        networkRequestBuilder.addCapability(12);
        networkRequestBuilder.addCapability(16);
        networkRequestBuilder.removeCapability(15);
        NetworkRequest networkRequest = networkRequestBuilder.build();
        ConnectivityManager.NetworkCallback createNetworkConnectivityCallback = createNetworkConnectivityCallback();
        this.mNetworkConnectivityCallback = createNetworkConnectivityCallback;
        this.mConnMgr.registerNetworkCallback(networkRequest, createNetworkConnectivityCallback, this.mHandler);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDataNetworkConnected() {
        NetworkInfo activeNetworkInfo = this.mConnMgr.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onReportAGpsStatus(final int agpsType, int agpsStatus, final byte[] suplIpAddr) {
        if (DEBUG) {
            Log.d("GnssNetworkConnectivityHandler", "AGPS_DATA_CONNECTION: " + agpsDataConnStatusAsString(agpsStatus));
        }
        switch (agpsStatus) {
            case 1:
                runOnHandler(new Runnable() { // from class: com.android.server.location.gnss.GnssNetworkConnectivityHandler$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        GnssNetworkConnectivityHandler.this.m4391x6ecd616d(agpsType, suplIpAddr);
                    }
                });
                return;
            case 2:
                runOnHandler(new Runnable() { // from class: com.android.server.location.gnss.GnssNetworkConnectivityHandler$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        GnssNetworkConnectivityHandler.this.m4392xbc8cd96e();
                    }
                });
                return;
            case 3:
            case 4:
            case 5:
                return;
            default:
                Log.w("GnssNetworkConnectivityHandler", "Received unknown AGPS status: " + agpsStatus);
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onReportAGpsStatus$1$com-android-server-location-gnss-GnssNetworkConnectivityHandler  reason: not valid java name */
    public /* synthetic */ void m4392xbc8cd96e() {
        handleReleaseSuplConnection(2);
    }

    private ConnectivityManager.NetworkCallback createNetworkConnectivityCallback() {
        return new ConnectivityManager.NetworkCallback() { // from class: com.android.server.location.gnss.GnssNetworkConnectivityHandler.2
            private HashMap<Network, NetworkCapabilities> mAvailableNetworkCapabilities = new HashMap<>(5);

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                if (!NetworkAttributes.hasCapabilitiesChanged(this.mAvailableNetworkCapabilities.get(network), capabilities)) {
                    if (GnssNetworkConnectivityHandler.VERBOSE) {
                        Log.v("GnssNetworkConnectivityHandler", "Relevant network capabilities unchanged. Capabilities: " + capabilities);
                        return;
                    }
                    return;
                }
                this.mAvailableNetworkCapabilities.put(network, capabilities);
                if (GnssNetworkConnectivityHandler.DEBUG) {
                    Log.d("GnssNetworkConnectivityHandler", "Network connected/capabilities updated. Available networks count: " + this.mAvailableNetworkCapabilities.size());
                }
                GnssNetworkConnectivityHandler.this.mGnssNetworkListener.onNetworkAvailable();
                GnssNetworkConnectivityHandler.this.handleUpdateNetworkState(network, true, capabilities);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                if (this.mAvailableNetworkCapabilities.remove(network) == null) {
                    Log.w("GnssNetworkConnectivityHandler", "Incorrectly received network callback onLost() before onCapabilitiesChanged() for network: " + network);
                    return;
                }
                Log.i("GnssNetworkConnectivityHandler", "Network connection lost. Available networks count: " + this.mAvailableNetworkCapabilities.size());
                GnssNetworkConnectivityHandler.this.handleUpdateNetworkState(network, false, null);
            }
        };
    }

    private ConnectivityManager.NetworkCallback createSuplConnectivityCallback() {
        return new ConnectivityManager.NetworkCallback() { // from class: com.android.server.location.gnss.GnssNetworkConnectivityHandler.3
            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                if (GnssNetworkConnectivityHandler.DEBUG) {
                    Log.d("GnssNetworkConnectivityHandler", "SUPL network connection available.");
                }
                GnssNetworkConnectivityHandler.this.handleSuplConnectionAvailable(network, linkProperties);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                Log.i("GnssNetworkConnectivityHandler", "SUPL network connection lost.");
                GnssNetworkConnectivityHandler.this.handleReleaseSuplConnection(2);
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onUnavailable() {
                Log.i("GnssNetworkConnectivityHandler", "SUPL network connection request timed out.");
                GnssNetworkConnectivityHandler.this.handleReleaseSuplConnection(5);
            }
        };
    }

    private void runOnHandler(Runnable event) {
        this.mWakeLock.acquire(60000L);
        if (!this.mHandler.post(runEventAndReleaseWakeLock(event))) {
            this.mWakeLock.release();
        }
    }

    private Runnable runEventAndReleaseWakeLock(final Runnable event) {
        return new Runnable() { // from class: com.android.server.location.gnss.GnssNetworkConnectivityHandler$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                GnssNetworkConnectivityHandler.this.m4393x69376eaf(event);
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$runEventAndReleaseWakeLock$2$com-android-server-location-gnss-GnssNetworkConnectivityHandler  reason: not valid java name */
    public /* synthetic */ void m4393x69376eaf(Runnable event) {
        try {
            event.run();
        } finally {
            this.mWakeLock.release();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUpdateNetworkState(Network network, boolean isConnected, NetworkCapabilities capabilities) {
        boolean networkAvailable;
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            networkAvailable = false;
        } else {
            boolean networkAvailable2 = isConnected && telephonyManager.getDataEnabled();
            networkAvailable = networkAvailable2;
        }
        NetworkAttributes networkAttributes = updateTrackedNetworksState(isConnected, network, capabilities);
        String apn = networkAttributes.mApn;
        int type = networkAttributes.mType;
        NetworkCapabilities capabilities2 = networkAttributes.mCapabilities;
        Log.i("GnssNetworkConnectivityHandler", String.format("updateNetworkState, state=%s, connected=%s, network=%s, capabilities=%s, availableNetworkCount: %d", agpsDataConnStateAsString(), Boolean.valueOf(isConnected), network, capabilities2, Integer.valueOf(this.mAvailableNetworkAttributes.size())));
        if (native_is_agps_ril_supported()) {
            native_update_network_state(isConnected, type, !capabilities2.hasTransport(18), networkAvailable, apn != null ? apn : "", network.getNetworkHandle(), NetworkAttributes.getCapabilityFlags(capabilities2));
        } else if (DEBUG) {
            Log.d("GnssNetworkConnectivityHandler", "Skipped network state update because GPS HAL AGPS-RIL is not  supported");
        }
    }

    private NetworkAttributes updateTrackedNetworksState(boolean isConnected, Network network, NetworkCapabilities capabilities) {
        if (!isConnected) {
            return this.mAvailableNetworkAttributes.remove(network);
        }
        NetworkAttributes networkAttributes = this.mAvailableNetworkAttributes.get(network);
        if (networkAttributes != null) {
            networkAttributes.mCapabilities = capabilities;
            return networkAttributes;
        }
        NetworkAttributes networkAttributes2 = new NetworkAttributes();
        networkAttributes2.mCapabilities = capabilities;
        NetworkInfo info = this.mConnMgr.getNetworkInfo(network);
        if (info != null) {
            networkAttributes2.mApn = info.getExtraInfo();
            networkAttributes2.mType = info.getType();
        }
        this.mAvailableNetworkAttributes.put(network, networkAttributes2);
        return networkAttributes2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSuplConnectionAvailable(Network network, LinkProperties linkProperties) {
        NetworkInfo info = this.mConnMgr.getNetworkInfo(network);
        String apn = null;
        if (info != null) {
            apn = info.getExtraInfo();
        }
        boolean z = DEBUG;
        if (z) {
            String message = String.format("handleSuplConnectionAvailable: state=%s, suplNetwork=%s, info=%s", agpsDataConnStateAsString(), network, info);
            Log.d("GnssNetworkConnectivityHandler", message);
        }
        if (this.mAGpsDataConnectionState == 1) {
            if (apn == null) {
                apn = "dummy-apn";
            }
            if (this.mAGpsDataConnectionIpAddr != null) {
                setRouting();
            }
            int apnIpType = getLinkIpType(linkProperties);
            if (z) {
                String message2 = String.format("native_agps_data_conn_open: mAgpsApn=%s, mApnIpType=%s", apn, Integer.valueOf(apnIpType));
                Log.d("GnssNetworkConnectivityHandler", message2);
            }
            native_agps_data_conn_open(network.getNetworkHandle(), apn, apnIpType);
            this.mAGpsDataConnectionState = 2;
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleRequestSuplConnection */
    public void m4391x6ecd616d(int agpsType, byte[] suplIpAddr) {
        this.mAGpsDataConnectionIpAddr = null;
        this.mAGpsType = agpsType;
        if (suplIpAddr != null) {
            if (VERBOSE) {
                Log.v("GnssNetworkConnectivityHandler", "Received SUPL IP addr[]: " + Arrays.toString(suplIpAddr));
            }
            try {
                this.mAGpsDataConnectionIpAddr = InetAddress.getByAddress(suplIpAddr);
                if (DEBUG) {
                    Log.d("GnssNetworkConnectivityHandler", "IP address converted to: " + this.mAGpsDataConnectionIpAddr);
                }
            } catch (UnknownHostException e) {
                Log.e("GnssNetworkConnectivityHandler", "Bad IP Address: " + suplIpAddr, e);
            }
        }
        boolean z = DEBUG;
        if (z) {
            String message = String.format("requestSuplConnection, state=%s, agpsType=%s, address=%s", agpsDataConnStateAsString(), agpsTypeAsString(agpsType), this.mAGpsDataConnectionIpAddr);
            Log.d("GnssNetworkConnectivityHandler", message);
        }
        if (this.mAGpsDataConnectionState != 0) {
            return;
        }
        this.mAGpsDataConnectionState = 1;
        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        networkRequestBuilder.addCapability(getNetworkCapability(this.mAGpsType));
        networkRequestBuilder.addTransportType(0);
        if (this.mNiHandler.getInEmergency() && this.mActiveSubId >= 0) {
            if (z) {
                Log.d("GnssNetworkConnectivityHandler", "Adding Network Specifier: " + Integer.toString(this.mActiveSubId));
            }
            networkRequestBuilder.setNetworkSpecifier(Integer.toString(this.mActiveSubId));
            networkRequestBuilder.removeCapability(13);
        }
        NetworkRequest networkRequest = networkRequestBuilder.build();
        ConnectivityManager.NetworkCallback networkCallback = this.mSuplConnectivityCallback;
        if (networkCallback != null) {
            this.mConnMgr.unregisterNetworkCallback(networkCallback);
        }
        ConnectivityManager.NetworkCallback createSuplConnectivityCallback = createSuplConnectivityCallback();
        this.mSuplConnectivityCallback = createSuplConnectivityCallback;
        try {
            this.mConnMgr.requestNetwork(networkRequest, createSuplConnectivityCallback, this.mHandler, SUPL_NETWORK_REQUEST_TIMEOUT_MILLIS);
        } catch (RuntimeException e2) {
            Log.e("GnssNetworkConnectivityHandler", "Failed to request network.", e2);
            this.mSuplConnectivityCallback = null;
            handleReleaseSuplConnection(5);
        }
    }

    private int getNetworkCapability(int agpsType) {
        switch (agpsType) {
            case 1:
            case 2:
                return 1;
            case 3:
                return 10;
            case 4:
                return 4;
            default:
                throw new IllegalArgumentException("agpsType: " + agpsType);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleReleaseSuplConnection(int agpsDataConnStatus) {
        if (DEBUG) {
            String message = String.format("releaseSuplConnection, state=%s, status=%s", agpsDataConnStateAsString(), agpsDataConnStatusAsString(agpsDataConnStatus));
            Log.d("GnssNetworkConnectivityHandler", message);
        }
        if (this.mAGpsDataConnectionState == 0) {
            return;
        }
        this.mAGpsDataConnectionState = 0;
        ConnectivityManager.NetworkCallback networkCallback = this.mSuplConnectivityCallback;
        if (networkCallback != null) {
            this.mConnMgr.unregisterNetworkCallback(networkCallback);
            this.mSuplConnectivityCallback = null;
        }
        switch (agpsDataConnStatus) {
            case 2:
                native_agps_data_conn_closed();
                return;
            case 5:
                native_agps_data_conn_failed();
                return;
            default:
                Log.e("GnssNetworkConnectivityHandler", "Invalid status to release SUPL connection: " + agpsDataConnStatus);
                return;
        }
    }

    private void setRouting() {
        boolean result = this.mConnMgr.requestRouteToHostAddress(3, this.mAGpsDataConnectionIpAddr);
        if (!result) {
            Log.e("GnssNetworkConnectivityHandler", "Error requesting route to host: " + this.mAGpsDataConnectionIpAddr);
        } else if (DEBUG) {
            Log.d("GnssNetworkConnectivityHandler", "Successfully requested route to host: " + this.mAGpsDataConnectionIpAddr);
        }
    }

    private void ensureInHandlerThread() {
        if (this.mHandler != null && Looper.myLooper() == this.mHandler.getLooper()) {
            return;
        }
        throw new IllegalStateException("This method must run on the Handler thread.");
    }

    private String agpsDataConnStateAsString() {
        switch (this.mAGpsDataConnectionState) {
            case 0:
                return "CLOSED";
            case 1:
                return "OPENING";
            case 2:
                return "OPEN";
            default:
                return "<Unknown>(" + this.mAGpsDataConnectionState + ")";
        }
    }

    private String agpsDataConnStatusAsString(int agpsDataConnStatus) {
        switch (agpsDataConnStatus) {
            case 1:
                return "REQUEST";
            case 2:
                return "RELEASE";
            case 3:
                return "CONNECTED";
            case 4:
                return "DONE";
            case 5:
                return "FAILED";
            default:
                return "<Unknown>(" + agpsDataConnStatus + ")";
        }
    }

    private String agpsTypeAsString(int agpsType) {
        switch (agpsType) {
            case 1:
                return "SUPL";
            case 2:
                return "C2K";
            case 3:
                return "EIMS";
            case 4:
                return "IMS";
            default:
                return "<Unknown>(" + agpsType + ")";
        }
    }

    private int getLinkIpType(LinkProperties linkProperties) {
        ensureInHandlerThread();
        boolean isIPv4 = false;
        boolean isIPv6 = false;
        List<LinkAddress> linkAddresses = linkProperties.getLinkAddresses();
        for (LinkAddress linkAddress : linkAddresses) {
            InetAddress inetAddress = linkAddress.getAddress();
            if (inetAddress instanceof Inet4Address) {
                isIPv4 = true;
            } else if (inetAddress instanceof Inet6Address) {
                isIPv6 = true;
            }
            if (DEBUG) {
                Log.d("GnssNetworkConnectivityHandler", "LinkAddress : " + inetAddress.toString());
            }
        }
        if (isIPv4 && isIPv6) {
            return 3;
        }
        if (isIPv4) {
            return 1;
        }
        if (isIPv6) {
            return 2;
        }
        return 0;
    }
}
