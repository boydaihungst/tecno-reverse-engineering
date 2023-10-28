package com.android.server.searchui;

import android.app.AppGlobals;
import android.app.search.ISearchCallback;
import android.app.search.Query;
import android.app.search.SearchContext;
import android.app.search.SearchSessionId;
import android.app.search.SearchTargetEvent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.service.search.ISearchUiService;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.infra.AbstractRemoteService;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.searchui.RemoteSearchUiService;
/* loaded from: classes2.dex */
public class SearchUiPerUserService extends AbstractPerUserSystemService<SearchUiPerUserService, SearchUiManagerService> implements RemoteSearchUiService.RemoteSearchUiServiceCallbacks {
    private static final String TAG = SearchUiPerUserService.class.getSimpleName();
    private RemoteSearchUiService mRemoteService;
    private final ArrayMap<SearchSessionId, SearchSessionInfo> mSessionInfos;
    private boolean mZombie;

    /* JADX INFO: Access modifiers changed from: protected */
    public SearchUiPerUserService(SearchUiManagerService master, Object lock, int userId) {
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
            updateRemoteServiceLocked();
        }
        return enabledChanged;
    }

    public void onCreateSearchSessionLocked(final SearchContext context, final SearchSessionId sessionId, IBinder token) {
        boolean serviceExists = resolveService(sessionId, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.searchui.SearchUiPerUserService$$ExternalSyntheticLambda1
            public final void run(IInterface iInterface) {
                ((ISearchUiService) iInterface).onCreateSearchSession(context, sessionId);
            }
        });
        if (serviceExists && !this.mSessionInfos.containsKey(sessionId)) {
            SearchSessionInfo sessionInfo = new SearchSessionInfo(sessionId, context, token, new IBinder.DeathRecipient() { // from class: com.android.server.searchui.SearchUiPerUserService$$ExternalSyntheticLambda2
                @Override // android.os.IBinder.DeathRecipient
                public final void binderDied() {
                    SearchUiPerUserService.this.m6410xf73ff7d1(sessionId);
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
    /* renamed from: lambda$onCreateSearchSessionLocked$1$com-android-server-searchui-SearchUiPerUserService  reason: not valid java name */
    public /* synthetic */ void m6410xf73ff7d1(SearchSessionId sessionId) {
        synchronized (this.mLock) {
            onDestroyLocked(sessionId);
        }
    }

    public void notifyLocked(final SearchSessionId sessionId, final Query query, final SearchTargetEvent event) {
        SearchSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.searchui.SearchUiPerUserService$$ExternalSyntheticLambda3
            public final void run(IInterface iInterface) {
                ((ISearchUiService) iInterface).onNotifyEvent(sessionId, query, event);
            }
        });
    }

    public void queryLocked(final SearchSessionId sessionId, final Query input, final ISearchCallback callback) {
        SearchSessionInfo sessionInfo = this.mSessionInfos.get(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.searchui.SearchUiPerUserService$$ExternalSyntheticLambda0
            public final void run(IInterface iInterface) {
                ((ISearchUiService) iInterface).onQuery(sessionId, input, callback);
            }
        });
    }

    public void onDestroyLocked(final SearchSessionId sessionId) {
        if (isDebug()) {
            Slog.d(TAG, "onDestroyLocked(): sessionId=" + sessionId);
        }
        SearchSessionInfo sessionInfo = this.mSessionInfos.remove(sessionId);
        if (sessionInfo == null) {
            return;
        }
        resolveService(sessionId, new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.searchui.SearchUiPerUserService$$ExternalSyntheticLambda4
            public final void run(IInterface iInterface) {
                ((ISearchUiService) iInterface).onDestroy(sessionId);
            }
        });
        sessionInfo.destroy();
    }

    @Override // com.android.server.searchui.RemoteSearchUiService.RemoteSearchUiServiceCallbacks
    public void onFailureOrTimeout(boolean timedOut) {
        if (isDebug()) {
            Slog.d(TAG, "onFailureOrTimeout(): timed out=" + timedOut);
        }
    }

    @Override // com.android.server.searchui.RemoteSearchUiService.RemoteSearchUiServiceCallbacks
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
    public void onServiceDied(RemoteSearchUiService service) {
        if (isDebug()) {
            Slog.w(TAG, "onServiceDied(): service=" + service);
        }
        synchronized (this.mLock) {
            this.mZombie = true;
        }
        updateRemoteServiceLocked();
    }

    private void updateRemoteServiceLocked() {
        RemoteSearchUiService remoteSearchUiService = this.mRemoteService;
        if (remoteSearchUiService != null) {
            remoteSearchUiService.destroy();
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
        RemoteSearchUiService remoteServiceLocked = getRemoteServiceLocked();
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
        for (SearchSessionInfo sessionInfo : this.mSessionInfos.values()) {
            sessionInfo.resurrectSessionLocked(this, sessionInfo.mToken);
        }
    }

    protected boolean resolveService(SearchSessionId sessionId, AbstractRemoteService.AsyncRequest<ISearchUiService> cb) {
        RemoteSearchUiService service = getRemoteServiceLocked();
        if (service != null) {
            service.executeOnResolvedService(cb);
        }
        return service != null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public RemoteSearchUiService getRemoteServiceLocked() {
        if (this.mRemoteService == null) {
            String serviceName = getComponentNameLocked();
            if (serviceName == null) {
                if (((SearchUiManagerService) this.mMaster).verbose) {
                    Slog.v(TAG, "getRemoteServiceLocked(): not set");
                    return null;
                }
                return null;
            }
            ComponentName serviceComponent = ComponentName.unflattenFromString(serviceName);
            this.mRemoteService = new RemoteSearchUiService(getContext(), "android.service.search.SearchUiService", serviceComponent, this.mUserId, this, ((SearchUiManagerService) this.mMaster).isBindInstantServiceAllowed(), ((SearchUiManagerService) this.mMaster).verbose);
        }
        return this.mRemoteService;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class SearchSessionInfo {
        private static final boolean DEBUG = true;
        private final RemoteCallbackList<ISearchCallback> mCallbacks = new RemoteCallbackList<ISearchCallback>() { // from class: com.android.server.searchui.SearchUiPerUserService.SearchSessionInfo.1
            /* JADX DEBUG: Method merged with bridge method */
            @Override // android.os.RemoteCallbackList
            public void onCallbackDied(ISearchCallback callback) {
                Slog.d(SearchUiPerUserService.TAG, "Binder died for session Id=" + SearchSessionInfo.this.mSessionId + " and callback=" + callback.asBinder());
                if (SearchSessionInfo.this.mCallbacks.getRegisteredCallbackCount() == 0) {
                    SearchSessionInfo.this.destroy();
                }
            }
        };
        final IBinder.DeathRecipient mDeathRecipient;
        private final SearchContext mSearchContext;
        private final SearchSessionId mSessionId;
        final IBinder mToken;

        SearchSessionInfo(SearchSessionId id, SearchContext context, IBinder token, IBinder.DeathRecipient deathRecipient) {
            Slog.d(SearchUiPerUserService.TAG, "Creating SearchSessionInfo for session Id=" + id);
            this.mSessionId = id;
            this.mSearchContext = context;
            this.mToken = token;
            this.mDeathRecipient = deathRecipient;
        }

        boolean linkToDeath() {
            try {
                this.mToken.linkToDeath(this.mDeathRecipient, 0);
                return true;
            } catch (RemoteException e) {
                Slog.w(SearchUiPerUserService.TAG, "Caller is dead before session can be started, sessionId: " + this.mSessionId);
                return false;
            }
        }

        void destroy() {
            Slog.d(SearchUiPerUserService.TAG, "Removing all callbacks for session Id=" + this.mSessionId + " and " + this.mCallbacks.getRegisteredCallbackCount() + " callbacks.");
            IBinder iBinder = this.mToken;
            if (iBinder != null) {
                iBinder.unlinkToDeath(this.mDeathRecipient, 0);
            }
            this.mCallbacks.kill();
        }

        void resurrectSessionLocked(SearchUiPerUserService service, IBinder token) {
            int callbackCount = this.mCallbacks.getRegisteredCallbackCount();
            Slog.d(SearchUiPerUserService.TAG, "Resurrecting remote service (" + service.getRemoteServiceLocked() + ") for session Id=" + this.mSessionId + " and " + callbackCount + " callbacks.");
            service.onCreateSearchSessionLocked(this.mSearchContext, this.mSessionId, token);
        }
    }
}
