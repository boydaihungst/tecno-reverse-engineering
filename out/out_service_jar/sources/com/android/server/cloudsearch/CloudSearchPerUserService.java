package com.android.server.cloudsearch;

import android.app.AppGlobals;
import android.app.cloudsearch.ICloudSearchManagerCallback;
import android.app.cloudsearch.SearchRequest;
import android.app.cloudsearch.SearchResponse;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.service.cloudsearch.ICloudSearchService;
import android.util.Slog;
import com.android.internal.infra.AbstractRemoteService;
import com.android.server.CircularQueue;
import com.android.server.cloudsearch.RemoteCloudSearchService;
import com.android.server.infra.AbstractPerUserSystemService;
/* loaded from: classes.dex */
public class CloudSearchPerUserService extends AbstractPerUserSystemService<CloudSearchPerUserService, CloudSearchManagerService> implements RemoteCloudSearchService.RemoteCloudSearchServiceCallbacks {
    private static final int QUEUE_SIZE = 10;
    private static final String TAG = CloudSearchPerUserService.class.getSimpleName();
    private final CircularQueue<String, CloudSearchCallbackInfo> mCallbackQueue;
    private final ComponentName mRemoteComponentName;
    private RemoteCloudSearchService mRemoteService;
    private final String mServiceName;
    private boolean mZombie;

