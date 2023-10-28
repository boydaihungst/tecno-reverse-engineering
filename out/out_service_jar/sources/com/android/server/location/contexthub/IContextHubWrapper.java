package com.android.server.location.contexthub;

import android.hardware.contexthub.ContextHubMessage;
import android.hardware.contexthub.HostEndpointInfo;
import android.hardware.contexthub.IContextHub;
import android.hardware.contexthub.IContextHubCallback;
import android.hardware.contexthub.NanoappBinary;
import android.hardware.contexthub.NanoappInfo;
import android.hardware.contexthub.V1_0.ContextHub;
import android.hardware.contexthub.V1_0.ContextHubMsg;
import android.hardware.contexthub.V1_0.HubAppInfo;
import android.hardware.contexthub.V1_0.IContexthub;
import android.hardware.contexthub.V1_2.IContexthub;
import android.hardware.contexthub.V1_2.IContexthubCallback;
import android.hardware.location.ContextHubInfo;
import android.hardware.location.NanoAppBinary;
import android.hardware.location.NanoAppMessage;
import android.hardware.location.NanoAppState;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.util.Log;
import android.util.Pair;
import com.android.server.location.contexthub.IContextHubWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
/* loaded from: classes.dex */
public abstract class IContextHubWrapper {
    private static final String TAG = "IContextHubWrapper";

    /* loaded from: classes.dex */
    public interface ICallback {
        void handleContextHubEvent(int i);

        void handleNanoappAbort(long j, int i);

        void handleNanoappInfo(List<NanoAppState> list);

        void handleNanoappMessage(short s, NanoAppMessage nanoAppMessage, List<String> list, List<String> list2);

        void handleTransactionResult(int i, boolean z);
    }

    public abstract int disableNanoapp(int i, long j, int i2) throws RemoteException;

    public abstract int enableNanoapp(int i, long j, int i2) throws RemoteException;

    public abstract Pair<List<ContextHubInfo>, List<String>> getHubs() throws RemoteException;

    public abstract int loadNanoapp(int i, NanoAppBinary nanoAppBinary, int i2) throws RemoteException;

    public abstract void onAirplaneModeSettingChanged(boolean z);

    public abstract void onBtMainSettingChanged(boolean z);

    public abstract void onBtScanningSettingChanged(boolean z);

    public abstract void onLocationSettingChanged(boolean z);

    public abstract void onMicrophoneSettingChanged(boolean z);

    public abstract void onWifiMainSettingChanged(boolean z);

    public abstract void onWifiScanningSettingChanged(boolean z);

    public abstract void onWifiSettingChanged(boolean z);

    public abstract int queryNanoapps(int i) throws RemoteException;

    public abstract void registerCallback(int i, ICallback iCallback) throws RemoteException;

    public abstract int sendMessageToContextHub(short s, int i, NanoAppMessage nanoAppMessage) throws RemoteException;

    public abstract boolean supportsAirplaneModeSettingNotifications();

    public abstract boolean supportsBtSettingNotifications();

    public abstract boolean supportsLocationSettingNotifications();

    public abstract boolean supportsMicrophoneSettingNotifications();

    public abstract boolean supportsWifiSettingNotifications();

    public abstract int unloadNanoapp(int i, long j, int i2) throws RemoteException;

    public static IContextHubWrapper maybeConnectTo1_0() {
        IContexthub proxy = null;
        try {
            proxy = IContexthub.getService(true);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while attaching to Context Hub HAL proxy", e);
        } catch (NoSuchElementException e2) {
            Log.i(TAG, "Context Hub HAL service not found");
        }
        if (proxy == null) {
            return null;
        }
        return new ContextHubWrapperV1_0(proxy);
    }

    public static IContextHubWrapper maybeConnectTo1_1() {
        android.hardware.contexthub.V1_1.IContexthub proxy = null;
        try {
            proxy = android.hardware.contexthub.V1_1.IContexthub.getService(true);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while attaching to Context Hub HAL proxy", e);
        } catch (NoSuchElementException e2) {
            Log.i(TAG, "Context Hub HAL service not found");
        }
        if (proxy == null) {
            return null;
        }
        return new ContextHubWrapperV1_1(proxy);
    }

