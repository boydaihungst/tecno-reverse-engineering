package com.android.server.vcn.routeselection;

import android.net.NetworkCapabilities;
import android.net.TelephonyNetworkSpecifier;
import android.net.vcn.VcnCellUnderlyingNetworkTemplate;
import android.net.vcn.VcnUnderlyingNetworkTemplate;
import android.net.vcn.VcnWifiUnderlyingNetworkTemplate;
import android.os.ParcelUuid;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.server.VcnManagementService;
import com.android.server.vcn.TelephonySubscriptionTracker;
import com.android.server.vcn.VcnContext;
import com.android.server.vcn.util.PersistableBundleUtils;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class NetworkPriorityClassifier {
    static final int PRIORITY_ANY = Integer.MAX_VALUE;
    private static final String TAG = NetworkPriorityClassifier.class.getSimpleName();
    static final int WIFI_ENTRY_RSSI_THRESHOLD_DEFAULT = -70;
    static final int WIFI_EXIT_RSSI_THRESHOLD_DEFAULT = -74;

    NetworkPriorityClassifier() {
    }

    public static int calculatePriorityClass(VcnContext vcnContext, UnderlyingNetworkRecord networkRecord, List<VcnUnderlyingNetworkTemplate> underlyingNetworkTemplates, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, UnderlyingNetworkRecord currentlySelected, PersistableBundleUtils.PersistableBundleWrapper carrierConfig) {
        if (networkRecord.isBlocked) {
            logWtf("Network blocked for System Server: " + networkRecord.network);
            return Integer.MAX_VALUE;
        } else if (snapshot == null) {
            logWtf("Got null snapshot");
            return Integer.MAX_VALUE;
        } else {
            int priorityIndex = 0;
            for (VcnUnderlyingNetworkTemplate nwPriority : underlyingNetworkTemplates) {
                if (checkMatchesPriorityRule(vcnContext, nwPriority, networkRecord, subscriptionGroup, snapshot, currentlySelected, carrierConfig)) {
                    return priorityIndex;
                }
                priorityIndex++;
            }
            return Integer.MAX_VALUE;
        }
    }

    public static boolean checkMatchesPriorityRule(VcnContext vcnContext, VcnUnderlyingNetworkTemplate networkPriority, UnderlyingNetworkRecord networkRecord, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, UnderlyingNetworkRecord currentlySelected, PersistableBundleUtils.PersistableBundleWrapper carrierConfig) {
        NetworkCapabilities caps = networkRecord.networkCapabilities;
        boolean isSelectedUnderlyingNetwork = currentlySelected != null && Objects.equals(currentlySelected.network, networkRecord.network);
        int meteredMatch = networkPriority.getMetered();
        boolean isMetered = !caps.hasCapability(11);
        if ((meteredMatch != 1 || isMetered) && (!(meteredMatch == 2 && isMetered) && caps.getLinkUpstreamBandwidthKbps() >= networkPriority.getMinExitUpstreamBandwidthKbps() && ((caps.getLinkUpstreamBandwidthKbps() >= networkPriority.getMinEntryUpstreamBandwidthKbps() || isSelectedUnderlyingNetwork) && caps.getLinkDownstreamBandwidthKbps() >= networkPriority.getMinExitDownstreamBandwidthKbps() && (caps.getLinkDownstreamBandwidthKbps() >= networkPriority.getMinEntryDownstreamBandwidthKbps() || isSelectedUnderlyingNetwork)))) {
            if (vcnContext.isInTestMode() && caps.hasTransport(7)) {
                return true;
            }
            if (networkPriority instanceof VcnWifiUnderlyingNetworkTemplate) {
                return checkMatchesWifiPriorityRule((VcnWifiUnderlyingNetworkTemplate) networkPriority, networkRecord, currentlySelected, carrierConfig);
            }
            if (networkPriority instanceof VcnCellUnderlyingNetworkTemplate) {
                return checkMatchesCellPriorityRule(vcnContext, (VcnCellUnderlyingNetworkTemplate) networkPriority, networkRecord, subscriptionGroup, snapshot);
            }
            logWtf("Got unknown VcnUnderlyingNetworkTemplate class: " + networkPriority.getClass().getSimpleName());
            return false;
        }
        return false;
    }

    public static boolean checkMatchesWifiPriorityRule(VcnWifiUnderlyingNetworkTemplate networkPriority, UnderlyingNetworkRecord networkRecord, UnderlyingNetworkRecord currentlySelected, PersistableBundleUtils.PersistableBundleWrapper carrierConfig) {
        NetworkCapabilities caps = networkRecord.networkCapabilities;
        if (caps.hasTransport(1) && isWifiRssiAcceptable(networkRecord, currentlySelected, carrierConfig)) {
            return networkPriority.getSsids().isEmpty() || networkPriority.getSsids().contains(caps.getSsid());
        }
        return false;
    }

    private static boolean isWifiRssiAcceptable(UnderlyingNetworkRecord networkRecord, UnderlyingNetworkRecord currentlySelected, PersistableBundleUtils.PersistableBundleWrapper carrierConfig) {
        NetworkCapabilities caps = networkRecord.networkCapabilities;
        boolean isSelectedNetwork = currentlySelected != null && networkRecord.network.equals(currentlySelected.network);
        return (isSelectedNetwork && caps.getSignalStrength() >= getWifiExitRssiThreshold(carrierConfig)) || caps.getSignalStrength() >= getWifiEntryRssiThreshold(carrierConfig);
    }

    public static boolean checkMatchesCellPriorityRule(VcnContext vcnContext, VcnCellUnderlyingNetworkTemplate networkPriority, UnderlyingNetworkRecord networkRecord, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot) {
        NetworkCapabilities caps = networkRecord.networkCapabilities;
        if (!caps.hasTransport(0)) {
            return false;
        }
        TelephonyNetworkSpecifier telephonyNetworkSpecifier = (TelephonyNetworkSpecifier) caps.getNetworkSpecifier();
        if (telephonyNetworkSpecifier == null) {
            logWtf("Got null NetworkSpecifier");
            return false;
        }
        int subId = telephonyNetworkSpecifier.getSubscriptionId();
        TelephonyManager subIdSpecificTelephonyMgr = ((TelephonyManager) vcnContext.getContext().getSystemService(TelephonyManager.class)).createForSubscriptionId(subId);
        if (!networkPriority.getOperatorPlmnIds().isEmpty()) {
            String plmnId = subIdSpecificTelephonyMgr.getNetworkOperator();
            if (!networkPriority.getOperatorPlmnIds().contains(plmnId)) {
                return false;
            }
        }
        if (!networkPriority.getSimSpecificCarrierIds().isEmpty()) {
            int carrierId = subIdSpecificTelephonyMgr.getSimSpecificCarrierId();
            if (!networkPriority.getSimSpecificCarrierIds().contains(Integer.valueOf(carrierId))) {
                return false;
            }
        }
        int roamingMatch = networkPriority.getRoaming();
        boolean isRoaming = !caps.hasCapability(18);
        if ((roamingMatch == 1 && !isRoaming) || (roamingMatch == 2 && isRoaming)) {
            return false;
        }
        int opportunisticMatch = networkPriority.getOpportunistic();
        boolean isOpportunistic = isOpportunistic(snapshot, caps.getSubscriptionIds());
        if (opportunisticMatch == 1) {
            if (!isOpportunistic) {
                return false;
            }
            if (snapshot.getAllSubIdsInGroup(subscriptionGroup).contains(Integer.valueOf(SubscriptionManager.getActiveDataSubscriptionId())) && !caps.getSubscriptionIds().contains(Integer.valueOf(SubscriptionManager.getActiveDataSubscriptionId()))) {
                return false;
            }
        } else if (opportunisticMatch == 2 && !isOpportunistic) {
            return false;
        }
        return true;
    }

    static boolean isOpportunistic(TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, Set<Integer> subIds) {
        if (snapshot == null) {
            logWtf("Got null snapshot");
            return false;
        }
        for (Integer num : subIds) {
            int subId = num.intValue();
            if (snapshot.isOpportunistic(subId)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getWifiEntryRssiThreshold(PersistableBundleUtils.PersistableBundleWrapper carrierConfig) {
        return carrierConfig != null ? carrierConfig.getInt("vcn_network_selection_wifi_entry_rssi_threshold", WIFI_ENTRY_RSSI_THRESHOLD_DEFAULT) : WIFI_ENTRY_RSSI_THRESHOLD_DEFAULT;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getWifiExitRssiThreshold(PersistableBundleUtils.PersistableBundleWrapper carrierConfig) {
        return carrierConfig != null ? carrierConfig.getInt("vcn_network_selection_wifi_exit_rssi_threshold", WIFI_EXIT_RSSI_THRESHOLD_DEFAULT) : WIFI_EXIT_RSSI_THRESHOLD_DEFAULT;
    }

    private static void logWtf(String msg) {
        String str = TAG;
        Slog.wtf(str, msg);
        VcnManagementService.LOCAL_LOG.log(str + " WTF: " + msg);
    }
}
