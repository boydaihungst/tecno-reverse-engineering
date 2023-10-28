package com.android.server.vcn;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkScore;
import android.net.Uri;
import android.net.vcn.VcnConfig;
import android.net.vcn.VcnGatewayConnectionConfig;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Message;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.VcnManagementService;
import com.android.server.timezonedetector.ServiceConfigAccessor;
import com.android.server.vcn.TelephonySubscriptionTracker;
import com.android.server.vcn.VcnNetworkProvider;
import com.android.server.vcn.util.LogUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
/* loaded from: classes2.dex */
public class Vcn extends Handler {
    private static final int MSG_CMD_BASE = 100;
    private static final int MSG_CMD_TEARDOWN = 100;
    private static final int MSG_EVENT_BASE = 0;
    private static final int MSG_EVENT_CONFIG_UPDATED = 0;
    private static final int MSG_EVENT_GATEWAY_CONNECTION_QUIT = 3;
    private static final int MSG_EVENT_MOBILE_DATA_TOGGLED = 5;
    private static final int MSG_EVENT_NETWORK_REQUESTED = 1;
    private static final int MSG_EVENT_SAFE_MODE_STATE_CHANGED = 4;
    private static final int MSG_EVENT_SUBSCRIPTIONS_CHANGED = 2;
    private static final int VCN_LEGACY_SCORE_INT = 52;
    private VcnConfig mConfig;
    private final VcnContentResolver mContentResolver;
    private volatile int mCurrentStatus;
    private final Dependencies mDeps;
    private boolean mIsMobileDataEnabled;
    private TelephonySubscriptionTracker.TelephonySubscriptionSnapshot mLastSnapshot;
    private final ContentObserver mMobileDataSettingsObserver;
    private final Map<Integer, VcnUserMobileDataStateListener> mMobileDataStateListeners;
    private final VcnNetworkRequestListener mRequestListener;
    private final ParcelUuid mSubscriptionGroup;
    private final VcnManagementService.VcnCallback mVcnCallback;
    private final VcnContext mVcnContext;
    private final Map<VcnGatewayConnectionConfig, VcnGatewayConnection> mVcnGatewayConnections;
    private static final String TAG = Vcn.class.getSimpleName();
    private static final List<Integer> CAPS_REQUIRING_MOBILE_DATA = Arrays.asList(12, 2);

    /* loaded from: classes2.dex */
    public interface VcnGatewayStatusCallback {
        void onGatewayConnectionError(String str, int i, String str2, String str3);

        void onQuit();

        void onSafeModeStatusChanged();
    }

    public Vcn(VcnContext vcnContext, ParcelUuid subscriptionGroup, VcnConfig config, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, VcnManagementService.VcnCallback vcnCallback) {
        this(vcnContext, subscriptionGroup, config, snapshot, vcnCallback, new Dependencies());
    }

    public Vcn(VcnContext vcnContext, ParcelUuid subscriptionGroup, VcnConfig config, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, VcnManagementService.VcnCallback vcnCallback, Dependencies deps) {
        super(((VcnContext) Objects.requireNonNull(vcnContext, "Missing vcnContext")).getLooper());
        this.mMobileDataStateListeners = new ArrayMap();
        this.mVcnGatewayConnections = new HashMap();
        this.mCurrentStatus = 2;
        this.mIsMobileDataEnabled = false;
        this.mVcnContext = vcnContext;
        this.mSubscriptionGroup = (ParcelUuid) Objects.requireNonNull(subscriptionGroup, "Missing subscriptionGroup");
        this.mVcnCallback = (VcnManagementService.VcnCallback) Objects.requireNonNull(vcnCallback, "Missing vcnCallback");
        Dependencies dependencies = (Dependencies) Objects.requireNonNull(deps, "Missing deps");
        this.mDeps = dependencies;
        VcnNetworkRequestListener vcnNetworkRequestListener = new VcnNetworkRequestListener();
        this.mRequestListener = vcnNetworkRequestListener;
        VcnContentResolver newVcnContentResolver = dependencies.newVcnContentResolver(vcnContext);
        this.mContentResolver = newVcnContentResolver;
        VcnMobileDataContentObserver vcnMobileDataContentObserver = new VcnMobileDataContentObserver(this);
        this.mMobileDataSettingsObserver = vcnMobileDataContentObserver;
        Uri uri = Settings.Global.getUriFor("mobile_data");
        newVcnContentResolver.registerContentObserver(uri, true, vcnMobileDataContentObserver);
        this.mConfig = (VcnConfig) Objects.requireNonNull(config, "Missing config");
        this.mLastSnapshot = (TelephonySubscriptionTracker.TelephonySubscriptionSnapshot) Objects.requireNonNull(snapshot, "Missing snapshot");
        this.mIsMobileDataEnabled = getMobileDataStatus();
        updateMobileDataStateListeners();
        vcnContext.getVcnNetworkProvider().registerListener(vcnNetworkRequestListener);
    }

