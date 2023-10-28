package com.android.server.location.contexthub;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.compat.Compatibility;
import android.content.Context;
import android.content.Intent;
import android.hardware.contexthub.HostEndpointInfo;
import android.hardware.location.ContextHubInfo;
import android.hardware.location.IContextHubClient;
import android.hardware.location.IContextHubClientCallback;
import android.hardware.location.IContextHubTransactionCallback;
import android.hardware.location.NanoAppMessage;
import android.hardware.location.NanoAppState;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
/* loaded from: classes.dex */
public class ContextHubClientBroker extends IContextHubClient.Stub implements IBinder.DeathRecipient, AppOpsManager.OnOpChangedListener {
    private static final int AUTHORIZATION_UNKNOWN = -1;
    private static final long CHANGE_ID_AUTH_STATE_DENIED = 181350407;
    private static final String RECEIVE_MSG_NOTE = "NanoappMessageDelivery ";
    private static final String TAG = "ContextHubClientBroker";
    private final AppOpsManager mAppOpsManager;
    private final ContextHubInfo mAttachedContextHubInfo;
    private String mAttributionTag;
    private IContextHubClientCallback mCallbackInterface;
    private final ContextHubClientManager mClientManager;
    private final Context mContext;
    private final IContextHubWrapper mContextHubProxy;
    private final Set<Long> mForceDeniedNapps;
    private final short mHostEndPointId;
    private AtomicBoolean mIsPendingIntentCancelled;
    private AtomicBoolean mIsPermQueryIssued;
    private final Map<Long, Integer> mMessageChannelNanoappIdMap;
    private final Map<Long, AuthStateDenialTimer> mNappToAuthTimerMap;
    private final String mPackage;
    private final PendingIntentRequest mPendingIntentRequest;
    private final int mPid;
    private final IContextHubTransactionCallback mQueryPermsCallback;
    private boolean mRegistered;
    private final ContextHubTransactionManager mTransactionManager;
    private final int mUid;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface CallbackConsumer {
        void accept(IContextHubClientCallback iContextHubClientCallback) throws RemoteException;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PendingIntentRequest {
        private long mNanoAppId;
        private PendingIntent mPendingIntent;
        private boolean mValid;

        PendingIntentRequest() {
            this.mValid = false;
        }

        PendingIntentRequest(PendingIntent pendingIntent, long nanoAppId) {
            this.mValid = false;
            this.mPendingIntent = pendingIntent;
            this.mNanoAppId = nanoAppId;
            this.mValid = true;
        }

        public long getNanoAppId() {
            return this.mNanoAppId;
        }

        public PendingIntent getPendingIntent() {
            return this.mPendingIntent;
        }

        public boolean hasPendingIntent() {
            return this.mPendingIntent != null;
        }

        public void clear() {
            this.mPendingIntent = null;
        }

        public boolean isValid() {
            return this.mValid;
        }
    }

    private ContextHubClientBroker(Context context, IContextHubWrapper contextHubProxy, ContextHubClientManager clientManager, ContextHubInfo contextHubInfo, short hostEndPointId, IContextHubClientCallback callback, String attributionTag, ContextHubTransactionManager transactionManager, PendingIntent pendingIntent, long nanoAppId, String packageName) {
        this.mCallbackInterface = null;
        this.mRegistered = true;
        this.mIsPendingIntentCancelled = new AtomicBoolean(false);
        this.mIsPermQueryIssued = new AtomicBoolean(false);
        this.mMessageChannelNanoappIdMap = new ConcurrentHashMap();
        this.mForceDeniedNapps = new HashSet();
        this.mNappToAuthTimerMap = new ConcurrentHashMap();
        this.mQueryPermsCallback = new IContextHubTransactionCallback.Stub() { // from class: com.android.server.location.contexthub.ContextHubClientBroker.1
            public void onTransactionComplete(int result) {
            }

            public void onQueryResponse(int result, List<NanoAppState> nanoAppStateList) {
                ContextHubClientBroker.this.mIsPermQueryIssued.set(false);
                if (result != 0 && nanoAppStateList != null) {
                    Log.e(ContextHubClientBroker.TAG, "Permissions query failed, but still received nanoapp state");
                } else if (nanoAppStateList != null) {
                    for (NanoAppState state : nanoAppStateList) {
                        if (ContextHubClientBroker.this.mMessageChannelNanoappIdMap.containsKey(Long.valueOf(state.getNanoAppId()))) {
                            List<String> permissions = state.getNanoAppPermissions();
                            ContextHubClientBroker.this.updateNanoAppAuthState(state.getNanoAppId(), permissions, false);
                        }
                    }
                }
            }
        };
        this.mContext = context;
        this.mContextHubProxy = contextHubProxy;
        this.mClientManager = clientManager;
        this.mAttachedContextHubInfo = contextHubInfo;
        this.mHostEndPointId = hostEndPointId;
        this.mCallbackInterface = callback;
        if (pendingIntent == null) {
            this.mPendingIntentRequest = new PendingIntentRequest();
        } else {
            this.mPendingIntentRequest = new PendingIntentRequest(pendingIntent, nanoAppId);
        }
        this.mPackage = packageName;
        this.mAttributionTag = attributionTag;
        this.mTransactionManager = transactionManager;
        this.mPid = Binder.getCallingPid();
        this.mUid = Binder.getCallingUid();
        this.mAppOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        startMonitoringOpChanges();
        sendHostEndpointConnectedEvent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubClientBroker(Context context, IContextHubWrapper contextHubProxy, ContextHubClientManager clientManager, ContextHubInfo contextHubInfo, short hostEndPointId, IContextHubClientCallback callback, String attributionTag, ContextHubTransactionManager transactionManager, String packageName) {
        this(context, contextHubProxy, clientManager, contextHubInfo, hostEndPointId, callback, attributionTag, transactionManager, null, 0L, packageName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubClientBroker(Context context, IContextHubWrapper contextHubProxy, ContextHubClientManager clientManager, ContextHubInfo contextHubInfo, short hostEndPointId, PendingIntent pendingIntent, long nanoAppId, String attributionTag, ContextHubTransactionManager transactionManager) {
        this(context, contextHubProxy, clientManager, contextHubInfo, hostEndPointId, null, attributionTag, transactionManager, pendingIntent, nanoAppId, pendingIntent.getCreatorPackage());
    }

    private void startMonitoringOpChanges() {
        this.mAppOpsManager.startWatchingMode(-1, this.mPackage, this);
    }

    public int sendMessageToNanoApp(NanoAppMessage message) {
        ContextHubServiceUtil.checkPermissions(this.mContext);
        if (isRegistered()) {
            int authState = this.mMessageChannelNanoappIdMap.getOrDefault(Long.valueOf(message.getNanoAppId()), -1).intValue();
            if (authState == 0) {
                if (Compatibility.isChangeEnabled((long) CHANGE_ID_AUTH_STATE_DENIED)) {
                    throw new SecurityException("Client doesn't have valid permissions to send message to " + message.getNanoAppId());
                }
                return 1;
            }
            if (authState == -1) {
                checkNanoappPermsAsync();
            }
            try {
                int result = this.mContextHubProxy.sendMessageToContextHub(this.mHostEndPointId, this.mAttachedContextHubInfo.getId(), message);
                return result;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in sendMessageToNanoApp (target hub ID = " + this.mAttachedContextHubInfo.getId() + ")", e);
                return 1;
            }
        }
        Log.e(TAG, "Failed to send message to nanoapp: client connection is closed");
        return 1;
    }

    public void close() {
        synchronized (this) {
            this.mPendingIntentRequest.clear();
        }
        onClientExit();
    }

    public int getId() {
        return this.mHostEndPointId;
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        onClientExit();
    }

    @Override // android.app.AppOpsManager.OnOpChangedListener
    public void onOpChanged(String op, String packageName) {
        if (packageName.equals(this.mPackage) && !this.mMessageChannelNanoappIdMap.isEmpty()) {
            checkNanoappPermsAsync();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getPackageName() {
        return this.mPackage;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAttributionTag(String attributionTag) {
        this.mAttributionTag = attributionTag;
    }

    String getAttributionTag() {
        return this.mAttributionTag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getAttachedContextHubId() {
        return this.mAttachedContextHubInfo.getId();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public short getHostEndPointId() {
        return this.mHostEndPointId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendMessageToClient(final NanoAppMessage message, List<String> nanoappPermissions, List<String> messagePermissions) {
        final long nanoAppId = message.getNanoAppId();
        int authState = updateNanoAppAuthState(nanoAppId, nanoappPermissions, false);
        if (authState == 1 && !messagePermissions.isEmpty()) {
            Log.e(TAG, "Dropping message from " + Long.toHexString(nanoAppId) + ". " + this.mPackage + " in grace period and napp msg has permissions");
        } else if (authState == 0 || !notePermissions(messagePermissions, RECEIVE_MSG_NOTE + nanoAppId)) {
            Log.e(TAG, "Dropping message from " + Long.toHexString(nanoAppId) + ". " + this.mPackage + " doesn't have permission");
        } else {
            invokeCallback(new CallbackConsumer() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda0
                @Override // com.android.server.location.contexthub.ContextHubClientBroker.CallbackConsumer
                public final void accept(IContextHubClientCallback iContextHubClientCallback) {
                    iContextHubClientCallback.onMessageFromNanoApp(message);
                }
            });
            Supplier<Intent> supplier = new Supplier() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda1
                @Override // java.util.function.Supplier
                public final Object get() {
                    return ContextHubClientBroker.this.m4293xd13f804a(nanoAppId, message);
                }
            };
            sendPendingIntent(supplier, nanoAppId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendMessageToClient$1$com-android-server-location-contexthub-ContextHubClientBroker  reason: not valid java name */
    public /* synthetic */ Intent m4293xd13f804a(long nanoAppId, NanoAppMessage message) {
        return createIntent(5, nanoAppId).putExtra("android.hardware.location.extra.MESSAGE", (Parcelable) message);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onNanoAppLoaded(final long nanoAppId) {
        checkNanoappPermsAsync();
        invokeCallback(new CallbackConsumer() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda6
            @Override // com.android.server.location.contexthub.ContextHubClientBroker.CallbackConsumer
            public final void accept(IContextHubClientCallback iContextHubClientCallback) {
                iContextHubClientCallback.onNanoAppLoaded(nanoAppId);
            }
        });
        sendPendingIntent(new Supplier() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda7
            @Override // java.util.function.Supplier
            public final Object get() {
                return ContextHubClientBroker.this.m4290x4d50353a(nanoAppId);
            }
        }, nanoAppId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onNanoAppLoaded$3$com-android-server-location-contexthub-ContextHubClientBroker  reason: not valid java name */
    public /* synthetic */ Intent m4290x4d50353a(long nanoAppId) {
        return createIntent(0, nanoAppId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onNanoAppUnloaded(final long nanoAppId) {
        invokeCallback(new CallbackConsumer() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda4
            @Override // com.android.server.location.contexthub.ContextHubClientBroker.CallbackConsumer
            public final void accept(IContextHubClientCallback iContextHubClientCallback) {
                iContextHubClientCallback.onNanoAppUnloaded(nanoAppId);
            }
        });
        sendPendingIntent(new Supplier() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda5
            @Override // java.util.function.Supplier
            public final Object get() {
                return ContextHubClientBroker.this.m4291xd25a0555(nanoAppId);
            }
        }, nanoAppId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onNanoAppUnloaded$5$com-android-server-location-contexthub-ContextHubClientBroker  reason: not valid java name */
    public /* synthetic */ Intent m4291xd25a0555(long nanoAppId) {
        return createIntent(1, nanoAppId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onHubReset() {
        invokeCallback(new CallbackConsumer() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda8
            @Override // com.android.server.location.contexthub.ContextHubClientBroker.CallbackConsumer
            public final void accept(IContextHubClientCallback iContextHubClientCallback) {
                iContextHubClientCallback.onHubReset();
            }
        });
        sendPendingIntent(new Supplier() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda9
            @Override // java.util.function.Supplier
            public final Object get() {
                return ContextHubClientBroker.this.m4288x441028de();
            }
        });
        sendHostEndpointConnectedEvent();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onHubReset$7$com-android-server-location-contexthub-ContextHubClientBroker  reason: not valid java name */
    public /* synthetic */ Intent m4288x441028de() {
        return createIntent(6);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onNanoAppAborted(final long nanoAppId, final int abortCode) {
        invokeCallback(new CallbackConsumer() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda10
            @Override // com.android.server.location.contexthub.ContextHubClientBroker.CallbackConsumer
            public final void accept(IContextHubClientCallback iContextHubClientCallback) {
                iContextHubClientCallback.onNanoAppAborted(nanoAppId, abortCode);
            }
        });
        Supplier<Intent> supplier = new Supplier() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda11
            @Override // java.util.function.Supplier
            public final Object get() {
                return ContextHubClientBroker.this.m4289x885eb48(nanoAppId, abortCode);
            }
        };
        sendPendingIntent(supplier, nanoAppId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onNanoAppAborted$9$com-android-server-location-contexthub-ContextHubClientBroker  reason: not valid java name */
    public /* synthetic */ Intent m4289x885eb48(long nanoAppId, int abortCode) {
        return createIntent(4, nanoAppId).putExtra("android.hardware.location.extra.NANOAPP_ABORT_CODE", abortCode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasPendingIntent(PendingIntent intent, long nanoAppId) {
        PendingIntent pendingIntent;
        long intentNanoAppId;
        synchronized (this) {
            pendingIntent = this.mPendingIntentRequest.getPendingIntent();
            intentNanoAppId = this.mPendingIntentRequest.getNanoAppId();
        }
        return pendingIntent != null && pendingIntent.equals(intent) && intentNanoAppId == nanoAppId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void attachDeathRecipient() throws RemoteException {
        IContextHubClientCallback iContextHubClientCallback = this.mCallbackInterface;
        if (iContextHubClientCallback != null) {
            iContextHubClientCallback.asBinder().linkToDeath(this, 0);
        }
    }

    boolean hasPermissions(List<String> permissions) {
        for (String permission : permissions) {
            if (this.mContext.checkPermission(permission, this.mPid, this.mUid) != 0) {
                return false;
            }
        }
        return true;
    }

    boolean notePermissions(List<String> permissions, String noteMessage) {
        for (String permission : permissions) {
            int opCode = AppOpsManager.permissionToOpCode(permission);
            if (opCode != -1) {
                try {
                    if (this.mAppOpsManager.noteOp(opCode, this.mUid, this.mPackage, this.mAttributionTag, noteMessage) != 0) {
                        return false;
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException: noteOp for pkg " + this.mPackage + " opcode " + opCode + ": " + e.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPendingIntentCancelled() {
        return this.mIsPendingIntentCancelled.get();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleAuthStateTimerExpiry(long nanoAppId) {
        AuthStateDenialTimer timer;
        synchronized (this.mMessageChannelNanoappIdMap) {
            timer = this.mNappToAuthTimerMap.remove(Long.valueOf(nanoAppId));
        }
        if (timer != null) {
            updateNanoAppAuthState(nanoAppId, Collections.emptyList(), true);
        }
    }

    private void checkNanoappPermsAsync() {
        if (!this.mIsPermQueryIssued.getAndSet(true)) {
            ContextHubServiceTransaction transaction = this.mTransactionManager.createQueryTransaction(this.mAttachedContextHubInfo.getId(), this.mQueryPermsCallback, this.mPackage);
            this.mTransactionManager.addTransaction(transaction);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int updateNanoAppAuthState(long nanoAppId, List<String> nanoappPermissions, boolean gracePeriodExpired) {
        return updateNanoAppAuthState(nanoAppId, nanoappPermissions, gracePeriodExpired, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:23:0x005c A[Catch: all -> 0x009d, TryCatch #0 {, blocks: (B:4:0x0003, B:6:0x001f, B:9:0x0031, B:23:0x005c, B:25:0x006a, B:30:0x0089, B:31:0x0096, B:27:0x0070, B:21:0x0050), top: B:38:0x0003 }] */
    /* JADX WARN: Removed duplicated region for block: B:26:0x006e  */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0089 A[Catch: all -> 0x009d, TryCatch #0 {, blocks: (B:4:0x0003, B:6:0x001f, B:9:0x0031, B:23:0x005c, B:25:0x006a, B:30:0x0089, B:31:0x0096, B:27:0x0070, B:21:0x0050), top: B:38:0x0003 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int updateNanoAppAuthState(long nanoAppId, List<String> nanoappPermissions, boolean gracePeriodExpired, boolean forceDenied) {
        int curAuthState;
        int newAuthState;
        synchronized (this.mMessageChannelNanoappIdMap) {
            boolean hasPermissions = hasPermissions(nanoappPermissions);
            curAuthState = this.mMessageChannelNanoappIdMap.getOrDefault(Long.valueOf(nanoAppId), -1).intValue();
            if (curAuthState == -1) {
                curAuthState = 2;
                this.mMessageChannelNanoappIdMap.put(Long.valueOf(nanoAppId), 2);
            }
            newAuthState = curAuthState;
            if (!forceDenied && !this.mForceDeniedNapps.contains(Long.valueOf(nanoAppId))) {
                if (gracePeriodExpired) {
                    if (curAuthState == 1) {
                        newAuthState = 0;
                    }
                } else if (curAuthState == 2 && !hasPermissions) {
                    newAuthState = 1;
                } else if (curAuthState != 2 && hasPermissions) {
                    newAuthState = 2;
                }
                if (newAuthState == 1) {
                    AuthStateDenialTimer timer = this.mNappToAuthTimerMap.remove(Long.valueOf(nanoAppId));
                    if (timer != null) {
                        timer.cancel();
                    }
                } else if (curAuthState == 2) {
                    AuthStateDenialTimer timer2 = new AuthStateDenialTimer(this, nanoAppId, Looper.getMainLooper());
                    this.mNappToAuthTimerMap.put(Long.valueOf(nanoAppId), timer2);
                    timer2.start();
                }
                if (curAuthState != newAuthState) {
                    this.mMessageChannelNanoappIdMap.put(Long.valueOf(nanoAppId), Integer.valueOf(newAuthState));
                }
            }
            newAuthState = 0;
            this.mForceDeniedNapps.add(Long.valueOf(nanoAppId));
            if (newAuthState == 1) {
            }
            if (curAuthState != newAuthState) {
            }
        }
        if (curAuthState != newAuthState) {
            sendAuthStateCallback(nanoAppId, newAuthState);
        }
        return newAuthState;
    }

    private void sendAuthStateCallback(final long nanoAppId, final int authState) {
        invokeCallback(new CallbackConsumer() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda2
            @Override // com.android.server.location.contexthub.ContextHubClientBroker.CallbackConsumer
            public final void accept(IContextHubClientCallback iContextHubClientCallback) {
                iContextHubClientCallback.onClientAuthorizationChanged(nanoAppId, authState);
            }
        });
        Supplier<Intent> supplier = new Supplier() { // from class: com.android.server.location.contexthub.ContextHubClientBroker$$ExternalSyntheticLambda3
            @Override // java.util.function.Supplier
            public final Object get() {
                return ContextHubClientBroker.this.m4292xc7b9ae30(nanoAppId, authState);
            }
        };
        sendPendingIntent(supplier, nanoAppId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$sendAuthStateCallback$11$com-android-server-location-contexthub-ContextHubClientBroker  reason: not valid java name */
    public /* synthetic */ Intent m4292xc7b9ae30(long nanoAppId, int authState) {
        return createIntent(7, nanoAppId).putExtra("android.hardware.location.extra.CLIENT_AUTHORIZATION_STATE", authState);
    }

    private synchronized void invokeCallback(CallbackConsumer consumer) {
        IContextHubClientCallback iContextHubClientCallback = this.mCallbackInterface;
        if (iContextHubClientCallback != null) {
            try {
                consumer.accept(iContextHubClientCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while invoking client callback (host endpoint ID = " + ((int) this.mHostEndPointId) + ")", e);
            }
        }
    }

    private Intent createIntent(int eventType) {
        Intent intent = new Intent();
        intent.putExtra("android.hardware.location.extra.EVENT_TYPE", eventType);
        intent.putExtra("android.hardware.location.extra.CONTEXT_HUB_INFO", (Parcelable) this.mAttachedContextHubInfo);
        return intent;
    }

    private Intent createIntent(int eventType, long nanoAppId) {
        Intent intent = createIntent(eventType);
        intent.putExtra("android.hardware.location.extra.NANOAPP_ID", nanoAppId);
        return intent;
    }

    private synchronized void sendPendingIntent(Supplier<Intent> supplier) {
        if (this.mPendingIntentRequest.hasPendingIntent()) {
            doSendPendingIntent(this.mPendingIntentRequest.getPendingIntent(), supplier.get());
        }
    }

    private synchronized void sendPendingIntent(Supplier<Intent> supplier, long nanoAppId) {
        if (this.mPendingIntentRequest.hasPendingIntent() && this.mPendingIntentRequest.getNanoAppId() == nanoAppId) {
            doSendPendingIntent(this.mPendingIntentRequest.getPendingIntent(), supplier.get());
        }
    }

    private void doSendPendingIntent(PendingIntent pendingIntent, Intent intent) {
        try {
            pendingIntent.send(this.mContext, 0, intent, null, null, "android.permission.ACCESS_CONTEXT_HUB", null);
        } catch (PendingIntent.CanceledException e) {
            this.mIsPendingIntentCancelled.set(true);
            Log.w(TAG, "PendingIntent has been canceled, unregistering from client (host endpoint ID " + ((int) this.mHostEndPointId) + ")");
            close();
        }
    }

    private synchronized boolean isRegistered() {
        return this.mRegistered;
    }

    private synchronized void onClientExit() {
        IContextHubClientCallback iContextHubClientCallback = this.mCallbackInterface;
        if (iContextHubClientCallback != null) {
            iContextHubClientCallback.asBinder().unlinkToDeath(this, 0);
            this.mCallbackInterface = null;
        }
        if (!this.mPendingIntentRequest.hasPendingIntent() && this.mRegistered) {
            this.mClientManager.unregisterClient(this.mHostEndPointId);
            this.mRegistered = false;
        }
        this.mAppOpsManager.stopWatchingMode(this);
        this.mContextHubProxy.onHostEndpointDisconnected(this.mHostEndPointId);
    }

    private String authStateToString(int state) {
        switch (state) {
            case 0:
                return "DENIED";
            case 1:
                return "DENIED_GRACE_PERIOD";
            case 2:
                return "GRANTED";
            default:
                return "UNKNOWN";
        }
    }

    private void sendHostEndpointConnectedEvent() {
        int i;
        HostEndpointInfo info = new HostEndpointInfo();
        info.hostEndpointId = (char) this.mHostEndPointId;
        info.packageName = this.mPackage;
        info.attributionTag = this.mAttributionTag;
        if (this.mUid == 1000) {
            i = 1;
        } else {
            i = 2;
        }
        info.type = i;
        this.mContextHubProxy.onHostEndpointConnected(info);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(ProtoOutputStream proto) {
        proto.write(CompanionMessage.MESSAGE_ID, (int) getHostEndPointId());
        proto.write(1120986464258L, getAttachedContextHubId());
        proto.write(1138166333443L, this.mPackage);
        if (this.mPendingIntentRequest.isValid()) {
            proto.write(1133871366149L, true);
            proto.write(1112396529668L, this.mPendingIntentRequest.getNanoAppId());
        }
        proto.write(1133871366150L, this.mPendingIntentRequest.hasPendingIntent());
        proto.write(1133871366151L, isPendingIntentCancelled());
        proto.write(1133871366152L, this.mRegistered);
    }

    public String toString() {
        String out;
        String out2 = ("[ContextHubClient endpointID: " + ((int) getHostEndPointId()) + ", ") + "contextHub: " + getAttachedContextHubId() + ", ";
        if (this.mAttributionTag != null) {
            out2 = out2 + "attributionTag: " + getAttributionTag() + ", ";
        }
        if (this.mPendingIntentRequest.isValid()) {
            out = (out2 + "intentCreatorPackage: " + this.mPackage + ", ") + "nanoAppId: 0x" + Long.toHexString(this.mPendingIntentRequest.getNanoAppId());
        } else {
            out = out2 + "package: " + this.mPackage;
        }
        if (this.mMessageChannelNanoappIdMap.size() > 0) {
            String out3 = out + " messageChannelNanoappSet: (";
            Iterator<Map.Entry<Long, Integer>> it = this.mMessageChannelNanoappIdMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Integer> entry = it.next();
                out3 = out3 + "0x" + Long.toHexString(entry.getKey().longValue()) + " auth state: " + authStateToString(entry.getValue().intValue());
                if (it.hasNext()) {
                    out3 = out3 + ",";
                }
            }
            out = out3 + ")";
        }
        return out + "]";
    }
}
