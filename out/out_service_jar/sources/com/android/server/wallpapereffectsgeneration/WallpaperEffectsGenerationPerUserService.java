package com.android.server.wallpapereffectsgeneration;

import android.app.AppGlobals;
import android.app.wallpapereffectsgeneration.CinematicEffectRequest;
import android.app.wallpapereffectsgeneration.CinematicEffectResponse;
import android.app.wallpapereffectsgeneration.ICinematicEffectListener;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.IInterface;
import android.os.RemoteException;
import android.service.wallpapereffectsgeneration.IWallpaperEffectsGenerationService;
import android.util.Slog;
import com.android.internal.infra.AbstractRemoteService;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.wallpapereffectsgeneration.RemoteWallpaperEffectsGenerationService;
/* loaded from: classes2.dex */
public class WallpaperEffectsGenerationPerUserService extends AbstractPerUserSystemService<WallpaperEffectsGenerationPerUserService, WallpaperEffectsGenerationManagerService> implements RemoteWallpaperEffectsGenerationService.RemoteWallpaperEffectsGenerationServiceCallback {
    private static final String TAG = WallpaperEffectsGenerationPerUserService.class.getSimpleName();
    private CinematicEffectListenerWrapper mCinematicEffectListenerWrapper;
    private RemoteWallpaperEffectsGenerationService mRemoteService;