    public void updateConfig(VcnConfig config) {
        Objects.requireNonNull(config, "Missing config");
        sendMessage(obtainMessage(0, config));
    }

    public void updateSubscriptionSnapshot(TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Missing snapshot");
        sendMessage(obtainMessage(2, snapshot));
    }

    public void teardownAsynchronously() {
        sendMessageAtFrontOfQueue(obtainMessage(100));
    }

    public int getStatus() {
        return this.mCurrentStatus;
    }

    public void setStatus(int status) {
        this.mCurrentStatus = status;
    }

    public Set<VcnGatewayConnection> getVcnGatewayConnections() {
        return Collections.unmodifiableSet(new HashSet(this.mVcnGatewayConnections.values()));
    }

    public Map<VcnGatewayConnectionConfig, VcnGatewayConnection> getVcnGatewayConnectionConfigMap() {
        return Collections.unmodifiableMap(new HashMap(this.mVcnGatewayConnections));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class VcnNetworkRequestListener implements VcnNetworkProvider.NetworkRequestListener {
        private VcnNetworkRequestListener() {
        }

        @Override // com.android.server.vcn.VcnNetworkProvider.NetworkRequestListener
        public void onNetworkRequested(NetworkRequest request) {
            Objects.requireNonNull(request, "Missing request");
            Vcn vcn = Vcn.this;
            vcn.sendMessage(vcn.obtainMessage(1, request));
        }
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        if (this.mCurrentStatus != 2 && this.mCurrentStatus != 3) {
            return;
        }
        switch (msg.what) {
            case 0:
                handleConfigUpdated((VcnConfig) msg.obj);
                return;
            case 1:
                handleNetworkRequested((NetworkRequest) msg.obj);
                return;
            case 2:
                handleSubscriptionsChanged((TelephonySubscriptionTracker.TelephonySubscriptionSnapshot) msg.obj);
                return;
            case 3:
                handleGatewayConnectionQuit((VcnGatewayConnectionConfig) msg.obj);
                return;
            case 4:
                handleSafeModeStatusChanged();
                return;
            case 5:
                handleMobileDataToggled();
                return;
            case 100:
                handleTeardown();
                return;
            default:
                logWtf("Unknown msg.what: " + msg.what);
                return;
        }
    }

    private void handleConfigUpdated(VcnConfig config) {
        logDbg("Config updated: old = " + this.mConfig.hashCode() + "; new = " + config.hashCode());
        this.mConfig = config;
        for (Map.Entry<VcnGatewayConnectionConfig, VcnGatewayConnection> entry : this.mVcnGatewayConnections.entrySet()) {
            VcnGatewayConnectionConfig gatewayConnectionConfig = entry.getKey();
            VcnGatewayConnection gatewayConnection = entry.getValue();
            if (!this.mConfig.getGatewayConnectionConfigs().contains(gatewayConnectionConfig)) {
                if (gatewayConnection == null) {
                    logWtf("Found gatewayConnectionConfig without GatewayConnection");
                } else {
                    logInfo("Config updated, restarting gateway " + gatewayConnection.getLogPrefix());
                    gatewayConnection.teardownAsynchronously();
                }
            }
        }
        this.mVcnContext.getVcnNetworkProvider().resendAllRequests(this.mRequestListener);
    }

    private void handleTeardown() {
        logDbg("Tearing down");
        this.mVcnContext.getVcnNetworkProvider().unregisterListener(this.mRequestListener);
        for (VcnGatewayConnection gatewayConnection : this.mVcnGatewayConnections.values()) {
            gatewayConnection.teardownAsynchronously();
        }
        for (VcnUserMobileDataStateListener listener : this.mMobileDataStateListeners.values()) {
            getTelephonyManager().unregisterTelephonyCallback(listener);
        }
        this.mMobileDataStateListeners.clear();
        this.mCurrentStatus = 1;
    }

    private void handleSafeModeStatusChanged() {
        logVdbg("VcnGatewayConnection safe mode status changed");
        boolean hasSafeModeGatewayConnection = false;
        Iterator<VcnGatewayConnection> it = this.mVcnGatewayConnections.values().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            VcnGatewayConnection gatewayConnection = it.next();
            if (gatewayConnection.isInSafeMode()) {
                hasSafeModeGatewayConnection = true;
                break;
            }
        }
        int oldStatus = this.mCurrentStatus;
        this.mCurrentStatus = hasSafeModeGatewayConnection ? 3 : 2;
        if (oldStatus != this.mCurrentStatus) {
            this.mVcnCallback.onSafeModeStatusChanged(hasSafeModeGatewayConnection);
            logInfo("Safe mode " + (this.mCurrentStatus == 3 ? "entered" : "exited"));
        }
    }