    public static IContextHubWrapper maybeConnectTo1_2() {
        android.hardware.contexthub.V1_2.IContexthub proxy = null;
        try {
            proxy = android.hardware.contexthub.V1_2.IContexthub.getService(true);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while attaching to Context Hub HAL proxy", e);
        } catch (NoSuchElementException e2) {
            Log.i(TAG, "Context Hub HAL service not found");
        }
        if (proxy == null) {
            return null;
        }
        return new ContextHubWrapperV1_2(proxy);
    }

    public static IContextHubWrapper maybeConnectToAidl() {
        IContextHub proxy = null;
        String aidlServiceName = IContextHub.class.getCanonicalName() + "/default";
        if (ServiceManager.isDeclared(aidlServiceName)) {
            proxy = IContextHub.Stub.asInterface(ServiceManager.waitForService(aidlServiceName));
            if (proxy == null) {
                Log.e(TAG, "Context Hub AIDL service was declared but was not found");
            }
        } else {
            Log.d(TAG, "Context Hub AIDL service is not declared");
        }
        if (proxy == null) {
            return null;
        }
        return new ContextHubWrapperAidl(proxy);
    }

    public void onHostEndpointConnected(HostEndpointInfo info) {
    }

    public void onHostEndpointDisconnected(short hostEndpointId) {
    }

