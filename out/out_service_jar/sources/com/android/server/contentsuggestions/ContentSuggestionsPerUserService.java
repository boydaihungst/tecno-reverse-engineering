package com.android.server.contentsuggestions;

import android.app.AppGlobals;
import android.app.contentsuggestions.ClassificationsRequest;
import android.app.contentsuggestions.IClassificationsCallback;
import android.app.contentsuggestions.ISelectionsCallback;
import android.app.contentsuggestions.SelectionsRequest;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.HardwareBuffer;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.contentsuggestions.RemoteContentSuggestionsService;
import com.android.server.infra.AbstractPerUserSystemService;
import com.android.server.wm.ActivityTaskManagerInternal;
/* loaded from: classes.dex */
public final class ContentSuggestionsPerUserService extends AbstractPerUserSystemService<ContentSuggestionsPerUserService, ContentSuggestionsManagerService> {
    private static final String TAG = ContentSuggestionsPerUserService.class.getSimpleName();
    private final ActivityTaskManagerInternal mActivityTaskManagerInternal;
    private RemoteContentSuggestionsService mRemoteService;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContentSuggestionsPerUserService(ContentSuggestionsManagerService master, Object lock, int userId) {
        super(master, lock, userId);
        this.mActivityTaskManagerInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
    }

    @Override // com.android.server.infra.AbstractPerUserSystemService
    protected ServiceInfo newServiceInfoLocked(ComponentName serviceComponent) throws PackageManager.NameNotFoundException {
        try {
            ServiceInfo si = AppGlobals.getPackageManager().getServiceInfo(serviceComponent, 128L, this.mUserId);
            if (!"android.permission.BIND_CONTENT_SUGGESTIONS_SERVICE".equals(si.permission)) {
                Slog.w(TAG, "ContentSuggestionsService from '" + si.packageName + "' does not require permission android.permission.BIND_CONTENT_SUGGESTIONS_SERVICE");
                throw new SecurityException("Service does not require permission android.permission.BIND_CONTENT_SUGGESTIONS_SERVICE");
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

    /* JADX INFO: Access modifiers changed from: package-private */
    public void provideContextImageFromBitmapLocked(Bundle bitmapContainingExtras) {
        provideContextImageLocked(-1, null, 0, bitmapContainingExtras);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void provideContextImageLocked(int taskId, HardwareBuffer snapshot, int colorSpaceIdForSnapshot, Bundle imageContextRequestExtras) {
        RemoteContentSuggestionsService service = ensureRemoteServiceLocked();
        if (service != null) {
            service.provideContextImage(taskId, snapshot, colorSpaceIdForSnapshot, imageContextRequestExtras);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void suggestContentSelectionsLocked(SelectionsRequest selectionsRequest, ISelectionsCallback selectionsCallback) {
        RemoteContentSuggestionsService service = ensureRemoteServiceLocked();
        if (service != null) {
            service.suggestContentSelections(selectionsRequest, selectionsCallback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void classifyContentSelectionsLocked(ClassificationsRequest classificationsRequest, IClassificationsCallback callback) {
        RemoteContentSuggestionsService service = ensureRemoteServiceLocked();
        if (service != null) {
            service.classifyContentSelections(classificationsRequest, callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyInteractionLocked(String requestId, Bundle bundle) {
        RemoteContentSuggestionsService service = ensureRemoteServiceLocked();
        if (service != null) {
            service.notifyInteraction(requestId, bundle);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRemoteServiceLocked() {
        RemoteContentSuggestionsService remoteContentSuggestionsService = this.mRemoteService;
        if (remoteContentSuggestionsService != null) {
            remoteContentSuggestionsService.destroy();
            this.mRemoteService = null;
        }
    }

    private RemoteContentSuggestionsService ensureRemoteServiceLocked() {
        if (this.mRemoteService == null) {
            String serviceName = getComponentNameLocked();
            if (serviceName == null) {
                if (((ContentSuggestionsManagerService) this.mMaster).verbose) {
                    Slog.v(TAG, "ensureRemoteServiceLocked(): not set");
                    return null;
                }
                return null;
            }
            ComponentName serviceComponent = ComponentName.unflattenFromString(serviceName);
            this.mRemoteService = new RemoteContentSuggestionsService(getContext(), serviceComponent, this.mUserId, new RemoteContentSuggestionsService.Callbacks() { // from class: com.android.server.contentsuggestions.ContentSuggestionsPerUserService.1
                /* JADX DEBUG: Method merged with bridge method */
                public void onServiceDied(RemoteContentSuggestionsService service) {
                    Slog.w(ContentSuggestionsPerUserService.TAG, "remote content suggestions service died");
                    ContentSuggestionsPerUserService.this.updateRemoteServiceLocked();
                }
            }, ((ContentSuggestionsManagerService) this.mMaster).isBindInstantServiceAllowed(), ((ContentSuggestionsManagerService) this.mMaster).verbose);
        }
        return this.mRemoteService;
    }
}
