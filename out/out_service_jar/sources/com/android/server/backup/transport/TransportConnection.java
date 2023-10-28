package com.android.server.backup.transport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.EventLog;
import com.android.internal.backup.IBackupTransport;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import dalvik.system.CloseGuard;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
/* loaded from: classes.dex */
public class TransportConnection {
    private static final int LOG_BUFFER_SIZE = 5;
    static final String TAG = "TransportConnection";
    private final Intent mBindIntent;
    private final CloseGuard mCloseGuard;
    private final ServiceConnection mConnection;
    private final Context mContext;
    private final String mCreatorLogString;
    private final String mIdentifier;
    private final Handler mListenerHandler;
    private final Map<TransportConnectionListener, String> mListeners;
    private final List<String> mLogBuffer;
    private final Object mLogBufferLock;
    private final String mPrefixForLog;
    private int mState;
    private final Object mStateLock;
    private volatile BackupTransportClient mTransport;
    private final ComponentName mTransportComponent;
    private final TransportStats mTransportStats;
    private final int mUserId;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface State {
        public static final int BOUND_AND_CONNECTING = 2;
        public static final int CONNECTED = 3;
        public static final int IDLE = 1;
        public static final int UNUSABLE = 0;
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface Transition {
        public static final int DOWN = -1;
        public static final int NO_TRANSITION = 0;
        public static final int UP = 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public TransportConnection(int userId, Context context, TransportStats transportStats, Intent bindIntent, ComponentName transportComponent, String identifier, String caller) {
        this(userId, context, transportStats, bindIntent, transportComponent, identifier, caller, new Handler(Looper.getMainLooper()));
    }

    TransportConnection(int userId, Context context, TransportStats transportStats, Intent bindIntent, ComponentName transportComponent, String identifier, String caller, Handler listenerHandler) {
        this.mStateLock = new Object();
        this.mLogBufferLock = new Object();
        CloseGuard closeGuard = CloseGuard.get();
        this.mCloseGuard = closeGuard;
        this.mLogBuffer = new LinkedList();
        this.mListeners = new ArrayMap();
        this.mState = 1;
        this.mUserId = userId;
        this.mContext = context;
        this.mTransportStats = transportStats;
        this.mTransportComponent = transportComponent;
        this.mBindIntent = bindIntent;
        this.mIdentifier = identifier;
        this.mCreatorLogString = caller;
        this.mListenerHandler = listenerHandler;
        this.mConnection = new TransportConnectionMonitor(context, this);
        String classNameForLog = transportComponent.getShortClassName().replaceFirst(".*\\.", "");
        this.mPrefixForLog = classNameForLog + "#" + identifier + ":";
        closeGuard.open("markAsDisposed");
    }

    public ComponentName getTransportComponent() {
        return this.mTransportComponent;
    }

    public void connectAsync(TransportConnectionListener listener, String caller) {
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            switch (this.mState) {
                case 0:
                    log(5, caller, "Async connect: UNUSABLE client");
                    notifyListener(listener, null, caller);
                    break;
                case 1:
                    boolean hasBound = this.mContext.bindServiceAsUser(this.mBindIntent, this.mConnection, 1, UserHandle.of(this.mUserId));
                    if (hasBound) {
                        log(3, caller, "Async connect: service bound, connecting");
                        setStateLocked(2, null);
                        this.mListeners.put(listener, caller);
                        break;
                    } else {
                        log(6, "Async connect: bindService returned false");
                        this.mContext.unbindService(this.mConnection);
                        notifyListener(listener, null, caller);
                        break;
                    }
                case 2:
                    log(3, caller, "Async connect: already connecting, adding listener");
                    this.mListeners.put(listener, caller);
                    break;
                case 3:
                    log(3, caller, "Async connect: reusing transport");
                    notifyListener(listener, this.mTransport, caller);
                    break;
            }
        }
    }

    public void unbind(String caller) {
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            log(3, caller, "Unbind requested (was " + stateToString(this.mState) + ")");
            switch (this.mState) {
                case 2:
                    setStateLocked(1, null);
                    this.mContext.unbindService(this.mConnection);
                    notifyListenersAndClearLocked(null);
                    break;
                case 3:
                    setStateLocked(1, null);
                    this.mContext.unbindService(this.mConnection);
                    break;
            }
        }
    }

    public void markAsDisposed() {
        synchronized (this.mStateLock) {
            Preconditions.checkState(this.mState < 2, "Can't mark as disposed if still bound");
            this.mCloseGuard.close();
        }
    }