    /* loaded from: classes.dex */
    private static class ContextHubWrapperAidl extends IContextHubWrapper {
        private final Map<Integer, ContextHubAidlCallback> mAidlCallbackMap = new HashMap();
        private Handler mHandler;
        private HandlerThread mHandlerThread;
        private IContextHub mHub;

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class ContextHubAidlCallback extends IContextHubCallback.Stub {
            private final ICallback mCallback;
            private final int mContextHubId;

            ContextHubAidlCallback(int contextHubId, ICallback callback) {
                this.mContextHubId = contextHubId;
                this.mCallback = callback;
            }

            public void handleNanoappInfo(NanoappInfo[] appInfo) {
                final List<NanoAppState> nanoAppStateList = ContextHubServiceUtil.createNanoAppStateList(appInfo);
                ContextHubWrapperAidl.this.mHandler.post(new Runnable() { // from class: com.android.server.location.contexthub.IContextHubWrapper$ContextHubWrapperAidl$ContextHubAidlCallback$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        IContextHubWrapper.ContextHubWrapperAidl.ContextHubAidlCallback.this.m4319xcd9dd95(nanoAppStateList);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$handleNanoappInfo$0$com-android-server-location-contexthub-IContextHubWrapper$ContextHubWrapperAidl$ContextHubAidlCallback  reason: not valid java name */
            public /* synthetic */ void m4319xcd9dd95(List nanoAppStateList) {
                this.mCallback.handleNanoappInfo(nanoAppStateList);
            }

            public void handleContextHubMessage(final ContextHubMessage msg, final String[] msgContentPerms) {
                ContextHubWrapperAidl.this.mHandler.post(new Runnable() { // from class: com.android.server.location.contexthub.IContextHubWrapper$ContextHubWrapperAidl$ContextHubAidlCallback$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        IContextHubWrapper.ContextHubWrapperAidl.ContextHubAidlCallback.this.m4318x7372b64e(msg, msgContentPerms);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$handleContextHubMessage$1$com-android-server-location-contexthub-IContextHubWrapper$ContextHubWrapperAidl$ContextHubAidlCallback  reason: not valid java name */
            public /* synthetic */ void m4318x7372b64e(ContextHubMessage msg, String[] msgContentPerms) {
                this.mCallback.handleNanoappMessage((short) msg.hostEndPoint, ContextHubServiceUtil.createNanoAppMessage(msg), new ArrayList(Arrays.asList(msg.permissions)), new ArrayList(Arrays.asList(msgContentPerms)));
            }

            public void handleContextHubAsyncEvent(final int evt) {
                ContextHubWrapperAidl.this.mHandler.post(new Runnable() { // from class: com.android.server.location.contexthub.IContextHubWrapper$ContextHubWrapperAidl$ContextHubAidlCallback$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        IContextHubWrapper.ContextHubWrapperAidl.ContextHubAidlCallback.this.m4317x51142ae0(evt);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$handleContextHubAsyncEvent$2$com-android-server-location-contexthub-IContextHubWrapper$ContextHubWrapperAidl$ContextHubAidlCallback  reason: not valid java name */
            public /* synthetic */ void m4317x51142ae0(int evt) {
                this.mCallback.handleContextHubEvent(ContextHubServiceUtil.toContextHubEventFromAidl(evt));
            }

            public void handleTransactionResult(final int transactionId, final boolean success) {
                ContextHubWrapperAidl.this.mHandler.post(new Runnable() { // from class: com.android.server.location.contexthub.IContextHubWrapper$ContextHubWrapperAidl$ContextHubAidlCallback$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        IContextHubWrapper.ContextHubWrapperAidl.ContextHubAidlCallback.this.m4320x66319112(transactionId, success);
                    }
                });
            }

            /* JADX INFO: Access modifiers changed from: package-private */
            /* renamed from: lambda$handleTransactionResult$3$com-android-server-location-contexthub-IContextHubWrapper$ContextHubWrapperAidl$ContextHubAidlCallback  reason: not valid java name */
            public /* synthetic */ void m4320x66319112(int transactionId, boolean success) {
                this.mCallback.handleTransactionResult(transactionId, success);
            }

            public String getInterfaceHash() {
                return "10abe2e5202d9b80ccebf5f6376d711a9a212b27";
            }

            public int getInterfaceVersion() {
                return 1;
            }
        }

        ContextHubWrapperAidl(IContextHub hub) {
            HandlerThread handlerThread = new HandlerThread("Context Hub AIDL callback", 10);
            this.mHandlerThread = handlerThread;
            this.mHub = hub;
            handlerThread.start();
            this.mHandler = new Handler(this.mHandlerThread.getLooper());
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public Pair<List<ContextHubInfo>, List<String>> getHubs() throws RemoteException {
            String[] strArr;
            Set<String> supportedPermissions = new HashSet<>();
            ArrayList<ContextHubInfo> hubInfoList = new ArrayList<>();
            for (android.hardware.contexthub.ContextHubInfo hub : this.mHub.getContextHubs()) {
                hubInfoList.add(new ContextHubInfo(hub));
                for (String permission : hub.supportedPermissions) {
                    supportedPermissions.add(permission);
                }
            }
            return new Pair<>(hubInfoList, new ArrayList(supportedPermissions));
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsLocationSettingNotifications() {
            return true;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsWifiSettingNotifications() {
            return true;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsAirplaneModeSettingNotifications() {
            return true;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsMicrophoneSettingNotifications() {
            return true;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsBtSettingNotifications() {
            return true;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onLocationSettingChanged(boolean enabled) {
            onSettingChanged((byte) 1, enabled);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onWifiSettingChanged(boolean enabled) {
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onAirplaneModeSettingChanged(boolean enabled) {
            onSettingChanged((byte) 4, enabled);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onMicrophoneSettingChanged(boolean enabled) {
            onSettingChanged((byte) 5, enabled);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onWifiMainSettingChanged(boolean enabled) {
            onSettingChanged((byte) 2, enabled);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onWifiScanningSettingChanged(boolean enabled) {
            onSettingChanged((byte) 3, enabled);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onBtMainSettingChanged(boolean enabled) {
            onSettingChanged((byte) 6, enabled);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onBtScanningSettingChanged(boolean enabled) {
            onSettingChanged((byte) 7, enabled);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onHostEndpointConnected(HostEndpointInfo info) {
            try {
                this.mHub.onHostEndpointConnected(info);
            } catch (RemoteException | ServiceSpecificException e) {
                Log.e(IContextHubWrapper.TAG, "Exception in onHostEndpointConnected" + e.getMessage());
            }
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onHostEndpointDisconnected(short hostEndpointId) {
            try {
                this.mHub.onHostEndpointDisconnected((char) hostEndpointId);
            } catch (RemoteException | ServiceSpecificException e) {
                Log.e(IContextHubWrapper.TAG, "Exception in onHostEndpointDisconnected" + e.getMessage());
            }
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int sendMessageToContextHub(short hostEndpointId, int contextHubId, NanoAppMessage message) throws RemoteException {
            try {
                this.mHub.sendMessageToHub(contextHubId, ContextHubServiceUtil.createAidlContextHubMessage(hostEndpointId, message));
                return 0;
            } catch (RemoteException | ServiceSpecificException e) {
                return 1;
            } catch (IllegalArgumentException e2) {
                return 2;
            }
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int loadNanoapp(int contextHubId, NanoAppBinary binary, int transactionId) throws RemoteException {
            NanoappBinary aidlNanoAppBinary = ContextHubServiceUtil.createAidlNanoAppBinary(binary);
            try {
                this.mHub.loadNanoapp(contextHubId, aidlNanoAppBinary, transactionId);
                return 0;
            } catch (RemoteException | ServiceSpecificException | UnsupportedOperationException e) {
                return 1;
            } catch (IllegalArgumentException e2) {
                return 2;
            }
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int unloadNanoapp(int contextHubId, long nanoappId, int transactionId) throws RemoteException {
            try {
                this.mHub.unloadNanoapp(contextHubId, nanoappId, transactionId);
                return 0;
            } catch (RemoteException | ServiceSpecificException | UnsupportedOperationException e) {
                return 1;
            } catch (IllegalArgumentException e2) {
                return 2;
            }
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int enableNanoapp(int contextHubId, long nanoappId, int transactionId) throws RemoteException {
            try {
                this.mHub.enableNanoapp(contextHubId, nanoappId, transactionId);
                return 0;
            } catch (RemoteException | ServiceSpecificException | UnsupportedOperationException e) {
                return 1;
            } catch (IllegalArgumentException e2) {
                return 2;
            }
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int disableNanoapp(int contextHubId, long nanoappId, int transactionId) throws RemoteException {
            try {
                this.mHub.disableNanoapp(contextHubId, nanoappId, transactionId);
                return 0;
            } catch (RemoteException | ServiceSpecificException | UnsupportedOperationException e) {
                return 1;
            } catch (IllegalArgumentException e2) {
                return 2;
            }
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int queryNanoapps(int contextHubId) throws RemoteException {
            try {
                this.mHub.queryNanoapps(contextHubId);
                return 0;
            } catch (RemoteException | ServiceSpecificException | UnsupportedOperationException e) {
                return 1;
            } catch (IllegalArgumentException e2) {
                return 2;
            }
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void registerCallback(int contextHubId, ICallback callback) throws RemoteException {
            this.mAidlCallbackMap.put(Integer.valueOf(contextHubId), new ContextHubAidlCallback(contextHubId, callback));
            try {
                this.mHub.registerCallback(contextHubId, this.mAidlCallbackMap.get(Integer.valueOf(contextHubId)));
            } catch (RemoteException | ServiceSpecificException | IllegalArgumentException e) {
                Log.e(IContextHubWrapper.TAG, "Exception while registering callback: " + e.getMessage());
            }
        }

        private void onSettingChanged(byte setting, boolean enabled) {
            try {
                this.mHub.onSettingChanged(setting, enabled);
            } catch (RemoteException | ServiceSpecificException e) {
                Log.e(IContextHubWrapper.TAG, "Exception while sending setting update: " + e.getMessage());
            }
        }
    }

    /* loaded from: classes.dex */
    private static abstract class ContextHubWrapperHidl extends IContextHubWrapper {
        protected ICallback mCallback = null;
        protected final Map<Integer, ContextHubWrapperHidlCallback> mHidlCallbackMap = new HashMap();
        private IContexthub mHub;

        /* loaded from: classes.dex */
        protected class ContextHubWrapperHidlCallback extends IContexthubCallback.Stub {
            private final ICallback mCallback;
            private final int mContextHubId;

            ContextHubWrapperHidlCallback(int contextHubId, ICallback callback) {
                this.mContextHubId = contextHubId;
                this.mCallback = callback;
            }

            public void handleClientMsg(ContextHubMsg message) {
                this.mCallback.handleNanoappMessage(message.hostEndPoint, ContextHubServiceUtil.createNanoAppMessage(message), Collections.emptyList(), Collections.emptyList());
            }

            public void handleTxnResult(int transactionId, int result) {
                this.mCallback.handleTransactionResult(transactionId, result == 0);
            }

            public void handleHubEvent(int eventType) {
                this.mCallback.handleContextHubEvent(ContextHubServiceUtil.toContextHubEvent(eventType));
            }

            public void handleAppAbort(long nanoAppId, int abortCode) {
                this.mCallback.handleNanoappAbort(nanoAppId, abortCode);
            }

            public void handleAppsInfo(ArrayList<HubAppInfo> nanoAppInfoList) {
                handleAppsInfo_1_2(ContextHubServiceUtil.toHubAppInfo_1_2(nanoAppInfoList));
            }

            public void handleClientMsg_1_2(android.hardware.contexthub.V1_2.ContextHubMsg message, ArrayList<String> messagePermissions) {
                this.mCallback.handleNanoappMessage(message.msg_1_0.hostEndPoint, ContextHubServiceUtil.createNanoAppMessage(message.msg_1_0), message.permissions, messagePermissions);
            }

            public void handleAppsInfo_1_2(ArrayList<android.hardware.contexthub.V1_2.HubAppInfo> nanoAppInfoList) {
                List<NanoAppState> nanoAppStateList = ContextHubServiceUtil.createNanoAppStateList(nanoAppInfoList);
                this.mCallback.handleNanoappInfo(nanoAppStateList);
            }
        }

        ContextHubWrapperHidl(IContexthub hub) {
            this.mHub = hub;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int sendMessageToContextHub(short hostEndpointId, int contextHubId, NanoAppMessage message) throws RemoteException {
            ContextHubMsg messageToNanoApp = ContextHubServiceUtil.createHidlContextHubMessage(hostEndpointId, message);
            return ContextHubServiceUtil.toTransactionResult(this.mHub.sendMessageToHub(contextHubId, messageToNanoApp));
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int loadNanoapp(int contextHubId, NanoAppBinary binary, int transactionId) throws RemoteException {
            android.hardware.contexthub.V1_0.NanoAppBinary hidlNanoAppBinary = ContextHubServiceUtil.createHidlNanoAppBinary(binary);
            return ContextHubServiceUtil.toTransactionResult(this.mHub.loadNanoApp(contextHubId, hidlNanoAppBinary, transactionId));
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int unloadNanoapp(int contextHubId, long nanoappId, int transactionId) throws RemoteException {
            return ContextHubServiceUtil.toTransactionResult(this.mHub.unloadNanoApp(contextHubId, nanoappId, transactionId));
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int enableNanoapp(int contextHubId, long nanoappId, int transactionId) throws RemoteException {
            return ContextHubServiceUtil.toTransactionResult(this.mHub.enableNanoApp(contextHubId, nanoappId, transactionId));
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int disableNanoapp(int contextHubId, long nanoappId, int transactionId) throws RemoteException {
            return ContextHubServiceUtil.toTransactionResult(this.mHub.disableNanoApp(contextHubId, nanoappId, transactionId));
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public int queryNanoapps(int contextHubId) throws RemoteException {
            return ContextHubServiceUtil.toTransactionResult(this.mHub.queryApps(contextHubId));
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void registerCallback(int contextHubId, ICallback callback) throws RemoteException {
            this.mHidlCallbackMap.put(Integer.valueOf(contextHubId), new ContextHubWrapperHidlCallback(contextHubId, callback));
            this.mHub.registerCallback(contextHubId, this.mHidlCallbackMap.get(Integer.valueOf(contextHubId)));
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsBtSettingNotifications() {
            return false;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onWifiMainSettingChanged(boolean enabled) {
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onWifiScanningSettingChanged(boolean enabled) {
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onBtMainSettingChanged(boolean enabled) {
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onBtScanningSettingChanged(boolean enabled) {
        }
    }

    /* loaded from: classes.dex */
    private static class ContextHubWrapperV1_0 extends ContextHubWrapperHidl {
        private IContexthub mHub;

        ContextHubWrapperV1_0(IContexthub hub) {
            super(hub);
            this.mHub = hub;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public Pair<List<ContextHubInfo>, List<String>> getHubs() throws RemoteException {
            ArrayList<ContextHubInfo> hubInfoList = new ArrayList<>();
            Iterator it = this.mHub.getHubs().iterator();
            while (it.hasNext()) {
                ContextHub hub = (ContextHub) it.next();
                hubInfoList.add(new ContextHubInfo(hub));
            }
            return new Pair<>(hubInfoList, new ArrayList());
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsLocationSettingNotifications() {
            return false;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsWifiSettingNotifications() {
            return false;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsAirplaneModeSettingNotifications() {
            return false;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsMicrophoneSettingNotifications() {
            return false;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onLocationSettingChanged(boolean enabled) {
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onWifiSettingChanged(boolean enabled) {
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onAirplaneModeSettingChanged(boolean enabled) {
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onMicrophoneSettingChanged(boolean enabled) {
        }
    }

    /* loaded from: classes.dex */
    private static class ContextHubWrapperV1_1 extends ContextHubWrapperHidl {
        private android.hardware.contexthub.V1_1.IContexthub mHub;

        ContextHubWrapperV1_1(android.hardware.contexthub.V1_1.IContexthub hub) {
            super(hub);
            this.mHub = hub;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public Pair<List<ContextHubInfo>, List<String>> getHubs() throws RemoteException {
            ArrayList<ContextHubInfo> hubInfoList = new ArrayList<>();
            Iterator it = this.mHub.getHubs().iterator();
            while (it.hasNext()) {
                ContextHub hub = (ContextHub) it.next();
                hubInfoList.add(new ContextHubInfo(hub));
            }
            return new Pair<>(hubInfoList, new ArrayList());
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsLocationSettingNotifications() {
            return true;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsWifiSettingNotifications() {
            return false;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsAirplaneModeSettingNotifications() {
            return false;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsMicrophoneSettingNotifications() {
            return false;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onLocationSettingChanged(boolean enabled) {
            try {
                this.mHub.onSettingChanged((byte) 0, enabled ? (byte) 1 : (byte) 0);
            } catch (RemoteException e) {
                Log.e(IContextHubWrapper.TAG, "Failed to send setting change to Contexthub", e);
            }
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onWifiSettingChanged(boolean enabled) {
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onAirplaneModeSettingChanged(boolean enabled) {
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onMicrophoneSettingChanged(boolean enabled) {
        }
    }

    /* loaded from: classes.dex */
    private static class ContextHubWrapperV1_2 extends ContextHubWrapperHidl implements IContexthub.getHubs_1_2Callback {
        private final android.hardware.contexthub.V1_2.IContexthub mHub;
        private Pair<List<ContextHubInfo>, List<String>> mHubInfo;

        ContextHubWrapperV1_2(android.hardware.contexthub.V1_2.IContexthub hub) {
            super(hub);
            this.mHubInfo = new Pair<>(Collections.emptyList(), Collections.emptyList());
            this.mHub = hub;
        }

        public void onValues(ArrayList<ContextHub> hubs, ArrayList<String> supportedPermissions) {
            ArrayList<ContextHubInfo> hubInfoList = new ArrayList<>();
            Iterator<ContextHub> it = hubs.iterator();
            while (it.hasNext()) {
                ContextHub hub = it.next();
                hubInfoList.add(new ContextHubInfo(hub));
            }
            this.mHubInfo = new Pair<>(hubInfoList, supportedPermissions);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public Pair<List<ContextHubInfo>, List<String>> getHubs() throws RemoteException {
            this.mHub.getHubs_1_2(this);
            return this.mHubInfo;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsLocationSettingNotifications() {
            return true;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsWifiSettingNotifications() {
            return true;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsAirplaneModeSettingNotifications() {
            return true;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public boolean supportsMicrophoneSettingNotifications() {
            return true;
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onLocationSettingChanged(boolean enabled) {
            sendSettingChanged((byte) 0, enabled ? (byte) 1 : (byte) 0);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onWifiSettingChanged(boolean enabled) {
            sendSettingChanged((byte) 1, enabled ? (byte) 1 : (byte) 0);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onAirplaneModeSettingChanged(boolean enabled) {
            sendSettingChanged((byte) 2, enabled ? (byte) 1 : (byte) 0);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper
        public void onMicrophoneSettingChanged(boolean enabled) {
            sendSettingChanged((byte) 3, enabled ? (byte) 1 : (byte) 0);
        }

        @Override // com.android.server.location.contexthub.IContextHubWrapper.ContextHubWrapperHidl, com.android.server.location.contexthub.IContextHubWrapper
        public void registerCallback(int contextHubId, ICallback callback) throws RemoteException {
            this.mHidlCallbackMap.put(Integer.valueOf(contextHubId), new ContextHubWrapperHidl.ContextHubWrapperHidlCallback(contextHubId, callback));
            this.mHub.registerCallback_1_2(contextHubId, this.mHidlCallbackMap.get(Integer.valueOf(contextHubId)));
        }

        private void sendSettingChanged(byte setting, byte newValue) {
            try {
                this.mHub.onSettingChanged_1_2(setting, newValue);
            } catch (RemoteException e) {
                Log.e(IContextHubWrapper.TAG, "Failed to send setting change to Contexthub", e);
            }
        }
    }
}
