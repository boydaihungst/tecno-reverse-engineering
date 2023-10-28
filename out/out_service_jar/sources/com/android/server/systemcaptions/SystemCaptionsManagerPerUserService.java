package com.android.server.systemcaptions;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.infra.AbstractPerUserSystemService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class SystemCaptionsManagerPerUserService extends AbstractPerUserSystemService<SystemCaptionsManagerPerUserService, SystemCaptionsManagerService> {
    private static final String TAG = SystemCaptionsManagerPerUserService.class.getSimpleName();
    private RemoteSystemCaptionsManagerService mRemoteService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SystemCaptionsManagerPerUserService(SystemCaptionsManagerService master, Object lock, boolean disabled, int userId) {
        super(master, lock, userId);
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        try {
            return AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 128L, this.mUserId);
        } catch (RemoteException e) {
            throw new PackageManager.NameNotFoundException("Could not get service for " + serviceComponent);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initializeLocked() {
        if (((SystemCaptionsManagerService) this.mMaster).verbose) {
            Slog.v(TAG, "initialize()");
        }
        RemoteSystemCaptionsManagerService service = getRemoteServiceLocked();
        if (service == null && ((SystemCaptionsManagerService) this.mMaster).verbose) {
            Slog.v(TAG, "initialize(): Failed to init remote server");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroyLocked() {
        if (((SystemCaptionsManagerService) this.mMaster).verbose) {
            Slog.v(TAG, "destroyLocked()");
        }
        RemoteSystemCaptionsManagerService remoteSystemCaptionsManagerService = this.mRemoteService;
        if (remoteSystemCaptionsManagerService != null) {
            remoteSystemCaptionsManagerService.destroy();
            this.mRemoteService = null;
        }
    }

    private RemoteSystemCaptionsManagerService getRemoteServiceLocked() {
        if (this.mRemoteService == null) {
            String serviceName = getComponentNameLocked();
            if (serviceName == null) {
                if (((SystemCaptionsManagerService) this.mMaster).verbose) {
                    Slog.v(TAG, "getRemoteServiceLocked(): Not set");
                    return null;
                }
                return null;
            }
            ComponentName serviceComponent = ComponentName.unflattenFromString(serviceName);
            this.mRemoteService = new RemoteSystemCaptionsManagerService(getContext(), serviceComponent, this.mUserId, ((SystemCaptionsManagerService) this.mMaster).verbose);
            if (((SystemCaptionsManagerService) this.mMaster).verbose) {
                Slog.v(TAG, "getRemoteServiceLocked(): initialize for user " + this.mUserId);
            }
            this.mRemoteService.initialize();
        }
        return this.mRemoteService;
    }
}
