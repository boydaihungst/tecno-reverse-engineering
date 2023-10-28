package com.android.server.smartspace;

import android.app.AppGlobals;
import android.app.smartspace.ISmartspaceCallback;
import android.app.smartspace.SmartspaceConfig;
import android.app.smartspace.SmartspaceSessionId;
import android.app.smartspace.SmartspaceTargetEvent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.service.smartspace.ISmartspaceService;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.infra.AbstractRemoteService;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.smartspace.RemoteSmartspaceService;
import com.android.server.smartspace.SmartspacePerUserService;
import java.util.function.Consumer;
/* loaded from: classes2.dex */
public class SmartspacePerUserService extends AbstractPerUserSystemService<SmartspacePerUserService, SmartspaceManagerService> implements RemoteSmartspaceService.RemoteSmartspaceServiceCallbacks {
    private static final String TAG = SmartspacePerUserService.class.getSimpleName();
    private RemoteSmartspaceService mRemoteService;
    private final ArrayMap<SmartspaceSessionId, SmartspaceSessionInfo> mSessionInfos;
    private boolean mZombie;

    /* JADX INFO: Access modifiers changed from: protected */
    public SmartspacePerUserService(SmartspaceManagerService master, Object lock, int userId) {
        super(master, lock, userId);
        this.mSessionInfos = new ArrayMap<>();
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        try {
            ServiceInfo si = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 128L, this.mUserId);
            return si;
        } catch (RemoteException e) {
            throw new PackageManager.NameNotFoundException("Could not get service for " + serviceComponent);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractPerUserSystemService
    public boolean updateLocked(boolean disabled) {
        boolean enabledChanged = super.updateLocked(disabled);
        if (enabledChanged) {
            if (isEnabledLocked()) {
                resurrectSessionsLocked();
            } else {
                updateRemoteServiceLocked();
            }
        }
        return enabledChanged;
    }

    public void onCreateSmartspaceSessionLocked(final SmartspaceConfig smartspaceConfig, final SmartspaceSessionId sessionId, IBinder token) {
        boolean serviceExists = resolveService(sessionId, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.smartspace.SmartspacePerUserService$$ExternalSyntheticLambda1
            public final void run(IInterface iInterface) {
                ((ISmartspaceService) iInterface).onCreateSmartspaceSession(smartspaceConfig, sessionId);
            }
        });
        if (serviceExists && !this.mSessionInfos.containsKey(sessionId)) {
            SmartspaceSessionInfo sessionInfo = new SmartspaceSessionInfo(sessionId, smartspaceConfig, token, new IBinder.DeathRecipient() { // from class: com.android.server.smartspace.SmartspacePerUserService$$ExternalSyntheticLambda2
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    SmartspacePerUserService.this.m6505x6adc4c6(sessionId);
                }
            });
            if (sessionInfo.linkToDeath()) {
                this.mSessionInfos.put(sessionId, sessionInfo);
            } else {
                onDestroyLocked(sessionId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCreateSmartspaceSessionLocked$1$com-android-server-smartspace-SmartspacePerUserService  reason: not valid java name */
    public /* synthetic */ void m6505x6adc4c6(SmartspaceSessionId sessionId) {
        synchronized (this.mLock) {
            onDestroyLocked(sessionId);
        }
    }

    public void notifySmartspaceEventLocked(final SmartspaceSessionId sessionId, final SmartspaceTargetEvent event) {
        SmartspaceSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.smartspace.SmartspacePerUserService$$ExternalSyntheticLambda4
            public final void run(IInterface iInterface) {
                ((ISmartspaceService) iInterface).notifySmartspaceEvent(sessionId, event);
            }
        });
    }

    public void requestSmartspaceUpdateLocked(final SmartspaceSessionId sessionId) {
        SmartspaceSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.smartspace.SmartspacePerUserService$$ExternalSyntheticLambda0
            public final void run(IInterface iInterface) {
                ((ISmartspaceService) iInterface).requestSmartspaceUpdate(sessionId);
            }
        });
    }

    public void registerSmartspaceUpdatesLocked(final SmartspaceSessionId sessionId, final ISmartspaceCallback callback) {
        SmartspaceSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        boolean serviceExists = resolveService(sessionId, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.smartspace.SmartspacePerUserService$$ExternalSyntheticLambda6
            public final void run(IInterface iInterface) {
                ((ISmartspaceService) iInterface).registerSmartspaceUpdates(sessionId, callback);
            }
        });
        if (serviceExists) {
            sessionInfo.addCallbackLocked(callback);
        }
    }

    public void unregisterSmartspaceUpdatesLocked(final SmartspaceSessionId sessionId, final ISmartspaceCallback callback) {
        SmartspaceSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        boolean serviceExists = resolveService(sessionId, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.smartspace.SmartspacePerUserService$$ExternalSyntheticLambda5
            public final void run(IInterface iInterface) {
                ((ISmartspaceService) iInterface).unregisterSmartspaceUpdates(sessionId, callback);
            }
        });
        if (serviceExists) {
            sessionInfo.removeCallbackLocked(callback);
        }
    }

    public void onDestroyLocked(final SmartspaceSessionId sessionId) {
        if (isDebug()) {
            Slog.d(TAG, "onDestroyLocked(): sessionId=" + sessionId);
        }
        SmartspaceSessionInfo sessionInfo = this.mSessionInfos.remove(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.smartspace.SmartspacePerUserService$$ExternalSyntheticLambda3
            public final void run(IInterface iInterface) {
                ((ISmartspaceService) iInterface).onDestroySmartspaceSession(sessionId);
            }
        });
        sessionInfo.destroy();
    }

    @Override // com.android.server.smartspace.RemoteSmartspaceService.RemoteSmartspaceServiceCallbacks
    public void onFailureOrTimeout(boolean timedOut) {
        if (isDebug()) {
            Slog.d(TAG, "onFailureOrTimeout(): timed out=" + timedOut);
        }
    }

    @Override // com.android.server.smartspace.RemoteSmartspaceService.RemoteSmartspaceServiceCallbacks
    public void onConnectedStateChanged(boolean connected) {
        if (isDebug()) {
            Slog.d(TAG, "onConnectedStateChanged(): connected=" + connected);
        }
        if (connected) {
            synchronized (this.mLock) {
                if (this.mZombie) {
                    if (this.mRemoteService == null) {
                        Slog.w(TAG, "Cannot resurrect sessions because remote service is null");
                    } else {
                        this.mZombie = false;
                        resurrectSessionsLocked();
                    }
                }
            }
        }
    }

    /* JADX DEBUG: Method merged with bridge method */
    public void onServiceDied(RemoteSmartspaceService service) {
        if (isDebug()) {
            Slog.w(TAG, "onServiceDied(): service=" + service);
        }
        synchronized (this.mLock) {
            this.mZombie = true;
        }
        updateRemoteServiceLocked();
    }

    private void updateRemoteServiceLocked() {
        RemoteSmartspaceService remoteSmartspaceService = this.mRemoteService;
        if (remoteSmartspaceService != null) {
            remoteSmartspaceService.destroy();
            this.mRemoteService = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageUpdatedLocked() {
        if (isDebug()) {
            Slog.v(TAG, "onPackageUpdatedLocked()");
        }
        destroyAndRebindRemoteService();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onPackageRestartedLocked() {
        if (isDebug()) {
            Slog.v(TAG, "onPackageRestartedLocked()");
        }
        destroyAndRebindRemoteService();
    }

    private void destroyAndRebindRemoteService() {
        if (this.mRemoteService == null) {
            return;
        }
        if (isDebug()) {
            Slog.d(TAG, "Destroying the old remote service.");
        }
        this.mRemoteService.destroy();
        this.mRemoteService = null;
        synchronized (this.mLock) {
            this.mZombie = true;
        }
        RemoteSmartspaceService remoteServiceLocked = getRemoteServiceLocked();
        this.mRemoteService = remoteServiceLocked;
        if (remoteServiceLocked != null) {
            if (isDebug()) {
                Slog.d(TAG, "Rebinding to the new remote service.");
            }
            this.mRemoteService.reconnect();
        }
    }

    private void resurrectSessionsLocked() {
        int numSessions = this.mSessionInfos.size();
        if (isDebug()) {
            Slog.d(TAG, "Resurrecting remote service (" + this.mRemoteService + ") on " + numSessions + " sessions.");
        }
        for (SmartspaceSessionInfo sessionInfo : this.mSessionInfos.values()) {
            sessionInfo.resurrectSessionLocked(this, sessionInfo.mToken);
        }
    }

    protected boolean resolveService(SmartspaceSessionId sessionId, AbstractRemoteService.AsyncRequest<ISmartspaceService> cb) {
        RemoteSmartspaceService service = getRemoteServiceLocked();
        if (service != null) {
            service.executeOnResolvedService(cb);
        }
        return service != null;
    }

    private RemoteSmartspaceService getRemoteServiceLocked() {
        if (this.mRemoteService == null) {
            String serviceName = getComponentNameLocked();
            if (serviceName == null) {
                if (((SmartspaceManagerService) this.mMaster).verbose) {
                    Slog.v(TAG, "getRemoteServiceLocked(): not set");
                    return null;
                }
                return null;
            }
            ComponentName serviceComponent = ComponentName.unflattenFromString(serviceName);
            this.mRemoteService = new RemoteSmartspaceService(getContext(), "android.service.smartspace.SmartspaceService", serviceComponent, this.mUserId, this, ((SmartspaceManagerService) this.mMaster).isBindInstantServiceAllowed(), ((SmartspaceManagerService) this.mMaster).verbose);
        }
        return this.mRemoteService;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class SmartspaceSessionInfo {
        private static final boolean DEBUG = false;
        private final RemoteCallbackList<ISmartspaceCallback> mCallbacks = new RemoteCallbackList<ISmartspaceCallback>() { // from class: com.android.server.smartspace.SmartspacePerUserService.SmartspaceSessionInfo.1
            /* JADX DEBUG: Method merged with bridge method */
            @Override // android.os.RemoteCallbackList
            public void onCallbackDied(ISmartspaceCallback callback) {
                if (SmartspaceSessionInfo.this.mCallbacks.getRegisteredCallbackCount() == 0) {
                    SmartspaceSessionInfo.this.destroy();
                }
            }
        };
        final IBinder.DeathRecipient mDeathRecipient;
        private final SmartspaceSessionId mSessionId;
        private final SmartspaceConfig mSmartspaceConfig;
        final IBinder mToken;

        SmartspaceSessionInfo(SmartspaceSessionId id, SmartspaceConfig context, IBinder token, IBinder.DeathRecipient deathRecipient) {
            this.mSessionId = id;
            this.mSmartspaceConfig = context;
            this.mToken = token;
            this.mDeathRecipient = deathRecipient;
        }

        void addCallbackLocked(ISmartspaceCallback callback) {
            this.mCallbacks.register(callback);
        }

        void removeCallbackLocked(ISmartspaceCallback callback) {
            this.mCallbacks.unregister(callback);
        }

        boolean linkToDeath() {
            try {
                this.mToken.linkToDeath(this.mDeathRecipient, 0);
                return true;
            } catch (RemoteException e) {
                return false;
            }
        }

        void destroy() {
            IBinder iBinder = this.mToken;
            if (iBinder != null) {
                iBinder.unlinkToDeath(this.mDeathRecipient, 0);
            }
            this.mCallbacks.kill();
        }

        void resurrectSessionLocked(final SmartspacePerUserService service, IBinder token) {
            this.mCallbacks.getRegisteredCallbackCount();
            service.onCreateSmartspaceSessionLocked(this.mSmartspaceConfig, this.mSessionId, token);
            this.mCallbacks.broadcast(new Consumer() { // from class: com.android.server.smartspace.SmartspacePerUserService$SmartspaceSessionInfo$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    SmartspacePerUserService.SmartspaceSessionInfo.this.m6507x47828ab9(service, (ISmartspaceCallback) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$resurrectSessionLocked$0$com-android-server-smartspace-SmartspacePerUserService$SmartspaceSessionInfo  reason: not valid java name */
        public /* synthetic */ void m6507x47828ab9(SmartspacePerUserService service, ISmartspaceCallback callback) {
            service.registerSmartspaceUpdatesLocked(this.mSessionId, callback);
        }
    }
}