    private void handleNetworkRequested(NetworkRequest request) {
        logVdbg("Received request " + request);
        for (VcnGatewayConnectionConfig gatewayConnectionConfig : this.mVcnGatewayConnections.keySet()) {
            if (isRequestSatisfiedByGatewayConnectionConfig(request, gatewayConnectionConfig)) {
                logVdbg("Request already satisfied by existing VcnGatewayConnection: " + request);
                return;
            }
        }
        for (VcnGatewayConnectionConfig gatewayConnectionConfig2 : this.mConfig.getGatewayConnectionConfigs()) {
            if (isRequestSatisfiedByGatewayConnectionConfig(request, gatewayConnectionConfig2) && !getExposedCapabilitiesForMobileDataState(gatewayConnectionConfig2).isEmpty()) {
                if (this.mVcnGatewayConnections.containsKey(gatewayConnectionConfig2)) {
                    logWtf("Attempted to bring up VcnGatewayConnection for config with existing VcnGatewayConnection");
                    return;
                }
                logInfo("Bringing up new VcnGatewayConnection for request " + request);
                VcnGatewayConnection vcnGatewayConnection = this.mDeps.newVcnGatewayConnection(this.mVcnContext, this.mSubscriptionGroup, this.mLastSnapshot, gatewayConnectionConfig2, new VcnGatewayStatusCallbackImpl(gatewayConnectionConfig2), this.mIsMobileDataEnabled);
                this.mVcnGatewayConnections.put(gatewayConnectionConfig2, vcnGatewayConnection);
                return;
            }
        }
        logVdbg("Request could not be fulfilled by VCN: " + request);
    }

    private Set<Integer> getExposedCapabilitiesForMobileDataState(VcnGatewayConnectionConfig gatewayConnectionConfig) {
        if (this.mIsMobileDataEnabled) {
            return gatewayConnectionConfig.getAllExposedCapabilities();
        }
        Set<Integer> exposedCapsWithoutMobileData = new ArraySet<>(gatewayConnectionConfig.getAllExposedCapabilities());
        exposedCapsWithoutMobileData.removeAll(CAPS_REQUIRING_MOBILE_DATA);
        return exposedCapsWithoutMobileData;
    }

    private void handleGatewayConnectionQuit(VcnGatewayConnectionConfig config) {
        logInfo("VcnGatewayConnection quit: " + config);
        this.mVcnGatewayConnections.remove(config);
        this.mVcnContext.getVcnNetworkProvider().resendAllRequests(this.mRequestListener);
    }

    private void handleSubscriptionsChanged(TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot) {
        this.mLastSnapshot = snapshot;
        for (VcnGatewayConnection gatewayConnection : this.mVcnGatewayConnections.values()) {
            gatewayConnection.updateSubscriptionSnapshot(this.mLastSnapshot);
        }
        updateMobileDataStateListeners();
        handleMobileDataToggled();
    }

