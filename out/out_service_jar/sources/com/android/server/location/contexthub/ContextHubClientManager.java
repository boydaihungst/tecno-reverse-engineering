package com.android.server.location.contexthub;

import android.app.PendingIntent;
import android.content.Context;
import android.hardware.location.ContextHubInfo;
import android.hardware.location.IContextHubClient;
import android.hardware.location.IContextHubClientCallback;
import android.hardware.location.NanoAppMessage;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ContextHubClientManager {
    public static final int ACTION_CANCELLED = 2;
    public static final int ACTION_REGISTERED = 0;
    public static final int ACTION_UNREGISTERED = 1;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm:ss.SSS");
    private static final boolean DEBUG_LOG_ENABLED = false;
    private static final int MAX_CLIENT_ID = 32767;
    private static final int NUM_CLIENT_RECORDS = 20;
    private static final String TAG = "ContextHubClientManager";
    private final Context mContext;
    private final IContextHubWrapper mContextHubProxy;
    private final ConcurrentHashMap<Short, ContextHubClientBroker> mHostEndPointIdToClientMap = new ConcurrentHashMap<>();
    private int mNextHostEndPointId = 0;
    private final ConcurrentLinkedEvictingDeque<RegistrationRecord> mRegistrationRecordDeque = new ConcurrentLinkedEvictingDeque<>(20);

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface Action {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class RegistrationRecord {
        private final int mAction;
        private final String mBroker;
        private final long mTimestamp = System.currentTimeMillis();

        RegistrationRecord(String broker, int action) {
            this.mBroker = broker;
            this.mAction = action;
        }

        void dump(ProtoOutputStream proto) {
            proto.write(1112396529665L, this.mTimestamp);
            proto.write(1120986464258L, this.mAction);
            proto.write(1138166333443L, this.mBroker);
        }

        public String toString() {
            String out = (("" + ContextHubClientManager.DATE_FORMAT.format(new Date(this.mTimestamp)) + " ") + (this.mAction == 0 ? "+ " : "- ")) + this.mBroker;
            if (this.mAction == 2) {
                return out + " (cancelled)";
            }
            return out;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContextHubClientManager(Context context, IContextHubWrapper contextHubProxy) {
        this.mContext = context;
        this.mContextHubProxy = contextHubProxy;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v2, resolved type: java.util.concurrent.ConcurrentHashMap<java.lang.Short, com.android.server.location.contexthub.ContextHubClientBroker> */
    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v1, types: [com.android.server.location.contexthub.ContextHubClientBroker, java.lang.Object, android.os.IBinder] */
    public IContextHubClient registerClient(ContextHubInfo contextHubInfo, IContextHubClientCallback clientCallback, String attributionTag, ContextHubTransactionManager transactionManager, String packageName) {
        ?? contextHubClientBroker;
        synchronized (this) {
            short hostEndPointId = getHostEndPointId();
            contextHubClientBroker = new ContextHubClientBroker(this.mContext, this.mContextHubProxy, this, contextHubInfo, hostEndPointId, clientCallback, attributionTag, transactionManager, packageName);
            this.mHostEndPointIdToClientMap.put(Short.valueOf(hostEndPointId), contextHubClientBroker);
            this.mRegistrationRecordDeque.add(new RegistrationRecord(contextHubClientBroker.toString(), 0));
        }
        try {
            contextHubClientBroker.attachDeathRecipient();
            Log.d(TAG, "Registered client with host endpoint ID " + ((int) contextHubClientBroker.getHostEndPointId()));
            return IContextHubClient.Stub.asInterface((IBinder) contextHubClientBroker);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to attach death recipient to client");
            contextHubClientBroker.close();
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Type inference failed for: r0v10 */
    /* JADX WARN: Type inference failed for: r0v7, types: [com.android.server.location.contexthub.ContextHubClientBroker, android.os.IBinder] */
    /* JADX WARN: Type inference failed for: r0v9 */
    public IContextHubClient registerClient(ContextHubInfo contextHubInfo, PendingIntent pendingIntent, long nanoAppId, String attributionTag, ContextHubTransactionManager transactionManager) {
        ?? r0;
        String registerString = "Regenerated";
        synchronized (this) {
            try {
                try {
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
                try {
                    ContextHubClientBroker broker = getClientBroker(contextHubInfo.getId(), pendingIntent, nanoAppId);
                    if (broker == null) {
                        short hostEndPointId = getHostEndPointId();
                        ContextHubClientBroker broker2 = new ContextHubClientBroker(this.mContext, this.mContextHubProxy, this, contextHubInfo, hostEndPointId, pendingIntent, nanoAppId, attributionTag, transactionManager);
                        this.mHostEndPointIdToClientMap.put(Short.valueOf(hostEndPointId), broker2);
                        registerString = "Registered";
                        this.mRegistrationRecordDeque.add(new RegistrationRecord(broker2.toString(), 0));
                        r0 = broker2;
                    } else {
                        broker.setAttributionTag(attributionTag);
                        r0 = broker;
                    }
                    Log.d(TAG, registerString + " client with host endpoint ID " + ((int) r0.getHostEndPointId()));
                    return IContextHubClient.Stub.asInterface((IBinder) r0);
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onMessageFromNanoApp(int contextHubId, short hostEndpointId, NanoAppMessage message, List<String> nanoappPermissions, List<String> messagePermissions) {
        if (message.isBroadcastMessage()) {
            if (!messagePermissions.isEmpty()) {
                Log.wtf(TAG, "Received broadcast message with permissions from " + message.getNanoAppId());
            }
            broadcastMessage(contextHubId, message, nanoappPermissions, messagePermissions);
            return;
        }
        ContextHubClientBroker proxy = this.mHostEndPointIdToClientMap.get(Short.valueOf(hostEndpointId));
        if (proxy != null) {
            proxy.sendMessageToClient(message, nanoappPermissions, messagePermissions);
        } else {
            Log.e(TAG, "Cannot send message to unregistered client (host endpoint ID = " + ((int) hostEndpointId) + ")");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterClient(short hostEndPointId) {
        ContextHubClientBroker broker = this.mHostEndPointIdToClientMap.get(Short.valueOf(hostEndPointId));
        if (broker != null) {
            int action = broker.isPendingIntentCancelled() ? 2 : 1;
            this.mRegistrationRecordDeque.add(new RegistrationRecord(broker.toString(), action));
        }
        if (this.mHostEndPointIdToClientMap.remove(Short.valueOf(hostEndPointId)) != null) {
            Log.d(TAG, "Unregistered client with host endpoint ID " + ((int) hostEndPointId));
        } else {
            Log.e(TAG, "Cannot unregister non-existing client with host endpoint ID " + ((int) hostEndPointId));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onNanoAppLoaded(int contextHubId, final long nanoAppId) {
        forEachClientOfHub(contextHubId, new Consumer() { // from class: com.android.server.location.contexthub.ContextHubClientManager$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ContextHubClientBroker) obj).onNanoAppLoaded(nanoAppId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onNanoAppUnloaded(int contextHubId, final long nanoAppId) {
        forEachClientOfHub(contextHubId, new Consumer() { // from class: com.android.server.location.contexthub.ContextHubClientManager$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ContextHubClientBroker) obj).onNanoAppUnloaded(nanoAppId);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onHubReset(int contextHubId) {
        forEachClientOfHub(contextHubId, new Consumer() { // from class: com.android.server.location.contexthub.ContextHubClientManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ContextHubClientBroker) obj).onHubReset();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onNanoAppAborted(int contextHubId, final long nanoAppId, final int abortCode) {
        forEachClientOfHub(contextHubId, new Consumer() { // from class: com.android.server.location.contexthub.ContextHubClientManager$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ContextHubClientBroker) obj).onNanoAppAborted(nanoAppId, abortCode);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forEachClientOfHub(int contextHubId, Consumer<ContextHubClientBroker> callback) {
        for (ContextHubClientBroker broker : this.mHostEndPointIdToClientMap.values()) {
            if (broker.getAttachedContextHubId() == contextHubId) {
                callback.accept(broker);
            }
        }
    }

    private short getHostEndPointId() {
        if (this.mHostEndPointIdToClientMap.size() == 32768) {
            throw new IllegalStateException("Could not register client - max limit exceeded");
        }
        int id = this.mNextHostEndPointId;
        int i = 0;
        while (true) {
            if (i > MAX_CLIENT_ID) {
                break;
            }
            if (!this.mHostEndPointIdToClientMap.containsKey(Short.valueOf((short) id))) {
                this.mNextHostEndPointId = id != MAX_CLIENT_ID ? id + 1 : 0;
            } else {
                if (id != MAX_CLIENT_ID) {
                    r4 = id + 1;
                }
                id = r4;
                i++;
            }
        }
        return (short) id;
    }

    private void broadcastMessage(int contextHubId, final NanoAppMessage message, final List<String> nanoappPermissions, final List<String> messagePermissions) {
        forEachClientOfHub(contextHubId, new Consumer() { // from class: com.android.server.location.contexthub.ContextHubClientManager$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ContextHubClientBroker) obj).sendMessageToClient(message, nanoappPermissions, messagePermissions);
            }
        });
    }

    private ContextHubClientBroker getClientBroker(int contextHubId, PendingIntent pendingIntent, long nanoAppId) {
        for (ContextHubClientBroker broker : this.mHostEndPointIdToClientMap.values()) {
            if (broker.hasPendingIntent(pendingIntent, nanoAppId) && broker.getAttachedContextHubId() == contextHubId) {
                return broker;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(ProtoOutputStream proto) {
        for (ContextHubClientBroker broker : this.mHostEndPointIdToClientMap.values()) {
            long token = proto.start(CompanionAppsPermissions.APP_PERMISSIONS);
            broker.dump(proto);
            proto.end(token);
        }
        Iterator<RegistrationRecord> it = this.mRegistrationRecordDeque.descendingIterator();
        while (it.hasNext()) {
            long token2 = proto.start(2246267895810L);
            it.next().dump(proto);
            proto.end(token2);
        }
    }

    public String toString() {
        String out = "";
        for (ContextHubClientBroker broker : this.mHostEndPointIdToClientMap.values()) {
            out = out + broker + "\n";
        }
        String out2 = out + "\nRegistration history:\n";
        Iterator<RegistrationRecord> it = this.mRegistrationRecordDeque.descendingIterator();
        while (it.hasNext()) {
            out2 = out2 + it.next() + "\n";
        }
        return out2;
    }
}