    /* JADX INFO: Access modifiers changed from: protected */
    public WallpaperEffectsGenerationPerUserService(WallpaperEffectsGenerationManagerService master, Object lock, int userId) {
        super(master, lock, userId);
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        try {
            ServiceInfo si = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 128L, this.mUserId);
            if (!"android.permission.BIND_WALLPAPER_EFFECTS_GENERATION_SERVICE".equals(si.permission)) {
                Slog.w(TAG, "WallpaperEffectsGenerationService from '" + si.packageName + "' does not require permission android.permission.BIND_WALLPAPER_EFFECTS_GENERATION_SERVICE");
                throw new SecurityException("Service does not require permission android.permission.BIND_WALLPAPER_EFFECTS_GENERATION_SERVICE");
            }
            return si;
        } catch (RemoteException e) {
            throw new PackageManager.NameNotFoundException("Could not get service for " + serviceComponent);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractPerUserSystemService
    public boolean updateLocked(boolean disabled) {
        boolean enabledChanged = super.updateLocked(disabled);
        updateRemoteServiceLocked();
        return enabledChanged;
    }

    public void onGenerateCinematicEffectLocked(final CinematicEffectRequest cinematicEffectRequest, ICinematicEffectListener cinematicEffectListener) {
        String newTaskId = cinematicEffectRequest.getTaskId();
        CinematicEffectListenerWrapper cinematicEffectListenerWrapper = this.mCinematicEffectListenerWrapper;
        if (cinematicEffectListenerWrapper != null) {
            if (cinematicEffectListenerWrapper.mTaskId.equals(newTaskId)) {
                invokeCinematicListenerAndCleanup(new CinematicEffectResponse.Builder(3, newTaskId).build());
                return;
            } else {
                invokeCinematicListenerAndCleanup(new CinematicEffectResponse.Builder(4, newTaskId).build());
                return;
            }
        }
        RemoteWallpaperEffectsGenerationService remoteService = ensureRemoteServiceLocked();
        if (remoteService != null) {
            remoteService.executeOnResolvedService(new AbstractRemoteService.AsyncRequest() { // from class: com.android.server.wallpapereffectsgeneration.WallpaperEffectsGenerationPerUserService$$ExternalSyntheticLambda0
                public final void run(IInterface iInterface) {
                    ((IWallpaperEffectsGenerationService) iInterface).onGenerateCinematicEffect(cinematicEffectRequest);
                }
            });
            this.mCinematicEffectListenerWrapper = new CinematicEffectListenerWrapper(newTaskId, cinematicEffectListener);
            return;
        }
        if (isDebug()) {
            Slog.d(TAG, "Remote service not found");
        }
        try {
            cinematicEffectListener.onCinematicEffectGenerated(createErrorCinematicEffectResponse(newTaskId));
        } catch (RemoteException e) {
            if (isDebug()) {
                Slog.d(TAG, "Failed to invoke cinematic effect listener for task [" + newTaskId + "]");
            }
        }
    }

    public void onReturnCinematicEffectResponseLocked(CinematicEffectResponse cinematicEffectResponse) {
        invokeCinematicListenerAndCleanup(cinematicEffectResponse);
    }

    public boolean isCallingUidAllowed(int callingUid) {
        return getServiceUidLocked() == callingUid;
    }

    private void updateRemoteServiceLocked() {
        RemoteWallpaperEffectsGenerationService remoteWallpaperEffectsGenerationService = this.mRemoteService;
        if (remoteWallpaperEffectsGenerationService != null) {
            remoteWallpaperEffectsGenerationService.destroy();
            this.mRemoteService = null;
        }
        CinematicEffectListenerWrapper cinematicEffectListenerWrapper = this.mCinematicEffectListenerWrapper;
        if (cinematicEffectListenerWrapper != null) {
            invokeCinematicListenerAndCleanup(createErrorCinematicEffectResponse(cinematicEffectListenerWrapper.mTaskId));
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
        RemoteWallpaperEffectsGenerationService ensureRemoteServiceLocked = ensureRemoteServiceLocked();
        this.mRemoteService = ensureRemoteServiceLocked;
        if (ensureRemoteServiceLocked != null) {
            if (isDebug()) {
                Slog.d(TAG, "Rebinding to the new remote service.");
            }
            this.mRemoteService.reconnect();
        }
        CinematicEffectListenerWrapper cinematicEffectListenerWrapper = this.mCinematicEffectListenerWrapper;
        if (cinematicEffectListenerWrapper != null) {
            invokeCinematicListenerAndCleanup(createErrorCinematicEffectResponse(cinematicEffectListenerWrapper.mTaskId));
        }
    }

    private CinematicEffectResponse createErrorCinematicEffectResponse(String taskId) {
        return new CinematicEffectResponse.Builder(0, taskId).build();
    }

    private void invokeCinematicListenerAndCleanup(CinematicEffectResponse cinematicEffectResponse) {
        try {
            try {
                CinematicEffectListenerWrapper cinematicEffectListenerWrapper = this.mCinematicEffectListenerWrapper;
                if (cinematicEffectListenerWrapper != null && cinematicEffectListenerWrapper.mListener != null) {
                    this.mCinematicEffectListenerWrapper.mListener.onCinematicEffectGenerated(cinematicEffectResponse);
                } else if (isDebug()) {
                    Slog.w(TAG, "Cinematic effect listener not found for task[" + this.mCinematicEffectListenerWrapper.mTaskId + "]");
                }
            } catch (RemoteException e) {
                if (isDebug()) {
                    Slog.w(TAG, "Error invoking cinematic effect listener for task[" + this.mCinematicEffectListenerWrapper.mTaskId + "]");
                }
            }
        } finally {
            this.mCinematicEffectListenerWrapper = null;
        }
    }

    private RemoteWallpaperEffectsGenerationService ensureRemoteServiceLocked() {
        if (this.mRemoteService == null) {
            ComponentName serviceComponent = updateServiceInfoLocked();
            if (serviceComponent == null) {
                if (((WallpaperEffectsGenerationManagerService) this.mMaster).verbose) {
                    Slog.v(TAG, "ensureRemoteServiceLocked(): not set");
                    return null;
                }
                return null;
            }
            this.mRemoteService = new RemoteWallpaperEffectsGenerationService(getContext(), serviceComponent, this.mUserId, this, ((WallpaperEffectsGenerationManagerService) this.mMaster).isBindInstantServiceAllowed(), ((WallpaperEffectsGenerationManagerService) this.mMaster).verbose);
        }
        return this.mRemoteService;
    }

    /* JADX DEBUG: Method merged with bridge method */
    public void onServiceDied(RemoteWallpaperEffectsGenerationService service) {
        Slog.w(TAG, "remote wallpaper effects generation service died");
        updateRemoteServiceLocked();
    }

    @Override // com.android.server.wallpapereffectsgeneration.RemoteWallpaperEffectsGenerationService.RemoteWallpaperEffectsGenerationServiceCallback
    public void onConnectedStateChanged(boolean connected) {
        if (!connected) {
            Slog.w(TAG, "remote wallpaper effects generation service disconnected");
            updateRemoteServiceLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static final class CinematicEffectListenerWrapper {
        private final ICinematicEffectListener mListener;
        private final String mTaskId;

        CinematicEffectListenerWrapper(String taskId, ICinematicEffectListener listener) {
            this.mTaskId = taskId;
            this.mListener = listener;
        }
    }
}