    private void updateMobileDataStateListeners() {
        Set<Integer> subIdsInGroup = this.mLastSnapshot.getAllSubIdsInGroup(this.mSubscriptionGroup);
        HandlerExecutor executor = new HandlerExecutor(this);
        for (Integer num : subIdsInGroup) {
            int subId = num.intValue();
            if (!this.mMobileDataStateListeners.containsKey(Integer.valueOf(subId))) {
                VcnUserMobileDataStateListener listener = new VcnUserMobileDataStateListener();
                getTelephonyManagerForSubid(subId).registerTelephonyCallback(executor, listener);
                this.mMobileDataStateListeners.put(Integer.valueOf(subId), listener);
            }
        }
        Iterator<Map.Entry<Integer, VcnUserMobileDataStateListener>> iterator = this.mMobileDataStateListeners.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, VcnUserMobileDataStateListener> entry = iterator.next();
            if (!subIdsInGroup.contains(entry.getKey())) {
                getTelephonyManager().unregisterTelephonyCallback(entry.getValue());
                iterator.remove();
            }
        }
    }

    private void handleMobileDataToggled() {
        boolean oldMobileDataEnabledStatus = this.mIsMobileDataEnabled;
        boolean mobileDataStatus = getMobileDataStatus();
        this.mIsMobileDataEnabled = mobileDataStatus;
        if (oldMobileDataEnabledStatus != mobileDataStatus) {
            for (Map.Entry<VcnGatewayConnectionConfig, VcnGatewayConnection> entry : this.mVcnGatewayConnections.entrySet()) {
                VcnGatewayConnectionConfig gatewayConnectionConfig = entry.getKey();
                VcnGatewayConnection gatewayConnection = entry.getValue();
                Set<Integer> exposedCaps = gatewayConnectionConfig.getAllExposedCapabilities();
                if (exposedCaps.contains(12) || exposedCaps.contains(2)) {
                    if (gatewayConnection == null) {
                        logWtf("Found gatewayConnectionConfig without GatewayConnection");
                    } else {
                        gatewayConnection.teardownAsynchronously();
                    }
                }
            }
            this.mVcnContext.getVcnNetworkProvider().resendAllRequests(this.mRequestListener);
            logInfo("Mobile data " + (this.mIsMobileDataEnabled ? ServiceConfigAccessor.PROVIDER_MODE_ENABLED : ServiceConfigAccessor.PROVIDER_MODE_DISABLED));
        }
    }

    private boolean getMobileDataStatus() {
        for (Integer num : this.mLastSnapshot.getAllSubIdsInGroup(this.mSubscriptionGroup)) {
            int subId = num.intValue();
            if (getTelephonyManagerForSubid(subId).isDataEnabled()) {
                return true;
            }
        }
        return false;
    }

    private boolean isRequestSatisfiedByGatewayConnectionConfig(NetworkRequest request, VcnGatewayConnectionConfig config) {
        NetworkCapabilities.Builder builder = new NetworkCapabilities.Builder();
        builder.addTransportType(0);
        builder.addCapability(28);
        for (Integer num : getExposedCapabilitiesForMobileDataState(config)) {
            int cap = num.intValue();
            builder.addCapability(cap);
        }
        return request.canBeSatisfiedBy(builder.build());
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager) this.mVcnContext.getContext().getSystemService(TelephonyManager.class);
    }

    private TelephonyManager getTelephonyManagerForSubid(int subid) {
        return getTelephonyManager().createForSubscriptionId(subid);
    }

    private String getLogPrefix() {
        return "(" + LogUtils.getHashedSubscriptionGroup(this.mSubscriptionGroup) + "-" + System.identityHashCode(this) + ") ";
    }

    private void logVdbg(String msg) {
    }

    private void logDbg(String msg) {
        Slog.d(TAG, getLogPrefix() + msg);
    }

    private void logDbg(String msg, Throwable tr) {
        Slog.d(TAG, getLogPrefix() + msg, tr);
    }

    private void logInfo(String msg) {
        Slog.i(TAG, getLogPrefix() + msg);
        VcnManagementService.LOCAL_LOG.log(getLogPrefix() + "INFO: " + msg);
    }

    private void logInfo(String msg, Throwable tr) {
        Slog.i(TAG, getLogPrefix() + msg, tr);
        VcnManagementService.LOCAL_LOG.log(getLogPrefix() + "INFO: " + msg + tr);
    }

    private void logErr(String msg) {
        Slog.e(TAG, getLogPrefix() + msg);
        VcnManagementService.LOCAL_LOG.log(getLogPrefix() + "ERR: " + msg);
    }

    private void logErr(String msg, Throwable tr) {
        Slog.e(TAG, getLogPrefix() + msg, tr);
        VcnManagementService.LOCAL_LOG.log(getLogPrefix() + "ERR: " + msg + tr);
    }

    private void logWtf(String msg) {
        Slog.wtf(TAG, getLogPrefix() + msg);
        VcnManagementService.LOCAL_LOG.log(getLogPrefix() + "WTF: " + msg);
    }

    private void logWtf(String msg, Throwable tr) {
        Slog.wtf(TAG, getLogPrefix() + msg, tr);
        VcnManagementService.LOCAL_LOG.log(getLogPrefix() + "WTF: " + msg + tr);
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("Vcn (" + this.mSubscriptionGroup + "):");
        pw.increaseIndent();
        pw.println("mCurrentStatus: " + this.mCurrentStatus);
        pw.println("mIsMobileDataEnabled: " + this.mIsMobileDataEnabled);
        pw.println();
        pw.println("mVcnGatewayConnections:");
        pw.increaseIndent();
        for (VcnGatewayConnection gw : this.mVcnGatewayConnections.values()) {
            gw.dump(pw);
        }
        pw.decreaseIndent();
        pw.println();
        pw.decreaseIndent();
    }

    public boolean isMobileDataEnabled() {
        return this.mIsMobileDataEnabled;
    }

    public void setMobileDataEnabled(boolean isMobileDataEnabled) {
        this.mIsMobileDataEnabled = isMobileDataEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static NetworkScore getNetworkScore() {
        return new NetworkScore.Builder().setLegacyInt(52).setTransportPrimary(true).build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class VcnGatewayStatusCallbackImpl implements VcnGatewayStatusCallback {
        public final VcnGatewayConnectionConfig mGatewayConnectionConfig;

        VcnGatewayStatusCallbackImpl(VcnGatewayConnectionConfig gatewayConnectionConfig) {
            this.mGatewayConnectionConfig = gatewayConnectionConfig;
        }

        @Override // com.android.server.vcn.Vcn.VcnGatewayStatusCallback
        public void onQuit() {
            Vcn vcn = Vcn.this;
            vcn.sendMessage(vcn.obtainMessage(3, this.mGatewayConnectionConfig));
        }

        @Override // com.android.server.vcn.Vcn.VcnGatewayStatusCallback
        public void onSafeModeStatusChanged() {
            Vcn vcn = Vcn.this;
            vcn.sendMessage(vcn.obtainMessage(4));
        }

        @Override // com.android.server.vcn.Vcn.VcnGatewayStatusCallback
        public void onGatewayConnectionError(String gatewayConnectionName, int errorCode, String exceptionClass, String exceptionMessage) {
            Vcn.this.mVcnCallback.onGatewayConnectionError(gatewayConnectionName, errorCode, exceptionClass, exceptionMessage);
        }
    }

    /* loaded from: classes2.dex */
    private class VcnMobileDataContentObserver extends ContentObserver {
        private VcnMobileDataContentObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            Vcn vcn = Vcn.this;
            vcn.sendMessage(vcn.obtainMessage(5));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class VcnUserMobileDataStateListener extends TelephonyCallback implements TelephonyCallback.UserMobileDataStateListener {
        VcnUserMobileDataStateListener() {
        }

        @Override // android.telephony.TelephonyCallback.UserMobileDataStateListener
        public void onUserMobileDataStateChanged(boolean enabled) {
            Vcn vcn = Vcn.this;
            vcn.sendMessage(vcn.obtainMessage(5));
        }
    }

    /* loaded from: classes2.dex */
    public static class Dependencies {
        public VcnGatewayConnection newVcnGatewayConnection(VcnContext vcnContext, ParcelUuid subscriptionGroup, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, VcnGatewayConnectionConfig connectionConfig, VcnGatewayStatusCallback gatewayStatusCallback, boolean isMobileDataEnabled) {
            return new VcnGatewayConnection(vcnContext, subscriptionGroup, snapshot, connectionConfig, gatewayStatusCallback, isMobileDataEnabled);
        }

        public VcnContentResolver newVcnContentResolver(VcnContext vcnContext) {
            return new VcnContentResolver(vcnContext);
        }
    }

    /* loaded from: classes2.dex */
    public static class VcnContentResolver {
        private final ContentResolver mImpl;

        public VcnContentResolver(VcnContext vcnContext) {
            this.mImpl = vcnContext.getContext().getContentResolver();
        }

        public void registerContentObserver(Uri uri, boolean notifyForDescendants, ContentObserver observer) {
            this.mImpl.registerContentObserver(uri, notifyForDescendants, observer);
        }
    }
}
