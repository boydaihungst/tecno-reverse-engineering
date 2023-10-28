package com.android.server.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.media.IMediaRoute2ProviderService;
import android.media.IMediaRoute2ProviderServiceCallback;
import android.media.MediaRoute2ProviderInfo;
import android.media.RouteDiscoveryPreference;
import android.media.RoutingSessionInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.media.MediaRoute2Provider;
import com.android.server.media.MediaRoute2ProviderServiceProxy;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class MediaRoute2ProviderServiceProxy extends MediaRoute2Provider implements ServiceConnection {
    private Connection mActiveConnection;
    private boolean mBound;
    private boolean mConnectionReady;
    private final Context mContext;
    private final Handler mHandler;
    private boolean mIsManagerScanning;
    private RouteDiscoveryPreference mLastDiscoveryPreference;
    final List<RoutingSessionInfo> mReleasingSessions;
    private boolean mRunning;
    private final int mUserId;
    private static final String TAG = "MR2ProviderSvcProxy";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    /* JADX INFO: Access modifiers changed from: package-private */
    public MediaRoute2ProviderServiceProxy(Context context, ComponentName componentName, int userId) {
        super(componentName);
        this.mLastDiscoveryPreference = null;
        this.mReleasingSessions = new ArrayList();
        this.mContext = (Context) Objects.requireNonNull(context, "Context must not be null.");
        this.mUserId = userId;
        this.mHandler = new Handler(Looper.myLooper());
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "Proxy");
        pw.println(prefix + "  mUserId=" + this.mUserId);
        pw.println(prefix + "  mRunning=" + this.mRunning);
        pw.println(prefix + "  mBound=" + this.mBound);
        pw.println(prefix + "  mActiveConnection=" + this.mActiveConnection);
        pw.println(prefix + "  mConnectionReady=" + this.mConnectionReady);
    }

    public void setManagerScanning(boolean managerScanning) {
        if (this.mIsManagerScanning != managerScanning) {
            this.mIsManagerScanning = managerScanning;
            updateBinding();
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void requestCreateSession(long requestId, String packageName, String routeId, Bundle sessionHints) {
        if (this.mConnectionReady) {
            this.mActiveConnection.requestCreateSession(requestId, packageName, routeId, sessionHints);
            updateBinding();
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void releaseSession(long requestId, String sessionId) {
        if (this.mConnectionReady) {
            this.mActiveConnection.releaseSession(requestId, sessionId);
            updateBinding();
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void updateDiscoveryPreference(RouteDiscoveryPreference discoveryPreference) {
        this.mLastDiscoveryPreference = discoveryPreference;
        if (this.mConnectionReady) {
            this.mActiveConnection.updateDiscoveryPreference(discoveryPreference);
        }
        updateBinding();
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void selectRoute(long requestId, String sessionId, String routeId) {
        if (this.mConnectionReady) {
            this.mActiveConnection.selectRoute(requestId, sessionId, routeId);
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void deselectRoute(long requestId, String sessionId, String routeId) {
        if (this.mConnectionReady) {
            this.mActiveConnection.deselectRoute(requestId, sessionId, routeId);
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void transferToRoute(long requestId, String sessionId, String routeId) {
        if (this.mConnectionReady) {
            this.mActiveConnection.transferToRoute(requestId, sessionId, routeId);
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void setRouteVolume(long requestId, String routeId, int volume) {
        if (this.mConnectionReady) {
            this.mActiveConnection.setRouteVolume(requestId, routeId, volume);
            updateBinding();
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void setSessionVolume(long requestId, String sessionId, int volume) {
        if (this.mConnectionReady) {
            this.mActiveConnection.setSessionVolume(requestId, sessionId, volume);
            updateBinding();
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public void prepareReleaseSession(String sessionId) {
        synchronized (this.mLock) {
            Iterator<RoutingSessionInfo> it = this.mSessionInfos.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                RoutingSessionInfo session = it.next();
                if (TextUtils.equals(session.getId(), sessionId)) {
                    this.mSessionInfos.remove(session);
                    this.mReleasingSessions.add(session);
                    break;
                }
            }
        }
    }

    @Override // com.android.server.media.MediaRoute2Provider
    public boolean hasComponentName(String packageName, String className) {
        return this.mComponentName.getPackageName().equals(packageName) && this.mComponentName.getClassName().equals(className);
    }

    public void start() {
        if (!this.mRunning) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Starting");
            }
            this.mRunning = true;
            updateBinding();
        }
    }

    public void stop() {
        if (this.mRunning) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Stopping");
            }
            this.mRunning = false;
            updateBinding();
        }
    }

    public void rebindIfDisconnected() {
        if (this.mActiveConnection == null && shouldBind()) {
            unbind();
            bind();
        }
    }

    private void updateBinding() {
        if (shouldBind()) {
            bind();
        } else {
            unbind();
        }
    }

    private boolean shouldBind() {
        if (this.mRunning) {
            RouteDiscoveryPreference routeDiscoveryPreference = this.mLastDiscoveryPreference;
            return ((routeDiscoveryPreference == null || routeDiscoveryPreference.getPreferredFeatures().isEmpty()) && getSessionInfos().isEmpty() && !this.mIsManagerScanning) ? false : true;
        }
        return false;
    }

    private void bind() {
        if (!this.mBound) {
            boolean z = DEBUG;
            if (z) {
                Slog.d(TAG, this + ": Binding");
            }
            Intent service = new Intent("android.media.MediaRoute2ProviderService");
            service.setComponent(this.mComponentName);
            try {
                boolean bindServiceAsUser = this.mContext.bindServiceAsUser(service, this, AudioFormat.AAC_MAIN, new UserHandle(this.mUserId));
                this.mBound = bindServiceAsUser;
                if (!bindServiceAsUser && z) {
                    Slog.d(TAG, this + ": Bind failed");
                }
            } catch (SecurityException ex) {
                if (DEBUG) {
                    Slog.d(TAG, this + ": Bind failed", ex);
                }
            }
        }
    }

    private void unbind() {
        if (this.mBound) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Unbinding");
            }
            this.mBound = false;
            disconnect();
            this.mContext.unbindService(this);
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceConnected(ComponentName name, IBinder service) {
        boolean z = DEBUG;
        if (z) {
            Slog.d(TAG, this + ": Connected");
        }
        if (this.mBound) {
            disconnect();
            IMediaRoute2ProviderService serviceBinder = IMediaRoute2ProviderService.Stub.asInterface(service);
            if (serviceBinder != null) {
                Connection connection = new Connection(serviceBinder);
                if (connection.register()) {
                    this.mActiveConnection = connection;
                    return;
                } else if (z) {
                    Slog.d(TAG, this + ": Registration failed");
                    return;
                } else {
                    return;
                }
            }
            Slog.e(TAG, this + ": Service returned invalid binder");
        }
    }

    @Override // android.content.ServiceConnection
    public void onServiceDisconnected(ComponentName name) {
        if (DEBUG) {
            Slog.d(TAG, this + ": Service disconnected");
        }
        disconnect();
    }

    @Override // android.content.ServiceConnection
    public void onBindingDied(ComponentName name) {
        if (DEBUG) {
            Slog.d(TAG, this + ": Service binding died");
        }
        unbind();
        if (shouldBind()) {
            bind();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onConnectionReady(Connection connection) {
        if (this.mActiveConnection == connection) {
            this.mConnectionReady = true;
            RouteDiscoveryPreference routeDiscoveryPreference = this.mLastDiscoveryPreference;
            if (routeDiscoveryPreference != null) {
                updateDiscoveryPreference(routeDiscoveryPreference);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onConnectionDied(Connection connection) {
        if (this.mActiveConnection == connection) {
            if (DEBUG) {
                Slog.d(TAG, this + ": Service connection died");
            }
            disconnect();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onProviderUpdated(Connection connection, MediaRoute2ProviderInfo providerInfo) {
        if (this.mActiveConnection != connection) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, this + ": updated");
        }
        setAndNotifyProviderState(providerInfo);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSessionCreated(Connection connection, long requestId, RoutingSessionInfo newSession) {
        if (this.mActiveConnection != connection) {
            return;
        }
        if (newSession == null) {
            Slog.w(TAG, "onSessionCreated: Ignoring null session sent from " + this.mComponentName);
            return;
        }
        RoutingSessionInfo newSession2 = assignProviderIdForSession(newSession);
        final String newSessionId = newSession2.getId();
        synchronized (this.mLock) {
            if (!this.mSessionInfos.stream().anyMatch(new Predicate() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = TextUtils.equals(((RoutingSessionInfo) obj).getId(), newSessionId);
                    return equals;
                }
            }) && !this.mReleasingSessions.stream().anyMatch(new Predicate() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$$ExternalSyntheticLambda2
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean equals;
                    equals = TextUtils.equals(((RoutingSessionInfo) obj).getId(), newSessionId);
                    return equals;
                }
            })) {
                this.mSessionInfos.add(newSession2);
                this.mCallback.onSessionCreated(this, requestId, newSession2);
                return;
            }
            Slog.w(TAG, "onSessionCreated: Duplicate session already exists. Ignoring.");
        }
    }

    private int findSessionByIdLocked(RoutingSessionInfo session) {
        for (int i = 0; i < this.mSessionInfos.size(); i++) {
            if (TextUtils.equals(this.mSessionInfos.get(i).getId(), session.getId())) {
                return i;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSessionsUpdated(Connection connection, List<RoutingSessionInfo> sessions) {
        Throwable th;
        if (this.mActiveConnection != connection) {
            return;
        }
        int targetIndex = 0;
        synchronized (this.mLock) {
            try {
                try {
                    for (RoutingSessionInfo session : sessions) {
                        if (session != null) {
                            RoutingSessionInfo session2 = assignProviderIdForSession(session);
                            int sourceIndex = findSessionByIdLocked(session2);
                            if (sourceIndex < 0) {
                                int targetIndex2 = targetIndex + 1;
                                this.mSessionInfos.add(targetIndex, session2);
                                dispatchSessionCreated(0L, session2);
                                targetIndex = targetIndex2;
                            } else if (sourceIndex < targetIndex) {
                                Slog.w(TAG, "Ignoring duplicate session ID: " + session2.getId());
                            } else {
                                this.mSessionInfos.set(sourceIndex, session2);
                                int targetIndex3 = targetIndex + 1;
                                Collections.swap(this.mSessionInfos, sourceIndex, targetIndex);
                                dispatchSessionUpdated(session2);
                                targetIndex = targetIndex3;
                            }
                        }
                    }
                    for (int i = this.mSessionInfos.size() - 1; i >= targetIndex; i--) {
                        RoutingSessionInfo releasedSession = this.mSessionInfos.remove(i);
                        dispatchSessionReleased(releasedSession);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSessionReleased(Connection connection, RoutingSessionInfo releaedSession) {
        if (this.mActiveConnection != connection) {
            return;
        }
        if (releaedSession == null) {
            Slog.w(TAG, "onSessionReleased: Ignoring null session sent from " + this.mComponentName);
            return;
        }
        RoutingSessionInfo releaedSession2 = assignProviderIdForSession(releaedSession);
        boolean found = false;
        synchronized (this.mLock) {
            Iterator<RoutingSessionInfo> it = this.mSessionInfos.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                RoutingSessionInfo session = it.next();
                if (TextUtils.equals(session.getId(), releaedSession2.getId())) {
                    this.mSessionInfos.remove(session);
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (RoutingSessionInfo session2 : this.mReleasingSessions) {
                    if (TextUtils.equals(session2.getId(), releaedSession2.getId())) {
                        this.mReleasingSessions.remove(session2);
                        return;
                    }
                }
            }
            if (!found) {
                Slog.w(TAG, "onSessionReleased: Matching session info not found");
            } else {
                this.mCallback.onSessionReleased(this, releaedSession2);
            }
        }
    }

    private void dispatchSessionCreated(long requestId, RoutingSessionInfo session) {
        Handler handler = this.mHandler;
        final MediaRoute2Provider.Callback callback = this.mCallback;
        Objects.requireNonNull(callback);
        handler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$$ExternalSyntheticLambda4
            public final void accept(Object obj, Object obj2, Object obj3) {
                MediaRoute2Provider.Callback.this.onSessionCreated((MediaRoute2ProviderServiceProxy) obj, ((Long) obj2).longValue(), (RoutingSessionInfo) obj3);
            }
        }, this, Long.valueOf(requestId), session));
    }

    private void dispatchSessionUpdated(RoutingSessionInfo session) {
        Handler handler = this.mHandler;
        final MediaRoute2Provider.Callback callback = this.mCallback;
        Objects.requireNonNull(callback);
        handler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$$ExternalSyntheticLambda0
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                MediaRoute2Provider.Callback.this.onSessionUpdated((MediaRoute2ProviderServiceProxy) obj, (RoutingSessionInfo) obj2);
            }
        }, this, session));
    }

    private void dispatchSessionReleased(RoutingSessionInfo session) {
        Handler handler = this.mHandler;
        final MediaRoute2Provider.Callback callback = this.mCallback;
        Objects.requireNonNull(callback);
        handler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$$ExternalSyntheticLambda3
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                MediaRoute2Provider.Callback.this.onSessionReleased((MediaRoute2ProviderServiceProxy) obj, (RoutingSessionInfo) obj2);
            }
        }, this, session));
    }

    private RoutingSessionInfo assignProviderIdForSession(RoutingSessionInfo sessionInfo) {
        return new RoutingSessionInfo.Builder(sessionInfo).setOwnerPackageName(this.mComponentName.getPackageName()).setProviderId(getUniqueId()).build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRequestFailed(Connection connection, long requestId, int reason) {
        if (this.mActiveConnection != connection) {
            return;
        }
        if (requestId == 0) {
            Slog.w(TAG, "onRequestFailed: Ignoring requestId REQUEST_ID_NONE");
        } else {
            this.mCallback.onRequestFailed(this, requestId, reason);
        }
    }

    private void disconnect() {
        Connection connection = this.mActiveConnection;
        if (connection != null) {
            this.mConnectionReady = false;
            connection.dispose();
            this.mActiveConnection = null;
            setAndNotifyProviderState(null);
            synchronized (this.mLock) {
                for (RoutingSessionInfo sessionInfo : this.mSessionInfos) {
                    this.mCallback.onSessionReleased(this, sessionInfo);
                }
                this.mSessionInfos.clear();
                this.mReleasingSessions.clear();
            }
        }
    }

    public String toString() {
        return "Service connection " + this.mComponentName.flattenToShortString();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public final class Connection implements IBinder.DeathRecipient {
        private final ServiceCallbackStub mCallbackStub = new ServiceCallbackStub(this);
        private final IMediaRoute2ProviderService mService;

        Connection(IMediaRoute2ProviderService serviceBinder) {
            this.mService = serviceBinder;
        }

        public boolean register() {
            try {
                this.mService.asBinder().linkToDeath(this, 0);
                this.mService.setCallback(this.mCallbackStub);
                MediaRoute2ProviderServiceProxy.this.mHandler.post(new Runnable() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$Connection$$ExternalSyntheticLambda6
                    @Override // java.lang.Runnable
                    public final void run() {
                        MediaRoute2ProviderServiceProxy.Connection.this.m4642xaab680dc();
                    }
                });
                return true;
            } catch (RemoteException e) {
                binderDied();
                return false;
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$register$0$com-android-server-media-MediaRoute2ProviderServiceProxy$Connection  reason: not valid java name */
        public /* synthetic */ void m4642xaab680dc() {
            MediaRoute2ProviderServiceProxy.this.onConnectionReady(this);
        }

        public void dispose() {
            this.mService.asBinder().unlinkToDeath(this, 0);
            this.mCallbackStub.dispose();
        }

        public void requestCreateSession(long requestId, String packageName, String routeId, Bundle sessionHints) {
            try {
                this.mService.requestCreateSession(requestId, packageName, routeId, sessionHints);
            } catch (RemoteException e) {
                Slog.e(MediaRoute2ProviderServiceProxy.TAG, "requestCreateSession: Failed to deliver request.");
            }
        }

        public void releaseSession(long requestId, String sessionId) {
            try {
                this.mService.releaseSession(requestId, sessionId);
            } catch (RemoteException e) {
                Slog.e(MediaRoute2ProviderServiceProxy.TAG, "releaseSession: Failed to deliver request.");
            }
        }

        public void updateDiscoveryPreference(RouteDiscoveryPreference discoveryPreference) {
            try {
                this.mService.updateDiscoveryPreference(discoveryPreference);
            } catch (RemoteException e) {
                Slog.e(MediaRoute2ProviderServiceProxy.TAG, "updateDiscoveryPreference: Failed to deliver request.");
            }
        }

        public void selectRoute(long requestId, String sessionId, String routeId) {
            try {
                this.mService.selectRoute(requestId, sessionId, routeId);
            } catch (RemoteException ex) {
                Slog.e(MediaRoute2ProviderServiceProxy.TAG, "selectRoute: Failed to deliver request.", ex);
            }
        }

        public void deselectRoute(long requestId, String sessionId, String routeId) {
            try {
                this.mService.deselectRoute(requestId, sessionId, routeId);
            } catch (RemoteException ex) {
                Slog.e(MediaRoute2ProviderServiceProxy.TAG, "deselectRoute: Failed to deliver request.", ex);
            }
        }

        public void transferToRoute(long requestId, String sessionId, String routeId) {
            try {
                this.mService.transferToRoute(requestId, sessionId, routeId);
            } catch (RemoteException ex) {
                Slog.e(MediaRoute2ProviderServiceProxy.TAG, "transferToRoute: Failed to deliver request.", ex);
            }
        }

        public void setRouteVolume(long requestId, String routeId, int volume) {
            try {
                this.mService.setRouteVolume(requestId, routeId, volume);
            } catch (RemoteException ex) {
                Slog.e(MediaRoute2ProviderServiceProxy.TAG, "setRouteVolume: Failed to deliver request.", ex);
            }
        }

        public void setSessionVolume(long requestId, String sessionId, int volume) {
            try {
                this.mService.setSessionVolume(requestId, sessionId, volume);
            } catch (RemoteException ex) {
                Slog.e(MediaRoute2ProviderServiceProxy.TAG, "setSessionVolume: Failed to deliver request.", ex);
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            MediaRoute2ProviderServiceProxy.this.mHandler.post(new Runnable() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$Connection$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MediaRoute2ProviderServiceProxy.Connection.this.m4636x4e191ae8();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$binderDied$1$com-android-server-media-MediaRoute2ProviderServiceProxy$Connection  reason: not valid java name */
        public /* synthetic */ void m4636x4e191ae8() {
            MediaRoute2ProviderServiceProxy.this.onConnectionDied(this);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$postProviderUpdated$2$com-android-server-media-MediaRoute2ProviderServiceProxy$Connection  reason: not valid java name */
        public /* synthetic */ void m4637xba8c7fab(MediaRoute2ProviderInfo providerInfo) {
            MediaRoute2ProviderServiceProxy.this.onProviderUpdated(this, providerInfo);
        }

        void postProviderUpdated(final MediaRoute2ProviderInfo providerInfo) {
            MediaRoute2ProviderServiceProxy.this.mHandler.post(new Runnable() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$Connection$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    MediaRoute2ProviderServiceProxy.Connection.this.m4637xba8c7fab(providerInfo);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$postSessionCreated$3$com-android-server-media-MediaRoute2ProviderServiceProxy$Connection  reason: not valid java name */
        public /* synthetic */ void m4639x6434956e(long requestId, RoutingSessionInfo sessionInfo) {
            MediaRoute2ProviderServiceProxy.this.onSessionCreated(this, requestId, sessionInfo);
        }

        void postSessionCreated(final long requestId, final RoutingSessionInfo sessionInfo) {
            MediaRoute2ProviderServiceProxy.this.mHandler.post(new Runnable() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$Connection$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    MediaRoute2ProviderServiceProxy.Connection.this.m4639x6434956e(requestId, sessionInfo);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$postSessionsUpdated$4$com-android-server-media-MediaRoute2ProviderServiceProxy$Connection  reason: not valid java name */
        public /* synthetic */ void m4641x11beeb81(List sessionInfo) {
            MediaRoute2ProviderServiceProxy.this.onSessionsUpdated(this, sessionInfo);
        }

        void postSessionsUpdated(final List<RoutingSessionInfo> sessionInfo) {
            MediaRoute2ProviderServiceProxy.this.mHandler.post(new Runnable() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$Connection$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MediaRoute2ProviderServiceProxy.Connection.this.m4641x11beeb81(sessionInfo);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$postSessionReleased$5$com-android-server-media-MediaRoute2ProviderServiceProxy$Connection  reason: not valid java name */
        public /* synthetic */ void m4640x3b126bd7(RoutingSessionInfo sessionInfo) {
            MediaRoute2ProviderServiceProxy.this.onSessionReleased(this, sessionInfo);
        }

        void postSessionReleased(final RoutingSessionInfo sessionInfo) {
            MediaRoute2ProviderServiceProxy.this.mHandler.post(new Runnable() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$Connection$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    MediaRoute2ProviderServiceProxy.Connection.this.m4640x3b126bd7(sessionInfo);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$postRequestFailed$6$com-android-server-media-MediaRoute2ProviderServiceProxy$Connection  reason: not valid java name */
        public /* synthetic */ void m4638x29aed311(long requestId, int reason) {
            MediaRoute2ProviderServiceProxy.this.onRequestFailed(this, requestId, reason);
        }

        void postRequestFailed(final long requestId, final int reason) {
            MediaRoute2ProviderServiceProxy.this.mHandler.post(new Runnable() { // from class: com.android.server.media.MediaRoute2ProviderServiceProxy$Connection$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    MediaRoute2ProviderServiceProxy.Connection.this.m4638x29aed311(requestId, reason);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class ServiceCallbackStub extends IMediaRoute2ProviderServiceCallback.Stub {
        private final WeakReference<Connection> mConnectionRef;

        ServiceCallbackStub(Connection connection) {
            this.mConnectionRef = new WeakReference<>(connection);
        }

        public void dispose() {
            this.mConnectionRef.clear();
        }

        public void notifyProviderUpdated(MediaRoute2ProviderInfo providerInfo) {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.postProviderUpdated(providerInfo);
            }
        }

        public void notifySessionCreated(long requestId, RoutingSessionInfo sessionInfo) {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.postSessionCreated(requestId, sessionInfo);
            }
        }

        public void notifySessionsUpdated(List<RoutingSessionInfo> sessionInfo) {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.postSessionsUpdated(sessionInfo);
            }
        }

        public void notifySessionReleased(RoutingSessionInfo sessionInfo) {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.postSessionReleased(sessionInfo);
            }
        }

        public void notifyRequestFailed(long requestId, int reason) {
            Connection connection = this.mConnectionRef.get();
            if (connection != null) {
                connection.postRequestFailed(requestId, reason);
            }
        }
    }
}
