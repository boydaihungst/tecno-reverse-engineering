package com.android.server.vcn.routeselection;

import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TelephonyNetworkSpecifier;
import android.net.vcn.VcnGatewayConnectionConfig;
import android.net.vcn.VcnUnderlyingNetworkTemplate;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.ParcelUuid;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.VcnManagementService;
import com.android.server.vcn.TelephonySubscriptionTracker;
import com.android.server.vcn.VcnContext;
import com.android.server.vcn.routeselection.UnderlyingNetworkRecord;
import com.android.server.vcn.util.LogUtils;
import com.android.server.vcn.util.PersistableBundleUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
/* loaded from: classes2.dex */
public class UnderlyingNetworkController {
    private static final String TAG = UnderlyingNetworkController.class.getSimpleName();
    private final TelephonyCallback mActiveDataSubIdListener;
    private PersistableBundleUtils.PersistableBundleWrapper mCarrierConfig;
    private final UnderlyingNetworkControllerCallback mCb;
    private final List<ConnectivityManager.NetworkCallback> mCellBringupCallbacks;
    private final VcnGatewayConnectionConfig mConnectionConfig;
    private final ConnectivityManager mConnectivityManager;
    private UnderlyingNetworkRecord mCurrentRecord;
    private final Dependencies mDeps;
    private final Handler mHandler;
    private boolean mIsQuitting;
    private TelephonySubscriptionTracker.TelephonySubscriptionSnapshot mLastSnapshot;
    private UnderlyingNetworkRecord.Builder mRecordInProgress;
    private UnderlyingNetworkListener mRouteSelectionCallback;
    private final ParcelUuid mSubscriptionGroup;
    private final VcnContext mVcnContext;
    private ConnectivityManager.NetworkCallback mWifiBringupCallback;
    private ConnectivityManager.NetworkCallback mWifiEntryRssiThresholdCallback;
    private ConnectivityManager.NetworkCallback mWifiExitRssiThresholdCallback;

    /* loaded from: classes2.dex */
    public interface UnderlyingNetworkControllerCallback {
        void onSelectedUnderlyingNetworkChanged(UnderlyingNetworkRecord underlyingNetworkRecord);
    }

    public UnderlyingNetworkController(VcnContext vcnContext, VcnGatewayConnectionConfig connectionConfig, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, UnderlyingNetworkControllerCallback cb) {
        this(vcnContext, connectionConfig, subscriptionGroup, snapshot, cb, new Dependencies());
    }

    private UnderlyingNetworkController(VcnContext vcnContext, VcnGatewayConnectionConfig connectionConfig, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, UnderlyingNetworkControllerCallback cb, Dependencies deps) {
        VcnActiveDataSubscriptionIdListener vcnActiveDataSubscriptionIdListener = new VcnActiveDataSubscriptionIdListener();
        this.mActiveDataSubIdListener = vcnActiveDataSubscriptionIdListener;
        this.mCellBringupCallbacks = new ArrayList();
        this.mIsQuitting = false;
        VcnContext vcnContext2 = (VcnContext) Objects.requireNonNull(vcnContext, "Missing vcnContext");
        this.mVcnContext = vcnContext2;
        this.mConnectionConfig = (VcnGatewayConnectionConfig) Objects.requireNonNull(connectionConfig, "Missing connectionConfig");
        ParcelUuid parcelUuid = (ParcelUuid) Objects.requireNonNull(subscriptionGroup, "Missing subscriptionGroup");
        this.mSubscriptionGroup = parcelUuid;
        this.mLastSnapshot = (TelephonySubscriptionTracker.TelephonySubscriptionSnapshot) Objects.requireNonNull(snapshot, "Missing snapshot");
        this.mCb = (UnderlyingNetworkControllerCallback) Objects.requireNonNull(cb, "Missing cb");
        this.mDeps = (Dependencies) Objects.requireNonNull(deps, "Missing deps");
        Handler handler = new Handler(vcnContext2.getLooper());
        this.mHandler = handler;
        this.mConnectivityManager = (ConnectivityManager) vcnContext2.getContext().getSystemService(ConnectivityManager.class);
        ((TelephonyManager) vcnContext2.getContext().getSystemService(TelephonyManager.class)).registerTelephonyCallback(new HandlerExecutor(handler), vcnActiveDataSubscriptionIdListener);
        this.mCarrierConfig = this.mLastSnapshot.getCarrierConfigForSubGrp(parcelUuid);
        registerOrUpdateNetworkRequests();
    }

