package com.android.server.backup.transport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.android.server.backup.TransportManager;
import java.io.PrintWriter;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
/* loaded from: classes.dex */
public class TransportConnectionManager {
    private static final String TAG = "TransportConnectionManager";
    private final Context mContext;
    private final Function<ComponentName, Intent> mIntentFunction;
    private Map<TransportConnection, String> mTransportClientsCallerMap;
    private int mTransportClientsCreated;
    private final Object mTransportClientsLock;
    private final TransportStats mTransportStats;
    private final int mUserId;

    /* JADX INFO: Access modifiers changed from: private */
    public static Intent getRealTransportIntent(ComponentName transportComponent) {
        return new Intent(TransportManager.SERVICE_ACTION_TRANSPORT_HOST).setComponent(transportComponent);
    }

    public TransportConnectionManager(int userId, Context context, TransportStats transportStats) {
        this(userId, context, transportStats, new Function() { // from class: com.android.server.backup.transport.TransportConnectionManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Intent realTransportIntent;
                realTransportIntent = TransportConnectionManager.getRealTransportIntent((ComponentName) obj);
                return realTransportIntent;
            }
        });
    }

    private TransportConnectionManager(int userId, Context context, TransportStats transportStats, Function<ComponentName, Intent> intentFunction) {
        this.mTransportClientsLock = new Object();
        this.mTransportClientsCreated = 0;
        this.mTransportClientsCallerMap = new WeakHashMap();
        this.mUserId = userId;
        this.mContext = context;
        this.mTransportStats = transportStats;
        this.mIntentFunction = intentFunction;
    }

    public TransportConnection getTransportClient(ComponentName transportComponent, String caller) {
        return getTransportClient(transportComponent, (Bundle) null, caller);
    }

    public TransportConnection getTransportClient(ComponentName transportComponent, Bundle extras, String caller) {
        Intent bindIntent = this.mIntentFunction.apply(transportComponent);
        if (extras != null) {
            bindIntent.putExtras(extras);
        }
        return getTransportClient(transportComponent, caller, bindIntent);
    }

    private TransportConnection getTransportClient(ComponentName transportComponent, String caller, Intent bindIntent) {
        TransportConnection transportConnection;
        synchronized (this.mTransportClientsLock) {
            transportConnection = new TransportConnection(this.mUserId, this.mContext, this.mTransportStats, bindIntent, transportComponent, Integer.toString(this.mTransportClientsCreated), caller);
            this.mTransportClientsCallerMap.put(transportConnection, caller);
            this.mTransportClientsCreated++;
            TransportUtils.log(3, TAG, TransportUtils.formatMessage(null, caller, "Retrieving " + transportConnection));
        }
        return transportConnection;
    }

    public void disposeOfTransportClient(TransportConnection transportConnection, String caller) {
        if (transportConnection != null) {
            transportConnection.unbind(caller);
            transportConnection.markAsDisposed();
            synchronized (this.mTransportClientsLock) {
                TransportUtils.log(3, TAG, TransportUtils.formatMessage(null, caller, "Disposing of " + transportConnection));
                this.mTransportClientsCallerMap.remove(transportConnection);
            }
        }
    }

    public void dump(PrintWriter pw) {
        pw.println("Transport clients created: " + this.mTransportClientsCreated);
        synchronized (this.mTransportClientsLock) {
            pw.println("Current transport clients: " + this.mTransportClientsCallerMap.size());
            for (TransportConnection transportConnection : this.mTransportClientsCallerMap.keySet()) {
                String caller = this.mTransportClientsCallerMap.get(transportConnection);
                pw.println("    " + transportConnection + " [" + caller + "]");
                for (String logEntry : transportConnection.getLogBuffer()) {
                    pw.println("        " + logEntry);
                }
            }
        }
    }
}