    /* JADX INFO: Access modifiers changed from: protected */
    public CloudSearchPerUserService(CloudSearchManagerService master, Object lock, int userId, String serviceName) {
        super(master, lock, userId);
        this.mCallbackQueue = new CircularQueue<>(10);
        this.mServiceName = serviceName;
        this.mRemoteComponentName = ComponentName.unflattenFromString(serviceName);
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

    public void onSearchLocked(final SearchRequest searchRequest, ICloudSearchManagerCallback callback) {
        String filterList;
        if (this.mRemoteComponentName == null) {
            return;
        }
        if (searchRequest.getSearchConstraints().containsKey("android.app.cloudsearch.SEARCH_PROVIDER_FILTER")) {
            filterList = searchRequest.getSearchConstraints().getString("android.app.cloudsearch.SEARCH_PROVIDER_FILTER");
        } else {
            filterList = "";
        }
        String remoteServicePackageName = this.mRemoteComponentName.getPackageName();
        boolean wantedProvider = true;
        if (filterList.length() > 0) {
            wantedProvider = false;
            String[] providersSpecified = filterList.split(";");
            int i = 0;
            while (true) {
                if (i >= providersSpecified.length) {
                    break;
                } else if (!providersSpecified[i].equals(remoteServicePackageName)) {
                    i++;
                } else {
                    wantedProvider = true;
                    break;
                }
            }
        }
        if (!wantedProvider) {
            return;
        }
        boolean serviceExists = resolveService(searchRequest, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.cloudsearch.CloudSearchPerUserService$$ExternalSyntheticLambda0
            public final void run(IInterface iInterface) {
                ((ICloudSearchService) iInterface).onSearch(searchRequest);
            }
        });
        final String requestId = searchRequest.getRequestId();
        if (serviceExists && !this.mCallbackQueue.containsKey(requestId)) {
            CloudSearchCallbackInfo sessionInfo = new CloudSearchCallbackInfo(requestId, searchRequest, callback, callback.asBinder(), new IBinder.DeathRecipient() { // from class: com.android.server.cloudsearch.CloudSearchPerUserService$$ExternalSyntheticLambda1
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    CloudSearchPerUserService.this.m2671x5fe361e3(requestId);
                }
            });
            if (sessionInfo.linkToDeath()) {
                CloudSearchCallbackInfo removedInfo = this.mCallbackQueue.put(requestId, sessionInfo);
                if (removedInfo != null) {
                    removedInfo.destroy();
                    return;
                }
                return;
            }
            onDestroyLocked(requestId);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onSearchLocked$1$com-android-server-cloudsearch-CloudSearchPerUserService  reason: not valid java name */
    public /* synthetic */ void m2671x5fe361e3(String requestId) {
        synchronized (this.mLock) {
            onDestroyLocked(requestId);
        }
    }

    public void onReturnResultsLocked(IBinder token, String requestId, SearchResponse response) {
        ICloudSearchService serviceInterface;
        RemoteCloudSearchService remoteCloudSearchService = this.mRemoteService;
        if (remoteCloudSearchService != null && (serviceInterface = remoteCloudSearchService.getServiceInterface()) != null && token == serviceInterface.asBinder() && this.mCallbackQueue.containsKey(requestId)) {
            response.setSource(this.mServiceName);
            CloudSearchCallbackInfo sessionInfo = this.mCallbackQueue.getElement(requestId);
            try {
                if (response.getStatusCode() == 0) {
                    sessionInfo.mCallback.onSearchSucceeded(response);
                } else {
                    sessionInfo.mCallback.onSearchFailed(response);
                }
            } catch (RemoteException e) {
                if (((CloudSearchManagerService) this.mMaster).debug) {
                    Slog.e(TAG, "Exception in posting results");
                    e.printStackTrace();
                }
                onDestroyLocked(requestId);
            }
        }
    }

    public void onDestroyLocked(String requestId) {
        if (isDebug()) {
            Slog.d(TAG, "onDestroyLocked(): requestId=" + requestId);
        }
        CloudSearchCallbackInfo sessionInfo = this.mCallbackQueue.removeElement(requestId);
        if (sessionInfo != null) {
            sessionInfo.destroy();
        }
    }

    @Override // com.android.server.cloudsearch.RemoteCloudSearchService.RemoteCloudSearchServiceCallbacks
    public void onFailureOrTimeout(boolean timedOut) {
        if (isDebug()) {
            Slog.d(TAG, "onFailureOrTimeout(): timed out=" + timedOut);
        }
    }

    @Override // com.android.server.cloudsearch.RemoteCloudSearchService.RemoteCloudSearchServiceCallbacks
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
    public void onServiceDied(RemoteCloudSearchService service) {
        if (isDebug()) {
            Slog.w(TAG, "onServiceDied(): service=" + service);
        }
        synchronized (this.mLock) {
            this.mZombie = true;
        }
        updateRemoteServiceLocked();
    }

    private void updateRemoteServiceLocked() {
        RemoteCloudSearchService remoteCloudSearchService = this.mRemoteService;
        if (remoteCloudSearchService != null) {
            remoteCloudSearchService.destroy();
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
        RemoteCloudSearchService remoteServiceLocked = getRemoteServiceLocked();
        this.mRemoteService = remoteServiceLocked;
        if (remoteServiceLocked != null) {
            if (isDebug()) {
                Slog.d(TAG, "Rebinding to the new remote service.");
            }
            this.mRemoteService.reconnect();
        }
    }

    private void resurrectSessionsLocked() {
        int numCallbacks = this.mCallbackQueue.size();
        if (isDebug()) {
            Slog.d(TAG, "Resurrecting remote service (" + this.mRemoteService + ") on " + numCallbacks + " requests.");
        }
        for (CloudSearchCallbackInfo callbackInfo : this.mCallbackQueue.values()) {
            callbackInfo.resurrectSessionLocked(this, callbackInfo.mToken);
        }
    }

    protected boolean resolveService(SearchRequest requestId, AbstractRemoteService.AsyncRequest<ICloudSearchService> cb) {
        RemoteCloudSearchService service = getRemoteServiceLocked();
        if (service != null) {
            service.executeOnResolvedService(cb);
        }
        return service != null;
    }

    private RemoteCloudSearchService getRemoteServiceLocked() {
        if (this.mRemoteService == null) {
            String serviceName = getComponentNameForMultipleLocked(this.mServiceName);
            if (serviceName == null) {
                if (((CloudSearchManagerService) this.mMaster).verbose) {
                    Slog.v(TAG, "getRemoteServiceLocked(): not set");
                    return null;
                }
                return null;
            }
            ComponentName serviceComponent = ComponentName.unflattenFromString(serviceName);
            this.mRemoteService = new RemoteCloudSearchService(getContext(), "android.service.cloudsearch.CloudSearchService", serviceComponent, this.mUserId, this, ((CloudSearchManagerService) this.mMaster).isBindInstantServiceAllowed(), ((CloudSearchManagerService) this.mMaster).verbose);
        }
        return this.mRemoteService;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class CloudSearchCallbackInfo {
        private static final boolean DEBUG = false;
        private final ICloudSearchManagerCallback mCallback;
        final IBinder.DeathRecipient mDeathRecipient;
        private final String mRequestId;
        private final SearchRequest mSearchRequest;
        final IBinder mToken;

        CloudSearchCallbackInfo(String id, SearchRequest request, ICloudSearchManagerCallback callback, IBinder token, IBinder.DeathRecipient deathRecipient) {
            this.mRequestId = id;
            this.mSearchRequest = request;
            this.mCallback = callback;
            this.mToken = token;
            this.mDeathRecipient = deathRecipient;
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
            this.mCallback.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
        }

        void resurrectSessionLocked(CloudSearchPerUserService service, IBinder token) {
            service.onSearchLocked(this.mSearchRequest, this.mCallback);
        }
    }
}
