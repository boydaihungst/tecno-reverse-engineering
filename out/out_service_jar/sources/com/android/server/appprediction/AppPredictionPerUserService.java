package com.android.server.appprediction;

import android.app.AppGlobals;
import android.app.prediction.AppPredictionContext;
import android.app.prediction.AppPredictionSessionId;
import android.app.prediction.AppTargetEvent;
import android.app.prediction.IPredictionCallback;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.provider.DeviceConfig;
import android.service.appprediction.IPredictionService;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.infra.AbstractRemoteService;
import com.android.server.LocalServices;
import com.android.server.appprediction.AppPredictionPerUserService;
import com.android.server.appprediction.RemoteAppPredictionService;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.people.PeopleServiceInternal;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class AppPredictionPerUserService extends AbstractPerUserSystemService<AppPredictionPerUserService, AppPredictionManagerService> implements RemoteAppPredictionService.RemoteAppPredictionServiceCallbacks {
    private static final String PREDICT_USING_PEOPLE_SERVICE_PREFIX = "predict_using_people_service_";
    private static final String REMOTE_APP_PREDICTOR_KEY = "remote_app_predictor";
    private static final String TAG = AppPredictionPerUserService.class.getSimpleName();
    private RemoteAppPredictionService mRemoteService;
    private final ArrayMap<AppPredictionSessionId, AppPredictionSessionInfo> mSessionInfos;
    private boolean mZombie;

    /* JADX INFO: Access modifiers changed from: protected */
    public AppPredictionPerUserService(AppPredictionManagerService master, Object lock, int userId) {
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
        if (enabledChanged && !isEnabledLocked()) {
            this.mRemoteService = null;
        }
        return enabledChanged;
    }

    public void onCreatePredictionSessionLocked(final AppPredictionContext context, final AppPredictionSessionId sessionId, IBinder token) {
        boolean usesPeopleService = DeviceConfig.getBoolean("systemui", PREDICT_USING_PEOPLE_SERVICE_PREFIX + context.getUiSurface(), false);
        if (context.getExtras() != null && context.getExtras().getBoolean(REMOTE_APP_PREDICTOR_KEY, false) && DeviceConfig.getBoolean("systemui", "dark_launch_remote_prediction_service_enabled", false)) {
            usesPeopleService = false;
        }
        boolean serviceExists = resolveService(sessionId, true, usesPeopleService, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.appprediction.AppPredictionPerUserService$$ExternalSyntheticLambda5
            public final void run(IInterface iInterface) {
                ((IPredictionService) iInterface).onCreatePredictionSession(context, sessionId);
            }
        });
        if (serviceExists && !this.mSessionInfos.containsKey(sessionId)) {
            AppPredictionSessionInfo sessionInfo = new AppPredictionSessionInfo(sessionId, context, usesPeopleService, token, new IBinder.DeathRecipient() { // from class: com.android.server.appprediction.AppPredictionPerUserService$$ExternalSyntheticLambda6
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    AppPredictionPerUserService.this.m1693xd35d2dde(sessionId);
                }
            });
            if (sessionInfo.linkToDeath()) {
                this.mSessionInfos.put(sessionId, sessionInfo);
            } else {
                onDestroyPredictionSessionLocked(sessionId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onCreatePredictionSessionLocked$1$com-android-server-appprediction-AppPredictionPerUserService  reason: not valid java name */
    public /* synthetic */ void m1693xd35d2dde(AppPredictionSessionId sessionId) {
        synchronized (this.mLock) {
            onDestroyPredictionSessionLocked(sessionId);
        }
    }

    public void notifyAppTargetEventLocked(final AppPredictionSessionId sessionId, final AppTargetEvent event) {
        AppPredictionSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, false, sessionInfo.mUsesPeopleService, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.appprediction.AppPredictionPerUserService$$ExternalSyntheticLambda1
            public final void run(IInterface iInterface) {
                ((IPredictionService) iInterface).notifyAppTargetEvent(sessionId, event);
            }
        });
    }

    public void notifyLaunchLocationShownLocked(final AppPredictionSessionId sessionId, final String launchLocation, final ParceledListSlice targetIds) {
        AppPredictionSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, false, sessionInfo.mUsesPeopleService, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.appprediction.AppPredictionPerUserService$$ExternalSyntheticLambda3
            public final void run(IInterface iInterface) {
                ((IPredictionService) iInterface).notifyLaunchLocationShown(sessionId, launchLocation, targetIds);
            }
        });
    }

    public void sortAppTargetsLocked(final AppPredictionSessionId sessionId, final ParceledListSlice targets, final IPredictionCallback callback) {
        AppPredictionSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, true, sessionInfo.mUsesPeopleService, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.appprediction.AppPredictionPerUserService$$ExternalSyntheticLambda8
            public final void run(IInterface iInterface) {
                ((IPredictionService) iInterface).sortAppTargets(sessionId, targets, callback);
            }
        });
    }

    public void registerPredictionUpdatesLocked(final AppPredictionSessionId sessionId, final IPredictionCallback callback) {
        AppPredictionSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        boolean serviceExists = resolveService(sessionId, true, sessionInfo.mUsesPeopleService, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.appprediction.AppPredictionPerUserService$$ExternalSyntheticLambda2
            public final void run(IInterface iInterface) {
                ((IPredictionService) iInterface).registerPredictionUpdates(sessionId, callback);
            }
        });
        if (serviceExists) {
            sessionInfo.addCallbackLocked(callback);
        }
    }

    public void unregisterPredictionUpdatesLocked(final AppPredictionSessionId sessionId, final IPredictionCallback callback) {
        AppPredictionSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        boolean serviceExists = resolveService(sessionId, false, sessionInfo.mUsesPeopleService, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.appprediction.AppPredictionPerUserService$$ExternalSyntheticLambda0
            public final void run(IInterface iInterface) {
                ((IPredictionService) iInterface).unregisterPredictionUpdates(sessionId, callback);
            }
        });
        if (serviceExists) {
            sessionInfo.removeCallbackLocked(callback);
        }
    }

    public void requestPredictionUpdateLocked(final AppPredictionSessionId sessionId) {
        AppPredictionSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, true, sessionInfo.mUsesPeopleService, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.appprediction.AppPredictionPerUserService$$ExternalSyntheticLambda7
            public final void run(IInterface iInterface) {
                ((IPredictionService) iInterface).requestPredictionUpdate(sessionId);
            }
        });
    }

    public void onDestroyPredictionSessionLocked(final AppPredictionSessionId sessionId) {
        if (isDebug()) {
            Slog.d(TAG, "onDestroyPredictionSessionLocked(): sessionId=" + sessionId);
        }
        AppPredictionSessionInfo sessionInfo = this.mSessionInfos.remove(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, false, sessionInfo.mUsesPeopleService, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.appprediction.AppPredictionPerUserService$$ExternalSyntheticLambda4
            public final void run(IInterface iInterface) {
                ((IPredictionService) iInterface).onDestroyPredictionSession(sessionId);
            }
        });
        sessionInfo.destroy();
    }

    @Override // com.android.server.appprediction.RemoteAppPredictionService.RemoteAppPredictionServiceCallbacks
    public void onFailureOrTimeout(boolean timedOut) {
        if (isDebug()) {
            Slog.d(TAG, "onFailureOrTimeout(): timed out=" + timedOut);
        }
    }

    @Override // com.android.server.appprediction.RemoteAppPredictionService.RemoteAppPredictionServiceCallbacks
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
    public void onServiceDied(RemoteAppPredictionService service) {
        if (isDebug()) {
            Slog.w(TAG, "onServiceDied(): service=" + service);
        }
        synchronized (this.mLock) {
            this.mZombie = true;
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
        RemoteAppPredictionService remoteServiceLocked = getRemoteServiceLocked();
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
        for (AppPredictionSessionInfo sessionInfo : this.mSessionInfos.values()) {
            sessionInfo.resurrectSessionLocked(this, sessionInfo.mToken);
        }
    }

    protected boolean resolveService(AppPredictionSessionId sessionId, boolean sendImmediately, boolean usesPeopleService, AbstractRemoteService.AsyncRequest<IPredictionService> cb) {
        if (usesPeopleService) {
            IPredictionService service = (IPredictionService) LocalServices.getService(PeopleServiceInternal.class);
            if (service != null) {
                try {
                    cb.run(service);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to invoke service:" + service, e);
                }
            }
            return service != null;
        }
        RemoteAppPredictionService service2 = getRemoteServiceLocked();
        if (service2 != null) {
            if (sendImmediately) {
                service2.executeOnResolvedService(cb);
            } else {
                service2.scheduleOnResolvedService(cb);
            }
        }
        return service2 != null;
    }

    private RemoteAppPredictionService getRemoteServiceLocked() {
        if (this.mRemoteService == null) {
            String serviceName = getComponentNameLocked();
            if (serviceName == null) {
                if (((AppPredictionManagerService) this.mMaster).verbose) {
                    Slog.v(TAG, "getRemoteServiceLocked(): not set");
                    return null;
                }
                return null;
            }
            ComponentName serviceComponent = ComponentName.unflattenFromString(serviceName);
            this.mRemoteService = new RemoteAppPredictionService(getContext(), "android.service.appprediction.AppPredictionService", serviceComponent, this.mUserId, this, ((AppPredictionManagerService) this.mMaster).isBindInstantServiceAllowed(), ((AppPredictionManagerService) this.mMaster).verbose);
        }
        return this.mRemoteService;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class AppPredictionSessionInfo {
        private static final boolean DEBUG = false;
        private final RemoteCallbackList<IPredictionCallback> mCallbacks = new RemoteCallbackList<IPredictionCallback>() { // from class: com.android.server.appprediction.AppPredictionPerUserService.AppPredictionSessionInfo.1
            /* JADX DEBUG: Method merged with bridge method */
            @Override // android.os.RemoteCallbackList
            public void onCallbackDied(IPredictionCallback callback) {
                if (AppPredictionSessionInfo.this.mCallbacks.getRegisteredCallbackCount() == 0) {
                    AppPredictionSessionInfo.this.destroy();
                }
            }
        };
        final IBinder.DeathRecipient mDeathRecipient;
        private final AppPredictionContext mPredictionContext;
        private final AppPredictionSessionId mSessionId;
        final IBinder mToken;
        private final boolean mUsesPeopleService;

        AppPredictionSessionInfo(AppPredictionSessionId id, AppPredictionContext predictionContext, boolean usesPeopleService, IBinder token, IBinder.DeathRecipient deathRecipient) {
            this.mSessionId = id;
            this.mPredictionContext = predictionContext;
            this.mUsesPeopleService = usesPeopleService;
            this.mToken = token;
            this.mDeathRecipient = deathRecipient;
        }

        void addCallbackLocked(IPredictionCallback callback) {
            this.mCallbacks.register(callback);
        }

        void removeCallbackLocked(IPredictionCallback callback) {
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

        void resurrectSessionLocked(final AppPredictionPerUserService service, IBinder token) {
            this.mCallbacks.getRegisteredCallbackCount();
            service.onCreatePredictionSessionLocked(this.mPredictionContext, this.mSessionId, token);
            this.mCallbacks.broadcast(new Consumer() { // from class: com.android.server.appprediction.AppPredictionPerUserService$AppPredictionSessionInfo$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    AppPredictionPerUserService.AppPredictionSessionInfo.this.m1696x3d2b3e5c(service, (IPredictionCallback) obj);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* renamed from: lambda$resurrectSessionLocked$0$com-android-server-appprediction-AppPredictionPerUserService$AppPredictionSessionInfo  reason: not valid java name */
        public /* synthetic */ void m1696x3d2b3e5c(AppPredictionPerUserService service, IPredictionCallback callback) {
            service.registerPredictionUpdatesLocked(this.mSessionId, callback);
        }
    }
}