    public BackupTransportClient connect(String caller) {
        Preconditions.checkState(!Looper.getMainLooper().isCurrentThread(), "Can't call connect() on main thread");
        BackupTransportClient transport = this.mTransport;
        if (transport != null) {
            log(3, caller, "Sync connect: reusing transport");
            return transport;
        }
        synchronized (this.mStateLock) {
            if (this.mState == 0) {
                log(5, caller, "Sync connect: UNUSABLE client");
                return null;
            }
            final CompletableFuture<BackupTransportClient> transportFuture = new CompletableFuture<>();
            TransportConnectionListener requestListener = new TransportConnectionListener() { // from class: com.android.server.backup.transport.TransportConnection$$ExternalSyntheticLambda1
                @Override // com.android.server.backup.transport.TransportConnectionListener
                public final void onTransportConnectionResult(BackupTransportClient backupTransportClient, TransportConnection transportConnection) {
                    transportFuture.complete(backupTransportClient);
                }
            };
            long requestTime = SystemClock.elapsedRealtime();
            log(3, caller, "Sync connect: calling async");
            connectAsync(requestListener, caller);
            try {
                BackupTransportClient transport2 = transportFuture.get();
                long time = SystemClock.elapsedRealtime() - requestTime;
                this.mTransportStats.registerConnectionTime(this.mTransportComponent, time);
                log(3, caller, String.format(Locale.US, "Connect took %d ms", Long.valueOf(time)));
                return transport2;
            } catch (InterruptedException | ExecutionException e) {
                String error = e.getClass().getSimpleName();
                log(6, caller, error + " while waiting for transport: " + e.getMessage());
                return null;
            }
        }
    }

    public BackupTransportClient connectOrThrow(String caller) throws TransportNotAvailableException {
        BackupTransportClient transport = connect(caller);
        if (transport == null) {
            log(6, caller, "Transport connection failed");
            throw new TransportNotAvailableException();
        }
        return transport;
    }

    public BackupTransportClient getConnectedTransport(String caller) throws TransportNotAvailableException {
        BackupTransportClient transport = this.mTransport;
        if (transport == null) {
            log(6, caller, "Transport not connected");
            throw new TransportNotAvailableException();
        }
        return transport;
    }

    public String toString() {
        return "TransportClient{" + this.mTransportComponent.flattenToShortString() + "#" + this.mIdentifier + "}";
    }