    private void registerOrUpdateNetworkRequests() {
        ConnectivityManager.NetworkCallback oldRouteSelectionCallback = this.mRouteSelectionCallback;
        ConnectivityManager.NetworkCallback oldWifiCallback = this.mWifiBringupCallback;
        ConnectivityManager.NetworkCallback oldWifiEntryRssiThresholdCallback = this.mWifiEntryRssiThresholdCallback;
        ConnectivityManager.NetworkCallback oldWifiExitRssiThresholdCallback = this.mWifiExitRssiThresholdCallback;
        List<ConnectivityManager.NetworkCallback> oldCellCallbacks = new ArrayList<>(this.mCellBringupCallbacks);
        this.mCellBringupCallbacks.clear();
        if (!this.mIsQuitting) {
            this.mRouteSelectionCallback = new UnderlyingNetworkListener();
            this.mConnectivityManager.registerNetworkCallback(getRouteSelectionRequest(), this.mRouteSelectionCallback, this.mHandler);
            this.mWifiEntryRssiThresholdCallback = new NetworkBringupCallback();
            this.mConnectivityManager.registerNetworkCallback(getWifiEntryRssiThresholdNetworkRequest(), this.mWifiEntryRssiThresholdCallback, this.mHandler);
            this.mWifiExitRssiThresholdCallback = new NetworkBringupCallback();
            this.mConnectivityManager.registerNetworkCallback(getWifiExitRssiThresholdNetworkRequest(), this.mWifiExitRssiThresholdCallback, this.mHandler);
            this.mWifiBringupCallback = new NetworkBringupCallback();
            this.mConnectivityManager.requestBackgroundNetwork(getWifiNetworkRequest(), this.mWifiBringupCallback, this.mHandler);
            for (Integer num : this.mLastSnapshot.getAllSubIdsInGroup(this.mSubscriptionGroup)) {
                int subId = num.intValue();
                NetworkBringupCallback cb = new NetworkBringupCallback();
                this.mCellBringupCallbacks.add(cb);
                this.mConnectivityManager.requestBackgroundNetwork(getCellNetworkRequestForSubId(subId), cb, this.mHandler);
            }
        } else {
            this.mRouteSelectionCallback = null;
            this.mWifiBringupCallback = null;
            this.mWifiEntryRssiThresholdCallback = null;
            this.mWifiExitRssiThresholdCallback = null;
        }
        if (oldRouteSelectionCallback != null) {
            this.mConnectivityManager.unregisterNetworkCallback(oldRouteSelectionCallback);
        }
        if (oldWifiCallback != null) {
            this.mConnectivityManager.unregisterNetworkCallback(oldWifiCallback);
        }
        if (oldWifiEntryRssiThresholdCallback != null) {
            this.mConnectivityManager.unregisterNetworkCallback(oldWifiEntryRssiThresholdCallback);
        }
        if (oldWifiExitRssiThresholdCallback != null) {
            this.mConnectivityManager.unregisterNetworkCallback(oldWifiExitRssiThresholdCallback);
        }
        for (ConnectivityManager.NetworkCallback cellBringupCallback : oldCellCallbacks) {
            this.mConnectivityManager.unregisterNetworkCallback(cellBringupCallback);
        }
    }

    private NetworkRequest getRouteSelectionRequest() {
        if (this.mVcnContext.isInTestMode()) {
            return getTestNetworkRequest(this.mLastSnapshot.getAllSubIdsInGroup(this.mSubscriptionGroup));
        }
        return getBaseNetworkRequestBuilder().addCapability(16).addCapability(21).setSubscriptionIds(this.mLastSnapshot.getAllSubIdsInGroup(this.mSubscriptionGroup)).build();
    }

    private NetworkRequest getWifiNetworkRequest() {
        return getBaseNetworkRequestBuilder().addTransportType(1).setSubscriptionIds(this.mLastSnapshot.getAllSubIdsInGroup(this.mSubscriptionGroup)).build();
    }

    private NetworkRequest getWifiEntryRssiThresholdNetworkRequest() {
        return getBaseNetworkRequestBuilder().addTransportType(1).setSubscriptionIds(this.mLastSnapshot.getAllSubIdsInGroup(this.mSubscriptionGroup)).setSignalStrength(NetworkPriorityClassifier.getWifiEntryRssiThreshold(this.mCarrierConfig)).build();
    }

    private NetworkRequest getWifiExitRssiThresholdNetworkRequest() {
        return getBaseNetworkRequestBuilder().addTransportType(1).setSubscriptionIds(this.mLastSnapshot.getAllSubIdsInGroup(this.mSubscriptionGroup)).setSignalStrength(NetworkPriorityClassifier.getWifiExitRssiThreshold(this.mCarrierConfig)).build();
    }

    private NetworkRequest getCellNetworkRequestForSubId(int subId) {
        return getBaseNetworkRequestBuilder().addTransportType(0).setNetworkSpecifier(new TelephonyNetworkSpecifier(subId)).build();
    }

