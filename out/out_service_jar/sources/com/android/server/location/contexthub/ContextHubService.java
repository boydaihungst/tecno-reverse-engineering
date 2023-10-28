package com.android.server.location.contexthub;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.hardware.SensorPrivacyManagerInternal;
import android.hardware.location.ContextHubInfo;
import android.hardware.location.ContextHubMessage;
import android.hardware.location.ContextHubTransaction;
import android.hardware.location.IContextHubCallback;
import android.hardware.location.IContextHubClient;
import android.hardware.location.IContextHubClientCallback;
import android.hardware.location.IContextHubService;
import android.hardware.location.IContextHubTransactionCallback;
import android.hardware.location.NanoApp;
import android.hardware.location.NanoAppBinary;
import android.hardware.location.NanoAppFilter;
import android.hardware.location.NanoAppInstanceInfo;
import android.hardware.location.NanoAppMessage;
import android.hardware.location.NanoAppState;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.location.contexthub.IContextHubWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class ContextHubService extends IContextHubService.Stub {
    public static final int CONTEXT_HUB_EVENT_RESTARTED = 1;
    public static final int CONTEXT_HUB_EVENT_UNKNOWN = 0;
    private static final boolean DEBUG_LOG_ENABLED = false;
    public static final int MSG_DISABLE_NANO_APP = 2;
    public static final int MSG_ENABLE_NANO_APP = 1;
    public static final int MSG_HUB_RESET = 7;
    public static final int MSG_LOAD_NANO_APP = 3;
    public static final int MSG_QUERY_MEMORY = 6;
    public static final int MSG_QUERY_NANO_APPS = 5;
    public static final int MSG_UNLOAD_NANO_APP = 4;
    private static final int OS_APP_INSTANCE = -1;
    private static final int PERIOD_METRIC_QUERY_DAYS = 1;
    private static final String TAG = "ContextHubService";
    private final ContextHubClientManager mClientManager;
    private final Context mContext;
    private final Map<Integer, ContextHubInfo> mContextHubIdToInfoMap;
    private final List<ContextHubInfo> mContextHubInfoList;
    private final IContextHubWrapper mContextHubWrapper;
    private final Map<Integer, IContextHubClient> mDefaultClientMap;
    private final SensorPrivacyManagerInternal mSensorPrivacyManagerInternal;
    private final List<String> mSupportedContextHubPerms;
    private final ContextHubTransactionManager mTransactionManager;
    private final RemoteCallbackList<IContextHubCallback> mCallbacksList = new RemoteCallbackList<>();
    private final NanoAppStateManager mNanoAppStateManager = new NanoAppStateManager();
    private final ScheduledThreadPoolExecutor mDailyMetricTimer = new ScheduledThreadPoolExecutor(1);
    private boolean mIsWifiAvailable = false;
    private boolean mIsWifiScanningEnabled = false;
    private boolean mIsWifiMainEnabled = false;
    private boolean mIsBtScanningEnabled = false;
    private boolean mIsBtMainEnabled = false;
    private Set<Integer> mMetricQueryPendingContextHubIds = Collections.newSetFromMap(new ConcurrentHashMap());
    private final Object mSendWifiSettingUpdateLock = new Object();
    private final Map<Integer, AtomicLong> mLastRestartTimestampMap = new HashMap();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface Type {
    }

    /* loaded from: classes.dex */
    private class ContextHubServiceCallback implements IContextHubWrapper.ICallback {
        private final int mContextHubId;

        ContextHubServiceCallback(int contextHubId) {
            this.mContextHubId = contextHubId;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper.ICallback
        public void handleTransactionResult(int transactionId, boolean success) {
            ContextHubService.this.handleTransactionResultCallback(this.mContextHubId, transactionId, success);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper.ICallback
        public void handleContextHubEvent(int eventType) {
            ContextHubService.this.handleHubEventCallback(this.mContextHubId, eventType);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper.ICallback
        public void handleNanoappAbort(long nanoappId, int abortCode) {
            ContextHubService.this.handleAppAbortCallback(this.mContextHubId, nanoappId, abortCode);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper.ICallback
        public void handleNanoappInfo(List<NanoAppState> nanoappStateList) {
            ContextHubService.this.handleQueryAppsCallback(this.mContextHubId, nanoappStateList);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper.ICallback
        public void handleNanoappMessage(short hostEndpointId, NanoAppMessage message, List<String> nanoappPermissions, List<String> messagePermissions) {
            ContextHubService.this.handleClientMessageCallback(this.mContextHubId, hostEndpointId, message, nanoappPermissions, messagePermissions);
        }
    }

    public ContextHubService(Context context) {
        Pair<List<ContextHubInfo>, List<String>> hubInfo;
        boolean z;
        boolean z2;
        long startTimeNs = SystemClock.elapsedRealtimeNanos();
        this.mContext = context;
        IContextHubWrapper contextHubWrapper = getContextHubWrapper();
        this.mContextHubWrapper = contextHubWrapper;
        if (contextHubWrapper == null) {
            this.mTransactionManager = null;
            this.mClientManager = null;
            this.mSensorPrivacyManagerInternal = null;
            this.mDefaultClientMap = Collections.emptyMap();
            this.mContextHubIdToInfoMap = Collections.emptyMap();
            this.mSupportedContextHubPerms = Collections.emptyList();
            this.mContextHubInfoList = Collections.emptyList();
            return;
        }
        try {
            Pair<List<ContextHubInfo>, List<String>> hubInfo2 = contextHubWrapper.getHubs();
            hubInfo = hubInfo2;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while getting Context Hub info", e);
            hubInfo = new Pair<>(Collections.emptyList(), Collections.emptyList());
        }
        long bootTimeNs = SystemClock.elapsedRealtimeNanos() - startTimeNs;
        int numContextHubs = ((List) hubInfo.first).size();
        ContextHubStatsLog.write(ContextHubStatsLog.CONTEXT_HUB_BOOTED, bootTimeNs, numContextHubs);
        Map<Integer, ContextHubInfo> unmodifiableMap = Collections.unmodifiableMap(ContextHubServiceUtil.createContextHubInfoMap((List) hubInfo.first));
        this.mContextHubIdToInfoMap = unmodifiableMap;
        this.mSupportedContextHubPerms = (List) hubInfo.second;
        this.mContextHubInfoList = new ArrayList(unmodifiableMap.values());
        ContextHubClientManager contextHubClientManager = new ContextHubClientManager(this.mContext, this.mContextHubWrapper);
        this.mClientManager = contextHubClientManager;
        this.mTransactionManager = new ContextHubTransactionManager(this.mContextHubWrapper, contextHubClientManager, this.mNanoAppStateManager);
        this.mSensorPrivacyManagerInternal = (SensorPrivacyManagerInternal) LocalServices.getService(SensorPrivacyManagerInternal.class);
        HashMap<Integer, IContextHubClient> defaultClientMap = new HashMap<>();
        for (Integer num : unmodifiableMap.keySet()) {
            int contextHubId = num.intValue();
            Pair<List<ContextHubInfo>, List<String>> hubInfo3 = hubInfo;
            this.mLastRestartTimestampMap.put(Integer.valueOf(contextHubId), new AtomicLong(SystemClock.elapsedRealtimeNanos()));
            ContextHubInfo contextHubInfo = this.mContextHubIdToInfoMap.get(Integer.valueOf(contextHubId));
            IContextHubClient client = this.mClientManager.registerClient(contextHubInfo, createDefaultClientCallback(contextHubId), (String) null, this.mTransactionManager, this.mContext.getPackageName());
            defaultClientMap.put(Integer.valueOf(contextHubId), client);
            try {
                this.mContextHubWrapper.registerCallback(contextHubId, new ContextHubServiceCallback(contextHubId));
            } catch (RemoteException e2) {
                Log.e(TAG, "RemoteException while registering service callback for hub (ID = " + contextHubId + ")", e2);
            }
            queryNanoAppsInternal(contextHubId);
            hubInfo = hubInfo3;
        }
        this.mDefaultClientMap = Collections.unmodifiableMap(defaultClientMap);
        if (!this.mContextHubWrapper.supportsLocationSettingNotifications()) {
            z = true;
        } else {
            sendLocationSettingUpdate();
            z = true;
            this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("location_mode"), true, new ContentObserver(null) { // from class: com.android.server.location.contexthub.ContextHubService.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    ContextHubService.this.sendLocationSettingUpdate();
                }
            }, -1);
        }
        if (this.mContextHubWrapper.supportsWifiSettingNotifications()) {
            sendWifiSettingUpdate(z);
            BroadcastReceiver wifiReceiver = new BroadcastReceiver() { // from class: com.android.server.location.contexthub.ContextHubService.2
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    if ("android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction()) || "android.net.wifi.action.WIFI_SCAN_AVAILABILITY_CHANGED".equals(intent.getAction())) {
                        ContextHubService.this.sendWifiSettingUpdate(false);
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            filter.addAction("android.net.wifi.action.WIFI_SCAN_AVAILABILITY_CHANGED");
            this.mContext.registerReceiver(wifiReceiver, filter);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("wifi_scan_always_enabled"), true, new ContentObserver(null) { // from class: com.android.server.location.contexthub.ContextHubService.3
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    ContextHubService.this.sendWifiSettingUpdate(false);
                }
            }, -1);
        }
        if (this.mContextHubWrapper.supportsAirplaneModeSettingNotifications()) {
            sendAirplaneModeSettingUpdate();
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, new ContentObserver(null) { // from class: com.android.server.location.contexthub.ContextHubService.4
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange) {
                    ContextHubService.this.sendAirplaneModeSettingUpdate();
                }
            }, -1);
        }
        if (!this.mContextHubWrapper.supportsMicrophoneSettingNotifications()) {
            z2 = true;
        } else {
            sendMicrophoneDisableSettingUpdateForCurrentUser();
            z2 = true;
            this.mSensorPrivacyManagerInternal.addSensorPrivacyListenerForAllUsers(1, new SensorPrivacyManagerInternal.OnUserSensorPrivacyChangedListener() { // from class: com.android.server.location.contexthub.ContextHubService$$ExternalSyntheticLambda2
                public final void onSensorPrivacyChanged(int i, boolean z3) {
                    ContextHubService.this.m4308x2cfbb135(i, z3);
                }
            });
        }
        if (this.mContextHubWrapper.supportsBtSettingNotifications()) {
            sendBtSettingUpdate(z2);
            BroadcastReceiver btReceiver = new BroadcastReceiver() { // from class: com.android.server.location.contexthub.ContextHubService.5
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context2, Intent intent) {
                    if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction()) || "android.bluetooth.adapter.action.BLE_STATE_CHANGED".equals(intent.getAction())) {
                        ContextHubService.this.sendBtSettingUpdate(false);
                    }
                }
            };
            IntentFilter filter2 = new IntentFilter();
            filter2.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
            filter2.addAction("android.bluetooth.adapter.action.BLE_STATE_CHANGED");
            this.mContext.registerReceiver(btReceiver, filter2);
        }
        scheduleDailyMetricSnapshot();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$new$0$com-android-server-location-contexthub-ContextHubService  reason: not valid java name */
    public /* synthetic */ void m4308x2cfbb135(int userId, boolean enabled) {
        if (userId == getCurrentUserId()) {
            Log.d(TAG, "User: " + userId + "mic privacy: " + enabled);
            sendMicrophoneDisableSettingUpdate(enabled);
        }
    }

    private IContextHubClientCallback createDefaultClientCallback(final int contextHubId) {
        return new IContextHubClientCallback.Stub() { // from class: com.android.server.location.contexthub.ContextHubService.6
            public void onMessageFromNanoApp(NanoAppMessage message) {
                int nanoAppHandle = ContextHubService.this.mNanoAppStateManager.getNanoAppHandle(contextHubId, message.getNanoAppId());
                ContextHubService.this.onMessageReceiptOldApi(message.getMessageType(), contextHubId, nanoAppHandle, message.getMessageBody());
            }

            public void onHubReset() {
                byte[] data = {0};
                ContextHubService.this.onMessageReceiptOldApi(7, contextHubId, -1, data);
            }

            public void onNanoAppAborted(long nanoAppId, int abortCode) {
            }

            public void onNanoAppLoaded(long nanoAppId) {
            }

            public void onNanoAppUnloaded(long nanoAppId) {
            }

            public void onNanoAppEnabled(long nanoAppId) {
            }

            public void onNanoAppDisabled(long nanoAppId) {
            }

            public void onClientAuthorizationChanged(long nanoAppId, int authorization) {
            }
        };
    }

    private IContextHubWrapper getContextHubWrapper() {
        IContextHubWrapper wrapper = IContextHubWrapper.maybeConnectToAidl();
        if (wrapper == null) {
            wrapper = IContextHubWrapper.maybeConnectTo1_2();
        }
        if (wrapper == null) {
            wrapper = IContextHubWrapper.maybeConnectTo1_1();
        }
        if (wrapper == null) {
            return IContextHubWrapper.maybeConnectTo1_0();
        }
        return wrapper;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r8v0, resolved type: com.android.server.location.contexthub.ContextHubService */
    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver result) {
        new ContextHubShellCommand(this.mContext, this).exec(this, in, out, err, args, callback, result);
    }

    public int registerCallback(IContextHubCallback callback) throws RemoteException {
        checkPermissions();
        this.mCallbacksList.register(callback);
        Log.d(TAG, "Added callback, total callbacks " + this.mCallbacksList.getRegisteredCallbackCount());
        return 0;
    }

    public int[] getContextHubHandles() throws RemoteException {
        checkPermissions();
        return ContextHubServiceUtil.createPrimitiveIntArray(this.mContextHubIdToInfoMap.keySet());
    }

    public ContextHubInfo getContextHubInfo(int contextHubHandle) throws RemoteException {
        checkPermissions();
        if (!this.mContextHubIdToInfoMap.containsKey(Integer.valueOf(contextHubHandle))) {
            Log.e(TAG, "Invalid Context Hub handle " + contextHubHandle + " in getContextHubInfo");
            return null;
        }
        return this.mContextHubIdToInfoMap.get(Integer.valueOf(contextHubHandle));
    }

    public List<ContextHubInfo> getContextHubs() throws RemoteException {
        checkPermissions();
        return this.mContextHubInfoList;
    }

    private IContextHubTransactionCallback createLoadTransactionCallback(final int contextHubId, final NanoAppBinary nanoAppBinary) {
        return new IContextHubTransactionCallback.Stub() { // from class: com.android.server.location.contexthub.ContextHubService.7
            public void onTransactionComplete(int result) {
                ContextHubService.this.handleLoadResponseOldApi(contextHubId, result, nanoAppBinary);
            }

            public void onQueryResponse(int result, List<NanoAppState> nanoAppStateList) {
            }
        };
    }

    private IContextHubTransactionCallback createUnloadTransactionCallback(final int contextHubId) {
        return new IContextHubTransactionCallback.Stub() { // from class: com.android.server.location.contexthub.ContextHubService.8
            public void onTransactionComplete(int result) {
                ContextHubService.this.handleUnloadResponseOldApi(contextHubId, result);
            }

            public void onQueryResponse(int result, List<NanoAppState> nanoAppStateList) {
            }
        };
    }

    private IContextHubTransactionCallback createQueryTransactionCallback(final int contextHubId) {
        return new IContextHubTransactionCallback.Stub() { // from class: com.android.server.location.contexthub.ContextHubService.9
            public void onTransactionComplete(int result) {
            }

            public void onQueryResponse(int result, List<NanoAppState> nanoAppStateList) {
                byte[] data = {(byte) result};
                ContextHubService.this.onMessageReceiptOldApi(5, contextHubId, -1, data);
            }
        };
    }

    public int loadNanoApp(int contextHubHandle, NanoApp nanoApp) throws RemoteException {
        checkPermissions();
        if (this.mContextHubWrapper == null) {
            return -1;
        }
        if (!isValidContextHubId(contextHubHandle)) {
            Log.e(TAG, "Invalid Context Hub handle " + contextHubHandle + " in loadNanoApp");
            return -1;
        } else if (nanoApp == null) {
            Log.e(TAG, "NanoApp cannot be null in loadNanoApp");
            return -1;
        } else {
            NanoAppBinary nanoAppBinary = new NanoAppBinary(nanoApp.getAppBinary());
            IContextHubTransactionCallback onCompleteCallback = createLoadTransactionCallback(contextHubHandle, nanoAppBinary);
            ContextHubServiceTransaction transaction = this.mTransactionManager.createLoadTransaction(contextHubHandle, nanoAppBinary, onCompleteCallback, getCallingPackageName());
            this.mTransactionManager.addTransaction(transaction);
            return 0;
        }
    }

    public int unloadNanoApp(int nanoAppHandle) throws RemoteException {
        checkPermissions();
        if (this.mContextHubWrapper == null) {
            return -1;
        }
        NanoAppInstanceInfo info = this.mNanoAppStateManager.getNanoAppInstanceInfo(nanoAppHandle);
        if (info == null) {
            Log.e(TAG, "Invalid nanoapp handle " + nanoAppHandle + " in unloadNanoApp");
            return -1;
        }
        int contextHubId = info.getContexthubId();
        long nanoAppId = info.getAppId();
        IContextHubTransactionCallback onCompleteCallback = createUnloadTransactionCallback(contextHubId);
        ContextHubServiceTransaction transaction = this.mTransactionManager.createUnloadTransaction(contextHubId, nanoAppId, onCompleteCallback, getCallingPackageName());
        this.mTransactionManager.addTransaction(transaction);
        return 0;
    }

    public NanoAppInstanceInfo getNanoAppInstanceInfo(int nanoAppHandle) throws RemoteException {
        checkPermissions();
        return this.mNanoAppStateManager.getNanoAppInstanceInfo(nanoAppHandle);
    }

    public int[] findNanoAppOnHub(int contextHubHandle, final NanoAppFilter filter) throws RemoteException {
        checkPermissions();
        final ArrayList<Integer> foundInstances = new ArrayList<>();
        if (filter != null) {
            this.mNanoAppStateManager.foreachNanoAppInstanceInfo(new Consumer() { // from class: com.android.server.location.contexthub.ContextHubService$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ContextHubService.lambda$findNanoAppOnHub$1(filter, foundInstances, (NanoAppInstanceInfo) obj);
                }
            });
        }
        int[] retArray = new int[foundInstances.size()];
        for (int i = 0; i < foundInstances.size(); i++) {
            retArray[i] = foundInstances.get(i).intValue();
        }
        return retArray;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$findNanoAppOnHub$1(NanoAppFilter filter, ArrayList foundInstances, NanoAppInstanceInfo info) {
        if (filter.testMatch(info)) {
            foundInstances.add(Integer.valueOf(info.getHandle()));
        }
    }

    private boolean queryNanoAppsInternal(int contextHubId) {
        if (this.mContextHubWrapper == null) {
            return false;
        }
        IContextHubTransactionCallback onCompleteCallback = createQueryTransactionCallback(contextHubId);
        ContextHubServiceTransaction transaction = this.mTransactionManager.createQueryTransaction(contextHubId, onCompleteCallback, getCallingPackageName());
        this.mTransactionManager.addTransaction(transaction);
        return true;
    }

    public int sendMessage(int contextHubHandle, int nanoAppHandle, ContextHubMessage msg) throws RemoteException {
        checkPermissions();
        if (this.mContextHubWrapper == null) {
            return -1;
        }
        if (msg == null) {
            Log.e(TAG, "ContextHubMessage cannot be null in sendMessage");
            return -1;
        } else if (msg.getData() == null) {
            Log.e(TAG, "ContextHubMessage message body cannot be null in sendMessage");
            return -1;
        } else if (!isValidContextHubId(contextHubHandle)) {
            Log.e(TAG, "Invalid Context Hub handle " + contextHubHandle + " in sendMessage");
            return -1;
        } else {
            boolean success = false;
            if (nanoAppHandle == -1) {
                if (msg.getMsgType() != 5) {
                    Log.e(TAG, "Invalid OS message params of type " + msg.getMsgType());
                } else {
                    success = queryNanoAppsInternal(contextHubHandle);
                }
            } else {
                NanoAppInstanceInfo info = getNanoAppInstanceInfo(nanoAppHandle);
                if (info == null) {
                    Log.e(TAG, "Failed to send nanoapp message - nanoapp with handle " + nanoAppHandle + " does not exist.");
                } else {
                    NanoAppMessage message = NanoAppMessage.createMessageToNanoApp(info.getAppId(), msg.getMsgType(), msg.getData());
                    IContextHubClient client = this.mDefaultClientMap.get(Integer.valueOf(contextHubHandle));
                    success = client.sendMessageToNanoApp(message) == 0;
                }
            }
            return success ? 0 : -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleClientMessageCallback(int contextHubId, short hostEndpointId, NanoAppMessage message, List<String> nanoappPermissions, List<String> messagePermissions) {
        this.mClientManager.onMessageFromNanoApp(contextHubId, hostEndpointId, message, nanoappPermissions, messagePermissions);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleLoadResponseOldApi(int contextHubId, int result, NanoAppBinary nanoAppBinary) {
        if (nanoAppBinary == null) {
            Log.e(TAG, "Nanoapp binary field was null for a load transaction");
            return;
        }
        byte[] data = new byte[5];
        data[0] = (byte) result;
        int nanoAppHandle = this.mNanoAppStateManager.getNanoAppHandle(contextHubId, nanoAppBinary.getNanoAppId());
        ByteBuffer.wrap(data, 1, 4).order(ByteOrder.nativeOrder()).putInt(nanoAppHandle);
        onMessageReceiptOldApi(3, contextHubId, -1, data);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUnloadResponseOldApi(int contextHubId, int result) {
        byte[] data = {(byte) result};
        onMessageReceiptOldApi(4, contextHubId, -1, data);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleTransactionResultCallback(int contextHubId, int transactionId, boolean success) {
        this.mTransactionManager.onTransactionResponse(transactionId, success);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleHubEventCallback(int contextHubId, int eventType) {
        if (eventType == 1) {
            long now = SystemClock.elapsedRealtimeNanos();
            long lastRestartTimeNs = this.mLastRestartTimestampMap.get(Integer.valueOf(contextHubId)).getAndSet(now);
            ContextHubStatsLog.write(ContextHubStatsLog.CONTEXT_HUB_RESTARTED, TimeUnit.NANOSECONDS.toMillis(now - lastRestartTimeNs), contextHubId);
            sendLocationSettingUpdate();
            sendWifiSettingUpdate(true);
            sendAirplaneModeSettingUpdate();
            sendMicrophoneDisableSettingUpdateForCurrentUser();
            sendBtSettingUpdate(true);
            this.mTransactionManager.onHubReset();
            queryNanoAppsInternal(contextHubId);
            this.mClientManager.onHubReset(contextHubId);
            return;
        }
        Log.i(TAG, "Received unknown hub event (hub ID = " + contextHubId + ", type = " + eventType + ")");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAppAbortCallback(int contextHubId, long nanoAppId, int abortCode) {
        this.mClientManager.onNanoAppAborted(contextHubId, nanoAppId, abortCode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleQueryAppsCallback(int contextHubId, List<NanoAppState> nanoappStateList) {
        if (this.mMetricQueryPendingContextHubIds.contains(Integer.valueOf(contextHubId))) {
            for (NanoAppState nanoappState : nanoappStateList) {
                ContextHubStatsLog.write(400, contextHubId, nanoappState.getNanoAppId(), (int) nanoappState.getNanoAppVersion());
            }
            this.mMetricQueryPendingContextHubIds.remove(Integer.valueOf(contextHubId));
            if (this.mMetricQueryPendingContextHubIds.isEmpty()) {
                scheduleDailyMetricSnapshot();
            }
        }
        this.mNanoAppStateManager.updateCache(contextHubId, nanoappStateList);
        this.mTransactionManager.onQueryResponse(nanoappStateList);
    }

    private boolean isValidContextHubId(int contextHubId) {
        return this.mContextHubIdToInfoMap.containsKey(Integer.valueOf(contextHubId));
    }

    public IContextHubClient createClient(int contextHubId, IContextHubClientCallback clientCallback, String attributionTag, String packageName) throws RemoteException {
        checkPermissions();
        if (!isValidContextHubId(contextHubId)) {
            throw new IllegalArgumentException("Invalid context hub ID " + contextHubId);
        }
        if (clientCallback == null) {
            throw new NullPointerException("Cannot register client with null callback");
        }
        ContextHubInfo contextHubInfo = this.mContextHubIdToInfoMap.get(Integer.valueOf(contextHubId));
        return this.mClientManager.registerClient(contextHubInfo, clientCallback, attributionTag, this.mTransactionManager, packageName);
    }

    public IContextHubClient createPendingIntentClient(int contextHubId, PendingIntent pendingIntent, long nanoAppId, String attributionTag) throws RemoteException {
        checkPermissions();
        if (!isValidContextHubId(contextHubId)) {
            throw new IllegalArgumentException("Invalid context hub ID " + contextHubId);
        }
        ContextHubInfo contextHubInfo = this.mContextHubIdToInfoMap.get(Integer.valueOf(contextHubId));
        return this.mClientManager.registerClient(contextHubInfo, pendingIntent, nanoAppId, attributionTag, this.mTransactionManager);
    }

    public void loadNanoAppOnHub(int contextHubId, IContextHubTransactionCallback transactionCallback, NanoAppBinary nanoAppBinary) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(contextHubId, transactionCallback, 0)) {
            return;
        }
        if (nanoAppBinary == null) {
            Log.e(TAG, "NanoAppBinary cannot be null in loadNanoAppOnHub");
            transactionCallback.onTransactionComplete(2);
            return;
        }
        ContextHubServiceTransaction transaction = this.mTransactionManager.createLoadTransaction(contextHubId, nanoAppBinary, transactionCallback, getCallingPackageName());
        this.mTransactionManager.addTransaction(transaction);
    }

    public void unloadNanoAppFromHub(int contextHubId, IContextHubTransactionCallback transactionCallback, long nanoAppId) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(contextHubId, transactionCallback, 1)) {
            return;
        }
        ContextHubServiceTransaction transaction = this.mTransactionManager.createUnloadTransaction(contextHubId, nanoAppId, transactionCallback, getCallingPackageName());
        this.mTransactionManager.addTransaction(transaction);
    }

    public void enableNanoApp(int contextHubId, IContextHubTransactionCallback transactionCallback, long nanoAppId) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(contextHubId, transactionCallback, 2)) {
            return;
        }
        ContextHubServiceTransaction transaction = this.mTransactionManager.createEnableTransaction(contextHubId, nanoAppId, transactionCallback, getCallingPackageName());
        this.mTransactionManager.addTransaction(transaction);
    }

    public void disableNanoApp(int contextHubId, IContextHubTransactionCallback transactionCallback, long nanoAppId) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(contextHubId, transactionCallback, 3)) {
            return;
        }
        ContextHubServiceTransaction transaction = this.mTransactionManager.createDisableTransaction(contextHubId, nanoAppId, transactionCallback, getCallingPackageName());
        this.mTransactionManager.addTransaction(transaction);
    }

    public void queryNanoApps(int contextHubId, IContextHubTransactionCallback transactionCallback) throws RemoteException {
        checkPermissions();
        if (!checkHalProxyAndContextHubId(contextHubId, transactionCallback, 4)) {
            return;
        }
        ContextHubServiceTransaction transaction = this.mTransactionManager.createQueryTransaction(contextHubId, transactionCallback, getCallingPackageName());
        this.mTransactionManager.addTransaction(transaction);
    }

    protected void dump(FileDescriptor fd, final PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            for (String arg : args) {
                if ("--proto".equals(arg)) {
                    dump(new ProtoOutputStream(fd));
                    return;
                }
            }
            pw.println("Dumping ContextHub Service");
            pw.println("");
            pw.println("=================== CONTEXT HUBS ====================");
            for (ContextHubInfo hubInfo : this.mContextHubIdToInfoMap.values()) {
                pw.println(hubInfo);
            }
            pw.println("Supported permissions: " + Arrays.toString(this.mSupportedContextHubPerms.toArray()));
            pw.println("");
            pw.println("=================== NANOAPPS ====================");
            this.mNanoAppStateManager.foreachNanoAppInstanceInfo(new Consumer() { // from class: com.android.server.location.contexthub.ContextHubService$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    pw.println((NanoAppInstanceInfo) obj);
                }
            });
            pw.println("");
            pw.println("=================== CLIENTS ====================");
            pw.println(this.mClientManager);
            pw.println("");
            pw.println("=================== TRANSACTIONS ====================");
            pw.println(this.mTransactionManager);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void denyClientAuthState(int contextHubId, final String packageName, final long nanoAppId) {
        this.mClientManager.forEachClientOfHub(contextHubId, new Consumer() { // from class: com.android.server.location.contexthub.ContextHubService$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ContextHubService.lambda$denyClientAuthState$3(packageName, nanoAppId, (ContextHubClientBroker) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$denyClientAuthState$3(String packageName, long nanoAppId, ContextHubClientBroker client) {
        if (client.getPackageName().equals(packageName)) {
            client.updateNanoAppAuthState(nanoAppId, Collections.emptyList(), false, true);
        }
    }

    private void dump(final ProtoOutputStream proto) {
        this.mContextHubIdToInfoMap.values().forEach(new Consumer() { // from class: com.android.server.location.contexthub.ContextHubService$$ExternalSyntheticLambda5
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ContextHubService.lambda$dump$4(proto, (ContextHubInfo) obj);
            }
        });
        long token = proto.start(1146756268034L);
        this.mClientManager.dump(proto);
        proto.end(token);
        proto.flush();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$dump$4(ProtoOutputStream proto, ContextHubInfo hubInfo) {
        long token = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
        hubInfo.dump(proto);
        proto.end(token);
    }

    private void checkPermissions() {
        ContextHubServiceUtil.checkPermissions(this.mContext);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int onMessageReceiptOldApi(int msgType, int contextHubHandle, int appInstance, byte[] data) {
        if (data == null) {
            return -1;
        }
        synchronized (this.mCallbacksList) {
            int callbacksCount = this.mCallbacksList.beginBroadcast();
            if (callbacksCount < 1) {
                return 0;
            }
            ContextHubMessage msg = new ContextHubMessage(msgType, 0, data);
            for (int i = 0; i < callbacksCount; i++) {
                IContextHubCallback callback = this.mCallbacksList.getBroadcastItem(i);
                try {
                    callback.onMessageReceipt(contextHubHandle, appInstance, msg);
                } catch (RemoteException e) {
                    Log.i(TAG, "Exception (" + e + ") calling remote callback (" + callback + ").");
                }
            }
            this.mCallbacksList.finishBroadcast();
            return 0;
        }
    }

    private boolean checkHalProxyAndContextHubId(int contextHubId, IContextHubTransactionCallback callback, int transactionType) {
        if (this.mContextHubWrapper == null) {
            try {
                callback.onTransactionComplete(8);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while calling onTransactionComplete", e);
            }
            return false;
        } else if (!isValidContextHubId(contextHubId)) {
            Log.e(TAG, "Cannot start " + ContextHubTransaction.typeToString(transactionType, false) + " transaction for invalid hub ID " + contextHubId);
            try {
                callback.onTransactionComplete(2);
            } catch (RemoteException e2) {
                Log.e(TAG, "RemoteException while calling onTransactionComplete", e2);
            }
            return false;
        } else {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendLocationSettingUpdate() {
        boolean enabled = ((LocationManager) this.mContext.getSystemService(LocationManager.class)).isLocationEnabledForUser(UserHandle.CURRENT);
        this.mContextHubWrapper.onLocationSettingChanged(enabled);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendWifiSettingUpdate(boolean forceUpdate) {
        boolean wifiAvailable;
        synchronized (this.mSendWifiSettingUpdateLock) {
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService(WifiManager.class);
            boolean wifiEnabled = wifiManager.isWifiEnabled();
            boolean wifiScanEnabled = wifiManager.isScanAlwaysAvailable();
            if (!wifiEnabled && !wifiScanEnabled) {
                wifiAvailable = false;
                if (!forceUpdate || this.mIsWifiAvailable != wifiAvailable) {
                    this.mIsWifiAvailable = wifiAvailable;
                    this.mContextHubWrapper.onWifiSettingChanged(wifiAvailable);
                }
                if (!forceUpdate || this.mIsWifiScanningEnabled != wifiScanEnabled) {
                    this.mIsWifiScanningEnabled = wifiScanEnabled;
                    this.mContextHubWrapper.onWifiScanningSettingChanged(wifiScanEnabled);
                }
                if (!forceUpdate || this.mIsWifiMainEnabled != wifiEnabled) {
                    this.mIsWifiMainEnabled = wifiEnabled;
                    this.mContextHubWrapper.onWifiMainSettingChanged(wifiEnabled);
                }
            }
            wifiAvailable = true;
            if (!forceUpdate) {
            }
            this.mIsWifiAvailable = wifiAvailable;
            this.mContextHubWrapper.onWifiSettingChanged(wifiAvailable);
            if (!forceUpdate) {
            }
            this.mIsWifiScanningEnabled = wifiScanEnabled;
            this.mContextHubWrapper.onWifiScanningSettingChanged(wifiScanEnabled);
            if (!forceUpdate) {
            }
            this.mIsWifiMainEnabled = wifiEnabled;
            this.mContextHubWrapper.onWifiMainSettingChanged(wifiEnabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendBtSettingUpdate(boolean forceUpdate) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            boolean btEnabled = adapter.isEnabled();
            boolean btScanEnabled = adapter.isBleScanAlwaysAvailable();
            if (forceUpdate || this.mIsBtScanningEnabled != btScanEnabled) {
                this.mIsBtScanningEnabled = btScanEnabled;
                this.mContextHubWrapper.onBtScanningSettingChanged(btScanEnabled);
            }
            if (forceUpdate || this.mIsBtMainEnabled != btEnabled) {
                this.mIsBtMainEnabled = btEnabled;
                this.mContextHubWrapper.onBtMainSettingChanged(btEnabled);
                return;
            }
            return;
        }
        Log.d(TAG, "BT adapter not available. Defaulting to disabled");
        if (this.mIsBtMainEnabled) {
            this.mIsBtMainEnabled = false;
            this.mContextHubWrapper.onBtMainSettingChanged(false);
        }
        if (this.mIsBtScanningEnabled) {
            this.mIsBtScanningEnabled = false;
            this.mContextHubWrapper.onBtScanningSettingChanged(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendAirplaneModeSettingUpdate() {
        boolean enabled = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
        this.mContextHubWrapper.onAirplaneModeSettingChanged(enabled);
    }

    private void sendMicrophoneDisableSettingUpdate(boolean enabled) {
        Log.d(TAG, "Mic Disabled Setting: " + enabled);
        this.mContextHubWrapper.onMicrophoneSettingChanged(!enabled);
    }

    private void sendMicrophoneDisableSettingUpdateForCurrentUser() {
        boolean isEnabled = this.mSensorPrivacyManagerInternal.isSensorPrivacyEnabled(getCurrentUserId(), 1);
        sendMicrophoneDisableSettingUpdate(isEnabled);
    }

    private void scheduleDailyMetricSnapshot() {
        Runnable queryAllContextHub = new Runnable() { // from class: com.android.server.location.contexthub.ContextHubService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ContextHubService.this.m4309x7d7747ba();
            }
        };
        try {
            this.mDailyMetricTimer.schedule(queryAllContextHub, 1L, TimeUnit.DAYS);
        } catch (Exception e) {
            Log.e(TAG, "Error when schedule a timer", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$scheduleDailyMetricSnapshot$5$com-android-server-location-contexthub-ContextHubService  reason: not valid java name */
    public /* synthetic */ void m4309x7d7747ba() {
        for (Integer num : this.mContextHubIdToInfoMap.keySet()) {
            int contextHubId = num.intValue();
            this.mMetricQueryPendingContextHubIds.add(Integer.valueOf(contextHubId));
            queryNanoAppsInternal(contextHubId);
        }
    }

    private String getCallingPackageName() {
        return this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
    }

    private int getCurrentUserId() {
        long id = Binder.clearCallingIdentity();
        try {
            UserInfo currentUser = ActivityManager.getService().getCurrentUser();
            int i = currentUser.id;
            Binder.restoreCallingIdentity(id);
            return i;
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(id);
            return 0;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(id);
            throw th;
        }
    }

    public void onUserChanged() {
        Log.d(TAG, "User changed to id: " + getCurrentUserId());
        sendLocationSettingUpdate();
        sendMicrophoneDisableSettingUpdateForCurrentUser();
    }
}