    protected void finalize() throws Throwable {
        synchronized (this.mStateLock) {
            this.mCloseGuard.warnIfOpen();
            if (this.mState >= 2) {
                log(6, "TransportClient.finalize()", "Dangling TransportClient created in [" + this.mCreatorLogString + "] being GC'ed. Left bound, unbinding...");
                try {
                    unbind("TransportClient.finalize()");
                } catch (IllegalStateException e) {
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onServiceConnected(IBinder binder) {
        IBackupTransport transportBinder = IBackupTransport.Stub.asInterface(binder);
        BackupTransportClient transport = new BackupTransportClient(transportBinder);
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            if (this.mState != 0) {
                log(3, "Transport connected");
                setStateLocked(3, transport);
                notifyListenersAndClearLocked(transport);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onServiceDisconnected() {
        synchronized (this.mStateLock) {
            log(6, "Service disconnected: client UNUSABLE");
            if (this.mTransport != null) {
                this.mTransport.onBecomingUnusable();
            }
            setStateLocked(0, null);
            try {
                this.mContext.unbindService(this.mConnection);
            } catch (IllegalArgumentException e) {
                log(5, "Exception trying to unbind onServiceDisconnected(): " + e.getMessage());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBindingDied() {
        synchronized (this.mStateLock) {
            checkStateIntegrityLocked();
            log(6, "Binding died: client UNUSABLE");
            if (this.mTransport != null) {
                this.mTransport.onBecomingUnusable();
            }
            switch (this.mState) {
                case 1:
                    log(6, "Unexpected state transition IDLE => UNUSABLE");
                    setStateLocked(0, null);
                    break;
                case 2:
                    setStateLocked(0, null);
                    this.mContext.unbindService(this.mConnection);
                    notifyListenersAndClearLocked(null);
                    break;
                case 3:
                    setStateLocked(0, null);
                    this.mContext.unbindService(this.mConnection);
                    break;
            }
        }
    }

    private void notifyListener(final TransportConnectionListener listener, final BackupTransportClient transport, String caller) {
        String transportString = transport != null ? "BackupTransportClient" : "null";
        log(4, "Notifying [" + caller + "] transport = " + transportString);
        this.mListenerHandler.post(new Runnable() { // from class: com.android.server.backup.transport.TransportConnection$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TransportConnection.this.m2241xf39dc0fb(listener, transport);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$notifyListener$1$com-android-server-backup-transport-TransportConnection  reason: not valid java name */
    public /* synthetic */ void m2241xf39dc0fb(TransportConnectionListener listener, BackupTransportClient transport) {
        listener.onTransportConnectionResult(transport, this);
    }

    private void notifyListenersAndClearLocked(BackupTransportClient transport) {
        for (Map.Entry<TransportConnectionListener, String> entry : this.mListeners.entrySet()) {
            TransportConnectionListener listener = entry.getKey();
            String caller = entry.getValue();
            notifyListener(listener, transport, caller);
        }
        this.mListeners.clear();
    }

    private void setStateLocked(int state, BackupTransportClient transport) {
        log(2, "State: " + stateToString(this.mState) + " => " + stateToString(state));
        onStateTransition(this.mState, state);
        this.mState = state;
        this.mTransport = transport;
    }

    private void onStateTransition(int oldState, int newState) {
        String transport = this.mTransportComponent.flattenToShortString();
        int bound = transitionThroughState(oldState, newState, 2);
        int connected = transitionThroughState(oldState, newState, 3);
        if (bound != 0) {
            int value = bound == 1 ? 1 : 0;
            EventLog.writeEvent((int) EventLogTags.BACKUP_TRANSPORT_LIFECYCLE, transport, Integer.valueOf(value));
        }
        if (connected != 0) {
            int value2 = connected == 1 ? 1 : 0;
            EventLog.writeEvent((int) EventLogTags.BACKUP_TRANSPORT_CONNECTION, transport, Integer.valueOf(value2));
        }
    }

    private int transitionThroughState(int oldState, int newState, int stateReference) {
        if (oldState < stateReference && stateReference <= newState) {
            return 1;
        }
        if (oldState >= stateReference && stateReference > newState) {
            return -1;
        }
        return 0;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void checkStateIntegrityLocked() {
        switch (this.mState) {
            case 0:
                checkState(this.mListeners.isEmpty(), "Unexpected listeners when state = UNUSABLE");
                checkState(this.mTransport == null, "Transport expected to be null when state = UNUSABLE");
                break;
            case 1:
                break;
            case 2:
                checkState(this.mTransport == null, "Transport expected to be null when state = BOUND_AND_CONNECTING");
                return;
            case 3:
                checkState(this.mListeners.isEmpty(), "Unexpected listeners when state = CONNECTED");
                checkState(this.mTransport != null, "Transport expected to be non-null when state = CONNECTED");
                return;
            default:
                checkState(false, "Unexpected state = " + stateToString(this.mState));
                return;
        }
        checkState(this.mListeners.isEmpty(), "Unexpected listeners when state = IDLE");
        checkState(this.mTransport == null, "Transport expected to be null when state = IDLE");
    }

    private void checkState(boolean assertion, String message) {
        if (!assertion) {
            log(6, message);
        }
    }

    private String stateToString(int state) {
        switch (state) {
            case 0:
                return "UNUSABLE";
            case 1:
                return "IDLE";
            case 2:
                return "BOUND_AND_CONNECTING";
            case 3:
                return "CONNECTED";
            default:
                return "<UNKNOWN = " + state + ">";
        }
    }

    private void log(int priority, String message) {
        TransportUtils.log(priority, TAG, TransportUtils.formatMessage(this.mPrefixForLog, null, message));
        saveLogEntry(TransportUtils.formatMessage(null, null, message));
    }

    private void log(int priority, String caller, String message) {
        TransportUtils.log(priority, TAG, TransportUtils.formatMessage(this.mPrefixForLog, caller, message));
        saveLogEntry(TransportUtils.formatMessage(null, caller, message));
    }

    private void saveLogEntry(String message) {
        List<String> list;
        CharSequence time = DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis());
        String message2 = ((Object) time) + " " + message;
        synchronized (this.mLogBufferLock) {
            if (this.mLogBuffer.size() == 5) {
                this.mLogBuffer.remove(list.size() - 1);
            }
            this.mLogBuffer.add(0, message2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<String> getLogBuffer() {
        List<String> unmodifiableList;
        synchronized (this.mLogBufferLock) {
            unmodifiableList = Collections.unmodifiableList(this.mLogBuffer);
        }
        return unmodifiableList;
    }

    /* loaded from: classes.dex */
    private static class TransportConnectionMonitor implements ServiceConnection {
        private final Context mContext;
        private final WeakReference<TransportConnection> mTransportClientRef;

        private TransportConnectionMonitor(Context context, TransportConnection transportConnection) {
            this.mContext = context;
            this.mTransportClientRef = new WeakReference<>(transportConnection);
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName transportComponent, IBinder binder) {
            TransportConnection transportConnection = this.mTransportClientRef.get();
            if (transportConnection == null) {
                referenceLost("TransportConnection.onServiceConnected()");
                return;
            }
            Binder.allowBlocking(binder);
            transportConnection.onServiceConnected(binder);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName transportComponent) {
            TransportConnection transportConnection = this.mTransportClientRef.get();
            if (transportConnection == null) {
                referenceLost("TransportConnection.onServiceDisconnected()");
            } else {
                transportConnection.onServiceDisconnected();
            }
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName transportComponent) {
            TransportConnection transportConnection = this.mTransportClientRef.get();
            if (transportConnection == null) {
                referenceLost("TransportConnection.onBindingDied()");
            } else {
                transportConnection.onBindingDied();
            }
        }

        private void referenceLost(String caller) {
            this.mContext.unbindService(this);
            TransportUtils.log(4, TransportConnection.TAG, caller + " called but TransportClient reference has been GC'ed");
        }
    }
}