    private NetworkRequest.Builder getBaseNetworkRequestBuilder() {
        return new NetworkRequest.Builder().addCapability(12).removeCapability(14).removeCapability(13).removeCapability(28);
    }

    private NetworkRequest getTestNetworkRequest(Set<Integer> subIds) {
        return new NetworkRequest.Builder().clearCapabilities().addTransportType(7).setSubscriptionIds(subIds).build();
    }

    public void updateSubscriptionSnapshot(TelephonySubscriptionTracker.TelephonySubscriptionSnapshot newSnapshot) {
        Objects.requireNonNull(newSnapshot, "Missing newSnapshot");
        TelephonySubscriptionTracker.TelephonySubscriptionSnapshot oldSnapshot = this.mLastSnapshot;
        this.mLastSnapshot = newSnapshot;
        this.mCarrierConfig = newSnapshot.getCarrierConfigForSubGrp(this.mSubscriptionGroup);
        if (oldSnapshot.getAllSubIdsInGroup(this.mSubscriptionGroup).equals(newSnapshot.getAllSubIdsInGroup(this.mSubscriptionGroup))) {
            return;
        }
        registerOrUpdateNetworkRequests();
    }

    public void teardown() {
        this.mVcnContext.ensureRunningOnLooperThread();
        this.mIsQuitting = true;
        registerOrUpdateNetworkRequests();
        ((TelephonyManager) this.mVcnContext.getContext().getSystemService(TelephonyManager.class)).unregisterTelephonyCallback(this.mActiveDataSubIdListener);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reevaluateNetworks() {
        UnderlyingNetworkListener underlyingNetworkListener;
        if (this.mIsQuitting || (underlyingNetworkListener = this.mRouteSelectionCallback) == null) {
            return;
        }
        TreeSet<UnderlyingNetworkRecord> sorted = underlyingNetworkListener.getSortedUnderlyingNetworks();
        UnderlyingNetworkRecord candidate = sorted.isEmpty() ? null : sorted.first();
        if (Objects.equals(this.mCurrentRecord, candidate)) {
            return;
        }
        String allNetworkPriorities = "";
        Iterator<UnderlyingNetworkRecord> it = sorted.iterator();
        while (it.hasNext()) {
            UnderlyingNetworkRecord record = it.next();
            if (!allNetworkPriorities.isEmpty()) {
                allNetworkPriorities = allNetworkPriorities + ", ";
            }
            allNetworkPriorities = allNetworkPriorities + record.network + ": " + record.getPriorityClass();
        }
        logInfo("Selected network changed to " + (candidate != null ? candidate.network : null) + ", selected from list: " + allNetworkPriorities);
        this.mCurrentRecord = candidate;
        this.mCb.onSelectedUnderlyingNetworkChanged(candidate);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class NetworkBringupCallback extends ConnectivityManager.NetworkCallback {
        NetworkBringupCallback() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class UnderlyingNetworkListener extends ConnectivityManager.NetworkCallback {
        private final Map<Network, UnderlyingNetworkRecord.Builder> mUnderlyingNetworkRecordBuilders;

        UnderlyingNetworkListener() {
            super(1);
            this.mUnderlyingNetworkRecordBuilders = new ArrayMap();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public TreeSet<UnderlyingNetworkRecord> getSortedUnderlyingNetworks() {
            TreeSet<UnderlyingNetworkRecord> sorted = new TreeSet<>(UnderlyingNetworkRecord.getComparator(UnderlyingNetworkController.this.mVcnContext, UnderlyingNetworkController.this.mConnectionConfig.getVcnUnderlyingNetworkPriorities(), UnderlyingNetworkController.this.mSubscriptionGroup, UnderlyingNetworkController.this.mLastSnapshot, UnderlyingNetworkController.this.mCurrentRecord, UnderlyingNetworkController.this.mCarrierConfig));
            for (UnderlyingNetworkRecord.Builder builder : this.mUnderlyingNetworkRecordBuilders.values()) {
                if (builder.isValid()) {
                    sorted.add(builder.build());
                }
            }
            return sorted;
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onAvailable(Network network) {
            this.mUnderlyingNetworkRecordBuilders.put(network, new UnderlyingNetworkRecord.Builder(network));
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            this.mUnderlyingNetworkRecordBuilders.remove(network);
            UnderlyingNetworkController.this.reevaluateNetworks();
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            UnderlyingNetworkRecord.Builder builder = this.mUnderlyingNetworkRecordBuilders.get(network);
            if (builder == null) {
                UnderlyingNetworkController.this.logWtf("Got capabilities change for unknown key: " + network);
                return;
            }
            builder.setNetworkCapabilities(networkCapabilities);
            if (builder.isValid()) {
                UnderlyingNetworkController.this.reevaluateNetworks();
            }
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            UnderlyingNetworkRecord.Builder builder = this.mUnderlyingNetworkRecordBuilders.get(network);
            if (builder == null) {
                UnderlyingNetworkController.this.logWtf("Got link properties change for unknown key: " + network);
                return;
            }
            builder.setLinkProperties(linkProperties);
            if (builder.isValid()) {
                UnderlyingNetworkController.this.reevaluateNetworks();
            }
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onBlockedStatusChanged(Network network, boolean isBlocked) {
            UnderlyingNetworkRecord.Builder builder = this.mUnderlyingNetworkRecordBuilders.get(network);
            if (builder == null) {
                UnderlyingNetworkController.this.logWtf("Got blocked status change for unknown key: " + network);
                return;
            }
            builder.setIsBlocked(isBlocked);
            if (builder.isValid()) {
                UnderlyingNetworkController.this.reevaluateNetworks();
            }
        }
    }

    private String getLogPrefix() {
        return "(" + LogUtils.getHashedSubscriptionGroup(this.mSubscriptionGroup) + "-" + this.mConnectionConfig.getGatewayConnectionName() + "-" + System.identityHashCode(this) + ") ";
    }

    private String getTagLogPrefix() {
        return "[ " + TAG + " " + getLogPrefix() + "]";
    }

    private void logInfo(String msg) {
        Slog.i(TAG, getLogPrefix() + msg);
        VcnManagementService.LOCAL_LOG.log("[INFO] " + getTagLogPrefix() + msg);
    }

    private void logInfo(String msg, Throwable tr) {
        Slog.i(TAG, getLogPrefix() + msg, tr);
        VcnManagementService.LOCAL_LOG.log("[INFO] " + getTagLogPrefix() + msg + tr);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logWtf(String msg) {
        String str = TAG;
        Slog.wtf(str, msg);
        VcnManagementService.LOCAL_LOG.log(str + "[WTF ] " + getTagLogPrefix() + msg);
    }

    private void logWtf(String msg, Throwable tr) {
        String str = TAG;
        Slog.wtf(str, msg, tr);
        VcnManagementService.LOCAL_LOG.log(str + "[WTF ] " + getTagLogPrefix() + msg + tr);
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("UnderlyingNetworkController:");
        pw.increaseIndent();
        pw.println("Carrier WiFi Entry Threshold: " + NetworkPriorityClassifier.getWifiEntryRssiThreshold(this.mCarrierConfig));
        pw.println("Carrier WiFi Exit Threshold: " + NetworkPriorityClassifier.getWifiExitRssiThreshold(this.mCarrierConfig));
        StringBuilder append = new StringBuilder().append("Currently selected: ");
        UnderlyingNetworkRecord underlyingNetworkRecord = this.mCurrentRecord;
        pw.println(append.append(underlyingNetworkRecord == null ? null : underlyingNetworkRecord.network).toString());
        pw.println("VcnUnderlyingNetworkTemplate list:");
        pw.increaseIndent();
        int index = 0;
        for (VcnUnderlyingNetworkTemplate priority : this.mConnectionConfig.getVcnUnderlyingNetworkPriorities()) {
            pw.println("Priority index: " + index);
            priority.dump(pw);
            index++;
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("Underlying networks:");
        pw.increaseIndent();
        UnderlyingNetworkListener underlyingNetworkListener = this.mRouteSelectionCallback;
        if (underlyingNetworkListener != null) {
            Iterator it = underlyingNetworkListener.getSortedUnderlyingNetworks().iterator();
            while (it.hasNext()) {
                UnderlyingNetworkRecord record = (UnderlyingNetworkRecord) it.next();
                record.dump(this.mVcnContext, pw, this.mConnectionConfig.getVcnUnderlyingNetworkPriorities(), this.mSubscriptionGroup, this.mLastSnapshot, this.mCurrentRecord, this.mCarrierConfig);
            }
        }
        pw.decreaseIndent();
        pw.println();
        pw.decreaseIndent();
    }

    /* loaded from: classes2.dex */
    private class VcnActiveDataSubscriptionIdListener extends TelephonyCallback implements TelephonyCallback.ActiveDataSubscriptionIdListener {
        private VcnActiveDataSubscriptionIdListener() {
        }

        @Override // android.telephony.TelephonyCallback.ActiveDataSubscriptionIdListener
        public void onActiveDataSubscriptionIdChanged(int subId) {
            UnderlyingNetworkController.this.reevaluateNetworks();
        }
    }

    /* loaded from: classes2.dex */
    private static class Dependencies {
        private Dependencies() {
        }
    }
}
