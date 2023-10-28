package com.android.server.vcn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.vcn.VcnManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.ParcelUuid;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.vcn.util.PersistableBundleUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
/* loaded from: classes2.dex */
public class TelephonySubscriptionTracker extends BroadcastReceiver {
    private static final boolean LOG_DBG = false;
    private static final String TAG = TelephonySubscriptionTracker.class.getSimpleName();
    private final ActiveDataSubscriptionIdListener mActiveDataSubIdListener;
    private final TelephonySubscriptionTrackerCallback mCallback;
    private final CarrierConfigManager mCarrierConfigManager;
    private final List<TelephonyManager.CarrierPrivilegesCallback> mCarrierPrivilegesCallbacks;
    private final Context mContext;
    private TelephonySubscriptionSnapshot mCurrentSnapshot;
    private final Dependencies mDeps;
    private final Handler mHandler;
    private final Map<Integer, Integer> mReadySubIdsBySlotId;
    private final Map<Integer, PersistableBundleUtils.PersistableBundleWrapper> mSubIdToCarrierConfigMap;
    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionChangedListener;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;

    /* loaded from: classes2.dex */
    public interface TelephonySubscriptionTrackerCallback {
        void onNewSnapshot(TelephonySubscriptionSnapshot telephonySubscriptionSnapshot);
    }

    public TelephonySubscriptionTracker(Context context, Handler handler, TelephonySubscriptionTrackerCallback callback) {
        this(context, handler, callback, new Dependencies());
    }

    TelephonySubscriptionTracker(Context context, Handler handler, TelephonySubscriptionTrackerCallback callback, Dependencies deps) {
        this.mReadySubIdsBySlotId = new HashMap();
        this.mSubIdToCarrierConfigMap = new HashMap();
        this.mCarrierPrivilegesCallbacks = new ArrayList();
        Context context2 = (Context) Objects.requireNonNull(context, "Missing context");
        this.mContext = context2;
        this.mHandler = (Handler) Objects.requireNonNull(handler, "Missing handler");
        this.mCallback = (TelephonySubscriptionTrackerCallback) Objects.requireNonNull(callback, "Missing callback");
        this.mDeps = (Dependencies) Objects.requireNonNull(deps, "Missing deps");
        this.mTelephonyManager = (TelephonyManager) context2.getSystemService(TelephonyManager.class);
        this.mSubscriptionManager = (SubscriptionManager) context2.getSystemService(SubscriptionManager.class);
        this.mCarrierConfigManager = (CarrierConfigManager) context2.getSystemService(CarrierConfigManager.class);
        this.mActiveDataSubIdListener = new ActiveDataSubscriptionIdListener();
        this.mSubscriptionChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() { // from class: com.android.server.vcn.TelephonySubscriptionTracker.1
            @Override // android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
            public void onSubscriptionsChanged() {
                TelephonySubscriptionTracker.this.handleSubscriptionsChanged();
            }
        };
    }

    public void register() {
        Executor handlerExecutor = new HandlerExecutor(this.mHandler);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        filter.addAction("android.telephony.action.MULTI_SIM_CONFIG_CHANGED");
        this.mContext.registerReceiver(this, filter, null, this.mHandler);
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(handlerExecutor, this.mSubscriptionChangedListener);
        this.mTelephonyManager.registerTelephonyCallback(handlerExecutor, this.mActiveDataSubIdListener);
        registerCarrierPrivilegesCallbacks();
    }

    private void registerCarrierPrivilegesCallbacks() {
        Executor handlerExecutor = new HandlerExecutor(this.mHandler);
        int modemCount = this.mTelephonyManager.getActiveModemCount();
        for (int i = 0; i < modemCount; i++) {
            try {
                TelephonyManager.CarrierPrivilegesCallback carrierPrivilegesCallback = new TelephonyManager.CarrierPrivilegesCallback() { // from class: com.android.server.vcn.TelephonySubscriptionTracker.2
                    public void onCarrierPrivilegesChanged(Set<String> privilegedPackageNames, Set<Integer> privilegedUids) {
                        TelephonySubscriptionTracker.this.handleSubscriptionsChanged();
                    }
                };
                this.mTelephonyManager.registerCarrierPrivilegesCallback(i, handlerExecutor, carrierPrivilegesCallback);
                this.mCarrierPrivilegesCallbacks.add(carrierPrivilegesCallback);
            } catch (IllegalArgumentException e) {
                Slog.wtf(TAG, "Encounted exception registering carrier privileges listeners", e);
                return;
            }
        }
    }

