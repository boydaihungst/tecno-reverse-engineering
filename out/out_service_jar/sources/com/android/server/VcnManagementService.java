package com.android.server;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.vcn.IVcnManagementService;
import android.net.vcn.IVcnStatusCallback;
import android.net.vcn.IVcnUnderlyingNetworkPolicyListener;
import android.net.vcn.VcnConfig;
import android.net.vcn.VcnUnderlyingNetworkPolicy;
import android.net.wifi.WifiInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.UserHandle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.LocalLog;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.net.module.util.LocationPermissionChecker;
import com.android.net.module.util.PermissionUtils;
import com.android.server.VcnManagementService;
import com.android.server.vcn.TelephonySubscriptionTracker;
import com.android.server.vcn.Vcn;
import com.android.server.vcn.VcnContext;
import com.android.server.vcn.VcnNetworkProvider;
import com.android.server.vcn.util.PersistableBundleUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class VcnManagementService extends IVcnManagementService.Stub {
    private static final int LOCAL_LOG_LINE_COUNT = 512;
    public static final boolean VDBG = false;
    private final PersistableBundleUtils.LockingReadWriteHelper mConfigDiskRwHelper;
    private final Context mContext;
    private final Dependencies mDeps;
    private final Handler mHandler;
    private final Looper mLooper;
    private final VcnNetworkProvider mNetworkProvider;
    private final TelephonySubscriptionTracker mTelephonySubscriptionTracker;
    private final TelephonySubscriptionTracker.TelephonySubscriptionTrackerCallback mTelephonySubscriptionTrackerCb;
    private final BroadcastReceiver mVcnBroadcastReceiver;
    private static final String TAG = VcnManagementService.class.getSimpleName();
    private static final long DUMP_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(5);
    public static final LocalLog LOCAL_LOG = new LocalLog(512);
    static final String VCN_CONFIG_FILE = new File(Environment.getDataSystemDirectory(), "vcn/configs.xml").getPath();
    static final long CARRIER_PRIVILEGES_LOST_TEARDOWN_DELAY_MS = TimeUnit.SECONDS.toMillis(30);
    private final TrackingNetworkCallback mTrackingNetworkCallback = new TrackingNetworkCallback();
    private final Map<ParcelUuid, VcnConfig> mConfigs = new ArrayMap();
    private final Map<ParcelUuid, Vcn> mVcns = new ArrayMap();
    private TelephonySubscriptionTracker.TelephonySubscriptionSnapshot mLastSnapshot = TelephonySubscriptionTracker.TelephonySubscriptionSnapshot.EMPTY_SNAPSHOT;
    private final Object mLock = new Object();
    private final Map<IBinder, PolicyListenerBinderDeath> mRegisteredPolicyListeners = new ArrayMap();
    private final Map<IBinder, VcnStatusCallbackInfo> mRegisteredStatusCallbacks = new ArrayMap();

    /* loaded from: classes.dex */
    public interface VcnCallback {
        void onGatewayConnectionError(String str, int i, String str2, String str3);

        void onSafeModeStatusChanged(boolean z);
    }

    VcnManagementService(Context context, Dependencies deps) {
        Context context2 = (Context) Objects.requireNonNull(context, "Missing context");
        this.mContext = context2;
        Dependencies dependencies = (Dependencies) Objects.requireNonNull(deps, "Missing dependencies");
        this.mDeps = dependencies;
        Looper looper = dependencies.getLooper();
        this.mLooper = looper;
        Handler handler = new Handler(looper);
        this.mHandler = handler;
        this.mNetworkProvider = new VcnNetworkProvider(context2, looper);
        VcnSubscriptionTrackerCallback vcnSubscriptionTrackerCallback = new VcnSubscriptionTrackerCallback();
        this.mTelephonySubscriptionTrackerCb = vcnSubscriptionTrackerCallback;
        this.mTelephonySubscriptionTracker = dependencies.newTelephonySubscriptionTracker(context2, looper, vcnSubscriptionTrackerCallback);
        this.mConfigDiskRwHelper = dependencies.newPersistableBundleLockingReadWriteHelper(VCN_CONFIG_FILE);
        VcnBroadcastReceiver vcnBroadcastReceiver = new VcnBroadcastReceiver();
        this.mVcnBroadcastReceiver = vcnBroadcastReceiver;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        intentFilter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        intentFilter.addDataScheme("package");
        context2.registerReceiver(vcnBroadcastReceiver, intentFilter, null, handler);
        handler.post(new Runnable() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                VcnManagementService.this.m543lambda$new$0$comandroidserverVcnManagementService();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-VcnManagementService  reason: not valid java name */
    public /* synthetic */ void m543lambda$new$0$comandroidserverVcnManagementService() {
        PersistableBundle configBundle;
        try {
            configBundle = this.mConfigDiskRwHelper.readFromDisk();
        } catch (IOException e1) {
            logErr("Failed to read configs from disk; retrying", e1);
            try {
                configBundle = this.mConfigDiskRwHelper.readFromDisk();
            } catch (IOException e2) {
                logWtf("Failed to read configs from disk", e2);
                return;
            }
        }
        if (configBundle != null) {
            Map<ParcelUuid, VcnConfig> configs = PersistableBundleUtils.toMap(configBundle, new PersistableBundleUtils.Deserializer() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda2
                @Override // com.android.server.vcn.util.PersistableBundleUtils.Deserializer
                public final Object fromPersistableBundle(PersistableBundle persistableBundle) {
                    return PersistableBundleUtils.toParcelUuid(persistableBundle);
                }
            }, new PersistableBundleUtils.Deserializer() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda3
                @Override // com.android.server.vcn.util.PersistableBundleUtils.Deserializer
                public final Object fromPersistableBundle(PersistableBundle persistableBundle) {
                    return new VcnConfig(persistableBundle);
                }
            });
            synchronized (this.mLock) {
                for (Map.Entry<ParcelUuid, VcnConfig> entry : configs.entrySet()) {
                    if (!this.mConfigs.containsKey(entry.getKey())) {
                        this.mConfigs.put(entry.getKey(), entry.getValue());
                    }
                }
                this.mTelephonySubscriptionTrackerCb.onNewSnapshot(this.mLastSnapshot);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static VcnManagementService create(Context context) {
        return new VcnManagementService(context, new Dependencies());
    }

    /* loaded from: classes.dex */
    public static class Dependencies {
        private HandlerThread mHandlerThread;

        public Looper getLooper() {
            if (this.mHandlerThread == null) {
                synchronized (this) {
                    if (this.mHandlerThread == null) {
                        HandlerThread handlerThread = new HandlerThread(VcnManagementService.TAG);
                        this.mHandlerThread = handlerThread;
                        handlerThread.start();
                    }
                }
            }
            return this.mHandlerThread.getLooper();
        }

        public TelephonySubscriptionTracker newTelephonySubscriptionTracker(Context context, Looper looper, TelephonySubscriptionTracker.TelephonySubscriptionTrackerCallback callback) {
            return new TelephonySubscriptionTracker(context, new Handler(looper), callback);
        }

        public int getBinderCallingUid() {
            return Binder.getCallingUid();
        }

        public PersistableBundleUtils.LockingReadWriteHelper newPersistableBundleLockingReadWriteHelper(String path) {
            return new PersistableBundleUtils.LockingReadWriteHelper(path);
        }

        public VcnContext newVcnContext(Context context, Looper looper, VcnNetworkProvider vcnNetworkProvider, boolean isInTestMode) {
            return new VcnContext(context, looper, vcnNetworkProvider, isInTestMode);
        }

        public Vcn newVcn(VcnContext vcnContext, ParcelUuid subscriptionGroup, VcnConfig config, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot, VcnCallback vcnCallback) {
            return new Vcn(vcnContext, subscriptionGroup, config, snapshot, vcnCallback);
        }

        public int getSubIdForWifiInfo(WifiInfo wifiInfo) {
            return wifiInfo.getSubscriptionId();
        }

        public LocationPermissionChecker newLocationPermissionChecker(Context context) {
            return new LocationPermissionChecker(context);
        }
    }

    public void systemReady() {
        this.mNetworkProvider.register();
        ((ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class)).registerNetworkCallback(new NetworkRequest.Builder().clearCapabilities().build(), this.mTrackingNetworkCallback);
        this.mTelephonySubscriptionTracker.register();
    }

    private void enforcePrimaryUser() {
        int uid = this.mDeps.getBinderCallingUid();
        if (uid == 1000) {
            throw new IllegalStateException("Calling identity was System Server. Was Binder calling identity cleared?");
        }
        if (!UserHandle.getUserHandleForUid(uid).isSystem()) {
            throw new SecurityException("VcnManagementService can only be used by callers running as the primary user");
        }
    }

    private void enforceCallingUserAndCarrierPrivilege(final ParcelUuid subscriptionGroup, String pkgName) {
        enforcePrimaryUser();
        final SubscriptionManager subMgr = (SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class);
        final List<SubscriptionInfo> subscriptionInfos = new ArrayList<>();
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda11
            public final void runOrThrow() {
                subscriptionInfos.addAll(subMgr.getSubscriptionsInGroup(subscriptionGroup));
            }
        });
        for (SubscriptionInfo info : subscriptionInfos) {
            TelephonyManager telMgr = ((TelephonyManager) this.mContext.getSystemService(TelephonyManager.class)).createForSubscriptionId(info.getSubscriptionId());
            if (SubscriptionManager.isValidSlotIndex(info.getSimSlotIndex()) && telMgr.checkCarrierPrivilegesForPackage(pkgName) == 1) {
                return;
            }
        }
        throw new SecurityException("Carrier privilege required for subscription group to set VCN Config");
    }

    private void enforceManageTestNetworksForTestMode(VcnConfig vcnConfig) {
        if (vcnConfig.isTestModeProfile()) {
            this.mContext.enforceCallingPermission("android.permission.MANAGE_TEST_NETWORKS", "Test-mode require the MANAGE_TEST_NETWORKS permission");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isActiveSubGroup(ParcelUuid subGrp, TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot) {
        if (subGrp == null || snapshot == null) {
            return false;
        }
        return Objects.equals(subGrp, snapshot.getActiveDataSubscriptionGroup());
    }

    /* loaded from: classes.dex */
    private class VcnBroadcastReceiver extends BroadcastReceiver {
        private VcnBroadcastReceiver() {
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -810471698:
                    if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 267468725:
                    if (action.equals("android.intent.action.PACKAGE_DATA_CLEARED")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 525384130:
                    if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1544582882:
                    if (action.equals("android.intent.action.PACKAGE_ADDED")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1580442797:
                    if (action.equals("android.intent.action.PACKAGE_FULLY_REMOVED")) {
                        c = 3;
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
                case 1:
                case 2:
                    VcnManagementService.this.mTelephonySubscriptionTracker.handleSubscriptionsChanged();
                    return;
                case 3:
                case 4:
                    String pkgName = intent.getData().getSchemeSpecificPart();
                    if (pkgName == null || pkgName.isEmpty()) {
                        VcnManagementService.this.logWtf("Package name was empty or null for intent with action" + action);
                        return;
                    }
                    synchronized (VcnManagementService.this.mLock) {
                        List<ParcelUuid> toRemove = new ArrayList<>();
                        for (Map.Entry<ParcelUuid, VcnConfig> entry : VcnManagementService.this.mConfigs.entrySet()) {
                            if (pkgName.equals(entry.getValue().getProvisioningPackageName())) {
                                toRemove.add(entry.getKey());
                            }
                        }
                        for (ParcelUuid subGrp : toRemove) {
                            VcnManagementService.this.stopAndClearVcnConfigInternalLocked(subGrp);
                        }
                        if (!toRemove.isEmpty()) {
                            VcnManagementService.this.writeConfigsToDiskLocked();
                        }
                    }
                    return;
                default:
                    Slog.wtf(VcnManagementService.TAG, "received unexpected intent: " + action);
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class VcnSubscriptionTrackerCallback implements TelephonySubscriptionTracker.TelephonySubscriptionTrackerCallback {
        private VcnSubscriptionTrackerCallback() {
        }

        @Override // com.android.server.vcn.TelephonySubscriptionTracker.TelephonySubscriptionTrackerCallback
        public void onNewSnapshot(TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot) {
            long teardownDelayMs;
            synchronized (VcnManagementService.this.mLock) {
                TelephonySubscriptionTracker.TelephonySubscriptionSnapshot oldSnapshot = VcnManagementService.this.mLastSnapshot;
                VcnManagementService.this.mLastSnapshot = snapshot;
                VcnManagementService.this.logInfo("new snapshot: " + VcnManagementService.this.mLastSnapshot);
                for (Map.Entry<ParcelUuid, VcnConfig> entry : VcnManagementService.this.mConfigs.entrySet()) {
                    ParcelUuid subGrp = entry.getKey();
                    if (snapshot.packageHasPermissionsForSubscriptionGroup(subGrp, entry.getValue().getProvisioningPackageName()) && VcnManagementService.this.isActiveSubGroup(subGrp, snapshot)) {
                        if (!VcnManagementService.this.mVcns.containsKey(subGrp)) {
                            VcnManagementService.this.startVcnLocked(subGrp, entry.getValue());
                        }
                        VcnManagementService.this.mHandler.removeCallbacksAndMessages(VcnManagementService.this.mVcns.get(subGrp));
                    }
                }
                for (Map.Entry<ParcelUuid, Vcn> entry2 : VcnManagementService.this.mVcns.entrySet()) {
                    final ParcelUuid subGrp2 = entry2.getKey();
                    VcnConfig config = (VcnConfig) VcnManagementService.this.mConfigs.get(subGrp2);
                    boolean isActiveSubGrp = VcnManagementService.this.isActiveSubGroup(subGrp2, snapshot);
                    boolean isValidActiveDataSubIdNotInVcnSubGrp = SubscriptionManager.isValidSubscriptionId(snapshot.getActiveDataSubscriptionId()) && !VcnManagementService.this.isActiveSubGroup(subGrp2, snapshot);
                    if (config != null && snapshot.packageHasPermissionsForSubscriptionGroup(subGrp2, config.getProvisioningPackageName()) && isActiveSubGrp) {
                        entry2.getValue().updateSubscriptionSnapshot(VcnManagementService.this.mLastSnapshot);
                    }
                    final Vcn instanceToTeardown = entry2.getValue();
                    if (isValidActiveDataSubIdNotInVcnSubGrp) {
                        teardownDelayMs = 0;
                    } else {
                        teardownDelayMs = VcnManagementService.CARRIER_PRIVILEGES_LOST_TEARDOWN_DELAY_MS;
                    }
                    VcnManagementService.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.VcnManagementService$VcnSubscriptionTrackerCallback$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            VcnManagementService.VcnSubscriptionTrackerCallback.this.m551xba5ca82(subGrp2, instanceToTeardown);
                        }
                    }, instanceToTeardown, teardownDelayMs);
                }
                Map<ParcelUuid, Set<Integer>> oldSubGrpMappings = VcnManagementService.this.getSubGroupToSubIdMappings(oldSnapshot);
                VcnManagementService vcnManagementService = VcnManagementService.this;
                Map<ParcelUuid, Set<Integer>> currSubGrpMappings = vcnManagementService.getSubGroupToSubIdMappings(vcnManagementService.mLastSnapshot);
                if (!currSubGrpMappings.equals(oldSubGrpMappings)) {
                    VcnManagementService.this.garbageCollectAndWriteVcnConfigsLocked();
                    VcnManagementService.this.notifyAllPolicyListenersLocked();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onNewSnapshot$0$com-android-server-VcnManagementService$VcnSubscriptionTrackerCallback  reason: not valid java name */
        public /* synthetic */ void m551xba5ca82(ParcelUuid uuidToTeardown, Vcn instanceToTeardown) {
            synchronized (VcnManagementService.this.mLock) {
                if (VcnManagementService.this.mVcns.get(uuidToTeardown) == instanceToTeardown) {
                    VcnManagementService.this.stopVcnLocked(uuidToTeardown);
                    VcnManagementService.this.notifyAllPermissionedStatusCallbacksLocked(uuidToTeardown, 1);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Map<ParcelUuid, Set<Integer>> getSubGroupToSubIdMappings(TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot) {
        Map<ParcelUuid, Set<Integer>> subGrpMappings = new ArrayMap<>();
        for (ParcelUuid subGrp : this.mVcns.keySet()) {
            subGrpMappings.put(subGrp, snapshot.getAllSubIdsInGroup(subGrp));
        }
        return subGrpMappings;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopVcnLocked(ParcelUuid uuidToTeardown) {
        logInfo("Stopping VCN config for subGrp: " + uuidToTeardown);
        Vcn vcnToTeardown = this.mVcns.get(uuidToTeardown);
        if (vcnToTeardown == null) {
            return;
        }
        vcnToTeardown.teardownAsynchronously();
        this.mVcns.remove(uuidToTeardown);
        notifyAllPolicyListenersLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAllPolicyListenersLocked() {
        for (final PolicyListenerBinderDeath policyListener : this.mRegisteredPolicyListeners.values()) {
            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda13
                public final void runOrThrow() {
                    VcnManagementService.this.m545x3bfc1e97(policyListener);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyAllPolicyListenersLocked$2$com-android-server-VcnManagementService  reason: not valid java name */
    public /* synthetic */ void m545x3bfc1e97(PolicyListenerBinderDeath policyListener) throws Exception {
        try {
            policyListener.mListener.onPolicyChanged();
        } catch (RemoteException e) {
            logDbg("VcnStatusCallback threw on VCN status change", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyAllPermissionedStatusCallbacksLocked(ParcelUuid subGroup, final int statusCode) {
        for (final VcnStatusCallbackInfo cbInfo : this.mRegisteredStatusCallbacks.values()) {
            if (isCallbackPermissioned(cbInfo, subGroup)) {
                Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda0
                    public final void runOrThrow() {
                        VcnManagementService.this.m544x9608f359(cbInfo, statusCode);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyAllPermissionedStatusCallbacksLocked$3$com-android-server-VcnManagementService  reason: not valid java name */
    public /* synthetic */ void m544x9608f359(VcnStatusCallbackInfo cbInfo, int statusCode) throws Exception {
        try {
            cbInfo.mCallback.onVcnStatusChanged(statusCode);
        } catch (RemoteException e) {
            logDbg("VcnStatusCallback threw on VCN status change", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startVcnLocked(ParcelUuid subscriptionGroup, VcnConfig config) {
        logInfo("Starting VCN config for subGrp: " + subscriptionGroup);
        if (!this.mVcns.isEmpty()) {
            for (ParcelUuid uuidToTeardown : this.mVcns.keySet()) {
                stopVcnLocked(uuidToTeardown);
            }
        }
        VcnCallbackImpl vcnCallback = new VcnCallbackImpl(subscriptionGroup);
        VcnContext vcnContext = this.mDeps.newVcnContext(this.mContext, this.mLooper, this.mNetworkProvider, config.isTestModeProfile());
        Vcn newInstance = this.mDeps.newVcn(vcnContext, subscriptionGroup, config, this.mLastSnapshot, vcnCallback);
        this.mVcns.put(subscriptionGroup, newInstance);
        notifyAllPolicyListenersLocked();
        notifyAllPermissionedStatusCallbacksLocked(subscriptionGroup, 2);
    }

    private void startOrUpdateVcnLocked(ParcelUuid subscriptionGroup, VcnConfig config) {
        logDbg("Starting or updating VCN config for subGrp: " + subscriptionGroup);
        if (this.mVcns.containsKey(subscriptionGroup)) {
            Vcn vcn = this.mVcns.get(subscriptionGroup);
            vcn.updateConfig(config);
        } else if (isActiveSubGroup(subscriptionGroup, this.mLastSnapshot)) {
            startVcnLocked(subscriptionGroup, config);
        }
    }

    public void setVcnConfig(final ParcelUuid subscriptionGroup, final VcnConfig config, String opPkgName) {
        Objects.requireNonNull(subscriptionGroup, "subscriptionGroup was null");
        Objects.requireNonNull(config, "config was null");
        Objects.requireNonNull(opPkgName, "opPkgName was null");
        if (!config.getProvisioningPackageName().equals(opPkgName)) {
            throw new IllegalArgumentException("Mismatched caller and VcnConfig creator");
        }
        logInfo("VCN config updated for subGrp: " + subscriptionGroup);
        ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(this.mDeps.getBinderCallingUid(), config.getProvisioningPackageName());
        enforceManageTestNetworksForTestMode(config);
        enforceCallingUserAndCarrierPrivilege(subscriptionGroup, opPkgName);
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda9
            public final void runOrThrow() {
                VcnManagementService.this.m547lambda$setVcnConfig$4$comandroidserverVcnManagementService(subscriptionGroup, config);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setVcnConfig$4$com-android-server-VcnManagementService  reason: not valid java name */
    public /* synthetic */ void m547lambda$setVcnConfig$4$comandroidserverVcnManagementService(ParcelUuid subscriptionGroup, VcnConfig config) throws Exception {
        synchronized (this.mLock) {
            this.mConfigs.put(subscriptionGroup, config);
            startOrUpdateVcnLocked(subscriptionGroup, config);
            writeConfigsToDiskLocked();
        }
    }

    private void enforceCarrierPrivilegeOrProvisioningPackage(ParcelUuid subscriptionGroup, String pkg) {
        enforcePrimaryUser();
        if (isProvisioningPackageForConfig(subscriptionGroup, pkg)) {
            return;
        }
        enforceCallingUserAndCarrierPrivilege(subscriptionGroup, pkg);
    }

    private boolean isProvisioningPackageForConfig(ParcelUuid subscriptionGroup, String pkg) {
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                VcnConfig config = this.mConfigs.get(subscriptionGroup);
                if (config != null && pkg.equals(config.getProvisioningPackageName())) {
                    return true;
                }
                Binder.restoreCallingIdentity(identity);
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void clearVcnConfig(final ParcelUuid subscriptionGroup, String opPkgName) {
        Objects.requireNonNull(subscriptionGroup, "subscriptionGroup was null");
        Objects.requireNonNull(opPkgName, "opPkgName was null");
        logInfo("VCN config cleared for subGrp: " + subscriptionGroup);
        ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(this.mDeps.getBinderCallingUid(), opPkgName);
        enforceCarrierPrivilegeOrProvisioningPackage(subscriptionGroup, opPkgName);
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda10
            public final void runOrThrow() {
                VcnManagementService.this.m540lambda$clearVcnConfig$5$comandroidserverVcnManagementService(subscriptionGroup);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$clearVcnConfig$5$com-android-server-VcnManagementService  reason: not valid java name */
    public /* synthetic */ void m540lambda$clearVcnConfig$5$comandroidserverVcnManagementService(ParcelUuid subscriptionGroup) throws Exception {
        synchronized (this.mLock) {
            stopAndClearVcnConfigInternalLocked(subscriptionGroup);
            writeConfigsToDiskLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopAndClearVcnConfigInternalLocked(ParcelUuid subscriptionGroup) {
        this.mConfigs.remove(subscriptionGroup);
        boolean vcnExists = this.mVcns.containsKey(subscriptionGroup);
        stopVcnLocked(subscriptionGroup);
        if (vcnExists) {
            notifyAllPermissionedStatusCallbacksLocked(subscriptionGroup, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void garbageCollectAndWriteVcnConfigsLocked() {
        SubscriptionManager subMgr = (SubscriptionManager) this.mContext.getSystemService(SubscriptionManager.class);
        boolean shouldWrite = false;
        Iterator<ParcelUuid> configsIterator = this.mConfigs.keySet().iterator();
        while (configsIterator.hasNext()) {
            ParcelUuid subGrp = configsIterator.next();
            List<SubscriptionInfo> subscriptions = subMgr.getSubscriptionsInGroup(subGrp);
            if (subscriptions == null || subscriptions.isEmpty()) {
                configsIterator.remove();
                shouldWrite = true;
            }
        }
        if (shouldWrite) {
            writeConfigsToDiskLocked();
        }
    }

    public List<ParcelUuid> getConfiguredSubscriptionGroups(String opPkgName) {
        Objects.requireNonNull(opPkgName, "opPkgName was null");
        ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(this.mDeps.getBinderCallingUid(), opPkgName);
        enforcePrimaryUser();
        List<ParcelUuid> result = new ArrayList<>();
        synchronized (this.mLock) {
            for (ParcelUuid subGrp : this.mConfigs.keySet()) {
                if (this.mLastSnapshot.packageHasPermissionsForSubscriptionGroup(subGrp, opPkgName) || isProvisioningPackageForConfig(subGrp, opPkgName)) {
                    result.add(subGrp);
                }
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeConfigsToDiskLocked() {
        try {
            PersistableBundle bundle = PersistableBundleUtils.fromMap(this.mConfigs, new PersistableBundleUtils.Serializer() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda6
                @Override // com.android.server.vcn.util.PersistableBundleUtils.Serializer
                public final PersistableBundle toPersistableBundle(Object obj) {
                    return PersistableBundleUtils.fromParcelUuid((ParcelUuid) obj);
                }
            }, new PersistableBundleUtils.Serializer() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda7
                @Override // com.android.server.vcn.util.PersistableBundleUtils.Serializer
                public final PersistableBundle toPersistableBundle(Object obj) {
                    return ((VcnConfig) obj).toPersistableBundle();
                }
            });
            this.mConfigDiskRwHelper.writeToDisk(bundle);
        } catch (IOException e) {
            logErr("Failed to save configs to disk", e);
            throw new ServiceSpecificException(0, "Failed to save configs");
        }
    }

    Map<ParcelUuid, VcnConfig> getConfigs() {
        Map<ParcelUuid, VcnConfig> unmodifiableMap;
        synchronized (this.mLock) {
            unmodifiableMap = Collections.unmodifiableMap(this.mConfigs);
        }
        return unmodifiableMap;
    }

    public Map<ParcelUuid, Vcn> getAllVcns() {
        Map<ParcelUuid, Vcn> unmodifiableMap;
        synchronized (this.mLock) {
            unmodifiableMap = Collections.unmodifiableMap(this.mVcns);
        }
        return unmodifiableMap;
    }

    public Map<IBinder, VcnStatusCallbackInfo> getAllStatusCallbacks() {
        Map<IBinder, VcnStatusCallbackInfo> unmodifiableMap;
        synchronized (this.mLock) {
            unmodifiableMap = Collections.unmodifiableMap(this.mRegisteredStatusCallbacks);
        }
        return unmodifiableMap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PolicyListenerBinderDeath implements IBinder.DeathRecipient {
        private final IVcnUnderlyingNetworkPolicyListener mListener;

        PolicyListenerBinderDeath(IVcnUnderlyingNetworkPolicyListener listener) {
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Log.e(VcnManagementService.TAG, "app died without removing VcnUnderlyingNetworkPolicyListener");
            VcnManagementService.this.removeVcnUnderlyingNetworkPolicyListener(this.mListener);
        }
    }

    public void addVcnUnderlyingNetworkPolicyListener(final IVcnUnderlyingNetworkPolicyListener listener) {
        Objects.requireNonNull(listener, "listener was null");
        PermissionUtils.enforceAnyPermissionOf(this.mContext, new String[]{"android.permission.NETWORK_FACTORY", "android.permission.MANAGE_TEST_NETWORKS"});
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda5
            public final void runOrThrow() {
                VcnManagementService.this.m539x93d370f9(listener);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$addVcnUnderlyingNetworkPolicyListener$6$com-android-server-VcnManagementService  reason: not valid java name */
    public /* synthetic */ void m539x93d370f9(IVcnUnderlyingNetworkPolicyListener listener) throws Exception {
        PolicyListenerBinderDeath listenerBinderDeath = new PolicyListenerBinderDeath(listener);
        synchronized (this.mLock) {
            this.mRegisteredPolicyListeners.put(listener.asBinder(), listenerBinderDeath);
            try {
                listener.asBinder().linkToDeath(listenerBinderDeath, 0);
            } catch (RemoteException e) {
                listenerBinderDeath.binderDied();
            }
        }
    }

    public void removeVcnUnderlyingNetworkPolicyListener(final IVcnUnderlyingNetworkPolicyListener listener) {
        Objects.requireNonNull(listener, "listener was null");
        PermissionUtils.enforceAnyPermissionOf(this.mContext, new String[]{"android.permission.NETWORK_FACTORY", "android.permission.MANAGE_TEST_NETWORKS"});
        Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda8
            public final void runOrThrow() {
                VcnManagementService.this.m546xc4660e37(listener);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$removeVcnUnderlyingNetworkPolicyListener$7$com-android-server-VcnManagementService  reason: not valid java name */
    public /* synthetic */ void m546xc4660e37(IVcnUnderlyingNetworkPolicyListener listener) throws Exception {
        synchronized (this.mLock) {
            PolicyListenerBinderDeath listenerBinderDeath = this.mRegisteredPolicyListeners.remove(listener.asBinder());
            if (listenerBinderDeath != null) {
                listener.asBinder().unlinkToDeath(listenerBinderDeath, 0);
            }
        }
    }

    private ParcelUuid getSubGroupForNetworkCapabilities(NetworkCapabilities networkCapabilities) {
        TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot;
        ParcelUuid subGrp = null;
        synchronized (this.mLock) {
            snapshot = this.mLastSnapshot;
        }
        for (Integer num : networkCapabilities.getSubscriptionIds()) {
            int subId = num.intValue();
            if (subGrp != null && !subGrp.equals(snapshot.getGroupForSubId(subId))) {
                logWtf("Got multiple subscription groups for a single network");
            }
            subGrp = snapshot.getGroupForSubId(subId);
        }
        return subGrp;
    }

    public VcnUnderlyingNetworkPolicy getUnderlyingNetworkPolicy(final NetworkCapabilities networkCapabilities, final LinkProperties linkProperties) {
        Objects.requireNonNull(networkCapabilities, "networkCapabilities was null");
        Objects.requireNonNull(linkProperties, "linkProperties was null");
        PermissionUtils.enforceAnyPermissionOf(this.mContext, new String[]{"android.permission.NETWORK_FACTORY", "android.permission.MANAGE_TEST_NETWORKS"});
        boolean isUsingManageTestNetworks = this.mContext.checkCallingOrSelfPermission("android.permission.NETWORK_FACTORY") != 0;
        if (isUsingManageTestNetworks && !networkCapabilities.hasTransport(7)) {
            throw new IllegalStateException("NetworkCapabilities must be for Test Network if using permission MANAGE_TEST_NETWORKS");
        }
        return (VcnUnderlyingNetworkPolicy) Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingSupplier() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda4
            public final Object getOrThrow() {
                return VcnManagementService.this.m542xdfee22b(networkCapabilities, linkProperties);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getUnderlyingNetworkPolicy$8$com-android-server-VcnManagementService  reason: not valid java name */
    public /* synthetic */ VcnUnderlyingNetworkPolicy m542xdfee22b(NetworkCapabilities networkCapabilities, LinkProperties linkProperties) throws Exception {
        NetworkCapabilities ncCopy = new NetworkCapabilities(networkCapabilities);
        ParcelUuid subGrp = getSubGroupForNetworkCapabilities(ncCopy);
        boolean isVcnManagedNetwork = false;
        boolean isRestrictedCarrierWifi = false;
        synchronized (this.mLock) {
            Vcn vcn = this.mVcns.get(subGrp);
            if (vcn != null) {
                if (vcn.getStatus() == 2) {
                    isVcnManagedNetwork = true;
                }
                if (ncCopy.hasTransport(1)) {
                    isRestrictedCarrierWifi = true;
                }
            }
        }
        NetworkCapabilities.Builder ncBuilder = new NetworkCapabilities.Builder(ncCopy);
        if (isVcnManagedNetwork) {
            ncBuilder.removeCapability(28);
        } else {
            ncBuilder.addCapability(28);
        }
        if (isRestrictedCarrierWifi) {
            ncBuilder.removeCapability(13);
        }
        NetworkCapabilities result = ncBuilder.build();
        VcnUnderlyingNetworkPolicy policy = new VcnUnderlyingNetworkPolicy(this.mTrackingNetworkCallback.requiresRestartForCarrierWifi(result), result);
        logVdbg("getUnderlyingNetworkPolicy() called for caps: " + networkCapabilities + "; and lp: " + linkProperties + "; result = " + policy);
        return policy;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class VcnStatusCallbackInfo implements IBinder.DeathRecipient {
        final IVcnStatusCallback mCallback;
        final String mPkgName;
        final ParcelUuid mSubGroup;
        final int mUid;

        private VcnStatusCallbackInfo(ParcelUuid subGroup, IVcnStatusCallback callback, String pkgName, int uid) {
            this.mSubGroup = subGroup;
            this.mCallback = callback;
            this.mPkgName = pkgName;
            this.mUid = uid;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Log.e(VcnManagementService.TAG, "app died without unregistering VcnStatusCallback");
            VcnManagementService.this.unregisterVcnStatusCallback(this.mCallback);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isCallbackPermissioned(VcnStatusCallbackInfo cbInfo, ParcelUuid subgroup) {
        return subgroup.equals(cbInfo.mSubGroup) && this.mLastSnapshot.packageHasPermissionsForSubscriptionGroup(subgroup, cbInfo.mPkgName);
    }

    /* JADX WARN: Can't wrap try/catch for region: R(7:11|(1:13)(1:38)|(4:(1:19)(3:29|(2:34|35)|36)|20|21|22)|37|20|21|22) */
    /* JADX WARN: Code restructure failed: missing block: B:31:0x009b, code lost:
        r7 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x009c, code lost:
        logDbg("VcnStatusCallback threw on VCN status change", r7);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void registerVcnStatusCallback(ParcelUuid subGroup, IVcnStatusCallback callback, String opPkgName) {
        int resultStatus;
        int callingUid = this.mDeps.getBinderCallingUid();
        long identity = Binder.clearCallingIdentity();
        try {
            Objects.requireNonNull(subGroup, "subGroup must not be null");
            Objects.requireNonNull(callback, "callback must not be null");
            Objects.requireNonNull(opPkgName, "opPkgName must not be null");
            ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(callingUid, opPkgName);
            IBinder cbBinder = callback.asBinder();
            VcnStatusCallbackInfo cbInfo = new VcnStatusCallbackInfo(subGroup, callback, opPkgName, callingUid);
            int vcnStatus = 0;
            try {
                cbBinder.linkToDeath(cbInfo, 0);
                synchronized (this.mLock) {
                    if (this.mRegisteredStatusCallbacks.containsKey(cbBinder)) {
                        throw new IllegalStateException("Attempting to register a callback that is already in use");
                    }
                    this.mRegisteredStatusCallbacks.put(cbBinder, cbInfo);
                    VcnConfig vcnConfig = this.mConfigs.get(subGroup);
                    Vcn vcn = this.mVcns.get(subGroup);
                    if (vcn != null) {
                        vcnStatus = vcn.getStatus();
                    }
                    if (vcnConfig != null && isCallbackPermissioned(cbInfo, subGroup)) {
                        if (vcn == null) {
                            resultStatus = 1;
                        } else {
                            if (vcnStatus != 2 && vcnStatus != 3) {
                                logWtf("Unknown VCN status: " + vcnStatus);
                                resultStatus = 0;
                            }
                            resultStatus = vcnStatus;
                        }
                        cbInfo.mCallback.onVcnStatusChanged(resultStatus);
                    }
                    resultStatus = 0;
                    cbInfo.mCallback.onVcnStatusChanged(resultStatus);
                }
            } catch (RemoteException e) {
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void unregisterVcnStatusCallback(IVcnStatusCallback callback) {
        long identity = Binder.clearCallingIdentity();
        try {
            Objects.requireNonNull(callback, "callback must not be null");
            IBinder cbBinder = callback.asBinder();
            synchronized (this.mLock) {
                VcnStatusCallbackInfo cbInfo = this.mRegisteredStatusCallbacks.remove(cbBinder);
                if (cbInfo != null) {
                    cbBinder.unlinkToDeath(cbInfo, 0);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    void setLastSnapshot(TelephonySubscriptionTracker.TelephonySubscriptionSnapshot snapshot) {
        this.mLastSnapshot = (TelephonySubscriptionTracker.TelephonySubscriptionSnapshot) Objects.requireNonNull(snapshot);
    }

    private void logVdbg(String msg) {
    }

    private void logDbg(String msg) {
        Slog.d(TAG, msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logDbg(String msg, Throwable tr) {
        Slog.d(TAG, msg, tr);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logInfo(String msg) {
        String str = TAG;
        Slog.i(str, msg);
        LOCAL_LOG.log("[INFO] [" + str + "] " + msg);
    }

    private void logInfo(String msg, Throwable tr) {
        String str = TAG;
        Slog.i(str, msg, tr);
        LOCAL_LOG.log("[INFO] [" + str + "] " + msg + tr);
    }

    private void logErr(String msg) {
        String str = TAG;
        Slog.e(str, msg);
        LOCAL_LOG.log("[ERR] [" + str + "] " + msg);
    }

    private void logErr(String msg, Throwable tr) {
        String str = TAG;
        Slog.e(str, msg, tr);
        LOCAL_LOG.log("[ERR ] [" + str + "] " + msg + tr);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logWtf(String msg) {
        String str = TAG;
        Slog.wtf(str, msg);
        LOCAL_LOG.log("[WTF] [" + str + "] " + msg);
    }

    private void logWtf(String msg, Throwable tr) {
        String str = TAG;
        Slog.wtf(str, msg, tr);
        LOCAL_LOG.log("[WTF ] [" + str + "] " + msg + tr);
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "| ");
        this.mHandler.runWithScissors(new Runnable() { // from class: com.android.server.VcnManagementService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                VcnManagementService.this.m541lambda$dump$9$comandroidserverVcnManagementService(pw);
            }
        }, DUMP_TIMEOUT_MILLIS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$dump$9$com-android-server-VcnManagementService  reason: not valid java name */
    public /* synthetic */ void m541lambda$dump$9$comandroidserverVcnManagementService(IndentingPrintWriter pw) {
        this.mNetworkProvider.dump(pw);
        pw.println();
        this.mTrackingNetworkCallback.dump(pw);
        pw.println();
        synchronized (this.mLock) {
            this.mLastSnapshot.dump(pw);
            pw.println();
            pw.println("mConfigs:");
            pw.increaseIndent();
            for (Map.Entry<ParcelUuid, VcnConfig> entry : this.mConfigs.entrySet()) {
                pw.println(entry.getKey() + ": " + entry.getValue().getProvisioningPackageName());
            }
            pw.decreaseIndent();
            pw.println();
            pw.println("mVcns:");
            pw.increaseIndent();
            for (Vcn vcn : this.mVcns.values()) {
                vcn.dump(pw);
            }
            pw.decreaseIndent();
            pw.println();
        }
        pw.println("Local log:");
        pw.increaseIndent();
        LOCAL_LOG.dump(pw);
        pw.decreaseIndent();
        pw.println();
    }

    /* loaded from: classes.dex */
    private class TrackingNetworkCallback extends ConnectivityManager.NetworkCallback {
        private final Map<Network, NetworkCapabilities> mCaps;

        private TrackingNetworkCallback() {
            this.mCaps = new ArrayMap();
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
            synchronized (this.mCaps) {
                this.mCaps.put(network, caps);
            }
        }

        @Override // android.net.ConnectivityManager.NetworkCallback
        public void onLost(Network network) {
            synchronized (this.mCaps) {
                this.mCaps.remove(network);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean requiresRestartForCarrierWifi(NetworkCapabilities caps) {
            boolean z = true;
            if (!caps.hasTransport(1) || caps.getSubscriptionIds() == null) {
                return false;
            }
            synchronized (this.mCaps) {
                for (NetworkCapabilities existing : this.mCaps.values()) {
                    if (existing.hasTransport(1) && caps.getSubscriptionIds().equals(existing.getSubscriptionIds())) {
                        if (existing.hasCapability(13) == caps.hasCapability(13)) {
                            z = false;
                        }
                        return z;
                    }
                }
                return false;
            }
        }

        public void dump(IndentingPrintWriter pw) {
            pw.println("TrackingNetworkCallback:");
            pw.increaseIndent();
            pw.println("mCaps:");
            pw.increaseIndent();
            synchronized (this.mCaps) {
                for (Map.Entry<Network, NetworkCapabilities> entry : this.mCaps.entrySet()) {
                    pw.println(entry.getKey() + ": " + entry.getValue());
                }
            }
            pw.decreaseIndent();
            pw.println();
            pw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class VcnCallbackImpl implements VcnCallback {
        private final ParcelUuid mSubGroup;

        private VcnCallbackImpl(ParcelUuid subGroup) {
            this.mSubGroup = (ParcelUuid) Objects.requireNonNull(subGroup, "Missing subGroup");
        }

        @Override // com.android.server.VcnManagementService.VcnCallback
        public void onSafeModeStatusChanged(boolean isInSafeMode) {
            synchronized (VcnManagementService.this.mLock) {
                if (VcnManagementService.this.mVcns.containsKey(this.mSubGroup)) {
                    int status = isInSafeMode ? 3 : 2;
                    VcnManagementService.this.notifyAllPolicyListenersLocked();
                    VcnManagementService.this.notifyAllPermissionedStatusCallbacksLocked(this.mSubGroup, status);
                }
            }
        }

        @Override // com.android.server.VcnManagementService.VcnCallback
        public void onGatewayConnectionError(final String gatewayConnectionName, final int errorCode, final String exceptionClass, final String exceptionMessage) {
            synchronized (VcnManagementService.this.mLock) {
                if (VcnManagementService.this.mVcns.containsKey(this.mSubGroup)) {
                    for (final VcnStatusCallbackInfo cbInfo : VcnManagementService.this.mRegisteredStatusCallbacks.values()) {
                        if (VcnManagementService.this.isCallbackPermissioned(cbInfo, this.mSubGroup)) {
                            Binder.withCleanCallingIdentity(new FunctionalUtils.ThrowingRunnable() { // from class: com.android.server.VcnManagementService$VcnCallbackImpl$$ExternalSyntheticLambda0
                                public final void runOrThrow() {
                                    VcnManagementService.VcnCallbackImpl.this.m550x193a59dd(cbInfo, gatewayConnectionName, errorCode, exceptionClass, exceptionMessage);
                                }
                            });
                        }
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$onGatewayConnectionError$0$com-android-server-VcnManagementService$VcnCallbackImpl  reason: not valid java name */
        public /* synthetic */ void m550x193a59dd(VcnStatusCallbackInfo cbInfo, String gatewayConnectionName, int errorCode, String exceptionClass, String exceptionMessage) throws Exception {
            try {
                cbInfo.mCallback.onGatewayConnectionError(gatewayConnectionName, errorCode, exceptionClass, exceptionMessage);
            } catch (RemoteException e) {
                VcnManagementService.this.logDbg("VcnStatusCallback threw on VCN status change", e);
            }
        }
    }
}
