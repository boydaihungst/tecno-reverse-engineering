package com.android.server.systemcaptions;

import android.content.Context;
import com.android.server.infra.AbstractMasterSystemService;
import com.android.server.infra.FrameworkResourcesServiceNameResolver;
/* loaded from: classes2.dex */
public final class SystemCaptionsManagerService extends AbstractMasterSystemService<SystemCaptionsManagerService, SystemCaptionsManagerPerUserService> {
    public SystemCaptionsManagerService(Context context) {
        super(context, new FrameworkResourcesServiceNameResolver(context, 17039945), null, 68);
    }

    @Override // com.android.server.SystemService
    public void onStart() {
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public SystemCaptionsManagerPerUserService newServiceLocked(int resolvedUserId, boolean disabled) {
        SystemCaptionsManagerPerUserService perUserService = new SystemCaptionsManagerPerUserService(this, this.mLock, disabled, resolvedUserId);
        perUserService.initializeLocked();
        return perUserService;
    }

    /* JADX DEBUG: Method merged with bridge method */
    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.infra.AbstractMasterSystemService
    public void onServiceRemoved(SystemCaptionsManagerPerUserService service, int userId) {
        synchronized (this.mLock) {
            service.destroyLocked();
        }
    }
}