    public void unregister() {
        this.mContext.unregisterReceiver(this);
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mSubscriptionChangedListener);
        this.mTelephonyManager.unregisterTelephonyCallback(this.mActiveDataSubIdListener);
        unregisterCarrierPrivilegesCallbacks();
    }

    private void unregisterCarrierPrivilegesCallbacks() {
        for (TelephonyManager.CarrierPrivilegesCallback carrierPrivilegesCallback : this.mCarrierPrivilegesCallbacks) {
            this.mTelephonyManager.unregisterCarrierPrivilegesCallback(carrierPrivilegesCallback);
        }
        this.mCarrierPrivilegesCallbacks.clear();
    }

    public void handleSubscriptionsChanged() {
        Map<ParcelUuid, Set<String>> privilegedPackages = new HashMap<>();
        Map<Integer, SubscriptionInfo> newSubIdToInfoMap = new HashMap<>();
        List<SubscriptionInfo> allSubs = this.mSubscriptionManager.getAllSubscriptionInfoList();
        if (allSubs == null) {
            return;
        }
        for (SubscriptionInfo subInfo : allSubs) {
            if (subInfo.getGroupUuid() != null) {
                newSubIdToInfoMap.put(Integer.valueOf(subInfo.getSubscriptionId()), subInfo);
                if (subInfo.getSimSlotIndex() != -1 && this.mReadySubIdsBySlotId.values().contains(Integer.valueOf(subInfo.getSubscriptionId()))) {
                    TelephonyManager subIdSpecificTelephonyManager = this.mTelephonyManager.createForSubscriptionId(subInfo.getSubscriptionId());
                    ParcelUuid subGroup = subInfo.getGroupUuid();
                    Set<String> pkgs = privilegedPackages.getOrDefault(subGroup, new ArraySet<>());
                    pkgs.addAll(subIdSpecificTelephonyManager.getPackagesWithCarrierPrivileges());
                    privilegedPackages.put(subGroup, pkgs);
                }
            }
        }
        final TelephonySubscriptionSnapshot newSnapshot = new TelephonySubscriptionSnapshot(this.mDeps.getActiveDataSubscriptionId(), newSubIdToInfoMap, this.mSubIdToCarrierConfigMap, privilegedPackages);
        if (!newSnapshot.equals(this.mCurrentSnapshot)) {
            this.mCurrentSnapshot = newSnapshot;
            this.mHandler.post(new Runnable() { // from class: com.android.server.vcn.TelephonySubscriptionTracker$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TelephonySubscriptionTracker.this.m7455x45ce9a(newSnapshot);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$handleSubscriptionsChanged$0$com-android-server-vcn-TelephonySubscriptionTracker  reason: not valid java name */
    public /* synthetic */ void m7455x45ce9a(TelephonySubscriptionSnapshot newSnapshot) {
        this.mCallback.onNewSnapshot(newSnapshot);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        char c;
        String action = intent.getAction();
        switch (action.hashCode()) {
            case -1138588223:
                if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1093296680:
                if (action.equals("android.telephony.action.MULTI_SIM_CONFIG_CHANGED")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                handleActionCarrierConfigChanged(context, intent);
                return;
            case 1:
                handleActionMultiSimConfigChanged(context, intent);
                return;
            default:
                Slog.v(TAG, "Unknown intent received with action: " + intent.getAction());
                return;
        }
    }

    private void handleActionMultiSimConfigChanged(Context context, Intent intent) {
        unregisterCarrierPrivilegesCallbacks();
        int modemCount = this.mTelephonyManager.getActiveModemCount();
        Iterator<Integer> slotIdIterator = this.mReadySubIdsBySlotId.keySet().iterator();
        while (slotIdIterator.hasNext()) {
            int slotId = slotIdIterator.next().intValue();
            if (slotId >= modemCount) {
                slotIdIterator.remove();
            }
        }
        registerCarrierPrivilegesCallbacks();
        handleSubscriptionsChanged();
    }

    private void handleActionCarrierConfigChanged(Context context, Intent intent) {
        int subId = intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1);
        int slotId = intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1);
        if (slotId == -1) {
            return;
        }
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            PersistableBundle carrierConfig = this.mCarrierConfigManager.getConfigForSubId(subId);
            if (this.mDeps.isConfigForIdentifiedCarrier(carrierConfig)) {
                this.mReadySubIdsBySlotId.put(Integer.valueOf(slotId), Integer.valueOf(subId));
                PersistableBundle minimized = PersistableBundleUtils.minimizeBundle(carrierConfig, VcnManager.VCN_RELATED_CARRIER_CONFIG_KEYS);
                if (minimized != null) {
                    this.mSubIdToCarrierConfigMap.put(Integer.valueOf(subId), new PersistableBundleUtils.PersistableBundleWrapper(minimized));
                }
                handleSubscriptionsChanged();
                return;
            }
            return;
        }
        Integer oldSubid = this.mReadySubIdsBySlotId.remove(Integer.valueOf(slotId));
        if (oldSubid != null) {
            this.mSubIdToCarrierConfigMap.remove(oldSubid);
        }
        handleSubscriptionsChanged();
    }

    void setReadySubIdsBySlotId(Map<Integer, Integer> readySubIdsBySlotId) {
        this.mReadySubIdsBySlotId.clear();
        this.mReadySubIdsBySlotId.putAll(readySubIdsBySlotId);
    }

    void setSubIdToCarrierConfigMap(Map<Integer, PersistableBundleUtils.PersistableBundleWrapper> subIdToCarrierConfigMap) {
        this.mSubIdToCarrierConfigMap.clear();
        this.mSubIdToCarrierConfigMap.putAll(subIdToCarrierConfigMap);
    }

    Map<Integer, Integer> getReadySubIdsBySlotId() {
        return Collections.unmodifiableMap(this.mReadySubIdsBySlotId);
    }

    Map<Integer, PersistableBundleUtils.PersistableBundleWrapper> getSubIdToCarrierConfigMap() {
        return Collections.unmodifiableMap(this.mSubIdToCarrierConfigMap);
    }

    /* loaded from: classes2.dex */
    public static class TelephonySubscriptionSnapshot {
        public static final TelephonySubscriptionSnapshot EMPTY_SNAPSHOT = new TelephonySubscriptionSnapshot(-1, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        private final int mActiveDataSubId;
        private final Map<ParcelUuid, Set<String>> mPrivilegedPackages;
        private final Map<Integer, PersistableBundleUtils.PersistableBundleWrapper> mSubIdToCarrierConfigMap;
        private final Map<Integer, SubscriptionInfo> mSubIdToInfoMap;

        TelephonySubscriptionSnapshot(int activeDataSubId, Map<Integer, SubscriptionInfo> subIdToInfoMap, Map<Integer, PersistableBundleUtils.PersistableBundleWrapper> subIdToCarrierConfigMap, Map<ParcelUuid, Set<String>> privilegedPackages) {
            this.mActiveDataSubId = activeDataSubId;
            Objects.requireNonNull(subIdToInfoMap, "subIdToInfoMap was null");
            Objects.requireNonNull(privilegedPackages, "privilegedPackages was null");
            Objects.requireNonNull(subIdToCarrierConfigMap, "subIdToCarrierConfigMap was null");
            this.mSubIdToInfoMap = Collections.unmodifiableMap(subIdToInfoMap);
            this.mSubIdToCarrierConfigMap = Collections.unmodifiableMap(subIdToCarrierConfigMap);
            Map<ParcelUuid, Set<String>> unmodifiableInnerSets = new ArrayMap<>();
            for (Map.Entry<ParcelUuid, Set<String>> entry : privilegedPackages.entrySet()) {
                unmodifiableInnerSets.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
            }
            this.mPrivilegedPackages = Collections.unmodifiableMap(unmodifiableInnerSets);
        }

        public int getActiveDataSubscriptionId() {
            return this.mActiveDataSubId;
        }

        public ParcelUuid getActiveDataSubscriptionGroup() {
            SubscriptionInfo info = this.mSubIdToInfoMap.get(Integer.valueOf(getActiveDataSubscriptionId()));
            if (info == null) {
                return null;
            }
            return info.getGroupUuid();
        }

        public Set<ParcelUuid> getActiveSubscriptionGroups() {
            return this.mPrivilegedPackages.keySet();
        }

        public boolean packageHasPermissionsForSubscriptionGroup(ParcelUuid subGrp, String packageName) {
            Set<String> privilegedPackages = this.mPrivilegedPackages.get(subGrp);
            return privilegedPackages != null && privilegedPackages.contains(packageName);
        }

        public ParcelUuid getGroupForSubId(int subId) {
            if (this.mSubIdToInfoMap.containsKey(Integer.valueOf(subId))) {
                return this.mSubIdToInfoMap.get(Integer.valueOf(subId)).getGroupUuid();
            }
            return null;
        }

        public Set<Integer> getAllSubIdsInGroup(ParcelUuid subGrp) {
            Set<Integer> subIds = new ArraySet<>();
            for (Map.Entry<Integer, SubscriptionInfo> entry : this.mSubIdToInfoMap.entrySet()) {
                if (subGrp.equals(entry.getValue().getGroupUuid())) {
                    subIds.add(entry.getKey());
                }
            }
            return subIds;
        }

        public boolean isOpportunistic(int subId) {
            if (this.mSubIdToInfoMap.containsKey(Integer.valueOf(subId))) {
                return this.mSubIdToInfoMap.get(Integer.valueOf(subId)).isOpportunistic();
            }
            return false;
        }

        public PersistableBundleUtils.PersistableBundleWrapper getCarrierConfigForSubGrp(ParcelUuid subGrp) {
            PersistableBundleUtils.PersistableBundleWrapper result = null;
            for (Integer num : getAllSubIdsInGroup(subGrp)) {
                int subId = num.intValue();
                PersistableBundleUtils.PersistableBundleWrapper config = this.mSubIdToCarrierConfigMap.get(Integer.valueOf(subId));
                if (config != null) {
                    result = config;
                    if (!isOpportunistic(subId)) {
                        return config;
                    }
                }
            }
            return result;
        }

        public int hashCode() {
            return Objects.hash(Integer.valueOf(this.mActiveDataSubId), this.mSubIdToInfoMap, this.mSubIdToCarrierConfigMap, this.mPrivilegedPackages);
        }

        public boolean equals(Object obj) {
            if (obj instanceof TelephonySubscriptionSnapshot) {
                TelephonySubscriptionSnapshot other = (TelephonySubscriptionSnapshot) obj;
                return this.mActiveDataSubId == other.mActiveDataSubId && this.mSubIdToInfoMap.equals(other.mSubIdToInfoMap) && this.mSubIdToCarrierConfigMap.equals(other.mSubIdToCarrierConfigMap) && this.mPrivilegedPackages.equals(other.mPrivilegedPackages);
            }
            return false;
        }

        public void dump(IndentingPrintWriter pw) {
            pw.println("TelephonySubscriptionSnapshot:");
            pw.increaseIndent();
            pw.println("mActiveDataSubId: " + this.mActiveDataSubId);
            pw.println("mSubIdToInfoMap: " + this.mSubIdToInfoMap);
            pw.println("mSubIdToCarrierConfigMap: " + this.mSubIdToCarrierConfigMap);
            pw.println("mPrivilegedPackages: " + this.mPrivilegedPackages);
            pw.decreaseIndent();
        }

        public String toString() {
            return "TelephonySubscriptionSnapshot{ mActiveDataSubId=" + this.mActiveDataSubId + ", mSubIdToInfoMap=" + this.mSubIdToInfoMap + ", mSubIdToCarrierConfigMap=" + this.mSubIdToCarrierConfigMap + ", mPrivilegedPackages=" + this.mPrivilegedPackages + " }";
        }
    }

    /* loaded from: classes2.dex */
    private class ActiveDataSubscriptionIdListener extends TelephonyCallback implements TelephonyCallback.ActiveDataSubscriptionIdListener {
        private ActiveDataSubscriptionIdListener() {
        }

        @Override // android.telephony.TelephonyCallback.ActiveDataSubscriptionIdListener
        public void onActiveDataSubscriptionIdChanged(int subId) {
            TelephonySubscriptionTracker.this.handleSubscriptionsChanged();
        }
    }

    /* loaded from: classes2.dex */
    public static class Dependencies {
        public boolean isConfigForIdentifiedCarrier(PersistableBundle bundle) {
            return CarrierConfigManager.isConfigForIdentifiedCarrier(bundle);
        }

        public int getActiveDataSubscriptionId() {
            return SubscriptionManager.getActiveDataSubscriptionId();
        }
    }
}
